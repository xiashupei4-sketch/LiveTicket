package com.liveticket.event.dto;

import lombok.Data;

@Data
public class EventQueryRequest {

    private long page = 1;
    private long pageSize = 20;
    private String keyword;
    private String cityCode;
    private String category;
}
