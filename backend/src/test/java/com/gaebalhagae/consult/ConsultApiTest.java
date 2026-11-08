package com.gaebalhagae.consult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.llm.GeminiClient;
import com.gaebalhagae.llm.GroqClient;
import com.gaebalhagae.llm.OpenRouterClient;
import com.gaebalhagae.support.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ConsultApiTest extends ApiTestSupport {

    @MockitoBean
    private GeminiClient gemini;
    @MockitoBean
    private GroqClient groq;
    @MockitoBean
    private OpenRouterClient openRouter;

    private static final String LOW = """
            {"urgency":"LOW","visitVet":false,"answer":"하루 정도 지켜보세요.","reasons":["식욕이 있음"],"homeCare":["물 자주 주기"]}""";
    private static final String HIGH = """
            {"urgency":"HIGH","visitVet":true,"answer":"오늘 병원에 가 보세요.","reasons":["반복 구토"],"homeCare":[]}""";

    @BeforeEach
    void names() {
        given(gemini.name()).willReturn("gemini/test");
        given(groq.name()).willReturn("groq/test");
        given(openRouter.name()).willReturn("openrouter/test");
    }

    @Test
    void 세_모델_답을_모아_저장하고_이름은_보내지_않는다() throws Exception {
        given(gemini.completeJson(anyString(), anyString())).willReturn(LOW);
        given(groq.completeJson(anyString(), anyString())).willReturn(HIGH);
        given(openRouter.completeJson(anyString(), anyString())).willThrow(new IllegalStateException("429"));
        String token = login("다인");
        long dogId = createDog(token, "콩이");

        Number id = read(postApi(token, "/api/dogs/{id}/consultations", Map.of("question", "어제부터 두 번 토했어요"), dogId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.urgency").value("HIGH"))
                .andExpect(jsonPath("$.data.visitVet").value(true))
                .andExpect(jsonPath("$.data.crossChecked").value(true))
                .andExpect(jsonPath("$.data.answeredCount").value(2))
                .andExpect(jsonPath("$.data.answers", hasSize(3)))
                .andExpect(jsonPath("$.data.answers[2].status").value("ERROR")), "$.data.id");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(groq).completeJson(anyString(), prompt.capture());
        assertThat(prompt.getValue()).contains("말티즈").doesNotContain("콩이");

        getApi(token, "/api/consultations/{id}", id).andExpect(jsonPath("$.data.summary", hasSize(3)));
        getApi(token, "/api/dogs/{id}/consultations", dogId).andExpect(jsonPath("$.data", hasSize(1)));
        getApi(login("남"), "/api/consultations/{id}", id).andExpect(status().isNotFound());
    }

    @Test
    void 모두_실패하면_503() throws Exception {
        given(gemini.completeJson(anyString(), anyString())).willThrow(new IllegalStateException("x"));
        given(groq.completeJson(anyString(), anyString())).willReturn("JSON 아님");
        given(openRouter.completeJson(anyString(), anyString())).willThrow(new IllegalStateException("x"));
        String token = login("다인");

        postApi(token, "/api/dogs/{id}/consultations", Map.of("question", "기침을 해요"), createDog(token, "콩이"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("LLM_UNAVAILABLE"));
    }

    @Test
    void 응급_표현이면_병원_안내가_먼저() throws Exception {
        given(gemini.completeJson(anyString(), anyString())).willReturn(LOW);
        given(groq.completeJson(anyString(), anyString())).willThrow(new IllegalStateException("x"));
        given(openRouter.completeJson(anyString(), anyString())).willThrow(new IllegalStateException("x"));
        String token = login("다인");

        postApi(token, "/api/dogs/{id}/consultations", Map.of("question", "갑자기 경련을 해요"), createDog(token, "콩이"))
                .andExpect(jsonPath("$.data.emergency").value(true))
                .andExpect(jsonPath("$.data.crossChecked").value(false))
                .andExpect(jsonPath("$.data.notices[0]").value(org.hamcrest.Matchers.containsString("즉시 동물병원")));
    }
}
