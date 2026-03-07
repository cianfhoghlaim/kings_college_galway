Deploying Komodo Periphery with Pangolin

Private Access and LanceDB Stack

1. Ansible Deployment of Komodo Periphery and Pangolin Newt

Agents

Komodo Periphery Agent Setup: Use Ansible to install and configure Komodo’s Periphery agent on the

target   host,   running   as   a   systemd   service   under   a   dedicated   user   (the   Komodo   Ansible   role   by

bpbradley.komodo handles this)

1

2

. For example, in your playbook:

- hosts: komodo_hosts

become: yes

roles:

- role: bpbradley.komodo

komodo_action: "install"

komodo_version: "latest"
komodo_passkeys: [...]

# Komodo auth keys for registration

This installs the Periphery agent binary on the host and registers it as a systemd service (usually named
komodo-periphery.service ). The agent will run deployment tasks on the host, so ensure it has
network access to Docker and the services. Verify that the service is running with  systemctl status
komodo-periphery .

Pangolin   Newt   VPN   Agent   Setup:  On   the   same   host,   deploy   Pangolin’s  Newt  client   as   a   systemd

service. Newt acts as a VPN gateway for Pangolin, establishing an outbound WireGuard tunnel to the

Pangolin server and proxying traffic. Ansible can automate these steps: download the Newt binary from
the official release, place it in  /usr/local/bin , and create a systemd unit file. For example:

# /etc/systemd/system/newt.service

[Unit]
Description=Pangolin Newt VPN Client (Olm Gateway)

After=network.target docker.service

[Service]

ExecStart=/usr/local/bin/newt --id <YOUR_NEWT_ID> --secret

<YOUR_NEWT_SECRET> \

--endpoint https://<your-pangolin-server> --accept-clients

Restart=always

User=pangolin

# run as a non-root user if possible

Environment="DOCKER_SOCKET=/var/run/docker.sock"

# (Optional: allow Newt to inspect Docker for container name resolution)

1

[Install]

WantedBy=multi-user.target

In the above, replace   <YOUR_NEWT_ID>   and   <YOUR_NEWT_SECRET>   with the credentials obtained
from Pangolin when you added the site. We include the  --accept-clients  flag to enable Olm client

access on this site

3

. In this client-accepting mode, Newt runs entirely in user space and does not

create a system-wide network interface (no root privileges or kernel WireGuard needed)

4

. Instead, it

only opens specific ports that we configure as Pangolin site resources. This means the VPS will not act as

a full VPN gateway; it will only forward traffic for the allowed services (enhancing security).

Note:  Running Newt as an unprivileged user (e.g.   pangolin ) is recommended when

using user-space mode, improving security

5

. If you do so, ensure this user has access

to   the   Docker   socket   if   you   plan   to   use   Newt’s   Docker   integration.   You   can   add   the
Environment="DOCKER_SOCKET=..."   variable   (and   potentially   adjust   group

permissions) so Newt can resolve Docker container names.

Use Ansible to copy this unit file to the host and run  systemctl daemon-reload  and  systemctl
enable --now newt  to start Newt. Verify the status with  systemctl status newt

 – it should

6

show Newt connected to your Pangolin server. Once connected, the host is registered as a  Pangolin
site.   With   --accept-clients ,   Pangolin   will   allow  Olm   VPN   clients  (team   members   or   services

running the Olm client) to reach designated ports on this host over the secure tunnel.

2. Deploying LanceDB via Docker Compose (VPN-Only Access)

Prepare   a   Docker   Compose   file   to   run   LanceDB   on   the   host.   The   goal   is   to   keep   LanceDB’s   API

accessible   only   through   Pangolin’s   private   network  (not   exposed   to   the   public   internet).   In   the

Compose YAML, define the LanceDB service and bind its port only to localhost or an internal network:

services:

lancedb:

image: lancedb/lancedb:latest

container_name: lancedb

restart: unless-stopped

volumes:

- lancedb-data:/data

# persist data on a named volume

7

ports:

- "127.0.0.1:8080:8080"

# bind LanceDB’s 8080 port to

*localhost* only

volumes:

lancedb-data:

In this example, LanceDB’s REST API (default internal port 8080) is mapped to host port 8080 on the
. This means the service is reachable at   http://127.0.0.1:8080   from the

loopback interface

8

host (including by the Newt agent locally), but  not accessible externally  on the host’s public IP. By
restricting the binding to  127.0.0.1 , we ensure that only Pangolin (via the Newt client on the host) or

local processes can reach LanceDB. This satisfies the private access requirement – the database will only

be reachable through the Pangolin VPN tunnel.

2

Note:  We   mounted   a   volume   for   /data   to   ensure   LanceDB’s   data   persists   across

container restarts

7

. Adjust the volume path or use an external volume as needed for

your data retention policy. LanceDB does not yet support remote object storage by itself

(as of late 2023), so persistent local storage is important

9

.

To bring up the LanceDB service, run  docker compose up -d  (Ansible can execute this as a task on

the host, or you can leverage Komodo to run the compose). All containers in the same Compose file will

share a Docker network, allowing inter-service communication. Here we only have LanceDB, but if there
were other services (e.g. an app server), they could reach LanceDB at  http://lancedb:8080  on the

internal   network

10

.   In   our   case,   Pangolin’s   Newt   will   be   configured   to   forward   VPN   traffic   to   this

LanceDB endpoint.

Pangolin Site Resource Configuration: After LanceDB is running, define a Pangolin site resource for

it. In Pangolin’s web UI, you would go to  Resources → Site Resources  and add a new TCP resource.
However, we will automate this via the API in a later step. The resource will specify: a  Local Port (e.g.

8080)   that   Olm   clients   will   connect   to,   and   the  Target  as   LanceDB’s   address   on   the   host   (here
127.0.0.1:8080   or   the   Docker   container’s   network   address)

.   Pangolin   will   then   forward   any

11

Olm-client   connections   on   the   site’s   virtual   IP   at   port   8080   to   the   LanceDB   service.   At   this   point,

LanceDB is essentially  air-gapped  from the public internet – only users who authenticate via Pangolin
Olm (or systems with an Olm client) can reach it

.

12

13

3. Installing 1Password CLI on the Periphery Host (Non-
Interactive Auth)

To   manage   secrets   (like   tokens   and   passwords)   securely,   install   the  1Password   CLI   ( op )  on   the

Komodo   periphery   host.   Since   Komodo’s   deployments   run   on   the   periphery   agent   (not   in   the   Core
container),  op  must be available on the host system

. We use Ansible pre_tasks to install the

2

1

CLI before Komodo setup. For example, on Ubuntu/Debian hosts:

pre_tasks:

- name: Add 1Password apt signing key

apt_key:

url: https://downloads.1password.com/linux/keys/1password.asc

state: present

- name: Add 1Password apt repository

apt_repository:

repo: "deb [arch=amd64] https://downloads.1password.com/linux/debian/

amd64 stable main"

filename: "1password"

state: present

update_cache: yes

- name: Install 1Password CLI

apt:

name: 1password-cli

state: present

update_cache: yes

3

This pre-task sequence (run before the Komodo role) adds 1Password’s official package repository and
installs the  op  tool

. For other distros, use the equivalent steps (e.g. adding the 1Password APK

14

15

on Alpine or using a curl script on RHEL as appropriate).

Service   Account   Token   for   Non-Interactive   Login:  Configure   the   1Password   CLI   for   headless
operation   using   a   1Password  service   account   token.   This   token   allows   op   to   authenticate   non-

interactively, so the deployment automation can fetch secrets without manual login. The token (a long
string starting with  ops_ ) should be treated as sensitive. A best practice is to store it in Ansible vault

or in your Komodo secret store and inject it as an environment variable on the host. For example, you

can add an environment variable in the periphery agent’s systemd service or profile:

# /etc/environment (or systemd drop-in for komodo-periphery.service)

OP_SERVICE_ACCOUNT_TOKEN=<YOUR_OPS_TOKEN>

By exporting   OP_SERVICE_ACCOUNT_TOKEN , any   op   command run on the host will automatically

use this token to authenticate. No interactive prompt is needed. Ensure the token is loaded for the

Komodo   Periphery’s   execution   context   (e.g.   if   Komodo   runs   deployment   scripts,   they   inherit   this
environment). You can test this by running a command like  op vault list  on the host – it should

succeed without interactive login.

With  op  authenticated, your Ansible tasks or Komodo deployment scripts can securely retrieve secrets

from   1Password.   For   instance,   you   might   store   the   Pangolin   API   key,   Newt   credentials,   or   other
passwords  in  1Password  and  use  commands  like   op  read  "op://Vault/Item/field"   to  inject

them   at   runtime.   This   approach   keeps   sensitive   values   out   of   playbooks   and   source   code.  (If   using

Komodo’s   built-in   secret   management   instead,   ensure   those   secrets   are   defined   and   accessible   to   the

periphery agent in a similar manner.)

4. Registering the LanceDB Service with Pangolin (Post-Deploy)

After LanceDB is deployed and running on the host, we need to  register it with Pangolin  so that

Pangolin knows to forward VPN traffic to this service. We have two options to automate this: use a

Komodo Action (TypeScript) or a post-deploy shell hook. Both achieve the same end result – calling

Pangolin’s API to create a resource – but they differ in integration and security.

Option   A:   Komodo   Action   (TypeScript):  Leverage   Komodo’s   TypeScript   SDK   and   Pangolin’s   API   to

perform registration in code. Komodo Actions are scripts that run on the Komodo server side with an

authenticated context, ideal for integrating external systems
(e.g.  RegisterPangolinResource ) that Komodo can invoke after deployment. This action would:

. You can create a custom Action

16

17

1.

Gather Deployment Info: Use the Komodo API (available as  komodo  client in the action) to get

details about the LanceDB service that was just deployed. For example, fetch the container name
or host/port. If Komodo deployed the Docker Compose, you know the service name ( lancedb )

and port (8080). The container name might be the same or retrievable via Komodo if it managed

the container.

2.

Call Pangolin REST API:  Using Node.js libraries (like fetch or axios) within the Action, send an

authenticated request to Pangolin to create the resource

18

. Pangolin’s API endpoint for adding

a site resource looks like:
POST https://<pangolin-server>/api/v1/orgs/{orgId}/sites/{siteId}/

4

resources

The JSON payload should include details such as:

3.

4.

5.

6.

name : A descriptive name (e.g.  "LanceDB" ).
type : Resource type, e.g.  "TCP"  (for a raw TCP/HTTP port forward).
localPort : The port on the Pangolin VPN interface for this resource (e.g. 8080).
target : The LanceDB service address. Best practice: use the Docker container name and
port (e.g.  "lancedb:8080" ) rather than an IP
( --docker-socket  enabled), Pangolin can resolve the container’s IP dynamically by name,

. With Newt’s Docker integration

19

ensuring the target remains correct even if the IP changes or the container restarts
using Docker name, you could use  127.0.0.1:8080  since we bound to localhost, but

20

. (If not

container name is more portable on the internal network.)

Here’s a simplified TypeScript snippet illustrating the action logic:

// Inside Komodo Action (TypeScript)

import fetch from 'node-fetch';

export async function run(params) {

const deploymentId = params.deploymentId;

// 1. Get deployment info from Komodo (pseudo-code):

const deployInfo = await komodo.deployments.get(deploymentId);

const containerName = deployInfo.containerName || "lancedb";

const orgId = process.env.PANGOLIN_ORG_ID;

const siteId = process.env.PANGOLIN_SITE_ID;

const apiToken = process.env.PANGOLIN_API_TOKEN;

// 2. Construct Pangolin resource payload:

const resource = {

name: "LanceDB",

type: "TCP",

localPort: 8080,

target: `${containerName}:8080`

};

// 3. Call Pangolin API to create the resource:

const url = `${process.env.PANGOLIN_URL}/api/v1/orgs/${orgId}/sites/$

{siteId}/resources`;

const res = await fetch(url, {

method: 'POST',
headers: {

'Authorization': `Bearer ${apiToken}`,

'Content-Type': 'application/json'

},

body: JSON.stringify(resource)

});

if (!res.ok) {

const errText = await res.text();

console.error("Pangolin registration failed:", errText);

throw new Error(`Pangolin API error: ${res.status}`);

}

console.log("Pangolin resource created for LanceDB.");

}

5

this   script,

  and
In
PANGOLIN_API_TOKEN   are   provided   via   environment   variables   or   Komodo   secure   variables.   The

  PANGOLIN_SITE_ID ,

  PANGOLIN_ORG_ID ,

  PANGOLIN_URL ,

Action   uses   these   to   authenticate   and   specify   where   to   register   the   service

21

22

.   After   Komodo

deploys LanceDB (e.g. via a Compose up), you would trigger this Action – possibly automatically. For
instance, Komodo could be configured to run   RegisterPangolinResource   as a post-deployment

hook, or you could invoke it from a CI pipeline by calling the Komodo API to run the action (as described

in the komodo-pr-deploy integration)

23

.

Result:  Pangolin creates a new  site resource  for LanceDB on the specified site (the VPS). Authorized

Olm VPN clients can now connect to LanceDB by targeting the Pangolin-assigned virtual IP for the site

at port 8080. The traffic will go through Pangolin’s WireGuard tunnel to Newt, which then proxies it to

the LanceDB container on localhost:8080.

Option B: Post-Deploy Shell Script: Alternatively, use a simpler shell script executed on the periphery
. This script would use a tool like  curl  to call the Pangolin API.

host after deployment (Approach 1)

24

You can store the script in a Komodo “Repo” resource or on the host, and have Komodo trigger it once

LanceDB is up. For example, a bash script might look like:

#!/bin/bash

PANGOLIN_URL="${PANGOLIN_URL:-https://<pangolin-server>}"

ORG_ID="<ORG_ID>"

SITE_ID="<SITE_ID>"

API_TOKEN="$PANGOLIN_API_TOKEN"

# assume exported by environment

# Target container name and port:

TARGET="lancedb:8080"

RESOURCE_NAME="LanceDB"

LOCAL_PORT=8080

curl -s -X POST "$PANGOLIN_URL/api/v1/orgs/$ORG_ID/sites/$SITE_ID/resources"

\

-H "Authorization: Bearer $API_TOKEN" \

-H "Content-Type: application/json" \

-d "{\"name\":\"$RESOURCE_NAME\",\"type\":\"TCP\",\"localPort\":

$LOCAL_PORT,\"target\":\"$TARGET\"}"

You could template the IDs and token via environment variables or Ansible. Then, in your Ansible play
or Komodo procedure, invoke this script after the  docker compose up  step. If using Komodo’s repo

execution, the output and exit code of the script can be captured by Komodo

25

26

. Make sure to

check the HTTP response and handle errors (e.g., log failure and possibly rollback or retry).

Both approaches achieve the registration. The  TypeScript Action  (Option A) is more integrated with

Komodo’s automation (with better error handling and visibility in Komodo’s UI)

27

28

, while the shell

script is straightforward but more manual in terms of error checking and secret handling

29

30

.

6

5. Secure Communication and Secret Management between

Komodo & Pangolin

Ensuring   that   Komodo   and   Pangolin   integrate  securely  is   paramount.   Follow   these   best   practices

drawn from the references:

•

Use TLS for Pangolin API: Always use  https://  for Pangolin’s endpoint URL so that API calls

(resource  creation,  etc.)  are  encrypted  in  transit.  Pangolin’s  server  uses  TLS  (especially  if  you

deployed it with a valid certificate or Pangolin Cloud) to protect the API and client traffic

31

. If

you are self-hosting Pangolin and using a self-signed cert or local CA, configure Komodo’s action

or script to trust that CA as needed.

•

Scoped API Tokens: Generate a Pangolin API key with the minimal scope needed

32

33

. For

example, if Pangolin allows creating a token limited to a specific org and site (and only to create

resources), use that. This way, even if the token were compromised, its permissions are limited

32

. Avoid using a super-admin API key. In Pangolin’s settings, create a token that can “manage

resources on Site X” rather than full global admin, and use that for the integration.

•

Store Secrets Securely:  Do  not  hardcode API keys or sensitive IDs in your code or playbooks

33

. Instead, use secret managers:

•

In Komodo, you can define secure variables or use an external vault. For instance, store
PANGOLIN_API_TOKEN ,  PANGOLIN_ORG_ID ,  PANGOLIN_SITE_ID  as encrypted variables in

Komodo’s configuration (or pass them via your CI’s secret store)

34

35

. Komodo supports

injecting these into Actions or scripts at runtime (e.g., via environment variables or parameter

arguments).

•

If  using  1Password  (as  we  set  up  in  Step  3),  keep  these  secrets  in  your  1Password  vault.  At
deployment time, retrieve them with the CLI ( op ) so they never appear in plaintext on disk or in
version   control.   For   example,   your   Ansible   task   or   Komodo   Action   could   do:   export
PANGOLIN_API_TOKEN=$(op   read   "op://DevOps/Pangolin   API   Token/credential") .
The Komodo Action code snippet above assumed the token and IDs are in  process.env  – this

could   be   achieved   by   having   the   Komodo   UI   or   CI   pipeline   pass   those   in   securely   when

triggering the action

36

.

•

No   Secrets   in   Logs:  Be   careful   to   avoid   printing   sensitive   values.   Komodo   will   mask   known
secret variables in its logs (e.g., replacing them with  *** ), but you should still avoid logging the
.   In   the   TypeScript   action,   do   not   console.log   the

token   or   Newt   secret   inadvertently

37

token or full request body. If using a shell script, do not echo the token. This prevents accidental

exposure of credentials in build logs or error messages

37

.

•

Komodo Action Security:  By encapsulating the Pangolin API call within a Komodo Action, you

keep the logic and secrets on the server-side, not on the user’s machine or a less secure runner

38

. The Action runs within Komodo’s controlled environment (Node.js sandbox on the server)

and   can   use   Komodo’s   already-authenticated   context   for   Komodo   API   calls,   plus   carefully

injected   Pangolin   credentials.   This   means   the   integration   is   performed   behind   the   scenes,

triggered   by   a   CI   or   user   with   permission,   rather   than   exposing   the   Pangolin   token   on   a

developer’s machine. As noted, Komodo doesn’t currently auto-provide secrets to Actions, so you

explicitly inject them via environment or Komodo’s config

35

. Ensure that only authorized users

7

or   CI   processes   can   trigger   the   action   (e.g.,   protect   it   behind   proper   Komodo   roles   or   API

authentication).

•

Verification and Testing: Once the deployment and registration steps complete, test the end-to-

end   connectivity   securely.   From   a   client   machine   with   Pangolin’s   Olm   VPN   client   configured
(using the client ID/secret from Pangolin), connect via   olm --id <client-id> --secret
<secret> --endpoint https://<pangolin-server>

. This establishes the WireGuard

39

tunnel. Verify that you can only reach LanceDB through this tunnel:

•

Try  curl http://<site-virtual-IP>:8080/v1/health  (or any LanceDB endpoint) while
the VPN is up – it should succeed (the virtual IP can be found in Pangolin’s UI or by  ping ing the

•

site’s name; Pangolin typically assigns an IP like 10... to the site)
Confirm that the LanceDB port is not reachable from the internet. Without the VPN,  curl
http://<public-server-IP>:8080  should time out or be refused. Pangolin’s zero-trust

40

.

setup ensures that port 8080 isn’t listening on the public interface at all (we bound it to

localhost), and Pangolin will only proxy traffic for authenticated clients.

By following the above steps, we: (1) deployed Komodo’s agent and Pangolin’s Newt on the host, (2) ran

LanceDB   in   Docker   with   no   public   exposure,   (3)   integrated   1Password   for   secret   management,   (4)

automated   Pangolin   resource   registration   for   LanceDB   via   a   secure   post-deploy   process,   and   (5)

adhered to best practices for security. Komodo and Pangolin now communicate over secure channels –

Komodo uses Pangolin’s HTTPS API with a scoped token, and Pangolin exposes LanceDB only over an

encrypted WireGuard tunnel to authorized clients. This setup achieves a robust zero-trust deployment:

LanceDB is accessible to your team (or services) through Pangolin Olm VPN as a private resource

12

,

while remaining invisible to the public internet, all managed through automated Ansible and Komodo

workflows.

Sources: The configuration and practices above were based on the Pangolin access model guidelines

3

12

, the  LanceDB  self-hosting instructions

41

7

, and Komodo’s integration notes for Pangolin

and 1Password

14

42

. These ensure the deployment is done in line with industry best practices for

security and automation.

1

2

14

15

Integrating 1Password CLI into Komodo Ansible Deployment.pdf

file://file_00000000f938720ab9d6df0f291553cb

3

4

5

11

12

13

31

39

40

Hosting LiteLLM on Pangolin_ Public vs. Private Access Models.pdf

file://file_0000000060d8720abaff79f184560c04

6

Install and Configure Newt VPN Client on Linux - LIBTECHNOPHILE

http://libtechnophile.blogspot.com/2025/04/install-and-configure-newt-vpn-client.html

7

8

9

10

41

Hosting LanceDB with Docker Compose.pdf

file://file_000000009be0720a9f4feec507e8a954

16

17

18

19

20

21

22

23

33

34

35

36

37

38

Extending __komodo-pr-deploy__ for Pangolin

Integration via Komodo Actions.pdf

file://file_00000000634c720a884bca9fc8683d75

24

25

26

27

28

29

30

32

42

Comparing Approaches for Pangolin Registration after Komodo

Deployment.pdf

file://file_00000000a2d4720abeefda968109e65d

8

