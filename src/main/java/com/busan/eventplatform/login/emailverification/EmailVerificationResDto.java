package com.busan.eventplatform.login.emailverification;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerificationResDto {

    private final String message;
    private final boolean verified;
}
/*
        new EmailVerificationResDto("인증번호가 발송되었습니다. 5분 안에 입력해주세요.",false);

        new EmailVerificationResDto("이메일 인증이 완료되었습니다.",true);
*/