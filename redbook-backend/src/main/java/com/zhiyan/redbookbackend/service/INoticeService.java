package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

import java.util.Map;

public interface INoticeService {

    Result interactionPage(long current, long size, String category);

    /**
     * 三类互动通知未读数（未点进过该分类前，未读=该分类全部条数）。
     * key：likeCollect、follow、comment
     */
    Map<String, Long> getNoticeUnreadCounts();

    /** 进入某类通知页后标记已读；category 同 interaction 接口：like_collect|follow|comment */
    Result markNoticeCategoryRead(String category);
}
