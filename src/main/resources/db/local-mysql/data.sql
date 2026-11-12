------------------------------ Company 테이블 --------------------------------
-- 개발용 더미 데이터
-- 비밀번호: 1234
-- 사업자 검증 상태는 관리자 기능 테스트를 위해 직접 지정한 값입니다.

INSERT INTO company_tb (
    name,
    email,
    password,
    business_number,
    representative_name,
    opened_on,
    address,
    business_certificate_file_name,
    contact_person_name,
    contact_person_phone,
    status,
    business_verification_status,
    review_feedback,
    point,
    created_at
)
VALUES
-- 1. 심사 대기 + API 일치: 승인·보완 요청 가능
(
    '부산문화재단',
    'company1@example.test',
    '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
    '0000000001',
    '가상대표1',
    '2020-01-01',
    '부산광역시 남구 가상로 1',
    NULL,
    '더미담당1',
    '010-0000-0001',
    'PENDING',
    'MATCHED',
    NULL,
    500000,
    CURRENT_TIMESTAMP
),
-- 2. 심사 대기 + API 오류: 승인 불가, 보완 요청 가능
(
    '해운대이벤트',
    'company2@example.test',
    '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
    '0000000002',
    '가상대표2',
    '2021-02-01',
    '부산광역시 해운대구 가상로 2',
    NULL,
    '더미담당2',
    '010-0000-0002',
    'PENDING',
    'ERROR',
    NULL,
    100000,
    CURRENT_TIMESTAMP
),
-- 3. 승인 완료: 추가 심사 처리 불가
(
    '부산페스티벌',
    'company3@example.test',
    '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
    '0000000003',
    '가상대표3',
    '2022-03-01',
    '부산광역시 부산진구 가상로 3',
    NULL,
    '더미담당3',
    '010-0000-0003',
    'APPROVED',
    'MATCHED',
    NULL,
    300000,
    CURRENT_TIMESTAMP
),
-- 4. 관리자 보완 요청: 최근 보완 사유 표시
(
    '광안리축제기획',
    'company4@example.test',
    '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
    '0000000004',
    '가상대표4',
    '2023-04-01',
    '부산광역시 수영구 가상로 4',
    NULL,
    '더미담당4',
    '010-0000-0004',
    'FEEDBACK_NEEDED',
    'MATCHED',
    '사업자등록증의 글자가 흐릿합니다. 선명한 파일로 다시 제출해주세요.',
    400000,
    CURRENT_TIMESTAMP
),
-- 5. API 불일치: 관리자 심사 대상 제외
(
    '부산공연기획사',
    'company5@example.test',
    '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
    '0000000005',
    '가상대표5',
    '2024-05-01',
    '부산광역시 동래구 가상로 5',
    NULL,
    '더미담당5',
    '010-0000-0005',
    'FEEDBACK_NEEDED',
    'MISMATCHED',
    NULL,
    150000,
    CURRENT_TIMESTAMP
),
-- 6. 최종 반려: 심사 처리 불가
(
    '반려테스트기업',
    'company6@example.test',
    '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
    '0000000006',
    '가상대표6',
    '2020-06-01',
    '부산광역시 가상로 6',
    NULL,
    '더미담당6',
    '010-0000-0006',
    'REJECTED',
    'MATCHED',
    '테스트용 최종 반려 사유입니다.',
    0,
    CURRENT_TIMESTAMP
),
-- 7. 이용 정지: 심사 처리 불가
(
    '정지테스트기업',
    'company7@example.test',
    '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
    '0000000007',
    '가상대표7',
    '2020-07-01',
    '부산광역시 가상로 7',
    NULL,
    '더미담당7',
    '010-0000-0007',
    'SUSPENDED',
    'MATCHED',
    NULL,
    0,
    CURRENT_TIMESTAMP
),
-- 8. 미검증: 관리자 심사 대상 제외
(
    '미검증테스트기업',
    'company8@example.test',
    '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
    '0000000008',
    '가상대표8',
    '2020-08-01',
    '부산광역시 가상로 8',
    NULL,
    '더미담당8',
    '010-0000-0008',
    'PENDING',
    'NOT_CHECKED',
    NULL,
    0,
    CURRENT_TIMESTAMP
);

------------------------------ User테이블----------------------------------------
-- 로컬 확인용 계정의 비밀번호는 1234입니다. BCrypt 해시로 저장합니다.
INSERT INTO users(display_name, role, email_normalized, point , password_hash, status, preferred_locale, email_verified_at, created_at)
VALUES
    ('관리자', 'ADMIN', 'admin@busan.com', 10000 , '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('김민수', 'MEMBER', 'minsu@naver.com', 0 ,'$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('이서연', 'MEMBER', 'seoyeon@naver.com', 40000 , '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('박지훈', 'MEMBER', 'jihun@naver.com', 70000 , '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('최유진', 'MEMBER', 'yujin@naver.com', 150000 , '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('정현우', 'MEMBER', 'hyunwoo@naver.com', 25000 , '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


------------------------------ event_category테이블----------------------------------------
INSERT INTO event_category_tb (id, name) VALUES
    (1, '공연'),
    (2, '음식/축제'),
    (3, '문화/전시');


------------------------------ event테이블----------------------------------------

INSERT INTO events (
    company_id,
    category_id,
    address,
    title,
    summary,
    status,
    description,
    apply_start_at,
    apply_end_at,
    published_at,
    cancelled_at,
    cancellation_reason,
    feedback_message,
    created_at
) VALUES
      (
          1,
          1,
          '부산광역시 해운대구 APEC로 55',
          '2026 부산 겨울 콘서트',
          '부산에서 즐기는 겨울 음악 축제',
          'PUBLISHED',
          '다양한 아티스트들이 참여하는 겨울 콘서트입니다.',
          '2026-11-10 09:00:00',
          '2026-12-10 18:00:00',
          '2026-11-09 10:00:00',
          NULL,
          NULL,
          NULL,
          NOW()
      ),
      (
          2,
          2,
          '부산광역시 부산진구 중앙대로 708',
          '부산 푸드 페스티벌',
          '부산의 다양한 음식을 즐기는 축제',
          'PUBLISHED',
          '부산의 대표 음식과 다양한 먹거리를 체험할 수 있습니다.',
          '2026-11-15 09:00:00',
          '2026-12-15 18:00:00',
          '2026-11-09 10:00:00',
          NULL,
          NULL,
          NULL,
          NOW()
      ),
      (
          3,
          3,
          '부산광역시 수영구 광안해변로 219',
          '2026 부산 문화 예술 전시회',
          '다양한 예술 작품을 감상하는 문화 행사',
          'PENDING_REVIEW',
          '지역 예술가들의 작품을 전시하고 체험하는 행사입니다.',
          '2026-12-01 09:00:00',
          '2026-12-20 18:00:00',
          NULL,
          NULL,
          NULL,
          NULL,
          NOW()
      );


------------------------------ eventSession테이블----------------------------------------

INSERT INTO event_sessions (
    capacity,
    name,
    event_id,
    registration_opens_at,
    registration_closes_at,
    starts_at,
    ends_at,
    occupied_count,
    next_reception_number,
    hold_duration_seconds,
    pricing_type,
    fee,
    reservation_deposit_points,
    minimum_participants,
    minimum_check_at,
    status,
    created_at
) VALUES
-- 1번 행사: 부산 겨울 콘서트
(
    100,
    '1회차 - 오후 공연',
    1,
    '2026-11-10 00:00:00',
    '2026-12-10 09:00:00',
    '2026-12-20 14:00:00',
    '2026-12-20 16:00:00',
    0, 1, 300, 'PAID', 25000, 0, 30,
    '2026-12-15 09:00:00',
    'OPEN',
    NOW()
),
(
    100,
    '2회차 - 저녁 공연',
    1,
    '2026-11-10 00:00:00',
    '2026-12-10 09:00:00',
    '2026-12-20 19:00:00',
    '2026-12-20 21:00:00',
    0, 1, 300, 'PAID', 30000, 0, 30,
    '2026-12-15 09:00:00',
    'OPEN',
    NOW()
),

-- 2번 행사: 부산 푸드 페스티벌
(
    200,
    '1회차 - 오전 입장',
    2,
    '2026-11-15 00:00:00',
    '2026-12-15 09:00:00',
    '2026-12-25 10:00:00',
    '2026-12-25 13:00:00',
    0, 1, 300, 'PAID', 10000, 0, 50,
    '2026-12-20 09:00:00',
    'OPEN',
    NOW()
),
(
    200,
    '2회차 - 오후 입장',
    2,
    '2026-11-15 00:00:00',
    '2026-12-15 09:00:00',
    '2026-12-25 14:00:00',
    '2026-12-25 17:00:00',
    0, 1, 300, 'PAID', 15000, 0, 50,
    '2026-12-20 09:00:00',
    'OPEN',
    NOW()
),

-- 3번 행사: 부산 문화 예술 전시회
(
    80,
    '1회차 - 오전 관람',
    3,
    '2026-12-01 00:00:00',
    '2026-12-20 09:00:00',
    '2026-12-28 10:00:00',
    '2026-12-28 12:00:00',
    0, 1, 300, 'FREE', 0, 0, 10,
    '2026-12-23 09:00:00',
    'PENDING',
    NOW()
),
(
    80,
    '2회차 - 오후 관람',
    3,
    '2026-12-01 00:00:00',
    '2026-12-20 09:00:00',
    '2026-12-28 14:00:00',
    '2026-12-28 16:00:00',
    0, 1, 300, 'FREE', 0, 0, 10,
    '2026-12-23 09:00:00',
    'PENDING',
    NOW()
);



