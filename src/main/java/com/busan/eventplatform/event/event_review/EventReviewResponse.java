package com.busan.eventplatform.event.event_review;

import lombok.Data;

import java.time.LocalDateTime;

public class EventReviewResponse {

    @Data
    public static class ListDto {

        private Long id;
        private String displayName;
        private String content;
        private int likeCount;
        private LocalDateTime createdAt;

        public ListDto(EventReview eventReview) {
            this.id = eventReview.getId();
            if (eventReview.getUser() != null) {
                this.displayName = eventReview.getUser().getDisplayName();
            }
            this.content = eventReview.getContent();
            this.likeCount = eventReview.getLikeCount();
            this.createdAt = eventReview.getCreatedAt();
        }
    }
}
