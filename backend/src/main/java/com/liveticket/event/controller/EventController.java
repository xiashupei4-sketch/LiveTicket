package com.liveticket.event.controller;

import com.liveticket.common.api.ApiResponse;
import com.liveticket.common.api.PageResponse;
import com.liveticket.common.context.UserContext;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.event.cache.EventCacheService;
import com.liveticket.event.dto.EventQueryRequest;
import com.liveticket.event.service.EventService;
import com.liveticket.event.vo.EventDetailVO;
import com.liveticket.event.vo.EventVO;
import com.liveticket.geo.service.GeoService;
import com.liveticket.geo.vo.NearbyEventVO;
import com.liveticket.statistics.service.UvStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Events", description = "演出")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventCacheService eventCacheService;
    private final GeoService geoService;
    private final UvStatisticsService uvStatisticsService;

    @Operation(summary = "演出列表", description = "支持关键词、城市、分类筛选与分页")
    @GetMapping
    public ApiResponse<PageResponse<EventVO>> listEvents(@ModelAttribute EventQueryRequest request) {
        if (request.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "pageSize must be <= 100");
        }
        return ApiResponse.ok(eventService.listEvents(request));
    }

    @Operation(summary = "热门演出", description = "按热度倒序的在售演出")
    @GetMapping("/hot")
    public ApiResponse<List<EventVO>> hotEvents(@RequestParam(defaultValue = "10") int limit) {
        if (limit <= 0 || limit > 50) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "limit must be 1-50");
        }
        return ApiResponse.ok(eventService.listHotEvents(limit));
    }

    @Operation(summary = "城市演出", description = "按城市查询在售演出")
    @GetMapping("/city")
    public ApiResponse<PageResponse<EventVO>> listByCity(@RequestParam String cityCode,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long pageSize) {
        if (pageSize > 100) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "pageSize must be <= 100");
        }
        return ApiResponse.ok(eventService.listEventsByCity(cityCode, page, pageSize));
    }

    @Operation(summary = "附近演出", description = "Redis GEO，默认半径 10km、20 条")
    @GetMapping("/nearby")
    public ApiResponse<List<NearbyEventVO>> nearby(
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        if (radiusKm <= 0 || radiusKm > 100) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "radiusKm must be 0-100");
        }
        if (pageSize <= 0 || pageSize > 50) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "pageSize must be 1-50");
        }
        return ApiResponse.ok(geoService.listNearby(longitude, latitude, radiusKm, pageSize));
    }

    @Operation(summary = "演出详情", description = "Caffeine + Redis 多级缓存，成功访问记入 HyperLogLog UV")
    @GetMapping("/{eventId}")
    public ApiResponse<EventDetailVO> getEventDetail(@PathVariable Long eventId) {
        EventDetailVO detail = eventCacheService.getEventDetail(eventId);
        uvStatisticsService.recordView(eventId, UserContext.getUserId());
        return ApiResponse.ok(detail);
    }
}
