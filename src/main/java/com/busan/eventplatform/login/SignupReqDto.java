package com.busan.eventplatform.login;

import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Getter
@Setter
@NoArgsConstructor
public class SignupReqDto {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private String email;
    private String password;
    private String passwordConfirm;
    private String displayName;

    public void validate() {
        if (email == null || email.isBlank()) {
            throw new Exception400("이메일을 입력해주세요.");
        }

        if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new Exception400("올바른 이메일 주소를 입력해주세요.");
        }

        if (password == null || password.isBlank()) {
            throw new Exception400("비밀번호를 입력해주세요.");
        }

        if (password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new Exception400("비밀번호는 8자 이상, UTF-8 기준 72바이트 이하로 입력해주세요.");
        }

        if (!password.equals(passwordConfirm)) {
            throw new Exception400("비밀번호 확인이 일치하지 않습니다.");
        }

        if (displayName == null || displayName.isBlank()) {
            throw new Exception400("이름을 입력해주세요.");
        }

        displayName = displayName.strip();

        if (displayName.length() > 50) {
            throw new Exception400("이름은 50자 이하로 입력해주세요.");
        }
    }
}
