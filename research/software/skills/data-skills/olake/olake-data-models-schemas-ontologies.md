# OLake Data Models, Schemas, and Ontologies

## Executive Summary

This document provides a comprehensive ontology of OLake's data models, schemas, and type systems. OLake is an open-source data replication tool that captures changes from operational databases (PostgreSQL, MySQL, MongoDB, Oracle) and loads them into data lakes using Apache Iceberg table format.

**Key Findings:**
- OLake uses a three-file configuration pattern: `source.json`, `destination.json`, and `streams.json`
- Supports multiple catalog backends: REST, JDBC, AWS Glue, Hive Metastore
- Provides automatic type mapping from source databases to Iceberg/Parquet types
- Implements CDC (Change Data Capture) with exactly-once semantics
- Supports schema evolution without breaking pipelines

---

## 1. Domain Model

### 1.1 Core Entities

```
┌─────────────────────────────────────────────────────────────────┐
│                      OLake Domain Model                         │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   Source     │         │   Pipeline   │         │ Destination  │
│              │────────>│              │────────>│              │
│ - Database   │ Config  │ - State      │ Writes  │ - Iceberg    │
│ - Connection │         │ - Metadata   │         │ - Catalog    │
│ - CDC Setup  │         │ - Transforms │         │ - Storage    │
└──────────────┘         └──────────────┘         └──────────────┘
       │                        │                         │
       │ Discovers              │ Manages                 │ Stores
       ▼                        ▼                         ▼
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   Stream     │         │    State     │         │    Table     │
│              │         │              │         │              │
│ - Schema     │         │ - Checkpoint │         │ - Snapshots  │
│ - Sync Mode  │         │ - Position   │         │ - Partitions │
│ - Selection  │         │ - Offsets    │         │ - Metadata   │
└──────────────┘         └──────────────┘         └──────────────┘
       │                                                  │
       │ Contains                                         │ References
       ▼                                                  ▼
┌──────────────┐                                  ┌──────────────┐
│   Column     │                                  │  Data File   │
│              │                                  │              │
│ - Name       │                                  │ - Parquet    │
│ - Type       │                                  │ - Path       │
│ - Nullable   │                                  │ - Metrics    │
└──────────────┘                                  └──────────────┘
```

### 1.2 Entity Relationships

**Source → Stream** (1:N)
- A source database contains multiple streams (tables/collections)
- Each stream is independently selectable and configurable

**Stream → Column** (1:N)
- Each stream has a schema composed of columns
- Columns have types that map to Iceberg types

**Pipeline → State** (1:1)
- Each pipeline maintains a single state object
- State tracks CDC position, checkpoints, and offsets

**Destination → Table** (1:N)
- A destination (Iceberg namespace) contains multiple tables
- Each table corresponds to a source stream

**Table → Snapshot** (1:N)
- Tables maintain a history of snapshots
- Each sync operation creates a new snapshot

**Table → DataFile** (1:N)
- Tables reference multiple Parquet data files
- Files are organized by partitions

### 1.3 State Machine Models

#### Pipeline States

```
┌─────────────┐
│   INITIAL   │
│ (not run)   │
└──────┬──────┘
       │ start
       ▼
┌─────────────┐
│ DISCOVERING │
│ (introspect)│
└──────┬──────┘
       │ discover complete
       ▼
┌─────────────┐      error      ┌─────────────┐
│  FULL_LOAD  │────────────────>│   FAILED    │
│ (initial)   │                 │             │
└──────┬──────┘                 └─────────────┘
       │ load complete              ▲
       ▼                            │
┌─────────────┐                    │ error
│     CDC     │────────────────────┘
│ (streaming) │
└──────┬──────┘
       │ stop
       ▼
┌─────────────┐
│   STOPPED   │
│             │
└─────────────┘
```

#### Replication States (Per Stream)

```
NOT_SELECTED ──> SELECTED ──> SYNCING ──> SYNCED
                                  │
                                  │ error
                                  ▼
                              ERROR_STATE
                                  │
                                  │ retry
                                  └──> SYNCING
```

### 1.4 Metadata Structures

#### Pipeline Metadata
```json
{
  "pipeline_id": "uuid",
  "source_type": "postgres|mysql|mongodb|oracle",
  "destination_type": "iceberg",
  "created_at": "timestamp",
  "updated_at": "timestamp",
  "state": {
    "last_sync": "timestamp",
    "checkpoint": {
      "lsn": "pg_lsn",
      "gtid": "mysql_gtid",
      "resume_token": "mongodb_token"
    },
    "stream_states": [
      {
        "stream_name": "table_name",
        "last_processed_offset": "offset_value",
        "row_count": 12345
      }
    ]
  }
}
```

---

## 2. Schema Definitions

### 2.1 Configuration Schemas

#### source.json (PostgreSQL Example)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["host", "port", "database", "username", "password"],
  "properties": {
    "host": {
      "type": "string",
      "description": "Database hostname or IP address"
    },
    "port": {
      "type": "integer",
      "default": 5432,
      "description": "Database port"
    },
    "database": {
      "type": "string",
      "description": "Database name to replicate"
    },
    "username": {
      "type": "string",
      "description": "Database user with replication privileges"
    },
    "password": {
      "type": "string",
      "description": "User password (encrypted at rest)"
    },
    "ssl": {
      "type": "object",
      "properties": {
        "mode": {
          "type": "string",
          "enum": ["disable", "require", "verify-ca", "verify-full"],
          "default": "require"
        },
        "ca_cert": {
          "type": "string",
          "description": "Path to CA certificate for SSL verification"
        }
      }
    },
    "update_method": {
      "type": "object",
      "description": "CDC configuration for logical replication",
      "properties": {
        "replication_slot": {
          "type": "string",
          "description": "Name of the logical replication slot"
        },
        "publication": {
          "type": "string",
          "description": "Name of the publication for CDC"
        },
        "initial_wait_time": {
          "type": "integer",
          "default": 120,
          "description": "Seconds to wait for initial replication setup"
        }
      }
    },
    "max_threads": {
      "type": "integer",
      "default": 5,
      "minimum": 1,
      "maximum": 20,
      "description": "Parallel threads for full load"
    }
  }
}
```

**Annotated Example:**
```json
{
  "host": "postgres.example.com",        // Source database endpoint
  "port": 5432,
  "database": "production",              // Database to replicate
  "username": "olake_user",              // User with REPLICATION privilege
  "password": "<SECURE_PASSWORD>",
  "ssl": {
    "mode": "require"                    // Enforce TLS
  },
  "update_method": {
    "replication_slot": "olake_slot",    // Created with pg_create_logical_replication_slot
    "publication": "olake_pub",          // Created with CREATE PUBLICATION
    "initial_wait_time": 120
  },
  "max_threads": 5                       // Parallel table dumps during full load
}
```

#### destination.json (Iceberg with REST Catalog)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["type", "writer"],
  "properties": {
    "type": {
      "type": "string",
      "enum": ["ICEBERG"],
      "description": "Destination writer type"
    },
    "writer": {
      "type": "object",
      "required": ["catalog_type", "iceberg_s3_path"],
      "properties": {
        "catalog_type": {
          "type": "string",
          "enum": ["rest", "jdbc", "glue", "hive"],
          "description": "Iceberg catalog backend"
        },
        "rest_catalog_url": {
          "type": "string",
          "format": "uri",
          "description": "REST catalog endpoint (required if catalog_type=rest)"
        },
        "jdbc_url": {
          "type": "string",
          "description": "JDBC connection string (required if catalog_type=jdbc)"
        },
        "iceberg_s3_path": {
          "type": "string",
          "format": "uri",
          "pattern": "^s3://",
          "description": "S3 path for Iceberg warehouse"
        },
        "s3_endpoint": {
          "type": "string",
          "format": "uri",
          "description": "Custom S3 endpoint (for R2, MinIO, etc.)"
        },
        "aws_region": {
          "type": "string",
          "default": "us-east-1",
          "description": "AWS region or 'auto' for S3-compatible storage"
        },
        "aws_access_key": {
          "type": "string",
          "description": "S3 access key ID"
        },
        "aws_secret_key": {
          "type": "string",
          "description": "S3 secret access key (encrypted)"
        },
        "iceberg_db": {
          "type": "string",
          "description": "Iceberg namespace/database for tables"
        },
        "token": {
          "type": "string",
          "description": "Bearer token for REST catalog auth"
        },
        "partition_spec": {
          "type": "object",
          "description": "Default partition specification for tables",
          "properties": {
            "partition_by": {
              "type": "array",
              "items": {
                "type": "string"
              },
              "description": "Columns to partition by (e.g., ['year(timestamp)', 'region'])"
            }
          }
        }
      }
    }
  }
}
```

**Annotated Example (Cloudflare R2 with REST Catalog):**
```json
{
  "type": "ICEBERG",
  "writer": {
    "catalog_type": "rest",
    "rest_catalog_url": "https://account-id.r2.cloudflarestorage.com/catalog",  // R2 Data Catalog endpoint
    "iceberg_s3_path": "s3://my-bucket/warehouse",                              // R2 bucket path
    "s3_endpoint": "https://account-id.r2.cloudflarestorage.com",              // R2 S3-compatible endpoint
    "aws_region": "auto",                                                       // R2 uses 'auto'
    "aws_access_key": "<R2_ACCESS_KEY>",
    "aws_secret_key": "<R2_SECRET_KEY>",
    "iceberg_db": "production",                                                 // Iceberg namespace
    "token": "<R2_API_TOKEN>",                                                  // For catalog auth
    "partition_spec": {
      "partition_by": ["year(created_at)"]                                      // Partition by year
    }
  }
}
```

#### streams.json (Generated by Discover)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "selected_streams": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "description": "List of stream names to replicate"
    },
    "streams": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "stream": {
            "type": "object",
            "properties": {
              "name": {
                "type": "string",
                "description": "Table/collection name"
              },
              "namespace": {
                "type": "string",
                "description": "Schema or database name"
              },
              "json_schema": {
                "type": "object",
                "description": "JSON Schema defining stream columns",
                "properties": {
                  "type": {
                    "const": "object"
                  },
                  "properties": {
                    "type": "object",
                    "additionalProperties": {
                      "type": "object",
                      "properties": {
                        "type": {
                          "type": ["string", "array"],
                          "description": "Column type (string, integer, number, boolean, array, object, null)"
                        },
                        "format": {
                          "type": "string",
                          "description": "Optional format (date, date-time, uuid, etc.)"
                        }
                      }
                    }
                  }
                }
              },
              "supported_sync_modes": {
                "type": "array",
                "items": {
                  "type": "string",
                  "enum": ["full_refresh", "incremental"]
                }
              }
            }
          },
          "sync_mode": {
            "type": "string",
            "enum": ["full_refresh", "incremental"],
            "description": "Selected sync mode for this stream"
          },
          "cursor_field": {
            "type": "array",
            "items": {
              "type": "string"
            },
            "description": "Columns used for incremental cursor"
          },
          "primary_key": {
            "type": "array",
            "items": {
              "type": "array",
              "items": {
                "type": "string"
              }
            },
            "description": "Primary key columns"
          }
        }
      }
    }
  }
}
```

**Annotated Example:**
```json
{
  "selected_streams": ["public.users", "public.orders"],  // Only these will be replicated
  "streams": [
    {
      "stream": {
        "name": "users",                                   // Table name
        "namespace": "public",                             // Schema
        "json_schema": {
          "type": "object",
          "properties": {
            "id": {
              "type": "integer"                            // Maps to Iceberg INT
            },
            "email": {
              "type": "string"                             // Maps to Iceberg STRING
            },
            "created_at": {
              "type": "string",
              "format": "date-time"                        // Maps to Iceberg TIMESTAMP
            },
            "metadata": {
              "type": "object"                             // Maps to Iceberg STRUCT
            }
          }
        },
        "supported_sync_modes": ["full_refresh", "incremental"]
      },
      "sync_mode": "incremental",                          // Use CDC after initial load
      "cursor_field": ["updated_at"],                      // Track changes by this column
      "primary_key": [["id"]]                              // Deduplication key
    }
  ]
}
```

### 2.2 State File Schemas

#### Pipeline State

```json
{
  "version": "1.0",
  "pipeline_id": "uuid",
  "last_sync_timestamp": "2025-01-15T10:30:00Z",
  "checkpoint": {
    "postgres": {
      "lsn": "0/3000060",                                  // PostgreSQL Log Sequence Number
      "snapshot_name": "olake-snapshot-1",
      "slot_name": "olake_slot"
    },
    "mysql": {
      "gtid": "3E11FA47-71CA-11E1-9E33-C80AA9429562:1-5", // MySQL GTID
      "binlog_file": "mysql-bin.000003",
      "binlog_position": 73
    },
    "mongodb": {
      "resume_token": "826F5D4D000000012B022C0100296E5A10...",  // MongoDB change stream token
      "cluster_time": {
        "t": 1642251600,
        "i": 1
      }
    }
  },
  "stream_states": {
    "public.users": {
      "last_processed_offset": "2025-01-15T10:29:55Z",
      "rows_synced": 125000,
      "snapshot_id": "8345678901234567890",                // Iceberg snapshot ID
      "data_files_count": 12,
      "partition_values": {
        "year": 2025
      }
    }
  }
}
```

### 2.3 Catalog Metadata Schemas

#### Iceberg Table Metadata (v2)

```json
{
  "format-version": 2,
  "table-uuid": "9c12d441-03fe-4693-9a96-a0705ddf69c1",
  "location": "s3://bucket/warehouse/db/users",
  "last-updated-ms": 1642251600000,
  "last-column-id": 5,
  "schema": {
    "type": "struct",
    "schema-id": 0,
    "fields": [
      {
        "id": 1,
        "name": "id",
        "required": true,
        "type": "int"
      },
      {
        "id": 2,
        "name": "email",
        "required": true,
        "type": "string"
      },
      {
        "id": 3,
        "name": "created_at",
        "required": false,
        "type": "timestamptz"
      },
      {
        "id": 4,
        "name": "metadata",
        "required": false,
        "type": {
          "type": "struct",
          "fields": [
            {
              "id": 5,
              "name": "preferences",
              "required": false,
              "type": "string"
            }
          ]
        }
      }
    ]
  },
  "current-schema-id": 0,
  "partition-spec": [
    {
      "name": "created_year",
      "transform": "year",
      "source-id": 3,
      "field-id": 1000
    }
  ],
  "default-spec-id": 0,
  "last-partition-id": 1000,
  "properties": {
    "write.parquet.compression-codec": "zstd",
    "write.metadata.compression-codec": "gzip"
  },
  "current-snapshot-id": 8345678901234567890,
  "refs": {
    "main": {
      "snapshot-id": 8345678901234567890,
      "type": "branch"
    }
  },
  "snapshots": [
    {
      "snapshot-id": 8345678901234567890,
      "parent-snapshot-id": 8345678901234567889,
      "timestamp-ms": 1642251600000,
      "summary": {
        "operation": "append",
        "added-data-files": "3",
        "added-records": "50000",
        "total-data-files": "15",
        "total-records": "125000"
      },
      "manifest-list": "s3://bucket/warehouse/db/users/metadata/snap-8345678901234567890.avro"
    }
  ],
  "snapshot-log": [
    {
      "timestamp-ms": 1642251600000,
      "snapshot-id": 8345678901234567890
    }
  ],
  "metadata-log": [
    {
      "timestamp-ms": 1642251600000,
      "metadata-file": "s3://bucket/warehouse/db/users/metadata/v1.metadata.json"
    }
  ]
}
```

---

## 3. Type Systems

### 3.1 PostgreSQL → Iceberg Type Mappings

| PostgreSQL Type | Iceberg Type | Parquet Type | Notes |
|-----------------|--------------|--------------|-------|
| `smallint` | `int` | `INT32` | 16-bit integer |
| `integer` | `int` | `INT32` | 32-bit integer |
| `bigint` | `long` | `INT64` | 64-bit integer |
| `real` | `float` | `FLOAT` | 32-bit floating point |
| `double precision` | `double` | `DOUBLE` | 64-bit floating point |
| `numeric(p,s)` | `decimal(p,s)` | `FIXED_LEN_BYTE_ARRAY` | Arbitrary precision decimal |
| `boolean` | `boolean` | `BOOLEAN` | True/false value |
| `char(n)` | `string` | `BYTE_ARRAY` | Fixed-length string |
| `varchar(n)` | `string` | `BYTE_ARRAY` | Variable-length string |
| `text` | `string` | `BYTE_ARRAY` | Unlimited text |
| `bytea` | `binary` | `BYTE_ARRAY` | Binary data |
| `date` | `date` | `INT32` | Days since epoch |
| `timestamp` | `timestamp` | `INT64` | Microseconds since epoch (no timezone) |
| `timestamptz` | `timestamptz` | `INT64` | Microseconds since epoch (with timezone) |
| `time` | `time` | `INT64` | Microseconds since midnight |
| `interval` | `string` | `BYTE_ARRAY` | Stored as ISO 8601 string |
| `uuid` | `uuid` | `FIXED_LEN_BYTE_ARRAY(16)` | 128-bit UUID |
| `json` | `string` | `BYTE_ARRAY` | Serialized JSON string |
| `jsonb` | `string` | `BYTE_ARRAY` | Serialized JSON string |
| `array[type]` | `list<type>` | `LIST` | Arrays map to Iceberg lists |
| `composite type` | `struct<fields>` | `STRUCT` | Custom types map to structs |
| `enum` | `string` | `BYTE_ARRAY` | Enum values as strings |
| `point` | `struct<x: double, y: double>` | `STRUCT` | Geometric point |
| `inet` | `string` | `BYTE_ARRAY` | IP address as string |
| `cidr` | `string` | `BYTE_ARRAY` | Network address as string |

### 3.2 MySQL → Iceberg Type Mappings

| MySQL Type | Iceberg Type | Parquet Type | Notes |
|------------|--------------|--------------|-------|
| `TINYINT` | `int` | `INT32` | 8-bit integer |
| `SMALLINT` | `int` | `INT32` | 16-bit integer |
| `MEDIUMINT` | `int` | `INT32` | 24-bit integer |
| `INT` | `int` | `INT32` | 32-bit integer |
| `BIGINT` | `long` | `INT64` | 64-bit integer |
| `FLOAT` | `float` | `FLOAT` | 32-bit floating point |
| `DOUBLE` | `double` | `DOUBLE` | 64-bit floating point |
| `DECIMAL(p,s)` | `decimal(p,s)` | `FIXED_LEN_BYTE_ARRAY` | Arbitrary precision decimal |
| `BIT` | `boolean` | `BOOLEAN` | Boolean value |
| `CHAR(n)` | `string` | `BYTE_ARRAY` | Fixed-length string |
| `VARCHAR(n)` | `string` | `BYTE_ARRAY` | Variable-length string |
| `TEXT` | `string` | `BYTE_ARRAY` | Long text |
| `MEDIUMTEXT` | `string` | `BYTE_ARRAY` | Medium text (16MB max) |
| `LONGTEXT` | `string` | `BYTE_ARRAY` | Long text (4GB max) |
| `BINARY(n)` | `binary` | `BYTE_ARRAY` | Fixed-length binary |
| `VARBINARY(n)` | `binary` | `BYTE_ARRAY` | Variable-length binary |
| `BLOB` | `binary` | `BYTE_ARRAY` | Binary large object |
| `DATE` | `date` | `INT32` | Date without time |
| `DATETIME` | `timestamp` | `INT64` | Date and time (no timezone) |
| `TIMESTAMP` | `timestamptz` | `INT64` | Date and time (with timezone) |
| `TIME` | `time` | `INT64` | Time without date |
| `YEAR` | `int` | `INT32` | Year value (e.g., 2025) |
| `JSON` | `string` | `BYTE_ARRAY` | Serialized JSON |
| `ENUM` | `string` | `BYTE_ARRAY` | Enum values as strings |
| `SET` | `list<string>` | `LIST` | Set stored as list |
| `GEOMETRY` | `binary` | `BYTE_ARRAY` | WKB (Well-Known Binary) format |

### 3.3 MongoDB → Iceberg Type Mappings

| MongoDB BSON Type | Iceberg Type | Parquet Type | Notes |
|-------------------|--------------|--------------|-------|
| `Double` | `double` | `DOUBLE` | 64-bit floating point |
| `String` | `string` | `BYTE_ARRAY` | UTF-8 string |
| `Object` | `struct<...>` | `STRUCT` | Nested document |
| `Array` | `list<type>` | `LIST` | Array of elements |
| `Binary Data` | `binary` | `BYTE_ARRAY` | Binary data |
| `ObjectId` | `string` | `BYTE_ARRAY` | Hex string representation |
| `Boolean` | `boolean` | `BOOLEAN` | True/false |
| `Date` | `timestamptz` | `INT64` | Milliseconds since epoch |
| `Null` | `null` | - | NULL value |
| `32-bit Integer` | `int` | `INT32` | 32-bit signed integer |
| `Timestamp` | `timestamptz` | `INT64` | BSON timestamp |
| `64-bit Integer` | `long` | `INT64` | 64-bit signed integer |
| `Decimal128` | `decimal(38,18)` | `FIXED_LEN_BYTE_ARRAY` | High-precision decimal |
| `MinKey` | `string` | `BYTE_ARRAY` | Special type (string representation) |
| `MaxKey` | `string` | `BYTE_ARRAY` | Special type (string representation) |

### 3.4 Complex Type Handling

#### Nested Objects (PostgreSQL JSONB → Iceberg Struct)

**Source (PostgreSQL):**
```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  profile JSONB
);

INSERT INTO users VALUES (1, '{"name": "Alice", "address": {"city": "NYC", "zip": "10001"}}');
```

**Destination (Iceberg Schema):**
```json
{
  "type": "struct",
  "fields": [
    {
      "id": 1,
      "name": "id",
      "required": true,
      "type": "int"
    },
    {
      "id": 2,
      "name": "profile",
      "required": false,
      "type": {
        "type": "struct",
        "fields": [
          {
            "id": 3,
            "name": "name",
            "required": false,
            "type": "string"
          },
          {
            "id": 4,
            "name": "address",
            "required": false,
            "type": {
              "type": "struct",
              "fields": [
                {
                  "id": 5,
                  "name": "city",
                  "required": false,
                  "type": "string"
                },
                {
                  "id": 6,
                  "name": "zip",
                  "required": false,
                  "type": "string"
                }
              ]
            }
          }
        ]
      }
    }
  ]
}
```

#### Arrays (PostgreSQL Array → Iceberg List)

**Source:**
```sql
CREATE TABLE products (
  id INTEGER PRIMARY KEY,
  tags TEXT[]
);

INSERT INTO products VALUES (1, ARRAY['electronics', 'laptop', 'sale']);
```

**Destination (Iceberg Schema):**
```json
{
  "type": "struct",
  "fields": [
    {
      "id": 1,
      "name": "id",
      "required": true,
      "type": "int"
    },
    {
      "id": 2,
      "name": "tags",
      "required": false,
      "type": {
        "type": "list",
        "element-id": 3,
        "element-required": false,
        "element": "string"
      }
    }
  ]
}
```

### 3.5 NULL Handling and Default Values

**Rule:** OLake preserves NULL semantics from source databases.

- **PostgreSQL:** `NULL` → Iceberg `NULL` (column must not be `required: true`)
- **MySQL:** `NULL` vs `NOT NULL` → Iceberg `required` field
- **MongoDB:** Missing fields → Iceberg `NULL`

**Default Values:**
- OLake does **not** apply default values during replication
- Default values must be handled at query time by the query engine
- Iceberg v2 spec supports default values in metadata (OLake respects this)

### 3.6 Timestamp and Timezone Conversions

**PostgreSQL:**
- `timestamp` → Iceberg `timestamp` (local time, no timezone info)
- `timestamptz` → Iceberg `timestamptz` (UTC normalized)

**MySQL:**
- `DATETIME` → Iceberg `timestamp` (local time)
- `TIMESTAMP` → Iceberg `timestamptz` (UTC converted)

**MongoDB:**
- `Date` → Iceberg `timestamptz` (stored as UTC milliseconds)

**Timezone Handling:**
OLake converts all `timestamptz` values to UTC before writing to Iceberg. Query engines handle timezone conversions for display.

---

## 4. API Contracts

### 4.1 Source Connector Interface

OLake source connectors implement a standard interface:

```python
class SourceConnector:
    """Base interface for all OLake source connectors."""
    
    def discover(self, config: Dict) -> List[Stream]:
        """
        Introspect source database and return available streams.
        
        Args:
            config: Source configuration from source.json
            
        Returns:
            List of Stream objects with schema and metadata
        """
        pass
    
    def read(
        self,
        config: Dict,
        catalog: Catalog,
        state: Optional[State]
    ) -> Generator[Record, None, State]:
        """
        Read data from source (full load or CDC).
        
        Args:
            config: Source configuration
            catalog: Selected streams and sync modes
            state: Previous pipeline state (for incremental sync)
            
        Yields:
            Record objects with data and metadata
            
        Returns:
            Updated state object
        """
        pass
    
    def check(self, config: Dict) -> CheckResult:
        """
        Test source connection and validate configuration.
        
        Args:
            config: Source configuration
            
        Returns:
            CheckResult with success status and error messages
        """
        pass
```

**Stream Object:**
```python
@dataclass
class Stream:
    name: str                          # Table/collection name
    namespace: str                     # Schema or database
    json_schema: Dict                  # JSON Schema for columns
    supported_sync_modes: List[str]    # ["full_refresh", "incremental"]
    source_defined_cursor: bool        # True if CDC available
    source_defined_primary_key: List[List[str]]  # Composite keys supported
    default_cursor_field: Optional[List[str]]     # Default cursor column
```

**Record Object:**
```python
@dataclass
class Record:
    stream: str                        # Stream name
    data: Dict[str, Any]               # Row data
    emitted_at: int                    # Timestamp (ms)
    namespace: str                     # Schema/database
    
    # CDC metadata
    source_metadata: Optional[Dict] = None  # LSN, GTID, resume_token, etc.
    operation: Optional[str] = None    # "INSERT", "UPDATE", "DELETE"
```

### 4.2 Destination Writer Interface

```python
class IcebergWriter:
    """OLake destination writer for Apache Iceberg."""
    
    def write_batch(
        self,
        stream: str,
        records: List[Record],
        catalog: IcebergCatalog,
        config: Dict
    ) -> WriteResult:
        """
        Write a batch of records to Iceberg table.
        
        Args:
            stream: Target table name
            records: Batch of records to write
            catalog: Iceberg catalog instance
            config: Destination configuration
            
        Returns:
            WriteResult with snapshot ID and metrics
        """
        pass
    
    def handle_schema_change(
        self,
        stream: str,
        old_schema: Schema,
        new_schema: Schema,
        catalog: IcebergCatalog
    ) -> None:
        """
        Apply schema evolution to Iceberg table.
        
        Args:
            stream: Target table name
            old_schema: Current Iceberg schema
            new_schema: New schema with added/modified columns
            catalog: Iceberg catalog instance
        """
        pass
    
    def create_table(
        self,
        stream: str,
        schema: Schema,
        partition_spec: PartitionSpec,
        catalog: IcebergCatalog,
        config: Dict
    ) -> Table:
        """
        Create new Iceberg table if it doesn't exist.
        
        Args:
            stream: Table name
            schema: Iceberg schema
            partition_spec: Partitioning configuration
            catalog: Iceberg catalog instance
            config: Destination configuration
            
        Returns:
            Iceberg Table object
        """
        pass
```

### 4.3 Catalog API Interactions (REST Catalog Protocol)

OLake interacts with Iceberg catalogs via the REST catalog specification:

#### List Namespaces
```http
GET /v1/namespaces HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "namespaces": [
    ["production"],
    ["staging"],
    ["dev"]
  ]
}
```

#### Create Namespace
```http
POST /v1/namespaces HTTP/1.1
Content-Type: application/json
Authorization: Bearer <token>

{
  "namespace": ["production"],
  "properties": {
    "owner": "olake",
    "created_at": "2025-01-15T10:00:00Z"
  }
}
```

#### List Tables
```http
GET /v1/namespaces/production/tables HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "identifiers": [
    {
      "namespace": ["production"],
      "name": "users"
    },
    {
      "namespace": ["production"],
      "name": "orders"
    }
  ]
}
```

#### Load Table Metadata
```http
GET /v1/namespaces/production/tables/users HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "metadata-location": "s3://bucket/warehouse/production/users/metadata/v1.metadata.json",
  "metadata": {
    "format-version": 2,
    "table-uuid": "9c12d441-03fe-4693-9a96-a0705ddf69c1",
    "location": "s3://bucket/warehouse/production/users",
    "last-updated-ms": 1642251600000,
    "schema": { "..." },
    "partition-spec": [ "..." ],
    "current-snapshot-id": 8345678901234567890,
    "snapshots": [ "..." ]
  }
}
```

#### Create Table
```http
POST /v1/namespaces/production/tables HTTP/1.1
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "users",
  "schema": {
    "type": "struct",
    "schema-id": 0,
    "fields": [
      {
        "id": 1,
        "name": "id",
        "required": true,
        "type": "int"
      },
      {
        "id": 2,
        "name": "email",
        "required": true,
        "type": "string"
      }
    ]
  },
  "partition-spec": [
    {
      "name": "created_year",
      "transform": "year",
      "source-id": 3,
      "field-id": 1000
    }
  ],
  "properties": {
    "write.parquet.compression-codec": "zstd"
  }
}
```

#### Commit Transaction (Append)
```http
POST /v1/namespaces/production/tables/users HTTP/1.1
Content-Type: application/json
Authorization: Bearer <token>

{
  "identifier": {
    "namespace": ["production"],
    "name": "users"
  },
  "requirements": [
    {
      "type": "assert-current-schema-id",
      "current-schema-id": 0
    }
  ],
  "updates": [
    {
      "action": "append",
      "manifest-list": "s3://bucket/warehouse/production/users/metadata/snap-8345678901234567890.avro"
    }
  ]
}
```

### 4.4 Monitoring/Metrics API

OLake provides metrics for monitoring pipeline health:

```python
@dataclass
class PipelineMetrics:
    """Metrics emitted by OLake pipeline."""
    
    pipeline_id: str
    source_type: str
    destination_type: str
    
    # Read metrics
    records_read: int                  # Total records read from source
    bytes_read: int                    # Total bytes read
    read_duration_ms: int              # Time spent reading
    
    # Write metrics
    records_written: int               # Records written to destination
    bytes_written: int                 # Bytes written
    write_duration_ms: int             # Time spent writing
    data_files_created: int            # Number of Parquet files
    
    # CDC metrics
    cdc_lag_ms: int                    # Lag behind source (for CDC)
    checkpoint_position: str           # Current CDC position
    
    # Error metrics
    errors_count: int                  # Number of errors
    retries_count: int                 # Number of retries
    
    # Snapshot metrics
    snapshot_id: str                   # Latest Iceberg snapshot ID
    snapshot_timestamp: int            # Snapshot commit time
```

**Example Output:**
```json
{
  "pipeline_id": "pg-to-iceberg-prod",
  "source_type": "postgres",
  "destination_type": "iceberg",
  "records_read": 50000,
  "bytes_read": 104857600,
  "read_duration_ms": 12500,
  "records_written": 50000,
  "bytes_written": 83886080,
  "write_duration_ms": 8750,
  "data_files_created": 3,
  "cdc_lag_ms": 250,
  "checkpoint_position": "0/3000060",
  "errors_count": 0,
  "retries_count": 0,
  "snapshot_id": "8345678901234567890",
  "snapshot_timestamp": 1642251600000
}
```

---

## 5. Metadata Management

### 5.1 Table Discovery Mechanisms

#### PostgreSQL Discovery

```sql
-- Discover tables in all schemas
SELECT
  schemaname AS namespace,
  tablename AS name,
  tableowner AS owner
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY schemaname, tablename;

-- Get column metadata
SELECT
  column_name,
  data_type,
  is_nullable,
  column_default,
  udt_name
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'users'
ORDER BY ordinal_position;

-- Get primary key
SELECT
  a.attname AS column_name
FROM pg_index i
JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey)
WHERE i.indrelid = 'public.users'::regclass AND i.indisprimary;
```

#### MySQL Discovery

```sql
-- Discover tables
SELECT
  TABLE_SCHEMA AS namespace,
  TABLE_NAME AS name,
  TABLE_TYPE AS type
FROM information_schema.TABLES
WHERE TABLE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
ORDER BY TABLE_SCHEMA, TABLE_NAME;

-- Get column metadata
SELECT
  COLUMN_NAME AS column_name,
  DATA_TYPE AS data_type,
  IS_NULLABLE AS is_nullable,
  COLUMN_DEFAULT AS column_default,
  COLUMN_TYPE AS column_type
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'mydb' AND TABLE_NAME = 'users'
ORDER BY ORDINAL_POSITION;

-- Get primary key
SELECT
  COLUMN_NAME AS column_name
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'mydb'
  AND TABLE_NAME = 'users'
  AND CONSTRAINT_NAME = 'PRIMARY'
ORDER BY ORDINAL_POSITION;
```

#### MongoDB Discovery

```javascript
// Discover collections
db.getCollectionNames().filter(name => !name.startsWith('system.'));

// Infer schema by sampling documents
db.users.aggregate([
  { $sample: { size: 1000 } },  // Sample 1000 documents
  { $project: {
      // Extract all field names and types
      fields: { $objectToArray: "$$ROOT" }
    }
  },
  { $unwind: "$fields" },
  { $group: {
      _id: "$fields.k",
      types: { $addToSet: { $type: "$fields.v" } },
      count: { $sum: 1 }
    }
  }
]);
```

### 5.2 Schema Inference and Evolution

#### Inference

OLake infers schemas by:
1. **Introspection:** Query source system metadata (information_schema, DESCRIBE, etc.)
2. **Sampling:** For schema-less sources (MongoDB), sample documents to infer types
3. **Type Promotion:** If column has mixed types, promote to most general type (e.g., `int` + `string` → `string`)

#### Evolution

**Supported Changes:**
- **Add Column:** New column with `NULL` default
- **Rename Column:** Via Iceberg metadata (old column deprecated)
- **Type Promotion:** Widen type (e.g., `int` → `long`)

**Unsupported Changes (Require Manual Intervention):**
- **Drop Column:** Must be handled outside OLake
- **Type Demotion:** Narrowing type (e.g., `long` → `int`)
- **Change Nullability:** Making `required` column `optional` (or vice versa)

**Example: Add Column**

**Source Change (PostgreSQL):**
```sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

**OLake Detects Change:**
```json
{
  "stream": "public.users",
  "schema_change": {
    "type": "add_column",
    "column_name": "phone",
    "column_type": "string",
    "nullable": true
  }
}
```

**Iceberg Schema Update:**
```python
table.update_schema().add_column(
    "phone",
    StringType(),
    doc="User phone number"
).commit()
```

**Result:**
- Existing Parquet files remain unchanged
- New files include `phone` column
- Queries return `NULL` for `phone` in old rows

### 5.3 Partition Metadata

Iceberg partitioning organizes data files for efficient querying.

**Partition Spec Example:**
```json
{
  "spec-id": 0,
  "fields": [
    {
      "source-id": 3,                  // Column ID for 'created_at'
      "field-id": 1000,
      "name": "created_year",
      "transform": "year"              // Transform function
    },
    {
      "source-id": 4,                  // Column ID for 'region'
      "field-id": 1001,
      "name": "region",
      "transform": "identity"          // No transform, use value as-is
    }
  ]
}
```

**Partition Values:**
```
s3://bucket/warehouse/production/users/
├── metadata/
│   ├── v1.metadata.json
│   └── snap-*.avro
└── data/
    ├── created_year=2024/
    │   ├── region=us-east/
    │   │   ├── data-00001.parquet
    │   │   └── data-00002.parquet
    │   └── region=us-west/
    │       └── data-00003.parquet
    └── created_year=2025/
        └── region=us-east/
            └── data-00004.parquet
```

**Partition Pruning:**
Query engines use partition metadata to skip irrelevant files:

```sql
-- Query only 2025 data in us-east
SELECT * FROM users
WHERE created_year = 2025 AND region = 'us-east';

-- Iceberg reads only: data/created_year=2025/region=us-east/*.parquet
-- Skips: 2024 data, us-west data
```

### 5.4 Lineage Tracking (Source → Lake Mapping)

OLake maintains lineage metadata linking source to destination:

```json
{
  "lineage": {
    "source": {
      "type": "postgres",
      "host": "postgres.example.com",
      "database": "production",
      "schema": "public",
      "table": "users"
    },
    "destination": {
      "type": "iceberg",
      "catalog": "rest",
      "namespace": "production",
      "table": "users",
      "location": "s3://bucket/warehouse/production/users"
    },
    "pipeline": {
      "id": "pg-to-iceberg-users",
      "created_at": "2025-01-01T00:00:00Z",
      "sync_mode": "incremental"
    },
    "mappings": [
      {
        "source_column": "id",
        "destination_column": "id",
        "type_mapping": "integer -> int"
      },
      {
        "source_column": "email",
        "destination_column": "email",
        "type_mapping": "varchar -> string"
      },
      {
        "source_column": "created_at",
        "destination_column": "created_at",
        "type_mapping": "timestamptz -> timestamptz"
      }
    ],
    "transformations": []               // OLake does not transform data
  }
}
```

**Accessing Lineage:**
- Stored in Iceberg table properties: `table.properties()['olake.source.database']`
- Queryable via Iceberg metadata tables
- Integrated with data catalogs (DataHub, Amundsen) via REST API

---

## 6. Semantic Layers

### 6.1 Business Logic Embedded in OLake

OLake is designed as a **pure replication tool** with minimal business logic:

**What OLake Does:**
- ✅ Type mapping (database types → Iceberg types)
- ✅ Schema inference and evolution
- ✅ CDC position tracking
- ✅ Exactly-once delivery semantics
- ✅ Partitioning based on configuration

**What OLake Does NOT Do:**
- ❌ Data transformations (filtering, aggregation, enrichment)
- ❌ Data quality checks (validation, deduplication)
- ❌ Business rule enforcement
- ❌ PII masking or encryption

**Rationale:**
OLake follows the **ELT (Extract-Load-Transform)** paradigm:
- Extract from source
- Load into lake
- Transform in query engines (DuckDB, Trino, Spark, etc.)

This keeps OLake simple, fast, and composable.

### 6.2 Data Quality Rules

Data quality should be enforced **downstream** of OLake:

#### Using Great Expectations
```python
import great_expectations as gx

# Define expectations on Iceberg table
context = gx.get_context()
suite = context.add_or_update_expectation_suite("users_suite")

suite.add_expectation(
    gx.core.ExpectationConfiguration(
        expectation_type="expect_column_values_to_not_be_null",
        kwargs={"column": "email"}
    )
)

suite.add_expectation(
    gx.core.ExpectationConfiguration(
        expectation_type="expect_column_values_to_match_regex",
        kwargs={
            "column": "email",
            "regex": r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
        }
    )
)

# Validate Iceberg table
batch_request = {
    "datasource_name": "iceberg_datasource",
    "data_connector_name": "default_inferred_data_connector_name",
    "data_asset_name": "users"
}

validator = context.get_validator(
    batch_request=batch_request,
    expectation_suite_name="users_suite"
)

results = validator.validate()
```

#### Using SQL Views (Downstream)
```sql
-- Create validated view in DuckDB/Trino
CREATE VIEW validated_users AS
SELECT *
FROM iceberg_scan('s3://bucket/warehouse/production/users')
WHERE
  email IS NOT NULL
  AND email LIKE '%@%'
  AND created_at >= '2020-01-01'
  AND id > 0;
```

### 6.3 Transformation Capabilities

OLake supports **limited transformations** via configuration:

#### Partition Transform
```json
{
  "destination": {
    "writer": {
      "partition_spec": {
        "partition_by": [
          "year(created_at)",            // Extract year from timestamp
          "bucket(10, user_id)"          // Hash user_id into 10 buckets
        ]
      }
    }
  }
}
```

**Supported Transforms:**
- `year(column)` - Extract year
- `month(column)` - Extract month
- `day(column)` - Extract day
- `hour(column)` - Extract hour
- `bucket(N, column)` - Hash into N buckets
- `truncate(width, column)` - Truncate string to width
- `identity(column)` - Use value as-is

#### Column Selection
```json
{
  "streams": [
    {
      "stream": {
        "name": "users",
        "json_schema": { "..." }
      },
      "selected_columns": ["id", "email", "created_at"],  // Only replicate these
      "excluded_columns": ["password_hash"]                // Explicitly exclude
    }
  ]
}
```

#### Advanced Transformations

For complex transformations, integrate with downstream tools:

**Using SQLMesh:**
```sql
-- models/cleaned_users.sql
MODEL (
  name production.cleaned_users,
  kind INCREMENTAL_BY_TIME_RANGE (
    time_column created_at
  )
);

SELECT
  id,
  LOWER(TRIM(email)) AS email,              -- Normalize email
  created_at,
  CASE
    WHEN region IS NULL THEN 'unknown'
    ELSE region
  END AS region
FROM iceberg_scan('s3://bucket/warehouse/production/users')
WHERE
  created_at BETWEEN @start_date AND @end_date
  AND email IS NOT NULL;
```

**Using Ibis (Python):**
```python
import ibis

con = ibis.duckdb.connect()
con.execute("INSTALL iceberg; LOAD iceberg;")

# Read Iceberg table
users = con.read_iceberg('s3://bucket/warehouse/production/users')

# Transform
cleaned_users = (
    users
    .mutate(
        email=users.email.lower().strip(),
        region=users.region.fillna('unknown')
    )
    .filter(users.email.notnull())
)

# Write to new Iceberg table
con.create_table('production.cleaned_users', cleaned_users)
```

---

## 7. Entity-Relationship Diagram (Text Format)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         OLake Complete Ontology                         │
└─────────────────────────────────────────────────────────────────────────┘

┌───────────────────┐
│  SourceDatabase   │
├───────────────────┤
│ - host            │
│ - port            │
│ - database        │
│ - username        │
│ - password        │
│ - ssl_config      │
│ - cdc_config      │
└─────────┬─────────┘
          │ contains
          │ 1:N
          ▼
┌───────────────────┐         ┌───────────────────┐
│     Stream        │────────>│  Column           │
├───────────────────┤ has     ├───────────────────┤
│ - name            │ 1:N     │ - name            │
│ - namespace       │         │ - data_type       │
│ - sync_mode       │         │ - is_nullable     │
│ - cursor_field    │         │ - is_primary_key  │
│ - primary_key     │         │ - ordinal_pos     │
└─────────┬─────────┘         └───────────────────┘
          │ replicated by
          │ N:1
          ▼
┌───────────────────┐
│    Pipeline       │
├───────────────────┤
│ - id              │
│ - source_config   │
│ - dest_config     │
│ - stream_catalog  │
│ - created_at      │
│ - updated_at      │
└─────────┬─────────┘
          │ maintains
          │ 1:1
          ▼
┌───────────────────┐
│      State        │
├───────────────────┤
│ - checkpoint      │
│   - lsn (PG)      │
│   - gtid (MySQL)  │
│   - token (Mongo) │
│ - stream_states   │
│ - last_sync_time  │
└───────────────────┘
          │ creates
          │ 1:N
          ▼
┌───────────────────┐         ┌───────────────────┐
│   IcebergTable    │────────>│   Snapshot        │
├───────────────────┤ has     ├───────────────────┤
│ - namespace       │ 1:N     │ - id              │
│ - name            │         │ - parent_id       │
│ - location        │         │ - timestamp       │
│ - uuid            │         │ - manifest_list   │
│ - schema_id       │         │ - summary         │
│ - partition_spec  │         └─────────┬─────────┘
└─────────┬─────────┘                   │ references
          │ composed of                 │ N:M
          │ 1:N                         ▼
          ▼                   ┌───────────────────┐
┌───────────────────┐         │   DataFile        │
│   IcebergSchema   │         ├───────────────────┤
├───────────────────┤         │ - path            │
│ - schema_id       │         │ - format          │
│ - fields          │         │ - partition       │
│   - id            │         │ - record_count    │
│   - name          │         │ - file_size       │
│   - type          │         │ - column_sizes    │
│   - required      │         │ - value_counts    │
└───────────────────┘         │ - null_counts     │
          │                   │ - lower_bounds    │
          │ defines           │ - upper_bounds    │
          │ 1:1               └───────────────────┘
          ▼
┌───────────────────┐
│ PartitionSpec     │
├───────────────────┤
│ - spec_id         │
│ - fields          │
│   - source_id     │
│   - field_id      │
│   - name          │
│   - transform     │
└───────────────────┘
          │ applied to
          │ 1:N
          ▼
┌───────────────────┐
│  Partition        │
├───────────────────┤
│ - spec_id         │
│ - field_values    │
│   - year=2025     │
│   - region=us     │
└─────────┬─────────┘
          │ contains
          │ 1:N
          ▼
┌───────────────────┐
│   DataFile        │
│   (referenced)    │
└───────────────────┘

┌───────────────────┐
│  IcebergCatalog   │
├───────────────────┤
│ - type            │
│   - REST          │
│   - JDBC          │
│   - Glue          │
│   - Hive          │
│ - endpoint        │
│ - credentials     │
└─────────┬─────────┘
          │ manages
          │ 1:N
          ▼
┌───────────────────┐
│   Namespace       │
├───────────────────┤
│ - name            │
│ - properties      │
└─────────┬─────────┘
          │ contains
          │ 1:N
          ▼
┌───────────────────┐
│   IcebergTable    │
│   (referenced)    │
└───────────────────┘
```

---

## 8. Summary and Key Takeaways

### 8.1 Core Concepts

**OLake is:**
- A **CDC replication tool** for moving data from operational databases to data lakes
- **Schema-agnostic**: Automatically infers and evolves schemas
- **ACID-compliant**: Leverages Iceberg for transactional guarantees
- **Catalog-flexible**: Supports REST, JDBC, Glue, Hive catalogs
- **Exactly-once**: Ensures data consistency with checkpointing

**OLake is NOT:**
- A transformation engine (use SQLMesh, dbt, Ibis)
- A data quality tool (use Great Expectations, dbt tests)
- A query engine (use DuckDB, Trino, Spark)

### 8.2 Configuration Pattern

```
source.json → discover → streams.json → sync → destination
                                              → state.json
```

### 8.3 Type Mapping Principles

1. **Preserve Semantics:** NULL, precision, timezone info maintained
2. **Promote Types:** Widen types when necessary (int → long)
3. **Serialize Complex:** JSON, arrays, structs preserved as Iceberg types
4. **Normalize Time:** All timestamps converted to UTC

### 8.4 API Contracts

- **Source Interface:** `discover()`, `read()`, `check()`
- **Destination Interface:** `write_batch()`, `handle_schema_change()`, `create_table()`
- **Catalog API:** Iceberg REST catalog specification (OpenAPI)

### 8.5 Metadata Management

- **Discovery:** Introspect source databases
- **Inference:** Sample data to determine types
- **Evolution:** Add columns, promote types automatically
- **Lineage:** Track source-to-destination mappings

### 8.6 Best Practices

1. **Enable CDC:** Use logical replication for low-latency sync
2. **Partition Wisely:** Partition by time or high-cardinality columns
3. **Monitor Lag:** Track CDC lag to ensure freshness
4. **Validate Downstream:** Enforce data quality in query layer
5. **Version Control:** Use LakeFS to version Iceberg tables
6. **Separate Concerns:** Transform data downstream (ELT pattern)

### 8.7 Integration Points

**Upstream:**
- PostgreSQL, MySQL, MongoDB, Oracle
- Kafka (future)

**Downstream:**
- Iceberg (primary format)
- Query engines: DuckDB, Trino, Spark, Flink, RisingWave
- Data catalogs: DataHub, Amundsen, OpenMetadata
- BI tools: Superset, Metabase, Tableau

**Orchestration:**
- Dagster (asset-based)
- Apache Airflow
- Prefect

---

## 9. References and Resources

### Official Documentation
- **OLake Docs:** https://olake.io/docs
- **Apache Iceberg:** https://iceberg.apache.org
- **REST Catalog Spec:** https://iceberg.apache.org/rest-catalog-spec/
- **REST Catalog OpenAPI:** https://github.com/apache/iceberg/blob/main/open-api/rest-catalog-open-api.yaml

### Type Mapping References
- **PostgreSQL Types:** https://www.postgresql.org/docs/current/datatype.html
- **MySQL Types:** https://dev.mysql.com/doc/refman/8.0/en/data-types.html
- **MongoDB BSON Types:** https://www.mongodb.com/docs/manual/reference/bson-types/
- **Iceberg Types:** https://iceberg.apache.org/spec/#schemas

### Repository
- **OLake GitHub:** https://github.com/datazip-inc/olake

### Related Projects
- **Lakekeeper:** https://docs.lakekeeper.io
- **LakeFS:** https://lakefs.io
- **DuckLake:** https://ducklake.select

---

## Appendix A: Example Configurations

### A.1 PostgreSQL to Iceberg (Full Setup)

**source.json:**
```json
{
  "host": "postgres.example.com",
  "port": 5432,
  "database": "production",
  "username": "olake_user",
  "password": "<PASSWORD>",
  "ssl": {
    "mode": "require"
  },
  "update_method": {
    "replication_slot": "olake_slot",
    "publication": "olake_publication",
    "initial_wait_time": 120
  },
  "max_threads": 5
}
```

**destination.json:**
```json
{
  "type": "ICEBERG",
  "writer": {
    "catalog_type": "rest",
    "rest_catalog_url": "https://lakekeeper.example.com/catalog",
    "iceberg_s3_path": "s3://my-bucket/warehouse",
    "s3_endpoint": "https://s3.amazonaws.com",
    "aws_region": "us-east-1",
    "aws_access_key": "<ACCESS_KEY>",
    "aws_secret_key": "<SECRET_KEY>",
    "iceberg_db": "production",
    "token": "<CATALOG_TOKEN>",
    "partition_spec": {
      "partition_by": ["year(created_at)"]
    }
  }
}
```

**streams.json (generated):**
```json
{
  "selected_streams": ["public.users", "public.orders"],
  "streams": [
    {
      "stream": {
        "name": "users",
        "namespace": "public",
        "json_schema": {
          "type": "object",
          "properties": {
            "id": {"type": "integer"},
            "email": {"type": "string"},
            "created_at": {"type": "string", "format": "date-time"}
          }
        },
        "supported_sync_modes": ["full_refresh", "incremental"]
      },
      "sync_mode": "incremental",
      "cursor_field": ["updated_at"],
      "primary_key": [["id"]]
    }
  ]
}
```

### A.2 MySQL to Iceberg (Cloudflare R2)

**source.json:**
```json
{
  "host": "mysql.example.com",
  "port": 3306,
  "database": "ecommerce",
  "username": "olake_user",
  "password": "<PASSWORD>",
  "ssl": {
    "mode": "required"
  },
  "update_method": {
    "method": "binlog",
    "server_id": 12345
  },
  "max_threads": 8
}
```

**destination.json:**
```json
{
  "type": "ICEBERG",
  "writer": {
    "catalog_type": "rest",
    "rest_catalog_url": "https://account-id.r2.cloudflarestorage.com/catalog",
    "iceberg_s3_path": "s3://my-r2-bucket/warehouse",
    "s3_endpoint": "https://account-id.r2.cloudflarestorage.com",
    "aws_region": "auto",
    "aws_access_key": "<R2_ACCESS_KEY>",
    "aws_secret_key": "<R2_SECRET_KEY>",
    "iceberg_db": "ecommerce",
    "token": "<R2_API_TOKEN>"
  }
}
```

### A.3 MongoDB to Iceberg

**source.json:**
```json
{
  "connection_string": "mongodb://user:pass@mongo.example.com:27017/mydb?replicaSet=rs0",
  "database": "mydb",
  "update_method": {
    "method": "change_stream",
    "resume_token": null
  },
  "max_threads": 4
}
```

**destination.json:**
```json
{
  "type": "ICEBERG",
  "writer": {
    "catalog_type": "jdbc",
    "jdbc_url": "jdbc:postgresql://catalog-db.example.com:5432/iceberg_catalog",
    "jdbc_username": "iceberg_user",
    "jdbc_password": "<PASSWORD>",
    "iceberg_s3_path": "s3://my-bucket/warehouse",
    "aws_region": "us-west-2",
    "aws_access_key": "<ACCESS_KEY>",
    "aws_secret_key": "<SECRET_KEY>",
    "iceberg_db": "mongodb_data"
  }
}
```

---

## Appendix B: Command Reference

### Discover Command
```bash
docker run --rm \
  -v /path/to/config:/mnt/config \
  olakego/source-postgres:latest \
  discover \
  --config /mnt/config/source.json \
  > /path/to/config/streams.json
```

### Sync Command (Full Load + CDC)
```bash
docker run --rm \
  -v /path/to/config:/mnt/config \
  olakego/source-postgres:latest \
  sync \
  --config /mnt/config/source.json \
  --catalog /mnt/config/streams.json \
  --destination /mnt/config/destination.json \
  --state /mnt/config/state.json
```

### Check Connection
```bash
docker run --rm \
  -v /path/to/config:/mnt/config \
  olakego/source-postgres:latest \
  check \
  --config /mnt/config/source.json
```

---

**Document Version:** 1.0  
**Last Updated:** 2025-01-18  
**Author:** AI Research Assistant  
**Status:** Complete
