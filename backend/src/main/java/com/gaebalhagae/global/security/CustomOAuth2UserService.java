package com.gaebalhagae.global.security;

import com.gaebalhagae.member.domain.Member;
import com.gaebalhagae.member.domain.Provider;
import com.gaebalhagae.member.service.MemberService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/** 카카오·네이버 사용자 정보에서 provider_id·닉네임만 꺼내 회원으로 저장합니다 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    static final String MEMBER_ID = "memberId";
    static final String ROLE = "role";

    private final MemberService memberService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();
        Member member = switch (registrationId) {
            case "kakao" -> kakao(user.getAttributes());
            case "naver" -> naver(user.getAttributes());
            default -> throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"));
        };
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())),
                Map.of(MEMBER_ID, member.getId(), ROLE, member.getRole().name()),
                MEMBER_ID);
    }

    @SuppressWarnings("unchecked")
    private Member kakao(Map<String, Object> attrs) {
        Map<String, Object> props = (Map<String, Object>) attrs.getOrDefault("properties", Map.of());
        return memberService.login(Provider.KAKAO, String.valueOf(attrs.get("id")), (String) props.get("nickname"));
    }

    @SuppressWarnings("unchecked")
    private Member naver(Map<String, Object> attrs) {
        Map<String, Object> res = (Map<String, Object>) attrs.getOrDefault("response", Map.of());
        return memberService.login(Provider.NAVER, String.valueOf(res.get("id")), (String) res.get("nickname"));
    }
}
