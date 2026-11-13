package com.busan.eventplatform.admin;

import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.user.Role;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {
    private final UserRepository userRepository;

    public AdminMemberResponse.PageDto findMembers(String keyword, UserStatus status, int page) {
        String search = keyword == null ? "" : keyword.trim();
        String pattern = "%" + search.toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var pageable = PageRequest.of(Math.max(1, page) - 1, 20,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return new AdminMemberResponse.PageDto(userRepository.findAdminMembers(pattern, status, pageable), search, status);
    }

    public AdminMemberResponse.MemberDto findMember(Long id) {
        return new AdminMemberResponse.MemberDto(userRepository.findById(id)
                .orElseThrow(() -> new Exception404("회원을 찾을 수 없습니다.")));
    }

    @Transactional
    public void changeMemberStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception404("회원을 찾을 수 없습니다."));
        if (status != UserStatus.ACTIVE && status != UserStatus.SUSPENDED) {
            throw new Exception400("활성 또는 정지 상태로만 변경할 수 있습니다.");
        }
        if (user.getRole() != Role.MEMBER) {
            throw new Exception400("관리자 계정의 상태는 변경할 수 없습니다.");
        }
        if (user.getStatus() == UserStatus.WITHDRAWN || user.getWithdrawnAt() != null) {
            throw new Exception400("탈퇴 회원의 상태는 변경할 수 없습니다.");
        }
        user.changeStatus(status);
    }

}
