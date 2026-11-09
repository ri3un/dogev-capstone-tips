package com.gaebalhagae.health;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

class HealthControllerTest extends ApiTestSupport {

    @Test
    void 헬스체크는_로그인_없이_공통_응답_형식으로_응답한다() throws Exception {
        getApi(null, "/api/health")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void 로그인이_필요한_API는_401과_공통_오류_형식으로_응답한다() throws Exception {
        getApi(null, "/api/members/me")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
