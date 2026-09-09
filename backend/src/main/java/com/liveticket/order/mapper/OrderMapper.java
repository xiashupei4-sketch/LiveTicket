package com.liveticket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liveticket.order.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 仅允许 PENDING → PAID
     */
    @Update("UPDATE lt_order SET status = 1, paid_at = NOW() "
            + "WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0")
    int markPaid(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    /**
     * 仅允许 PENDING → CANCELLED，同时清空 purchase_guard 允许重新购买
     */
    @Update("UPDATE lt_order SET status = 2, purchase_guard = NULL, cancelled_at = NOW() "
            + "WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0")
    int markCancelled(@Param("orderNo") String orderNo, @Param("userId") Long userId);
}
