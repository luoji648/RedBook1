package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface IWalletRechargeService {

    Result createPagePay(long amountCent);
}
