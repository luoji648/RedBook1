package com.zhiyan.redbookbackend.dto.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Dify阻塞式调用响应.
 */
@Data
public class BlockResponse implements Serializable {

    /**
     * 不同模式下的事件类型.
     */
    private String event;

    /**
     * 消息唯一 ID.
     */
    @JsonProperty("message_id")
    private String messageId;

    /**
     * 任务ID.
     */
    @JsonProperty("task_id")
    private String taskId;

    /**
     * agent_thought id.
     */
    private String id;

    /**
     * 会话 ID.
     */
    @JsonProperty("conversation_id")
    private String conversationId;

    /**
     * App 模式，固定为 chat.
     */
    private String mode;

    /**
     * 完整回复内容.
     */
    private String answer;

    /**
     * 元数据.
     */
    private Map<String, Map<String, String>> metadata;

    /**
     * 创建时间戳.
     */
    @JsonProperty("created_at")
    private Long createdAt;

}
