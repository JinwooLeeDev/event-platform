-- LOCAL FIXTURES ONLY. Run through scripts/init-local-mysql.py.
-- Runner owns START TRANSACTION / COMMIT and the one-time marker.
-- All dates are UTC relative to the FIRST initialization, never refreshed on restart.
-- Policy numbers below are demo values, not decisions on unresolved P01/P02/P08/P09/P10.
SET @base = UTC_TIMESTAMP(6);
SET @policy = JSON_OBJECT('fixture', TRUE, 'version', 'local-v1', 'hold_seconds', 600,
 'reservation_points', 0, 'minimum_count', 1, 'reward_points', 3000);
-- Actual PBKDF2-HMAC-SHA256 digest for LocalDemo123!, 310000 iterations.
-- Fixture format only; no login API/PasswordEncoder is implemented yet.
SET @hash = 'pbkdf2_sha256:310000:6c6f63616c2d64656d6f2d6f6e6c79:d0535fd09c57107aba4836db9420e9f02ce4aab0f947cb737cf7a8353a5af8a9';

INSERT INTO users (id,email,password_hash,display_name,role,status,email_verified_at,created_at) VALUES
 (1,'admin@example.test',@hash,'로컬 관리자','ADMIN','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (2,'company1@example.test',@hash,'부산문화연구소','COMPANY','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (3,'company2@example.test',@hash,'해운대아트랩','COMPANY','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (4,'company3@example.test',@hash,'서면메이커스','COMPANY','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (5,'member01@example.test',@hash,'부산청년01','MEMBER','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (6,'member02@example.test',@hash,'부산청년02','MEMBER','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (7,'member03@example.test',@hash,'부산청년03','MEMBER','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (8,'member04@example.test',@hash,'부산청년04','MEMBER','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (9,'member05@example.test',@hash,'부산청년05','MEMBER','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (10,'member06@example.test',@hash,'부산청년06','MEMBER','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (11,'member07@example.test',@hash,'부산청년07','MEMBER','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (12,'member08@example.test',@hash,'Alex','MEMBER','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (13,'member09@example.test',@hash,'Yuki','MEMBER','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY),
 (14,'member10@example.test',@hash,'Lin','MEMBER','ACTIVE',@base-INTERVAL 60 DAY,@base-INTERVAL 60 DAY);

INSERT INTO email_verifications (email,purpose,code_hash,expires_at,verified_at,consumed_at,failure_count,sent_at,created_at)
SELECT email,'SIGNUP',SHA2(CONCAT('local-fixture-code:',id),256),
 created_at+INTERVAL 10 MINUTE,created_at,created_at,0,created_at,created_at FROM users;

INSERT INTO companies (id,user_id,business_number,name,representative_name,opened_on,contact_name,contact_phone,status,
 approved_at,business_check_status,business_checked_at,reviewer_id,reviewed_at,created_at) VALUES
 (1,2,'0000000001','부산문화연구소','가상대표1','2020-01-01','더미담당1','010-0000-0001','APPROVED',@base-INTERVAL 50 DAY,'MOCK_VALID',@base-INTERVAL 51 DAY,1,@base-INTERVAL 50 DAY,@base-INTERVAL 55 DAY),
 (2,3,'0000000002','해운대아트랩','가상대표2','2021-01-01','더미담당2','010-0000-0002','APPROVED',@base-INTERVAL 50 DAY,'MOCK_VALID',@base-INTERVAL 51 DAY,1,@base-INTERVAL 50 DAY,@base-INTERVAL 55 DAY),
 (3,4,'0000000003','서면메이커스','가상대표3','2022-01-01','더미담당3','010-0000-0003','APPROVED',@base-INTERVAL 50 DAY,'MOCK_VALID',@base-INTERVAL 51 DAY,1,@base-INTERVAL 50 DAY,@base-INTERVAL 55 DAY);

INSERT INTO events (id,company_id,title,description,address,venue,latitude,longitude,status,
 starts_at,ends_at,published_at,category_code,region_code,policy_snapshot,created_at) VALUES
 (1,1,'광안리 문화 팝업','진행 중인 유료 문화 체험 더미 행사','부산 수영구 광안해변로','더미 문화공간',35.1532000,129.1185000,'PUBLISHED',@base-INTERVAL 1 HOUR,@base+INTERVAL 1 HOUR,@base-INTERVAL 20 DAY,'POPUP','SUYEONG',@policy,@base-INTERVAL 30 DAY),
 (2,2,'해운대 청년 미술 전시','완료된 무료 전시 더미 행사','부산 해운대구 해운대로','더미 전시장',35.1631000,129.1635000,'COMPLETED',@base-INTERVAL 2 DAY,@base-INTERVAL 1 DAY,@base-INTERVAL 20 DAY,'EXHIBITION','HAEUNDAE',@policy,@base-INTERVAL 30 DAY),
 (3,3,'서면 개발자 특강','접수 중인 유료 특강 더미 행사','부산 부산진구 중앙대로','더미 세미나실',35.1579000,129.0595000,'PUBLISHED',@base+INTERVAL 7 DAY,@base+INTERVAL 7 DAY+INTERVAL 2 HOUR,@base-INTERVAL 20 DAY,'LECTURE','BUSANJIN',@policy,@base-INTERVAL 30 DAY),
 (4,1,'부산 로컬 문화 워크숍','임시 확보 상태 확인용 무료 행사','부산 수영구 수영로','더미 워크숍실',35.1532000,129.1185000,'PUBLISHED',@base+INTERVAL 10 DAY,@base+INTERVAL 10 DAY+INTERVAL 3 HOUR,@base-INTERVAL 20 DAY,'WORKSHOP','SUYEONG',@policy,@base-INTERVAL 30 DAY),
 (5,2,'해운대 야외 체험','취소 및 보증금 처리 확인용 행사','부산 해운대구 해운대해변로','더미 야외공간',35.1587000,129.1604000,'CANCELLED',@base+INTERVAL 5 DAY,@base+INTERVAL 5 DAY+INTERVAL 2 HOUR,@base-INTERVAL 20 DAY,'EXPERIENCE','HAEUNDAE',@policy,@base-INTERVAL 30 DAY);
UPDATE events SET cancelled_at=@base-INTERVAL 1 DAY,cancellation_reason='LOCAL_DEMO_CANCEL',cancel_job_status='COMPLETED' WHERE id=5;

INSERT INTO event_sessions (id,event_id,name,status,starts_at,ends_at,registration_opens_at,registration_closes_at,
 capacity,occupied_count,next_queue_number,hold_seconds,admission_points,reservation_points,minimum_count,created_at)
SELECT id,id,'기본 회차',IF(id=2,'COMPLETED',IF(id=5,'CANCELLED','SCHEDULED')),
 starts_at,ends_at,@base-INTERVAL 15 DAY,
 IF(id=1,@base-INTERVAL 2 HOUR,IF(id=2,@base-INTERVAL 3 DAY,starts_at-INTERVAL 1 DAY)),
 CASE id WHEN 1 THEN 2 ELSE 20 END,
 CASE id WHEN 1 THEN 2 WHEN 2 THEN 3 WHEN 3 THEN 1 WHEN 4 THEN 1 ELSE 0 END,
 CASE id WHEN 1 THEN 4 WHEN 2 THEN 4 WHEN 3 THEN 3 ELSE 2 END,
 600,CASE id WHEN 1 THEN 10000 WHEN 3 THEN 20000 ELSE 0 END,0,1,@base-INTERVAL 25 DAY
FROM events;

INSERT INTO event_reviews (id,event_id,round_no,reviewer_id,status,submitted_at,reviewed_at,created_at)
SELECT id,id,1,1,'APPROVED',@base-INTERVAL 22 DAY,@base-INTERVAL 20 DAY,@base-INTERVAL 22 DAY FROM events;

INSERT INTO registrations (id,session_id,user_id,status,attempt_no,queue_number,applied_at,hold_expires_at,
 confirmed_at,cancelled_at,cancellation_reason,policy_snapshot,created_at) VALUES
 (1,1,5,'CONFIRMED',1,1,@base-INTERVAL 4 DAY,NULL,@base-INTERVAL 4 DAY,NULL,NULL,@policy,@base-INTERVAL 4 DAY),
 (2,1,6,'CONFIRMED',1,2,@base-INTERVAL 4 DAY,NULL,@base-INTERVAL 4 DAY,NULL,NULL,@policy,@base-INTERVAL 4 DAY),
 (3,1,7,'WAITING',1,3,@base-INTERVAL 4 DAY,NULL,NULL,NULL,NULL,@policy,@base-INTERVAL 4 DAY),
 (4,2,8,'CONFIRMED',1,1,@base-INTERVAL 4 DAY,NULL,@base-INTERVAL 4 DAY,NULL,NULL,@policy,@base-INTERVAL 4 DAY),
 (5,3,9,'CANCELLED',1,1,@base-INTERVAL 4 DAY,NULL,@base-INTERVAL 4 DAY,@base-INTERVAL 2 DAY,'MEMBER_CANCEL',@policy,@base-INTERVAL 4 DAY),
 (6,4,10,'HOLD',1,1,@base,@base+INTERVAL 600 SECOND,NULL,NULL,NULL,@policy,@base),
 (7,5,11,'EXPIRED',1,1,@base-INTERVAL 3 DAY,@base-INTERVAL 3 DAY+INTERVAL 600 SECOND,NULL,NULL,NULL,@policy,@base-INTERVAL 3 DAY),
 (8,2,12,'CONFIRMED',1,2,@base-INTERVAL 4 DAY,NULL,@base-INTERVAL 4 DAY,NULL,NULL,@policy,@base-INTERVAL 4 DAY),
 (9,3,13,'CONFIRMED',1,2,@base-INTERVAL 4 DAY,NULL,@base-INTERVAL 4 DAY,NULL,NULL,@policy,@base-INTERVAL 4 DAY),
 (10,2,14,'CONFIRMED',1,3,@base-INTERVAL 4 DAY,NULL,@base-INTERVAL 4 DAY,NULL,NULL,@policy,@base-INTERVAL 4 DAY);

INSERT INTO registration_history (registration_id,attempt_no,from_status,to_status,actor_id,reason_code,occurred_at,created_at)
SELECT id,1,NULL,IF(status='WAITING','WAITING','HOLD'),user_id,'APPLY',applied_at,applied_at FROM registrations;
INSERT INTO registration_history (registration_id,attempt_no,from_status,to_status,actor_id,reason_code,occurred_at,created_at)
SELECT id,1,'HOLD','CONFIRMED',user_id,'CONFIRM',confirmed_at,confirmed_at FROM registrations WHERE confirmed_at IS NOT NULL;
INSERT INTO registration_history (registration_id,attempt_no,from_status,to_status,actor_id,reason_code,occurred_at,created_at)
SELECT id,1,'CONFIRMED','CANCELLED',user_id,'MEMBER_CANCEL',cancelled_at,cancelled_at FROM registrations WHERE status='CANCELLED';
INSERT INTO registration_history (registration_id,attempt_no,from_status,to_status,actor_id,reason_code,occurred_at,created_at)
SELECT id,1,'HOLD','EXPIRED',NULL,'HOLD_TIMEOUT',hold_expires_at,hold_expires_at FROM registrations WHERE status='EXPIRED';

INSERT INTO attendances (id,registration_id,attempt_no,checked_by,attended_at,created_at) VALUES
 (1,1,1,2,@base-INTERVAL 30 MINUTE,@base-INTERVAL 30 MINUTE),
 (2,4,1,3,@base-INTERVAL 2 DAY+INTERVAL 1 HOUR,@base-INTERVAL 2 DAY+INTERVAL 1 HOUR),
 (3,8,1,3,@base-INTERVAL 2 DAY+INTERVAL 1 HOUR,@base-INTERVAL 2 DAY+INTERVAL 1 HOUR),
 (4,10,1,3,@base-INTERVAL 2 DAY+INTERVAL 1 HOUR,@base-INTERVAL 2 DAY+INTERVAL 1 HOUR);

INSERT INTO point_accounts (id,code,kind,balance,status,created_at)
SELECT 1000+id,CONCAT('LOCAL_USER_',id),'USER_AVAILABLE',0,'ACTIVE',@base-INTERVAL 60 DAY FROM users;
INSERT INTO point_accounts (id,code,kind,balance,status,created_at) VALUES
 (2001,'LOCAL_DEPOSIT_ESCROW','DEPOSIT_ESCROW',0,'ACTIVE',@base-INTERVAL 60 DAY),
 (2002,'LOCAL_TICKET_ESCROW','TICKET_ESCROW',0,'ACTIVE',@base-INTERVAL 60 DAY),
 (2003,'LOCAL_PLATFORM_REVENUE','PLATFORM_REVENUE',0,'ACTIVE',@base-INTERVAL 60 DAY),
 (2004,'LOCAL_EXTERNAL_CLEARING','EXTERNAL_CLEARING',0,'ACTIVE',@base-INTERVAL 60 DAY);
INSERT INTO wallets (id,user_id,account_id,created_at) SELECT id,id,1000+id,@base-INTERVAL 60 DAY FROM users;

INSERT INTO point_transactions (id,transaction_key,type,actor_id,reason_code,posted_at,created_at)
SELECT id,CONCAT('local:topup:',id),'TOPUP',id,'LOCAL_DEMO_TOPUP',@base-INTERVAL 40 DAY,@base-INTERVAL 40 DAY FROM users;
INSERT INTO point_entries (transaction_id,account_id,delta,created_at)
SELECT id,1000+id,500000,@base-INTERVAL 40 DAY FROM users;
INSERT INTO point_entries (transaction_id,account_id,delta,created_at)
SELECT id,2004,-500000,@base-INTERVAL 40 DAY FROM users;
INSERT INTO topups (id,wallet_id,order_key,amount_krw,status,credited_transaction_id,expires_at,provider,provider_payment_id,verified_at,created_at)
SELECT id,id,CONCAT('local:topup-order:',id),500000,'PAID',id,@base-INTERVAL 39 DAY,'LOCAL_MOCK',
 CONCAT('local-payment-',id),@base-INTERVAL 40 DAY,@base-INTERVAL 40 DAY FROM users;

INSERT INTO point_transactions (id,transaction_key,type,actor_id,reason_code,posted_at,created_at)
SELECT 100+e.id,CONCAT('local:deposit:',e.id),'DEPOSIT',c.user_id,'LOCAL_DEMO_DEPOSIT',@base-INTERVAL 22 DAY,@base-INTERVAL 22 DAY
FROM events e JOIN companies c ON c.id=e.company_id;
INSERT INTO point_entries (transaction_id,account_id,delta,created_at)
SELECT 100+e.id,1000+c.user_id,-150000,@base-INTERVAL 22 DAY FROM events e JOIN companies c ON c.id=e.company_id;
INSERT INTO point_entries (transaction_id,account_id,delta,created_at)
SELECT 100+id,2001,150000,@base-INTERVAL 22 DAY FROM events;
INSERT INTO event_deposits (id,event_id,wallet_id,charge_transaction_id,amount_points,status,policy_snapshot,created_at)
SELECT e.id,e.id,c.user_id,100+e.id,150000,'HELD',@policy,@base-INTERVAL 22 DAY FROM events e JOIN companies c ON c.id=e.company_id;

INSERT INTO point_transactions (id,transaction_key,type,actor_id,reason_code,posted_at,created_at) VALUES
 (201,'local:charge:1','CHARGE',5,'ADMISSION',@base-INTERVAL 4 DAY,@base-INTERVAL 4 DAY),
 (202,'local:charge:2','CHARGE',6,'ADMISSION',@base-INTERVAL 4 DAY,@base-INTERVAL 4 DAY),
 (203,'local:charge:5','CHARGE',9,'ADMISSION',@base-INTERVAL 4 DAY,@base-INTERVAL 4 DAY),
 (204,'local:charge:9','CHARGE',13,'ADMISSION',@base-INTERVAL 4 DAY,@base-INTERVAL 4 DAY);
INSERT INTO point_entries (transaction_id,account_id,delta,created_at) VALUES
 (201,1005,-10000,@base-INTERVAL 4 DAY),(201,2002,10000,@base-INTERVAL 4 DAY),
 (202,1006,-10000,@base-INTERVAL 4 DAY),(202,2002,10000,@base-INTERVAL 4 DAY),
 (203,1009,-20000,@base-INTERVAL 4 DAY),(203,2002,20000,@base-INTERVAL 4 DAY),
 (204,1013,-20000,@base-INTERVAL 4 DAY),(204,2002,20000,@base-INTERVAL 4 DAY);
INSERT INTO registration_charges (id,registration_id,attempt_no,wallet_id,component,amount_points,transaction_id,status,policy_snapshot,created_at) VALUES
 (1,1,1,5,'ADMISSION',10000,201,'PAID',@policy,@base-INTERVAL 4 DAY),
 (2,2,1,6,'ADMISSION',10000,202,'PAID',@policy,@base-INTERVAL 4 DAY),
 (3,5,1,9,'ADMISSION',20000,203,'REFUNDED',@policy,@base-INTERVAL 4 DAY),
 (4,9,1,13,'ADMISSION',20000,204,'PAID',@policy,@base-INTERVAL 4 DAY);

-- A pending settlement avoids paying before a fixture's refund window closes.
INSERT INTO settlements (id,registration_charge_id,company_id,wallet_id,gross_points,refunded_points,fee_points,net_points,
 policy_snapshot,status,transaction_id,paid_at,created_at) VALUES
 (1,1,1,2,10000,0,1000,9000,JSON_OBJECT('fixture',TRUE,'fee_percent',10),'PENDING',NULL,NULL,@base);

INSERT INTO point_transactions (id,transaction_key,type,actor_id,reason_code,posted_at,created_at) VALUES
 (401,'local:reward:2','REWARD',3,'LOCAL_DEMO_PARTICIPATION',@base-INTERVAL 1 DAY,@base-INTERVAL 1 DAY);
INSERT INTO point_entries (transaction_id,account_id,delta,created_at) VALUES
 (401,1003,-3000,@base-INTERVAL 1 DAY),(401,1008,3000,@base-INTERVAL 1 DAY);
INSERT INTO participation_rewards (id,attendance_id,company_id,from_wallet_id,to_wallet_id,amount_points,transaction_id,policy_snapshot,created_at)
VALUES (1,2,2,3,8,3000,401,@policy,@base-INTERVAL 1 DAY);

INSERT INTO reviews (id,attendance_id,body,status,created_at) VALUES
 (1,1,'지역 문화를 체험하기 좋았어요. 로컬 개발용 후기입니다.','VISIBLE',@base-INTERVAL 20 MINUTE),
 (2,2,'다국어 안내가 편리했습니다. 더미 후기입니다.','VISIBLE',@base-INTERVAL 2 DAY+INTERVAL 2 HOUR);
INSERT INTO chat_messages (id,sender_id,client_message_key,kind,body,status,sent_at,event_id,created_at) VALUES
 (1,2,'local-notice-1','NOTICE','입구에서 출석 확인을 해주세요.','VISIBLE',@base-INTERVAL 3 HOUR,1,@base-INTERVAL 3 HOUR),
 (2,5,'local-chat-1','CHAT','행사장에 도착했습니다.','VISIBLE',@base-INTERVAL 45 MINUTE,1,@base-INTERVAL 45 MINUTE),
 (3,6,'local-chat-2','CHAT','안내 감사합니다!','VISIBLE',@base-INTERVAL 40 MINUTE,1,@base-INTERVAL 40 MINUTE);

INSERT INTO products (id,code,kind,name,status,unit_points,terms_snapshot,created_at) VALUES
 (1,'LOCAL_AD_BANNER','AD','더미 메인 배너','ACTIVE',5000,JSON_OBJECT('fixture',TRUE,'days',7),@base-INTERVAL 30 DAY),
 (2,'LOCAL_REVIEW_FEE','REVIEW_FEE','더미 별도 심사','ACTIVE',2000,JSON_OBJECT('fixture',TRUE,'refundable',TRUE),@base-INTERVAL 30 DAY);
INSERT INTO point_transactions (id,transaction_key,type,actor_id,reason_code,posted_at,created_at) VALUES
 (601,'local:product:1','CHARGE',2,'LOCAL_DEMO_AD',@base-INTERVAL 8 DAY,@base-INTERVAL 8 DAY),
 (602,'local:product:2','CHARGE',3,'LOCAL_DEMO_REVIEW_FEE',@base-INTERVAL 22 DAY,@base-INTERVAL 22 DAY);
INSERT INTO point_entries (transaction_id,account_id,delta,created_at) VALUES
 (601,1002,-5000,@base-INTERVAL 8 DAY),(601,2003,5000,@base-INTERVAL 8 DAY),
 (602,1003,-2000,@base-INTERVAL 22 DAY),(602,2003,2000,@base-INTERVAL 22 DAY);
INSERT INTO product_orders (id,company_id,wallet_id,event_id,event_review_id,order_key,quantity,unit_points,total_points,status,
 charge_transaction_id,product_id,terms_snapshot,created_at) VALUES
 (1,1,2,1,NULL,'local:product-order:1',1,5000,5000,'FULFILLED',601,1,JSON_OBJECT('fixture',TRUE,'days',7),@base-INTERVAL 8 DAY),
 (2,2,3,2,2,'local:product-order:2',1,2000,2000,'REFUNDED',602,2,JSON_OBJECT('fixture',TRUE,'refundable',TRUE),@base-INTERVAL 22 DAY);
INSERT INTO ad_campaigns (id,order_id,event_id,placement,priority,starts_at,ends_at,status,created_at) VALUES
 (1,1,1,'MAIN_BANNER',10,@base-INTERVAL 7 DAY,@base+INTERVAL 1 HOUR,'ACTIVE',@base-INTERVAL 8 DAY);

INSERT INTO point_transactions (id,transaction_key,type,original_transaction_id,actor_id,reason_code,posted_at,created_at) VALUES
 (301,'local:refund:registration:5','REFUND',203,9,'MEMBER_CANCEL',@base-INTERVAL 2 DAY,@base-INTERVAL 2 DAY),
 (302,'local:refund:product:2','REFUND',602,1,'LOCAL_DEMO_PRODUCT_CANCEL',@base-INTERVAL 19 DAY,@base-INTERVAL 19 DAY),
 (303,'local:refund:deposit:5','REFUND',105,1,'LOCAL_DEMO_EVENT_CANCEL',@base-INTERVAL 1 DAY,@base-INTERVAL 1 DAY),
 (304,'local:forfeiture:deposit:5','FEE',105,1,'LOCAL_DEMO_FORFEITURE',@base-INTERVAL 1 DAY,@base-INTERVAL 1 DAY);
INSERT INTO point_entries (transaction_id,account_id,delta,created_at) VALUES
 (301,2002,-20000,@base-INTERVAL 2 DAY),(301,1009,20000,@base-INTERVAL 2 DAY),
 (302,2003,-2000,@base-INTERVAL 19 DAY),(302,1003,2000,@base-INTERVAL 19 DAY),
 (303,2001,-50000,@base-INTERVAL 1 DAY),(303,1003,50000,@base-INTERVAL 1 DAY),
 (304,2001,-100000,@base-INTERVAL 1 DAY),(304,2003,100000,@base-INTERVAL 1 DAY);
INSERT INTO point_refunds (id,refund_key,wallet_id,deposit_id,registration_charge_id,product_order_id,
 original_transaction_id,refund_transaction_id,amount_points,reason_code,status,policy_snapshot,created_at) VALUES
 (1,'local:refund:registration:5',9,NULL,3,NULL,203,301,20000,'MEMBER_CANCEL','SUCCEEDED',@policy,@base-INTERVAL 2 DAY),
 (2,'local:refund:product:2',3,NULL,NULL,2,602,302,2000,'LOCAL_DEMO_PRODUCT_CANCEL','SUCCEEDED',@policy,@base-INTERVAL 19 DAY),
 (3,'local:refund:deposit:5',3,5,NULL,NULL,105,303,50000,'LOCAL_DEMO_EVENT_CANCEL','SUCCEEDED',@policy,@base-INTERVAL 1 DAY);
UPDATE event_deposits SET status='FORFEITED',forfeiture_transaction_id=304 WHERE id=5;

-- Derive balance caches from the actual balanced entries, never unrelated constants.
UPDATE point_accounts a LEFT JOIN (SELECT account_id,SUM(delta) total FROM point_entries GROUP BY account_id) e
 ON e.account_id=a.id SET a.balance=COALESCE(e.total,0);

-- No sendable pending email fixtures: future worker implementations must not send mail.
INSERT INTO notifications (id,recipient_id,channel,destination_snapshot,content_snapshot,status,attempt_count,event_key,created_at) VALUES
 (1,5,'INAPP',NULL,JSON_OBJECT('fixture',TRUE,'title','참가 확정','body','광안리 문화 팝업 신청이 확정되었습니다.'),'AVAILABLE',0,'local:registration:1:1:confirmed',@base-INTERVAL 4 DAY),
 (2,5,'EMAIL','member01@example.test',JSON_OBJECT('fixture',TRUE,'title','더미 발송 완료 알림'),'SENT',1,'local:registration:1:1:confirmed',@base-INTERVAL 4 DAY),
 (3,9,'INAPP',NULL,JSON_OBJECT('fixture',TRUE,'title','포인트 반환 완료','points',20000),'AVAILABLE',0,'local:refund:registration:5',@base-INTERVAL 2 DAY);
INSERT INTO audit_logs (id,actor_id,action,target_type,target_id,request_id,reason,changes,occurred_at,created_at) VALUES
 (1,1,'COMPANY_APPROVED','COMPANY',1,'local-audit-company-1','로컬 더미 기업 승인',JSON_OBJECT('from','PENDING','to','APPROVED'),@base-INTERVAL 50 DAY,@base-INTERVAL 50 DAY),
 (2,1,'EVENT_CANCELLED','EVENT',5,'local-audit-event-5','로컬 더미 행사 취소',JSON_OBJECT('from','PUBLISHED','to','CANCELLED'),@base-INTERVAL 1 DAY,@base-INTERVAL 1 DAY);
-- Metadata only. These paths intentionally refer to no actual storage objects.
INSERT INTO files (id,uploader_id,storage_key,original_name,mime_type,size_bytes,status,company_id,event_id,registration_id,purpose,sort_order,created_at) VALUES
 (1,2,'local-fixtures/company-1/certificate.pdf','더미사업자증빙.pdf','application/pdf',1024,'READY',1,NULL,NULL,'BUSINESS_CERT',1,@base-INTERVAL 55 DAY),
 (2,2,'local-fixtures/event-1/poster.png','더미포스터.png','image/png',2048,'READY',NULL,1,NULL,'POSTER',1,@base-INTERVAL 30 DAY),
 (3,5,'local-fixtures/registration-1/proof.pdf','더미신청증빙.pdf','application/pdf',512,'READY',NULL,NULL,1,'REGISTRATION_PROOF',1,@base-INTERVAL 4 DAY);
