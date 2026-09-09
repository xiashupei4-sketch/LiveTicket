package com.liveticket.ticket.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.ticket.entity.TicketSku;
import com.liveticket.ticket.mapper.TicketSkuMapper;
import com.liveticket.ticket.service.TicketService;
import com.liveticket.ticket.vo.TicketSkuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketSkuMapper ticketSkuMapper;
    private final EventMapper eventMapper;

    @Override
    public List<TicketSkuVO> listByEventId(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        return ticketSkuMapper.selectList(Wrappers.<TicketSku>lambdaQuery()
                        .eq(TicketSku::getEventId, eventId)
                        .eq(TicketSku::getStatus, 1)
                        .orderByAsc(TicketSku::getPrice))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private TicketSkuVO toVO(TicketSku sku) {
        TicketSkuVO vo = new TicketSkuVO();
        vo.setId(sku.getId());
        vo.setEventId(sku.getEventId());
        vo.setSkuName(sku.getSkuName());
        vo.setPrice(sku.getPrice());
        vo.setTotalStock(sku.getTotalStock());
        vo.setAvailableStock(sku.getAvailableStock());
        vo.setPerUserLimit(sku.getPerUserLimit());
        vo.setStatus(sku.getStatus());
        return vo;
    }
}
