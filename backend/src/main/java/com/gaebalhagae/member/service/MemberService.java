package com.gaebalhagae.member.service;

import com.gaebalhagae.dog.service.DogService;
import com.gaebalhagae.family.service.FamilyService;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.member.domain.Member;
import com.gaebalhagae.member.domain.Provider;
import com.gaebalhagae.member.dto.MeResponse;
import com.gaebalhagae.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String DEFAULT_NICKNAME = "보호자";

    private final MemberRepository memberRepository;
    private final FamilyService familyService;
    private final DogService dogService;
    private final Clock clock;

    /** SNS 로그인 성공 시: 처음이면 회원을 만들고, 가족 그룹이 없으면 혼자인 그룹을 만듭니다 (결정 26) */
    @Transactional
    public Member login(Provider provider, String providerId, String nickname) {
        Member member = memberRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> memberRepository.save(new Member(provider, providerId,
                        nickname == null || nickname.isBlank() ? DEFAULT_NICKNAME : nickname.strip())));
        familyService.ensurePersonalGroup(member.getId());
        return member;
    }

    /** 회원 탈퇴: 혼자면 반려견·기록까지 삭제, 가족이 있으면 기록은 가족에게 남기고 회원은 익명 처리 */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .filter(m -> !m.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (familyService.isAlone(memberId)) {
            dogService.deleteAllInMyGroup(memberId);
        }
        familyService.withdraw(memberId);
        member.withdraw(LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long memberId) {
        return memberRepository.findById(memberId)
                .map(MeResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
