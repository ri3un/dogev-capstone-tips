package com.gaebalhagae.family.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 가족 그룹. 반려견의 주인은 회원이 아니라 이 그룹입니다 (결정 26) */
@Getter
@Entity
@Table(name = "family_group")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyGroup extends BaseTimeEntity {

    public static final int MAX_MEMBERS = 6;
    public static final String DEFAULT_NAME = "우리 가족";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "invite_code", nullable = false, unique = true, length = 8)
    private String inviteCode;

    @Column(name = "owner_member_id", nullable = false)
    private Long ownerMemberId;

    public FamilyGroup(String inviteCode, Long ownerMemberId) {
        this.name = DEFAULT_NAME;
        this.inviteCode = inviteCode;
        this.ownerMemberId = ownerMemberId;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeOwner(Long memberId) {
        this.ownerMemberId = memberId;
    }

    public void changeInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}
