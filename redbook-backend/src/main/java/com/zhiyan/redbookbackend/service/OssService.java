package com.zhiyan.redbookbackend.service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.zhiyan.redbookbackend.config.AliyunOssProperties;
import com.zhiyan.redbookbackend.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OssService {

    private final AliyunOssProperties ossProperties;

    public Result presignedPut(String ext) {
        if (ossProperties.getAccessKeyId() == null || ossProperties.getAccessKeyId().startsWith("your-")) {
            return Result.fail("请在配置中填写有效的 aliyun.oss.access-key-id / secret");
        }
        String suffix = (ext == null || ext.isBlank()) ? "" : ext.startsWith(".") ? ext : "." + ext;
        String objectKey = "uploads/" + UUID.randomUUID().toString().replace("-", "") + suffix;
        try {
            OSS oss = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret());
            Date exp = new Date(System.currentTimeMillis() + 3600_000L);
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(ossProperties.getBucketName(), objectKey, HttpMethod.PUT);
            req.setExpiration(exp);
            URL url = oss.generatePresignedUrl(req);
            oss.shutdown();
            Map<String, String> m = new HashMap<>();
            m.put("uploadUrl", url.toString());
            m.put("objectKey", objectKey);
            m.put("publicUrl", trimSlash(ossProperties.getUrlPrefix()) + "/" + objectKey);
            return Result.ok(m);
        } catch (Exception e) {
            return Result.fail("生成上传地址失败: " + e.getMessage());
        }
    }

    private static String trimSlash(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
