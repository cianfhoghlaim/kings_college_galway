# Feast Feature Store for LLM and AI Applications

## Executive Summary

Feast (Feature Store) is an open-source feature store that has evolved to support modern AI/LLM applications beyond traditional ML. It provides a unified data access layer for managing structured data at scale during both training and inference, with emerging support for vector embeddings and RAG applications.

---

## 1. LLM Feature Use Cases

### 1.1 Embedding Storage and Retrieval

Feast now supports vector similarity search (alpha feature) for storing and retrieving embeddings:

```python
from feast import Field, FeatureView, FileSource
from feast.types import Array, Float32, String

# Define entity
chunk = Entity(name="chunk_id", join_keys=["chunk_id"])

# Define embedding feature view
embedding_feature_view = FeatureView(
    name="document_embeddings",
    entities=[chunk],
    schema=[
        Field(name="chunk_id", dtype=String),
        Field(name="text", dtype=String),
        Field(
            name="vector",
            dtype=Array(Float32),
            vector_index=True,
            vector_search_metric="COSINE"
        ),
    ],
    source=FileSource(path="data/embeddings.parquet"),
)
```

**Key Benefits:**
- Treat embeddings as proper ML features with lifecycle management
- Version control and governance for document repositories
- Consistent API across multiple vector database backends

### 1.2 User Preference Features for Personalization

Feature stores enable LLM personalization by providing user context at request time:

```python
# Define user preference feature view
user_preferences = FeatureView(
    name="user_preferences",
    entities=[user],
    schema=[
        Field(name="preferred_topics", dtype=Array(String)),
        Field(name="communication_style", dtype=String),
        Field(name="language", dtype=String),
        Field(name="interaction_history_embedding", dtype=Array(Float32)),
    ],
    source=user_data_source,
    ttl=timedelta(days=1),
)

# Retrieve at inference time
features = store.get_online_features(
    features=[
        "user_preferences:preferred_topics",
        "user_preferences:communication_style",
        "user_preferences:language",
    ],
    entity_rows=[{"user_id": "user123"}],
).to_dict()
```

**Personalization Patterns:**
- Pre-pend user context to LLM prompts
- Retrieve user history embeddings for semantic matching
- Combine structured preferences with vector similarity

### 1.3 Context Features for RAG Systems

Feast serves as the service layer for RAG applications:

```python
# Retrieve relevant documents for RAG context
from sentence_transformers import SentenceTransformer

model = SentenceTransformer("all-MiniLM-L6-v2")
query_embedding = model.encode(user_query)

# Vector similarity search
context_data = store.retrieve_online_documents_v2(
    features=[
        "document_embeddings:vector",
        "document_embeddings:text",
        "document_embeddings:chunk_id",
    ],
    query=query_embedding,
    top_k=5,
    distance_metric="COSINE"
).to_df()

# Use retrieved context for LLM
context = "\n".join(context_data["text"].tolist())
prompt = f"""Context: {context}

Question: {user_query}

Answer based on the context provided:"""
```

### 1.4 Real-time Feature Serving for Agents

LLM agents can use Feast to retrieve contextual features during multi-step reasoning:

```python
# Agent retrieves session context
session_features = store.get_online_features(
    features=[
        "session_context:recent_actions",
        "session_context:current_task",
        "session_context:tool_usage_history",
    ],
    entity_rows=[{"session_id": agent_session_id}],
).to_dict()

# Agent retrieves entity-specific knowledge
entity_features = store.get_online_features(
    features=[
        "product_catalog:description",
        "product_catalog:specifications",
        "product_catalog:embedding",
    ],
    entity_rows=[{"product_id": product_id}],
).to_dict()
```

---

## 2. Vector Database Integration

### 2.1 Supported Vector Stores

Feast integrates with multiple vector databases:

| Database | Vector Retrieval | Indexing | V2 API | Notes |
|----------|-----------------|----------|--------|-------|
| **Milvus** | Yes | Yes | Yes | Full support, recommended |
| **SQLite** | Yes | No | Yes | Local development |
| **Elasticsearch** | Yes | Yes | No | Enterprise search |
| **Pgvector** | Yes | No | No | PostgreSQL extension |
| **Qdrant** | Yes | Yes | No | Cloud-native |
| **Faiss** | Limited | No | No | In development |

### 2.2 Configuration

**feature_store.yaml:**

```yaml
project: rag_application
provider: local
registry: data/registry.db
offline_store:
  type: file
online_store:
  type: milvus
  path: data/online_store.db
  vector_enabled: true
  embedding_dim: 384
  index_type: "IVF_FLAT"
  metric_type: "COSINE"
```

**Installation:**

```bash
# Milvus (recommended)
pip install feast[milvus]

# PostgreSQL with pgvector
pip install feast[postgres]

# Elasticsearch
pip install feast[elasticsearch]

# Qdrant
pip install feast[qdrant]

# SQLite (local development)
pip install feast[sqlite_vec]
```

### 2.3 Vector Feature Views

```python
from feast import FeatureView, Field, Entity
from feast.types import Array, Float32, String

# Entity definition
document = Entity(name="document_id", join_keys=["document_id"])
chunk = Entity(name="chunk_id", join_keys=["chunk_id"])

# Vector-enabled feature view
city_embeddings = FeatureView(
    name="city_embeddings",
    entities=[chunk],
    schema=[
        Field(name="item_id", dtype=String),
        Field(name="text", dtype=String),
        Field(
            name="vector",
            dtype=Array(Float32),
            vector_index=True,
            vector_search_metric="COSINE"  # Options: COSINE, L2, IP
        ),
    ],
    source=FileSource(
        path="data/city_wikipedia_summaries.parquet",
        timestamp_field="event_timestamp",
    ),
)
```

### 2.4 Similarity Search Patterns

**Basic Retrieval:**

```python
# Initialize store
store = FeatureStore(repo_path=".")

# Query embedding
query = "What is the population of Paris?"
query_embedding = embedding_model.encode(query)

# Retrieve similar documents
results = store.retrieve_online_documents_v2(
    features=[
        "city_embeddings:vector",
        "city_embeddings:text",
        "city_embeddings:item_id",
    ],
    query=query_embedding,
    top_k=3,
    distance_metric="COSINE"
).to_df()

print(results[["item_id", "text", "_distance"]])
```

**Combined Vector + Structured Features:**

```python
# Retrieve both embeddings and metadata
context = store.retrieve_online_documents_v2(
    features=[
        "document_embeddings:vector",
        "document_embeddings:text",
        "document_embeddings:source_url",
        "document_embeddings:created_at",
        "document_embeddings:author",
    ],
    query=query_embedding,
    top_k=5,
    distance_metric="COSINE"
).to_df()
```

---

## 3. Real-time AI Applications

### 3.1 Low-Latency Feature Serving

Feast uses a push model for online serving, achieving sub-100ms latency:

```python
# Low-latency online retrieval
features = store.get_online_features(
    features=[
        "user_features:click_rate",
        "user_features:session_duration",
        "item_features:popularity_score",
    ],
    entity_rows=[
        {"user_id": "u1", "item_id": "i1"},
        {"user_id": "u2", "item_id": "i2"},
    ],
).to_dict()
```

**Performance Optimizations:**
- Pre-computed features stored in Redis/Milvus
- Push-based materialization (not pull)
- Batch lookups for multiple entities

### 3.2 Streaming Features

Feast supports real-time feature updates via push sources:

```python
from feast import PushSource, FeatureView

# Define push source for streaming data
push_source = PushSource(
    name="user_activity_push",
    batch_source=BigQuerySource(
        table="project.dataset.user_activity",
    ),
)

# Stream feature view
stream_features = FeatureView(
    name="user_activity_stream",
    entities=[user],
    schema=[
        Field(name="last_action", dtype=String),
        Field(name="action_count_1h", dtype=Int64),
        Field(name="session_embedding", dtype=Array(Float32)),
    ],
    source=push_source,
    ttl=timedelta(hours=1),
)

# Push real-time events
store.push(
    push_source_name="user_activity_push",
    df=events_df,
    to=PushMode.ONLINE,
)
```

### 3.3 Streaming with Denormalized

For complex streaming aggregations:

```python
# Denormalized integration for real-time aggregations
from denormalized import Context, FeastSink

# Configure Feast sink
feast_sink = FeastSink(
    repo_path="/path/to/feast/repo",
    push_source_name="realtime_features",
)

# Stream processing pipeline
ctx = Context()
ds = ctx.from_topic("user_events", json_schema)
ds.window(
    window_type=TumblingWindow,
    window_size=timedelta(minutes=5),
).aggregate(
    group_by=["user_id"],
    aggregates=[
        Avg(field="response_time"),
        Count(field="event_id"),
    ],
).sink_to_feast(feast_sink)
```

### 3.4 On-Demand Feature Transformations

Apply transformations at request time for LLM features:

```python
from feast import on_demand_feature_view, Field
from feast.types import Float64, String

@on_demand_feature_view(
    sources=[user_features_view, input_request],
    schema=[
        Field(name="personalization_score", dtype=Float64),
        Field(name="context_prompt", dtype=String),
    ],
    mode="python",
    singleton=True,
)
def compute_personalization(inputs: dict) -> dict:
    """Compute personalization features at request time."""

    # Combine user preferences with request context
    user_topics = inputs["preferred_topics"]
    query_topic = inputs["query_topic"]

    # Calculate relevance score
    score = calculate_topic_overlap(user_topics, query_topic)

    # Generate personalized prompt context
    prompt = f"User prefers {', '.join(user_topics)}. Respond in {inputs['style']} tone."

    return {
        "personalization_score": score,
        "context_prompt": prompt,
    }

# Retrieve with on-demand computation
features = store.get_online_features(
    features=[
        "compute_personalization:personalization_score",
        "compute_personalization:context_prompt",
    ],
    entity_rows=[{
        "user_id": "u123",
        "query_topic": "machine learning",
    }],
).to_dict()
```

---

## 4. MLOps for LLMs

### 4.1 Feature Pipelines for LLM Applications

**RAG Data Pipeline:**

```python
# Step 1: Data Ingestion
from feast import FeatureStore
import pandas as pd

store = FeatureStore(repo_path=".")

# Step 2: Text Processing & Embedding Generation
from docling.document_converter import DocumentConverter
from sentence_transformers import SentenceTransformer

converter = DocumentConverter()
model = SentenceTransformer("all-MiniLM-L6-v2")

def process_documents(file_paths):
    chunks = []
    for path in file_paths:
        doc = converter.convert(path).document
        for chunk in doc.chunks:
            embedding = model.encode(chunk.text)
            chunks.append({
                "chunk_id": chunk.id,
                "document_id": doc.id,
                "text": chunk.text,
                "vector": embedding.tolist(),
                "event_timestamp": datetime.now(),
            })
    return pd.DataFrame(chunks)

# Step 3: Ingest to Feature Store
df = process_documents(["doc1.pdf", "doc2.pdf"])
store.write_to_online_store(
    feature_view_name="document_embeddings",
    df=df,
)

# Step 4: Materialize to offline store for training
store.materialize(
    start_date=datetime.now() - timedelta(days=7),
    end_date=datetime.now(),
)
```

### 4.2 Training-Serving Consistency

Feast eliminates training-serving skew:

```python
# Training: Get historical features
training_df = store.get_historical_features(
    entity_df=entity_df,
    features=[
        "user_preferences:embedding",
        "document_embeddings:vector",
        "interaction_features:click_rate",
    ],
).to_df()

# Train model
model.fit(training_df)

# Serving: Same feature definitions, different store
serving_features = store.get_online_features(
    features=[
        "user_preferences:embedding",
        "document_embeddings:vector",
        "interaction_features:click_rate",
    ],
    entity_rows=[{"user_id": "u1", "document_id": "d1"}],
).to_dict()

# Predict
prediction = model.predict(serving_features)
```

### 4.3 A/B Testing with Features

While Feast doesn't provide A/B testing directly, it ensures consistency during experiments:

```python
# Consistent features across model variants
features = store.get_online_features(
    features=[
        "user_features:embedding",
        "product_features:description",
    ],
    entity_rows=[{"user_id": user_id, "product_id": product_id}],
).to_dict()

# Route to different models based on experiment
if experiment_variant == "control":
    result = model_v1.predict(features)
elif experiment_variant == "treatment":
    result = model_v2.predict(features)

# Log for analysis
log_experiment_result(
    variant=experiment_variant,
    features=features,
    result=result,
)
```

**Integration with Experimentation Platforms:**
- LaunchDarkly
- Split.io
- Statsig
- Optimizely

### 4.4 Feature Monitoring for AI Systems

Feast integrates with observability platforms for drift detection:

**Arize AI Integration:**

```python
# Log features to Arize for monitoring
from arize.pandas.logger import Client

arize_client = Client(space_key="...", api_key="...")

# Get features from Feast
features = store.get_online_features(
    features=["user_features:embedding", "user_features:preferences"],
    entity_rows=[{"user_id": user_id}],
).to_df()

# Log to Arize
arize_client.log(
    model_id="rag_model_v1",
    model_version="1.0",
    prediction_id=prediction_id,
    features=features,
    prediction_label=prediction,
    actual_label=actual,  # When available
)
```

**Evidently AI for Data Drift:**

```python
from evidently.metrics import DataDriftTable
from evidently.report import Report

# Compare training vs production features
reference_data = store.get_historical_features(
    entity_df=training_entities,
    features=feature_list,
).to_df()

production_data = store.get_historical_features(
    entity_df=production_entities,
    features=feature_list,
).to_df()

# Generate drift report
report = Report(metrics=[DataDriftTable()])
report.run(
    reference_data=reference_data,
    current_data=production_data,
)
report.save_html("drift_report.html")
```

**WhyLabs Integration:**

```python
import whylogs as why

# Profile features from Feast
features_df = store.get_historical_features(
    entity_df=entity_df,
    features=feature_list,
).to_df()

# Create profile
profile = why.log(features_df)

# Upload to WhyLabs
profile.writer("whylabs").write()
```

---

## 5. Complete RAG Example

### 5.1 Project Structure

```
rag_project/
├── feature_store.yaml
├── definitions.py
├── data/
│   ├── documents.parquet
│   └── registry.db
└── notebooks/
    └── demo.ipynb
```

### 5.2 Configuration

**feature_store.yaml:**

```yaml
project: rag_demo
provider: local
registry: data/registry.db
offline_store:
  type: file
online_store:
  type: milvus
  path: data/online_store.db
  vector_enabled: true
  embedding_dim: 384
  index_type: "IVF_FLAT"
entity_key_serialization_version: 2
```

### 5.3 Feature Definitions

**definitions.py:**

```python
from datetime import timedelta
from feast import Entity, FeatureView, Field, FileSource
from feast.types import Array, Float32, String

# Entities
chunk = Entity(
    name="chunk_id",
    join_keys=["chunk_id"],
    description="Unique identifier for document chunks",
)

document = Entity(
    name="document_id",
    join_keys=["document_id"],
    description="Unique identifier for source documents",
)

# Data source
documents_source = FileSource(
    path="data/documents.parquet",
    timestamp_field="event_timestamp",
)

# Feature view with vector embeddings
document_embeddings = FeatureView(
    name="document_embeddings",
    entities=[chunk, document],
    schema=[
        Field(name="text", dtype=String),
        Field(name="source_url", dtype=String),
        Field(
            name="vector",
            dtype=Array(Float32),
            vector_index=True,
            vector_search_metric="COSINE"
        ),
    ],
    source=documents_source,
    ttl=timedelta(days=30),
)
```

### 5.4 RAG Application

```python
from feast import FeatureStore
from sentence_transformers import SentenceTransformer
import openai

# Initialize
store = FeatureStore(repo_path=".")
embedding_model = SentenceTransformer("all-MiniLM-L6-v2")
openai.api_key = "your-api-key"

def rag_query(user_question: str, top_k: int = 5) -> str:
    """Execute RAG query using Feast."""

    # Step 1: Generate query embedding
    query_embedding = embedding_model.encode(user_question)

    # Step 2: Retrieve relevant documents
    context_df = store.retrieve_online_documents_v2(
        features=[
            "document_embeddings:vector",
            "document_embeddings:text",
            "document_embeddings:source_url",
        ],
        query=query_embedding,
        top_k=top_k,
        distance_metric="COSINE"
    ).to_df()

    # Step 3: Build context
    context_parts = []
    for _, row in context_df.iterrows():
        context_parts.append(f"Source: {row['source_url']}\n{row['text']}")
    context = "\n\n---\n\n".join(context_parts)

    # Step 4: Generate response with LLM
    response = openai.ChatCompletion.create(
        model="gpt-4",
        messages=[
            {
                "role": "system",
                "content": "Answer questions based on the provided context. "
                           "Cite sources when possible."
            },
            {
                "role": "user",
                "content": f"Context:\n{context}\n\nQuestion: {user_question}"
            }
        ],
        temperature=0.7,
    )

    return response.choices[0].message.content

# Usage
answer = rag_query("What are the main features of Feast?")
print(answer)
```

---

## 6. Production Deployment

### 6.1 Kubernetes with Feast Operator

```yaml
# feast-feature-store.yaml
apiVersion: feast.dev/v1alpha1
kind: FeatureStore
metadata:
  name: rag-feature-store
spec:
  feastProject: rag_project
  services:
    onlineStore:
      persistence:
        store:
          type: milvus
          config:
            host: milvus.default.svc.cluster.local
            port: 19530
    offlineStore:
      persistence:
        store:
          type: file
```

### 6.2 Feature Server Helm Chart

```bash
helm install feast-server feast/feast \
  --set featureStore.project=rag_project \
  --set featureStore.registry.path=s3://bucket/registry.db \
  --set onlineStore.type=milvus \
  --set onlineStore.host=milvus:19530
```

### 6.3 API Endpoint

```python
import requests

# Query feature server
response = requests.post(
    "http://feast-server:6566/get-online-features",
    json={
        "features": [
            "document_embeddings:vector",
            "document_embeddings:text",
        ],
        "entities": {"chunk_id": ["c1", "c2", "c3"]},
    }
)

features = response.json()
```

---

## 7. Best Practices

### 7.1 Feature Design for LLMs

1. **Treat embeddings as features**: Version and manage document embeddings like any other ML feature
2. **Use consistent embedding models**: Same model for indexing and querying
3. **Include metadata**: Store source URLs, timestamps, and categories alongside vectors
4. **Set appropriate TTLs**: Configure time-to-live based on data freshness requirements

### 7.2 Performance Optimization

1. **Choose appropriate index types**:
   - `FLAT`: Small datasets (<100k vectors), exact search
   - `IVF_FLAT`: Medium datasets, approximate search
   - `HNSW`: Large datasets, fast approximate search

2. **Batch operations**: Use batch writes and reads when possible
3. **Pre-compute transformations**: Use `write_to_online_store=True` for ODFVs

### 7.3 Monitoring

1. **Track feature freshness**: Monitor when features were last updated
2. **Monitor embedding drift**: Compare embedding distributions over time
3. **Log retrieval quality**: Track relevance scores and user feedback

---

## 8. Resources

### Official Documentation
- Feast Documentation: https://docs.feast.dev/
- Vector Database Guide: https://docs.feast.dev/reference/alpha-vector-database
- RAG Tutorial: https://docs.feast.dev/tutorials/rag-with-docling

### Code Examples
- Milvus Quickstart: https://github.com/feast-dev/feast/blob/master/examples/rag/milvus-quickstart.ipynb
- RAG with Docling: https://github.com/feast-dev/feast/tree/master/examples/rag-docling

### Community
- GitHub: https://github.com/feast-dev/feast
- Slack: https://slack.feast.dev/

### Integration Guides
- Milvus + Feast: https://milvus.io/docs/build_RAG_with_milvus_and_feast.md
- Arize Integration: https://arize.com/blog/feast-and-arize-supercharge-feature-management-and-model-monitoring-for-mlops/

---

## Summary

Feast provides a robust foundation for LLM and AI applications by:

1. **Unifying feature management**: Single API for embeddings, structured data, and metadata
2. **Supporting vector search**: Native integration with Milvus, Elasticsearch, and other vector stores
3. **Enabling low-latency serving**: Sub-100ms feature retrieval for real-time applications
4. **Ensuring consistency**: Same feature definitions for training and serving
5. **Integrating with MLOps**: Works with monitoring, experimentation, and orchestration tools

The key insight is that document embeddings and user preferences are ML features that benefit from proper lifecycle management, versioning, and governance - exactly what feature stores provide.
