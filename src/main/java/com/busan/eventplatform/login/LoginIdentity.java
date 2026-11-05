
package com.busan.eventplatform.login;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginIdentity {

    private final Long id;
    private final LoginType loginType; // (USER, COMPANY) ENUM 타입
}