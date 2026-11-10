package com.busan.eventplatform.login.mypage;

import com.busan.eventplatform.api.purchase.PurchaseService;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordChangeTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private MyPasswordUpdateReqDto request(String current, String next, String confirm) {
        MyPasswordUpdateReqDto dto = new MyPasswordUpdateReqDto();
        dto.setCurrentPassword(current);
        dto.setNewPassword(next);
        dto.setNewPasswordConfirm(confirm);
        return dto;
    }

    @Test
    void storesHashThatAcceptsNewPasswordAndRejectsOldPassword() {
        User user = User.builder().passwordHash(encoder.encode("old-password")).build();
        UserRepository repository = mock(UserRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        new MyPageService(repository, encoder).updatePassword(1L,
                request("old-password", "new-password", "new-password"));
        assertNotEquals("new-password", user.getPasswordHash());
        assertTrue(encoder.matches("new-password", user.getPasswordHash()));
        assertFalse(encoder.matches("old-password", user.getPasswordHash()));
    }

    @Test
    void wrongCurrentPasswordAndUnchangedPasswordDoNotModifyHash() {
        User user = User.builder().passwordHash(encoder.encode("old-password")).build();
        String originalHash = user.getPasswordHash();
        UserRepository repository = mock(UserRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        MyPageService service = new MyPageService(repository, encoder);
        assertThrows(Exception400.class, () -> service.updatePassword(1L,
                request("incorrect", "new-password", "new-password")));
        assertThrows(Exception400.class, () -> service.updatePassword(1L,
                request("old-password", "old-password", "old-password")));
        assertEquals(originalHash, user.getPasswordHash());
    }

    @Test
    void validatesMissingFieldsConfirmationAndUtf8ByteLimit() {
        assertThrows(Exception400.class, () -> request(null, "new-password", "new-password").validate());
        assertThrows(Exception400.class, () -> request("old", "new-password", "different").validate());
        assertThrows(Exception400.class, () -> request("old", "short", "short").validate());
        String tooLong = "가".repeat(25);
        assertThrows(Exception400.class, () -> request("old", tooLong, tooLong).validate());
    }

    @Test
    void successfulChangeInvalidatesSessionAndRedirectsToLogin() {
        MyPageService service = mock(MyPageService.class);
        MyPageController controller = new MyPageController(service, mock(PurchaseService.class));
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        MyPasswordUpdateReqDto dto = request("old-password", "new-password", "new-password");
        assertEquals("redirect:/login", controller.updatePassword(
                SessionUserDto.builder().id(1L).build(), dto, session, flash));
        verify(service).updatePassword(1L, dto);
        assertTrue(session.isInvalid());
        assertTrue(flash.getFlashAttributes().containsKey("passwordChangedMessage"));
    }

    @Test
    void validationFailureKeepsSessionAndAnonymousRequestIsRejected() {
        MyPageService service = mock(MyPageService.class);
        MyPageController controller = new MyPageController(service, mock(PurchaseService.class));
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        MyPasswordUpdateReqDto dto = request("wrong", "new-password", "new-password");
        doThrow(new Exception400("현재 비밀번호 확인")).when(service).updatePassword(1L, dto);
        assertEquals("redirect:/mypage#password-settings", controller.updatePassword(
                SessionUserDto.builder().id(1L).build(), dto, session, flash));
        assertFalse(session.isInvalid());
        assertEquals(1, flash.getFlashAttributes().size());
        assertTrue(flash.getFlashAttributes().containsKey("passwordError"));
        assertThrows(Exception401.class, () -> controller.updatePassword(null, dto, session, flash));
    }
}
