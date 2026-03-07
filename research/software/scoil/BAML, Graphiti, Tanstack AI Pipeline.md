# **ARCHITECTURAL CONVERGENCE FOR DETERMINISTIC AGENTIC SYSTEMS: INTEGRATING BAML, GRAPHITI, AND TANSTACK AI WITHIN THE IRISH EDUCATIONAL DOMAIN**

## **1\. Introduction: The Imperative for Deterministic AI Architectures**

The rapid ascendancy of Large Language Models (LLMs) has fundamentally altered the software engineering landscape, transitioning development paradigms from explicit instruction coding to intent-based probabilistic modeling. However, this transition has introduced a critical "impedance mismatch" between the stochastic nature of generative AI—which excels at creative, unstructured text generation—and the rigid, deterministic requirements of enterprise application logic and database schemas. In high-stakes domains such as educational certification and academic planning, where data integrity is paramount, the tolerance for hallucination or structural malformation is effectively zero.  
This report presents a comprehensive architectural analysis of a novel end-to-end pipeline designed to resolve these friction points. The architecture integrates three cutting-edge technologies: **BAML (Boundary AI Markup Language)** for deterministic "schema engineering" and fault-tolerant parsing; **Graphiti** for the management of temporal knowledge graphs and dynamic state; and **TanStack AI** (specifically within the TanStack Start framework) for isomorphic, type-safe full-stack application development.  
The analysis is grounded in a specific, complex domain: the **Irish Education System**, encompassing the Leaving Certificate examination, the Central Applications Office (CAO) points mechanism, and university matriculation requirements. This domain was selected because it presents a rigorous test case for AI systems. It requires not only the extraction of structured data (grades, subjects, levels) but also the maintenance of state over time (a student's progress through the two-year Senior Cycle) and the precise execution of logic based on evolving rules.1  
A primary focus of this research is the resolution of the interoperability challenge between BAML and the frontend ecosystem. While BAML provides superior handling of LLM outputs via its generated TypeScript interfaces, libraries like TanStack AI require runtime validation schemas—specifically **Zod**—to define tool inputs. The report identifies and details architectural patterns to bridge this gap, ensuring a seamless flow of type information from the prompt engineering layer to the user interface.  
By synthesizing these technologies, we propose a "Schema-First, Temporally-Aware" architecture. This approach moves beyond the limitations of standard Retrieval-Augmented Generation (RAG) and fragile JSON-mode prompting, offering a robust blueprint for the next generation of intelligent educational agents.

## ---

**2\. The Deterministic Interface: BAML and Schema Engineering**

The foundational layer of the proposed architecture is BAML (Boundary AI Markup Language). To understand its necessity, one must first analyze the failure modes of current LLM interaction patterns. Standard approaches rely on "prompt engineering," where developers craft English instructions hoping to coerce a model into outputting valid JSON. This is inherently fragile. Models frequently output "JSON-like" strings that fail strict parsing (e.g., missing quotes, trailing commas) or hallucinate keys not present in the requested schema.3

### **2.1 The Philosophy of BAML: Prompts as Functions**

BAML represents a paradigm shift from prompt engineering to "schema engineering." It treats the interaction with an LLM not as a conversational exchange but as a function call with a strictly defined signature. A BAML function, defined in a .baml file, declares its input parameters and its return type explicitly. The BAML compiler then handles the translation of this signature into the prompt logic required by the specific model (e.g., OpenAI, Anthropic).5  
This approach addresses the core entropy problem of LLMs. By defining the output structure using a Domain-Specific Language (DSL) that mirrors TypeScript or Pydantic, BAML restricts the search space of the model. More critically, it employs **Schema-Aligned Parsing (SAP)**. Unlike a standard JSON.parse() which fails catastrophically on a single syntax error, SAP is a fault-tolerant, rule-based parsing engine designed specifically for the idiosyncrasies of LLM token generation. It allows the system to recover valid structured data from imperfect model outputs in milliseconds, eliminating the need for costly and slow retry loops.3

### **2.2 Deep Research: Bridging BAML and Zod**

The user's query specifically prioritizes identifying a mechanism to derive **Zod schemas** quickly from BAML classes. This is a non-trivial engineering challenge because BAML and Zod serve different stages of the application lifecycle.  
**The Lifecycle Disconnect:**

* **BAML (Compile Time/Generation):** BAML is designed to define the *output* structure of an LLM. Its compiler generates static types (e.g., TypeScript interfaces, Python classes) to ensure that the code consuming the LLM response is type-safe. It does *not* natively output runtime validation schemas like Zod or Pydantic models by default, as its internal SAP parser handles the validation logic.3  
* **TanStack AI / Zod (Runtime/Validation):** Conversely, frameworks like TanStack AI require schema definitions available at *runtime* to validate incoming HTTP requests or function arguments. Specifically, the toolDefinition and createServerFnTool functions in TanStack AI demand a z.object(...) schema to define the shape of valid inputs for a tool.7

**The Gap:** A developer using BAML has perfect TypeScript interfaces for their data (e.g., interface StudentProfile), but TanStack AI requires a const StudentProfileSchema \= z.object(...). Keeping these two definitions in sync manually is a violation of the DRY (Don't Repeat Yourself) principle and a source of potential bugs.

#### **2.2.1 Strategy A: The "Interface-Implements" Pattern (Recommended)**

Research into advanced TypeScript patterns and the specific constraints of the BAML ecosystem suggests that the most robust method—short of a custom compiler plugin—is the "Interface-Implements" pattern. This leverages TypeScript's structural typing to force the Zod schema to conform to the BAML-generated interface.  
In this pattern, the developer generates the BAML client standardly. Then, when defining the Zod schema for TanStack AI, a utility type is employed to ensure that the inferred type of the Zod schema is assignable to the BAML interface.  
**Table 1: The BAML-Zod Synchronization Matrix**

| Feature | BAML Generated Type | Zod Schema Requirement | Synchronization Mechanism |
| :---- | :---- | :---- | :---- |
| **Nature** | Static Interface (interface) | Runtime Object (z.object) | z.infer\<typeof Schema\> |
| **Usage** | Compile-time checking | Runtime validation | Implements\<Interface, Schema\> |
| **Source** | .baml file compilation | Manual definition / Generator | Type Assertion |
| **Handling** | SAP (Fuzzy Parsing) | Strict Validation | Zod transformations |

**Technical Implementation:**

TypeScript

// 1\. Import the generated BAML interface  
// This is created automatically by running \`baml-cli generate\`  
import { LeavingCertSubject } from '@/baml\_client/types';  
import { z } from 'zod';

// 2\. Define a Utility Type for Strict Compliance  
// This type ensures that 'Schema' produces a type that strictly matches 'Model'  
type Implements\<Model, Schema\> \= Schema extends z.ZodType\<Model\>? Schema : never;

// 3\. Define the Zod Schema  
// If the BAML definition changes (e.g., a new field is added),  
// this code will throw a TypeScript error, forcing the developer to update the Zod schema.  
export const LeavingCertSubjectSchema: Implements\<LeavingCertSubject, typeof schema\> \= z.object({  
  subject: z.string(),  
  // BAML Enums must be mapped to Zod Enums or Unions  
  level: z.enum(\["Higher", "Ordinary", "Foundation"\]),   
  grade: z.string().optional(),  
  is\_bonus\_math: z.boolean()  
});

const schema \= LeavingCertSubjectSchema;

This strategy aligns with the "schema-first" philosophy. The BAML file remains the single source of truth for the domain model. Any divergence in the runtime Zod schema is caught immediately at compile time.9

#### **2.2.2 Strategy B: Automated Generation via Custom Scripts**

For large-scale applications where manual synchronization is prohibitive, a more advanced approach involves analyzing the BAML Abstract Syntax Tree (AST) or parsing the .baml files directly to generate Zod definitions. While baml-cli does not currently output Zod, the .baml syntax is concise and C-like, making it amenable to simple regex-based transpilation or parsing via a custom script.  
A "BAML-to-Zod" generator script would perform the following mappings:

* **BAML class** $\\rightarrow$ **Zod z.object()**  
* **BAML enum** $\\rightarrow$ **Zod z.enum()**  
* **BAML string** $\\rightarrow$ **Zod z.string()**  
* **BAML int** $\\rightarrow$ **Zod z.number().int()**  
* **BAML T** $\\rightarrow$ **Zod z.array(T)**  
* **BAML T?** $\\rightarrow$ **Zod z.nullable().optional()**

This automation would be integrated into the build pipeline: baml-cli generate && node scripts/generate-zod.js. This ensures that the frontend validation logic (Zod) is always an exact derivative of the AI extraction logic (BAML).6

## ---

**3\. Domain Modeling: The Irish Education System**

To demonstrate the efficacy of this pipeline, we apply it to the Irish Leaving Certificate. This domain is characterized by a complex set of rules governing subject levels, grading bands, and points calculation. It is an ideal test case because the data is highly structured but the rules for interpreting it are rigid—a probabilistic LLM guessing a grade is unacceptable; it must be precise.1

### **3.1 The Ontology of the Leaving Certificate**

The Irish system operates on a rigorous ontology. Students take a set of subjects (typically 6-8). Each subject is taken at a specific **Level**: Higher, Ordinary, or Foundation. The grade achieved falls into a specific **Band** (H1-H8 or O1-O8).  
**Crucial Domain Rules:**

1. **Grading Scale:** Since 2017, grades are strictly H1-H8 and O1-O8. Previous systems used A1, B2, etc. An LLM trained on older data might hallucinate "B1". The schema must enforce the new standard.1  
2. **Points Calculation:** Only the best six subjects count.  
3. **Math Bonus:** A grade of H6 or higher in Mathematics awards 25 bonus points. This is a conditional logic rule that the system must handle.2  
4. **Matriculation:** Specific universities (NUI) require Irish and a third language. Engineering courses often require Higher Level Math and a Laboratory Science.12

### **3.2 BAML Class Definitions for Irish Education**

The following BAML definitions encode these domain rules directly into the schema. By using BAML's @description decorator, we provide semantic hints to the LLM, improving extraction accuracy for domain-specific terms.  
**File: baml\_src/irish\_education.baml**

Rust

// 1\. Enums for Strict Value Control  
// Using enums prevents the LLM from outputting "Honours" instead of "Higher"  
// or "A1" instead of "H1".

enum SubjectLevel {  
  Higher @description("Honours level papers")  
  Ordinary @description("Pass level papers")  
  Foundation @description("Basic level, available for Math and Irish only")  
}

enum GradeBand {  
  // Higher Level Bands  
  H1 @description("90-100% \- Worth 100 points")  
  H2 @description("80-89% \- Worth 88 points")  
  H3 @description("70-79% \- Worth 77 points")  
  H4 @description("60-69% \- Worth 66 points")  
  H5 @description("50-59% \- Worth 56 points")  
  H6 @description("40-49% \- Worth 46 points")  
  H7 @description("30-39% \- Worth 37 points")  
  H8 @description("0-29% \- Worth 0 points")  
    
  // Ordinary Level Bands  
  O1 @description("90-100% \- Worth 56 points")  
  O2 @description("80-89% \- Worth 46 points")  
  O3 @description("70-79% \- Worth 37 points")  
  O4 @description("60-69% \- Worth 28 points")  
  O5 @description("50-59% \- Worth 20 points")  
  O6 @description("40-49% \- Worth 12 points")  
  O7 @description("30-39% \- Worth 0 points")  
  O8 @description("0-29% \- Worth 0 points")  
}

enum SubjectGroup {  
  Core @description("Mandatory subjects: Math, English, Irish")  
  Science @description("Biology, Chemistry, Physics, Ag Science")  
  Business @description("Business, Accounting, Economics")  
  Humanities @description("History, Geography, Classical Studies")  
  Languages @description("French, German, Spanish, etc.")  
  Artistic @description("Art, Music")  
  Applied @description("Construction, Engineering, Technology")  
}

// 2\. Class Definitions  
// These classes structure the data extracted from the student's input.

class LeavingCertSubject {  
  name string @description("The official subject name, e.g., 'Gaeilge' not 'Irish'")  
  level SubjectLevel  
  group SubjectGroup  
  current\_grade GradeBand? @description("The grade currently achieved or predicted")  
  target\_grade GradeBand? @description("The grade the student aims for")  
  is\_bonus\_math bool @description("True only if Subject is Math and Level is Higher")  
}

class StudentProfile {  
  student\_id string?  
  academic\_year string @description("e.g., '5th Year', '6th Year'")  
  subjects LeavingCertSubject  
  total\_cao\_points int? @description("Calculated points based on best 6 subjects including bonus")  
  matriculation\_status string @description("List of potential universities qualified for")  
}

// 3\. Extraction Function  
// This function signature defines the contract for the LLM.

function ExtractStudentData(convo\_text: string) \-\> StudentProfile {  
  client GPT4o  
  prompt \#"  
    Analyze the following student text regarding their Leaving Certificate.  
    Extract the subject details, levels, and grades strictly according to the schema.  
    If the student mentions "Honours Math", map it to Level: Higher.  
      
    Student Input:  
    {{ convo\_text }}  
      
    {{ ctx.output\_format }}  
  "\#  
}

### **3.3 Data Representation: CAO Points Table**

The logic for calculating points must be precise. While the LLM *can* estimate points, a robust system would use the extracted BAML data to calculate the points deterministically in code (TypeScript/Python) rather than relying on the LLM's arithmetic. The BAML extraction ensures the input to this calculation is valid.  
**Table 2: Official CAO Points Mapping (Standard vs Math Bonus)**

| Grade | % Range | Higher Level Points | Ordinary Level Points | Higher Math (w/ Bonus) |
| :---- | :---- | :---- | :---- | :---- |
| **H1 / O1** | 90 \- 100 | 100 | 56 | 125 |
| **H2 / O2** | 80 \- 89 | 88 | 46 | 113 |
| **H3 / O3** | 70 \- 79 | 77 | 37 | 102 |
| **H4 / O4** | 60 \- 69 | 66 | 28 | 91 |
| **H5 / O5** | 50 \- 59 | 56 | 20 | 81 |
| **H6 / O6** | 40 \- 49 | 46 | 12 | 71 |
| **H7 / O7** | 30 \- 39 | 37 | 0 | 37 (No Bonus) |
| **H8 / O8** | 0 \- 29 | 0 | 0 | 0 |

This table illustrates the discrete nature of the data. BAML ensures that a student saying "I got a generic B" generates a null or a request for clarification, rather than guessing "H3".11

## ---

**4\. Temporal Memory: The Graphiti Integration**

The second pillar of the architecture is memory. Educational data is inherently temporal. A student's proficiency is not a static fact; it is a trajectory. Standard RAG systems flatten this history, retrieving documents based on semantic similarity regardless of *when* they were relevant. Graphiti addresses this by implementing a **Bi-Temporal Knowledge Graph**.13

### **4.1 Temporal Graph Theory**

Graphiti differentiates between two timelines:

1. **Valid Time:** The time period during which a fact is true in the real world (e.g., "Student took Higher Level Math from Sept 2023 to Jan 2024").  
2. **Transaction Time:** The time at which the system recorded the fact.

This allows the agent to model state transitions. If a student drops from Higher Level to Ordinary Level Math—a common occurrence in the Irish system known as "dropping down"—Graphiti does not merely overwrite the Level attribute. Instead, it creates a new edge with a new valid time start date and closes the valid time interval of the previous edge.  
**Graph Structure Visualization:**

* **Node:** Student (ID: 123\)  
* **Node:** Subject (Name: Mathematics)  
* **Edge 1 (Expired):** \`\`  
* **Edge 2 (Active):** \`\`

This structure enables the AI to answer longitudinal queries: "How has my potential CAO score changed since the start of the year?" A standard SQL or Vector DB approach would struggle to reconstruct this history without complex, manual audit tables.14

### **4.2 Integration Architecture: The Node-Python Bridge**

A significant implementation detail is that Graphiti is a Python-native library, while the target frontend (TanStack) is typically React/Node.js. This necessitates a "Sidecar" or Microservices architecture.  
The Graphiti Service:  
The optimal deployment strategy is to run Graphiti as a standalone service (using FastAPI or the provided Docker container) that exposes a REST or MCP (Model Context Protocol) interface. The TanStack application then communicates with this service via HTTP.  
**Workflow:**

1. **TanStack Server Function** receives the extracted BAML data.  
2. It formats this data into a Graphiti "Episode" (a discrete unit of knowledge).  
3. It posts this Episode to the Python Graphiti service.  
4. Graphiti ingests the episode, performs entity resolution (deduplicating "Maths" vs "Mathematics"), and updates the temporal graph.16

## ---

**5\. The Integration Layer: TanStack AI and Full-Stack Isomorphism**

The final piece of the pipeline is the execution environment. **TanStack Start** provides a full-stack framework where server-side logic and client-side UI are co-located but distinct. **TanStack AI** acts as the glue, providing a standardized way to define tools that the AI can invoke.

### **5.1 The createServerFnTool Primitive**

The createServerFnTool is the critical primitive in TanStack AI. It allows developers to define a tool that runs securely on the server (accessing private keys, databases, and the BAML/Graphiti services) but is fully typed and accessible from the client.  
The power of this primitive lies in its isomorphism. The input schema (defined by Zod, synced to BAML) acts as the contract. The client sends data matching this contract; the server executes the heavy lifting (BAML parsing, Graphiti storage) and returns the result.8

### **5.2 Frontend Integration Strategy**

To integrate the Irish Education BAML classes into the frontend, we utilize the "Schema Sandwich" pattern described earlier.

1. **User Input:** The student types "I got a H3 in Physics."  
2. **TanStack Chat Hook:** The useChat hook sends this message to the LLM.  
3. **Tool Selection:** The LLM (e.g., GPT-4o) recognizes an intent to update grades. It calls the update\_student\_grade tool.  
4. **Zod Validation:** The tool verifies the arguments against the LeavingCertSubjectSchema.  
5. **Server Execution:**  
   * The server function invokes b.ExtractStudentData(input) (BAML) to ensure precise extraction of the "H3" and "Physics" entities.  
   * It calculates the new CAO points total.  
   * It pushes the update to Graphiti.  
6. **Response:** The tool returns the updated points total, which the LLM uses to generate a confirmation message: "Great work\! That H3 adds 77 points to your total."

**Code Example: The Integrated Tool**

TypeScript

// src/tools/education.ts  
import { createServerFnTool } from '@tanstack/ai-react';  
import { z } from 'zod';  
import { b } from '@/baml\_client'; // BAML Client  
import { graphitiClient } from '@/lib/graphiti'; // Custom wrapper for Python service

// Zod schema mirroring the BAML LeavingCertSubject class  
// This ensures the LLM knows EXACTLY what structure to provide  
const SubjectUpdateSchema \= z.object({  
  raw\_text: z.string().describe("The user's statement about their grades"),  
});

export const updateGradeTool \= createServerFnTool({  
  name: 'update\_grade',  
  description: 'Records a new grade for a Leaving Cert subject.',  
  inputSchema: SubjectUpdateSchema,  
    
  // This function runs on the server  
  execute: async ({ raw\_text }) \=\> {  
    console.log("Processing update via BAML...");  
      
    // 1\. BAML Extraction: Convert raw text to structured object  
    // This is safer than relying on the chat model's JSON generation  
    const structuredData \= await b.ExtractStudentData(raw\_text);  
      
    // 2\. Business Logic: CAO Points Calculation  
    // (Simplified logic for demonstration)  
    const points \= calculatePoints(structuredData.subjects);  
      
    // 3\. Memory Update: Send to Graphiti  
    await graphitiClient.addEpisode({  
      content: raw\_text,  
      entities: structuredData.subjects,  
      timestamp: new Date().toISOString()  
    });  
      
    return {  
      success: true,  
      extracted\_subjects: structuredData.subjects,  
      new\_total\_points: points,  
      message: "Grade graph updated successfully."  
    };  
  },  
});

### **5.3 User Interface Integration**

In TanStack Start, the UI simply consumes this tool via the standard chat interface components. The complexity of BAML and Graphiti is completely abstracted away from the React component.

TypeScript

// src/routes/dashboard.tsx  
import { useChat } from '@tanstack/ai-react';  
import { updateGradeTool } from '@/tools/education';

export default function Dashboard() {  
  const { messages, input, handleInputChange, handleSubmit } \= useChat({  
    api: '/api/chat',  
    // The tool definition is passed here.   
    // The library handles the round-trip execution.  
    tools: {  
      update\_grade: updateGradeTool  
    }  
  });

  return (  
    \<div className="education-assistant"\>  
      \<h1\>Leaving Cert Copilot\</h1\>  
      \<div className="chat-window"\>  
        {messages.map(m \=\> (  
          \<div key={m.id} className={\`message ${m.role}\`}\>  
            {m.content}  
            {/\* Tool invocations are handled automatically or can be visualized \*/}  
            {m.toolInvocations?.map(tool \=\> (  
               \<div className="tool-result"\>  
                 Updated Graph: {JSON.stringify(tool.result)}  
               \</div\>  
            ))}  
          \</div\>  
        ))}  
      \</div\>  
      \<form onSubmit={handleSubmit}\>  
        \<input   
          value={input}   
          onChange={handleInputChange}   
          placeholder="Tell me your latest grades..."   
        /\>  
        \<button type="submit"\>Update Profile\</button\>  
      \</form\>  
    \</div\>  
  );  
}

## ---

**6\. Performance and Scalability Analysis**

The integration of these three technologies offers distinct performance advantages over traditional stacks.

### **6.1 Token Efficiency and Latency**

BAML significantly reduces token usage. By using a specialized, compressed schema format in the prompt (rather than verbose JSON Schema), BAML reduces the prompt size by up to 40%.19 Furthermore, the SAP parser's ability to fix minor errors means that the "first pass" success rate is much higher, reducing the average latency by eliminating retry loops.

### **6.2 Data Integrity**

The use of Zod validation at the TanStack tool boundary acts as a first line of defense. If the LLM generates a tool call that violates the Zod schema (e.g., passing a number for a string field), TanStack AI catches this before it ever reaches the server function logic. This "fail-fast" mechanism protects the integrity of the BAML processing layer.10

### **6.3 Scalability of Temporal Queries**

Graphiti's use of graph databases (Neo4j/FalkorDB) allows for highly efficient traversal of connected data. A query like "Show me all students who take Higher Level Math and Physics" is a graph traversal operation, which is typically O(1) or O(log N) per node, compared to the expensive scan operations often required in unstructured vector stores. This makes the architecture suitable for scaling to thousands of students and millions of temporal data points.16

## ---

**7\. Conclusion**

The pipeline analyzed in this report—**BAML** for extraction, **Graphiti** for temporal memory, and **TanStack AI** for orchestration—represents a robust, enterprise-grade architecture for agentic AI.  
For the specific domain of the **Irish Education System**, this stack provides:

1. **Precision:** BAML ensures that grades and levels are extracted with strict adherence to the official ontology (H1-H8), avoiding hallucinated formats.  
2. **Context:** Graphiti allows the agent to "remember" the student's journey, enabling advice that considers progress over time (e.g., "You've improved from H5 to H3").  
3. **Safety:** TanStack AI and Zod allow for the secure execution of logic, ensuring that database updates and points calculations are performed on verified, structured data.

By implementing the "Interface-Implements" pattern to synchronize BAML types with Zod schemas, developers can bridge the gap between AI generation and runtime validation, creating a cohesive, type-safe development experience that scales to meet the demands of complex, real-world domains.

#### **Works cited**

1. Leaving Certificate \- Citizens Information, accessed December 5, 2025, [https://www.citizensinformation.ie/en/education/state-examinations/leaving-certificate/](https://www.citizensinformation.ie/en/education/state-examinations/leaving-certificate/)  
2. Understanding the Irish Leaving Certificate: A Guide for International Students and Parents, accessed December 5, 2025, [https://www.hsinet.org/post/understanding-the-irish-leaving-certificate-a-guide-for-international-students-and-parents](https://www.hsinet.org/post/understanding-the-irish-leaving-certificate-a-guide-for-international-students-and-parents)  
3. Why I'm excited about BAML and the future of agentic workflows \- The Data Quarry, accessed December 5, 2025, [https://thedataquarry.com/blog/baml-and-future-agentic-workflows/](https://thedataquarry.com/blog/baml-and-future-agentic-workflows/)  
4. Every Way To Get Structured Output From LLMs | BAML Blog, accessed December 5, 2025, [https://boundaryml.com/blog/structured-output-from-llms](https://boundaryml.com/blog/structured-output-from-llms)  
5. BoundaryML/baml: The AI framework that adds the engineering to prompt engineering (Python/TS/Ruby/Java/C\#/Rust/Go compatible) \- GitHub, accessed December 5, 2025, [https://github.com/BoundaryML/baml](https://github.com/BoundaryML/baml)  
6. JSON schema support for dynamic types · Issue \#771 · BoundaryML/baml \- GitHub, accessed December 5, 2025, [https://github.com/BoundaryML/baml/issues/771](https://github.com/BoundaryML/baml/issues/771)  
7. TanStack AI Alpha: Your AI, Your Way, accessed December 5, 2025, [https://tanstack.com/blog/tanstack-ai-alpha-your-ai-your-way](https://tanstack.com/blog/tanstack-ai-alpha-your-ai-your-way)  
8. TanStack/ai: SDK that enhances your applications with AI capabilities \- GitHub, accessed December 5, 2025, [https://github.com/TanStack/ai](https://github.com/TanStack/ai)  
9. \[ts-to-zod\] How do you best keep ts interface and zod schema in sync? : r/typescript \- Reddit, accessed December 5, 2025, [https://www.reddit.com/r/typescript/comments/1oe45zr/tstozod\_how\_do\_you\_best\_keep\_ts\_interface\_and\_zod/](https://www.reddit.com/r/typescript/comments/1oe45zr/tstozod_how_do_you_best_keep_ts_interface_and_zod/)  
10. Zod for TypeScript: A must-know library for AI development \- WorkOS, accessed December 5, 2025, [https://workos.com/blog/zod-for-typescript](https://workos.com/blog/zod-for-typescript)  
11. Leaving Certificate (Ireland) \- Wikipedia, accessed December 5, 2025, [https://en.wikipedia.org/wiki/Leaving\_Certificate\_(Ireland)](https://en.wikipedia.org/wiki/Leaving_Certificate_\(Ireland\))  
12. Irish Leaving Certificate \- University College Cork, accessed December 5, 2025, [https://www.ucc.ie/en/study/undergrad/entryreqs/irishleavingcertificate/](https://www.ucc.ie/en/study/undergrad/entryreqs/irishleavingcertificate/)  
13. Graphiti: Knowledge Graph Memory for an Agentic World \- Neo4j, accessed December 5, 2025, [https://neo4j.com/blog/developer/graphiti-knowledge-graph-memory/](https://neo4j.com/blog/developer/graphiti-knowledge-graph-memory/)  
14. Building AI Agents with Knowledge Graph Memory: A Comprehensive Guide to Graphiti | by Saeed Hajebi | Medium, accessed December 5, 2025, [https://medium.com/@saeedhajebi/building-ai-agents-with-knowledge-graph-memory-a-comprehensive-guide-to-graphiti-3b77e6084dec](https://medium.com/@saeedhajebi/building-ai-agents-with-knowledge-graph-memory-a-comprehensive-guide-to-graphiti-3b77e6084dec)  
15. Zep AI: Build Agents That Recall What Matters. End-to-End Context Engineering… | Y Combinator, accessed December 5, 2025, [https://www.ycombinator.com/companies/zep-ai](https://www.ycombinator.com/companies/zep-ai)  
16. graphiti/mcp\_server/README.md at main \- GitHub, accessed December 5, 2025, [https://github.com/getzep/graphiti/blob/main/mcp\_server/README.md](https://github.com/getzep/graphiti/blob/main/mcp_server/README.md)  
17. Overview | Zep Documentation, accessed December 5, 2025, [https://help.getzep.com/graphiti/getting-started/overview](https://help.getzep.com/graphiti/getting-started/overview)  
18. Server Functions | TanStack Start React Docs, accessed December 5, 2025, [https://tanstack.com/start/latest/docs/framework/react/guide/server-functions](https://tanstack.com/start/latest/docs/framework/react/guide/server-functions)  
19. The Prompting Language Every AI Engineer Should Know: A BAML Deep Dive \- Towards AI, accessed December 5, 2025, [https://pub.towardsai.net/the-prompting-language-every-ai-engineer-should-know-a-baml-deep-dive-6a4cd19a62db](https://pub.towardsai.net/the-prompting-language-every-ai-engineer-should-know-a-baml-deep-dive-6a4cd19a62db)