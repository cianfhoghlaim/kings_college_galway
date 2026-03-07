---
title: "Enable Integration API"
source: "https://docs.pangolin.net/self-host/advanced/integration-api"
author:
  - "[[Pangolin Docs]]"
published:
created: 2025-12-08
description: "Enable and configure the Integration API for external access"
tags:
  - "clippings"
---
[Skip to main content](https://docs.pangolin.net/self-host/advanced/#content-area)

The Integration API provides programmatic access to Pangolin functionality. It includes OpenAPI documentation via Swagger UI.

## Enable Integration API

Update your Pangolin configuration file:

config.yml

```
flags:

  enable_integration_api: true
```

If you want to specify a port other than the default `3003`, you can do so in the config as well:

config.yml

```
server:

  integration_port: 3003 # Specify different port
```

## Configure Traefik Routing

Add the following configuration to your `config/traefik/dynamic_config.yml` to expose the Integration API at `https://api.example.com/v1`:

dynamic\_config.yml

```
routers:

    # Add the following two routers

    int-api-router-redirect:

      rule: "Host(\`api.example.com\`)"

      service: int-api-service

      entryPoints:

        - web

      middlewares:

        - redirect-to-https

    int-api-router:

      rule: "Host(\`api.example.com\`)"

      service: int-api-service

      entryPoints:

        - websecure

      tls:

        certResolver: letsencrypt

  services:

    # Add the following service

    int-api-service:

      loadBalancer:

        servers:

          - url: "http://pangolin:3003"
```

## Access Documentation

Once configured, access the Swagger UI documentation at:

```
https://api.example.com/v1/docs
```

![Swagger UI Preview](https://mintcdn.com/fossorial/u-2SUNWyK_LJL3sU/images/swagger.png?w=280&fit=max&auto=format&n=u-2SUNWyK_LJL3sU&q=85&s=163c4b68c9f9d9ee2589898f8af8fedf)

Swagger UI Preview

Swagger UI documentation interface

The Integration API will be accessible at `https://api.example.com/v1` for external applications.

Was this page helpful?