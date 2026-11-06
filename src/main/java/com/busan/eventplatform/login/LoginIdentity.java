
package com.busan.eventplatform.login;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginIdentity {

    private final Long id;
    private final LoginType loginType; // (USER, COMPANY) ENUM 타입

    // loginType으로 User || Company 구분하려는 편의 메서드 생성.
    public boolean isUser() {return this.loginType == LoginType.USER;}
    public boolean isCompany() {return this.loginType == LoginType.COMPANY;}
}