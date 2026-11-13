
package com.busan.eventplatform.event.eventSession;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionRequest;
import com.busan.eventplatform.event.event_session.EventSessionService;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.registration.RegistrationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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


    // 업체 소유 행사 생성
    private Event createEvent(Long companyId) {

        Company company = mock(Company.class);
        when(company.getId()).thenReturn(companyId);

        Event event = mock(Event.class);
        when(event.getCompany()).thenReturn(company);

        return event;
    }


    // 테스트용 회차 생성
    private EventSession createEventSession(Long companyId) {

        Event event = createEvent(companyId);

        return EventSession.builder()
                .id(10L)
                .event(event)
                .name("테스트 회차")
                .startsAt(LocalDateTime.now())
                .endsAt(LocalDateTime.now().plusHours(2))
                .capacity(10)
                .occupiedCount(5)
                .nextReceptionNumber(1)
                .holdDurationSeconds(300)
                .pricingType("FREE")
                .fee(0)
                .reservationDepositPoints(0)
                .minimumParticipants(2)
                .minimumCheckAt(LocalDateTime.now())
                .status(EventSessionStatus.SCHEDULED)
                .build();
    }


    // 회차 등록 성공
    @Test
    void save_test() {

        Event event = createEvent(1L);

        EventSessionRequest.SaveDto dto =
                new EventSessionRequest.SaveDto();

        dto.setName("테스트 회차");
        dto.setStartsAt(LocalDateTime.now());
        dto.setEndsAt(LocalDateTime.now().plusHours(2));
        dto.setCapacity(10);
        dto.setHoldDurationSeconds(300);
        dto.setPricingType("FREE");
        dto.setFee(0);
        dto.setReservationDepositPoints(0);
        dto.setMinimumParticipants(2);
        dto.setMinimumCheckAt(LocalDateTime.now());

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));

        when(eventSessionRepository.save(any(EventSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventSession result =
                eventSessionService.save(1L, 1L, dto);

        assertEquals("테스트 회차", result.getName());
        assertEquals(10, result.getCapacity());
        assertEquals(EventSessionStatus.SCHEDULED, result.getStatus());

        verify(eventSessionRepository).save(any(EventSession.class));
    }


    // 다른 업체의 행사에 회차 등록 시도
    @Test
    void save_forbidden_test() {

        Event event = createEvent(1L);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception403.class,
                () -> eventSessionService.save(
                        1L, 2L, new EventSessionRequest.SaveDto()
                )
        );

        verify(eventSessionRepository, never())
                .save(any(EventSession.class));
    }


    // 존재하지 않는 회차 조회
    @Test
    void findById_notFound_test() {

        when(eventSessionRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventSessionService.findById(10L)
        );
    }


    // 정원 증가 시 대기자 승격 호출
    @Test
    void update_capacityIncrease_promoteWaitingUsers_test() {

        EventSession eventSession = createEventSession(1L);

        when(eventSessionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(eventSession));

        EventSessionRequest.UpdateDto dto =
                new EventSessionRequest.UpdateDto();

        dto.setName("수정된 회차");
        dto.setStartsAt(LocalDateTime.now());
        dto.setEndsAt(LocalDateTime.now().plusHours(2));
        dto.setCapacity(15);
        dto.setHoldDurationSeconds(300);
        dto.setPricingType("FREE");
        dto.setFee(0);
        dto.setReservationDepositPoints(0);
        dto.setMinimumParticipants(2);
        dto.setMinimumCheckAt(LocalDateTime.now());

        eventSessionService.update(10L, dto, 1L);

        assertEquals(15, eventSession.getCapacity());

        verify(registrationService)
                .promoteWaitingUsers(10L);
    }


    // 다른 업체의 회차 삭제 시도
    @Test
    void delete_forbidden_test() {

        EventSession eventSession = createEventSession(1L);

        when(eventSessionRepository.findById(10L))
                .thenReturn(Optional.of(eventSession));

        assertThrows(
                Exception403.class,
                () -> eventSessionService.delete(10L, 2L)
        );

        verify(eventSessionRepository, never())
                .delete(any(EventSession.class));
    }
}
