---
title: "useAgent Hook"
source: "https://docs.copilotkit.ai/direct-to-llm/guides/use-agent-hook"
author:
published:
created: 2025-12-15
description: "Access and interact with your Direct-to-LLM agent directly from React components"
tags:
  - "clippings"
---
CopilotKit 1.50 is available!

[See What's New](https://docs.copilotkit.ai/whats-new/v1-50)

Access and interact with your Direct-to-LLM agent directly from React components

## Overview

The `useAgent` hook provides direct access to your Direct-to-LLM agent from any React component. It gives you real-time access to the agent's state, messages, execution status, and allows you to subscribe to custom events.

This enables you to build custom agent dashboards, monitoring tools, and interactive features that respond to your agent's behavior.

This page covers everything you need to know about using `useAgent` with Direct-to-LLM. Select where you'd like to get started below.### [Getting started](https://docs.copilotkit.ai/direct-to-llm/guides/#getting-started)

[

Learn the basics of accessing your agent and displaying its properties.

](https://docs.copilotkit.ai/direct-to-llm/guides/#getting-started)Working with State

Access and update shared state between your app and agent.

[View original](https://docs.copilotkit.ai/direct-to-llm/guides/#working-with-state)Agent Events

Subscribe to agent lifecycle events and custom events.

[View original](https://docs.copilotkit.ai/direct-to-llm/guides/#subscribing-to-agent-events)Reference

Complete API reference documentation for useAgent.

[View original](https://docs.copilotkit.ai/reference/hooks/useAgent)

## Getting started

Let's start by building a simple component that displays agent information.

### Import the hook

First, import `useAgent` from the v2 package:

page.tsx

```
import { useAgent } from "@copilotkit/react-core/v2";
```

### Access your agent

Call the hook to get a reference to your agent:

page.tsx

```
export function AgentInfo() {

  const { agent } = useAgent(); 

  return (

    <div>

      <p>Agent ID: {agent.id}</p>

      <p>Thread ID: {agent.threadId}</p>

      <p>Status: {agent.isRunning ? "Running" : "Idle"}</p>

      <p>Messages: {agent.messages.length}</p>

    </div>

  );

}
```

The hook will throw an error if no agent is configured, so you can safely use `agent` without null checks.

### Display messages

Access the agent's conversation history:

page.tsx

```
export function MessageList() {

  const { agent } = useAgent();

  return (

    <div>

      {agent.messages.map((msg) => (

        <div key={msg.id}>

          <strong>{msg.role}:</strong>

          <span>{msg.content}</span>

        </div>

      ))}

    </div>

  );

}
```

### Show running status

Add a loading indicator when the agent is processing:

page.tsx

```
export function AgentStatus() {

  const { agent } = useAgent();

  return (

    <div>

      {agent.isRunning ? (

        <div>

          <div className="spinner" />

          <span>Agent is processing...</span>

        </div>

      ) : (

        <span>Ready</span>

      )}

    </div>

  );

}
```

## Working with State

Agents expose their state through the `agent.state` property. This state is shared between your application and the agent - both can read and modify it.

### Reading State

Access your agent's current state:

page.tsx

```
export function StateDisplay() {

  const { agent } = useAgent();

  return (

    <div>

      <h3>Agent State</h3>

      <pre>{JSON.stringify(agent.state, null, 2)}</pre>

      {/* Access specific properties */}

      {agent.state.user_name && <p>User: {agent.state.user_name}</p>}

      {agent.state.preferences && <p>Preferences: {JSON.stringify(agent.state.preferences)}</p>}

    </div>

  );

}
```

Your component automatically re-renders when the agent's state changes.

### Updating State

Update state that your agent can access:

page.tsx

```
export function ThemeSelector() {

  const { agent } = useAgent();

  const updateTheme = (theme: string) => {

    agent.setState({

      ...agent.state,

      user_theme: theme,

    });

  };

  return (

    <div>

      <button onClick={() => updateTheme("dark")}>Dark Mode</button>

      <button onClick={() => updateTheme("light")}>Light Mode</button>

      <p>Current: {agent.state.user_theme || "default"}</p>

    </div>

  );

}
```

State updates are immediately available to your agent in its next execution.

## Subscribing to Agent Events

You can subscribe to agent events using the `subscribe()` method. This is useful for logging, monitoring, or responding to specific agent behaviors.

### Basic Event Subscription

page.tsx

### Available Events

The `AgentSubscriber` interface provides:

- **`onCustomEvent`** - Custom events emitted by the agent
- **`onRunStartedEvent`** - Agent starts executing
- **`onRunFinalized`** - Agent completes execution
- **`onStateChanged`** - Agent's state changes
- **`onMessagesChanged`** - Messages are added or modified

## Rendering Tool Calls

You can customize how agent tool calls are displayed in your UI. First, define your tool renderers:

components/weather-tool.tsx

```
import { defineToolCallRenderer } from "@copilotkit/react-core/v2";

export const weatherToolRender = defineToolCallRenderer({

  name: "get_weather",

  render: ({ args, status }) => {

    return <WeatherCard location={args.location} status={status} />;

  },

});

function WeatherCard({ location, status }: { location?: string; status: string }) {

  return (

    <div className="rounded-lg border p-6 shadow-sm">

      <h3 className="text-xl font-semibold">Weather in {location}</h3>

      <div className="mt-4">

        <span className="text-5xl font-light">70°F</span>

      </div>

      {status === "executing" && <div className="spinner">Loading...</div>}

    </div>

  );

}
```

Register your tool renderers with CopilotKit:

layout.tsx

```
import { CopilotKit } from "@copilotkit/react-core";

import { weatherToolRender } from "./components/weather-tool";

export default function RootLayout({ children }) {

  return (

    <CopilotKit

      runtimeUrl="/api/copilotkit"

      renderToolCalls={[weatherToolRender]}

    >

      {children}

    </CopilotKit>

  );

}
```

Then use `useRenderToolCall` to render tool calls from agent messages:

components/message-list.tsx

```
import { useAgent, useRenderToolCall } from "@copilotkit/react-core/v2";

export function MessageList() {

  const { agent } = useAgent();

  const renderToolCall = useRenderToolCall();

  return (

    <div className="messages">

      {agent.messages.map((message) => (

        <div key={message.id}>

          {/* Display message content */}

          {message.content && <p>{message.content}</p>}

          {/* Render tool calls if present */}

          {message.role === "assistant" && message.toolCalls?.map((toolCall) => {

            const toolMessage = agent.messages.find(

              (m) => m.role === "tool" && m.toolCallId === toolCall.id

            );

            return (

              <div key={toolCall.id}>

                {renderToolCall({ toolCall, toolMessage })}

              </div>

            );

          })}

        </div>

      ))}

    </div>

  );

}
```

## Building a Complete Dashboard

Here's a full example combining all concepts into an interactive agent dashboard:

page.tsx

```
"use client";

import { useAgent } from "@copilotkit/react-core/v2";

export default function AgentDashboard() {

  const { agent } = useAgent();

  return (

    <div className="p-8 max-w-4xl mx-auto space-y-6">

      {/* Status */}

      <div className="p-6 bg-white rounded-lg shadow">

        <h2 className="text-xl font-bold mb-4">Agent Status</h2>

        <div className="space-y-2">

          <div className="flex items-center gap-2">

            <div className={\`w-3 h-3 rounded-full ${

              agent.isRunning ? "bg-yellow-500 animate-pulse" : "bg-green-500"

            }\`} />

            <span>{agent.isRunning ? "Running" : "Idle"}</span>

          </div>

          <div>Thread: {agent.threadId}</div>

          <div>Messages: {agent.messages.length}</div>

        </div>

      </div>

      {/* State */}

      <div className="p-6 bg-white rounded-lg shadow">

        <h2 className="text-xl font-bold mb-4">Agent State</h2>

        <pre className="bg-gray-50 p-4 rounded text-sm overflow-auto">

          {JSON.stringify(agent.state, null, 2)}

        </pre>

      </div>

      {/* Messages */}

      <div className="p-6 bg-white rounded-lg shadow">

        <h2 className="text-xl font-bold mb-4">Conversation</h2>

        <div className="space-y-3">

          {agent.messages.map((msg) => (

            <div

              key={msg.id}

              className={\`p-3 rounded-lg ${

                msg.role === "user" ? "bg-blue-50 ml-8" : "bg-gray-50 mr-8"

              }\`}

            >

              <div className="font-semibold text-sm mb-1">

                {msg.role === "user" ? "You" : "Agent"}

              </div>

              <div>{msg.content}</div>

            </div>

          ))}

        </div>

      </div>

    </div>

  );

}
```

## See Also

- [useAgent API Reference](https://docs.copilotkit.ai/reference/hooks/useAgent) - Complete API documentation[Vibe Coding MCP](https://docs.copilotkit.ai/direct-to-llm/guides/vibe-coding-mcp)

[

Use our MCP server to connect your LLM to CopilotKit.

](https://docs.copilotkit.ai/direct-to-llm/guides/vibe-coding-mcp)[

Customize UI

Customize the look, feel, and functionality of CopilotKit's UI components.

](https://docs.copilotkit.ai/direct-to-llm/guides/custom-look-and-feel)

![](https://static.scarf.sh/a.png?x-pxid=ffc9f65d-0186-4575-b065-61d62ea9d7d3)