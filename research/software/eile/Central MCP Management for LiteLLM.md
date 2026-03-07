# **Unified Model Context Protocol (MCP) Orchestration: Centralized Management of Hybrid AI Architectures**

## **1\. Introduction: The Evolution of AI Tooling Architectures**

The operational landscape of Generative AI development has undergone a seismic shift with the introduction of the Model Context Protocol (MCP). Historically, the integration of Large Language Models (LLMs) with external tools—databases, filesystems, web browsers, and proprietary APIs—relied on bespoke, proprietary implementation layers. OpenAI had its "Function Calling" definitions, Anthropic utilized "Tool Use" XML structures, and LangChain attempted to unify these through abstract wrappers. This fragmentation forced developers into a "mesh" topology where every client application had to be individually configured to talk to every tool, resulting in massive configuration redundancy, security vulnerabilities, and maintenance overhead.  
The user's objective—to unify the consumption of self-hosted open-weights models (specifically Qwen3-VL in GGUF format) and proprietary MCP servers (Z.ai Vision, Browserbase) across a diverse array of clients (Claude Code, Roo Code, GitHub Copilot)—represents the vanguard of modern AI systems engineering. It necessitates a move away from the decentralized mesh topology toward a **Centralized Gateway Architecture**.  
In this architecture, a middleware layer acts as the universal broker. It handles the lifecycle of local tools, manages authentication with remote services, translates between disparate model API formats, and exposes a singular, standardized interface to all downstream clients. This report rigorously analyzes the implementation of such an architecture using **LiteLLM** as the central orchestration engine. We will explore the theoretical underpinnings of MCP transport mechanisms, the practicalities of bridging local stdio processes to network-addressable endpoints, and the nuanced configuration required to make open-source Vision Language Models (VLMs) like Qwen3-VL interoperate seamlessly with tools designed for proprietary frontier models.

### **1.1 The Configuration Sprawl Problem**

In a standard, unmanaged setup, a developer utilizing three clients (Claude Code, Roo Code, GitHub Copilot) and three tool sources (Z.ai, Browserbase, Local Filesystem) faces a combinatorial explosion of configuration.

* **Redundancy:** API keys for Z.ai and Browserbase must be replicated in \~/.claude/config.json, .vscode/mcp.json, and project-specific environment variables.1  
* **Version Drift:** Local stdio servers often depend on specific Node.js or Python runtimes. If Claude Code spawns a process using the system Node version, while Roo Code uses a version managed by nvm or a VS Code extension, subtle incompatibilities in tool execution can arise.4  
* **Resource Contention:** A local "mesh" setup means every client spawns its own instance of the MCP server. If three clients are open, three separate headless browsers (via Browserbase or Puppeteer) might be running simultaneously, consuming excessive RAM and CPU cycles.  
* **Context Fragmentation:** A critical operational flaw in decentralized setups is the lack of shared state. A browsing session initiated in Roo Code via Browserbase is invisible to Claude Code. This prevents multi-agent workflows where one agent performs research and another performs coding based on that research.6

### **1.2 The Gateway Solution**

The proposed solution places **LiteLLM** at the center of the infrastructure. LiteLLM is not merely a model proxy; it has evolved into a comprehensive MCP Gateway. By configuring LiteLLM to manage the connections to Z.ai and Browserbase, and to proxy requests to the Qwen3-VL inference engine (e.g., Ollama), we achieve a **Hub-and-Spoke** topology. Clients connect only to LiteLLM. Tools connect only to LiteLLM.  
This report will detail the precise configuration steps, architectural decisions, and troubleshooting methodologies required to realize this unified vision, ensuring that the final system is robust, secure, and easily extensible.

## ---

**2\. Technical Foundations: The Model Context Protocol (MCP)**

To understand *why* a central gateway is necessary, one must first understand the mechanics of MCP transport layers. The protocol defines how clients (LLMs/Agents) discover and invoke tools provided by servers.

### **2.1 Transport Mechanisms: Stdio vs. SSE**

MCP supports two primary transport mechanisms, each with distinct advantages and limitations that directly impact our centralization strategy.

#### **2.1.1 Standard Input/Output (Stdio)**

The stdio transport is the default for local integrations. The client application (e.g., VS Code) acts as the parent process and spawns the MCP server (e.g., a Node.js script) as a subprocess. Communication occurs over the standard input (stdin) and standard output (stdout) streams of the subprocess.

* **Advantages:** Zero network latency, simplified security (no open ports), and shared lifecycle (server dies when client dies).  
* **Disadvantages:** It is inherently **single-tenant**. Only the process that spawned the server can communicate with it. This is the root cause of the "configuration sprawl" user experience—every client must be configured to spawn its own copy of the tool.7

#### **2.1.2 Server-Sent Events (SSE) / HTTP**

The SSE transport mechanism decouples the client from the server. The MCP server runs as a standalone web service (often wrapped in a simple HTTP server). The client connects to an endpoint (e.g., http://localhost:8080/sse) to receive updates and sends requests via HTTP POST.

* **Advantages:** **Multi-tenancy**. Multiple clients can connect to the same URL. The server can persist independently of the clients. It allows for remote execution (hosting tools on a separate server or container).  
* **Disadvantages:** Requires managing a long-running process, dealing with network security (CORS, Firewalls), and potential latency introduction.

### **2.2 The Bridging Imperative**

The core technical challenge addressed in this report is **Bridging**. Most local tools, including the Z.ai Vision MCP and Browserbase MCP (when running locally), are distributed as NPM packages designed for stdio execution. To centralize them, we must wrap these stdio processes in a service that exposes them via SSE.  
LiteLLM performs this function natively. It acts as the "Parent Process" for the stdio tools, keeping them alive, and then re-broadcasts their capabilities over its own HTTP/SSE interface. This "Bridge" pattern allows clients that support remote MCP (like Roo Code and GitHub Copilot) to access local command-line tools as if they were remote web services.9

## ---

**3\. The Aggregation Layer: LiteLLM Architecture**

The Aggregation Layer is the heart of our centralized architecture. It serves two distinct functions: **Model Proxy** (standardizing the Qwen3-VL API) and **Tool Gateway** (hosting Z.ai and Browserbase).

### **3.1 Docker-Based Deployment Strategy**

To ensure consistency and manage the dependencies required by the MCP tools (specifically Node.js for Z.ai and Browserbase), LiteLLM should be deployed via Docker. However, the standard LiteLLM Docker image is optimized for Python and does not contain the Node.js runtime required to execute npx commands.  
**Crucial Insight:** attempting to run command: "npx" inside a standard Python container will result in a "command not found" error. Therefore, a custom Docker image is mandatory for this hybrid setup.

#### **3.1.1 Custom Dockerfile Construction**

We must extend the base LiteLLM image to include the necessary runtimes.

Dockerfile

\# Base image: Official LiteLLM  
FROM ghcr.io/berriai/litellm:main-latest

\# INSIGHT: The base image lacks Node.js.   
\# Z.ai and Browserbase MCPs are Node.js applications distributed via npm.  
\# We must install Node.js and npm to allow 'npx' execution within the container.  
USER root  
RUN apt-get update && \\  
    apt-get install \-y nodejs npm curl && \\  
    apt-get clean && \\  
    rm \-rf /var/lib/apt/lists/\*

\# Optional: Pre-install global packages to improve startup time and cache layers.  
\# This avoids fetching packages every time the container restarts.  
RUN npm install \-g @z\_ai/mcp-server @browserbasehq/mcp-server-browserbase

\# Return to the default user for security  
USER litellm

\# Set working directory  
WORKDIR /app

\# Copy configuration  
COPY config.yaml /app/config.yaml

\# Entrypoint  
CMD \["--config", "/app/config.yaml", "--detailed\_debug"\]

This configuration ensures that when LiteLLM attempts to spawn the Z.ai MCP server using npx, the environment is compliant.10

### **3.2 The Master Configuration (config.yaml)**

The config.yaml file acts as the single source of truth for the entire AI infrastructure. It defines the models, the tools, and the security policies.

#### **3.2.1 Environment Variable Management**

Hardcoding API keys in config.yaml is a security anti-pattern. LiteLLM supports reading from the container's environment variables. This allows us to inject secrets at runtime via Docker Compose.

YAML

environment\_variables:  
  \# Z.ai Vision MCP Credentials  
  Z\_AI\_API\_KEY: "os.environ/Z\_AI\_API\_KEY"  
  Z\_AI\_MODE: "ZAI" \# or ZHIPU depending on subscription \[11\]  
    
  \# Browserbase Credentials  
  BROWSERBASE\_API\_KEY: "os.environ/BROWSERBASE\_API\_KEY"  
  BROWSERBASE\_PROJECT\_ID: "os.environ/BROWSERBASE\_PROJECT\_ID"  
    
  \# Qwen3-VL Backend (Ollama running on host)  
  \# 'host.docker.internal' allows the container to talk to the host machine  
  OLLAMA\_API\_BASE: "http://host.docker.internal:11434"

#### **3.2.2 Model Configuration (Qwen3-VL)**

Configuring Vision Language Models (VLMs) like Qwen3-VL in a proxy setup requires specific attention to how tool calls are handled. Open-weights models often deviate from the strict OpenAI tool-calling schema.

YAML

model\_list:  
  \- model\_name: qwen3-vl  
    litellm\_params:  
      model: ollama/qwen3-vl:fp16  \# Matches the tag in 'ollama list'  
      api\_base: os.environ/OLLAMA\_API\_BASE  
      \# INSIGHT: VLMs often struggle with structured tool outputs.  
      \# Enabling this flag hints LiteLLM to parse tool calls aggressively.  
      supports\_function\_calling: true  
      \# Standardize the input format to OpenAI chat completions  
      input\_cost\_per\_token: 0.000001 \# Optional cost tracking  
      output\_cost\_per\_token: 0.000002

#### **3.2.3 MCP Server Configuration**

This is the critical section where we define the "Bridge." We configure LiteLLM to launch the local stdio tools and expose them.

YAML

mcp\_servers:  
  \# SERVER 1: Z.ai Vision MCP  
  \# We use 'stdio' transport so LiteLLM manages the process.  
  z\_ai\_vision:  
    transport: "stdio"  
    command: "npx"   
    \# The '-y' flag suppresses "Need to install..." prompts which hang headless processes  
    args: \["-y", "@z\_ai/mcp-server"\]   
    env:  
      Z\_AI\_API\_KEY: os.environ/Z\_AI\_API\_KEY  
      Z\_AI\_MODE: os.environ/Z\_AI\_MODE  
    \# Access Groups allow precise permission scoping later  
    access\_groups: \["vision\_team", "global"\]

  \# SERVER 2: Browserbase MCP  
  browserbase\_agent:  
    transport: "stdio"  
    command: "npx"  
    args: \["@browserbasehq/mcp-server-browserbase", "--proxies"\]  
    env:  
      BROWSERBASE\_API\_KEY: os.environ/BROWSERBASE\_API\_KEY  
      BROWSERBASE\_PROJECT\_ID: os.environ/BROWSERBASE\_PROJECT\_ID  
    access\_groups: \["web\_team", "global"\]

**Architectural Insight:** By using transport: "stdio" here, we effectively treat the remote-capable tools (distributed via NPM) as local resources within the Gateway container. This is generally more reliable than configuring them as separate Docker containers and trying to network them together, as it avoids complex CORS and internal networking issues between containers. LiteLLM handles the stdio streams directly.9

### **3.3 Deploying the Gateway (Docker Compose)**

The docker-compose.yml file orchestrates the deployment, networking, and secret injection.

YAML

services:  
  litellm:  
    build:.  \# Uses our custom Dockerfile  
    image: litellm-mcp-gateway:v1  
    container\_name: litellm\_gateway  
    ports:  
      \- "4000:4000"  
    volumes:  
      \-./config.yaml:/app/config.yaml  
    environment:  
      \# Inject keys from the host's environment or a.env file  
      \- Z\_AI\_API\_KEY=${Z\_AI\_API\_KEY}  
      \- BROWSERBASE\_API\_KEY=${BROWSERBASE\_API\_KEY}  
      \- BROWSERBASE\_PROJECT\_ID=${BROWSERBASE\_PROJECT\_ID}  
      \# Master key is REQUIRED for managing MCP tool permissions  
      \- LITELLM\_MASTER\_KEY=sk-admin-master-key-1234  
    extra\_hosts:  
      \# Critical for Linux users to access Ollama on localhost  
      \- "host.docker.internal:host-gateway"  
    restart: unless-stopped

## ---

**4\. The Intelligence Layer: Hosting Qwen3-VL**

The user specifically requested the use of **Qwen3-VL** in **GGUF** format via **LiteLLM**. This presents specific challenges regarding tool calling reliability and multimodal handling.

### **4.1 The GGUF Hosting Backend: Ollama vs. vLLM**

While LiteLLM is a proxy, it needs a backend inference engine to actually run the GGUF model. **Ollama** is the industry standard for running GGUF files with an OpenAI-compatible API layer.

* **Configuration:** The user must ensure the Qwen3-VL model is pulled or imported into Ollama.  
  * Command: ollama run qwen2.5-vl (Note: At the time of writing, Qwen3-VL GGUF support is often aliased or experimental; Qwen2.5-VL is the stable target, but the mechanics are identical).  
  * **Context Window:** VLMs require massive context for high-resolution images. Ensure Ollama is launched with a sufficient context window (e.g., OLLAMA\_NUM\_CTX=32768) to handle the verbose JSON schemas of the MCP tools.

### **4.2 The "Thinking" vs. "Tool Calling" Conflict**

A major issue with open-weights models like Qwen-VL is that they are often trained to output "Chain of Thought" (CoT) reasoning or "thinking" tags (\<think\>...) before generating the JSON for a tool call. Standard OpenAI-compatible parsers in clients (like Roo Code) often fail if the tool call is not a strict JSON object or if it is wrapped in markdown blocks.  
The LiteLLM Solution (drop\_params):  
Qwen-VL models may confuse the reasoning\_effort parameter (used by OpenAI o1 models) or fail to structure the tool call correctly.

* **Fix:** In litellm\_config.yaml, enabling drop\_params: true in the litellm\_settings block helps strip unsupported parameters that confuse the Ollama backend.12  
* Fix: LiteLLM has built-in logic to parse JSON blocks found inside the content field if the tool\_calls field is empty. This is crucial for Qwen, which often outputs:  
  I will search for the weather.json  
  { "function": "get\_weather",... }  
  LiteLLM detects this pattern and reformats it into a standard \`tool\_calls\` object that Roo Code and Copilot can understand.

### **4.3 Synergy: Vision Tools vs. Vision Models**

A common question in this architecture is the redundancy between **Qwen3-VL (a vision model)** and **Z.ai Vision MCP (a vision tool)**.

* **Qwen3-VL Native:** Used when you upload an image *directly into the chat interface*. The model "sees" the raw pixels. This is fast and local.  
* **Z.ai Vision MCP:** Used when the agent is operating autonomously. If Roo Code navigates to a website using Browserbase, it doesn't have the image file locally to send to Qwen. Instead, it takes a screenshot (via Browserbase), passes that screenshot URL/path to the **Z.ai tool**, and Z.ai returns a text description or structured analysis (e.g., "The button is at coordinates x,y").  
* **Insight:** The centralized gateway allows you to mix these. You can ask Qwen (the model) to orchestrate the Z.ai (the tool) to analyze a complex UI artifact that Qwen's native resolution might miss, or simply to offload the processing to Z.ai's specialized infrastructure.13

## ---

**5\. The Tool Layer: Specifics of Z.ai and Browserbase**

Integrating these specific tools requires understanding their unique configuration flags which are passed via the args array in our config.yaml.

### **5.1 Z.ai Vision MCP**

The Z.ai MCP server provides tools like ui\_to\_artifact, extract\_text\_from\_screenshot, and diagnose\_error\_screenshot.

* **Environment:** Requires Z\_AI\_MODE (ZAI or ZHIPU) and Z\_AI\_API\_KEY.  
* **Installation:** It is an NPM package @z\_ai/mcp-server.  
* **Gateway Handling:** By running this in LiteLLM, we expose these capabilities to Claude Code (terminal). This is significant because Claude Code in the terminal cannot natively render images for the user to see, but it *can* use the Z.ai tool to "read" screenshots of errors and suggest fixes, effectively giving vision capabilities to a text-only terminal interface.11

### **5.2 Browserbase MCP**

Browserbase provides a headless browser API.

* **Flags:** The \--proxies flag enables IP rotation, and \--advancedStealth helps bypass anti-bot measures.14  
* **Session Persistence:** One of the most powerful features of centralizing Browserbase is **Session Persistence**. By configuring a specific contextId in the args (or managing it via the client prompt), multiple clients could theoretically interact with the same browser session context, although managing the state requires careful prompting.  
* **Gateway Handling:** When Roo Code asks to "Go to google.com," the request travels Roo \-\> LiteLLM \-\> Browserbase MCP (inside Docker) \-\> Browserbase Cloud \-\> Google.

## ---

**6\. The Consumption Layer: Unified Client Configuration**

This section addresses the user's core requirement: **"not repeating mcp server config multiple times."** Instead of configuring tools in each client, we configure each client to look at the **LiteLLM Gateway**.

### **6.1 Understanding the Universal Endpoint**

LiteLLM exposes all configured stdio MCP servers via a standardized SSE endpoint.

* **Base URL:** http://localhost:4000 (assuming Docker mapping).  
* **SSE Endpoint:** http://localhost:4000/sse  
* **Capabilities:** When a client connects to /sse, LiteLLM aggregates the tool definitions from *all* active stdio servers defined in config.yaml and presents them as a single list of tools.9

### **6.2 Client 1: Roo Code (VS Code Extension)**

Roo Code (formerly Cline) has native support for connecting to remote MCP servers via SSE.  
**Configuration File:**

* **Location:** \~/.vscode/extensions/rooveterinaryinc.roo-cline.../settings/mcp\_settings.json OR the project-specific .roo/mcp.json.  
* **Configuration:**  
  JSON  
  {  
    "mcpServers": {  
      "unified-gateway": {  
        "type": "sse",  
        "url": "http://localhost:4000/sse",  
        "headers": {  
          "Authorization": "Bearer sk-admin-master-key-1234"  
        },  
        "disabled": false,  
        "alwaysAllow":  
      }  
    }  
  }

* **Insight:** Roo Code will now see *all* tools (Z.ai and Browserbase) coming from this single "unified-gateway" source. You do not need to add Z.ai or Browserbase separately in Roo Code.2

### **6.3 Client 2: Claude Code (CLI)**

Claude Code interacts via the terminal. It traditionally uses mcp add which writes to \~/.claude.json.  
Configuration Command:  
Run this command once in your terminal.

Bash

claude mcp add unified-gateway \--transport sse \--url http://localhost:4000/sse \--header "Authorization: Bearer sk-admin-master-key-1234"

* **Result:** This writes to the global user config. Any Claude Code session started in any directory will now have access to the Z.ai vision tools and Browserbase automation.  
* **Compatibility Note:** Claude Code previously deprecated SSE in favor of HTTP, but recent updates have restored/maintained support for generic SSE endpoints provided by bridges like LiteLLM.15

### **6.4 Client 3: GitHub Copilot (VS Code)**

GitHub Copilot (specifically Copilot Chat in VS Code) recently added MCP support.  
**Configuration File:**

* **Location:** User Settings JSON or .vscode/mcp.json.  
* **Configuration:**  
  JSON  
  {  
    "servers": {  
      "unified-gateway": {  
        "type": "sse",  
        "url": "http://localhost:4000/sse",  
        "headers": {  
          "Authorization": "Bearer sk-admin-master-key-1234"  
        }  
      }  
    }  
  }

* **Constraint:** This feature often requires the "MCP servers in Copilot" policy to be enabled if you are using an Enterprise license. For individual users, it is generally available in VS Code Insiders or the latest stable builds.3

## ---

**7\. Operational Excellence: Security and Observability**

Centralizing control introduces a single point of failure and a single point of attack. Operational rigor is required.

### **7.1 Security: The Principle of Least Privilege**

In the configuration above, we used a master\_key. However, LiteLLM supports **Virtual Keys**.

* **Strategy:** Generate distinct keys for each client.  
  * Key 1 (Roo Code): sk-roo-code-key \-\> Allowed to access vision\_team and web\_team access groups.  
  * Key 2 (Copilot): sk-copilot-key \-\> Allowed to access only vision\_team (perhaps you don't want Copilot browsing the web).  
* **Implementation:** In LiteLLM, you can map keys to specific access\_groups defined in the config.yaml.17 This prevents a compromised CLI token from granting access to expensive Browserbase quotas.

### **7.2 Observability and Debugging**

A major benefit of this architecture is unified logging.

* **Unified Logs:** LiteLLM logs every tool call. You can see exactly how often Roo Code calls Z.ai vs. how often Copilot calls it.  
* **Debugging Tool Calls:** If Qwen3-VL is failing to execute a tool, check the LiteLLM logs (docker logs litellm\_gateway). Look for the tool\_calls payload. If Qwen is outputting malformed JSON, LiteLLM's logs will show the raw text, allowing you to refine the system prompt in the config.yaml to include strict JSON examples.

### **7.3 Latency Considerations**

* **Stdio Latency:** Negligible.  
* **Network Latency:** Localhost HTTP is fast (\<1ms).  
* **Model Latency:** This is the bottleneck. Qwen3-VL on Ollama must be GPU-accelerated. If the model takes 10 seconds to generate the tool call JSON, the client (Roo Code) might timeout.  
* **Timeout Config:** Increase the timeout settings in Roo Code's mcp\_settings.json if you experience dropped connections during complex vision tasks.2

## ---

**8\. Conclusion and Future Outlook**

This report demonstrates that the "Best Way" to manage a hybrid MCP environment is to invert the traditional topology. Rather than configuring tools at the *edge* (clients), we configure them at the *core* (LiteLLM Gateway).  
**Summary of Benefits:**

1. **Zero Redundancy:** Add a tool once in LiteLLM config.yaml, and it instantly becomes available to Roo, Claude, and Copilot via the shared SSE endpoint.  
2. **Hybrid Intelligence:** Seamlessly mixes self-hosted Qwen3-VL reasoning with proprietary Z.ai/Browserbase capabilities.  
3. **Unified Security:** Centralized API key management and granular access control via Virtual Keys.  
4. **Cross-Client Context:** While not fully shared memory yet, this architecture lays the groundwork for shared session persistence (e.g., passing a Browserbase contextId between agents).

By adopting this Centralized Gateway Architecture, the developer transitions from a chaotic mesh of brittle connections to a managed, enterprise-grade AI infrastructure capable of scaling with the rapidly evolving MCP ecosystem.

### **Appendix: Quick Reference Configuration Table**

| Component | Configuration Location | Connection Type | Target / Command |
| :---- | :---- | :---- | :---- |
| **Z.ai Vision** | LiteLLM config.yaml | stdio (managed) | npx @z\_ai/mcp-server |
| **Browserbase** | LiteLLM config.yaml | stdio (managed) | npx @browserbasehq/... |
| **Qwen3-VL** | LiteLLM config.yaml | HTTP Proxy | http://host.docker.internal:11434 |
| **Roo Code** | .roo/mcp.json | SSE | http://localhost:4000/sse |
| **Claude Code** | \~/.claude.json | SSE | http://localhost:4000/sse |
| **GitHub Copilot** | .vscode/mcp.json | SSE | http://localhost:4000/sse |

#### **Works cited**

1. Claude Code settings \- Claude Code Docs, accessed December 24, 2025, [https://code.claude.com/docs/en/settings](https://code.claude.com/docs/en/settings)  
2. Using MCP in Roo Code | Roo Code Documentation \- Roo Code Docs, accessed December 24, 2025, [https://docs.roocode.com/features/mcp/using-mcp-in-roo](https://docs.roocode.com/features/mcp/using-mcp-in-roo)  
3. Use MCP servers in VS Code \- Visual Studio Code, accessed December 24, 2025, [https://code.visualstudio.com/docs/copilot/customization/mcp-servers](https://code.visualstudio.com/docs/copilot/customization/mcp-servers)  
4. Configuring MCP Tools in Claude Code \- The Better Way \- Scott Spence, accessed December 24, 2025, [https://scottspence.com/posts/configuring-mcp-tools-in-claude-code](https://scottspence.com/posts/configuring-mcp-tools-in-claude-code)  
5. Step-by-Step Guide: Build Multi-Server MCP System with StdioClientTransport \- Medium, accessed December 24, 2025, [https://medium.com/@shivamchamoli1997/step-by-step-guide-build-multi-server-mcp-system-with-stdioclienttransport-9ecb7f555bb7](https://medium.com/@shivamchamoli1997/step-by-step-guide-build-multi-server-mcp-system-with-stdioclienttransport-9ecb7f555bb7)  
6. Example Clients \- Model Context Protocol, accessed December 24, 2025, [https://modelcontextprotocol.io/clients](https://modelcontextprotocol.io/clients)  
7. mcp-proxy | MCP Tool | CodeboltAI, accessed December 24, 2025, [https://www.codebolt.ai/registry/mcp-tools/48/](https://www.codebolt.ai/registry/mcp-tools/48/)  
8. MCP Server MCP-Proxy: A Deep Dive for AI Engineers \- Skywork.ai, accessed December 24, 2025, [https://skywork.ai/skypage/en/MCP-Server-MCP-Proxy:-A-Deep-Dive-for-AI-Engineers/1972560741652951040](https://skywork.ai/skypage/en/MCP-Server-MCP-Proxy:-A-Deep-Dive-for-AI-Engineers/1972560741652951040)  
9. MCP Overview \- LiteLLM, accessed December 24, 2025, [https://docs.litellm.ai/docs/mcp](https://docs.litellm.ai/docs/mcp)  
10. Connect MCP Servers to Claude Desktop with Docker MCP Toolkit, accessed December 24, 2025, [https://www.docker.com/blog/connect-mcp-servers-to-claude-desktop-with-mcp-toolkit/](https://www.docker.com/blog/connect-mcp-servers-to-claude-desktop-with-mcp-toolkit/)  
11. Vision MCP Server \- Overview \- Z.AI DEVELOPER DOCUMENT, accessed December 24, 2025, [https://docs.z.ai/devpack/mcp/vision-mcp-server](https://docs.z.ai/devpack/mcp/vision-mcp-server)  
12. Compatibility Issues with Qwen Series Models (VL, QVQ-max) via LiteLLM Proxy \#161, accessed December 24, 2025, [https://github.com/bytebot-ai/bytebot/issues/161](https://github.com/bytebot-ai/bytebot/issues/161)  
13. @z\_ai/mcp-server \- npm, accessed December 24, 2025, [https://www.npmjs.com/package/@z\_ai/mcp-server](https://www.npmjs.com/package/@z_ai/mcp-server)  
14. Browserbase MCP Server Configuration, accessed December 24, 2025, [https://docs.browserbase.com/integrations/mcp/configuration](https://docs.browserbase.com/integrations/mcp/configuration)  
15. CLI Tool \- Claude Code Subagents & Commands Collection, accessed December 24, 2025, [https://www.buildwithclaude.com/docs/cli](https://www.buildwithclaude.com/docs/cli)  
16. Connect Claude Code to tools via MCP, accessed December 24, 2025, [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp)  
17. MCP Permission Management \- LiteLLM, accessed December 24, 2025, [https://docs.litellm.ai/docs/mcp\_control](https://docs.litellm.ai/docs/mcp_control)