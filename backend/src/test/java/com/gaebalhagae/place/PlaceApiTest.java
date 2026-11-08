package com.gaebalhagae.place;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gaebalhagae.support.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaceApiTest extends ApiTestSupport {

    /** 수집 JSON(CollectedPlace + ReviewSummary)과 같은 모양 */
    static Map<String, Object> sample() {
        Map<String, Object> cafe = Map.of("kakaoId", "k1", "name", "카페 콩", "category", "음식점 > 카페",
                "categoryGroupCode", "CE7", "address", "경기 안양시 동안구 1", "roadAddress", "경기 안양시 동안구 로 1",
                "lat", 37.3900, "lng", 126.9500, "foundBy", List.of("안양 애견카페"),
                "firstSeenAt", "2026-11-01T10:00:00");
        Map<String, Object> park = Map.of("kakaoId", "k2", "name", "평촌중앙공원", "category", "여행 > 공원",
                "categoryGroupCode", "AT4", "address", "경기 안양시 동안구 2", "roadAddress", "",
                "lat", 37.3910, "lng", 126.9510, "foundBy", List.of("안양 공원"),
                "firstSeenAt", "2026-11-01T10:00:00");
        Map<String, Object> summary = Map.of("placeName", "카페 콩",
                "summary", List.of("소형견만 들어갈 수 있어요", "실내 동반 가능", "주차는 어려워요"),
                "keywords", List.of("#소형견만", "#실내동반", "#조용함"),
                "conditions", Map.of(
                        "size_allowed", Map.of("value", "small_only", "evidenceCount", 3, "positiveCount", 3,
                                "evidence", List.of("소형견만 가능")),
                        "indoor", Map.of("value", "indoor", "evidenceCount", 2, "positiveCount", 2,
                                "evidence", List.of())),
                "mood", List.of("#조용함"), "sourceCount", 3, "sourceUrls", List.of("https://blog.naver.com/a"),
                "model", "groq/test", "summarizedAt", "2026-11-01T10:00:00");
        return Map.of("places", List.of(cafe, park), "summaries", List.of(summary));
    }

    long placeId(String token, String name) throws Exception {
        List<Integer> ids = read(getApi(token, "/api/places"), "$.data[?(@.name == '" + name + "')].id");
        return ids.get(0);
    }

    @Test
    void 관리자가_수집_JSON을_넣으면_목록과_상세에_나온다() throws Exception {
        String admin = login("관리자", true);
        postApi(admin, "/api/admin/places/import", sample())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(2))
                .andExpect(jsonPath("$.data.summariesApplied").value(1));

        String user = login("다인");
        getApi(user, "/api/places").andExpect(jsonPath("$.data", hasSize(2)));
        getApi(user, "/api/places?category=PARK").andExpect(jsonPath("$.data[0].name").value("평촌중앙공원"));
        getApi(user, "/api/places?conditions=indoor:indoor").andExpect(jsonPath("$.data", hasSize(1)));
        getApi(user, "/api/places?lat=37.39&lng=126.95&radiusKm=1")
                .andExpect(jsonPath("$.data[0].distanceM").isNumber());

        getApi(user, "/api/places/{id}", placeId(user, "카페 콩"))
                .andExpect(jsonPath("$.data.summary", hasSize(3)))
                .andExpect(jsonPath("$.data.conditions[*].key", hasItem("size_allowed")))
                .andExpect(jsonPath("$.data.sources[0].url").value("https://blog.naver.com/a"));
    }

    @Test
    void 다시_넣어도_중복되지_않는다() throws Exception {
        String admin = login("관리자", true);
        postApi(admin, "/api/admin/places/import", sample());
        postApi(admin, "/api/admin/places/import", sample())
                .andExpect(jsonPath("$.data.created").value(0))
                .andExpect(jsonPath("$.data.updated").value(2));
    }

    @Test
    void 우리_강아지_기준으로_판정한다() throws Exception {
        postApi(login("관리자", true), "/api/admin/places/import", sample());
        String user = login("다인");
        long bigDog = createDog(user, "보리", "30", "2020-01");
        long cafe = placeId(user, "카페 콩");

        getApi(user, "/api/places/{id}?dogId={dog}", cafe, bigDog)
                .andExpect(jsonPath("$.data.verdict.level").value("HARD"));
        getApi(user, "/api/places?dogId={dog}&fitOnly=true", bigDog)
                .andExpect(jsonPath("$.data[*].name", org.hamcrest.Matchers.not(hasItem("카페 콩"))));
    }

    @Test
    void 다녀왔어요는_활동_기록이_된다() throws Exception {
        postApi(login("관리자", true), "/api/admin/places/import", sample());
        String user = login("다인");
        long dogId = createDog(user, "콩이");

        postApi(user, "/api/places/{id}/visits", Map.of("dogId", dogId), placeId(user, "평촌중앙공원"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("VISIT"));
        getApi(user, "/api/dogs/{id}/records?category=ACTIVITY", dogId).andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void 일반_회원은_import_못한다() throws Exception {
        postApi(login("다인"), "/api/admin/places/import", sample()).andExpect(status().isForbidden());
    }
}
