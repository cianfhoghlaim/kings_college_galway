---
title: "High-Availability Kubernetes on Hetzner with Talos 1.11"
source: "https://blog.icod.de/2025/12/13/high-availability-kubernetes-on-hetzner-with-talos-1-11/"
author:
  - "[[Darko Luketic]]"
published: 2025-12-13
created: 2025-12-21
description: "Stop Overpaying for Cloud: High-Availability Kubernetes on Hetzner with Talos 1.11 If you are running production workloads like Mastodon, Odoo, or a fleet of WordPress sites, you might think you need to stick with the major hyperscalers. However, you don’t need to burn money on AWS or Google Cloud just to get reliability. In fact,…"
tags:
  - "clippings"
---
## Stop Overpaying for Cloud: High-Availability Kubernetes on Hetzner with Talos 1.11

If you are running production workloads like Mastodon, Odoo, or a fleet of WordPress sites, you might think you need to stick with the major hyperscalers. **However**, you don’t need to burn money on AWS or Google Cloud just to get reliability. **In fact**, you can build a **massive**, enterprise-grade Kubernetes cluster on Hetzner Cloud for a fraction of the cost.

**Specifically**, in this guide, we will deploy three different architectures using **Talos Linux 1.11** and **Cilium** (managed via OpenTofu). **Whether** you need a beast of a cluster for databases, a cost-effective setup for static sites, **or** raw dedicated metal for heavy computation, we have you covered.

**[Grab your €20 Free Credit on Hetzner Cloud here](https://hetzner.cloud/?ref=p8DSeX0ZVH63)** to follow along.

## Option 1: The “Business Powerhouse” (3x CX53 Converged)

***Best for:** Production Apps, Mastodon Instances, Odoo ERP, High-Traffic WordPress Networks.*

**First and foremost**, this is the balanced production setup. We are using the **CX53** nodes, which are absolute monsters for the price. **By** running a “Converged” setup (where every node is both a control plane and a worker), we get high availability (HA). **Consequently**, we can use every ounce of RAM for our applications without wasting resources on idle management nodes.

### The Hardware Specs

- **Nodes:** 3x **CX53** (16 vCPU, 32GB RAM, 320GB NVMe)
- **Networking:** 1x Load Balancer (LB11) for the API **and** 1x Failover IP for the Gateway.
- **Storage:** Hetzner Object Storage (S3 Compatible) for media assets.

### Monthly Cost Breakdown

- 3x CX53 Nodes (@ €17.49/mo): **€52.47**
- 1x Load Balancer (LB11): **€5.39**
- 1x Floating IP (IPv4) for Gateway: **€3.60**
- 1x Object Storage (1TB included): **~€5.00**
- **TOTAL: ~€66.46 / month**

**As a result**, for roughly **€66 a month**, you are getting **48 vCPUs and 96GB of RAM**. **In comparison**, if you tried getting that on AWS, you would be paying over €400.

[Sign up now to get your €20 credit and build this beast.](https://hetzner.cloud/?ref=p8DSeX0ZVH63)

---

## Option 2: The “Indie Hacker” (1 CP + 3 Workers)

***Best for:** Single Page Apps (SPA), Static Sites, Dev Environments, Low-Traffic APIs.*

**Alternatively**, if you don’t need HA for the control plane and just want a cheap place to host React/Vue apps or static sites, this tiered setup offers unbeatable value. **Here**, we use one node to manage the cluster **while** three nodes handle the actual work.

### The Hardware Specs & Cost

- **Control Plane:** 1x **CX23** (2 vCPU, 4GB RAM)
- **Workers:** 3x **CX23** (2 vCPU, 4GB RAM)
- **Networking:** Direct ingress (Point DNS to a worker or use a Floating IP).
- **Total Cost:****~€17.56 / month** (Less than the price of Netflix!)

---

## Option 3: The “Nuclear Option” (3x Dedicated AX41-NVMe)

***Best for:** Video Encoding, Big Data, Machine Learning, Heavy Database Loads.*

If you want the absolute best price-to-performance ratio and don’t mind getting your hands dirty, the **Hetzner Robot (Dedicated)** line is unbeatable. **Specifically**, ordering **3x AX41-NVMe** servers gives you a cluster that outperforms most startups’ entire infrastructure.

### The Specs (Per Node)

- **CPU:** AMD Ryzen 5 3600 (Hexa-Core)
- **RAM:** 64 GB DDR4
- **Disk:** 2x 512 GB NVMe (Configured in **RAID 1** for redundancy)
- **Price:****€37.30 / month** (excl. VAT)

### The “Software RAID” Challenge

There is a catch. The AX41 uses **Software RAID (mdraid)**. **Unfortunately**, Talos Linux does not currently support booting from software RAID arrays. It demands full ownership of the disk, which means you would have to disable RAID and lose your drive redundancy—not acceptable for production.

### The Solution: Flatcar Container Linux

The best alternative for this setup is **Flatcar Container Linux**. It is immutable (like Talos), auto-updating, and native to Kubernetes, but it fully supports software RAID via Ignition configs.

#### Coming Soon: The “Flatcar on Metal” Deep Dive

Setting up Flatcar on bare metal with custom RAID layouts requires writing advanced **Butane/Ignition** configurations. This is too complex to cover in this single post.

**I am currently writing a dedicated guide** specifically on how to bootstrap a Flatcar Kubernetes cluster on Hetzner Dedicated Servers.  
*Stay tuned—this post will be linked here as soon as it is live!*

### The “Right Now” Workaround: Proxmox

If you need this hardware **today**, the easiest path is to install **Proxmox VE** (which handles the RAID) and run Talos inside VMs. This gives you the best of both worlds: hardware redundancy managed by Proxmox, and the immutable Talos OS managing your Kubernetes workloads.

---

## Infrastructure as Code: OpenTofu & Cilium

**For deployment**, we use **OpenTofu** (the open-source fork of Terraform) to provision the infrastructure. **Additionally**, we will enable the **Gateway API** feature in Cilium 1.18+ to handle traffic routing efficiently.

**Why use the “Debian 12” Image?** Hetzner Cloud does not have a native API image for Talos Linux.  
**Therefore**, we use a clever “Bootstrap” strategy: we provision a standard Debian 12 server and use a

| 1 | user\_data |
| --- | --- |

script to wipe the disk and install Talos automatically on the very first boot.

### main.tf (Converged CX53 Example)

| 1  2  3  4  5  6  7  8  9  10  11  12  13  14  15  16  17  18  19  20  21  22  23  24  25  26  27  28  29  30  31  32  33  34  35  36  37  38  39  40  41  42  43  44  45  46  47  48  49  50  51  52  53  54  55  56  57  58 | terraform {  required\_providers {  hcloud \= { source \= "hetznercloud/hcloud",version \= "~> 1.45" }  }  }  variable "hcloud\_token" { sensitive \= true }  provider "hcloud" { token \= var.hcloud \_ token }  \# Private Network  resource "hcloud\_network" "talos\_net" {  name \= "talos-net"  ip\_range \= "10.0.0.0/16"  }  resource "hcloud\_network\_subnet" "talos\_subnet" {  network\_id \= hcloud\_network.talos\_net.id  type \= "cloud"  network\_zone \= "nbg1"  ip\_range \= "10.0.1.0/24"  }  \# Load Balancer for Control Plane (API)  resource "hcloud\_load\_balancer" "cp\_lb" {  name \= "k8s-api"  load\_balancer\_type \= "lb11"  location \= "nbg1"  }  \# The Powerhouse Nodes (CX53)  resource "hcloud\_server" "node" {  count \= 3  name \= "talos-cx53-${count.index + 1}"  server\_type \= "cx53"  image \= "debian-12" \# Bootstrapping OS  location \= "nbg1"  \# This script wipes Debian and installs Talos 1.11  user\_data \= << EOF  #cloud-config  runcmd:  \- apt \- get update && apt \- get install \- y zstd  \- wget \- O / tmp / talos.raw.zst https://github.com/siderolabs/talos/releases/download/v1.11.5/metal-amd64.raw.zst  \- zstd \- d \- c / tmp / talos.raw.zst \| dd of \= / dev / sda && sync  \- reboot  EOF  network {  network\_id \= hcloud\_network.talos\_net.id  ip \= "10.0.1.1${count.index}"  }  }  \# Floating IP for Cilium Gateway  resource "hcloud\_floating\_ip" "gateway\_ip" {  type \= "ipv4"  home\_location \= "nbg1"  } |
| --- | --- | --- |

### Setting up Cilium & Gateway API

**Once** Talos 1.11 is bootstrapped, you should install Cilium with Gateway API enabled. **This step** effectively replaces the legacy Ingress Controller.

| 1  2  3  4  5 | helm install cilium cilium / cilium \-- version 1.18.4 \\  \-- namespace kube \- system \\  \-- set gatewayAPI.enabled \= true \\  \-- set kubeProxyReplacement \= true \\  \-- set hcloud.enabled \= true |
| --- | --- |

**Finally**, you then configure the **Hetzner Cloud Controller Manager** to bind your Floating IP to the Cilium Gateway LoadBalancer service. **This ensures** that if one of your massive CX53 nodes reboots, traffic instantly shifts to another node without downtime.

---

## Storage Strategy: Keeping Data Safe

You might be wondering: *“If a node dies and my database moves to another server, what happens to my data?”*

If you rely on the local NVMe disk, that data is gone. **However**, Hetzner has a native solution called **Hetzner Cloud Volumes** that we automate using the **Container Storage Interface (CSI)**.

**Cost Note:** Cloud Volumes (Block Storage) cost extra, but they are cheap.  
You pay roughly **€0.044 per GB/month**. A 10GB volume for your database will only cost you about **€0.44/month**.

### 1\. Block Storage (Databases & PVCs)

For applications like PostgreSQL, MySQL, or WordPress uploads, we use the **Hetzner CSI Driver**. This allows Kubernetes to provision persistent volumes that live **outside** your servers.

**How it works:** If Node A fails, Kubernetes sees that your database pod is down. It automatically **detaches** the storage volume from Node A and **reattaches** it to Node B before starting the pod there. Your data survives the crash intact.

### 2\. Object Storage (Media & Backups)

For “unstructured” data like Mastodon media files, user avatars, or Nextcloud backups, do not use Block Storage. It is expensive and hard to resize.

**Instead**, use **Hetzner Object Storage** (S3 Compatible). It is dirt cheap (~€5/TB), infinitely scalable, and accessible from any node instantly without waiting for volumes to mount/unmount.

---

## Disaster Recovery: What Happens When a Server Dies?

Hardware failures happen. **Therefore**, it is crucial to understand how the cluster behaves.

**If ANY Node fails in the 3-Node Setup:** It is a non-event. Because we run 3 Control Plane nodes, the cluster maintains “quorum” (2 out of 3 votes). The Hetzner Load Balancer instantly detects the dead node and stops sending API traffic to it. Your apps get rescheduled to the surviving 2 nodes automatically. You likely won’t even notice until you check your alerts.

### How to Fix a Broken Node (The “GitOps” Way)

Since we are using OpenTofu (Terraform), we don’t fix servers; we replace them. If

| 1 | talos \- cx53 \- 2 |
| --- | --- |

dies, you simply tell OpenTofu to destroy and recreate it. Talos will automatically boot on the fresh server and rejoin the cluster.

| 1  2  3  4  5 | \# 1. Mark the broken server for recreation  tofu taint "hcloud\_server.node\[1\]" \# This targets the 2nd node (index starts at 0)  \# 2. Apply the changes  tofu apply |
| --- | --- |

---

## Conclusion

**To summarize**, Hetzner Cloud combined with Talos Linux is a cheat code for infrastructure. You get the performance of bare metal with the flexibility of the cloud, **and** all at prices that make the hyperscalers look ridiculous.

Ready to deploy? **Then** don’t forget to claim your startup credits below.

[  
Get €20 Cloud Credits & Start Building  
](https://hetzner.cloud/?ref=p8DSeX0ZVH63)

(Valid for all Hetzner Cloud products)