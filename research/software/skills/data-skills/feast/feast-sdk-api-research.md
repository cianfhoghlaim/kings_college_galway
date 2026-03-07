# Feast SDK, APIs, and Ontologies - Comprehensive Research

## Table of Contents

1. [Python SDK - FeatureStore Class](#python-sdk---featurestore-class)
2. [Feature Retrieval APIs](#feature-retrieval-apis)
3. [Core Objects and Ontology](#core-objects-and-ontology)
4. [Type System](#type-system)
5. [Data Sources](#data-sources)
6. [Configuration](#configuration)
7. [Online Stores](#online-stores)
8. [Offline Stores](#offline-stores)
9. [CLI Commands](#cli-commands)

---

## Python SDK - FeatureStore Class

### Class Definition

```python
class feast.feature_store.FeatureStore(
    repo_path: Optional[str] = None,
    config: Optional[feast.repo_config.RepoConfig] = None
)
```

**Description**: A FeatureStore object is used to define, create, and retrieve features. It serves as the main entry point for all Feast operations.

### Core Methods

#### `apply()`

Registers objects to metadata store and updates related infrastructure.

```python
def apply(
    self,
    objects: Union[
        Entity,
        FeatureView,
        OnDemandFeatureView,
        StreamFeatureView,
        FeatureService,
        List[Union[Entity, FeatureView, OnDemandFeatureView, StreamFeatureView, FeatureService]]
    ],
    commit: bool = True
) -> None
```

**Parameters:**
- `objects`: One or more Feast objects to register
- `commit`: Whether to commit changes to the registry

**Description**: Registers one or more definitions (e.g., Entity, FeatureView) and updates these objects in the Feast registry. Once the registry has been updated, the apply method will update related infrastructure (e.g., create tables in an online store).

**Example:**
```python
from feast import FeatureStore, Entity, FeatureView, Field
from feast.types import Int64, Float32

fs = FeatureStore(repo_path=".")

driver = Entity(name="driver", join_keys=["driver_id"])
driver_stats = FeatureView(
    name="driver_stats",
    entities=[driver],
    schema=[
        Field(name="trips_today", dtype=Int64),
        Field(name="rating", dtype=Float32),
    ],
    source=parquet_source
)

fs.apply([driver, driver_stats])
```

---

#### `plan()`

Dry-runs registration and produces a list of changes.

```python
def plan(
    self,
    objects: List[Union[Entity, FeatureView, OnDemandFeatureView, StreamFeatureView, FeatureService]]
) -> Tuple[RegistryDiff, InfraDiff]
```

**Description**: Dry-runs registering one or more definitions and produces a list of all the changes that would be introduced in the feature repo. Changes are for informational purposes and not actually applied.

---

#### `get_historical_features()`

Retrieves historical feature values for training or batch scoring.

```python
def get_historical_features(
    self,
    entity_df: Union[pd.DataFrame, str],
    features: Union[List[str], FeatureService],
    full_feature_names: bool = False
) -> RetrievalJob
```

**Parameters:**
- `entity_df`: A DataFrame or SQL query containing entity keys and timestamps
- `features`: List of feature references or a FeatureService
- `full_feature_names`: If True, returns feature names prefixed with feature view name

**Returns**: `RetrievalJob` - Call `.to_df()` or `.to_arrow()` to get results

**Entity DataFrame Requirements:**
- Must include entity key columns (e.g., `driver_id`)
- Must include event timestamp column indicating when the event occurred

**Example:**
```python
from datetime import datetime
import pandas as pd

entity_df = pd.DataFrame.from_dict({
    "driver_id": [1001, 1002, 1003],
    "event_timestamp": [
        datetime(2021, 4, 12, 10, 59, 42),
        datetime(2021, 4, 12, 8, 12, 10),
        datetime(2021, 4, 12, 16, 40, 26),
    ]
})

training_df = fs.get_historical_features(
    entity_df=entity_df,
    features=[
        "driver_hourly_stats:trips_today",
        "driver_hourly_stats:conv_rate",
        "driver_hourly_stats:acc_rate"
    ]
).to_df()

# Using FeatureService
training_df = fs.get_historical_features(
    entity_df=entity_df,
    features=fs.get_feature_service("driver_activity")
).to_df()
```

---

#### `get_online_features()`

Retrieves the latest online feature data for real-time inference.

```python
def get_online_features(
    self,
    features: Union[List[str], FeatureService],
    entity_rows: List[Dict[str, Any]],
    full_feature_names: bool = False
) -> OnlineResponse
```

**Parameters:**
- `features`: List of feature references (format: `"feature_view:feature"`) or FeatureService
- `entity_rows`: List of dictionaries containing entity keys
- `full_feature_names`: If True, returns feature names prefixed with feature view name

**Returns**: `OnlineResponse` - Call `.to_dict()` or `.to_df()` to get results

**Note**: Unlike `get_historical_features`, entity_rows do not need timestamps since only the latest feature value per entity key is retrieved.

**Example:**
```python
# Using feature references
features = fs.get_online_features(
    features=[
        "driver_hourly_stats:conv_rate",
        "driver_hourly_stats:acc_rate",
        "driver_hourly_stats:trips_today"
    ],
    entity_rows=[
        {"driver_id": 1001},
        {"driver_id": 1002}
    ]
).to_dict()

# Using FeatureService
feature_service = fs.get_feature_service("driver_activity")
features = fs.get_online_features(
    features=feature_service,
    entity_rows=[{"driver_id": 1001}]
).to_dict()
```

---

#### `materialize()`

Materializes feature data from offline to online store.

```python
def materialize(
    self,
    start_date: datetime,
    end_date: datetime,
    feature_views: Optional[List[str]] = None
) -> None
```

**Parameters:**
- `start_date`: Start date for time range of data to materialize
- `end_date`: End date for time range of data to materialize
- `feature_views`: Optional list of feature view names to materialize (if not specified, materializes all)

**Example:**
```python
from datetime import datetime

fs.materialize(
    start_date=datetime(2021, 1, 1),
    end_date=datetime(2021, 12, 31),
    feature_views=["driver_hourly_stats"]
)
```

---

#### `materialize_incremental()`

Materializes incrementally from last materialization time.

```python
def materialize_incremental(
    self,
    end_date: datetime,
    feature_views: Optional[List[str]] = None
) -> None
```

**Parameters:**
- `end_date`: End date for materialization
- `feature_views`: Optional list of feature view names

**Description**: The start time is either the most recent end time of a prior materialization or `(now - ttl)` if no prior materialization exists.

---

#### `push()`

Pushes feature data to online and/or offline stores.

```python
def push(
    self,
    push_source_name: str,
    df: pd.DataFrame,
    to: PushMode = PushMode.ONLINE,
    allow_registry_cache: bool = True
) -> None
```

**Parameters:**
- `push_source_name`: Name of the push source
- `df`: DataFrame containing feature data (must include entity columns and timestamps)
- `to`: Target store(s) - `PushMode.ONLINE`, `PushMode.OFFLINE`, or `PushMode.ONLINE_AND_OFFLINE`
- `allow_registry_cache`: Whether to use cached registry

**Example:**
```python
from feast.data_source import PushMode
import pandas as pd

feature_data = pd.DataFrame({
    "driver_id": [1001, 1002],
    "trips_today": [5, 10],
    "event_timestamp": [datetime.now(), datetime.now()]
})

fs.push(
    push_source_name="driver_stats_push",
    df=feature_data,
    to=PushMode.ONLINE_AND_OFFLINE
)
```

---

#### `retrieve_online_documents()`

Vector similarity search for RAG applications.

```python
def retrieve_online_documents(
    self,
    features: List[str],
    query: str,
    top_k: int = 10
) -> OnlineResponse
```

**Example:**
```python
results = fs.retrieve_online_documents(
    features=["documents:embedding"],
    query="What is the biggest city in the USA?",
    top_k=5
).to_dict()
```

---

#### `get_feature_service()`

Retrieves a feature service by name.

```python
def get_feature_service(
    self,
    name: str,
    allow_cache: bool = False
) -> FeatureService
```

---

#### Additional FeatureStore Methods

| Method | Description |
|--------|-------------|
| `get_entity(name)` | Retrieves an entity by name |
| `get_feature_view(name)` | Retrieves a feature view by name |
| `get_on_demand_feature_view(name)` | Retrieves an on-demand feature view |
| `get_stream_feature_view(name)` | Retrieves a stream feature view |
| `list_entities()` | Lists all registered entities |
| `list_feature_views()` | Lists all registered feature views |
| `list_on_demand_feature_views()` | Lists all on-demand feature views |
| `list_stream_feature_views()` | Lists all stream feature views |
| `list_feature_services()` | Lists all feature services |
| `list_data_sources()` | Lists all data sources |
| `delete_feature_view(name)` | Deletes a feature view |
| `delete_feature_service(name)` | Deletes a feature service |
| `teardown()` | Tears down infrastructure |

---

## Feature Retrieval APIs

### RetrievalJob

Returned by `get_historical_features()`.

**Methods:**
- `.to_df()` - Returns a pandas DataFrame
- `.to_arrow()` - Returns a PyArrow Table
- `.to_sql()` - Returns SQL query (for SQL-backed offline stores)

### OnlineResponse

Returned by `get_online_features()`.

**Methods:**
- `.to_dict()` - Returns a dictionary
- `.to_df()` - Returns a pandas DataFrame

### Feature References

Feature references use the format: `<feature_view>:<feature>`

```python
features = [
    "driver_hourly_stats:trips_today",
    "driver_hourly_stats:conv_rate"
]
```

### Feature Server HTTP API

Start with: `feast serve`

**Endpoint**: `POST /get-online-features`

```python
import requests
import json

request = {
    "features": [
        "driver_hourly_stats:conv_rate",
        "driver_hourly_stats:trips_today"
    ],
    "entities": {
        "driver_id": [1001, 1002]
    }
}

response = requests.post(
    'http://localhost:6566/get-online-features',
    data=json.dumps(request)
)
```

---

## Core Objects and Ontology

### Entity

Defines a collection of semantically related features and serves as a primary key for feature retrieval.

```python
class feast.entity.Entity(
    *,
    name: str,
    join_keys: Optional[List[str]] = None,
    value_type: Optional[ValueType] = None,
    description: str = '',
    tags: Optional[Dict[str, str]] = None,
    owner: str = ''
)
```

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `name` | `str` | Unique name of the entity |
| `join_keys` | `List[str]` | List of properties that uniquely identify entities (currently supports size one) |
| `value_type` | `ValueType` | Type of entity (inferred from data source if not specified) |
| `description` | `str` | Human-readable description |
| `tags` | `Dict[str, str]` | Key-value pairs for arbitrary metadata |
| `owner` | `str` | Email of primary maintainer |

**Example:**
```python
from feast import Entity
from feast.value_type import ValueType

driver = Entity(
    name="driver",
    join_keys=["driver_id"],
    value_type=ValueType.INT64,
    description="Driver entity for ride-hailing service",
    tags={"team": "driver-features"},
    owner="ml-team@company.com"
)
```

---

### FeatureView

Defines a logical grouping of servable features.

```python
class feast.feature_view.FeatureView(
    *,
    name: str,
    entities: List[Entity] = [],
    ttl: Optional[timedelta] = None,
    source: DataSource = None,
    schema: Optional[List[Field]] = None,
    online: bool = True,
    description: str = '',
    tags: Optional[Dict[str, str]] = None,
    owner: str = ''
)
```

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `name` | `str` | Unique name of the feature view |
| `entities` | `List[Entity]` | List of entities (can be empty for global features) |
| `ttl` | `timedelta` | Time-to-live for features; `timedelta(0)` means infinite |
| `source` | `DataSource` | Data source (FileSource, BigQuerySource, etc.) |
| `schema` | `List[Field]` | List of Field definitions (inferred if not specified) |
| `online` | `bool` | Whether to materialize to online store |
| `description` | `str` | Human-readable description |
| `tags` | `Dict[str, str]` | Arbitrary metadata |
| `owner` | `str` | Email of primary maintainer |

**Example:**
```python
from feast import Entity, FeatureView, Field, FileSource
from feast.types import Int64, Float32
from datetime import timedelta

driver = Entity(name="driver", join_keys=["driver_id"])

driver_hourly_stats = FeatureView(
    name="driver_hourly_stats",
    entities=[driver],
    ttl=timedelta(hours=2),
    schema=[
        Field(name="trips_today", dtype=Int64),
        Field(name="conv_rate", dtype=Float32),
        Field(name="acc_rate", dtype=Float32),
    ],
    source=FileSource(
        path="data/driver_stats.parquet",
        timestamp_field="event_timestamp",
    ),
    online=True,
    description="Hourly driver statistics",
    tags={"team": "driver-features"},
    owner="ml-team@company.com"
)
```

**Feature View Without Entities (Global Features):**
```python
global_stats = FeatureView(
    name="global_stats",
    entities=[],
    schema=[
        Field(name="total_trips_today", dtype=Int64),
    ],
    source=BigQuerySource(table="project.dataset.global_stats")
)
```

---

### BatchFeatureView

Defines a logical group of features with only a batch data source.

```python
class feast.batch_feature_view.BatchFeatureView(
    *,
    name: str,
    entities: List[Entity] = [],
    ttl: Optional[timedelta] = None,
    source: DataSource,
    schema: Optional[List[Field]] = None,
    online: bool = True,
    description: str = '',
    tags: Optional[Dict[str, str]] = None,
    owner: str = ''
)
```

---

### StreamFeatureView

Handles both stream and batch data sources for fresher online features.

```python
class feast.stream_feature_view.StreamFeatureView(
    *,
    name: str,
    entities: List[Entity] = [],
    ttl: Optional[timedelta] = None,
    source: KafkaSource | KinesisSource,
    schema: Optional[List[Field]] = None,
    aggregations: Optional[List[Aggregation]] = None,
    mode: str = "spark",
    timestamp_field: str = "",
    online: bool = True,
    description: str = '',
    tags: Optional[Dict[str, str]] = None,
    owner: str = ''
)
```

---

### OnDemandFeatureView

Enables lightweight transformations at retrieval time.

```python
class feast.on_demand_feature_view.OnDemandFeatureView(
    *,
    name: str,
    schema: List[Field],
    sources: Dict[str, Union[FeatureView, RequestSource]],
    udf: Callable,
    description: str = '',
    tags: Optional[Dict[str, str]] = None,
    owner: str = ''
)
```

**Parameters:**
- `sources`: Map from source names to FeatureView or RequestSource
- `udf`: User-defined transformation function (must take pandas DataFrames as inputs)

**Example:**
```python
from feast import on_demand_feature_view, Field
from feast.types import Float32

@on_demand_feature_view(
    sources=[driver_hourly_stats],
    schema=[
        Field(name="conv_rate_plus_acc", dtype=Float32),
    ]
)
def transformed_conv_rate(inputs: pd.DataFrame) -> pd.DataFrame:
    df = pd.DataFrame()
    df["conv_rate_plus_acc"] = inputs["conv_rate"] + inputs["acc_rate"]
    return df
```

---

### Field

Defines feature schema with name and type.

```python
class feast.field.Field(
    *,
    name: str,
    dtype: FeastType,
    description: str = '',
    tags: Optional[Dict[str, str]] = None
)
```

**Example:**
```python
from feast import Field
from feast.types import Int64, Float32, String, Bool

fields = [
    Field(name="trips_today", dtype=Int64),
    Field(name="rating", dtype=Float32),
    Field(name="name", dtype=String),
    Field(name="is_active", dtype=Bool),
]
```

---

### FeatureService

Groups features from multiple feature views for a specific model.

```python
class feast.feature_service.FeatureService(
    *,
    name: str,
    features: List[Union[FeatureView, OnDemandFeatureView]],
    description: str = '',
    tags: Optional[Dict[str, str]] = None,
    owner: str = ''
)
```

**Example:**
```python
from feast import FeatureService

driver_activity_service = FeatureService(
    name="driver_activity",
    features=[
        driver_hourly_stats,
        driver_ratings[["lifetime_rating"]],  # Select specific features
    ],
    description="Features for driver activity model",
    owner="ml-team@company.com"
)
```

---

## Type System

### PrimitiveFeastType Enum

```python
class feast.types.PrimitiveFeastType(enum.Enum):
    INVALID = 0
    BYTES = 1
    STRING = 2
    INT32 = 3
    INT64 = 4
    FLOAT64 = 5
    FLOAT32 = 6
    BOOL = 7
    UNIX_TIMESTAMP = 8
```

### Type Aliases

Convenient module-level type aliases from `feast.types`:

| Alias | Type |
|-------|------|
| `Invalid` | `PrimitiveFeastType.INVALID` |
| `Bytes` | `PrimitiveFeastType.BYTES` |
| `String` | `PrimitiveFeastType.STRING` |
| `Bool` | `PrimitiveFeastType.BOOL` |
| `Int32` | `PrimitiveFeastType.INT32` |
| `Int64` | `PrimitiveFeastType.INT64` |
| `Float32` | `PrimitiveFeastType.FLOAT32` |
| `Float64` | `PrimitiveFeastType.FLOAT64` |
| `UnixTimestamp` | `PrimitiveFeastType.UNIX_TIMESTAMP` |

**Usage:**
```python
from feast.types import Int64, Float32, String, Bool, Bytes, UnixTimestamp
```

### Array Types

For list/array types, use the `Array` class:

```python
from feast.types import Array, Int64, Float32

schema = [
    Field(name="embedding", dtype=Array(Float32)),
    Field(name="tags", dtype=Array(String)),
]
```

### ValueType Enum (Legacy)

```python
class feast.value_type.ValueType(enum.Enum):
    UNKNOWN = 0
    BYTES = 1
    STRING = 2
    INT32 = 3
    INT64 = 4
    DOUBLE = 5
    FLOAT = 6
    BOOL = 7
    UNIX_TIMESTAMP = 8
    BYTES_LIST = 11
    STRING_LIST = 12
    INT32_LIST = 13
    INT64_LIST = 14
    DOUBLE_LIST = 15
    FLOAT_LIST = 16
    BOOL_LIST = 17
    UNIX_TIMESTAMP_LIST = 18
```

### Type Mapping (Python to Feast)

| Python/NumPy Type | Feast ValueType |
|-------------------|-----------------|
| `int` | `INT64` |
| `str` | `STRING` |
| `float` | `DOUBLE` |
| `bytes` | `BYTES` |
| `float64` | `DOUBLE` |
| `float32` | `FLOAT` |
| `int64` | `INT64` |
| `int32` | `INT32` |
| `bool` | `BOOL` |
| `boolean` | `BOOL` |
| `timedelta` | `UNIX_TIMESTAMP` |

---

## Data Sources

### Core Batch Data Sources

#### FileSource

For local Parquet/Delta files:

```python
from feast import FileSource
from feast.data_format import ParquetFormat

source = FileSource(
    path="data/driver_stats.parquet",
    timestamp_field="event_timestamp",
    created_timestamp_column="created",
    file_format=ParquetFormat(),
    description="Driver statistics",
    tags={"source": "data-lake"},
    owner="data-team@company.com"
)
```

#### BigQuerySource

```python
from feast import BigQuerySource

source = BigQuerySource(
    table="project.dataset.driver_stats",
    timestamp_field="event_timestamp",
    created_timestamp_column="created",
    query="SELECT * FROM project.dataset.driver_stats WHERE is_active = TRUE",
)
```

#### SnowflakeSource

```python
from feast.infra.offline_stores.snowflake_source import SnowflakeSource

source = SnowflakeSource(
    database="FEAST_DB",
    schema="PUBLIC",
    table="DRIVER_STATS",
    timestamp_field="EVENT_TIMESTAMP",
)
```

#### RedshiftSource

```python
from feast.infra.offline_stores.redshift_source import RedshiftSource

source = RedshiftSource(
    database="feast",
    schema="public",
    table="driver_stats",
    timestamp_field="event_timestamp",
)
```

#### SparkSource (Community)

```python
from feast.infra.offline_stores.contrib.spark_offline_store.spark_source import SparkSource

source = SparkSource(
    table="driver_stats",
    timestamp_field="event_timestamp",
)
```

### Stream Data Sources

#### KafkaSource

```python
from feast.data_source import KafkaSource

kafka_source = KafkaSource(
    name="driver_stats_stream",
    kafka_bootstrap_servers="localhost:9092",
    topic="driver_stats",
    timestamp_field="event_timestamp",
    batch_source=BigQuerySource(table="project.dataset.driver_stats"),
    message_format=AvroFormat(schema_json="..."),
)
```

#### KinesisSource

```python
from feast.data_source import KinesisSource

kinesis_source = KinesisSource(
    name="driver_stats_stream",
    stream_name="driver-stats-stream",
    region="us-east-1",
    timestamp_field="event_timestamp",
    batch_source=BigQuerySource(table="project.dataset.driver_stats"),
)
```

### PushSource

For real-time feature updates:

```python
from feast.data_source import PushSource

push_source = PushSource(
    name="driver_stats_push",
    batch_source=BigQuerySource(table="project.dataset.driver_stats"),
)
```

### RequestSource

For request-time data in on-demand feature views:

```python
from feast import RequestSource

input_request = RequestSource(
    name="request_data",
    schema=[
        Field(name="driver_trip_distance", dtype=Float32),
    ]
)
```

---

## Configuration

### feature_store.yaml Structure

```yaml
project: my_feature_project
registry: data/registry.db
provider: local
online_store:
  type: sqlite
  path: data/online_store.db
offline_store:
  type: file
entity_key_serialization_version: 2
```

### Configuration Options

#### Project

```yaml
project: my_feature_project
```

- Defines namespace for the feature store
- Used to isolate multiple deployments
- Should only contain letters, numbers, and underscores

#### Registry Options

**Local File:**
```yaml
registry: data/registry.db
```

**S3:**
```yaml
registry:
  path: s3://my-bucket/registry.pb
  cache_ttl_seconds: 60
```

**GCS:**
```yaml
registry:
  path: gs://my-bucket/registry.pb
  cache_ttl_seconds: 60
```

**SQL (PostgreSQL):**
```yaml
registry:
  registry_type: sql
  path: postgresql://user:password@localhost:5432/feast
  cache_ttl_seconds: 60
  sqlalchemy_config_kwargs:
    echo: false
    pool_pre_ping: true
```

**PostgreSQL Registry Store:**
```yaml
registry:
  registry_store_type: PostgreSQLRegistryStore
  path: feast_registry
  host: localhost
  port: 5432
  database: feast
  db_schema: public
  user: feast
  password: feast
```

#### Provider Options

| Provider | Description | Default Stores |
|----------|-------------|----------------|
| `local` | Local development | File offline, SQLite online |
| `gcp` | Google Cloud Platform | BigQuery offline, Datastore online |
| `aws` | Amazon Web Services | Redshift offline, DynamoDB online |

#### Entity Key Serialization

```yaml
entity_key_serialization_version: 2
```

Version 2 is recommended for new projects.

### Complete Configuration Examples

#### Local Development

```yaml
project: driver_features
registry: data/registry.db
provider: local
online_store:
  type: sqlite
  path: data/online_store.db
offline_store:
  type: file
```

#### AWS Production

```yaml
project: driver_features
registry:
  path: s3://feast-bucket/registry.pb
  cache_ttl_seconds: 60
provider: aws
online_store:
  type: dynamodb
  region: us-east-1
offline_store:
  type: redshift
  cluster_id: my-redshift-cluster
  region: us-east-1
  database: feast
  user: admin
  s3_staging_location: s3://feast-bucket/staging/
  iam_role: arn:aws:iam::123456789:role/redshift-role
```

#### GCP Production

```yaml
project: driver_features
registry:
  path: gs://feast-bucket/registry.pb
  cache_ttl_seconds: 60
provider: gcp
online_store:
  type: datastore
  project_id: my-gcp-project
offline_store:
  type: bigquery
  project_id: my-gcp-project
  dataset: feast_dataset
```

#### Snowflake

```yaml
project: driver_features
registry: data/registry.db
provider: local
offline_store:
  type: snowflake.offline
  account: myaccount.us-east-1
  user: feast_user
  password: feast_password
  role: SYSADMIN
  warehouse: COMPUTE_WH
  database: FEAST_DB
  schema: PUBLIC
online_store:
  type: sqlite
  path: data/online_store.db
```

#### PostgreSQL (All-in-One)

```yaml
project: driver_features
registry:
  registry_type: sql
  path: postgresql://feast:feast@localhost:5432/feast
  cache_ttl_seconds: 60
provider: local
offline_store:
  type: postgres
  host: localhost
  port: 5432
  database: feast
  db_schema: public
  user: feast
  password: feast
online_store:
  type: postgres
  host: localhost
  port: 5432
  database: feast
  db_schema: public
  user: feast
  password: feast
```

---

## Online Stores

### SQLite (Default Local)

```yaml
online_store:
  type: sqlite
  path: data/online_store.db
```

### Redis

```yaml
online_store:
  type: redis
  connection_string: localhost:6379
  # Optional
  key_ttl_seconds: 86400
```

**Redis Cluster:**
```yaml
online_store:
  type: redis
  connection_string: redis1:6379,redis2:6379,redis3:6379
  redis_type: redis_cluster
```

**Redis Sentinel:**
```yaml
online_store:
  type: redis
  connection_string: localhost:26379
  redis_type: redis_sentinel
  sentinel_master: mymaster
```

### PostgreSQL

```yaml
online_store:
  type: postgres
  host: localhost
  port: 5432
  database: feast
  db_schema: public
  user: feast_user
  password: feast_password
  # Optional SSL
  sslmode: require
  sslcert_path: /path/to/client-cert.pem
  sslkey_path: /path/to/client-key.pem
  sslrootcert_path: /path/to/ca-cert.pem
  # Optional PGVector support
  vector_enabled: true
```

### DynamoDB

```yaml
online_store:
  type: dynamodb
  region: us-east-1
  # Optional
  table_name_template: feast_{project}_{table_name}
```

**Required IAM Permissions:**
- `dynamodb:CreateTable`
- `dynamodb:DescribeTable`
- `dynamodb:DeleteTable`
- `dynamodb:BatchWriteItem`
- `dynamodb:BatchGetItem`

### Cassandra / Astra DB

```yaml
online_store:
  type: cassandra
  hosts:
    - 192.168.1.1
    - 192.168.1.2
  port: 9042
  keyspace: feast_keyspace
  username: cassandra_user
  password: cassandra_password
  # Optional
  protocol_version: 4
  load_balancing:
    local_dc: datacenter1
```

**Astra DB:**
```yaml
online_store:
  type: cassandra
  secure_bundle_path: /path/to/secure-connect-bundle.zip
  keyspace: feast_keyspace
  username: token
  password: AstraCS:...
```

### Datastore (GCP)

```yaml
online_store:
  type: datastore
  project_id: my-gcp-project
  # Optional
  namespace: feast
```

### Bigtable (GCP)

```yaml
online_store:
  type: bigtable
  project_id: my-gcp-project
  instance: feast-instance
```

---

## Offline Stores

### File (Parquet)

```yaml
offline_store:
  type: file
```

### BigQuery

```yaml
offline_store:
  type: bigquery
  project_id: my-gcp-project
  dataset: feast_dataset
  # Optional
  location: US
```

### Redshift

```yaml
offline_store:
  type: redshift
  cluster_id: my-redshift-cluster
  region: us-east-1
  database: feast
  user: admin
  s3_staging_location: s3://feast-bucket/staging/
  iam_role: arn:aws:iam::123456789:role/redshift-role
```

### Snowflake

```yaml
offline_store:
  type: snowflake.offline
  account: myaccount.us-east-1
  user: feast_user
  password: feast_password
  role: SYSADMIN
  warehouse: COMPUTE_WH
  database: FEAST_DB
  schema: PUBLIC
```

### PostgreSQL

```yaml
offline_store:
  type: postgres
  host: localhost
  port: 5432
  database: feast
  db_schema: public
  user: feast_user
  password: feast_password
```

### Spark (Community)

```yaml
offline_store:
  type: spark
  spark_conf:
    spark.master: local[*]
    spark.ui.enabled: "false"
```

---

## CLI Commands

### Global Options

```bash
feast [OPTIONS] COMMAND [ARGS]...
```

| Option | Description |
|--------|-------------|
| `-c, --chdir TEXT` | Switch to a different feature repository directory |
| `--help` | Show help message |

### Commands Reference

#### `feast init`

Creates a new Feast repository.

```bash
feast init [OPTIONS] REPO_NAME

Options:
  -m, --minimal              Create empty project repository
  -t, --template TEXT        Template to use (default: local)
                             Options: local, gcp, aws, snowflake, spark,
                             postgres, hbase, cassandra, rockset, hazelcast
```

**Examples:**
```bash
feast init my_feature_repo
feast init -t gcp my_gcp_repo
feast init -t postgres my_postgres_repo
feast init --minimal empty_repo
```

#### `feast apply`

Registers objects and updates infrastructure.

```bash
feast apply [OPTIONS]

Options:
  --skip-source-validation   Skip validation of data sources
```

**Description**: Scans Python files for Feast object definitions, validates them, syncs metadata to registry, and deploys necessary infrastructure.

#### `feast materialize`

Materializes data for a specific time range.

```bash
feast materialize [OPTIONS] START_DATE END_DATE

Options:
  -v, --views TEXT           Feature views to materialize (can specify multiple)
  --disable-event-timestamp  Disable event timestamp validation
```

**Examples:**
```bash
feast materialize 2021-01-01T00:00:00 2021-12-31T23:59:59
feast materialize -v driver_hourly_stats -v driver_daily_stats 2021-01-01 2021-12-31
feast materialize --disable-event-timestamp 2021-01-01 2021-12-31
```

#### `feast materialize-incremental`

Materializes incrementally from last run.

```bash
feast materialize-incremental [OPTIONS] END_DATE

Options:
  -v, --views TEXT           Feature views to materialize
```

**Examples:**
```bash
feast materialize-incremental $(date +%Y-%m-%d)
feast materialize-incremental -v driver_hourly_stats 2021-12-31
```

#### `feast serve`

Starts the feature server.

```bash
feast serve [OPTIONS]

Options:
  -h, --host TEXT    Host to bind (default: 127.0.0.1)
  -p, --port INTEGER Port to listen on (default: 6566)
  -t, --type TEXT    Server type: rest or grpc (default: rest)
  --no-access-log    Disable access logging
  --workers INTEGER  Number of worker processes
```

**Examples:**
```bash
feast serve
feast serve --port 8080
feast serve --host 0.0.0.0 --port 6566
feast serve --type grpc --port 6567
```

#### `feast ui`

Starts the Feast Web UI.

```bash
feast ui [OPTIONS]

Options:
  -h, --host TEXT           Host to bind (default: 0.0.0.0)
  -p, --port INTEGER        Port to listen on (default: 8888)
  --registry_ttl_sec INT    Registry cache TTL in seconds (default: 5)
```

**Examples:**
```bash
feast ui
feast ui --port 8080 --registry_ttl_sec 60
```

#### `feast entities`

Lists all registered entities.

```bash
feast entities list [OPTIONS]

Options:
  --tags TEXT    Filter by tags (e.g., --tags 'key:value')
```

#### `feast feature-views`

Lists all registered feature views.

```bash
feast feature-views list [OPTIONS]

Options:
  --tags TEXT    Filter by tags
```

#### `feast registry-dump`

Prints contents of metadata registry.

```bash
feast registry-dump
```

#### `feast configuration`

Displays current configuration.

```bash
feast configuration
```

#### `feast teardown`

Tears down deployed infrastructure.

```bash
feast teardown
```

#### `feast version`

Displays Feast SDK version.

```bash
feast version
```

#### `feast permissions`

Manages access controls.

```bash
feast permissions list [OPTIONS]
feast permissions describe PERMISSION_NAME
feast permissions check
feast permissions list-roles [OPTIONS]

Options:
  --tags TEXT      Filter by tags
  -v, --verbose    Show detailed information
```

---

## Point-in-Time Joins

### Overview

Point-in-time joins enable Feast to reproduce the state of features at a specific point in the past, preventing data leakage in ML pipelines.

### TTL (Time-to-Live)

The TTL parameter defines the temporal window for feature retrieval:

```python
driver_stats = FeatureView(
    name="driver_stats",
    entities=[driver],
    ttl=timedelta(hours=2),  # Look back up to 2 hours
    ...
)
```

**Important**: TTL is relative to each timestamp in the entity dataframe, NOT the current time.

### Timestamp Fields

| Field | Description |
|-------|-------------|
| `event_timestamp` | When the feature value was generated/valid |
| `created_timestamp` | When the row was written to the data source |

**Example:**
```python
source = FileSource(
    path="data/driver_stats.parquet",
    timestamp_field="event_timestamp",
    created_timestamp_column="created_at",
)
```

### Example with Point-in-Time Correctness

```python
# Entity dataframe with timestamps
entity_df = pd.DataFrame({
    "driver_id": [1001, 1001, 1001],
    "event_timestamp": [
        datetime(2021, 4, 12, 10, 0, 0),   # Get features as of 10:00
        datetime(2021, 4, 12, 11, 0, 0),   # Get features as of 11:00
        datetime(2021, 4, 12, 12, 0, 0),   # Get features as of 12:00
    ]
})

# Each row gets the latest feature values available at its timestamp
# within the TTL window
training_df = fs.get_historical_features(
    entity_df=entity_df,
    features=["driver_stats:trips_today"]
).to_df()
```

---

## Complete Example

### Project Structure

```
my_feature_repo/
  feature_store.yaml
  features.py
  data/
    driver_stats.parquet
    registry.db
    online_store.db
```

### features.py

```python
from datetime import timedelta
from feast import Entity, FeatureView, Field, FileSource, FeatureService
from feast.types import Int64, Float32, String

# Entity definition
driver = Entity(
    name="driver",
    join_keys=["driver_id"],
    description="Driver entity",
)

# Data source
driver_stats_source = FileSource(
    path="data/driver_stats.parquet",
    timestamp_field="event_timestamp",
)

# Feature view
driver_hourly_stats = FeatureView(
    name="driver_hourly_stats",
    entities=[driver],
    ttl=timedelta(hours=1),
    schema=[
        Field(name="trips_today", dtype=Int64),
        Field(name="conv_rate", dtype=Float32),
        Field(name="acc_rate", dtype=Float32),
    ],
    source=driver_stats_source,
    online=True,
)

# Feature service
driver_activity_service = FeatureService(
    name="driver_activity",
    features=[driver_hourly_stats],
)
```

### feature_store.yaml

```yaml
project: driver_features
registry: data/registry.db
provider: local
online_store:
  type: sqlite
  path: data/online_store.db
```

### Usage

```python
from feast import FeatureStore
from datetime import datetime
import pandas as pd

# Initialize
fs = FeatureStore(repo_path=".")

# Apply definitions
fs.apply([driver, driver_hourly_stats, driver_activity_service])

# Materialize to online store
fs.materialize_incremental(end_date=datetime.now())

# Get training data
entity_df = pd.DataFrame({
    "driver_id": [1001, 1002],
    "event_timestamp": [datetime.now(), datetime.now()]
})
training_df = fs.get_historical_features(
    entity_df=entity_df,
    features=fs.get_feature_service("driver_activity")
).to_df()

# Get online features
online_features = fs.get_online_features(
    features=["driver_hourly_stats:conv_rate", "driver_hourly_stats:trips_today"],
    entity_rows=[{"driver_id": 1001}]
).to_dict()
```

---

## Resources

- **Official Documentation**: https://docs.feast.dev/
- **Python API Reference**: https://rtd.feast.dev/ or https://api.docs.feast.dev/python/
- **GitHub Repository**: https://github.com/feast-dev/feast
- **CLI Reference**: https://docs.feast.dev/reference/feast-cli-commands
- **Feature Store YAML**: https://docs.feast.dev/reference/feature-repository/feature-store-yaml
