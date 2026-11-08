# 01 — Software Requirements Specification (SRS)

**Product:** Cedar POS (working name — can be changed at any time)
**Version:** 1.0 (planning) · **Date:** 2026-11-08
**Context:** Internship project · 1 developer · 8 weeks of implementation

---

## 1. Introduction

### 1.1 Purpose
This document defines **what** Cedar POS must do: functions, business rules, quality requirements and limits. Every later document (database, API, screens, tests, roadmap) traces back to the requirement IDs here (`FR-…`, `BR-…`, `NFR-…`).

### 1.2 Product summary
Cedar POS is a retail point-of-sale system made of three connected parts sharing **one central database**:

| Part | Who uses it | Main job |
|---|---|---|
| **Desktop POS** (JavaFX, Windows) | Cashiers, managers in the store | Sell, return, manage cash, and run the back office |
| **Mobile app** (Flutter, Android/iOS) | Owner, managers, stock keepers | Monitor sales, count and check stock, approve requests, view reports |
| **Backend API** (Spring Boot + PostgreSQL) | Both apps | Single source of truth, business rules, sync, reports |

Both apps keep a **local SQLite database** so they keep working when the network is down, and **synchronize** automatically when it returns.

### 1.3 Scope decisions (agreed 2026-11-08)

| Topic | Decision |
|---|---|
| Purpose | Internship project — must be complete and integrated; commercial sale is **not** a goal |
| Timeline | ~2 months, one developer |
| Language | **English only** (UI, receipts, reports) |
| Currencies | **USD and LBP** (Lebanese Pound), used together |
| Store type | General retail, with **optional product variants** (e.g. size × colour) |
| Offline | **Desktop and mobile** both work offline (see §1.4) |
| Design | "Ledger" visual direction (see document 13) |

### 1.4 What "offline" means on each app

| Works offline | Desktop POS | Mobile app |
|---|---|---|
| Selling, payments, receipts, cash drawer | ✅ | — (mobile POS is out of scope) |
| Returns (against a receipt in the local history) | ✅ | — |
| Open / close cash session, pay-in / pay-out | ✅ | — |
| Product & barcode lookup, stock levels (last synced) | ✅ | ✅ |
| Stock counting | — | ✅ |
| Stock adjustments (damage, loss…) | ✅ | ✅ |
| Receiving goods against a purchase | — | ✅ |
| Dashboard / reports | Last synced snapshot | Last synced snapshot, clearly dated |
| Editing master data (products, prices, users, settings) | ❌ online only | ❌ online only |
| Manager approvals | Manager PIN at terminal | ❌ needs connection |

> **Why master data is online-only:** if two people edit the same product offline, someone's change must be thrown away. Real POS systems avoid this by allowing only *operations* (selling, counting) offline. This removes almost all sync conflicts and saves weeks of work.

### 1.5 Definitions

| Term | Meaning |
|---|---|
| **Business** | A company using the system (data is isolated per business) |
| **Branch** | A physical store of a business |
| **Device** | A registered desktop terminal or phone |
| **Product / Variant** | A product is the general item ("Polo Shirt"); a variant is the exact sellable item ("Polo Shirt – M – Navy"). Simple products have exactly one variant. |
| **SKU** | Stock-keeping unit — the code of one variant |
| **Packaging** | A way of selling/buying a variant in a bigger unit (e.g. "Box of 24") |
| **Cash session (shift)** | Period between opening and closing a cash drawer |
| **X-report / Z-report** | Mid-shift summary / final end-of-shift report (Z closes the session) |
| **Outbox** | Local queue of records waiting to be sent to the server |
| **Base currency** | USD — the currency in which accounts and reports are kept |
| **Rate** | USD→LBP exchange rate (e.g. 1 USD = 89,500 LBP — example only) |

### 1.6 Users (actors)

| Role | Description |
|---|---|
| **Owner** | Full access to all branches; configures the business |
| **Branch Manager** | Runs one or more branches; approves exceptions |
| **Cashier** | Sells, returns with receipt, handles own cash session |
| **Stock Keeper** | Receives goods, counts stock, adjustments, transfers |
| **Accountant** | Reads financial reports, records expenses, supplier payments |

Full permission detail: document 03.

---

## 2. Assumptions and constraints

| ID | Assumption / constraint |
|---|---|
| AC-1 | One developer, ~40 working days. Requirements are prioritised (MoSCoW) so that lower priorities can be cut without breaking the core. |
| AC-2 | Backend + PostgreSQL run on one machine (developer laptop or a single server/VM). Devices reach it over LAN or the internet. |
| AC-3 | **PostgreSQL 18** (free, open source, no size limit) managed with **pgAdmin 4** or **DBeaver**; runs natively on Windows or in Docker. The same database can later be moved to any cloud provider without licence cost. |
| AC-4 | Desktop terminals run Windows 10/11, 1366×768 minimum, keyboard + mouse or touch. |
| AC-5 | Hardware: 80 mm ESC/POS thermal printer (USB or network), cash drawer connected to the printer, USB barcode scanner in keyboard mode. The system must also work **without** hardware (on-screen receipt / PDF) for demos. |
| AC-6 | Card payments are taken on a separate bank terminal; the cashier records the amount and reference (no card data in our system). |
| AC-7 | VAT in Lebanon is currently 11% (a rise to 12% was proposed in 2026 but not enacted). The rate is a **setting**, not hard-coded. Businesses not registered for VAT can disable tax. |
| AC-8 | Multiple businesses are supported in the data model (isolation by `business_id`), but there is no subscription/billing system. A business is created by a setup script or the Owner sign-up screen. |

---

## 3. Priorities (MoSCoW)

- **M — Must:** needed for a working, integrated system. Done in 8 weeks.
- **S — Should:** planned in 8 weeks; cut first if late.
- **C — Could:** only if time remains.
- **W — Won't (this version):** documented for the future.

---

## 4. Functional requirements

### 4.1 Authentication, devices and users

| ID | Requirement | P |
|---|---|---|
| FR-AUTH-01 | Users log in with username + password (back office, mobile). | M |
| FR-AUTH-02 | At a desktop terminal, staff log in quickly by choosing their name and entering a 4–6 digit **PIN**. | M |
| FR-AUTH-03 | PIN login works offline using cached, hashed PINs for users assigned to that branch. Offline login is allowed for up to **72 hours** since the last successful sync (configurable). | M |
| FR-AUTH-04 | After 5 wrong PIN/password attempts the account is locked for 5 minutes. | M |
| FR-AUTH-05 | A device must be **activated** once using a one-time code generated by an Owner/Manager. It is then bound to one branch. | M |
| FR-AUTH-06 | An Owner can deactivate a device (lost/stolen); it can no longer sync. | M |
| FR-AUTH-07 | Sessions use short-lived access tokens + refresh tokens; logout revokes the refresh token. | M |
| FR-AUTH-08 | Auto-lock the POS screen after N minutes idle (default 5). | S |

### 4.2 Business, branches and settings

| ID | Requirement | P |
|---|---|---|
| FR-ORG-01 | Create/edit a business: name, address, phone, VAT number, VAT enabled, prices include VAT (default yes). | M |
| FR-ORG-02 | Create/edit branches: code (e.g. `B01`), name, address, phone, active flag. | M |
| FR-ORG-03 | Receipt settings: header, footer, show LBP total, show VAT breakdown. | M |
| FR-ORG-04 | Register devices and cash registers per branch. | M |
| FR-ORG-05 | Configure the LBP cash rounding step (default 1,000 LBP; options 1,000 / 5,000 / 10,000). | M |

### 4.3 Currency and exchange rate

| ID | Requirement | P |
|---|---|---|
| FR-CUR-01 | Support two currencies: **USD** (base, 2 decimals) and **LBP** (0 decimals). | M |
| FR-CUR-02 | A Manager/Owner sets the USD→LBP rate. Every change is stored with date, time and user (history kept). | M |
| FR-CUR-03 | Every transaction (sale, return, payment, purchase, expense) stores the rate used at that moment. | M |
| FR-CUR-04 | Terminals use the latest rate they received; if it is older than 24 h (configurable) the POS shows a warning. | M |
| FR-CUR-05 | Prices can be defined in USD or in LBP per product (e.g. bread priced in LBP). | S |

### 4.4 Catalog: products, categories, variants, units, barcodes

| ID | Requirement | P |
|---|---|---|
| FR-CAT-01 | Manage categories (two levels: category › sub-category), with colour for POS quick buttons. | M |
| FR-CAT-02 | Manage products: name, category, description, tax rate, track stock (yes/no), active flag, image (optional). | M |
| FR-CAT-03 | Every product has at least one **variant** (SKU). A simple product has one default variant created automatically. | M |
| FR-CAT-04 | Optional variants: define attributes (e.g. Size, Colour) and values; the system generates the variant matrix (S, M, L × Red, Navy = 6 SKUs). Each variant can have its own SKU, barcode, price and cost. | S |
| FR-CAT-05 | Units: each variant has a base unit (piece, kg, litre). Optional **packagings** (e.g. Box = 24 pieces) with their own barcode and price. | M |
| FR-CAT-06 | Multiple barcodes per variant/packaging. Barcode must be unique within the business. | M |
| FR-CAT-07 | Generate internal barcodes (Code-128) for items without one. | M |
| FR-CAT-08 | Print barcode/shelf labels (to a normal or label printer, via PDF). | C |
| FR-CAT-09 | Selling price per variant/packaging, with optional branch-specific override. Price history is kept. | M |
| FR-CAT-10 | Import products from CSV/Excel with validation report. | S |
| FR-CAT-11 | Weighed items sold by kg with decimal quantity (manual quantity entry; scale integration out of scope). | S |

### 4.5 Point of Sale (desktop)

| ID | Requirement | P |
|---|---|---|
| FR-POS-01 | Add items by **barcode scan**, by search (name/SKU), or by quick-button grid (category → product). | M |
| FR-POS-02 | Change quantity (+/−/type), remove a line before payment (permission `pos.void_line`). | M |
| FR-POS-03 | Line discount and invoice discount (% or amount). Cashier limit configurable (default 10%); above the limit requires manager approval. | M |
| FR-POS-04 | Price override requires permission or manager approval. | S |
| FR-POS-05 | Attach a customer to a sale (search by name/phone, quick-create). | M |
| FR-POS-06 | **Hold** a sale and **recall** it later (held sales are local to the terminal). | S |
| FR-POS-07 | Payment screen supporting **split tender**: Cash USD, Cash LBP, Card (USD or LBP), Customer credit — in any combination. | M |
| FR-POS-08 | Show the amount due in **both currencies** at all times; recalculate remaining due after each tender. | M |
| FR-POS-09 | Calculate change; cashier chooses change in USD, LBP, or mixed. Default suggestion: whole dollars in USD, remainder in LBP. | M |
| FR-POS-10 | Complete the sale as one atomic local transaction; print the receipt; open the cash drawer if cash was involved. | M |
| FR-POS-11 | Every sale gets a unique invoice number `{Branch}-{Terminal}-{Number}`, e.g. `B01-T02-000123`, generated locally (works offline). | M |
| FR-POS-12 | Reprint the last receipt or any past receipt (marked "COPY"). | M |
| FR-POS-13 | Void a completed sale (same session only) with manager approval; stock and cash are reversed. | S |
| FR-POS-14 | Allow selling when system stock is zero or negative, with a visible warning (configurable: allow/warn/block). | M |
| FR-POS-15 | Keyboard shortcuts for every main action (document 08). | M |
| FR-POS-16 | Visible connection/sync status at all times ("Online · synced 10 s ago" / "Offline · 7 waiting"). | M |
| FR-POS-17 | Open drawer without sale ("No sale") — permission + audit. | S |

### 4.6 Returns and refunds

| ID | Requirement | P |
|---|---|---|
| FR-RET-01 | Return items against an original receipt (scan receipt barcode or search by number). | M |
| FR-RET-02 | Cannot return more than was sold minus already returned (checked locally and on the server). | M |
| FR-RET-03 | Refund is calculated at the **original sale prices** (incl. discounts and tax). | M |
| FR-RET-04 | Refund default goes to the original payment method; cashier may choose cash USD/LBP; refunds of credit sales reduce customer balance. | M |
| FR-RET-05 | Choose per line whether the item goes back to stock (resellable) or not (damaged). | M |
| FR-RET-06 | Return **without receipt**: requires manager approval and refunds as store credit or cash at current price. | S |
| FR-RET-07 | Exchange = return + new sale in one flow. | C |

### 4.7 Customers

| ID | Requirement | P |
|---|---|---|
| FR-CUS-01 | Manage customers: name, phone (unique per business), email, notes. | M |
| FR-CUS-02 | **Credit accounts** ("pay later"): credit limit in USD; sale on credit is blocked above the limit unless manager approves. | S |
| FR-CUS-03 | Record customer payments (USD/LBP) and show balance + statement. | S |
| FR-CUS-04 | Customer purchase history. | M |
| FR-CUS-05 | Loyalty points | W |

### 4.8 Inventory

| ID | Requirement | P |
|---|---|---|
| FR-INV-01 | Stock is tracked **per branch per variant** in base units. | M |
| FR-INV-02 | Every change of stock is a **stock movement** record (sale, return, purchase, adjustment, transfer, count correction, opening). Stock on hand = sum of movements. Movements are never edited or deleted. | M |
| FR-INV-03 | Stock adjustments with reason (damaged, expired, lost/theft, found, other) and note. Adjustments above a value threshold need approval. | M |
| FR-INV-04 | Stock count (stocktake): create a count for a branch (all, by category); count with barcode scanning on mobile/desktop; review variances; approve → corrections posted. | M |
| FR-INV-05 | Transfers between branches: Draft → Sent (stock leaves source) → Received (stock arrives; differences recorded). | C |
| FR-INV-06 | Reorder point and reorder quantity per variant per branch; low-stock list and notification. | M |
| FR-INV-07 | **Weighted average cost** per variant per branch, updated on every purchase receipt. | M |
| FR-INV-08 | Stock movement history with filters (product, branch, type, date, user). | M |
| FR-INV-09 | Batch / expiry tracking | W |
| FR-INV-10 | Serial / IMEI tracking | W |

### 4.9 Purchasing and suppliers

| ID | Requirement | P |
|---|---|---|
| FR-PUR-01 | Manage suppliers: name, contact, phone, default currency, notes. | M |
| FR-PUR-02 | Create a **purchase** with lines (variant/packaging, quantity, unit cost), currency and rate. | M |
| FR-PUR-03 | Purchase statuses: Draft → Ordered (optional) → Received → (Cancelled). Receiving posts stock movements and updates average cost. | M |
| FR-PUR-04 | Partial receiving (receive less than ordered). | S |
| FR-PUR-05 | Supplier payments (USD/LBP) and supplier balance (what we owe). | S |
| FR-PUR-06 | **Purchase recommendations**: suggested quantities per supplier based on sales speed and reorder points (formula in document 09 §6). | S |

### 4.10 Cash registers and cash sessions

| ID | Requirement | P |
|---|---|---|
| FR-CSH-01 | A cashier must open a cash session (with opening float in USD **and** LBP) before selling. | M |
| FR-CSH-02 | Pay-in / pay-out with reason (e.g. paying a delivery), in either currency. | M |
| FR-CSH-03 | X-report at any time (if permitted). | M |
| FR-CSH-04 | Close session with **blind count** per currency (optionally by denomination). System calculates expected cash per currency and the difference (over/short). | M |
| FR-CSH-05 | Z-report printed at close, numbered per terminal; a closed session cannot be reopened. | M |
| FR-CSH-06 | Cash differences above a threshold notify the manager. | S |

### 4.11 Expenses

| ID | Requirement | P |
|---|---|---|
| FR-EXP-01 | Record expenses: category, branch, amount, currency, date, description, paid from cash drawer (yes/no). | M |
| FR-EXP-02 | Expenses paid from drawer appear as pay-outs in the cash session. | S |

### 4.12 Reports and dashboard

| ID | Requirement | P |
|---|---|---|
| FR-REP-01 | Dashboard: today's sales, number of sales, average basket, gross profit, top products, sales by hour, payment mix, low stock count. Per branch or all. | M |
| FR-REP-02 | Sales report by period / branch / cashier / category / product. | M |
| FR-REP-03 | Profit report (revenue − cost of goods sold − returns) and profit & loss summary incl. expenses. | M |
| FR-REP-04 | Inventory valuation (quantity × average cost) per branch. | M |
| FR-REP-05 | Cash sessions report (differences per cashier). | M |
| FR-REP-06 | VAT report (sales VAT collected per period). | S |
| FR-REP-07 | Customer and supplier balances report. | S |
| FR-REP-08 | Export any report to CSV and PDF. | S |
| FR-REP-09 | All monetary report figures in USD; LBP shown where relevant at the transaction rates. | M |

### 4.13 Mobile app

| ID | Requirement | P |
|---|---|---|
| FR-MOB-01 | Role-based home screen (Owner/Manager vs Stock Keeper). | M |
| FR-MOB-02 | Business dashboard (FR-REP-01) with branch filter. | M |
| FR-MOB-03 | **Live sales**: feed of sales as they sync, refreshing every 15 s while open. | M |
| FR-MOB-04 | Product lookup by **camera barcode scan** or search: price, stock in each branch. | M |
| FR-MOB-05 | Stock counting with camera scanner, **offline**, submitted when online. | M |
| FR-MOB-06 | Stock adjustments offline. | S |
| FR-MOB-07 | **Receive goods by camera scanning** at the stockroom door: open an ordered purchase, scan each item, confirm quantities (partial allowed), add supplier invoice number. Works **offline** (queued). On sync: stock increases and average cost updates. Over-receiving is flagged. | S |
| FR-MOB-08 | Branch monitoring: each branch's sales, open sessions, terminals online/offline, last sync. | M |
| FR-MOB-09 | Employee monitoring: sales per cashier, voids, discounts, refunds, cash differences. | S |
| FR-MOB-10 | **Approval requests**: list, details, approve/reject; terminal is updated within seconds. | S |
| FR-MOB-11 | **Push notifications** (Firebase): approval requests, low stock, large cash difference, sync problems. In-app notification list as fallback. | S |
| FR-MOB-12 | Purchase recommendations list (FR-PUR-06). | S |
| FR-MOB-13 | Sales and financial reports (FR-REP-02/03/05). | M |
| FR-MOB-14 | AI business assistant | W |
| FR-MOB-16 | **Set the USD→LBP rate from the phone**; all terminals receive it within 30 s and show a "New rate" notice to the cashier; history with who/when. | M |
| FR-MOB-17 | **Daily closing summary** pushed to the Owner (after the last session closes, or at a configured time): sales USD/LBP, transactions, gross profit, cash differences per currency, returns, top products, comparison with the same weekday last week. | S |
| FR-MOB-18 | **Suspicious-activity alerts** evaluated by the server on every sync: voids, no-sale drawer opens, discounts, returns without receipt and cash differences above per-business thresholds, per cashier per session. Each alert links to the evidence (sales, audit entries). | S |
| FR-MOB-15 | Mobile POS | W |

### 4.14 Approvals

| ID | Requirement | P |
|---|---|---|
| FR-APR-01 | Actions needing approval: discount over limit, price override, void sale, return without receipt, sale on credit over limit, large stock adjustment. | M |
| FR-APR-02 | Approval by **manager PIN at the terminal** (works offline). | M |
| FR-APR-03 | Approval by **mobile request** when online: terminal shows "Waiting for approval…", manager approves on phone; request expires after 5 min. | S |
| FR-APR-04 | Every approval is recorded: who requested, who approved, how (PIN/mobile), when, what. | M |

### 4.15 Audit and sync administration

| ID | Requirement | P |
|---|---|---|
| FR-AUD-01 | Audit log of security- and money-relevant actions: logins, failed logins, price changes, discounts, voids, refunds, drawer opens, cash differences, stock adjustments, rate changes, permission changes, device activation. | M |
| FR-AUD-02 | Audit log is append-only; viewable with filters by Owner/Manager. | M |
| FR-AUD-03 | Tamper evidence: each entry stores a hash of the previous entry. | C |
| FR-SYN-01 | Sync status screen: pending items, last sync, errors, "Sync now". | M |
| FR-SYN-02 | Server-side **sync issues** list (e.g. return exceeding sold quantity from two offline terminals) for manager review. | M |

---

## 5. Business rules

### 5.1 Money and currency

| ID | Rule |
|---|---|
| BR-MON-01 | Money is never stored or calculated in floating point. Java `BigDecimal`, Dart `Decimal`, SQL `DECIMAL(19,4)`; rates `DECIMAL(19,6)`. |
| BR-MON-02 | USD is the base currency. Every monetary record stores its **original currency + amount**, the **rate**, and the **USD equivalent**. |
| BR-MON-03 | Line totals are calculated in USD to 4 decimals; the invoice grand total is rounded to **0.01 USD** (half-up). |
| BR-MON-04 | LBP amount due = USD total × rate, rounded to the configured LBP step (default 1,000), half-up. The difference is stored as `rounding_usd` on the sale. |
| BR-MON-05 | An LBP-priced item is converted to USD at the sale's rate for totals; the original LBP price is kept on the line. |
| BR-MON-06 | **Cash is counted per currency.** USD and LBP drawers have separate floats, expected amounts, counts and differences. They are never converted for reconciliation. |
| BR-MON-07 | Change may be given in USD, LBP or both; each change portion is recorded as a negative cash tender in its currency (so the drawer totals stay exact). |
| BR-MON-08 | A tender is complete when (sum of tenders in USD equivalent) ≥ total due − tolerance of half an LBP step. |

**Worked example** (rate 89,500; LBP step 1,000):
Total = $7.40 → LBP due = 662,300 → rounded **662,000**.
Customer pays **$10** cash. Change = $2.60 → suggested: **$2** + 0.60 × 89,500 = 53,700 → **54,000 LBP**.
Drawer: USD +10 −2 = **+$8**; LBP **−54,000**. The store gave 300 LBP more than exact change (≈ $0.0034); this is recorded as `rounding_usd = −0.0034` on the sale so reports still balance to the cent.

### 5.2 Tax

| ID | Rule |
|---|---|
| BR-TAX-01 | VAT can be enabled per business; default rate 11%, editable; product-level tax rate (0% allowed). |
| BR-TAX-02 | Default pricing is **VAT-inclusive** (shelf price = what the customer pays). Tax = price × rate / (100 + rate). |
| BR-TAX-03 | Discounts reduce the taxable amount proportionally. |
| BR-TAX-04 | Each sale line stores the tax rate and tax amount used (rates may change later). |

### 5.3 Sales, returns, stock

| ID | Rule |
|---|---|
| BR-SAL-01 | A completed sale is **immutable**. Corrections are voids (same session) or returns. |
| BR-SAL-02 | A sale line stores product name, price, discount, tax and (server-assigned) cost as they were at the moment of sale. |
| BR-SAL-03 | Invoice numbers are unique per terminal series and never reused; gaps (e.g. app crash before commit) are allowed but logged. |
| BR-SAL-04 | Sales are linked to an open cash session; a sale cannot be completed without one. |
| BR-RET-01 | Returned quantity per original line ≤ sold − already returned. Server re-checks on sync; violations are **accepted and flagged** (money already left the drawer) as a sync issue. |
| BR-STK-01 | Stock on hand = Σ movements. A `stock_levels` table is kept as a fast running total, updated in the same database transaction as each movement. |
| BR-STK-02 | Negative stock is allowed (configurable) and highlighted in reports. |
| BR-STK-03 | Average cost: new_avg = (old_qty × old_avg + received_qty × unit_cost) / (old_qty + received_qty); if old_qty ≤ 0, new_avg = unit_cost. |
| BR-STK-04 | Cost of a sale line = branch average cost at the time the server records the sale. |
| BR-STK-05 | Count approval posts a `COUNT_CORRECTION` movement = counted − expected at the moment of approval, adjusted by movements that happened during counting. |

### 5.4 Cash sessions

| ID | Rule |
|---|---|
| BR-CSH-01 | One open session per cash register at a time. |
| BR-CSH-02 | Expected cash (per currency) = opening float + cash tenders − cash change − cash refunds + pay-ins − pay-outs. |
| BR-CSH-03 | Blind close: the cashier enters counted amounts **before** seeing the expected amounts (unless permission `cash.view_expected`). |

---

## 6. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | Scan → line visible < 150 ms (local). Sale commit + print start < 1 s. Product search over 10,000 items < 300 ms locally. |
| NFR-02 | Performance | API: 95% of requests < 300 ms for the demo dataset (10k products, 100k sales). |
| NFR-03 | Availability | Desktop selling continues indefinitely during network/server outage (subject to FR-AUTH-03). |
| NFR-04 | Sync | Pending records sync within 60 s after connection returns; no record lost or duplicated (verified by tests). |
| NFR-05 | Integrity | All financial writes are atomic (local SQLite transaction and server DB transaction). |
| NFR-06 | Security | Passwords/PINs hashed with bcrypt; HTTPS in any non-local deployment; JWT signed; device credential revocable; local SQLite stores only data needed by that branch. |
| NFR-07 | Security | Data isolation: every query is scoped by `business_id` from the authenticated token; automated tests prove a user of business A cannot read business B. |
| NFR-08 | Auditability | Every money/stock change can be traced to user, device, time and source document. |
| NFR-09 | Backup | Server DB: daily full + 15-minute log backups; documented, tested restore. Desktop: unsynced data kept until server confirms. RPO ≤ 15 min, RTO ≤ 4 h (demo target). |
| NFR-10 | Usability | Cashier can complete a 5-item cash sale in < 20 s using the keyboard only. All touch targets ≥ 44 px. WCAG AA contrast. |
| NFR-11 | Maintainability | Modular code (document 02), migrations versioned (Flyway), API documented with OpenAPI/Swagger, code style enforced. |
| NFR-12 | Testability | Core money/tax/sync logic ≥ 80% line coverage (document 11). |
| NFR-13 | Portability | Desktop: Windows 10/11 installer with bundled Java runtime. Mobile: Android 8+; iOS 14+ (if a Mac is available for building). |
| NFR-14 | Observability | Server logs structured (JSON) with request id, user, device; desktop logs to rolling files for support. |

---

## 7. Key use cases (summary)

**UC-01 Cash sale with mixed currency** — Cashier logs in by PIN → session already open → scans 3 items → presses Pay (F12) → customer gives 500,000 LBP and $5 → system shows remaining $0.00 and change → cashier confirms change in LBP → receipt prints, drawer opens → sale saved locally and queued → synced within seconds → appears on owner's phone in Live Sales.

**UC-02 Offline sale** — Network drops → status pill turns amber "Offline" → selling continues normally → 40 sales queue in outbox → network returns → outbox pushes in batches → server acknowledges each sale ID → pill turns green.

**UC-03 Discount over limit** — Cashier applies 25% (limit 10%) → approval required → option A: manager types PIN at terminal; option B: "Ask on phone" → manager approves on mobile → discount applied → audit record created.

**UC-04 Stock count on phone** — Stock keeper opens count "Aisle 3" → scans each item, types quantity → no Wi-Fi in storeroom → counts saved locally → back online → submits → manager reviews variances on desktop → approves → stock corrected.

**UC-05 Close shift** — Cashier chooses Close Session → enters counted USD and LBP → system shows differences → Z-report prints → manager notified if difference > threshold.

**UC-06 Purchase receipt** — Manager creates and orders a purchase → delivery arrives at the back door → stock keeper opens it on the phone, scans each box, confirms quantities (even without Wi-Fi) → on sync, stock increases and average cost updates → supplier balance increases → accountant records supplier payment.

**UC-07 Rate change** — Owner, away from the store, updates the rate to a new value on the phone → within 30 s every terminal shows "New rate: 1 USD = … LBP" → all new sales use it; earlier sales keep their rate.

**UC-08 End of day** — Last cashier closes the session → owner receives "Today: $4,812 · 312 sales · profit $1,388 · cash −$6 / −45,000 LBP at T01" → taps to open the details.

---

## 8. Out of scope (this version)

Mobile POS · AI assistant · integrated card terminals · e-commerce sync · loyalty/gift cards · batch/expiry and serial tracking · scale integration · promotions engine (buy X get Y) · full accounting/general ledger · multi-language · subscription billing · branch local servers.

These are listed in the roadmap "Future" section and the data model leaves room for them.

---

## 9. Requirement traceability

Each FR is mapped to: database tables (05), API endpoints (07), screens (08/09), tests (11) and roadmap phase (12). Traceability column format used in later documents: `FR-POS-07 → sale_payments → POST /sync/push (Sale) → POS Payment dialog → T-POS-07 → Phase 3`.
