package com.liveticket.ticket.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TicketSkuVO {

    private Long id;
    private Long eventId;
    private String skuName;
    private BigDecimal price;
    private Integer totalStock;
    private Integer availableStock;
    private Integer perUserLimit;
    private Integer status;
}
