package com.liveticket.seckill.service;

import com.liveticket.seckill.vo.SeckillResultVO;

public interface SeckillService {

    SeckillResultVO executeSeckill(Long userId, Long ticketSkuId);

    SeckillResultVO getResult(Long userId, Long ticketSkuId);
}
