package com.zhiyan.redbookbackend.dto.req;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * 兼容根节点为 JSON 字符串（客户端二次 stringify）的请求体。
 * 嵌入字符串解码后，长文本里可能出现未转义换行（非严格 JSON），需放宽内层解析。
 */
public class NoteSaveDTODeserializer extends JsonDeserializer<NoteSaveDTO> {

    /** 与主 ObjectMapper 解耦，避免 convertValue 再次命中本反序列化器造成递归 */
    private static final ObjectMapper INNER = new ObjectMapper();

    /** 仅用于解析「整段包在引号里的伪 JSON」内层文本 */
    private static final ObjectMapper LENIENT_EMBEDDED = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

    @Override
    public NoteSaveDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node;
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            node = LENIENT_EMBEDDED.readTree(p.getValueAsString());
        } else {
            node = mapper.readTree(p);
        }
        return INNER.convertValue(node, NoteSaveDTO.class);
    }
}
