package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.event.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface PurchaseRepository extends JpaRepository<Purchase , Long>{
    // TODO 이벤트 ID로 이벤트 존재하는지? 추후 EventSession이 어느정도 구현되면 삭제.
    //  --> 추후 event의 이미지 , title 화면에 뿌릴예정이므로 join fetch.
    @Query("select es from EventSession es join fetch es.event where es.id = :eventId")
    Optional<EventSession> findByIdWithEvent(@Param("eventId") Long eventId);

    // 해당 이벤트를 이미 구매 결제한 유저가 있는지?
    @Query("""
            select case when count(p.user) > 0 then true else false end
            from Purchase p 
            where p.user.id = :userId and p.eventSession.id = :eventId 
            """
            )
    boolean existsEventByUser (@Param("userId") Long userId , @Param("eventId") Long eventId);


    // 해당 이벤트를 이미 구매 결제한 기업이 있는지?
    @Query("""
            select case when count(p.company) > 0 then true else false end
            from Purchase p 
            where p.company.id = :companyId and p.eventSession.id = :eventId
            """
    )
    boolean existsEventByCompany (@Param("companyId") Long companyId , @Param("eventId") Long eventId);

    @Query("select ")
    List<PurchaseRes.PurchaseListDto> findByUserIdWithPurchaseList(Long userId);
}
