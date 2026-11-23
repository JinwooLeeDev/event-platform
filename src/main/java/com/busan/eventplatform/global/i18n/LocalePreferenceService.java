package com.busan.eventplatform.global.i18n;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocalePreferenceService {
    private final UserRepository users;

    @Transactional
    public SessionUserDto synchronizeLogin(SessionUserDto authenticated, String explicit, String cookie) {
        User user = activeUser(authenticated.getId());
        String code;
        if (SupportedLocale.isSupported(explicit)) {
            code = explicit;
            user.updatePreferredLocale(code);
            users.flush();
        } else {
            code = SupportedLocale.isSupported(user.getPreferredLocale()) ? user.getPreferredLocale() : SupportedLocale.normalize(cookie);
        }
        return sessionUser(user, code);
    }

    @Transactional
    public SessionUserDto change(Long userId, String code) {
        if (!SupportedLocale.isSupported(code)) throw new Exception400("지원하는 언어를 선택해주세요.");
        User user = activeUser(userId);
        user.updatePreferredLocale(code);
        users.flush();
        return sessionUser(user, code);
    }

    private User activeUser(Long id) {
        if (id == null) throw new Exception401("로그인 후 이용해주세요.");
        User user = users.findById(id).orElseThrow(() -> new Exception401("회원 정보를 확인할 수 없습니다."));
        if (user.getStatus() != UserStatus.ACTIVE || user.getWithdrawnAt() != null) throw new Exception403("현재 이용할 수 없는 계정입니다.");
        return user;
    }

    private SessionUserDto sessionUser(User user, String code) {
        return SessionUserDto.builder().id(user.getId()).displayName(user.getDisplayName()).role(user.getRole()).preferredLocale(code).build();
    }
}
