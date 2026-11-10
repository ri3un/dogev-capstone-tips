package com.gaebalhagae.place.collect;

import java.util.List;

/**
 * 안양이 아닌 곳의 같은 이름 가게 글을 걸러 냅니다.
 * 다른 지역명만 있고 안양 지명이 없는 글은 제외, 지역명이 아예 없는 글은 남깁니다.
 */
public final class RegionFilter {

    public static final String HOME_REGION = "경기도 안양시";

    // 안양 시·구·동네 이름 (하나라도 있으면 안양 글)
    static final List<String> LOCAL_WORDS = List.of(
            "안양", "평촌", "범계", "인덕원", "동안구", "만안구", "명학", "석수", "호계", "비산",
            "관양", "박달", "부림", "귀인", "달안", "부흥", "신촌동", "벌말");

    // 같은 이름 가게가 많은 다른 지역 (시·구·유명 상권)
    static final List<String> OTHER_WORDS = List.of(
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "제주", "서귀포",
            "수원", "성남", "분당", "판교", "용인", "수지", "기흥", "고양", "일산", "파주", "김포",
            "부천", "광명", "시흥", "안산", "화성", "동탄", "오산", "평택", "하남", "미사", "구리", "남양주",
            "의정부", "양주", "포천", "가평", "양평", "이천", "여주", "춘천", "강릉", "속초", "양양",
            "청주", "천안", "아산", "전주", "군산", "여수", "순천", "목포", "포항", "경주", "창원", "김해", "거제", "통영",
            "강남", "서초", "송파", "잠실", "강동", "마포", "홍대", "연남", "망원", "합정", "성수", "용산", "이태원",
            "종로", "을지로", "여의도", "영등포", "목동", "강서", "노원", "해운대", "광안리", "서면", "송도");

    private RegionFilter() {
    }

    /** 안양 글이거나 지역을 알 수 없는 글이면 true (다른 지역 지점 글이면 false) */
    public static boolean isLocalOrUnknown(String text) {
        String t = text == null ? "" : text.replaceAll("\\s+", "");
        if (LOCAL_WORDS.stream().anyMatch(t::contains)) {
            return true;
        }
        return OTHER_WORDS.stream().noneMatch(t::contains);
    }

    public static boolean isLocalOrUnknown(BlogPost post) {
        return isLocalOrUnknown(post.title() + " " + post.description());
    }
}
