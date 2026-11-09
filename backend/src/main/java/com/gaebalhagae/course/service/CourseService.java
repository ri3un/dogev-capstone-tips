package com.gaebalhagae.course.service;

import com.gaebalhagae.course.domain.Course;
import com.gaebalhagae.course.domain.CoursePlace;
import com.gaebalhagae.course.dto.CourseDtos;
import com.gaebalhagae.course.repository.CoursePlaceRepository;
import com.gaebalhagae.course.repository.CourseRepository;
import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.dog.domain.DogDeletedEvent;
import com.gaebalhagae.family.service.FamilyAccessService;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.place.domain.Place;
import com.gaebalhagae.place.repository.PlaceRepository;
import com.gaebalhagae.place.review.ConditionKey;
import com.gaebalhagae.place.service.PlaceQueryService;
import com.gaebalhagae.place.service.PlaceVerdict;
import com.gaebalhagae.record.domain.HealthRecord;
import com.gaebalhagae.record.domain.RecordCategory;
import com.gaebalhagae.record.domain.RecordSource;
import com.gaebalhagae.record.repository.HealthRecordRepository;
import com.gaebalhagae.record.service.RecordService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 조건 기반 코스 추천 + "다녀왔어요" (결정 5·31) */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    /** 후보가 부족하면 반경을 단계적으로 넓힙니다 */
    static final int[] RADII_M = {1000, 3000, 5000};

    private final FamilyAccessService access;
    private final PlaceQueryService placeQueryService;
    private final PlaceRepository placeRepository;
    private final HealthRecordRepository recordRepository;
    private final RecordService recordService;
    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final Clock clock;

    public CourseDtos.Response recommend(Long memberId, CourseDtos.Recommend req) {
        Dog dog = access.checkDog(memberId, req.dogId());
        LocalDate today = LocalDate.now(clock);
        CourseRules rules = CourseRules.of(new CourseRules.Input(dog.ageMonths(today), dog.getDiseases(),
                dog.getActivityLevel(), symptomMemos(dog.getId(), today), looseStools(dog.getId(), today)));

        PlaceVerdict.DogInfo info = PlaceVerdict.DogInfo.of(dog);
        List<GreedyCourseBuilder.Spot> spots = placeQueryService.candidates().stream()
                .filter(c -> PlaceVerdict.judge(info, c.conditions()).level() != PlaceVerdict.Level.HARD)
                .map(c -> new GreedyCourseBuilder.Spot(c.place().getId(), c.place().getCategory(), c.place().getLat(),
                        c.place().getLng(), c.conditions().containsKey(ConditionKey.OFF_LEASH_AREA.key())
                        && "yes".equals(c.conditions().get(ConditionKey.OFF_LEASH_AREA.key()).value())))
                .toList();

        int[] radii = req.radiusKm() == null ? RADII_M : new int[]{(int) Math.round(req.radiusKm() * 1000)};
        GreedyCourseBuilder.Plan plan = Arrays.stream(radii)
                .mapToObj(r -> GreedyCourseBuilder.build(req.lat(), req.lng(), req.placeTypes(), spots, rules, r))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_ENOUGH_PLACES));

        Course course = courseRepository.save(new Course(memberId, dog.getId(), plan.startToFirstM(), plan.totalM(),
                plan.totalMinutes(), String.join("\n", rules.reasons())));
        int seq = 1;
        for (GreedyCourseBuilder.Stop s : plan.stops()) {
            coursePlaceRepository.save(new CoursePlace(course.getId(), s.spot().placeId(), seq++, s.legM(),
                    s.legMinutes()));
        }
        return toResponse(course);
    }

    @Transactional(readOnly = true)
    public CourseDtos.Response get(Long memberId, Long courseId) {
        return toResponse(find(memberId, courseId));
    }

    @Transactional(readOnly = true)
    public List<CourseDtos.Response> list(Long memberId, Long dogId) {
        access.checkDog(memberId, dogId);
        return courseRepository.findByDogIdOrderByIdDesc(dogId).stream().map(this::toResponse).toList();
    }

    /** 코스 결과의 "다녀왔어요": 활동 기록(산책 시간 = 예상 시간)으로 저장, 두 번 눌러도 한 번만 (결정 5-A) */
    public CourseDtos.Response complete(Long memberId, Long courseId) {
        find(memberId, courseId);   // 권한 확인
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (course.getVisitedAt() == null) {
            LocalDateTime now = LocalDateTime.now(clock);
            course.markVisited(now);
            List<String> names = toResponse(course).stops().stream().map(CourseDtos.Stop::name).toList();
            recordService.createActivity(course.getDogId(), memberId, now.toLocalDate(), RecordSource.COURSE,
                    course.getTotalMinutes(), "산책 코스 다녀왔어요 (%d곳, %.1fkm)".formatted(names.size(),
                            course.getTotalDistanceM() / 1000.0),
                    Map.of("courseId", course.getId(), "distanceM", course.getTotalDistanceM(), "places", names));
        }
        return toResponse(course);
    }

    @EventListener
    public void onDogDeleted(DogDeletedEvent event) {
        coursePlaceRepository.deleteByDogId(event.dogId());
        courseRepository.deleteByDogId(event.dogId());
    }

    private Course find(Long memberId, Long courseId) {
        Course c = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        try {
            access.checkDog(memberId, c.getDogId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        return c;
    }

    private List<String> symptomMemos(Long dogId, LocalDate today) {
        return recordRepository.findByDogIdAndCategoryAndRecordDateBetweenOrderByRecordDateAscIdAsc(dogId,
                RecordCategory.SYMPTOM, today.minusDays(13), today).stream().map(HealthRecord::memo).toList();
    }

    private long looseStools(Long dogId, LocalDate today) {
        return recordRepository.findByDogIdAndCategoryAndRecordDateBetweenOrderByRecordDateAscIdAsc(dogId,
                        RecordCategory.STOOL, today.minusDays(2), today).stream()
                .filter(r -> r.getStoolStatus() != null && r.getStoolStatus().isLoose()).count();
    }

    private CourseDtos.Response toResponse(Course c) {
        List<CoursePlace> cps = coursePlaceRepository.findByCourseIdOrderBySeqAsc(c.getId());
        Map<Long, Place> places = placeRepository.findAllById(cps.stream().map(CoursePlace::getPlaceId).toList())
                .stream().collect(Collectors.toMap(Place::getId, Function.identity()));
        List<CourseDtos.Stop> stops = new ArrayList<>();
        List<CourseDtos.Leg> legs = new ArrayList<>();
        for (CoursePlace cp : cps) {
            Place p = places.get(cp.getPlaceId());
            if (p == null) {
                continue;   // 장소가 지워진 경우
            }
            stops.add(new CourseDtos.Stop(cp.getSeq(), p.getId(), p.getName(), p.getCategory(),
                    p.getCategory().label(), p.getLat(), p.getLng(), List.copyOf(p.getReviewKeywords()),
                    cp.getLegDistanceM(), cp.getLegMinutes()));
            legs.add(new CourseDtos.Leg(cp.getSeq() - 1, cp.getSeq(), cp.getLegDistanceM(), cp.getLegMinutes()));
        }
        List<String> reasons = c.getReasonText() == null || c.getReasonText().isBlank() ? List.of()
                : List.of(c.getReasonText().split("\n"));
        return new CourseDtos.Response(c.getId(), c.getDogId(), stops, legs, c.getStartToFirstM(),
                c.getTotalDistanceM(), c.getTotalMinutes(), reasons, c.getVisitedAt(), c.getCreatedBy(),
                c.getCreatedAt());
    }
}
