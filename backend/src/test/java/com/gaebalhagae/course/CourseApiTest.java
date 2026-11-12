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
    void 들를_곳이_4곳이면_400() throws Exception {
        String user = login("다인");
        long dogId = createDog(user, "콩이", "4", "2022-01");

        postApi(user, "/api/courses/recommend", Map.of("dogId", dogId, "lat", 37.3895, "lng", 126.9495,
                "placeTypes", List.of("PARK", "PARK", "CAFE", "CAFE"))).andExpect(status().isBadRequest());
    }

    @Test
    void 다시_추천은_안_다녀온_코스만_바꾸고_다녀온_코스는_남긴다() throws Exception {
        postApi(login("관리자", true), "/api/admin/places/import", PlaceApiTest.sample());
        String user = login("다인");
        long dogId = createDog(user, "콩이", "4", "2022-01");
        Number first = read(postApi(user, "/api/courses/recommend", body(dogId)), "$.data.id");

        Number second = read(postApi(user, "/api/courses/recommend", retry(dogId, List.of(), first))
                .andExpect(status().isOk()), "$.data.id");
        getApi(user, "/api/courses/{id}", first).andExpect(status().isNotFound());
        getApi(user, "/api/dogs/{id}/courses", dogId).andExpect(jsonPath("$.data", hasSize(1)));

        postApi(user, "/api/courses/{id}/complete", null, second);
        postApi(user, "/api/courses/recommend", retry(dogId, List.of(), second)).andExpect(status().isOk());
        getApi(user, "/api/courses/{id}", second).andExpect(status().isOk());
        getApi(user, "/api/dogs/{id}/courses", dogId).andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void 다른_가족_코스로_다시_추천하면_404() throws Exception {
        postApi(login("관리자", true), "/api/admin/places/import", PlaceApiTest.sample());
        String user = login("다인");
        Number id = read(postApi(user, "/api/courses/recommend", body(createDog(user, "콩이", "4", "2022-01"))),
                "$.data.id");
        String other = login("남");
        long otherDog = createDog(other, "보리", "4", "2022-01");

        postApi(other, "/api/courses/recommend", retry(otherDog, List.of(), id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
        getApi(user, "/api/courses/{id}", id).andExpect(status().isOk());
    }

    @Test
    void 후보를_모두_제외하면_부족_오류이고_기존_코스는_남는다() throws Exception {
        postApi(login("관리자", true), "/api/admin/places/import", PlaceApiTest.sample());
        String user = login("다인");
        long dogId = createDog(user, "콩이", "4", "2022-01");
        var result = postApi(user, "/api/courses/recommend", body(dogId));
        Number id = read(result, "$.data.id");
        Number cafeId = read(result, "$.data.stops[0].placeId");

        postApi(user, "/api/courses/recommend", retry(dogId, List.of(cafeId), id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_ENOUGH_PLACES"));
        getApi(user, "/api/courses/{id}", id).andExpect(status().isOk());
    }

    private Map<String, Object> retry(long dogId, List<Number> exclude, Number replaceCourseId) {
        return Map.of("dogId", dogId, "lat", 37.3895, "lng", 126.9495, "placeTypes", List.of("CAFE", "PARK"),
                "excludePlaceIds", exclude, "replaceCourseId", replaceCourseId);
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
