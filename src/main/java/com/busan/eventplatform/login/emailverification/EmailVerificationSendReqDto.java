package com.busan.eventplatform.login.emailverification;

import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Getter
@NoArgsConstructor
public class EmailVerificationSendReqDto {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private String email;

    public void validate() {
        email = validateEmail(email);
    }

    static String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new Exception400("이메일을 입력해주세요.");
        }

        String trimmedEmail = email.strip();

        if (trimmedEmail.length() > 254
                || !EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new Exception400("올바른 이메일 주소를 입력해주세요.");
        }

        return trimmedEmail;
    }
}