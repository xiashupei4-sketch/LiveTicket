package com.liveticket.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lt_post_like")
public class PostLike {

    private Long id;
    private Long postId;
    private Long userId;
    private LocalDateTime createdAt;
}
