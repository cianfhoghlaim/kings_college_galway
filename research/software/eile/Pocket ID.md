---
title: "Pocket ID"
source: "https://docs.pangolin.net/manage/identity-providers/pocket-id"
author:
  - "[[​]]"
published:
created: 2025-12-08
description: "Configure Pocket ID Single Sign-On using OpenID Connect"
tags:
  - "clippings"
---
The following steps will integrate Pocket ID with Pangolin SSO using OpenID Connect (OIDC).

## Prerequisites

Before you can start, you’ll need to have Pocket ID accessible and ensure it’s not secured with Pangolin SSO.

### Creating an OIDC Client in Pocket ID

In Pocket ID, create a new OIDC Client.

The callback URL is displayed in the IdP settings after you create the IdP in Pangolin.

After you have created the OIDC Client, take note of the following fields from the top of the page (click “Show more details” to see all of them):
- **Client ID**
- **Client secret**
- **Authorization URL**
- **Token URL**

## Configuring Identity Providers in Pangolin

In Pangolin, go to “Identity Providers” and click “Add Identity Provider”. Select the OAuth2/OIDC provider option.“Name” should be set to something memorable (eg. Pocket ID). The “Provider Type” should be set to the default `OAuth2/OIDC`.

### OAuth2/OIDC Configuration (Provider Credentials and Endpoints)

In the OAuth2/OIDC Configuration, you’ll need the following fields:Client ID

string

required

The Client ID from your Pocket ID OIDC client.Client Secret

string

required

The Client secret from your Pocket ID OIDC client.

Authorization URL

string

required

The Authorization URL from your Pocket ID OIDC client.Token URL

string

required

The Token URL from your Pocket ID OIDC client.

## Token Configuration

You should leave all of the paths default. In the “Scopes” field, add `openid profile email`.

Set the “Identifier Path” to `preferred_username` for Pocket ID integration.

When you’re done, click “Create Identity Provider”! Then, copy the Redirect URL in the “General” tab as you will now need this for your Pocket ID OIDC client.

## Returning to Pocket ID

Lastly, you’ll need to return to your Pocket ID OIDC client in order to add the redirect URI created by Pangolin. Add the URI to “Callback URLs”, then save your changes! Your configuration should now be complete. You’ll now need to add an external user to Pangolin, or if you have “Auto Provision Users” enabled, you can now log in using Pocket ID SSO.