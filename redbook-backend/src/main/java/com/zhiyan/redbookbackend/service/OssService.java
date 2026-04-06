package com.zhiyan.redbookbackend.service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.zhiyan.redbookbackend.config.AliyunOssProperties;
import com.zhiyan.redbookbackend.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private static final long UPLOAD_VIA_API_MAX_BYTES = 10L * 1024 * 1024;

    private static final Set<String> IMAGE_UPLOAD_EXT = Set.of(
            "jpg", "jpeg", "jfif", "pjpeg", "pjp", "png", "apng", "gif", "webp",
            "bmp", "dib", "svg", "ico", "heic", "heif", "avif");

    private final AliyunOssProperties ossProperties;

    private Result validateOssConfigured() {
        if (ossProperties.getAccessKeyId() == null || ossProperties.getAccessKeyId().isBlank()
                || ossProperties.getAccessKeyId().startsWith("your-")) {
            return Result.fail("请在配置或环境变量中填写有效的 ALIYUN_OSS_ACCESS_KEY_ID / ALIYUN_OSS_ACCESS_KEY_SECRET");
        }
        if (ossProperties.getAccessKeySecret() == null || ossProperties.getAccessKeySecret().isBlank()) {
            return Result.fail("请在配置或环境变量中填写 ALIYUN_OSS_ACCESS_KEY_SECRET（当前为空）");
        }
        if (ossProperties.getBucketName() == null || ossProperties.getBucketName().isBlank()) {
            return Result.fail("请在配置或环境变量中填写 ALIYUN_OSS_BUCKET（Bucket 名称，当前为空）");
        }
        String endpoint = normalizeEndpoint(ossProperties.getEndpoint());
        if (endpoint.isBlank()) {
            return Result.fail("请在配置或环境变量中填写 ALIYUN_OSS_ENDPOINT（如 oss-cn-beijing.aliyuncs.com）");
        }
        return null;
    }

    /**
     * 经服务端写入 OSS，浏览器只请求本 API，避免外网环境下 OSS Bucket 未配置 CORS 时直连 PUT 失败。
     * 不设对象 ACL，兼容「禁用 Bucket ACL」的 Bucket；公开读依赖 Bucket 策略/CDN。
     */
    public Result uploadViaApi(MultipartFile file) {
        Result bad = validateOssConfigured();
        if (bad != null) {
            return bad;
        }
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择文件");
        }
        if (file.getSize() > UPLOAD_VIA_API_MAX_BYTES) {
            return Result.fail("文件过大，单文件不超过 10MB");
        }
        if (!isAllowedImagePart(file.getOriginalFilename(), file.getContentType())) {
            return Result.fail("仅支持常见图片格式");
        }
        String ct = safeContentType(file.getContentType());
        String endpoint = normalizeEndpoint(ossProperties.getEndpoint());
        String suffix = resolveSuffixFromFilename(file.getOriginalFilename());
        String objectKey = "uploads/" + UUID.randomUUID().toString().replace("-", "") + suffix;
        OSS oss = null;
        try {
            oss = new OSSClientBuilder().build(
                    endpoint,
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret());
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            meta.setContentType(ct);
            try (InputStream in = file.getInputStream()) {
                oss.putObject(ossProperties.getBucketName(), objectKey, in, meta);
            }
            Map<String, Object> m = new HashMap<>();
            m.put("objectKey", objectKey);
            m.put("publicUrl", resolvePublicBaseUrl() + "/" + objectKey);
            m.put("putAclPublicRead", false);
            return Result.ok(m);
        } catch (Exception e) {
            log.warn("OSS 服务端上传失败 bucket={} key={}", ossProperties.getBucketName(), objectKey, e);
            return Result.fail("上传失败: " + e.getMessage());
        } finally {
            if (oss != null) {
                oss.shutdown();
            }
        }
    }

    /** contentType 须与浏览器 PUT 请求的 Content-Type 一致，否则 OSS 返回 403。 */
    public Result presignedPut(String ext, String contentType) {
        Result bad = validateOssConfigured();
        if (bad != null) {
            return bad;
        }
        String endpoint = normalizeEndpoint(ossProperties.getEndpoint());
        String suffix = (ext == null || ext.isBlank()) ? "" : ext.startsWith(".") ? ext : "." + ext;
        String objectKey = "uploads/" + UUID.randomUUID().toString().replace("-", "") + suffix;
        String ct = safeContentType(contentType);
        try {
            OSS oss = new OSSClientBuilder().build(
                    endpoint,
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
            log.warn("OSS 预签名失败 endpoint={} bucket={}", endpoint, ossProperties.getBucketName(), e);
            return Result.fail("生成上传地址失败: " + e.getMessage());
        }
    }

    /** SDK 建议使用带协议的 Endpoint；无协议时补上 https:// */
    private static String normalizeEndpoint(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return s;
        }
        return "https://" + s;
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

    private static String lastExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        String name = originalFilename.trim();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot >= name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isAllowedImagePart(String originalFilename, String rawContentType) {
        String ct = rawContentType == null ? "" : rawContentType.trim().toLowerCase(Locale.ROOT);
        if (ct.startsWith("image/")) {
            return true;
        }
        return IMAGE_UPLOAD_EXT.contains(lastExtension(originalFilename));
    }

    private static String resolveSuffixFromFilename(String originalFilename) {
        String ext = lastExtension(originalFilename);
        return ext.isEmpty() ? ".jpg" : "." + ext;
    }
}
