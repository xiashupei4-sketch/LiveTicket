package com.liveticket.social.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liveticket.common.constant.RedisKeys;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.social.entity.Follow;
import com.liveticket.social.entity.Post;
import com.liveticket.social.mapper.FollowMapper;
import com.liveticket.social.mapper.PostMapper;
import com.liveticket.social.service.FeedService;
import com.liveticket.social.vo.FeedPageVO;
import com.liveticket.social.vo.PostVO;
import com.liveticket.user.entity.User;
import com.liveticket.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final StringRedisTemplate stringRedisTemplate;
    private final FollowMapper followMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final EventMapper eventMapper;

    @Override
    public void fanoutToFollowers(Post post) {
        double score = toMillis(post.getCreatedAt());
        Long postId = post.getId();

        // 粉丝列表（动态发布者被谁关注）
        List<Long> followerIds = followMapper.selectList(Wrappers.<Follow>lambdaQuery()
                        .eq(Follow::getFollowUserId, post.getUserId()))
                .stream()
                .map(Follow::getUserId)
                .collect(Collectors.toList());

        for (Long followerId : followerIds) {
            stringRedisTemplate.opsForZSet().add(RedisKeys.feed(followerId), String.valueOf(postId), score);
        }
        log.info("[FEED] fanout postId={} score={} followers={}", postId, (long) score, followerIds.size());
    }

    @Override
    public FeedPageVO readFeed(Long userId, Long maxTime, int offset, int pageSize) {
        String feedKey = RedisKeys.feed(userId);
        double max = maxTime == null ? System.currentTimeMillis() : maxTime.doubleValue();

        // 多取一条判断 hasNext（等价 ZREVRANGEBYSCORE key maxTime -inf LIMIT offset pageSize+1，含 score）
        // Spring Data 双参重载参数顺序为 (min, max)
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(feedKey, Double.NEGATIVE_INFINITY, max, offset, pageSize + 1);

        FeedPageVO page = new FeedPageVO();
        page.setPageSize(pageSize);
        if (tuples == null || tuples.isEmpty()) {
            page.setRecords(Collections.emptyList());
            page.setNextMaxTime(maxTime);
            page.setNextOffset(offset);
            page.setHasNext(false);
            return page;
        }

        boolean hasNext = tuples.size() > pageSize;
        List<ZSetOperations.TypedTuple<String>> items = tuples.stream().limit(pageSize).collect(Collectors.toList());
        List<Long> postIds = items.stream()
                .map(t -> Long.parseLong(t.getValue()))
                .collect(Collectors.toList());
        Map<Long, Double> scoreMap = items.stream()
                .collect(Collectors.toMap(t -> Long.parseLong(t.getValue()), ZSetOperations.TypedTuple::getScore,
                        (a, b) -> a, LinkedHashMap::new));

        Map<Long, Post> postMap = postMapper.selectBatchIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p));
        List<Long> authorIds = postMap.values().stream().map(Post::getUserId).distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = authorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(authorIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
        List<Long> eventIds = postMap.values().stream().map(Post::getEventId)
                .filter(eid -> eid != null).distinct().collect(Collectors.toList());
        Map<Long, Event> eventMap = eventIds.isEmpty() ? Map.of()
                : eventMapper.selectBatchIds(eventIds).stream()
                        .collect(Collectors.toMap(Event::getId, e -> e));

        List<PostVO> records = postIds.stream()
                .map(postMap::get)
                .filter(p -> p != null)
                .map(post -> {
                    PostVO vo = new PostVO();
                    vo.setPostId(post.getId());
                    vo.setContent(post.getContent());
                    vo.setImageUrl(post.getImageUrl());
                    vo.setLikeCount(post.getLikeCount());
                    vo.setCreatedAt(post.getCreatedAt());
                    vo.setAuthorId(post.getUserId());
                    User author = userMap.get(post.getUserId());
                    if (author != null) {
                        vo.setAuthorNickname(author.getNickname());
                        vo.setAuthorAvatarUrl(author.getAvatarUrl());
                    }
                    vo.setEventId(post.getEventId());
                    Event event = post.getEventId() == null ? null : eventMap.get(post.getEventId());
                    if (event != null) {
                        vo.setEventTitle(event.getTitle());
                        vo.setEventCoverUrl(event.getCoverUrl());
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        page.setRecords(records);
        page.setHasNext(hasNext);
        if (!records.isEmpty()) {
            PostVO last = records.get(records.size() - 1);
            double lastScore = scoreMap.getOrDefault(last.getPostId(),
                    last.getCreatedAt() == null ? 0 : toMillis(last.getCreatedAt()));
            page.setNextMaxTime((long) lastScore);
            int trailingSameScore = 0;
            for (int i = records.size() - 1; i >= 0; i--) {
                PostVO vo = records.get(i);
                double s = scoreMap.getOrDefault(vo.getPostId(),
                        vo.getCreatedAt() == null ? 0 : toMillis(vo.getCreatedAt()));
                if (s == lastScore) {
                    trailingSameScore++;
                } else {
                    break;
                }
            }
            page.setNextOffset(offset + trailingSameScore);
        } else {
            page.setNextMaxTime(maxTime);
            page.setNextOffset(offset);
        }
        return page;
    }

    private double toMillis(LocalDateTime time) {
        if (time == null) {
            return System.currentTimeMillis();
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
