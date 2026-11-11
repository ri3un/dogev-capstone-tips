package com.gaebalhagae.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** LLM 이 준 문자열을 JSON 으로 읽습니다. ```json 코드 블록으로 감싸 와도 벗겨 냅니다 */
public final class LlmJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LlmJson() {
    }

    public static JsonNode read(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LlmFormatException("빈 응답");
        }
        String s = raw.strip();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(json)?", "").replaceFirst("```$", "").strip();
        }
        try {
            JsonNode node = MAPPER.readTree(s);
            if (node == null || !node.isObject()) {
                throw new LlmFormatException("JSON 객체가 아님");
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new LlmFormatException("JSON 파싱 실패");
        }
    }

    /** 문자열 필드. 없거나 비면 null */
    public static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() || v.asText().isBlank() ? null : v.asText().strip();
    }
}
