---
title: "Database Options"
source: "https://docs.pangolin.net/self-host/advanced/database-options"
author:
  - "[[Pangolin Docs]]"
published:
created: 2025-12-08
description: "Configure SQLite or PostgreSQL database for Pangolin"
tags:
  - "clippings"
---
Pangolin supports two database options: SQLite for simplicity and PostgreSQL for production deployments.

## SQLite (Default)

- No configuration required
- Easy to use and portable
- Built into the main image
- Perfect for development

## PostgreSQL

- Production-ready database
- Better performance at scale
- Requires separate image
- Advanced configuration options

## SQLite

By default, Pangolin uses SQLite for its ease of use and portability.**Docker Image**: `fosrl/pangolin:<version>`

No configuration is required to use SQLite with Pangolin.

## PostgreSQL

You can optionally use PostgreSQL for production deployments.**Docker Image**: `fosrl/pangolin:postgresql-<version>`

### Configuration

Add the following section to your Pangolin configuration file:

config.yml

```
postgres:

  connection_string: postgresql://<user>:<password>@<host>:<port>/<database>
```

Replace the placeholders with your actual PostgreSQL connection details.

### Docker Compose Example

This example sets up PostgreSQL with health checks to ensure the database is ready before Pangolin starts:

docker-compose.yml

```
name: pangolin

services:

  pangolin:

    image: fosrl/pangolin:postgresql-latest # Don't use latest in production

    container_name: pangolin

    restart: unless-stopped

    depends_on:

      postgres:

        condition: service_healthy

    volumes:

      - ./config:/app/config

    healthcheck:

      test: ["CMD", "curl", "-f", "http://localhost:3001/api/v1/"]

      interval: "10s"

      timeout: "10s"

      retries: 15

  # ... other services ...

  postgres:

    image: postgres:17

    container_name: postgres

    restart: unless-stopped

    environment:

      POSTGRES_USER: postgres

      POSTGRES_PASSWORD: postgres

    volumes:

      - ./config/postgres:/var/lib/postgresql/data

    healthcheck:

      test: ["CMD-SHELL", "pg_isready -U postgres"]

      interval: 10s

      timeout: 5s

      retries: 5
```

This example is not necessarily production-ready. Adjust the configuration according to your needs and security requirements.

Do not use `latest` tags in production. Use specific version tags for stability.[Internal CLI (pangctl)](https://docs.pangolin.net/self-host/advanced/container-cli-tool)

[

Previous

](https://docs.pangolin.net/self-host/advanced/container-cli-tool)[

Enable Integration API

Next

](https://docs.pangolin.net/self-host/advanced/integration-api)