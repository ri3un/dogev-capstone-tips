package com.gaebalhagae.place;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.support.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EditRequestApiTest extends ApiTestSupport {

    private long cafe(String token) throws Exception {
        List<Integer> ids = read(getApi(token, "/api/places"), "$.data[?(@.name == '카페 콩')].id");
        return ids.get(0);
    }

    private Number request(String user, long placeId, String value) throws Exception {
        return read(postApi(user, "/api/places/{id}/edit-requests",
                Map.of("conditionKey", "size_allowed", "requestedValue", value, "comment", "중형견도 들어갔어요"),
                placeId).andExpect(status().isOk()), "$.data.id");
    }

    @Test
    void 승인하면_조건이_바뀌고_다시_수집해도_유지된다() throws Exception {
        String admin = login("관리자", true);
        postApi(admin, "/api/admin/places/import", PlaceApiTest.sample());
        String user = login("다인");
        long placeId = cafe(user);
        Number id = request(user, placeId, "up_to_medium");

        getApi(user, "/api/me/edit-requests").andExpect(jsonPath("$.data[0].status").value("PENDING"));
        getApi(admin, "/api/admin/edit-requests?status=PENDING").andExpect(jsonPath("$.data", hasSize(1)));

        postApi(admin, "/api/admin/edit-requests/{id}/approve", null, id)
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        postApi(admin, "/api/admin/edit-requests/{id}/approve", null, id)
                .andExpect(jsonPath("$.error.code").value("EDIT_REQUEST_ALREADY_HANDLED"));

        postApi(admin, "/api/admin/places/import", PlaceApiTest.sample());
        getApi(user, "/api/places/{id}", placeId)
                .andExpect(jsonPath("$.data.conditions[?(@.key == 'size_allowed')].value").value("up_to_medium"))
                .andExpect(jsonPath("$.data.conditions[?(@.key == 'size_allowed')].source").value("ADMIN"))
                .andExpect(jsonPath("$.data.keywords[0]").value("#중형견까지"));
    }

    @Test
    void 반려하면_사유만_남고_장소는_그대로() throws Exception {
        String admin = login("관리자", true);
        postApi(admin, "/api/admin/places/import", PlaceApiTest.sample());
        String user = login("다인");
        long placeId = cafe(user);
        Number id = request(user, placeId, "up_to_large");

        postApi(admin, "/api/admin/edit-requests/{id}/reject", Map.of("reason", "근거가 부족해요"), id)
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReason").value("근거가 부족해요"));
        getApi(user, "/api/places/{id}", placeId)
                .andExpect(jsonPath("$.data.conditions[?(@.key == 'size_allowed')].value").value("small_only"));
    }

    @Test
    void 없는_조건이나_값은_400() throws Exception {
        postApi(login("관리자", true), "/api/admin/places/import", PlaceApiTest.sample());
        String user = login("다인");
        postApi(user, "/api/places/{id}/edit-requests",
                Map.of("conditionKey", "size_allowed", "requestedValue", "giant"), cafe(user))
                .andExpect(status().isBadRequest());
    }
}
