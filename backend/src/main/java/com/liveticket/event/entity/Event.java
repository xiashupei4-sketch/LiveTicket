package com.liveticket.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("lt_event")
public class Event {

    private Long id;
    private String title;
    private String artist;
    private String category;
    private String description;
    private String cityCode;
    private String venueName;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String coverUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private Integer status;
    private Long heatScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
