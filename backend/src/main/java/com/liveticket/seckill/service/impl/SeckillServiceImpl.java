package com.liveticket.seckill.service.impl;

import com.liveticket.common.constant.RedisKeys;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.enums.EventStatus;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.common.util.OrderNoGenerator;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.order.mq.message.OrderCreateMessage;
import com.liveticket.order.mq.producer.OrderMessageProducer;
import com.liveticket.seckill.service.SeckillService;
import com.liveticket.seckill.vo.SeckillResultVO;
import com.liveticket.ticket.entity.TicketSku;
import com.liveticket.ticket.mapper.TicketSkuMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private static final Duration RESULT_TTL = Duration.ofSeconds(1800);
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String RESULT_SEPARATOR = ":";

    private final StringRedisTemplate stringRedisTemplate;
    private final TicketSkuMapper ticketSkuMapper;
    private final EventMapper eventMapper;
    private final OrderMessageProducer orderMessageProducer;

    private RedisScript<Long> seckillScript;

    @PostConstruct
    void initScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/seckill.lua"));
        script.setResultType(Long.class);
        this.seckillScript = script;
    }

    /**
     * 启动预热：把所有在售演出的票档库存写入 Redis（SETNX，已存在的 Key 不覆盖，
     * 避免重启打断进行中的秒杀）。管理接口 /api/admin/seckill/init/{skuId} 可强制重置。
     */
    @PostConstruct
    void warmUpSeckillStock() {
        try {
            List<TicketSku> skus = ticketSkuMapper.selectList(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers.<TicketSku>lambdaQuery()
                            .eq(TicketSku::getStatus, 1)
                            .gt(TicketSku::getAvailableStock, 0));
            int warmed = 0;
            for (TicketSku sku : skus) {
                Event event = eventMapper.selectById(sku.getEventId());
                if (event == null || event.getStatus() == null
                        || event.getStatus() != EventStatus.ON_SALE.getCode()) {
                    continue;
                }
                Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                        RedisKeys.seckillStock(sku.getId()), String.valueOf(sku.getAvailableStock()));
                if (Boolean.TRUE.equals(ok)) {
                    warmed++;
                }
            }
            log.info("[SECKILL] warmup done: {} on-sale SKU stock keys initialized (existing keys kept)", warmed);
        } catch (Exception e) {
            // 预热失败不阻断启动，可由管理接口手动初始化
            log.error("[SECKILL] warmup failed, fallback to admin init API", e);
        }
    }

    @Override
    public SeckillResultVO executeSeckill(Long userId, Long ticketSkuId) {
        TicketSku sku = ticketSkuMapper.selectById(ticketSkuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BusinessException(ErrorCode.TICKET_SKU_NOT_FOUND);
        }

        Event event = eventMapper.selectById(sku.getEventId());
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        if (event.getSaleStartTime() != null && now.isBefore(event.getSaleStartTime())) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (event.getSaleEndTime() != null && now.isAfter(event.getSaleEndTime())
                || event.getStatus() == null || event.getStatus() != EventStatus.ON_SALE.getCode()) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED);
        }

        String stockKey = RedisKeys.seckillStock(ticketSkuId);
        String userKey = RedisKeys.seckillUsers(ticketSkuId);
        Long result = stringRedisTemplate.execute(
                seckillScript, List.of(stockKey, userKey), String.valueOf(userId));

        int resultCode = result == null ? -1 : result.intValue();
        if (resultCode == 1) {
            log.info("[SECKILL] userId={} skuId={} result=OUT_OF_STOCK", userId, ticketSkuId);
            throw new BusinessException(ErrorCode.SECKILL_OUT_OF_STOCK);
        }
        if (resultCode == 2) {
            log.info("[SECKILL] userId={} skuId={} result=DUPLICATE", userId, ticketSkuId);
            throw new BusinessException(ErrorCode.DUPLICATE_PURCHASE);
        }
        if (resultCode != 0) {
            log.warn("[SECKILL] userId={} skuId={} result=UNKNOWN({})", userId, ticketSkuId, resultCode);
            throw new BusinessException(ErrorCode.SECKILL_FAILED);
        }

        String resultKey = RedisKeys.seckillResult(userId, ticketSkuId);
        stringRedisTemplate.opsForValue().set(resultKey, STATUS_PROCESSING, RESULT_TTL);

        // 生成订单号与 messageId，投递 RabbitMQ 异步创建订单
        String orderNo = OrderNoGenerator.generate();
        String messageId = java.util.UUID.randomUUID().toString();
        OrderCreateMessage message = new OrderCreateMessage(
                messageId, orderNo, userId, sku.getEventId(), ticketSkuId, 1, System.currentTimeMillis());
        orderMessageProducer.send(message);

        log.info("[SECKILL] userId={} skuId={} result=ACCEPTED status=PROCESSING messageId={} orderNo={}",
                userId, ticketSkuId, messageId, orderNo);

        return new SeckillResultVO(STATUS_PROCESSING, null, null);
    }

    @Override
    public SeckillResultVO getResult(Long userId, Long ticketSkuId) {
        String resultKey = RedisKeys.seckillResult(userId, ticketSkuId);
        String value = stringRedisTemplate.opsForValue().get(resultKey);

        if (value == null) {
            return new SeckillResultVO(STATUS_PROCESSING, null, null);
        }
        if (STATUS_PROCESSING.equals(value)) {
            return new SeckillResultVO(STATUS_PROCESSING, null, null);
        }
        if (value.startsWith(STATUS_SUCCESS + RESULT_SEPARATOR)) {
            String orderNo = value.substring(STATUS_SUCCESS.length() + RESULT_SEPARATOR.length());
            return new SeckillResultVO(STATUS_SUCCESS, orderNo, null);
        }
        if (value.startsWith(STATUS_FAILED + RESULT_SEPARATOR)) {
            String reason = value.substring(STATUS_FAILED.length() + RESULT_SEPARATOR.length());
            return new SeckillResultVO(STATUS_FAILED, null, reason);
        }
        return new SeckillResultVO(STATUS_PROCESSING, null, null);
    }
}
