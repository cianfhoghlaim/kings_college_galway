# Feast Feature Store: Patterns and Best Practices

A comprehensive guide to production-ready patterns for Feast feature store implementations.

## Table of Contents

1. [Feature Engineering Patterns](#1-feature-engineering-patterns)
2. [Data Model Patterns](#2-data-model-patterns)
3. [Operational Patterns](#3-operational-patterns)
4. [Integration Patterns](#4-integration-patterns)

---

## 1. Feature Engineering Patterns

### 1.1 Point-in-Time Correct Joins

Point-in-time joins are critical for preventing data leakage during model training. Feast ensures that only features available at each historical timestamp are joined to training data.

**How It Works:**
- User provides an entity dataframe with timestamps representing historical events
- For each row, Feast queries feature values from the data source
- The system scans backward in time from the entity timestamp up to the TTL limit
- Features are joined onto the entity dataframe

```python
import pandas as pd
from feast import FeatureStore

store = FeatureStore(".")

# Entity dataframe with timestamps
entity_df = pd.DataFrame({
    "driver_id": [1001, 1002, 1003],
    "event_timestamp": pd.to_datetime([
        "2023-01-01 10:00:00",
        "2023-01-01 11:00:00",
        "2023-01-01 12:00:00"
    ])
})

# Retrieve point-in-time correct features
training_df = store.get_historical_features(
    entity_df=entity_df,
    features=[
        "driver_hourly_stats:trips_today",
        "driver_hourly_stats:earnings_today",
        "driver_hourly_stats:rating"
    ],
).to_df()
```

**Key Principle**: TTL is relative to each timestamp in the entity dataframe, NOT relative to when you run the query.

### 1.2 Feature Freshness Strategies

#### TTL (Time-to-Live) Configuration

TTL defines the maximum lookback window for features, ensuring stale data is excluded:

```python
from datetime import timedelta
from feast import Entity, FeatureView, Field, FileSource
from feast.types import Int64, Float32

driver = Entity(name="driver", join_keys=["driver_id"])

driver_stats_fv = FeatureView(
    name="driver_hourly_stats",
    entities=[driver],
    schema=[
        Field(name="trips_today", dtype=Int64),
        Field(name="earnings_today", dtype=Float32),
    ],
    ttl=timedelta(hours=2),  # Features older than 2 hours are excluded
    source=FileSource(
        path="driver_hourly_stats.parquet",
        timestamp_field="event_timestamp"
    )
)
```

#### Fresh Feature Views Pattern

Create separate feature views for different freshness requirements:

```python
# Standard freshness (hourly)
driver_hourly_stats = FeatureView(
    name="driver_hourly_stats",
    ttl=timedelta(hours=1),
    # ... configuration
)

# High freshness (real-time)
driver_realtime_stats = FeatureView(
    name="driver_realtime_stats_fresh",
    ttl=timedelta(minutes=5),
    # ... configuration
)
```

#### Materialization for Online Freshness

Keep online store features up-to-date:

```bash
# Materialize features to online store
feast materialize-incremental $(date -u +"%Y-%m-%dT%H:%M:%S")

# Or with explicit time range
feast materialize 2023-01-01T00:00:00 2023-01-31T23:59:59
```

### 1.3 Handling Late-Arriving Data

#### Event Timestamps

Use event timestamps (when the data was generated) rather than processing timestamps:

```python
from feast import FileSource

# Correct: Use event_timestamp from source data
source = FileSource(
    path="data/driver_stats.parquet",
    timestamp_field="event_timestamp",  # When the event actually occurred
    created_timestamp_column="created_at"  # When row was inserted (optional)
)
```

#### Watermark for Streaming

For stream sources, set watermark delays to handle late data:

```python
from feast import KafkaSource
from feast.data_format import JsonFormat

driver_stats_stream = KafkaSource(
    name="driver_stats_stream",
    kafka_bootstrap_servers="localhost:9092",
    topic="drivers",
    timestamp_field="event_timestamp",
    batch_source=driver_stats_batch_source,
    message_format=JsonFormat(
        schema_json="driver_id integer, event_timestamp timestamp, trips int"
    ),
    watermark_delay_threshold=timedelta(minutes=5),  # Allow 5 min late data
)
```

### 1.4 Feature Versioning

#### Naming Convention Pattern

Use version suffixes for feature tracking:

```python
# Version 1: Basic conversion rate
conv_rate_v1 = FeatureView(
    name="driver_conv_rate_v1",
    schema=[
        Field(name="conv_rate", dtype=Float32),
    ],
    # ... configuration
)

# Version 2: With smoothing applied
conv_rate_v2 = FeatureView(
    name="driver_conv_rate_v2",
    schema=[
        Field(name="conv_rate_smoothed", dtype=Float32),
    ],
    # ... configuration
)
```

**Benefits of _vN suffix:**
- List all features with a prefix and sort by version
- Build reports/UI showing feature evolution
- Support gradual migration strategies

#### Metadata for Version Tracking

```python
from feast import FeatureView

driver_stats_fv = FeatureView(
    name="driver_hourly_stats_v2",
    description="Driver statistics with improved null handling",
    tags={
        "version": "2.0",
        "deprecated_by": "",
        "deprecation_date": "",
        "owner": "ml-team@company.com",
        "change_log": "Added null imputation for missing values"
    },
    # ... rest of configuration
)
```

---

## 2. Data Model Patterns

### 2.1 Entity Design Patterns

#### Basic Entity Definition

```python
from feast import Entity

# Simple entity with single join key
driver = Entity(
    name="driver",
    join_keys=["driver_id"],
    description="Driver entity for ride-hailing features"
)

# Entity with composite key
user_merchant = Entity(
    name="user_merchant",
    join_keys=["user_id", "merchant_id"],
    description="User-merchant interaction features"
)
```

#### Entity Aliasing

Use aliases when entity dataframe columns don't match feature view columns:

```python
# Base entity
location = Entity(name="location", join_keys=["location_id"])

# Feature view
location_weather_fv = FeatureView(
    name="location_weather",
    entities=[location],
    schema=[
        Field(name="temperature", dtype=Float32),
        Field(name="humidity", dtype=Float32),
    ],
    source=weather_source
)

# Query with different column names using alias
entity_df = pd.DataFrame({
    "origin_location_id": [1, 2],  # Different column name
    "event_timestamp": pd.to_datetime(["2023-01-01", "2023-01-02"])
})

# Use join_key_map for aliasing
training_df = store.get_historical_features(
    entity_df=entity_df,
    features=["location_weather:temperature"],
    full_feature_names=True
).to_df()
```

#### Cardinality Patterns

```python
# Zero entities: Global features
global_stats_fv = FeatureView(
    name="global_stats",
    entities=[],  # No entity - global features
    schema=[
        Field(name="total_active_users", dtype=Int64),
        Field(name="platform_avg_rating", dtype=Float32),
    ],
    source=global_stats_source
)

# Single entity: User-specific features
user_profile_fv = FeatureView(
    name="user_profile",
    entities=[user],
    schema=[
        Field(name="account_age_days", dtype=Int64),
        Field(name="lifetime_value", dtype=Float64),
    ],
    source=user_source
)

# Multiple entities: Composite features
user_merchant_fv = FeatureView(
    name="user_merchant_interactions",
    entities=[user, merchant],
    schema=[
        Field(name="purchase_count", dtype=Int64),
        Field(name="avg_order_value", dtype=Float64),
    ],
    source=interaction_source
)
```

### 2.2 Feature View Organization

#### Domain-Based Organization

```
feature_store/
├── feature_store.yaml
├── entities/
│   ├── __init__.py
│   ├── user.py
│   ├── driver.py
│   └── merchant.py
├── features/
│   ├── __init__.py
│   ├── user_features.py
│   ├── driver_features.py
│   ├── transaction_features.py
│   └── real_time_features.py
├── sources/
│   ├── __init__.py
│   ├── batch_sources.py
│   └── stream_sources.py
└── services/
    ├── __init__.py
    └── feature_services.py
```

#### Feature Services for Grouping

Group related features for specific use cases:

```python
from feast import FeatureService

# Service for fraud detection model
fraud_detection_service = FeatureService(
    name="fraud_detection_v1",
    features=[
        driver_hourly_stats[["trips_today", "earnings_today"]],
        user_transaction_stats[["avg_transaction_amount", "transaction_count_7d"]],
        real_time_location[["current_speed", "distance_from_home"]],
    ],
    tags={
        "model": "fraud_detection",
        "team": "trust_safety",
        "version": "1.0"
    }
)

# Service for recommendation model
recommendation_service = FeatureService(
    name="recommendation_v1",
    features=[
        user_preferences,
        item_embeddings,
        user_item_interactions,
    ]
)
```

### 2.3 Feature Naming Conventions

#### Recommended Patterns

```python
# Pattern: {entity}_{domain}_{feature_name}_{version}
# Examples:
"driver_activity_trips_today_v1"
"user_engagement_session_count_7d_v2"
"merchant_performance_rating_avg_v1"

# Pattern: {domain}_{aggregation}_{time_window}
# Examples:
"transaction_sum_amount_24h"
"login_count_7d"
"purchase_avg_value_30d"
```

#### Naming Best Practices

1. **Be descriptive**: `user_login_count_7d` not `cnt7`
2. **Include time windows**: `_24h`, `_7d`, `_30d`, `_lifetime`
3. **Specify aggregations**: `_sum`, `_avg`, `_max`, `_count`
4. **Use version suffixes**: `_v1`, `_v2`
5. **Indicate data type when ambiguous**: `_flag`, `_ratio`, `_pct`

### 2.4 Feature Groups and Domains

#### Project Namespacing

Each Feast project provides natural namespacing:

```yaml
# feature_store.yaml for user team
project: user_features
registry: s3://feast-registry/user-features
provider: aws
```

```yaml
# feature_store.yaml for merchant team
project: merchant_features
registry: s3://feast-registry/merchant-features
provider: aws
```

---

## 3. Operational Patterns

### 3.1 CI/CD for Feature Stores

#### GitHub Actions Workflow

```yaml
# .github/workflows/feast-apply.yml
name: Feast Apply

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  feast-plan:
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request'
    steps:
      - uses: actions/checkout@v3

      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'

      - name: Install dependencies
        run: |
          pip install feast[redis,postgres]

      - name: Run feast plan
        run: |
          cd feature_store
          feast plan
        env:
          FEAST_REGISTRY: ${{ secrets.FEAST_REGISTRY }}

  feast-apply:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3

      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'

      - name: Install dependencies
        run: |
          pip install feast[redis,postgres]

      - name: Run feast apply
        run: |
          cd feature_store
          feast apply
        env:
          FEAST_REGISTRY: ${{ secrets.FEAST_REGISTRY }}
```

#### Multi-Environment Structure

```
feature_store/
├── staging/
│   ├── feature_store.yaml
│   ├── entities.py
│   └── features.py
├── production/
│   ├── feature_store.yaml
│   ├── entities.py
│   └── features.py
└── .github/
    └── workflows/
        ├── staging.yml
        └── production.yml
```

**staging/feature_store.yaml:**
```yaml
project: my_project_staging
registry: s3://feast-registry-staging/registry.db
provider: aws
online_store:
  type: redis
  connection_string: ${REDIS_STAGING_URL}
offline_store:
  type: snowflake
  account: ${SNOWFLAKE_ACCOUNT}
  database: STAGING_DB
```

**production/feature_store.yaml:**
```yaml
project: my_project_production
registry: s3://feast-registry-prod/registry.db
provider: aws
online_store:
  type: redis
  connection_string: ${REDIS_PROD_URL}
offline_store:
  type: snowflake
  account: ${SNOWFLAKE_ACCOUNT}
  database: PRODUCTION_DB
```

### 3.2 Testing Features

#### Unit Testing Feature Definitions

```python
import pytest
from datetime import timedelta
from feast import Entity, FeatureView, Field
from feast.types import Int64, Float32

def test_feature_view_schema():
    """Test that feature view has expected schema."""
    driver = Entity(name="driver", join_keys=["driver_id"])

    fv = FeatureView(
        name="driver_stats",
        entities=[driver],
        schema=[
            Field(name="trips_today", dtype=Int64),
            Field(name="rating", dtype=Float32),
        ],
        ttl=timedelta(hours=2),
        source=mock_source
    )

    assert fv.name == "driver_stats"
    assert len(fv.schema) == 2
    assert fv.ttl == timedelta(hours=2)

def test_entity_join_keys():
    """Test entity configuration."""
    driver = Entity(name="driver", join_keys=["driver_id"])
    assert driver.join_keys == ["driver_id"]
```

#### Integration Testing with Feast

```python
import pandas as pd
from datetime import datetime, timedelta
from feast import FeatureStore

def test_historical_feature_retrieval():
    """Test point-in-time correct feature retrieval."""
    store = FeatureStore(".")

    entity_df = pd.DataFrame({
        "driver_id": [1001, 1002],
        "event_timestamp": [
            datetime.now() - timedelta(hours=1),
            datetime.now() - timedelta(hours=2)
        ]
    })

    features = store.get_historical_features(
        entity_df=entity_df,
        features=["driver_stats:trips_today"]
    ).to_df()

    assert "driver_id" in features.columns
    assert "trips_today" in features.columns
    assert len(features) == 2

def test_online_feature_retrieval():
    """Test online feature serving."""
    store = FeatureStore(".")

    features = store.get_online_features(
        features=["driver_stats:trips_today", "driver_stats:rating"],
        entity_rows=[{"driver_id": 1001}]
    ).to_dict()

    assert "trips_today" in features
    assert "rating" in features
```

### 3.3 Monitoring and Observability

#### StatsD Metrics Configuration

```yaml
# Helm chart values for Feast with metrics
metrics:
  enabled: true
  statsd:
    host: statsd-exporter.monitoring
    port: 9125
```

#### Key Metrics to Monitor

1. **Feature Serving Latency**: p50, p95, p99 latency for online feature retrieval
2. **Feature Freshness**: Time since last materialization
3. **Feature Coverage**: Percentage of requests with all features available
4. **Registry Sync**: Time since last registry update

#### Data Quality Monitoring with Great Expectations

```python
from feast import FeatureStore
from feast.dqm.profilers.ge_profiler import ge_profiler
from great_expectations.dataset import Dataset
from great_expectations.core.expectation_suite import ExpectationSuite

@ge_profiler
def feature_quality_profiler(dataset: Dataset) -> ExpectationSuite:
    """Define data quality expectations for features."""
    # Numeric range checks
    dataset.expect_column_values_to_be_between(
        "rating", min_value=0, max_value=5
    )

    # Null checks
    dataset.expect_column_values_to_not_be_null("driver_id")

    # Distribution checks
    dataset.expect_column_mean_to_be_between(
        "trips_today", min_value=0, max_value=100
    )

    return dataset.get_expectation_suite()

# Apply validation during feature retrieval
store = FeatureStore(".")
job = store.get_historical_features(
    entity_df=entity_df,
    features=["driver_stats:rating", "driver_stats:trips_today"]
)

# Validate against reference dataset
reference = store.get_saved_dataset("driver_stats_reference")
validated_df = job.to_df(
    validation_reference=reference.as_reference(profiler=feature_quality_profiler)
)
```

### 3.4 Schema Evolution

#### Adding New Features

```python
# Version 1: Original feature view
driver_stats_v1 = FeatureView(
    name="driver_stats",
    schema=[
        Field(name="trips_today", dtype=Int64),
        Field(name="rating", dtype=Float32),
    ],
    # ...
)

# Version 2: Add new feature (backward compatible)
driver_stats_v2 = FeatureView(
    name="driver_stats",
    schema=[
        Field(name="trips_today", dtype=Int64),
        Field(name="rating", dtype=Float32),
        Field(name="acceptance_rate", dtype=Float32),  # New feature
    ],
    # ...
)
```

#### Migration Strategy

1. **Deprecation Warning**: Tag old features with deprecation metadata
2. **Transition Period**: Run both old and new feature views simultaneously
3. **Consumer Migration**: Update all consumers to use new features
4. **Cleanup**: Remove deprecated features after all consumers migrate

```python
# Deprecated feature view with warning
driver_stats_old = FeatureView(
    name="driver_stats_v1",
    tags={
        "deprecated": "true",
        "deprecated_date": "2024-01-01",
        "migration_guide": "Use driver_stats_v2 instead",
        "removal_date": "2024-03-01"
    },
    # ... configuration
)
```

---

## 4. Integration Patterns

### 4.1 Training Pipeline Integration

#### Airflow DAG Example

```python
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta

def materialize_features():
    """Materialize features to online store."""
    from feast import FeatureStore
    store = FeatureStore("/opt/airflow/feature_store")
    store.materialize_incremental(datetime.now())

def generate_training_data():
    """Generate training dataset with features."""
    import pandas as pd
    from feast import FeatureStore

    store = FeatureStore("/opt/airflow/feature_store")

    # Load entity dataframe with labels
    entity_df = pd.read_parquet("s3://data/training_entities.parquet")

    # Get features
    training_df = store.get_historical_features(
        entity_df=entity_df,
        features=[
            "driver_stats:trips_today",
            "driver_stats:rating",
            "user_stats:lifetime_value"
        ]
    ).to_df()

    # Save training dataset
    training_df.to_parquet("s3://data/training_dataset.parquet")

with DAG(
    "feature_training_pipeline",
    schedule_interval=timedelta(days=1),
    start_date=datetime(2024, 1, 1),
    catchup=False
) as dag:

    materialize = PythonOperator(
        task_id="materialize_features",
        python_callable=materialize_features
    )

    generate_data = PythonOperator(
        task_id="generate_training_data",
        python_callable=generate_training_data
    )

    materialize >> generate_data
```

### 4.2 Inference Pipeline Integration

#### Online Inference with Feature Server

```python
from feast import FeatureStore
import numpy as np

class FraudDetectionService:
    def __init__(self):
        self.store = FeatureStore(".")
        self.model = self._load_model()

    def predict(self, user_id: int, transaction_amount: float) -> dict:
        # Get online features
        features = self.store.get_online_features(
            features=[
                "user_stats:transaction_count_7d",
                "user_stats:avg_transaction_amount",
                "user_stats:fraud_flag_count"
            ],
            entity_rows=[{"user_id": user_id}]
        ).to_dict()

        # Prepare feature vector
        feature_vector = np.array([
            features["transaction_count_7d"][0],
            features["avg_transaction_amount"][0],
            features["fraud_flag_count"][0],
            transaction_amount
        ])

        # Make prediction
        prediction = self.model.predict([feature_vector])[0]

        return {
            "user_id": user_id,
            "fraud_probability": float(prediction),
            "features_used": features
        }
```

#### Batch Inference Pipeline

```python
import pandas as pd
from feast import FeatureStore

def batch_inference():
    """Run batch inference on all users."""
    store = FeatureStore(".")

    # Get all users for scoring
    entity_df = pd.read_sql(
        "SELECT user_id, CURRENT_TIMESTAMP as event_timestamp FROM users",
        connection
    )

    # Retrieve features
    features_df = store.get_historical_features(
        entity_df=entity_df,
        features=[
            "user_stats:lifetime_value",
            "user_stats:churn_risk_score",
            "user_behavior:session_count_30d"
        ]
    ).to_df()

    # Load model and predict
    model = load_model("churn_model")
    predictions = model.predict(features_df[feature_columns])

    # Save predictions
    results = pd.DataFrame({
        "user_id": entity_df["user_id"],
        "churn_prediction": predictions,
        "prediction_timestamp": datetime.now()
    })
    results.to_parquet("s3://predictions/churn_batch.parquet")
```

### 4.3 Batch vs Real-Time Serving

#### Decision Framework

| Pattern | Use Case | Latency | Freshness | Cost |
|---------|----------|---------|-----------|------|
| Batch Materialization | Daily scoring, training | Minutes-Hours | Hours-Days | Low |
| Online Serving | Real-time predictions | Milliseconds | Seconds-Minutes | Medium |
| Stream Processing | Near real-time | Seconds | Seconds | High |
| Precomputed Predictions | High-volume, static entities | Milliseconds | Hours-Days | Low |

#### Hybrid Pattern Example

```python
from feast import FeatureStore
import redis

class HybridScoringService:
    def __init__(self):
        self.store = FeatureStore(".")
        self.cache = redis.Redis(host='localhost', port=6379)
        self.model = self._load_model()

    def get_prediction(self, user_id: int) -> float:
        # Check cache for precomputed prediction
        cached = self.cache.get(f"prediction:{user_id}")
        if cached:
            return float(cached)

        # Fall back to real-time inference
        features = self.store.get_online_features(
            features=["user_stats:all_features"],
            entity_rows=[{"user_id": user_id}]
        ).to_dict()

        prediction = self.model.predict([list(features.values())])[0]

        # Cache for future requests
        self.cache.setex(f"prediction:{user_id}", 3600, prediction)

        return prediction
```

### 4.4 Data Quality Validation

#### Validation in Training Pipeline

```python
from feast import FeatureStore
from feast.dqm.profilers.ge_profiler import ge_profiler
from great_expectations.dataset import Dataset
from great_expectations.core.expectation_suite import ExpectationSuite

@ge_profiler
def training_data_profiler(dataset: Dataset) -> ExpectationSuite:
    """Comprehensive data quality checks for training data."""

    # Schema validation
    dataset.expect_table_columns_to_match_ordered_list([
        "driver_id", "trips_today", "rating", "event_timestamp"
    ])

    # Completeness checks
    dataset.expect_column_values_to_not_be_null("driver_id")
    dataset.expect_column_values_to_not_be_null("rating")

    # Range validation
    dataset.expect_column_values_to_be_between("rating", 1, 5)
    dataset.expect_column_values_to_be_between("trips_today", 0, 100)

    # Distribution checks (detect drift)
    dataset.expect_column_mean_to_be_between("rating", 3.5, 4.5)
    dataset.expect_column_stdev_to_be_between("trips_today", 5, 20)

    # Uniqueness
    dataset.expect_compound_columns_to_be_unique(
        ["driver_id", "event_timestamp"]
    )

    return dataset.get_expectation_suite()

# Usage
store = FeatureStore(".")

try:
    training_df = store.get_historical_features(
        entity_df=entity_df,
        features=["driver_stats:trips_today", "driver_stats:rating"]
    ).to_df(
        validation_reference=store.get_saved_dataset("reference_data")
            .as_reference(profiler=training_data_profiler)
    )
except ValidationFailed as e:
    print(f"Data quality check failed: {e}")
    # Alert on-call, skip training, or use fallback data
```

#### Continuous Monitoring Pattern

```python
from datetime import datetime, timedelta
from feast import FeatureStore
import logging

def monitor_feature_freshness():
    """Monitor feature freshness and alert if stale."""
    store = FeatureStore(".")

    # Get last materialization time
    registry = store.registry
    feature_views = registry.list_feature_views(project=store.project)

    alerts = []
    for fv in feature_views:
        last_updated = registry.get_materialization_intervals(
            fv.name, store.project
        )

        if last_updated:
            latest = max(interval.end_time for interval in last_updated)
            staleness = datetime.utcnow() - latest

            if staleness > timedelta(hours=fv.ttl.total_seconds() / 3600 * 0.5):
                alerts.append({
                    "feature_view": fv.name,
                    "last_updated": latest,
                    "staleness_hours": staleness.total_seconds() / 3600
                })

    if alerts:
        logging.warning(f"Stale features detected: {alerts}")
        # Send to alerting system
```

---

## Common Anti-Patterns to Avoid

### 1. Not Using Point-in-Time Joins
**Anti-pattern**: Using latest feature values for all training rows
**Solution**: Always use `get_historical_features()` with timestamps

### 2. Over-Engineering Feature Views
**Anti-pattern**: One feature view per feature
**Solution**: Group semantically related features in single feature views

### 3. Ignoring TTL Configuration
**Anti-pattern**: Setting very long TTLs or no TTL
**Solution**: Set appropriate TTLs based on feature freshness requirements

### 4. Not Versioning Features
**Anti-pattern**: Modifying features in place
**Solution**: Use version suffixes and maintain backward compatibility

### 5. Tight Coupling to Serving Layer
**Anti-pattern**: Direct database queries instead of Feast abstractions
**Solution**: Always use Feast SDK for feature retrieval

### 6. Skipping Data Validation
**Anti-pattern**: Trusting upstream data blindly
**Solution**: Implement Great Expectations validation in pipelines

### 7. Ignoring Feature Lineage
**Anti-pattern**: Not tracking feature dependencies
**Solution**: Use tags and documentation to maintain lineage

---

## Quick Reference Commands

```bash
# Initialize new feature repository
feast init my_feature_repo

# Apply feature definitions to registry
feast apply

# Plan changes without applying
feast plan

# Materialize features to online store
feast materialize-incremental $(date -u +"%Y-%m-%dT%H:%M:%S")

# Serve features via HTTP
feast serve

# View registered features
feast registry-dump

# Validate feature definitions
feast validate
```

---

## Resources

- **Official Documentation**: https://docs.feast.dev/
- **GitHub Repository**: https://github.com/feast-dev/feast
- **Tutorials**: https://docs.feast.dev/tutorials/tutorials-overview
- **Community Slack**: https://feast-slack.slack.com/
