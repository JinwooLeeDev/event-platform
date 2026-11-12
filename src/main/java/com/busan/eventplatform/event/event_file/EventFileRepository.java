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
    @Query("""
            select f from EventFile f
            where f.event.id = :eventId
            """)
    List<EventFile> findAllByEventId(@Param("eventId") Long eventId);

    // 행사 ID와 저장 키로 연결 정보 조회
    @Query("""
            select f from EventFile f
            where f.event.id = :eventId
            and f.storageKey = :storageKey
            """)
    Optional<EventFile> findByEventIdAndStorageKey(
            @Param("eventId") Long eventId,
            @Param("storageKey") String storageKey
    );

    // 파일 교체 또는 연결 정보 삭제
    @Modifying
    @Transactional
    @Query("""
        delete from EventFile f
        where f.event.id = :eventId
        and f.storageKey = :storageKey
        """)
    void deleteByEventIdAndStorageKey(
            @Param("eventId") Long eventId,
            @Param("storageKey") String storageKey
    );

    // 행사 ID로 대표 이미지 조회
    @Query("""
            select f from EventFile f
            where f.event.id = :eventId
            """)
    Optional<EventFile> findByEventId(@Param("eventId") Long eventId);

}