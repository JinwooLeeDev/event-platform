package com.busan.eventplatform.login.mypage;

import com.busan.eventplatform.global.util.MyDateUtil;
import com.busan.eventplatform.user.User;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
@Builder
public class MyPageResDto {

    private final String email;
    private final String displayName;
    private final String preferredLocale;
    private final String createdAt;
    private final int point;


    public static MyPageResDto from(User user) {
        return MyPageResDto.builder()
                .email(user.getEmailNormalized())
                .displayName(user.getDisplayName())
                .preferredLocale(
                        user.getPreferredLocale() == null
                                ? "ko" // 언어 기본값은 ko로 설정
                                : user.getPreferredLocale()
                )
                .createdAt(
                        user.getCreatedAt() == null
                                ? "-"
                                : MyDateUtil.timestampFormat(
                                Timestamp.from(user.getCreatedAt())
                        )
                )
                .point(user.getPoint())
                .build();
    }
}