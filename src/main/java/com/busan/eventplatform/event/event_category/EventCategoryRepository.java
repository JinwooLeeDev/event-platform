package com.busan.eventplatform.event.event_category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {

    // 카테고리 중복 체크
    @Query("select c from EventCategory c where c.name = :name")
    Optional<EventCategory> findByName(@Param("name") String name);

}
