package com.busan.eventplatform.global.util;

import java.security.SecureRandom;

public class MailUtil {

    // 예측하기 어려운 난수 생성기 (보안 용도)
    private static final SecureRandom RANDOM = new SecureRandom();

    // 6자리 인증번호 생성 (100000 ~ 999999)
    public static String generateCode() {
        int code = 100000 + RANDOM.nextInt(900000); // nextInt(900000) : 0 ~ 899999
        return String.valueOf(code);
    }
}