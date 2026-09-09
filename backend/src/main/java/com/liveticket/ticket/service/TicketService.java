package com.liveticket.ticket.service;

import com.liveticket.ticket.vo.TicketSkuVO;

import java.util.List;

public interface TicketService {

    List<TicketSkuVO> listByEventId(Long eventId);
}
