# Theme: Document Intelligence & VLM Fine-Tuning

## Analysis of Similar Documents

This report compares `taighde_meaisínfhoghlaim/Open-Source VLMs For PDF Extraction.md` and `taighde_teanga/Celtic Language OCR Resource Analysis.md`.

### Core Focus
- **Open-Source VLMs For PDF Extraction:** Focuses on the "Semantic Frontier" of STEM extraction (math, diagrams) using a provider-agnostic hybrid pipeline (Docling for structure, Qwen3-VL for reasoning). It emphasizes a "Quota-Aware Router" to balance cloud (AWS/Azure) and local inference.
- **Celtic Language OCR Resource Analysis:** Focuses on the "Epistemological Shift" required for Celtic languages (Gaeilge, Welsh, etc.). It emphasizes fine-tuning Qwen-VL with philological competence using CLARIN-UK resources (Dúchas, eDIL, CorCenCC).

### Comparison Matrix

| Feature | PDF Extraction Report | Celtic OCR Analysis |
|---------|-----------------------|---------------------|
| **Primary Model** | Qwen3-VL / Docling / DeepSeek | Qwen3-VL (fine-tuned) |
| **Main Challenge** | STEM/Math flattening in Cloud OCR | Celtic orthography (fadas, mutations) |
| **Data Source** | Leaving Cert Math Papers / Syllabus | CLARIN-UK / Dúchas / eDIL |
| **Strategy** | Hybrid-Local Routing | Philological Fine-Tuning |
| **Tooling** | vLLM, Docker, Airflow | Unsloth, MLflow, Ragas |

### Synthesis and Synergy
There is significant synergy between these two tracks. The "STEM extraction" track provides the structural pipeline (routing tables to Docling, math to Qwen), while the "Celtic OCR" track provides the linguistic depth (fine-tuning for specific character sets).

**Proposed Unified Strategy:**
1. Use the **Quota-Aware Router** to identify document type (Tabular vs. STEM vs. Prose).
2. Route Irish/Celtic prose to the **Philologically Fine-Tuned Qwen-VL**.
3. Route complex tables (Syllabus/NCCA) to **IBM Granite-Docling**.
4. Use **Bilingual Consensus Checking** (from the STEM report) to verify accuracy across parallel Irish/English exam papers.
