package com.busan.eventplatform.event.event_file;

import com.busan.eventplatform.event.events.Event;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_files")
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

    // 행사 화면에 파일을 보여줄 순서
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
