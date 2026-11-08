package com.gaebalhagae.dog;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

class PhotoApiTest extends ApiTestSupport {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    private ResultActions upload(String token, long dogId, byte[] bytes, String name) throws Exception {
        return mvc.perform(multipart("/api/dogs/{id}/photo", dogId)
                .file(new MockMultipartFile("file", name, "image/png", bytes))
                .header("Authorization", "Bearer " + token));
    }

    @Test
    void 사진을_올리면_주소가_저장된다() throws Exception {
        String token = login("다인");
        long dogId = createDog(token, "콩이");

        upload(token, dogId, PNG, "kong.png")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.photoUrl", startsWith("/uploads/dogs/")));
    }

    @Test
    void 이미지가_아니면_400() throws Exception {
        String token = login("다인");
        upload(token, createDog(token, "콩이"), "hello".getBytes(), "evil.png")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE"));
    }

    @Test
    void 남의_반려견에는_못_올린다() throws Exception {
        long dogId = createDog(login("주인"), "콩이");
        upload(login("남"), dogId, PNG, "x.png").andExpect(status().isNotFound());
    }

    @Test
    void 잘못된_파라미터는_500이_아니라_400() throws Exception {
        getApi(login("다인"), "/api/places?category=NOPE").andExpect(status().isBadRequest());
    }
}
