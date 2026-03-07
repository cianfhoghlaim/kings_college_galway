# **The Convergent Stack: Architecting Reactive Data Systems with TanStack DB, DuckDB, RisingWave, and Marimo**

## **1\. Executive Summary: The Architectural Imperative for Client-First Data Systems**

The contemporary landscape of web application architecture is currently navigating a profound structural inflection point. For the better part of a decade, the dominant paradigm has treated the web browser primarily as a transient view layer—a thin client responsible for rendering HTML and managing ephemeral interactions, while the "source of truth" remained rigidly locked behind RESTful or GraphQL APIs on remote servers. This model, often characterized by the "fetch-render-discard" lifecycle, fundamentally couples user experience to network latency and server availability. As applications evolve into sophisticated, data-intensive workspaces—typified by tools like Figma, Linear, and complex financial dashboards—this server-centric orthodoxy is proving increasingly insufficient. The demand for sub-millisecond interactivity, offline capability, and complex client-side data manipulation has necessitated the emergence of a new architectural primitive: the reactive, normalized client-side database.  
This report provides an exhaustive, expert-level analysis of **TanStack DB**, a technology that arguably represents the maturation of this client-first philosophy.1 Unlike its predecessor, TanStack Query, which focused on managing asynchronous server state (effectively caching promises), TanStack DB introduces a persistent, database-grade storage engine directly into the browser runtime. It leverages differential dataflow to enable real-time joins, filtering, and aggregations on local data without requiring round-trips to the backend.2 This capability transforms the frontend from a passive consumer of APIs into an active participant in data management.  
However, a database engine is only as powerful as the data sources it can ingest and query. Consequently, this analysis extends beyond TanStack DB in isolation to explore its integration with three distinct classes of high-performance data engines, creating a "Composable Stack" that rivals traditional monolithic architectures:

1. **The Analytical Engine:** **DuckDB**, operating via WebAssembly (WASM) and extending to the cloud via **MotherDuck**, enables the browser to perform OLAP (Online Analytical Processing) queries on millions of rows with near-native performance.3  
2. **The Streaming Engine:** **RisingWave**, a distributed streaming database, provides the capability to define continuous materialized views over high-velocity event streams, pushing real-time aggregations to the client.5  
3. **The Computational Engine:** **Marimo**, a reactive Python notebook environment, serves as a sophisticated logic layer, exposing complex data science workflows and visualizations as API endpoints or WASM islands.7

Finally, to provide a balanced architectural perspective, this composable approach is rigorously benchmarked against **Convex**, a vertically integrated "Backend-as-a-Platform" (BaaS) that offers a competing vision of end-to-end reactivity.9 Through detailed technical dissection, this report aims to equip software architects with the nuanced understanding required to navigate the trade-offs between modular flexibility and integrated convenience in next-generation data systems.

## ---

**2\. TanStack DB: The Foundation of Client-Side Reactivity**

To comprehend the integration patterns with external engines like DuckDB or RisingWave, one must first deconstruct the internal architecture of TanStack DB. It is not merely a state management library; it is a full-fledged database implemented in TypeScript, designed to fundamentally invert the data ownership model of web applications.

### **2.1. The Differential Dataflow Paradigm**

At the core of TanStack DB lies a TypeScript implementation of **differential dataflow** (d2ts).11 This is a significant departure from the signal-based or subscriber-based reactivity models found in libraries like Redux or MobX. In traditional state managers, a change in a store triggers a re-computation of derived selectors. If the dataset is large, this re-computation can be computationally expensive, often blocking the main thread.  
Differential dataflow operates on the principle of incremental maintenance. When a collection is modified—for instance, a single record is updated in a list of 10,000 items—the engine does not re-evaluate the entire query. Instead, it computes the "diff" or delta caused by that specific change and propagates it through the dataflow graph to update the result set. This allows TanStack DB to support **Live Queries** that remain performant even as data volume scales, enabling sub-millisecond updates for complex joins and aggregations.11 This mechanism is crucial when integrating with high-volume data sources like DuckDB or RisingWave, where the client may be holding significant subsets of analytical data.

### **2.2. Core Primitives and Collection Architecture**

TanStack DB establishes three primary architectural primitives that define how developers interact with data:

1. **Collections:** These are the storage units of the database, analogous to tables in a SQL database. Unlike loose Javascript arrays, Collections are typed and normalized. They enforce a schema (often defined via Zod) and ensure that entities are stored by their primary keys, eliminating data duplication.11 This normalization is critical for maintaining consistency; if a "User" entity is updated in one context, that update is immediately reflected across all queries referencing that user.  
2. **Live Queries:** These serve as the read layer. A component subscribes to a query (e.g., collection.find({ status: 'active' })). The differential engine ensures that this component only re-renders when the *result* of the query changes, not just when the collection changes. This fine-grained reactivity is what distinguishes TanStack DB from a simple event emitter.2  
3. **Transactional Mutations:** The write layer is designed with "Optimistic UI" as a first-class citizen. When a mutation occurs, TanStack DB applies the change immediately to the local cache (the "Optimistic Store"), updating the UI instantly. Simultaneously, it queues the operation for synchronization with the backend. If the backend confirmation fails, the system automatically rolls back the optimistic change, ensuring eventual consistency.2

### **2.3. Synchronization Modes and Data Loading**

One of TanStack DB's most versatile features is its agnosticism toward data loading. It supports three distinct sync modes, allowing architects to tailor the data strategy to the specific integration target (e.g., DuckDB vs. REST API):

* **Eager Mode (Default):** The client attempts to load the entire collection upfront. This is the optimal strategy for reference data, configuration tables, or small user-specific datasets (typically \<10,000 rows). In this mode, the client database acts as a complete replica of the server table, enabling ultra-fast, zero-latency local queries.11  
* **On-Demand Mode:** For larger datasets where loading everything is infeasible (e.g., a product catalog with millions of SKUs), TanStack DB operates as a smart cache. It translates client-side query predicates (filters, sorts, pagination) into backend API parameters. This "Query-Driven Sync" ensures that only the data currently required by the user's view is fetched, while still maintaining a normalized cache of previously loaded records.12  
* **Progressive Mode:** A hybrid approach where the application loads a critical subset of data immediately (e.g., the first page of a feed) to ensure a fast "First Contentful Paint," while a background process synchronizes the remainder of the dataset. This is particularly powerful for "Local-First" applications that aim to provide offline capabilities after the initial sync.11

This architectural flexibility is what makes TanStack DB a viable integration partner for diverse backend technologies. The following sections will detail exactly how these primitives map to specific external engines.

## ---

**3\. Integration Pattern A: The In-Browser Data Warehouse with DuckDB**

The integration of TanStack DB with **DuckDB** represents a paradigm shift in how analytical applications are constructed. By embedding an OLAP (Online Analytical Processing) database directly into the browser via WebAssembly (WASM), architects can eliminate the traditional latency associated with analytical queries. In this "Zero-Backend" or "Thick Client" architecture, the browser becomes the execution environment, and the backend is reduced to a dumb storage layer (e.g., S3 buckets).

### **3.1. Architectural Overview: The WASM Analytics Stack**

In this pattern, DuckDB WASM serves as the heavy-lifting execution engine, while TanStack DB acts as the reactive presentation layer.

* **The Engine (DuckDB WASM):** DuckDB is compiled to WebAssembly, allowing it to run within the browser's sandbox. It utilizes Apache Arrow for in-memory columnar storage, which is significantly more efficient for analytical workloads than standard JavaScript objects.3  
* **The Store (TanStack DB):** While DuckDB executes the SQL, TanStack DB manages the React component lifecycle, handling the synchronization of query states (loading, error, success) and providing the optimistic mutation layer that raw SQL engines lack.

### **3.2. Integration Mechanics: The Custom Collection Adapter**

Integrating DuckDB requires bridging the synchronous world of TanStack DB collections with the asynchronous, worker-based world of DuckDB WASM. This is best achieved through the **Custom Collection** pattern, specifically leveraging the QueryCollection architecture.13

#### **3.2.1. The Connection Lifecycle**

The application must first initialize the DuckDB runtime. This involves loading the .wasm binary (which can be 10-20MB, necessitating aggressive caching strategies like Service Workers or HTTP cache headers) and spinning up a Web Worker. This isolation is critical; because WASM execution happens on the main thread by default in some configurations, offloading to a worker prevents the UI from freezing during complex aggregations.16  
Once initialized, the "Database" is essentially a virtual file system in the browser's memory. To ingest data, the application utilizes DuckDB's registerFileUrl or HTTPFS extension. This allows DuckDB to query remote Parquet files directly from an object store (like S3 or R2) using HTTP Range requests. Crucially, this means the browser downloads *only* the specific columns and row groups required for a query, rather than the entire file, optimizing bandwidth usage.14

#### **3.2.2. The Reactive Query Loop**

The integration logic resides within the queryFn of the TanStack DB collection.

1. **State Capture:** The user interacts with the UI (e.g., filtering a dashboard by "Region: North America"). TanStack DB captures this state change.  
2. **Translation:** The queryFn receives the filter context. It constructs a SQL query (e.g., SELECT sum(revenue) FROM parquet\_scan('data.parquet') WHERE region \= 'NA').  
3. **Execution:** The SQL string is passed to the DuckDB worker via an asynchronous message.  
4. **Result Handling:** DuckDB executes the query using its vectorized engine. The result—typically an Arrow table—is serialized back to the main thread.  
5. **Normalization:** TanStack DB receives the result. If the result is meant to be a collection of entities, TanStack DB normalizes them by ID and updates its cache. If it is an aggregate (e.g., a total count), it is stored as query metadata.  
6. **Render:** The React components subscribed to this query automatically re-render with the new data.

This architecture enables "Live Analytics." As the user drags a slider, the queries re-run against the local WASM instance in milliseconds, providing an interaction loop that feels instantaneous compared to server-side API calls.

### **3.3. Scaling Out: The MotherDuck Hybrid Model**

While DuckDB WASM is powerful, it is bound by the browser's resource limits. WebAssembly environments are typically hard-capped at 4GB of memory, and browser tabs are often killed if they consume excessive resources.16 For datasets exceeding these limits, the architecture extends to **MotherDuck**.  
MotherDuck is a managed cloud service for DuckDB that enables "Hybrid Execution".4 The architectural brilliance here lies in the seamless routing of queries. The DuckDB client in the browser acts as a smart router.

* **Local Execution:** If the query targets a small, local Parquet file or a temporary table, it executes entirely within the WASM instance in the browser.  
* **Remote Execution:** If the query targets a massive dataset stored in MotherDuck's cloud (e.g., historical logs), the client automatically routes the SQL command to the MotherDuck server, where it is executed on powerful infrastructure, and only the results are streamed back to the client.4

For TanStack DB, this transition is transparent. The queryFn simply executes SQL against the DuckDB connection; the decision of *where* that SQL runs is abstracted away by the DuckDB/MotherDuck client library. This allows architects to build applications that start local-first but scale to petabyte-level warehousing without rewriting the frontend data layer.

### **3.4. Challenges and Performance Considerations**

Implementing this stack is not without challenges. The primary hurdle is the **initial load time**. Downloading the WASM bundle and the initial Parquet headers can take seconds. To mitigate this, standard patterns include:

* **Lazy Loading:** Only initializing DuckDB when the user navigates to an analytical route.  
* **Persistence:** Using the **Origin Private File System (OPFS)** to cache Parquet files locally. DuckDB WASM can mount OPFS, allowing it to read/write persistent files that survive browser refreshes, effectively turning the browser into a persistent database server.19  
* **Write Performance:** Columnar stores like DuckDB are optimized for reads (OLAP), not writes (OLTP). High-frequency single-row inserts can be slow. The recommended pattern is to batch writes or use TanStack DB's optimistic layer to display changes immediately while buffering them for a batch insert into DuckDB.17

## ---

**4\. Integration Pattern B: The Real-Time Streaming Bridge with RisingWave**

While DuckDB solves the problem of analytical depth, **RisingWave** addresses the challenge of *velocity*. RisingWave is a distributed SQL streaming database designed to process high-throughput event streams (from Kafka, Kinesis, CDC) and maintain continuous materialized views.5

### **4.1. The Streaming Architecture Gap**

The fundamental challenge in integrating streaming databases with frontend clients is the "Push vs. Pull" mismatch.

* **The Backend (RisingWave):** Generates a continuous stream of updates. It is a server-side system.  
* **The Frontend (TanStack DB):** Typically expects to fetch data or receive updates via a standardized sync protocol.

RisingWave is wire-compatible with PostgreSQL, which implies it can be queried using standard Postgres tools.20 However, simply polling RisingWave for updates negates the benefits of streaming. The architecture requires a mechanism to *push* these updates to the client in real-time.

### **4.2. The Solution: The Postgres-Electric Bridge**

The most robust architectural pattern identified involves chaining RisingWave with **ElectricSQL**, a sync engine designed to synchronize Postgres data to local clients.21

#### **4.2.1. The Protocol Mismatch**

ElectricSQL relies on PostgreSQL's Logical Replication feature (specifically the pgoutput plugin) to listen for changes in the Write-Ahead Log (WAL) and stream them to clients.21  
While RisingWave supports the Postgres wire protocol, it does not fully implement the internal storage engine mechanics of Postgres required to act as a Logical Replication Publisher for the pgoutput plugin in the way tools like Debezium or ElectricSQL expect.23 RisingWave has its own internal concepts of "Sources" and "Sinks" that are distinct from Postgres tables.25  
Therefore, a direct connection (RisingWave \-\> ElectricSQL) is currently not the standard supported path. Instead, the architecture necessitates an intermediate buffer.

#### **4.2.2. The Bridge Pipeline**

The recommended pipeline to bridge this gap is:

1. **Ingestion (RisingWave):** RisingWave ingests raw event data from sources like Kafka or direct CDC connectors from upstream operational databases (MySQL/Postgres).26  
2. **Processing (RisingWave):** Complex streaming SQL queries are defined to compute Materialized Views (MVs). For example, a "Live Dashboard" view that aggregates financial transactions by region in 1-second windows.28  
3. **Sinking (RisingWave \-\> Postgres):** RisingWave utilizes its **JDBC Sink Connector** to continuously write the results of these MVs into a standard, external PostgreSQL database.29 This sink supports "Upsert" mode, ensuring the Postgres table reflects the current state of the RisingWave view.  
4. **Syncing (Postgres \-\> ElectricSQL \-\> Client):** ElectricSQL is connected to this intermediate PostgreSQL database. It detects the changes (inserts/updates driven by the RisingWave sink) via Logical Replication and synchronizes them to the **ElectricCollection** running inside TanStack DB on the client.21

### **4.3. Implementing the RisingWave Sink**

Creating the link from RisingWave to the Postgres bridge requires defining a specific Sink resource. The SQL syntax follows this pattern:

SQL

CREATE SINK dashboard\_bridge\_sink FROM dashboard\_mv  
WITH (  
    connector \= 'jdbc',  
    jdbc.url \= 'jdbc:postgresql://postgres-bridge:5432/mydb',  
    table.name \= 'dashboard\_sync\_table',  
    type \= 'upsert',  \-- Crucial for maintaining current state  
    primary\_key \= 'region\_id'  
);

.29  
This command instructs RisingWave to continuously push updates from the dashboard\_mv materialized view into the dashboard\_sync\_table in Postgres.

### **4.4. Client-Side Consumption**

On the client side, the integration is handled by the @tanstack/electric-db-collection package. The developer defines an ElectricCollection that maps to the dashboard\_sync\_table.

TypeScript

// Conceptual integration  
const dashboardCollection \= createElectricCollection({  
  shape: { url:... table: 'dashboard\_sync\_table' },  
  //... configuration  
});

.21  
The result is a "Live Dashboard" where the heavy computational lifting (windowing, joining streams) is handled by RisingWave, and the "last mile" delivery to the browser is handled by ElectricSQL, with TanStack DB providing the reactive bindings for the UI.

### **4.5. Alternative: Direct WebSocket Subscription**

For use cases where the overhead of an intermediate Postgres instance is undesirable, architects can implement a custom **WebSocket Collection**.

1. **RisingWave Subscription:** RisingWave allows creating a SUBSCRIPTION that can output data to message brokers like Kafka.23  
2. **Relay Service:** A lightweight service (Node/Go) consumes the Kafka topic and broadcasts updates via WebSockets.  
3. **Custom TanStack DB Collection:** A custom collection creator is implemented 15 that listens to the WebSocket. When a message arrives, it calls the internal collection.insert or collection.update methods directly. This removes the ElectricSQL dependency but requires more custom code to handle reconnection logic, initial state fetching (snapshotting), and conflict resolution.

## ---

**5\. Integration Pattern C: The Computational Notebook Backend with Marimo**

The third integration pattern moves beyond databases to **Computational Engines**. **Marimo** is a reactive Python notebook environment that fundamentally reimagines the role of notebooks in production systems.7 Unlike Jupyter, which is often static and difficult to version control, Marimo notebooks are stored as pure Python files and execute reactively—running a cell automatically updates dependent cells.

### **5.1. Marimo as an API Server**

The most powerful integration pattern treats Marimo not as a tool for analysis, but as an application backend. Marimo notebooks can be executed programmatically and exposed as ASGI (Asynchronous Server Gateway Interface) applications via **FastAPI**.8

#### **5.1.1. The Architecture**

In this setup, TanStack DB acts as the state manager for the application's "Inputs," while Marimo serves as the processor for the "Outputs."

1. **Notebook Definition:** A Data Scientist authors a forecast.py notebook in Marimo. It defines inputs (e.g., mo.ui.slider) and complex Python logic (e.g., running a PyTorch model or a Pandas simulation).  
2. **Server Wrapping:** The notebook is wrapped in a FastAPI server using marimo.create\_asgi\_app(). This exposes the notebook's logic as a web service.  
   Python  
   \# forecast\_server.py  
   import marimo  
   from fastapi import FastAPI

   server \= marimo.create\_asgi\_app().with\_app(path="/forecast", root="forecast.py")  
   app \= FastAPI()  
   app.mount("/api", server.build())

.8  
3\. Client Integration: The TanStack DB client uses a QueryCollection or a standard useQuery hook to fetch data from this endpoint. When the user modifies inputs in the React UI (e.g., changing simulation parameters), TanStack DB triggers a fetch to the Marimo endpoint.  
4\. Reactivity: The Marimo server executes the relevant cells in the notebook and returns the computed results (JSON or HTML) to the client.  
This architecture effectively allows "Notebooks as Microservices," bridging the gap between Data Science teams (who work in Python) and Product Engineering teams (who work in React/TanStack).

### **5.2. Marimo "Islands" (WASM Integration)**

For scenarios requiring zero server-side latency, Marimo supports exporting notebooks to **WASM-powered HTML**, creating "Islands" of interactivity.32

* **Mechanism:** The notebook is compiled to run on **Pyodide** (Python in WASM).  
* **Integration:** The exported HTML/JS can be embedded into the React application.  
* **State Sync:** Communication between the React host and the Marimo island is achieved via the browser's window.postMessage API or shared localStorage. TanStack DB's LocalStorageCollection 30 is particularly useful here. The React app writes user inputs to LocalStorage; the Marimo WASM island watches LocalStorage for changes, re-runs the Python logic, and writes results back, which TanStack DB then picks up to update the UI.

This creates a fully client-side, offline-capable application where the "Backend" logic is actually Python code running in the browser, managed by TanStack DB.

## ---

**6\. Comparative Analysis: TanStack DB Stack vs. Convex**

Having established the "Composable Stack" (TanStack DB \+ DuckDB/RisingWave/Marimo), it is essential to compare this architecture against the "Integrated Stack" represented by **Convex**. Convex is a Backend-as-a-Service (BaaS) that provides a database, sync engine, and serverless functions in a single, vertically integrated platform.9

### **6.1. Architectural Philosophy**

* **The Composable Stack (TanStack DB):** Follows the "Headless" or "Local-First" philosophy. The developer explicitly chooses the database engine (DuckDB for analytics, Postgres/RisingWave for operations) and the sync layer. The client (TanStack DB) is the primary orchestrator of truth. This offers maximum flexibility and leverages best-in-class tools for specific problems (e.g., DuckDB is vastly superior to Convex for OLAP).  
* **The Integrated Stack (Convex):** Follows the "Batteries-Included" philosophy. The database is opaque and proprietary. The sync layer is invisible to the developer—you simply query a function, and it updates. The server is the absolute source of truth, but the client "feels" local due to aggressive caching and optimistic updates.34

### **6.2. Synchronization Mechanics**

**Convex:** Uses a push-based model over WebSockets. The client subscribes to a query function on the server. When the underlying data changes, the server re-runs the query and pushes the new result to the client. This is "Server-Side Reactivity".10 It is highly efficient for transactional data (OLTP) but less suitable for complex analytical queries that would be expensive to re-run constantly on the server.  
**TanStack DB:** Uses a pull-based or hybrid model (depending on the sync engine). With DuckDB, the "Server" is local, so synchronization is effectively memory access. With RisingWave/Electric, it relies on replication logs. The key difference is that TanStack DB performs **Client-Side Reactivity** via differential dataflow. The client can take raw data from the server and perform complex joins or filters *locally* without asking the server for a new dataset.12

### **6.3. Trade-Off Analysis**

| Feature | TanStack DB \+ Composable Backend | Convex (Integrated PaaS) |
| :---- | :---- | :---- |
| **Setup Complexity** | **High.** Requires configuring DBs, WASM bundles, Sync bridges (Electric), and client adapters. | **Low.** "Zero-config." Define a schema and a function, and it works immediately. |
| **Analytical Power** | **Extreme (via DuckDB).** Can handle millions of rows, complex SQL aggregations, and Parquet files in-browser. | **Limited.** Best for transactional data (documents, chats). Not designed for heavy OLAP or extensive aggregations. |
| **Real-Time Latency** | **Variable.** Local DuckDB queries are zero-latency. RisingWave streams depend on the pipeline (RisingWave \-\> PG \-\> Electric) latency (\~500ms). | **Low & Consistent.** WebSockets provide near-instant updates for standard transactional workloads. |
| **Offline Capability** | **Native.** Designed for local-first. DuckDB/LocalStorage collections work fully offline. | **Partial.** Supports optimistic updates, but fundamentally relies on server connectivity for truth. |
| **Vendor Lock-In** | **Low.** You own the infrastructure. Can swap RisingWave for Flink or DuckDB for SQLite. | **High.** deeply coupled to Convex's hosting, database, and auth systems. |

### **6.4. Developer Experience (DX)**

Convex offers a superior "Time to Hello World." The integration between the backend functions and the React frontend is seamless, with automatic type inference.9 TanStack DB requires more boilerplate—defining Zod schemas, configuring QueryClients, and managing the synchronization lifecycle. However, for specialized use cases—specifically heavy analytics (DuckDB) or high-velocity streaming (RisingWave)—the "Generic" nature of Convex becomes a limitation, whereas the specialized nature of the Composable Stack becomes a necessity.

## ---

**7\. Conclusion: The Convergence of Specialized Engines**

The integration of **TanStack DB** with **DuckDB**, **RisingWave**, and **Marimo** signals the arrival of a "Convergent Stack." Rather than relying on a single, monolithic backend to handle transactions, analytics, and streaming, architects can now compose specialized engines that run closer to the user than ever before.

* **DuckDB WASM** transforms the browser into an analytical warehouse, enabling "thick client" applications that defy traditional web performance limits.  
* **RisingWave** brings the power of continuous stream processing to the frontend, bridged by the robust synchronization capabilities of the Postgres ecosystem (via ElectricSQL).  
* **Marimo** unlocks the vast potential of the Python data science ecosystem, making it a viable reactive backend for web applications.

While **Convex** provides an unparalleled experience for standard real-time transactional apps, the Composable Stack is the superior choice for data-intensive, analytical, or scientific applications. By leveraging TanStack DB as the unifying reactive layer, architects can build systems that are not only performant and offline-capable but also modular enough to evolve with the changing landscape of data technology. The future of web architecture is not just client-side; it is **client-native**, where the database, the stream, and the notebook all converge within the user's browser.

#### **Works cited**

1. An Interactive Guide to TanStack DB | Frontend at Scale, accessed December 10, 2025, [https://frontendatscale.com/blog/tanstack-db/](https://frontendatscale.com/blog/tanstack-db/)  
2. TanStack DB, accessed December 10, 2025, [https://tanstack.com/db](https://tanstack.com/db)  
3. duckdb/duckdb-wasm: WebAssembly version of DuckDB \- GitHub, accessed December 10, 2025, [https://github.com/duckdb/duckdb-wasm](https://github.com/duckdb/duckdb-wasm)  
4. databases | MotherDuck Docs, accessed December 10, 2025, [https://motherduck.com/docs/integrations/databases/](https://motherduck.com/docs/integrations/databases/)  
5. RisingWave: Real-Time Event Streaming Platform, accessed December 10, 2025, [https://risingwave.com/](https://risingwave.com/)  
6. What is RisingWave? \- RisingWave, accessed December 10, 2025, [https://docs.risingwave.com/](https://docs.risingwave.com/)  
7. accessed December 10, 2025, [https://docs.marimo.io/llms.txt](https://docs.marimo.io/llms.txt)  
8. Programmatic \- marimo, accessed December 10, 2025, [https://docs.marimo.io/guides/deploying/programmatically/](https://docs.marimo.io/guides/deploying/programmatically/)  
9. Databases | TanStack Start React Docs, accessed December 10, 2025, [https://tanstack.com/start/latest/docs/framework/react/guide/databases](https://tanstack.com/start/latest/docs/framework/react/guide/databases)  
10. Convex with TanStack Query | Convex Developer Hub, accessed December 10, 2025, [https://docs.convex.dev/client/tanstack/tanstack-query/](https://docs.convex.dev/client/tanstack/tanstack-query/)  
11. Overview | TanStack DB Docs, accessed December 10, 2025, [https://tanstack.com/db/latest/docs](https://tanstack.com/db/latest/docs)  
12. TanStack DB 0.5 — Query-Driven Sync, accessed December 10, 2025, [https://tanstack.com/blog/tanstack-db-0.5-query-driven-sync](https://tanstack.com/blog/tanstack-db-0.5-query-driven-sync)  
13. Query Collection | TanStack DB Docs, accessed December 10, 2025, [https://tanstack.com/db/latest/docs/collections/query-collection](https://tanstack.com/db/latest/docs/collections/query-collection)  
14. DuckDB-Wasm: Efficient Analytical SQL in the Browser, accessed December 10, 2025, [https://duckdb.org/2021/10/29/duckdb-wasm](https://duckdb.org/2021/10/29/duckdb-wasm)  
15. Creating a Collection Options Creator | TanStack DB Docs, accessed December 10, 2025, [https://tanstack.com/db/latest/docs/guides/collection-options-creator](https://tanstack.com/db/latest/docs/guides/collection-options-creator)  
16. DuckDB Wasm, accessed December 10, 2025, [https://duckdb.org/docs/stable/clients/wasm/overview](https://duckdb.org/docs/stable/clients/wasm/overview)  
17. My browser WASM't prepared for this. Using DuckDB, Apache Arrow and Web Workers in real life \- Motif Analytics, accessed December 10, 2025, [https://motifanalytics.medium.com/my-browser-wasmt-prepared-for-this-using-duckdb-apache-arrow-and-web-workers-in-real-life-e3dd4695623d](https://motifanalytics.medium.com/my-browser-wasmt-prepared-for-this-using-duckdb-apache-arrow-and-web-workers-in-real-life-e3dd4695623d)  
18. Use DuckDB-WASM to query TB of data in browser \- Hacker News, accessed December 10, 2025, [https://news.ycombinator.com/item?id=45774571](https://news.ycombinator.com/item?id=45774571)  
19. Interactive SQL directly in the browser using DuckDB WASM \- Reddit, accessed December 10, 2025, [https://www.reddit.com/r/DuckDB/comments/1od5mas/interactive\_sql\_directly\_in\_the\_browser\_using/](https://www.reddit.com/r/DuckDB/comments/1od5mas/interactive_sql_directly_in_the_browser_using/)  
20. PostgreSQL Compatibility \- RisingWave: Real-Time Event Streaming Platform, accessed December 10, 2025, [https://risingwave.com/glossary/postgresql-compatibility/](https://risingwave.com/glossary/postgresql-compatibility/)  
21. Real-time sync for Postgres \- ElectricSQL, accessed December 10, 2025, [https://electric-sql.com/docs/llms/\_intro\_redux](https://electric-sql.com/docs/llms/_intro_redux)  
22. Mastering Real-Time Change Data Capture with Postgres \- RisingWave, accessed December 10, 2025, [https://risingwave.com/blog/mastering-real-time-change-data-capture-with-postgres/](https://risingwave.com/blog/mastering-real-time-change-data-capture-with-postgres/)  
23. Subscription \- RisingWave: Real-Time Event Streaming Platform, accessed December 10, 2025, [https://risingwave.com/glossary/subscription-risingwave-specific/](https://risingwave.com/glossary/subscription-risingwave-specific/)  
24. manually create replication slot for publication in PostgreSQL 10 \- Stack Overflow, accessed December 10, 2025, [https://stackoverflow.com/questions/49323419/manually-create-replication-slot-for-publication-in-postgresql-10](https://stackoverflow.com/questions/49323419/manually-create-replication-slot-for-publication-in-postgresql-10)  
25. Source, Table, MV, and Sink \- RisingWave docs, accessed December 10, 2025, [https://docs.risingwave.com/get-started/source-table-mv-sink](https://docs.risingwave.com/get-started/source-table-mv-sink)  
26. Connect to PostgreSQL CDC \- RisingWave docs, accessed December 10, 2025, [https://docs.risingwave.com/ingestion/sources/postgresql/pg-cdc](https://docs.risingwave.com/ingestion/sources/postgresql/pg-cdc)  
27. Ingest data from MySQL CDC \- RisingWave docs, accessed December 10, 2025, [https://docs.risingwave.com/ingestion/sources/mysql/mysql-cdc](https://docs.risingwave.com/ingestion/sources/mysql/mysql-cdc)  
28. Overview of data processing \- RisingWave docs, accessed December 10, 2025, [https://docs.risingwave.com/processing/overview](https://docs.risingwave.com/processing/overview)  
29. Sink data from RisingWave to PostgreSQL, accessed December 10, 2025, [https://docs.risingwave.com/integrations/destinations/postgresql](https://docs.risingwave.com/integrations/destinations/postgresql)  
30. Installation | TanStack DB Docs, accessed December 10, 2025, [https://tanstack.com/db/latest/docs/installation](https://tanstack.com/db/latest/docs/installation)  
31. marimo | a next-generation Python notebook, accessed December 10, 2025, [https://marimo.io/](https://marimo.io/)  
32. Exporting to HTML and other formats \- marimo, accessed December 10, 2025, [https://docs.marimo.io/guides/exporting/](https://docs.marimo.io/guides/exporting/)  
33. Cloudflare \- marimo, accessed December 10, 2025, [https://docs.marimo.io/guides/publishing/cloudflare/](https://docs.marimo.io/guides/publishing/cloudflare/)  
34. Use cases for using Tanstack Query and Convex together? \- Answer Overflow, accessed December 10, 2025, [https://www.answeroverflow.com/m/1281341284933701705](https://www.answeroverflow.com/m/1281341284933701705)