package com.busan.eventplatform.event.event_review;

import lombok.Data;

public class EventReviewRequest {

    @Data
    public static class SaveDto {
        private String content;
    }

    @Data
    public static class UpdateDto {
        private String content;
    }
}
