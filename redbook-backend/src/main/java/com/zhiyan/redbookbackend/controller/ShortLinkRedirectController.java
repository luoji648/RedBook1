package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.config.RedbookProperties;
import com.zhiyan.redbookbackend.entity.ShareLink;
import com.zhiyan.redbookbackend.mapper.ShareLinkMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
public class ShortLinkRedirectController {

    @Resource
    private ShareLinkMapper shareLinkMapper;
    @Resource
    private RedbookProperties redbookProperties;

    @GetMapping("/s/{code}")
    public void redirect(@PathVariable String code, HttpServletResponse resp) throws IOException {
        ShareLink link = shareLinkMapper.selectById(code);
        if (link == null) {
            resp.sendError(404);
            return;
        }
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())) {
            resp.sendError(410);
            return;
        }
        String target = redbookProperties.getFrontendNoteUrl().replace("{id}", String.valueOf(link.getNoteId()));
        resp.sendRedirect(target);
    }
}
