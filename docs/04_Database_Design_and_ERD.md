# 04 — Database Design and ERD

**Product:** Cedar POS · **Version:** 1.0 (planning) · **Date:** 2026-11-08
**Engine:** PostgreSQL 18 (central) · SQLite (desktop & mobile local)
**Editable sources:** `database/cedar_pos.dbml` (dbdiagram.io) · Mermaid blocks in this document (`diagrams/*.mmd`)

---

## 1. Design principles

| # | Principle | What it means in practice |
|---|---|---|
| P1 | **Every row belongs to a business** | `business_id` on every top-level table; child tables (lines) inherit it through their parent. All queries filter by it. |
| P2 | **UUID primary keys** (`uuid`, generated as time-ordered UUIDv7 by the app/device) | Devices create IDs offline; the same ID is the duplicate-protection key. *(UUIDv7 is time-ordered, so B-tree indexes stay compact.)* |
| P3 | **Facts are append-only** | `sales`, `sale_lines`, `sale_payments`, `returns*`, `stock_movements`, `cash_movements`, ledger entries, `audit_logs` are never updated or deleted (except server-maintained fields like `qty_returned`, `unit_cost_usd`). |
| P4 | **Snapshots at transaction time** | Names, prices, tax rates, costs and exchange rates are copied onto the transaction so history never changes when master data changes. |
| P5 | **Money = original + rate + USD** | Every monetary record holds `currency`, `amount`, `exchange_rate`, `amount_usd`. |
| P6 | **Stock = ledger + running total** | `stock_movements` is the truth; `stock_levels` is a fast running total updated in the same transaction. A nightly check verifies they match. |
| P7 | **Soft delete for master data** | `is_active = 0` instead of DELETE, because old sales still reference the product. |
| P8 | **Optimistic locking** | `version` column on editable master data; update fails with 409 if someone else changed it first. |
| P9 | **UTC everywhere** | `timestamptz` (stored UTC); converted to Beirut time only for display and day boundaries. |
| P10 | **Constraints in the database, not only in code** | Foreign keys, unique keys and CHECK constraints are the last line of defence. |

**Size:** 57 tables in 12 groups.

---

## 2. Domain map

```mermaid
flowchart LR
  ORG["Organization<br/>businesses · branches · devices<br/>cash_registers · settings"]
  CUR["Currency<br/>currencies · exchange_rates"]
  IAM["Identity<br/>users · roles · permissions"]
  CAT["Catalog<br/>products · variants · packagings<br/>barcodes · prices · taxes"]
  INV["Inventory<br/>stock_levels · stock_movements<br/>adjustments · counts · transfers"]
  PUR["Purchasing<br/>suppliers · purchases · payments"]
  SAL["Sales<br/>sales · lines · payments<br/>returns · refunds"]
  CUS["Customers<br/>customers · ledger · payments"]
  CSH["Cash<br/>cash_sessions · cash_movements"]
  EXP["Expenses"]
  APR["Approvals & Notifications"]
  SYN["Sync & Audit<br/>change_log · processed_operations<br/>sync_issues · audit_logs"]
  ORG --> IAM
  ORG --> CAT
  CUR --> SAL
  CAT --> INV
  CAT --> SAL
  PUR --> INV
  SAL --> INV
  SAL --> CSH
  SAL --> CUS
  EXP --> CSH
  APR --> SAL
  SYN -.-> SAL
  SYN -.-> INV
```

---

## 3. ERD — Organization, identity, currency

```mermaid
erDiagram
  businesses ||--o{ branches : has
  businesses ||--o{ users : employs
  businesses ||--o{ roles : defines
  businesses ||--o{ exchange_rates : sets
  businesses ||--o{ business_settings : configures
  branches ||--o{ devices : "registers"
  branches ||--o{ cash_registers : has
  devices |o--o| cash_registers : controls
  roles ||--o{ users : "assigned to"
  roles ||--o{ role_permissions : grants
  permissions ||--o{ role_permissions : "granted by"
  users ||--o{ user_branches : "works in"
  branches ||--o{ user_branches : staffs
  users ||--o{ refresh_tokens : holds
  devices ||--o{ refresh_tokens : "issued to"
  users ||--o{ push_tokens : receives
  currencies ||--o{ exchange_rates : "from/to"

  businesses {
    uuid id PK
    string name
    char base_currency FK
    char secondary_currency FK
    bool vat_enabled
    bool prices_include_vat
    int lbp_rounding_step
    string negative_stock_policy
  }
  branches {
    uuid id PK
    uuid business_id FK
    string code UK
    string name
  }
  devices {
    uuid id PK
    uuid branch_id FK
    string device_type
    string code UK
    string secret_hash
    bool is_active
  }
  cash_registers {
    uuid id PK
    uuid branch_id FK
    uuid device_id FK
    string name
  }
  users {
    uuid id PK
    uuid business_id FK
    uuid role_id FK
    string username UK
    string password_hash
    string pin_hash
  }
  roles {
    uuid id PK
    uuid business_id FK
    string name
    decimal max_discount_percent
  }
  permissions {
    string code PK
    string module
  }
  role_permissions {
    uuid role_id PK
    string permission_code PK
  }
  user_branches {
    uuid user_id PK
    uuid branch_id PK
  }
  exchange_rates {
    uuid id PK
    uuid business_id FK
    char from_currency FK
    char to_currency FK
    decimal rate
    datetime effective_from
  }
  currencies {
    char code PK
    int decimals
  }
```

---

## 4. ERD — Catalog

```mermaid
erDiagram
  categories ||--o{ categories : "parent of"
  categories ||--o{ products : groups
  units ||--o{ products : "base unit"
  tax_rates ||--o{ products : taxes
  products ||--|{ product_variants : "has (≥1)"
  attributes ||--o{ attribute_values : has
  product_variants ||--o{ variant_attribute_values : "described by"
  attribute_values ||--o{ variant_attribute_values : "used in"
  product_variants ||--o{ packagings : "packed as"
  units ||--o{ packagings : unit
  product_variants ||--o{ barcodes : identified
  packagings |o--o{ barcodes : identified
  product_variants ||--o{ variant_prices : priced
  packagings |o--o{ variant_prices : priced
  branches |o--o{ variant_prices : "override for"

  products {
    uuid id PK
    uuid category_id FK
    uuid base_unit_id FK
    uuid tax_rate_id FK
    string name
    bool has_variants
    bool track_stock
  }
  product_variants {
    uuid id PK
    uuid product_id FK
    string sku UK
    string variant_name
    bool is_default
  }
  packagings {
    uuid id PK
    uuid variant_id FK
    uuid unit_id FK
    decimal quantity_in_base
  }
  barcodes {
    uuid id PK
    uuid variant_id FK
    uuid packaging_id FK
    string code UK
  }
  variant_prices {
    uuid id PK
    uuid variant_id FK
    uuid packaging_id FK
    uuid branch_id FK
    char currency FK
    decimal amount
    datetime effective_from
    datetime effective_to
  }
  categories {
    uuid id PK
    uuid parent_id FK
    string name
  }
  attributes {
    uuid id PK
    string name
  }
  attribute_values {
    uuid id PK
    uuid attribute_id FK
    string value
  }
  variant_attribute_values {
    uuid variant_id PK
    uuid attribute_value_id PK
  }
  units {
    uuid id PK
    string name
    bool allows_decimal
  }
  tax_rates {
    uuid id PK
    decimal rate_percent
  }
```

**Why every product has a variant:** stock, prices, barcodes and sale lines always point to `product_variants`. A simple product ("Coca-Cola 330 ml") has one hidden default variant. A clothing product has many. One code path handles both — no special cases.

---

## 5. ERD — Inventory and purchasing

```mermaid
erDiagram
  branches ||--o{ stock_levels : holds
  product_variants ||--o{ stock_levels : "stocked as"
  product_variants ||--o{ stock_movements : moves
  branches ||--o{ stock_movements : "happens in"
  stock_adjustments ||--|{ stock_adjustment_lines : contains
  stock_counts ||--|{ stock_count_lines : contains
  stock_transfers ||--|{ stock_transfer_lines : contains
  branches ||--o{ stock_transfers : "from / to"
  suppliers ||--o{ purchases : supplies
  purchases ||--|{ purchase_lines : contains
  suppliers ||--o{ supplier_payments : "paid by"
  purchases |o--o{ supplier_payments : settles
  product_variants ||--o{ purchase_lines : bought

  stock_levels {
    uuid branch_id PK
    uuid variant_id PK
    decimal quantity_on_hand
    decimal avg_cost_usd
    decimal reorder_point
  }
  stock_movements {
    uuid id PK
    uuid branch_id FK
    uuid variant_id FK
    string movement_type
    decimal quantity
    decimal unit_cost_usd
    string source_type
    uuid source_id
    datetime occurred_at
  }
  stock_adjustments {
    uuid id PK
    uuid branch_id FK
    string reason
    string status
  }
  stock_adjustment_lines {
    uuid id PK
    uuid adjustment_id FK
    uuid variant_id FK
    decimal quantity
  }
  stock_counts {
    uuid id PK
    uuid branch_id FK
    string status
  }
  stock_count_lines {
    uuid id PK
    uuid count_id FK
    uuid variant_id FK
    decimal counted_qty
  }
  stock_transfers {
    uuid id PK
    uuid from_branch_id FK
    uuid to_branch_id FK
    string status
  }
  stock_transfer_lines {
    uuid id PK
    uuid transfer_id FK
    decimal qty_sent
    decimal qty_received
  }
  suppliers {
    uuid id PK
    string name
    char default_currency FK
  }
  purchases {
    uuid id PK
    uuid supplier_id FK
    uuid branch_id FK
    string status
    char currency FK
    decimal exchange_rate
    decimal total_usd
  }
  purchase_lines {
    uuid id PK
    uuid purchase_id FK
    uuid variant_id FK
    decimal qty_ordered
    decimal qty_received
    decimal unit_cost
  }
  supplier_payments {
    uuid id PK
    uuid supplier_id FK
    uuid purchase_id FK
    char currency FK
    decimal amount
    decimal amount_usd
  }
```

`stock_movements.source_type + source_id` is a **polymorphic link** to the document that caused the movement (sale, return, purchase, adjustment, transfer, count). It is not a database FK (it points to different tables), so integrity is guaranteed by the service layer and verified by tests and the nightly reconciliation job.

---

## 6. ERD — Sales, returns, cash, customers

```mermaid
erDiagram
  cash_registers ||--o{ cash_sessions : "opened on"
  cash_sessions ||--o{ sales : records
  cash_sessions ||--o{ returns : records
  cash_sessions ||--o{ cash_movements : records
  cash_sessions |o--o{ customer_payments : records
  sales ||--|{ sale_lines : contains
  sales ||--|{ sale_payments : "paid by"
  sales |o--o{ returns : "returned via"
  returns ||--|{ return_lines : contains
  returns ||--|{ return_refunds : "refunded by"
  sale_lines |o--o{ return_lines : "returned as"
  customers |o--o{ sales : buys
  customers ||--o{ customer_ledger_entries : owes
  customers ||--o{ customer_payments : pays
  users ||--o{ sales : "cashier of"
  approval_requests |o--o{ sales : authorizes
  product_variants ||--o{ sale_lines : sold

  cash_sessions {
    uuid id PK
    uuid cash_register_id FK
    int session_no
    string status
    decimal opening_float_usd
    decimal opening_float_lbp
    decimal counted_usd
    decimal counted_lbp
    decimal difference_usd
    decimal difference_lbp
  }
  cash_movements {
    uuid id PK
    uuid cash_session_id FK
    string movement_type
    char currency FK
    decimal amount
  }
  sales {
    uuid id PK
    uuid cash_session_id FK
    uuid customer_id FK
    uuid cashier_id FK
    string invoice_no UK
    string status
    decimal exchange_rate
    decimal total_usd
    decimal total_lbp
    datetime occurred_at
  }
  sale_lines {
    uuid id PK
    uuid sale_id FK
    uuid variant_id FK
    decimal quantity
    decimal unit_price
    char price_currency FK
    decimal tax_usd
    decimal line_total_usd
    decimal unit_cost_usd
    decimal qty_returned
  }
  sale_payments {
    uuid id PK
    uuid sale_id FK
    string method
    bool is_change
    char currency FK
    decimal amount
    decimal amount_usd
  }
  returns {
    uuid id PK
    uuid original_sale_id FK
    string return_no UK
    decimal total_refund_usd
  }
  return_lines {
    uuid id PK
    uuid return_id FK
    uuid original_sale_line_id FK
    decimal quantity
    bool restock
  }
  return_refunds {
    uuid id PK
    uuid return_id FK
    string method
    char currency FK
    decimal amount
  }
  customers {
    uuid id PK
    string name
    string phone UK
    decimal credit_limit_usd
  }
  customer_ledger_entries {
    uuid id PK
    uuid customer_id FK
    string entry_type
    decimal amount_usd
  }
  customer_payments {
    uuid id PK
    uuid customer_id FK
    char currency FK
    decimal amount
  }
```

---

## 7. ERD — Expenses, approvals, notifications, sync, audit

```mermaid
erDiagram
  expense_categories ||--o{ expenses : classifies
  branches ||--o{ expenses : incurs
  cash_sessions |o--o{ expenses : "paid from"
  expenses |o--o| cash_movements : "creates pay-out"
  devices ||--o{ approval_requests : raises
  users ||--o{ approval_requests : "requests / decides"
  users ||--o{ notifications : receives
  devices ||--o{ processed_operations : pushes
  devices ||--|| device_sync_state : tracks
  processed_operations |o--o{ sync_issues : causes
  businesses ||--o{ change_log : feeds
  businesses ||--o{ audit_logs : records

  expenses {
    uuid id PK
    uuid category_id FK
    uuid branch_id FK
    char currency FK
    decimal amount
    decimal amount_usd
    bool paid_from_drawer
  }
  approval_requests {
    uuid id PK
    string request_type
    string status
    string channel
    uuid requested_by FK
    uuid decided_by FK
    datetime expires_at
  }
  notifications {
    uuid id PK
    uuid user_id FK
    string notif_type
    datetime read_at
  }
  processed_operations {
    uuid operation_id PK
    uuid device_id FK
    string entity_type
    string result
  }
  change_log {
    bigint seq PK
    uuid business_id FK
    uuid branch_id FK
    string entity_type
    string entity_id
    string operation
  }
  device_sync_state {
    uuid device_id PK
    bigint last_pulled_seq
    int clock_skew_seconds
  }
  sync_issues {
    uuid id PK
    uuid operation_id FK
    string issue_type
    string status
  }
  audit_logs {
    bigint id PK
    uuid user_id FK
    string action
    string entity_type
    string entity_id
    datetime occurred_at
    string entry_hash
  }
```

---

## 8. Integrity rules enforced in PostgreSQL

| Rule | Implementation |
|---|---|
| Invoice numbers unique per business | `UNIQUE (business_id, invoice_no)` on `sales`; same for `returns.return_no`, `purchases.reference_no` |
| A sale cannot be pushed twice | PK `sales.id` (device UUID) + `processed_operations.operation_id` PK |
| One open session per register | Partial unique index: `CREATE UNIQUE INDEX … ON cash_sessions (cash_register_id) WHERE status = 'OPEN'` |
| Barcode unique | `UNIQUE (business_id, code)` on `barcodes` |
| Customer phone unique when present | Partial unique index `WHERE phone IS NOT NULL` |
| Positive amounts | `CHECK (amount > 0)` on payments/refunds (except `is_change = 1` → `amount < 0`), `CHECK (rate > 0)` |
| Valid enums | `CHECK (status IN (...))` on every status/type column (plain `varchar` + CHECK instead of PostgreSQL `ENUM`, so adding a value is a simple migration) |
| Movement never zero | `CHECK (quantity <> 0)` on `stock_movements` |
| Transfer to another branch | `CHECK (from_branch_id <> to_branch_id)` |
| Category depth ≤ 2 | Service rule + test (SQL can't express it simply) |
| Sale totals correct | Service recomputes with `pos-core` on ingest; mismatch > $0.01 → `sync_issue` (TOTAL_MISMATCH) |
| Stock running total correct | Nightly job: `stock_levels.quantity_on_hand = SUM(stock_movements.quantity)`; mismatch → alert |
| Append-only tables | Application has no update/delete code paths; the app's DB role has `DELETE` revoked on fact tables (`REVOKE DELETE ON sales, … FROM cedarpos_app`) |

### 8.1 Concurrency on stock (two sales of the same item at the same moment)
```sql
-- inside the sale-ingest transaction, per line:
UPDATE stock_levels
   SET quantity_on_hand = quantity_on_hand - :qty,
       version          = version + 1,
       last_movement_at = now()
 WHERE branch_id = :branch AND variant_id = :variant
RETURNING quantity_on_hand, avg_cost_usd;
-- (if no row: INSERT … ON CONFLICT (branch_id, variant_id) DO UPDATE …)
-- then INSERT INTO stock_movements (…, qty_after = <returned qty>, unit_cost_usd = <returned avg cost>)
```
PostgreSQL locks the row during the `UPDATE`, so two simultaneous sales are applied one after the other; both succeed and the running total is exact.

### 8.2 Isolation level
PostgreSQL's default `READ COMMITTED` with MVCC: readers (reports, mobile dashboard) never block writers (sales ingest) and vice versa — no extra setting needed.

---

## 9. Local databases (SQLite)

Local databases hold **a branch-scoped copy** of master data plus the device's own **unsynced work**. They are not full replicas.

### 9.1 Desktop POS (`cedarpos-local.db`)

| Group | Tables | Source |
|---|---|---|
| Reference (pulled) | `branch`, `business_settings`, `users_cache` (with PIN hash, role, permissions), `currencies`, `exchange_rate_current`, `tax_rates`, `units`, `categories`, `products`, `product_variants`, `packagings`, `barcodes`, `prices_current` (resolved for this branch), `customers`, `stock_levels` (this branch) | Pull sync |
| Own transactions | `sales`, `sale_lines`, `sale_payments`, `returns`, `return_lines`, `return_refunds`, `cash_sessions`, `cash_movements`, `customer_payments`, `stock_adjustments(+lines)`, `approvals_local` | Created locally, pushed |
| Recent history (pulled) | `sales_history` (last 30 days from all terminals of the branch — for returns and reprints) | Pull sync |
| Terminal-only | `held_sales`, `cart_draft`, `local_sequences` (invoice / return / Z counters) | Never synced |
| Sync | `outbox`, `sync_state` (last_pulled_seq, last success), `sync_log` | — |

Local IDs are the **same UUIDs** used on the server; no mapping table is needed.

### 9.2 Mobile (`cedarpos-mobile.db`, Drift)

| Group | Tables |
|---|---|
| Reference | `products`, `variants`, `barcodes`, `prices_current`, `stock_levels` (user's branches), `branches`, `me` (user + permissions), `open_purchases` (ORDERED/PARTIAL purchases + lines for receiving), `exchange_rate_current` |
| Own work | `stock_counts`, `stock_count_lines`, `stock_adjustments`, `stock_adjustment_lines`, `purchase_receipts_local` (goods received by scanning) |
| Snapshots | `dashboard_snapshot` (JSON + fetched_at), `reports_cache`, `notifications` |
| Sync | `outbox`, `sync_state` |

### 9.3 The outbox table (both apps)

| Column | Purpose |
|---|---|
| `id` (UUID) | Operation id → server idempotency key |
| `entity_type` | `SALE`, `RETURN`, `CASH_SESSION_OPEN`, `CASH_SESSION_CLOSE`, `CASH_MOVEMENT`, `CUSTOMER_PAYMENT`, `STOCK_ADJUSTMENT`, `STOCK_COUNT_LINES`, `STOCK_COUNT_SUBMIT`, `PURCHASE_RECEIPT` (mobile) |
| `entity_id` | UUID of the aggregate |
| `payload_json` | Full aggregate (e.g. sale + lines + payments) |
| `created_at` | Device time — keeps order |
| `status` | `PENDING`, `SENDING`, `DONE`, `FAILED` |
| `attempts`, `last_error`, `next_attempt_at` | Retry with backoff |

---

## 10. Migrations

| Item | Approach |
|---|---|
| Server | **Flyway**, files in `pos-server/src/main/resources/db/migration`: `V1__baseline_schema.sql`, `V2__seed_permissions_roles.sql`, `V3__seed_currencies_units.sql`, `V4__...`. Never edit an applied migration; always add a new one. |
| Demo data | `R__demo_data.sql` (repeatable, dev profile only): 2 branches, 4 devices, 8 users, 500 products incl. variants, 90 days of sales. |
| Desktop SQLite | `pos-desktop/src/main/resources/local-migrations/001_init.sql …`; a `schema_version` table; the app migrates on start-up before showing the login screen. |
| Mobile | Drift `schemaVersion` + `MigrationStrategy` with step-by-step migrations. |
| Compatibility | Sync payloads carry `schemaVersion`; the server accepts the current and previous version so an older terminal can still upload its offline sales after a server update. |

---

## 11. Backup and recovery (database part)

| What | How | Frequency |
|---|---|---|
| Logical backup | `pg_dump -Fc cedarpos > cedarpos_YYYYMMDD.dump` (compressed, restorable per table) | Daily 02:00 |
| Point-in-time recovery | `archive_mode = on` + `archive_command` copying WAL files to a backup folder; weekly `pg_basebackup` | Continuous (RPO ≤ 15 min via `archive_timeout = 900`) |
| Off-machine copy | Robocopy / rclone to second disk or cloud folder | After each backup |
| Verification | Monthly restore to `cedarpos_restore_test` (`pg_restore`) + row-count check script | Monthly |
| Scheduler | Windows Task Scheduler (or cron in Docker/Linux) running `scripts/backup/*.ps1` | — |
| Desktop | Copy of `cedarpos-local.db` (SQLite online backup API) on app close and daily; 7 kept | Daily |

Scripts: `scripts/backup/backup-daily.ps1`, `archive-wal.ps1`, `restore-point-in-time.md`, `RESTORE.md`.
