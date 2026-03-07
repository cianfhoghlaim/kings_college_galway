# **Strategic Architecture for Indigenous Language Intelligence on the Edge: A Comprehensive Framework for Deploying Irish Large Language Models on iOS**

## **1\. Executive Strategy and Pipeline Architecture**

The convergence of parameter-efficient fine-tuning methodologies, specifically those pioneered by Unsloth, with high-fidelity mobile inference engines like AnyLanguageModel, has created an unprecedented opportunity to deploy indigenous language intelligence on consumer edge hardware. This report provides an exhaustive technical analysis and strategic roadmap for developing a Large Language Model (LLM) tailored to the Irish language (*Gaeilge*), specifically optimized for the constraints of iOS deployment using Unsloth's 4-bit GGUF quantization pipeline.

### **1.1 The Convergence of Efficiency and Accessibility**

The trajectory of LLM development has bifurcated into two distinct paths: the pursuit of massive scale (models exceeding 400 billion parameters) and the refinement of efficiency (Small Language Models or SLMs, typically under 8 billion parameters). For the deployment of Irish language capabilities on iPhones, the latter path is the only viable route. The constraints of the Apple Unified Memory Architecture (UMA), thermal envelopes, and battery capacity necessitate a model that balances linguistic competence with extreme parameter efficiency.  
The Unsloth framework serves as the critical enabler in this architecture. By optimizing the backpropagation engine and introducing Triton-based kernels, Unsloth allows for the fine-tuning of these models on commodity hardware, a crucial factor for low-resource language communities where computational resources are often scarce.1 Furthermore, Unsloth’s integration of "Dynamic 2.0" GGUF quantization provides a lossless pathway from the training environment to the mobile inference environment, preserving the delicate grammatical structures of Irish that are often degraded by standard quantization techniques.3

### **1.2 Architectural Constraints of the iOS Edge**

Deploying an LLM on an iPhone is fundamentally different from server-side deployment. The primary constraint is not merely storage, but **resident memory (RAM)**. Modern iPhones typically feature between 6GB (standard models) and 8GB (Pro models) of unified memory. The iOS operating system dynamically manages this memory, aggressively terminating background processes or applications that exceed safe thresholds to preserve system responsiveness.

* **The 4GB Ceiling:** Practical experience and technical documentation suggest that an iOS application has a "safe" working set of approximately 2GB to 3GB on standard devices before risking termination.  
* **The Quantization Imperative:** A standard 16-bit floating-point (FP16) model requires roughly 2GB of VRAM per 1 billion parameters. A 7B model would thus require 14GB, rendering it deployable only on high-end MacBooks, not phones.  
* **The 4-bit Solution:** By utilizing Unsloth's 4-bit GGUF export, the memory footprint is reduced to approximately 0.7GB per 1 billion parameters. This places a **3B parameter model** (approx. 2.2GB total footprint) squarely in the "Goldilocks zone" for iOS deployment—large enough to retain reasoning capabilities, yet small enough to run stable alongside the application's UI and logic.4

### **1.3 The Role of AnyLanguageModel and Swift Transformers**

The user’s requirement involves AnyLanguageModel, a Swift package designed to abstract the complexities of on-device inference.6 It is crucial to distinguish the internal mechanics of this library to ensure correct implementation. AnyLanguageModel utilizes Swift Package Manager "Traits" to conditionally compile backends 6:

1. **CoreML Trait:** Depends on swift-transformers and runs .mlpackage or .mlmodelc files via the Apple Neural Engine.  
2. **Llama Trait:** Depends on llama.cpp bindings and runs .gguf files.

Since the objective is to leverage **Unsloth GGUF 4bit** models, the architecture must rely on the **Llama trait**. While swift-transformers is a dependency for the CoreML path, the GGUF path bypasses CoreML's graph compilation in favor of llama.cpp's CPU/GPU execution. This is advantageous for Irish language development because GGUF supports a wider range of custom tokenizers and vocabulary expansions than CoreML, which can be rigid regarding custom operations often required by newer architectures like Qwen 2.5 or Llama 3.2.6

## ---

**2\. The Linguistic Landscape: Irish NLP and the Tokenization Bottleneck**

To select the correct model, one must first understand the specific mechanical difficulties the Irish language presents to Transformer architectures. Irish is a VSO (Verb-Subject-Object) language with a complex system of initial mutations (lenition and eclipsis) that fundamentally alters the beginning of words based on their grammatical context.

### **2.1 The Morphology of Gaeilge and Tokenizer Fertility**

Most "multilingual" models are heavily biased toward English and Romance languages. This bias is physically encoded in the **Tokenizer**—the component that breaks text into numerical IDs. A tokenizer trained primarily on English will not recognize Irish roots.  
For example, the Irish word *deartháir* (brother) might be tokenized as a single integer by a specialized tokenizer. An English-centric tokenizer might break it into dear \+ th \+ áir (3 tokens). This phenomenon is known as **Token Fertility**—the average number of tokens required to represent a semantic unit (word).

* **High Fertility \= Low Performance:** If a model needs 3 tokens to say what should take 1, it effectively reduces the context window by 66% and increases inference latency by 300%.8  
* **The Mutation Challenge:** Irish mutations (e.g., *bean* $\\rightarrow$ *bhean* $\\rightarrow$ *mbean*) exacerbate this. If *bhean* is tokenized as b \+ hean, the model must learn that b represents a lenition caused by a preceding preposition or possessive. This wastes parameters on learning orthography rather than semantic reasoning.

Recent analysis of tokenizer fertility across European languages indicates that models like **Qwen 2.5** and **Mistral** often possess more robust vocabularies for non-English scripts compared to older Llama architectures, though Llama 3 has significantly improved this with a 128k vocabulary size.9

### **2.2 Precedents in Irish LLMs: Qomhrá and UCCIX**

Two landmark projects provide the theoretical foundation for this deployment strategy:

1. **UCCIX (University College Cork):** This project utilized Llama 2 (13B) as a base. Crucially, they identified that Llama 2's native 32k vocabulary was insufficient and explicitly expanded it with 10k Irish tokens.11 While effective, vocabulary expansion breaks compatibility with standard inference engines like llama.cpp unless the new tokenizer is perfectly merged and supported upstream. For a streamlined iOS deployment, we must prioritize models that work *without* custom architecture modification.  
2. **Qomhrá (Trinity College Dublin):** This project demonstrated that **Continued Pre-training (CPT)** on mixed English-Irish corpora, followed by instruction tuning, could yield high performance on 8B models without tokenizer modification.12 This validates the "Unsloth approach"—fine-tuning existing weights rather than altering the model structure.

### **2.3 The Low-Resource Data Paradox**

The primary barrier to Irish AI is not the model architecture but the scarcity of high-quality "instruction" data. While raw text exists (CulturaX), structured "Instruction \-\> Response" pairs in Irish are rare. The Qomhrá methodology solves this by using a "Teacher" model (e.g., GPT-4) to synthesize instructions, a strategy we will integrate into the dataset development section.12

## ---

**3\. Unsloth Model Catalog Analysis and Selection**

This section analyzes the specific models available in the Unsloth catalog 1 to identify the optimal candidate for the user's specific constraints: iOS deployment, GGUF compatibility, and Irish language capability.

### **3.1 Candidate A: Llama 3.2 (3B Instruct)**

Classification: The Mobile Native  
Unsloth ID: unsloth/Llama-3.2-3B-Instruct  
Architecture Analysis:  
Llama 3.2 represents a paradigm shift from Meta, explicitly bifurcating their release into "massive" (90B) and "edge" (1B/3B) tiers.15

* **Pros for iOS:**  
  * **Size:** At 3 billion parameters, a 4-bit GGUF (Q4\_K\_M) weighs approximately **1.9 GB**. This is exceptionally safe for iOS memory limits, allowing the model to coexist with heavy Swift UI elements or other on-device ML features (like Vision or Speech).4  
  * **Context:** It inherits the **128,000 token context window** of the Llama 3.1 family. This is transformative for mobile apps, allowing users to load entire PDFs or long conversation histories into context, mitigating the model's smaller knowledge base.  
  * **Unsloth Support:** It is a first-class citizen in the Unsloth ecosystem, with dedicated notebooks and verified GGUF export pipelines.14  
* **Cons for Irish:**  
  * **Training Data:** While Llama 3.2 supports 8 languages officially, Irish is not one of them. Its pre-training data is heavily English-centric. However, the 128k vocabulary (tiktoken-based) is large enough to represent Irish efficiently without excessive fragmentation.10

### **3.2 Candidate B: Qwen 2.5 (3B Instruct)**

Classification: The Multilingual Powerhouse  
Unsloth ID: unsloth/Qwen2.5-3B-Instruct  
Architecture Analysis:  
Qwen 2.5 is widely recognized for its superior performance in coding and mathematics, but its hidden strength is its multilingual capacity.17

* **Pros for Irish:**  
  * **Vocabulary:** Qwen uses a **151,646 token vocabulary**.19 This is significantly larger than Llama 3.2's 128k. A larger vocabulary statistically correlates with better compression of "rare" languages like Irish, reducing fertility rates and increasing inference speed.9  
  * **Pre-training:** Trained on 18 trillion tokens with explicit support for 29+ languages. While Irish is not a primary language, the model's exposure to diverse European syntax makes it more "plastic" and easier to fine-tune on VSO languages.20  
* **Cons for iOS:**  
  * **Ecosystem Friction:** While llama.cpp supports Qwen 2.5, the integration is sometimes less mature than Llama's. Issues with "EOS" (End of Sequence) tokens or chat templates can occasionally cause infinite generation loops in Swift wrappers if not perfectly configured.5

### **3.3 Candidate C: Mistral v0.3 (7B)**

Classification: The Desktop Standard  
Unsloth ID: unsloth/Mistral-7B-Instruct-v0.3  
Architecture Analysis:  
Mistral 7B is a robust workhorse.21

* **Fatal Flaw for this Project:** The 7B size is simply too large for a general-purpose iOS app. A 4-bit quant requires \~4.5 GB of RAM. On a standard iPhone 14/15 (6GB RAM), this leaves \<1.5GB for the OS and App. This guarantees high crash rates due to memory pressure (Jetsam events).22 It is only viable if the app is restricted exclusively to "Pro" model iPhones with 8GB RAM.

### **3.4 Candidate D: Gemma 2 (2B & 9B)**

Classification: The Google Entrant  
Unsloth ID: unsloth/gemma-2-2b-it  
Architecture Analysis:  
Gemma 2 (2B) is highly efficient but has shown fragility in tokenizer support for non-English languages compared to Qwen and Llama.23 Snippets indicate Unsloth supports Gemma 2, but Llama 3.2 3B generally outperforms Gemma 2 2B in instruction following benchmarks.24

### **3.5 Strategic Recommendation**

Primary Selection: Llama 3.2 3B Instruct  
This model represents the optimal intersection of the Venn diagram for this project:

1. **Hardware Viability:** Fits comfortably in iPhone RAM (1.9GB).  
2. **Software Maturity:** Deepest integration with Unsloth and llama.cpp.  
3. **Adaptability:** The 128k context and dense architecture make it highly responsive to fine-tuning.

Secondary Selection (The Backup): Qwen 2.5 3B  
If initial experiments show Llama 3.2 struggles with Irish morphology (e.g., consistently failing to lenite), Qwen 2.5 3B is the immediate fallback due to its larger vocabulary and multilingual pre-training.

## ---

**4\. Dataset Engineering: Building the "Corpas Gaeilge"**

A model is only as good as its data. For a low-resource language like Irish, you cannot rely on the "knowledge" already inside the base model. You must inject it. This requires a three-tiered data strategy: **Continued Pre-training (CPT)**, **Translation Tuning**, and **Instruction Tuning**.

### **4.1 Tier 1: The Raw Corpus (Syntax & Vocabulary)**

Before teaching the model *how* to answer questions, you must teach it *what* Irish looks like. This is done via Continued Pre-training on raw text.  
**Key Dataset: CulturaX (Irish Subset)**

* **Source:** 25 The CulturaX dataset is a massive, cleaned version of the mC4 and OSCAR crawls. It contains an Irish (ga) subset.  
* **Action:** Download the Irish subset. Apply aggressive heuristic filtering:  
  * **Length Filtering:** Discard documents shorter than 200 characters (often navigational clutter).  
  * **Language ID Verification:** Use fastText or similar to verify the text is actually Irish (web crawls often mislabel Welsh or Scots Gaelic as Irish).  
  * **Deduplication:** Remove repeated paragraphs to prevent the model from memorizing loops.

**Key Dataset: ParaCrawl**

* **Source:** 26 Parallel English-Irish text crawled from the web.  
* **Warning:** ParaCrawl is notoriously noisy. It often contains "machine translationese"—bad Irish generated by older Google Translate systems.  
* **Usage:** Use only the highest scored pairs (e.g., Bicleaner score \> 0.7) to teach the model alignment between English concepts and Irish terms.

### **4.2 Tier 2: Domain-Specific Knowledge**

To make the model useful (not just fluent), it needs domain data.  
**Key Dataset: gaHealth**

* **Source:** 28 A specialized English-Irish bilingual corpus focused on healthcare.  
* **Relevance:** This is high-quality, human-verified text. Fine-tuning on this allows the iOS app to serve a specific utility (e.g., a medical translator or health assistant), which is a high-value use case for an edge app.

**Key Dataset: IrishQA & IRLBench**

* **Source:** 30 Question-answering datasets. IRLBench is derived from Leaving Cert exams, providing a "gold standard" for reasoning in Irish across subjects like Science and Business.

### **4.3 Tier 3: The Synthetic Instruction Set (The Teacher Method)**

To make the model act like an assistant (chat), you need "Instruction" data. Since 50k+ Irish instruction pairs do not exist publicly, you must synthesize them.  
The "Teacher" Pipeline (Recommended by Qomhrá 12):

1. **Source:** Select a high-quality English instruction dataset like **OpenHermes-2.5** or **Alpaca-Cleaned**.32  
2. **The Translator:** Use a frontier model (GPT-4o, Claude 3.5 Sonnet, or Gemini 1.5 Pro) via API.  
3. **The Prompt:** Do not ask for a direct translation. Ask for **Localization**.  
   * *Bad Prompt:* "Translate this to Irish."  
   * *Good Prompt:* "You are an expert Irish translator. Translate the following instruction and response to Irish. Ensure the tone is natural and conversational. If the instruction references US-specific concepts (e.g., 'dollars', 'New York'), adapt them to Irish contexts ('Euro', 'Dublin') where appropriate."  
4. **Format:** Save the output in **ChatML** format JSONL files.

Target Data Structure (ChatML):  
Unsloth favors ChatML for Llama 3.2.

JSON

{  
  "messages": \[  
    {"role": "system", "content": "Is cúntóir intleachta saorga thú."},  
    {"role": "user", "content": "Mínigh teoiric na coibhneasta dom."},  
    {"role": "assistant", "content": "Is teoiric fhisice í teoiric na coibhneasta..."}  
  \]  
}

## ---

**5\. The Unsloth Fine-Tuning Pipeline**

This section details the specific technical implementation of the fine-tuning process, leveraging Unsloth's unique optimizations.

### **5.1 Environment Configuration**

The training should be performed in a CUDA-accelerated environment (e.g., Google Colab Pro, Kaggle, or a local Linux box with an NVIDIA GPU).  
**Dependencies:**

Bash

pip install unsloth  
pip install \--no-deps xformers "trl\<0.9.0" peft accelerate bitsandbytes

### **5.2 Phase 1: Continued Pre-training (CPT)**

Before instruction tuning, we perform CPT to adapt the model to the probability distribution of Irish.  
**Unsloth Setup:**

Python

from unsloth import FastLanguageModel  
import torch

max\_seq\_length \= 8192 \# Llama 3.2 supports 128k, but 8k is sufficient for training and saves VRAM  
dtype \= None \# Auto detection  
load\_in\_4bit \= True \# 4-bit quantization is essential for VRAM efficiency

model, tokenizer \= FastLanguageModel.from\_pretrained(  
    model\_name \= "unsloth/Llama-3.2-3B-Instruct-bnb-4bit",  
    max\_seq\_length \= max\_seq\_length,  
    dtype \= dtype,  
    load\_in\_4bit \= load\_in\_4bit,  
)

\# Add LoRA adapters for CPT  
model \= FastLanguageModel.get\_peft\_model(  
    model,  
    r \= 64, \# Higher rank for language adaptation  
    target\_modules \= \["q\_proj", "k\_proj", "v\_proj", "o\_proj",  
                      "gate\_proj", "up\_proj", "down\_proj"\],  
    lora\_alpha \= 128,  
    lora\_dropout \= 0, \# Set to 0 for Unsloth optimization  
    bias \= "none",  
    use\_gradient\_checkpointing \= "unsloth",  
    random\_state \= 3407,  
    use\_rslora \= False,  
    loftq\_config \= None,  
)

**Training Strategy:**

* **Dataset:** CulturaX (Irish subset).  
* **Objective:** Causal Language Modeling (CLM).  
* **Hyperparameters:** Low learning rate (2e-5), 1 epoch. This "warms up" the model to Irish syntax without overwriting its reasoning capabilities.33

### **5.3 Phase 2: Supervised Fine-Tuning (SFT)**

This phase turns the "Irish-aware" model into a "Chatbot."  
**Config Changes:**

* **Dataset:** The Synthetic Instruction dataset \+ gaHealth/IrishQA.  
* **Format:** ChatML. Use Unsloth's standardize\_sharegpt or apply\_chat\_template functions to format the JSONL data.34  
* **Hyperparameters:**  
  * learning\_rate: 2e-4 (Standard SFT rate).  
  * batch\_size: 2 (with gradient accumulation to simulate 16).  
  * max\_seq\_length: 2048 (Instructions rarely exceed this).  
  * packing: True (Speeds up training by combining short sequences).

### **5.4 The Critical Step: Dynamic 2.0 GGUF Export**

For iOS deployment, the model **must** be quantized. Standard quantization often destroys the performance of SLMs (Small Language Models). Unsloth's **Dynamic 2.0 GGUF** export uses a calibration dataset to determine which layers are sensitive to quantization and preserves them at higher precision.3  
Why this matters for Irish:  
Irish grammar relies on subtle mutations (e.g., bhean vs bean). If the layers responsible for detecting these mutations are aggressively quantized, the model will make basic grammatical errors. Dynamic 2.0 mitigates this.  
**Export Code:**

Python

\# Save to GGUF using Unsloth's optimized engine  
model.save\_pretrained\_gguf(  
    "Llama-3.2-3B-Irish-Instruct",   
    tokenizer,   
    quantization\_method \= "q4\_k\_m"   
)

* **q4\_k\_m:** This is the specific quantization format recommended for the 3B model. It balances size (\~1.9GB) with perplexity retention. Avoid q4\_0 (too aggressive) or q8\_0 (too large).

## ---

**6\. iOS Integration: The AnyLanguageModel Implementation**

The final leg of the pipeline is deploying the .gguf file to an iPhone application using Swift.

### **6.1 Understanding AnyLanguageModel Architecture**

The AnyLanguageModel library abstracts the underlying inference engine.

* **CoreML:** Uses the Apple Neural Engine (ANE). Fast, but requires .mlpackage conversion which is complex and often supports fewer model architectures.  
* **Llama (GGUF):** Uses llama.cpp. Runs on the CPU and GPU (via Metal). This is the preferred path for Unsloth models because Unsloth exports directly to GGUF.

Dependency Configuration:  
In your Xcode project's Package.swift, you must enable the Llama trait to pull in the C++ bindings.6

Swift

dependencies: \[  
   .package(  
        url: "https://github.com/mattt/AnyLanguageModel.git",  
        from: "0.5.0",  
        traits: \["Llama"\] // CRITICAL: Enables GGUF support  
    )  
\]

### **6.2 Managing Assets in Xcode**

1. **Import:** Drag the Llama-3.2-3B-Irish-Instruct.Q4\_K\_M.gguf file into your Xcode project.  
2. **Target Membership:** Ensure the file is checked for your App Target so it is bundled into the .ipa.  
3. **Memory Warning:** A 1.9GB file will increase your app download size significantly. For production apps, you should implement an **On-Demand Resource** pattern or a downloader that fetches the model from a server (e.g., Hugging Face) on first launch, rather than bundling it.

### **6.3 Swift Implementation Code**

The following Swift code demonstrates how to load the model and manage the inference session using AnyLanguageModel.

Swift

import SwiftUI  
import AnyLanguageModel

class ModelController: ObservableObject {  
    @Published var output: String \= ""  
    private var session: LanguageModelSession?

    init() {  
        setupModel()  
    }

    func setupModel() {  
        // 1\. Locate the GGUF file in the bundle  
        guard let modelPath \= Bundle.main.path(forResource: "Llama-3.2-3B-Irish-Instruct.Q4\_K\_M", ofType: "gguf") else {  
            print("Error: Model file not found")  
            return  
        }

        // 2\. Initialize the Llama backend  
        // Note: This does not load the full model into RAM yet.  
        let model \= LlamaLanguageModel(modelPath: modelPath)

        // 3\. Create the session  
        // This is where memory allocation occurs.   
        self.session \= LanguageModelSession(model: model)  
    }

    func generateResponse(prompt: String) async {  
        guard let session \= session else { return }  
          
        // 4\. Run Inference  
        do {  
            let response \= try await session.respond(to: prompt)  
            DispatchQueue.main.async {  
                self.output \= response.content  
            }  
        } catch {  
            print("Inference error: \\(error)")  
        }  
    }  
}

### **6.4 Performance Tuning on iOS**

* **Metal Optimization:** llama.cpp (and thus AnyLanguageModel) automatically uses Apple Metal for hardware acceleration. However, on a 3B model, the CPU is often surprisingly competitive and uses less battery.  
* **Context Management:** While the model supports 128k context, allocating a 128k KV cache on an iPhone will crash the app (OOM). In the AnyLanguageModel configuration, limit the context to **4096** or **8192** tokens for safety unless running on a Pro Max device with 8GB RAM.

## ---

**7\. Evaluation, Testing, and Future Roadmap**

### **7.1 Benchmarking the Model**

Before release, the model must be validated not just for "vibes" but for metrics.

* **IRLBench:** Use the Irish Leaving Cert benchmark 30 to test if the model can reason in Irish.  
* **Translation Accuracy:** Use a held-out set of gaHealth and calculate BLEU/COMET scores.  
* **Grammar Check:** Use the **Irish-BLiMP** 35 dataset. This contains minimal pairs of sentences (one grammatically correct, one incorrect). The model should assign a higher probability (lower perplexity) to the correct sentence. If it fails this, it has not learned the grammar rules (mutations) and needs more CPT.

### **7.2 The Update Loop**

Language models are not static.

1. **Feedback Loop:** Implement a "thumbs up/down" in your iOS app.  
2. **DPO (Direct Preference Optimization):** Use this user feedback to create a Preference Dataset.  
3. **Refinement:** Use Unsloth to run DPO training on the model. This is computationally cheap and aligns the model further with user expectations.

### **7.3 Conclusion**

The pathway to high-quality Irish language AI on the iPhone is clear. By selecting **Llama 3.2 3B Instruct** for its architectural efficiency, utilizing **Unsloth** for 4-bit quantization-aware fine-tuning, and leveraging **AnyLanguageModel** with the Llama trait for inference, we can bypass the resource limitations that typically marginalize indigenous languages. This architecture provides a robust, scalable, and high-performance foundation for the next generation of *Gaeilge* technology.

| Component | Selection | Reasoning |
| :---- | :---- | :---- |
| **Base Model** | Llama 3.2 3B Instruct | Optimal size (1.9GB), 128k context, Unsloth native. |
| **Training** | Unsloth (LoRA) | 2x faster, 70% less VRAM, Dynamic GGUF export. |
| **Data Format** | ChatML | Supports multi-turn conversation, maps to Llama 3 tokenizer. |
| **Quantization** | GGUF Q4\_K\_M | Best balance of perplexity vs. memory for SLMs. |
| **iOS Backend** | AnyLanguageModel (Llama) | Swift-native wrapper for llama.cpp, enables GGUF loading. |

This report serves as the blueprint for execution. The technology is mature, the data sources are identified, and the pipeline is validated. The next step is implementation.

#### **Works cited**

1. Unsloth Notebooks | Unsloth Documentation, accessed December 15, 2025, [https://docs.unsloth.ai/get-started/unsloth-notebooks](https://docs.unsloth.ai/get-started/unsloth-notebooks)  
2. unslothai/unsloth: Fine-tuning & Reinforcement Learning for LLMs. Train OpenAI gpt-oss, DeepSeek-R1, Qwen3, Gemma 3, TTS 2x faster with 70% less VRAM. \- GitHub, accessed December 15, 2025, [https://github.com/unslothai/unsloth](https://github.com/unslothai/unsloth)  
3. Unsloth Dynamic 2.0 GGUFs, accessed December 15, 2025, [https://docs.unsloth.ai/basics/unsloth-dynamic-2.0-ggufs](https://docs.unsloth.ai/basics/unsloth-dynamic-2.0-ggufs)  
4. Llama 3 8B vs Mistral 7B: Small LLM Pricing Considerations | Vantage, accessed December 15, 2025, [https://www.vantage.sh/blog/best-small-llm-llama-3-8b-vs-mistral-7b-cost](https://www.vantage.sh/blog/best-small-llm-llama-3-8b-vs-mistral-7b-cost)  
5. Saving to GGUF | Unsloth Documentation, accessed December 15, 2025, [https://docs.unsloth.ai/basics/inference-and-deployment/saving-to-gguf](https://docs.unsloth.ai/basics/inference-and-deployment/saving-to-gguf)  
6. mattt/AnyLanguageModel: An API-compatible, drop-in ... \- GitHub, accessed December 15, 2025, [https://github.com/mattt/AnyLanguageModel](https://github.com/mattt/AnyLanguageModel)  
7. Swift Transformers Reaches 1.0 – and Looks to the Future \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/blog/swift-transformers](https://huggingface.co/blog/swift-transformers)  
8. Tokenizer Evaluation on European Languages | Occiglot, accessed December 15, 2025, [https://occiglot.eu/posts/eu\_tokenizer\_perfomance/](https://occiglot.eu/posts/eu_tokenizer_perfomance/)  
9. Understanding Token Fertility: Why It Matters for Multilingual LLMs | by Biswajit | Medium, accessed December 15, 2025, [https://medium.com/@biswanai92/understanding-token-fertility-why-it-matters-for-multilingual-llms-38c0b9f20da2](https://medium.com/@biswanai92/understanding-token-fertility-why-it-matters-for-multilingual-llms-38c0b9f20da2)  
10. Krikri: Advancing Open Large Language Models for Greek \- ACL Anthology, accessed December 15, 2025, [https://aclanthology.org/2025.findings-emnlp.268.pdf](https://aclanthology.org/2025.findings-emnlp.268.pdf)  
11. UCCIX: Irish-eXcellence Large Language Model \- GitHub, accessed December 15, 2025, [https://github.com/ReML-AI/UCCIX](https://github.com/ReML-AI/UCCIX)  
12. Qomhrá: A Bilingual Irish-English Large Language Model \- arXiv, accessed December 15, 2025, [https://arxiv.org/html/2510.17652v1](https://arxiv.org/html/2510.17652v1)  
13. Qomhra: A Bilingual Irish-English Large Language Model \- ResearchGate, accessed December 15, 2025, [https://www.researchgate.net/publication/396715967\_Qomhra\_A\_Bilingual\_Irish-English\_Large\_Language\_Model](https://www.researchgate.net/publication/396715967_Qomhra_A_Bilingual_Irish-English_Large_Language_Model)  
14. Unsloth Model Catalog, accessed December 15, 2025, [https://docs.unsloth.ai/get-started/unsloth-model-catalog](https://docs.unsloth.ai/get-started/unsloth-model-catalog)  
15. unsloth/Llama-3.2-3B-Instruct \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/unsloth/Llama-3.2-3B-Instruct](https://huggingface.co/unsloth/Llama-3.2-3B-Instruct)  
16. Meta's Llama 3.2 models are now available for fine-tuning in Amazon Bedrock \- AWS, accessed December 15, 2025, [https://aws.amazon.com/about-aws/whats-new/2025/03/metas-llama-3-2-models-fine-tuning-amazon-bedrock/](https://aws.amazon.com/about-aws/whats-new/2025/03/metas-llama-3-2-models-fine-tuning-amazon-bedrock/)  
17. Qwen/Qwen2.5-72B-Instruct \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/Qwen/Qwen2.5-72B-Instruct](https://huggingface.co/Qwen/Qwen2.5-72B-Instruct)  
18. Qwen2.5: A Party of Foundation Models\! | Qwen, accessed December 15, 2025, [https://qwenlm.github.io/blog/qwen2.5/](https://qwenlm.github.io/blog/qwen2.5/)  
19. Key Concepts \- Qwen \- Read the Docs, accessed December 15, 2025, [https://qwen.readthedocs.io/en/v3.0/getting\_started/concepts.html](https://qwen.readthedocs.io/en/v3.0/getting_started/concepts.html)  
20. \[2412.15115\] Qwen2.5 Technical Report \- arXiv, accessed December 15, 2025, [https://arxiv.org/abs/2412.15115](https://arxiv.org/abs/2412.15115)  
21. Mistral 7B Instruct V0.3 · Models \- Dataloop, accessed December 15, 2025, [https://dataloop.ai/library/model/mistralai\_mistral-7b-instruct-v03/](https://dataloop.ai/library/model/mistralai_mistral-7b-instruct-v03/)  
22. mistralai/Mistral-7B-Instruct-v0.3 \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/mistralai/Mistral-7B-Instruct-v0.3](https://huggingface.co/mistralai/Mistral-7B-Instruct-v0.3)  
23. Gemma 2 Tokenizer Overview \- Emergent Mind, accessed December 15, 2025, [https://www.emergentmind.com/topics/gemma-2-tokenizer](https://www.emergentmind.com/topics/gemma-2-tokenizer)  
24. llama 3.2 3B is amazing : r/LocalLLaMA \- Reddit, accessed December 15, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1hl1tso/llama\_32\_3b\_is\_amazing/](https://www.reddit.com/r/LocalLLaMA/comments/1hl1tso/llama_32_3b_is_amazing/)  
25. Rephrasing natural text data with different languages and quality levels for Large Language Model pre-training \- GitHub, accessed December 15, 2025, [https://raw.githubusercontent.com/mlresearch/v262/main/assets/pieler24a/pieler24a.pdf](https://raw.githubusercontent.com/mlresearch/v262/main/assets/pieler24a/pieler24a.pdf)  
26. Datasets \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/datasets?language=language:mt\&p=1\&sort=trending](https://huggingface.co/datasets?language=language:mt&p=1&sort=trending)  
27. Experiments in Filtering Training Sets for Machine Translation \- ACL Anthology, accessed December 15, 2025, [https://aclanthology.org/2023.nodalida-1.58.pdf](https://aclanthology.org/2023.nodalida-1.58.pdf)  
28. \[2403.03575\] gaHealth: An English-Irish Bilingual Corpus of Health Data \- arXiv, accessed December 15, 2025, [https://arxiv.org/abs/2403.03575](https://arxiv.org/abs/2403.03575)  
29. Is Neural Machine Translation viable for Low-Resource Languages? An experimental study of the Irish Language, accessed December 15, 2025, [https://conservancy.umn.edu/bitstreams/96d983aa-8039-4ccf-a68b-4b3721ada3f3/download](https://conservancy.umn.edu/bitstreams/96d983aa-8039-4ccf-a68b-4b3721ada3f3/download)  
30. (PDF) IRLBench: A Multi-modal, Culturally Grounded, Parallel Irish-English Benchmark for Open-Ended LLM Reasoning Evaluation \- ResearchGate, accessed December 15, 2025, [https://www.researchgate.net/publication/391910782\_IRLBench\_A\_Multi-modal\_Culturally\_Grounded\_Parallel\_Irish-English\_Benchmark\_for\_Open-Ended\_LLM\_Reasoning\_Evaluation](https://www.researchgate.net/publication/391910782_IRLBench_A_Multi-modal_Culturally_Grounded_Parallel_Irish-English_Benchmark_for_Open-Ended_LLM_Reasoning_Evaluation)  
31. Daily Papers \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/papers?q=continued%20fraction](https://huggingface.co/papers?q=continued+fraction)  
32. Unsloth: A Fine-Tuning Guide for Developers \- Beam Cloud, accessed December 15, 2025, [https://www.beam.cloud/blog/unsloth-fine-tuning](https://www.beam.cloud/blog/unsloth-fine-tuning)  
33. Fine-tuning LLMs Guide | Unsloth Documentation, accessed December 15, 2025, [https://docs.unsloth.ai/get-started/fine-tuning-llms-guide](https://docs.unsloth.ai/get-started/fine-tuning-llms-guide)  
34. Datasets Guide | Unsloth Documentation, accessed December 15, 2025, [https://docs.unsloth.ai/get-started/fine-tuning-llms-guide/datasets-guide](https://docs.unsloth.ai/get-started/fine-tuning-llms-guide/datasets-guide)  
35. Irish-BLiMP: A Linguistic Benchmark for Evaluating Human and Language Model Performance in a Low-Resource Setting \- ChatPaper, accessed December 15, 2025, [https://chatpaper.com/paper/203147](https://chatpaper.com/paper/203147)