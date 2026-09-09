package com.liveticket.social.controller;

import com.liveticket.common.api.ApiResponse;
import com.liveticket.common.context.UserContext;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.social.service.FeedService;
import com.liveticket.social.vo.FeedPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Social", description = "Feed 流")
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "我的 Feed", description = "滚动分页：首次不带 maxTime/offset，之后传上页返回的 nextMaxTime/nextOffset")
    @GetMapping
    public ApiResponse<FeedPageVO> feed(
            @RequestParam(required = false) Long maxTime,
            @RequestParam(required = false) Integer offset,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (pageSize <= 0 || pageSize > 50) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "pageSize must be 1-50");
        }
        int safeOffset = offset == null ? 0 : offset;
        if (safeOffset < 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "offset must be >= 0");
        }
        return ApiResponse.ok(feedService.readFeed(UserContext.getUserId(), maxTime, safeOffset, pageSize));
    }
}
