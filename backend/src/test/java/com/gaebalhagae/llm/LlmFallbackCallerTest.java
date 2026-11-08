package com.gaebalhagae.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmFallbackCallerTest {

    /** 미리 정한 답을 차례로 돌려주는 가짜 LLM. "ERROR"면 예외 */
    static class FakeLlm implements LlmClient {
        final Deque<String> answers;
        final String name;
        int calls;

        FakeLlm(String name, String... answers) {
            this.name = name;
            this.answers = new ArrayDeque<>(List.of(answers));
        }

        @Override
        public String completeJson(String system, String user) {
            calls++;
            String a = answers.isEmpty() ? "ERROR" : answers.poll();
            if (a.equals("ERROR")) {
                throw new IllegalStateException("429");
            }
            return a;
        }

        @Override
        public String name() {
            return name;
        }
    }

    private static String parse(String raw) {
        if (!raw.startsWith("{")) {
            throw new LlmFormatException("JSON 아님");
        }
        return raw;
    }

    @Test
    void 첫_모델이_되면_그_답을_쓴다() {
        FakeLlm groq = new FakeLlm("groq", "{\"ok\":1}");
        FakeLlm gemini = new FakeLlm("gemini", "{\"ok\":2}");

        var result = new LlmFallbackCaller(List.of(groq, gemini)).call("s", "u", LlmFallbackCallerTest::parse);

        assertThat(result.value()).isEqualTo("{\"ok\":1}");
        assertThat(result.model()).isEqualTo("groq");
        assertThat(gemini.calls).isZero();
    }

    @Test
    void 형식이_틀리면_같은_모델에_한번_더_묻는다() {
        FakeLlm groq = new FakeLlm("groq", "틀림", "{\"ok\":1}");

        var result = new LlmFallbackCaller(List.of(groq)).call("s", "u", LlmFallbackCallerTest::parse);

        assertThat(result.value()).isEqualTo("{\"ok\":1}");
        assertThat(groq.calls).isEqualTo(2);
    }

    @Test
    void 오류나_두번_틀리면_다음_모델로() {
        FakeLlm groq = new FakeLlm("groq", "틀림", "또틀림");
        FakeLlm gemini = new FakeLlm("gemini", "{\"ok\":2}");

        assertThat(new LlmFallbackCaller(List.of(groq, gemini)).call("s", "u", LlmFallbackCallerTest::parse).model())
                .isEqualTo("gemini");
    }

    @Test
    void 모두_실패하면_503_LLM_UNAVAILABLE() {
        LlmFallbackCaller caller = new LlmFallbackCaller(List.of(new FakeLlm("groq", "ERROR"), new FakeLlm("gemini")));

        assertThatThrownBy(() -> caller.call("s", "u", LlmFallbackCallerTest::parse))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LLM_UNAVAILABLE);
    }
}
