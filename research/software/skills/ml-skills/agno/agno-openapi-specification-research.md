# Agno OpenAPI Specification Research

**Research Date:** 2025-11-22
**Status:** CONFIRMED - Official OpenAPI Specification Found

---

## Executive Summary

**YES - An official OpenAPI specification exists for Agno AgentOS.**

### Primary Location
**URL:** `https://raw.githubusercontent.com/agno-agi/agno-docs/main/reference-api/openapi.json`

**Version:** 1.0.0
**Title:** AI Agent Operating System API
**Format:** JSON (YAML version also available)

---

## Official OpenAPI Specification Details

### Location & Access

1. **GitHub Repository:** [agno-agi/agno-docs](https://github.com/agno-agi/agno-docs)
   - Path: `reference-api/openapi.json`
   - Alternative format: `reference-api/openapi.yaml`
   - Raw URL: `https://raw.githubusercontent.com/agno-agi/agno-docs/main/reference-api/openapi.json`

2. **Local Development:**
   - When running Agno locally, the OpenAPI spec is available at: `http://localhost:7777/openapi.json`
   - This is generated from the AgentOS cookbook which exposes all supported interfaces

3. **Documentation Site:**
   - Official docs: `https://docs.agno.com/`
   - API Reference: Auto-generated from the OpenAPI spec using Mintlify
   - Swagger UI: Available at `http://localhost:8000/docs` when running agent-api

---

## API Capabilities Overview

The Agno AgentOS API provides comprehensive capabilities for building, managing, and executing AI agent systems:

### Core Features

1. **Agent Management**
   - Create and execute individual agents with multimodal support (text, images, audio, video, documents)
   - Cancel or resume agent execution mid-run
   - List and retrieve agent details

2. **Team Collaboration**
   - Coordinate teams of agents working together
   - Task transfer between team members
   - Shared knowledge and memory systems across teams

3. **Workflow Orchestration**
   - Execute complex multi-step workflows
   - Sequential or parallel execution support
   - Input validation and error handling

4. **Session & History Management**
   - Manage conversation histories and execution contexts
   - Rename, update state, and filter by user/component
   - Track runs across multiple sessions

5. **Memory Systems**
   - Store and retrieve user memories
   - Topic tagging and fuzzy search
   - Per-user memory statistics

6. **Evaluation Framework**
   - Run reliability, accuracy, and performance tests
   - Track metrics over time for agents and teams

7. **Metrics & Analytics**
   - Aggregate usage data (token consumption, active users)
   - Model performance tracking
   - Date-range filtering for metrics

8. **Knowledge Base**
   - Upload documents (PDF, CSV, DOCX, etc.)
   - Asynchronous processing
   - Custom chunking strategies

9. **Integration Points**
   - Slack event handling
   - WhatsApp webhooks
   - Agent-to-Agent (A2A) messaging protocols

### Authentication
All endpoints require HTTP Bearer authentication.

---

## Complete API Endpoint Reference

### Home & Health
- `GET /` - API Information
- `GET /health` - Health Check

### Core Configuration
- `GET /config` - Get OS Configuration
- `GET /models` - Get Available Models

### Agents
- `GET /agents` - List All Agents
- `GET /agents/{agent_id}` - Get Agent Details
- `POST /agents/{agent_id}/runs` - Create Agent Run
- `POST /agents/{agent_id}/runs/{run_id}/cancel` - Cancel Agent Run
- `POST /agents/{agent_id}/runs/{run_id}/continue` - Continue Agent Run

### Teams
- `GET /teams` - List All Teams
- `GET /teams/{team_id}` - Get Team Details
- `POST /teams/{team_id}/runs` - Create Team Run
- `POST /teams/{team_id}/runs/{run_id}/cancel` - Cancel Team Run

### Workflows
- `GET /workflows` - List All Workflows
- `GET /workflows/{workflow_id}` - Get Workflow Details
- `POST /workflows/{workflow_id}/runs` - Execute Workflow
- `POST /workflows/{workflow_id}/runs/{run_id}/cancel` - Cancel Workflow Run

### Sessions
- `GET /sessions` - List Sessions
- `POST /sessions` - Create New Session
- `DELETE /sessions` - Delete Multiple Sessions
- `GET /sessions/{session_id}` - Get Session by ID
- `DELETE /sessions/{session_id}` - Delete Session
- `PATCH /sessions/{session_id}` - Update Session
- `POST /sessions/{session_id}/rename` - Rename Session
- `GET /sessions/{session_id}/runs` - Get Session Runs
- `GET /sessions/{session_id}/runs/{run_id}` - Get Run by ID

### Memory
- `GET /memories` - List Memories
- `POST /memories` - Create Memory
- `DELETE /memories` - Delete Multiple Memories
- `GET /memories/{memory_id}` - Get Memory by ID
- `DELETE /memories/{memory_id}` - Delete Memory
- `PATCH /memories/{memory_id}` - Update Memory
- `GET /memory_topics` - Get Memory Topics
- `GET /user_memory_stats` - Get User Memory Statistics

### Evaluations
- `GET /eval-runs` - List Evaluation Runs
- `POST /eval-runs` - Execute Evaluation
- `DELETE /eval-runs` - Delete Evaluation Runs
- `GET /eval-runs/{eval_run_id}` - Get Evaluation Run
- `PATCH /eval-runs/{eval_run_id}` - Update Evaluation Run

### Metrics
- `GET /metrics` - Get AgentOS Metrics
- `POST /metrics/refresh` - Refresh Metrics

### Knowledge
- `GET /knowledge/content` - List Content
- `POST /knowledge/content` - Upload Content

### Integration Channels
- `POST /slack/events` - Slack Events
- `GET /whatsapp/status` - WhatsApp Status
- `GET /whatsapp/webhook` - Verify WhatsApp Webhook
- `POST /whatsapp/webhook` - Handle WhatsApp Messages

### Agent-to-Agent Communication
- `POST /a2a/message/send` - Send Message
- `POST /a2a/message/stream` - Stream Message

### AGUI & Status
- `POST /agui` - Run Agent
- `GET /status` - Get Status

---

## Related Projects & Standards

### Open Agent API (Separate Initiative)

There is a related but separate project called "Open Agent API" that aims to standardize agent communication:

**Repository:** [swedishembedded/open-agent-api](https://github.com/swedishembedded/open-agent-api)
**Specification:** `open-agent-api.yaml`
**Version:** 0.7.0

#### Relationship to Agno
- The Open Agent API project references Agno as an architectural inspiration
- Agno's team-based architecture influenced the Open Agent API design
- This is a community-driven standardization effort, not the official Agno API

#### Open Agent API Features
- Authentication (Magic link and OAuth2)
- OpenAI-compatible chat completion interface
- Multi-agent teams
- Toolkit access and management
- Knowledge base search
- Stripe billing integration
- Model Context Protocol (MCP) integration

#### Differences from Agno AgentOS API
- Open Agent API includes integrated billing/subscriptions
- Explicit OAuth support
- Positioned as a cross-platform standard
- Agno AgentOS API is specific to the Agno framework

---

## Documentation Workflow

The Agno documentation team follows this workflow for updating the OpenAPI specification:

1. **Generate OpenAPI spec** from the AgentOS cookbook
   ```bash
   curl -o reference-api/openapi.json http://localhost:7777/openapi.json
   ```

2. **Convert to YAML** (for compatibility)
   ```bash
   swagger-cli bundle openapi.json -o openapi.yaml
   ```

3. **Generate API reference docs** using Mintlify scraping
   ```bash
   npx @mintlify/scraping@latest openapi-file reference-api/openapi.json
   ```

4. **Update documentation site** at docs.agno.com

---

## Community Discussions & Feature Requests

### OpenAPI Integration with Agno Tools

There are active community discussions about enhancing OpenAPI integration:

1. **Auto-generating Tools from OpenAPI Specs**
   - Issue: [Using any REST API as Agno tools #2994](https://github.com/agno-agi/agno/issues/2994)
   - Request: Create Agno tools directly from existing REST APIs and their OpenAPI specifications
   - Current state: Agno provides helpers to manually define tools, but no built-in mechanism for auto-generation from OpenAPI specs

2. **Open Agent API Schema Standardization**
   - Issue: [Let's agree on Open Agent API Schema! #2790](https://github.com/agno-agi/agno/issues/2790)
   - Discussion: Community interest in standardizing agent APIs across frameworks
   - Related to the open-agent-api.yaml project

---

## Feasibility Assessment

### For API Clients
**Status:** EXCELLENT

The official OpenAPI specification makes it trivial to:
- Generate type-safe clients in any language (TypeScript, Python, Go, Java, Rust, etc.)
- Use with OpenAPI-compatible tools (Postman, Insomnia, Bruno)
- Auto-generate SDK documentation
- Validate requests and responses

### For Integration
**Status:** PRODUCTION-READY

The Agno AgentOS API provides:
- Comprehensive endpoint coverage for all agent operations
- Bearer token authentication
- FastAPI-based runtime with SSE-compatible endpoints
- Horizontal scalability (stateless design)
- Local development support with Swagger UI

### For Custom Extensions
**Status:** EXTENSIBLE

The OpenAPI spec enables:
- Understanding the full API surface area
- Creating wrapper libraries and higher-level abstractions
- Building custom integrations with external systems
- Generating mock servers for testing

---

## Key Resources & Links

### Official Agno Resources
- **Main Repository:** [github.com/agno-agi/agno](https://github.com/agno-agi/agno)
- **Documentation:** [docs.agno.com](https://docs.agno.com/)
- **Docs Repository:** [github.com/agno-agi/agno-docs](https://github.com/agno-agi/agno-docs)
- **Agent API:** [github.com/agno-agi/agent-api](https://github.com/agno-agi/agent-api)
- **Community Forum:** [community.agno.com](https://community.agno.com/)

### OpenAPI Specification
- **Direct URL:** `https://raw.githubusercontent.com/agno-agi/agno-docs/main/reference-api/openapi.json`
- **YAML Version:** `https://raw.githubusercontent.com/agno-agi/agno-docs/main/reference-api/openapi.yaml`

### Related Standards
- **Open Agent API:** [github.com/swedishembedded/open-agent-api](https://github.com/swedishembedded/open-agent-api)

### Integration Guides
- **Agno + Groq:** [console.groq.com/docs/agno](https://console.groq.com/docs/agno)
- **Agno on AI/ML API:** [docs.aimlapi.com/integrations/agno](https://docs.aimlapi.com/integrations/agno)
- **APIpie.ai Integration:** [apipie.ai/docs/Integrations/Agent-Frameworks/Agno](https://apipie.ai/docs/Integrations/Agent-Frameworks/Agno)

---

## Recommendations

### For Development Teams

1. **Use the Official OpenAPI Spec**
   - Download from: `https://raw.githubusercontent.com/agno-agi/agno-docs/main/reference-api/openapi.json`
   - Keep updated by checking the agno-docs repository
   - Generate type-safe clients for your preferred language

2. **Local Development Setup**
   - Use the agent-api repository for a minimal setup
   - Access Swagger UI at `http://localhost:8000/docs`
   - Test endpoints interactively before integration

3. **Monitor Community Discussions**
   - Follow GitHub issues for OpenAPI-related enhancements
   - Contribute to standardization efforts if interested
   - Watch for auto-generation features from OpenAPI specs

### For API Client Generation

**Recommended Tools:**
- **TypeScript/JavaScript:** openapi-generator, openapi-typescript
- **Python:** openapi-generator (python-pydantic), httpx + pydantic
- **Go:** oapi-codegen, openapi-generator
- **Rust:** openapi-generator, progenitor

### For Production Deployment

The AgentOS provides:
- Ready-to-use FastAPI app
- SSE-compatible endpoints
- Stateless, horizontally scalable architecture
- Minimal memory footprint (async by default)

---

## Conclusion

**Agno has a comprehensive, production-ready OpenAPI specification** that covers all aspects of the AgentOS API. The specification is:

- ✅ Officially maintained
- ✅ Version controlled in GitHub
- ✅ Auto-generated from the framework
- ✅ Well-documented
- ✅ Used for official documentation generation
- ✅ Suitable for client generation in any language
- ✅ Production-ready and battle-tested

The availability of this OpenAPI spec makes Agno highly accessible for developers who want to build integrations, generate type-safe clients, or understand the full API surface area.

Additionally, the community-driven Open Agent API standardization effort shows strong interest in making agent APIs interoperable across different frameworks, with Agno serving as an architectural reference.
