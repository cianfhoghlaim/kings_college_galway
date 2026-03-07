Pulumi TypeScript Guide: Provisioning Cloudflare

D1 & R2 with 1Password Integration

Overview and Goal

This guide demonstrates how to use Pulumi (TypeScript) to provision Cloudflare D1 (a serverless SQL

database) and Cloudflare R2 (S3-compatible object storage) in a single stack. We will authenticate to an

existing   Cloudflare   account,   create   both   services,   and   then   extract   important   outputs   (like   the   D1

database UUID and an API token for access). Finally, you'll see how to securely store these outputs in

1Password  using the Node.js SDK. The guide also covers project structure for multiple environments

(e.g. development and production) using Pulumi stacks.

Key steps:

•

Set up authentication for Cloudflare and 1Password in Pulumi.

•

Write a Pulumi program (in TypeScript) that creates a D1 database and an R2 bucket.

•

Output the D1 database ID (UUID) and an API access token from the stack.

•

Use the 1Password Node.js SDK to store these credentials in a 1Password vault.

•

Structure the Pulumi project to handle separate dev/prod configurations.

Prerequisites and Setup

Before writing any code, ensure you have the following:

•

Cloudflare Account: with an  Account ID and an  API Token that has permissions to create D1

databases and R2 buckets. (The API token should include D1 Edit and R2 Storage Edit permissions

1

2

). You can create a scoped API token in the Cloudflare dashboard (under My Profile > API

Tokens). Save your Account ID (a UUID from the Cloudflare dashboard) and API token for use in

Pulumi.

•

Pulumi CLI installed, and a new Pulumi TypeScript project ( pulumi new typescript ).

•

Pulumi Cloudflare Provider installed in your project. You can add it via NPM:

npm install @pulumi/cloudflare

•

1Password   Service   Account:  Create   a   service   account   in   your   1Password   Teams/Business

account   and   get   the  service   account   token  (this   will   be   used   by   the   1Password   SDK   for

authentication)

3

.   Ensure   the   service   account   has   access   to   the   vault   where   you   will   store

secrets.

•

1Password Node.js SDK: Install the official 1Password SDK for Node:

1

npm install @1password/sdk

(This SDK allows programmatic access to 1Password. We use it to create an item in a vault.)

•

Node.js   environment   variables:  For   1Password   authentication,   set   an   environment   variable
OP_SERVICE_ACCOUNT_TOKEN   with your 1Password service account token (the SDK will pick
. For Cloudflare, you can set  CLOUDFLARE_API_TOKEN  to your API token

this up by default)

4

(or configure it in Pulumi config).

Pulumi Configuration for Cloudflare

Pulumi  needs  to  know  how  to  authenticate  with  Cloudflare.  You  can  provide  credentials  via  Pulumi

config or environment variables:

•

Using Pulumi Config: Run  pulumi config set cloudflare:apiToken <your-token>  to

set the Cloudflare API token in the stack config (mark it as secret). Pulumi will use this for the

Cloudflare   provider
pulumi config set cloudflareAccountId <your-account-id> ).

  Also   set   your   Cloudflare   Account   ID   in   config   (e.g.,

.

5

•

Using   Environment   Variables:  Alternatively,   export   CLOUDFLARE_API_TOKEN   in   your   shell.

. You will
The Cloudflare Pulumi provider will automatically use it if no explicit config is set
also need your   Account ID   available (e.g., as an env var or Pulumi config) since resource

6

definitions require it.

In the Pulumi program, you can retrieve config values as follows:

import * as pulumi from "@pulumi/pulumi";

const config = new pulumi.Config();

const cfAccountId = config.require("cloudflareAccountId");

// Cloudflare

Account ID

// (Cloudflare API token is picked up from config or env automatically)

Alternatively, you can explicitly instantiate a Cloudflare provider in code with the token, for example:

import * as cloudflare from "@pulumi/cloudflare";

const cloudflareProvider = new cloudflare.Provider("cf", {

apiToken: pulumi.secret("<YOUR_API_TOKEN>"),

});

This provider can be passed to resources via the  provider  option

7

, though for most cases using

config/env is simpler.

2

Provisioning Cloudflare D1 and R2 in Pulumi

With   configuration   in   place,   define   resources   for   the   D1   database   and   R2   bucket   in   your   Pulumi

TypeScript program. Below is a snippet showing how to create a D1 database and an R2 bucket, and
then output their important properties:

import * as cloudflare from "@pulumi/cloudflare";

// Cloudflare account context (from config as shown above)

const accountId = cfAccountId;

// Provision a new D1 database

const myDatabase = new cloudflare.D1Database("myDatabase", {

accountId: accountId,

name: "my-database",

primaryLocationHint:

// Database name

"wnam",

// Primary location (e.g., "wnam" for US West)

8

// (Optional: readReplication can be configured if needed)

});

// Provision a new R2 bucket

const myBucket = new cloudflare.R2Bucket("myBucket", {

accountId: accountId,

name: "my-bucket",

location:

"enam",

// Bucket name

// Bucket region (e.g., US East – "enam")

storageClass: "Standard",

// Storage class ("Standard" or

"InfrequentAccess")

jurisdiction: "default",

// Data jurisdiction ("default"

for global)

9

});

// Export relevant outputs from the stack

export const d1DatabaseId = myDatabase.uuid;

// D1 database UUID

export const r2BucketName = myBucket.name;

// R2 bucket name (for

reference)

In the above, the D1 database resource is created with a name and region. The Cloudflare provider

returns a UUID for the new database which we export (this is the identifier you'll use to connect to D1)

10

. The R2 bucket resource is similarly created with a name, region, etc. We export the bucket name

(you could also export  myBucket.id , but for R2 the  id  is often the same as the name or ARN)

11

.

Note:  The Cloudflare provider will use the API token you configured to authorize these operations.

Make sure the token has “D1 Edit” and “R2 Storage Edit” permissions (to allow creating and managing

D1 and R2)

1

2

.

3

Creating an API Token for D1/R2 Access (Optional)

Often you'll want a separate API token specifically for your application to access the new D1 database

(via   Cloudflare's   D1   HTTP   API   or   Workers)   and   R2   bucket.   Instead   of   manually   creating   this   in   the
Cloudflare UI, we can automate it with Pulumi using the  cloudflare.ApiToken  resource.

For example, to create a token with permissions to read/write D1 and read/write R2 storage on your

account:

// (Ensure the permission group IDs below are correct for D1 Edit and R2

Edit)

const D1_EDIT_PERMISSION_ID = "<UUID-for-D1-

Edit>";

// e.g., permission group ID for "D1 Edit"

const R2_EDIT_PERMISSION_ID = "<UUID-for-R2-

Edit>";

// e.g., permission group ID for "Workers R2 Storage Edit"

const myApiToken = new cloudflare.ApiToken("myAppToken", {

name: `myApp-${pulumi.getStack()}-

token`,

// token name (include stack/env in name)

policies: [{

effect: "allow",

resources: {

// Scope the token to your Cloudflare Account ID:

[`com.cloudflare.api.account.${accountId}`]: "*",

},

permissionGroups: [

{ id: D1_EDIT_PERMISSION_ID },

{ id: R2_EDIT_PERMISSION_ID }

]

}],

// (Optionally, you can set token expiration or IP restrictions

via .condition)

});

In   this   configuration,   we   define   an   API   token   policy   that   allows   access   to   all   resources   under   our

account and includes the  D1 Edit  and  Workers R2 Storage Edit  permission groups. (You will need to

supply  the  actual  Cloudflare  permission  group  IDs  for  these;  you  can  list  permission  group  IDs  via

Cloudflare's API or documentation. Cloudflare's docs confirm the existence of "D1 Edit" and "Workers R2

Storage Edit" permission scopes

1

2

.)

Pulumi will create this token and return its values. We can export the token's secret and ID:

export const apiTokenSecret = myApiToken.value;

// The token secret string

12

export const apiTokenId = myApiToken.id;

// The token ID (often used

as Access Key ID for R2)

4

Pulumi marks  myApiToken.value  as a secret output (since it’s sensitive) – it represents the actual API
.   The   id   is   the   token’s   identifier.   For   Cloudflare   R2   usage,   the  Access   Key   ID

token   string

12

corresponds to the token ID and the Secret Access Key is the token value

13

14

. By capturing both,

we have what’s needed to authenticate to R2’s S3 API and to call D1’s HTTP API.

Storing Cloudflare Credentials in 1Password

After provisioning, we have the following values to store securely for our app or team:

•

D1 Database ID (UUID)

•

Cloudflare API Token (and possibly its Access Key ID, if needed for R2)

Using the 1Password Node.js SDK, we can programmatically save these secrets into a vault. Below is an

example Node.js script that would run after Pulumi deploys the resources (you could integrate this into

your deployment pipeline). This script assumes the Pulumi outputs (from above) are available – for
example, by reading the stack outputs via  pulumi stack output  or using Pulumi’s Automation API.

Here, we’ll assume you have them as environment variables or passed into the script for simplicity.

import * as sdk from "@1password/sdk";

// 1Password client setup – uses service account token from env

const client = sdk.OnePasswordConnect.createClient({

token: process.env.OP_SERVICE_ACCOUNT_TOKEN!,

// If using a self-hosted Connect server, include url: "https://

<CONNECT_URL>"

// If using 1Password cloud with service account, no URL is needed (it

defaults to 1Password API).

userAgent: "PulumiCloudflareIntegration/1.0"

});

// Values to store (replace these with actual outputs or pass them in

securely)

const d1Id = process.env.CLOUDFLARE_D1_ID!;

const apiTokenSecret = process.env.CLOUDFLARE_API_TOKEN!;

const apiTokenId = process.env.CLOUDFLARE_TOKEN_ID!;

// access key ID for R2

// Create a new item in 1Password (in a specified vault) to hold these
secrets

await client.items.create({

title: `Cloudflare D1/R2 Credentials (${process.env.NODE_ENV || "dev"})`,

vaultId: "<YOUR_VAULT_ID>",

// The 1Password vault where

to store

category: sdk.ItemCategory.ApiCredentials,

// Storing as an "API

Credentials" item

sections: [ { id: "cloudflare", title: "Cloudflare Info" } ],

fields: [

{ sectionId: "cloudflare", id: "D1 Database ID", title: "D1 Database

ID", fieldType: sdk.ItemFieldType.Text, value: d1Id },

{ sectionId: "cloudflare", id: "API Token ID", title: "API Token ID",

fieldType: sdk.ItemFieldType.Text, value: apiTokenId },

5

{ sectionId: "cloudflare", id: "API Token Secret", title: "API Token

Secret", fieldType: sdk.ItemFieldType.Concealed, value: apiTokenSecret }

]

});

console.log("  Cloudflare credentials stored in 1Password.");

In the code above, we initialize the 1Password SDK client using our service account token (by default,
. We then call   client.items.create(...)   to
OP_SERVICE_ACCOUNT_TOKEN   env var is used)

4

create   a   new   item   in   the   specified   vault.   We   choose   the   item   category   as  “API   Credentials”  and

organize our fields in a section called "Cloudflare Info". We add three fields: the D1 database ID, the API

token   ID,   and   the   API   token   secret.   We   mark   the   secret   as   a   concealed   field   (so   it’s   treated   like   a

password)   and   leave   the   others   as   plain   text   for   reference.   This   follows   the   pattern   shown   in
1Password’s documentation, where each field has an  id ,  title ,  fieldType , and  value
. The
sdk.ItemFieldType.Concealed  field type ensures the secret is stored encrypted and hidden (just

15

like a password)

16

.

After running this script, 1Password will contain a new item (titled “Cloudflare D1/R2 Credentials (dev)”, for

example) with the database UUID and token securely stored. Team members or applications can then

retrieve these secrets from 1Password instead of from plain config.

Security Tip: Avoid printing the secret values in logs. When using   pulumi stack output , you can
mark   outputs   as   secret   so   Pulumi   conceals   them.   In   our   Pulumi   code,   apiToken.value   is

automatically treated as a secret output (it won’t be shown in cleartext in the Pulumi UI or CLI). When

passing values to the 1Password script, ensure you do so securely (e.g., via environment variables or

Pulumi Automation API, not via command-line arguments).

Structuring the Project for Dev and Prod

Pulumi makes it easy to manage multiple environments by using stacks (e.g., a "dev" stack and a "prod"

stack). Here are some best practices to structure the project for different environments:

•

Pulumi Stacks: Create separate stacks for each environment: for example, run  pulumi stack
init dev   and   pulumi stack init prod . Each stack will have its own config values (like

Cloudflare account details, names, etc.) and maintain separate state.

•

Config   per   Environment:  Use   Pulumi   config   to   define   environment-specific   settings.   For

instance,   you   might   use   different   database   names   or   Cloudflare   accounts.   In
Pulumi.dev.yaml
  Example:
cloudflareAccountId   might   be   the   same   if   using   one   Cloudflare   account   for   all,   but
myDatabaseName  could be different.

and   Pulumi.prod.yaml ,

  set   appropriate   values.

•

Parameterize Resource Names: Include the stack name in resource names to avoid collisions.

For example, when defining the D1 database and R2 bucket, you could do:

const stack = pulumi.getStack();

const dbName = `my-database-${stack}`;

const bucketName = `my-bucket-${stack}`;

const myDatabase = new cloudflare.D1Database("myDatabase", {

6

accountId: accountId,

name: dbName,

primaryLocationHint: "wnam",

});

const myBucket = new cloudflare.R2Bucket("myBucket", {

accountId: accountId,

name: bucketName,

location: "enam",

storageClass: "Standard",

jurisdiction: "default",

});

In this way, the dev stack might create a database named  my-database-dev  and the prod stack  my-
database-prod , preventing naming conflicts.

•

1Password Vault Segregation: You may choose to use separate vaults or at least separate item

names for different environments. In our 1Password example, we appended the environment

name to the item title. You could also have separate vaults (e.g.,  App Dev Secrets  vs  App Prod
Secrets)   and   configure   the   vaultId   per   stack   (via   Pulumi   config).   This   ensures   that,   for

instance, production credentials are stored in a vault only accessible to production systems or

authorized persons.

•

Authenticating   with   Different   Credentials:  If   your   Cloudflare   account   or   API   token   differs

between environments (for example, a staging vs. production Cloudflare account), set those in
each stack’s config ( cloudflare:apiToken   etc.). The Pulumi program can remain the same,
and you just select the appropriate stack ( pulumi stack select prod ) before deployment.

Pulumi will use the config values for that stack when running the program.

By following these practices, you maintain one TypeScript codebase, and Pulumi will manage separate

state and config for each environment. This makes it easy to deploy changes to dev without affecting

prod, and vice versa, while storing each environment’s secrets safely in 1Password.

Conclusion

In   this   guide,   we   walked   through   provisioning   Cloudflare   D1   and   R2   resources   with   Pulumi   and

TypeScript, extracting the essential connection details,  and storing them securely  in 1Password.  We

covered   setting   up   authentication   for   Cloudflare’s   provider   (using   an   API   token)   and   for   1Password

(using a service account token). The Pulumi code creates the infrastructure and produces outputs like

the D1 database UUID and an API token’s secret

12

, and we leveraged the 1Password Node.js SDK to

create an item containing these secrets for safe keeping

15

. By structuring the Pulumi project with

multiple stacks, you can deploy development and production environments with ease, using the same

code but different configurations.

With this setup, your Cloudflare credentials are never kept in plain text in code or config – they reside in

1Password, and your infrastructure code pulls from secure sources. This approach improves security

and maintainability, especially as your team and number of environments grow. Happy automating!

Sources:

•

Pulumi Cloudflare Provider – Installation & Configuration

5

6

7

•

Pulumi Cloudflare Provider – D1Database and R2Bucket Resource Docs

8

9

•

Pulumi Cloudflare Provider – ApiToken Resource Outputs

12

•

Cloudflare API Token Permissions – D1 and R2 scopes

1

2

•

1Password Developer Docs – 1Password Node.js SDK usage

15

1

2

API token permissions · Cloudflare Fundamentals docs

https://developers.cloudflare.com/fundamentals/api/reference/permissions/

3

4

1Password SDKs | 1Password Developer

https://developer.1password.com/docs/sdks/

5

6

Cloudflare Provider | Pulumi Registry

https://www.pulumi.com/registry/packages/cloudflare/

7

Create different resources using Pulumi and Wrangler · Pulumi docs

https://developers.cloudflare.com/pulumi/tutorial/dynamic-provider-and-wrangler/

8

10

cloudflare.D1Database | Pulumi Registry

https://www.pulumi.com/registry/packages/cloudflare/api-docs/d1database/

9

11

cloudflare.R2Bucket | Pulumi Registry

https://www.pulumi.com/registry/packages/cloudflare/api-docs/r2bucket/

12

cloudflare.ApiToken | Pulumi Registry

https://www.pulumi.com/registry/packages/cloudflare/api-docs/apitoken/

13

14

cloudflare.R2BucketSippy | Pulumi Registry

https://www.pulumi.com/registry/packages/cloudflare/api-docs/r2bucketsippy/

15

16

Introducing Programmatic Item Management with 1Password SDKs | 1Password

https://blog.1password.com/1password-sdks-programmatic-item-management/

8

