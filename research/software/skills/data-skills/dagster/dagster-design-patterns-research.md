# Dagster Design Patterns and Best Practices - Research Findings

**Last Updated:** 2025-11-18

This document contains comprehensive research on Dagster design patterns, best practices, and real-world implementation examples gathered from official documentation, community discussions, and production case studies.

---

## Table of Contents

1. [Design Patterns](#design-patterns)
   - [Asset-based vs Op-based Patterns](#asset-based-vs-op-based-patterns)
   - [Factory Patterns for Assets](#factory-patterns-for-assets)
   - [Resource Management Patterns](#resource-management-patterns)
   - [Configuration Patterns](#configuration-patterns)
   - [Testing Patterns](#testing-patterns)
   - [Error Handling and Retry Strategies](#error-handling-and-retry-strategies)
   - [Dependency Management Patterns](#dependency-management-patterns)
2. [Best Practices](#best-practices)
   - [Code Organization and Project Structure](#code-organization-and-project-structure)
   - [Asset Naming Conventions](#asset-naming-conventions)
   - [Performance Optimization](#performance-optimization)
   - [Observability and Logging](#observability-and-logging)
   - [CI/CD Integration](#cicd-integration)
   - [Multi-Environment Setups](#multi-environment-setups)
3. [Common Use Cases](#common-use-cases)
   - [ETL/ELT Workflows](#etlelt-workflows)
   - [ML Pipelines](#ml-pipelines)
   - [dbt Integration](#dbt-integration)
   - [Data Quality Checks](#data-quality-checks)
   - [Incremental Processing and Partitioning](#incremental-processing-and-partitioning)
   - [Cross-Team Collaboration](#cross-team-collaboration)
4. [Production Case Studies](#production-case-studies)
5. [Anti-Patterns to Avoid](#anti-patterns-to-avoid)

---

## Design Patterns

### Asset-based vs Op-based Patterns

#### Current Recommendation (2024-2025)

**Assets are the primary design pattern** in modern Dagster development. Ops are now primarily for legacy support or specialized edge cases.

#### When to Use Assets

- **Primary Use Case:** Building data pipelines where the goal is to produce and manage data artifacts
- **Best For:** New Dagster projects and teams
- **Represents:** Logical units of data (tables, datasets, ML models)
- **Benefits:**
  - Natural fit for data pipelines
  - Automatic data lineage tracking
  - Built-in observability
  - Declarative approach

**Example:**
```python
from dagster import asset

@asset
def customers_table(context):
    """Represents a table of customer data"""
    # Extract, transform, load logic
    return df

@asset(deps=[customers_table])
def customers_analytics(context):
    """Analytics derived from customers"""
    # Depends on customers_table
    return analytics_df
```

#### When to Use Ops

- **Edge Cases Only:** Workflows not centered around producing persistent data
- **Examples:**
  - Sending emails or notifications
  - Setting up database permissions
  - Correcting known data mistakes
  - Triggering webhooks
  - Scanning for unused tables to delete

**Example:**
```python
from dagster import op, job

@op
def send_notification(context):
    """Sends email - no persistent artifact created"""
    send_email(context.op_config["recipient"])

@job
def notification_job():
    send_notification()
```

#### Key Takeaway

> If your workflow produces a persistent data product, use **assets**. If it performs an action without creating a persistent artifact, consider **ops**.

---

### Factory Patterns for Assets

Asset factories are a powerful pattern for reducing code duplication and creating multiple similar assets from configuration.

#### Use Cases

1. **Repetitive ETL tasks** (e.g., processing multiple CSV files with same logic)
2. **Multi-source ingestion** (e.g., same API pattern for different endpoints)
3. **Per-client/tenant asset generation**
4. **Standardized transformations** across multiple data sources

#### Basic Factory Pattern

```python
from dagster import asset, AssetExecutionContext

def create_s3_csv_asset(asset_name: str, s3_key: str):
    """Factory that creates an asset for each CSV file"""

    @asset(name=asset_name)
    def _asset(context: AssetExecutionContext):
        # Download CSV from S3
        df = download_csv_from_s3(s3_key)
        # Transform
        transformed = run_sql_query(df)
        # Upload back to S3
        upload_to_s3(transformed, f"processed/{asset_name}.csv")
        return transformed

    return _asset

# Create multiple assets from factory
raw_data_asset = create_s3_csv_asset("raw_sales", "input/sales.csv")
raw_users_asset = create_s3_csv_asset("raw_users", "input/users.csv")
```

#### Advanced Factory with Dependencies

```python
from dagster import AssetIn, asset

def create_analytics_asset(source_name: str, upstream_asset_name: str):
    """Create analytics asset with dependency"""

    @asset(
        name=f"{source_name}_analytics",
        ins={"source_data": AssetIn(upstream_asset_name)}
    )
    def _analytics_asset(context, source_data):
        # Perform analytics on source data
        return compute_analytics(source_data)

    return _analytics_asset
```

#### Factory with Custom Decorator (DRY Pattern)

```python
import functools
from dagster import asset

def monitoring_asset(func):
    """Custom decorator that wraps asset with monitoring logic"""
    @functools.wraps(func)
    def wrapper(context, *args, **kwargs):
        context.log.info(f"Starting {func.__name__}")
        start_time = time.time()

        try:
            result = func(context, *args, **kwargs)
            duration = time.time() - start_time
            context.log_event(
                AssetMaterialization(
                    asset_key=func.__name__,
                    metadata={"duration": duration}
                )
            )
            return result
        except Exception as e:
            context.log.error(f"Failed: {str(e)}")
            raise

    return asset(wrapper)

@monitoring_asset
def my_asset(context):
    # Your logic here
    pass
```

#### Best Practices

- ✅ Use factories to keep code DRY
- ✅ Provide clear asset names via factory parameters
- ✅ Document what each factory creates
- ✅ Consider using components for complex factory patterns (new Dagster feature)
- ⚠️ Avoid creating too many assets from a single factory (limit: ~100)

---

### Resource Management Patterns

Resources provide dependency injection, abstracting external services and configuration.

#### ConfigurableResource Pattern (Modern Approach)

```python
from dagster import ConfigurableResource, asset
from pydantic import Field

class DatabaseResource(ConfigurableResource):
    """Database connection resource"""
    host: str
    port: int = Field(default=5432)
    database: str
    username: str
    password: str

    def get_connection(self):
        return psycopg2.connect(
            host=self.host,
            port=self.port,
            database=self.database,
            user=self.username,
            password=self.password
        )

    def execute_query(self, query: str):
        with self.get_connection() as conn:
            return pd.read_sql(query, conn)

# Using the resource in an asset
@asset
def user_data(database: DatabaseResource):
    """Asset using database resource"""
    return database.execute_query("SELECT * FROM users")
```

#### Resource Dependencies (Nested Resources)

```python
from dagster import ConfigurableResource

class AwsCredentials(ConfigurableResource):
    """Shared AWS credentials"""
    access_key: str
    secret_key: str
    region: str = "us-east-1"

class S3Resource(ConfigurableResource):
    """S3 resource depending on credentials"""
    credentials: AwsCredentials
    bucket: str

    def upload(self, key: str, data: bytes):
        client = boto3.client(
            's3',
            aws_access_key_id=self.credentials.access_key,
            aws_secret_access_key=self.credentials.secret_key,
            region_name=self.credentials.region
        )
        client.put_object(Bucket=self.bucket, Key=key, Body=data)

class RedshiftResource(ConfigurableResource):
    """Redshift also using same credentials"""
    credentials: AwsCredentials
    cluster: str

    def query(self, sql: str):
        # Use shared credentials
        pass

# In Definitions
from dagster import Definitions

defs = Definitions(
    assets=[user_data],
    resources={
        "aws_creds": AwsCredentials(
            access_key=EnvVar("AWS_ACCESS_KEY"),
            secret_key=EnvVar("AWS_SECRET_KEY")
        ),
        "s3": S3Resource(
            credentials=AwsCredentials(...),
            bucket="my-bucket"
        ),
        "redshift": RedshiftResource(
            credentials=AwsCredentials(...),
            cluster="my-cluster"
        )
    }
)
```

#### Environment-Specific Resources

```python
from dagster import EnvVar

# Using EnvVar for runtime evaluation
class ApiResource(ConfigurableResource):
    api_key: str
    base_url: str

# In Definitions
resources_by_env = {
    "dev": {
        "api": ApiResource(
            api_key=EnvVar("DEV_API_KEY"),
            base_url="https://dev-api.example.com"
        )
    },
    "prod": {
        "api": ApiResource(
            api_key=EnvVar("PROD_API_KEY"),
            base_url="https://api.example.com"
        )
    }
}

# Select based on environment
import os
env = os.getenv("DAGSTER_DEPLOYMENT", "dev")
selected_resources = resources_by_env[env]
```

#### Launch-Time Configuration

```python
from dagster import ConfigurableResource

class DynamicResource(ConfigurableResource):
    """Resource configured at launch time"""
    endpoint: str
    timeout: int = 30

# Mark for launch-time configuration
@asset
def my_asset(dynamic: DynamicResource):
    # Resource will be configured when run is launched
    pass

# In Definitions
defs = Definitions(
    assets=[my_asset],
    resources={
        "dynamic": DynamicResource.configure_at_launch()
    }
)
```

#### Best Practices

- ✅ Use `ConfigurableResource` for type-safe, validated resources
- ✅ Use `EnvVar` for secrets (evaluated at runtime, not visible in UI)
- ✅ Use nested resources for shared configuration (e.g., cloud credentials)
- ✅ Keep resources focused on external systems (databases, APIs, cloud services)
- ✅ Mock resources in tests using direct instantiation
- ⚠️ Avoid loading heavy clients at module level (see Anti-Patterns)

---

### Configuration Patterns

#### Multi-Environment Configuration

```python
from dagster import Definitions, EnvVar
import os

def get_resources_for_deployment():
    """Select resources based on deployment environment"""
    deployment = os.getenv("DAGSTER_DEPLOYMENT", "local")

    if deployment == "production":
        return {
            "warehouse": SnowflakeResource(
                account=EnvVar("SNOWFLAKE_ACCOUNT"),
                user=EnvVar("SNOWFLAKE_USER"),
                password=EnvVar("SNOWFLAKE_PASSWORD"),
                database="PROD_DB"
            )
        }
    elif deployment == "staging":
        return {
            "warehouse": SnowflakeResource(
                account=EnvVar("SNOWFLAKE_ACCOUNT"),
                user=EnvVar("SNOWFLAKE_USER"),
                password=EnvVar("SNOWFLAKE_PASSWORD"),
                database="STAGING_DB"
            )
        }
    else:  # local
        return {
            "warehouse": MockWarehouse()
        }

defs = Definitions(
    assets=all_assets,
    resources=get_resources_for_deployment()
)
```

#### Using YAML/Config Files

```python
import yaml
from pathlib import Path

def load_config(env: str):
    """Load configuration from YAML files"""
    config_path = Path(__file__).parent / "config" / f"{env}.yaml"
    with open(config_path) as f:
        return yaml.safe_load(f)

# config/prod.yaml
# database:
#   host: prod-db.example.com
#   port: 5432
# feature_flags:
#   enable_ml_pipeline: true

config = load_config(os.getenv("DAGSTER_DEPLOYMENT", "local"))

defs = Definitions(
    assets=all_assets,
    resources={
        "db": DatabaseResource(**config["database"])
    }
)
```

---

### Testing Patterns

#### Unit Testing Assets

```python
from dagster import materialize, AssetExecutionContext

def test_user_data_asset():
    """Test individual asset"""
    # Create mock resource
    mock_db = DatabaseResource(
        host="localhost",
        database="test_db",
        username="test",
        password="test"
    )

    # Materialize the asset with mock resource
    result = materialize(
        [user_data],
        resources={"database": mock_db}
    )

    # Assert success
    assert result.success

    # Get materialized data
    output = result.output_for_node("user_data")
    assert len(output) > 0
    assert "user_id" in output.columns
```

#### Testing Assets with Dependencies

```python
def test_downstream_asset():
    """Test multiple assets together"""
    mock_db = DatabaseResource(...)

    # Materialize both upstream and downstream
    result = materialize(
        [user_data, user_analytics],
        resources={"database": mock_db}
    )

    assert result.success
    analytics = result.output_for_node("user_analytics")
    assert analytics["avg_age"] > 0
```

#### Integration Testing

```python
from dagster import build_op_context

def test_asset_with_real_database():
    """Integration test with real external service"""
    # Use actual test database
    test_db = DatabaseResource(
        host=os.getenv("TEST_DB_HOST"),
        database="test_dagster",
        username=os.getenv("TEST_DB_USER"),
        password=os.getenv("TEST_DB_PASSWORD")
    )

    result = materialize(
        [user_data],
        resources={"database": test_db}
    )

    assert result.success
```

#### Testing with Fixtures (pytest)

```python
import pytest
from dagster import materialize

@pytest.fixture
def mock_database():
    """Reusable mock database fixture"""
    return DatabaseResource(
        host="localhost",
        database="test",
        username="test",
        password="test"
    )

@pytest.fixture
def mock_s3():
    """Mock S3 resource"""
    return MockS3Resource(bucket="test-bucket")

def test_etl_pipeline(mock_database, mock_s3):
    """Test full pipeline with fixtures"""
    result = materialize(
        all_assets,
        resources={
            "database": mock_database,
            "s3": mock_s3
        }
    )

    assert result.success
```

#### Best Practices

- ✅ Test individual assets as Python functions when possible
- ✅ Use `materialize()` to test assets with resources
- ✅ Mock external services to avoid hitting real APIs/databases in unit tests
- ✅ Create integration tests for critical paths using test environments
- ✅ Use pytest fixtures for reusable test resources
- ✅ Test asset logic in development, not after expensive batch runs in staging
- ⚠️ Don't test entire jobs; focus on individual assets or small groups

---

### Error Handling and Retry Strategies

#### Op-Level Retry Policies

```python
from dagster import asset, RetryPolicy, Backoff, Jitter

# Basic retry policy
@asset(
    retry_policy=RetryPolicy(
        max_retries=3,
        delay=1,  # seconds between retries
    )
)
def flaky_api_call(context):
    """Automatically retries on failure"""
    return call_external_api()

# Advanced retry with exponential backoff
@asset(
    retry_policy=RetryPolicy(
        max_retries=5,
        delay=2,
        backoff=Backoff.EXPONENTIAL,  # 2s, 4s, 8s, 16s, 32s
        jitter=Jitter.PLUS_MINUS  # Add randomness to avoid thundering herd
    )
)
def cloud_service_call(context):
    """Handles transient cloud service failures"""
    return upload_to_cloud_service()
```

#### Manual Retry Control

```python
from dagster import asset, RetryRequested

@asset
def conditional_retry_asset(context):
    """Custom retry logic based on error type"""
    try:
        return process_data()
    except TransientNetworkError as e:
        # Retry on transient errors
        context.log.warning(f"Transient error: {e}, retrying...")
        raise RetryRequested(max_retries=3, seconds_to_wait=5)
    except PermanentError as e:
        # Don't retry on permanent errors
        context.log.error(f"Permanent error: {e}")
        raise
```

#### Failure Exceptions with Metadata

```python
from dagster import asset, Failure, MetadataValue

@asset
def data_validation_asset(context):
    """Asset that can fail with structured metadata"""
    data = load_data()

    if data.null_count() > 1000:
        raise Failure(
            description="Too many null values detected",
            metadata={
                "null_count": MetadataValue.int(data.null_count()),
                "total_rows": MetadataValue.int(len(data)),
                "threshold": MetadataValue.int(1000),
                "sample_nulls": MetadataValue.json(
                    data[data.isnull()].head().to_dict()
                )
            },
            allow_retries=False  # Don't retry, this is a data quality issue
        )

    return data
```

#### Run-Level Retries

```python
from dagster import job, asset, RunRequest

# Configure run retries via tags
@asset(
    tags={
        "dagster/retry_strategy": "FROM_FAILURE",  # Default
        # or "ALL_STEPS" to re-run everything
    }
)
def my_asset(context):
    return process()

# In schedules/sensors
@schedule(...)
def my_schedule(context):
    return RunRequest(
        tags={
            "dagster/max_retries": "3",
            "dagster/retry_strategy": "FROM_FAILURE"
        }
    )
```

#### Best Practices

- ✅ Use `RetryPolicy` for transient failures (network, cloud services, timeouts)
- ✅ Use exponential backoff for external APIs to avoid overwhelming them
- ✅ Add jitter to prevent thundering herd problems
- ✅ Use `Failure` with `allow_retries=False` for data quality issues
- ✅ Include rich metadata in failures for debugging
- ✅ Use manual `RetryRequested` for conditional retry logic
- ⚠️ Don't retry framework errors (Dagster internal errors)
- ⚠️ Be mindful of retry limits on billable cloud services

---

### Dependency Management Patterns

#### Basic Asset Dependencies

```python
from dagster import asset

# Implicit dependency via function argument
@asset
def upstream_data(context):
    return fetch_raw_data()

@asset
def downstream_analytics(context, upstream_data):
    """Depends on upstream_data (argument name matches asset name)"""
    return compute_analytics(upstream_data)
```

#### Explicit Dependencies with `deps`

```python
from dagster import asset

@asset(deps=[upstream_data])
def side_effect_asset(context):
    """Depends on upstream_data but doesn't need the data"""
    # Just needs upstream to run first
    send_notification("upstream_data is ready")
```

#### AssetIn for Custom Configuration

```python
from dagster import asset, AssetIn

@asset(
    ins={
        "raw_data": AssetIn(
            key="upstream_data",  # Different name than parameter
            metadata={"schema": "raw"},
        )
    }
)
def transformed_data(context, raw_data):
    """Custom dependency configuration"""
    return transform(raw_data)
```

#### Partitioned Asset Dependencies

```python
from dagster import asset, DailyPartitionsDefinition, AssetIn

daily_partitions = DailyPartitionsDefinition(start_date="2024-01-01")

@asset(partitions_def=daily_partitions)
def daily_sales(context):
    """Partitioned by day"""
    date = context.partition_key
    return fetch_sales_for_date(date)

@asset(
    partitions_def=daily_partitions,
    ins={
        "daily_sales": AssetIn(
            partition_mapping=TimeWindowPartitionMapping()
        )
    }
)
def weekly_sales_report(context, daily_sales):
    """Aggregates daily sales into weekly reports"""
    return aggregate_to_weekly(daily_sales)
```

#### Cross-Asset Dependencies

```python
from dagster import asset, AssetIn

# Asset from another team/project
@asset
def external_customer_data(context):
    """Managed by Team A"""
    return load_from_warehouse("team_a.customers")

# Your asset depending on external asset
@asset(deps=["external_customer_data"])
def customer_analytics(context):
    """Managed by Team B, depends on Team A's asset"""
    return compute_analytics()
```

#### Multi-Asset Dependencies

```python
from dagster import multi_asset, AssetOut

@multi_asset(
    outs={
        "customers": AssetOut(),
        "orders": AssetOut(),
    }
)
def extract_from_database(context):
    """Single asset definition creating multiple assets"""
    customers_df = extract_customers()
    orders_df = extract_orders()
    return customers_df, orders_df

# Depending on outputs of multi-asset
@asset
def customer_orders_joined(context, customers, orders):
    """Depends on both outputs of the multi-asset"""
    return customers.join(orders, on="customer_id")
```

#### Best Practices

- ✅ Use function arguments for dependencies when you need the data
- ✅ Use `deps` parameter when you only need ordering, not data
- ✅ Use `AssetIn` for custom partition mappings or metadata
- ✅ Keep dependency chains understandable (avoid deeply nested dependencies)
- ✅ Document cross-team dependencies clearly
- ⚠️ Avoid circular dependencies (Dagster will error)

---

## Best Practices

### Code Organization and Project Structure

#### Default Scaffold Structure

```
my_dagster_project/
├── pyproject.toml
├── setup.py
├── my_project/
│   ├── __init__.py
│   ├── definitions.py      # Main Definitions object
│   └── assets/             # All asset definitions
│       ├── __init__.py
│       └── core_assets.py
└── tests/
    └── test_assets.py
```

#### Organization by Technology (Recommended for Technical Teams)

```
my_dagster_project/
├── my_project/
│   ├── definitions.py
│   ├── assets/
│   │   ├── postgres/          # PostgreSQL assets
│   │   │   ├── raw_tables.py
│   │   │   └── transformed.py
│   │   ├── s3/                # S3 assets
│   │   │   └── data_lake.py
│   │   ├── snowflake/         # Snowflake assets
│   │   │   └── warehouse.py
│   │   └── apis/              # External API assets
│   │       ├── salesforce.py
│   │       └── stripe.py
│   ├── resources/
│   │   ├── postgres.py
│   │   ├── s3.py
│   │   └── snowflake.py
│   ├── schedules/
│   │   └── daily_jobs.py
│   └── sensors/
│       └── file_sensors.py
```

#### Organization by Business Domain (Recommended for Data Products)

```
my_dagster_project/
├── my_project/
│   ├── definitions.py
│   ├── assets/
│   │   ├── raw_data/          # Landing zone
│   │   │   ├── customer_raw.py
│   │   │   └── sales_raw.py
│   │   ├── cleaned/           # Cleaned/standardized
│   │   │   └── customer_cleaned.py
│   │   ├── analytical/        # Business logic applied
│   │   │   ├── customer_360.py
│   │   │   └── sales_analytics.py
│   │   ├── bi_reports/        # BI tool datasets
│   │   │   └── executive_dashboard.py
│   │   └── ml_models/         # ML features/models
│   │       ├── churn_features.py
│   │       └── churn_model.py
│   └── resources/
│       └── shared_resources.py
```

#### Organization by Data Processing Concept

```
my_dagster_project/
├── my_project/
│   ├── assets/
│   │   ├── ingestion/         # Data ingestion
│   │   │   ├── api_ingestion.py
│   │   │   └── file_ingestion.py
│   │   ├── transformation/    # Data transformation
│   │   │   ├── bronze_to_silver.py
│   │   │   └── silver_to_gold.py
│   │   └── serving/           # Data serving
│   │       ├── analytics_marts.py
│   │       └── ml_features.py
```

#### Evolution Strategy

**Phase 1: Everything in One File (0-400 lines)**
```python
# definitions.py
from dagster import asset, Definitions

@asset
def asset1(): ...

@asset
def asset2(): ...

defs = Definitions(
    assets=[asset1, asset2],
    resources={...},
    schedules=[...]
)
```

**Phase 2: Separate Concerns (400-2000 lines)**
```python
# assets/__init__.py
from .raw_data import raw_assets
from .analytics import analytics_assets

all_assets = [*raw_assets, *analytics_assets]

# definitions.py
from .assets import all_assets
from .resources import all_resources
from .schedules import all_schedules

defs = Definitions(
    assets=all_assets,
    resources=all_resources,
    schedules=all_schedules
)
```

**Phase 3: Domain/Technology Grouping (2000+ lines, multiple teams)**
```python
# Organize by domain as shown in previous examples
```

#### Integration Projects (dbt, Sling, etc.)

```
project_root/
├── dagster_project/           # Dagster orchestration code
│   └── my_project/
│       ├── definitions.py
│       └── assets/
│           └── dbt_assets.py  # References dbt project
├── dbt_project/               # Separate dbt project
│   ├── dbt_project.yml
│   ├── models/
│   └── tests/
└── sling_project/             # Separate Sling project
    └── replication.yaml
```

#### Best Practices

- ✅ Start simple (one file) and evolve as needed
- ✅ Organize by business domain if stakeholders think in terms of data products
- ✅ Organize by technology if engineers need to find tech-specific code quickly
- ✅ Keep related assets together in the same module
- ✅ Use `__init__.py` to export public APIs
- ✅ Store integration projects (dbt, Jupyter) outside Dagster project
- ✅ Mirror your organization's language in your structure
- ⚠️ Don't over-engineer structure for small projects

---

### Asset Naming Conventions

#### General Naming Guidelines

```python
from dagster import asset

# ✅ Good: Descriptive, noun-based, lowercase with underscores
@asset
def daily_active_users(context):
    """Clear what this represents"""
    pass

@asset
def customer_churn_predictions(context):
    """Describes the data product"""
    pass

# ❌ Bad: Verb-based, unclear, overly technical
@asset
def process_data(context):  # What data?
    pass

@asset
def etl_job_3(context):  # Meaningless name
    pass
```

#### Naming by Layer (Medallion Architecture)

```python
# Bronze layer (raw data)
@asset
def bronze_customers_raw(context):
    """Raw customer data from source system"""
    pass

# Silver layer (cleaned, conformed)
@asset
def silver_customers_cleaned(context):
    """Cleaned and validated customer data"""
    pass

# Gold layer (business-ready)
@asset
def gold_customer_360_view(context):
    """Complete customer 360 view for analytics"""
    pass
```

#### Naming with Source Prefix

```python
# Indicate source system
@asset
def salesforce_accounts(context):
    pass

@asset
def stripe_payments(context):
    pass

@asset
def postgres_users(context):
    pass
```

#### Naming for Derived Assets

```python
@asset
def customers(context):
    """Base customer data"""
    pass

@asset
def customers_with_orders(context, customers):
    """Customers enriched with order data"""
    pass

@asset
def high_value_customers(context, customers_with_orders):
    """Customers with >$1000 lifetime value"""
    pass
```

#### Asset Keys and Groups

```python
from dagster import asset, AssetKey

# Using groups to organize related assets
@asset(group_name="customer_data")
def customers(context):
    pass

@asset(group_name="customer_data")
def customer_orders(context):
    pass

@asset(group_name="analytics")
def customer_analytics(context):
    pass

# Custom asset keys for complex naming
@asset(key=AssetKey(["warehouse", "prod", "customers"]))
def customers_asset(context):
    """Asset with multi-part key: warehouse/prod/customers"""
    pass
```

#### Best Practices

- ✅ Use descriptive, noun-based names
- ✅ Use lowercase with underscores (Python convention)
- ✅ Include source/layer information when helpful
- ✅ Be consistent within your organization
- ✅ Use groups to organize related assets
- ✅ Name assets after the data they represent, not the process
- ⚠️ Avoid overly long names (>50 characters)
- ❌ Don't use verb phrases like "get_data" or "process_orders"

---

### Performance Optimization

#### Resource Allocation (Dagster+ Hybrid)

**Agent Container:**
- Start: 0.25 vCPU core, 1 GB RAM
- Scale with: Number of concurrent runs, number of code locations

**Code Server Container:**
- Start: 0.25 vCPU cores, 1GB RAM
- Scale with: Import size, definition graph size, heavy initialization

**Run Containers (K8s/ECS):**
- Standard: 4 vCPU cores, 8-16 GB RAM
- Compute-heavy: Scale CPU and memory in run workers, not code servers

#### Limiting Concurrency

```python
from dagster import define_asset_job, Definitions
from dagster import RunRequest, sensor

# Limit concurrent runs in job config
@sensor(...)
def my_sensor(context):
    return RunRequest(
        run_key=f"run_{timestamp}",
        tags={
            "dagster/concurrency_key": "my_pipeline",
            "dagster/max_concurrent": "3"  # Only 3 runs at once
        }
    )
```

#### Optimizing Asset Execution

```python
from dagster import asset, OpExecutionContext

@asset
def optimized_asset(context: OpExecutionContext):
    """Asset with performance optimizations"""

    # 1. Use generators for large datasets
    def process_in_batches():
        for batch in fetch_data_in_batches(batch_size=10000):
            yield process_batch(batch)

    # 2. Log progress for long-running operations
    total_records = 1000000
    for i, result in enumerate(process_in_batches()):
        if i % 100 == 0:
            context.log.info(f"Processed {i*10000}/{total_records} records")

    # 3. Return metadata for observability
    context.add_output_metadata({
        "num_records": total_records,
        "execution_time_seconds": 123.45
    })
```

#### Partition Optimization

```python
from dagster import asset, DailyPartitionsDefinition

# ✅ Good: Reasonable partition count
daily_partitions = DailyPartitionsDefinition(
    start_date="2024-01-01",
    end_offset=0  # Don't partition future dates
)

@asset(partitions_def=daily_partitions)
def daily_data(context):
    """Recommended: <100,000 partitions"""
    date = context.partition_key
    return fetch_data_for_date(date)

# ⚠️ Warning: Too many partitions
hourly_partitions = HourlyPartitionsDefinition(
    start_date="2020-01-01"  # Creates 40,000+ partitions
)
```

#### Dynamic Output Optimization

```python
from dagster import asset, Output

@asset
def efficient_io_manager_asset(context):
    """Efficient use of I/O managers"""

    large_dataframe = process_data()

    # Write in chunks to S3 instead of single large write
    return Output(
        large_dataframe,
        metadata={
            "row_count": len(large_dataframe),
            "partition_strategy": "chunked"
        }
    )
```

#### Best Practices

- ✅ Start with recommended resource allocations and scale based on metrics
- ✅ Limit partition counts to <100,000 per asset
- ✅ Use concurrency limits to prevent overwhelming external systems
- ✅ Process large datasets in batches/chunks
- ✅ Log progress for long-running operations
- ✅ Use Dagster Insights to identify bottlenecks
- ⚠️ Monitor memory usage in code server containers
- ⚠️ Don't load heavy libraries at module level (see Anti-Patterns)

---

### Observability and Logging

#### Structured Logging

```python
from dagster import asset, OpExecutionContext

@asset
def observable_asset(context: OpExecutionContext):
    """Asset with comprehensive logging"""

    # Different log levels
    context.log.debug("Starting data fetch")
    context.log.info("Processing 10,000 records")
    context.log.warning("Found 5 invalid records, skipping")
    context.log.error("Failed to connect to API, retrying")

    # Structured logging with metadata
    context.log.info(
        "Data processing complete",
        extra={
            "records_processed": 10000,
            "invalid_records": 5,
            "processing_time_ms": 1234
        }
    )

    return data
```

#### Asset Materialization Metadata

```python
from dagster import asset, MetadataValue, OpExecutionContext

@asset
def asset_with_metadata(context: OpExecutionContext):
    """Asset that logs rich metadata"""

    result_df = process_data()

    # Add metadata to materialization
    context.add_output_metadata({
        # Scalar values
        "num_rows": len(result_df),
        "num_columns": len(result_df.columns),

        # Statistical metadata
        "mean_age": MetadataValue.float(result_df["age"].mean()),
        "null_count": MetadataValue.int(result_df.isnull().sum().sum()),

        # URLs and paths
        "data_location": MetadataValue.url("s3://bucket/data.parquet"),

        # JSON metadata
        "column_stats": MetadataValue.json({
            col: {
                "mean": result_df[col].mean(),
                "std": result_df[col].std()
            }
            for col in result_df.select_dtypes(include=['number']).columns
        }),

        # Markdown reports
        "quality_report": MetadataValue.md(f"""
        ## Data Quality Report
        - Total Rows: {len(result_df)}
        - Null Values: {result_df.isnull().sum().sum()}
        - Duplicate Rows: {result_df.duplicated().sum()}
        """)
    })

    return result_df
```

#### Asset Observations (for External Assets)

```python
from dagster import asset, AssetObservation, MetadataValue

@asset
def observed_external_asset(context):
    """Observe external asset without materializing"""

    # Check external data source
    s3_data = check_s3_bucket("my-bucket/data/")

    # Log observation
    context.log_event(
        AssetObservation(
            asset_key="external_s3_data",
            metadata={
                "file_count": MetadataValue.int(len(s3_data)),
                "total_size_mb": MetadataValue.float(
                    sum(f.size for f in s3_data) / 1024 / 1024
                ),
                "last_modified": MetadataValue.timestamp(
                    max(f.last_modified for f in s3_data)
                )
            }
        )
    )
```

#### Integration with CloudWatch

```python
from dagster_aws.cloudwatch import cloudwatch_logger
from dagster import Definitions

# Send logs to CloudWatch
defs = Definitions(
    assets=all_assets,
    resources={
        "cloudwatch": cloudwatch_logger
    }
)
```

#### Custom Metrics and Monitoring

```python
from dagster import asset, OpExecutionContext
import time

@asset
def monitored_asset(context: OpExecutionContext):
    """Asset with custom metrics"""

    start_time = time.time()

    try:
        result = process_data()

        # Log success metrics
        duration = time.time() - start_time
        context.add_output_metadata({
            "success": True,
            "duration_seconds": duration,
            "records_processed": len(result),
            "throughput_records_per_sec": len(result) / duration
        })

        return result

    except Exception as e:
        # Log failure metrics
        context.log.error(f"Asset failed: {str(e)}")
        context.add_output_metadata({
            "success": False,
            "error_message": str(e),
            "error_type": type(e).__name__
        })
        raise
```

#### Best Practices

- ✅ Use appropriate log levels (debug, info, warning, error)
- ✅ Add rich metadata to asset materializations
- ✅ Include business metrics in metadata (row counts, data quality)
- ✅ Use MetadataValue types for proper rendering in UI
- ✅ Log observations for external assets you monitor but don't create
- ✅ Integrate with external monitoring (CloudWatch, Datadog, etc.)
- ✅ Track execution time and throughput
- ⚠️ Don't log sensitive data (PII, credentials)
- ⚠️ Be mindful of log volume (can impact performance)

---

### CI/CD Integration

#### Branch Deployments Pattern

Dagster+ provides automatic branch deployments for every pull request.

**Benefits:**
- Automatic staging environment per PR
- Preview changes without affecting production
- Compare asset definitions between branch and main
- Test changes in isolation

**Setup with GitHub Actions:**

```yaml
# .github/workflows/dagster-cloud-deploy.yml
name: Dagster Cloud Deploy

on:
  push:
    branches:
      - main
  pull_request:

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Validate dagster_cloud.yaml
        run: |
          pip install dagster-cloud
          dagster-cloud ci check --project-dir .

      - name: Deploy to Dagster Cloud
        env:
          DAGSTER_CLOUD_API_TOKEN: ${{ secrets.DAGSTER_CLOUD_API_TOKEN }}
        run: |
          if [ "${{ github.ref }}" == "refs/heads/main" ]; then
            dagster-cloud ci deploy --project-dir . --deployment prod
          else
            dagster-cloud ci deploy --project-dir . --deployment pr-${{ github.event.pull_request.number }}
          fi
```

#### Hybrid Deployment with Kubernetes

```yaml
# .github/workflows/dagster-hybrid-deploy.yml
name: Deploy Dagster User Code

on:
  push:
    branches: [main]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Build Docker Image
        run: |
          docker build -t my-dagster-code:${{ github.sha }} .
          docker push my-registry/dagster-code:${{ github.sha }}

      - name: Update Kubernetes ConfigMap
        run: |
          kubectl create configmap dagster-workspace \
            --from-file=workspace.yaml \
            --dry-run=client -o yaml | kubectl apply -f -

      - name: Deploy to Dagster+
        env:
          DAGSTER_CLOUD_API_TOKEN: ${{ secrets.DAGSTER_CLOUD_API_TOKEN }}
        run: |
          dagster-cloud ci deploy \
            --location-name my_code_location \
            --image my-registry/dagster-code:${{ github.sha }}
```

#### Serverless Deployment

```yaml
# dagster_cloud.yaml
locations:
  - location_name: my_dagster_code
    code_source:
      package_name: my_project
    build:
      directory: .
      registry: docker.io/mycompany/dagster
```

#### Testing in CI

```yaml
# .github/workflows/test.yml
name: Test Dagster Code

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.10'

      - name: Install dependencies
        run: |
          pip install -e ".[dev]"

      - name: Run tests
        run: |
          pytest tests/ -v

      - name: Validate definitions
        run: |
          python -c "from my_project.definitions import defs; print(defs)"

      - name: Check asset dependencies
        run: |
          dagster asset list
          dagster asset check
```

#### Separation of Concerns Pattern

**System Deployment** (Dagit, daemons): Managed by Dagster+/ops team

**User Code Deployment** (your data pipelines): Managed by data teams via CI/CD

This separation allows data teams to deploy code independently without affecting the Dagster platform.

#### Best Practices

- ✅ Use branch deployments for PR previews
- ✅ Run tests before deploying to production
- ✅ Validate `dagster_cloud.yaml` in CI
- ✅ Use separate deployments for dev/staging/prod
- ✅ Tag Docker images with git SHA for traceability
- ✅ Automate deployment on merge to main
- ✅ Use environment-specific secrets in CI
- ⚠️ Don't skip validation steps to speed up CI
- ❌ Never force push to main or skip hooks

---

### Multi-Environment Setups

#### Environment-Based Resource Selection

```python
from dagster import Definitions, EnvVar
import os

def get_resources_by_environment():
    """Select resources based on DAGSTER_DEPLOYMENT environment variable"""

    env = os.getenv("DAGSTER_DEPLOYMENT", "local")

    if env == "production":
        return {
            "database": SnowflakeResource(
                account=EnvVar("SNOWFLAKE_ACCOUNT"),
                user=EnvVar("SNOWFLAKE_USER"),
                password=EnvVar("SNOWFLAKE_PASSWORD"),
                database="PROD",
                warehouse="PROD_WH",
                role="PROD_ROLE"
            ),
            "s3": S3Resource(
                bucket="prod-data-bucket",
                aws_access_key_id=EnvVar("AWS_ACCESS_KEY_ID"),
                aws_secret_access_key=EnvVar("AWS_SECRET_ACCESS_KEY")
            )
        }

    elif env == "staging":
        return {
            "database": SnowflakeResource(
                account=EnvVar("SNOWFLAKE_ACCOUNT"),
                user=EnvVar("SNOWFLAKE_USER"),
                password=EnvVar("SNOWFLAKE_PASSWORD"),
                database="STAGING",
                warehouse="STAGING_WH",
                role="STAGING_ROLE"
            ),
            "s3": S3Resource(
                bucket="staging-data-bucket",
                aws_access_key_id=EnvVar("AWS_ACCESS_KEY_ID"),
                aws_secret_access_key=EnvVar("AWS_SECRET_ACCESS_KEY")
            )
        }

    else:  # local development
        return {
            "database": MockSnowflakeResource(),
            "s3": MockS3Resource()
        }

defs = Definitions(
    assets=all_assets,
    resources=get_resources_by_environment()
)
```

#### EnvVar vs os.getenv

```python
from dagster import ConfigurableResource, EnvVar
import os

class MyResource(ConfigurableResource):
    # ✅ EnvVar: Retrieved at runtime, not visible in UI
    api_key: str = EnvVar("API_KEY")

    # ⚠️ os.getenv: Retrieved at load time, visible in UI
    endpoint: str = os.getenv("API_ENDPOINT", "https://api.example.com")
```

**Key Differences:**
- `EnvVar`: Value retrieved when resource is used (runtime), not visible in Dagster UI, more secure for secrets
- `os.getenv`: Value retrieved when code loads, visible in UI, use for non-sensitive config

#### Dagster+ Environment Variables

In Dagster+, you can configure environment variables with different scopes:

```yaml
# Via Dagster+ UI or API
# Deployment scope: production, staging, branch deployments
# Code location scope: specific code locations

# Example scopes:
# - production deployment, all code locations
# - production deployment, specific code location
# - branch deployments only
# - all deployments
```

#### Configuration File Pattern

```python
# config/local.yaml
database:
  type: sqlite
  path: ./dev.db

storage:
  type: local
  path: ./data

# config/production.yaml
database:
  type: snowflake
  account: ${SNOWFLAKE_ACCOUNT}
  database: PROD

storage:
  type: s3
  bucket: prod-data-bucket

# Load configuration
import yaml
from pathlib import Path

def load_config():
    env = os.getenv("DAGSTER_DEPLOYMENT", "local")
    config_path = Path(__file__).parent / "config" / f"{env}.yaml"

    with open(config_path) as f:
        return yaml.safe_load(f)

config = load_config()
```

#### Feature Flags per Environment

```python
from dagster import asset, Definitions
import os

ENABLE_ML_PIPELINE = os.getenv("ENABLE_ML_PIPELINE", "false") == "true"
ENABLE_EXPERIMENTAL_FEATURES = os.getenv("ENABLE_EXPERIMENTAL", "false") == "true"

@asset
def ml_predictions(context):
    """Only runs if ML pipeline is enabled"""
    return train_and_predict()

# Conditionally include assets
ml_assets = [ml_predictions] if ENABLE_ML_PIPELINE else []

defs = Definitions(
    assets=[
        *core_assets,
        *ml_assets,  # Only included in environments where flag is true
    ],
    resources=get_resources_by_environment()
)
```

#### Testing Different Environments

```python
import pytest
import os

@pytest.fixture
def local_env(monkeypatch):
    """Set up local environment"""
    monkeypatch.setenv("DAGSTER_DEPLOYMENT", "local")
    yield

@pytest.fixture
def prod_env(monkeypatch):
    """Set up production environment"""
    monkeypatch.setenv("DAGSTER_DEPLOYMENT", "production")
    monkeypatch.setenv("SNOWFLAKE_ACCOUNT", "test-account")
    monkeypatch.setenv("SNOWFLAKE_USER", "test-user")
    monkeypatch.setenv("SNOWFLAKE_PASSWORD", "test-pass")
    yield

def test_local_resources(local_env):
    """Test that local environment uses mocks"""
    from my_project.definitions import defs
    assert isinstance(defs.resources["database"], MockSnowflakeResource)

def test_prod_resources(prod_env):
    """Test that prod environment uses real resources"""
    from my_project.definitions import defs
    assert isinstance(defs.resources["database"], SnowflakeResource)
```

#### Best Practices

- ✅ Use `DAGSTER_DEPLOYMENT` environment variable to determine environment
- ✅ Use `EnvVar` for secrets (runtime evaluation, not visible in UI)
- ✅ Use `os.getenv` for non-sensitive configuration
- ✅ Provide mock resources for local development
- ✅ Use Dagster+ environment variable scoping for fine-grained control
- ✅ Test your configuration loading logic
- ✅ Document required environment variables
- ⚠️ Never commit secrets to version control
- ⚠️ Validate required environment variables at startup

---

## Common Use Cases

### ETL/ELT Workflows

#### Basic ETL Pipeline

```python
from dagster import asset, AssetExecutionContext

# Extract
@asset
def raw_customer_data(context: AssetExecutionContext) -> pd.DataFrame:
    """Extract customer data from source database"""
    context.log.info("Extracting customer data from PostgreSQL")

    df = pd.read_sql(
        "SELECT * FROM customers WHERE updated_at >= %(since)s",
        connection,
        params={"since": "2024-01-01"}
    )

    context.add_output_metadata({
        "num_rows": len(df),
        "columns": list(df.columns)
    })

    return df

# Transform
@asset
def cleaned_customer_data(
    context: AssetExecutionContext,
    raw_customer_data: pd.DataFrame
) -> pd.DataFrame:
    """Clean and validate customer data"""
    context.log.info("Cleaning customer data")

    # Remove duplicates
    df = raw_customer_data.drop_duplicates(subset=['customer_id'])

    # Validate email format
    df = df[df['email'].str.contains('@')]

    # Standardize phone numbers
    df['phone'] = df['phone'].apply(standardize_phone)

    # Handle nulls
    df['country'] = df['country'].fillna('Unknown')

    context.add_output_metadata({
        "num_rows": len(df),
        "duplicates_removed": len(raw_customer_data) - len(df),
        "null_counts": df.isnull().sum().to_dict()
    })

    return df

# Load
@asset
def customer_warehouse_table(
    context: AssetExecutionContext,
    cleaned_customer_data: pd.DataFrame,
    warehouse: SnowflakeResource
):
    """Load cleaned data to Snowflake"""
    context.log.info("Loading data to Snowflake")

    warehouse.write_table(
        table="prod.customers",
        data=cleaned_customer_data,
        if_exists="replace"
    )

    context.add_output_metadata({
        "rows_loaded": len(cleaned_customer_data),
        "target_table": "prod.customers"
    })
```

#### ELT Pipeline (Load then Transform in Warehouse)

```python
from dagster import asset

# Extract & Load (EL)
@asset
def raw_sales_landing(context, s3: S3Resource, snowflake: SnowflakeResource):
    """Extract from S3 and load raw to Snowflake"""

    # Copy directly from S3 to Snowflake (no transformation)
    snowflake.execute("""
        COPY INTO raw.sales
        FROM 's3://bucket/sales/'
        CREDENTIALS = (AWS_KEY_ID='...' AWS_SECRET_KEY='...')
        FILE_FORMAT = (TYPE = 'CSV');
    """)

    return "loaded"

# Transform in warehouse
@asset(deps=[raw_sales_landing])
def silver_sales_cleaned(context, snowflake: SnowflakeResource):
    """Transform data using SQL in Snowflake"""

    snowflake.execute("""
        CREATE OR REPLACE TABLE silver.sales AS
        SELECT
            order_id,
            customer_id,
            DATE(order_date) as order_date,
            ROUND(amount, 2) as amount,
            UPPER(TRIM(status)) as status
        FROM raw.sales
        WHERE amount > 0
            AND order_date >= CURRENT_DATE - 90
    """)

    return "transformed"

@asset(deps=[silver_sales_cleaned])
def gold_daily_sales_summary(context, snowflake: SnowflakeResource):
    """Create business-ready aggregates"""

    snowflake.execute("""
        CREATE OR REPLACE TABLE gold.daily_sales_summary AS
        SELECT
            order_date,
            COUNT(DISTINCT order_id) as num_orders,
            COUNT(DISTINCT customer_id) as num_customers,
            SUM(amount) as total_revenue,
            AVG(amount) as avg_order_value
        FROM silver.sales
        GROUP BY order_date
    """)
```

#### Multi-Source ETL with Joins

```python
@asset
def salesforce_accounts(context):
    """Extract from Salesforce"""
    return extract_from_salesforce("Account")

@asset
def stripe_payments(context):
    """Extract from Stripe"""
    return extract_from_stripe("charges")

@asset
def postgres_users(context):
    """Extract from PostgreSQL"""
    return extract_from_postgres("users")

@asset
def unified_customer_view(
    context,
    salesforce_accounts,
    stripe_payments,
    postgres_users
):
    """Join data from multiple sources"""

    # Merge on email
    customers = postgres_users.merge(
        salesforce_accounts,
        on='email',
        how='left'
    )

    # Add payment data
    customers = customers.merge(
        stripe_payments.groupby('customer_email').agg({
            'amount': 'sum',
            'charge_id': 'count'
        }).rename(columns={'charge_id': 'num_purchases'}),
        left_on='email',
        right_index=True,
        how='left'
    )

    return customers
```

---

### ML Pipelines

#### End-to-End ML Pipeline

```python
from dagster import asset, AssetExecutionContext
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
import joblib

# 1. Feature Engineering
@asset
def ml_features(context: AssetExecutionContext, cleaned_customer_data: pd.DataFrame):
    """Engineer features for ML model"""

    features = cleaned_customer_data.copy()

    # Create features
    features['account_age_days'] = (
        pd.Timestamp.now() - pd.to_datetime(features['created_at'])
    ).dt.days

    features['is_premium'] = features['subscription_type'] == 'premium'

    # Encode categoricals
    features = pd.get_dummies(features, columns=['country', 'industry'])

    context.add_output_metadata({
        "num_features": len(features.columns),
        "num_samples": len(features)
    })

    return features

# 2. Train/Test Split
@asset
def train_test_data(context, ml_features: pd.DataFrame):
    """Split data into train and test sets"""

    X = ml_features.drop(columns=['churn_label'])
    y = ml_features['churn_label']

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    context.add_output_metadata({
        "train_samples": len(X_train),
        "test_samples": len(X_test),
        "positive_rate_train": y_train.mean(),
        "positive_rate_test": y_test.mean()
    })

    return {
        "X_train": X_train,
        "X_test": X_test,
        "y_train": y_train,
        "y_test": y_test
    }

# 3. Train Model
@asset
def churn_model(context, train_test_data: dict):
    """Train churn prediction model"""

    model = RandomForestClassifier(
        n_estimators=100,
        max_depth=10,
        random_state=42
    )

    model.fit(
        train_test_data["X_train"],
        train_test_data["y_train"]
    )

    # Save model
    model_path = "/models/churn_model.pkl"
    joblib.dump(model, model_path)

    context.add_output_metadata({
        "model_path": model_path,
        "n_estimators": 100,
        "max_depth": 10
    })

    return model

# 4. Evaluate Model
@asset
def model_evaluation(context, churn_model, train_test_data: dict):
    """Evaluate model performance"""
    from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score

    y_pred = churn_model.predict(train_test_data["X_test"])

    metrics = {
        "accuracy": accuracy_score(train_test_data["y_test"], y_pred),
        "precision": precision_score(train_test_data["y_test"], y_pred),
        "recall": recall_score(train_test_data["y_test"], y_pred),
        "f1": f1_score(train_test_data["y_test"], y_pred)
    }

    context.add_output_metadata({
        **{k: MetadataValue.float(v) for k, v in metrics.items()},
        "evaluation_report": MetadataValue.md(f"""
        ## Model Performance
        - Accuracy: {metrics['accuracy']:.3f}
        - Precision: {metrics['precision']:.3f}
        - Recall: {metrics['recall']:.3f}
        - F1 Score: {metrics['f1']:.3f}
        """)
    })

    return metrics

# 5. Generate Predictions
@asset(deps=[model_evaluation])  # Only run if evaluation passed
def customer_churn_predictions(context, churn_model, cleaned_customer_data):
    """Generate predictions for all customers"""

    # Load model
    model = churn_model

    # Prepare features (same as training)
    features = prepare_features(cleaned_customer_data)

    # Predict
    predictions = model.predict_proba(features)[:, 1]

    result = cleaned_customer_data.copy()
    result['churn_probability'] = predictions
    result['churn_risk_level'] = pd.cut(
        predictions,
        bins=[0, 0.3, 0.7, 1.0],
        labels=['low', 'medium', 'high']
    )

    context.add_output_metadata({
        "num_predictions": len(result),
        "high_risk_customers": (result['churn_risk_level'] == 'high').sum(),
        "avg_churn_probability": predictions.mean()
    })

    return result
```

#### MLOps Pattern with Model Registry

```python
from dagster import asset, Config

class ModelConfig(Config):
    """Configuration for model training"""
    n_estimators: int = 100
    max_depth: int = 10
    min_samples_split: int = 2

@asset
def trained_model_with_registry(
    context,
    train_test_data: dict,
    config: ModelConfig
):
    """Train model and register to MLflow"""
    import mlflow

    with mlflow.start_run():
        # Log parameters
        mlflow.log_params({
            "n_estimators": config.n_estimators,
            "max_depth": config.max_depth,
            "min_samples_split": config.min_samples_split
        })

        # Train model
        model = RandomForestClassifier(
            n_estimators=config.n_estimators,
            max_depth=config.max_depth,
            min_samples_split=config.min_samples_split
        )
        model.fit(train_test_data["X_train"], train_test_data["y_train"])

        # Evaluate
        train_score = model.score(
            train_test_data["X_train"],
            train_test_data["y_train"]
        )
        test_score = model.score(
            train_test_data["X_test"],
            train_test_data["y_test"]
        )

        # Log metrics
        mlflow.log_metrics({
            "train_accuracy": train_score,
            "test_accuracy": test_score
        })

        # Register model
        mlflow.sklearn.log_model(model, "churn_model")

    return model
```

---

### dbt Integration

#### Basic dbt Integration

```python
from dagster import AssetExecutionContext
from dagster_dbt import DbtProject, dbt_assets

# Define dbt project
dbt_project = DbtProject(
    project_dir="/path/to/dbt/project",
    target="prod"
)

# Load dbt models as Dagster assets
@dbt_assets(
    manifest=dbt_project.manifest_path,
    project=dbt_project
)
def my_dbt_assets(context: AssetExecutionContext):
    """All dbt models as Dagster assets"""
    yield from dbt_project.cli(["build"], context=context).stream()
```

#### dbt with Upstream Dagster Assets

```python
from dagster import asset, AssetExecutionContext
from dagster_dbt import dbt_assets, DbtProject

# Dagster asset that dbt depends on
@asset
def raw_customers_csv(context: AssetExecutionContext, s3: S3Resource):
    """Extract raw data to S3"""
    data = extract_from_api()
    s3.write("raw/customers.csv", data)
    return data

# dbt assets
dbt_project = DbtProject(project_dir="dbt_project")

@dbt_assets(
    manifest=dbt_project.manifest_path,
    project=dbt_project
)
def dbt_models(context: AssetExecutionContext):
    """dbt models that depend on raw_customers_csv"""
    # raw_customers_csv runs first, then dbt models
    yield from dbt_project.cli(["build"], context=context).stream()
```

#### Dagster Assets Depending on dbt Models

```python
# Don't need to re-declare dbt assets, just reference them
@asset(deps=["dbt_model_name"])
def ml_features_from_dbt(context, snowflake: SnowflakeResource):
    """Asset that depends on dbt model"""
    # dbt model runs first, then this asset
    df = snowflake.read_table("analytics.dbt_model_name")
    return engineer_features(df)
```

#### dbt Incremental Models with Partitions

```python
from dagster import DailyPartitionsDefinition, AssetExecutionContext
from dagster_dbt import dbt_assets, DbtProject

daily_partitions = DailyPartitionsDefinition(start_date="2024-01-01")

dbt_project = DbtProject(
    project_dir="dbt_project",
    target="prod"
)

@dbt_assets(
    manifest=dbt_project.manifest_path,
    project=dbt_project,
    partitions_def=daily_partitions
)
def partitioned_dbt_assets(context: AssetExecutionContext):
    """dbt models with daily partitions"""

    # Get partition date
    partition_date = context.partition_key

    # Pass to dbt as variable
    yield from dbt_project.cli(
        [
            "build",
            "--vars",
            f'{{"partition_date": "{partition_date}"}}'
        ],
        context=context
    ).stream()

# In dbt model (models/incremental_sales.sql):
# {{ config(materialized='incremental') }}
#
# SELECT * FROM {{ source('raw', 'sales') }}
# WHERE date = '{{ var("partition_date") }}'
# {% if is_incremental() %}
#   AND date > (SELECT MAX(date) FROM {{ this }})
# {% endif %}
```

#### dbt Defer Pattern (Dev vs Prod)

```python
from dagster_dbt import DbtProject
import os

env = os.getenv("DAGSTER_DEPLOYMENT", "dev")

if env == "dev":
    # In dev, use defer to production state
    dbt_project = DbtProject(
        project_dir="dbt_project",
        target="dev",
        state_path="/path/to/prod/manifest"  # Reference prod state
    )
else:
    # In prod, no defer
    dbt_project = DbtProject(
        project_dir="dbt_project",
        target="prod"
    )
```

#### Best Practices for dbt Integration

- ✅ Use DbtProject component for easy integration
- ✅ Reference dbt models by asset key, don't re-declare them
- ✅ Use partitions for incremental models
- ✅ Leverage dbt defer for development workflows
- ✅ Keep dbt project outside Dagster project directory
- ✅ Use separate automation for ingestion vs transformation
- ⚠️ Don't run dbt models more frequently than data updates

---

### Data Quality Checks

#### Asset Checks (Native Dagster)

```python
from dagster import asset, asset_check, AssetCheckResult, AssetCheckSeverity

@asset
def customer_data(context):
    """Customer data asset"""
    return load_customer_data()

# Define checks for the asset
@asset_check(asset=customer_data)
def check_no_nulls_in_email(context):
    """Check that email column has no nulls"""
    df = load_customer_data()
    null_count = df['email'].isnull().sum()

    return AssetCheckResult(
        passed=null_count == 0,
        metadata={
            "null_count": null_count,
            "total_rows": len(df)
        },
        severity=AssetCheckSeverity.ERROR if null_count > 0 else None
    )

@asset_check(asset=customer_data)
def check_email_format(context):
    """Check email format validity"""
    df = load_customer_data()
    invalid = ~df['email'].str.contains('@')
    invalid_count = invalid.sum()

    return AssetCheckResult(
        passed=invalid_count == 0,
        metadata={
            "invalid_count": invalid_count,
            "invalid_emails": df[invalid]['email'].tolist()[:10]  # Sample
        }
    )

@asset_check(asset=customer_data)
def check_reasonable_age_range(context):
    """Check age is in reasonable range"""
    df = load_customer_data()
    out_of_range = (df['age'] < 18) | (df['age'] > 120)
    invalid_count = out_of_range.sum()

    return AssetCheckResult(
        passed=invalid_count == 0,
        metadata={
            "invalid_count": invalid_count,
            "min_age": df['age'].min(),
            "max_age": df['age'].max()
        },
        severity=AssetCheckSeverity.WARN  # Warning, not error
    )
```

#### Multi-Asset Checks

```python
from dagster import multi_asset_check, AssetCheckKey

@multi_asset_check(
    asset_keys=["customer_data", "order_data"],
    name="referential_integrity"
)
def check_referential_integrity(context):
    """Check that all orders reference valid customers"""
    customers = load_customer_data()
    orders = load_order_data()

    # Find orders with invalid customer_id
    invalid_orders = orders[~orders['customer_id'].isin(customers['customer_id'])]

    return [
        AssetCheckResult(
            passed=len(invalid_orders) == 0,
            asset_key="order_data",
            metadata={
                "invalid_orders": len(invalid_orders),
                "sample_invalid_ids": invalid_orders['customer_id'].unique()[:5].tolist()
            }
        )
    ]
```

#### Great Expectations Integration

```python
from dagster import asset
import great_expectations as ge

@asset
def validated_customer_data(context):
    """Customer data with Great Expectations validation"""

    df = load_customer_data()

    # Convert to GE dataset
    ge_df = ge.from_pandas(df)

    # Define expectations
    ge_df.expect_column_values_to_not_be_null("email")
    ge_df.expect_column_values_to_match_regex("email", r"^[^@]+@[^@]+\.[^@]+$")
    ge_df.expect_column_values_to_be_between("age", min_value=18, max_value=120)
    ge_df.expect_column_values_to_be_in_set("status", ["active", "inactive", "suspended"])

    # Validate
    validation_result = ge_df.validate()

    # Log results
    context.add_output_metadata({
        "validation_success": validation_result.success,
        "expectations_evaluated": validation_result.statistics["evaluated_expectations"],
        "successful_expectations": validation_result.statistics["successful_expectations"],
        "validation_report": MetadataValue.md(
            validation_result.to_json_dict()
        )
    })

    if not validation_result.success:
        # Optionally fail the asset or just warn
        context.log.warning("Data quality checks failed!")

    return df
```

#### Data Quality Dimensions

```python
from dagster import asset_check, AssetCheckResult

# 1. Completeness
@asset_check(asset=customer_data)
def check_completeness(context):
    """Check all required records are present"""
    df = load_customer_data()
    expected_count = get_expected_record_count()
    actual_count = len(df)

    return AssetCheckResult(
        passed=actual_count >= expected_count * 0.95,  # 95% threshold
        metadata={
            "expected": expected_count,
            "actual": actual_count,
            "completeness_pct": (actual_count / expected_count) * 100
        }
    )

# 2. Uniqueness
@asset_check(asset=customer_data)
def check_uniqueness(context):
    """Check for duplicate records"""
    df = load_customer_data()
    duplicates = df.duplicated(subset=['customer_id']).sum()

    return AssetCheckResult(
        passed=duplicates == 0,
        metadata={"duplicate_count": duplicates}
    )

# 3. Timeliness
@asset_check(asset=customer_data)
def check_timeliness(context):
    """Check data freshness"""
    df = load_customer_data()
    latest_timestamp = pd.to_datetime(df['updated_at']).max()
    age_hours = (pd.Timestamp.now() - latest_timestamp).total_seconds() / 3600

    return AssetCheckResult(
        passed=age_hours < 24,  # Data should be less than 24 hours old
        metadata={
            "latest_timestamp": str(latest_timestamp),
            "age_hours": age_hours
        }
    )

# 4. Consistency
@asset_check(asset=customer_data)
def check_consistency(context):
    """Check cross-field consistency"""
    df = load_customer_data()

    # Example: premium customers should have valid payment method
    premium_without_payment = df[
        (df['subscription_type'] == 'premium') &
        (df['payment_method'].isnull())
    ]

    return AssetCheckResult(
        passed=len(premium_without_payment) == 0,
        metadata={
            "inconsistent_records": len(premium_without_payment)
        }
    )
```

---

### Incremental Processing and Partitioning

#### Time-Based Partitions

```python
from dagster import asset, DailyPartitionsDefinition, AssetExecutionContext

daily_partitions = DailyPartitionsDefinition(start_date="2024-01-01")

@asset(partitions_def=daily_partitions)
def daily_sales(context: AssetExecutionContext):
    """Process one day of sales data at a time"""

    # Get the partition date
    partition_date = context.partition_key

    context.log.info(f"Processing sales for {partition_date}")

    # Query only that day's data
    df = fetch_sales_for_date(partition_date)

    context.add_output_metadata({
        "partition_date": partition_date,
        "num_sales": len(df),
        "total_revenue": df['amount'].sum()
    })

    return df
```

#### Static Partitions

```python
from dagster import asset, StaticPartitionsDefinition

regions = StaticPartitionsDefinition(["north", "south", "east", "west"])

@asset(partitions_def=regions)
def regional_sales(context: AssetExecutionContext):
    """Process sales by region"""

    region = context.partition_key

    df = fetch_sales_for_region(region)

    return df
```

#### Dynamic Partitions

```python
from dagster import asset, DynamicPartitionsDefinition, sensor, RunRequest

# Define dynamic partitions
customer_partitions = DynamicPartitionsDefinition(name="customers")

@asset(partitions_def=customer_partitions)
def customer_report(context: AssetExecutionContext):
    """Generate report for each customer"""
    customer_id = context.partition_key
    return generate_report(customer_id)

# Sensor to add new partitions dynamically
@sensor(asset_selection=[customer_report])
def new_customer_sensor(context):
    """Detect new customers and create partitions"""

    new_customers = detect_new_customers()

    if new_customers:
        # Add new partitions
        customer_partitions.add_partitions(new_customers)

        # Request runs for new partitions
        for customer_id in new_customers:
            yield RunRequest(
                partition_key=customer_id,
                tags={"customer_id": customer_id}
            )
```

#### Two-Dimensional Partitioning

```python
from dagster import asset, MultiPartitionsDefinition, DailyPartitionsDefinition, StaticPartitionsDefinition

# Partition by both date and region
multi_partitions = MultiPartitionsDefinition({
    "date": DailyPartitionsDefinition(start_date="2024-01-01"),
    "region": StaticPartitionsDefinition(["north", "south", "east", "west"])
})

@asset(partitions_def=multi_partitions)
def regional_daily_sales(context: AssetExecutionContext):
    """Process sales by region and date"""

    # Access both partition dimensions
    partition_keys = context.partition_key.keys_by_dimension
    date = partition_keys["date"]
    region = partition_keys["region"]

    context.log.info(f"Processing {region} sales for {date}")

    df = fetch_sales(date=date, region=region)

    return df
```

#### Partition Mappings for Dependencies

```python
from dagster import asset, TimeWindowPartitionMapping, DailyPartitionsDefinition, WeeklyPartitionsDefinition

daily = DailyPartitionsDefinition(start_date="2024-01-01")
weekly = WeeklyPartitionsDefinition(start_date="2024-01-01")

@asset(partitions_def=daily)
def daily_metrics(context):
    """Daily metrics"""
    date = context.partition_key
    return compute_daily_metrics(date)

@asset(
    partitions_def=weekly,
    ins={
        "daily_metrics": AssetIn(
            partition_mapping=TimeWindowPartitionMapping()
        )
    }
)
def weekly_rollup(context, daily_metrics):
    """Aggregate daily metrics to weekly"""
    # Receives all daily partitions for the week
    week = context.partition_key
    return aggregate_to_weekly(daily_metrics)
```

#### Incremental File Processing

```python
from dagster import asset, sensor, RunRequest, DynamicPartitionsDefinition

file_partitions = DynamicPartitionsDefinition(name="files")

@asset(partitions_def=file_partitions)
def processed_file(context: AssetExecutionContext, s3: S3Resource):
    """Process a single file"""
    file_key = context.partition_key

    # Download and process file
    data = s3.read(file_key)
    processed = process_data(data)

    # Write output
    s3.write(f"processed/{file_key}", processed)

    return processed

@sensor(asset_selection=[processed_file])
def new_file_sensor(context, s3: S3Resource):
    """Detect new files in S3 and create partitions"""

    # List files in bucket
    all_files = s3.list_objects("raw/")

    # Get already processed files
    existing_partitions = context.instance.get_dynamic_partitions(
        file_partitions.name
    )

    # Find new files
    new_files = [f for f in all_files if f not in existing_partitions]

    if new_files:
        # Add partitions for new files
        file_partitions.add_partitions(new_files)

        # Request runs for each new file
        for file_key in new_files:
            yield RunRequest(
                partition_key=file_key,
                tags={"file": file_key}
            )
```

#### Backfilling Partitions

```python
# Via CLI
# dagster asset materialize --select daily_sales --partition 2024-01-01:2024-12-31

# Programmatic backfill
from dagster import build_asset_context

def backfill_partitions(start_date, end_date):
    """Backfill partitions for a date range"""
    dates = pd.date_range(start_date, end_date)

    for date in dates:
        context = build_asset_context(partition_key=date.strftime("%Y-%m-%d"))
        result = daily_sales(context)
        print(f"Processed {date}: {len(result)} records")
```

#### Best Practices

- ✅ Use partitions for incremental processing of large datasets
- ✅ Limit partition count to <100,000 per asset
- ✅ Use TimeWindowPartitionMapping for aggregations across time windows
- ✅ Use dynamic partitions for data with unknown keys (e.g., new files, customers)
- ✅ Use sensors to detect new data and trigger partition runs
- ✅ Use backfills to reprocess historical data
- ⚠️ Be cautious with fine-grained partitions (hourly/minutely) - they multiply quickly
- ⚠️ Monitor partition count growth over time

---

### Cross-Team Collaboration

#### Multi-Project Workspace

```
workspace_root/
├── team_a_project/
│   ├── pyproject.toml
│   └── team_a/
│       ├── definitions.py
│       └── assets/
│           └── raw_data.py
├── team_b_project/
│   ├── pyproject.toml
│   └── team_b/
│       ├── definitions.py
│       └── assets/
│           └── analytics.py
└── workspace.yaml
```

**workspace.yaml:**
```yaml
load_from:
  - python_package:
      package_name: team_a
      location_name: team_a_location

  - python_package:
      package_name: team_b
      location_name: team_b_location
```

#### Code Locations for Team Separation

```python
# Team A's definitions.py
from dagster import Definitions, asset

@asset(group_name="team_a")
def customer_raw_data(context):
    """Managed by Team A"""
    return extract_customers()

team_a_defs = Definitions(
    assets=[customer_raw_data],
    resources={...}
)

# Team B's definitions.py
from dagster import Definitions, asset, AssetKey

@asset(
    group_name="team_b",
    deps=[AssetKey("customer_raw_data")]  # Reference Team A's asset
)
def customer_analytics(context):
    """Managed by Team B, depends on Team A"""
    # Load Team A's output
    customers = load_from_warehouse("team_a.customer_raw_data")
    return compute_analytics(customers)

team_b_defs = Definitions(
    assets=[customer_analytics],
    resources={...}
)
```

#### Asset Sensors for Cross-Team Coordination

```python
from dagster import asset_sensor, AssetKey, RunRequest

@asset_sensor(
    asset_key=AssetKey("customer_raw_data"),  # Team A's asset
    job=customer_analytics_job  # Team B's job
)
def customer_data_updated_sensor(context, asset_event):
    """Trigger Team B's job when Team A's data updates"""

    yield RunRequest(
        run_key=f"analytics_{asset_event.partition_key}",
        tags={
            "upstream_run_id": asset_event.dagster_run_id,
            "trigger": "customer_data_updated"
        }
    )
```

#### Shared Resources Pattern

```python
# shared_resources.py
from dagster import ConfigurableResource, EnvVar

class SharedWarehouse(ConfigurableResource):
    """Shared Snowflake resource for all teams"""
    account: str
    database: str
    warehouse: str

    def read_table(self, schema: str, table: str):
        """Read from team's schema"""
        return pd.read_sql(
            f"SELECT * FROM {self.database}.{schema}.{table}",
            self.get_connection()
        )

# Team A uses it
team_a_defs = Definitions(
    assets=[...],
    resources={
        "warehouse": SharedWarehouse(
            account=EnvVar("SNOWFLAKE_ACCOUNT"),
            database="SHARED_DB",
            warehouse="TEAM_A_WH"
        )
    }
)

# Team B uses it
team_b_defs = Definitions(
    assets=[...],
    resources={
        "warehouse": SharedWarehouse(
            account=EnvVar("SNOWFLAKE_ACCOUNT"),
            database="SHARED_DB",
            warehouse="TEAM_B_WH"
        )
    }
)
```

#### Dagster+ Agent Routing (Isolation)

```yaml
# dagster_cloud.yaml
locations:
  - location_name: team_a
    code_source:
      package_name: team_a
    agent:
      agent_label: team-a-agent  # Dedicated agent

  - location_name: team_b
    code_source:
      package_name: team_b
    agent:
      agent_label: team-b-agent  # Separate agent
```

Benefits:
- Separate execution environments
- Independent resource allocation
- Isolated failures
- Different deployment cycles

#### Documentation and Ownership

```python
from dagster import asset

@asset(
    group_name="customer_data",
    metadata={
        "owner": "team-a@company.com",
        "documentation": "https://wiki.company.com/customer-data",
        "sla_hours": 24,
        "update_frequency": "daily"
    }
)
def customer_raw_data(context):
    """
    Raw customer data from Salesforce

    Owner: Team A (Data Platform)
    Update Schedule: Daily at 2 AM UTC
    Dependencies: None
    Consumers: Team B (Analytics), Team C (ML)

    Schema:
    - customer_id: Unique identifier
    - email: Customer email
    - created_at: Account creation timestamp
    """
    return extract_customers()
```

#### Shared Data Contracts

```python
# contracts/customer_schema.py
from pydantic import BaseModel
from datetime import datetime

class CustomerSchema(BaseModel):
    """Shared schema contract for customer data"""
    customer_id: str
    email: str
    created_at: datetime
    status: str

# Team A (producer)
@asset
def customer_raw_data(context) -> list[CustomerSchema]:
    """Conforms to CustomerSchema contract"""
    data = extract_customers()
    # Validate against schema
    validated = [CustomerSchema(**row) for row in data]
    return validated

# Team B (consumer)
@asset
def customer_analytics(context, customer_raw_data: list[CustomerSchema]):
    """Expects CustomerSchema contract"""
    # Guaranteed to have correct schema
    df = pd.DataFrame([c.dict() for c in customer_raw_data])
    return compute_analytics(df)
```

#### Best Practices

- ✅ Use separate code locations for teams with different dependencies or release cycles
- ✅ Use asset sensors for cross-team coordination
- ✅ Document asset ownership, SLAs, and consumers
- ✅ Use shared resource definitions for common infrastructure
- ✅ Establish data contracts/schemas for shared assets
- ✅ Use agent routing for execution isolation in Dagster+
- ✅ Organize assets by group_name to visualize team boundaries
- ⚠️ Balance centralization (shared standards) vs autonomy (team independence)
- ⚠️ Monitor cross-team dependencies to avoid tight coupling

---

## Production Case Studies

### 1. US Foods (Fortune 500)

**Scale:** $24B annual operations

**Achievements:**
- 99.996% uptime
- Eliminated data silos
- Built self-service data platform

**Architecture:**
- Dagster orchestrating end-to-end data pipelines
- Multi-source integration
- Self-service capabilities for business users

### 2. easyJet Holidays (Travel)

**Problem:** Fragmented AWS stack, slow pipelines

**Results:**
- **15x faster pipeline execution** (2.5 hours → 10 minutes)
- Eliminated manual troubleshooting overhead
- Unified data orchestration platform

**Migration:** From fragmented AWS services to Dagster

### 3. Ida (AI for Food Waste - Multi-Tenant SaaS)

**Use Case:** Processing millions of rows daily for multiple clients

**Architecture:**
- Medallion architecture (Bronze → Silver → Gold)
- Multi-tenant data processing with chaotic source data
- dbt used as "modeling clay" for production middleware
- BigQuery for ELT transformations

**Pattern:** Load raw chaotic data first, then transform in warehouse

### 4. Belgian Government (Fédération Wallonie-Bruxelles)

**Team:** Data engineer Martin Erpicum

**Results:**
- **2x faster pipeline delivery**
- Shift from reactive maintenance to proactive data product development
- Improved data quality detection and resolution
- Data teams now propose new products instead of just reacting to requests

**Platform Transformation:** From operational overhead to strategic data platform

### 5. Mejuri (E-commerce Jewelry)

**Production Experience:** 2+ years

**Scale:**
- 47 pipelines
- 2 Dagster production instances

**Use Cases:**
- E-commerce analytics
- Inventory management
- Customer behavior tracking

### 6. KIPP (Education)

**Challenge:** Fragmented data stack

**Solution:** Dagster as unified platform

**Results:**
- Improved observability and lineage
- Faster development (weeks → days for new integrations)
- Better data quality

### 7. Zippi (YC 2019 - Fintech)

**Use Case:** ML model operationalization for loan underwriting

**Pattern:** Incremental Dagster adoption
1. Started with ML model scoring pipeline
2. Created data assets in S3 for credit team
3. Expanded to other workflows

**Lesson:** Start small, expand gradually

---

## Anti-Patterns to Avoid

### 1. Loading Heavy Libraries at Module Level

**Problem:** Everything imported at module level loads into memory when code location starts

**❌ Anti-Pattern:**
```python
# asset_definitions.py
import postal  # Adds 2GB to memory!
from transformers import AutoModel  # Adds 1GB+!

@asset
def parse_addresses(context):
    # postal library already loaded
    return postal.parse_address(...)
```

**✅ Solution:**
```python
# asset_definitions.py

@asset
def parse_addresses(context):
    # Import only when asset runs
    import postal
    return postal.parse_address(...)

# OR use resource pattern
class PostalResource(ConfigurableResource):
    def __init__(self):
        import postal
        self._postal = postal

    def parse(self, address):
        return self._postal.parse_address(address)
```

**Impact:** At scale (50+ code locations), module-level imports can consume 5-10GB of memory before any work is done.

### 2. Too Many Code Locations

**Problem:** Each code location pod consumes baseline resources (CPU, memory, gRPC server)

**❌ Anti-Pattern:**
- One code location per team member
- One code location per small project
- 50+ code locations for a medium-sized team

**Resource Math:**
- 1 code location: 100MB baseline
- 50 code locations: 5GB before loading any code
- Plus memory for imports, definitions, etc.

**✅ Solution:**
- Group related assets in same code location
- Use asset groups for organization, not separate code locations
- Limit code locations to teams with truly conflicting dependencies

### 3. Overly Complex Partition Schemes

**❌ Anti-Pattern:**
```python
# Creates 175,200 partitions!
hourly_by_customer = MultiPartitionsDefinition({
    "hour": HourlyPartitionsDefinition(start_date="2022-01-01"),
    "customer": StaticPartitionsDefinition([...1000 customers...])
})
```

**Problems:**
- UI becomes slow
- Backfills take forever
- Dagster database bloat

**✅ Solution:**
```python
# Reasonable partition count
daily_partitions = DailyPartitionsDefinition(
    start_date="2024-01-01",
    end_offset=0  # Don't partition future
)
# ~365 partitions per year
```

**Rule of Thumb:** Keep partitions < 100,000 per asset

### 4. Not Using Resources for External Systems

**❌ Anti-Pattern:**
```python
@asset
def my_asset(context):
    # Hardcoded connection details
    conn = psycopg2.connect(
        host="prod-db.example.com",
        user="dagster",
        password="hardcoded_password"  # Security risk!
    )
    # Can't mock in tests
    # Can't swap dev/prod easily
```

**✅ Solution:**
```python
class DatabaseResource(ConfigurableResource):
    host: str
    user: str
    password: str

@asset
def my_asset(context, database: DatabaseResource):
    conn = database.get_connection()
    # Easy to mock
    # Environment-specific configuration
    # Secrets via EnvVar
```

### 5. Ignoring Asset Checks

**❌ Anti-Pattern:**
```python
@asset
def customer_data(context):
    df = load_data()
    # No validation!
    return df

# Bad data propagates downstream
```

**✅ Solution:**
```python
@asset
def customer_data(context):
    df = load_data()
    return df

@asset_check(asset=customer_data)
def validate_customer_data(context):
    df = load_data()
    null_count = df['email'].isnull().sum()
    return AssetCheckResult(
        passed=null_count == 0,
        metadata={"null_count": null_count}
    )
```

### 6. Overly Broad Asset Dependencies

**❌ Anti-Pattern:**
```python
@asset
def huge_monolithic_asset(context):
    # Loads ALL data
    # Does ALL transformations
    # 10,000 lines of code
    # Takes 6 hours to run
    # Fails at hour 5, must restart from beginning
    pass
```

**✅ Solution:**
```python
# Break into smaller, focused assets
@asset
def raw_data(context):
    return extract()  # 10 minutes

@asset
def cleaned_data(context, raw_data):
    return clean(raw_data)  # 5 minutes

@asset
def analytics(context, cleaned_data):
    return analyze(cleaned_data)  # 15 minutes

# Benefits:
# - Failures are isolated
# - Can retry individual steps
# - Parallel execution possible
# - Easier to understand and maintain
```

### 7. Using ops Instead of assets for Data Pipelines

**❌ Anti-Pattern (for data pipelines):**
```python
@op
def extract_customers():
    return fetch_customers()

@op
def transform_customers(customers):
    return transform(customers)

@job
def customer_pipeline():
    transform_customers(extract_customers())

# No asset lineage
# No observability of data products
# Can't reuse across jobs easily
```

**✅ Solution:**
```python
@asset
def customers(context):
    return fetch_customers()

@asset
def transformed_customers(context, customers):
    return transform(customers)

# Automatic lineage
# Asset-centric UI
# Reusable across definitions
```

### 8. Not Using Retry Policies for Transient Failures

**❌ Anti-Pattern:**
```python
@asset
def flaky_api_call(context):
    # Fails on transient network errors
    # No retry
    return call_external_api()
```

**✅ Solution:**
```python
@asset(
    retry_policy=RetryPolicy(
        max_retries=3,
        delay=2,
        backoff=Backoff.EXPONENTIAL
    )
)
def resilient_api_call(context):
    return call_external_api()
```

### 9. Forgetting to Set Partition End Offsets

**❌ Anti-Pattern:**
```python
# Partitions future dates unnecessarily
daily = DailyPartitionsDefinition(
    start_date="2024-01-01"
    # Missing end_offset
)
# Creates partitions through 2030!
```

**✅ Solution:**
```python
daily = DailyPartitionsDefinition(
    start_date="2024-01-01",
    end_offset=0  # Only partition through today
)
```

### 10. Poor Naming Conventions

**❌ Anti-Pattern:**
```python
@asset
def job1(context):  # Meaningless
    pass

@asset
def process_data(context):  # Too vague
    pass

@asset
def myAsset(context):  # Wrong convention
    pass
```

**✅ Solution:**
```python
@asset
def daily_customer_sales_summary(context):  # Clear, descriptive
    pass

@asset
def stripe_payment_records(context):  # Indicates source
    pass

@asset
def gold_customer_360_view(context):  # Indicates layer
    pass
```

---

## Key Takeaways

### Top Design Patterns to Adopt

1. **Use Assets for Data Pipelines** - Assets are the modern Dagster paradigm
2. **Factory Patterns for Repetitive Logic** - Keep code DRY
3. **ConfigurableResource for External Systems** - Type-safe dependency injection
4. **Asset Checks for Data Quality** - Validate early and often
5. **Partitioning for Incremental Processing** - Process only what's needed
6. **Multi-Environment Configuration** - Dev/staging/prod separation

### Top Best Practices

1. **Start Simple, Evolve** - Begin with one file, refactor as you grow
2. **Test Assets Like Python Functions** - Use materialize() with mocks
3. **Use EnvVar for Secrets** - Runtime evaluation, not visible in UI
4. **Add Rich Metadata** - Help future you understand what happened
5. **Implement CI/CD with Branch Deployments** - Preview changes safely
6. **Monitor Resource Usage** - Especially code location memory

### Top Anti-Patterns to Avoid

1. **Module-Level Heavy Imports** - Load on demand, not at import time
2. **Too Many Code Locations** - Consolidate when possible
3. **Excessive Partitions** - Keep under 100K per asset
4. **Monolithic Assets** - Break into focused, reusable pieces
5. **Hardcoded Configuration** - Use resources and environment variables
6. **No Data Quality Checks** - Validate data, don't propagate bad data

### Additional Resources

- **Official Docs:** https://docs.dagster.io
- **Dagster University:** https://courses.dagster.io (Free courses)
- **Community:** https://discuss.dagster.io
- **GitHub:** https://github.com/dagster-io/dagster
- **Blog:** https://dagster.io/blog

---

**Document Version:** 1.0
**Research Date:** November 18, 2025
**Sources:** Official Dagster documentation, community discussions, production case studies
