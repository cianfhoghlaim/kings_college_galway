---
title: "Using MotherDuck with PlanetScale — PlanetScale"
source: "https://planetscale.com/blog/using-motherduck-with-planetscale"
author:
published: 2025-12-16
created: 2025-12-16
description: "Using MotherDuck with PlanetScale"
tags:
  - "clippings"
---
$50 Metal Postgres databases are here.[Learn more](https://planetscale.com/blog/50-dollar-planetscale-metal-is-ga-for-postgres)

[Blog](https://planetscale.com/blog) |

## Using MotherDuck with PlanetScale

By Ben Dicken |

DuckDB has gained significant traction for OLAP workloads.It's powerful, flexible, and has a feature-rich SQL dialect, making it perfect to use for analytics alongside OLTP-oriented relational databases.

Today, we're excited to announce support for the `pg_duckdb` extension for Postgres databases on PlanetScale alongside our partnership with MotherDuck.

## DuckDB in Postgres

DuckDB can be run as a standalone OLAP database, but also alongside Postgres via the [`pg_duckdb` extension](https://github.com/duckdb/pg_duckdb).The extension integrates DuckDB's column-store analytics engine right inside of Postgres, allowing you to seamlessly combine OLTP and OLAP queries over Postgres connections.

When enabled, tables can be created either using the standard Postgres table format *or* temporary tables in the DuckDB vectorized column format.Queries can then be selectively executed either using the Postgres engine or DuckDB.`pg_duckdb` can also be used to work with and query external datasources in popular formats like Apache Parquet and Iceberg.

Having DuckDB as a built-in extension makes data movement between Postgres and DuckDB formats simpler, and unifies the experience of combining analytics results with the rest of your relational data.

## MotherDuck

Though DuckDB is extremely powerful, many prefer to separate analytical compute from OLTP compute.This is useful to ensure that heavy analytics queries don't negatively impact application performance, and vice-versa.

MotherDuck is a cloud data warehouse with deep integration and support for DuckDB, and is a perfect solution to this problem.The `pg_duckdb` extension supports offloading analytics queries to the MotherDuck cloud.Analytics queries can be executed from within your PlanetScale Postgres database, but the analytics query execution can be offloaded to your data sets stored in the MotherDuck cloud.The results can then be returned to Postgres for further processing.

To use DuckDB and MotherDuck together with your PlanetScale database:

- Enable `pg_duckdb` via the "Extensions" table on the "Clusters" page of your database.

![Enable pg_duckdb](https://planetscale-images.imgix.net/assets/enable-duckdb-extension-DHkLGkML.png?auto=compress%2Cformat)

- Connect to your Postgres database and run `GRANT CREATE ON SCHEMA public to pscale_superuser;` to allow the addition of the MotherDuck catalog in Postgres and `CREATE EXTENSION pg_duckdb;` to create the extension.
- Add your MotherDuck token with `CALL duckdb.enable_motherduck('YOUR_TOKEN');`
- Start running your analytics queries!

Check out [our docs](https://planetscale.com/docs/postgres/extensions/pg_duckdb) and the [MotherDuck docs](https://motherduck.com/docs/concepts/pgduckdb/) for more information on how to use `pg_duckdb` with MotherDuck.