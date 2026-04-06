package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "OSS 上传")
@RestController
@RequestMapping("/oss")
@RequiredArgsConstructor
public class OssController {

    private final OssService ossService;

    @Operation(summary = "获取 PUT 预签名上传地址")
    @PostMapping("/presign")
    public Result presign(
            @RequestParam(value = "ext", required = false) String ext,
            @RequestParam(value = "contentType", required = false) String contentType) {
        return ossService.presignedPut(ext, contentType);
    }

    @Operation(summary = "经服务端转存 OSS（适合头像等，避免浏览器直连 OSS 的跨域问题）")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result upload(@RequestPart("file") MultipartFile file) {
        return ossService.uploadViaApi(file);
    }
}
