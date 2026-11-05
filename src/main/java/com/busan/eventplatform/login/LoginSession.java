package com.busan.eventplatform.login;

import com.busan.eventplatform.global.error.exception.Exception401;
import jakarta.servlet.http.HttpSession;

public final class LoginSession {

    public static final String IDENTITY = "loginIdentity";
    public static final String USER = "sessionUser";
    public static final String COMPANY = "sessionCompany";

    private LoginSession() {
    }

    public static LoginIdentity requireIdentity(HttpSession session) {
        if (session == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }

        Object value = session.getAttribute(IDENTITY);

        if (!(value instanceof LoginIdentity identity)
                || identity.getId() == null
                || identity.getLoginType() == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }

        return identity;
    }
}