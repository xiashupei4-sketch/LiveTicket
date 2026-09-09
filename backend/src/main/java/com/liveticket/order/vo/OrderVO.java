package com.liveticket.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private String orderNo;
    private Long userId;
    private Long eventId;
    private String eventTitle;
    private String eventCoverUrl;
    private LocalDateTime eventStartTime;
    private String venueName;
    private Long ticketSkuId;
    private String skuName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer status;
    private Integer source;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
}
