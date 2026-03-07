

# **Orchestrating Distributed Infrastructure: A Technical Analysis of Komodo v2 (Dev-90), Ansible Automation, and Identity-Aware Ingress**

## **1\. Introduction: The Evolution of Self-Hosted Orchestration**

The landscape of self-hosted infrastructure management has historically been fragmented, often necessitating a patchwork of disparate tools to achieve what hyperscale cloud providers offer as a cohesive platform. In the context of Docker-based environments, administrators have traditionally relied on monolithic management interfaces or bare-metal CLI interactions, both of which struggle to scale efficiently across multi-server topologies. The emergence of **Komodo**, particularly its second major version iteration (v2), represents a significant architectural shift towards a distributed, API-first orchestration model designed to bridge the gap between simple container management and complex infrastructure-as-code (IaC) workflows.1  
This report provides an exhaustive technical analysis of deploying the **Komodo v2 ecosystem**, specifically focusing on the v2.0.0-dev-90 release candidate.3 This specific build serves as a critical case study due to its introduction of hardened database schemas, enhanced telemetry capabilities, and strict image tagging conventions that differentiate it from the v1 lineage. Furthermore, effective orchestration at scale requires robust automation; thus, this analysis deeply investigates the **ansible-role-komodo** (specifically the komodo\_v2 branch maintained by Brian Bradley), which shifts the deployment paradigm of the Komodo Periphery agent from a containerized workload to a highly integrated, systemd-managed system service.4  
To complete the architectural picture, the report integrates **Pangolin**, a modern identity-aware reverse proxy.5 By layering Pangolin’s tunneling and authentication capabilities over Komodo’s management plane, administrators can establish a zero-trust ingress model, securing the critical control plane against unauthorized access while simplifying the network topology required for multi-node communication.  
The following sections will deconstruct the architectural requirements, detailed deployment procedures, and automation logic required to synthesize these components into a unified, resilient infrastructure platform. This analysis assumes a professional DevOps context, prioritizing reproducibility, security, and scalability over ease of initial setup.

## **2\. Architectural Foundations of the Komodo v2 Ecosystem**

Understanding the deployment mechanics of Komodo v2 requires a foundational grasp of its bipartite architecture, which decouples the management state from execution logic. This separation is the defining characteristic that allows Komodo to scale beyond the limitations of traditional, agentless Docker management tools.

### **2.1 The Core-Periphery Dichotomy**

The architecture is predicated on two primary components: **Komodo Core** and **Komodo Periphery**. This design mirrors the "control plane/data plane" separation found in Kubernetes and other distributed systems, though tailored specifically for Docker Compose workflows.

#### **2.1.1 Komodo Core: The State Engine**

Komodo Core acts as the centralized monolith responsible for state management, API exposure, and user interaction. It serves as the authoritative source of truth for the entire infrastructure.1

* **Functionality:** The Core hosts the web UI, processes API requests, manages the persistent database (MongoDB), and orchestrates deployment procedures. It does not directly execute Docker commands on remote hosts; instead, it delegates these tasks to the Periphery agents.  
* **State Management:** All configurations—Stacks, Repositories, Users, and Alerts—are stored in the Core's database. This centralization simplifies backup and recovery strategies, as securing the Core's data volume effectively secures the configuration state of the entire fleet.7  
* **Connectivity:** The Core requires ingress connectivity (typically HTTP/HTTPS) to serve the UI and receive webhooks from git providers (e.g., GitHub, Gitea).8 Crucially, in the v2 architecture, the Core actively establishes connections to Periphery agents (a "pull" model from the perspective of the operator, but technically the Core initiates the request to the agent's API).1

#### **2.1.2 Komodo Periphery: The Execution Agent**

The Periphery is a lightweight, stateless agent deployed on every server managed by Komodo—including the server hosting the Core itself.1

* **Statelessness:** The Periphery agent maintains minimal local state. Its primary function is to translate API directives from the Core into local system calls (e.g., docker compose up, git pull, cat /proc/meminfo).  
* **Execution Scope:** The Periphery binary interacts with the host's Docker socket (or Podman equivalent) to manage containers. It also manages local git repositories used for building images.  
* **Security Model:** The Periphery exposes a REST API (typically on port 8120\) protected by a passkey. The Core must present this passkey to authenticate its requests. In v2, this authentication is bidirectional in concept; the Periphery allows the Core to connect, but the Core must also be configured with the correct passkey to authorize the connection.9  
* **Telemetry:** A significant enhancement in v2 (and specifically noted in the komodo\_v2 Ansible role variables) is the integration of OpenTelemetry (OTLP). The Periphery can stream traces and metrics to an external collector, providing granular observability into infrastructure performance beyond simple CPU/RAM usage.4

### **2.2 The Database Transformation: MongoDB vs. FerretDB v2**

One of the most critical architectural decisions in deploying Komodo v2 (dev-90) is the choice of the backing store. The transition from v1 to v2 introduced significant changes to the database layer, driven by performance requirements and licensing considerations.

#### **2.2.1 Native MongoDB (Recommended)**

For the majority of deployments, **MongoDB** is the standard and recommended database.11 Komodo Core uses the MongoDB driver to interact with the database.

* **Version Requirements:** Recent MongoDB versions (5.0+) impose strict hardware requirements, specifically the support for AVX (Advanced Vector Extensions) instruction sets on x86\_64 processors.13 This can be a blockage for users running on older hardware (e.g., Celeron J-series, older Pentiums) or certain virtualization platforms that do not pass through these CPU flags.14  
* **Compatibility:** Native MongoDB ensures full compatibility with Komodo's complex relational mapping between Users, Repos, and Stacks. The dev-90 release and its predecessors in the v2 cycle have optimized queries specifically for the MongoDB document model.

#### **2.2.2 FerretDB v2: The SQL Alternative**

For users who cannot run MongoDB due to hardware constraints (lack of AVX) or licensing preferences (avoiding SSPL), Komodo supports **FerretDB**. However, the v2 release cycle introduced a **breaking change**: support for FerretDB v1 (which used a standard PostgreSQL backend) was dropped in favor of **FerretDB v2**.15

* **Backend Shift:** FerretDB v2 requires a specialized backend: postgres-documentdb (an extension for PostgreSQL that enables native BSON handling). This is *not* compatible with standard PostgreSQL instances.17  
* **Migration Complexity:** Upgrading from a v1 installation using SQLite or FerretDB v1 to Komodo v2 requires a complex migration process involving a komodo-util container to transcode data to the new schema.18  
* **Architectural Implication:** For a fresh deployment of dev-90, attempting to use the legacy SQLite or standard Postgres configurations will result in startup failures. The operator must strictly choose between Native MongoDB or the FerretDB v2 \+ postgres-documentdb combination.

### **2.3 Pangolin: Identity-Aware Ingress**

Pangolin complements the Komodo architecture by serving as the secure entry point. Unlike a standard reverse proxy (Nginx, Traefik) that simply routes traffic based on hostnames, Pangolin acts as an **identity-aware proxy** with tunneling capabilities.5

* **Fossorial Components:** Pangolin's architecture consists of the "Pangolin" dashboard/control plane, "Gerbil" (the server-side tunnel endpoint), and "Newt" (the client-side tunneling agent).5  
* **Tunneled Access:** For distributed Komodo deployments where Periphery agents reside behind restrictive firewalls or CGNAT, Pangolin's tunneling (via WireGuard) allows the Core to communicate with Periphery agents without exposing ports 8120 to the public internet.  
* **Context-Aware Access:** Pangolin enforces access control policies (OIDC, MFA) *before* traffic reaches Komodo Core, adding a defense-in-depth layer critical for infrastructure management interfaces.6

---

## **3\. Deployment Strategy: Komodo Core v2 (Release dev-90)**

Deploying the dev-90 release of Komodo Core requires meticulous attention to the docker-compose configuration. The "dev" nomenclature implies a rolling release channel, but specific snapshots like dev-90 often contain database schema changes that serve as checkpoints in the development process.

### **3.1 Prerequisite Requirements**

Before initiating the deployment, the hosting environment must meet specific criteria to support the Komodo stack.

| Component | Requirement | Context |
| :---- | :---- | :---- |
| **CPU** | x86\_64 (with AVX) or ARM64 | Essential for MongoDB 5.0+ and komodo-core binaries.13 |
| **OS** | Linux (Debian/Ubuntu preferred) | The ansible-role-komodo is optimized for Debian-like systems.4 |
| **Container Engine** | Docker Engine 24+ & Compose v2 | Komodo relies heavily on docker compose CLI commands.2 |
| **Network** | Ports 80, 443, 9120, 8120 | 9120 for Core UI, 8120 for local Periphery, 80/443 for Pangolin.20 |

### **3.2 The docker-compose.yaml Configuration**

The following configuration represents a production-ready definition for Komodo Core v2 (dev-90), incorporating the native MongoDB backend and the requisite environment variables for v2 operation. This configuration is synthesized from the official Moghtech examples and community adaptations for v2 stability.10

YAML

services:  
  \# \---------------------------------------------------------------------------  
  \# Komodo Core: The Central Management Plane  
  \# \---------------------------------------------------------------------------  
  core:  
    \# Pinning to the 2-dev tag. Ideally, one should use the specific SHA digest   
    \# for dev-90 if immutability is required, as dev tags roll forward.  
    image: ghcr.io/moghtech/komodo-core:2-dev  
    container\_name: komodo-core  
    restart: unless-stopped  
      
    \# Core listens on 9120 by default.   
    \# In a Pangolin setup, this might not need to be exposed publicly,   
    \# but is exposed here for initial bootstrapping.  
    ports:  
      \- "9120:9120"  
      
    \# Environment variables override config.toml defaults.  
    environment:  
      \# \--- General Configuration \---  
      \- KOMODO\_HOST=https://komodo.example.com  
      \- KOMODO\_TITLE=Komodo Orchestrator  
      \- TZ=Etc/UTC  
        
      \# \--- Database Connection (MongoDB) \---  
      \# V2 Core uses the MongoDB driver.  
      \- KOMODO\_DATABASE\_ADDRESS=mongo:27017  
      \- KOMODO\_DATABASE\_USERNAME=komodo  
      \- KOMODO\_DATABASE\_PASSWORD=secure\_db\_password  
      \- KOMODO\_DATABASE\_DB\_NAME=komodo  
        
      \# \--- Security & Authentication \---  
      \# The Passkey is the shared secret for Core\<-\>Periphery communication.  
      \# In v2, this is critical for the initial handshake.  
      \- KOMODO\_PASSKEY=super\_secure\_shared\_secret  
        
      \# \--- OIDC Integration (Optional) \---  
      \# Enabled for external identity providers (Keycloak, Google, GitHub).  
      \- KOMODO\_OIDC\_ENABLED=false  
      \# \- KOMODO\_OIDC\_CLIENT\_ID=...  
      \# \- KOMODO\_OIDC\_PROVIDER=...  
        
    volumes:  
      \# Persistent configuration file (optional but recommended for complex configs)  
      \-./config/core.config.toml:/config/config.toml  
      \# SSH keys for accessing private Git repositories  
      \-./ssh-keys:/home/nonroot/.ssh  
      
    depends\_on:  
      mongo:  
        condition: service\_started

  \# \---------------------------------------------------------------------------  
  \# MongoDB: The State Store  
  \# \---------------------------------------------------------------------------  
  mongo:  
    \# Using MongoDB 6.0 based on Komodo recommendations.   
    \# Warning: Requires AVX CPU support. Use mongo:4.4 if on older hardware.  
    image: mongo:6.0  
    container\_name: komodo-mongo  
    restart: unless-stopped  
    command:  
    environment:  
      \- MONGO\_INITDB\_ROOT\_USERNAME=root  
      \- MONGO\_INITDB\_ROOT\_PASSWORD=root\_password  
      \# This creates the initial user/db for Komodo  
      \- MONGO\_INITDB\_DATABASE=komodo  
    volumes:  
      \- mongo\_data:/data/db  
      \- mongo\_config:/data/configdb

  \# \---------------------------------------------------------------------------  
  \# Local Periphery: Managing the Core Server  
  \# \---------------------------------------------------------------------------  
  \# Even the Core server needs a Periphery agent to manage itself   
  \# (Self-hosting orchestration).  
  periphery:  
    image: ghcr.io/moghtech/komodo-periphery:2-dev  
    container\_name: komodo-periphery  
    restart: unless-stopped  
    environment:  
      \# The agent must know where to find the Core to report telemetry  
      \- KOMODO\_HOST=http://core:9120  
      \# Must match the KOMODO\_PASSKEY in Core  
      \- KOMODO\_PASSKEY=super\_secure\_shared\_secret  
      \# Identification  
      \- KOMODO\_SERVER\_NAME=Local-Core-Node  
    volumes:  
      \# Access to the Docker socket is mandatory for Periphery functionality  
      \- /var/run/docker.sock:/var/run/docker.sock  
      \# Storage for locally cloned repos and build artifacts  
      \- /etc/komodo:/etc/komodo  
    depends\_on:  
      \- core

volumes:  
  mongo\_data:  
  mongo\_config:

### **3.3 Deep Analysis of Configuration Parameters**

#### **3.3.1 Image Tagging and Stability Risks**

The release notes for dev-90 highlight a critical operational risk: **Database Schema Instability**. The dev branch is fluid. When the container updates from dev-90 to a newer build (e.g., dev-93), the application may automatically apply database schema migrations. If these migrations are buggy or if the administrator wishes to rollback, the database may be in an incompatible state.3

* **Mitigation:** The dev-90 release includes a CLI tool within the core container: km. Specifically, the command km database v1-downgrade is provided to revert schema changes if a rollback to v1 or an earlier v2 snapshot is necessary.  
* **Best Practice:** In a production environment utilizing dev tags, it is advisable to pin the image by its SHA256 digest (e.g., ghcr.io/moghtech/komodo-core@sha256:f4986...) to prevent unintended schema upgrades during a docker compose pull.22

#### **3.3.2 The core.config.toml vs. Environment Variables**

While the example above uses environment variables, Komodo v2 strongly advocates for the use of a TOML configuration file (core.config.toml) mounted into the container. This approach is superior for managing complex OIDC configurations and nested structures that are cumbersome to represent as flat environment variables.9

* **OIDC Configuration:** When enabling OIDC (e.g., with Keycloak), the host parameter in core.config.toml becomes critical. It determines the redirect URI (\<KOMODO\_HOST\>/auth/oidc/callback) that must be registered with the identity provider. A mismatch here will cause authentication loops.

#### **3.3.3 The Local Periphery**

The inclusion of a periphery service in the docker-compose.yml is essential for the "self-management" capability. Without this local agent, Komodo Core can manage *other* servers but cannot manage the stack it runs on. By deploying a local agent, Komodo can auto-update itself—a meta-circular capability that defines advanced orchestration platforms.24  
---

## **4\. Automating Infrastructure: The komodo\_v2 Ansible Role**

While Docker Compose is sufficient for the Core, manually deploying Periphery agents to dozens of servers is inefficient. The **ansible-role-komodo** (specifically the komodo\_v2 branch) provides a sophisticated automation framework that shifts the Periphery deployment model from containerization to **systemd-managed binaries**.4

### **4.1 Architectural Shift: Systemd vs. Docker**

The komodo\_v2 Ansible role deliberately installs the Periphery agent as a binary service directly on the host OS, managed by Systemd. This offers several distinct advantages over the Docker-based deployment used in the Core setup:

1. **Reduced Overhead:** Eliminates the container runtime overhead for the agent itself.  
2. **Simplified Socket Access:** Avoids the complexities of mounting /var/run/docker.sock into a container, which can sometimes be complicated by SELinux or AppArmor profiles.  
3. **User Isolation:** The role creates a dedicated komodo user. This user is granted membership in the docker group, allowing it to manage containers without running the agent process as root. This adheres to the Principle of Least Privilege.4

### **4.2 The Ansible Configuration Workflow**

To utilize this role, the control node (where Ansible runs) must be configured with specific inventory variables that drive the automation logic.

#### **4.2.1 Installation**

The role is installed via Ansible Galaxy, but one must ensure the komodo\_v2 branch is targeted if pulling from source, or the latest version if using the package manager.

Bash

ansible-galaxy role install bpbradley.komodo

#### **4.2.2 Inventory Variable Reference**

The following table details the critical variables required to configure the role for a dev-90 environment. These variables effectively program the automation behavior.4

| Variable Name | Default / Example | Function & Implication |
| :---- | :---- | :---- |
| komodo\_version | "2-dev" | **Crucial:** Sets the version of the binary to download. Must match the Core version to avoid API incompatibilities. |
| komodo\_action | "install" | Controls the playbook mode (install, update, uninstall). |
| enable\_server\_management | true | **Automation Key:** If true, the playbook contacts the Core API to register the server automatically. |
| komodo\_core\_url | "https://komodo.example.com" | The endpoint the Periphery will use to connect to Core. |
| komodo\_passkeys | \["secret\_key"\] | A list of valid passkeys injected into periphery.config.toml. Core must use one of these to connect. |
| komodo\_logging\_level | "info" | Sets the verbosity of the agent logs in Systemd journal. |
| komodo\_user | "komodo" | The system user created to run the service. |
| komodo\_group | "docker" | The group assignment allowing the agent to execute Docker commands. |

#### **4.2.3 Server Management: The "Auto-Registration" Feature**

A standout feature of the komodo\_v2 role is enable\_server\_management. In a standard manual setup, an administrator must install the agent, then log into the Komodo UI, click "Add Server," and type in the IP and passkey.  
The Ansible role automates this loop:

1. **Check:** It queries the Core API (using provided API credentials) to see if a server with the hostname already exists.  
2. **Register:** If not, it sends a POST request to create the server, automatically populating the ip\_address and passkey fields.  
3. Update: If the server exists but the IP has changed, it updates the record.  
   This transforms the deployment from a manual operational task to a fully idempotent code-driven process.4

### **4.3 The Playbook Implementation**

Below is a complete deploy\_komodo.yaml playbook that integrates these concepts. It assumes the use of Ansible Vault for securing sensitive API keys and passkeys.

YAML

\---  
\- name: Orchestrate Komodo Periphery Deployment  
  hosts: komodo\_nodes  
  become: true  
    
  vars\_files:  
    \- secrets/vault.yml  \# Contains encrypted API keys and Passkeys

  vars:  
    \# Target the v2 Dev branch logic  
    komodo\_version: "2-dev"  
      
    \# Configure Systemd Service  
    komodo\_user: "komodo"  
    komodo\_group: "docker"  
      
    \# Automation Configuration  
    enable\_server\_management: true  
    komodo\_core\_url: "https://komodo.example.com"  
      
    \# Vaulted Variables Mapping  
    komodo\_core\_api\_key: "{{ vault\_komodo\_api\_key }}"  
    komodo\_core\_api\_secret: "{{ vault\_komodo\_api\_secret }}"  
    komodo\_passkeys: \["{{ vault\_komodo\_shared\_passkey }}"\]

  roles:  
    \- role: bpbradley.komodo  
      vars:  
        komodo\_action: "install"  
        \# Security: Only allow connections from localhost (tunnel) or Core IP  
        komodo\_allowed\_ips:  
          \- "127.0.0.1"   
          \- "192.168.10.5" \# IP of the Core server

  tasks:  
    \- name: Verify Service Health  
      systemd:  
        name: komodo-periphery  
        state: started  
        enabled: yes

### **4.4 Automation Logic and Security Nuances**

The role's logic explicitly handles the v2 architecture's requirement for the komodo user.

* **Linger:** For user-scope systemd services, the role enables "linger" (loginctl enable-linger komodo). This ensures the Periphery process does not terminate when the SSH session creating the user closes. This is a subtle but common failure mode in manual systemd-user setups.4  
* **Binary Stripping:** The role supports cargo strip to reduce the binary size. The dev-90 release notes emphasize that binary sizes were reduced by \~5MB using this technique, improving deployment speed on low-bandwidth edge devices.3

---

## **5\. Integrating Pangolin: The Secure Ingress Layer**

While Komodo orchestrates the containers, **Pangolin** provides the secure pathway to access them. In a robust deployment, Pangolin sits in front of Komodo Core.

### **5.1 Deploying Pangolin via Docker Compose**

Pangolin itself is a containerized stack comprising the Dashboard, the Gerbil server, and Traefik.

YAML

services:  
  pangolin:  
    image: ghcr.io/fosrl/pangolin:latest  
    container\_name: pangolin  
    restart: unless-stopped  
    ports:  
      \- "80:80"  
      \- "443:443"  
      \# WireGuard ports for Tunnels (Gerbil)  
      \- "51820:51820/udp"  
    environment:  
      \- PANGOLIN\_DOMAIN=network.example.com  
      \- PANGOLIN\_EMAIL=admin@example.com  
    volumes:  
      \-./config:/data/config  
      \-./letsencrypt:/data/letsencrypt

### **5.2 The Tunneling Advantage (Gerbil & Newt)**

The synergy between Komodo and Pangolin is most evident in **Tunneled Ingress**.

* **Problem:** A Komodo Periphery agent is deployed on a server at a remote site behind a residential router (Dynamic IP, no port forwarding). Komodo Core cannot connect to it.  
* **Solution:**  
  1. Deploy the **Newt** client (Pangolin's tunneling agent) on the remote server alongside Komodo Periphery.  
  2. Newt establishes an outbound WireGuard connection to the Pangolin server (Gerbil).  
  3. Pangolin assigns a stable internal hostname (e.g., remote-node.pangolin.internal).  
  4. Komodo Core is configured to connect to http://remote-node.pangolin.internal:8120.  
  5. Traffic flows: Core \-\> Pangolin \-\> WireGuard Tunnel \-\> Newt \-\> Local Periphery.

This architecture enables Komodo to manage servers anywhere in the world without exposing any management ports to the public internet, significantly reducing the attack surface.5

### **5.3 Configuring Traefik for Komodo Core**

To expose the Komodo Core UI securely through Pangolin, one must utilize the dynamic\_config.yml feature of Pangolin's Traefik instance. This allows for hot-reloading of routing rules.  
**config/traefik/dynamic\_config.yml:**

YAML

http:  
  routers:  
    komodo-ui:  
      rule: "Host(\`komodo.network.example.com\`)"  
      service: komodo-service  
      tls:  
        certResolver: letsencrypt  
      middlewares:  
        \- "pangolin-auth" \# Enforce OIDC/Auth via Pangolin before reaching Komodo

  services:  
    komodo-service:  
      loadBalancer:  
        servers:  
          \- url: "http://core:9120"

By applying the pangolin-auth middleware, access to the Komodo control plane is gated by Pangolin's identity provider. This means an attacker cannot even see the Komodo login screen without first authenticating against the corporate/admin identity provider configured in Pangolin.6  
---

## **6\. Operational Workflows and Day-2 Operations**

Once deployed, the focus shifts to utilizing the platform for "GitOps" workflows and maintaining the stability of the dev release channel.

### **6.1 The GitOps Pipeline: From Code to Container**

Komodo v2 streamlines the deployment pipeline. The workflow automated by this architecture is as follows:

1. **Code Push:** Developer pushes code to a Git repository (GitHub/Gitea).  
2. **Webhook:** The Git provider sends a webhook payload to Komodo Core (via the Pangolin ingress).  
3. **Authentication:** Pangolin verifies the request (if configured) or passes it to Core which validates the webhook secret.  
4. **Procedure Execution:**  
   * Core identifies the Stack associated with the repo.  
   * Core instructs the target Periphery agent to git pull the changes.  
   * Core instructs the Periphery to docker compose build (if building from source) or docker compose pull (if using pre-built images).  
   * Core instructs Periphery to docker compose up \-d to roll the update.  
5. **Telemetry:** The Periphery streams the deployment logs back to the Core UI in real-time.

### **6.2 Managing Database Schema Migrations**

The dev-90 release notes explicitly warn about schema volatility.

* **The Check:** Before upgrading the Core container, administrators should back up the mongo\_data volume.  
* **The Fix:** If an update fails due to schema mismatch, the km utility is the recovery tool.  
  Bash  
  docker compose exec core km database v1-downgrade \-y

  This command, introduced in the v2 dev cycle, allows the database to be reverted to a state compatible with previous versions, preventing data loss during failed upgrades.3

### **6.3 Telemetry and Observability**

The komodo\_v2 Ansible role introduces variables for **OpenTelemetry (OTLP)**.

* komodo\_logging\_otlp\_endpoint: If set (e.g., to a Grafana Tempo or Jaeger endpoint), the Periphery binary will emit trace data.  
* **Insight:** This moves monitoring beyond simple "is it up?" checks. Administrators can visualize the latency of Docker commands, identifying if slow disk I/O on a specific node is causing deployment timeouts. This level of observability is characteristic of the mature "v2" architecture compared to the opaque operation of v1.4

## **7\. Conclusion**

The deployment of **Komodo v2 (dev-90)**, orchestrated by the **komodo\_v2 Ansible role** and secured by **Pangolin**, represents a sophisticated reference architecture for modern self-hosted infrastructure. This setup transcends simple container management, offering a unified platform that is:

1. **Scalable:** Through the Core/Periphery split and the use of efficient, systemd-managed binaries (via Ansible) rather than resource-heavy agent containers.  
2. **Automated:** By leveraging Ansible's enable\_server\_management to make infrastructure self-registering, eliminating manual inventory management in the UI.  
3. **Resilient:** Utilizing native MongoDB for complex state management and providing tooling (km) to handle the inevitable volatility of development release channels.  
4. **Secure:** Implementing a zero-trust ingress model via Pangolin's tunneling and identity-aware proxying, protecting the management plane from the open internet.

For the professional infrastructure engineer, this stack offers a compelling alternative to heavy Kubernetes deployments or fragmented scripts, providing a cohesive "glass cockpit" for the entire server fleet. The complexity of the initial setup—requiring careful database selection, Ansible configuration, and ingress planning—is the necessary investment for a platform that offers such granular control and broad automation capabilities.

#### **Works cited**

1. What is Komodo? | Komodo, accessed December 1, 2025, [https://komo.do/docs/intro](https://komo.do/docs/intro)  
2. moghtech/komodo: a tool to build and deploy software on ... \- GitHub, accessed December 1, 2025, [https://github.com/moghtech/komodo](https://github.com/moghtech/komodo)  
3. Releases · moghtech/komodo \- GitHub, accessed December 1, 2025, [https://github.com/moghtech/komodo/releases](https://github.com/moghtech/komodo/releases)  
4. Ansible role for simplified deployment of Komodo with systemd \- GitHub, accessed December 1, 2025, [https://github.com/bpbradley/ansible-role-komodo](https://github.com/bpbradley/ansible-role-komodo)  
5. Pangolin Docs: Introduction to Pangolin, accessed December 1, 2025, [https://docs.pangolin.net/](https://docs.pangolin.net/)  
6. fosrl/pangolin: Identity-Aware Tunneled Reverse Proxy Server with Dashboard UI \- GitHub, accessed December 1, 2025, [https://github.com/fosrl/pangolin](https://github.com/fosrl/pangolin)  
7. Komodo \- v1.19.1 \- Edit all .env and config files in UI : r/selfhosted \- Reddit, accessed December 1, 2025, [https://www.reddit.com/r/selfhosted/comments/1mz6sa4/komodo\_v1191\_edit\_all\_env\_and\_config\_files\_in\_ui/](https://www.reddit.com/r/selfhosted/comments/1mz6sa4/komodo_v1191_edit_all_env_and_config_files_in_ui/)  
8. Streamline Your Deployments : Komodo \+ GitHub Webhooks | by Rishav Kapil \- Medium, accessed December 1, 2025, [https://medium.com/@rishavkapil61/streamline-your-deployments-komodo-github-webhooks-51d4d3a04891](https://medium.com/@rishavkapil61/streamline-your-deployments-komodo-github-webhooks-51d4d3a04891)  
9. Advanced Configuration \- Komodo, accessed December 1, 2025, [https://komo.do/docs/setup/advanced](https://komo.do/docs/setup/advanced)  
10. accessed December 1, 2025, [https://raw.githubusercontent.com/moghtech/komodo/main/config/core.config.toml](https://raw.githubusercontent.com/moghtech/komodo/main/config/core.config.toml)  
11. Setup Komodo Core, accessed December 1, 2025, [https://komo.do/docs/setup](https://komo.do/docs/setup)  
12. MongoDB \- Komodo, accessed December 1, 2025, [https://komo.do/docs/setup/mongo](https://komo.do/docs/setup/mongo)  
13. Production Notes for Self-Managed Deployments \- Database Manual \- MongoDB Docs, accessed December 1, 2025, [https://www.mongodb.com/docs/manual/administration/production-notes/](https://www.mongodb.com/docs/manual/administration/production-notes/)  
14. MongoDB relies on AVX enabled CPU. If you can't run Mongo, use FerretDB instead. · Issue \#59 · moghtech/komodo \- GitHub, accessed December 1, 2025, [https://github.com/moghtech/komodo/issues/59](https://github.com/moghtech/komodo/issues/59)  
15. FerretDB v2.5.0 is available\!, accessed December 1, 2025, [https://blog.ferretdb.io/ferretdb-v250-is-available/](https://blog.ferretdb.io/ferretdb-v250-is-available/)  
16. Plain Authentication not enabled: Komodo stack fails after auto-updating to new ferretdb 2.0 version · Issue \#331 \- GitHub, accessed December 1, 2025, [https://github.com/moghtech/komodo/issues/331](https://github.com/moghtech/komodo/issues/331)  
17. FerretDB 2.0 GA: Open Source MongoDB alternative, ready for production, accessed December 1, 2025, [https://blog.ferretdb.io/ferretdb-v2-ga-open-source-mongodb-alternative-ready-for-production/](https://blog.ferretdb.io/ferretdb-v2-ga-open-source-mongodb-alternative-ready-for-production/)  
18. Komodo Migration Guide: SQLite / PostgreSQL → FerretDB v2 \#5689 \- GitHub, accessed December 1, 2025, [https://github.com/community-scripts/ProxmoxVE/discussions/5689](https://github.com/community-scripts/ProxmoxVE/discussions/5689)  
19. How to Install and Run Pangolin Locally on Your Server | daily.dev, accessed December 1, 2025, [https://app.daily.dev/posts/how-to-install-and-run-pangolin-locally-on-your-server-9eavjtnaj](https://app.daily.dev/posts/how-to-install-and-run-pangolin-locally-on-your-server-9eavjtnaj)  
20. Docker Compose \- Pangolin Docs, accessed December 1, 2025, [https://docs.pangolin.net/self-host/manual/docker-compose](https://docs.pangolin.net/self-host/manual/docker-compose)  
21. provision fedora coreos with ucore, komodo core, and komodo periphery as a systemd service \- GitHub Gist, accessed December 1, 2025, [https://gist.github.com/b-/f2c0f5269d6463793f07418e37467dae](https://gist.github.com/b-/f2c0f5269d6463793f07418e37467dae)  
22. komodo-periphery versions · moghtech \- GitHub, accessed December 1, 2025, [https://github.com/orgs/moghtech/packages/container/komodo-periphery/558666720?tag=2-dev](https://github.com/orgs/moghtech/packages/container/komodo-periphery/558666720?tag=2-dev)  
23. komodo-periphery versions · moghtech \- GitHub, accessed December 1, 2025, [https://github.com/orgs/moghtech/packages/container/komodo-periphery/558666720?tag=2.0.0-dev](https://github.com/orgs/moghtech/packages/container/komodo-periphery/558666720?tag=2.0.0-dev)  
24. Komodo \- Docker Container / Compose management \- v1.17 release : r/selfhosted, accessed December 1, 2025, [https://www.reddit.com/r/selfhosted/comments/1jilchk/komodo\_docker\_container\_compose\_management\_v117/](https://www.reddit.com/r/selfhosted/comments/1jilchk/komodo_docker_container_compose_management_v117/)  
25. Self-Host a Tunneled Reverse Proxy with Pangolin \- Pi My Life Up, accessed December 1, 2025, [https://pimylifeup.com/pangolin-linux/](https://pimylifeup.com/pangolin-linux/)