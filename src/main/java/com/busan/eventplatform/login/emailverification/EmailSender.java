
package com.busan.eventplatform.login.emailverification;

public interface EmailSender {
    void sendVerificationCode(String email, String code);
}