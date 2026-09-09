package com.liveticket.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull(message = "ticketSkuId is required")
    private Long ticketSkuId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be >= 1")
    @Max(value = 10, message = "quantity must be <= 10")
    private Integer quantity;
}
