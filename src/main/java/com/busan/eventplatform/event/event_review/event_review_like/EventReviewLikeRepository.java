package com.busan.eventplatform.event.event_review.event_review_like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventReviewLikeRepository extends JpaRepository<EventReviewLike, Long> {

    // 해당 회원이 해당 후기에 좋아요를 눌렀는지 확인 -- 중복 방지
    @Query("""
            select erl from EventReviewLike erl
            where erl.eventReview.id = :eventReviewId
            and erl.user.id = :userId
            """)
    Optional<EventReviewLike> findLike(
            @Param("eventReviewId") Long eventReviewId,
            @Param("userId") Long userId
    );

    // 해당 후기의 좋아요 기록 전체 삭제
    @Query("""
            delete from EventReviewLike erl
            where erl.eventReview.id = :reviewId
            """)
    @Modifying
    void deleteAllByReviewId(@Param("reviewId") Long reviewId);
}
