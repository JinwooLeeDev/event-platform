package com.busan.eventplatform.user.attendance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * ★ [f/42] 출석 QR 서명 — QR 주소의 code 값을 만들고 확인한다.
 * <p>
 * QR 주소: /attendance/check/{회차 id}?code={서명}
 * - 서명 = HMAC-SHA256("attendance:" + 회차 id, 서버 비밀값) → URL 안전 Base64 22자
 * - 회차 id만 바꿔 주소를 임의로 만들면 서명이 맞지 않아 출석할 수 없다.
 * - DB 저장 없음 (회차·엔티티 수정 없이 동작). 같은 회차는 항상 같은 QR.
 * - 오용 방지는 '참가 확정자 본인 + 출석 가능 시간' 검사가 함께 담당한다.
 * <p>
 * 비밀값: application 설정 attendance.qr-secret (없으면 로컬 기본값). 배포 시 환경 변수로 바꾸는 것을 권장.
 */
@Component
public class AttendanceQrSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int CODE_LENGTH = 22;

    private final SecretKeySpec key;

    public AttendanceQrSigner(@Value("${attendance.qr-secret:busan-play-attendance-local}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /** 회차의 출석 QR 서명 */
    public String sign(Long sessionId) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);   // Mac 은 스레드 안전하지 않아 호출마다 새로 만든다
            mac.init(key);
            byte[] raw = mac.doFinal(("attendance:" + sessionId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw).substring(0, CODE_LENGTH);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("출석 QR 서명을 만들 수 없습니다", e);
        }
    }

    /** QR 로 받은 code 가 이 회차의 서명과 같은지 (시간 차 공격 방지 비교) */
    public boolean matches(Long sessionId, String code) {
        if (sessionId == null || code == null || code.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                sign(sessionId).getBytes(StandardCharsets.UTF_8),
                code.strip().getBytes(StandardCharsets.UTF_8));
    }
}