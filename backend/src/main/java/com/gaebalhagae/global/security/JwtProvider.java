package com.gaebalhagae.global.security;

import com.gaebalhagae.member.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtProvider {

    private static final String ROLE = "role";

    private final SecretKey key;
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public JwtProvider(@Value("${app.jwt.secret:}") String secret,
                       @Value("${app.jwt.expire-hours:168}") long expireHours,
                       Clock clock,
                       @Value("#{environment.matchesProfiles('prod')}") boolean prod) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            if (prod) {
                // 배포에서 임시 키를 쓰면 재배포할 때마다 전원 로그아웃되므로 시작을 막음
                throw new IllegalStateException("prod 에서는 32바이트 이상의 JWT_SECRET 이 필요합니다");
            }
            log.warn("JWT_SECRET 이 없거나 32바이트보다 짧아 임시 키를 사용합니다. 서버를 재시작하면 로그인이 풀립니다.");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.ttl = Duration.ofHours(expireHours);
        this.clock = clock;
    }

    public String issue(Long memberId, Role role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** 위조·만료·형식 오류면 빈 값 */
    public Optional<LoginMember> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new LoginMember(Long.valueOf(claims.getSubject()),
                    Role.valueOf(claims.get(ROLE, String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
