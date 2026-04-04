package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface IOrderService {
    Result createFromCart();

    Result my(long current, long size);
}
