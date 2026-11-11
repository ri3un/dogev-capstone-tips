package com.gaebalhagae.dog.service;

import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.dog.domain.DogDeletedEvent;
import com.gaebalhagae.dog.dto.DogRequest;
import com.gaebalhagae.dog.dto.DogResponse;
import com.gaebalhagae.dog.repository.DogRepository;
import com.gaebalhagae.family.service.FamilyAccessService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class DogService {

    private final DogRepository dogRepository;
    private final FamilyAccessService access;
    private final ApplicationEventPublisher events;
    private final PhotoStorage photoStorage;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<DogResponse> list(Long memberId) {
        return dogRepository.findByFamilyGroupIdOrderByIdAsc(access.groupIdOf(memberId)).stream()
                .map(d -> DogResponse.of(d, today()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DogResponse get(Long memberId, Long dogId) {
        return DogResponse.of(access.checkDog(memberId, dogId), today());
    }

    public DogResponse create(Long memberId, DogRequest request) {
        Dog dog = dogRepository.save(new Dog(access.groupIdOf(memberId), memberId, request.toProfile()));
        return DogResponse.of(dog, today());
    }

    public DogResponse update(Long memberId, Long dogId, DogRequest request) {
        Dog dog = access.checkDog(memberId, dogId);
        dog.update(request.toProfile());
        return DogResponse.of(dog, today());
    }

    /** 관리자만. 기록·질문·상담·코스는 DogDeletedEvent 를 받은 각 기능이 같은 트랜잭션에서 지웁니다 */
    public void delete(Long memberId, Long dogId) {
        Dog dog = access.checkDogOwner(memberId, dogId);
        events.publishEvent(new DogDeletedEvent(dogId));
        dogRepository.delete(dog);
        photoStorage.delete(dog.getPhotoUrl());
    }

    /** 회원 탈퇴(혼자인 그룹)일 때 그룹의 반려견과 딸린 기록을 모두 삭제 */
    public void deleteAllInMyGroup(Long memberId) {
        dogRepository.findByFamilyGroupIdOrderByIdAsc(access.groupIdOf(memberId))
                .forEach(dog -> delete(memberId, dog.getId()));
    }

    public DogResponse changePhoto(Long memberId, Long dogId, MultipartFile file) {
        Dog dog = access.checkDog(memberId, dogId);
        String old = dog.getPhotoUrl();
        dog.changePhoto(photoStorage.save(file));
        photoStorage.delete(old);
        return DogResponse.of(dog, today());
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
