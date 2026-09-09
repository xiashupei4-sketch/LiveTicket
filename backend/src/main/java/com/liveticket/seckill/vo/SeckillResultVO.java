package com.liveticket.seckill.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeckillResultVO {

    private String status;
    private String orderNo;
    private String reason;
}
