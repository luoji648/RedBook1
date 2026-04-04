package com.zhiyan.redbookbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyan.redbookbackend.dto.req.DifyRequestBody;
import com.zhiyan.redbookbackend.dto.resp.BlockResponse;
import com.zhiyan.redbookbackend.dto.resp.StreamResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class DifyService {

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_STRING =
            new ParameterizedTypeReference<>() {};

    @Value("${dify.url}")
    private String url;

    private final RestTemplate restTemplate;

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    /**
     * 流式调用dify.
     *
     * @param query  查询文本
     * @param userId 用户id
     * @param apiKey apiKey 通过 apiKey 获取权限并区分不同的 dify 应用
     * @return Flux 响应流
     */
    public Flux<StreamResponse> streamingMessage(String query, Long userId, String apiKey, Map<String, String> extraInputs) {
        //1.设置请求体
        DifyRequestBody body = new DifyRequestBody();
        HashMap<String, String> inputs = new HashMap<>();
        if (extraInputs != null) {
            inputs.putAll(extraInputs);
        }
        body.setInputs(inputs);
        body.setQuery(query);
        body.setResponseMode("streaming");
        // 新会话不要传空字符串，部分 Dify 版本会 400；省略 conversation_id 由服务端按新会话处理
        body.setUser(userId.toString());
        String json = toDifyJson(body);
        //2.使用webclient发送post请求
        return webClient.post()
                .uri(url)
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setAccept(List.of(MediaType.TEXT_EVENT_STREAM));
                    httpHeaders.setBearerAuth(apiKey);
                })
                .bodyValue(json)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(b -> Mono.error(new IllegalStateException(
                                        "Dify API " + response.statusCode() + (b.isEmpty() ? "" : ": " + b)))))
                .bodyToFlux(SSE_STRING)
                .flatMap(this::sseDataToStreamResponse);
    }

    private Flux<StreamResponse> sseDataToStreamResponse(ServerSentEvent<String> sse) {
        String data = sse.data();
        if (data == null || data.isBlank()) {
            return Flux.empty();
        }
        String trimmed = data.trim();
        if ("[DONE]".equalsIgnoreCase(trimmed)) {
            return Flux.empty();
        }
        try {
            return Flux.just(objectMapper.readValue(data, StreamResponse.class));
        } catch (JsonProcessingException e) {
            return Flux.empty();
        }
    }

    public Flux<StreamResponse> streamingMessage(String query, Long userId, String apiKey) {
        return streamingMessage(query, userId, apiKey, null);
    }

    /**
     * 阻塞式调用dify.
     *
     * @param query 查询文本
     * @param userId 用户id
     * @param apiKey apiKey 通过 apiKey 获取权限并区分不同的 dify 应用
     * @return BlockResponse
     */
    public BlockResponse blockingMessage(String query, Long userId, String apiKey, Map<String, String> extraInputs) {
        //1.设置请求体
        DifyRequestBody body = new DifyRequestBody();
        HashMap<String, String> inputs = new HashMap<>();
        if (extraInputs != null) {
            inputs.putAll(extraInputs);
        }
        body.setInputs(inputs);
        body.setQuery(query);
        body.setResponseMode("blocking");
        body.setUser(userId.toString());
        //2.设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(apiKey);
        //3.封装请求体和请求头
        String jsonString = toDifyJson(body);
        HttpEntity<String> entity = new HttpEntity<>(jsonString, headers);
        //4.发送post请求，阻塞式
        ResponseEntity<BlockResponse> stringResponseEntity =
                restTemplate.postForEntity(url, entity, BlockResponse.class);
        //5.返回响应体
        return stringResponseEntity.getBody();
    }

    public BlockResponse blockingMessage(String query, Long userId, String apiKey) {
        return blockingMessage(query, userId, apiKey, null);
    }

    /** 序列化 Dify 请求体；conversation_id 上 NON_EMPTY，空值不会写入 JSON。 */
    private String toDifyJson(DifyRequestBody body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Dify 请求体序列化失败", e);
        }
    }
}
