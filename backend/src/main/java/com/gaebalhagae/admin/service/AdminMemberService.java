package com.gaebalhagae.admin.service;

import com.gaebalhagae.admin.dto.AdminMemberDtos.DataCount;
import com.gaebalhagae.admin.dto.AdminMemberDtos.MemberDetail;
import com.gaebalhagae.admin.dto.AdminMemberDtos.MemberList;
import com.gaebalhagae.admin.dto.AdminMemberDtos.MemberRow;
import com.gaebalhagae.family.domain.FamilyGroup;
import com.gaebalhagae.family.repository.FamilyGroupRepository;
import com.gaebalhagae.family.repository.FamilyMemberRepository;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.member.domain.Member;
import com.gaebalhagae.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 회원 현황. 회원이 입력한 데이터는 개수와 마지막 입력일만 셉니다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    /** 셀 항목: 키, 화면 이름, 엔티티, 작성자 필드 */
    private record Source(String key, String label, String entity, String writer) {
    }

    private static final List<Source> SOURCES = List.of(
            new Source("dog", "반려견 등록", "Dog", "createdBy"),
            new Source("record", "건강 기록", "HealthRecord", "createdBy"),
            new Source("consult", "AI 상담", "Consultation", "createdBy"),
            new Source("course", "코스 추천", "Course", "createdBy"),
            new Source("question", "수의사 질문", "VetQuestion", "createdBy"),
            new Source("editRequest", "장소 수정 요청", "PlaceEditRequest", "memberId"));

    private record Stat(long count, LocalDateTime lastAt) {
    }

    private final EntityManager em;
    private final MemberRepository memberRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyGroupRepository familyGroupRepository;

    public MemberList list() {
        Map<String, Map<Long, Stat>> stats = allStats();
        List<MemberRow> rows = memberRepository.findAll().stream()
                .map(m -> row(m, stats))
                .sorted(Comparator.comparing(MemberRow::joinedAt).reversed())   // 최근 가입 순
                .toList();
        long withdrawn = rows.stream().filter(MemberRow::withdrawn).count();
        return new MemberList(rows.size(), rows.size() - withdrawn, withdrawn, rows);
    }

    public MemberDetail detail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Map<String, Map<Long, Stat>> stats = allStats();
        List<DataCount> counts = SOURCES.stream().map(s -> {
            Stat st = stats.get(s.key()).getOrDefault(memberId, new Stat(0, null));
            return new DataCount(s.key(), s.label(), st.count(), st.lastAt());
        }).toList();
        var membership = familyMemberRepository.findByMemberId(memberId);
        var group = membership.flatMap(fm -> familyGroupRepository.findById(fm.getFamilyGroupId()));
        return new MemberDetail(row(member, stats),
                group.map(FamilyGroup::getName).orElse(null),
                membership.map(fm -> fm.getRole().name()).orElse(null),
                membership.map(fm -> (int) familyMemberRepository.countByFamilyGroupId(fm.getFamilyGroupId())).orElse(null),
                counts);
    }

    private MemberRow row(Member m, Map<String, Map<Long, Stat>> stats) {
        long total = 0;
        LocalDateTime last = null;
        for (Source s : SOURCES) {
            Stat st = stats.get(s.key()).get(m.getId());
            if (st == null) {
                continue;
            }
            total += st.count();
            last = latest(last, st.lastAt());
        }
        return new MemberRow(m.getId(), m.getNickname(), m.getProvider(), m.getRole(), m.getCreatedAt(),
                m.isWithdrawn(), total, last);
    }

    // 항목마다 group by 한 번 (회원 수만큼 쿼리하지 않음)
    private Map<String, Map<Long, Stat>> allStats() {
        Map<String, Map<Long, Stat>> result = new HashMap<>();
        for (Source s : SOURCES) {
            String jpql = "select e.%s, count(e), max(e.createdAt) from %s e group by e.%s"
                    .formatted(s.writer(), s.entity(), s.writer());
            Map<Long, Stat> byMember = new HashMap<>();
            for (Object[] r : em.createQuery(jpql, Object[].class).getResultList()) {
                byMember.put((Long) r[0], new Stat(((Number) r[1]).longValue(), (LocalDateTime) r[2]));
            }
            result.put(s.key(), byMember);
        }
        return result;
    }

    private static LocalDateTime latest(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) {
            return a == null ? b : a;
        }
        return a.isAfter(b) ? a : b;
    }
}
