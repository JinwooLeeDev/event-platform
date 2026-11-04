package com.busan.eventplatform.event.event_category;

import lombok.Data;

public class EventCategoryRequest {

    @Data
    public static class SaveDto {
        private String name;

        public void validate() {
            if (name == null || name.trim().isEmpty()) {
                throw new Exception("")
            }
        }
    }
}
