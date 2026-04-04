package com.zhiyan.redbookbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "redbook")
public class RedbookProperties {
    private int likeUserShards = 8;
    private int likeNoteShards = 8;
    private int smsMaxPerDay = 20;
    private String frontendNoteUrl = "http://localhost:5173/note/{id}";
}
