package com.gaebalhagae.place.dto;

import com.gaebalhagae.place.domain.ConditionSource;
import com.gaebalhagae.place.domain.PlaceCategory;
import com.gaebalhagae.place.service.PlaceVerdict;
import java.time.LocalDateTime;
import java.util.List;

public final class PlaceResponses {

    private PlaceResponses() {
    }

    /** 지도 마커·목록 카드 */
    public record Item(Long id, String name, PlaceCategory category, String categoryLabel, String address,
                       double lat, double lng, Integer distanceM, List<String> keywords, String summaryFirstLine,
                       PlaceVerdict.Verdict verdict) {
    }

    public record Detail(Long id, String name, PlaceCategory category, String categoryLabel, String categoryName,
                         String address, String roadAddress, double lat, double lng, String phone, String placeUrl,
                         String imageUrl, List<String> summary, List<String> keywords, int reviewSourceCount,
                         LocalDateTime reviewSummarizedAt, List<Condition> conditions, List<Source> sources,
                         PlaceVerdict.Verdict verdict) {
    }

    /** 조건 하나. value 가 unknown 이면 "정보 없음", NEEDS_CHECK 면 "확인 필요"로 보여 줍니다 */
    public record Condition(String key, String label, String value, String tag, Integer weightLimitKg,
                            int evidenceCount, int positiveCount, ConditionSource source) {
    }

    public record Source(String url, String title) {
    }
}
