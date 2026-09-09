package com.liveticket.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lt_follow")
public class Follow {

    private Long id;
    private Long userId;
    private Long followUserId;
    private LocalDateTime createdAt;
}
