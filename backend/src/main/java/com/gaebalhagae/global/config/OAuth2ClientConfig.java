package com.gaebalhagae.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * 카카오·네이버 OAuth2 등록. yml 자동 설정은 .env 에 빈 키가 있으면 서버가 아예 안 떠서 코드로 만듭니다.
 * 키가 비어 있으면 "none"으로 등록해 서버는 뜨고 그 로그인만 실패합니다.
 */
@Configuration
public class OAuth2ClientConfig {

    private static final String REDIRECT = "{baseUrl}/login/oauth2/code/{registrationId}";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.oauth.kakao-client-id:}") String kakaoId,
            @Value("${app.oauth.kakao-client-secret:}") String kakaoSecret,
            @Value("${app.oauth.naver-client-id:}") String naverId,
            @Value("${app.oauth.naver-client-secret:}") String naverSecret) {
        ClientRegistration kakao = ClientRegistration.withRegistrationId("kakao")
                .clientId(orNone(kakaoId))
                .clientSecret(orNone(kakaoSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(REDIRECT)
                .scope("profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
        ClientRegistration naver = ClientRegistration.withRegistrationId("naver")
                .clientId(orNone(naverId))
                .clientSecret(orNone(naverSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(REDIRECT)
                .scope("nickname")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me")
                .userNameAttributeName("response")
                .clientName("Naver")
                .build();
        return new InMemoryClientRegistrationRepository(kakao, naver);
    }

    private static String orNone(String value) {
        return value == null || value.isBlank() ? "none" : value.strip();
    }
}
