package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.event.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface PurchaseRepository extends JpaRepository<Purchase , Long>{
    // TODO 추후 EventSession이 어느정도 구현되면 삭제.
    @Query("select es from EventSession es join fetch es.event where es.id = :eventId")
    Optional<EventSession> findByIdWithEvent(@Param("eventId") Long eventId);

    @Query("""
            select case when count(p.user) > 0 then true else false end
            from Purchase p 
            where p.user.id = :userId and p.eventSession.id = :eventId 
            """
            )
    boolean existsEventByUser (@Param("userId") Long userId , @Param("eventId") Long eventId);


    @Query("""
            select case when count(p.company) > 0 then true else false end
            from Purchase p 
            where p.company.id = :companyId and p.eventSession.id = :eventId
            """
    )
    boolean existsEventByCompany (@Param("companyId") Long companyId , @Param("eventId") Long eventId);
}
