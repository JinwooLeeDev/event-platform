package com.busan.eventplatform.login.companylogin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionCompanyDto {
    private final Long id;
    private final String displayName;
}