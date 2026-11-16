package com.busan.eventplatform.admin;

import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.interceptor.AdminInterceptor;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.LoginType;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AdminAccessTest {
    private final AdminInterceptor interceptor = new AdminInterceptor();

    private MockHttpServletRequest request(LoginType type, Long identityId, Long userId, Role role) {
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(LoginSession.IDENTITY,
                LoginIdentity.builder().id(identityId).loginType(type).build());
        request.getSession().setAttribute(LoginSession.USER,
                SessionUserDto.builder().id(userId).role(role).build());
        return request;
    }

    @Test
    void anonymousRequestIsRejectedWithoutCreatingSession() {
        var request = new MockHttpServletRequest();
        assertThrows(Exception401.class, () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertNull(request.getSession(false));
    }

    @Test
    void memberAndCompanyCannotAccessEvenWithLeftoverAdminSession() {
        assertThrows(Exception403.class, () -> interceptor.preHandle(
                request(LoginType.USER, 1L, 1L, Role.MEMBER), new MockHttpServletResponse(), new Object()));
        assertThrows(Exception403.class, () -> interceptor.preHandle(
                request(LoginType.COMPANY, 1L, 1L, Role.ADMIN), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void mismatchedOrMissingIdentityCannotUseAdminSession() {
        assertThrows(Exception401.class, () -> interceptor.preHandle(
                request(LoginType.USER, 1L, 2L, Role.ADMIN), new MockHttpServletResponse(), new Object()));
        var request = request(LoginType.USER, 1L, 1L, Role.ADMIN);
        request.getSession().removeAttribute(LoginSession.IDENTITY);
        assertThrows(Exception401.class, () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void matchingAdminCanAccess() throws Exception {
        assertTrue(interceptor.preHandle(request(LoginType.USER, 1L, 1L, Role.ADMIN),
                new MockHttpServletResponse(), new Object()));
    }
}
