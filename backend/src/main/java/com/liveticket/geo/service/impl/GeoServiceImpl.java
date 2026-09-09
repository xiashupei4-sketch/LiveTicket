package com.liveticket.geo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liveticket.common.constant.RedisKeys;
import com.liveticket.common.enums.EventStatus;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.geo.service.GeoService;
import com.liveticket.geo.vo.NearbyEventVO;
import com.liveticket.ticket.entity.TicketSku;
import com.liveticket.ticket.mapper.TicketSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoServiceImpl implements GeoService {

    private final StringRedisTemplate stringRedisTemplate;
    private final EventMapper eventMapper;
    private final TicketSkuMapper ticketSkuMapper;

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void rebuildGeoCache() {
        List<Event> onSaleEvents = eventMapper.selectList(Wrappers.<Event>lambdaQuery()
                .eq(Event::getStatus, EventStatus.ON_SALE.getCode()));
        List<String> cityCodes = onSaleEvents.stream()
                .map(Event::getCityCode)
                .distinct()
                .collect(Collectors.toList());

        // 先删除当前在售城市的 GEO Key
        for (String cityCode : cityCodes) {
            stringRedisTemplate.delete(RedisKeys.geoEvent(cityCode));
        }
        // 按城市分组 GEOADD
        Map<String, List<Event>> byCity = onSaleEvents.stream()
                .collect(Collectors.groupingBy(Event::getCityCode));
        for (Map.Entry<String, List<Event>> entry : byCity.entrySet()) {
            String key = RedisKeys.geoEvent(entry.getKey());
            for (Event event : entry.getValue()) {
                stringRedisTemplate.opsForGeo().add(key, new Point(
                        event.getLongitude().doubleValue(),
                        event.getLatitude().doubleValue()),
                        String.valueOf(event.getId()));
            }
        }
        log.info("[GEO] rebuilt cities={} events={}", byCity.size(), onSaleEvents.size());
    }

    @Override
    public List<NearbyEventVO> listNearby(double longitude, double latitude, double radiusKm, int pageSize) {
        List<Event> onSaleEvents = eventMapper.selectList(Wrappers.<Event>lambdaQuery()
                .eq(Event::getStatus, EventStatus.ON_SALE.getCode()));
        List<String> cityCodes = onSaleEvents.stream()
                .map(Event::getCityCode)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Event> eventMap = onSaleEvents.stream()
                .collect(Collectors.toMap(Event::getId, e -> e));
        Map<Long, BigDecimal> minPriceMap = queryMinPrice(
                onSaleEvents.stream().map(Event::getId).collect(Collectors.toList()));

        // 各城市 GEO Key 逐一 GEORADIUS（Redis 5.0 兼容，不使用 GEOSEARCH）
        List<NearbyEventVO> merged = new ArrayList<>();
        for (String cityCode : cityCodes) {
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                    .radius(RedisKeys.geoEvent(cityCode),
                            new Circle(new Point(longitude, latitude), new Distance(radiusKm, Metrics.KILOMETERS)),
                            RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                    .includeDistance()
                                    .sortAscending());
            if (results == null) {
                continue;
            }
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult : results) {
                Long eventId = Long.parseLong(geoResult.getContent().getName());
                Event event = eventMap.get(eventId);
                if (event == null) {
                    continue;
                }
                NearbyEventVO vo = new NearbyEventVO();
                vo.setId(event.getId());
                vo.setTitle(event.getTitle());
                vo.setArtist(event.getArtist());
                vo.setCategory(event.getCategory());
                vo.setCityCode(event.getCityCode());
                vo.setVenueName(event.getVenueName());
                vo.setAddress(event.getAddress());
                vo.setCoverUrl(event.getCoverUrl());
                vo.setStartTime(event.getStartTime());
                vo.setEndTime(event.getEndTime());
                vo.setSaleStartTime(event.getSaleStartTime());
                vo.setSaleEndTime(event.getSaleEndTime());
                vo.setStatus(event.getStatus());
                vo.setHeatScore(event.getHeatScore());
                vo.setMinPrice(minPriceMap.get(event.getId()));
                vo.setDistanceKm(geoResult.getDistance().getValue());
                merged.add(vo);
            }
        }

        return merged.stream()
                .sorted(Comparator.comparingDouble(NearbyEventVO::getDistanceKm))
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    private Map<Long, BigDecimal> queryMinPrice(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return ticketSkuMapper.selectList(Wrappers.<TicketSku>lambdaQuery()
                        .in(TicketSku::getEventId, eventIds)
                        .eq(TicketSku::getStatus, 1))
                .stream()
                .collect(Collectors.toMap(TicketSku::getEventId, TicketSku::getPrice, BigDecimal::min));
    }
}
