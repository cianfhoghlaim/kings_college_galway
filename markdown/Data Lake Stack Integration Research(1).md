# **Comprehensive Architecture Report: The Unified Hybrid Data Lakehouse**

## **Converging Lance Namespace, Lakekeeper, DuckLake, and Federated Object Storage**

### **1\. Architectural Paradigm and Executive Vision**

The contemporary data infrastructure landscape is undergoing a radical transformation, shifting from monolithic, vertically integrated data warehouses toward modular, composable "lakehouse" architectures. This report provides an exhaustive technical analysis and implementation roadmap for a specific, high-performance hybrid configuration proposed for deployment. The architecture in question represents a "Grand Unification" of disparate open table formats—Apache Iceberg, Lance, and DuckLake—under a single compute umbrella (DuckDB), underpinned by a federated storage layer comprising self-hosted Garage S3 on Hetzner and managed Cloudflare R2.  
This report validates the feasibility of concurrently utilizing these technologies. It confirms that while the integration is complex due to the divergence of metadata philosophies (REST-based vs. SQL-based vs. File-based), it offers a sovereignty-preserving, cost-efficient, and performance-optimized alternative to hyperscaler platforms. By leveraging the specific strengths of each component—Lance for high-dimensional vector storage, Iceberg for standard analytical reliability, and DuckLake for lightweight SQL-native management—the architecture avoids vendor lock-in while maximizing query performance across both analytical (OLAP) and machine learning (AI/ML) workloads.  
However, the analysis identifies critical integration constraints, specifically the incompatibility between the Lakekeeper catalog (which strictly mandates a PostgreSQL backend) and the user's desire to utilize PlanetScale (a MySQL/Vitess platform). This report resolves these conflicts through a bifurcated metadata strategy, detailing the precise configuration pathways, code-level interaction flows, and infrastructure requirements necessary to achieve seamless interoperability.

### **2\. The Storage Substrate: Federated Object Storage Architecture**

The foundational layer of this lakehouse is a hybrid, multi-cloud object storage system. This design rejects the "single bucket" orthodoxy in favor of a federated model that places data based on access patterns, egress economics, and compute locality.

#### **2.1 Garage S3 on Hetzner: The Performance & Sovereignty Tier**

**Garage** serves as the primary "hot" storage tier in this architecture. Unlike traditional S3-compatible implementations like MinIO, which are often configured for strong consistency within a single cluster, Garage is designed around a Conflict-free Replicated Data Type (CRDT) architecture.1 This design choice is fundamental to its deployment on Hetzner VPS instances, as it allows for robust operation even in the face of transient network partitions or node failures common in cost-optimized, distributed commodity hardware environments.  
The utilization of Garage on Hetzner provides three distinct strategic advantages:

1. **Data Sovereignty and Locality:** Data resides on infrastructure under direct control, physically located within GDPR-compliant zones (assuming Hetzner's EU regions), which is critical for compliance-sensitive datasets.  
2. **Zero-Egress Compute Locality:** By co-locating the primary compute engines (Lakekeeper and potentially the heavy-lifting DuckDB workers) on the same Hetzner private network as the Garage nodes, the architecture eliminates egress fees for internal processing.  
3. **Cost Efficiency:** Hetzner's storage pricing (via Storage Boxes or dedicated disks) combined with Garage's efficiency allows for a cost-per-terabyte ratio significantly lower than AWS S3 Standard or even Infrequent Access tiers.3

Implementation Criticality: Virtual-Host Addressing  
A pivotal technical requirement identified in the research is the configuration of S3 addressing styles. Standard S3 SDKs, including those used by the Lance and Iceberg Python clients, increasingly default to "virtual-host" style addressing (e.g., http://my-bucket.s3.domain.com/key) rather than the older "path-style" (e.g., http://s3.domain.com/my-bucket/key).4 Garage supports both, but virtual-host style—which is necessary for seamless integration with the Lance Namespace iceberg.py implementation—requires specific DNS and configuration steps that are often overlooked.  
To enable this, the garage.toml configuration must explicitly define the root\_domain parameter within the \[s3\_api\] section.6 Furthermore, a wildcard DNS record (e.g., \*.s3.h.yourdomain.com) must be provisioned to resolve to the Garage ingress IP. Failure to configure this will result in the Iceberg REST Catalog returning locations that the Lance or DuckDB clients cannot resolve, breaking the "easy/concurrent" utilization requirement.

#### **2.2 Cloudflare R2: The Global Distribution Tier**

Cloudflare R2 functions as the "warm" or "distribution" tier. Its primary architectural role here is to serve data to consumers outside the Hetzner private network (e.g., analysts on local laptops, external ML training clusters) without incurring the egress penalties associated with traditional cloud providers.  
Integration Nuances with Lakekeeper:  
R2's S3 compatibility is high but not absolute. Crucially, R2 does not support the AWS Security Token Service (STS) AssumeRole functionality in the same manner as AWS.7 This impacts how the Iceberg Catalog (Lakekeeper) vends credentials to clients. In a standard AWS setup, a catalog might vend temporary session tokens. For R2, Lakekeeper must be configured to use Remote Signing. In this mode, the client (DuckDB or Lance) does not receive raw credentials. Instead, it generates a request hash, sends it to Lakekeeper, and Lakekeeper (holding the high-privilege R2 Admin keys) signs the request and returns the signature. This allows the client to interact directly with R2 securely. This distinction is vital for the "concurrent" operation of the system, as the catalog configuration must differ between the Garage warehouse (which might use static keys or internal IAM) and the R2 warehouse.

#### **2.3 Comparative Storage Characteristics**

| Feature | Garage S3 (Hetzner) | Cloudflare R2 |
| :---- | :---- | :---- |
| **Consistency Model** | Eventual (CRDT-based) | Strong (Global) |
| **Primary Use Case** | High-throughput local compute (ETL, Training) | Global read access, Disaster Recovery |
| **Addressing Style** | Configurable (Path/V-Host) | Virtual-Host Preferred |
| **Auth Mechanism** | Static Keys / Internal | API Token (Admin Read/Write) |
| **Lakekeeper Integ.** | Direct / Static Creds | Remote Signing Required 7 |
| **Egress Cost** | Low (Internal), Standard (External) | Zero (Global) |

### ---

**3\. The Metadata Layer: Divergent Catalogs and the "Grand Unification"**

The core complexity—and innovation—of this architecture lies in its metadata management. The prompt asks to utilize Lakekeeper (Iceberg), Lance Namespace (shimmed into Iceberg), and DuckLake (SQL-backed) concurrently. This requires a sophisticated "Federated Metadata" approach, as no single catalog natively supports all three with equal fidelity.

#### **3.1 Lakekeeper: The High-Performance Iceberg Bastion**

**Lakekeeper** acts as the central source of truth for the Iceberg-format tables. It is a Rust-native implementation of the Iceberg REST Catalog specification, optimized for high concurrency and low latency.8  
The Backend Conflict: PostgreSQL vs. PlanetScale  
The user request specifies leveraging "PlanetScale's $5 PostgreSQL." This indicates a potential misunderstanding of the PlanetScale platform. PlanetScale is exclusively a MySQL-compatible platform, built on the Vitess clustering system.9 It does not offer a PostgreSQL interface.  
However, deep research into Lakekeeper's documentation reveals a strict requirement: **Lakekeeper currently only supports PostgreSQL (version 15 or higher) as its persistence backend**.11 It does not support MySQL. Therefore, it is impossible to use PlanetScale as the backend for Lakekeeper.  
Architectural Resolution:  
To satisfy the requirement of "utilizing... PlanetScale for DuckLake metadata" while maintaining Lakekeeper, the architecture must bifurcate the metadata storage:

1. **Lakekeeper Backend:** A self-hosted PostgreSQL container must be deployed on the Hetzner infrastructure alongside the Lakekeeper binary. This ensures low latency between the catalog and its database, preserving the performance benefits of the Rust implementation.  
2. **DuckLake Backend:** The architecture will leverage PlanetScale (MySQL) strictly for DuckLake tables, as DuckLake is designed to utilize generic SQL interfaces for metadata management.

This separation creates a robust "Cell-based" architecture where the failure of the PlanetScale connection does not impact the availability of the Iceberg/Lance catalog, and vice-versa.

#### **3.2 The Lance Namespace: The "Trojan Horse" Strategy**

The most intricate integration point is the use of lance-namespace-impls/iceberg.py to manage Lance tables within the Lakekeeper catalog.13 This component functions as an adapter, effectively masking Lance tables as Iceberg tables to allow them to be registered in the REST catalog.  
Mechanism of Action:  
Based on the implementation analysis of iceberg.py 14, the integration follows a specific sequence:

1. **Registration:** When a client uses the Lance Namespace Python SDK to create a table, the iceberg.py adapter sends a CreateTable request to Lakekeeper.  
2. **The Dummy Schema:** Since Iceberg mandates a schema, the adapter creates a valid Iceberg table definition with a "dummy" schema (e.g., a single nullable string column named dummy).  
3. **Property Injection:** Crucially, it injects a specific table property: table\_type=lance.  
4. **Location Mapping:** It sets the Iceberg metadata location field to the actual S3 path (on Garage or R2) where the Lance dataset resides.

Operational Implication:  
To the Lakekeeper server, this looks like a valid (albeit empty) Iceberg table. To a standard Iceberg client (like Trino or Spark reading via standard Iceberg libraries), it appears as a table with one column and no data files. However, to a Lance-aware client configured with the iceberg namespace, the presence of table\_type=lance triggers a logic branch: it ignores the dummy Iceberg metadata and instead initializes the native Lance dataset found at the location URL.14  
This "Trojan Horse" strategy allows the user to have a **Single Pane of Glass** (Lakekeeper) listing both their standard analytical tables (Iceberg) and their AI/Vector tables (Lance), fulfilling the concurrency requirement.

#### **3.3 DuckLake: The SQL-Native Catalog**

**DuckLake** represents a philosophical departure from the file-based metadata of Iceberg and Lance. It stores table metadata (schemas, file lists, statistics) directly in a transactional SQL database.15  
PlanetScale Integration:  
PlanetScale is the ideal backend for DuckLake in this architecture. Its underlying Vitess architecture provides massive horizontal scalability for the metadata store, ensuring that even if the number of files in Garage/R2 grows into the billions, the metadata operations (listing files, planning queries) remain performant.

* **Connection Security:** PlanetScale requires secure connections. The DuckDB mysql extension supports ssl\_mode=verify\_identity 10, ensuring that the link between the DuckDB compute node (potentially on a laptop) and the PlanetScale cloud is encrypted and authenticated via the system root CAs.

### **4\. Integration Strategy: The Unified Compute Layer**

The "Grand Unification" occurs at the compute layer. DuckDB is uniquely capable of loading multiple storage and format extensions simultaneously, acting as a federated query engine that bridges the gaps between these systems.

#### **4.1 Dependency Management and Extension Loading**

To achieve the requested concurrency, the DuckDB environment must be primed with a specific suite of extensions. The following SQL sequence demonstrates the initialization state required for a unified session:

SQL

\-- 1\. Base File System Support  
INSTALL httpfs; LOAD httpfs; \-- Enables S3/R2 connectivity  
INSTALL aws; LOAD aws;       \-- Advanced credential management

\-- 2\. Table Format Support  
INSTALL iceberg; LOAD iceberg; \-- For Lakekeeper/Iceberg standard tables  
INSTALL lance; LOAD lance;     \-- For reading Lance data (via custom scan)  
INSTALL ducklake; LOAD ducklake; \-- For DuckLake tables

\-- 3\. Database Backend Support  
INSTALL mysql; LOAD mysql;     \-- Required for PlanetScale connection

#### **4.2 Configuring the Storage Secrets**

DuckDB's secret management system allows for granular control over how different S3 endpoints are accessed. This is critical for the hybrid Garage/R2 setup.  
**Secret 1: Garage S3 (Hetzner)**

SQL

CREATE SECRET garage\_secret (  
    TYPE S3,  
    KEY\_ID 'garage\_access\_key',  
    SECRET 'garage\_secret\_key',  
    REGION 'garage', \-- Matches garage.toml s3\_region  
    ENDPOINT 'http://s3.h.yourdomain.com:3900', \-- Virtual-host capable endpoint  
    URL\_STYLE 'vhost', \-- Critical for Lance compatibility  
    USE\_SSL true  
);

**Secret 2: Cloudflare R2**

SQL

CREATE SECRET r2\_secret (  
    TYPE S3,  
    KEY\_ID 'r2\_access\_key',  
    SECRET 'r2\_secret\_key',  
    REGION 'auto',  
    ENDPOINT 'https://\<account\_id\>.r2.cloudflarestorage.com',  
    URL\_STYLE 'path' \-- R2 generally prefers path style or specific vhost config  
);

#### **4.3 Attaching the Catalogs**

With storage configured, the catalogs are attached to the DuckDB session. This is where the concurrent utilization becomes tangible.  
**Attachment A: Lakekeeper (Iceberg & Lance Registry)**

SQL

\-- Attach Lakekeeper. This exposes all standard Iceberg tables directly.  
ATTACH 'https://catalog.yourdomain.com/ws/garage-warehouse'   
AS lakekeeper   
(TYPE ICEBERG, TOKEN 'lakekeeper\_auth\_token');

**Attachment B: DuckLake (PlanetScale)**

SQL

\-- Attach PlanetScale as the metadata store for DuckLake  
\-- Note the 'mysql' protocol and explicit SSL requirement  
ATTACH 'ducklake:mysql:host=aws.connect.psdb.cloud user=... password=... database=ducklake\_db ssl\_mode=required'   
AS my\_ducklake   
(DATA\_PATH 's3://garage-data-bucket/ducklake/');

### **5\. Operational Workflows and Concurrency**

The system is now wired. The following sections detail the operational workflows for reading and writing data across this hybrid topology, addressing the "ease of use" factor.

#### **5.1 The Read Path: Federated Querying**

Querying standard Iceberg tables via Lakekeeper and DuckLake tables via PlanetScale is transparent in DuckDB. However, querying Lance tables registered in Lakekeeper requires a specific workflow due to the "Trojan Horse" nature of the integration.  
The Lance Query Challenge:  
If a user runs SELECT \* FROM lakekeeper.default.my\_lance\_table, the DuckDB iceberg extension will read the Iceberg metadata. It will see the dummy schema (dummy column) and no data files (or dummy data files). It will not automatically switch to the lance extension to read the underlying Vector/Lance data.  
The Solution: Explicit lance\_scan:  
To query the Lance data, the user must bypass the Iceberg abstraction at the read layer while using it at the discovery layer.

1. **Discovery:** The user lists tables in Lakekeeper to find the Lance dataset.  
2. **Access:** The user utilizes the lance\_scan table function, pointing it to the physical S3 path.

*Future Optimization:* A Python wrapper or a custom DuckDB macro could be written to query the catalog, extract the location property for tables where table\_type=lance, and automatically construct the lance\_scan query.  
**Unified SQL Example:**

SQL

\-- 1\. Query an Analytical Report from Iceberg (on Garage)  
WITH sales\_data AS (  
    SELECT user\_id, amount   
    FROM lakekeeper.sales\_schema.transactions  
    WHERE date \> '2023-01-01'  
),

\-- 2\. Query User Metadata from DuckLake (on R2 via PlanetScale)  
user\_meta AS (  
    SELECT user\_id, region, segment  
    FROM my\_ducklake.users.profiles  
)

\-- 3\. Join with Vector Embeddings from Lance (on Garage)  
\-- Note: Requires knowledge of the S3 path, potentially looked up via Python client  
SELECT   
    s.amount,  
    u.region,  
    l.embedding\_vector  
FROM sales\_data s  
JOIN user\_meta u ON s.user\_id \= u.user\_id  
JOIN lance\_scan('s3://garage-data-bucket/lance/vectors.lance') l   
  ON l.user\_id \= s.user\_id;

This query demonstrates the true concurrency of the system: a single engine execution plan joining data from three different formats residing on two different storage backends, managed by two different catalogs.

#### **5.2 The Write Path: Ingestion and Management**

* **Iceberg Writes:** Performed via standard DuckDB SQL (INSERT INTO lakekeeper...) or Spark/Trino. The data is written to Garage/R2, and Lakekeeper is updated via the REST API.  
* **DuckLake Writes:** Performed via DuckDB. DuckDB writes Parquet files to the DATA\_PATH (Garage/R2) and commits the transaction to PlanetScale (MySQL). This offers strong ACID guarantees due to the relational database lock.  
* **Lance Writes:** Performed primarily via the **Lance Python SDK**. The user utilizes the lance\_namespace client to create\_table. This client handles the dual-write: putting the Lance files on S3 and registering the dummy metadata in Lakekeeper.  
  * *Note:* DuckDB's lance extension currently focuses on *read* support. Writing Lance data is best handled via Python/Pandas/Polars integration.

### **6\. Deep Analysis: Constraints, Risks, and Mitigations**

#### **6.1 The PostgreSQL/PlanetScale Divergence**

Risk: The complexity of maintaining two database technologies (Postgres for Lakekeeper, MySQL/PlanetScale for DuckLake).  
Mitigation: This is an acceptable trade-off for the specific benefits. The self-hosted Postgres for Lakekeeper can be a minimal, "set-and-forget" Docker container since the Iceberg catalog state is relatively compact compared to the data itself. PlanetScale provides the serverless scale needed for DuckLake's potentially high-frequency metadata transactions without operational overhead.

#### **6.2 Consistency Models**

Risk: Garage S3 is eventually consistent (CRDT). Iceberg relies on atomic file swaps.  
Mitigation: Lakekeeper mitigates this risk. By acting as the authoritative catalog, Lakekeeper ensures that clients receive the location of the latest committed metadata file. Even if Garage's listing is slightly stale, the direct path to the metadata file provided by Lakekeeper allows the client to read the correct state. However, heavily concurrent writes to the same table on Garage should be handled with caution, relying on Lakekeeper's optimistic locking mechanisms to prevent data loss.

#### **6.3 "Ease" of Use**

Assessment: The setup is architecturally elegant but operationally complex. It is not "easy" in the sense of a turnkey Snowflake solution. It requires significant DevOps proficiency to configure Garage DNS, manage SSL certificates for Lakekeeper, and handle the Python-based Lance registration workflows.  
Mitigation: investing in "Infrastructure as Code" (Terraform/Ansible) to deploy the Hetzner stack (Garage \+ Lakekeeper \+ Postgres) and developing a simple Python utility library to abstract the Lance registration/querying friction will significantly improve the developer experience.

### **7\. Conclusion**

The proposed architecture successfully meets the requirement of utilizing **Garage S3**, **Cloudflare R2**, **Lance Namespace**, **Lakekeeper**, and **DuckLake** concurrently. It achieves this by treating **DuckDB** as the universal adapter and maintaining a strict separation of concerns at the metadata layer.  
The "Grand Unification" is achieved not by forcing all data into one format, but by federating the metadata management:

1. **Lakekeeper (with self-hosted Postgres)** governs the Iceberg and Lance domains.  
2. **PlanetScale** governs the DuckLake domain.  
3. **Garage and R2** provide the flexible, cost-effective storage substrate.

While the "easy" utilization requires upfront investment in configuration—specifically regarding DNS for virtual-host addressing and the "shim" logic for Lance tables—the resulting system is a robust, sovereign, and highly performant data lakehouse capable of supporting the next generation of multimodal AI and analytical workloads.

### **8\. References**

**Research Snippets Cited:**

* **Lance Namespace/Iceberg:**.13  
* **Lakekeeper:**.7  
* **DuckLake:**.10  
* **Garage/S3:**.1  
* **PlanetScale:**.9  
* **DuckDB Integration:**.25

#### **Works cited**

1. List of Garage features \- Deuxfleurs, accessed December 26, 2025, [https://garagehq.deuxfleurs.fr/documentation/reference-manual/features/](https://garagehq.deuxfleurs.fr/documentation/reference-manual/features/)  
2. Garage \- An S3 object store so reliable you can run it outside datacenters, accessed December 26, 2025, [https://garagehq.deuxfleurs.fr/](https://garagehq.deuxfleurs.fr/)  
3. S3 storage solution: Object Storage by Hetzner, accessed December 26, 2025, [https://www.hetzner.com/storage/object-storage/](https://www.hetzner.com/storage/object-storage/)  
4. Amazon S3 Compatibility API Virtual Host Style Support in Object Storage, accessed December 26, 2025, [https://docs.oracle.com/en-us/iaas/Content/Object/s3-virtual-style.htm](https://docs.oracle.com/en-us/iaas/Content/Object/s3-virtual-style.htm)  
5. Virtual hosting of general purpose buckets \- Amazon Simple Storage Service, accessed December 26, 2025, [https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html](https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html)  
6. Garage S3: A Lightweight Alternative for Self-Hosted Object Storage \- UnixHost Blog, accessed December 26, 2025, [https://unixhost.pro/blog/2025/09/garage-s3-a-lightweight-alternative-for-self-hosted-object-storage/](https://unixhost.pro/blog/2025/09/garage-s3-a-lightweight-alternative-for-self-hosted-object-storage/)  
7. Storage \- Lakekeeper Docs, accessed December 26, 2025, [https://docs.lakekeeper.io/docs/latest/storage/](https://docs.lakekeeper.io/docs/latest/storage/)  
8. lakekeeper/lakekeeper: Lakekeeper is an Apache ... \- GitHub, accessed December 26, 2025, [https://github.com/lakekeeper/lakekeeper](https://github.com/lakekeeper/lakekeeper)  
9. Connect any application to PlanetScale, accessed December 26, 2025, [https://planetscale.com/docs/vitess/tutorials/connect-any-application](https://planetscale.com/docs/vitess/tutorials/connect-any-application)  
10. PlanetScale | MotherDuck Docs, accessed December 26, 2025, [https://motherduck.com/docs/integrations/databases/planetscale/](https://motherduck.com/docs/integrations/databases/planetscale/)  
11. Concepts \- Lakekeeper Docs, accessed December 26, 2025, [https://docs.lakekeeper.io/docs/0.10.x/concepts/](https://docs.lakekeeper.io/docs/0.10.x/concepts/)  
12. Configuration \- Lakekeeper Docs, accessed December 26, 2025, [https://docs.lakekeeper.io/docs/0.5.x/configuration/](https://docs.lakekeeper.io/docs/0.5.x/configuration/)  
13. lance-namespace \- PyPI, accessed December 26, 2025, [https://pypi.org/project/lance-namespace/](https://pypi.org/project/lance-namespace/)  
14. Apache Iceberg REST Catalog \- Lance, accessed December 26, 2025, [https://lance.org/format/namespace/integrations/iceberg/](https://lance.org/format/namespace/integrations/iceberg/)  
15. DuckLake is an integrated data lake and catalog format – DuckLake, accessed December 26, 2025, [https://ducklake.select/](https://ducklake.select/)  
16. DuckLake \+ SQLMesh Tutorial: Build a Modern Data Lakehouse On Your Laptop, accessed December 26, 2025, [https://www.tobikodata.com/blog/ducklake-sqlmesh-tutorial-a-hands-on](https://www.tobikodata.com/blog/ducklake-sqlmesh-tutorial-a-hands-on)  
17. MySQL Extension \- DuckDB, accessed December 26, 2025, [https://duckdb.org/docs/stable/core\_extensions/mysql](https://duckdb.org/docs/stable/core_extensions/mysql)  
18. Lance Namespace is an open specification for describing access and operations against a collection of tables in a multimodal lakehouse \- GitHub, accessed December 26, 2025, [https://github.com/lance-format/lance-namespace](https://github.com/lance-format/lance-namespace)  
19. Configuration \- Lakekeeper Docs, accessed December 26, 2025, [https://docs.lakekeeper.io/docs/latest/configuration/](https://docs.lakekeeper.io/docs/latest/configuration/)  
20. Concepts \- Lakekeeper Docs, accessed December 26, 2025, [https://docs.lakekeeper.io/docs/0.5.x/concepts/](https://docs.lakekeeper.io/docs/0.5.x/concepts/)  
21. DuckLake is an integrated data lake and catalog format \- GitHub, accessed December 26, 2025, [https://github.com/duckdb/ducklake](https://github.com/duckdb/ducklake)  
22. DuckLake \- SlingData.IO, accessed December 26, 2025, [https://docs.slingdata.io/connections/datalake-connections/ducklake](https://docs.slingdata.io/connections/datalake-connections/ducklake)  
23. S3 Compatibility status | Garage HQ, accessed December 26, 2025, [https://garagehq.deuxfleurs.fr/documentation/reference-manual/s3-compatibility/](https://garagehq.deuxfleurs.fr/documentation/reference-manual/s3-compatibility/)  
24. Connecting to PlanetScale securely, accessed December 26, 2025, [https://planetscale.com/docs/vitess/connecting/secure-connections](https://planetscale.com/docs/vitess/connecting/secure-connections)  
25. Iceberg REST Catalogs \- DuckDB, accessed December 26, 2025, [https://duckdb.org/docs/stable/core\_extensions/iceberg/iceberg\_rest\_catalogs](https://duckdb.org/docs/stable/core_extensions/iceberg/iceberg_rest_catalogs)  
26. Iceberg Extension \- DuckDB, accessed December 26, 2025, [https://duckdb.org/docs/stable/core\_extensions/iceberg/overview](https://duckdb.org/docs/stable/core_extensions/iceberg/overview)  
27. DuckDB \- LanceDB, accessed December 26, 2025, [https://lancedb.com/docs/integrations/platforms/duckdb/](https://lancedb.com/docs/integrations/platforms/duckdb/)