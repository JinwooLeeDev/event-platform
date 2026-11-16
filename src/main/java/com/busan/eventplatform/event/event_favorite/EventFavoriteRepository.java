package com.busan.eventplatform.event.event_favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventFavoriteRepository extends JpaRepository<EventFavorite, Long> {

    // 회원의 행사 좋아요 여부
    @Query("""
                select count(f) > 0 from EventFavorite f
                where f.user.id = :userId
                and f.event.id = :eventId
            """)
    boolean existsByUserIdAndEventId(@Param("userId") Long userId, @Param("eventId") Long eventId);

    // 좋아요 데이터 조회
    @Query("""
                select f from EventFavorite f
                where f.user.id = :userId
                and f.event.id = :eventId
            """)
    Optional<EventFavorite> findByUserIdAndEventId(@Param("userId") Long userId, @Param("eventId") Long eventId);

    // 행사별 좋아요 개수
    @Query("""
            select count(f) from EventFavorite f
            where f.event.id = :eventId
            """)
    long countByEventId(@Param("eventId") Long eventId);

    // 회원별 좋아요 목록
    @Query("""
                select f from EventFavorite f
                where f.user.id = :userId
                order by f.createdAt desc
            """)
    List<EventFavorite> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

}
