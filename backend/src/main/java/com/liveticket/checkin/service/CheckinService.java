package com.liveticket.checkin.service;

import com.liveticket.checkin.vo.CheckinCalendarVO;

public interface CheckinService {

    /**
     * 观演打卡：INSERT lt_checkin + SETBIT lt:checkin:{userId}:{yyyyMM}（offset = dayOfMonth - 1）
     */
    void checkin(Long userId, Long eventId);

    CheckinCalendarVO calendar(Long userId, String yyyyMM);

    long streak(Long userId);
}
