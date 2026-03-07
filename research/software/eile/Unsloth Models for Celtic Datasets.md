# **Optimizing Open-Weights Large Language Models for Celtic Linguistics, Educational Analytics, and Multimodal Asset Generation: A Comprehensive Technical Analysis of the Unsloth Ecosystem**

## **1\. Introduction**

The democratization of artificial intelligence through open-weights Large Language Models (LLMs) has fundamentally altered the landscape of computational linguistics and educational technology. For specialized domains such as the revitalization of Celtic languages (Irish, Welsh, Scottish Gaelic) and the development of rigorous educational analytics, the reliance on proprietary, closed-source models often presents insurmountable barriers regarding data privacy, cost, and linguistic inclusivity. The emergence of **Unsloth**, a specialized fine-tuning framework, has dismantled the hardware barriers that previously restricted frontier-class model adaptation to well-funded research laboratories. By optimizing the backpropagation pipeline and leveraging custom Triton kernels, Unsloth enables the fine-tuning of massive architectures—including Llama 3.3 70B, Qwen 2.5, and DeepSeek-R1—on consumer-grade hardware, making high-fidelity model adaptation accessible for niche applications.1  
This report provides an exhaustive analysis of the current Unsloth model catalog, evaluating architectures from Meta, Alibaba Cloud, DeepSeek, Google, Microsoft, and Black Forest Labs against the tripartite objectives of Celtic language preservation, multimodal educational asset generation, and advanced pedagogical reasoning. It synthesizes performance benchmarks, architectural innovations, and licensing constraints to offer a strategic roadmap for dataset compilation and fine-tuning execution.

## **2\. The Unsloth Optimization Framework: Technical Architecture**

To understand the viability of deploying 70-billion parameter models for Celtic translation or educational reasoning on constrained budgets, one must first understand the mechanical innovations of the Unsloth framework. Traditional fine-tuning via Hugging Face’s transformers library relies on PyTorch’s standard autograd engine, which, while flexible, incurs significant memory overhead due to suboptimal memory fragmentation and redundant activation storage.

### **2.1 Gradient Checkpointing and Memory Efficiency**

Unsloth’s primary contribution to the field is its manual derivation of backpropagation gradients for LoRA (Low-Rank Adaptation) layers, implemented via custom OpenAI Triton kernels. This approach bypasses the automated but memory-heavy gradient computation of PyTorch. Furthermore, Unsloth implements a "smart" gradient checkpointing algorithm that intelligently offloads activation states to system RAM rather than consuming precious GPU VRAM. This innovation is critical for processing the long-context documents typical of educational curricula or historical Celtic literature.  
Empirical benchmarks demonstrate the magnitude of this efficiency. Fine-tuning a **Llama 3.3 70B** model, which typically requires massive H100 clusters, is made possible on a single 48GB GPU (such as an NVIDIA RTX A6000 or A40) or dual 24GB GPUs (RTX 3090/4090) through Unsloth’s optimization. Specifically, Unsloth reduces VRAM usage by over 60% compared to standard Flash Attention 2 implementations while increasing training speed by approximately 2x.3 For the smaller **Llama 3.1 8B** model, Unsloth enables context windows of up to 342,000 tokens on an 80GB GPU, a 12x increase over native implementations, allowing for the ingestion of entire textbooks or legislative archives in a single training pass.3

### **2.2 Quantization Dynamics**

The framework heavily utilizes 4-bit NormalFloat (NF4) quantization, a technique that compresses the model weights while preserving the distribution of the data. Unlike standard quantization which can degrade reasoning capabilities—a fatal flaw for educational analytics—Unsloth employs "Dynamic 2.0" quantization. This methodology selectively upcasts critical layers (such as the input/output embeddings and specific attention heads) to higher precision (16-bit) during the forward pass, ensuring that the model’s logical coherence remains intact while minimizing the memory footprint.2 This balance is essential when the model must discern subtle grammatical mutations in Welsh or complex algebraic proofs in an educational setting.

### **2.3 The Unsloth Model Catalog**

The versatility of Unsloth lies in its rapid support for new architectures. The catalog currently encompasses:

* **Dense Architectures**: Llama 3.x (Meta), Qwen 2.5 (Alibaba), Gemma 2/3 (Google), Mistral (Mistral AI), Phi-4 (Microsoft).  
* **Mixture-of-Experts (MoE)**: Mixtral 8x7B/8x22B, DeepSeek-V3, Qwen-MoE.  
* **Vision-Language Models (VLMs)**: Llama 3.2 Vision, Qwen 2.5-VL, Pixtral.6  
* **Reasoning Models**: DeepSeek-R1 (and distillations), QwQ-32B.

This broad compatibility allows researchers to select the optimal architecture for specific sub-tasks: high-throughput reasoning for analytics, dense multilingualism for translation, or vision capabilities for asset generation.

## **3\. Comparative Analysis of Architectures for Celtic and Educational Domains**

Selecting the appropriate base model is the foundational decision in any fine-tuning workflow. The requirements for Celtic languages (low-resource, morphologically rich) and educational analytics (high reasoning, zero-tolerance for hallucination) demand distinct architectural strengths.

### **3.1 Qwen 2.5: The Multilingual and Mathematical Apex**

The Qwen 2.5 series, particularly the 72B and 32B variants, represents a significant advancement in open-weights capability, often challenging proprietary models like GPT-4o.  
**Linguistic Capability**: Qwen 2.5 is trained on a massive, diverse corpus spanning over 29 languages.8 While the specific volume of Celtic tokens is not publicly disclosed, the model's architecture demonstrates superior cross-lingual transfer compared to Llama 3\. Benchmarks indicate that **Qwen 2.5 72B** consistently outperforms **Llama 3.3 70B** in multilingual tasks (MMLU multilingual subtasks, MGSM).8 This makes it the premier candidate for fine-tuning on Irish or Welsh datasets, as the pre-trained weights likely contain latent knowledge of European linguistic structures that can be rapidly activated.  
**Educational Reasoning**: For educational analytics, specifically in STEM, Qwen 2.5 is unrivaled among open models. The **Qwen2.5-Math** variants utilize specialized pre-training to achieve scores on the MATH benchmark (83.1% for 72B) that surpass even specialized closed models.8 Furthermore, the introduction of **QwQ-32B**, a reasoning-focused model utilizing reinforcement learning similar to DeepSeek-R1, provides a "thinking" model that fits on consumer hardware (24GB-48GB VRAM). QwQ-32B has shown parity with DeepSeek-R1 in mathematical reasoning (AIME 2024 score of \~79.5%), offering a powerful engine for automated grading systems that require step-by-step logic verification.12

### **3.2 DeepSeek-R1 and V3: The Reasoning Revolution**

DeepSeek has introduced a paradigm shift with its "Reasoning" models (R1), employing extensive Chain-of-Thought (CoT) training reinforced via Group Relative Policy Optimization (GRPO).14  
**Mechanism of Reasoning**: Unlike standard instruction-tuned models, DeepSeek-R1 generates a visible "thinking" process (encapsulated in \<think\> tags) before outputting a final answer. This internal monologue allows the model to self-correct, verify logic, and explore alternative solution paths. In an educational context, this is transformative. An R1-based tutor can not only correct a student's answer but also analyze the student's work to pinpoint the exact step where logic failed—be it a misapplied algebraic rule or a misunderstanding of the *tuiseal ginideach* (genitive case) in Irish grammar.16  
**Performance Metrics**: DeepSeek-R1 achieves a pass@1 score of 79.8% on the rigorous AIME 2024 math benchmark, rivaling OpenAI's o1 model. On the MATH-500 benchmark, it attains 97.3%, significantly outperforming standard dense models.18 However, R1's verbosity and tendency to output long reasoning traces make it slower and more expensive to run for simple tasks. Its application is best reserved for deep asynchronous analytics rather than real-time chat.

### **3.3 Llama 3.3 and 3.2: The Industrial Standard**

Meta’s Llama series remains the most robust ecosystem for general-purpose deployment.  
**Llama 3.3 70B**: This instruction-tuned model delivers performance comparable to the massive Llama 3.1 405B but is optimized for efficiency. It excels in instruction following (IFEval score 92.1) and general coding tasks.19 While its multilingual support is officially limited to 8 core languages (not including Celtic ones), its strong generalization capabilities make it a viable candidate for "teaching" Celtic languages via translation pairs, provided the fine-tuning dataset is sufficiently large.  
**Llama 3.2 Vision**: For multimodal educational assets, Llama 3.2 Vision (11B and 90B) is critical. Unsloth allows fine-tuning of the 11B model on a single Tesla T4 (16GB VRAM).21 This capability enables the creation of tools that can analyze handwritten student diagrams or generate textual descriptions of visual heritage assets (e.g., describing the Book of Kells in Irish) by fine-tuning on image-text pairs.

### **3.4 Gemma 3: The Hyper-Multilingual Contender**

Google’s **Gemma 3** (released 2025\) directly addresses the language gap. Unlike many models that treat non-English languages as an afterthought, Gemma 3 explicitly supports over 140 languages in its pre-training.23  
**Celtic Implications**: The explicit "140+" language support strongly suggests that Irish, Welsh, and possibly Scottish Gaelic are represented in the base model's vocabulary and embeddings to a significant degree. This reduces the need for extensive "vocabulary expansion" or heavy continuous pre-training. Gemma 3 27B offers a "sweet spot" for performance and deployability, fitting within 24GB VRAM when quantized, yet delivering strong multimodal capabilities (native image and text input).26

### **3.5 Phi-4: Efficiency via Synthetic Data**

Microsoft’s **Phi-4** (14B) demonstrates that high-quality synthetic data can allow smaller models to punch above their weight. Trained heavily on synthetic "textbook-quality" data, Phi-4 excels in reasoning benchmarks (MATH, GPQA), often outperforming Llama 3.1 70B in specific logic tasks.28  
**Deployment Scenario**: Phi-4 is the ideal candidate for "edge" educational analytics—software running locally on a teacher's laptop or a classroom tablet to grade assignments without sending student data to the cloud. Its small size allows for rapid fine-tuning and inference on modest hardware while maintaining high reasoning fidelity.

### **Summary of Model Suitability**

| Model Architecture | Parameter Size | Primary Strength | Celtic Viability | Educational Analytics | Asset Generation | Unsloth Support |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| **Qwen 2.5** | 72B / 32B | Multilingual Mastery | High (Broad language base) | Very High (Math/Coding) | High (OCR/VL variants) | Full (QLoRA) |
| **DeepSeek-R1** | 671B (MoE) / Distills | Advanced Reasoning | Medium (Needs Fine-tuning) | Excellent (Logic Tracing) | Low (Text-only) | Full (GRPO) |
| **Llama 3.3** | 70B | Instruction Following | Medium (English-centric) | High (General) | Medium | Full (Long Context) |
| **Gemma 3** | 27B / 12B | 140+ Languages | Very High (Native support) | High | High (Native Multimodal) | Full |
| **Phi-4** | 14B | Synthetic Efficiency | Low (English-centric) | High (Math/Logic) | Low | Full |
| **FLUX.2** | 12B+ | Image Synthesis | N/A | N/A | Excellent (Visuals) | N/A (LoRA only) |

## **4\. Dataset Compilation Strategy for Celtic Languages**

The primary bottleneck for Celtic LLMs is the scarcity of high-quality, digitized training data. While English models consume trillions of tokens, Celtic datasets often number in the mere millions. To bridge this gap, a strategy combining **source aggregation**, **synthetic generation**, and **rigorous formatting** is required.

### **4.1 Data Sourcing and Aggregation**

**Parallel Corpora**: The most high-value data for fine-tuning comes from parallel texts where sentences are aligned between English and the target Celtic language.

* *Legislative Records*: The Welsh Parliament (Senedd) and the Irish Oireachtas produce bilingual records mandated by law. These provide high-quality, formal register translations ideal for training grammatical accuracy.31  
* *Educational Resources*: Open Educational Resources (OER) and curriculum materials (e.g., from CCEA in Northern Ireland or CBAC in Wales) provide domain-specific terminology essential for educational fine-tuning.

**Cultural Archives**: Digitization of public domain literature (Project Gutenberg, National Libraries) captures the literary and idiomatic richness of the languages. However, older texts may use archaic spelling (e.g., pre-standardization Irish), necessitating a normalization preprocessing step to align with modern educational standards.

### **4.2 The "Cold Start" Synthetic Data Strategy**

DeepSeek’s research on R1 highlights the efficacy of **synthetic data** to bootstrap reasoning capabilities. This "Cold Start" method is directly applicable to low-resource languages.14  
**The Pipeline**:

1. **Reasoning Generation**: Use a strong reasoning model (DeepSeek-R1 or GPT-4o) to generate thousands of "Chain of Thought" (CoT) reasoning traces for math, logic, and grammar problems in English.  
2. **Translation & Adaptation**: Use a specialized translation model (e.g., NLLB or a fine-tuned Qwen) to translate these reasoning traces into Irish/Welsh. Crucially, the "thinking" steps must be translated to model the *internal logic* in the target language.  
3. **Verification**: Employ a "Teacher-Student" loop where a larger model verifies the translated logic, or use human-in-the-loop verification for a subset of data to ensure the specific mutations and syntax of Celtic languages are preserved.34

This method creates a synthetic "textbook" of reasoning in the target language, allowing the model to learn not just *what* the answer is, but *how* to think in Irish or Welsh.

### **4.3 Dataset Formatting Standards**

Unsloth supports specific JSONL formats that optimize training efficiency.

* **Alpaca Format**: Best for simple instruction/response pairs (e.g., translation, definitions).  
  JSON  
  {"instruction": "Translate the following into Welsh.", "input": "The cat sat on the mat.", "output": "Eisteddodd y gath ar y mat."}

* **ShareGPT Format**: Essential for multi-turn conversations and maintaining context in educational dialogues. Unsloth’s standardize\_sharegpt function can automatically convert varying formats into this standard.35  
  JSON  
  {"conversations": \[  
    {"from": "human", "value": "Ciamar a chanas mi 'Hello' ann an Gàidhlig?"},  
    {"from": "gpt", "value": "Is e 'Halò' a chanas tu."}  
  \]}

* **Reasoning Format**: For R1-style training, the dataset must separate the reasoning trace from the final answer.  
  JSON  
  {"instruction": "Solve for x...", "output": "\<think\>First, I will subtract 5...\</think\> The answer is 10."}

## **5\. Fine-Tuning Methodologies: Execution and Hyperparameters**

Fine-tuning for Celtic languages and educational analytics requires a nuanced approach to hyperparameters to prevent "catastrophic forgetting" (losing English reasoning) while instilling new linguistic capabilities.

### **5.1 The Unsloth Fine-Tuning Pipeline**

The Unsloth workflow leverages **QLoRA** (Quantized Low-Rank Adaptation) to update only a fraction of the model's parameters (adapters) while keeping the base model frozen in 4-bit precision.  
**Configuration Essentials**:

* **Target Modules**: It is critical to target *all* linear layers (q\_proj, k\_proj, v\_proj, o\_proj, gate\_proj, up\_proj, down\_proj). Targeting only attention heads (Q/V) is insufficient for learning new languages, as the MLP layers (gate/up/down) are believed to store factual knowledge and linguistic patterns.1  
* **Rank (r) and Alpha**: For learning a new language or complex reasoning, a higher rank is necessary. Set r=64 or r=128 (with lora\_alpha typically set to 2\*r, though Unsloth suggests alpha=r or standard values like 16 for stability). Low ranks (r=8) are insufficient for the complexity of Celtic morphology.21  
* **LoRA Dropout**: Set to 0 to maximize memory efficiency and deterministic training, as recommended by Unsloth documentation.1

### **5.2 Reasoning with GRPO (Group Relative Policy Optimization)**

For creating a "Celtic Reasoning" model, standard supervised fine-tuning (SFT) is often insufficient. Unsloth now supports **GRPO**, the reinforcement learning algorithm used for DeepSeek-R1.7  
**The GRPO Workflow**:

1. **Prompt**: Feed the model a question (e.g., a math problem in Welsh).  
2. **Generation**: The model generates a group of outputs (e.g., 4-8 different reasoning paths).  
3. **Reward Function**: A programmatic function evaluates the outputs. This could be a simple exact-match check for a math answer, or a more complex LLM-as-a-Judge check for grammatical correctness.  
4. **Optimization**: The model updates its policy to favor the reasoning paths that led to the correct answer. This incentivizes the model to develop its own internal verification strategies in the target language.39

**Hardware Requirement**: While R1 training originally required massive clusters, Unsloth’s GRPO implementation allows training **Llama 3.2 3B** or **Qwen 2.5 7B** on a single 16GB-24GB GPU, making it feasible for university researchers or EdTech startups.38

### **5.3 Vision Fine-Tuning for Educational Assets**

For multimodal assets (e.g., analyzing diagrams), fine-tuning **Llama 3.2 Vision** or **Qwen 2.5-VL** is required.

* **Unsloth Implementation**: Unsloth treats the vision encoder and the language model as separate but trainable entities. Users can choose to fine-tune finetune\_vision\_layers=True, finetune\_language\_layers=True, or both. For educational diagrams, fine-tuning *both* is recommended to align visual feature extraction with the specific pedagogical vocabulary.21  
* **VRAM Constraints**: Fine-tuning Llama 3.2 11B Vision requires approx. 16GB VRAM with Unsloth’s 4-bit quantization, fitting on a Tesla T4 (free Colab) or RTX 4060 Ti.22

## **6\. Educational Analytics: Benchmarking and Bias**

The deployment of LLMs in education necessitates rigorous evaluation beyond standard perplexity scores. Educational analytics models must be evaluated for pedagogical effectiveness and fairness.

### **6.1 Specialized Benchmarks**

* **StatEval**: A new benchmark specifically for statistical reasoning, which is crucial for data science education. It assesses an LLM's ability to reason under uncertainty, a capability often lacking in standard models. Fine-tuned models should be evaluated against StatEval to ensure they don't just calculate but *reason* statistically.34  
* **AIME and MATH**: For STEM education, performance on the AIME (American Invitational Mathematics Examination) and MATH benchmarks is the gold standard. DeepSeek-R1’s high scores here (79.8% AIME) validate its use as a math tutor. QwQ-32B’s similar performance makes it a cost-effective alternative.14

### **6.2 Bias Detection in Feedback**

Recent research highlights that LLMs can exhibit gender and cultural bias in educational feedback. For example, models might provide more autonomy-supportive feedback to users identified as male compared to female users.43

* **Embedding-Based Auditing**: A robust benchmarking framework involves generating feedback for identical student essays while toggling gender markers (names, pronouns). By analyzing the semantic distance between the resulting feedback embeddings, researchers can quantify bias. This step is mandatory before deploying any "Celtic Tutor" model to ensure it serves all learners equitably.

## **7\. Asset Generation: Visuals and Copyright**

Generating visual educational aids (diagrams, illustrations) requires navigating both technical capabilities and complex licensing landscapes.

### **7.1 FLUX for High-Fidelity Visuals**

**FLUX.2** (from Black Forest Labs) sets the standard for open-weights image generation.

* **Technical Merit**: It supports "multi-reference" generation, allowing a character (e.g., a mascot for a Welsh language course) to remain consistent across different images. Its text rendering capabilities are superior to Stable Diffusion 3, allowing it to generate diagrams with legible labels.45  
* **Licensing Constraint**: The "Dev" versions of FLUX.2 and FLUX.1 are released under a **Non-Commercial License**. This strictly prohibits their use for revenue-generating educational platforms. For commercial projects, one must acquire a commercial license or use the inferior but permissive **FLUX.1 \[schnell\]** (Apache 2.0).48

### **7.2 The OCR Verification Loop**

To ensure generated educational assets are accurate:

1. **Generate**: Use FLUX.2 to create a labeled diagram (e.g., "A diagram of a cell labeled in Irish").  
2. **Verify**: Pass the generated image to **Qwen 2.5-VL**, which currently holds the title for best open-source OCR (surpassing GPT-4o on some benchmarks).50  
3. **Iterate**: If Qwen detects misspelled labels, the system can automatically regenerate the image with refined prompts.

## **8\. Strategic Roadmap and Conclusions**

The convergence of efficient fine-tuning via Unsloth, the reasoning depth of DeepSeek-R1/Qwen, and the linguistic breadth of Gemma 3 provides a complete toolkit for revolutionizing Celtic educational technology.  
**Strategic Recommendations**:

1. **Adopt Gemma 3 27B** as the foundational base for Celtic language translation and general instruction, leveraging its 140+ language pre-training to minimize data requirements.  
2. **Utilize Qwen 2.5 72B** via Unsloth (4-bit) for high-end offline processing where maximum reasoning and multilingual context (128k) are required.  
3. **Implement "Distilled Reasoning"**: Do not deploy 671B models for students. Use DeepSeek-R1 to generate synthetic Celtic reasoning data, then fine-tune **Phi-4** or **Llama 3.2 3B** via Unsloth. This places a powerful, reasoning-capable tutor on local classroom hardware.  
4. **Establish a Bias Audit**: Integrate embedding-based bias detection into the CI/CD pipeline of any educational model to prevent the automation of pedagogical stereotypes.

By adhering to this technical and ethical framework, developers can build AI systems that not only preserve Celtic languages but elevate them to the forefront of educational innovation.

#### **Works cited**

1. Fine-tuning LLMs Guide | Unsloth Documentation, accessed December 13, 2025, [https://docs.unsloth.ai/get-started/fine-tuning-llms-guide](https://docs.unsloth.ai/get-started/fine-tuning-llms-guide)  
2. Unsloth: A Guide from Basics to Fine-Tuning Vision Models \- Learn OpenCV, accessed December 13, 2025, [https://learnopencv.com/unsloth-guide-efficient-llm-fine-tuning/](https://learnopencv.com/unsloth-guide-efficient-llm-fine-tuning/)  
3. Unsloth Benchmarks | Unsloth Documentation, accessed December 13, 2025, [https://docs.unsloth.ai/basics/unsloth-benchmarks](https://docs.unsloth.ai/basics/unsloth-benchmarks)  
4. Fine-tune Llama 3.3 with Unsloth, accessed December 13, 2025, [https://unsloth.ai/blog/llama3-3](https://unsloth.ai/blog/llama3-3)  
5. Unsloth Model Catalog, accessed December 13, 2025, [https://docs.unsloth.ai/get-started/unsloth-model-catalog](https://docs.unsloth.ai/get-started/unsloth-model-catalog)  
6. unslothai/unsloth: Fine-tuning & Reinforcement Learning for LLMs. Train OpenAI gpt-oss, DeepSeek-R1, Qwen3, Gemma 3, TTS 2x faster with 70% less VRAM. \- GitHub, accessed December 13, 2025, [https://github.com/unslothai/unsloth](https://github.com/unslothai/unsloth)  
7. Vision Reinforcement Learning (VLM RL) | Unsloth Documentation, accessed December 13, 2025, [https://docs.unsloth.ai/new/vision-reinforcement-learning-vlm-rl](https://docs.unsloth.ai/new/vision-reinforcement-learning-vlm-rl)  
8. Qwen 2.5 72b vs Llama 3.3 70b: Which Model Suits Your Needs? \- Novita AI Blog, accessed December 13, 2025, [https://blogs.novita.ai/qwen-2-5-72b-vs-llama-3-3-70b-which-model-suits-your-needs/](https://blogs.novita.ai/qwen-2-5-72b-vs-llama-3-3-70b-which-model-suits-your-needs/)  
9. Qwen/Qwen2.5-72B-Instruct \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/Qwen/Qwen2.5-72B-Instruct](https://huggingface.co/Qwen/Qwen2.5-72B-Instruct)  
10. Qwen2.5 72B Instruct: Pricing, Context Window, Benchmarks, and More \- LLM Stats, accessed December 13, 2025, [https://llm-stats.com/models/qwen-2.5-72b-instruct](https://llm-stats.com/models/qwen-2.5-72b-instruct)  
11. QwenLM/Qwen2.5-Math: A series of math-specific large language models of our Qwen2 series. \- GitHub, accessed December 13, 2025, [https://github.com/QwenLM/Qwen2.5-Math](https://github.com/QwenLM/Qwen2.5-Math)  
12. QwQ-32B vs DeepSeek-R1: Which AI Excels for Your Use Case? \- RiseUnion, accessed December 13, 2025, [https://www.theriseunion.com/blog/QwQ-32B-vs-DeepSeek-R1-32B.html](https://www.theriseunion.com/blog/QwQ-32B-vs-DeepSeek-R1-32B.html)  
13. QwQ-32B vs DeepSeek-R1 Ultimate 2025 Local Inference Showdown \- Skywork ai, accessed December 13, 2025, [https://skywork.ai/blog/llm/qwq-32b-vs-deepseek-r1-ultimate-2025-local-inference-showdown/](https://skywork.ai/blog/llm/qwq-32b-vs-deepseek-r1-ultimate-2025-local-inference-showdown/)  
14. DeepSeek-R1: Incentivizing Reasoning Capability in LLMs via Reinforcement Learning \- arXiv, accessed December 13, 2025, [https://arxiv.org/pdf/2501.12948](https://arxiv.org/pdf/2501.12948)  
15. deepseek-ai/DeepSeek-R1 \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/deepseek-ai/DeepSeek-R1](https://huggingface.co/deepseek-ai/DeepSeek-R1)  
16. DeepSeek-R1: The AI That Taught Itself to Think — And It’s Kind of Mind-Blowing, accessed December 13, 2025, [https://medium.com/@digitalconsumer777/deepseek-r1-the-ai-that-taught-itself-to-think-and-its-kind-of-mind-blowing-792c37f1ddf4](https://medium.com/@digitalconsumer777/deepseek-r1-the-ai-that-taught-itself-to-think-and-its-kind-of-mind-blowing-792c37f1ddf4)  
17. DeepSeek R1 Quickstart \- Together.ai Docs, accessed December 13, 2025, [https://docs.together.ai/docs/deepseek-r1](https://docs.together.ai/docs/deepseek-r1)  
18. DeepSeek-R1: Incentivizing Reasoning Capability in LLMs via Reinforcement Learning, accessed December 13, 2025, [https://arxiv.org/html/2501.12948v1](https://arxiv.org/html/2501.12948v1)  
19. What You Need to Know About Meta Llama 3.3 70B \- Hyperstack, accessed December 13, 2025, [https://www.hyperstack.cloud/blog/thought-leadership/what-is-meta-llama-3-3-70b-features-use-cases-more](https://www.hyperstack.cloud/blog/thought-leadership/what-is-meta-llama-3-3-70b-features-use-cases-more)  
20. unsloth/Llama-3.3-70B-Instruct \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/unsloth/Llama-3.3-70B-Instruct](https://huggingface.co/unsloth/Llama-3.3-70B-Instruct)  
21. Fine-Tuning Llama 3.2 Vision \- DataCamp, accessed December 13, 2025, [https://www.datacamp.com/tutorial/fine-tuning-llama-3-2-vision](https://www.datacamp.com/tutorial/fine-tuning-llama-3-2-vision)  
22. unsloth/Llama-3.2-11B-Vision-unsloth-bnb-4bit \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/unsloth/Llama-3.2-11B-Vision-unsloth-bnb-4bit](https://huggingface.co/unsloth/Llama-3.2-11B-Vision-unsloth-bnb-4bit)  
23. Google models | Generative AI on Vertex AI, accessed December 13, 2025, [https://docs.cloud.google.com/vertex-ai/generative-ai/docs/models](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/models)  
24. Gemma 3: Google's Latest Lightweight AI Model, Challenging Llama 3 and DeepSeek-V3, accessed December 13, 2025, [https://ikala.ai/blog/ai-trends/gemma-3-intro\_en/](https://ikala.ai/blog/ai-trends/gemma-3-intro_en/)  
25. Introducing Gemma 3: The most capable model you can run on a single GPU or TPU, accessed December 13, 2025, [https://blog.google/technology/developers/gemma-3/](https://blog.google/technology/developers/gemma-3/)  
26. Notes on Google's Gemma 3 \- Simon Willison's Weblog, accessed December 13, 2025, [https://simonwillison.net/2025/Mar/12/gemma-3/](https://simonwillison.net/2025/Mar/12/gemma-3/)  
27. What Is Gemma 3? Google's Open-Weight AI Model \- Vapi AI Blog, accessed December 13, 2025, [https://vapi.ai/blog/what-is-gemma-3](https://vapi.ai/blog/what-is-gemma-3)  
28. Microsoft's Phi-4: Step-by-Step Tutorial With Demo Project | DataCamp, accessed December 13, 2025, [https://www.datacamp.com/tutorial/phi-4-microsoft](https://www.datacamp.com/tutorial/phi-4-microsoft)  
29. Phi-4 Technical Report \- arXiv, accessed December 13, 2025, [https://arxiv.org/html/2412.08905v1](https://arxiv.org/html/2412.08905v1)  
30. Microsoft phi-4: The best smallest LLM | by Mehul Gupta | Data Science in Your Pocket, accessed December 13, 2025, [https://medium.com/data-science-in-your-pocket/microsoft-phi-4-the-best-smallest-llm-1cbaa5706e9e](https://medium.com/data-science-in-your-pocket/microsoft-phi-4-the-best-smallest-llm-1cbaa5706e9e)  
31. Training Data preparation for Customizing LLMs | by Sulbha Jain \- Medium, accessed December 13, 2025, [https://medium.com/@sulbha.jindal/training-data-preparation-for-customizing-llms-e19c1e7bdcfe](https://medium.com/@sulbha.jindal/training-data-preparation-for-customizing-llms-e19c1e7bdcfe)  
32. Fine-tune Deepseek-R1 with a Synthetic Reasoning Dataset \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/blog/sdiazlor/fine-tune-deepseek-with-a-synthetic-reasoning-data](https://huggingface.co/blog/sdiazlor/fine-tune-deepseek-with-a-synthetic-reasoning-data)  
33. Using DeepSeek R1 for Distributed Synthetic Data Generation (2 Million Samples) \- Reddit, accessed December 13, 2025, [https://www.reddit.com/r/singularity/comments/1ijngi1/synthetic1\_using\_deepseek\_r1\_for\_distributed/](https://www.reddit.com/r/singularity/comments/1ijngi1/synthetic1_using_deepseek_r1_for_distributed/)  
34. StatEval: A Comprehensive Benchmark for Large Language Models in Statistics \- arXiv, accessed December 13, 2025, [https://arxiv.org/html/2510.09517v1](https://arxiv.org/html/2510.09517v1)  
35. Datasets Guide | Unsloth Documentation, accessed December 13, 2025, [https://docs.unsloth.ai/get-started/fine-tuning-llms-guide/datasets-guide](https://docs.unsloth.ai/get-started/fine-tuning-llms-guide/datasets-guide)  
36. Chat Templates | Unsloth Documentation, accessed December 13, 2025, [https://docs.unsloth.ai/basics/chat-templates](https://docs.unsloth.ai/basics/chat-templates)  
37. Tutorial: How to Fine-tune gpt-oss | Unsloth Documentation, accessed December 13, 2025, [https://docs.unsloth.ai/models/gpt-oss-how-to-run-and-fine-tune/tutorial-how-to-fine-tune-gpt-oss](https://docs.unsloth.ai/models/gpt-oss-how-to-run-and-fine-tune/tutorial-how-to-fine-tune-gpt-oss)  
38. Tutorial: Train your own Reasoning model with GRPO | Unsloth Documentation, accessed December 13, 2025, [https://docs.unsloth.ai/get-started/reinforcement-learning-rl-guide/tutorial-train-your-own-reasoning-model-with-grpo](https://docs.unsloth.ai/get-started/reinforcement-learning-rl-guide/tutorial-train-your-own-reasoning-model-with-grpo)  
39. DeepSeek-R1 incentivizes reasoning in LLMs through reinforcement learning, accessed December 13, 2025, [https://www.reddit.com/r/singularity/comments/1nk43b1/deepseekr1\_incentivizes\_reasoning\_in\_llms\_through/](https://www.reddit.com/r/singularity/comments/1nk43b1/deepseekr1_incentivizes_reasoning_in_llms_through/)  
40. DeepSeek-R1 \- GitHub, accessed December 13, 2025, [https://github.com/deepseek-ai/DeepSeek-R1](https://github.com/deepseek-ai/DeepSeek-R1)  
41. Qwen2.5\_VL\_(7B)-Vision.ipynb \- Colab, accessed December 13, 2025, [https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Qwen2.5\_VL\_(7B)-Vision.ipynb](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Qwen2.5_VL_\(7B\)-Vision.ipynb)  
42. Llama 3.2 Vision finetuning now in Unsloth \<16GB VRAM & 2x faster Colab \- Reddit, accessed December 13, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1gwoqm9/llama\_32\_vision\_finetuning\_now\_in\_unsloth\_16gb/](https://www.reddit.com/r/LocalLLaMA/comments/1gwoqm9/llama_32_vision_finetuning_now_in_unsloth_16gb/)  
43. Benchmarking Educational LLMs with Analytics: A Case Study on Gender Bias in Feedback, accessed December 13, 2025, [https://arxiv.org/html/2511.08225v1](https://arxiv.org/html/2511.08225v1)  
44. Benchmarking Educational LLMs with Analytics: A Case Study on Gender Bias in Feedback, accessed December 13, 2025, [https://www.researchgate.net/publication/397521921\_Benchmarking\_Educational\_LLMs\_with\_Analytics\_A\_Case\_Study\_on\_Gender\_Bias\_in\_Feedback](https://www.researchgate.net/publication/397521921_Benchmarking_Educational_LLMs_with_Analytics_A_Case_Study_on_Gender_Bias_in_Feedback)  
45. FLUX.2 Image Generation Models Now Released, Optimized for NVIDIA RTX GPUs, accessed December 13, 2025, [https://blogs.nvidia.com/blog/rtx-ai-garage-flux-2-comfyui/](https://blogs.nvidia.com/blog/rtx-ai-garage-flux-2-comfyui/)  
46. FLUX.2 | Black Forest Labs, accessed December 13, 2025, [https://bfl.ai/flux2](https://bfl.ai/flux2)  
47. FLUX 2.0 Is Finally Here, accessed December 13, 2025, [https://flux2.io/flux-2-0-is-finally-here/](https://flux2.io/flux-2-0-is-finally-here/)  
48. LICENSE.txt · black-forest-labs/FLUX.2-dev at main \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/black-forest-labs/FLUX.2-dev/blob/main/LICENSE.txt](https://huggingface.co/black-forest-labs/FLUX.2-dev/blob/main/LICENSE.txt)  
49. Licensing | Black Forest Labs, accessed December 13, 2025, [https://bfl.ai/licensing](https://bfl.ai/licensing)  
50. Qwen-2.5-72b is now the best open source OCR model : r/LocalLLaMA \- Reddit, accessed December 13, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1jm4agx/qwen2572b\_is\_now\_the\_best\_open\_source\_ocr\_model/](https://www.reddit.com/r/LocalLLaMA/comments/1jm4agx/qwen2572b_is_now_the_best_open_source_ocr_model/)  
51. unsloth/Qwen2.5-VL-7B-Instruct \- Hugging Face, accessed December 13, 2025, [https://huggingface.co/unsloth/Qwen2.5-VL-7B-Instruct](https://huggingface.co/unsloth/Qwen2.5-VL-7B-Instruct)