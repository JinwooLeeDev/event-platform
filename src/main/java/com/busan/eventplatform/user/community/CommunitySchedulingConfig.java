package com.busan.eventplatform.user.community;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * [f/40] 스케줄러 켜기 — 프로젝트에 @EnableScheduling 이 없어서(11-16 확인) 커뮤니티 패키지에 둔다.
 * - 이 설정은 앱 전체에 적용되므로, 다른 파트에서 @Scheduled 를 쓸 때 따로 켤 필요 없음 (팀 공유)
 * - 메인 클래스(조장님 파일)는 수정하지 않기 위해 별도 설정 클래스로 분리
 */
@Configuration
@EnableScheduling
public class CommunitySchedulingConfig {
}
