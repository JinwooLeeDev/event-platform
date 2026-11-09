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



------------------------------ event테이블----------------------------------------

INSERT INTO event_tb
(title, status, description, starts_at, ends_at, canceled_at)
VALUES
    (
        '부산 오션 뮤직 페스티벌',
        'OPEN',
        '부산에서 진행되는 야외 음악 축제',
        '2026-12-05 18:00:00',
        '2026-12-05 22:00:00',
        NULL
    ),
    (
        '해운대 문화 예술 축제',
        'OPEN',
        '다양한 공연과 체험 프로그램을 제공하는 행사',
        '2026-12-12 14:00:00',
        '2026-12-12 20:00:00',
        NULL
    );


------------------------------ eventSession테이블----------------------------------------

INSERT INTO event_sessions
(event_id, fee)
VALUES
    (1, 25000),
    (1, 15000),
    (2, 10000);



