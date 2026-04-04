package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface ICartService {
    Result add(Long productId, int quantity);

    Result my();

    Result updateQty(Long cartId, int quantity);

    Result remove(Long cartId);
}
