---
title: "Telemetry"
source: "https://docs.pangolin.net/self-host/telemetry"
author:
  - "[[Pangolin Docs]]"
published:
created: 2025-12-08
description: "Understanding Pangolin's anonymous usage data collection"
tags:
  - "clippings"
---
[Skip to main content](https://docs.pangolin.net/self-host/#content-area)

Pangolin collects anonymous usage telemetry to help us understand how the software is used and guide future improvements and feature development.

## What We Collect

The telemetry system collects **anonymous, aggregated data** about your Pangolin deployment. For example:
- **System metrics**: Number of sites, users, resources, and clients
- **Usage patterns**: Resource types, protocols, and SSO configurations
- **Performance data**: Site traffic volumes and online status
- **Deployment info**: App version and installation timestamp

## Privacy & Anonymity

**No personal information is ever collected or transmitted.** All data is:
- **Anonymized**: Identifying info is hashed using SHA-256
- **Non-identifying**: Cannot be used to identify specific users or organizations

## Configuration

You can control telemetry collection in your `config.yml`:

```
app:

  telemetry:

    anonymous_usage: true  # Set to false to disable
```

## What This Helps

Anonymous usage data helps us:
- Identify popular features and usage patterns
- Prioritize development efforts
- Improve performance and reliability
- Make Pangolin better for everyone
If you have concerns about telemetry collection, you can disable it entirely by setting `anonymous_usage: false` in your configuration.

Was this page helpful?