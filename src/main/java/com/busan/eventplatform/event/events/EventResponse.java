package com.busan.eventplatform.event.events;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
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
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String title;
        private String summary;

        public EventDto(Event event) {
            this.id = event.getId();
            if (event.getCategory() != null) {
                this.categoryId = event.getCategory().getId();
                this.categoryName = event.getCategory().getName();
            }
            this.title = event.getTitle();
            this.summary = event.getSummary();
        }
    }

    // 행사 상세 조회
    @Data
    public static class DetailDto {
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

        public DetailDto(Event event) {
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
        }
    }

}
