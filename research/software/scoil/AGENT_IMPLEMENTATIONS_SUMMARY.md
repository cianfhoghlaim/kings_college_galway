# Agent-Related Implementations Analysis
## Web/Agents Exploration Summary

### Overview
This exploration covers two major agent frameworks found in `/Users/cliste/dev/bonneagar/hackathon/examples/web/agents`:
1. **CopilotKit** - Comprehensive AI copilot framework with React/Next.js integration
2. **Agent OS (agent_os)** - Modern chat interface for AgentOS instances with Agno platform integration

---

## 1. CopilotKit Framework

### 1.1 Architecture Overview

CopilotKit is a monorepo-based framework (using Turbo and pnpm) with the following core packages:

```
CopilotKit/CopilotKit/packages/
├── react-core/          # Core React hooks and context
├── react-textarea/      # Textarea component integration
├── react-ui/            # Pre-built UI components
├── runtime/             # Backend runtime implementation
├── runtime-client-gql/  # GraphQL client for runtime
├── sdk-js/              # JavaScript SDK
└── shared/              # Shared types and utilities
```

### 1.2 Context and State Management

#### CopilotContext (react-core/src/context/copilot-context.tsx)

The central context manages:

**Global State:**
- `actions`: Frontend actions registry
- `coAgentStateRenders`: Co-agent state rendering configurations
- `chatComponentsCache`: Cached components for actions
- `coagentStates`: Agent state tracking
- `threadId/runId`: Session management
- `agentSession`: Current agent session info
- `runtimeClient`: GraphQL client for backend communication
- `authStates_c`: Authentication states per action
- `extensions`: MCP (Model Context Protocol) server extensions
- `langGraphInterruptAction`: LangGraph interrupts for user input

**Key Interfaces:**

```typescript
export interface CopilotContextParams {
  // Function-calling
  actions: Record<string, FrontendAction<any>>;
  setAction: (id: string, action: FrontendAction<any>) => void;
  removeAction: (id: string) => void;

  // CoAgent management
  coAgentStateRenders: Record<string, CoAgentStateRender<any>>;
  setCoAgentStateRender: (id: string, stateRender: CoAgentStateRender<any>) => void;
  
  // Context management
  addContext: (context: string, parentId?: string, categories?: string[]) => TreeNodeId;
  removeContext: (id: TreeNodeId) => void;
  getContextString: (documents: DocumentPointer[], categories: string[]) => string;
  
  // Agent state
  coagentStates: Record<string, CoagentState>;
  setCoagentStates: React.Dispatch<React.SetStateAction<Record<string, CoagentState>>>;
  
  // Runtime and API
  runtimeClient: CopilotRuntimeClient;
  copilotApiConfig: CopilotApiConfig;
  
  // Extensions/MCP
  extensions: ExtensionsInput;
  setExtensions: React.Dispatch<React.SetStateAction<ExtensionsInput>>;
}
```

### 1.3 Action System (Frontend Actions)

**FrontendAction Type Definition:**

```typescript
export type FrontendAction<T extends Parameter[] | [] = []> = Action<T> & {
  name: Exclude<N, "*">;
  available?: FrontendActionAvailability; // "disabled" | "enabled" | "remote" | "frontend"
  pairedAction?: string;
  followUp?: boolean;
} & (
  | {
      // Standard render
      render?: string | ((props: ActionRenderProps<T>) => string | React.ReactElement);
    }
  | {
      // Interactive with response handling
      renderAndWaitForResponse?: (props: ActionRenderPropsWait<T>) => React.ReactElement;
    }
);
```

**Action Render States:**

```typescript
type ActionRenderProps<T> = 
  | { status: "inProgress"; args: Partial<MappedParameterTypes<T>>; result: undefined }
  | { status: "executing"; args: MappedParameterTypes<T>; result: undefined }
  | { status: "complete"; args: MappedParameterTypes<T>; result: any };
```

**Action Availability:**
- `disabled`: Action unavailable to agent
- `enabled`: Standard agent-callable action
- `remote`: Only callable from backend, rendered on frontend
- `frontend`: Client-side only, never sent to backend

### 1.4 CoAgent System (Shared State Agentic Copilots)

**CoagentState:**

```typescript
export interface CoagentState {
  name: string;
  state: any;
  running: boolean;
  active: boolean;
  threadId?: string;
  config?: {
    configurable?: Record<string, any>;
  };
  nodeName?: string; // Current LangGraph node
  runId?: string;
}
```

**CoAgentStateRender:**

```typescript
export interface CoAgentStateRender<T = any> {
  name: string;
  nodeName?: string;
  handler?: (props: CoAgentStateRenderHandlerArguments<T>) => void | Promise<void>;
  render?: ((props: CoAgentStateRenderProps<T>) => React.ReactElement | undefined | null) | string;
}

export type CoAgentStateRenderProps<T> = {
  state: T;
  nodeName: string;
  status: "inProgress" | "complete";
};
```

### 1.5 Core Hooks

#### useCopilotAction

Primary hook for registering frontend actions:

```typescript
// Simple action
useCopilotAction({
  name: "myAction",
  parameters: [{ name: "arg1", type: "string" }],
  handler: ({ arg1 }) => { /* handle */ },
});

// Interactive action with UI
useCopilotAction({
  name: "handleMeeting",
  parameters: [
    { name: "meeting", type: "string", required: true },
    { name: "date", type: "string", required: true },
  ],
  renderAndWaitForResponse: ({ args, respond, status }) => (
    <MeetingConfirmationDialog
      meeting={args.meeting}
      date={args.date}
      onConfirm={() => respond('confirmed')}
      onCancel={() => respond('canceled')}
    />
  ),
});

// Catch-all action
useCopilotAction({
  name: "*",
  render: ({ name, args, status, result, respond }) => {
    return <div>Rendering action: {name}</div>;
  },
});
```

#### useCoAgent

Manages bidirectional agent state synchronization:

```typescript
type AgentState = { count: number };

const {
  name,      // Agent name
  nodeName,  // Current LangGraph node
  state,     // Current state
  setState,  // Update state
  running,   // Is running?
  start,     // Start agent
  stop,      // Stop agent
  run,       // Re-run with hint
} = useCoAgent<AgentState>({
  name: "my-agent",
  initialState: { count: 0 },
});
```

#### useCopilotChat

Headless chat hook for programmatic control:

```typescript
const {
  visibleMessages,     // Current messages
  appendMessage,       // Add message
  reloadMessages,      // Regenerate response
  stopGeneration,      // Stop generation
  reset,              // Clear chat
  isLoading,          // Generation state
  runChatCompletion,  // Manual completion
  mcpServers,         // MCP configurations
  setMcpServers,      // Update MCP servers
} = useCopilotChat();
```

### 1.6 Runtime Architecture

**Service Adapters (Multi-LLM Support):**

The runtime supports multiple LLM providers:
- OpenAI (with Assistant API support)
- Anthropic
- Google GenAI
- Groq
- Bedrock (AWS)
- Unify
- Ollama (experimental)
- LangChain/LangServe
- Custom adapters

**LangGraph Integration:**

```typescript
// agents/langgraph/
├── events.ts          # Event types and handling
├── event-source.ts    # EventSource streaming
```

Supports LangGraph state management and node-based workflows.

### 1.7 API Configuration

```typescript
export interface CopilotApiConfig {
  publicApiKey?: string;
  cloud?: CopilotCloudConfig;
  chatApiEndpoint: string;
  transcribeAudioUrl?: string;
  textToSpeechUrl?: string;
  headers: Record<string, string>;
  properties?: Record<string, any>;
  credentials?: RequestCredentials;
  mcpServers?: Array<{ endpoint: string; apiKey?: string }>;
}
```

### 1.8 Layout Setup Example

```typescript
// app/layout.tsx
export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <CopilotKit
          publicApiKey={process.env.COPILOT_CLOUD_PUBLIC_API_KEY}
          runtimeUrl={
            process.env.COPILOT_CLOUD_PUBLIC_API_KEY
              ? undefined // Copilot Cloud
              : "/api/copilotkit" // Local runtime
          }
          showDevConsole={false}
        >
          {children}
        </CopilotKit>
      </body>
    </html>
  );
}
```

### 1.9 UI Components

**Key Provided Components:**
- `CopilotChat` - Full chat interface
- `CopilotPopup` - Floating popup chat
- Custom message components (UserMessage, AssistantMessage)
- Custom response buttons and generative UI components

**Customization:**

```typescript
<CopilotChat
  UserMessage={CustomUserMessage}
  AssistantMessage={CustomAssistantMessage}
  ResponseButton={CustomResponseButton}
  labels={{
    initial: "Hi! How can I help?"
  }}
/>
```

---

## 2. Agent OS (agent_os)

### 2.1 Architecture Overview

Modern chat UI for interacting with AgentOS instances on the Agno platform.

**Tech Stack:**
- Next.js 15.2.3
- React 18.3.1
- Zustand (state management)
- Tailwind CSS
- shadcn/ui components
- Framer Motion (animations)
- SSE/Streaming for real-time responses

### 2.2 State Management (Zustand Store)

**Store Structure:**

```typescript
interface Store {
  // UI State
  hydrated: boolean;
  setHydrated: () => void;

  // Endpoint Management
  endpoints: { endpoint: string; id__endpoint: string }[];
  setEndpoints: (endpoints: [...]) => void;
  selectedEndpoint: string;
  setSelectedEndpoint: (endpoint: string) => void;
  isEndpointActive: boolean;
  isEndpointLoading: boolean;

  // Chat State
  messages: ChatMessage[];
  setMessages: (messages: [...] | fn) => void;
  isStreaming: boolean;
  setIsStreaming: (state: boolean) => void;
  streamingErrorMessage: string;

  // Agent/Team Management
  agents: AgentDetails[];
  setAgents: (agents: AgentDetails[]) => void;
  teams: TeamDetails[];
  setTeams: (teams: TeamDetails[]) => void;
  mode: 'agent' | 'team';
  setMode: (mode: 'agent' | 'team') => void;

  // Authentication
  authToken: string;
  setAuthToken: (token: string) => void;

  // Model Selection
  selectedModel: string;
  setSelectedModel: (model: string) => void;

  // Sessions
  sessionsData: SessionEntry[] | null;
  setSessionsData: (sessions: [...] | fn) => void;
  isSessionsLoading: boolean;

  // DOM References
  chatInputRef: React.RefObject<HTMLTextAreaElement | null>;
}

// Persisted to localStorage (selected endpoint)
export const useStore = create<Store>()(
  persist((set) => ({...}), {
    name: 'endpoint-storage',
    storage: createJSONStorage(() => localStorage),
    partialize: (state) => ({ selectedEndpoint: state.selectedEndpoint }),
  })
);
```

### 2.3 Type Definitions

**Core Types:**

```typescript
export interface Agent {
  agent_id: string;
  name: string;
  description: string;
  model: Model;
  storage?: boolean;
}

export interface Team {
  team_id: string;
  name: string;
  description: string;
  model: Model;
  storage?: boolean;
}

export interface Model {
  name: string;
  model: string;
  provider: string;
}

export interface ChatMessage {
  role: 'user' | 'agent' | 'system' | 'tool';
  content: string;
  streamingError?: boolean;
  created_at: number;
  tool_calls?: ToolCall[];
  extra_data?: {
    reasoning_steps?: ReasoningSteps[];
    reasoning_messages?: ReasoningMessage[];
    references?: ReferenceData[];
  };
  images?: ImageData[];
  videos?: VideoData[];
  audio?: AudioData[];
  response_audio?: ResponseAudio;
}
```

**Tool Call Tracking:**

```typescript
export interface ToolCall {
  role: 'user' | 'tool' | 'system' | 'assistant';
  content: string | null;
  tool_call_id: string;
  tool_name: string;
  tool_args: Record<string, string>;
  tool_call_error: boolean;
  metrics: { time: number };
  created_at: number;
}
```

**Agent Execution Events:**

```typescript
export enum RunEvent {
  RunStarted = 'RunStarted',
  RunContent = 'RunContent',
  RunCompleted = 'RunCompleted',
  RunError = 'RunError',
  RunOutput = 'RunOutput',
  UpdatingMemory = 'UpdatingMemory',
  ToolCallStarted = 'ToolCallStarted',
  ToolCallCompleted = 'ToolCallCompleted',
  MemoryUpdateStarted = 'MemoryUpdateStarted',
  MemoryUpdateCompleted = 'MemoryUpdateCompleted',
  ReasoningStarted = 'ReasoningStarted',
  ReasoningStep = 'ReasoningStep',
  ReasoningCompleted = 'ReasoningCompleted',
  RunCancelled = 'RunCancelled',
  RunPaused = 'RunPaused',
  RunContinued = 'RunContinued',
  // Team events...
  TeamRunStarted = 'TeamRunStarted',
  // etc...
}

export interface RunResponseContent {
  event: RunEvent;
  content?: string | object;
  content_type: string;
  messages?: ModelMessage[];
  tools?: ToolCall[];
  extra_data?: AgentExtraData;
  images?: ImageData[];
  videos?: VideoData[];
  audio?: AudioData[];
  created_at: number;
}
```

**Extra Data (Reasoning & References):**

```typescript
export interface AgentExtraData {
  reasoning_steps?: ReasoningSteps[];
  reasoning_messages?: ReasoningMessage[];
  references?: ReferenceData[];
}

export interface ReasoningSteps {
  title: string;
  action?: string;
  result: string;
  reasoning: string;
  confidence?: number;
  next_action?: string;
}

export interface ReferenceData {
  query: string;
  references: Reference[];
  time?: number;
}

export interface Reference {
  content: string;
  meta_data: {
    chunk: number;
    chunk_size: number;
  };
  name: string;
}
```

### 2.4 API Layer

**API Routes:**

```typescript
// src/api/routes.ts
export const APIRoutes = {
  Status: (base: string) => `${base}/status`,
  GetAgents: (endpoint: string) => `${endpoint}/agents`,
  GetTeams: (endpoint: string) => `${endpoint}/teams`,
  GetSessions: (base: string) => `${base}/sessions`,
  GetSession: (base: string, sessionId: string) => `${base}/sessions/${sessionId}`,
  // ... more routes
};
```

**API Functions (src/api/os.ts):**

```typescript
// Helper for auth headers
const createHeaders = (authToken?: string): HeadersInit => {
  const headers: HeadersInit = {
    'Content-Type': 'application/json'
  };
  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`;
  }
  return headers;
};

// Key API calls
export const getAgentsAPI = async (endpoint: string, authToken?: string): Promise<AgentDetails[]> => {...};
export const getStatusAPI = async (base: string, authToken?: string): Promise<number> => {...};
export const getAllSessionsAPI = async (base: string, type: 'agent' | 'team', componentId: string, dbId: string, authToken?: string): Promise<Sessions> => {...};
export const getSessionAPI = async (base: string, type: 'agent' | 'team', sessionId: string, dbId?: string, authToken?: string) => {...};
```

### 2.5 Custom Hooks

#### useChatActions

Manages chat operations and endpoint interactions:

```typescript
const useChatActions = () => {
  const {
    getStatus,           // Check endpoint availability
    getAgents,           // Fetch available agents
    getTeams,            // Fetch available teams
    clearChat,           // Clear message history
    focusChatInput,      // Focus textarea
    addMessage,          // Add message to store
    initialize,          // Full initialization
    selectAgent,         // Select active agent
    selectTeam,          // Select active team
    selectSession,       // Load session
  } = useChatActions();
};
```

#### useAIStreamHandler

Processes SSE streaming responses:

```typescript
const useAIChatStreamHandler = () => {
  // Processes RunEvent stream
  const processToolCall = (toolCall: ToolCall, prevToolCalls: ToolCall[]) => {
    // Merge/update tool calls with deduplication
  };

  const processChunkToolCalls = (chunk: any, existingToolCalls: ToolCall[]) => {
    // Handle both single tool object and tools array
  };

  // Handles RunResponseContent events
  // Updates message states based on event type
};
```

#### useAIResponseStream

Manages the actual SSE connection:

```typescript
const useAIResponseStream = () => {
  const { streamResponse } = useAIResponseStream();
  
  // Usage:
  // const stream = streamResponse({
  //   endpoint: selectedEndpoint,
  //   method: 'POST',
  //   path: '/run',
  //   body: {...},
  //   onData: (data) => {...},
  //   onError: (error) => {...},
  //   signal: abortController.signal,
  // });
};
```

#### useSessionLoader

Loads previous chat sessions:

```typescript
const useSessionLoader = () => {
  // Handles:
  // - Fetching all sessions
  // - Loading specific session messages
  // - Session pagination
};
```

### 2.6 UI Component Structure

**Main Layout:**

```typescript
export default function Home() {
  const hasEnvToken = !!process.env.NEXT_PUBLIC_OS_SECURITY_KEY;
  const envToken = process.env.NEXT_PUBLIC_OS_SECURITY_KEY || '';
  
  return (
    <div className="flex h-screen bg-background/80">
      <Sidebar hasEnvToken={hasEnvToken} envToken={envToken} />
      <ChatArea />
    </div>
  );
}
```

**Component Features:**
- Real-time message streaming
- Tool call visualization
- Reasoning steps display
- Reference/citation display
- Multi-media support (images, videos, audio)
- Session history sidebar
- Agent/Team selection
- Dynamic model selection

### 2.7 Configuration

**Environment Variables:**

```bash
NEXT_PUBLIC_OS_SECURITY_KEY=your_auth_token_here
```

**Endpoint Configuration:**
- Default: `http://localhost:7777`
- Can be changed via UI
- Supports both local and production endpoints
- Persisted to localStorage

---

## 3. Key Integration Patterns

### 3.1 CopilotKit Integration Patterns

**Pattern 1: Simple Action Handler**

```typescript
useCopilotAction({
  name: "updateUserProfile",
  parameters: [
    { name: "name", type: "string" },
    { name: "email", type: "string" },
  ],
  handler: async ({ name, email }) => {
    await api.updateProfile({ name, email });
  },
});
```

**Pattern 2: Interactive UI with Response**

```typescript
useCopilotAction({
  name: "selectFromList",
  parameters: [
    { name: "options", type: "string[]" },
    { name: "title", type: "string" },
  ],
  renderAndWaitForResponse: ({ args, respond, status }) => {
    return (
      <SelectDialog
        title={args.title}
        options={args.options}
        onSelect={(selected) => respond(selected)}
      />
    );
  },
});
```

**Pattern 3: Remote-Only Actions (LLM-driven)**

```typescript
useCopilotAction({
  name: "sendEmail",
  available: "remote",
  parameters: [
    { name: "to", type: "string" },
    { name: "subject", type: "string" },
    { name: "body", type: "string" },
  ],
  // No handler - only backend implementation
});
```

**Pattern 4: CoAgent Shared State**

```typescript
const { state, setState } = useCoAgent<FormState>({
  name: "form-filler",
  initialState: { fields: {} },
});

return (
  <Form
    data={state.fields}
    onChange={(newFields) => setState({ fields: newFields })}
  />
);
```

### 3.2 Agent OS Integration Patterns

**Pattern 1: Endpoint Management**

```typescript
const selectedEndpoint = useStore((state) => state.selectedEndpoint);
const authToken = useStore((state) => state.authToken);

const agents = await getAgentsAPI(selectedEndpoint, authToken);
```

**Pattern 2: Streaming Response Handling**

```typescript
const { streamResponse } = useAIResponseStream();

streamResponse({
  endpoint: selectedEndpoint,
  method: 'POST',
  path: '/run',
  body: { agent_id, message, session_id },
  onData: (data: RunResponseContent) => {
    switch (data.event) {
      case RunEvent.ToolCallStarted:
        // Show tool execution
        break;
      case RunEvent.RunContent:
        // Append to message
        break;
      case RunEvent.ReasoningStep:
        // Display reasoning
        break;
      case RunEvent.RunCompleted:
        // Mark complete
        break;
    }
  },
  onError: (error) => {
    setStreamingErrorMessage(error.message);
  },
});
```

**Pattern 3: Message Accumulation from Events**

```typescript
const processChunkToolCalls = (chunk: any, existingToolCalls: ToolCall[]) => {
  if (chunk.tool) {
    return processToolCall(chunk.tool, existingToolCalls);
  } else if (chunk.tools && Array.isArray(chunk.tools)) {
    return chunk.tools.reduce(
      (acc, tc) => processToolCall(tc, acc),
      existingToolCalls
    );
  }
  return existingToolCalls;
};
```

**Pattern 4: Reasoning & References Display**

```typescript
// Messages contain extra_data with reasoning and references
const message: ChatMessage = {
  role: 'agent',
  content: '...',
  extra_data: {
    reasoning_steps: [...],
    references: [...]
  }
};
```

---

## 4. State Management Comparison

| Aspect | CopilotKit | Agent OS |
|--------|-----------|----------|
| State Manager | React Context | Zustand |
| Persistence | In-memory | localStorage (endpoint) |
| Scale | Multi-action, multi-agent | Endpoint-scoped |
| Auth | Per-action | Global token |
| Thread Management | Via runtime client | Via session IDs |
| Backend Coupling | Custom runtime required | Direct API calls |

---

## 5. Event-Driven Architecture

### CopilotKit
- **Message-based**: Appends user/assistant messages
- **Function calls**: Tools called via action names
- **CoAgent updates**: State changes streamed back to client

### Agent OS
- **Event-based**: `RunEvent` enum drives state
- **Tool calls**: Streamed as `ToolCall` objects
- **Reasoning**: `ReasoningStep` events for transparency
- **Session-aware**: All events tied to session/run IDs

---

## 6. UI/UX Rendering Capabilities

### CopilotKit
- **Generative UI**: Render arbitrary React components from LLM
- **Action rendering**: Status-based UI (inProgress, executing, complete)
- **Interactive handlers**: `renderAndWaitForResponse` for user input

### Agent OS
- **Tool visualization**: Display tool calls and results
- **Reasoning display**: Step-by-step reasoning transparency
- **References**: Citation/source display
- **Multi-media**: Images, videos, audio support
- **Streaming UI**: Real-time message updates

---

## 7. Authentication & Authorization

### CopilotKit
- **Per-action auth**: Each action can have `authState` configuration
- **SignInComponent**: Custom auth UI provider
- **Headers**: Custom headers in `CopilotApiConfig`
- **MCP integration**: Separate auth for Model Context Protocol servers

### Agent OS
- **Bearer token**: Single `Authorization: Bearer <token>` header
- **Environment-based**: `NEXT_PUBLIC_OS_SECURITY_KEY` env var
- **UI configuration**: Editable auth token in sidebar
- **Per-endpoint**: Auth token applied to all calls for endpoint

---

## 8. Extensibility Points

### CopilotKit

1. **Custom Runtime Adapter**: Implement `ServiceAdapter` interface
2. **Custom Actions**: Use `useCopilotAction` hook
3. **Custom UI Components**: Pass to CopilotChat
4. **MCP Servers**: Configure in `copilotApiConfig`
5. **Error Handling**: `onError` callback in CopilotKit props

### Agent OS

1. **API Routes**: Add to `APIRoutes` object
2. **Event Handlers**: Switch on `RunEvent` types
3. **Components**: Create custom message renderers
4. **Hooks**: Extend with custom React hooks
5. **Store**: Extend Zustand store for additional state

---

## 9. Recommended Patterns for crypteolas

### For Agent Integration:

1. **Use CopilotKit for:**
   - Complex action logic with generative UI
   - Multi-agent coordination
   - Rich interactive experiences
   - Need for LangGraph integration

2. **Use Agent OS approach for:**
   - Simple streaming chat UI
   - Cryptocurrency-specific data streams
   - Event-driven transparency (reasoning, tool calls)
   - Session-based conversation history

### Hybrid Approach:

```typescript
// CopilotKit for AI reasoning and actions
<CopilotKit runtimeUrl="/api/copilotkit">
  <ChatInterface />
  
  {/* CoAgent for real-time portfolio state */}
  <PortfolioCoAgent />
</CopilotKit>

// Agent OS for market data streaming
<MarketDataStream endpoint="http://agentOS:7777" />
```

### Key Considerations:

1. **Event Streaming**: Use SSE like Agent OS for real-time crypto data
2. **Tool Calls**: Display execution transparently like Agent OS
3. **State Management**: Use Zustand for crypto state (prices, positions)
4. **Reasoning Display**: Show agent's decision-making process
5. **References**: Citation of data sources (price feeds, etc.)

---

## 10. File Structure Reference

### CopilotKit Key Files:

```
/Users/cliste/dev/bonneagar/hackathon/examples/web/agents/CopilotKit/
├── CopilotKit/packages/
│   ├── react-core/src/
│   │   ├── context/copilot-context.tsx
│   │   ├── types/
│   │   │   ├── frontend-action.ts
│   │   │   ├── coagent-action.ts
│   │   │   └── coagent-state.ts
│   │   └── hooks/
│   │       ├── use-copilot-action.ts
│   │       ├── use-coagent.ts
│   │       └── use-copilot-chat.ts
│   ├── runtime/src/
│   │   └── service-adapters/
│   │       ├── openai/
│   │       ├── anthropic/
│   │       └── langchain/
│   └── react-ui/
├── examples/
│   ├── copilot-fully-custom/
│   └── coagents-wait-user-input/
└── docs/
```

### Agent OS Key Files:

```
/Users/cliste/dev/bonneagar/hackathon/examples/web/agents/agent_os/
├── src/
│   ├── api/
│   │   ├── routes.ts
│   │   └── os.ts
│   ├── hooks/
│   │   ├── useChatActions.ts
│   │   ├── useAIStreamHandler.tsx
│   │   ├── useAIResponseStream.tsx
│   │   └── useSessionLoader.tsx
│   ├── types/
│   │   └── os.ts
│   ├── store.ts
│   ├── app/
│   │   ├── layout.tsx
│   │   └── page.tsx
│   └── components/
│       ├── chat/
│       │   ├── Sidebar/
│       │   └── ChatArea/
│       └── ui/
└── package.json
```

