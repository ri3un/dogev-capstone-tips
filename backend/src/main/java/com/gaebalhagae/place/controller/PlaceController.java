package com.gaebalhagae.place.controller;

import com.gaebalhagae.family.service.FamilyAccessService;
import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import com.gaebalhagae.place.domain.Place;
import com.gaebalhagae.place.domain.PlaceCategory;
import com.gaebalhagae.place.dto.EditRequestDtos;
import com.gaebalhagae.place.dto.PlaceResponses;
import com.gaebalhagae.place.service.PlaceQueryService;
import com.gaebalhagae.record.domain.HealthRecord;
import com.gaebalhagae.record.domain.RecordSource;
import com.gaebalhagae.record.dto.RecordResponses;
import com.gaebalhagae.record.service.RecordService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceQueryService placeQueryService;
    private final RecordService recordService;
    private final FamilyAccessService access;
    private final Clock clock;

    /**
     * 지도·목록. dogId 를 주면 "우리 강아지 기준" 판정, fitOnly=true 면 "어려움" 장소를 뺍니다.
     * conditions 예: indoor:indoor,parking:yes
     */
    @GetMapping("/api/places")
    public ApiResponse<List<PlaceResponses.Item>> list(@AuthenticationPrincipal LoginMember me,
                                                       @RequestParam(required = false) PlaceCategory category,
                                                       @RequestParam(required = false) String q,
                                                       @RequestParam(required = false) Double lat,
                                                       @RequestParam(required = false) Double lng,
                                                       @RequestParam(required = false) Double radiusKm,
                                                       @RequestParam(required = false) Long dogId,
                                                       @RequestParam(defaultValue = "false") boolean fitOnly,
                                                       @RequestParam(required = false) List<String> conditions) {
        return ApiResponse.ok(placeQueryService.list(me.id(), new PlaceQueryService.Filter(category, q, lat, lng,
                radiusKm, dogId, fitOnly, conditions)));
    }

    @GetMapping("/api/places/{placeId}")
    public ApiResponse<PlaceResponses.Detail> detail(@AuthenticationPrincipal LoginMember me,
                                                     @PathVariable Long placeId,
                                                     @RequestParam(required = false) Long dogId) {
        return ApiResponse.ok(placeQueryService.detail(me.id(), placeId, dogId));
    }

    /** 장소 상세의 "다녀왔어요": 오늘(또는 visitedOn) 활동 기록으로 저장 (결정 5-A) */
    @PostMapping("/api/places/{placeId}/visits")
    @Transactional
    public ApiResponse<RecordResponses.Record> visit(@AuthenticationPrincipal LoginMember me,
                                                     @PathVariable Long placeId,
                                                     @Valid @RequestBody EditRequestDtos.Visit request) {
        access.checkDog(me.id(), request.dogId());
        Place place = placeQueryService.get(placeId);
        LocalDate today = LocalDate.now(clock);
        LocalDate date = request.visitedOn() == null || request.visitedOn().isAfter(today) ? today : request.visitedOn();
        HealthRecord record = recordService.createActivity(request.dogId(), me.id(), date, RecordSource.VISIT, null,
                place.getName() + " 다녀왔어요", Map.of("placeId", place.getId(), "placeName", place.getName()));
        return ApiResponse.ok(recordService.toResponse(record));
    }
}
