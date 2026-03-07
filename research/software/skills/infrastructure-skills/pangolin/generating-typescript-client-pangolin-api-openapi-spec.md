Generating a TypeScript Client for the Pangolin

API from its OpenAPI Spec

Obtaining the Pangolin API OpenAPI Specification

Pangolin’s Integration API is documented via a Swagger UI that serves an OpenAPI (Swagger) spec

1

2

. To generate a client, you first need to fetch this OpenAPI specification (a JSON or YAML document

describing the API). There are a few ways to get it:

•

From   the
https://api.pangolin.net/v1/docs

live   Swagger   UI:  Navigate   to   the   Pangolin   API

  docs   at

(the   interactive   Swagger   UI)

2

.   Using   your

browser’s developer tools, you can find the URL of the OpenAPI JSON. Swagger UIs often have a
“Download” or “Raw” button for the spec. If not, look for network requests to  .json  or  .yaml
– for Pangolin, the spec may be available at an endpoint like   /v1/openapi.json   or similar.
Save this JSON/YAML file (e.g., as  pangolin-api.json ) for use in code generation.

•

From   a   self-hosted   instance:  If   you   are   running   Pangolin   Community   Edition,   enable   the
Integration   API   (set   enable_integration_api:   true   in   config)   and   access   your   own
Swagger   UI   at   https://api.<your-domain>/v1/docs

.   You   can   then   retrieve   the

3

OpenAPI spec from your instance’s docs (the path to the JSON spec should be similar).

•

From  source  code  or  repository:  The  Pangolin  GitHub  repositories  ( fosrl/pangolin   and
fosrl/docs-v2 ) do not directly include a static OpenAPI spec file – the spec is generated at
build time (note that  config/openapi.yaml  is in the gitignore)

. This means you won’t find

4

a checked-in spec in the repo. Instead, you’d generate it by running the server or using the

Swagger UI as above. In short, use the live docs or a running Pangolin instance to obtain the

spec.

Choosing a TypeScript OpenAPI Generation Tool

There are several tools that can turn an OpenAPI spec into a  strongly-typed TypeScript client. The

best options for a Node.js/Bun environment include:

•

OpenAPI Generator (CLI):  The OpenAPI Generator is a versatile Java-based tool that supports

many languages. It has generators for TypeScript, including  TypeScript-Fetch  (which uses the

Fetch API) and  TypeScript-Axios  (which uses Axios)

5

. For example, using the CLI you could

run
openapi-generator-cli   generate   -i   pangolin-api.json   -g   typescript-axios   -

o ./src/pangolinClient . This will produce a TypeScript SDK with classes and methods for

each Pangolin API endpoint. OpenAPI Generator is very robust and supports OpenAPI 3.x, but it
requires   Java.   (There   is   an   NPM   package   wrapper   for   convenience,   e.g.   @openapitools/
openapi-generator-cli ).   If   you   use   this,   you’ll   choose   between   typescript-fetch   or
typescript-axios  generators. Axios may be preferred if you want automatic error handling

on non-2xx responses (fetch by default does not throw on HTTP errors)

6

. The generator will

output models (TS interfaces for request/response bodies) and API classes. You can configure

1

options like output folder structure, ES module vs CommonJS, etc., via command-line flags or

config files

7

.

•

swagger-typescript-api (NPM CLI): This is a popular Node.js tool to generate a TypeScript client
from an OpenAPI 3.0/2.0 spec. It runs via Node (no Java required). You can install it with  npm
install

run
npx   swagger-typescript-api   generate   --path   pangolin-api.json   --output   ./

swagger-typescript-api

--save-dev

and

src/pangolinClient/

8

. By default it produces a single TypeScript file ( Api.ts  by default)

containing an API class with methods for each endpoint. It supports both Fetch and Axios: use
the   --axios   flag   to   generate   an   Axios-based   client   instead   of   Fetch
.   For   example,
adding  --axios  will inject Axios  HttpClient  usage and require Axios as a dependency. This
tool also offers a   --modular  flag to split the output into multiple files (e.g. separate files for

10

9

models,   HTTP   client,   and   routes)

11

,   which   can   make   integration   easier   in   a   large   project.

swagger-typescript-api  is well-suited for Node/Bun – if you target modern Node (v18+ or v20+),

the Fetch API is available, so the default fetch client will work out-of-the-box. In Bun, Fetch is also

supported   natively,   so   a   fetch-based   client   works   without   extra   libraries.   If   supporting   older

Node versions, the Axios option or a Node-fetch polyfill is recommended.

•

openapi-typescript  (NPM  library   by   drwpow):  This   tool   focuses   on  type   definitions  rather

than a full request client. It generates TypeScript types for your OpenAPI spec – e.g. request/

response schemas and even path/method names – but does not generate functions to call the
API.   Install   with   npm   i   -D   openapi-typescript   and   run   npx   openapi-typescript
pangolin-api.json   -o   pangolin-api.d.ts .   You’ll   get   a   .d.ts   (or   .ts )   file   with

definitions for all endpoints. This is great for ensuring compile-time type safety if you plan to

write your own calls. For example, you can combine it with a minimal fetch wrapper: libraries like
openapi-fetch   or   zodios   can use these types to perform calls. However, using openapi-
typescript means you must manually handle HTTP requests (via  fetch ,  axios , etc.) in your

code. This approach gives flexibility and zero runtime bloat (types only), but requires more work

to integrate calls.

•

openapi-typescript-codegen   (ferdikoomen):  A   Node.js   codegen   library   (now   maintained   as
@hey-api/openapi-ts )   that,   similar   to   swagger-typescript-api,   generates   a   complete
TypeScript   client.   You   can   use   it   via   CLI   or   as   an   import.   For   example:   npx   openapi-
typescript-codegen --input pangolin-api.json --output src/pangolinClient --

client fetch . It supports multiple HTTP client options: fetch, node-fetch, Axios, XHR, etc.
.
Setting  --client fetch  gives a fetch-based client (good for browser or Node 18+/Bun), while
--client node   produces a Node-compatible client that explicitly uses   node-fetch   under
. (The default  fetch  client is meant for browser and will not automatically include
the hood
Node-specific polyfills, so use  --client node  if you need compatibility with Node <18 or want

12

13

to   ensure   use   of   the   node-fetch   library

13

.)   This   tool   generates   separate   files   for   models,

services (API classes), and core utilities by default (configurable via flags) – for instance, each
group   of   endpoints   (tagged   in   the   spec)   becomes   a   <Tag>Service   class,   and   you   get   an
ApiError   class,   etc.   The   project   has   been   forked   into   @hey-api/openapi-ts   for   active

maintenance,   but   usage   is   nearly   the   same.   The   advantage   here   is   you   get   a   lightweight,

framework-agnostic TS client without needing Java, and it’s easy to script as part of your build.

All of the above tools will produce TypeScript typings for Pangolin API paths and models, so you get

compile-time checking. In a Node.js or Bun environment, you’ll likely prefer a  fetch-based  or  Axios-

based  client. Axios might be familiar and has built-in features (interceptors, automatic JSON parsing

and   error   throwing   on   non-2xx),   whereas   a   fetch   client   has   no   external   deps   and   aligns   with   web

2

standards. Both swagger-typescript-api and openapi-typescript-codegen let you choose Axios vs fetch.

OpenAPI   Generator   offers   separate   generators   for   each.   In   summary,  if   you   want   a   quick,   Node-

friendly   solution,   a   good   choice   is   to   use   a   NodeJS   tool   like  swagger-typescript-api  or  openapi-

typescript-codegen  (so   you   don’t   need   Java).   These   will   generate   a   ready-to-use   TS   client   class   for

Pangolin.

Configuration and Integration in a Node/Bun Project

Once you have chosen a tool, you should set up your project to generate and use the client code. Key

considerations:

•

Automate the code generation: Treat the generated client as a build artifact. You can add an
NPM script, e.g.  "generate:pangolin-api": "swagger-typescript-api -p ./openapi/
pangolin-api.json -o src/pangolinClient -n PangolinApi.ts --api-class-name

PangolinApi" . This assumes you saved the spec to  openapi/pangolin-api.json . Running
npm run generate:pangolin-api   will then regenerate the   src/pangolinClient   code.

It’s a good idea to commit the generated code to your repo (especially if you want to avoid

requiring the generator in production), but you should  not manually edit  the generated files.

Instead, configure the generator to output code in your preferred structure and simply re-run it

when the API spec updates.

•

Output structure: Different tools have different defaults. For instance, OpenAPI Generator with
typescript-axios   will create multiple files (one per API class and model definitions) under
the   output   folder.  swagger-typescript-api  by   default   outputs   a   single   Api.ts   (which   can   be
  src/pangolinClient/
renamed)   –   you   might   keep   that   in   a   dedicated   folder   (e.g.
PangolinApi.ts ).   You   can   also   use   --modular   with   swagger-typescript-api   to   split   into
PangolinApi.ts ,   http-client.ts ,   data-contracts.ts   etc.,   which   can   be   easier   to
 openapi-typescript-codegen  will   by   default   produce   subfolders   like   models ,
navigate.
services , and a core  ApiClient  or  request.ts . You can customize or trim these outputs

via flags (e.g., skip schemas if not needed). Organize the output in your project (and consider
adding those files or folder to  .eslintignore  or similar, so your linter doesn’t complain about

generated code). After generation, you’ll typically have TypeScript definitions for all of Pangolin’s

API endpoints and some class or functions to call them.

•

HTTP client choice (Fetch vs Axios vs Node HTTP): In a Node 18+/Bun environment, the Fetch

API is available globally. A fetch-based generated client will work in Node without additional
libraries (for Node 16 or earlier, you’d need to polyfill  fetch  or use the generator’s Node mode/
axios mode). If you prefer Axios, ensure you install Axios ( npm install axios ) and generate

using   the   Axios   option;   the   generated   code   will   import   Axios   and   use   it   internally.   Both

approaches are fine – using fetch means one less dependency, using Axios might give more

familiar  promise  behavior  (e.g.  throwing   on  HTTP   errors).   There   is   generally   no   need   to   use
Node’s built-in   http / https   module directly – none of the high-level generators target that

low-level API, and using fetch or Axios is more convenient.

•

Base URL configuration: The Pangolin OpenAPI spec defines the base path (likely as  /v1  on

the current host)
. In the generated code, you’ll need to specify the actual server URL (i.e.
https://api.pangolin.net/v1   for   the   cloud   service,   or   your   own   host   if   self-hosted).

14

Typically:

3

•

With OpenAPI Generator (typescript-axios or fetch), a  Configuration  object is generated
where you can set the  basePath . For example:  const config = new
Configuration({ basePath: "https://api.pangolin.net/v1", accessToken:

•

PANGOLIN_API_TOKEN });  then pass that to the API constructors.
With swagger-typescript-api, the generated  PangolinApi  class constructor accepts an object
where you can override the  baseURL . We can pass  new PangolinApi({ baseURL:
"https://api.pangolin.net/v1", ... }) . By default, if you don’t set it, it may default to
the server info in the spec (which could be just the relative  /v1 ). It’s safest to explicitly set the

full base URL.

•

With openapi-typescript-codegen, the service classes might have a default baseUrl constant you
can modify, or you instantiate the  ApiClient  with a base url. Check the generated README or

docs in the output for how to configure base paths.

•

Authentication handling: Pangolin’s API uses Bearer token auth (API keys) for all requests

15

16

. The OpenAPI spec likely includes a security scheme for this. Most generators will provide a

way to include the bearer token:

•

In OpenAPI Generator (axios), each API call method may accept an  options  parameter where
you can set headers, or you set an  accessToken / apiKey  in the configuration (the exact

mechanism depends on how the spec is defined; often, you can set
config.accessToken = () => "Bearer <token>"  in the generated config).

•

In swagger-typescript-api, the generated class will allow setting default headers. For instance,
when constructing  PangolinApi , you can pass  headers: { Authorization: "Bearer
<YOUR_API_KEY>" }  in the config object, which the client will include on each request.
(Alternatively, it might generate a  securityWorker  function or a parameter to supply auth –

but simplest is to give a default Authorization header in the client config.)

•

With openapi-typescript-codegen, if using the fetch or axios templates, check for a
ApiRequestOptions  or similar. Often you might find in  request.ts  a place to inject
headers globally. For node-fetch client, you might set  process.env["TOKEN"]  or directly edit

a base configuration object. Refer to their docs for the exact method.

Important:  Never   hard-code   API   keys   in   the   generated   code   –   use   environment   variables   or

configuration at runtime to pass the bearer token. All these tools let you configure headers at runtime.

Automating Regeneration on API Updates

One challenge is keeping the client in sync with Pangolin’s API as it evolves. Here are some strategies to

automate or manage updates:

•

Watch for Pangolin releases: The Pangolin project is open source and actively updated. When a

new version is released (check the GitHub releases or change log), see if the Integration API

changed. The Pangolin docs or release notes should mention new API endpoints or changes. You

can then re-fetch the OpenAPI spec from the updated Swagger UI and re-run your generator. It’s

a   good   practice   to   version-control   the   spec   (or   at   least   record   the   Pangolin   version   you

generated against).

•

Periodic  spec  polling:  Since  the  public  Swagger  UI  is  accessible,  you  could  script  a  periodic
check. For example, a CI job or cron script could GET  https://api.pangolin.net/v1/docs

(or the raw JSON endpoint if identified) and diff it against the previous spec. If differences are

4

found,   alert   the   team   or   auto-regenerate   the   client.   This   ensures   you   catch   updates   even   if

they’re not loudly announced. Be mindful of rate limiting; polling once a day or week should be

plenty.

•

GitHub Action for codegen: You could integrate code generation into your build or CI pipeline.

For instance, have a GitHub Action that runs on a schedule or on dependency updates – it could

fetch the latest spec and run the generation. This can open a pull request with the changes.

Tools   like   Renovate   (which   usually   track   dependencies)   might   not   directly   track   OpenAPI

changes, but a custom action can accomplish this. If Pangolin’s spec is accessible via a URL, you

can even have the generator pull directly from the URL (e.g.,  swagger-typescript-api  supports a
URL input for  --path ). In practice, you might do:  swagger-typescript-api -p https://
api.pangolin.net/v1/docs -o ...   (if that URL returns JSON – if not, download the JSON

first).

•

Manual refresh with oversight:  In some cases, you might prefer to manually update to have

control. For example, run the generator locally whenever you bump the Pangolin server version

in your project. This allows you to review the diff of the generated code (to see what changed in

the API). If there are breaking changes, you can address them in your app code. Keeping the

generated client code in your repo helps with diffing changes over time (you can see exactly
what endpoints or models changed by comparing the before/after code).

In all cases, try to minimize manual edits to the generated code – regenerate from source whenever

possible. If you need custom tweaks (e.g., custom helper functions), it’s better to wrap the generated

client   in   your   own   abstraction,   or   use   templates/custom   generator   hooks,   rather   than   editing   the

generated files (which would get overwritten next time).

Step-by-Step: Generating and Using a Pangolin API Client

Let’s   walk   through   an   example   using  swagger-typescript-api  (one   of   the   recommended   tools)   to

generate a Pangolin API client and call an endpoint:

1.

Download   the   OpenAPI   spec:  Using   the   Swagger   UI   at   https://api.pangolin.net/v1/
docs , download or copy the OpenAPI JSON. (In Swagger UI, click “Raw” or use dev tools to get
the   URL   for   the   JSON   spec).   Save   it   as   pangolin-api.json   in   your   project   (e.g.,   in   an
openapi/  folder).

2.

Install   the   generator   tool:  In   your   Node/Bun   project,   add   the   codegen   tool   as   a   dev

dependency. For our example:

npm install --save-dev swagger-typescript-api

(If you prefer another tool like OpenAPI Generator, install its CLI instead.)

3.

Generate the TypeScript client code: Run the generator with appropriate options. For example:

npx swagger-typescript-api generate \

--path openapi/pangolin-api.json \

--output src/pangolinClient/ \

5

--name PangolinApi.ts \

--api-class-name PangolinApi \

--axios

This command reads the spec from our saved JSON, and outputs a TypeScript file named
PangolinApi.ts  in  src/pangolinClient . We used  --axios  to use Axios for HTTP
requests (ensure you have Axios installed). If you prefer to use Fetch, omit the  --axios  flag –

the generated code will then use the Fetch API (which works in Node 18+, Bun, or browsers). The
--api-class-name PangolinApi  ensures the generated class has a readable name. After
running this, check the  src/pangolinClient  folder – you should see  PangolinApi.ts

(and possibly some additional support files or d.ts files). All Pangolin API endpoints should now
be represented as methods on the  PangolinApi  class, and interfaces for the request/

response models will be defined.

4.

Initialize the API client in your code:  Now you can use the generated client in your Node or

Bun   project.   Import   the   class   and   create   an   instance,   configuring   the   base   URL   and

authentication. For example:

import { PangolinApi } from './pangolinClient/PangolinApi';

// Initialize the client with base URL and default headers (e.g. auth)

const pangolinApi = new PangolinApi({

baseURL: "https://api.pangolin.net/v1",

// Pangolin API endpoint

headers: {

Authorization: `Bearer ${process.env.PANGOLIN_API_KEY}`

// Your API key

for Pangolin

}

// ... (other Axios config like timeout can be set here if needed)

});

In this example, we pass an Axios config object with a baseURL and Authorization header. This will apply
to all requests made by  pangolinApi . (If you generated a fetch-based client, the initialization might

accept a similar config object or you might set a global base URL in the generated code; consult the

generated docs. Often, fetch clients have a default base URL baked in from the spec’s servers, so you

might only need to set the auth header.)

1.

Call Pangolin API endpoints with full TypeScript support: You can now invoke methods on
pangolinApi  to interact with Pangolin. For instance, if you want to list roles for an

organization (assuming the Pangolin API has such an endpoint), there may be a generated

method for it. Using the info from Pangolin’s docs, an example endpoint is
GET /org/{orgId}/roles

. The generated method might be named based on the path

17

(e.g.,  getOrgRoles  or similar under a  roles  tag). You would call it like:

const orgId = "12345";

// your Pangolin Organization ID

const response = await pangolinApi.getOrgRoles({ orgId });

console.log("Roles in org:", response.data);

6

Here,  getOrgRoles  is a method provided by the generated  PangolinApi  class. We pass the
orgId  as a parameter (the generator will have created a parameter interface for it). The result
response  (or it might be returned directly depending on template) is fully typed – for example,
.data  might be an array of Role objects with specific fields. Your IDE will autocomplete property

names and types for these objects. This strong typing is the big benefit of generating the client.

Usage Note: Different generators have slight variations in function signatures: - OpenAPI Generator’s
axios client often returns AxiosResponse<T> objects. You might need to access   response.data . -

swagger-typescript-api   by   default   can   be   configured   to   unwrap   response   data   (--unwrap-response-

data). If used, the methods might return the raw data directly. - Adjust the usage according to how your

generated code works (check the documentation comments in generated code).

1.

Handle errors and edge cases: The generated client will throw or return errors if the HTTP call

fails. For Axios-based clients, a non-2xx status results in a thrown error (you can catch it with try/

catch). For fetch-based clients, you might need to check a response status property unless the

generator built that in. Also, handle network errors (the client will propagate exceptions if the
server is unreachable, etc.). The types for errors may be available (e.g., a custom   ApiError

class or AxiosError). Refer to the generator docs for best practices on error handling.

2.

Re-generate as needed:  Whenever Pangolin releases updates to the API, update   pangolin-
api.json  (download the new spec) and rerun the generation (step 3). This will update the client

library. You can then review compile-time errors to see where your usage might need to change

(for   example,   if   an   endpoint   signature   changed).   Because   the   client   is   strongly   typed,   any

breaking   changes   in   the   API   are   likely   to   surface   as   TypeScript   errors   or   differences   in   the

generated code that you can address proactively.

Pangolin’s Swagger UI provides interactive API documentation. You can explore endpoints and try requests in

the browser. The same OpenAPI spec that drives this UI can be used to generate a TypeScript client.

Limitations and Gotchas

•

Spec  accuracy:  Ensure  the  OpenAPI   spec  is   up-to-date.  The   Pangolin   team  maintains   it,   but

occasionally there could be minor mismatches (like a documentation typo that was noted in an

issue)

17

.   If   you   notice   an   endpoint   not   working   as   generated,   double-check   the   spec   (and

Pangolin’s issues) – you might have to adjust or wait for a fix if the spec was wrong. You can

always override the request path manually as a workaround.

•

Java requirement (OpenAPI Generator): If using the official OpenAPI Generator CLI, remember

it   needs   a   Java   runtime.   In   Node   projects,   many   prefer   to   avoid   this.   The   NPM   package
openapi-generator-cli   can download the needed JAR automatically, but it increases your

dev   environment   setup.   Tools   like   swagger-typescript-api   or   openapi-typescript-codegen   are

pure JavaScript/TypeScript and easier to include in a Node toolchain.

•

Generated code size:  A full client for a large API may be somewhat heavy. Pangolin’s API isn’t

enormous, but expect dozens of methods and interfaces. If bundle size is a concern (e.g., if using

this in a front-end), you might tree-shake or only import what you need. In Node/Bun, this is
usually not an issue. If you only need type info, using  openapi-typescript  might be leaner,

at the cost of writing your own calls.

7

•

Axios vs Fetch nuance:  Axios will automatically throw for HTTP errors and parses JSON, while
fetch requires checking   response.ok   and manually calling   response.json() . Generated

fetch   clients   often   handle   this   internally.   For   example,   OpenAPI   Generator’s   fetch   template

typically includes code to parse JSON and throw on bad status. If you use swagger-typescript-api

with
consider
--disable-throw-on-error  flags as needed

fetch,

using

the

  --unwrap-response-data

and

9

18

. With Axios, you might not need those

adjustments. Test a couple calls to ensure the behavior (e.g., what happens on a 404).

•

Node-specific adjustments: If you generate a fetch-based client and run in Node, on Node 16

or earlier you’ll get “fetch is not defined” at runtime. The simplest fix is to upgrade Node or
include a polyfill ( node-fetch ). Alternatively, generate specifically for Node (for instance, using
openapi-typescript-codegen’s   --client node   which uses node-fetch)

. In Bun and Node

13

18+, fetch is built-in and works seamlessly.

•

Customizing   generation:  These   tools   often   allow   templating   or   customizing   the   output   (for

instance, swagger-typescript-api lets you supply your own EJS templates for how the code is

generated). This is powerful if you need to enforce a certain coding style or integrate with a

particular   framework.   For   most   cases,   the   default   templates   are   fine.   But   be   aware   of   this

capability if you have special requirements (e.g., you want the client to use a specific logger or

error format – you could tweak the templates).

•

Using the Pangolin Node library:  (As an aside, Pangolin’s GitHub shows a   pangolin-node

repository which might be an official Node SDK. If it’s mature, that’s another route instead of

generating   your   own.   However,   generating   from   OpenAPI   ensures   you   have   the   latest   API

surface and full control. Check Pangolin’s documentation to see if an official SDK exists and how

up-to-date it is.)

By following the above steps, you’ll have a TypeScript SDK for the Pangolin API that you can use in a

Node.js   or   Bun   environment.   It   will   streamline   calling   Pangolin   (for   example,   creating   tunnels,

managing resources, etc.) with full type safety. Just remember to keep the spec and generated code in

sync with Pangolin’s updates. Happy coding!

Sources:  The Pangolin documentation confirms the availability of an OpenAPI (Swagger) spec and UI

1

2

. We referenced the swagger-typescript-api tool’s documentation for generation options

10

 and

openapi-typescript-codegen   for   client   options   like   fetch   vs   node-fetch

12

13

.   The   Pangolin   GitHub

issue   tracker   provides   insight   into   API   endpoints   and   potential   documentation   quirks

17

.   All   these

informed the recommended setup and usage.

1

3

Enable Integration API - Pangolin Docs

https://docs.pangolin.net/self-host/advanced/integration-api

2

15

16

Integration API - Pangolin Docs

https://docs.pangolin.net/manage/integration-api

4

pangolin/.gitignore at 632333c49f11eb2bee82142b5d83cedd536bbef1 - vrr/pangolin - VRR Forge

https://git.viorsan.com/vrr/pangolin/src/commit/632333c49f11eb2bee82142b5d83cedd536bbef1/.gitignore

5

Openapi generator, Svelte vs Axios. : r/sveltejs - Reddit

https://www.reddit.com/r/sveltejs/comments/1mk3lad/openapi_generator_svelte_vs_axios/

8

6

node.js - OpenAPI Generator `typescript-fetch` vs ... - Stack Overflow

https://stackoverflow.com/questions/71213188/openapi-generator-typescript-fetch-vs-typescript-node-vs-typescript-axios

7

Generating TypeScript Types with OpenAPI for REST API ...

https://www.hackerone.com/blog/generating-typescript-types-openapi-rest-api-consumption

8

GitHub - acacode/swagger-typescript-api: Generate the API Client for Fetch or Axios from an

OpenAPI Specification

https://github.com/acacode/swagger-typescript-api

9

10

11

18

swagger-typescript-api - Fig.io

https://fig.io/manual/swagger-typescript-api

12

GitHub - ferdikoomen/openapi-typescript-codegen: NodeJS library that generates Typescript or

Javascript clients based on the OpenAPI specification

https://github.com/ferdikoomen/openapi-typescript-codegen

13

Node‐Fetch Support · ferdikoomen/openapi-typescript-codegen Wiki · GitHub

https://github.com/ferdikoomen/openapi-typescript-codegen/wiki/Node%E2%80%90Fetch-Support

14

Swagger UI - Pangolin

https://api.pangolin.fossorial.io/v1/docs/

17

small error on "Docs" (swagger) · Issue #1339 · fosrl/pangolin · GitHub

https://github.com/fosrl/pangolin/issues/1339

9

