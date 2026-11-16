
package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.registration.RegistrationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSessionServiceTest {

    @Mock
    private EventSessionRepository eventSessionRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private EventSessionService eventSessionService;

    private Company company;
    private Event event;
    private EventSession session;

    private final Long eventId = 100L;
    private final Long companyId = 1L;
    private final Long sessionId = 200L;

    @BeforeEach
    void setUp() {
        company = mock(Company.class);

        event = Event.builder()
                .id(eventId)
                .company(company)
                .title("부산 전시 행사")
                .build();

        session = createSession();
    }

    private void setCompanyOwner() {
        when(company.getId()).thenReturn(companyId);
    }

    private EventSession createSession() {
        return EventSession.builder()
                .id(sessionId)
                .event(event)
                .name("1회차")
                .startsAt(LocalDateTime.of(2026, 12, 1, 10, 0))
                .endsAt(LocalDateTime.of(2026, 12, 1, 18, 0))
                .applyStartAt(LocalDateTime.of(2026, 11, 20, 9, 0))
                .applyEndAt(LocalDateTime.of(2026, 11, 30, 18, 0))
                .capacity(30)
                .occupiedCount(5)
                .nextReceptionNumber(1)
                .holdDurationSeconds(300)
                .pricingType("FREE")
                .fee(0)
                .reservationDepositPoints(0)
                .minimumParticipants(1)
                .minimumCheckAt(LocalDateTime.of(2026, 12, 1, 18, 0))
                .status(EventSessionStatus.SCHEDULED)
                .build();
    }

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

    private EventSessionRequest.UpdateDto createUpdateDto(int capacity) {
        EventSessionRequest.UpdateDto dto =
                new EventSessionRequest.UpdateDto();

        dto.setName("수정된 1회차");
        dto.setStartsAt(LocalDateTime.of(2026, 12, 2, 10, 0));
        dto.setApplyStartAt(LocalDateTime.of(2026, 11, 20, 9, 0));
        dto.setApplyEndAt(LocalDateTime.of(2026, 11, 30, 18, 0));
        dto.setEndsAt(LocalDateTime.of(2026, 12, 2, 18, 0));
        dto.setCapacity(capacity);
        dto.setHoldDurationSeconds(600);
        dto.setPricingType("FREE");
        dto.setFee(0);
        dto.setReservationDepositPoints(0);
        dto.setMinimumParticipants(1);
        dto.setMinimumCheckAt(LocalDateTime.of(2026, 12, 2, 18, 0));

        return dto;
    }

    // 회차 등록

    @Test
    @DisplayName("회차를 정상적으로 등록한다")
    void save_success() {
        setCompanyOwner();

        EventSessionRequest.SaveDto dto = createSaveDto();

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(eventSessionRepository.save(any(EventSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventSession result =
                eventSessionService.save(eventId, companyId, dto);

        assertNotNull(result);
        assertEquals("1회차", result.getName());
        assertEquals(30, result.getCapacity());
        assertEquals(0, result.getOccupiedCount());
        assertEquals(1, result.getNextReceptionNumber());
        assertEquals(EventSessionStatus.SCHEDULED, result.getStatus());

        verify(eventSessionRepository).save(any(EventSession.class));
    }

    @Test
    @DisplayName("존재하지 않는 행사에는 회차를 등록할 수 없다")
    void save_eventNotFound() {
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventSessionService.save(
                        eventId, companyId, createSaveDto()
                )
        );

        verify(eventSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 기업은 회차를 등록할 수 없다")
    void save_forbidden() {
        setCompanyOwner();

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception403.class,
                () -> eventSessionService.save(
                        eventId, 999L, createSaveDto()
                )
        );

        verify(eventSessionRepository, never()).save(any());
    }

    // 회차 수정

    @Test
    @DisplayName("존재하지 않는 회차를 수정하면 404 예외가 발생한다")
    void update_sessionNotFound() {
        when(eventSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventSessionService.update(
                        sessionId, createUpdateDto(40), companyId
                )
        );
    }

    @Test
    @DisplayName("회차 정원을 증가시키면 대기자가 승격된다")
    void update_capacityIncreased() {
        setCompanyOwner();

        when(eventSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        EventSession result = eventSessionService.update(
                sessionId,
                createUpdateDto(40),
                companyId
        );

        assertEquals(40, result.getCapacity());
        assertEquals("수정된 1회차", result.getName());

        verify(registrationService).promoteWaitingUsers(sessionId);
    }

    @Test
    @DisplayName("회차 정원이 증가하지 않으면 대기자를 승격하지 않는다")
    void update_capacityNotIncreased() {
        setCompanyOwner();

        when(eventSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        eventSessionService.update(
                sessionId,
                createUpdateDto(30),
                companyId
        );

        verify(registrationService, never())
                .promoteWaitingUsers(anyLong());
    }

    @Test
    @DisplayName("다른 기업은 회차를 수정할 수 없다")
    void update_forbidden() {
        setCompanyOwner();

        when(eventSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        assertThrows(
                Exception403.class,
                () -> eventSessionService.update(
                        sessionId, createUpdateDto(40), 999L
                )
        );

        verify(registrationService, never())
                .promoteWaitingUsers(anyLong());
    }

    @Test
    @DisplayName("현재 점유 인원보다 작은 정원으로 수정할 수 없다")
    void update_capacityBelowOccupiedCount() {
        setCompanyOwner();

        when(eventSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        assertThrows(
                Exception400.class,
                () -> eventSessionService.update(
                        sessionId, createUpdateDto(3), companyId
                )
        );
    }

    // 회차 삭제

    @Test
    @DisplayName("회차를 정상적으로 삭제한다")
    void delete_success() {
        setCompanyOwner();

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        assertDoesNotThrow(
                () -> eventSessionService.delete(sessionId, companyId)
        );

        verify(eventSessionRepository).delete(session);
    }

    @Test
    @DisplayName("존재하지 않는 회차는 삭제할 수 없다")
    void delete_notFound() {
        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventSessionService.delete(sessionId, companyId)
        );

        verify(eventSessionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("다른 기업은 회차를 삭제할 수 없다")
    void delete_forbidden() {
        setCompanyOwner();

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        assertThrows(
                Exception403.class,
                () -> eventSessionService.delete(sessionId, 999L)
        );

        verify(eventSessionRepository, never()).delete(any());
    }
}
