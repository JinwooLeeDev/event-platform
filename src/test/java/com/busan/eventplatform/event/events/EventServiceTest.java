
package com.busan.eventplatform.event.events;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.event.event_category.EventCategoryRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import com.busan.eventplatform.user.registration.RegistrationStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventCategoryRepository eventCategoryRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private EventService eventService;

    private Company company;
    private EventCategory category;
    private Event event;

    @BeforeEach
    void setUp() {
        company = mock(Company.class);
        category = mock(EventCategory.class);
        event = mock(Event.class);
    }

    // 행사 등록

    @Test
    void save_success() {
        EventRequest.SaveDto dto = mock(EventRequest.SaveDto.class);

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));
        when(eventCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(dto.getCategoryId()).thenReturn(1L);
        when(dto.toEntity()).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);

        Event result = eventService.save(dto, 1L);

        assertNotNull(result);
        verify(eventRepository).save(event);
        verify(event).setCompany(company);
        verify(event).setCategory(category);
        verify(event).setStatus(EventStatus.PENDING_REVIEW);
    }

    @Test
    void save_companyNotFound() {
        EventRequest.SaveDto dto = mock(EventRequest.SaveDto.class);

        when(companyRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.save(dto, 1L));
    }

    @Test
    void save_categoryNotFound() {
        EventRequest.SaveDto dto = mock(EventRequest.SaveDto.class);

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));
        when(dto.getCategoryId()).thenReturn(1L);
        when(eventCategoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.save(dto, 1L));
    }

    // 행사 상세 조회

    @Test
    void findById_success() {
        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));
        when(event.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(1L);

        when(registrationRepository.countTotalByEventIdAndStatus(
                1L, RegistrationStatus.CONFIRMED)).thenReturn(3L);
        when(registrationRepository.countTotalByEventIdAndStatus(
                1L, RegistrationStatus.PENDING)).thenReturn(2L);
        when(registrationRepository.countTotalByEventIdAndStatus(
                1L, RegistrationStatus.WAITING)).thenReturn(1L);
        when(registrationRepository.countConfirmedByEventIdAndUserId(
                1L, 10L, RegistrationStatus.CONFIRMED)).thenReturn(1L);

        EventResponse.DetailDto result =
                eventService.findById(1L, 1L, 10L);

        assertNotNull(result);
        verify(eventRepository).findByDetail(1L);
    }

    @Test
    void findById_notFound() {
        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.findById(1L, null, null));
    }

    @Test
    void findById_guestCannotChat() {
        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));

        when(registrationRepository.countTotalByEventIdAndStatus(
                anyLong(), any())).thenReturn(0L);

        EventResponse.DetailDto result =
                eventService.findById(1L, null, null);

        assertNotNull(result);

        verify(registrationRepository, never())
                .countConfirmedByEventIdAndUserId(
                        anyLong(), anyLong(), any());
    }

    // 행사 수정 화면

    @Test
    void updateForm_success() {
        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(1L)).thenReturn(true);

        EventResponse.UpdateFormDto result =
                eventService.update(1L, 1L);

        assertNotNull(result);
    }

    @Test
    void updateForm_forbidden() {
        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(2L)).thenReturn(false);

        assertThrows(Exception403.class,
                () -> eventService.update(1L, 2L));
    }

    @Test
    void updateForm_nullCompanyId() {
        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));

        assertThrows(Exception403.class,
                () -> eventService.update(1L, null));
    }

    // 행사 수정 저장

    @Test
    void update_success() {
        EventRequest.UpdateDto dto = mock(EventRequest.UpdateDto.class);

        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(1L)).thenReturn(true);
        when(dto.getCategoryId()).thenReturn(1L);
        when(eventCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        Event result = eventService.update(1L, 1L, dto);

        assertNotNull(result);
        verify(dto).toEntity(event, category);
    }

    @Test
    void update_forbidden() {
        EventRequest.UpdateDto dto = mock(EventRequest.UpdateDto.class);

        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(2L)).thenReturn(false);

        assertThrows(Exception403.class,
                () -> eventService.update(1L, 2L, dto));
    }

    @Test
    void update_nullCompanyId() {
        EventRequest.UpdateDto dto = mock(EventRequest.UpdateDto.class);

        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));

        assertThrows(Exception403.class,
                () -> eventService.update(1L, null, dto));
    }

    // 행사 재제출

    @Test
    void resubmit_success() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(1L)).thenReturn(true);
        when(event.getStatus()).thenReturn(EventStatus.FEEDBACK_REQUIRED);

        Event result = eventService.resubmit(1L, 1L);

        assertNotNull(result);
        verify(event).setStatus(EventStatus.PENDING_REVIEW);
        verify(event).setFeedbackMessage(null);
    }

    @Test
    void resubmit_invalidStatus() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(1L)).thenReturn(true);
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);

        assertThrows(Exception400.class,
                () -> eventService.resubmit(1L, 1L));
    }

    @Test
    void resubmit_forbidden() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(2L)).thenReturn(false);

        assertThrows(Exception403.class,
                () -> eventService.resubmit(1L, 2L));
    }

    // 행사 삭제

    @Test
    void delete_success() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(1L)).thenReturn(true);

        assertDoesNotThrow(() -> eventService.delete(1L, 1L));

        verify(eventRepository).delete(event);
    }

    @Test
    void delete_forbidden() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(2L)).thenReturn(false);

        assertThrows(Exception403.class,
                () -> eventService.delete(1L, 2L));
    }

    @Test
    void delete_notFound() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.delete(1L, 1L));
    }

    // 행사 취소

    @Test
    void cancel_success() {
        EventRequest.CancelDto dto = mock(EventRequest.CancelDto.class);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(dto.getCancellationReason()).thenReturn("행사 취소");

        Event result = eventService.cancel(1L, dto);

        assertNotNull(result);
        verify(event).setStatus(EventStatus.CANCELLED);
        verify(event).setCancellationReason("행사 취소");
        verify(event).setCancelledAt(any());
    }

    @Test
    void cancel_alreadyCancelled() {
        EventRequest.CancelDto dto = mock(EventRequest.CancelDto.class);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.CANCELLED);

        assertThrows(Exception400.class,
                () -> eventService.cancel(1L, dto));
    }

    // 행사 승인

    @Test
    void approve_success() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PENDING_REVIEW);

        Event result = eventService.approve(1L);

        assertNotNull(result);
        verify(event).setStatus(EventStatus.PUBLISHED);
        verify(event).setPublishedAt(any());
    }

    @Test
    void approve_invalidStatus() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);

        assertThrows(Exception400.class,
                () -> eventService.approve(1L));
    }

    // 행사 보완 요청

    @Test
    void requestFeedback_success() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PENDING_REVIEW);

        Event result =
                eventService.requestFeedback(1L, "주소를 수정해주세요.");

        assertNotNull(result);
        verify(event).setStatus(EventStatus.FEEDBACK_REQUIRED);
        verify(event).setFeedbackMessage("주소를 수정해주세요.");
    }

    @Test
    void requestFeedback_invalidStatus() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);

        assertThrows(Exception400.class,
                () -> eventService.requestFeedback(1L, "수정해주세요."));
    }

    // 보완 요청 행사 목록

    @Test
    void findAllFeedback_success() {
        when(eventRepository.findAllByCompanyIdAndStatus(
                1L, EventStatus.FEEDBACK_REQUIRED))
                .thenReturn(List.of(event));

        List<Event> result = eventService.findAllFeedback(1L);

        assertEquals(1, result.size());
    }

    // 기업 행사 목록

    @Test
    void findAllByCompanyId_success() {
        when(eventRepository.findAllByCompanyId(1L))
                .thenReturn(List.of(event));

        EventResponse.ListDto result =
                eventService.findAllByCompanyId(1L);

        assertNotNull(result);
        verify(eventRepository).findAllByCompanyId(1L);
    }
}
