package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface IProductFootprintService {

    Result record(Long productId);

    /**
     * 当前用户足迹，含商品标题/封面/价格。
     */
    Result my(long current, long size);
}
