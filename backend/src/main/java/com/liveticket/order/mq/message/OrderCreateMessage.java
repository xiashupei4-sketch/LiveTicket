package com.liveticket.order.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateMessage implements Serializable {

    private String messageId;
    private String orderNo;
    private Long userId;
    private Long eventId;
    private Long ticketSkuId;
    private Integer quantity;
    private Long timestamp;
}
