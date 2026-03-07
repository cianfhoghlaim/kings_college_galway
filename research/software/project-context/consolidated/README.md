# Organized Research Documentation

This directory consolidates approximately 40 research documents into a structured hierarchy of 6 thematic categories. The reorganization eliminates duplication while preserving technical depth and cross-references.

---

## Categories Overview

| # | Category | Focus | Documents |
|---|----------|-------|-----------|
| 01 | [Irish EdTech Platform](./01-irish-edtech-platform/) | Leaving Certificate tutoring system | 4 |
| 02 | [Multimodal Document Intelligence](./02-multimodal-document-intelligence/) | OCR, VLM, document processing | 4 |
| 03 | [AI-Native Data Pipelines](./03-ai-native-data-pipelines/) | Dagster, dlt, BAML, lakehouse | 5 |
| 04 | [Web Automation & Archival](./04-web-automation-archival/) | Scraping, anti-bot, Irish archives | 4 |
| 05 | [Knowledge Graph Infrastructure](./05-knowledge-graph-infrastructure/) | Graphiti, Cognee, visualization | 4 |
| 06 | [Platform Engineering](./06-platform-engineering/) | Docker, Komodo, deployment | 4 |

---

## 01-irish-edtech-platform/

Domain-specific architecture for an Irish educational technology platform targeting Leaving Certificate students.

| Document | Description |
|----------|-------------|
| `README.md` | Category overview and domain model |
| `frontend-stack.md` | TanStack Start, React, tRPC patterns |
| `data-architecture.md` | Curriculum data models, CAO points system |
| `subject-implementations.md` | Subject-specific features (Maths, Irish, etc.) |

**Key Technologies:** TanStack Start, tRPC, Pydantic, Temporal graphs

---

## 02-multimodal-document-intelligence/

Processing pipeline for extracting structured data from educational documents including PDFs, images, and handwritten content.

| Document | Description |
|----------|-------------|
| `README.md` | Category overview and pipeline architecture |
| `ocr-vlm-stack.md` | Vision model comparison, OCR strategies |
| `handwriting-recognition.md` | HTR models, dataset creation |
| `orchestration-infrastructure.md` | Model serving, batching, caching |
| `apple-silicon-deployment.md` | MLX optimization, local inference |

**Key Technologies:** Qwen2.5-VL, olmOCR, ColPali, MLX, llama.cpp

---

## 03-ai-native-data-pipelines/

Data engineering patterns for AI workloads including schema-first extraction, orchestration, and real-time lakehouse architecture.

| Document | Description |
|----------|-------------|
| `README.md` | Category overview and pipeline patterns |
| `baml-dlt-integration.md` | Schema-Aligned Parsing, Pydantic inference |
| `dagster-orchestration.md` | Asset-based workflows, CocoIndex integration |
| `metadata-control-plane.md` | DuckDB-backed dynamic source management |
| `lakehouse-architecture.md` | OLake, Lakekeeper, RisingWave stack |

**Key Technologies:** BAML, dlt, Dagster, DuckDB, OLake, Lakekeeper, RisingWave

---

## 04-web-automation-archival/

Autonomous web scraping architectures with anti-bot evasion and domain-specific Irish educational archive workflows.

| Document | Description |
|----------|-------------|
| `README.md` | Category overview and tool selection matrix |
| `stealth-browser-stack.md` | Patchright, CDP, Cloudflare bypass |
| `agentic-scraping-architecture.md` | Skyvern, Stagehand, Crawl4AI patterns |
| `irish-archives-workflow.md` | examinations.ie, canuint.ie, duchas.ie |

**Key Technologies:** Patchright, Crawl4AI, Stagehand, Skyvern, MCP

---

## 05-knowledge-graph-infrastructure/

Temporal knowledge graphs and semantic memory systems for AI agents with visualization patterns.

| Document | Description |
|----------|-------------|
| `README.md` | Category overview and bi-temporal modeling |
| `graphiti-temporal-graphs.md` | Episode architecture, time-travel queries |
| `cognee-entity-resolution.md` | Entity merging, ontology mapping |
| `graph-visualization.md` | react-force-graph, temporal UI components |

**Key Technologies:** Graphiti, Cognee, FalkorDB, Neo4j, react-force-graph

---

## 06-platform-engineering/

Deployment infrastructure patterns including containerization, orchestration, and Apple Silicon optimization.

| Document | Description |
|----------|-------------|
| `README.md` | Category overview and service topology |
| `docker-compose-patterns.md` | Multi-service stacks, networking, volumes |
| `komodo-deployment.md` | Core-Periphery architecture, Ansible automation |
| `apple-silicon-deployment.md` | MLX, llama.cpp, launchd services |

**Key Technologies:** Docker Compose, LiteLLM, Komodo, Pangolin, MLX

---

## Cross-Category Dependencies

```
┌──────────────────────────────────────────────────────────────────┐
│                    01-IRISH-EDTECH-PLATFORM                       │
│                    (Domain Requirements)                          │
└──────────────────────────────────────────────────────────────────┘
                              ↓
    ┌─────────────────────────┼─────────────────────────┐
    ↓                         ↓                         ↓
┌──────────┐           ┌──────────┐           ┌──────────┐
│    02    │           │    04    │           │    05    │
│   OCR    │           │ SCRAPING │           │  GRAPHS  │
│   VLM    │           │ ARCHIVAL │           │ TEMPORAL │
└──────────┘           └──────────┘           └──────────┘
    ↓                         ↓                         ↓
    └─────────────────────────┼─────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                    03-AI-NATIVE-DATA-PIPELINES                    │
│              (Orchestration & Data Movement)                      │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                    06-PLATFORM-ENGINEERING                        │
│              (Infrastructure & Deployment)                        │
└──────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference

### Starting Points by Goal

| Goal | Start With |
|------|------------|
| **Build educational app** | 01 → 02 → 03 |
| **Set up scraping pipeline** | 04 → 03 → 06 |
| **Implement AI memory** | 05 → 03 → 06 |
| **Deploy local LLMs** | 06 (apple-silicon) → 02 |
| **Process documents** | 02 → 03 → 05 |

### Technology Index

| Technology | Primary Category | Documents |
|------------|-----------------|-----------|
| BAML | 03 | `baml-dlt-integration.md` |
| Cognee | 05 | `cognee-entity-resolution.md` |
| CocoIndex | 03 | `dagster-orchestration.md` |
| Crawl4AI | 04 | `agentic-scraping-architecture.md` |
| Dagster | 03 | `dagster-orchestration.md` |
| dlt | 03 | `baml-dlt-integration.md` |
| Docker Compose | 06 | `docker-compose-patterns.md` |
| DuckDB | 03 | `metadata-control-plane.md` |
| FalkorDB | 05 | `graphiti-temporal-graphs.md` |
| Graphiti | 05 | `graphiti-temporal-graphs.md` |
| Komodo | 06 | `komodo-deployment.md` |
| Lakekeeper | 03 | `lakehouse-architecture.md` |
| LiteLLM | 06 | `docker-compose-patterns.md` |
| llama.cpp | 06 | `apple-silicon-deployment.md` |
| MLX | 02, 06 | `apple-silicon-deployment.md` |
| OLake | 03 | `lakehouse-architecture.md` |
| Pangolin | 06 | `komodo-deployment.md` |
| Patchright | 04 | `stealth-browser-stack.md` |
| Qwen2.5-VL | 02 | `ocr-vlm-stack.md` |
| RisingWave | 03 | `lakehouse-architecture.md` |
| Skyvern | 04 | `agentic-scraping-architecture.md` |
| Stagehand | 04 | `agentic-scraping-architecture.md` |
| TanStack | 01 | `frontend-stack.md` |

---

## Source File Mapping

The following original research files were consolidated:

### Category 01
- `Building Bilingual EdTech Platform.md`
- `Leaving Certificate Material App.md`
- `Leaving Certificate Subject Analysis Plan.md`
- `BAML Schemas for Irish Education.md`
- `Backend Strategy For Educational Tutoring System.md`

### Category 02
- `LLM and OCR Deployment Research.md`
- `Handwriting Recognition and Dataset Creation.md`
- `Local macOS MLX_MPS LLM Workflow.md`
- `Setting Up Local LLM Services on Mac.md`

### Category 03
- `BAML, DLT, and AI Workflow Integration.md`
- `Dagster Orchestration for Cocoindex, Graphiti.md`
- `Managing Diverse Data Sources for Pipelines.md`
- `Integrating Olake, Lakekeeper, RisingWave.md`
- `BAML, Graphiti, Tanstack AI Pipeline.md`

### Category 04
- `Open-Source Crawl4ai Anti-Bot Stack.md`
- `Integrating Skyvern with Crawl4AI_Stagehand.md`
- `Open-Source Web Scraping Architecture Analysis.md`
- `Celtic Data Scraping and Integration Plan.md`
- `Unified Scraping Swarm Stack Optimization.md`
- `Scraping Irish Audio Files.md`

### Category 05
- `Visualizing Cognee and Graphiti Graphs.md`
- `Ontology and Temporal Graphs Research.md`
- `Graph Tech Integration and Recommendation.md`

### Category 06
- `Komodo Deployment Technical Outline.md`
- `Pigsty, Mathesar, Komodo Deployment Outline.md`
- `Enhancing Monorepo Ansible Workflow.md`
- `Portfolio Tech Stack & Cloudflare R2.md`
- `Integrating TanStack AI with LiteLLM.md`

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- Deploy infrastructure stack (Category 06)
- Set up Dagster orchestration (Category 03)
- Configure local LLM serving (Category 06)

### Phase 2: Data Acquisition (Week 3-4)
- Implement scraping workflows (Category 04)
- Build document processing pipeline (Category 02)
- Configure metadata control plane (Category 03)

### Phase 3: Knowledge Layer (Week 5-6)
- Deploy knowledge graph (Category 05)
- Implement entity resolution (Category 05)
- Build temporal query interface (Category 05)

### Phase 4: Application (Week 7-8)
- Build frontend application (Category 01)
- Integrate all pipeline components
- Deploy production environment

---

## Maintenance

### Adding New Research
1. Identify primary category
2. Check for cross-category implications
3. Update relevant documents
4. Update this index if new technologies introduced

### Document Updates
- Each document includes a "References" section with source links
- Implementation priorities sections guide development order
- Code examples are production-ready patterns (not pseudocode)
