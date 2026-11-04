package com.busan.eventplatform.user;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.sql.Timestamp;

@Entity
@Table(name = "user_tb")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)   @ColumnDefault( "0")
    private Long point;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role;    // 플랫폼측 관리자와 일반회원 2개로 나눔

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    private Timestamp createdAt;
}