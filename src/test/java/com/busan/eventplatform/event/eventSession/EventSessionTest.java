
package com.busan.eventplatform.event;

import com.busan.eventplatform.event.event_session.EventSessionRequest;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.global.error.exception.Exception400;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventSessionTest {

    private EventSession createSession(
            int capacity,
            int occupiedCount
    ) {
        return EventSession.builder()
                .id(1L)
                .name("1회차")
                .capacity(capacity)
                .occupiedCount(occupiedCount)
                .nextReceptionNumber(1)
                .startsAt(LocalDateTime.of(2026, 12, 1, 10, 0))
                .endsAt(LocalDateTime.of(2026, 12, 1, 18, 0))
                .applyStartAt(LocalDateTime.of(2026, 11, 20, 9, 0))
                .applyEndAt(LocalDateTime.of(2026, 11, 30, 18, 0))
                .holdDurationSeconds(300)
                .pricingType("FREE")
                .fee(0)
                .reservationDepositPoints(0)
                .minimumParticipants(1)
                .minimumCheckAt(LocalDateTime.of(2026, 12, 1, 18, 0))
                .status(EventSessionStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("정원에 여유가 있으면 신청할 수 있다")
    void hasRemainingSeat_true() {
        EventSession session = createSession(30, 10);

        assertTrue(session.hasRemainingSeat());
    }

    @Test
    @DisplayName("정원이 가득 차면 신청할 수 없다")
    void hasRemainingSeat_false() {
        EventSession session = createSession(30, 30);

        assertFalse(session.hasRemainingSeat());
    }

    @Test
    @DisplayName("정원 한 자리를 점유한다")
    void occupySeat_success() {
        EventSession session = createSession(30, 10);

        session.occupySeat();

        assertEquals(11, session.getOccupiedCount());
    }

    @Test
    @DisplayName("정원이 가득 차면 추가 점유할 수 없다")
    void occupySeat_full() {
        EventSession session = createSession(30, 30);

        assertThrows(
                Exception400.class,
                session::occupySeat
        );

        assertEquals(30, session.getOccupiedCount());
    }

    @Test
    @DisplayName("점유된 정원 한 자리를 반환한다")
    void releaseSeat_success() {
        EventSession session = createSession(30, 10);

        session.releaseSeat();

        assertEquals(9, session.getOccupiedCount());
    }

    @Test
    @DisplayName("점유 인원이 0이면 정원을 반환할 수 없다")
    void releaseSeat_empty() {
        EventSession session = createSession(30, 0);

        assertThrows(
                IllegalStateException.class,
                session::releaseSeat
        );
    }

    @Test
    @DisplayName("접수 번호를 발급하면 다음 번호가 증가한다")
    void issueReceptionNumber_success() {
        EventSession session = createSession(30, 0);

        int first = session.issueReceptionNumber();
        int second = session.issueReceptionNumber();

        assertEquals(1, first);
        assertEquals(2, second);
    }

    @Test
    @DisplayName("점유 인원보다 작은 정원으로 수정할 수 없다")
    void update_capacityBelowOccupiedCount() {
        EventSession session = createSession(30, 10);

        EventSessionRequest.UpdateDto dto = createUpdateDto(9);

        assertThrows(
                Exception400.class,
                () -> session.update(dto)
        );

        assertEquals(30, session.getCapacity());
    }

    @Test
    @DisplayName("점유 인원 이상으로 정원을 수정할 수 있다")
    void update_capacitySuccess() {
        EventSession session = createSession(30, 10);

        EventSessionRequest.UpdateDto dto = createUpdateDto(40);
        dto.setApplyStartAt(LocalDateTime.of(2026, 11, 20, 9, 0));
        dto.setApplyEndAt(LocalDateTime.of(2026, 11, 30, 18, 0));

        session.update(dto);

        assertEquals(40, session.getCapacity());
        assertEquals("수정된 회차", session.getName());
        assertEquals(dto.getApplyStartAt(), session.getApplyStartAt());
        assertEquals(dto.getApplyEndAt(), session.getApplyEndAt());
    }

    @Test
    @DisplayName("모집 마감 시간이 지나면 신청이 마감된다")
    void applicationClosed_afterDeadline() {
        EventSession session = EventSession.builder()
                .applyEndAt(LocalDateTime.now().minusMinutes(1))
                .build();

        assertTrue(session.isApplicationClosed());
    }

    @Test
    @DisplayName("모집 마감 시간이 지나지 않으면 신청이 가능하다")
    void applicationClosed_beforeDeadline() {
        EventSession session = EventSession.builder()
                .applyEndAt(LocalDateTime.now().plusDays(1))
                .build();

        assertFalse(session.isApplicationClosed());
    }

    private EventSessionRequest.UpdateDto createUpdateDto(int capacity) {
        EventSessionRequest.UpdateDto dto =
                new EventSessionRequest.UpdateDto();

        dto.setName("수정된 회차");
        dto.setCapacity(capacity);
        dto.setStartsAt(LocalDateTime.of(2026, 12, 2, 10, 0));
        dto.setApplyStartAt(LocalDateTime.of(2026, 11, 20, 9, 0));
        dto.setApplyEndAt(LocalDateTime.of(2026, 11, 30, 18, 0));
        dto.setEndsAt(LocalDateTime.of(2026, 12, 2, 18, 0));
        dto.setHoldDurationSeconds(300);
        dto.setPricingType("FREE");
        dto.setFee(0);
        dto.setReservationDepositPoints(0);
        dto.setMinimumParticipants(1);
        dto.setMinimumCheckAt(LocalDateTime.of(2026, 12, 2, 18, 0));

        return dto;
    }
}
