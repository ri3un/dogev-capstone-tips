package com.gaebalhagae.member.dto;

import com.gaebalhagae.member.domain.Member;
import com.gaebalhagae.member.domain.Provider;
import com.gaebalhagae.member.domain.Role;

public record MeResponse(Long id, String nickname, Provider provider, Role role) {

    public static MeResponse from(Member m) {
        return new MeResponse(m.getId(), m.getNickname(), m.getProvider(), m.getRole());
    }
}
