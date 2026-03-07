---
title: "Extensions: pg_duckdb"
source: "https://planetscale.com/docs/postgres/extensions/pg_duckdb"
author:
  - "[[PlanetScale]]"
published:
created: 2025-12-22
description: "pg_duckdb is a Postgres extension that embeds DuckDB, a high-performance analytical database, directly into Postgres."
tags:
  - "clippings"
---
[Skip to main content](https://planetscale.com/docs/postgres/extensions/#content-area)

We don’t recommend running `pg_duckdb` directly on your PlanetScale Postgres database as it can consume significant resources during analytical queries. If you want to use DuckDB for analytics, we recommend using [MotherDuck](https://motherduck.com/) to host your analytical workloads separately.

## Dashboard Configuration

This extension requires activation via the PlanetScale dashboard before it can be used. It must be enabled through shared libraries and requires a database restart.To enable pg\_duckdb:

## Parameters

### duckdb.postgres\_role

- **Type**: String
- **Default**: `pscale_superuser`
- **Description**: Specifies the Postgres role that is allowed to use DuckDB execution and manage secrets.

### duckdb.memory\_limit

- **Type**: Integer
- **Default**: 0
- **Description**: Maximum memory DuckDB can use per connection in megabytes. Setting to 0 activates DuckDB’s default (80% of available RAM).

## Usage

After enabling the extension through the dashboard, you can install it in your database:Once installed, you can use DuckDB’s analytical capabilities directly from PostgreSQL. For example:

## External Documentation

For more detailed information about `pg_duckdb` usage and functionality, see the [official `pg_duckdb` documentation](https://github.com/duckdb/pg_duckdb).

Was this page helpful?