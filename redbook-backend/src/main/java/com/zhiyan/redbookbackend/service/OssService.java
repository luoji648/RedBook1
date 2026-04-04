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

    /** contentType 须与浏览器 PUT 请求的 Content-Type 一致，否则 OSS 返回 403。 */
    public Result presignedPut(String ext, String contentType) {
        if (ossProperties.getAccessKeyId() == null || ossProperties.getAccessKeyId().startsWith("your-")) {
            return Result.fail("请在配置中填写有效的 aliyun.oss.access-key-id / secret");
        }
        String suffix = (ext == null || ext.isBlank()) ? "" : ext.startsWith(".") ? ext : "." + ext;
        String objectKey = "uploads/" + UUID.randomUUID().toString().replace("-", "") + suffix;
        String ct = safeContentType(contentType);
        try {
            OSS oss = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret());
            Date exp = new Date(System.currentTimeMillis() + 3600_000L);
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(ossProperties.getBucketName(), objectKey, HttpMethod.PUT);
            req.setExpiration(exp);
            req.setContentType(ct);
            if (ossProperties.isObjectPublicRead()) {
                req.addHeader("x-oss-object-acl", "public-read");
            }
            URL url = oss.generatePresignedUrl(req);
            oss.shutdown();
            Map<String, Object> m = new HashMap<>();
            m.put("uploadUrl", url.toString());
            m.put("objectKey", objectKey);
            m.put("publicUrl", resolvePublicBaseUrl() + "/" + objectKey);
            m.put("putAclPublicRead", ossProperties.isObjectPublicRead());
            return Result.ok(m);
        } catch (Exception e) {
            return Result.fail("生成上传地址失败: " + e.getMessage());
        }
    }

    /**
     * 未配置 {@code url-prefix} 时，按虚拟主机风格拼出公网地址：https://{bucket}.{endpoint}
     * 避免返回 “/uploads/…” 相对路径导致浏览器向本站请求而 404。
     */
    private String resolvePublicBaseUrl() {
        String configured = trimSlash(ossProperties.getUrlPrefix());
        if (!configured.isEmpty()) {
            return configured;
        }
        String bucket = ossProperties.getBucketName();
        String ep = ossProperties.getEndpoint();
        if (bucket == null || bucket.isBlank() || ep == null || ep.isBlank()) {
            return "";
        }
        ep = ep.replaceFirst("^https?://", "");
        return "https://" + bucket.trim() + "." + ep.trim();
    }

    private static String trimSlash(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String safeContentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "application/octet-stream";
        }
        String s = raw.trim();
        if (s.length() > 127 || s.chars().anyMatch(c -> c < 32 || c > 126)) {
            return "application/octet-stream";
        }
        return s;
    }
}
