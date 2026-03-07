Integrating Dagger into a Polyglot Monorepo CI/

CD Workflow

Overview: Dagger for a Containerized Monorepo

A monorepo that houses a TypeScript web app, Python data/AI code, and infrastructure scripts (Pulumi,

Komodo, Pangolin) can greatly benefit from  Dagger. Dagger is an open-source CI/CD devkit that lets

you define build and delivery pipelines in code, using containers as the execution environment

1

. In

essence,  “Dagger   is   a   glorified   Dockerfile   generator   available   in   your   favorite   programming   language,”

producing a graph of containerized steps that BuildKit can optimize, parallelize, and cache

1

. This

approach aligns well with a container-heavy monorepo:

•

Unified Pipelines in Code: Instead of separate YAML configs or ad-hoc scripts for each project

(TypeScript,   Python,   infra),   you   write   one  declarative   pipeline  in   a   real   language   (Dagger
supports   Go,   Python,   TypeScript,   etc.).   This   pipeline   can   orchestrate   building,   testing,   and

deploying  all parts  of the stack in a  single workflow, while still letting each part use the best-

suited tools or language

2

. Dagger even allows one pipeline function to call another written in

a different language (Python calling a TypeScript function, etc.), so teams can work in languages

they’re comfortable with without fragmenting the CI process

2

.

•

Monorepo-Optimized:  Dagger   0.13   introduced  first-class   monorepo   support  to   handle   large

codebases efficiently

3

. Two features are notable: context directory access (pipeline functions

can automatically see files in their subdirectory context without manual path passing) and pre-

call filtering (Dagger only uploads and considers files relevant to a given function)

3

4

. This

means   each   project/component   in   your   monorepo   can   have   its   own   Dagger   module

(encapsulating how to build/test it) and Dagger will only transfer or rebuild what’s needed for

that   component

5

4

.   In   a   very   large   repo,   Dagger   will  skip   irrelevant   files   and   avoid

invalidating caches when unrelated code changes, which yields massive performance gains

4

.

In   short,   it’s   now  “realistic   to   use   Dagger   even   in   very   large   monorepos”  because   of   these

optimizations

6

.

•

Dev-Friendly, Not Just CI: Dagger pipelines are decoupled from any CI server. You can run the

exact same pipeline locally on a developer machine as runs in GitHub Actions or other CI

7

. This

addresses   a   key   monorepo   challenge:   ensuring   consistency   between   local   dev   tasks   and   CI

builds. Dagger lets you package your workflow logic into a portable program that runs anywhere

(your laptop or a CI runner) with the same results. This property will help achieve your goals of

accelerating local development and having declarative, cross-environment workflows.

In   the   sections   below,   we’ll   dive   into   how   Dagger   can  accelerate   development,  improve   CI/CD

performance,   and  unify   workflows  across   the   TypeScript,   Python,   and   infrastructure   code   in   the

monorepo.  We’ll  also  cover  concrete  ways  to  replace  or  complement  Docker  Compose,  and  how  to

integrate   Dagger   with  GitHub   Actions  and  Komodo  (your   deployment   manager)   for   end-to-end

automation.

1

Declarative, Cross-Language Workflows as Code

One major advantage of Dagger is writing CI/CD as high-level code instead of complex YAML files. This

means you can leverage the  full ecosystem  of your language (libraries, tests, type checking) to build
robust pipelines. For example, Civo (a cloud provider) migrated from GitLab YAML to Go-based Dagger

pipelines so that developers could use familiar Go tools to manage CI logic

8

9

. Each team at Civo

now owns and tests their portion of the pipeline code, rather than handing off YAML to a separate

DevOps team

10

9

. In your monorepo, this approach could empower the web developers to define

how   to   build/test   the   TypeScript   app   in   TypeScript   code,   while   data   engineers   automate   Python

workflows in Python – yet all these pieces can plug into one unified pipeline.

Dagger encourages a modular pipeline design which is ideal for monorepos. You can give each project or

microservice in the repo its own Dagger module encapsulating its build and test steps. These modules

can   declare   dependencies   between   each   other   (e.g.   the   web   module   might   depend   on   the

infrastructure module if it needs a provisioned resource), and Dagger will resolve and execute them in

the right order

5

. Under the hood, all these modules still run on the same Dagger engine, so they

share caching and can run in parallel when possible. This cross-language, multi-component support

means your CI workflow can be expressed as a single declarative graph of operations spanning Node.js,

Python, Pulumi, etc., rather than siloed pipelines. As the Dagger docs put it: you no longer need to care

which   language   a   particular   part   of   the   workflow   is   implemented   in   –  “use   the   one   you’re   most

comfortable with” and Dagger will connect them seamlessly

2

.

Furthermore, writing the pipeline in code makes it declarative and readable despite the complex tech

stack. You use concise APIs to declare what should happen (build container, run tests, push image), and

Dagger translates that into a DAG of container actions. This is analogous to writing a Pulumi script to

declare cloud resources instead of hand-writing YAML – except here it’s for CI/CD. For example, you

might write a TypeScript Dagger function to build the frontend image, and a Python Dagger function to

run   data   pipeline   tests;   calling   them   both   in   one   flow   is   one   line   each.   All   configuration   (like   base

images, test commands, secrets) is expressed in one place with type safety and documentation. This

satisfies the goal of “declarative, cross-language, ecosystem-friendly workflows”: each part of your stack is

handled in its native ecosystem (npm for Node, pip for Python, etc.), but coordinated through a high-

level declarative pipeline.

Proven pattern: The Dagger team itself has highlighted the Monorepo + Dagger pattern in community

cases. In one case study, they show how a project with many microservices used Dagger to intelligently

build only what changed and run needed tests (e.g. parsing each sub-project’s config to decide which

Go version or dependencies to use)

11

. They were able to write this logic in Go (their primary language)

– something not possible in a static YAML CI. Because the pipeline was real code, they even wrote unit

tests for the pipeline functions  (for critical logic like version detection) using standard Go testing

libraries

9

. This improved the reliability of the CI/CD process itself. These are the kinds of capabilities

that become possible when you express your pipeline as code.

Accelerating Local Development with Dagger

One of the biggest developer experience wins Dagger provides is the ability to  run your entire CI

pipeline locally  during development. This tightens the feedback loop and reduces those “push and

pray” scenarios where you only discover CI failures after a commit. Many teams using Dagger have

noted this as a game-changer. For example, the OpenMeter team said Dagger  “eliminated the dreaded

push’n’pray  —  that  endless  cycle  of  pushing  to  Git  and  then  praying  the  CI  would  turn  green”,  because

developers can run the exact same pipeline on their machine before pushing

12

. Similarly, Civo’s CTO

2

said “the greatest thing about Dagger is that it lets you unit test your CI pipeline locally, in the same tools you

use to write code.”

13

  Instead of a black-box CI, your devs can iteratively run builds, tests, and even

deployments through Dagger, using real container environments, and trust that if it passes locally it will

pass in CI.

How this works:  When you run   dagger   (via CLI or in code) on your laptop, it spins up the Dagger

Engine (which is essentially a BuildKit-based runtime) and executes the pipeline defined in your repo. All

steps run in containers, so you don’t need to have all languages and services installed – just Docker.

Developers can, for instance, build the full stack by running a Dagger function, or launch an integration

test environment on-demand. This local run is identical to what happens on a CI runner, down to the

container images and commands used. It prevents the classic “works on my machine” issues, because

your machine is actually running the containerized CI workflow. As Solomon Hykes (Dagger co-founder)

explained, the pipeline logic is completely decoupled from the CI service – it’s like having a portable

script that you can run pre-push or post-push, whichever is needed

7

. You’re essentially treating CI as

code that developers can debug and iterate on like application code.

From a practical perspective, this means faster iteration. Suppose a developer modifies some TypeScript

and wants to ensure it passes tests and lint. Instead of pushing to trigger GitHub Actions and waiting,
they can run   dagger run test   (or whatever function) locally and get immediate feedback. If the

pipeline involves building a Docker image, it will do so on the local engine with caching (so subsequent

runs   are   faster).   If   it   needs   a   database   to   run   integration   tests,   Dagger   will  automatically   start   a

service container for the database (more on that below), so the dev doesn’t have to manually spin up

any dependent services. Everything is scripted and repeatable.

Ephemeral dev environments:  Dagger’s  service containers  feature is particularly useful to accelerate

local testing and to replace ad-hoc Docker Compose usage in dev. You can define, in your pipeline code,

a service like “start Postgres on port 5432 with this initial data” and another step that runs your app’s

tests pointed at that Postgres. When running locally, Dagger will bring up the Postgres  just in time,

ensure it’s healthy, run the tests, and then tear it down automatically

14

15

. All of this is done in

isolated containers, so you don’t pollute your dev machine with long-running services or conflicting

versions. For example, Civo’s monorepo pipeline uses this approach: “if a project needs an SQL database

for its unit tests, a Dagger pipeline spins up a transient database service and attaches it to the test.”  This

allowed their developers to run even complex integration tests locally with ease, reducing reliance on

ops team setups

16

.

To illustrate, here’s a simplified example inspired by Dagger’s docs. We start an HTTP server as a service

in one function, and then use it from another function that simulates a test call:

@function

def http_service(self) -> dagger.Service:

# Launch a simple HTTP server inside a Python container

return (

dag.container().from_("python:3.11")

.with_new_file("index.html", "Hello, world!")

.with_exposed_port(8080)

.as_service(args=["python", "-m", "http.server", "8080"])

)

@function

async def get(self) -> str:

3

# Container that calls the HTTP service via alias "www"

return await (

dag.container().from_("alpine")

.with_service_binding("www", self.http_service())

.with_exec(["wget", "-qO-", "http://www:8080"])

.stdout()

)

In this example (from Dagger’s documentation), the  http_service  function starts a container running a
web   server,   and   get   uses   with_service_binding("www",   ...)   to   bind   that   service   under   the
hostname   www   for   an   Alpine   container   that   fetches   the   page

.   The   service   container   will   be

17

18

automatically started and cleaned up by Dagger.

You could define analogous functions for, say, a PostgreSQL service and a test runner container. This

programmatic replacement for Docker Compose means developers (and the CI) can spin up exactly
the services needed for a test on the fly. No more maintaining separate   docker-compose.yml  files

for dev or test environments – the logic lives in code and uses content-addressed containers. Dagger

ensures   each   service   is   reachable   by   the   others   (it   sets   up   an   internal   network   and   canonical

. And if you do want to access a service from your host (say, to manually inspect a
hostnames)
DB or view a web UI), Dagger provides an  up  command to expose it on localhost during an interactive

14

15

session

19

20

.

Overall, integrating Dagger for local development will make the “inner loop” of coding -> building ->

testing   much   faster   and   more   consistent.   Developers   get   quick   feedback   with   production-like

conditions, and any pipeline issues can be caught and fixed before pushing (even pipeline code itself

can   be   tested   locally).   This   addresses   your   goal   of   accelerating   local   dev   and   using   declarative

workflows – developers run a single command that brings up everything they need, via code, in a self-

documenting way.

Caching and Reusing Containers Between Local and CI

Heavy   use   of   containerization   can   sometimes   hurt   CI   speed   (e.g.   rebuilding   images   or   reinstalling

dependencies repeatedly). Dagger attacks this problem with aggressive caching at every step. Because

all pipeline steps run in a BuildKit engine, Dagger can cache intermediate results like Docker image

layers and file system contents. In practice, this means huge speed improvements for both local and

CI workflows, by reusing work across runs.

Build layer caching: Think of each step in your pipeline (install dependencies, compile code, run tests,

etc.)   as   analogous   to   a   Dockerfile   layer.   Dagger,   via   BuildKit,   will   cache   the   outcome   of   each   step

identified by its inputs. If nothing relevant changed, the step is skipped and its result (a container or

files from a previous run) is reused

21

22

. For example, if you’ve already built the Python wheel or

Node modules once, and your code hasn’t changed since, Dagger can reuse that from cache instead of

rebuilding. This caching is automatic – you don’t have to explicitly configure keys or restore steps as in

traditional CI. In fact, Dagger users often stop using CI-specific caching entirely:  “We use Dagger and

GitHub Actions ourselves, and have completely stopped using GHA’s caching system. Why bother, when Dagger

caches everything automatically?”  notes Solomon Hykes

23

. The cache is content-addressed (keyed by

the exact code/command inputs), so it’s highly reliable and doesn’t suffer from flaky misses or size limits

that GitHub’s cache might have

24

25

.

4

Cache volumes: In addition to layer caching, Dagger supports cache volumes for persistent data like

package download caches, build artifacts, etc. These are analogous to docker cache mounts or volumes
.   For   instance,   you   could   cache   ~/.npm   or   ~/.cache/pip

that   survive   across   pipeline   runs

26

between runs so that dependency downloads are not repeated each time. Or cache a compiled build

output to speed up subsequent test runs. OpenMeter’s team describes using both layer caching and

cache volumes as  “instrumental in speeding up what would otherwise be time-consuming processes”

27

.

They configured their Go build pipeline to mount the Go module cache and build cache as volumes,

yielding a  significant decrease in build time

28

. You can do the same for Node modules, Python pip

cache,   etc.,   in   your   monorepo’s   pipeline.   For   example,   a   Dagger   pipeline   step   might   look   like:
dag.Container().from('node:18').withMountedCache('/app/node_modules',

dag.CacheVolume('npm-cache'))  (conceptually) so that  npm install  results persist.

Distributed cache for CI:  On a developer’s laptop, caching is straightforward – the Dagger engine

persists caches on disk between runs. But CI runners (like GitHub’s) are ephemeral and normally start

fresh each time. Dagger provides solutions for this as well. One option is using Dagger Cloud’s remote

cache: when enabled, the engine will push/pull cache layers from a shared service so that even new

runners can retrieve past build results

29

30

. Another option the Dagger community uses is Depot (a

service that provides persistent BuildKit runners for GitHub Actions). Depot’s hosted runners come pre-

connected to an external Dagger Engine with automatic persistent caching

31

32

. In either case, the

effect  is  that  your  CI  pipeline  can  use  the  same  cache  across  runs,  avoiding  the  cost  of  rebuilding

everything on each commit. The Dagger docs note that Depot’s solution  “makes it easier than ever to

integrate Dagger into your workflows without sacrificing performance,” by providing automatic persistent

layer caching and multi-arch support out of the box

33

.

Real-world impact: Teams have seen dramatic improvements in CI times thanks to Dagger’s caching.

Civo cut their monorepo build from 30 minutes to 5 minutes after adopting Dagger and its caching logic

34

. OpenMeter similarly reports going from 25 to 5 minutes, a  5× speedup, after optimizing their

pipeline   with   Dagger   +   Depot

35

36

.   These   gains   come   from   not   re-doing   work   unnecessarily.

Dagger’s caching is intelligent: it will only rebuild parts of the stack that changed. For example, if you

only modified a Python module, the Node app build can come straight from cache, and vice versa. Also,

by structuring the pipeline for cache-friendliness (e.g. installing dependencies before adding changing

source code), you maximize reuse of layers

37

38

. Dagger encourages this structure, similar to writing

an efficient Dockerfile.

In practice, when using Dagger for your monorepo, you’ll find that after an initial run, subsequent runs
(both local and in CI) are  blazingly fast. A developer running   dagger test   repeatedly while making

small changes will notice that only the tests re-run, while building containers or downloading deps

happens just once. In CI, if you build a Docker image for the TypeScript app and tag it, Dagger can skip
rebuilding layers like   npm install   if the   package-lock.json   hasn’t changed, etc. This not only

speeds up CI but also reduces flakiness (fewer moving parts each run) and cloud cost (time on runners,

network usage for pulling base images, etc.). Essentially, Dagger turns your CI pipeline into an efficient,

incremental process –  “Dagger most definitely saves you from having to deal with GitHub Actions’ always-

blank starting point” by providing its own persistent caching

23

.

For   your   workflow,   enabling   Dagger’s   cache   might   involve   setting   up   Dagger   Cloud   (with   a
DAGGER_CLOUD_TOKEN )  or  using  Depot’s  GitHub  Action  runners  for  best  results.  But  even  without

those, if you use self-hosted runners or a long-lived runner, the Dagger engine can reuse its cache

directory on that machine. The key takeaway is that Dagger will automatically reuse containers and

artifacts between local and CI runs whenever possible, giving a consistent speed boost and fulfilling

the goal of leveraging caching across environments.

5

Replacing or Complementing Docker Compose Workflows

Your monorepo “relies heavily on containerization,” likely using Docker Compose to coordinate multiple

services (databases, app, AI agents, etc.) for local development or testing. Dagger can step in to either
replace Docker Compose with a more flexible, coded solution or complement it by handling builds and

tests in a Compose workflow.

Dagger as a Compose replacement (ephemeral environments):  As shown earlier, Dagger’s  service

containers allow you to declare multi-container setups in code. This covers many use cases traditionally
handled by a   docker-compose.yml . For example, if you currently start your TypeScript frontend, a

Python   API,   and   a   PostgreSQL   DB   via   Compose   for   integration   testing,   you   could   create   a   Dagger

pipeline function that does the same: launch each component as a service container, then run tests

once all are healthy. The advantage is that you can imbue logic – e.g., wait for DB migrations to finish,

seed test data, etc. – using normal code constructs. Also, Dagger services are content-addressed and

isolated,   which   means   if   two   tests   need   the   same   service   they   won’t   spawn   duplicate   containers

unnecessarily (Dagger will de-duplicate by configuration)

14

. They also shut down automatically when

no longer needed, so you don’t have to remember to tear things down

39

.

For instance, you might have something like this in Python Dagger SDK:

@function

def db_service(self) -> dagger.Service:

return (dag.container().from_("postgres:15")

.with_env_variable("POSTGRES_PASSWORD", "test")

.with_exposed_port(5432)

.as_service(healthcheck=["pg_isready"]))

@function

async def integration_test(self) -> None:

db = self.db_service()

# Build/launch the TS app container, bind DB service to it:

app = (dag.container().build("./frontend").with_service_binding("db",

db))

# ... maybe seed DB or run migrations here ...

# Run tests from Python side, connecting to app and DB:

await (dag.container().from_("python:3.11")

.with_service_binding("app", app)

.with_exec(["pytest", "tests/test_full_stack.py"]))

(Pseudo-code for illustration). In this style, Compose’s role is replaced by Dagger coordinating containers.
The Dagger Engine ensures network connectivity (the   .with_service_binding("db", db)   gives
the   app   container   a   host   db   to   reach   Postgres)

  and   can   perform   health   checks   before

41

40

proceeding
code) alongside build steps, and can be invoked easily (one  dagger  command).

. The benefit is that this  dev/test environment is defined in one place (the pipeline

14

Crucially, Dagger-run services can be used both locally and in CI, meaning you don’t need separate

Compose setups for “local vs CI” – the pipeline abstracts that. This unification can reduce maintenance

overhead and errors. As a bonus, Dagger’s service approach can handle dependencies that might be

6

tricky   in   Compose.   For   example,   if   your   AI   agent   service   depends   on   the   data   engineering   code

container being built, the Dagger pipeline can ensure the build happens (using cached layers) and then

run the agent container with the freshly built image in one seamless flow, which is harder to orchestrate

with plain Compose.

Compose interop: If you have existing Docker Compose configurations that you want to keep (say for

Komodo’s use in production), Dagger can still work alongside. One approach is to let Dagger handle all

builds and testing, then output images that your Compose file references. For instance, instead of
using  docker-compose build , you run the Dagger pipeline to build all images and push them to a
registry (or load into the local Docker daemon). Then developers or Komodo can do  docker-compose
up   purely to run containers, pulling the pre-built images. This makes the Compose step much faster

and simpler (no in-line builds) and ensures that the images running have passed through the same CI

pipeline. Essentially, Dagger can guarantee that “built image X passed tests Y and Z” before Compose ever

deploys it.

There are also integration points if needed – the community has created a Dagger Docker module that

can invoke Docker Engine or even Docker Compose commands from within a Dagger pipeline
.
For example, you could write a Dagger function that calls  dag.docker().compose(dir)  to bring up

43

42

. This is a more niche scenario (since it effectively shells out to
services defined in a compose file
Compose inside a container), but it’s available if you, say, want Dagger to leverage an existing Compose

43

42

file directly. However, the more idiomatic pattern is to translate those compose services into Dagger

service containers as described above, for finer control and better caching.

In the context of  Pangolin: Pangolin’s Newt and Olm agents (for zero-trust networking) are currently

distributed as Docker images and often run via Docker Compose in deployments

44

. With Dagger, you

could incorporate Pangolin setup into your pipeline as well. For example, a Dagger function could spin

up  the  Pangolin  “Newt”  container  (using  Pangolin’s  official  image)  as  a  service  while  you  run  some

connectivity tests, instead of relying on a separate compose workflow. Pangolin’s docs mention a one-

line installer or Compose setup for these agents

45

44

; Dagger could automate that by running the

installer inside a container or running the container directly. This would unify even the infrastructure-

level services into the same pipeline. So if your integration tests or deployments require Pangolin’s

tunnel to be active, the pipeline can ensure Newt is running as part of the test stage. In summary, any

scenario where you’d reach for Docker Compose – multi-container orchestration, especially ephemeral/

test instances – can likely be handled in a cleaner way with Dagger’s pipeline-as-code approach.

Integrating with GitHub Actions and Komodo

GitHub Actions Integration:  Dagger plays very well with GitHub Actions (GHA), using minimal glue.
The Dagger team provides an official Action   dagger/dagger-for-github  that you can use in your

workflow YAML to invoke your Dagger pipeline functions

33

. Essentially, your GitHub Actions file can be

boiled down to:

jobs:

ci:

runs-on: ubuntu-latest

steps:

- uses: actions/checkout@v3

- uses: dagger/dagger-for-github@v8.2.0

with:

7

version: "latest"

# e.g., call a Dagger module's function:

module: ./ci-module # (could also be a git URL)

args: test --env=ci

This will install the Dagger CLI and run your specified pipeline (e.g.   test   function) inside the GHA

runner.   All   the   heavy   lifting   (building   images,   running   containers)   then   happens   inside   the   Dagger

engine. The GHA log will show the output of your pipeline steps, and the overall job status will reflect

the pipeline’s success/failure. Compared to a traditional CI setup, the GitHub Actions YAML remains

extremely  slim  – no multi-step complex logic, just a call into Dagger. Solomon Hykes describes it as

replacing a “much larger custom mess of YAML and shell scripts” with a small standardized snippet (“install

Dagger, then run this command”)

46

. This means fewer moving parts in your CI config and more in

your code (under version control and testable).

One thing to plan for is caching on GitHub runners. As discussed, without persistent runners, you’ll

want to use Dagger Cloud or Depot to carry caches between runs. The Dagger for GitHub Action can
seamlessly integrate with Dagger Cloud – you simply provide a  cloud-token  (stored as a secret) and

it will connect the pipeline to the remote cache

47

. Alternatively, if you use Depot’s managed runners,

. Either approach
those runners come pre-connected to a long-lived Dagger engine with caching
will significantly speed up Actions by avoiding redundant builds. In fact, teams using Dagger on GitHub

49

48

Actions often disable GHA’s own cache steps entirely (with their 5GB limits and restore keys) because

Dagger’s caching is more effective

23

.

Your CI/CD process can thus be triggered by GitHub Actions (on push, PR, schedule, etc.), but almost all

the  workflow  logic  lives  in  the  monorepo’s   Dagger   code.   This   decoupling   also   makes   it   CI-platform

agnostic – tomorrow if you switch to another CI, you carry over the same Dagger pipeline. It’s also easy

to extend: for instance, adding a step to publish an artifact or run a security scan is just adding a few

lines to the Dagger code, not editing YAML. And since Dagger pipelines can call out to any tool, you can

integrate things like Komodo CLI or Pulumi commands directly in the pipeline, which brings us to Komodo

integration.

Komodo   Integration:  Komodo   is   your   deployment   tool   that   manages   servers   and   uses   Docker

Compose under the hood for deploying stacks. Integrating Dagger with Komodo will ensure a smooth

code-to-deployment flow:

•

Building Images for Komodo: Komodo deploys “stacks” defined by compose files, which usually

either   build   images   or   pull   them.   You   can   modify   this   so   that   Komodo   always  pulls   pre-built

images  (built   by   Dagger)   from   a   registry.   Dagger   can   take   over   the   image   build   process

completely.   For   example,   your   Dagger   pipeline   can   build   the   TypeScript   app   image   and   the

Python   service   image,   tag   them   (perhaps   with   the   Git   commit   or   version),   and   push   to   a

container registry (e.g., GHCR or AWS ECR). The Docker Compose files used by Komodo can then
refer to these images by tag. This way, Komodo’s deployment step is just   docker-compose
pull && up  on the target servers, which is faster and more reliable than building there. It also

means the exact same artifact that was tested in CI is what goes to production (no “rebuild” on

the server). This practice of Dagger building and Komodo deploying combines their strengths

well.

•

Triggering Deployments: You have a few options on how a Dagger-run in CI triggers Komodo

to   actually   deploy   the   new   images.   One   common   pattern   is   using   GitHub   webhooks   or   the

Komodo API. Komodo supports a GitOps-style webhook trigger – in fact, Komodo’s “Procedure”

8

. If you’re using that, you might
can automatically pull latest Git changes and deploy the stack
adjust it such that a push to a particular branch (say  main ) fires Komodo, but only after the CI

50

pipeline   succeeds.   One   way   is   to   have   the   Dagger   pipeline,   upon   success,   call   a   Komodo
webhook URL (Komodo has a  KOMODO_WEBHOOK_SECRET  for authenticating such triggers)

51

52

. This could be done with a simple HTTP request step inside Dagger (for example, using a

curl container to POST to Komodo’s endpoint). Essentially, Dagger becomes the orchestrator that,

after building and testing, signals Komodo: “deploy stack X with the new images now.”

Alternatively,   if   Komodo   monitors   a   Git   repo   directly,   you   might   arrange   for   Dagger   to   update

something in that repo to prompt a deployment. For instance, some teams version their compose files

or a deployment descriptor – the Dagger pipeline could open a pull request or commit that updates the

image tags in the compose file, which Komodo then notices and deploys. This is more complex, though.

A direct webhook call or CLI invocation is more straightforward.

•

Komodo CLI/API via Dagger: If Komodo offers a CLI or API token, you can embed that in the
pipeline. For example, run a container with  komodo  CLI (if available) to push a new stack config

or to instruct Komodo to update. Since Komodo is open source, it likely has an HTTP API. Dagger

can use a Secret for the API key and invoke that. This way, the deployment step is part of the

pipeline code – after tests pass, the pipeline might do something like:

dag.container().from_("curlimages/curl:latest")

.with_secret_variable("KOMODO_TOKEN", komodo_token_secret)

.with_exec(["curl", "-X", "POST", "-H", "Authorization: Bearer

$KOMODO_TOKEN",

"-d", "@stack.yaml", "https://komodo.yourdomain/api/deploy"])

(hypothetical   example).   The   idea   is   to   integrate   with   Komodo’s   deployment   mechanism

programmatically.

•

Procedures vs Dagger: It’s worth noting that Komodo was designed to automate deployments

on git pushes even without a full CI system, via its  Procedures  (pull code, then deploy)

50

. By

introducing Dagger, you might actually simplify Komodo’s usage: you could use Komodo mainly

as a remote orchestrator of running containers, and offload the build/test pipeline to Dagger.

In practice, this might mean turning off Komodo’s auto-build procedure and replacing it with “CI

(Dagger) builds and pushes images, then notifies Komodo to do a compose up.” This ensures

that only tested artifacts get deployed. It also aligns with Komodo’s philosophy of GitOps – but

with Dagger ensuring the repository is always in a deployable state (tests passed, images built).

•

Pulumi and other Infra: Since your stack also uses Pulumi (perhaps for provisioning cloud infra

or network setup), Dagger can include that as well. For example, you can have a Dagger step
that runs  pulumi up  (using a Pulumi Docker image or installing Pulumi CLI in a container) to

provision   any   required   infrastructure  before  deploying   the   application   containers.   This   can

integrate with 1Password for secrets or other tools as needed – notably, Dagger has integrations

for  secrets  management  (like  1Password  Connect  and  Hashicorp  Vault)

53

  which  could  help

supply cloud credentials to Pulumi in a secure way. By running Pulumi within Dagger, you keep

your   entire   CI/CD   declarative   and   in-repo:   infrastructure   changes   get   applied   in   the   same

pipeline as code changes. This cross-cutting step is safe because the pipeline is code – e.g., you
could write logic to only run  pulumi up  if the Pulumi files changed or if certain environment

triggers are present. Many Dagger users do similar things with Terraform or other IaC, using

Dagger to drive those tools and even to fan out environments.

9

Summary: The integration of Dagger with GitHub Actions and Komodo can be visualized as follows:

1.

GitHub Actions trigger:  A push or PR triggers the GHA workflow, which invokes the Dagger
pipeline (using  dagger-for-github  action). The pipeline code lives in the repo (possibly as a
module like  //ci  directory).

2.

Dagger pipeline execution: Within that pipeline:

3.

Build and test the TypeScript and Python projects using container steps (with caching).

4.

Spin up any necessary services (databases, message brokers, Pangolin Newt/Olm, etc.) to run

integration tests.

5.

Build final Docker images for the services and push them to the registry (tagged with a version).

6.

Possibly run Pulumi to ensure infrastructure (databases, networks, etc. that Pulumi configures) is

up-to-date.

7.

Finally,   deploy   step:   either   trigger   Komodo   or   directly   run   deployment.   For   example,   call

Komodo’s webhook to deploy the updated stack, or use Komodo’s CLI/API to pull the new images

on all servers.

8.

Komodo deployment: Komodo receives the deployment trigger (via webhook or API call), pulls

the new images and applies the Compose stack on the target servers (which could be a group of

VMs   or   a   K8s   via   Komodo,   depending   on   your   setup).   Since   Komodo   excels   at   multi-server

coordination, it can ensure all your servers update the containers, respecting any zero-downtime

settings you have. Komodo’s UI can then show the updated stack running, and it can manage

monitoring, log aggregation, etc., as it normally does

54

  – none of that is disrupted by using

Dagger for the CI portion.

By   uniting   these,   you   get   a  declarative,   end-to-end   CI/CD   pipeline:   code   changes   flow   through

Dagger’s build/test stages and automatically into Komodo’s deploy stage, with GitHub Actions as the

initial trigger and audit log. Everything is containerized and reproducible, and each tool is used for what

it does best (Dagger for build/test logic and caching; Komodo for server orchestration and runtime

management).

Finally, all of this remains extensible and maintainable. Need to add a new microservice (say another

Python agent)? Just add a new Dagger module for it and perhaps a new compose service in Komodo –

the pipeline can build and deploy it in parallel with others. Need to support a new environment or

target? You can parameterize the Dagger pipeline (e.g., dev vs prod deploy) and call it with different

args from Actions or Komodo. The key is that Dagger provides the unifying layer to coordinate your

polyglot tooling in a declarative, cached, and streamlined way.

By adopting Dagger in this monorepo, you can expect: faster local iteration, more reliable and faster CI

builds   (thanks   to   caching   and   eliminating   YAML   errors),   easier   multi-language   coordination,   and   a

smoother   path   to   deployment   that   ties   in   with   Komodo/Compose   rather   than   fighting   them.   This

approach   is   backed   by   public   examples   –   companies   have   seen   dramatic   improvements   (5×   faster

pipelines, happier developers) by moving to Dagger

34

35

, and Dagger’s features (monorepo support,

services, caching) are explicitly designed for these kinds of challenges. The result should be a CI/CD

setup   that   meets   your   goals   of   acceleration,   declarativity,   and   integration   across   your   TypeScript,

Python, and infrastructure stack.

10

Sources:

•

Daniel Gafni, “Cracking the Python Monorepo: build pipelines with uv and Dagger” – monorepo

tooling and Dagger overview

1

55

•

Dagger Documentation – cross-language pipelines, services, GitHub Actions integration, and

caching mechanisms

2

56

31

21

•

Dagger Blog – “Dagger 0.13: First-class Monorepo Support” (context directories and filtering for

large repos)

3

4

; Case studies (Civo, OpenMeter) on monorepo CI and performance

16

27

•

Hacker News (Solomon Hykes comments) – on decoupling pipelines from CI and built-in caching

advantages

7

23

•

Komodo & Pangolin docs – context on using Docker Compose for deployment and how Dagger

can fit into that model

44

50

.

1

~/danielgafni • Cracking the Python Monorepo

https://gafni.dev/blog/cracking-the-python-monorepo/

2

Basics | Dagger

https://docs.dagger.io/getting-started/quickstarts/basics/

3

4

5

6

Dagger 0.13: First-class Monorepo Support, Private Modules, a New CLI Command, and

More | Dagger

https://dagger.io/blog/dagger-0-13

7

23

24

25

46

GitHub Actions is a horrible CI/CD system. You cannot run steps in parallel on t... |

Hacker News

https://news.ycombinator.com/item?id=37616905

8

9

10

11

13

16

34

Adopting a Monorepo Strategy: Civo’s Experience | Dagger

https://dagger.io/blog/adopting-monorepo-strategy

12

21

22

26

27

28

29

30

35

36

37

38

How We Made Our CI Pipeline 5x Faster | OpenMeter

https://openmeter.io/blog/how-we-made-our-ci-pipeline-5x-faster

14

15

17

18

19

20

39

40

41

56

Services | Dagger

https://docs.dagger.io/extending/services/

31

32

33

47

48

49

GitHub Actions | Dagger

https://docs.dagger.io/getting-started/ci-integrations/github-actions/

42

43

docker :: Daggerverse

https://daggerverse.dev/mod/github.com/frantjc/daggerverse/docker@4c9adb6c3337edc1860746ba89cffa614c6e95f5

44

45

Deciding Between Systemd Agents and Docker-Compose for Komodo & Pangolin.pdf

file://file_0000000020a0720a94149aab2041ac55

50

51

52

54

Streamline Your Deployments : Komodo + GitHub Webhooks | by Rishav Kapil | Oct, 2025

| Medium

https://medium.com/@rishavkapil61/streamline-your-deployments-komodo-github-webhooks-51d4d3a04891

53

Dagger | Blog

https://dagger.io/blog

55

Cracking the Python Monorepo: build pipelines with uv and Dagger : r/Python

https://www.reddit.com/r/Python/comments/1iy4h5k/cracking_the_python_monorepo_build_pipelines_with/

11

