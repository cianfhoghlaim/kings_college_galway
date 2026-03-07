Dagger + Docker Compose Workflow for Komodo

Periphery, Pangolin Newt, and Olm

Overview

This   outline   describes   a  Dagger-powered   pipeline   that   complements   a   Docker   Compose-based

development and deployment workflow for Komodo Periphery, Pangolin Newt, and Olm containers.

The   goal   is   to   automate   image   building,   configuration,   and   testing   steps   around   Docker   Compose,
while integrating  1Password CLI ( op )  for secrets management and using  Forgejo  as a local artifact

registry.   We   will   define   modular   Dagger   pipelines   for   building   container   images,   injecting   secrets,

validating   Compose   files,   and   even   launching   ephemeral   test   environments.   This   ensures   a   clean

separation   between   local   development   tasks   and   CI/release   workflows,   and   aligns   with   production

deployment via Ansible (using Komodo for Compose stack management).

Modular Pipeline Structure

Using   Dagger,   we   organize   the   CI/CD   logic   into  reusable   modules  or   pipeline   components.   Each
module focuses on a specific concern, making the pipeline easier to maintain and adapt. Key modules

include:

•

Infrastructure Module (Images & Registry): Handles building Docker images for Periphery,
Newt, and Olm, including installing the  op  CLI in each image. It also manages publishing

images to the Forgejo container registry and pulling base images or cache layers.

•

Secrets Module (1Password Integration): Manages the flow of secrets from 1Password into the

application containers. It uses the 1Password CLI to inject secrets into configuration files or
environment variables, producing Compose-ready config (like  .env  files) with actual secrets.

This module ensures secrets are never stored in Git—only references are, which get resolved at
runtime via  op inject

.

1

•

Compose Module (Config & Orchestration): Deals with Docker Compose concerns. It can
validate the syntax and integrity of  docker-compose.yml  (and any overrides) and ensure that

the required config (env files, etc.) is in place. This module can also spin up an ephemeral multi-

container environment for integration testing, using Dagger’s services API to mimic a Docker

Compose “up” inside the pipeline

2

.

Each module can be invoked independently or as part of a unified pipeline (e.g. local dev vs CI run).

Below we detail these components and their integration points, including code examples using the

Dagger Python SDK.

Infrastructure Pipeline: Image Builds and Forgejo Registry

This part of the pipeline builds Docker images for Komodo Periphery, Pangolin Newt, and Olm. The

images are built from source (cloned or context directories) and include the 1Password CLI binary so

that containers can perform secret injection internally. For example, the Dockerfile might base on the

official 1Password CLI image or add the CLI via package manager:

1

# Example base Dockerfile snippet for Newt with 1Password CLI

FROM 1password/op:2-alpine

# Base image with op CLI v2 installed

3

COPY --from=builder /app/newt /usr/local/bin/newt

# copy built binary from

builder stage

Using Dagger's Python SDK, we can programmatically build and test these images:

import dagger

async with dagger.Connection() as client:

# Build the Newt image with op CLI pre-installed

newt_img = await client.container().build(

context="services/newt",

dockerfile="Dockerfile.newt"

).with_exec(["op", "--version"]).exit_code()

# verify op is installed

# Repeat for Olm and Periphery images...

After   building,   the   pipeline   can  push   images   to   Forgejo  (which   serves   as   a   local   OCI-compliant
registry). Dagger provides  with_registry_auth  and  publish  methods to authenticate and push

the image. For example:

registry = "forgejo.example.com/myteam"

# Forgejo registry URL (private)

auth = client.set_secret("password", FORGEJO_TOKEN)

# Forgejo PAT or

password

# Tag and push the image

await client.container().from_("newt:build-output")\

.with_registry_auth(f"{registry}/newt:dev", FORGEJO_USER, auth)\

.publish(f"{registry}/newt:dev")

This will build and push the  newt:dev  image to Forgejo. (In CI, the tag might be a git SHA or release

version.)   The   same   pattern   applies   for   Periphery   and   Olm   images.   The   Forgejo   registry   supports

standard Docker pushes for OCI images

4

5

. Forgejo can also host other package types (it provides

npm and PyPI registries, etc. for any Node/Python components)

6

.

Secrets Pipeline: 1Password CLI Integration

Secret management  is centralized via 1Password. Instead of hardcoding secrets, we use 1Password
secret references ( op:// ) in our Compose configs, which get resolved at deploy time. The Dagger

secrets module automates pulling these secrets and injecting them where needed:

•

1Password Authentication: We ensure the Dagger pipeline (or the environment running it) is

authenticated to 1Password. This could be via a 1Password service account token stored in an

environment variable or passed as a Dagger Secret. For instance, the Komodo Periphery
container might have an env var like  OP_SERVICE_ACCOUNT_TOKEN  which the pipeline and

•

7

container will use
Generating  .env  or Config Files: The pipeline can run  op inject  to produce a Docker
Compose  .env  file filled with real secrets. We maintain a template (e.g.  .env.template ) in

.

2

source with lines like  API_KEY=op://vault/Item/field . The Dagger pipeline will mount this
template and execute  op inject  to create the actual  .env :

# Mount the template and run op inject inside a temporary container

secrets_template = client.host().directory(".").file(".env.template")

injector = (client.container().from_("1password/op:2")

# image with op CLI

.with_file("/app/.env.template", secrets_template)

.with_env_variable("OP_SERVICE_ACCOUNT_TOKEN", OP_TOKEN)

# auth

token for op

".env"]))

.with_workdir("/app")

.with_exec(["op", "inject", "-i", ".env.template", "-o",

await injector.exit_code()

# run injection

secrets_env = await injector.file("/app/.env").contents()

# read the output

file

After this step, the pipeline has a populated   .env   in memory (or it could export it to the host). The
docker-compose.yml   references this   .env   file for secret values. Using   op inject   in this way

means we can safely check in the template with secret references, and only resolve them at runtime

8

.

•

Alternate: In-Container Injection: In production, Komodo can perform a similar injection step

just-in-time before bringing up the stack. In fact, Komodo’s Periphery agent supports running a
pre-deploy hook to call  op inject  and update the secrets file on the server
. Our images
already include the  op  CLI, so the same command can run inside the container if needed. By

9

setting the 1Password token in the Periphery container’s environment, the agent can retrieve

secrets on each deployment without storing them persistently
. This approach ensures that if
the  .env.secrets  file is missing or not injected, the deployment will fail (which is safer than

7

accidentally running with placeholder values).

•

Secret  Flow  Summary:  For  local  development,  the  Dagger  pipeline  will  produce  the  secret-
injected config (e.g. writing a local   .env   that developers can use with Compose, or directly
exporting   environment   variables   via   op   run ).   For  CI  and  staging/production,   secrets   are

pulled via service account and either injected into files or provided as env vars to containers. In

all cases, secrets flow from 1Password vaults into the running containers only at execution time,
and never live in source control or image artifacts. (1Password’s templates and   op inject

even support using different vaults or items per environment, allowing the pipeline to switch

secrets between dev/staging/prod easily

10

.)

Compose Pipeline: Config Validation and Ephemeral Services

This module focuses on using the Docker Compose definitions in a safe, testable way:

Compose  File  Validation:  Before  deploying  or  running  the  stack,  we  validate  the  Docker  Compose
YAML and environment configuration. This can be done by running  docker compose config  (which

parses the YAML, merges any overrides, and can catch syntax or missing variable errors). The Dagger

pipeline can spin up a one-off Docker CLI container to do this:

3

# Validate docker-compose.yml using Docker CLI in Dagger

compose_check = (client.container().from_("docker:24.0.5")

# Docker CLI

image

.with_mounted_directory("/proj", client.host().directory("."))

# mount

project

.with_workdir("/proj")

.with_exec(["docker", "compose", "config", "-q"]))

await compose_check.exit_code()

# will be non-zero if the config is invalid

This  ensures  that  the  YAML  is  well-formed  and  all  required  env  vars  (from  the  injected   .env )  are

present. It’s a quick static check in the pipeline.

Ephemeral   Integration   Testing:  Dagger   can   go   beyond   static   validation   by   actually   launching   the
services in an ephemeral environment (similar to running   docker-compose up   in CI, but without

persisting anything on the host). Thanks to Dagger’s services API, we can define each service container

and run them concurrently in the pipeline network. For example, we can define a Pangolin service and a

Newt service and test their interaction:

# Define Pangolin service container

pangolin_svc = client.container().from_("pangolin:latest")\

.with_exposed_port(443)\

.with_env_variable("PANGOLIN_ENV", "dev")\

.with_env_variable("OP_SERVICE_ACCOUNT_TOKEN", OP_TOKEN)\

.with_workdir("/app")\

.as_service(["/app/pangolin", "--serve"])

# hypothetical start command

# Define Newt container with Pangolin service bound

newt_test = client.container().from_("newt:dev")\

.with_service_binding("pangolin", pangolin_svc) \  # bind Pangolin under

hostname "pangolin"

.with_env_variable("PANGOLIN_URL", "https://pangolin:443")\

.with_exec(["/app/newt", "--connect-test"])

newt_output = await newt_test.stdout()

print("Newt connectivity test:", newt_output)

In   this   snippet,   the   Newt   container   is   started   with   the   Pangolin   service   accessible   via   hostname
pangolin  (the alias we set) on port 443

. The Newt client might attempt to register with Pangolin

11

or open a tunnel; we then capture its output or exit code to verify success. We could similarly start an

Olm container if it plays a role in connectivity (Olm might act as another node or a client in the mesh).
Using  Service  bindings ensures all these containers run in the same virtual network inside Dagger,

without affecting the host, and we can coordinate their startup order if needed. For example, we might

add a slight delay or a health-check loop to ensure Pangolin is listening before Newt tries to connect.

Test   Scenarios:  With   the   full   Compose-defined   stack   running   in   Dagger,   we   can   run   a   battery   of

integration tests: -  Container Health Checks:  Ensure each service (Periphery, Pangolin, Newt, Olm)

starts without errors. This can involve checking process exit codes or calling health endpoints (e.g., an
HTTP   GET /healthz   via a bound service as shown above). -  Secrets Injection Verification:  Each

container   should   be   able   to   access   the   secrets   it   needs.   For   instance,   if   Olm   needs   a   key   from

4

1Password,   we   could   verify   that   an   expected   env   var   is   set   inside   the   running   container   (using
with_exec printenv  or similar in a test container). - Connectivity Tests: As noted, verify that Newt

can successfully connect to Pangolin (and Olm if applicable). This might be done by inspecting logs or

using the service’s CLI/API. In Dagger, one container could curl another’s API endpoint, or we could

. For example, after bringing up the Pangolin
expose a port to the host and use a host-side request
service, use a Dagger  Host().http()  (host network tunnel) to call Pangolin’s web UI or API to ensure

12

it’s responding. -  Full-Stack Integration Test:  Optionally, run a lightweight test client or script that

exercises the system end-to-end. This could be done by launching an ephemeral container (or using the

Periphery   container)   that   simulates   a   user   action   (for   example,   hitting   a   URL   that   goes   through

Pangolin/Newt).

All these test steps can be orchestrated in the Dagger pipeline. They give confidence that the Compose

setup will work in production before we actually deploy it.

Integration with Ansible for Production

In production (and staging), deployments are managed via  Ansible  and  Komodo. The outputs of our

Dagger pipelines integrate with this process as follows:

•

Image Deployment: The Docker images built and published to the Forgejo registry by the CI

pipeline are referenced in the Ansible playbooks or Komodo stack definitions. For instance, an

Ansible playbook might update the Docker Compose YAML on the servers to use
forgejo.example.com/myteam/newt:<version>  tags, or it might trigger
docker compose pull  for the new image. Because the images are in a Forgejo (on-prem)

registry, the servers (and Komodo Periphery agent) can pull them directly with the correct
credentials (Forgejo user or token). The Forgejo registry fully supports standard  docker pull

commands for the tagged images

5

.

•

Secrets on Servers: We avoid storing plaintext secrets on disk in production. Instead, the
Komodo Periphery agent (which runs on each server) uses the  op  CLI to inject secrets just

before launching the stack

9

. In our pipeline, we ensured that the container images have  op

installed and that the agent is configured with a 1Password token (via environment or a

mounted config)

7

. The Ansible playbook is responsible for the one-time setup of these

conditions: it can provision the 1Password CLI binary on the host or in the container, and set the

necessary environment variable or config file with the 1Password credentials (often a service
account token or an OTP for the agent to use). Once set up, Komodo/Ansible will run  docker
compose up  through the Komodo agent, which internally calls  op inject  as defined in the

stack’s pre-deploy hook. This means that when the Compose stack comes up on the server, all

secret placeholders have been replaced by real values fetched from 1Password, and this

happens on every deployment (ensuring updated secrets propagate without manual

intervention).

•

Ansible Playbook Structure: The playbook may include tasks like:

•

•

•

•

Updating the Compose YAML (or pulling the latest from a Git repo that our pipeline updated).
Logging into the Forgejo registry ( docker login ) using stored credentials (which could also
be pulled from 1Password via Ansible vault or  op  integration).
Pulling the new container images ( docker compose pull  or  docker pull  for each).
Running  docker compose up -d  to apply the new deployment. Komodo can handle this step

via its API/agent, or Ansible can directly invoke Docker if Komodo is primarily an orchestrator/UI.

•

Verifying the services are running (Ansible can hit health endpoints similar to our pipeline tests).

5

•

(Optionally) Running  op inject  via an Ansible task if for some reason it’s not done by the
Komodo agent. This would involve copying the template env file and running the  op  command

on the remote host.

All these steps are facilitated by artifacts from the Dagger pipeline: container images in the registry,

Compose files (with secret references or env file templates) in version control, and possibly exported
.env  files for reference. The separation of concerns is maintained — Dagger/CI prepares everything

(images, templates, tested configs) and Ansible/Komodo performs the actual deployment using those

inputs.

Local Development vs CI Workflows

We differentiate between the local developer experience and the CI/release automation, even though

they use the same Dagger pipeline code under the hood. Environment variables or CLI flags can control
the behavior (for example, a  CI=true  flag might trigger pushes to the registry, whereas a developer

run would not push).

Local   Development   Pipeline:  Developers   can   run   the   Dagger   pipeline   on   their   machine   to   rapidly

iterate:   -  Image   Building:  A   dev   can   build   updated   images   for   Periphery/Newt/Olm   using   Dagger

(which uses the local Docker daemon or Dagger engine). These images can be loaded into the local
Docker engine (using something like   client.container().export   to a tar or the Docker API) for
use with Docker Compose. Alternatively, the pipeline could directly perform   docker compose up

using the newly built images. One approach is mounting the host Docker socket into a Dagger task
container, allowing Dagger to run  docker compose up  on the host’s Docker – though this requires
caution.   Many   devs   will   simply   run   docker   compose   up   themselves   after   Dagger   has   built   the
images and generated the  .env  file. - Secrets Handling: For local work, the pipeline can output the
filled  .env  (with real secrets) for the developer. This might be written to a known path or printed for
use. Because this file contains sensitive data, it’s typically added to  .gitignore  and maybe omitted
after use. The developer needs to be signed in to 1Password ( op signin ) beforehand, or the pipeline
uses their session (for example, Dagger can use the host’s  op  session if available). - Ephemeral Stack

(Optional):  A developer can run the integration tests in Dagger to verify changes without bringing

everything up on their host. This is faster and avoids polluting the local environment. Once tests pass in
Dagger, they might do a final  docker compose up -d  for manual testing or debugging in their IDE.

CI/CD Pipeline (Forgejo Actions or other CI): In continuous integration, the pipeline does everything

needed for a release: - Build and Publish: It builds all images and pushes them to the Forgejo registry
with appropriate tags (e.g.  :pr-123 ,  :main , or version tags on release). This ensures a consistent,
versioned artifact delivery. Using Dagger’s  publish()  in CI automates this push
. For example, on
a   merge   to   main,   CI   might   push   forgejo.example.com/org/komodo-periphery:main   and
similarly for Newt/Olm, whereas on a git tag it might push a version like  :v1.2.3 . - Automated Tests:

13

The CI pipeline will run the full test suite – linting, unit tests, plus the Dagger-powered integration tests

(Compose validation, connectivity tests, etc.). These give a green light before artifacts are released. -

Secrets in CI: The CI environment can supply the 1Password credentials (perhaps as a masked secret or
using Dagger’s 1Password secret provider with   --token=op://...   in the Dagger CLI
). This
way,   CI   can   perform   the   same   op   inject   and   secret   verification   steps   as   local,   without   storing

14

15

secrets in CI variables directly. Dagger’s integration with secret managers allows fetching secrets on the

fly in the pipeline. - Deployment Workflow: Depending on the setup, a successful CI run might trigger

deployment. If using Forgejo Actions or another CI, one could define a job that, after pushing images,

invokes Ansible or calls the Komodo API to deploy. However, often deployments are manual or triggered

separately. In our case, we might have a separate Ansible pipeline (possibly also containerized or using

6

Dagger’s SSH module) that runs on demand or on schedule to deploy the latest images to production.

This separation ensures that only vetted images (e.g., those on a tagged release) are deployed. The

Dagger pipeline can also output a  release bundle  (for example, a Compose file with all image tags

pinned to that release, plus the secrets template) which Ansible then uses for the deployment.

In summary,  local dev  uses Dagger primarily for convenience (building images fast, injecting secrets,

spinning   up   test   containers   on   the   fly),   whereas  CI  uses   Dagger   for   consistency   and   automation

(ensuring the same steps run headlessly, producing deployable artifacts). Both benefit from the single-

source pipeline logic. By structuring the pipeline into modular functions (build, test, inject, etc.), we can

mix and match these in different contexts. For example, a developer might skip the registry push, while
CI will include it; the code might check an env var to decide whether to run  image.publish()  or not.

This approach yields a robust, consistent deployment pipeline: from writing code -> Dagger builds &

tests -> images to Forgejo -> Komodo/Ansible deployment – all while keeping secrets secure and out

of source control.

Sources:  The   solution   referenced   Dagger’s   documentation   and   community   examples   for   building/

pushing images and using service containers

13

11

, 1Password’s official guidance on secret injection

1

, and community insight into Komodo’s 1Password integration for Docker Compose

9

7

. These

informed the design of a pipeline that seamlessly ties together Docker Compose, Dagger, 1Password,

and Forgejo in both development and production stages.

1

8

10

Use secret references with 1Password CLI | 1Password Developer

https://developer.1password.com/docs/cli/secret-references/

2

12

Dagger 0.9: Host-to-Container, Container-to-Host, and other Networking Improvements |

Dagger

https://dagger.io/blog/dagger-0-9

3

Upgrade to 1Password CLI 2 | 1Password Developer

https://developer.1password.com/docs/cli/upgrade/

4

5

Container Registry | Forgejo – Beyond coding. We forge.

https://forgejo.org/docs/latest/user/packages/container/

6

PyPI Package Registry | Forgejo – Beyond coding. We forge.

https://forgejo.org/docs/latest/user/packages/pypi/

7

9

Is anybody using 1Password for Docker Secrets? : r/selfhosted

https://www.reddit.com/r/selfhosted/comments/1kn3o7l/is_anybody_using_1password_for_docker_secrets/

11

main.py

https://github.com/dagger/dagger/blob/44d67ca4b5ef2420317350e99f91c284de96bcb8/docs/current_docs/extending/

modules/snippets/services/bind-services/python/main.py

13

How to Create a Dagger Pipeline on Akamai | Linode Docs

https://www.linode.com/docs/guides/create-a-dagger-pipeline/

14

15

Secrets Integration | Dagger

https://docs.dagger.io/features/secrets/

7

