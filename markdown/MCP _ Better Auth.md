---
title: "MCP | Better Auth"
source: "https://www.better-auth.com/docs/plugins/mcp"
author:
  - "[[Device Authorization]]"
published:
created: 2025-12-16
description: "MCP provider plugin for Better Auth"
tags:
  - "clippings"
---
## MCP

`OAuth` `MCP`

The **MCP** plugin lets your app act as an OAuth provider for MCP clients. It handles authentication and makes it easy to issue and manage access tokens for MCP applications.

## Installation

### Add the Plugin

Add the MCP plugin to your auth configuration and specify the login page path.

auth.ts

### Generate Schema

Run the migration or generate the schema to add the necessary fields and tables to the database.

```
npx @better-auth/cli migrate
```

```
npx @better-auth/cli generate
```

The MCP plugin uses the same schema as the OIDC Provider plugin. See the [OIDC Provider Schema](https://www.better-auth.com/docs/plugins/oidc-provider#schema) section for details.

## Usage

### OAuth Discovery Metadata

Better Auth already handles the `/api/auth/.well-known/oauth-authorization-server` route automatically but some client may fail to parse the `WWW-Authenticate` header and default to `/.well-known/oauth-authorization-server` (this can happen, for example, if your CORS configuration doesn't expose the `WWW-Authenticate`). For this reason it's better to add a route to expose OAuth metadata for MCP clients:

.well-known/oauth-authorization-server/route.ts

```
import { oAuthDiscoveryMetadata } from "better-auth/plugins";

import { auth } from "../../../lib/auth";

export const GET = oAuthDiscoveryMetadata(auth);
```

### OAuth Protected Resource Metadata

Better Auth already handles the `/api/auth/.well-known/oauth-protected-resource` route automatically but some client may fail to parse the `WWW-Authenticate` header and default to `/.well-known/oauth-protected-resource` (this can happen, for example, if your CORS configuration doesn't expose the `WWW-Authenticate`). For this reason it's better to add a route to expose OAuth metadata for MCP clients:

/.well-known/oauth-protected-resource/route.ts

```
import { oAuthProtectedResourceMetadata } from "better-auth/plugins";

import { auth } from "@/lib/auth";

export const GET = oAuthProtectedResourceMetadata(auth);
```

### MCP Session Handling

You can use the helper function `withMcpAuth` to get the session and handle unauthenticated calls automatically.

api/\[transport\]/route.ts

```
import { auth } from "@/lib/auth";

import { createMcpHandler } from "@vercel/mcp-adapter";

import { withMcpAuth } from "better-auth/plugins";

import { z } from "zod";

const handler = withMcpAuth(auth, (req, session) => {

    // session contains the access token record with scopes and user ID

    return createMcpHandler(

        (server) => {

            server.tool(

                "echo",

                "Echo a message",

                { message: z.string() },

                async ({ message }) => {

                    return {

                        content: [{ type: "text", text: \`Tool echo: ${message}\` }],

                    };

                },

            );

        },

        {

            capabilities: {

                tools: {

                    echo: {

                        description: "Echo a message",

                    },

                },

            },

        },

        {

            redisUrl: process.env.REDIS_URL,

            basePath: "/api",

            verboseLogs: true,

            maxDuration: 60,

        },

    )(req);

});

export { handler as GET, handler as POST, handler as DELETE };
```

You can also use `auth.api.getMcpSession` to get the session using the access token sent from the MCP client:

api/\[transport\]/route.ts

```
import { auth } from "@/lib/auth";

import { createMcpHandler } from "@vercel/mcp-adapter";

import { z } from "zod";

const handler = async (req: Request) => {

     // session contains the access token record with scopes and user ID

    const session = await auth.api.getMcpSession({

        headers: req.headers

    })

    if(!session){

        //this is important and you must return 401

        return new Response(null, {

            status: 401

        })

    }

    return createMcpHandler(

        (server) => {

            server.tool(

                "echo",

                "Echo a message",

                { message: z.string() },

                async ({ message }) => {

                    return {

                        content: [{ type: "text", text: \`Tool echo: ${message}\` }],

                    };

                },

            );

        },

        {

            capabilities: {

                tools: {

                    echo: {

                        description: "Echo a message",

                    },

                },

            },

        },

        {

            redisUrl: process.env.REDIS_URL,

            basePath: "/api",

            verboseLogs: true,

            maxDuration: 60,

        },

    )(req);

}

export { handler as GET, handler as POST, handler as DELETE };
```

## Configuration

The MCP plugin accepts the following configuration options:

Prop

Type

### OIDC Configuration

The plugin supports additional OIDC configuration options through the `oidcConfig` parameter:

Prop

Type

## Schema

The MCP plugin uses the same schema as the OIDC Provider plugin. See the [OIDC Provider Schema](https://www.better-auth.com/docs/plugins/oidc-provider#schema) section for details.

[Edit on GitHub](https://github.com/better-auth/better-auth/blob/canary/docs/content/docs/plugins/mcp.mdx)[Previous Page](https://www.better-auth.com/docs/plugins/api-key)

[

API Key

](https://www.better-auth.com/docs/plugins/api-key)[

Next Page

Organization

](https://www.better-auth.com/docs/plugins/organization)