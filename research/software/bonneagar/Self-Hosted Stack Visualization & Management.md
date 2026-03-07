# **Architectural Convergence in Modern Self-Hosted Infrastructure: A Comprehensive Analysis of Visualization, Centralization, and Observability Strategies**

## **1\. Introduction: The Epistemology of the Modern Homelab**

The landscape of self-hosted infrastructure has undergone a radical transformation, evolving from disparate collections of shell scripts and virtual machines into sophisticated, cloud-native platforms that mirror enterprise-grade internal developer platforms (IDPs). The contemporary "homelab" or self-hosted engineering stack is no longer merely a hobbyist's playground but a complex ecosystem involving Infrastructure as Code (IaC), container orchestration, distributed observability, and specialized workflows for Large Language Models (LLM) engineering. This shift has introduced a critical challenge: fragmentation. As engineers adopt specialized "best-in-class" tools—**Pulumi** for declarative infrastructure, **Cloudflare** for edge networking, **Komodo** for container management, and **Docker Compose** for definition—the cognitive load required to maintain a mental model of the system increases exponentially.  
The core problem articulated in the inquiry is one of *coherence*. How does an architect map the "truth" of the infrastructure (defined in code) to a visual representation? Furthermore, how does one unify the operational telemetry from a fragmented stack—logs from **Dozzle**, metrics from **Beszel**, session replays from **Highlight.io**, traces from **Logfire**, and LLM analytics from **Langfuse**—into a "Single Pane of Glass"?  
This report provides an exhaustive analysis of these challenges, dissecting the architectural trade-offs between **Visual Aggregation** (centralizing user interfaces via dashboards like **Glance**) and **Data Aggregation** (centralizing telemetry via data lakes like **ClickHouse ClickStack**). It further explores the viability of "Do It Yourself" (DIY) backends for Pulumi, validating the feasibility of high-performance, low-cost IaC state management without SaaS dependencies. By synthesizing deep research into modern tooling, this document aims to provide a blueprint for constructing a unified, self-hosted engineering platform that is both visually comprehensible and operationally robust.

## ---

**2\. Infrastructure Documentation and Visualization Strategy**

The first imperative of any platform engineering initiative is to establish a dynamic, accurate map of the infrastructure. In a stack defined by **Pulumi** and **Docker Compose**, the "truth" resides in text files (YAML, TypeScript, Python). However, text is poor at conveying topology, dependency chains, and resource state to human operators. We must bridge the gap between *code definition* and *architectural visualization*.

### **2.1 The Pulumi DIY Architecture: Feasibility of Self-Hosted State**

The user specifically queries the viability of pulumi/diy-idp or the general "DIY" approach using a self-hosted S3 backend. This is a pivotal architectural decision. The standard Pulumi experience relies on the Pulumi Service (SaaS) for state management, history, and locking. Decoupling from this service requires a robust alternative to manage the "State"—the JSON file that maps declarative code to real-world resources.

#### **2.1.1 The Mechanics of the S3 State Backend**

Pulumi’s architecture is uniquely modular. The CLI engine communicates with a "Backend" interface, which can be satisfied by the managed SaaS *or* a "DIY" object store. This is not a "hack" or a workaround but a supported operational mode designed for air-gapped environments and privacy-conscious teams.1  
When configured for a DIY backend, Pulumi serializes the stack's state (resources, outputs, configuration, and secrets) into a comprehensive JSON checkpoint file. This file is then stored in an S3-compatible bucket (AWS S3, MinIO, Ceph, SeaweedFS). The implications of this architecture are profound:

* **Data Sovereignty**: The user retains absolute control over the infrastructure map. There is no external dependency; if the internet is severed, infrastructure operations can continue against the local or LAN-based object store.1  
* **Concurrency Control**: The Pulumi Service automatically handles locking to prevent two engineers from modifying the same stack simultaneously. In a DIY S3 backend, locking relies on the atomicity of the underlying object store or must be managed manually. However, modern S3-compatible systems like MinIO offer strong consistency guarantees that mitigate the risks of race conditions during state writes.2  
* **Project Scoping and Namespaces**: A historical criticism of the DIY backend was its flat namespace, which made managing complex organizations difficult. Recent updates have introduced **Project-Scoped Stacks** to the DIY backend.4 This critical feature enables a hierarchical organization structure (e.g., organization/project/stack), bringing the self-hosted experience into parity with the SaaS organizational model. Users can now architect their state buckets with the same logical separation used in enterprise environments.4

Architectural Configuration for Self-Hosted S3:  
To operationalize this, the Pulumi CLI must be explicitly directed to the self-hosted endpoint. This is achieved by bypassing the default login mechanisms and utilizing cloud-agnostic environment variables.

Bash

\# Configuration for MinIO/Self-Hosted S3  
export AWS\_ACCESS\_KEY\_ID=minio\_identity  
export AWS\_SECRET\_ACCESS\_KEY=minio\_secret  
export AWS\_REGION=us-east-1  \# Essential dummy region for SDK compatibility \[3\]  
export AWS\_ENDPOINT=http://minio.lan:9000

\# The Login Command  
\# s3ForcePathStyle is critical for self-hosted stores that do not use DNS buckets  
pulumi login "s3://pulumi-state-bucket?endpoint=http://minio.lan:9000\&s3ForcePathStyle=true"

This configuration establishes the S3 bucket as the "Source of Truth" for the infrastructure.

#### **2.1.2 Visualizing the DIY Stack: The Role of pulumi-ui**

The user referenced pulumi/diy-idp. Research indicates this likely refers to **mlops-club/pulumi-ui**, a community-driven project explicitly engineered to solve the "blindness" of the DIY backend.5  
While the Pulumi CLI allows for operational commands (up, destroy), it lacks a visual interface for exploring the state file. pulumi-ui acts as a visualization layer that sits on top of the S3 bucket.

* **Architecture**: It is a lightweight containerized application (React frontend, Python/Node backend) that authenticates against the same S3 bucket used by the CLI. It reads the JSON state files and reconstructs a visual dashboard.5  
* **Capabilities**:  
  * **Stack Visualization**: It lists all stacks found in the bucket, respecting the project hierarchies.  
  * **Resource Graphing**: It parses the dependency graph within the state file to show how resources relate (e.g., an S3 bucket used by a Lambda function).  
  * **Output Inspection**: It provides a clean interface to view stack outputs (URLs, IP addresses) without running CLI commands.  
* **Limitations**: It is primarily read-only and lacks the sophisticated Policy-as-Code enforcement (CrossGuard) and Role-Based Access Control (RBAC) of the paid SaaS.5 However, for a self-hosted homelab or small team, it effectively fills the role of an Infrastructure IDP.

**Conclusion on Question 1**: Yes, the S3 backend is fully functional and supports modern features like project scoping. The "DIY IDP" requirement is satisfied by deploying pulumi-ui alongside the S3 bucket, effectively creating a self-sovereign infrastructure portal.

### **2.2 Visualization of Container Architectures (Docker Compose)**

For the **Komodo**, **Pangolin**, and general **Docker Compose** layers, visualization requires parsing docker-compose.yml files, which define the runtime topology.

#### **2.2.1 Static Topology Generation**

Tools like **docker-compose-diagram** 6 and **docker-compose-viz** 7 provide automated generation of architectural diagrams. These utilities parse the YAML definitions to identify:

* **Services**: Nodes in the graph.  
* **Links/Depends\_on**: Directed edges representing startup order and networking dependencies.  
* **Volumes**: Storage nodes linked to services.  
* **Networks**: Subgraph clusters grouping services.

Integrating these tools into a CI/CD pipeline (e.g., GitHub Actions or a local Git hook) ensures that every commit to the infrastructure repository automatically generates an updated PNG/SVG of the architecture.8 This "Diagram-as-Code" approach guarantees that documentation never drifts from reality.

#### **2.2.2 Dynamic Management via Komodo**

**Komodo** itself 9 serves as a dynamic visualization layer. Unlike static diagrams, Komodo connects to the Docker socket (or remote Docker hosts) to visualize the *running* state of the stack. It acts as a specialized IDP for containers, offering:

* **Resource Utilization**: Real-time graphs of CPU/RAM per container.  
* **Log Streams**: Integrated console views.  
* **Deployment History**: Tracking changes to container images and configurations.

By utilizing Komodo, the user effectively covers the "Management" and "Operational Visualization" requirements for the container layer, complementing the "Structural Visualization" provided by pulumi-ui.

### **2.3 The "C4 Model" and Strategic Documentation**

To thoroughly "explain" software stacks as requested, raw resource graphs are often too granular. The **C4 Model** (Context, Containers, Components, Code) provides a hierarchical framework for documenting software architecture.11  
**Structurizr Lite** 13 is the recommended self-hosted tool for this. It allows the user to define the architecture using a Domain Specific Language (DSL) and renders interactive diagrams.

* **Integration**: The Structurizr DSL can be stored in the same Git repository as the Pulumi code.  
* **Visualization**: It provides a high-level "System Context" view (how Cloudflare interacts with Pangolin) that is often missing from the low-level resource graphs generated by Pulumi or Docker.  
* **Self-Hosting**: Structurizr Lite runs as a single Docker container, serving the diagrams locally, perfectly aligning with the user's self-hosted ethos.15

## ---

**3\. Visual Aggregation: The "Single Pane of Glass" Dashboard Strategy**

The second major requirement is centralizing the user interfaces of **Dozzle**, **Beszel**, **Highlight.io**, **Langfuse**, and **Logfire** into a unified dashboard like **Glance**. This strategy represents **Visual Aggregation**—creating a meta-interface that composes other interfaces.

### **3.1 The Dashboard Engine: Glance Architecture**

**Glance** is identified as a premier choice for this functionality due to its lightweight nature (Go binary) and focus on aggregation via widgets.16 Unlike static dashboards (like Homer) that merely link to services, Glance attempts to *embed* functionality directly into the dashboard.  
The primary mechanism for this deep integration is the **Iframe Widget**.17 By defining a widget type of iframe in the glance.yml configuration, users can render external web applications within the Glance grid.

YAML

\# Conceptual Glance Configuration  
widgets:  
  \- type: iframe  
    url: https://dozzle.internal  
    title: Container Logs  
  \- type: iframe  
    url: https://beszel.internal  
    title: System Metrics

### **3.2 The Technical Barrier: Browser Security Policies (X-Frame-Options & CSP)**

The integration of modern web applications into iframes is frequently obstructed by browser security standards designed to prevent Clickjacking.

* **X-Frame-Options**: This legacy HTTP header is widely used. Values of DENY or SAMEORIGIN instruct the browser to refuse rendering the page if it is embedded in a frame on a different domain.19  
* **Content-Security-Policy (CSP)**: The modern frame-ancestors directive provides more granular control but is equally restrictive by default. It specifies exactly which parents are allowed to embed the page.21

Tools like **Highlight.io** and **Langfuse**, being secure enterprise-grade applications, invariably set these headers to restrictive defaults. Consequently, a naive attempt to embed them in Glance will result in "refused to connect" errors within the widget.

#### **3.2.1 The Solution: Reverse Proxy Header Injection**

To bypass these restrictions in a trusted homelab environment, the user must implement an **Interception Layer** using a reverse proxy (Nginx, Traefik, or Caddy). The proxy must strip the blocking headers from the upstream application's response and inject permissive ones.  
**Nginx Configuration Strategy:**

Nginx

location /tool/ {  
    proxy\_pass http://upstream\_tool:8080/;  
      
    \# 1\. Strip the blocking legacy header  
    proxy\_hide\_header X-Frame-Options;  
      
    \# 2\. Strip the modern blocking directive (if present)  
    \# Note: Requires sophisticated regex replacement if CSP is complex  
      
    \# 3\. Inject a permissive CSP allowing the Glance dashboard  
    add\_header Content-Security-Policy "frame-ancestors 'self' https://glance.my-domain.com";  
}

**Warning**: This modification degrades the security posture of the embedded tools, making them theoretically vulnerable to UI redress attacks. It should strictly be limited to services accessed via a secure, authenticated internal network (VPN/WireGuard).21

### **3.3 Deep Analysis of Tool Integration into Glance**

#### **3.3.1 Dozzle (Container Logs)**

* **Function**: Real-time log streaming for Docker containers.23  
* **Integration Feasibility**: **High**. Dozzle is a lightweight, responsive UI ideal for embedding.  
* **Configuration**: Dozzle supports a \--base-url flag (e.g., /dozzle), which simplifies reverse proxy configuration.  
* **Auth Handling**: Dozzle supports "Forward Proxy Authentication".24 If the user employs an authentication gateway (like Authelia or Authentik) in front of Glance and Dozzle, the iframe can inherit the session seamlessly. Without this, the iframe will present a login screen, which is a poor user experience.

#### **3.3.2 Beszel (System Metrics)**

* **Function**: Lightweight resource monitoring (CPU, RAM, Disk, Docker stats) via a Hub/Agent architecture.25  
* **Integration Feasibility**: **High**. The Beszel Hub web interface is clean and responsive.  
* **Configuration**: The Beszel Hub is a single binary/container. Embedding it allows for "at a glance" traffic light monitoring of system health.  
* **Alternative**: Beszel exposes an API. An advanced Glance configuration could theoretically use a custom-api widget to fetch JSON metrics from Beszel and render a native Glance graph, avoiding the iframe overhead entirely.17 This yields a more cohesive UI but requires writing custom JavaScript/Go templates for Glance.

#### **3.3.3 Highlight.io (Full-Stack Observability)**

* **Function**: Session replay, error monitoring, and logging.27  
* **Integration Feasibility**: **Low/Medium**. Highlight.io is a "heavy" Single Page Application (SPA).  
* **Constraint**: The UI is information-dense, designed for a full 1080p+ viewport. Squeezing it into a dashboard widget renders it unusable.  
* **Recording vs. Viewing**: While Highlight *supports* recording sessions inside iframes (for the monitored app) 29, embedding the *Highlight Dashboard itself* is different.  
* **Recommendation**: Configure Highlight as a **link tile** in Glance rather than an embedded widget. Alternatively, embed only specific, simplified views if the Highlight UI permits deep linking to "kiosk mode" pages (though documentation suggests this is not a native feature).

#### **3.3.4 Langfuse (LLM Engineering)**

* **Function**: Tracing, evaluation, prompt management, and cost tracking for LLM apps.30  
* **Integration Feasibility**: **Low**. Langfuse is a complex platform.  
* **Dashboard Limitations**: While Langfuse offers "Custom Dashboards" internally 32, there is no documented feature to expose these dashboards via a public/shared link that bypasses authentication for embedding.33  
* **Security Friction**: Embedding Langfuse requires the user to maintain an active session cookie. If the session expires, the widget breaks. Given the sensitivity of LLM data (prompts/completions), Langfuse's strict security headers are difficult to bypass safely.

#### **3.3.5 Logfire (Structured Logging)**

* **Function**: SQL-queryable logging and tracing, optimized for Python/Pydantic.34  
* **Integration Feasibility**: **Medium**. Similar to Langfuse, it is a complex query interface.  
* **Self-Hosting**: Logfire can be self-hosted via Helm.35  
* **Utility**: Embedding a query interface is rarely useful in a "Glance" context. Dashboards are for *answers*, not *questions*. Unless Logfire allows saving a specific visualization (e.g., "Error Rate Last Hour") as a standalone embeddable view, it is better served as a linked tool.

### **3.4 The Verdict on Visual Aggregation**

While technically possible via "Iframe Hacking" (proxy header manipulation), centralizing these tools into Glance results in a **Portal of Portals**. This creates a disjointed experience:

* **Scrollbar Hell**: Multiple nested scrollbars (browser, dashboard, widget).  
* **Auth Fragmentation**: Widgets timing out independently.  
* **Visual Noise**: Inconsistent fonts, themes, and layouts across widgets.

Glance is best used as a **Launchpad** (bookmarks) and **Status Board** (using native widgets for simple up/down checks), rather than a container for complex applications.

## ---

**4\. Data Aggregation: The Unified Data Lake Strategy**

The user explicitly asks: "Is ClickHouse ClickStack a better centralized alternative?"  
This question marks a pivot from Visual Aggregation (combining UIs) to Data Aggregation (combining telemetry). The architectural consensus for advanced self-hosting is that Data Aggregation provides superior observability.

### **4.1 The Architecture of ClickStack**

**ClickStack** represents an open-source observability ecosystem centered around **ClickHouse** as the unified storage engine. It typically comprises three layers 36:

1. **Storage**: **ClickHouse**. A high-performance columnar OLAP database.  
2. **Ingestion**: **OpenTelemetry (OTel)**. The universal standard for collecting traces, metrics, and logs.  
3. **Visualization**: **HyperDX**. A unified UI (acquired/backed by ClickHouse principles) designed to correlate data types.

#### **4.1.1 Why ClickHouse?**

ClickHouse fundamentally changes the economics of observability.

* **Compression**: It achieves massive compression ratios (10-30x) using algorithms like LZ4 and ZSTD.39 This allows self-hosters to store terabytes of logs on relatively cheap storage (even S3-backed) without the massive RAM requirements of Elasticsearch (the traditional backend for Highlight.io/ELK).  
* **Performance**: ClickHouse processes analytical queries (aggregations) at sub-second speeds, enabling "Live Tail" and complex filtering over massive datasets.38

### **4.2 Comparative Analysis: ClickStack vs. The Fragmented Stack**

Does ClickStack replace the user's current toolset?

| Current Tool | Function | ClickStack / HyperDX Replacement Capability | Architectural Verdict |
| :---- | :---- | :---- | :---- |
| **Dozzle** | Docker Logs | **Strong Replacement**. An OTel collector (using filelog receiver) scrapes Docker JSON logs and pushes them to ClickHouse. HyperDX provides "Live Tail," full-text search (Lucene syntax), and alerting.36 | **Superior**. Dozzle is ephemeral (logs die with container); ClickStack offers retention and search. |
| **Beszel** | Host Metrics | **Strong Replacement**. The OTel collector (hostmetrics receiver) gathers CPU/RAM/Disk/Network stats. These are stored in ClickHouse metric tables (\_sum, \_gauge).41 HyperDX graphs these alongside logs. | **Superior**. Allows correlation (e.g., "Show logs when CPU \> 90%"). |
| **Highlight.io** | Session Replay | **Partial Replacement**. HyperDX has native Session Replay capabilities that link DOM events to backend traces.36 While Highlight.io offers deeper UX insights (heatmaps, funnels), HyperDX is sufficient for engineering debugging. | **Alternative**. Use ClickStack for debugging; keep Highlight if Marketing/Product teams need UX analytics. |
| **Logfire** | Tracing | **Direct Replacement**. Logfire is essentially a polished OTel wrapper. Sending OTel traces directly to ClickStack achieves the same visibility without a separate tool.43 | **Consolidated**. Removes a redundant tool. |
| **Langfuse** | LLM Ops | **Irreplaceable (mostly)**. While ClickStack can store LLM traces, it lacks the *domain-specific logic* of Langfuse: Prompt Playgrounds, Dataset Evaluation, and complex Token Cost calculations.31 | **Complementary**. Keep Langfuse for workflows; use ClickHouse as its backend. |

### **4.3 The "Better" Argument**

ClickStack is **better** because it solves the correlation problem.

* **The Scenario**: An LLM response is slow.  
  * *Fragmented Stack*: You check Langfuse for the trace. You check Dozzle for errors at that timestamp. You check Beszel to see if the host was overloaded.  
  * *ClickStack Scenario*: You open the trace in HyperDX. It automatically shows the associated backend logs (from the same trace\_id) and overlays the host metrics for that exact time window.

### **4.4 Implementing the Hybrid Architecture**

The optimal path is a **Hybrid ClickHouse-Centric Architecture**.

1. **Deploy ClickHouse**: Set up a single-node ClickHouse instance (or cluster). This becomes the "Gravity Well" for all data.  
2. **Instrument with OpenTelemetry**:  
   * Deploy **OTel Collectors** as agents on all Docker hosts. Configure them to scrape Docker logs and Host Metrics.41  
   * Configure **Komodo**, **Pangolin**, and application containers to send OTLP traces to the Collector.  
3. **Deploy HyperDX**: Connect it to ClickHouse. This replaces Dozzle and Beszel for *viewing* data.  
4. **Integrate Langfuse**:  
   * Keep Langfuse for its specialized LLM features (Prompt Engineering, Cost Tracking).  
   * **Crucially**, configure Langfuse to use **ClickHouse** as its analytical backend.45 Langfuse natively supports ClickHouse for high-volume trace storage, solving the scalability issues of its default Postgres backend. This unifies the storage layer even if the UIs remain separate.

## ---

**5\. Strategic Recommendations**

### **5.1 The Documentation Stack**

* **Backend**: Use **Pulumi DIY S3** with project-scoped stacks. This is enterprise-grade and cost-effective.  
* **Visualization**: Deploy **pulumi-ui** for dynamic state inspection. Use **Structurizr Lite** for high-level C4 architectural documentation.  
* **Automation**: Integrate pulumi stack graph and docker-compose-diagram into CI pipelines to generate static documentation artifacts automatically.

### **5.2 The Observability Stack**

* **Abandon Visual Aggregation**: Do not try to jam complex apps like Langfuse into Glance iframes. It creates a fragile, insecure user experience. Use Glance only as a "Launchpad" with simple up/down status checks.  
* **Adopt Data Aggregation**: Migrate to **ClickStack (ClickHouse \+ HyperDX)**.  
  * Replace **Dozzle** and **Beszel** (viewing layer) with HyperDX dashboards.  
  * Consolidate **Logfire** traces into HyperDX.  
  * Retain **Langfuse** but back it with the shared ClickHouse cluster to unify the data gravity.

### **5.3 Summary of the Unified Platform**

By adopting this architecture, the user transitions from a collection of tools to a cohesive **Internal Developer Platform**:

| Layer | Tool Selection | Role |
| :---- | :---- | :---- |
| **Interface** | **Glance** | Launchpad / Status Board (No complex embedding) |
| **Management** | **Komodo** | Container Lifecycle (Start/Stop/Update) |
| **Infrastructure** | **Pulumi \+ pulumi-ui** | Definition & State Visualization |
| **Data Lake** | **ClickHouse** | Central Storage for Logs, Metrics, Traces |
| **Observability** | **HyperDX** | "Single Pane of Glass" for Engineering Debugging |
| **Specialized AI** | **Langfuse** | LLM Prompt Management & Evaluation |

This architecture reduces the distinct UIs from \~6 to 3 core interfaces (Infrastructure, Observability, AI), creating a robust, scalable, and comprehensible self-hosted environment.  
---

**Table 1: Integration Compatibility Matrix for Glance (Visual Aggregation)**

| Tool | Integration Method | Security Barriers | Authentication Handling | Experience Rating |
| :---- | :---- | :---- | :---- | :---- |
| **Dozzle** | Iframe | Low (Configurable Base URL) | Forward Proxy / Basic Auth | ⭐⭐⭐⭐ (Good) |
| **Beszel** | Iframe | Low | Native / Proxy | ⭐⭐⭐⭐ (Good) |
| **Highlight.io** | Iframe | High (Strict CSP/X-Frame) | Token-based / Complex | ⭐⭐ (Poor \- UI too dense) |
| **Langfuse** | Iframe | High (Strict Security) | Session Cookies | ⭐ (Poor \- Security friction) |
| **Logfire** | Iframe | Medium | SaaS/Self-hosted Auth | ⭐⭐ (Mediocre) |

**Table 2: Data Aggregation Capabilities (ClickStack)**

| Data Type | Source Tool | Ingestion Method | Visualization in HyperDX | Value Add |
| :---- | :---- | :---- | :---- | :---- |
| **Container Logs** | Dozzle | OTel Filelog Receiver | Live Tail / Search | **Retention & Correlation** |
| **Host Metrics** | Beszel | OTel Hostmetrics Receiver | Time-series Graphs | **Unified Context** |
| **App Traces** | Logfire | OTel OTLP Exporter | Trace Waterfall | **Single DB Storage** |
| **LLM Traces** | Langfuse | OTel OTLP Exporter | Trace Waterfall | **Performance Analysis** |
| **Session Replay** | Highlight | HyperDX JS SDK | Replay Player | **Debug Integration** |

#### **Works cited**

1. Managing state & backend options \- Pulumi, accessed December 14, 2025, [https://www.pulumi.com/docs/iac/concepts/state-and-backends/](https://www.pulumi.com/docs/iac/concepts/state-and-backends/)  
2. Using OVHcloud Object Storage as Pulumi Backend to store your Pulumi state, accessed December 14, 2025, [https://help.ovhcloud.com/csm/en-public-cloud-compute-pulumi-high-perf-object-storage-backend-state?id=kb\_article\_view\&sysparm\_article=KB0062958](https://help.ovhcloud.com/csm/en-public-cloud-compute-pulumi-high-perf-object-storage-backend-state?id=kb_article_view&sysparm_article=KB0062958)  
3. Pulumi Cloud self-hosted API, accessed December 14, 2025, [https://www.pulumi.com/docs/administration/self-hosting/components/api/](https://www.pulumi.com/docs/administration/self-hosting/components/api/)  
4. Aligning Projects between Service and DIY Backend | Pulumi Blog, accessed December 14, 2025, [https://www.pulumi.com/blog/project-scoped-stacks-in-self-managed-backend/](https://www.pulumi.com/blog/project-scoped-stacks-in-self-managed-backend/)  
5. mlops-club/pulumi-ui: UI for visualizing self-hosted Pulumi ... \- GitHub, accessed December 14, 2025, [https://github.com/mlops-club/pulumi-ui](https://github.com/mlops-club/pulumi-ui)  
6. skonik/docker-compose-diagram \- GitHub, accessed December 14, 2025, [https://github.com/skonik/docker-compose-diagram](https://github.com/skonik/docker-compose-diagram)  
7. Creating block diagrams from docker-compose files \- DEV Community, accessed December 14, 2025, [https://dev.to/krishnakummar/creating-block-diagrams-from-docker-compose-files-7kf](https://dev.to/krishnakummar/creating-block-diagrams-from-docker-compose-files-7kf)  
8. Automatic Diagram Generation for Always-Accurate Diagrams | Pulumi Blog, accessed December 14, 2025, [https://www.pulumi.com/blog/automating-diagramming-in-your-ci-cd/](https://www.pulumi.com/blog/automating-diagramming-in-your-ci-cd/)  
9. 15 Docker Containers That Make Your Home Lab Instantly Better \- Virtualization Howto, accessed December 14, 2025, [https://www.virtualizationhowto.com/2025/11/15-docker-containers-that-make-your-home-lab-instantly-better/](https://www.virtualizationhowto.com/2025/11/15-docker-containers-that-make-your-home-lab-instantly-better/)  
10. Ultimate Home Lab Starter Stack for 2026 (Key Recommendations) \- Virtualization Howto, accessed December 14, 2025, [https://www.virtualizationhowto.com/2025/12/ultimate-home-lab-starter-stack-for-2026-key-recommendations/](https://www.virtualizationhowto.com/2025/12/ultimate-home-lab-starter-stack-for-2026-key-recommendations/)  
11. Top 9 tools for C4 model diagrams \- IcePanel, accessed December 14, 2025, [https://icepanel.io/blog/2025-08-28-top-9-tools-for-c4-model-diagrams](https://icepanel.io/blog/2025-08-28-top-9-tools-for-c4-model-diagrams)  
12. C4 model tools, accessed December 14, 2025, [https://c4model.tools/](https://c4model.tools/)  
13. Structurizr, accessed December 14, 2025, [https://structurizr.com/](https://structurizr.com/)  
14. Structurizr Lite, accessed December 14, 2025, [https://docs.structurizr.com/lite](https://docs.structurizr.com/lite)  
15. Structurizr Lite \- GitHub, accessed December 14, 2025, [https://github.com/structurizr/lite](https://github.com/structurizr/lite)  
16. 8 Reasons Not to Embed Dashboards with iFrames \- Embeddable, accessed December 14, 2025, [https://embeddable.com/blog/iframes-for-embedding](https://embeddable.com/blog/iframes-for-embedding)  
17. glanceapp/glance: A self-hosted dashboard that puts all your feeds in one place \- GitHub, accessed December 14, 2025, [https://github.com/glanceapp/glance](https://github.com/glanceapp/glance)  
18. iFrame | Homarr documentation, accessed December 14, 2025, [https://homarr.dev/docs/widgets/iframe/](https://homarr.dev/docs/widgets/iframe/)  
19. X-Frame-Options header \- HTTP \- MDN Web Docs, accessed December 14, 2025, [https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-Frame-Options](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-Frame-Options)  
20. X-Frame-Options: Examples and Benefits \- Indusface, accessed December 14, 2025, [https://www.indusface.com/learning/x-frame-options/](https://www.indusface.com/learning/x-frame-options/)  
21. How to embed iframes by bypassing X-Frame-Options and frame-ancestors directive, accessed December 14, 2025, [https://requestly.com/blog/bypass-iframe-busting-header/](https://requestly.com/blog/bypass-iframe-busting-header/)  
22. Missing X-Frame-Options Header: You Should Be Using CSP Anyway \- Invicti, accessed December 14, 2025, [https://www.invicti.com/blog/web-security/missing-x-frame-options-header](https://www.invicti.com/blog/web-security/missing-x-frame-options-header)  
23. Dozzle: Home, accessed December 14, 2025, [https://dozzle.dev/](https://dozzle.dev/)  
24. Authentication \- Dozzle, accessed December 14, 2025, [https://dozzle.dev/guide/authentication](https://dozzle.dev/guide/authentication)  
25. Getting Started | Beszel, accessed December 14, 2025, [https://beszel.dev/guide/getting-started](https://beszel.dev/guide/getting-started)  
26. Beszel — Lightweight self-hosted server monitoring for your homelab | Akash Rajpurohit, accessed December 14, 2025, [https://akashrajpurohit.com/blog/beszel-selfhosted-server-monitoring-solution/](https://akashrajpurohit.com/blog/beszel-selfhosted-server-monitoring-solution/)  
27. Session Replay Features \- Highlight.io, accessed December 14, 2025, [https://www.highlight.io/docs/general/product-features/session-replay/overview](https://www.highlight.io/docs/general/product-features/session-replay/overview)  
28. A ClickHouse-powered Observability Solution: Overview of Highlight.io, accessed December 14, 2025, [https://clickhouse.com/blog/overview-of-highlightio](https://clickhouse.com/blog/overview-of-highlightio)  
29. iframe Recording \- Highlight.io, accessed December 14, 2025, [https://www.highlight.io/docs/getting-started/browser/replay-configuration/iframes](https://www.highlight.io/docs/getting-started/browser/replay-configuration/iframes)  
30. Understanding LLM Observability | Engineering | ClickHouse Resource Hub, accessed December 14, 2025, [https://clickhouse.com/resources/engineering/llm-observability](https://clickhouse.com/resources/engineering/llm-observability)  
31. Why do customers choose Langfuse?, accessed December 14, 2025, [https://langfuse.com/handbook/chapters/why](https://langfuse.com/handbook/chapters/why)  
32. Custom Dashboards \- Langfuse, accessed December 14, 2025, [https://langfuse.com/docs/metrics/features/custom-dashboards](https://langfuse.com/docs/metrics/features/custom-dashboards)  
33. Show dashboard in IFrame by user claim · langfuse · Discussion \#8539 \- GitHub, accessed December 14, 2025, [https://github.com/orgs/langfuse/discussions/8539](https://github.com/orgs/langfuse/discussions/8539)  
34. Complete AI Application Observability | Monitor LLMs, APIs & Databases | Pydantic Logfire, accessed December 14, 2025, [https://pydantic.dev/logfire](https://pydantic.dev/logfire)  
35. Self Hosted Introduction \- Pydantic Logfire, accessed December 14, 2025, [https://logfire.pydantic.dev/docs/reference/self-hosted/overview/](https://logfire.pydantic.dev/docs/reference/self-hosted/overview/)  
36. ClickStack \- The ClickHouse Observability Stack | ClickHouse Docs, accessed December 14, 2025, [https://clickhouse.com/docs/use-cases/observability/clickstack/overview](https://clickhouse.com/docs/use-cases/observability/clickstack/overview)  
37. ClickStack — The ClickHouse Observability Stack | by Girff \- Medium, accessed December 14, 2025, [https://girff.medium.com/clickstack-the-clickhouse-observability-stack-1aa99fdbd915](https://girff.medium.com/clickstack-the-clickhouse-observability-stack-1aa99fdbd915)  
38. ClickStack: High-Performance Open-Source Observability | Logs, Metrics, Traces with ClickHouse, accessed December 14, 2025, [https://clickhouse.com/use-cases/observability](https://clickhouse.com/use-cases/observability)  
39. Cost Optimization in LLM Observability: How LangFuse Handles Petabytes Without Breaking the Bank | by Sharan Harsoor | Nov, 2025 | Medium, accessed December 14, 2025, [https://medium.com/@sharanharsoor/cost-optimization-in-llm-observability-how-langfuse-handles-petabytes-without-breaking-the-bank-0b0451242d1e](https://medium.com/@sharanharsoor/cost-optimization-in-llm-observability-how-langfuse-handles-petabytes-without-breaking-the-bank-0b0451242d1e)  
40. Affordable full-stack production debugging & monitoring. \- HyperDX, accessed December 14, 2025, [https://www.hyperdx.io/v2](https://www.hyperdx.io/v2)  
41. opentelemetry-collector-contrib/exporter/clickhouseexporter/README.md at main \- GitHub, accessed December 14, 2025, [https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/exporter/clickhouseexporter/README.md](https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/exporter/clickhouseexporter/README.md)  
42. Top 10 HyperDX Alternatives in 2025 | Better Stack Community, accessed December 14, 2025, [https://betterstack.com/community/comparisons/hyperdx-alternatives/](https://betterstack.com/community/comparisons/hyperdx-alternatives/)  
43. Integrating OpenTelemetry | ClickHouse Docs, accessed December 14, 2025, [https://clickhouse.com/docs/observability/integrating-opentelemetry](https://clickhouse.com/docs/observability/integrating-opentelemetry)  
44. Model Usage & Cost Tracking for LLM applications (open source) \- Langfuse, accessed December 14, 2025, [https://langfuse.com/docs/observability/features/token-and-cost-tracking](https://langfuse.com/docs/observability/features/token-and-cost-tracking)  
45. Langfuse and ClickHouse: A new data stack for modern LLM applications, accessed December 14, 2025, [https://clickhouse.com/blog/langfuse-and-clickhouse-a-new-data-stack-for-modern-llm-applications](https://clickhouse.com/blog/langfuse-and-clickhouse-a-new-data-stack-for-modern-llm-applications)