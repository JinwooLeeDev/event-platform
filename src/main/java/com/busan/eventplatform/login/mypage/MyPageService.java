package com.busan.eventplatform.login.mypage;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void updatePassword(Long userId, MyPasswordUpdateReqDto request) {
        request.validate();
        User user = findActiveUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new Exception400("입력하신 현재 비밀번호가 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new Exception400("변경할 비밀번호는 현재 비밀번호와 달라야합니다.");
        }
        String newPassword = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(newPassword);
    }

    @Transactional(readOnly = true)
    public MyPageResDto getMyPage(Long userId) {
        User user = findActiveUser(userId);

        return MyPageResDto.from(user);
    }

    @Transactional
    public SessionUserDto updateMyPage(Long userId, MyPageUpdateReqDto request) {
        request.validate();

        User user = findActiveUser(userId);

        user.changeDisplayName(request.getDisplayName());
        user.updatePreferredLocale(request.getPreferredLocale());

        return SessionUserDto.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .preferredLocale(user.getPreferredLocale())
                .build();
    }

    private User findActiveUser(Long userId) {
        if (userId == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception401(
                        "회원 정보를 확인할 수 없습니다. 다시 로그인해주세요."
                ));

        if (user.getStatus() != UserStatus.ACTIVE || user.getWithdrawnAt() != null) {
            throw new Exception403("현재 이용할 수 없는 계정입니다.");
        }
        return user;
    }
}