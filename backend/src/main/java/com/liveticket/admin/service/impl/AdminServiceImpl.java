package com.liveticket.admin.service.impl;

import com.liveticket.admin.service.AdminService;
import com.liveticket.common.constant.RedisKeys;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.ticket.entity.TicketSku;
import com.liveticket.ticket.mapper.TicketSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final TicketSkuMapper ticketSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public long initSeckillStock(Long ticketSkuId) {
        TicketSku sku = ticketSkuMapper.selectById(ticketSkuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BusinessException(ErrorCode.TICKET_SKU_NOT_FOUND);
        }

        long availableStock = sku.getAvailableStock() == null ? 0 : sku.getAvailableStock();
        String stockKey = RedisKeys.seckillStock(ticketSkuId);
        String userKey = RedisKeys.seckillUsers(ticketSkuId);
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(availableStock));
        stringRedisTemplate.delete(userKey);

        log.info("[ADMIN] seckill init skuId={} stock={} usersSetCleared", ticketSkuId, availableStock);
        return availableStock;
    }
}
