---
title: "Provision Resources on Hetzner Cloud with Pulumi"
source: "https://blog.rasc.ch/2025/07/pulumihetzner.html"
author:
published:
created: 2025-12-21
description:
tags:
  - "clippings"
---
In previous blog posts ([here](https://blog.rasc.ch/2021/11/pulumi.html), [here](https://blog.rasc.ch/2021/12/go-lambda.html), [here](https://blog.rasc.ch/2024/09/python_lambda.html),[here](https://blog.rasc.ch/2022/01/aws-backend-1.html), [here](https://blog.rasc.ch/2022/01/aws-backend-2.html), [here](https://blog.rasc.ch/2024/10/awsbatchgpu.html), [here](https://blog.rasc.ch/2022/01/sqs-protobuf.html)) I showed you how to provision resources on AWS with [Pulumi](https://www.pulumi.com/). But Pulumi and Terraform are not limited to just the big three cloud providers: AWS, Azure, and GCP. You can use both tools to provision resources on many different cloud providers. You can find a list of all Pulumi packages [here](https://www.pulumi.com/registry/).

In this blog post, I will show you how to use Pulumi to provision resources on [Hetzner Cloud](https://www.hetzner.com/de/cloud). Hetzner is a German cloud provider that offers virtual machines, storage, and networking resources.

To make this example more interesting, I will show you how to use Pulumi to provision a [Wireguard](https://www.wireguard.com/) VPN server.

## Prerequisites

To follow this blog post, you need to have the following prerequisites:

- A Hetzner Cloud account. You can sign up for an account [here](https://www.hetzner.com/cloud).
- Pulumi installed. You can find the installation instructions [here](https://www.pulumi.com/docs/get-started/install/).

I will write the Pulumi code in TypeScript, so you also need to have [Node.js](https://nodejs.org/en/download/) installed. Pulumi supports many programming languages, including Python, Go, C#, and Java, and you can use any of them to write your Pulumi code. The concepts can easily be transferred to other languages, so you can use your preferred language.

Pulumi accesses the Hetzner Cloud via their [API](https://docs.hetzner.cloud/). For this reason, you need to create an API token in your Hetzner Cloud account. In the menu on the left side of the Hetzner Cloud console, click on "Security" and on the screen that opens, click on the tab "API tokens". Create a new API token by clicking on the button "Generate API token". Make sure that the API token has the Read and Write permission.

## Initialize Pulumi project

Create a new directory for your Pulumi project and navigate into it. Initialize a new Pulumi project with the following command:

```
pulumi new typescript
```

Enter the name of your project, a description, the name of the stack, and a password for encrypting the secrets.

Install the Pulumi Hetzner package by running the following command:

```
npm install @pulumi/hcloud
```

Next, we will add the Hetzner API token to the Pulumi configuration. Run the following command:

```
pulumi config set --secret hcloud:token <your-hetzner-api-token>
```

The name `hcloud:token` here is important because the Pulumi Hetzner package expects the API token to be stored under this name in the configuration. If you don't want to store the token in the Pulumi configuration, you can set the environment variable `HCLOUD_TOKEN` instead.

Now we are ready to provision our first resource on Hetzner Cloud.

## Provision Virtual Server

Open the file `index.ts` in an editor and replace its content with the following code:

```
import * as hcloud from "@pulumi/hcloud";

new hcloud.Server("server", {

  serverType: "cx22",

  image: "ubuntu-24.04"

});
```

This code will provision a new virtual server with the type `CX22` and the image `ubuntu-24.04`. CX22 is currently the smallest and least expensive server type on Hetzner Cloud. To provision the server, run the following command:

```
pulumi up
```

In the Hetzner Cloud console, you can now see the new server under the "Servers" section. To not unnecessarily incur costs, delete the server by running the following command:

```
pulumi destroy
```

The Pulumi Hetzner package offers many more resources that you can provision, such as volumes, networks, firewalls, and load balancers. You can find the documentation for the package [here](https://www.pulumi.com/registry/packages/hcloud/).

## Installing software

Pulumi and Terraform are used for provisioning resources; they are not used for installing software on the provisioned resources. Fortunately, most cloud providers offer a way to run scripts on the provisioned resources after they have been created. The standard for this is the [cloud-init](https://cloudinit.readthedocs.io/en/latest/) tool. cloud-init scripts are written in YAML and can be used to install software, configure the server, and run commands.

All images provided by Hetzner Cloud support cloud-init, so we can use it to run scripts on the provisioned server. To pass the cloud-init script to the server, we use the `userData` property of the `hcloud.Server` resource.

Here is an example that installs nginx on the server:

```
import * as hcloud from "@pulumi/hcloud";

new hcloud.Server("server", {

  serverType: "cx22",

  image: "ubuntu-24.04",

  userData: \`#cloud-config

package_update: true

packages:

  - nginx

write_files:

  - path: /var/www/html/index.html

    content: |

      hello world

runcmd:

  - [ systemctl, enable, --now, nginx ]

\`

});
```

After running `pulumi up`, the server will be provisioned, and nginx will be installed and started. You should see the message "hello world" when you access the server's IP address in your web browser. You need to wait a few seconds after the server has been provisioned before you can access it.

## Provision Wireguard VPN Server

Next, we will provision a Wireguard VPN server. For this Pulumi script, I installed two additional libraries into the project:

```
npm install @pulumi/command

npm install @pulumi/local
```

The `@pulumi/command` package will be used for running a remote command, and the local package is used for reading a file into the Pulumi script.

First, we need to generate a Wireguard key pair for the client. Check out this article on how to generate key pairs: [Wireguard Key Generation](https://www.wireguard.com/quickstart/#key-generation).

The private key stays on the client device, while the public key will be sent to the server. So we add it to the Pulumi configuration; then, we can transfer it to the server via the `userData` property.

```
pulumi config set wireguardClientPublicKey kr7lX... --secret
```

In addition, we also set a few other configuration values that we will use later in the script. The Wireguard server's private IP range, the Wireguard listen port, and the region where the server will be provisioned. In this case, I will use the `hel1` region in Helsinki, Finland. Finally, set the Wireguard client's IP address.

```
pulumi config set wireguardServerPrivateIp 10.0.0.1/24

pulumi config set wireguardServerListenPort "51820"

pulumi config set serverRegion hel1

pulumi config set wireguardClientIp 10.0.0.3/32
```

The cloud-init file used in this example installs and configures WireGuard and UFW. It generates a WireGuard key pair for the server, sets up network forwarding, configures firewall rules for secure VPN operation, and writes a basic WireGuard server configuration. You can find the complete cloud-init file [here](https://github.com/ralscha/blog2022/blob/master/pulumi-hetzner/wireguard.yaml).

Now let's go through the Pulumi code step by step.

First, the code imports the necessary packages and modules and retrieves the configuration values and secrets from the Pulumi configuration:

```
import * as pulumi from "@pulumi/pulumi";

import * as hcloud from "@pulumi/hcloud";

import * as local from "@pulumi/local";

import * as command from "@pulumi/command";

import * as path from "path";

import * as forge from "node-forge";

// 1. Retrieve configuration values and secrets

const config = new pulumi.Config();

const serverRegion = config.require("serverRegion");

const wireguardServerPrivateIp = config.require("wireguardServerPrivateIp");

const wireguardServerListenPort = config.requireNumber("wireguardServerListenPort");

const wireguardClientPublicKey = config.requireSecret("wireguardClientPublicKey");

const wireguardClientIp = config.require("wireguardClientIp");
```

[index.ts](https://github.com/ralscha/blog2022/blob/master/pulumi-hetzner/index.ts#L1-L14)

Next, the script reads the cloud-init script into memory. This file contains placeholders that need to be replaced with the actual values from the configuration.

```
// 2. Read the external cloud-init script content

const cloudInitScript = local.getFile({

  filename: path.join(__dirname, "wireguard.yaml"),

});

// 3. Prepare the cloud-init user_data with interpolated secrets and configuration

const userData = pulumi.all([

  cloudInitScript,

  wireguardClientPublicKey,

]).apply(([script, publicKey]) => {

  return script.content

    .replace(/WIREGUARD_SERVER_PRIVATE_IP_PLACEHOLDER/g, wireguardServerPrivateIp)

    .replace(/WIREGUARD_SERVER_LISTEN_PORT_PLACEHOLDER/g, wireguardServerListenPort.toString())

    .replace(/WIREGUARD_CLIENT_PUBLIC_KEY_PLACEHOLDER/g, publicKey)

    .replace(/WIREGUARD_CLIENT_IP_PLACEHOLDER/g, wireguardClientIp);

});
```

[index.ts](https://github.com/ralscha/blog2022/blob/master/pulumi-hetzner/index.ts#L16-L31)

Next, the Pulumi code generates an SSH key pair for the server. This key pair will be used to access the server via SSH. We do not store the private key here because we only want to use this SSH key for executing a remote command in a later step. The package [`node-forge`](https://github.com/digitalbazaar/forge) is used to generate the SSH key pair.

```
// 4. Generate SSH key pair for the server

const keypair = forge.pki.rsa.generateKeyPair(4096);

const sshPrivateKey = forge.ssh.privateKeyToOpenSSH(keypair.privateKey);

const sshPublicKey = forge.ssh.publicKeyToOpenSSH(keypair.publicKey, "root@host");

const sshKey = new hcloud.SshKey("wireguard-ssh-key", {

  name: \`wireguard-server-ssh-key\`,

  publicKey: sshPublicKey,

});
```

[index.ts](https://github.com/ralscha/blog2022/blob/master/pulumi-hetzner/index.ts#L33-L41)

In the next step, the server is provisioned with the Debian 12 image. Like in the example above, I chose the CX22 server type, the least expensive type available. In the userData property, the program passes the cloud-init script prepared earlier.

```
// 5. Provision the Hetzner Cloud Server

const wireguardServer = new hcloud.Server("wireguard-server", {

  name: "wireguard-server",

  sshKeys: [sshKey.id],

  serverType: "cx22",

  image: "debian-12",

  location: serverRegion,

  userData: userData

});
```

[index.ts](https://github.com/ralscha/blog2022/blob/master/pulumi-hetzner/index.ts#L43-L51)

The next step defines an external firewall that will be attached to the server. It opens the SSH port (22/TCP) and the WireGuard port (51820/UDP) for incoming traffic.

```
// 6. Define the Hetzner Cloud Firewall

const wireguardFirewall = new hcloud.Firewall("wireguard-firewall", {

  name: "wireguard-server-firewall",

  rules: [

    {

      direction: "in",

      protocol: "tcp",

      port: "22",

      sourceIps: ["0.0.0.0/0", "::/0"],

      description: "Allow SSH access",

    },

    {

      direction: "in",

      protocol: "udp",

      port: wireguardServerListenPort.toString(),

      sourceIps: ["0.0.0.0/0", "::/0"],

      description: "Allow WireGuard VPN traffic",

    }

  ]

});

// 7. Attach the Firewall to the Server

new hcloud.FirewallAttachment("wireguard-firewall-attachment", {

  firewallId: wireguardFirewall.id.apply(id => parseInt(id, 10)),

  serverIds: [wireguardServer.id.apply(id => parseInt(id, 10))],

});
```

[index.ts](https://github.com/ralscha/blog2022/blob/master/pulumi-hetzner/index.ts#L53-L78)

  

Now the script has to wait for the server to be fully provisioned and cloud-init to complete. This is necessary because our WireGuard client needs to know the WireGuard server's public key, which is generated during the cloud-init process.

To wait for cloud-init to complete, the script runs the `cloud-init status --wait` command on the server. This is where we use the SSH key that was generated earlier. This call blocks until cloud-init has finished running.

```
// 8. Wait for cloud-init to complete

const waitForCloudInit = new command.remote.Command("wait-for-cloud-init", {

  connection: {

    host: wireguardServer.ipv4Address,

    user: "root",

    privateKey: sshPrivateKey,

  },

  create: "cloud-init status --wait || true",

}, {dependsOn: [wireguardServer]});
```

[index.ts](https://github.com/ralscha/blog2022/blob/master/pulumi-hetzner/index.ts#L80-L88)

The final step is to export the information we need for the WireGuard client configuration. Exporting variables in Pulumi is done using the `export` keyword. When you run `pulumi up`, these variables will be displayed in the console output.

The client needs to know the server's public IP address and the Wireguard server's public key. The public IP address is easily accessible via the `ipv4Address` property of the `wireguardServer` resource. For the public key, the script runs the command `cat /etc/wireguard/server_public.key` on the server to retrieve it.

```
// 9. Export relevant outputs

export const serverPublicIp = wireguardServer.ipv4Address;

const getWireguardPublicKey = new command.remote.Command("get-wireguard-public-key", {

  connection: {

    host: wireguardServer.ipv4Address,

    user: "root",

    privateKey: sshPrivateKey

  },

  create: "cat /etc/wireguard/server_public.key",

}, {dependsOn: [waitForCloudInit]});

export const wireguardServerPublicKey = getWireguardPublicKey.stdout;
```

[index.ts](https://github.com/ralscha/blog2022/blob/master/pulumi-hetzner/index.ts#L91-L103)

You can now run `pulumi up` to provision the WireGuard VPN server. After the provisioning is complete, Pulumi prints the server's public IP address and the server's public key into the console output. Copy these values to your WireGuard client configuration file.

The client configuration file should look like this:

```
[Interface]

PrivateKey = yIap...

Address = 10.0.0.3/32

DNS = 193.110.81.0

[Peer]

PublicKey = Ds1k79...

AllowedIPs = 0.0.0.0/0

Endpoint = 95.216.157.143:51820
```

You should now be able to connect to the WireGuard VPN server using the WireGuard client.

With this configuration, you now have an easy way to start a WireGuard VPN server whenever you need it and tear it down when you are done. Hetzner Cloud charges you only for the time the server is running, so you can save costs by not running the server all the time.

## Conclusion

In this blog post, I showed you how to use Pulumi to provision resources on Hetzner Cloud. I showed you how to provision a virtual server and install WireGuard on it using cloud-init. Pulumi is a powerful tool that allows you to provision resources on many different cloud providers, not just AWS, Azure, and GCP.

I hope this blog post was helpful, and you learned something new. If you have any questions or suggestions, feel free to [send feedback](https://blog.rasc.ch/feedback/2025-07-pulumihetzner.html).