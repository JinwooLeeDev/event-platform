package com.busan.eventplatform.login;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class SessionUserDto {

    // Todo - ENUM Type Role 정의 후 주석 해제
//    private final Long id;
//    private final String displayName;
//    private final Role role;
//    private final String preferredLocale;
//
//    public boolean isAdmin() {
//        return role == Role.ADMIN;
//    }
//
//    public boolean isMember() {
//        return role == Role.MEMBER;
//    }
}
