package com.busan.eventplatform.point;

import com.busan.eventplatform.api.purchase.*;
import com.busan.eventplatform.api.refund.*;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.event.events.*;
import com.busan.eventplatform.user.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonalRefundRepositoryTest {
    @Test
    void fetchesOnlyCompletedRefundsForOwnersOwnPurchasesWithoutLazyLoading() {
        var configuration = new Configuration()
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:personal-refunds;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        for (var type : List.of(User.class, Company.class, EventCategory.class, Event.class,
                EventSession.class, Purchase.class, Deposit.class, Refund.class)) {
            configuration.addAnnotatedClass(type);
        }
        try (var factory = configuration.buildSessionFactory(); var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            var owner = user("owner");
            var other = user("other");
            em.persist(owner);
            em.persist(other);
            var company = Company.builder().name("기업").email("company@example.com").password("test")
                    .businessNumber("1234567890").representativeName("대표").openedOn(LocalDate.of(2020, 1, 1))
                    .address("부산").contactPersonName("담당자").contactPersonPhone("010-1234-5678").build();
            em.persist(company);
            var category = EventCategory.builder().name("체험").build();
            em.persist(category);
            var event = Event.builder().title("부산 행사").summary("소개").description("내용").address("부산")
                    .company(company).category(category).status(EventStatus.PUBLISHED).build();
            em.persist(event);
            var start = LocalDateTime.of(2026, 12, 1, 10, 0);
            var session = EventSession.builder().event(event).name("오후 회차").capacity(10).startsAt(start)
                    .endsAt(start.plusHours(1)).applyStartAt(start.minusDays(10)).applyEndAt(start.minusDays(1))
                    .pricingType("PAID").fee(10000).minimumParticipants(1).minimumCheckAt(start)
                    .status(EventSessionStatus.SCHEDULED).build();
            em.persist(session);
            var ownPurchase = Purchase.builder().user(owner).eventSession(session).price(10000).build();
            var otherPurchase = Purchase.builder().user(other).eventSession(session).price(10000).build();
            em.persist(ownPurchase);
            em.persist(otherPurchase);
            var completed = Refund.builder().user(owner).purchase(ownPurchase).amount(5000).build();
            var pending = Refund.builder().user(owner).purchase(ownPurchase).amount(5000).build();
            ReflectionTestUtils.setField(pending, "status", RefundStatus.PENDING);
            em.persist(completed);
            em.persist(pending);
            em.persist(Refund.builder().user(other).purchase(otherPurchase).amount(10000).build());
            em.persist(Refund.builder().user(owner).purchase(otherPurchase).amount(10000).build());
            var deposit = Deposit.builder().company(company).eventSession(session).build();
            em.persist(deposit);
            var companyRefund = Refund.companyBuilder().company(company).deposit(deposit).reason("취소").build();
            companyRefund.setCompanyRefund(150000, "취소");
            em.persist(companyRefund);
            em.flush();
            Long ownerId = owner.getId();
            Long completedId = completed.getId();
            em.clear();
            var repository = new JpaRepositoryFactory(em).getRepository(RefundRepository.class);

            var result = repository.findByUserIdWithPurchase(ownerId, RefundStatus.REFUND);

            assertEquals(1, result.size());
            assertEquals(completedId, result.getFirst().getId());
            em.clear();
            assertEquals("부산 행사", result.getFirst().getPurchase().getEventSession().getEvent().getTitle());
            assertEquals(5000, result.getFirst().getAmount());
            em.getTransaction().rollback();
        }
    }

    private User user(String name) {
        return User.builder().displayName(name).emailNormalized(name + "@example.com")
                .passwordHash("test").role(Role.MEMBER).status(UserStatus.ACTIVE).build();
    }
}
