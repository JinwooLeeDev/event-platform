package com.busan.eventplatform.admin;

import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserStatus;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminMemberResponse {
    private static String date(Instant value) {
        return value == null ? "—" : DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
                .withZone(ZoneId.of("Asia/Seoul")).format(value);
    }

    @Getter
    public static class MemberDto {
        private final Long id;
        private final String displayName;
        private final String email;
        private final String roleName;
        private final String statusName;
        private final int point;
        private final String createdAt;
        private final String emailVerifiedAt;
        private final String withdrawnAt;
        private final String preferredLocale;
        private final boolean canSuspend;
        private final boolean canActivate;

        public MemberDto(User user) {
            id = user.getId();
            displayName = user.getDisplayName();
            email = user.getEmailNormalized();
            roleName = user.getRole() == com.busan.eventplatform.user.Role.ADMIN ? "관리자" : "일반 회원";
            statusName = switch (user.getStatus()) {
                case ACTIVE -> "활성";
                case SUSPENDED -> "정지";
                case WITHDRAWN -> "탈퇴";
            };
            point = user.getPoint();
            createdAt = date(user.getCreatedAt());
            emailVerifiedAt = date(user.getEmailVerifiedAt());
            withdrawnAt = date(user.getWithdrawnAt());
            preferredLocale = user.getPreferredLocale() == null ? "—" : user.getPreferredLocale();
            boolean editable = user.getRole() == com.busan.eventplatform.user.Role.MEMBER
                    && user.getWithdrawnAt() == null;
            canSuspend = editable && user.getStatus() == UserStatus.ACTIVE;
            canActivate = editable && user.getStatus() == UserStatus.SUSPENDED;
        }
    }

    @Getter
    public static class PageDto {
        private final List<MemberDto> members;
        private final String keyword;
        private final long totalElements;
        private final int currentPage;
        private final int totalPages;
        private final boolean allSelected;
        private final boolean activeSelected;
        private final boolean suspendedSelected;
        private final boolean withdrawnSelected;
        private final String previousUrl;
        private final String nextUrl;

        public PageDto(Page<User> page, String keyword, UserStatus status) {
            members = page.getContent().stream().map(MemberDto::new).toList();
            this.keyword = keyword;
            totalElements = page.getTotalElements();
            currentPage = page.getNumber() + 1;
            totalPages = Math.max(1, page.getTotalPages());
            allSelected = status == null;
            activeSelected = status == UserStatus.ACTIVE;
            suspendedSelected = status == UserStatus.SUSPENDED;
            withdrawnSelected = status == UserStatus.WITHDRAWN;
            String url = "/admin/members?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&status=" + (status == null ? "" : status.name()) + "&page=";
            previousUrl = page.hasPrevious() ? url + (currentPage - 1) : "";
            nextUrl = page.hasNext() ? url + (currentPage + 1) : "";
        }
    }
}
