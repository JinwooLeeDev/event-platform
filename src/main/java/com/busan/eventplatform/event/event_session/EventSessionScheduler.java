package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.user.community.CommunityPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class EventSessionScheduler {
    private final EventSessionService eventSessionService;
    private final TaskScheduler taskScheduler;


    /**
     *  4시간마다 종료된 행사가 있는지 조회. --> 정상종료된 기업에게 보증금을 돌려주는 refundEventCompleted에서 DepositStatus를 Refund로 변경하므로
     *  이전부터 정상종료된 행사를 전부 조회하는게 아닌 최근 정상종료된 행사만 조회.
     */

    @Scheduled(cron = "0 0 */4 * * *", zone = "Asia/Seoul")
    public void processEventCompleted(){
        LocalDateTime now = LocalDateTime.now(CommunityPolicy.ZONE);

        for(Long sessionId : eventSessionService.completedSessionIds(now)) {
            try{
                eventSessionService.completedSession(sessionId , now);
            }
            catch (RuntimeException e) {
                log.error("예외 발생회차 Id : {}" , sessionId);
            }
        }
    }


    /**
     *
     * taskScheduler.schedule( 1 , 2)
     * 1번째 인자 - 어떤 함수를 실행할지     2번째 인자 - 언제 실행할지
     * es.getStartsAt().atZone(CommunityPolicy.ZONE).toInstant()) : 세션의 시작시간이 CommunityPolicy.ZONE의 시간과 일치하면.
     * toInstant() : Instant요소로 변환.
     */

    public void processEventStarted(EventSession es) {
        taskScheduler.schedule(() -> eventSessionService.startSession(es.getId()) ,
                es.getStartsAt().atZone(CommunityPolicy.ZONE).toInstant());
    }

}
