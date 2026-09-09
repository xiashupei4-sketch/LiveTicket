package com.liveticket.order.controller;

import com.liveticket.common.api.ApiResponse;
import com.liveticket.common.api.PageResponse;
import com.liveticket.common.context.UserContext;
import com.liveticket.order.dto.CreateOrderRequest;
import com.liveticket.order.service.OrderService;
import com.liveticket.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders", description = "订单")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建普通订单", description = "普通订单同步创建，不走 RabbitMQ")
    @PostMapping
    public ApiResponse<OrderVO> createNormalOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.createNormalOrder(UserContext.getUserId(), request));
    }

    @Operation(summary = "我的订单列表", description = "可按状态筛选：0 待支付 / 1 已支付 / 2 已取消 / 3 已关闭")
    @GetMapping
    public ApiResponse<PageResponse<OrderVO>> listOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(orderService.listUserOrders(UserContext.getUserId(), status, page, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderNo}")
    public ApiResponse<OrderVO> getByOrderNo(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.getByOrderNo(UserContext.getUserId(), orderNo));
    }

    @Operation(summary = "模拟支付", description = "PENDING → PAID")
    @PostMapping("/{orderNo}/pay")
    public ApiResponse<OrderVO> pay(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.pay(UserContext.getUserId(), orderNo));
    }

    @Operation(summary = "取消订单", description = "PENDING → CANCELLED，库存恢复")
    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<OrderVO> cancel(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.cancel(UserContext.getUserId(), orderNo));
    }
}
