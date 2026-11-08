package com.gaebalhagae.family.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** 무작위 8자 초대 코드. 헷갈리는 글자(0·O·1·I·L)는 뺍니다 */
@Component
public class InviteCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
