package com.liveticket.statistics.controller;

import com.liveticket.common.api.ApiResponse;
import com.liveticket.statistics.service.UvStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Statistics", description = "UV 统计（HyperLogLog）")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final UvStatisticsService uvStatisticsService;

    @Operation(summary = "演出当日 UV", description = "date 格式 yyyy-MM-dd，默认当天")
    @GetMapping("/events/{eventId}/uv")
    public ApiResponse<Map<String, Object>> uv(@PathVariable Long eventId,
                                               @RequestParam(required = false) String date) {
        String yyyyMMdd = date == null
                ? java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                : date.replace("-", "");
        long uv = uvStatisticsService.getUv(eventId, yyyyMMdd);
        return ApiResponse.ok(Map.of("eventId", eventId, "date", yyyyMMdd, "uv", uv));
    }
}
