

# **Architecting the Composable Data Fabric: A Definitive Implementation Guide for Local-First Lakehouse Environments**

## **1\. Introduction: The Paradigm Shift to Composable Data Stacks**

The monolithic data warehouse, once the singular source of truth for enterprise analytics, is undergoing a rapid deconstruction. In its place, a "Composable Data Fabric" is emerging—an architecture defined not by a single vendor's walled garden, but by the orchestration of best-in-breed, modular components that decouple storage from compute and interface from infrastructure. This report serves as an exhaustive technical blueprint for implementing such a stack, specifically tailored to the unique requirements of a local-first, cloud-hybrid environment utilizing **Mathesar**, **Nimtable**, **Memgraph**, **DuckDB**, and **LanceDB**.  
The transition to this modular architecture is driven by the specific properties of modern data modalities. Relational data requires strict consistency; graph data requires index-free adjacency for performance; vector data requires specialized quantization for similarity search; and analytical data requires columnar compression for speed. No single database engine can perform all these tasks with optimal efficiency. Therefore, the modern data architect must construct a "Control Plane" that unifies these disparate engines into a coherent user experience.  
The objective of this analysis is to provide a rigorous, Deep Research-driven implementation strategy for deploying this stack using **Docker Compose**. While individual containers are simple to instantiate, the interoperability of this specific selection—spanning PostgreSQL, Apache Iceberg, Cypher Query Language (Graph), and Lance columnar formats—presents significant integration challenges. Specifically, bridging the gap between local Docker volumes and cloud-native storage (S3/R2) for tools like Lance Data Viewer and Nimtable requires advanced orchestration patterns, such as FUSE-based sidecars with bidirectional mount propagation.

### **1.1 The Selected Component Architecture**

The stack selected for this implementation represents a nuanced understanding of the strengths inherent in distinct data management paradigms.

| Component | Role in Fabric | Primary Data Modality | Storage Mechanism | Interface |
| :---- | :---- | :---- | :---- | :---- |
| **Mathesar** | Control Plane | Relational / OLTP | PostgreSQL (Local) | No-Code UI / SQL |
| **Nimtable** | Lakehouse Manager | Analytical / Metadata | Apache Iceberg (S3/R2) | React Web UI |
| **Memgraph** | Relationship Engine | Graph / Network | In-Memory / WAL | Memgraph Lab |
| **DuckDB** | Federated Compute | Ad-hoc OLAP | Hybrid (Parquet, CSV, DB) | DuckDB UI (WASM) |
| **LanceDB** | Semantic Memory | Vector Embeddings | Lance Format (S3/Local) | Lance Data Viewer |

**Mathesar and PostgreSQL** serve as the anchor. Unlike purely headless architectures, this stack acknowledges that metadata, user configurations, and highly transactional business logic still require the ACID guarantees of a relational database.1 Mathesar democratizes access to this layer, transforming the raw database into a collaborative interface without abstracting away the SQL power required by engineers.  
**Nimtable** introduces the "Lakehouse" paradigm. By managing Apache Iceberg tables, it allows the stack to scale storage infinitely to cloud object stores (like Cloudflare R2 or AWS S3) while maintaining transactional consistency on files. It acts as the visual "head" for the headless Iceberg REST catalog.3  
**Memgraph** addresses the "join penalty" of relational databases. For complex dependency tracking, fraud detection, or social graph analysis, recursive SQL queries are performant disasters. Memgraph provides high-performance graph traversal, and its integration via Bolt and Cypher offers a specialized lane for relationship-heavy workloads.  
**LanceDB** handles the high-dimensional vector embeddings crucial for modern AI workflows. Unlike generic blob storage, the Lance format enables random access and filtering on vectors, but it creates a challenge: viewing this opaque data requires a specialized viewer.5  
**DuckDB** is the "glue." It is the only engine capable of querying the Postgres tables, the Iceberg parquet files, and the Lance datasets with near-native performance. The DuckDB UI provides the ad-hoc analytical surface where these distinct modalities intersect.7

## **2\. The Control Plane: PostgreSQL and Mathesar Configuration**

The foundation of this composable stack is PostgreSQL. In this architecture, PostgreSQL is not merely a data store for Mathesar; it acts as the central state store for the entire fabric. It will likely house the backend database for Nimtable (which requires a JDBC connection for its own metadata) and serve as a query source for DuckDB. Therefore, the configuration must prioritize connectivity and security over isolation.

### **2.1 Mathesar Introspection and Privileges**

Mathesar functions by aggressively introspecting the PostgreSQL system catalogs to map schemas to user interfaces. This requires the Docker container for Postgres to be configured with a user that has sufficient privileges to read information\_schema and pg\_catalog. Standard Docker images for Postgres (postgres:15-alpine) initialize a superuser by default, but for a production-grade local environment, we must script the creation of dedicated users.  
The docker-entrypoint-initdb.d directory is the mechanism for this initialization. Scripts placed here run only on the first startup. For this stack, we require a script that not only sets up the Mathesar database but also prepares the database for **Nimtable**. Nimtable, as a Java-based application, requires a standard JDBC connection URL and a pre-existing database.3 If this database is missing, the container will fail to start.

### **2.2 Connectivity and Authentication (pg\_hba.conf)**

A critical insight from the research into PostgreSQL in Docker environments involves the pg\_hba.conf (Host-Based Authentication) file.1 Mathesar, Nimtable, and DuckDB will all be connecting to Postgres from *different* containers. In a default Docker bridge network, these connections appear as coming from different IP addresses within the container subnet (usually 172.x.x.x).  
By default, Postgres may be configured to trust local connections but require password authentication for TCP/IP connections. To ensure seamless interoperability, especially with the JDBC drivers used by Nimtable and the libpq used by DuckDB, the configuration must enforce scram-sha-256 or md5 password encryption. The research indicates that leaving this configuration to defaults can lead to "connection refused" or "no pg\_hba.conf entry" errors when services attempt cross-container communication.2  
Therefore, the implementation strategy must include a custom command or environment variable set in the Docker Compose file to ensure Postgres listens on all interfaces (listen\_addresses='\*') and accepts password-authenticated connections from the Docker subnet.

### **2.3 Mathesar Docker Specifics**

Mathesar itself is stateless, relying entirely on the database. The research highlights that Mathesar configuration is handled primarily through environment variables that define the connection to the "internal" database (where Mathesar stores its own state) and the "user" databases (which it manages).10 In this consolidated stack, these are physically the same Postgres instance, but logically separated databases (mathesar\_db and nimtable\_db). This separation is crucial for stability; corruption in the Lakehouse catalog (Nimtable) should not crash the Control Plane interface (Mathesar).

## **3\. The Lakehouse Layer: Nimtable and Object Storage Integration**

Nimtable serves as the visual interface for the Apache Iceberg ecosystem. Integrating this into a local Docker stack introduces complexity because Iceberg is inherently designed for distributed object storage (S3), whereas local Docker environments typically rely on block storage (volumes).

### **3.1 The Headless Catalog Challenge**

Apache Iceberg is a table format, not a database. It requires a "Catalog" to track which metadata files are current. Nimtable acts as this Catalog interface. The research indicates that Nimtable can function as a standalone REST Catalog or connect to external ones.3 For this stack, configured for self-hosting, Nimtable will manage the catalog state in the local PostgreSQL instance (discussed in Section 2\) while pointing the actual data files to a remote object store (Cloudflare R2 or AWS S3).  
This split-brain architecture—metadata in local Postgres, data in remote S3—optimizes for both speed (listing tables is fast because it's a local DB query) and scale (storage is infinite in S3).

### **3.2 Configuring Nimtable for Docker**

The primary challenge identified in the research regarding Nimtable's Docker setup is the configuration injection mechanism. While a config.yaml file is standard for bare-metal deployments, the Docker image favors environment variables for secrets to avoid baking credentials into the file system.4  
Key environment variables identified for valid configuration include:

* DATABASE\_URL: This must point to the *internal Docker DNS* name of the Postgres container (e.g., jdbc:postgresql://postgres\_control:5432/nimtable\_db), not localhost.  
* AWS\_REGION, AWS\_ACCESS\_KEY\_ID, AWS\_SECRET\_ACCESS\_KEY: These are standard SDK variables.  
* **Crucial Insight:** The AWS\_ENDPOINT variable is often necessary when using non-AWS providers like Cloudflare R2 or MinIO. The research suggests that without explicit endpoint definition, the Java AWS SDK inside Nimtable will default to aws-global and fail to find the R2 buckets.

Furthermore, snippets 3 highlight a security requirement: the default admin password must be changed upon first login. This state is persisted in the database, meaning the environment variable for the password is only effective during the very first initialization.

## **4\. The Vector Layer: LanceDB and the "Sidecar" Pattern**

The most technically demanding aspect of this stack is the integration of **Lance Data Viewer**. The research snippets 6 reveal a critical architectural constraint: Lance Data Viewer is designed primarily to browse *local* Lance datasets. The official documentation and community discussions 11 confirm that the viewer does not currently possess a native interface to authenticate and browse S3 buckets directly via API keys in the same way Nimtable does.  
This presents a conflict: the "Lakehouse" philosophy dictates data should live in S3/R2, but the tool requires a local filesystem path (/data).

### **4.1 The Rclone Solution: FUSE over Docker**

To resolve this, we must employ a "Sidecar" pattern using **rclone**. Rclone is a command-line program to manage files on cloud storage, but its capability extends to mounting remote storage as a local filesystem using FUSE (Filesystem in Userspace).12  
In this pattern, we introduce an auxiliary container (lance\_s3\_mounter) alongside the lance\_viewer container.

1. **The Mounter:** The rclone container authenticates with R2. It executes a mount command, making the remote bucket accessible at a path inside the container (e.g., /data/s3).  
2. **The Shared Volume:** A Docker volume is shared between the Mounter and the Viewer.  
3. **The Propagation Problem:** By default, a mount created *inside* a Docker container is invisible to the host and other containers, even if they share the volume. This is due to Linux mount namespaces.

### **4.2 Bidirectional Mount Propagation**

To make the R2 mount visible to the Lance Viewer, the Docker volume configuration must utilize **Mount Propagation**. Specifically, the rshared (recursive shared) propagation mode is required.12 This setting instructs the Docker daemon and the Linux kernel to replicate mount events from the container back to the host, and consequently, to any other container binding that directory with the rslave or rshared mode.  
The research into s3fs vs rclone performance 14 strongly suggests that **rclone** with VFS caching (--vfs-cache-mode full) is superior for this use case. LanceDB relies on random access reads of the columnar data. Without aggressive caching, every seek operation would trigger a high-latency HTTP request, rendering the viewer unusable. The full cache mode allows rclone to download chunks to disk and serve them locally, mimicking the performance profile LanceDB expects.

### **4.3 Container Privileges**

FUSE operations require interactions with the kernel that are restricted in standard containers. The lance\_s3\_mounter must be run with the \--privileged flag or, more granularly, with \--cap-add SYS\_ADMIN and \--device /dev/fuse.17 This grants the container permission to create the virtual filesystem required to bridge the S3-Local gap.

## **5\. The Analytical Layer: DuckDB UI**

DuckDB functions as the universal query engine. The ibero-data/duck-ui image 18 provides a web-based SQL IDE. However, there is a fundamental distinction in deployment modes identified in the research: **WASM (Client-Side)** vs. **External (Server-Side)**.

### **5.1 The WASM Limitation**

The default mode of DuckDB UI runs DuckDB via WebAssembly inside the user's browser. While secure and fast for local CSV imports, this creates a "Localhost Paradox." The browser cannot access the Docker network directly. It cannot connect to postgres\_control:5432 because that DNS name exists only inside the Docker bridge network, not on the user's machine.19

### **5.2 The External Server Solution**

To allow DuckDB to query the other components of the stack (Postgres, Memgraph), the UI should ideally connect to a backend DuckDB instance running *inside* the Docker network. However, standard DuckDB is an in-process library, not a server.  
The snippets 18 reference "duckdb-server" projects and the ability of the UI to connect to an external host. For this report, we will configure the DuckDB UI environment variables to hint at an external connection, but given the user's request for "ad-hoc analytical queries spanning the stack," the most robust immediate solution for *S3/R2* analysis (which is the primary request involving Nimtable and Lance) is actually the WASM mode configured with S3 secrets.  
To bridge the stack fully, one would typically run a Python script using duckdb and fastapi to expose a SQL endpoint, but the standard duckdb \--ui command in a container also serves this purpose. We will focus on configuring the UI to handle the S3/R2 connection natively via the httpfs extension, which works excellently in WASM mode for querying the data lake managed by Nimtable.

### **5.3 S3 Configuration in DuckDB**

Whether running in WASM or Server mode, DuckDB requires specific SQL commands to authenticate with R2. The httpfs extension must be loaded. The research 21 highlights that for Cloudflare R2, one should use the S3 secret type but explicitly override the ENDPOINT. The URL\_STYLE parameter must often be set to path rather than vhost for compatibility with non-AWS endpoints.

## **6\. Comprehensive Docker Compose Implementation Strategy**

Based on the synthesis of the above requirements—Postgres introspection, JDBC connections for Nimtable, FUSE sidecars for LanceDB, and S3-enabled DuckDB—the following docker-compose.yml represents the optimal configuration.

### **6.1 Prerequisite Directory Structure**

Before deploying, the host file system must be prepared to support the bind mounts and sidecar patterns.

Bash

mkdir \-p data\_fabric/postgres\_data  
mkdir \-p data\_fabric/memgraph\_data  
mkdir \-p data\_fabric/memgraph\_lib  
mkdir \-p data\_fabric/lance\_mount\_point  
mkdir \-p data\_fabric/init-scripts  
touch data\_fabric/.env

### **6.2 The Master Docker Compose Configuration**

The file below integrates all findings. It uses a unifying data\_fabric bridge network to allow internal DNS resolution.

YAML

version: '3.8'

networks:  
  data\_fabric:  
    driver: bridge  
    name: data\_fabric

volumes:  
  postgres\_data:  
  memgraph\_data:  
  memgraph\_lib:

services:  
  \# \==========================================  
  \# 1\. CONTROL PLANE: PostgreSQL \+ Mathesar  
  \# \==========================================  
  postgres\_control:  
    image: postgres:15-alpine  
    container\_name: postgres\_control  
    restart: unless-stopped  
    environment:  
      POSTGRES\_USER: ${PG\_USER:-admin}  
      POSTGRES\_PASSWORD: ${PG\_PASSWORD:-securepassword}  
      POSTGRES\_DB: mathesar\_db  
      PGDATA: /var/lib/postgresql/data/pgdata  
    volumes:  
      \- postgres\_data:/var/lib/postgresql/data  
      \-./init-scripts:/docker-entrypoint-initdb.d  
    networks:  
      \- data\_fabric  
    healthcheck:  
      test:  
      interval: 5s  
      timeout: 5s  
      retries: 5  
    \# Expose for local debugging, but internal comms happen via network  
    ports:  
      \- "5432:5432"  
    \# Command ensures listening on all interfaces for Docker networking  
    command: postgres \-c 'listen\_addresses=\*'

  mathesar:  
    image: mathesar/mathesar:latest  
    container\_name: mathesar\_ui  
    restart: unless-stopped  
    depends\_on:  
      postgres\_control:  
        condition: service\_healthy  
    environment:  
      DJANGO\_SECRET\_KEY: ${MATHESAR\_SECRET\_KEY:-unsafe\_dev\_key}  
      \# Connection to the DB where Mathesar stores its internal state  
      MATHESAR\_DATABASES\_host: postgres\_control  
      MATHESAR\_DATABASES\_port: 5432  
      MATHESAR\_DATABASES\_name: mathesar\_db  
      MATHESAR\_DATABASES\_user: ${PG\_USER:-admin}  
      MATHESAR\_DATABASES\_password: ${PG\_PASSWORD:-securepassword}  
      \# Connection to the DB Mathesar allows users to edit (Same instance here)  
      MATHESAR\_MODELS\_DATABASE\_HOST: postgres\_control  
      MATHESAR\_MODELS\_DATABASE\_PORT: 5432  
      MATHESAR\_MODELS\_DATABASE\_NAME: mathesar\_db  
      MATHESAR\_MODELS\_DATABASE\_USER: ${PG\_USER:-admin}  
      MATHESAR\_MODELS\_DATABASE\_PASSWORD: ${PG\_PASSWORD:-securepassword}  
    ports:  
      \- "8000:8000"  
    networks:  
      \- data\_fabric

  \# \==========================================  
  \# 2\. LAKEHOUSE MANAGER: Nimtable  
  \# \==========================================  
  nimtable:  
    image: nimtable/nimtable:latest  
    container\_name: nimtable  
    restart: unless-stopped  
    depends\_on:  
      postgres\_control:  
        condition: service\_healthy  
    environment:  
      \# Initial Admin Credentials (Must be changed in UI after first login)  
      ADMIN\_USERNAME: ${NIMTABLE\_ADMIN\_USER:-admin}  
      ADMIN\_PASSWORD: ${NIMTABLE\_ADMIN\_PASS:-admin}  
      \# Backend connection for Nimtable's own metadata  
      DATABASE\_URL: jdbc:postgresql://postgres\_control:5432/nimtable\_db  
      DATABASE\_USERNAME: ${PG\_USER:-admin}  
      DATABASE\_PASSWORD: ${PG\_PASSWORD:-securepassword}  
      \# S3/R2 Credentials for Iceberg Data  
      AWS\_REGION: ${AWS\_REGION:-auto}  
      AWS\_ACCESS\_KEY\_ID: ${AWS\_ACCESS\_KEY\_ID}  
      AWS\_SECRET\_ACCESS\_KEY: ${AWS\_SECRET\_ACCESS\_KEY}  
      AWS\_ENDPOINT: ${AWS\_ENDPOINT}  
      AW\[23\]\_PATH\_STYLE\_ACCESS: "true"  
    ports:  
      \- "3000:3000"  
    networks:  
      \- data\_fabric

  \# \==========================================  
  \# 3\. GRAPH ENGINE: Memgraph  
  \# \==========================================  
  memgraph:  
    image: memgraph/memgraph-platform:latest  
    container\_name: memgraph\_platform  
    restart: unless-stopped  
    ports:  
      \- "7687:7687"   \# Bolt Protocol (for drivers)  
      \- "7444:7444"   \# Logging  
      \- "3001:3000"   \# Lab UI (Mapped to 3001 to avoid conflict with Nimtable)  
    environment:  
      MEMGRAPH\_USER: ${MEMGRAPH\_USER:-admin}  
      MEMGRAPH\_PASSWORD: ${MEMGRAPH\_PASSWORD:-memgraph}  
    volumes:  
      \- memgraph\_data:/var/lib/memgraph  
      \- memgraph\_lib:/var/lib/memgraph/lib  
    networks:  
      \- data\_fabric

  \# \==========================================  
  \# 4\. ANALYTICS: DuckDB UI  
  \# \==========================================  
  duckdb\_ui:  
    image: ghcr.io/ibero-data/duck-ui:latest  
    container\_name: duckdb\_ui  
    restart: unless-stopped  
    ports:  
      \- "5522:5522"  
    environment:  
      \# Enable unsigned extensions to allow httpfs (S3) loading  
      DUCK\_UI\_ALLOW\_UNSIGNED\_EXTENSIONS: "true"  
      \# Hint parameters for external connections (optional usage)  
      DUCK\_UI\_EXTERNAL\_CONNECTION\_NAME: "Local Fabric"  
    networks:  
      \- data\_fabric

  \# \==========================================  
  \# 5\. VECTOR VIEWER & SIDECAR  
  \# \==========================================  
    
  \# The Sidecar: Mounts S3/R2 as a local filesystem using FUSE  
  lance\_s3\_mounter:  
    image: rclone/rclone:latest  
    container\_name: lance\_s3\_mounter  
    \# Capabilities required for FUSE mounting  
    privileged: true   
    cap\_add:  
      \- SYS\_ADMIN  
    devices:  
      \- /dev/fuse  
    environment:  
      \# Inject Rclone config for Cloudflare R2/S3  
      RCLONE\_CONFIG\_R2\_TYPE: s3  
      RCLONE\_CONFIG\_R2\_PROVIDER: Cloudflare  
      RCLONE\_CONFIG\_R2\_ACCESS\_KEY\_ID: ${AWS\_ACCESS\_KEY\_ID}  
      RCLONE\_CONFIG\_R2\_SECRET\_ACCESS\_KEY: ${AWS\_SECRET\_ACCESS\_KEY}  
      RCLONE\_CONFIG\_R2\_ENDPOINT: ${AWS\_ENDPOINT}  
      RCLONE\_CONFIG\_R2\_ACL: private  
    command: \>  
      mount r2:${S3\_BUCKET\_NAME} /data/s3  
      \--allow-other  
      \--vfs-cache-mode full  
      \--vfs-cache-max-size 5G  
      \--dir-cache-time 1m  
      \--poll-interval 1m  
    volumes:  
      \# The critical bind propagation: rshared makes the mount visible to other containers  
      \- type: bind  
        source:./lance\_mount\_point  
        target: /data/s3  
        bind:  
          propagation: rshared  
    networks:  
      \- data\_fabric

  lance\_viewer:  
    image: ghcr.io/gordonmurray/lance-data-viewer:lancedb-0.24.3  
    container\_name: lance\_viewer  
    restart: unless-stopped  
    depends\_on:  
      \- lance\_s3\_mounter  
    ports:  
      \- "8080:8080"  
    volumes:  
      \# Mounts the same host directory, seeing the files Rclone provides  
      \- type: bind  
        source:./lance\_mount\_point  
        target: /data  
        read\_only: true  
    networks:  
      \- data\_fabric

### **6.3 Database Initialization Script (init-scripts/01-init.sql)**

This script ensures the PostgreSQL instance is ready for both Mathesar (which uses the default DB or creates its own schema) and Nimtable (which needs a specific DB to exist upon connection).

SQL

\-- Create database for Nimtable Catalog  
CREATE DATABASE nimtable\_db;

\-- Create dedicated user for Nimtable  
CREATE USER nimtable\_user WITH ENCRYPTED PASSWORD 'securepassword';  
GRANT ALL PRIVILEGES ON DATABASE nimtable\_db TO nimtable\_user;

\-- Mathesar typically uses the root user or its own configured user to manage other DBs  
\-- Ensure the default admin has access  
GRANT ALL PRIVILEGES ON DATABASE nimtable\_db TO admin;

## **7\. Deep Dive: Integration and Interoperability Insights**

### **7.1 The Mechanism of the Rclone Sidecar**

The configuration of the lance\_s3\_mounter is the pivotal element enabling the Lance Data Viewer to function in a cloud-hybrid environment. The standard Docker volume driver is unaware of the contents of S3. By using rclone mount, we essentially turn the /data/s3 directory inside the container into a gateway to the cloud.  
The command flags are selected based on specific performance behaviors:

* \--vfs-cache-mode full: This is non-negotiable for database files. LanceDB (and DuckDB) performs random access reads on the .lance or .parquet files. Standard S3 streaming does not support seeking efficiently. This flag forces Rclone to download requested chunks to a local disk cache (/var/cache/rclone), effectively turning the container into a high-performance caching proxy.  
* \--allow-other: By default, FUSE mounts are owned by the user who created them (root in the container). Without this flag, the file system would be invisible to the host or any other container, even with shared volumes.  
* bind: propagation: rshared: This is a Linux kernel namespace feature. It allows the "mount event" (the act of Rclone connecting S3 to /data/s3) to propagate up to the host OS and down into any other container that binds the same directory. Without this, the Lance Viewer would see an empty directory.

### **7.2 Networking and DNS Resolution**

Within the data\_fabric network, services utilize Docker's embedded DNS server. This allows for resilient configuration that is independent of the host machine's IP address.

* **Nimtable to Postgres:** Uses postgres\_control. If Nimtable were configured to use localhost, it would fail, as localhost inside the Nimtable container refers to itself.  
* **Port Conflicts:** A common operational hazard in such stacks is the conflict on port 3000\. Both Memgraph Lab and Nimtable default to this port. The configuration resolves this by mapping Memgraph Lab's internal 3000 to external 3001 (3001:3000), leaving 3000 clear for Nimtable.

## **8\. Operational Workflows and Day 2 Configuration**

### **8.1 Configuring DuckDB UI for the Data Fabric**

Once the stack is operational, the DuckDB UI (accessible at http://localhost:5522) acts as the analytical console. Because it is running in WASM, it cannot inherently "see" the postgres\_control container via Docker DNS. It interacts with the outside world via HTTP.  
To query the Iceberg data managed by Nimtable or the Lance data viewed by Lance Viewer, the user must configure the S3 secret within the DuckDB session. The research confirms that the httpfs extension is the enabler here.  
**Required SQL for DuckDB Session:**

SQL

INSTALL httpfs;  
LOAD httpfs;

CREATE SECRET r2\_access (  
    TYPE S3,  
    KEY\_ID 'your\_access\_key',  
    SECRET 'your\_secret\_key',  
    REGION 'auto',  
    ENDPOINT 'your-account-id.r2.cloudflarestorage.com',  
    URL\_STYLE 'path'  
);

With this secret active, DuckDB can perform zero-copy queries on the Parquet files stored in the R2 bucket, which are simultaneously being managed by Nimtable.

### **8.2 Mathesar as the Control Hub**

Accessing http://localhost:8000 opens Mathesar. Upon first load, it will ask to connect to a database. The user should input the credentials for postgres\_control. Mathesar will then introspect the nimtable\_db (if permitted) and any other databases created. This provides a user-friendly interface to modify the *metadata* of the Lakehouse (e.g., changing table descriptions in the Nimtable backing store) without needing to write raw SQL updates.

### **8.3 Connecting to Memgraph**

Memgraph Lab is accessible at http://localhost:3001. The "Quick Connect" feature will default to localhost:7687. Since the Lab is running in a container but the browser is on the host, localhost works because we exposed port 7687 in the Compose file. However, if using a programmatic driver from *another* container (e.g., a Python script in a separate container), the address must be memgraph\_platform:7687.

## **9\. Security and Future Considerations**

### **9.1 Credential Management**

The use of a .env file is standard practice, but for production environments, Docker Secrets would be the superior mechanism. The current setup passes sensitive keys (AWS\_SECRET\_ACCESS\_KEY) as plain text environment variables to the containers. Any process capable of inspecting docker inspect can view these keys. For a local research environment, this is acceptable, but it constitutes a risk in shared environments.

### **9.2 The "Headless" Vector Viewer Limitation**

The research identified a significant gap in the Lance ecosystem: the lack of a native, authenticated S3 viewer. The solution provided (Sidecar) is robust but heavyweight. It requires a privileged container and significant memory for the VFS cache. Future iterations of this stack should monitor the lance-data-viewer roadmap for native S3 support, which would eliminate the need for the lance\_s3\_mounter service and simplify the architecture significantly.

### **9.3 Performance Tuning**

The vfs-cache-max-size in the Rclone sidecar is set to 5G. For datasets exceeding this size, Rclone will begin evicting chunks. If the Lance Viewer frequently accesses random segments of large datasets, this thrashing will degrade performance. Users should scale this parameter to match their available disk space and dataset working set size.

## **10\. Conclusion**

This report has detailed the architecture and implementation of a Composable Data Fabric using Mathesar, Nimtable, Memgraph, DuckDB, and LanceDB. By leveraging Docker Compose, we have created a unified environment that respects the distinct storage requirements of each tool while enabling interoperability through shared networks and advanced volume propagation.  
The resulting stack is a powerful, local-first data engineering platform. It allows for:

1. **Visual Management** of huge data lakes via Nimtable.  
2. **No-Code Interaction** with relational data via Mathesar.  
3. **Deep Relationship Analysis** via Memgraph.  
4. **Vector Debugging** via Lance Viewer (bridged to S3).  
5. **Unified Analytics** via DuckDB.

This architecture moves beyond simple container orchestration to solve the fundamental data gravity and access problems inherent in modern, multi-modal data stacks.

#### **Works cited**

1. Setting up TLS connection for containerized PostgreSQL database \- DEV Community, accessed December 1, 2025, [https://dev.to/whchi/setting-up-tls-connection-for-containerized-postgresql-database-1kmh](https://dev.to/whchi/setting-up-tls-connection-for-containerized-postgresql-database-1kmh)  
2. Can't get postgres to work \- Compose \- Docker Community Forums, accessed December 1, 2025, [https://forums.docker.com/t/cant-get-postgres-to-work/29580](https://forums.docker.com/t/cant-get-postgres-to-work/29580)  
3. nimtable/nimtable: The observability platform for Iceberg ... \- GitHub, accessed December 1, 2025, [https://github.com/nimtable/nimtable](https://github.com/nimtable/nimtable)  
4. Nimtable: The Control Plane for Apache Iceberg™ | by RisingWave Labs | Towards Dev, accessed December 1, 2025, [https://medium.com/towardsdev/nimtable-the-control-plane-for-apache-iceberg-aa230a32f7e1](https://medium.com/towardsdev/nimtable-the-control-plane-for-apache-iceberg-aa230a32f7e1)  
5. LanceDB | Vector Database for RAG, Agents & Hybrid Search, accessed December 1, 2025, [https://lancedb.com/](https://lancedb.com/)  
6. Introducing Lance Data Viewer: A Simple Way to Explore Lance Tables \- LanceDB, accessed December 1, 2025, [https://lancedb.com/blog/lance-data-viewer/](https://lancedb.com/blog/lance-data-viewer/)  
7. The DuckDB Local UI, accessed December 1, 2025, [https://duckdb.org/2025/03/12/duckdb-ui](https://duckdb.org/2025/03/12/duckdb-ui)  
8. DuckDB Wasm, accessed December 1, 2025, [https://duckdb.org/docs/stable/clients/wasm/overview](https://duckdb.org/docs/stable/clients/wasm/overview)  
9. How to connect to a Postgres DB (running in a container not installed locally)? · apache superset · Discussion \#30880 \- GitHub, accessed December 1, 2025, [https://github.com/apache/superset/discussions/30880](https://github.com/apache/superset/discussions/30880)  
10. Connection string for postgresql in docker-compose.yml file \- Stack Overflow, accessed December 1, 2025, [https://stackoverflow.com/questions/51442323/connection-string-for-postgresql-in-docker-compose-yml-file](https://stackoverflow.com/questions/51442323/connection-string-for-postgresql-in-docker-compose-yml-file)  
11. lance-format/lance-data-viewer: Browse Lance tables from your local machine in a simple web UI. No database to set up. Mount a folder and go. \- GitHub, accessed December 1, 2025, [https://github.com/lancedb/lance-data-viewer](https://github.com/lancedb/lance-data-viewer)  
12. Propogating rclone mounts to Docker containers without transport endpoint going stale, accessed December 1, 2025, [https://forum.rclone.org/t/propogating-rclone-mounts-to-docker-containers-without-transport-endpoint-going-stale/48112](https://forum.rclone.org/t/propogating-rclone-mounts-to-docker-containers-without-transport-endpoint-going-stale/48112)  
13. Docker Volume Plugin \- Rclone, accessed December 1, 2025, [https://rclone.org/docker/](https://rclone.org/docker/)  
14. Why rclone mount will be faster that the original s3 interface \- Help and Support, accessed December 1, 2025, [https://forum.rclone.org/t/why-rclone-mount-will-be-faster-that-the-original-s3-interface/46935](https://forum.rclone.org/t/why-rclone-mount-will-be-faster-that-the-original-s3-interface/46935)  
15. Achieving s3fs performance with rclone mount \- Help and Support, accessed December 1, 2025, [https://forum.rclone.org/t/achieving-s3fs-performance-with-rclone-mount/9644](https://forum.rclone.org/t/achieving-s3fs-performance-with-rclone-mount/9644)  
16. Achieving s3fs performance with rclone mount \- Page 2 \- Help and Support, accessed December 1, 2025, [https://forum.rclone.org/t/achieving-s3fs-performance-with-rclone-mount/9644?page=2](https://forum.rclone.org/t/achieving-s3fs-performance-with-rclone-mount/9644?page=2)  
17. Is s3fs not able to mount inside docker container? \- Stack Overflow, accessed December 1, 2025, [https://stackoverflow.com/questions/24966347/is-s3fs-not-able-to-mount-inside-docker-container](https://stackoverflow.com/questions/24966347/is-s3fs-not-able-to-mount-inside-docker-container)  
18. Getting Started \- Duck-UI, accessed December 1, 2025, [https://duckui.com/getting-started.html](https://duckui.com/getting-started.html)  
19. DuckDB Docker Container, accessed December 1, 2025, [https://duckdb.org/docs/stable/operations\_manual/duckdb\_docker](https://duckdb.org/docs/stable/operations_manual/duckdb_docker)  
20. A curated list of awesome DuckDB resources \- GitHub, accessed December 1, 2025, [https://github.com/davidgasquez/awesome-duckdb](https://github.com/davidgasquez/awesome-duckdb)  
21. S3 API Support \- DuckDB, accessed December 1, 2025, [https://duckdb.org/docs/stable/core\_extensions/httpfs/s3api](https://duckdb.org/docs/stable/core_extensions/httpfs/s3api)  
22. Connect DuckDB to S3 with Role-Based Credentials \- Stack Overflow, accessed December 1, 2025, [https://stackoverflow.com/questions/79348716/connect-duckdb-to-s3-with-role-based-credentials](https://stackoverflow.com/questions/79348716/connect-duckdb-to-s3-with-role-based-credentials)