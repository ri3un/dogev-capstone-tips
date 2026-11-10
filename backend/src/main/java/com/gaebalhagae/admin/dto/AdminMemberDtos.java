package com.gaebalhagae.admin.dto;

import com.gaebalhagae.member.domain.Provider;
import com.gaebalhagae.member.domain.Role;
import java.time.LocalDateTime;
import java.util.List;

/** 관리자 회원 현황: 입력 내용은 보내지 않고 항목별 개수·마지막 입력일만 (결정: 개인정보 최소화) */
public final class AdminMemberDtos {

    private AdminMemberDtos() {
    }

    /** 항목 하나의 개수와 마지막 입력 시각 */
    public record DataCount(String key, String label, long count, LocalDateTime lastAt) {
    }

    /** 목록 한 줄 */
    public record MemberRow(Long id, String nickname, Provider provider, Role role, LocalDateTime joinedAt,
                            boolean withdrawn, long totalCount, LocalDateTime lastActivityAt) {
    }

    /** 목록 + 요약 */
    public record MemberList(long memberCount, long activeCount, long withdrawnCount, List<MemberRow> members) {
    }

    /** 자세히 보기: 가족 그룹 정보와 항목별 개수 */
    public record MemberDetail(MemberRow member, String familyName, String familyRole, Integer familySize,
                               List<DataCount> counts) {
    }
}
