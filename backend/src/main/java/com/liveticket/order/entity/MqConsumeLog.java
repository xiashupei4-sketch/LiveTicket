package com.liveticket.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lt_mq_consume_log")
public class MqConsumeLog {

    /** 0 处理中 / 1 成功 / 2 最终失败 */
    public static final int STATUS_PROCESSING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;

    private Long id;
    private String messageId;
    private String businessType;
    private String businessId;
    private Integer consumeStatus;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
