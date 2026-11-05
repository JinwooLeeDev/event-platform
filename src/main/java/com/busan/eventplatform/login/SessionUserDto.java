package com.busan.eventplatform.login;

import com.busan.eventplatform.user.Role;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class SessionUserDto {

    private final Long id;
    private final String displayName;
    private final Role role;
    private final String preferredLocale;

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isMember() {
        return role == Role.MEMBER;
    }
}
