# 05 — Database Tables and Relationships Explained

**Product:** Cedar POS · **Version:** 1.0 (planning) · **Date:** 2026-11-08
Full column definitions: `database/cedar_pos.dbml`. This document explains **why** each table exists and how it connects to others.

Legend — **Kind:** M = master data (editable, versioned), T = transaction (append-only), S = system. **Synced to:** D = desktop, Mo = mobile.

---

## 1. Organization & currency

| Table | Kind | Purpose | Key relationships | Synced to |
|---|---|---|---|---|
| `currencies` | S | Fixed list: USD (2 decimals), LBP (0 decimals) | Referenced by every money column's `currency` | D, Mo |
| `businesses` | M | The company; holds business-wide policies (VAT, rounding step, negative stock policy, offline limits, alert thresholds) | Parent of nearly everything via `business_id` | D, Mo |
| `branches` | M | Physical stores; `code` (B01) appears in document numbers; receipt header/footer per branch | 1 business → many branches; branch → devices, registers, stock, sales | D, Mo |
| `devices` | M | Every desktop terminal and phone; activation and revocation; `code` (T01) appears in invoice numbers | 1 branch → many devices; device → sales, sessions, approvals, sync state | D (self) |
| `cash_registers` | M | A cash drawer. Linked 1-to-1 to the desktop that controls it | branch → registers; register → cash sessions | D |
| `exchange_rates` | T | History of USD→LBP rates. The *current* rate = latest `effective_from` ≤ now. Never updated, only new rows | set_by → users | D, Mo (current) |
| `business_settings` | M | Key/value settings that don't deserve a column (receipt options, label templates…) | business → settings | D |

**Relationship walk-through:** *Cedar Market* (business) has branches *B01 Hamra* and *B02 Jounieh*. B01 has two desktops T01, T02, each controlling one cash register, and one manager phone M01.

---

## 2. Identity & security

| Table | Kind | Purpose | Key relationships | Synced to |
|---|---|---|---|---|
| `permissions` | S | Catalogue of permission codes (document 03); seeded by migration | ← role_permissions | D, Mo |
| `roles` | M | Named permission bundles + `max_discount_percent` limit | business → roles; role → users | D, Mo |
| `role_permissions` | M | Many-to-many roles ↔ permissions | composite PK | D, Mo |
| `users` | M | Staff accounts: password hash (back office/mobile), PIN hash (till), lockout fields | role → users; user ↔ branches | D (branch users), Mo (self) |
| `user_branches` | M | Which branches a user may work in (ignored when `roles.all_branches = 1`) | many-to-many users ↔ branches | D |
| `refresh_tokens` | S | Long-lived login tokens, stored hashed; revoked on logout/device deactivation | user + device | — |
| `push_tokens` | S | Firebase token per user per phone | user + device | — |

---

## 3. Catalog

| Table | Kind | Purpose | Key relationships | Synced to |
|---|---|---|---|---|
| `tax_rates` | M | VAT 11%, Exempt 0%, etc. Default rate flagged | ← products | D, Mo |
| `units` | M | Piece, kg, litre…; `allows_decimal` for weighed items | ← products (base unit), ← packagings | D, Mo |
| `categories` | M | Two-level tree (self-reference `parent_id`); colour for POS buttons | ← products | D, Mo |
| `products` | M | The general item: name, category, tax, base unit, track-stock flag | 1 product → ≥1 variants | D, Mo |
| `attributes` | M | Variant dimensions: Size, Colour | → attribute_values | D |
| `attribute_values` | M | S, M, L / Red, Navy | ↔ variants via variant_attribute_values | D |
| `product_variants` | M | **The sellable, stockable item (SKU).** Simple products have one default variant | ← stock, prices, barcodes, sale/purchase lines | D, Mo |
| `variant_attribute_values` | M | Which values describe a variant (M + Navy) | composite PK | D |
| `packagings` | M | Bigger selling/buying units ("Box of 24" = 24 base units) | variant → packagings; ← barcodes, prices, lines | D, Mo |
| `barcodes` | M | One or more codes per variant or packaging; scanning a box barcode adds the box | variant / packaging | D, Mo |
| `variant_prices` | M | Price history: per variant/packaging, optional branch override, in USD **or** LBP; current price = row with `effective_to IS NULL` | variant, packaging, branch | D, Mo (current only, resolved per branch) |

**Price resolution order at the POS:** branch-specific price for packaging → general price for packaging → (if no packaging price) base price × quantity_in_base.

---

## 4. Inventory

| Table | Kind | Purpose | Key relationships | Synced to |
|---|---|---|---|---|
| `stock_movements` | T | **The stock ledger.** One row per change of stock, signed quantity in base units, cost at that moment, link to the source document, device and user | branch + variant; polymorphic `source_type/source_id` | — (server only) |
| `stock_levels` | S | Running total per branch × variant + **weighted average cost** + reorder point/qty. Updated in the same transaction as each movement | composite PK (branch, variant) | D (own branch), Mo |
| `stock_adjustments` | T | Header for manual corrections (damaged, expired, lost, found); may need approval above a value threshold | branch; created_by, approved_by | D, Mo (own) |
| `stock_adjustment_lines` | T | Items and signed quantities of an adjustment | → adjustment, variant | D, Mo (own) |
| `stock_counts` | T* | A stocktake session (status changes OPEN → SUBMITTED → APPROVED) | branch, optional category | Mo, D |
| `stock_count_lines` | T | Counted quantity per variant; server stores expected qty and posted correction | → count, variant | Mo, D |
| `stock_transfers` | T* | Move goods between branches (DRAFT → SENT → RECEIVED) | from/to branch | D |
| `stock_transfer_lines` | T | Quantities sent and received (difference = loss in transit) | → transfer, variant | D |

\*Header status changes are allowed; lines are append-only once submitted.

**How a sale changes stock:** sale ingested → for each line, one `SALE` movement (−base_quantity) → `stock_levels.quantity_on_hand` decreases → `sale_lines.unit_cost_usd` = current `avg_cost_usd` → change_log entry so terminals and phones pull the new level.

---

## 5. Purchasing

| Table | Kind | Purpose | Key relationships | Synced to |
|---|---|---|---|---|
| `suppliers` | M | Vendor details, default currency, lead time (used by recommendations) | → purchases, payments | D |
| `purchases` | T* | One table covers **order and receipt**: DRAFT → ORDERED → PARTIAL/RECEIVED; currency + rate stored. Received on desktop or **by scanning on mobile** (offline-capable) | supplier, branch | D, Mo (open orders) |
| `purchase_lines` | T | Ordered vs received quantity, unit cost in purchase currency and per-base-unit USD cost (used for average cost) | → purchase, variant, packaging | D |
| `supplier_payments` | T | Money paid to suppliers, optionally against a purchase, optionally from a cash drawer | supplier, purchase, cash_session | D |

**Supplier balance** = Σ `purchases.total_usd` (RECEIVED/PARTIAL, received portion) − Σ `supplier_payments.amount_usd`. Calculated, not stored.

---

## 6. Customers

| Table | Kind | Purpose | Key relationships | Synced to |
|---|---|---|---|---|
| `customers` | M | Customer details, credit enabled + limit | → sales, ledger, payments | D |
| `customer_ledger_entries` | T | **Customer account ledger** in USD: credit sales (+), payments (−), credit refunds (−). Balance = Σ amount_usd | customer; polymorphic source | D (balances) |
| `customer_payments` | T | Payment received on account, any currency, possibly into a cash drawer | customer, cash_session, device | D |

---

## 7. Cash

| Table | Kind | Purpose | Key relationships | Synced to |
|---|---|---|---|---|
| `cash_sessions` | T* | A shift on a register: opening floats, expected/counted/difference **per currency**, Z number. Opened and closed on the terminal (offline OK) | register, device, opened_by/closed_by; ← sales, returns, movements | D |
| `cash_movements` | T | Pay-ins, pay-outs, no-sale drawer opens | → session; optional expense, approval | D |

**Expected cash per currency** (BR-CSH-02) =
opening float
+ Σ `sale_payments` (method CASH, that currency; change rows are negative)
− Σ `return_refunds` (CASH, that currency)
+ Σ `customer_payments` (CASH, that currency)
+ Σ PAY_IN − Σ PAY_OUT (that currency)
− Σ `supplier_payments` paid from the drawer (that currency).

---

## 8. Sales & returns

| Table | Kind | Purpose | Key relationships | Synced to |
|---|---|---|---|---|
| `sales` | T | Invoice header: who, where, which session, rate used, totals in USD and LBP, rounding, invoice discount, approval link, void info | session, device, branch, customer, cashier; → lines, payments, returns | D (own + 30-day branch history), Mo (live feed via API) |
| `sale_lines` | T | Snapshot of each item sold: description, qty, price in its currency, USD price, discount share, tax, total; server adds cost and tracks `qty_returned` | → sale, variant, packaging | D |
| `sale_payments` | T | Each tender **and each change portion** (negative, `is_change = 1`) in its own currency | → sale | D |
| `returns` | T | Refund header; links to original sale (or none with approval) | session, original sale, customer, approval | D |
| `return_lines` | T | Which original lines and quantities; refund at original price; restock flag | → return, original sale_line | D |
| `return_refunds` | T | How the money went back (cash USD/LBP, card, customer credit) | → return | D |

**Void vs return:** a *void* cancels a sale in the same open session (status → VOIDED, reversing `SALE_VOID` movements, cash reversed in that session). After the session closes, only a *return* is possible. This keeps closed Z-reports unchanged forever.

---

## 9. Expenses, approvals, notifications

| Table | Kind | Purpose | Key relationships | Synced to |
|---|---|---|---|---|
| `expense_categories` | M | Rent, electricity, generator subscription, salaries… | → expenses | D |
| `expenses` | T | Business costs in any currency; if paid from drawer, also creates a PAY_OUT cash movement | category, branch, session | D |
| `approval_requests` | T* | Every approval (PIN or mobile): type, JSON payload of what was approved, status, who decided | device, requester, decider; ← sales, returns, cash_movements | D, Mo |
| `notifications` | T | In-app notification list per user (approvals, low stock, cash difference, **daily summary**, **suspicious activity**, receipt differences, rate changed); `pushed_at` when sent via FCM | user | Mo |

---

## 10. Sync

| Table | Kind | Purpose | Key relationships |
|---|---|---|---|
| `processed_operations` | S | Every outbox operation already handled, with its result. **The duplicate shield:** a resend returns the stored result | device |
| `change_log` | S | Ordered feed of what changed (entity + id + seq). Devices pull "everything after seq N" for their business/branch | business, branch |
| `device_sync_state` | S | Per device: last pulled seq, last push/pull time, reported outbox size, measured clock skew. Powers "terminals online/offline" on mobile | 1-to-1 device |
| `sync_issues` | S | Problems found while applying offline data that need a human (over-return, totals mismatch, clock skew…) | device, operation |

## 11. Audit

| Table | Kind | Purpose |
|---|---|---|
| `audit_logs` | T | Who did what, when, from which device, with before/after values and reason. Append-only; optional hash chain (`prev_hash`, `entry_hash`) makes deletion or editing detectable |

---

## 12. Relationship summary (cardinalities)

| From | To | Cardinality | Rule |
|---|---|---|---|
| business | branch | 1 : N | Every branch belongs to exactly one business |
| branch | device | 1 : N | A device is bound to one branch at activation |
| device | cash_register | 1 : 0..1 | A desktop controls at most one drawer |
| cash_register | cash_session | 1 : N | At most one OPEN at a time |
| cash_session | sale | 1 : N | Every sale belongs to an open session |
| sale | sale_line | 1 : 1..N | A sale has at least one line |
| sale | sale_payment | 1 : 1..N | Payments (incl. change) sum to total ± rounding |
| sale_line | return_line | 1 : 0..N | Σ returned ≤ sold |
| product | product_variant | 1 : 1..N | At least one (default) variant |
| variant | barcode | 1 : 0..N | Barcode unique per business |
| branch × variant | stock_level | 1 : 1 | Created on first movement |
| stock_level | stock_movement | 1 : N | quantity_on_hand = Σ quantity |
| supplier | purchase | 1 : N | — |
| customer | ledger entry | 1 : N | balance = Σ amount_usd ≤ credit limit (or approved) |
| role | user | 1 : N | Each user has exactly one role |
| user | branch | N : M | via user_branches |
