package com.busan.eventplatform.user.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RegistrationHistoryRepository extends JpaRepository<RegistrationHistory, Long> {

    @Query("""
            select h from RegistrationHistory h
            join fetch h.registration r
            where r.user.id = :userId and r.id in :registrationIds
            order by h.createdAt asc, h.id asc
            """)
    List<RegistrationHistory> findByUserIdAndRegistrationIds(@Param("userId") Long userId,
                                                            @Param("registrationIds") List<Long> registrationIds);
}
