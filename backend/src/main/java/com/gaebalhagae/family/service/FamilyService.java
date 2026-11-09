package com.gaebalhagae.family.service;

import com.gaebalhagae.dog.repository.DogRepository;
import com.gaebalhagae.family.domain.FamilyGroup;
import com.gaebalhagae.family.domain.FamilyMember;
import com.gaebalhagae.family.domain.FamilyRole;
import com.gaebalhagae.family.dto.FamilyResponse;
import com.gaebalhagae.family.dto.InvitePreviewResponse;
import com.gaebalhagae.family.dto.InviteResponse;
import com.gaebalhagae.family.repository.FamilyGroupRepository;
import com.gaebalhagae.family.repository.FamilyMemberRepository;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.member.domain.Member;
import com.gaebalhagae.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가족 그룹 정책 (결정 26, 10/22 회의)
 * - 한 사람은 그룹 하나, 관리자 1명, 최대 6명
 * - 수락하면 내 반려견도 함께 이동, 나가면 반려견은 남고 나는 새 개인 그룹
 * - 초대 링크는 횟수·시간 제한 없음, "새로 만들기"로 옛 링크 무효화
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FamilyService {

    private final FamilyGroupRepository groupRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final MemberRepository memberRepository;
    private final DogRepository dogRepository;
    private final FamilyAccessService access;
    private final InviteCodeGenerator codeGenerator;
    private final Clock clock;

    /** 로그인할 때마다 호출. 그룹이 없으면 혼자인 그룹을 만들어 혼자·가족을 같은 코드로 처리합니다 */
    public void ensurePersonalGroup(Long memberId) {
        if (familyMemberRepository.findByMemberId(memberId).isEmpty()) {
            FamilyGroup group = newGroup(memberId);
            familyMemberRepository.save(new FamilyMember(group.getId(), memberId, FamilyRole.OWNER, now()));
        }
    }

    @Transactional(readOnly = true)
    public FamilyResponse getMyFamily(Long memberId) {
        FamilyMember me = access.membership(memberId);
        return toResponse(group(me.getFamilyGroupId()), me);
    }

    public FamilyResponse rename(Long memberId, String name) {
        FamilyMember me = access.requireOwner(memberId);
        FamilyGroup group = group(me.getFamilyGroupId());
        group.rename(name.strip());
        return toResponse(group, me);
    }

    @Transactional(readOnly = true)
    public InviteResponse getInvite(Long memberId) {
        return InviteResponse.of(group(access.groupIdOf(memberId)).getInviteCode());
    }

    public InviteResponse resetInvite(Long memberId) {
        FamilyGroup group = group(access.requireOwner(memberId).getFamilyGroupId());
        group.changeInviteCode(uniqueCode());
        return InviteResponse.of(group.getInviteCode());
    }

    @Transactional(readOnly = true)
    public InvitePreviewResponse preview(String code) {
        FamilyGroup group = byCode(code);
        long count = familyMemberRepository.countByFamilyGroupId(group.getId());
        String owner = memberRepository.findById(group.getOwnerMemberId()).map(Member::getNickname).orElse("");
        return new InvitePreviewResponse(group.getName(), owner, count, count >= FamilyGroup.MAX_MEMBERS);
    }

    public FamilyResponse accept(Long memberId, String code) {
        FamilyGroup target = byCode(code);
        FamilyMember me = access.membership(memberId);
        Long oldGroupId = me.getFamilyGroupId();
        if (oldGroupId.equals(target.getId())) {
            return toResponse(target, me);
        }
        if (familyMemberRepository.countByFamilyGroupId(target.getId()) >= FamilyGroup.MAX_MEMBERS) {
            throw new BusinessException(ErrorCode.FAMILY_FULL);
        }
        if (familyMemberRepository.countByFamilyGroupId(oldGroupId) > 1) {
            throw new BusinessException(ErrorCode.FAMILY_ALREADY_JOINED);
        }
        dogRepository.moveGroup(oldGroupId, target.getId());
        me = access.membership(memberId);   // moveGroup 이 영속성 컨텍스트를 비워서 다시 읽음
        me.moveTo(target.getId(), FamilyRole.EDITOR, now());
        groupRepository.deleteById(oldGroupId);
        return toResponse(target, me);
    }

    /** 관리자가 가족을 내보냄. 반려견은 남고 내보낸 사람은 새 개인 그룹 */
    public void kick(Long ownerId, Long targetMemberId) {
        FamilyMember owner = access.requireOwner(ownerId);
        FamilyMember target = sameGroupMember(owner, targetMemberId);
        if (target.getMemberId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "자기 자신은 내보낼 수 없습니다");
        }
        moveToNewPersonalGroup(target);
    }

    public void transferOwner(Long ownerId, Long targetMemberId) {
        FamilyMember owner = access.requireOwner(ownerId);
        FamilyMember target = sameGroupMember(owner, targetMemberId);
        if (target.getMemberId().equals(ownerId)) {
            return;
        }
        owner.changeRole(FamilyRole.EDITOR);
        target.changeRole(FamilyRole.OWNER);
        group(owner.getFamilyGroupId()).changeOwner(targetMemberId);
    }

    public void leave(Long memberId) {
        FamilyMember me = access.membership(memberId);
        long count = familyMemberRepository.countByFamilyGroupId(me.getFamilyGroupId());
        if (count == 1) {
            return;   // 이미 혼자인 그룹
        }
        if (me.isOwner()) {
            throw new BusinessException(ErrorCode.FAMILY_OWNER_MUST_TRANSFER);
        }
        moveToNewPersonalGroup(me);
    }

    private void moveToNewPersonalGroup(FamilyMember fm) {
        FamilyGroup group = newGroup(fm.getMemberId());
        fm.moveTo(group.getId(), FamilyRole.OWNER, now());
    }

    private FamilyMember sameGroupMember(FamilyMember owner, Long targetMemberId) {
        return familyMemberRepository.findByMemberId(targetMemberId)
                .filter(t -> t.getFamilyGroupId().equals(owner.getFamilyGroupId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_MEMBER_NOT_FOUND));
    }

    private FamilyGroup newGroup(Long ownerMemberId) {
        return groupRepository.save(new FamilyGroup(uniqueCode(), ownerMemberId));
    }

    private String uniqueCode() {
        String code;
        do {
            code = codeGenerator.next();
        } while (groupRepository.existsByInviteCode(code));
        return code;
    }

    private FamilyGroup byCode(String code) {
        return groupRepository.findByInviteCode(code == null ? "" : code.strip().toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));
    }

    private FamilyGroup group(Long groupId) {
        return groupRepository.findById(groupId).orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
    }

    private FamilyResponse toResponse(FamilyGroup group, FamilyMember me) {
        List<FamilyMember> members = familyMemberRepository.findByFamilyGroupIdOrderByJoinedAtAsc(group.getId());
        Map<Long, String> names = memberRepository.findAllById(members.stream().map(FamilyMember::getMemberId).toList())
                .stream().collect(Collectors.toMap(Member::getId, Member::getNickname, (a, b) -> a));
        List<FamilyResponse.MemberItem> items = members.stream()
                .map(m -> new FamilyResponse.MemberItem(m.getMemberId(), names.getOrDefault(m.getMemberId(), ""),
                        m.getRole(), m.getJoinedAt()))
                .toList();
        int dogCount = dogRepository.findByFamilyGroupIdOrderByIdAsc(group.getId()).size();
        return new FamilyResponse(group.getId(), group.getName(), me.getRole(), items, dogCount,
                FamilyGroup.MAX_MEMBERS);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
