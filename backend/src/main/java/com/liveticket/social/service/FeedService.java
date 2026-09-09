package com.liveticket.social.service;

import com.liveticket.social.entity.Post;
import com.liveticket.social.vo.FeedPageVO;

public interface FeedService {

    /**
     * Fan-out on Write：发布动态后 ZADD 到每个粉丝的 lt:feed:{followerId}
     */
    void fanoutToFollowers(Post post);

    /**
     * 滚动分页读取 Feed：ZREVRANGEBYSCORE key maxTime -inf LIMIT offset pageSize
     */
    FeedPageVO readFeed(Long userId, Long maxTime, int offset, int pageSize);
}
