package com.gaebalhagae.place.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.gaebalhagae.global.response.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NaverBlogClientTest {

    private static final String BASE = "https://naverapihub.apigw.ntruss.com";

    @Test
    void API_HUB_헤더로_검색하고_결과를_읽는다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverBlogClient client = new NaverBlogClient(builder, BASE, "id", "secret");

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/search/v1/blog?query=")))
                .andExpect(header("X-NCP-APIGW-API-KEY-ID", "id"))
                .andExpect(header("X-NCP-APIGW-API-KEY", "secret"))
                .andRespond(withSuccess("""
                        {"items":[{"title":"<b>카페</b>","link":"u1","description":"강아지","bloggername":"b","postdate":"20261001"}]}
                        """, MediaType.APPLICATION_JSON));

        var items = client.search("카페 애견동반", 10);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).link()).isEqualTo("u1");
        server.verify();
    }

    @Test
    void 인증_실패는_EXTERNAL_API_FAILED로_바꾼다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverBlogClient client = new NaverBlogClient(builder, BASE, "id", "wrong");
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.search("카페", 10)).isInstanceOf(BusinessException.class);
    }
}
