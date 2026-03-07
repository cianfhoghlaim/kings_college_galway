---
name: LanceDB: Initialize
description: Initialize LanceDB setup in your project with best practices
category: Database
tags: [lancedb, setup, initialization]
---

# Initialize LanceDB in Project

Set up a production-ready LanceDB configuration in your project.

## Steps

1. **Analyze current project structure**
   - Check for existing LanceDB usage
   - Identify data models and requirements
   - Review integration points (LangChain, LlamaIndex, Agno, etc.)

2. **Create database module** with:
   - Connection manager (singleton pattern)
   - Schema definitions
   - Index configuration
   - Migration support

3. **Set up recommended directory structure**:
   ```
   data/
     lancedb/          # Local database files
   src/
     database/
       __init__.py
       connection.py   # Connection manager
       schemas.py      # Pydantic schemas
       operations.py   # CRUD operations
       config.py       # Configuration
   ```

4. **Generate configuration** with:
   - Database URI (local/S3/cloud)
   - Index parameters
   - Embedding model settings
   - Performance tuning options

5. **Create initialization script** that:
   - Sets up tables with proper schemas
   - Creates indexes
   - Validates configuration
   - Provides health check

6. **Add utilities**:
   - Batch ingestion helper
   - Search wrapper with common patterns
   - Maintenance scripts (compaction, cleanup)
   - Monitoring/logging

7. **Generate example usage** showing:
   - Basic CRUD operations
   - Vector search
   - Hybrid search (if applicable)
   - Integration with existing code

8. **Create documentation**:
   - Setup instructions
   - Schema definitions
   - Common operations
   - Troubleshooting guide

## Connection Manager Template

```python
# src/database/connection.py
import lancedb
from typing import Optional
import os

class LanceDBConnection:
    """Singleton connection manager for LanceDB."""

    _instance: Optional['LanceDBConnection'] = None
    _db: Optional[lancedb.DBConnection] = None
    _tables: dict = {}

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def connect(self, uri: Optional[str] = None):
        """Connect to LanceDB."""
        if self._db is None:
            uri = uri or os.getenv("LANCEDB_URI", "data/lancedb")
            self._db = lancedb.connect(uri)
        return self._db

    def get_table(self, name: str):
        """Get or cache table reference."""
        if name not in self._tables:
            self._tables[name] = self._db.open_table(name)
        return self._tables[name]

    def create_table(self, name: str, schema=None, data=None, mode="create"):
        """Create table with schema."""
        table = self._db.create_table(name, schema=schema, data=data, mode=mode)
        self._tables[name] = table
        return table

    def close(self):
        """Close connection and clear cache."""
        self._tables.clear()
        self._db = None

# Global instance
db_manager = LanceDBConnection()
```

## Schema Template

```python
# src/database/schemas.py
from lancedb.pydantic import LanceModel, Vector
from lancedb.embeddings import get_registry
from datetime import datetime
from typing import Optional

# Configure embedding model
embedding_model = get_registry().get("sentence-transformers").create(
    name="BAAI/bge-small-en-v1.5"
)

class DocumentSchema(LanceModel):
    """Schema for document storage."""

    # Core fields
    id: str
    text: str = embedding_model.SourceField()
    vector: Vector(embedding_model.ndims()) = embedding_model.VectorField()

    # Metadata
    title: Optional[str] = None
    category: str
    source: str
    created_at: datetime
    updated_at: Optional[datetime] = None

    # Custom metadata
    metadata: dict = {}
```

## Operations Template

```python
# src/database/operations.py
from .connection import db_manager
from .schemas import DocumentSchema
import pandas as pd

class DocumentOperations:
    """CRUD operations for documents."""

    def __init__(self):
        self.db = db_manager.connect()
        self.table_name = "documents"

    def initialize(self):
        """Initialize table with indexes."""
        # Create table
        table = db_manager.create_table(
            self.table_name,
            schema=DocumentSchema,
            mode="create"
        )

        # Create indexes
        table.create_index(metric="cosine", index_type="IVF_PQ")
        table.create_scalar_index("category", index_type="BTREE")
        table.create_scalar_index("created_at", index_type="BTREE")
        table.create_fts_index("text")

        return table

    def add_documents(self, documents: list[dict]):
        """Add documents in batch."""
        table = db_manager.get_table(self.table_name)
        table.add(documents)

    def search(self, query: str, category: Optional[str] = None, limit: int = 10):
        """Hybrid search with optional filtering."""
        table = db_manager.get_table(self.table_name)

        search = table.search(query, query_type="hybrid")

        if category:
            search = search.where(f"category = '{category}'")

        return search.limit(limit).to_pandas()

    def get_by_id(self, doc_id: str):
        """Get document by ID."""
        table = db_manager.get_table(self.table_name)
        results = table.search([0.0] * 384).where(f"id = '{doc_id}'").limit(1).to_pandas()
        return results.iloc[0] if len(results) > 0 else None

    def delete(self, doc_id: str):
        """Delete document by ID."""
        table = db_manager.get_table(self.table_name)
        table.delete(f"id = '{doc_id}'")

    def compact(self):
        """Compact table for better performance."""
        table = db_manager.get_table(self.table_name)
        table.compact_files()
```

## Configuration Template

```python
# src/database/config.py
from pydantic import BaseModel
import os

class LanceDBConfig(BaseModel):
    """LanceDB configuration."""

    # Connection
    uri: str = os.getenv("LANCEDB_URI", "data/lancedb")

    # Embedding model
    embedding_model: str = "BAAI/bge-small-en-v1.5"
    embedding_dim: int = 384

    # Index parameters
    index_metric: str = "cosine"
    index_type: str = "IVF_PQ"
    num_partitions: int = 256
    num_sub_vectors: int = 24  # embedding_dim // 16

    # Performance
    read_consistency_interval: int = 30
    batch_size: int = 10000

    # Maintenance
    compact_after_n_inserts: int = 50
    keep_versions: int = 10

# Global config
config = LanceDBConfig()
```

## Initialization Script Template

```python
# scripts/init_lancedb.py
"""Initialize LanceDB database with proper configuration."""

import sys
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))

from database.connection import db_manager
from database.operations import DocumentOperations
from database.config import config

def main():
    """Initialize database."""
    print(f"Initializing LanceDB at {config.uri}")

    # Connect
    db = db_manager.connect(config.uri)
    print(f"✓ Connected to database")

    # Initialize tables
    ops = DocumentOperations()
    table = ops.initialize()
    print(f"✓ Created table '{ops.table_name}' with indexes")

    # Verify
    stats = table.stats()
    print(f"✓ Table stats: {stats.num_rows} rows, {stats.num_fragments} fragments")

    print("\nDatabase initialized successfully!")
    print(f"\nTo use:")
    print(f"  from database.operations import DocumentOperations")
    print(f"  ops = DocumentOperations()")
    print(f"  results = ops.search('query text')")

if __name__ == "__main__":
    main()
```

## Your Task

1. Review the project structure
2. Ask about specific requirements (data models, use cases, scale)
3. Create the database module with appropriate schemas
4. Set up connection management
5. Generate initialization script
6. Provide usage examples
7. Create maintenance scripts if needed

## Questions to Ask

Before implementing, clarify:
- What data will be stored? (documents, images, code, etc.)
- What's the expected scale? (thousands, millions, billions of vectors)
- What embedding model to use?
- Integration requirements? (LangChain, LlamaIndex, Agno, custom)
- Deployment target? (local, S3, LanceDB Cloud)
- Search patterns? (pure vector, hybrid, full-text)
- Metadata filtering needs?

---

Ask me about your project to get started with LanceDB initialization!
