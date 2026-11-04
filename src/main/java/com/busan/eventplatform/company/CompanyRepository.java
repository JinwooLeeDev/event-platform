package com.busan.eventplatform.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query("SELECT c FROM Company c WHERE c.email = :email")
    Optional<Company> findByEmail(@Param("email") String email);

    @Query("SELECT c FROM Company c WHERE c.businessNumber = :businessNumber")
    Optional<Company> findByBusinessNumber(@Param("businessNumber") String businessNumber);

//  TODO 서비스단 구현 + 비밀번호 해싱처리 필요.
}
