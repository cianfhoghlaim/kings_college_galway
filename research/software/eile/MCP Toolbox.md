---
title: "MCP Toolbox"
source: "https://docs.agno.com/basics/tools/mcp/mcp-toolbox"
author:
  - "[[Agno]]"
published:
created: 2025-12-13
description: "Learn how to use MCPToolbox with Agno to connect to MCP Toolbox for Databases with tool filtering capabilities."
tags:
  - "clippings"
---
**MCPToolbox** enables Agents to connect to Google’s [MCP Toolbox for Databases](https://googleapis.github.io/genai-toolbox/getting-started/introduction/) with advanced filtering capabilities. It extends Agno’s `MCPTools` functionality to filter tools by toolset or tool name, allowing agents to load only the specific database tools they need.

## Prerequisites

You’ll need the following to use MCPToolbox:

```
pip install toolbox-core
```

Our default setup will also require you to have Docker or Podman installed, to run the MCP Toolbox server and database for the examples.

## Quick Start

Get started with MCPToolbox instantly using our fully functional demo.This starts a PostgreSQL database with sample hotel data and an MCP Toolbox server that exposes database operations as filtered tools.

## Verification

To verify that your docker/podman setup is working correctly, you can check the database connection:

```
# Using Docker Compose

docker-compose exec db psql -U toolbox_user -d toolbox_db -c "SELECT COUNT(*) FROM hotels;"

# Using Podman

podman exec db psql -U toolbox_user -d toolbox_db -c "SELECT COUNT(*) FROM hotels;"
```

## Basic Example

Here’s the simplest way to use MCPToolbox (after running the Quick Start setup):

```
import asyncio

from agno.agent import Agent

from agno.models.openai import OpenAIChat

from agno.tools.mcp_toolbox import MCPToolbox

async def main():

    # Connect to the running MCP Toolbox server and filter to hotel tools only

    async with MCPToolbox(

        url="http://127.0.0.1:5001",

        toolsets=["hotel-management"]  # Only load hotel search tools

    ) as toolbox:

        agent = Agent(

            model=OpenAIChat(),

            tools=[toolbox],

            instructions="You help users find hotels. Always mention hotel ID, name, location, and price tier."

        )

        

        # Ask the agent to find hotels

        await agent.aprint_response("Find luxury hotels in Zurich")

# Run the example

asyncio.run(main())
```

## How MCPToolbox Works

MCPToolbox solves the **tool overload problem**. Without filtering, your agent gets overwhelmed with too many database tools:**Without MCPToolbox (50+ tools):**

```
# Agent gets ALL database tools - overwhelming!

tools = MCPTools(url="http://127.0.0.1:5001")  # 50+ tools
```

**With MCPToolbox (3 relevant tools):**

```
# Agent gets only hotel management tools - focused!

tools = MCPToolbox(url="http://127.0.0.1:5001", toolsets=["hotel-management"])  # 3 tools
```

**The flow:**
1. MCP Toolbox Server exposes 50+ database tools
2. MCPToolbox connects and loads ALL tools internally
3. Filters to only the `hotel-management` toolset (3 tools)
4. Agent sees only the 3 relevant tools and stays focused

## Advanced Usage

### Multiple Toolsets

Load tools from multiple related toolsets:

cookbook/tools/mcp/mcp\_toolbox\_for\_db.py

```
import asyncio

from textwrap import dedent

from agno.agent import Agent

from agno.tools.mcp_toolbox import MCPToolbox

url = "http://127.0.0.1:5001"

async def run_agent(message: str = None) -> None:

    """Run an interactive CLI for the Hotel agent with the given message."""

    async with MCPToolbox(

        url=url, toolsets=["hotel-management", "booking-system"]

    ) as db_tools:

        print(db_tools.functions)  # Print available tools for debugging

        agent = Agent(

            tools=[db_tools],

            instructions=dedent(

                """ \

                You're a helpful hotel assistant. You handle hotel searching, booking and

                cancellations. When the user searches for a hotel, mention it's name, id,

                location and price tier. Always mention hotel ids while performing any

                searches. This is very important for any operations. For any bookings or

                cancellations, please provide the appropriate confirmation. Be sure to

                update checkin or checkout dates if mentioned by the user.

                Don't ask for confirmations from the user.

            """

            ),

            markdown=True,

            show_tool_calls=True,

            add_history_to_messages=True,

            debug_mode=True,

        )

        await agent.acli_app(message=message, stream=True)

if __name__ == "__main__":

    asyncio.run(run_agent(message=None))
```

### Custom Authentication and Parameters

For production scenarios with authentication:

```
async def production_example():

    async with MCPToolbox(url=url) as toolbox:

        # Load with authentication and bound parameters

        hotel_tools = await toolbox.load_toolset(

            "hotel-management",

            auth_token_getters={"hotel_api": lambda: "your-hotel-api-key"},

            bound_params={"region": "us-east-1"},

        )

        booking_tools = await toolbox.load_toolset(

            "booking-system",

            auth_token_getters={"booking_api": lambda: "your-booking-api-key"},

            bound_params={"environment": "production"},

        )

        # Use individual tools instead of the toolbox

        all_tools = hotel_tools + booking_tools[:2]  # First 2 booking tools only

        

        agent = Agent(tools=all_tools, instructions="Hotel management with auth.")

        await agent.aprint_response("Book a hotel for tonight")
```

### Manual Connection Management

For explicit control over connections:

```
async def manual_connection_example():

    # Initialize without auto-connection

    toolbox = MCPToolbox(url=url, toolsets=["hotel-management"])

    

    try:

        await toolbox.connect()

        agent = Agent(

            tools=[toolbox],

            instructions="Hotel search assistant.",

            markdown=True

        )

        await agent.aprint_response("Show me hotels in Basel")

    finally:

        await toolbox.close()  # Always clean up
```

## Toolkit Params

Only one of `toolsets` or `tool_name` can be specified. The implementation validates this and raises a `ValueError` if both are provided.

## Toolkit Functions

## Demo Examples

The complete demo includes multiple working patterns:
- **[Basic Agent](https://github.com/agno-agi/agno/blob/main/cookbook/tools/mcp/mcp_toolbox_demo/agent.py)**: Simple hotel assistant with toolset filtering
- **[AgentOS Integration](https://github.com/agno-agi/agno/blob/main/cookbook/tools/mcp/mcp_toolbox_demo/agent_os.py)**: Integration with AgentOS control plane
- **[Workflow Integration](https://github.com/agno-agi/agno/blob/main/cookbook/tools/mcp/mcp_toolbox_demo/hotel_management_workflows.py)**: Using MCPToolbox in Agno workflows
- **[Type-Safe Agent](https://github.com/agno-agi/agno/blob/main/cookbook/tools/mcp/mcp_toolbox_demo/hotel_management_typesafe.py)**: Implementation with Pydantic models
You can use `include_tools` or `exclude_tools` to modify the list of tools the agent has access to. Learn more about [selecting tools](https://docs.agno.com/basics/tools/selecting-tools).

## Developer Resources

- View [Tools](https://github.com/agno-agi/agno/blob/main/libs/agno/agno/tools/mcp_toolbox.py)
- View [Cookbook](https://github.com/agno-agi/agno/tree/main/cookbook/tools/mcp/mcp_toolbox_demo)
For more information about MCP Toolbox for Databases, visit the [official documentation](https://googleapis.github.io/genai-toolbox/getting-started/introduction/).

[Overview](https://docs.agno.com/basics/tools/mcp/overview) [Multiple MCP Servers](https://docs.agno.com/basics/tools/mcp/multiple-servers)