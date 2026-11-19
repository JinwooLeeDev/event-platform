package com.busan.eventplatform.event.eventReview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EventReviewRepositoryTest {

    @Autowired
    private EventReviewRepository eventReviewRepository;

    private final Long NOT_EXIST_SESSION_ID = 999999L;
    private final Long NOT_EXIST_USER_ID = 999999L;
    private final Long NOT_EXIST_REVIEW_ID = 999999L;

    @Test
    void 특정_회차_후기목록_조회() {
        var result = eventReviewRepository
                .findAllByEventSessionId(NOT_EXIST_SESSION_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void 특정_회원_후기작성_여부_확인() {
        boolean result = eventReviewRepository
                .existsReview(NOT_EXIST_SESSION_ID, NOT_EXIST_USER_ID);

        assertFalse(result);
    }

    @Test
    void 존재하지_않는_후기_작성자와_함께_조회() {
        Optional<EventReview> result =
                eventReviewRepository.findByIdWithUser(NOT_EXIST_REVIEW_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void 특정_회차_좋아요순_후기목록_조회() {
        var result = eventReviewRepository
                .findAllByEventSessionIdOrderByLikeCount(NOT_EXIST_SESSION_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}