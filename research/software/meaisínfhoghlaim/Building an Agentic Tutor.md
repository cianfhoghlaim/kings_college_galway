# **Architectural Blueprint for Autonomous Agentic Tutoring Systems: Integrating Hybrid Knowledge Graphs, Temporal Reasoning, and Generative UI Standards**

## **1\. Strategic Context and Domain Analysis**

The intersection of Large Language Models (LLMs) and educational technology has precipitated a fundamental shift in how automated tutoring systems are conceptualized. Traditional paradigms, largely predicated on static information retrieval or rule-based logic, fail to capture the nuance, temporal evolution, and structured complexity of advanced curricula such as the Irish Leaving Certificate. To construct an "Agentic Tutor" capable of generating high-fidelity mock exam resources in domains like Mathematics and Chemistry, architects must move beyond simple Retrieval-Augmented Generation (RAG). Instead, a composite architecture is required—one that synthesizes semantic understanding, temporal context, and generative user interfaces into a cohesive, interactive system.  
This report articulates a comprehensive technical specification for such a system. It leverages a cutting-edge stack comprising **CocoIndex** for intelligent data transformation, **Cloudflare R2** for durable asset storage, **LanceDB** for vector retrieval, **Graphiti** (backed by **FalkorDB**) for temporal knowledge graph reasoning, **Agno** for agent orchestration, and a sophisticated frontend layer utilizing **CopilotKit** and the **mcp-ui** standard.

### **1.1 The Domain Constraint: Educational Data Topology**

Data within the educational domain possesses a unique topology that resists naive processing. Unlike corporate wikis or customer support logs, educational artifacts—specifically syllabus documents, exam papers, and marking schemes—are characterized by rigid hierarchical structures, multimodal dependencies, and strict temporal validity.

#### **1.1.1 The Hierarchy of Assessment**

A standard Leaving Certificate Mathematics paper is not a flat text document. It is a structured artifact where meaning is derived from hierarchy:

* **The Paper:** Defined by Year, Level (Higher/Ordinary), and Season (June/deferred).  
* **The Section:** Section A (Concepts and Skills) vs. Section B (Contexts and Applications).  
* **The Question:** A discrete unit of assessment, often containing a preamble.  
* **The Sub-part:** Items (a), (b), (c) which often depend on prior results.

A retrieval system that chunks a PDF every 500 tokens risks severing the preamble from part (c), rendering the question unsolvable. Furthermore, marking schemes are separate documents that map 1:1 to these questions. An effective tutor must retrieve the question *and* its marking scheme simultaneously to provide feedback. This necessitates a **Smart Chunking Strategy** that respects document layout and semantic boundaries, a capability central to the **CocoIndex** framework.1

#### **1.1.2 Temporal Validity and Curriculum Drift**

Curricula are not static; they drift. A Chemistry question regarding a specific industrial process valid in 2010 may be removed from the syllabus in 2023\. A standard vector database, which retrieves based on semantic similarity, cannot inherently discern "valid" from "obsolete." It will happily retrieve the 2010 question if it matches the user's query vector. To prevent negative transfer of learning (teaching students obsolete material), the system requires a **Temporal Knowledge Graph**. **Graphiti**, with its episodic memory capabilities, allows us to tag knowledge with validity windows, ensuring the agent reasons about *current* syllabus alignment before generating content.3

### **1.2 The Interaction Shift: From Chat to Generative UI**

Text is a low-bandwidth medium for mathematics. Describing a geometry problem or a complex chemical equilibrium via text is cognitively taxing for the learner. The standard "Chatbot" interface is insufficient. The proposed architecture adopts a **Generative UI** paradigm. When a student requests a mock paper, the agent should not output Markdown text; it should render a structured, interactive React component—a "Mock Exam Widget"—complete with timers, expandable marking schemes, and LaTeX-rendered formulas. This is achieved through the **mcp-ui** specification and **CopilotKit**, which bridge the gap between the agent's structured reasoning and the frontend's visual capabilities.5

### **1.3 Architectural High-Level Design**

The system is bifurcated into two primary planes of operation: the **Ingestion Plane** (asynchronous, heavy processing) and the **Interaction Plane** (synchronous, real-time reasoning).  
**Table 1: Architectural Component Analysis**

| Layer | Component | Role | Justification |
| :---- | :---- | :---- | :---- |
| **Ingestion** | **CocoIndex** | ETL Orchestrator | Provides declarative "Dataflow" management, incremental processing, and robust handling of PDF-to-Markdown transformations.8 |
| **Storage** | **Cloudflare R2** | Object Store | Zero egress fees for serving source PDFs; S3-compatible API for easy integration.9 |
| **Vector** | **LanceDB** | Semantic Search | Serverless, high-performance vector retrieval for unstructured text similarity.2 |
| **Graph** | **Graphiti / FalkorDB** | Knowledge Graph | Bi-temporal modeling to handle curriculum changes; hybrid search (Graph \+ Vector).3 |
| **Agent** | **Agno** | Orchestration | Lightweight agent framework with native structured outputs and MCP support.12 |
| **Frontend** | **CopilotKit / mcp-ui** | Generative UI | Standardized protocol for rendering agent-generated UI components (mcp-ui) within a React app.5 |

## ---

**2\. The Ingestion Fabric: CocoIndex and Intelligent Transformation**

The efficacy of the Agentic Tutor is deterministically bound to the quality of its index. We utilize **CocoIndex** to define a rigorous ingestion pipeline that transforms raw PDFs into a dual-substrate knowledge base (Vector \+ Graph).

### **2.1 The Philosophy of Dataflow vs. ETL**

Traditional ETL (Extract, Transform, Load) scripts are often brittle and stateless. If the embedding model changes, the entire corpus must be re-processed. **CocoIndex** introduces a **Dataflow** programming model.8 In this paradigm, developers declare the *transformations* (e.g., "Chunk this text," "Embed this chunk"), and the engine manages the state. Crucially, CocoIndex supports **Incremental Processing**. If a new syllabus PDF is added to the source directory, CocoIndex detects the delta and processes only that file, propagating changes to LanceDB and FalkorDB without redundant computation. This is vital for maintaining a "live" tutor that can ingest new exam papers immediately upon publication.14

### **2.2 Configuring the Hierarchical Chunking Pipeline**

To address the structural constraints of exam papers, we implement a custom CocoIndex flow. This flow does not use simple character counts. Instead, it employs a **Parent-Child Chunking Strategy** powered by SplitRecursively and custom logic.

#### **2.2.1 Step 1: Ingestion and PDF Parsing**

The flow begins by ingesting raw PDFs. Standard text extraction often fails on multi-column layouts common in exam papers. We integrate a specialized parser (e.g., a custom function wrapping LlamaParse or similar) that converts PDF to Markdown, preserving headers (\# Question 1), tables, and LaTeX equations.

Python

\# Conceptual CocoIndex Flow Definition  
@cocoindex.flow\_def(name="LeavingCertIngestion")  
def ingestion\_flow(flow\_builder: cocoindex.FlowBuilder, data\_scope: cocoindex.DataScope):  
      
    \# 1\. Ingest Raw PDFs from Local Directory or S3  
    data\_scope\["pdfs"\] \= flow\_builder.add\_source(  
        cocoindex.sources.LocalFile(path="./syllabus\_data"),  
        refresh\_interval=timedelta(minutes=60)  
    )

    \# 2\. PDF Parsing & Preservation  
    \# Transformation to Markdown while preserving structure  
    with data\_scope\["pdfs"\].row() as pdf:  
        pdf\["markdown"\] \= pdf\["content"\].transform(  
            cocoindex.functions.Custom(  
                name="PdfToMarkdown",  
                executor=PdfLayoutParserExecutor, \# Custom executor logic  
                cache=True   
            )  
        )

#### **2.2.2 Step 2: Cloudflare R2 Asset Management**

A critical requirement is "referring back to their source." The agent must not just answer; it must provide a link to the original PDF. We utilize **Cloudflare R2** for this. Within the CocoIndex flow, we define a transformation that uploads the raw PDF to R2 and generates a stable URL.9  
The boto3 library is used within a custom CocoIndex function to handle this upload. Crucially, we generate **Presigned URLs** or public object URLs which are then stored as metadata alongside the text chunks.

Python

    \# 3\. Upload Original to Cloudflare R2  
    pdf\["r2\_url"\] \= pdf\["content"\].transform(  
        cocoindex.functions.Custom(  
            name="UploadToR2",  
            executor=R2UploaderExecutor,  
            bucket\_name="leaving-cert-source"  
        )  
    )

The R2UploaderExecutor would internally utilize the S3-compatible API:  
s3\_client.upload\_fileobj(io.BytesIO(content), 'bucket-name', 'key').  
Using R2 ensures that we avoid high egress fees associated with students downloading large PDF files repeatedly.9

#### **2.2.3 Step 3: Smart Hierarchical Chunking**

This is the most critical step for mathematical integrity. We utilize SplitRecursively but configure it with separators specific to the domain.17 For a Markdown-formatted exam paper, questions are typically denoted by headers or specific bold text patterns.

* **Level 1 Split:** Split by "Section" (e.g., \#\# Section A).  
* **Level 2 Split:** Split by "Question" (e.g., \#\#\# Question 1).  
* **Level 3 Split (Optional):** Split by sub-parts if the question is extremely long.

The key is that the **Child Chunk** (the text of Question 1\) must retain the metadata of its **Parent** (the Year, Subject, and Level).

Python

    \# 4\. Semantic Chunking  
    with data\_scope\["pdfs"\].row() as pdf:  
        pdf\["chunks"\] \= pdf\["markdown"\].transform(  
            cocoindex.functions.SplitRecursively(  
                language="markdown",  
                chunk\_size=1000,   
                chunk\_overlap=50,   
                separators=\["\#\# Question", "\#\#\# Part"\] \# Domain-specific separators  
            )  
        )

This strategy ensures that when the vector database returns a chunk, it is a complete question unit, not a fragmented paragraph.18

### **2.3 Indexing into LanceDB (The Semantic Layer)**

The chunks are then embedded and stored in **LanceDB**. LanceDB is chosen for its simplicity and serverless capabilities, allowing for embedded vector search without managing a heavy infrastructure cluster.2  
The embedding model selection is crucial. Standard models (like text-embedding-3-small) are adequate for text, but for Mathematics and Chemistry, models fine-tuned on scientific papers (like allenai/specter or mixedbread-ai/mxbai-embed-large-v1) often yield better results as they "understand" LaTeX tokens and chemical formulas better.  
The CocoIndex collector for LanceDB aggregates:

* vector: The embedding.  
* text: The markdown content.  
* metadata: { "year": 2023, "subject": "Chemistry", "topic": "Stoichiometry", "r2\_link": "https://..." }.

This metadata is extracted via Regex from the filename or via a lightweight LLM extraction step prior to chunking.

## ---

**3\. The Cognitive Architecture: Graphiti and FalkorDB**

While LanceDB handles semantic similarity ("Find questions *like* this one"), it lacks structural reasoning. It cannot easily answer: *"Give me the prerequisite topics for Calculus"* or *"Show me questions from 2019 that are no longer on the syllabus."* This requires a **Temporal Knowledge Graph**.

### **3.1 Graphiti: The Temporal Layer**

**Graphiti** is a Python library and MCP server designed specifically to build dynamic, temporally-aware knowledge graphs. It runs on top of **FalkorDB** (a high-performance graph database using GraphBLAS).3  
Graphiti introduces the concept of **Episodes**. An "Episode" is a discrete ingestion event (e.g., processing the "2023 Math Syllabus"). Graphiti tracks *when* this episode occurred. This allows the system to model the *evolution* of knowledge.

* **Fact:** "Matrices are on the syllabus." (Valid: 2010–2020).  
* **Fact:** "Matrices are removed." (Valid: 2021–Present).

When the agent queries the graph, Graphiti filters edges based on the current temporal context, preventing the "hallucination of pedagogy" where the AI suggests obsolete topics.4

### **3.2 Defining the Educational Ontology**

We must define a schema that Graphiti will enforce. This ontology maps the "Leaving Certificate" domain.  
**Nodes (Entities):**

1. **Concept:** A fundamental unit of knowledge (e.g., "Integration", "Moles").  
2. **LearningOutcome:** A specific syllabus requirement (e.g., "Derive the formula for...").  
3. **Question:** An instance of assessment.  
4. **ExamPaper:** A container entity.

**Edges (Relationships):**

1. (Question) \--\> (Concept)  
2. (Concept) \--\> (Concept)  
3. (Question) \--\> (ExamPaper)  
4. (Concept) \--\> (SyllabusSection)

### **3.3 Automating Graph Population with CocoIndex**

Populating this graph manually is impossible. We utilize **CocoIndex's ExtractByLlm** function to automate this.20 Within the pipeline, we pass the text of each question to an LLM with a specific prompt:  
"Analyze the following math question. Identify the mathematical concepts it tests based on the provided Syllabus Taxonomy. Extract the relationships."  
The output is a structured object (Pydantic model) representing the graph triples.

Python

\# Graph Extraction Logic  
@dataclasses.dataclass  
class ConceptRelation:  
    source: str  
    relation: str  
    target: str

\# In the CocoIndex Flow  
pdf\["graph\_data"\] \= pdf\["markdown"\].transform(  
    cocoindex.functions.ExtractByLlm(  
        llm\_spec=cocoindex.LlmSpec(model="gpt-4o"),  
        output\_type=list,  
        instruction="Extract triples: (Question) \-\> \-\> (Topic)"  
    )  
)

These triples are then exported to **FalkorDB**. CocoIndex supports exporting to graph databases by mapping the extracted fields to Node Labels and Edge Types.21

### **3.4 Hybrid Search Strategy**

The true power of this architecture lies in **Hybrid Search**. The Graphiti MCP server exposes tools like search\_facts and search\_nodes. The Agno agent utilizes these in conjunction with LanceDB.23

* **Query:** "Create a mock paper on Organic Chemistry."  
* **Graph Step:** search\_nodes("Organic Chemistry") in FalkorDB identifies the central node. The agent then traverses \<-- edges to find all Question IDs linked to this topic.  
* **Vector Step:** The agent uses the text of these identified questions to query LanceDB for *other* questions that might be semantically related (e.g., "Esterification") but missed the explicit tag in the graph.  
* **Result:** A comprehensive, verified set of questions.

## ---

**4\. The Agentic Control Plane: Agno (Phidata)**

**Agno** (formerly Phidata) serves as the orchestration layer. It is the "Brain" that decides *when* to search the vector DB, *when* to traverse the graph, and *how* to format the final response.12

### **4.1 Configuring the Agentic Tutor**

We define an Agent in Agno specifically tuned for the tutoring role.

Python

from agno.agent import Agent  
from agno.models.openai import OpenAIChat  
from agno.tools.mcp import MCPTools 

\# Connect to Graphiti MCP Server  
graphiti\_tool \= MCPTools(  
    transport="sse",   
    url="http://localhost:8000/sse" \# Graphiti Docker Container  
)

tutor\_agent \= Agent(  
    name="LeavingCertTutor",  
    model=OpenAIChat(id="gpt-4o"),  
    instructions=,  
    tools=\[graphiti\_tool, lancedb\_search\_tool\],  
    response\_model=MockExamPaperSchema, \# Enforce structured output  
    markdown=True  
)

### **4.2 Structured Output and Pydantic Schemas**

A key requirement is generating resources like mock exam papers. Text output is insufficient for this; we need structured data that the UI can render. Agno's response\_model parameter enforces strict adherence to a Pydantic schema.24  
**The Mock Exam Schema:**

Python

class QuestionItem(BaseModel):  
    id: str  
    content: str \# Markdown/LaTeX  
    marks: int  
    topic\_tags: List\[str\]  
    source\_ref: str \# URL to R2 PDF  
    solution\_summary: str

class MockExamPaper(BaseModel):  
    title: str  
    total\_marks: int  
    duration\_min: int  
    sections: List \# Contains list of QuestionItems

By forcing the LLM to output this structure, we guarantee that the frontend receives a valid JSON object, preventing "UI hallucinations" where the renderer crashes due to malformed data.

### **4.3 Agentic Reasoning Patterns**

The agent utilizes a **ReAct** (Reasoning \+ Acting) loop.

1. **Thought:** "The user wants a mock paper on Algebra. I need to check which Algebra topics are on the Higher Level syllabus."  
2. **Action:** Call graphiti.search\_nodes(query="Algebra syllabus").  
3. **Observation:** "Algebra includes Complex Numbers, Inequalities, and Logs."  
4. **Thought:** "Now I need questions for these topics from the last 5 years."  
5. **Action:** Call lancedb.search(query="Complex Numbers exam question", filter="year \> 2018").  
6. **Synthesis:** The agent collates these items into the MockExamPaper structure.

## ---

**5\. The Interaction Layer: CopilotKit and mcp-ui**

The user experience (UX) differentiates this system from a simple search engine. We employ **Generative UI** to render the mock exam not as text, but as an interactive application component.

### **5.1 The mcp-ui Standard**

**mcp-ui** is an extension of the Model Context Protocol that allows agents to return UI components. A tool call can return a UIResource which points to a definition of a visual element.5  
The core concept is the ui:// URI scheme.

* **Agent Response:** instead of text, the agent returns {"type": "resource", "resource": {"uri": "ui://components/mock-exam-widget", "mimeType": "application/json", "blob": "..."}}.  
* **Host (Frontend):** detects this resource and renders the corresponding component.

### **5.2 CopilotKit Integration**

**CopilotKit** acts as the bridge between the React frontend and the Agentic backend. It manages the context (chat history, application state) and the streaming of responses.6  
We utilize **CopilotKit's Generative UI** capabilities (specifically the AG-UI protocol, which is interoperable with mcp-ui concepts) to render the exam paper.  
Frontend Implementation (Next.js):  
We define a useCopilotAction hook. This hook listens for the specific "tool call" from the Agno agent that signifies a mock paper has been generated.

TypeScript

// Frontend: Defining the Generative UI Action  
useCopilotAction({  
  name: "renderMockExam",  
  description: "Displays the generated mock exam paper to the student",  
  parameters:,  
  render: (props) \=\> {  
    // This component renders the structured JSON into a beautiful UI  
    return \<MockExamPaperComponent paper={props.args} /\>;  
  }  
});

### **5.3 mcp-ui-on-tanstack: The Fetching Pattern**

To efficiently manage the data flow for these UI components, we adopt the **mcp-ui-on-tanstack** pattern.25 This involves using **TanStack Query** (React Query) to handle the fetching and caching of the UI resources.  
When the agent cites a resource (e.g., a specific graph diagram or a syllabus table), the frontend component uses a useQuery hook:  
useQuery(\['mcp', 'resource', uri\], fetchResource).  
**Benefits:**

1. **Caching:** If the user generates a paper, navigates away, and returns, the paper is re-rendered instantly from the cache.  
2. **Optimistic Updates:** The UI can show a "Skeleton Loader" while the agent is streaming the complex JSON payload.  
3. **Separation of Concerns:** The fetching logic is decoupled from the rendering logic.

### **5.4 Rendering Mathematics (LaTeX) and Security**

Rendering math is non-negotiable. The MockExamPaperComponent utilizes **KaTeX** or **MathJax** to render the LaTeX strings provided by the agent.26  
Security Criticality:  
If we were strictly following the mcp-ui iframe specification, we would be rendering untrusted HTML (generated by an LLM) inside our app. This presents XSS risks.

* **Mitigation 1 (Sandboxing):** We must use the sandbox attribute on iframes: sandbox="allow-scripts". We explicitly **exclude** allow-same-origin to prevents the iframe from accessing the parent's cookies or local storage.27  
* **Mitigation 2 (Declarative UI):** Ideally, we rely on CopilotKit's approach where the agent returns *JSON Data*, not *HTML Strings*. The React component then maps this data to secure, pre-built UI elements. This mitigates XSS risks significantly compared to rendering raw HTML blobs.

## ---

**6\. End-to-End Workflow: The "Mock Paper" Scenario**

To visualize the system in action, we trace a complete transaction.  
Step 1: The User Intent  
The student types: "I'm weak on Calculus. Generate a 50-mark mock paper focusing on differentiation, similar to the 2022 paper."  
**Step 2: Agentic Reasoning (Agno)**

* The Agno agent receives the prompt. It parses "Calculus" and "Differentiation".  
* **Tool Call 1 (Graphiti):** search\_nodes("Differentiation"). The graph returns related nodes: "Product Rule", "Quotient Rule", "Chain Rule".  
* **Tool Call 2 (FalkorDB):** search\_facts(topic="Differentiation", year="2022"). The graph returns the specific question IDs from 2022 that tested this.  
* **Tool Call 3 (LanceDB):** search(vector="Differentiation Exam Question", filter="level='Higher'"). This retrieves the *text* and *vectors* of similar questions from other years (e.g., 2019, 2021).

Step 3: Content Assembly  
The agent selects 3 questions. It structures them into Section A (Short) and Section B (Long).

* It verifies the marks sum to 50\.  
* It appends the r2\_source\_link (e.g., https://r2.cloudflare.com/exams/2019-maths-p1.pdf) to each question.

**Step 4: Generative UI Delivery**

* The agent outputs the MockExamPaper JSON structure via the renderMockExam tool.  
* **CopilotKit** on the client intercepts this.  
* The **mcp-ui** renderer activates. It displays a "Generating Paper..." skeleton.  
* As the JSON streams in, the UI updates in real-time.  
* The final result is a polished, interactive exam paper card. The student sees formatted Math ($f(x) \= x^2$), a timer, and a "Source" button.

Step 5: Reference Verification  
The student clicks "Source". The frontend uses the r2\_source\_link to open the original PDF in a side panel, centered exactly on the relevant page (using PDF fragment identifiers, e.g., \#page=4). This closes the trust loop.

## ---

**7\. Operational Assurance and Second-Order Insights**

### **7.1 "Syllabus Drift" and Temporal Hygiene**

A hidden danger in EdTech AI is "Syllabus Drift." A question valid in 2015 might use terminology or methods now deprecated.

* **Implication:** The Knowledge Graph is not just a retrieval mechanism; it is a **Validity Filter**.  
* **Mechanism:** Every node in Graphiti has valid\_from and valid\_to properties. The Agent's system prompt includes a directive: *"Filter all candidate questions against the active syllabus epoch (2025)."* This prevents the agent from surfacing obsolete content, a common failure mode in pure vector-RAG systems.

### **7.2 The Hallucination of Pedagogy**

LLMs are good at text, but bad at "Scaffolding" (ordering questions from easy to hard).

* **Insight:** We encode pedagogical rules into the graph. (Concept A) \--\> (Concept B).  
* **Application:** When generating a paper, the Agent is instructed to traverse this dependency tree. Question 1 must test a "Root" concept (e.g., Algebra basics) before Question 2 tests a "Leaf" concept (e.g., Complex Numbers depending on Algebra). This ensures the mock paper flows logically, mimicking a human setter.

### **7.3 Latency vs. Accuracy Trade-off**

Hybrid search (Graph \+ Vector \+ Reasoning) is computationally expensive. Generating a full paper might take 30-60 seconds.

* **Architecture Mitigation:** We implement **Streaming Generative UI**. The MockExamPaper component is designed to render *incrementally*. As soon as Question 1 is generated, it appears on screen. The student can start reading/solving Q1 while the agent is still computing Q2 and Q3 in the background. This perceived latency reduction is critical for user retention.

### **7.4 Security Implications of Generated Content**

Allowing an LLM to dictate UI structure creates a theoretical attack vector where an LLM (via prompt injection) could try to render a "Fake Login Form" to steal credentials.

* **Defense:** We implement a **Strict Content Security Policy (CSP)**. The mcp-ui components are rendered in isolated containers. If using iframes, we use the sandbox attribute to block forms and same-origin access.28 Furthermore, the MockExamPaperSchema is rigid; the agent can inject text/latex, but it cannot inject arbitrary HTML tags (\<script\>, \<input\>) because the React component controls the rendering, treating agent output purely as data strings.

## ---

**8\. Conclusion**

The architecture defined in this report represents a sophisticated convergence of modern AI engineering patterns. It moves beyond the "Chatbot" model to create an **Agentic Application**.  
By utilizing **CocoIndex**, we solve the "messy data" problem of educational PDFs through robust, incremental, hierarchical ingestion. **FalkorDB and Graphiti** provide the necessary "Temporal Brain," allowing the system to understand the evolution of the curriculum and avoid logical/pedagogical hallucinations. **LanceDB** provides the semantic flexibility to match student intent. Finally, **Agno** orchestrates this symphony, while **CopilotKit** and **mcp-ui** render the output in a format that respects the visual and interactive nature of learning.  
This system is not merely a retrieval engine; it is a curriculum-aware, pedagogically sound, autonomous tutor capable of supporting high-stakes exam preparation with a level of fidelity previously unattainable in automated systems. The integration of source-referencing (via **Cloudflare R2**) ensures that while the AI provides the guidance, the "Source of Truth" remains firmly anchored in the official curriculum, building the essential trust required for educational adoption.

#### **Works cited**

1. How I Built a Semantic Search Engine with CocoIndex \- DEV Community, accessed December 13, 2025, [https://dev.to/cocoindex/how-i-built-a-semantic-search-engine-with-cocoindex-5ak9](https://dev.to/cocoindex/how-i-built-a-semantic-search-engine-with-cocoindex-5ak9)  
2. CocoIndex Flow Definition, accessed December 13, 2025, [https://cocoindex.io/docs/core/flow\_def](https://cocoindex.io/docs/core/flow_def)  
3. Graphiti \- FalkorDB Docs, accessed December 13, 2025, [https://docs.falkordb.com/agentic-memory/graphiti.html](https://docs.falkordb.com/agentic-memory/graphiti.html)  
4. Overview | Zep Documentation, accessed December 13, 2025, [https://help.getzep.com/graphiti/getting-started/overview](https://help.getzep.com/graphiti/getting-started/overview)  
5. MCP-UI Just Gave MCP a Frontend \- Medium, accessed December 13, 2025, [https://medium.com/@kenzic/mcp-ui-just-gave-mcp-a-frontend-aea0ebc02253](https://medium.com/@kenzic/mcp-ui-just-gave-mcp-a-frontend-aea0ebc02253)  
6. Introduction to CopilotKit, accessed December 13, 2025, [https://docs.copilotkit.ai/](https://docs.copilotkit.ai/)  
7. MCP Apps: Bringing Interactive UIs to AI Conversations \- fka.dev, accessed December 13, 2025, [https://blog.fka.dev/blog/2025-11-22-mcp-apps-101-bringing-interactive-uis-to-ai-conversations/](https://blog.fka.dev/blog/2025-11-22-mcp-apps-101-bringing-interactive-uis-to-ai-conversations/)  
8. CocoIndex, accessed December 13, 2025, [https://cocoindex.io/](https://cocoindex.io/)  
9. boto3 · Cloudflare R2 docs, accessed December 13, 2025, [https://developers.cloudflare.com/r2/examples/aws/boto3/](https://developers.cloudflare.com/r2/examples/aws/boto3/)  
10. cocoindex \- PyPI, accessed December 13, 2025, [https://pypi.org/project/cocoindex/](https://pypi.org/project/cocoindex/)  
11. Graphiti MCP Server \- FalkorDB Docs, accessed December 13, 2025, [https://docs.falkordb.com/agentic-memory/graphiti-mcp-server.html](https://docs.falkordb.com/agentic-memory/graphiti-mcp-server.html)  
12. agno-agi/agno: The unified stack for multi-agent systems. \- GitHub, accessed December 13, 2025, [https://github.com/agno-agi/agno](https://github.com/agno-agi/agno)  
13. Easily Build a Frontend for your AWS Strands Agents using AG-UI in 30 minutes, accessed December 13, 2025, [https://dev.to/copilotkit/easily-build-a-frontend-for-your-aws-strands-agents-using-ag-ui-in-30-minutes-42ji](https://dev.to/copilotkit/easily-build-a-frontend-for-your-aws-strands-agents-using-ag-ui-in-30-minutes-42ji)  
14. CocoIndex Changelog 2025-10-19, accessed December 13, 2025, [https://cocoindex.io/blogs/cocoindex-changelog-2025-10-19](https://cocoindex.io/blogs/cocoindex-changelog-2025-10-19)  
15. Live Updates \- CocoIndex, accessed December 13, 2025, [https://cocoindex.io/docs/tutorials/live\_updates](https://cocoindex.io/docs/tutorials/live_updates)  
16. Presigned URLs · Cloudflare R2 docs, accessed December 13, 2025, [https://developers.cloudflare.com/r2/api/s3/presigned-urls/](https://developers.cloudflare.com/r2/api/s3/presigned-urls/)  
17. Academic Papers Indexing \- CocoIndex, accessed December 13, 2025, [https://cocoindex.io/docs/examples/academic\_papers\_index](https://cocoindex.io/docs/examples/academic_papers_index)  
18. Want AI to Actually Understand Your Code? This Tool Says It Can Help | HackerNoon, accessed December 13, 2025, [https://hackernoon.com/want-ai-to-actually-understand-your-code-this-tool-says-it-can-help](https://hackernoon.com/want-ai-to-actually-understand-your-code-this-tool-says-it-can-help)  
19. Graphiti MCP Server: The Definitive Guide to Building Agentic Memory \- Skywork.ai, accessed December 13, 2025, [https://skywork.ai/skypage/en/graphiti-mcp-server-agentic-memory/1978662683507544064](https://skywork.ai/skypage/en/graphiti-mcp-server-agentic-memory/1978662683507544064)  
20. CocoIndex \+ Kuzu: Real-time knowledge graph with Kuzu, accessed December 13, 2025, [https://cocoindex.io/blogs/kuzu-integration](https://cocoindex.io/blogs/kuzu-integration)  
21. Neo4j | CocoIndex, accessed December 13, 2025, [https://cocoindex.io/docs/targets/neo4j](https://cocoindex.io/docs/targets/neo4j)  
22. Targets \- CocoIndex, accessed December 13, 2025, [https://cocoindex.io/docs/targets](https://cocoindex.io/docs/targets)  
23. Graphiti MCP Server \- LobeHub, accessed December 13, 2025, [https://lobehub.com/mcp/getzep-graphiti\_mcp](https://lobehub.com/mcp/getzep-graphiti_mcp)  
24. Structured outputs \- Agno, accessed December 13, 2025, [https://docs.agno.com/faq/structured-outputs](https://docs.agno.com/faq/structured-outputs)  
25. Turning documentation into action with Model Context Protocol (MCP) | by Steliana Vassileva | Nov, 2025, accessed December 13, 2025, [https://medium.com/@steliana.vassileva/turning-documentation-into-action-with-model-context-protocol-mcp-servers-274d2df85e02](https://medium.com/@steliana.vassileva/turning-documentation-into-action-with-model-context-protocol-mcp-servers-274d2df85e02)  
26. LaTeX \- assistant-ui, accessed December 13, 2025, [https://www.assistant-ui.com/docs/guides/Latex](https://www.assistant-ui.com/docs/guides/Latex)  
27. Play safely in sandboxed IFrames | Articles \- web.dev, accessed December 13, 2025, [https://web.dev/articles/sandboxed-iframes](https://web.dev/articles/sandboxed-iframes)  
28. Is it safe to have sandbox="allow-scripts allow-popups allow-same-origin" on  
29. How to securely load user genereated Javascript code from IFrame into my website?, accessed December 13, 2025, [https://security.stackexchange.com/questions/280141/how-to-securely-load-user-genereated-javascript-code-from-iframe-into-my-website](https://security.stackexchange.com/questions/280141/how-to-securely-load-user-genereated-javascript-code-from-iframe-into-my-website)