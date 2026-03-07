

# **Architectural Divergence in Postgres-Centric Stacks: A Comparative Analysis of Monolithic Supabase Docker Distributions versus Modular Pigsty Orchestration**

## **1\. Introduction: The Evolution of the Self-Hosted Database Platform**

The contemporary landscape of backend infrastructure has witnessed a paradigm shift from discrete database management to integrated "Backend-as-a-Service" (BaaS) platforms. At the forefront of this transition stands Supabase, which has successfully commoditized the PostgreSQL ecosystem by wrapping the core database engine with a suite of middleware—authentication, auto-generated APIs, real-time subscriptions, and object storage. For developers, this proposition is compelling: it reduces the friction of backend engineering to mere configuration. However, as organizations seek to repatriate data from the cloud or optimize infrastructure costs through self-hosting, the monolithic architecture that makes Supabase's cloud offering seamless often introduces significant friction in a self-managed environment.  
The request to compare self-hosting Supabase via its standard Docker Compose distribution against the Pigsty PostgreSQL distribution reveals a fundamental tension in modern DevOps: the trade-off between "black-box" convenience and "glass-box" control. On one hand, the official Supabase Docker image offers an immediate, albeit rigid, replication of the cloud experience. On the other, Pigsty represents a "batteries-included" approach to PostgreSQL governance, emphasizing infrastructure-as-code (IaC), high availability, and extension management at the operating system level.  
This report provides an exhaustive technical analysis of these two approaches, specifically framed within an orchestration environment managed by Komodo. Furthermore, it addresses the architectural challenge of "unbundling" the Supabase monolith to create a minimal, high-performance stack that replaces the proprietary-adjacent Supabase Auth (GoTrue) with the open-source library Better-Auth. This hybrid architecture aims to retain the high-value components of Supabase—specifically the Studio dashboard and PostgREST API—while shedding the operational weight of unused services, thereby creating a bespoke, minimal, and highly observable data platform.  
---

## **2\. Infrastructure Orchestration: The Role of Komodo in Fleet Management**

Before dissecting the database layer, it is essential to establish the orchestration context. The user’s requirement involves deploying these services via Docker Compose, managed by Komodo. This selection of tooling is not merely a preference but a strategic architectural decision that influences how stateful and stateless workloads are decoupled.

### **2.1 The Core-Periphery Architecture of Komodo**

Komodo differentiates itself from traditional Docker management interfaces like Portainer or Dockge through its distinct "Core-Periphery" architecture. In standard container orchestration, the management tool often resides on the same server or communicates directly with the Docker socket, posing potential security risks and scalability limits. Komodo, conversely, employs a centralized **Core**—a web application hosting the API and UI—and distributed **Periphery** agents deployed on target servers.1  
This architecture is particularly relevant for the proposed split-stack deployment (database on Pigsty, middleware on Docker). The Komodo Periphery agent is a lightweight, stateless web server that exposes an API strictly for the Core to consume. It allows for the execution of Docker commands, retrieval of system resource usage (CPU, memory, disk), and stream logs, all while protected by an IP allowlist and mutual authentication.2 For a self-hosted Supabase setup, which traditionally spans multiple containers (Studio, Kong, GoTrue, PostgREST), this capability allows an administrator to visualize the entire stack's health across multiple nodes from a single "pane of glass" without SSH-ing into individual machines.

### **2.2 UI-Defined Stacks and GitOps Integration**

A critical feature of Komodo for managing the complexity of a Supabase distribution is its support for "Stacks." In the Komodo lexicon, a Stack is synonymous with a Docker Compose definition, but with enhanced lifecycle management. The platform supports **UI Defined** stacks, where the compose configuration is stored within Komodo’s internal database, or **Git-Synced** stacks, where the configuration is pulled from a remote repository.4  
For the minimal setup requested—where Better-Auth replaces GoTrue—the GitOps capability of Komodo becomes indispensable. It allows the infrastructure code (the docker-compose.yml defining Better-Auth, PostgREST, and Studio) to be version-controlled alongside the application code. When a developer pushes a change to the Better-Auth configuration (e.g., adding a new OAuth provider or modifying the schema adapter settings), Komodo’s webhook listeners can trigger an automatic redeployment of the specific container.4 This capability bridges the gap between the "set-and-forget" nature of database hosting and the agile, iterative nature of application authentication development.

### **2.3 Variable Interpolation and Secret Management**

One of the most pervasive challenges in self-hosting Supabase is the management of shared secrets—specifically the JWT\_SECRET that allows the separate services (Auth, Realtime, PostgREST) to trust one another. In a raw Docker Compose environment, these are often scattered across .env files, creating security vulnerabilities and synchronization drifts.  
Komodo addresses this through a centralized secret interpolation system. Variables defined at the project or server level in Komodo can be injected into the Compose stacks at runtime.1 This ensures that the JWT\_SECRET used by the custom Better-Auth container to sign tokens is cryptographically identical to the secret used by the PostgREST container to verify them. If a rotation is required, it can be updated in one location within the Komodo UI, triggering a controlled restart of all dependent services. This feature alone significantly elevates the security posture of the self-hosted stack compared to manual docker-compose up invocations.  
---

## **3\. The Data Layer Comparison: Monolithic Docker vs. Pigsty Distribution**

The heart of the Supabase experience is PostgreSQL. However, Supabase does not run "vanilla" PostgreSQL. It relies on a heavily modified instance equipped with a specific suite of extensions (pg\_graphql, pg\_net, vault, wrappers) that enable its BaaS features. The method of delivering this database—either as a pre-packaged Docker image or as a managed OS-level distribution—constitutes the primary divergence between the official Supabase self-hosting method and Pigsty.

### **3.1 Option A: The Official Supabase Docker Distribution**

The standard path for self-hosting Supabase involves pulling the supabase/postgres Docker image. This image is a monolithic artifact that bundles the PostgreSQL kernel with the required extensions and configuration files.

#### **3.1.1 The Convenience of the Black Box**

The primary advantage of this approach is immediacy. A developer can clone the Supabase repository, run docker compose up, and have a functional clone of the Supabase Cloud platform within minutes.5 The docker-compose.yml provided by Supabase orchestrates the interactions between the database and the middleware (GoTrue, PostgREST, Realtime, Storage, Kong) without requiring the user to understand the underlying connection strings or authentication flows.5

#### **3.1.2 The Hidden Cost: Extension Management and Lock-In**

The convenience of the Docker image comes at the cost of flexibility and maintainability. The research highlights a form of "implicit vendor lock-in" inherent in this design.6 The supabase/postgres image pins specific versions of the database kernel and its extensions. If a user wishes to upgrade PostgreSQL (e.g., from v15 to v16) or patch a specific extension like pg\_vector independently of the Supabase release cycle, they are effectively blocked. They must wait for Supabase to release a new image or build their own from scratch, which requires deep knowledge of the complex build chain.  
Furthermore, many of the extensions Supabase relies on—specifically wrappers (Foreign Data Wrappers) and pg\_graphql—are not available in the standard PostgreSQL Global Development Group (PGDG) repositories.6 This means a user cannot simply spin up a generic Postgres container and expect Supabase Studio to work; they are tethered to the Supabase-maintained image.

#### **3.1.3 Operational Limitations: High Availability and Backups**

Running the database as a single container within a Docker Compose stack introduces severe "Day 2" operational risks.

* **Single Point of Failure:** If the Docker daemon crashes or the container filesystem corrupts, the entire platform goes offline.  
* **Scaling Difficulties:** Adding read replicas or setting up synchronous replication in a Docker Compose environment is non-trivial, often requiring manual configuration of streaming replication and fragile networking setups between containers.  
* **Backup Complexity:** While simple pg\_dump scripts can be scheduled, implementing enterprise-grade Point-in-Time Recovery (PITR) with WAL archiving usually requires running a sidecar container (like wal-g) and managing shared volumes, which complicates the stack.7

### **3.2 Option B: The Pigsty Distribution (PostgreSQL in Great STYle)**

Pigsty represents a fundamentally different philosophy: it is an open-source, local-first RDS (Relational Database Service) alternative. Rather than wrapping Postgres in a container, Pigsty uses Ansible to provision a production-grade PostgreSQL cluster directly on the operating system (though it can also manage Docker-based deployments).8

#### **3.2.1 Solving the Extension Gap**

One of Pigsty's most significant contributions to the self-hosted ecosystem is its resolution of the "Extension Gap." The research indicates that Pigsty maintains its own repository of RPM and DEB packages, compiling over 400 PostgreSQL extensions, including the elusive Supabase suite (pg\_graphql, pg\_jsonschema, pg\_net, vault, wrappers).6  
This is a crucial differentiator. It allows the user to run a standard, upstream PostgreSQL kernel (supported by the broader community) while still installing the specific plugins required to power Supabase Studio and PostgREST. This decouples the database engine from the Supabase platform version, granting the administrator the freedom to upgrade the database kernel or extensions independently.6

#### **3.2.2 Enterprise-Grade High Availability (HA)**

Unlike the singleton Docker container, Pigsty defaults to a high-availability architecture powered by **Patroni**. Patroni is the industry standard for managing PostgreSQL HA.

* **Mechanism:** Pigsty deploys a Distributed Consensus Store (DCS), typically etcd, to manage the cluster state. Patroni agents on each database node communicate with etcd to elect a leader.  
* **Failover:** If the primary node fails, Patroni automatically detects the outage and promotes the most up-to-date replica to primary, reconfiguring the remaining replicas to follow the new leader. This process happens in seconds and is transparent to the application (Better-Auth, PostgREST) if a Virtual IP (VIP) or HAProxy is used.7  
* **Infrastructure:** This architecture transforms the self-hosted setup from a fragile dev environment into a robust, "Cloud-Exit" capable platform. Users can leverage local NVMe SSDs for performance that orders of magnitude cheaper than equivalent cloud storage (EBS/S3).6

#### **3.2.3 Observability and Monitoring**

The research underscores Pigsty’s massive observability capabilities. A standard Pigsty deployment includes a complete telemetry stack: Prometheus for metrics collection, Grafana for visualization, and Loki for log aggregation.9

* **Visibility:** Administrators get out-of-the-box dashboards detailing query performance (via pg\_stat\_statements), operating system metrics, connection pool saturation (Pgbouncer), and replication lag.  
* **Contrast:** Achieving this level of visibility with the official Supabase Docker setup requires manually configuring external exporters and setting up a separate monitoring stack, a task that often exceeds the complexity of hosting the database itself.

### **3.3 Comparative Summary: Data Layer**

| Feature | Supabase Docker Image | Pigsty Distribution |
| :---- | :---- | :---- |
| **Deployment Model** | Single Container (Docker Compose) | OS-Level Cluster (Ansible/RPM) |
| **Extension Availability** | Pre-baked, Fixed Versions | Modular, 400+ Packages (RPM/DEB) |
| **High Availability** | Manual / Difficult to Configure | Native (Patroni \+ ETCD \+ HAProxy) |
| **Observability** | Basic Logs (docker logs) | Full Stack (Prometheus/Grafana/Loki) |
| **Backup / PITR** | Manual Configuration | Built-in (pgBackRest) |
| **Updates** | Image Replacement (Downtime) | Package Manager (yum update) |
| **Vendor Lock-in** | High (Custom Image) | Low (Upstream Kernel \+ Packages) |

---

## **4\. Deconstructing the Monolith: Defining the Minimal Architecture**

The user’s request explicitly seeks a "minimal" setup, identifying features of Supabase that are surplus to requirements. The official Supabase Docker Compose file is a monolith containing over a dozen services. To achieve efficiency and simplicity, we must audit these services to determine which are essential for the desired functionality (Studio \+ API) and which can be excised.

### **4.1 The Critical Path: Mandatory Components**

To maintain the "Supabase Experience"—specifically the ability to use the Studio UI to manage tables and the PostgREST API to query data—the following components are non-negotiable.

#### **4.1.1 PostgreSQL (The State Store)**

As established, this will be provided by Pigsty. It must host the data, the schema definitions, and the active extensions (pg\_graphql, etc.).

#### **4.1.2 Supabase Studio (supabase/studio)**

This is the dashboard interface. It provides the Table Editor, SQL Editor, and database settings UI.

* **Dependency:** Studio is a Next.js application that does not connect directly to the database for schema operations. Instead, it relies on the postgres-meta service.  
* **Login Dependency:** A critical finding in the research is Studio's hardcoded dependency on Supabase Auth (GoTrue). The research indicates that even with flags like ENABLE\_EMAIL\_AUTOCONFIRM=false, Studio attempts to redirect unauthenticated users to a GoTrue-managed login flow.10  
* **Workaround:** For a truly minimal setup without GoTrue, the "Auth" tab in Studio will be non-functional. Access to Studio itself (the dashboard) must be secured. Since Studio's internal auth is tightly coupled to the platform, the recommended approach for self-hosting without GoTrue is to place Studio behind a reverse proxy (like Nginx or Traefik, managed by Komodo) that enforces **Basic Authentication** or connects to an external Identity Provider (like Authelia). This effectively "walls off" the dashboard, bypassing the need for Studio's internal login logic.5

#### **4.1.3 Postgres-Meta (supabase/postgres-meta)**

This is a lightweight RESTful API that acts as a middleware between Studio and the PostgreSQL database.

* **Function:** It creates the abstraction layer that allows Studio to fetch table columns, run SQL queries, and manage extensions.  
* **Configuration:** It requires a direct connection string to the database and a PG\_META\_CRYPTO\_KEY for encrypting secrets.5 It is stateless and easily containerized.

#### **4.1.4 PostgREST (postgrest/postgrest)**

This is the engine that auto-generates the REST API from the database schema.

* **Function:** It turns database tables into REST endpoints.  
* **Security:** It relies heavily on the JWT\_SECRET. PostgREST verifies the signature of incoming bearer tokens. If the signature is valid, it inspects the role claim in the token (e.g., authenticated) and switches to that PostgreSQL role to execute the query. This mechanism is what enforces Row Level Security (RLS).11

### **4.2 The Optional Components: Candidates for Removal**

To achieve the "minimal" goal, the following services can be removed or replaced, reducing the memory footprint and attack surface of the stack.

#### **4.2.1 Supabase Auth / GoTrue (supabase/gotrue)**

* **Status:** **REMOVE** (per User Request).  
* **Implication:** This service manages the auth schema (users, identities, sessions). Removing it means the standard Supabase client libraries (supabase-js) will not be able to perform supabase.auth.signIn(). This responsibility will be transferred to Better-Auth.  
* **Database Impact:** The auth schema in the database will remain empty or can be repurposed.

#### **4.2.2 Kong API Gateway (supabase/kong)**

* **Status:** **REMOVE**.  
* **Function:** In the standard stack, Kong routes requests to the appropriate service (e.g., /auth/v1 to GoTrue, /rest/v1 to PostgREST) and handles API key validation (the anon and service\_role keys).  
* **Replacement:** Since the user is employing Komodo, which typically orchestrates a reverse proxy (like Traefik, Caddy, or Nginx Proxy Manager), Kong is redundant. The routing rules can be defined directly in the proxy layer:  
  * api.domain.com/rest/\* \-\> PostgREST Container  
  * api.domain.com/auth/\* \-\> Better-Auth Container  
  * dashboard.domain.com \-\> Supabase Studio Container  
* **Benefit:** Kong is resource-intensive (Java/Lua). Removing it significantly lowers the RAM requirements of the stack.13

#### **4.2.3 Supabase Realtime (supabase/realtime)**

* **Status:** **OPTIONAL / REMOVE**.  
* **Function:** It listens to the PostgreSQL replication stream (WAL) and broadcasts changes to clients via WebSockets.  
* **Decision:** Unless the user is building a chat application or a live collaborative tool, Realtime is unnecessary. Standard CRUD applications do not need it. Removing it saves CPU cycles associated with WAL processing and Elixir runtime overhead.

#### **4.2.4 Storage API (supabase/storage-api)**

* **Status:** **OPTIONAL**.  
* **Function:** It provides an S3-compatible API wrapper that integrates with Postgres RLS for file permissions.  
* **Replacement:** If the user needs file storage, Pigsty deploys **MinIO** by default.7 The application (via Better-Auth or the backend) can communicate directly with MinIO using standard AWS S3 SDKs. This bypasses the Supabase wrapper, offering a more standard, vendor-neutral storage implementation.

#### **4.2.5 Edge Functions, Vector, and ImgProxy**

* **Status:** **REMOVE**.  
* **Reasoning:** These are specialized services. pgvector functionality is handled natively by the Pigsty database extension. Image resizing and serverless functions are better handled by the application backend or dedicated services rather than maintaining complex Deno runtimes in a minimal stack.

---

## **5\. The Authentication Pivot: Integrating Better-Auth**

The decision to replace GoTrue with Better-Auth is the most architecturally significant change. It moves the "Source of Truth" for identity from a proprietary service (GoTrue) to an open-source, schema-flexible library (Better-Auth) running in a Node.js/Bun container. The challenge lies in making this new identity provider compatible with the existing PostgREST authorization model.

### **5.1 The Schema Conflict and Resolution**

Supabase's architecture reserves the auth schema for GoTrue. PostgREST is often configured to look for user information in auth.users, and many standard RLS policies (auth.uid()) rely on this specific schema structure.  
**The Conflict:** Better-Auth needs to store its own tables (user, session, account, verification). If Better-Auth attempts to write to the auth schema, it may conflict with triggers or foreign keys expected by the Supabase ecosystem.14  
**The Strategy:**

1. **Dedicated Schema:** Configure Better-Auth to use a separate schema (e.g., better\_auth or app\_auth) to avoid polluting the global namespace or colliding with any residual Supabase definitions.  
2. **Schema Configuration in Better-Auth:**  
   TypeScript  
   import { betterAuth } from "better-auth";  
   import { Pool } from "pg";

   export const auth \= betterAuth({  
       database: new Pool({  
           connectionString: process.env.DATABASE\_URL,  
           // Force the search path to the custom schema  
           options: "-c search\_path=better\_auth,public"   
       }),  
       //...  
   });

   This ensures that Better-Auth's migrations create tables in the correct location.15

### **5.2 The JWT Bridge: Minting Tokens for PostgREST**

For PostgREST to serve data securely, it requires an **Authorization Bearer** token. It does not care *who* minted the token, only that the token is signed by the JWT\_SECRET it possesses.16

#### **5.2.1 Shared Secret Architecture**

* **Constraint:** The JWT\_SECRET must be a shared secret (symmetric HS256) known to both the Token Issuer (Better-Auth) and the Token Verifier (PostgREST).  
* **Configuration:**  
  * In the **PostgREST** container (managed by Komodo), set the PGRST\_JWT\_SECRET environment variable to a strong, 32+ character string.  
  * In the **Better-Auth** container, configure the library to use this *exact same string* for signing tokens.

#### **5.2.2 Payload Compatibility and Role Injection**

PostgREST expects specific claims in the JWT payload to function correctly. If these are missing, it will default to the anonymous role or reject the request.

1. **role Claim:** This is the most critical claim. It tells Postgres which database role to masquerade as. For logged-in users, this must be authenticated.  
2. **sub (Subject) Claim:** This typically holds the User ID (UUID). RLS policies use this to filter data (e.g., user\_id \= auth.uid()).  
3. **exp (Expiration):** Validity timestamp.

Better-Auth Implementation:  
Standard Better-Auth session tokens are opaque or have different structures. We must use the Better-Auth JWT Plugin to customize the payload to match PostgREST's expectations.17  
**Code Example for Better-Auth Config:**

TypeScript

import { betterAuth } from "better-auth";  
import { jwt } from "better-auth/plugins";

export const auth \= betterAuth({  
    plugins:,  
    // The shared secret must match PGRST\_JWT\_SECRET  
    secret: process.env.BETTER\_AUTH\_SECRET   
});

### **5.3 The "Anon" Role and Public Access**

In the standard Supabase stack, the "anon" key is a long-lived JWT provided to the frontend client. It allows unauthenticated users to access endpoints that have public RLS policies.

* **Challenge:** Better-Auth does not issue "anon" tokens by default.  
* **Solution:** PostgREST has a configuration setting PGRST\_DB\_ANON\_ROLE (usually set to anon or web\_anon). If a request arrives *without* an Authorization header, PostgREST automatically switches to this role.  
* **Front-End Implication:** The frontend application should be configured *not* to send any Authorization header when the user is logged out. This triggers the default anon role in PostgREST, allowing access to public data without needing a specific "anon key".12

---

## **6\. Implementation Roadmap: Configuring the Komodo Stack**

This section provides the specific configuration details for assembling the minimal, decoupled stack using Pigsty and Komodo.

### **6.1 Step 1: The Pigsty Host Configuration**

The foundation is the database. Using the supa template in Pigsty ensures the correct extensions are loaded.  
**File:** pigsty.yml (Snippet for supa template modification)

YAML

supa:  
  hosts:  
    10.10.10.10: { supa\_seq: 1 } \# Host IP  
  vars:  
    \# Essential extensions provided by Pigsty repo  
    pg\_extensions:   
      \- pg\_graphql  
      \- pg\_net  
      \- wrappers  
      \- pg\_jsonschema  
      \- vector  
      
    \# User Management (Replaces GoTrue's DB management)  
    pg\_users:  
      \- { name: authenticator, password: SECURE\_PASSWORD, login: true }  
      \- { name: anon, login: false } \# Cannot login directly  
      \- { name: authenticated, login: false } \# Cannot login directly  
      
    \# HBA Rules: CRITICAL for Docker Connectivity  
    \# Allow the Docker subnet (e.g., 172.18.0.0/16) to connect  
    pg\_hba\_rules:  
      \- { user: all, db: all, addr: 172.18.0.0/16, auth: md5, title: 'Allow Docker' }

**Execution:** Run ./install.yml to provision the High-Availability cluster.

### **6.2 Step 2: The Komodo Stack Definition**

In Komodo, create a new "UI Defined Stack" or point to a Git repo with the following docker-compose.yml.  
**Stack:** minimal-supabase

YAML

version: '3.8'

services:  
  \# Service 1: Better-Auth (The Identity Provider)  
  auth:  
    image: my-registry/better-auth-server:latest \# Custom build  
    environment:  
      DATABASE\_URL: postgres://authenticator:SECURE\_PASSWORD@10.10.10.10:5432/supa  
      BETTER\_AUTH\_SECRET: ${SHARED\_JWT\_SECRET} \# Interpolated by Komodo  
      BETTER\_AUTH\_URL: https://auth.mydomain.com  
    networks:  
      \- supabase\_net

  \# Service 2: PostgREST (The Data API)  
  api:  
    image: postgrest/postgrest:latest  
    environment:  
      \# Connects to Pigsty Host IP, not localhost  
      PGRST\_DB\_URI: postgres://authenticator:SECURE\_PASSWORD@10.10.10.10:5432/supa  
      PGRST\_DB\_SCHEMAS: public,graphql\_public  
      PGRST\_DB\_ANON\_ROLE: anon  
      PGRST\_JWT\_SECRET: ${SHARED\_JWT\_SECRET} \# Interpolated by Komodo  
    networks:  
      \- supabase\_net

  \# Service 3: Postgres-Meta (Helper for Studio)  
  meta:  
    image: supabase/postgres-meta:latest  
    environment:  
      PG\_META\_DB\_URL: postgres://postgres:SUPERUSER\_PASSWORD@10.10.10.10:5432/supa  
      PG\_META\_PORT: 8080  
    networks:  
      \- supabase\_net

  \# Service 4: Supabase Studio (The Dashboard)  
  studio:  
    image: supabase/studio:latest  
    environment:  
      STUDIO\_PG\_META\_URL: http://meta:8080  
      POSTGRES\_PASSWORD: SUPERUSER\_PASSWORD  
      \# Workaround: Dummy URL to prevent startup crash, though Auth tab will fail  
      SUPABASE\_URL: http://api:3000   
      \# Disable platform specific checks  
      NEXT\_PUBLIC\_IS\_PLATFORM: "false"   
    networks:  
      \- supabase\_net

networks:  
  supabase\_net:  
    driver: bridge

### **6.3 Step 3: Networking and Access Control**

1. **Reverse Proxy:** Use Komodo to configure the ingress (e.g., Traefik labels) to route traffic.  
   * auth.mydomain.com \-\> auth container (port 3000/4000)  
   * api.mydomain.com \-\> api container (port 3000\)  
   * studio.mydomain.com \-\> studio container (port 3000\)  
2. **Security Layer:** Since Studio's internal login is broken without GoTrue, configure the Reverse Proxy to require **Basic Auth** for the studio.mydomain.com route. This provides a simple but effective login screen before loading the dashboard.

---

## **7\. Conclusion**

The transition from a monolithic Supabase Docker deployment to a modular, "unbundled" architecture represents a maturation of self-hosted infrastructure. By leveraging **Pigsty**, the data layer gains the resilience, observability, and extensibility of an enterprise distribution, effectively eliminating the vendor lock-in associated with custom Docker images. By employing **Komodo**, the orchestration layer gains GitOps capabilities and centralized secret management that standard Docker Compose lacks.  
Replacing Supabase Auth with **Better-Auth** significantly reduces the stack's footprint and complexity, aligning the authentication mechanism with modern, open-source standards. The critical integration point—the shared JWT\_SECRET and the injection of the role claim—allows PostgREST to function transparently, enforcing Row Level Security without awareness of the underlying identity provider shift.  
This report confirms that the proposed architecture is not only viable but superior for teams prioritizing control, minimalism, and long-term maintainability over immediate "out-of-the-box" convenience. The result is a platform that scales with the robustness of bare-metal Postgres but retains the developer velocity of the Supabase ecosystem.

#### **Works cited**

1. What is Komodo? | Komodo, accessed December 2, 2025, [https://komo.do/docs/intro](https://komo.do/docs/intro)  
2. Komodo (komo.do): A Build & Deployment System for Docker/Compose | by mario marco, accessed December 2, 2025, [https://medium.com/@mariomarco08/komodo-komo-do-a-build-deployment-system-for-docker-compose-9470136d5751](https://medium.com/@mariomarco08/komodo-komo-do-a-build-deployment-system-for-docker-compose-9470136d5751)  
3. Taming Your Containers: A Deep Dive into Komodo, the Ultimate Open-Source Management GUI \- Quadrata, accessed December 2, 2025, [https://www.quadrata.ae/taming-your-containers-a-deep-dive-into-komodo-the-ultimate-open-source-management-gui/](https://www.quadrata.ae/taming-your-containers-a-deep-dive-into-komodo-the-ultimate-open-source-management-gui/)  
4. Resources \- Komodo, accessed December 2, 2025, [https://komo.do/docs/resources](https://komo.do/docs/resources)  
5. Self-Hosting with Docker | Supabase Docs, accessed December 2, 2025, [https://supabase.com/docs/guides/self-hosting/docker](https://supabase.com/docs/guides/self-hosting/docker)  
6. Self-Hosting Supabase on PostgreSQL \- Pigsty, accessed December 2, 2025, [https://vonng.com/en/pg/supabase/](https://vonng.com/en/pg/supabase/)  
7. Supabase \- Pigsty Docs, accessed December 2, 2025, [https://doc.pgsty.com/app/supabase/](https://doc.pgsty.com/app/supabase/)  
8. pgsty/pigsty: Free RDS for PostgreSQL \- GitHub, accessed December 2, 2025, [https://github.com/pgsty/pigsty](https://github.com/pgsty/pigsty)  
9. Modules | Pigsty, accessed December 2, 2025, [https://v27.pgsty.com/docs/about/module/](https://v27.pgsty.com/docs/about/module/)  
10. Has anyone been able to get Login feature to work on a self hosted Supabase instance?, accessed December 2, 2025, [https://www.reddit.com/r/Supabase/comments/1ofj6k9/has\_anyone\_been\_able\_to\_get\_login\_feature\_to\_work/](https://www.reddit.com/r/Supabase/comments/1ofj6k9/has_anyone_been_able_to_get_login_feature_to_work/)  
11. pgEdge and PostgREST, accessed December 2, 2025, [https://www.pgedge.com/blog/pgedge-distributed-postgresql-and-postgrest](https://www.pgedge.com/blog/pgedge-distributed-postgresql-and-postgrest)  
12. Authentication — PostgREST devel documentation, accessed December 2, 2025, [https://docs.postgrest.org/en/latest/references/auth.html](https://docs.postgrest.org/en/latest/references/auth.html)  
13. The ultimate Supabase self-hosting Guide \- David Lorenz, accessed December 2, 2025, [https://activeno.de/blog/2023-08/the-ultimate-supabase-self-hosting-guide/](https://activeno.de/blog/2023-08/the-ultimate-supabase-self-hosting-guide/)  
14. Migrating from Supabase Auth to Better Auth, accessed December 2, 2025, [https://www.better-auth.com/docs/guides/supabase-migration-guide](https://www.better-auth.com/docs/guides/supabase-migration-guide)  
15. PostgreSQL | Better Auth, accessed December 2, 2025, [https://www.better-auth.com/docs/adapters/postgresql](https://www.better-auth.com/docs/adapters/postgresql)  
16. How to use RLS when using better-auth \- Supabase \- Answer Overflow, accessed December 2, 2025, [https://www.answeroverflow.com/m/1415118854014763139](https://www.answeroverflow.com/m/1415118854014763139)  
17. JWT \- Better Auth, accessed December 2, 2025, [https://www.better-auth.com/docs/plugins/jwt](https://www.better-auth.com/docs/plugins/jwt)