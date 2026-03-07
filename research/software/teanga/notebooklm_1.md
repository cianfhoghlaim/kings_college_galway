### **Irish (Gaeilge)**
**Models (LLM & Small)**
*   **Qomhrá (2025):** An 8B parameter bilingual (Irish-English) LLM based on Qwen-3. It utilizes a complete pipeline of Continued Pre-Training (CPT), Instruction Tuning (IT), and Alignment from Human Preferences (AHP).
*   **UCCIX:** An open-source LLM based on Llama 2-13B. It uses a dynamic language adaptation framework that selectively trains specific layers (interface vs. reasoning) to prevent catastrophic forgetting of English while learning Irish.
*   **EuroLLM-22B-Instruct:** A multilingual 22B parameter model trained on 4 trillion tokens, explicitly supporting Irish among 35 languages.

**Datasets & Evals**
*   **Irish-BLiMP (2025):** The first benchmark for fine-grained linguistic competence, containing 1020 minimal pairs across 11 syntactic features. It revealed that even GPT-5 only achieves 73.5% accuracy compared to 90.1% for humans.
*   **LC2024:** A new benchmark derived from the Irish Leaving Certificate for evaluating mathematical reasoning.
*   **Qomhrá Datasets:** A 30K synthetic parallel instruction tuning dataset and a 1K human preference dataset generated via Gemini-2.5-Pro.
*   **National Corpus for Irish (2024):** A foundational large-scale text corpus managed by the Gaois research group.

**Speech & Tools**
*   **Fotheidil:** A web-based automatic transcription system (ASR) utilizing Semi-Supervised Learning (SSL) to improve acoustic models for underrepresented dialects (e.g., Ulster Irish).
*   **ABAIR:** Continues to provide synthetic voices and text-to-speech resources.

### **Welsh (Cymraeg)**
**Models**
*   **UK-LLM (Welsh):** A high-resource initiative using NVIDIA’s Nemotron foundation models (49B and 9B parameters). It utilized the Isambard-AI supercomputer to translate 30 million English entries into Welsh for training.

**Datasets & Evals**
*   **CorCenCC:** The National Corpus of Contemporary Welsh, containing 11 million words across spoken, written, and e-language contexts. It features a bespoke part-of-speech tagger (**CyTag**) and semantic tagger (**CySemTag**).
*   **FreeTxt:** A bilingual analysis toolkit for open-ended survey data, integrating CorCenCC’s tagging tools for non-expert users.

### **Scottish Gaelic (Gàidhlig)**
**Models & Techniques**
*   **ÈIST Project:** Focuses on ASR and text-to-speech. It is currently developing an interactive chatbot to engage speakers and generate essential training data.
*   **Cross-Lingual Text Expansion (XLTE):** A technique used to synthesize a training corpus for traditional narratives. It fine-tunes GPT-4o to expand English summaries into full Gaelic narratives, reducing perplexity by 57.2% compared to baselines.

**Datasets**
*   **ARCOSG & Tobar an Dualchais:** Key sources for annotated corpora and oral history recordings used to train ASR systems.

### **Breton, Cornish, & Manx**
*   **Brythonic Strategy:** Due to extreme data sparsity, researchers are proposing a unified "Brythonic" LLM approach for **Breton** and **Cornish**, exploiting their linguistic proximity (diverging only AD 800-1100) to create a shared base corpus for pre-training.
*   **Manx:** Remains acutely under-resourced, relying heavily on transfer learning from Irish and Scottish Gaelic.
*   **Breton Tools:** New parsing software is being developed to support machine translation.

### **Cross-Cutting Techniques & Infrastructure**
*   **Unsloth:** A fine-tuning framework crucial for Celtic LLMs (like Qomhrá). It manually derives backpropagation steps and uses handwritten GPU kernels to make training up to 30x faster with 90% less VRAM, allowing 8B+ models to be tuned on consumer hardware.
*   **English-Pivoted CoT:** A training method where the model is fine-tuned to "think" (generate Chain-of-Thought) in English but output the final response in the target low-resource language (e.g., Irish). This achieved up to 28.33% improvement in reasoning tasks.
*   **DR-LIB:** A new CLARIN Knowledge Centre established in late 2024 to centralize digital resources and advisory support for all languages in Ireland and Britain.

Given the success of the "English-Pivoted" reasoning technique for Irish, are you interested in how this might be applied to the proposed unified Breton-Cornish model?