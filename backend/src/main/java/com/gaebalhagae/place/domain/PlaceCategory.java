package com.gaebalhagae.place.domain;

/** 지도 필터·코스 장소 유형. 카카오 그룹 코드로 정합니다 */
public enum PlaceCategory {
    CAFE("카페"), RESTAURANT("식당"), PARK("산책로·공원"), ETC("기타");

    private final String label;

    PlaceCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** CE7 카페, FD6 음식점, AT4(관광명소) 또는 이름·분류에 공원·산책로가 있으면 PARK */
    public static PlaceCategory of(String groupCode, String name, String categoryName) {
        String text = (name == null ? "" : name) + " " + (categoryName == null ? "" : categoryName);
        if ("CE7".equals(groupCode)) {
            return CAFE;
        }
        if ("FD6".equals(groupCode)) {
            return RESTAURANT;
        }
        if ("AT4".equals(groupCode) || text.contains("공원") || text.contains("산책로") || text.contains("숲")) {
            return PARK;
        }
        return ETC;
    }
}
