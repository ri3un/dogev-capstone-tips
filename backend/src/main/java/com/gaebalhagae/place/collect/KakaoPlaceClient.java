package com.gaebalhagae.place.collect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 로컬 키워드 검색 API 호출. 검색어 하나에 15건씩 최대 3페이지(45곳)까지 받습니다.
 */
@Component
public class KakaoPlaceClient {

    static final int PAGE_SIZE = 15;
    static final int MAX_PAGE = 3;   // 카카오가 검색어당 45건까지만 주므로 3페이지가 끝

    private final RestClient restClient;

    public KakaoPlaceClient(RestClient.Builder builder,
                            @Value("${kakao.local.base-url}") String baseUrl,
                            @Value("${kakao.local.rest-key}") String restKey) {
        this.restClient = builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "KakaoAK " + restKey)
                .build();
    }

    /** API 응답 그대로의 모양. x는 경도, y는 위도입니다. */
    public record Response(Meta meta, List<Document> documents) {
        public record Meta(@JsonProperty("is_end") boolean isEnd) {
        }
    }

    public record Document(
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("category_group_code") String categoryGroupCode,
            String phone,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("road_address_name") String roadAddressName,
            String x,
            String y,
            @JsonProperty("place_url") String placeUrl) {
    }

    /** 마지막 페이지(is_end)까지 또는 3페이지까지 모아서 돌려줍니다. */
    public List<Document> search(String query) {
        List<Document> all = new ArrayList<>();
        try {
            for (int page = 1; page <= MAX_PAGE; page++) {
                int p = page;
                Response res = restClient.get()
                        .uri(u -> u.path("/v2/local/search/keyword.json")
                                .queryParam("query", query)
                                .queryParam("page", p)
                                .queryParam("size", PAGE_SIZE)
                                .build())
                        .retrieve()
                        .body(Response.class);
                if (res == null || res.documents() == null) {
                    break;
                }
                all.addAll(res.documents());
                if (res.meta() == null || res.meta().isEnd()) {
                    break;
                }
            }
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED, "카카오 장소 검색 실패: " + e.getMessage());
        }
        return all;
    }
}
