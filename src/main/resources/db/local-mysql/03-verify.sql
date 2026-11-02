-- Read-only verification. A FAIL row makes the bootstrap command exit nonzero.
SELECT IF(COUNT(*)=27,'PASS','FAIL'),'application_tables',COUNT(*)
FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name<>'_local_dev_bootstrap';
SELECT IF(COUNT(*)=60,'PASS','FAIL'),'foreign_keys',COUNT(*)
FROM information_schema.referential_constraints WHERE constraint_schema=DATABASE();

SELECT 'COUNT',name,total FROM (
 SELECT 'users' name,COUNT(*) total FROM users UNION ALL
 SELECT 'email_verifications',COUNT(*) FROM email_verifications UNION ALL
 SELECT 'companies',COUNT(*) FROM companies UNION ALL
 SELECT 'events',COUNT(*) FROM events UNION ALL
 SELECT 'event_sessions',COUNT(*) FROM event_sessions UNION ALL
 SELECT 'event_reviews',COUNT(*) FROM event_reviews UNION ALL
 SELECT 'registrations',COUNT(*) FROM registrations UNION ALL
 SELECT 'registration_history',COUNT(*) FROM registration_history UNION ALL
 SELECT 'attendances',COUNT(*) FROM attendances UNION ALL
 SELECT 'point_accounts',COUNT(*) FROM point_accounts UNION ALL
 SELECT 'wallets',COUNT(*) FROM wallets UNION ALL
 SELECT 'point_transactions',COUNT(*) FROM point_transactions UNION ALL
 SELECT 'point_entries',COUNT(*) FROM point_entries UNION ALL
 SELECT 'topups',COUNT(*) FROM topups UNION ALL
 SELECT 'event_deposits',COUNT(*) FROM event_deposits UNION ALL
 SELECT 'registration_charges',COUNT(*) FROM registration_charges UNION ALL
 SELECT 'participation_rewards',COUNT(*) FROM participation_rewards UNION ALL
 SELECT 'settlements',COUNT(*) FROM settlements UNION ALL
 SELECT 'reviews',COUNT(*) FROM reviews UNION ALL
 SELECT 'chat_messages',COUNT(*) FROM chat_messages UNION ALL
 SELECT 'products',COUNT(*) FROM products UNION ALL
 SELECT 'product_orders',COUNT(*) FROM product_orders UNION ALL
 SELECT 'ad_campaigns',COUNT(*) FROM ad_campaigns UNION ALL
 SELECT 'notifications',COUNT(*) FROM notifications UNION ALL
 SELECT 'audit_logs',COUNT(*) FROM audit_logs UNION ALL
 SELECT 'files',COUNT(*) FROM files UNION ALL
 SELECT 'point_refunds',COUNT(*) FROM point_refunds
) counts ORDER BY name;

SELECT IF(COUNT(*)=0,'PASS','FAIL'),'balanced_transactions',COUNT(*) FROM (
 SELECT t.id FROM point_transactions t LEFT JOIN point_entries e ON e.transaction_id=t.id
 GROUP BY t.id HAVING COALESCE(SUM(e.delta),1)<>0 OR COUNT(DISTINCT e.account_id)<2
) invalid;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'nonzero_entries',COUNT(*) FROM point_entries WHERE delta=0;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'balance_cache',COUNT(*)
FROM point_accounts a LEFT JOIN (SELECT account_id,SUM(delta) total FROM point_entries GROUP BY account_id) e
ON a.id=e.account_id WHERE a.balance<>COALESCE(e.total,0);
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'nonnegative_balances',COUNT(*)
FROM point_accounts WHERE kind<>'EXTERNAL_CLEARING' AND balance<0;
SELECT IF(COALESCE(SUM(balance),1)=0,'PASS','FAIL'),'global_ledger_balance',SUM(balance) FROM point_accounts;

SELECT IF(COUNT(*)=0,'PASS','FAIL'),'session_occupancy',COUNT(*) FROM event_sessions s
LEFT JOIN (SELECT session_id,COUNT(*) occupied FROM registrations WHERE status IN ('HOLD','CONFIRMED') GROUP BY session_id) r
ON s.id=r.session_id WHERE s.occupied_count<>COALESCE(r.occupied,0) OR s.occupied_count<0 OR s.occupied_count>s.capacity;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'session_dates',COUNT(*) FROM event_sessions
WHERE NOT(registration_opens_at<registration_closes_at AND registration_closes_at<=starts_at AND starts_at<ends_at)
OR admission_points<0 OR reservation_points<0;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'queue_sequence',COUNT(*) FROM event_sessions s
JOIN (SELECT session_id,MAX(queue_number) last_number FROM registrations GROUP BY session_id) r
ON s.id=r.session_id WHERE s.next_queue_number<=r.last_number;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'attendance_current_attempt',COUNT(*) FROM attendances a
JOIN registrations r ON r.id=a.registration_id WHERE r.status<>'CONFIRMED' OR r.attempt_no<>a.attempt_no;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'review_creation_window',COUNT(*) FROM reviews v
JOIN attendances a ON a.id=v.attendance_id JOIN registrations r ON r.id=a.registration_id
JOIN event_sessions s ON s.id=r.session_id JOIN events e ON e.id=s.event_id
WHERE v.created_at<a.attended_at OR v.created_at>=e.ends_at;

SELECT IF(COUNT(*)=0,'PASS','FAIL'),'refund_source_and_wallet',COUNT(*) FROM point_refunds r
LEFT JOIN event_deposits d ON d.id=r.deposit_id
LEFT JOIN registration_charges c ON c.id=r.registration_charge_id
LEFT JOIN product_orders o ON o.id=r.product_order_id
WHERE ((r.deposit_id IS NOT NULL)+(r.registration_charge_id IS NOT NULL)+(r.product_order_id IS NOT NULL))<>1
OR r.wallet_id<>COALESCE(d.wallet_id,c.wallet_id,o.wallet_id)
OR r.original_transaction_id<>COALESCE(d.charge_transaction_id,c.transaction_id,o.charge_transaction_id)
OR r.amount_points<=0;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'refund_limits',COUNT(*) FROM (
 SELECT original_transaction_id,SUM(amount_points) refunded FROM point_refunds WHERE status='SUCCEEDED'
 GROUP BY original_transaction_id
) r JOIN (
 SELECT charge_transaction_id id,amount_points amount FROM event_deposits
 UNION ALL SELECT transaction_id,amount_points FROM registration_charges
 UNION ALL SELECT charge_transaction_id,total_points FROM product_orders
) source ON source.id=r.original_transaction_id WHERE r.refunded>source.amount;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'deposit_disposition_limit',COUNT(*) FROM event_deposits d
LEFT JOIN (SELECT deposit_id,SUM(amount_points) refunded FROM point_refunds WHERE status='SUCCEEDED' GROUP BY deposit_id) r ON r.deposit_id=d.id
LEFT JOIN (SELECT transaction_id,SUM(delta) forfeited FROM point_entries e JOIN point_accounts a ON a.id=e.account_id
 WHERE a.kind='PLATFORM_REVENUE' GROUP BY transaction_id) f ON f.transaction_id=d.forfeiture_transaction_id
WHERE d.amount_points<>150000 OR COALESCE(r.refunded,0)+COALESCE(f.forfeited,0)>d.amount_points;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'settlement_arithmetic',COUNT(*) FROM settlements
WHERE gross_points-refunded_points<>fee_points+net_points OR LEAST(gross_points,refunded_points,fee_points,net_points)<0;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'product_order_arithmetic',COUNT(*) FROM product_orders
WHERE quantity<=0 OR unit_points<0 OR total_points<>quantity*unit_points;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'file_single_target',COUNT(*) FROM files
WHERE ((company_id IS NOT NULL)+(event_id IS NOT NULL)+(registration_id IS NOT NULL))>1 OR size_bytes<0;
SELECT IF(COUNT(*)=0,'PASS','FAIL'),'reward_ownership',COUNT(*) FROM participation_rewards p
JOIN attendances a ON a.id=p.attendance_id JOIN registrations r ON r.id=a.registration_id
JOIN event_sessions s ON s.id=r.session_id JOIN events e ON e.id=s.event_id
JOIN companies c ON c.id=e.company_id JOIN wallets fw ON fw.id=p.from_wallet_id JOIN wallets tw ON tw.id=p.to_wallet_id
WHERE p.company_id<>e.company_id OR fw.user_id<>c.user_id OR tw.user_id<>r.user_id OR p.amount_points<=0;
