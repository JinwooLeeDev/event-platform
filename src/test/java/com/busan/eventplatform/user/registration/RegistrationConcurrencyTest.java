package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.api.payment.PaymentService;
import com.busan.eventplatform.api.purchase.PurchaseService;
import com.busan.eventplatform.api.purchase.PurchaseStatus;
import com.busan.eventplatform.api.refund.RefundService;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventCategory.EventCategory;
import com.busan.eventplatform.event.eventSession.EventSessionStatus;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginType;
import com.busan.eventplatform.user.Role;
import com.busan.eventplatform.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 실제 서비스 + JPA + 별도 H2 DB로 신청·차감·좌석의 일관성을 검사한다.
 * 외부 결제/메일/API, 웹 로그인, 커뮤니티 리스너, 스케줄러는 실행하지 않는다.
 * H2 검증 결과이며 배포 DB의 락 동작은 같은 DB 환경에서 별도 확인해야 한다.
 */
@ActiveProfiles("registration-concurrency")
@DataJpaTest(showSql = false, properties = {
        // application.yml/.env 및 개인 프로필의 DB 설정을 읽지 않는다.
        "spring.config.location=optional:classpath:/registration-concurrency-test.properties",
        "spring.datasource.url=jdbc:h2:mem:registration-concurrency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.test.database.replace=NONE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Import({RegistrationPurchaseService.class, RegistrationService.class,
        PurchaseService.class, RefundService.class, PaymentService.class})
// 테스트 전체를 한 트랜잭션으로 묶으면 작업 스레드가 준비 데이터를 볼 수 없다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RegistrationConcurrencyTest {

    private static final int FEE = 10_000;
    private static final int INITIAL_POINT = 20_000;

    @Autowired
    private RegistrationPurchaseService registrationPurchaseService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("정원 1명에 두 명이 동시에 신청하면 확정 1명·대기 1명이고 각자 한 번만 차감된다")
    void twoUsersCompeteForLastSeat() throws Exception {
        Fixture fixture = createFixture(INITIAL_POINT);

        List<Attempt> attempts = applyTogether(fixture.sessionId(),
                fixture.firstUserId(), fixture.secondUserId());

        assertThat(attempts).allSatisfy(attempt -> assertThat(attempt.failure()).isNull());
        assertThat(attempts).extracting(attempt -> attempt.response().getStatus())
                .containsExactlyInAnyOrder(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITING);

        // 작업 트랜잭션이 커밋된 뒤 새 트랜잭션으로 DB를 다시 읽는다.
        DatabaseState state = readState(fixture);
        assertThat(state.statuses()).containsExactlyInAnyOrder(
                RegistrationStatus.CONFIRMED, RegistrationStatus.WAITING);
        assertThat(state.occupiedCount()).isEqualTo(1);
        assertThat(state.receptionNumbers()).containsExactlyInAnyOrder(1, 2);
        assertThat(state.nextReceptionNumber()).isEqualTo(3);
        assertThat(state.firstPoint()).isEqualTo(INITIAL_POINT - FEE);
        assertThat(state.secondPoint()).isEqualTo(INITIAL_POINT - FEE);
        assertPurchasesAndHistory(state, 2);
        assertThat(state.purchaseUserIds()).containsExactlyInAnyOrder(
                fixture.firstUserId(), fixture.secondUserId());
    }

    @Test
    @DisplayName("동일 계정의 동시 중복 신청은 한 번만 성공하고 신청·구매·차감도 한 번이다")
    void sameUserCannotApplyTwiceConcurrently() throws Exception {
        Fixture fixture = createFixture(INITIAL_POINT);

        List<Attempt> attempts = applyTogether(fixture.sessionId(),
                fixture.firstUserId(), fixture.firstUserId());

        assertThat(attempts.stream().filter(attempt -> attempt.failure() == null).count()).isEqualTo(1);
        List<RuntimeException> failures = attempts.stream()
                .filter(attempt -> attempt.failure() != null)
                .map(Attempt::failure).toList();
        assertThat(failures).hasSize(1);
        // 락 타임아웃·DB 오류를 정상적인 중복 거부로 잘못 통과시키지 않는다.
        assertThat(failures.getFirst()).isInstanceOf(Exception400.class)
                .hasMessageContaining("이미 신청한");

        DatabaseState state = readState(fixture);
        assertThat(state.statuses()).containsExactly(RegistrationStatus.CONFIRMED);
        assertThat(state.occupiedCount()).isEqualTo(1);
        assertThat(state.receptionNumbers()).containsExactly(1);
        assertThat(state.nextReceptionNumber()).isEqualTo(2);
        assertThat(state.firstPoint()).isEqualTo(INITIAL_POINT - FEE);
        assertThat(state.secondPoint()).isEqualTo(INITIAL_POINT);
        assertPurchasesAndHistory(state, 1);
        assertThat(state.purchaseUserIds()).containsExactly(fixture.firstUserId());
    }

    @Test
    @DisplayName("잔액이 부족하면 신청·구매·이력이 남지 않고 잔액과 좌석도 유지된다")
    void insufficientPointLeavesNoPartialData() {
        Fixture fixture = createFixture(5_000);

        Exception400 failure = assertThrows(Exception400.class,
                () -> registrationPurchaseService.applyWithPayment(
                        fixture.sessionId(), identity(fixture.firstUserId())));
        assertThat(failure).hasMessageContaining("잔액이 부족");

        DatabaseState state = readState(fixture);
        assertThat(state.statuses()).isEmpty();
        assertThat(state.occupiedCount()).isZero();
        assertThat(state.nextReceptionNumber()).isEqualTo(1);
        assertThat(state.firstPoint()).isEqualTo(5_000);
        assertThat(state.secondPoint()).isEqualTo(INITIAL_POINT);
        assertPurchasesAndHistory(state, 0);
    }

    private void assertPurchasesAndHistory(DatabaseState state, int expectedCount) {
        assertThat(state.purchaseIds()).hasSize(expectedCount).doesNotHaveDuplicates();
        assertThat(state.linkedPurchaseIds()).hasSize(expectedCount).doesNotContainNull()
                .doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(state.purchaseIds());
        assertThat(state.purchasePrices()).hasSize(expectedCount).allMatch(price -> price == FEE);
        assertThat(state.purchaseStatuses()).hasSize(expectedCount)
                .allMatch(status -> status == PurchaseStatus.PURCHASED);
        assertThat(state.historyCount()).isEqualTo(expectedCount);
    }

    private List<Attempt> applyTogether(Long sessionId, Long firstUserId, Long secondUserId)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Attempt>> futures = new ArrayList<>();
        try {
            for (Long userId : List.of(firstUserId, secondUserId)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("동시 신청 시작 신호를 받지 못했습니다.");
                    }
                    try {
                        // 주입된 Spring 프록시를 호출하므로 요청별 트랜잭션이 생성된다.
                        return new Attempt(registrationPurchaseService.applyWithPayment(
                                sessionId, identity(userId)), null);
                    } catch (RuntimeException failure) {
                        return new Attempt(null, failure);
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).as("두 작업 준비 완료").isTrue();
            start.countDown();
            List<Attempt> results = new ArrayList<>();
            for (Future<Attempt> future : futures) {
                // 작업 스레드의 예외·타임아웃을 숨기지 않고 테스트 실패로 전달한다.
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                    .as("신청 작업 스레드 종료").isTrue();
        }
    }

    private Fixture createFixture(int firstPoint) {
        return new TransactionTemplate(transactionManager).execute(transaction -> {
            String suffix = UUID.randomUUID().toString();
            Long companyCount = entityManager.createQuery("select count(c) from Company c", Long.class)
                    .getSingleResult();
            Company company = Company.builder()
                    .name("동시 신청 테스트 기업").email("company-" + suffix + "@example.test")
                    .password("test-password").businessNumber(String.format("%010d", companyCount + 1))
                    .representativeName("대표자").openedOn(LocalDate.of(2020, 1, 1))
                    .address("부산").contactPersonName("담당자").contactPersonPhone("010-0000-0000")
                    .status(CompanyStatus.APPROVED).build();
            entityManager.persist(company);

            EventCategory category = EventCategory.builder().name("동시 신청 테스트").build();
            entityManager.persist(category);
            Event event = Event.builder().company(company).category(category)
                    .title("동시 신청 테스트 행사").summary("정원·중복·차감 검증")
                    .description("테스트 전용 데이터").address("부산")
                    .status(EventStatus.PUBLISHED).build();
            entityManager.persist(event);

            LocalDateTime now = LocalDateTime.now();
            EventSession session = EventSession.builder().event(event).name("정원 1명 회차")
                    .capacity(1).occupiedCount(0).nextReceptionNumber(1)
                    .startsAt(now.plusDays(7)).endsAt(now.plusDays(7).plusHours(2))
                    .applyStartAt(now.minusDays(1)).applyEndAt(now.plusDays(6))
                    .pricingType("PAID").fee(FEE).holdDurationSeconds(0)
                    .reservationDepositPoints(0).minimumParticipants(1)
                    .minimumCheckAt(now.plusDays(6)).status(EventSessionStatus.SCHEDULED).build();
            entityManager.persist(session);

            User first = createUser("first-" + suffix, firstPoint);
            User second = createUser("second-" + suffix, INITIAL_POINT);
            entityManager.persist(first);
            entityManager.persist(second);
            entityManager.flush();
            // execute() 반환 시 커밋된 후에만 작업 스레드를 시작한다.
            return new Fixture(session.getId(), first.getId(), second.getId());
        });
    }

    private User createUser(String name, int point) {
        return User.builder().emailNormalized(name + "@example.test")
                .passwordHash("test-password").displayName(name)
                .role(Role.MEMBER).point(point).build();
    }

    private LoginIdentity identity(Long userId) {
        return LoginIdentity.builder().id(userId).loginType(LoginType.USER).build();
    }

    private DatabaseState readState(Fixture fixture) {
        return new TransactionTemplate(transactionManager).execute(transaction -> {
            EventSession session = entityManager.find(EventSession.class, fixture.sessionId());
            List<RegistrationStatus> statuses = entityManager.createQuery(
                            "select r.status from Registration r where r.eventSession.id = :id", RegistrationStatus.class)
                    .setParameter("id", fixture.sessionId()).getResultList();
            List<Integer> receptionNumbers = entityManager.createQuery(
                            "select r.receptionNumber from Registration r where r.eventSession.id = :id", Integer.class)
                    .setParameter("id", fixture.sessionId()).getResultList();
            List<Long> linkedPurchaseIds = entityManager.createQuery(
                            "select p.id from Registration r left join r.purchase p where r.eventSession.id = :id", Long.class)
                    .setParameter("id", fixture.sessionId()).getResultList();
            List<Long> purchaseIds = entityManager.createQuery(
                            "select p.id from Purchase p where p.eventSession.id = :id", Long.class)
                    .setParameter("id", fixture.sessionId()).getResultList();
            List<Long> purchaseUserIds = entityManager.createQuery(
                            "select p.user.id from Purchase p where p.eventSession.id = :id", Long.class)
                    .setParameter("id", fixture.sessionId()).getResultList();
            List<Integer> prices = entityManager.createQuery(
                            "select p.price from Purchase p where p.eventSession.id = :id", Integer.class)
                    .setParameter("id", fixture.sessionId()).getResultList();
            List<PurchaseStatus> purchaseStatuses = entityManager.createQuery(
                            "select p.status from Purchase p where p.eventSession.id = :id", PurchaseStatus.class)
                    .setParameter("id", fixture.sessionId()).getResultList();
            Long historyCount = entityManager.createQuery(
                            "select count(h) from RegistrationHistory h where h.registration.eventSession.id = :id", Long.class)
                    .setParameter("id", fixture.sessionId()).getSingleResult();
            return new DatabaseState(session.getOccupiedCount(), session.getNextReceptionNumber(),
                    entityManager.find(User.class, fixture.firstUserId()).getPoint(),
                    entityManager.find(User.class, fixture.secondUserId()).getPoint(),
                    statuses, receptionNumbers, purchaseIds, linkedPurchaseIds, purchaseUserIds,
                    prices, purchaseStatuses, historyCount);
        });
    }

    private record Fixture(Long sessionId, Long firstUserId, Long secondUserId) { }

    private record Attempt(RegistrationRes.ApplyDto response, RuntimeException failure) { }

    private record DatabaseState(int occupiedCount, int nextReceptionNumber,
                                 int firstPoint, int secondPoint,
                                 List<RegistrationStatus> statuses, List<Integer> receptionNumbers,
                                 List<Long> purchaseIds, List<Long> linkedPurchaseIds,
                                 List<Long> purchaseUserIds, List<Integer> purchasePrices,
                                 List<PurchaseStatus> purchaseStatuses, long historyCount) { }
}
