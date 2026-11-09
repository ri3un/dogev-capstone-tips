package com.gaebalhagae.family.service;

import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.dog.repository.DogRepository;
import com.gaebalhagae.family.domain.FamilyMember;
import com.gaebalhagae.family.repository.FamilyMemberRepository;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "이 반려견의 그룹에 내가 있나"를 확인하는 단 한 곳 (결정 26).
 * 남의 반려견은 존재 여부를 드러내지 않도록 404, 관리자 전용 기능은 403.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyAccessService {

    private final FamilyMemberRepository familyMemberRepository;
    private final DogRepository dogRepository;

    public FamilyMember membership(Long memberId) {
        return familyMemberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
    }

    public Long groupIdOf(Long memberId) {
        return membership(memberId).getFamilyGroupId();
    }

    public Dog checkDog(Long memberId, Long dogId) {
        Long groupId = groupIdOf(memberId);
        return dogRepository.findById(dogId)
                .filter(dog -> dog.getFamilyGroupId().equals(groupId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));
    }

    /** 반려견 삭제처럼 관리자만 할 수 있는 일 */
    public Dog checkDogOwner(Long memberId, Long dogId) {
        Dog dog = checkDog(memberId, dogId);
        requireOwner(memberId);
        return dog;
    }

    public FamilyMember requireOwner(Long memberId) {
        FamilyMember me = membership(memberId);
        if (!me.isOwner()) {
            throw new BusinessException(ErrorCode.FAMILY_OWNER_ONLY);
        }
        return me;
    }
}
