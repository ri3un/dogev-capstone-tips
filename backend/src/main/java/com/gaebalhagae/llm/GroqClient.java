package com.gaebalhagae.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Groq (OpenAI 호환 API). 장소 리뷰 요약의 메인 모델입니다. (결정 31)
 */
@Component
public class GroqClient implements LlmClient {

    private final RestClient restClient;
    private final String model;

    public GroqClient(RestClient.Builder builder,
                      @Value("${llm.groq.base-url}") String baseUrl,
                      @Value("${llm.groq.api-key}") String apiKey,
                      @Value("${llm.groq.model}") String model) {
        this.restClient = builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
    }

    @Override
    public String completeJson(String system, String user) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)),
                "response_format", Map.of("type", "json_object"),   // JSON만 돌려받기
                "reasoning_effort", "low",                          // 생각 토큰을 줄여 분당 토큰 한도 절약
                "temperature", 0.2,
                "max_completion_tokens", 1500);
        JsonNode res = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return res.path("choices").path(0).path("message").path("content").asText();
    }

    @Override
    public String name() {
        return "groq/" + model;
    }
}
