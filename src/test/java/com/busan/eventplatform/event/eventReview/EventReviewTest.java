
package com.busan.eventplatform.event.eventReview;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventReviewTest {

    // 후기 내용 수정
    @Test
    void updateContent_success() {
        EventReview review = EventReview.builder()
                .content("기존 후기")
                .likeCount(0)
                .build();

        review.updateContent("수정한 후기");

        assertEquals("수정한 후기", review.getContent());
    }

    // 좋아요 수 증가
    @Test
    void increaseLikeCount_success() {
        EventReview review = EventReview.builder()
                .likeCount(0)
                .build();

        review.increaseLikeCount();

        assertEquals(1, review.getLikeCount());
    }

    // 좋아요 수 감소
    @Test
    void decreaseLikeCount_success() {
        EventReview review = EventReview.builder()
                .likeCount(2)
                .build();

        review.decreaseLikeCount();

        assertEquals(1, review.getLikeCount());
    }

    // 좋아요 수가 0일 때 음수가 되지 않음
    @Test
    void decreaseLikeCount_not_below_zero() {
        EventReview review = EventReview.builder()
                .likeCount(0)
                .build();

        review.decreaseLikeCount();

        assertEquals(0, review.getLikeCount());
    }

    // 생성 시간 자동 등록
    @Test
    void onCreate_success() {
        EventReview review = EventReview.builder()
                .content("후기 내용")
                .likeCount(0)
                .build();

        assertNull(review.getCreatedAt());

        review.onCreate();

        assertNotNull(review.getCreatedAt());
        assertTrue(
                review.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1))
        );
    }
}
