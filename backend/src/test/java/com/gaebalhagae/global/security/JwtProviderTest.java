package com.gaebalhagae.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.member.domain.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key-0123456789";
    private final Clock now = Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void 발급한_토큰에서_회원_번호와_권한을_되찾는다() {
        JwtProvider provider = new JwtProvider(SECRET, 168, now);

        String token = provider.issue(7L, Role.ADMIN);

        assertThat(provider.parse(token)).contains(new LoginMember(7L, Role.ADMIN));
    }

    @Test
    void 다른_키로_서명한_토큰은_거부한다() {
        String token = new JwtProvider("another-secret-another-secret-another-secret-0000", 168, now).issue(1L, Role.USER);

        assertThat(new JwtProvider(SECRET, 168, now).parse(token)).isEmpty();
    }

    @Test
    void 만료된_토큰은_거부한다() {
        String token = new JwtProvider(SECRET, 1, now).issue(1L, Role.USER);
        Clock later = Clock.offset(now, Duration.ofHours(2));

        assertThat(new JwtProvider(SECRET, 1, later).parse(token)).isEmpty();
    }

    @Test
    void 키가_없으면_임시_키로도_동작한다() {
        JwtProvider provider = new JwtProvider("", 168, now);

        assertThat(provider.parse(provider.issue(3L, Role.USER))).isPresent();
        assertThat(provider.parse("not-a-token")).isEmpty();
    }
}
