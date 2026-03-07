# Irish EdTech Platform Architecture

This directory consolidates research on building a comprehensive bilingual (Irish/English) educational technology platform for the Irish Leaving Certificate curriculum.

## Overview

The Irish education system presents a complex data landscape with three distinct governance structures:
- **NCCA** (National Council for Curriculum and Assessment): Defines pedagogical intent via curriculumonline.ie
- **SEC** (State Examinations Commission): Provides evidentiary truth via examinations.ie
- **Department of Education**: Manages temporal governance via circulars

## Documents in this Category

### Core Architecture Documents

| Document | Focus | Key Technologies |
|----------|-------|------------------|
| `data-architecture.md` | Knowledge graphs, ontologies, BAML schemas | FalkorDB, Cognee, Graphiti, CocoIndex |
| `frontend-stack.md` | Edge-native UI, WebAssembly, visualizations | TanStack Start, Marimo, Cloudflare, Deck.gl |
| `ai-ml-pipeline.md` | Document processing, model fine-tuning, RAG | Qwen2.5-VL, ColPali, Unsloth, BAML |
| `subject-implementations.md` | Per-subject technical blueprints | BAML schemas, assessment logic |

## Key Architectural Decisions

### 1. Schema-First Design
- BAML for type-safe LLM extraction
- Polymorphic schemas handling diverse content (prose, poetry, marking schemes)
- Unified concept nodes with dual-language properties

### 2. Temporal Knowledge Graphs
- Graphiti for bi-temporal data (valid time + transaction time)
- Policy supersession tracking for circulars
- Syllabus version management

### 3. Edge-Native Computing
- Browser-based computation via WebAssembly (Marimo, DuckDB)
- Cloudflare Workers for global distribution
- Durable Objects for session state

### 4. Bilingual Architecture
- Irish as first-class citizen in all schemas
- Dialectal variation support (Connacht, Munster, Ulster)
- UCCIX models for Irish language support

## Source Files Consolidated

This category merges content from:
- `BAML Schemas for Irish Education.md`
- `Building Bilingual EdTech Platform.md`
- `Backend Strategy For Educational Tutoring System.md`
- `Leaving Certificate Material App.md`
- `Leaving Certificate Subject Analysis Plan.md`
- `irish-english-education.md`
- `Educational Website Tech Stack.md`

## Quick Reference

### Curriculum Structure
```
Subject (e.g., Mathematics)
├── Cycle (Junior/Senior)
│   ├── Strand (e.g., Algebra)
│   │   ├── Topic (e.g., Equations)
│   │   │   └── Learning Outcome
│   │   └── Assessment Items
│   │       ├── Exam Questions
│   │       └── Marking Schemes (Scales 10A-D)
```

### Technology Stack Summary
```
Document Ingestion: ColPali → Qwen2.5-VL → Granite-Docling → BAML
Knowledge Base: FalkorDB + Qdrant (hybrid vector/graph)
RAG Retrieval: BGE-M3 embeddings + ColPali visual retrieval
Generation: Qwen2.5-Math-7B (fine-tuned via Unsloth)
Frontend: TanStack Start + Marimo WASM + Cloudflare Edge
```

### Assessment Logic by Subject Group
| Subject Group | Assessment Model | Key Edge Types |
|--------------|------------------|----------------|
| Mathematics | Step-based (Scale 10C) | :PREREQUISITE, :ASSESSES |
| Sciences | Diagram + Taxonomy | :FLOWS_TO, :INTERACTS |
| Humanities | SRP Count + Argument | :CAUSED, :LOCATED_AT |
| Languages | PCLM Rubric | :EXPLORES, :TRANSLATES |
| Business | Exact Layout/Values | :DEBITS, :STRUCTURED_AS |
