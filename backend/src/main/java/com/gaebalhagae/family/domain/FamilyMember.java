package com.gaebalhagae.family.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회원의 가족 그룹 소속. member_id UNIQUE: 한 사람은 그룹 하나에만 속함 */
@Getter
@Entity
@Table(name = "family_member", indexes = @Index(columnList = "family_group_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_group_id", nullable = false)
    private Long familyGroupId;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FamilyRole role;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    public FamilyMember(Long familyGroupId, Long memberId, FamilyRole role, LocalDateTime joinedAt) {
        this.familyGroupId = familyGroupId;
        this.memberId = memberId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public void moveTo(Long familyGroupId, FamilyRole role, LocalDateTime joinedAt) {
        this.familyGroupId = familyGroupId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public void changeRole(FamilyRole role) {
        this.role = role;
    }

    public boolean isOwner() {
        return role == FamilyRole.OWNER;
    }
}
