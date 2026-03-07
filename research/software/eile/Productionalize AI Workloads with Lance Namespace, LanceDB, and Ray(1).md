---
title: "Productionalize AI Workloads with Lance Namespace, LanceDB, and Ray"
source: "https://lancedb.com/blog/lance-namespace-lancedb-and-ray/"
author:
  - "[[[Jack Ye]]]"
published: 2025-09-04
created: 2025-12-23
description: "Learn how to productionalize AI workloads with Lance Namespace's enterprise stack integration and the scalability of LanceDB and Ray for end-to-end ML …"
tags:
  - "clippings"
---
In our [previous post](https://lancedb.com/blog/introducing-lance-namespace-spark-integration), we introduced [Lance Namespace](https://lance.org/format/namespace/) and its integration with Apache Spark. Today, we’re excited to showcase how to **productionalize your AI workloads** by combining:

- **Lance Namespace** for seamless enterprise stack integration with your existing metadata services
- **Ray** for data ingestion and feature engineering at scale
- **LanceDB** for efficient [vector search](https://docs.lancedb.com/search/vector-search/) and [full‑text search](https://docs.lancedb.com/search/full-text-search/)

This powerful combination enables you to build production-ready AI applications that integrate with your existing infrastructure while maintaining the scalability needed for real-world deployments.

## What’s New

### Lance–Ray Integration

The [lance-ray](https://pypi.org/project/lance-ray/) package has now evolved into its own independent subproject, bringing seamless integration between Ray and Lance. It enables distributed read, write, and data evolution operations on Lance datasets using Ray’s parallel processing capabilities, making it simple to handle large-scale data transformations and feature engineering workloads across your compute cluster.

### Lance Namespace Python and Rust SDKs

Lance Namespace now provides native Python and Rust SDKs that enable seamless enterprise integration across languages. This is what enables integration with both `lance-ray` and LanceDB.

## Building an End-to-End AI Pipeline

Let’s walk through a complete example using real data from Hugging Face to build a question-answering system. We’ll use the [BeIR/quora](https://huggingface.co/datasets/BeIR/quora) dataset to demonstrate the entire workflow.

### Step 1: Setting Up the Environment

First, install the required packages:

code

```fallback
pip install lance-ray sentence-transformers datasets

pip install --no-deps lancedb==0.25.0

pip install --no-deps lance-namespace==0.0.14
```

Initialize your Ray cluster and import the necessary libraries:

python

```python
import ray

import pyarrow as pa

from lance_ray import write_lance, read_lance, add_columns

from datasets import load_dataset

from sentence_transformers import SentenceTransformer

import numpy as np

# Initialize Ray with sufficient resources for parallel processing

ray.init()

# Load the embedding model (we'll use it later)

model = SentenceTransformer('BAAI/bge-small-en-v1.5')
```

### Step 2: Initialize Lance Namespace

Lance Namespace provides a unified interface to store and manage your Lance tables across different metadata services. Depending on your enterprise environment requirements, you can choose from various supported catalog services:

python

```python
import lance_namespace as ln

# Example 1: Directory-based namespace (for development/testing)

namespace = ln.connect("dir", {"root": "./lance_tables"})

# Example 2: Hive Metastore (for Hadoop/Spark ecosystems)

# namespace = ln.connect("hive", {"uri": "thrift://hive-metastore:9083"})

# Example 3: AWS Glue Catalog (for AWS-based infrastructure)

# namespace = ln.connect("glue", {"region": "us-east-1"})

# Example 4: Unity Catalog (for Databricks environments)

# namespace = ln.connect("unity", {"url": "https://your-workspace.cloud.databricks.com"})
```

For this example, we’ll use a directory-based namespace for simplicity, but you can seamlessly switch to any of the above options based on your infrastructure. See the [namespace implementations documentation](https://lance.org/format/namespace/impls) for detailed configuration options of each integrated service.

### Step 3: Distributed Data Ingestion with Ray

Now let’s load the Quora dataset and ingest it into [Lance format](https://docs.lancedb.com/lance/) using Ray’s distributed processing:

python

```python
# Load Quora dataset from Hugging Face

print("Loading Quora dataset...")

dataset = load_dataset("BeIR/quora", "corpus", split="corpus[:10000]", trust_remote_code=True)

# Convert to Ray Dataset for distributed processing

ray_dataset = ray.data.from_huggingface(dataset)

# Define schema with proper types

schema = pa.schema([

    pa.field("_id", pa.string()),

    pa.field("title", pa.string()),

    pa.field("text", pa.string()),

])

# Write to Lance format using namespace

print("Writing data to Lance format via namespace...")

write_lance(

    ray_dataset,

    namespace=namespace,

    table_id=["quora_questions"],

    schema=schema,

    mode="create",

    max_rows_per_file=5000,

)

print(f"Ingested {ray_dataset.count()} documents into Lance format")
```

### Step 4: Feature Engineering with Lance–Ray

Now we’ll use Ray’s distributed processing to generate embeddings for all documents.

python

```python
def generate_embeddings(batch: pa.RecordBatch) -> pa.RecordBatch:

    """Generate embeddings for text using sentence-transformers."""

    from sentence_transformers import SentenceTransformer

    

    # Initialize model (will be cached per Ray worker)

    model = SentenceTransformer('BAAI/bge-small-en-v1.5')

    

    # Combine title and text for better semantic representation

    texts = []

    for i in range(len(batch)):

        title = batch["title"][i].as_py() or ""

        text = batch["text"][i].as_py() or ""

        combined = f"{title}. {text}".strip()

        texts.append(combined)

    

    # Generate embeddings

    embeddings = model.encode(texts, normalize_embeddings=True)

    

    # Return as RecordBatch with fixed-size list field

    return pa.RecordBatch.from_arrays(

        [pa.array(embeddings.tolist(), type=pa.list_(pa.float32(), 384))],

        names=["vector"]

    )

# Add embeddings column using distributed processing with namespace

print("Generating embeddings using Ray...")

add_columns(

    None, # no static URI

    namespace=namespace,

    table_id=["quora_questions"],

    transform=generate_embeddings,

    read_columns=["title", "text"],  # Only read necessary columns

    batch_size=100,  # Process in batches of 100

    concurrency=4,  # Use 4 parallel workers

    ray_remote_args={"num_gpus": 0.25} if ray.cluster_resources().get("GPU", 0) > 0 else {}

)

print("Embeddings generated successfully!")
```

The `add_columns` functionality in Ray allows ML/AI scientists to quickly start feature engineering with a local or remote Ray cluster. For more advanced feature engineering capabilities such as lazy materialization, partial backfill, fault-tolerant execution, check out [LanceDB’s Geneva](https://lancedb.com/docs/geneva/) - our feature engineering framework that provides schema enforcement, versioning, and complex transformations. You can also follow our [multimodal lakehouse tutorial](https://lancedb.com/docs/tutorials/mmlh/) for comprehensive examples.

Now let’s connect to our Lance dataset through [LanceDB](https://docs.lancedb.com/) using the same namespace and perform vector similarity search:

python

```python
import lancedb

from sentence_transformers import SentenceTransformer

# Connect to LanceDB using the same namespace

db = lancedb.connect_namespace("dir", {"root": "./lance_tables"})

table = db.open_table("quora_questions")

# Create [vector index](https://docs.lancedb.com/indexing/vector-index/) for fast similarity search

print("Creating vector index...")

table.create_index(

    metric="cosine",

    vector_column_name="vector",

    index_type="IVF_PQ",

    num_partitions=32,

    num_sub_vectors=48,

)

# Perform vector similarity search

query_text = "How do I learn machine learning?"

model = SentenceTransformer('BAAI/bge-small-en-v1.5')

query_embedding = model.encode([query_text], normalize_embeddings=True)[0]

vector_results = (

    table.search(query_embedding, vector_column_name="vector")

    .limit(5)

    .to_pandas()

)

print("\n=== Vector Search Results ===")

print(f"Query: {query_text}\n")

for idx, row in vector_results.iterrows():

    print(f"{idx + 1}. {row['title']}")

    print(f"   {row['text'][:150]}...")

    print()
```

Now let’s also do a full text search against the `text` column:

python

```python
print("Creating full-text search index...")

table.create_fts_index("text")

# Example 1: Full‑Text Search

keyword_results = (

    table.search("machine learning algorithms", query_type="fts")

    .limit(5)

    .to_pandas()

)

print("\n=== Full-Text Search Results ===")

print("Keywords: 'machine learning algorithms'\n")

for idx, row in keyword_results.iterrows():

    print(f"{idx + 1}. {row['title']}")

    print(f"   {row['text'][:150]}...")

    print()
```

### Step 7: Beyond the Examples

Now, you can continue playing around with the dataset. You can add more feature columns with python functions through Ray. LanceDB also allows [hybrid search](https://docs.lancedb.com/search/hybrid-search/) that combines the semantic understanding of [vector search](https://docs.lancedb.com/search/vector-search/) with the precision of [keyword matching](https://docs.lancedb.com/search/full-text-search/). You can also load data into tools like PyTorch and LangChain for other AI activities.

## Real-World Use Cases

This integration pattern is particularly powerful for:

1. **RAG Applications**: Ingest documents, generate embeddings, and serve semantic search
2. **Recommendation Systems**: Process user interactions and build vector indices at scale
3. **Multimodal Search**: Process images and text together using Ray’s distributed computing
4. **Feature Stores**: Transform and store ML features with versioning via Lance Namespace
5. **Real-time Analytics**: Combine batch processing with low-latency search

## Getting Started Today

Ready to scale your AI workloads? Here’s how to get started:

1. **Install the packages**: `pip install lance-ray lancedb`
2. **Read the documentation**: [Lance–Ray](https://lance.org/integrations/ray/), [LanceDB](https://docs.lancedb.com/), [Vector Search](https://docs.lancedb.com/search/vector-search/), [Full‑Text Search](https://docs.lancedb.com/search/full-text-search/), [Hybrid Search](https://docs.lancedb.com/search/hybrid-search/), [Vector Indexing](https://docs.lancedb.com/indexing/vector-index/), [FTS Indexing](https://docs.lancedb.com/indexing/fts-index/), [Filtering](https://docs.lancedb.com/search/filtering/), [Reranking](https://docs.lancedb.com/reranking/), [Quickstart](https://docs.lancedb.com/tables/), [LanceDB Geneva](https://docs.lancedb.com/geneva/)
3. **Join the community**: [Discord](https://discord.gg/zMM32dvNtd) and [GitHub Discussions](https://github.com/lancedb/lance/discussions)

## Thank You to Our Contributors

We’d like to extend our heartfelt thanks to the community members who have contributed to making this integration a reality, shoutout to:

- **Enwei Jiao** from Luma AI
- **Bryan Keller** from Netflix
- **Jay Narale** from Uber
- **Jay Ju** from ByteDance
- **Jiebao Xiao** from Xiaomi

Your contributions, feedback, and real-world use cases have been instrumental in shaping this integration to meet the needs of production AI workloads.

## Conclusion

The combination of [Lance Namespace](https://lance.org/format/namespace/), Ray, and [LanceDB](https://docs.lancedb.com/) provides a complete solution for productionalizing AI workloads. [Lance Namespace](https://lance.org/format/namespace/) ensures seamless integration with your existing enterprise metadata services, Ray delivers the distributed computing power needed for data ingestion and feature engineering at scale, and LanceDB provides efficient [vector search](https://docs.lancedb.com/search/vector-search/), [full‑text search](https://docs.lancedb.com/search/full-text-search/), and [hybrid search](https://docs.lancedb.com/search/hybrid-search/) capabilities for serving your AI applications.

This integrated approach bridges the gap between experimentation and production, enabling you to build AI systems that not only scale but also fit naturally into your existing infrastructure. Get started with the [Quickstart](https://docs.lancedb.com/quickstart/) or explore [indexing](https://docs.lancedb.com/indexing/vector-index/) options.

Whether you’re building a [RAG system](https://docs.lancedb.com/tutorials/agents/), recommendation engine, or [multimodal search](https://docs.lancedb.com/tutorials/agents/multimodal-agent) application, this powerful trio gives you the enterprise integration, scalability, and performance you need for production deployments.

Try it out today and let us know what you build! We’re excited to see how you use [Lance Namespace](https://lance.org/format/namespace/), Ray, and [LanceDB](https://docs.lancedb.com/) to productionalize your AI workloads.