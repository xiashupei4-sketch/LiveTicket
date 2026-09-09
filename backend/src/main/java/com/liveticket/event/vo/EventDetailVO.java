package com.liveticket.event.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EventDetailVO extends EventVO {

    private String description;
}
