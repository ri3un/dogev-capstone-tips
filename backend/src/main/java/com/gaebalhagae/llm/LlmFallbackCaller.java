package com.gaebalhagae.llm;

import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * LLM 하나로 충분한 작업(글 → 기록, 리포트 요약)용. Groq 먼저, 실패하면 Gemini (결정 31).
 * 형식이 틀리면 같은 모델에 1회 다시 묻고, 모두 실패하면 503 LLM_UNAVAILABLE.
 */
@Slf4j
@Component
public class LlmFallbackCaller {

    private final List<LlmClient> clients;

    @Autowired
    public LlmFallbackCaller(GroqClient groq, GeminiClient gemini) {
        this(List.of(groq, gemini));
    }

    LlmFallbackCaller(List<LlmClient> clients) {
        this.clients = clients;
    }

    public record Result<T>(T value, String model) {
    }

    public <T> Result<T> call(String system, String user, Function<String, T> parser) {
        for (LlmClient client : clients) {
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    return new Result<>(parser.apply(client.completeJson(system, user)), client.name());
                } catch (LlmFormatException e) {
                    log.warn("LLM 형식 오류 model={} attempt={} reason={}", client.name(), attempt, e.getMessage());
                } catch (BusinessException e) {
                    throw e;
                } catch (RuntimeException e) {
                    log.warn("LLM 호출 실패 model={} reason={}", client.name(), e.getMessage());
                    break;
                }
            }
        }
        throw new BusinessException(ErrorCode.LLM_UNAVAILABLE);
    }
}
