package com.busan.eventplatform.login.mypage;

import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class MyPageUpdateReqDto {

    private static final Set<String> ALLOWED_LOCALES =
            Set.of("ko", "en", "ja", "zh-CN");

    private String displayName;
    private String preferredLocale;

    public void validate() {
        if (displayName == null || displayName.isBlank()) {
            throw new Exception400("이름을 입력해주세요.");
        }

        displayName = displayName.strip();

        if (displayName.length() > 100) {
            throw new Exception400("이름은 100자 이하로 입력해주세요.");
        }

        if (preferredLocale == null
                || !ALLOWED_LOCALES.contains(preferredLocale)) {
            throw new Exception400("지원하는 언어를 선택해주세요.");
        }
    }
}