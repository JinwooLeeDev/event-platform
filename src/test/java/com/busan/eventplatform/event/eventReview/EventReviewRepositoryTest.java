package com.busan.eventplatform.event.eventReview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventReviewRepositoryTest {

    @Autowired
    private EventReviewRepository eventReviewRepository;

    // Repository 주입 확인
    @Test
    void repository_load_success() {
        assertNotNull(eventReviewRepository);
    }

    // 존재하지 않는 후기 조회
    @Test
    void findById_not_found() {
        Optional<EventReview> result =
                eventReviewRepository.findById(999999L);

        assertTrue(result.isEmpty());
    }

    // 특정 회차 후기 목록 조회
    @Test
    void findAllByEventSessionId_success() {
        var result =
                eventReviewRepository.findAllByEventSessionId(999999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // 특정 회원의 후기 작성 여부 확인
    @Test
    void existsReview_false_when_not_exists() {
        boolean result =
                eventReviewRepository.existsReview(999999L, 999999L);

        assertFalse(result);
    }

    // 특정 회차 후기 좋아요순 조회
    @Test
    void findAllByEventSessionIdOrderByLikeCount_success() {
        var result =
                eventReviewRepository
                        .findAllByEventSessionIdOrderByLikeCount(999999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // 특정 후기와 작성자 함께 조회
    @Test
    void findByIdWithUser_not_found() {
        Optional<EventReview> result =
                eventReviewRepository.findByIdWithUser(999999L);

        assertTrue(result.isEmpty());
    }

    // 출석하지 않은 회원 확인
    @Test
    void existsAttendance_false_when_not_attended() {
        boolean result =
                eventReviewRepository.existsAttendance(999999L, 999999L);

        assertFalse(result);
    }
}