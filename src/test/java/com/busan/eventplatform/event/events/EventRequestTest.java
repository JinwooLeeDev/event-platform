
package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.eventSession.EventSessionRequest;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventRequestTest {

    // 정상적인 회차 정보 생성
    private EventSessionRequest.SaveDto validSession() {
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

    // 정상적인 회차 없는 행사 정보 생성
    private EventRequest.SaveDto validEvent() {
        EventRequest.SaveDto dto = new EventRequest.SaveDto();
        dto.setTitle("테스트 행사");
        dto.setSummary("행사 소개");
        dto.setDescription("행사 상세 설명");
        dto.setAddress("부산광역시");
        dto.setCategoryId(1L);
        dto.setHasSessions(false);
        dto.setStartDate(LocalDate.of(2027, 1, 10));
        dto.setEndDate(LocalDate.of(2027, 1, 11));
        dto.setApplyStartAt(LocalDateTime.of(2027, 1, 1, 0, 0));
        dto.setApplyEndAt(LocalDateTime.of(2027, 1, 9, 23, 59));
        dto.setPricingType("FREE");
        dto.setFee(0);
        return dto;
    }

    @Test
    void 정상적인_행사_등록_검증() {
        EventRequest.SaveDto dto = validEvent();

        assertDoesNotThrow(dto::validate);
    }

    @Test
    void 행사_제목이_없으면_예외() {
        EventRequest.SaveDto dto = validEvent();
        dto.setTitle(" ");

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 카테고리가_없으면_예외() {
        EventRequest.SaveDto dto = validEvent();
        dto.setCategoryId(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 행사_시작일이_마감일보다_늦으면_예외() {
        EventRequest.SaveDto dto = validEvent();
        dto.setStartDate(LocalDate.of(2027, 1, 12));
        dto.setEndDate(LocalDate.of(2027, 1, 11));

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 모집_시작일이_마감일보다_늦으면_예외() {
        EventRequest.SaveDto dto = validEvent();
        dto.setApplyStartAt(LocalDateTime.of(2027, 1, 10, 0, 0));
        dto.setApplyEndAt(LocalDateTime.of(2027, 1, 9, 0, 0));

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 무료_행사는_참가비가_0으로_설정됨() {
        EventRequest.SaveDto dto = validEvent();
        dto.setFee(5000);

        dto.validate();

        assertEquals(0, dto.getFee());
    }

    @Test
    void 유료_행사인데_참가비가_0이면_예외() {
        EventRequest.SaveDto dto = validEvent();
        dto.setPricingType("PAID");
        dto.setFee(0);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 잘못된_참가비_유형이면_예외() {
        EventRequest.SaveDto dto = validEvent();
        dto.setPricingType("INVALID");

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 회차가_있는_행사인데_회차가_없으면_예외() {
        EventRequest.SaveDto dto = validEvent();
        dto.setHasSessions(true);
        dto.setSessions(List.of());

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    void 정상적인_회차가_있는_행사는_검증_통과() {
        EventRequest.SaveDto dto = validEvent();
        dto.setHasSessions(true);
        dto.setSessions(List.of(validSession()));

        assertDoesNotThrow(dto::validate);
    }

    @Test
    void 회차_행사에_잘못된_회차가_포함되면_예외() {
        EventRequest.SaveDto dto = validEvent();

        EventSessionRequest.SaveDto session = validSession();
        session.setCapacity(0);

        dto.setHasSessions(true);
        dto.setSessions(List.of(session));

        assertThrows(Exception400.class, dto::validate);
    }
}
