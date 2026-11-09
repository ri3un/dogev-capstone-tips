package com.gaebalhagae.record;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.llm.GroqClient;
import com.gaebalhagae.support.ApiTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RecordApiTest extends ApiTestSupport {

    @MockitoBean
    private GroqClient groq;

    private final String today = LocalDate.now().toString();

    @Test
    void 글을_기록_후보로_바꾸고_위험_음식을_경고한다() throws Exception {
        given(groq.name()).willReturn("groq/test");
        given(groq.completeJson(anyString(), anyString())).willReturn("""
                {"records":[
                  {"category":"MEAL","date":"%s","memo":"사료 반 컵","details":{"amount":"반 컵"}},
                  {"category":"SNACK","date":"%s","memo":"포도 두 알"},
                  {"category":"STOOL","date":"%s","memo":"무른 변","stoolStatus":"SOFT"}
                ]}""".formatted(today, today, today));
        String token = login("다인");
        long dogId = createDog(token, "콩이");

        postApi(token, "/api/dogs/{id}/records/parse", Map.of("text", "사료 반 컵 먹고 포도 두 알, 변이 좀 물렀어"), dogId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidates", hasSize(3)))
                .andExpect(jsonPath("$.data.candidates[2].stoolStatus").value("SOFT"))
                .andExpect(jsonPath("$.data.warnings[0].food").value("포도·건포도"));
    }

    @Test
    void 후보를_한번에_저장하고_질문은_질문_메모로_간다() throws Exception {
        String token = login("다인");
        long dogId = createDog(token, "콩이");
        List<Map<String, Object>> records = List.of(
                Map.of("category", "WEIGHT", "recordDate", today, "memo", "몸무게", "weightKg", 8.1),
                Map.of("category", "ACTIVITY", "recordDate", today, "memo", "산책", "walkMinutes", 30),
                Map.of("category", "QUESTION", "recordDate", today, "memo", "발을 자주 핥아요"));

        postApi(token, "/api/dogs/{id}/records/batch", Map.of("records", records), dogId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        getApi(token, "/api/dogs/{id}/records", dogId).andExpect(jsonPath("$.data", hasSize(2)));
        getApi(token, "/api/dogs/{id}", dogId).andExpect(jsonPath("$.data.weightKg").value(8.1));
        getApi(token, "/api/dogs/{id}/vet-questions", dogId)
                .andExpect(jsonPath("$.data[0].content").value("발을 자주 핥아요"));
        getApi(token, "/api/dogs/{id}/records/weights", dogId).andExpect(jsonPath("$.data[0].weightKg").value(8.1));
        getApi(token, "/api/dogs/{id}/records/summary", dogId)
                .andExpect(jsonPath("$.data.byCategory.ACTIVITY").value(1))
                .andExpect(jsonPath("$.data.walkMinutesTotal").value(30));
    }

    @Test
    void 기록_수정_삭제와_남의_기록() throws Exception {
        String token = login("다인");
        long dogId = createDog(token, "콩이");
        Number id = read(postApi(token, "/api/dogs/{id}/records",
                Map.of("category", "MEAL", "recordDate", today, "memo", "사료"), dogId), "$.data.id");

        putApi(token, "/api/records/{id}", Map.of("category", "MEAL", "recordDate", today, "memo", "사료 한 컵"), id)
                .andExpect(jsonPath("$.data.memo").value("사료 한 컵"))
                .andExpect(jsonPath("$.data.createdByNickname").value("다인"));
        deleteApi(login("남"), "/api/records/{id}", id).andExpect(status().isNotFound());
        deleteApi(token, "/api/records/{id}", id).andExpect(status().isOk());
    }

    @Test
    void 미래_날짜는_저장하지_않는다() throws Exception {
        String token = login("다인");
        long dogId = createDog(token, "콩이");
        postApi(token, "/api/dogs/{id}/records",
                Map.of("category", "MEAL", "recordDate", LocalDate.now().plusDays(2).toString()), dogId)
                .andExpect(status().isBadRequest());
    }

    @Test
    void 질문_메모_해결_표시() throws Exception {
        String token = login("다인");
        long dogId = createDog(token, "콩이");
        Number id = read(postApi(token, "/api/dogs/{id}/vet-questions", Map.of("content", "예방접종 언제?"), dogId),
                "$.data.id");

        patchApi(token, "/api/vet-questions/{id}", Map.of("resolved", true), id)
                .andExpect(jsonPath("$.data.resolved").value(true));
        deleteApi(token, "/api/vet-questions/{id}", id).andExpect(status().isOk());
    }
}
