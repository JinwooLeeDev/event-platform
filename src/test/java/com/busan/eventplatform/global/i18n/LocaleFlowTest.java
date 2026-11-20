package com.busan.eventplatform.global.i18n;

import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.*;
import com.busan.eventplatform.login.emailverification.EmailVerificationSession;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.dao.DataAccessResourceFailureException;
import com.busan.eventplatform.company.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {"spring.sql.init.mode=never", "spring.datasource.url=jdbc:h2:mem:locale-flow;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("h2")
class LocaleFlowTest {
    @Autowired WebApplicationContext context;
    @MockitoSpyBean UserRepository users;
    @Autowired CompanyRepository companies;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;
    static final String COOKIE = "BUSAN_PLAY_LOCALE";
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    @Test void firstVisitOffersLanguageWithoutBreakingLoginForm() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"locale-modal\"")))
                .andExpect(content().string(containsString("data-first-visit=\"true\"")))
                .andExpect(content().string(containsString("id=\"login-modal-form\"")));
    }

    @ParameterizedTest @ValueSource(strings = {"ko", "en", "ja", "zh-CN"})
    void choiceSurvivesNavigationAndBrowserRestart(String code) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/signup").session(session));
        var result = mvc.perform(post("/locale").session(session).param("locale", code)
                        .param("_localeCsrf", token(session)).param("returnTo", "/signup"))
                .andExpect(status().isSeeOther()).andExpect(header().string("Location", "/signup")).andReturn();
        String header = result.getResponse().getHeader("Set-Cookie");
        assertThat(header).contains(COOKIE + "=" + code, "Path=/", "Max-Age=31536000", "HttpOnly", "SameSite=Lax");
        mvc.perform(get("/login").cookie(new Cookie(COOKIE, code)))
                .andExpect(content().string(containsString("data-first-visit=\"false\"")))
                .andExpect(content().string(containsString(switch (code) {
                    case "en" -> "Choose your language";
                    case "ja" -> "言語を選択してください";
                    case "zh-CN" -> "请选择语言";
                    default -> "사용할 언어를 선택해주세요";
                })));
    }

    @Test void invalidCookieDefaultsToKoreanAndUnsupportedChoiceAndCsrfAreRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/login").session(session).cookie(new Cookie(COOKIE, "fr")))
                .andExpect(content().string(containsString("data-first-visit=\"true\"")));
        mvc.perform(post("/locale").session(session).param("locale", "en")).andExpect(status().isForbidden());
        mvc.perform(post("/locale").session(session).param("locale", "fr").param("_localeCsrf", token(session)))
                .andExpect(status().isBadRequest()).andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test void externalRedirectIsDiscardedAndHttpsCookieIsSecure() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/login").session(session));
        mvc.perform(post("/locale").secure(true).session(session).param("locale", "en")
                        .param("_localeCsrf", token(session)).param("returnTo", "//evil.example/path"))
                .andExpect(header().string("Location", "/"))
                .andExpect(header().string("Set-Cookie", containsString("Secure")));
    }

    @ParameterizedTest @ValueSource(strings = {"ko", "en", "ja", "zh-CN", "fr", "absent"})
    void signupUsesCookieAndKeepsEmailVerificationAndPasswordHashing(String code) throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        MockHttpSession session = new MockHttpSession();
        var verified = EmailVerificationSession.builder().email(email).sentAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        verified.markVerified(Instant.now()); session.setAttribute(Define.EMAIL_VERIFICATION, verified);
        var signup = post("/signup").session(session);
        if (!code.equals("absent")) signup.cookie(new Cookie(COOKIE, code));
        mvc.perform(signup
                        .param("email", email).param("password", "password123").param("passwordConfirm", "password123")
                        .param("displayName", UUID.randomUUID().toString()).param("preferredLocale", "fr"))
                .andExpect(redirectedUrl("/login"));
        User saved = users.findByEmailNormalized(email).orElseThrow();
        assertThat(saved.getPreferredLocale()).isEqualTo(code.equals("fr") || code.equals("absent") ? "ko" : code);
        assertThat(encoder.matches("password123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getEmailVerifiedAt()).isNotNull();
        assertThat(session.getAttribute(Define.EMAIL_VERIFICATION)).isNull();
    }

    @Test void loginRestoresDatabaseLocaleAndLogoutPreservesLanguageCookie() throws Exception {
        User user = user("ja"); MockHttpSession session = new MockHttpSession();
        var result = mvc.perform(post("/login").session(session).cookie(new Cookie(COOKIE, "en"))
                        .param("email", user.getEmailNormalized()).param("password", "password123"))
                .andExpect(redirectedUrl("/")).andExpect(cookie().value(COOKIE, "ja")).andReturn();
        assertThat(((SessionUserDto)session.getAttribute(LoginSession.USER)).getPreferredLocale()).isEqualTo("ja");
        mvc.perform(post("/logout").session(session)).andExpect(redirectedUrl("/login"))
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertThat(result.getResponse().getCookie(COOKIE).getMaxAge()).isPositive();
    }

    @Test void explicitChoiceOverridesDatabaseThenIsConsumedForNextAccount() throws Exception {
        User first = user("ja"), second = user("zh-CN"); MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/login").session(session));
        mvc.perform(post("/locale").session(session).param("locale", "en").param("_localeCsrf", token(session)))
                .andExpect(status().isSeeOther());
        mvc.perform(post("/login").session(session).cookie(new Cookie(COOKIE, "en"))
                        .param("email", first.getEmailNormalized()).param("password", "wrongpass"))
                .andExpect(status().isUnauthorized());
        assertThat(users.findById(first.getId()).orElseThrow().getPreferredLocale()).isEqualTo("ja");
        login(session, first, "en").andExpect(cookie().value(COOKIE, "en"));
        assertThat(users.findById(first.getId()).orElseThrow().getPreferredLocale()).isEqualTo("en");
        login(session, second, "en").andExpect(cookie().value(COOKIE, "zh-CN"));
    }

    @Test void authenticatedChangeUpdatesDatabaseSessionAndMypageCookie() throws Exception {
        User user = user("ko"); MockHttpSession session = new MockHttpSession(); login(session, user, "ko");
        mvc.perform(get("/mypage").session(session));
        mvc.perform(post("/locale").session(session).param("locale", "zh-CN").param("_localeCsrf", token(session)))
                .andExpect(status().isSeeOther()).andExpect(cookie().value(COOKIE, "zh-CN"));
        assertThat(users.findById(user.getId()).orElseThrow().getPreferredLocale()).isEqualTo("zh-CN");
        assertThat(((SessionUserDto)session.getAttribute(LoginSession.USER)).getPreferredLocale()).isEqualTo("zh-CN");
        mvc.perform(post("/mypage").session(session).param("_localeCsrf", token(session))
                        .param("displayName", user.getDisplayName()).param("preferredLocale", "en"))
                .andExpect(redirectedUrl("/mypage")).andExpect(cookie().value(COOKIE, "en"));
    }

    @Test void profileLanguageChangeAlsoRequiresCsrfToken() throws Exception {
        User user = user("ja"); MockHttpSession session = new MockHttpSession(); login(session, user, "ja");
        mvc.perform(post("/mypage").session(session).param("displayName", user.getDisplayName()).param("preferredLocale", "en"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Set-Cookie"));
        assertThat(users.findById(user.getId()).orElseThrow().getPreferredLocale()).isEqualTo("ja");
    }

    @Test void errorPageHasLanguageUiAndStaticResponsesDoNot() throws Exception {
        mvc.perform(get("/mypage")).andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("id=\"locale-modal\"")));
        mvc.perform(get("/favicon.ico")).andExpect(content().string(org.hamcrest.Matchers.not(containsString("locale-modal"))));
    }

    @Test void databaseFailureRollsBackAndPreservesCookieAndSession() throws Exception {
        User user = user("ja"); MockHttpSession session = new MockHttpSession(); login(session, user, "ja");
        mvc.perform(get("/mypage").session(session));
        Object previous = session.getAttribute(LoginSession.USER);
        MockHttpServletRequest request = new MockHttpServletRequest(); request.setSession(session);
        request.setCookies(new Cookie(COOKIE, "ja"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(new DataAccessResourceFailureException("simulated database failure")).when(users).flush();
        assertThatThrownBy(() -> context.getBean(LocaleController.class).change("en", token(session), "/mypage", request, response))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(users.findById(user.getId()).orElseThrow().getPreferredLocale()).isEqualTo("ja");
        assertThat(session.getAttribute(LoginSession.USER)).isSameAs(previous);
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    }

    @Test void loginLocaleFailureDoesNotEstablishAuthenticationOrConsumeChoice() throws Exception {
        User user = user("ja"); MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/login").session(session));
        mvc.perform(post("/locale").session(session).param("locale", "en").param("_localeCsrf", token(session)));
        doThrow(new DataAccessResourceFailureException("simulated database failure")).when(users).flush();
        assertThatThrownBy(() -> login(session, user, "en")).hasRootCauseInstanceOf(DataAccessResourceFailureException.class);
        assertThat(session.getAttribute(LoginSession.IDENTITY)).isNull();
        assertThat(LocaleSelectionState.choice(session)).isEqualTo("en");
        assertThat(users.findById(user.getId()).orElseThrow().getPreferredLocale()).isEqualTo("ja");
    }

    @Test void invalidDatabaseLanguageFallsBackToCookieThenKorean() throws Exception {
        User user = user("fr");
        login(new MockHttpSession(), user, "en").andExpect(cookie().value(COOKIE, "en"));
        login(new MockHttpSession(), user, "invalid").andExpect(cookie().value(COOKIE, "ko"));
    }

    @Test void signupWithoutVerificationFailsAndDoesNotCreateUser() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/signup").cookie(new Cookie(COOKIE, "en"))
                        .param("email", email).param("password", "password123").param("passwordConfirm", "password123")
                        .param("displayName", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest()).andExpect(header().doesNotExist("Set-Cookie"));
        assertThat(users.findByEmailNormalized(email)).isEmpty();
    }

    @Test void companyLoginPreservesCookieAndConsumesChoiceWithoutTouchingPersonalPreferences() throws Exception {
        String id = UUID.randomUUID().toString();
        Company company = companies.saveAndFlush(Company.builder().name("Locale company").email(id + "@example.test")
                .password(encoder.encode("password123")).businessNumber("1234567890")
                .representativeName("Owner").openedOn(java.time.LocalDate.of(2020, 1, 1)).address("Busan")
                .contactPersonName("Contact").contactPersonPhone("01012345678").status(CompanyStatus.APPROVED).build());
        MockHttpSession session = new MockHttpSession(); mvc.perform(get("/login").session(session));
        mvc.perform(post("/locale").session(session).param("locale", "en").param("_localeCsrf", token(session)));
        mvc.perform(post("/login/company").session(session).cookie(new Cookie(COOKIE, "en"))
                        .param("email", company.getEmail()).param("password", "password123"))
                .andExpect(redirectedUrl("/")).andExpect(header().doesNotExist("Set-Cookie"));
        assertThat(LoginSession.requireIdentity(session).isCompany()).isTrue();
        assertThat(session.getAttribute(LoginSession.USER)).isNull();
        assertThat(LocaleSelectionState.choice(session)).isNull();
        mvc.perform(post("/locale").session(session).param("locale", "ja").param("_localeCsrf", token(session)))
                .andExpect(cookie().value(COOKIE, "ja"));
        User personal = user("zh-CN");
        login(session, personal, "ja").andExpect(cookie().value(COOKIE, "zh-CN"));
        assertThat(session.getAttribute(LoginSession.COMPANY)).isNull();
    }

    @Test void emailVerificationApiRetainsJsonAndNeverGetsLanguageMarkup() throws Exception {
        mvc.perform(post("/api/v1/email-verifications/confirm").contentType("application/json")
                        .accept("application/json").content("{\"email\":\"test@example.test\",\"code\":\"123456\"}"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("locale-modal"))));
        mvc.perform(get("/css/locale.css")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));
    }

    @ParameterizedTest @ValueSource(strings = {"/css/missing.css", "/js/missing.js", "/api/missing", "/images/missing.png"})
    void frameworkErrorsForResourcesAndApiNeverContainLanguageModal(String originalPath) throws Exception {
        mvc.perform(get("/error").requestAttr("jakarta.servlet.error.request_uri", originalPath)
                        .requestAttr("jakarta.servlet.error.status_code", 404).accept("text/html"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"locale-modal\""))));
    }

    private String token(MockHttpSession session) { return (String)session.getAttribute("BUSAN_PLAY_LOCALE_CSRF"); }
    private User user(String locale) {
        String id = UUID.randomUUID().toString();
        return users.saveAndFlush(User.builder().emailNormalized(id + "@example.test").displayName(id)
                .passwordHash(encoder.encode("password123")).preferredLocale(locale).role(Role.MEMBER).build());
    }
    private org.springframework.test.web.servlet.ResultActions login(MockHttpSession session, User user, String cookie) throws Exception {
        return mvc.perform(post("/login").session(session).cookie(new Cookie(COOKIE, cookie))
                .param("email", user.getEmailNormalized()).param("password", "password123")).andExpect(redirectedUrl("/"));
    }
}
