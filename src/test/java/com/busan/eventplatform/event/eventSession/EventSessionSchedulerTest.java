package com.busan.eventplatform.event.eventSession;

import com.busan.eventplatform.event.EventSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSessionSchedulerTest {

    @Mock
    private EventSessionService eventSessionService;

    @Mock
    private TaskScheduler taskScheduler;

    @InjectMocks
    private EventSessionScheduler eventSessionScheduler;

    @Test
    void 회차의_시작_시각에_시작_작업을_예약한다() {
        Long sessionId = 10L;
        LocalDateTime startsAt = LocalDateTime.of(2026, 11, 20, 14, 30);

        EventSession session = mock(EventSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.getStartsAt()).thenReturn(startsAt);

        eventSessionScheduler.processEventStarted(session);

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

        // 한국 시간 14:30은 UTC 05:30이다.
        verify(taskScheduler).schedule(
                taskCaptor.capture(),
                eq(Instant.parse("2026-11-20T05:30:00Z"))
        );

        // 예약 직후에는 아직 회차 시작 메서드가 호출되지 않는다.
        verifyNoInteractions(eventSessionService);

        // 예약 시각에 스케줄러가 실행할 작업을 직접 실행한다.
        taskCaptor.getValue().run();

        verify(eventSessionService).startSession(sessionId);
    }

    @Test
    void 종료_처리_중_한_회차가_실패해도_다음_회차를_처리한다() {
        when(eventSessionService.completedSessionIds(any(LocalDateTime.class)))
                .thenReturn(List.of(1L, 2L, 3L));

        doThrow(new IllegalStateException("2번 회차 보증금 반환 실패"))
                .when(eventSessionService)
                .completedSession(eq(2L), any(LocalDateTime.class));

        assertDoesNotThrow(eventSessionScheduler::processEventCompleted);

        ArgumentCaptor<LocalDateTime> nowCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        InOrder order = inOrder(eventSessionService);

        order.verify(eventSessionService).completedSessionIds(nowCaptor.capture());
        LocalDateTime now = nowCaptor.getValue();

        // 조회할 때 사용한 시각을 각 회차 처리에도 전달하는지 확인한다.
        order.verify(eventSessionService).completedSession(1L, now);
        order.verify(eventSessionService).completedSession(2L, now);
        order.verify(eventSessionService).completedSession(3L, now);
    }

    @Test
    void 종료_대상_회차가_없으면_종료_처리를_호출하지_않는다() {
        when(eventSessionService.completedSessionIds(any(LocalDateTime.class)))
                .thenReturn(List.of());

        eventSessionScheduler.processEventCompleted();

        verify(eventSessionService).completedSessionIds(any(LocalDateTime.class));
        verify(eventSessionService, never())
                .completedSession(anyLong(), any(LocalDateTime.class));
    }
}