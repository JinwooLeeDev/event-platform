package com.busan.eventplatform.login.emailVerification;

import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.emailverification.EmailAvailabilityService;
import com.busan.eventplatform.login.emailverification.EmailSender;
import com.busan.eventplatform.login.emailverification.EmailVerificationService;
import com.busan.eventplatform.login.emailverification.EmailVerificationSession;
import com.busan.eventplatform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailAvailabilityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private EmailSender emailSender;

    private EmailAvailabilityService availabilityService;
    private EmailVerificationService verificationService;

    @BeforeEach
    void setUp() {
        // 실제 공통 검사 서비스를 사용하고 DB 조회만 Mock으로 대체한다.
        availabilityService = new EmailAvailabilityService(
                userRepository,
                companyRepository
        );

        // 실제 발송 서비스 흐름을 사용하되 메일 전송은 Mock으로 대체한다.
        verificationService = new EmailVerificationService(
                emailSender,
                availabilityService
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void 이메일이_없으면_검사를_거부한다(String email) {
        assertThatThrownBy(() ->
                availabilityService.validateAvailable(email)
        ).isInstanceOf(Exception400.class);

        verifyNoInteractions(userRepository, companyRepository);
    }

    @Test
    void 개인이_사용중인_이메일이면_거부한다() {
        String email = "member@example.com";
        when(userRepository.existsByEmailNormalized(email))
                .thenReturn(true);

        assertThatThrownBy(() ->
                availabilityService.validateAvailable(email)
        )
                .isInstanceOf(Exception400.class)
                .hasMessage("이미 사용중인 이메일입니다.");

        // 개인에서 발견했으므로 기업 조회까지 할 필요가 없다.
        verifyNoInteractions(companyRepository);
    }

    @Test
    void 기업이_사용중인_이메일이면_거부한다() {
        String email = "company@example.com";
        when(userRepository.existsByEmailNormalized(email))
                .thenReturn(false);
        when(companyRepository.existsByEmail(email))
                .thenReturn(true);

        assertThatThrownBy(() ->
                availabilityService.validateAvailable(email)
        )
                .isInstanceOf(Exception400.class)
                .hasMessage("이미 사용중인 이메일입니다.");
    }

    @Test
    void 양쪽_모두_사용하지_않는_이메일이면_허용한다() {
        String email = "new@example.com";
        when(userRepository.existsByEmailNormalized(email))
                .thenReturn(false);
        when(companyRepository.existsByEmail(email))
                .thenReturn(false);

        assertThatCode(() ->
                availabilityService.validateAvailable(email)
        ).doesNotThrowAnyException();

        verify(userRepository).existsByEmailNormalized(email);
        verify(companyRepository).existsByEmail(email);
    }

    @Test
    void 개인_중복이면_메일을_발송하지_않는다() {
        String email = "member@example.com";
        MockHttpSession session = new MockHttpSession();

        when(userRepository.existsByEmailNormalized(email))
                .thenReturn(true);

        assertThatThrownBy(() ->
                verificationService.sendCode(email, session)
        ).isInstanceOf(Exception400.class);

        verifyNoInteractions(emailSender);
        assertThat(session.getAttribute(Define.EMAIL_VERIFICATION))
                .isNull();
    }

    @Test
    void 기업_중복이면_메일을_발송하지_않는다() {
        String email = "company@example.com";
        MockHttpSession session = new MockHttpSession();

        when(userRepository.existsByEmailNormalized(email))
                .thenReturn(false);
        when(companyRepository.existsByEmail(email))
                .thenReturn(true);

        assertThatThrownBy(() ->
                verificationService.sendCode(email, session)
        ).isInstanceOf(Exception400.class);

        verifyNoInteractions(emailSender);
        assertThat(session.getAttribute(Define.EMAIL_VERIFICATION))
                .isNull();
    }

    @Test
    void 미사용_이메일이면_메일을_발송하고_인증세션을_저장한다() {
        String email = "new@example.com";
        MockHttpSession session = new MockHttpSession();

        when(userRepository.existsByEmailNormalized(email))
                .thenReturn(false);
        when(companyRepository.existsByEmail(email))
                .thenReturn(false);

        verificationService.sendCode(email, session);

        verify(emailSender).sendVerificationCode(
                eq(email),
                anyString()
        );

        assertThat(session.getAttribute(Define.EMAIL_VERIFICATION))
                .isInstanceOf(EmailVerificationSession.class);
    }
}