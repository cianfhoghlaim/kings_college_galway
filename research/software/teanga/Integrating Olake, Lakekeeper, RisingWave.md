# **Architecting the Real-Time Open Data Lakehouse: A Comprehensive Technical Analysis of Integrating OLake, Lakekeeper, and RisingWave**

## **Executive Summary**

The enterprise data landscape is currently navigating a critical inflection point, transitioning from rigid, high-latency batch processing systems toward fluid, real-time architectures. This shift is characterized by the adoption of the "Lakehouse" paradigm, which seeks to unify the massive scalability and cost-efficiency of data lakes with the transactional integrity, governance, and performance of traditional data warehouses. However, the first generation of lakehouse implementations often relied on a fragmented assembly of legacy components—heavy Java-based ingestion tools like Debezium, centralized bottlenecks like the Hive Metastore (HMS), and high-latency batch engines like Apache Spark. While functional, these stacks frequently fail to deliver the low-latency data freshness and operational simplicity required by modern digital businesses.  
This report presents an exhaustive technical analysis and implementation strategy for a "second-generation" open data lakehouse stack. We propose and detail the integration of three emerging, high-performance technologies: **OLake** for ultra-fast, log-based Change Data Capture (CDC) and ingestion; **Lakekeeper** for secure, Rust-native metadata management via the Apache Iceberg REST protocol; and **RisingWave** for streaming analytics and materialized views. By synthesizing these components, organizations can construct a data platform that eliminates the "GC pauses" and memory overhead of JVM-based legacy stacks, enforces strict governance through distinct control planes, and delivers sub-second data freshness from transactional sources to analytical endpoints.  
Through a rigorous examination of architectural internals, configuration specifications, and operational mechanics, this document serves as a definitive guide for data architects and engineers tasked with building high-velocity, vendor-agnostic data infrastructure. We explore the mechanical interplay between database transaction logs (WAL, Binlog, Oplog), atomic metadata commits in the Iceberg tree, and the stateful stream processing capabilities of RisingWave, providing a blueprint for a system that is not only faster but fundamentally more robust and easier to manage than its predecessors.

## ---

**1\. The Modern Data Stack Crisis and the Open Lakehouse Solution**

### **1.1 Deconstructing the Legacy Bottlenecks**

To understand the necessity of the OLake-Lakekeeper-RisingWave stack, one must first rigorously diagnose the ailments of the prevailing architectures. The traditional "Modern Data Stack" (MDS) has ironically become a source of significant technical debt. Ingestion pipelines built on tools like Debezium, while pioneering, introduce substantial operational complexity. Debezium relies heavily on the Java Virtual Machine (JVM) and typically mandates an external message broker like Apache Kafka to buffer changes.1 This architecture creates a "heavy" footprint: the JVM requires careful tuning of heap sizes to avoid Garbage Collection (GC) pauses that induce latency spikes, while Kafka introduces management overhead for topics, partitions, and offset tracking. Furthermore, specific limitations, such as the 16MB document size cap in Debezium’s MongoDB connector, pose hard constraints for applications dealing with rich, nested data structures.1  
Simultaneously, the metadata layer has suffered from the inertia of the Hive Metastore (HMS). Originally designed for the batch-oriented Hadoop era, HMS struggles with the high-concurrency requirements of modern object-store-based lakes. It lacks native support for the atomic, multi-table transactions that are the hallmark of Apache Iceberg. As data volumes grow, the HMS becomes a centralized contention point, slowing down query planning and commit operations.

### **1.2 The Convergence of Streaming and Storage**

The solution lies in the convergence of streaming processing and open table formats. The "Open Data Lakehouse" is defined by the decoupling of compute and storage, mediated by an open standard for table metadata—Apache Iceberg. Iceberg provides ACID (Atomicity, Consistency, Isolation, Durability) guarantees on top of immutable object storage (S3, GCS, Azure Blob), enabling multiple engines to operate on the same data safely.  
The proposed architecture represents a radical optimization of this model:

* **Ingestion (OLake):** Shifts from heavy JVM-based ETL to a lightweight, Go-based ELT framework. It focuses on maximizing throughput via parallelization and minimizing resource footprint, bypassing intermediate message brokers where possible to write directly to the lake.2  
* **Governance (Lakekeeper):** Replaces the legacy HMS with a high-performance, Rust-based implementation of the Iceberg REST Catalog. This layer introduces strict contract enforcement, security via vended credentials, and low-latency metadata resolution.4  
* **Compute (RisingWave):** Transitions from batch-based SQL engines (like Spark SQL) to a streaming database. RisingWave treats Iceberg tables not just as static archives but as dynamic sources and sinks, enabling continuous materialized views that are always up-to-date.6

This triad creates a "Golden Path" for data: changes occur in the operational database, are instantly captured and committed to the lake by OLake, governed by Lakekeeper, and immediately processed and served by RisingWave.

## ---

**2\. OLake: Engineering High-Velocity Ingestion**

OLake distinguishes itself as a purpose-built tool for database-to-lakehouse replication. Unlike generic ETL tools that attempt to be "jacks of all trades," OLake is engineered specifically to exploit the mechanics of modern databases and the Iceberg format to achieve maximum throughput. Written in Go, it avoids the memory management overhead of Java, positioning itself as a leaner, faster alternative to Debezium.8

### **2.1 Architectural Internals and the Protocol Layer**

At the core of OLake lies a sophisticated **Protocol Layer** that orchestrates data movement. This layer is designed to be modular, separating the logic of *extraction* (Drivers) from *loading* (Writers).3 The design philosophy emphasizes maintaining data fidelity while maximizing parallelism.

#### **2.1.1 Parallelized Snapshotting and Chunking Strategies**

A critical weakness of many replication tools is the "initial snapshot" phase—the process of copying existing data before switching to CDC. Single-threaded snapshots on large tables (e.g., terabytes of data) can take days. OLake addresses this through **Parallelized Chunking**, splitting source tables into virtual segments that are processed concurrently.2  
The strategy for chunking varies by database engine to optimize for the underlying storage layout:

* **PostgreSQL (Physical Block Splitting):** OLake leverages the CTID (tuple identifier), which represents the physical location of a row (block number and tuple index). By splitting ranges based on CTID, OLake can read distinct physical pages from the disk in parallel, avoiding the high cost of logical OFFSET queries which require scanning and discarding rows.3  
* **MySQL (Key-Range Splitting):** For MySQL, which organizes data in B-Trees (InnoDB), OLake utilizes range splits based on the Primary Key. This allows it to issue queries like SELECT \* FROM table WHERE pk \>= X AND pk \< Y, which the database can answer efficiently using index seeks.2  
* **MongoDB (Vector Splitting):** In distributed databases like MongoDB, OLake employs commands like Split-Vector or Bucket-Auto to determine balanced partition boundaries, ensuring that worker threads receive roughly equal data volumes.2

This parallel architecture allows OLake to saturate the available network bandwidth and I/O capacity. Benchmarks indicate that this approach can yield sync speeds exceeding 300,000 rows per second, drastically outperforming standard connectors.8

#### **2.1.2 The Mechanics of Log-Based CDC**

Once the snapshot is complete, OLake transitions to Change Data Capture (CDC) to maintain synchronization. This is achieved by tapping into the database's immutable transaction log.

* **PostgreSQL (pgoutput):** OLake functions as a logical replication consumer. It connects to a **Replication Slot** and consumes the Write-Ahead Log (WAL) stream via the pgoutput plugin.9 This requires the creation of a **Publication** (CREATE PUBLICATION...) on the source, which defines the scope of data to be broadcast. The pgoutput plugin decodes the low-level WAL entries into logical change events (INSERT, UPDATE, DELETE) which OLake then serializes.  
* **MySQL (Binlog):** OLake acts as a slave instance, connecting to the MySQL master and requesting the binary log stream. It requires the binlog format to be set to ROW (binlog\_format=ROW) and the image to be FULL (binlog\_row\_image=FULL) to ensure that both the "before" and "after" images of updated rows are captured.10 This fidelity is crucial for Iceberg, which may require the previous values to perform equality deletes efficiently.  
* **MongoDB (Oplog):** OLake tails the Operations Log (oplog), a special capped collection that records all modifications to the data. Unlike Debezium, which converts BSON to a generic internal struct (often triggering memory issues with large documents), OLake maintains the native BSON structure as far as possible during the extraction phase, effectively handling documents larger than 16MB.1

### **2.2 Configuration Architecture**

OLake employs a declarative configuration model using JSON files. This approach supports "Infrastructure as Code" (IaC) principles, allowing data engineers to version control their pipelines.

#### **2.2.1 Source Configuration (source.json)**

The source.json file encapsulates all connection parameters and tuning knobs for the source database. For a PostgreSQL source, the configuration allows for granular control over the replication behavior.  
Table 1: Detailed Parameter Analysis of source.json for PostgreSQL 9

| Parameter Category | Parameter | Description & Operational Implication |
| :---- | :---- | :---- |
| **Connection** | host, port, database | Standard connection details. |
|  | jdbc\_url\_params | A map for driver-specific tuning (e.g., connectTimeout, tcpKeepAlive). Crucial for maintaining long-lived replication connections in unstable network environments. |
| **Replication** | update\_method.replication\_slot | The name of the persistent replication slot on the Postgres server. This slot ensures the server retains WAL segments until OLake acknowledges them. |
|  | update\_method.publication | The name of the publication (e.g., olake\_pub). This acts as a filter on the source, determining which tables emit events. |
|  | update\_method.initial\_wait\_time | Time (in seconds) to wait before retrying a connection, handling transient network partitions. |
| **Concurrency** | max\_threads | Defines the parallelism for the initial snapshot. Setting this too high can overwhelm the source DB's I/O; setting it too low underutilizes bandwidth. |
| **Security** | ssl.mode | modes like disable, require, verify-ca, verify-full. Essential for securing data in transit over public networks. |
| **Tunnels** | ssh\_config | Native support for SSH tunneling (host, username, key), allowing OLake to connect to databases in private VPCs without exposing them publicly. |

#### **2.2.2 Destination Configuration (destination.json)**

To integrate with **Lakekeeper**, the destination must be configured to use the generic Iceberg REST catalog interface. While OLake supports specific implementations like AWS Glue, the REST configuration is the standard for open interoperability.  
Table 2: Configuration Parameters for REST Catalog Integration 12

| Parameter | Recommended Value (Context) | Technical Explanation |
| :---- | :---- | :---- |
| type | "ICEBERG" | Declares the top-level writer implementation. |
| writer.catalog\_type | "rest" | Specifies adherence to the Apache Iceberg REST OpenAPI specification, enabling communication with Lakekeeper. |
| writer.uri | http://lakekeeper:8181/catalog/ | The endpoint where Lakekeeper is listening. Note the /catalog/ suffix which is standard for the REST spec. |
| writer.iceberg\_s3\_path | s3://warehouse/ | The "warehouse" root location. Lakekeeper uses this as the base for resolving table locations. |
| writer.io\_impl | org.apache.iceberg.aws.s3.S3FileIO | The Java class used for S3 interaction. This is critical; using the Hadoop S3A file system is often slower and less compatible than the native Iceberg S3FileIO. |
| writer.s3\_path\_style | true | **Crucial for MinIO.** Forces the client to use host/bucket addressing instead of bucket.host DNS addressing, which often fails in local Docker networks. |
| writer.auth.type | "oauth2" (or none) | If Lakekeeper is secured, this configures the bearer token flow. |

#### **2.2.3 Discovery and Stream Mapping (streams.json)**

Before synchronization begins, OLake executes a discover command. This inspects the source database schema and generates a streams.json file.14

* **Schema Normalization:** OLake automatically maps source types (e.g., Postgres TIMESTAMPTZ) to Iceberg types (Timestamp).  
* **Partitioning Definition:** The streams.json file allows users to define partition strategies using regex or explicit column names (e.g., partition\_regex: "/{created\_at, month}"). This instructs OLake to physically organize the Parquet files in the destination by these partitions, which is vital for downstream query performance (partition pruning).14

### **2.3 Resiliency and State Management**

OLake is designed to be fault-tolerant. It maintains a local cursor file, state.json, which records the exact position in the transaction log (LSN for Postgres, Binlog filename/offset for MySQL) that has been successfully committed to Iceberg.9

* **Exactly-Once Semantics:** In the event of a crash, OLake restarts, reads the state.json, and resumes consumption from the last checkpoint. Because Iceberg commits are atomic, there is no risk of partial data or corruption.  
* **CDC Cursor Preservation:** A sophisticated feature of OLake is its ability to handle the addition of new tables without disrupting existing streams. If a user adds a new table to the streams.json configuration, OLake triggers a background snapshot for that specific table while continuing to process CDC events for the others. This avoids the operational nightmare of "resetting the world" to add a single dataset.8

## ---

**3\. Lakekeeper: The Governance and Metadata Control Plane**

While OLake handles the physical movement of data bytes, **Lakekeeper** manages the "truth" of the data. Lakekeeper is a modern, high-performance implementation of the Apache Iceberg REST Catalog, written entirely in Rust.4 Its role in this stack is to serve as the authoritative metadata store, governing access, enforcing schema consistency, and creating a unified view of the data lake.

### **3.1 The Rust Architecture Advantage**

The choice of Rust for Lakekeeper is architectural, not merely stylistic. Catalog services in a data lakehouse are high-concurrency metadata servers. Every time a query engine (like Trino or RisingWave) plans a query, and every time an ingestion tool (like OLake) commits a batch, they must interact with the catalog.

* **Latency Determinism:** JVM-based catalogs (like the reference Java REST catalog) are susceptible to Garbage Collection pauses, which can introduce unpredictable latency spikes during high-load commit storms. Lakekeeper's Rust foundation ensures predictable, low-latency responses, which is critical for maintaining the "real-time" feel of the lakehouse.5  
* **Memory Safety and Efficiency:** Lakekeeper compiles to a single binary with a minimal memory footprint. This efficiency allows it to be deployed as a sidecar or in dense Kubernetes clusters without the resource bloating associated with Hadoop-era services (e.g., Hive Metastore).4

### **3.2 Entity Hierarchy and Multi-Tenancy**

Lakekeeper introduces a structured entity hierarchy that extends the basic Iceberg concepts to support enterprise-grade multi-tenancy.15

1. **Server:** The root instance of the application.  
2. **Project:** A logical isolation boundary (e.g., "Finance", "Engineering"). This allows a single Lakekeeper deployment to serve multiple independent teams without name collisions or security cross-talk.  
3. **Warehouse:** Represents a specific storage backend configuration (e.g., an S3 bucket). Lakekeeper strictly enforces isolation at this level; credentials for one warehouse cannot access data in another. This prevents the "leaky abstraction" problems often found in simple catalogs.  
4. **Namespace:** Hierarchical grouping of tables (e.g., sales.regional.uk).  
5. **Table/View:** The leaf nodes—the actual Iceberg tables.

### **3.3 Security: Vended Credentials and OpenFGA**

Lakekeeper fundamentally upgrades the security model of the data lake through **Credential Vending** and **Remote Signing**.4

#### **3.3.1 The Security Gap in Traditional Lakes**

In a standard S3-based data lake, any compute engine (Spark, Trino) effectively needs "god mode" access (long-term AWS Access Keys) to the S3 bucket to read and write files. If a compute worker is compromised, the entire lake is at risk.

#### **3.3.2 The Vended Credentials Solution**

Lakekeeper acts as a security broker. When a client (like RisingWave) requests access to a table:

1. The client authenticates with Lakekeeper (via OAuth2/OIDC).  
2. Lakekeeper verifies permissions.  
3. Lakekeeper interacts with the storage provider (e.g., AWS STS) to assume a role and generate **short-lived, scoped credentials**.  
4. These temporary credentials, which grant access only to the specific prefix of the requested table, are returned to the client.  
   This "Table-Level Access Control" brings database-like security granualarity to object storage.

#### **3.3.3 Fine-Grained Authorization (OpenFGA)**

Lakekeeper integrates with OpenFGA (Open Fine-Grained Authorization) to implement Relationship-Based Access Control (ReBAC). Instead of simple RBAC roles, architects can define complex policies (e.g., "A user can read this table if they are an owner of the parent project OR if they are in the auditor group"). This externalizes authorization logic, allowing it to be audited and managed centrally.4

### **3.4 Operational Bootstrapping**

Lakekeeper is "self-hosted" and requires explicit initialization. The bootstrapping process sets up the initial administrative user and default project structure.

* **Bootstrap Command:** POST /management/v1/bootstrap initializes the system, accepting terms and creating the root user.  
* **Warehouse Creation:** The warehouse must be defined with its specific storage profile. This tells Lakekeeper how to generate the vended credentials (e.g., which S3 bucket and region to use).16

## ---

**4\. RisingWave: The Streaming Compute Engine**

**RisingWave** completes the stack by providing the compute capability. It is a distributed SQL streaming database that is fully compatible with PostgreSQL. In this architecture, it serves a dual purpose: it acts as a sink for real-time streams (from Kafka or other sources) writing into Iceberg, and as a source reading from Iceberg tables managed by Lakekeeper.7

### **4.1 Architecture: Hummock and S3**

RisingWave is built on a cloud-native architecture. Its storage engine, **Hummock**, is a Log-Structured Merge (LSM) tree designed specifically for S3-compatible storage. This aligns perfectly with the lakehouse philosophy, as both the compute engine's internal state and the external Iceberg tables reside on the same cost-effective object storage tier.

### **4.2 Integration via Iceberg REST Catalog**

RisingWave treats Iceberg as a first-class citizen. It connects to Lakekeeper using the standard CREATE CONNECTION syntax, effectively mounting the external catalog into its own namespace.

* **The Connection Object:** RisingWave encapsulates the complexity of catalog connectivity into a reusable connection object. This object stores the REST URI, warehouse path, and credential vending configuration (or direct S3 keys if vending is not used).  
* **Interoperability:** Because the integration relies on the standard REST protocol, RisingWave can interoperate seamlessly with other engines. A table created and populated by OLake is immediately visible to RisingWave for querying, joining, or use as a reference table in a streaming join.17

## ---

**5\. Integration Architecture: The "Golden Path"**

This section synthesizes the three components into a cohesive architectural diagram (described in narrative form) illustrating the flow of data and metadata.

### **5.1 The Data Pipeline Topology**

1. **Source (Operational Layer):** Transactions occur in the source database (e.g., PostgreSQL).  
2. **Capture & Ingest (OLake):**  
   * OLake's CDC driver captures the pgoutput WAL stream.  
   * It buffers these changes into micro-batches in memory.  
   * It writes the raw data as Parquet files to the S3 Bucket (under the prefixes defined by the table structure).  
   * **Crucially**, it sends a **Commit Transaction** request to **Lakekeeper** via the REST API. This request contains the list of new data files and the schema.  
3. **Governance (Lakekeeper):**  
   * Lakekeeper receives the commit request.  
   * It validates the request against the current schema (checking for compatibility).  
   * It atomically updates the metadata.json pointer to a new snapshot that includes the new files.  
   * It acknowledges the commit to OLake.  
4. **Compute (RisingWave):**  
   * RisingWave, configured with the Lakekeeper connection, polls the catalog (or is triggered) to detect the new snapshot.  
   * It reads the new Parquet files from S3.  
   * It updates its materialized views or serves the fresh data to downstream BI tools via its Postgres-compatible interface.

### **5.2 Latency and Consistency Analysis**

* **Latency:** The end-to-end latency is the sum of the database replication lag, OLake's buffering time (configurable via batch size/time), and RisingWave's refresh interval. In a tuned system, this can be in the sub-minute range, qualifying as "near real-time."  
* **Consistency:** The system guarantees **Read Committed** or **Snapshot Isolation**. RisingWave will never see partial writes because the switch to the new snapshot in Lakekeeper is atomic. There are no "dirty reads" of files that are being written but not yet committed.

## ---

**6\. Comprehensive Implementation Guide**

This section provides a concrete, reproducible guide to deploying this stack using Docker Compose. It integrates the specific configurations found across the research snippets into a unified deployment manifest.

### **6.1 Infrastructure Definition (docker-compose.yml)**

The following Docker Compose file orchestrates the entire stack: MinIO (Storage), Postgres (Metadata), Lakekeeper, RisingWave, and OLake.

YAML

version: "3.8"

services:  
  \# \--- 1\. Storage Layer (MinIO) \---  
  minio:  
    image: minio/minio:latest  
    command: server /data \--console-address ":9090"  
    ports: \["9000:9000", "9090:9090"\]  
    environment:  
      MINIO\_ROOT\_USER: minioadmin  
      MINIO\_ROOT\_PASSWORD: minioadmin  
    volumes:  
      \- minio\_data:/data  
    networks:  
      \- ice\_net

  \# \--- 2\. Metadata Database (Postgres) \---  
  \# Shared backend for Lakekeeper and RisingWave meta-store  
  postgres:  
    image: postgres:15  
    environment:  
      POSTGRES\_USER: postgres  
      POSTGRES\_PASSWORD: password  
      POSTGRES\_DB: postgres  
    volumes:  
      \- pg\_data:/var/lib/postgresql/data  
    networks:  
      \- ice\_net

  \# \--- 3\. Governance Layer (Lakekeeper) \---  
  lakekeeper:  
    image: quay.io/lakekeeper/catalog:latest  
    ports: \["8181:8181"\]  
    depends\_on:  
      postgres:  
        condition: service\_healthy  
    environment:  
      \# Database connection for Lakekeeper's internal state  
      LAKEKEEPER\_\_PG\_DATABASE\_URL\_READ: postgresql://postgres:password@postgres:5432/postgres  
      LAKEKEEPER\_\_PG\_DATABASE\_URL\_WRITE: postgresql://postgres:password@postgres:5432/postgres  
      \# Encryption key for sensitive data (secrets) at rest  
      LAKEKEEPER\_\_PG\_ENCRYPTION\_KEY: "super-secret-development-key-change-me"  
      \# Logging level  
      RUST\_LOG: info  
    command: \["serve"\]  
    networks:  
      \- ice\_net

  \# \--- 4\. Ingestion Layer (OLake UI & Backend) \---  
  olake-ui:  
    image: registry-1.docker.io/olakego/ui:latest  
    ports: \["8000:8000"\]  
    depends\_on:  
      \- postgres  
    environment:  
      \# OLake requires its own persistence (can share PG instance with different DB/schema)  
      POSTGRES\_DB: "postgres://postgres:password@postgres:5432/olake\_db"  
      \# Directory mapping for local config  
      PERSISTENT\_DIR: /mnt/olake-data  
    volumes:  
      \-./olake-data:/mnt/olake-data  
    networks:  
      \- ice\_net

  \# \--- 5\. Compute Layer (RisingWave) \---  
  risingwave:  
    image: risingwavelabs/risingwave:latest  
    ports: \["4566:4566", "5691:5691"\]  
    command: \>  
      risingwave playground  
    depends\_on:  
      \- minio  
      \- postgres  
    networks:  
      \- ice\_net

networks:  
  ice\_net:  
    driver: bridge

volumes:  
  minio\_data:  
  pg\_data:

*Note: This configuration assumes a local development environment. For production, strict network isolation, secret management (e.g., AWS Secrets Manager), and resource limits must be applied.*

### **6.2 Bootstrapping the Environment**

#### **Step 1: Initialize Lakekeeper**

Lakekeeper starts in a raw state and needs to be bootstrapped to create the default project and warehouse.  
Command:

Bash

\# 1\. Initialize the system  
curl \-X POST http://localhost:8181/management/v1/bootstrap \\  
\-H 'Content-Type: application/json' \\  
\-d '{"accept-terms-of-use": true}'

\# 2\. Configure the Warehouse (Connecting to MinIO)  
curl \-X POST http://localhost:8181/management/v1/warehouse \\  
\-H 'Content-Type: application/json' \\  
\-d '{  
  "warehouse-name": "main-warehouse",  
  "storage-profile": {  
    "type": "s3",  
    "bucket": "iceberg-data",  
    "endpoint": "http://minio:9000",  
    "region": "us-east-1",  
    "path-style-access": true,  
    "flavor": "minio"  
  },  
  "storage-credential": {  
    "type": "s3",  
    "aws-access-key-id": "minioadmin",  
    "aws-secret-access-key": "minioadmin"  
  }  
}'

*Insight:* The flavor: minio and path-style-access: true parameters are critical. Without them, the S3 client might attempt to resolve DNS buckets (e.g., iceberg-data.minio:9000), which will fail in a standard Docker network.16

#### **Step 2: Configure OLake Data Pipeline**

Access the OLake UI (http://localhost:8000) to create the replication job.

1. **Define Source:** Connect to your upstream Postgres/MySQL DB. Ensure the credentials have replication privileges.  
2. **Define Destination (The Integration Point):**  
   * **Type:** Iceberg  
   * **Catalog Type:** REST  
   * **Catalog URI:** http://lakekeeper:8181/catalog/  
   * **Warehouse Path:** s3://iceberg-data/  
   * **S3 Config:** Set the endpoint to http://minio:9000, access key minioadmin, secret minioadmin, and region us-east-1.  
3. **Start Sync:** OLake will generate the streams.json, snapshot the tables, and begin CDC streaming.

#### **Step 3: Connect RisingWave to the Lake**

Once OLake has populated the data, connect RisingWave to Lakekeeper to query it.  
SQL Command (Execute in RisingWave PSQL):

SQL

CREATE CONNECTION lakekeeper\_conn WITH (  
  type \= 'iceberg',  
  catalog.type \= 'rest',  
  catalog.uri \= 'http://lakekeeper:8181/catalog/',  
  warehouse.path \= 'main-warehouse',  
  s3.endpoint \= 'http://minio:9000',  
  s3.access.key \= 'minioadmin',  
  s3.secret.key \= 'minioadmin',  
  s3.region \= 'us-east-1',  
  s3.path.style.access \= 'true'  
);

\-- Set this connection as the default for Iceberg engine operations  
SET iceberg\_engine\_connection \= 'lakekeeper\_conn';

Now, you can query the tables directly:

SQL

\-- Query the table created by OLake (assuming namespace 'public' and table 'users')  
SELECT \* FROM main\_warehouse.public.users LIMIT 10;

17

## ---

**7\. Operational Excellence and Day 2 Considerations**

Deploying the stack is only the first step. Operating it at scale requires attention to monitoring, schema evolution, and performance tuning.

### **7.1 Handling Schema Evolution**

Schema drift is inevitable. The source database schema will change (e.g., ALTER TABLE ADD COLUMN new\_flag).

* **Detection:** OLake's CDC reader detects the DDL event in the replication stream.  
* **Propagation:** OLake pauses the data write, constructs an Iceberg UpdateSchema operation, and sends it to Lakekeeper.  
* **Validation:** Lakekeeper checks if the change is a "safe" evolution (e.g., adding an optional column). If valid, it updates the metadata.  
* **Consumption:** RisingWave does not automatically poll for schema changes on every query for performance reasons. Users may need to issue a REFRESH command or rely on the auto.schema.change configuration for sinks to propagate these changes downstream.19

### **7.2 Monitoring and Observability**

* **OLake:** Generates a stats.json file updated in real-time, containing metrics like rows\_synced, speed\_rps, and memory\_usage. This should be ingested into a monitoring tool (Prometheus/Grafana) to alert on latency lags or throughput drops.14  
* **Lakekeeper:** As a Rust application, it exposes structured logs (RUST\_LOG=info). Monitoring the HTTP 5xx rate on the /catalog/ endpoints is crucial for detecting governance failures.  
* **RisingWave:** Provides a built-in dashboard (typically on port 5691\) and exposes extensive Prometheus metrics regarding barrier latency, state store size, and compaction status.

### **7.3 Managing "Small Files"**

High-frequency streaming ingestion (like OLake's CDC) can result in a "small file problem"—thousands of tiny Parquet files that degrade query performance.

* **Mitigation Strategy:** While RisingWave has internal compaction for its own state, external Iceberg tables require explicit maintenance. Implementing a periodic "Compaction Job" (using Flink or a specialized maintenance tool) to rewrite these small files into larger, read-optimized files is a standard best practice. Future versions of Lakekeeper plan to support automated table maintenance hooks.20

## ---

**8\. Strategic Conclusion**

The integration of OLake, Lakekeeper, and RisingWave represents a maturation of the open data stack. By moving away from generic, heavy JVM-based tools to specialized, high-performance components (Go and Rust), organizations can achieve a dramatic reduction in infrastructure footprint and operational complexity. This architecture delivers on the promise of the Data Lakehouse: an open, secure, and real-time platform where data is not just stored, but actively governed and instantly actionable. For enterprises seeking to build a future-proof data strategy that avoids the lock-in of proprietary cloud warehouses, this stack offers a powerful, technically rigorous alternative.

## ---

**Citations**

.1

#### **Works cited**

1. Show HN: OLake\[open source\] Fastest database to Iceberg data replication tool, accessed December 5, 2025, [https://news.ycombinator.com/item?id=43002938](https://news.ycombinator.com/item?id=43002938)  
2. OLake Data Replication: Fastest Open Source Iceberg Lakehouse Tool, accessed December 5, 2025, [https://olake.io/docs/](https://olake.io/docs/)  
3. Deep Dive into OLake Architecture & Data Replication, accessed December 5, 2025, [https://olake.io/blog/olake-architecture-deep-dive/](https://olake.io/blog/olake-architecture-deep-dive/)  
4. Lakekeeper is an Apache-Licensed, secure, fast and easy to use Apache Iceberg REST Catalog written in Rust. \- GitHub, accessed December 5, 2025, [https://github.com/lakekeeper/lakekeeper](https://github.com/lakekeeper/lakekeeper)  
5. Building Modern Lakehouse with Iceberg, OLake, Lakekeeper & Trino | Fastest Open Source Data Replication Tool, accessed December 5, 2025, [https://olake.io/blog/building-modern-data-lakehouse-with-olake-iceberg-lakekeeper-trino/](https://olake.io/blog/building-modern-data-lakehouse-with-olake-iceberg-lakekeeper-trino/)  
6. Take Full Control of Your Lakehouse with RisingWave's Iceberg REST Catalog, accessed December 5, 2025, [https://medium.risingwave.com/take-full-control-of-your-lakehouse-with-risingwaves-iceberg-rest-catalog-dd2e7144b1f8](https://medium.risingwave.com/take-full-control-of-your-lakehouse-with-risingwaves-iceberg-rest-catalog-dd2e7144b1f8)  
7. Build a Streaming Logistics Lakehouse: RisingWave \+ Lakekeeper ..., accessed December 5, 2025, [https://risingwave.com/blog/build-a-streaming-logistics-lakehouse-risingwave-lakekeeper-iceberg/](https://risingwave.com/blog/build-a-streaming-logistics-lakehouse-risingwave-lakekeeper-iceberg/)  
8. Fastest Open Source Data Replication Tool, accessed December 5, 2025, [https://olake.io/](https://olake.io/)  
9. Step-by-Step Guide \- Replicating PostgreSQL to Iceberg with OLake & AWS Glue, accessed December 5, 2025, [https://olake.io/iceberg/postgres-to-iceberg-using-glue/](https://olake.io/iceberg/postgres-to-iceberg-using-glue/)  
10. MySQL to Apache Iceberg Replication | Modern Analytics Pipeline \- OLake, accessed December 5, 2025, [https://olake.io/blog/mysql-apache-iceberg-replication/](https://olake.io/blog/mysql-apache-iceberg-replication/)  
11. OLake Architecture \- Fast, Modular & Scalable Data Pipeline | Fastest Open Source Data Replication Tool, accessed December 5, 2025, [https://olake.io/blog/olake-architecture/](https://olake.io/blog/olake-architecture/)  
12. RESTIcebergWriterUIConfigDeta, accessed December 5, 2025, [https://olake.io/docs/shared/config/RESTIcebergWriterUIConfigDetails/](https://olake.io/docs/shared/config/RESTIcebergWriterUIConfigDetails/)  
13. OLake CLI Commands & Flags Reference | Developer Guide, accessed December 5, 2025, [https://olake.io/docs/community/commands-and-flags/](https://olake.io/docs/community/commands-and-flags/)  
14. OLake Docker CLI Setup | Configure Source, Destination, Sync, accessed December 5, 2025, [https://olake.io/docs/install/docker-cli/](https://olake.io/docs/install/docker-cli/)  
15. Concepts \- Lakekeeper Docs, accessed December 5, 2025, [https://docs.lakekeeper.io/docs/0.10.x/concepts/](https://docs.lakekeeper.io/docs/0.10.x/concepts/)  
16. We Built an Open Source S3 Tables Alternative | by Yingjun Wu \- Data Engineer Things, accessed December 5, 2025, [https://blog.dataengineerthings.org/we-built-an-open-source-s3-tables-alternative-2b3c95ef4b3a](https://blog.dataengineerthings.org/we-built-an-open-source-s3-tables-alternative-2b3c95ef4b3a)  
17. Quick start: Build a streaming lakehouse \- RisingWave, accessed December 5, 2025, [https://docs.risingwave.com/iceberg/quick-start](https://docs.risingwave.com/iceberg/quick-start)  
18. Full Control of Your Lakehouse: RisingWave's Iceberg REST Catalog Support, accessed December 5, 2025, [https://risingwave.com/blog/risingwave-iceberg-rest-catalog/](https://risingwave.com/blog/risingwave-iceberg-rest-catalog/)  
19. Highlights of RisingWave v2.6. Real-time event streaming platform… \- Medium, accessed December 5, 2025, [https://medium.com/real-time-data-evolution/highlights-of-risingwave-v2-6-640e8ccd4aeb](https://medium.com/real-time-data-evolution/highlights-of-risingwave-v2-6-640e8ccd4aeb)  
20. Lakekeeper \- Lakekeeper Docs, accessed December 5, 2025, [https://docs.lakekeeper.io/](https://docs.lakekeeper.io/)  
21. accessed December 5, 2025, [https://app.livestorm.co/datazip-inc/a-journey-into-data-lake-introducing-apache-iceberg\#:\~:text=OLake%20is%20an%20open%2Dsource,lakehouse%20formats%2C%20like%20Apache%20Iceberg.](https://app.livestorm.co/datazip-inc/a-journey-into-data-lake-introducing-apache-iceberg#:~:text=OLake%20is%20an%20open%2Dsource,lakehouse%20formats%2C%20like%20Apache%20Iceberg.)  
22. OLake UI Installation Guide \- Docker Compose Setup & Configuration, accessed December 5, 2025, [https://olake.io/docs/install/olake-ui/](https://olake.io/docs/install/olake-ui/)