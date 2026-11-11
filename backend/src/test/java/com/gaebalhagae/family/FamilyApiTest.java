package com.gaebalhagae.family;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.support.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FamilyApiTest extends ApiTestSupport {

    private String inviteCode(String token) throws Exception {
        return read(getApi(token, "/api/family/invite").andExpect(status().isOk()), "$.data.inviteCode");
    }

    private void accept(String token, String code) throws Exception {
        postApi(token, "/api/family/invites/{code}/accept", null, code).andExpect(status().isOk());
    }

    @Test
    void 처음_로그인하면_혼자인_그룹의_관리자() throws Exception {
        getApi(login("다인"), "/api/family")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.members", hasSize(1)));
    }

    @Test
    void 초대를_수락하면_내_반려견도_함께_옮겨진다() throws Exception {
        String owner = login("카카오계정");
        String joiner = login("네이버계정");
        createDog(owner, "콩이");
        createDog(joiner, "보리");

        accept(joiner, inviteCode(owner));

        getApi(owner, "/api/dogs").andExpect(jsonPath("$.data", hasSize(2)));
        getApi(joiner, "/api/family")
                .andExpect(jsonPath("$.data.myRole").value("EDITOR"))
                .andExpect(jsonPath("$.data.members", hasSize(2)));
    }

    @Test
    void 초대_코드_미리보기와_잘못된_코드() throws Exception {
        String owner = login("관리자");
        getApi(login("손님"), "/api/family/invites/{code}", inviteCode(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(1));
        postApi(login("손님2"), "/api/family/invites/{code}/accept", null, "ZZZZZZZZ")
                .andExpect(status().isNotFound());
    }

    @Test
    void 링크를_새로_만들면_옛_코드는_못_쓴다() throws Exception {
        String owner = login("관리자");
        String old = inviteCode(owner);
        postApi(owner, "/api/family/invite/reset", null).andExpect(status().isOk());

        postApi(login("손님"), "/api/family/invites/{code}/accept", null, old).andExpect(status().isNotFound());
    }

    @Test
    void 가족은_최대_6명() throws Exception {
        String owner = login("관리자");
        String code = inviteCode(owner);
        for (int i = 1; i <= 5; i++) {
            accept(login("가족" + i), code);
        }
        postApi(login("일곱번째"), "/api/family/invites/{code}/accept", null, code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("FAMILY_FULL"));
    }

    @Test
    void 다른_가족이_있는_그룹에서는_수락할_수_없다() throws Exception {
        String a = login("A");
        String b = login("B");
        String aFamily = login("A의가족");
        accept(aFamily, inviteCode(a));

        postApi(aFamily, "/api/family/invites/{code}/accept", null, inviteCode(b))
                .andExpect(jsonPath("$.error.code").value("FAMILY_ALREADY_JOINED"));
    }

    @Test
    void 나가면_반려견은_남고_나는_새_개인_그룹() throws Exception {
        String owner = login("관리자");
        String editor = login("가족");
        createDog(owner, "콩이");
        accept(editor, inviteCode(owner));

        postApi(editor, "/api/family/leave", null).andExpect(status().isOk());

        getApi(editor, "/api/dogs").andExpect(jsonPath("$.data", hasSize(0)));
        getApi(editor, "/api/family").andExpect(jsonPath("$.data.myRole").value("OWNER"));
        getApi(owner, "/api/dogs").andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void 관리자는_가족이_있으면_넘긴_뒤에_나간다() throws Exception {
        String owner = login("관리자");
        String editor = login("가족");
        accept(editor, inviteCode(owner));
        Number editorId = read(getApi(editor, "/api/members/me"), "$.data.id");

        postApi(owner, "/api/family/leave", null)
                .andExpect(jsonPath("$.error.code").value("FAMILY_OWNER_MUST_TRANSFER"));

        postApi(owner, "/api/family/owner", Map.of("memberId", editorId)).andExpect(status().isOk());
        getApi(editor, "/api/family").andExpect(jsonPath("$.data.myRole").value("OWNER"));
        postApi(owner, "/api/family/leave", null).andExpect(status().isOk());
    }

    @Test
    void 관리자만_내보내기와_이름_변경() throws Exception {
        String owner = login("관리자");
        String editor = login("가족");
        accept(editor, inviteCode(owner));
        Number editorId = read(getApi(editor, "/api/members/me"), "$.data.id");

        patchApi(editor, "/api/family", Map.of("name", "바뀐 이름"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FAMILY_OWNER_ONLY"));
        patchApi(owner, "/api/family", Map.of("name", "콩이네")).andExpect(jsonPath("$.data.name").value("콩이네"));

        deleteApi(owner, "/api/family/members/{id}", editorId).andExpect(status().isOk());
        getApi(owner, "/api/family").andExpect(jsonPath("$.data.members", hasSize(1)));
    }
}
