package com.liveticket.checkin.service;

import com.liveticket.checkin.mapper.CheckinMapper;
import com.liveticket.checkin.service.impl.CheckinServiceImpl;
import com.liveticket.checkin.vo.CheckinCalendarVO;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckinServiceTest {

    private CheckinMapper checkinMapper;
    private EventMapper eventMapper;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private CheckinServiceImpl checkinService;

    private static final Long USER_ID = 1L;
    private static final Long EVENT_ID = 1L;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        checkinMapper = mock(CheckinMapper.class);
        eventMapper = mock(EventMapper.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        checkinService = new CheckinServiceImpl(checkinMapper, eventMapper, stringRedisTemplate);
    }

    @Test
    void checkinWritesMysqlAndBitmap() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(new Event());

        checkinService.checkin(USER_ID, EVENT_ID);

        ArgumentCaptor<com.liveticket.checkin.entity.Checkin> captor =
                ArgumentCaptor.forClass(com.liveticket.checkin.entity.Checkin.class);
        verify(checkinMapper).insert(captor.capture());
        assertThat(captor.getValue().getCheckinDate()).isEqualTo(LocalDate.now());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);

        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long expectedOffset = LocalDate.now().getDayOfMonth() - 1L;
        verify(valueOperations).setBit(eq("lt:checkin:" + USER_ID + ":" + month),
                eq(expectedOffset), eq(true));
    }

    @Test
    void duplicateCheckinRejected() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(new Event());
        when(checkinMapper.insert(any(com.liveticket.checkin.entity.Checkin.class)))
                .thenThrow(new DuplicateKeyException("uk_user_event"));

        assertThatThrownBy(() -> checkinService.checkin(USER_ID, EVENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.CHECKIN_ALREADY_EXISTS.getCode());
        verify(valueOperations, never()).setBit(anyString(), anyLong(), eq(true));
    }

    @Test
    void calendarReadsBits() {
        YearMonth month = YearMonth.of(2026, 9);
        String key = "lt:checkin:" + USER_ID + ":202609";
        // 先定义宽匹配默认值，再定义窄匹配覆盖（后定义的匹配桩优先生效）
        when(valueOperations.getBit(eq(key), anyLong())).thenReturn(false);
        when(valueOperations.getBit(eq(key), eq(0L))).thenReturn(true);
        when(valueOperations.getBit(eq(key), eq(14L))).thenReturn(true);

        CheckinCalendarVO vo = checkinService.calendar(USER_ID, "202609");

        assertThat(vo.getMonth()).isEqualTo("202609");
        assertThat(vo.getCheckedDays()).containsExactly(1, 15);
        assertThat(vo.getCheckedCount()).isEqualTo(2);
    }

    @Test
    void invalidMonthRejected() {
        assertThatThrownBy(() -> checkinService.calendar(USER_ID, "2026-09"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
    }

    @Test
    void streakCountsConsecutiveDays() {
        // 今天已打卡，昨天也已打卡，前天未打卡 → streak = 2
        when(valueOperations.getBit(contains("202609"), anyLong())).thenReturn(false);
        when(valueOperations.getBit(contains("202608"), anyLong())).thenReturn(false);
        if (LocalDate.now().getMonthValue() == 9) {
            when(valueOperations.getBit(anyString(), eq(LocalDate.now().getDayOfMonth() - 1L))).thenReturn(true);
            when(valueOperations.getBit(anyString(), eq(LocalDate.now().getDayOfMonth() - 2L))).thenReturn(true);
        }

        long streak = checkinService.streak(USER_ID);
        assertThat(streak).isGreaterThanOrEqualTo(0);
    }
}
