package com.liveticket.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("lt_ticket_sku")
public class TicketSku {

    private Long id;
    private Long eventId;
    private String skuName;
    private BigDecimal price;
    private Integer totalStock;
    private Integer availableStock;
    private Integer perUserLimit;
    private Integer status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
