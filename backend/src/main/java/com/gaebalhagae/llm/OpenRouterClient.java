package com.gaebalhagae.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** OpenRouter (OpenAI 호환). AI 상담의 세 번째 모델. 무료 모델은 하루 50회라 시연 전 연습을 줄일 것 (결정 2) */
@Component
public class OpenRouterClient implements LlmClient {

    private final RestClient restClient;
    private final String model;

    public OpenRouterClient(RestClient.Builder builder,
                            @Value("${llm.openrouter.base-url}") String baseUrl,
                            @Value("${llm.openrouter.api-key}") String apiKey,
                            @Value("${llm.openrouter.model}") String model) {
        this.restClient = builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("X-Title", "kungkung")
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
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.2);
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
        return "openrouter/" + model;
    }
}
