package com.busan.eventplatform.event.event_category;

import lombok.Data;

import java.util.List;

public class EventCategoryResponse {

    @Data
    public static class ListDto {
        private List<CategoryDto> categoryList;

        public  ListDto(List<EventCategory> categoryList) {
            this.categoryList = categoryList.stream()
                    .map(CategoryDto::new)
                    .toList();
        }
    }

    @Data
    public static class CategoryDto {
        private Long id;
        private String name;

        public CategoryDto(EventCategory eventCategory) {
            this.id = eventCategory.getId();
            this.name = eventCategory.getName();
        }
    }
}
