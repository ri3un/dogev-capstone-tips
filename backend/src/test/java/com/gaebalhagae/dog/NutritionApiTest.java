package com.gaebalhagae.dog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.support.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NutritionApiTest extends ApiTestSupport {

    @Test
    void 영양_카드는_공식과_고정_데이터로_만든다() throws Exception {
        String token = login("다인");
        long dogId = createDog(token, "콩이", "10", "2022-01");

        getApi(token, "/api/dogs/{id}/nutrition-card", dogId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rerKcal").value(394))
                .andExpect(jsonPath("$.data.derKcal").value(630))
                .andExpect(jsonPath("$.data.dangerFoods[*].name", hasItem("초콜릿")))
                .andExpect(jsonPath("$.data.safeFoods[0].howToFeed").isNotEmpty())
                .andExpect(jsonPath("$.data.notices[0]", containsString("진단이 아닙니다")));
    }

    @Test
    void 지병과_알레르기를_반영한다() throws Exception {
        String token = login("다인");
        Map<String, Object> body = dogBody("콩이", "10", "2022-01");
        body.put("diseases", List.of("JOINT"));
        body.put("allergies", List.of("닭"));
        Number dogId = read(postApi(token, "/api/dogs", body), "$.data.id");

        getApi(token, "/api/dogs/{id}/nutrition-card", dogId)
                .andExpect(jsonPath("$.data.notices[0]", containsString("수의사")))
                .andExpect(jsonPath("$.data.diseaseCautions[0].diseaseKey").value("JOINT"))
                .andExpect(jsonPath("$.data.safeFoods[*].name", org.hamcrest.Matchers.not(hasItem("닭가슴살"))))
                .andExpect(jsonPath("$.data.cautionFoods[*].name", hasItem("닭가슴살")))
                .andExpect(jsonPath("$.data.cautionFoods[?(@.name == '닭가슴살')].reasonTag", hasItem("ALLERGY")));
    }

    @Test
    void 비만이면_고구마를_주의로_옮기고_감량_기준으로_계산한다() throws Exception {
        String token = login("다인");
        Map<String, Object> body = dogBody("콩이", "10", "2022-01");
        body.put("diseases", List.of("OBESITY"));
        Number dogId = read(postApi(token, "/api/dogs", body), "$.data.id");

        getApi(token, "/api/dogs/{id}/nutrition-card", dogId)
                .andExpect(jsonPath("$.data.factor").value(1.0))
                .andExpect(jsonPath("$.data.weightLoss").value(true))
                .andExpect(jsonPath("$.data.foodGrams").value(112))
                .andExpect(jsonPath("$.data.foodGramsEstimated").value(true))
                .andExpect(jsonPath("$.data.safeFoods[*].name", org.hamcrest.Matchers.not(hasItem("고구마"))))
                .andExpect(jsonPath("$.data.cautionFoods[?(@.name == '고구마')].reason", hasItem("비만이 있어 조금만 주세요")))
                .andExpect(jsonPath("$.data.cautionFoods[?(@.name == '고구마')].reasonTag", hasItem("DISEASE")));
    }

    @Test
    void 지병이_없으면_고구마는_먹어도_되는_음식() throws Exception {
        String token = login("다인");
        long dogId = createDog(token, "콩이", "10", "2022-01");

        getApi(token, "/api/dogs/{id}/nutrition-card", dogId)
                .andExpect(jsonPath("$.data.safeFoods[*].name", hasItem("고구마")))
                .andExpect(jsonPath("$.data.weightLoss").value(false))
                .andExpect(jsonPath("$.data.foodGrams").value(180));
    }

    @Test
    void 안내가_없는_지병은_수의사_상의_문구() throws Exception {
        String token = login("다인");
        Map<String, Object> body = dogBody("콩이", "10", "2022-01");
        body.put("diseases", List.of("갑상선"));
        Number dogId = read(postApi(token, "/api/dogs", body), "$.data.id");

        getApi(token, "/api/dogs/{id}/nutrition-card", dogId)
                .andExpect(jsonPath("$.data.unknownDiseases[0]").value("갑상선"))
                .andExpect(jsonPath("$.data.notices", hasItem("등록된 안내가 없는 질환이에요. 식단은 수의사와 상의하세요.")));
    }
}
