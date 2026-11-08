package com.gaebalhagae.family.dto;

import com.gaebalhagae.family.domain.FamilyRole;
import java.time.LocalDateTime;
import java.util.List;

public record FamilyResponse(Long id, String name, FamilyRole myRole, List<MemberItem> members, int dogCount,
                             int maxMembers) {

    public record MemberItem(Long memberId, String nickname, FamilyRole role, LocalDateTime joinedAt) {
    }
}
