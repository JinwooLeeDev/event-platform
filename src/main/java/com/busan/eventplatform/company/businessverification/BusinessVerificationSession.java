package com.busan.eventplatform.company.businessverification;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
public class BusinessVerificationSession implements Serializable { // 이메일 인증과 동일하게, 세션 데이터를 직렬화함.
    private static final long serialVersionUID = 1L;

//    검증 성공 결과의 유효시간
    private static final Duration VALID_DURATION = Duration.ofMinutes(10);

    private final String businessNumber;
    private final String representativeName;
    private final LocalDate openedOn;
    private final Instant verifiedAt;

    @Builder
    public BusinessVerificationSession(String businessNumber,
                                       String representativeName,
                                       LocalDate openedOn,
                                       Instant verifiedAt) {
//        필수 정보가 빠진 세션 객체 생성을 막음.
        this.businessNumber = Objects.requireNonNull(businessNumber);
        this.representativeName = Objects.requireNonNull(representativeName);
        this.openedOn = Objects.requireNonNull(openedOn);
        this.verifiedAt = Objects.requireNonNull(verifiedAt);
    }

    public boolean matches(
            String businessNumber,
            String representativeName,
            LocalDate openedOn
    ) {
        return this.businessNumber.equals(businessNumber)
                && this.representativeName.equals(representativeName)
                && this.openedOn.equals(openedOn);
    }

    public boolean isExpired(Instant now) {
        Instant expiresAt = verifiedAt.plus(VALID_DURATION);

        return !now.isBefore(expiresAt);
    }
}
