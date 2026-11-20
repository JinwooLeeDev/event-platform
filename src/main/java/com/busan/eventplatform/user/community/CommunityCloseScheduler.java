package com.busan.eventplatform.user.community;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * [f/40] 커뮤니티 종료 (Q4·Q10·Q11) — 회차 종료일 24시에 방을 닫는다 + 종료일 당일 11:00 안내(5-2)
 * - 매일 00:00 (Asia/Seoul) 실행
 * - 서버 시작 시에도 1회 실행 → 서버가 꺼져 있어 자정 실행을 놓쳐도 정리됨
 * - 순서: DB 정리(멤버 비활성·메시지 삭제, 한 트랜잭션) → 커밋 후 CLOSED 알림 (접속 중인 화면 종료)
 *
 * 로컬 테스트: IntelliJ 실행 설정 VM options 에 -Dcommunity.close-cron="0 * * * * *" → 매분 실행 (파일 수정 없이)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityCloseScheduler {

    private final CommunityService communityService;
    private final CommunityNotifier communityNotifier;

    @Scheduled(cron = "${community.close-cron:0 0 0 * * *}", zone = CommunityPolicy.ZONE_NAME)
    public void closeAtMidnight() {
        run("자정");
    }

    /**
     * ★ [5-2] 종료일 당일 11:00 안내 — 오늘이 종료일인 방에 실시간 안내 1번
     * 접속해 있지 않던 사람은 방에 들어올 때 상단 배너로 봄(getRoom) → 따로 저장하지 않아 재시작해도 중복 없음
     * 로컬 테스트: VM 옵션 -Dcommunity.end-day-notice-cron="0 * * * * *"
     */
    @Scheduled(cron = "${community.end-day-notice-cron:0 0 11 * * *}", zone = CommunityPolicy.ZONE_NAME)
    public void noticeEndDay() {
        try {
            List<Long> rooms = communityService.findRoomsEndingToday(CommunityPolicy.now());
            rooms.forEach(communityNotifier::notifyEndDay);
            log.info("[community] 종료일 안내 {}개 방 {}", rooms.size(), rooms);
        } catch (Exception e) {
            log.error("[community] 종료일 안내 실패", e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void closeOnStartup() {
        run("서버 시작");
    }

    private void run(String trigger) {
        try {
            List<Long> closed = communityService.closeExpired(CommunityPolicy.now()); // 여기서 트랜잭션 커밋 완료
            closed.forEach(communityNotifier::notifyClosed);                         // 커밋 후 알림
            log.info("[community] 종료 처리({}) {}개 방 {}", trigger, closed.size(), closed);
        } catch (Exception e) {
            // 실패해도 앱은 계속 동작 — 화면·STOMP 에서 종료 시각을 직접 계산해 막고 있음 (이중 차단)
            log.error("[community] 종료 처리({}) 실패", trigger, e);
        }
    }

}
