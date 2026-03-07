# **Convergence of Spatial Analytics and Digital Folkloristics: A Technical and Theoretical Examination of *Hidden Heritages* and *Canúint.ie***

## **1\. Introduction: The Spatial Turn in Digital Heritage**

The digitization of cultural heritage has transitioned from a phase of static preservation—scanning manuscripts and archiving audio—to a dynamic era of computational interrogation and geospatial visualization. Within the specific context of the Gaelic-speaking world, this shift is exemplified by two vanguard projects: **Hidden Heritages** (Díchódú Oidhreachtaí Folaithe) and **Canúint.ie** (Taisce Chanúintí na Gaeilge). These initiatives do not merely present archives on a digital shelf; they explode the archive into a spatial dimension, allowing for the visualization of intangible cultural assets—narrative motifs and dialect phonemes—across the physical landscapes of Ireland and Scotland.  
This report provides an exhaustive technical analysis of the geospatial strategies employed by these projects, situated within the broader infrastructure of the **Gaois** research group at Dublin City University (DCU). It further explores the potential for next-generation analytics using **DuckDB** and its spatial extension, proposing a high-performance architectural paradigm for querying the massive, complex datasets inherent to digital folkloristics.

### **1.1 The Epistemology of the Digital Map**

In the realm of *Hidden Heritages* and *Canúint.ie*, the map functions as more than a navigational aid; it is an epistemological tool. It fundamentally alters the user's relationship with the data. Traditional archival access is hierarchical and textual: one selects a collection, then a volume, then a page. The geospatial interface, however, is non-linear and immediate. It asserts that the *location* of a story's telling is as significant as the *content* of the story itself.1  
For *Hidden Heritages*, the map visualizes the "phylogeography" of folklore—tracing how a tale type like *ATU 425* ('The Search for the Lost Husband') mutates as it migrates across the Irish Sea, effectively treating narrative elements as biological traits subject to evolutionary pressures.3 For *Canúint.ie*, the map serves as a linguistic atlas, grounding ephemeral speech acts in specific townlands, thereby rendering the invisible boundaries of dialect (isoglosses) visible.2

### **1.2 The Gaois Technical Ecosystem**

The shared lineage of these projects within the Gaois research group provides a unified technical baseline. Gaois has established itself as a premier developer of linguistic infrastructure, leveraging a stack that historically favors **Microsoft technologies (ASP.NET, SQL Server)** while increasingly integrating open-source geospatial libraries.5  
The analysis of their public repositories reveals a modular architecture:

* **Backend:** Robust APIs built on ASP.NET Core (Gaois.Localizer, Gaois.QueryLogger) that serve structured JSON/XML data.7  
* **Data Persistence:** SQL Server databases managing complex relational schemas of terminology, toponymy, and bibliography.5  
* **Frontend:** A heavy reliance on JavaScript mapping libraries—specifically **Leaflet** and **OpenLayers**—to render geospatial data delivered via these APIs.8

This report dissects these components, contrasting the lightweight, mobile-first approach likely employed by *Canúint.ie* with the heavy-duty, vector-rich requirements of *Hidden Heritages*, before demonstrating how **DuckDB** can serve as a powerful analytical engine to bridge these domains.

## ---

**2\. Theoretical Framework: Digital Folkloristics and Spatial Data**

To understand the technical requirements of these projects, one must first appreciate the complexity of the data they handle. "Digital Folkloristics" is not simply the storage of folklore; it is the computational analysis of tradition.

### **2.1 The Challenge of "Deep Mapping"**

Deep mapping refers to the layering of diverse data types—text, audio, image, and metadata—onto a single geographic locus.

* **Temporal Depth:** A single coordinate (e.g., a hearth in Dunquin, Kerry) may be associated with stories collected in 1930, 1945, and 1970\. The spatial database must handle this temporal dimension effectively, allowing users to filter the map by time.1  
* **Typological Depth:** The data is not unstructured. It is rigorously classified according to the **Aarne-Thompson-Uther (ATU)** index. *Hidden Heritages* specifically focuses on complex "Wonder Tales" (Märchen), such as *ATU 400* ('The Search for the Lost Wife') and *ATU 503* ('The Gifts of the Little People').3

The geospatial data architecture must therefore support **one-to-many relationships** (one location, many tales) and **hierarchical filtering** (showing all tales of type *ATU 4xx*).

### **2.2 Phylogenetics and Spatial Diffusion**

The *Hidden Heritages* project introduces a novel computational method: **Phylogenetics**. Originating in evolutionary biology, this approach builds "trees" of story versions to determine their ancestral relationships.

* **Spatial Implication:** When these phylogenetic trees are projected onto a map, they reveal the *routes* of cultural transmission. If Version A (Donegal) is computationally determined to be the "parent" of Version B (Hebrides), the map draws a vector of transmission.  
* **Data Requirement:** This requires the geospatial system to handle not just *points* (locations of tales) but *vectors/edges* (relationships between tales), necessitating a graph-based data structure overlaying the geographic layer.3

### **2.3 Acoustic Geographies and Dialectology**

*Canúint.ie* deals with **acoustic geography**. The primary data object is the **phoneme** or **lexical item** as realized in a specific location.

* **The Isogloss:** In linguistics, an isogloss is a line on a map marking the boundary between two linguistic features (e.g., where the pronunciation of a vowel changes).  
* **Spatial Granularity:** Unlike folktales, which might be attributed to a general parish, dialect data often requires precise *townland* specificity to accurately map these boundaries. The system must support high-precision geocoding and the ability to render "heat maps" or cluster visualizations to show the density of specific dialect features.2

## ---

**3\. Case Study I: Hidden Heritages (Díchódú Oidhreachtaí Folaithe)**

### **3.1 Project Scope and Architecture**

*Hidden Heritages* represents a massive undertaking in "text mining" and "data curation." It serves as a bridge between the **National Folklore Collection (NFC)** in Dublin and the **School of Scottish Studies Archives (SSSA)** in Edinburgh.  
**Data Scale:**

* **Corpus:** \~80,000 manuscript pages.  
* **Collections:** The Main Manuscript Collection (NFC) and the Tale Archive (SSSA).3  
* **Geographic Scope:** The entire Gaeltacht regions of Ireland and the Gàidhealtachd of Scotland.

### **3.2 The "Manuscripts to Models" Pipeline**

The geospatial visualization is the final output of a rigorous processing pipeline known as "From Manuscripts to Models."

1. **Digitization:** High-resolution scanning of physical volumes.  
2. **Handwritten Text Recognition (HTR):** The project utilizes **Transkribus**, an AI-powered HTR platform. This is critical because the source material is often in non-standard scripts (Gaelic script) or cursive English.  
   * **Performance:** The project reports training models on \~500 pages of manuscript to achieve a Character Error Rate (CER) of \~4.39%. This high accuracy is essential for the subsequent NLP steps.14  
3. **Text Mining & Entity Extraction:** Once the text is digital, NLP algorithms (likely Python-based, given the gaoisalign and transformers references 5) extract metadata:  
   * **Toponyms:** Placenames are identified and resolved against authority databases (*Logainm.ie*).  
   * **Motifs:** Key narrative elements are identified to classify the tale.  
4. **Geospatial Indexing:** The extracted toponyms are converted to coordinates (Latitude/Longitude) and stored in the project's database, linked to the text segment.

### **3.3 Geospatial Visualization Strategy**

The project's frontend likely employs a "Dual-View" interface.

* **The Map View:** Users see the distribution of tales. A filter for "Giants" (*Fuamhairean*) or "Fairies" (*Na Daoine Beaga*) updates the map markers in real-time.1  
* **The Graph View:** Users see the phylogenetic tree of a tale type.  
* **Integration:** Clicking a node in the tree highlights the corresponding point on the map. This requires a tight coupling between the graph data structure (nodes/edges) and the spatial data structure (points).

**Technical Assumption:** Given the complexity of visualizing phylogenetic networks alongside maps, the project likely utilizes **D3.js** for the graph visualizations and overlays them on **OpenLayers** or **Leaflet** maps. The robust API capabilities of the Gaois stack would serve the node-edge-coordinate data as a single JSON payload.11

## ---

**4\. Case Study II: Canúint.ie (Taisce Chanúintí na Gaeilge)**

### **4.1 Project Scope and Archival Integration**

*Canúint.ie* is a "Repository of Irish Dialects" focusing on the audio heritage of the language. It represents a partnership between the academic rigor of DCU and the archival wealth of **RTÉ** (Raidió Teilifís Éireann).2  
**Data Characteristics:**

* **Source:** Historic radio recordings, often from the mid-20th century.  
* **Content:** Interviews, storytelling, and conversation capturing natural speech.  
* **Metadata:** Rich biographical data on speakers (age, gender, occupation) which is crucial for sociolinguistic analysis.2

### **4.2 Geospatial Implementation: Mapping the Intangible**

For *Canúint.ie*, the geospatial challenge is indexing. Audio files do not have inherent coordinates. The spatial data is **derived** from the speaker's biography.

#### **4.2.1 The "Speaker-Location" Bond**

The system likely links each audio asset to a **Person Entity** in the database. This Person Entity is linked to a **Place Entity** (Townland/Parish).

* **Database Schema Implication:**  
  * Table: AudioAssets \-\> FK: SpeakerID  
  * Table: Speakers \-\> FK: PlaceID  
  * Table: Places \-\> Columns: Lat, Long, Geometry (Polygon)  
  * *Result:* The map query joins these three tables to plot the audio recording.15

#### **4.2.2 User Experience (UX) and Discovery**

The map on *Canúint.ie* is the primary search engine.

* **Clustering:** Given the high density of recordings in specific Gaeltacht areas (e.g., West Kerry, Connemara), the map must use **clustering algorithms**. At low zoom levels, users see a circle with a number (e.g., "50" recordings in Kerry). As they zoom in, the cluster breaks apart into individual markers.8  
* **Polygon Overlays:** The interface likely displays the official **Gaeltacht boundaries** as polygon layers, allowing users to visually distinguish between the "official" Irish-speaking regions and the historical distribution of the dialects.16

## ---

**5\. Comparative Technical Analysis: The Gaois Geospatial Stack**

Both projects are underpinned by the Gaois research group's technical infrastructure. Analyzing their GitHub repositories and documentation reveals a distinct preference for specific technologies that enable these rich spatial experiences.

### **5.1 The Mapping Engine: Leaflet vs. OpenLayers**

A critical architectural decision in any geospatial project is the choice of client-side library. The Gaois ecosystem appears to utilize both, depending on the project's density and complexity.17

#### **5.1.1 Leaflet (The Likely Choice for Canúint.ie)**

* **Profile:** Lightweight (\~40KB), mobile-optimized, plugin-based.  
* **Why for Canúint.ie?** The primary interaction on *Canúint.ie* is "point-and-play." Users need to find a marker and listen to audio. Leaflet excels at handling marker clusters and simple popups without the overhead of a full GIS engine. It allows for a snappy, responsive experience on mobile devices, which is crucial for public engagement projects.19  
* **Data Handling:** Leaflet consumes GeoJSON natively. The API likely serves a lightweight GeoJSON feed of recording locations:  
  JSON  
  { "type": "Feature", "geometry": { "type": "Point", "coordinates": \[...\] }, "properties": { "url": "audio.mp3" } }

#### **5.1.2 OpenLayers (The Likely Choice for Hidden Heritages)**

* **Profile:** Heavyweight, feature-rich, supports complex vector projections and OGC standards (WMS/WFS).  
* **Why for Hidden Heritages?** This project involves "phylogenetic maps" and potential overlays of historical boundaries that might not match modern projections. OpenLayers offers robust support for:  
  * **Vector Tiling:** Rendering thousands of tale locations efficiently.  
  * **Projections:** Handling historical map layers (e.g., Cassini 6-inch maps) that require on-the-fly reprojection to align with modern satellite imagery.  
  * **Complex Interactions:** Drawing vectors between points to visualize the "movement" of a story.8

### **5.2 Backend Architecture: The API Layer**

The "Gaois" GitHub repositories (Gaois.QueryLogger, Gaois.Localizer) indicate a.NET-centric backend.5

* **Framework:** **ASP.NET Core**. This provides a high-performance, cross-platform server environment.  
* **Database:** **SQL Server**. Microsoft's RDBMS supports GEOGRAPHY and GEOMETRY data types natively, allowing for spatial indexing (QuadTree/Grid) at the database level.  
* **API Design:** The projects likely expose RESTful endpoints.  
  * GET /api/tales?bbox=...: Returns tales within the current map viewport.  
  * GET /api/dialects/{id}/geojson: Returns the specific geometry for a dialect region.

### **5.3 Metadata Standards**

Both projects rely on rich metadata.

* **Hidden Heritages:** Uses the **ATU Index** (Aarne-Thompson-Uther) as a controlled vocabulary for narrative content. This allows for precise filtering (e.g., "Show me all *ATU 425* tales").3  
* **Canúint.ie:** Likely adheres to **Dublin Core** or similar archival standards for describing digital assets, ensuring interoperability with the broader European digital library ecosystem (Europeana).7

## ---

**6\. Advanced Analytics: The DuckDB Spatial Paradigm**

While the web interfaces of *Hidden Heritages* and *Canúint.ie* provide excellent *access*, they are not designed for deep, heavy-duty computational analysis. A researcher wishing to query the entire dataset—cross-referencing 80,000 pages of text with geospatial boundaries and temporal filters—would face significant latency using standard web APIs.  
This is where **DuckDB** enters the architecture. DuckDB is an in-process SQL OLAP (Online Analytical Processing) database that can query massive datasets with extreme speed, without the overhead of a server like PostgreSQL/PostGIS.

### **6.1 Why DuckDB for Digital Folkloristics?**

1. **Columnar Storage:** Unlike row-based databases (PostgreSQL), DuckDB stores data by column. For a query like "Calculate the average year of collection for all tales in Donegal," DuckDB only reads the Year and County columns, ignoring the massive Text column. This results in orders-of-magnitude faster analytics.21  
2. **Vectorized Execution:** DuckDB processes data in batches (vectors) rather than row-by-row, leveraging modern CPU architectures (SIMD instructions) for speed.21  
3. **The spatial Extension:** This extension adds geospatial capabilities comparable to PostGIS but optimized for local, file-based analysis. It supports the OGC Simple Features standard (POINT, POLYGON, etc.).23

### **6.2 Technical Sample: Analyzing Folklore Distribution**

The following section provides a detailed technical walkthrough of how a researcher could use DuckDB to analyze the *Hidden Heritages* dataset.  
**Scenario:** We want to ingest a raw GeoJSON dump of folklore sites, extract complex nested properties (metadata), and perform a spatial join to find which tales fall within specific Gaeltacht boundaries.

#### **6.2.1 Installation and Loading**

First, the researcher must initialize the DuckDB environment and load the necessary extensions. The spatial extension is not autoloaded and must be explicitly loaded.25

SQL

\-- Install and Load the Spatial Extension  
INSTALL spatial;  
LOAD spatial;

\-- Install and Load the JSON Extension (critical for GeoJSON properties)  
INSTALL json;  
LOAD json;

#### **6.2.2 Ingesting GeoJSON with ST\_Read**

The ST\_Read function is the gateway. It utilizes the **GDAL** library under the hood to parse geospatial formats. However, GeoJSON often contains nested objects in the properties field that standard tabular import might flatten or ignore.  
**The Naive Approach:**

SQL

CREATE TABLE raw\_sites AS SELECT \* FROM ST\_Read('folklore\_sites.geojson');

*Critique:* This works for simple files but often fails to capture deep metadata structures common in digital humanities (e.g., a list of motifs inside a properties object).  
The Robust Approach (JSON Parsing):  
A better method involves reading the file as raw JSON and explicitly parsing the structure. This allows the researcher to control exactly how the metadata is mapped to columns.27

SQL

\-- Create a structured table from the raw GeoJSON  
CREATE TABLE folklore\_analysis AS  
SELECT  
    \-- 1\. Extract Geometry  
    \-- We extract the geometry object and convert it to DuckDB's internal GEOMETRY type  
    ST\_GeomFromGeoJSON(json\_extract(feature, '$.geometry')) AS geom,

    \-- 2\. Extract Top-Level Properties  
    json\_extract\_string(feature, '$.properties.title') AS title,  
    json\_extract\_string(feature, '$.properties.collector\_id') AS collector\_id,

    \-- 3\. Extract Nested Metadata (The "Attached Data")  
    \-- Example: Extracting the ATU Tale Type from a nested metadata object  
    json\_extract\_string(feature, '$.properties.metadata.atu\_type') AS atu\_type,  
      
    \-- Example: Extracting the Informant's Gender for demographic analysis  
    json\_extract\_string(feature, '$.properties.metadata.informant.gender') AS gender,

    \-- Example: Extracting the recording year  
    CAST(json\_extract\_string(feature, '$.properties.date.year') AS INTEGER) AS year

FROM   
    \-- Read the GeoJSON file as a list of JSON objects (newline delimited or array)  
    read\_json\_auto('folklore\_sites.geojson', format='auto') AS data(feature)  
WHERE   
    \-- Ensure we only process valid spatial features  
    json\_extract(feature, '$.geometry') IS NOT NULL;

**Analysis of the Code:**

* ST\_GeomFromGeoJSON: This function parses the JSON geometry fragment (e.g., {"type": "Point", "coordinates": \[...\]}) into DuckDB's binary GEOMETRY format. This binary format is optimized for spatial operations.29  
* json\_extract\_string: Digital heritage data is notoriously "messy." By extracting specific paths, we sanitize the input before analysis.

#### **6.2.3 Spatial Joins: Point-in-Polygon Analysis**

Once the data is in DuckDB, we can perform spatial joins. A common question in *Canúint.ie* research might be: "Which recordings fall within the official 1956 Gaeltacht boundaries?"  
The Join Operation:  
DuckDB uses an R-Tree index (or similar bounding volume hierarchy) to optimize spatial joins. It first checks if the bounding box of the point intersects the bounding box of the polygon (fast), and only then performs the precise geometry check (slow).16

SQL

\-- Step 1: Load the Gaeltacht Boundaries (Polygons)  
CREATE TABLE gaeltacht\_boundaries AS   
SELECT \* FROM ST\_Read('gaeltacht\_1956.shp');

\-- Step 2: Perform the Spatial Join  
SELECT   
    g.district\_name AS gaeltacht\_district,  
    f.atu\_type,  
    COUNT(\*) AS tale\_count  
FROM   
    folklore\_analysis f  
JOIN   
    gaeltacht\_boundaries g  
ON   
    \-- The Spatial Predicate: Is the point INSIDE the polygon?  
    ST\_Intersects(f.geom, g.geom)  
WHERE   
    f.year BETWEEN 1930 AND 1940  
GROUP BY   
    g.district\_name, f.atu\_type  
ORDER BY   
    tale\_count DESC;

Performance Implications:  
In a traditional Python script using shapely, this join would happen in a loop and could take minutes for 80,000 points. In DuckDB, thanks to vectorized execution and spatial indexing, this query typically runs in sub-second timeframes.21

#### **6.2.4 Exporting Results**

Finally, the researcher can export the analyzed subset back to GeoJSON for visualization on the *Hidden Heritages* web map.

SQL

\-- Export filtered dataset to GeoJSON  
COPY (  
    SELECT   
        title,   
        atu\_type,   
        \-- Convert the binary geometry back to GeoJSON text  
        ST\_AsGeoJSON(geom) AS geometry   
    FROM folklore\_analysis   
    WHERE gaeltacht\_district \= 'Conamara'  
) TO 'conamara\_tales.geojson'  
WITH (FORMAT GDAL, DRIVER 'GeoJSON');

This COPY... TO command utilizes the GDAL driver to write a perfectly formatted GeoJSON file, ready for consumption by Leaflet or OpenLayers.30

## ---

**7\. Future Directions: The AI and Vector Horizon**

The convergence of technologies seen in *Hidden Heritages* and *Canúint.ie* points toward a future where the map is the primary interface for all digital humanities.

### **7.1 From Tiles to Vectors**

As the datasets grow—potentially mapping every word in the 80,000-page corpus—the current strategy of loading GeoJSON files into the browser will hit memory limits. The next logical step for Gaois is the adoption of **Vector Tiles (MVT)**.

* **Mechanism:** Instead of sending the whole dataset, the server sends only the vector data visible in the current viewport, sliced into tiles.  
* **DuckDB Role:** DuckDB can generate MVTs dynamically (ST\_AsMVT), allowing for massive datasets to be browsed seamlessly without heavy frontend loading.

### **7.2 Semantic Search and LLMs**

The "Hidden Heritages" project's use of NLP suggests a future where users can query the map semantically. Instead of searching for "ATU 425", a user could ask, "Show me where stories about transformation into animals are told." Large Language Models (LLMs) integrated with the spatial database could translate this natural language query into the SQL/Spatial query demonstrated above.

## **8\. Conclusion**

*Hidden Heritages* and *Canúint.ie* demonstrate that the preservation of folklore is no longer a static endeavor. By coupling the rich, qualitative data of the **National Folklore Collection** and **RTÉ Archives** with rigorous geospatial engineering, these projects reveal the hidden structures of culture. They show that stories and dialects are not just abstract concepts; they are grounded in the physical landscape, moving and evolving across the hills and coastlines of Ireland and Scotland.  
The technical architecture underpinning this—a sophisticated blend of **Transkribus** for ingestion, **Gaois's ASP.NET** stack for delivery, and **Leaflet/OpenLayers** for visualization—provides a robust platform for discovery. Furthermore, the integration of advanced analytical tools like **DuckDB Spatial** empowers researchers to move beyond simple viewing to deep, computational interrogation of the archive. This synthesis of tradition and technology ensures that these "hidden heritages" are not only decoded but dynamically revitalized for the digital age.

### ---

**Data Summary Tables**

**Table 1: Comparative Geospatial Architecture**

| Feature | Hidden Heritages (Díchódú Oidhreachtaí Folaithe) | Canúint.ie (Taisce Chanúintí na Gaeilge) |
| :---- | :---- | :---- |
| **Primary Data Object** | **Text / Narrative** (Folktale) | **Audio / Speech** (Dialect Recording) |
| **Spatial Nature** | **Phylogeographic:** Tracks movement/evolution of tales. | **Dialectological:** Indexes speech to specific loci. |
| **Data Volume** | \~80,000 manuscript pages (Text Mining). | Thousands of hours of audio (RTÉ Archive). |
| **Mapping Library** | **OpenLayers** (Likely) \- For complex vector/graph overlays. | **Leaflet** (Likely) \- For responsive clustering/playback. |
| **Key Metadata** | ATU Tale Type, Motif, Narrator, Collector. | Speaker Bio, Dialect Region, Recording Date. |
| **Key Technologies** | Transkribus (HTR), NLP, D3.js (Graphs). | Audio Streaming, Clustering Algorithms. |

**Table 2: DuckDB Spatial Capabilities for Digital Heritage**

| Function Category | Function Name | Usage in Digital Folkloristics | Implementation |
| :---- | :---- | :---- | :---- |
| **Ingestion** | ST\_Read | Loading GeoJSON/Shapefiles of sites and boundaries. | GDAL Wrapper |
| **Conversion** | ST\_GeomFromGeoJSON | Parsing raw API responses into binary geometry. | Native (DuckDB) |
| **Predicates** | ST\_Intersects | Determining if a tale point is inside a Gaeltacht polygon. | GEOS Library |
| **Analysis** | ST\_Buffer | Creating a "catchment area" around a collector's home. | GEOS Library |
| **Export** | COPY... TO | Generating filtered GeoJSON for web maps. | GDAL Wrapper |

#### **Works cited**

1. Gaelic Algorithmic Research Group – Rannsachadh digiteach air a' Ghàidhlig \- Blogs \- The University of Edinburgh, accessed December 7, 2025, [https://blogs.ed.ac.uk/garg/](https://blogs.ed.ac.uk/garg/)  
2. Seoladh Canúint.ie | Gaois research group, accessed December 7, 2025, [https://www.gaois.ie/en/blog/seoladh-canuint-ie](https://www.gaois.ie/en/blog/seoladh-canuint-ie)  
3. Decoding Hidden Heritages in Gaelic Traditional Narrative with Text-Mining and Phylogenetics, accessed December 7, 2025, [https://www.hiddenheritages.ai/en/about/dhh](https://www.hiddenheritages.ai/en/about/dhh)  
4. Taisce Chanúintí na Gaeilge, accessed December 7, 2025, [https://www.canuint.ie/ga/](https://www.canuint.ie/ga/)  
5. gaois repositories \- GitHub, accessed December 7, 2025, [https://github.com/orgs/gaois/repositories](https://github.com/orgs/gaois/repositories)  
6. Gaois \- GitHub, accessed December 7, 2025, [https://github.com/gaois](https://github.com/gaois)  
7. Gaois.Localizer | docs.gaois.ie, accessed December 7, 2025, [https://docs.gaois.ie/en/software/localizer](https://docs.gaois.ie/en/software/localizer)  
8. Leaflet vs OpenLayers: Pros and Cons of Both Libraries | Geoapify, accessed December 7, 2025, [https://www.geoapify.com/leaflet-vs-openlayers/](https://www.geoapify.com/leaflet-vs-openlayers/)  
9. A digital language | Dublin City University \- DCU, accessed December 7, 2025, [https://www.dcu.ie/blog/2056/digital-language](https://www.dcu.ie/blog/2056/digital-language)  
10. Two new collections digitsed and available on dúchas.ie: Acetate disc recordings and photographs \- Gaois, accessed December 7, 2025, [https://www.gaois.ie/en/blog/abhar-fuaime-agus-grianghraif-digitithe](https://www.gaois.ie/en/blog/abhar-fuaime-agus-grianghraif-digitithe)  
11. Decoding Hidden Heritages in Gaelic Traditional Narrative with Text-Mining and Phylogenetics | Gaois research group, accessed December 7, 2025, [https://www.gaois.ie/en/about/decoding-hidden-heritages](https://www.gaois.ie/en/about/decoding-hidden-heritages)  
12. wlamb – Gaelic Algorithmic Research Group \- Blogs, accessed December 7, 2025, [https://blogs.ed.ac.uk/garg/author/wlamb/](https://blogs.ed.ac.uk/garg/author/wlamb/)  
13. The version I know: phylogenetic analysis for the Decoding Hidden Heritages project \- Gaois, accessed December 7, 2025, [https://www.gaois.ie/en/blog/dhh-the-version-i-know](https://www.gaois.ie/en/blog/dhh-the-version-i-know)  
14. Handwritten Text Recognition (HTR) for Irish-Language Folklore \- LREC, accessed December 7, 2025, [http://www.lrec-conf.org/proceedings/lrec2022/workshops/CLTW4/pdf/2022.cltw4-1.17.pdf](http://www.lrec-conf.org/proceedings/lrec2022/workshops/CLTW4/pdf/2022.cltw4-1.17.pdf)  
15. Dúchas Application Programming Interface (Version 0.6) \- Gaois Documentation, accessed December 7, 2025, [https://docs.gaois.ie/en/data/duchas/v0.6/api](https://docs.gaois.ie/en/data/duchas/v0.6/api)  
16. Spatial Joins in DuckDB, accessed December 7, 2025, [https://duckdb.org/2025/08/08/spatial-joins](https://duckdb.org/2025/08/08/spatial-joins)  
17. land cover datasets: Topics by Science.gov, accessed December 7, 2025, [https://www.science.gov/topicpages/l/land+cover+datasets](https://www.science.gov/topicpages/l/land+cover+datasets)  
18. Logainmneacha Dhobhair agus Stair Áitiúil le Pádraig Mac Gairbheith \- Meitheal Logainm.ie, accessed December 7, 2025, [https://meitheal.logainm.ie/pdf/meitheal.logainm.ie-logainmneacha-dhobhair-agus-stair-aitiuil.pdf](https://meitheal.logainm.ie/pdf/meitheal.logainm.ie-logainmneacha-dhobhair-agus-stair-aitiuil.pdf)  
19. Leaflet \- a JavaScript library for interactive maps, accessed December 7, 2025, [https://leafletjs.com/](https://leafletjs.com/)  
20. Choosing OpenLayers or Leaflet? \[closed\] \- GIS Stack Exchange, accessed December 7, 2025, [https://gis.stackexchange.com/questions/33918/choosing-openlayers-or-leaflet](https://gis.stackexchange.com/questions/33918/choosing-openlayers-or-leaflet)  
21. A Beginner's Guide to Geospatial with DuckDB Spatial and MotherDuck, accessed December 7, 2025, [https://motherduck.com/blog/geospatial-for-beginner-duckdb-spatial-motherduck/](https://motherduck.com/blog/geospatial-for-beginner-duckdb-spatial-motherduck/)  
22. How to use DuckDB's ST\_Read function to read and convert zipped shapefiles \- Flother, accessed December 7, 2025, [https://www.flother.is/til/duckdb-st-read/](https://www.flother.is/til/duckdb-st-read/)  
23. Spatial Extension – DuckDB, accessed December 7, 2025, [https://aidoczh.com/duckdb/docs/archive/0.9/extensions/spatial.html](https://aidoczh.com/duckdb/docs/archive/0.9/extensions/spatial.html)  
24. Spatial Extension – DuckDB, accessed December 7, 2025, [https://duckdb.org/docs/stable/core\_extensions/spatial/overview](https://duckdb.org/docs/stable/core_extensions/spatial/overview)  
25. Extensions \- DuckDB, accessed December 7, 2025, [https://duckdb.org/docs/stable/extensions/overview](https://duckdb.org/docs/stable/extensions/overview)  
26. Spatial Extension – DuckDB \- AiDocZh, accessed December 7, 2025, [https://www.aidoczh.com/duckdb/docs/archive/1.0/extensions/spatial.html](https://www.aidoczh.com/duckdb/docs/archive/1.0/extensions/spatial.html)  
27. DuckDB constructing a full GeoJSON feature collection \- Stack Overflow, accessed December 7, 2025, [https://stackoverflow.com/questions/78832305/duckdb-constructing-a-full-geojson-feature-collection](https://stackoverflow.com/questions/78832305/duckdb-constructing-a-full-geojson-feature-collection)  
28. JSON Processing Functions \- DuckDB, accessed December 7, 2025, [https://duckdb.org/docs/stable/data/json/json\_functions](https://duckdb.org/docs/stable/data/json/json_functions)  
29. Spatial Functions \- DuckDB, accessed December 7, 2025, [https://duckdb.org/docs/stable/core\_extensions/spatial/functions](https://duckdb.org/docs/stable/core_extensions/spatial/functions)  
30. GDAL Integration \- DuckDB, accessed December 7, 2025, [https://duckdb.org/docs/stable/core\_extensions/spatial/gdal](https://duckdb.org/docs/stable/core_extensions/spatial/gdal)