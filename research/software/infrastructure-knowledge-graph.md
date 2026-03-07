# Theme: Knowledge Graph Infrastructure & EdTech Backend

## Analysis of Similar Documents

This report compares `taighde_bonneagar/00-infra-overview/ARCHITECTURE.md`, `taighde_teanga/ARCHITECTURE_ANALYSIS.md`, and `taighde_teanga/Backend Strategy For Educational Tutoring System.md`.

### Core Focus
- **Infrastructure ARCHITECTURE.md:** The "Ground" layer. Focuses on Dagger (CI/CD), Komodo (Deployment), Pangolin (Zero-Trust), and 1Password (Secrets). It provides the platform for all other services.
- **DuckLake ARCHITECTURE_ANALYSIS.md:** The "Data Lakehouse" layer. Focuses on the "DuckLake Unified Platform" using DuckDB, Iceberg REST (Lakekeeper), Lance Namespace, and SQLMesh. It manages the long-term storage and analytical processing of extracted research and student data.
- **EdTech Backend Strategy:** The "Application" layer. Focuses on the "Bilingual Temporal Knowledge Graph" using Cognee (Ontology), Graphiti (Temporal Logic), and FalkorDB (Persistence). It models the Irish math curriculum hierarchy and student mastery.

### Comparison Matrix

| Feature | Infra Architecture | DuckLake Platform | EdTech Backend |
|---------|-------------------|-------------------|----------------|
| **Core Storage** | Docker Volumes / Registry | S3 (MinIO/R2) / Parquet / Lance | FalkorDB / Graphiti / Cognee |
| **Data Processing** | Dagger Pipelines | SQLMesh / dbt / Ibis | Cocoindex / BAML |
| **Logic Layer** | Komodo Orchestration | Lakekeeper (Iceberg) | Cognee (OWL) / Graphiti (Temporal) |
| **State** | Ansible / Pulumi | SQLMesh State | Graphiti Episodic Memory |

### Synthesis and Synergy

The relationship between these three documents defines a 3-tier architecture:
1. **Platform (Infra):** Provides the Docker environment and secure tunnels for all services.
2. **Lakehouse (DuckLake):** Acts as the source of truth for raw extracted data (from OCR/VLM) and historical logs.
3. **Application (EdTech Backend):** Consumes structured data from the Lakehouse to build the highly relational Knowledge Graph for real-time tutoring.

**Proposed Unified Strategy:**
- **Centralize Identity:** Use **1Password** as the single source of secrets for all layers (from R2 keys to FalkorDB passwords).
- **Streamline Dataflow:** Use **Cocoindex** to ingest raw PDF extracts into the **DuckLake (Parquet/Iceberg)**, and then use a secondary Cocoindex flow to populate the **FalkorDB/Cognee** graph.
- **Temporal Synchronization:** Ensure **Graphiti's** temporal markers align with **SQLMesh's** incremental snapshots to allow "Time Travel" across both the application and the analytical warehouse.
