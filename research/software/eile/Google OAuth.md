---
title: "Google OAuth"
source: "https://tinyauth.app/docs/guides/google-oauth"
author:
published:
created: 2025-12-29
description: "Use Google's OAuth screen to authenticate to Tinyauth."
tags:
  - "clippings"
---
Guides

Use Google's OAuth screen to authenticate to Tinyauth.

Tinyauth has built-in support for Google OAuth, making it straightforward to set up.

## Requirements

- A domain name (gTLDs required)
- A Google account

## Creating the Google OAuth App

To begin, create an app in the [Google Cloud Console](https://console.cloud.google.com/). Create a new project (a default project may already exist). After creating the project, the following screen should appear:

![Google Cloud Console Home](https://tinyauth.app/assets/google-cloud-home-Dicdx44B.png)

From the quick access menu, click **APIs & Services**, then select **OAuth consent screen** from the sidebar. Click the **Get Started** button in the middle of the screen.

Google has updated the OAuth section. This guide uses the new OAuth experience. If a button appears saying "Try the new OAuth experience," click it to match the steps in this guide.

After clicking the button, the following screen should appear:

![Configure OAuth Consent Screen](https://tinyauth.app/assets/google-cloud-oauth-configure-y3TS6-09.png)

- **App Name**: Use `Tinyauth`.
- **Support Email**: Select the available email address.
- **Audience**: Choose **External**.
- **Contact Information**: Enter an email address.
- Agree to the data use policy and click **Create**.

After some time, the OAuth homepage will appear:

![Google Cloud OAuth Home](https://tinyauth.app/assets/google-cloud-oauth-home-CWM-UiKB.png)

Click **Create OAuth Client**.

- **Application Type**: Select **Web Application**.
- **Name**: Optionally rename the client (default is `Web Client 1`).
- **Authorized Redirect URIs**: Add the domain with the `/api/oauth/callback/google` suffix, e.g., `https://tinyauth.example.com/api/oauth/callback/google`.

Click **Create**. Once the application is created, the following screen will appear:

![Google Cloud OAuth Clients](https://tinyauth.app/assets/google-cloud-oauth-created-Cmhg9N16.png)

Click the client (e.g., `Web Client 1`) and copy the Client ID and Client Secret from the Additional Information section.

## Configuring Tinyauth

Add the following environment variables to the Tinyauth Docker container:

```
services:

  tinyauth:

    environment:

      - PROVIDERS_GOOGLE_CLIENT_ID=your-google-client-id

      - PROVIDERS_GOOGLE_CLIENT_SECRET=your-google-secret
```

OAuth alone does not guarantee security. By default, any GitHub account can log in as a normal user. To restrict access, use the `OAUTH_WHITELIST` environment variable to allow specific email addresses. Refer to the [configuration](https://tinyauth.app/docs/reference/configuration) page for details.

Restart Tinyauth. Upon visiting the login screen, an additional option to log in with Google will appear.[GitHub OAuth](https://tinyauth.app/docs/guides/github-oauth)

[

Use GitHub OAuth for authenticating to Tinyauth.

](https://tinyauth.app/docs/guides/github-oauth)[

LDAP

Use a centralized LDAP server for user management in Tinyauth.

](https://tinyauth.app/docs/guides/ldap)

### On this page

[Requirements](https://tinyauth.app/docs/guides/#requirements) [Creating the Google OAuth App](https://tinyauth.app/docs/guides/#creating-the-google-oauth-app) [Configuring Tinyauth](https://tinyauth.app/docs/guides/#configuring-tinyauth)