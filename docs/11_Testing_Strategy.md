# 11 — Testing Strategy

**Product:** Cedar POS · **Version:** 1.0 (planning) · **Date:** 2026-11-08

---

## 1. Goals

1. **Money is always right**: totals, tax, rounding, change, cash expected per currency.
2. **Nothing is lost or duplicated** when the network fails.
3. **Data never leaks** between businesses or to users without permission.
4. **The demo works** on real hardware and with the network unplugged.

The rule from the project brief applies: **a feature is not "done" until its tests pass and its acceptance criteria are verified** (document 12). That result is written in `PROJECT_STATUS.md`.

---

## 2. Test pyramid and where effort goes

| Level | What | Tools | Share of effort |
|---|---|---|---|
| **Unit** | `pos-core` money/tax/pricing/rounding/numbering; server services with mocks; Dart money and formula helpers | JUnit 5, AssertJ, Mockito, `flutter_test` | 45% |
| **Integration (DB)** | Repositories, migrations, sale ingest with real PostgreSQL, stock locking, change feed | Spring Boot Test + **Testcontainers** (PostgreSQL image — small and fast) or a dedicated `cedarpos_test` DB if Docker isn't available | 25% |
| **API** | Every endpoint: success, 401, 403, other tenant (404), validation (422) | MockMvc / RestAssured | 10% |
| **Sync / chaos** | Duplicate, out-of-order, lost-response, concurrent offline tills | JUnit scenario tests against the running server + desktop `SyncEngine` | 10% |
| **UI** | Desktop smoke (login → sale → payment); Flutter widget tests for key screens; one Flutter integration test (offline count) | TestFX, `flutter_test`, `integration_test` | 5% |
| **Manual / exploratory** | Hardware, usability, full demo scripts | Checklists (§7) | 5% |

**Coverage targets:** `pos-core` ≥ 90% lines; server `sales`, `inventory`, `cash`, `sync` ≥ 80%; overall server ≥ 70%. Coverage is a guide, not the goal.

---

## 3. Critical unit tests (`pos-core`)

| ID | Case | Expected |
|---|---|---|
| T-MON-01 | $7.40 at 89,500, step 1,000 | LBP due 662,000; rounding recorded |
| T-MON-02 | LBP-priced item 65,000 + USD item $1.00 | USD total 1.7263 → 1.73; LBP due 155,000 |
| T-MON-03 | Pay $10 for $7.40, change mixed | $2 + 54,000 LBP |
| T-MON-04 | Split tender $5 + 492,000 LBP for $10.50 (exact LBP would be 492,250) | Short 250 LBP ≤ half step (500) → complete; rounding recorded |
| T-MON-05 | Tender short by more than half an LBP step | Cannot complete |
| T-MON-06 | No `double` anywhere in money API | Architecture test (ArchUnit) fails on `double`/`float` in `money`, `pricing` |
| T-TAX-01 | Inclusive 11% on $11.10 | Tax $1.10 |
| T-TAX-02 | 10% invoice discount spread over lines with 11% and 0% tax | Tax computed on discounted lines; sum of line discounts = invoice discount exactly (remainder cents assigned to the largest line) |
| T-TAX-03 | Exempt product | Tax 0 |
| T-PRC-01 | Box of 24 scanned, box price absent | Price = base × 24 |
| T-PRC-02 | Branch override price present | Override used |
| T-CSH-01 | Expected cash per currency with sales, change, refunds, pay-ins/outs | Matches formula BR-CSH-02 |
| T-NUM-01 | Format/parse `B01-T02-000123` | Round-trips; rejects invalid |
| T-AVG-01 | Weighted average: 10 @ $2 then 20 @ $3 | Avg $2.6667 |
| T-AVG-02 | Receive when on-hand negative | Avg = new unit cost |
| T-REC-01 | Recommendation: D=4/day, L=3, C=14, S=12, H=20, O=0, pack 6 | 4×17+12−20 = 60 → 60 |

*(Each case is also a living example of the business rules for new developers.)*

---

## 4. Server integration & API tests (highlights)

| ID | Scenario | Expected |
|---|---|---|
| T-SAL-01 | Ingest valid sale | Sale, lines, payments, SALE movements, stock decreased, costs set, change_log rows — all in one transaction |
| T-SAL-02 | Ingest same opId twice | Second = DUPLICATE; row counts unchanged |
| T-SAL-03 | Same sale id, different opId | Rejected by PK; treated as DUPLICATE |
| T-SAL-04 | Server totals differ from device by $0.05 | Saved + `TOTAL_MISMATCH` issue |
| T-SAL-05 | Credit sale | Customer ledger +amount |
| T-RET-01 | Return 2 of 3 then 2 more | Second return FLAGGED `OVER_RETURN`; `qty_returned` = 4 |
| T-STK-01 | 50 parallel sales of the same item (threads) | Final quantity exact; `qty_after` sequence consistent |
| T-STK-02 | Reconciliation query | `stock_levels` = Σ movements for all rows |
| T-CNT-01 | Sales during a count, then approve | Correction excludes sales made after counted_at |
| T-PUR-01 | Partial receive then full | Status PARTIAL → RECEIVED; avg cost updated twice |
| T-CSH-02 | Open second session on same register | 409 SESSION_ALREADY_OPEN |
| T-ALR-01 | Session with 5 voids (threshold 3) | One SUSPICIOUS_ACTIVITY alert to owner/manager, none to the cashier |
| T-ALR-02 | Last session of the day closes | One DAILY_SUMMARY with totals equal to the profit report for that day |
| T-SEC-01 | User of business A requests a product of business B | 404 (not 403 — no existence leak) |
| T-SEC-02 | Cashier calls `/reports/profit` | 403 PERMISSION_DENIED |
| T-SEC-03 | Manager of B01 queries B02 | 403 BRANCH_NOT_ALLOWED |
| T-SEC-04 | Deactivated device pushes | 401 DEVICE_REVOKED (except pre-revocation ops → flagged) |
| T-SEC-05 | 6 wrong PINs | 423 ACCOUNT_LOCKED for 5 min |
| T-MIG-01 | Flyway migrate on empty DB + on previous version | Success; schema validated by Hibernate `validate` |
| T-API-xx | Generated per endpoint | success / 401 / 403 / 404-other-tenant / 422 |

---

## 5. Sync and chaos tests (the "network failure" proof)

| ID | Scenario | How | Pass criterion |
|---|---|---|---|
| T-SYN-01 | 200 sales offline, reconnect | Desktop with server stopped → start server | All 200 on server within 60 s; no duplicates; outbox empty |
| T-SYN-02 | Response lost after commit | Test proxy drops response of 1 in 3 pushes | Every sale exactly once |
| T-SYN-03 | Server killed mid-batch | Kill process during push of 50 | After restart: all 50 exactly once |
| T-SYN-04 | Two tills offline sell last unit | Two desktop instances, same branch | Both sales applied; stock −1; negative-stock listed |
| T-SYN-05 | Same receipt returned on both tills offline | — | One APPLIED, one FLAGGED; sync issue visible on mobile |
| T-SYN-06 | Clock 3 h wrong | Change OS time | CLOCK_SKEW issue; reports by recorded time |
| T-SYN-07 | Price change while till offline | — | Old price on sale; new price after pull |
| T-SYN-08 | Pull resume | Interrupt pull mid-page | No partial apply; resumes from last saved `since` |
| T-SYN-09 | Mobile count offline 100 lines → online | Airplane mode | Lines on server once; count submitted |
| T-SYN-10 | 72 h offline limit | Fake clock | Register enters Restricted; close session still possible |
| T-SYN-12 | Goods received on phone offline, two phones on the same order | Airplane mode, then reconnect | Quantities summed; over-receipt flagged; avg cost correct |
| T-SYN-13 | Rate set on phone | Owner sets new rate | All online terminals show the notice within 30 s; basket in payment keeps old rate |
| T-SYN-11 | App crash during sale commit | Kill process in the middle of a transaction (debug hook) | Either full sale or none; invoice counter consistent |

A small **fault-injection proxy** (a filter in the `test` profile) makes T-SYN-02/03 repeatable.

---

## 6. Desktop & mobile tests

| Area | Tests |
|---|---|
| Desktop local repos | SQLite in-memory; migrations 001→latest |
| Desktop SyncEngine | Against a mocked API: retry/backoff, status transitions, FAILED handling |
| Desktop UI smoke (TestFX) | PIN login → scan 3 items → pay mixed currency → receipt preview shows correct totals |
| Scanner filter | Fast key sequence → scan event; slow typing → normal input |
| Flutter unit | Money formatters, recommendation formula, permission guard |
| Flutter widget | Dashboard renders snapshot & offline banner; approval detail buttons; count keypad |
| Flutter integration | Login (mock server) → start count → offline → scan 3 → online → submit |

---

## 7. Manual test checklists

**Hardware (per printer/drawer/scanner model)**
- [ ] Test print from Settings; receipt fits 80 mm; LBP numbers aligned
- [ ] Drawer opens on cash sale, not on card-only sale
- [ ] Scanner adds item while focus is on another field
- [ ] Printer unplugged → sale completes, "Reprint" offered later

**Usability**
- [ ] A new person completes a 5-item cash sale with the keyboard in < 20 s after a 2-minute explanation
- [ ] Every register action reachable by keyboard
- [ ] Offline state understood by a non-technical user without explanation

**Demo script (end-to-end, before each review)**
1. Owner sets rate 89,500 on phone → appears on register within 30 s
2. Cashier opens session, sells mixed-currency sale, prints
3. Discount 25% → approval on phone → applied
4. Unplug network → 5 sales → stock keeper counts offline on phone
5. Reconnect → everything syncs → live feed and dashboard update
6. Return one item → refund LBP
7. Close session with a $2 difference → manager notified → Z-report
8. Profit report and inventory valuation match the manual calculation

---

## 8. Test data

- `R__demo_data.sql`: 2 branches, 4 devices, 8 users (all roles), 500 products (40 with variants, 30 with packagings, 15 LBP-priced), 10 suppliers, 50 customers (10 with credit), 90 days of generated sales (~25k) with returns and expenses.
- A printed **barcode sheet** (`scripts/demo/barcodes.pdf`) for scanning in demos.
- Test fixtures built with builders (`aSale().withLine(...).paidCashUsd(...)`).

---

## 9. Definition of Done (per task)

1. Code builds; no warnings in new code.
2. Unit + integration tests written and passing (CI green).
3. Acceptance criteria of the roadmap item verified **and evidence noted** (test names, screenshot or demo step) in `PROJECT_STATUS.md`.
4. API documented in Swagger; DB changes as a Flyway migration.
5. `CHANGELOG.md` updated; any decision added to `DECISIONS.md`.
