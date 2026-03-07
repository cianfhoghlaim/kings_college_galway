---
name: LanceDB Quick Reference
description: Quick reference for common LanceDB operations and patterns
category: Database
tags: [lancedb, reference, cheatsheet]
---

# LanceDB Quick Reference

## Connection & Setup

```python
import lancedb

# Local
db = lancedb.connect("data/my-db")

# S3
db = lancedb.connect("s3://bucket/db", storage_options={...})

# LanceDB Cloud
db = lancedb.connect("db://my-db", api_key="...", region="us-east-1")
```

## Schema Definition

```python
from lancedb.pydantic import LanceModel, Vector
from lancedb.embeddings import get_registry

model = get_registry().get("sentence-transformers").create(
    name="BAAI/bge-small-en-v1.5"
)

class Document(LanceModel):
    text: str = model.SourceField()
    vector: Vector(model.ndims()) = model.VectorField()
    metadata: dict
    created_at: datetime
```

## Table Operations

```python
# Create
table = db.create_table("docs", data)
table = db.create_table("docs", schema=Document)

# Open
table = db.open_table("docs")

# List
tables = db.table_names()

# Drop
db.drop_table("docs")
```

## Data Ingestion

```python
# Add rows
table.add([{"text": "doc1", ...}, {"text": "doc2", ...}])

# Batch from iterator
def batches():
    for batch in data:
        yield pd.DataFrame(batch)
table = db.create_table("large", data=batches())

# Update
table.update(where="id = '123'", values={"price": 99.99})

# Delete
table.delete("category = 'old'")

# Upsert
table.merge_insert("id") \
    .when_matched_update_all() \
    .when_not_matched_insert_all() \
    .execute(new_data)
```

## Vector Search

```python
# Basic search
results = table.search(query_vector).limit(10).to_pandas()

# With filter
results = table.search(query_vector) \
    .where("price > 100") \
    .limit(10) \
    .to_pandas()

# Pre-filtering
results = table.search(query_vector) \
    .where("category = 'tech'", prefilter=True) \
    .limit(10)

# Automatic embedding
results = table.search("what is LanceDB?").limit(5).to_pandas()
```

## Hybrid Search

```python
from lancedb.rerankers import LinearCombinationReranker

# Hybrid search
results = table.search("query", query_type="hybrid") \
    .limit(10) \
    .to_pandas()

# With reranking
reranker = LinearCombinationReranker(weight=0.7)
results = table.search("query", query_type="hybrid") \
    .rerank(reranker=reranker) \
    .limit(5) \
    .to_pandas()
```

## Indexing

```python
# Vector index (IVF-PQ)
table.create_index(
    metric="cosine",
    num_partitions=256,
    num_sub_vectors=96,
    index_type="IVF_PQ"
)

# HNSW
table.create_index(metric="cosine", index_type="IVF_HNSW_SQ")

# Scalar indexes
table.create_scalar_index("category", index_type="BTREE")
table.create_scalar_index("tags", index_type="LABEL_LIST")
table.create_scalar_index("status", index_type="BITMAP")

# Full-text search
table.create_fts_index("content")

# Drop index
table.drop_index()
```

## Schema Evolution

```python
# Add column
table.add_columns({"new_field": "cast(NULL as string)"})

# Rename column
table.alter_columns({"path": "old_name", "rename": "new_name"})

# Drop column
table.drop_columns(["field1", "field2"])
```

## Versioning

```python
# List versions
versions = table.list_versions()

# Checkout version
old_table = table.checkout_version(5)

# Restore
table.restore(version=3)

# Cleanup
table.cleanup_old_versions(older_than_days=7)
table.cleanup_old_versions(keep_latest=10)
```

## Optimization

```python
# Compact fragments
table.compact_files()

# Query analysis
plan = table.search(vector).where("price < 100").explain_plan()
analysis = table.search(vector).analyze_plan()

# Stats
stats = table.stats()
print(f"Rows: {stats.num_rows}, Fragments: {stats.num_fragments}")
```

## Index Parameter Calculation

```python
import math

num_rows = 1_000_000
embedding_dim = 768

num_partitions = int(math.sqrt(num_rows))  # 1000
num_sub_vectors = embedding_dim // 16       # 48
```

## Common Patterns

### Connection Singleton
```python
class LanceDBManager:
    _instance = None
    _db = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def get_connection(self, uri):
        if self._db is None:
            self._db = lancedb.connect(uri)
        return self._db
```

### RAG with Hybrid Search
```python
db = lancedb.connect("/tmp/lancedb")
table = db.open_table("knowledge")
table.create_index(metric="cosine")
table.create_fts_index("content")

results = table.search("query", query_type="hybrid") \
    .limit(5) \
    .to_pandas()
```

### Multimodal Search
```python
from lancedb.embeddings import get_registry

clip = get_registry().get("open-clip").create(name="ViT-B-32")

class MultimodalDoc(LanceModel):
    image_uri: str = clip.SourceField()
    vector: Vector(clip.ndims()) = clip.VectorField()

# Search with text
results = table.search("red dress").limit(10)

# Search with image
results = table.search("path/to/image.jpg").limit(10)
```

## Integration Examples

### LangChain
```python
from langchain.vectorstores import LanceDB
from langchain.embeddings import OpenAIEmbeddings

embeddings = OpenAIEmbeddings()
vectorstore = LanceDB.from_documents(
    documents=docs,
    embedding=embeddings,
    connection=table
)
```

### LlamaIndex
```python
from llama_index.vector_stores.lancedb import LanceDBVectorStore

vector_store = LanceDBVectorStore(
    uri="/tmp/lancedb",
    table_name="docs",
    query_type="hybrid"
)
```

### Agno
```python
from agno.vectordb.lancedb import LanceDb

vector_db = LanceDb(
    table_name="vectors",
    uri="tmp/lancedb",
)
```

## Best Practices Checklist

- ✅ Use singleton pattern for connections
- ✅ Batch inserts (don't insert one by one)
- ✅ Create indexes AFTER bulk load
- ✅ Create scalar indexes on filter columns
- ✅ Always use `.limit()` on queries
- ✅ Compact regularly for write-heavy workloads
- ✅ Cleanup old versions periodically
- ✅ Use appropriate index type for dataset size

## Anti-Patterns to Avoid

- ❌ Creating indexes before bulk load
- ❌ Individual row inserts
- ❌ Filtering without scalar indexes
- ❌ Queries without limit
- ❌ Repeated connection creation
- ❌ Using transformations in WHERE clauses (LOWER, UPPER, etc.)

## Troubleshooting

### Slow queries?
```python
# Check indexes
table.list_indexes()

# Analyze query
table.search(vector).where("...").explain_plan()

# Create missing indexes
table.create_scalar_index("filter_column", index_type="BTREE")
```

### High memory?
- Use IVF-PQ instead of HNSW
- Use float16 instead of float32
- Increase num_sub_vectors (more quantization)

### Storage growing?
```python
# Compact fragments
table.compact_files()

# Cleanup old versions
table.cleanup_old_versions(older_than_days=7)
```

---

For detailed documentation, see `/home/user/hackathon/lancedb-llms.txt`
