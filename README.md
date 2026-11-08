# Cedar POS

Retail point-of-sale system — **internship project** (1 developer, 8 weeks).

| Part | Technology |
|---|---|
| Desktop POS (cashier + back office) | Java · JavaFX · SQLite (offline) |
| Mobile app (owners, managers, stock staff) | Flutter · SQLite (offline) |
| Backend API | Java · Spring Boot 4 · PostgreSQL 18 |

Key features: offline selling with safe synchronization, USD + LBP dual currency, multi-branch inventory, roles & permissions, audit trail.

## Repository layout
```
docs/       planning documents (SRS, architecture, database, API, screens, tests, roadmap)
database/   cedar_pos.dbml — ERD source (paste into dbdiagram.io)
design/     UI mockups (Ledger design system)
java/       Maven project: pos-core, pos-server, pos-desktop   (Phase P0)
mobile/     Flutter app                                         (Phase P5)
scripts/    dev and backup scripts
```

## Status
See [PROJECT_STATUS.md](PROJECT_STATUS.md) · [ROADMAP.md](ROADMAP.md) · [DECISIONS.md](DECISIONS.md) · [CHANGELOG.md](CHANGELOG.md)
