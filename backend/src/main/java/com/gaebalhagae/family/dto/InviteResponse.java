package com.gaebalhagae.family.dto;

/** invitePath 는 프론트 주소 뒤에 붙여 링크로 공유합니다 (예: https://도메인/invite/ABCD2345) */
public record InviteResponse(String inviteCode, String invitePath) {

    public static InviteResponse of(String code) {
        return new InviteResponse(code, "/invite/" + code);
    }
}
