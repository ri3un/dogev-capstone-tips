package com.gaebalhagae.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.member.repository.MemberRepository;
import com.gaebalhagae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthApiTest extends ApiTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 개발용_로그인_토큰으로_내_정보를_본다() throws Exception {
        String token = login("다인");

        getApi(token, "/api/members/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("다인"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void 토큰이_없거나_틀리면_401_공통_오류() throws Exception {
        getApi(null, "/api/members/me")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        getApi("broken", "/api/members/me").andExpect(status().isUnauthorized());
    }

    @Test
    void 같은_계정으로_두번_로그인해도_회원은_한명() throws Exception {
        long before = memberRepository.count();
        login("주희");
        login("주희");

        assertThat(memberRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void 관리자_API는_ADMIN만() throws Exception {
        getApi(login("일반"), "/api/admin/edit-requests").andExpect(status().isForbidden());
        getApi(login("관리자", true), "/api/admin/edit-requests").andExpect(status().isOk());
    }
}
