package com.busan.eventplatform.emailverification;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;

@Getter
public class EmailVerificationSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String email;         // 인증 대상 이메일
    private final Instant sentAt;       // 재발송 간격 계산
    private final Instant expiresAt;    // 코드 만료 판단

    private String codeHash;            // 입력한 코드와 비교할 해시
    private int failedAttempts;         // 반복 입력 제한
    private Instant verifiedAt;         // 인증 완료 여부와 결과 만료 판단

    @Builder
    public EmailVerificationSession(
            String email,
            String codeHash,
            Instant sentAt,
            Instant expiresAt
    ) {
        this.email = email;
        this.codeHash = codeHash;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
    }

    public boolean isCodeExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void increaseFailedAttempts() {
        failedAttempts++;
    }

    public void markVerified(Instant now) {
        verifiedAt = now;
        codeHash = null;
    }
}

// 생성 할때는 아래와 같이 사용
// EmailVerificationSession verification =
//        EmailVerificationSession.builder()
//                .email(email)
//                .codeHash(codeHash)
//                .sentAt(now)
//                .expiresAt(now.plusSeconds(300))
//                .build();