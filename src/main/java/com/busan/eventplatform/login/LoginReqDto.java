package com.busan.eventplatform.login;

import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginReqDto {

    private String email;
    private String password;

    public void validate() {
        if (email == null || email.isBlank()) {
            throw new Exception400("이메일을 입력해주세요.");
        }

        if (!password.matches(
                "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@!%*#?&])[A-Za-z\\d$@!%*#?&]{8,16}$"
        )) {
            throw new Exception400(
                    "비밀번호는 영문·숫자·특수문자($@!%*#?&)를 모두 포함한 8~16자로 입력해주세요."
            );
        }

        if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new Exception400("올바른 이메일 주소를 입력해주세요.");
        }

        if (password == null || password.isBlank()) {
            throw new Exception400("비밀번호를 입력해주세요.");
        }
    }
}
