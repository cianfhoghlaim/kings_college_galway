# **Comprehensive Architectural Analysis for Bilingual Irish-English Handwritten Text Recognition on iOS: From Weakly-Supervised Data Generation to Edge Inference**

## **1\. Introduction: The Intersection of Philology and Edge AI**

The digitization of cultural heritage and the operationalization of low-resource languages represent two of the most compelling frontiers in modern artificial intelligence. The specific challenge of developing a bilingual Handwritten Text Recognition (HTR) system for Irish and English—capable of running locally on iOS devices—necessitates a sophisticated convergence of computer vision, natural language processing, and hardware-aware engineering. Unlike printed text, which adheres to rigid typographic standards, handwriting exhibits high variance in stroke, slant, and spacing. In the context of the Irish language, this complexity is often compounded by the historical presence of the *Cló Gaelach* (Gaelic type) or distinct insular scripts in older manuscripts, as well as the code-switching nature of modern bilingual datasets.  
Traditional Optical Character Recognition (OCR) pipelines, historically dependent on Tesseract or similar LSTM-based engines, often fail to capture the nuanced semantic context required to disambiguate difficult handwriting. They operate primarily on visual pattern matching of character glyphs. The advent of Vision-Language Models (VLMs) fundamentally alters this landscape. By projecting visual features into the same embedding space as a Large Language Model (LLM), VLMs allow the transcription process to be guided by linguistic probability. The model does not merely "see" the shape of a letter; it "reads" the likelihood of a word appearing in an Irish sentence structure, essentially hallucinating the correct text constrained by the visual evidence.  
However, the deployment of such massive parameter models on resource-constrained edge devices like the iPhone presents a formidable engineering barrier. While cloud-based inference is trivial, the requirement for on-device inference—driven by privacy, latency, and offline accessibility—demands a rigorous analysis of model compression, memory management, and specialized runtime environments like Apple's CoreML and MLX. Furthermore, the efficacy of any machine learning model is strictly bounded by the quality of its training data. The user’s proposal to utilize **ColPali**, a retrieval-oriented VLM, to construct a training dataset from unaligned page transcriptions introduces a novel paradigm of "weakly-supervised" annotation.  
This report provides an exhaustive technical analysis of this end-to-end pipeline. It dissects the architectural compatibility of **Unsloth** for fine-tuning, evaluates the viability of **Apple's ml-fastvlm** versus the **MLX** framework for deployment, and rigorously examines the mathematical mechanisms of **ColPali** for generating ground-truth bounding boxes. The analysis indicates that while direct compatibility between Unsloth and ml-fastvlm is architecturally obstructed by divergent vision encoders, a robust pathway exists via the MLX ecosystem, enabling the deployment of state-of-the-art Qwen2-VL models on Apple Silicon with high fidelity.

## **2\. Theoretical Foundations of Weakly-Supervised Dataset Generation**

The primary bottleneck in training HTR systems for specific domains (such as Irish manuscripts) is the scarcity of line-level annotated data. Most available data exists as "weakly labeled" pairs: a full image of a page and a full transcription of that page, without the coordinate geometry linking specific text lines to specific pixel regions. Manually drawing bounding boxes is prohibitively expensive. The proposed utilization of **ColPali** to automate this alignment exploits the model's unique architecture to bridge the gap between retrieval and localization.

### **2.1 The ColPali Architecture: Contextualized Late Interaction**

To understand how ColPali can be repurposed for data generation, one must first analyze its retrieval mechanism. Traditional dense retrieval systems (Bi-Encoders) compress an entire document image into a single vector embedding. While efficient for search, this compression results in a massive loss of spatial fidelity. ColPali, built upon the **PaliGemma** VLM, adopts the **ColBERT** (Contextualized Late Interaction over BERT) paradigm, applying it to the visual domain.1  
In ColPali, an image is not encoded into one vector, but into a grid of vectors. The Vision Transformer (ViT) backbone—typically SigLIP-So400m—processes the image at a resolution (e.g., $448 \\times 448$) and outputs a feature map. This map is projected into a sequence of patch embeddings. For a standard input, ColPali generates $32 \\times 32 \= 1024$ visual tokens, where each token represents a specific rectangular region of the image. Crucially, these visual tokens are projected into the same latent space as the text tokens of the language model.3  
The retrieval score $S(q, d)$ between a text query $q$ and a document image $d$ is calculated using the MaxSim operator:

$$S(q, d) \= \\sum\_{i=1}^{|q|} \\max\_{j=1}^{|d|} (E\_{q\_i} \\cdot E\_{d\_j})$$  
Here, $E\_{q\_i}$ is the embedding of the $i$-th token of the text query, and $E\_{d\_j}$ is the embedding of the $j$-th visual patch. This formula dictates that for every word in the query, the model searches for the single most similar patch in the image, and the total score is the sum of these maximum similarities.

### **2.2 Algorithmic Transformation: Attention-to-Geometry**

The user's insight—to use ColPali for indexing and matching to avoid alignment problems—can be operationalized into a rigorous segmentation algorithm. Since the MaxSim operator explicitly links text tokens to image patches, the internal state of the model during this calculation contains the localization data required to build the HTR dataset. By treating a single line of the transcription as the "query" and the full page as the "document," we can extract the **Attention Map** (or Similarity Map) to spatially locate the handwriting.5  
The process of generating the dataset follows a multi-stage pipeline:

1. **Indexing (Forward Pass):** The full page of the Irish manuscript is passed through the ColPali vision encoder. This results in a tensor of shape $$, representing the 1024 patches, each with a 128-dimensional embedding.  
2. **Querying:** A specific line from the transcription (e.g., *"Tá sé páirteach..."*) is tokenized and embedded by the text encoder.  
3. **Similarity Matrix Computation:** A dot product is computed between every text token embedding and every image patch embedding. This yields a matrix of shape $\[N\_{text}, 1024\]$.  
4. **Heatmap Aggregation:** To visualize where the whole line is located, one aggregates this matrix across the text dimension. A common approach is to sum the similarity scores for each patch, resulting in a $$ vector. This vector is reshaped back into a $32 \\times 32$ grid.7  
5. **Upscaling and Thresholding:** The $32 \\times 32$ grid is low-resolution. To derive a usable bounding box:  
   * The grid is bi-linearly interpolated up to the original image resolution (e.g., $2000 \\times 3000$).  
   * A thresholding algorithm (such as **Otsu’s Binarization**) is applied to the heatmap to separate the "active" regions (the text) from the background.9  
   * Contour detection algorithms (like those in OpenCV) identify the bounding rectangle of the largest connected component.11

This algorithm effectively converts the "soft" attention of the VLM into "hard" coordinates for cropping.

### **2.3 Resolving Alignment Challenges in Bilingual Text**

Irish manuscripts often contain mixed scripts or bilingual marginalia. A traditional OCR engine might struggle to differentiate between the main Irish text and English annotations, or might fail to recognize the *Cló Gaelach* entirely. ColPali offers a distinct advantage here: **Semantic Grounding**.  
Because ColPali utilizes a Language Model (Gemma-2B/PaliGemma), it understands the semantic content of the query. If the query is an Irish sentence, the model will attend to the visual features that correspond to those specific words, even if the handwriting is stylized. This is distinct from layout analysis models (like YOLO trained on generic documents) which only look for "text-like" blobs. ColPali aligns the *meaning* of the text to the *pixels*, making it robust against layout noise or interlineations common in handwritten datasets.12  
However, the analysis indicates a critical limitation: **Granularity**. The $32 \\times 32$ patch grid implies that each patch covers a significant area (roughly $60 \\times 60$ pixels on a standard scan). While this is sufficient for identifying the general region of a line, it is not pixel-perfect. The bounding boxes generated via this weakly-supervised method will be "loose." For finetuning Qwen2-VL, this is actually acceptable, as VLMs are generally robust to some background noise around the text, provided the text itself is fully contained.14

## **3\. Deep Dive: The Qwen2-VL Architecture and Unsloth Optimization**

With the dataset of image-text pairs generated via ColPali, the focus shifts to the recognition model. The user has specifically identified **Qwen2-VL** and the **Unsloth** framework. This choice is technically sound; Qwen2-VL represents the current state-of-the-art in open-weights VLMs, outperforming larger proprietary models in OCR benchmarks like DocVQA and OCRBench.15

### **3.1 Qwen2-VL: Naive Dynamic Resolution and M-ROPE**

The suitability of Qwen2-VL for HTR lies in its handling of visual inputs. Traditional VLMs (like the original LLaVA) resize all images to a fixed square (e.g., $336 \\times 336$). For handwriting, which often consists of long, narrow lines or vertically oriented marginalia, this resizing introduces disastrous distortion or downsampling artifacts that obliterate the fine details of the stroke.  
Qwen2-VL introduces **Naive Dynamic Resolution**. It does not enforce a fixed input size. Instead, it processes the image at its native resolution (constrained by a min\_pixels and max\_pixels hyperparameter range). The image is divided into patches of $14 \\times 14$. A line of handwriting that is $1000 \\times 50$ pixels will be tokenized into a sequence of patches that preserves this aspect ratio.17  
To manage this variable sequence length, Qwen2-VL employs **M-ROPE (Multimodal Rotary Positional Embedding)**. Standard ROPE encodes position in a 1D sequence. M-ROPE decomposes the positional embedding into three components: temporal (for video), height, and width. This allows the LLM to understand the 2D spatial relationships of the visual tokens regardless of the grid shape. This is critical for HTR, where the model must track the horizontal progression of cursive script across the image.18

### **3.2 Unsloth: The Mathematics of Efficiency**

Training a VLM like Qwen2-VL (even the 2B version) can be VRAM-intensive due to the long sequence lengths generated by high-resolution images. **Unsloth** provides the necessary optimization infrastructure to make this feasible on consumer-grade or mid-tier hardware.17  
Unsloth optimizes the fine-tuning process not through quantization (though it supports it) but through the manual derivation of backpropagation gradients. In standard PyTorch, the autograd engine constructs a graph that stores intermediate activations for every operation. Unsloth replaces standard Transformer modules (like MLP and Self-Attention) with custom implementations where the backward pass is mathematically derived and implemented in **OpenAI Triton** kernels.19  
**Key Optimizations for Qwen2-VL:**

* **Gradient Checkpointing:** Unsloth manages activation recomputation more efficiently, reducing VRAM usage by up to 60%. This allows for larger batch sizes or higher resolution inputs (higher max\_pixels), which is directly correlated with HTR accuracy.  
* **LoRA Integration:** Unsloth natively integrates Low-Rank Adaptation (LoRA). For HTR, it is recommended to target not just the attention layers (q\_proj, v\_proj) but also the MLP layers (gate\_proj, up\_proj, down\_proj). This "all-linear" targeting has been shown to improve the model's ability to learn new syntactic patterns, such as the specific grammar of Irish.20  
* **Bfloat16 Support:** Unsloth leverages bfloat16 precision, which prevents the numerical instability often seen in mixed-precision training of VLMs, particularly with the large gradient norms associated with visual encoders.

### **3.3 Fine-Tuning Strategy for Irish HTR**

To finetune Qwen2-VL via Unsloth for this specific application, the following configuration is optimal:

* **Model:** unsloth/Qwen2-VL-2B-Instruct-bnb-4bit. The 2B model is selected to fit within the iOS memory budget. The 4-bit quantization (bnb-4bit) enables training on GPUs with as little as 12GB VRAM.20  
* **Vision Tower:** Typically frozen. However, if the Irish handwriting is stylistically divergent from the pre-training data (which is mostly web data and standard OCR datasets), one might consider applying LoRA adapters to the vision tower as well. Unsloth allows setting target\_modules to include vision encoder layers, though this increases VRAM usage.22  
* **Data Formatting:** The dataset must be converted to the conversational format:  
  JSON  
  {  
    "messages":  
      },  
      {  
        "role": "assistant",   
        "content": \[{"type": "text", "text": "Lá breá grianmhar a bhí ann."}\]  
      }  
    \]  
  }

  This format aligns the visual perception with the instruction-following capability of the model.20

## **4\. Architectural Divergence: ml-fastvlm vs. MLX**

A central component of the user's query is the investigation of Apple's ml-fastvlm repository. The analysis reveals a critical architectural schism that impacts the deployment strategy.

### **4.1 Deconstructing ml-fastvlm and FastViT**

ml-fastvlm is the official implementation of the **FastVLM** paper (CVPR 2025). Its primary goal is to solve the latency bottleneck of Vision Transformers on edge devices. Standard ViTs (like the SigLIP encoder in Qwen2-VL) use global self-attention, which scales quadratically with the number of tokens ($O(N^2)$). On mobile chips, this is computationally expensive.23  
FastVLM replaces the standard Transformer vision encoder with **FastViT-HD**. FastViT is a hybrid architecture that uses **structural reparameterization**. During training, it uses complex blocks (RepMixer) that capture diverse features. During inference, these blocks collapse into a single $3 \\times 3$ convolution. This creates a model that is extremely fast on the Apple Neural Engine (ANE), which is optimized for convolutions.25  
The Incompatibility:  
The weights of the Qwen2-VL model (fine-tuned via Unsloth) correspond to a SigLIP-like Vision Transformer. The ml-fastvlm codebase expects a FastViT convolutional encoder. These are fundamentally different neural architectures. One cannot simply "export" the Unsloth Qwen2-VL weights into ml-fastvlm. To use ml-fastvlm, the user would need to:

1. Initialize a FastVLM architecture (FastViT encoder \+ Qwen2 LLM).  
2. Perform **Pre-training (Stage 1 & 2\)** to align the FastViT encoder with the LLM, requiring massive image-text datasets (e.g., LLaVA-665k).  
3. Perform **Supervised Fine-Tuning** on the Irish dataset.

This process is computationally expensive and redundant given the existence of Qwen2-VL. Therefore, ml-fastvlm is **not recommended** for this specific pipeline unless extreme latency optimization (sub-50ms) is the primary constraint over development time.25

### **4.2 The Solution: MLX and mlx-vlm**

**MLX** is Apple's array framework designed specifically for Apple Silicon (M-series and A-series chips). It provides a unified memory model, allowing the CPU and GPU to access the same data without copying, which is crucial for memory-heavy VLMs.27  
The **mlx-vlm** library (and the associated mlx-swift-examples) provides native support for the standard Qwen2-VL architecture. This includes the implementation of the specific ViT encoder, the M-ROPE positional embeddings, and the dynamic resolution preprocessing logic.28  
**Advantages of MLX for iOS Deployment:**

* **Architecture Parity:** It supports the exact model architecture trained by Unsloth.  
* **Conversion Pipeline:** There is a direct, supported path to convert Hugging Face weights (safetensors) to MLX format (weights.npz).  
* **Quantization:** MLX offers 4-bit and 8-bit quantization that is highly optimized for the A-series GPU. A 2B parameter Qwen2-VL model quantized to 4-bits requires approximately 1.2GB \- 1.5GB of RAM.30 This fits comfortably within the "wired memory" limits of modern iPhones (which typically have 6GB or 8GB of RAM), leaving sufficient headroom for the iOS operating system and the application's UI.

### **4.3 CoreML vs. MLX**

The user also inquired about CoreML. While coremltools is the standard for iOS ML, it struggles with the dynamism of Large Language Models and VLMs.

* **Static Graph Requirement:** CoreML traditionally prefers static computation graphs. Qwen2-VL's dynamic resolution (where the number of visual tokens changes per image) and the autoregressive nature of text generation are difficult to express efficiently in CoreML without padding to fixed sizes, which wastes computation.32  
* **ANE Limitations:** The Apple Neural Engine (ANE) lacks support for certain operations required by modern Transformers (like specific types of casting or complex attention masks), forcing fallback to the GPU or CPU. MLX, by contrast, is designed to execute dynamic graphs efficiently on the GPU/CPU immediately.27

**Verdict:** For LLMs and VLMs on iOS today, MLX is the superior choice over pure CoreML.

## **5\. Deployment Implementation Roadmap**

The following roadmap outlines the step-by-step execution of the project, integrating the missing details identified in the analysis.

### **Phase 1: Data Curation (Python/ColPali)**

1. **Ingest:** Load the scanned Irish manuscript pages and their corresponding transcriptions.  
2. **Index:** Use the colpali-engine to encode all page images into patch embeddings.  
3. **Localize:**  
   * Iterate through each line of the transcription.  
   * Compute the MaxSim attention map between the line text and the page image.  
   * Apply **Gaussian smoothing** to the raw attention map to reduce noise.  
   * Apply **Otsu's thresholding** to binarize the map.  
   * Extract the bounding box of the active region.  
   * *Refinement:* Expand the bounding box by 10-15% (padding) to ensure no ascenders/descenders are clipped.  
4. **Crop & Save:** Generate the training pairs: {"image": "crop\_001.jpg", "text": "Agus ansin dúirt sé..."}.

### **Phase 2: Fine-Tuning (Python/Unsloth)**

1. **Setup:** Initialize FastVisionModel from Unsloth with load\_in\_4bit=True.  
2. **Configure LoRA:**  
   * r (rank): 16 or 32\.  
   * target\_modules: \["q\_proj", "k\_proj", "v\_proj", "o\_proj", "gate\_proj", "up\_proj", "down\_proj"\].  
   * *Crucial:* Ensure use\_gradient\_checkpointing="unsloth" is enabled to save VRAM.  
3. **Train:** Run the SFTTrainer (Supervised Fine-Tuning Trainer) on the generated dataset. Monitor the validation loss on a held-out set of Irish handwriting to prevent overfitting to the specific scribal hand.20  
4. **Fuse:** Once training is complete, fuse the LoRA adapters back into the base model using model.save\_pretrained\_merged(...). This is essential because the mobile inference engine requires a single static model file, not a base+adapter configuration.20

### **Phase 3: Conversion (Python/MLX)**

1. **Install:** pip install mlx-vlm.  
2. **Convert:** Use the conversion script to transform the fused Hugging Face model to MLX format.  
   Bash  
   python \-m mlx\_vlm.convert \--hf-path./qwen2-vl-irish-fused \--quantize \--q-bits 4 \--mlx-path./qwen2-vl-irish-4bit

   This command performs the quantization (reducing weights to 4-bit integers) and saves the weights.npz and config.json.33

### **Phase 4: iOS Development (Swift)**

1. **Dependencies:** Add the mlx-swift and mlx-swift-examples packages to the Xcode project.  
2. **Model Loading:** Use the VLMModelFactory to load the model from the local bundle (or download it from Hugging Face).  
3. **Inference Logic:**  
   * Preprocess the camera input or selected image. *Note: Ensure the Swift preprocessor matches the min\_pixels / max\_pixels used during Unsloth training.*  
   * Pass the image and the prompt (e.g., "Transcribe this text") to the generate() function.  
   * Handle the output stream to display text in real-time.  
4. **Performance Tuning:** Monitoring the "Wired Memory" gauge in Xcode Instruments is vital. If memory pressure is too high, reduce the KV-cache quantization to 4-bit or limit the maximum sequence length (context window) since HTR tasks typically require short outputs.34

## **6\. Comparative Analysis: Model Specifications**

The following tables summarize the critical decision points in the architecture.  
**Table 1: Inference Engine Comparison for iOS VLMs**

| Feature | Apple ml-fastvlm | MLX (mlx-vlm) | CoreML |
| :---- | :---- | :---- | :---- |
| **Vision Encoder** | FastViT (Hybrid ConvNet) | SigLIP/ViT (Transformer) | Various (Static) |
| **Qwen2-VL Support** | **No** (Requires retraining) | **Yes** (Native) | **Partial** (Complex conversion) |
| **Dynamic Resolution** | Limited | **Full** (Naive Dynamic) | Difficult (Requires padding) |
| **Memory Efficiency** | High (ANE Optimized) | High (Unified Memory) | Moderate |
| **Dev Effort** | High (Research Code) | Low (Python-to-Swift) | Very High |
| **Best Use Case** | Ultra-low latency, fixed tasks | Generative AI, RAG, HTR | Classical CV, Classification |

**Table 2: Estimated Resource Footprint on iOS (iPhone 15 Pro)**

| Model Variant | Quantization | RAM Usage (Est.) | Inference Speed (Text) | Suitability |
| :---- | :---- | :---- | :---- | :---- |
| Qwen2-VL-2B | FP16 | \~4.5 GB | Slow | **Low** (OOM Risk) |
| Qwen2-VL-2B | 4-bit | **\~1.2 GB** | **\~40 tok/sec** | **High** (Production Ready) |
| Qwen2-VL-7B | 4-bit | \~4.0 GB | \~15 tok/sec | **Moderate** (Pro models only) |

**Table 3: Unsloth Training Metrics (Qwen2-VL-2B)**

| Metric | Standard Hugging Face | Unsloth | Improvement |
| :---- | :---- | :---- | :---- |
| VRAM Usage (2B) | \~14 GB | **\~6 GB** | \-58% |
| Training Speed | 1x | **1.8x \- 2x** | \+80% |
| Batch Size | Low | High | Stability |

## **7\. Future Directions and Bilingual Considerations**

While the primary goal is HTR, the bilingual nature of the data (Irish/English) presents opportunities for "Agentic HTR." Instead of simple transcription, the app could leverage the Qwen2-VL language capabilities to perform tasks like:

* **Translation:** "Transcribe and translate this Irish text to English."  
* **Summarization:** "Summarize the content of this handwritten note."  
* **Entity Extraction:** "List all names and dates found in this manuscript."

These capabilities are inherent to the VLM architecture and are preserved when deploying via MLX (unlike specialized OCR models which only output text). To ensure the model does not "forget" English while learning Irish handwriting, the training dataset should be a mix (e.g., 70% Irish crops, 30% English/Generic crops) to act as regularization.

## **8\. Conclusion**

The development of a bilingual HTR app for Irish on iOS is not only possible but feasible with high performance using the proposed pipeline. By rejecting the architectural rigidity of ml-fastvlm in favor of the **MLX** ecosystem, the developer gains access to the cutting-edge **Qwen2-VL** architecture. Simultaneously, the innovative application of **ColPali** as a weak supervisor solves the chronic lack of annotated data for the Irish language. This integration of retrieval-augmented data generation, efficiency-optimized training via **Unsloth**, and hardware-accelerated inference via **MLX** constitutes a robust, modern solution for mobile Document AI.

#### **Works cited**

1. ColPali: Efficient Document Retrieval with Vision Language Models \- arXiv, accessed December 15, 2025, [https://arxiv.org/html/2407.01449v5](https://arxiv.org/html/2407.01449v5)  
2. \[2407.01449\] ColPali: Efficient Document Retrieval with Vision Language Models \- arXiv, accessed December 15, 2025, [https://arxiv.org/abs/2407.01449](https://arxiv.org/abs/2407.01449)  
3. Advanced Retrieval with ColPali & Qdrant Vector Database, accessed December 15, 2025, [https://qdrant.tech/blog/qdrant-colpali/](https://qdrant.tech/blog/qdrant-colpali/)  
4. Scaling ColPali to billions of PDFs with Vespa, accessed December 15, 2025, [https://blog.vespa.ai/scaling-colpali-to-billions/](https://blog.vespa.ai/scaling-colpali-to-billions/)  
5. illuin-tech/colpali: The code used to train and run inference ... \- GitHub, accessed December 15, 2025, [https://github.com/illuin-tech/colpali](https://github.com/illuin-tech/colpali)  
6. Spatially-Grounded Document Retrieval via Patch-to-Region Relevance Propagation \- arXiv, accessed December 15, 2025, [https://arxiv.org/html/2512.02660v1](https://arxiv.org/html/2512.02660v1)  
7. ColPali: Efficient Document Retrieval with Vision Language Models \- arXiv, accessed December 15, 2025, [https://arxiv.org/html/2407.01449v2](https://arxiv.org/html/2407.01449v2)  
8. ColPali: Enhancing Financial Report Analysis with Multimodal RAG and Gemini, accessed December 15, 2025, [https://learnopencv.com/multimodal-rag-with-colpali/](https://learnopencv.com/multimodal-rag-with-colpali/)  
9. Bounding box extraction from attention maps. \- ResearchGate, accessed December 15, 2025, [https://www.researchgate.net/figure/Bounding-box-extraction-from-attention-maps\_fig2\_386577739](https://www.researchgate.net/figure/Bounding-box-extraction-from-attention-maps_fig2_386577739)  
10. Image Thresholding \- OpenCV Documentation, accessed December 15, 2025, [https://docs.opencv.org/4.x/d7/d4d/tutorial\_py\_thresholding.html](https://docs.opencv.org/4.x/d7/d4d/tutorial_py_thresholding.html)  
11. How to get the feature bounded by the detected box in object detection? \#6311 \- GitHub, accessed December 15, 2025, [https://github.com/ultralytics/ultralytics/issues/6311](https://github.com/ultralytics/ultralytics/issues/6311)  
12. Transforming Product Discovery and Interpretation Using Vision–Language Models \- MDPI, accessed December 15, 2025, [https://www.mdpi.com/0718-1876/20/3/191](https://www.mdpi.com/0718-1876/20/3/191)  
13. Introduction to OCR Free Vision RAG using Colpali For Complex Documents, accessed December 15, 2025, [https://techcommunity.microsoft.com/blog/azure-ai-foundry-blog/introduction-to-ocr-free-vision-rag-using-colpali-for-complex-documents/4276357](https://techcommunity.microsoft.com/blog/azure-ai-foundry-blog/introduction-to-ocr-free-vision-rag-using-colpali-for-complex-documents/4276357)  
14. Spatially-Grounded Document Retrieval via Patch-to-Region Relevance Propagation, accessed December 15, 2025, [https://www.researchgate.net/publication/398269244\_Spatially-Grounded\_Document\_Retrieval\_via\_Patch-to-Region\_Relevance\_Propagation](https://www.researchgate.net/publication/398269244_Spatially-Grounded_Document_Retrieval_via_Patch-to-Region_Relevance_Propagation)  
15. Qwen2-VL | OpenLM.ai, accessed December 15, 2025, [https://openlm.ai/qwen2-vl/](https://openlm.ai/qwen2-vl/)  
16. Qwen/Qwen2-VL-2B-Instruct \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/Qwen/Qwen2-VL-2B-Instruct](https://huggingface.co/Qwen/Qwen2-VL-2B-Instruct)  
17. unsloth/Qwen2-VL-2B-Instruct \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/unsloth/Qwen2-VL-2B-Instruct](https://huggingface.co/unsloth/Qwen2-VL-2B-Instruct)  
18. unsloth/Qwen2-VL-2B-Instruct-bnb-4bit \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/unsloth/Qwen2-VL-2B-Instruct-bnb-4bit](https://huggingface.co/unsloth/Qwen2-VL-2B-Instruct-bnb-4bit)  
19. Make LLM Fine-tuning 2x faster with Unsloth and TRL \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/blog/unsloth-trl](https://huggingface.co/blog/unsloth-trl)  
20. Qwen2 Vision Finetuning Unsloth \- Kaggle, accessed December 15, 2025, [https://www.kaggle.com/code/danielhanchen/qwen2-vision-finetuning-unsloth-kaggle](https://www.kaggle.com/code/danielhanchen/qwen2-vision-finetuning-unsloth-kaggle)  
21. Fine-tune Llama3 with function calling via MLX-LM | by Anchen \- Medium, accessed December 15, 2025, [https://medium.com/@anchen.li/fine-tune-llama3-with-function-calling-via-mlx-lm-5ebbee41558f](https://medium.com/@anchen.li/fine-tune-llama3-with-function-calling-via-mlx-lm-5ebbee41558f)  
22. Vision Fine-tuning | Unsloth Documentation, accessed December 15, 2025, [https://docs.unsloth.ai/basics/vision-fine-tuning](https://docs.unsloth.ai/basics/vision-fine-tuning)  
23. apple/ml-fastvlm: This repository contains the official ... \- GitHub, accessed December 15, 2025, [https://github.com/apple/ml-fastvlm](https://github.com/apple/ml-fastvlm)  
24. FastVLM: Efficient Vision Encoding for Vision Language Models : r/apple \- Reddit, accessed December 15, 2025, [https://www.reddit.com/r/apple/comments/1m7gb3j/fastvlm\_efficient\_vision\_encoding\_for\_vision/](https://www.reddit.com/r/apple/comments/1m7gb3j/fastvlm_efficient_vision_encoding_for_vision/)  
25. Fastvlm: Efficient Vision Encoding For Vision Language Models | PDF \- Scribd, accessed December 15, 2025, [https://www.scribd.com/document/863828552/2412-13303v2](https://www.scribd.com/document/863828552/2412-13303v2)  
26. FastVLM: Efficient Vision Encoding for Vision Language Models \- CVF Open Access, accessed December 15, 2025, [https://openaccess.thecvf.com/content/CVPR2025/papers/Vasu\_FastVLM\_Efficient\_Vision\_Encoding\_for\_Vision\_Language\_Models\_CVPR\_2025\_paper.pdf](https://openaccess.thecvf.com/content/CVPR2025/papers/Vasu_FastVLM_Efficient_Vision_Encoding_for_Vision_Language_Models_CVPR_2025_paper.pdf)  
27. MLX Swift: Run LLMs and VLMs in iOS Apps \- Reddit, accessed December 15, 2025, [https://www.reddit.com/r/swift/comments/1j4v70y/mlx\_swift\_run\_llms\_and\_vlms\_in\_ios\_apps/](https://www.reddit.com/r/swift/comments/1j4v70y/mlx_swift_run_llms_and_vlms_in_ios_apps/)  
28. Qwen2-VL Best Practice — swift 2.6.1 documentation \- Read the Docs, accessed December 15, 2025, [https://swift2x-en.readthedocs.io/en/latest/Multi-Modal/qwen2-vl-best-practice.html](https://swift2x-en.readthedocs.io/en/latest/Multi-Modal/qwen2-vl-best-practice.html)  
29. mlx-community/Qwen2-VL-2B-4bit \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/mlx-community/Qwen2-VL-2B-4bit](https://huggingface.co/mlx-community/Qwen2-VL-2B-4bit)  
30. README.md · riddhimanrana/fastvlm-0.5b-captions at main \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/riddhimanrana/fastvlm-0.5b-captions/blob/main/README.md](https://huggingface.co/riddhimanrana/fastvlm-0.5b-captions/blob/main/README.md)  
31. Speed Benchmark \- Qwen, accessed December 15, 2025, [https://qwen.readthedocs.io/en/v2.0/benchmark/speed\_benchmark.html](https://qwen.readthedocs.io/en/v2.0/benchmark/speed_benchmark.html)  
32. Load and Convert Model Workflow — Guide to Core ML Tools \- Apple, accessed December 15, 2025, [https://apple.github.io/coremltools/docs-guides/source/load-and-convert-model.html](https://apple.github.io/coremltools/docs-guides/source/load-and-convert-model.html)  
33. mlx-community/Jan-v2-VL-high-8bit-mlx \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/mlx-community/Jan-v2-VL-high-8bit-mlx](https://huggingface.co/mlx-community/Jan-v2-VL-high-8bit-mlx)  
34. llm-tool \- ml-explore/mlx-swift-examples \- GitHub, accessed December 15, 2025, [https://github.com/ml-explore/mlx-swift-examples/blob/main/Tools/llm-tool/README.md](https://github.com/ml-explore/mlx-swift-examples/blob/main/Tools/llm-tool/README.md)