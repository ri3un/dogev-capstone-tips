package com.gaebalhagae.admin;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

class AdminMemberApiTest extends ApiTestSupport {

    private long memberId(String token) throws Exception {
        Number id = read(getApi(token, "/api/members/me"), "$.data.id");
        return id.longValue();
    }

    @Test
    void 일반_회원은_볼_수_없다() throws Exception {
        getApi(login("다인"), "/api/admin/members").andExpect(status().isForbidden());
    }

    @Test
    void 회원_목록에_입력_개수가_나온다() throws Exception {
        String user = login("다인");
        createDog(user, "콩이");
        long id = memberId(user);

        getApi(login("관리자", true), "/api/admin/members")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members[?(@.id == %d)].totalCount", id).value(hasItem(1)));
    }

    @Test
    void 자세히_보기는_항목별_개수와_날짜만_준다() throws Exception {
        String user = login("다인");
        createDog(user, "콩이");
        long id = memberId(user);

        getApi(login("관리자", true), "/api/admin/members/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.familyRole").value("OWNER"))
                .andExpect(jsonPath("$.data.counts[0].key").value("dog"))
                .andExpect(jsonPath("$.data.counts[0].count").value(1))
                .andExpect(jsonPath("$.data.counts[0].lastAt").exists());
    }

    @Test
    void 탈퇴한_회원은_표시된다() throws Exception {
        String user = login("다인");
        long id = memberId(user);
        deleteApi(user, "/api/members/me").andExpect(status().isOk());

        getApi(login("관리자", true), "/api/admin/members/{id}", id)
                .andExpect(jsonPath("$.data.member.withdrawn").value(true))
                .andExpect(jsonPath("$.data.member.nickname").value("탈퇴한 회원"));
    }
}
