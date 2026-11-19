package com.busan.eventplatform.login.emailverification;

import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailAvailabilityService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public void validateAvailable(String email) {
        if (email == null || email.isBlank()) {
            throw new Exception400("이메일을 입력해주세요.");
        }

        boolean alreadyUsed = userRepository.existsByEmailNormalized(email) || companyRepository.existsByEmail(email);

        if (alreadyUsed) {
            throw new Exception400("이미 사용중인 이메일입니다.");
        }
    }
}
