package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.service.IShareLinkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "分享短链")
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {

    private final IShareLinkService shareLinkService;

    @PostMapping("/note/{noteId}")
    public Result create(@PathVariable Long noteId) {
        return shareLinkService.createForNote(noteId);
    }
}
