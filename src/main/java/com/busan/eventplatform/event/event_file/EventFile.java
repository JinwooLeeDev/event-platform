package com.busan.eventplatform.event.event_file;

import com.busan.eventplatform.event.events.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_files")
// eventId, file_id, purpose, display_order
public class EventFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 파일이 첨부된 행사 -- 행사와 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // 실제 파일 정보
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "file_id", nullable = false)
//    private File file;

    // 행사 화면에 파일을 보여줄 순서
    @Column(nullable = false)
    private Integer displayOrder;
}
