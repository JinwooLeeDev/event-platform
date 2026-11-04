package com.busan.eventplatform.emailverification;

import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailVerificationConfirmReqDto {

    private String email;
    private String code;

    public void validate() {
        email = EmailVerificationSendReqDto.validateEmail(email);

        if (code == null || code.isBlank()) {
            throw new Exception400("인증번호를 입력해주세요.");
        }

        code = code.strip();

        if (!code.matches("[0-9]{6}")) {
            throw new Exception400("인증번호는 숫자 6자리로 입력해주세요.");
        }
    }
}
