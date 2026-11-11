
package com.busan.eventplatform.event.eventSession;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventSessionTest {

    // 잔여 정원 확인
    @Test
    void hasRemainingSeat_test() {
        EventSession session = EventSession.builder()
                .capacity(10)
                .occupiedCount(5)
                .build();

        assertTrue(session.hasRemainingSeat());
    }

    // 정원 점유
    @Test
    void occupySeat_test() {
        EventSession session = EventSession.builder()
                .capacity(10)
                .occupiedCount(5)
                .build();

        session.occupySeat();

        assertEquals(6, session.getOccupiedCount());
    }

    // 정원 초과 방지
    @Test
    void occupySeat_fullCapacity_test() {
        EventSession session = EventSession.builder()
                .capacity(10)
                .occupiedCount(10)
                .build();

        assertThrows(
                Exception400.class,
                session::occupySeat
        );
    }

    // 정원 반환
    @Test
    void releaseSeat_test() {
        EventSession session = EventSession.builder()
                .capacity(10)
                .occupiedCount(5)
                .build();

        session.releaseSeat();

        assertEquals(4, session.getOccupiedCount());
    }

    // 접수 번호 발급
    @Test
    void issueReceptionNumber_test() {
        EventSession session = EventSession.builder()
                .nextReceptionNumber(1)
                .build();

        int number = session.issueReceptionNumber();

        assertEquals(1, number);
        assertEquals(2, session.getNextReceptionNumber());
    }
}
