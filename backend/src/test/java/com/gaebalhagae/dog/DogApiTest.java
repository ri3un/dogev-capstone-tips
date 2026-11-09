package com.gaebalhagae.dog;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.support.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DogApiTest extends ApiTestSupport {

    @Test
    void 반려견을_등록하면_크기가_자동으로_정해진다() throws Exception {
        String token = login("다인");
        long id = createDog(token, "콩이", "12", "2020-05");

        getApi(token, "/api/dogs/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value("MEDIUM"))
                .andExpect(jsonPath("$.data.birthYm").value("2020-05"));
    }

    @Test
    void 필수값이_없으면_400() throws Exception {
        postApi(login("다인"), "/api/dogs", Map.of("name", "콩이"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 남의_반려견은_404() throws Exception {
        long id = createDog(login("주인"), "콩이");

        getApi(login("남"), "/api/dogs/{id}", id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DOG_NOT_FOUND"));
    }

    @Test
    void 수정하고_관리자만_삭제한다() throws Exception {
        String owner = login("관리자");
        String editor = login("가족");
        long id = createDog(owner, "콩이");
        String code = read(getApi(owner, "/api/family/invite"), "$.data.inviteCode");
        postApi(editor, "/api/family/invites/{code}/accept", null, code);

        Map<String, Object> body = dogBody("콩이", "30", "2015-01");
        body.put("diseases", java.util.List.of("JOINT"));
        patchApi(editor, "/api/dogs/{id}", body, id)
                .andExpect(jsonPath("$.data.size").value("LARGE"))
                .andExpect(jsonPath("$.data.diseases[0]").value("JOINT"));

        deleteApi(editor, "/api/dogs/{id}", id).andExpect(status().isForbidden());
        deleteApi(owner, "/api/dogs/{id}", id).andExpect(status().isOk());
        getApi(owner, "/api/dogs").andExpect(jsonPath("$.data", hasSize(0)));
    }
}
