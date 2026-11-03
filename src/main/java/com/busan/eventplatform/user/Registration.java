package com.busan.eventplatform.user;


import com.busan.eventplatform.event.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Entity
@Table(name = "registration_tb")
@Getter
@NoArgsConstructor
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User 엔티티 연관관계(N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Event 엔티티 연관관계(N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.WAITING;;

    @Column(name = "payment_id")
    private String paymentId;

    // 연관된 출석 정보(1:1)
    @OneToOne(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true)
    // todo - RegistrationHistory 구현 후 돌아오기
    private List<RegistrationHistory> histories = new ArrayList<>();



}
