# **Architecting the Adaptive Classroom: A Technical Blueprint for Agentic Educational Systems Using Agno, Restate, and BAML**

## **Executive Summary**

The digitization of educational assessment has historically been constrained by the rigid nature of data processing pipelines. While Large Language Models (LLMs) offer semantic understanding, integrating them into high-stakes educational workflows requires a level of reliability, structure, and adaptability that standard "chatbot" architectures cannot provide. This report delineates the technical architecture for a next-generation "Student/Teacher Project"—an interactive ecosystem designed to programmatically derive assessment criteria from syllabi and utilize those criteria to process, index, and grade exam papers with high fidelity.  
The proposed architecture moves beyond the fragility of standard prompt engineering by implementing a robust triad of technologies: **Agno** (formerly Phidata) serves as the cognitive kernel, providing agentic memory and knowledge retrieval; **Restate.dev** acts as the nervous system, ensuring durable, fault-tolerant execution of long-running grading workflows; and **BAML** (Boundary AI Markup Language) functions as the structural enforcement layer, specifically utilizing its dynamic TypeBuilder capabilities to generate Pydantic schemas at runtime.  
This report focuses heavily on the novel methodology of **Dynamic Schema Generation**. In traditional systems, the data schema (e.g., what constitutes an "exam question") is hard-coded by engineers. In this proposed system, the schema is derived programmatically from the *Syllabus* or *Marking Scheme* itself. The system reads the pedagogical rules first, constructs a custom data model representing those rules, and only then processes the exam papers. This ensures that the nuances of different educational boards—whether the "Assessment Objectives" of Cambridge International or the "Command Terms" of the International Baccalaureate—are natively understood and indexed by the system.  
By synthesizing Agno's **Agentic Chunking** for semantic preservation, Restate's **Awakeables** for human-in-the-loop teacher verification, and BAML's **Schema-Aligned Parsing**, this architecture solves the "last mile" problem in EdTech: the gap between the probabilistic nature of AI and the deterministic requirements of academic assessment.

## ---

**1\. The Imperative for Adaptive Architecture in EdTech**

The fundamental challenge in applying Artificial Intelligence to educational administration and pedagogy is the variability of the source material. Unlike standardized data domains such as finance (where a transaction always has an amount and a date) or logistics (where a shipment has an origin and destination), educational artifacts are semi-structured and highly heterogeneous. A Physics exam paper is structured fundamentally differently from a History essay prompt; a Marking Scheme for Mathematics operates on binary correctness, whereas one for English Literature operates on qualitative rubric bands.

### **1.1 The Limitations of Static Schema Definitions**

In conventional software engineering, data models are defined at compile time. An engineer might create a database schema or a Pydantic class representing a generic ExamQuestion:

Python

class ExamQuestion(BaseModel):  
    id: str  
    text: str  
    marks: int

This static approach causes massive information loss in an educational context. It fails to capture domain-specific metadata that is critical for pedagogical analysis. For instance, a Biology syllabus might categorize questions by "Bloom's Taxonomy" levels (Recall, Application, Analysis), while a Computer Science syllabus might categorize them by "Cognitive Complexity." If the system's underlying data model does not have fields for these specific attributes, the AI cannot index or retrieve questions based on them. A teacher asking, "Show me all questions requiring *Analysis* from the 2023 paper," would receive poor results because the "Analysis" tag was never extracted.  
Hard-coding these fields is impossible due to the sheer diversity of subjects. Creating a "Super-Schema" with optional fields for every possible educational metric results in a sparse, unmanageable data structure. The solution, therefore, is **Dynamic Schema Generation**: a system that learns the schema from the syllabus *before* it processes the content.

### **1.2 The Technological Trinity**

To solve this, we require a stack that separates Intelligence, State, and Structure.

| Layer | Technology | Role in Educational Project | Key Capability Leveraged |
| :---- | :---- | :---- | :---- |
| **Cognitive** | **Agno** | Agentic Logic & Memory | **Agentic RAG / Chunking:** Intelligent segmentation of exam papers based on semantic boundaries rather than character counts.1 |
| **Orchestration** | **Restate.dev** | Durable Execution | **Awakeables & Virtual Objects:** Managing long-running grading sagas and persisting student state across semesters without database complexity.3 |
| **Structural** | **BAML** | Schema Enforcement | **TypeBuilder:** Runtime generation of Pydantic models from unstructured syllabus text to drive precise extraction.5 |

This report details the integration of these three layers, with a specific focus on the BAML-driven pipeline for deriving Pydantic classes from educational documents.

## ---

**2\. The Cognitive Layer: Agno and Agentic Intelligence**

Agno (formerly Phidata) provides the application framework for building autonomous agents. In the context of the Student/Teacher Project, Agno is not merely a wrapper for LLM API calls; it is the infrastructure that manages the "Knowledge Base"—the repository of syllabi, marking schemes, and exam papers.

### **2.1 Knowledge Base Architecture for Exam Papers**

Educational documents present a unique retrieval challenge. A single PDF of an exam paper contains multiple questions, diagrams, and instructions. Standard RAG (Retrieval Augmented Generation) pipelines typically use "Fixed Size Chunking," splitting the document into arbitrary 500-character segments.  
This is catastrophic for exam papers. A split might occur in the middle of a question, separating the prompt ("Calculate the velocity...") from the necessary data ("...given a mass of 5kg"). When the vector database retrieves one chunk without the other, the Agent hallucinates the missing values.  
Agno addresses this through **Agentic Chunking**. As described in the Agno documentation 2, Agentic Chunking utilizes a language model to scan the document and identify semantic boundaries.  
**The Agentic Chunking Workflow:**

1. **Ingestion:** The system loads the Exam Paper PDF.  
2. **Semantic Scanning:** An Agno-configured LLM (e.g., GPT-4o-mini) reads the text stream.  
3. **Boundary Detection:** The model is prompted to identify the start and end of distinct *Assessment Units* (e.g., "Question 1", "Question 1(a)", "Question 1(b)").  
4. **Indexing:** The system creates chunks that correspond 1:1 with these assessment units, preserving the integrity of the question.

This ensures that when a student asks, "Help me with Question 3," the Agent retrieves the *entirety* of Question 3 as a single, coherent context unit.

### **2.2 Vector Database Strategy**

Agno supports multiple vector stores, including **pgvector** and **LanceDB**.8 For this project, **pgvector** is the optimal choice. It allows the vector embeddings of questions to live alongside the relational metadata (e.g., "Subject", "Year", "Difficulty") in a single PostgreSQL database.  
This architecture supports **Hybrid Search**. When a teacher searches for "Calculus questions from 2022," Agno performs a keyword filter on the metadata (topic="Calculus", year=2022) *before* performing the semantic vector search. This drastically improves precision compared to pure vector similarity, which might return a Physics question about rates of change (semantically similar to Calculus) when the teacher specifically wanted Mathematics questions.

### **2.3 Agentic Memory and Student Profiles**

Agno provides built-in memory management 9, allowing Agents to retain context across sessions. This is critical for the "Student" persona.

* **User Memory:** Stores personal preferences and learning styles (e.g., "The student prefers visual explanations").  
* **Session State:** Tracks the immediate context of the current tutoring session.

By leveraging Agno's Memory modules, the system builds a longitudinal profile of the student. If a student consistently struggles with questions tagged "AO2: Application," the Agent can reference this historical data to adjust its tutoring strategy, offering more scaffolded support for application-style questions in future sessions.

## ---

**3\. The Orchestration Layer: Durable Execution with Restate**

While Agno provides the intelligence, **Restate.dev** provides the reliability. An educational platform must handle long-running, asynchronous processes that cannot be lost due to server restarts or API timeouts. Grading a batch of 100 exams using an LLM is a process that takes time and is prone to "transient failures" (e.g., rate limits).

### **3.1 The Philosophy of Durable Execution**

Restate implements a paradigm where code execution is guaranteed to complete. It acts as an orchestrator that sits in front of the application services. When a function is called via Restate, every step of execution is logged in a durable journal. If the server crashes, Restate replays the journal to restore the function's state to exactly where it was before the crash, and then resumes execution.3  
This "Invincible Workflow" concept is applied to two critical workflows in the Student/Teacher Project: **The Grading Saga** and **The Human-in-the-Loop Approval**.

### **3.2 The Grading Saga: Handling Rate Limits and Retries**

When a teacher uploads a stack of student submissions, the system initiates a GradeSubmission workflow. This involves complex chaining:

1. **Extraction:** Extract the student's handwritten answer (OCR).  
2. **Retrieval:** Fetch the relevant Marking Scheme criteria from Agno's Knowledge Base.  
3. **Analysis:** Call the LLM to compare the answer against the criteria.  
4. **Persistence:** Save the grade and feedback to the database.

In a standard application, if the LLM API throws a 429 Too Many Requests error during Step 3, the entire process might fail, and the teacher would see a generic error message. Restate handles this natively. The LLM call is wrapped in a ctx.run block.10 Restate automatically handles retries with exponential backoff for transient errors. The developer does not write retry loops; the infrastructure guarantees the code eventually succeeds.

### **3.3 The "Teacher Verification" Pattern (Awakeables)**

A critical requirement for educational AI is that the AI should *assist*, not *replace*, the teacher. High-stakes grading requires human verification. Restate's **Awakeables** mechanism 4 is perfect for this "Human-in-the-Loop" pattern.  
**Workflow Logic:**

1. The AI calculates a provisional grade.  
2. The workflow generates a unique "Approval ID" (an Awakeable).  
3. The workflow sends an email or dashboard notification to the teacher with this ID.  
4. The code executes await ctx.awakeable(). **This is the key differentiation.**  
   * In a standard server, pausing execution consumes a thread and memory. If the teacher waits 3 days to approve, the server resource is held or times out.  
   * In Restate, the function *suspends* completely. The state is serialized to disk. The system consumes zero compute resources while waiting.  
5. Days later, when the teacher clicks "Approve," Restate wakes up the function, restores the local variables (the provisional grade, the student ID), and resumes execution at the very next line of code to finalize the grade in the database.

### **3.4 Virtual Objects for Class Management**

Restate's **Virtual Objects** 3 provide a way to model stateful entities that must be accessed sequentially. We model each **Classroom** or **ExamSession** as a Virtual Object.

* **Concurrency Control:** Virtual Objects process requests one at a time. This prevents race conditions where two students submitting exams simultaneously might corrupt the "Class Average" statistics.  
* **State Isolation:** The state of "Class 10-A" is isolated from "Class 10-B." This aligns perfectly with the privacy requirements of educational data.

## ---

**4\. The Structural Core: Dynamic Schema Generation with BAML**

The most innovative aspect of this architecture is the use of **BAML (Boundary AI Markup Language)** to solve the "Syllabus Variance" problem. The core requirement is to "derive Pydantic classes from Syllabi" \[User Query\]. This is a metaprogramming challenge: the code must write code (or at least, schema) based on the input data.

### **4.1 BAML and the TypeBuilder Paradigm**

Standard BAML usage involves defining .baml files with static classes (like Pydantic models) and prompts. However, BAML also provides a **TypeBuilder** SDK (available in Python and TypeScript) that allows developers to construct these types dynamically at runtime.5  
This capability is essential because we cannot define the ExamQuestion class in advance.

* **Scenario A:** A Math syllabus defines questions with properties: Topic, Difficulty, WorkingOutMarks, AnswerMarks.  
* **Scenario B:** An English syllabus defines questions with properties: Theme, CharacterReference, AO1 (Articulate response), AO2 (Analyze language).

A single static class cannot serve both without becoming a loose "bag of attributes" (e.g., Dict\[str, Any\]), which sacrifices the type safety and validation benefits of Pydantic. By using TypeBuilder, we create a *precise* Pydantic model for Scenario A and a different *precise* model for Scenario B, on the fly.

### **4.2 The Two-Pass Generation Algorithm**

To implement this, we design a two-pass pipeline.

#### **Phase 1: The Meta-Extraction**

First, we use a *static* BAML function to analyze the Syllabus PDF. The goal is not to extract questions, but to extract the *structure* of the exam.  
**Static BAML Definition (syllabus\_analyzer.baml):**

Rust

class GradingCriterion {  
  name: string  
  description: string  
  max\_score: int  
  code: string // e.g., "AO1", "AO2"  
}

class ExamStructure {  
  subject\_name: string  
  paper\_code: string  
  sections: string  
  criteria: GradingCriterion  
}

function ExtractExamStructure(syllabus\_text: string) \-\> ExamStructure {  
  client GPT4o  
  prompt \#"  
    Analyze this syllabus. Define the structure of the exam paper.  
    Identify the specific Assessment Objectives (AO) or grading criteria used.  
      
    {{ syllabus\_text }}  
  "\#  
}

When this runs, it returns an ExamStructure object. For a Physics paper, it might list criteria like {"name": "Knowledge", "code": "AO1"} and {"name": "Handling Data", "code": "AO2"}.

#### **Phase 2: Runtime Type Construction**

The Python application takes this ExamStructure object and uses the BAML TypeBuilder to define the schema for the actual exam papers.  
**Python Implementation (Conceptual):**

Python

from baml\_client.type\_builder import TypeBuilder  
from baml\_client import b

async def create\_dynamic\_parser(structure: ExamStructure):  
    tb \= TypeBuilder()  
      
    \# 1\. Define the 'AssessmentObjectives' class dynamically  
    ao\_class \= tb.add\_class("AssessmentObjectives")  
    for criterion in structure.criteria:  
        \# Dynamically add a float field for each criterion found in the syllabus  
        \# e.g., adds field 'AO1' of type float  
        ao\_class.add\_property(criterion.code, tb.float().optional())  
          
    \# 2\. Define the 'ExamQuestion' class utilizing the dynamic AO class  
    question\_class \= tb.add\_class("ExamQuestion")  
    question\_class.add\_property("question\_number", tb.string())  
    question\_class.add\_property("question\_text", tb.string())  
    question\_class.add\_property("max\_marks", tb.int())  
      
    \# Inject the dynamic assessment breakdown  
    question\_class.add\_property("criteria\_breakdown", ao\_class)  
      
    \# 3\. Create the extraction function signature  
    \# We tell BAML: "We want a list of this new 'ExamQuestion' class"  
    tb.function("ExtractQuestions").returns(tb.list(question\_class))  
      
    return tb

async def process\_exam\_paper(exam\_text: str, tb: TypeBuilder):  
    \# Call the BAML function, injecting the dynamic types  
    \# BAML's runtime replaces the generic return type with our custom schema  
    result \= await b.ExtractQuestions(exam\_text, {"tb": tb})  
    return result

#### **Phase 3: Pydantic Class Derivation**

The result returned by b.ExtractQuestions is not a generic dictionary. BAML's Python client automatically generates a Pydantic BaseModel corresponding to the dynamic type.6  
This means the application receives a Pydantic object where result.criteria\_breakdown.AO1 is a valid, typed field. This Pydantic model can then be used for:

1. **Validation:** Ensuring the extracted data meets the types (e.g., marks are integers).  
2. **Serialization:** Converting to JSON for storage.  
3. **Vector Metadata:** The fields of the Pydantic model map directly to the metadata columns in Agno's pgvector store.

### **4.3 Why BAML Beats Pure Pydantic for this Task**

One might ask: "Why not just use OpenAI's response\_format with a Pydantic model created via create\_model?"  
The answer lies in **Token Efficiency** and **Schema-Aligned Parsing**.

1. **Token Efficiency:** Standard JSON Schema definitions (used by OpenAI) are verbose. BAML uses a terse, pseudo-code syntax in the prompt to define the schema. This reduces the token overhead by 60-80% 14, which is crucial when processing hundreds of pages of exam text. The context window is saved for the *content*, not the *schema*.  
2. **Schema-Aligned Parsing (SAP):** BAML's parser is fault-tolerant. If the LLM misses a closing brace or adds a comment where it shouldn't, BAML's Rust-based parser can often recover the data without needing a costly re-run.14 Pydantic's validation is strict; if the JSON is malformed, it fails. BAML fixes the JSON *before* it reaches Pydantic.

## ---

**5\. System Integration: The EduFlow Architecture**

Having defined the components, we now assemble the full system architecture. This section describes the data flow from a Teacher uploading a syllabus to a Student searching for practice questions.

### **5.1 The Data Pipeline**

#### **Step 1: Syllabus Ingestion (The Teacher Agent)**

* **Trigger:** Teacher uploads Physics\_Syllabus\_2024.pdf.  
* **Action:** Agno TeacherAgent receives the file.  
* **Process:**  
  1. Agent calls b.ExtractExamStructure (BAML).  
  2. System stores the resulting structure (the "Meta-Schema") in the SQL database, linked to the subject\_id.  
  3. System uses TypeBuilder to generate the PhysicsQuestion2024 Pydantic model and registers it in the Schema Registry.

#### **Step 2: Exam Indexing (The Indexing Worker)**

* **Trigger:** Teacher uploads Physics\_Paper\_1.pdf.  
* **Action:** Restate triggers the IndexExamSaga.  
* **Process:**  
  1. **Look up:** Fetch the "Meta-Schema" for Physics 2024\.  
  2. **Reconstruct:** Rebuild the PhysicsQuestion2024 type using TypeBuilder.  
  3. **Read:** Agno PDFReader reads the exam PDF.  
  4. **Chunk:** Agno AgenticChunking splits the text into question units.  
  5. **Extract:** BAML extracts structured data from each chunk using the dynamic PhysicsQuestion2024 schema.  
  6. **Embed:** Agno embeds the question\_text field.  
  7. **Upsert:** Store the embedding in pgvector. Crucially, flatten the Pydantic object (including the dynamic AO fields) into the vector's metadata JSONB column.

#### **Step 3: Student Interaction (The Student Agent)**

* **Trigger:** Student asks, "Give me a hard question on Kinematics involving calculations."  
* **Action:** Agno StudentAgent analyzes the intent.  
* **Reasoning:**  
  * "Hard" maps to difficulty \> 0.8 (if available) or mark\_count \> 5\.  
  * "Kinematics" maps to topic="Kinematics".  
  * "Calculations" maps to AO2 \> 0 (assuming AO2 is "Application/Calculation" in this syllabus).  
* **Retrieval:** Agent executes a Hybrid Search on the Vector DB:  
  SQL  
  SELECT \* FROM exam\_questions   
  WHERE metadata-\>\>'topic' \= 'Kinematics'   
  AND (metadata-\>\>'AO2')::int \> 0  
  ORDER BY embedding \<=\> query\_embedding  
  LIMIT 1

* **Response:** The Agent presents the question. Because it has the structured breakdown, it can also act as a tutor: "This question requires you to apply your knowledge (AO2). Start by listing your known variables..."

### **5.2 Fault Tolerance and Scalability**

The integration of Restate ensures that this pipeline scales. If 50 teachers upload exams simultaneously:

* Restate queues the invocations.  
* The "Saga" pattern ensures that if the Vector DB insertion fails, the text extraction is not lost; the system simply retries the DB step.  
* Agno's stateless agent architecture (instantiating in microseconds) allows dynamic scaling of "Indexing Agents" to meet demand without heavy memory overhead.9

## ---

**6\. Implementation Specifications**

This section provides concrete technical specifications for the developers building this system.

### **6.1 BAML Configuration**

The BAML generator must be configured for the Python client.

Rust

// baml\_src/generator.baml  
generator target {  
  output\_type "python/pydantic"  
  output\_dir "../app/baml\_client"  
  default\_client\_mode "async"  
}

This configuration ensures that BAML generates asynchronous Python code, which is required for integration with Agno (which is fully async) and Restate (which uses async/await for suspension).

### **6.2 Agno Knowledge Base Configuration**

The Knowledge object in Agno must be customized to accept the Pydantic models derived from BAML.

Python

from agno.knowledge import Knowledge  
from agno.vectordb.pgvector import PgVector

\# We define a generic wrapper that can handle the dynamic metadata  
class DynamicKnowledge(Knowledge):  
    def add\_dynamic\_document(self, pydantic\_obj: BaseModel):  
        \# Convert the dynamic Pydantic model to a document dict  
        content \= pydantic\_obj.question\_text  
        \# Flatten all other fields into metadata  
        metadata \= pydantic\_obj.model\_dump(exclude={"question\_text"})  
          
        \# Create embedding and store  
        self.vector\_db.upsert(  
            documents=  
        )

### **6.3 Restate Service Definition**

The Restate service acts as the controller.

TypeScript

// grading\_service.ts  
import \* as restate from "@restatedev/restate-sdk";

const gradingService \= restate.service({  
  name: "grading",  
  handlers: {  
    processExam: async (ctx, params) \=\> {  
      // Step 1: Durable Call to BAML Extraction  
      // We wrap the BAML call in a generic durable function  
      const questions \= await ctx.run("extract\_questions", () \=\>   
        callBamlExtraction(params.text, params.syllabusId)  
      );

      // Step 2: Indexing Loop  
      for (const question of questions) {  
        // Step 3: Durable Call to Agno Indexing  
        await ctx.run(\`index\_q\_${question.id}\`, () \=\>   
          agnoClient.index(question)  
        );  
      }  
        
      return { status: "indexed", count: questions.length };  
    }  
  }  
});

This loop demonstrates the granularity of Restate. Even if the server crashes after indexing Question 5, upon restart, Restate knows that the loop for Questions 1-5 completed and resumes at Question 6\.

## ---

**7\. Operational Analysis and Future Outlook**

### **7.1 Performance Considerations**

* **Latency:** The "TypeBuilder" compilation adds a negligible overhead (milliseconds). The primary latency driver is the LLM inference.  
* **Cost:** BAML significantly reduces token usage compared to sending raw JSON schemas. However, Agentic Chunking requires an LLM pass over the entire document *before* indexing. This increases ingestion cost but dramatically reduces search costs (by retrieving accurate contexts) and improves user satisfaction.  
* **Storage:** Storing flattened Pydantic models in JSONB columns in Postgres (via pgvector) is highly efficient and allows for standard SQL indexing on the metadata fields.

### **7.2 Security and Privacy**

Educational data concerning minors requires strict governance.

* **Restate:** Can run entirely on-premise or in a private VPC, ensuring workflow logs never leave the secure environment.  
* **Agno:** Supports local LLMs (via Ollama) and local Vector DBs. This architecture is fully compatible with an "air-gapped" deployment where no student data is sent to public model APIs like OpenAI.

### **7.3 Conclusion**

The "Student/Teacher Project" described here represents a paradigm shift from **Static AI** (chatbots that guess) to **Dynamic AI** (systems that learn the rules). By leveraging BAML to programmatically derive the "laws" of the exam from the syllabus, we ensure high fidelity. By using Agno, we provide the cognitive agency to navigate that data. By using Restate, we ensure the system is robust enough for the classroom.  
This architecture solves the core problem of EdTech scaling: how to handle the infinite variety of curricula without writing infinite lines of custom code. The answer is to write code that writes the schema.

## ---

**8\. Appendix: Component Interaction Matrix**

The following table summarizes the interaction between the three core technologies across different functional requirements of the project.

| Functional Requirement | Agno (Intelligence) | Restate (Orchestration) | BAML (Structure) |
| :---- | :---- | :---- | :---- |
| **Syllabus Parsing** | Hosts the SyllabusAgent | Durable workflow for multi-page parsing | **TypeBuilder** creates the ExamStructure schema. |
| **Exam Indexing** | **Agentic Chunking** splits PDF | Retries failed embedding calls | Extracts structured data using dynamic schema. |
| **Grading** | Agent performs reasoning | **Awakeable** pauses for teacher approval | Enforces grading rubric schema on output. |
| **Student Chat** | Manages **User Memory** & Session | Manages concurrency via **Virtual Objects** | N/A |
| **System Recovery** | Stateless (restarts instantly) | Replays journal to restore state | Stateless (types rebuilt at runtime). |

This matrix illustrates the clean separation of concerns that makes this architecture robust, maintainable, and scalable for institutional use.

#### **Works cited**

1. Agents \- Agno, accessed December 4, 2025, [https://docs.agno.com/basics/agents/overview](https://docs.agno.com/basics/agents/overview)  
2. Agentic Chunking \- Agno, accessed December 4, 2025, [https://docs.agno.com/basics/knowledge/chunking/agentic-chunking](https://docs.agno.com/basics/knowledge/chunking/agentic-chunking)  
3. Tour of Restate for Agents with Vercel AI SDK, accessed December 4, 2025, [https://docs.restate.dev/tour/vercel-ai-agents](https://docs.restate.dev/tour/vercel-ai-agents)  
4. External Events \- Restate docs, accessed December 4, 2025, [https://docs.restate.dev/develop/ts/external-events](https://docs.restate.dev/develop/ts/external-events)  
5. Dynamic Types \- TypeBuilder \- Boundary Documentation \- BAML, accessed December 4, 2025, [https://docs.boundaryml.com/guide/baml-advanced/dynamic-types](https://docs.boundaryml.com/guide/baml-advanced/dynamic-types)  
6. TypeBuilder \- Boundary Documentation \- BAML, accessed December 4, 2025, [https://docs.boundaryml.com/ref/baml\_client/type-builder](https://docs.boundaryml.com/ref/baml_client/type-builder)  
7. What is Chunking? \- Agno, accessed December 4, 2025, [https://docs.agno.com/basics/knowledge/chunking/overview](https://docs.agno.com/basics/knowledge/chunking/overview)  
8. Knowledge Bases \- Agno, accessed December 4, 2025, [https://docs.agno.com/basics/knowledge/knowledge-bases](https://docs.agno.com/basics/knowledge/knowledge-bases)  
9. agno-agi/agno: The open-source stack for building, running and managing multi-agent systems. \- GitHub, accessed December 4, 2025, [https://github.com/agno-agi/agno](https://github.com/agno-agi/agno)  
10. Workflows \- Restate docs, accessed December 4, 2025, [https://docs.restate.dev/tour/workflows](https://docs.restate.dev/tour/workflows)  
11. External Events \- Restate docs, accessed December 4, 2025, [https://docs.restate.dev/develop/go/external-events](https://docs.restate.dev/develop/go/external-events)  
12. Microservice Orchestration \- Restate docs, accessed December 4, 2025, [https://docs.restate.dev/tour/microservice-orchestration](https://docs.restate.dev/tour/microservice-orchestration)  
13. Dynamic models \- Pydantic Validation, accessed December 4, 2025, [https://docs.pydantic.dev/latest/examples/dynamic\_models/](https://docs.pydantic.dev/latest/examples/dynamic_models/)  
14. BAML: The Structured-Output Power Tool Your LLM Workflow Has Been Missing \- Medium, accessed December 4, 2025, [https://medium.com/@manavisrani07/baml-the-structured-output-power-tool-your-llm-workflow-has-been-missing-f326046d019b](https://medium.com/@manavisrani07/baml-the-structured-output-power-tool-your-llm-workflow-has-been-missing-f326046d019b)  
15. The Prompting Language Every AI Engineer Should Know: A BAML Deep Dive | Towards AI, accessed December 4, 2025, [https://towardsai.net/p/machine-learning/the-prompting-language-every-ai-engineer-should-know-a-baml-deep-dive](https://towardsai.net/p/machine-learning/the-prompting-language-every-ai-engineer-should-know-a-baml-deep-dive)  
16. Agno Framework: A Lightweight Library for Building Multimodal Agents \- Analytics Vidhya, accessed December 4, 2025, [https://www.analyticsvidhya.com/blog/2025/03/agno-framework/](https://www.analyticsvidhya.com/blog/2025/03/agno-framework/)