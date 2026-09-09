package com.liveticket.admin.controller;

import com.liveticket.admin.service.AdminService;
import com.liveticket.common.api.ApiResponse;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.geo.service.GeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Admin", description = "管理演示接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final AdminService adminService;
    private final GeoService geoService;

    @Value("${app.admin-token}")
    private String adminToken;

    @Operation(summary = "秒杀库存初始化", description = "Header: X-Admin-Token。从 MySQL 读取 available_stock 写入 Redis，并清空已抢用户集合")
    @PostMapping("/seckill/init/{ticketSkuId}")
    public ApiResponse<Map<String, Long>> initSeckill(@PathVariable Long ticketSkuId,
                                                      @RequestHeader(value = ADMIN_TOKEN_HEADER, required = false)
                                                      String token) {
        checkAdminToken(token);
        long stock = adminService.initSeckillStock(ticketSkuId);
        return ApiResponse.ok(Map.of("ticketSkuId", ticketSkuId, "stock", stock));
    }

    @Operation(summary = "GEO 缓存重建", description = "Header: X-Admin-Token。删除在售城市 Key 后按 city_code 重新 GEOADD")
    @PostMapping("/cache/geo/rebuild")
    public ApiResponse<Map<String, Object>> rebuildGeo(
            @RequestHeader(value = ADMIN_TOKEN_HEADER, required = false) String token) {
        checkAdminToken(token);
        geoService.rebuildGeoCache();
        return ApiResponse.ok(Map.of("status", "REBUILT"));
    }

    private void checkAdminToken(String token) {
        if (!StringUtils.hasText(token) || !adminToken.equals(token)) {
            throw new BusinessException(ErrorCode.ADMIN_TOKEN_INVALID);
        }
    }
}
