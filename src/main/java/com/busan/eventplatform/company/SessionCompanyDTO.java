package com.busan.eventplatform.company;

import lombok.Builder;
import lombok.Getter;

//임시 세션 클래스
@Getter
public class SessionCompanyDTO {
    private Long id;
    private String name;
    private CompanyStatus status;

    public SessionCompanyDTO(Company company) {
        this.id = company.getId();
        this.name = company.getName();
        this.status = company.getStatus();
    }
}
