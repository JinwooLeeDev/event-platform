package com.busan.eventplatform.event.event_category;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table(name = "event_category_tb")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCategory {

    // 카테고리 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카테고리 이름
    @Column(nullable = false, length = 100)
    private String name;
}
