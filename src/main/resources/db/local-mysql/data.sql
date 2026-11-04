------------------------------ event테이블----------------------------------------
INSERT INTO event_tb(title, status, description, starts_at, ends_at, canceled_at)
VALUES
    ('부산 불꽃축제', '개최 확정', '광안리 해수욕장에서 열리는 불꽃축제',
     '2026-11-20 19:00:00', '2026-11-20 22:00:00', NULL),

    ('부산 국제 영화제', '승인대기중', '국내외 다양한 영화를 감상하는 영화제',
     '2026-12-05 10:00:00', '2026-12-10 22:00:00', NULL),

    ('해운대 음악 페스티벌', '개최 확정', '해운대에서 진행되는 야외 음악 공연',
     '2026-12-15 18:00:00', '2026-12-15 23:00:00', NULL),

    ('부산 크리스마스 축제', '취소', '크리스마스를 기념하는 겨울 축제',
     '2026-12-24 17:00:00', '2026-12-25 22:00:00', '2026-11-01 12:00:00'),

    ('광안리 드론 라이트쇼', '개최 확정', '광안리 해변에서 펼쳐지는 드론 공연',
     '2026-12-12 19:00:00', '2026-12-12 21:00:00', NULL),

    ('부산 겨울 바다축제', '승인대기중', '겨울 바다를 배경으로 진행되는 체험형 축제',
     '2027-01-15 10:00:00', '2027-01-17 18:00:00', NULL);

------------------------------ Company테이블----------------------------------------
-- Company 테이블: 개발용 더미 데이터
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
    '1234',
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
    '1234',
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
    '1234',
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
    '1234',
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
    '1234',
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
INSERT INTO user_tb(display_name, role, email_normalized, password_hash, status, preferred_locale, email_verified_at, created_at)
VALUES
    ('관리자', 'ADMIN', 'admin@busan.com', '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('김민수', 'MEMBER', 'minsu@naver.com', '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('이서연', 'MEMBER', 'seoyeon@naver.com', '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('박지훈', 'COMPANY', 'jihun@naver.com', '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('최유진', 'MEMBER', 'yujin@naver.com', '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('정현우', 'COMPANY', 'hyunwoo@naver.com', '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G', 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
