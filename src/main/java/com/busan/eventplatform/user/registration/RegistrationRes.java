package com.busan.eventplatform.user.registration;


import lombok.Builder;
import lombok.Getter;

// 데이터 전달 객체
@Getter
@Builder
public class RegistrationRes {
    private boolean isSuccess; // 요청한 작업(참가 신청 등)의 최종 성공(true) 여부
    private String message; // 작업 결과에 대해 사용자에게 보여줄 구체적인 안내 텍스트 -> alert 창을 통해 안내
    private Integer queueNumber; // 대기열 번호(확정자일 경우 null 또는 0)
    private String status; // 현재 상태(CONFIRMED, WAITING, CANCELLED)
}
