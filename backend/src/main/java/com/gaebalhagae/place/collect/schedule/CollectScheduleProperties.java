package com.gaebalhagae.place.collect.schedule;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 collect.schedule.* */
@ConfigurationProperties("collect.schedule")
public record CollectScheduleProperties(boolean enabled, String cron, int dailyPlaces, int queriesPerDay,
                                        String addressPrefix, List<String> areas, List<String> keywords,
                                        long summaryDelayMs) {

    public CollectScheduleProperties {
        areas = areas == null ? List.of() : List.copyOf(areas);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }
}
