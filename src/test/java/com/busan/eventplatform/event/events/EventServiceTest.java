
package com.busan.eventplatform.event.events;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.event.event_category.EventCategoryRepository;
import com.busan.eventplatform.event.event_file.EventFileService;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionService;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock EventCategoryRepository eventCategoryRepository;
    @Mock CompanyRepository companyRepository;
    @Mock RegistrationRepository registrationRepository;
    @Mock EventSessionService eventSessionService;
    @Mock EventSessionRepository eventSessionRepository;
    @Mock EventFileService eventFileService;

    @InjectMocks EventService eventService;

    @Test
    void 행사_등록_시_기업이_없으면_404() {
        EventRequest.SaveDto dto = mock(EventRequest.SaveDto.class);

        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.save(dto, 1L, null));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void 행사_등록_시_카테고리가_없으면_404() {
        EventRequest.SaveDto dto = mock(EventRequest.SaveDto.class);

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(mock(Company.class)));
        when(dto.getCategoryId()).thenReturn(10L);
        when(eventCategoryRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.save(dto, 1L, null));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void 행사_등록_시_기본_회차를_생성한다() {
        EventRequest.SaveDto dto = mock(EventRequest.SaveDto.class);
        Company company = mock(Company.class);
        EventCategory category = mock(EventCategory.class);
        Event event = mock(Event.class);

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));
        when(eventCategoryRepository.findById(10L))
                .thenReturn(Optional.of(category));
        when(dto.getCategoryId()).thenReturn(10L);
        when(dto.toEntity()).thenReturn(event);
        when(dto.isHasSessions()).thenReturn(false);

        when(dto.getStartDate()).thenReturn(
                java.time.LocalDate.now().plusDays(10));
        when(dto.getEndDate()).thenReturn(
                java.time.LocalDate.now().plusDays(11));
        when(dto.getApplyStartAt()).thenReturn(
                java.time.LocalDateTime.now().plusDays(1));
        when(dto.getApplyEndAt()).thenReturn(
                java.time.LocalDateTime.now().plusDays(9));
        when(dto.getPricingType()).thenReturn("FREE");
        when(dto.getFee()).thenReturn(0);

        when(event.getId()).thenReturn(100L);
        when(eventRepository.save(event)).thenReturn(event);

        Event result = eventService.save(dto, 1L, null);

        assertSame(event, result);
        verify(event).setCompany(company);
        verify(event).setCategory(category);
        verify(event).setStatus(EventStatus.PENDING_REVIEW);
        verify(eventSessionService).save(eq(100L), eq(1L), any());
        verify(eventFileService).attachFile(100L, null);
    }

    @Test
    void 행사_상세_조회_시_행사가_없으면_404() {
        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.findById(1L, null, null));
    }

    @Test
    void 행사_수정_시_행사가_없으면_404() {
        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.update(1L, 1L,
                        mock(EventRequest.UpdateDto.class)));
    }

    @Test
    void 행사_수정_시_기업_소유자가_아니면_403() {
        Event event = mock(Event.class);

        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(2L)).thenReturn(false);

        assertThrows(Exception403.class,
                () -> eventService.update(1L, 2L,
                        mock(EventRequest.UpdateDto.class)));
    }

    @Test
    void 승인된_행사는_수정할_수_없다() {
        Event event = mock(Event.class);

        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(1L)).thenReturn(true);
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);

        assertThrows(Exception400.class,
                () -> eventService.update(1L, 1L,
                        mock(EventRequest.UpdateDto.class)));
    }

    @Test
    void 보완요청_상태가_아니면_수정할_수_없다() {
        Event event = mock(Event.class);

        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(1L)).thenReturn(true);
        when(event.getStatus()).thenReturn(EventStatus.PENDING_REVIEW);

        assertThrows(Exception400.class,
                () -> eventService.update(1L, 1L,
                        mock(EventRequest.UpdateDto.class)));
    }

    @Test
    void 보완요청_상태의_행사를_수정한다() {
        Event event = mock(Event.class);
        EventCategory category = mock(EventCategory.class);
        EventRequest.UpdateDto dto = mock(EventRequest.UpdateDto.class);

        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(1L)).thenReturn(true);
        when(event.getStatus()).thenReturn(EventStatus.FEEDBACK_REQUIRED);
        when(dto.getCategoryId()).thenReturn(10L);
        when(eventCategoryRepository.findById(10L))
                .thenReturn(Optional.of(category));
        when(dto.toEntity(event, category)).thenReturn(event);

        Event result = eventService.update(1L, 1L, dto);

        assertSame(event, result);
        verify(dto).toEntity(event, category);
    }

    @Test
    void 재제출_시_행사가_없으면_404() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.resubmit(1L, 1L));
    }

    @Test
    void 재제출_시_소유자가_아니면_403() {
        Event event = mock(Event.class);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(2L)).thenReturn(false);

        assertThrows(Exception403.class,
                () -> eventService.resubmit(1L, 2L));
    }

    @Test
    void 보완요청_상태가_아니면_재제출할_수_없다() {
        Event event = mock(Event.class);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(1L)).thenReturn(true);
        when(event.getStatus()).thenReturn(EventStatus.PENDING_REVIEW);

        assertThrows(Exception400.class,
                () -> eventService.resubmit(1L, 1L));
    }

    @Test
    void 재제출하면_승인대기_상태로_변경하고_보완내용을_초기화한다() {
        Event event = mock(Event.class);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.isOwner(1L)).thenReturn(true);
        when(event.getStatus()).thenReturn(EventStatus.FEEDBACK_REQUIRED);

        Event result = eventService.resubmit(1L, 1L);

        assertSame(event, result);
        verify(event).setStatus(EventStatus.PENDING_REVIEW);
        verify(event).setFeedbackMessage(null);
    }

    @Test
    void 승인_대기_상태가_아니면_승인할_수_없다() {
        Event event = mock(Event.class);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);

        assertThrows(Exception400.class,
                () -> eventService.approve(1L));
    }

    @Test
    void 승인하면_결제대기_상태로_변경한다() {
        Event event = mock(Event.class);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PENDING_REVIEW);

        Event result = eventService.approve(1L);

        assertSame(event, result);
        verify(event).setStatus(EventStatus.PAID_READY);
    }

    @Test
    void 보완사유가_없으면_400() {
        assertThrows(Exception400.class,
                () -> eventService.requestFeedback(1L, null));

        assertThrows(Exception400.class,
                () -> eventService.requestFeedback(1L, "   "));
    }

    @Test
    void 보완사유가_천자를_초과하면_400() {
        String message = "가".repeat(1001);

        assertThrows(Exception400.class,
                () -> eventService.requestFeedback(1L, message));
    }

    @Test
    void 승인대기_상태가_아니면_보완요청할_수_없다() {
        Event event = mock(Event.class);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PAID_READY);

        assertThrows(Exception400.class,
                () -> eventService.requestFeedback(1L, "수정해주세요"));
    }

    @Test
    void 보완요청하면_상태와_사유를_저장한다() {
        Event event = mock(Event.class);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PENDING_REVIEW);

        Event result = eventService.requestFeedback(1L, " 수정해주세요 ");

        assertSame(event, result);
        verify(event).setStatus(EventStatus.FEEDBACK_REQUIRED);
        verify(event).setFeedbackMessage("수정해주세요");
    }

    @Test
    void 보완요청_목록을_기업별로_조회한다() {
        eventService.findAllFeedback(1L);

        verify(eventRepository).findAllByCompanyIdAndStatus(
                1L, EventStatus.FEEDBACK_REQUIRED);
    }
}
