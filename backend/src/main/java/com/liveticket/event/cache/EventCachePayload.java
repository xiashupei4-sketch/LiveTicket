package com.liveticket.event.cache;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.liveticket.event.vo.EventDetailVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Redis 演出详情缓存值结构：
 * {"data": {...演出详情...}, "logicalExpireAt": "yyyy-MM-dd'T'HH:mm:ss"}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventCachePayload {

    private EventDetailVO data;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime logicalExpireAt;
}
