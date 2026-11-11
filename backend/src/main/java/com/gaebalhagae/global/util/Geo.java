package com.gaebalhagae.global.util;

/** 직선거리(하버사인)와 걷는 시간. 경로 API(TMAP) 없이 계산합니다 (결정 5) */
public final class Geo {

    private static final double EARTH_RADIUS_M = 6_371_000;
    /** 반려견과 걷는 속도 시속 약 4km */
    private static final double WALK_M_PER_MIN = 4000.0 / 60;

    private Geo() {
    }

    public static int distanceM(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (int) Math.round(2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a)));
    }

    public static int walkMinutes(int meters) {
        return (int) Math.round(meters / WALK_M_PER_MIN);
    }
}
