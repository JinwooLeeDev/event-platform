
package com.busan.eventplatform.event.events;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.event.event_category.EventCategoryRepository;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionRequest;
import com.busan.eventplatform.event.event_session.EventSessionService;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import com.busan.eventplatform.user.registration.RegistrationStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
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

    @Mock
    private EventSessionService eventSessionService;

    @Mock
    private EventSessionRepository eventSessionRepository;
    @Mock
    private com.busan.eventplatform.event.event_file.EventFileService eventFileService;

    @InjectMocks
    private EventService eventService;

    private Company company;
    private EventCategory category;
    private Event event;

    private final Long companyId = 1L;
    private final Long categoryId = 10L;
    private final Long eventId = 100L;

    @BeforeEach
    void setUp() {
        company = mock(Company.class);
        category = mock(EventCategory.class);

        event = Event.builder()
                .id(eventId)
                .company(company)
                .category(category)
                .title("부산 전시 행사")
                .summary("전시 행사 소개")
                .description("전시 행사 상세 설명")
                .address("부산광역시 해운대구")
                .status(EventStatus.PENDING_REVIEW)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private EventRequest.SaveDto createSaveDto() {
        EventRequest.SaveDto dto = new EventRequest.SaveDto();

        dto.setTitle("부산 전시 행사");
        dto.setSummary("전시 행사 소개");
        dto.setDescription("전시 행사 상세 설명");
        dto.setAddress("부산광역시 해운대구");
        dto.setCategoryId(categoryId);

        dto.setHasSessions(false);
        dto.setStartDate(LocalDate.of(2026, 12, 1));
        dto.setEndDate(LocalDate.of(2026, 12, 31));

        dto.setApplyStartAt(LocalDateTime.of(2026, 11, 20, 9, 0));
        dto.setApplyEndAt(LocalDateTime.of(2026, 11, 30, 18, 0));

        dto.setPricingType("FREE");
        dto.setFee(0);

        return dto;
    }

    private EventRequest.UpdateDto createUpdateDto() {
        EventRequest.UpdateDto dto = new EventRequest.UpdateDto();

        dto.setTitle("수정된 행사");
        dto.setSummary("수정된 소개");
        dto.setDescription("수정된 설명");
        dto.setAddress("부산광역시 수영구");
        dto.setCategoryId(categoryId);

        return dto;
    }

    private Event createOwnedEvent(EventStatus status) {
        return Event.builder()
                .id(eventId)
                .company(company)
                .category(category)
                .title("부산 전시 행사")
                .summary("전시 행사 소개")
                .description("전시 행사 상세 설명")
                .address("부산광역시 해운대구")
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // =========================
    // 행사 등록
    // =========================

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void save_preservesEachSessionRecruitmentPeriod(boolean commonPeriodPresent) {
        var dto = createSaveDto();
        dto.setHasSessions(true);
        if (!commonPeriodPresent) { dto.setApplyStartAt(null); dto.setApplyEndAt(null); }
        var firstStart = LocalDateTime.of(2026, 11, 21, 10, 0);
        var secondStart = firstStart.plusDays(2);
        var sessions = List.of(firstStart, secondStart).stream().map(start -> {
            var row = new EventSessionRequest.SaveDto();
            row.setName("Session"); row.setStartsAt(start.plusDays(10)); row.setEndsAt(start.plusDays(11));
            row.setApplyStartAt(start); row.setApplyEndAt(start.plusDays(3));
            row.setCapacity(30); row.setPricingType("FREE"); row.setMinimumParticipants(1);
            row.setMinimumCheckAt(start.plusDays(9));
            return row;
        }).toList();
        dto.setSessions(sessions);
        dto.validate();
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(eventCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(eventRepository.save(any())).thenAnswer(call -> {
            Event saved = call.getArgument(0); saved.setId(eventId); return saved;
        });
        when(eventSessionService.save(eq(eventId), eq(companyId), any())).thenAnswer(call -> {
            EventSessionRequest.SaveDto row = call.getArgument(2); row.validate(); return null;
        });
        eventService.save(dto, companyId);
        assertEquals(firstStart, sessions.get(0).getApplyStartAt());
        assertEquals(firstStart.plusDays(3), sessions.get(0).getApplyEndAt());
        assertEquals(secondStart, sessions.get(1).getApplyStartAt());
        assertEquals(secondStart.plusDays(3), sessions.get(1).getApplyEndAt());
        verify(eventSessionService, times(2)).save(eq(eventId), eq(companyId), any());
    }

    @Test
    @DisplayName("행사 등록 시 기본 회차를 생성한다")
    void save_createsDefaultSession() {
        EventRequest.SaveDto dto = createSaveDto();

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.of(company));

        when(eventCategoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> {
                    Event saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", eventId);
                    return saved;
                });

        Event savedEvent = eventService.save(dto, companyId);

        assertNotNull(savedEvent);
        assertEquals(eventId, savedEvent.getId());
        assertEquals(EventStatus.PENDING_REVIEW, savedEvent.getStatus());
        assertEquals("부산 전시 행사", savedEvent.getTitle());

        ArgumentCaptor<EventSessionRequest.SaveDto> captor =
                ArgumentCaptor.forClass(EventSessionRequest.SaveDto.class);

        verify(eventSessionService).save(
                eq(eventId),
                eq(companyId),
                captor.capture()
        );

        EventSessionRequest.SaveDto sessionDto = captor.getValue();

        assertEquals("기본 회차", sessionDto.getName());
        assertEquals(Integer.MAX_VALUE, sessionDto.getCapacity());
        assertEquals("FREE", sessionDto.getPricingType());
        assertEquals(0, sessionDto.getFee());
        assertEquals(0, sessionDto.getReservationDepositPoints());
        assertEquals(1, sessionDto.getMinimumParticipants());
        assertEquals(0, sessionDto.getHoldDurationSeconds());
    }

    @Test
    @DisplayName("기업이 존재하지 않으면 행사 등록에 실패한다")
    void save_companyNotFound() {
        EventRequest.SaveDto dto = createSaveDto();

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventService.save(dto, companyId)
        );

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("카테고리가 존재하지 않으면 행사 등록에 실패한다")
    void save_categoryNotFound() {
        EventRequest.SaveDto dto = createSaveDto();

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.of(company));

        when(eventCategoryRepository.findById(categoryId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventService.save(dto, companyId)
        );

        verify(eventRepository, never()).save(any());
    }

    // =========================
    // 행사 목록 조회
    // =========================

    @Test
    @DisplayName("행사 목록을 조회한다")
    void findAllPage_success() {
        Page<Event> page = new PageImpl<>(List.of(event));

        when(eventRepository.findPublishedFiltered(
                eq(EventStatus.PUBLISHED),
                eq(categoryId),
                eq("부산"),
                any(Pageable.class)
        )).thenReturn(page);

        when(eventSessionRepository.findAllByEventIds(anyList()))
                .thenReturn(List.of());

        EventResponse.PageDto response =
                eventService.findAllPage(1, 10, " 부산 ", categoryId);

        assertNotNull(response);

        verify(eventRepository).findPublishedFiltered(
                eq(EventStatus.PUBLISHED),
                eq(categoryId),
                eq("부산"),
                any(Pageable.class)
        );

        verify(eventSessionRepository).findAllByEventIds(anyList());
    }

    @Test
    @DisplayName("검색어가 null이면 빈 문자열로 조회한다")
    void findAllPage_nullKeyword() {
        when(eventRepository.findPublishedFiltered(
                eq(EventStatus.PUBLISHED),
                isNull(),
                eq(""),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        EventResponse.PageDto response =
                eventService.findAllPage(1, 10, null);

        assertNotNull(response);
    }

    @Test
    @DisplayName("페이지 번호가 0보다 작으면 첫 페이지로 조회한다")
    void findAllPage_normalizesPage() {
        when(eventRepository.findPublishedFiltered(
                eq(EventStatus.PUBLISHED),
                isNull(),
                eq(""),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        eventService.findAllPage(0, 10, null);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(eventRepository).findPublishedFiltered(
                eq(EventStatus.PUBLISHED),
                isNull(),
                eq(""),
                captor.capture()
        );

        assertEquals(0, captor.getValue().getPageNumber());
    }

    @Test
    @DisplayName("페이지 크기가 50보다 크면 50으로 제한한다")
    void findAllPage_limitsSize() {
        when(eventRepository.findPublishedFiltered(
                eq(EventStatus.PUBLISHED),
                isNull(),
                eq(""),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        eventService.findAllPage(1, 100, null);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(eventRepository).findPublishedFiltered(
                eq(EventStatus.PUBLISHED),
                isNull(),
                eq(""),
                captor.capture()
        );

        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("페이지 크기가 1보다 작으면 1로 제한한다")
    void findAllPage_minimumSize() {
        when(eventRepository.findPublishedFiltered(
                eq(EventStatus.PUBLISHED),
                isNull(),
                eq(""),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        eventService.findAllPage(1, 0, null);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(eventRepository).findPublishedFiltered(
                eq(EventStatus.PUBLISHED),
                isNull(),
                eq(""),
                captor.capture()
        );

        assertEquals(1, captor.getValue().getPageSize());
    }

    // =========================
    // 행사 상세 조회
    // =========================

    @Test
    @DisplayName("행사 상세 정보를 조회한다")
    void findById_success() {
        event = createOwnedEvent(EventStatus.PUBLISHED);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        when(registrationRepository.countTotalByEventIdAndStatus(
                eventId, RegistrationStatus.CONFIRMED
        )).thenReturn(3L);

        when(registrationRepository.countTotalByEventIdAndStatus(
                eventId, RegistrationStatus.PENDING
        )).thenReturn(2L);

        when(registrationRepository.countTotalByEventIdAndStatus(
                eventId, RegistrationStatus.WAITING
        )).thenReturn(4L);

        when(registrationRepository.countConfirmedByEventIdAndUserId(
                eventId, 20L, RegistrationStatus.CONFIRMED
        )).thenReturn(1L);

        EventResponse.DetailDto response =
                eventService.findById(eventId, companyId, 20L);

        assertNotNull(response);

        verify(eventRepository).findByDetail(eventId);

        verify(registrationRepository).countTotalByEventIdAndStatus(
                eventId, RegistrationStatus.CONFIRMED
        );

        verify(registrationRepository).countTotalByEventIdAndStatus(
                eventId, RegistrationStatus.PENDING
        );

        verify(registrationRepository).countTotalByEventIdAndStatus(
                eventId, RegistrationStatus.WAITING
        );

        verify(registrationRepository).countConfirmedByEventIdAndUserId(
                eventId, 20L, RegistrationStatus.CONFIRMED
        );
    }

    @Test
    @DisplayName("로그인하지 않은 사용자도 행사 상세를 조회할 수 있다")
    void findById_guest() {
        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        when(registrationRepository.countTotalByEventIdAndStatus(
                anyLong(), any(RegistrationStatus.class)
        )).thenReturn(0L);

        EventResponse.DetailDto response =
                eventService.findById(eventId, null, null);

        assertNotNull(response);

        verify(registrationRepository, never())
                .countConfirmedByEventIdAndUserId(
                        anyLong(), anyLong(), any()
                );
    }

    @Test
    @DisplayName("존재하지 않는 행사 상세 조회는 실패한다")
    void findById_notFound() {
        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventService.findById(eventId, null, null)
        );
    }

    // =========================
    // 행사 수정 화면 조회
    // =========================

    @Test
    @DisplayName("보완 요청된 행사의 수정 화면을 조회한다")
    void updateForm_success() {
        event = createOwnedEvent(EventStatus.FEEDBACK_REQUIRED);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        EventResponse.UpdateFormDto response =
                eventService.update(eventId, companyId);

        assertNotNull(response);
    }

    @Test
    @DisplayName("행사 수정 권한이 없으면 실패한다")
    void updateForm_forbidden() {
        event = createOwnedEvent(EventStatus.FEEDBACK_REQUIRED);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception403.class,
                () -> eventService.update(eventId, 2L)
        );
    }

    @Test
    @DisplayName("기업 ID가 null이면 수정 화면을 조회할 수 없다")
    void updateForm_nullCompanyId() {
        event = createOwnedEvent(EventStatus.FEEDBACK_REQUIRED);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception403.class,
                () -> eventService.update(eventId, null)
        );
    }

    @Test
    @DisplayName("승인된 행사는 수정 화면을 조회할 수 없다")
    void updateForm_published() {
        event = createOwnedEvent(EventStatus.PUBLISHED);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception400.class,
                () -> eventService.update(eventId, companyId)
        );
    }

    @Test
    @DisplayName("보완 요청 상태가 아닌 행사는 수정 화면을 조회할 수 없다")
    void updateForm_invalidStatus() {
        event = createOwnedEvent(EventStatus.PENDING_REVIEW);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception400.class,
                () -> eventService.update(eventId, companyId)
        );
    }

    // =========================
    // 행사 수정
    // =========================

    @Test
    @DisplayName("보완 요청된 행사를 수정한다")
    void update_success() {
        event = createOwnedEvent(EventStatus.FEEDBACK_REQUIRED);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        when(eventCategoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        Event result = eventService.update(
                eventId,
                companyId,
                createUpdateDto()
        );

        assertEquals("수정된 행사", result.getTitle());
        assertEquals("수정된 소개", result.getSummary());
        assertEquals("수정된 설명", result.getDescription());
        assertEquals("부산광역시 수영구", result.getAddress());
        assertSame(category, result.getCategory());
    }

    @Test
    @DisplayName("행사 수정 권한이 없으면 실패한다")
    void update_forbidden() {
        event = createOwnedEvent(EventStatus.FEEDBACK_REQUIRED);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception403.class,
                () -> eventService.update(
                        eventId, 2L, createUpdateDto()
                )
        );
    }

    @Test
    @DisplayName("승인된 행사는 수정할 수 없다")
    void update_published() {
        event = createOwnedEvent(EventStatus.PUBLISHED);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception400.class,
                () -> eventService.update(
                        eventId, companyId, createUpdateDto()
                )
        );
    }

    @Test
    @DisplayName("보완 요청 상태가 아니면 수정할 수 없다")
    void update_invalidStatus() {
        event = createOwnedEvent(EventStatus.PENDING_REVIEW);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception400.class,
                () -> eventService.update(
                        eventId, companyId, createUpdateDto()
                )
        );
    }

    @Test
    @DisplayName("수정할 카테고리가 없으면 실패한다")
    void update_categoryNotFound() {
        event = createOwnedEvent(EventStatus.FEEDBACK_REQUIRED);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        when(eventCategoryRepository.findById(categoryId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventService.update(
                        eventId, companyId, createUpdateDto()
                )
        );
    }

    // =========================
    // 행사 재제출
    // =========================

    @Test
    @DisplayName("보완 요청된 행사를 재제출한다")
    void resubmit_success() {
        event = createOwnedEvent(EventStatus.FEEDBACK_REQUIRED);
        event.setFeedbackMessage("행사 설명을 보완해주세요.");

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        Event result = eventService.resubmit(eventId, companyId);

        assertEquals(EventStatus.PENDING_REVIEW, result.getStatus());
        assertNull(result.getFeedbackMessage());
    }

    @Test
    void updateAndResubmit_savesCorrectionAndReturnsToPendingReview() {
        var corrected = createOwnedEvent(EventStatus.FEEDBACK_REQUIRED);
        corrected.setFeedbackMessage("Please update the description");
        when(company.getId()).thenReturn(companyId);
        when(eventRepository.findByDetail(eventId)).thenReturn(Optional.of(corrected));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(corrected));
        when(eventCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        var result = eventService.updateAndResubmit(eventId, companyId, createUpdateDto());

        assertEquals(createUpdateDto().getTitle(), result.getTitle());
        assertEquals(createUpdateDto().getDescription(), result.getDescription());
        assertEquals(EventStatus.PENDING_REVIEW, result.getStatus());
        assertNull(result.getFeedbackMessage());
    }

    @Test
    void updateAndResubmit_reusesSessionUpdateAndOptionalImageReplacement() {
        var corrected = createOwnedEvent(EventStatus.FEEDBACK_REQUIRED);
        when(company.getId()).thenReturn(companyId);
        when(eventRepository.findByDetail(eventId)).thenReturn(Optional.of(corrected));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(corrected));
        when(eventCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        var request = createUpdateDto();
        var row = new EventSessionRequest.UpdateDto();
        row.setId(12L); row.setName("Updated session"); row.setStartsAt(LocalDateTime.now().plusDays(4));
        row.setEndsAt(row.getStartsAt().plusHours(2)); row.setCapacity(30); row.setPricingType("PAID"); row.setFee(5000);
        row.setApplyStartAt(row.getStartsAt().minusDays(3)); row.setApplyEndAt(row.getStartsAt().minusDays(1));
        row.setMinimumParticipants(1); row.setMinimumCheckAt(row.getStartsAt().minusHours(1));
        request.setSessions(List.of(row));
        var detail = mock(com.busan.eventplatform.event.event_session.EventSessionResponse.DetailDto.class);
        when(detail.getEventId()).thenReturn(eventId);
        when(eventSessionService.findById(12L)).thenReturn(detail);
        var file = new org.springframework.mock.web.MockMultipartFile("file", "poster.png", "image/png", new byte[]{1});

        var result = eventService.updateAndResubmit(eventId, companyId, request, file);

        verify(eventSessionService).update(12L, row, companyId);
        verify(eventFileService).attachFile(eventId, file);
        assertEquals(EventStatus.PENDING_REVIEW, result.getStatus());
    }

    @Test
    void updateAndResubmit_rejectsSessionFromAnotherEventBeforeSaving() {
        var request = createUpdateDto();
        var row = new EventSessionRequest.UpdateDto();
        row.setId(12L); row.setName("Session"); row.setStartsAt(LocalDateTime.now().plusDays(4));
        row.setEndsAt(row.getStartsAt().plusHours(2)); row.setCapacity(30); row.setPricingType("FREE");
        row.setApplyStartAt(row.getStartsAt().minusDays(3)); row.setApplyEndAt(row.getStartsAt().minusDays(1));
        row.setMinimumParticipants(1); row.setMinimumCheckAt(row.getStartsAt().minusHours(1));
        request.setSessions(List.of(row));
        var detail = mock(com.busan.eventplatform.event.event_session.EventSessionResponse.DetailDto.class);
        when(detail.getEventId()).thenReturn(999L);
        when(eventSessionService.findById(12L)).thenReturn(detail);
        assertThrows(Exception403.class, () -> eventService.updateAndResubmit(eventId, companyId, request));
        verifyNoInteractions(eventRepository, eventFileService);
        verify(eventSessionService, never()).update(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("행사 재제출 권한이 없으면 실패한다")
    void resubmit_forbidden() {
        event = createOwnedEvent(EventStatus.FEEDBACK_REQUIRED);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception403.class,
                () -> eventService.resubmit(eventId, 2L)
        );
    }

    @Test
    @DisplayName("보완 요청 상태가 아니면 재제출할 수 없다")
    void resubmit_invalidStatus() {
        event = createOwnedEvent(EventStatus.PENDING_REVIEW);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception400.class,
                () -> eventService.resubmit(eventId, companyId)
        );
    }

    @Test
    @DisplayName("존재하지 않는 행사는 재제출할 수 없다")
    void resubmit_notFound() {
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventService.resubmit(eventId, companyId)
        );
    }

    // =========================
    // 행사 삭제
    // =========================

    @Test
    @DisplayName("행사를 삭제한다")
    void delete_success() {
        event = createOwnedEvent(EventStatus.PENDING_REVIEW);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertDoesNotThrow(
                () -> eventService.delete(eventId, companyId)
        );

        verify(eventRepository).delete(event);
    }

    @Test
    @DisplayName("삭제 권한이 없으면 실패한다")
    void delete_forbidden() {
        event = createOwnedEvent(EventStatus.PENDING_REVIEW);

        when(company.getId()).thenReturn(companyId);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception403.class,
                () -> eventService.delete(eventId, 2L)
        );

        verify(eventRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 행사는 삭제할 수 없다")
    void delete_notFound() {
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventService.delete(eventId, companyId)
        );
    }

    // =========================
    // 행사 취소
    // =========================

    @Test
    @DisplayName("행사를 취소한다")
    void cancel_success() {
        event = createOwnedEvent(EventStatus.PUBLISHED);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason("행사 운영 사정");

        LocalDateTime before = LocalDateTime.now();

        Event result = eventService.cancel(eventId, dto);

        LocalDateTime after = LocalDateTime.now();

        assertEquals(EventStatus.CANCELLED, result.getStatus());
        assertEquals("행사 운영 사정", result.getCancellationReason());
        assertNotNull(result.getCancelledAt());

        assertFalse(result.getCancelledAt().isBefore(before));
        assertFalse(result.getCancelledAt().isAfter(after));
    }

    @Test
    @DisplayName("이미 취소된 행사는 다시 취소할 수 없다")
    void cancel_alreadyCancelled() {
        event = createOwnedEvent(EventStatus.CANCELLED);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason("취소 사유");

        assertThrows(
                Exception400.class,
                () -> eventService.cancel(eventId, dto)
        );
    }

    @Test
    @DisplayName("존재하지 않는 행사는 취소할 수 없다")
    void cancel_notFound() {
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason("취소 사유");

        assertThrows(
                Exception404.class,
                () -> eventService.cancel(eventId, dto)
        );
    }

    // =========================
    // 행사 승인
    // =========================

    @Test
    @DisplayName("승인 대기 중인 행사를 승인한다")
    void approve_success() {
        event.setStatus(EventStatus.PENDING_REVIEW);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        Event result = eventService.approve(eventId);

        assertEquals(EventStatus.PAID_READY, result.getStatus());
    }

    @Test
    @DisplayName("승인 대기 상태가 아니면 승인할 수 없다")
    void approve_invalidStatus() {
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception400.class,
                () -> eventService.approve(eventId)
        );
    }

    @Test
    @DisplayName("존재하지 않는 행사는 승인할 수 없다")
    void approve_notFound() {
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventService.approve(eventId)
        );
    }

    // =========================
    // 행사 보완 요청
    // =========================

    @Test
    @DisplayName("승인 대기 중인 행사에 보완 요청한다")
    void requestFeedback_success() {
        event.setStatus(EventStatus.PENDING_REVIEW);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        Event result = eventService.requestFeedback(
                eventId,
                "행사 설명을 보완해주세요."
        );

        assertEquals(EventStatus.FEEDBACK_REQUIRED, result.getStatus());
        assertEquals(
                "행사 설명을 보완해주세요.",
                result.getFeedbackMessage()
        );
    }

    @Test
    @DisplayName("승인 대기 상태가 아니면 보완 요청할 수 없다")
    void requestFeedback_invalidStatus() {
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                Exception400.class,
                () -> eventService.requestFeedback(
                        eventId, "보완 요청"
                )
        );
    }

    @Test
    @DisplayName("존재하지 않는 행사는 보완 요청할 수 없다")
    void requestFeedback_notFound() {
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventService.requestFeedback(
                        eventId, "보완 요청"
                )
        );
    }

    // =========================
    // 기업별 보완 요청 행사 조회
    // =========================

    @Test
    @DisplayName("기업의 보완 요청 행사 목록을 조회한다")
    void findAllFeedback_success() {
        List<Event> events = List.of(event);

        when(eventRepository.findAllByCompanyIdAndStatus(
                companyId,
                EventStatus.FEEDBACK_REQUIRED
        )).thenReturn(events);

        List<Event> result = eventService.findAllFeedback(companyId);

        assertEquals(1, result.size());
        assertEquals(event, result.get(0));
    }

    // =========================
    // 기업별 행사 전체 조회
    // =========================

    @Test
    @DisplayName("기업이 등록한 행사 목록을 조회한다")
    void findAllByCompanyId_success() {
        when(eventRepository.findAllByCompanyId(companyId))
                .thenReturn(List.of(event));

        when(eventSessionRepository.findAllByEventIds(anyList()))
                .thenReturn(List.of());

        EventResponse.ListDto response =
                eventService.findAllByCompanyId(companyId);

        assertNotNull(response);

        verify(eventRepository).findAllByCompanyId(companyId);
        verify(eventSessionRepository).findAllByEventIds(anyList());
    }

    @Test
    @DisplayName("기업이 등록한 행사가 없으면 회차 조회를 하지 않는다")
    void findAllByCompanyId_empty() {
        when(eventRepository.findAllByCompanyId(companyId))
                .thenReturn(List.of());

        EventResponse.ListDto response =
                eventService.findAllByCompanyId(companyId);

        assertNotNull(response);

        verify(eventSessionRepository, never())
                .findAllByEventIds(anyList());
    }
}
