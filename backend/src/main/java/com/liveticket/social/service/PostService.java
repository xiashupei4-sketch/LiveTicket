package com.liveticket.social.service;

import com.liveticket.social.dto.CreatePostRequest;
import com.liveticket.social.vo.PostVO;

public interface PostService {

    PostVO createPost(Long userId, CreatePostRequest request);

    void like(Long userId, Long postId);

    void unlike(Long userId, Long postId);
}
