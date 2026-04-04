package com.zhiyan.redbookbackend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.zhiyan.redbookbackend.dto.LoginFormDTO;
import com.zhiyan.redbookbackend.dto.LoginFormDTODeserializer;
import com.zhiyan.redbookbackend.dto.req.NoteSaveDTO;
import com.zhiyan.redbookbackend.dto.req.NoteSaveDTODeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * 显式注册，确保 MVC HttpMessageConverter 使用的 ObjectMapper 一定使用自定义反序列化
     * （仅类上 {@code @JsonDeserialize} 在部分环境下不会作用于 {@code @RequestBody} 根类型）。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer registerLoginFormDtoDeserializer() {
        return builder -> builder.postConfigurer((ObjectMapper om) -> {
            // 部分客户端会把验证码写成 076544；Jackson 2.x 词法层默认拒绝数字前导零
            om.enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);
            om.registerModule(new SimpleModule()
                    .addDeserializer(LoginFormDTO.class, new LoginFormDTODeserializer())
                    .addDeserializer(NoteSaveDTO.class, new NoteSaveDTODeserializer()));
        });
    }
}
