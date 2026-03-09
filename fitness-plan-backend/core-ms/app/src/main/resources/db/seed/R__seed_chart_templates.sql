DELETE FROM chart_of_accounts_template WHERE name IN ('E_WALLET_BASIC', 'REMITTANCE_STANDARD');

INSERT INTO chart_of_accounts_template (name, description, industry_type, template_json, is_active, created_by, last_modified_by)
VALUES ('STANDARD', 'Standard chart of accounts', 'GENERAL',
'{
  "strict_accounts_code": false,
  "currencies": [],
  "journals": [],
  "chart_of_accounts": {
    "code": "STANDARD",
    "description": "Standard Chart of Accounts",
    "created": "2025-01-01T00:00:00Z",
    "composite_accounts": [
      {
        "code": "assets",
        "type": "DEBIT",
        "description": "Assets",
        "currency": "USD",
        "accounts": [
          {"code": "cash", "type": "DEBIT", "description": "Cash and Bank Accounts", "account_type": "CASH"}
        ],
        "composite_accounts": []
      },
      {
        "code": "liabilities",
        "type": "CREDIT",
        "description": "Liabilities",
        "currency": "USD",
        "accounts": [],
        "composite_accounts": [
          {
            "code": "customer-liabilities",
            "type": "CREDIT",
            "description": "Customer Balances",
            "currency": "USD",
            "accounts": [],
            "composite_accounts": []
          }
        ]
      },
      {
        "code": "equity",
        "type": "CREDIT",
        "description": "Equity",
        "currency": "USD",
        "accounts": [
          {"code": "retained-earnings", "type": "CREDIT", "description": "Retained Earnings", "account_type": "EQUITY"},
          {"code": "capital", "type": "CREDIT", "description": "Capital", "account_type": "EQUITY"}
        ],
        "composite_accounts": []
      },
      {
        "code": "revenue",
        "type": "CREDIT",
        "description": "Revenue",
        "currency": "USD",
        "accounts": [
          {"code": "transaction-fees", "type": "CREDIT", "description": "Transaction Fees", "account_type": "FEE"},
          {"code": "commission-income", "type": "CREDIT", "description": "Commission Income", "account_type": "FEE"},
          {"code": "interest-income", "type": "CREDIT", "description": "Interest Income", "account_type": "REVENUE"}
        ],
        "composite_accounts": []
      },
      {
        "code": "expenses",
        "type": "DEBIT",
        "description": "Expenses",
        "currency": "USD",
        "accounts": [
          {"code": "payment-processor-fees", "type": "DEBIT", "description": "Payment Processor Fees", "account_type": "EXPENSE"},
          {"code": "bank-charges", "type": "DEBIT", "description": "Bank Charges", "account_type": "EXPENSE"},
          {"code": "operational-costs", "type": "DEBIT", "description": "Operational Costs", "account_type": "EXPENSE"}
        ],
        "composite_accounts": []
      },
      {
        "code": "conversions",
        "type": "DEBIT",
        "description": "Currency Conversions",
        "currency": "USD",
        "accounts": [
          {"code": "forex-gains-losses", "type": "DEBIT", "description": "Foreign Exchange Gains/Losses", "account_type": "CONVERSION"}
        ],
        "composite_accounts": []
      }
    ]
  }
}', true, 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO UPDATE SET template_json = EXCLUDED.template_json, description = EXCLUDED.description;

INSERT INTO chart_of_accounts_template (name, description, industry_type, template_json, is_active, created_by, last_modified_by)
VALUES ('FINTECH_STANDARD', 'Standard fintech chart of accounts', 'FINTECH',
'{
  "strict_accounts_code": false,
  "currencies": [],
  "journals": [],
  "chart_of_accounts": {
    "code": "FINTECH",
    "description": "Fintech Standard Chart of Accounts",
    "created": "2025-01-01T00:00:00Z",
    "composite_accounts": [
      {
        "code": "assets",
        "type": "DEBIT",
        "description": "Assets",
        "currency": "USD",
        "accounts": [
          {"code": "cash", "type": "DEBIT", "description": "Cash and Bank Accounts", "account_type": "CASH"},
          {"code": "receivables", "type": "DEBIT", "description": "Accounts Receivable", "account_type": "CASH"}
        ],
        "composite_accounts": []
      },
      {
        "code": "liabilities",
        "type": "CREDIT",
        "description": "Liabilities",
        "currency": "USD",
        "accounts": [],
        "composite_accounts": [
          {
            "code": "customer-liabilities",
            "type": "CREDIT",
            "description": "Customer Balances",
            "currency": "USD",
            "accounts": [],
            "composite_accounts": []
          }
        ]
      },
      {
        "code": "equity",
        "type": "CREDIT",
        "description": "Equity",
        "currency": "USD",
        "accounts": [
          {"code": "retained-earnings", "type": "CREDIT", "description": "Retained Earnings", "account_type": "EQUITY"},
          {"code": "capital", "type": "CREDIT", "description": "Capital", "account_type": "EQUITY"}
        ],
        "composite_accounts": []
      },
      {
        "code": "revenue",
        "type": "CREDIT",
        "description": "Revenue",
        "currency": "USD",
        "accounts": [
          {"code": "transaction-fees", "type": "CREDIT", "description": "Transaction Fees", "account_type": "FEE"},
          {"code": "commission-income", "type": "CREDIT", "description": "Commission Income", "account_type": "FEE"},
          {"code": "interest-income", "type": "CREDIT", "description": "Interest Income", "account_type": "REVENUE"}
        ],
        "composite_accounts": []
      },
      {
        "code": "expenses",
        "type": "DEBIT",
        "description": "Expenses",
        "currency": "USD",
        "accounts": [
          {"code": "payment-processor-fees", "type": "DEBIT", "description": "Payment Processor Fees", "account_type": "EXPENSE"},
          {"code": "bank-charges", "type": "DEBIT", "description": "Bank Charges", "account_type": "EXPENSE"},
          {"code": "operational-costs", "type": "DEBIT", "description": "Operational Costs", "account_type": "EXPENSE"}
        ],
        "composite_accounts": []
      },
      {
        "code": "conversions",
        "type": "DEBIT",
        "description": "Currency Conversions",
        "currency": "USD",
        "accounts": [
          {"code": "forex-gains-losses", "type": "DEBIT", "description": "Foreign Exchange Gains/Losses", "account_type": "CONVERSION"}
        ],
        "composite_accounts": []
      }
    ]
  }
}', true, 'system', 'system')
ON CONFLICT (name) WHERE merchant_id IS NULL DO UPDATE SET template_json = EXCLUDED.template_json, description = EXCLUDED.description;
