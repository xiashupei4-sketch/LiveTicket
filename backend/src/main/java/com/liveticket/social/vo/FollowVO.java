package com.liveticket.social.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime followedAt;
}
