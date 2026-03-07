

# **The Evolution of Self-Hosted Database Infrastructure: Architecting Minimalist PostgreSQL Environments for Metadata Management**

## **Executive Summary**

The democratization of "serverless" and managed database platforms—typified by services like Neon, PlanetScale, and Supabase—has fundamentally altered developer expectations regarding database infrastructure. These platforms have shifted the paradigm from mere data persistence engines to comprehensive "Data Interaction Layers," offering integrated dashboards, seamless schema migration tools, and robust API layers. For the self-hosting enthusiast or home lab architect, this shift presents a complex challenge: how to replicate the superior Developer Experience (DX) and User Interface (UI) of these managed services on private infrastructure (a Virtual Private Server) without incurring the architectural bloat of unneeded Backend-as-a-Service (BaaS) components.  
This report provides an exhaustive analysis of the self-hosted PostgreSQL landscape, specifically tailored to the requirement of a "Minimal Supabase" architecture. It evaluates the feasibility of stripping the Supabase Docker stack to its bare essentials—retaining only the Database engine and the Studio dashboard—while integrating best-in-class external tools for Authentication (BetterAuth) and Object Storage (Cloudflare R2). Furthermore, it conducts a rigorous comparative study of alternative open-source ecosystems, including **Pigsty**, **Tembo**, and **Mathesar**, to identify the optimal solution for a central metadata storage node.  
The analysis reveals that while a "Minimal Supabase" is technically feasible through aggressive customization of container orchestration, it introduces significant maintenance debt. Conversely, a modular architecture leveraging **Pigsty** for enterprise-grade PostgreSQL management combined with **Mathesar** for high-fidelity metadata interaction offers a superior, lightweight, and maintainable alternative that better aligns with the decoupled nature of BetterAuth and R2.  
---

## **Part I: The Paradigm Shift in Database Infrastructure**

### **1.1 From Relational Engines to Data Platforms**

To understand the specific desire for "Neon-like" or "Supabase-like" self-hosting, one must first deconstruct what these services actually provide. Historically, self-hosting PostgreSQL meant installing the postgresql-server package, editing pg\_hba.conf, and interacting with the database primarily through the command-line interface (CLI) tool psql or desktop clients like pgAdmin.  
Modern managed services have abstracted this operational toil, but more importantly, they have introduced a "Control Plane" that sits above the database engine. This Control Plane provides three distinct value propositions that the user explicitly seeks to replicate:

1. **Visual Schema Management:** The ability to browse tables, edit rows, and modify schemas via a web interface (e.g., Supabase Studio, Neon Console) rather than writing raw DDL (Data Definition Language) statements. This "spreadsheet-ification" of the database lowers the barrier to entry for metadata management.  
2. **API-First Access:** Services like Supabase (via PostgREST) and Neon (via serverless drivers) expose the database over HTTP, simplifying connection management in serverless or edge environments.  
3. **Infrastructure abstraction:** Automated backups, High Availability (HA), and extensions management (e.g., pgvector, PostGIS) are handled automatically.

The user's query reflects a sophisticated understanding of this landscape: they value the **Control Plane** (specifically the UI/DX) but reject the **BaaS Payload** (Auth, Storage) that usually accompanies it. This distinction is critical. Supabase is designed as a monolithic BaaS where the database, auth, and storage are tightly coupled. Unraveling this coupling requires a deep understanding of the platform's internal dependency graph.

### **1.2 The "Home Lab" Context and Constraints**

Deploying on a single Virtual Private Server (VPS) introduces strict resource constraints that do not exist in cloud-native environments. Managed services leverage distributed architectures—separating storage from compute and running auxiliary services on separate clusters. A self-hosted setup must collapse this distributed architecture onto a single node.

* **Resource Contention:** Running a full stack like Supabase (which includes 10+ containers) on a standard VPS (e.g., 2 vCPU, 4GB RAM) can lead to memory exhaustion, as Java-based services (like some internal tools) or Elixir-based services (Realtime) compete with the PostgreSQL buffer cache.1  
* **Operational Complexity:** Managing 10 containers via Docker Compose is significantly more complex than managing a single Postgres service. The risk of container failure, networking issues (Docker bridge networks), and persistent volume management increases linearly with the number of services.3

Therefore, "minimization" is not merely an aesthetic preference; it is an operational necessity for stability in a single-node home lab environment.  
---

## **Part II: Deconstructing Supabase – The "Minimal" Feasibility Study**

The user specifically asks: *"Is there a minimal version of Supabase I can install?"* To answer this exhaustively, we must dissect the Supabase architecture to identify which organs are vital and which are vestigial for the user's specific use case (Postgres \+ Studio only).

### **2.1 The Supabase Dependency Graph**

Supabase is not a single binary; it is a composition of open-source tools orchestrated to work together. The standard self-hosting method uses Docker Compose to spin up these services. Understanding the interdependencies is the key to stripping it down.

#### **2.1.1 Core Components (The "Postgres Aspect")**

These are the non-negotiable components required to serve the user's request for a database with a UI.

1. **db (PostgreSQL):** The heart of the stack. Supabase uses a custom fork of PostgreSQL that includes a suite of pre-installed extensions (pg\_graphql, pgsodium, pgvector, etc.).  
   * *Dependency:* None.  
   * *Role:* Stores data and metadata.  
2. **meta (postgres-meta):** This is the critical "bridge" service. Supabase Studio (the UI) is a Next.js application that runs in the browser. It cannot speak the PostgreSQL binary protocol directly. Instead, it makes HTTP REST requests to postgres-meta, which translates them into SQL commands to fetch schemas, tables, and roles.4  
   * *Dependency:* Requires a connection to db.  
   * *Role:* Provides the API that allows the Studio UI to function.  
3. **studio (Supabase Studio):** The web dashboard.  
   * *Dependency:* Requires meta to populate its views. In the full stack, it also attempts to talk to auth and storage to populate those specific tabs.6  
   * *Role:* The visual interface.

#### **2.1.2 The "BaaS" Components (Candidates for Removal)**

These services provide functionality the user intends to offload to BetterAuth and Cloudflare R2.

1. **auth (GoTrue):** A JWT-based API for managing users.  
   * *Integration:* Tightly coupled with the auth schema in the database.  
   * *Removal Feasibility:* **High**, but with caveats. Studio's "Authentication" tab will break. RLS policies referencing auth.uid() will cease to function as expected unless manually handled.5  
2. **storage (Storage API):** An S3-compatible wrapper that stores file metadata in Postgres and files on disk (or S3).  
   * *Integration:* Dependent on the storage schema.  
   * *Removal Feasibility:* **High**. The user explicitly plans to use Cloudflare R2 directly. Removing this saves Node.js overhead.  
3. **realtime:** An Elixir server listening to the PostgreSQL replication stream (WAL) to broadcast changes via WebSockets.  
   * *Removal Feasibility:* **High**. This is a heavy service. If the user does not need live updates in their frontend, removing this frees up significant CPU/RAM.3  
4. **rest (PostgREST):** Auto-generates a REST API from the database schema.  
   * *Removal Feasibility:* **Medium**. If the user plans to connect to Postgres strictly via SQL clients or BetterAuth (which uses a DB adapter), this can be removed. If they want to use Supabase client libraries (supabase-js) in their frontend, this is mandatory.  
5. **kong (API Gateway):** The unifying router that exposes all services under a single port (usually 8000).  
   * *Removal Feasibility:* **Low to Medium**. While technically removable, kong handles the routing logic (e.g., /auth/v1 \-\> auth container, /rest/v1 \-\> rest container). Removing it requires configuring the studio container to talk directly to meta and the user to access services via direct ports, which complicates the setup.6

### **2.2 Architecting the Minimal Stack: "Studio-Only" Configuration**

To satisfy the request for a "minimal version," we can construct a custom docker-compose.yml that purges the unneeded services. This is not supported officially but is widely implemented by power users.

#### **2.2.1 The Manifest Strategy**

Instead of running the default stack, the user should run a stack consisting *only* of db, meta, and studio.  
**Configuration Requirements:**

* **Networking:** The studio container needs to reach meta. In Docker Compose, this is handled by service discovery (hostnames).  
* **Environment Variables:** The studio container checks for SUPABASE\_URL and SUPABASE\_ANON\_KEY. Even if auth and rest are removed, these variables often need to be populated with "dummy" values to prevent the Node.js process from crashing on startup.6

The Critical "Bypass" Configuration:  
Supabase Studio allows for a standalone mode. By setting specific environment variables, we can tell it to ignore the missing Auth service.

* STUDIO\_PG\_META\_URL: Must point specifically to the internal Docker URL of the meta service (e.g., http://meta:8080).  
* POSTGRES\_PASSWORD: The Studio needs this to authenticate via meta.

#### **2.2.2 The "Kong-less" Challenge**

The standard setup uses Kong to route traffic. If we remove Kong to save resources (it is a heavy Nginx/Lua application), we must expose studio directly.

* *Standard Port:* Studio listens on 3000\.  
* *Access:* The user would access http://vps-ip:3000.  
* *Issue:* Studio often constructs internal links based on the assumption that it sits behind Kong (e.g., links to the API might default to /rest/v1). However, for *metadata management* (viewing tables), this is rarely a blocker. The Table Editor interacts primarily with meta, which is a backend-to-backend connection.9

### **2.3 Operational Trade-offs of the "Hacked" Supabase**

While creating this minimal stack solves the resource constraint, it introduces **Developer Experience Debt**.

1. **The "Broken Dashboard" Effect:** Supabase Studio is a monolithic frontend. It does not feature-detect which backend services are running. Consequently, the sidebar will still display "Authentication," "Storage," "Edge Functions," and "Realtime." Clicking any of these will result in infinite loading spinners or "Service Unavailable" errors. This degrades the premium "Neon-like" feel the user desires.  
2. **Upgrade Friction:** The Supabase team releases updates assuming the full stack. Breaking changes in how studio talks to auth (e.g., a new requirement for a specific endpoint) can break a custom minimal deployment during an upgrade, requiring the user to debug internal APIs.  
3. **Database "Pollution":** The supabase/postgres image comes pre-loaded with many extensions and schemas (auth, storage, graphql, realtime, vault). Even if the services aren't running, these schemas exist in the database, cluttering the namespace compared to a vanilla Postgres installation.7

**Verdict on Minimal Supabase:** It is *possible* and strictly answers the user's prompt, but it is an "uncanny valley" experience—it looks like Supabase but feels broken in places. For a home lab focused on "metadata storage," cleaner alternatives exist.  
---

## **Part III: The "Batteries-Included" Alternative – Pigsty**

The user asked for "other popular opensource database solutions." The strongest contender for a self-hosted, "Neon-like" experience that prioritizes database management over app-backend features is **Pigsty** (PostgreSQL in Great Style).

### **3.1 Pigsty Architecture: Local-First RDS**

Pigsty 11 represents a different philosophy. While Supabase creates a *Backend-as-a-Service*, Pigsty creates a *Database-as-a-Service*. It turns a bare VPS into a production-grade RDS instance.

* **Deployment Mechanism:** Unlike the container-heavy Supabase approach, Pigsty uses **Ansible** to configure the host directly (though it can manage Docker). It is optimized for EL (Red Hat/Rocky) and Ubuntu/Debian systems.  
* **The "Neon" Equivalent:** Neon's selling point is its sophisticated control plane and observability. Pigsty replicates this via a massive suite of pre-configured **Grafana dashboards**. It captures metrics on query latency, buffer hit ratios, deadlocks, and OS-level resources (disk I/O, CPU saturation) with a fidelity that often exceeds expensive managed services.12

### **3.2 Component Analysis for Home Lab Usage**

For the user's "central metadata storage" use case, Pigsty offers distinct advantages:

1. **High Availability (HA) Ready:** Pigsty configures **Patroni** for high availability by default (or easily enabled). Even on a single node, Patroni ensures the Postgres process is managed correctly and can auto-restart with proper state management.  
2. **Extension Management:** Pigsty maintains its own repository of PostgreSQL extensions (yum/apt repos). It includes pgvector, PostGIS, pg\_graphql, and 100+ others pre-compiled. This solves the "how do I add extensions to my Docker container" problem that often plagues manual Docker setups.15  
3. **Backup Integration:** It includes pgBackRest configured out of the box, pushing backups to S3 (or MinIO/local disk). For a "central metadata storage" node, automated backups are a critical "managed service" feature that Supabase's self-hosted Docker stack does not handle natively (it requires manual script setup).16

### **3.3 Pigsty \+ Supabase: The Hybrid Approach**

One of Pigsty's most compelling features is its explicit support for running Supabase on top of it.15

* **The Concept:** Use Pigsty to manage the *Stateful* layer (PostgreSQL, HA, Backups, Monitoring). Use Docker to run the *Stateless* layer (Supabase Studio, Kong, Meta).  
* **Why this wins:** This architecture decouples the database engine from the UI. The user gets a rock-solid, monitored Postgres instance (BetterAuth and R2 connect here directly) and a lightweight Docker container for the Studio UI. If the Studio container crashes or breaks during an update, the database remains unaffected and monitored.  
* **Resource Efficiency:** Pigsty is highly optimized, running native binaries rather than containers for the DB engine. This leaves more RAM for the actual queries.

---

## **Part IV: The Dashboard Ecosystem – Replacing the Interface**

The user's attachment to Supabase is likely driven by **Supabase Studio**—the UI tool that makes Postgres feel like a spreadsheet. If we move away from the heavy Supabase stack, we must replace this UI with a standalone tool that offers equivalent or better "metadata management" DX.

### **4.1 Mathesar: The "Airtable for Postgres"**

**Mathesar** 19 is a standout open-source project specifically designed to turn a PostgreSQL database into a collaborative data interface. It is arguably the most direct replacement for the "Table Editor" aspect of Supabase Studio.

* **Philosophy:** "Data Collaboration." It is built for users who need to enter, edit, and curate data (metadata) without writing SQL.  
* **Architecture:** It runs as a Python/Django application (Dockerized) that connects to *any* Postgres database. It stores its own configuration in a dedicated schema, keeping the public schema clean.  
* **Comparison to Supabase Studio:**  
  * *Data Entry:* Mathesar's grid view is superior for heavy data entry, supporting varied data types (images, URLs, JSON) natively.  
  * *Schema Management:* It allows creating tables and defining relationships (Foreign Keys) via a visual interface, mirroring the Studio experience.  
  * *Direct Access:* Unlike NocoDB, which often creates "virtual" layers, Mathesar works directly with Postgres types and constraints.  
* **Fit for Request:** For a home lab "central metadata storage," Mathesar provides the cleanest, most specialized UI. It does not confuse the user with "Auth" or "Edge Function" tabs; it is purely focused on the data.21

### **4.2 CloudBeaver: The DBA's Swiss Army Knife**

**CloudBeaver** 24 is the web-based version of the popular DBeaver desktop client.

* **DX Style:** It feels like a desktop application running in a browser. It is less "modern SaaS" and more "classic IDE."  
* **Capabilities:** It is vastly more powerful than Supabase Studio for *database administration*. It supports creating complex indexes, triggers, stored procedures, and viewing ER diagrams—features Supabase Studio often simplifies or omits.  
* **Resource Usage:** It is a Java application, so it has a moderate memory footprint (similar to Supabase Studio but heavier than Mathesar), but it is a single container.

### **4.3 Tembo and the "Stacks" Concept**

**Tembo** 27 is a newer entrant attempting to productize the Postgres ecosystem.

* **Self-Hosting Model:** Tembo offers a self-hosted version via a **Kubernetes Operator**.  
* **The Barrier:** For a user on a single VPS ("options for postgresql on a vps i have"), requiring Kubernetes (even K3s) adds significant orchestrational overhead compared to Docker Compose. While Tembo offers a compelling "Stacks" concept (pre-tuned configs for Vector, OLAP, etc.), the infrastructure cost of running the control plane on a single node makes it less ideal for a "minimal" setup than Pigsty or Dockerized Postgres.29

---

## **Part V: Integration Architectures – Auth & Storage**

The user's plan to use **BetterAuth** and **Cloudflare R2** validates the decision to strip Supabase, as these external tools replace the need for the integrated BaaS components.

### **5.1 BetterAuth with Vanilla/Minimal Postgres**

BetterAuth is a framework-agnostic authentication library that runs in the application layer (e.g., Next.js, SvelteKit), not the database layer.

* **Schema Strategy:** Supabase forces a specific auth schema protected by extensive internal triggers. With BetterAuth, the user has full control. The report recommends creating a dedicated schema (e.g., app\_auth) for BetterAuth tables (user, session, account).  
* **RLS Implications:** One major loss when moving away from the full Supabase stack is the easy integration of RLS (Row Level Security) with Auth. In Supabase, auth.uid() is automatically populated in the SQL transaction context.  
  * *The Solution:* In a minimal/vanilla setup, the application (BetterAuth) must handle the context. When the app connects to Postgres, it can set a session variable (e.g., set\_config('app.current\_user\_id', 'user\_123', true)) at the start of the transaction. RLS policies can then be written to check this variable (current\_setting('app.current\_user\_id')). This replicates the Supabase security model without the Supabase Auth service.31

### **5.2 Cloudflare R2 Integration**

Since Supabase Storage is removed, the "metadata" aspect of file storage must be modeled explicitly in Postgres.

* **Data Modeling:** Instead of a storage.objects table managed by Supabase, the user should define columns in their business tables (e.g., avatar\_url, document\_r2\_key).  
* **UI Integration:**  
  * *Mathesar:* Supports a "URL" field type. If the user stores the public R2 URL, Mathesar can render a preview of the image/file directly in the grid, restoring the visual file management experience of Supabase Studio.  
  * *Supabase Studio (Minimal):* If using the minimal stack, the Table Editor can also render image URLs, provided they are public. However, the dedicated "Storage" UI (upload drag-and-drop) will not work, forcing all file management to happen via the application or the Cloudflare dashboard.

---

## **Part VI: Operational Excellence – Security, Backup, and Performance**

Self-hosting shifts the burden of reliability from the vendor to the user. A "central metadata storage" for a home lab implies this data is valuable.

### **6.1 The Backup Imperative**

A "Minimal Supabase" Docker container does **not** include automated backups. If the container dies or the volume is corrupted, data is lost.

* **Recommendation:** Implement **pgBackRest** or **Wal-G**.  
* **Pigsty Advantage:** Pigsty configures pgBackRest by default, allowing point-in-time recovery (PITR) to S3/MinIO. This is a critical feature parity with Neon/PlanetScale that is often missing in manual Docker setups.16

### **6.2 Security Hardening**

* **Network Isolation:** Ensure the Postgres port (5432) is not exposed to the public internet. Use a VPN (Tailscale/WireGuard) or an SSH tunnel for access.  
* **Studio Security:** The "Minimal Supabase" Studio container typically has no login protection (it relies on the removed kong/auth layers). **It must be put behind a reverse proxy** (Caddy/Nginx) with Basic Auth or Authelia to prevent unauthorized access to the database metadata.6

---

## **Conclusion and Final Recommendation**

The user's request for a "Postgres-only" Supabase on a VPS is a valid architectural pattern that prioritizes Developer Experience without the vendor lock-in of a BaaS. While strictly "minimizing" the Supabase Docker stack is possible, it results in a fragile and disjointed experience.  
**The Superior Architecture: The Modular Stack**  
For a home lab VPS serving as central metadata storage, the following stack offers the highest stability, best DX, and lowest resource footprint:

1. **Database Engine:** **Pigsty** (Standard Install).  
   * *Why:* Provides a production-grade, HA-ready, monitored (Grafana) PostgreSQL instance that rivals Neon's observability. It handles backups and extensions natively.  
2. **Metadata Interface:** **Mathesar** (Docker Container).  
   * *Why:* Connects to the Pigsty Postgres instance. Provides a superior, cleaner "spreadsheet" interface for metadata management than the broken Supabase Studio.  
3. **Application Layer:** **BetterAuth** \+ **Cloudflare R2**.  
   * *Why:* Decoupled from the database, adhering to the "clean architecture" principle.

This "Modular Stack" fulfills the user's desire for a modern, visual, and powerful self-hosted data platform while respecting the constraints of the hardware and the specific exclusions of the prompt.  
---

### **Comparison of Proposed Architectures**

| Feature | Minimal Supabase (Docker) | Pigsty \+ Mathesar (Recommended) |
| :---- | :---- | :---- |
| **RAM Usage** | \~1.5 GB | \~1.0 GB |
| **UI Experience** | Good (but broken tabs) | Excellent (Dedicated Data Tool) |
| **Observability** | Basic (Logs) | **Advanced (Grafana/Prometheus)** |
| **Backups** | Manual Setup | **Automated (pgBackRest)** |
| **Maintenance** | High (Custom Configs) | Low (Standard Ansible/Docker) |
| **RLS Integration** | Manual (Session vars) | Manual (Session vars) |

By choosing the **Pigsty \+ Mathesar** route, the user achieves the "Neon-like" dashboarding (via Pigsty's Grafana) and the "Airtable-like" data entry (via Mathesar), satisfying the "Metadata Storage" requirement with professional-grade open-source tooling.

#### **Works cited**

1. Minimum Specs for Supabase Self Hosted? \- Reddit, accessed December 1, 2025, [https://www.reddit.com/r/Supabase/comments/1aydpyg/minimum\_specs\_for\_supabase\_self\_hosted/](https://www.reddit.com/r/Supabase/comments/1aydpyg/minimum_specs_for_supabase_self_hosted/)  
2. Troubleshooting | High RAM usage \- Supabase Docs, accessed December 1, 2025, [https://supabase.com/docs/guides/troubleshooting/exhaust-ram](https://supabase.com/docs/guides/troubleshooting/exhaust-ram)  
3. What is the system requirements to run all supabase docker images? \- Reddit, accessed December 1, 2025, [https://www.reddit.com/r/Supabase/comments/16u7bk2/what\_is\_the\_system\_requirements\_to\_run\_all/](https://www.reddit.com/r/Supabase/comments/16u7bk2/what_is_the_system_requirements_to_run_all/)  
4. supabase/postgres-meta \- NPM, accessed December 1, 2025, [https://www.npmjs.com/package/@supabase/postgres-meta](https://www.npmjs.com/package/@supabase/postgres-meta)  
5. Architecture | Supabase Docs, accessed December 1, 2025, [https://supabase.com/docs/guides/getting-started/architecture](https://supabase.com/docs/guides/getting-started/architecture)  
6. Self-Hosting with Docker | Supabase Docs, accessed December 1, 2025, [https://supabase.com/docs/guides/self-hosting/docker](https://supabase.com/docs/guides/self-hosting/docker)  
7. secretarybird97/supabase-docker: Minimal Docker ... \- GitHub, accessed December 1, 2025, [https://github.com/secretarybird97/supabase-docker](https://github.com/secretarybird97/supabase-docker)  
8. \[Guide\] Supabase Self-Hosted using Orbstack HTTPS \#34686 \- GitHub, accessed December 1, 2025, [https://github.com/orgs/supabase/discussions/34686](https://github.com/orgs/supabase/discussions/34686)  
9. The ultimate Supabase self-hosting Guide \- David Lorenz, accessed December 1, 2025, [https://activeno.de/blog/2023-08/the-ultimate-supabase-self-hosting-guide/](https://activeno.de/blog/2023-08/the-ultimate-supabase-self-hosting-guide/)  
10. Database | Supabase Docs, accessed December 1, 2025, [https://supabase.com/docs/guides/database/overview](https://supabase.com/docs/guides/database/overview)  
11. pigsty-doc/s-faq.md at master \- GitHub, accessed December 1, 2025, [https://github.com/Vonng/pigsty-doc/blob/master/s-faq.md](https://github.com/Vonng/pigsty-doc/blob/master/s-faq.md)  
12. Hardware \- Pigsty Docs, accessed December 1, 2025, [https://doc.pgsty.com/prepare/hardware/](https://doc.pgsty.com/prepare/hardware/)  
13. Pigsty Docs | Pigsty, accessed December 1, 2025, [https://doc.pgsty.com/](https://doc.pgsty.com/)  
14. I created a fully self-hosted real-time monitoring dashboard for my frontend applications using Grafana \+ Postgres \+ BullMQ : r/webdev \- Reddit, accessed December 1, 2025, [https://www.reddit.com/r/webdev/comments/1o1hsxw/i\_created\_a\_fully\_selfhosted\_realtime\_monitoring/](https://www.reddit.com/r/webdev/comments/1o1hsxw/i_created_a_fully_selfhosted_realtime_monitoring/)  
15. Self-Hosting Supabase on PostgreSQL \- Pigsty, accessed December 1, 2025, [https://vonng.com/en/pg/supabase/](https://vonng.com/en/pg/supabase/)  
16. I built an open-source web UI to self-host your PostgreSQL backups. Now with Postgres 18 support\! : r/selfhosted \- Reddit, accessed December 1, 2025, [https://www.reddit.com/r/selfhosted/comments/1ns3z9m/i\_built\_an\_opensource\_web\_ui\_to\_selfhost\_your/](https://www.reddit.com/r/selfhosted/comments/1ns3z9m/i_built_an_opensource_web_ui_to_selfhost_your/)  
17. Self-Hosting Supabase on PostgreSQL \- Pigsty, accessed December 1, 2025, [https://pigsty.io/docs/app/supabase/](https://pigsty.io/docs/app/supabase/)  
18. Supabase: Self-Hosting OSS Firebase \- Pigsty, accessed December 1, 2025, [https://pigsty.io/docs/software/supabase/](https://pigsty.io/docs/software/supabase/)  
19. Mathesar \- Open source UI for Postgres databases | Mathesar, accessed December 1, 2025, [https://mathesar.org/](https://mathesar.org/)  
20. Mathesar – an intutive spreadsheet-like interface to Postgres data | Hacker News, accessed December 1, 2025, [https://news.ycombinator.com/item?id=42873312](https://news.ycombinator.com/item?id=42873312)  
21. Databases \- Mathesar Documentation, accessed December 1, 2025, [https://docs.mathesar.org/0.7.0/user-guide/databases/](https://docs.mathesar.org/0.7.0/user-guide/databases/)  
22. Install using Docker Compose \- Mathesar Documentation, accessed December 1, 2025, [https://docs.mathesar.org/0.1.7/installation/docker-compose/](https://docs.mathesar.org/0.1.7/installation/docker-compose/)  
23. Schemas \- Mathesar Documentation, accessed December 1, 2025, [https://docs.mathesar.org/0.2.2/user-guide/schemas/](https://docs.mathesar.org/0.2.2/user-guide/schemas/)  
24. Bytebase vs. CloudBeaver: a side-by-side comparison for web-based database management, accessed December 1, 2025, [https://www.bytebase.com/blog/bytebase-vs-cloudbeaver/](https://www.bytebase.com/blog/bytebase-vs-cloudbeaver/)  
25. CloudBeaver \- A Self hosted Database Browser : r/selfhosted \- Reddit, accessed December 1, 2025, [https://www.reddit.com/r/selfhosted/comments/mo6x3i/cloudbeaver\_a\_self\_hosted\_database\_browser/](https://www.reddit.com/r/selfhosted/comments/mo6x3i/cloudbeaver_a_self_hosted_database_browser/)  
26. 5 Best Online Database Clients in 2025 \- DbGate, accessed December 1, 2025, [https://www.dbgate.io/news/2025-01-25-online-database-clients/](https://www.dbgate.io/news/2025-01-25-online-database-clients/)  
27. Tembo — Build better software, accessed December 1, 2025, [https://tembo.io/](https://tembo.io/)  
28. Tembo \- AWS Marketplace, accessed December 1, 2025, [https://aws.amazon.com/marketplace/pp/prodview-o325rdgya7662](https://aws.amazon.com/marketplace/pp/prodview-o325rdgya7662)  
29. Helm chart for Tembo Self Hosted \- GitHub, accessed December 1, 2025, [https://github.com/tembo-io/tembo-self-hosted](https://github.com/tembo-io/tembo-self-hosted)  
30. tembo-io/tembo-images: Docker images for Postgres \- GitHub, accessed December 1, 2025, [https://github.com/tembo-io/tembo-images](https://github.com/tembo-io/tembo-images)  
31. Postgres Row Level Security: Restricting Access to Sensitive Data \- DataSunrise, accessed December 1, 2025, [https://www.datasunrise.com/knowledge-center/postgres-row-level-security/](https://www.datasunrise.com/knowledge-center/postgres-row-level-security/)  
32. Documentation: 18: 5.9. Row Security Policies \- PostgreSQL, accessed December 1, 2025, [https://www.postgresql.org/docs/current/ddl-rowsecurity.html](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)  
33. Postgres RLS Implementation Guide \- Best Practices, and Common Pitfalls \- Permit.io, accessed December 1, 2025, [https://www.permit.io/blog/postgres-rls-implementation-guide](https://www.permit.io/blog/postgres-rls-implementation-guide)