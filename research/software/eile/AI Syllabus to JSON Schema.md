# **Bria Fibo and the Hugging Face Ecosystem: Architecting Educational Visualization Pipelines via Structured JSON Synthesis**

## **1\. Introduction: The Pedagogical Imperative for Deterministic Visualization**

The intersection of generative Artificial Intelligence (AI) and educational technology stands at a precipice. For decades, the visualization of complex pedagogical concepts—ranging from the subcellular mechanisms of photosynthesis to the abstract dynamics of macroeconomic supply chains—has relied on static, standardized stock imagery or expensive bespoke illustrations. The advent of latent diffusion models promised a revolution: the ability to generate infinite, customized visual aids on demand. However, this promise has been fundamentally hindered by the "stochasticity problem." In high-stakes educational environments, where visual fidelity to a curriculum is paramount, the inherent randomness of standard text-to-image (T2I) models renders them unreliable. A prompt describing "atomic structure" might yield a scientifically outdated planetary model rather than an accurate quantum probability cloud, driven by training data biases rather than pedagogical intent.  
This report presents an exhaustive technical analysis of **Bria AI's Fibo**, a model that fundamentally reimagines the generation pipeline by replacing free-form textual prompting with a rigorous, deterministic **JSON-native schema**. We explore this architecture within the context of the **Fibo Hackathon**, a $30,000 initiative designed to incentivize the creation of professional-grade, agentic workflows.1 Specifically, we analyze the integration of Fibo with the broader **Hugging Face ecosystem**—including the diffusers library, smolagents framework, and constrained generation libraries like instructor and outlines—to architect a system capable of autonomously parsing educational syllabi and synthesizing curriculum-aligned visual assets with surgical precision.2  
We argue that the transition from "prompt engineering" to "schema engineering" represents a paradigm shift essential for the educational sector. By decoupling visual attributes—lighting, composition, camera parameters, and object relationships—into structured data fields, Fibo allows developers to treat image generation as a programmable logic problem rather than a linguistic guessing game.3 This report serves as a blueprint for hackathon participants, detailing the computational pathways required to translate the unstructured text of a PDF syllabus into the structured visual language of Fibo.

## **2\. Bria Fibo: Technical Anatomy of a JSON-Native Model**

To effectively leverage Fibo in an educational pipeline, one must first understand its distinct architectural innovations. Unlike its predecessors, which rely on a single, entangled text embedding, Fibo is engineered to respect independent visual variables, solving the "Prompt Dilemma" where a minor textual edit inadvertently alters the entire scene composition.1

### **2.1 The Architecture of Disentanglement**

The core differentiator of Fibo is its training on **long structured captions**.3 Standard datasets (like LAION-5B) typically pair images with short, noisy alt-text. In contrast, Fibo's training data utilizes comprehensive JSON-based annotations that explicitly separate an image’s content (the objects present) from its presentation (lighting, style, camera angle).  
This separation facilitates **native disentanglement**. In a standard model, prompting for a "gloomy biology lab" might cause the model to render the scientific equipment as old or broken, conflating the atmospheric adjective "gloomy" with the object state. Fibo’s architecture separates these into distinct schema fields: mood\_atmosphere governs the "gloom," while the objects array defines the equipment state. This allows an educational developer to programmatically request a "clean, modern microscope" (Object State) situated in a "dramatic, low-key lit room" (Atmosphere) without semantic bleed.3

#### **2.1.1 DimFusion: Managing High-Density Inputs**

Processing the dense, 1,000+ word structured prompts required for such control presents a computational bottleneck for traditional Transformer-based text encoders (like CLIP or T5). Bria introduces **DimFusion**, a novel fusion mechanism designed to integrate the intermediate tokens from a Large Language Model (LLM) into the image generation process efficiently.3  
DimFusion allows Fibo to digest complex syllabus requirements—such as a multi-step chemical reaction with specific spatial constraints—without hitting the token limits or "forgetting" instructions that plague standard architectures. For hackathon participants, this means the model can handle the *entirety* of a complex learning objective's visual requirements in a single pass, rather than requiring multiple in-painting steps to correct details.3

### **2.2 The Three Modes of Interaction**

Fibo supports three distinct operational modes, each serving a specific function in an educational content pipeline.1

| Interaction Mode | Educational Application | Technical Mechanism |
| :---- | :---- | :---- |
| **Generate** | Creating a new visual from a learning objective. | The VLM expands a short intent (e.g., "Show mitosis") into a full JSON schema, which drives the diffusion process. |
| **Refine** | Correcting scientific inaccuracies or adjusting layout. | The user updates specific JSON fields (e.g., change background from "white" to "lab"). The model regenerates only the affected latents, preserving the core subject.1 |
| **Inspire** | Style transfer from reference textbooks. | An input image (e.g., a specific textbook diagram style) is fed into the VLM to extract a style schema, which is then applied to new content.4 |

**Insight:** The "Refine" mode is particularly critical for the **Scientific QA loop**. If an initial generation of a "water molecule" shows incorrect bond angles, an automated agent can detect this error (via VQA) and issue a precise JSON update to the relationship field of the atoms, ensuring the final output is scientifically valid without needing to re-roll the random seed and potentially lose the correct lighting or style.4

### **2.3 Legal and Commercial Viability in Education**

A significant barrier to AI adoption in education is copyright liability. Schools and publishers cannot risk using imagery generated from scraped artist data. Bria Fibo is trained exclusively on **licensed data**, offering enterprise indemnification and C2PA watermarking for provenance tracking.1 This "legal safety" feature is a crucial value proposition for hackathon projects targeting institutional education markets, distinguishing Fibo-based solutions from those built on models with contested copyright status.5

## **3\. The Hugging Face Ecosystem: The Builder’s Toolkit**

Bria Fibo does not function in isolation. It is deeply embedded within the Hugging Face (HF) ecosystem, providing the necessary infrastructure to build "Syllabus-to-Image" pipelines.

### **3.1 Inference and Pipeline Integration via diffusers**

Bria provides the BriaFiboPipeline within the standard Hugging Face diffusers library, enabling seamless integration with existing ML workflows.4 The pipeline accepts the structured JSON directly, abstracting the complexity of the underlying tensor operations.  
For hackathon participants looking to optimize performance—critical when generating thousands of images for a digital textbook—Bria offers **briaai/Fibo-lite** and a **Guidance Distillation LoRA**.6 The distillation LoRA allows the model to run at a Guidance Scale (CFG) of 1.0, effectively skipping the negative prompt pass and doubling inference speed. While this introduces a slight quality degradation, the trade-off is often acceptable for high-volume educational assets where speed and throughput are prioritized over hyper-realistic texture detail.6

### **3.2 Orchestration with smolagents**

Complex educational workflows require decision-making logic. A simple script cannot decide whether a "Civil War" syllabus requires a map or a portrait. This is where **smolagents**, Hugging Face's lightweight agent framework, becomes essential.7  
smolagents allows developers to wrap the Fibo generation process into a **Tool**. An agent can then be instructed to:

1. **Analyze** a syllabus section.  
2. **Decide** on the best visual aid (e.g., "This section on thermodynamics needs a graph").  
3. **Call** the Fibo Tool with specific parameters.

Crucially, smolagents supports **structured outputs** via Pydantic integration. This means the agent's output can be strictly typed to match the Fibo JSON schema, preventing the generation of invalid parameters that would cause API failures.9

### **3.3 Semantic Segmentation with RMBG-2.0**

Educational visuals often require composability—placing a generated 3D molecule onto a specific slide background. Bria’s **briaai/RMBG-2.0** (Remove Background) model, also available on HF, is a state-of-the-art segmentation tool.10 A comprehensive hackathon solution might chain Fibo (generation) \-\> RMBG-2.0 (segmentation) \-\> Canvas API (composition) to create modular learning objects rather than flattened raster images.

## **4\. Computational Syllabus Analysis: Ingesting the Curriculum**

Before an image can be generated, the syllabus—the "source of truth"—must be parsed and understood. Educational syllabi are often unstructured PDFs containing a mix of administrative policies, schedules, and learning objectives.11

### **4.1 PDF Parsing and OCR**

The first challenge is extracting clean text and structural hierarchy from PDF documents.

* **Llama 3.2 Vision:** This multimodal model serves as a powerful OCR engine. It can read PDF pages as images, preserving the semantic layout of tables and diagrams that traditional OCR (like Tesseract) might scramble.12  
* **Extraction Logic:** The parser must identify **Learning Objectives (LOs)**. These are typically distinct sections labeled "Student Learning Outcomes" or "Objectives."  
* **Hierarchical Mapping:** An effective parsing agent separates content into a hierarchy: *Course \-\> Module \-\> Unit \-\> Concept*. This context is vital; the concept "Bonding" means something very different in a *Chemistry* hierarchy than in a *Sociology* hierarchy.11

### **4.2 Knowledge Graph Construction**

To ensure deep semantic alignment, the extracted text should be converted into a **Knowledge Graph (KG)**.14

* **Nodes:** Concepts (e.g., "Photosynthesis," "Mitosis").  
* **Edges:** Relationships (e.g., "is a type of," "requires," "produces").  
* **Implementation:** Using **Neo4j** or **Docling** alongside an LLM, the syllabus text is transformed into graph structures.16 This allows the visualization agent to understand dependencies. If a syllabus mentions "Calvin Cycle," the KG informs the agent that this is a *sub-process* of "Photosynthesis," and therefore the visual should likely be a detailed diagram within a chloroplast context.18

### **4.3 Taxonomy of Learning Objectives**

Not all learning objectives require the same type of visual. Using **Bloom's Taxonomy**, the parsing agent can classify objectives to determine the visual strategy 19:

* **"Identify" / "Define"**: Requires concrete, isolated object visuals (e.g., "A picture of a mitochondria").  
* **"Analyze" / "Compare"**: Requires composite visuals (e.g., "Side-by-side comparison of Plant vs. Animal cells").20  
* **"Apply"**: Requires scenario-based visuals (e.g., "A photo of a bridge illustrating tension forces").

**Insight:** Automating this classification prevents the generation of "decorative" images that distract learners, focusing instead on "instructive" images that directly support cognitive processing.19

## **5\. Semantic Translation: From Learning Objectives to Visual Schemas**

This section details the logic required to bridge the gap between abstract pedagogical goals and concrete visual definitions. This "Semantic Bridge" is the core intellectual property of any robust Fibo-based educational application.

### **5.1 The Role of Visual Metaphors**

Many educational concepts are invisible (e.g., "Entropy," "Justice," "Atomic Orbitals"). To visualize them, we must employ **visual metaphors**.21 An LLM agent must act as a "Pedagogical Translator," selecting the appropriate analogy before attempting generation.

* **Case: Atomic Structure.** A syllabus might state: *"Understand the modern atomic theory."*  
  * *Naive Generation:* Prompting "Atom" might yield a Bohr planetary model. While iconic, this is scientifically inaccurate for advanced physics.  
  * *Agentic Intervention:* The agent checks the *grade level*. If "University Physics," it rejects the planetary metaphor and selects the "Electron Cloud" metaphor.22  
  * *Fibo Input:* The agent constructs a JSON describing "a dense central nucleus surrounded by a diffuse, probabilistic haze," explicitly avoiding "orbits" in the description.23

### **5.2 Structured Generation Libraries: Instructor, Outlines, and Guidance**

To reliably generate the complex JSON required by Fibo from an LLM, developers must use **constrained generation libraries**. These tools ensure the LLM's output conforms strictly to the Fibo schema, eliminating syntax errors.24

#### **5.2.1 Instructor (Pydantic-First)**

The instructor library is ideal for pipelines using OpenAI-compatible endpoints (like vLLM or LiteLLM). It allows developers to define the Fibo schema as a **Pydantic model**.26  
**Code Logic Example:**

Python

import instructor  
from pydantic import BaseModel, Field  
from typing import Literal

class FiboObject(BaseModel):  
    description: str \= Field(..., description="Visual description of the object")  
    location: str \= Field(..., description="Position: 'center', 'background', 'left'")  
    relationship: str \= Field(..., description="Interaction with other objects")

class FiboPrompt(BaseModel):  
    short\_description: str  
    style\_medium: Literal  
    lighting: str  
    objects: list\[FiboObject\]

\# The 'patch' ensures the LLM output matches FiboPrompt schema perfectly  
client \= instructor.from\_provider(OpenAI())  
fibo\_json \= client.chat.completions.create(  
    response\_model=FiboPrompt,  
    messages=  
)

This approach provides **type safety** and **validation retries**, ensuring the downstream Fibo API call never fails due to malformed JSON.26

#### **5.2.2 Outlines and Guidance (Local/Low-Level Control)**

For local inference or tighter control over specific string patterns (e.g., enforcing that aspect\_ratio is exactly "16:9" via Regex), **outlines** and **guidance** offer powerful alternatives.25

* **outlines**: Can enforce a JSON schema on local Llama models using finite-state machine (FSM) decoding. This is highly efficient and guarantees schema compliance at the *token generation level*.25  
* **guidance**: Allows for interleaved generation, where Python logic and LLM generation are mixed. This is useful for building dynamic prompts where the syllabus content determines the prompt structure in real-time.28

### **5.3 Agentic Logic for Schema Population**

The "Translator Agent" must make deterministic decisions to populate the JSON fields based on syllabus metadata.

* **Style Medium Mapping:**  
  * *Input:* Syllabus Metadata Subject: History.  
  * *Logic:* Map to style\_medium: "oil painting" or "archival photograph" to evoke historical authenticity.31  
  * *Input:* Syllabus Metadata Subject: Geometry.  
  * *Logic:* Map to style\_medium: "vector art", background\_setting: "grid paper" for clarity.  
* **Lighting and Mood:**  
  * *Input:* Subject: Literature (Gothic Horror unit).  
  * *Logic:* Map to lighting: "low key", "shadowy"; mood\_atmosphere: "eerie".  
  * *Input:* Subject: Lab Safety.  
  * *Logic:* Map to lighting: "bright studio"; mood\_atmosphere: "clean, sterile".32

## **6\. The Fibo JSON Schema: A Deep Technical Reference**

Understanding the nuances of the Fibo JSON schema is the key to unlocking its "pro-level control".1 This section breaks down the specific fields and their valid values, aggregated from model documentation.32

### **6.1 Top-Level Fields**

| Field | Type | Description | Valid Values (Examples) |
| :---- | :---- | :---- | :---- |
| short\_description | string | The conceptual anchor of the image. | "A cross-section of a plant cell." |
| style\_medium | string | The artistic technique. | photograph, digital illustration, 3D render, sketch, oil painting, vector art.31 |
| background\_setting | string | The environment surrounding the subject. | "A blurred classroom background," "Solid white background," "A lush rainforest." |
| lighting | object / string | Illumination parameters. | conditions: "natural", "studio"; direction: "top-down", "backlit"; shadows: "soft", "harsh".32 |
| photographic\_characteristics | object | Camera simulation settings. | lens\_focal\_length: "85mm", "35mm"; depth\_of\_field: "shallow", "deep"; camera\_angle: "eye level", "low angle".32 |

### **6.2 The objects Array: The Composition Engine**

The objects array allows for explicit scene composition. This is where educational rigor is enforced.

* **relationship**: Describes physical or semantic interaction.  
  * *Example:* "The moon is *orbiting* the earth."  
  * *Example:* "The catalyst is *mixed with* the solution."  
* **relative\_size**: Critical for scientific scale.  
  * *Example:* "The sun is *massive compared to* the planet."  
* **location**: Defines screen space.  
  * *Example:* "Top-right corner," "Foreground center."

**Insight:** By programmatically populating the location field, developers can create **sequential narratives**. For a comic-strip style history lesson, the agent can generate three separate images where the main character (Object A) moves from location: "left" (Image 1\) to location: "center" (Image 2\) to location: "right" (Image 3), maintaining narrative flow.33

### **6.3 Aesthetics and Atmosphere**

The aesthetics object controls the "vibe."

* **color\_scheme**: Can be used to enforce branding or coding (e.g., "Use standard CPK coloring for molecular models" \- Carbon is black, Oxygen is red).33  
* **composition**: Values like rule of thirds or symmetrical help create balanced, professional-looking slides.33

## **7\. Architecting the Hackathon Solution: The "Curriculum-to-Pixel" Pipeline**

For the Fibo Hackathon, we propose an end-to-end architecture that leverages the full stack of identified technologies. This "Curriculum-to-Pixel" pipeline represents a holistic solution to the challenge of automated educational content creation.

### **7.1 Architecture Diagram & Data Flow**

1. **Ingestion Layer (The Librarian):**  
   * **Input:** User uploads a PDF syllabus (e.g., "AP Bio Semester 1").  
   * **Process:** LlamaParse or pytesseract extracts text. **Llama 3.2 Vision** extracts images of existing diagrams to serve as style references.12  
2. **Analysis Layer (The Pedagogue):**  
   * **Agent:** smolagents "Curriculum Agent."  
   * **Task:** Decompose syllabus into atomic Learning Objectives. Consults a **Neo4j Knowledge Graph** to identify dependencies and appropriate metaphors.16  
   * **Output:** A list of "Visual Concepts" tagged with Grade Level and Subject.  
3. **Synthesis Layer (The Art Director):**  
   * **Agent:** smolagents "Fibo Architect."  
   * **Task:** Convert each Visual Concept into a valid Fibo JSON schema.  
   * **Tool:** Uses instructor with a Pydantic model of the Fibo schema to ensure validity.  
   * **Logic:** Applies the "Metaphor Mapping" logic (e.g., selects "3D Render" for physics).  
4. **Generation Layer (The Artist):**  
   * **Engine:** BriaFiboPipeline running on **HF Inference Endpoints** or **Fal.ai**.33  
   * **Optimization:** Uses briaai/Fibo-lite for rapid prototyping of the entire course asset list.  
5. **Quality Assurance Layer (The Editor):**  
   * **Agent:** "Critic Agent" using a VLM (e.g., Idefics3).  
   * **Task:** Compares the generated image against the original Learning Objective.  
   * **Loop:** If the VLM detects an error (e.g., "The diagram is missing a nucleus"), the agent triggers the **Fibo Refine** mode with a specific correction prompt.4

### **7.2 Implementation Nuances**

* **Style Consistency via LoRA:** To ensure the generated textbook looks cohesive, the pipeline can train a lightweight **LoRA** on a specific illustration style (e.g., "Khan Academy Style") and load this into the Fibo pipeline. This ensures that a history image and a math image share the same color palette and line weight.6  
* **Background Removal:** For slide deck generation, the pipeline should automatically pass the Fibo output through **briaai/RMBG-2.0** to create transparent assets that can be layered onto slide templates.10

## **8\. Case Studies in Educational Visualization**

To demonstrate the practical application of this architecture, we examine two specific domain workflows.

### **8.1 Case Study A: The Biological Process (Photosynthesis)**

**Syllabus Input:** *"Students will explain the inputs (sunlight, H2O, CO2) and outputs (O2, glucose) of photosynthesis."*.34  
**Pipeline Execution:**

1. **Metaphor Selection:** The Pedagogue Agent selects the "Leaf Cross-Section" model, rejecting the "Factory" metaphor as too abstract for the target Grade 9 audience.34  
2. **JSON Construction:**  
   * short\_description: "Cross-section of a leaf showing cellular structure."  
   * style\_medium: "Digital Illustration."  
   * objects:  
     * {"description": "Sun rays", "location": "top-left", "relationship": "entering the leaf"}.  
     * {"description": "Water molecules", "location": "bottom stem", "relationship": "moving up"}.  
     * {"description": "Stomata pores", "location": "underside", "relationship": "open for gas exchange"}.34  
3. **Refinement:** The initial image shows the stomata on top. The Critic Agent (VLM) flags this as biologically incorrect. The Refine mode is triggered: *"Move stomata to the underside of the leaf."* Fibo corrects the image without altering the style.

### **8.2 Case Study B: The Abstract Concept (Network Topology)**

**Syllabus Input:** *"Understanding mesh vs. star network topologies in computer science."*.35  
**Pipeline Execution:**

1. **Metaphor Selection:** The agent identifies "Star" and "Mesh" as topological graphs.  
2. **JSON Construction:**  
   * **Image 1 (Star):** objects: \`\`.  
   * **Image 2 (Mesh):** objects: \[{"description": "Network Nodes", "location": "distributed"}, {"description": "Connection Lines", "relationship": "interconnecting every node to every other node"}\].  
3. **Output:** Two distinct, clean diagrams that visually demonstrate the connectivity difference, enforced by the relationship parameter.35

## **9\. Ethical, Legal, and Future Implications**

### **9.1 Copyright Safety as a Feature**

In the educational publishing market, legal indemnity is not a luxury; it is a requirement. Bria’s "Risk-Free Development" promise, backed by licensed data and C2PA watermarking, allows hackathon participants to pitch their solutions to major publishers (e.g., Pearson, McGraw-Hill) who cannot use Midjourney or Stable Diffusion due to legal opacity.1

### **9.2 The "Living Textbook"**

The ultimate promise of this technology is the **Dynamic Textbook**. Instead of a static PDF, a Fibo-backed curriculum can adapt visually to the student.

* **Localization:** A math problem about "calculating area" can instantly generate an image of a *baseball field* for a US student and a *cricket pitch* for an Indian student, simply by swapping the background\_setting in the JSON.36  
* **Accessibility:** For visually impaired students, the very JSON used to generate the image serves as a perfect, detailed **Alt-Text** description, solving a major accessibility compliance challenge.19

## **10\. Conclusion**

The Fibo Hackathon presents an opportunity to solve one of EdTech's most persistent challenges: the scalable production of accurate, high-quality visual content. By leveraging the **Bria Fibo** model's structured JSON architecture, developers can bypass the unpredictability of traditional generative AI. When combined with the **Hugging Face ecosystem**—specifically the reasoning capabilities of **smolagents** and the structured synthesis of **instructor**—it becomes possible to build autonomous pipelines that function as "AI Illustrators," translating the dry text of a syllabus into vivid, curriculum-aligned imagery.  
For the hackathon participant, the path to victory lies in the **integration**: building a system where the Syllabus (PDF) informs the Agent (Logic), which constructs the Schema (JSON), which drives the Model (Fibo). This is not merely image generation; it is the **semantic compilation of knowledge into pixel form**.

### ---

**Table 1: Comparative Analysis of Educational Generation Models**

| Feature | Bria Fibo | Flux / SDXL | Midjourney | Educational Implication |
| :---- | :---- | :---- | :---- | :---- |
| **Input Interface** | Structured JSON Schema | Natural Language Prompt | Natural Language Prompt | Fibo allows programmatic control of variables (e.g., looping through historical eras) impossible with text prompts. |
| **Disentanglement** | Native (via Architecture) | Weak (Prompt Bleed) | Weak (Style Bleed) | Fibo can change the "lighting" without accidentally changing the "chemical reaction" shown. |
| **Training Data** | Licensed, Structured Captions | Scraped Web Data (LAION) | Proprietary / Scraped | Fibo is legally safe for textbook publishing; others pose copyright risks.1 |
| **Refinement** | Parametric Update (JSON) | In-painting / Img2Img | Vary Region | Fibo allows semantic updates ("Make the bond double") rather than just pixel masking. |
| **Text Rendering** | Moderate | High (Flux) | Moderate | Fibo requires post-processing for labels, while Flux is better at raw text generation. |

### **Table 2: Syllabus-to-JSON Mapping Matrix**

| Syllabus Attribute | Extracted Metadata | Fibo JSON Field Target | Example Value |
| :---- | :---- | :---- | :---- |
| **Subject Domain** | "Chemistry" | style\_medium | "3D molecular render" |
| **Target Audience** | "Grade 3" | style\_medium | "vibrant digital illustration" |
| **Historical Context** | "Victorian Era" | aesthetics.mood\_atmosphere | "sepia tone, industrial, foggy" |
| **Key Concept** | "Photosynthesis" | objects.description | "Chloroplast", "Sunlight", "Water" |
| **Concept Relation** | "X leads to Y" | objects.relationship | "Arrow pointing from X to Y" |
| **Setting** | "In the field" | background\_setting | "Outdoor nature scene, shallow focus" |

### **Table 3: Valid Fibo Parameter Sets for Educational Domains**

| Domain | Recommended style\_medium | Recommended lighting | Recommended camera |
| :---- | :---- | :---- | :---- |
| **Biology** | photograph, macro | natural, diffused | lens: "100mm macro", focus: "shallow" |
| **Physics** | 3D render, schematic | studio, hard shadows | angle: "isometric", lens: "50mm" |
| **History** | oil painting, archival photo | cinematic, low key | angle: "eye level", lens: "35mm" |
| **Math** | vector art, sketch | flat, even | angle: "top-down", background: "grid" |
| **Literature** | digital illustration, watercolor | dramatic, moody | angle: "dutch angle" (for tension) |

#### **Works cited**

1. FIBO Open-Source T2I Model Built for Pro-Level Creative Control, accessed December 13, 2025, [https://bria.ai/fibo](https://bria.ai/fibo)  
2. BRIA Launches FIBO: A New Era of Controllable Visual AI for Businesses \- CEPIC, accessed December 13, 2025, [https://www.cepic.org/post/bria-launches-fibo-a-new-era-of-controllable-visual-ai-for-businesses](https://www.cepic.org/post/bria-launches-fibo-a-new-era-of-controllable-visual-ai-for-businesses)  
3. Enhancing Text-to-Image With Structured Captions \- arXiv, accessed December 13, 2025, [https://arxiv.org/html/2511.06876v1](https://arxiv.org/html/2511.06876v1)  
4. briaai/FIBO \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/briaai/FIBO](https://huggingface.co/briaai/FIBO)  
5. Bria.ai | Generate AI Images at Scale, accessed December 13, 2025, [https://bria.ai/](https://bria.ai/)  
6. briaai/Fibo-lite \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/briaai/Fibo-lite](https://huggingface.co/briaai/Fibo-lite)  
7. Structured Outputs with Inference Providers \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/docs/inference-providers/guides/structured-output](https://huggingface.co/docs/inference-providers/guides/structured-output)  
8. Agents \- Guided tour \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/docs/smolagents/guided\_tour](https://huggingface.co/docs/smolagents/guided_tour)  
9. Tools \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/docs/smolagents/tutorials/tools](https://huggingface.co/docs/smolagents/tutorials/tools)  
10. briaai/RMBG-2.0 \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/briaai/RMBG-2.0](https://huggingface.co/briaai/RMBG-2.0)  
11. How Skills Extraction Works \- Mapademics, accessed December 13, 2025, [https://docs.mapademics.com/skills-processing/how-skills-extraction-works](https://docs.mapademics.com/skills-processing/how-skills-extraction-works)  
12. Building Visual RAG Pipelines with Llama 3.2 Vision & Ollama \- Codecademy, accessed December 13, 2025, [https://www.codecademy.com/article/rag-with-llama-3-2](https://www.codecademy.com/article/rag-with-llama-3-2)  
13. Meta AI PDF Reading: availability, functionality, and developer workflows for document analysis \- Data Studios, accessed December 13, 2025, [https://www.datastudios.org/post/meta-ai-pdf-reading-availability-functionality-and-developer-workflows-for-document-analysis](https://www.datastudios.org/post/meta-ai-pdf-reading-availability-functionality-and-developer-workflows-for-document-analysis)  
14. How to create a knowledge graph from 1000s of unstructured documents? \- Reddit, accessed December 13, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1imgyw9/how\_to\_create\_a\_knowledge\_graph\_from\_1000s\_of/](https://www.reddit.com/r/LocalLLaMA/comments/1imgyw9/how_to_create_a_knowledge_graph_from_1000s_of/)  
15. rahulnyk/knowledge\_graph: Convert any text to a graph of knowledge. This can be used for Graph Augmented Generation or Knowledge Graph based QnA \- GitHub, accessed December 13, 2025, [https://github.com/rahulnyk/knowledge\_graph](https://github.com/rahulnyk/knowledge_graph)  
16. How to Convert Unstructured Text to Knowledge Graphs Using LLMs \- Neo4j, accessed December 13, 2025, [https://neo4j.com/blog/developer/unstructured-text-to-knowledge-graph/](https://neo4j.com/blog/developer/unstructured-text-to-knowledge-graph/)  
17. Build a knowledge graph from documents using Docling | by Alain Airom (Ayrom) | Medium, accessed December 13, 2025, [https://alain-airom.medium.com/build-a-knowledge-graph-from-documents-using-docling-8bc05e1389f7](https://alain-airom.medium.com/build-a-knowledge-graph-from-documents-using-docling-8bc05e1389f7)  
18. How to Visualize Photosynthesis: A Simple Science Experiment \- Thoughtfully Sustainable, accessed December 13, 2025, [https://thoughtfullysustainable.com/visualize-photosynthesis-experiment/](https://thoughtfullysustainable.com/visualize-photosynthesis-experiment/)  
19. Supporting Learning with AI-Generated Images: A Research-Backed Guide, accessed December 13, 2025, [https://mitsloanedtech.mit.edu/2024/03/06/supporting-learning-with-ai-generated-images-a-research-backed-guide/](https://mitsloanedtech.mit.edu/2024/03/06/supporting-learning-with-ai-generated-images-a-research-backed-guide/)  
20. AI Illustration for Educators: Creating Engaging Teaching Materials \- Forem, accessed December 13, 2025, [https://forem.com/localfaceswap/ai-illustration-for-educators-creating-engaging-teaching-materials-3com](https://forem.com/localfaceswap/ai-illustration-for-educators-creating-engaging-teaching-materials-3com)  
21. Instructional Analogies Dominate, Domain-Inherent Metaphors Are Overlooked: A Systematic Review of Metaphorical Mappings in Chemistry Education \- ACS Publications, accessed December 13, 2025, [https://pubs.acs.org/doi/10.1021/acs.jchemed.4c01537](https://pubs.acs.org/doi/10.1021/acs.jchemed.4c01537)  
22. accessed December 13, 2025, [https://www.researchgate.net/figure/Various-visual-representations-of-atomic-structure-from-Turkish-chemistry-textbooks\_fig1\_333936096\#:\~:text=According%20to%20current%20modern%20atom,the%20notion%20of%20the%20atom.](https://www.researchgate.net/figure/Various-visual-representations-of-atomic-structure-from-Turkish-chemistry-textbooks_fig1_333936096#:~:text=According%20to%20current%20modern%20atom,the%20notion%20of%20the%20atom.)  
23. Atoms, Molecules and Enzymes: 3D and Animation Practices as a Mechanism to Visualise Quantum Theory \- Figshare, accessed December 13, 2025, [https://figshare.com/ndownloader/files/54335723](https://figshare.com/ndownloader/files/54335723)  
24. Structured model outputs | OpenAI API, accessed December 13, 2025, [https://platform.openai.com/docs/guides/structured-outputs](https://platform.openai.com/docs/guides/structured-outputs)  
25. Outlines \- Docs by LangChain, accessed December 13, 2025, [https://docs.langchain.com/oss/python/integrations/providers/outlines](https://docs.langchain.com/oss/python/integrations/providers/outlines)  
26. Instructor \- Multi-Language Library for Structured LLM Outputs | Python, TypeScript, Go, Ruby \- Instructor, accessed December 13, 2025, [https://python.useinstructor.com/](https://python.useinstructor.com/)  
27. From Chaos to Order: Structured JSON with Pydantic and Instructor in LLMs \- Kusho Blog, accessed December 13, 2025, [https://blog.kusho.ai/from-chaos-to-order-structured-json-with-pydantic-and-instructor-in-llms/](https://blog.kusho.ai/from-chaos-to-order-structured-json-with-pydantic-and-instructor-in-llms/)  
28. guidance | control LM output \- Microsoft Research, accessed December 13, 2025, [https://www.microsoft.com/en-us/research/project/guidance-control-lm-output/](https://www.microsoft.com/en-us/research/project/guidance-control-lm-output/)  
29. dottxt-ai/outlines: Structured Outputs \- GitHub, accessed December 13, 2025, [https://github.com/dottxt-ai/outlines](https://github.com/dottxt-ai/outlines)  
30. guidance-ai/guidance: A guidance language for controlling large language models. \- GitHub, accessed December 13, 2025, [https://github.com/guidance-ai/guidance](https://github.com/guidance-ai/guidance)  
31. briaai/FIBO · Hugging Face : r/ethicaldiffusion \- Reddit, accessed December 13, 2025, [https://www.reddit.com/r/ethicaldiffusion/comments/1ok959r/briaaifibo\_hugging\_face/](https://www.reddit.com/r/ethicaldiffusion/comments/1ok959r/briaaifibo_hugging_face/)  
32. Fibo | Text to JSON | fal.ai, accessed December 13, 2025, [https://fal.ai/models/bria/fibo/generate/structured\_prompt/api](https://fal.ai/models/bria/fibo/generate/structured_prompt/api)  
33. Fibo | Text to Image \- Fal.ai, accessed December 13, 2025, [https://fal.ai/models/bria/fibo/generate/api](https://fal.ai/models/bria/fibo/generate/api)  
34. Activities and Experiments to Explore Photosynthesis in the Classroom, accessed December 13, 2025, [https://www.plt.org/educator-tips/activities-experiments-photosynthesis-classroom/](https://www.plt.org/educator-tips/activities-experiments-photosynthesis-classroom/)  
35. An Extended Platter Metaphor for Effective Reconfigurable Network Visualization, accessed December 13, 2025, [https://www.researchgate.net/publication/215721067\_An\_Extended\_Platter\_Metaphor\_for\_Effective\_Reconfigurable\_Network\_Visualization](https://www.researchgate.net/publication/215721067_An_Extended_Platter_Metaphor_for_Effective_Reconfigurable_Network_Visualization)  
36. How to Write Great AI Art Prompts | Articulate, accessed December 13, 2025, [https://www.articulate.com/blog/how-to-write-great-ai-art-prompts/](https://www.articulate.com/blog/how-to-write-great-ai-art-prompts/)