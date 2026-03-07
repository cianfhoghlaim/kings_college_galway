# **Architecting the Isomorphic AI Tutor: A Comprehensive Research Report on Integrating TanStack AI, BAML, and LiteLLM**

## **1\. Introduction and Architectural Thesis**

The landscape of educational technology is currently undergoing a seismic shift, driven by the convergence of generative artificial intelligence and full-stack application frameworks. The objective of this research report is to architect a next-generation "Isomorphic AI Tutor" specifically tailored for the Irish educational curriculum, leveraging the newly released **TanStack AI**, the structured extraction capabilities of **BAML (Boundary Abstract Markup Language)**, and the inference orchestration provided by **LiteLLM**.  
This report addresses a critical strategic pivot: moving away from disjointed client-server architectures where AI agents operate as separate microservices, towards a unified, isomorphic model powered by **TanStack Start**. The core premise examined here is the efficacy of the createServerFnTool primitive—a mechanism that unifies server-side business logic with AI tool definitions. This unification promises to reduce development friction, enhance type safety, and lower latency for students engaging with complex syllabus content.  
The analysis is deeply informed by the provided project context, specifically the requirements for a bilingual (English/Irish) system, the rigorous parsing of "Junior Cycle" and "Leaving Certificate" examination papers, and the deployment of fine-tuned models via Hugging Face. A significant portion of this report is dedicated to validating the user's hypothesis: *"Our BAML classes can generate Zod schemas, should it be very easy to develop this end-to-end then?"* The research indicates that while the potential for ease is high, the reality involves navigating a "Schema Gap" between BAML's output-centric design and TanStack AI's input-centric requirements.  
We define the **Isomorphic AI Stack** as a distinct architectural pattern where the distinction between a "frontend API call" and an "AI Tool invocation" vanishes. In this model, a function defined to fetch a Chemistry marking scheme for the UI is identical to the tool an LLM uses to grade a student's answer. This report details the implementation of this stack, traversing the layers from the React frontend, through the TanStack server runtime, bridging into the BAML extraction layer, and finally executing inference through LiteLLM proxies to Hugging Face Text Generation Inference (TGI) endpoints.

### **1.1 The Domain Context: Irish Education and High-Fidelity Extraction**

To understand the architectural necessities, one must first appreciate the data complexity outlined in the project attachments. The system is not merely a chatbot; it is a pedagogical engine. It must handle:

* **Junior Cycle Science:** Which relies on "Transverse Links" between contextual strands (e.g., Earth and Space) and the "Nature of Science" unifying strand.1  
* **Circular Governance:** The parsing of Department of Education circulars to track policy changes over time (e.g., Supersedes or Rescinds relationships).1  
* **Logic-Gate Marking:** The application of "Penalty Rules" (e.g., "Slip" vs. "Blunder") found in STEM marking schemes.1

These requirements necessitate a backend that is deterministic and strictly typed. The AI cannot simply "guess" the marking scheme; it must extract it exactly as defined by the State Examinations Commission. This validates the choice of BAML, but also imposes a constraint: the AI tools used to retrieve this data must be as rigorously defined as the data itself.

## ---

**2\. The Isomorphic Paradigm: TanStack Start and AI Integration**

The release of TanStack AI marks a pivotal moment for React developers, offering a "Framework Agnostic" approach that integrates deeply with **TanStack Start**. This section analyzes the mechanics of this integration and its specific application to the educational resources platform.

### **2.1 The Evolution of Server-Side Tooling**

Historically, integrating an AI agent with backend resources involved a tripartite duplication of labor. A developer would first write a Python or Node.js function to query a database (e.g., get\_syllabus\_topic). They would then wrap this in a REST or GraphQL API to make it accessible to the frontend. Finally, they would write a separate JSON Schema definition (OpenAI Function spec) to explain this API to the Large Language Model (LLM). This violated the DRY (Don't Repeat Yourself) principle and introduced synchronization risks—if the API changed, the LLM schema would break.  
**TanStack Start** resolves this through the concept of Server Functions. These are functions that execute exclusively on the server but can be imported and called from client-side code as if they were local functions.2 **TanStack AI** extends this with createServerFnTool.  
As detailed in the TanStack AI documentation 3, createServerFnTool allows for the definition of a single entity that serves three simultaneous purposes:

1. **Server Execution Context:** It contains the actual business logic (e.g., querying the Postgres database via Drizzle ORM).  
2. **Input Validator:** It utilizes **Zod** schemas to rigorously validate incoming data at the network boundary, protecting the server from malformed requests whether they originate from a student's browser or an LLM's hallucination.  
3. **AI Tool Definition:** It automatically generates the JSON Schema required by OpenAI-compatible models, including descriptions and parameter types inferred from the Zod schema.

### **2.2 Implementing the Isomorphic Pattern for Educational Resources**

For the educational platform, this architecture implies that the "API" layer effectively disappears. Instead, we define a library of domain capabilities. Consider the requirement to fetch "Community Summaries" for syllabus topics.1  
In the proposed architecture, we define this capability once:

TypeScript

import { createServerFnTool } from '@tanstack/ai-react';  
import { z } from 'zod';  
import { getTopicSummary } from '@/lib/graph\_rag'; // Internal logic

export const fetchCommunitySummary \= createServerFnTool({  
  name: 'fetch\_community\_summary',  
  description: 'Retrieves the pre-computed community summary for a specific syllabus topic, aggregated from student performance data.',  
  inputSchema: z.object({  
    topicId: z.string().describe("The official ID of the syllabus topic (e.g., 'JC-SCI-1.4')"),  
    level: z.enum(\['Higher', 'Ordinary'\]).default('Higher'),  
  }),  
  execute: async ({ topicId, level }) \=\> {  
    // This runs securely on the TanStack Start server  
    return await getTopicSummary(topicId, level);  
  },  
});

This code snippet represents a significant architectural efficiency. The **React Component** responsible for displaying the summary to a student uses this function directly:

TypeScript

const summary \= useServerFn(fetchCommunitySummary)({ topicId: 'JC-SCI-1.4' });

Simultaneously, the **AI Agent** responsible for answering student questions is provided with this tool:

TypeScript

const chat \= useChat({  
  tools:,  
});

When a student asks, "Why do people struggle with stoichiometry?", the AI can invoke fetch\_community\_summary to retrieve the data. The "isomorphic" nature means the security rules, validation logic (Zod), and database connections are identical for both consumers.

### **2.3 State Management and Streaming**

The "Backend Strategy" document highlights the need for low-latency responses using pre-computed summaries.1 However, strictly dynamic queries (e.g., "Find the hardest calculus question from 2018-2023") cannot be pre-computed. Here, TanStack AI's integration with **streaming** becomes critical.  
TanStack Start supports streaming Server-Side Rendering (SSR).1 When createServerFnTool is used, the return values can be streamed. This aligns with the token-generation nature of LLMs. If a tool execution involves a long-running BAML extraction process (e.g., reading a new PDF uploaded by a teacher), the tool can stream partial progress or "thoughts" back to the main chat context.  
The protocol documentation for TanStack AI indicates support for tool\_call and tool\_result chunks.4 This ensures that the user interface remains responsive. For the educational platform, this allows for the creation of "Thinking UIs," where the student sees the AI explicitly performing steps: "Searching Syllabus..." \-\> "Retrieving Marking Scheme..." \-\> "Analyzing Penalties...". This transparency is pedagogical in itself, teaching the student how to break down the problem.

## ---

**3\. The Schema Gap: BAML to Zod Integration**

The user's query posits a specific assumption: *"our baml classes can generate zod schemas, should it be very easy to develop this end-to-end then?"*  
**Research Finding:** This assumption is partially incorrect and highlights the primary integration challenge. BAML does **not** natively generate Zod schemas for input validation. It generates **TypeScript Interfaces** (for static typing) and **Pydantic Models** (for Python runtime validation).5

### **3.1 The Divergence of Schema Philosophy**

To understand why this gap exists, we must analyze the differing design philosophies of BAML and TanStack AI:

* **BAML (Output-Centric):** BAML is designed to coerce the chaotic, non-deterministic output of an LLM *into* a structured format. It uses "Schema-Aligned Parsing" (SAP) to be forgiving. If an LLM returns a JSON missing a closing brace or a quote, BAML attempts to repair it. Therefore, its generated code focuses on the *result* of an AI call.  
* **TanStack AI / Zod (Input-Centric):** The inputSchema in createServerFnTool defines the *contract* for calling a tool. It uses Zod to be strict. If an AI (or user) tries to call a tool with a missing argument or wrong type, Zod rejects it immediately to prevent runtime errors in the server function.

**The Friction Point:** To use a BAML-defined entity (like the ScienceOutcome class from the Irish curriculum 1) as an *input* to a tool (e.g., saveOutcome(outcome: ScienceOutcome)), we need a Zod schema that mirrors the BAML class. BAML generates the TypeScript interface ScienceOutcome, but it does not generate z.object({... }).

### **3.2 Solutions for Bridging the Gap**

To achieve the "very easy" workflow the user desires, we must bridge this gap. Manual duplication (writing a Zod schema that matches the BAML file) is error-prone and violates DRY. The research identifies three architectural strategies:

#### **Strategy A: BAML-First with Introspection (Recommended)**

This strategy treats the .baml files as the single source of truth. Since BAML internally compiles to a JSON Schema to prompt the LLM 7, we can intercept this artifact.

1. **The Mechanism:** When baml-cli build runs, it generates artifacts. While the CLI commands generate typically target Python or TypeScript, the internal representation is a JSON Schema.  
2. **The Adapter:** We can implement a build-step script using the json-schema-to-zod library.9 This script scans the BAML build output, locates the JSON schema definitions for entities like MarkingPoint or CircularMetadata, and transpiles them into a TypeScript file exporting Zod schemas.  
3. **The Workflow:**  
   * Developer defines class ExamQuestion {... } in BAML.  
   * Developer runs npm run generate-schemas.  
   * The script produces src/gen/zod/ExamQuestion.ts containing export const ExamQuestionSchema \= z.object(...).  
   * Developer imports this schema into createServerFnTool.

This approach satisfies the requirement for an "end-to-end" workflow where the BAML file drives the entire type system.

#### **Strategy B: Zod-First with Code Generation**

Alternatively, we can invert the relationship. If the team prefers Zod as the source of truth (due to its rich ecosystem in the React/TanStack world):

1. **Define Models in Zod:** src/schemas/science.ts defines the ScienceOutcome schema.  
2. **Generate BAML:** A custom generator script utilizes zod-to-json-schema to create a temporary JSON schema, which is then converted into BAML syntax (using Handlebars or simple string manipulation) and written to baml\_src/generated.baml.  
3. **Execution:** The application uses Zod for the frontend and TanStack AI tools, while BAML uses the generated definitions for the extraction prompts.

**Verdict:** Given the user's investment in BAML (evident from the detailed schema attachments), **Strategy A** is the superior architectural choice. It respects the complexity of the BAML schemas (like the nested TransverseLink logic) which might be cumbersome to express in Zod first.

### **3.3 Implementing the Bridge: Technical Specifics**

The integration requires a specific pattern to handle the "BAML Client" within the "TanStack Server Function."  
If the BAML client is generated for **Python** (as implied by the "Backend Strategy" referencing Pydantic 1), the TanStack server function (running in Node.js) cannot call it directly. It requires an **IPC (Inter-Process Communication)** bridge.  
**The Sidecar Pattern:**

1. **Python Microservice:** Runs the BAML client and exposes endpoints like /extract/exam-paper.  
2. **TanStack Server Function:**  
   TypeScript  
   const extractTool \= createServerFnTool({  
     name: 'extract',  
     inputSchema: z.object({ text: z.string() }),  
     execute: async ({ text }) \=\> {  
       // Isomorphic Node.js code calling Python sidecar  
       const response \= await fetch('http://localhost:8000/extract', {  
         method: 'POST',  
         body: JSON.stringify({ text })  
       });  
       return response.json(); // Returns typed data derived from BAML  
     }  
   });

If the team utilizes the **BAML TypeScript Generator** 5, this complexity evaporates. The BAML client can be imported directly into the TanStack server function, allowing for a monolithic, strictly typed architecture.

## ---

**4\. The Inference Gateway: LiteLLM and Hugging Face**

The user explicitly requests using **LiteLLM** via their OpenAI endpoint to utilize Hugging Face models. This layer is crucial for the project's financial and technical viability, enabling the use of open-weights models that can be fine-tuned for the Irish curriculum (e.g., "Gaeilge" language support).

### **4.1 The Protocol Translation Challenge**

TanStack AI is built to communicate with the **OpenAI Chat Completion API**. It expects a specific JSON structure for tools (function definitions) and tool\_calls (the model's request to execute a function).  
**The Mismatch:** Hugging Face's **Text Generation Inference (TGI)** endpoints, while offering an "OpenAI Compatible" Messages API 10, often have subtle divergences, particularly regarding tool calling formats. Some fine-tuned open models do not support the native tool\_calls parameter and instead rely on prompt engineering (e.g., inserting XML tags like \<tool\>...\</tool\>).  
LiteLLM as the Rosetta Stone:  
LiteLLM functions as a proxy server that normalizes these interactions. It resides between TanStack AI and the Hugging Face endpoint.

1. **TanStack AI** sends a standard OpenAI POST /v1/chat/completions request to LiteLLM, containing the tools definition generated from our Zod schemas.  
2. **LiteLLM** receives this request. Based on the model configuration (e.g., model="huggingface/tgi"), it transforms the request.11  
   * If the target TGI model supports native tool calling (e.g., Llama 3.1 Instruct), LiteLLM passes the parameters through.  
   * If the model requires a custom prompt format (e.g., Functionary or older Mistral versions), LiteLLM's **templating engine** injects the tool definitions into the system prompt.  
3. **Hugging Face TGI** generates the response.  
4. **LiteLLM** parses the output (extracting the tool call from the text if necessary) and reformats it into the standard OpenAI tool\_calls JSON structure.  
5. **TanStack AI** receives the clean JSON, unaware that the underlying model was not GPT-4.

### **4.2 Fine-Tuned Model Configuration**

The "Backend Strategy" implies the use of fine-tuned models for specific tasks like "Marking Scheme Extraction." To integrate these with TanStack AI via LiteLLM, specific configuration is required.  
Configuration Strategy:  
The LiteLLM config.yaml must explicitly map the "model name" requested by TanStack AI to the specific Hugging Face repository URL.

YAML

model\_list:  
  \- model\_name: irish-tutor-v1  
    litellm\_params:  
      model: huggingface/HuggingFaceH4/zephyr-7b-beta \# Example fine-tune  
      api\_base: https://api-inference.huggingface.co/models/YourOrg/Irish-Curriculum-Llama-3  
      api\_key: os.environ/HF\_TOKEN  
      \# Critical for handling tool calls on non-native models  
      prompt\_template: "llama-3-tool-use" 

In the TanStack application, the adapter is configured to point to this proxy:

TypeScript

import { OpenAIAdapter } from '@tanstack/ai-openai';

const adapter \= new OpenAIAdapter({  
  baseURL: "http://localhost:4000", // LiteLLM Local Proxy  
  apiKey: "sk-litellm-proxy", // Virtual key  
});

### **4.3 Handling "Gaeilge" and Bilingual Tokens**

For the bilingual requirement, standard tokenizers often fragment Irish words poorly, increasing latency and cost. By using Hugging Face models (like an Irish-fine-tuned Llama 3\) served via TGI, the project ensures efficient tokenization. LiteLLM's pass-through architecture preserves the integrity of these specialized tokens, ensuring that when the AI generates an Irish response, the accent marks (fadas) and grammatical structures remain intact, which is critical for the "High Fidelity" requirement of the educational content.

## ---

**5\. Domain-Specific Implementation Strategies**

This section translates the architectural theory into concrete implementation strategies for the specific educational domains identified in the research snippets.

### **5.1 Scenario 1: The "Junior Cycle Science" Navigator**

Context: The syllabus is non-linear, linking "Contextual Strands" via "Transverse Links".1  
Requirement: The AI must help a student navigate these links.  
**Implementation:**

1. **BAML Layer:** We define a BAML class TransverseLink that maps a source\_outcome\_id to a target\_nos\_id (Nature of Science).  
2. **Tool Layer (createServerFnTool):** We create a tool explore\_science\_connections.  
   * **Input:** z.object({ outcomeId: z.string(), depth: z.number() }).  
   * **Logic:** The function queries the GraphRAG database (Neo4j) to find connections. It might also call a lightweight BAML function to "generate" a connection if one is missing in the graph but semantically relevant.  
3. **Usage:** When a student asks "How does this experiment relate to the Nature of Science?", the AI calls the tool. The tool returns the structured link. TanStack AI allows the frontend to render this not just as text, but as an interactive node graph using the isomorphic data.

### **5.2 Scenario 2: The "Circular" Policy Engine**

Context: Circulars have temporal validity (Supersedes, EffectiveDate).1  
Requirement: The AI must answer "Is this rule still valid?"  
**Implementation:**

1. **BAML Layer:** The ExtractCircularMeta function extracts the supersedes\_id from raw PDF text.  
2. **Tool Layer:** check\_circular\_validity.  
   * **Input:** z.object({ circularNumber: z.string() }).  
   * **Logic:** The server function performs a recursive graph lookup. It finds the circular, checks if any newer circular points to it via a Supersedes edge.  
   * **Streaming:** Because this recursion might take time, the server function streams the audit trail: "Found Circular 001/2023... Checked for updates... Found Circular 005/2024 which supersedes it...".  
3. **Result:** The AI creates a response: "Circular 001/2023 is no longer valid; it was replaced by 005/2024 on this date."

### **5.3 Scenario 3: The "Marking Scheme" Logic Gate**

Context: Grading relies on "Penalty Rules" and "Valid Alternatives".1  
Requirement: The AI must grade a student's answer.  
**Implementation:**

1. **BAML Layer:** ExtractMarkingScheme parses the complex rules into a MarkingRubric object.  
2. **Tool Layer:** grade\_answer.  
   * **Input:** z.object({ questionId: z.string(), studentAnswer: z.string() }).  
   * **Logic:**  
     * Step 1: Fetch the MarkingRubric (cached or via BAML).  
     * Step 2: This is a complex reasoning task. Instead of just "calculating" the grade, the tool might invoke a *second* BAML function ApplyRubric which takes the rubric and the student answer and uses a high-reasoning model (GPT-4o via OpenAI) to apply the logic.  
   * **Orchestration:** This demonstrates **Server-Side Agentic Loops**. The TanStack server function acts as an agent itself, coordinating between the database, the extraction model (BAML), and the reasoning model (OpenAI), before returning the final grade to the client-side chat.

## ---

**6\. Detailed Integration Workflow**

To "develop this end-to-end," the following specific workflow is recommended to the engineering team.

### **Phase 1: The Schema Build Pipeline**

Create a script scripts/sync-baml-zod.ts. This script should:

1. Execute baml-cli build to generate the internal JSON Schema artifacts.  
2. Iterate through the specific domain classes (ExamPaper, MarkingScheme, CircularMetadata).  
3. Use a templating engine to generate a file src/lib/zod-schemas.ts.  
   * *Mapping:* BAML string \-\> Zod z.string(). BAML enum \-\> Zod z.enum(). BAML optional \-\> Zod .optional().  
4. Add this script to the npm run dev and npm run build hooks to ensure the Zod schemas are always isomorphic with the BAML definitions.

### **Phase 2: The Isomorphic Tool Registry**

Create a directory src/app/tools/. Each file here should export a createServerFnTool.

* Import the auto-generated Zod schemas from Phase 1\.  
* Implement the execute function using Drizzle ORM for database access.  
* If BAML extraction is needed at runtime (e.g., user uploads a photo), import the BAML client here.

### **Phase 3: The Inference Configuration**

1. Deploy **LiteLLM Proxy** using Docker.  
2. Configure model\_list in config.yaml to include:  
   * gpt-4o (Direct OpenAI proxy for reasoning).  
   * huggingface/irish-llama (The fine-tuned model for content generation).  
3. Set LITELLM\_BASE\_URL in the TanStack Start environment.

### **Phase 4: The UI Implementation**

In the React components (src/routes/\_\_root.tsx or specific chat routes):

1. Initialize useChat from @tanstack/ai-react.  
2. Pass the imported tools from src/app/tools/ into the tools array.  
3. TanStack AI handles the rest: rendering the UI, sending the message, handling the tool call round-trip, executing the server function, and streaming the response.

## ---

**7\. Analysis of "Ease of Development" Claim**

The user asked: *"should it be very easy to develop this end-to-end then?"*  
**The Verdict:** It is **architecturally elegant**, but not "easy" without the specific setup described above.

* **The "Easy" Part:** Once the createServerFnTool is set up, adding new capabilities is trivial. You write a function, add a Zod schema, and the AI immediately knows how to use it. There is no context switching between frontend, backend API, and AI config.  
* **The "Hard" Part:** The initial setup of the "BAML-to-Zod" bridge is non-trivial. BAML and Zod are distinct ecosystems. Without the automated generation script proposed in Section 3.2, developers will find themselves manually keeping two sets of schema files in sync, which is a recipe for frustration.  
* **The "LiteLLM" Complexity:** While LiteLLM is powerful, debugging tool-call formats on open-source models can be difficult. It requires a deep understanding of prompt templates.

**Conclusion:** The development experience will become "very easy" *only after* the investment in the foundational tooling (Schema Bridge \+ LiteLLM Configuration) is made.

## ---

**8\. Strategic Recommendations and Future Outlook**

1. **Prioritize the Schema Bridge:** The engineering team should treat the baml-to-zod generator as a P0 infrastructure task. This is the glue that holds the stack together.  
2. **Hybrid Inference Strategy:** Do not rely solely on Hugging Face models for logic. Use OpenAI (GPT-4o) via LiteLLM for the "Orchestration" and "Logic Gate" application (grading), and use the Fine-Tuned Hugging Face models for "Content Generation" (writing explanations, translating Irish). This balances cost with reasoning capability.  
3. **Leverage Isomorphism for SEO:** Since TanStack Start renders these server functions, the "Community Summaries" generated by the AI tools can be indexed by search engines. Ensure the UI renders the tool outputs as semantic HTML, not just chat bubbles, to capture this SEO value.  
4. **Observability:** Use LiteLLM's built-in cost tracking and logging. With students potentially driving high volumes of queries, understanding the cost-per-syllabus-topic is vital for the business model.

By executing this architecture, the platform will achieve a level of integration that is rare in the current EdTech market: a system where the AI is not a sidecar, but a deeply integrated, type-safe participant in the educational data flow. The "Isomorphic AI Tutor" is not just a technological upgrade; it is a structural evolution that aligns the software architecture with the pedagogical complexity of the Irish curriculum.

### **8.1 Future Proofing: The "Agentic" Shift**

Looking ahead, this architecture positions the platform for the "Agentic" future. As models become capable of longer-horizon planning, the createServerFnTool definitions become the "actions" available to the agent. Because they are standard server functions, they can be composed. An "Exam Prep Agent" could autonomously call navigateSyllabus, then fetchExamPapers, then grade\_answer, and finally generate\_report, all within a single autonomous loop running on the TanStack server. This is the ultimate promise of the Isomorphic AI Stack.

## ---

**9\. Conclusion**

The integration of TanStack AI with the existing BAML and LiteLLM infrastructure represents a highly viable and forward-looking strategy. It capitalizes on the strengths of each component: TanStack for isomorphic application logic, BAML for data integrity, and LiteLLM for model flexibility. While the "Schema Gap" presents a tangible integration hurdle, the solutions outlined in this report provide a clear path to resolution. The resulting system will be a highly performant, type-safe, and scalable educational platform capable of delivering deep, personalized learning experiences.

#### **Works cited**

1. BAML Schemas for Irish Education.md  
2. Server Functions | TanStack Start React Docs, accessed December 5, 2025, [https://tanstack.com/start/latest/docs/framework/react/guide/server-functions](https://tanstack.com/start/latest/docs/framework/react/guide/server-functions)  
3. TanStack/ai: SDK that enhances your applications with AI capabilities \- GitHub, accessed December 5, 2025, [https://github.com/TanStack/ai](https://github.com/TanStack/ai)  
4. ai/CHANGELOG.md at main · TanStack/ai \- GitHub, accessed December 5, 2025, [https://github.com/TanStack/ai/blob/main/CHANGELOG.md](https://github.com/TanStack/ai/blob/main/CHANGELOG.md)  
5. Typescript | Boundary Documentation, accessed December 5, 2025, [https://docs.boundaryml.com/guide/installation-language/typescript](https://docs.boundaryml.com/guide/installation-language/typescript)  
6. OpenAI Adapter | TanStack AI Docs, accessed December 5, 2025, [https://tanstack.com/ai/latest/docs/adapters/openai](https://tanstack.com/ai/latest/docs/adapters/openai)  
7. JSON schema support for dynamic types · Issue \#771 · BoundaryML/baml \- GitHub, accessed December 5, 2025, [https://github.com/BoundaryML/baml/issues/771](https://github.com/BoundaryML/baml/issues/771)  
8. ctx.output\_format | Boundary Documentation, accessed December 5, 2025, [https://docs.boundaryml.com/ref/prompt-syntax/ctx-output-format](https://docs.boundaryml.com/ref/prompt-syntax/ctx-output-format)  
9. json-schema-to-zod \- NPM, accessed December 5, 2025, [https://www.npmjs.com/package/json-schema-to-zod](https://www.npmjs.com/package/json-schema-to-zod)  
10. From OpenAI to Open LLMs with Messages API on Hugging Face, accessed December 5, 2025, [https://huggingface.co/blog/tgi-messages-api](https://huggingface.co/blog/tgi-messages-api)  
11. Hugging Face \- LiteLLM Docs, accessed December 5, 2025, [https://docs.litellm.ai/docs/providers/huggingface](https://docs.litellm.ai/docs/providers/huggingface)