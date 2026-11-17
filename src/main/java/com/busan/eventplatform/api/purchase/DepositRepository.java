package com.busan.eventplatform.api.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DepositRepository extends JpaRepository<Deposit , Long> {



    @Query("""
        select d from Deposit d where d.company.id = :companyId and d.eventSession.id = :sessionId
""")
    Optional<Deposit> findByCompanyIdAndSessionId(Long companyId, Long sessionId);
}
