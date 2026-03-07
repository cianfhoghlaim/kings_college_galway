---
title: "AG-UI and A2UI: Understanding the Differences | CopilotKit"
source: "https://www.copilotkit.ai/ag-ui-and-a2ui"
author:
published:
created: 2025-12-15
description: "Learn about AG-UI, the User Interaction protocol, and A2UI, a generative UI specification. Discover how they work together to enable powerful agent-user interactions."
tags:
  - "clippings"
---
CopilotKit v1.50 is here, with brand new interfaces, streamlined internals, and no breaking changes!

[Check it Out](https://docs.copilotkit.ai/whats-new/v1-50)

AG-UI and A2UI

## AG-UI and A2UI

Despite the naming similarities, AG-UI and A2UI serve very different and complementary roles in the agentic application stack.

- • **AG-UI** (Agent–User Interaction) is the general-purpose, bi-directional connection between a user-facing application and any agentic backend.
- • **A2UI** is the soon to be announced Generative UI spec, originated by Google, which agents can use to return UI widgets.
  

CopilotKit and AG-UI are working on full A2UI compatability, which will be available concurrently with the official A2UI release.

## Generative UI Specifications

![AG-UI and A2UI diagram](https://www.copilotkit.ai/_next/image?url=%2Fimages%2Fag-ui%2Fa2ui-genui-spec.png&w=1920&q=90)

Several recently released specs have enabled agents to return generative UI, increasing the power and flexibility of the Agent↔User conversation.

**A2UI**, **MCP-UI**, and **Open-JSON-UI** are all **generative UI specifications.** Generative UIs allow agents to respond to users not only with text but also with dynamic UI components.

| Specification | Origin / Maintainer | Purpose |
| --- | --- | --- |
| [A2UI](https://a2ui.org/) | Google | A declarative, LLM-friendly Generative UI spec. JSONL-based and streaming, designed for platform-agnostic rendering. |
| [Open-JSON-UI](https://json-schema.org/) | OpenAI | An open standardization of OpenAI's internal declarative Generative UI schema. |
| [MCP-UI](https://mcpui.dev/) | Microsoft + Shopify | A fully open, iframe-based Generative UI standard extending MCP for user-facing experiences. |

## AG-UI: The Agent-User Interaction Protocol

**AG-UI is not a generative UI specification** — it's an **Agent-User Interaction protocol** that provides the **bi-directional runtime connection** between the agentic and the agentic frontend.

AG-UI natively supports all of the above generative UI specs (including A2UI) and allows developers to define **their own custom generative UI standards** as well.

### AG-UI Protocol

- • Provides the **runtime connection** between agent and app
- • Handles **bi-directional communication** and state synchronization
- • Focuses on **how** agents and applications communicate

![AG-UI and A2UI diagram](https://www.copilotkit.ai/_next/image?url=%2Fimages%2Fag-ui%2Fag-ui-full.png&w=3840&q=90)

## How They Work Together

AG-UI and generative UI specifications are **complementary technologies** that work together seamlessly:

1. **Agents generate UI** using a generative UI spec like A2UI, describing the components they want to create.
2. **AG-UI transports** that specification from the agent to the application over the bi-directional runtime connection.
3. **The application renders** the UI components according to the spec, creating the dynamic interface.
4. **User interactions flow back** through AG-UI to the agent, enabling real-time collaboration.

This combination gives developers the flexibility to choose their preferred generative UI format while providing a robust, standardized way to connect agents with user-facing applications.

![](https://static.scarf.sh/a.png?x-pxid=1c040678-b704-471e-a3f5-69c6bf52b703)