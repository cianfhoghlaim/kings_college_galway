# Dagster Terminology, Ontology, and Conceptual Model Research

**Research Date:** 2025-11-18
**Current Stable Version:** Dagster 1.12.2 (Released November 13, 2025)
**Python Support:** Python 3.10 - 3.13
**Status:** Production/Stable (Development Status: 5)

---

## Table of Contents
1. [Core Terminology](#core-terminology)
2. [Data Model](#data-model)
3. [API Structure](#api-structure)
4. [Integration Model](#integration-model)
5. [Conceptual Relationships](#conceptual-relationships)

---

## 1. Core Terminology

### 1.1 Asset
**Definition:** A logical unit of data such as a table, dataset, ML model, or any persisted object that you want to keep track of.

**Key Characteristics:**
- Assets are the core abstraction in modern Dagster
- Can have dependencies on other assets, forming data lineage
- Behind the scenes, the Python function in an asset is an op
- Represents the "what" of your data pipeline (what data exists)

**Relationships:**
- Assets depend on other assets (upstream/downstream dependencies)
- Assets are materialized through execution
- Assets can be partitioned
- Assets can have checks associated with them

**Example:**
```python
@asset
def my_table(context: AssetExecutionContext):
    # Computation that produces data
    return some_data
```

---

### 1.2 Materialization
**Definition:** The act of running an asset's function and saving the results to persistent storage.

**Key Characteristics:**
- Encompasses the entire lifecycle from asset definition through execution to event logging
- Generates `AssetMaterialization` events in Dagster's event log
- Can be triggered from the Dagster UI or via Python APIs
- Recorded in the asset catalog with metadata

**Related Concepts:**
- **Asset Observation:** Records metadata about an asset without mutating it
- **Rematerialization:** Re-running an asset's computation to update its value

---

### 1.3 Op (Operation)
**Definition:** A computational unit of work representing the smallest unit of computation in Dagster.

**Key Characteristics:**
- Core unit of computation in Dagster
- Arranged into a `GraphDefinition` to dictate execution order
- Can be composed into graphs
- Legacy approach; assets are now preferred for most use cases

**Relationships:**
- Ops are composed into graphs
- Graphs are executed as jobs
- Assets are implemented as ops under the hood

**Example:**
```python
@op
def process_data(context: OpExecutionContext, input_data):
    # Computation logic
    return processed_data
```

**When to Use:**
- Managing existing ops in legacy codebases
- Complex use cases requiring fine-grained control
- For new projects, assets are strongly recommended

---

### 1.4 Graph
**Definition:** A collection of ops or nested graphs connected via dependencies, representing the structure of computation.

**Key Characteristics:**
- Defines the dependency structure between ops
- Supports arbitrary nesting levels
- Can contain both ops and other graphs as nodes
- Created using `@graph` decorator or `GraphDefinition` class

**Composition:**
- **Input Mappings:** Define graph inputs and how they map to constituent ops
- **Output Mappings:** Define graph outputs and how they map from constituent ops
- **Dependencies:** Declare how op inputs depend on other op outputs

**Example:**
```python
@graph
def data_pipeline():
    raw = extract_data()
    transformed = transform_data(raw)
    load_data(transformed)
```

---

### 1.5 Job
**Definition:** The main unit of execution and monitoring in Dagster, representing an executable subset of assets or a graph of ops.

**Key Characteristics:**
- Jobs are the main form of execution in Dagster
- Can execute asset subsets or op graphs
- Can be triggered by schedules, sensors, or manually
- Produces runs when executed

**Types:**
- **Asset Jobs:** Execute a selection of assets
- **Op Jobs:** Execute a graph of ops

**Example:**
```python
@job
def my_job():
    op1()
    op2()

# Or for assets
asset_job = define_asset_job("my_asset_job", selection="my_asset*")
```

---

### 1.6 Run
**Definition:** A single execution instance of a job.

**Status Lifecycle:**
1. **STARTING:** Run is launched, waiting for run worker to spin up
2. **STARTED:** Run worker has marked the run as started
3. **SUCCESS:** Run completed successfully (DagsterRunStatus.SUCCESS)
4. **FAILED:** Run failed during execution
5. **CANCELING:** Run is being terminated
6. **CANCELED:** Run has been terminated and cleaned up

**Timeout Parameters:**
- `start_timeout_seconds`: Max time in STARTING before marked as failed
- `cancel_timeout_seconds`: Max time in CANCELING before marked as canceled

**Related Concepts:**
- **Run Request:** Object returned by schedules/sensors to trigger a run
- **Run Status Sensor:** Monitors runs and triggers actions on status changes
- **Run Monitoring:** Daemon that detects and manages crashed workers

---

### 1.7 Partition
**Definition:** A logical division of an asset or job's data, typically based on time windows or categorical dimensions.

**Key Characteristics:**
- Enables incremental processing of data
- Supports time-based and categorical partitioning
- Can be multi-dimensional
- Enables targeted backfills

**Partition Types:**
- **Time Window Partitions:** Daily, hourly, monthly, etc.
- **Static Partitions:** Fixed set of partition keys
- **Dynamic Partitions:** Partition keys determined at runtime
- **Multi-Partitions:** Multiple dimensions (e.g., date + region)

**Partition Dependencies:**
- Same partition dependencies (default for matching PartitionsDefinitions)
- Time window intersections (for time-partitioned assets)
- Custom partition mappings (via PartitionMapping)
- Self-dependencies (asset depends on its own earlier partitions)

---

### 1.8 Backfill
**Definition:** The process of running partitions for assets that either don't exist or updating existing records with new logic.

**Types:**
- **Multi-Run Backfills (Default):** N partitions = N separate runs
- **Single-Run Backfills:** Execute all partitions in one run (e.g., single SQL query)

**BackfillPolicy:**
- Specifies how Dagster should backfill a partitioned asset
- Can optimize for performance or granularity
- Configurable per asset

**Common Use Cases:**
- Initial setup of pipelines with historical data
- Updating historical data after logic changes
- Fixing data quality issues retroactively

---

### 1.9 Schedule
**Definition:** A time-based trigger that creates runs on a regular cadence, defined using cron syntax.

**Key Characteristics:**
- Defined with `@schedule` decorator
- Takes `ScheduleEvaluationContext` as parameter
- Returns `RunRequest` objects or `SkipReason`
- Can access resources for external service calls
- Evaluated by Dagster daemon

**Example:**
```python
@schedule(job=my_job, cron_schedule="0 0 * * *")
def daily_schedule(context: ScheduleEvaluationContext):
    return RunRequest(run_config={...})
```

---

### 1.10 Sensor
**Definition:** An event-driven trigger that evaluates a condition and creates runs based on external events.

**Key Characteristics:**
- Defined with `@sensor` decorator
- Polls at a specified interval
- Can monitor assets, files, external systems, or run status
- Returns `RunRequest` or `SkipReason`

**Sensor Types:**
- **Asset Sensors:** Monitor asset materializations
- **Run Status Sensors:** Monitor run completion/failure
- **Custom Sensors:** Monitor arbitrary conditions

**Example:**
```python
@asset_sensor(asset_key=AssetKey("upstream_asset"), job=my_job)
def my_asset_sensor(context, asset_event):
    return RunRequest(run_key=context.cursor)
```

---

### 1.11 Resource
**Definition:** An external service, connection, or configuration made available to ops, assets, schedules, and sensors during execution.

**Key Characteristics:**
- Scoped way to make external resources available
- Defined with `@resource` decorator
- Takes `InitResourceContext` as parameter
- Supports lifecycle management (setup/teardown)
- Can be mocked for testing

**Modern Pythonic Config:**
- Resources can now be defined using Python dataclasses
- Pydantic validation under the hood
- Standardizes connections across all Dagster definitions

**Example:**
```python
@resource
def database_connection(init_context: InitResourceContext):
    conn = create_connection(init_context.resource_config["connection_string"])
    yield conn
    conn.close()
```

---

### 1.12 IO Manager
**Definition:** A component that handles reading and writing data for assets and ops, separating data processing logic from storage operations.

**Key Characteristics:**
- Subclasses must implement `handle_output` and `load_input` methods
- Reduces code redundancy across assets
- Makes storage configurations flexible across environments
- Can be extended as `ConfigurableIOManager` for config schemas

**Built-in IO Managers:**
- Filesystem IO Manager (using pickling)
- Cloud storage managers (S3, GCS, Azure Blob)
- Database managers (via integration libraries)

**When to Use:**
- Repeated read/write patterns across assets
- Standardized path structures for multiple assets
- Different storage needs across environments (local, staging, prod)
- Dependencies need to be loaded into memory before computation

**Example:**
```python
class MyIOManager(IOManager):
    def handle_output(self, context, obj):
        # Write obj to storage
        write_to_storage(context.asset_key, obj)

    def load_input(self, context):
        # Read from storage
        return read_from_storage(context.asset_key)
```

---

### 1.13 Code Location
**Definition:** A collection of Dagster definitions loadable and accessible by Dagster's tools (CLI, UI, Dagster+).

**Structure:**
- Reference to a Python module containing a `Definitions` instance
- Python environment that can successfully load that module
- Loaded in separate process, communicates via RPC

**Key Rules:**
- Only ONE `Definitions` object per code location
- `Definitions` must be a top-level variable
- Multiple code locations can exist per Dagster instance

**Benefits:**
- Code location updates picked up without webserver restart
- Different code locations can have separate Python environments
- Enables team-based organization with dependency isolation

---

### 1.14 Definitions
**Definition:** A central registry object that encapsulates all Dagster definitions (assets, jobs, schedules, sensors, resources).

**Key Characteristics:**
- Replaces the legacy "repository" concept
- Must be a singleton per code location
- Must be available as top-level variable
- Acts as the entry point for Dagster tools

**Example:**
```python
from dagster import Definitions

defs = Definitions(
    assets=[asset1, asset2],
    jobs=[job1, job2],
    schedules=[schedule1],
    sensors=[sensor1],
    resources={"db": database_resource}
)
```

---

## 2. Data Model

### 2.1 Event System
**Definition:** A structured stream of metadata events emitted during pipeline execution, forming an immutable log of all system activity.

**Key Characteristics:**
- Events are immutable once written
- Stored in Dagster's event log storage
- Available for querying and visualization
- Some system-generated, some user-provided

**Event Categories:**
1. **System Events:** Automatically emitted by Dagster
   - Op start/completion
   - Step execution events
   - Engine events

2. **User Events:** Explicitly yielded by user code
   - Asset materializations
   - Asset observations
   - Expectation results
   - Custom metadata

---

### 2.2 Event Types

#### AssetMaterialization
**Purpose:** Records that a data asset has been written to external storage.

**Characteristics:**
- Automatically generated for assets
- Can be manually yielded from ops
- Records metadata about the materialized asset
- Tracked in asset catalog
- Can trigger asset sensors

**Example:**
```python
yield AssetMaterialization(
    asset_key="my_table",
    metadata={
        "num_rows": 1000,
        "schema_version": "v2"
    }
)
```

---

#### AssetObservation
**Purpose:** Records metadata about an asset without indicating mutation.

**Use Cases:**
- Monitoring external tables not managed by Dagster
- Recording data quality metrics
- Tracking asset freshness
- Observing schema changes

**Difference from Materialization:**
- Does NOT indicate the asset was written
- Used for passive monitoring
- Does NOT trigger asset sensors by default

---

#### Output
**Purpose:** Passes data from one op to another, the most critical event for Dagster functionality.

**Characteristics:**
- Enables data flow between ops
- Can include metadata
- Supports conditional branching
- Type annotations maintained

**Enhanced with Output Object:**
```python
yield Output(
    value=my_data,
    metadata={
        "row_count": len(my_data)
    }
)
```

---

#### AssetCheckResult
**Purpose:** Returns the result of a data quality check on an asset (modern approach, replacing ExpectationResult).

**Characteristics:**
- Returned from `@asset_check` decorated functions
- Operates on specific assets
- Flexibly schedulable
- Integrated into asset catalog

**Example:**
```python
@asset_check(asset=my_asset)
def check_row_count(asset_materialization):
    return AssetCheckResult(
        passed=row_count > 0,
        metadata={"row_count": row_count}
    )
```

---

#### ExpectationResult
**Purpose:** DEPRECATED - Records data quality test results (replaced by AssetCheckResult).

**Status:**
- Will be removed in Dagster 2.0
- Use `AssetCheckResult` and `@asset_check` instead for assets

---

#### DynamicOutput
**Purpose:** Represents one item in a set for dynamic fan-out operations.

**Characteristics:**
- Must have unique `mapping_key`
- Enables dynamic parallelism
- Must be used with `map()` or `collect()`

**Pattern:**
- **Fan-out:** Use `.map()` to process each dynamic output
- **Fan-in:** Use `.collect()` to gather results

**Example:**
```python
@op(out=DynamicOut())
def dynamic_op():
    for i in range(10):
        yield DynamicOutput(i, mapping_key=str(i))

@job
def dynamic_job():
    results = dynamic_op().map(process_item)
    final = aggregate(results.collect())
```

---

### 2.3 Asset Catalog

**Definition:** A centralized registry of all data assets tracked by Dagster, automatically populated and always synchronized with pipelines.

**Key Features:**
- **Automatic Population:** Assets registered through code
- **Metadata Storage:** Columns, row counts, schema versions, custom metadata
- **Lineage Tracking:** Visual dependency graph
- **Run History:** All materializations with timestamps
- **Partitions View:** Status of partitioned assets
- **Asset Checks:** Data quality check results

**Integration with dbt:**
- dbt models automatically appear as assets
- Metadata from dbt manifest included
- Lineage preserved from dbt DAG

**Benefits:**
- Single place to investigate issues
- Metadata, lineage, and run status connected
- Always up to date (part of orchestration layer)

---

### 2.4 Pipeline Representation

**Hierarchical Structure:**
```
Definitions (Code Location)
├── Assets
│   ├── Dependencies (AssetDeps)
│   ├── Partitions (PartitionsDefinition)
│   └── Checks (AssetChecks)
├── Jobs
│   ├── Asset Jobs (asset selections)
│   └── Op Jobs (graphs)
├── Graphs
│   ├── Ops (compute units)
│   └── Nested Graphs
├── Schedules
├── Sensors
└── Resources
```

**Asset-First Model:**
- Modern Dagster centers on assets (the "what")
- Jobs and graphs are execution mechanisms (the "how")
- Lineage automatically derived from asset dependencies
- UI emphasizes asset catalog over job runs

---

### 2.5 Metadata Schema

**Metadata Types:**
1. **Text:** String values
2. **URL:** Links to external resources
3. **Path:** File system paths
4. **JSON:** Structured data
5. **Markdown:** Rich text documentation
6. **Float/Int:** Numeric metrics
7. **Bool:** Boolean flags
8. **Table:** Structured table data
9. **Asset:** References to other assets

**Metadata Locations:**
- Asset definitions (static metadata)
- Materialization events (runtime metadata)
- Asset observations
- Asset checks
- Op outputs

**Column-Level Lineage:**
- Track dependencies at column level
- Understand how columns are created and used
- Available for database table assets
- Improves collaboration and debugging

---

## 3. API Structure

### 3.1 Decorators

#### @asset
**Purpose:** Define a software-defined asset.

**Parameters:**
- `name`: Asset name (defaults to function name)
- `deps`: Upstream asset dependencies (without loading data)
- `ins`: Input definitions for loading upstream assets
- `config_schema`: Configuration schema
- `required_resource_keys`: Resources needed
- `partitions_def`: Partition definition
- `metadata`: Static metadata
- `io_manager_key`: IO manager to use
- `compute_kind`: Display tag (e.g., "SQL", "Python")
- `code_version`: Version tracking for caching

**Example:**
```python
@asset(
    deps=["raw_data"],
    config_schema={"threshold": int},
    required_resource_keys={"database"},
    partitions_def=DailyPartitionsDefinition(start_date="2024-01-01"),
    compute_kind="Python"
)
def processed_data(context: AssetExecutionContext, raw_data):
    threshold = context.op_config["threshold"]
    # Process data
    return result
```

---

#### @op
**Purpose:** Create an operation/computation unit.

**Parameters:**
- `name`: Op name
- `ins`: Input definitions with types
- `out`: Output definition(s)
- `config_schema`: Configuration schema
- `required_resource_keys`: Resources needed
- `tags`: Metadata tags

**Example:**
```python
@op(
    config_schema={"iterations": int},
    out={"result": Out(), "metrics": Out()}
)
def complex_computation(context: OpExecutionContext, input_data):
    # Computation
    return result, metrics
```

---

#### @graph
**Purpose:** Compose ops into a reusable graph structure.

**Characteristics:**
- Can be nested within other graphs
- Doesn't execute directly (needs to be part of a job)
- Defines computational structure

**Example:**
```python
@graph
def etl_pipeline():
    raw = extract()
    cleaned = clean(raw)
    transformed = transform(cleaned)
    return load(transformed)
```

---

#### @job
**Purpose:** Create an executable job from ops/graphs or configure an asset job.

**Parameters:**
- `name`: Job name
- `config`: Run configuration
- `resource_defs`: Resource definitions
- `hooks`: Success/failure hooks
- `tags`: Metadata tags
- `description`: Job description

**Example:**
```python
@job(
    resource_defs={"db": database_resource},
    hooks={my_failure_hook}
)
def data_pipeline():
    load(transform(extract()))
```

---

#### @resource
**Purpose:** Define a resource for external services/connections.

**Parameters:**
- `config_schema`: Resource configuration schema
- `description`: Resource description
- `required_resource_keys`: Nested resource dependencies

**Example:**
```python
@resource(config_schema={"connection_string": str})
def database(init_context: InitResourceContext):
    conn = connect(init_context.resource_config["connection_string"])
    try:
        yield conn
    finally:
        conn.close()
```

---

#### @schedule
**Purpose:** Create a time-based schedule for a job.

**Parameters:**
- `job`: Job to execute
- `cron_schedule`: Cron expression
- `execution_timezone`: Timezone for schedule
- `default_status`: RUNNING or STOPPED

**Example:**
```python
@schedule(
    job=my_job,
    cron_schedule="0 9 * * MON-FRI",
    execution_timezone="America/New_York"
)
def business_hours_schedule(context: ScheduleEvaluationContext):
    return RunRequest(
        run_key=context.scheduled_execution_time.isoformat(),
        run_config={"ops": {"process": {"config": {"date": context.scheduled_execution_time}}}}
    )
```

---

#### @sensor
**Purpose:** Create an event-driven sensor.

**Parameters:**
- `job`: Job to execute
- `minimum_interval_seconds`: Polling interval
- `description`: Sensor description
- `default_status`: RUNNING or STOPPED

**Example:**
```python
@sensor(job=my_job, minimum_interval_seconds=60)
def file_sensor(context: SensorEvaluationContext):
    if new_file_exists():
        return RunRequest(run_key=get_file_hash())
    return SkipReason("No new files")
```

---

#### @asset_check
**Purpose:** Define a data quality check for an asset.

**Parameters:**
- `asset`: Asset to check
- `name`: Check name
- `description`: Check description
- `required_resource_keys`: Resources needed

**Example:**
```python
@asset_check(asset=my_table, name="row_count_positive")
def check_has_rows(context):
    row_count = query_row_count()
    return AssetCheckResult(
        passed=row_count > 0,
        metadata={"row_count": row_count}
    )
```

---

#### @success_hook / @failure_hook
**Purpose:** Define success/failure handling for ops.

**Parameters:**
- `required_resource_keys`: Resources needed (e.g., Slack, PagerDuty)

**Example:**
```python
@failure_hook(required_resource_keys={"slack"})
def notify_failure(context: HookContext):
    context.resources.slack.send_message(
        f"Op {context.op.name} failed: {context.op_exception}"
    )
```

**Application:**
```python
@job(hooks={notify_failure})
def monitored_job():
    my_op()
```

---

### 3.2 Context Objects

#### OpExecutionContext
**Purpose:** Provides system information to ops during execution.

**Key Properties/Methods:**
- `log`: DagsterLogManager for logging
- `resources`: Access to configured resources
- `op_config`: Configuration for the op
- `run_id`: Unique identifier for the run
- `run`: Run object with metadata
- `instance`: DagsterInstance for querying storage
- `partition_key`: Partition being processed (if partitioned)
- `asset_key`: Asset key (for asset-backed ops)

**Construction for Testing:**
```python
context = build_op_context(
    resources={"db": mock_db},
    config={"threshold": 10}
)
```

---

#### AssetExecutionContext
**Purpose:** Context object specifically for assets (subtype of OpExecutionContext).

**Rationale:**
- Exposes only fields relevant to assets
- Hides op implementation details
- Cleaner API surface for asset authors

**Additional Asset-Specific Properties:**
- `asset_key`: The asset being materialized
- `asset_partition_key`: Current partition
- `asset_partition_key_range`: Range for multi-partition runs
- `asset_partitions_def`: Partition definition

**Relationship:**
- `AssetExecutionContext` is a subtype of `OpExecutionContext`
- Underlying `OpExecutionContext` used when needed
- Recommended type annotation for asset functions

**Construction for Testing:**
```python
context = build_asset_context(
    resources={"db": mock_db},
    partition_key="2024-01-01"
)
```

---

#### InitResourceContext
**Purpose:** Context provided to resource initialization functions.

**Key Properties:**
- `resource_config`: Configuration for the resource
- `resources`: Access to other resources (for dependencies)
- `log`: Logger
- `instance`: DagsterInstance

---

#### HookContext
**Purpose:** Context provided to hook functions.

**Key Properties:**
- `log`: Logger
- `op`: Op that succeeded/failed
- `op_exception`: Exception object (for failure hooks)
- `resources`: Access to resources
- `run_id`: Current run ID

---

#### ScheduleEvaluationContext
**Purpose:** Context for schedule evaluation.

**Key Properties:**
- `scheduled_execution_time`: When the schedule tick occurred
- `instance`: DagsterInstance
- `resources`: Access to resources
- `log`: Logger
- `cursor`: Persistent state (optional)

---

#### SensorEvaluationContext
**Purpose:** Context for sensor evaluation.

**Key Properties:**
- `cursor`: Persistent state for tracking progress
- `last_run_key`: Key of last run launched
- `resources`: Access to resources
- `instance`: DagsterInstance
- `log`: Logger

---

### 3.3 Configuration APIs

**Modern Pythonic Config:**
```python
from dagster import Config

class MyOpConfig(Config):
    threshold: int
    mode: str = "standard"

@op
def my_op(context: OpExecutionContext, config: MyOpConfig):
    if config.threshold > 10:
        # ...
```

**Legacy Config Schema:**
```python
@op(config_schema={"threshold": int, "mode": str})
def my_op(context: OpExecutionContext):
    threshold = context.op_config["threshold"]
```

**Run Configuration:**
```python
run_config = {
    "ops": {
        "my_op": {
            "config": {"threshold": 10}
        }
    },
    "resources": {
        "database": {
            "config": {"connection_string": "..."}
        }
    }
}
```

---

### 3.4 Testing APIs

#### Direct Asset/Op Invocation
```python
# Assets can be invoked directly
result = my_asset(context=build_asset_context())

# Same for ops
result = my_op(context=build_op_context())
```

---

#### Mocking Resources
**Using mock.Mock:**
```python
from unittest.mock import Mock

mock_db = Mock(spec=DatabaseResource)
mock_db.query.return_value = [...]

context = build_asset_context(resources={"db": mock_db})
result = my_asset(context)
```

**Using ResourceDefinition.mock_resource:**
```python
from dagster import ResourceDefinition

mock_resource = ResourceDefinition.mock_resource()
```

---

#### Context Builders
- `build_op_context()`: Create OpExecutionContext for testing
- `build_asset_context()`: Create AssetExecutionContext for testing
- `build_init_resource_context()`: Create InitResourceContext for testing

---

#### Execution Helpers
```python
# Execute a job
result = my_job.execute_in_process(
    resources={"db": mock_db},
    run_config={...}
)

# Materialize assets
from dagster import materialize

result = materialize(
    [asset1, asset2],
    resources={"db": mock_db}
)
```

---

### 3.5 GraphQL API

**Endpoint:** `/graphql` (e.g., `http://localhost:3000/graphql`)

**Key Capabilities:**
- Query runs, jobs, assets, and schedules
- Retrieve metadata and dependency structures
- Launch job executions and re-executions
- Query config schemas

**GraphQL Playground:**
- Access at `/graphql` in browser
- Interactive schema exploration
- Query building and testing

**Python Client:**
```python
from dagster_graphql import DagsterGraphQLClient

client = DagsterGraphQLClient("localhost", port_number=3000)
# Or for Dagster+
client = DagsterGraphQLClient("YOUR_ORG.dagster.cloud")
```

**Python Equivalents (Preferred Inside Executions):**
- Use `context.instance` when inside op/asset/schedule/sensor
- Methods: `instance.get_runs()`, `instance.get_asset_records()`
- Avoid GraphQL for internal queries; use instance API

**Configuration Schema:**
- `RunConfigData`: Must conform to job's config schema
- Validation errors return `RunConfigValidationInvalid`

---

## 4. Integration Model

### 4.1 Plugin Architecture

**Core Concept:** Dagster extends to external services via integration libraries that provide specialized components and resources.

**Integration Pattern:**
- Each integration is a separate Python package (e.g., `dagster-dbt`, `dagster-aws`)
- Integrations provide:
  - Custom resource definitions
  - Specialized decorators/components
  - Pre-built ops/assets
  - IO managers for specific systems

**Maintained Integrations:**
- AWS (dagster-aws)
- Google Cloud Platform (dagster-gcp)
- Azure (dagster-azure)
- dbt (dagster-dbt)
- Databricks (dagster-databricks)
- Snowflake (dagster-snowflake)
- Fivetran (dagster-fivetran)
- Airbyte (dagster-airbyte)
- Great Expectations (dagster-ge)
- Pandas (dagster-pandas)
- Spark (dagster-spark)
- PySpark (dagster-pyspark)
- Datadog (dagster-datadog)
- And many more...

---

### 4.2 dbt Integration (dagster-dbt)

**Component-Based Approach:**
```python
from dagster_dbt import DbtProjectComponent

dbt_project = DbtProjectComponent(
    project_dir="path/to/dbt_project",
    manifest="path/to/manifest.json"
)

defs = Definitions(
    assets=dbt_project.assets,
    ...
)
```

**Key Features:**
- **Asset-Level Understanding:** Each dbt model becomes a Dagster asset
- **Automatic Lineage:** dbt DAG preserved in Dagster asset graph
- **Metadata Integration:** dbt metadata (columns, tests) in Dagster catalog
- **Mixed Orchestration:** Combine dbt with Spark, Python, etc.
- **Automatic Events:** AssetMaterialization events on dbt model runs

**@dbt_assets Decorator:**
```python
from dagster_dbt import dbt_assets

@dbt_assets(manifest=manifest_json)
def my_dbt_models(context: AssetExecutionContext, dbt: DbtCliResource):
    yield from dbt.cli(["build"], context=context).stream()
```

**Customization:**
- Create subclass of `DbtProjectComponent` for custom behavior
- Override execution methods
- Add custom metadata
- Customize op configuration

---

### 4.3 Cloud Platform Integrations

**AWS (dagster-aws):**
- S3 IO Manager
- Secrets Manager integration
- ECS/Fargate execution
- EMR for Spark
- Redshift resources

**GCP (dagster-gcp):**
- GCS IO Manager
- BigQuery resources
- Cloud Run execution
- Dataproc for Spark

**Azure (dagster-azure):**
- Blob Storage IO Manager
- Azure Data Lake integration
- Synapse resources

---

### 4.4 Data Processing Integrations

**Spark/PySpark:**
- SparkResource
- PySpark ops
- DataFrame transformations
- Cluster management

**Pandas:**
- DataFrame type system
- Validation decorators
- Summary statistics

**Polars:**
- DataFrame integration
- Type checking

---

### 4.5 Data Observability Integrations

**Datadog:**
- Metrics export
- Event logging
- APM integration

**Great Expectations:**
- Expectation suites as asset checks
- Validation results as events

---

### 4.6 Integration Patterns

**Software-Defined Assets Framework:**
- All integrations align with asset-first model
- External tools represented as assets
- Lineage automatically tracked
- Single orchestration layer

**Resource Pattern:**
```python
from dagster_aws.s3 import S3Resource

defs = Definitions(
    assets=[...],
    resources={
        "s3": S3Resource(region="us-west-2")
    }
)
```

**Extending Dagster:**
- Create custom resources by implementing `@resource`
- Create custom IO managers by extending `IOManager`
- Create custom integrations as Python packages
- Publish to PyPI for community use

---

## 5. Conceptual Relationships

### 5.1 Asset-Centric Data Model

```
Asset
  ├── Has Dependencies (other Assets)
  ├── Has Partitions (PartitionsDefinition)
  ├── Has Checks (AssetChecks)
  ├── Produces Materializations (events)
  ├── Can be Observed (AssetObservation events)
  ├── Belongs to Code Location
  ├── Uses IO Manager (for storage)
  ├── Uses Resources (for external services)
  └── Implemented as Op (under the hood)
```

### 5.2 Execution Hierarchy

```
Code Location (Process boundary)
  └── Definitions (Singleton registry)
       ├── Assets
       │    └── Implicitly create Asset Jobs
       ├── Jobs
       │    ├── Asset Jobs (asset selection)
       │    └── Op Jobs (graph execution)
       │         └── Graphs
       │              └── Ops (leaves) / Graphs (nested)
       ├── Schedules (trigger Jobs on time)
       ├── Sensors (trigger Jobs on events)
       └── Resources (available to all)

Execution:
  Schedule/Sensor/Manual → Job → Run → Steps → Events
```

### 5.3 Event Flow

```
Job Execution
  └── Run (STARTING → STARTED → SUCCESS/FAILED)
       └── Op/Asset Execution
            ├── System Events (step start/end)
            ├── User Events
            │    ├── Output (data flow)
            │    ├── AssetMaterialization
            │    ├── AssetObservation
            │    ├── AssetCheckResult
            │    └── Metadata
            └── Event Log (immutable storage)
                 └── Asset Catalog (queryable view)
```

### 5.4 Data Flow Patterns

**Asset Pattern (Modern):**
```
Asset A → Asset B → Asset C
  (automatic lineage, implicit data passing via IO Manager)
```

**Op Pattern (Legacy):**
```
Op A → (Output) → Op B → (Output) → Op C
  (explicit data passing via function returns)
```

**Dynamic Pattern:**
```
Op A → DynamicOut[0..N] → Op B.map() → N parallel executions
                        → Op C.collect() → single result
```

### 5.5 Partitioning Model

```
Partitioned Asset
  ├── PartitionsDefinition
  │    ├── TimeWindowPartitionsDefinition (daily, hourly, etc.)
  │    ├── StaticPartitionsDefinition (fixed keys)
  │    ├── DynamicPartitionsDefinition (runtime keys)
  │    └── MultiPartitionsDefinition (e.g., date × region)
  │
  ├── Partition Mappings (dependency rules)
  │    ├── IdentityPartitionMapping (default, same key)
  │    ├── TimeWindowPartitionMapping (with offsets)
  │    └── MultiToSingleDimensionPartitionMapping
  │
  └── Backfill Strategies
       ├── Multi-Run (default, N partitions = N runs)
       └── Single-Run (all partitions in one run)
```

### 5.6 Code Organization

```
Project
  ├── __init__.py
  ├── assets/
  │    ├── raw_data.py (extract assets)
  │    ├── staging.py (transform assets)
  │    └── marts.py (load assets)
  ├── resources/
  │    ├── database.py
  │    └── apis.py
  ├── jobs/
  │    └── adhoc_jobs.py (op-based jobs)
  ├── schedules/
  │    └── daily_schedules.py
  ├── sensors/
  │    └── file_sensors.py
  └── definitions.py
       └── Definitions object (entry point)
```

### 5.7 Testing Model

```
Unit Testing
  ├── Direct Invocation
  │    ├── asset_function(build_asset_context())
  │    └── op_function(build_op_context())
  │
  ├── Mocked Resources
  │    ├── Mock(spec=ResourceClass)
  │    └── ResourceDefinition.mock_resource()
  │
  └── Isolated Context
       └── build_*_context(resources={...}, config={...})

Integration Testing
  ├── materialize([assets], resources={...})
  └── job.execute_in_process(resources={...})

Production Testing
  ├── Asset Checks (@asset_check)
  └── Sensors (monitoring and alerting)
```

---

## Summary: Dagster's Semantic Model

**Core Philosophy:** Dagster is an **orchestrator that thinks in terms of data assets** rather than just tasks or jobs. The semantic model reflects this:

1. **Assets First:** The primary abstraction is the data asset (what you're producing), not the job (how you produce it).

2. **Declarative Lineage:** Dependencies are declared, and Dagster automatically builds the execution graph and tracks lineage.

3. **Event-Driven Observability:** Everything produces events that form an immutable audit log, enabling rich debugging and monitoring.

4. **Unified Catalog:** All assets live in a single catalog that's always in sync with code, making data discovery natural.

5. **Flexible Execution:** The same asset definitions can be executed on different schedules, partitions, or subsets without changing code.

6. **Testing-First:** Every component can be tested in isolation with built-in mocking and context builders.

7. **Integration-Friendly:** Plugin architecture allows seamless integration with the modern data stack while maintaining consistent abstractions.

8. **Type Safety:** Strong type system with Pydantic-based config and Python type hints throughout.

**Key Innovation:** Dagster bridges the gap between **data engineering** (managing data assets and their relationships) and **orchestration** (scheduling and executing computations), treating them as unified concerns rather than separate domains.

---

## References

- **Official Documentation:** https://docs.dagster.io/
- **API Reference:** https://docs.dagster.io/api
- **GitHub Repository:** https://github.com/dagster-io/dagster
- **PyPI Package:** https://pypi.org/project/dagster/
- **Changelog:** https://docs.dagster.io/about/changelog
- **GraphQL API:** https://docs.dagster.io/concepts/webserver/graphql

**Research Compiled:** 2025-11-18
**Based on:** Dagster 1.12.2 (latest stable as of November 2025)
