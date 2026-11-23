package com.busan.eventplatform.global.i18n;

import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.emailverification.SmtpEmailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VerificationMailLocaleTest {
    private Object originalSource;
    @BeforeEach void remember() { originalSource = ReflectionTestUtils.getField(LocaleMessages.class, "displaySource"); }
    @AfterEach void restore() {
        ReflectionTestUtils.setField(LocaleMessages.class, "displaySource", originalSource);
        LocaleContextHolder.resetLocaleContext();
    }

    @ParameterizedTest @ValueSource(strings={"ko","en","ja","zh-CN"})
    void mailUsesRequestedLanguageAndPreservesCodeAndExpiry(String code) {
        var messages = new LocaleMessages(new I18nConfig().messageSource());
        messages.registerDisplaySource();
        LocaleContextHolder.setLocale(SupportedLocale.locale(code));
        var mail = mock(JavaMailSender.class);
        var sender = new SmtpEmailSender(mail);
        ReflectionTestUtils.setField(sender, "from", "audit@example.test");
        sender.sendVerificationCode("recipient@example.test", "ABC123");
        var captured = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(captured.capture());
        assertThat(captured.getValue().getTo()).containsExactly("recipient@example.test");
        assertThat(captured.getValue().getSubject()).contains("Busan Play");
        assertThat(captured.getValue().getText()).contains("ABC123", Integer.toString(Define.EMAIL_CODE_EXPIRE_MINUTES));
        String label = switch (code) {
            case "en" -> "Email verification code";
            case "ja" -> "メール認証コード";
            case "zh-CN" -> "邮箱验证码";
            default -> "이메일 인증번호";
        };
        assertThat(captured.getValue().getText()).contains(label);
        if (!code.equals("ko")) assertThat(captured.getValue().getSubject()).doesNotContainPattern("[가-힣]");
    }
}
