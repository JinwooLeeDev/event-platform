package com.busan.eventplatform.event.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // 공개된 행사 조회 (eventStatus가 PUBLISHED일 때)
    @Query(value = """
                select e from Event e left join fetch e.category
                where e.status = :status""",
            countQuery = "select count(e) from Event e where e.status = :status")
    Page<Event> findAllPublished(
            @Param("status") EventStatus status,
            Pageable pageable
    );

    // 공개된 행사 검색 조회 (PUBLISHED)
    @Query(value = """
            select e from Event e left join fetch e.category where e.status = :status
            and (lower(e.title) like lower(concat('%', :keyword, '%'))
            or lower(e.summary) like lower(concat('%', :keyword, '%')))""",
            countQuery = """
                    select count(e) from Event e where e.status = :status
                    and (lower(e.title) like lower(concat('%', :keyword, '%'))
                    or lower(e.summary) like lower(concat('%', :keyword, '%')))
                    """)
    Page<Event> findAllKeyWord(
            @Param("status") EventStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
