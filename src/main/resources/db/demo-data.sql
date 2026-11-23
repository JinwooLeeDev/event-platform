-- Minimal development data for PostgreSQL, MySQL and H2. Password: 1234 (BCrypt).
-- Only missing rows are inserted. Existing accounts, balances and categories are preserved.
-- MEMBER: member@example.test / ADMIN: admin@example.test / COMPANY: company@example.test

INSERT INTO users (
    email_normalized, password_hash, display_name, role, point, status,
    preferred_locale, email_verified_at, created_at
)
SELECT 'member@example.test', '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
       '테스트일반회원', 'MEMBER', 1000000, 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email_normalized = 'member@example.test');

INSERT INTO users (
    email_normalized, password_hash, display_name, role, point, status,
    preferred_locale, email_verified_at, created_at
)
SELECT 'admin@example.test', '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
       '테스트운영자', 'ADMIN', 0, 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email_normalized = 'admin@example.test');

INSERT INTO company_tb (
    name, email, password, business_number, representative_name, opened_on,
    address, contact_person_name, contact_person_phone, status, point,
    business_verification_status, email_verified_at, created_at
)
SELECT
    '테스트기업', 'company@example.test',
    '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
    '9990000001', '테스트대표', DATE '2020-01-01',
    '부산광역시 해운대구 APEC로 55', '테스트담당자', '010-0000-0001',
    'APPROVED', 1000000, 'MATCHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM company_tb WHERE email = 'company@example.test');

INSERT INTO event_category_tb (name)
SELECT '전시' WHERE NOT EXISTS (SELECT 1 FROM event_category_tb WHERE name = '전시');

INSERT INTO event_category_tb (name)
SELECT '공연' WHERE NOT EXISTS (SELECT 1 FROM event_category_tb WHERE name = '공연');

INSERT INTO event_category_tb (name)
SELECT '축제' WHERE NOT EXISTS (SELECT 1 FROM event_category_tb WHERE name = '축제');

INSERT INTO event_category_tb (name)
SELECT '체험' WHERE NOT EXISTS (SELECT 1 FROM event_category_tb WHERE name = '체험');
