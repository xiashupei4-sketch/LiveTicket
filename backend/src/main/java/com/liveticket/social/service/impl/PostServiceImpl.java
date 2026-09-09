package com.liveticket.social.service.impl;

import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.social.dto.CreatePostRequest;
import com.liveticket.social.entity.Post;
import com.liveticket.social.entity.PostLike;
import com.liveticket.social.mapper.PostLikeMapper;
import com.liveticket.social.mapper.PostMapper;
import com.liveticket.social.service.FeedService;
import com.liveticket.social.service.PostService;
import com.liveticket.social.vo.PostVO;
import com.liveticket.user.entity.User;
import com.liveticket.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final UserMapper userMapper;
    private final EventMapper eventMapper;
    private final FeedService feedService;

    @Override
    public PostVO createPost(Long userId, CreatePostRequest request) {
        User author = userMapper.selectById(userId);
        if (author == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (request.getEventId() != null && eventMapper.selectById(request.getEventId()) == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setEventId(request.getEventId());
        post.setImageUrl(request.getImageUrl());
        post.setLikeCount(0);
        postMapper.insert(post);

        // Fan-out on Write：推送到所有粉丝的 Feed
        feedService.fanoutToFollowers(post);

        log.info("[POST] created postId={} userId={}", post.getId(), userId);
        return toVO(post, author, request.getEventId() == null ? null
                : eventMapper.selectById(request.getEventId()));
    }

    @Override
    public void like(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "post not found");
        }
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        try {
            postLikeMapper.insert(like);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return;
        }
        postMapper.incrementLike(postId);
    }

    @Override
    public void unlike(Long userId, Long postId) {
        int affected = postLikeMapper.deleteLike(postId, userId);
        if (affected == 0) {
            return;
        }
        postMapper.decrementLike(postId);
    }

    private PostVO toVO(Post post, User author, Event event) {
        PostVO vo = new PostVO();
        vo.setPostId(post.getId());
        vo.setContent(post.getContent());
        vo.setImageUrl(post.getImageUrl());
        vo.setLikeCount(post.getLikeCount());
        vo.setCreatedAt(post.getCreatedAt());
        vo.setAuthorId(post.getUserId());
        if (author != null) {
            vo.setAuthorNickname(author.getNickname());
            vo.setAuthorAvatarUrl(author.getAvatarUrl());
        }
        vo.setEventId(post.getEventId());
        if (event != null) {
            vo.setEventTitle(event.getTitle());
            vo.setEventCoverUrl(event.getCoverUrl());
        }
        return vo;
    }
}
