package com.liveticket.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private List<T> records;
    private long page;
    private long pageSize;
    private long total;
    private boolean hasNext;

    public static <T> PageResponse<T> of(List<T> records, long page, long pageSize, long total) {
        boolean hasNext = page * pageSize < total;
        return new PageResponse<>(records, page, pageSize, total, hasNext);
    }
}
