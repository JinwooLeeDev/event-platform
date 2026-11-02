-- Local development only. User-supplied 2026-11-02 model: 27 tables / 60 FKs.
-- MySQL 8.0+. UTC DATETIME(6). No DROP, no disabled foreign-key checks.
-- IF NOT EXISTS allows retry after MySQL auto-committed DDL.
-- Cross-row policies and ledger invariants remain application responsibilities.

CREATE TABLE IF NOT EXISTS users (
 id BIGINT NOT NULL AUTO_INCREMENT,
 email VARCHAR(254) NOT NULL, password_hash VARCHAR(255) NOT NULL,
 display_name VARCHAR(80) NOT NULL, phone VARCHAR(30) NULL,
 role VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL,
 email_verified_at DATETIME(6) NULL, withdrawn_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(id), UNIQUE KEY uq_users_1(email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS email_verifications (
 id BIGINT NOT NULL AUTO_INCREMENT, email VARCHAR(254) NOT NULL,
 purpose VARCHAR(30) NOT NULL, code_hash VARCHAR(255) NOT NULL,
 expires_at DATETIME(6) NOT NULL, verified_at DATETIME(6) NULL,
 consumed_at DATETIME(6) NULL, failure_count INTEGER NOT NULL,
 sent_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS companies (
 id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL,
 business_number VARCHAR(10) NOT NULL, name VARCHAR(160) NOT NULL,
 representative_name VARCHAR(80) NOT NULL, opened_on DATE NULL,
 contact_name VARCHAR(80) NOT NULL, contact_phone VARCHAR(30) NOT NULL,
 status VARCHAR(30) NOT NULL, approved_at DATETIME(6) NULL,
 business_check_status VARCHAR(30) NULL, business_checked_at DATETIME(6) NULL,
 reviewer_id BIGINT NULL, review_feedback TEXT NULL, reviewed_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(id), UNIQUE KEY uq_companies_1(user_id), UNIQUE KEY uq_companies_2(business_number),
 CONSTRAINT fk_companies_user_id FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_companies_reviewer_id FOREIGN KEY(reviewer_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS events (
 id BIGINT NOT NULL AUTO_INCREMENT, company_id BIGINT NOT NULL,
 title VARCHAR(200) NULL, description TEXT NULL, address VARCHAR(300) NULL,
 venue VARCHAR(160) NULL, latitude NUMERIC(10,7) NULL, longitude NUMERIC(10,7) NULL,
 status VARCHAR(30) NOT NULL, starts_at DATETIME(6) NULL, ends_at DATETIME(6) NULL,
 published_at DATETIME(6) NULL, cancelled_at DATETIME(6) NULL, cancellation_reason TEXT NULL,
 category_code VARCHAR(40) NULL, region_code VARCHAR(30) NULL, policy_snapshot JSON NULL,
 cancel_job_status VARCHAR(20) NULL, cancel_cursor_registration_id BIGINT NULL,
 cancel_lease_token VARCHAR(80) NULL, cancel_lease_until DATETIME(6) NULL,
 cancel_retry_at DATETIME(6) NULL, cancel_error_code VARCHAR(80) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 CONSTRAINT fk_events_company_id FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS event_sessions (
 id BIGINT NOT NULL AUTO_INCREMENT, event_id BIGINT NOT NULL, name VARCHAR(120) NOT NULL,
 status VARCHAR(20) NOT NULL, starts_at DATETIME(6) NOT NULL, ends_at DATETIME(6) NOT NULL,
 registration_opens_at DATETIME(6) NOT NULL, registration_closes_at DATETIME(6) NOT NULL,
 capacity INTEGER NOT NULL, occupied_count INTEGER NOT NULL, next_queue_number BIGINT NOT NULL,
 hold_seconds INTEGER NULL, admission_points BIGINT NOT NULL, reservation_points BIGINT NULL,
 minimum_count INTEGER NULL, judgment_at DATETIME(6) NULL, judged_at DATETIME(6) NULL,
 judged_confirmed_count INTEGER NULL, judgment_result VARCHAR(20) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 CONSTRAINT fk_event_sessions_event_id FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS event_reviews (
 id BIGINT NOT NULL AUTO_INCREMENT, event_id BIGINT NOT NULL, round_no INTEGER NOT NULL,
 reviewer_id BIGINT NULL, status VARCHAR(30) NOT NULL, feedback TEXT NULL,
 submitted_at DATETIME(6) NOT NULL, reviewed_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(id), UNIQUE KEY uq_event_reviews_1(event_id, round_no),
 CONSTRAINT fk_event_reviews_event_id FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_event_reviews_reviewer_id FOREIGN KEY(reviewer_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS registrations (
 id BIGINT NOT NULL AUTO_INCREMENT, session_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 status VARCHAR(20) NOT NULL, attempt_no INTEGER NOT NULL, queue_number BIGINT NOT NULL,
 applied_at DATETIME(6) NOT NULL, hold_expires_at DATETIME(6) NULL,
 confirmed_at DATETIME(6) NULL, cancelled_at DATETIME(6) NULL,
 cancellation_reason VARCHAR(80) NULL, policy_snapshot JSON NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_registrations_1(session_id, user_id), UNIQUE KEY uq_registrations_2(session_id, queue_number),
 CONSTRAINT fk_registrations_session_id FOREIGN KEY(session_id) REFERENCES event_sessions(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_registrations_user_id FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS registration_history (
 id BIGINT NOT NULL AUTO_INCREMENT, registration_id BIGINT NOT NULL, attempt_no INTEGER NOT NULL,
 from_status VARCHAR(20) NULL, to_status VARCHAR(20) NOT NULL, actor_id BIGINT NULL,
 reason_code VARCHAR(80) NOT NULL, occurred_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 CONSTRAINT fk_registration_history_registration_id FOREIGN KEY(registration_id) REFERENCES registrations(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_registration_history_actor_id FOREIGN KEY(actor_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS attendances (
 id BIGINT NOT NULL AUTO_INCREMENT, registration_id BIGINT NOT NULL, attempt_no INTEGER NOT NULL,
 checked_by BIGINT NOT NULL, attended_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_attendances_1(registration_id),
 CONSTRAINT fk_attendances_registration_id FOREIGN KEY(registration_id) REFERENCES registrations(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_attendances_checked_by FOREIGN KEY(checked_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS point_accounts (
 id BIGINT NOT NULL AUTO_INCREMENT, code VARCHAR(120) NOT NULL, kind VARCHAR(30) NOT NULL,
 balance BIGINT NOT NULL, status VARCHAR(20) NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(id), UNIQUE KEY uq_point_accounts_1(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS wallets (
 id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, account_id BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_wallets_1(user_id), UNIQUE KEY uq_wallets_2(account_id),
 CONSTRAINT fk_wallets_user_id FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_wallets_account_id FOREIGN KEY(account_id) REFERENCES point_accounts(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS point_transactions (
 id BIGINT NOT NULL AUTO_INCREMENT, transaction_key VARCHAR(160) NOT NULL,
 type VARCHAR(30) NOT NULL, original_transaction_id BIGINT NULL, actor_id BIGINT NULL,
 reason_code VARCHAR(80) NOT NULL, posted_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_point_transactions_1(transaction_key),
 CONSTRAINT fk_point_transactions_original_transaction_id FOREIGN KEY(original_transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_point_transactions_actor_id FOREIGN KEY(actor_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS point_entries (
 id BIGINT NOT NULL AUTO_INCREMENT, transaction_id BIGINT NOT NULL, account_id BIGINT NOT NULL,
 delta BIGINT NOT NULL, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_point_entries_1(transaction_id, account_id),
 CONSTRAINT fk_point_entries_transaction_id FOREIGN KEY(transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_point_entries_account_id FOREIGN KEY(account_id) REFERENCES point_accounts(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS topups (
 id BIGINT NOT NULL AUTO_INCREMENT, wallet_id BIGINT NOT NULL, order_key VARCHAR(120) NOT NULL,
 amount_krw BIGINT NOT NULL, status VARCHAR(20) NOT NULL, credited_transaction_id BIGINT NULL,
 expires_at DATETIME(6) NOT NULL, provider VARCHAR(50) NOT NULL, provider_payment_id VARCHAR(160) NULL,
 verified_at DATETIME(6) NULL, retry_at DATETIME(6) NULL, lease_token VARCHAR(80) NULL, lease_until DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_topups_1(order_key), UNIQUE KEY uq_topups_2(credited_transaction_id), UNIQUE KEY uq_topups_3(provider, provider_payment_id),
 CONSTRAINT fk_topups_wallet_id FOREIGN KEY(wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_topups_credited_transaction_id FOREIGN KEY(credited_transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS event_deposits (
 id BIGINT NOT NULL AUTO_INCREMENT, event_id BIGINT NOT NULL, wallet_id BIGINT NOT NULL,
 charge_transaction_id BIGINT NOT NULL, amount_points BIGINT NOT NULL, status VARCHAR(30) NOT NULL,
 forfeiture_transaction_id BIGINT NULL, policy_snapshot JSON NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_event_deposits_1(event_id), UNIQUE KEY uq_event_deposits_2(charge_transaction_id),
 UNIQUE KEY uq_event_deposits_3(forfeiture_transaction_id),
 CONSTRAINT fk_event_deposits_event_id FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_event_deposits_wallet_id FOREIGN KEY(wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_event_deposits_charge_transaction_id FOREIGN KEY(charge_transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_event_deposits_forfeiture_transaction_id FOREIGN KEY(forfeiture_transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS registration_charges (
 id BIGINT NOT NULL AUTO_INCREMENT, registration_id BIGINT NOT NULL, attempt_no INTEGER NOT NULL,
 wallet_id BIGINT NOT NULL, component VARCHAR(20) NOT NULL, amount_points BIGINT NOT NULL,
 transaction_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL, policy_snapshot JSON NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_registration_charges_1(registration_id, attempt_no, component),
 CONSTRAINT fk_registration_charges_registration_id FOREIGN KEY(registration_id) REFERENCES registrations(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_registration_charges_wallet_id FOREIGN KEY(wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_registration_charges_transaction_id FOREIGN KEY(transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS participation_rewards (
 id BIGINT NOT NULL AUTO_INCREMENT, attendance_id BIGINT NOT NULL, company_id BIGINT NOT NULL,
 from_wallet_id BIGINT NOT NULL, to_wallet_id BIGINT NOT NULL, amount_points BIGINT NOT NULL,
 transaction_id BIGINT NOT NULL, reversal_transaction_id BIGINT NULL, policy_snapshot JSON NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_participation_rewards_1(attendance_id), UNIQUE KEY uq_participation_rewards_2(transaction_id),
 UNIQUE KEY uq_participation_rewards_3(reversal_transaction_id),
 CONSTRAINT fk_participation_rewards_attendance_id FOREIGN KEY(attendance_id) REFERENCES attendances(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_participation_rewards_company_id FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_participation_rewards_from_wallet_id FOREIGN KEY(from_wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_participation_rewards_to_wallet_id FOREIGN KEY(to_wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_participation_rewards_transaction_id FOREIGN KEY(transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_participation_rewards_reversal_transaction_id FOREIGN KEY(reversal_transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS settlements (
 id BIGINT NOT NULL AUTO_INCREMENT, registration_charge_id BIGINT NOT NULL,
 company_id BIGINT NOT NULL, wallet_id BIGINT NOT NULL, gross_points BIGINT NOT NULL,
 refunded_points BIGINT NOT NULL, fee_points BIGINT NOT NULL, net_points BIGINT NOT NULL,
 policy_snapshot JSON NOT NULL, status VARCHAR(20) NOT NULL, transaction_id BIGINT NULL, paid_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_settlements_1(registration_charge_id), UNIQUE KEY uq_settlements_2(transaction_id),
 CONSTRAINT fk_settlements_registration_charge_id FOREIGN KEY(registration_charge_id) REFERENCES registration_charges(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_settlements_company_id FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_settlements_wallet_id FOREIGN KEY(wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_settlements_transaction_id FOREIGN KEY(transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS reviews (
 id BIGINT NOT NULL AUTO_INCREMENT, attendance_id BIGINT NOT NULL, body TEXT NOT NULL,
 status VARCHAR(20) NOT NULL, edited_at DATETIME(6) NULL, deleted_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_reviews_1(attendance_id),
 CONSTRAINT fk_reviews_attendance_id FOREIGN KEY(attendance_id) REFERENCES attendances(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS chat_messages (
 id BIGINT NOT NULL AUTO_INCREMENT, sender_id BIGINT NOT NULL, client_message_key VARCHAR(120) NOT NULL,
 kind VARCHAR(20) NOT NULL, body TEXT NOT NULL, status VARCHAR(20) NOT NULL,
 sent_at DATETIME(6) NOT NULL, event_id BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_chat_messages_1(event_id, sender_id, client_message_key),
 CONSTRAINT fk_chat_messages_sender_id FOREIGN KEY(sender_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_chat_messages_event_id FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS products (
 id BIGINT NOT NULL AUTO_INCREMENT, code VARCHAR(80) NOT NULL, kind VARCHAR(30) NOT NULL,
 name VARCHAR(160) NOT NULL, status VARCHAR(20) NOT NULL, unit_points BIGINT NOT NULL,
 terms_snapshot JSON NOT NULL, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(id), UNIQUE KEY uq_products_1(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS product_orders (
 id BIGINT NOT NULL AUTO_INCREMENT, company_id BIGINT NOT NULL, wallet_id BIGINT NOT NULL,
 event_id BIGINT NULL, event_review_id BIGINT NULL, order_key VARCHAR(160) NOT NULL,
 quantity INTEGER NOT NULL, unit_points BIGINT NOT NULL, total_points BIGINT NOT NULL,
 status VARCHAR(30) NOT NULL, charge_transaction_id BIGINT NULL, product_id BIGINT NOT NULL,
 terms_snapshot JSON NOT NULL, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_product_orders_1(order_key), UNIQUE KEY uq_product_orders_2(charge_transaction_id),
 CONSTRAINT fk_product_orders_company_id FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_product_orders_wallet_id FOREIGN KEY(wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_product_orders_event_id FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_product_orders_event_review_id FOREIGN KEY(event_review_id) REFERENCES event_reviews(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_product_orders_charge_transaction_id FOREIGN KEY(charge_transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_product_orders_product_id FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS ad_campaigns (
 id BIGINT NOT NULL AUTO_INCREMENT, order_id BIGINT NOT NULL, event_id BIGINT NOT NULL,
 placement VARCHAR(40) NOT NULL, priority INTEGER NOT NULL, starts_at DATETIME(6) NOT NULL,
 ends_at DATETIME(6) NOT NULL, status VARCHAR(20) NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id), UNIQUE KEY uq_ad_campaigns_1(order_id),
 CONSTRAINT fk_ad_campaigns_order_id FOREIGN KEY(order_id) REFERENCES product_orders(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_ad_campaigns_event_id FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS notifications (
 id BIGINT NOT NULL AUTO_INCREMENT, recipient_id BIGINT NOT NULL, channel VARCHAR(20) NOT NULL,
 destination_snapshot VARCHAR(254) NULL, content_snapshot JSON NOT NULL, status VARCHAR(20) NOT NULL,
 read_at DATETIME(6) NULL, attempt_count INTEGER NOT NULL, retry_at DATETIME(6) NULL,
 lease_token VARCHAR(80) NULL, lease_until DATETIME(6) NULL, event_key VARCHAR(180) NOT NULL,
 last_error_code VARCHAR(80) NULL, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_notifications_1(event_key, recipient_id, channel),
 CONSTRAINT fk_notifications_recipient_id FOREIGN KEY(recipient_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS audit_logs (
 id BIGINT NOT NULL AUTO_INCREMENT, actor_id BIGINT NULL, action VARCHAR(80) NOT NULL,
 target_type VARCHAR(80) NOT NULL, target_id BIGINT NOT NULL, request_id VARCHAR(120) NOT NULL,
 reason TEXT NULL, changes JSON NOT NULL, occurred_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 CONSTRAINT fk_audit_logs_actor_id FOREIGN KEY(actor_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS files (
 id BIGINT NOT NULL AUTO_INCREMENT, uploader_id BIGINT NOT NULL, storage_key VARCHAR(500) NOT NULL,
 original_name VARCHAR(255) NOT NULL, mime_type VARCHAR(120) NOT NULL, size_bytes BIGINT NOT NULL,
 status VARCHAR(20) NOT NULL, company_id BIGINT NULL, event_id BIGINT NULL, registration_id BIGINT NULL,
 purpose VARCHAR(30) NULL, sort_order INTEGER NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id), UNIQUE KEY uq_files_1(storage_key),
 CONSTRAINT fk_files_uploader_id FOREIGN KEY(uploader_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_files_company_id FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_files_event_id FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_files_registration_id FOREIGN KEY(registration_id) REFERENCES registrations(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS point_refunds (
 id BIGINT NOT NULL AUTO_INCREMENT, refund_key VARCHAR(160) NOT NULL, wallet_id BIGINT NOT NULL,
 deposit_id BIGINT NULL, registration_charge_id BIGINT NULL, product_order_id BIGINT NULL,
 original_transaction_id BIGINT NOT NULL, refund_transaction_id BIGINT NULL, amount_points BIGINT NOT NULL,
 reason_code VARCHAR(80) NOT NULL, status VARCHAR(20) NOT NULL, policy_snapshot JSON NOT NULL,
 retry_at DATETIME(6) NULL, lease_token VARCHAR(80) NULL, lease_until DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id),
 UNIQUE KEY uq_point_refunds_1(refund_key), UNIQUE KEY uq_point_refunds_2(refund_transaction_id),
 CONSTRAINT fk_point_refunds_wallet_id FOREIGN KEY(wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_point_refunds_deposit_id FOREIGN KEY(deposit_id) REFERENCES event_deposits(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_point_refunds_registration_charge_id FOREIGN KEY(registration_charge_id) REFERENCES registration_charges(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_point_refunds_product_order_id FOREIGN KEY(product_order_id) REFERENCES product_orders(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_point_refunds_original_transaction_id FOREIGN KEY(original_transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_point_refunds_refund_transaction_id FOREIGN KEY(refund_transaction_id) REFERENCES point_transactions(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
