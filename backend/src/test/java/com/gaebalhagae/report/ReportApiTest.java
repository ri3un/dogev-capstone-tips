package com.gaebalhagae.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.llm.GeminiClient;
import com.gaebalhagae.llm.GroqClient;
import com.gaebalhagae.support.ApiTestSupport;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ReportApiTest extends ApiTestSupport {

    @MockitoBean
    private GroqClient groq;
    @MockitoBean
    private GeminiClient gemini;

    private long dogWithRecords(String token) throws Exception {
        long dogId = createDog(token, "콩이");
        LocalDate t = LocalDate.now();
        List<Map<String, Object>> records = List.of(
                Map.of("category", "WEIGHT", "recordDate", t.minusDays(20).toString(), "weightKg", 10.0),
                Map.of("category", "WEIGHT", "recordDate", t.toString(), "weightKg", 10.6),
                Map.of("category", "STOOL", "recordDate", t.minusDays(1).toString(), "stoolStatus", "SOFT", "memo", "무른 변"),
                Map.of("category", "STOOL", "recordDate", t.toString(), "stoolStatus", "DIARRHEA", "memo", "설사"),
                Map.of("category", "MEAL", "recordDate", t.toString(), "memo", "사료 한 컵"),
                Map.of("category", "ACTIVITY", "recordDate", t.toString(), "memo", "산책", "walkMinutes", 40),
                Map.of("category", "HOSPITAL", "recordDate", t.toString(), "memo", "피부과 진료"),
                Map.of("category", "QUESTION", "recordDate", t.toString(), "memo", "사료를 바꿔도 될까요?"));
        postApi(token, "/api/dogs/{id}/records/batch", Map.of("records", records), dogId).andExpect(status().isOk());
        return dogId;
    }

    @Test
    void 미리보기는_9개_섹션과_규칙_요약을_준다() throws Exception {
        given(groq.completeJson(anyString(), anyString())).willThrow(new IllegalStateException("x"));
        given(gemini.completeJson(anyString(), anyString())).willThrow(new IllegalStateException("x"));
        String token = login("다인");
        long dogId = dogWithRecords(token);

        getApi(token, "/api/dogs/{id}/reports/preview", dogId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basic.name").value("콩이"))
                .andExpect(jsonPath("$.data.summary", hasSize(3)))
                .andExpect(jsonPath("$.data.summaryModel").doesNotExist())
                .andExpect(jsonPath("$.data.signals", hasSize(2)))
                .andExpect(jsonPath("$.data.meals", hasSize(1)))
                .andExpect(jsonPath("$.data.stoolCounts['설사']").value(1))
                .andExpect(jsonPath("$.data.walkMinutesTotal").value(40))
                .andExpect(jsonPath("$.data.weights", hasSize(2)))
                .andExpect(jsonPath("$.data.medical[0].memo").value("피부과 진료"))
                .andExpect(jsonPath("$.data.questions[0].content").value("사료를 바꿔도 될까요?"));
    }

    @Test
    void LLM이_되면_요약_3줄을_쓴다() throws Exception {
        given(groq.name()).willReturn("groq/test");
        given(groq.completeJson(anyString(), anyString()))
                .willReturn("{\"summary\":[\"한 줄\",\"두 줄\",\"세 줄\"]}");
        String token = login("다인");

        getApi(token, "/api/dogs/{id}/reports/preview", dogWithRecords(token))
                .andExpect(jsonPath("$.data.summary[1]").value("두 줄"))
                .andExpect(jsonPath("$.data.summaryModel").value("groq/test"));
    }

    @Test
    void PDF는_한글_폰트와_체중_그래프를_넣어_만든다() throws Exception {
        given(groq.completeJson(anyString(), anyString())).willThrow(new IllegalStateException("x"));
        given(gemini.completeJson(anyString(), anyString())).willThrow(new IllegalStateException("x"));
        String token = login("다인");

        byte[] pdf = getApi(token, "/api/dogs/{id}/reports/pdf", dogWithRecords(token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        String raw = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(raw).startsWith("%PDF");
        assertThat(raw).contains("Pretendard");
        assertThat(raw).contains("/Image");
    }

    @Test
    void 기간은_최대_92일() throws Exception {
        String token = login("다인");
        getApi(token, "/api/dogs/{id}/reports/preview?from=2026-01-01&to=2026-06-01", createDog(token, "콩이"))
                .andExpect(status().isBadRequest());
    }
}
