---
title: "Get started with a 1Password Connect server | 1Password Developer"
source: "https://developer.1password.com/docs/connect/get-started/"
author:
published:
created: 2025-12-08
description: "Learn how to set up and use 1Password Connect to secure, orchestrate, and manage your company's infrastructure secrets."
tags:
  - "clippings"
---
1Password Connect servers are a type of [Secrets Automation workflow](https://developer.1password.com/docs/secrets-automation/) that allows you to securely access your 1Password items and vaults in your company's apps and cloud infrastructure.

![](https://www.youtube.com/watch?v=PMwxZxZT2Pc)

## Requirements

Before you can create a 1Password Secrets Automation workflow as a Connect server, make sure you complete the prerequisite tasks. The tasks vary depending on how you plan to deploy.

## Deployment

Use the following instructions to deploy a 1Password Connect Server.

### Step 1: Create a Secrets Automation workflow

You can create a Connect server Secrets Automation workflow through the 1Password.com dashboard or 1Password CLI. Following these instructions creates:

- A `1password-credentials.json` file. It contains the credentials necessary to deploy 1Password Connect Server.
- An access token. Use this in your applications or services to authenticate with the [Connect REST API](https://developer.1password.com/docs/connect/api-reference/). You can [issue additional tokens later](https://developer.1password.com/docs/connect/manage-connect#create-a-token).

### Step 2: Deploy a 1Password Connect Server

### Step 3: Set up applications and services to get information from 1Password

Applications and services get information from 1Password through REST API requests to a Connect server. The requests are authenticated with an access token. [Create a new token](https://developer.1password.com/docs/connect/manage-connect#create-a-token) for each application or service you use.

If your language or platform isn't listed, you can [build your own client using the 1Password Connect Server REST API](https://developer.1password.com/docs/connect/api-reference/).

You can also [use 1Password CLI](https://developer.1password.com/docs/connect/cli/) with your Connect server to provision secrets and retrieve item information on the command line.

## Get help

To change the vaults a token has access to, [issue a new token](https://developer.1password.com/docs/connect/manage-connect#create-a-token).

To get help and share feedback, join the discussion with the .