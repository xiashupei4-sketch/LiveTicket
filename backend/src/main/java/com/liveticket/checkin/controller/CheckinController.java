package com.liveticket.checkin.controller;

import com.liveticket.checkin.service.CheckinService;
import com.liveticket.checkin.vo.CheckinCalendarVO;
import com.liveticket.common.api.ApiResponse;
import com.liveticket.common.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Tag(name = "Checkin", description = "观演打卡（Bitmap）")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @Operation(summary = "观演打卡", description = "MySQL lt_checkin + Redis Bitmap 同时写入")
    @PostMapping("/events/{eventId}/checkin")
    public ApiResponse<Void> checkin(@PathVariable Long eventId) {
        checkinService.checkin(UserContext.getUserId(), eventId);
        return ApiResponse.ok();
    }

    @Operation(summary = "打卡日历", description = "month 格式 yyyyMM，默认当月")
    @GetMapping("/checkins/calendar")
    public ApiResponse<CheckinCalendarVO> calendar(
            @RequestParam(required = false) String month) {
        String yyyyMM = month == null ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) : month;
        return ApiResponse.ok(checkinService.calendar(UserContext.getUserId(), yyyyMM));
    }

    @Operation(summary = "连续打卡天数")
    @GetMapping("/checkins/streak")
    public ApiResponse<Long> streak() {
        return ApiResponse.ok(checkinService.streak(UserContext.getUserId()));
    }
}
