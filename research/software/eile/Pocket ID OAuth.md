---
title: "Pocket ID OAuth"
source: "https://tinyauth.app/docs/guides/pocket-id/"
author:
published:
created: 2025-12-29
description: "Use Pocket ID as an OAuth provider in Tinyauth."
tags:
  - "clippings"
---
Guides

Use Pocket ID as an OAuth provider in Tinyauth.

[Pocket ID](https://pocket-id.org/) is a popular OIDC server that enables login to apps with passkeys. Most proxies do not support OIDC/OAuth servers for authentication, meaning Pocket ID cannot be connected with them. With Tinyauth, Pocket ID can be integrated with proxies to secure apps.

## Requirements

A working Pocket ID installation is required. Refer to Pocket ID's [documentation](https://pocket-id.org/docs/setup/installation) for installation instructions.

## Configuring Pocket ID

Begin by accessing Pocket ID's admin dashboard:

![Pocket ID Admin Page](https://tinyauth.app/assets/pocket-id-home-DOztcnkK.png)

Navigate to the **OIDC Clients** tab and click **Add OIDC Client**. Provide the following details:

| Name | Value |
| --- | --- |
| Name | Assign a name to the client, such as `Tinyauth`. |
| Callback URLs | Enter the Tinyauth app URL followed by `/api/oauth/callback/pocketid`. For example: `https://tinyauth.example.com/api/oauth/callback/pocketid`. |

![Pocket ID Create Client](https://tinyauth.app/assets/pocket-id-new-client-B5IY6qhg.png)

Optionally, upload a logo for the OIDC client. The Tinyauth logo is available on [GitHub](https://github.com/steveiliop56/tinyauth/blob/main/assets/logo.png).

Click **Save**. A new page will display the OIDC credentials:

![Pocket ID Client Page](https://tinyauth.app/assets/pocket-id-client-page-DFFF-SUD.png)

Note down the client ID and secret for later use.

## Configuring Tinyauth

To integrate Tinyauth with Pocket ID, add the following environment variables to the Tinyauth Docker container:

```
services:

  tinyauth:

    environment:

      - PROVIDERS_POCKETID_CLIENT_ID=your-pocket-id-client-id

      - PROVIDERS_POCKETID_CLIENT_SECRET=your-pocket-id-client-secret

      - PROVIDERS_POCKETID_AUTH_URL=https://pocket-id.example.com/authorize

      - PROVIDERS_POCKETID_TOKEN_URL=https://pocket-id.example.com/api/oidc/token

      - PROVIDERS_POCKETID_USER_INFO_URL=https://pocket-id.example.com/api/oidc/userinfo

      - PROVIDERS_POCKETID_REDIRECT_URL=https://tinyauth.example.com/api/oauth/callback/pocketid

      - PROVIDERS_POCKETID_SCOPES=openid email profile groups

      - PROVIDERS_POCKETID_NAME=Pocket ID
```

OAuth alone does not guarantee security. By default, any Pocket ID account can log in as a normal user. To restrict access, use the `OAUTH_WHITELIST` environment variable to allow specific email addresses. Refer to the [configuration](https://tinyauth.app/docs/reference/configuration) page for details.

Restart Tinyauth to apply the changes. The login screen will now include an option to log in with Pocket ID.

## Access Controls with Pocket ID Groups

Pocket ID supports user groups, which can simplify access control management. To use groups, create one by navigating to the **User Groups** tab and clicking **Add Group**. Assign a name and save the group:

![Pocket ID New Group](https://tinyauth.app/assets/pocket-id-new-group-BIC3yRTT.png)

Select users to include in the group:

![Pocket ID Group Home](https://tinyauth.app/assets/pocket-id-group-home-Cy5VJ1ta.png)

Configure Tinyauth-protected apps to require OAuth groups by adding the `oauth.groups` label:

```
tinyauth.apps.myapp.oauth.groups: admins
```

In this example, only Pocket ID users in the `admins` group can access the app. Users outside the group will be redirected to an unauthorized page.

By default, Tinyauth uses the subdomain name of the request to find a matching container for labels. For example, a request to `myapp.example.com` checks for labels in the container named `myapp`. This behavior can be modified using the `tinyauth.apps.[app].config.domain` label. Refer to the [access controls](https://tinyauth.app/docs/guides/pocket-id/access-controls.md#label-discovery) guide for more information.[Nginx Proxy Manager](https://tinyauth.app/docs/guides/nginx-proxy-manager)

[

Use Tinyauth with the Nginx Proxy Manager reverse proxy.

](https://tinyauth.app/docs/guides/nginx-proxy-manager)[

Runtipi

Use Tinyauth with the Runtipi homeserver management platform.

](https://tinyauth.app/docs/guides/runtipi)

### On this page

[Requirements](https://tinyauth.app/docs/guides/pocket-id/#requirements) [Configuring Pocket ID](https://tinyauth.app/docs/guides/pocket-id/#configuring-pocket-id) [Configuring Tinyauth](https://tinyauth.app/docs/guides/pocket-id/#configuring-tinyauth) [Access Controls with Pocket ID Groups](https://tinyauth.app/docs/guides/pocket-id/#access-controls-with-pocket-id-groups)