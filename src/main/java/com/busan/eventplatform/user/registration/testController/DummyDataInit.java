package com.busan.eventplatform.user.registration.testController;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository; // 패키지 경로 확인 필요
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.event.event_category.EventCategoryRepository; // 패키지 경로 확인 필요
import com.busan.eventplatform.event.events.EventStatus;
import com.busan.eventplatform.user.Role;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DummyDataInit implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventSessionRepository eventSessionRepository;
    private final CompanyRepository companyRepository;           // 👈 회사 repository
    private final EventCategoryRepository categoryRepository;    // 👈 카테고리 repository

    @Override
    public void run(String... args) throws Exception {
        // 1. 테스트용 유저(ID: 1) 생성
        if (!userRepository.existsById(1L)) {
            User dummyUser = User.builder()
                    .emailNormalized("test@naver.com")
                    .passwordHash("dummy_password_hash")
                    .displayName("테스트(박지훈)")
                    .role(Role.MEMBER)
                    .point(70000)
                    .preferredLocale("ko")
                    .build();
            userRepository.save(dummyUser);
        }

        // 2. 테스트용 주최 기업(Company) 생성 (없을 경우)
        Company dummyCompany = companyRepository.findAll().stream().findFirst().orElseGet(() -> {
            Company company = Company.builder()
                    // Company 엔티티의 필수 필드에 맞게 작성해주세요 (예시 닉네임/이름 등)
                    .build();
            return companyRepository.save(company);
        });

        // 3. 테스트용 행사 카테고리(EventCategory) 생성 (없을 경우)
        EventCategory dummyCategory = categoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            EventCategory category = EventCategory.builder()
                    // EventCategory 엔티티의 필수 필드에 맞게 작성해주세요
                    .name("IT / 컨퍼런스")
                    .build();
            return categoryRepository.save(category);
        });

        // 4. 테스트용 행사(Event) 생성 (모든 필수값 포함)
        Event dummyEvent = eventRepository.findById(1L).orElseGet(() -> {
            Event event = Event.builder()
                    .company(dummyCompany)
                    .category(dummyCategory)
                    .title("2026 부산 벡스코 IT 컨퍼런스")
                    .summary("스프링 부트와 프론트엔드 연동 세션")
                    .description("상세 설명: 실전 프로젝트 구축을 위한 기술 공유 세션입니다.")
                    .address("부산 해운대구 센텀중앙로 97, 벡스코 제1전시장")
                    .status(EventStatus.PUBLISHED) // 또는 상수에 맞는 상태값
                    .applyStartAt(LocalDateTime.now().minusDays(1))
                    .applyEndAt(LocalDateTime.now().plusDays(7))
                    .build();
            return eventRepository.save(event);
        });

        // 5. 테스트용 행사 세션(ID: 1) 생성
        if (!eventSessionRepository.existsById(1L)) {
            EventSession dummySession = EventSession.builder()
                    .event(dummyEvent)
                    .name("1회차 일반 세션") // 👈 세션 이름 추가
                    .capacity(50)
                    .fee(10000)
                    .build();
            eventSessionRepository.save(dummySession);
        }
    }
}