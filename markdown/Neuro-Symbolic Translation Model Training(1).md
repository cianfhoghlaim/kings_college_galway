# **Architectural Blueprint for the Neuro-Symbolic Gaeilge Engine: Integrating InkSpire Diffusion Architectures with Sovereign AI Infrastructure for High-Fidelity Technical Translation**

## **1\. Executive Summary and Theoretical Framework**

The preservation and digital activation of low-resource languages, particularly those characterized by complex morphological structures and deep cultural significance such as Irish (*Gaeilge*), necessitates a fundamental paradigm shift from traditional stochastic machine translation to architecturally robust, neuro-symbolic systems. This research report delineates the technical specifications for the "Neuro-Symbolic Gaeilge Engine," a sovereign AI system designed to facilitate high-fidelity English-to-Irish translation of technical educational materials. The proposed system utilizes the Leaving Certificate Chemistry and Computer Science curriculum specifications as a foundational bilingual dataset. These documents, rich in domain-specific taxonomy and hierarchical pedagogical structures, serve as the ground truth for training a next-generation generative model. The core innovation of this blueprint lies in the integration of the 'InkSpire' diffusion model architecture—originally designed for stylized handwriting synthesis—adapted here to unify source semantic content (English technical specifications) and target linguistic features (Irish syntax and orthography) within a shared latent space.1  
To achieve this, the system employs a sophisticated "Cognitive Federation" of infrastructure components: Agno for high-performance agentic orchestration, Cocoindex for immutable incremental dataflow, Cognee for semantic knowledge graph construction, and BAML (Boundary AI Markup Language) for rigorous schema compliance and ontology enforcement. This report provides an exhaustive technical analysis of how these components converge to prepare structured data for training a Masked Conditional Flow Matching (CFM) diffusion model, capable of generating Irish technical documentation that preserves both semantic precision and cultural authenticity.

### **1.1 The Sovereign AI Imperative**

Current approaches to automated translation often rely on monolithic Large Language Models (LLMs) which, while capable of fluent text generation, frequently suffer from "hallucination" and a lack of structural adherence, particularly when dealing with the strict formatting requirements of state examination papers. The "Sovereign AI" blueprint proposed here rejects the fragility of these monolithic calls in favor of a modular, neuro-symbolic pipeline.1 By fusing the probabilistic reasoning of Large Multimodal Models (LMMs) with deterministic logic and strictly typed data structures, the system ensures that the translation of sensitive educational material is accurate, consistent, and structurally sound.

### **1.2 The Bilingual Ground Truth**

The datasets selected for this architectural validation are the *Leaving Certificate Computer Science Curriculum Specification* (English and Irish versions) 1 and the *Leaving Certificate Chemistry Curriculum Specification* (English and Irish versions).1 These documents represent the "gold standard" of technical Irish. They contain not just general vocabulary, but highly specific nomenclature—such as *stócaiméadracht* (stoichiometry) in Chemistry and *algartaim* (algorithms) in Computer Science—embedded within a rigid hierarchical structure of "Strands" and "Learning Outcomes." The successful translation of such documents requires a system that understands not just the words, but the pedagogical architecture they inhabit.

## ---

**2\. The Agentic Control Plane: Agno Implementation**

In the orchestration of complex cognitive architectures, the latency and overhead introduced by agent frameworks can often become a bottleneck, particularly when processing high-volume archival datasets. **Agno** (formerly Phidata) functions as the central nervous system of the Neuro-Symbolic Gaeilge Engine, selected specifically for its "pure Python" design philosophy which prioritizes minimalist code structures over heavy abstractions.1

### **2.1 Architectural Philosophy and Performance Optimization**

The ingestion of dense PDF specifications, such as the 44-page Chemistry curriculum or the Computer Science specification, requires an agentic framework capable of rapid instantiation and tear-down. Agno provides an agentic control plane where agent instantiation occurs in approximately 3 microseconds.1 This micro-latency is critical for a system designed to spawn thousands of ephemeral micro-agents, each tasked with analyzing a specific learning outcome, assessment criterion, or diagrammatic element without incurring significant computational debt. Unlike heavier frameworks that burden the runtime with extensive boilerplate, Agno’s lightweight footprint allows the Sovereign AI system to scale horizontally, processing multiple curriculum strands in parallel.

### **2.2 Hierarchical Team Topology**

To manage the complexity of the bilingual dataset effectively, Agno orchestrates a **Hierarchical Team Structure**.1 This topology is designed to prevent "context pollution"—a common failure mode in agentic systems where an agent confuses instructions from different domains (e.g., applying the assessment criteria of Chemistry to the syntactic rules of Computer Science code). The architecture segregates duties into specialized personas, creating a "Cognitive Federation" of agents:

* **The Chief Examiner (Orchestrator):** This agent acts as the primary entry point for the system. It receives the raw PDF documents (e.g., LC-Computer-Science-specification-updated.pdf) and decomposes the ingestion task into sub-processes using Agno’s AgentTeam class.1 The Chief Examiner analyzes the document structure, identifying the boundaries between the "Introduction," "Strands," and "Assessment" sections. It determines whether a specific section requires visual parsing (for diagrams and tables) or textual extraction and delegates the task accordingly to the specialized sub-agents.  
* **The Palaeographer (Vision Specialist):** While standard Optical Character Recognition (OCR) is sufficient for plain text, the Leaving Certificate specifications contain complex visual elements that carry significant semantic weight. For instance, the Chemistry specification contains molecular structures and reaction diagrams 1, while the Computer Science specification includes logic gate diagrams and flowcharts.1 The Palaeographer utilizes the **Z.ai GLM-4.6v** model via a custom **Model Context Protocol (MCP)** client to visually perceive and interpret these elements.1 This agent does not merely transcribe text; it generates a semantic description of the visual data, ensuring that the "visual semantics" of the source material—such as the spatial arrangement of a logic gate—are captured alongside the text.  
* **The Ontologist (Structure Specialist):** This agent is responsible for mapping raw extracted text into the strict ontology defined by the Irish State Examinations Commission (SEC). It utilizes **BAML** tools to parse text into strictly typed JSON objects, ensuring that a "Learning Outcome" in English is correctly associated with its Irish equivalent (*Toradh Foghlama*).1 The Ontologist ensures that the hierarchical relationship between a "Strand" (*Snáithe*) and its constituent "Key Concepts" (*Príomhchoincheapa*) is preserved in the data structure.

### **2.3 Custom Model Integration via MCP**

Agno’s extensibility allows for the seamless integration of specialized inference engines, a feature leveraged heavily in this blueprint. The system extends the OpenAILike model class to support **Z.ai’s GLM-4.6**, enabling a specific "Thinking Mode" for complex reasoning tasks.1 For example, when translating a complex chemical principle involving "stoichiometry" (*stócaiméadracht*) 1, the agent can engage in a multi-step reasoning chain to verify dialectal accuracy before finalizing the output. Furthermore, Agno leverages **Model Context Protocol (MCP)** servers to wrap vision capabilities as modular tools (e.g., analyze\_image), allowing the Palaeographer agent to "see" the document layout without embedding visual processing logic directly into the agent's core code.1 This decoupling ensures that the vision models can be upgraded or swapped without disrupting the orchestration logic.

## ---

**3\. The Dataflow Engine: Cocoindex Implementation**

Data integrity and lineage are paramount when dealing with national curriculum standards. **Cocoindex** provides the robust, incremental Extract, Transform, Load (ETL) pipeline that manages the flow of data from the raw PDF specifications to the structured database. It ensures that the system operates on an "immutable dataflow" principle, where every transformation is recorded, verifiable, and reproducible.

### **3.1 Dataflow Programming and Immutable Lineage**

Cocoindex treats data transformations as a **Directed Acyclic Graph (DAG)**.1 Each node in the graph represents a specific transformation step (e.g., PDF\_to\_Image, Extract\_Text, Generate\_Embedding, Map\_Ontology). Crucially, Cocoindex employs **memoization** for every step in this graph. This means that the system maintains a cryptographic record of every transformation result. If the pipeline is interrupted due to a system crash or network failure, it can resume exactly where it left off without re-processing successful steps. This feature is essential for processing long, multipage documents like the 44-page Chemistry specification 1, ensuring that a failure on page 43 does not require a computationally expensive restart from page 1\.

### **3.2 Incremental State Management and Cost Optimization**

A key efficiency mechanism within Cocoindex is its use of **PostgreSQL** as an internal state store to track data lineage via content hashes.1 The system calculates a hash for every file and every individual page within that file.

* **Delta Detection:** If the Department of Education releases an updated version of the Computer Science specification where only the "Assessment" section has changed, Cocoindex detects the delta immediately. It recognizes that the content hashes for the "Strand 1" and "Strand 2" sections remain unchanged.  
* **Selective Processing:** Consequently, it triggers the transformation logic (and the associated expensive LLM inference costs) *only* for the modified pages.1 This makes the pipeline economically viable for continuous integration of curriculum updates, ensuring the training dataset remains current without wasteful re-computation.

### **3.3 Orchestration of Cognitive Steps**

The Cocoindex pipeline creates a structured bridge between raw data and cognitive processing. It defines a flow that orchestrates the entire ingestion lifecycle:

1. **Source Monitoring:** The pipeline watches local repositories for the bilingual PDF datasets (LC-Computer-Science-specification-updated-as-Gaeilge.pdf, SC-Chemistry-Specification-EN.pdf, etc.).1  
2. **Pre-processing Transformation:** It uses PyMuPDF to convert PDF pages into high-resolution images for the Vision Agent and simultaneously extracts raw text layers for the Ontologist.1 This dual-stream approach ensures that both visual layout and semantic text are available for analysis.  
3. **The Cognitive Step:** Cocoindex invokes the Agno agents as custom transformation functions. This is the stage where raw data is "enriched" with semantic meaning. The Agno agent analyzes the text and returns structured metadata (e.g., identifying that a specific paragraph belongs to "Strand 1: Practices and Principles").1  
4. **Vector Embedding:** The system generates high-dimensional embeddings (e.g., using all-MiniLM-L6-v2) for the extracted content.1 These embeddings facilitate semantic search and alignment between the English and Irish texts.  
5. **Persistence:** Finally, the pipeline exports the enriched, structured data to PostgreSQL, automatically handling pgvector indexing for downstream retrieval.1

## ---

**4\. Semantic Architecture: Cognee and the Knowledge Graph**

While Cocoindex handles the mechanical flow of data, **Cognee** is responsible for structuring that data into a navigable **Knowledge Graph (Semantic Graph)** within the database. This component moves the system beyond simple keyword search (RAG) to true semantic understanding and reasoning.

### **4.1 Cognitive Federation and Graph Orchestration**

Cognee builds upon the "raw material" indexed by Cocoindex.1 It takes the enriched JSON objects and vector embeddings produced by the Agno agents and maps them into a rigorous graph structure stored in **PostgreSQL** and **LanceDB**.1 In the context of the Leaving Certificate dataset, Cognee constructs nodes for entities such as "Subject," "Strand," "Learning Outcome," "Key Skill," and "Assessment Criterion." It then defines the edges (relationships) between them, such as "belongs\_to," "assesses," "requires," and "is\_translation\_of."  
For example, a node representing "Computational Thinking" (*Smaointeoireacht ríomhaireachtúil*) in the Computer Science graph is linked to the "Problem Solving" node via an "is\_component\_of" edge. This structure allows the system to understand the pedagogical relationships inherent in the curriculum, rather than just treating the text as isolated strings.

### **4.2 Cross-Lingual Semantic Mapping**

Cognee is pivotal for the translation system because it establishes the semantic equivalence between English and Irish terms within the graph. It maps the node "Standard Deviation" in the English Mathematics graph to the node "Diall Caighdeánach" in the Irish graph.1 Similarly, in the Chemistry specification, it links the node for "Stoichiometry" to "Stócaiméadracht." This explicit mapping ensures that the InkSpire model, during its training phase, understands that these two terms represent the exact same mathematical or scientific concept, despite their linguistic difference. This "semantic anchoring" prevents the model from hallucinating incorrect translations for technical terms.

### **4.3 Deep Reasoning Capabilities**

By structuring the curriculum as a graph, Cognee enables the system to perform complex reasoning tasks that would be impossible with flat text retrieval. The system can answer queries that require traversing relationships, such as "How do the key skills in the Chemistry specification influence the assessment criteria for coursework?" or "Find all learning outcomes in Computer Science Strand 2 that reference 'Abstraction' (*Teibíocht*) and link them to their corresponding assessment metrics".1 This depth of understanding is necessary to prepare high-quality, context-aware training data for the diffusion model, ensuring that the generated Irish text is not just grammatically correct but pedagogically aligned.

## ---

**5\. Logical Guardrails: BAML Implementation**

**BAML** (Boundary AI Markup Language) serves as the deterministic enforcement layer that governs the non-deterministic outputs of the LLMs. In a system designed for educational standards, accuracy is non-negotiable. BAML ensures that the data entering the training pipeline is rigorously structured and schema-compliant.

### **5.1 Strict Structured Output and Ontology Definition**

BAML addresses the inherent "probabilistic" nature of LLMs by enforcing strict output schemas. It defines the **Leaving Certificate Ontology** in .baml files, specifying the exact hierarchy of the documents.1 This ontology defines:

* **Paper Level:** Metadata such as Year (e.g., 2023, 2025), Level (Ordinary/Higher), and Subject (Chemistry/Computer Science).  
* **Section Level:** Distinctions between structural elements like "Strand 1: Practices and Principles" (*Cleachtais agus prionsabail*) and "Strand 2: Core Concepts" (*Príomhchoincheapa*).  
* **Outcome Level:** Nested structures for specific learning outcomes, ensuring that bullet points, sub-clauses, and numbering systems are captured as structured list elements rather than unstructured blobs of text.

### **5.2 Schema-Aligned Parsing (SAP)**

BAML compiles these definitions into a parser that sits between the LLM and the application logic. It performs **Schema-Aligned Parsing (SAP)** on the raw token stream generated by the Agno agents.1

* **Real-time Validation:** As the LLM generates tokens, BAML validates them against the defined schema in real-time. If the LLM attempts to output a string where an integer is required (e.g., for a mark allocation), or if it hallucinates a field that doesn't exist in the ontology, BAML detects the error immediately.  
* **Correction Strategies:** Crucially, BAML can apply heuristic correction strategies on the fly, steering the LLM back to the correct format. This guarantees that the JSON data exported to the Knowledge Graph is "pristine" and free from the formatting errors, hallucinations, or markdown artifacts that often plague LLM-based data extraction.1 This clean data is essential for the stability of the subsequent diffusion model training.

## ---

**6\. The Bilingual Dataset: Specifications as Ground Truth**

To train a translation system capable of generating high-fidelity Irish technical content, the system utilizes the official **Leaving Certificate Computer Science** and **Chemistry** specifications. These documents provide a parallel corpus of high-register, domain-specific language, acting as the "ground truth" for the system.

### **6.1 Computer Science Specification Analysis**

The **Leaving Certificate Computer Science Specification** (updated 2023\) serves as the primary dataset for computational terminology. The system ingests both the English 1 and Irish 1 versions to build a parallel linguistic corpus.

#### **6.1.1 Structural Framework**

The specification is constructed around three interwoven strands, which the system captures as the primary nodes in the Knowledge Graph:

* **Strand 1: Practices and Principles (*Cleachtais agus prionsabail*):** This strand covers the overarching methodology of the subject. It includes "Computational Thinking" (*Smaointeoireacht ríomhaireachtúil*), "Computers and Society" (*Ríomhairí agus an tsochaí*), and "Creative Design" (*Dearadh cruthaitheach*).1 The system aligns these concepts to teach the model how abstract processes are described in Irish.  
* **Strand 2: Core Concepts (*Príomhchoincheapa*):** This strand deals with the technical fundamentals. It includes "Abstraction" (*Teibíocht*), "Algorithms" (*Algartaim*), "Computer Systems" (*Ríomhchórais*), "Data" (*Sonraí*), and "Evaluation/Testing" (*Measúnú agus tástáil*).1 The alignment here is critical for technical translation, ensuring that terms like "binary" (*dénártha*) and "hexadecimal" (*heicsidheachúil*) are consistently mapped.  
* **Strand 3: Computer Science in Practice (*An Ríomheolaíocht i gcleachtas*):** This strand focuses on implementation. It covers "Applied Learning Tasks" (*Tascanna foghlama feidhmí*), programming languages, and the creation of artefacts.1

#### **6.1.2 Key Skills and Assessment**

The specification integrates five key senior cycle skills (*Príomhscileanna*): "Information Processing" (*Próiseáil Faisnéise*), "Critical and Creative Thinking" (*Smaointeoireacht chriticiúil agus chruthaitheach*), "Communicating" (*Cumarsáid*), "Working with Others" (*Obair le daoine eile*), and "Being Personally Effective" (*A bheith éifeachtach ar bhonn pearsanta*).1 The assessment structure includes an **End-of-course examination** (*Scrúdú ag deireadh an chúrsa*) (70%) and **Coursework assessment** (*Measúnú ar an obair chúrsa*) (30%).1 The system extracts the specific assessment criteria (*Critéir mheasúnaithe*) to understand the linguistic register used for evaluating student performance in Irish.

### **6.2 Chemistry Specification Analysis**

The **Leaving Certificate Chemistry Specification** (for introduction in 2025\) provides the dataset for scientific and chemical terminology.1

#### **6.2.1 Structural Framework**

The Chemistry specification is organized into five strands, which provide a rich source of scientific vocabulary:

* **Unifying Strand: The Nature of Science (*Snáithe Aontach: Nádúr na hEolaíochta*):** This strand emphasizes the scientific method, ethics, and the history of science.1 It provides the system with the language of inquiry (*fiosrúchán*), hypothesis generation (*hipitéis*), and evidence evaluation (*fianaise*).  
* **Strand 1: Nature of Matter (*Nádúr an Ábhair*):** Covers atomic structure, the periodic table, and stoichiometry.1  
* **Strand 2: Behaviour of Matter (*Iompraíocht an Ábhair*):** Focuses on chemical bonding, gas laws, and states of matter.1  
* **Strand 3: Interactions of Matter (*Idirghníomhaíochtaí damhna*):** Deals with thermochemistry, kinetics, equilibrium, and acids/bases.1  
* **Strand 4: Matter in our World (*Ábhar inár nDomhan*):** Connects chemistry to society, sustainability, and industrial applications.1

#### **6.2.2 Cross-Cutting Themes and Competencies**

The specification identifies three cross-cutting themes: **Health**, **Technology**, and **Sustainability**.1 These themes act as lenses through which the content is viewed. The system captures these thematic links to enable context-aware translation (e.g., ensuring "Sustainability" is translated as *Inbhuanaitheacht* in an environmental context). The document also details "Key Competencies" (*Príomhinniúlachtaí*) which mirror the key skills in CS but are contextualized for laboratory science.1

## ---

**7\. The Generative Motor: InkSpire Architecture Adaptation**

The "Motor Layer" of this Sovereign AI system is powered by **InkSpire**, a state-of-the-art **diffusion transformer model**. While originally conceptualized for stylized handwriting generation, this blueprint adapts the InkSpire architecture to serve as a high-fidelity **visual translation engine**. This adaptation leverages the model's unique ability to handle multi-line masked infilling and unified latent representations to generate Irish text that respects the spatial and semantic logic of the source material.

### **7.1 Unified Latent Representation Theory**

InkSpire represents a radical departure from traditional generative models that treat style and content as separate entities processed by distinct encoders (e.g., a text encoder for content and an image encoder for style). Instead, InkSpire unifies **Style**, **Content**, and **Noise** within a **shared latent space**.1

#### **7.1.1 Elimination of Auxiliary Encoders**

Traditional methods (such as One-DM or TGC-Diff) utilize separate encoders for the content (text) and the style (handwriting/font). This separation often leads to optimization difficulties and a loss of fine-grained interaction between the two modalities. InkSpire eliminates these explicit encoders.2 By removing the auxiliary text encoder, the model is forced to learn the relationship between content and style purely through the interaction of tokens in the latent space.

#### **7.1.2 In-Context Latent "Ink" Tokens**

The model leverages a **Variational Autoencoder (VAE)** to embed both the target semantic content (the English technical text) and the target linguistic style (the Irish orthography and layout) into the same latent feature space.2 This allows the model to learn "morpho-graphemic dependencies"—how the shape and structure of the output text (e.g., Irish ligatures, specific scientific notation layouts, or the *punctum delens*) are inextricably linked to the semantic content.1 In the context of translation, this means the model doesn't just predict the next word; it predicts the visual representation of the Irish text block that corresponds to the English input block.

### **7.2 Multi-line Masked Infilling Strategy**

To train the model on the complex, dense layouts of the Leaving Certificate specifications, the system employs a **Multi-line Masked Infilling Strategy**.2

#### **7.2.1 Paragraph-Level Processing**

Instead of training on single sentences or cropped lines, which is common in standard machine translation, InkSpire processes full paragraph blocks. This is crucial for capturing the "spatial logic" of the documents—how headings relate to body text, how bullet points are indented, and how mathematical formulas are aligned vertically.1 The model learns that a "Learning Outcome" header is usually followed by a bulleted list, and that this list must be translated into Irish while preserving the list structure.

#### **7.2.2 Infilling Objective**

During training, the system randomly masks tokens, words, or entire lines within a block of the bilingual dataset. The diffusion transformer is then tasked with "infilling" these missing pixels. This forces the model to learn the contextual relationships between the English source (provided as context) and the Irish target (masked for prediction), effectively treating translation as a high-dimensional image reconstruction task.1

### **7.3 Visual Conditioning**

InkSpire operates without a traditional text encoder. It conditions generation purely on **visual "ink" tokens**.1 This is a profound shift for translation. By treating text as visual tokens, the model bypasses the limitations of standard Unicode tokenizers which may struggle with specific Irish diacritics or archaic scribal abbreviations found in historical educational texts. It allows the system to generate "visual text" that is typographically identical to the official state examination papers, preserving fonts, bolding, and specialized layout formatting.

### **7.4 Rotated Aligned Position Encoding (R-APE)**

Technical translation often involves complex layouts, such as chemical equations, matrices, or code snippets in Computer Science. InkSpire addresses this via **Rotated Aligned Position Encoding (R-APE)**.1

* **Spatial Alignment:** R-APE ensures that the positional embeddings of the tokens account for the 2D structure of the page. It allows the model to maintain vertical alignment for fractions and matrices, ensuring that the generated Irish mathematical content is not just linguistically correct but spatially rigorous.1 This is particularly relevant for the Computer Science specification, where code indentation (e.g., in Python) carries semantic meaning.

## ---

**8\. Masked Conditional Flow Matching (CFM): The Training Objective**

The training objective for InkSpire is defined by **Masked Conditional Flow Matching** (MCFM), a superior alternative to standard DDPM (Denoising Diffusion Probabilistic Models) for this application.

### **8.1 The Flow Matching Paradigm**

Flow Matching models the generation process as a continuous-time transformation from a simple prior distribution (Gaussian noise) to the complex data distribution (the Irish technical documents).3

* **Vector Fields:** Unlike DDPMs which approximate a stochastic process, Flow Matching learns a deterministic **time-dependent vector field** (velocity field) that pushes the noise distribution towards the data distribution along a straight path.3  
* **ODE Solvers:** Generation is performed by solving an Ordinary Differential Equation (ODE) using this learned vector field. This results in faster sampling and higher fidelity compared to stochastic diffusion.5 The InkSpire model optimizes a loss function defined simply as $\\mathcal{L} \= \\mathcal{L}\_{diff}$, representing the difference between the predicted velocity and the true vector field.1

### **8.2 Masked Conditional Objective**

The "Masked Conditional" aspect is critical for the translation task.

* **Mechanism:** The objective restricts the flow matching loss to specific "target regions" defined by a binary mask, while treating the rest of the input (the reference/source) as a conditioning signal.7  
* **Application:** In the context of English-to-Irish translation, the "Reference" is the English text layout (embedded in the latent space). The "Target" is the masked region where the Irish text should be. The model learns the flow required to transform noise into Irish text *conditional* on the English context structure. This ensures "robust identity preservation"—meaning the layout, font, and formatting of the original English document are perfectly preserved in the translated Irish output.7

## ---

**9\. Technical Implementation Outline: The Pipeline**

The following technical outline details the end-to-end workflow of how the infrastructure prepares data to train the InkSpire-like diffusion model.

### **Phase 1: Ingestion and Semantic Structuring**

1. **Source Acquisition:** The system monitors a secure repository for the PDF specifications (SC-Chemistry-Specification-EN.pdf, LC-Computer-Science-specification-updated.pdf, and their Irish counterparts).  
2. **Cocoindex Orchestration:** Cocoindex detects new files. It utilizes PyMuPDF to render each page as a high-resolution image (for visual training data) and extracts the text layer (for semantic alignment).  
3. **Agentic Extraction (Agno/BAML):**  
   * The **Palaeographer Agent** (GLM-4.6v) scans the images to identify layout blocks (Headers, Paragraphs, Diagrams). It creates bounding box annotations for every text element.  
   * The **Ontologist Agent** uses BAML to classify these text elements according to the curriculum ontology (e.g., classifying a block as "Strand 2 Learning Outcome").  
4. **Graph Construction (Cognee):** Cognee consumes this metadata to build the Knowledge Graph. It links the English node for "Atomic Structure" to the Irish node for "Struchtúr Adamhach", establishing the ground truth alignment for the training dataset.

### **Phase 2: Latent Space Preparation (The "Unified" Step)**

1. **VAE Encoding:** The system uses a pre-trained Variational Autoencoder (VAE) to encode the page images into a compressed latent representation.  
2. **Conditioning Pairs:** Using the alignment from Cognee, the system constructs training pairs.  
   * *Input:* A latent representation of the English page layout.  
   * *Target:* A latent representation of the corresponding Irish page.  
3. **Unified Latent Construction:** The system combines these into a single tensor. The English content serves as the "Context" ($X\_{ctx}$), and the Irish content serves as the "Target" ($X\_{mis}$).  
4. **Mask Generation:** A masking strategy is applied. The pixels corresponding to the text content are masked out, while the layout structure (headers, lines, diagram boundaries) is left unmasked. This prepares the model for the "Masked Infilling" task.

### **Phase 3: Training the InkSpire Model**

1. **Model Architecture:** A Diffusion Transformer (DiT) is initialized with **Rotated Aligned Position Encoding (R-APE)** to handle the document layout.  
2. **Flow Matching Objective:** The model is trained to predict the "velocity" required to transport random noise in the masked regions towards the latent representation of the Irish text, conditioned on the unmasked English layout context.  
   * **Loss Function:** $\\mathcal{L}\_{diff}$ is calculated as the difference between the predicted velocity and the true vector field pointing to the Irish data distribution.  
3. **Optimization:** The model minimizes this loss, effectively learning to "write" Irish technical text that perfectly fits the visual and semantic structure of the source English document.

### **Phase 4: Inference and Generation**

1. **Input:** An English text string or document section (e.g., a new exam question).  
2. **Reasoning:** **Qwen3-VL** (integrated via Agno) reasons over the input and generates the Irish translation text.1  
3. **Synthesis:** This text is fed into the InkSpire decoder. The model "infills" the text into the visual template, generating a high-fidelity image of the Irish document, complete with correct mathematical notation and formatting.

## **10\. Conclusion**

The integration of the **InkSpire** diffusion architecture with the **Sovereign AI** neuro-symbolic blueprint represents a transformative approach to low-resource language technology. By moving beyond text-only translation and embracing a multimodal, visual-latent approach, this system honors the "spatial logic" and cultural integrity of Irish educational materials. The rigorous orchestration provided by Agno, Cocoindex, and BAML ensures that the data feeding this sophisticated model is accurate, structured, and compliant, creating a sustainable ecosystem for the digital preservation and activation of the Irish language. This architecture not only solves the immediate challenge of translating technical specifications but establishes a replicable framework for the preservation of any low-resource language rich in cultural and structural nuance.

#### **Works cited**

1. AI Agents for Irish Language Resources.md  
2. Learning to Generate Stylized Handwritten Text via a Unified Representation of Style, Content, and Noise | OpenReview, accessed December 23, 2025, [https://openreview.net/forum?id=FBPuLChGNX](https://openreview.net/forum?id=FBPuLChGNX)  
3. An introduction to Flow Matching · Cambridge MLG Blog, accessed December 23, 2025, [https://mlg.eng.cam.ac.uk/blog/2024/01/20/flow-matching.html](https://mlg.eng.cam.ac.uk/blog/2024/01/20/flow-matching.html)  
4. Flow Matching for Conditional Text Generation in a Few Sampling Steps \- ACL Anthology, accessed December 23, 2025, [https://aclanthology.org/2024.eacl-short.33.pdf](https://aclanthology.org/2024.eacl-short.33.pdf)  
5. Diffusion Bridge or Flow Matching? A Unifying Framework and Comparative Analysis \- arXiv, accessed December 23, 2025, [https://arxiv.org/html/2509.24531v1](https://arxiv.org/html/2509.24531v1)  
6. Flow Matching at Scale: A Machine Learning Framework for Efficient Large-Size Sampling of Many-Body Systems \- arXiv, accessed December 23, 2025, [https://arxiv.org/html/2508.15318v3](https://arxiv.org/html/2508.15318v3)  
7. Taming Identity Consistency and Prompt Diversity in Diffusion Models via Latent Concatenation and Masked Conditional Flow Matching \- ChatPaper, accessed December 23, 2025, [https://chatpaper.com/paper/208534](https://chatpaper.com/paper/208534)