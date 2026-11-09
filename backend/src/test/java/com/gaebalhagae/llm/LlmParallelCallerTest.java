package com.gaebalhagae.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.llm.LlmParallelCaller.Status;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmParallelCallerTest {

    private static String parse(String raw) {
        if (!raw.startsWith("{")) {
            throw new LlmFormatException("JSON 아님");
        }
        return raw;
    }

    static LlmClient fixed(String name, String... answers) {
        return new LlmFallbackCallerTest.FakeLlm(name, answers);
    }

    static LlmClient slow(String name, long millis) {
        return new LlmClient() {
            @Override
            public String completeJson(String system, String user) {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "{}";
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    @Test
    void 모델마다_결과_상태를_돌려준다() {
        LlmParallelCaller caller = new LlmParallelCaller(List.of(
                fixed("ok", "{\"a\":1}"),
                fixed("retry-ok", "틀림", "{\"a\":2}"),
                fixed("invalid", "틀림", "또틀림"),
                fixed("error", "ERROR"),
                slow("slow", 2000)), Duration.ofMillis(300));

        var answers = caller.call("s", "u", LlmParallelCallerTest::parse);

        assertThat(answers).extracting(LlmParallelCaller.Answer::status)
                .containsExactly(Status.OK, Status.OK, Status.INVALID, Status.ERROR, Status.TIMEOUT);
        assertThat(answers.get(1).value()).isEqualTo("{\"a\":2}");
        assertThat(answers.get(4).latencyMs()).isGreaterThanOrEqualTo(300);
    }
}
