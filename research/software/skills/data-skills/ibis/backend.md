---
description: Connect to Ibis backends and select the right one for your use case.
---

# Ibis Backend Selection Assistant

Help users choose the right Ibis backend and configure connections properly.

## Reference

Use `@/ibis-llms.txt` for comprehensive backend documentation.

## Backend Selection Guide

### Quick Decision Tree

**Local Development / Testing**: DuckDB
**Data < 10GB**: DuckDB or Polars
**Data 10-100GB**: DuckDB
**Data > 100GB**: BigQuery, Snowflake, or Trino
**Existing PostgreSQL**: PostgreSQL
**Cloud Production**: BigQuery or Snowflake
**Distributed Processing**: PySpark or Trino

### Backend Comparison

| Backend | Best For | Limitations |
|---------|----------|-------------|
| **DuckDB** | Local dev, CI/CD, small-medium data | Single machine |
| **PostgreSQL** | Existing Postgres infrastructure | Limited analytics functions |
| **BigQuery** | Large-scale cloud analytics | GCP only, query costs |
| **Snowflake** | Enterprise data warehouse | Snowflake account required |
| **PySpark** | Distributed processing, existing Spark | Complex setup, overhead |
| **Polars** | Fast local processing | Some query limitations |

## Connection Patterns

### DuckDB (Recommended for Development)

```python
import ibis

# In-memory (fastest for testing)
con = ibis.duckdb.connect()

# File-based (persistent)
con = ibis.duckdb.connect("mydb.duckdb")

# With configuration
con = ibis.duckdb.connect(
    threads=4,
    memory_limit="8GB"
)

# Read files directly
t = con.read_parquet("data/*.parquet")
t = con.read_csv("data.csv")

# MotherDuck (cloud DuckDB)
con = ibis.duckdb.connect("md:")
```

### PostgreSQL

```python
import ibis

# Using parameters
con = ibis.postgres.connect(
    host="localhost",
    port=5432,
    user="username",
    password="password",
    database="mydb"
)

# Using URL
con = ibis.connect("postgresql://user:password@host:5432/database")

# With SSL
con = ibis.postgres.connect(
    host="host",
    database="db",
    user="user",
    password="pass",
    sslmode="require"
)
```

### MySQL

```python
import ibis

con = ibis.mysql.connect(
    host="localhost",
    port=3306,
    user="username",
    password="password",
    database="mydb"
)

# URL format
con = ibis.connect("mysql://user:password@host:3306/database")
```

### SQLite

```python
import ibis

# In-memory
con = ibis.sqlite.connect()

# File-based
con = ibis.sqlite.connect("mydb.sqlite")

# URL format
con = ibis.connect("sqlite:///path/to/database.db")
```

### BigQuery

```python
import ibis

# Basic (uses gcloud CLI auth)
con = ibis.bigquery.connect(
    project_id="my-project",
    dataset_id="my_dataset"
)

# URL format
con = ibis.connect("bigquery://project-id/dataset-id")

# With location
con = ibis.bigquery.connect(
    project_id="my-project",
    dataset_id="my_dataset",
    location="US"  # or "EU"
)

# With explicit credentials
from google.oauth2 import service_account

credentials = service_account.Credentials.from_service_account_file(
    "service-account.json"
)

con = ibis.bigquery.connect(
    project_id="my-project",
    dataset_id="my_dataset",
    credentials=credentials
)
```

### Snowflake

```python
import ibis

# Basic connection
con = ibis.snowflake.connect(
    user="username",
    password="password",
    account="org-account",  # e.g., "abc12345.us-east-1"
    database="MY_DATABASE",
    schema="MY_SCHEMA"
)

# With warehouse
con = ibis.snowflake.connect(
    user="username",
    password="password",
    account="org-account",
    database="MY_DATABASE",
    schema="MY_SCHEMA",
    warehouse="MY_WAREHOUSE"
)

# SSO authentication
con = ibis.snowflake.connect(
    user="username",
    account="org-account",
    database="MY_DATABASE",
    authenticator="externalbrowser"
)

# Key pair authentication
con = ibis.snowflake.connect(
    user="username",
    account="org-account",
    database="MY_DATABASE",
    private_key=private_key_bytes
)
```

### PySpark

```python
import ibis
from pyspark.sql import SparkSession

# From existing session
session = SparkSession.builder.getOrCreate()
con = ibis.pyspark.connect(session)

# With custom config
session = (
    SparkSession.builder
    .appName("MyApp")
    .config("spark.executor.memory", "4g")
    .getOrCreate()
)
con = ibis.pyspark.connect(session)
```

### Polars

```python
import ibis
import polars as pl

# Basic connection
con = ibis.polars.connect()

# With initial tables
con = ibis.polars.connect(tables={
    "my_table": pl.LazyFrame({"a": [1, 2, 3]})
})
```

### DataFusion

```python
import ibis

con = ibis.datafusion.connect()

# Note: DataFusion does NOT support memtable()
```

### Trino

```python
import ibis

con = ibis.trino.connect(
    user="username",
    host="trino-host",
    port=8080,
    database="catalog",  # Catalog name
    schema="schema"
)

# With HTTPS
con = ibis.trino.connect(
    user="username",
    auth="password",
    host="trino-host",
    port=443,
    database="catalog",
    schema="schema",
    http_scheme="https"
)
```

## Development to Production Pattern

The key advantage of Ibis is writing code once and running it anywhere:

```python
import ibis

def get_connection(env: str):
    """Get connection based on environment."""
    if env == "dev":
        return ibis.duckdb.connect("dev_data.duckdb")
    elif env == "staging":
        return ibis.postgres.connect(...)
    elif env == "prod":
        return ibis.bigquery.connect(
            project_id="prod-project",
            dataset_id="analytics"
        )

def analysis(con):
    """Same analysis code works on all backends."""
    t = con.table("sales")
    return (
        t
        .filter(_.date >= "2024-01-01")
        .group_by("region")
        .aggregate(total=_.amount.sum())
    )

# Development
con = get_connection("dev")
result = analysis(con).to_pandas()

# Production
con = get_connection("prod")
result = analysis(con).to_pandas()
```

## Common Connection Operations

```python
# List tables
tables = con.list_tables()

# Access a table
t = con.table("my_table")

# Create a table
con.create_table("new_table", df)
con.create_table("new_table", ibis_expr)

# Execute raw SQL
result = con.sql("SELECT * FROM my_table LIMIT 10")

# Check if operation is supported
ibis.duckdb.has_operation(ibis.ops.GeoDistance)
```

## Best Practices

1. **Start with DuckDB** for fast iteration
2. **Use environment variables** for credentials
3. **Test locally, deploy remotely** - same code works
4. **Check operation support** before using advanced features
5. **Use connection pooling** in production (backend-specific)

## Troubleshooting

### Connection Issues
- Check credentials and network access
- Verify correct driver is installed (`pip install 'ibis-framework[backend]'`)
- For BigQuery: ensure `gcloud` CLI is authenticated

### Operation Not Supported
```python
# Check if operation exists
if con.has_operation(ibis.ops.ArrayConcat):
    # Use array concat
    pass
else:
    # Alternative approach
    pass
```

### Performance Issues
- DuckDB: Increase `memory_limit` and `threads`
- BigQuery: Check query costs and use partitioning
- PostgreSQL: Ensure proper indexes exist

Now, which backend would you like to connect to?
