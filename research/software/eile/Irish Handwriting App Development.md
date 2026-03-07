# **Operationalizing Irish Handwriting Recognition on Apple Silicon: An Exhaustive Architectural Analysis of MLX, Llama.cpp, and Transformers.js**

## **1\. Executive Summary**

The convergence of high-performance mobile silicon, specifically the Apple M2 architecture, with the advent of efficient Vision-Language Models (VLMs) creates a distinct inflection point for philological computing. This report provides a comprehensive, expert-level technical analysis of engineering a native iOS/iPadOS application dedicated to Irish language (*Gaeilge*) handwriting recognition (HTR). The proposed solution leverages the iPad Air M2 and Apple Pencil Pro, utilizing a hybrid inference stack comprising **mlx-swift**, **llama.cpp**, and **transformers.js** via WebGPU to overcome the historical computational barriers of on-device transcription.  
The analysis establishes that generic Optical Character Recognition (OCR) systems are fundamentally ill-suited for the nuances of Irish orthography—specifically the *Cló Gaelach* (Gaelic type) and the diacritical *punctum delens* used for lenition (*séimhiú*). To address this, the report proposes a domain-specific adaptation of state-of-the-art multimodal models, principally **Qwen3-VL** (and its stable predecessor Qwen2.5-VL) and **Gemma 3n**. These models, unlike their predecessors, possess the visual reasoning capabilities required to distinguish ambiguous glyphs (such as the distinctively similar ‘r’ and ‘s’ in Gaelic script) and the semantic understanding to perform "thinking" or Chain-of-Thought (CoT) error correction in real-time.  
Crucially, this document details the mechanisms for deploying these massive models within the strict thermal and memory envelopes of the iPad Air M2 (8GB Unified Memory). We delineate the precise role of **MLX** as the primary inference engine due to its direct mapping to the Metal Performance Shaders (MPS) graph, while positioning **llama.cpp** and **transformers.js** as strategic fallbacks for cross-platform compatibility and web-based interaction. Furthermore, the report explores the integration of the **Apple Pencil Pro’s** novel interaction paradigms—squeeze detection and haptic feedback—to construct a "Human-in-the-Loop" transcription workflow that dramatically accelerates the digitization of Ireland's archival heritage, specifically referencing the **Dúchas.ie** and **An Gaodhal** datasets as the foundation for fine-tuning.

## ---

**2\. Hardware Architecture: The Apple Silicon Substrate**

The feasibility of running Multi-Modal Large Language Models (VLMs) on a slate form factor is strictly governed by the underlying hardware architecture. The iPad Air M2 represents a shift from mobile-first to desktop-class architecture in a constrained thermal envelope. Understanding the nuances of this silicon is prerequisite to successful application deployment.

### **2.1 The M2 Unified Memory Architecture (UMA) and VLM Inference**

In traditional computing architectures (x86/CUDA), the CPU and GPU maintain discrete memory pools. This necessitates the continuous copying of data across the PCI-Express bus, introducing significant latency and power consumption—prohibitive factors for mobile real-time inference. The M2 chip utilized in the iPad Air eliminates this bottleneck through its **Unified Memory Architecture (UMA)**.

* **Memory Bandwidth and Access:** The M2 features a 128-bit wide memory interface providing approximately **100 GB/s** of bandwidth.1 For VLM inference, where high-resolution image tensors (from the visual encoder) must be processed alongside massive weight matrices (from the language decoder), this bandwidth is the enabling factor. It allows the GPU and Neural Engine (ANE) to access the same data without duplication.  
* **The 8GB Physical Constraint:** The standard iPad Air M2 configuration includes 8GB of RAM. This is the single most critical constraint for the proposed application.  
  * **OS Reservation:** iOS/iPadOS aggressively manages memory. The kernel, window server, and display buffers typically reserve 2–3GB.  
  * **Jetsam Limits:** The operating system employs a "jetsam" mechanism that terminates background processes or memory-hogging foreground apps to preserve system stability. The effective "safe" budget for the application is approximately **4.5 GB to 5 GB**.2  
  * **Model Implications:** A standard 3-billion parameter model in FP16 (16-bit floating point) precision requires roughly 6GB of VRAM for weights alone, plus additional overhead for the KV cache (context window) and activation buffers. This exceeds the safe budget.  
  * **Quantization Necessity:** To fit a capable VLM like **Qwen2.5-VL-3B** or **Gemma 3n E4B** onto the device, **4-bit quantization (Q4)** is mandatory. A Q4 version of a 3B model occupies approximately **2.68 GB**.3 This fits comfortably within the resident set size, leaving approximately 2GB for the visual encoder, the KV cache (which grows linearly with text length), and the application's UI assets.

### **2.2 Apple Pencil Pro: Interaction Physics and Sensor Fusion**

The Apple Pencil Pro introduces specific hardware capabilities—a strain gauge for "squeeze" detection, a gyroscope for "barrel roll," and a haptic engine—that fundamentally alter the user interface design for handwriting recognition tasks.

* **Squeeze-to-Infer (Low Latency Trigger):** In a transcription workflow, latency breaks the "flow state" of the archivist. Traditional UI requires lifting the stylus to tap a button. By implementing the UIPencilInteractionDelegate protocol, the application can trigger the inference pass immediately upon the detection of a squeeze gesture.4 This hardware interrupt allows for a seamless "write-squeeze-verify" loop.  
* **Haptic Feedback for Confidence Signaling:** The Haptic Engine provides programmable tactile feedback. This is particularly relevant for Irish manuscripts where ink fading or damage is common. If the VLM's confidence score (log-probability) for a recognized segment falls below a set threshold (e.g., \< 75%), the app can trigger a distinct haptic pulse using UIImpactFeedbackGenerator.6 This alerts the user to visually verify the transcription without needing to constantly check the screen, mimicking the tactile feedback of a physical tool encountering resistance.  
* **Barrel Roll for Parameter Tuning:** The barrel roll gesture allows for continuous analog input. In the context of HTR, this can be mapped to the "temperature" of the model or the "brush size" of the eraser. For example, rotating the pencil could dynamically adjust the contrast threshold of the input image preprocessing, allowing the user to "tune in" faint text physically.7

## ---

**3\. The Inference Ecosystem: A Comparative Technical Analysis**

To deploy high-capability models on the iPad, we must select an inference engine that balances performance, memory efficiency, and developer ergonomics. The research highlights three primary frameworks: **MLX**, **Llama.cpp**, and **Transformers.js**.

### **3.1 MLX and MLX-Swift: The Native Metal Sovereign**

**MLX** is an array framework designed by Apple specifically for Apple Silicon.8 Unlike cross-platform tools that treat Apple Silicon as a generic ARM target, MLX is architected around the unified memory model and the Metal API.

* **Lazy Evaluation:** MLX utilizes lazy evaluation, meaning computations are only executed when the results are materialized. In a VLM pipeline, where the visual encoder runs once but the language decoder runs iteratively (token by token), this allows for highly efficient memory management. Graphs are compiled dynamically, optimizing the execution plan for the specific operations required.9  
* **MLX-Swift Integration:** The mlx-swift package provides a Swift API that bridges directly to the C++ core. This eliminates the overhead associated with Python-Swift bridging or Objective-C wrappers. Crucially, mlx-swift exposes the MLXArray object, which can be zero-copy shared with Metal compute shaders. This allows the application to perform custom image preprocessing (e.g., binarization of manuscript pages) using native Metal shaders and pass the result directly to the VLM without CPU round-tripping.  
* **VLM Specific Support:** The mlx-swift-examples repository contains a dedicated library, MLXVLM.10 This library implements the complex architectures of modern VLMs, separating the Vision Tower (typically a CLIP or SigLIP variant) from the Language Model. The research indicates that support for **Qwen2.5-VL** is already present in the python counterparts and is being actively ported to Swift.11  
* **Advantage:** MLX offers the highest throughput (tokens per second) and best energy efficiency on M-series chips because it avoids the overhead of generic compute layers. It supports "Thinking" models and complex architectures like MoE (Mixture of Experts) natively.12

### **3.2 Llama.cpp: The Ubiquitous Fallback**

**Llama.cpp** is a C++ inference engine focused on pure CPU/GPU inference with no dependencies.13 Its primary strength lies in its **GGUF** file format and extreme quantization capabilities.

* **Quantization Leadership:** Llama.cpp supports advanced quantization types such as **IQ3\_XS** (approx. 3 bits per weight) and **IQ2\_XXS**.14 These formats use importance matrices (imatrix) to preserve model accuracy even at extreme compression. For an older iPad or for users multitasking heavily, running a Qwen model in IQ3\_XS format via llama.cpp might be the only way to prevent OOM errors, reducing a 3B model to under 1.5GB of RAM.  
* **Integration Complexity:** While llama.cpp can be compiled as a library for iOS, VLM support (Vision) has historically been segregated into "examples" or forks (e.g., llama-server with projection layers) rather than the core library. Snippets indicate that Qwen2.5-VL support required specific forks initially 14, though it is merging into the main branch.  
* **Performance Profile:** Benchmarks indicate that for single-batch inference (one user), llama.cpp provides excellent Time To First Token (TTFT) latency, often beating server-oriented frameworks like vLLM which optimize for throughput over latency.15 This makes it a highly responsive backend for a real-time handwriting app.

### **3.3 Transformers.js and WebGPU: The Browser-Based Companion**

**Transformers.js** enables running models directly in the browser using the ONNX Runtime and **WebGPU**.17

* **WebGPU on iOS 18:** With the release of iOS 18, Safari (and WKWebView) supports WebGPU. This allows JavaScript to access the M2 GPU directly via WGSL (WebGPU Shading Language).  
* **The "Share" Utility:** The primary use case for transformers.js in this architecture is interoperability. A user can export their transcribed notes and the model configuration from the iPad app. Because the model weights (quantized ONNX) can be loaded by transformers.js, the user can then open a web link on a Windows PC or Android device and continue verifying the transcription, leveraging the local GPU of that device.  
* **Limitations:** Snippets note that while embedding models run 40-75x faster on WebGPU, full VLM support (especially for complex architectures like Gemma 3n's MatFormer) is still maturing in the browser environment, currently often requiring Node.js for full feature parity.19

**Synthesis:** For the "Peannaire" (Scribe) app, **MLX-Swift** is the optimal *primary* engine due to its native performance and memory handling. **Llama.cpp** serves as a robust "Low Power" or "High Compatibility" mode. **Transformers.js** is the enabling technology for a "Review Anywhere" web companion.

## ---

**4\. Foundation Model Analysis: The Cognitive Layer**

The capability of the application relies on the underlying model's ability to not just recognize characters (OCR), but to *understand* the visual context of handwriting.

### **4.1 Qwen2.5-VL and Qwen3-VL: The Visual Reasoners**

The **Qwen-VL** series (specifically Qwen2.5-VL and the referenced future Qwen3-VL) represents the current state-of-the-art for open-weights visual processing.20

* **NaViT (Native Resolution Vision Transformer):** Most VLMs resize input images to a fixed square (e.g., 336x336 pixels). This is catastrophic for handwriting, where a line of text is a long, thin horizontal strip. Resizing it to a square distorts the aspect ratio and crushes character details. Qwen2.5-VL utilizes a NaViT-like approach, processing images at their **native resolution** by dynamically creating patches.22 This preserves the stroke fidelity essential for distinguishing Irish characters.  
* **"Thinking" Process:** Snippets mention "Qwen3-VL-Thinking" models.23 These models employ test-time compute to generate a Chain-of-Thought (CoT) before outputting the final answer. In the context of Irish HTR, this allows the model to "reason" through ambiguity.  
  * *Example:* The model sees a glyph that could be 'r' or 's'.  
  * *CoT:* "The preceding article is 'an'. The noun is feminine. Therefore, lenition is likely. The stroke has a descender typical of 's' in this hand. I will transcribe as 's'."  
* **Agentic Capabilities:** Qwen2.5-VL is trained for tool use and acting as a visual agent.22 This allows the app to go beyond transcription. The user could circle a paragraph and write "Summarize this in English," and the model can perform the task using its internal reasoning capabilities.

### **4.2 Gemma 3n: The Mobile-Optimized Multimodal**

**Gemma 3n** (Nano) is Google's mobile-first model family.24

* **MatFormer Architecture:** This is a critical architectural innovation for the iPad app. MatFormer (Matryoshka Transformer) allows a single model to operate at different sizes (e.g., E2B and E4B) by "slicing" the weights.24  
  * *Implementation:* The app can run the **E2B** slice continuously for real-time, low-latency preview as the user writes. When the user pauses or triggers the "Squeeze" gesture for final commit, the app momentarily engages the full **E4B** layers for maximum accuracy. This **Elastic Inference** optimizes battery life without sacrificing peak performance.  
* **Multimodal Inputs:** Gemma 3n supports audio natively. This enables a multimodal correction workflow. The user can point to a word and *say* "This is actually 'Béal', not 'Bael'," and the model uses both the visual context of the handwriting and the audio input to correct the transcription.26

### **4.3 FunctionGemma: The Semantic Action Layer**

**FunctionGemma** 27 is specialized for converting natural language into structured API calls.

* **Role:** It acts as the bridge between the transcribed text and the iOS ecosystem. If the user writes a diary entry *"Visit the archives in Galway next Tuesday,"* FunctionGemma parses this text and outputs a structured JSON object:  
  JSON  
  {  
    "tool": "Calendar",  
    "action": "createEvent",  
    "parameters": {  
      "title": "Visit Archives",  
      "location": "Galway",  
      "date": "next Tuesday"  
    }  
  }

  The Swift app then executes this using EventKit.

## ---

**5\. The Irish Language Domain: Linguistic & Data Engineering**

Developing a robust Irish HTR system requires solving specific linguistic challenges that generic models fail to address.

### **5.1 The Challenge of Cló Gaelach and Orthography**

Historical Irish manuscripts (pre-1960s) predominantly use the **Cló Gaelach** (Gaelic Type). This script introduces unique OCR challenges:

* **Lenition (Séimhiú):** In modern Irish (Roman type), lenition is marked by an 'h' (e.g., *mháthair*). In Cló Gaelach, it is marked by a **Punctum Delens** (a dot) over the consonant (e.g., *ṁáṫair*). Generic models often mistake this dot for noise or a speck of dust.28  
* **The Tironian Et:** The symbol ⁊ is used for "agus" (and). Standard models often misread this as a '7'.  
* **Glyph Confusion:** The Gaelic 'r' (ꞃ) is visually similar to 'p' or 'x'. The 's' (ſ) resembles 'f' without the crossbar.  
* **Solution:** Zero-shot performance on this script is poor. We must fine-tune the models specifically to recognize these features.

### **5.2 Dataset Strategy for Fine-Tuning**

To train Qwen or Gemma to read Irish effectively, we must curate a high-quality instruction-tuning dataset.  
**1\. Dúchas.ie (The Schools' Collection)**

* **Source:** \~500,000 pages of folklore collected in the 1930s.29  
* **Ground Truth:** The *Meitheal Dúchas* project has crowdsourced transcriptions for a significant portion of this archive.30  
* **Pipeline:** We need to construct a dataset of (Image\_Region, Text\_Transcription) pairs. This involves:  
  1. Scraping the Dúchas API for pages with validated transcriptions.  
  2. Using a layout analysis model (like YOLOv8 or specific MLX-based layout parsers) to segment the page into lines.  
  3. Matching the lines to the XML transcription data.

**2\. Transkribus & An Gaodhal**

* **Source:** The *An Gaodhal* project created specific OCR models for bilingual Irish/English newspapers.31  
* **Methodology:** This project successfully utilized "masking" to separate English and Irish text for training language-specific models.  
* **Data Export:** The snippet confirms that *An Gaodhal* data is available in **ALTO XML** format.32 ALTO (Analyzed Layout and Text Object) contains precise coordinate data for every word on the page.  
* **Conversion:** A Python script is required to parse the ALTO XML, crop the word/line images from the high-res rasters, and format them into a JSONL file compatible with mlx-vlm fine-tuning (e.g., {"messages":}, {"role": "assistant", "content": "..."}\]}).

**3\. Logainm.ie (Placenames Database)**

* **Source:** The official database of Irish placenames.33  
* **RAG Implementation:** Handwriting often contains obscure local placenames (townlands). We can export the Logainm dataset 34 and build a local vector database on the iPad (using a Swift library like USearch or CoreData with embedding support). When the VLM outputs a low-confidence token sequence that looks like a placename, the app queries this local database to "snap" the transcription to the nearest valid official Irish placename.

## ---

**6\. "Peannaire" Application Architecture: Implementation Roadmap**

We define the reference architecture for the application, hereby named "Peannaire" (Scribe).

### **6.1 View Layer: PencilKit and Canvas Management**

The UI is built on PKCanvasView to provide a native writing experience.

* **Stroke Capture Strategy:** We do not stream the entire screen to the VLM continuously, as this is computationally wasteful. Instead, we implement a **Stroke-Based Trigger**.  
* **Implementation:**  
  Swift  
  // Swift Pseudo-code for intelligent capturing  
  func canvasViewDrawingDidChange(\_ canvasView: PKCanvasView) {  
      // Debounce timer: Wait 1 second after last stroke  
      debounceTimer?.invalidate()  
      debounceTimer \= Timer.scheduledTimer(withTimeInterval: 1.0, repeats: false) { \_ in  
          self.captureAndInfer(canvasView.drawing)  
      }  
  }

* **Dark Mode Inversion:** VLMs are typically trained on black text on white backgrounds. If the iPad is in Dark Mode, the PKDrawing image export must be inverted (white strokes on black background \-\> black strokes on white) before inference to ensure accuracy.

### **6.2 Logic Layer: MLX-Swift VLM Integration**

The core logic manages the MLX model container.

* Model Loading & Quantization:  
  To respect the 8GB memory limit, we load the 4-bit quantized Qwen model.  
  Swift  
  import MLXVLM

  // Configuration pointing to the Hugging Face repo  
  let config \= ModelConfiguration(  
      id: "mlx-community/Qwen2.5-VL-3B-Instruct-4bit"   
  )

  // Load the model container (weights downloaded to sandbox)  
  let modelContainer \= try await VLMModelFactory.shared.loadContainer(configuration: config)

* Inference Loop:  
  When the "Squeeze" interaction is detected:  
  1. **Rasterize:** canvasView.drawing.image(from: rect, scale: 2.0) generates a UIImage.  
  2. **Preprocess:** Resize to native resolution (multiples of 28px for Qwen) using CoreImage.  
  3. **Generate:**  
     Swift  
     let input \= UserInput(images: \[processedImage\], prompt: "Transcribe the Irish text in this image.")  
     let result \= try await modelContainer.perform { context in  
         let input \= try await context.processor.prepare(input: input)  
         return try MLXLMCommon.generate(input: input, parameters: params, context: context)  
     }

  4. **Haptic Feedback:** Parse result confidence. If confident, light tick (.success). If uncertain, double pulse (.warning).6

### **6.3 Data Layer: Core Data & RAG**

* **Local Storage:** Transcriptions are stored in Core Data alongside the PKDrawing binary data.  
* **RAG Pipeline:**  
  * **Embedding Model:** We run a small embedding model (e.g., all-MiniLM-L6-v2 quantized) via mlx-swift or transformers.js to embed the transcribed text.  
  * **Vector Search:** This allows the user to search their handwritten notes semantically (e.g., searching for "Wedding" finds notes about "Pósadh").

## ---

**7\. Deep Research Insights & Future Implications**

### **7.1 The "Model Collapse" Risk in Low-Resource Languages**

A critical insight derived from the analysis of Irish datasets is the risk of **Model Collapse** or **Hallucination Loop**. Generic models trained on the web (Common Crawl) have seen very little *Cló Gaelach*. Without fine-tuning, models often hallucinate English words that visually resemble the Irish script.

* **Mitigation:** The "Thinking" capability of Qwen3 is not just a feature; it is a safety mechanism. By prompting the model to explicitly reason about the visual strokes (*"Does this glyph have the ascender of a 'b' or the descender of a 'p'?"*) before committing to a token, we force the model to ground its output in visual evidence rather than language model probability.

### **7.2 Hybrid Deployment Strategy**

The concurrent maturation of mlx-swift (Native) and transformers.js (Web) suggests a hybrid deployment future.

* **Scenario:** A researcher uses the native iPad app (MLX) for heavy-duty transcription of archives in the field (offline). They then generate a "Web Review Link."  
* **Mechanism:** This link opens a page that loads the *same* quantized weights (converted to ONNX) using transformers.js and WebGPU. This allows a second user (e.g., a student) to review and correct the transcription on a non-Apple device (Windows/Android) without needing the native app. This interoperability bridges the gap between the high-performance Apple ecosystem and the wider research community.

### **7.3 Cultural Data Sovereignty**

Running this stack locally on Apple Silicon is a matter of **Data Sovereignty**.

* **Context:** Indigenous communities are increasingly wary of uploading cultural heritage data to centralized cloud APIs (OpenAI/Google) where it might be used to train proprietary models without consent.  
* **Impact:** A fully offline "Peannaire" app empowers archivists in the *Gaeltacht* to digitize sensitive folklore records with the guarantee that the data never leaves the physical device. This aligns with the ethical frameworks of modern digital humanities.

## ---

**8\. Conclusion**

The combination of the **iPad Air M2's Unified Memory Architecture**, the interactive precision of the **Apple Pencil Pro**, and the efficiency of the **MLX** framework creates a uniquely capable platform for revitalizing Irish language resources. By moving beyond simple OCR to **Vision-Language Reasoning**, and by grounding these models in the rich, specific datasets of *Dúchas* and *An Gaodhal*, developers can build tools that do not merely transcribe text, but understand the cultural and linguistic context of the written word. This architecture represents the cutting edge of what is technically possible in 2025, transforming the iPad from a consumption device into a primary instrument of cultural preservation.

## **9\. Appendix: Comparative Specifications**

| Feature | MLX-Swift | Llama.cpp | Transformers.js (WebGPU) |
| :---- | :---- | :---- | :---- |
| **Primary Use Case** | Native iOS/macOS App | Low-End / Compatibility | Web / Cross-Platform |
| **VLM Support** | High (Qwen, Gemma, LLaVA) | Moderate (Experimental) | Emerging (Node.js first) |
| **Quantization** | 4-bit, 8-bit | 1-bit to 8-bit (GGUF) | 8-bit (ONNX) |
| **Performance (M2)** | **Highest (Metal Optimized)** | High (CPU/Metal Hybrid) | High (Shader based) |
| **Memory Efficiency** | **Excellent (Unified Memory)** | **Best (Low-bit GGUF)** | Moderate (Browser Limits) |
| **Implementation** | Swift / C++ Bridge | C++ / C Bridge | JavaScript / Wasm |

| Model | Parameters | Quantization | Est. Memory (M2) | Irish Capability |
| :---- | :---- | :---- | :---- | :---- |
| **Qwen2.5-VL-3B** | 3 Billion | 4-bit (Q4) | \~2.7 GB | High (Visual Reasoning) |
| **Gemma 3n E4B** | 4 Billion | 4-bit (Q4) | \~3.0 GB | High (Multimodal/Mobile) |
| **FunctionGemma** | Varies | 4-bit (Q4) | \~2.0 GB | Specialized (Agentic) |

#### **Works cited**

1. Performance of llama.cpp on Apple Silicon M-series \#4167 \- GitHub, accessed December 24, 2025, [https://github.com/ggml-org/llama.cpp/discussions/4167](https://github.com/ggml-org/llama.cpp/discussions/4167)  
2. Building Offline RAG on iOS: How to Run Gemma 3N Locally | by Greg Sommerville | Google Cloud \- Community | Dec, 2025 | Medium, accessed December 24, 2025, [https://medium.com/google-cloud/building-offline-rag-on-ios-how-to-run-gemma-3n-locally-ffdfda6f7217](https://medium.com/google-cloud/building-offline-rag-on-ios-how-to-run-gemma-3n-locally-ffdfda6f7217)  
3. Qwen2.5-3B: Specifications and GPU VRAM Requirements \- ApX Machine Learning, accessed December 24, 2025, [https://apxml.com/models/qwen2-5-3b](https://apxml.com/models/qwen2-5-3b)  
4. Handling double taps from Apple Pencil | Apple Developer Documentation, accessed December 24, 2025, [https://developer.apple.com/documentation/applepencil/handling-double-taps-from-apple-pencil](https://developer.apple.com/documentation/applepencil/handling-double-taps-from-apple-pencil)  
5. Apple Pencil interactions | Apple Developer Documentation, accessed December 24, 2025, [https://developer.apple.com/documentation/uikit/apple-pencil-interactions](https://developer.apple.com/documentation/uikit/apple-pencil-interactions)  
6. Playing haptic feedback in your app | Apple Developer Documentation, accessed December 24, 2025, [https://developer.apple.com/documentation/applepencil/playing-haptic-feedback-in-your-app](https://developer.apple.com/documentation/applepencil/playing-haptic-feedback-in-your-app)  
7. Apple Pencil and Scribble | Apple Developer Documentation, accessed December 24, 2025, [https://developer.apple.com/design/human-interface-guidelines/apple-pencil-and-scribble](https://developer.apple.com/design/human-interface-guidelines/apple-pencil-and-scribble)  
8. ml-explore/mlx-swift: Swift API for MLX \- GitHub, accessed December 24, 2025, [https://github.com/ml-explore/mlx-swift](https://github.com/ml-explore/mlx-swift)  
9. Unleashing Vision AI on Apple Silicon: A Practical Guide to MLX-VLM \- Level Up Coding, accessed December 24, 2025, [https://levelup.gitconnected.com/unleashing-vision-ai-on-apple-silicon-a-practical-guide-to-mlx-vlm-a6fecabadf39](https://levelup.gitconnected.com/unleashing-vision-ai-on-apple-silicon-a-practical-guide-to-mlx-vlm-a6fecabadf39)  
10. ml-explore/mlx-swift-examples \- GitHub, accessed December 24, 2025, [https://github.com/ml-explore/mlx-swift-examples](https://github.com/ml-explore/mlx-swift-examples)  
11. Exploring MLX Swift: Porting Qwen 3VL 4B from Python to Swift | Rudrank Riyam, accessed December 24, 2025, [https://rudrank.com/exploring-mlx-swift-porting-python-model-to-swift](https://rudrank.com/exploring-mlx-swift-porting-python-model-to-swift)  
12. Releases · ml-explore/mlx-swift-examples \- GitHub, accessed December 24, 2025, [https://github.com/ml-explore/mlx-swift-examples/releases](https://github.com/ml-explore/mlx-swift-examples/releases)  
13. ggml-org/llama.cpp: LLM inference in C/C++ \- GitHub, accessed December 24, 2025, [https://github.com/ggml-org/llama.cpp](https://github.com/ggml-org/llama.cpp)  
14. Mungert/Qwen2.5-VL-3B-Instruct-GGUF \- Hugging Face, accessed December 24, 2025, [https://huggingface.co/Mungert/Qwen2.5-VL-3B-Instruct-GGUF](https://huggingface.co/Mungert/Qwen2.5-VL-3B-Instruct-GGUF)  
15. vLLM or llama.cpp: Choosing the right LLM inference engine for your use case, accessed December 24, 2025, [https://developers.redhat.com/articles/2025/09/30/vllm-or-llamacpp-choosing-right-llm-inference-engine-your-use-case](https://developers.redhat.com/articles/2025/09/30/vllm-or-llamacpp-choosing-right-llm-inference-engine-your-use-case)  
16. llama.cpp vs. vllm performance comparison : r/LocalLLaMA \- Reddit, accessed December 24, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1ml4cz0/llamacpp\_vs\_vllm\_performance\_comparison/](https://www.reddit.com/r/LocalLLaMA/comments/1ml4cz0/llamacpp_vs_vllm_performance_comparison/)  
17. WebGPU is now supported in major browsers | Blog \- web.dev, accessed December 24, 2025, [https://web.dev/blog/webgpu-supported-major-browsers](https://web.dev/blog/webgpu-supported-major-browsers)  
18. Transformers.js \- Hugging Face, accessed December 24, 2025, [https://huggingface.co/docs/transformers.js/index](https://huggingface.co/docs/transformers.js/index)  
19. onnx-community/gemma-3n-E2B-it-ONNX \- Hugging Face, accessed December 24, 2025, [https://huggingface.co/onnx-community/gemma-3n-E2B-it-ONNX](https://huggingface.co/onnx-community/gemma-3n-E2B-it-ONNX)  
20. Qwen \- Wikipedia, accessed December 24, 2025, [https://en.wikipedia.org/wiki/Qwen](https://en.wikipedia.org/wiki/Qwen)  
21. Qwen/Qwen3-VL-8B-Instruct \- Hugging Face, accessed December 24, 2025, [https://huggingface.co/Qwen/Qwen3-VL-8B-Instruct](https://huggingface.co/Qwen/Qwen3-VL-8B-Instruct)  
22. Qwen/Qwen2.5-VL-3B-Instruct \- Hugging Face, accessed December 24, 2025, [https://huggingface.co/Qwen/Qwen2.5-VL-3B-Instruct](https://huggingface.co/Qwen/Qwen2.5-VL-3B-Instruct)  
23. Qwen/Qwen3-VL-8B-Thinking \- Demo \- DeepInfra, accessed December 24, 2025, [https://deepinfra.com/Qwen/Qwen3-VL-8B-Thinking](https://deepinfra.com/Qwen/Qwen3-VL-8B-Thinking)  
24. Gemma 3n model overview \- Google AI for Developers, accessed December 24, 2025, [https://ai.google.dev/gemma/docs/gemma-3n](https://ai.google.dev/gemma/docs/gemma-3n)  
25. Introducing Gemma 3n: The developer guide \- Google Developers Blog, accessed December 24, 2025, [https://developers.googleblog.com/en/introducing-gemma-3n-developer-guide/](https://developers.googleblog.com/en/introducing-gemma-3n-developer-guide/)  
26. Gemma 3n – Vertex AI \- Google Cloud Console, accessed December 24, 2025, [https://console.cloud.google.com/vertex-ai/publishers/google/model-garden/gemma3n](https://console.cloud.google.com/vertex-ai/publishers/google/model-garden/gemma3n)  
27. Gemma releases | Google AI for Developers, accessed December 24, 2025, [https://ai.google.dev/gemma/docs/releases](https://ai.google.dev/gemma/docs/releases)  
28. Irish orthography \- Wikipedia, accessed December 24, 2025, [https://en.wikipedia.org/wiki/Irish\_orthography](https://en.wikipedia.org/wiki/Irish_orthography)  
29. Dúchas \- National Folklore Collection \- University College Dublin, accessed December 24, 2025, [https://www.ucd.ie/irishfolklore/en/duchas/](https://www.ucd.ie/irishfolklore/en/duchas/)  
30. Research Newsletter \- Issue 109: Open Research in Action \- DCU, accessed December 24, 2025, [https://www.dcu.ie/research/research-newsletter-issue-109-open-research-action](https://www.dcu.ie/research/research-newsletter-issue-109-open-research-action)  
31. Training a bilingual Irish-English model in Transkribus using An Gaodhal, accessed December 24, 2025, [https://blog.transkribus.org/en/training-a-bilingual-irish-english-model-in-transkribus-using-an-gaodhal](https://blog.transkribus.org/en/training-a-bilingual-irish-english-model-in-transkribus-using-an-gaodhal)  
32. An Gaodhal Newspaper (1881-1898) Full-Text OCR Output Files \- UltraViolet, accessed December 24, 2025, [https://ultraviolet.library.nyu.edu/records/5ya5n-mc504](https://ultraviolet.library.nyu.edu/records/5ya5n-mc504)  
33. Placenames Database of Ireland \- Wikipedia, accessed December 24, 2025, [https://en.wikipedia.org/wiki/Placenames\_Database\_of\_Ireland](https://en.wikipedia.org/wiki/Placenames_Database_of_Ireland)  
34. Logainm: The Placenames Database of Ireland \- Dataset \- data.gov.ie, accessed December 24, 2025, [https://data.gov.ie/dataset/logainm](https://data.gov.ie/dataset/logainm)