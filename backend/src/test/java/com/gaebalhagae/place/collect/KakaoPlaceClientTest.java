package com.gaebalhagae.place.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

import com.gaebalhagae.global.response.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoPlaceClientTest {

    private static final String BASE = "https://dapi.kakao.com";

    private static String page(boolean isEnd, String id) {
        return """
                {"meta":{"is_end":%s,"total_count":20},"documents":[{"id":"%s","place_name":"카페%s",
                 "category_name":"음식점 > 카페","category_group_code":"CE7","phone":"",
                 "address_name":"경기 안양시 동안구 평촌동 1","road_address_name":"경기 안양시 동안구 시민대로 1",
                 "x":"126.95","y":"37.39","place_url":"http://place.map.kakao.com/%s"}]}
                """.formatted(isEnd, id, id, id);
    }

    @Test
    void KakaoAK_헤더로_마지막_페이지까지_모은다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoPlaceClient client = new KakaoPlaceClient(builder, BASE, "key");

        server.expect(requestTo(containsString("page=1")))
                .andExpect(header("Authorization", "KakaoAK key"))
                .andRespond(withSuccess(page(false, "1"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("page=2")))
                .andRespond(withSuccess(page(true, "2"), MediaType.APPLICATION_JSON));

        var docs = client.search("안양 애견동반 카페");

        assertThat(docs).extracting(KakaoPlaceClient.Document::placeName).containsExactly("카페1", "카페2");
        assertThat(docs.get(0).addressName()).startsWith("경기 안양시");
        server.verify();
    }

    @Test
    void 인증_실패는_EXTERNAL_API_FAILED로_바꾼다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoPlaceClient client = new KakaoPlaceClient(builder, BASE, "wrong");
        server.expect(requestTo(containsString(BASE))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.search("안양 공원")).isInstanceOf(BusinessException.class);
    }
}
