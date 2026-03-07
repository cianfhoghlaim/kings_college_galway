# **Architecting a Sovereign Multimodal Neuro-Symbolic System for the Preservation and Generative Synthesis of Irish Cultural Heritage**

## **1\. Introduction: The Convergence of Archival Science and Generative AI**

The digital preservation of low-resource languages constitutes one of the most significant challenges in modern computational linguistics. While dominant languages benefit from petabyte-scale datasets, languages such as Irish (Gaeilge)—specifically its dialectal variants found in the *Gaeltacht* regions—occupy a precarious position where data scarcity intersects with high morphological complexity. The National Folklore Collection (Dúchas), particularly the Schools’ Collection from the 1930s, represents a critical repository of this linguistic heritage. However, the existing digitisation efforts, while foundational, largely rely on traditional Optical Character Recognition (OCR) pipelines that fracture under the weight of archaic scripts (*Cló Gaelach*), non-standard dialectal orthography, and the idiosyncratic handwriting styles of early 20th-century scribes.  
This research proposes a paradigm shift from static digitization to dynamic, generative preservation. By synthesizing state-of-the-art Vision-Language Models (VLMs)—specifically **Qwen3-VL** and **Gemma 3**—with advanced diffusion-based handwriting generation techniques derived from the **InkSpire** architecture, we establish a theoretical and practical framework for a system capable of not only reading but *writing* in the hand of the ancestors. This system, underpinned by a modern data lakehouse architecture utilizing **LanceDB** and **Apache Iceberg**, aims to generate novel educational content—specifically Leaving Certificate Mathematics solutions—rendered in the authentic, stylized handwriting of historical folklore scribes.  
The architecture detailed herein rejects the binary distinction between "online" (trajectory-based) and "offline" (image-based) handwriting recognition in favor of a unified visual token modeling approach. By leveraging the **Masked Visual Token Modeling (MVTM)** capabilities of Qwen3-VL and the **multi-line masked infilling strategy** of InkSpire, the system learns to predict handwriting as a continuous visual manifold rather than a sequence of discrete characters. This generative capability is rigorously governed by a Human-in-the-Loop (HITL) evaluation workflow orchestrated via **DSPy** programs and optimized by **GEPA (Genetic-Pareto)** algorithms, ensuring that the synthesized output adheres to the strict dialectal fidelity required by the Cill Éinne folklore samples and the mathematical rigor demanded by the Irish State Examinations Commission.

## ---

**2\. The Data Infrastructure: A Multimodal Lakehouse for Cultural Heritage**

The foundational layer of this system is a high-performance, multimodal data lakehouse designed to ingest, index, and retrieve heterogenous data types—ranging from raw manuscript images and PDF exam papers to high-dimensional vector embeddings and structured curriculum metadata. The integration of **Apache Iceberg** and **LanceDB** addresses the dual requirements of ACID compliance for metadata management and low-latency vector search for semantic retrieval.

### **2.1 Schema Design and Metadata Management with Apache Iceberg**

Apache Iceberg serves as the control plane for the dataset, managing the structured metadata extracted from the Dúchas archives and the Leaving Certificate curriculum specifications. Unlike traditional file-based data lakes, Iceberg’s table format allows for schema evolution and time-travel, features essential for managing the iterative corrections inherent in transcription projects.  
The ingestion pipeline targets the duchas.ie repository and the hiddenheritages.ai aggregator. For the **Cill Éinne** dataset 1, the schema must capture granular metadata that traditional OCR overlooks. We define a partition strategy based on Scribe\_ID, School\_Roll\_Number, and Dialect\_Region. The Iceberg tables are structured to support "Scribe-Specific" clustering, a necessity derived from the findings of Raghallaigh et al. 1, who demonstrated that HTR models trained on specific hands outperform generic models.  
**Table 1: Proposed Iceberg Schema for Folklore Metadata**

| Column Name | Data Type | Description | Indexing Strategy |
| :---- | :---- | :---- | :---- |
| manuscript\_id | UUID | Unique identifier for the physical volume/page. | Primary Key |
| image\_uri | String | S3/Object Storage path to the high-res scan. |  |
| scribe\_id | String | Identifier for the specific hand (e.g., Máire Ní Dhioráin). | Partition Key |
| dialect\_region | String | The Gaeltacht region (e.g., Árainn/Connacht). | Partition Key |
| transcript\_text | String | The "Gold Standard" human transcription. | Full-text Index |
| topic\_tags | List | Extracted themes (e.g., "Weather Lore", "Piseoga"). | Inverted Index |
| script\_type | Enum | Classification of script: Gaelic, Roman, Mixed. |  |
| embedding\_ref | Binary | Foreign key pointer to LanceDB vector ID. |  |

This schema explicitly links the visual asset (image\_uri) with its semantic representation (transcript\_text) and its vector representation (embedding\_ref). The script\_type field is crucial for handling the "Code and Script Switching" phenomenon observed in the corpus, where scribes switch between Irish (Gaelic script) and English (cursive).1 Iceberg's ability to handle partition evolution allows the system to adapt as new dialectal clusters are identified without rewriting the underlying data files.2

### **2.2 Vector-Native Storage with LanceDB**

While Iceberg manages the metadata, **LanceDB** serves as the AI-native storage layer for the high-dimensional embeddings generated by the vision encoders. LanceDB is selected for its multimodal capabilities; unlike traditional vector databases that store embeddings and metadata separately, LanceDB utilizes the **Lance** file format to store the raw data (images, document chunks) alongside the vectors.2  
This "Zero-Copy" architecture is vital for the ColPali retrieval pipeline. When the system retrieves a page of folklore based on a semantic query (e.g., "Signs of bad weather involving birds"), LanceDB allows the VLM to access the raw image data directly from the disk without the serialization overhead associated with fetching images from external object storage. This significantly reduces latency during the training of the Qwen3-VL and Gemma 3 models, which require high-throughput access to image-text pairs.2  
The curriculum data, specifically the curriculum\_index.json 1, is ingested into LanceDB to create a "RAG-ready" index of educational standards. The hierarchical structure of the curriculum (Cycle \-\> Subject \-\> Tranche) is flattened into vectorizable chunks, but the Lance format preserves the nested JSON structure in the metadata columns, allowing for hybrid search (vector similarity \+ structured filtering). For instance, a query can target "Geometry proofs" (vector) while filtering for "Leaving Certificate Higher Level" (metadata).3

## ---

**3\. Advanced Retrieval: The ColPali Paradigm**

The retrieval layer of the proposed system represents a departure from text-centric indexing. Traditional RAG pipelines rely on OCR to extract text from PDFs or images, which is then embedded. For Irish folklore manuscripts and complex Mathematics papers, this approach is fundamentally flawed due to the "lossy" nature of OCR when dealing with handwritten dialectal Irish and mathematical notation.1

### **3.1 Contextualized Late Interaction over PaliGemma (ColPali)**

We employ **ColPali**, a vision-language retrieval model that enables "Retrieve-and-See" capabilities. ColPali operates on the principle of **Contextualized Late Interaction**.4 Instead of compressing a document page into a single dense vector (which obliterates fine-grained detail), ColPali maps the image into a grid of visual tokens (patches), each represented by a vector embedding.  
Mechanism of Action for Handwriting:  
When a user (or an agent) queries the folklore database, the query is tokenized into text embeddings. The ColPali model then computes a similarity matrix between the query tokens and the visual patch embeddings of the manuscript pages. This "Late Interaction" (MaxSim) operation allows the model to identify relevant content based on visual features—such as the shape of a diagram in a math paper or the specific layout of a poem in a manuscript—without ever converting the image to text.6  
This is particularly powerful for the **Cill Éinne** dataset. A query about "*liabhán*" (basking shark) can match the visual representation of the word in the manuscript, even if the OCR failed to recognize the archaic spelling or the handwriting was ambiguous. The model learns to associate the *visual pattern* of the word with the semantic concept, bypassing the noise of intermediate text generation.

### **3.2 Indexing Leaving Certificate Mathematics**

For the mathematics component, ColPali's ability to index visual structures is indispensable. Mathematics papers 1 contain complex layouts with diagrams, graphs, and symbolic notation ($|x-3| \\le 12$) that are often destroyed by standard PDF parsers.

* **Visual-Semantic Alignment:** By indexing the *pages* of the exam papers as images, ColPali preserves the spatial relationship between the problem statement and the accompanying diagram.7  
* **Multimodal Querying:** The system allows for multimodal queries. An input can be a sketch of a graph or a LaTeX formula, which is embedded into the shared latent space to retrieve visually and semantically similar problems from the archive.

## ---

**4\. The Cognitive Layer: Qwen3-VL and Gemma 3**

The "brain" of the system comprises a sophisticated ensemble of Vision-Language Models (VLMs) tasked with reasoning, translation, and content generation. We focus on **Qwen3-VL** and **Gemma 3** due to their architectural suitability for handling variable-resolution inputs and long-context reasoning.

### **4.1 Qwen3-VL: Visual Reasoning and Instruction Tuning**

**Qwen3-VL** (specifically the 8B parameter variant) serves as the primary reasoning agent. Its architecture features **Naive Dynamic Resolution** and **M-RoPE (Multimodal Rotary Positional Embeddings)** 8, which allows it to process images of arbitrary aspect ratios without destructive resizing. This is crucial for handling the varied dimensions of scanned folklore ledgers.  
Finetuning Strategy with Unsloth:  
We utilize Unsloth to finetune Qwen3-VL, leveraging GRPO (Group Relative Policy Optimization) to align the model with the specific constraints of the Irish educational curriculum.

1. **Environment Setup:** The Unsloth framework enables 2x faster training and 60% memory reduction, allowing us to finetune the 8B model on consumer-grade hardware (e.g., NVIDIA GPUs with 24GB VRAM).10 We use the Qwen3\_VL\_(8B)-Vision-GRPO.ipynb notebook as a base.1  
2. **Dataset Construction:** The training dataset consists of tuples: {Image, Question, Chain-of-Thought, Answer}. The images are the folklore manuscripts or math papers; the questions are prompts like "Transcribe this page preserving dialect" or "Solve this geometry problem in Irish"; the answers are the ground-truth transcripts or expert-verified solutions.  
3. **GRPO for Reasoning:** Unlike standard SFT (Supervised Fine-Tuning) which optimizes for the next token, GRPO optimizes for the *outcome*. We generate multiple reasoning paths (groups) for a given math problem and use a reward function to reinforce the path that leads to the correct solution *and* uses correct Irish terminology.11 This effectively teaches the model to "think" in Irish mathematical logic.

### **4.2 Gemma 3: Multimodal Native Processing**

**Gemma 3** (and its mobile-optimized variant **Gemma 3n**) introduces a native multimodal architecture based on the **SigLIP** vision encoder.12 Its "Pan & Scan" capability allows it to adaptively crop high-resolution document images into smaller tokens, preserving fine details like the *punctum delens* (dot above a letter) used in old Irish script.  
**Role in the Pipeline:**

* **Gemma 3 (27B/12B):** Acts as the "Teacher" model for distilling knowledge into smaller models. It handles the most complex OCR tasks where context from the entire page (up to 128k tokens) is required to decipher ambiguous handwriting.14  
* **Gemma 3n (E4B):** Deployed as the "Edge" model for offline inference. This allows the system to run on local devices (e.g., tablets used in schools) without cloud dependency, a key requirement for privacy-preserving educational tools.15

### **4.3 Finetuning for "Online" vs "Offline" Recognition**

While the Dúchas dataset provides "offline" data (static images), the goal is to improve both offline OCR and "online" recognition (real-time stroke interpretation).

* **Offline HTR:** We finetune Qwen3-VL to perform OCR by treating it as a dense captioning task. The prompt \<|vision\_start|\>\<image\>\<|vision\_end|\> Transcribe the text: triggers the model to decode the visual tokens into text tokens.16  
* **Online Simulation:** We bridge the gap to online recognition by converting the offline images into "pseudo-online" trajectories. By using the **InkSpire** encoder (discussed in Section 5), we can extract stroke-level latent representations. Qwen3-VL is then trained to predict these stroke sequences, effectively learning the *motor program* of the handwriting, not just the static pixel distribution.17

## ---

**5\. The Motor Layer: Generative Handwriting with InkSpire**

The most novel aspect of this research is the generation of *new* handwritten content—specifically Leaving Certificate answers—in the style of the folklore scribes. To achieve this, we adapt the **InkSpire** architecture 1, a diffusion transformer model that unifies style, content, and noise.

### **5.1 Unified Latent Representation**

InkSpire departs from previous HTG methods (like One-DM or TGC-Diff) which used separate encoders for style and content. Instead, it embeds both the target text (Content) and the reference handwriting sample (Style) into a **shared latent space** using a VAE (Variational Autoencoder).1

* **Implication for Irish Script:** This unified representation is critical for *Cló Gaelach*. In this script, the "content" (the letter) and the "style" (the scribe's hand) are inextricably linked; the shape of a 'g' changes depending on whether it is followed by a vowel or a consonant (ligatures). By processing them in a shared space, InkSpire captures these morpho-graphemic dependencies that separate encoders miss.19

### **5.2 Multi-line Masked Infilling Strategy**

We implement InkSpire's **Multi-line Masked Infilling** strategy to train the model on the Dúchas dataset.

* **Training Protocol:** Instead of cropping the manuscript pages into single lines (which destroys vertical context), we feed full paragraph blocks into the model. We randomly mask out tokens, words, or entire lines and force the diffusion transformer to "infill" the missing pixels.1  
* **Learning Spatial Logic:** This forces the model to learn the *spatial logic* of the scribe: how they handle margins, how line spacing varies, and how ascenders/descenders from adjacent lines interact. This is essential for generating multi-line Math solutions where vertical alignment (fractions, matrices) is crucial.  
* **Visual Conditioning:** The model operates without a traditional text encoder. It conditions the generation purely on the visual "ink" tokens of the context. This allows it to generate characters that may not exist in standard Unicode (e.g., specific scribal abbreviations found in the Cill Éinne sample).1

### **5.3 Adapting InkSpire for VLM Integration**

We integrate InkSpire as the "decoder" for our VLM.

1. **Reasoning:** Qwen3-VL solves the math problem and outputs the solution string in LaTeX/Text format.  
2. **Style Retrieval:** The system queries LanceDB (via ColPali) to find a folklore page with a handwriting style that matches the desired persona (e.g., Máire Ní Dhioráin).  
3. **Synthesis:** The solution text and the retrieved style image are fed into InkSpire. The model generates the handwritten image of the solution, applying the **Rotated Aligned Position Encoding (R-APE)** to handle the layout of the mathematical formulas.1

## ---

**6\. Human-in-the-Loop Evaluation: DSPy, GEPA, and Ragas**

To ensure the generated content is both mathematically accurate and culturally authentic, we establish a rigorous evaluation loop.

### **6.1 Programmatic Evaluation with DSPy**

We use **DSPy** (Declarative Self-improving Python) to define the entire pipeline as a compiled program.

* **Modules:** We define DSPy modules for RetrieveFolklore, SolveMath, TranslateToIrish, and GenerateHandwriting.  
* **Signatures:** Each module has a typed signature (e.g., question: str \-\> answer: str, reasoning: str). This enforces structured outputs, preventing the model from outputting unstructured chatter.20

### **6.2 Reflective Prompt Optimization with GEPA**

Writing prompts that balance "Math Accuracy," "Irish Grammar," and "Folklore Tone" is a multi-objective optimization problem. We use **GEPA (Genetic-Pareto)** to evolve these prompts automatically.21

* **Mechanism:** GEPA maintains a population of prompts. It runs the pipeline on a validation set (Leaving Cert sample questions) and evaluates the results.  
* **Reflection:** When the system fails (e.g., the model uses an English math term "Hypotenuse" instead of "Taobhagán"), GEPA uses a "Reflection LM" (Qwen3-Thinking) to analyze the trace. It identifies the cause (e.g., "The prompt didn't explicitly forbid English terms") and mutates the prompt to fix it.21  
* **Pareto Optimization:** GEPA optimizes for multiple conflicting metrics simultaneously (e.g., increasing "Dialect Strength" might decrease "Math Accuracy"). It identifies the Pareto frontier of prompts that offer the best trade-offs, ensuring we don't sacrifice mathematical correctness for aesthetic flair.22

### **6.3 Retrieval Evaluation with Ragas**

To validate the ColPali retrieval component, we employ **Ragas** (Retrieval Augmented Generation Assessment).23

* **Multimodal Faithfulness:** We adapt this metric to check if the generated Irish answer is faithful to the *visual* evidence in the retrieved folklore diagrams or math specification images.  
* **Multimodal Relevance:** This metric scores how well the retrieved manuscript patches answer the user's query regarding dialect or style.24

### **6.4 Domain-Specific Metrics: HCPR and AIR**

We implement the novel metrics defined in 1 to specifically evaluate the "historical" quality of the generation:

* **HCPR (Historical Character Preservation Rate):** Measures the percentage of generated characters that validly belong to the target script (*Cló Gaelach*). We use a classifier trained on the Dúchas dataset to detect valid glyphs.  
* **AIR (Archaic Insertion Rate):** Monitors for "Over-historicization"—a hallucination mode where the model inserts characters that are *too* old (e.g., Middle Irish forms) or incorrect for the 1930s period. This ensures the model targets the specific *temporal* window of the Schools' Collection.1

## ---

**7\. Implementation Roadmap and Future Directions**

The realization of this system follows a phased deployment strategy.  
**Phase 1: Data Ingestion (Months 1-2)**

* Scrape duchas.ie and hiddenheritages.ai using respecting robots.txt and copyright.  
* Ingest PDF specifications and Exam Papers.  
* Process all images through ColPali and store embeddings in LanceDB.  
* Populate Iceberg tables with metadata.

**Phase 2: Model Training (Months 3-4)**

* Finetune Qwen3-VL (8B) using Unsloth on the HTR task (Image \-\> Text).  
* Train the InkSpire diffusion model on the raw folklore images (Text \+ Style \-\> Image).  
* Use GEPA to optimize the system prompts for the reasoning tasks.

**Phase 3: Integration & Evaluation (Months 5-6)**

* Deploy the DSPy pipeline connecting Retrieval, Reasoning, and Generation.  
* Run the "Leaving Cert" benchmark: Generate full exam solutions.  
* Conduct HITL evaluation: Have fluent Irish speakers and Math teachers grade the outputs. Feed this feedback back into GEPA to refine the prompts.

This framework represents a robust, scalable approach to Digital Humanities. By treating handwriting not as static pixels but as a generative, semantic modality, we unlock new possibilities for education and cultural preservation. The system does not merely archive the Irish language; it grants it a new, digital life, capable of reasoning, solving, and expressing itself in the visual voice of its heritage.

## **8\. Detailed Analysis of Key Components**

### **8.1 The "InkSpire" Masking Technique: A Deep Dive**

The **InkSpire** paper 1 outlines a critical innovation for this project: the **Multi-line Masked Infilling Strategy**. Unlike standard diffusion models that generate images from pure noise, InkSpire is trained to reconstruct missing parts of a document.

* **The "Ink" Token:** The model treats the visual strokes of handwriting as "ink tokens" in a latent space. This is conceptually similar to how LLMs treat text tokens.  
* **Training Process:** During training, the model receives a full page of the Cill Éinne folklore. Random blocks of the text are masked out. The model must predict the missing "ink tokens" based on two conditions:  
  1. **Global Style:** The visible surrounding text provides the style cues (slant, pressure, ligature style).  
  2. **Local Content:** The textual transcription provides the semantic content of what *should* be there.  
* **Why this matters for Irish:** Irish handwriting is dense with ligatures and superscripts (*síneadh fada*). A standard "text-to-image" model might generate a generic "Celtic" font. InkSpire, trained via masking, learns that *this specific scribe* always connects the 'r' to the 'i' in a certain way. When we ask it to generate a math solution, it applies these specific microscopic rules to the new content, creating a hyper-realistic forgery of the historical style.

### **8.2 Unsloth & Liger Kernel for Efficient Training**

Training VLMs is computationally expensive. **Unsloth** provides the necessary optimizations to make this feasible on academic or consumer hardware.

* **Liger Kernel:** Unsloth integrates **Liger Kernel**, which are highly optimized Triton kernels for the Qwen architecture.25 These kernels reduce the memory overhead of the attention mechanism (Flash Attention 2\) and the MLP layers.  
* **Impact:** This allows us to finetune the Qwen3-VL 8B model with a 4x larger batch size or 4x longer context length than standard PyTorch. For the Dúchas dataset, which contains long narratives, this extended context is vital. We can feed an entire multi-page story into the context window during training, ensuring the model learns narrative consistency.

### **8.3 LanceDB: The Multimodal Native Database**

**LanceDB** is chosen over alternatives like Pinecone or Milvus because of its **Lance** file format.2

* **Columnar Storage for Multimodal:** Lance stores vector embeddings and the actual image data (blobs) in a columnar format that is optimized for ML workloads.  
* **Random Access:** It supports fast random access to the image data. This is crucial for the **ColPali** "Late Interaction" step. When ColPali computes the similarity score, it needs to access the visual patch embeddings of the candidate documents. LanceDB allows this to happen at extreme speeds without moving data between a database and an object store (S3).  
* **Iceberg Interop:** LanceDB tables can be exposed as Iceberg tables. This allows our data scientists to query the folklore metadata using standard SQL engines (like Trino or Spark) via the Iceberg catalog, while the ML pipeline accesses the vectors via LanceDB.2

### **8.4 Ragas Metrics for Visual Retrieval**

Standard RAG evaluation metrics (Context Precision, Answer Relevancy) are text-based. We extend **Ragas** for the visual domain.

* **Visual Context Precision:** Measures the proportion of retrieved visual patches that are actually relevant to the query. If the user asks for "Math problems about triangles," and ColPali retrieves a page of algebra equations (just because it looks similar layout-wise), this metric captures the failure.  
* **Implementation:** We implement this by using a VLM (Qwen3-VL) as the "Judge." The Judge is given the query and the retrieved image patch and asked to score relevance on a binary scale. Ragas manages the aggregation of these scores over the evaluation dataset.24

### **8.5 GEPA: Evolutionary Optimization of the "Math-in-Irish" Persona**

Generating math solutions in Irish requires a specific "Persona." The model must act as a student or teacher from a Gaeltacht school.

* **The Optimization Challenge:** If we prompt the model "Solve this," it might output English. If we prompt "Réitigh é seo," it might use modern standardized Irish, not the dialectal Irish of the 1930s.  
* **GEPA Loop:**  
  1. **Seed Prompt:** "You are a math student in 1935 Connemara. Solve this problem in Irish."  
  2. **Evaluation:** The system generates an answer. An evaluator (or the VLM itself) checks the output against a list of "forbidden" modern words and "required" dialect terms.  
  3. **Mutation:** GEPA modifies the prompt. "You are a math student... use 'do' instead of 'go'... strictly follow the notation in the 2025 spec."  
  4. **Selection:** The prompt that yields the highest score (correct math \+ correct dialect) is selected for the next generation. This evolutionary process discovers the optimal prompt structure without manual trial and error.21

## ---

**9\. Appendix: Technical Specifications**

### **9.1 Hardware Requirements**

* **Finetuning (Qwen3-VL 8B):** 1x NVIDIA A100 (80GB) or 2x RTX 4090 (24GB) using Unsloth QLoRA.  
* **Inference (Gemma 3n E4B):** Can run on local edge devices (e.g., Apple Silicon Macs, high-end Android tablets) for the "offline" educational app component.  
* **Storage:** 2TB NVMe SSD for the LanceDB dataset (images \+ vectors).

### **9.2 Software Stack Versions**

* **Unsloth:** v2025.2+ (Supports Qwen2.5/3 VL)  
* **Transformers:** v4.48+ (Required for Gemma 3\)  
* **LanceDB:** v0.17+  
* **Ragas:** v0.2.0+ (Multimodal support)

### **9.3 Dataset Statistics (Projected)**

* **Dúchas Corpus:** \~40,000 pages (DHH project subset).  
* **Leaving Cert Archive:** \~500 exam papers (1995-2025).  
* **Synthetic Math Data:** \~10,000 generated problem-solution pairs (Irish).

#### **Works cited**

1. Cill Éinne · The Schools' Collection \_ dúchas.ie.pdf  
2. From BI to AI: A Modern Lakehouse Stack with Lance and Iceberg \- LanceDB, accessed December 22, 2025, [https://lancedb.com/blog/from-bi-to-ai-lance-and-iceberg/](https://lancedb.com/blog/from-bi-to-ai-lance-and-iceberg/)  
3. The Future of Open Source Table Formats: Apache Iceberg and Lance \- LanceDB, accessed December 22, 2025, [https://lancedb.com/blog/the-future-of-open-source-table-formats-iceberg-and-lance/](https://lancedb.com/blog/the-future-of-open-source-table-formats-iceberg-and-lance/)  
4. ColPali: Capabilities and Enterprise Applications \- Nexastack, accessed December 22, 2025, [https://www.nexastack.ai/blog/colpali-enterprise-applications](https://www.nexastack.ai/blog/colpali-enterprise-applications)  
5. Beyond Text: The Rise of Vision-Driven Document Retrieval for RAG | Vespa Blog, accessed December 22, 2025, [https://blog.vespa.ai/the-rise-of-vision-driven-document-retrieval-for-rag/](https://blog.vespa.ai/the-rise-of-vision-driven-document-retrieval-for-rag/)  
6. Bringing Vision-Language Intelligence to RAG with ColPali | Towards Data Science, accessed December 22, 2025, [https://towardsdatascience.com/bringing-vision-language-intelligence-to-rag-with-colpali/](https://towardsdatascience.com/bringing-vision-language-intelligence-to-rag-with-colpali/)  
7. ColPali: Efficient Document Retrieval with Vision Language Models \- Hugging Face, accessed December 22, 2025, [https://huggingface.co/blog/manu/colpali](https://huggingface.co/blog/manu/colpali)  
8. Qwen2-VL: A hands-on code walkthrough | by tangbasky | Data Science Collective | Medium, accessed December 22, 2025, [https://medium.com/data-science-collective/qwen2-vl-a-hands-on-code-walkthrough-c5a4e073e9b3](https://medium.com/data-science-collective/qwen2-vl-a-hands-on-code-walkthrough-c5a4e073e9b3)  
9. Qwen2-VL: Enhancing Vision-Language Model's Perception of the World at Any Resolution, accessed December 22, 2025, [https://arxiv.org/html/2409.12191v1](https://arxiv.org/html/2409.12191v1)  
10. Vision Fine-tuning | Unsloth Documentation, accessed December 22, 2025, [https://docs.unsloth.ai/basics/vision-fine-tuning](https://docs.unsloth.ai/basics/vision-fine-tuning)  
11. Fine-Tuning Qwen2.5-3B-Instruct (GRPO, PEFT) \- Kaggle, accessed December 22, 2025, [https://www.kaggle.com/code/ksmooi/fine-tuning-qwen2-5-3b-instruct-grpo-peft](https://www.kaggle.com/code/ksmooi/fine-tuning-qwen2-5-3b-instruct-grpo-peft)  
12. google/gemma-3-27b-it \- Hugging Face, accessed December 22, 2025, [https://huggingface.co/google/gemma-3-27b-it](https://huggingface.co/google/gemma-3-27b-it)  
13. Gemma 3 Technical Report \- arXiv, accessed December 22, 2025, [https://arxiv.org/html/2503.19786v1](https://arxiv.org/html/2503.19786v1)  
14. Gemma 3 model overview \- Google AI for Developers, accessed December 22, 2025, [https://ai.google.dev/gemma/docs/core](https://ai.google.dev/gemma/docs/core)  
15. Use Gemma open models | Generative AI on Vertex AI \- Google Cloud Documentation, accessed December 22, 2025, [https://docs.cloud.google.com/vertex-ai/generative-ai/docs/open-models/use-gemma](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/open-models/use-gemma)  
16. Visual-language assistant with Qwen2VL and OpenVINO, accessed December 22, 2025, [https://docs.openvino.ai/2024/notebooks/qwen2-vl-with-output.html](https://docs.openvino.ai/2024/notebooks/qwen2-vl-with-output.html)  
17. \[Literature Review\] Representing Online Handwriting for Recognition in Large Vision-Language Models \- Moonlight, accessed December 22, 2025, [https://www.themoonlight.io/en/review/representing-online-handwriting-for-recognition-in-large-vision-language-models](https://www.themoonlight.io/en/review/representing-online-handwriting-for-recognition-in-large-vision-language-models)  
18. Representing Online Handwriting for Recognition in Large Vision-Language Models \- arXiv, accessed December 22, 2025, [https://arxiv.org/html/2402.15307v1](https://arxiv.org/html/2402.15307v1)  
19. Learning to Generate Stylized Handwritten Text via a Unified Representation of Style, Content, and Noise | OpenReview, accessed December 22, 2025, [https://openreview.net/forum?id=FBPuLChGNX](https://openreview.net/forum?id=FBPuLChGNX)  
20. Tutorial: Retrieval-Augmented Generation (RAG) \- DSPy, accessed December 22, 2025, [https://dspy.ai/tutorials/rag/](https://dspy.ai/tutorials/rag/)  
21. gepa-ai/gepa: Optimize prompts, code, and more with AI-powered Reflective Text Evolution \- GitHub, accessed December 22, 2025, [https://github.com/gepa-ai/gepa](https://github.com/gepa-ai/gepa)  
22. Demystifying GEPA (Genetic-Pareto) Prompt Optimizer | by GUANGYUAN PIAO \- Medium, accessed December 22, 2025, [https://medium.com/@parklize/demystifying-gepa-genetic-pareto-prompt-optimizer-53db5081cdb2](https://medium.com/@parklize/demystifying-gepa-genetic-pareto-prompt-optimizer-53db5081cdb2)  
23. Overview of Metrics \- Ragas, accessed December 22, 2025, [https://docs.ragas.io/en/stable/concepts/metrics/overview/](https://docs.ragas.io/en/stable/concepts/metrics/overview/)  
24. Multi modal relevance \- Ragas, accessed December 22, 2025, [https://docs.ragas.io/en/stable/concepts/metrics/available\_metrics/multi\_modal\_relevance/](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/multi_modal_relevance/)  
25. An open-source implementaion for fine-tuning Qwen-VL series by Alibaba Cloud. \- GitHub, accessed December 22, 2025, [https://github.com/2U1/Qwen-VL-Series-Finetune](https://github.com/2U1/Qwen-VL-Series-Finetune)