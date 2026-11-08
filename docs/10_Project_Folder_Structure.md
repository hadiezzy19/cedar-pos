# 10 — Project Folder Structure

**Product:** Cedar POS · **Version:** 1.0 (planning) · **Date:** 2026-11-08

One Git repository (monorepo) holds all three applications, the documents and the scripts. A single history makes it easy to see that a sync change touched the server, the desktop and the mobile app together.

```
cedar-pos/
├── README.md                     # how to run everything locally (5 steps)
├── PROJECT_STATUS.md             # completed / in progress / blocked / next task
├── ROADMAP.md                    # phases & acceptance criteria (summary of doc 12)
├── DECISIONS.md                  # architecture & business decisions log
├── CHANGELOG.md                  # per version / per phase changes
├── .gitignore  .editorconfig
├── .github/workflows/
│   ├── java.yml                  # build + test pos-core, pos-server, pos-desktop
│   └── flutter.yml               # analyze + test mobile
│
├── docs/                         # this planning package (Markdown) + exported PDF
│   ├── 00_… 13_*.md
│   ├── images/                   # mockups, rendered diagrams
│   ├── diagrams/                 # *.mmd Mermaid sources
│   └── api/openapi.json          # exported from the server build
│
├── database/
│   ├── cedar_pos.dbml            # ERD source (dbdiagram.io)
│   └── sql/                      # ad-hoc SQL: views for reports, checks
│       └── reconcile_stock.sql
│
├── design/
│   ├── tokens.json               # Ledger design tokens (source of truth)
│   ├── fonts/                    # Inter, Lora, JetBrains Mono (OFL)
│   └── mockups/                  # HTML mockups
│
├── java/                         # Maven multi-module (open this folder in NetBeans)
│   ├── pom.xml                   # parent: Java 21, dependency versions, plugins
│   │
│   ├── pos-core/                 # shared, no framework dependencies
│   │   └── src/main/java/com/cedarpos/core/
│   │       ├── money/            # Money, Currency, ExchangeRate, Rounding (LBP step)
│   │       ├── tax/              # TaxCalculator (inclusive/exclusive)
│   │       ├── pricing/          # SaleCalculator: lines, discounts, totals, change
│   │       ├── numbering/        # DocumentNumber format/parse
│   │       ├── sync/             # SyncOperation, PushRequest/Response, PullResponse DTOs
│   │       └── model/            # shared enums (PaymentMethod, MovementType…)
│   │   └── src/test/java/…       # most important unit tests of the project
│   │
│   ├── pos-server/               # Spring Boot application
│   │   ├── src/main/java/com/cedarpos/server/
│   │   │   ├── CedarPosServerApplication.java
│   │   │   ├── common/           # TenantContext, security config, errors, audit writer, ids
│   │   │   ├── identity/         # api/ application/ domain/ infrastructure/
│   │   │   ├── organization/
│   │   │   ├── currency/
│   │   │   ├── catalog/
│   │   │   ├── inventory/
│   │   │   ├── purchasing/
│   │   │   ├── sales/
│   │   │   ├── customers/
│   │   │   ├── cash/
│   │   │   ├── expenses/
│   │   │   ├── approvals/
│   │   │   ├── notifications/
│   │   │   ├── reporting/
│   │   │   ├── sync/
│   │   │   └── audit/
│   │   ├── src/main/resources/
│   │   │   ├── application.yml  application-dev.yml  application-test.yml
│   │   │   └── db/migration/     # V1__baseline_schema.sql, V2__seed_…, R__demo_data.sql
│   │   └── src/test/java/…       # unit, integration (DB), API, sync, tenant-isolation tests
│   │
│   └── pos-desktop/              # JavaFX application
│       ├── src/main/java/com/cedarpos/desktop/
│       │   ├── CedarPosApp.java  # JavaFX entry point
│       │   ├── app/              # AppContext (DI), Navigator, config, session state
│       │   ├── ui/
│       │   │   ├── components/   # MoneyLabel, CurrencyField, PinPad, StatusPill, KeyCap…
│       │   │   ├── register/     # login, open-session, sale, payment, returns, close…
│       │   │   └── backoffice/   # dashboard, catalog, inventory, purchasing, … admin
│       │   ├── local/            # SQLite: Database, migrations runner, repositories
│       │   ├── sync/             # SyncEngine, Outbox, PullApplier, ConnectivityMonitor
│       │   ├── api/              # ApiClient (HttpClient + Jackson), auth token store
│       │   ├── hardware/         # ReceiptPrinter, CashDrawer, ScannerInputFilter, receipt templates
│       │   └── util/
│       ├── src/main/resources/
│       │   ├── fxml/             # one FXML per screen
│       │   ├── css/              # ledger-light.css, ledger-dark.css
│       │   ├── fonts/  icons/
│       │   └── local-migrations/ # 001_init.sql …
│       ├── src/test/java/…       # local repo tests, sync engine tests, TestFX smoke
│       └── packaging/            # jpackage config, icon, installer scripts
│
├── mobile/                       # Flutter application
│   ├── pubspec.yaml
│   ├── lib/
│   │   ├── main.dart
│   │   ├── app/                  # app widget, router (go_router + guards), theme (Ledger)
│   │   ├── core/
│   │   │   ├── api/              # Dio client, interceptors, DTOs
│   │   │   ├── db/               # Drift database, tables, DAOs
│   │   │   ├── sync/             # SyncService, outbox, pull applier
│   │   │   ├── auth/             # session, secure storage, permissions
│   │   │   ├── money/            # Decimal helpers, formatters USD/LBP
│   │   │   └── widgets/          # MoneyText, StatusPill, KpiCard, ScannerView…
│   │   └── features/
│   │       ├── auth/  dashboard/  live_sales/  lookup/  counts/  adjustments/
│   │       ├── stock/  reorder/  approvals/  branches/  employees/  reports/
│   │       └── notifications/  sync_center/  settings/
│   │       # each feature: data/ domain/ presentation/
│   ├── test/                     # unit + widget tests
│   ├── integration_test/
│   ├── android/  ios/
│   └── assets/fonts/  assets/icons/
│
└── scripts/
    ├── dev/                      # start-sqlserver.ps1, reset-db.ps1, seed-demo.ps1
    ├── backup/                   # backup-full.sql, backup-log.sql, restore-point-in-time.sql,
    │                             # schedule-backups.ps1, RESTORE.md
    └── demo/                     # offline-demo checklist, sample barcodes PDF
```

## Naming conventions

| Item | Convention | Example |
|---|---|---|
| Java packages | `com.cedarpos.<app>.<module>.<layer>` | `com.cedarpos.server.sales.application` |
| Java classes | PascalCase; suffixes `Controller`, `Service`, `Repository`, `Request`, `Response` | `SaleIngestService` |
| DB tables / columns | `snake_case`, plural tables | `sale_lines.unit_price_usd` |
| Flyway files | `V{n}__{description}.sql` | `V7__add_reorder_columns.sql` |
| REST paths | kebab-case plural | `/stock-adjustments` |
| Dart files | `snake_case.dart`; one widget per file for screens | `count_session_screen.dart` |
| Branches (Git) | `main`, `feature/<phase>-<topic>` | `feature/p3-payment-dialog` |
| Commits | Conventional commits | `feat(sync): idempotent sale ingest` |
