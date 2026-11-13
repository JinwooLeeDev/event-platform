package com.busan.eventplatform.user.registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum RegistrationStatus {
    CONFIRMED("참가 확정"),
    PENDING("승인 대기"),
    WAITING("대기열"),
    CANCELLED("신청 취소"),
    REJECTED("반려");

    private final String description;

    /** 정원을 차지하는 상태 (확정 + 승인대기) */
    public static final Set<RegistrationStatus> SEAT_OCCUPYING = EnumSet.of(CONFIRMED, PENDING);

    /** 아직 유효한 신청 상태 (중복 신청 판단용) */
    public static final Set<RegistrationStatus> ACTIVE = EnumSet.of(CONFIRMED, PENDING, WAITING);

    public boolean isSeatOccupying() {
        return SEAT_OCCUPYING.contains(this);
    }

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
