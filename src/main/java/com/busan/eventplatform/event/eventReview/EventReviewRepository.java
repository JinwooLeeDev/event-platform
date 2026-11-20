package com.busan.eventplatform.event.eventReview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventReviewRepository extends JpaRepository<EventReview, Long> {

    // 특정 회차의 후기 목록 조회
    @Query("""
            select er from EventReview er
            where er.eventSession.id = :eventSessionId
            order by er.createdAt desc
            """)
    List<EventReview> findAllByEventSessionId(@Param("eventSessionId") Long eventSessionId);

    // 특정 회원의 해당 회차 후기 작성 여부 확인 -- 중복 후기 작성 막기
    @Query("""
            select count(er) > 0 from EventReview er
            where er.eventSession.id = :eventSessionId
            and er.user.id = :userId
            """)
    boolean existsReview(
            @Param("eventSessionId") Long eventSessionId,
            @Param("userId") Long userId
    );

    // 후기, 작성자 조회 -- 후기 수정 시 작성자 확인 위함
    @Query("""
            select er from EventReview er
            join fetch er.user
            where er.id = :id
            """)
    Optional<EventReview> findByIdWithUser(@Param("id") Long id);

    // 특정 회차의 후기 좋아요순 조회
    @Query("""
            select er from EventReview er
            where er.eventSession.id = :eventSessionId
            order by er.likeCount desc, er.createdAt desc
            """)
    List<EventReview> findAllByEventSessionIdOrderByLikeCount(@Param("eventSessionId") Long eventSessionId);

    // 특정 회원의 해당 회차 출석 여부 확인 -- 출석한 회원만 후기 작성 허용
    @Query("""
        select count(a) > 0
        from Attendance a
        where a.registration.eventSession.id = :eventSessionId
        and a.registration.user.id = :userId
        """)
    boolean existsAttendance(
            @Param("eventSessionId") Long eventSessionId,
            @Param("userId") Long userId
    );

}
