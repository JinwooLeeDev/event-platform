package com.busan.eventplatform.event.event_category;

import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Data;

public class EventCategoryRequest {

    @Data
    public static class SaveDto {
        private String name;

        public void validate() {
            if (name == null || name.trim().isEmpty()) {
                throw new Exception400("이름은 필수 입니다.");
            }
        }
        public EventCategory toEntity() {
            return EventCategory.builder()
                    .name(this.name)
                    .build();
        }
    }

    @Data
    public static class UpdateDto {
        private String name;

        public void validate() {
            if (name == null || name.trim().isEmpty()) {
                throw new Exception400("이름은 필수 입니다.");
            }
        }
    }
}
