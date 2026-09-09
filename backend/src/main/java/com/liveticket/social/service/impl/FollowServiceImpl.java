package com.liveticket.social.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liveticket.common.api.PageResponse;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.social.entity.Follow;
import com.liveticket.social.mapper.FollowMapper;
import com.liveticket.social.service.FollowService;
import com.liveticket.social.vo.FollowVO;
import com.liveticket.user.entity.User;
import com.liveticket.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    @Override
    public void follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(targetUserId);
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.FOLLOW_ALREADY_EXISTS);
        }
    }

    @Override
    public void unfollow(Long userId, Long targetUserId) {
        int affected = followMapper.deleteFollow(userId, targetUserId);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "follow relation not found");
        }
    }

    @Override
    public PageResponse<FollowVO> listFollowers(Long targetUserId, long page, long pageSize) {
        Page<Follow> mpPage = new Page<>(page, pageSize);
        Page<Follow> result = followMapper.selectPage(mpPage, Wrappers.<Follow>lambdaQuery()
                .eq(Follow::getFollowUserId, targetUserId)
                .orderByDesc(Follow::getCreatedAt));
        return toVOPage(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal(), false);
    }

    @Override
    public PageResponse<FollowVO> listFollowing(Long userId, long page, long pageSize) {
        Page<Follow> mpPage = new Page<>(page, pageSize);
        Page<Follow> result = followMapper.selectPage(mpPage, Wrappers.<Follow>lambdaQuery()
                .eq(Follow::getUserId, userId)
                .orderByDesc(Follow::getCreatedAt));
        return toVOPage(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal(), true);
    }

    private PageResponse<FollowVO> toVOPage(List<Follow> records, long page, long pageSize, long total,
                                            boolean followingMode) {
        List<Long> userIds = records.stream()
                .map(f -> followingMode ? f.getFollowUserId() : f.getUserId())
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        List<FollowVO> vos = records.stream().map(f -> {
            Long uid = followingMode ? f.getFollowUserId() : f.getUserId();
            User user = userMap.get(uid);
            FollowVO vo = new FollowVO();
            vo.setUserId(uid);
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
                vo.setAvatarUrl(user.getAvatarUrl());
            }
            vo.setFollowedAt(f.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
        return PageResponse.of(vos, page, pageSize, total);
    }
}
