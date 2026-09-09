package com.liveticket.event.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liveticket.common.api.PageResponse;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.enums.EventStatus;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.event.dto.EventQueryRequest;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.event.service.EventService;
import com.liveticket.event.vo.EventVO;
import com.liveticket.ticket.entity.TicketSku;
import com.liveticket.ticket.mapper.TicketSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventMapper eventMapper;
    private final TicketSkuMapper ticketSkuMapper;

    @Override
    public PageResponse<EventVO> listEvents(EventQueryRequest request) {
        Page<Event> page = new Page<>(request.getPage(), request.getPageSize());
        Page<Event> result = eventMapper.selectPage(page, Wrappers.<Event>lambdaQuery()
                .and(StringUtils.hasText(request.getKeyword()), w -> w
                        .like(Event::getTitle, request.getKeyword())
                        .or()
                        .like(Event::getArtist, request.getKeyword()))
                .eq(StringUtils.hasText(request.getCityCode()), Event::getCityCode, request.getCityCode())
                .eq(StringUtils.hasText(request.getCategory()), Event::getCategory, request.getCategory())
                .orderByAsc(Event::getStartTime));

        List<EventVO> records = toVOsWithMinPrice(result.getRecords());
        return PageResponse.of(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public List<EventVO> listHotEvents(int limit) {
        Page<Event> page = new Page<>(1, limit);
        Page<Event> result = eventMapper.selectPage(page, Wrappers.<Event>lambdaQuery()
                .eq(Event::getStatus, EventStatus.ON_SALE.getCode())
                .orderByDesc(Event::getHeatScore));
        return toVOsWithMinPrice(result.getRecords());
    }

    @Override
    public PageResponse<EventVO> listEventsByCity(String cityCode, long page, long pageSize) {
        Page<Event> mpPage = new Page<>(page, pageSize);
        Page<Event> result = eventMapper.selectPage(mpPage, Wrappers.<Event>lambdaQuery()
                .eq(Event::getCityCode, cityCode)
                .eq(Event::getStatus, EventStatus.ON_SALE.getCode())
                .orderByAsc(Event::getStartTime));

        List<EventVO> records = toVOsWithMinPrice(result.getRecords());
        return PageResponse.of(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    private Map<Long, BigDecimal> queryMinPrice(List<Long> eventIds) {
        return ticketSkuMapper.selectList(Wrappers.<TicketSku>lambdaQuery()
                        .in(TicketSku::getEventId, eventIds)
                        .eq(TicketSku::getStatus, 1))
                .stream()
                .collect(Collectors.toMap(TicketSku::getEventId, TicketSku::getPrice, BigDecimal::min));
    }

    private List<EventVO> toVOsWithMinPrice(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, BigDecimal> minPriceMap = queryMinPrice(eventIds);

        return events.stream()
                .map(event -> {
                    EventVO vo = new EventVO();
                    copyToVO(event, vo);
                    vo.setMinPrice(minPriceMap.get(event.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private void copyToVO(Event event, EventVO vo) {
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
    }
}
