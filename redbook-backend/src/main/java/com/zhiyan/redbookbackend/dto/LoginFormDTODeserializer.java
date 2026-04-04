package com.zhiyan.redbookbackend.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 兼容根节点为 JSON 字符串（被二次 stringify）的请求体；字段支持数字或字符串。
 */
public class LoginFormDTODeserializer extends JsonDeserializer<LoginFormDTO> {

    @Override
    public LoginFormDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node;
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            node = mapper.readTree(p.getValueAsString());
        } else {
            node = mapper.readTree(p);
        }
        return fromNode(node);
    }

    private static LoginFormDTO fromNode(JsonNode node) {
        LoginFormDTO dto = new LoginFormDTO();
        if (node.has("phone") && !node.get("phone").isNull()) {
            dto.setPhone(asTextFlexible(node.get("phone")));
        }
        if (node.has("code") && !node.get("code").isNull()) {
            dto.setCode(asTextFlexible(node.get("code")));
        }
        if (node.has("password") && !node.get("password").isNull()) {
            dto.setPassword(node.get("password").asText());
        }
        return dto;
    }

    private static String asTextFlexible(JsonNode n) {
        if (n.isNumber()) {
            return n.asText();
        }
        return n.asText();
    }
}
