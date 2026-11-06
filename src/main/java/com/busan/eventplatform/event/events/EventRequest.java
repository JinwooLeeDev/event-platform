package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EventRequest {

    // 행사 등록
    @Data
    public static class SaveDto {
        private String title;
        private String summary; // 목록에 표시할 짧은 소개
        private String description; // 상세 페이지에 표시할 설명
        private String address; // 행사 주소
        private Long categoryId; // 선택한 카테고리 ID
        private LocalDateTime applyStartAt;
        private LocalDateTime applyEndAt;

        public void validate() {
            if (title == null || title.trim().isEmpty()) {
                throw new Exception400("제목은 필수 입니다.");
            }
            if (summary == null || summary.trim().isEmpty()) {
                throw new Exception400("소개는 필수 입니다.");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new Exception400("설명은 필수 입니다.");
            }
            if (address == null || address.trim().isEmpty()) {
                throw new Exception400("주소는 필수 입니다.");
            }
            if (categoryId == null) {
                throw new Exception400("카테고리는 필수 입니다.");
            }
            if (applyStartAt == null) {
                throw new Exception400("모집 시작일은 필수 입니다.");
            }
            if (applyEndAt == null) {
                throw new Exception400("모집 마감일은 필수 입니다.");
            }
            if (applyStartAt.isAfter(applyEndAt)) {
                throw new Exception400("모집 시작일은 마감일보다 늦을 수 없습니다.");
            }
        }

        public Event toEntity() {
            return Event.builder()
                    .title(this.title)
                    .summary(this.summary)
                    .description(this.description)
                    .address(this.address)
                    .applyStartAt(this.applyStartAt)
                    .applyEndAt(this.applyEndAt)
                    .build();
        }
    }

    // 행사 수정
    @Data
    public static class UpdateDto {
        private String title;
        private String summary;
        private String description;
        private String address;
        private Long categoryId;
        private LocalDateTime applyStartAt;
        private LocalDateTime applyEndAt;

        public void validate() {
            if (title == null || title.trim().isEmpty()) {
                throw new Exception400("제목은 필수 입니다.");
            }
            if (summary == null || summary.trim().isEmpty()) {
                throw new Exception400("소개는 필수 입니다.");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new Exception400("설명은 필수 입니다.");
            }
            if (address == null || address.trim().isEmpty()) {
                throw new Exception400("주소는 필수 입니다.");
            }
            if (categoryId == null) {
                throw new Exception400("카테고리는 필수 입니다.");
            }
            if (applyStartAt == null) {
                throw new Exception400("모집 시작일은 필수 입니다.");
            }
            if (applyEndAt == null) {
                throw new Exception400("모집 마감일은 필수 입니다.");
            }
            if (applyStartAt.isAfter(applyEndAt)) {
                throw new Exception400("모집 시작일은 마감일보다 늦을 수 없습니다.");
            }
        }

        // 기존 Event 객체의 값 수정
        public Event toEntity(Event event, EventCategory category) {
            event.setTitle(this.title);
            event.setSummary(this.summary);
            event.setDescription(this.description);
            event.setAddress(this.address);
            event.setCategory(category);
            event.setApplyStartAt(this.applyStartAt);
            event.setApplyEndAt(this.applyEndAt);

            return event;
        }
    }

    @Data
    public static class CancelDto {
        private String cancellationReason;

        public void validate() {
            if (cancellationReason == null || cancellationReason.trim().isEmpty()) {
                throw new Exception400("취소 사유를 입력해주세요.");
            }
        }
    }
}
