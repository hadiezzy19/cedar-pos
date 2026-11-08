# 03 — User Roles and Permissions Matrix

**Product:** Cedar POS · **Version:** 1.0 (planning) · **Date:** 2026-11-08

---

## 1. How permissions work (plain language)

- The system checks **permissions**, not role names. A permission is a small code like `pos.sell` or `inventory.adjust`.
- A **role** is just a named bundle of permissions. Five roles come pre-configured (below). The Owner can create custom roles by ticking permissions (Should).
- Each user has **one role** and access to **one or more branches**. A Branch Manager of B01 cannot see B02 unless given access.
- Some actions are allowed only **up to a limit** (e.g. discounts ≤ 10%). Above the limit, an approval is needed (document 01 §4.14).
- The server enforces every permission. The apps also hide buttons the user can't use, but hiding is only for convenience — **the server is the real gatekeeper**.
- Offline, the desktop checks permissions from its cached copy of the user's role.

## 2. Default roles

| Role | Branch scope | Typical person |
|---|---|---|
| **Owner** | All branches (always) | Business owner |
| **Branch Manager** | Assigned branches | Store manager |
| **Cashier** | One assigned branch | Till operator |
| **Stock Keeper** | Assigned branches | Warehouse / inventory clerk |
| **Accountant** | All branches, read-mostly | Bookkeeper |

## 3. Permissions matrix

Legend: ✅ allowed · ◐ allowed with limit / own data only · 🔑 needs approval (PIN or mobile) · ✗ not allowed

### 3.1 Point of sale

| Code | Description | Owner | Manager | Cashier | Stock K. | Accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|
| `pos.sell` | Make sales | ✅ | ✅ | ✅ | ✗ | ✗ |
| `pos.discount` | Line/invoice discount up to limit | ✅ | ✅ | ◐ ≤10% | ✗ | ✗ |
| `pos.discount.over_limit` | Discount above the role limit (Manager up to 50%) | ✅ | ◐ ≤50% | 🔑 | ✗ | ✗ |
| `pos.price_override` | Change price on a line | ✅ | ✅ | 🔑 | ✗ | ✗ |
| `pos.void_line` | Remove line before payment | ✅ | ✅ | ✅ | ✗ | ✗ |
| `pos.void_sale` | Void completed sale (same session) | ✅ | ✅ | 🔑 | ✗ | ✗ |
| `pos.hold` | Hold / recall sales | ✅ | ✅ | ✅ | ✗ | ✗ |
| `pos.reprint` | Reprint receipt | ✅ | ✅ | ✅ | ✗ | ✗ |
| `pos.no_sale` | Open drawer without sale | ✅ | ✅ | 🔑 | ✗ | ✗ |
| `pos.sell_on_credit` | Charge to customer account (within limit) | ✅ | ✅ | ✅ | ✗ | ✗ |
| `pos.credit_over_limit` | Exceed customer credit limit | ✅ | ✅ | 🔑 | ✗ | ✗ |
| `pos.negative_stock` | Sell when stock ≤ 0 (if policy = warn) | ✅ | ✅ | ✅ | ✗ | ✗ |

### 3.2 Returns

| Code | Description | Owner | Manager | Cashier | Stock K. | Accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|
| `returns.with_receipt` | Return against receipt | ✅ | ✅ | ✅ | ✗ | ✗ |
| `returns.without_receipt` | Return without receipt | ✅ | ✅ | 🔑 | ✗ | ✗ |
| `returns.cash_refund_any` | Refund cash for a non-cash sale | ✅ | ✅ | 🔑 | ✗ | ✗ |

### 3.3 Cash

| Code | Description | Owner | Manager | Cashier | Stock K. | Accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|
| `cash.open` | Open cash session | ✅ | ✅ | ✅ | ✗ | ✗ |
| `cash.close` | Close own session | ✅ | ✅ | ✅ | ✗ | ✗ |
| `cash.close_any` | Close another user's session | ✅ | ✅ | ✗ | ✗ | ✗ |
| `cash.pay_in_out` | Pay-in / pay-out | ✅ | ✅ | 🔑 | ✗ | ✗ |
| `cash.view_expected` | See expected cash before counting | ✅ | ✅ | ✗ | ✗ | ✅ |
| `cash.x_report` | Print X-report | ✅ | ✅ | ◐ own | ✗ | ✅ |
| `cash.sessions_view` | View all sessions & differences | ✅ | ✅ | ✗ | ✗ | ✅ |

### 3.4 Catalog

| Code | Description | Owner | Manager | Cashier | Stock K. | Accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|
| `catalog.view` | View products | ✅ | ✅ | ✅ | ✅ | ✅ |
| `catalog.edit` | Create/edit products, categories, variants, barcodes | ✅ | ✅ | ✗ | ✅ | ✗ |
| `catalog.price_edit` | Change selling prices | ✅ | ✅ | ✗ | ✗ | ✗ |
| `catalog.view_cost` | See cost prices & margins | ✅ | ✅ | ✗ | ◐ | ✅ |
| `catalog.import` | Import products | ✅ | ✅ | ✗ | ✗ | ✗ |
| `catalog.labels` | Print labels | ✅ | ✅ | ✗ | ✅ | ✗ |

### 3.5 Inventory

| Code | Description | Owner | Manager | Cashier | Stock K. | Accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|
| `inventory.view` | Stock levels & movements | ✅ | ✅ | ◐ qty only | ✅ | ✅ |
| `inventory.adjust` | Create adjustments | ✅ | ✅ | ✗ | ✅ | ✗ |
| `inventory.adjust_approve` | Approve large adjustments | ✅ | ✅ | ✗ | ✗ | ✗ |
| `inventory.count` | Perform stock counts | ✅ | ✅ | ✗ | ✅ | ✗ |
| `inventory.count_approve` | Approve count & post corrections | ✅ | ✅ | ✗ | ✗ | ✗ |
| `inventory.transfer` | Create / send / receive transfers | ✅ | ✅ | ✗ | ✅ | ✗ |
| `inventory.reorder_edit` | Edit reorder points | ✅ | ✅ | ✗ | ✅ | ✗ |

### 3.6 Purchasing & suppliers

| Code | Description | Owner | Manager | Cashier | Stock K. | Accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|
| `purchases.view` | View purchases | ✅ | ✅ | ✗ | ✅ | ✅ |
| `purchases.create` | Create/edit purchases | ✅ | ✅ | ✗ | ✅ | ✗ |
| `purchases.receive` | Receive goods | ✅ | ✅ | ✗ | ✅ | ✗ |
| `suppliers.manage` | Create/edit suppliers | ✅ | ✅ | ✗ | ✅ | ✅ |
| `suppliers.pay` | Record supplier payments | ✅ | ✅ | ✗ | ✗ | ✅ |

### 3.7 Customers

| Code | Description | Owner | Manager | Cashier | Stock K. | Accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|
| `customers.view` | Search & view | ✅ | ✅ | ✅ | ✗ | ✅ |
| `customers.manage` | Create/edit | ✅ | ✅ | ◐ create | ✗ | ✅ |
| `customers.credit_limit` | Set credit limit | ✅ | ✅ | ✗ | ✗ | ✅ |
| `customers.receive_payment` | Take payment on account | ✅ | ✅ | ✅ | ✗ | ✅ |

### 3.8 Expenses & reports

| Code | Description | Owner | Manager | Cashier | Stock K. | Accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|
| `expenses.view` | View expenses | ✅ | ✅ | ✗ | ✗ | ✅ |
| `expenses.create` | Record expenses | ✅ | ✅ | ✗ | ✗ | ✅ |
| `dashboard.view` | Dashboard | ✅ | ✅ | ✗ | ◐ stock tiles | ✅ |
| `live_sales.view` | Live sales feed | ✅ | ✅ | ✗ | ✗ | ✅ |
| `reports.sales` | Sales reports | ✅ | ✅ | ✗ | ✗ | ✅ |
| `reports.financial` | Profit, P&L, VAT | ✅ | ◐ own branch | ✗ | ✗ | ✅ |
| `reports.inventory` | Stock & valuation | ✅ | ✅ | ✗ | ✅ | ✅ |
| `reports.employees` | Employee performance | ✅ | ✅ | ✗ | ✗ | ✗ |

### 3.9 Administration

| Code | Description | Owner | Manager | Cashier | Stock K. | Accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|
| `users.manage` | Create/edit users | ✅ | ◐ own branch, not Owners | ✗ | ✗ | ✗ |
| `roles.manage` | Create/edit roles | ✅ | ✗ | ✗ | ✗ | ✗ |
| `branches.manage` | Branches | ✅ | ✗ | ✗ | ✗ | ✗ |
| `devices.manage` | Activate/deactivate devices | ✅ | ◐ own branch | ✗ | ✗ | ✗ |
| `settings.manage` | Business settings, tax, receipt | ✅ | ✗ | ✗ | ✗ | ✗ |
| `exchange_rate.set` | Set USD→LBP rate | ✅ | ✅ | ✗ | ✗ | ✅ |
| `approvals.decide` | Approve requests (PIN or mobile) | ✅ | ✅ | ✗ | ✗ | ✗ |
| `audit.view` | View audit log | ✅ | ◐ own branch | ✗ | ✗ | ✅ |
| `sync.issues` | View/resolve sync issues | ✅ | ✅ | ✗ | ✗ | ✗ |

## 4. Mobile app access by role

| Mobile area | Owner | Manager | Stock Keeper | Accountant | Cashier |
|---|:-:|:-:|:-:|:-:|:-:|
| Dashboard | ✅ | ✅ | stock tiles | ✅ | — |
| Live sales | ✅ | ✅ | ✗ | ✅ | — |
| Product lookup / scan | ✅ | ✅ | ✅ | ✅ | — |
| Stock counting | ✅ | ✅ | ✅ | ✗ | — |
| Adjustments | ✅ | ✅ | ✅ | ✗ | — |
| Branch monitoring | ✅ | ◐ own | ✗ | ✅ | — |
| Employee monitoring | ✅ | ◐ own | ✗ | ✗ | — |
| Approvals | ✅ | ✅ | ✗ | ✗ | — |
| Set exchange rate | ✅ | ✅ | ✗ | ✅ | — |
| Receive goods (scan) | ✅ | ✅ | ✅ | ✗ | — |
| Daily closing summary | ✅ | ◐ own | ✗ | ✅ | — |
| Suspicious-activity alerts | ✅ | ◐ own | ✗ | ✗ | — |
| Purchase recommendations | ✅ | ✅ | ✅ | ✗ | — |
| Reports | ✅ | ✅ | inventory only | ✅ | — |

Cashiers do not use the mobile app in this version (login is refused with a friendly message).

## 5. Implementation notes

- Seeded by Flyway migration `V2__seed_permissions_and_roles.sql`.
- Limits are stored on the role: `roles.max_discount_percent`, plus business setting `adjustment_approval_threshold_usd`.
- The token carries `role_id` and a permissions **version**; when a role changes, devices pull the new permission set via sync.
- Every permission denial is logged (not audited) to detect misuse.
