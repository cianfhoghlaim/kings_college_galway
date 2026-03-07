---
title: "Integration API"
source: "https://docs.pangolin.net/manage/integration-api"
author:
  - "[[Pangolin Docs]]"
published:
created: 2025-12-05
description: "Learn how to use Pangolin's REST API to automate and script operations with fine-grained permissions"
tags:
  - "clippings"
---
[Skip to main content](https://docs.pangolin.net/manage/#content-area)

The API is REST-based and supports many operations available through the web interface. Authentication uses Bearer tokens, and you can create multiple API keys with specific permissions for different use cases.

For Pangolin Community Edition, the integration API must be enabled. Check out [the documentation](https://docs.pangolin.net/self-host/advanced/integration-api) for how to enable the integration API.

## Authentication

All API requests require authentication using a Bearer token in the Authorization header:

## API Key Types

Pangolin supports two types of API keys with different permission levels:

### Organization API Keys

Organization API keys are created by organization admins and have limited scope to perform actions only in that organization.

### Root API Keys

Root API keys have some extra permissions and can execute operations across orgs. They are only available in the Community Edition of Pangolin:

Root API keys have elevated permissions and should be used carefully. Only create them when you need server-wide access.

## Creating API Keys

## API Documentation

View the Swagger docs here: [https://api.pangolin.net/v1/docs](https://api.pangolin.net/v1/docs).Interactive API documentation is available through Swagger UI:

![Swagger Docs](https://mintcdn.com/fossorial/u-2SUNWyK_LJL3sU/images/swagger.png?w=280&fit=max&auto=format&n=u-2SUNWyK_LJL3sU&q=85&s=163c4b68c9f9d9ee2589898f8af8fedf)

Swagger Docs

Swagger UI showing API endpoints and interactive testing

For self-hosted Pangolin, access the documentation at `https://api.your-domain.com/v1/docs`.

Was this page helpful?[Domains](https://docs.pangolin.net/manage/domains)

[

Previous

](https://docs.pangolin.net/manage/domains)[

Branding

Next

](https://docs.pangolin.net/manage/branding)