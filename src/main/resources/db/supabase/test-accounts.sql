-- Supabase/PostgreSQL development test accounts. Password: 1234 (BCrypt).
-- Existing accounts are preserved; generated identity values are not overridden.
-- MEMBER: member@example.test / ADMIN: admin@example.test / COMPANY: company@example.test

INSERT INTO users (
    email_normalized, password_hash, display_name, role, point, status,
    preferred_locale, email_verified_at, created_at
) VALUES
    ('member@example.test', '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
     '테스트일반회원', 'MEMBER', 1000000, 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('admin@example.test', '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
     '테스트운영자', 'ADMIN', 0, 'ACTIVE', 'ko', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email_normalized) DO NOTHING;

INSERT INTO company_tb (
    name, email, password, business_number, representative_name, opened_on,
    address, contact_person_name, contact_person_phone, status, point,
    business_verification_status, email_verified_at, created_at
) VALUES (
    '테스트기업', 'company@example.test',
    '$2a$10$qqSYOXRVGAbe.uzf/0lAI.M.2i2japKDA7x/t05IFOrF0DkyuIs1G',
    '9990000001', '테스트대표', DATE '2020-01-01',
    '부산광역시 해운대구 APEC로 55', '테스트담당자', '010-0000-0001',
    'APPROVED', 1000000, 'MATCHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;
