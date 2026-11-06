package com.busan.eventplatform.login.companylogin;

import com.busan.eventplatform.company.*;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.emailverification.EmailVerificationSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.LoginType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@ActiveProfiles("h2")
class CompanySignupMigrationTest {
    private static final Path STORAGE_ROOT = createStorageRoot();

    @DynamicPropertySource
    static void fileStorageSettings(DynamicPropertyRegistry registry) {
        registry.add("app.file.upload-dir", () -> STORAGE_ROOT.toString());
    }

    private static Path createStorageRoot() {
        try {
            return Files.createTempDirectory("company-signup-verification-");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
    @Autowired CompanyAuthService authService;
    @Autowired CompanyAuthController controller;
    @Autowired CompanyRepository companies;
    @Autowired PasswordEncoder encoder;
    @Autowired WebApplicationContext webContext;

    @Test
    void multipartSignupBindsFieldsAndFileFromNewScreen() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(webContext).build();
        mvc.perform(get("/signup/company"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(containsString("name=\"passwordConfirm\"")));
        CompanySignupReqDto request = request();
        MockHttpSession session = verifiedSession(request.getEmail());
        mvc.perform(multipart("/signup/company")
                        .file((MockMultipartFile) request.getBusinessCertificate()).session(session)
                        .param("email", request.getEmail()).param("password", request.getPassword())
                        .param("passwordConfirm", request.getPasswordConfirm()).param("name", request.getName())
                        .param("businessNumber", request.getBusinessNumber())
                        .param("representativeName", request.getRepresentativeName())
                        .param("openedOn", request.getOpenedOn().toString()).param("address", request.getAddress())
                        .param("contactPersonName", request.getContactPersonName())
                        .param("contactPersonPhone", request.getContactPersonPhone())
                        .param("status", "APPROVED").param("point", "999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?type=company"));
        Company saved = companies.findByEmail(request.getEmail()).orElseThrow();
        assertThat(saved.getBusinessCertificateFileName()).isNotBlank();
        assertThat(saved.getStatus()).isEqualTo(CompanyStatus.PENDING);
        assertThat(saved.getPoint()).isZero();
    }

    @Test
    void corporateLoginAndCompanyPagesShareCommonSession() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(webContext).build();
        CompanySignupReqDto request = request();
        Company company = Company.builder().name(request.getName()).email(request.getEmail())
                .password(encoder.encode(request.getPassword())).businessNumber(request.getBusinessNumber())
                .representativeName(request.getRepresentativeName()).openedOn(request.getOpenedOn())
                .address(request.getAddress()).contactPersonName(request.getContactPersonName())
                .contactPersonPhone(request.getContactPersonPhone()).status(CompanyStatus.APPROVED).build();
        companies.saveAndFlush(company);
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/company/login").session(session)
                        .param("email", request.getEmail()).param("password", request.getPassword()))
                .andExpect(redirectedUrl("/"));
        LoginIdentity identity = LoginSession.requireIdentity(session);
        assertThat(identity.getLoginType()).isEqualTo(LoginType.COMPANY);
        assertThat(identity.getId()).isEqualTo(company.getId());
        assertThat(session.getAttribute("companySession")).isNull();
        mvc.perform(get("/company/detail").session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString(request.getName())));
        mvc.perform(post("/company/update").session(session).param("contactPersonName", "Updated contact"))
                .andExpect(redirectedUrl("/company/detail"));
        assertThat(companies.findById(company.getId()).orElseThrow().getContactPersonName()).isEqualTo("Updated contact");
        mvc.perform(get("/company/detail")).andExpect(status().isUnauthorized());
    }

    @Test
    void signupStoresFileAndConsumesVerificationAfterCommit() throws Exception {
        CompanySignupReqDto request = request();
        MockHttpSession session = verifiedSession(request.getEmail());
        Instant verifiedAt = ((EmailVerificationSession) session.getAttribute(Define.EMAIL_VERIFICATION)).getVerifiedAt();
        assertThat(controller.signup(request, session, new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/login?type=company");
        Company company = companies.findByEmail(request.getEmail()).orElseThrow();
        assertThat(company.getName()).isEqualTo(request.getName());
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING);
        assertThat(company.getPoint()).isZero();
        assertThat(company.getBusinessCertificateFileName()).matches("files/[0-9a-f-]{36}\\.pdf");
        assertThat(Files.readAllBytes(STORAGE_ROOT.resolve(company.getBusinessCertificateFileName())))
                .isEqualTo(request.getBusinessCertificate().getBytes());
        assertThat(encoder.matches(request.getPassword(), company.getPassword())).isTrue();
        assertThat(company.getEmailVerifiedAt()).isCloseTo(verifiedAt, within(1, java.time.temporal.ChronoUnit.MILLIS));
        assertThat(session.getAttribute(Define.EMAIL_VERIFICATION)).isNull();
    }

    @Test
    void unverifiedSignupDoesNotPersistCompany() {
        CompanySignupReqDto request = request();
        assertThatThrownBy(() -> authService.signup(request, new MockHttpSession())).isInstanceOf(Exception400.class);
        assertThat(companies.findByEmail(request.getEmail())).isEmpty();
    }

    @Test
    void duplicateEmailOrBusinessNumberIsRejectedAndVerificationIsRetained() {
        CompanySignupReqDto original = request();
        authService.signup(original, verifiedSession(original.getEmail()));
        MockHttpSession session = verifiedSession(original.getEmail());
        assertThatThrownBy(() -> controller.signup(original, session, new RedirectAttributesModelMap()))
                .isInstanceOf(Exception400.class);
        assertThat(session.getAttribute(Define.EMAIL_VERIFICATION)).isNotNull();
        CompanySignupReqDto duplicate = request();
        duplicate.setBusinessNumber(original.getBusinessNumber());
        assertThatThrownBy(() -> authService.signup(duplicate, verifiedSession(duplicate.getEmail())))
                .isInstanceOf(Exception400.class);
        assertThat(companies.findByEmail(duplicate.getEmail())).isEmpty();
    }

    @Test
    void requestValidationRunsForDirectServiceCalls() {
        CompanySignupReqDto request = request();
        request.setPassword("weak");
        assertThatThrownBy(() -> authService.signup(request, verifiedSession(request.getEmail())))
                .isInstanceOf(Exception400.class);
    }

    private CompanySignupReqDto request() {
        CompanySignupReqDto request = new CompanySignupReqDto();
        request.setName("Migration test company");
        request.setEmail("Company-" + UUID.randomUUID() + "@Example.test");
        request.setPassword("Password1!");
        request.setPasswordConfirm("Password1!");
        request.setBusinessCertificate(new MockMultipartFile(
                "businessCertificate", "certificate.pdf", "application/pdf",
                "%PDF-1.7\nfixture".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        request.setBusinessNumber(Long.toString(ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L)));
        request.setRepresentativeName("Test representative");
        request.setOpenedOn(LocalDate.of(2020, 1, 1));
        request.setAddress("Busan test address");
        request.setContactPersonName("Test contact");
        request.setContactPersonPhone("010-0000-0001");
        return request;
    }

    private MockHttpSession verifiedSession(String email) {
        Instant now = Instant.now();
        EmailVerificationSession verification = EmailVerificationSession.builder().email(email)
                .sentAt(now).expiresAt(now.plusSeconds(300)).codeHash("unused").build();
        verification.markVerified(now);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Define.EMAIL_VERIFICATION, verification);
        return session;
    }
}
