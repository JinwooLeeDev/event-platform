package com.busan.eventplatform.event.event_favorite;

import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.LoginSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MyFavoriteController {
    private final EventFavoriteService eventFavoriteService;

    @GetMapping("/myfavorites")
    public String list(HttpSession session, Model model) {
        var identity = LoginSession.requireIdentity(session);
        if (!identity.isUser()) throw new Exception403("개인 회원만 좋아요한 행사를 조회할 수 있습니다.");
        var favorites = eventFavoriteService.findAllByUserId(identity.getId());
        model.addAttribute("favorites", favorites);
        model.addAttribute("favoriteTotal", favorites.size());
        model.addAttribute("hasFavorites", !favorites.isEmpty());
        return "user/myfavorites";
    }
}
