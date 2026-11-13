package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.event.event_session.EventSessionRequest;
import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class EventRequest {

    // 행사 등록
    @Data
    public static class SaveDto {
        private String title;
        private String summary; // 목록에 표시할 짧은 소개
        private String description; // 상세 페이지에 표시할 설명
        private String address; // 행사 주소
        private Long categoryId; // 선택한 카테고리 ID
        private boolean hasSessions; // 회차 사용 여부
        private String pricingType;
        private int fee;

        // 회차가 없는 경우 사용할 날짜와 모집 기간
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime applyStartAt;
        private LocalDateTime applyEndAt;

        // 회차가 있는 경우 입력받을 회차 목록
        private List<EventSessionRequest.SaveDto> sessions;


        public void validate() {
            if (title == null || title.trim().isEmpty()) {
                throw new Exception400("행사 제목은 필수 입니다.");
            }
            if (summary == null || summary.trim().isEmpty()) {
                throw new Exception400("행사 소개는 필수 입니다.");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new Exception400("행사 설명은 필수 입니다.");
            }
            if (address == null || address.trim().isEmpty()) {
                throw new Exception400("행사 주소는 필수 입니다.");
            }
            if (categoryId == null) {
                throw new Exception400("행사 카테고리는 필수 입니다.");
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

            // 회차가 있는 행사
            if (hasSessions) {

                if (sessions == null || sessions.isEmpty()) {
                    throw new Exception400("회차 정보를 하나 이상 등록해주세요.");
                }

                sessions.forEach(EventSessionRequest.SaveDto::validate);

            } else {
                // 회차 없는 행사
                if (startDate == null || endDate == null) {
                    throw new Exception400("행사 시작일과 마감일을 입력해주세요.");
                }

                if (startDate.isAfter(endDate)) {
                    throw new Exception400("행사 마감일은 시작일보다 빠를 수 없습니다.");
                }

                if (applyStartAt == null || applyEndAt == null) {
                    throw new Exception400("모집 시작일과 마감일을 입력해주세요.");
                }

                if (applyStartAt.isAfter(applyEndAt)) {
                    throw new Exception400("모집 마감일은 시작일보다 빠를 수 없습니다.");
                }
                if ("FREE".equals(pricingType)) {
                    fee = 0;
                } else if ("PAID".equals(pricingType)) {
                    if (fee <= 0) {
                        throw new Exception400("유료 행사는 참가비를 입력해주세요.");
                    }
                } else {
                    throw new Exception400("올바른 참가비 유형이 아닙니다.");
                }
            }
        }

        public Event toEntity() {
            return Event.builder()
                    .title(this.title)
                    .summary(this.summary)
                    .description(this.description)
                    .address(this.address)
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

        }

        // 기존 Event 객체의 값 수정
        public Event toEntity(Event event, EventCategory category) {
            event.setTitle(this.title);
            event.setSummary(this.summary);
            event.setDescription(this.description);
            event.setAddress(this.address);
            event.setCategory(category);

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
