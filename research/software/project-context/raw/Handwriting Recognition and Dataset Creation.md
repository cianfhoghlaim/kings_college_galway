# **Advanced Architectures for Bilingual Heritage Archiving and Mathematical Document Intelligence: A Deep Research Report**

## **1\. Executive Context and Architectural Overview**

The intersection of computational linguistics, computer vision, and archival science is currently undergoing a profound transformation driven by the emergence of Multimodal Large Language Models (MLLMs) and advanced vector retrieval mechanisms. This research report provides a comprehensive technical analysis of a dual-objective engineering challenge: the automated curation of a large-scale Irish Gaelic (*Gaeilge*) heritage dataset from the National Folklore Collection (*Dúchas*) and the development of a bespoke mathematical handwriting recognition system capable of interpreting tertiary-level calculus and linear algebra.  
The user's query presents a sophisticated edge-case in Document Intelligence. It requires the synthesis of unstructured heritage data—characterized by 1930s Gaelic script, dialectal variance, and non-standard orthography—with highly structured yet spatially complex mathematical handwriting. The proposed software stack, integrating crawl4ai for robust data ingestion and a combination of ColPali, Docling, DeepSeek-Math, and Qwen-VL architectures for analysis, represents a cutting-edge approach to these distinct but related problems.  
This report evaluates the feasibility of scraping the Dúchas archive 1 to create a "Rosetta Stone" dataset of aligned image-text pairs, leveraging the legacy XML infrastructure before its imminent deprecation. Simultaneously, it deconstructs the user's personal mathematical manuscript 1, analyzing the spatial topology of phase portraits, matrix algebra, and Hamiltonian calculus to determine the optimal inference pipeline. We critically assess the role of vector retrieval (ColPali) not merely as a search engine, but as a mechanism for "style injection" into generative models, enabling the system to adapt to the user's specific idiolect without the computational overhead of full parameter fine-tuning.  
The analysis suggests that while off-the-shelf Optical Character Recognition (OCR) solutions are insufficient for these tasks, a composite pipeline utilizing the resolution-aware vision encoders of Qwen-VL, the mathematical reasoning priors of DeepSeek-Math-V2, and the document layout understanding of Docling can achieve state-of-the-art results. This report details the architectural blueprint for such a system, exploring the nuances of bilingual tokenization, the preservation of mathematical semantics during transcription, and the ethical curation of digital heritage.

## ---

**2\. The Dúchas Dataset: Engineering a Heritage Scraper**

The Schools’ Collection (*Bailiúchán na Scol*), compiled between 1937 and 1939, represents a corpus of immense complexity and cultural value. With over 500,000 pages of folklore collected by schoolchildren in the Irish Free State, it offers a unique dataset for training HTR (Handwritten Text Recognition) models specialized in the Irish language. However, the absence of a documented, modern API necessitates a robust, custom-engineered scraping solution.

### **2.1 Technical Analysis of the Dúchas Infrastructure**

The provided research material 1 offers a critical glimpse into the backend structure of the Dúchas web interface. The snippet explicitly reveals a warning: *"We will soon deprecate our XML Application Programming Interface and a new, comprehensive JSON API will be made available."* This statement is the pivot point for the entire scraping strategy. It indicates that the current XML links are likely direct file paths or legacy endpoints that expose the Text Encoding Initiative (TEI) compliant transcriptions used by the project’s researchers.  
TEI XML is the gold standard for digital humanities because, unlike a simple JSON text field, it often preserves metadata about line breaks (\<lb/\>), page breaks (\<pb/\>), and unclear text (\<unclear\>). Accessing these files before the API transition is paramount. A JSON API, while more modern, is often designed for frontend consumption—delivering the text as a continuous string for display—rather than preserving the spatial and structural integrity required to train an HTR model. If the JSON API flattens the document structure, the ability to align specific lines of handwriting in the image with specific sentences in the text is lost, significantly degrading the quality of the training data.  
The URL structure identified in the snippet—https://www.duchas.ie/en/cbes/4606380/4593631—implies a strict hierarchical taxonomy. The segment cbes denotes the collection (Schools' Collection), 4606380 likely represents the Volume ID (often correlating to a specific school or parish), and 4593631 is the Page ID. This integer-based indexing suggests that the database can be traversed sequentially or via an index scraping method, reducing the complexity of the crawler's discovery logic.

### **2.2 Implementing the Crawl4AI Strategy**

The user’s suggestion to utilize crawl4ai is technically astute and superior to traditional static scrapers like BeautifulSoup or Scrapy for this specific application. The Dúchas site 1 employs a sophisticated image viewer (likely utilizing a tiling standard like IIIF or a proprietary Zoomify implementation) to display high-resolution scans. These viewers rely heavily on client-side JavaScript to request image tiles and stitch them into a viewable canvas. A standard HTTP request would only return the skeleton HTML, missing the critical image assets.  
crawl4ai, which typically wraps headless browser technologies such as Playwright or Puppeteer, executes the JavaScript payload. This allows the scraper to interact with the Document Object Model (DOM) as a user would. The scraping pipeline must be designed to perform specific "human-like" interactions:

1. **Navigation:** Iterate through the volume indices.  
2. **Asset Exposure:** On each page, the scraper may need to simulate a click event on the "Download" or "XML" button to trigger the generation of the temporary download link.  
3. **Wait States:** The scraper must implement intelligent waiting (e.g., await page.waitForSelector('.xml-download-link')) to ensure the asset is ready before attempting retrieval.

The "Zoomify" nature of the images presents a secondary challenge. The "Download" button might provide a pre-generated JPEG, but for HTR purposes, the highest possible resolution is required. If the download button provides a compressed version, the scraper might need to intercept the network traffic (HAR logging) to identify the source tiles of the IIIF server and reconstruct the full-resolution image programmatically. However, based on the snippet 1, the interface appears to offer direct downloads, which simplifies the pipeline to a button-click automation task.

### **2.3 Linguistic and Orthographic Challenges in Scraping**

The Dúchas dataset is not merely a collection of images and text; it is a repository of a language in transition. The 1930s marked a period where the traditional Gaelic script (*Cló Gaelach*) was still in widespread use, coexisting with the Roman script (*Cló Rómhánach*).  
The XML transcriptions are likely to be normalized to some degree, but the images will exhibit the distinct features of the Gaelic script:

* **The Ponc Séimhithe:** The dot above a consonant (e.g., ḃ, ċ, ḋ) indicating lenition, which is represented by an 'h' in modern Romanized Irish (bh, ch, dh).  
* **Distinct Letterforms:** The Gaelic 'r', 's', and 'g' differ significantly from their Roman counterparts. The 's' resembles a modern 'r', and the 'r' resembles a simplified 'x'.

A naive scraping pipeline that blindly pairs the XML text with the image without accounting for this script drift will create a "noisy" dataset. For example, if the image shows a word with a *ponc* (e.g., "ḃean") and the XML contains the Romanized expansion ("bhean"), the HTR model might learn to hallucinate the letter 'h' where none exists visually. This is acceptable if the goal is translation/transliteration, but problematic if the goal is diplomatic transcription (reproducing exactly what is on the page).  
The metadata visible in snippet 1—Collector (*Bailitheoir*), Informant (*Faisnéiseoir*), and Location—is not ancillary; it is a critical hyper-parameter for training. Irish dialects (Connacht, Munster, Ulster) vary significantly in vocabulary and spelling conventions. By scraping this metadata alongside the primary assets, the user can create a "conditioned" dataset. This allows for the training of an HTR model that accepts a "dialect token" as part of its prompt, priming the language model to expect specific spelling variants associated with, for example, Co. Galway (as seen in the snippet) versus Co. Kerry.  
The user's plan to automate this with crawl4ai is feasible and recommended, provided the scraper includes logic to parse the sidebar metadata 1 and inject it into the dataset manifest. The scraper should also implement a "Quality Gate," checking the file size of the downloaded XML. An XML file of only a few bytes likely indicates an empty or placeholder transcription, which should be discarded to prevent the pollution of the training set with null data.

## ---

**3\. Computational Mathematics: Analyzing the User’s Dataset**

The second component of the user's request involves a personal dataset of tertiary-level mathematics notes.1 This represents a radically different challenge from the Dúchas archive. While the folklore collection is narrative and linear, mathematical handwriting is spatial, symbolic, and diagrammatic.

### **3.1 Content Analysis of the "Summer Exam" Document**

The uploaded snippet 1 reveals a document rich in advanced mathematical concepts, specifically Dynamical Systems, Linear Algebra, and Calculus. Analyzing the visual syntax of this document is crucial for selecting the correct recognition architecture.

#### **3.1.1 The Linear Algebra Zone**

The document features 2x2 matrices used for eigenvalue analysis:

$$\\begin{pmatrix} 2-\\lambda & 3 \\\\ \-1 & 1-\\lambda \\end{pmatrix}$$

This structure defies the standard "reading order" assumption of most OCR engines. A typical OCR model scans left-to-right, top-to-bottom. It would likely transcribe the above matrix as a single line of text: "2 \- lambda 3 \- 1 1 \- lambda". This linearizes the data in a way that destroys its mathematical meaning. The vertical alignment of the columns is the defining feature of the matrix.  
The user's handwriting in 1 is cursive and rapid, typical of an exam setting. The brackets enclosing the matrices are drawn loosely. A key challenge here is **Layout Analysis**. The model must recognize that the two large parentheses define a distinct "zone" of the page where the rules of reading change from linear text to tabular alignment.

#### **3.1.2 The Calculus and Greek Notation**

The text contains frequent use of Greek letters ($\\lambda, \\Delta, \\tau$) and calculus notation ($\\frac{dx}{dt}, \\dot{x}$). The presence of the "dot" notation for time derivatives ($\\dot{x}$) is a notorious failure point for standard vision models. The dot is often only a few pixels in size. If the vision encoder resizes the image to a standard square (e.g., 224x224 pixels) for processing, the dot may vanish due to downsampling. The user’s dataset requires a model with **High-Resolution Encoding** capabilities to preserve these semantic micro-features.  
Furthermore, the document contains complex algebraic expansions like $x(x^2 \- 4)$. The distinction between $x^2$ (x squared) and $x2$ (x times 2\) relies entirely on the relative vertical position of the character '2'. A model that is not "spatially aware" will flatten this to "x2", rendering the equation mathematically nonsensical.

#### **3.1.3 The Diagrammatic Reasoning (Phase Portraits)**

Page 2 of the snippet 1 illustrates a "Delta-Tau Plane" and a Phase Portrait (a spiral). This is the most complex data modality.

* **The Text-Image Dependency:** The text describes an "unstable spiral." The diagram shows a curve spiraling outwards from the origin. The arrowheads on the curve indicate the direction of flow.  
* **The HTR Challenge:** A standard HTR model will ignore the drawing. A standard image captioning model might say "a drawing of a spiral." Neither captures the full truth. The requirement is a **Multimodal Model** that can link the text "unstable" to the visual feature of "outward arrows." The model must "read" the diagram as part of the sentence.

### **3.2 The Feasibility of Fine-Tuning on User Data**

The user asks if they can fine-tune models to *generate* or *detect* their particular handwriting.  
"Detecting" (Transcribing) is highly feasible. By annotating a subset of these notes (e.g., manually typing the LaTeX code for 50 pages), the user can create a high-quality fine-tuning dataset. The user's handwriting is consistent (idiolect), meaning a model can quickly overfit to their specific style of drawing $\\lambda$ or $\\zeta$, achieving high accuracy with relatively little data.  
"Generating" handwriting is a different proposition. This would imply creating *new* images that look like the user wrote them. While generative diffusion models (like Stable Diffusion) can be fine-tuned with LoRA (Low-Rank Adaptation) to mimic visual styles, the utility of this for a mathematics workflow is limited compared to the utility of transcribing the notes into editable LaTeX. We assume the primary goal is **transcription** (image-to-text) and **detection** (retrieval), as generating fake handwritten notes has fewer practical applications in research workflows.

## ---

**4\. The Vector Paradigm: ColPali and Retrieval-Augmented HTR**

The user specifically inquires about **ColPali** and its ability to "finetune models to be able to generate or detect" handwriting. This requires a clarification of what ColPali is and how it functions within an HTR pipeline.

### **4.1 ColPali: The Architecture of Visual Retrieval**

ColPali is not a generative model; it is a **Retrieval Model**. It is a fusion of the **ColBERT** (Contextualized Late Interaction over BERT) retrieval architecture and the **PaliGemma** Vision-Language Model.

* **The Mechanism:** Traditional retrieval systems convert a document to text via OCR, then embed that text into a single vector. If the OCR fails (which is likely with the user's handwritten math), the text is garbage ("x2 \- 4" instead of $x^2 \- 4$), and the vector is useless. You can never find that document again.  
* **The ColPali Advantage:** ColPali does not OCR the document first. It divides the image of the page into a grid of patches. It embeds each patch into a vector space. It creates a "bag of visual embeddings" for the page.  
* **Late Interaction:** When the user searches for "unstable spiral," the query text is embedded into vectors. The model then performs a "MaxSim" operation, comparing the query vectors directly against the image patch vectors. This allows it to match the *concept* of a spiral or the *visual shape* of the equation $x^2-4$ without ever explicitly transcribing it first.

### **4.2 Using ColPali for "Detection" (Style Retrieval)**

The user asks if ColPali can be used to *detect* their handwriting. The answer is yes, but via a **Retrieval-Augmented Generation (RAG)** workflow.  
ColPali serves as the **Memory Module** of the system.

1. **Indexing:** The user processes all their past exam papers 1 through ColPali. The system builds a vector index of the user's specific way of writing.  
2. **Inference:** When the user uploads a new, messy note, the system uses ColPali to search the archive: "Find other instances where the user wrote a matrix like this."  
3. **Few-Shot Prompting:** ColPali retrieves three clean examples of the user's matrix handwriting (where the ground truth LaTeX is known). These examples are fed into the context window of the generative model (Qwen or DeepSeek).  
4. **Result:** The generative model looks at the new messy note *and* the three clean examples. It infers: "Ah, when this user draws a squiggle like that, they mean $\\xi$. I will transcribe it as \\xi."

This uses ColPali to *adapt* the model to the user's handwriting without the expensive process of updating the model's weights (fine-tuning). It is "In-Context Learning" powered by visual retrieval.

## ---

**5\. Generative Frontiers: DeepSeek, Qwen, and Docling**

The user requests deep research into the capabilities of **Docling**, **DeepSeek-OCR/Math-V2**, and **Qwen3-VL** (interpreted here as the Qwen-VL lineage, specifically Qwen2-VL and its trajectory).

### **5.1 Docling: The Structural Parser**

**Docling**, developed by IBM Research, is essential for the *pre-processing* stage. It is not an HTR engine itself, but a **Document Layout Analysis (DLA)** system.  
In the context of the user's exam paper 1, Docling plays a vital role. The document is not a uniform block of text. It has:

* **Headers:** "MP491: Summer Exam" (Page 1).  
* **Metadata:** "Cian Mac Liatháin" (Student Name).  
* **Content Blocks:** The math questions.

If we feed the entire page into an HTR model, it might transcribe the header into the middle of an equation if the layout is complex. Docling parses the visual structure of the PDF. It identifies the bounding boxes of the header, the footer, and the body text. It outputs a structured JSON or Markdown representation that segregates these elements.  
For the **Dúchas** dataset, Docling is equally valuable. It can separate the main story text from the marginalia (teacher's corrections) or the page numbers, ensuring that the training data derived from the scrape is clean and focused only on the target handwriting.

### **5.2 DeepSeek-Math-V2 and DeepSeek-OCR**

**DeepSeek-Math-V2** is a specialized Mixture-of-Experts (MoE) language model trained on a massive corpus of mathematical content (arXiv papers, code, textbooks).

* The Reasoning Advantage: DeepSeek-Math brings "Cognitive Corrections" to the HTR process. A standard OCR model looks at pixels and guesses characters. If the user writes "2 \+ 2 \= 4" but the '4' is messy and looks like an 'A', standard OCR outputs "2 \+ 2 \= A".  
  DeepSeek-Math, however, understands the logic. It predicts the next token based on mathematical rules. It sees "2 \+ 2 \=" and assigns a high probability to "4". Even if the visual evidence is ambiguous, the mathematical prior guides the model to the correct transcription.  
* **DeepSeek-VL / OCR:** The multimodal variant (DeepSeek-VL or Janus) integrates vision encoders. While highly capable, benchmarks suggest that its "in-the-wild" handwriting recognition for non-Chinese scripts is strong but perhaps slightly less robust than Qwen2-VL for pure transcription tasks. Its strength lies in *reasoning about* the math (e.g., solving the equation it reads) rather than just reading it.

### **5.3 Qwen2-VL (and the "Qwen3" Trajectory)**

**Qwen2-VL** (specifically the 72B Instruct model) is currently the state-of-the-art for this specific use case. The user's mention of "Qwen3-VL" likely anticipates the next iteration, but the architectural principles of the Qwen-VL line are what matter.

* **Naive Vision Transformer (NaViT):** This is the game-changer for the user's math notes. Traditional models (like CLIP or the original LLaVA) resize all images to a fixed square (e.g., 336x336 pixels).  
  * *The Problem:* A scanned A4 page of handwriting 1 has an aspect ratio of 1:1.4. Squashing it into a square distorts the text. Furthermore, downsampling a 4000-pixel high scan to 336 pixels erases small details like the dot in $\\dot{x}$ or the comma in a list.  
  * *The Qwen Solution:* Qwen2-VL handles **dynamic resolution**. It breaks the image into patches based on its *native* resolution and aspect ratio. It "sees" the image clearly, pixel-for-pixel (up to a token limit). This makes it uniquely suited for the Dúchas scans (which are high-res heritage documents) and the math notes (where detail is critical).  
* **Bilingual Capability:** Qwen2-VL is trained on a massive multilingual corpus. It shows exceptional performance on European languages. For the **Dúchas** dataset, this means it can handle the code-switching between English instructions and Irish stories seamlessly. It does not need to be "told" which language it is reading; it infers it from the token probability distribution.  
* **Video/Sequence Understanding:** The "Qwen3" trajectory focuses heavily on long-context video understanding. While the user has static images, this capability is relevant. A multi-page PDF can be treated as a "video" (a sequence of frames). This allows the model to maintain context across pages. If a variable $x$ is defined on Page 1, the model remembers it when it appears on Page 2, aiding in consistent transcription.

## ---

**6\. Comparison of Models for User Requirements**

The following table synthesizes the deep research into a direct comparison for the user's specific workloads.

| Feature / Requirement | Qwen2-VL (72B) | DeepSeek-Math-V2 | Docling | ColPali |
| :---- | :---- | :---- | :---- | :---- |
| **Primary Role** | Generative HTR & Vision | Mathematical Reasoning | Layout Analysis & Parsing | Visual Retrieval & RAG |
| **Handwriting (Irish)** | **High** (Dynamic Res) | Medium (Generalization) | N/A | **High** (Style Matching) |
| **Handwriting (Math)** | **Very High** (LaTeX Output) | **Very High** (Logic Check) | N/A | High (Equation Search) |
| **Spatial Awareness** | High (NaViT) | Low (Text-only input) | **Very High** (Structure) | Medium (Patch-based) |
| **Diagram Understanding** | **High** (Vector flows) | Low (Needs text description) | Medium (Bitmap detection) | High (Visual similarity) |
| **Code-Switching (En/Ga)** | Excellent | Good | N/A | N/A |
| **Resolution Handling** | **Native / Dynamic** | N/A | Native | Fixed Patching |

## ---

**7\. Proposed Integration Pipeline**

To satisfy the user's request for a "new dataset" and a "mathematical handwriting system," we propose the following unified architecture.

### **7.1 Phase 1: The Dúchas "Heritage Scraper"**

This phase utilizes crawl4ai to secure the raw materials.

1. **Targeting:** The script targets duchas.ie/en/cbes.  
2. **DOM Interaction:** The scraper waits for the .xml-download element (or equivalent) to become interactive.  
3. **Extraction:** It downloads the XML and the corresponding high-res image.  
4. **Metadata Injection:** It scrapes the sidebar for "County," "School," and "Teacher." This metadata is written into a metadata.jsonl file keyed to the image filename.  
5. **Filtering:** A script analyzes the XML. If the character count is \< 50, the file is flagged as "Empty/Untranscribed" and moved to a separate "To-Do" folder for future inference.

### **7.2 Phase 2: The "Math-RAG" System**

This phase addresses the user's personal math notes.

1. **Ingestion:** The PDF 1 is passed to **Docling**. Docling strips the headers and segments the page into "Equation Blocks."  
2. **Indexing:** These blocks are passed to **ColPali**. The visual embeddings are stored in a vector database (e.g., LanceDB).  
3. **Inference (The "Detection" Loop):**  
   * User uploads a new image.  
   * **ColPali** retrieves the top-5 most visually similar equation blocks from the history.  
   * **Qwen2-VL** is prompted: *"You are a mathematical scribe. Convert the central image to LaTeX. Reference the 5 provided examples to understand the user's specific handwriting style for symbols."*  
   * **DeepSeek-Math** (Optional Validator): The generated LaTeX is passed to DeepSeek-Math to check for syntax errors or logical inconsistencies (e.g., checking if the dimensions of the transcribed matrices align).

### **7.3 Phase 3: Fine-Tuning (The "Generation" Loop)**

If the user wishes to "generate" text *in the style* of their handwriting (or rather, fine-tune the model to perfectly transcribe it), **QLoRA** (Quantized Low-Rank Adaptation) is the method.

* **Base Model:** Qwen2-VL-7B-Instruct (manageable on consumer GPUs) or 72B (requires A100s).  
* **Dataset:** The pairs scraped from Dúchas \+ the user's annotated math notes.  
* **Training Objective:** Autoregressive Next-Token Prediction.  
* **Result:** A model adapter (adapter\_model.bin) that can be loaded on top of Qwen2-VL, transforming it into a specialized expert on "1930s Irish Folklore and 2020s Tertiary Calculus."

## ---

**8\. Conclusion and Future Outlook**

The user's project sits at the vanguard of what is technically possible in 2025\. The convergence of **Heritage Scraping** (via crawl4ai) and **Cognitive HTR** (via Qwen/DeepSeek) offers a solution to the "digital dark age" of untranscribed archives. By extracting the TEI XML before the Dúchas API deprecation, the user secures a foundational dataset that links the visual past to the digital future.  
Simultaneously, the application of **ColPali** resolves the "personalization" bottleneck. Instead of training a new model for every student's handwriting, we can simply "retrieve" their style and inject it into the context of a massive, reasoning-capable model. This RAG-for-Vision approach allows the system to interpret the nuanced, multimodal topology of the user's phase portraits and matrix algebra with a fidelity that standard OCR could never achieve. The resulting system will not just "read" the notes; it will, in a very real sense, "understand" the mathematics they contain.

#### **Works cited**

1. Cill Éinne · The Schools' Collection \_ dúchas.ie.pdf