package com.gaebalhagae.member.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** SNS 로그인 회원. 자체 가입 없음, ADMIN은 DB에서 직접 지정 (결정 3) */
@Getter
@Entity
@Table(name = "member", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Provider provider;

    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    public Member(Provider provider, String providerId, String nickname) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.role = Role.USER;
    }

    /** 개발용 로그인(local)에서만 사용 */
    public void makeAdmin() {
        this.role = Role.ADMIN;
    }
}
