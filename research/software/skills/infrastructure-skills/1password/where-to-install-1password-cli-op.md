Where to Install the 1Password CLI ( op )

Komodo’s deployment tasks (like build or pre-deploy scripts) run on the Periphery agent rather than in

the Core container. The Periphery service is essentially an agent that executes build and deployment
commands on the target host (e.g. running pre-build scripts before   docker build )
. Therefore,
the 1Password CLI should be installed in the Komodo Periphery container. Installing  op  in Core isn’t

1

useful   because   Core   mainly   orchestrates   deployments,   while   Periphery   executes   the   actual   shell
commands on connected servers. In short, add  op  to the Periphery image so that any pre-deploy or

build script can call it during a deployment.

Dockerfile Snippet: Adding 1Password CLI to Periphery

To   include   the   1Password   CLI   in   Komodo’s   Periphery   container,   you   will   build   a   custom   image   that
extends the official one. For example, create a Dockerfile (e.g.  Dockerfile.periphery-op ) that uses
the official Komodo Periphery image as the base and installs   op . Using the official 1Password Linux

. For instance, if the base image is
installation method via APT is recommended for compatibility
Debian/Ubuntu-based,   you   can   add   1Password’s   apt   repository   and   install   the   1password-cli

2

package:

# Dockerfile.periphery-op

FROM ghcr.io/moghtech/komodo-periphery:latest

# Extend official Periphery

image

# Install dependencies for adding new apt repo

RUN apt-get update && apt-get install -y curl gnupg debsig-verify && rm -rf /

var/lib/apt/lists/*

# Add 1Password apt repository and key, then install the CLI

RUN curl -sS https://downloads.1password.com/linux/keys/1password.asc | \

gpg --dearmor -o /usr/share/keyrings/1password-archive-keyring.gpg && \

echo 'deb [signed-by=/usr/share/keyrings/1password-archive-keyring.gpg]

https://downloads.1password.com/linux/debian/amd64 stable main' > \

/etc/apt/sources.list.d/1password.list && \

mkdir -p /etc/debsig/policies/AC2D62742012EA22 /usr/share/debsig/

keyrings/AC2D62742012EA22 && \

curl -sS -o /etc/debsig/policies/AC2D62742012EA22/1password.pol https://

downloads.1password.com/linux/debian/debsig/1password.pol && \

curl -sS -o /usr/share/debsig/keyrings/AC2D62742012EA22/debsig.gpg

https://downloads.1password.com/linux/keys/1password.asc && \

apt-get update && apt-get install -y 1password-cli && rm -rf /var/lib/

apt/lists/*

In this example, we add 1Password’s GPG key and apt repository, include the debsig policy (to satisfy
package signature checks), then install the  1password-cli  package. This uses the official 1Password
CLI package (providing the   op   binary)
. If Komodo’s base image is Alpine instead, adjust to use
1Password’s Alpine APK repository (adding the appropriate   apk   repository and key before running

2

1

apk add 1password-cli ). The goal is to use 1Password’s official installation method for the CLI,

ensuring you get the correct version and updates.

Building and Using the Custom Image in Docker Compose

Since we need a modified image, we’ll build a custom Periphery image and tell Docker Compose to use

it. You have a couple of options:

•

Build and push to a registry (or local): Build the image using the Dockerfile above, tag it (e.g.
mykomodo-periphery:op ), and push to a registry (or keep it local). Then in your Compose file
( mongo.compose.yaml   or an override), change the   periphery:   service to use   image:
mykomodo-periphery:op  instead of the official image.

•

Use a Compose override to build: Place the Dockerfile in your Komodo directory and create a
docker-compose.override.yaml  (or edit the compose file) to build the image. For example,

in an override file you could specify:

services:

periphery:

build:

context: .

dockerfile: Dockerfile.periphery-op

This tells Compose to build the custom Dockerfile (extending the official base) for the periphery

service. The official Komodo images are designed to be used as base images for extension (e.g.
FROM ghcr.io/moghtech/komodo-core:<tag>

, similarly for periphery), so this

3

approach is compatible with Komodo’s deployment structure.

After adding the override, deploy Komodo using both the base compose file and your override. For

example:

docker compose -p komodo -f komodo/mongo.compose.yaml -f docker-

compose.override.yaml up -d

This will build the custom image and start the stack. The Core container remains unchanged (still using

the official image), and the Periphery container will be running your custom image with the 1Password

CLI available.

Non-Interactive  op  Configuration (Service Account)

To use the 1Password CLI in scripts non-interactively (so it can fetch secrets without manual login), you

should   use   a   1Password  Service   Account  token.   1Password   allows   CLI   login   via   an   environment
variable token, avoiding any interactive prompts. Specifically, set the   OP_SERVICE_ACCOUNT_TOKEN

environment variable to the token value for your service account
. With this variable present, the
op  CLI will automatically authenticate as that service account when invoked, without needing an  op
signin  command.

4

2

Configuration   steps:  Add   the   service   account   token   as   an   environment   variable   in   the   Periphery
container. Since Komodo’s  compose.env  file is loaded into both Core and Periphery by default
you can put the token there or set it in a service-specific override. For example, in  compose.env  add:

5

6

,

OP_SERVICE_ACCOUNT_TOKEN="<your-1Password-service-account-token>"

This will be passed into the Periphery container (and Core, though Core will simply ignore it). Ensure

you keep this token secure – if you prefer, you can use Docker Compose’s secret file mechanism
instead (Komodo supports the  ${VAR}_FILE  pattern for secrets
deploy or build script running in the Periphery agent can call  op  commands and retrieve secrets from

). With the token set, any pre-

7

1Password without interactive login. This non-interactive setup uses the official 1Password

recommendation for CI/service usage

4

, allowing Komodo to fetch needed secrets (API keys,

credentials, etc.) from your 1Password vault as part of the deployment process.

Summary: Install the  op  CLI in the Periphery container (where deployment scripts run) by extending

the official image with 1Password’s official install steps. Use a custom Dockerfile and either override the

Docker Compose service or build a new image to include the CLI. Finally, configure the 1Password CLI

for   headless   use   by   providing   a   service   account   token   via   environment   variable,   so   Komodo’s

deployment routines can authenticate to 1Password and retrieve secrets automatically without human

intervention. This approach cleanly integrates 1Password into Komodo’s Docker Compose deployment

using supported methods and keeps your secrets management secure and automated.

Sources:

•

•

Komodo documentation on build/deploy flow and periphery agent
1Password official CLI installation (APT package  1password-cli )

1

2

•

GitHub package info confirming use of Komodo images as base images

3

•

1Password documentation on service account CLI usage (env token)

4

1

Building Images | Komodo

https://komo.do/docs/build-images

2

Update to the latest version of 1Password CLI

https://developer.1password.com/docs/cli/reference/update/

3

komodo-core versions · moghtech · GitHub

https://github.com/moghtech/komodo/pkgs/container/komodo-core/446106672?tag=1

4

Use service accounts with 1Password CLI | 1Password Developer

https://developer.1password.com/docs/service-accounts/use-with-1password-cli/

5

6

7

How to install Komodo build and deployment system | sleeplessbeastie's notes

https://sleeplessbeastie.eu/2025/09/16/how-to-install-komodo-build-and-deployment-system/

3

