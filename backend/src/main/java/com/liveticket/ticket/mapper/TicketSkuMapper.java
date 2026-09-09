package com.liveticket.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liveticket.ticket.entity.TicketSku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface TicketSkuMapper extends BaseMapper<TicketSku> {

    /**
     * 条件扣减库存：available_stock >= quantity 才会生效，affectedRows == 1 表示扣减成功
     */
    @Update("UPDATE lt_ticket_sku SET available_stock = available_stock - #{quantity} "
            + "WHERE id = #{id} AND available_stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 库存回滚（订单取消）
     */
    @Update("UPDATE lt_ticket_sku SET available_stock = available_stock + #{quantity} WHERE id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
