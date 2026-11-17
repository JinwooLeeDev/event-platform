
package com.busan.eventplatform.event.eventSession;

import com.busan.eventplatform.event.event_session.EventSessionRequest;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventSessionRequestTest {

    private EventSessionRequest.SaveDto validDto() {
        EventSessionRequest.SaveDto dto = new EventSessionRequest.SaveDto();
        dto.setName("1회차");
        dto.setStartsAt(LocalDateTime.of(2027, 1, 10, 10, 0));
        dto.setEndsAt(LocalDateTime.of(2027, 1, 10, 18, 0));
        dto.setApplyStartAt(LocalDateTime.of(2027, 1, 1, 0, 0));
        dto.setApplyEndAt(LocalDateTime.of(2027, 1, 9, 23, 59));
        dto.setCapacity(100);
        dto.setHoldDurationSeconds(300);
        dto.setPricingType("FREE");
        dto.setFee(0);
        dto.setReservationDepositPoints(0);
        dto.setMinimumParticipants(1);
        dto.setMinimumCheckAt(LocalDateTime.of(2027, 1, 10, 18, 0));
        return dto;
    }

    @Test
    void 정상적인_회차_검증() {
        assertDoesNotThrow(() -> validDto().validate());
    }

    @Test
    void 회차_이름이_없으면_예외() {
        var dto = validDto();
        dto.setName("");

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 행사_시작일이_종료일보다_늦으면_예외() {
        var dto = validDto();
        dto.setStartsAt(LocalDateTime.of(2027, 1, 11, 10, 0));

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 행사_시작일이_모집_마감일보다_빠르면_예외() {
        var dto = validDto();
        dto.setStartsAt(LocalDateTime.of(2027, 1, 9, 10, 0));

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 정원이_1명_미만이면_예외() {
        var dto = validDto();
        dto.setCapacity(0);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 임시_정원_확보_시간이_음수이면_예외() {
        var dto = validDto();
        dto.setHoldDurationSeconds(-1);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 참가비가_음수이면_예외() {
        var dto = validDto();
        dto.setFee(-1);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 예약_보증금이_음수이면_예외() {
        var dto = validDto();
        dto.setReservationDepositPoints(-1);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 최소_인원이_정원보다_크면_예외() {
        var dto = validDto();
        dto.setMinimumParticipants(101);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 최소_인원_확인_시점이_없으면_예외() {
        var dto = validDto();
        dto.setMinimumCheckAt(null);

        assertThrows(Exception400.class, dto::validate);
    }
}
