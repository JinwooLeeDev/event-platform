
package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.global.error.exception.Exception400;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventSessionRequestTest {

    private EventSessionRequest.SaveDto createSaveDto() {
        EventSessionRequest.SaveDto dto =
                new EventSessionRequest.SaveDto();

        dto.setName("1회차");
        dto.setStartsAt(LocalDateTime.of(2026, 12, 1, 10, 0));
        dto.setEndsAt(LocalDateTime.of(2026, 12, 1, 18, 0));
        dto.setApplyStartAt(LocalDateTime.of(2026, 11, 20, 9, 0));
        dto.setApplyEndAt(LocalDateTime.of(2026, 11, 30, 18, 0));
        dto.setCapacity(30);
        dto.setHoldDurationSeconds(300);
        dto.setPricingType("FREE");
        dto.setFee(0);
        dto.setReservationDepositPoints(0);
        dto.setMinimumParticipants(1);
        dto.setMinimumCheckAt(LocalDateTime.of(2026, 12, 1, 18, 0));

        return dto;
    }

    private EventSessionRequest.UpdateDto createUpdateDto() {
        EventSessionRequest.UpdateDto dto =
                new EventSessionRequest.UpdateDto();

        dto.setName("1회차");
        dto.setStartsAt(LocalDateTime.of(2026, 12, 1, 10, 0));
        dto.setEndsAt(LocalDateTime.of(2026, 12, 1, 18, 0));
        dto.setCapacity(30);
        dto.setHoldDurationSeconds(300);
        dto.setPricingType("FREE");
        dto.setFee(0);
        dto.setReservationDepositPoints(0);
        dto.setMinimumParticipants(1);
        dto.setMinimumCheckAt(LocalDateTime.of(2026, 12, 1, 18, 0));

        return dto;
    }

    @Test
    @DisplayName("정상적인 회차 등록 요청은 검증을 통과한다")
    void saveDto_validate_success() {
        EventSessionRequest.SaveDto dto = createSaveDto();

        assertDoesNotThrow(dto::validate);
    }

    @Test
    @DisplayName("회차 이름이 없으면 등록할 수 없다")
    void saveDto_validate_nameRequired() {
        EventSessionRequest.SaveDto dto = createSaveDto();
        dto.setName(" ");

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("행사 시작일이 종료일보다 늦으면 등록할 수 없다")
    void saveDto_validate_invalidDate() {
        EventSessionRequest.SaveDto dto = createSaveDto();
        dto.setStartsAt(LocalDateTime.of(2026, 12, 2, 10, 0));

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("모집 시작일이 마감일보다 늦으면 등록할 수 없다")
    void saveDto_validate_invalidApplyDate() {
        EventSessionRequest.SaveDto dto = createSaveDto();
        dto.setApplyStartAt(LocalDateTime.of(2026, 12, 1, 10, 0));

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("정원이 0명이면 등록할 수 없다")
    void saveDto_validate_invalidCapacity() {
        EventSessionRequest.SaveDto dto = createSaveDto();
        dto.setCapacity(0);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("임시 정원 확보 시간이 음수이면 등록할 수 없다")
    void saveDto_validate_invalidHoldDuration() {
        EventSessionRequest.SaveDto dto = createSaveDto();
        dto.setHoldDurationSeconds(-1);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("참가비가 음수이면 등록할 수 없다")
    void saveDto_validate_invalidFee() {
        EventSessionRequest.SaveDto dto = createSaveDto();
        dto.setFee(-1);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("최소 진행 인원이 정원보다 크면 등록할 수 없다")
    void saveDto_validate_invalidMinimumParticipants() {
        EventSessionRequest.SaveDto dto = createSaveDto();
        dto.setMinimumParticipants(31);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("정상적인 회차 수정 요청은 검증을 통과한다")
    void updateDto_validate_success() {
        EventSessionRequest.UpdateDto dto = createUpdateDto();

        assertDoesNotThrow(dto::validate);
    }

    @Test
    @DisplayName("회차 수정 시 정원이 0명이면 실패한다")
    void updateDto_validate_invalidCapacity() {
        EventSessionRequest.UpdateDto dto = createUpdateDto();
        dto.setCapacity(0);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("회차 수정 시 최소 인원이 정원보다 크면 실패한다")
    void updateDto_validate_invalidMinimumParticipants() {
        EventSessionRequest.UpdateDto dto = createUpdateDto();
        dto.setMinimumParticipants(31);

        assertThrows(Exception400.class, dto::validate);
    }
}
