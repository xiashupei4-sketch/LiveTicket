package com.liveticket.social.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostVO {

    private Long postId;
    private String content;
    private String imageUrl;
    private Integer likeCount;
    private LocalDateTime createdAt;

    private Long authorId;
    private String authorNickname;
    private String authorAvatarUrl;

    private Long eventId;
    private String eventTitle;
    private String eventCoverUrl;
}
