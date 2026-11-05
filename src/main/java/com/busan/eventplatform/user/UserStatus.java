package com.busan.eventplatform.user;

public enum UserStatus {
    ACTIVE, // 계정 활성 상태
    SUSPENDED, // 제재/정지 상태
    WITHDRAWN // 탈퇴 상태 (개인정보 물리 삭제 지연 시 활용)
}
