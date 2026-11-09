package com.gaebalhagae.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Google Gemini. Groq가 한도 초과·오류일 때만 대신 부르는 대안 모델입니다. (결정 31)
 */
@Component
public class GeminiClient implements LlmClient {

    private final RestClient restClient;
    private final String model;

    public GeminiClient(RestClient.Builder builder,
                        @Value("${llm.gemini.base-url}") String baseUrl,
                        @Value("${llm.gemini.api-key}") String apiKey,
                        @Value("${llm.gemini.model}") String model) {
        this.restClient = builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        this.model = model;
    }

    @Override
    public String completeJson(String system, String user) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", system))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", user)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2));
        JsonNode res = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return res.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
    }

    @Override
    public String name() {
        return "gemini/" + model;
    }
}
