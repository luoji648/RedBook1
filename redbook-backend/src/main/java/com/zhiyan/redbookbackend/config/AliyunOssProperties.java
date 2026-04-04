package com.zhiyan.redbookbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {
    private String endpoint = "";
    private String accessKeyId = "";
    private String accessKeySecret = "";
    private String bucketName = "";
    /** Public base URL for objects, e.g. https://bucket.oss-cn-xxx.aliyuncs.com/ */
    private String urlPrefix = "";
    /**
     * 预签名 PUT 是否带 public-read ACL。默认 false：开启「阻止公共访问」的 Bucket 会因该头拒绝上传（403）。
     * 需要匿名读图时，可改为 true（并确保 RAM/策略允许），或在控制台用 Bucket 策略对 uploads/* 放行读。
     */
    private boolean objectPublicRead = false;
}
