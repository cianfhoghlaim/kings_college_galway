

# **Architectural Blueprint for the Unified Deployment of Pigsty and Mathesar: A Simplified Komodo-Ansible Integration**

## **1\. Executive Summary**

In the contemporary landscape of data platform engineering, the convergence of robust, high-availability database infrastructure with intuitive, low-code user interfaces represents a critical architectural pattern. This report provides an exhaustive technical analysis and implementation blueprint for deploying **Pigsty**, a battery-included PostgreSQL distribution, alongside **Mathesar**, an open-source database interface, utilizing **Komodo** for stack orchestration and **Ansible** for declarative automation.  
The primary objective of this analysis is to distill a simplified yet production-grade technical outline that integrates these disparate components. We reference the automation patterns established by the bpbradley/ansible-role-komodo project but deliberately strip away abstraction layers—such as complex vaulting mechanisms and multi-architecture dynamic detection—to expose the core deployment logic. The focus is specifically on the integration vector: bridging Pigsty’s declarative infrastructure (Ansible-managed PostgreSQL) with Komodo’s containerized stack management (Docker-managed Mathesar).  
This architecture effectively decouples the stateful data layer from the stateless application layer while ensuring tight operational integration through precise networking and security configurations. By leveraging Pigsty as the "Data Operating System" and Komodo as the "Stack Orchestrator," we eliminate the need for redundant database containers, utilizing the host-based PostgreSQL cluster for both the application’s internal state and the business data it visualizes.  
The following report breaks down the deployment into critical architectural layers: the Data Layer (Pigsty), the Orchestration Layer (Komodo), and the Application Layer (Mathesar), culminating in a comprehensive guide on integrating the network and security models to facilitate seamless communication between the Dockerized application and the host-based database infrastructure.  
---

## **2\. Architectural Philosophy and Component Analysis**

To understand the integration strategy, one must first analyze the design philosophy of the constituent components. The friction in deploying Mathesar on Pigsty often arises from the clash between "Container-Native" workflows (where the database is just another container) and "Bare-Metal" workflows (where the database is a privileged host service). This architecture bridges that gap.

### **2.1 The Data Layer: Pigsty as a Data Operating System**

Pigsty is not merely a collection of Ansible playbooks for installing PostgreSQL; it is a comprehensive "Data Operating System" that transforms a Linux node into a production-grade database cluster.1 Unlike a standard package installation (e.g., apt install postgresql), Pigsty introduces a sophisticated stack of intermediary services designed to ensure High Availability (HA), connection pooling, and deep observability.

#### **2.1.1 The "Battery-Included" Architecture**

The core value proposition of Pigsty lies in its pre-configured ecosystem. When a node is provisioned via Pigsty, it automatically includes:

* **Patroni:** For high availability and cluster state management, using a distributed consensus store (ETCD) to manage leader election and failover.2  
* **HAProxy:** For traffic routing. Pigsty exposes services, not servers. Applications connect to HAProxy ports (e.g., 5433), which route traffic to the current primary or replica node based on Patroni’s health checks.3  
* **Pgbouncer:** For connection pooling. This sits between HAProxy and PostgreSQL, reducing the overhead of establishing new connections—a critical feature for web applications like Mathesar that may generate bursty traffic.4  
* **Observability Stack:** Prometheus, Grafana, and Loki are pre-wired to scrape metrics from the database, the operating system, and the connection pooler.2

#### **2.1.2 Declarative Configuration Model**

Pigsty utilizes a strictly declarative configuration model rooted in a single inventory file, typically pigsty.yml. This file defines the desired state of the entire infrastructure: which users should exist, which databases should be created, and which extensions should be installed.5 This IaC (Infrastructure as Code) approach allows us to define the Mathesar dependency requirements (its internal database and service user) *before* the application stack is ever launched, ensuring a deterministic deployment.

### **2.2 The Orchestration Layer: Komodo and the bpbradley Pattern**

Komodo serves as the lightweight orchestration engine for the stateless components of the stack (i.e., the Mathesar Docker container). It offers a simplified alternative to heavy orchestrators like Kubernetes or Portainer, focusing on "Stacks" (Docker Compose definitions) managed via a clean UI and an agent-based architecture.6

#### **2.2.1 The bpbradley Automation Pattern**

The bpbradley/ansible-role-komodo is a recognized community standard for deploying the Komodo "Periphery" (agent) and "Core" (server). It is a robust, highly abstract Ansible role that handles:

* **Systemd Management:** Running Komodo as a service, either in system scope or user scope.6  
* **Vault Integration:** Using Ansible Vault to encrypt and inject passkeys.6  
* **Architecture Detection:** Dynamically determining whether to download ARM64 or AMD64 binaries.7

#### **2.2.2 The "Stripped" Complexity Approach**

For this specific implementation, strict adherence to the full bpbradley role introduces unnecessary overhead. Our goal is a "Simplified Technical Outline." Therefore, we extract the *essence* of the pattern—the Systemd unit file structure and the user permission model—while discarding the complexity of dynamic architecture detection and vault management. We opt for a linear, "flat" Ansible playbook that is easier to audit and modify for this specific integration. This reduction reduces the "Cognitive Load" of the deployment, making the mechanism transparent: we are simply placing a binary, configuring a TOML file, and starting a systemd service.

### **2.3 The Application Layer: Mathesar**

Mathesar acts as a collaborative interface for PostgreSQL databases. Unlike tools that ingest data into their own silo, Mathesar works directly on the Postgres schema. This makes the integrity of the connection to Pigsty paramount.

#### **2.3.1 Dual-Database Requirement**

Mathesar has a dual relationship with the database layer:

1. **Internal State (mathesar\_django):** Mathesar is a Django application. It requires a PostgreSQL database to store its own users, permissions, and interface metadata.8  
2. **Business Data:** Mathesar connects to existing Postgres databases to allow users to view and edit data.9

In a Docker-native setup, one might spin up a separate Postgres container for the internal state. However, in our architecture, we leverage Pigsty to host *both* the internal state and the business data. This unifies backup strategies (via Pigsty’s pgBackRest) and monitoring (via Pigsty’s Grafana), significantly reducing operational complexity.  
---

## **3\. Deep Dive: The Network Integration Challenge**

The central technical challenge in this deployment is the network boundary between the Docker container (managed by Komodo) and the Host services (managed by Pigsty). Understanding this is prerequisite to configuring the firewall rules.

### **3.1 The Docker Bridge Isolation**

By default, Docker containers run in a bridge network (e.g., 172.17.0.0/16). They are isolated from the host’s loopback interface (127.0.0.1).

* **The Problem:** If the Mathesar configuration sets POSTGRES\_HOST=localhost, the container attempts to connect to *itself*. Since Postgres is not running inside the Mathesar container, the connection fails.10  
* **The Naive Solution (Host Networking):** One could run the container with \--network host. This shares the host's network namespace, allowing localhost to work. However, this is architecturally discouraged as it creates port conflicts (Mathesar wants port 8000; what if another service wants port 8000?) and reduces security isolation.11

### **3.2 The Host Gateway Solution (host.docker.internal)**

The robust solution, supported by modern Docker versions on Linux, is to use the host-gateway mapping. This feature allows us to map a special DNS name, host.docker.internal, to the IP address of the bridge gateway (the host's interface on the Docker network).12  
Mechanism:  
In the Docker Compose definition managed by Komodo, we add:

YAML

extra\_hosts:  
  \- "host.docker.internal:host-gateway"

When Mathesar resolves host.docker.internal, it receives the IP address 172.17.0.1 (typically). It then sends packets to this IP on port 5433 (Pigsty's HAProxy).

### **3.3 The Firewall Barrier (pg\_hba.conf)**

Successfully routing the packet to the host is only half the battle. The PostgreSQL server (and the HAProxy in front of it) must *accept* the connection.

* **Pigsty Default:** Pigsty is secure by default. Its pg\_hba.conf is configured to allow connections from known intranet subnets, but it may not explicitly whitelist the dynamic Docker bridge subnet.13  
* **The Authentication Failure:** If the HBA rules are not updated, the connection from 172.17.0.1 will be rejected with FATAL: no pg\_hba.conf entry for host "172.17.0.1".

**Architectural Decision:** We must declaratively add the Docker subnet to Pigsty’s configuration in pigsty.yml to allow these connections. This is a critical integration point where the Data Layer config must be aware of the Orchestration Layer's networking model.  
---

## **4\. Implementation Blueprint: The Data Layer (Pigsty)**

This section details the specific configurations required in Pigsty to prepare the ground for Mathesar. We utilize Pigsty’s Ansible playbooks to enforce this state.

### **4.1 Declarative User and Database Definition**

We must define the Mathesar artifacts in the pigsty.yml inventory. This replaces manual SQL execution.

#### **4.1.1 The Mathesar Service User**

Mathesar requires a database user with permissions to create schemas (for its internal operation) and read/write data.  
**Configuration Snippet (pigsty.yml):**

YAML

pg\_users:  
  \- name: dbuser\_mathesar  
    password: "Production\_Strong\_Password\_\!23"  
    pgbouncer: true  
    roles: \[dbrole\_readwrite\]  
    comment: "Service user for Mathesar Docker Stack"

* **pgbouncer: true**: This is crucial. Pigsty maintains a userlist.txt for Pgbouncer authentication. If this flag is omitted, the user is created in Postgres but *not* in Pgbouncer. Since Mathesar should connect via the Pgbouncer port (5433) for performance, omitting this flag would cause "Authentication failed" errors at the proxy layer.14  
* **roles: \[dbrole\_readwrite\]**: Pigsty’s default role system. This grants the user standard CRUD privileges. Mathesar, being a schema management tool, might eventually require dbrole\_admin on specific databases, but readwrite is the secure starting point.14

#### **4.1.2 The Internal Database**

Mathesar needs a distinct database for its Django ORM.  
**Configuration Snippet (pigsty.yml):**

YAML

pg\_databases:  
  \- name: mathesar\_django  
    owner: dbuser\_mathesar  
    extensions:  
      \- { name: citext, schema: public } \# Mathesar often benefits from citext  
    comment: "Mathesar Internal Metadata Store"

By defining the owner as dbuser\_mathesar, we ensure the service user has full DDL (Data Definition Language) rights on this specific database, allowing Mathesar to run its initial migrations successfully.5

### **4.2 Security Configuration (pg\_hba.conf)**

To solve the network challenge described in Section 3.3, we append a rule to pg\_hba\_rules.  
**Configuration Snippet (pigsty.yml):**

YAML

pg\_hba\_rules:  
  \#... existing rules...  
  \- { type: host, db: all, user: all, addr: 172.16.0.0/12, method: md5, comment: "Allow Docker Containers (Bridge Network)" }

**Analysis:** We use 172.16.0.0/12 to cover the entire private range often used by Docker (172.16.x.x through 172.31.x.x). This is a broad permission. In a highly sensitive environment, one would restrict this to the specific subnet (e.g., 172.17.0.0/16), but Docker subnets can change on daemon restart if conflicts arise. The md5 method (which supports SCRAM-SHA-256 in modern Postgres) ensures that even with network access, a valid password is required.13

### **4.3 Applying the Configuration**

Once pigsty.yml is updated, the state is enforced via Pigsty’s playbooks.

1. **./pgsql.yml**: Runs the full provisioning. It creates users, databases, and updates the HBA rules. It also reloads the Postgres service to apply the HBA changes without downtime.1

---

## **5\. Implementation Blueprint: The Orchestration Layer (Komodo via Ansible)**

With the database ready, we move to deploying the Orchestrator. Here we implement the "Stripped Complexity" pattern derived from bpbradley.

### **5.1 Simplifying the bpbradley Pattern**

The original role splits tasks across multiple files and uses advanced Jinja2 templating for cross-platform support. Our simplified outline consolidates this into a single linear playbook designed for x86\_64 (standard server) architectures, removing the ansible-vault dependency in favor of standard variable injection (which can still be encrypted at the file level if needed).  
**Key Simplification Decisions:**

1. **Single Scope:** bpbradley supports both User and System systemd scopes. We mandate **System scope** for the server deployment to ensure boot persistence and log centralization, simplifying the unit file logic.6  
2. **Direct Binary Fetch:** Instead of querying GitHub APIs to find the latest release dynamically, we define a version variable and construct the URL directly. This is more deterministic and less prone to API rate limits during deployment.  
3. **Hardcoded Paths:** We standardize on /usr/local/bin for the binary and /etc/komodo for config, removing the logic that allows these to be completely arbitrary. This creates a "Known Good State" for documentation.

### **5.2 The Simplified Ansible Playbook**

The following playbook deploys the Komodo Periphery (Agent). It assumes the Komodo Core (Server) is already running elsewhere, or it can be adapted to deploy Core by changing the binary name.  
**Playbook: komodo\_deploy.yml**

YAML

\---  
\- name: Deploy Komodo Periphery (Simplified)  
  hosts: pigsty\_nodes  
  become: true  
  vars:  
    komodo\_version: "v1.16.11"  
    komodo\_user: "komodo"  
    \# In production, use ansible-vault for this variable  
    komodo\_passkey: "your\_secure\_passkey\_matching\_core"   
    komodo\_core\_url: "http://komodo-core.example.com:9120"  
      
  tasks:  
    \# 1\. User Management (Stripped from bpbradley's user.yml)  
    \- name: Ensure Komodo service user exists  
      ansible.builtin.user:  
        name: "{{ komodo\_user }}"  
        shell: /usr/sbin/nologin  
        system: true  
        groups: docker  
        append: true  
      \# Insight: The 'groups: docker' directive is the critical link.   
      \# Without this, the Komodo agent cannot spawn the Mathesar containers.

    \# 2\. Directory Structure  
    \- name: Create configuration directory  
      ansible.builtin.file:  
        path: /etc/komodo  
        state: directory  
        owner: "{{ komodo\_user }}"  
        mode: '0755'

    \# 3\. Binary Installation (Simplified from bpbradley's install.yml)  
    \- name: Download Komodo Periphery binary  
      ansible.builtin.get\_url:  
        url: "https://github.com/mbecker/komodo/releases/download/{{ komodo\_version }}/komodo-periphery\_linux\_amd64"  
        dest: /usr/local/bin/komodo-periphery  
        mode: '0755'  
        owner: "{{ komodo\_user }}"

    \# 4\. Configuration (Direct TOML generation)  
    \- name: Generate Komodo Configuration  
      ansible.builtin.copy:  
        dest: /etc/komodo/config.toml  
        owner: "{{ komodo\_user }}"  
        content: |  
          \[server\]  
          address \= "0.0.0.0"  
          port \= 9120  
            
          \[system\]  
          passkey \= "{{ komodo\_passkey }}"  
            
          \# Optional: Automatic registration with Core (if supported by version)  
          \# \[client\]  
          \# url \= "{{ komodo\_core\_url }}"

    \# 5\. Systemd Unit (Simplified from bpbradley's service.yml)  
    \- name: Create Systemd Unit  
      ansible.builtin.copy:  
        dest: /etc/systemd/system/komodo-periphery.service  
        content: |  
          \[Unit\]  
          Description=Komodo Periphery Agent  
          After=network.target docker.service  
          Requires=docker.service

           
          Type=simple  
          User={{ komodo\_user }}  
          Group=docker  
          ExecStart=/usr/local/bin/komodo-periphery \--config /etc/komodo/config.toml  
          \# Restart logic ensures resilience  
          Restart=always  
          RestartSec=5  
          \# Security hardening (optional but recommended)  
          NoNewPrivileges=true

          \[Install\]  
          WantedBy=multi-user.target

    \# 6\. Service Activation  
    \- name: Start and Enable Komodo  
      ansible.builtin.systemd:  
        name: komodo-periphery  
        state: started  
        enabled: true  
        daemon\_reload: true

### **5.3 Execution and Verification**

Running this playbook establishes the Orchestration Layer.

* **Verification:** systemctl status komodo-periphery should show "Active (running)".  
* **Log Check:** journalctl \-u komodo-periphery will show the agent initializing and listening on port 9120\.

---

## **6\. Implementation Blueprint: The Application Layer (Mathesar)**

With the Data Layer ready (Pigsty) and the Orchestration Layer active (Komodo), the final step is defining the Mathesar Stack within Komodo. This is where the integration comes to life.

### **6.1 The Stack Definition (Docker Compose)**

In the Komodo UI, we create a new stack for the node. This definition integrates the networking workaround (extra\_hosts) and the database credentials.  
**Table 1: Mathesar Environment Configuration Mapping**

| Environment Variable | Value | Explanation |
| :---- | :---- | :---- |
| POSTGRES\_HOST | host.docker.internal | Routes traffic to the host gateway IP (172.17.0.1). |
| POSTGRES\_PORT | 5433 | Connects to Pigsty's **Pgbouncer** port for connection pooling.4 |
| POSTGRES\_DB | mathesar\_django | The internal state database created in Section 4.1. |
| POSTGRES\_USER | dbuser\_mathesar | The service user created in Section 4.1. |
| POSTGRES\_PASSWORD | *(Secure Password)* | Must match the pigsty.yml definition. |
| SECRET\_KEY | *(Random 50-char string)* | Required for Django cryptographic signing.15 |
| DOMAIN\_NAME | http://\<HOST\_IP\>:8000 | Mathesar's trusted origin setting.8 |

**The Komodo Stack YAML:**

YAML

version: '3.8'

services:  
  mathesar:  
    image: mathesar/mathesar-prod:0.1.7  
    container\_name: mathesar  
    restart: always  
    ports:  
      \- "8000:8000"  
      
    \# THE CRITICAL INTEGRATION POINT  
    extra\_hosts:  
      \- "host.docker.internal:host-gateway"  
      
    environment:  
      \- POSTGRES\_HOST=host.docker.internal  
      \- POSTGRES\_PORT=5433  
      \- POSTGRES\_DB=mathesar\_django  
      \- POSTGRES\_USER=dbuser\_mathesar  
      \- POSTGRES\_PASSWORD=Production\_Strong\_Password\_\!23  
      \- SECRET\_KEY=k3920-239d-239d-239d-239d239d239d  
      \- DOMAIN\_NAME=http://10.10.10.10:8000  
      \- DEBUG=0  
      
    volumes:  
      \# Persist user-uploaded media files  
      \- mathesar\_media:/data/media

volumes:  
  mathesar\_media:

### **6.2 Transaction Mode Nuance (Pgbouncer vs. Direct)**

A subtle but critical architectural detail involves Pgbouncer's "Transaction Pooling" mode, which Pigsty uses by default on port 5433\.

* **The Conflict:** Transaction pooling does not support certain PostgreSQL features like Prepared Statements, LISTEN/NOTIFY, or session-level advisory locks in the way some ORMs expect.4  
* **The Mitigation:** If Mathesar logs errors related to "prepared statement 'S\_1' does not exist," the architecture allows an immediate fallback. Change POSTGRES\_PORT in the Komodo stack from 5433 to 5436\.  
* **Port 5436:** This is Pigsty’s "Primary Direct" service. It routes through HAProxy (for HA) but bypasses Pgbouncer, connecting directly to the Postgres backend process.3 This sacrifices pooling efficiency for full feature compatibility.

---

## **7\. Operational Lifecycle and "Day 2" Considerations**

The deployment is only the beginning. This architecture provides distinct advantages for long-term operations.

### **7.1 Unified Backups (pgBackRest)**

Because Mathesar’s state resides in the mathesar\_django database managed by Pigsty, it is automatically included in the cluster’s backup policy.

* **Mechanism:** Pigsty configures pgbackrest to run scheduled full, differential, and incremental backups to a local repo or S3.2  
* **Benefit:** There is no need to configure a separate "docker exec pg\_dump" cron job for the Mathesar container. The data is protected by the same enterprise-grade policy as the core business data.

### **7.2 Observability and Metrics**

Pigsty’s Prometheus stack automatically scrapes metrics from the database layer.

* **Pgbouncer Metrics:** By connecting Mathesar to port 5433, we can visualize its connection usage, wait times, and query throughput on Pigsty’s "Pgbouncer Instance" dashboard.4  
* **Postgres Metrics:** The resource consumption of the mathesar\_django database (CPU, IOPS, deadlocks) is visible on the "PGSQL Database" dashboard.  
* **Integration Insight:** This provides immediate visibility into the "Cost" of the Mathesar application on the underlying infrastructure, allowing DBAs to tune resource limits if Mathesar queries become aggressive.

### **7.3 Updates and Patching**

The decoupled nature of the Data and Application layers simplifies updates:

* **Mathesar Update:** Change the image tag in the Komodo Stack (e.g., 0.1.7 \-\> 0.1.8) and click "Deploy." Komodo pulls the new image and restarts the container. The database schema migrations run automatically on startup.  
* **PostgreSQL Update:** Pigsty allows for minor version upgrades via yum update (handled by Ansible). Major version upgrades can be handled via Pigsty's side-by-side migration playbooks.1

---

## **8\. Conclusion**

The integration of Pigsty and Mathesar via Komodo and Ansible represents a powerful pattern for modern data infrastructure. By stripping the complexity from the bpbradley automation role, we achieve a lightweight, understandable, and reproducible deployment mechanism for the orchestration agent. By leveraging Pigsty’s declarative power, we solve the complex problems of database user management, high availability, and observability without writing custom scripts.  
The success of this architecture hinges on the precise configuration of the network boundary: explicitly allowing Docker bridge traffic in pg\_hba.conf and correctly mapping host.docker.internal in the Komodo stack definition. This creates a "Glass Box" environment where data persistence is robust (Host/Pigsty) and application delivery is agile (Docker/Komodo), satisfying the requirements of both stability and modern application lifecycle management.

#### **Works cited**

1. Playbooks | PIGSTY, accessed December 1, 2025, [https://pigsty.io/docs/setup/playbook/](https://pigsty.io/docs/setup/playbook/)  
2. Architecture \- Pigsty Docs, accessed December 1, 2025, [https://doc.pgsty.com/pgsql/arch/](https://doc.pgsty.com/pgsql/arch/)  
3. Service \- Pigsty Docs, accessed December 1, 2025, [https://doc.pgsty.com/pgsql/service/](https://doc.pgsty.com/pgsql/service/)  
4. Architecture | PIGSTY, accessed December 1, 2025, [https://pigsty.io/docs/pgsql/arch/](https://pigsty.io/docs/pgsql/arch/)  
5. Databases | PIGSTY, accessed December 1, 2025, [https://pigsty.io/docs/pgsql/db/](https://pigsty.io/docs/pgsql/db/)  
6. Ansible role for simplified deployment of Komodo with systemd \- GitHub, accessed December 1, 2025, [https://github.com/bpbradley/ansible-role-komodo](https://github.com/bpbradley/ansible-role-komodo)  
7. bpbradley/ansible-role-komodo · GitHub \- Workflow runs, accessed December 1, 2025, [https://github.com/bpbradley/ansible-role-komodo/actions](https://github.com/bpbradley/ansible-role-komodo/actions)  
8. Using Docker Compose \- Mathesar Documentation, accessed December 1, 2025, [https://docs.mathesar.org/0.5.0/administration/install-via-docker-compose/](https://docs.mathesar.org/0.5.0/administration/install-via-docker-compose/)  
9. Databases \- Mathesar Documentation, accessed December 1, 2025, [https://docs.mathesar.org/0.2.1/user-guide/databases/](https://docs.mathesar.org/0.2.1/user-guide/databases/)  
10. Allow docker container to connect to a local/host postgres database \- Codemia, accessed December 1, 2025, [https://codemia.io/knowledge-hub/path/allow\_docker\_container\_to\_connect\_to\_a\_localhost\_postgres\_database\_closed](https://codemia.io/knowledge-hub/path/allow_docker_container_to_connect_to_a_localhost_postgres_database_closed)  
11. Accessing Host Services from Docker Containers \- DEV Community, accessed December 1, 2025, [https://dev.to/mjnaderi/accessing-host-services-from-docker-containers-1a97](https://dev.to/mjnaderi/accessing-host-services-from-docker-containers-1a97)  
12. The Equivalent of –add-host=host.docker.internal:host-gateway in Docker Compose | Baeldung on Ops, accessed December 1, 2025, [https://www.baeldung.com/ops/docker-compose-add-host](https://www.baeldung.com/ops/docker-compose-add-host)  
13. Allow Docker Container Access to Host's Postgres Database on linux (ubuntu) \- GitHub Gist, accessed December 1, 2025, [https://gist.github.com/MauricioMoraes/87d76577babd4e084cba70f63c04b07d](https://gist.github.com/MauricioMoraes/87d76577babd4e084cba70f63c04b07d)  
14. Users | PIGSTY, accessed December 1, 2025, [https://pigsty.io/docs/pgsql/user/](https://pigsty.io/docs/pgsql/user/)  
15. Install using Docker Compose \- Mathesar Documentation, accessed December 1, 2025, [https://docs.mathesar.org/0.1.7/installation/docker-compose/](https://docs.mathesar.org/0.1.7/installation/docker-compose/)