# DECISIONS

Log of every important decision: what was decided, why, and who approved it.
Status: ✅ Approved by owner · 🟡 Proposed by architect, awaiting approval · ❌ Rejected · 🔁 Superseded

| ID | Date | Decision | Reason | Status |
|---|---|---|---|---|
| D-001 | 2026-11-08 | No code until the planning documents are approved | Owner brief | ✅ |
| D-002 | 2026-11-08 | Project is an **internship project**: complete and integrated, commercial sale not a goal → no subscriptions, licensing or billing | Owner answer | ✅ |
| D-003 | 2026-11-08 | Timeline **2 months**, **one developer** | Owner answer | ✅ |
| D-004 | 2026-11-08 | UI language **English only** | Owner answer | ✅ |
| D-005 | 2026-11-08 | Currencies **USD (base) + LBP**; prices may be in either; rate snapshot on every transaction; cash counted per currency | Owner answer; Lebanese market practice | ✅ |
| D-006 | 2026-11-08 | Store type: **general retail with optional variants** | Owner answer | ✅ |
| D-007 | 2026-11-08 | **Desktop + mobile offline.** Desktop: full selling offline. Mobile: offline lookup, counting, adjustments and **receiving goods** (mobile POS out of scope) | Owner chose desktop + mobile offline; mobile scope limited to fit 8 weeks | ✅ |
| D-008 | 2026-11-08 | Visual direction **"Ledger"** | Owner answer | ✅ |
| D-009 | 2026-11-08 | ~~Keep SQL Server~~ → **PostgreSQL 18** (pgAdmin 4 / DBeaver) as the main database | Most-used database among professional developers (Stack Overflow 2025); free at any size; standard on every cloud; better for future use and the job market. Switched before any code, so no rework | ✅ (approved 15:18) |
| D-010 | 2026-11-08 | Keep **JavaFX** desktop, **Maven** multi-module (opens in NetBeans), Java 21, shared `pos-core` library | Shares money/tax logic with server | 🟡 |
| D-011 | 2026-11-08 | Deployment: server + DB on one machine (laptop/VM), devices over LAN; no cloud required | Internship demo | 🟡 |
| D-012 | 2026-11-08 | Sync model: append-only transactions, stock as sum of movements, device UUIDs as idempotency keys, change-log pull | Removes most conflicts; prevents duplicates | 🟡 |
| D-013 | 2026-11-08 | **Master data editable online only** (products, prices, users, settings) | Avoids edit/edit conflicts; saves weeks | 🟡 |
| D-014 | 2026-11-08 | Backend as **modular monolith** | Right size for one developer | 🟡 |
| D-015 | 2026-11-08 | Money as BigDecimal / DECIMAL(19,4); rates DECIMAL(19,6); never floating point | Financial accuracy | 🟡 |
| D-016 | 2026-11-08 | VAT configurable, default 11% inclusive (Lebanon standard rate; 12% proposed but not enacted as of mid-2026) | Legal accuracy without hard-coding | 🟡 |
| D-017 | 2026-11-08 | Negative stock allowed with warning (policy configurable) | Required for offline selling | 🟡 |
| D-018 | 2026-11-08 | Inventory costing: weighted average cost | Simple, standard for retail | 🟡 |
| D-019 | 2026-11-08 | Card payments non-integrated (amount + reference recorded) | No card data, no PCI scope | 🟡 |
| D-020 | 2026-11-08 | Working product name **"Cedar POS"** (package `com.cedarpos`) | Placeholder; easy to rename before P0 | 🟡 |
| D-021 | 2026-11-08 | LBP cash rounding step default 1,000 LBP (configurable 1,000 / 5,000 / 10,000) | Practical cash handling | 🟡 |
| D-022 | 2026-11-08 | Earlier recommendation of SaaS hosting | Superseded by D-002, D-011 (PostgreSQL part later adopted in D-009) | 🔁 |
| D-023 | 2026-11-08 | **Mobile app as an essential tool** — the phone does what a fixed till cannot: set exchange rate from the phone (Must), receive goods by scanning offline (Should), daily closing summary (Should), suspicious-activity alerts (Should) | Makes the mobile app indispensable for owners and stock staff, not a copy of the desktop | ✅ (approved 15:18) |
| D-024 | 2026-11-08 | Transfers between branches moved from Should to **Could** | Frees ~1.5 days for D-023; branches keep working independently | ✅ (approved with D-023) |
| D-025 | 2026-11-08 | Schedule: P4 5 days, P5 8 days, buffer 1 day (total 40) | Fits D-023 inside 8 weeks; cut line applied strictly | ✅ (approved with D-023) |
