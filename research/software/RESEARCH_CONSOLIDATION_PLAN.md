# Research Analysis & Centralization Plan

## Themes Identified

1. **Celtic Language AI Resources (Irish, Scottish Gaelic, Welsh, Manx)**
   - Status: Highly developed for Irish/Welsh, emerging for Scottish Gaelic.
   - Key Files: `taighde_teanga/CELTIC_LANGUAGES_AI_RESOURCES.md`, `taighde_teanga/irish_bilingual_dataset_research.md`.

2. **OCR and Vision-Language Models (VLM)**
   - Status: Focus on fine-tuning Qwen3-VL, FastVLM, and Docling for Irish/Gaelic documents.
   - Key Files: `taighde_meaisínfhoghlaim/ANALYSIS_SUMMARY.md`, `taighde_teanga/Finetuning Qwen3-VL for Gaelic OCR.md`.

3. **Infrastructure & Data Pipelines (DuckLake, Lakehouse)**
   - Status: Advanced unified architecture using DuckDB, Iceberg, Lance, and Dagster.
   - Key Files: `taighde_teanga/ARCHITECTURE_ANALYSIS.md`, `taighde_bonneagar/INDEX.md`.

4. **Educational Platform Development (Irish EdTech)**
   - Status: Strategy for BAML-driven syllabus extraction and agentic tutoring.
   - Key Files: `taighde_teanga/Agentic Education Platform Development.md`, `taighde_teanga/BAML for Syllabus-Driven Data Extraction.md`.

5. **Geospatial & Linguistic Data**
   - Status: Integrating DuckDB Spatial and Ibis for mapping and linguistics.
   - Key Files: `taighde_teanga/Geospatial Data Visualization with Ibis.md`.

## Similarity Analysis (Candidate Documents for Comparison)

- **VLM OCR:** `taighde_meaisínfhoghlaim/Open-Source VLMs For PDF Extraction.md` vs `taighde_teanga/Celtic Language OCR Resource Analysis.md`.
- **Infrastructure:** `taighde_bonneagar/00-infra-overview/ARCHITECTURE.md` vs `taighde_teanga/ARCHITECTURE_ANALYSIS.md`.
- **EdTech:** `taighde_teanga/Backend Strategy For Educational Tutoring System.md` vs `taighde_teanga/Agentic Education Platform Development.md`.

## Centralization Strategy (`taighde_new/`)

- Move and merge related documents into themed subdirectories or consolidated files.
- Create a master `README.md` and `INDEX.md` in `taighde_new/`.
- Use subagents to perform deeper extraction and comparison.
