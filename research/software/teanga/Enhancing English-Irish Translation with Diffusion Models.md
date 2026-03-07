# **The Convergence of Diffusion Generative Models and Agentic Workflows: A Paradigm Shift for Low-Resource Neural Machine Translation**

## **Executive Summary: The Imperative for Architectural Evolution**

The field of Neural Machine Translation (NMT) stands at a critical inflection point. For the past decade, the autoregressive (AR) Transformer has served as the undisputed hegemon of sequence-to-sequence modeling, achieving remarkable fluency in high-resource languages by modeling the conditional probability of a token given its predecessors. However, the analysis of current research indicates that this paradigm is approaching an asymptotic limit, particularly when applied to low-resource languages (LRLs) such as Irish (*Gaeilge*). The inherent left-to-right causality of AR models introduces a fundamental fragility: the "error propagation" bottleneck. In data-scarce environments, where the model's confidence in the next token is often low due to insufficient coverage of the linguistic distribution, a single hallucinatory step compels the model to condition all subsequent generation on a fallacy, leading to catastrophic semantic drift.  
This report posits that the future of high-fidelity Irish-English translation lies not in scaling existing AR architectures, but in a fundamental transition toward **Diffusion Models (DMs)**—specifically, the emerging class of unified continuous-discrete frameworks and semi-autoregressive block diffusion architectures. By treating text generation as an iterative denoising process rather than a sequential classification task, diffusion models offer a mechanism to refine the entire sequence holistically, utilizing bidirectional context to resolve the complex morphological dependencies (such as initial mutations and VSO word order) that characterize the Irish language.  
Furthermore, this analysis synthesizes a comprehensive technical roadmap for overcoming the primary obstacle to this transition: the scarcity of high-quality, diverse parallel data. We delineate a novel "Multimodal Data Foundry" architecture that synergizes three cutting-edge technologies: **Qwen3-VL** for agentic visual reasoning and OCR, **Google Agent Development Kit (ADK)** for orchestrating complex multi-step data curation workflows, and **LanceDB** as the high-performance multimodal lakehouse. This stack enables the extraction of parallel corpora from non-traditional, non-digitized sources—archival manuscripts, scanned folklore, and visual media—transforming the "low-resource" problem into a tractable data engineering challenge.

## ---

**1\. The Theoretical Frontiers of Diffusion NMT: Transcending Autoregression**

The migration from autoregressive to diffusion-based text generation represents a shift from determining *what comes next* to determining *what belongs*. While AR models ask, "Given the word 'The', what is likely to follow?", diffusion models ask, "Given a noisy representation of a sentence, how do I clarify it into 'The cat sat on the mat'?" This section analyzes the theoretical underpinnings of this shift and evaluates the specific architectures—NeoDiff and Block Diffusion—that constitute the current State of the Art (SOTA).

### **1.1 The Dichotomy of Text Diffusion: Discrete vs. Continuous Paradigms**

The application of diffusion probabilistic models (DPMs) to natural language processing has historically been bifurcated by the nature of the data itself. Images, the native domain of diffusion, are continuous signals; text is inherently discrete. This discrepancy has led to two distinct modeling lineages, each with critical trade-offs for NMT performance.  
**Discrete Diffusion Models** attempt to apply the diffusion process directly in the categorical space of vocabulary tokens.

* **Mechanism:** These models, exemplified by architectures like D3PM or Diffusion-NAT 1, define the forward diffusion process as a Markov chain where tokens are randomly replaced by a \`\` token or sampled from a uniform distribution over the vocabulary. The reverse process involves training a neural network to predict the original token $x\_0$ (or the previous state $x\_{t-1}$) given the corrupted state $x\_t$.  
* **Advantages:** This approach respects the discrete nature of language and allows for the direct use of standard cross-entropy loss functions.3  
* **Limitations:** The transition between "masked" and "unmasked" is abrupt. Discrete diffusion struggles to capture the subtle gradients of semantic uncertainty. When a token is masked, all semantic information is obliterated; there is no "partial" state that retains the grammatical category of a word while obscuring its specific identity. This results in a lack of fine-grained control during generation, limiting the model's ability to perform the nuanced "polishing" required for high-quality translation.4

**Continuous Diffusion Models** circumvent the discreteness problem by mapping text into a continuous embedding space (e.g., Gaussian diffusion on word vectors).

* **Mechanism:** The forward process adds Gaussian noise to the word embeddings until they resemble pure white noise. The reverse process learns to denoise these vectors in the latent space.  
* **Advantages:** This allows the model to utilize the full arsenal of gradient-based optimization and continuous control techniques developed for image generation. It enables the model to traverse semantic space smoothly; a "noisy" vector might represent a superposition of "cat" and "dog" before resolving to one, preserving semantic category information throughout the process.3  
* **Limitations:** The fundamental challenge, known as the "rounding problem," occurs at the final step of generation. Mapping a denoised continuous vector back to a discrete token often results in incoherence, as the vector may not land precisely on a valid vocabulary embedding. Furthermore, applying uniform noise across all tokens (as is typical in image diffusion) ignores the linguistic reality that some words (content words) are information-dense and robust, while others (function words) are fragile and context-dependent.6

### **1.2 State-of-the-Art: NeoDiff (Non-simultaneous Continuous Diffusion)**

The most significant advancement in 2025 is the unification of these paradigms through **NeoDiff** (Non-simultaneous Continuous Diffusion Models). The analysis suggests that NeoDiff represents the current SOTA for NMT because it resolves the "uniform noise" limitation of continuous diffusion while retaining its gradient-based advantages.4  
The Bi-Temporal Framework:  
NeoDiff introduces a sophisticated temporal architecture that disentangles the global progress of the generation from the local progress of individual tokens.

1. **Extrinsic Time ($t$):** This represents the standard diffusion timeline, tracking the overall noise level of the entire sequence from $t=1$ (pure noise) to $t=0$ (clean text).  
2. **Intrinsic Time ($\\tau$):** This is a novel variable that tracks the diffusion progress of *each individual token*. Unlike previous models where $\\tau \= t$ for all tokens, NeoDiff allows $\\tau$ to vary across the sequence.8

The Poisson Diffusion Process:  
To govern the relationship between extrinsic and intrinsic time, NeoDiff employs a Poisson process for the forward corruption pass. This is a critical innovation. A Poisson process models the random arrival of events over time. In NeoDiff, these "events" are discrete jumps in the noise level of a token. This allows tokens to "age" (accumulate noise) at different rates stochastically.

* *Mathematical Implication:* The probability of a token being at a certain noise level is governed by the Poisson distribution $P(k; \\lambda)$, where $\\lambda$ is a function of the extrinsic time $t$. This bridges the gap between discrete state transitions and continuous noise accumulation.5

Context-Aware Denoising:  
In the reverse (generation) direction, NeoDiff utilizes a Context-Aware Time Predictor. Instead of forcing the model to denoise the entire sentence synchronously, the Time Predictor estimates the optimal intrinsic time $\\tau$ for each token based on the current context.

* *Mechanism:* The model identifies tokens that are "easier" to resolve (e.g., determiners, conjunctions, or highly probable verbs) and reduces their noise level faster (advancing their intrinsic time to 0).  
* *Benefit:* These resolved tokens then act as stable anchors for the model to attend to while it continues to refine the "harder" tokens (e.g., complex entities or ambiguous nouns). This creates a dynamic, curriculum-based generation process that mirrors human cognition: establishing the grammatical skeleton of a sentence before filling in the semantic details.4

Relevance to Irish-English Translation:  
For the Irish language, this non-simultaneous generation is transformative. Irish is a VSO (Verb-Subject-Object) language with a complex system of initial mutations (lenition and eclipsis) triggered by preceding particles or grammatical environments.

* *Scenario:* Consider translating "The boat" to *An bád*. If the context implies "on the boat" (*ar an mbád*), the noun *bád* must undergo eclipsis to become *mbád*.  
* *AR Limitation:* An autoregressive model must predict the preposition *ar*, then *an*, and finally *mbád*. If it hallucinates the wrong preposition, the mutation will be incorrect.  
* *NeoDiff Advantage:* NeoDiff generates the whole sequence iteratively. It might resolve the noun *bád* and the preposition *ar* first. In subsequent denoising steps, the Context-Aware Time Predictor allows the model to perceive the conflict between *ar* and the unmutated *bád*, adjusting the noun to *mbád* to satisfy the morphosyntactic constraints. This ability to "look ahead" and "fix backwards" is inherent to diffusion but absent in AR.1

### **1.3 Efficiency Breakthroughs: Block Diffusion**

While NeoDiff offers superior quality, the iterative nature of diffusion (requiring 50 to 1000 forward passes of the neural network) creates a latency bottleneck that creates challenges for production deployment. **Block Diffusion** (or Semi-Autoregressive Diffusion) has emerged as the architectural solution to this "inference speed vs. quality" trade-off.10  
Architecture and Mechanism:  
Block Diffusion hybridizes the autoregressive and diffusion approaches. It generates text in "blocks" (chunks of tokens, e.g., 4, 8, or 16 tokens at a time).

1. **Inter-Block Autoregression:** The model generates Block $N$ conditioned on Block $N-1$, Block $N-2$, etc. This sequential dependency allows the model to utilize **KV Caching** (Key-Value Caching), a standard optimization in Transformers that stores the attention computations of previous tokens. Pure diffusion models cannot use KV caching because they modify the *entire* sequence at every step, invalidating the cache. Block Diffusion reclaims this efficiency.11  
2. **Intra-Block Diffusion:** Within the current block, the tokens are generated via a diffusion process. The model refines these $K$ tokens simultaneously, allowing for bidirectional reasoning *within the local window*.

The "Gradient Variance" Problem and Solution:  
Research indicates that training diffusion models on discrete data often suffers from high gradient variance, leading to instability. Block Diffusion addresses this by introducing custom data-driven noise schedules. Instead of a fixed noise schedule, the model analyzes the variance of the gradients during training and adapts the noise levels to minimize this variance. This results in faster convergence and lower perplexity compared to standard diffusion training.12  
Strategic Fit for the Stack:  
For our proposed pipeline, Block Diffusion offers the ideal compromise. It allows us to leverage the Qwen3-VL (which is an AR model) as a backbone. We can adapt a Qwen model to operate in Block Diffusion mode by modifying its attention masking (allowing bidirectional attention within blocks) and adding a diffusion head. This "warm-starting" from a massive pre-trained model significantly reduces the data requirements compared to training a NeoDiff model from scratch.11

### **Table 1: Comparative Analysis of NMT Architectures**

| Feature | Autoregressive (Transformer) | Discrete Diffusion (e.g., D3PM) | Continuous Diffusion (e.g., Diffusion-LM) | NeoDiff (SOTA 2025\) | Block Diffusion |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **Generation Order** | Left-to-Right (Sequential) | Parallel / Iterative | Parallel / Iterative | Non-simultaneous / Adaptive | Block-wise Sequential |
| **Context** | Unidirectional ($x\_{\<t}$) | Bidirectional (noisy) | Bidirectional (noisy) | Bidirectional (context-aware) | Hybrid (Global AR \+ Local Bidirectional) |
| **Error Recovery** | Impossible (Error Propagation) | Limited (Mask-Predict) | High (Gradient Guidance) | Very High (Context-Guided) | High (within block) |
| **Inference Speed** | Fast ($O(N)$) | Slow ($O(T)$ steps) | Slow ($O(T)$ steps) | Moderate (Optimized Schedule) | Fast ($O(N/K \\times T)$) |
| **Handling Morphology** | Weak (context dependent) | Moderate | Moderate | **Strong** (Global coherence) | **Strong** (Local coherence) |
| **KV Cache Support** | Yes | No | No | No | **Yes** |

## ---

**2\. The Low-Resource Irish Landscape: Challenges and Opportunities**

To effectively deploy these advanced architectures, one must first understand the specific constraints of the Irish language data landscape. Irish is classified as an "extremely low-resource" language in the context of NMT, typically relying on datasets of 50,000 to 100,000 sentence pairs—orders of magnitude fewer than the hundreds of millions available for English-French or English-Spanish.13

### **2.1 The Current Baseline: UCCIX and Qomhrá**

Recent academic efforts have attempted to bridge this gap by adapting English-centric Large Language Models (LLMs) to Irish. Analyzing these baselines provides critical insights into what works and what remains to be solved.  
UCCIX (Llama 2-13B Adaptation):  
UCCIX represents a "vocabulary expansion" approach. The researchers expanded the Llama 2 tokenizer with native Irish sub-words and performed Continued Pre-Training (CPT) on a collection of approximately 520 million Irish tokens.

* *Key Insight:* The study found that **layer-selective training** is crucial. Instead of fine-tuning all parameters (which leads to catastrophic forgetting of English reasoning capabilities), UCCIX focused on training the layers responsible for "language understanding" (typically lower/middle layers) while freezing the "reasoning" layers. This suggests that for our diffusion model, we should likely freeze the backbone and only train the diffusion head and embedding adapters.15

Qomhrá (Bilingual 8B Model):  
Qomhrá focused on the "instruction tuning" phase. Recognizing the lack of Irish instruction datasets (like Alpaca or Dolly), the team used a larger, closed-source model (Gemini 1.5 Pro) to translate the English Dolly V2 dataset into Irish.

* *Key Insight:* **Synthetic Data Efficacy.** Qomhrá demonstrated that synthetic data, even if imperfect, can successfully align a model to follow instructions in Irish. The model achieved gains of up to 29% in Irish benchmarks. This validates our proposed strategy of using Qwen3-VL to generate synthetic training data.17

### **2.2 The "Translationese" Trap and the Multimodal Opportunity**

Despite these successes, existing models suffer from "Translationese"—text that is grammatically correct but syntactically mimics English. This occurs because the training data is overwhelmingly dominated by translated legal texts (EU legislation, the Constitution) which prioritize strict adherence to the source text over natural Irish phrasing.19  
There is a severe lack of **conversational, literary, and technical data**. Furthermore, there are effectively **zero** large-scale multimodal (image-text) datasets for Irish. This is a critical missed opportunity. Visual grounding—learning that the word *bád* correlates with an image of a boat—provides a "semantic anchor" that is independent of English. A model trained on (Image, Irish Text) pairs learns the *concept* of *bád* directly, rather than just learning that *bád* is the statistical equivalent of the English token "boat".20

## ---

**3\. The Technology Stack \- Deep Dive: Building the Foundry**

To construct the dataset required to train a NeoDiff or Block Diffusion model for Irish, we propose a "Multimodal Data Foundry" built upon a specific, high-performance stack: **Qwen3-VL**, **Google ADK**, and **LanceDB**. This section analyzes the technical capabilities of each component and justifies their selection.

### **3.1 The Engine: Qwen3-VL (Vision-Language Model)**

**Qwen3-VL** (specifically the 235B-A22B "Thinking" variant or the efficient 32B Instruct) is chosen not merely as a model, but as a "cognitive engine" capable of structured data extraction.22  
OCR Supremacy:  
Standard OCR tools (Tesseract) fail miserably on Irish text, particularly with older fonts (Cló Gaelach) or the punctum delens (the dot over a letter indicating lenition, e.g., ḃ \= bh). Qwen3-VL supports 32 languages and is engineered to handle "in-the-wild" text: blurred, tilted, handwritten, or low-light.24 This capability is non-negotiable for mining Irish archives (Dúchas.ie) which contain handwritten folklore records.  
"Thinking" Mode (System 2 Reasoning):  
Unlike standard VLMs that simply caption images, Qwen3-VL possesses a "Thinking" mode (similar to Chain-of-Thought). When presented with a scanned page of a bilingual book, it does not just output a stream of text. It can be prompted to:

1. Analyze the spatial layout (columns, side-by-side translation).  
2. Reason about alignment ("The paragraph on the left clearly corresponds to the paragraph on the right").  
3. Self-correct OCR errors based on semantic context.  
   This reasoning capability allows for the extraction of aligned parallel data from unstructured PDFs, a task that previously required manual human effort.25

Agentic Interaction:  
Qwen3-VL is trained to be a "Visual Agent," meaning it can navigate Graphical User Interfaces (GUIs). It can interpret screenshots of web pages, identify "Download" buttons, or navigate through paginated digital archives. This allows us to build agents that autonomously "browse" Irish cultural websites to scrape content, rather than writing brittle HTML scrapers for every site.23

### **3.2 The Orchestrator: Google Agent Development Kit (ADK)**

**Google ADK** provides the structural framework to control Qwen3-VL and manage the complexity of the data pipeline. It moves beyond simple scripting to define a robust multi-agent system.26  
The "Artifact" Architecture:  
The most critical feature of ADK for this project is its handling of Artifacts. In ADK, an Artifact is a typed, versioned data object—not just a variable in memory.28

* *Implementation:* A scanned page from the National Library of Ireland enters the system as an ImageArtifact. The OCR agent processes it and produces a TextArtifact (JSON). The Translation agent produces a TranslationArtifact.  
* *Benefit:* This creates an immutable lineage. If we later improve our OCR prompting strategy, we can trace back to the original ImageArtifact and re-process it without re-scraping. This versioning is essential for iterative dataset development.28

Tool Abstraction:  
ADK allows us to wrap Qwen3-VL, Python scripts, and LanceDB queries as standard Tools.30

* *Example:* We can define a save\_to\_lancedb tool. The agent doesn't need to know the database schema; it simply calls the tool with a JSON object, and the tool handles the serialization and insertion. This decoupling allows us to swap out backend components (e.g., changing embedding models) without rewriting the agent logic.31

### **3.3 The Substrate: LanceDB**

**LanceDB** serves as the multimodal lakehouse. It fundamentally differs from traditional databases (SQL) or vector stores (Pinecone) by natively handling **multimodal data** via the **Lance** columnar format.32  
The Multimodal Schema:  
In LanceDB, we can define a schema where a single row contains the raw image (as a binary blob), the extracted text, and the vector embeddings for both.

* *Technical Detail:* LanceDB uses **Pydantic** models to define schemas. This allows strict type validation before data ingestion.  
  Python  
  class IrishData(LanceModel):  
      image: bytes \= func.SourceField() \# The raw image data  
      irish\_text: str \= func.SourceField()  
      english\_text: str \= func.SourceField()  
      vector: Vector(1536) \= func.VectorField() \# Embedding

This "Deep Search" capability means we can query the database using text ("Find me sentences about fishing") and retrieve the corresponding *images* to verify the context.34  
Zero-Copy Training with LanceDataset:  
This is the "killer feature" for our diffusion model training. Traditionally, training on large image-text datasets requires copying data from object storage (S3) to local disk, then loading it into RAM. LanceDB supports LanceDataset for PyTorch, which streams data directly from the Lance files on disk (or S3) to the GPU memory.

* *Impact:* It eliminates the I/O bottleneck and allows us to train on datasets larger than RAM. For a diffusion model that requires millions of iterations, this efficiency is paramount.36

## ---

**4\. Developing the "Multimodal Data Foundry": Implementation Strategy**

This section provides the specific implementation logic for the data generation pipeline, orchestrated by Google ADK.

### **Phase 1: The Archivist (Ingestion Agent)**

**Role:** To autonomously navigate identified repositories of Irish content (e.g., *Tipperary Studies*, *Dúchas.ie*, *Project Gutenberg*) and acquire raw assets.  
**Tools:**

* browser\_tool: A headless browser (controlled via ADK's Computer Use capabilities) to navigate websites.  
* download\_tool: To fetch PDFs and images.  
* artifact\_saver: To persist the raw files as ADK Artifacts.

**Workflow Logic:**

1. The agent visits a URL (e.g., a digital folklore collection).  
2. It uses Qwen3-VL (via the browser tool) to identify links to "Irish Language" or "Bilingual" documents.  
3. It downloads the file.  
4. Crucially, it extracts metadata (Publication Year, Dialect, Source) and saves it alongside the file in the Artifact metadata. This allows us later to filter data (e.g., "Exclude texts pre-1950 to avoid archaic spelling").

### **Phase 2: The Analyst (Extraction & Alignment Agent)**

**Role:** To convert raw image artifacts into structured text.  
**Tools:**

* qwen\_vision\_tool: A wrapper around the Qwen3-VL API.  
* image\_cropper: To split PDF pages into individual processing units.

The "Thinking" Prompt Strategy:  
To leverage Qwen3-VL's reasoning, the prompt must be explicit:  
"You are an expert archivist. Analyze this image. It is a page from a bilingual book.

1. Identify the layout structure (e.g., Irish in left column, English in right).  
2. Transcribe the Irish text exactly. *Note: If you see a dot over a consonant (e.g., ḃ), transcribe it as the consonant followed by 'h' (bh).*  
3. Transcribe the corresponding English text.  
4. Output a JSON list of pairs: \[{'irish': '...', 'english': '...'}\].  
5. If the text is monolingual Irish, generate a summary of the visual context (images) to serve as a synthetic English caption."

Handling Handwriting:  
For handwritten manuscripts, the prompt is adjusted to request a "diplomatic transcription" (preserving errors) and a "normalized transcription" (standardizing spelling). Qwen3-VL's training on vast multilingual corpora allows it to infer the intended word even if the handwriting is ambiguous, utilizing the semantic context of the sentence.25

### **Phase 3: The Translator (Synthetic Generation Agent)**

**Role:** To generate synthetic translations for monolingual data and perform quality assurance.  
**Workflow:**

1. **Forward Translation:** For monolingual Irish text, the agent uses Qwen3-VL (or a specialized model like UCCIX if integrated) to generate an English translation.  
2. **Visual Grounding:** If the source was an image (e.g., a photo with an Irish caption), the agent asks Qwen3-VL to "Describe this image in English." This creates a triplet: (Image, Irish Caption, English Description). This is distinct from translation; it is *grounding*. The English description describes the *scene*, not just the text, providing richer semantic signals for the diffusion model.  
3. **Back-Translation Cycle (Quality Control):** The agent translates the synthetic English back into Irish. It then computes the **BLEU** and **BERTScore** between the original Irish and the back-translated Irish.  
4. **Filtering:** Pairs with a consistency score below a threshold (e.g., 0.7) are discarded or flagged for human review. This rigorous filtering prevents the "poisoning" of the dataset with hallucinations.37

### **Phase 4: The Curator (Storage Agent)**

**Role:** To index the validated data into LanceDB.  
**Tools:**

* lancedb\_insert\_tool: Validates the data against the Pydantic schema and inserts it.  
* embedding\_tool: Uses a multilingual embedding model (e.g., LaBSE or text-embedding-3-large) to generate vectors for the text and images.

**Schema Implementation (Python/Pydantic):**

Python

import lancedb  
from lancedb.pydantic import LanceModel, Vector  
from lancedb.embeddings import get\_registry

\# Initialize embedding function (e.g., OpenAI or OpenCLIP)  
func \= get\_registry().get("openai").create(name="text-embedding-3-large")

class IrishMultimodalPair(LanceModel):  
    \# Metadata  
    source\_id: str  
    dialect: str  
    year: int  
      
    \# Data  
    image\_bytes: bytes \= func.SourceField() \# For training VLMs  
    irish\_text: str \= func.SourceField()  
    english\_text: str \= func.SourceField()  
      
    \# Embeddings (Auto-generated by LanceDB)  
    irish\_vector: Vector(3072) \= func.VectorField()  
    english\_vector: Vector(3072) \= func.VectorField()  
      
    \# Quality Metrics  
    synthetic\_score: float \# From the Back-Translation phase

\# Connect and Create Table  
db \= lancedb.connect("gs://my-irish-dataset-bucket")  
table \= db.create\_table("training\_corpus", schema=IrishMultimodalPair)

This code snippet illustrates how LanceDB abstracts away the complexity of vectorization. By defining func.VectorField(), LanceDB automatically computes and stores the embeddings whenever new text is added.34

## ---

**5\. Improving English-Irish Translation: Training the Diffusion Model**

With a high-quality, multimodal dataset residing in LanceDB, we proceed to the training phase. We recommend a **Block Diffusion** architecture, initialized from a pre-trained multilingual backbone (like Qwen or Llama), and trained using the **NeoDiff** objective.

### **5.1 Model Architecture: Hybrid Block-Diffusion**

We do not train from scratch. We take a pre-trained AR model (e.g., **Qomhrá 8B** or **Qwen 7B**) and adapt it.

* **Adaptation:** We replace the standard causal masking (which hides all future tokens) with **Block Masking**. We divide the sequence into blocks of size $K$ (e.g., $K=8$). Inside each block, we allow full bidirectional attention.  
* **Diffusion Head:** We add a lightweight Multi-Layer Perceptron (MLP) head on top of the transformer output. This head projects the hidden states to the continuous latent space where the diffusion process occurs.

### **5.2 The Training Objective (Loss Function)**

We employ the composite loss function derived from NeoDiff, which ensures the model learns both the global structure and the local token details.5

$$L \= \\lambda\_1 L\_z \+ \\lambda\_2 L\_{\\tau} \+ \\lambda\_3 L\_{anchor}$$

1. **Latent Variable Loss ($L\_z$):** This is the standard diffusion loss (Mean Squared Error). It measures how well the model predicts the denoised latent vector $z\_0$ given the noisy vector $z\_t$ and the extrinsic time $t$.  
   * *Role:* Ensures the model learns the semantic "trajectory" of the Irish sentence.  
2. **Time Predictor Loss ($L\_{\\tau}$):** This trains the internal mechanism that decides *which* tokens to denoise first.  
   * *Role:* For Irish, the model will learn that prepositions and particles (which trigger mutations) should be denoised *before* the nouns they modify. This is the key to solving the mutation problem.  
3. **Anchor Loss ($L\_{anchor}$):** A cross-entropy loss that forces the predicted continuous vector $\\hat{z}\_0$ to map to a valid token in the discrete vocabulary.  
   * *Role:* Prevents the model from generating "gibberish vectors" that don't correspond to real Irish words.

### **5.3 Curriculum Learning via LanceDB**

We utilize the metadata stored in LanceDB to implement **Curriculum Learning**.

* **Stage 1 (Easy):** We query LanceDB for short sentences ($\<15$ words) with high synthetic\_score (\>0.9) and modern spelling. This stabilizes the diffusion training.  
* **Stage 2 (Hard):** We introduce complex sentences, older texts, and synthetic data with lower confidence.  
* **Stage 3 (Multimodal):** We introduce the image-text pairs. We condition the diffusion model on the image embeddings (retrieved from the image\_bytes column in LanceDB). This grounds the translation in visual reality, helping the model distinguish between homonyms based on visual context.21

## ---

**6\. Strategic Implications and Future Outlook**

The methodology proposed herein extends beyond the immediate technical goal of improving BLEU scores. It represents a strategic blueprint for the preservation and revitalization of low-resource languages in the AI era.

### **6.1 Solving "Catastrophic Forgetting" of Syntax**

Current fine-tuning methods (SFT) often lead to models that speak Irish with English grammar ("Béarlachas"). By using **NeoDiff's** intrinsic timing, the model is forced to learn the *structure* of the language. It learns that in Irish, the verb comes first and dictates the form of the subject. The diffusion process allows it to "go back" and adjust the verb mutation once the subject is fully realized—a corrective capability that AR models fundamentally lack.

### **6.2 The "Living Dataset"**

Traditional datasets are static snapshots (e.g., CommonCrawl 2023). The **Google ADK** approach creates a *living dataset*. The "Archivist" agent can be scheduled to run continuously, monitoring Irish language news sites (*Tuairisc.ie*, *RTÉ*) and social media. It constantly ingests new terms and neologisms into LanceDB. The LanceDB "Version Travel" feature allows us to train models on "Irish as it was spoken in 2025" vs "2026," enabling longitudinal studies of language evolution.33

### **6.3 Data Archaeology**

The use of **Qwen3-VL** opens the door to "Data Archaeology." Ireland possesses vast non-digitized archives. This pipeline provides a scalable, automated way to turn physical assets into training data. It transforms the definition of "Low Resource" from "lacking digital text" to "possessing untapped physical wealth."

## **7\. Conclusion**

The convergence of **Diffusion Models** and **Agentic Workflows** offers a definitive solution to the stagnation of low-resource NMT. By moving away from the rigid sequentiality of autoregression to the flexible, context-aware refinement of **NeoDiff** and **Block Diffusion**, we can model the complex morphology of the Irish language with unprecedented fidelity.  
However, the model is only the engine; data is the fuel. The proposed "Multimodal Data Foundry"—powered by **Qwen3-VL's** reasoning, orchestrated by **Google ADK**, and anchored by **LanceDB**—provides the refinery needed to produce this fuel. It transforms the challenge of Irish NMT from a problem of scarcity into a problem of engineering. We are no longer limited by the number of translated sentences on the web; we are limited only by our capacity to mine the rich, multimodal seams of Irish culture that already exist. This holistic approach ensures that the Irish language will not merely survive in the digital age but will thrive as a first-class citizen of the AI landscape.

#### **Works cited**

1. A Survey on Diffusion Language Models \- arXiv, accessed December 23, 2025, [https://arxiv.org/html/2508.10875v2](https://arxiv.org/html/2508.10875v2)  
2. Lancelot39/DiffusionNAT: EACL2024: Diffusion-NAT: Self-Prompting Discrete Diffusion for Non-Autoregressive Text Generation \- GitHub, accessed December 23, 2025, [https://github.com/Lancelot39/DiffusionNAT](https://github.com/Lancelot39/DiffusionNAT)  
3. What is the difference between discrete and continuous diffusion models? \- Milvus, accessed December 23, 2025, [https://milvus.io/ai-quick-reference/what-is-the-difference-between-discrete-and-continuous-diffusion-models](https://milvus.io/ai-quick-reference/what-is-the-difference-between-discrete-and-continuous-diffusion-models)  
4. Unifying Continuous and Discrete Text Diffusion with Non ... \- arXiv, accessed December 23, 2025, [https://arxiv.org/abs/2505.22165](https://arxiv.org/abs/2505.22165)  
5. Unifying Continuous and Discrete Text Diffusion with Non-simultaneous Diffusion Processes \- ACL Anthology, accessed December 23, 2025, [https://aclanthology.org/2025.acl-long.565.pdf](https://aclanthology.org/2025.acl-long.565.pdf)  
6. Continuous Diffusion Model for Language Modeling \- arXiv, accessed December 23, 2025, [https://arxiv.org/html/2502.11564v1](https://arxiv.org/html/2502.11564v1)  
7. Unifying Continuous and Discrete Text Diffusion with Non-simultaneous Diffusion Processes \- ACL Anthology, accessed December 23, 2025, [https://aclanthology.org/2025.acl-long.565/](https://aclanthology.org/2025.acl-long.565/)  
8. \[Literature Review\] Unifying Continuous and Discrete Text Diffusion with Non-simultaneous Diffusion Processes \- Moonlight, accessed December 23, 2025, [https://www.themoonlight.io/en/review/unifying-continuous-and-discrete-text-diffusion-with-non-simultaneous-diffusion-processes](https://www.themoonlight.io/en/review/unifying-continuous-and-discrete-text-diffusion-with-non-simultaneous-diffusion-processes)  
9. \[Papierüberprüfung\] Unifying Continuous and Discrete Text Diffusion with Non-simultaneous Diffusion Processes \- Moonlight, accessed December 23, 2025, [https://www.themoonlight.io/de/review/unifying-continuous-and-discrete-text-diffusion-with-non-simultaneous-diffusion-processes](https://www.themoonlight.io/de/review/unifying-continuous-and-discrete-text-diffusion-with-non-simultaneous-diffusion-processes)  
10. Block Diffusion \- arXiv, accessed December 23, 2025, [https://arxiv.org/pdf/2503.09573?](https://arxiv.org/pdf/2503.09573)  
11. From Next-Token to Next-Block: A Principled Adaptation Path for Diffusion LLMs \- arXiv, accessed December 23, 2025, [https://arxiv.org/html/2512.06776v1](https://arxiv.org/html/2512.06776v1)  
12. Block Diffusion \- arXiv, accessed December 23, 2025, [https://arxiv.org/pdf/2503.09573](https://arxiv.org/pdf/2503.09573)  
13. Irish-based Large Language Model with Extreme Low-Resource Settings in Machine Translation \- ResearchGate, accessed December 23, 2025, [https://www.researchgate.net/publication/384214551\_Irish-based\_Large\_Language\_Model\_with\_Extreme\_Low-Resource\_Settings\_in\_Machine\_Translation](https://www.researchgate.net/publication/384214551_Irish-based_Large_Language_Model_with_Extreme_Low-Resource_Settings_in_Machine_Translation)  
14. Irish-based Large Language Model with Extreme Low-Resource Settings in Machine Translation \- ACL Anthology, accessed December 23, 2025, [https://aclanthology.org/2024.loresmt-1.20.pdf](https://aclanthology.org/2024.loresmt-1.20.pdf)  
15. UCCIX: Irish-eXcellence Large Language Model \- GitHub, accessed December 23, 2025, [https://github.com/ReML-AI/UCCIX](https://github.com/ReML-AI/UCCIX)  
16. ReliableAI/UCCIX-Llama2-13B \- Hugging Face, accessed December 23, 2025, [https://huggingface.co/ReliableAI/UCCIX-Llama2-13B](https://huggingface.co/ReliableAI/UCCIX-Llama2-13B)  
17. Qomhrá: A Bilingual Irish-English Large Language Model \- arXiv, accessed December 23, 2025, [https://arxiv.org/html/2510.17652v1](https://arxiv.org/html/2510.17652v1)  
18. Qomhra: A Bilingual Irish-English Large Language Model \- ResearchGate, accessed December 23, 2025, [https://www.researchgate.net/publication/396715967\_Qomhra\_A\_Bilingual\_Irish-English\_Large\_Language\_Model](https://www.researchgate.net/publication/396715967_Qomhra_A_Bilingual_Irish-English_Large_Language_Model)  
19. About the parallel corpus — Parallel English-Irish Corpus of Legislation \- Gaois, accessed December 23, 2025, [https://www.gaois.ie/en/corpora/parallel/about](https://www.gaois.ie/en/corpora/parallel/about)  
20. Multimodal Neural Machine Translation: A Survey of the State of the Art \- ResearchGate, accessed December 23, 2025, [https://www.researchgate.net/publication/397424851\_Multimodal\_Neural\_Machine\_Translation\_A\_Survey\_of\_the\_State\_of\_the\_Art](https://www.researchgate.net/publication/397424851_Multimodal_Neural_Machine_Translation_A_Survey_of_the_State_of_the_Art)  
21. Multimodal Neural Machine Translation: A Survey of the State of the Art \- ACL Anthology, accessed December 23, 2025, [https://aclanthology.org/2025.emnlp-main.1125.pdf](https://aclanthology.org/2025.emnlp-main.1125.pdf)  
22. Alibaba Launches Qwen3-VL With Open Source Flagship Model \- Analytics India Magazine, accessed December 23, 2025, [https://analyticsindiamag.com/ai-news-updates/alibaba-launches-qwen3-vl-with-open-source-flagship-model/](https://analyticsindiamag.com/ai-news-updates/alibaba-launches-qwen3-vl-with-open-source-flagship-model/)  
23. Qwen3-VL: Open Source Multimodal AI with Advanced Vision \- Kanaries Docs, accessed December 23, 2025, [https://docs.kanaries.net/articles/qwen3-vl](https://docs.kanaries.net/articles/qwen3-vl)  
24. Qwen/Qwen3-VL-8B-Instruct \- Hugging Face, accessed December 23, 2025, [https://huggingface.co/Qwen/Qwen3-VL-8B-Instruct](https://huggingface.co/Qwen/Qwen3-VL-8B-Instruct)  
25. Qwen3-VL is the multimodal large language model series developed by Qwen team, Alibaba Cloud. \- GitHub, accessed December 23, 2025, [https://github.com/QwenLM/Qwen3-VL](https://github.com/QwenLM/Qwen3-VL)  
26. Overview of Agent Development Kit | Vertex AI Agent Builder \- Google Cloud Documentation, accessed December 23, 2025, [https://docs.cloud.google.com/agent-builder/agent-development-kit/overview](https://docs.cloud.google.com/agent-builder/agent-development-kit/overview)  
27. Agent Development Kit \- Google, accessed December 23, 2025, [https://google.github.io/adk-docs/](https://google.github.io/adk-docs/)  
28. Artifacts \- Agent Development Kit \- Google, accessed December 23, 2025, [https://google.github.io/adk-docs/artifacts/](https://google.github.io/adk-docs/artifacts/)  
29. Empowering Your AI Agent: File Downloads with Google ADK Artifacts | by Naitik Gada, accessed December 23, 2025, [https://medium.com/@naitikgada/empowering-your-ai-agent-file-downloads-with-google-adk-artifacts-2ddb00fec0e2](https://medium.com/@naitikgada/empowering-your-ai-agent-file-downloads-with-google-adk-artifacts-2ddb00fec0e2)  
30. Function tools \- Google, accessed December 23, 2025, [https://google.github.io/adk-docs/tools-custom/function-tools/](https://google.github.io/adk-docs/tools-custom/function-tools/)  
31. Custom Tools for ADK \- Agent Development Kit \- Google, accessed December 23, 2025, [https://google.github.io/adk-docs/tools-custom/](https://google.github.io/adk-docs/tools-custom/)  
32. LanceDB | Vector Database for RAG, Agents & Hybrid Search, accessed December 23, 2025, [https://lancedb.com/](https://lancedb.com/)  
33. Building an Open Lakehouse for Multimodal AI with LanceDB on Amazon S3 Express One Zone | by Soumil Shah | Nov, 2025 | Medium, accessed December 23, 2025, [https://medium.com/@shahsoumil519/building-an-open-lakehouse-for-multimodal-ai-with-lancedb-on-s3-937106455a2e](https://medium.com/@shahsoumil519/building-an-open-lakehouse-for-multimodal-ai-with-lancedb-on-s3-937106455a2e)  
34. Jina Embedding Models \- LanceDB, accessed December 23, 2025, [https://lancedb.com/docs/integrations/embedding/jina/](https://lancedb.com/docs/integrations/embedding/jina/)  
35. Multimodal Myntra Fashion Search Engine Using LanceDB, accessed December 23, 2025, [https://lancedb.com/blog/multimodal-myntra-fashion-search-engine-using-lancedb/](https://lancedb.com/blog/multimodal-myntra-fashion-search-engine-using-lancedb/)  
36. Distributed Training with LanceDB and Tigris | Tigris Object Storage, accessed December 23, 2025, [https://www.tigrisdata.com/blog/lancedb-training/](https://www.tigrisdata.com/blog/lancedb-training/)  
37. Scaling Low-Resource MT via Synthetic Data Generation with LLMs \- ACL Anthology, accessed December 23, 2025, [https://aclanthology.org/2025.emnlp-main.1408.pdf](https://aclanthology.org/2025.emnlp-main.1408.pdf)  
38. Vectorizing and Embedding Data with LanceDB, accessed December 23, 2025, [https://lancedb.com/docs/embedding/](https://lancedb.com/docs/embedding/)  
39. Unifying Continuous and Discrete Text Diffusion with Non ... \- arXiv, accessed December 23, 2025, [https://arxiv.org/pdf/2505.22165](https://arxiv.org/pdf/2505.22165)