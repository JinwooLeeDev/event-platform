
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    private Event event;
    private Company company;
    private EventCategory category;

    @BeforeEach
    void setUp() {
        company = mock(Company.class);
        lenient().when(company.getId()).thenReturn(1L);

        category = mock(EventCategory.class);
        lenient().when(category.getId()).thenReturn(1L);
        lenient().when(category.getName()).thenReturn("전시");

        event = Event.builder()
                .id(10L)
                .company(company)
                .category(category)
                .title("부산 전시회")
                .summary("전시회 소개")
                .description("전시회 상세 설명")
                .address("부산광역시 해운대구")
                .status(EventStatus.PENDING_REVIEW)
                .applyStartAt(LocalDateTime.now())
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private EventRequest.SaveDto createSaveDto() {
        EventRequest.SaveDto dto = new EventRequest.SaveDto();
        dto.setTitle("부산 전시회");
        dto.setSummary("전시회 소개");
        dto.setDescription("전시회 상세 설명");
        dto.setAddress("부산광역시 해운대구");
        dto.setCategoryId(1L);
        dto.setApplyStartAt(LocalDateTime.now());
        dto.setApplyEndAt(LocalDateTime.now().plusDays(7));
        return dto;
    }

    private EventRequest.UpdateDto createUpdateDto() {
        EventRequest.UpdateDto dto = new EventRequest.UpdateDto();
        dto.setTitle("수정된 전시회");
        dto.setSummary("수정된 소개");
        dto.setDescription("수정된 상세 설명");
        dto.setAddress("부산광역시 수영구");
        dto.setCategoryId(1L);
        dto.setApplyStartAt(LocalDateTime.now());
        dto.setApplyEndAt(LocalDateTime.now().plusDays(10));
        return dto;
    }

    // 행사 등록 성공
    @Test
    void save_test() {
        EventRequest.SaveDto dto = createSaveDto();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(eventCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventService.save(dto, 1L);

        assertEquals("부산 전시회", result.getTitle());
        assertEquals(EventStatus.PENDING_REVIEW, result.getStatus());
        assertEquals(company, result.getCompany());
        assertEquals(category, result.getCategory());

        verify(eventRepository).save(any(Event.class));
    }

    // 행사 등록 시 기업이 없으면 404
    @Test
    void save_companyNotFound_test() {
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.save(createSaveDto(), 1L));

        verify(eventRepository, never()).save(any(Event.class));
    }

    // 행사 등록 시 카테고리가 없으면 404
    @Test
    void save_categoryNotFound_test() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(eventCategoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.save(createSaveDto(), 1L));

        verify(eventRepository, never()).save(any(Event.class));
    }

    // 공개 행사 목록 조회
    @Test
    void findAllPage_test() {
        Page<Event> eventPage = new PageImpl<>(
                List.of(event),
                PageRequest.of(0, 10),
                1
        );

        when(eventRepository.findAllPublished(
                eq(EventStatus.PUBLISHED), any()))
                .thenReturn(eventPage);

        EventResponse.PageDto result =
                eventService.findAllPage(1, 10, "");

        assertEquals(1, result.getEventList().size());
        assertEquals("부산 전시회", result.getEventList().get(0).getTitle());

        verify(eventRepository).findAllPublished(
                eq(EventStatus.PUBLISHED), any());
    }

    // 키워드 검색
    @Test
    void findAllPage_keyword_test() {
        Page<Event> eventPage = new PageImpl<>(List.of(event));

        when(eventRepository.findAllKeyWord(
                eq(EventStatus.PUBLISHED), eq("전시"), any()))
                .thenReturn(eventPage);

        EventResponse.PageDto result =
                eventService.findAllPage(1, 10, "전시");

        assertEquals(1, result.getEventList().size());

        verify(eventRepository).findAllKeyWord(
                eq(EventStatus.PUBLISHED), eq("전시"), any());
    }

    // 행사 상세 조회
    @Test
    void findById_test() {
        when(eventRepository.findByDetail(10L))
                .thenReturn(Optional.of(event));

        when(registrationRepository.countTotalByEventIdAndStatus(
                10L, RegistrationStatus.CONFIRMED)).thenReturn(3L);

        when(registrationRepository.countTotalByEventIdAndStatus(
                10L, RegistrationStatus.PENDING)).thenReturn(2L);

        when(registrationRepository.countTotalByEventIdAndStatus(
                10L, RegistrationStatus.WAITING)).thenReturn(4L);

        when(registrationRepository.countConfirmedByEventIdAndUserId(
                10L, 100L, RegistrationStatus.CONFIRMED)).thenReturn(1L);

        EventResponse.DetailDto result =
                eventService.findById(10L, 1L, 100L);

        assertEquals(10L, result.getId());
        assertEquals("부산 전시회", result.getTitle());
        assertTrue(result.isOwner());
        assertEquals(5L, result.getConfirmedCount());
        assertEquals(4L, result.getWaitingCount());
        assertTrue(result.isChatAvailable());
    }

    // 존재하지 않는 행사 상세 조회
    @Test
    void findById_notFound_test() {
        when(eventRepository.findByDetail(999L))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventService.findById(999L, null, null));
    }

    // 비로그인 회원은 채팅 참가 불가
    @Test
    void findById_guest_chatUnavailable_test() {
        when(eventRepository.findByDetail(10L))
                .thenReturn(Optional.of(event));

        when(registrationRepository.countTotalByEventIdAndStatus(
                anyLong(), any())).thenReturn(0L);

        EventResponse.DetailDto result =
                eventService.findById(10L, null, null);

        assertFalse(result.isOwner());
        assertFalse(result.isChatAvailable());

        verify(registrationRepository, never())
                .countConfirmedByEventIdAndUserId(
                        anyLong(), anyLong(), any());
    }

    // 행사 수정 화면 조회
    @Test
    void updateForm_test() {
        when(eventRepository.findByDetail(10L))
                .thenReturn(Optional.of(event));

        EventResponse.UpdateFormDto result =
                eventService.update(10L, 1L);

        assertEquals(10L, result.getId());
        assertEquals("부산 전시회", result.getTitle());
    }

    // 다른 기업은 행사 수정 화면에 접근할 수 없음
    @Test
    void updateForm_forbidden_test() {
        when(eventRepository.findByDetail(10L))
                .thenReturn(Optional.of(event));

        assertThrows(Exception403.class,
                () -> eventService.update(10L, 2L));
    }

    // 행사 수정
    @Test
    void update_test() {
        when(eventRepository.findByDetail(10L))
                .thenReturn(Optional.of(event));

        when(eventCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        EventRequest.UpdateDto dto = createUpdateDto();

        Event result = eventService.update(10L, 1L, dto);

        assertEquals("수정된 전시회", result.getTitle());
        assertEquals("수정된 소개", result.getSummary());
        assertEquals("부산광역시 수영구", result.getAddress());
        assertEquals(category, result.getCategory());
    }

    // 다른 기업은 행사 수정 불가
    @Test
    void update_forbidden_test() {
        when(eventRepository.findByDetail(10L))
                .thenReturn(Optional.of(event));

        assertThrows(Exception403.class,
                () -> eventService.update(10L, 2L, createUpdateDto()));

        verify(eventCategoryRepository, never()).findById(anyLong());
    }

    // 보완 요청된 행사 재제출
    @Test
    void resubmit_test() {
        event.setStatus(EventStatus.FEEDBACK_REQUIRED);
        event.setFeedbackMessage("주소를 확인해주세요.");

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        Event result = eventService.resubmit(10L, 1L);

        assertEquals(EventStatus.PENDING_REVIEW, result.getStatus());
        assertNull(result.getFeedbackMessage());
    }

    // 보완 요청 상태가 아니면 재제출 불가
    @Test
    void resubmit_invalidStatus_test() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        assertThrows(Exception400.class,
                () -> eventService.resubmit(10L, 1L));
    }

    // 다른 기업은 재제출 불가
    @Test
    void resubmit_forbidden_test() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        assertThrows(Exception403.class,
                () -> eventService.resubmit(10L, 2L));
    }

    // 행사 삭제
    @Test
    void delete_test() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        eventService.delete(10L, 1L);

        verify(eventRepository).delete(event);
    }

    // 다른 기업은 행사 삭제 불가
    @Test
    void delete_forbidden_test() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        assertThrows(Exception403.class,
                () -> eventService.delete(10L, 2L));

        verify(eventRepository, never()).delete(any(Event.class));
    }

    // 행사 취소
    @Test
    void cancel_test() {
        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason("행사 운영 사정");

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        Event result = eventService.cancel(10L, dto);

        assertEquals(EventStatus.CANCELLED, result.getStatus());
        assertEquals("행사 운영 사정", result.getCancellationReason());
        assertNotNull(result.getCancelledAt());
    }

    // 이미 취소된 행사
    @Test
    void cancel_alreadyCancelled_test() {
        event.setStatus(EventStatus.CANCELLED);

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason("취소 사유");

        assertThrows(Exception400.class,
                () -> eventService.cancel(10L, dto));
    }

    // 행사 승인
    @Test
    void approve_test() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        Event result = eventService.approve(10L);

        assertEquals(EventStatus.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedAt());
    }

    // 승인 대기 상태가 아니면 승인 불가
    @Test
    void approve_invalidStatus_test() {
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        assertThrows(Exception400.class,
                () -> eventService.approve(10L));
    }

    // 보완 요청
    @Test
    void requestFeedback_test() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        Event result =
                eventService.requestFeedback(10L, "행사 설명을 보완해주세요.");

        assertEquals(EventStatus.FEEDBACK_REQUIRED, result.getStatus());
        assertEquals("행사 설명을 보완해주세요.", result.getFeedbackMessage());
    }

    // 승인 대기 상태가 아니면 보완 요청 불가
    @Test
    void requestFeedback_invalidStatus_test() {
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        assertThrows(Exception400.class,
                () -> eventService.requestFeedback(10L, "보완 요청"));
    }

    // 기업의 보완 요청 행사 목록
    @Test
    void findAllFeedback_test() {
        when(eventRepository.findAllByCompanyIdAndStatus(
                1L, EventStatus.FEEDBACK_REQUIRED))
                .thenReturn(List.of(event));

        List<Event> result = eventService.findAllFeedback(1L);

        assertEquals(1, result.size());

        verify(eventRepository).findAllByCompanyIdAndStatus(
                1L, EventStatus.FEEDBACK_REQUIRED);
    }

    // 기업의 전체 행사 목록
    @Test
    void findAllByCompanyId_test() {
        when(eventRepository.findAllByCompanyId(1L))
                .thenReturn(List.of(event));

        EventResponse.ListDto result =
                eventService.findAllByCompanyId(1L);

        assertEquals(1, result.getEventList().size());
        assertEquals("부산 전시회",
                result.getEventList().get(0).getTitle());
    }
}
