package com.liveticket.social.service;

import com.liveticket.common.api.PageResponse;
import com.liveticket.social.vo.FollowVO;

public interface FollowService {

    void follow(Long userId, Long targetUserId);

    void unfollow(Long userId, Long targetUserId);

    PageResponse<FollowVO> listFollowers(Long targetUserId, long page, long pageSize);

    PageResponse<FollowVO> listFollowing(Long userId, long page, long pageSize);
}
