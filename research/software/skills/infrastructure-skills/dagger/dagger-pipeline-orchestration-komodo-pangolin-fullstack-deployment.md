Dagger Pipeline Orchestration for Komodo &

Pangolin Full-Stack Deployment

1. Orchestrating Komodo Docker Compose Stacks with Dagger

•

Dagger triggers Komodo “Stack” deployments: Use Komodo’s Stack resource to deploy

Docker Compose files on target servers via the Komodo Periphery agent

1

. The Dagger

pipeline (e.g. a Node/TypeScript pipeline) can call Komodo’s API or TypeScript SDK to initiate a

stack deployment on a specified server.

•

Komodo Pre-Deploy hooks: Komodo supports running pre-deploy shell commands before

bringing up the Compose stack

2

 (e.g. seeding a database or performing migrations). The

Dagger pipeline can configure and invoke these predeploy actions by including the commands in

the Komodo stack configuration.

•

Invocation via TS SDK: Using the Komodo TypeScript SDK (available on NPM
code can programmatically create or update a Stack resource and call operations like
DeployStack . This avoids manual API calls – the SDK provides typed methods to deploy stacks

), the pipeline

3

and query status.

•

Monitoring deployment progress: Once triggered, Komodo’s core coordinates pulling images
and running  docker compose up  on the remote server. Dagger can poll Komodo for status or

wait for completion:

•

Use Komodo API/SDK endpoints to check the stack’s state (e.g. “deployed/running” or any error

reported). The pipeline might retrieve deployment logs or container statuses via Komodo’s API to

ensure all services started correctly.

•

If Komodo’s API doesn’t offer explicit post-deploy hooks (as of now only pre-deploy is built-in

2

), the Dagger pipeline itself acts as the post-deploy orchestrator. For example, after

Komodo reports a successful Compose deployment, the pipeline can proceed to run tests or

health checks (see Section 2) – effectively implementing post-deployment actions that Komodo

doesn’t handle internally.

•

Komodo Procedures vs. Dagger: Komodo also has a concept of Procedures (to chain actions

like builds and deploys in sequence) and Actions (custom TS scripts run server-side)

4

3

. In

this setup, however, Dagger is the top-level coordinator: it can trigger Komodo’s own automation

features if needed (e.g. call a Komodo Procedure via API), but generally Dagger will directly

orchestrate each step for transparency and flexibility.

2. Deploying Newt, Olm, and Periphery via Docker Compose

•

Compose stack for environment services: The full-stack deployment includes launching

Komodo’s Periphery agent alongside Pangolin’s components (Newt/Olm) on the target

infrastructure. A Docker Compose file (or multiple files) defines these services:

•

Komodo Periphery Agent: The Komodo Periphery is a lightweight Rust agent that runs on each

managed server to execute container operations as instructed by the Komodo Core

5

. In this

pipeline, the Compose stack can include a service for the Periphery container (or the agent may

be pre-installed on the VM). The Dagger pipeline ensures the agent is up and connected to the

Core before deploying other services.

•

Pangolin Newt (tunnel agent): Newt is Pangolin’s user-space WireGuard tunnel client that runs

on private networks to expose local services through Pangolin

6

. The Compose deployment

1

brings up a Newt container on the target network/site. The Newt service will register with the

Pangolin server (endpoint URL and credentials provided via env vars or config).

•

Olm (client node): Olm is the counterpart tunneling client that runs on a user’s machine to

connect into remote Newt sites

7

. In a test scenario, Olm can be run as a container or process

to simulate an external client. The Dagger pipeline might spin up an Olm container (with the
appropriate  OLM_ID ,  OLM_SECRET , and Pangolin endpoint configured) to verify connectivity

into the Newt site. Alternatively, Olm could be launched on-demand via the pipeline as a

separate step for testing, rather than as part of the always-on Compose stack.

•

Deployment via Dagger + Komodo: Dagger can deploy the above services by instructing

Komodo to bring up the Compose stack on the designated server. For example, the pipeline
could use  komodoSdk.deployStack({stackName: “…”, composeFiles: [...]})  to start

Periphery, Newt, and related containers on the target. Environment variables (IDs, secrets, config

values) for these services are injected via Komodo’s stack definition (with Dagger supplying

secrets at runtime – see Section 4 on secrets).

•

Post-deployment validation: After Komodo reports the stack is running, the pipeline performs

tests to ensure Newt, Olm, and the tunnels are functioning correctly:

•

Health checks: Dagger can run a lightweight container (or use the Olm container) to confirm that

it can reach a service on the remote network through the Pangolin tunnel. For instance, if Newt

is exposing an internal web service, the pipeline can attempt to curl that service via the Pangolin

URL or through the WireGuard interface established by Olm.

•

Connection status: The pipeline might call Pangolin’s API (via the TS client, Section 3) to verify that

the Newt site is online and that the Olm client is connected. Pangolin’s Integration API can

provide status of sites and clients, confirming that Newt registered and tunnels are active.

•

Komodo container status: Using Komodo’s API, the pipeline can double-check that all Compose

services are reported healthy (Komodo can list running containers and their exit codes or

uptime). Any container failures would be caught and the pipeline can fail fast.

•

If any test fails, Dagger can automatically roll back or bring down the Compose stack (e.g. call
komodoSdk.removeStack(...)  or run  docker compose down ) as part of cleanup. If tests

pass, the environment is proven to be deployed correctly.

3. Integrating TypeScript SDKs (Komodo & Pangolin)

•

Komodo TypeScript SDK for deployment control: The Dagger pipeline leverages the Komodo

TS client library

3

 to interact with the Komodo Core. Instead of making raw HTTP calls, the

pipeline code (running in a Node.js container) can import the SDK and use it to:
Trigger deployments: e.g.  komodo.deployStack(stackId)  or create new resources via a

•

provided method. The SDK uses the Komodo Core’s REST API under the hood, handling auth

(possibly using an API key or session – provided as a secret) and providing typed responses.

•

Retrieve status and logs: The pipeline can call SDK methods to get the deployment status or

fetch logs from the Komodo Core. For example, after calling deploy, it might poll
komodo.getStack(stackId)  until the status changes to “Running” or an error. This tight

integration allows the pipeline to wait for completion without guessing sleep times.

•

Invoke Komodo Procedures/Actions if defined: If complex sequences are defined inside

Komodo (like an Action script for additional setup), the pipeline could call those via SDK (e.g. run

a custom “PostDeploy” procedure after the stack is up). However, in many cases it’s simpler to

handle sequential steps directly in the Dagger pipeline.

•

Pangolin TypeScript client (OpenAPI generated): Pangolin exposes an Integration API

(available via Swagger/OpenAPI docs) for programmatic control

8

. To interact with Pangolin

(e.g. to query or configure tunnels) in TypeScript, the pipeline can generate a Pangolin API

client:

2

•

Client generation via OpenAPI: Using a tool like OpenAPI Generator or Swagger Codegen in a

Dagger step, the pipeline can produce a TypeScript client library from Pangolin’s OpenAPI spec.
(For example, Dagger can run a container  openapitools/openapi-generator-cli  pointing
at the Pangolin  /v1/docs  endpoint or a local spec file, and output a TypeScript SDK package.)

•

Using the Pangolin client: The generated client can be compiled or directly used in the pipeline

container. This allows Dagger to perform actions such as: creating a Pangolin Site or Resource

via API, retrieving the status of Newt/Olm connections, or running integration tests (e.g. calling a

protected endpoint through Pangolin to verify access control). If Pangolin requires certain

resources (like defining which internal services Newt should expose via “blueprints”), the pipeline

can automate that via API calls rather than manual UI configuration.

•

Automation considerations: The Dagger pipeline could generate the Pangolin client on the fly for

each run, but a more efficient approach is to generate it once (or cache it) unless the API

changes frequently. Still, this demonstrates that Dagger can automate client generation tasks

as part of the pipeline, ensuring the latest API spec is used.

•

Combining both SDKs in pipeline: The pipeline’s TypeScript runtime can use both the Komodo

SDK and the Pangolin client in tandem. For example, after deploying the stack with Komodo, it

can use the Pangolin client to verify that the Newt agent registered and perhaps automatically

configure a test route. This integration of multiple SDKs in one pipeline script highlights Dagger’s

ability to orchestrate across the full stack: from infrastructure deployment to application-level

configuration.

4. Secure Secret Injection with 1Password

•

1Password CLI ( op ) in Dagger pipelines: To keep secrets out of source code and CI logs, the

pipeline uses 1Password to fetch and inject sensitive values at runtime. One approach is to

utilize the 1Password CLI within the Dagger pipeline steps:
The pipeline can start a service container (e.g. official  1password/connect  or run  op  in a

•

utility container) that authenticates to 1Password (using a token or credentials stored in CI). For
example, a pipeline step might run:  op read "Vault/KomodoAPIKey"  to retrieve Komodo
Core’s API key, or  op read "Vault/PangolinAdminPassword"  for Pangolin credentials.

•

Building Komodo Periphery with secrets: If the Periphery agent container needs a secret (for
instance, a registration token or a TLS key to trust the Core), the pipeline can fetch it via  op  and

supply it as an environment variable when deploying the Compose stack. Dagger allows

templating the compose file or setting env vars programmatically – these values would come

from 1Password. Similarly, Pangolin’s Newt/Olm require an ID and secret pair to authenticate

9

10

; the pipeline can generate or retrieve those secrets and inject them into the containers’ env.

•

The  op  CLI can also be used to perform one-time secret provisioning steps. For example,
before running Pulumi or Komodo deploys, the pipeline might run  op inject -i
config.template.env -o config.env  to produce a config file with all the needed secrets in

place (mapping values from 1Password into the file).

•

Dagger’s native 1Password integration: As of Dagger 0.16, there is built-in support for

fetching secrets directly from 1Password and other vaults
declare a secret provider in code – for example, in a Node pipeline:  const apiKey = await
dagger.secrets.onepassword.findSecret(vault, item, field) . The Dagger engine

. This means the pipeline can

11

will securely retrieve that secret at execution time, without manual CLI calls.

•

Advantages: The secret never appears in plaintext in logs or outputs, and you avoid managing an
op  session manually. Dagger handles authentication to 1Password (you’d configure a

1Password Connect token or credentials in the pipeline’s context) and returns a
dagger.Secret  object that can be easily passed to container environments.

•

If available, using the native integration simplifies secret management: for example,
komodoApiKey = client.setSecret(await onepassword.findSecret("OpsVault",

3

"Komodo API Key", "password"))  could load the API key and make it available to any

container that needs it (Komodo SDK client, Periphery container, etc.), all in-memory.

•

Alternatives and secret hygiene: In cases where 1Password isn’t accessible (e.g. an open-

source CI where self-hosting 1Password Connect is not feasible), Dagger pipelines can fall back

to other secure injections: using CI-provided secrets (as environment variables or a mounted file)

which the pipeline then treats as Dagger secrets. The outline’s focus, however, is using

1Password for a cohesive secret workflow.

•

The pipeline should ensure no secrets are hard-coded. All sensitive data (cloud API keys,

Komodo tokens, Pangolin client secrets, etc.) reside in 1Password. Dagger pulls them just-in-time

12

, uses them for the necessary deployment steps, and ensures they aren’t printed. This setup

significantly reduces the risk of leaking credentials in transit.

5. Pulumi Integration for Infrastructure Provisioning

•

Provisioning cloud resources via Pulumi: In a full-stack scenario, some infrastructure (network,

storage, databases, etc.) might need to be set up before deploying containers. Using Pulumi’s

TypeScript SDK (IaC tool) within the Dagger pipeline allows automation of this provisioning. The

pipeline can either execute Pulumi CLI commands in a container or use Pulumi’s Automation API

in Node.js. Key integration points include:

•

Cloud services for Komodo/Pangolin: If Komodo or Pangolin require external services, the pipeline

can provision them on-the-fly. For example, Komodo or Pangolin might need an S3-compatible

storage (Cloudflare R2) for backups or an external database. The pipeline’s Pulumi code can

create an R2 bucket or a Cloudflare D1 database and output the credentials/URLs needed to use

them. Similarly, Pulumi can set up network infrastructure such as DNS records (e.g. pointing a
domain like  pangolin.example.com  to the Pangolin server), or even provision a VM/

container host on a cloud provider to deploy the stack onto.

•

Example – Cloudflare setup: Using Pulumi’s Cloudflare provider, the pipeline can ensure that

necessary DNS entries exist for the Pangolin endpoints (perhaps creating an
api.example.com  record for Pangolin’s integration API, as mentioned in Pangolin docs). If

Pangolin were to use Cloudflare Tunnel in some mode, Pulumi could also orchestrate any

Cloudflare access configurations. In general, Pulumi handles any infrastructure external to the

Docker Compose stack, so that by the time Komodo deploys the containers, all prerequisites are

satisfied.

•

•

Running Pulumi in the pipeline: The Dagger pipeline can containerize the Pulumi execution for
consistency. For instance, it could use the official Pulumi Docker image to run  pulumi up :
Mount the Pulumi code (and  .pulumi  config) into a container, pass cloud provider credentials

(fetched from 1Password) as environment secrets, and run the Pulumi program. This will apply

the infrastructure changes. The pipeline can enforce that Pulumi runs in preview or up mode

depending on environment (maybe preview in dev, up in CI).

•

Alternatively, since the pipeline is in TS, one could use Pulumi’s Automation API to run Pulumi
inline. This means writing Pulumi resource definitions in the pipeline code and using  await
stack.up()  to programmatically deploy. This method avoids needing a separate CLI

invocation, but adds complexity and is used if fine-grained control or integration is needed.

•

Integrating Pulumi outputs into deployment: After Pulumi finishes, the pipeline captures the

outputs (especially any sensitive ones) and injects them into the next stages:

•

Pulumi often returns outputs as a JSON or structured data. The pipeline can parse these. For
example, the R2 bucket creation might output an  accessKey  and  secretKey . These can
immediately be stored as Dagger secrets. The pipeline could do  const r2Key =
dagger.setSecret(pulumiOutput.accessKey)  and likewise for the secret, marking them

sensitive.

4

•

Storing secrets via 1Password: In many cases, you’ll want the newly created credentials to

persist for later use. The pipeline can leverage the 1Password CLI to store these outputs in a

vault. For instance, if Pulumi created a database password or Pangolin client secret, the pipeline
might run an  op item edit  or  op item create  command to update a “Staging/Pangolin

Secrets” entry with the new values. This way, the next pipeline run or other systems can retrieve

those secrets securely. (This step requires the pipeline to have write access to the vault – which

can be given to a CI service account.)

•

The pipeline then uses these outputs for the application deployment. For example, it could pass

the Cloudflare R2 credentials into the Pangolin container (via environment variables) so that

Pangolin can connect to that storage. Or if a DNS name was created, use it in configuration

(perhaps telling Newt or Olm what host to connect to).

•

Pulumi and Dagger synergy: By integrating Pulumi, the pipeline achieves Infrastructure-as-

Code setup as part of CI/CD. Dagger ensures this runs in a controlled containerized step, and

secrets from Pulumi are immediately handled. Any Pulumi secrets are not printed (Pulumi marks

them and Dagger can treat them as confidential). Combining this with Komodo deployments

means the pipeline truly spans from cloud provisioning to application deployment in one flow.

One must be cautious with ordering: ensure Pulumi completes before Komodo tries to deploy

containers that depend on those resources. Dagger can enforce this by structuring the pipeline

with sequential stages (Pulumi then Komodo, etc.).

6. Deployment Architecture & Environment Considerations (Local

vs CI)

•

Separation of Concerns – “Where each tool lives”: It’s important to delineate which

environment each component runs in during this orchestrated deployment:

•

Dagger Pipeline: Runs on the CI runner or developer’s machine as a series of containers. This is

where orchestration code executes (TypeScript pipeline code, Pulumi code, etc.). For example,

the Komodo SDK and Pangolin client run in the pipeline’s Node.js container – they communicate

with external services via network. The pipeline also launches utility containers (e.g. 1Password

CLI, Pulumi container) as needed. These are ephemeral – they exist only for the duration of the

pipeline run.

•

Komodo Core: The central Komodo service (web UI and API) typically runs persistently (could be a

long-running container or service in the infrastructure). In a CI test scenario, Komodo Core

might be running in a dedicated environment (for example, a staging instance accessible over

HTTPS) or the pipeline could spin up a Komodo Core container locally. Assuming a persistent

Core: the pipeline communicates with it (via its API) to perform deployments. The Komodo Core

is not part of the Dagger ephemeral containers – it’s an external dependency, akin to calling a

cloud service. (For local development, a developer might run Komodo Core via Docker Compose
on localhost and point the pipeline to  localhost:8080  for API calls.)

•

Komodo Periphery: The agent runs on each target server. In our context, one of the Compose

services is the Periphery agent container. That container runs on the target Docker host (which

could be a VM or the same machine if using Docker-in-Docker for testing). It is launched by

Komodo (through the pipeline’s request). Once running, it listens for instructions from Komodo

Core. The Periphery container is part of the runtime environment of the application stack (not

inside the pipeline).

•

Pangolin Core (Server): Pangolin’s main server (sometimes just called “Pangolin”) could be

deployed as part of the infrastructure. If the goal is to test full connectivity, the Pangolin server

might be running on a cloud VM or container; Newt and Olm will communicate with it. In a

contained test, you might even include Pangolin in the Docker Compose stack (running it locally

for integration tests). In that case, Pangolin runs alongside Newt on the test host. The pipeline

5

needs network access to Pangolin’s API (for verification steps) – this can be achieved via Docker

network setup if running locally (e.g., the Dagger engine could be on the same network as the

Pangolin container).

•

Newt and other runtime containers: These (Newt, and possibly application containers that Newt

exposes) run on the target host under the Periphery’s oversight. They are the actual workload

containers. Dagger doesn’t run these directly – it requests Komodo to run them. Once up,

Dagger can only interact via network (or via Komodo’s monitoring). For instance, to test an app

container’s endpoint, the pipeline might connect to it through the Newt/Olm tunnel or directly if

networking is open.

•

Olm client: Olm is somewhat unique – it’s a client typically run on an end-user’s machine. For

testing, the pipeline itself can act as an Olm host. This could mean running the Olm binary in a

sidecar container on the CI runner. That container would need permission to create a WireGuard

tunnel (which might require privileged mode if using the kernel module, though Olm can run

user-space tunnels as well). An alternative in CI is to run Olm on a separate lightweight VM that

the pipeline can control. In local dev, a developer could just run Olm on their own machine to

connect to the test environment. The outline assumes we might containerize Olm for

automation, but this depends on the CI capabilities (Docker daemon allowing privileged

containers, etc.).

•

Local Development vs CI pipelines: The pipeline is designed to be usable in both scenarios, but

certain configurations differ:

•

Local Dev: A developer can run the Dagger pipeline from their laptop. In this case, they might use

a local Docker host as the deployment target (perhaps even the same Docker daemon that

Dagger uses). For example, a dev could run a Komodo Core container locally and register their

local machine as a Komodo server (running a Periphery container). Running the pipeline would

then deploy Newt/Olm locally. This allows quick iteration: the developer sees the containers

come up on their machine and can manually inspect them if needed. Secrets via 1Password in
local dev might leverage the user’s logged-in session – e.g., if  op  CLI is already authenticated

on the laptop, the Dagger pipeline can mount the necessary auth or use the local 1Password

agent.

•

Continuous Integration: In CI, everything runs non-interactively. Typically, the CI runner starts the

Dagger pipeline inside a fresh environment (e.g., a GitHub Actions job or similar). The pipeline

will connect to external services (Komodo Core, 1Password Connect, etc.) over the network.

Ensuring connectivity is key: the CI environment must allow access to the Komodo Core API

(which might be behind a VPN or firewall – possibly the CI runner is in the same network or one

could run Komodo Core in a container within the CI job for isolation). If Komodo Core is not

accessible publicly, one approach is to start a Komodo Core container at the beginning of the

pipeline (point it to an in-memory SQLite or test DB) purely for the test’s duration.

•

Environment setup in CI: The CI pipeline can use Dagger to simulate an entire environment within

the job. For instance, the pipeline might start a Docker Engine (DinD) inside which it deploys the

Compose stack (Pangolin, Newt, etc.) via Komodo. This effectively means the CI job is hosting the

“remote” environment. In such a setup, Komodo Core and Periphery could both be run within

the job container network: the pipeline would spin up a Komodo Core container, start a

Periphery container (perhaps directly instead of via Core, for simplicity in testing), then deploy

Newt, etc. However, this approach can be complex – many teams will instead have a dedicated

test environment and use CI just to trigger and verify.

•

Handling secrets in CI: In a CI setting, you cannot rely on an interactive login for 1Password.

Instead, you use a 1Password token or Connect server. Typically, a CI secret (in GitHub Actions,

for example) would store a 1Password Connect API token or credentials. The Dagger pipeline
then uses that to authenticate (either by running  op login  non-interactively or via the built-in

integration configured with the token). The pipeline code should be written to pull this from env

and not require any prompts. Developers running locally will similarly need to have a method –

6

perhaps they export a read-only token for their personal vault or use the 1Password app

integration.

•

Cleanup: On CI, the pipeline should clean up resources after testing to avoid side effects (stop

containers, remove any test data). Komodo can be instructed to tear down the stack after tests
(e.g. using the Komodo SDK to remove the stack or running  docker compose down  via a

Komodo Action). In local development, a user might choose to keep the stack running for further

manual exploration, or have the pipeline also stop it depending on use-case. It’s wise to

parameterize this (e.g. an input to the pipeline “cleanup: true/false”).

•

Summary: The Dagger pipeline provides a consistent workflow but must adapt to its

environment. In all cases, the tools placement remains: Dagger = orchestrator (in CI or local),

Komodo = deployment manager (central service + agents on targets), Pangolin = networking

service (server + agents/clients), and 1Password/Pulumi = supporting services for config and

infra. By clearly separating these roles, we ensure that local tests mimic CI as closely as possible

(for example, using the same Compose files and pipeline code), differing only in endpoints and

authentication methods. This outline ensures full-stack deployment and testing can be done

reliably in both a developer’s laptop and an automated CI pipeline, orchestrated through

Dagger’s containerized approach.

1

3

4

Resources | Komodo

https://komo.do/docs/resources

2

[Feature] Post Deploy · Issue #227 · moghtech/komodo · GitHub

https://github.com/moghtech/komodo/issues/227

5

Komodo Monitoring Platform: A Comprehensive Analysis and Open Source Alternatives |

0DeepResearch Insights

https://0deepresearch.com/posts/komodo-monitoring-platform-a-comprehensive-analysis-and-open-source-alternatives/

6

10

GitHub - fosrl/newt: A tunneling client for Pangolin

https://github.com/fosrl/newt

7

9

GitHub - fosrl/olm: A tunneling client to Newt sites

https://github.com/fosrl/olm

8

Enable Integration API - Pangolin Docs

https://docs.pangolin.net/self-host/advanced/integration-api

11

12

Dagger 0.16: 1Password and Hashicorp Vault Integrations, Performance Boost, and More |

Dagger

https://dagger.io/blog/dagger-0-16

7

