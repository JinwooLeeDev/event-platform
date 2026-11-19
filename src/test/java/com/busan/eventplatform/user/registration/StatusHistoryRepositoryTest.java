package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.api.purchase.*;
import com.busan.eventplatform.api.refund.Refund;
import com.busan.eventplatform.company.*;
import com.busan.eventplatform.company.companyAdminReview.*;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventCategory.EventCategory;
import com.busan.eventplatform.event.eventSession.EventSessionStatus;
import com.busan.eventplatform.event.events.*;
import com.busan.eventplatform.user.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.sql.Timestamp;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatusHistoryRepositoryTest {
    @Test
    void scopesHistoriesToOwnerAndCompanyAndOrdersEqualTimestampsById() {
        var configuration = new Configuration().setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:status-history;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        for (var type : List.of(User.class, Company.class, EventCategory.class, Event.class, EventSession.class,
                Purchase.class, Deposit.class, Refund.class, Registration.class, RegistrationHistory.class, CompanyReviewHistory.class)) {
            configuration.addAnnotatedClass(type);
        }
        try (var factory = configuration.buildSessionFactory(); var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            var owner = user("owner", Role.MEMBER);
            var other = user("other", Role.MEMBER);
            var admin = user("admin", Role.ADMIN);
            for (var user : List.of(owner, other, admin)) em.persist(user);
            var company = company("first", "1234567890");
            var otherCompany = company("second", "1234567891");
            em.persist(company);
            em.persist(otherCompany);
            var category = EventCategory.builder().name("체험").build();
            em.persist(category);
            var event = Event.builder().title("부산 행사").summary("소개").description("내용").address("부산")
                    .company(company).category(category).status(EventStatus.PUBLISHED).build();
            em.persist(event);
            var start = LocalDateTime.of(2026, 12, 1, 10, 0);
            var session = EventSession.builder().event(event).name("오후 회차").capacity(10).startsAt(start)
                    .endsAt(start.plusHours(1)).applyStartAt(start.minusDays(10)).applyEndAt(start.minusDays(1))
                    .pricingType("FREE").minimumParticipants(1).minimumCheckAt(start).status(EventSessionStatus.SCHEDULED).build();
            em.persist(session);
            var own = Registration.create(owner, session, null, RegistrationStatus.CONFIRMED, 1);
            var reapplied = Registration.create(owner, session, null, RegistrationStatus.CONFIRMED, 2);
            var foreign = Registration.create(other, session, null, RegistrationStatus.CONFIRMED, 3);
            for (var registration : List.of(own, reapplied, foreign)) em.persist(registration);
            var first = change(own, RegistrationStatus.WAITING);
            var second = change(own, RegistrationStatus.CONFIRMED);
            for (var change : List.of(first, second, change(reapplied, RegistrationStatus.CONFIRMED), change(foreign, RegistrationStatus.WAITING))) em.persist(change);
            var olderReview = review(company, admin, CompanyStatus.FEEDBACK_NEEDED);
            var latestReview = review(company, admin, CompanyStatus.APPROVED);
            for (var review : List.of(olderReview, latestReview, review(otherCompany, admin, CompanyStatus.APPROVED))) em.persist(review);
            em.flush();
            em.createQuery("update RegistrationHistory set createdAt = :time")
                    .setParameter("time", Instant.parse("2026-11-19T00:00:00Z")).executeUpdate();
            em.createQuery("update CompanyReviewHistory set createdAt = :time")
                    .setParameter("time", Timestamp.valueOf("2026-11-19 09:00:00")).executeUpdate();
            var ownerId = owner.getId();
            var ownId = own.getId();
            var foreignId = foreign.getId();
            var companyId = company.getId();
            var expectedChanges = List.of(first.getId(), second.getId());
            var expectedReviews = List.of(latestReview.getId(), olderReview.getId());
            em.clear();
            var repositories = new JpaRepositoryFactory(em);
            var changes = repositories.getRepository(RegistrationHistoryRepository.class)
                    .findByUserIdAndRegistrationIds(ownerId, List.of(ownId, foreignId));
            assertEquals(expectedChanges, changes.stream().map(RegistrationHistory::getId).toList());
            var reviews = repositories.getRepository(CompanyReviewHistoryRepository.class)
                    .findByCompanyIdOrderByCreatedAtDesc(companyId);
            assertEquals(expectedReviews, reviews.stream().map(CompanyReviewHistory::getId).toList());
            em.clear();
            assertEquals(ownId, changes.getFirst().getRegistration().getId());
            assertEquals("admin", reviews.getFirst().getReviewer().getDisplayName());
            em.getTransaction().rollback();
        }
    }

    private User user(String name, Role role) {
        return User.builder().displayName(name).emailNormalized(name + "@example.com").passwordHash("test").role(role).build();
    }

    private Company company(String name, String number) {
        return Company.builder().name(name).email(name + "@example.com").password("test").businessNumber(number)
                .representativeName("대표").openedOn(LocalDate.of(2020, 1, 1)).address("부산")
                .contactPersonName("담당자").contactPersonPhone("010-1234-5678").build();
    }

    private RegistrationHistory change(Registration registration, RegistrationStatus status) {
        return RegistrationHistory.builder().registration(registration).changedStatus(status).reason("변경 사유").build();
    }

    private CompanyReviewHistory review(Company company, User admin, CompanyStatus status) {
        return CompanyReviewHistory.builder().company(company).reviewer(admin).fromStatus(CompanyStatus.PENDING)
                .toStatus(status).feedback("심사 사유").build();
    }
}
