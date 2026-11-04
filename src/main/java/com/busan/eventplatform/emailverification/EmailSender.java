
package com.busan.eventplatform.emailverification;

public interface EmailSender {
    void sendVerificationCode(String email, String code);
}