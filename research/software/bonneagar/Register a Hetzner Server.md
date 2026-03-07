---
title: "Register a Hetzner Server"
source: "https://docs.siderolabs.com/omni/omni-cluster-setup/registering-machines/register-a-hetzner-server"
author:
  - "[[Authentication and Authorization]]"
published:
created: 2025-12-20
description:
tags:
  - "clippings"
---
[Skip to main content](https://docs.siderolabs.com/omni/omni-cluster-setup/registering-machines/#content-area)

Upon logging in you will be presented with the Omni dashboard.

First, download the Hetzner image from the Omni portal by clicking on the “Download Installation Media” button. Now, click on the “Options” dropdown menu and search for the “Hetzner” option. Notice there are two options: one for `amd64` and another for `arm64`. Select the appropriate option for the machine you are registering. Now, click the “Download” button.

- Packer

Place the following in the same directory as the downloaded installation media and name the file `hcloud.pkr.hcl`:Copy Now, run the following:Take note of the image ID produced by running this command.

**Warning**

Machines must be able to egress to your account’s WireGuard port and port 443.

Navigate to the “Machines” menu in the sidebar. You should now see a machine listed.You now have a Hetzner server registered with Omni and ready to provision.

Was this page helpful?

[Register a GCP Instance](https://docs.siderolabs.com/omni/omni-cluster-setup/registering-machines/register-a-gcp-instance) [Join machines to Omni](https://docs.siderolabs.com/omni/omni-cluster-setup/registering-machines/join-machines-to-omni)