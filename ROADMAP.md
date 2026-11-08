# ROADMAP

Full detail: `docs/12_Development_Roadmap.md`. Start assumed Mon 16 Nov 2026 (after document approval).

| Phase | Days | Depends on | Outcome | Status |
|---|---|---|---|---|
| P0 Setup & skeletons | 3 | Docs approved | Repo, builds, PostgreSQL schema migrated, apps start | ⏳ Waiting for approval |
| P1 Backend core & catalog API | 6 | P0 | Auth, org, rate, catalog, pull feed | ⏳ |
| P2 Desktop shell & pull sync | 4 | P1 | Activation, PIN login, local catalog | ⏳ |
| P3 Register & push sync ★ | 8 | P2 | Offline mixed-currency sales, cash sessions, receipts | ⏳ |
| P4 Operations | 5 | P3 | Returns, customers, inventory, purchasing, expenses | ⏳ |
| P5 Mobile | 8 | P3, P4 | Rate from phone, dashboard, live, scan, offline counts & receiving, approvals, daily summary, alerts | ⏳ |
| P6 Reports & hardening | 5 | P4, P5 | Reports, chaos tests, backups, demo | ⏳ |
| Buffer | 1 | — | — | ⏳ |

Cut line (drop first if late): labels/exports → FCM push → extra alert rules → variant generator → recommendations → credit accounts → phone approvals → employees/VAT reports. Transfers between branches already moved to Could.
