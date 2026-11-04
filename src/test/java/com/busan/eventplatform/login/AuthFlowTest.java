package com.busan.eventplatform.login;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.GlobalExceptionHandler;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.emailverification.EmailVerificationSession;
import com.busan.eventplatform.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@ActiveProfiles("h2")
class AuthFlowTest {
    @Autowired AuthService authService;
    @Autowired AuthController controller;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void signupAndLoginPreserveEmailAndConsumeVerification() throws Exception {
        String email = "MixedCase-" + UUID.randomUUID() + "@Example.com";
        MockHttpSession session = verifiedSession(email, Instant.now());
        Instant verifiedAt = ((EmailVerificationSession) session.getAttribute(Define.EMAIL_VERIFICATION)).getVerifiedAt();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/signup").session(session)
                        .param("email", email).param("password", "password123")
                        .param("passwordConfirm", "password123").param("displayName", "테스트회원"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        User saved = users.findByEmailNormalized(email).orElseThrow();
        assertThat(saved.getEmailNormalized()).isEqualTo(email);
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(encoder.matches("password123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getRole()).isEqualTo(Role.MEMBER);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getEmailVerifiredAt()).isCloseTo(verifiedAt, within(1, java.time.temporal.ChronoUnit.MILLIS));
        assertThat(session.getAttribute(Define.EMAIL_VERIFICATION)).isNull();

        String oldSessionId = session.getId();
        mvc.perform(post("/login").session(session).param("email", email).param("password", "password123"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        assertThat(session.getId()).isNotEqualTo(oldSessionId);
        SessionUserDto loggedIn = (SessionUserDto) session.getAttribute(AuthController.SESSION_USER);
        assertThat(loggedIn.getId()).isEqualTo(saved.getId());
        assertThat(loggedIn.getDisplayName()).isEqualTo("테스트회원");
        mvc.perform(post("/logout").session(session))
                .andExpect(redirectedUrl("/login"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void unverifiedAndExpiredSignupAreRejected() {
        SignupReqDto request = signupRequest();
        assertThatThrownBy(() -> authService.signup(request, new MockHttpSession()))
                .isInstanceOf(Exception400.class);
        assertThatThrownBy(() -> authService.signup(request,
                verifiedSession(request.getEmail(), Instant.now().minusSeconds(601))))
                .isInstanceOf(Exception400.class);
        assertThat(users.existsByEmailNormalized(request.getEmail())).isFalse();
    }

    @Test
    void duplicateSignupAndIncorrectPasswordAreRejected() {
        SignupReqDto request = signupRequest();
        authService.signup(request, verifiedSession(request.getEmail(), Instant.now()));
        assertThatThrownBy(() -> authService.signup(request, verifiedSession(request.getEmail(), Instant.now())))
                .isInstanceOf(Exception400.class);
        LoginReqDto login = new LoginReqDto();
        login.setEmail(request.getEmail());
        login.setPassword("wrong-password");
        assertThatThrownBy(() -> authService.login(login)).isInstanceOf(Exception401.class);
    }

    @Test
    void restrictedAndWithdrawnAccountsCannotLogin() {
        for (UserStatus state : new UserStatus[]{UserStatus.SUSPENDED, UserStatus.WITHDRAWN}) {
            String email = UUID.randomUUID() + "@example.com";
            users.saveAndFlush(User.builder().emailNormalized(email).passwordHash(encoder.encode("password123"))
                    .displayName("제한회원").status(state).build());
            LoginReqDto login = new LoginReqDto();
            login.setEmail(email);
            login.setPassword("password123");
            assertThatThrownBy(() -> authService.login(login)).isInstanceOf(Exception403.class);
        }
    }

    private SignupReqDto signupRequest() {
        SignupReqDto request = new SignupReqDto();
        request.setEmail(UUID.randomUUID() + "@example.com");
        request.setPassword("password123");
        request.setPasswordConfirm("password123");
        request.setDisplayName("테스트회원");
        return request;
    }

    private MockHttpSession verifiedSession(String email, Instant verifiedAt) {
        EmailVerificationSession verification = EmailVerificationSession.builder().email(email)
                .sentAt(verifiedAt).expiresAt(verifiedAt.plusSeconds(300)).codeHash("unused").build();
        verification.markVerified(verifiedAt);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Define.EMAIL_VERIFICATION, verification);
        return session;
    }
}
