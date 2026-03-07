# **Architectural Synthesis of Sovereign Game State: Integrating SpacetimeDB, DuckDB WASM, TanStack Start, and CopilotKit**

## **1\. Introduction: The Convergence of Thick Clients and Smart Servers**

The contemporary landscape of decentralized application (dApp) development is witnessing a profound paradigm shift, moving away from fragmented, multi-tier architectures toward unified, high-performance stacks that collapse the distinction between database, server, and client. The integration of **Rust-based SpacetimeDB**, **DuckDB WASM**, **TanStack Start**, and **CopilotKit** represents a cutting-edge instance of this convergence, specifically tailored for complex, state-heavy applications like the *Tuath* MMO. This report analyzes the architectural synthesis of these four technologies to construct a "Thick Client, Smart Server" ecosystem, providing a comprehensive blueprint for developers seeking to build resilient, sovereign, and agentic digital territories.  
The *Tuath* project, characterized by its "Proof of Learning" (PoL) model and the *Anam* (a soulbound dynamic NFT), presents a unique set of technical challenges that necessitate this specific technology stack.1 Unlike traditional "Play-to-Earn" models which rely on simple transaction loops, *Tuath* requires the verifiable tracking of human capital development, complex "Education Tax" calculations on currency transfers, and the visualization of evolving avatar states based on linguistic progression.1 These requirements demand a backend capable of executing complex logic within transactions (SpacetimeDB), a frontend capable of heavy analytical processing without server round-trips (DuckDB WASM), a robust application framework to manage the hybrid rendering lifecycle (TanStack Start), and a semantic interface to lower the cognitive load for players (CopilotKit).

### **1.1 The Shift to "Database-as-Server"**

Traditional web architectures often suffer from the "impedance mismatch" between the application server (where logic resides) and the database (where state resides). This separation introduces latency, synchronization errors, and API fragility. SpacetimeDB addresses this by allowing developers to write game logic in Rust that executes directly within the database's transaction loop.1 This "Database-as-Server" paradigm ensures that the simulation state is always consistent and that complex operations—such as calculating the dynamic "Education Tax" based on a player's *Anam* level—are atomic.1  
However, pushing logic to the database creates a new challenge: data visibility. While SpacetimeDB excels at transactional throughput, it is not designed for the heavy, read-only analytical queries required by an AI agent or a data-rich dashboard. This is where the architecture bifurcates. We utilize SpacetimeDB for **Operational Transformation** (the authority) and introduce DuckDB WASM for **Analytical Processing** (the insight).

### **1.2 The "Thick Client" Analytical Layer**

The concept of the "Thick Client" is revitalized by WebAssembly (WASM). By embedding DuckDB—a high-performance, columnar SQL OLAP database—directly into the browser, we grant the client the ability to perform complex aggregations on the game state without querying the server.2 This is critical for the *Tuath* architecture. For an AI Copilot to advise a player on the optimal time to transfer assets to minimize tax, it must analyze historical ledger data and current "global learning velocity".1 Doing this on the server for thousands of concurrent players would be prohibitively expensive. Doing it on the client, inside a DuckDB instance synchronized via SpacetimeDB's subscription system, distributes the compute load to the edge.3

### **1.3 Agentic Semantic Binding**

The final piece of the convergence is CopilotKit. In a complex crypto-economic system, the user interface (UI) can become overwhelming. CopilotKit acts as the semantic binding layer, translating natural language user intent ("How am I doing in my language lessons?") into structured queries against the local DuckDB instance, and translating agentic intent ("Verify this task") into server-side SpacetimeDB reducer calls.4 This integration moves beyond simple chatbots to create a system where the AI has direct, governed access to the application's state and logic.

## ---

**2\. The Authoritative State Layer: SpacetimeDB & Rust**

The foundation of the proposed stack is SpacetimeDB, which manages the immutable state of the *Anam*, the *Ogham* currency ledgers, and the verification of tasks. By running Rust logic directly within the database transaction loop, it eliminates the need for a separate API server, collapsing the backend into a single deployable unit.

### **2.1 Rust Module Architecture and Reducers**

In SpacetimeDB, the core unit of logic is the **Reducer**. A reducer is a function that takes the current state of the database and an input, and transitions the database to a new state. This functional approach aligns perfectly with Rust's ownership model and type safety.6  
For the *Tuath* MMO, we define the primary tables using Rust structs annotated with \#\[spacetimedb::table\]. These tables serve as the single source of truth.  
**Table 1: Core Entity definitions for Tuath in Rust**

| Entity | Rust Struct | Purpose | Copilot Relevance |
| :---- | :---- | :---- | :---- |
| **Anam** | AnamState | Tracks knowledge\_level, particle\_count, color\_vector.1 | Used to visualize progress and determine tax brackets. |
| **Ledger** | OghamLedger | Tracks pending\_balance, synced\_balance, transaction history.1 | Source data for financial analysis and tax calculation. |
| **Tasks** | TaskLog | Records task\_id, verification\_status, timestamp.1 | Context for the agent to suggest next learning steps. |
| **Agents** | MechRequest | Queue for AI verification tasks (Olas Mech).1 | Interface for agent-to-agent verification. |

The reducers implementation requires careful consideration of "Agentic Access." Standard reducers are designed for human interaction speeds and UI feedback loops. However, an AI Copilot might need to batch operations or query hypothetical states. While SpacetimeDB does not currently support "dry-run" transactions natively in the client SDK, we can architect specific "Simulation Reducers" that calculate outcomes (like estimated tax) without committing changes, although a more efficient approach discussed later involves replicating this logic in DuckDB.  
The implementation of the transfer reducer, which applies the "Education Tax," illustrates the power of server-side Rust logic. The transfer hook program checks the sender's *Anam* level. If the level is low (indicating a speculator), a tax is applied. This logic is immutable and enforced by the database.1

Rust

\#\[reducer\]  
pub fn transfer(ctx: \&ReducerContext, recipient: Identity, amount: u64) \-\> Result\<(), String\> {  
    let sender\_anam \= ctx.db.anam\_state().find\_by\_identity(\&ctx.sender).ok\_or("No Anam found")?;  
    let tax\_rate \= calculate\_tax\_rate(sender\_anam.knowledge\_level);   
    let tax \= (amount as f64 \* tax\_rate) as u64;  
    let net\_amount \= amount \- tax;  
      
    // Update Ledger  
    ctx.db.ogham\_ledger().insert(OghamLedger {   
        identity: ctx.sender,   
        balance: current\_balance \- amount   
    });  
    //... distribute tax and update recipient  
    Ok(())  
}

This code compiles to WebAssembly and runs inside the SpacetimeDB host. The critical architectural detail here is that the *client* (TanStack Start) does not calculate the tax; it only requests the transfer. However, the *Copilot* needs to know the tax rate to advise the user. This necessitates the synchronization of the knowledge\_level and the tax formula to the client-side DuckDB instance.

### **2.2 Data Serialization and the SATS-JSON Bridge**

SpacetimeDB uses the Spacetime Algebraic Type System (SATS) for defining schemas. Communication with the client occurs via WebSockets using SATS-JSON, a JSON representation of the algebraic types.7  
The decision to use SATS-JSON over the binary BSATN format for the client interaction is driven by compatibility. While BSATN is more bandwidth-efficient, decoding binary streams in the browser to feed into DuckDB (which prefers Arrow or Parquet) introduces significant complexity. SATS-JSON allows us to use standard JavaScript JSON parsing, which—while slower than zero-copy Arrow buffers—is sufficient for the text-heavy data of an MMO like *Tuath* (quest logs, chat, inventory ids).  
However, a performance bottleneck exists here. High-frequency updates (e.g., the *Anam* particle vector changing 60 times a second) would overwhelm a JSON-based WebSocket subscription. Therefore, the architecture distinguishes between **High-Frequency State** (visuals) and **Low-Frequency State** (ledgers, levels). High-frequency state should be handled via transient client-side interpolation or a dedicated UDP channel if supported, while SpacetimeDB handles the authoritative Low-Frequency state.1

### **2.3 Identity and Authentication Integration**

SpacetimeDB provides a built-in identity system, mapping public keys to Identity structs. This creates a seamless onboarding experience where a cryptographic wallet (Solana, as mentioned in the *Tuath* research 1) can serve as the authenticator.  
The integration with TanStack Start involves the spacetimedb-sdk which manages the WebSocket connection and authentication lifecycle. A key requirement is determining where the Auth Token is stored. For a web-based game, localStorage is the standard, but this poses security risks. A more robust solution involves an HTTP-only cookie managed by the TanStack Start server functions, which proxies the initial authentication handshake, creating a session that the client SDK then utilizes.9  
The SpacetimeDB TypeScript SDK exposes a DbConnection class. We instantiate this as a singleton within the React application context. This connection manages the subscription lifecycle. When the user logs in, the client sends a subscribe message containing SQL queries (e.g., SELECT \* FROM AnamState WHERE identity \= @user). The server responds with an initial snapshot followed by incremental INSERT, UPDATE, and DELETE events.8

### **2.4 Managing the Subscription Lifecycle**

Efficient bandwidth usage requires dynamic subscriptions. A player exploring the "Forest of Syntax" only needs data for that region. SpacetimeDB supports this via spatial filtering in queries.  
The architecture employs a "View Manager" within the React state. As the player moves, the View Manager updates the subscription query:  
SELECT \* FROM WorldObjects WHERE x \> 100 AND x \< 200\.  
SpacetimeDB pushes the diff. This diff is not rendered directly; instead, it is piped into DuckDB. This decoupling is crucial. If we rendered directly from the WebSocket stream, the UI would flicker with every update. By buffering into DuckDB, we can query the local database at the render frame rate (60fps) while the network updates happen at their own pace.10

## ---

**3\. The Application Shell: TanStack Start and Vite Configuration**

TanStack Start serves as the unifying meta-framework, chosen for its ability to bridge the gap between a robust, indexable website (SSR) and a highly interactive, state-driven application (SPA).12 It leverages Vite as its build tool, which provides the necessary ecosystem for handling the complex WASM requirements of both SpacetimeDB and DuckDB.

### **3.1 Advanced Vite Configuration for Multi-WASM Support**

Integrating multiple WebAssembly modules into a single application creates a complex build environment. DuckDB WASM and SpacetimeDB's SDK both rely on modern browser features that can conflict with default bundler settings.  
To support these technologies, vite.config.ts requires specific plugins. vite-plugin-wasm is essential to allow standard ES module imports of .wasm files. Additionally, vite-plugin-top-level-await is required because the initialization patterns of these libraries often use the await keyword at the module level, which older browser targets do not support.14  
**Configuration specifics for the Tuath stack:**

1. **Target settings:** The build target must be set to esnext or at least es2022. This prevents Vite/esbuild from attempting to transpile the top-level await syntax into a promise chain that often breaks the WASM instantiation flow.16  
2. **Worker configuration:** DuckDB WASM is best run inside a Web Worker to keep the main thread free for React rendering. Vite handles workers via the new Worker() syntax, but the worker script itself needs the WASM plugins applied to its own build context.14  
3. **Optimization exclusions:** Libraries like @duckdb/duckdb-wasm should often be excluded from Vite's optimizeDeps pre-bundling. Pre-bundling can sometimes strip necessary assets or misconfigure the relative paths required to load the binary WASM file lazily.14

The configuration enables a seamless development experience where import duckdb from '@duckdb/duckdb-wasm' works directly, handling the underlying asset piping automatically.

### **3.2 Selective Server-Side Rendering (SSR) Strategy**

One of the most critical architectural decisions in using TanStack Start for a game like *Tuath* is the management of Server-Side Rendering. While SSR is excellent for the landing page, marketing content, and initial SEO, it is catastrophic for the game interface itself.  
The game logic relies on browser-specific APIs: WebSocket for SpacetimeDB, Worker for DuckDB, and Canvas or WebGL for the *Anam* visualization. None of these exist in the Node.js or Cloudflare Workers environment where the SSR happens. Attempting to render the game dashboard on the server would lead to immediate crashes or hydration errors where the server HTML (blank) differs from the client HTML (game board).  
TanStack Start provides the ssr: false option in the route definition.17 We apply this strictly to the /app and /game routes.

* **Public Routes (/, /about):** SSR enabled. The server renders the description of the game, the "Proof of Learning" manifesto, and community stats (fetched via a separate HTTP API if needed).  
* **Game Routes (/play, /dashboard):** SSR disabled. The server returns a skeletal HTML shell. The JavaScript bundle loads, initializes the WASM modules, connects the WebSocket, and then renders the UI. This "Client-Only" mode is essential for stability.17

### **3.3 Server Functions as Secure Proxies**

Although the game client connects directly to SpacetimeDB, TanStack Start's **Server Functions** play a vital role in security and integration with third-party services. The CopilotKit integration requires an API key for the LLM provider (e.g., OpenAI or Anthropic). Embedding this key in the client code is a security vulnerability.  
Server Functions allow us to create a secure endpoint that generates a temporary session token or proxies the chat request.

* **Implementation:** A server function generateCopilotToken() is defined. It runs strictly on the server (Node.js/Edge). It calls the LLM provider's API to generate an ephemeral key or signs a request.  
* **Usage:** The React client calls this function during initialization. TanStack Start handles the RPC wiring, ensuring the sensitive environment variables (OPENAI\_API\_KEY) never leak to the browser.19

Additionally, Server Functions can be used to interface with the Olas Mech marketplace for task verification if direct browser-to-blockchain interaction is not desired or requires a backend signature.1

### **3.4 Deployment on Cloudflare Workers**

The requirement to deploy this stack on Cloudflare Workers aligns with the distributed, serverless nature of the project. TanStack Start supports this via the @cloudflare/vite-plugin and proper wrangler configuration.20  
The deployment process involves:

1. **Build Output:** Vite generates two bundles: a client bundle (static assets) and a server bundle (worker script).  
2. **Wrangler Config:** The wrangler.toml must point to the server bundle as the entry point and the client bundle as the static assets directory.  
3. **Compatibility Flags:** The nodejs\_compat flag is often required if any dependencies rely on Node.js built-ins (like Buffer), even if polyfilled.20

This setup ensures that the static assets (WASM files, images) are served from Cloudflare's CDN, while the initial HTML render and Server Functions execute on the Edge, providing low-latency access globally.

## ---

**4\. The Analytical Engine: DuckDB WASM Integration**

The introduction of DuckDB WASM creates a high-performance "Live Data Warehouse" inside the user's browser. This component bridges the gap between the transactional stream from SpacetimeDB and the analytical needs of the Copilot.

### **4.1 The SpacetimeDB-to-DuckDB Pipeline**

The most technically demanding aspect of this architecture is the data pipeline. We must ingest the stream of SATS-JSON updates from SpacetimeDB into DuckDB tables efficiently.  
**The Pipeline Stages:**

1. **Ingestion (WebSocket):** The spacetimedb-sdk receives an onInsert event for the TaskLog table. This event contains a JavaScript object representing the new row.  
2. **Buffering:** Inserting rows one by one into DuckDB is inefficient due to the overhead of crossing the WASM boundary. We implement a **Micro-Batching Buffer**. Updates are collected in a Javascript array.  
3. **Flushing:** Every 100ms (or when the buffer reaches 1000 items), the buffer is flushed to DuckDB.  
4. **Insertion Strategy:**  
   * **Small Batches:** For typical gameplay updates (1-10 rows), we construct a parameterized SQL INSERT statement: INSERT INTO TaskLog VALUES (?,?,?), (?,?,?).  
   * **Large Snapshots:** When the game first loads, SpacetimeDB sends the entire table state (potentially tens of thousands of rows). Using SQL INSERT here is too slow.

The Arrow Optimization:  
Research highlights that using Apache Arrow for data ingestion is significantly faster than JSON parsing—up to 10-100x faster for large datasets.3 For the initial snapshot load:

1. We collect the raw SpacetimeDB objects.  
2. We use the apache-arrow JavaScript library to construct an ArrowTable in memory.  
3. We use DuckDB's insertArrowFromIPCStream (or similar API depending on version) to load this table with zero-copy overhead.  
   This optimization is critical. Without it, the "loading" screen of the game would persist for seconds while the browser parses JSON, degrading the user experience.21

### **4.2 Web Worker Architecture for Non-Blocking Analytics**

Analytical queries (OLAP) can be CPU intensive. Calculating the "global learning velocity" might involve aggregating millions of data points across the TaskLog. Running this on the main UI thread would cause the game to stutter (drop frames), breaking the immersion of the *Anam* visualization.  
To prevent this, DuckDB WASM is instantiated inside a **Web Worker**.

* **Isolation:** The heavy WASM binary and the memory heap reside in the worker.  
* **Communication:** The main thread (React) sends commands via postMessage.  
  * Command: {"action": "ingest", "table": "TaskLog", "data": \[...\]}  
  * Command: {"action": "query", "sql": "SELECT avg(score) FROM TaskLog"}  
* **Asynchronous Context:** The Copilot integration (discussed below) relies on this asynchronous nature. When the user asks a question, the Copilot sends a query message to the worker and awaits the promise resolution, leaving the UI responsive.21

### **4.3 Persistence and the Origin Private File System (OPFS)**

A key feature of DuckDB WASM is its ability to persist data to the browser's Origin Private File System (OPFS).22 This is crucial for the "Indigenous Data Sovereignty" aspect of *Tuath*.  
Instead of re-downloading the player's entire history every session, we can cache the *Anam* state and *TaskLog* locally in an OPFS-backed DuckDB database file.

* **Mechanism:** On startup, DuckDB checks for an existing database file.  
* **Synchronization:** The client sends the last\_synced\_timestamp to SpacetimeDB.  
* Delta Update: SpacetimeDB sends only the rows changed since that timestamp.  
  This drastically reduces bandwidth costs and server load, while giving the user true ownership of their data file, which can be exported or backed up independently of the central server.

## ---

**5\. The Agentic Layer: CopilotKit Implementation**

CopilotKit is the semantic interface that makes the application "Smart." It allows the user to interact with the game state using natural language. The integration focuses on two primary hooks: useCopilotReadable for providing context, and useCopilotAction for executing tasks.

### **5.1 Hierarchical Context with useCopilotReadable**

LLMs have a limited context window. We cannot feed the entire DuckDB database into the prompt. useCopilotReadable allows us to define *what* information is available, rather than providing the information itself.  
Schema-First Context Strategy:  
We provide the Copilot with the schema of the DuckDB tables.

TypeScript

useCopilotReadable({  
  description: "Game Analytics Database Schema (DuckDB)",  
  value: {  
    tables: \[  
      {   
        name: "OghamLedger",   
        columns: \["identity", "balance", "timestamp", "transaction\_type"\],  
        description: "Records of all currency transfers and taxes."  
      },  
      {  
        name: "AnamState",  
        columns: \["knowledge\_level", "color\_vector"\],  
        description: "Current state of the player's avatar."  
      }  
    \]  
  }  
});

This tells the agent *what* it can ask about. It does not bloat the context with rows.23  
Text-to-SQL Tooling:  
When the user asks, "How much tax did I pay last week?", the Copilot does not have the answer in its context. Instead, it recognizes that it needs to query the OghamLedger. We define a generic action/tool that allows the Copilot to execute SQL.

### **5.2 Bridging Actions with useCopilotAction**

The useCopilotAction hook connects the intent to the execution. We define a tool named queryGameStats.

TypeScript

useCopilotAction({  
  name: "queryGameStats",  
  description: "Executes a SQL query against the local game database to answer analytics questions.",  
  parameters:,  
  handler: async ({ sqlQuery }) \=\> {  
    // Send query to DuckDB Worker  
    const result \= await duckDBWorker.query(sqlQuery);  
    // Return result to Copilot to summarize for the user  
    return JSON.stringify(result);  
  }  
});

This pattern—**Structured RAG via SQL**—is far more effective for quantitative data than vector-based RAG. Vector search might find "similar" transactions, but SQL calculates the *exact* sum.24  
Transactional Actions:  
For mutating state, such as transferring Ogham, we define actions that wrap the SpacetimeDB reducers.

* **Action:** transferOgham(recipient, amount)  
* **Handler:**  
  1. **Pre-flight Check:** Query DuckDB to see if the user has sufficient balance.  
  2. **Advisory:** If the tax rate is high (checked via local logic), the Copilot can prompt the user: "Warning: Your current level implies a 5% tax. Proceed?" (This utilizes the Human-in-the-Loop capability of CopilotKit).  
  3. **Execution:** Call spacetimeDB.reducers.transfer(...).

This creates a "Thick Client" agent. The agent runs locally, checks local data, and acts as a guardian before interacting with the irreversible blockchain/database layer.1

### **5.3 Generative UI for Data Visualization**

Textual answers are often insufficient for game data. CopilotKit's **Generative UI** allows the agent to render React components in the chat stream.

* **Scenario:** User asks "Visualize my learning progress."  
* **Action:** Copilot calls renderProgressChart.  
* **Component:** The handler returns a \<Recharts /\> component.  
* **Data Source:** The component props are populated by the result of a DuckDB query executed implicitly by the agent.

This seamless blend of Chat, SQL, and UI Components creates a dashboard that builds itself based on user curiosity.25

## ---

**6\. Deployment and Operational Considerations**

### **6.1 Monorepo Structure and Type Sharing**

To maintain sanity in a stack with Rust (Server) and TypeScript (Client), a monorepo structure is mandatory.  
/tuath-monorepo  
/packages  
/server-module (Rust: SpacetimeDB)  
/src/lib.rs  
spacetime.toml  
/client-web (TypeScript: TanStack Start)  
/src  
/module\_bindings (Generated types)  
/workers (DuckDB)  
vite.config.ts  
wrangler.toml  
Type Sharing Pipeline:  
The spacetime generate CLI command is the glue. It reads the Rust structs and generates TypeScript interfaces. This command should be part of the build pipeline.

* npm run build:types: Runs spacetime generate targeting the client's source folder.  
* This ensures that if the Rust developer adds a tax\_bracket column to OghamLedger, the TypeScript client (and the Copilot schema definition) immediately receives type errors until updated, preventing runtime failures.26

### **6.2 Performance Tuning and Memory Limits**

WASM in the browser has hard limits. Chrome tabs typically crash around 2GB-4GB of memory usage.

* **DuckDB Limits:** We must configure DuckDB's memory limit option during instantiation to prevent it from consuming all available heap, which would crash the SpacetimeDB connection.  
* **Eviction Policies:** The TaskLog table can grow indefinitely. The client needs an eviction policy. E.g., "Keep detailed logs for 7 days, then aggregate into weekly summaries and delete raw rows." This logic can be automated using DuckDB's scheduled queries or a simple startup routine.22

### **6.3 Security of the Agentic Interface**

Allowing an LLM to execute SQL queries and call reducers introduces Prompt Injection risks.

* **ReadOnly Connection:** The DuckDB connection used by queryGameStats should be read-only to prevent the agent (or a malicious prompt) from dropping tables.  
* **Reducer Scoping:** The transfer action should strictly validate inputs. The SpacetimeDB reducer itself is the ultimate gatekeeper, checking signatures and balances, so even if the Agent is tricked into calling transfer with bad data, the database transaction will fail safely.27

## ---

**7\. Conclusion**

The integration of **SpacetimeDB**, **DuckDB WASM**, **TanStack Start**, and **CopilotKit** creates a technology stack that is greater than the sum of its parts. It solves the fundamental tension in decentralized gaming: the need for authoritative, secure state (Rust/SpacetimeDB) versus the need for rich, responsive, and personalized user experiences (DuckDB/CopilotKit).  
By utilizing SpacetimeDB as the "Database-as-Server," we reduce backend complexity. By leveraging DuckDB WASM, we enable "Indigenous Data Sovereignty," allowing players to own and analyze their data locally. TanStack Start provides the robust delivery mechanism, and CopilotKit humanizes the interaction. This architecture is not merely a collection of tools but a comprehensive strategy for building the next generation of sovereign, intelligent, and persistent digital worlds.

#### **Works cited**

1. Ogham Crypto MMO Research.md  
2. DuckDB-Wasm: Efficient Analytical SQL in the Browser, accessed December 20, 2025, [https://duckdb.org/2021/10/29/duckdb-wasm](https://duckdb.org/2021/10/29/duckdb-wasm)  
3. Building a High-Performance Statistical Dashboard with DuckDB-WASM and Apache Arrow, accessed December 20, 2025, [https://medium.com/@ryanaidilp/building-a-high-performance-statistical-dashboard-with-duckdb-wasm-and-apache-arrow-d6178aeaae6d](https://medium.com/@ryanaidilp/building-a-high-performance-statistical-dashboard-with-duckdb-wasm-and-apache-arrow-d6178aeaae6d)  
4. Frontend Actions \- CopilotKit docs, accessed December 20, 2025, [https://docs.copilotkit.ai/crewai-flows/frontend-actions](https://docs.copilotkit.ai/crewai-flows/frontend-actions)  
5. useCopilotAction \- CopilotKit Docs, accessed December 20, 2025, [https://docs.copilotkit.ai/reference/hooks/useCopilotAction](https://docs.copilotkit.ai/reference/hooks/useCopilotAction)  
6. spacetimedb \- Rust \- Docs.rs, accessed December 20, 2025, [https://docs.rs/spacetimedb/latest/spacetimedb/](https://docs.rs/spacetimedb/latest/spacetimedb/)  
7. SATS-JSON Data Format | SpacetimeDB docs, accessed December 20, 2025, [https://spacetimedb.com/docs/sats-json](https://spacetimedb.com/docs/sats-json)  
8. Subscription Reference | SpacetimeDB docs, accessed December 20, 2025, [https://spacetimedb.com/docs/subscriptions/](https://spacetimedb.com/docs/subscriptions/)  
9. React Integration | SpacetimeDB docs, accessed December 20, 2025, [https://spacetimedb.com/docs/spacetimeauth/react-integration/](https://spacetimedb.com/docs/spacetimeauth/react-integration/)  
10. spacetimedb \- NPM, accessed December 20, 2025, [https://www.npmjs.com/package/spacetimedb](https://www.npmjs.com/package/spacetimedb)  
11. SpacetimeDB/crates/client-api-messages/src/websocket.rs at master \- GitHub, accessed December 20, 2025, [https://github.com/clockworklabs/SpacetimeDB/blob/master/crates/client-api-messages/src/websocket.rs](https://github.com/clockworklabs/SpacetimeDB/blob/master/crates/client-api-messages/src/websocket.rs)  
12. Key Web Development Trends for 2026 | by Onix React | Dec, 2025 \- Medium, accessed December 20, 2025, [https://medium.com/@onix\_react/key-web-development-trends-for-2026-800dbf0a7c8c](https://medium.com/@onix_react/key-web-development-trends-for-2026-800dbf0a7c8c)  
13. TanStack Start, accessed December 20, 2025, [https://tanstack.com/start](https://tanstack.com/start)  
14. vite-plugin-wasm \- NPM, accessed December 20, 2025, [https://www.npmjs.com/package/vite-plugin-wasm](https://www.npmjs.com/package/vite-plugin-wasm)  
15. Using Rust WebAssembly in Vite \+ React: A Modern Game of Life Example, accessed December 20, 2025, [https://dev.to/jambochen/using-rust-webassembly-in-vite-react-a-modern-game-of-life-example-hde](https://dev.to/jambochen/using-rust-webassembly-in-vite-react-a-modern-game-of-life-example-hde)  
16. Features \- Vite, accessed December 20, 2025, [https://vite.dev/guide/features](https://vite.dev/guide/features)  
17. Selective Server-Side Rendering (SSR) | TanStack Start React Docs, accessed December 20, 2025, [https://tanstack.com/start/latest/docs/framework/react/guide/selective-ssr](https://tanstack.com/start/latest/docs/framework/react/guide/selective-ssr)  
18. Turning off SSR doesn't seem to work · TanStack router · Discussion \#4616 \- GitHub, accessed December 20, 2025, [https://github.com/TanStack/router/discussions/4616](https://github.com/TanStack/router/discussions/4616)  
19. Server Functions | TanStack Start React Docs, accessed December 20, 2025, [https://tanstack.com/start/latest/docs/framework/react/guide/server-functions](https://tanstack.com/start/latest/docs/framework/react/guide/server-functions)  
20. TanStack Start · Cloudflare Workers docs, accessed December 20, 2025, [https://developers.cloudflare.com/workers/framework-guides/web-apps/tanstack-start/](https://developers.cloudflare.com/workers/framework-guides/web-apps/tanstack-start/)  
21. My browser WASM't prepared for this. Using DuckDB, Apache Arrow and Web Workers in real life \- Motif Analytics, accessed December 20, 2025, [https://motifanalytics.medium.com/my-browser-wasmt-prepared-for-this-using-duckdb-apache-arrow-and-web-workers-in-real-life-e3dd4695623d](https://motifanalytics.medium.com/my-browser-wasmt-prepared-for-this-using-duckdb-apache-arrow-and-web-workers-in-real-life-e3dd4695623d)  
22. DuckDB Wasm, accessed December 20, 2025, [https://duckdb.org/docs/stable/clients/wasm/overview](https://duckdb.org/docs/stable/clients/wasm/overview)  
23. useCopilotReadable \- CopilotKit Docs, accessed December 20, 2025, [https://docs.copilotkit.ai/reference/hooks/useCopilotReadable](https://docs.copilotkit.ai/reference/hooks/useCopilotReadable)  
24. RAG vs. Prompt Stuffing: Overcoming Context Window Limits for Large, Information-Dense Documents \- Spyglass MTG, accessed December 20, 2025, [https://www.spyglassmtg.com/blog/rag-vs.-prompt-stuffing-overcoming-context-window-limits-for-large-information-dense-documents](https://www.spyglassmtg.com/blog/rag-vs.-prompt-stuffing-overcoming-context-window-limits-for-large-information-dense-documents)  
25. Build Your Own Knowledge-Based RAG Copilot | Blog | CopilotKit, accessed December 20, 2025, [https://www.copilotkit.ai/blog/build-your-own-knowledge-based-rag-copilot](https://www.copilotkit.ai/blog/build-your-own-knowledge-based-rag-copilot)  
26. TypeScript Reference | SpacetimeDB docs, accessed December 20, 2025, [https://spacetimedb.com/docs/sdks/typescript/](https://spacetimedb.com/docs/sdks/typescript/)  
27. Rust Quickstart | SpacetimeDB docs, accessed December 20, 2025, [https://spacetimedb.com/docs/modules/rust/quickstart/](https://spacetimedb.com/docs/modules/rust/quickstart/)