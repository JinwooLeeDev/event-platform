package com.busan.eventplatform.login.mypage;

import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
@NoArgsConstructor
public class MyPasswordUpdateReqDto {
    private String currentPassword;
    private String newPassword;
    private String newPasswordConfirm;

    public void validate() {
        if (currentPassword == null || currentPassword.isBlank() || newPassword == null
            || newPassword.isBlank() || newPasswordConfirm == null || newPasswordConfirm.isBlank()) {
            throw new Exception400("비밀번호를 입력해주세요.");
        }

        if (newPassword.length() < 8
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new Exception400("비밀번호는 8자 이상, UTF-8 기준 72바이트 이하로 입력해주세요.");
        }

        if (!newPassword.equals(newPasswordConfirm)) {
            throw new Exception400("비밀번호 확인란이 일치하지 않습니다.");
        }
    }
}
