# Frontend Integration for Crypto Analytics Platform

## Overview

This document specifies the frontend architecture for the crypto analytics platform, integrating AI chat interfaces, Web3 components, and data visualization for cryptocurrency assets and knowledge graphs.

## Technology Stack

### Core Framework
- **TanStack Start** - Full-stack React framework with SSR
- **TanStack Router** - File-based type-safe routing
- **TanStack Query** - Server state management
- **Tailwind CSS v4** - Utility-first styling

### Component Libraries
| Library | Purpose | Location |
|---------|---------|----------|
| **CopilotKit** | AI chat interface | `examples/frontend/CopilotKit` |
| **Ant Design Web3** | Multi-chain wallet | `examples/frontend/ant-design-web3` |
| **Agno UI** | Agent reasoning display | `examples/frontend/agno` |
| **Crypto-Charts** | Price visualization | `examples/frontend/crypto-charts` |
| **Duck-UI** | DuckDB analytics | `examples/frontend/duck-ui` |
| **shadcn/ui** | Base UI components | TanStack integration |

## Architecture

### Application Layout

```
┌────────────────────────────────────────────────────────────────────┐
│  Header: Logo | Navigation | WalletConnect (Ant Design Web3)      │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐  ┌─────────────────────────────────────────────┐ │
│  │  Sidebar    │  │  Main Content                                │ │
│  │             │  │                                              │ │
│  │  Sessions   │  │  ┌────────────────────────────────────────┐ │ │
│  │  (CopilotKit)│  │  │  Dashboard / Analysis View            │ │ │
│  │             │  │  │  - Crypto Charts (prices)              │ │ │
│  │  Assets     │  │  │  - Portfolio (Ant Design Web3)         │ │ │
│  │  (Ant Web3) │  │  │  - Knowledge Graph (D3/force-graph)    │ │ │
│  │             │  │  │  - Analytics (Duck-UI patterns)        │ │ │
│  │  Knowledge  │  │  └────────────────────────────────────────┘ │ │
│  │  Entities   │  │                                              │ │
│  │             │  │  ┌────────────────────────────────────────┐ │ │
│  └─────────────┘  │  │  Chat Panel (CopilotKit + Agno)        │ │ │
│                   │  │  - Message stream                       │ │ │
│                   │  │  - Tool calls visualization             │ │ │
│                   │  │  - Agent reasoning steps                │ │ │
│                   │  └────────────────────────────────────────┘ │ │
│                   └─────────────────────────────────────────────┘ │
│                                                                     │
├────────────────────────────────────────────────────────────────────┤
│  Status Bar: Chain status | Gas | Prices | Agent status           │
└────────────────────────────────────────────────────────────────────┘
```

### File-Based Routing Structure

```
src/
├── routes/
│   ├── __root.tsx           # Root layout with providers
│   ├── index.tsx            # Dashboard landing
│   ├── portfolio/
│   │   ├── index.tsx        # Portfolio overview
│   │   └── $asset.tsx       # Single asset view
│   ├── analytics/
│   │   ├── index.tsx        # Analytics dashboard
│   │   ├── queries.tsx      # DuckDB query interface
│   │   └── charts.tsx       # Price charts view
│   ├── knowledge/
│   │   ├── index.tsx        # Knowledge graph explorer
│   │   ├── entities.tsx     # Entity browser
│   │   └── documents.tsx    # Document viewer
│   ├── chat/
│   │   ├── index.tsx        # Main chat interface
│   │   └── $session.tsx     # Session-specific chat
│   └── settings/
│       └── index.tsx        # User settings
├── components/
│   ├── chat/                # CopilotKit + Agno integration
│   ├── web3/                # Ant Design Web3 wrappers
│   ├── charts/              # Price chart components
│   ├── graph/               # Knowledge graph visualization
│   └── ui/                  # shadcn/ui components
├── lib/
│   ├── web3.ts              # Wallet configuration
│   ├── chat.ts              # CopilotKit configuration
│   └── graph.ts             # Graph query utilities
└── stores/
    ├── portfolio.ts         # Portfolio state
    ├── chat.ts              # Chat session state
    └── graph.ts             # Graph navigation state
```

## Component Integration

### 1. CopilotKit Chat Interface

```tsx
// components/chat/ChatProvider.tsx
import { CopilotKit, useCopilotChat } from "@copilotkit/react-core";
import { CopilotPopup, CopilotSidebar } from "@copilotkit/react-ui";

export function CryptoAssistantProvider({ children }: { children: React.ReactNode }) {
  return (
    <CopilotKit
      runtimeUrl="/api/copilot"
      transcribeAudioUrl="/api/transcribe"
      textToSpeechUrl="/api/speak"
    >
      {children}
      <CopilotSidebar
        defaultOpen={true}
        instructions={`
          You are a crypto analytics assistant with access to:
          - Real-time price data from CoinGecko and Pyth
          - On-chain analytics via DuckDB
          - Protocol knowledge graph (Memgraph)
          - Document search (LanceDB vectors)

          Help users analyze DeFi protocols, track portfolios, and understand risks.
        `}
        labels={{
          title: "Crypto Analyst",
          initial: "How can I help you analyze crypto markets today?"
        }}
      />
    </CopilotKit>
  );
}
```

### 2. Ant Design Web3 Wallet Connection

```tsx
// components/web3/WalletProvider.tsx
import {
  Web3ConfigProvider,
  ConnectButton,
  useAccount,
  useBalance
} from "@ant-design/web3";
import { WagmiProvider, createConfig, http } from "wagmi";
import { mainnet, polygon, arbitrum, base } from "wagmi/chains";

const config = createConfig({
  chains: [mainnet, polygon, arbitrum, base],
  transports: {
    [mainnet.id]: http(),
    [polygon.id]: http(),
    [arbitrum.id]: http(),
    [base.id]: http(),
  },
});

export function WalletProvider({ children }: { children: React.ReactNode }) {
  return (
    <WagmiProvider config={config}>
      <Web3ConfigProvider
        theme={{
          algorithm: "dark", // or "light"
        }}
      >
        {children}
      </Web3ConfigProvider>
    </WagmiProvider>
  );
}

// Header wallet button
export function WalletConnect() {
  return (
    <ConnectButton
      type="primary"
      showBalance
      profileModal={{
        showDisconnect: true,
        showAssets: true,
        showTransactions: true,
      }}
    />
  );
}
```

### 3. Crypto Price Charts

```tsx
// components/charts/PriceChart.tsx
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";
import { usePythPrice } from "../hooks/usePythPrice";

interface PriceChartProps {
  symbol: string;
  timeframe: "1h" | "1d" | "1w" | "1m";
}

export function PriceChart({ symbol, timeframe }: PriceChartProps) {
  const { data, isLoading } = usePythPrice(symbol, timeframe);

  if (isLoading) {
    return <ChartSkeleton />;
  }

  return (
    <div className="h-[400px] w-full">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data}>
          <XAxis
            dataKey="timestamp"
            tickFormatter={(ts) => formatTime(ts, timeframe)}
          />
          <YAxis
            domain={["auto", "auto"]}
            tickFormatter={(price) => formatPrice(price)}
          />
          <Tooltip
            content={<CustomTooltip />}
            cursor={{ strokeDasharray: "3 3" }}
          />
          <Line
            type="monotone"
            dataKey="price"
            stroke="#10b981"
            strokeWidth={2}
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
```

### 4. Knowledge Graph Visualization

```tsx
// components/graph/KnowledgeGraph.tsx
import ForceGraph2D from "react-force-graph-2d";
import { useGraphData } from "../hooks/useGraphData";

interface KnowledgeGraphProps {
  centerEntity?: string;
  entityTypes?: string[];
  maxDepth?: number;
}

export function KnowledgeGraph({
  centerEntity,
  entityTypes = ["Token", "Protocol", "Exchange"],
  maxDepth = 2
}: KnowledgeGraphProps) {
  const { nodes, links, isLoading } = useGraphData({
    centerEntity,
    entityTypes,
    maxDepth
  });

  const nodeColorMap: Record<string, string> = {
    Token: "#3b82f6",      // blue
    Protocol: "#10b981",   // green
    Exchange: "#f59e0b",   // amber
    LiquidityPool: "#8b5cf6", // purple
    Risk: "#ef4444",       // red
    Document: "#6b7280",   // gray
  };

  return (
    <div className="h-[600px] w-full border rounded-lg">
      <ForceGraph2D
        graphData={{ nodes, links }}
        nodeAutoColorBy="type"
        nodeColor={(node) => nodeColorMap[node.type] || "#888"}
        nodeLabel={(node) => `${node.type}: ${node.label}`}
        linkLabel={(link) => link.type}
        linkDirectionalArrowLength={3}
        linkDirectionalArrowRelPos={1}
        onNodeClick={(node) => handleNodeClick(node)}
        cooldownTicks={100}
        onEngineStop={() => console.log("Graph stabilized")}
      />
    </div>
  );
}
```

### 5. Agent Reasoning Display (Agno Pattern)

```tsx
// components/chat/AgentReasoning.tsx
import { cn } from "@/lib/utils";

interface ReasoningStep {
  type: "thinking" | "tool_call" | "tool_result" | "conclusion";
  content: string;
  tool?: string;
  duration?: number;
}

export function AgentReasoning({ steps }: { steps: ReasoningStep[] }) {
  return (
    <div className="space-y-2 border-l-2 border-gray-200 pl-4 ml-4">
      {steps.map((step, i) => (
        <div
          key={i}
          className={cn(
            "py-2",
            step.type === "thinking" && "text-gray-500 italic",
            step.type === "tool_call" && "bg-blue-50 rounded px-2",
            step.type === "tool_result" && "bg-green-50 rounded px-2",
            step.type === "conclusion" && "font-medium"
          )}
        >
          {step.type === "tool_call" && (
            <span className="text-xs bg-blue-200 px-1 rounded mr-2">
              Calling: {step.tool}
            </span>
          )}
          {step.type === "tool_result" && (
            <span className="text-xs bg-green-200 px-1 rounded mr-2">
              Result from: {step.tool}
            </span>
          )}
          <span className="text-sm">{step.content}</span>
          {step.duration && (
            <span className="text-xs text-gray-400 ml-2">
              ({step.duration}ms)
            </span>
          )}
        </div>
      ))}
    </div>
  );
}
```

### 6. DuckDB Analytics Interface

```tsx
// components/analytics/QueryInterface.tsx
import { useState } from "react";
import { useDuckDB } from "../hooks/useDuckDB";
import { Editor } from "@monaco-editor/react";
import { DataTable } from "./DataTable";

export function QueryInterface() {
  const [query, setQuery] = useState("");
  const { execute, result, isLoading, error } = useDuckDB();

  const exampleQueries = [
    {
      name: "Top tokens by volume",
      query: `
        SELECT symbol, SUM(volume_24h) as total_volume
        FROM token_metrics
        WHERE timestamp > NOW() - INTERVAL '24 hours'
        GROUP BY symbol
        ORDER BY total_volume DESC
        LIMIT 10
      `
    },
    {
      name: "Protocol TVL changes",
      query: `
        SELECT
          protocol,
          tvl_usd,
          LAG(tvl_usd) OVER (PARTITION BY protocol ORDER BY timestamp) as prev_tvl,
          (tvl_usd - LAG(tvl_usd) OVER (PARTITION BY protocol ORDER BY timestamp)) / LAG(tvl_usd) OVER (PARTITION BY protocol ORDER BY timestamp) * 100 as pct_change
        FROM protocol_tvl
        ORDER BY timestamp DESC
        LIMIT 20
      `
    }
  ];

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        {exampleQueries.map((ex) => (
          <button
            key={ex.name}
            onClick={() => setQuery(ex.query)}
            className="text-sm px-2 py-1 bg-gray-100 rounded hover:bg-gray-200"
          >
            {ex.name}
          </button>
        ))}
      </div>

      <Editor
        height="200px"
        language="sql"
        theme="vs-dark"
        value={query}
        onChange={(value) => setQuery(value || "")}
        options={{
          minimap: { enabled: false },
          fontSize: 14,
        }}
      />

      <button
        onClick={() => execute(query)}
        disabled={isLoading}
        className="px-4 py-2 bg-blue-600 text-white rounded disabled:opacity-50"
      >
        {isLoading ? "Running..." : "Execute Query"}
      </button>

      {error && (
        <div className="p-4 bg-red-50 text-red-700 rounded">
          {error.message}
        </div>
      )}

      {result && <DataTable data={result} />}
    </div>
  );
}
```

## State Management

### Zustand Stores

```typescript
// stores/portfolio.ts
import { create } from "zustand";
import { persist } from "zustand/middleware";

interface PortfolioState {
  assets: Asset[];
  totalValue: number;
  selectedAsset: string | null;

  // Actions
  setAssets: (assets: Asset[]) => void;
  selectAsset: (symbol: string | null) => void;
  refreshPrices: () => Promise<void>;
}

export const usePortfolioStore = create<PortfolioState>()(
  persist(
    (set, get) => ({
      assets: [],
      totalValue: 0,
      selectedAsset: null,

      setAssets: (assets) => {
        const totalValue = assets.reduce(
          (sum, a) => sum + a.balance * a.price,
          0
        );
        set({ assets, totalValue });
      },

      selectAsset: (symbol) => set({ selectedAsset: symbol }),

      refreshPrices: async () => {
        const { assets } = get();
        const prices = await fetchPrices(assets.map((a) => a.symbol));
        const updated = assets.map((a) => ({
          ...a,
          price: prices[a.symbol] || a.price,
        }));
        get().setAssets(updated);
      },
    }),
    { name: "portfolio-storage" }
  )
);
```

```typescript
// stores/chat.ts
import { create } from "zustand";

interface ChatState {
  sessions: ChatSession[];
  activeSession: string | null;
  messages: Message[];

  // Actions
  createSession: () => string;
  selectSession: (id: string) => void;
  addMessage: (message: Message) => void;
  streamMessage: (content: string) => void;
}

export const useChatStore = create<ChatState>((set, get) => ({
  sessions: [],
  activeSession: null,
  messages: [],

  createSession: () => {
    const id = crypto.randomUUID();
    const session = { id, title: "New Chat", createdAt: new Date() };
    set((state) => ({
      sessions: [...state.sessions, session],
      activeSession: id,
      messages: [],
    }));
    return id;
  },

  selectSession: (id) => {
    // Load messages for session
    set({ activeSession: id, messages: loadMessages(id) });
  },

  addMessage: (message) => {
    set((state) => ({
      messages: [...state.messages, message],
    }));
  },

  streamMessage: (content) => {
    set((state) => {
      const messages = [...state.messages];
      const last = messages[messages.length - 1];
      if (last?.role === "assistant" && last.streaming) {
        last.content += content;
      }
      return { messages };
    });
  },
}));
```

## API Integration

### Server Functions (TanStack Start)

```typescript
// lib/api/graph.ts
import { createServerFn } from "@tanstack/react-start";

export const queryKnowledgeGraph = createServerFn("POST", async (params: {
  query: string;
  entityTypes?: string[];
  limit?: number;
}) => {
  const { query, entityTypes = [], limit = 50 } = params;

  // Query Memgraph
  const session = graphDriver.session();
  try {
    const result = await session.run(`
      MATCH (n)
      WHERE ANY(label IN labels(n) WHERE label IN $entityTypes)
        AND (n.name CONTAINS $query OR n.symbol CONTAINS $query)
      OPTIONAL MATCH (n)-[r]->(m)
      RETURN n, collect({rel: type(r), target: m}) as connections
      LIMIT $limit
    `, { query, entityTypes, limit });

    return formatGraphResults(result.records);
  } finally {
    await session.close();
  }
});

export const searchDocuments = createServerFn("POST", async (params: {
  query: string;
  docTypes?: string[];
  limit?: number;
}) => {
  const { query, docTypes, limit = 10 } = params;

  // Search LanceDB vectors
  const table = await lanceDb.openTable("crypto_docs");
  const embedding = await embedQuery(query);

  let search = table.search(embedding).limit(limit);

  if (docTypes?.length) {
    search = search.filter(`doc_type IN (${docTypes.map(t => `'${t}'`).join(",")})`);
  }

  return search.toArray();
});
```

### React Query Hooks

```typescript
// hooks/useGraphData.ts
import { useQuery } from "@tanstack/react-query";
import { queryKnowledgeGraph } from "../lib/api/graph";

export function useGraphData(params: {
  centerEntity?: string;
  entityTypes: string[];
  maxDepth: number;
}) {
  return useQuery({
    queryKey: ["graph", params],
    queryFn: () => queryKnowledgeGraph({
      query: params.centerEntity || "",
      entityTypes: params.entityTypes,
      limit: 100
    }),
    select: (data) => transformToGraphFormat(data),
    staleTime: 30000, // 30 seconds
  });
}
```

## Demo Application Structure

### Package.json Dependencies

```json
{
  "dependencies": {
    "@tanstack/react-router": "^1.x",
    "@tanstack/react-query": "^5.x",
    "@tanstack/react-start": "^1.x",
    "@copilotkit/react-core": "^1.x",
    "@copilotkit/react-ui": "^1.x",
    "@ant-design/web3": "^1.x",
    "@ant-design/web3-wagmi": "^1.x",
    "wagmi": "^2.x",
    "viem": "^2.x",
    "recharts": "^2.x",
    "react-force-graph-2d": "^1.x",
    "@monaco-editor/react": "^4.x",
    "zustand": "^4.x",
    "tailwindcss": "^4.x",
    "@radix-ui/react-*": "latest",
    "class-variance-authority": "^0.7.x",
    "clsx": "^2.x"
  }
}
```

### Demo Features Checklist

- [ ] Wallet connection (multi-chain)
- [ ] Portfolio display with balances
- [ ] Price charts (Pyth feeds)
- [ ] AI chat interface
- [ ] Tool calling visualization
- [ ] Knowledge graph explorer
- [ ] Document search
- [ ] DuckDB query interface
- [ ] Agent reasoning display
- [ ] Transaction history
- [ ] Protocol analytics dashboard

## References

- CopilotKit docs: https://docs.copilotkit.ai
- Ant Design Web3: https://web3.ant.design
- TanStack Start: https://tanstack.com/start
- Recharts: https://recharts.org
- react-force-graph: https://github.com/vasturiano/react-force-graph
