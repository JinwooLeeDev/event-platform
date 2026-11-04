package com.busan.eventplatform.emailverification;

import com.busan.eventplatform.global.error.exception.Exception500;
import com.busan.eventplatform.global.util.Define;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[Busan Play] 회원가입 이메일 인증번호");
        message.setText("""
                안녕하세요. Busan Play입니다.

                이메일 인증번호: %s

                %d분 안에 인증번호를 입력해주세요.
                본인이 요청하지 않았다면 이 메일을 무시해주세요.
                """.formatted(
                code,
                Define.EMAIL_CODE_EXPIRE_MINUTES
        ));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            // 인증번호나 메일 본문은 로그에 기록하지 않습니다.
            log.error("인증 메일 발송 실패: type={}", exception.getClass().getSimpleName());

            throw new Exception500("인증 메일 발송에 실패했습니다.");
        }
    }
}