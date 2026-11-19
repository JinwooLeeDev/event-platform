package com.busan.eventplatform.user.attendance;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.user.registration.Registration;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import com.busan.eventplatform.user.registration.RegistrationRes;
import com.busan.eventplatform.user.registration.RegistrationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ★ [f/42] 출석 (F04) — 사이트가 만든 QR 로 참가 확정 회원이 현장에서 출석한다.
 * <pre>
 * [주최 기업] GET /attendance/qr/{회차 id}  → 사이트가 QR 생성 (주소 + 서명 code) → 현장에 띄움
 * [참가자]    QR 인식 → GET /attendance/check/{회차 id}?code=…  → [출석 체크]
 *            POST /attendance/check/{회차 id}  → 검증 후 출석 저장
 * </pre>
 * 검증: 서명 code · 개인 회원 · 본인의 참가 확정(CONFIRMED) 신청 · 출석 가능 시간(시작 30분 전 ~ 종료) · 중복 출석
 * <p>
 * 출석 취소·정정은 없다. 본인 취소·기업 거절이 회차 시작 후 막혀 있어(f/41) 출석한 신청이 나중에 취소·거절되지 않는다.
 * <p>
 * [후기 담당 제공] isAttended(userId, sessionId), getAttendedSessionIds(userId)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final RegistrationRepository registrationRepository;
    private final EventSessionRepository eventSessionRepository;
    private final AttendanceQrSigner qrSigner;

    // =========================================================
    // [후기 담당 제공]
    // =========================================================

    /** 회원이 회차에 출석했는지 — 참가 확정 신청 + 출석 기록이 있을 때만 true (후기 저장 전 검증) */
    public boolean isAttended(Long userId, Long sessionId) {
        if (userId == null || sessionId == null) {
            return false;
        }
        return attendanceRepository.existsAttended(userId, sessionId, RegistrationStatus.CONFIRMED);
    }

    /** 회원이 출석한 회차 id 목록 (최근 출석순) — 후기 탭에 '후기 창'을 띄울 회차 */
    public List<Long> getAttendedSessionIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return attendanceRepository.findAttendedSessionIds(userId, RegistrationStatus.CONFIRMED);
    }

    // =========================================================
    // [주최 기업] 출석 QR 화면
    // =========================================================

    public AttendanceRes.QrPageDto getQrPage(Long sessionId, LoginIdentity identity) {
        if (identity == null || !identity.isCompany()) {
            throw new Exception403("행사 주최 기업만 출석 QR을 열 수 있습니다.");
        }
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception404("행사 회차가 존재하지 않습니다"));
        if (!session.getEvent().isOwner(identity.getId())) {
            throw new Exception403("행사 주최 기업만 출석 QR을 열 수 있습니다.");
        }

        return AttendanceRes.QrPageDto.builder()
                .sessionId(sessionId)
                .eventTitle(session.getEvent().getTitle())
                .sessionName(session.getName())
                .scheduleText(RegistrationRes.formatSchedule(session.getStartsAt(), session.getEndsAt()))
                .checkPath("/attendance/check/" + sessionId + "?code=" + qrSigner.sign(sessionId))
                .checkTimeText(AttendancePolicy.checkTimeText(session.getStartsAt(), session.getEndsAt()))
                .attendedCount(attendanceRepository.countAttended(sessionId, RegistrationStatus.CONFIRMED))
                .confirmedCount(registrationRepository.countConfirmedBySession(sessionId))
                .build();
    }

    // =========================================================
    // [참가자] QR 인식 → 확인 화면(GET) → 출석(POST)
    // =========================================================

    /** QR 인식 직후 화면 — 출석할 수 있는지 미리 보여준다 (저장하지 않음) */
    public AttendanceRes.CheckPageDto getCheckPage(Long sessionId, String code, LoginIdentity identity) {
        return evaluate(sessionId, code, identity, false);
    }

    /** [출석 체크] — 같은 검사를 회차 락 안에서 다시 하고 통과하면 저장 (동시에 두 번 눌러도 1건) */
    @Transactional
    public AttendanceRes.CheckPageDto check(Long sessionId, String code, LoginIdentity identity) {
        return evaluate(sessionId, code, identity, true);
    }

    /**
     * 출석 검사 한 곳 — 화면 표시(save=false)와 저장(save=true)이 같은 규칙을 쓴다.
     * 업무 규칙 위반은 예외 대신 안내 문구로 돌려준다 (휴대폰에서 오류 페이지 대신 이유를 바로 보여주기 위해).
     */
    private AttendanceRes.CheckPageDto evaluate(Long sessionId, String code, LoginIdentity identity, boolean save) {
        Optional<EventSession> found = save
                ? eventSessionRepository.findByIdForUpdate(sessionId)   // 저장 시 회차 락 → 중복 저장 방지
                : eventSessionRepository.findById(sessionId);
        if (found.isEmpty()) {
            return result(sessionId, null, null).message("존재하지 않는 회차의 출석 QR입니다.").messageClass("is-error").build();
        }
        EventSession session = found.get();

        // 1. QR 서명 — 회차 id 만 바꿔 만든 주소 차단
        if (!qrSigner.matches(sessionId, code)) {
            return result(sessionId, session, null).message("유효하지 않은 출석 QR입니다. 현장에 안내된 QR을 다시 인식해 주세요.").messageClass("is-error").build();
        }

        // 2. 로그인 · 개인 회원
        if (identity == null || identity.getId() == null) {
            return result(sessionId, session, code).loginRequired(true)
                    .message("로그인한 뒤 QR을 다시 인식해 주세요.").messageClass("is-info").build();
        }
        if (!identity.isUser()) {
            return result(sessionId, session, code)
                    .message("개인 회원 계정으로 로그인해야 출석할 수 있습니다.").messageClass("is-error").build();
        }

        // 3. 본인의 참가 확정 신청
        Registration registration = registrationRepository
                .findFirstByUserIdAndEventSessionIdAndStatus(identity.getId(), sessionId, RegistrationStatus.CONFIRMED)
                .orElse(null);
        if (registration == null) {
            return result(sessionId, session, code)
                    .message("이 회차에 참가가 확정된 회원만 출석할 수 있습니다.").messageClass("is-error").build();
        }

        // 4. 이미 출석 — 오류가 아니라 완료로 안내
        if (attendanceRepository.existsByRegistrationId(registration.getId())) {
            return result(sessionId, session, code).done(true)
                    .message("이미 출석 체크되었습니다. 신청 내역에서 후기를 남길 수 있습니다.").messageClass("is-success").build();
        }

        // 5. 출석 가능 시간 (시작 30분 전 ~ 종료)
        if (!AttendancePolicy.isCheckTime(session.getStartsAt(), session.getEndsAt(), LocalDateTime.now())) {
            return result(sessionId, session, code)
                    .message("지금은 출석할 수 있는 시간이 아닙니다.").messageClass("is-error").build();
        }

        if (!save) {
            return result(sessionId, session, code).canSubmit(true)
                    .message("아래 버튼을 누르면 출석이 완료됩니다.").messageClass("is-info").build();
        }

        // 6. 저장 — 확인자(checkedBy)는 QR 을 띄운 주최 기업 (시드 데이터와 같은 의미)
        attendanceRepository.save(Attendance.create(registration, session.getEvent().getCompany().getId()));
        return result(sessionId, session, code).done(true)
                .message("출석 체크가 완료되었습니다. 신청 내역에서 후기를 남길 수 있습니다.").messageClass("is-success").build();
    }

    /** 회차 정보가 채워진 화면 DTO 빌더 (code 는 유효할 때만 다시 넘긴다) */
    private AttendanceRes.CheckPageDto.CheckPageDtoBuilder result(Long sessionId, EventSession session, String code) {
        AttendanceRes.CheckPageDto.CheckPageDtoBuilder builder = AttendanceRes.CheckPageDto.builder()
                .sessionId(sessionId)
                .code(code);
        if (session != null) {
            builder.eventTitle(session.getEvent().getTitle())
                    .sessionName(session.getName())
                    .scheduleText(RegistrationRes.formatSchedule(session.getStartsAt(), session.getEndsAt()))
                    .checkTimeText(AttendancePolicy.checkTimeText(session.getStartsAt(), session.getEndsAt()));
        }
        return builder;
    }
}