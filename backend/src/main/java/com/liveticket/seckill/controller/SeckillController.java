package com.liveticket.seckill.controller;

import com.liveticket.common.api.ApiResponse;
import com.liveticket.common.context.UserContext;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.seckill.service.SeckillService;
import com.liveticket.seckill.vo.SeckillResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Seckill", description = "限量抢票")
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @Operation(summary = "抢票", description = "Redis Lua 原子预扣，异步创建订单")
    @PostMapping("/{ticketSkuId}")
    public ApiResponse<SeckillResultVO> seckill(@PathVariable Long ticketSkuId) {
        return ApiResponse.ok(seckillService.executeSeckill(UserContext.getUserId(), ticketSkuId));
    }

    @Operation(summary = "抢票结果查询", description = "PROCESSING / SUCCESS / FAILED")
    @GetMapping("/{ticketSkuId}/result")
    public ResponseEntity<ApiResponse<SeckillResultVO>> getResult(@PathVariable Long ticketSkuId) {
        SeckillResultVO result = seckillService.getResult(UserContext.getUserId(), ticketSkuId);
        if (result != null && "FAILED".equals(result.getStatus())) {
            return ResponseEntity.status(ErrorCode.SECKILL_FAILED.getHttpStatus())
                    .body(ApiResponse.of(ErrorCode.SECKILL_FAILED.getCode(),
                            ErrorCode.SECKILL_FAILED.getMessage(), result));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
