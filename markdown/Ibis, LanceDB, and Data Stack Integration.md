# **The Converged Lakehouse: Architecting a Multimodal Data Environment with Lance Namespace and the Composable Stack**

## **1\. Executive Introduction: The Era of the Composable AI Stack**

The contemporary data infrastructure landscape is witnessing a fundamental dissolution of the historical barriers between Online Transactional Processing (OLTP), Online Analytical Processing (OLAP), and the burgeoning domain of Artificial Intelligence (AI) data management. We are moving beyond the monolithic paradigms of the single-vendor data warehouse and the unmanaged data lake into a third era: the **Composable AI Stack**. The environment proposed in this research—integrating **Ibis**, **DuckDB**, **MotherDuck**, **PlanetScale**, **Cloudflare R2**, **Iceberg**, **DuckLake**, and **Lance Namespace**—represents the vanguard of this architectural shift. It is a system designed not merely for "data processing" in the abstract, but specifically for the high-fidelity management of multimodal assets, such as PDF documents and their semantic vector embeddings, alongside rigorous transactional state management.  
The core challenge addressed by this architecture is the "impedance mismatch" between structured business data (users, subscriptions, access logs) and unstructured AI data (vectors, binary blobs, neural indices). Historically, these lived in separate silos: Postgres for the business, S3 for the files, and a specialized vector database for the embeddings. This fragmentation introduces latency, data drift, and governance nightmares. By unifying these layers through **Cloudflare R2** (as the universal storage substrate) and bridging them with **Lance Namespace** (as the metadata unifier), this architecture proposes a "Zero-Copy," "Zero-Egress" future where compute engines are brought to the data, rather than data being shipped to the compute.  
This report serves as an exhaustive architectural blueprint and implementation guide for this specific stack. It places a heavy emphasis on the role of **Lance Namespace**, dissecting its function as the integration layer that allows "AI-native" data (Lance format) to coexist and interoperate with "Analytics-native" data (Iceberg/DuckLake) and "Transaction-native" data (Postgres). We will explore the theoretical underpinnings of storage-compute separation, the mechanics of hybrid execution, and the practical implementation details of serving PDF files at the edge using this converged infrastructure.

## ---

**2\. The Architectural Foundation: Unbundling the Database**

To understand how best to utilize Lance Namespace within this stack, one must first rigorously define the role of each component. This ecosystem relies on the principle of "best-of-breed" specialization, where distinct tools solve specific classes of data engineering problems but are loosely coupled through open standards (Arrow, Parquet, Lance, SQL).

### **2.1. The Universal Interface: Ibis as the Control Plane**

In this heterogeneous environment, the developer experience is the primary risk factor. Managing connections to PlanetScale (MySQL/Postgres protocol), MotherDuck (DuckDB protocol), and LanceDB (Native/Arrow protocol) requires a unifying linguistic layer. **Ibis** fulfills this role as the portable Python DataFrame API.  
Unlike eager-execution libraries like pandas, which pull data into memory immediately, Ibis operates on a **lazy evaluation** model. It constructs an intermediate semantic representation of the query—a logical plan—and then compiles this plan into the native dialect of the target backend.1 This capability is indispensable in a stack where data resides in different physical locations (PlanetScale in AWS/GCP, MotherDuck in the cloud, Lance in R2).  
Ibis acts as the **federation coordinator**. While Ibis typically pushes a query to a single backend, the integration with **DuckDB** allows Ibis to act as a virtualization layer. Through DuckDB's ability to attach to external databases (Postgres via postgres\_scanner, S3 via httpfs), Ibis can express complex join logic across these systems in a single, fluent Python syntax.1 For the specific requirement of handling Lance datasets, Ibis serves as the orchestration tool that defines *what* data is needed, relying on DuckDB and Lance Namespace to handle the *how* of retrieval from R2.

### **2.2. The Compute Engine: DuckDB and MotherDuck**

**DuckDB** is the "engine room" of this architecture. As an in-process SQL OLAP database, it runs directly within the application container or the data processing worker. Its vectorized execution engine is optimized for analytical queries on columnar data, making it the ideal processor for the Parquet and Lance files stored in R2.2  
**MotherDuck** extends DuckDB into a serverless cloud data warehouse. It introduces the concept of **Hybrid Execution**, where a query plan can be split: purely local operations run on the developer's machine or worker node, while heavy aggregations or joins on large datasets are shipped to the MotherDuck cloud.4

* **Role in this Stack:** MotherDuck is the primary engine for heavy analytical lifting. It is responsible for joining the high-volume clickstream/access logs (stored in DuckLake format) with the dimensional user data (from PlanetScale).  
* **DuckLake:** This is MotherDuck’s optimized table format and catalog. Unlike generic data lakes, DuckLake brings ACID compliance and "time travel" to data stored in object storage.5 It is designed to work seamlessly with the DuckDB engine, offering features like **Data Inlining**, where small inserts are stored directly in the metadata to avoid the "small file problem" common in S3-based lakes.6

### **2.3. The Operational Store: PlanetScale PostgreSQL**

PlanetScale has historically been synonymous with Vitess and MySQL. However, the introduction of **PlanetScale for Postgres** fundamentally changes the integration dynamic of this stack.7

* **Role:** It serves as the immutable "System of Record" for transactional entities: User IDs, Billing, Authentication, and the mutable metadata of the PDF uploads (e.g., "is\_public", "owner\_id").  
* **The pg\_duckdb Bridge:** This is a critical synergy. PlanetScale Postgres supports the pg\_duckdb extension, which embeds the DuckDB engine *inside* the Postgres process.4 This allows the transactional database to query external data lakes (Parquet/Lance on R2) directly. It effectively blurs the line between OLTP and OLAP, allowing a developer to write a SQL query in PlanetScale that joins a local users table with a remote vector\_search\_logs table stored in MotherDuck.

### **2.4. The Storage Layer: Cloudflare R2**

**Cloudflare R2** is the physical foundation of the "Lake." Its S3-compatible API ensures compatibility with every tool in this stack (DuckDB, LanceDB, Iceberg).

* **Economic Strategic Advantage:** The "serving of PDF files" implies a high-read-volume workload. Traditional cloud object stores (AWS S3, Google GCS) charge significant egress fees for data moving out of their network. R2’s **zero-egress** model is the economic enabler of this architecture.9 It allows the PDFs to be served directly to users or retrieved by compute nodes for vectorization without incurring bandwidth penalties.  
* **Performance:** R2’s global distribution and tiering ensure low latency for retrieving large binary blobs (PDFs), effectively acting as a storage-backed CDN.

### **2.5. The Metadata Layer: Iceberg REST and Lance Namespace**

This layer provides the "governance and discovery" capabilities. Without a shared catalog, files in R2 are just "dark data," invisible to the query engines.

* **Iceberg REST Catalog:** This is the industry standard for tracking table metadata (schemas, snapshots, partitions) in a vendor-neutral way.10 It decouples the table state from the file system.  
* **Lance Namespace:** This is the specialized integration layer for the user’s vector data. It allows Lance-formatted tables (which are optimized for AI) to be registered and managed within the standard Iceberg REST catalog, making them discoverable alongside standard analytical tables.11

## ---

**3\. Deep Dive: Lance Namespace Integration Strategy**

The user's core inquiry revolves around "how best to use Lance Namespace integrating with the rest of this stack." This section serves as the definitive guide to that integration, moving from conceptual architecture to concrete implementation patterns.

### **3.1. The Problem Space: The "Split-Brain" Lakehouse**

In a standard data architecture, one often encounters a bifurcation:

1. **The Analytics Lake:** Tables stored in Parquet/Iceberg format, managed by a Hive Metastore or Iceberg Catalog, and queried by Spark, Trino, or DuckDB.  
2. **The AI Silo:** Vector embeddings stored in a specialized Vector Database (Pinecone, Milvus) or in raw files managed by a proprietary application logic.

This separation creates a "Split-Brain" problem. The data engineering team (using Iceberg) cannot see the vector data. The AI team (using vectors) cannot easily join their results with business dimensions in the analytics lake. **Lance Namespace** is the architectural solution to this schism. It is a specification and set of adapters that allow Lance datasets to "live inside" standard metadata catalogs.

### **3.2. Architecture of Lance Namespace with Iceberg REST**

When configuring Lance Namespace to use an **Iceberg REST Catalog**, the system employs a "Companion Table" mechanism. This is a sophisticated masquerade that allows the Lance data to be managed by Iceberg without forcing the data into the less-optimal Parquet format.

#### **3.2.1. The Physical vs. Logical Layout**

* **Physical Layer (R2):** The Lance data files (.lance), indices, and fragments are written to Cloudflare R2. For example: r2://my-data-lake/vectors/contracts/.  
* **Logical Layer (Iceberg REST):** The Lance Namespace implementation registers a table in the Iceberg catalog. However, this is not a standard Iceberg table.  
  * **Dummy Schema:** The registered Iceberg table often contains a placeholder schema (e.g., a single column dummy\_lance\_placeholder string). This satisfies the Iceberg requirement that a table must have a schema.  
  * **Table Properties as Pointers:** The integration relies heavily on **Iceberg Table Properties**. It sets specific keys that identify the table's true nature:  
    * table\_type: Set to lance.10  
    * lance\_location: Points to the R2 URI of the Lance dataset.  
    * lance\_schema: May cache the JSON representation of the actual Lance schema (vectors, blobs, metadata).

#### **3.2.2. The Resolution Workflow**

When a client application interacts with this setup:

1. **Discovery:** The client (e.g., Ibis or a Python script) asks the Iceberg Catalog for the table contracts.  
2. **Interception:** The Lance Namespace client (wrapping the connection) inspects the returned metadata. It sees table\_type=lance.  
3. **Redirection:** Instead of trying to read the table as an Iceberg/Parquet table, the client "mounts" the data found at lance\_location using the native Lance reader.

This architecture ensures that **Iceberg is the Single Source of Truth** for *existence, access control, and ownership*, while **Lance is the Storage Format** for *performance and vector capabilities*.

### **3.3. Strategic Implementation for "Serving PDFs and Embeddings"**

The user's specific requirement is to store and serve PDF files and their embeddings. The optimal strategy utilizes Lance's multimodal capabilities, specifically its efficiency with **Binary Large Objects (BLOBs)**.

#### **3.3.1. The "Fat Table" Schema Strategy**

Traditional architectures utilize a "Pointer Strategy": storing the PDF in S3, getting a URL, and storing the URL \+ Embedding in the database.

* **Drawback:** This creates an "N+1" query problem during retrieval. To serve the top 5 relevant documents, the application must (1) Query the vector DB (1 request), receive 5 URLs, and then (2) Make 5 separate HTTP requests to S3 to fetch the content.

**The Lance Recommendation:** Use a "Fat Table" schema where the PDF binary blob is stored *directly* in the Lance column.  
**Proposed Ibis/Lance Schema:**

Python

import pyarrow as pa

schema \= pa.schema()

Why this works on R2 with Lance:  
Lance is a fragment-based columnar format. Unlike Parquet, which must decompress and scan entire row groups, Lance supports O(1) random access to specific row IDs.

1. **Retrieval Efficiency:** When a vector search identifies the top K matches, Lance can perform a **Projection** to retrieve *only* the pdf\_blob column for those K rows.  
2. **Ranged Reads:** The Lance reader issues HTTP Range requests to R2. It does not download the whole file; it downloads only the bytes corresponding to the specific PDFs required.  
3. **Consolidated I/O:** This effectively reduces the "N+1" problem to a single (or very few) parallelized storage requests, drastically reducing latency for the user.

#### **3.3.2. Configuring the Lance Namespace with Iceberg REST and R2**

This section details the specific configuration required to wire these components together. The user must configure the Lance client to authenticate with both the Iceberg REST service (for metadata) and Cloudflare R2 (for data).  
**Python Configuration Pattern:**

Python

import lance  
from lance.namespace import connect

\# 1\. R2 Storage Configuration (S3-Compatible)  
\# These options tell Lance how to talk to Cloudflare R2  
storage\_options \= {  
    "s3\_endpoint\_override": "https://\<ACCOUNT\_ID\>.r2.cloudflarestorage.com",  
    "region": "auto",  
    "aws\_access\_key\_id": "\<R2\_ACCESS\_KEY\_ID\>",  
    "aws\_secret\_access\_key": "\<R2\_SECRET\_ACCESS\_KEY\>",  
    "allow\_http": "true", \# Required if bridging via certain proxies, otherwise false for R2  
    "timeout": "60s"  
}

\# 2\. Iceberg REST Catalog Configuration  
\# This tells Lance where to find the metadata  
catalog\_uri \= "https://\<ICEBERG\_REST\_URL\>/v1"  
warehouse\_path \= "r2://\<BUCKET\_NAME\>/lance-warehouse"

\# 3\. Connect to the Namespace  
\# This object 'ns' becomes the handle to create/manage tables  
ns \= connect(  
    "iceberg",   
    uri=catalog\_uri,   
    warehouse=warehouse\_path,   
    storage\_options=storage\_options  
)

\# 4\. Creating the Table (DDL)  
\# This registers the table in Iceberg AND creates the physical artifacts in R2  
tbl \= ns.create\_table(  
    "pdf\_documents",  
    schema=schema,  
    mode="create"   
)

### **3.4. Bridging Lance Namespace and Ibis/DuckDB**

The final piece of the integration puzzle is making these Lance tables accessible to **Ibis**. Ibis does not currently have a native "Lance Namespace" backend. Instead, we utilize the **Ibis DuckDB Backend**.  
The Integration Pattern: "Resolve and Register"  
Since DuckDB has a native lance extension (capable of reading .lance files) but may not yet automatically traverse the Iceberg/Lance-Namespace redirection link transparently, the application layer must bridge this gap.

1. **Resolve:** The application uses the lance.namespace client (as shown above) to look up the table pdf\_documents. The client returns the physical R2 URI (r2://.../data.lance).  
2. **Register:** The application registers this URI as a **View** or **Scanner** in the DuckDB connection used by Ibis.

Python

\#... assuming 'ns' is connected as above...

\# 1\. Resolve logical name to physical dataset  
lance\_table \= ns.open\_table("pdf\_documents")  
physical\_uri \= lance\_table.uri 

\# 2\. Setup Ibis with DuckDB  
import ibis  
con \= ibis.duckdb.connect()

\# 3\. Install Lance Extension in DuckDB  
con.raw\_sql("INSTALL lance; LOAD lance;")

\# 4\. Register the dataset as a View  
\# Note: We must pass the S3/R2 credentials to DuckDB as well  
con.raw\_sql(f"""  
    CREATE SECRET r2\_secret (  
        TYPE R2,  
        KEY\_ID '{r2\_key\_id}',  
        SECRET '{r2\_secret}',  
        ACCOUNT\_ID '{r2\_account\_id}'  
    );  
""")

\# Register the view using the lance\_scan function  
con.raw\_sql(f"CREATE VIEW pdf\_docs\_view AS SELECT \* FROM lance\_scan('{physical\_uri}');")

\# 5\. Ibis Object Creation  
\# Now Ibis treats it as a native table  
docs \= con.table("pdf\_docs\_view")

\# 6\. Usage: Ibis executes SQL, DuckDB scans Lance on R2  
result \= docs.filter(docs.file\_name.like("%.pdf")).execute()

This pattern provides the best of both worlds: the governance of the Namespace/Catalog and the fluid query API of Ibis.

## ---

**4\. Workflows: The Life of a PDF**

To further elucidate the stack's operation, we will trace the lifecycle of a PDF file through ingestion, storage, and serving.

### **4.1. Ingestion Workflow (Write Path)**

The write path is designed for **Concurrency** and **Atomicity**, leveraging the Iceberg REST catalog to manage state.

1. **Upload & Trigger:** A user uploads a file to the application.  
2. **Vectorization Worker:** A background worker (using Python/Ray) picks up the file. It extracts text and generates an embedding (e.g., using OpenAI or a local BERT model).  
3. **Constructing the Record:** The worker creates an Arrow RecordBatch containing:  
   * id: Generated UUID.  
   * pdf\_blob: The raw bytes of the file.  
   * vector: The computed embedding.  
   * metadata: JSON object with user\_id, timestamp, etc.  
4. **Lance Commit:** The worker calls ns.open\_table("documents").add(batch).  
   * **Phase 1 (Write):** The Lance writer writes new data fragments (files) to R2. These are invisible to readers.  
   * **Phase 2 (Commit):** The Lance client contacts the **Iceberg REST Catalog**. It attempts to swap the metadata pointer to include the new fragments.  
   * **Concurrency:** If multiple workers invoke this simultaneously, the Iceberg Catalog (backed by a database like Postgres) serializes the commits. One will succeed; the other will retry. This guarantees ACID compliance on object storage.12

### **4.2. Serving Workflow (Read Path)**

The read path optimizes for **Low Latency** using R2 and Lance’s random access capabilities.

1. **Request:** User asks "Show me contracts related to NDA."  
2. **Vector Search:** The application generates a query vector for "contracts related to NDA."  
3. **LanceDB Query:**  
   * The application connects to the Lance dataset.  
   * It executes a vector search: .search(query\_vector).limit(5).  
   * **Index Usage:** It utilizes the IVF-PQ index (stored in R2, cached locally on the compute node) to find the nearest neighbors.  
4. **Blob Retrieval:**  
   * The search returns 5 Row IDs.  
   * The query includes a request for the pdf\_blob column.  
   * **Ranged Read:** The Lance reader calculates the exact byte offsets of the blobs in the R2 files. It sends 5 parallel HTTP GET Range requests to R2.  
5. **Response:** The application receives the PDF bytes and streams them to the user.

## ---

**5\. Comparative Analysis: DuckLake vs. Iceberg REST**

The user's stack includes both **DuckLake** and **Iceberg REST**. A critical architectural decision is determining *when* to use which, as having two catalogs can lead to fragmentation.

| Feature | DuckLake | Iceberg REST (with Lance Namespace) | Recommendation for this Stack |
| :---- | :---- | :---- | :---- |
| **Primary Engine** | MotherDuck / DuckDB | Spark / Trino / LanceDB |  |
| **Metadata Storage** | SQL Database (MotherDuck managed) | JSON/Avro Files (standard spec) |  |
| **Write Latency** | **Low** (Data Inlining for small inserts) | **Higher** (File rotation required) | Use **DuckLake** for high-velocity logs (e.g., clickstream, access logs). |
| **Vector Support** | Limited (via Extensions) | **First-Class** (via Lance Namespace) | Use **Iceberg/Lance** for AI data (PDFs, Embeddings). |
| **Interoperability** | DuckDB Ecosystem primarily | Universal (Standard open format) | Use **Iceberg** for data shared with external teams/tools. |

Synthesis Strategy:  
The report recommends a Hybrid Catalog Strategy:

* **Operational Analytics:** Use **DuckLake** for tables that are primarily generated and queried by MotherDuck (e.g., aggregated usage metrics, session logs). DuckLake's "Data Inlining" feature 6 is superior for streaming small updates.  
* **AI Assets:** Use **Iceberg REST** hosting the **Lance Namespace** for the documents and embeddings tables. This adheres to the open standard for the AI assets, ensuring they are future-proof and accessible to other tools (like Spark for bulk training).  
* **Unified View:** Use Ibis \+ DuckDB to create a "Virtual Data Warehouse" that joins tables from both catalogs seamlessly.

## ---

**6\. Integrating PlanetScale and MotherDuck**

The relationship between PlanetScale (OLTP) and MotherDuck (OLAP) is the bridge between the application state and the data intelligence.

### **6.1. The pg\_duckdb Extension**

The inclusion of pg\_duckdb in the stack is pivotal. It allows the PlanetScale Postgres database to become an analytical query initiator.

* **Mechanism:** pg\_duckdb embeds a DuckDB instance inside the Postgres worker process.  
* **Capability:** It can read from MotherDuck.  
* **Workflow:**  
  1. Application writes a user subscription update to PlanetScale users table.  
  2. Analyst wants to see "Average PDF downloads per Premium User."  
  3. **Query:**  
     SQL  
     \-- Executed in PlanetScale  
     SELECT u.subscription\_tier, AVG(d.download\_count)  
     FROM public.users u  
     JOIN motherduck.analytics.daily\_downloads d ON u.id \= d.user\_id  
     GROUP BY u.subscription\_tier;

  4. **Execution:** Postgres handles the users scan. pg\_duckdb pushes the daily\_downloads aggregation to MotherDuck's cloud. The reduced result is returned to Postgres for the final join.  
  * **Performance:** Benchmarks indicate that offloading the analytical portion to MotherDuck via this extension can be **99% faster** than running the analysis in native Postgres, while avoiding resource contention on the transactional primary.4

## ---

**7\. Operationalizing the Stack on R2**

### **7.1. R2 Data Catalog vs. Self-Hosted Iceberg**

Cloudflare has recently introduced the **R2 Data Catalog** (in beta), which essentially provides a managed Iceberg REST endpoint for buckets.9

* **Recommendation:** For this stack, the user should prioritize using the **R2 Data Catalog** if available, as it removes the need to self-host an Iceberg REST service (e.g., Tabular or a Docker container).  
* **Configuration:** The Lance Namespace connection string would simply point to the R2 Data Catalog endpoint provided by Cloudflare, simplifying the infrastructure complexity significantly.

### **7.2. Caching Strategy**

Serving PDFs via Lance on R2 relies on network I/O.

* **Tiered Cache:** Enable **Smart Tiered Cache** on the R2 bucket. This helps adjacent requests for the same PDF fragments hit Cloudflare’s regional caches rather than the R2 origin, reducing latency.13  
* **Local NVMe:** For the compute nodes running LanceDB/DuckDB, ensure they have fast local NVMe storage. Lance leverages local disk to cache the **Vector Index**. A "cold" search (fetching index from R2) can take hundreds of milliseconds; a "warm" search (index on local NVMe) takes milliseconds.14

## ---

**8\. Conclusion and Future Outlook**

The proposed architecture represents a sophisticated, future-proof approach to the **AI Data Lakehouse**. By leveraging **Ibis** as the orchestrator, it achieves code portability. By utilizing **PlanetScale** and **MotherDuck**, it optimally segments transactional and analytical workloads while maintaining query interoperability.  
Most importantly, the strategic deployment of **Lance Namespace** transforms the handling of unstructured data. It elevates PDF documents and embeddings from "files in a bucket" to structured, governed, and queryable assets within the **Iceberg** catalog ecosystem. This allows for a system where a user's subscription status, their download history, and the semantic content of their documents can be queried and joined in a single, high-performance request—a capability that defines the next generation of intelligent applications.  
The successful implementation of this stack relies not on monolithic tooling, but on the disciplined integration of these composable parts, glued together by the open standards of Arrow, Lance, and the Iceberg REST protocol.

#### **Works cited**

1. Integration with Ibis \- DuckDB, accessed December 24, 2025, [https://duckdb.org/docs/stable/guides/python/ibis](https://duckdb.org/docs/stable/guides/python/ibis)  
2. DuckDB \- LanceDB, accessed December 24, 2025, [https://lancedb.com/docs/integrations/platforms/duckdb/](https://lancedb.com/docs/integrations/platforms/duckdb/)  
3. Reading and Writing Parquet Files \- DuckDB, accessed December 24, 2025, [https://duckdb.org/docs/stable/data/parquet/overview](https://duckdb.org/docs/stable/data/parquet/overview)  
4. MotherDuck Integrates with PlanetScale Postgres, accessed December 24, 2025, [https://motherduck.com/blog/motherduck-planetscale-integration/](https://motherduck.com/blog/motherduck-planetscale-integration/)  
5. accessed December 24, 2025, [https://motherduck.com/docs/integrations/file-formats/ducklake/\#:\~:text=1%20through%201.4.,files%20and%20a%20SQL%20database.](https://motherduck.com/docs/integrations/file-formats/ducklake/#:~:text=1%20through%201.4.,files%20and%20a%20SQL%20database.)  
6. DuckLake | MotherDuck Docs, accessed December 24, 2025, [https://motherduck.com/docs/integrations/file-formats/ducklake/](https://motherduck.com/docs/integrations/file-formats/ducklake/)  
7. PlanetScale Postgres, accessed December 24, 2025, [https://planetscale.com/docs/postgres](https://planetscale.com/docs/postgres)  
8. Using MotherDuck with PlanetScale, accessed December 24, 2025, [https://planetscale.com/blog/using-motherduck-with-planetscale](https://planetscale.com/blog/using-motherduck-with-planetscale)  
9. R2 Data Catalog: Managed Apache Iceberg tables with zero egress fees, accessed December 24, 2025, [https://blog.cloudflare.com/r2-data-catalog-public-beta/](https://blog.cloudflare.com/r2-data-catalog-public-beta/)  
10. Apache Iceberg REST Catalog \- Lance, accessed December 24, 2025, [https://lance.org/format/namespace/integrations/iceberg/](https://lance.org/format/namespace/integrations/iceberg/)  
11. lance-format/lance-namespace: Lance Namespace is an ... \- GitHub, accessed December 24, 2025, [https://github.com/lance-format/lance-namespace](https://github.com/lance-format/lance-namespace)  
12. Writing to LanceDB in cloud object storage while other processes are reading? \#1888, accessed December 24, 2025, [https://github.com/lancedb/lancedb/discussions/1888](https://github.com/lancedb/lancedb/discussions/1888)  
13. Public buckets · Cloudflare R2 docs, accessed December 24, 2025, [https://developers.cloudflare.com/r2/buckets/public-buckets/](https://developers.cloudflare.com/r2/buckets/public-buckets/)  
14. Storage Architecture in LanceDB, accessed December 24, 2025, [https://lancedb.com/docs/storage/](https://lancedb.com/docs/storage/)