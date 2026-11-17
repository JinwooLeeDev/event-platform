
package com.busan.eventplatform.event.eventSession;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionRequest;
import com.busan.eventplatform.event.event_session.EventSessionService;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.registration.RegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSessionServiceTest {

    @Mock
    EventSessionRepository eventSessionRepository;
    @Mock EventRepository eventRepository;
    @Mock RegistrationService registrationService;

    @InjectMocks
    EventSessionService eventSessionService;

    private Event createEvent(Long companyId) {
        Event event = mock(Event.class);
        com.busan.eventplatform.company.Company company =
                mock(com.busan.eventplatform.company.Company.class);

        when(event.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(companyId);

        return event;
    }

    @Test
    void 회차_등록_시_행사가_없으면_404() {
        EventSessionRequest.SaveDto dto =
                mock(EventSessionRequest.SaveDto.class);

        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventSessionService.save(1L, 1L, dto));

        verify(eventSessionRepository, never()).save(any());
    }

    @Test
    void 다른_기업의_행사에는_회차를_등록할_수_없다() {
        Event event = createEvent(2L);
        EventSessionRequest.SaveDto dto =
                mock(EventSessionRequest.SaveDto.class);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));

        assertThrows(Exception403.class,
                () -> eventSessionService.save(1L, 1L, dto));
    }


    @Test
    void 회차_등록에_성공한다() {
        Event event = createEvent(1L);
        EventSession session = mock(EventSession.class);
        EventSessionRequest.SaveDto dto =
                mock(EventSessionRequest.SaveDto.class);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));

        when(dto.getStartsAt()).thenReturn(now.plusDays(10));
        when(dto.getEndsAt()).thenReturn(now.plusDays(11));
        when(dto.getApplyStartAt()).thenReturn(now.plusDays(1));
        when(dto.getApplyEndAt()).thenReturn(now.plusDays(9));

        when(dto.toEntity(event)).thenReturn(session);
        when(eventSessionRepository.save(session)).thenReturn(session);

        EventSession result = eventSessionService.save(1L, 1L, dto);

        assertSame(session, result);
        verify(dto).validate();
        verify(eventSessionRepository).save(session);
    }


    @Test
    void 회차_단건_조회_시_없으면_404() {
        when(eventSessionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventSessionService.findById(1L));
    }

    @Test
    void 회차_수정_시_회차가_없으면_404() {
        EventSessionRequest.UpdateDto dto =
                mock(EventSessionRequest.UpdateDto.class);

        when(eventSessionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventSessionService.update(1L, dto, 1L));
    }

    @Test
    void 다른_기업은_회차를_수정할_수_없다() {
        Event event = createEvent(2L);
        EventSession session = mock(EventSession.class);
        EventSessionRequest.UpdateDto dto =
                mock(EventSessionRequest.UpdateDto.class);

        when(eventSessionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(session));
        when(session.getEvent()).thenReturn(event);

        assertThrows(Exception403.class,
                () -> eventSessionService.update(1L, dto, 1L));

        verify(session, never()).update(any());
    }

    @Test
    void 회차_수정_시_정원이_증가하면_대기자를_승격한다() {
        Event event = createEvent(1L);
        EventSession session = mock(EventSession.class);
        EventSessionRequest.UpdateDto dto =
                mock(EventSessionRequest.UpdateDto.class);

        when(eventSessionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(session));
        when(session.getEvent()).thenReturn(event);
        when(session.getCapacity()).thenReturn(10, 20);

        EventSession result = eventSessionService.update(1L, dto, 1L);

        assertSame(session, result);
        verify(session).update(dto);
        verify(registrationService).promoteWaitingUsers(1L);
    }

    @Test
    void 정원이_증가하지_않으면_대기자를_승격하지_않는다() {
        Event event = createEvent(1L);
        EventSession session = mock(EventSession.class);
        EventSessionRequest.UpdateDto dto =
                mock(EventSessionRequest.UpdateDto.class);

        when(eventSessionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(session));
        when(session.getEvent()).thenReturn(event);
        when(session.getCapacity()).thenReturn(20, 10);

        eventSessionService.update(1L, dto, 1L);

        verify(registrationService, never()).promoteWaitingUsers(anyLong());
    }

    @Test
    void 회차_삭제_시_회차가_없으면_404() {
        when(eventSessionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventSessionService.delete(1L, 1L));
    }

    @Test
    void 다른_기업은_회차를_삭제할_수_없다() {
        Event event = createEvent(2L);
        EventSession session = mock(EventSession.class);

        when(eventSessionRepository.findById(1L))
                .thenReturn(Optional.of(session));
        when(session.getEvent()).thenReturn(event);

        assertThrows(Exception403.class,
                () -> eventSessionService.delete(1L, 1L));

        verify(eventSessionRepository, never())
                .deleteIfNoRegistrationHistory(anyLong());
    }

    @Test
    void 신청_이력이_없으면_회차를_실제_삭제한다() {
        Event event = createEvent(1L);
        EventSession session = mock(EventSession.class);

        when(eventSessionRepository.findById(1L))
                .thenReturn(Optional.of(session));
        when(session.getEvent()).thenReturn(event);
        when(eventSessionRepository.deleteIfNoRegistrationHistory(1L))
                .thenReturn(1);

        eventSessionService.delete(1L, 1L);

        verify(eventSessionRepository)
                .deleteIfNoRegistrationHistory(1L);
        verify(session, never()).setStatus(any());
    }

    @Test
    void 신청_이력이_있으면_회차를_취소_상태로_변경한다() {
        Event event = createEvent(1L);
        EventSession session = mock(EventSession.class);

        when(eventSessionRepository.findById(1L))
                .thenReturn(Optional.of(session));
        when(session.getEvent()).thenReturn(event);
        when(eventSessionRepository.deleteIfNoRegistrationHistory(1L))
                .thenReturn(0);

        eventSessionService.delete(1L, 1L);

        verify(session).setStatus(EventSessionStatus.CANCELLED);
    }
}
