package com.busan.eventplatform.event.eventReview.eventReviewLike;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EventReviewLikeRepositoryTest {

    @Autowired
    private EventReviewLikeRepository eventReviewLikeRepository;

    private final Long NOT_EXIST_REVIEW_ID = 999999L;
    private final Long NOT_EXIST_USER_ID = 999999L;

    @Test
    void 좋아요_기록_조회() {
        var result = eventReviewLikeRepository
                .findLike(NOT_EXIST_REVIEW_ID, NOT_EXIST_USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void 존재하지_않는_후기의_좋아요_전체삭제() {
        assertDoesNotThrow(() ->
                eventReviewLikeRepository.deleteAllByReviewId(NOT_EXIST_REVIEW_ID)
        );
    }
}