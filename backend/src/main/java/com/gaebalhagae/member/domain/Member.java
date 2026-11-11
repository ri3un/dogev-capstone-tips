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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** SNS 로그인 회원. 자체 가입 없음, ADMIN은 DB에서 직접 지정 (결정 3) */
@Getter
@Entity
@Table(name = "member", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    public static final String WITHDRAWN_NICKNAME = "탈퇴한 회원";

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

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;   // 탈퇴 시각, null 이면 사용 중

    public Member(Provider provider, String providerId, String nickname) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.role = Role.USER;
    }

    /** 탈퇴: 닉네임·SNS 식별값을 지워 익명 처리 (같은 SNS로 다시 가입하면 새 회원) */
    public void withdraw(LocalDateTime now) {
        this.nickname = WITHDRAWN_NICKNAME;
        this.providerId = "withdrawn-" + id;
        this.deletedAt = now;
    }

    public boolean isWithdrawn() {
        return deletedAt != null;
    }

    /** 개발용 로그인(local)에서만 사용 */
    public void makeAdmin() {
        this.role = Role.ADMIN;
    }
}
