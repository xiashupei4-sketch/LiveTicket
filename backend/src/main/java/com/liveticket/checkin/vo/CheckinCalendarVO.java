package com.liveticket.checkin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CheckinCalendarVO {

    private String month;
    private List<Integer> checkedDays;
    private int checkedCount;
}
