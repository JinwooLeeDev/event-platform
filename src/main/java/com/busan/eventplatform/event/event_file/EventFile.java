package com.busan.eventplatform.event.event_file;

import com.busan.eventplatform.event.events.Event;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_files",
        uniqueConstraints = {@UniqueConstraint(name = "uk_event_file_event_id",
                columnNames = "event_id")}) // 행사 하나당 대표 이미지 하나만 등록되도록 제한
public class EventFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 파일이 첨부된 행사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // 실제 파일 저장 위치를 식별하는 키
    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    // 대표 이미지 교체
    public void changeStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }
}
