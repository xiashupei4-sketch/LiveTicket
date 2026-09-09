package com.liveticket.social.controller;

import com.liveticket.common.api.ApiResponse;
import com.liveticket.common.context.UserContext;
import com.liveticket.social.dto.CreatePostRequest;
import com.liveticket.social.service.PostService;
import com.liveticket.social.vo.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Social", description = "动态与点赞")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "发布动态", description = "Fan-out on Write 推送粉丝 Feed")
    @PostMapping
    public ApiResponse<PostVO> createPost(@Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.ok(postService.createPost(UserContext.getUserId(), request));
    }

    @Operation(summary = "点赞")
    @PostMapping("/{postId}/like")
    public ApiResponse<Void> like(@PathVariable Long postId) {
        postService.like(UserContext.getUserId(), postId);
        return ApiResponse.ok();
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/{postId}/like")
    public ApiResponse<Void> unlike(@PathVariable Long postId) {
        postService.unlike(UserContext.getUserId(), postId);
        return ApiResponse.ok();
    }
}
