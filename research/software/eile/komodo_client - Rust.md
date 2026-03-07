---
title: "komodo_client - Rust"
source: "https://docs.rs/komodo_client/latest/komodo_client/"
author:
published:
created: 2025-12-20
description: "Komodo"
tags:
  - "clippings"
---
## Crate komodo\_client

## Crate komodo\_client

[Source](https://docs.rs/komodo_client/latest/src/komodo_client/lib.rs.html#1-213)

[Search](https://docs.rs/komodo_client/latest/komodo_client/?search=)

Expand description

## Komodo

*A system to build and deploy software across many servers*. [**https://komo.do**](https://komo.do/)

This is a client library for the Komodo Core API. It contains:

- Definitions for the application [api](https://docs.rs/komodo_client/latest/komodo_client/api/index.html "mod komodo_client::api") and [entities](https://docs.rs/komodo_client/latest/komodo_client/entities/index.html "mod komodo_client::entities").
- A [client](https://docs.rs/komodo_client/latest/komodo_client/struct.KomodoClient.html "struct komodo_client::KomodoClient") to interact with the Komodo Core API.
- Information on configuring Komodo [Core](https://docs.rs/komodo_client/latest/komodo_client/entities/config/core/index.html "mod komodo_client::entities::config::core") and [Periphery](https://docs.rs/komodo_client/latest/komodo_client/entities/config/periphery/index.html "mod komodo_client::entities::config::periphery").

### Client Configuration

The client includes a convenenience method to parse the Komodo API url and credentials from the environment:

- `KOMODO_ADDRESS`
- `KOMODO_API_KEY`
- `KOMODO_API_SECRET`

### Client Example

```
dotenvy::dotenv().ok();

let client = KomodoClient::new_from_env()?;

// Get all the deployments
let deployments = client.read(ListDeployments::default()).await?;

println!("{deployments:#?}");

let update = client.execute(RunBuild { build: "test-build".to_string() }).await?:
```

## Modules

[api](https://docs.rs/komodo_client/latest/komodo_client/api/index.html "mod komodo_client::api")

Komodo Core API

[busy](https://docs.rs/komodo_client/latest/komodo_client/busy/index.html "mod komodo_client::busy")

[deserializers](https://docs.rs/komodo_client/latest/komodo_client/deserializers/index.html "mod komodo_client::deserializers")

Deserializers for custom behavior and backward compatibility.

[entities](https://docs.rs/komodo_client/latest/komodo_client/entities/index.html "mod komodo_client::entities")

[parsers](https://docs.rs/komodo_client/latest/komodo_client/parsers/index.html "mod komodo_client::parsers")

[terminal](https://docs.rs/komodo_client/latest/komodo_client/terminal/index.html "mod komodo_client::terminal")

[ws](https://docs.rs/komodo_client/latest/komodo_client/ws/index.html "mod komodo_client::ws")

## Structs

[Komodo  Client](https://docs.rs/komodo_client/latest/komodo_client/struct.KomodoClient.html "struct komodo_client::KomodoClient")

Client to interface with [Komodo](https://komo.do/docs/api#rust-client)

[Komodo  Env](https://docs.rs/komodo_client/latest/komodo_client/struct.KomodoEnv.html "struct komodo_client::KomodoEnv")

Default environment variables for the [KomodoClient](https://docs.rs/komodo_client/latest/komodo_client/struct.KomodoClient.html "struct komodo_client::KomodoClient").

## Functions

[komodo\_  client](https://docs.rs/komodo_client/latest/komodo_client/fn.komodo_client.html "fn komodo_client::komodo_client")

&’static KomodoClient initialized from environment.