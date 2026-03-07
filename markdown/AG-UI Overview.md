---
title: "AG-UI Overview"
source: "https://docs.ag-ui.com/introduction"
author:
  - "[[Agent User Interaction Protocol]]"
published:
created: 2025-12-29
description:
tags:
  - "clippings"
---
AG-UI is an **open**, **lightweight**, **event-based** protocol that standardizes how AI agents connect to user-facing applications.AG-UI is designed to be the general-purpose, bi-directional connection between a user-facing application and any agentic backend.Built for simplicity and flexibility, it standardizes how agent state, UI intents, and user interactions flow between your model/agent runtime and user-facing frontend applications—to allow application developers to ship reliable, debuggable, user‑friendly agentic features fast while focusing on application needs and avoiding complex ad-hoc wiring.

![AG-UI Overview](https://mintcdn.com/tawkitai/p1KIIAcVU0goF39y/images/ag-ui-overview-with-partners.png?w=280&fit=max&auto=format&n=p1KIIAcVU0goF39y&q=85&s=2375a441a8cfddd98d19e04c1e7f3471)

AG-UI Overview

---

## Agentic Protocols

**Confused about “A2UI” and “AG-UI”?** That’s understandable! Despite the naming similarities, they are quite different and work well together. A2UI is a [generative UI specification](https://docs.ag-ui.com/concepts/generative-ui-specs) - allowing agents to deliver UI widgets, where AG-UI is the Agent↔User Interaction protocol - which connects an agentic frontend to any agentic backend. [Learn more](https://copilotkit.ai/ag-ui-and-a2ui)

AG-UI is one of three prominent open [agentic protocols](https://docs.ag-ui.com/agentic-protocols).

---

## Building blocks (today & upcoming)

---

## Why Agentic Apps need AG-UI

Agentic applications break the simple request/response model that dominated frontend-backend development in the pre-agentic era: a client makes a request, the server returns data, the client renders it, and the interaction ends.

#### The requirements of user‑facing agents

While agents are just software, they exhibit characteristics that make them challenging to serve behind traditional REST/GraphQL APIs:
- Agents are **long‑running** and **stream** intermediate work—often across multi‑turn sessions.
- Agents are **nondeterministic** and can **control application UI nondeterministically**.
- Agents simultanously mix **structured + unstructured IO** (e.g. text & voice, alongside tool calls and state updates).
- Agents need user-interactive **composition**: e.g. they may call sub‑agents, often recursively.
- And more…
AG-UI is an event-based protocol that enables dynamic communication between agentic frontends and backends. It builds on top of the foundational protocols of the web (HTTP, WebSockets) as an abstraction layer designed for the agentic age—bridging the gap between traditional client-server architectures and the dynamic, stateful nature of AI agents.

---

## AG-UI in Action

 <video width="100%" height="auto" controls="controls"><source src="https://mintcdn.com/tawkitai/3646SYYoMG8sZMDf/videos/Dojo-overview.mp4?fit=max&amp;auto=format&amp;n=3646SYYoMG8sZMDf&amp;q=85&amp;s=0b26be27024fa21fcadace432f310d64" type="video/mp4"> <span>Your browser does not support the video tag.</span></video>

You can see demo apps of the AG-UI features with the framework of your choice, with preview, code, and walkthrough docs in the [AG-UI Dojo](https://dojo.ag-ui.com/)

---

## Supported Integrations

AG-UI was born from CopilotKit’s initial **partnership** with LangGraph and CrewAI - and brings the incredibly popular agent-user-interactivity infrastructure to the wider agentic ecosystem.**1st party** = the platforms that have AG‑UI built in and provide documentation for guidance.

### Direct to LLM

### Agent Framework - 1st Party

### Agent Framework - Community

### Specification (standard)

### SDKs

### Clients

---

## Quick Start

Choose the path that fits your needs:## [Build agentic applications](https://docs.ag-ui.com/quickstart/applications)

[

Build agentic applications powered by AG-UI compatible agents.

](https://docs.ag-ui.com/quickstart/applications)Build new AG-UI integrations

Build integrations for new agent frameworks, custom in-house solutions, or use AG-UI without any agent framework.

[View original](https://docs.ag-ui.com/quickstart/introduction)Build AG-UI compatible clients

Build new clients for AG-UI-compatible agents (web, mobile, slack, messaging, etc.)

[View original](https://docs.ag-ui.com/quickstart/clients)

## Explore AG-UI

Dive deeper into AG-UI’s core concepts and capabilities:## [Core architecture](https://docs.ag-ui.com/concepts/architecture)

[

Understand how AG-UI connects agents, protocols, and front-ends

](https://docs.ag-ui.com/concepts/architecture)Events

Learn about AG-UI’s event-driven protocol

[View original](https://docs.ag-ui.com/concepts/events)

## Resources

Explore guides, tools, and integrations to help you build, optimize, and extend your AG-UI implementation. These resources cover everything from practical development workflows to debugging techniques.## [Developing with Cursor](https://docs.ag-ui.com/tutorials/cursor)

[

Use Cursor to build AG-UI implementations faster

](https://docs.ag-ui.com/tutorials/cursor)Troubleshooting AG-UI

Fix common issues when working with AG-UI servers and clients

[View original](https://docs.ag-ui.com/tutorials/debugging)

## Contributing

Want to contribute? Check out our [Contributing Guide](https://docs.ag-ui.com/development/contributing) to learn how you can help improve AG-UI.Here’s how to get help or provide feedback:
- For bug reports and feature requests related to the AG-UI specification, SDKs, or documentation (open source), please [create a GitHub issue](https://github.com/ag-ui-protocol/ag-ui/issues)
- For discussions or Q&A about AG-UI, please join the [Discord community](https://discord.gg/Jd3FzfdJa8)

[MCP, A2A, and AG-UI](https://docs.ag-ui.com/agentic-protocols)