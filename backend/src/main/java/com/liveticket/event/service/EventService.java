package com.liveticket.event.service;

import com.liveticket.common.api.PageResponse;
import com.liveticket.event.dto.EventQueryRequest;
import com.liveticket.event.vo.EventDetailVO;
import com.liveticket.event.vo.EventVO;

import java.util.List;

public interface EventService {

    PageResponse<EventVO> listEvents(EventQueryRequest request);

    List<EventVO> listHotEvents(int limit);

    PageResponse<EventVO> listEventsByCity(String cityCode, long page, long pageSize);
}
