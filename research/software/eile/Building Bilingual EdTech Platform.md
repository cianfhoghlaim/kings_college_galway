# **Architectural Blueprint for a Bilingual EdTech Platform: Leveraging Edge Computing and WebAssembly for the Irish Leaving Certificate**

## **1\. Executive Summary and Architectural Thesis**

The modernization of the Irish Leaving Certificate, specifically with the introduction of Computer Science (LCCS) as an examinable subject and the continued evolution of the Project Maths syllabus, necessitates a radical rethinking of educational infrastructure. Traditional Learning Management Systems (LMS) are static repositories of information—digital filing cabinets that serve PDFs and quizzes. They fail to provide the *experiential* learning required for computational thinking and mathematical exploration. The objective of this research report is to define the architectural specifications for a high-performance, interactive, and bilingual (English-Irish) educational platform that transcends these limitations.  
This report proposes a distinct departure from traditional server-centric architectures—such as the Firecracker-based model employed by iximiuz Labs 1—in favor of an Edge-native and Browser-native approach. By utilizing **Marimo notebooks** with WebAssembly (Wasm) export, **Cloudflare Workers**, **Durable Objects**, and **TanStack Start**, alongside a self-hosted **Coder** instance, we can create a platform that offers instant-start coding environments, reactive mathematical visualizations, and seamless bilingual toggling.  
The reference architecture provided by iximiuz Labs demonstrates a robust, bare-metal approach to serving Docker and Kubernetes training environments.1 It relies on a "Foreman" for orchestration, a custom "Bender" daemon for VM provisioning, and a "Conductor" for stream management.1 While effective for heavy infrastructure training, this model is architecturally overweight for the specific requirements of the Irish secondary school syllabus. Our analysis suggests that by shifting the computational load for the majority of the syllabus (Mathematics and introductory Python) to the client’s browser via WebAssembly, and reserving heavy server-side resources (via Coder) only for advanced systems programming tasks, we can achieve a solution that is orders of magnitude more cost-effective and scalable.  
This document serves as a comprehensive technical design specification, detailing how each component of the proposed stack replaces and improves upon the reference architecture's counterparts, specifically tailored to the bilingual and pedagogical needs of Irish education.

## ---

**2\. Contextual Analysis: The Pedagogical & Technical Landscape**

To architect a solution that fits the user's need, we must first deeply understand the "customer"—the syllabus itself—and the unique linguistic constraints of the Irish educational system.

### **2.1 The Leaving Certificate Computer Science (LCCS) Specification**

The LCCS specification is a rigorous introduction to the field, divided into three core strands which dictate the technical requirements of our platform.

* **Strand 1: Practices and Principles:** This covers the design process and computational thinking. It requires tools for flow-charting, pseudocode, and iterative design.  
* **Strand 2: Core Concepts:** This involves abstraction, algorithms, computer systems, and data. Students must engage with sorting algorithms, binary logic, and data structures.  
* **Strand 3: Computer Science in Practice:** This is the applied component, involving the creation of software artifacts. The "Applied Learning Task" (ALT) is a significant coursework element where students might build web applications, embedded systems projects, or data analytics reports.

Architectural Implication:  
The reference platform 1 focuses heavily on Docker and Kubernetes—tools for infrastructure operations. While LCCS touches on "computers and the internet," the primary focus is on application logic, coding (Python/JavaScript), and data analytics. The iximiuz architecture 1 is optimized for "Systems Ops" (booting Linux kernels, configuring networks). Simulating a full Linux kernel via Firecracker for every student merely to run a simple Python sorting algorithm is an inefficient use of resources. WebAssembly (via Marimo) is the superior fit for Strands 1 and 2, offering instant startup and zero server cost. However, Strand 3 (Web Development) requires a true server environment to expose ports and run databases, necessitating the integration of Coder.

### **2.2 The Mathematics Syllabus (Project Maths)**

The Irish Maths syllabus, known as Project Maths, emphasizes "understanding over rote learning." It moves away from abstract manipulation of formulas toward understanding concepts through application and visualization.

* **Calculus:** Understanding rates of change, derivatives, and integration through visual slopes.  
* **Statistics:** Normal distributions, correlation coefficients, and regression lines.  
* **Complex Numbers:** Visualizing the Argand diagram and rotations.

The Marimo Advantage:  
Marimo notebooks are "reactive." In a standard Jupyter notebook (often used in education), if a student defines x \= 5, runs a cell to calculate y \= x^2, and then goes back to change x \= 10 without re-running the second cell, the state becomes inconsistent ($y$ remains 25). This "out-of-order execution" is a major pedagogical stumbling block. Marimo enforces a dataflow graph: change x, and y updates instantly. This reactivity is pedagogically superior for teaching mathematical relationships, acting as a live "mathematical playground" akin to the Docker playgrounds in the reference material 1 but optimized for logic and mathematics.

### **2.3 The Bilingual Requirement (An Ghaeilge)**

The user query emphasizes "bilingual Irish-English education." This is a profound architectural constraint, not merely a UI preference. Ireland has a network of *Gaelcholáistí* (Irish-medium secondary schools) where all subjects are taught in Irish. A major pain point in this sector is the "translation lag"—resources are often available in English years before they are translated.  
A digital platform must treat Irish (Gaeilge) as a first-class citizen in the data schema and user interface. This requires an architecture capable of instantaneous, context-aware state switching between languages without losing the user's progress or context. The "static" nature of the iximiuz frontend (Vue.js served by Node) 1 must be evolved into a dynamic, edge-routed system (TanStack Start) that can inject localized terminology into live coding environments on the fly.

## ---

**3\. High-Level Architecture: The Edge-Native Shift**

This section contrasts the reference architecture with the proposed solution, establishing the rationale for the selected technologies.

### **3.1 Critique of the Reference Model (iximiuz Labs)**

The reference document 1 describes a robust, centralized architecture designed for heavy infrastructure training.

* **Foreman:** A monolithic Node.js application handling authentication, API, and orchestration logic.1  
* **Workers:** A fleet of bare-metal servers (Hetzner) running Firecracker microVMs.1  
* **Bender:** A custom, privileged Go daemon responsible for creating rootfs, networking bridges, and launching VMs.1  
* **Conductor:** A daemon managing WebSocket streams for terminal sessions and task updates.1  
* **Examiner:** A gRPC-based service for checking student solutions inside the VM.1

While impressive, this model presents significant friction for a high-school syllabus:

* **Cost & Waste:** It requires persistent bare-metal servers. If a student is merely reading text or running a simple calculation, the server resources are underutilized.  
* **Complexity:** Managing custom networking bridges, iptables, and TAP interfaces 1 incurs high operational overhead.  
* **Latency:** Terminal inputs must travel to the data center (Germany/Finland) and back. For a student in rural Kerry or Donegal, this latency degrades the interactive experience.

### **3.2 The Proposed Edge-Native Model**

We propose inverting this model. Instead of bringing the user to the server (Firecracker), we bring the compute to the user (WebAssembly) or the network edge (Cloudflare).

| Architectural Layer | iximiuz Reference Implementation | Proposed Architecture (Leaving Cert Platform) | Primary Advantage |
| :---- | :---- | :---- | :---- |
| **Frontend Framework** | Vue.js / Nuxt (Node.js) | **TanStack Start** (Edge-rendered) | Unified Type Safety & Edge Routing |
| **Compute Engine** | Bare Metal / Node.js Monolith | **Cloudflare Workers** | Distributed, Serverless, Low Latency |
| **State Management** | Redis / MongoDB | **Durable Objects** | Strong Consistency, No Ops, Real-time |
| **Lab Runtime (Light)** | Firecracker MicroVMs | **Marimo (WebAssembly)** | Zero Cost, Instant Load, Offline Capable |
| **Lab Runtime (Heavy)** | Firecracker MicroVMs | **Self-Hosted Coder** | Standardized Environments (Terraform) |
| **Orchestrator** | "Bender" (Custom Go Daemon) | **Coder Control Plane** | Proven Stability, Less Custom Code |
| **Transport Layer** | "Conductor" (WebSocket/SSH) | **Durable Objects (WebSockets)** | Programmable Edge State |

This architecture decouples the "light" computational tasks (maths visualization, basic Python) from the "heavy" tasks (web server hosting, database design), handling the former in the browser and the latter via Coder.

## ---

**4\. The Frontend Layer: TanStack Start & Cloudflare**

The backbone of the system replaces the "Foreman" component described in the iximiuz research.1 Instead of a monolithic Node.js application managing the fleet, we utilize Cloudflare Workers hosting a TanStack Start application.

### **4.1 TanStack Start: The Meta-Framework**

TanStack Start is the ideal choice for this platform because of its full-stack type safety and server-side rendering (SSR) capabilities, which are crucial for the bilingual requirement (SEO and initial load performance) and for maintaining a robust codebase.  
Replacing the Node.js Foreman:  
In the iximiuz model, the Foreman handled SSR and API requests.1 TanStack Start, deployed on Cloudflare Workers, creates a unified codebase where the boundary between frontend and backend is fluid.

* **Server Functions:** API endpoints for student progress tracking are defined as server functions within the application code, executed at the Edge. This eliminates the context switch between writing UI code and API code.  
* **Hydration:** The initial HTML is generated at the nearest Cloudflare PoP (Point of Presence) to the student. For a student in Dublin, the HTML is generated in Dublin, ensuring sub-50ms Time to First Byte (TTFB).

### **4.2 Bilingual Routing and Localization Strategy**

A critical requirement is the Irish-English toggle. TanStack Start's routing system allows us to implement internationalization (i18n) at the URL level (e.g., /en/calculus/derivatives vs. /ga/calcalas/díorthaigh).  
**Implementation Strategy:**

1. **Middleware detection:** A Cloudflare Worker middleware inspects the Accept-Language header to redirect new users to their preferred language path.  
2. **Streaming Resources:** Unlike standard i18n libraries that load huge JSON blobs for the entire site, we use the Edge to stream only the required language segments for the current lesson.  
3. **Terminology Mapping (The Glossary Service):** A key-value store (Cloudflare KV) holds the glossary (e.g., "Integer" \-\> "Slánuimhir"). This allows distinct pedagogical terms to be injected dynamically into the lesson content. When a student hovers over a term in the lesson text, a tooltip fetches the definition from KV.

### **4.3 Edge-Native Authentication**

The iximiuz platform utilized GitHub OAuth and stored user profiles in a database.1 In our Edge architecture, we utilize **Cloudflare Access** combined with a custom JWT implementation within the Worker. This eliminates the need for a central authentication server, reducing the attack surface—a concept highlighted in the reference document as a security concern for the Foreman component.1 By validating tokens at the Edge, we prevent unauthenticated requests from ever touching the backend Coder infrastructure or Durable Objects.

## ---

**5\. The Computational Core: Marimo & WebAssembly**

The most significant divergence from the iximiuz architecture is the use of Marimo with WebAssembly export for the bulk of the curriculum. This effectively replaces the "Worker Servers" and "Firecracker" components 1 for 80% of the platform's utility.

### **5.1 Why Marimo? The Reactive Paradigm**

Marimo is a next-generation Python notebook that is fundamentally reactive. It addresses the "hidden state" problem of Jupyter.  
Pedagogical Relevance to Project Maths:  
Consider a lesson on Complex Numbers ($z \= x \+ iy$).

* **Traditional:** A static graph of the Argand Diagram.  
* **Marimo:** A slider controls the value of $\\theta$ (the argument) and $r$ (the modulus). As the student moves the slider, the vector rotates and extends on the Argand Diagram in real-time.  
* **Mechanism:** The Python code calculating the rotation runs in the browser via Pyodide. The plotting library (e.g., Altair or Matplotlib) renders the new graph instantly. This creates the "Microworld" learning environment where students explore properties by manipulating variables.

### **5.2 WebAssembly Export (The "Client-Side MicroVM")**

The user query specifies "Marimo notebooks and its webassembly export." This feature allows us to package a full Python environment, including scientific libraries like pandas, numpy, and scipy, into a static HTML/Wasm bundle.  
**Architectural Impact & Comparison:**

* **Cost Reduction:** In the iximiuz model, every active lesson required a running Firecracker microVM on a paid bare-metal server.1 With Marimo Wasm, the "server" is the student's laptop. The infrastructure cost drops to near zero (bandwidth only).  
* **Offline Capability:** Once the Wasm bundle is downloaded, the student can continue working on the Maths syllabus without an active internet connection. This is vital for students in rural Ireland with poor broadband reliability, a constraint not present in the typically server-connected DevOps training world.  
* **Isolation:** The iximiuz model required "Bender" to set up intricate iptables rules to isolate VMs.1 Marimo Wasm runs inside the browser sandbox. It is isolated by design. A student cannot accidentally inspect another student's process because they are running on physically different machines.

### **5.3 Integration with TanStack Start**

The Marimo notebook is embedded into the TanStack Start application as a secure iframe or a web component. The Marimo export is served as a static asset from Cloudflare R2 (Object Storage).  
The Communication Bridge:  
To track progress (e.g., "Did the student correctly calculate the derivative?"), the Wasm environment must communicate with the main application. We establish a message protocol similar to the iximiuz "Conductor" but entirely client-side.

* **PostMessage API:** The Marimo notebook emits events via window.parent.postMessage.  
  * { type: "TASK\_COMPLETE", taskId: "calc\_deriv", payload: { result: 24 } }  
* **Validation:** The TanStack Start client receives the message. It then relays this to a Durable Object to update the student's grade. This mimics the "Examiner" daemon in the iximiuz architecture 1 but without the overhead of gRPC or SSH tunnels.

## ---

**6\. Heavy Compute Orchestration: Self-Hosted Coder**

While Marimo Wasm handles Python and Maths, the Leaving Cert Computer Science (LCCS) syllabus also includes topics like "Web Technologies" (hosting a server), "Database Design," and "Embedded Systems." These cannot be fully simulated in a browser-based Wasm environment due to browser sandbox restrictions (e.g., opening raw TCP ports, running Docker).  
To address this, we integrate **self-hosted Coder**, effectively replacing the "Bender" and "Worker Fleet" components from the iximiuz architecture 1 for these specific tasks.

### **6.1 Coder vs. Bender: The Orchestration Strategy**

The iximiuz "Bender" daemon was a custom-built Go application handling rootfs creation, network namespaces, and VM lifecycle.1 Building such a tool is complex, error-prone, and requires deep systems programming knowledge.

* **Coder:** An open-source platform that provisions remote development environments using Terraform. It acts as the control plane.  
* **Integration:** We deploy Coder on a dedicated server (or cluster), similar to the Hetzner worker nodes in the reference architecture 1, but we rely on Coder's mature codebase rather than maintaining a custom orchestration daemon.

### **6.2 The Hybrid Workflow**

The user interface (TanStack Start) determines the backend required for the lesson:

* **Lesson Type A (Maths/Basic Python):** Loads Marimo Wasm (Client-side). Cost: $0.  
* **Lesson Type B (Web Server/Database):** Calls the Coder API to provision a container. Cost: Marginal server time.

The Coder Template for LCCS:  
We define a Coder Terraform template specifically for the Irish syllabus:

* **Base Image:** Ubuntu or Alpine Linux.  
* **Tools:** Python 3, SQLite, HTML/CSS linters, and a bilingual man page system (custom alias wrapping man pages to provide Irish summaries).  
* **Isolation:** Coder manages the container/VM isolation using Docker or Podman. This mirrors the security goals of the iximiuz platform 1 but abstracts the complexity of manual networking.

### **6.3 Bridging Coder and the Frontend**

To achieve the seamless experience seen in labs.iximiuz.com, we embed the Coder IDE (VS Code Web) inside the TanStack Start dashboard.

* **Authentication Hand-off:** The Cloudflare Worker generates an OIDC token for Coder, logging the student in automatically.  
* **Iframe Embedding:** The Coder workspace is rendered inside the application layout, maintaining the bilingual navigation bar and instructional sidebar.  
* **Secure Tunneling:** We utilize **Cloudflare Tunnel** to expose the Coder workspaces. This ensures that no ports are open to the public internet. The Coder instance sits safely behind Cloudflare's Zero Trust firewall, replacing the complex Envoy proxy setup described in the iximiuz reference.1

## ---

**7\. State Management & Collaboration: Durable Objects**

In the iximiuz Labs story, a "Conductor" daemon managed WebSocket connections to stream terminal data.1 In our Cloudflare-based architecture, **Durable Objects (DO)** fulfill this role, acting as the stateful "brain" of each active session.

### **7.1 Replacing the Conductor**

The Conductor in the reference architecture was responsible for multiplexing SSH sessions and broadcasting task states.1 A Durable Object is a single instance of a class that guarantees strong consistency and unique addressing.  
The "Classroom" Object:  
We leverage the "Classroom" model for our Durable Objects.

1. **WebSocket Termination:** All students in a virtual class connect via WebSocket to a single Durable Object instance.  
2. **State Synchronization:** If a teacher wants to demonstrate a concept, they can broadcast commands through the DO. The DO relays these to the Marimo instances running in the students' browsers.  
3. **Presence & Telemetry:** The DO maintains a list of active users and their current progress (e.g., "Student A is on Step 3"). This replaces the need for a separate Redis cluster and the polling mechanisms in the iximiuz stack.1

### **7.2 Real-Time Bilingual Toggling**

One specific use case for Durable Objects in this context is managing the bilingual state during collaborative sessions. If a teacher switches the "Master View" to Irish, the DO broadcasts an event: { "action": "set\_lang", "lang": "ga" }. Every connected student client immediately updates the UI labels and glossary terms via the TanStack Start frontend, ensuring the entire class is synchronized on the terminology.

### **7.3 Persistence and Resume**

Unlike the ephemeral Firecracker VMs which are terminated after a session 1, Durable Objects provide transactional storage.

* **Code Storage:** When a student writes a Python script in a Marimo notebook, the code is periodically synced to the DO's internal storage via the WebSocket.  
* **Resumption:** If the student closes the tab and reopens it later, the DO serves the latest state immediately. This provides a "stateful serverless" experience that is difficult to achieve with standard cloud functions.

## ---

**8\. Pedagogical Engineering: Code-Switching & Assessment**

A unique aspect of this research request is the bilingual Irish-English requirement. This goes beyond simple translation; it requires **Code-Switching Pedagogy**.

### **8.1 The Interface of Code**

In Computer Science, keywords (print, if, while) are inextricably linked to English. This creates a cognitive load for Gaeilgeoirí (Irish speakers) who must mentally translate concepts before applying the syntax.

* **Strategy:** The platform provides a "Bilingual Linter" running in the Coder environment or Marimo.  
* **Feature:** If a student hovers over an English keyword, a tooltip explains the concept in Irish (e.g., while loop \-\> *lúb fhad is*).  
* **Implementation:** The Language Server Protocol (LSP) can be intercepted. We deploy a custom LSP proxy (via Cloudflare Workers or inside the Coder container) that injects these translation hints into the editor.

### **8.2 Bilingual Data Sets for Analytics**

For Data Analytics modules (Strand 2 of LCCS), the platform creates localized datasets.

* **English Context:** dataset.csv with columns "Name", "Age", "County".  
* **Irish Context:** tacar\_sonraí.csv with columns "Ainm", "Aois", "Contae".  
* **Architecture:** The Marimo notebook loads the dataset dynamically based on the URL locale parameter managed by TanStack Start. This allows the student to analyze data in their vernacular, lowering the barrier to entry for statistical concepts.

### **8.3 AI-Assisted Assessment (Replacing the Examiner)**

The iximiuz platform uses an "Examiner" daemon to run shell commands to verify state.1 We improve on this by using **Cloudflare Workers AI**.

* **Mechanism:** When a student submits a Python function in Marimo, the code is sent to a Worker.  
* **AI Analysis:** Instead of just checking if the output is correct (unit testing), we pipe the code to a Llama 3 model running on Cloudflare.  
* **Prompt:** "Analyze this Python code. Does it use a 'for' loop as requested? Is the variable naming descriptive? Reply in Irish."  
* **Result:** The student receives qualitative feedback in Irish ("*Maith thú\! D'úsáid tú lúb 'for' i gceart, ach déan iarracht ainmneacha níos fearr a thabhairt ar do athróga.*"). This level of semantic feedback is impossible with the rigid gRPC checks of the reference architecture.1

## ---

**9\. Implementation Details and Data Structures**

To substantiate the architecture, we detail the specific data structures and protocols that glue the components together.

### **9.1 The Marimo-to-Worker Protocol**

Unlike the SSH tunnel used in iximiuz 1, we use a lightweight JSON protocol over WebSockets.

| Field | Type | Description |
| :---- | :---- | :---- |
| event\_id | UUID | Unique identifier for deduplication |
| type | String | TASK\_SUBMIT, HEARTBEAT, ERROR |
| payload | Object | The code snippet or answer payload |
| timestamp | ISO8601 | Client-side timestamp |
| locale | String | en\_IE or ga\_IE |

This payload is processed by the Durable Object. The locale field ensures that any automated feedback generated by the system is returned in the correct language.

### **9.2 The Durable Object Class Structure**

The TypeScript definition for the LabSession Durable Object illustrates how it replaces the "Conductor" state machine.

TypeScript

export class LabSession implements DurableObject {  
  state: DurableObjectState;  
  sessions: Map\<string, WebSocket\>; // Map\<SessionID, WebSocket\>

  constructor(state: DurableObjectState, env: Env) {  
    this.state \= state;  
    this.sessions \= new Map();  
  }

  async fetch(request: Request) {  
    if (request.headers.get("Upgrade") \=== "websocket") {  
      const pair \= new WebSocketPair();  
      const \[client, server\] \= Object.values(pair);  
        
      // Handle the WebSocket connection  
      this.handleSession(server);  
      return new Response(null, { status: 101, webSocket: client });  
    }  
    //... handle standard HTTP requests for metadata  
  }

  async handleSession(ws: WebSocket) {  
    ws.accept();  
    // Replaces Conductor's stream multiplexing   
    ws.addEventListener("message", async (msg) \=\> {  
       const event \= JSON.parse(msg.data);  
       if (event.type \=== "TASK\_SUBMIT") {  
          // Trigger grading logic  
          await this.gradeTask(event.payload);  
       }  
    });  
  }  
}

### **9.3 Infrastructure-as-Code: The Coder Template**

To replace the custom "Bender" logic 1, we use Terraform within Coder.

Terraform

\# main.tf (Conceptual)  
resource "coder\_agent" "main" {  
  arch           \= "amd64"  
  os             \= "linux"  
  startup\_script \= \<\<EOT  
    \# Install Irish Language Pack aliases  
    echo "alias liosta='ls \-la'" \>\> /home/coder/.bashrc  
    \# Start web server for Strand 3  
    python3 \-m http.server 8080 &  
  EOT  
}

resource "docker\_container" "workspace" {  
  image \= "ghcr.io/leaving-cert/lccs-env:latest"  
  \#... resource limits  
}

This declarative approach is far more maintainable than the imperative Go code required for the iximiuz "Bender" daemon.1

## ---

**10\. Operational Logistics: Security, Scalability, and Cost**

### **10.1 Attack Surface Analysis**

* **Reference Model (iximiuz):** The "Bender" daemon runs as root on the host.1 A breakout from the Firecracker VM could theoretically compromise the bare metal server. The system relies on iptables and custom bridges, which are prone to misconfiguration.  
* **Proposed Model (Wasm):** Marimo runs in the browser. The attack surface is the student's own browser. A malicious script cannot affect the platform infrastructure because there is no server execution context for this workload.  
* **Proposed Model (Coder):** Coder environments are containers. While container escape is a risk, we mitigate this by running Coder on isolated ephemeral nodes (e.g., Fly.io Machines or distinct Hetzner instances) that are recycled after every session. We utilize **gVisor** (Google's sandboxed container runtime) within the Coder setup to provide near-VM isolation, matching the security profile of Firecracker 1 without the management overhead.

### **10.2 Scalability Profile**

* **iximiuz Model:** Scaling requires provisioning new bare-metal servers and joining them to the Foreman fleet.1 This is a linear scaling cost and slow to react to bursts.  
* **Proposed Model:**  
  * **Frontend/Wasm:** Scales infinitely on Cloudflare's global network. 10 students or 10,000 students cost roughly the same in terms of management effort.  
  * **Durable Objects:** Cloudflare automatically distributes DOs across their network.  
  * **Coder:** This is the bottleneck. However, since only \~20% of the syllabus (Strand 3 Web Dev) requires full environments, the scale factor is significantly reduced. We can set up Coder to auto-scale its compute nodes based on active workspace demand.

### **10.3 Cost Analysis**

The iximiuz author pays \~$40/month for a Hetzner server.1

* **Proposed Stack Costs:**  
  * **Cloudflare Workers/Pages:** The free tier is generous (100k requests/day). The Pro plan ($5/mo) handles millions.  
  * **Durable Objects:** Charged by request count and duration. For text-based state sync, this is negligible.  
  * **Self-hosted Coder:** Can run on a smaller VPS (e.g., $10-20/mo) because it handles only the "heavy" overflow, not the entire user base.  
  * **Total:** Comparable operational cost (\~$50/mo), but with significantly better performance (global CDN caching), higher reliability (no single point of failure like the Foreman), and massive burst capacity for exam periods.

## ---

**11\. Conclusion**

The architecture proposed herein leverages the specific strengths of the modern "Edge Stack" to solve the unique constraints of the Irish Leaving Certificate syllabus. By rejecting the premise that "everything must run on a server" (the iximiuz/Firecracker approach 1), we shift the paradigm to the client (Marimo/Wasm) for mathematical exploration and algorithmic thinking.  
We introduce complexity only where necessary—using self-hosted Coder for the systems programming aspects of the syllabus—and utilize Cloudflare Workers and Durable Objects to glue these disparate experiences into a cohesive, bilingual, and highly responsive educational platform. This design not only matches the interactive capability of the reference Docker platform but exceeds it in terms of interactivity (reactive maths), accessibility (offline Wasm), and operational efficiency (serverless orchestration).  
The result is a platform that does not merely digitize the textbook but creates a living, breathing, bilingual environment where Irish students can explore the frontiers of Computer Science and Mathematics with the same immediacy and power as professional engineers.

#### **Works cited**

1. Building a Firecracker-Powered Course Platform To Learn Docker and Kubernetes.pdf