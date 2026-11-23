-- H2 local showcase only. Existing records are preserved; reruns insert missing rows only.
-- Fictional event/venue content for filming. Dates are relative to the initial insertion.
-- Poster source: 1.png through 10.png; physical files must accompany this SQL.

-- 1. 바다가 머문 자리
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 수영구', '광안 바다사진 갤러리', '바다가 머문 자리', '부산의 바다와 도시가 남긴 순간을 사진으로 만나는 전시.', '새벽의 항구부터 해 질 무렵의 해변까지, 익숙한 부산의 풍경을 새로운 시선으로 담았습니다. 파도에 닳은 방파제와 골목 사이의 수평선을 따라 도시의 이야기를 만나보세요. 관람 후에는 마음에 남은 장면을 짧은 글로 기록할 수 있습니다.', 'PUBLISHED', TIMESTAMPADD(MINUTE, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -1, CURRENT_TIMESTAMP)
FROM company_tb c JOIN event_category_tb k ON k.name = '전시'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test'));

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT MIN(id) FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), '기본 회차', TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 15, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 15, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 15, CURRENT_DATE))), 80, 0, 1, 300, 'FREE', 0, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 15, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT MIN(id) FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT MIN(id) FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), 'files/5a0ca5e0-2026-4000-8000-000000000001.png'
WHERE (SELECT MIN(id) FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT MIN(id) FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 2. 빛의 조각, 도시의 밤
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 해운대구', '해운대 미디어아트 스튜디오', '빛의 조각, 도시의 밤', '빛과 색, 반사로 다시 그려낸 부산의 밤.', '항구의 불빛과 물결, 창문 너머의 일상을 모티브로 한 미디어아트 전시입니다. 관람객의 움직임에 따라 달라지는 조명과 반사 속에서 자신만의 빛의 순간을 찾아보세요.', 'PUBLISHED', TIMESTAMPADD(MINUTE, -2, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -2, CURRENT_TIMESTAMP)
FROM company_tb c JOIN event_category_tb k ON k.name = '전시'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test'));

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT MIN(id) FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), '기본 회차', TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 16, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 16, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 16, CURRENT_DATE))), 60, 0, 1, 300, 'PAID', 12000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 16, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT MIN(id) FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT MIN(id) FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), 'files/5a0ca5e0-2026-4000-8000-000000000002.png'
WHERE (SELECT MIN(id) FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT MIN(id) FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 3. 골목에 피어난 색
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 사하구', '감천 골목아트 공간', '골목에 피어난 색', '부산의 골목과 생활 풍경을 회화와 일러스트로 만납니다.', '오래된 계단과 작은 가게의 간판, 담벼락 아래 화분처럼 지나치기 쉬운 풍경을 모았습니다. 작가마다 다른 색과 선을 감상하고 나만의 골목 풍경을 엽서에 그려 가져가세요.', 'PUBLISHED', TIMESTAMPADD(MINUTE, -3, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -3, CURRENT_TIMESTAMP)
FROM company_tb c JOIN event_category_tb k ON k.name = '전시'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test'));

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT MIN(id) FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), '기본 회차', TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 17, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 17, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 17, CURRENT_DATE))), 50, 0, 1, 300, 'FREE', 0, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 17, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT MIN(id) FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT MIN(id) FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), 'files/5a0ca5e0-2026-4000-8000-000000000003.png'
WHERE (SELECT MIN(id) FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT MIN(id) FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 4. 선셋 웨이브
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 수영구', '광안 선셋 스테이지', '선셋 웨이브', '노을이 내려앉는 시간, 바다 곁에서 만나는 어쿠스틱 라이브.', '기타와 건반, 목소리가 어우러지는 작은 무대에 초대합니다. 편안한 어쿠스틱 음악부터 리듬감 있는 인디 팝까지, 저녁 바다와 어울리는 곡들을 들려드립니다.', 'PUBLISHED', TIMESTAMPADD(MINUTE, -4, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -4, CURRENT_TIMESTAMP)
FROM company_tb c JOIN event_category_tb k ON k.name = '공연'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test'));

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT MIN(id) FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), '기본 회차', TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 18, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 18, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 18, CURRENT_DATE))), 100, 0, 1, 300, 'PAID', 15000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 18, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT MIN(id) FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT MIN(id) FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), 'files/5a0ca5e0-2026-4000-8000-000000000004.png'
WHERE (SELECT MIN(id) FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT MIN(id) FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 5. 항구의 재즈 나잇
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 동구', '북항 재즈 라운지', '항구의 재즈 나잇', '항구의 밤을 채우는 재즈 트리오의 라이브 공연.', '피아노와 콘트라베이스, 드럼이 만드는 재즈의 매력을 전합니다. 익숙한 스탠더드와 영화 속 멜로디를 재해석한 연주, 곡 사이의 짧은 이야기와 함께 여유로운 밤을 보내세요.', 'PUBLISHED', TIMESTAMPADD(MINUTE, -5, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -5, CURRENT_TIMESTAMP)
FROM company_tb c JOIN event_category_tb k ON k.name = '공연'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test'));

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT MIN(id) FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), '기본 회차', TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 19, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 19, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 19, CURRENT_DATE))), 60, 0, 1, 300, 'PAID', 25000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 19, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT MIN(id) FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT MIN(id) FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), 'files/5a0ca5e0-2026-4000-8000-000000000005.png'
WHERE (SELECT MIN(id) FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT MIN(id) FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 6. 골목극장: 오늘의 안부
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 남구', '대연 골목극장', '골목극장: 오늘의 안부', '평범한 하루의 작은 만남을 담은 소극장 연극.', '동네 세탁소에서 우연히 마주친 사람들이 서로의 하루를 묻기 시작합니다. 가볍게 건넨 인사가 위로가 되는 이야기. 가까운 거리에서 배우들의 표정과 호흡을 느끼며 웃음과 공감의 시간을 만나보세요.', 'PUBLISHED', TIMESTAMPADD(MINUTE, -6, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -6, CURRENT_TIMESTAMP)
FROM company_tb c JOIN event_category_tb k ON k.name = '공연'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test'));

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT MIN(id) FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), '기본 회차', TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 20, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 20, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 20, CURRENT_DATE))), 40, 0, 1, 300, 'PAID', 18000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 20, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT MIN(id) FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT MIN(id) FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), 'files/5a0ca5e0-2026-4000-8000-000000000006.png'
WHERE (SELECT MIN(id) FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT MIN(id) FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 7. 부산 골목마켓
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 부산진구', '전포 골목문화광장', '부산 골목마켓', '로컬 브랜드와 창작자의 취향이 모이는 주말 마켓.', '직접 만든 소품과 독립출판물, 일상에 즐거움을 더하는 생활용품을 만나보세요. 창작자의 제작 이야기를 듣고 간단한 체험과 거리 공연을 즐기며 나만의 발견을 가져가세요.', 'PUBLISHED', TIMESTAMPADD(MINUTE, -7, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -7, CURRENT_TIMESTAMP)
FROM company_tb c JOIN event_category_tb k ON k.name = '축제'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test'));

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT MIN(id) FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), '기본 회차', TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 21, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 21, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 21, CURRENT_DATE))), 200, 0, 1, 300, 'FREE', 0, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 21, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT MIN(id) FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT MIN(id) FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), 'files/5a0ca5e0-2026-4000-8000-000000000007.png'
WHERE (SELECT MIN(id) FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT MIN(id) FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 8. 파도빛 야간문화제
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 해운대구', '송정 바다산책 문화공간', '파도빛 야간문화제', '바다의 색을 담은 조명과 음악이 어우러지는 저녁 문화축제.', '해가 지면 산책길을 따라 빛의 작품들이 모습을 드러냅니다. 파도를 모티브로 한 조명 설치와 작은 음악 공연, 함께 완성하는 빛 메시지 공간에서 해변의 밤을 즐겨보세요.', 'PUBLISHED', TIMESTAMPADD(MINUTE, -8, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -8, CURRENT_TIMESTAMP)
FROM company_tb c JOIN event_category_tb k ON k.name = '축제'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test'));

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT MIN(id) FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), '기본 회차', TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 22, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 22, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 22, CURRENT_DATE))), 200, 0, 1, 300, 'FREE', 0, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 22, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT MIN(id) FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT MIN(id) FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), 'files/5a0ca5e0-2026-4000-8000-000000000008.png'
WHERE (SELECT MIN(id) FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT MIN(id) FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 9. 영도 필름 산책
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 영도구', '영도 사진산책 작업실', '영도 필름 산책', '골목과 바다를 천천히 바라보며 배우는 필름 감성 사진 워크숍.', '사진의 기본 구도와 빛을 읽는 방법을 익힌 뒤 영도의 골목과 해안 풍경을 함께 걸으며 촬영합니다. 스마트폰으로도 참여할 수 있으며, 산책 후에는 각자의 사진을 함께 보며 이야기를 나눕니다.', 'PUBLISHED', TIMESTAMPADD(MINUTE, -9, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -9, CURRENT_TIMESTAMP)
FROM company_tb c JOIN event_category_tb k ON k.name = '체험'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test'));

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT MIN(id) FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), '기본 회차', TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 23, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 23, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 23, CURRENT_DATE))), 15, 0, 1, 300, 'PAID', 20000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 23, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT MIN(id) FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT MIN(id) FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), 'files/5a0ca5e0-2026-4000-8000-000000000009.png'
WHERE (SELECT MIN(id) FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT MIN(id) FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 10. 바다색 도자기 공방
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 기장군', '기장 바다색 도자공방', '바다색 도자기 공방', '부산의 바다에서 고른 색으로 나만의 도자기 접시를 만듭니다.', '흙을 손으로 다듬어 작은 접시를 빚고 파도와 모래에서 떠올린 무늬를 더해보세요. 도구 사용부터 채색까지 안내합니다. 완성한 작품은 건조와 소성 후 수령하며 일정과 방법은 현장에서 안내합니다.', 'PUBLISHED', TIMESTAMPADD(MINUTE, -10, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -10, CURRENT_TIMESTAMP)
FROM company_tb c JOIN event_category_tb k ON k.name = '체험'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test'));

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT MIN(id) FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), '기본 회차', TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 24, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 24, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 24, CURRENT_DATE))), 12, 0, 1, 300, 'PAID', 35000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 24, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT MIN(id) FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT MIN(id) FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')), 'files/5a0ca5e0-2026-4000-8000-000000000010.png'
WHERE (SELECT MIN(id) FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT MIN(id) FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT MIN(id) FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test')) AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- Second page copies: independent files, same visible content.
-- 11. 바다가 머문 자리
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 수영구', '광안 바다사진 갤러리', '바다가 머문 자리', '부산의 바다와 도시가 남긴 순간을 사진으로 만나는 전시.', '새벽의 항구부터 해 질 무렵의 해변까지, 익숙한 부산의 풍경을 새로운 시선으로 담았습니다. 파도에 닳은 방파제와 골목 사이의 수평선을 따라 도시의 이야기를 만나보세요. 관람 후에는 마음에 남은 장면을 짧은 글로 기록할 수 있습니다.', 'PUBLISHED', TIMESTAMP '2026-11-01 12:00:01', TIMESTAMP '2026-11-01 12:00:01'
FROM company_tb c JOIN event_category_tb k ON k.name = '전시'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:01');

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT id FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:01'), '기본 회차', TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 15, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 15, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 15, CURRENT_DATE))), 80, 0, 1, 300, 'FREE', 0, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 15, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT id FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:01') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT id FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:01'));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT id FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:01'), 'files/5a0ca5e0-2026-4000-8000-000000000011.png'
WHERE (SELECT id FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:01') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT id FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:01'));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT id FROM events WHERE title = '바다가 머문 자리' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:01') AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 12. 빛의 조각, 도시의 밤
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 해운대구', '해운대 미디어아트 스튜디오', '빛의 조각, 도시의 밤', '빛과 색, 반사로 다시 그려낸 부산의 밤.', '항구의 불빛과 물결, 창문 너머의 일상을 모티브로 한 미디어아트 전시입니다. 관람객의 움직임에 따라 달라지는 조명과 반사 속에서 자신만의 빛의 순간을 찾아보세요.', 'PUBLISHED', TIMESTAMP '2026-11-01 12:00:02', TIMESTAMP '2026-11-01 12:00:02'
FROM company_tb c JOIN event_category_tb k ON k.name = '전시'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:02');

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT id FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:02'), '기본 회차', TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 16, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 16, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 16, CURRENT_DATE))), 60, 0, 1, 300, 'PAID', 12000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 16, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT id FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:02') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT id FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:02'));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT id FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:02'), 'files/5a0ca5e0-2026-4000-8000-000000000012.png'
WHERE (SELECT id FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:02') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT id FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:02'));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT id FROM events WHERE title = '빛의 조각, 도시의 밤' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:02') AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 13. 골목에 피어난 색
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 사하구', '감천 골목아트 공간', '골목에 피어난 색', '부산의 골목과 생활 풍경을 회화와 일러스트로 만납니다.', '오래된 계단과 작은 가게의 간판, 담벼락 아래 화분처럼 지나치기 쉬운 풍경을 모았습니다. 작가마다 다른 색과 선을 감상하고 나만의 골목 풍경을 엽서에 그려 가져가세요.', 'PUBLISHED', TIMESTAMP '2026-11-01 12:00:03', TIMESTAMP '2026-11-01 12:00:03'
FROM company_tb c JOIN event_category_tb k ON k.name = '전시'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:03');

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT id FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:03'), '기본 회차', TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 17, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 17, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 17, CURRENT_DATE))), 50, 0, 1, 300, 'FREE', 0, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 17, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT id FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:03') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT id FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:03'));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT id FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:03'), 'files/5a0ca5e0-2026-4000-8000-000000000013.png'
WHERE (SELECT id FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:03') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT id FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:03'));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT id FROM events WHERE title = '골목에 피어난 색' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:03') AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 14. 선셋 웨이브
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 수영구', '광안 선셋 스테이지', '선셋 웨이브', '노을이 내려앉는 시간, 바다 곁에서 만나는 어쿠스틱 라이브.', '기타와 건반, 목소리가 어우러지는 작은 무대에 초대합니다. 편안한 어쿠스틱 음악부터 리듬감 있는 인디 팝까지, 저녁 바다와 어울리는 곡들을 들려드립니다.', 'PUBLISHED', TIMESTAMP '2026-11-01 12:00:04', TIMESTAMP '2026-11-01 12:00:04'
FROM company_tb c JOIN event_category_tb k ON k.name = '공연'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:04');

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT id FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:04'), '기본 회차', TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 18, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 18, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 18, CURRENT_DATE))), 100, 0, 1, 300, 'PAID', 15000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 18, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT id FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:04') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT id FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:04'));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT id FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:04'), 'files/5a0ca5e0-2026-4000-8000-000000000014.png'
WHERE (SELECT id FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:04') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT id FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:04'));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT id FROM events WHERE title = '선셋 웨이브' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:04') AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 15. 항구의 재즈 나잇
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 동구', '북항 재즈 라운지', '항구의 재즈 나잇', '항구의 밤을 채우는 재즈 트리오의 라이브 공연.', '피아노와 콘트라베이스, 드럼이 만드는 재즈의 매력을 전합니다. 익숙한 스탠더드와 영화 속 멜로디를 재해석한 연주, 곡 사이의 짧은 이야기와 함께 여유로운 밤을 보내세요.', 'PUBLISHED', TIMESTAMP '2026-11-01 12:00:05', TIMESTAMP '2026-11-01 12:00:05'
FROM company_tb c JOIN event_category_tb k ON k.name = '공연'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:05');

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT id FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:05'), '기본 회차', TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 19, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 19, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 19, CURRENT_DATE))), 60, 0, 1, 300, 'PAID', 25000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 19, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT id FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:05') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT id FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:05'));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT id FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:05'), 'files/5a0ca5e0-2026-4000-8000-000000000015.png'
WHERE (SELECT id FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:05') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT id FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:05'));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT id FROM events WHERE title = '항구의 재즈 나잇' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:05') AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 16. 골목극장: 오늘의 안부
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 남구', '대연 골목극장', '골목극장: 오늘의 안부', '평범한 하루의 작은 만남을 담은 소극장 연극.', '동네 세탁소에서 우연히 마주친 사람들이 서로의 하루를 묻기 시작합니다. 가볍게 건넨 인사가 위로가 되는 이야기. 가까운 거리에서 배우들의 표정과 호흡을 느끼며 웃음과 공감의 시간을 만나보세요.', 'PUBLISHED', TIMESTAMP '2026-11-01 12:00:06', TIMESTAMP '2026-11-01 12:00:06'
FROM company_tb c JOIN event_category_tb k ON k.name = '공연'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:06');

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT id FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:06'), '기본 회차', TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 20, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 20, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 20, CURRENT_DATE))), 40, 0, 1, 300, 'PAID', 18000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 20, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT id FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:06') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT id FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:06'));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT id FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:06'), 'files/5a0ca5e0-2026-4000-8000-000000000016.png'
WHERE (SELECT id FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:06') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT id FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:06'));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT id FROM events WHERE title = '골목극장: 오늘의 안부' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:06') AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 17. 부산 골목마켓
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 부산진구', '전포 골목문화광장', '부산 골목마켓', '로컬 브랜드와 창작자의 취향이 모이는 주말 마켓.', '직접 만든 소품과 독립출판물, 일상에 즐거움을 더하는 생활용품을 만나보세요. 창작자의 제작 이야기를 듣고 간단한 체험과 거리 공연을 즐기며 나만의 발견을 가져가세요.', 'PUBLISHED', TIMESTAMP '2026-11-01 12:00:07', TIMESTAMP '2026-11-01 12:00:07'
FROM company_tb c JOIN event_category_tb k ON k.name = '축제'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:07');

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT id FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:07'), '기본 회차', TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 21, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 21, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 21, CURRENT_DATE))), 200, 0, 1, 300, 'FREE', 0, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 21, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT id FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:07') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT id FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:07'));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT id FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:07'), 'files/5a0ca5e0-2026-4000-8000-000000000017.png'
WHERE (SELECT id FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:07') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT id FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:07'));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT id FROM events WHERE title = '부산 골목마켓' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:07') AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 18. 파도빛 야간문화제
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 해운대구', '송정 바다산책 문화공간', '파도빛 야간문화제', '바다의 색을 담은 조명과 음악이 어우러지는 저녁 문화축제.', '해가 지면 산책길을 따라 빛의 작품들이 모습을 드러냅니다. 파도를 모티브로 한 조명 설치와 작은 음악 공연, 함께 완성하는 빛 메시지 공간에서 해변의 밤을 즐겨보세요.', 'PUBLISHED', TIMESTAMP '2026-11-01 12:00:08', TIMESTAMP '2026-11-01 12:00:08'
FROM company_tb c JOIN event_category_tb k ON k.name = '축제'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:08');

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT id FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:08'), '기본 회차', TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 22, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 22, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 22, CURRENT_DATE))), 200, 0, 1, 300, 'FREE', 0, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 18, TIMESTAMPADD(DAY, 22, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT id FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:08') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT id FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:08'));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT id FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:08'), 'files/5a0ca5e0-2026-4000-8000-000000000018.png'
WHERE (SELECT id FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:08') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT id FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:08'));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT id FROM events WHERE title = '파도빛 야간문화제' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:08') AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 19. 영도 필름 산책
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 영도구', '영도 사진산책 작업실', '영도 필름 산책', '골목과 바다를 천천히 바라보며 배우는 필름 감성 사진 워크숍.', '사진의 기본 구도와 빛을 읽는 방법을 익힌 뒤 영도의 골목과 해안 풍경을 함께 걸으며 촬영합니다. 스마트폰으로도 참여할 수 있으며, 산책 후에는 각자의 사진을 함께 보며 이야기를 나눕니다.', 'PUBLISHED', TIMESTAMP '2026-11-01 12:00:09', TIMESTAMP '2026-11-01 12:00:09'
FROM company_tb c JOIN event_category_tb k ON k.name = '체험'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:09');

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT id FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:09'), '기본 회차', TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 23, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 23, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 23, CURRENT_DATE))), 15, 0, 1, 300, 'PAID', 20000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 23, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT id FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:09') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT id FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:09'));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT id FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:09'), 'files/5a0ca5e0-2026-4000-8000-000000000019.png'
WHERE (SELECT id FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:09') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT id FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:09'));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT id FROM events WHERE title = '영도 필름 산책' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:09') AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);

-- 20. 바다색 도자기 공방
INSERT INTO events (company_id, category_id, address, place_name, title, summary, description, status, published_at, created_at)
SELECT c.id, k.id, '부산광역시 기장군', '기장 바다색 도자공방', '바다색 도자기 공방', '부산의 바다에서 고른 색으로 나만의 도자기 접시를 만듭니다.', '흙을 손으로 다듬어 작은 접시를 빚고 파도와 모래에서 떠올린 무늬를 더해보세요. 도구 사용부터 채색까지 안내합니다. 완성한 작품은 건조와 소성 후 수령하며 일정과 방법은 현장에서 안내합니다.', 'PUBLISHED', TIMESTAMP '2026-11-01 12:00:10', TIMESTAMP '2026-11-01 12:00:10'
FROM company_tb c JOIN event_category_tb k ON k.name = '체험'
WHERE c.email = 'company@example.test' AND NOT EXISTS (SELECT 1 FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:10');

INSERT INTO event_sessions (event_id, name, starts_at, ends_at, apply_start_at, apply_end_at, capacity, occupied_count, next_reception_number, hold_duration_seconds, pricing_type, fee, reservation_deposit_points, minimum_participants, minimum_check_at, status, created_at)
SELECT (SELECT id FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:10'), '기본 회차', TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 24, CURRENT_DATE)), TIMESTAMPADD(HOUR, 3, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 24, CURRENT_DATE))), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 24, CURRENT_DATE))), 12, 0, 1, 300, 'PAID', 35000, 0, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, 14, TIMESTAMPADD(DAY, 24, CURRENT_DATE))), 'SCHEDULED', CURRENT_TIMESTAMP
WHERE (SELECT id FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:10') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_sessions WHERE event_id = (SELECT id FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:10'));

INSERT INTO event_files (event_id, storage_key)
SELECT (SELECT id FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:10'), 'files/5a0ca5e0-2026-4000-8000-000000000020.png'
WHERE (SELECT id FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:10') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_files WHERE event_id = (SELECT id FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:10'));

INSERT INTO deposit_tb (company_id, event_session_id, status, amount, paid_at)
SELECT e.company_id, s.id, 'PAID', 150000, CURRENT_TIMESTAMP
FROM events e JOIN event_sessions s ON s.event_id = e.id
WHERE e.id = (SELECT id FROM events WHERE title = '바다색 도자기 공방' AND company_id = (SELECT id FROM company_tb WHERE email = 'company@example.test') AND created_at = TIMESTAMP '2026-11-01 12:00:10') AND NOT EXISTS (SELECT 1 FROM deposit_tb d WHERE d.event_session_id = s.id);
