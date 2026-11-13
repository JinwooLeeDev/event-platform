package com.busan.eventplatform.event.eventSession;

import com.busan.eventplatform.event.event_session.EventSessionRequest;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventSessionRequestTest {

    @Test
    void saveDto_validate_success_test() {
        EventSessionRequest.SaveDto dto =
                new EventSessionRequest.SaveDto();

        dto.setName("테스트 행사");
        dto.setStartsAt(LocalDateTime.now());
        dto.setEndsAt(LocalDateTime.now().plusHours(2));
        dto.setCapacity(10);
        dto.setHoldDurationSeconds(300);
        dto.setPricingType("FREE");
        dto.setFee(0);
        dto.setReservationDepositPoints(0);
        dto.setMinimumParticipants(2);
        dto.setMinimumCheckAt(LocalDateTime.now());

        assertDoesNotThrow(dto::validate);
    }

    @Test
    void saveDto_nameEmpty_test() {
        EventSessionRequest.SaveDto dto =
                new EventSessionRequest.SaveDto();

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void saveDto_capacityZero_test() {
        EventSessionRequest.SaveDto dto =
                new EventSessionRequest.SaveDto();

        dto.setName("테스트");
        dto.setStartsAt(LocalDateTime.now());
        dto.setEndsAt(LocalDateTime.now().plusHours(2));
        dto.setCapacity(0);
        dto.setHoldDurationSeconds(300);
        dto.setPricingType("FREE");
        dto.setMinimumParticipants(1);
        dto.setMinimumCheckAt(LocalDateTime.now());

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void saveDto_minimumParticipantsOverCapacity_test() {
        EventSessionRequest.SaveDto dto =
                new EventSessionRequest.SaveDto();

        dto.setName("테스트");
        dto.setStartsAt(LocalDateTime.now());
        dto.setEndsAt(LocalDateTime.now().plusHours(2));
        dto.setCapacity(5);
        dto.setHoldDurationSeconds(300);
        dto.setPricingType("FREE");
        dto.setFee(0);
        dto.setReservationDepositPoints(0);
        dto.setMinimumParticipants(10);
        dto.setMinimumCheckAt(LocalDateTime.now());

        assertThrows(Exception400.class, dto::validate);
    }
}
