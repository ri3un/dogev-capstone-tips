package com.gaebalhagae.place.collect;

import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 네이버 블로그 검색 API 호출. 키는 NAVER API HUB(네이버 클라우드)에서 발급받은 것이라
 * openapi.naver.com 이 아니라 API HUB 주소와 X-NCP-APIGW-* 헤더를 씁니다.
 */
@Component
public class NaverBlogClient {

    private final RestClient restClient;

    public NaverBlogClient(RestClient.Builder builder,
                           @Value("${naver.search.base-url}") String baseUrl,
                           @Value("${naver.search.client-id}") String clientId,
                           @Value("${naver.search.client-secret}") String clientSecret) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                .defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
                .build();
    }

    /** API 응답 그대로의 모양. title·description에는 <b> 태그가 섞여 있습니다. */
    public record Response(List<Item> items) {
    }

    public record Item(String title, String link, String description, String bloggername, String postdate) {
    }

    /** 최신순으로 최대 display건(1~100)을 가져옵니다. */
    public List<Item> search(String query, int display) {
        try {
            Response res = restClient.get()
                    .uri(u -> u.path("/search/v1/blog")
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("sort", "date")
                            .build())
                    .retrieve()
                    .body(Response.class);
            return res == null || res.items() == null ? List.of() : res.items();
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED, "블로그 검색 실패: " + e.getMessage());
        }
    }
}
