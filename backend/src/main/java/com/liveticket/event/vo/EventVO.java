package com.liveticket.event.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EventVO {

    private Long id;
    private String title;
    private String artist;
    private String category;
    private String cityCode;
    private String venueName;
    private String address;
    private String coverUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private Integer status;
    private Long heatScore;
    private BigDecimal minPrice;
}
