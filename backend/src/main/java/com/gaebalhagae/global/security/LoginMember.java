package com.gaebalhagae.global.security;

import com.gaebalhagae.member.domain.Role;

/** 토큰에서 꺼낸 로그인 정보. 컨트롤러에서 @AuthenticationPrincipal LoginMember me 로 받습니다. 가족 정보는 넣지 않음 */
public record LoginMember(Long id, Role role) {
}
