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
        private LocalDate eventDate;
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
            if (eventDate == null) {
                throw new Exception400("행사 날짜는 필수 입니다.");
            }
            if (applyStartAt == null) {
                throw new Exception400("모집 시작일은 필수 입니다.");
            }
            if (applyEndAt == null) {
                throw new Exception400("모집 마감일은 필수 입니다.");
            }
        }
        public Event toEntity() {
            return Event.builder()
                    .title(this.title)
                    .summary(this.summary)
                    .description(this.description)
                    .address(this.address)
                    .eventDate(this.eventDate)
                    .applyStartAt(this.applyStartAt)
                    .applyEndAt(this.applyEndAt)
                    .build();
        }
    }

    // 행사 수정
}
