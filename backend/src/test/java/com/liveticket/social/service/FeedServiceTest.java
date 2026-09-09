package com.liveticket.social.service;

import com.liveticket.common.constant.RedisKeys;
import com.liveticket.social.entity.Follow;
import com.liveticket.social.entity.Post;
import com.liveticket.social.mapper.FollowMapper;
import com.liveticket.social.mapper.PostMapper;
import com.liveticket.social.service.impl.FeedServiceImpl;
import com.liveticket.social.vo.FeedPageVO;
import com.liveticket.social.vo.PostVO;
import com.liveticket.user.entity.User;
import com.liveticket.user.mapper.UserMapper;
import com.liveticket.event.mapper.EventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedServiceTest {

    private StringRedisTemplate stringRedisTemplate;
    private ZSetOperations<String, String> zSetOperations;
    private FollowMapper followMapper;
    private PostMapper postMapper;
    private UserMapper userMapper;
    private FeedServiceImpl feedService;

    private static final Long AUTHOR_ID = 2L;
    private static final Long FOLLOWER_A = 1L;
    private static final Long FOLLOWER_B = 3L;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        zSetOperations = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        followMapper = mock(FollowMapper.class);
        postMapper = mock(PostMapper.class);
        userMapper = mock(UserMapper.class);
        feedService = new FeedServiceImpl(stringRedisTemplate, followMapper, postMapper, userMapper,
                mock(EventMapper.class));
    }

    private Post publishedPost(long postId, LocalDateTime createdAt) {
        Post post = new Post();
        post.setId(postId);
        post.setUserId(AUTHOR_ID);
        post.setContent("今晚现场氛围太好了。");
        post.setLikeCount(0);
        post.setCreatedAt(createdAt);
        return post;
    }

    @Test
    void fanoutWritesToEveryFollowerFeed() {
        Follow f1 = new Follow();
        f1.setUserId(FOLLOWER_A);
        f1.setFollowUserId(AUTHOR_ID);
        Follow f2 = new Follow();
        f2.setUserId(FOLLOWER_B);
        f2.setFollowUserId(AUTHOR_ID);
        when(followMapper.selectList(any())).thenReturn(List.of(f1, f2));

        Post post = publishedPost(9L, LocalDateTime.now());
        feedService.fanoutToFollowers(post);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(zSetOperations, times(2)).add(keyCaptor.capture(), eq("9"), anyDouble());
        assertThat(keyCaptor.getAllValues())
                .containsExactly(RedisKeys.feed(FOLLOWER_A), RedisKeys.feed(FOLLOWER_B));
    }

    @Test
    void scrollPaginationHasNoOverlap() {
        User author = new User();
        author.setId(AUTHOR_ID);
        author.setNickname("Mina");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(author));

        long now = System.currentTimeMillis();
        Post p3 = publishedPost(3L, LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now + 2),
                java.time.ZoneId.systemDefault()));
        Post p2 = publishedPost(2L, LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now + 1),
                java.time.ZoneId.systemDefault()));
        Post p1 = publishedPost(1L, LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now),
                java.time.ZoneId.systemDefault()));
        when(postMapper.selectBatchIds(any())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return ids.stream().map(id -> switch (id.intValue()) {
                case 3 -> p3;
                case 2 -> p2;
                default -> p1;
            }).toList();
        });

        ZSetOperations.TypedTuple<String> t3 = tuple("3", now + 2);
        ZSetOperations.TypedTuple<String> t2 = tuple("2", now + 1);
        ZSetOperations.TypedTuple<String> t1 = tuple("1", now);
        when(zSetOperations.reverseRangeByScoreWithScores(eq(RedisKeys.feed(FOLLOWER_A)), anyDouble(), anyDouble(),
                eq(0L), eq(3L)))
                .thenReturn(new java.util.LinkedHashSet<>(List.of(t3, t2, t1)));
        when(zSetOperations.reverseRangeByScoreWithScores(eq(RedisKeys.feed(FOLLOWER_A)), anyDouble(), anyDouble(),
                eq(1L), eq(3L))).thenReturn(new java.util.LinkedHashSet<>(List.of(t1)));

        FeedPageVO page1 = feedService.readFeed(FOLLOWER_A, null, 0, 2);
        assertThat(page1.getRecords()).extracting(PostVO::getPostId).containsExactly(3L, 2L);
        assertThat(page1.isHasNext()).isTrue();
        assertThat(page1.getNextMaxTime()).isEqualTo(now + 1);

        FeedPageVO page2 = feedService.readFeed(FOLLOWER_A, page1.getNextMaxTime(), page1.getNextOffset(), 2);
        assertThat(page2.getRecords()).extracting(PostVO::getPostId).containsExactly(1L);
        assertThat(page2.isHasNext()).isFalse();

        // 两页无重复
        List<Long> page1Ids = page1.getRecords().stream().map(PostVO::getPostId).toList();
        List<Long> page2Ids = page2.getRecords().stream().map(PostVO::getPostId).toList();
        assertThat(page1Ids).doesNotContainAnyElementsOf(page2Ids);
    }

    @Test
    void emptyFeedReturnsEmptyPage() {
        when(zSetOperations.reverseRangeByScoreWithScores(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong()))
                .thenReturn(Set.of());

        FeedPageVO page = feedService.readFeed(FOLLOWER_A, null, 0, 10);

        assertThat(page.getRecords()).isEmpty();
        assertThat(page.isHasNext()).isFalse();
    }

    @SuppressWarnings("unchecked")
    private ZSetOperations.TypedTuple<String> tuple(String value, double score) {
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn(value);
        when(tuple.getScore()).thenReturn(score);
        return tuple;
    }
}
