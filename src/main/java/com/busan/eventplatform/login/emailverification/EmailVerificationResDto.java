package com.busan.eventplatform.login.emailverification;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerificationResDto {

    private final String message;
    private final boolean verified;
}