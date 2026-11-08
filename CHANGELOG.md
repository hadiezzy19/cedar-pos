# CHANGELOG

## [Planning 1.1] — 2026-11-08
### Changed
- Main database switched from SQL Server to **PostgreSQL 18** across all documents and `cedar_pos.dbml` (types: uuid, timestamptz, numeric, boolean, text; partial indexes; `UPDATE … RETURNING` stock locking; pg_dump + WAL archiving backups) — D-009
- Mobile app upgraded: rate from phone (Must), offline goods receiving by scanning, daily closing summary, suspicious-activity alerts — new screens M25–M30, sync op `PURCHASE_RECEIPT`, endpoints, tests T-SYN-12/13, T-ALR-01/02 — D-023
- Transfers between branches → Could; roadmap P4 5 d, P5 8 d, buffer 1 d — D-024, D-025

## [Planning 1.0] — 2026-11-08
### Added
- Critical analysis and open questions (doc 00)
- Planning package: SRS, architecture, roles & permissions, database design + ERD (DBML + Mermaid), table reference, offline & sync design, API plan, desktop and mobile screen structures, folder structure, testing strategy, roadmap, Ledger design system with mockups
- Combined PDF: `Cedar_POS_Planning_Package.pdf`

### Changed
- Scope re-planned for an internship: 2 months, 1 developer, English only, USD + LBP, SQL Server on one machine, no commercial features (see DECISIONS D-002 … D-022)
