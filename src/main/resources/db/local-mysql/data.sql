------------------------------ Company테이블----------------------------------------
-- Company 테이블: 개발용 더미 데이터
-- 로컬 확인용 비밀번호는 1234이며 BCrypt 해시로 저장합니다.
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
    point,
    created_at
)
VALUES
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
    'APPROVED',
    500000,
    CURRENT_TIMESTAMP
),
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
    100000,
    CURRENT_TIMESTAMP
),
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
    300000,
    CURRENT_TIMESTAMP
),
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
    'APPROVED',
    400000,
    CURRENT_TIMESTAMP
),
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
    'PENDING',
    150000,
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




-- 추가 행사 임시 데이터: 기존 행사 1~3 및 연결 회차는 유지합니다.
-- 신규 행사는 회사 이메일로 연결하고, 모집 기간은 실행일 기준으로 설정합니다.
-- 실제 행사/이미지가 아닌 개발용 예시입니다. 대표 이미지와 회차는 별도 등록이 필요합니다.

INSERT INTO events (company_id, category_id, address, title, summary, status, description,
    apply_start_at, apply_end_at, published_at, cancelled_at, cancellation_reason, feedback_message, created_at)
SELECT c.id, k.id, '부산시립미술관', '부산 미디어아트 특별전', '빛과 색으로 만나는 부산의 새로운 모습', 'PUBLISHED', '빛과 색으로 만나는 부산의 새로운 모습 (개발용 임시 데이터)',
    TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 14, CURRENT_TIMESTAMP),
    TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP),
    NULL, NULL,
    NULL, TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP)
FROM company_tb c CROSS JOIN event_category_tb k
WHERE c.email = 'company1@example.test' AND k.id = 3
    AND NOT EXISTS (SELECT 1 FROM events e WHERE e.title = '부산 미디어아트 특별전' AND e.company_id = c.id);

INSERT INTO events (company_id, category_id, address, title, summary, status, description,
    apply_start_at, apply_end_at, published_at, cancelled_at, cancellation_reason, feedback_message, created_at)
SELECT c.id, k.id, '광안리 해변', '광안리 문화 산책', '바다 곁에서 즐기는 문화 산책 프로그램', 'PUBLISHED', '바다 곁에서 즐기는 문화 산책 프로그램 (개발용 임시 데이터)',
    TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 10, CURRENT_TIMESTAMP),
    TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP),
    NULL, NULL,
    NULL, TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP)
FROM company_tb c CROSS JOIN event_category_tb k
WHERE c.email = 'company4@example.test' AND k.id = 3
    AND NOT EXISTS (SELECT 1 FROM events e WHERE e.title = '광안리 문화 산책' AND e.company_id = c.id);

INSERT INTO events (company_id, category_id, address, title, summary, status, description,
    apply_start_at, apply_end_at, published_at, cancelled_at, cancellation_reason, feedback_message, created_at)
SELECT c.id, k.id, 'F1963', '부산 독립출판 북페어', '지역 작가와 독립출판물을 만나는 자리', 'PUBLISHED', '지역 작가와 독립출판물을 만나는 자리 (개발용 임시 데이터)',
    TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 20, CURRENT_TIMESTAMP),
    TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP),
    NULL, NULL,
    NULL, TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP)
FROM company_tb c CROSS JOIN event_category_tb k
WHERE c.email = 'company1@example.test' AND k.id = 3
    AND NOT EXISTS (SELECT 1 FROM events e WHERE e.title = '부산 독립출판 북페어' AND e.company_id = c.id);

INSERT INTO events (company_id, category_id, address, title, summary, status, description,
    apply_start_at, apply_end_at, published_at, cancelled_at, cancellation_reason, feedback_message, created_at)
SELECT c.id, k.id, '전포공방거리', '전포 로컬 크래프트 클래스', '부산의 감성을 담은 소품 만들기', 'PUBLISHED', '부산의 감성을 담은 소품 만들기 (개발용 임시 데이터)',
    TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 7, CURRENT_TIMESTAMP),
    TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP),
    NULL, NULL,
    NULL, TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP)
FROM company_tb c CROSS JOIN event_category_tb k
WHERE c.email = 'company4@example.test' AND k.id = 3
    AND NOT EXISTS (SELECT 1 FROM events e WHERE e.title = '전포 로컬 크래프트 클래스' AND e.company_id = c.id);

INSERT INTO events (company_id, category_id, address, title, summary, status, description,
    apply_start_at, apply_end_at, published_at, cancelled_at, cancellation_reason, feedback_message, created_at)
SELECT c.id, k.id, '영화의전당', '가을밤 야외 영화제', '별빛 아래에서 함께하는 영화 이야기', 'PUBLISHED', '별빛 아래에서 함께하는 영화 이야기 (개발용 임시 데이터)',
    TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 12, CURRENT_TIMESTAMP),
    TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP),
    NULL, NULL,
    NULL, TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP)
FROM company_tb c CROSS JOIN event_category_tb k
WHERE c.email = 'company3@example.test' AND k.id = 1
    AND NOT EXISTS (SELECT 1 FROM events e WHERE e.title = '가을밤 야외 영화제' AND e.company_id = c.id);

INSERT INTO events (company_id, category_id, address, title, summary, status, description,
    apply_start_at, apply_end_at, published_at, cancelled_at, cancellation_reason, feedback_message, created_at)
SELECT c.id, k.id, '부산시민공원', '부산 로컬 푸드 마켓', '지역의 맛과 브랜드를 만나는 주말 마켓', 'PUBLISHED', '지역의 맛과 브랜드를 만나는 주말 마켓 (개발용 임시 데이터)',
    TIMESTAMPADD(DAY, 3, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 21, CURRENT_TIMESTAMP),
    TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP),
    NULL, NULL,
    NULL, TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP)
FROM company_tb c CROSS JOIN event_category_tb k
WHERE c.email = 'company3@example.test' AND k.id = 2
    AND NOT EXISTS (SELECT 1 FROM events e WHERE e.title = '부산 로컬 푸드 마켓' AND e.company_id = c.id);

INSERT INTO events (company_id, category_id, address, title, summary, status, description,
    apply_start_at, apply_end_at, published_at, cancelled_at, cancellation_reason, feedback_message, created_at)
SELECT c.id, k.id, '전포공방거리', '겨울 공예 체험 보완 요청', '관리자 보완 요청 상태 확인용 행사', 'FEEDBACK_REQUIRED', '관리자 보완 요청 상태 확인용 행사 (개발용 임시 데이터)',
    TIMESTAMPADD(DAY, 5, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 25, CURRENT_TIMESTAMP),
    NULL,
    NULL, NULL,
    '행사 운영 계획을 보완해주세요.', TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP)
FROM company_tb c CROSS JOIN event_category_tb k
WHERE c.email = 'company4@example.test' AND k.id = 3
    AND NOT EXISTS (SELECT 1 FROM events e WHERE e.title = '겨울 공예 체험 보완 요청' AND e.company_id = c.id);

INSERT INTO events (company_id, category_id, address, title, summary, status, description,
    apply_start_at, apply_end_at, published_at, cancelled_at, cancellation_reason, feedback_message, created_at)
SELECT c.id, k.id, '부산문화회관', '지난 부산 재즈 공연', '종료된 행사 목록 확인용 공연', 'COMPLETED', '종료된 행사 목록 확인용 공연 (개발용 임시 데이터)',
    TIMESTAMPADD(DAY, -30, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -10, CURRENT_TIMESTAMP),
    TIMESTAMPADD(DAY, -35, CURRENT_TIMESTAMP),
    NULL, NULL,
    NULL, TIMESTAMPADD(DAY, -40, CURRENT_TIMESTAMP)
FROM company_tb c CROSS JOIN event_category_tb k
WHERE c.email = 'company3@example.test' AND k.id = 1
    AND NOT EXISTS (SELECT 1 FROM events e WHERE e.title = '지난 부산 재즈 공연' AND e.company_id = c.id);

INSERT INTO events (company_id, category_id, address, title, summary, status, description,
    apply_start_at, apply_end_at, published_at, cancelled_at, cancellation_reason, feedback_message, created_at)
SELECT c.id, k.id, '광안리 해변', '부산 해변 음악회 취소', '행사 취소 상태 확인용 음악회', 'CANCELLED', '행사 취소 상태 확인용 음악회 (개발용 임시 데이터)',
    TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 8, CURRENT_TIMESTAMP),
    TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP),
    CURRENT_TIMESTAMP, '기상 악화에 따른 취소 예시',
    NULL, TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP)
FROM company_tb c CROSS JOIN event_category_tb k
WHERE c.email = 'company4@example.test' AND k.id = 1
    AND NOT EXISTS (SELECT 1 FROM events e WHERE e.title = '부산 해변 음악회 취소' AND e.company_id = c.id);



INSERT INTO purchase_tb (
    user_id,
    company_id,
    event_session_id,
    price
)
SELECT
    u.id,
    NULL,
    s.id,
    COALESCE(s.fee, 0)
FROM users u
         CROSS JOIN event_sessions s
WHERE u.email_normalized = 'minsu@naver.com'
  AND s.id IN (
    SELECT selected_session.id
    FROM (
             SELECT id
             FROM event_sessions
             ORDER BY id
                 LIMIT 3
         ) selected_session
)
  AND NOT EXISTS (
    SELECT 1
    FROM purchase_tb p
    WHERE p.user_id = u.id
      AND p.event_session_id = s.id
);
