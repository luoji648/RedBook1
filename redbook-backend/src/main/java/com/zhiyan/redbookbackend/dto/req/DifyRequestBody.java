package com.zhiyan.redbookbackend.dto.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Dify请求体.
 */
@Data
public class DifyRequestBody implements Serializable {

    /**
     * 用户输入/提问内容.
     */
    private String query;

    /**
     * 允许传入 App 定义的各变量值.
     */
    private Map<String, String> inputs;

    /**
     * 响应模式，streaming 流式，blocking 阻塞.
     */
    @JsonProperty("response_mode")
    private String responseMode;

    /**
     * 用户标识.
     */
    private String user;

    /**
     * 会话id（新会话不传或省略；空串部分 Dify 版本会 400）.
     */
    @JsonProperty("conversation_id")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String conversationId;
}
