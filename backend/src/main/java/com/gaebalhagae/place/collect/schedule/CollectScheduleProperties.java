package com.gaebalhagae.place.collect.schedule;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 collect.schedule.*
 * onlyCategory: CAFE·RESTAURANT·PARK 중 하나면 그 분류 장소만 블로그·요약 (예: 산책로·공원만 따로 모을 때). 비우면 전부
 */
@ConfigurationProperties("collect.schedule")
public record CollectScheduleProperties(boolean enabled, String cron, int dailyPlaces, int queriesPerDay,
                                        String addressPrefix, List<String> areas, List<String> keywords,
                                        long summaryDelayMs, String onlyCategory) {

    public CollectScheduleProperties {
        areas = areas == null ? List.of() : List.copyOf(areas);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }
}
