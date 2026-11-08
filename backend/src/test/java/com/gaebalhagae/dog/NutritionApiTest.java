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
                .andExpect(jsonPath("$.data.cautionFoods[*].name", hasItem("닭가슴살")));
    }
}
