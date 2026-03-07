# **Unified Schema Architecture for Agentic AI Systems: Integrating BAML, dlt, and TanStack AI across Multi-Modal Data Layers**

## **1\. Introduction: The Convergence of Probabilistic and Deterministic Systems**

The rapid evolution of artificial intelligence from simple text generation to complex, agentic workflows has precipitated a fundamental crisis in software architecture: the impedance mismatch between probabilistic generative models and deterministic data systems. Large Language Models (LLMs) operate in a stochastic domain, producing variable, unstructured, or semi-structured natural language outputs. In contrast, the operational backbone of modern enterprise software—comprising relational databases, vector stores, graph ontologies, and strictly typed frontend applications—demands rigid, validated, and deterministic schemas.  
This report creates a comprehensive architectural blueprint for bridging this divide. It explores a rigorous, "schema-first" methodology that leverages **BAML (Boundary AI Markup Language)** as a single source of truth.1 By defining data contracts in BAML, we establish a unified definition that propagates downstream into two distinct ecosystem branches: the **Python data engineering layer**, orchestrated by **dlt (Data Load Tool)** and **Pydantic**, and the **TypeScript application layer**, governed by **Zod**, **TanStack AI**, and **oRPC**.  
This analysis specifically addresses the challenge of creating an end-to-end workflow where a single schema definition drives data ingestion into a complex, multi-modal persistence layer. This layer includes **LanceDB** for high-performance vector similarity search, **FalkorDB** for graph-based relationship modeling, **Graphiti** for temporal, episodic memory, and **PostgreSQL** and **DuckDB** for traditional relational and analytical processing. We will demonstrate that by treating BAML as the interface definition language (IDL) for AI, organizations can automate the generation of **Pydantic models** and **Zod schemas**, thereby ensuring type safety, reducing schema drift, and accelerating the deployment of reliable AI agents.3

### **1.1 The Shift from Prompt Engineering to Schema Engineering**

Historically, "prompt engineering" involved crafting natural language instructions to cajole an LLM into producing a desired output format, often JSON. This approach is brittle; a model update or a slight variation in input can break the downstream parser.6 The industry is shifting towards "schema engineering," where the focus is on defining the exact structure of the desired output, and the tooling handles the prompt formulation and parsing.  
BAML represents the maturation of this shift. Unlike libraries that rely on runtime JSON schema validation (which can be token-heavy and slow), BAML uses a specialized compiler and a "Schema-Aligned Parsing" (SAP) algorithm.1 This allows developers to define types (classes, enums) in a DSL that feels like TypeScript or Python but compiles into optimal prompt instructions and robust parsing logic.  
The central thesis of this architecture is that **BAML should act as the master definition**. From this master definition, all other representations—SQL tables in Postgres, Node labels in FalkorDB, Zod validators in the browser—are derived. This prevents the common anti-pattern where the frontend TypeScript interface and the backend database schema slowly diverge, leading to application failures when the AI produces data that satisfies one but not the other.

### **1.2 Architectural Overview**

The proposed system functions as a bi-directional pipeline centered on BAML:

1. **Definition Layer:** BAML files define the entities (e.g., ResearchPaper, UserIntent) and the functions that extract them from unstructured text.  
2. **Generative Layer (Python):** The BAML compiler generates **Pydantic** models. **dlt** uses these models to inspect the schema and automatically construct pipelines that load data into LanceDB, FalkorDB, Graphiti, and SQL databases.3  
3. **Application Layer (TypeScript):** The BAML compiler generates **TypeScript interfaces**. A secondary transpile step (using ts-to-zod) converts these interfaces into runtime **Zod schemas**. These schemas then drive **TanStack AI** tool definitions, **oRPC** API contracts, and **Drizzle ORM** interactions.4

This report provides the exhaustive technical implementation details for every component of this triad, ensuring a "clear and easy" workflow as requested.

## ---

**2\. The Single Source of Truth: BAML Configuration and Mechanics**

To achieve the goal of a unified workflow, the BAML environment must be configured not just as a prompt manager, but as a polyglot code generator. This section details the specific configurations required to output client code for both the Python (backend/data) and TypeScript (frontend/API) environments simultaneously.

### **2.1 Dual-Target Code Generation**

Standard BAML tutorials often focus on a single language. However, for a full-stack application using Python for heavy data lifting (dlt) and TypeScript for the application interface (TanStack/Next.js), we must configure multiple generators. This is done in the baml\_src/generators.baml file.10  
**Code Listing 1: generators.baml for Polyglot Support**

Code snippet

// baml\_src/generators.baml

// Generator 1: Python Data Layer  
// This targets the dlt pipeline and backend processing.  
generator python\_client {  
  output\_type "python/pydantic"  
  output\_dir "../backend/baml\_client" // Path to Python backend  
  version "0.76.2"  
  default\_client\_mode "async" // Asynchronous for high-throughput dlt ingestion  
}

// Generator 2: TypeScript Application Layer  
// This targets the Next.js/React frontend and oRPC layer.  
generator typescript\_client {  
  output\_type "typescript"  
  output\_dir "../frontend/src/baml\_client" // Path to TS frontend  
  version "0.76.2"  
  default\_client\_mode "async"  
}

This configuration ensures that every time baml-cli generate is executed, two distinct but semantically identical libraries are created. The Python library will contain **Pydantic BaseModel** definitions, while the TypeScript library will contain **TypeScript interface** definitions.3 This synchronization is the bedrock of the entire architecture.

### **2.2 Defining Complex Entities**

The complexity of the target databases—specifically the Graph and Vector stores—requires rich entity definitions. A simple flat schema is insufficient. We must define relationships and nested structures in BAML that can eventually be mapped to graph nodes and edges.  
**Code Listing 2: models.baml**

Code snippet

// baml\_src/models.baml

enum EntityType {  
  PERSON  
  ORGANIZATION  
  LOCATION  
  CONCEPT  
}

class IdentifiedEntity {  
  name string @description("The canonical name of the entity")  
  type EntityType  
  confidence float  
}

class ResearchInsight {  
  id string @description("UUID")  
  title string  
  summary string  
  // Nested objects for Graph extraction  
  entities IdentifiedEntity  
  // Vector embedding target  
  embedding\_context string @description("Text used for vectorization")  
  citations string  
  published\_date string  
}

function ExtractInsight(text: string) \-\> ResearchInsight {  
  client "openai/gpt-4o"  
  prompt \#"  
    Analyze the following text and extract the research insight.  
    Identify key entities and their types.  
      
    {{ ctx.output\_format }}

    Text:  
    {{ text }}  
  "\#  
}

The annotation @description is critical. In the Pydantic output, these become field descriptions usable by dlt for documentation. In the TypeScript output, they act as JSDoc comments, aiding developers using the generated types.4

## ---

**3\. Python Data Layer: Integrating BAML, Pydantic, and dlt**

The integration of BAML with **dlt** (Data Load Tool) leverages dlt's native ability to introspect Pydantic models. This capability turns the BAML-generated classes into "Schema Hints," allowing dlt to automatically create tables, handle nesting, and enforce data contracts without manual DDL (Data Definition Language).5

### **3.1 The BAML-to-dlt Bridge**

dlt operates on the concept of **Resources**. A resource is a generator function that yields data items. By annotating a resource with a BAML-generated Pydantic model, we inform dlt of the expected schema.  
**Code Listing 3: The dlt Resource Definition**

Python

import dlt  
from typing import Iterator  
from backend.baml\_client import b  
from backend.baml\_client.types import ResearchInsight

\# Define a dlt source that uses the BAML function  
@dlt.source  
def research\_source(texts: list\[str\]):  
      
    @dlt.resource(  
        name="research\_insights",  
        write\_disposition="merge", \# Upsert based on primary key  
        primary\_key="id",  
        columns=ResearchInsight \# Pydantic model defines the schema  
    )  
    def extract\_insights() \-\> Iterator:  
        for text in texts:  
            \# BAML call: Returns a validated Pydantic object  
            \# This object is strictly typed according to models.baml  
            insight \= b.ExtractInsight(text)  
            yield insight

    return extract\_insights

In this workflow, the **Schema-Aligned Parsing (SAP)** of BAML ensures that the object yielded to dlt is valid.1 If the LLM produces a malformed string that cannot be coerced into the ResearchInsight structure, BAML raises an exception before dlt ever sees the data. This "fail-fast" mechanism prevents schema pollution in the downstream databases.

### **3.2 Multi-Database Ingestion Strategy**

The user's requirement involves a diverse set of destinations: **LanceDB**, **FalkorDB**, **Graphiti**, **PostgreSQL**, and **DuckDB**. dlt supports "polyglot persistence"—sending data to multiple destinations—but the implementation varies significantly for each.

#### **3.2.1 Relational and Analytical Stores: PostgreSQL and DuckDB**

These are supported natively by dlt. The Pydantic model maps directly to SQL tables. Nested fields like entities (List of Objects) are handled by dlt's normalization engine, which automatically creates a child table research\_insights\_\_entities linked by foreign keys.5  
Configuration:  
The choice between Postgres and DuckDB is strictly a configuration change in secrets.toml. No code change is required in the BAML or Python logic.

Ini, TOML

\# secrets.toml  
\[destination.postgres\]  
credentials \= "postgresql://loader:password@localhost:5432/dlt\_data"

\[destination.duckdb\]  
credentials \= "duckdb:///./data/analytics.db"

#### **3.2.2 Vector Database: LanceDB**

LanceDB integration requires defining *what* to vectorise. BAML provides the structured data, but dlt's lancedb\_adapter is needed to instruct the embedding model.12  
Implementation:  
The ResearchInsight model contains a field embedding\_context. We use the adapter to tell LanceDB to generate embeddings for this specific field.

Python

from dlt.destinations.adapters import lancedb\_adapter

def configure\_lancedb\_pipeline():  
    \# Instantiate the source  
    source \= research\_source(\["..."\])  
      
    \# Apply the adapter to the resource within the source  
    \# This modifies the schema hint to include embedding instructions  
    lancedb\_adapter(  
        source.extract\_insights,  
        embed=\["embedding\_context", "summary"\]   
    )  
      
    pipeline \= dlt.pipeline(  
        pipeline\_name="vector\_ingestion",  
        destination="lancedb",  
        dataset\_name="research\_vectors"  
    )  
    return pipeline

This integration is seamless because the Pydantic model generated by BAML guarantees the existence of embedding\_context and summary. If the BAML definition changes (e.g., removing summary), the Pydantic model updates, and dlt (or static analysis tools like mypy) will catch the error in the adapter configuration immediately.

#### **3.2.3 Graph Database: FalkorDB**

FalkorDB, a Redis-based graph database, does not have a native dlt destination. To support the user's requirement, we must architect a **Custom dlt Destination**. This involves writing a function decorated with @dlt.destination that accepts the BAML-generated items and translates them into Cypher queries.14  
The Custom Destination Logic:  
The challenge here is mapping the flat (or hierarchical) Pydantic structure into Nodes and Edges.  
**Code Listing 4: FalkorDB Custom Destination**

Python

import dlt  
from falkordb import FalkorDB

@dlt.destination(batch\_size=50)  
def falkordb\_destination(items, table\_schema):  
    """  
    Custom dlt destination to load BAML objects into FalkorDB.  
    """  
    \# Connect to FalkorDB  
    client \= FalkorDB(host='localhost', port=6379)  
    graph \= client.select\_graph('KnowledgeGraph')  
      
    for item in items:  
        \# 1\. Create the Insight Node  
        \# We sanitize inputs; in production use proper parameter binding  
        query\_insight \= """  
        MERGE (i:Insight {id: $id})  
        SET i.title \= $title, i.summary \= $summary, i.date \= $date  
        """  
        params\_insight \= {  
            'id': item\['id'\],  
            'title': item\['title'\],  
            'summary': item\['summary'\],  
            'date': item\['published\_date'\]  
        }  
        graph.query(query\_insight, params\_insight)  
          
        \# 2\. Create Entity Nodes and Relationships  
        \# Note: dlt might pass 'entities' as a list if we configured   
        \# dlt not to normalize, or we might need to join child tables.  
        \# Assuming we receive the nested list (via dlt\_config={"skip\_nested\_types": True}):  
          
        if 'entities' in item:  
            for entity in item\['entities'\]:  
                \# Cypher to merge entity and link it  
                query\_rel \= """  
                MATCH (i:Insight {id: $id})  
                MERGE (e:Entity {name: $e\_name})  
                SET e.type \= $e\_type  
                MERGE (i)--\>(e)  
                """  
                params\_rel \= {  
                    'id': item\['id'\],  
                    'e\_name': entity\['name'\],  
                    'e\_type': entity\['type'\],  
                    'conf': entity\['confidence'\]  
                }  
                graph.query(query\_rel, params\_rel)

This custom destination enables the "clear and easy" workflow requested: the BAML class defines the structure, and this generic loader persists it to the graph. The use of MERGE ensures idempotency, which aligns with dlt's retry mechanisms.16

#### **3.2.4 Agentic Memory: Graphiti**

Graphiti is a higher-level abstraction than FalkorDB, specifically designed for *temporal* knowledge graphs (managing "episodes"). It requires a different ingestion approach via its add\_episode API rather than raw Cypher.18  
**Code Listing 5: Graphiti Custom Destination**

Python

from graphiti\_core import Graphiti, EpisodeType  
import asyncio

@dlt.destination(batch\_size=10)  
def graphiti\_destination(items, table\_schema):  
    """  
    Loads data into Graphiti as episodes.  
    """  
    async def \_ingest\_batch():  
        \# Graphiti client initialization  
        client \= Graphiti("falkor://localhost:6379")  
          
        for item in items:  
            \# We treat each BAML output as a 'JSON' episode.  
            \# Graphiti will parse the JSON and extract temporal edges automatically.  
            await client.add\_episode(  
                name=f"insight\_{item\['id'\]}",  
                episode\_body=item, \# Pass the entire Pydantic dict  
                source=EpisodeType.json,  
                source\_description="BAML Extracted Research",  
                reference\_time=datetime.now() \# Or parsed from item\['published\_date'\]  
            )  
          
        await client.close()

    \# dlt runs synchronously, so we must bridge to asyncio  
    asyncio.run(\_ingest\_batch())

This integration is particularly powerful. By feeding the BAML-structured output directly into Graphiti's JSON ingestion, we leverage Graphiti's internal logic to handle temporal deduplication and history tracking, which effectively gives the AI agent a "long-term memory" of all research findings processed by the pipeline.18

## ---

**4\. TypeScript Application Layer: BAML to Zod to TanStack**

The frontend requirements involve **TanStack AI** (formerly Vercel AI SDK competitors/alternatives within the TanStack ecosystem) and **oRPC** (end-to-end typed APIs). Both libraries rely heavily on **Zod** for runtime schema validation.  
Since BAML generates TypeScript *interfaces* but not Zod *schemas* natively, we must bridge this gap. This section details the automation of this bridge, creating the "Source of Truth" workflow.

### **4.1 The Automation Bridge: ts-to-zod**

The key to a maintainable workflow is avoiding manual Zod schema writing. If you write the Zod schema manually, it will eventually drift from the BAML definition. We solve this with ts-to-zod, a tool that generates Zod schemas from TypeScript definitions.9  
**Workflow Setup:**

1. **BAML Generation:** Outputs frontend/src/baml\_client/types.ts.  
2. **Zod Generation:** Reads types.ts and outputs frontend/src/gen/zod.ts.

**Code Listing 6: Automation Script (package.json)**

JSON

{  
  "scripts": {  
    "generate:baml": "baml-cli generate",  
    "generate:zod": "ts-to-zod \--input./src/baml\_client/types.ts \--output./src/gen/zod.ts \--skipValidation",  
    "codegen": "npm run generate:baml && npm run generate:zod"  
  }  
}

By running npm run codegen, the developer ensures that the Zod schemas available to the frontend are mathematically identical to the types defined in the BAML files. This fulfills the "easy way" requirement.

### **4.2 Integrating with TanStack AI**

TanStack AI uses the concept of "Isomorphic Tools"—tools that can be defined once and run on either the client or server. These tools require a Zod schema to validate the input arguments provided by the LLM.8  
**Code Listing 7: Defining a TanStack Tool with BAML Schemas**

TypeScript

// src/tools/researchTools.ts  
import { toolDefinition } from '@tanstack/ai';  
import { researchInsightSchema } from '../gen/zod'; // Generated via ts-to-zod  
import { z } from 'zod';

// Tool to save a research finding  
// The input schema is derived directly from the BAML definition  
export const saveInsightTool \= toolDefinition({  
  name: 'save\_insight',  
  description: 'Persists a validated research insight to the database.',  
  inputSchema: researchInsightSchema,   
  execute: async (insight) \=\> {  
    // 'insight' is fully typed as ResearchInsight interface  
    // Implementation to save to DB (via oRPC or Drizzle)  
    console.log(\`Saving insight: ${insight.title}\`);  
    return { success: true, id: insight.id };  
  },  
});

This integration implies that if the BAML definition of ResearchInsight changes (e.g., adding a sentiment field), the ts-to-zod script updates researchInsightSchema. Consequently, the TanStack AI tool definition automatically updates to require sentiment. The next time the LLM calls this tool, it will be validated against the new schema, ensuring end-to-end consistency.22

### **4.3 Integrating with oRPC and Drizzle**

**oRPC** (Open RPC) allows defining type-safe API procedures. It pairs naturally with Zod. **Drizzle ORM** is a TypeScript ORM that also integrates with Zod.  
**The Data Flow:**

1. **BAML:** Defines the shape of the data.  
2. **Zod:** Validates the data at the API boundary (oRPC).  
3. **Drizzle:** Persists the data to Postgres.

**Code Listing 8: oRPC Router Definition**

TypeScript

import { os } from '@orpc/server';  
import { researchInsightSchema } from '../gen/zod';  
import { db } from '../db/drizzle';  
import { insightsTable } from '../db/schema';

export const appRouter \= os.router({  
  // Define a procedure that accepts the BAML-structure  
  submitInsight: os.procedure  
   .input(researchInsightSchema) // Runtime validation using BAML-derived Zod  
   .handler(async ({ input }) \=\> {  
      // 'input' is strictly typed.  
      // Drizzle insert:  
      await db.insert(insightsTable).values({  
        id: input.id,  
        title: input.title,  
        summary: input.summary,  
        publishedDate: input.published\_date,  
        // For simple Postgres storage, we might store complex objects as JSON  
        entities: input.entities   
      });  
      return { status: 'stored' };  
    }),  
});

Drizzle Considerations:  
Drizzle tables (pgTable) are static definitions. While libraries like drizzle-zod exist to generate Zod from Drizzle, our architecture requires the reverse (BAML $\\to$ Zod $\\to$ Drizzle usage).  
To keep Drizzle in sync, developers should define the Drizzle columns to match the BAML types. The use of input.entities in the example above assumes the entities column in Postgres is defined as jsonb type, which is a best practice for storing complex nested BAML outputs in a relational database without excessive normalization.5

## ---

**5\. End-to-End Workflow Synthesis**

To visualize how these disparate components function as a single unit, we outline the lifecycle of a single data point—a "Research Finding"—through the system.

### **5.1 The Workflow Lifecycle**

| Stage | Component | Action | Schema State |
| :---- | :---- | :---- | :---- |
| **1\. Definition** | **BAML** | Developer defines class Finding { title string... } in .baml. | **Source of Truth** |
| **2\. Compilation** | **baml-cli** | Generates Finding Pydantic model and Finding TS interface. | Synced |
| **3\. Bridge** | **ts-to-zod** | Generates findingSchema (Zod). | Synced |
| **4\. Ingestion** | **Python / BAML** | b.ExtractFinding(text) calls LLM. BAML parser (SAP) enforces valid JSON matching Finding. | Validated Instance |
| **5\. ETL** | **dlt** | Pipeline receives Finding object. Routes to: \- **LanceDB:** Embeds summary via adapter. \- **FalkorDB:** Cypher MERGE via custom dest. \- **Postgres:** Inserts row via standard dest. | Persisted |
| **6\. Frontend** | **TanStack AI** | User asks "What findings mention AI?". Agent calls search\_tool. | \- |
| **7\. API** | **oRPC** | Tool calls search procedure. oRPC validates response using Zod schema. | Validated Response |
| **8\. UI** | **React** | Component receives typed data. TS Interface ensures type safety in .tsx rendering. | Rendered |

### **5.2 Handling Schema Evolution (The "Easy" Way)**

A major requirement of the user was ease of use. Consider the scenario where the requirement changes: we now need to track the author of a finding.  
**Step 1: Update BAML**

Code snippet

class ResearchInsight {  
  //... existing fields  
  author string? @description("Primary author name")  
}

Step 2: Run Codegen  
Run npm run codegen (which runs baml-cli generate and ts-to-zod).  
Step 3: Update dlt (Automatic)  
On the next pipeline run, dlt detects the new author field in the Pydantic model. It automatically performs an ALTER TABLE on Postgres to add the column and updates the LanceDB schema. No migration script is needed.5  
Step 4: Update Frontend (Compiler Guided)  
The TypeScript compiler will flag any oRPC handlers or TanStack tool usages that might strict-check keys (though optional fields usually pass). The Zod schema automatically includes .optional() for the author, so runtime validation continues to pass for old data.  
This workflow minimizes the manual overhead typically associated with managing schemas across five different databases and a full-stack application.

## ---

**6\. Table: Feature Matrix of Integration Components**

The following table summarizes the capability mapping between the BAML source of truth and the various downstream consumers.

| Component | Role | BAML Integration Mechanism | Validation Timing |
| :---- | :---- | :---- | :---- |
| **dlt (Core)** | Pipeline Orchestrator | Pydantic Model (Direct) | Runtime (Schema Contract) |
| **PostgreSQL** | Relational Store | dlt Native Destination | Write-Time (DB Constraints) |
| **LanceDB** | Vector Store | dlt Adapter (lancedb\_adapter) | Write-Time (Schema Check) |
| **FalkorDB** | Graph Store | Custom Destination (Cypher) | Write-Time (Graph Logic) |
| **Graphiti** | Agent Memory | Custom Dest. (add\_episode) | Ingestion-Time (Graphiti Internal) |
| **TanStack AI** | Tool Definitions | Zod (via ts-to-zod) | Generation-Time (LLM output) |
| **oRPC** | API RPC | Zod (via ts-to-zod) | Request-Time (API Boundary) |
| **Drizzle** | ORM | Manual Mapping / Zod Validation | Write-Time (Query Construction) |

## ---

**7\. Strategic Insights and Second-Order Implications**

### **7.1 The "Compiler" Paradigm Shift**

By moving the validation logic from the runtime application code (checking if data.key exists) to the BAML compiler level, the architecture effectively "shifts left" on data quality. The BAML compiler's SAP algorithm is deterministic in its parsing, even if the LLM is probabilistic.1 This implies that the system behaves more like a compiled application: if it builds (i.e., if BAML extracts successfully), it runs. This contrasts with "Tool Calling" APIs where the LLM might call a tool with valid JSON syntax but invalid business logic schema, requiring complex error handling in the application.

### **7.2 The Graphiti Temporal Advantage**

Integrating Graphiti via dlt offers a subtle but profound advantage over standard RAG. Standard vector stores (LanceDB) provide semantic similarity ("This text looks like that text"). Graphiti provides temporal provenance ("This fact was true on Tuesday but contradicted on Wednesday").18  
By using dlt to feed Graphiti, the pipeline essentially creates a "Time Machine" for the AI agent. The agent can query not just what is known, but when it was learned. This is critical for research applications where data obsolescence is a factor.

### **7.3 Latency vs. Throughput in Hybrid Storage**

Writing to five databases (Postgres, DuckDB, LanceDB, FalkorDB, Graphiti) introduces latency.

* **Insight:** dlt pipelines should be asynchronous and decoupled from the user-facing request loop.  
* **Architecture:** The BAML extraction should occur in a background worker (e.g., Celery or Temporal). The frontend should utilize **Optimistic UI** patterns or polling, as waiting for a synchronous write to five distinct storage engines will degrade user experience.  
* **Optimization:** Use dlt's parallelism features (asyncio) for the extraction phase, but acknowledge that the loading phase (especially to FalkorDB and Graphiti) may need to be serialized or batched carefully to avoid rate limits or locking issues.23

## **8\. Conclusion**

The integration of **BAML classes** into **TypeScript Zod** and **Python Pydantic** workflows is not only possible but constitutes a robust, enterprise-grade architecture for modern AI applications.  
The path is clear:

1. **Adopt BAML** as the unyielding source of truth.  
2. **Automate** the Pydantic-to-dlt and Interface-to-Zod bridges using standard CLI tools.  
3. **Implement Custom Destinations** for the advanced graph and memory stores (FalkorDB, Graphiti) where native dlt support is nascent.  
4. **Leverage TanStack AI's Isomorphic Tools** to bind the generated Zod schemas directly to the agent's cognitive capabilities.

This approach transforms the chaotic stochasticity of LLM outputs into a structured, type-safe, and multi-modal data river, enabling the construction of AI agents that are both powerful and reliable.

#### **Works cited**

1. Why I'm excited about BAML and the future of agentic workflows \- The Data Quarry, accessed December 5, 2025, [https://thedataquarry.com/blog/baml-and-future-agentic-workflows/](https://thedataquarry.com/blog/baml-and-future-agentic-workflows/)  
2. BoundaryML/baml: The AI framework that adds the engineering to prompt engineering (Python/TS/Ruby/Java/C\#/Rust/Go compatible) \- GitHub, accessed December 5, 2025, [https://github.com/BoundaryML/baml](https://github.com/BoundaryML/baml)  
3. Python \- Boundary Documentation \- BAML, accessed December 5, 2025, [https://docs.boundaryml.com/guide/installation-language/python](https://docs.boundaryml.com/guide/installation-language/python)  
4. Typescript \- Boundary Documentation \- BAML, accessed December 5, 2025, [https://docs.boundaryml.com/guide/installation-language/typescript](https://docs.boundaryml.com/guide/installation-language/typescript)  
5. Resource | dlt Docs \- dltHub, accessed December 5, 2025, [https://dlthub.com/docs/general-usage/resource](https://dlthub.com/docs/general-usage/resource)  
6. Your prompts are using 4x more tokens than you need | BAML Blog, accessed December 5, 2025, [https://boundaryml.com/blog/type-definition-prompting-baml](https://boundaryml.com/blog/type-definition-prompting-baml)  
7. Get structured output from a Language Model using BAML | Thomas Queste, accessed December 5, 2025, [https://www.tomsquest.com/blog/2024/08/get-structured-output-from-llm-using-baml/](https://www.tomsquest.com/blog/2024/08/get-structured-output-from-llm-using-baml/)  
8. Schemas | TanStack DB Docs, accessed December 5, 2025, [https://tanstack.com/db/latest/docs/guides/schemas](https://tanstack.com/db/latest/docs/guides/schemas)  
9. fabien0102/ts-to-zod: Generate zod schemas from typescript types/interfaces \- GitHub, accessed December 5, 2025, [https://github.com/fabien0102/ts-to-zod](https://github.com/fabien0102/ts-to-zod)  
10. generator \- Boundary Documentation \- BAML, accessed December 5, 2025, [https://docs.boundaryml.com/ref/baml/generator](https://docs.boundaryml.com/ref/baml/generator)  
11. Models \- Pydantic Validation, accessed December 5, 2025, [https://docs.pydantic.dev/latest/concepts/models/](https://docs.pydantic.dev/latest/concepts/models/)  
12. LanceDB | dlt Docs \- dltHub, accessed December 5, 2025, [https://dlthub.com/docs/dlt-ecosystem/destinations/lancedb](https://dlthub.com/docs/dlt-ecosystem/destinations/lancedb)  
13. LanceDB | Vector Database for RAG, Agents & Hybrid Search, accessed December 5, 2025, [https://lancedb.com/](https://lancedb.com/)  
14. Custom destination: Reverse ETL \- dlt Docs \- dltHub, accessed December 5, 2025, [https://dlthub.com/docs/dlt-ecosystem/destinations/destination](https://dlthub.com/docs/dlt-ecosystem/destinations/destination)  
15. FalkorDB/falkordblite: Embedded FalkorDB in a python module. \- GitHub, accessed December 5, 2025, [https://github.com/FalkorDB/falkordblite](https://github.com/FalkorDB/falkordblite)  
16. GRAPH.QUERY \- FalkorDB Docs, accessed December 5, 2025, [https://docs.falkordb.com/commands/graph.query.html](https://docs.falkordb.com/commands/graph.query.html)  
17. String Interning in Graph Databases: Save Memory, Boost Performance \- FalkorDB, accessed December 5, 2025, [https://www.falkordb.com/blog/string-interning-graph-database/](https://www.falkordb.com/blog/string-interning-graph-database/)  
18. Graphiti \- FalkorDB Docs, accessed December 5, 2025, [https://docs.falkordb.com/agentic-memory/graphiti.html](https://docs.falkordb.com/agentic-memory/graphiti.html)  
19. Adding Episodes \- Zep Documentation, accessed December 5, 2025, [https://help.getzep.com/graphiti/core-concepts/adding-episodes](https://help.getzep.com/graphiti/core-concepts/adding-episodes)  
20. @culur/generate-zod \- npm, accessed December 5, 2025, [https://www.npmjs.com/package/@culur/generate-zod](https://www.npmjs.com/package/@culur/generate-zod)  
21. TanStack/ai: SDK that enhances your applications with AI capabilities \- GitHub, accessed December 5, 2025, [https://github.com/TanStack/ai](https://github.com/TanStack/ai)  
22. Foundations: Tools \- AI SDK, accessed December 5, 2025, [https://ai-sdk.dev/docs/foundations/tools](https://ai-sdk.dev/docs/foundations/tools)  
23. Create new destination | dlt Docs \- dltHub, accessed December 5, 2025, [https://dlthub.com/docs/walkthroughs/create-new-destination](https://dlthub.com/docs/walkthroughs/create-new-destination)