package com.busan.eventplatform.event.event_file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface EventFileRepository extends JpaRepository<EventFile, Long> {

    // 행사 ID로 첨부파일 목록 조회
    @Query("select f from EventFile f where f.event.id = :eventId")
    List<EventFile> findAllByEventId(@Param("eventId") Long eventId);

    // 행사 ID와 파일 ID로 연결 정보 조회
    @Query("""
            select f from EventFile f
            where f.event.id = :eventId
            and f.file.id = :fileId
            """)
    Optional<EventFile> findByEventIdAndFileId(
            @Param("eventId") Long eventId,
            @Param("fileId") Long fileId
    );

    // 행사별 표시 순서 오름차순 조회
    @Query("""
            select f from EventFile f
            where f.event.id = :eventId 
            order by f.displayOrder asc
            """)
    List<EventFile> findAllByEventIdOrderByDisplayOrder(
            @Param("eventId") Long eventId
    );

    // 파일 교체
    @Modifying
    @Transactional
    @Query("""
            delete from Eventfile f
            where f.event.id = :eventId
            and f.file.id = :fileId
            """)
    void deleteByEventIdAndFileId(
            @Param("eventId") Long eventId,
            @Param("eventId") Long fileId
    );

}
