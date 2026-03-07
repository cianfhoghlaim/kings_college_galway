---
title: "komodo_client::api - Rust"
source: "https://docs.rs/komodo_client/latest/komodo_client/api/index.html"
author:
published:
created: 2025-12-05
description: "Komodo Core API"
tags:
  - "clippings"
---
## Module api

## Module api

[Source](https://docs.rs/komodo_client/latest/src/komodo_client/api/mod.rs.html#1-74)

[Search](https://docs.rs/komodo_client/latest/komodo_client/api/index.html?search=)

Expand description

## Komodo Core API

Komodo Core exposes an HTTP api using standard JSON serialization.

All calls share some common HTTP params:

- Method: `POST`
- Path: `/auth`, `/user`, `/read`, `/write`, `/execute`
- Headers:
	- Content-Type: `application/json`
	- Authorization: `your_jwt`
	- X-Api-Key: `your_api_key`
	- X-Api-Secret: `your_api_secret`
	- Use either Authorization *or* X-Api-Key and X-Api-Secret to authenticate requests.
- Body: JSON specifying the request type (`type`) and the parameters (`params`).

You can create API keys for your user, or for a Service User with limited permissions, from the Komodo UI Settings page.

To call the api, construct JSON bodies following the schemas given in [read](https://docs.rs/komodo_client/latest/komodo_client/api/read/index.html "mod komodo_client::api::read"), [write](https://docs.rs/komodo_client/latest/komodo_client/api/write/index.html "mod komodo_client::api::write"), [execute](https://docs.rs/komodo_client/latest/komodo_client/api/execute/index.html "mod komodo_client::api::execute"), and so on.

For example, this is an example body for [read::GetDeployment](https://docs.rs/komodo_client/latest/komodo_client/api/read/struct.GetDeployment.html "struct komodo_client::api::read::GetDeployment"):

```json
{
  "type": "GetDeployment",
  "params": {
    "deployment": "66113df3abe32960b87018dd"
  }
}
```

The request’s parent module (eg. [read](https://docs.rs/komodo_client/latest/komodo_client/api/read/index.html "mod komodo_client::api::read"), [write](https://docs.rs/komodo_client/latest/komodo_client/api/write/index.html "mod komodo_client::api::write")) determines the http path which must be used for the requests. For example, requests under [read](https://docs.rs/komodo_client/latest/komodo_client/api/read/index.html "mod komodo_client::api::read") are made using http path `/read`.

### Curl Example

Putting it all together, here is an example `curl` for [write::UpdateBuild](https://docs.rs/komodo_client/latest/komodo_client/api/write/struct.UpdateBuild.html "struct komodo_client::api::write::UpdateBuild"), to update the version:

### Modules

- [auth](https://docs.rs/komodo_client/latest/komodo_client/api/auth/index.html "mod komodo_client::api::auth"): Requests relating to logging in / obtaining authentication tokens.
- [user](https://docs.rs/komodo_client/latest/komodo_client/api/user/index.html "mod komodo_client::api::user"): User self-management actions (manage api keys, etc.)
- [read](https://docs.rs/komodo_client/latest/komodo_client/api/read/index.html "mod komodo_client::api::read"): Read only requests which retrieve data from Komodo.
- [execute](https://docs.rs/komodo_client/latest/komodo_client/api/execute/index.html "mod komodo_client::api::execute"): Run actions on Komodo resources, eg [execute::RunBuild](https://docs.rs/komodo_client/latest/komodo_client/api/execute/struct.RunBuild.html "struct komodo_client::api::execute::RunBuild").
- [write](https://docs.rs/komodo_client/latest/komodo_client/api/write/index.html "mod komodo_client::api::write"): Requests which alter data, like create / update / delete resources.

### Errors

Request errors will be returned with a JSON body containing information about the error. They will have the following common format:

```json
{
  "error": "top level error message",
  "trace": [
    "first traceback message",
    "second traceback message"
  ]
}
```

## Modules

[auth](https://docs.rs/komodo_client/latest/komodo_client/api/auth/index.html "mod komodo_client::api::auth")

[execute](https://docs.rs/komodo_client/latest/komodo_client/api/execute/index.html "mod komodo_client::api::execute")

[read](https://docs.rs/komodo_client/latest/komodo_client/api/read/index.html "mod komodo_client::api::read")

[terminal](https://docs.rs/komodo_client/latest/komodo_client/api/terminal/index.html "mod komodo_client::api::terminal")

[user](https://docs.rs/komodo_client/latest/komodo_client/api/user/index.html "mod komodo_client::api::user")

[write](https://docs.rs/komodo_client/latest/komodo_client/api/write/index.html "mod komodo_client::api::write")