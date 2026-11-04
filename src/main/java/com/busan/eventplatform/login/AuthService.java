package com.busan.eventplatform.login;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.emailverification.EmailVerificationService;
import com.busan.eventplatform.user.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public void signup(SignupReqDto request, HttpSession session) {
        request.validate();
        Instant verifiedAt = emailVerificationService.requireVerifiedEmail(request.getEmail(), session);
        if (userRepository.existsByEmailNormalized(request.getEmail())) {
            throw new Exception400("이미 가입된 이메일입니다.");
        }
        // Todo - 유저 클래스 완성되면 완성본으로 수정
//        User user = User.builder()
//                .emailNormalized(request.getEmail())
//                .passwordHash(passwordEncoder.encode(request.getPassword()))
//                .displayName(request.getDisplayName())
//                .role(Role.MEMBER)
//                .status(UserStatus.ACTIVE)
//                .preferredLocale("ko")
//                .emailVerifiredAt(verifiedAt)
//                .build();
        // userRepository.saveAndFlush(user);
//    }

//    @Transactional(readOnly = true)
//    public SessionUserDto login(LoginReqDto request) {
//        request.validate();
//        User user = userRepository.findByEmailNormalized(request.getEmail())
//                .orElseThrow(() -> new Exception401("이메일 또는 비밀번호가 올바르지 않습니다."));
//         Todo - User 클래스 완성되면 수정
//        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
//            throw new Exception401("이메일 또는 비밀번호가 올바르지 않습니다.");
//        }
//        if (user.getStatus() != UserStatus.ACTIVE || user.getWithdrawnAt() != null) {
//            throw new Exception403("현재 로그인할 수 없는 계정입니다.");
//        }
//        return SessionUserDto.builder()
//                .id(user.getId())
//                .displayName(user.getDisplayName())
//                .role(user.getRole())
//                .preferredLocale(user.getPreferredLocale())
//                .build();
//    }
//}
    }
}
