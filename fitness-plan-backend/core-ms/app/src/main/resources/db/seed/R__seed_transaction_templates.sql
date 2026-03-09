INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('DIRECT_TRANSFER', 'P2P', false, 10, true, 'Direct Transfer', 'SENDER', 'TRANSFER', 'DEBIT_ACCOUNT,CREDIT_ACCOUNT', 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO UPDATE SET description = EXCLUDED.description, account_charged = EXCLUDED.account_charged, process_type = EXCLUDED.process_type, required_account_roles = EXCLUDED.required_account_roles;

INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('DIRECT_INBOUND', 'INBOUND', false, 10, true, 'Direct Inbound', 'RECIPIENT', 'DEPOSIT', 'DEBIT_ACCOUNT,CREDIT_ACCOUNT', 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO UPDATE SET description = EXCLUDED.description, account_charged = EXCLUDED.account_charged, process_type = EXCLUDED.process_type, required_account_roles = EXCLUDED.required_account_roles;

INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('DIRECT_OUTBOUND', 'OUTBOUND', false, 10, true, 'Direct Outbound', 'SENDER', 'WITHDRAWAL', 'DEBIT_ACCOUNT,CREDIT_ACCOUNT', 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO UPDATE SET description = EXCLUDED.description, account_charged = EXCLUDED.account_charged, process_type = EXCLUDED.process_type, required_account_roles = EXCLUDED.required_account_roles;

INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('PENDING_INBOUND', 'INBOUND', true, 10, true, 'Pending Inbound', 'RECIPIENT', 'DEPOSIT', 'DEBIT_ACCOUNT,CREDIT_ACCOUNT', 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO UPDATE SET description = EXCLUDED.description, account_charged = EXCLUDED.account_charged, process_type = EXCLUDED.process_type, required_account_roles = EXCLUDED.required_account_roles;

INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('PENDING_OUTBOUND', 'OUTBOUND', true, 10, true, 'Pending Outbound', 'SENDER', 'WITHDRAWAL', 'DEBIT_ACCOUNT,CREDIT_ACCOUNT', 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO UPDATE SET description = EXCLUDED.description, account_charged = EXCLUDED.account_charged, process_type = EXCLUDED.process_type, required_account_roles = EXCLUDED.required_account_roles;

INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('INCENTIVE_CASHBACK', 'INTERNAL', false, 10, true, 'Incentive Cashback', 'SENDER', NULL, NULL, 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO NOTHING;

INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('INCENTIVE_DISCOUNT', 'INTERNAL', false, 10, true, 'Incentive Discount', 'SENDER', NULL, NULL, 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO NOTHING;

INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('INCENTIVE_POINTS', 'INTERNAL', false, 10, true, 'Incentive Points', 'SENDER', NULL, NULL, 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO NOTHING;

INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('HOLD_CREATE', 'HOLD', false, 10, true, 'Create Hold on Account', 'SENDER', 'LIEN_AMOUNT', 'DEBIT_ACCOUNT,LOCK_ACCOUNT', 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO NOTHING;

INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('HOLD_RELEASE', 'HOLD', false, 10, true, 'Release Hold on Account', 'SENDER', 'LIEN_AMOUNT', 'DEBIT_ACCOUNT,LOCK_ACCOUNT', 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO NOTHING;

INSERT INTO transaction_template (name, transaction_group, is_pending, priority, is_active, description, account_charged, process_type, required_account_roles, created_by, last_modified_by)
VALUES ('HOLD_CAPTURE', 'HOLD', false, 10, true, 'Capture Hold Amount', 'SENDER', 'LIEN_AMOUNT', 'DEBIT_ACCOUNT,LOCK_ACCOUNT,CREDIT_ACCOUNT', 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO NOTHING;

DELETE FROM template_entry_rule WHERE template_id IN (SELECT id FROM transaction_template WHERE merchant_id IS NULL);

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'DEBIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'DIRECT_TRANSFER';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'CREDIT_ACCOUNT', 'CREDIT_ACCOUNT', 'CREDIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'DIRECT_TRANSFER';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'DEBIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'DIRECT_INBOUND';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'CREDIT_ACCOUNT', 'CREDIT_ACCOUNT', 'CREDIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'DIRECT_INBOUND';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'DEBIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'DIRECT_OUTBOUND';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'CREDIT_ACCOUNT', 'CREDIT_ACCOUNT', 'CREDIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'DIRECT_OUTBOUND';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'DEBIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'PENDING_INBOUND';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'CREDIT_ACCOUNT', 'CREDIT_ACCOUNT', 'CREDIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'PENDING_INBOUND';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'DEBIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'PENDING_OUTBOUND';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'CREDIT_ACCOUNT', 'CREDIT_ACCOUNT', 'CREDIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'PENDING_OUTBOUND';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'DEBIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'INCENTIVE_CASHBACK';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'CREDIT_ACCOUNT', 'CREDIT_ACCOUNT', 'CREDIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'INCENTIVE_CASHBACK';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'DEBIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'INCENTIVE_DISCOUNT';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'CREDIT_ACCOUNT', 'CREDIT_ACCOUNT', 'CREDIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'INCENTIVE_DISCOUNT';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'DEBIT', 'POINTS', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'INCENTIVE_POINTS';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'CREDIT_ACCOUNT', 'CREDIT_ACCOUNT', 'CREDIT', 'POINTS', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'INCENTIVE_POINTS';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'DEBIT', 'ON_HOLD', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'HOLD_CREATE';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'LOCK_ACCOUNT', 'LOCK_ACCOUNT', 'CREDIT', 'ON_HOLD', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'HOLD_CREATE';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'LOCK_ACCOUNT', 'LOCK_ACCOUNT', 'DEBIT', 'ON_HOLD', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'HOLD_RELEASE';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'CREDIT', 'ON_HOLD', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'HOLD_RELEASE';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 1, 'BASE_LAYER', 'LOCK_ACCOUNT', 'LOCK_ACCOUNT', 'DEBIT', 'ON_HOLD', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'HOLD_CAPTURE';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 2, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'CREDIT', 'ON_HOLD', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'HOLD_CAPTURE';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 3, 'BASE_LAYER', 'DEBIT_ACCOUNT', 'DEBIT_ACCOUNT', 'DEBIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'HOLD_CAPTURE';

INSERT INTO template_entry_rule (template_id, sequence, phase, source_account_role, target_account_role, entry_leg, layer_type, amount_type, is_active, created_by, last_modified_by)
SELECT id, 4, 'BASE_LAYER', 'CREDIT_ACCOUNT', 'CREDIT_ACCOUNT', 'CREDIT', 'BASE', 'FULL', true, 'system', 'system'
FROM transaction_template WHERE name = 'HOLD_CAPTURE';

DELETE FROM template_completion_rule WHERE template_id IN (SELECT id FROM transaction_template WHERE merchant_id IS NULL);
