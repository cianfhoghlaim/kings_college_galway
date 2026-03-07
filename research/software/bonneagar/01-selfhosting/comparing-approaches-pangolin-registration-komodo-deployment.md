Comparing Approaches for Pangolin

Registration after Komodo Deployment

Deploying   a   containerized   service   with  Komodo  and   then   registering   it   with  Pangolin  (a   tunneled

reverse   proxy)   can   be   achieved   via   three   main   methods.   Many   in   the   self-hosting   community   are

interested in seamless  Komodo + Pangolin integration

1

. Below, we compare the three approaches

side-by-side and evaluate them on key criteria:

•

Approach 1: Komodo Post-Deploy Shell Script calling the Pangolin API

•

Approach 2: Komodo Action (TypeScript SDK) to call Pangolin programmatically

•

Approach 3: Komodo Procedure combining deployment & Pangolin registration in one

workflow

Comparison of Registration Methods

Criterion

Approach 1 – Post-

Approach 2 – TypeScript

Approach 3 – Komodo

Deploy Shell Script

SDK Action

Procedure Workflow

Minimal – uses shell/

Integration

OS script outside

with

Komodo’s TS API. Not

TypeScript &

directly in TS code, so

CI/CD

extra glue needed to

invoke it.

High – fully in TypeScript.

Use Komodo’s TS SDK and

Pangolin’s TS client in one

code flow, fitting well into

Node.js automation

2

.

Moderate – implemented

within Komodo’s UI/

engine (Procedure +

possibly a TS Action). Less

external code; can be

triggered via Komodo

(button, webhook) or via

Komodo API.

Good – Komodo tracks

each step in a Procedure.

Basic – must parse

Robust – leverage try/

Deployment and

script exit codes/logs

catch and logging in TS.

registration steps have

manually. Limited

Can programmatically

statuses; failures halt the

Error

visibility in Komodo

react to failures (e.g. retry

procedure. Console

Handling &

UI (unless captured

or rollback). If run in an

output from an Action or

Observability

as a Komodo “Repo”

external process, logs go

script is visible in

run)

3

. Errors could

to CI pipeline; if as a

Komodo’s logs. Less

be missed if script

Komodo Action, console

flexible branching on

fails silently.

logs appear in Komodo UI.

error (unless coded in the

Action), but clear success/

failure reporting.

1

Criterion

Approach 1 – Post-

Approach 2 – TypeScript

Approach 3 – Komodo

Deploy Shell Script

SDK Action

Procedure Workflow

Security &

Secret

Management

Flexibility &

Reusability

Requires careful

handling of Pangolin

API token. Likely

stored as a Komodo

secret variable or on

the host. Can inject

the token into the

script’s env via

Komodo’s secret

interpolation

(keeping it hidden

from logs/users)

4

5

. The script will

use a scoped

Pangolin API key

(with minimal

permissions) for

safety

6

.

Fair – A shell script

can be written to

handle different

services if it accepts

parameters (e.g.

service name,

domain). However,

you must attach or

invoke this script for

each deployment. It’s

not inherently

reusable across

many stacks unless

manually

orchestrated (e.g. a

separate script call in

CI for each service).

Needs Pangolin token in

the app environment (e.g.

an env var or secret in CI).

If running as Komodo

Action, Komodo’s API

doesn’t expose secret

values easily

7

, so you

might resort to passing it

as a plain variable or

config file. External TS

code can load the token

from a vault or env. In all

cases, use Pangolin’s

scoped API key for least

privilege

8

.

High – Code can be

designed to deploy

multiple services and

register each in a loop, or

be called with parameters

for different services/

environments. The TS SDK

and Pangolin client allow

building a generic

deployment function. Easy

to integrate into existing

automation workflows or

pipelines for multiple

stacks.

Can leverage Komodo’s

built-in secrets

management. For

example, store the

Pangolin API token as a

secret variable in

Komodo (or in a

periphery agent’s config)

and interpolate it into the

registration step securely

5

. The Procedure’s

custom Action or script

can read it without

exposing it in logs.

Overall secure, as the

token stays within

Komodo/Pangolin

systems.

Good – The Procedure

can define a generic

workflow (e.g. deploy a

stack then register it). You

can potentially use

wildcards or patterns to

target multiple stacks in

one procedure stage

9

10

. For multiple

environments, you might

create one procedure per

environment or use

Komodo tags to select

targets. Reusability is

decent, though updating

the workflow (e.g. adding

a step) means editing the

procedure in Komodo (or

its TOML config).

2

Criterion

Approach 1 – Post-

Approach 2 – TypeScript

Approach 3 – Komodo

Deploy Shell Script

SDK Action

Procedure Workflow

Maintenance

& Scalability

Script logic is

separate from

Centralized code – easy to

It’s visual and declarative

Workflow is maintained in

Komodo’s configuration.

Komodo, so updates

maintain in one place (e.g.

(stages/actions defined in

require editing the

a Node project or within

UI or TOML) – easier to

script (in a repo or on

Komodo Action). New

manage for ops teams

each host). If you

Pangolin features or API

not wanting to dive into

have many services,

changes can be handled

code. Updating it (e.g. to

you’ll need to ensure

by updating the TS client

change how registration

each uses the latest

or code logic. Scales well:

is done) means editing

script version.

one script can orchestrate

the procedure’s steps or

Scaling to many

any number of

the embedded Action

deployments could

deployments/registrations

code. For many services,

mean managing

sequentially or in parallel.

the Procedure approach

many script triggers.

However, running a large

scales by grouping

On the plus side, the

number of deployments

operations (e.g. “deploy

script is simple and

via one process may

all stacks with tag X, then

environment-

require careful coding

register each”) but

agnostic, but that

(async handling, rate

complex logic may be

also means Komodo

limiting calls to Pangolin,

harder to express.

doesn’t “know” about

etc.).

it.

Komodo itself handles

the parallelism/

sequencing for you

11

.

Approach 1: Post-Deploy Shell Script calling Pangolin API

This   method   relies   on   a   shell   script   (or   similar)   executed   after   Komodo   deploys   the   container.   For

example, one might configure a Komodo Repo resource that contains a script and run it on the target
. The script would use an HTTP client (e.g.  curl  or a small program) to call

server post-deployment

3

Pangolin’s   Integration   API   and   register   the   new   service.   Pangolin’s   REST   API   supports  all   the

operations   available   in   its   UI  (including   creating   “sites”   for   new   services)

12

,   so   the   script   can

automate exposing the service.

Ease   of   Integration:  This   approach   is   relatively  low-level.   It   does   not   integrate   with   Komodo’s

TypeScript SDK at all – it’s an external step. If you already use TypeScript-based automation, invoking a

separate shell script adds a layer of complexity. You might trigger the script via a CI pipeline or Komodo

webhook, but the logic lives outside of your TypeScript code. This makes the flow less cohesive if your

deployment logic is otherwise in code.

Error Handling & Observability: With a post-deploy script, error handling is mostly manual. The script

should   exit   with   non-zero   status   on   failure   so   that   whatever   triggers   it   can   detect   the   error.

Observability is limited: you may need to log to a file or stdout and then inspect those logs. If run via

Komodo’s   Repo   mechanism,   Komodo   will   capture   the   output   and   status   (as   it   does   for   any   script

execution on a server)

3

, which helps centralize logs. However, you won’t have structured, high-level

error info – just whatever the script prints. There’s no built-in retry or complex logic unless you code it

into the shell script.

3

Security & Secret Management: The script needs the Pangolin API key to authenticate with Pangolin.

Storing and passing this secret is a concern. Komodo allows defining secure variables that won’t appear

in logs or UIs
. You can store the Pangolin token as a Komodo secret and inject it into the script’s
environment (e.g. via  [[TOKEN_NAME]]  interpolation in the Repo execution config). Another security

4

best practice is to use Pangolin’s scoped API keys – e.g., a key restricted to creating a specific site or

operating within one organization

13

. That way, even if the key is exposed, its misuse is limited. One

advantage of running a script on the Komodo periphery agent is that you can keep the secret local to

that server. Komodo supports mounting a secret config on the periphery so that the token is available

only on that host and never sent over the network

5

. This approach can thus be made secure, but it

requires conscious setup of secrets management.

Flexibility   &   Reusability:  The   shell   script   approach   can   be   as   flexible   as   you   make   it,   but   it’s   not

inherently modular. You might write the script to accept arguments (like service name, internal URL,

desired domain) so it can register any service. This makes it somewhat reusable across multiple services

or environments – you’d just call it with different parameters. Still, you need to arrange for it to run after

each deployment. If you have 10 services, you’ll either run the same script 10 times (with different args)

or have 10 nearly identical scripts configured. There’s no built-in loop or multi-target capability (unlike

Komodo’s   Procedure,   which   can   target   multiple   resources   with   one   action   stage

9

).   So,   reuse   is

possible but not automatic – you must orchestrate it.

Maintenance & Scalability:  A standalone script is easy to understand in isolation, but maintaining it

across many services or over time can be cumbersome. If Pangolin’s API changes or you need to add

new features (say, setting up access controls or health checks via API), you’ll update the script and

ensure all deployments use the new version. In a small setup, this is fine; in a large environment, you

might end up with configuration drift if each service had its own copy. Scaling out means making sure

every   deployed   stack   triggers   the   script   –   possibly   via   CI   jobs   or   webhooks.   There   isn’t   a   central

“controller” besides your external automation. On the plus side, this decoupling means Komodo itself

remains unaware of Pangolin, so Komodo updates won’t affect the script. But overall, as you scale the

burden is on you to consistently apply the script everywhere.

Approach 2: Using Komodo Actions via TypeScript SDK

This approach uses Komodo’s TypeScript capabilities (either through the Komodo Action resource or an

external script using the Komodo SDK) to perform the Pangolin registration in code. Komodo provides a

TypeScript   client/SDK   (published   on   NPM)   that   you   can   use   to   script   operations

2

.   We   assume

Pangolin also has a TypeScript client (generated from its OpenAPI), which lets you call Pangolin’s API

from Node.js instead of using raw HTTP calls. Essentially, you’d write a  TypeScript script  that does:

deploy the Docker Compose stack via Komodo’s API, then call Pangolin’s API to register the service. This script

could run externally (e.g. as part of a Node.js application or CI pipeline) or potentially as a Komodo

Action (Komodo can run user-defined TS code within its UI)

2

.

Ease of Integration: For anyone already automating with TypeScript, this method is very natural. You

can orchestrate everything in one language and process. The Komodo TS client gives you programmatic
control of deployments (e.g.  komodoClient.deployStack(stackConfig) ), and right after that you
can   invoke   Pangolin’s   registration   (e.g.   pangolinClient.createSite({...}) ).   Because   Komodo

Actions   run   with   an   already-authenticated   client

2

,   if   you   execute   this   within   Komodo’s   Action

framework you don’t even need to handle Komodo API keys. If running externally, you’d use an API key

for Komodo as well. In summary, this approach cleanly fits into CI/CD pipelines or custom deployment

scripts. No context-switching to bash scripts – everything stays in code, which improves maintainability.

4

Error Handling & Observability: Using a high-level language like TypeScript means you can implement

robust error handling. For example, you can catch errors from the Komodo deployment call or the

Pangolin API call and decide how to respond – maybe retry the Pangolin registration a few times if it

fails, or roll back the deployment if registration fails. You can log detailed messages or even send alerts

from the code. If this runs in a CI system or as a CLI, you’ll see the output in the job logs. If run as a
Komodo Action (triggered via Komodo’s UI or API), any   console.log   output or thrown error will

appear in Komodo’s interface for that Action run. This gives  better observability  than a silent shell

script – you have one place (the script’s output or Komodo’s Action logs) to check what happened at

each step.

Security & Secret Management: In a TS program, you will need to supply the Pangolin API token to the

code. Typically, this is done via environment variables or a secrets store integrated with your CI (for

external runs). That can be quite secure if your CI has proper secret masking. If you execute the code

inside Komodo (as an Action), accessing secrets is a bit trickier. Komodo’s API does not return secret

values   even   to   admin   users
,   which   means   your   Action   script   can’t   simply   call
komodoClient.readSecret("PANGOLIN_TOKEN")  unless you stored it as a non-secret variable (not

7

recommended).   A   workaround   is   to   pass   the   token   in   via   configuration:   for   instance,   store   it

unencrypted as a normal variable in Komodo (only visible to admins) or read it from a mounted config

file. These are not ideal, so many would prefer to run this TS code in an external context where your
secret management is under your control. In both cases, using Pangolin’s scoped API keys remains

important – you give the script a minimally privileged token

8

. The advantage here is that no token

touches any disk in plain text (it’s in memory in the Node process), and if using Komodo Action, you

avoid   network   transmission   of   the   token   by   possibly   having   it   in   Komodo’s   config.   Overall,   the   TS

approach can be made as secure as your secret storage practices allow.

Flexibility & Reusability:  This method shines in flexibility. As a developer, you can abstract the logic
into functions – e.g. a function  deployAndExpose(stackConfig, pangolinOptions)  that you call

for each service. Your script can read configuration files or accept input (like a list of services to deploy).

This makes it easy to extend to multiple environments or dozens of services. If you need to target

different Pangolin instances (say dev vs prod Pangolin servers), your code can handle that with different

API   endpoints   or   tokens   per   environment.   Essentially,   you   have  full   programming   capabilities,   so

loops, conditionals, and dynamic adjustments are all on the table. Reusing this across projects is as

simple as sharing the script or packaging it as a small CLI tool. Compared to Approach 1, there’s less

manual per-service setup – you could deploy N services by calling your function N times, or by iterating

through a config. The integration in a single script also means you don’t forget the Pangolin registration

step; it’s part of the defined workflow every time.

Maintenance   &   Scalability:  With   everything   in   code,   maintenance   becomes   a   standard   software

development   task.   You   keep   the   deployment+registration   script   in   version   control.   If   Pangolin’s   API

changes (for example, they introduce a new required field for creating a site), you update your TS client

or API calls in one place. If Komodo’s SDK updates, you update your dependency. There’s a bit of an

implicit dependency on two APIs (Komodo and Pangolin), but both are stable and versioned. As your

infrastructure   grows,   this   approach   scales:   you   can   incorporate   threading   or   asynchronous   calls   to

handle multiple deployments in parallel, or integrate backoff and rate limiting if needed. One thing to

consider   is  observability   at   scale  –   if   you   deploy   many   services   at   once   via   code,   make   sure   to

structure logs or use a monitoring system to track each deployment’s status. Overall, this approach is

highly maintainable  for those comfortable with coding, since it centralizes the logic and leverages

familiar development workflows (linting, testing, etc.). It reduces the “snowflake” configurations on each

Komodo instance in favor of one orchestrator script.

5

Approach 3: Komodo Procedure (Deployment + Pangolin

Registration Workflow)

Komodo’s Procedure resource offers a way to chain multiple actions into a repeatable workflow

14

. In

this approach, you create a Procedure that encapsulates the deployment of the stack and the Pangolin

registration as a single process. For example, the Procedure could have Stage 1 with a  DeployStack

execution,   and   Stage   2   with   a   custom  Action  (TypeScript   script)   or   a  RunRepo  execution   that   calls

Pangolin.   Komodo   will   ensure   Stage   1   completes   successfully   before   moving   to   Stage   2

15

.   This

effectively bundles the two steps so that end-users (or automated triggers) can execute one Procedure

and   accomplish   both   tasks.   It’s   a   more  declarative/orchestrated  approach,   living   inside   Komodo’s

environment.

Ease   of   Integration   with   TypeScript   &   Automation:  This   method   is  integrated   into   Komodo’s

ecosystem rather than an external script. You might still write some TypeScript (for the Pangolin call) as

an  Action  within the procedure, but that code is managed in Komodo’s UI and stored in Komodo’s

database. If your goal is to have everything automated via code, you can trigger the Procedure via

Komodo’s API or webhooks. From an automation perspective, it’s one API call (like “run procedure X”) to

deploy and register, which is convenient. It’s not as flexible as writing your own TS script from scratch,

but   Komodo’s   design   makes   it   pretty   straightforward   to   integrate   –   especially   if   you   already   use

Komodo’s UI or GitOps sync. In TypeScript terms, you might not be writing a full external program, but

the Action in Stage 2 is indeed TypeScript, so you still have the power of TS for the Pangolin API call if

needed

16

. Overall, it’s a middle-ground: less coding overall (because Komodo handles the deploy step

with a built-in action), but also less custom-tailored than Approach 2. It fits well if you want a low-code

solution that’s still programmable.

Error Handling & Observability: Komodo’s Procedure gives you a clear view of each stage’s outcome. If

the stack deployment fails (Stage 1), the Procedure will stop and you’ll see that failure in the Komodo UI

(and any configured alerts). This prevents moving on to Pangolin registration if the container didn’t

come up. If the custom Pangolin registration step (Stage 2) fails, that too is logged and marked as

failed, and the Procedure overall is marked failed. However, handling errors within the Pangolin step

may   require   writing   defensive   code   in   the   Action   –   e.g.   catching   an   error   from   Pangolin’s   API   and

deciding   to   retry   or   abort   gracefully.   You   could   even   use   the   TS   Action   to   roll   back   the   stack   if

registration   fails   (by   calling   Komodo’s   API   to   remove   the   stack),   though   that   adds   complexity.

Observability is quite good: all logs from the Action (like API responses or debug info) can be printed to

Komodo’s console and viewed in the UI. Each execution of the Procedure is recorded, so you have a

history of deployments and registrations in one place. What you sacrifice is some flexibility in error

handling logic (since the Procedure is mostly linear). But for most cases (where you just need “deploy,

then register”), the transactional nature of a Procedure is sufficient and nicely visible.

Security & Secret Management: Since the Procedure runs entirely within Komodo’s domain, you can

take   advantage   of   Komodo’s   secret   management   for   passing   the   Pangolin   API   token.   A   common

pattern would be: store the Pangolin token as a secret variable in Komodo Core (or as a periphery

secret on the agent if you want it tied to a specific host)

17

5

. In the Procedure’s Action script (the

Pangolin registration step), you can inject this token. One way is to define an environment variable for
the Action execution like  PANGOLIN_TOKEN=[[MY_PANGOLIN_TOKEN_SECRET]] . Then in the TS code,
read   it   via   Deno.env.get('PANGOLIN_TOKEN')   or   similar   (Komodo’s   Action   uses   Deno,   which

supports secure env access). This keeps the secret out of code and logs. Because the token lives in

Komodo, it’s protected by Komodo’s access controls (only admins can modify secrets, and values aren’t

shown)

4

. The Pangolin API key should still be scoped to limit its powers

13

. The benefit here is you’re

not   spreading   the   secret   to   external   systems   –   it’s   only   in   Komodo   and   Pangolin.   Also,   if   using   a

6

periphery agent secret, the token doesn’t even traverse the network: Komodo’s core doesn’t see it, only

the agent on the Pangolin host does

5

. Overall, Approach 3 can be very secure with the proper secret

configuration.

Flexibility & Reusability:  The Procedure approach is  highly reusable within the Komodo context.

Once you create a procedure (say “Deploy and Expose Service”), you or your team can run it any time,

and it will do the same steps reliably. If it’s parameterized by naming conventions or tags, you could use

one procedure to handle multiple services. For example, Komodo supports wildcard patterns for batch

actions

9

  – you could have the procedure’s deploy step target a family of stacks (like all stacks in a

project). However, this might be more static than writing a loop in code. You may end up creating a

separate procedure per service group or per environment to tailor the Pangolin settings (e.g. different

domain   names   per   environment).   Reusability   across   multiple   environments   is   possible:   you   might

define identical procedures in each Komodo instance (dev, staging, prod) if you run separate Komodo

servers, or one procedure that deploys to different servers based on input. Komodo doesn’t currently

allow arbitrary user input into procedures at runtime, so you rely on pre-defined configurations. In

practice, you might maintain the procedure definition as code (Komodo allows exporting resources as

TOML) and deploy that to each environment’s Komodo via GitOps

18

. This is a bit heavy, but it ensures

consistency. In summary, for teams that live in the Komodo UI, this approach is very convenient and

reusable; for those who prefer code, it’s another config to manage (less flexible than pure code, but

more structured).

Maintenance & Scalability: Maintaining a Komodo procedure is mostly about updating the workflow

when   necessary.   If   your   deployment   process   changes   (say   Komodo   gets   a   new   deploy   feature   or

Pangolin adds a new required field), you’d edit the Procedure: possibly updating the Action’s TS code or

the execution parameters. This can be done in the Komodo UI or via editing the TOML in a Git repo if

using   Resource   Sync.   It’s   a   bit   more   of   a   configuration   management   task   than   a   coding   task.   One

potential downside is that if you have many procedures (e.g. one per service or per team), updating all

of them could be effort – although you could script that with Komodo’s API if needed. In terms of

scalability,   Komodo’s   procedure   can   orchestrate   multiple   actions   in   parallel,   which   is   a   plus   for

deploying many services at once. You could, for instance, have a stage that registers multiple services in

Pangolin   concurrently   if   they   are   independent.   The   sequential   stage   design   ensures   that   you   don’t

overload Pangolin or Komodo unintentionally – you can control how much runs at once. The scaling

limit will be what Komodo and Pangolin can handle, but those are quite robust for moderate loads. One

consideration:   procedures   reside   in   the   Komodo   service,   so   if   Komodo   is   down   or   busy,   your

deployment+expose workflow is unavailable. An external script (Approach 2) might be decoupled and

run from elsewhere. But assuming Komodo is your central orchestration tool, having the procedure

defined there is logical. Maintenance is simplified by the fact that  the process is documented and

versioned in Komodo, not scattered across scripts. It aligns with a GitOps philosophy if you export the

config  –  you  treat  the  deployment  workflow  as  declarative  config,  which  can  be  very  clean  as  your

infrastructure scales.

Recommendations: Choosing the Right Approach

Each approach has its merits, and the best choice can depend on your team’s skills and the environment

in which you’re operating. Here are some guidelines for when to use each:

•

Use   Approach   1   (Post-Deploy   Script)  if   you   need   a  quick,   ad-hoc   solution  or   have   a   very

simple environment. This works well for a one-off integration or a scenario where you don’t want

to invest in coding. For example, if you’ve already deployed a service via Komodo and just need

to call Pangolin’s API once, writing a small shell script might be fastest. It’s also viable when you

7

cannot modify the Komodo instance much (no custom actions) – e.g., in a restricted environment

where you can only deploy stacks and run basic scripts. However, for long-term and multiple

services, this method gets harder to manage. Treat it as a simple glue for straightforward cases

or prototyping.

•

Use Approach 2 (TypeScript SDK & Actions) if you desire a robust, code-driven workflow and

have the resources to maintain a script or application. This is ideal for teams who already employ

TypeScript   for   infrastructure   automation   or   CI/CD.   For   instance,   in   a   continuous   deployment

pipeline,   you   can   incorporate   this   script   to   automatically   deploy   a   new   service   version   and

update Pangolin. It offers the best control over error handling and logic. Choose this when you

need to integrate with other systems as well – e.g., update a database or call another API as part

of deployment – since it’s easy to extend the script. Also, if you plan to open-source or share your

deployment tool, a TypeScript CLI could be more accessible to others than a Komodo-specific

procedure. In short, Approach 2 is best for maximum flexibility and integration into existing

dev   workflows.   Just   ensure   you   are   comfortable   managing   API   keys   and   that   you   test   the

automation thoroughly.

•

Use   Approach   3   (Komodo   Procedure)  if   you   want   a  streamlined,   “low-code”   pipeline

maintained largely within Komodo’s interface. This is great for operator-centric scenarios: for
example, if you have an ops team that prefers using Komodo’s GUI to trigger deployments, a

one-click procedure that does everything is very attractive. It’s also a good choice when you

foresee using the same deployment+registration pattern frequently – you set it up once and

anyone can reuse it. If you value having the deployment audit trail and configuration in one

place (Komodo’s database) rather than spread across external scripts, this approach delivers that

cohesion. Additionally, if you plan to leverage Komodo’s webhook triggers (e.g., auto-deploy on

git push)

19

, a procedure can be the target of a webhook, thereby achieving a full GitOps style

automation with Pangolin steps included. Go with Approach 3 when consistency, security, and

ease of use by multiple team members  is a priority, and when you’re okay with the slight

rigidity of a predefined workflow. It may require a bit more upfront setup in Komodo, but once in

place, it’s very scalable for routine use.

In many cases, you might even combine approaches. For example, you could use a Komodo Procedure

(Approach 3) and trigger it via an external TS script (Approach 2) for the best of both – the heavy lifting

defined in Komodo, but orchestrated as part of a larger code-driven pipeline. However, if we consider

each   in   isolation,   the   recommendations   above   should   help   you   pick   the   method   that   fits   your

circumstances:

•

Small-scale or exploratory deployment: Approach 1 for simplicity.

•

CI-integrated, code-centric deployment: Approach 2 for control and power.

•

Team-oriented, repeatable operations: Approach 3 for consistency and safety.

By evaluating these dimensions – integration, error handling, security, flexibility, and maintainability –

you can select the approach that aligns with your needs. In summary,  Approach 2  often appeals to

developers looking for a programmable solution, Approach 3 appeals to DevOps/SRE folks looking for a

reliable push-button workflow, and  Approach 1 is there for quick fixes or very simple use cases. Each

can achieve the end goal of  deploying Compose stacks via Komodo and exposing them through

Pangolin, but with trade-offs in effort and complexity as outlined above.

Sources:

•

Komodo Documentation – Resource Types (Procedures, Actions, Repo, etc.)

20

3

8

•

Komodo Documentation – Using Variables and Secrets

4

5

•

Pangolin Release Notes – Integration API availability and usage

12

•

Community Discussion – Interest in Komodo–Pangolin integration

1

1

6

8

12

13

19

Pangolin 1.4.0: Auto-provisioning IdP users and integration API now available for

everyone! : r/selfhosted

https://www.reddit.com/r/selfhosted/comments/1klp8sq/pangolin_140_autoprovisioning_idp_users_and/

2

3

14

15

16

20

Resources | Komodo

https://komo.do/docs/resources

4

5

7

17

Variables and Secrets | Komodo

https://komo.do/docs/resources/variables

9

10

11

18

Procedures and Actions | Komodo

https://komo.do/docs/resources/procedures

9

