Here is a reproduction plan leveraging Unsloth’s efficiency to recreate and potentially outperform the state-of-the-art (SOTA) for Celtic languages, broken down by language cluster and estimated cost.

### **Phase 1: The Goidelic Cluster (Irish, Scottish Gaelic, Manx)**
**Goal:** Recreate the **Qomhrá** (Irish) and **ÈIST** (Gaelic) pipelines, but merge them to exploit Goidelic morphological similarities.

*   **Best Unsloth Model:** **Unsloth/Qwen2.5-14B-Instruct** or **Llama-3.1-8B-Instruct**.
    *   *Why:* Qomhrá used Qwen; Unsloth’s Qwen implementation is highly optimized. 14B is the "Goldilocks" size—reasoning capabilities of larger models but fits on a single A100 with Unsloth.
*   **Datasets to Gather:**
    *   **Irish:** **CulturaX-ga** (Pre-training), **Qomhrá 30K** (Instruction Tuning - Synthetic), **Irish-BLiMP** (Evaluation).
    *   **Gaelic:** **ARCOSG** (Corpus), **Tobar an Dualchais** summaries (for XLTE).
*   **The Technique:**
    1.  **CPT (Continued Pre-Training):** Combine Irish and Gaelic corpora. Use Unsloth’s *padding-free* and *packing* features to maximize throughput.
    2.  **English-Pivoted CoT:** Fine-tune the model to output reasoning chains in English but final answers in Irish/Gaelic (proven to boost math/reasoning by ~28%).

**Cost Estimate (Irish + Gaelic CPT & IT)**
*   *Reference:* Qomhrá took ~88 GPU hours (2x H100s for 44 hrs).
*   *Unsloth Factor:* 2-3x speedup via kernel optimization + packing. Est. 35 GPU Hours.
*   *Hardware Recommendation:* **Nvidia H100 ($3.95/h)** for raw throughput.
*   **Estimated Cost:** 35 hrs $\times$ $3.95 = **~$138.25**

---

### **Phase 2: The Brythonic Cluster (Welsh, Breton, Cornish)**
**Goal:** Recreate **UK-LLM** (Welsh) but solve the data scarcity of Breton/Cornish by treating them as a single linguistic unit ("Brythonic Strategy").

*   **Best Unsloth Model:** **Unsloth/Llama-3.1-8B-Nemotron**.
    *   *Why:* UK-LLM used Nemotron. Unsloth supports the Llama architecture underlying Nemotron.
*   **Datasets to Gather:**
    *   **Welsh:** **CorCenCC** (11M words), **FreeTxt** data.
    *   **Breton/Cornish:** Wikipedia dumps, **An Drouizig** (Breton tools/lexicons).
*   **The Technique:**
    1.  **Synthetic Scaling (The "Welsh Blueprint"):** Use a strong teacher model (e.g., GPT-4o) to translate 1M+ instruction pairs from English to Welsh/Breton (mimicking the 30M UK-LLM dataset on a budget).
    2.  **Joint Fine-Tuning:** Train on Welsh (anchor) + Breton + Cornish simultaneously to force transfer learning.

**Cost Estimate (Brythonic Joint Tuning)**
*   *Workload:* Heavy on instruction tuning (SFT), lighter on CPT.
*   *Unsloth Factor:* Unsloth reduces VRAM by ~60%, allowing larger batch sizes.
*   *Hardware Recommendation:* **Nvidia A100 80GB ($2.50/h)**. Sufficient VRAM for long-context synthetic data.
*   **Estimated Cost:** 40 hrs $\times$ $2.50 = **~$100.00**

---

### **Phase 3: Centralized Evaluation & Reasoning**
**Goal:** Run **Irish-BLiMP**, **BritEval**, and **LC2024** (math) across all models to benchmark.

*   **Technique:** Use Unsloth’s fast inference (or export to GGUF/Ollama) to run benchmarks.
*   *Hardware Recommendation:* **Nvidia L4 ($0.80/h)**. Cheap and efficient for inference.
*   **Estimated Cost:** 20 hrs $\times$ $0.80 = **~$16.00**

---

### **Total Reproduction & Improvement Cost**

| Task | Recommended GPU | Hours (Est.) | Hourly Rate | Total Cost |
| :--- | :--- | :--- | :--- | :--- |
| **Goidelic CPT** (Irish/Gaelic) | Nvidia H100 | 35 | $3.95 | $138.25 |
| **Brythonic SFT** (Welsh/Breton) | Nvidia A100 80GB | 40 | $2.50 | $100.00 |
| **Evaluation** (All Languages) | Nvidia L4 | 20 | $0.80 | $16.00 |
| **Buffer** (Debugging/Failures) | Nvidia A100 40GB | 10 | $2.10 | $21.00 |
| **GRAND TOTAL** | | | | **~$275.25** |

### **Why Unsloth Changes the Game**
1.  **VRAM to Model Size:** Standard training for a 14B model usually requires A100s. Unsloth allows you to train a **Qwen 2.5 14B** on a single **A100 40GB ($2.10/h)** or even quantize to fit an **A10 ($1.10/h)** if you accept slight precision loss.
    *   *Budget Option:* Using A10s for everything would drop the cost to roughly **$100 total**, though training would take longer.
2.  **Data Efficiency:** Unsloth's **"Uncontaminated Packing"** removes padding tokens. Since Celtic datasets are often sparse and of varying lengths, this prevents computing on "empty" data, effectively making your dataset processed 2-5x faster.

### **Next Steps for You**
1.  **Download:** `unsloth` library and the **Qomhrá 30K** dataset (Hugging Face).
2.  **Rent:** An **H100** for 24 hours to blast through the Irish/Gaelic pre-training.
3.  **Rent:** An **A100** to fine-tune the Welsh/Brythonic models.
4.  **Result:** You will likely reproduce (and potentially beat due to newer base models like Llama 3.1/Qwen 2.5) the results of academic papers that used older architectures (Llama 2), all for under $300.