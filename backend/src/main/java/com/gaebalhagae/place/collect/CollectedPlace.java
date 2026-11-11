package com.gaebalhagae.place.collect;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 카카오 검색으로 찾은 장소 한 곳. 나중에 place 테이블의 한 행이 됩니다.
 * kakaoId로 중복을 판단하고, 블로그 수집은 name으로 합니다.
 */
public record CollectedPlace(
        String kakaoId,
        String name,
        String category,            // 예: 음식점 > 카페
        String categoryGroupCode,   // 예: CE7(카페), FD6(음식점), AT4(관광명소)
        String address,             // 지번 주소
        String roadAddress,
        double lat,
        double lng,
        String phone,
        String placeUrl,
        List<String> foundBy,       // 이 장소를 찾은 검색어들
        LocalDateTime firstSeenAt
) {
}
