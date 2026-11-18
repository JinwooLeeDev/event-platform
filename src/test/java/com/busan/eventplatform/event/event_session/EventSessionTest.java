
package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventSessionTest {

    private EventSession createSession() {
        return EventSession.builder()
                .id(1L)
                .name("1회차")
                .capacity(100)
                .occupiedCount(0)
                .nextReceptionNumber(1)
                .startsAt(LocalDateTime.now().plusDays(10))
                .endsAt(LocalDateTime.now().plusDays(10).plusHours(8))
                .applyStartAt(LocalDateTime.now().plusDays(1))
                .applyEndAt(LocalDateTime.now().plusDays(9))
                .holdDurationSeconds(300)
                .pricingType("FREE")
                .fee(0)
                .reservationDepositPoints(0)
                .minimumParticipants(1)
                .minimumCheckAt(LocalDateTime.now().plusDays(10))
                .status(EventSessionStatus.SCHEDULED)
                .build();
    }

    private EventSessionRequest.UpdateDto createUpdateDto(int capacity) {
        EventSessionRequest.UpdateDto dto =
                new EventSessionRequest.UpdateDto();

        dto.setName("수정된 회차");
        dto.setCapacity(capacity);
        dto.setStartsAt(LocalDateTime.now().plusDays(20));
        dto.setEndsAt(LocalDateTime.now().plusDays(20).plusHours(8));
        dto.setApplyStartAt(LocalDateTime.now().plusDays(11));
        dto.setApplyEndAt(LocalDateTime.now().plusDays(19));
        dto.setHoldDurationSeconds(600);
        dto.setPricingType("PAID");
        dto.setFee(5000);
        dto.setReservationDepositPoints(1000);
        dto.setMinimumParticipants(2);
        dto.setMinimumCheckAt(LocalDateTime.now().plusDays(20));

        return dto;
    }

    @Test
    void 잔여_정원이_있으면_true() {
        EventSession session = createSession();

        assertTrue(session.hasRemainingSeat());
    }

    @Test
    void 정원이_가득_차면_false() {
        EventSession session = createSession();
        session.occupySeat();
        // 테스트용으로 정원까지 점유
        for (int i = 1; i < 100; i++) {
            session.occupySeat();
        }

        assertFalse(session.hasRemainingSeat());
    }

    @Test
    void 정원_한자리_점유() {
        EventSession session = createSession();

        session.occupySeat();

        assertEquals(1, session.getOccupiedCount());
    }

    @Test
    void 정원이_가득_차면_추가_점유_불가() {
        EventSession session = createSession();

        for (int i = 0; i < 100; i++) {
            session.occupySeat();
        }

        assertThrows(Exception400.class, session::occupySeat);
    }

    @Test
    void 정원_한자리_반환() {
        EventSession session = createSession();
        session.occupySeat();

        session.releaseSeat();

        assertEquals(0, session.getOccupiedCount());
    }

    @Test
    void 점유_인원이_없으면_정원_반환_불가() {
        EventSession session = createSession();

        assertThrows(IllegalStateException.class, session::releaseSeat);
    }

    @Test
    void 접수번호를_발급하면_다음_번호로_증가() {
        EventSession session = createSession();

        assertEquals(1, session.issueReceptionNumber());
        assertEquals(2, session.issueReceptionNumber());
        assertEquals(3, session.issueReceptionNumber());
    }

    @Test
    void 행사_시작일이_과거이면_날짜_검증_실패() {
        LocalDateTime now = LocalDateTime.now();

        assertThrows(Exception400.class, () ->
                EventSession.validateDates(
                        now.minusDays(1),
                        now.plusDays(1),
                        now.plusDays(1),
                        now.plusDays(2)
                )
        );
    }

    @Test
    void 모집_시작일이_과거이면_날짜_검증_실패() {
        LocalDateTime now = LocalDateTime.now();

        assertThrows(Exception400.class, () ->
                EventSession.validateDates(
                        now.plusDays(10),
                        now.plusDays(11),
                        now.minusDays(1),
                        now.plusDays(9)
                )
        );
    }

    @Test
    void 행사_종료일이_시작일보다_빠르면_검증_실패() {
        LocalDateTime now = LocalDateTime.now();

        assertThrows(Exception400.class, () ->
                EventSession.validateDates(
                        now.plusDays(10),
                        now.plusDays(9),
                        now.plusDays(1),
                        now.plusDays(8)
                )
        );
    }

    @Test
    void 모집_마감일이_시작일보다_빠르면_검증_실패() {
        LocalDateTime now = LocalDateTime.now();

        assertThrows(Exception400.class, () ->
                EventSession.validateDates(
                        now.plusDays(10),
                        now.plusDays(11),
                        now.plusDays(5),
                        now.plusDays(4)
                )
        );
    }

    @Test
    void 행사_시작일이_모집_마감일보다_빠르면_검증_실패() {
        LocalDateTime now = LocalDateTime.now();

        assertThrows(Exception400.class, () ->
                EventSession.validateDates(
                        now.plusDays(5),
                        now.plusDays(10),
                        now.plusDays(1),
                        now.plusDays(6)
                )
        );
    }

    @Test
    void 정상적인_날짜는_검증_통과() {
        LocalDateTime now = LocalDateTime.now();

        assertDoesNotThrow(() ->
                EventSession.validateDates(
                        now.plusDays(10),
                        now.plusDays(11),
                        now.plusDays(1),
                        now.plusDays(9)
                )
        );
    }

    @Test
    void 점유_인원보다_정원을_작게_수정하면_실패() {
        EventSession session = createSession();
        session.occupySeat();
        session.occupySeat();

        EventSessionRequest.UpdateDto dto = createUpdateDto(1);

        assertThrows(Exception400.class, () -> session.update(dto));
    }

    @Test
    void 정상적으로_회차_수정() {
        EventSession session = createSession();

        session.update(createUpdateDto(200));

        assertEquals("수정된 회차", session.getName());
        assertEquals(200, session.getCapacity());
        assertEquals("PAID", session.getPricingType());
        assertEquals(5000, session.getFee());
    }

    @Test
    void 회차_상태를_취소로_변경() {
        EventSession session = createSession();

        session.setStatus(EventSessionStatus.CANCELLED);

        assertTrue(session.isCanceled());
    }

    @Test
    void 회차_상태를_예정으로_변경() {
        EventSession session = createSession();
        session.setStatus(EventSessionStatus.CANCELLED);

        session.setScheduled();

        assertEquals(EventSessionStatus.SCHEDULED, session.getStatus());
    }
}
