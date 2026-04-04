package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface IShareLinkService {
    Result createForNote(Long noteId);
}
