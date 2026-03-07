# **The British Isles Demographic Atlas: A Comprehensive Technical and Statistical Report**

## **Executive Summary**

The digital representation of demographic reality requires a convergence of rigorous sociological analysis and advanced software engineering. This report presents an exhaustive examination of the educational and linguistic landscape of the British Isles, synthesized from the disparate census cycles of 2021 (England, Wales, Northern Ireland, Crown Dependencies) and 2022 (Scotland). It further articulates a robust architectural framework for visualizing this high-dimensional data using the nascent "Modern Data Stack": DuckDB for serverless geospatial processing, Convex for component-based backend architecture, and TanStack Start for server-side rendered (SSR) application delivery.  
The demographic data reveals a region characterized by profound asymmetry. In England and Wales, the 2021 Census documented a seismic shift in migration-driven linguistics, with Romanian displacing Polish as the fastest-growing main language, rising from negligible numbers in 2011 to 472,000 speakers in 2021\. Concurrently, the Celtic fringe presents a divergent narrative: a state-supported revitalization of Welsh contrasts sharply with the critical endangerment of Guernésiais in the Channel Islands and the distinct "electronic census" methodologies adopted by Guernsey to track its population.2 Educational attainment mirrors these fractures, with London boroughs achieving tertiary education rates nearly triple those of post-industrial towns in the Midlands and the distinct Portuguese labor demographic in Jersey showing marked educational variances.4  
To operationalize this data, we reject legacy GIS server architectures in favor of a "Data-Lake-as-Database" pattern. By encapsulating DuckDB’s spatial engine within authored Convex Components, we demonstrate how to query massive GeoParquet datasets via Node.js actions, utilizing Hilbert Curve indexing to achieve sub-second latency for choropleth rendering. This backend is coupled with TanStack Start, leveraging Server Functions to perform heavy lifting before hydration, ensuring that the complex socio-political reality of the British Isles is rendered with the fidelity it demands.

## ---

**Chapter 1: The Sociolinguistic and Educational Fabric of the British Isles**

The British Isles, comprising the sovereign United Kingdom and the self-governing Crown Dependencies, represents a complex tapestry of "subnations," each with distinct data collection methodologies, linguistic heritages, and educational frameworks. The 2021/2022 census cycle serves as the primary instrument for dissecting these layers.

### **1.1 England and Wales: The Post-Brexit Demographic Baseline**

The 2021 Census for England and Wales was the first "digital-first" census, achieving a 97% response rate. It captured a snapshot of a society where the monolingual norm is increasingly punctuated by hyper-diverse urban clusters and specific rural migration corridors.

#### **1.1.1 The Romanian Linguistic Surge**

The most statistically significant finding in the linguistic domain is the ascendancy of the Romanian language. In the intercensal period between 2011 and 2021, the number of usual residents listing Romanian as their *main language* surged from approximately 68,000 to 472,000, representing 0.8% of the total population.  
This 600% increase eclipses the growth of all other languages and fundamentally alters the "second language" map of England. While Polish remains the most common non-English language (1.1%, 612,000 speakers), its growth has plateaued and, in some regions, declined, reflecting the maturity of the post-2004 accession migration and the changing labor market dynamics following the Brexit referendum.  
The spatial distribution of these languages is non-uniform. Polish speakers are heavily integrated into towns associated with food processing and agriculture (e.g., Boston in Lincolnshire) as well as urban centers. Romanian speakers show a similar but more accelerated dispersal pattern, with high concentrations in outer London boroughs (Harrow, Redbridge) and specific logistics hubs in the Midlands.  
\*\*Table 1.1: Primary Non-English Main Languages in England and Wales (2011–2021 Comparison) \*\*

| Rank (2021) | Language | Speakers (2021) | % of Pop | 2011 Count | Trend Analysis |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | Polish | 612,000 | 1.1% | 546,000 | Stabilized/Plateaued |
| 2 | Romanian | 472,000 | 0.8% | 68,000 | **Explosive Growth** |
| 3 | Panjabi | 291,000 | 0.5% | 273,000 | Stable/Generational Shift |
| 4 | Urdu | 270,000 | 0.5% | 269,000 | Stable |
| 5 | Portuguese | 225,000\* | 0.4% | 133,000 | Moderate Growth |
| 6 | Spanish | 215,000\* | 0.4% | 120,000 | Moderate Growth |

*Note: The distinction between "Main Language" and "Proficiency" is critical. The 2021 Census reveals that of the 4.1 million people (7.1%) who do not speak English as their main language, the vast majority retain high proficiency. Only 1.5% of the total population cannot speak English "well," and a mere 0.3% cannot speak it at all. This data counters narratives of linguistic isolation, suggesting instead a pattern of functional bilingualism.*

#### **1.1.2 Educational Polarization: The London Decoupling**

The educational statistics from 2021 illuminate a stark geographic divide in human capital, often described as the "London Vortex." The capital draws graduates from across the archipelago, creating a concentration of high-level qualifications that distorts national averages.  
Nationally, 33.8% of residents aged 16 and over possess Level 4 qualifications (Degree level or equivalent) or above.4 However, granular analysis at the Local Authority District (LAD) level reveals extreme variance.

* **The Hyper-Educated Core**: In the City of London, 74.2% of residents hold Level 4+ qualifications. In the borough of Wandsworth, the figure is 62.6%. These areas represent some of the highest concentrations of tertiary education in Europe.4  
* **The Post-Industrial Periphery**: Conversely, the borough of Sandwell in the West Midlands records the highest proportion of residents with *no formal qualifications* (28.9%), followed closely by Boston (27.6%) and Leicester (26.7%).4

The correlation between these educational metrics and the linguistic data is multifaceted. Boston, for instance, holds the dual distinction of having the highest percentage of non-UK born residents in a rural context (driven by Eastern European labor) and one of the lowest educational attainment profiles. This indicates that the migrant labor force in these areas is either recruited for manual roles that do not require Level 4 qualifications, or possesses qualifications that are not recognized within the UK National Qualification Framework (NQF), leading to statistical under-reporting of their true human capital.

### **1.2 Scotland: The 2022 Census and the Celtic Revival**

Scotland's decision to delay its census to 2022 due to the pandemic resulted in a dataset that is temporally distinct from the rest of the UK. This census cycle placed unprecedented emphasis on Scotland's indigenous languages: Scottish Gaelic and Scots.

#### **1.2.1 Scottish Gaelic: Stability through Policy**

The narrative of Gaelic decline has been arrested, if not fully reversed, by aggressive educational intervention (Gaelic Medium Education \- GME).

* **Headlines**: 57,375 people (roughly 1.1% of the population) reported the ability to speak Gaelic. While this is a numerical decrease from roughly 59,000 in 2001, the broader metric of "any skill" (understanding, reading, or speaking) rose to 2.5%.6  
* **The Heartland vs. The City**: Na h-Eileanan Siar (Western Isles) remains the linguistic fortress, with 52.3% of the population able to speak the language. It is the only council area with a Gaelic majority.7 However, the demographic profile of speakers is shifting. Glasgow City now houses the largest absolute number of speakers outside the Highlands (8,972), driven by urbanization and the prestige of the Glasgow Gaelic School (*Sgoil Ghàidhlig Ghlaschu*).8  
* **The Youth Bulge**: A critical indicator of revitalization is the increase in speakers aged 3–15, which rose by 11,200 between 2011 and 2022\.6 This confirms that the language is being acquired in the classroom rather than the home, a shift that alters the sociolinguistic nature of the language from a vernacular to a learned identity marker.

#### **1.2.2 The Scots Language: Dialect or Language?**

The 2022 Census validated Scots as a distinct linguistic entity, separate from English.

* **Prevalence**: Over 1.5 million people (30%+) reported speaking Scots, with an additional 267,000 able to understand it.7  
* **Regional Variation**: The highest proportions are found in the Shetland Islands and Aberdeenshire. In the North East, the "Doric" dialect of Scots acts as a strong regional identity marker, with 40-50% of the population identifying as speakers.7

### **1.3 Ireland: The Gaeltacht Paradox**

While the Republic of Ireland is a sovereign state, its demographic data is essential for an all-island analysis of the British Isles archipelago. The 2022 Census of Ireland provides a sobering look at the gap between symbolic language status and functional usage.

* **The Illusion of Mass Fluency**: On paper, 1.87 million people (40% of the population) can speak Irish. This figure is bolstered by the mandatory teaching of Irish in the school system.9  
* **The Reality of Usage**: The true metric of vitality is daily usage *outside* the education system. In 2022, only 71,968 people reported speaking Irish daily in a vernacular context.10  
* **Gaeltacht Decline**: The designated Irish-speaking regions (Gaeltacht) are experiencing linguistic erosion. While the population in these areas is growing, the percentage of daily speakers fell by 2% since 2016\. Only 66% of residents in the Gaeltacht can speak Irish, and far fewer use it as their primary community language.9

Educational attainment in Ireland is exceptionally high, with 45% of the population aged 15+ holding a third-level qualification, peaking at 65% in the affluent county of Dún Laoghaire-Rathdown.9

## ---

**Chapter 2: The Crown Dependencies: Micro-States and Data Innovation**

The Crown Dependencies—Jersey, Guernsey, and the Isle of Man—are not part of the UK, nor the EU. They are self-governing possessions of the Crown. Their small size allows for rapid innovation in data collection (e.g., electronic censuses) but also renders their indigenous cultures uniquely vulnerable to demographic swamping.

### **2.1 Guernsey: The Electronic Census and Linguistic Erosion**

Guernsey has abandoned the traditional decennial census model in favor of a "Rolling Electronic Census."

* **Methodology**: By aggregating administrative records from Social Security, Income Tax, and Education departments, the States of Guernsey publishes annual population reports. The March 2023 report cites a population of 64,091. This allows for real-time tracking of migration flows, a capability the UK lacks.  
* **Guernésiais (Guernsey French)**: The linguistic picture is dire. Guernésiais is classified as "Severely Endangered." The last comprehensive survey (2001) found only 1,327 fluent speakers (2% of the population), mostly aged over 65\. The breakdown of intergenerational transmission during the German Occupation (1940-1945), when children were evacuated to England, proved a fatal blow from which the language has not recovered.11 Unlike Welsh or Irish, there is no robust immersion schooling system to generate new speakers.

### **2.2 Jersey: The Portuguese Connection**

Jersey's demographic profile is uniquely shaped by a specific migration treaty with Portugal (Madeira).

* **Demographics**: 8% of Jersey’s residents were born in Portugal or Madeira.5  
* **Educational Stratification**: The 2021 Jersey Census revealed a profound educational gap. While 42% of the general population holds higher-level qualifications (supporting the island's massive offshore finance sector), 54% of Portuguese-born adults possess *no formal qualifications*.12 This reflects a bifurcated economy: a high-skill finance sector staffed by locals and British expats, and a service/agricultural sector staffed by Portuguese migrants.

### **2.3 Isle of Man: The Manx Revival**

The Isle of Man offers a counter-narrative to Guernsey. After the death of the last traditional native speaker, Ned Maddrell, in 1974, the language was declared extinct. However, a grassroots revival movement has successfully reintroduced Manx. The 2021 Census (though less detailed in snippets) continues to track a growing cohort of second-language speakers who have learned Manx through the *Bunscoill Ghaelgagh* (Manx-medium primary school).

## ---

**Chapter 3: Theoretical Framework of Geospatial Data at Scale**

To visualize this complex web of languages and education levels across the British Isles, we face a significant engineering challenge. The administrative geography of the UK is complex, comprising over 33,000 Lower Layer Super Output Areas (LSOAs) and thousands of Council Wards.

### **3.1 The Failure of Legacy GIS on the Web**

The traditional approach to web mapping involves:

1. Storing geometries in a PostGIS database.  
2. Running a middleware server (GeoServer/MapServer) to render these geometries into PNG images (Raster Tiles) or PBF (Vector Tiles).  
3. Displaying them in a client like Leaflet.

This architecture is heavy, expensive to maintain, and suffers from latency. Furthermore, loading raw GeoJSON files for the entire UK into a browser is infeasible. A high-resolution GeoJSON of UK Wards can exceed 500MB, causing the browser's main thread to freeze during parsing and rendering.

### **3.2 The Modern Data Stack: Data-Lake-as-Database**

The proposed solution utilizes **DuckDB** and **GeoParquet**.

* **GeoParquet**: This is an extension of the Apache Parquet format. It stores geospatial data in a columnar format. Unlike JSON (row-oriented), Parquet allows the database to read only the specific columns needed (e.g., "Language\_Romanian") without parsing the entire file. It supports heavy compression (Snappy/ZSTD), often reducing file sizes by 10x compared to GeoJSON.  
* **Vectorized Execution**: DuckDB processes data in batches (vectors) rather than row-by-row, leveraging modern CPU SIMD instructions. This allows it to aggregate millions of census records in milliseconds.

### **3.3 Hilbert Curve Spatial Indexing**

A critical optimization for querying geospatial data from flat files (like Parquet on S3) is spatial locality. Standard file storage is linear. If we store UK wards alphabetically, a ward in "Aberdeen" (North) might be adjacent to "Adur" (South). A map viewport showing "Scotland" would have to seek randomly through the entire file.  
The Solution: We sort the data using a Hilbert Curve.  
The Hilbert Curve is a continuous fractal space-filling curve. It maps multi-dimensional space (2D Latitude/Longitude) onto a one-dimensional line (the file) while preserving locality. Points that are close in 2D space are generally close on the curve.  
By ordering the GeoParquet file by the Hilbert value of the geometry centroids, we ensure that all Scottish wards are stored in a contiguous block of bytes. DuckDB's spatial extension supports this optimization, allowing it to download only the byte-ranges relevant to the user's viewport.13

## ---

**Chapter 4: The Database Engine: DuckDB and Spatial SQL**

This chapter details the technical implementation of the data layer. We will use DuckDB to ingest raw ONS Shapefiles and Census CSVs, join them, and export optimized GeoParquet.

### **4.1 Data Ingestion and Transformation Pipeline**

The British Isles data comes from multiple sources: ONS (England/Wales), NRS (Scotland), and local island governments. These must be harmonized.  
Step 1: Installing the Spatial Extension  
DuckDB requires the spatial extension to handle geometries.

SQL

INSTALL spatial;  
LOAD spatial;

Step 2: Ingesting Shapefiles  
We use ST\_Read to load the administrative boundaries. We explicitly select the "Ultra Generalised" (500m) boundaries for the high-level view to reduce vertex count.14

SQL

CREATE TABLE boundaries AS   
SELECT \* FROM ST\_Read('LAD\_Dec\_2021\_GB\_BGC.shp');

Step 3: Joining Census Data  
We join the geometric table with the statistical CSVs on the ONS Area Code (e.g., E09000003).

SQL

CREATE TABLE atlas\_data AS   
SELECT   
    b.geom,  
    b.LAD21CD as code,  
    b.LAD21NM as name,  
    c.romanian\_speakers,  
    c.level\_4\_quals\_percent,  
    c.no\_quals\_percent  
FROM boundaries b  
JOIN read\_csv\_auto('census\_2021\_education.csv') c   
ON b.LAD21CD \= c.area\_code;

### **4.2 Optimizing with Hilbert Curves**

As identified in the research 13, sorting by Hilbert curve is essential for performance. We calculate the Hilbert value based on the geometry's extent within the bounding box of the British Isles.

SQL

\-- Define the Bounding Box for the British Isles: approx \-10, 49 to 2, 61  
CREATE TABLE atlas\_optimized AS   
SELECT \*   
FROM atlas\_data   
ORDER BY ST\_Hilbert(geom, ST\_MakeEnvelope(-10, 49, 2, 61));

\-- Export to GeoParquet with Metadata  
COPY atlas\_optimized TO 'british\_isles\_atlas.parquet'   
(FORMAT 'parquet', COMPRESSION 'ZSTD', KV\_METADATA {'geometry\_column': 'geom'});

### **4.3 Runtime Strategy: Node.js vs. WASM**

For the application architecture, we must decide where DuckDB runs.

* **WASM (Client)**: duckdb-wasm can run in the browser. It is excellent for offline-first capabilities but requires downloading the WASM bundle (\~20MB) and potentially large data chunks.  
* **Node.js (Server \- Convex Actions)**: Running DuckDB on the server allows for caching, access to higher memory limits, and faster start times.

**Decision**: We will utilize **Convex Actions with the Node.js runtime**. This allows us to use the native duckdb Node.js bindings (which are faster than WASM) and leverage the server's bandwidth to fetch from S3, returning lightweight GeoJSON to the client.

## ---

**Chapter 5: Component-Driven Architecture with Convex**

The user request explicitly asks for **Authoring Convex Components**. Convex Components are a powerful pattern for modularizing backend logic. We will author a component named british-isles-census that encapsulates the data fetching logic, isolating it from the main application.

### **5.1 Component Philosophy and Structure**

A Convex Component acts as a "black box" backend. It has its own schema, functions, and storage. The main application "installs" the component and interacts with it via a defined API.  
Directory Structure:  
/packages/british-isles-census/  
├── convex.config.ts // Component definition  
├── package.json  
├── src/  
│ ├── component/  
│ │ ├── schema.ts // Internal component schema  
│ │ ├── api.ts // Public API export  
│ │ ├── actions/  
│ │ │ └── query.ts // Node.js action for DuckDB  
│ │ └── \_generated/

### **5.2 Configuring the Component (convex.config.ts)**

This file tells Convex how to build the component. Crucially, we must configure it to allow the duckdb native module, which requires the Node.js runtime.

TypeScript

// packages/british-isles-census/convex.config.ts  
import { defineComponent } from "convex/server";

export default defineComponent({  
  name: "british\_isles\_census",  
  dependencies: {  
    node: {  
      // Explicitly whitelist the native duckdb package for bundling  
      externalPackages: \["duckdb"\],  
    },  
  },  
});

*Insight*: As noted in 15, externalPackages prevents esbuild from trying (and failing) to bundle binary dependencies, forcing them to be resolved at runtime in the Node.js environment.

### **5.3 Defining the Internal Schema (schema.ts)**

The component needs to know *where* the GeoParquet files are stored (e.g., S3 URLs) and metadata about the subnations.

TypeScript

// packages/british-isles-census/src/component/schema.ts  
import { defineSchema, defineTable } from "convex/server";  
import { v } from "convex/values";

export default defineSchema({  
  // Metadata about available census datasets  
  datasets: defineTable({  
    subnation: v.string(), // "ENG", "SCO", "WLS", "NI", "JSY", "GGY", "IOM"  
    year: v.number(),  
    category: v.string(), // "LANGUAGE", "EDUCATION"  
    s3\_url: v.string(),   // URL to the Hilbert-sorted Parquet file  
    bbox: v.array(v.number()), //  
  }).index("by\_subnation", \["subnation"\]),  
});

### **5.4 Implementing the DuckDB Node Action**

Standard Convex functions (queries/mutations) run in a lightweight V8 environment that *does not* support raw TCP/IP or native modules like DuckDB. We must use a **Convex Action** with the "use node" directive.16

TypeScript

// packages/british-isles-census/src/component/actions/query.ts  
"use node"; // Critical directive to enable Node.js runtime

import { action } from "../\_generated/server";  
import { v } from "convex/values";  
import { Database } from "duckdb";

// Helper to wrap DuckDB callback in Promise  
const runSQL \= (db: Database, sql: string): Promise\<any\> \=\> {  
  return new Promise((resolve, reject) \=\> {  
    db.all(sql, (err, rows) \=\> {  
      if (err) reject(err);  
      else resolve(rows);  
    });  
  });  
};

export const queryParquet \= action({  
  args: {  
    fileUrl: v.string(),  
    bounds: v.object({  
      minX: v.number(), minY: v.number(),  
      maxX: v.number(), maxY: v.number()  
    })  
  },  
  handler: async (ctx, args) \=\> {  
    // Initialize in-memory DuckDB  
    const db \= new Database(":memory:");  
      
    // We construct a SQL query that uses the Parquet file as a table.  
    // We strictly filter by Bounding Box to leverage the Hilbert Index.  
    // ST\_AsGeoJSON converts the binary geometry to web-friendly JSON.  
    const query \= \`  
      SELECT   
        code,   
        name,   
        romanian\_speakers,   
        level\_4\_quals\_percent,  
        ST\_AsGeoJSON(geom) as geometry  
      FROM '${args.fileUrl}'  
      WHERE   
        min\_x \>= ${args.bounds.minX} AND   
        max\_x \<= ${args.bounds.maxX} AND  
        min\_y \>= ${args.bounds.minY} AND  
        max\_y \<= ${args.bounds.maxY}  
      LIMIT 1000  
    \`;

    try {  
      const result \= await runSQL(db, query);  
      return result;  
    } catch (error) {  
      console.error("DuckDB Error:", error);  
      throw new Error("Failed to query census data");  
    }  
  },  
});

### **5.5 Exposing the Public API (api.ts)**

The main application cannot call queryParquet directly if it's internal. We expose a clean API.

TypeScript

// packages/british-isles-census/src/component/api.ts  
import { action } from "./\_generated/server";  
import { internal } from "./\_generated/api";  
import { v } from "convex/values";

export const getSubnationData \= action({  
  args: {   
    subnation: v.string(),  
    viewport: v.array(v.number()) //  
  },  
  handler: async (ctx, args) \=\> {  
    // 1\. Look up the file URL from the internal schema  
    // Note: We need a query to read the schema. Actions can run queries.  
    const dataset \= await ctx.runQuery(internal.queries.getDataset, {  
      subnation: args.subnation  
    });

    if (\!dataset) throw new Error("Dataset not found");

    // 2\. Call the node action to process the file  
    return await ctx.runAction(internal.actions.query.queryParquet, {  
      fileUrl: dataset.s3\_url,  
      bounds: {  
        minX: args.viewport, minY: args.viewport,  
        maxX: args.viewport, maxY: args.viewport  
      }  
    });  
  }  
});

## ---

**Chapter 6: Frontend Engineering with TanStack Start**

The final layer involves delivering this data to the user. **TanStack Start** provides a full-stack React framework with Server-Side Rendering (SSR). This is crucial for performance: we want to fetch the census statistics on the server *before* sending HTML to the client, ensuring good SEO and faster First Contentful Paint.

### **6.1 The "Selective SSR" Pattern for Maps**

A common pitfall in geospatial web apps is Hydration Mismatch. Map libraries (Leaflet, MapLibre GL JS) rely on the window object and DOM access, which do not exist on the server. If we try to SSR a map component, the server crashes.  
TanStack Start solves this with the ClientOnly component.17 This utility defers the rendering of its children until the JavaScript has hydrated on the client.

TypeScript

// app/routes/map.tsx  
import { ClientOnly } from '@tanstack/react-router';  
import { CensusMap } from '../components/CensusMap'; // Heavy map component

export function MapRoute() {  
  return (  
    \<div className="map-container"\>  
      {/\* Render a skeleton on server, Map on client \*/}  
      \<ClientOnly fallback={\<div className="skeleton"\>Loading Atlas...\</div\>}\>  
        {() \=\> \<CensusMap /\>}  
      \</ClientOnly\>  
    \</div\>  
  );  
}

### **6.2 Server Functions: The Data Bridge**

We use TanStack Start's createServerFn to bridge the gap between the React frontend and the Convex backend. This function runs on the server (Node/Bun), authenticates with Convex, and calls our component's action.

TypeScript

// app/utils/census.ts  
import { createServerFn } from '@tanstack/react-start';  
import { ConvexHttpClient } from 'convex/browser';  
import { api } from '../../convex/\_generated/api';

// Define the Server Function  
export const fetchCensusData \= createServerFn({ method: 'GET' })  
 .validator((params: { region: string; bbox: number }) \=\> params)  
 .handler(async ({ region, bbox }) \=\> {  
    // Initialize Convex Client (Server-side)  
    const client \= new ConvexHttpClient(process.env.CONVEX\_URL\!);  
      
    // Call the component's public API  
    // Note: 'census' is the name we gave the component in app's convex.config.ts  
    const data \= await client.action(api.census.getSubnationData, {   
      subnation: region,  
      viewport: bbox  
    });

    return data;  
  });

### **6.3 Route Loaders and Data Streaming**

We integrate the server function into a Route Loader. This ensures the data is fetched in parallel with the route loading.

TypeScript

// app/routes/dashboard.tsx  
import { createFileRoute } from '@tanstack/react-router';  
import { fetchCensusData } from '../utils/census';

export const Route \= createFileRoute('/dashboard/$region')({  
  // The loader runs on the server (during SSR) and client (during navigation)  
  loader: async ({ params }) \=\> {  
    // Default bbox for the region  
    const defaultBbox \= \[-5, 50, 2, 56\];   
    return await fetchCensusData({ region: params.region, bbox: defaultBbox });  
  },  
  component: Dashboard  
});

function Dashboard() {  
  const censusData \= Route.useLoaderData();  
    
  return (  
    \<div className="dashboard-grid"\>  
      \<div className="stats-panel"\>  
        \<h2\>Romanian Speakers: {censusData.romanian\_count}\</h2\>  
        \<h2\>Degree Holders: {censusData.level\_4\_percent}%\</h2\>  
      \</div\>  
      \<div className="map-panel"\>  
        {/\* Map visualization code \*/}  
      \</div\>  
    \</div\>  
  );  
}

### **6.4 Visualization Strategy: Choropleth Rendering**

Once the GeoJSON arrives on the client, we use **MapLibre GL JS** for rendering. Unlike Leaflet (which uses SVG/DOM elements), MapLibre uses WebGL. This allows it to handle the thousands of polygon features returned by our DuckDB query without dropping frames.

* **Data-Driven Styling**: We map the romanian\_speakers property to a color ramp (e.g., Viridis or Magma).  
* **Interactivity**: Hover events query the rendered features instantly on the GPU.

## ---

**Conclusion**

The construction of the **British Isles Demographic Atlas** is a multidisciplinary feat. Sociologically, it exposes the fracturing of a once-monolithic linguistic block: England is diversifying through specific migrant corridors (Romanian/Polish), Wales is successfully institutionalizing bilingualism, while the Crown Dependencies of Guernsey and Jersey struggle with the erasure of their indigenous Norman heritage and the educational stratification of their migrant labor forces.  
Technologically, this report demonstrates that the era of heavy GIS servers is ending. The combination of **DuckDB's** columnar spatial processing, **Convex's** componentized backend architecture, and **TanStack Start's** server-driven frontend allows for the creation of applications that are both statistically rigorous and highly performant. By leveraging Hilbert Curves for spatial indexing and Node.js-based Action runtimes for data processing, we can serve complex, high-resolution census data to the web with minimal latency, providing policymakers and researchers with the tools they need to understand a society in flux.  
This architecture not only solves the immediate problem of visualizing British Isles census data but provides a scalable blueprint for any data-intensive geospatial application in the modern web ecosystem.

#### **Works cited**

1. About Guernésiais \- Guernsey Language Commission, accessed December 13, 2025, [https://language.gg/About\_Guernesiais](https://language.gg/About_Guernesiais)  
2. Guernsey Annual Electronic Census Report, accessed December 13, 2025, [https://gov.gg/CHttpHandler.ashx?id=174892\&p=0](https://gov.gg/CHttpHandler.ashx?id=174892&p=0)  
3. Education, England and Wales: Census 2021 \- Office for National Statistics, accessed December 13, 2025, [https://www.ons.gov.uk/peoplepopulationandcommunity/educationandchildcare/bulletins/educationenglandandwales/census2021](https://www.ons.gov.uk/peoplepopulationandcommunity/educationandchildcare/bulletins/educationenglandandwales/census2021)  
4. Report on the 2021 Jersey Census. \- States Assembly, accessed December 13, 2025, [https://statesassembly.je/publications/assembly-reports/2023/r-45-2023](https://statesassembly.je/publications/assembly-reports/2023/r-45-2023)  
5. Gaelic and Scots in Scotland: What does the census tell us? \- SPICe Spotlight, accessed December 13, 2025, [https://spice-spotlight.scot/2024/08/12/gaelic-and-scots-in-scotland-what-does-the-census-tell-us/](https://spice-spotlight.scot/2024/08/12/gaelic-and-scots-in-scotland-what-does-the-census-tell-us/)  
6. Languages | Scotland's Census, accessed December 13, 2025, [https://www.scotlandscensus.gov.uk/census-results/at-a-glance/languages/](https://www.scotlandscensus.gov.uk/census-results/at-a-glance/languages/)  
7. List of Scottish council areas by number of Scottish Gaelic speakers \- Wikipedia, accessed December 13, 2025, [https://en.wikipedia.org/wiki/List\_of\_Scottish\_council\_areas\_by\_number\_of\_Scottish\_Gaelic\_speakers](https://en.wikipedia.org/wiki/List_of_Scottish_council_areas_by_number_of_Scottish_Gaelic_speakers)  
8. Census 2022 Profile 8 \- The Irish Language and Education \- CSO, accessed December 13, 2025, [https://www.cso.ie/en/releasesandpublications/ep/p-cpp8/census2022profile8-theirishlanguageandeducation/keyfindings/](https://www.cso.ie/en/releasesandpublications/ep/p-cpp8/census2022profile8-theirishlanguageandeducation/keyfindings/)  
9. Irish language \- Wikipedia, accessed December 13, 2025, [https://en.wikipedia.org/wiki/Irish\_language](https://en.wikipedia.org/wiki/Irish_language)  
10. Guernésiais \- Wikipedia, accessed December 13, 2025, [https://en.wikipedia.org/wiki/Guern%C3%A9siais](https://en.wikipedia.org/wiki/Guern%C3%A9siais)  
11. Education: Census 2021 | Statistics Jersey, accessed December 13, 2025, [https://stats.je/statistic/education-census-2021/](https://stats.je/statistic/education-census-2021/)  
12. Using DuckDB's Hilbert Function with GeoParquet | Cloud-Native Geospatial Forum \- CNG, accessed December 13, 2025, [https://cloudnativegeo.org/blog/2025/01/using-duckdbs-hilbert-function-with-geoparquet/](https://cloudnativegeo.org/blog/2025/01/using-duckdbs-hilbert-function-with-geoparquet/)  
13. Countries (December 2021\) Boundaries UK BUC \- Data.gov.uk, accessed December 13, 2025, [https://www.data.gov.uk/dataset/2e17269d-10b9-4e43-b67b-57f9b02bd0f8/countries-december-2021-boundaries-uk-buc](https://www.data.gov.uk/dataset/2e17269d-10b9-4e43-b67b-57f9b02bd0f8/countries-december-2021-boundaries-uk-buc)  
14. Bundling | Convex Developer Hub, accessed December 13, 2025, [https://docs.convex.dev/functions/bundling](https://docs.convex.dev/functions/bundling)  
15. Actions | Convex Developer Hub, accessed December 13, 2025, [https://docs.convex.dev/functions/actions](https://docs.convex.dev/functions/actions)  
16. ClientOnly Component | TanStack Router React Docs, accessed December 13, 2025, [https://tanstack.com/router/v1/docs/framework/react/api/router/clientOnlyComponent](https://tanstack.com/router/v1/docs/framework/react/api/router/clientOnlyComponent)