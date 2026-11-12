
package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.event.EventSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventSessionTest {

    private EventSession eventSession;

    @BeforeEach
    void setUp() {
        eventSession = EventSession.builder()
                .name("1회차")
                .capacity(10)
                .occupiedCount(0)
                .nextReceptionNumber(1)
                .startsAt(LocalDateTime.now().plusDays(3))
                .endsAt(LocalDateTime.now().plusDays(3).plusHours(2))
                .applyStartAt(LocalDateTime.now().minusDays(1))
                .applyEndAt(LocalDateTime.now().plusDays(2))
                .holdDurationSeconds(600)
                .minimumParticipants(2)
                .build();
    }

    @Test
    void 남은_좌석이_있으면_true() {
        assertThat(eventSession.hasRemainingSeat()).isTrue();
    }

    @Test
    void 정원이_가득차면_false() {
        for (int i = 0; i < 10; i++) {
            eventSession.occupySeat();
        }

        assertThat(eventSession.hasRemainingSeat()).isFalse();
    }

    @Test
    void 좌석을_점유하면_점유인원이_증가한다() {
        eventSession.occupySeat();

        assertThat(eventSession.getOccupiedCount()).isEqualTo(1);
    }

    @Test
    void 좌석을_반환하면_점유인원이_감소한다() {
        eventSession.occupySeat();

        eventSession.releaseSeat();

        assertThat(eventSession.getOccupiedCount()).isEqualTo(0);
    }

    @Test
    void 접수번호를_발급하면_다음_번호가_증가한다() {
        int receptionNumber = eventSession.issueReceptionNumber();

        assertThat(receptionNumber).isEqualTo(1);
        assertThat(eventSession.getNextReceptionNumber()).isEqualTo(2);
    }

    @Test
    void 모집마감시간이_지나면_신청이_마감된다() {
        // 모집 마감시간을 과거로 변경하는 테스트는
        // 실제 엔티티 생성 방식에 맞춰 작성해야 함.
    }
}
