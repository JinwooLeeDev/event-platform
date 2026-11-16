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
import java.util.Map;

/**
 * [f/40] 커뮤니티 화면 + 채팅 전송
 * - GET  /community              내 커뮤니티 목록 (Q9)
 * - GET  /community/{sessionId}  채팅방 (활성 멤버만, 종료 후 진입 불가)
 * - STOMP SEND /app/community/{sessionId}/messages  채팅 전송 → 저장 → 방 전체에 전달
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
        Long userId = requireUser(identity);
        List<CommunityRes.MyCommunityDto> communities = communityService.getMyCommunities(userId);
        model.addAttribute("communities", communities);
        model.addAttribute("empty", communities.isEmpty());
        return "community/list";
    }

    @GetMapping("/community/{sessionId}")
    public String room(
            @PathVariable Long sessionId,
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity identity,
            Model model) {
        Long userId = requireUser(identity);
        model.addAttribute("room", communityService.getRoom(sessionId, userId));
        return "community/room";
    }

    /** 채팅 전송 — 권한은 CommunityChannelInterceptor + saveChat 에서 검사 */
    @MessageMapping("/community/{sessionId}/messages")
    public void send(@DestinationVariable Long sessionId,
                     @Payload ChatRequest request,
                     SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        Long userId = attributes == null ? null
                : (Long) attributes.get(CommunityHandshakeInterceptor.ATTR_USER_ID);
        CommunityMessage message = communityService.saveChat(sessionId, userId, request.content());
        messagingTemplate.convertAndSend("/topic/community/" + sessionId, message);
    }

    /** 채팅 전송 실패 → 보낸 사람에게만 안내 (빈 메시지, 500자 초과 등) */
    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/community", broadcast = false)
    public CommunityMessage handleChatError(Exception e) {
        String reason = e.getMessage() != null ? e.getMessage() : "메시지를 보낼 수 없습니다";
        return CommunityMessage.system(CommunityMessage.Type.ERROR, null, null, reason);
    }

    /** 채팅 전송 요청 본문 {"content": "..."} */
    public record ChatRequest(String content) {
    }

    /** /myhistory 와 같은 방식의 로그인 확인 */
    private Long requireUser(LoginIdentity identity) {
        if (identity == null || identity.getId() == null || identity.getLoginType() == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }
        if (!identity.isUser()) {
            throw new Exception403("개인 회원만 커뮤니티를 이용할 수 있습니다.");
        }
        return identity.getId();
    }
}
