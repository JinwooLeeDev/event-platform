package com.busan.eventplatform.login.emailverification;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.global.util.MailUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailSender emailSender;

    private final PasswordEncoder codeEncoder = new BCryptPasswordEncoder();

    // 1. 인증번호 발송
    public void sendCode(String email, HttpSession session) {
        synchronized (session) {
            Instant now = Instant.now(); // 현재 시간을 now 에 저장

            EmailVerificationSession previous = getVerification(session);

            // 다른 이메일로 변경해도 재발송 대기시간은 유지
            if (previous != null) {
                Instant resendAvailableAt = previous.getSentAt().plusSeconds(Define.EMAIL_RESEND_INTERVAL_SECONDS);
                if (now.isBefore(resendAvailableAt)) {
                    throw new Exception400("인증번호는 60초 후 다시 요청해주세요.");
                }
            }

            String code = MailUtil.generateCode();
            String codeHash = codeEncoder.encode(code);

            // 발송 실패 시 기존 인증 정보는 유지
            emailSender.sendVerificationCode(email, code);

            // SMTP 발송에 걸린 시간을 제외하고 유효기간 계산
            Instant sentAt = Instant.now();

            EmailVerificationSession verification =
                    EmailVerificationSession.builder()
                            .email(email)
                            .codeHash(codeHash)
                            .sentAt(sentAt)
                            .expiresAt(sentAt.plus(Define.EMAIL_CODE_EXPIRE_MINUTES, ChronoUnit.MINUTES))
                            // expiresAt(만료시간)을 sentAt 시간 기준 + 5분
                            .build();

            // 새 코드 발송 성공 후 이전 코드와 인증 완료 상태 교체
            session.setAttribute(Define.EMAIL_VERIFICATION, verification);
        }
    }

    // 2. 인증번호 확인
    public void confirmCode(String email, String code, HttpSession session) {
        synchronized (session) {EmailVerificationSession verification = getVerification(session);

            if (verification == null) {
                throw new Exception400("인증번호를 먼저 요청해주세요.");
            }

            if (!verification.getEmail().equals(email)) {
                throw new Exception400("인증번호를 요청한 이메일과 일치하지 않습니다.");
            }

            if (verification.isVerified()) {
                throw new Exception400("이미 인증이 완료되었습니다.");
            }

            Instant now = Instant.now();

            if (verification.isCodeExpired(now)) {
                throw new Exception400("인증번호가 만료되었습니다. 다시 요청해주세요.");
            }

            if (verification.getFailedAttempts() >= Define.EMAIL_MAX_FAILED_ATTEMPTS) {
                throw new Exception400("인증 시도 횟수를 초과했습니다. " + "인증번호를 다시 요청해주세요.");
            }

            if (!codeEncoder.matches(code, verification.getCodeHash())) {
                verification.increaseFailedAttempts();

                // 변경한 상태를 세션에 다시 저장
                session.setAttribute(Define.EMAIL_VERIFICATION, verification);

                if (verification.getFailedAttempts() >= Define.EMAIL_MAX_FAILED_ATTEMPTS) {
                    throw new Exception400("인증 시도 횟수를 초과했습니다. " + "인증번호를 다시 요청해주세요.");
                }

                throw new Exception400("인증번호가 일치하지 않습니다.");
            }

            verification.markVerified(now);

            session.setAttribute(
                    Define.EMAIL_VERIFICATION,
                    verification
            );
        }
    }

    // 3. 회원가입 전에 인증 완료 여부 확인
    public void requireVerifiedEmail(String email, HttpSession session) {
        synchronized (session) {EmailVerificationSession verification = getVerification(session);

            if (verification == null || !verification.isVerified()) {
                throw new Exception400("이메일 인증을 먼저 완료해주세요.");
            }

            if (!verification.getEmail().equals(email)) {
                throw new Exception400("인증한 이메일과 가입 이메일이 다릅니다.");
            }

            Instant verifiedUntil = verification.getVerifiedAt().plus(Define.EMAIL_VERIFIED_EXPIRE_MINUTES, ChronoUnit.MINUTES);

            if (!Instant.now().isBefore(verifiedUntil)) {
                throw new Exception400("이메일 인증 유효시간이 지났습니다. " + "다시 인증해주세요.");
            }
        }
    }

    private EmailVerificationSession getVerification(HttpSession session) {
        return (EmailVerificationSession) session.getAttribute(Define.EMAIL_VERIFICATION);
    }
}
