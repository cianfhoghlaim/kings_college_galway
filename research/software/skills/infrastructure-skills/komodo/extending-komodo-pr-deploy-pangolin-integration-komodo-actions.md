Extending komodo-pr-deploy for Pangolin

Integration via Komodo Actions

Current PR Preview Deployment Workflow

The  komodo-pr-deploy  repository provides a GitHub Action and Node script that builds and deploys

a pull request branch to a Komodo server, then publishes the deployed service via Pangolin

1

. In

practice, the workflow is as follows:

•

GitHub Action Trigger: On opening or updating a PR (for non-main branches), the GitHub
Action is triggered. It uses a custom Docker image ( komodo-cli:latest ) containing the

•

Komodo CLI/SDK environment
Komodo Build & Deploy: The action runs the  deploy.mjs  script, passing the PR branch name.
This script uses the Komodo TypeScript SDK (imported as  komodo_client ) to authenticate to

. This avoids rebuilding the client on each run.

2

your Komodo instance and invoke its API

3

. It triggers a Docker image build from the PR’s code

and then deploys a container on the specified Komodo server. Komodo handles building the

Docker image (using a connected build agent) and running the container on a target server.

•

Preview Environment: The deployed container is typically given a unique name or tag (often

derived from the branch or PR) and is attached to a network accessible to Pangolin. For example,
the README suggests using a wildcard domain (like  *.example.com ) so that each PR’s app can

be exposed at a unique subdomain

4

. You must ensure the container joins the Pangolin

network (e.g. via Docker network configuration in Komodo) so that Pangolin can route to it.

•

Pangolin Publication: After a successful deploy, the script calls Pangolin’s Integration API to

register the new container as a published resource. It uses Pangolin’s REST API (enabled via

Pangolin’s settings

4

) to create or update a Resource (and associated Target/Rule)

corresponding to the PR deployment. This step essentially tells Pangolin’s reverse proxy about

the container (host/port and domain) so that the service becomes accessible at a URL (e.g.
pr-123.yourdomain.com ). The repository’s configuration confirms this integration: you must
provide  PANGOLIN_URL , an  API_TOKEN , and IDs for the Pangolin Org, Domain, and Site in

the GitHub repository secrets

5

. These secrets allow the script to authenticate to Pangolin and

specify where to register the service.

In   summary,  komodo-pr-deploy  automates   ephemeral   preview   environments   by   using   Komodo   for

Docker   orchestration   and   Pangolin   as   a   zero-trust   access   layer.   The   key   points   in   this   flow   are   the

Komodo API calls (build/deploy) and the Pangolin API call to publish the container.

Inserting Pangolin Registration in the Workflow

To support container registration with Pangolin, the integration point is  immediately after Komodo

reports a successful deployment of the container. At that moment, we know the container is running

and can be registered in Pangolin’s routing system. In the current implementation, the Node script

performs the Pangolin API call at the end of the deployment sequence (after confirming the container is

up). This is the correct place to add or modify logic, since we want to only register with Pangolin once

the service is available.

1

Concretely, after invoking Komodo’s deploy action (e.g.,  komodoClient.deploy(...) ) and receiving

a result (such as a deployment ID or container name), the script should initiate Pangolin registration. In

the existing code, this likely involves constructing an HTTP request to Pangolin’s REST API using the
provided   PANGOLIN_*   secrets.  For  example,  it  would  use  the  Org  ID,  Site  ID,  and  Domain  ID  to
create a new resource via an endpoint like   /v1/orgs/{orgId}/sites/{siteId}/resources   (and

possibly   create   a   Target   if   required   by   the   Pangolin   API).   The   necessary   data   includes   the  target

container’s network address (often the Docker container name or hostname on the Pangolin network)

and the  port  the service listens on. These can be determined from the deployment metadata – for

instance,   if   the   container   runs   on   port   3000   internally,   and   it’s   attached   to   Pangolin’s   network,   the
Pangolin target might be  container_name:3000 . The script would then call Pangolin’s API to register

this target under a new resource, associating it with the wildcard domain. (Pangolin’s docs recommend

using container names on the Pangolin network rather than IPs

6

.)

By   adding   the   Pangolin   registration   step   after   Komodo’s   deployment,   each   preview   environment   is

automatically   made   accessible.   If   this   integration   step   fails   (e.g.,   Pangolin   API   error),   it   should   be

handled gracefully – perhaps by marking the GitHub Action as failed or retrying – because a deployment

without an accessible route isn’t useful. Logging a clear error (without exposing sensitive data) will help

troubleshoot issues with Pangolin.

Using Komodo Actions via the TypeScript SDK (Approach 2)

Komodo   provides   a   powerful   extension   mechanism   called  Komodo   Actions,   which   are   essentially

TypeScript scripts that run on the Komodo server side

7

. These actions have access to a pre-initialized

Komodo API client (no extra auth needed for Komodo itself) and can perform arbitrary tasks – including

interacting with external systems. Approach 2 suggests leveraging this feature: instead of having the CI

script call Pangolin’s API directly, we can invoke a Komodo Action from the CI pipeline to handle the

Pangolin registration.

Feasibility: Yes – the Komodo TypeScript SDK (which the  komodo-pr-deploy  script already uses) can

invoke Komodo Actions. The SDK exposes resources like Action and Procedure, allowing you to trigger an

action   by   ID   or   name.   For   example,   if   you   create   an   action   script   in   Komodo   (let’s   call   it
"RegisterPangolinResource" ),
like
komodoClient.action.run('RegisterPangolinResource',  {  deploymentId:  X,  branch:

something

could

you

call

Y })  from within  deploy.mjs . The GitHub Action’s Docker container includes the  komodo_client

NPM module, which is the Komodo SDK

3

, so it is fully capable of making this additional API call to

Komodo.

Defining the Komodo Action:  You would write a TypeScript script (via Komodo’s UI or config) that

Komodo will execute. This script would encapsulate the Pangolin integration logic. For instance, inside

the action you can:

•

Use the Komodo API (client is injected as  komodo ) to fetch details about the deployment or
container (e.g., get the container name, internal port, etc., using the  deploymentId  or

deployment name passed in).

•

Call Pangolin’s API to register the container. The action can use Node fetch/HTTP libraries to
make REST calls. (Since Komodo Actions run in Node.js, you can  fetch  or import an HTTP

client.) If desired, you could even generate and include a Pangolin API client (TypeScript) for

stronger typing, though this adds complexity. A simpler approach is to call the Pangolin

endpoints directly with fetch or axios, using the known request payloads.

•

Handle success or errors (perhaps logging to Komodo’s logs or sending an alert if it fails).

2

Triggering the Action: Once this action is set up on the Komodo server, the CI pipeline needs to invoke
it after deploying. In the  deploy.mjs  sequence, after the  komodoClient.deploy  returns success,

you insert a call to run the Komodo Action. The SDK likely provides a method or you can use a generic

resource   invocation.   Another   approach   is   to   create   a  Komodo   Procedure  that   contains   the   action

).   You   could   call
(Procedures   can   sequence   actions   and   can   be   triggered   via   API   or   webhooks
komodoClient.procedure.run(<procedureId>)   if using that route. However, simply calling the

8

action directly is straightforward if supported.

By   offloading   Pangolin   registration   to   a   Komodo   Action,   you   gain   a   few   benefits:   -  Separation   of

concerns:  The   CI   script   remains   focused   on   Komodo   operations,   and   Komodo   itself   handles   the

Pangolin   integration.   This   makes   the   logic   more   portable   (e.g.,   if   you   run   a   deploy   manually   via

Komodo’s UI, it could also run the action to update Pangolin). - Secure context: The action runs on the

Komodo server, which means you can store the Pangolin API token and other secrets server-side (more

on   this   below)   instead   of   passing   them   through   the   CI   runner.   -  No   extra   CI   dependencies:  Since

Komodo’s environment already has the TS client and can run the action, the CI container doesn’t need

the Pangolin client or even direct internet access to Pangolin – Komodo will do that.

In short,  Komodo Actions are invokable via the TS SDK, and it’s feasible to call one from within the

GitHub Action’s Node script to perform the Pangolin registration. This aligns with Approach 2, using the
Komodo platform’s built-in automation features instead of custom code in the CI pipeline.

Integrating Pangolin’s API via a TypeScript Client

To interact with Pangolin from either the CI script or a Komodo Action, you have two options:

1.

Direct REST calls: Use  fetch ,  axios , or similar to call Pangolin’s Integration API endpoints.

This is how the current workflow likely operates – by manually constructing HTTP requests with

the Pangolin URL, API token, and JSON payload. Given Pangolin provides a Swagger/OpenAPI

spec for its API

9

, you can craft requests to create resources, targets, etc. This approach is

straightforward and doesn’t require additional dependencies. Just be sure to parse responses

and handle HTTP errors (e.g., if the resource already exists, you may need to update or delete-

and-recreate it).

2.

Generated TypeScript client:  Using Pangolin’s OpenAPI definition (available via Swagger UI),

you could generate a TypeScript client SDK for Pangolin. This would provide typed methods for
operations   like   createResource ,   listResources ,   etc.,   which   could   be   more   convenient

and less error-prone than manual REST calls. There isn’t an official Pangolin NPM package as of

now, but OpenAPI generator or Swagger Codegen can produce a client library. If you choose this

route, you’d include the generated client in your project (or action). For example, the Komodo

Action   script   could   import   a   Pangolin   API   client   module   and   use   it   to   call
pangolinClient.createResource(...)  with strong typing for the request and response.

Both approaches can be made to work inside the GitHub Action or Komodo Action. Given that komodo-

pr-deploy is a fairly small project, the direct REST approach might be sufficient. But for maintainability,

a generated client could help, especially if Pangolin’s API is complex.

Secure integration:  In either case, treat the Pangolin credentials carefully. They should be passed to

the   client   or   requests   at   runtime   via   environment   variables   or   parameters   –   never   hard-coded.   For

instance, if doing direct fetch in Node, you might do:

3

await fetch(`${process.env.PANGOLIN_URL}/v1/orgs/${ORG_ID}/sites/${SITE_ID}/

resources`, {

method: 'POST',

headers: { 'Authorization': `Bearer ${process.env.PANGOLIN_API_TOKEN}` ,

'Content-Type': 'application/json'},

body: JSON.stringify({ name: resourceName, domainId:

DOMAIN_ID, ...targetConfig })

});

If using a generated client, you’d initialize it with the base URL and the token (the token could be set in

an HTTP header interceptor or passed in each call depending on the library design).

By integrating the Pangolin API calls in code (whether CI or Komodo Action), ensure that sensitive data

(token, IDs) are not logged. For example, avoid printing the full HTTP request or token in logs. Only log

high-level statuses or errors.

Note: Pangolin’s integration API must be enabled on the server (in Pangolin’s config) for this to work

10

11

, which the README already calls out. Also verify that the API token has the necessary scope. The
README suggests using a token with broad permissions ( Resource: Allow All ,  Target: Allow
All , etc.)

. For better security, you could create a more restricted token if Pangolin allows granular

4

scopes (e.g., only allow creating/deleting resources on the specific site).

Secure Token and Credential Management

GitHub Secrets: The current setup relies on GitHub repository secrets for all sensitive values – Komodo

API key/secret, Pangolin API token, Docker registry creds, etc.

12

5

. This is a good practice: it keeps

secrets out of the code and in GitHub’s secret storage. You should continue this approach. In the GitHub
Action workflow YAML ( komodo-pr-deploy.yml ), ensure these secrets are mapped to environment
variables   for   the   job   (the   README   shows   that   REPO_NAME ,   KOMODO_URL ,   PANGOLIN_URL ,   etc.,
should be provided, likely via  env  or  with:  in the action configuration).

When extending the repo, do not hard-code tokens or IDs. Instead, document that users must set up
the required secrets. The   .env.example   file already lists the needed variables, which is helpful for

local testing. Just make sure any new variables (if needed for approach 2) are also documented.

Komodo Action secret handling: If you implement the Pangolin call inside a Komodo Action, you have

a choice: - Continue to feed Pangolin credentials from the CI side (meaning the CI calls the action and

passes along the token as a parameter or environment variable), or - Store the Pangolin credentials on

the Komodo server side so that the action can retrieve them internally.

The latter is more secure, because your Pangolin API token wouldn’t traverse the CI pipeline at all.

However,   Komodo   currently   doesn’t   have   a   built-in   secrets   vault   exposed   to   action   scripts   (unlike

environment variables for deployments). One way to do this is to save the Pangolin token as a Komodo

Secret  resource  or  variable  in  Komodo’s   config   (Komodo   supports   defining   variables/secrets   in   its

config or UI that can be referenced in actions or deploys

13

14

). If Komodo allows injecting certain

environment  vars  into  the  Action  execution  context,   you   could   use   that.   This   may   require   checking

Komodo’s docs or updates on Actions.

4

If storing on Komodo isn’t straightforward, it’s acceptable to pass the token from CI to the action. You

could, for example, call the action with a payload containing the token (though you’d want to use HTTPS

and ensure Komodo is using TLS). The action would receive it and use it to call Pangolin. This still keeps

the token out of persistent storage on CI or code, but it does expose it in transit to Komodo (make sure

your Komodo server is secure).

Best   practices:  No   matter   the   approach,   follow   these   guidelines   from   Pangolin’s   documentation:   -

Never commit API keys to version control. Use environment variables or secret managers

15

. - Limit the

scope of tokens if possible. (If Pangolin supports scoping a token to only certain operations or a single

site, use that.) - Mask tokens in logs: Both GitHub Actions and Komodo might log outputs. In GitHub,
any output line containing a secret value is automatically masked (GitHub replaces it with  *** ). Verify
that   your   action   doesn’t   accidentally   echo   the   token   (for   instance,   don’t   console.log   the   whole

environment). In Komodo’s Action, if you do logging, similarly avoid printing the token.

Finally, consider rotation of tokens. For example, if the Pangolin API token is long-lived, have a process

to rotate it periodically and update the secret in CI (and Komodo, if stored there).

Managing Deployment Metadata for Pangolin Integration

When   integrating   Pangolin   registration,   it’s   important   to   manage   how   you   track   the   relationship

between a Komodo deployment (container) and the Pangolin resource:

•

Naming Convention:  Use a clear naming scheme for Pangolin resources, ideally derived from

the   GitHub   branch   or   PR   number.   For   example,   if   your   project   is   “Portal”   and   the   branch   is
“feature-login”, you might name the Pangolin resource  "portal-feature-login"  (or similar).

This name could be used as the subdomain prefix for the URL. In fact, Pangolin’s Domain likely is
a   wildcard   like   *.example.com ,   and   the   resource’s   configured   host   might   determine   the

subdomain. Ensure that whatever name you choose for the Pangolin resource doesn’t conflict

with others. The script can construct this name dynamically each run. (If the branch name has

characters not allowed in DNS, you’d want to sanitize or hash it, or use the PR number.)

•

Idempotent Updates: If the CI run is triggered multiple times for the same PR (e.g., after new

commits), you don’t want to create a new Pangolin resource every time without cleaning up the

old one. Ideally, the preview URL stays the same for the life of the PR. Thus, your integration

logic should check if a Pangolin resource for that branch already exists:

•

If not, create it (first deployment of this PR).

•

If yes, update it. Updating might involve pointing it to the new container instance or simply

doing nothing if the container name/port hasn’t changed. If Komodo reuses the same container

name for redeploys (it might, if it updates the existing deployment), Pangolin might continue

working without changes. But if Komodo creates a new container with a different name each

time, you’ll need to update the Pangolin target to the new name. You can query Pangolin’s API

for the existing resource and then update its target if needed (via a PUT/PATCH).

•

In case of resource name collisions (two PRs with similar names), using unique identifiers like PR

number helps.

•

Cleanup: Plan how to remove Pangolin resources when they are no longer needed. Komodo can

automatically stop or remove deployments when a PR is closed (you might implement this with a

GitHub webhook or manual trigger). Pangolin resources left behind would clutter the system or

5

could pose a security risk if not disabled. A best practice is to delete the Pangolin resource when

the corresponding deployment is removed. If you use Komodo Actions, you could create another

action (or extend the same one) to handle teardown. For example, a  “PR closed”  action might
call  DELETE /resources/{id}  on Pangolin. This could be triggered via the GitHub Actions on

PR  closure  or  via  Komodo’s  webhooks  (Komodo  supports  webhooks  on  deployments

16

).  At

minimum, document a manual cleanup procedure so nothing is forgotten.

•

Storing   Metadata:  It   may   help   to   store   the   Pangolin   Resource   ID   or   name   somewhere

associated with the deployment for easy reference. Since Komodo allows tagging resources

17

,

you could tag the Komodo deployment with the Pangolin resource name or ID. This way, if you

need   to   find   the   Pangolin   resource   for   a   given   container,   you   can   look   at   the   Komodo

deployment’s tags. Conversely, Pangolin might allow tagging or adding a note that includes the

PR   number   or   branch.   Using   these   annotations   prevents   confusion   when   multiple   preview

environments exist.

•

Network configuration:  Ensure the Docker container is accessible to Pangolin. Typically, that

means the container must be on the same Docker network that Pangolin uses to find back-end

services. You might have configured this in Komodo’s deployment settings (e.g., Komodo might

allow specifying a network in the deployment environment or Docker arguments). Double-check

that the deployed container has no published ports to the host (not needed if Pangolin connects

internally) and is only reachable via Pangolin’s secured tunnel network (improving security by

closing open ports).

Recommendations for Repository Changes

1.

Create   a   Komodo   Action   for   Pangolin:  Develop   a   Komodo   Action   script   (TypeScript)   that

handles registering a deployment with Pangolin. You might place the source for this in your

repository (for reference or for use with Komodo’s config-as-code) or at least document how to

create it in Komodo’s UI. This action should use environment variables or Komodo config for
PANGOLIN_API_TOKEN ,   etc.,   rather   than   hard-coding   values.   Test   this   action   manually   with

sample inputs to ensure it can create and update resources correctly.

2.

Modify   deploy.mjs :  After the Komodo deployment step, add logic to call the new Komodo

Action via the SDK. For example, using the Komodo client instance:

if (deploySuccess) {

console.log("Deployment succeeded on Komodo, registering with

Pangolin...");

await komodo.actions.run("RegisterPangolinResource", {

deploymentName: myDeployName,

branch: branchName

});

}

(The exact method may differ; consult Komodo SDK docs for running actions or use Komodo’s

REST API to trigger it.) Pass the minimal info the action needs – probably an identifier for the

deployment or container and the desired domain name.

6

3.

Add Configuration for Komodo Action (if needed): If Komodo requires the action to be defined

via its configuration (toml or UI), include instructions in the README. E.g., “Create a Komodo

Action with the following script…”. If using Resource Sync (Git-based config), you could include an

example in the repo.

4.

Securely handle secrets:  In the GitHub Action workflow file, ensure all necessary secrets are
exposed   as   env   vars.   You   might   add   PANGOLIN_API_TOKEN   if   not   already,   and   any   new

variables   for   approach   2   (though   likely   the   same   set   is   used).   If   moving   token   storage   to

Komodo, you’d remove the need to expose the token to GitHub – instead, the CI would trust

Komodo to know it. In that case, update documentation to reflect storing the Pangolin token on

the Komodo server (e.g., via an environment variable in the Komodo container or a config file
mounted at  /config/config.toml  with an entry for the token).

5.

Documentation and Example:  Update the README to explain the new flow. Mention that the

Pangolin registration is now done via a Komodo Action (Approach 2) and highlight any setup

steps for it. For example, note that Komodo  Procedures and Actions can orchestrate complex

flows

8

, and that we leverage that to integrate with Pangolin. Reiterate any prerequisites, like

Pangolin’s Integration API being enabled and the token permissions (as already described in the

README)

4

.

6.

Testing and Error Handling: It’s wise to test the extended workflow on a sample PR. Monitor the

GitHub Action logs – you should see the Komodo build, deploy, then a call to run the Komodo

action. If the action fails, make sure the GitHub step surfaces that (the script can exit non-zero or

throw). On the Komodo side, test that the action indeed creates the Pangolin resource and that

the PR app becomes reachable. Also test tearing down: when you close the PR and Komodo

removes/stops the deployment, does Pangolin remove the route? If not automated, consider

adding a job to do so.

By implementing these changes, komodo-pr-deploy will be extended to use Komodo’s own automation

hooks   to   perform   Pangolin   container   registration.   This   Approach   2   offers   a   cleaner   separation   and

potentially more secure handling of credentials. All API tokens remain secured in env vars or Komodo’s

secret   store,   consistent   with   best   practices   (never   checked   into   Git)

15

.   The   deployment   metadata

(branch   names,   container   names,   etc.)   is   systematically   used   for   consistent   preview   URLs   and   easy

cleanup.   Overall,   this   makes   the   preview   deployment   process   more   robust   and   maintainable,   while

leveraging the full capabilities of both Komodo and Pangolin.

Sources:

•

Komodo PR deploy README (T. Müller) – repository overview and requirements

1

5

•

Komodo Documentation – on Actions and extending via TypeScript SDK

7

•

Pangolin Documentation – enabling Integration API and API key management

10

15

1

2

3

4

5

12

16

GitHub - tobiasmllr/komodo-pr-deploy: Github Action to build and deploy a

pull-request branch to komodo, then publish via pangolin

https://github.com/tobiasmllr/komodo-pr-deploy

6

Portainer and Pangolin : r/PangolinReverseProxy - Reddit

https://www.reddit.com/r/PangolinReverseProxy/comments/1ncmute/portainer_and_pangolin/

7

7

8

13

14

17

Resources | Komodo

https://komo.do/docs/resources

9

10

11

15

Integration API - Pangolin Docs

https://docs.pangolin.net/manage/integration-api

8

