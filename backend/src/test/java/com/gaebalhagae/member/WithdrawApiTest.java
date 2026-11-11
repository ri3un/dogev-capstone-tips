package com.gaebalhagae.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.dog.repository.DogRepository;
import com.gaebalhagae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WithdrawApiTest extends ApiTestSupport {

    @Autowired
    DogRepository dogRepository;

    private void join(String owner, String joiner) throws Exception {
        String code = read(getApi(owner, "/api/family/invite"), "$.data.inviteCode");
        postApi(joiner, "/api/family/invites/{code}/accept", null, code).andExpect(status().isOk());
    }

    @Test
    void 혼자면_반려견까지_지우고_남은_토큰은_막힌다() throws Exception {
        String me = login("다인");
        long dogId = createDog(me, "콩이");

        deleteApi(me, "/api/members/me").andExpect(status().isOk());

        getApi(me, "/api/members/me").andExpect(status().isUnauthorized());
        assertThat(dogRepository.existsById(dogId)).isFalse();
    }

    @Test
    void 관리자가_탈퇴하면_가장_먼저_들어온_가족이_관리자() throws Exception {
        String owner = login("관리자");
        String first = login("첫째");
        String second = login("둘째");
        createDog(owner, "콩이");
        join(owner, first);
        join(owner, second);

        deleteApi(owner, "/api/members/me").andExpect(status().isOk());

        getApi(first, "/api/family")
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.members", hasSize(2)));
        getApi(second, "/api/dogs").andExpect(jsonPath("$.data", hasSize(1)));   // 반려견은 가족에게 남음
    }

    @Test
    void 같은_계정으로_다시_로그인하면_새_회원() throws Exception {
        String me = login("다인");
        createDog(me, "콩이");
        deleteApi(me, "/api/members/me").andExpect(status().isOk());

        String again = login("다인");
        getApi(again, "/api/dogs").andExpect(jsonPath("$.data", hasSize(0)));
        getApi(again, "/api/family").andExpect(jsonPath("$.data.myRole").value("OWNER"));
    }
}
