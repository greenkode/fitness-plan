CREATE TABLE IF NOT EXISTS event_publication
(
    id                     uuid                     NOT NULL PRIMARY KEY,
    listener_id            text                     NOT NULL,
    event_type             text                     NOT NULL,
    serialized_event       text                     NOT NULL,
    publication_date       timestamp with time zone NOT NULL,
    completion_date        timestamp with time zone,
    status                 text,
    completion_attempts    integer,
    last_resubmission_date timestamp with time zone
);

CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx
    ON event_publication USING hash (serialized_event);

CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx
    ON event_publication (completion_date);

CREATE TABLE IF NOT EXISTS event_publication_archive
(
    id                     uuid                     NOT NULL PRIMARY KEY,
    listener_id            text                     NOT NULL,
    event_type             text                     NOT NULL,
    serialized_event       text                     NOT NULL,
    publication_date       timestamp with time zone NOT NULL,
    completion_date        timestamp with time zone,
    status                 text,
    completion_attempts    integer,
    last_resubmission_date timestamp with time zone
);

CREATE INDEX IF NOT EXISTS event_publication_archive_serialized_event_hash_idx
    ON event_publication_archive USING hash (serialized_event);

CREATE INDEX IF NOT EXISTS event_publication_archive_by_completion_date_idx
    ON event_publication_archive (completion_date);

CREATE TABLE IF NOT EXISTS account_profile
(
    id               serial       NOT NULL PRIMARY KEY,
    name             varchar(255) NOT NULL,
    description      varchar(255) NOT NULL,
    public_id        uuid         NOT NULL DEFAULT gen_random_uuid(),
    skip_limits      boolean      NOT NULL DEFAULT false,
    created_by       varchar(50)  NOT NULL,
    created_at       timestamp             DEFAULT NULL,
    last_modified_by varchar(50)           DEFAULT NULL,
    last_modified_at timestamp             DEFAULT NULL,
    version          bigint       NOT NULL,
    CONSTRAINT ux_account_profile_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS account
(
    id               bigserial    NOT NULL PRIMARY KEY,
    merchant_id      uuid         NOT NULL,
    subclass         varchar(1)   NOT NULL DEFAULT ' ',
    name             varchar(255),
    alias            varchar(255),
    trust_level      varchar(100),
    owner_id         uuid         NOT NULL,
    type             varchar(255),
    currency         varchar(255),
    balance_snapshot numeric      NOT NULL DEFAULT 0,
    parent_id        bigint
        CONSTRAINT fk_account_parent_id REFERENCES account (id),
    root_id          bigint
        CONSTRAINT fk_account_root_id REFERENCES account (id),
    profile_id       int
        CONSTRAINT fk_account_profile_id REFERENCES account_profile (id),
    status           varchar(50),
    is_default       boolean,
    code             varchar(255),
    description      varchar(255),
    expiration       timestamp,
    gl_type          integer      NOT NULL DEFAULT 0,
    rollover_policy  varchar(20)           DEFAULT 'NONE',
    period_type      varchar(20)           DEFAULT 'ANNUAL',
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    version          bigint       NOT NULL,
    public_id        uuid         NOT NULL DEFAULT gen_random_uuid() UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_account_code ON account (code);
CREATE INDEX IF NOT EXISTS idx_account_root_id ON account (root_id);
CREATE INDEX IF NOT EXISTS idx_account_merchant_id ON account (merchant_id);

CREATE TABLE IF NOT EXISTS account_address
(
    id               bigserial    NOT NULL PRIMARY KEY,
    merchant_id      uuid         NOT NULL,
    address          varchar(255) NOT NULL,
    type             varchar(255) NOT NULL,
    platform         varchar(255) NOT NULL,
    currency         varchar(25)  NOT NULL,
    account_id       bigint       NOT NULL
        CONSTRAINT fk_account_address_account_id REFERENCES account (id),
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    version          bigint       NOT NULL,
    CONSTRAINT fk_unique_address UNIQUE (address, type, platform)
);

CREATE INDEX IF NOT EXISTS idx_account_address_merchant_id ON account_address (merchant_id);

CREATE TABLE IF NOT EXISTS account_address_property
(
    account_address_id bigint       NOT NULL,
    name               varchar(255) NOT NULL,
    value              varchar(255) NOT NULL,
    PRIMARY KEY (account_address_id, name),
    FOREIGN KEY (account_address_id) REFERENCES account_address (id)
);

CREATE TABLE IF NOT EXISTS account_property
(
    id               bigserial    NOT NULL PRIMARY KEY,
    merchant_id      uuid         NOT NULL,
    name             varchar(255) NOT NULL,
    value            text         NOT NULL,
    account_id       bigint
        CONSTRAINT fk_account_property_account_id REFERENCES account (id),
    scope            varchar(255) NOT NULL,
    scope_value      varchar(255) NOT NULL DEFAULT 'N/A',
    created_by       varchar(50)  NOT NULL,
    created_at       timestamp,
    last_modified_by varchar(50),
    last_modified_at timestamp,
    version          bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ux_account_property_scope UNIQUE (name, scope, scope_value, account_id)
);

CREATE INDEX IF NOT EXISTS idx_account_property_merchant_id ON account_property (merchant_id);

CREATE TABLE IF NOT EXISTS process
(
    id                   bigserial    NOT NULL PRIMARY KEY,
    merchant_id          uuid         NOT NULL,
    type                 varchar(255) NOT NULL,
    description          varchar(255) NOT NULL,
    state                varchar(255) NOT NULL,
    channel              varchar(255) NOT NULL,
    public_id            uuid         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    external_reference   varchar(255),
    integrator_reference varchar(255),
    expiry               timestamp,
    created_at           timestamp    NOT NULL DEFAULT now(),
    last_modified_at     timestamp    NOT NULL DEFAULT now(),
    created_by           varchar(255) NOT NULL,
    last_modified_by     varchar(255) NOT NULL,
    version              bigint       NOT NULL,
    CONSTRAINT unique_process_type_external_reference UNIQUE (type, external_reference)
);

CREATE INDEX IF NOT EXISTS idx_process_merchant_id ON process (merchant_id);

CREATE TABLE IF NOT EXISTS process_request
(
    id               bigserial    NOT NULL PRIMARY KEY,
    merchant_id      uuid         NOT NULL,
    process_id       bigint       NOT NULL
        CONSTRAINT fk_process_request_process REFERENCES process (id),
    user_id          uuid         NOT NULL,
    type             varchar(255) NOT NULL,
    state            varchar(255) NOT NULL,
    channel          varchar(255) NOT NULL,
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    version          bigint       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_process_request_merchant_id ON process_request (merchant_id);

CREATE TABLE IF NOT EXISTS process_request_data
(
    merchant_id        uuid         NOT NULL,
    process_request_id bigint       NOT NULL
        CONSTRAINT fk_process_request_data REFERENCES process_request (id),
    name               varchar(255) NOT NULL,
    value              text         NOT NULL,
    created_at         timestamp    NOT NULL DEFAULT now(),
    last_modified_at   timestamp    NOT NULL DEFAULT now(),
    created_by         varchar(255) NOT NULL,
    last_modified_by   varchar(255) NOT NULL,
    version            bigint       NOT NULL,
    PRIMARY KEY (process_request_id, name)
);

CREATE INDEX IF NOT EXISTS idx_process_request_data_merchant_id ON process_request_data (merchant_id);

CREATE TABLE IF NOT EXISTS process_request_stakeholder
(
    id                 bigserial    NOT NULL PRIMARY KEY,
    merchant_id        uuid         NOT NULL,
    process_request_id bigint       NOT NULL
        CONSTRAINT fk_process_request_stakeholder_request REFERENCES process_request (id),
    stakeholder_id     varchar(100) NOT NULL,
    type               varchar(255) NOT NULL,
    created_at         timestamp    NOT NULL DEFAULT now(),
    last_modified_at   timestamp    NOT NULL DEFAULT now(),
    created_by         varchar(255) NOT NULL,
    last_modified_by   varchar(255) NOT NULL,
    version            bigint       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_process_request_stakeholder_merchant_id ON process_request_stakeholder (merchant_id);

CREATE TABLE IF NOT EXISTS process_event_transition
(
    id               bigserial    NOT NULL PRIMARY KEY,
    merchant_id      uuid         NOT NULL,
    process_id       bigint       NOT NULL
        CONSTRAINT fk_process_event_transition_process REFERENCES process (id),
    event            varchar(255) NOT NULL,
    user_id          uuid         NOT NULL,
    old_state        varchar(255),
    new_state        varchar(255),
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    version          bigint       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_process_event_transition_merchant_id ON process_event_transition (merchant_id);

CREATE TABLE IF NOT EXISTS journal
(
    id       bigserial    NOT NULL PRIMARY KEY,
    merchant_id uuid     NOT NULL,
    name     varchar(255) NOT NULL,
    start    date,
    end_     date,
    closed   boolean      NOT NULL DEFAULT false,
    lockdate date,
    chart    bigint       NOT NULL
        CONSTRAINT fk_journal_chart REFERENCES account (id),
    CONSTRAINT ux_journal_name_merchant UNIQUE (name, merchant_id)
);

CREATE INDEX IF NOT EXISTS idx_journal_merchant_id ON journal (merchant_id);

CREATE TABLE IF NOT EXISTS layer
(
    id            smallint    NOT NULL,
    merchant_id   uuid        NOT NULL,
    journal       bigint      NOT NULL
        CONSTRAINT fk_layer_journal REFERENCES journal (id),
    name          varchar(80),
    currency_code varchar(10),
    PRIMARY KEY (id, journal)
);

CREATE INDEX IF NOT EXISTS idx_layer_merchant_id ON layer (merchant_id);

CREATE TABLE IF NOT EXISTS rule_info
(
    id          bigserial    NOT NULL PRIMARY KEY,
    merchant_id uuid         NOT NULL,
    description varchar(255),
    rule_type   varchar(50),
    layers      varchar(255),
    param       varchar(255),
    journal     bigint
        CONSTRAINT FKRuleInfoJournal REFERENCES journal (id),
    account     bigint
        CONSTRAINT FKRuleInfoAccount REFERENCES account (id)
);

CREATE INDEX IF NOT EXISTS idx_rule_info_merchant_id ON rule_info (merchant_id);

CREATE TABLE IF NOT EXISTS account_lock
(
    merchant_id uuid   NOT NULL,
    journal bigint NOT NULL
        CONSTRAINT FKAccountLockJournal REFERENCES journal (id),
    account bigint NOT NULL
        CONSTRAINT FKAccountLockAccount REFERENCES account (id),
    PRIMARY KEY (journal, account)
);

CREATE INDEX IF NOT EXISTS idx_account_lock_merchant_id ON account_lock (merchant_id);

CREATE TABLE IF NOT EXISTS balance_cache
(
    merchant_id uuid         NOT NULL,
    journal bigint       NOT NULL
        CONSTRAINT FKBalanceCacheJournal REFERENCES journal (id),
    account bigint       NOT NULL
        CONSTRAINT FKBalanceCacheAccount REFERENCES account (id),
    layers  varchar(255) NOT NULL,
    ref     bigint,
    balance numeric(14, 2),
    PRIMARY KEY (journal, account, layers)
);

CREATE INDEX IF NOT EXISTS idx_balance_cache_merchant_id ON balance_cache (merchant_id);

CREATE TABLE IF NOT EXISTS checkpoint
(
    merchant_id uuid         NOT NULL,
    date    timestamp    NOT NULL,
    layers  varchar(255) NOT NULL,
    journal bigint       NOT NULL
        CONSTRAINT FKCheckpointJournal REFERENCES journal (id),
    account bigint       NOT NULL
        CONSTRAINT FKCheckpointAccount REFERENCES account (id),
    balance numeric(14, 2),
    PRIMARY KEY (date, layers, journal, account)
);

CREATE INDEX IF NOT EXISTS idx_checkpoint_merchant_id ON checkpoint (merchant_id);

CREATE TABLE IF NOT EXISTS latest_balance_snapshot
(
    merchant_id uuid           NOT NULL,
    journal  bigint         NOT NULL,
    account  varchar(255)   NOT NULL,
    layers   varchar(255)   NOT NULL,
    balance  numeric(14, 2) NOT NULL DEFAULT 0,
    currency varchar(10)    NOT NULL,
    PRIMARY KEY (journal, account, layers)
);

CREATE INDEX IF NOT EXISTS idx_latest_balance_snapshot_merchant_id ON latest_balance_snapshot (merchant_id);

CREATE TABLE IF NOT EXISTS transaction
(
    id                        bigserial    NOT NULL PRIMARY KEY,
    merchant_id               uuid         NOT NULL,
    completed_date            timestamp    NULL,
    type                      varchar(255) NOT NULL,
    transaction_group         varchar(255) NOT NULL DEFAULT 'TRANSFER',
    sender_amount             numeric      NOT NULL,
    sender_currency           varchar(255) NOT NULL,
    recipient_amount          numeric      NOT NULL,
    recipient_currency        varchar(255) NOT NULL,
    exchange_rate             numeric      NOT NULL DEFAULT 1,
    fee                       numeric      NOT NULL DEFAULT 0,
    commission                numeric      NOT NULL DEFAULT 0,
    rebate                    numeric      NOT NULL DEFAULT 0,
    description               text,
    display_ref               varchar(30)  NOT NULL,
    internal_reference        uuid         NOT NULL,
    external_reference        varchar(255),
    status                    varchar(255),
    channel                   varchar(255),
    lend                      boolean      NOT NULL DEFAULT false,
    reconciled                boolean      NOT NULL DEFAULT false,
    sender_running_balance    numeric      NOT NULL DEFAULT 0,
    recipient_running_balance numeric      NOT NULL DEFAULT 0,
    reversed_by               bigint
        CONSTRAINT fk_transaction_reversed_by REFERENCES transaction (id),
    reverses                  bigint
        CONSTRAINT fk_transaction_reverses REFERENCES transaction (id),
    process_id                uuid         NOT NULL
        CONSTRAINT fk_transaction_process_id REFERENCES process (public_id),
    sender_account_id         uuid         NOT NULL
        CONSTRAINT fk_transaction_sender_account REFERENCES account (public_id),
    recipient_account_id      uuid         NOT NULL
        CONSTRAINT fk_transaction_recipient_account REFERENCES account (public_id),
    post_date                 timestamp,
    journal_id                bigint
        CONSTRAINT fk_transaction_journal REFERENCES journal (id),
    created_at                timestamp    NOT NULL DEFAULT now(),
    last_modified_at          timestamp    NOT NULL DEFAULT now(),
    created_by                varchar(255) NOT NULL,
    last_modified_by          varchar(255) NOT NULL,
    version                   bigint       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_transaction_internal_reference ON transaction (internal_reference);
CREATE INDEX IF NOT EXISTS idx_transaction_external_reference ON transaction (external_reference);
CREATE INDEX IF NOT EXISTS idx_transaction_display_ref ON transaction (display_ref);
CREATE INDEX IF NOT EXISTS idx_transaction_status_journal ON transaction (journal_id, status, post_date);
CREATE INDEX IF NOT EXISTS idx_transaction_merchant_id ON transaction (merchant_id);

CREATE TABLE IF NOT EXISTS transaction_group
(
    id          bigserial NOT NULL PRIMARY KEY,
    merchant_id uuid     NOT NULL,
    reference   uuid     NOT NULL
);

CREATE INDEX IF NOT EXISTS transaction_group_name ON transaction_group (reference);
CREATE INDEX IF NOT EXISTS idx_transaction_group_merchant_id ON transaction_group (merchant_id);

CREATE TABLE IF NOT EXISTS transaction_group_transactions
(
    transaction_group bigint NOT NULL
        CONSTRAINT FKTransactionTransactionGroup REFERENCES transaction_group (id),
    transaction       bigint NOT NULL
        CONSTRAINT FKTransactionGroupTransaction REFERENCES transaction (id),
    PRIMARY KEY (transaction_group, transaction)
);

CREATE TABLE IF NOT EXISTS transaction_entry
(
    id          bigserial      NOT NULL PRIMARY KEY,
    merchant_id uuid           NOT NULL,
    subclass    varchar(1)     NOT NULL DEFAULT ' ',
    detail      text,
    is_credit   boolean        NOT NULL DEFAULT false,
    layer       int            NOT NULL DEFAULT 0,
    account     bigint         NOT NULL
        CONSTRAINT fk_transaction_entry_account REFERENCES account (id),
    transaction bigint
        CONSTRAINT fk_transaction_entry_transaction REFERENCES transaction (id),
    amount      numeric(14, 2) NOT NULL,
    currency    varchar(10)    NOT NULL,
    posn        int
);

CREATE INDEX IF NOT EXISTS idx_acct ON transaction_entry (account);
CREATE INDEX IF NOT EXISTS idx_txn ON transaction_entry (transaction);
CREATE INDEX IF NOT EXISTS idx_transaction_entry_limit_lookup ON transaction_entry (account, layer) WHERE is_credit = false;
CREATE INDEX IF NOT EXISTS idx_transaction_entry_checkpoint ON transaction_entry (account, layer, id) WHERE is_credit = false;
CREATE INDEX IF NOT EXISTS idx_transaction_entry_merchant_id ON transaction_entry (merchant_id);

CREATE TABLE IF NOT EXISTS transaction_entry_property
(
    entry_id         bigint       NOT NULL
        CONSTRAINT fk_transaction_entry_property_entry REFERENCES transaction_entry (id),
    name             varchar(255) NOT NULL,
    value            text         NOT NULL,
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    version          bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY (entry_id, name)
);

CREATE TABLE IF NOT EXISTS transaction_property
(
    transaction_id   bigint       NOT NULL,
    name             varchar(255) NOT NULL,
    value            text         NOT NULL,
    property_group   varchar(255) NOT NULL,
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    version          bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY (name, transaction_id),
    FOREIGN KEY (transaction_id) REFERENCES transaction (id)
);

CREATE TABLE IF NOT EXISTS transaction_reference_sequences
(
    date_key       date    NOT NULL,
    merchant_id    uuid    NOT NULL,
    sequence_value integer NOT NULL DEFAULT 0,
    PRIMARY KEY (date_key, merchant_id)
);

CREATE INDEX IF NOT EXISTS idx_transaction_reference_sequences_merchant_id ON transaction_reference_sequences (merchant_id);

CREATE TABLE IF NOT EXISTS template_limit
(
    id                     bigserial                NOT NULL PRIMARY KEY,
    template_name          varchar(100)             NOT NULL,
    profile_id             uuid                     NOT NULL,
    currency               varchar(3)               NOT NULL,
    max_daily_debit        numeric(19, 4)           NOT NULL,
    max_daily_credit       numeric(19, 4)           NOT NULL,
    cumulative_debit       numeric(19, 4)           NOT NULL,
    cumulative_credit      numeric(19, 4)           NOT NULL,
    min_transaction_debit  numeric(19, 4)           NOT NULL,
    min_transaction_credit numeric(19, 4)           NOT NULL,
    max_transaction_debit  numeric(19, 4)           NOT NULL,
    max_transaction_credit numeric(19, 4)           NOT NULL,
    max_account_balance    numeric(19, 4)           NOT NULL,
    valid_from             timestamp with time zone NOT NULL,
    valid_to               timestamp with time zone,
    created_at             timestamp with time zone          DEFAULT CURRENT_TIMESTAMP,
    last_modified_at       timestamp with time zone          DEFAULT CURRENT_TIMESTAMP,
    created_by             varchar(255),
    last_modified_by       varchar(255),
    version                bigint                            DEFAULT 0,
    CONSTRAINT uk_template_limit UNIQUE (template_name, profile_id, currency)
);

CREATE INDEX IF NOT EXISTS idx_template_limit_lookup
    ON template_limit (template_name, profile_id, currency, valid_from, valid_to);

CREATE TABLE IF NOT EXISTS limit_checkpoint
(
    id               bigserial      NOT NULL PRIMARY KEY,
    merchant_id      uuid           NOT NULL,
    journal          bigint         NOT NULL,
    account          bigint         NOT NULL,
    layer            smallint       NOT NULL,
    cumulative_debit numeric(19, 4) NOT NULL,
    last_entry_id    bigint         NOT NULL,
    entry_count      bigint         NOT NULL DEFAULT 0,
    created_at       timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_limit_checkpoint UNIQUE (journal, account, layer),
    CONSTRAINT fk_limit_checkpoint_journal FOREIGN KEY (journal) REFERENCES journal (id),
    CONSTRAINT fk_limit_checkpoint_account FOREIGN KEY (account) REFERENCES account (id)
);

CREATE INDEX IF NOT EXISTS idx_limit_checkpoint_merchant_id ON limit_checkpoint (merchant_id);

CREATE TABLE IF NOT EXISTS banking_integration_log
(
    id                 bigserial   NOT NULL PRIMARY KEY,
    merchant_id        uuid        NOT NULL,
    session_id         text,
    request            text,
    response           text,
    error              text,
    external_reference varchar(255),
    internal_reference uuid,
    inbound            boolean     NOT NULL,
    created_by         varchar(50) NOT NULL,
    created_at         timestamp,
    last_modified_by   varchar(50),
    last_modified_at   timestamp,
    version            bigint      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_banking_integration_log_merchant_id ON banking_integration_log (merchant_id);

CREATE TABLE IF NOT EXISTS integration_config
(
    id               bigserial    NOT NULL PRIMARY KEY,
    merchant_id      uuid         NOT NULL,
    identifier       varchar(255) NOT NULL,
    priority         int          NOT NULL,
    action           varchar(255) NOT NULL,
    status           varchar(255) NOT NULL,
    exchange_id      varchar(255) NOT NULL,
    public_id        uuid         NOT NULL DEFAULT gen_random_uuid(),
    start            timestamp    NOT NULL DEFAULT now(),
    expiry           timestamp,
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    version          bigint       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_integration_config_merchant_id ON integration_config (merchant_id);

CREATE TABLE IF NOT EXISTS access_token
(
    id               bigserial    NOT NULL PRIMARY KEY,
    merchant_id      uuid         NOT NULL,
    type             varchar(255) NOT NULL,
    expiry           timestamp    NOT NULL,
    access_token     text         NOT NULL,
    refresh_token    text,
    resource         varchar(255) NOT NULL,
    institution      varchar(255) NOT NULL,
    created_by       varchar(50)  NOT NULL DEFAULT 'system',
    created_at       timestamp             DEFAULT now(),
    last_modified_by varchar(50)           DEFAULT now(),
    last_modified_at timestamp             DEFAULT now(),
    version          bigint       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_access_token_merchant_id ON access_token (merchant_id);

CREATE TABLE IF NOT EXISTS notification_device
(
    id                   bigserial   NOT NULL PRIMARY KEY,
    merchant_id          uuid        NOT NULL,
    public_id            uuid        NOT NULL,
    notification_channel varchar(50) NOT NULL,
    value                text        NOT NULL,
    user_id              uuid        NOT NULL,
    notification_type    varchar(50) NOT NULL,
    created_by           varchar(50) NOT NULL DEFAULT 'system',
    created_at           timestamp            DEFAULT now(),
    last_modified_by     varchar(50)          DEFAULT now(),
    last_modified_at     timestamp            DEFAULT now(),
    version              bigint      NOT NULL DEFAULT 0,
    UNIQUE (notification_type, notification_channel, user_id)
);

CREATE INDEX IF NOT EXISTS idx_notification_device_merchant_id ON notification_device (merchant_id);

CREATE TABLE IF NOT EXISTS system_property
(
    id               serial       NOT NULL PRIMARY KEY,
    name             varchar(100) NOT NULL,
    scope            varchar(255) NOT NULL,
    value            text         NOT NULL,
    created_by       varchar(50)  NOT NULL,
    created_at       timestamp,
    last_modified_by varchar(50),
    last_modified_at timestamp,
    version          bigint       NOT NULL,
    CONSTRAINT system_property_unique_name_scope UNIQUE (name, scope)
);

CREATE TABLE IF NOT EXISTS currency
(
    id               serial       NOT NULL PRIMARY KEY,
    name             varchar(255) NOT NULL,
    code             varchar(10)  NOT NULL UNIQUE,
    major_single     varchar(20)  NOT NULL,
    major_plural     varchar(50)  NOT NULL,
    iso_num          int          NOT NULL,
    symbol           varchar(255) NOT NULL,
    symbol_native    varchar(255) NOT NULL,
    minor_single     varchar(50)  NOT NULL,
    minor_plural     varchar(50)  NOT NULL,
    iso_digits       int          NOT NULL,
    decimals         int          NOT NULL,
    num_to_basic     int          NOT NULL,
    enabled          boolean      NOT NULL DEFAULT false,
    image_url        varchar(255) NOT NULL DEFAULT '',
    type             varchar(50)  NOT NULL DEFAULT 'FIAT',
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    version          bigint       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS fiat_currency_crypto_mapping
(
    id               serial       NOT NULL PRIMARY KEY,
    fiat_currency    varchar(10)  NOT NULL,
    crypto_currency  varchar(10)  NOT NULL,
    integrator_id    varchar(255) NOT NULL,
    issuer           varchar(255) NOT NULL,
    platform         varchar(50)  NOT NULL,
    is_default       boolean      NOT NULL DEFAULT false,
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL DEFAULT 'system',
    last_modified_by varchar(255) NOT NULL DEFAULT 'system',
    version          bigint       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_fiat_crypto_mapping_fiat_integrator
    ON fiat_currency_crypto_mapping (fiat_currency, integrator_id);

CREATE TABLE IF NOT EXISTS currency_token_mapping
(
    id               bigserial    NOT NULL PRIMARY KEY,
    merchant_id      uuid         NOT NULL,
    public_id        uuid         NOT NULL DEFAULT gen_random_uuid(),
    exchange_id      varchar(255) NOT NULL,
    fiat_currency    varchar(10)  NOT NULL,
    token_symbol     varchar(20)  NOT NULL,
    token_name       varchar(100),
    network          varchar(50),
    priority         int          NOT NULL DEFAULT 0,
    is_active        boolean      NOT NULL DEFAULT true,
    version          bigint       NOT NULL DEFAULT 0,
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL DEFAULT 'system',
    last_modified_by varchar(255) NOT NULL DEFAULT 'system',
    UNIQUE (exchange_id, fiat_currency, token_symbol)
);

CREATE INDEX IF NOT EXISTS idx_currency_token_mapping_fiat ON currency_token_mapping (fiat_currency);
CREATE INDEX IF NOT EXISTS idx_currency_token_mapping_exchange ON currency_token_mapping (exchange_id, fiat_currency, is_active);
CREATE INDEX IF NOT EXISTS idx_currency_token_mapping_merchant_id ON currency_token_mapping (merchant_id);

CREATE TABLE IF NOT EXISTS integration_monitoring_log
(
    id                bigserial    NOT NULL PRIMARY KEY,
    merchant_id       uuid         NOT NULL,
    public_id         uuid         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    integration_id    varchar(255) NOT NULL,
    method_name       varchar(255) NOT NULL,
    request_body      text,
    response_body     text,
    error_message     text,
    duration_ms       bigint       NOT NULL,
    status            varchar(50)  NOT NULL,
    direction         varchar(50)  NOT NULL,
    process_reference uuid,
    version           bigint       NOT NULL DEFAULT 0,
    created_at        timestamp    NOT NULL DEFAULT now(),
    last_modified_at  timestamp    NOT NULL DEFAULT now(),
    created_by        varchar(255) NOT NULL DEFAULT 'system',
    last_modified_by  varchar(255) NOT NULL DEFAULT 'system'
);

CREATE INDEX IF NOT EXISTS idx_integration_log_integration_id ON integration_monitoring_log (integration_id);
CREATE INDEX IF NOT EXISTS idx_integration_log_created_at ON integration_monitoring_log (created_at);
CREATE INDEX IF NOT EXISTS idx_integration_log_status ON integration_monitoring_log (status);
CREATE INDEX IF NOT EXISTS idx_integration_log_process_ref ON integration_monitoring_log (process_reference);
CREATE INDEX IF NOT EXISTS idx_integration_log_direction ON integration_monitoring_log (direction);
CREATE INDEX IF NOT EXISTS idx_integration_log_method ON integration_monitoring_log (integration_id, method_name);
CREATE INDEX IF NOT EXISTS idx_integration_monitoring_log_merchant_id ON integration_monitoring_log (merchant_id);

CREATE TABLE IF NOT EXISTS transaction_template (
    id                   bigserial    PRIMARY KEY,
    public_id            uuid         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    name                 varchar(100) NOT NULL UNIQUE,
    transaction_group    varchar(50)  NOT NULL,
    is_pending           boolean      NOT NULL DEFAULT false,
    entry_type_filter    varchar(50),
    priority             int          NOT NULL DEFAULT 0,
    is_active            boolean      NOT NULL DEFAULT true,
    description          text,
    account_charged      varchar(20)           DEFAULT 'SENDER',
    process_type         varchar(50),
    required_account_roles text,
    version              bigint       NOT NULL DEFAULT 0,
    created_at           timestamp    NOT NULL DEFAULT now(),
    last_modified_at     timestamp    NOT NULL DEFAULT now(),
    created_by           varchar(255) NOT NULL,
    last_modified_by     varchar(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transaction_template_group ON transaction_template(transaction_group);
CREATE INDEX IF NOT EXISTS idx_transaction_template_active ON transaction_template(is_active);

CREATE TABLE IF NOT EXISTS template_entry_rule (
    id                     bigserial    PRIMARY KEY,
    template_id            bigint       NOT NULL REFERENCES transaction_template(id),
    sequence               int          NOT NULL,
    phase                  varchar(50)  NOT NULL,
    source_account_role    varchar(50)  NOT NULL,
    target_account_role    varchar(50)  NOT NULL,
    entry_leg              varchar(10)  NOT NULL,
    layer_type             varchar(50)  NOT NULL,
    amount_type            varchar(20)  NOT NULL DEFAULT 'FULL',
    amount_value           numeric,
    condition_expression   text,
    tag_name               varchar(50),
    tag_value_role         varchar(50),
    is_active              boolean      NOT NULL DEFAULT true,
    version                bigint       NOT NULL DEFAULT 0,
    created_at             timestamp    NOT NULL DEFAULT now(),
    last_modified_at       timestamp    NOT NULL DEFAULT now(),
    created_by             varchar(255) NOT NULL,
    last_modified_by       varchar(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_template_entry_rule_template ON template_entry_rule(template_id);
CREATE INDEX IF NOT EXISTS idx_template_entry_rule_sequence ON template_entry_rule(template_id, sequence);

CREATE TABLE IF NOT EXISTS template_completion_rule (
    id                     bigserial    PRIMARY KEY,
    template_id            bigint       NOT NULL REFERENCES transaction_template(id),
    sequence               int          NOT NULL,
    action_type            varchar(50)  NOT NULL,
    source_tag_type        varchar(50),
    reversal_layer_types   varchar(255),
    is_active              boolean      NOT NULL DEFAULT true,
    version                bigint       NOT NULL DEFAULT 0,
    created_at             timestamp    NOT NULL DEFAULT now(),
    last_modified_at       timestamp    NOT NULL DEFAULT now(),
    created_by             varchar(255) NOT NULL,
    last_modified_by       varchar(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_template_completion_rule_template ON template_completion_rule(template_id);
CREATE INDEX IF NOT EXISTS idx_template_completion_rule_sequence ON template_completion_rule(template_id, sequence);

CREATE TABLE IF NOT EXISTS journal_opening_balance_status (
    id                   bigserial    PRIMARY KEY,
    merchant_id          uuid         NOT NULL,
    target_journal_id    bigint       NOT NULL REFERENCES journal(id),
    source_journal_id    bigint       NOT NULL REFERENCES journal(id),
    transaction_id       bigint       REFERENCES transaction(id),
    status               varchar(20)  NOT NULL DEFAULT 'PENDING',
    entries_created      int          NOT NULL DEFAULT 0,
    created_at           timestamp    NOT NULL DEFAULT now(),
    completed_at         timestamp,
    UNIQUE (target_journal_id, source_journal_id)
);

CREATE INDEX IF NOT EXISTS idx_journal_ob_status_target ON journal_opening_balance_status(target_journal_id);
CREATE INDEX IF NOT EXISTS idx_journal_ob_status_source ON journal_opening_balance_status(source_journal_id);
CREATE INDEX IF NOT EXISTS idx_journal_ob_status_status ON journal_opening_balance_status(status);
CREATE INDEX IF NOT EXISTS idx_journal_ob_status_merchant_id ON journal_opening_balance_status(merchant_id);

CREATE TABLE IF NOT EXISTS account_rollover_status (
    id                   bigserial    PRIMARY KEY,
    merchant_id          uuid         NOT NULL,
    target_journal_id    bigint       NOT NULL REFERENCES journal(id),
    account_id           bigint       NOT NULL REFERENCES account(id),
    year                 int          NOT NULL,
    status               varchar(20)  NOT NULL DEFAULT 'PENDING',
    created_at           timestamp    NOT NULL DEFAULT now(),
    completed_at         timestamp,
    UNIQUE (target_journal_id, account_id, year)
);

CREATE INDEX IF NOT EXISTS idx_account_rollover_journal ON account_rollover_status(target_journal_id);
CREATE INDEX IF NOT EXISTS idx_account_rollover_account ON account_rollover_status(account_id);
CREATE INDEX IF NOT EXISTS idx_account_rollover_status ON account_rollover_status(status);
CREATE INDEX IF NOT EXISTS idx_account_rollover_year ON account_rollover_status(year);
CREATE INDEX IF NOT EXISTS idx_account_rollover_merchant_id ON account_rollover_status(merchant_id);

CREATE TABLE IF NOT EXISTS campaign (
    id                           bigserial PRIMARY KEY,
    public_id                    uuid NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    name                         varchar(255) NOT NULL,
    type                         varchar(50) NOT NULL,
    value                        numeric(14, 4) NOT NULL,
    min_purchase_amount          numeric(14, 2),
    max_redemption_amount        numeric(14, 2),
    max_total_redemptions        int,
    max_redemptions_per_customer int,
    start_date                   timestamp NOT NULL,
    end_date                     timestamp,
    status                       varchar(50) NOT NULL DEFAULT 'ACTIVE',
    redemption_count             int NOT NULL DEFAULT 0,
    merchant_id                  uuid NOT NULL,
    currency                     varchar(10) NOT NULL,
    is_merchant_wide             boolean NOT NULL DEFAULT true,
    created_at                   timestamp NOT NULL DEFAULT now(),
    last_modified_at             timestamp NOT NULL DEFAULT now(),
    created_by                   varchar(255) NOT NULL,
    last_modified_by             varchar(255) NOT NULL,
    version                      bigint NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_campaign_merchant_status ON campaign (merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_campaign_status_dates ON campaign (status, start_date, end_date);

CREATE TABLE IF NOT EXISTS campaign_eligibility (
    id               bigserial PRIMARY KEY,
    merchant_id      uuid NOT NULL,
    public_id        uuid NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    campaign_id      bigint NOT NULL REFERENCES campaign (id),
    customer_id      uuid NOT NULL,
    redemption_count int NOT NULL DEFAULT 0,
    is_active        boolean NOT NULL DEFAULT true,
    created_at       timestamp NOT NULL DEFAULT now(),
    last_modified_at timestamp NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    version          bigint NOT NULL DEFAULT 0,
    UNIQUE (campaign_id, customer_id)
);

CREATE INDEX IF NOT EXISTS idx_campaign_eligibility_customer ON campaign_eligibility (customer_id);
CREATE INDEX IF NOT EXISTS idx_campaign_eligibility_merchant_id ON campaign_eligibility (merchant_id);

CREATE TABLE IF NOT EXISTS campaign_redemption (
    id                    bigserial PRIMARY KEY,
    merchant_id           uuid NOT NULL,
    public_id             uuid NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    campaign_id           bigint NOT NULL REFERENCES campaign (id),
    customer_id           uuid NOT NULL,
    transaction_reference uuid NOT NULL,
    original_amount       numeric(14, 2) NOT NULL,
    campaign_amount       numeric(14, 2) NOT NULL,
    currency              varchar(10) NOT NULL,
    redeemed_at           timestamp NOT NULL DEFAULT now(),
    created_at            timestamp NOT NULL DEFAULT now(),
    last_modified_at      timestamp NOT NULL DEFAULT now(),
    created_by            varchar(255) NOT NULL,
    last_modified_by      varchar(255) NOT NULL,
    version               bigint NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_campaign_redemption_campaign ON campaign_redemption (campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_redemption_customer ON campaign_redemption (customer_id);
CREATE INDEX IF NOT EXISTS idx_campaign_redemption_transaction ON campaign_redemption (transaction_reference);
CREATE INDEX IF NOT EXISTS idx_campaign_redemption_merchant_id ON campaign_redemption (merchant_id);

CREATE TABLE IF NOT EXISTS chart_of_accounts_template (
    id               bigserial    NOT NULL PRIMARY KEY,
    public_id        uuid         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    name             varchar(100) NOT NULL UNIQUE,
    description      text,
    industry_type    varchar(50)  NOT NULL,
    template_json    text         NOT NULL,
    is_active        boolean      NOT NULL DEFAULT true,
    version          bigint       NOT NULL DEFAULT 1,
    created_at       timestamp    NOT NULL DEFAULT now(),
    last_modified_at timestamp    NOT NULL DEFAULT now(),
    created_by       varchar(255) NOT NULL DEFAULT 'system',
    last_modified_by varchar(255) NOT NULL DEFAULT 'system'
);

CREATE INDEX IF NOT EXISTS idx_coa_template_industry ON chart_of_accounts_template (industry_type);
CREATE INDEX IF NOT EXISTS idx_coa_template_active ON chart_of_accounts_template (is_active);

CREATE TABLE IF NOT EXISTS retry_audit (
    id                bigserial PRIMARY KEY,
    event_type        varchar(255) NOT NULL,
    listener_class    varchar(255) NOT NULL,
    attempt_number    integer      NOT NULL,
    max_attempts      integer      NOT NULL,
    exception_message text,
    status            varchar(20)  NOT NULL,
    created_at        timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_retry_audit_event_type ON retry_audit (event_type);
CREATE INDEX IF NOT EXISTS idx_retry_audit_status ON retry_audit (status);
CREATE INDEX IF NOT EXISTS idx_retry_audit_created_at ON retry_audit (created_at);

CREATE TABLE IF NOT EXISTS QRTZ_JOB_DETAILS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(200) NOT NULL,
    JOB_GROUP         VARCHAR(200) NOT NULL,
    DESCRIPTION       VARCHAR(250),
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        BOOLEAN NOT NULL,
    IS_NONCONCURRENT  BOOLEAN NOT NULL,
    IS_UPDATE_DATA    BOOLEAN NOT NULL,
    REQUESTS_RECOVERY BOOLEAN NOT NULL,
    JOB_DATA          BYTEA,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_TRIGGERS (
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(200) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    JOB_NAME       VARCHAR(200) NOT NULL,
    JOB_GROUP      VARCHAR(200) NOT NULL,
    DESCRIPTION    VARCHAR(250),
    NEXT_FIRE_TIME BIGINT,
    PREV_FIRE_TIME BIGINT,
    PRIORITY       INTEGER,
    TRIGGER_STATE  VARCHAR(16) NOT NULL,
    TRIGGER_TYPE   VARCHAR(8) NOT NULL,
    START_TIME     BIGINT NOT NULL,
    END_TIME       BIGINT,
    CALENDAR_NAME  VARCHAR(200),
    MISFIRE_INSTR  SMALLINT,
    JOB_DATA       BYTEA,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    REPEAT_COUNT    BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_CRON_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1    VARCHAR(512),
    STR_PROP_2    VARCHAR(512),
    STR_PROP_3    VARCHAR(512),
    INT_PROP_1    INTEGER,
    INT_PROP_2    INTEGER,
    LONG_PROP_1   BIGINT,
    LONG_PROP_2   BIGINT,
    DEC_PROP_1    NUMERIC(13, 4),
    DEC_PROP_2    NUMERIC(13, 4),
    BOOL_PROP_1   BOOLEAN,
    BOOL_PROP_2   BOOLEAN,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_BLOB_TRIGGERS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA     BYTEA,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_CALENDARS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    CALENDAR      BYTEA NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);

CREATE TABLE IF NOT EXISTS QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_FIRED_TRIGGERS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(140) NOT NULL,
    TRIGGER_NAME      VARCHAR(200) NOT NULL,
    TRIGGER_GROUP     VARCHAR(200) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    FIRED_TIME        BIGINT NOT NULL,
    SCHED_TIME        BIGINT NOT NULL,
    PRIORITY          INTEGER NOT NULL,
    STATE             VARCHAR(16) NOT NULL,
    JOB_NAME          VARCHAR(200),
    JOB_GROUP         VARCHAR(200),
    IS_NONCONCURRENT  BOOLEAN,
    REQUESTS_RECOVERY BOOLEAN,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE IF NOT EXISTS QRTZ_SCHEDULER_STATE (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL  BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE IF NOT EXISTS QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

CREATE INDEX IF NOT EXISTS IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS (SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_J ON QRTZ_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_JG ON QRTZ_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_C ON QRTZ_TRIGGERS (SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_G ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS (SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
