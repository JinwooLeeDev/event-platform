package com.busan.eventplatform.user.community;

import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;

/**
 * [f/40] 커뮤니티 화면 + 채팅·공지 전송
 * - GET  /community              내 커뮤니티 목록 (Q9) — ★ 기업은 주최한 행사의 운영 중 회차
 * - GET  /community/{sessionId}  채팅방 (참가 확정자 · ★ 주최 기업, 종료 후 진입 불가)
 * - STOMP SEND /app/community/{sessionId}/messages  채팅 전송 → 저장 → 방 전체에 전달
 * - ★ STOMP SEND /app/community/{sessionId}/notice    공지 등록·수정·내림 (주최 기업만) → 방 전체에 전달
 */
@Controller
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/community")
    public String myCommunities(
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity identity,
            Model model) {
        CommunityViewer viewer = requireViewer(identity);
        List<CommunityRes.MyCommunityDto> communities = communityService.getMyCommunities(viewer);
        model.addAttribute("communities", communities);
        model.addAttribute("empty", communities.isEmpty());
        model.addAttribute("host", viewer.isCompany());   // ★ [6단계] 기업이면 안내 문구가 다름
        return "community/list";
    }

    @GetMapping("/community/{sessionId}")
    public String room(
            @PathVariable Long sessionId,
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity identity,
            Model model) {
        CommunityViewer viewer = requireViewer(identity);
        model.addAttribute("room", communityService.getRoom(sessionId, viewer));
        return "community/room";
    }

    /** 채팅 전송 — 권한은 CommunityChannelInterceptor + saveChat 에서 검사 */
    @MessageMapping("/community/{sessionId}/messages")
    public void send(@DestinationVariable Long sessionId,
                     @Payload ChatRequest request,
                     SimpMessageHeaderAccessor accessor) {
        CommunityViewer viewer = CommunityViewer.from(accessor.getSessionAttributes());
        CommunityMessage message = communityService.saveChat(sessionId, viewer, request.content());
        messagingTemplate.convertAndSend("/topic/community/" + sessionId, message);
    }

    /** ★ [6단계] 공지 등록·수정 (빈 문구 = 공지 내림) — 주최 기업만 (saveNotice 에서 검사) */
    @MessageMapping("/community/{sessionId}/notice")
    public void notice(@DestinationVariable Long sessionId,
                       @Payload ChatRequest request,
                       SimpMessageHeaderAccessor accessor) {
        CommunityViewer viewer = CommunityViewer.from(accessor.getSessionAttributes());
        CommunityMessage message = communityService.saveNotice(sessionId, viewer, request.content());
        messagingTemplate.convertAndSend("/topic/community/" + sessionId, message);
    }

    /** 채팅·공지 전송 실패 → 보낸 사람에게만 안내 (빈 메시지, 500자 초과, 권한 없음 등) */
    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/community", broadcast = false)
    public CommunityMessage handleChatError(Exception e) {
        String reason = e.getMessage() != null ? e.getMessage() : "메시지를 보낼 수 없습니다";
        return CommunityMessage.system(CommunityMessage.Type.ERROR, null, null, reason);
    }

    /** 채팅·공지 전송 요청 본문 {"content": "..."} */
    public record ChatRequest(String content) {
    }

    /**
     * /myhistory 와 같은 방식의 로그인 확인
     * ★ [6단계] 개인 회원 → USER, 기업 → COMPANY (기업은 주최한 행사의 방만 — 서비스에서 검사)
     */
    private CommunityViewer requireViewer(LoginIdentity identity) {
        if (identity == null || identity.getId() == null || identity.getLoginType() == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }
        if (identity.isUser()) {
            return CommunityViewer.user(identity.getId());
        }
        if (identity.isCompany()) {
            return CommunityViewer.company(identity.getId());
        }
        throw new Exception403("개인 회원 또는 행사를 주최한 기업만 커뮤니티를 이용할 수 있습니다.");
    }
}
