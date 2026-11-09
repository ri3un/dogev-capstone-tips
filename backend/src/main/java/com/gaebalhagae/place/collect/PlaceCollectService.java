package com.gaebalhagae.place.collect;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * "지역 + 키워드"로 카카오에서 장소를 찾아, 해당 지역 주소인 곳만 목록에 더합니다.
 * 같은 장소(kakaoId)는 한 번만 저장하고 찾은 검색어만 추가합니다.
 */
@Service
@RequiredArgsConstructor
public class PlaceCollectService {

    private final KakaoPlaceClient kakaoPlaceClient;
    private final CollectedPlaceStore placeStore;
    private final Clock clock;

    public record KeywordResult(String query, int fetched, int inArea, int added) {
    }

    public record CollectResult(List<KeywordResult> keywords, int total) {
    }

    /**
     * @param area          검색어 앞에 붙일 지역 이름 (예: 안양)
     * @param addressPrefix 이 문자열로 시작하는 주소만 남김 (예: 경기 안양시)
     */
    public CollectResult collect(String area, String addressPrefix, List<String> keywords) {
        // 기존 목록을 kakaoId 기준으로 불러와 순서를 지키며 합침
        Map<String, CollectedPlace> places = new LinkedHashMap<>();
        placeStore.findAll().forEach(p -> places.put(p.kakaoId(), p));

        List<KeywordResult> results = new ArrayList<>();
        for (String keyword : keywords) {
            String query = (area + " " + keyword.trim()).trim();
            List<KakaoPlaceClient.Document> docs = kakaoPlaceClient.search(query);
            List<KakaoPlaceClient.Document> inArea = docs.stream()
                    .filter(d -> d.addressName() != null && d.addressName().startsWith(addressPrefix))  // 다른 지역 제외
                    .toList();
            int added = 0;
            for (KakaoPlaceClient.Document d : inArea) {
                CollectedPlace old = places.get(d.id());
                if (old == null) {
                    places.put(d.id(), toPlace(d, query));
                    added++;
                } else if (!old.foundBy().contains(query)) {
                    places.put(d.id(), withQuery(old, query));
                }
            }
            results.add(new KeywordResult(query, docs.size(), inArea.size(), added));
        }
        List<CollectedPlace> all = List.copyOf(places.values());
        placeStore.saveAll(all);
        return new CollectResult(results, all.size());
    }

    public List<CollectedPlace> findAll() {
        return placeStore.findAll();
    }

    private CollectedPlace toPlace(KakaoPlaceClient.Document d, String query) {
        return new CollectedPlace(d.id(), d.placeName(), d.categoryName(), d.categoryGroupCode(),
                d.addressName(), d.roadAddressName(), parse(d.y()), parse(d.x()), d.phone(), d.placeUrl(),
                List.of(query), LocalDateTime.now(clock));
    }

    private static CollectedPlace withQuery(CollectedPlace p, String query) {
        List<String> foundBy = new ArrayList<>(p.foundBy());
        foundBy.add(query);
        return new CollectedPlace(p.kakaoId(), p.name(), p.category(), p.categoryGroupCode(), p.address(),
                p.roadAddress(), p.lat(), p.lng(), p.phone(), p.placeUrl(), foundBy, p.firstSeenAt());
    }

    private static double parse(String s) {
        try {
            return s == null ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
