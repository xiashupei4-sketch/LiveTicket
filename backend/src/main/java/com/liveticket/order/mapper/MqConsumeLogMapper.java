package com.liveticket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liveticket.order.entity.MqConsumeLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface MqConsumeLogMapper extends BaseMapper<MqConsumeLog> {

    /**
     * 每次消费失败更新重试次数与最后错误
     */
    @Update("UPDATE lt_mq_consume_log SET retry_count = #{retryCount}, last_error = #{lastError} "
            + "WHERE message_id = #{messageId}")
    int updateRetry(@Param("messageId") String messageId,
                    @Param("retryCount") int retryCount,
                    @Param("lastError") String lastError);

    /**
     * 消费成功：在同一事务内将 consume_status 置为 SUCCESS
     */
    @Update("UPDATE lt_mq_consume_log SET consume_status = 1 WHERE message_id = #{messageId}")
    int markSuccess(@Param("messageId") String messageId);
}
