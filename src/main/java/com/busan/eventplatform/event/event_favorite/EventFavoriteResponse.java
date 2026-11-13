package com.busan.eventplatform.event.event_favorite;

import lombok.Data;

import java.time.LocalDateTime;

public class EventFavoriteResponse {

    @Data
    public static class ListDto {
        private Long id;
        private Long eventId;
        private LocalDateTime createdAt;

        public ListDto(EventFavorite eventFavorite) {
            this.id = eventFavorite.getId();
            if (eventFavorite.getEvent() != null) {
                this.eventId = eventFavorite.getEvent().getId();
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