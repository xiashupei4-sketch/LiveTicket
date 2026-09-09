package com.liveticket.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("lt_checkin")
public class Checkin {

    private Long id;
    private Long userId;
    private Long eventId;
    private LocalDate checkinDate;
    private LocalDateTime createdAt;
}
