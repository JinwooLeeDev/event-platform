package com.busan.eventplatform.admin;

import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminMemberServiceTest {
    @Test
    void suspendsAndReactivatesMemberWithoutChangingOtherFields() {
        var repository = mock(UserRepository.class);
        var user = User.builder().displayName("회원").role(Role.MEMBER).point(123).build();
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        var service = new AdminMemberService(repository);
        service.changeMemberStatus(1L, UserStatus.SUSPENDED);
        assertEquals(UserStatus.SUSPENDED, user.getStatus());
        assertTrue(new AdminMemberResponse.MemberDto(user).isCanActivate());
        assertFalse(new AdminMemberResponse.MemberDto(user).isCanSuspend());
        service.changeMemberStatus(1L, UserStatus.ACTIVE);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(new AdminMemberResponse.MemberDto(user).isCanSuspend());
        assertEquals(123, user.getPoint());
        assertNull(user.getWithdrawnAt());
    }

    @Test
    void rejectsAdminWithdrawnMemberAndInvalidTargetStatuses() {
        var repository = mock(UserRepository.class);
        var admin = User.builder().role(Role.ADMIN).build();
        var withdrawn = User.builder().role(Role.MEMBER).status(UserStatus.WITHDRAWN).build();
        var member = User.builder().role(Role.MEMBER).build();
        when(repository.findById(1L)).thenReturn(Optional.of(admin));
        when(repository.findById(2L)).thenReturn(Optional.of(withdrawn));
        when(repository.findById(3L)).thenReturn(Optional.of(member));
        when(repository.findById(4L)).thenReturn(Optional.empty());
        var service = new AdminMemberService(repository);
        assertThrows(com.busan.eventplatform.global.error.exception.Exception400.class,
                () -> service.changeMemberStatus(1L, UserStatus.SUSPENDED));
        assertThrows(com.busan.eventplatform.global.error.exception.Exception400.class,
                () -> service.changeMemberStatus(2L, UserStatus.ACTIVE));
        assertThrows(com.busan.eventplatform.global.error.exception.Exception400.class,
                () -> service.changeMemberStatus(3L, UserStatus.WITHDRAWN));
        assertThrows(com.busan.eventplatform.global.error.exception.Exception400.class,
                () -> service.changeMemberStatus(3L, null));
        assertThrows(Exception404.class, () -> service.changeMemberStatus(4L, UserStatus.ACTIVE));
        assertEquals(UserStatus.ACTIVE, admin.getStatus());
        assertEquals(UserStatus.WITHDRAWN, withdrawn.getStatus());
        assertEquals(UserStatus.ACTIVE, member.getStatus());
        assertFalse(new AdminMemberResponse.MemberDto(admin).isCanSuspend());
        assertFalse(new AdminMemberResponse.MemberDto(withdrawn).isCanActivate());
    }

    @Test
    void escapesSearchWildcardsAndPreservesStatusAndPage() {
        var repository = mock(UserRepository.class);
        when(repository.findAdminMembers(anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        var result = new AdminMemberService(repository).findMembers("  a_%!  ", UserStatus.SUSPENDED, 2);
        verify(repository).findAdminMembers(eq("%a!_!%!!%"), eq(UserStatus.SUSPENDED),
                argThat(page -> page.getPageNumber() == 1 && page.getPageSize() == 20));
        assertEquals("a_%!", result.getKeyword());
        assertTrue(result.isSuspendedSelected());
        assertTrue(result.getMembers().isEmpty());
    }

    @Test
    void mapsPublicMemberFieldsAndRejectsUnknownMember() {
        var repository = mock(UserRepository.class);
        var user = User.builder().displayName("회원").emailNormalized("member@example.com")
                .passwordHash("must-not-be-exposed").role(Role.MEMBER).status(UserStatus.ACTIVE).point(123).build();
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.findById(2L)).thenReturn(Optional.empty());
        var service = new AdminMemberService(repository);
        var member = service.findMember(1L);
        assertEquals("회원", member.getDisplayName());
        assertEquals("활성", member.getStatusName());
        assertEquals(123, member.getPoint());
        assertThrows(Exception404.class, () -> service.findMember(2L));
    }
}
