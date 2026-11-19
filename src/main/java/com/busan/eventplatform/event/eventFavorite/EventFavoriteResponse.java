package com.busan.eventplatform.event.eventFavorite;

import lombok.Data;

import java.time.LocalDateTime;

public class EventFavoriteResponse {

    @Data //
    public static class ListDto {
        private com.busan.eventplatform.event.events.EventResponse.EventDto event;
        private Long id;
        private Long eventId;
        private LocalDateTime createdAt;

        public ListDto(EventFavorite eventFavorite) {
            this.id = eventFavorite.getId();
            if (eventFavorite.getEvent() != null) {
                this.eventId = eventFavorite.getEvent().getId();
                this.event = new com.busan.eventplatform.event.events.EventResponse.EventDto(eventFavorite.getEvent());
            }
            this.createdAt = eventFavorite.getCreatedAt();
        }
    }

    @Data
    public static class DetailDto {
        private boolean isFavorite;
        private Long favoriteCount;

        public DetailDto(boolean isFavorite, Long favoriteCount) {
            this.isFavorite = isFavorite; // 회원 좋아요 여부
            this.favoriteCount = favoriteCount; // 행사 좋아요 개수
        }
    }
}
