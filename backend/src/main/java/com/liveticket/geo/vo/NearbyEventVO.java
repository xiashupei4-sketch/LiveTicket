package com.liveticket.geo.vo;

import com.liveticket.event.vo.EventVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NearbyEventVO extends EventVO {

    private double distanceKm;
}
