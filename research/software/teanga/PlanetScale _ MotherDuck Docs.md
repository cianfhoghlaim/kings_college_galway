---
title: "PlanetScale | MotherDuck Docs"
source: "https://motherduck.com/docs/integrations/databases/planetscale/"
author:
published:
created: 2025-12-24
description: "Connect PlanetScale Postgres to MotherDuck using pg_duckdb extension or the Postgres connector for analytical query acceleration"
tags:
  - "clippings"
---
PlanetScale offers hosted PostgreSQL and MySQL Vitess Databases. MotherDuck supports PlanetScale Postgres via the [pg\_duckdb extension](https://motherduck.com/docs/concepts/pgduckdb/), as well as the [Postgres Connector](https://motherduck.com/docs/integrations/databases/postgres/). In our internal benchmarking, pg\_duckdb offers 100x or greater query acceleration for analytical queries when compared to vanilla Postgres.

## Prerequisites

Before connecting PlanetScale to MotherDuck, ensure you have:

- A PlanetScale account with a Postgres database created
- The `pg_duckdb` extension enabled in your PlanetScale database (see [PlanetScale extension documentation](https://planetscale.com/docs/postgres/extensions/pg_duckdb))
- A MotherDuck account and authentication token (get your token from the [MotherDuck dashboard](https://app.motherduck.com/))
- Database connection credentials from your PlanetScale dashboard (host, port, username, password, database name)

## Connecting pg\_duckdb to MotherDuck

To run pg\_duckdb, make sure to add it your [extensions in PlanetScale](https://planetscale.com/docs/postgres/extensions/pg_duckdb).

```sql
-- Grant necessary permissions to the PlanetScale superuser
GRANT CREATE ON SCHEMA public to pscale_superuser;

-- Create the pg_duckdb extension in your Postgres database
CREATE EXTENSION pg_duckdb;

-- Enable a MotherDuck connection with your authentication token
CALL duckdb.enable_motherduck(<your token>);
```

To swap tokens, you can drop the MotherDuck connection and then re-add with:

```sql
-- Remove the existing MotherDuck server connection
DROP SERVER motherduck CASCADE;

-- Re-enable MotherDuck with a new authentication token
CALL duckdb.enable_motherduck(<your token>);
```

### Using Read Replicas with PlanetScale

Switching from read-write to read-only is done with the following SQL statement in Postgres:

```sql
-- Create a snapshot of your MotherDuck database to ensure consistency
SELECT * FROM duckdb.raw_query('CREATE SNAPSHOT OF <db_name>');

-- Drop the existing MotherDuck connection
DROP SERVER motherduck CASCADE;

-- Re-enable MotherDuck with your read-only token
CALL duckdb.enable_motherduck(<your read only token>);

-- Refresh the database to sync with the snapshot
SELECT * FROM duckdb.raw_query('REFRESH DATABASE <db_name>');
```

### Reading from MotherDuck

Once the catalog is in sync between MotherDuck and Postgres, the data can be queried directly from Postgres. If it is out of sync for any reason, it can be re-sync'd with the following SQL command:

```sql
-- Terminate the pg_duckdb sync worker to force a re-sync
SELECT * FROM pg_terminate_backend((
  SELECT pid FROM pg_stat_activity WHERE backend_type = 'pg_duckdb sync worker'
));
```

#### Sample MotherDuck Queries

Once the catalog is synchronized to Postgres, we can query the data as if it was normal data in Postgres.

```sql
-- Query data from a MotherDuck database and schema
-- Note: Non-main schemas use the ddb$database$schema naming convention
SELECT * 
FROM "ddb$sample_data$nyc".taxi
ORDER BY tpep_dropoff_datetime DESC 
LIMIT 10;
```

Of course, we can also join with data in Postgres.

```sql
-- Join MotherDuck data with local Postgres tables
SELECT a.col1, b.col2
-- MotherDuck table from a non-main schema
FROM "ddb$my_database$my_schema".my_table AS a
-- Local Postgres table in the public schema
LEFT JOIN public.another_table AS b on a.key = b.key
```

The DuckDB `iceberg_scan` function also works as well:

```sql
-- Use DuckDB's iceberg_scan function to query Iceberg tables
SELECT COUNT(*) 
FROM iceberg_scan('https://motherduck-demo.s3.amazonaws.com/iceberg/lineitem_iceberg', allow_moved_paths := true)
```
```sql
-- Use duckdb.query for SELECT queries that return tabular data
-- This example lists all databases in MotherDuck
SELECT * FROM duckdb.query('FROM md_databases()')
```
```sql
-- Use duckdb.raw_query for DDL queries that return void
-- This example drops a table in MotherDuck
SELECT * FROM duckdb.raw_query('DROP TABLE my_database.my_schema.some_table')
```

### Replicating data to MotherDuck

```sql
-- Create a table in MotherDuck and populate it with data from Postgres
-- Replace my_database and my_schema with your target database and schema names
CREATE TABLE "ddb$my_database$my_schema".my_table USING duckdb AS
SELECT * FROM public.my_table
```

The [pg\_duckdb github repo](https://github.com/duckdb/pg_duckdb) contains [further documentation](https://github.com/duckdb/pg_duckdb/blob/main/docs/README.md) of all available functions.

For ease of finding the documentation, a table of the documentation sections is below:

| Topic | Description |
| --- | --- |
| [**Functions**](https://github.com/duckdb/pg_duckdb/blob/main/docs/functions.md) | Complete reference for all available functions |
| [**Syntax Guide & Gotchas**](https://github.com/duckdb/pg_duckdb/blob/main/docs/gotchas_and_syntax.md) | Quick reference for common SQL patterns and things to know |
| [**Types**](https://github.com/duckdb/pg_duckdb/blob/main/docs/types.md) | Supported data types and type mappings |
| [**Extensions**](https://github.com/duckdb/pg_duckdb/blob/main/docs/extensions.md) | DuckDB extension installation and usage |
| [**Settings**](https://github.com/duckdb/pg_duckdb/blob/main/docs/settings.md) | Configuration options and parameters |
| [**Transactions**](https://github.com/duckdb/pg_duckdb/blob/main/docs/transactions.md) | Transaction behavior and limitations |

## Connecting with the Postgres Extension

You can also connect to PlanetScale Postgres with the DuckDB Postgres extension. This approach allows you to query PlanetScale data directly from DuckDB or MotherDuck.

### Install and Load the Extension

```sql
-- Install the Postgres extension from DuckDB's extension registry
INSTALL postgres;

-- Load the extension to enable Postgres connectivity
LOAD postgres;

-- Attach your PlanetScale database using a connection string
ATTACH '<connection string>' AS postgres_db (TYPE postgres);
```

### Connection String Format

The connection string format follows PostgreSQL's standard connection parameters. Here's an example with explanations:

```sql
ATTACH 'host=<host> port=<port> user=<user> password=<pw> dbname=<db> sslmode=require' 
    AS planetscale (TYPE postgres);
```

**Connection Parameters:**

- `host`: Your PlanetScale database hostname (found in your PlanetScale dashboard)
- `port`: The database port (typically 3306 for MySQL or 5432 for Postgres)
- `user`: Your PlanetScale database username
- `password`: Your PlanetScale database password
- `dbname`: The name of your database in PlanetScale
- `sslmode=require`: Ensures SSL encryption is used (required for PlanetScale)