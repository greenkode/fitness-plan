DO $$
DECLARE
    tenant_tables text[] := ARRAY[
        'account',
        'account_address',
        'account_property',
        'process',
        'process_request',
        'process_request_data',
        'process_request_stakeholder',
        'process_event_transition',
        'journal',
        'layer',
        'rule_info',
        'account_lock',
        'balance_cache',
        'checkpoint',
        'latest_balance_snapshot',
        'transaction',
        'transaction_group',
        'transaction_entry',
        'transaction_reference_sequences',
        'limit_checkpoint',
        'banking_integration_log',
        'integration_config',
        'access_token',
        'notification_device',
        'currency_token_mapping',
        'integration_monitoring_log',
        'journal_opening_balance_status',
        'account_rollover_status',
        'campaign',
        'campaign_eligibility',
        'campaign_redemption',
        'template_pricing',
        'customer',
        'account_metadata',
        'export_config',
        'export_record',
        'webhook_delivery',
        'billing_usage_snapshot',
        'billing_invoice',
        'payment_event'
    ];
    shared_tenant_tables text[] := ARRAY[
        'chart_of_accounts_template',
        'transaction_template',
        'account_limit',
        'account_profile'
    ];
    t text;
BEGIN
    FOREACH t IN ARRAY tenant_tables
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);

        EXECUTE format(
            'DROP POLICY IF EXISTS tenant_isolation_%I ON %I', t, t
        );

        EXECUTE format(
            'CREATE POLICY tenant_isolation_%I ON %I
                USING (merchant_id = current_setting(''app.tenant_id'', true)::uuid)
                WITH CHECK (merchant_id = current_setting(''app.tenant_id'', true)::uuid)',
            t, t
        );
    END LOOP;

    FOREACH t IN ARRAY shared_tenant_tables
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);

        EXECUTE format(
            'DROP POLICY IF EXISTS tenant_isolation_%I ON %I', t, t
        );

        EXECUTE format(
            'CREATE POLICY tenant_isolation_%I ON %I
                USING (merchant_id IS NULL OR merchant_id = current_setting(''app.tenant_id'', true)::uuid)
                WITH CHECK (merchant_id = current_setting(''app.tenant_id'', true)::uuid)',
            t, t
        );
    END LOOP;
END
$$;
