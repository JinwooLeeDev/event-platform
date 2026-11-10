package com.busan.eventplatform.event.events;

import com.busan.eventplatform.user.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    // 행사 상세 조회 - 기업, 카테고리 정보 함께 조회
    @Query("""
            select e from Event e
            join fetch e.company
            join fetch e.category
            where e.id = :id
        """)
    Optional<Event> findByDetail(@Param("id") Long id);

    // 기업이 보완 요청받은 행사 목록 조회
    @Query("""
            select e from Event e
            join fetch e.company
            join fetch e.category
            where e.company.id = :companyId
            and e.status = :status
            order by e.id desc
        """)
    List<Event> findAllByCompanyIdAndStatus(
            @Param("companyId") Long companyId,
            @Param("status") EventStatus status
    );

    // 기업이 등록한 행사 전체 조회
    @Query("""
                select e from Event e
                join fetch e.company
                join fetch e.category
                where e.company.id = :companyId
                order by e.id desc
            """)
    List<Event> findAllByCompanyId(@Param("companyId") Long companyId);

    // 특정 회원이 특정 행사의 참가 확정 상태인지 확인 -- 채팅 버튼 관련
    @Query("""
            select count(r)
            from Registration r
            where r.session.event.id = :eventId
            and r.user.id = :userId
            and r.status = :status
        """)
    long countConfirmedByEventIdAndUserId(
            @Param("eventId") Long eventId,
            @Param("userId") Long userId,
            @Param("status") RegistrationStatus status
    );
}


