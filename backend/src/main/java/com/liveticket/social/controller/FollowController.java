package com.liveticket.social.controller;

import com.liveticket.common.api.ApiResponse;
import com.liveticket.common.api.PageResponse;
import com.liveticket.common.context.UserContext;
import com.liveticket.social.service.FollowService;
import com.liveticket.social.vo.FollowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Social", description = "关注关系")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "关注用户")
    @PostMapping("/{targetUserId}/follow")
    public ApiResponse<Void> follow(@PathVariable Long targetUserId) {
        followService.follow(UserContext.getUserId(), targetUserId);
        return ApiResponse.ok();
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/{targetUserId}/follow")
    public ApiResponse<Void> unfollow(@PathVariable Long targetUserId) {
        followService.unfollow(UserContext.getUserId(), targetUserId);
        return ApiResponse.ok();
    }

    @Operation(summary = "粉丝列表")
    @GetMapping("/{targetUserId}/followers")
    public ApiResponse<PageResponse<FollowVO>> followers(
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(followService.listFollowers(targetUserId, page, pageSize));
    }

    @Operation(summary = "关注列表")
    @GetMapping("/{userId}/following")
    public ApiResponse<PageResponse<FollowVO>> following(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(followService.listFollowing(userId, page, pageSize));
    }
}
