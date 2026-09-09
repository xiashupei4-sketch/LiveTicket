package com.liveticket.checkin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liveticket.checkin.entity.Checkin;
import com.liveticket.checkin.mapper.CheckinMapper;
import com.liveticket.checkin.service.CheckinService;
import com.liveticket.checkin.vo.CheckinCalendarVO;
import com.liveticket.common.constant.RedisKeys;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final CheckinMapper checkinMapper;
    private final EventMapper eventMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void checkin(Long userId, Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        LocalDate today = LocalDate.now();
        Checkin checkin = new Checkin();
        checkin.setUserId(userId);
        checkin.setEventId(eventId);
        checkin.setCheckinDate(today);
        try {
            checkinMapper.insert(checkin);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CHECKIN_ALREADY_EXISTS);
        }

        String key = RedisKeys.checkin(userId, today.format(MONTH_FORMAT));
        long offset = today.getDayOfMonth() - 1L;
        stringRedisTemplate.opsForValue().setBit(key, offset, true);
        log.info("[CHECKIN] userId={} eventId={} date={} bitOffset={}", userId, eventId, today, offset);
    }

    @Override
    public CheckinCalendarVO calendar(Long userId, String yyyyMM) {
        YearMonth month;
        try {
            month = YearMonth.parse(yyyyMM, MONTH_FORMAT);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "month must be yyyyMM");
        }

        String key = RedisKeys.checkin(userId, yyyyMM);
        List<Integer> checkedDays = new ArrayList<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(key, day - 1L);
            if (Boolean.TRUE.equals(bit)) {
                checkedDays.add(day);
            }
        }
        return new CheckinCalendarVO(yyyyMM, checkedDays, checkedDays.size());
    }

    @Override
    public long streak(Long userId) {
        LocalDate cursor = LocalDate.now();
        if (!bitAt(userId, cursor)) {
            // 今天未打卡时，允许从昨天开始计数连续打卡
            cursor = cursor.minusDays(1);
            if (!bitAt(userId, cursor)) {
                return 0;
            }
        }
        long streak = 0;
        for (int i = 0; i < 3650; i++) {
            if (!bitAt(userId, cursor)) {
                break;
            }
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private boolean bitAt(Long userId, LocalDate date) {
        String key = RedisKeys.checkin(userId, date.format(MONTH_FORMAT));
        Boolean bit = stringRedisTemplate.opsForValue().getBit(key, date.getDayOfMonth() - 1L);
        return Boolean.TRUE.equals(bit);
    }
}
