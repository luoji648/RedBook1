package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface INoticeService {

    Result interactionPage(long current, long size, String category);
}
