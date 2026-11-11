package com.gaebalhagae.course;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.place.PlaceApiTest;
import com.gaebalhagae.support.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CourseApiTest extends ApiTestSupport {

    private Map<String, Object> body(long dogId) {
        return Map.of("dogId", dogId, "lat", 37.3895, "lng", 126.9495, "placeTypes", List.of("CAFE", "PARK"));
    }

    @Test
    void 코스를_추천하고_다녀왔어요로_기록한다() throws Exception {
        postApi(login("관리자", true), "/api/admin/places/import", PlaceApiTest.sample());
        String user = login("다인");
        long dogId = createDog(user, "콩이", "4", "2022-01");

        Number id = read(postApi(user, "/api/courses/recommend", body(dogId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stops", hasSize(2)))
                .andExpect(jsonPath("$.data.stops[0].name").value("카페 콩"))
                .andExpect(jsonPath("$.data.legs", hasSize(2)))
                .andExpect(jsonPath("$.data.totalMinutes").isNumber()), "$.data.id");

        getApi(user, "/api/dogs/{id}/courses", dogId).andExpect(jsonPath("$.data", hasSize(1)));
        postApi(user, "/api/courses/{id}/complete", null, id)
                .andExpect(jsonPath("$.data.visitedAt").isNotEmpty());
        getApi(user, "/api/dogs/{id}/records?category=ACTIVITY", dogId)
                .andExpect(jsonPath("$.data[0].source").value("COURSE"));
        getApi(login("남"), "/api/courses/{id}", id).andExpect(status().isNotFound());
    }

    @Test
    void 코스를_삭제해도_다녀온_활동_기록은_남는다() throws Exception {
        postApi(login("관리자", true), "/api/admin/places/import", PlaceApiTest.sample());
        String user = login("다인");
        long dogId = createDog(user, "콩이", "4", "2022-01");
        Number id = read(postApi(user, "/api/courses/recommend", body(dogId)), "$.data.id");
        postApi(user, "/api/courses/{id}/complete", null, id);

        deleteApi(login("남"), "/api/courses/{id}", id).andExpect(status().isNotFound());
        deleteApi(user, "/api/courses/{id}", id).andExpect(status().isOk());

        getApi(user, "/api/courses/{id}", id).andExpect(status().isNotFound());
        getApi(user, "/api/dogs/{id}/courses", dogId).andExpect(jsonPath("$.data", hasSize(0)));
        getApi(user, "/api/dogs/{id}/records?category=ACTIVITY", dogId).andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void 들어갈_수_없는_장소만_있으면_404() throws Exception {
        postApi(login("관리자", true), "/api/admin/places/import", PlaceApiTest.sample());
        String user = login("다인");
        long bigDog = createDog(user, "보리", "30", "2022-01");

        postApi(user, "/api/courses/recommend", body(bigDog))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_ENOUGH_PLACES"));
    }

    @Test
    void 노령견은_이유를_함께_보여_준다() throws Exception {
        postApi(login("관리자", true), "/api/admin/places/import", PlaceApiTest.sample());
        String user = login("다인");
        long oldDog = createDog(user, "할매", "4", "2014-01");

        postApi(user, "/api/courses/recommend", body(oldDog))
                .andExpect(jsonPath("$.data.reasons", hasItem("노령견이라 2km 이내로 추천했어요")));
    }
}
