package com.liveticket.social.vo;

import lombok.Data;

import java.util.List;

/**
 * Feed 滚动分页响应：必须使用 maxTime + offset 继续向后翻页，禁止普通 page/pageSize
 */
@Data
public class FeedPageVO {

    private List<PostVO> records;

    /** 下一页起点：上一页最后一条的 score（createdAt 毫秒） */
    private Long nextMaxTime;

    /** 下一页偏移量：上一页末尾与 nextMaxTime 相同 score 的条数 */
    private Integer nextOffset;

    private boolean hasNext;

    private int pageSize;
}
