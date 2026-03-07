---
title: "GitHub OAuth"
source: "https://tinyauth.app/docs/guides/github-oauth"
author:
published:
created: 2025-12-29
description: "Use GitHub OAuth for authenticating to Tinyauth."
tags:
  - "clippings"
---
Guides

Use GitHub OAuth for authenticating to Tinyauth.

Tinyauth has built-in support for GitHub OAuth with just two environment variables. Most of the configuration happens on the GitHub side rather than Tinyauth.

## Requirements

- A domain name (non-gTLDs are supported)
- A GitHub account

## Creating the GitHub OAuth App

Begin by creating a GitHub OAuth app. Navigate to the [GitHub developer settings](https://github.com/settings/developers) and click **New OAuth App**. Fill in the following details:

![GitHub new OAuth app](https://tinyauth.app/assets/github-new-oauth-app-D9hMSz-B.png)

After entering the details, click **Register Application**.

## Retrieving Credentials

Once the application is created, the following screen will appear:

![GitHub OAuth app homepage](https://tinyauth.app/assets/github-oauth-app-homepage-x7Xf0XN0.png)

Note down the client ID. To generate the client secret, click **Generate a new client secret**. GitHub will prompt for login confirmation and then display the secret:

![GitHub OAuth Client Secret](https://tinyauth.app/assets/github-oauth-client-secret-Dgb3uWe5.png)

Note down the client ID and secret for later use.

## Configuring Tinyauth

Add the following environment variables to the Tinyauth Docker container:

```
services:

  tinyauth:

    environment:

      - PROVIDERS_GITHUB_CLIENT_ID=your-github-client-id

      - PROVIDERS_GITHUB_CLIENT_SECRET=your-github-secret
```

OAuth alone does not guarantee security. By default, any GitHub account can log in as a normal user. To restrict access, use the `OAUTH_WHITELIST` environment variable to allow specific email addresses. Refer to the [configuration](https://tinyauth.app/docs/reference/configuration) page for details.

Restart Tinyauth. Upon visiting the login screen, an additional option to log in with GitHub will appear.[GitHub Apps OAuth](https://tinyauth.app/docs/guides/github-app-oauth)

[

Use the GitHub Apps OAuth screen for authenticating to Tinyauth.

](https://tinyauth.app/docs/guides/github-app-oauth)[

Google OAuth

Use Google's OAuth screen to authenticate to Tinyauth.

](https://tinyauth.app/docs/guides/google-oauth)