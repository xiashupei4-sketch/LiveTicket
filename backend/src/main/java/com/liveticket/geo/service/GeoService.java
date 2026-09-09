package com.liveticket.geo.service;

import com.liveticket.geo.vo.NearbyEventVO;

import java.util.List;

public interface GeoService {

    /**
     * 重建 GEO 缓存：删除在售城市 Key 后按 city_code GEOADD 在售演出
     */
    void rebuildGeoCache();

    /**
     * 附近演出：合并各城市 GEO Key 的 GEORADIUS 结果，按距离升序
     */
    List<NearbyEventVO> listNearby(double longitude, double latitude, double radiusKm, int pageSize);
}
