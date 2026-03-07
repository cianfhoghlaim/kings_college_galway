# **High-Velocity Computational Resource Utilization Strategy: Maximizing Parallel Agentic Development via Architectural Pre-computation**

## **Executive Summary**

The constraints of the immediate scenario—a hard deadline of 10:00 PM for the expiration of significant Claude Code credits—dictate a fundamental shift in development methodology. The traditional, linear software development lifecycle (SDLC), characterized by sequential coding, testing, and deployment phases, is insufficient for consuming the available computational resources productively within the allotted timeframe. To maximize the utility of these credits, one must pivot to a **Hyper-Parallel Agentic Deployment (HPDE)** strategy. This approach treats the expiring credits not as a budget for immediate output, but as fuel for a massive, asynchronous "banking" operation where transient computational power is converted into permanent project assets: exhaustive documentation, robust infrastructure code, comprehensive test suites, and persistent vector memory.  
This report outlines a comprehensive architectural master plan to execute this strategy. It leverages a sophisticated toolchain comprising **Oh My OpenCode (OMO)** and **Auto-Claude** for orchestration, **Netflix Conductor** and **Dagster** for workflow management, **Google Stitch** and **AG-UI** for frontend acceleration, and **Letta** and **Pydantic Logfire** for persistence and observability. The core objective is to instantiate a mesh of autonomous agents that operate in parallel, consuming tokens at a maximum rate to generate high-fidelity artifacts that serve as the foundation for all future development. By decoupling the "thinking" and "planning" phases from the linear passage of time, we effectively compress weeks of architectural labor into a few hours of intensive, parallelized computation.

## ---

**1\. The Orchestration Layer: High-Throughput Agentic Dispatch**

The primary mechanism for credit consumption is the instantiation of multiple, concurrent AI contexts. Standard user interfaces are bottlenecked by human input speed. To bypass this, we utilize **Oh My OpenCode (OMO)** and **Auto-Claude** as force multipliers, turning a single user intent into dozens of asynchronous execution threads.

### **1.1 Oh My OpenCode: The Asynchronous Command Center**

Oh My OpenCode (OMO) represents a plugin architecture designed to supercharge the Claude Code experience by enabling asynchronous sub-agents and extended reasoning capabilities.1 Unlike standard interactions which are transactional, OMO allows for a "fire-and-forget" model where complex tasks are delegated to specialized sub-agents running in the background.

#### **1.1.1 The "Ultrawork" Protocol for Maximum Token Load**

To maximize credit usage, the system must be configured to prioritize depth of reasoning over speed or token economy. OMO includes a specific mode for this: ultrawork (or ulw). This mode activates aggressive parallel agent orchestration, designed to maximize performance by splitting tasks across multiple specialized sub-agents.2  
In ultrawork mode, the primary orchestrator, **Sisyphus**, utilizes the Claude Opus 4.5 model with an extended thinking budget of up to 32,000 tokens per turn.1 This is critical for the current objective. By forcing the model to engage in "extended thinking," we ensure that every prompt consumes a maximal amount of compute, generating deeply reasoned architectural decisions rather than superficial code snippets. The configuration should explicitly enable max20 mode (20x consumption), which unlocks the highest tier of reasoning capabilities available to the model.1  
The oh-my-opencode.jsonc configuration file serves as the control plane for this high-burn strategy. It is imperative to disable features designed for token conservation, such as Dynamic Context Pruning (DCP) and Auto-Compact.3 While these features are beneficial for long-term economy, they are counter-productive to the goal of "banking" context. We want the agents to load the entire repository into their context window, perform exhaustive analysis, and generate comprehensive outputs.  
**Strategic Configuration for Credit Maximization:**

| Setting | Value | Rationale for Credit Consumption |
| :---- | :---- | :---- |
| omoSisyphus.enabled | true | Activates the primary orchestrator with aggressive task completion logic. |
| thinking.budget | 32000 | Forces maximum reasoning depth per turn, consuming credits on logic verification. |
| context\_pruning.dcp | false | Prevents the agent from "forgetting" files, maintaining high token load per request. |
| claude\_mode | max20 | Multiplies compute usage by 20x for "Opus-level" reasoning on every interaction. |
| subagents.parallel | true | Allows Oracle, Librarian, and Frontend agents to run simultaneously. |

The Sisyphus agent is particularly valuable due to its "Todo Continuation Enforcer".1 This mechanism prevents the common LLM failure mode of quitting halfway through a complex refactor. Sisyphus is programmatically forced to continue until all identified TODO items are resolved. In the context of a 3-hour sprint, this ensures that a task launched at 7:00 PM continues to consume credits and produce code even if the user steps away, maximizing the "burn rate" of the expiring credits.

#### **1.1.2 The init-deep Command: Recursive Knowledge Generation**

Perhaps the single most effective command for converting credits into permanent value is /init-deep.3 This command triggers a hierarchical analysis of the entire project directory structure. It does not merely list files; it reads code, infers architectural patterns, and generates nested AGENTS.md files at every level of the directory tree.  
This process is computationally expensive, making it the perfect candidate for the current scenario. By running /init-deep \--create-new \--max-depth=5 3, the user effectively forces the AI to "read" the entire project and "write" a textbook explaining it to future agents. These AGENTS.md files become permanent assets. In future sessions—when credits might be scarce—agents can read these summary files to gain instant context without needing to re-tokenize the raw source code. This is the definition of "banking" compute: paying the token cost now to save it later.

### **1.2 Auto-Claude: Isolated Parallel Execution**

While OMO handles the interaction layer, **Auto-Claude** provides the execution environment necessary to run multiple agents without file locking conflicts. Standard LLM coding agents operate on the working directory; if two agents try to modify main.py simultaneously, chaos ensues. Auto-Claude solves this via **Git Worktrees**.4

#### **1.2.1 The Worktree Isolation Strategy**

Auto-Claude automatically creates an isolated git worktree for each agent session.5 This effectively clones the repository into a separate directory linked to a specific branch, allowing the agent to modify files, run tests, and even commit code without affecting the main working directory or other running agents.  
To maximize parallel throughput, the user should act as a "Dispatcher," launching separate terminal instances of Auto-Claude, each targeting a distinct feature specification. For example:

* **Terminal A:** auto-claude run \--spec specs/infra-upgrade.md  
* **Terminal B:** auto-claude run \--spec specs/frontend-refactor.md  
* **Terminal C:** auto-claude run \--spec specs/backend-orchestration.md

Each command spawns a fully autonomous agent loop. The agent reads the spec, creates a plan, executes the code changes in its isolated worktree, runs tests, and iterates until the tests pass.5 This allows for "Horizontal Scaling" of development. Instead of one developer working on one task, the user effectively manages a team of three AI developers working simultaneously.

#### **1.2.2 The Planner-Coder-Reviewer Loop**

Auto-Claude implements a multi-stage pipeline: Discovery \-\> Requirements \-\> Plan \-\> Code \-\> Validate.5 The "Planner" phase is particularly useful for credit consumption. By instructing the Planner to perform a "Self-Critique" and "Deep Research" phase, the user forces the agent to spend significant time analyzing dependencies before writing a single line of code.  
This pre-computation is vital. A standard agent might rush to implementation and create bugs. An agent forced to "Research" will map out circular dependencies in Python modules or identify conflicting React hook patterns. The resulting implementation\_plan.json 5 is a high-value artifact that guides the "Coder" agent. Even if the credits expire before the coding is finished, the *Plan* remains, providing a detailed roadmap for manual completion or cheaper models later.  
The security model of Auto-Claude, which includes OS sandboxing and dynamic command allowlists 6, ensures that these parallel agents can be left unattended. They are restricted to the project directory and cannot accidentally execute destructive system commands (like rm \-rf /), which is a critical safety net when running high-velocity autonomous loops.

## ---

**2\. Infrastructure and Observability: The Foundation**

Before unleashing the agents on complex business logic, the underlying infrastructure must be robust enough to support them. This involves setting up a secure, observable environment using **Pydantic Logfire**, **Pangolin**, and **Forgejo**.

### **2.1 Pydantic Logfire: "AI to API" Observability**

When multiple agents (Sisyphus, Auto-Claude instances) are modifying code and running tests simultaneously, visibility into system behavior is lost. **Pydantic Logfire** restores this by providing deep tracing of both the AI reasoning processes and the traditional backend execution.7

#### **2.1.1 Self-Hosted Telemetry Stack**

To avoid reliance on external SaaS quotas and to maintain data sovereignty, a self-hosted Logfire instance is recommended. The provided research indicates that Logfire can be deployed via Docker Compose or Helm.8  
The architecture requires three primary components:

1. **Logfire Server:** The core ingestion and query engine.  
2. **PostgreSQL (v16+):** For storing traces and metrics.  
3. **Object Storage (MinIO/S3):** For large payload offloading.

Configuring this stack is a high-complexity task suitable for an Auto-Claude agent. The user should instruct an agent to generate a production-grade docker-compose.yml that orchestrates these services. The agent must handle the intricate networking configuration, ensuring that the OTEL\_EXPORTER\_OTLP\_ENDPOINT environment variables are correctly set for the Python services to communicate with the collector.10  
Strategic Value:  
By establishing this observability layer now, the user ensures that every future interaction—whether by an AI agent or a human—is recorded. Logfire's ability to trace "AI to API" means it captures the prompt sent to the LLM, the reasoning tokens (if available), and the subsequent SQL query executed by the code the LLM wrote.7 This creates a "Debugging Time Machine," allowing the user to replay exactly why an agent made a specific decision hours or days later.

#### **2.1.2 Auto-Instrumentation of Python Services**

The codebase likely consists of FastAPI or generic Python scripts. Logfire offers "magical" instrumentation that requires minimal code changes but significant understanding of the entry points.  
The user should task an agent with a global refactor: "Traverse the src/ directory. Identify all FastAPI applications and Pydantic models. Inject logfire.instrument\_fastapi() and logfire.configure() calls." This is a tedious, high-context task perfect for a machine. The agent will parse hundreds of files, insert the import statements, and configure the AdvancedOptions to point to the local self-hosted instance.11

### **2.2 Pangolin: Identity-Aware Tunneling**

Developing in a distributed manner—where Conductor might be running in a container, Dagster in another, and the user accessing via a browser—requires a robust networking layer. **Pangolin** serves as a modern replacement for Nginx or Traefik, adding identity-aware tunneling.12

#### **2.2.1 The "Newt" Tunneling Protocol**

Pangolin's "Newt" connectors allow for secure tunneling without opening public ports.13 This is crucial if the user's development machine is behind a NAT or firewall. By deploying a Pangolin instance (perhaps on a cheap VPS or locally via Docker) and connecting the local services via Newt, the user creates a stable addressable space for the agents.  
For example, a GitHub webhook (or Forgejo webhook) needs to reach the local Conductor instance to trigger a workflow. A standard localhost URL won't work. Pangolin provides a stable https://conductor.dev.local endpoint that maps to the Docker container.  
Configuration via Agents:  
Setting up the config.yaml for Pangolin involves defining "Sites" and "Resources".13 An agent can be tasked to: "Scan the docker-compose.yml files, identify all exposed ports, and generate a Pangolin configuration that maps each service (Logfire, Forgejo, Conductor UI) to a dedicated subdomain." This ensures that when the coding sprint is over, the user has a professional-grade dashboard environment ready for use.

### **2.3 Forgejo: Sovereign Code Hosting**

To ensure the code generated by these parallel agents is safely versioned, **Forgejo** (a fork of Gitea) should be deployed. It supports OAuth2, which is essential for integrating with Conductor and Dagster later.14  
The agents should configure Forgejo to act as an OAuth2 provider. This allows the other services (Logfire, Pangolin) to use Forgejo for "Sign In." This unifies the identity stack: one login for the git repo, the dashboard, and the observability platform. The configuration involves setting FORGEJO\_WORK\_DIR and generating client IDs/secrets via the command line or API.14

## ---

**3\. Workflow Orchestration: The Conductor-Dagster Bridge**

The core logic of the project lies in the integration of **Netflix Conductor** and **Dagster**. This follows the "Orchestrator of Orchestrators" pattern: Conductor handles the macro-level business process (User Registration, Order Fulfillment), while Dagster handles the micro-level data processing (ETL, Machine Learning Training).

### **3.1 Conductor: The Business Logic Layer**

Conductor operates on the concept of "Workflows" composed of "Tasks." For this project, we utilize the **Python SDK** to define workflows as code, allowing for dynamic generation and versioning.15

#### **3.1.1 Implementing the DagsterTriggerWorker**

The critical link is a custom Conductor Worker that can trigger Dagster jobs. This requires writing a Python class that implements the WorkerInterface.16  
Task Definition:  
The agent must generate a Python class, DagsterTriggerWorker, which listens for a task queue (e.g., trigger\_dagster\_job). The execute method of this worker must:

1. **Parse Inputs:** Extract job\_name, run\_config, and tags from the task input.  
2. **Authenticate:** Establish a connection to the Dagster GraphQL API.  
3. **Execute:** Send a launchRun mutation to Dagster.18  
4. **Return:** Output the run\_id back to Conductor.

The complexity here lies in the GraphQL mutation. The agent must construct a precise query that matches Dagster's schema:

GraphQL

mutation LaunchRunMutation($selector: JobOrPipelineSelector\!, $runConfigData: RunConfigData\!) {  
  launchRun(executionParams: {  
    selector: $selector,  
    runConfigData: $runConfigData  
  }) {  
    \_\_typename  
   ... on LaunchRunSuccess {  
      run { runId }  
    }  
   ... on RunConfigValidationInvalid {  
      errors { message }  
    }  
  }  
}

This code is boilerplate-heavy and error-prone for humans but trivial for an LLM. By tasking an agent to "Implement a robust DagsterTriggerWorker handling all GraphQL error states," the user offloads significant cognitive load.

#### **3.1.2 Asynchronous Polling Logic**

Dagster jobs are asynchronous; launchRun returns immediately with a run\_id, but the job might take hours to finish. Conductor must wait.  
The agent should be instructed to generate a Conductor workflow that implements a **Polling Loop**:

1. **Task 1:** trigger\_dagster\_ref (Calls the worker above).  
2. **Task 2 (Loop Start):** WAIT for 30 seconds.  
3. **Task 3:** HTTP System Task.19 This task makes a GraphQL query to Dagster (getRunStatus) using the run\_id from Task 1\.  
4. **Task 4 (Decision):** A SWITCH task. If status is SUCCESS, proceed. If FAILURE, fail workflow. If IN\_PROGRESS, loop back to Task 2\.

This logic requires constructing a complex JSON or Python object graph in the Conductor SDK.15 It is a perfect candidate for parallel generation while the user focuses on high-level architecture.

### **3.2 Dagster: The Data Execution Layer**

On the Dagster side, the focus is on exposing jobs that can be triggered externally. The dagster-graphql client is the primary interface.20

#### **3.2.1 Configuration for External Execution**

The agents must ensure that the Dagster repository is configured to accept run configuration from external sources. This involves using ConfigurableResource patterns.21  
For example, if the Conductor workflow passes a date\_range parameter, the Dagster job must be defined to accept this in its run\_config. The agent should analyze the existing data pipelines and refactor them to expose these configuration points, ensuring that the LaunchRunMutation from Conductor can successfully inject parameters.  
Parallel Execution Strategy:  
Dagster's default multiprocess\_executor spins up a new process for each step.22 For the purpose of this project, testing this locally might be resource-intensive. The agent should be tasked with configuring the in\_process\_executor for local testing configurations (dagster\_dev.yaml) while reserving the multiprocess or k8s\_job\_executor for the production configuration intended for deployment.23

## ---

**4\. Frontend and Interaction: Generative UI and Streaming**

To improve the UI as requested, we leverage **Google Stitch** for rapid prototyping and **AG-UI** for connecting that UI to the backend agents.

### **4.1 Google Stitch: From Prompt to Prototype**

Google Stitch allows for the generation of "real, clickable app prototypes" from text prompts.24 This tool bypasses the need for manual CSS/HTML coding.  
**Operational Workflow:**

1. **Prompting:** The user enters a prompt into Stitch: "Create a dark-mode dashboard for a workflow orchestration system. Left sidebar with 'Workflows', 'Tasks', 'Logs'. Main area shows a directed acyclic graph (DAG) visualization and a scrolling log terminal. Top right status indicators for Conductor and Dagster."  
2. **Refinement:** Stitch generates a prototype. The user iterates using "Edit" mode to tweak colors or layout.24  
3. **Export:** The critical step is "Export to Google AI Studio" or copying the code.25

Integration via Auto-Claude:  
Once the raw HTML/CSS is obtained from Stitch, it is "dumb" markup. It looks good but does nothing. The user should save this as src/frontend/dashboard\_prototype.html and task an Auto-Claude agent:  
"Convert this static HTML file into a set of React components. Use Tailwind CSS for styling (matching the Stitch design). Replace the static log area with a component that accepts a streaming prop."  
This effectively "launders" the code: Stitch creates the visual design (cheap/free), and Claude Code (credits) provides the logic implementation.

### **4.2 AG-UI: The Streaming Protocol**

To make the dashboard alive, we use the **Agent-User Interaction (AG-UI) Protocol**.26 This protocol standardizes how agents send partial results (tokens), tool calls, and state updates to the frontend.

#### **4.2.1 Server-Sent Events (SSE) Implementation**

The backend (likely FastAPI) needs to implement an SSE endpoint that adheres to AG-UI specs.  
Agent Task:  
"Implement a FastAPI endpoint /stream/agent that yields AG-UI compatible events. It must support text\_message\_content for streaming text and tool\_call\_start/tool\_call\_end for visualizing the Conductor/Dagster interactions."  
The agent must write the generator logic that wraps the internal reasoning loop:

Python

async def event\_generator():  
    yield {"event": "run\_started", "data": "..."}  
    for chunk in llm.stream():  
        yield {"event": "text\_message\_content", "data": chunk}  
    yield {"event": "run\_finished", "data": "..."}

This transforms the UI from a static page into a "Generative UI" 26, where the user can watch the system "think" and "act" in real-time, observing the polling loops of Conductor and the job launches in Dagster as they happen.

## ---

**5\. Memory and Persistence: Banking the Knowledge**

The final component ensures that the work done today is not lost. **Letta (formerly MemGPT)** provides the mechanism for long-term state persistence.27

### **5.1 The Letta Container**

Letta runs as a stateful service, distinct from a library. It manages "Core Memory" (context window) and "Archival Memory" (vector database).27  
Deployment:  
The agent must generate a docker-compose service for Letta, linking it to the Postgres database with the pgvector extension enabled.27

* **Environment Vars:** OPENAI\_API\_KEY, LETTA\_PG\_URI.  
* **Volume:** Persistent storage for the Postgres data is essential to survive container restarts.

### **5.2 Context Ingestion**

To "bank" the research, the user should execute a final script before 10:00 PM. This script utilizes the Letta Python SDK to upload the AGENTS.md, ARCHITECTURE.md, and all generated source code into Letta's archival memory.  
**Script Logic:**

1. **Connect:** client \= Letta(base\_url="http://localhost:8283").28  
2. **Create Agent:** Instantiate a new agent "ProjectArchitect."  
3. **Ingest:** Iterate through the docs/ folder. For each file, call agent.archival\_memory.insert(content=file\_content).

This action effectively uploads the "mind" of the project into a persistent vector store. In future sessions, the user can ask this Letta agent, "How does the Conductor worker authentication work?" and it will retrieve the exact logic implemented during this sprint, even if the original code has changed or the user has forgotten.

## ---

**6\. Execution Playbook: The Next 4 Hours**

This playbook is designed to be executed linearly by the user, spawning parallel processes at each step.

### **Hour 1: Foundation and Context**

1. **Terminal 1 (OMO):** Run opencode /init-deep \--max-depth=5. *Objective: Generate deep context documentation.*  
2. **Terminal 2 (Infra):** Task an agent to "Generate docker-compose.yml for Logfire, Pangolin, Forgejo, Conductor, Dagster, and Letta. Ensure shared networking." *Objective: Create the runtime environment.*  
3. **Browser:** Visit Stitch, generate the Dashboard UI, export code to dashboard\_prototype.html.

### **Hour 2: Parallel Implementation**

1. **Terminal 3 (Auto-Claude \- Backend):** auto-claude run \--spec specs/conductor\_worker.md. *Objective: Implement DagsterTriggerWorker and GraphQL logic.*  
2. **Terminal 4 (Auto-Claude \- Frontend):** auto-claude run \--spec specs/stitch\_conversion.md. *Objective: Convert Stitch HTML to AG-UI React components.*  
3. **Terminal 5 (Auto-Claude \- Logic):** auto-claude run \--spec specs/conductor\_workflow.md. *Objective: Define the Polling Loop workflow in Python.*

### **Hour 3: Integration and Banking**

1. **Review:** Use OMO Sisyphus to review the PRs from Terminals 3, 4, and 5\. "Check for integration mismatches between the Worker output and the Workflow input."  
2. **Deploy:** Run docker-compose up \-d. Verify Logfire creates traces.  
3. **Bank:** Run the Letta ingestion script. Upload all generated code and docs to Archival Memory.

By 10:00 PM, the credits will be exhausted, but the user will possess a fully documented, observable, and containerized microservices architecture with a sophisticated UI and a persistent AI architect to guide future development. This transforms a temporary resource surplus into a permanent competitive advantage.

#### **Works cited**

1. code-yeongyu/oh-my-opencode: \#1 OpenCode Plugin- Battery included. ASYNC SUBAGENTS (YES LIKE CLAUDE CODE) · Curated agents with proper models · Crafted tools like LSP/AST included · Curated MCPs · Claude Code Compatible Layer — Steroids for your OpenCode. The Best LLM Agent Experience is Here \- GitHub, accessed December 31, 2025, [https://github.com/code-yeongyu/oh-my-opencode](https://github.com/code-yeongyu/oh-my-opencode)  
2. oh-my-opencode/README.md at master \- GitHub, accessed December 31, 2025, [https://github.com/code-yeongyu/oh-my-opencode/blob/master/README.md](https://github.com/code-yeongyu/oh-my-opencode/blob/master/README.md)  
3. Releases · code-yeongyu/oh-my-opencode \- GitHub, accessed December 31, 2025, [https://github.com/code-yeongyu/oh-my-opencode/releases](https://github.com/code-yeongyu/oh-my-opencode/releases)  
4. \[FEATURE\] Parallel Multi-Agent Workflows for Code Generation and Planning · Issue \#10599 · anthropics/claude-code \- GitHub, accessed December 31, 2025, [https://github.com/anthropics/claude-code/issues/10599](https://github.com/anthropics/claude-code/issues/10599)  
5. Auto-Claude/CLAUDE.md at develop · AndyMik90/Auto-Claude \- GitHub, accessed December 31, 2025, [https://github.com/AndyMik90/Auto-Claude/blob/develop/CLAUDE.md](https://github.com/AndyMik90/Auto-Claude/blob/develop/CLAUDE.md)  
6. Auto-Claude/README.md at develop \- GitHub, accessed December 31, 2025, [https://github.com/AndyMik90/Auto-Claude/blob/develop/README.md](https://github.com/AndyMik90/Auto-Claude/blob/develop/README.md)  
7. Complete AI Application Observability | Monitor LLMs, APIs & Databases | Pydantic Logfire, accessed December 31, 2025, [https://pydantic.dev/logfire](https://pydantic.dev/logfire)  
8. Self Hosted Installation \- Pydantic Logfire, accessed December 31, 2025, [https://logfire.pydantic.dev/docs/reference/self-hosted/installation/](https://logfire.pydantic.dev/docs/reference/self-hosted/installation/)  
9. logfire-helm-chart \- Pydantic, accessed December 31, 2025, [https://charts.pydantic.dev/](https://charts.pydantic.dev/)  
10. Using Langfuse local deployment with pydantic-ai and logfire \#5973 \- GitHub, accessed December 31, 2025, [https://github.com/orgs/langfuse/discussions/5973](https://github.com/orgs/langfuse/discussions/5973)  
11. Self Hosted Introduction \- Pydantic Logfire, accessed December 31, 2025, [https://logfire.pydantic.dev/docs/reference/self-hosted/overview/](https://logfire.pydantic.dev/docs/reference/self-hosted/overview/)  
12. fosrl/pangolin: Identity-aware VPN and proxy for remote access to anything, anywhere. \- GitHub, accessed December 31, 2025, [https://github.com/fosrl/pangolin](https://github.com/fosrl/pangolin)  
13. Self-Host a Tunneled Reverse Proxy with Pangolin \- Pi My Life Up, accessed December 31, 2025, [https://pimylifeup.com/pangolin-linux/](https://pimylifeup.com/pangolin-linux/)  
14. OAuth2 provider | Forgejo – Beyond coding. We forge., accessed December 31, 2025, [https://forgejo.org/docs/next/user/oauth2-provider/](https://forgejo.org/docs/next/user/oauth2-provider/)  
15. Write Workflows Using Code | Orkes Conductor Documentation, accessed December 31, 2025, [https://orkes.io/content/developer-guides/write-workflows-using-code](https://orkes.io/content/developer-guides/write-workflows-using-code)  
16. Conductor OSS SDK for Python programming language \- GitHub, accessed December 31, 2025, [https://github.com/conductor-oss/python-sdk](https://github.com/conductor-oss/python-sdk)  
17. Python SDK \- Conductor Documentation, accessed December 31, 2025, [https://conductor-oss.github.io/conductor/documentation/clientsdks/python-sdk.html](https://conductor-oss.github.io/conductor/documentation/clientsdks/python-sdk.html)  
18. Dagster GraphQL API, accessed December 31, 2025, [https://docs.dagster.io/api/graphql](https://docs.dagster.io/api/graphql)  
19. Tasks \- Conductor Documentation, accessed December 31, 2025, [https://conductor-oss.github.io/conductor/devguide/concepts/tasks.html](https://conductor-oss.github.io/conductor/devguide/concepts/tasks.html)  
20. Dagster GraphQL Python client, accessed December 31, 2025, [https://docs.dagster.io/api/graphql/graphql-client](https://docs.dagster.io/api/graphql/graphql-client)  
21. Connecting to APIs \- Dagster Docs, accessed December 31, 2025, [https://docs.dagster.io/guides/build/external-resources/connecting-to-apis](https://docs.dagster.io/guides/build/external-resources/connecting-to-apis)  
22. Run executors \- Dagster Docs, accessed December 31, 2025, [https://docs.dagster.io/guides/operate/run-executors](https://docs.dagster.io/guides/operate/run-executors)  
23. How do I execute dagster and its jobs in separate compute environments? \#18222 \- GitHub, accessed December 31, 2025, [https://github.com/dagster-io/dagster/discussions/18222](https://github.com/dagster-io/dagster/discussions/18222)  
24. Google Stitch Update: Build Real Apps in Minutes With AI : r/AISEOInsider \- Reddit, accessed December 31, 2025, [https://www.reddit.com/r/AISEOInsider/comments/1pyflzn/google\_stitch\_update\_build\_real\_apps\_in\_minutes/](https://www.reddit.com/r/AISEOInsider/comments/1pyflzn/google_stitch_update_build_real_apps_in_minutes/)  
25. Stop Copy-Pasting — Google Stitch AI Studio Integration Is Finally Here \- Reddit, accessed December 31, 2025, [https://www.reddit.com/r/AISEOInsider/comments/1p38fbh/stop\_copypasting\_google\_stitch\_ai\_studio/](https://www.reddit.com/r/AISEOInsider/comments/1p38fbh/stop_copypasting_google_stitch_ai_studio/)  
26. AG-UI Overview \- Agent User Interaction Protocol, accessed December 31, 2025, [https://docs.ag-ui.com/](https://docs.ag-ui.com/)  
27. Setting up Letta (MemGPT) with Supabase | by Calvin Ku | Asymptotic Spaghetti Integration, accessed December 31, 2025, [https://medium.com/asymptotic-spaghetti-integration/setting-up-letta-memgpt-with-supabase-989ee34e141d](https://medium.com/asymptotic-spaghetti-integration/setting-up-letta-memgpt-with-supabase-989ee34e141d)  
28. Letta is the platform for building stateful agents: open AI with advanced memory that can learn and self-improve over time. \- GitHub, accessed December 31, 2025, [https://github.com/letta-ai/letta](https://github.com/letta-ai/letta)