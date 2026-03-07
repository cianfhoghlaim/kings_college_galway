# **Architectural Synthesis of High-Performance Geospatial Workflows: Integrating Cloud-Native OLAP and WebGPU Rendering for Meteorological Particle Simulation**

## **Executive Summary**

The geospatial data science landscape is currently undergoing a structural revolution, transitioning from file-based, desktop-centric workflows to cloud-native, serverless architectures that prioritize zero-copy data transport and hardware-accelerated visualization. This report presents a comprehensive technical blueprint for a modern geospatial workflow, explicitly designed to ingest, process, and visualize high-velocity meteorological data from the **UK Met Office** and **GeoHive (Ireland)**. The proposed architecture leverages the **opengeos** and **giswqs** ecosystem to integrate **DuckDB**, **MotherDuck**, **PlanetScale**, **GeoParquet**, **Lonboard**, **Ibis**, and **Marimo**. The primary technical objective is to replicate "game-like" visual fidelity—specifically particle-based wind flow simulations—within a browser-based analytical environment.  
A central component of this analysis is a rigorous comparative evaluation against **SpacetimeDB**, an emerging "database-as-backend" technology. While SpacetimeDB offers a unified approach to state management ideal for multiplayer gaming logic, this report argues that the **DuckDB-GeoArrow-Lonboard** pipeline provides superior performance for scientific visualization. This advantage stems from its optimization for vectorized memory transport and client-side WebGPU compute, which decouples the visual simulation from the bandwidth constraints of server-authoritative state synchronization.  
This document serves as a foundational reference for data engineers and geospatial architects seeking to implement high-performance dashboards that bridge the gap between traditional GIS precision and modern video game graphics.

## ---

**1\. Introduction: The Convergence of GIS and Real-Time Graphics**

### **1.1 The Shift to Cloud-Native Geospatial**

Historically, Geographic Information Systems (GIS) have been characterized by heavy client-side software, monolithic spatial databases (like PostGIS), and intermediate file formats (Shapefiles, GeoJSON) that require significant serialization overhead. However, the emergence of the "Modern Data Stack" has introduced tools that are modular, ephemeral, and incredibly fast. This shift is typified by the "Cloud-Native Geospatial" paradigm, which emphasizes accessing data directly from object storage (S3) using range requests, rather than downloading entire datasets.  
The **opengeos** initiative, championed by researchers and developers such as Qiusheng Wu (giswqs), represents the vanguard of this movement.1 By prioritizing open-source tools that leverage binary formats and efficient memory management, this ecosystem allows for the processing of datasets—such as global weather models—that were previously the domain of supercomputers or dedicated workstations.

### **1.2 The Challenge of Game-Like Fidelity**

Users increasingly demand visualization interfaces that match the fluidity and responsiveness of video games. In the context of meteorology, this means moving beyond static isobars or "hedgehog" arrow plots to dynamic, animated particle systems that visualize wind flow as a continuous fluid medium. Achieving this requires a rendering pipeline that can handle hundreds of thousands of moving entities at 60 frames per second (FPS).  
This requirement creates a technical tension between **Analytical Precision** (the domain of SQL databases) and **Visual Performance** (the domain of Game Engines/GPUs). The workflow proposed herein seeks to resolve this tension by integrating a high-performance analytical engine (**DuckDB**) with a WebGPU-accelerated visualization library (**Lonboard**), mediated by a zero-copy transport layer (**GeoArrow**).

### **1.3 Scope of Analysis**

This report focuses on two primary meteorological datasets:

1. **Met Office (UK):** Global Spot Data and Atmospheric Model outputs.  
2. **GeoHive / Met Éireann (Ireland):** The HARMONIE-AROME high-resolution numerical weather prediction (NWP) model.

The analysis will detail the ingestion of these datasets, their normalization via **Ibis**, state management via **PlanetScale**, and final rendering in **Marimo** notebooks. It will then contrast this approach with **SpacetimeDB**, evaluating the trade-offs between a modular OLAP-centric stack and a unified, reducer-based simulation backend.

## ---

**2\. The Modern Geospatial Stack: Component Architecture**

The proposed architecture is not a monolithic application but a composable pipeline. Each tool is selected for its ability to handle specific types of complexity: computational, semantic, or visual.

### **2.1 The Computational Core: DuckDB and MotherDuck**

#### **2.1.1 DuckDB: The In-Process Analytical Engine**

**DuckDB** serves as the primary computational engine for this workflow. Often described as "SQLite for analytics," DuckDB is an embedded SQL OLAP database designed for vectorized query execution.3 Unlike row-oriented databases (PostgreSQL, MySQL), DuckDB organizes data by columns, which allows for highly efficient compression and CPU cache utilization—critical factors when processing the dense float arrays found in meteorological GRIB2 files.  
The pivotal feature for this workflow is the **DuckDB Spatial Extension**. This extension bundles the **GDAL** (Geospatial Data Abstraction Library) drivers, enabling DuckDB to function as a virtual file system. Through functions like ST\_Read, DuckDB can mount remote GRIB2 files (hosted on HTTP servers or S3 buckets) and query them directly as if they were local tables.4 This capability is fundamental to the "Cloud-Native" approach, as it eliminates the need for an intermediate ETL (Extract, Transform, Load) step to ingest massive weather model runs into a database before querying.

#### **2.1.2 MotherDuck: Hybrid Execution Strategy**

**MotherDuck** extends DuckDB into the cloud, enabling a serverless, collaborative data warehousing model. In the context of this workflow, MotherDuck solves the "Data Gravity" problem.

* **Historical Archive:** While local DuckDB instances are excellent for processing the "latest" forecast (hundreds of megabytes), analyzing historical trends (e.g., "Compare today's Storm Kathleen with 2014's Storm Darwin") involves terabytes of data. MotherDuck hosts this historical archive.  
* **Hybrid Querying:** The duckdb client can execute queries that join local data (the current forecast GRIB file) with remote data (historical climatology in MotherDuck). MotherDuck’s engine intelligently separates the query plan, executing the heavy aggregations in the cloud and returning only the results to the local Marimo client.5

### **2.2 The Transactional State Layer: PlanetScale**

While DuckDB handles immutable analytical data, an interactive application requires a mutable state: user preferences, saved viewports, annotation layers, and session management. **PlanetScale** fulfills this role.

#### **2.2.1 Architecture and Integration**

PlanetScale is built on **Vitess**, a database clustering system for horizontal scaling of MySQL (and now PostgreSQL). Recently, PlanetScale introduced support for PostgreSQL, including the **pg\_duckdb** extension.6 This integration is architecturally significant.

* **The "Lakehouse" Pattern:** By installing pg\_duckdb within PlanetScale, the transactional database acts as a gateway to the analytical warehouse. A user application can send a standard SQL query to PlanetScale to retrieve a user's saved location, and in the same transaction, join that location data with wind vector data residing in MotherDuck.7  
* **Performance:** PlanetScale’s architecture is optimized for high-concurrency, low-latency lookups (OLTP). This ensures that the application interface remains snappy (e.g., logging in, loading lists of saved maps) even while heavy analytical queries are processing in the background.

### **2.3 The Semantic Layer: Ibis**

One of the persistent challenges in geospatial engineering is "SQL Dialect Fatigue." Syntax varies between PostGIS, DuckDB Spatial, and BigQuery GIS. **Ibis** addresses this by providing a unified, Pythonic dataframe API that compiles to SQL.8

* **Expression Trees:** Unlike Pandas, which executes operations immediately (eager evaluation), Ibis builds a lazy expression tree. This allows the framework to optimize the query before execution.  
* **Engine Agnosticism:** By writing the geospatial transformation logic (e.g., table.filter(st\_intersects(...))) in Ibis, the workflow becomes decoupled from the backend. The same Python code can drive a local DuckDB instance during development and a MotherDuck or PlanetScale backend in production, simply by switching the connection object.9

### **2.4 The Transport Layer: GeoParquet and GeoArrow**

The bottleneck in most web-based GIS is serialization. Converting binary database rows into textual GeoJSON (a JSON-based format) requires expensive parsing and significantly inflates file size.

* **GeoParquet:** This format extends Apache Parquet to support geospatial types. It is used for the *persistent storage* of processed weather tiles. Its columnar compression (Snappy, Zstd) is highly effective for repetitive grid coordinates.10  
* **GeoArrow:** This is the *in-memory* standard. When DuckDB executes a query, it can output the result as an Arrow table—a contiguous block of memory. This binary buffer can be passed directly to Python and then to the JavaScript/GPU layer without serialization. This "Zero-Copy" transfer is the technological breakthrough that enables visualizing millions of particles in the browser.11

### **2.5 The Visualization Layer: Lonboard and Marimo**

**Lonboard** is a bridge library connecting Python data (GeoArrow) to **Deck.gl** (JavaScript/WebGL). **Marimo** is a next-generation reactive notebook environment.

* **Reactive Execution:** Unlike Jupyter, which maintains a hidden global state that can lead to out-of-order execution errors, Marimo treats the notebook as a Directed Acyclic Graph (DAG).13 If a user moves a time slider, Marimo automatically re-executes only the dependent cells (e.g., the DuckDB query and the map render), ensuring a responsive, glitch-free dashboard.  
* **WebGPU Context:** Marimo supports **AnyWidget**, a protocol for embedding modern JavaScript widgets. This allows the workflow to instantiate custom Deck.gl layers that utilize WebGPU compute shaders, bypassing the limitations of the standard DOM.14

## ---

**3\. Data Engineering: Ingesting UK and Irish Meteorological Data**

To visualize wind flow, the system must ingest **Vector Fields**: grids where every point contains a $U$ (Zonal/East-West) and $V$ (Meridional/North-South) component.

### **3.1 UK Met Office Data Structure**

The UK Met Office exposes data via the **Weather DataHub**. For high-fidelity visualisations, two primary products are relevant:

1. **Global Spot Data:** Provides point-based forecasts. While useful for validation, it lacks the spatial continuity required for particle simulation.15  
2. **Atmospheric Models (UKV / Global):** These provide gridded fields. The data is delivered in **GRIB2** format.

#### **3.1.1 GRIB2 Structure**

A GRIB2 (General Regularly-distributed Information in Binary form) file is a container format composed of multiple "messages." Each message corresponds to a specific variable (e.g., Wind Speed) at a specific vertical level (e.g., 10m above ground) and forecast step.

* **Section 0:** Indicator Section (File type).  
* **Section 3:** Grid Definition Template (Defining the geometry—Lat/Lon vs Rotated Pole).  
* **Section 4:** Product Definition Template (Parameter category: Momentum; Parameter number: U-component).  
* **Section 5:** Data Representation Template (Packing method, typically JPEG2000 or CCSDS).  
* **Section 7:** Data Template (The actual binary payload).

**Ingestion Strategy:** DuckDB's ST\_Read utilizes the GDAL GRIB driver. To extract the wind vectors, the query must filter by the GRIB "Element" or "Band." Typically, Band 1 is $U$ and Band 2 is $V$ in combined files, but often they are distributed as separate files.

### **3.2 Met Éireann (GeoHive) Data Structure**

Met Éireann operates the **HARMONIE-AROME** model, a Limited Area Model (LAM) focused on Ireland.

* **Resolution:** 2.5km horizontal grid.  
* **Update Cycle:** 54-hour forecasts produced every 3 hours (00Z, 03Z, etc.).  
* **Systems:** Historically **IREPS** (Irish Regional Ensemble Prediction System), recently upgraded to **DINI-EPS** (Denmark-Ireland-Netherlands-Iceland) collaboration.16

#### **3.2.1 Access via Open Data**

While GeoHive acts as the geospatial portal, the raw GRIB2 files are hosted on Met Éireann's Open Data HTTP servers (https://opendata.met.ie).

* **File Naming Convention:** Harmonie\_IRE\_2.5km\_wind\_YYYYMMDDHH.grib2.  
* **Projection:** HARMONIE uses a **Lambert Conformal Conic** projection (to minimize distortion over Ireland) or a Rotated Lat/Lon grid. This contrasts with the Global Met Office models which often use WGS84 (EPSG:4326).

Ingestion Challenge: Particle visualization libraries (Deck.gl) generally expect Web Mercator or WGS84 coordinates.  
Solution: The DuckDB ingestion query must perform an on-the-fly coordinate transformation (ST\_Transform) to reproject the HARMONIE vectors from Lambert Conformal to WGS84.

### **3.3 Harmonization via Ibis**

The power of Ibis lies in its ability to abstract these differences. We can define a "Virtual Schema" for wind data and map both sources to it.

| Standard Field | Met Office Source | Met Éireann Source |
| :---- | :---- | :---- |
| timestamp | forecast\_reference\_time \+ step | validityTime |
| geometry | ST\_Point(lon, lat) | ST\_Transform(ST\_Point(x,y), 2157, 4326\) |
| u\_vector | band\_1 (Param 2, Cat 2\) | u-component |
| v\_vector | band\_2 (Param 3, Cat 2\) | v-component |

**Table 1:** Schema Mapping for Wind Vector Normalization.

Python

\# Conceptual Ibis Normalization Logic  
import ibis

def normalize\_wind(table, source\_type):  
    if source\_type \== 'met\_office':  
        return table.select(  
            time='forecast\_time',  
            u=table\['wind\_u\_10m'\],  
            v=table\['wind\_v\_10m'\],  
            geometry=ibis.geo.point(table.lon, table.lat)  
        )  
    elif source\_type \== 'met\_eireann':  
        \# Apply projection transform if needed via expression  
        return table.select(  
            time='validity\_time',  
            u=table\['u\_10m'\],  
            v=table\['v\_10m'\],  
            geometry=ibis.geo.transform(table.geom, 4326\)  
        )

## ---

**4\. Visualization Mechanics: Creating "Game-Like" Particle Effects**

The requirement for "game-like" effects implies a level of interactivity and visual smoothness (60 FPS) that static map tiles cannot provide. In fluid dynamics visualization, this is achieved through **Lagrangian Particle Tracking**.

### **4.1 The Physics of Flow**

There are two ways to represent fluid flow:

1. **Eulerian:** Inspecting the fluid properties (velocity, pressure) at fixed points in space (the Grid). This is what the GRIB2 file contains.  
2. **Lagrangian:** Following specific particles as they move through space and time. This is what the visualization renders.

The Simulation Loop:  
To visualize the Eulerian data (grid) in a Lagrangian way (particles), the rendering engine must perform Numerical Integration.

$$P\_{t+1} \= P\_t \+ \\vec{V}(P\_t) \\cdot \\Delta t$$

Where:

* $P\_t$ is the particle position at time $t$.  
* $\\vec{V}(P\_t)$ is the velocity vector sampled from the grid at position $P\_t$.  
* $\\Delta t$ is the time step.

### **4.2 WebGPU and Deck.gl Implementation**

Simulating 100,000+ particles using this equation on a CPU is too slow for JavaScript. The solution uses **WebGPU** (or WebGL2 Transform Feedback) to perform this integration on the Graphics Processing Unit.

#### **4.2.1 The Texture Strategy**

Instead of passing 100,000 velocity values to the GPU every frame, we pass the "Vector Field" as a **Texture** (an image).

* **Red Channel:** Encodes the U-component (scaled to 0-255).  
* **Green Channel:** Encodes the V-component.  
* **Blue Channel:** (Optional) Encodes temperature or magnitude.

DuckDB reads the GRIB2 data and exports it not as a list of points, but as a binary image buffer (PNG or raw bytes). This buffer is uploaded to the GPU memory once.

#### **4.2.2 The Compute Shader**

A WebGPU Compute Shader runs for every particle instance:

1. **Sample:** It reads the particle's current coordinate $(x, y)$.  
2. **Lookup:** It samples the Velocity Texture at $(x, y)$ to get $\\vec{V}$.  
3. **Integrate:** It calculates the new position.  
4. **Boundary Check:** If the particle moves off-screen or exceeds a "lifetime" counter, it resets to a random position.

### **4.3 Extending Lonboard with AnyWidget**

**Lonboard** natively supports ScatterplotLayer and PathLayer, which are insufficient for this simulation loop. We must extend it using **AnyWidget**.  
**AnyWidget** allows us to write a custom JavaScript module that wraps a specialized Deck.gl layer (like ParticleLayer from the weatherlayers or deck.gl-particle community packages) and expose it to Python.17

* **Python Side (WindWidget.py):** Defines a class inheriting from anywidget.AnyWidget. It has Traitlets for u\_texture, v\_texture, particle\_count, and speed\_factor.  
* **JavaScript Side (widget.js):** Listens for changes to these traits. When the u\_texture changes (because the user moved the time slider in Marimo), the JS updates the Deck.gl layer's texture uniform.

Synchronization:  
Because Marimo uses a reactive execution graph, connecting the Time Slider to the DuckDB query automatically triggers the chain:  
Slider Move \-\> DuckDB Query \-\> Ibis Processing \-\> GeoArrow/Image Output \-\> AnyWidget Update \-\> GPU Render.  
This creates a seamless, "game-like" experience where the wind field shifts smoothly as the user scrubs through time.

## ---

**5\. Comparative Architecture: SpacetimeDB**

To fully evaluate the proposed stack, we must compare it against **SpacetimeDB**, a technology that fundamentally rethinks the relationship between the database and the application.

### **5.1 SpacetimeDB: The Database IS the Server**

Traditional architectures separate the Database (Postgres) from the Backend Server (Node.js/Python). **SpacetimeDB** unifies them. It is a relational database that executes application logic (written in Rust or C\#) *inside* the database transaction loop.18

* **Reducers:** Instead of API endpoints, you define "Reducers"—functions that mutate the database state.  
* **Tick Rate:** The database has a concept of "time" and can run scheduled reducers (e.g., update\_physics()) every tick.  
* **Client Sync:** Clients subscribe to tables. When a reducer changes a row, the database automatically pushes the update to the client SDK.

### **5.2 The Particle Effect Challenge in SpacetimeDB**

How would one implement the "Wind Particle" simulation in SpacetimeDB?

#### **5.2.1 Approach A: Server-Authoritative Particles**

In this model, every particle is a row in a Particles table: (id, x, y, velocity).

* A server-side reducer iterates through the table 60 times a second, updating $x$ and $y$ based on the wind field.  
* **Failure Mode:** This requires broadcasting the position of 100,000 particles to every connected client 60 times a second. The bandwidth requirement (approx 100MB/s) is impossible for web clients. SpacetimeDB is optimized for *game state* (inventory, player health, position of 50 players), not *dense simulation data*.20

#### **5.2.2 Approach B: Client-Side Simulation (The Hybrid)**

In this model, SpacetimeDB stores only the **Wind Field** (the grid data).

* The client connects and downloads the Wind Field.  
* The client performs the particle simulation locally (using Unity/C\# or JS).  
* **Comparison:** In this scenario, SpacetimeDB acts merely as a data distribution API. However, it lacks the specialized compression of GeoParquet or the range-request capabilities of DuckDB. It would require parsing the GRIB2 file into SpacetimeDB tables (inserting millions of rows), which is far less efficient than DuckDB's zero-copy ST\_Read.

### **5.3 Comparison Matrix**

| Feature | OpenGEOS Stack (DuckDB/Lonboard) | SpacetimeDB |
| :---- | :---- | :---- |
| **Primary Philosophy** | **Data Gravity:** Move compute to the data (SQL/WebGPU). | **Unified State:** Logic lives with the data (Reducers). |
| **Data Ingestion** | **Native:** Reads GRIB2/Parquet directly. Zero-ETL. | **Custom:** Requires writing parsers to import data into DB tables. |
| **Particle Simulation** | **Client-Side (GPU):** Simulates 1M+ particles at 60 FPS. | **Server-Side (CPU):** Bandwidth limited. **Client-Side:** Lacks native geospatial compression. |
| **State Synchronization** | **Manual:** Re-query on change. Good for analytics. | **Automatic:** Real-time push. Good for multiplayer interactions. |
| **Geospatial Support** | **Mature:** GDAL, Proj4, GeoArrow ecosystem. | **Nascent:** Basic geometric types, no complex projection support. |
| **Network Overhead** | **Low:** Sends compressed vector field once. | **High:** If simulating on server. Medium if sending raw table data. |
| **Best Use Case** | Scientific Visualization, High-Fidelity Dashboards. | MMORPGs, Chat, Lobbies, Inventory Systems. |

**Key Insight:** SpacetimeDB excels at **Consistency** (ensuring all players see the *exact same* state at the same time), whereas the OpenGEOS stack excels at **Throughput** and **Visual Fidelity** (rendering massive datasets smoothly). For visualization, where "good enough" synchronization is acceptable but dropped frames are not, the OpenGEOS stack is superior.

## ---

**6\. Implementation Workflow: The "Storm Watch" Dashboard**

This section provides a narrative walkthrough of implementing the system to visualize a hypothetical storm moving across the UK and Ireland.

### **6.1 Phase 1: Ingestion and Normalization (DuckDB & Ibis)**

The workflow begins with DuckDB. Using the spatial extension, we mount the S3 buckets containing the Met Office UKV model and the Met Éireann HARMONIE model.  
We write an Ibis script to define the "virtual table." This script standardizes the column names (mapping u-component-of-wind to u) and performs a coordinate transformation on the Irish data, projecting it from ITM to WGS84 to match the UK data. Crucially, this step does not download the data yet; it simply defines the compute graph.

### **6.2 Phase 2: State Definition (PlanetScale)**

A user connects to the dashboard. PlanetScale retrieves their profile. The user selects "Storm Ciara \- Feb 2020." PlanetScale stores this state: view\_center: \[53.5, \-4.0\], zoom: 6, timestamp: 2020-02-09T12:00:00Z.  
Through the pg\_duckdb extension, PlanetScale can query the metadata table in MotherDuck to confirm that data for this timestamp is available and "warm" (cached).

### **6.3 Phase 3: The Reactive Loop (Marimo & GeoArrow)**

The user launches the **Marimo** notebook.

1. **Slider Interaction:** The user drags the time slider.  
2. **Reactive Trigger:** Marimo detects the variable change. It triggers the Ibis/DuckDB query.  
3. **Execution:** DuckDB executes the query. It reads the relevant "chunks" of the GRIB2/GeoParquet files for that specific hour.  
4. **Zero-Copy Transfer:** DuckDB outputs a **GeoArrow Table**. This binary object contains the U and V vectors for the viewport.  
5. **Data-to-Texture:** A Python helper converts this grid into a PNG or binary texture.

### **6.4 Phase 4: The Render (Lonboard & WebGPU)**

The texture is passed to the **AnyWidget** running in the browser.

1. The custom WindLayer (Deck.gl) receives the new texture.  
2. The **WebGPU Compute Shader** updates. It instantly applies the new wind vectors to the 100,000 particles currently swirling on the screen.  
3. **Result:** The user sees the wind patterns shift instantly as the storm moves across the Irish Sea. The particles accelerate where the gradient is steep (high wind speed) and spiral into low-pressure centers.

## ---

**7\. Strategic Recommendations and Future Outlook**

The convergence of cloud-native data formats and browser-based GPU compute has rendered the traditional "GIS Server" architecture obsolete for high-performance visualization. The **OpenGEOS/DuckDB/Lonboard** stack represents the optimal path for creating game-like meteorological visualizations.

### **7.1 Recommendations**

1. **Adopt GeoParquet:** Convert incoming GRIB2 data to GeoParquet immediately. While DuckDB *can* read GRIB2, Parquet is orders of magnitude faster for repeated querying and supports better compression.  
2. **Use SpacetimeDB for Collaboration, Not Simulation:** If the dashboard requires multiplayer features (e.g., users drawing annotation lines on the map that others must see instantly), use SpacetimeDB to handle *that specific layer*. Do not attempt to pipe the massive wind field data through it.  
3. **Leverage WebGPU:** Monitor the maturity of WebGPU in Deck.gl (v9.0+). Migrating from WebGL2 to WebGPU will allow for even more complex simulations, such as particles interacting with 3D terrain (mountains) or changing color based on real-time temperature probing.

### **7.2 Conclusion**

By decoupling the **Analytical Plane** (DuckDB/MotherDuck) from the **Transactional Plane** (PlanetScale) and the **Visual Plane** (Lonboard/WebGPU), this architecture achieves the best of all worlds: the query speed of an OLAP engine, the reliability of an ACID database, and the visual fidelity of a modern video game. This is the future of geospatial intelligence.

#### **Works cited**

1. Preface \- Introduction to GIS Programming \- Qiusheng Wu, accessed December 18, 2025, [https://gispro.gishub.org/book/preface.html](https://gispro.gishub.org/book/preface.html)  
2. Qiusheng Wu giswqs \- GitHub, accessed December 18, 2025, [https://github.com/giswqs](https://github.com/giswqs)  
3. Performance Guide \- DuckDB, accessed December 18, 2025, [https://duckdb.org/docs/stable/guides/performance/overview](https://duckdb.org/docs/stable/guides/performance/overview)  
4. How to use DuckDB's ST\_Read function to read and convert zipped shapefiles \- Flother, accessed December 18, 2025, [https://www.flother.is/til/duckdb-st-read/](https://www.flother.is/til/duckdb-st-read/)  
5. MotherDuck Integrates with PlanetScale Postgres \- MotherDuck Blog, accessed December 18, 2025, [https://motherduck.com/blog/motherduck-planetscale-integration/](https://motherduck.com/blog/motherduck-planetscale-integration/)  
6. DuckDB and MotherDuck support for PlanetScale Postgres, accessed December 18, 2025, [https://planetscale.com/changelog/postgres-extension-pg-duckdb-motherduck](https://planetscale.com/changelog/postgres-extension-pg-duckdb-motherduck)  
7. Using MotherDuck with PlanetScale, accessed December 18, 2025, [https://planetscale.com/blog/using-motherduck-with-planetscale](https://planetscale.com/blog/using-motherduck-with-planetscale)  
8. Integration with Ibis \- DuckDB, accessed December 18, 2025, [https://duckdb.org/docs/stable/guides/python/ibis](https://duckdb.org/docs/stable/guides/python/ibis)  
9. Ibis \+ DuckDB geospatial: a match made on Earth :: SciPy 2024 :: pretalx, accessed December 18, 2025, [https://cfp.scipy.org/2024/talk/PSR9BP/](https://cfp.scipy.org/2024/talk/PSR9BP/)  
10. Lonboard \- Overture Maps Documentation, accessed December 18, 2025, [https://docs.overturemaps.org/examples/lonboard/](https://docs.overturemaps.org/examples/lonboard/)  
11. What's New in Lonboard | Kyle Barron, accessed December 18, 2025, [https://kylebarron.dev/blog/new-in-lonboard/](https://kylebarron.dev/blog/new-in-lonboard/)  
12. How it works? \- lonboard \- Development Seed, accessed December 18, 2025, [https://developmentseed.org/lonboard/latest/how-it-works/](https://developmentseed.org/lonboard/latest/how-it-works/)  
13. Mixing code with widgets \- Marimo, accessed December 18, 2025, [https://marimo.io/features/feat-widgets](https://marimo.io/features/feat-widgets)  
14. Build plugins with anywidget\! \- Marimo, accessed December 18, 2025, [https://marimo.io/blog/anywidget](https://marimo.io/blog/anywidget)  
15. Met Office Weather DataHub \- Met Office, accessed December 18, 2025, [https://www.metoffice.gov.uk/services/data/met-office-weather-datahub](https://www.metoffice.gov.uk/services/data/met-office-weather-datahub)  
16. Meteorological improvements. \- Met Éireann, accessed December 18, 2025, [https://opendata2.met.ie/opendata2/docs/NWP\_explained.odt](https://opendata2.met.ie/opendata2/docs/NWP_explained.odt)  
17. AnyWidget \- marimo, accessed December 18, 2025, [https://docs.marimo.io/api/inputs/anywidget/](https://docs.marimo.io/api/inputs/anywidget/)  
18. Overview | SpacetimeDB docs, accessed December 18, 2025, [https://spacetimedb.com/docs/](https://spacetimedb.com/docs/)  
19. SpacetimeDB, accessed December 18, 2025, [https://spacetimedb.com/](https://spacetimedb.com/)  
20. SpacetimeDB \- Hacker News, accessed December 18, 2025, [https://news.ycombinator.com/item?id=43631822](https://news.ycombinator.com/item?id=43631822)  
21. SpacetimeDB: A new database written in Rust that replaces your server entirely \- Reddit, accessed December 18, 2025, [https://www.reddit.com/r/programming/comments/15mgp4i/spacetimedb\_a\_new\_database\_written\_in\_rust\_that/](https://www.reddit.com/r/programming/comments/15mgp4i/spacetimedb_a_new_database_written_in_rust_that/)