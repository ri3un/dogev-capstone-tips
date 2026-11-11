package com.gaebalhagae.place.service;

import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.member.domain.Member;
import com.gaebalhagae.member.repository.MemberRepository;
import com.gaebalhagae.place.domain.EditRequestStatus;
import com.gaebalhagae.place.domain.Place;
import com.gaebalhagae.place.domain.PlaceCondition;
import com.gaebalhagae.place.domain.PlaceEditRequest;
import com.gaebalhagae.place.dto.EditRequestDtos;
import com.gaebalhagae.place.repository.PlaceConditionRepository;
import com.gaebalhagae.place.repository.PlaceEditRequestRepository;
import com.gaebalhagae.place.repository.PlaceRepository;
import com.gaebalhagae.place.review.ConditionKey;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 장소 조건 수정 요청과 관리자 승인·반려 (결정 14-③). 반려하면 장소 정보는 그대로 */
@Service
@RequiredArgsConstructor
@Transactional
public class EditRequestService {

    private final PlaceEditRequestRepository requestRepository;
    private final PlaceRepository placeRepository;
    private final PlaceConditionRepository conditionRepository;
    private final MemberRepository memberRepository;

    public EditRequestDtos.Response create(Long memberId, Long placeId, EditRequestDtos.Create in) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        ConditionKey key = ConditionKey.fromKey(in.conditionKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "없는 조건입니다"));
        if (!key.isKnownValue(in.requestedValue())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "조건에 맞지 않는 값입니다");
        }
        PlaceEditRequest saved = requestRepository.save(new PlaceEditRequest(place.getId(), memberId, key.key(),
                in.requestedValue(), in.comment() == null ? null : in.comment().strip()));
        return toResponses(List.of(saved)).get(0);
    }

    @Transactional(readOnly = true)
    public List<EditRequestDtos.Response> mine(Long memberId) {
        return toResponses(requestRepository.findByMemberIdOrderByIdDesc(memberId));
    }

    @Transactional(readOnly = true)
    public List<EditRequestDtos.Response> forAdmin(EditRequestStatus status) {
        return toResponses(status == null ? requestRepository.findAllByOrderByIdDesc()
                : requestRepository.findByStatusOrderByIdAsc(status));
    }

    public EditRequestDtos.Response approve(Long requestId) {
        PlaceEditRequest r = pending(requestId);
        PlaceCondition c = conditionRepository.findByPlaceIdAndConditionKey(r.getPlaceId(), r.getConditionKey())
                .orElseGet(() -> {
                    PlaceCondition created = new PlaceCondition(r.getPlaceId(), r.getConditionKey());
                    created.update(r.getRequestedValue(), null, 0, 0,
                            com.gaebalhagae.place.domain.ConditionSource.ADMIN);
                    return conditionRepository.save(created);
                });
        c.overrideByAdmin(r.getRequestedValue());
        r.approve();
        placeRepository.findById(r.getPlaceId()).ifPresent(this::rebuildKeywords);
        return toResponses(List.of(r)).get(0);
    }

    public EditRequestDtos.Response reject(Long requestId, String reason) {
        PlaceEditRequest r = pending(requestId);
        r.reject(reason.strip());
        return toResponses(List.of(r)).get(0);
    }

    private void rebuildKeywords(Place place) {
        Map<String, String> values = conditionRepository.findByPlaceId(place.getId()).stream()
                .collect(Collectors.toMap(PlaceCondition::getConditionKey, PlaceCondition::getConditionValue));
        place.changeKeywords(PlaceKeywords.build(values, PlaceKeywords.moodOf(place.getReviewKeywords())));
    }

    private PlaceEditRequest pending(Long id) {
        PlaceEditRequest r = requestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EDIT_REQUEST_NOT_FOUND));
        if (!r.isPending()) {
            throw new BusinessException(ErrorCode.EDIT_REQUEST_ALREADY_HANDLED);
        }
        return r;
    }

    private List<EditRequestDtos.Response> toResponses(List<PlaceEditRequest> list) {
        Map<Long, Place> places = placeRepository.findAllById(list.stream().map(PlaceEditRequest::getPlaceId)
                .distinct().toList()).stream().collect(Collectors.toMap(Place::getId, p -> p));
        Map<Long, String> names = memberRepository.findAllById(list.stream().map(PlaceEditRequest::getMemberId)
                .distinct().toList()).stream().collect(Collectors.toMap(Member::getId, Member::getNickname));
        Map<String, String> current = list.stream().collect(Collectors.toMap(
                r -> r.getPlaceId() + ":" + r.getConditionKey(),
                r -> conditionRepository.findByPlaceIdAndConditionKey(r.getPlaceId(), r.getConditionKey())
                        .map(PlaceCondition::getConditionValue).orElse(ConditionKey.UNKNOWN),
                (a, b) -> a));
        return list.stream().map(r -> new EditRequestDtos.Response(r.getId(), r.getPlaceId(),
                places.containsKey(r.getPlaceId()) ? places.get(r.getPlaceId()).getName() : null,
                r.getConditionKey(), ConditionKey.fromKey(r.getConditionKey()).map(ConditionKey::label).orElse(null),
                current.get(r.getPlaceId() + ":" + r.getConditionKey()), r.getRequestedValue(), r.getComment(),
                r.getStatus(), r.getRejectReason(), r.getMemberId(), names.get(r.getMemberId()), r.getCreatedAt()))
                .toList();
    }
}
