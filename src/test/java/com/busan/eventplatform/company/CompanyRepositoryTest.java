package com.busan.eventplatform.company;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EntityManager entityManager;

    private Long companyId;

    @BeforeEach
    void setUp() {
        Company company = Company.builder()
                .name("테스트기업")
                .email("company@example.test")
                .businessNumber("0000000001")
                .representativeName("테스트대표")
                .openedOn(LocalDate.of(2020, 1, 1))
                .address("부산광역시")
                .contactPersonName("테스트담당자")
                .contactPersonPhone("010-0000-0001")
                .status(CompanyStatus.PENDING)
                .build();

        companyId = companyRepository.saveAndFlush(company).getId();

        // 저장된 객체 대신 실제 DB 조회 결과를 확인
        entityManager.clear();
    }

    @Test
    void 이메일이_일치하는_기업을_조회한다() {
        Optional<Company> result =
                companyRepository.findByEmail("company@example.test");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(companyId);
        assertThat(result.orElseThrow().getEmail())
                .isEqualTo("company@example.test");
    }

    @Test
    void 존재하지_않는_이메일이면_빈_결과를_반환한다() {
        Optional<Company> result =
                companyRepository.findByEmail("missing@example.test");

        assertThat(result).isEmpty();
    }

    @Test
    void 사업자번호가_일치하는_기업을_조회한다() {
        Optional<Company> result =
                companyRepository.findByBusinessNumber("0000000001");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(companyId);
        assertThat(result.orElseThrow().getBusinessNumber())
                .isEqualTo("0000000001");
    }

    @Test
    void 존재하지_않는_사업자번호면_빈_결과를_반환한다() {
        Optional<Company> result =
                companyRepository.findByBusinessNumber("9999999999");

        assertThat(result).isEmpty();
    }
}