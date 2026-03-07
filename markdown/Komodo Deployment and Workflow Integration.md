# **Comprehensive Architectural Analysis and Implementation Strategy for Sovereign Infrastructure: Komodo, Pangolin, and Middleware Integration**

## **1\. Executive Summary and Strategic Context**

The contemporary landscape of self-hosted infrastructure has undergone a paradigm shift, moving away from fragmented, imperative scripting toward cohesive, declarative "Sovereign Stacks." This evolution is driven by a critical need for data sovereignty, architectural autonomy, and the mitigation of vendor lock-in risks associated with hyperscale cloud providers. In this context, the integration of **Komodo** (Infrastructure Orchestration), **Pangolin** (Tunneled Mesh Ingress), and **TinyAuth** (Identity Assurance) represents a sophisticated architectural pattern that rivals commercial Platform-as-a-Service (PaaS) offerings while operating entirely on user-controlled hardware.  
This report provides an exhaustive technical analysis and implementation roadmap for this specific stack. The primary objective is to define a unified, automated workflow where Komodo serves as the central "Source of Truth" (SoT), managing not only the application workloads but also the lifecycle of the underlying network fabric (Pangolin/Newt) and the security perimeter (TinyAuth/Middleware Manager).

### **1.1 The Operational Mandate**

The analysis addresses a complex set of operational requirements:

1. **Recursive Orchestration:** The capability of the orchestration engine (Komodo) to deploy and update its own distributed agents (Periphery), creating a self-sustaining management loop.  
2. **Zero-Trust Networking:** The establishment of a secure, tunneled ingress architecture using Pangolin and its Newt agent to eliminate open port vulnerabilities and bypass CGNAT (Carrier-Grade NAT) restrictions.  
3. **Identity-Aware Access:** The implementation of a robust authentication layer using TinyAuth, integrated via Pangolin's Middleware Manager to enforce Forward Authentication (ForwardAuth) protocols at the edge.  
4. **Unified Workflow Automation:** The synthesis of disparate configuration artifacts—Declarative Blueprints, TypeScript Actions, and Docker Compose YAML—into a single, atomic deployment procedure.

The ensuing sections deconstruct these components, analyzing their internal architectures, data flows, and integration points to provide a definitive guide for deploying this high-resilience infrastructure.

## ---

**2\. Architectural Deconstruction: The Komodo Orchestration Plane**

To understand the efficacy of the proposed workflow, one must first analyze the design philosophy and internal mechanics of the Komodo platform. Unlike traditional container management solutions that often rely on local state or simple API wrappers, Komodo employs a rigorous GitOps-centric architecture built on a "Core/Periphery" distributed model. This separation of concerns is fundamental to achieving the user's goal of self-managed infrastructure.

### **2.1 The Core-Periphery Dichotomy**

The Komodo architecture is bifurcated into two distinct operational roles: the **Core**, which serves as the decision-making brain, and the **Periphery**, which acts as the execution arm.

#### **2.1.1 Komodo Core: The State Engine**

Komodo Core is the centralized control plane. Hosted typically on a high-availability node or a stable VPS, it is responsible for maintaining the system's state, managing the database (supporting MongoDB, FerretDB, and PostgreSQL), and serving the user interface.1

* **State Reconciliation:** Core continuously polls linked Git repositories to detect changes in infrastructure definitions (Docker Compose files, configuration TOMLs). Upon detecting a divergence between the repository (Desired State) and the reported status from agents (Actual State), Core calculates the necessary diff operations—such as pulling a new image, restarting a container, or injecting updated environment variables.  
* **API and Webhooks:** Core acts as the ingress point for external triggers. It processes webhooks from Git providers (GitHub, GitLab, Gitea) to initiate deployment pipelines instantly upon code commits, a feature critical for the automated workflow discussed in Section 7\.2

#### **2.1.2 Komodo Periphery: The Privileged Agent**

The Periphery agent is a lightweight, compiled binary (written in Rust) that resides on every managed server. Its role is execution and telemetry. Unlike agentless systems that rely on SSH—which can be fragile and difficult to secure behind NAT—Periphery establishes a persistent, secure communication channel with Core.  
**Operational Capabilities:**

* **Docker Socket Control:** Periphery mounts the host's Docker socket (/var/run/docker.sock), granting it the authority to spawn, kill, and inspect sibling containers. This architecture allows Komodo to manage the very infrastructure it runs on, enabling the "self-managing" requirement.3  
* **System Telemetry:** By mounting the host's /proc directory, Periphery leverages system information libraries (likely sysinfo in Rust) to scrape granular metrics—CPU load, memory usage, disk I/O—and stream this data back to Core for alerting and visualization.4  
* **Secure Connectivity:** Communication between Core and Periphery is secured via a shared secret (KOMODO\_PASSKEY) and can be encapsulated in mTLS (Mutual TLS). This ensures that even if the Periphery port (default 8120\) is exposed, unauthorized actors cannot issue orchestration commands without the cryptographic keys.5

### **2.2 The Recursive Deployment Paradox**

A unique challenge identified in the research is the "Bootstrap Problem": How does one use Komodo to manage the Periphery agent that Komodo relies on to communicate with the server?  
The solution lies in Komodo’s ability to treat the Periphery agent as just another "Stack" (a Docker Compose deployment). By defining the Periphery configuration in a Git repository and importing it into Komodo Core as a Stack, we create a recursive management loop.  
**Mechanism of Action:**

1. **Initial State:** The administrator manually installs the Periphery agent on a server via CLI.  
2. **Adoption:** The administrator defines the Periphery's docker-compose.yaml in Git and creates a Stack in Komodo Core targeting that server.  
3. **The Handover:** When Komodo deploys this Stack, it essentially instructs the running Periphery agent to "redeploy yourself."  
4. **Graceful Replacement:** The Docker daemon handles the request. It pulls the new Periphery image, stops the old container, and starts the new one. Because the configuration (Passkeys, Certs) is persisted in host volumes (e.g., /etc/komodo), the new agent immediately reconnects to Core, completing the update cycle without severing administrative control.

This capability is pivotal for the "Single Workflow" requirement, as it ensures that updates to the orchestration layer itself can be automated alongside application deployments.

## ---

**3\. Network Fabric Analysis: Pangolin and the Tunneled Mesh**

With the orchestration plane established, the analysis turns to the connectivity layer. The user requirements specify the use of **Pangolin** and its **Newt** agent. This choice indicates a preference for a "Tunneled Mesh" architecture over traditional port-forwarding or VPN solutions.

### **3.1 The Pangolin Ingress Model**

Pangolin operates as a self-hosted alternative to Cloudflare Tunnels. It solves the problem of exposing services running in private networks (home labs, corporate intranets, VPCs without public IPs) to the public internet without opening inbound firewall ports.

#### **3.1.1 The Core Gateway**

Pangolin Core acts as the public-facing gateway. It terminates SSL connections using automatically provisioned Let's Encrypt certificates and enforces access control policies. It listens for incoming tunnel connections from Newt agents, effectively reversing the traditional connection model. Instead of the internet connecting *in* to the server, the server connects *out* to Pangolin.7

#### **3.1.2 The Newt Agent: User-Space WireGuard**

The Newt agent is the linchpin of this architecture. It is a user-space application that establishes a WireGuard tunnel to the Pangolin Core.

* **User-Space Advantage:** Unlike kernel-level WireGuard, Newt does not require root privileges to modify network interfaces (though it often runs as root in Docker for convenience). It creates a virtualized network path, encapsulating traffic from the Pangolin Core and forwarding it to specific containers within the Docker network.8  
* **Multiplexing:** A single Newt connection can route traffic for multiple distinct services based on subdomains. This allows for a "Sidecar" deployment pattern, where a single Newt agent services an entire Docker Compose stack, or a "Router" pattern, where one Newt agent services an entire host.

### **3.2 Declarative Configuration: The Blueprint System**

Integration with Komodo relies heavily on Pangolin's "Blueprint" feature. Blueprints allow administrators to define the routing and access rules declaratively, rather than configuring them in the Pangolin UI.  
**Configuration Vectors:**

1. **YAML Definition:** A standalone file defining resources, upstream targets, and auth policies.  
2. **Docker Labels:** The preferred method for Komodo integration. By adding specific labels to a container in the Docker Compose file (e.g., pangolin.resource.name=MyApp), the Newt agent automatically detects the service and registers it with the Core. This adheres strictly to the "Infrastructure as Code" (IaC) philosophy managed by Komodo.9

**Table 3.1: Comparison of Configuration Methods**

| Feature | Pangolin UI | YAML Blueprint | Docker Labels (Recommended) |
| :---- | :---- | :---- | :---- |
| **Source of Truth** | Database (Opaque) | File System | Git (via Komodo) |
| **Automation** | Low (Manual Click) | Medium (File Sync) | High (Auto-Discovery) |
| **Coupling** | Decoupled from App | Decoupled from App | Tightly Coupled with App |
| **Komodo Synergy** | Low | Medium | **Maximum** |

## ---

**4\. Security Architecture: Identity Assurance and Middleware**

The mere exposure of services via Pangolin/Newt is insufficient; they must be secured. The research materials point to a specific integration pattern involving **TinyAuth** and the **Pangolin Middleware Manager**. This section analyzes how these components enforce a Zero Trust model.

### **4.1 TinyAuth: The Forward Authentication Provider**

TinyAuth is a minimalist authentication service designed to integrate with reverse proxies like Traefik (which underpins Pangolin). It implements the "ForwardAuth" protocol.  
**The ForwardAuth Protocol:**

1. **Interception:** An incoming request reaches Pangolin (Traefik).  
2. **Delegation:** Traefik pauses the request and sends a verification sub-request to TinyAuth.  
3. **Verification:** TinyAuth checks for a valid session cookie.  
   * *If Valid:* TinyAuth returns HTTP 200 OK, potentially injecting headers like X-User.  
   * *If Invalid:* TinyAuth returns HTTP 401/302, redirecting the user's browser to the TinyAuth login portal.11  
4. **Resumption:** Upon successful login, the user is redirected back to the original resource.

Operational Simplicity:  
Unlike complex IdPs (Identity Providers) like Keycloak, TinyAuth is designed for "homelab" scale, using a simple flat-file or environment-variable based user database (username:hash). This makes it ideal for embedding directly into a Komodo stack without external dependencies.

### **4.2 The Middleware Manager**

Pangolin abstracts Traefik, but advanced users often need direct access to Traefik's middleware features (Rate Limiting, IP Whitelisting, ForwardAuth). The **Middleware Manager** is a helper microservice that bridges this gap.

* **Function:** It provides a UI and API to define Traefik middlewares. It writes these definitions to a dynamic configuration file (YAML) that is mounted into the Pangolin/Traefik container.  
* **Integration:** By defining a forwardAuth middleware in the Manager pointing to the TinyAuth container, administrators can create a reusable security object. This object can then be referenced in Pangolin Blueprints (e.g., pangolin.resource.middleware=tinyauth-protection), effectively applying the security policy to any resource managed by Komodo.12

## ---

**5\. Technical Outline: Recursive Deployment of Komodo Periphery**

This section addresses the user's first specific technical request: a guide to using Komodo to manage its own Periphery. This procedure transforms the "Pet" server (manually managed) into "Cattle" (automated).

### **5.1 Prerequisite Configuration**

Before automation can take over, the bootstrap environment must be correctly configured.

* **Docker Network:** The Periphery container needs to reside on a network that allows outbound access to the Komodo Core URL.  
* **Volume Persistence:** The /etc/komodo directory on the host must be preserved. This directory contains the generated unique ID of the agent. If this is lost during a redeployment, the agent will appear as a *new* server in Core, breaking the continuity.

### **5.2 The Deployment Workflow**

The following steps outline the transformation of the manual Periphery install into a Komodo-managed Stack.

#### **Step 1: Git Repository Preparation**

Create a dedicated Git repository (e.g., infrastructure-live) to house the infrastructure definitions. Create a directory periphery/ and add the docker-compose.yaml.

#### **Step 2: The Compose Definition**

This YAML definition is critical. It must use environment variables for sensitive data (Passkeys) to allow Komodo to inject them securely.

YAML

\# infrastructure-live/periphery/docker-compose.yaml  
services:  
  periphery:  
    \# Use a variable for the tag to allow controlled upgrades via Komodo  
    image: ghcr.io/moghtech/komodo-periphery:${KOMODO\_IMAGE\_TAG:-latest}  
    container\_name: komodo-periphery  
    restart: unless-stopped  
      
    \# CRITICAL: This label prevents Komodo from stopping this container  
    \# during "Stop All" actions, which would kill the management connection.  
    labels:  
      \- "komodo.skip=true"  
        
    environment:  
      \- PERIPHERY\_ROOT\_DIRECTORY=/etc/komodo  
      \- KOMODO\_HOST=${KOMODO\_CORE\_URL}  
      \- PERIPHERY\_PASSKEYS=${KOMODO\_SHARED\_PASSKEY}  
      \- PERIPHERY\_SSL\_ENABLED=true  
      \# Disable remote terminal for security if not needed  
      \- PERIPHERY\_DISABLE\_TERMINALS=false  
        
    volumes:  
      \# The Agent requires control over the Docker Daemon  
      \- /var/run/docker.sock:/var/run/docker.sock  
      \# Read-only access to host processes for metrics  
      \- /proc:/proc:ro  
      \# Persistence for Agent Identity and SSL Certs  
      \- /etc/komodo:/etc/komodo  
        
    logging:  
      driver: "json-file"  
      options:  
        max-size: "10m"  
        max-file: "3"

#### **Step 3: Stack Configuration in Core**

1. Navigate to the **Stacks** menu in Komodo Core.  
2. Create a new Stack named Self-Managed-Periphery.  
3. **Repository:** Connect the infrastructure-live repo.  
4. **Variables:** Define the variables used in the Compose file:  
   * KOMODO\_CORE\_URL: https://core.yourdomain.com (or internal IP).  
   * KOMODO\_SHARED\_PASSKEY: The secure key defined in Core settings.  
   * KOMODO\_IMAGE\_TAG: v1.16.1 (or latest).  
5. **Deploy:** Execute the deployment. Komodo will pull the repo, inject variables, and instruct the *existing* Periphery agent to replace itself with the *new* configuration defined in Git.

Risk Mitigation:  
The transition is almost instantaneous, but if the new configuration is invalid (e.g., wrong Passkey), the agent will fail to reconnect. Recommendation: Always test the configuration on a staging server before applying it to the Core server's own agent.

## ---

**6\. Technical Outline: Pangolin, Newt, and TinyAuth Integration**

This section synthesizes the remaining components into the requested "single workflow." The goal is to deploy an application stack that is automatically connected to the internet via a secure tunnel and protected by authentication, without manual configuration steps.

### **6.1 The Infrastructure Bootstrap (One-Time Setup)**

Before individual apps can be deployed, the shared infrastructure (Pangolin Core, TinyAuth, Middleware Manager) must be established. This is done via a "Base Infrastructure" Stack in Komodo.  
**Base Infrastructure Compose (Snippet):**

YAML

services:  
  \# Pangolin Core (The Tunnel Server)  
  pangolin:  
    image: fosrl/pangolin:latest  
    volumes:  
      \-./pangolin-data:/data  
      \-./pangolin-config:/config  
      \# Shared volume for Middleware Manager to write config  
      \- pangolin\_traefik\_dynamic:/app/traefik/dynamic

  \# TinyAuth (The Auth Provider)  
  tinyauth:  
    image: ghcr.io/steveiliop56/tinyauth:v4  
    environment:  
      \- APP\_URL=https://auth.yourdomain.com  
      \# Users format: username:bcrypt\_hash  
      \- USERS=admin:$2y$10$hashed\_secret...  
    labels:  
      \# Self-expose TinyAuth via Pangolin so users can login  
      \- "pangolin.resource.name=TinyAuth"  
      \- "pangolin.resource.domain=auth.yourdomain.com"  
      \- "pangolin.resource.target=http://tinyauth:3000"

  \# Middleware Manager (The Config Injector)  
  middleware-manager:  
    image: hhftechnology/middleware-manager:latest  
    environment:  
      \- PANGOLIN\_API\_URL=http://pangolin:3001  
    volumes:  
      \# Writes the ForwardAuth config here for Pangolin to read  
      \- pangolin\_traefik\_dynamic:/traefik-config

volumes:  
  pangolin\_traefik\_dynamic:

Configuration Action:  
Once deployed, log in to the Middleware Manager UI and create a Middleware object:

* **Name:** secure-access  
* **Type:** forwardAuth  
* **Address:** http://tinyauth:3000/verify (Internal Docker network address)  
* **TrustForwardHeader:** true

This creates the reusable security policy.

### **6.2 The Unified Workflow: "Deploy-with-Tunnel" Procedure**

The user requests a method to integrate these components into a "single workflow." The primary obstacle is that the Newt agent requires unique credentials (ID/Secret) which are generated by the Pangolin API. We can automate this using **Komodo Actions** (TypeScript automation) and **Procedures**.

#### **6.2.1 The TypeScript Automation Script**

We create a Komodo **Action** named Provision-Pangolin-Tunnel. This script interacts with the Pangolin API to generate a new Site (tunnel endpoint) whenever a new stack is deployed.  
**Script Logic Analysis:**

1. **Trigger:** The script accepts an argument (e.g., STACK\_NAME).  
2. **API Call:** It sends a POST request to the Pangolin Core API (/api/v1/sites) creating a site named STACK\_NAME.  
3. **Extraction:** It parses the JSON response to retrieve the id and secret for the Newt agent.  
4. **Injection:** It uses the Komodo Client SDK to update the **Stack Variables** of the target stack, injecting NEWT\_ID and NEWT\_SECRET.14

*Note: While the specific API endpoints for Pangolin were not accessible in the snippets, the standard pattern for such integrations involves RESTful interaction authenticated via an API token.*

#### **6.2.2 The Stack Definition (Sidecar Pattern)**

The application stack in Git is defined with a "Sidecar" Newt container. It relies on the variables injected by the Action.

YAML

\# my-app/docker-compose.yaml  
services:  
  \# The Application Workload  
  wiki:  
    image: wikijs/wikijs:2  
    container\_name: wiki  
    labels:  
      \# Pangolin Blueprint: Define the public route  
      \- "pangolin.resource.name=Wiki"  
      \- "pangolin.resource.domain=wiki.yourdomain.com"  
      \- "pangolin.resource.target=http://wiki:3000"  
      \# Security: Apply the TinyAuth middleware defined in Step 6.1  
      \- "pangolin.resource.middlewares=secure-access"

  \# The Network Sidecar (Newt)  
  tunnel:  
    image: fosrl/newt:latest  
    restart: unless-stopped  
    environment:  
      \# Variables populated by the Komodo Action  
      \- NEWT\_ID=${NEWT\_ID}  
      \- NEWT\_SECRET=${NEWT\_SECRET}  
      \- PANGOLIN\_ENDPOINT=https://pangolin.yourdomain.com  
    volumes:  
      \# Discovery: Reads the labels from the 'wiki' container  
      \- /var/run/docker.sock:/var/run/docker.sock:ro

#### **6.2.3 The Execution Procedure**

Finally, we define a **Komodo Procedure** to bind it all together. This represents the "Single Workflow."  
**Procedure: Deploy Secure App**

1. **Stage 1 (Provision):** Run Action Provision-Pangolin-Tunnel with arg wiki-stack.  
   * *Result:* Komodo talks to Pangolin, gets keys, updates Stack Variables.  
2. **Stage 2 (Deploy):** Deploy Stack wiki-stack.  
   * *Result:* Komodo pulls the Compose file, injects the new keys, and starts the containers.  
3. **Stage 3 (Verify):** (Optional) Run Action Check-Tunnel-Health.

**Outcome:** The administrator clicks one button. The system generates unique credentials, deploys the app, establishes a secure encrypted tunnel, and applies ForwardAuth security policies automatically.

## ---

**7\. Resilience, Observability, and Failure Modes**

Implementing such a complex stack requires understanding potential failure points. This section analyzes critical operational risks.

### **7.1 The "Split-Brain" Tunnel Risk**

If a Stack is deleted in Komodo but the Site is not deleted in Pangolin, "zombie" sites accumulate. Conversely, if the Newt container is recreated (e.g., during an update), it must reuse the same credentials.

* **Solution:** The workflow defined in 6.2.1 persists credentials into Komodo Variables. This ensures that when the stack redeploys, it reuses the *existing* NEWT\_ID, preventing the creation of duplicate tunnels. A "Teardown" Procedure should be created to call the Pangolin API and DELETE the site when the stack is destroyed.

### **7.2 Security of the Docker Socket**

Both the Komodo Periphery and the Newt Agent require access to /var/run/docker.sock. This is a high-privilege interface; access to it is equivalent to root access on the host.

* **Mitigation Strategy:**  
  * **Newt:** Use the :ro (read-only) flag for the Newt container volume mount (/var/run/docker.sock:/var/run/docker.sock:ro). Newt only needs to *read* labels to configure Blueprints; it does not need to execute commands.16  
  * **Periphery:** Must have write access. Mitigation involves network segmentation (VLANs) and strictly controlling access to the Komodo Core UI via Multi-Factor Authentication (MFA) or OIDC (e.g., Keycloak), as a compromise of Core leads to a compromise of all Periphery agents.14

### **7.3 Database Schema Volatility**

Research indicates that the dev branches of Komodo often introduce breaking database schema changes.17

* **Operational Discipline:** Never set image tags to latest in a production environment. Pin versions (e.g., ghcr.io/moghtech/komodo-core:v1.16) in your Stack definitions. Create a "Maintenance Procedure" in Komodo that backups the MongoDB database before pulling new images, ensuring a rollback path exists if a schema migration fails.

## ---

**8\. Conclusion**

The integration of Komodo, Pangolin, and TinyAuth creates a formidable "Sovereign Stack" that addresses the trifecta of modern infrastructure needs: Orchestration, Connectivity, and Security. By leveraging the recursive deployment capabilities of Komodo, administrators can manage the management layer itself, ensuring the system is self-updating and resilient. The automated provisioning of Newt tunnels via Komodo Actions eliminates the friction of manual credential handling, while the Blueprint system ensures that network configuration is version-controlled alongside application code. Finally, the inclusion of TinyAuth via the Middleware Manager provides a robust, zero-trust security perimeter that travels with the application, regardless of the underlying network topology. This architecture represents a mature, enterprise-grade approach to self-hosting.

#### **Works cited**

1. accessed December 5, 2025, [https://raw.githubusercontent.com/moghtech/komodo/main/compose/mongo.compose.yaml](https://raw.githubusercontent.com/moghtech/komodo/main/compose/mongo.compose.yaml)  
2. Streamline Your Deployments : Komodo \+ GitHub Webhooks | by Rishav Kapil | Medium, accessed December 5, 2025, [https://medium.com/@rishavkapil61/streamline-your-deployments-komodo-github-webhooks-51d4d3a04891](https://medium.com/@rishavkapil61/streamline-your-deployments-komodo-github-webhooks-51d4d3a04891)  
3. Komodo (komo.do): A Build & Deployment System for Docker/Compose | by mario marco, accessed December 5, 2025, [https://medium.com/@mariomarco08/komodo-komo-do-a-build-deployment-system-for-docker-compose-9470136d5751](https://medium.com/@mariomarco08/komodo-komo-do-a-build-deployment-system-for-docker-compose-9470136d5751)  
4. \[HELP\] komodo-periphery memory leak · Issue \#203 \- GitHub, accessed December 5, 2025, [https://github.com/mbecker20/komodo/issues/203](https://github.com/mbecker20/komodo/issues/203)  
5. Ansible role for simplified deployment of Komodo with systemd \- GitHub, accessed December 5, 2025, [https://github.com/bpbradley/ansible-role-komodo](https://github.com/bpbradley/ansible-role-komodo)  
6. Has anyone managed to figure out periphery for komodo? : r/selfhosted \- Reddit, accessed December 5, 2025, [https://www.reddit.com/r/selfhosted/comments/1j5ydbx/has\_anyone\_managed\_to\_figure\_out\_periphery\_for/](https://www.reddit.com/r/selfhosted/comments/1j5ydbx/has_anyone_managed_to_figure_out_periphery_for/)  
7. fosrl/pangolin: Identity-Aware Tunneled Reverse Proxy Server with Dashboard UI \- GitHub, accessed December 5, 2025, [https://github.com/fosrl/pangolin](https://github.com/fosrl/pangolin)  
8. fosrl/newt: Pangolin tunneled site & network connector \- GitHub, accessed December 5, 2025, [https://github.com/fosrl/newt](https://github.com/fosrl/newt)  
9. Infrastructure-as-Code for Proxies: Pangolin Blueprints with YAML and Docker Labels, accessed December 5, 2025, [https://pangolin.net/blog/posts/blueprints](https://pangolin.net/blog/posts/blueprints)  
10. Pangolin 1.10.2: Declarative configs & Docker labels, multi-site failover, path-based routing, and more : r/selfhosted \- Reddit, accessed December 5, 2025, [https://www.reddit.com/r/selfhosted/comments/1nnry8a/pangolin\_1102\_declarative\_configs\_docker\_labels/](https://www.reddit.com/r/selfhosted/comments/1nnry8a/pangolin_1102_declarative_configs_docker_labels/)  
11. Getting Started \- Tinyauth, accessed December 5, 2025, [https://tinyauth.app/docs/getting-started/](https://tinyauth.app/docs/getting-started/)  
12. Middleware Manager \- Pangolin Docs, accessed December 5, 2025, [https://docs.pangolin.net/self-host/community-guides/middlewaremanager](https://docs.pangolin.net/self-host/community-guides/middlewaremanager)  
13. Middleware Manager for your Pangolin Deployment- Update with Adds Features & Fixes : r/selfhosted \- Reddit, accessed December 5, 2025, [https://www.reddit.com/r/selfhosted/comments/1k2856d/middleware\_manager\_for\_your\_pangolin\_deployment/](https://www.reddit.com/r/selfhosted/comments/1k2856d/middleware_manager_for_your_pangolin_deployment/)  
14. Advanced Configuration \- Komodo, accessed December 5, 2025, [https://komo.do/docs/setup/advanced](https://komo.do/docs/setup/advanced)  
15. Resources \- Komodo, accessed December 5, 2025, [https://komo.do/docs/resources](https://komo.do/docs/resources)  
16. Docker Network and Service Configuration for newt if you are getting Bad Gateway \- Reddit, accessed December 5, 2025, [https://www.reddit.com/r/PangolinReverseProxy/comments/1mufupo/docker\_network\_and\_service\_configuration\_for\_newt/](https://www.reddit.com/r/PangolinReverseProxy/comments/1mufupo/docker_network_and_service_configuration_for_newt/)  
17. Releases · moghtech/komodo \- GitHub, accessed December 5, 2025, [https://github.com/moghtech/komodo/releases](https://github.com/moghtech/komodo/releases)