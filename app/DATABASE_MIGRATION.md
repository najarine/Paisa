# Paisa — Production-Safe Database Migration & Data Preservation Documentation

## Overview
Paisa is an offline-first financial management application for Android. This document describes the database schema versioning, migration strategy, data integrity safeguards, and verification procedures designed to ensure zero data loss during application updates.

---

## Database Configuration
- **Database Class**: `com.paisa.najarine.data.local.PaisaDatabase`
- **Database Name**: `paisa_financial_db`
- **Database Technology**: Room Database over standard SQLite engine.
- **Current Database Version**: `4`

---

## Migration Architecture

### Non-Negotiable Data Preservation Rules
1. `fallbackToDestructiveMigration()` is **strictly disabled** and forbidden across all database instantiation configurations.
2. Every schema change increments `version` in `@Database(...)` and includes explicit, versioned `Migration(oldVersion, newVersion)` instances.
3. Schema additions use `CREATE TABLE IF NOT EXISTS` or `ALTER TABLE ... ADD COLUMN` with safe non-null defaults or backfill queries.
4. Schema updates operate within SQLite database transactions provided by Room to guarantee atomicity.
5. Direct upgrades (e.g. `v1 -> v4`) are fully supported via sequential Room migration execution (`v1 -> v2 -> v3 -> v4`).

---

## Migration History & Changes

### Version 1 (v1)
- **Initial Core Financial Schema**:
  - `workspaces`: Workspace partitioning for multi-account isolate boundaries.
  - `wallets`: Cash, Bank, bKash, Nagad, Rocket, Credit Card, and MFS accounts.
  - `transactions`: Incomes, Expenses, Transfers with category, amount, tags, date.
  - `categories`: Default and custom category metadata.
  - `budgets`: Monthly category spending limits and period tracking.

### Version 1 → Version 2 (Migration: `MIGRATION_1_2`)
- **Schema Changes**:
  - Validated and created missing core financial tables (`workspaces`, `wallets`, `transactions`, `categories`, `budgets`) safely using `CREATE TABLE IF NOT EXISTS`.
- **Data Transformation**:
  - No existing table structure altered or recreated.
- **Preservation Strategy**:
  - Preserved existing primary keys (`id`), foreign keys (`workspaceId`, `walletId`), amounts, dates, and category strings.

### Version 2 → Version 3 (Migration: `MIGRATION_2_3`)
- **Schema Changes**:
  - Introduced financial expansion tables:
    - `goals_vaults`: Savings goals, goal amounts, and completion status.
    - `bills_subscriptions`: Recurring payments, payment cycles, and due dates.
    - `debts_dena_pona`: Loans, receivables, debt types, and settlement statuses.
- **Data Transformation**:
  - Schema additive migration only.
- **Preservation Strategy**:
  - New tables created without touching existing transaction or wallet records.

### Version 3 → Version 4 (Migration: `MIGRATION_3_4`)
- **Schema Changes**:
  - Added `updatedAt` timestamp column to `transactions` (`ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0`).
  - Added offline sync queue table `sync_outbox` for durable pending mutation syncs.
  - Added `sync_tombstones` table to track deleted records and protect against cloud sync deletion resurrections.
- **Data Transformation**:
  - Backfilled existing transactions' `updatedAt` timestamps with `dateMillis` (`UPDATE transactions SET updatedAt = dateMillis WHERE updatedAt = 0`).
- **Preservation Strategy**:
  - Zero transaction records modified or lost. All IDs, balances, attachments, notes, and tags preserved intact.

---

## Data Integrity & Offline-First Sync Integration
1. **Durable Sync Outbox (`sync_outbox`)**:
   - Every local create, edit, or settlement inserts an outbox mutation record.
   - Database migrations preserve all pending outbox tasks across app updates so offline transactions are synced to Firebase/cloud once connectivity is re-established.
2. **Stable Identifiers**:
   - UUIDs are generated at entity creation and preserved permanently. Syncing never generates replacement local IDs.
3. **No Destructive Logout/Clear**:
   - User logouts or preference resets do NOT clear the local SQLite database. Database destruction requires an explicit user action ("Delete All Local Data") with affirmative dialog confirmation.

---

## Automated Migration Testing
Database migration integrity is validated via automated unit tests in `DatabaseMigrationTest.kt`:
- **Test 1 (`testAllMigrationsPreserveData`)**: Seeds realistic user data across accounts (Cash, bKash), transactions (Salary, Rent, Food), debt records, categories, and budgets into an in-memory database instance, applies migrations 1 → 2 → 3 → 4, and verifies 100% record retention, key stability, balance accuracy, and schema compliance.
