# **Advanced Architectures for Visualizing Temporal and Semantic Knowledge Graphs: A Deep Dive into Cognee, Graphiti, and Modern Frontend Integration**

## **Executive Summary**

The rapid evolution of Artificial Intelligence from static query-response models to persistent, autonomous agents has necessitated a fundamental architectural shift in memory systems. The prevailing Retrieval-Augmented Generation (RAG) paradigm, which treats knowledge as a flat collection of vectorized text chunks, is proving insufficient for complex reasoning tasks that require global sensemaking, multi-hop traversals, and temporal awareness.1 In response, the industry is moving toward GraphRAG—a synthesis of graph databases and Large Language Models (LLMs)—where "memory" is structured as a rich, interconnected graph of entities, relationships, and events.  
This report provides an exhaustive, expert-level analysis of the visualization challenges and solutions associated with two leading frameworks in this domain: **Cognee** and **Graphiti**. These frameworks represent distinct philosophies in AI memory: Cognee focuses on the deterministic construction of ontology-aligned knowledge graphs suitable for static enterprise data 3, while Graphiti pioneers an "episodic" and temporally-aware architecture designed for dynamic agentic workflows.5  
The opacity of high-dimensional graph structures poses a significant barrier to adoption and debugging. Without effective visualization, the semantic connections formed by Cognee or the temporal evolutions tracked by Graphiti remain black boxes. This document bridges the gap between backend graph construction and frontend rendering. It details the precise methodologies for extracting data from these Python-based frameworks and rendering it within a modern React stack using libraries such as react-force-graph, Cytoscape.js, and specialized temporal UI components. We further analyze the critical role of **BAML (Boundary AI Markup Language)** in ensuring the structural integrity of the data upstream, thereby enabling high-fidelity visualizations downstream.7  
The following analysis is structured to guide systems architects and frontend engineers through the complete lifecycle of a GraphRAG visualization pipeline: from ingestion and schema enforcement to API serialization and high-performance WebGL rendering.

## **1\. The Epistemological Shift: From Vector Indices to Observable Graph Memory**

To effectively visualize AI memory, one must first understand the structural transformation occurring in the underlying data layer. The visualization requirements for a GraphRAG system are fundamentally different from those of traditional data dashboards or vector similarity visualizations.

### **1.1 The Limitations of Vector-Based Memory**

Traditional RAG architectures rely on vector similarity search, where documents are chunked, embedded, and retrieved based on cosine similarity to a user query. While effective for simple fact retrieval, this approach suffers from "contextual fragmentation".2 A vector index has no inherent concept of structure or relationship; it sees the world as a bag of disconnected points in high-dimensional space. Consequently, visualizing a vector index usually involves dimensionality reduction techniques like t-SNE or UMAP, which produce abstract scatter plots that are unintelligible to non-experts and lack semantic explainability.  
In contrast, knowledge graphs allow for "global sensemaking." They explicitly model the relationships between entities (e.g., "Person A" *worked\_at* "Company B" *during* "Timeframe C"). Visualizing this structure requires node-link diagrams that can reveal paths, clusters, and hierarchies. The shift to GraphRAG is driven by the need for agents to perform multi-hop reasoning, such as connecting a news article about a merger to a user's stock portfolio preferences.1 For the visualization engineer, this means the data source is no longer a flat list of scores but a complex topology of nodes (entities) and edges (relationships), often enriched with temporal metadata.

### **1.2 The Divergent Philosophies of Cognee and Graphiti**

Our deep research highlights a critical divergence in how Cognee and Graphiti approach graph construction, which dictates the visualization strategy.  
**Cognee** creates what can be termed a "Semantic Snapshot." It ingests unstructured data and uses LLMs to deterministically map it to a graph structure, often guided by a pre-defined ontology (using Pydantic models).3 Its primary goal is to organize data into a coherent, queryable structure that mimics a mental map. Visualizing Cognee is akin to visualizing a static map of knowledge; the focus is on the *types* of entities (Ontology) and the *structure* of their connections. The visualization answers questions like "How is the codebase structured?" or "What are the relationships between these medical concepts?".9  
**Graphiti**, developed by Zep AI, creates a "Temporal Narrative." It treats memory as a stream of **Episodes**—discrete events like chat messages, emails, or system logs. It employs a bi-temporal data model that tracks not just *what* happened, but *when* it was valid and *when* the system learned it.5 Visualizing Graphiti requires a dynamic approach. A static snapshot is insufficient because the graph's state changes over time. The visualization must answer questions like "What did the agent know about the user's preferences last week versus today?" or "How has the relationship between these two entities evolved?" This necessitates a frontend capable of "time travel" via scrubbing mechanisms.11

### **1.3 The Visualization Gap**

Both frameworks provide powerful backend capabilities but leave the frontend integration largely to the developer. Cognee offers a basic visualize\_graph utility that generates static HTML files using pyvis, useful for debugging but inadequate for production applications.12 Graphiti provides search APIs but relies on the developer to construct the visual representation of the returned subgraphs.14 This report aims to fill this gap, providing the "glue" code and architectural patterns to build professional-grade visualizations.

## ---

**2\. Cognee: Architecture, Data Extraction, and Structural Visualization**

Cognee acts as a middleware that transforms unstructured data into a structured knowledge graph. Understanding its internal pipeline is essential for extracting the data required for visualization.

### **2.1 The Cognee Ingestion and Processing Pipeline**

Cognee's workflow is pipeline-driven. The user adds data, which is then "cognified."

1. **Ingestion (.add)**: Data points (documents, code files) are loaded into the system.4  
2. **Cognification (.cognify)**: This is the core processing step. Cognee chunks the data, generates embeddings, and uses an LLM to extract entities and relationships based on the graph ontology.3  
3. **Storage**: The resulting structure is stored in a graph database. Cognee supports **NetworkX** for local, in-memory storage and **FalkorDB** or **Neo4j** for production persistence.15

The choice of storage backend has immediate implications for data extraction. When running locally with the NetworkX adapter, the graph exists as a Python object in memory, which is highly accessible for manipulation and export. In production modes (Neo4j/FalkorDB), the graph resides in an external database, requiring Cypher queries or adapter methods to retrieve the topology.

### **2.2 Analyzing Cognee's Built-in Visualization**

Cognee includes a method await cognee.visualize\_graph(output\_path).12 Code analysis reveals that this function typically leverages the **PyVis** library.13 PyVis is a Python wrapper around the vis.js JavaScript library. It works by:

1. taking the internal NetworkX graph,  
2. converting it to a JSON-like structure compatible with vis.js,  
3. embedding this JSON into an HTML template containing the vis.js library code, and  
4. writing the result to a file.

While this provides an immediate visual result (e.g., graph\_visualization.html 16), it creates a "detached" artifact. The visualization runs in a separate browser tab, disconnected from the main application's React state. It cannot react to user clicks in the main app, nor can it drive navigation within the app. It is a debugging tool, not a UI component.

### **2.3 Architecting a Custom React Integration**

To integrate Cognee visualizations into a React application, we must bypass visualize\_graph and expose the raw graph data via an API.

#### **2.3.1 Leveraging the NetworkX Adapter for JSON Export**

For local development and smaller graphs, Cognee's use of NetworkX is a significant advantage. NetworkX provides robust serialization tools. The networkx.readwrite.json\_graph module contains node\_link\_data, which converts a graph into a dictionary perfectly formatted for frontend libraries like D3.js or react-force-graph.17  
Backend Implementation Pattern (FastAPI/Python):  
The goal is to create an endpoint that returns the current state of the knowledge graph.

Python

import cognee  
import networkx as nx  
from networkx.readwrite import json\_graph  
from fastapi import FastAPI

app \= FastAPI()

@app.get("/api/knowledge-graph")  
async def get\_graph\_data():  
    \# 1\. Access the underlying graph client  
    \# Cognee's architecture abstracts this, so we access the adapter  
    from cognee.infrastructure.databases.graph import get\_graph\_client  
    client \= get\_graph\_client()  
      
    \# 2\. Extract the NetworkX object  
    \# If using the NetworkX adapter, the 'graph' attribute is the DiGraph object  
    if hasattr(client, 'graph'):  
        G \= client.graph  
    else:  
        \# If using Neo4j/FalkorDB, we might need to construct a subgraph  
        \# or use a specific export function provided by the adapter  
        \# This is a simplified fallback for the NetworkX adapter scenario  
        G \= nx.DiGraph()   
      
    \# 3\. Serialize to Node-Link JSON  
    \# This format is standard: {'nodes': \[...\], 'links': \[...\]}  
    data \= json\_graph.node\_link\_data(G)  
      
    return data

This API endpoint acts as the bridge. The frontend can now fetch('/api/knowledge-graph') and receive a clean JSON object containing all entities and relationships.17

#### **2.3.2 Handling Production Backends (Neo4j/FalkorDB)**

When Cognee is configured with Neo4j or FalkorDB 15, we cannot simply access a .graph property. Instead, we must query the database to retrieve the nodes and edges.

* **Cypher Query for Full Export**: MATCH (n)-\[r\]-\>(m) RETURN n, r, m  
* **Data Transformation**: The raw database result must be mapped to the node-link format.  
  * **Nodes**: Extract id, labels (type), and properties (e.g., name, summary).  
  * **Edges**: Extract source (start node ID), target (end node ID), and type (relationship label).

This transformation logic should sit in the API layer, shielding the frontend from database specifics.

### **2.4 Visualizing Semantic Types and Ontologies**

One of Cognee's strengths is its support for **Ontologies**. Users define data points using Pydantic models (e.g., class Company(DataPoint), class Employee(DataPoint)).9 This strict typing should be reflected in the visualization.  
**Visual Encoding Strategy:**

* **Color by Type**: In the JSON data, ensure each node has a group or type attribute corresponding to its Pydantic class name. In the frontend, map these types to a categorical color scale (e.g., Company \= Blue, Employee \= Orange).  
* **Ontology-Driven Layout**: Use the ontology structure itself to organize the graph. For example, if the ontology defines a hierarchy (Organization \-\> Department \-\> Team \-\> Person), a hierarchical layout algorithm (like Dagre) might be more appropriate than a force-directed one. react-flow or cytoscape-dagre are excellent libraries for this specific requirement.19

## ---

**3\. Graphiti: Temporal Dynamics and Episodic Visualization**

Graphiti introduces a higher level of complexity by adding **Time** as a first-class citizen. Visualizing Graphiti is not just about showing connections; it's about showing *evolution*.

### **3.1 The Episodic Data Model**

Graphiti's ingestion unit is the **Episode**. Unlike Cognee, which might ingest a whole corpus to build a static graph, Graphiti ingests events.

* **Episodes as Nodes**: An Episode is itself a node in the graph. Entities extracted from that episode are linked to it via MENTIONS edges.5  
* **Visualization Insight**: This allows for a "Provenance View." Users can click on a fact (edge) and trace it back to the specific conversation or document (Episode) where it originated. In a visualization, Episode nodes often act as hubs, clustering the facts derived from them.

### **3.2 The Bi-Temporal Edge Structure**

The most critical feature of Graphiti for visualization is its **Bi-Temporal** data model.6 Every edge contains metadata that defines its lifecycle:

* **created\_at**: The system time when the information was ingested.  
* **valid\_at**: The real-world time when the fact became true.  
* **invalid\_at**: The real-world time when the fact ceased to be true (or was superseded).  
* **expired\_at**: The system time when the fact was deemed obsolete.21

This structure allows the system to model changing states, such as "The President of the US is Barack Obama" (valid 2009-2017) vs. "The President of the US is Donald Trump" (valid 2017-2021). A standard static graph would erroneously show the entity "US" connected to multiple "President" entities simultaneously without context.

### **3.3 Visualizing Time: The Challenge of Invalidation**

Graphiti handles conflicting information via **Edge Invalidation**.22 When a new fact contradicts an old one, the old edge is not deleted; its invalid\_at field is updated. This preserves history.  
**Visualizing Invalidation:**

* **State**: Active edges (where current\_time \< invalid\_at) should be solid and opaque.  
* **History**: Invalid edges (where current\_time \> invalid\_at) can be rendered as:  
  * **Hidden**: To show the "current state" of the world.  
  * **Ghosted**: Rendered with high transparency (low opacity) and dashed lines to show "past knowledge."  
  * **Animated**: As the user moves a time slider, the edge fades out or snaps away.

### **3.4 The Search API and Subgraph Retrieval**

Graphiti's search method returns a SearchResult object that is richer than a simple list.23

* **Hybrid Search**: It combines semantic similarity (vector) with keyword search (BM25) and graph traversal.  
* **Reranking**: Algorithms like Reciprocal Rank Fusion (RRF) and Maximal Marginal Relevance (MMR) score the results.23

Visualizing Search Results:  
Instead of a text list, the search result can be rendered as a Contextual Subgraph.

1. **Central Node**: The query entity (e.g., "Elon Musk").  
2. **Neighbor Nodes**: The entities returned by the search.  
3. **Visual Weight**: Use the search score (relevance) to determine the **size** of the nodes or the **thickness** of the edges. A highly relevant fact appears bold and prominent; a tangential fact appears smaller. This effectively uses visualization as a relevance filter.

## ---

**4\. The Frontend Stack: Detailed Library Ecosystem Analysis**

Transitioning to the frontend, the React ecosystem offers several powerful libraries for graph visualization. The choice depends heavily on the scale of the graph (Cognee's static enterprise graphs vs. Graphiti's potentially massive temporal logs) and the required interactivity.

### **4.1 react-force-graph: The High-Performance Workhorse**

For most AI memory applications, react-force-graph (and its underlying force-graph engine) is the recommended standard due to its performance and flexibility.24

* **Rendering Engines**: It supports three modes:  
  * **2D (Canvas/HTML5)**: Good for text readability and standard interactions.  
  * **3D (WebGL/ThreeJS)**: Essential for large datasets (\>1,000 nodes). It uses the GPU to render thousands of elements at 60FPS. This is critical for "Deep Research" visualization where an agent might generate a massive memory graph.  
  * **VR/AR**: Experimental modes for immersive analytics.  
* **Features for Cognee/Graphiti**:  
  * **Auto-Coloring**: nodeAutoColorBy="group" automatically assigns colors based on Cognee's node types.  
  * **Particles**: linkDirectionalParticles can be used to visualize the flow of information or the "activity" of an edge, which is useful for visualizing Graphiti's "Episodes" feeding into entities.26  
  * **Incremental Updates**: The library monitors the graphData prop. When data changes (e.g., due to a time slider movement), the engine smoothly transitions nodes to their new positions using d3-force physics.27

### **4.2 Cytoscape.js (react-cytoscapejs): The Analytical Precision Tool**

While react-force-graph excels at exploration, Cytoscape.js is superior for structured analysis and strict layouts.28

* **Compound Nodes**: Cytoscape supports nodes *inside* nodes. This is perfect for visualizing Cognee's hierarchical ontologies (e.g., a "Module" node containing "Function" nodes).  
* **Layout Algorithms**: It offers sophisticated layouts like **Dagre** (Directed Acyclic Graph) and **Cola** (constraint-based layout). If the goal is to show a dependency tree of code (as in Cognee's repo-to-graph feature 9), Dagre is far superior to a force-directed layout which creates a "hairball."  
* **Export**: It has native support for exporting graphs to images (PNG/JPG), which is useful for reporting.

### **4.3 ReGraph: The Commercial Alternative**

For enterprise applications where budget permits, **ReGraph** (by Cambridge Intelligence) offers a specialized React SDK.29 It includes a **Time Bar** component out of the box, specifically designed for temporal graph filtering. This aligns perfectly with Graphiti's architecture, significantly reducing the engineering effort required to build custom time sliders.

## ---

**5\. Integration Architecture: Building the Temporal Visualization Stack**

This section details the concrete implementation of a React-based temporal graph visualizer, specifically tailored for Graphiti's data.

### **5.1 Architecture Diagram**

The architecture consists of three layers:

1. **Ingestion/Storage Layer (Python)**: Graphiti \+ FalkorDB/Neo4j.  
2. **API Layer (Python/FastAPI)**: Exposes endpoints for search and graph\_snapshot.  
3. **Presentation Layer (React)**: State management and WebGL rendering.

### **5.2 The React State Model for Temporal Graphs**

To visualize a changing graph, the frontend must manage "Global Time" state.

JavaScript

// Pseudo-code for React State Management  
const \[fullGraph, setFullGraph\] \= useState({ nodes:, links: }); // The complete dataset  
const \= useState({ nodes:, links: }); // What is currently rendered  
const \= useState(Date.now()); // The temporal cursor

// The Filter Effect  
useEffect(() \=\> {  
  if (\!fullGraph.links.length) return;

  // Filter links based on Graphiti's valid\_at/invalid\_at logic  
  const activeLinks \= fullGraph.links.filter(link \=\> {  
    const validFrom \= new Date(link.valid\_at).getTime();  
    const validUntil \= link.invalid\_at? new Date(link.invalid\_at).getTime() : Infinity;  
    return currentTime \>= validFrom && currentTime \< validUntil;  
  });

  // Filter nodes: Keep nodes that have at least one active link  
  // (Or keep all nodes if you want to show isolated entities)  
  const activeNodeIds \= new Set();  
  activeLinks.forEach(l \=\> {  
    activeNodeIds.add(l.source);  
    activeNodeIds.add(l.target);  
  });  
  const activeNodes \= fullGraph.nodes.filter(n \=\> activeNodeIds.has(n.id));

  setDisplayedGraph({ nodes: activeNodes, links: activeLinks });  
},);

### **5.3 Designing the Time Slider Component**

The Time Slider is the user's primary navigation tool. It requires specific features for graph data:

* **Range vs. Point**: A single handle slider (Point) shows the state of the graph *at that moment*. A dual handle slider (Range) shows all events that occurred *within that window*. Graphiti's episodic nature often benefits from a Range slider to see a cluster of recent events.30  
* **Histogram**: A best practice is to render a histogram (bar chart) behind the slider, showing the volume of events (Episodes) at each time bucket. This guides the user to "interesting" periods where the graph was active.  
* **Animation**: A "Play" button that automatically increments currentTime allows users to watch the graph evolve—a technique known as dynamic network visualization.11

### **5.4 Implementing Semantic Zoom (Level of Detail)**

As the user zooms in and out of the graph, the visualization should adapt to prevent information overload.

* **Zoom Level 0 (High)**: Render "Communities" or clusters (e.g., abstracting 50 "Email" nodes into a single "Email History" supernode). Graphiti supports community detection algorithms that can facilitate this.32  
* **Zoom Level 1 (Mid)**: Render individual Nodes but hide labels. Use size to indicate importance (PageRank).  
* **Zoom Level 2 (Low)**: Render full details, including labels and edge text.  
* **Implementation**: react-force-graph exposes the onZoom callback. We can map the zoom level to visual properties (e.g., nodeLabel={zoom \> 2? 'label' : null}).

## ---

**6\. The Role of BAML in Visualization Quality**

A critical, often overlooked aspect of graph visualization is the quality of the data source. If the LLM generates inconsistent schemas (e.g., mixing "Founder" and "Co-founder" relationship types), the visualization becomes a messy "hairball" with duplicate nodes and fragmented clusters.  
**BAML (Boundary AI Markup Language)** is a domain-specific language that enforces strict structural guarantees on LLM outputs.7

### **6.1 Schema Enforcement for Visual Consistency**

By defining a BAML schema *before* ingestion, we ensure that the graph has a consistent topology.

Code snippet

// BAML Schema Definition  
enum EntityType {  
  Person  
  Organization  
  Event  
}

class Node {  
  id: string  
  type: EntityType  
  label: string  
}

When this schema is applied, the backend guarantees that every node has a type property that is exactly one of the enum values.

* **Visual Impact**: The frontend can now safely use a color mapping: const colors \= { Person: 'blue', Organization: 'red', Event: 'green' }. Without BAML, the frontend would need complex error handling for hallucinated types like "People" or "Org" or "Company".  
* **Data Integrity**: BAML's parser repairs malformed JSON from the LLM, ensuring that the graph structure sent to the visualization is valid (no missing IDs, no broken edges).8

### **6.2 BAML \+ Cognee Pipeline**

The ideal architecture involves using BAML to extract structured data from text, and then feeding that pristine data into Cognee's add\_data\_points method. This results in a "Clean Graph" that is significantly easier to visualize and navigate.

## ---

**7\. Performance Optimization for "Deep Research" Scale**

In a Deep Research scenario, an AI agent might process thousands of documents, resulting in a graph with tens of thousands of nodes. Rendering this directly in the DOM (Document Object Model) will crash the browser.

### **7.1 WebGL is Mandatory**

For graphs \> 1,000 nodes, SVG/Canvas based libraries (D3, Cytoscape) struggle. react-force-graph-3d uses **ThreeJS (WebGL)**, delegating rendering to the GPU.24 This allows for the visualization of 50,000+ entities with smooth frame rates.

### **7.2 Server-Side Subgraphing**

Never send the entire database to the client if it exceeds 10k nodes.

* **Pattern**: Initial load fetches the "Meta-Graph" (high-level ontology or clusters).  
* **Interaction**: When a user searches for a term or clicks a node, the frontend requests a *local neighborhood* (e.g., k-hop neighbors) from the API.  
* **Merging**: The frontend merges this new subgraph into the existing visualization state. react-force-graph handles this incremental addition gracefully, animating the new nodes sprouting from the parent.35

### **7.3 Graph Database Indexing**

To support real-time visualization (especially with a time slider), the backend graph database must be indexed heavily.

* **FalkorDB/Redis**: Its in-memory nature provides sub-millisecond access, which is ideal for the rapid queries generated by a dragging time slider.3  
* **Indexing Strategy**: Create composite indices on valid\_at and entity\_type to allow for fast range queries.

## ---

**8\. Conclusion**

The visualization of knowledge graphs in AI systems is transitioning from a static debugging task to a dynamic, user-facing interaction paradigm. **Cognee** provides the structural rigor through its deterministic pipelines, making it the bedrock for static knowledge representation. **Graphiti** introduces the necessary temporal dimension, enabling the visualization of memory *evolution*—a critical feature for autonomous agents that operate over long time horizons.  
For the frontend architect, the challenge lies in managing the complexity of bi-temporal data and high-dimensional topologies. By adopting a stack that combines **BAML** for schema integrity, **Graphiti** for temporal storage, and **WebGL-powered React libraries** (react-force-graph) for rendering, it is possible to create immersive, "Time Machine" interfaces. These interfaces do not merely display data; they explain the AI's reasoning process, fostering the trust required for the widespread adoption of agentic AI.  
The future of this domain lies in **Generative UI**, where the agent not only retrieves the graph but also generates the optimal visualization configuration (filters, colors, layouts) on the fly to best answer the user's specific query.

### **Summary Table: Architectural Recommendations**

| Component | Recommendation | Justification |
| :---- | :---- | :---- |
| **Extraction** | **BAML** | Enforces strict schema, preventing "hairball" graphs due to dirty data. |
| **Static Memory** | **Cognee** | Best for ontology-aligned, deterministic knowledge bases. |
| **Dynamic Memory** | **Graphiti** | Essential for tracking state changes and history (Time Travel). |
| **Database** | **FalkorDB** | Low latency required for real-time visualization interaction. |
| **Vis Library** | **react-force-graph-3d** | Best performance for large datasets; 3D effectively separates clusters. |
| **Layout** | **Force-Directed** | Best for general exploration; switch to **Dagre** (Cytoscape) for hierarchies. |
| **Interaction** | **Time Slider** | Mandatory for Graphiti to filter edges by valid\_at timestamps. |

#### **Works cited**

1. From RAG to Graphs: How Cognee is Building Self-Improving AI Memory \- Memgraph, accessed December 5, 2025, [https://memgraph.com/blog/from-rag-to-graphs-cognee-ai-memory](https://memgraph.com/blog/from-rag-to-graphs-cognee-ai-memory)  
2. The AI-Native GraphDB \+ GraphRAG \+ Graph Memory Landscape & Market Catalog, accessed December 5, 2025, [https://dev.to/yigit-konur/the-ai-native-graphdb-graphrag-graph-memory-landscape-market-catalog-2198](https://dev.to/yigit-konur/the-ai-native-graphdb-graphrag-graph-memory-landscape-market-catalog-2198)  
3. Cognee | FalkorDB Docs, accessed December 5, 2025, [https://docs.falkordb.com/agentic-memory/cognee.html](https://docs.falkordb.com/agentic-memory/cognee.html)  
4. Introduction \- Cognee Documentation, accessed December 5, 2025, [https://docs.cognee.ai/getting-started/introduction](https://docs.cognee.ai/getting-started/introduction)  
5. getzep/graphiti: Build Real-Time Knowledge Graphs for AI Agents \- GitHub, accessed December 5, 2025, [https://github.com/getzep/graphiti](https://github.com/getzep/graphiti)  
6. Graphiti: Knowledge Graph Memory for an Agentic World \- Neo4j, accessed December 5, 2025, [https://neo4j.com/blog/developer/graphiti-knowledge-graph-memory/](https://neo4j.com/blog/developer/graphiti-knowledge-graph-memory/)  
7. BAML documentation, accessed December 5, 2025, [https://docs.boundaryml.com/home](https://docs.boundaryml.com/home)  
8. Neo4j Live: Generating Graph Data from Unstructured Data with BAML, accessed December 5, 2025, [https://neo4j.com/videos/neo4j-live-generating-graph-data-from-unstructured-data-with-baml/](https://neo4j.com/videos/neo4j-live-generating-graph-data-from-unstructured-data-with-baml/)  
9. Build a Knowledge Graph from a Python Repo: A Simple Guide \- Cognee, accessed December 5, 2025, [https://www.cognee.ai/blog/deep-dives/repo-to-knowledge-graph](https://www.cognee.ai/blog/deep-dives/repo-to-knowledge-graph)  
10. Cognee vs RAG: graph-powered AI memory in Deepnote, accessed December 5, 2025, [https://deepnote.com/explore/cognee-vs-rag-graph-powered-ai-memory-in-deepnote](https://deepnote.com/explore/cognee-vs-rag-graph-powered-ai-memory-in-deepnote)  
11. Temporal force-directed graph / D3 \- Observable, accessed December 5, 2025, [https://observablehq.com/@d3/temporal-force-directed-graph](https://observablehq.com/@d3/temporal-force-directed-graph)  
12. Cognee \- LlamaIndex, accessed December 5, 2025, [https://developers.llamaindex.ai/python/framework-api-reference/graph\_rag/cognee/](https://developers.llamaindex.ai/python/framework-api-reference/graph_rag/cognee/)  
13. Tutorial — pyvis 0.1.3.1 documentation \- Read the Docs, accessed December 5, 2025, [https://pyvis.readthedocs.io/en/latest/tutorial.html](https://pyvis.readthedocs.io/en/latest/tutorial.html)  
14. graphiti/mcp\_server/README.md at main \- GitHub, accessed December 5, 2025, [https://github.com/getzep/graphiti/blob/main/mcp\_server/README.md](https://github.com/getzep/graphiti/blob/main/mcp_server/README.md)  
15. Graph Stores \- Cognee Documentation, accessed December 5, 2025, [https://docs.cognee.ai/setup-configuration/graph-stores](https://docs.cognee.ai/setup-configuration/graph-stores)  
16. Deploying Cognee AI Starter App on AWS ECS Using Terraform \- DEV Community, accessed December 5, 2025, [https://dev.to/aws-builders/deploying-cognee-ai-starter-app-on-aws-ecs-using-terraform-4ma9](https://dev.to/aws-builders/deploying-cognee-ai-starter-app-on-aws-ecs-using-terraform-4ma9)  
17. Method to export networkx graph to json graph file? \- Stack Overflow, accessed December 5, 2025, [https://stackoverflow.com/questions/32133009/method-to-export-networkx-graph-to-json-graph-file](https://stackoverflow.com/questions/32133009/method-to-export-networkx-graph-to-json-graph-file)  
18. Method to save networkx graph to json graph? \- Stack Overflow, accessed December 5, 2025, [https://stackoverflow.com/questions/3162909/method-to-save-networkx-graph-to-json-graph](https://stackoverflow.com/questions/3162909/method-to-save-networkx-graph-to-json-graph)  
19. React Cytoscape Examples. In this blog post, I will explain the… | by Onur Dayıbaşı | Enterprise React Knowledge Maps | Medium, accessed December 5, 2025, [https://medium.com/react-digital-garden/react-cytoscape-examples-45dd84a1507d](https://medium.com/react-digital-garden/react-cytoscape-examples-45dd84a1507d)  
20. Adding Episodes \- Zep Documentation, accessed December 5, 2025, [https://help.getzep.com/graphiti/core-concepts/adding-episodes](https://help.getzep.com/graphiti/core-concepts/adding-episodes)  
21. Searching the Graph \- Zep Documentation, accessed December 5, 2025, [https://help.getzep.com/v2/searching-the-graph](https://help.getzep.com/v2/searching-the-graph)  
22. Building Temporal Knowledge Graphs with Graphiti \- FalkorDB, accessed December 5, 2025, [https://www.falkordb.com/blog/building-temporal-knowledge-graphs-graphiti/](https://www.falkordb.com/blog/building-temporal-knowledge-graphs-graphiti/)  
23. Searching the Graph \- Zep Documentation, accessed December 5, 2025, [https://help.getzep.com/graphiti/working-with-data/searching](https://help.getzep.com/graphiti/working-with-data/searching)  
24. vasturiano/react-force-graph: React component for 2D, 3D, VR and AR force directed graphs \- GitHub, accessed December 5, 2025, [https://github.com/vasturiano/react-force-graph](https://github.com/vasturiano/react-force-graph)  
25. 15 Best Graph Visualization Tools for Your Neo4j Graph Database, accessed December 5, 2025, [https://neo4j.com/blog/graph-visualization/neo4j-graph-visualization-tools/](https://neo4j.com/blog/graph-visualization/neo4j-graph-visualization-tools/)  
26. react-force-graph/example/expandable-nodes/index.html at master \- GitHub, accessed December 5, 2025, [https://github.com/vasturiano/react-force-graph/blob/master/example/expandable-nodes/index.html](https://github.com/vasturiano/react-force-graph/blob/master/example/expandable-nodes/index.html)  
27. Interactive & Dynamic Force-Directed Graphs with D3 | by Robin Weser \- Medium, accessed December 5, 2025, [https://medium.com/ninjaconcept/interactive-dynamic-force-directed-graphs-with-d3-da720c6d7811](https://medium.com/ninjaconcept/interactive-dynamic-force-directed-graphs-with-d3-da720c6d7811)  
28. Cytoscape.js: A Versatile Data Visualization Tool \- Rapidops, accessed December 5, 2025, [https://www.rapidops.com/blog/cytoscape-js/](https://www.rapidops.com/blog/cytoscape-js/)  
29. ReGraph | Graph Visualization Software For React Developers \- Cambridge Intelligence, accessed December 5, 2025, [https://cambridge-intelligence.com/regraph/](https://cambridge-intelligence.com/regraph/)  
30. react-time-range-slider \- GitHub Pages, accessed December 5, 2025, [https://ashvin27.github.io/react-time-range-slider/](https://ashvin27.github.io/react-time-range-slider/)  
31. Build a Custom Time Slider Component with Ant Design and Next.js | Paige Niedringhaus, accessed December 5, 2025, [https://www.paigeniedringhaus.com/blog/build-a-custom-time-slider-component-with-ant-design-and-next-js/](https://www.paigeniedringhaus.com/blog/build-a-custom-time-slider-component-with-ant-design-and-next-js/)  
32. Graphiti (Knowledge Graph Agent Memory) Gets Custom Entity Types : r/LLMDevs \- Reddit, accessed December 5, 2025, [https://www.reddit.com/r/LLMDevs/comments/1j0ca03/graphiti\_knowledge\_graph\_agent\_memory\_gets\_custom/](https://www.reddit.com/r/LLMDevs/comments/1j0ca03/graphiti_knowledge_graph_agent_memory_gets_custom/)  
33. The Prompting Language Every AI Engineer Should Know: A BAML Deep Dive \- Towards AI, accessed December 5, 2025, [https://pub.towardsai.net/the-prompting-language-every-ai-engineer-should-know-a-baml-deep-dive-6a4cd19a62db](https://pub.towardsai.net/the-prompting-language-every-ai-engineer-should-know-a-baml-deep-dive-6a4cd19a62db)  
34. Why I'm excited about BAML and the future of agentic workflows \- The Data Quarry, accessed December 5, 2025, [https://thedataquarry.com/blog/baml-and-future-agentic-workflows/](https://thedataquarry.com/blog/baml-and-future-agentic-workflows/)  
35. react-d3-graph 2.6.0 | Documentation, accessed December 5, 2025, [https://danielcaldas.github.io/react-d3-graph/docs/](https://danielcaldas.github.io/react-d3-graph/docs/)