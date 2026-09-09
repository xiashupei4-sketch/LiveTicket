package com.liveticket.social.service;

import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.social.dto.CreatePostRequest;
import com.liveticket.social.entity.Post;
import com.liveticket.social.mapper.PostLikeMapper;
import com.liveticket.social.mapper.PostMapper;
import com.liveticket.social.service.impl.PostServiceImpl;
import com.liveticket.social.vo.PostVO;
import com.liveticket.user.entity.User;
import com.liveticket.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceTest {

    private PostMapper postMapper;
    private PostLikeMapper postLikeMapper;
    private UserMapper userMapper;
    private EventMapper eventMapper;
    private FeedService feedService;
    private PostServiceImpl postService;

    private static final Long USER_ID = 2L;
    private static final Long POST_ID = 9L;
    private static final Long EVENT_ID = 1L;

    @BeforeEach
    void setUp() {
        postMapper = mock(PostMapper.class);
        postLikeMapper = mock(PostLikeMapper.class);
        userMapper = mock(UserMapper.class);
        eventMapper = mock(EventMapper.class);
        feedService = mock(FeedService.class);
        postService = new PostServiceImpl(postMapper, postLikeMapper, userMapper, eventMapper, feedService);
    }

    private User author() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("demo02");
        user.setNickname("Mina");
        return user;
    }

    private CreatePostRequest request() {
        CreatePostRequest request = new CreatePostRequest();
        request.setContent("今晚现场氛围太好了。");
        request.setEventId(EVENT_ID);
        return request;
    }

    @Test
    void createPostInsertsAndFansOut() {
        when(userMapper.selectById(USER_ID)).thenReturn(author());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(new Event());
        when(postMapper.insert(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(POST_ID);
            return 1;
        });

        PostVO vo = postService.createPost(USER_ID, request());

        assertThat(vo.getPostId()).isEqualTo(POST_ID);
        assertThat(vo.getAuthorNickname()).isEqualTo("Mina");
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        verify(feedService).fanoutToFollowers(any(Post.class));
    }

    @Test
    void createPostWithUnknownEventRejected() {
        when(userMapper.selectById(USER_ID)).thenReturn(author());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(null);

        assertThatThrownBy(() -> postService.createPost(USER_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode());
        verify(feedService, never()).fanoutToFollowers(any());
    }

    @Test
    void likeIncrementsOnceAndDuplicateIsNoOp() {
        Post post = new Post();
        post.setId(POST_ID);
        post.setLikeCount(1);
        when(postMapper.selectById(POST_ID)).thenReturn(post);

        postService.like(USER_ID, POST_ID);
        verify(postMapper).incrementLike(POST_ID);

        when(postLikeMapper.insert(any(com.liveticket.social.entity.PostLike.class)))
                .thenThrow(new DuplicateKeyException("uk_post_user"));
        postService.like(USER_ID, POST_ID);
        verify(postMapper, org.mockito.Mockito.times(1)).incrementLike(POST_ID);
    }

    @Test
    void unlikeDecrementsOnlyWhenLikeExists() {
        when(postLikeMapper.deleteLike(POST_ID, USER_ID)).thenReturn(1).thenReturn(0);

        postService.unlike(USER_ID, POST_ID);
        verify(postMapper).decrementLike(POST_ID);

        postService.unlike(USER_ID, POST_ID);
        verify(postMapper, org.mockito.Mockito.times(1)).decrementLike(POST_ID);
    }
}
