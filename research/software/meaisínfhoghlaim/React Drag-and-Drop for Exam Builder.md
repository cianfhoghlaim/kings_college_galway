# **Architectural Blueprint for an Intelligent, British Curriculum-Aligned Interactive Exam Builder**

## **1\. Introduction: The Convergence of Assessment Rigor and Modern Interactive Architecture**

The digitization of high-stakes assessment infrastructure represents one of the most significant technical challenges in the modern educational technology landscape. The objective of developing an interactive platform capable of indexing the entirety of the British Isles’ syllabus and exam paper corpus—spanning AQA, OCR, Pearson Edexcel, WJEC, and CCEA—while simultaneously providing a fluid, drag-and-drop "Exam Paper Builder" assisted by generative AI, necessitates a departure from traditional CRUD (Create, Read, Update, Delete) application architectures. This report serves as a comprehensive technical analysis and implementation roadmap for such a platform, specifically targeting the integration of **TanStack Start**, **CopilotKit**, and advanced **React** drag-and-drop primitives.  
The project requirements imply a system that must handle high-cardinality data indexing (tens of thousands of past paper questions) while delivering a client-side experience akin to a desktop publishing tool. The user persona—likely a teacher, examiner, or advanced tutor—demands precision. Unlike a generic form builder, an exam builder must respect the semantic hierarchy of the British education system: the distinction between Foundation and Higher tiers, the precise allocation of marks, the nesting of sub-questions (e.g., 1(a), 1(b)), and the rigorous metadata association required for validity. Furthermore, the integration of **CopilotKit** introduces an agentic layer, transforming the application from a passive tool into an active assistant capable of "reasoning" about the exam paper’s balance, difficulty, and coverage.  
This analysis prioritizes architectural robustness, accessibility (a critical legal requirement in UK education), and performance. It evaluates the ecosystem of React drag-and-drop libraries against the specific constraints of **TanStack Start’s** server-side rendering (SSR) model and proposes a data schema compliant with the complexities of the **Joint Council for Qualifications (JCQ)** standards.

## ---

**2\. Domain Analysis: The British Assessment Data Landscape**

To architect the software correctly, one must first rigorously model the domain. The "British Isles" qualification landscape is not a monolith; it is a federated system of competing exam boards and varying national standards (England, Wales, Northern Ireland, and Scotland). The software must ingest, normalize, and present this data without losing the specific pedagogical nuance of each board.

### **2.1 The Structural Heterogeneity of Exam Boards**

The primary exam boards—**Assessment and Qualifications Alliance (AQA)**, **Oxford, Cambridge and RSA (OCR)**, and **Pearson Edexcel**—dominate the landscape in England, while **WJEC** serves Wales and **CCEA** serves Northern Ireland.1 While they all adhere to Ofqual regulations, their data structures differ significantly.

* **AQA** is renowned for straightforward, structured questions, often utilizing multiple-choice sections in sciences which require specific rendering logic (e.g., ticking boxes vs. circling answers).1  
* **OCR** tends towards more interpretive, context-heavy questions, particularly in subjects like History and Computer Science. This implies the "draggable item" on the canvas is not just a question but often a "Resource Booklet" or a "Case Study" block that must remain associated with multiple subsequent questions.1  
* **Pearson Edexcel** in Mathematics is data-heavy and analytical, requiring the rendering of complex statistical diagrams and data tables that must be preserved during the drag-and-drop operation.1

The implication for the drag-and-drop software is that the "atom" of the interface cannot simply be a text string. It must be a **Polymorphic Component** capable of rendering LaTeX formulas, complex tables, and high-resolution vector graphics. Furthermore, the system must handle the **Tiering System** (Foundation vs. Higher). A dragged question from a Foundation paper must carry metadata that warns the user if it is dropped onto a Higher Tier paper, potentially invalidating the assessment's integrity.

### **2.2 The QTI Standard and JSON Schema Design**

While the **IMS Question and Test Interoperability (QTI)** standard 3 is the global interchange format for assessment data, its XML-heavy structure is cumbersome for modern React applications. A "British Exams" specific JSON schema is recommended for internal application state, which can be mapped to QTI for export if necessary.  
The internal data model for a draggable question item must capture specific attributes to enable the **CopilotKit** AI to function effectively. If the AI is to "suggest a follow-up question," it needs access to the question's taxonomy.  
**Table 1: Proposed JSON Schema for Exam Item Metadata**

| Field | Type | Description | Relevance to Architecture |
| :---- | :---- | :---- | :---- |
| id | UUID | Unique identifier. | Essential for dnd-kit key management. |
| board | Enum | AQA, OCR, Edexcel, WJEC, CCEA. | Faceted search filter in Sidebar. |
| qualification | Enum | GCSE, A-Level, AS, BTEC. | Determines complexity and grading logic. |
| taxonomy | Array\<String\> | \`\`. | Critical for CopilotKit useCopilotReadable context. |
| cognitiveLevel | Enum | Knowledge, Application, Evaluation. | AI uses this to balance paper difficulty. |
| content | Block | Array of text, image, or LaTeX blocks. | The payload rendered by the Draggable component. |
| marks | Integer | Maximum marks available. | Used for "Total Marks" calculation state. |
| interactionType | Enum | choiceInteraction, extendedTextInteraction. | Maps to QTI v3.0 standard.3 |

This schema design addresses the limitations found in generic drag-and-drop tutorials, which often assume simple string lists. By structuring the data this way, we enable **TanStack Start** to fetch tailored subsets of data (e.g., "All Algebra questions") via Server Functions, optimizing the payload sent to the client.4

### **2.3 The "Clone" vs. "Move" Paradigm in Exam Building**

In a typical Kanban board (like Trello), moving a card removes it from column A and places it in column B. In an exam builder, the interaction pattern is fundamentally different. The "Syllabus Index" (Sidebar) is an immutable source of truth. Dragging a question from the Sidebar to the Canvas must trigger a **Clone Operation**, creating a new instance of that question on the paper while leaving the original in the index.6  
This "Sidebar-to-Canvas" pattern is the primary architectural driver for the choice of drag-and-drop library. The library must support:

1. **Multiple Containers:** Distinct contexts for the Sidebar (Source) and Exam Paper (Target).  
2. **ID Regeneration:** The ability to generate a new unique ID (e.g., nanoid) on the fly when an item is dropped, preventing React key collisions.8  
3. **Visual Cloning:** The user must see a "ghost" of the question moving while the original remains visible in the sidebar.

## ---

**3\. The Drag-and-Drop Engine: A Technical Evaluation**

The React ecosystem offers several candidates for this functionality: **React-DnD**, **React-Beautiful-DnD** (now effectively deprecated), **dnd-kit**, and Atlassian's new **Pragmatic Drag and Drop**. For this specific application—integrating with TanStack Start and requiring high accessibility—the evaluation points toward a clear winner.

### **3.1 The Case for dnd-kit**

**dnd-kit** emerges as the optimal software choice for the Exam Paper Builder.9 Unlike older libraries, dnd-kit is modular, lightweight, and built specifically for modern React hooks (useDraggable, useDroppable, useSortable).

#### **3.1.1 Headless Architecture**

dnd-kit is headless, meaning it provides the logic and event listeners but assumes nothing about the markup. This is critical for rendering complex exam questions. An exam question might contain a dense HTML structure (tables, MathJax, images). With dnd-kit, you can wrap this complex component in a useSortable hook without fighting against pre-packaged styles or DOM wrappers that break the visual layout of the exam paper.9

#### **3.1.2 Accessibility (A11y)**

The UK public sector (including education) is subject to strict accessibility regulations (WCAG 2.1). dnd-kit is one of the few libraries that prioritizes keyboard-accessible drag-and-drop operations out of the box. It allows users to focus on a question, press Space to lift it, use arrow keys to move it, and Space again to drop it.11 This capability is essential for compliance in an educational tool.

#### **3.1.3 The Sidebar-to-Canvas Implementation Strategy**

To implement the requirement of dragging from an index to a builder, dnd-kit supports a specific pattern involving \<DragOverlay\>.

* **The Sidebar:** Items are wrapped in useDraggable.  
* **The Canvas:** Wrapped in useDroppable and SortableContext.  
* **The Interaction:** When a drag starts from the sidebar, the application renders a \<DragOverlay\> containing a visual copy of the question. This overlay follows the cursor. The original item in the sidebar remains untouched.  
* **The Drop:** On onDragEnd, if the target is the Canvas, the application logic inserts a *copy* of the item data into the Canvas state array, generating a new unique ID.8

### **3.2 The High-Performance Alternative: Pragmatic Drag and Drop**

Atlassian's **Pragmatic Drag and Drop (PDND)** 13 is a newer contender that leverages the browser's native Drag and Drop API.

* **Pros:** It is extremely performant because the browser handles the pixel-pushing of the drag ghost, not React. This is beneficial if the user is dragging a very large, complex component (e.g., a full A4 page question).  
* **Cons:** The native API is historically quirky and less customizable than the pointer-event emulation used by dnd-kit. Styling the "drag image" (the ghost) is restrictive.15  
* **Verdict:** While PDND is faster for massive lists, **dnd-kit** offers a superior developer experience for the *interactions* required here—specifically, the smooth, animated sorting of questions on the canvas, which mimics the tactile feel of arranging physical papers. The "feel" is a crucial metric for user adoption in creative tools.

### **3.3 The Visual Editor Alternative: Puck**

The user query asks for "good software for that type of drag and drop experience." **Puck** 16 is a self-contained "Visual Editor for React."

* **Role:** Puck is essentially a CMS for React components with a drag-and-drop UI pre-built. It solves the "Sidebar to Canvas" problem out of the box.  
* **Integration:** You pass it a config object defining your components (e.g., QuestionBlock, HeaderBlock). Puck handles the sidebar, the drop zones, and the JSON state output.  
* **Limitation:** Puck is opinionated. If your exam paper requires highly specific, non-standard layouts (e.g., side-by-side columns that interact with each other) or complex cross-component validation (e.g., ensuring Question 1a is always followed by 1b), you may find yourself fighting Puck's constraints.  
* **Recommendation:** If the goal is rapid development of a standard layout builder, **Puck** is excellent. However, for a bespoke Exam Builder where the "Question" entity has deep relationships with the "Syllabus" entity, **dnd-kit** provides the necessary architectural control.

## ---

**4\. Full-Stack Architecture with TanStack Start**

Integrating a client-heavy drag-and-drop interface into a server-side rendering (SSR) framework like **TanStack Start** requires a precise management of the execution boundary.

### **4.1 The Client-Server Boundary Dilemma**

Drag-and-drop libraries rely entirely on the browser's DOM (window, document, pointer events). These do not exist on the server. Attempting to render a DndContext or a Draggable component during the SSR pass of TanStack Start will result in hydration mismatches (where the server HTML differs from the client's initial render) or outright crashes.18  
Architectural Pattern: Selective Hydration with \<ClientOnly\>  
TanStack Start provides the \<ClientOnly\> component (or Suspense boundaries with lazy loading) to handle this. The Exam Builder route should be architected such that the heavy interaction logic is strictly client-side.

TypeScript

// routes/builder.tsx  
import { ClientOnly } from '@tanstack/react-start'  
import { ExamBuilderSkeleton } from '@/components/skeletons'

export default function BuilderPage() {  
  return (  
    \<div className="builder-layout"\>  
      {/\* The Sidebar index can be SSR'd for SEO/Speed \*/}  
      \<SyllabusSidebar /\>   
        
      {/\* The Interactive Builder is Client Only \*/}  
      \<ClientOnly fallback={\<ExamBuilderSkeleton /\>}\>  
        {() \=\> import('@/features/builder/ExamCanvas').then(m \=\> m.ExamCanvas)}  
      \</ClientOnly\>  
    \</div\>  
  )  
}

This hybrid approach leverages TanStack Start's strengths: the syllabus sidebar (containing the indexed links) is server-rendered for SEO, while the builder canvas loads as a dynamic application.18

### **4.2 Handling Massive Indexes with Server Functions**

The user mentions "indexing syllabus and exam papers of all subjects." This dataset will be massive—potentially millions of rows if indexing down to the question level across decades of papers. Sending this entire index to the client is impossible.  
**TanStack Start Server Functions (createServerFn)** are the solution.4 Instead of a traditional REST API, createServerFn allows the frontend to call backend logic directly with type safety.  
**Implementation Strategy:**

1. **Input Validation:** Use Zod to validate the search filters (e.g., { board: 'AQA', topic: 'Algebra' }).4  
2. **Database Query:** The server function executes a query against a vector database or search engine (like Postgres with pgvector or Meilisearch) to find relevant questions.  
3. **Serialization:** The function returns a lightweight JSON array of question metadata (IDs, snippets, tags) to the frontend Sidebar.  
4. **Streaming:** For large result sets, TanStack Start supports streaming. The sidebar can begin rendering the first 20 results while the rest are still being fetched, ensuring the UI remains responsive.20

**Table 2: Data Fetching Strategy per Component**

| Component | Fetching Strategy | Rationale |
| :---- | :---- | :---- |
| **Syllabus Index (SEO Pages)** | loader (SSR) | Content must be visible to search engines. |
| **Builder Sidebar (Search)** | useServerFn (Client-invoked) | Interactive filtering; reduces initial bundle size. |
| **Question Detail (Modal)** | useQuery \+ serverFn | Lazy-load heavy assets (images/LaTeX) only on demand. |
| **Paper Persistence (Save)** | createServerFn (Mutation) | Secure write access to the user's exam paper database. |

### **4.3 Virtualization for Performance**

Rendering thousands of draggable items in the sidebar will degrade performance. **TanStack Virtual** must be integrated with the drag-and-drop list. This creates a technical challenge: if a user drags an item and scrolls the list, the virtualizer unmounts the source item. **dnd-kit** handles this gracefully by using the \<DragOverlay\>, which portals the dragged item to the document body, ensuring it persists visually even if its source component is virtualized out of existence.21

## ---

**5\. Integrating CopilotKit: The AI Teaching Assistant**

The integration of **CopilotKit** transforms the Exam Builder from a static tool into an intelligent agent. The AI's role is to understand the pedagogical context of the exam paper and assist the teacher in constructing it.

### **5.1 Context Awareness with useCopilotReadable**

The primary mechanism for giving the AI "sight" is the useCopilotReadable hook.22 This hook feeds the application state (the current list of questions on the canvas) into the LLM's context window.  
Challenge: The Context Window Limit  
An exam paper might contain 50 questions, each with extensive text, marking schemes, and metadata. Feeding the raw JSON of 50 questions into the LLM context (which might be 8k or 32k tokens depending on the model) will quickly exhaust the limit, leading to "context forgetting" or high costs.23  
Optimization Strategy: Hierarchical Summarization  
Instead of feeding the full content, the application should feed a semantic summary to useCopilotReadable.

TypeScript

// Optimized Context for Copilot  
useCopilotReadable({  
  description: "The current exam paper structure",  
  value: examPaperItems.map(item \=\> ({  
    id: item.id,  
    topic: item.meta.topic, // e.g. "Calculus"  
    difficulty: item.meta.difficulty, // e.g. "Hard"  
    marks: item.meta.marks, // e.g. 5  
    summary: item.meta.shortDescription // "Integration by parts question"  
  }))  
});

This lightweight representation allows the AI to reason about the *structure* ("This paper has too much Calculus and not enough Trigonometry") without consuming tokens on the verbatim question text. If the AI needs the full text of a specific question (e.g., to rewrite it), it can request it via a specific tool call.25

### **5.2 Agentic Actions with useCopilotAction**

The useCopilotAction hook enables the AI to manipulate the exam paper.26 This enables powerful "generative UI" workflows.  
**Use Case: "Balance this Paper"**

1. **User Prompt:** "The paper is too hard. Replace the last two questions with easier ones on the same topic."  
2. **Copilot Logic:**  
   * Analyzes the current state via useCopilotReadable.  
   * Identifies the last two questions and their topics.  
   * Calls a custom action: findAndReplaceQuestion({ targetId: '...', difficulty: 'Easy', topic: '...' }).  
3. **Action Implementation:**  
   * The action handler invokes a **TanStack Start Server Function** to search the vector database for "Easy" questions on that topic.  
   * It updates the local React state (the examPaperItems array) with the new questions.  
4. **Result:** The UI updates instantly via the drag-and-drop engine's state, showing the new questions on the canvas.

Warning: Hallucination Risk  
In education, accuracy is paramount. The AI should never generate a question from scratch (hallucinate) if it can be avoided, as it might introduce mathematical errors or curriculum deviations. The architecture must enforce a Retrieval Augmented Generation (RAG) pattern where the AI uses tool calls to find validated past paper questions from the index rather than inventing them.

### **5.3 Copilot Textarea for Rubrics**

The builder likely includes text areas for "Rubrics" (instructions to candidates). The \<CopilotTextarea\> component 27 is perfect here. It acts as an AI-enhanced autocomplete. A teacher can type "Standard AQA Physics instructions," and the Copilot can autocomplete the specific required legal text for that exam board ("Calculators are permitted," "Show all working," etc.).

## ---

**6\. Security, Compliance, and Data Sovereignty**

Operating within the UK education sector imposes specific data governance requirements.

### **6.1 GDPR and Student Data**

While the Syllabus Index is public data, any "Exam Papers" created by teachers might contain student data or proprietary school information.

* **Data Residency:** The database and hosting (TanStack Start server) should ideally reside in UK-based regions (e.g., AWS London, Azure UK South) to simplify GDPR compliance.  
* **Authentication:** Integration with **Clerk** or **Auth0** is recommended for managing teacher accounts. TanStack Start has first-class integration patterns for Clerk, allowing route protection at the middleware level.28

### **6.2 Preventing AI Abuse**

There is a risk of students using the tool to generate "answers" or bypass learning.

* **Role-Based Access Control (RBAC):** The "Teacher" persona should have access to useCopilotAction for generating marking schemes. The "Student" persona (if accessing the platform for practice) should have the AI features restricted to "Tutor Mode" (hints only), preventing the AI from revealing full answers. This logic can be enforced in the TanStack Start Server Functions that power the AI actions.

## ---

**7\. Performance Optimization & Infrastructure**

To ensure the "drag and drop experience" is smooth (60fps), rigorous performance optimization is required.

### **7.1 Optimistic Updates with TanStack Query**

When a user drops a question, the UI must reflect the change immediately. Waiting for a server round-trip (to save the paper) will make the drag-and-drop feel sluggish.

* **Strategy:** Use TanStack Query's onMutate to optimistically update the client cache. The visual state changes instantly. The background sync to the server happens asynchronously. If the server save fails (e.g., network error), TanStack Query automatically rolls back the UI change and shows an error toast.29

### **7.2 Code Splitting and Bundle Size**

The combination of dnd-kit, CopilotKit, and TanStack Start can lead to a large JavaScript bundle.

* **Route-Based Splitting:** TanStack Start automatically splits code by route. The "Exam Builder" code should not be loaded on the "Home Page."  
* **Lazy Loading:** Heavy components like the "Math Renderer" (KaTeX/MathJax) or "PDF Previewer" should be lazy-loaded using React.lazy and Suspense. They should only be fetched when the user actually adds a math question or clicks "Preview".31

## ---

**8\. Conclusion and Strategic Roadmap**

The development of a British Isles-focused Interactive Exam Builder is a sophisticated engineering endeavor that transcends simple web development. It requires a symbiotic integration of structured data (the syllabus index), fluid interaction (the drag-and-drop builder), and intelligent assistance (CopilotKit).  
**Summary of Recommendations:**

1. **Architecture:** Adopt **TanStack Start** to hybridize the application. Use Server-Side Rendering for the public Syllabus Index (SEO) and Client-Side Rendering (wrapped in ClientOnly) for the interactive Builder.  
2. **Interaction Engine:** Select **dnd-kit** over competitors. Its headless nature allows for the precise, accessible rendering of complex exam questions, and its architecture supports the critical "Sidebar-to-Canvas cloning" pattern required for this domain.  
3. **Artificial Intelligence:** Leverage **CopilotKit** not just as a chatbot, but as a state-aware operator. Feed it summarized metadata via useCopilotReadable to respect context windows, and restrict its generative capabilities to RAG-based retrieval to ensure curriculum accuracy.  
4. **Data Strategy:** Implement a robust JSON schema that mirrors the **QTI** standard but is optimized for the semantic nuances of AQA, OCR, and Edexcel exams.

**Implementation Roadmap:**

* **Phase 1: The Index.** Build the TanStack Start application with Server Functions to ingest and search a sample dataset of past papers. Focus on the Zod schema validation and database performance.  
* **Phase 2: The Core Builder.** Implement the dnd-kit engine. Build the "Sidebar" (Draggable) and "Canvas" (Sortable/Droppable). Achieve the "Clone on Drop" functionality with ID regeneration.  
* **Phase 3: The Copilot.** Integrate useCopilotReadable. Train the system to "understand" the paper structure. Implement the first action: "Search and Add Question."  
* **Phase 4: Refinement.** Address accessibility (keyboard navigation for the builder) and implement the PDF generation pipeline for the final artifact.

This platform represents the next generation of EdTech tools: ones that do not just store content, but actively assist educators in determining the best way to assess their students.

## **9\. Detailed Architecture Specifications**

### **9.1 Data Schema: The British Exam Model**

A robust data model is the foundation of the system. The schema must handle the idiosyncrasies of British exams, such as "nested" questions (e.g., 1a, 1b, 1c) and shared resource blocks (e.g., a reading passage shared by questions 1-5).  
**Proposed JSON Structure:**

JSON

{  
  "examPaper": {  
    "id": "uuid-paper-1",  
    "title": "GCSE Math Mock \- Higher Tier",  
    "metadata": {  
      "board": "AQA",  
      "level": "GCSE",  
      "tier": "Higher",  
      "year": 2024  
    },  
    "sections":  
            },  
            "markingScheme": "..."  
          },  
          {  
            "type": "resource",  
            "id": "res-456",  
            "content": "Refer to Source A...",  
            "linkedQuestions": \["q-124", "q-125"\] // Complexity: Shared Context  
          }  
        \]  
      }  
    \]  
  }  
}

**Architectural Implication:** The drag-and-drop system must handle sections as sortable containers. The linkedQuestions field implies that dragging a "Resource" might inherently drag the questions attached to it—a "Group Drag" feature supported by dnd-kit via custom modifiers or multi-drag strategies.

### **9.2 Copilot Context Strategy: Managing the Window**

When a user builds a large paper (e.g., 20 pages), the JSON state grows massive. Passing the entire object to CopilotKit will hit token limits (e.g., 8k tokens) or cause latency.  
Strategy: Context Pruning and Summarization  
We must use a "Level of Detail" (LOD) approach for the AI context.

1. **High-Level Context (Always On):** The AI receives a summary of the paper structure.  
   * *Example:* "Paper contains 3 Sections. Section A has 5 Algebra questions (Total 15 marks). Section B has 2 Geometry questions..."  
2. **Focused Context (On Demand):** When the user asks about a specific question ("Rewrite Question 3"), the system (or the AI via a tool call) retrieves the *full* text of just that question and injects it into the immediate context.

This mimics how a human works: keeping the "Table of Contents" in memory but reading specific pages only when necessary.

### **9.3 Server-Side Rendering (SSR) vs. Interactive Shells**

The **Syllabus Index** is a classic SEO target. A teacher searching Google for "AQA GCSE Biology Past Papers" should land on your indexed page.

* **TanStack Start Implementation:** Use loader functions in the route definition to fetch the syllabus tree on the server. This renders full HTML for bots.

The **Builder** is an application. SEO matters less, but load time matters.

* **TanStack Start Implementation:** Use deferred loading for the sidebar data. The shell of the builder loads instantly. The heavy list of 5,000 draggable questions streams in afterwards. This prevents the "White Screen of Death" while the database query executes.

### **9.4 Accessibility: The Legal Imperative**

In the UK, educational software must often comply with **WCAG 2.1 AA** standards.

* **Keyboard Navigation:** dnd-kit is superior here. It introduces a specific "keyboard sensor." A user tabs to a question, hears "Question 1, Algebra, Draggable," presses Space, uses Up/Down arrows to reorder, and presses Space to drop.  
* **Screen Readers:** You must ensure the aria-describedby attributes on the draggable items provide context. "Question 1, moved to position 3." dnd-kit provides an Announcements prop specifically for this, allowing you to define the voiceover strings for every drag event.

## **10\. Final Recommendation Summary**

For the developer tasked with this project, the path forward is clear but rigorous. You are not just building a web app; you are building a specialized authoring tool.

1. **Adopt TanStack Start** as the framework to bridge the gap between the SEO-heavy syllabus index and the client-heavy builder.  
2. **Commit to dnd-kit** as the interaction engine. Its accessibility features and headless architecture are the only viable path for the complex, nested, and accessible interfaces required by British education standards.  
3. **Architect for AI Agency** with CopilotKit. Do not treat AI as a gimmick. architect the data flow (via useCopilotReadable) so the AI has a "mental model" of the exam paper, and restrict its output (via useCopilotAction) to valid, retrieved curriculum content to ensure the tool remains a trustworthy educational resource.

This architecture ensures the platform is scalable, legally compliant, and genuinely useful to the educators it aims to serve.

#### **Works cited**

1. AQA vs Edexcel vs OCR (2025): Which GCSE Exam Board Is Best? \- Tutopiya, accessed December 14, 2025, [https://www.tutopiya.com/tools/blog/gcse-exam-boards-comparison-aqa-vs-edexcel-vs-ocr-2025](https://www.tutopiya.com/tools/blog/gcse-exam-boards-comparison-aqa-vs-edexcel-vs-ocr-2025)  
2. A Complete Guide to Every GCSE Exam Board \- Ivy Education, accessed December 14, 2025, [https://www.ivyeducation.co.uk/insights/gcse-exam-boards](https://www.ivyeducation.co.uk/insights/gcse-exam-boards)  
3. Question & Test Interoperability (QTI) 3.0 Best Practices and Implementation Guide, accessed December 14, 2025, [https://www.imsglobal.org/spec/qti/v3p0/impl](https://www.imsglobal.org/spec/qti/v3p0/impl)  
4. Server Functions | TanStack Start React Docs, accessed December 14, 2025, [https://tanstack.com/start/latest/docs/framework/react/guide/server-functions](https://tanstack.com/start/latest/docs/framework/react/guide/server-functions)  
5. Building a Full Stack DevJokes App with TanStack Start, accessed December 14, 2025, [https://tanstack.com/start/latest/docs/framework/react/tutorial/reading-writing-file](https://tanstack.com/start/latest/docs/framework/react/tutorial/reading-writing-file)  
6. dnd-kit-drag-from-sidebar-clone-item (forked) \- CodeSandbox, accessed December 14, 2025, [https://codesandbox.io/s/dnd-kit-drag-from-sidebar-clone-item-forked-h9936g](https://codesandbox.io/s/dnd-kit-drag-from-sidebar-clone-item-forked-h9936g)  
7. dndkit-drag-clone \- Codesandbox, accessed December 14, 2025, [https://codesandbox.io/p/sandbox/dndkit-drag-clone-xfpzrk](https://codesandbox.io/p/sandbox/dndkit-drag-clone-xfpzrk)  
8. Primitive Example of dragging from source list and cloning into sortable destination list · clauderic dnd-kit · Discussion \#1452 \- GitHub, accessed December 14, 2025, [https://github.com/clauderic/dnd-kit/discussions/1452](https://github.com/clauderic/dnd-kit/discussions/1452)  
9. 8 Best React Form Libraries for Developers (2025) \- Snappify, accessed December 14, 2025, [https://snappify.com/blog/best-react-form-libraries](https://snappify.com/blog/best-react-form-libraries)  
10. Top 5 Drag-and-Drop Libraries for React in 2025 \- Puck, accessed December 14, 2025, [https://puckeditor.com/blog/top-5-drag-and-drop-libraries-for-react](https://puckeditor.com/blog/top-5-drag-and-drop-libraries-for-react)  
11. React Drag & Drop Made Easy with @dnd-kit \- YouTube, accessed December 14, 2025, [https://www.youtube.com/watch?v=ZALLXGVc\_HU](https://www.youtube.com/watch?v=ZALLXGVc_HU)  
12. Simplified Drag-and-Drop with @dnd-kit in React | by Nov | Medium, accessed December 14, 2025, [https://medium.com/@novadwynt28/simplified-drag-and-drop-with-dnd-kit-in-react-99394aa27322](https://medium.com/@novadwynt28/simplified-drag-and-drop-with-dnd-kit-in-react-99394aa27322)  
13. Tutorial \- index \- Components \- Atlassian Design System, accessed December 14, 2025, [https://atlassian.design/components/pragmatic-drag-and-drop/tutorial](https://atlassian.design/components/pragmatic-drag-and-drop/tutorial)  
14. Pragmatic drag and drop \- About \- Components \- Atlassian Design System, accessed December 14, 2025, [https://atlassian.design/components/pragmatic-drag-and-drop](https://atlassian.design/components/pragmatic-drag-and-drop)  
15. Designed for delight, built for performance: The journey of pragmatic drag and drop \- Work Life by Atlassian, accessed December 14, 2025, [https://www.atlassian.com/blog/design/designed-for-delight-built-for-performance](https://www.atlassian.com/blog/design/designed-for-delight-built-for-performance)  
16. puckeditor/puck: The visual editor for React \- GitHub, accessed December 14, 2025, [https://github.com/puckeditor/puck](https://github.com/puckeditor/puck)  
17. Puck, a visual editor for React. \- Medium, accessed December 14, 2025, [https://medium.com/@ramunarasinga/puck-a-visual-editor-for-react-13fa72b6587b](https://medium.com/@ramunarasinga/puck-a-visual-editor-for-react-13fa72b6587b)  
18. Selective Server-Side Rendering (SSR) | TanStack Start React Docs, accessed December 14, 2025, [https://tanstack.com/start/latest/docs/framework/react/guide/selective-ssr](https://tanstack.com/start/latest/docs/framework/react/guide/selective-ssr)  
19. Only render component on the client? \- TanStack \- Answer Overflow, accessed December 14, 2025, [https://www.answeroverflow.com/m/1399758080362680343](https://www.answeroverflow.com/m/1399758080362680343)  
20. TanStack Start, accessed December 14, 2025, [https://tanstack.com/start](https://tanstack.com/start)  
21. The dragged item is not displayed outside the virtualized list (react-virtuoso, @dnd-kit), accessed December 14, 2025, [https://stackoverflow.com/questions/76785503/the-dragged-item-is-not-displayed-outside-the-virtualized-list-react-virtuoso](https://stackoverflow.com/questions/76785503/the-dragged-item-is-not-displayed-outside-the-virtualized-list-react-virtuoso)  
22. useCopilotReadable \- CopilotKit docs, accessed December 14, 2025, [https://docs.copilotkit.ai/reference/hooks/useCopilotReadable](https://docs.copilotkit.ai/reference/hooks/useCopilotReadable)  
23. Understanding the Impact of Increasing LLM Context Windows \- Meibel, accessed December 14, 2025, [https://www.meibel.ai/post/understanding-the-impact-of-increasing-llm-context-windows](https://www.meibel.ai/post/understanding-the-impact-of-increasing-llm-context-windows)  
24. Context Window Guide | DevClarity, accessed December 14, 2025, [https://www.devclarity.ai/resources/context-window-for-ai-tools-and-models](https://www.devclarity.ai/resources/context-window-for-ai-tools-and-models)  
25. If you think Copilot's context window is too small, try this workflow : r/GithubCopilot \- Reddit, accessed December 14, 2025, [https://www.reddit.com/r/GithubCopilot/comments/1phnj0e/if\_you\_think\_copilots\_context\_window\_is\_too\_small/](https://www.reddit.com/r/GithubCopilot/comments/1phnj0e/if_you_think_copilots_context_window_is_too_small/)  
26. CopilotKit v1.50 Brings AG-UI Agents Directly Into Your App With the New useAgent Hook, accessed December 14, 2025, [https://www.marktechpost.com/2025/12/11/copilotkit-v1-50-brings-ag-ui-agents-directly-into-your-app-with-the-new-useagent-hook/](https://www.marktechpost.com/2025/12/11/copilotkit-v1-50-brings-ag-ui-agents-directly-into-your-app-with-the-new-useagent-hook/)  
27. CopilotKit docs, accessed December 14, 2025, [https://docs.copilotkit.ai/](https://docs.copilotkit.ai/)  
28. TanStack React Start Quickstart (beta) \- Clerk, accessed December 14, 2025, [https://clerk.com/docs/tanstack-react-start/getting-started/quickstart](https://clerk.com/docs/tanstack-react-start/getting-started/quickstart)  
29. Mutations | TanStack Query Angular Docs, accessed December 14, 2025, [https://tanstack.com/query/v5/docs/framework/angular/guides/mutations](https://tanstack.com/query/v5/docs/framework/angular/guides/mutations)  
30. Mutations | TanStack Query React Docs, accessed December 14, 2025, [https://tanstack.com/query/v5/docs/react/guides/mutations](https://tanstack.com/query/v5/docs/react/guides/mutations)  
31. ClientOnly Component | TanStack Router React Docs, accessed December 14, 2025, [https://tanstack.com/router/v1/docs/framework/react/api/router/clientOnlyComponent](https://tanstack.com/router/v1/docs/framework/react/api/router/clientOnlyComponent)