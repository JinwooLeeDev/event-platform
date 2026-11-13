package com.busan.eventplatform.event.events;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventResponse {

    @Data
    public static class PageDto {
        private List<EventDto> eventList;
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private int size;
        private boolean isFirst;
        private boolean isLast;

        public PageDto(Page<Event> eventPage) {
            this.eventList = eventPage.getContent().stream()
                    .map(EventDto::new)
                    .toList();
            this.currentPage = eventPage.getNumber() + 1;
            this.totalPages = eventPage.getTotalPages();
            this.totalElements = eventPage.getTotalElements();
            this.size = eventPage.getSize();
            this.isFirst = eventPage.isFirst();
            this.isLast = eventPage.isLast();
        }
    }

    // 행사 목록 조회
    @Data
    public static class ListDto {
        private List<EventDto> eventList;
        public ListDto(List<Event> eventList) {
            this.eventList = eventList.stream()
                    .map(EventDto::new)
                    .toList();
        }
    }

    @Data
    public static class EventDto {
        private static final DateTimeFormatter APPLICATION_DATE_FORMAT =
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String title;
        private String summary;
        private String applyStartAt;
        private String applyEndAt;
        private LocalDateTime createdAt;
        private LocalDateTime publishedAt;
        private EventStatus status;
        private String feedbackMessage;
        private boolean paidReadyEvent; /// 신청한 행사중 "결제대기" 행사는 결제하기 버튼 보이게 하기위해 추가.
        private int sessionCount; /// 행사 결제할때 필요한 행사 회차수 조회를 위해 추가.

        public EventDto(Event event) {
            this.id = event.getId();
            if (event.getCategory() != null) {
                this.categoryId = event.getCategory().getId();
                this.categoryName = event.getCategory().getName();
            }
            this.title = event.getTitle();
            this.summary = event.getSummary();
            this.applyStartAt = event.getApplyStartAt() == null
                    ? "일정 확인 중" : event.getApplyStartAt().format(APPLICATION_DATE_FORMAT);
            this.applyEndAt = event.getApplyEndAt() == null
                    ? "일정 확인 중" : event.getApplyEndAt().format(APPLICATION_DATE_FORMAT);
            this.createdAt = event.getCreatedAt();
            this.publishedAt = event.getPublishedAt();
            this.status = event.getStatus();
            this.feedbackMessage = event.getFeedbackMessage();
            this.paidReadyEvent = event.getStatus() == EventStatus.PAID_READY;
        }
    }

    // 행사 상세 조회
    @Data
    public static class DetailDto {
        private Long id;
        private String title;
        private String companyName;
        private Long categoryId;
        private String categoryName;
        private String summary;
        private String description;
        private String address;
        private LocalDateTime applyStartAt;
        private LocalDateTime applyEndAt;
        private LocalDateTime publishedAt;
        private LocalDateTime createdAt;
        private boolean applicationClosed;
        private boolean owner;
        private Long confirmedCount;
        private Long waitingCount;
        private boolean chatAvailable; // 채팅 참가 버튼

        public DetailDto(Event event, boolean owner, Long confirmedCount, Long waitingCount, boolean chatAvailable) {
            this.id = event.getId();
            this.title = event.getTitle();
            if (event.getCompany() != null) {
                this.companyName = event.getCompany().getName();
            }
            if (event.getCategory() != null) {
                this.categoryId = event.getCategory().getId();
                this.categoryName = event.getCategory().getName();
            }
            this.summary = event.getSummary();
            this.description = event.getDescription();
            this.address = event.getAddress();
            this.applyStartAt = event.getApplyStartAt();
            this.applyEndAt = event.getApplyEndAt();
            this.publishedAt = event.getPublishedAt();
            this.createdAt = event.getCreatedAt();
            this.applicationClosed = event.isApplicationClosed();
            this.owner = owner;
            this.confirmedCount = confirmedCount;
            this.waitingCount = waitingCount;
            this.chatAvailable = chatAvailable;
        }
    }

    // 게시물 수정 화면 응답 DTO
    @Data
    public static class UpdateFormDto {
        private Long id;
        private String title;
        private Long companyId;
        private Long categoryId;
        private String categoryName;
        private String summary;
        private String description;
        private String address;
        private LocalDateTime applyStartAt;
        private LocalDateTime applyEndAt;
        private LocalDateTime publishedAt;
        private String feedbackMessage;

        public UpdateFormDto(Event event) {
            this.id = event.getId();
            this.title = event.getTitle();
            if (event.getCompany() != null) {
                this.companyId = event.getCompany().getId();
            }
            if (event.getCategory() != null) {
                this.categoryId = event.getCategory().getId();
                this.categoryName = event.getCategory().getName();
            }
            this.summary = event.getSummary();
            this.description = event.getDescription();
            this.address = event.getAddress();
            this.applyStartAt = event.getApplyStartAt();
            this.applyEndAt = event.getApplyEndAt();
            this.publishedAt = event.getPublishedAt();
            this.feedbackMessage = event.getFeedbackMessage();
        }
    }
}
