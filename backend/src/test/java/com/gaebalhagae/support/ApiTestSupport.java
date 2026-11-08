package com.gaebalhagae.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** API 통합 테스트 공통: H2 + dev-login 토큰으로 실제 요청을 보냅니다. 테스트마다 롤백. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper om;

    protected String login(String nickname) throws Exception {
        return login(nickname, false);
    }

    protected String login(String nickname, boolean admin) throws Exception {
        String body = mvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("nickname", nickname, "admin", admin))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    protected ResultActions getApi(String token, String url, Object... vars) throws Exception {
        return mvc.perform(auth(get(url, vars), token));
    }

    protected ResultActions postApi(String token, String url, Object body, Object... vars) throws Exception {
        return mvc.perform(json(auth(post(url, vars), token), body));
    }

    protected ResultActions patchApi(String token, String url, Object body, Object... vars) throws Exception {
        return mvc.perform(json(auth(patch(url, vars), token), body));
    }

    protected ResultActions putApi(String token, String url, Object body, Object... vars) throws Exception {
        return mvc.perform(json(auth(put(url, vars), token), body));
    }

    protected ResultActions deleteApi(String token, String url, Object... vars) throws Exception {
        return mvc.perform(auth(delete(url, vars), token));
    }

    /** 응답 JSON에서 값 하나를 꺼냅니다. 예: read(result, "$.data.id") */
    protected <T> T read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    protected long createDog(String token, String name) throws Exception {
        return createDog(token, name, "7.5", "2022-03");
    }

    protected long createDog(String token, String name, String weightKg, String birthYm) throws Exception {
        Map<String, Object> body = dogBody(name, weightKg, birthYm);
        Number id = read(postApi(token, "/api/dogs", body).andExpect(status().isOk()), "$.data.id");
        return id.longValue();
    }

    protected Map<String, Object> dogBody(String name, String weightKg, String birthYm) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("breed", "말티즈");
        body.put("weightKg", weightKg);
        body.put("birthYm", birthYm);
        body.put("sex", "FEMALE");
        body.put("neutered", true);
        return body;
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return token == null ? builder : builder.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        if (body == null) {
            return builder;
        }
        return builder.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
    }
}
