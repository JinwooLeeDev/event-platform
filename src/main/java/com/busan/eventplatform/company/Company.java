package com.busan.eventplatform.company;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.sql.Timestamp;

@Entity     @Table(name = "company_tb")
public class Company {
    @Id     @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)   @ColumnDefault("0")
    private Long point;
}
