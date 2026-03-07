Implementation Plan: Self‑Hosting BetterAuth,

Convex, Supabase PostgreSQL, Hono & TanStack

Start

Architecture Overview

In this unified authentication architecture, BetterAuth acts as the central Identity Provider (IdP) using

OpenID Connect. All components (frontend and backend) rely on BetterAuth for user authentication.

Supabase PostgreSQL (self-hosted) serves as the primary database backing BetterAuth – storing user

accounts, authentication sessions, and any OIDC data (clients, tokens, etc.)

1

. The  TanStack Start

frontend   (React/Solid   SSR   framework)   integrates   with   BetterAuth   for   user   login,   while   the   backend

services (Hono API and Convex) validate and trust BetterAuth-issued tokens for secure interactions.

Key components and their roles:

•

BetterAuth (OIDC Provider): Handles user authentication (email/password, social logins, etc.)

and issues OIDC tokens (ID/Access tokens) via standard flows (e.g. Authorization Code with

PKCE)

2

. It persists user profiles, credentials, sessions, and OIDC client data in Postgres for

reliability

1

.

•

Supabase PostgreSQL: The Postgres database instance that stores BetterAuth’s data (tables for

users, sessions, accounts, etc.) and can also store application data. BetterAuth is configured to

connect to this DB (via a connection URL and credentials)

3

4

. (In our Docker Compose setup,

this is a single Postgres container for simplicity).

•

Hono (Backend API): A lightweight TypeScript web server (running on Node) that implements

business logic. Hono integrates the BetterAuth library as middleware/route handlers, so it
effectively hosts the BetterAuth authentication endpoints (e.g.  /api/auth/*  routes) and also

provides protected application APIs. This means the Hono server will serve login, token, and

userinfo routes via BetterAuth’s handler, and also serve other API routes for the app

5

. All API

routes will require valid BetterAuth tokens.

•

Convex (Self-Hosted Backend): A real-time backend data platform (Convex’s open-source

reactive database) that our app uses for application data and server-side logic. We self-host

Convex as a separate service connected to its own underlying store (we will use the same

Postgres instance for Convex’s data for convenience). Convex is configured to trust BetterAuth

as an OIDC provider, so it will accept BetterAuth’s JWTs to authenticate incoming client requests

6

. This allows Convex functions to know the caller’s identity securely.

•

TanStack Start (Frontend): The front-end application (React/Solid) that interacts with the

backend. It uses BetterAuth for user authentication (either by redirecting to BetterAuth’s hosted

login page or by using BetterAuth’s client utilities) and obtains OIDC tokens upon login. It then

calls the Hono API and/or the Convex backend, presenting the BetterAuth token for

authorization.

Inter-component   trust:  Both   Hono   and   Convex   validate   JWTs   issued   by   BetterAuth   using   OIDC

standards. BetterAuth publishes a JWKS (JSON Web Key Set) endpoint for its signing keys

2

, which

Hono and Convex use to verify token signatures and claims. This means the backend services do  not

manage passwords or separate session stores – they defer to BetterAuth for auth. Convex’s integration

1

specifically   uses   BetterAuth’s  issuer   domain  and   the   client   ID   (audience)   to   validate   ID   tokens

7

,

ensuring tokens are genuine and meant for our application.

Below is a high-level flow of authentication and data calls in this architecture:

1.

User Login (Frontend → BetterAuth): A user on the TanStack Start app initiates login. The
frontend redirects the user to BetterAuth’s OIDC authorization endpoint (hosted by the

BetterAuth service) or invokes the BetterAuth client SDK, which in turn hits the BetterAuth auth

routes. The user provides credentials (or social login) on BetterAuth’s interface.

2.

Authentication & Token Issuance (BetterAuth → Postgres → Frontend): BetterAuth verifies
credentials (e.g. checks email/password) and creates a user session. All user and session data is

persisted in the Postgres DB (for example, new user records or session tokens are stored)

1

.

Upon success, BetterAuth issues an ID token (JWT) and an access token (and possibly a refresh

token) for the user

2

. These tokens are returned to the frontend (either via redirect callback

with an authorization code that the frontend exchanges for tokens, or directly if using an implicit

flow). The ID/Access tokens are signed by BetterAuth and include claims like the user’s ID, the
issuer ( iss  = BetterAuth URL), audience ( aud  = our app’s client ID), and expiration.

3.

Frontend Stores Token & Requests API: The TanStack frontend receives the tokens (or a

session cookie if using cookies) and stores them (commonly in memory or an HttpOnly cookie).

The user is now logged in on the frontend. For any protected data operations, the frontend
includes the access token (usually as an  Authorization: Bearer <token>  header) when

making requests. For example, to call our application’s backend API (Hono) to fetch user data,

the frontend sends the JWT in the header.

4.

Backend API Authentication (Frontend → Hono): The Hono API receives the request with the
user’s JWT. It verifies the token by checking its signature and claims against BetterAuth’s public

keys. Because we use BetterAuth’s OIDC provider, Hono can retrieve the JWKS from BetterAuth’s
.well-known/jwks.json  URL and cache it for token verification
iss  matches the BetterAuth issuer URL and that it’s not expired. (If we configure audience
checking, Hono can also ensure the token’s  aud  matches the expected client/application ID.) If

. Hono ensures the token

8

the token is valid, Hono knows the user’s identity (e.g. user ID from token claims) and authorizes

the request.

5.

Implementation: In our Hono server code, we will likely use a middleware or the BetterAuth
library to protect routes. For example, BetterAuth provides an  auth.handler()  that we
mount on  /api/auth/*  for authentication endpoints

, and we can use its utilities or a JWT

5

verification middleware for other routes. Once verified, Hono’s route handlers can access the

user info from the token (or BetterAuth session) for authorization logic.

6.

Convex Backend Authentication (Frontend → Convex): The TanStack app may also
communicate with Convex directly (using Convex’s client SDK for real-time updates and queries).

We configure the Convex client to supply the BetterAuth ID token on every request. Convex,

running self-hosted, is set up with a Custom OIDC provider config pointing to BetterAuth. This

means Convex knows the BetterAuth issuer domain and our application’s OIDC client ID
(audience), as configured in Convex’s  auth.config.ts

. When the frontend opens a

7

WebSocket or sends an HTTP request to Convex, Convex receives the JWT and validates it against

BetterAuth’s issuer and JWKS keys, just like Hono does

6

. A valid token lets Convex identify the

user (Convex will make the user’s identity available in its server functions securely). If the token is

missing or invalid, Convex denies access. (Note: Alternatively, Hono could act as a proxy to Convex,

but the typical integration is the client communicates directly with Convex using the token.)

7.

Secure Data Access and Interaction: Once the user is authenticated to backend services,

requests proceed to data interactions:

8.

Hono can now perform its API logic. It may read or write data in the Supabase Postgres (for

example, if Hono needs to query relational data or use Postgres for additional storage beyond

2

Convex). The Hono service would use a Postgres client or ORM to query the database. It can

safely use the user’s identity from the token to, say, query rows belonging to that user.

(BetterAuth’s user ID can serve as a foreign key in application tables if needed).

9.

Hono can also call Convex if needed. In cases where our API layer needs to trigger Convex

actions (server-to-server), Hono could call Convex’s HTTP endpoints or use Convex client libraries

on the server. For instance, Hono could forward a request to a Convex function. In doing so,

Hono would include the user’s JWT (or use an admin key if performing privileged actions). This

ensures Convex receives a valid token even for calls initiated server-side.

10.

Convex, upon validating the user via JWT, executes its functions and database operations.

Convex’s state is stored in the underlying Postgres (in a separate schema or database). In our
setup, we’ll create a dedicated database (e.g.  convex_self_hosted ) within the Postgres

instance for Convex to use

9

. Convex will persist and query app data there, and can also

enforce access control within its functions using the user identity from the token.

11.

Response to Frontend: Both backend services then return responses to the frontend. Hono

sends back JSON responses over HTTP (e.g. user profile data, etc.), while Convex’s responses

(queries, reactive updates) flow back over its real-time connection. The frontend now has the

requested data and can render the UI. All interactions were gated by BetterAuth’s authentication,

ensuring the user was authorized at each step.

Throughout   this   flow,  BetterAuth   is   the   single   source   of   truth   for   identity.   If   a   user   logs   out,

BetterAuth can revoke the session or tokens (e.g. a refresh token can be invalidated in the DB). Backend

services could consult BetterAuth’s UserInfo or introspection endpoint (if available) to get additional

user   details   if   needed,   but   in   most   cases   the   JWTs   themselves   contain   needed   info.   The   Supabase

Postgres   persists   all   critical   auth   data   so   that   the   system   remains   stateful   (e.g.   allowing   session

persistence across restarts).

Docker Compose Setup

We will use Docker Compose to orchestrate all components on a common network. Each service runs in

its own container, and we define environment variables, networking, and volumes for data persistence.
Below is a sample  docker-compose.yml  snippet illustrating the setup:

version: '3.8'

services:

db:

image: postgres:15-alpine

# Using Postgres (Supabase-

compatible) for data

container_name: supabase-db

environment:

POSTGRES_USER: postgres

POSTGRES_PASSWORD: <YOUR_DB_PASSWORD>

POSTGRES_DB: postgres

# Default database (used by

BetterAuth)

volumes:

- supabase-data:/var/lib/postgresql/data

networks:

- internal

# (Optional: expose 5432 if you need external DB access)

api:

3

build: ./hono-api

# Hono API + BetterAuth service

(Dockerfile builds the Node app)

container_name: hono-api

depends_on:

- db

- convex

environment:

DATABASE_URL: postgres://postgres:<YOUR_DB_PASSWORD>@db:5432/postgres

BETTER_AUTH_SECRET: <YOUR_AUTH_SECRET>

BETTER_AUTH_URL: http://localhost:4000

# Base URL of the Hono API

service (for BetterAuth config)

10

# OIDC settings, e.g. client IDs, can be set in auth config or env as

needed

networks:

- internal

ports:

- "4000:4000"

endpoints) on port 4000

convex:

# Expose Hono (and BetterAuth

image: ghcr.io/get-convex/convex-backend:latest

container_name: convex-backend

depends_on:

- db

environment:

DATABASE_URL: postgresql://postgres:<YOUR_DB_PASSWORD>@db

#

Convex will use Postgres; ensure DB exists

9

# (No DB name here because Convex expects a default; we'll create a

'convex_self_hosted' DB in the Postgres service)

CONVEX_CLOUD_ORIGIN: http://convex:3210

# Convex internal URL

for core service

CONVEX_SITE_ORIGIN: http://convex:3211

# Convex internal URL

for HTTP actions

# (Above origins default to localhost; here we set them to container

name for clarity on the network)

networks:

- internal

ports:

- "3210:3210"

(database queries, etc.)

- "3211:3211"

# Convex client connection

# Convex HTTP endpoints (actions, webhooks)

# (Optional: expose dashboard on 6791 if using convex-dashboard

container)

frontend:

build: ./tanstack-start

# TanStack Start SSR app (Node

server or static build)

container_name: tanstack-frontend

depends_on:

- api

4

environment:

PUBLIC_AUTH_URL: "http://localhost:4000/api/auth"

# URL to BetterAuth endpoints (through Hono)

PUBLIC_API_URL: "http://localhost:4000/api"

# Base URL for Hono API

PUBLIC_CONVEX_URL: "http://localhost:3210"

# Convex URL for

client SDK

# OIDC client ID, redirect URIs could be configured here if needed

networks:

- internal

ports:

- "3000:3000"

3000 for users

# Expose frontend (SSR) on port

# (If TanStack Start is purely static, you might serve it differently)

networks:

internal:

driver: bridge

volumes:

supabase-data:

Networking: All services join the  internal  network, which allows them to communicate by container
name. For example, the Hono service can reach Postgres at host   db:5432 , and Convex can reach

Postgres at the same hostname. In the Compose file, we set environment URLs accordingly (e.g. the
DATABASE_URL   for BetterAuth uses host   db ). We expose only the needed ports to the host: - The

frontend (TanStack Start) is exposed on port 3000 (for users to access the web app). - The Hono API

(which   also   hosts   BetterAuth’s   OIDC   endpoints)   is   on   port   4000.   -   The  Convex   backend  exposes
3210/3211 for client connections. The frontend will connect to Convex at  localhost:3210  (mapped

to the convex service). - The  Postgres  service is  not  exposed to the host (for security), but you can

expose 5432 if you need direct database access for admin tasks. It’s confined to the internal network for

use by BetterAuth and Convex.

Environment Variables & Secrets: We use environment variables to configure each service securely: -
Postgres   ( db   service):  Set   a   strong   POSTGRES_PASSWORD .   (In   a   production   setup,   also   consider
using   Docker   secrets   for   the   password).   The   volume   supabase-data   ensures   data   is   persisted
between container restarts. - BetterAuth (within  api  service): -  DATABASE_URL : Connection string
.   This   points   to   the   db   service   and   includes   the
for   the   Postgres   DB   that   BetterAuth   will   use
credentials.   In   our   example,   BetterAuth   will   store   its   tables   in   the   default   postgres   database.
(Optionally, you could set   POSTGRES_DB: auth   on the db service and use that for BetterAuth, to
separate it from other data.) -  BETTER_AUTH_SECRET : A secret key for BetterAuth used for encryption/
. This should be a long random string (you can generate one with  openssl rand -hex

hashing
32   or use BetterAuth’s CLI)
. It’s critical to keep this secret consistent across deployments (so that
session tokens/hashes remain valid) and  never expose it publicly. -   BETTER_AUTH_URL : The base

10

3

4

URL of the application (used by BetterAuth for OIDC issuer identification, callback URLs, etc.)

10

. In

development we set it to localhost (as shown). In production, this would be your API’s external URL (e.g.
https://api.myapp.com ). This ensures tokens have the correct issuer and any absolute URLs in

emails or redirects are correct. - OIDC Client config: If using BetterAuth’s OIDC provider plugin, you will

configure OIDC clients. For example, you might register the frontend as a public client in BetterAuth
(with a client ID like  tanstack_frontend  and redirect URI  http://localhost:3000  for dev). This

5

can be done via BetterAuth’s API/CLI

11

, or by adding a config snippet. In a simple setup, you might

not need to set these as env vars – you could register clients in code or at runtime. Ensure that the

client ID (audience) you use here is the same one Convex expects (we will configure Convex with it as

well). - Trusted Origins (CORS): In the BetterAuth config, specify the allowed origins for requests. We
include the frontend’s origin here. For example, in code:  trustedOrigins: ['http://localhost:
3000']   to   allow   the   dev   server’s   origin

.   This   is   important   for   security   –   BetterAuth   will   block

12

requests (like XHR for token refresh or login) from origins not listed
. In our integration, because we
serve BetterAuth through Hono at the same origin as the API, the frontend will be calling   http://
localhost:4000   from   http://localhost:3000 ,   so   we   need   http://localhost:3000   as   a
trusted origin. -  Convex ( convex   service):  -   DATABASE_URL : Tells Convex to use Postgres for its

13

storage (instead of the default SQLite)

9

. We provide the connection without a database name so that

Convex will use the default and look for (or create) its own DB. Convex by default uses a database
named  convex_self_hosted  in the Postgres instance
. We must ensure this exists – we can do so
by extending our Compose setup: for example, add an initialization script in   docker-entrypoint-
initdb.d  for the Postgres service to create that database. (Alternatively, manually connect once and
create   the   DB.)   For   clarity,   you   could   also   use   POSTGRES_DB:   convex_self_hosted   in   the

14

environment, but that would set the default database for the Postgres container itself; better to create

an   additional   DB   for   Convex   explicitly.   -  Convex   Auth   Config:  In   Convex   application   code   (not   an
environment variable), we set up   auth.config.ts   to recognize BetterAuth. We add an entry with
domain: "<BetterAuth Issuer URL>"   and   applicationID: "<OIDC Client ID>"
. For
example, if BetterAuth’s issuer is  http://localhost:4000  (as set by  BETTER_AUTH_URL ) and the
client   ID   for   our   frontend   is   tanstack_frontend ,   we   put   those   in   Convex’s   config.   This   ensures
Convex   will   accept   tokens   where   iss   matches   the   BetterAuth   URL   and   aud   matches
. (If BetterAuth’s ID tokens do not include an   aud , or you want to accept
tanstack_frontend

15

7

tokens without strict aud matching, Convex also supports a custom JWT mode

16

  – but using proper

OIDC fields is recommended.) - No sensitive secret is needed for Convex here (except the DB password

which is already in the connection string). However, Convex does have an  admin key  for deploying

updates or using the dashboard. We will generate this key after the container is running (using the
provided script  ./generate_admin_key.sh  inside the convex container)

. That key can be set as

17

an   environment   variable   or   provided   when   using   the   Convex   CLI   to   deploy   function   code.   For   this

architecture, the admin key is not directly involved in runtime auth flow, so we simply note that it should
be kept secret if used. - Frontend ( frontend  service): - We pass configuration for the frontend app to
know how to reach the other services. For example,  PUBLIC_AUTH_URL  is the endpoint of BetterAuth’s
routes.   In   development   it   might   be   http://localhost:4000/api/auth   (pointing   to   Hono),   as

shown above. This could be used by a BetterAuth client SDK on the frontend to direct login requests.
  PUBLIC_API_URL   could   be   used   for   constructing   REST   API   calls   to   Hono,   and
Similarly,
PUBLIC_CONVEX_URL   for Convex. - If the TanStack Start app needs to know the OIDC  client ID  or

redirect   URL,   those   can   be   baked   into   the   app   or   provided   via   env.   Typically,   you’ll   configure   the

BetterAuth client library in the frontend with the issuer and client ID. For instance, if using BetterAuth’s

client plugin, you might instantiate it with the known config (client ID, issuer, scopes). - The TanStack

Start  framework  supports  server-side  rendering  and  server  functions,  so  it  might  also  interact  with

BetterAuth on the server side. In a full SSR scenario, instead of using the BetterAuth endpoints directly

from the browser, the SSR server could handle the OAuth redirect callback: e.g., the user is redirected

back to a TanStack Start server route, which then calls BetterAuth (via Hono) to exchange the auth code

for tokens and set an HttpOnly cookie for the session. This approach would involve the frontend talking

“through” the backend for auth. We’ll discuss this in the next section.

All services share the  internal  network, allowing them to refer to each other by service name (e.g.,
db ,   api ,   convex ,   etc.).   We’ve   used   depends_on   to   ensure   that,   for   example,   the   Hono   API

doesn’t start before the database and Convex are up.

6

Volumes: We defined a named volume  supabase-data  for Postgres data persistence. You might also

mount volumes for any service that needs to persist data (Convex stores most data in Postgres, but if it

used any local disk for e.g. search indexes or files, you’d mount those too). The Convex container’s
data  volume (for SQLite or other ephemeral data) is less crucial if using Postgres, but the Compose

file we based on uses an in-container volume for any data directory by default

18

19

. In our case, the

critical persistence is already handled by Postgres.

Authentication Flow Details

To   ensure   clarity,   here’s   a   step-by-step   authentication   flow   from   the   frontend   to   backend   in   this

integrated system:

1.

User Initiates Login: The user navigates to the TanStack Start frontend. If not authenticated, the

frontend (TanStack app) will present a login option. When the user clicks “Login,” the app triggers

BetterAuth’s authentication flow. This could happen via redirect – e.g., the app redirects the
browser to  http://localhost:4000/api/auth/sign-in  (the BetterAuth sign-in page

served by Hono), or opens a pop-up. Alternatively, if using BetterAuth’s JavaScript client, it may

perform a fetch to the auth endpoint. Either way, the user is now interacting with BetterAuth
(through the Hono service) for credentials. The BetterAuth service (on the  /api/auth  routes)

serves the login form (or forwards to an identity provider if using social logins).

2.

BetterAuth Authenticates User: The user enters credentials (for example, email and

password). BetterAuth verifies these – possibly consulting the Postgres DB for the user’s hashed

password and checking it. On success, BetterAuth creates a new session entry in the database

(recording the user ID, session token, expiration, etc.)

20

21

. If using an OIDC code flow,

BetterAuth now generates an authorization code. If using an implicit flow or BetterAuth’s own

session mechanism, it might directly create a JWT and a session cookie. Assuming Authorization
Code Flow: BetterAuth redirects the user back to a specified callback URL (e.g.,  http://
localhost:3000/auth/callback  on the frontend) with the authorization code.

3.

Token Issuance (OIDC Code Exchange): The TanStack frontend (or its SSR server) handles the

callback. If the code is returned to the frontend app (browser), the app now makes a back-
channel request to BetterAuth’s token endpoint ( /api/auth/callback  or
/api/auth/token ) to exchange the code for tokens. Often this request is done by the frontend

server to keep the client secret safe (for confidential clients). In our setup, since the frontend is a

public client (no secret in browser), we can use PKCE. The frontend sends the code + PKCE

verifier to BetterAuth (again hitting the Hono API’s auth route). BetterAuth validates the code

and responds with an ID token (JWT) and access token, and possibly a refresh token
ID token contains the user’s identity claims (e.g.,  sub  claim as user ID, email, etc.), and the

2

. The

access token is typically a shorter-lived JWT meant for resource server authorization.

4.

Frontend Stores Credentials: The frontend now has the tokens. If TanStack Start has an SSR

component, it might set an HttpOnly cookie with the access token or a session identifier at this

point (particularly if the SSR server did the token exchange, it can set a cookie on the app’s

domain). Alternatively, in a pure SPA scenario, the tokens might be stored in memory or

localStorage (though http-only cookies are more secure). The user is now logged in from the

frontend’s perspective.

5.

Frontend Calls Protected APIs: The user interacts with the app (e.g., clicking a “Load my profile”

button). The frontend needs data from the backend, so it makes a request to Hono’s API (for
example,  GET /api/profile ). It includes the access token in the  Authorization  header:
Bearer <JWT> . If we opted to use cookies for auth, the cookie (containing a session or token)

would be automatically sent. The frontend may also initiate a connection to Convex at this point

7

(the Convex client library will take the ID token and call  ConvexClient.setAuth(token)  to

include it in its WebSocket communications).
Hono API Verifies Token: The Hono server receives the API call at  /api/profile  (for

6.

instance). We have secured this route by requiring authentication. In the Hono code, before

reaching the handler, a middleware (or the handler itself) uses BetterAuth to verify the request. If
we integrated BetterAuth fully, we might call  auth.getSession()  or similar to validate any

session cookie, or we manually validate the JWT. JWT validation involves checking the signature
with BetterAuth’s public key. Hono fetches the JWKS from BetterAuth’s  .well-known  endpoint

(if not cached)
verify the JWT signature. It also checks that the token has not expired ( exp  claim) and that the

, and finds the key that matches the token’s  kid . It then uses that key to

8

issuer matches our BetterAuth URL and audience matches our client ID (if we configured Hono

to expect a certain aud). With a valid token, Hono knows the user is authenticated. It may then

decode the JWT to get the user’s ID and any claims (like roles or permissions if included) to

authorize the specific action.

7.

Convex Verifies Token (if accessed): Meanwhile, if the frontend opened a Convex connection or

called a Convex function, the Convex backend performs a similar verification. Convex’s server
(running in the  convex  container) inspects the token sent by the Convex client. According to
our Convex auth config, it expects  iss == http://localhost:4000  (BetterAuth) and  aud
== tanstack_frontend  (for example). It finds the token satisfies these (BetterAuth issued it

with that audience). Convex then uses the cached JWKS for BetterAuth to verify the token’s

signature as well

6

. (Convex caches the JWKS from the domain given in config; BetterAuth’s

OIDC plugin exposes the JWKS URL by default under the base auth URL

22

.) If valid, Convex

establishes an authenticated session for the client connection, associating it with the user
described in the JWT. From now on, any Convex function can call  auth.getUser()  (or similar)

to get the user’s ID and claims, knowing they were validated.

8.

Backend Processes Request: The Hono API handler now executes the requested operation (e.g.,

fetch user profile). It might query the Postgres database (since BetterAuth stores user info

there, Hono could even join additional data from user profiles table, etc.). For example, Hono
could run a SQL query on the  users  table (populated by BetterAuth) or on another table that
references  users . Since Hono trusts the user’s identity from the token, it can safely use the
userId  to query only that user’s data. Hono could also call a Convex function from the server

side – for instance, if some data is in Convex, Hono might use the Convex HTTP API to retrieve it.

In that case, Hono would include either an admin auth (to call Convex as a privileged service) or

forward the user’s token in the request to Convex. For simplicity, our architecture leans toward

the frontend calling Convex directly, but these options exist for server-to-server communication.

9.

Responses Sent Back: After performing the logic, Hono sends the HTTP response (e.g., JSON

with profile data) back to the frontend. Convex, if queried, sends its response data (over the

WebSocket or HTTP response for a function call) back to the frontend as well. The TanStack

frontend now has the data it needs. From the user’s perspective, they seamlessly fetched

protected data after logging in. All subsequent interactions follow a similar pattern: the frontend

presents a token, backends verify it. When the token expires, BetterAuth’s refresh token can be
used by the frontend (likely via a silent refresh call to  POST /api/auth/session/refresh  or

similar) to get a new token, which again involves checking the session in the database. This

keeps the user logged in without re-entering credentials, until logout.

Throughout these steps,  BetterAuth is the core  that ties everything together: it provided the tokens

that   both   Convex   and   Hono   rely   on   for   authenticating   requests.   Supabase   Postgres   ensures   that

BetterAuth’s state (users, sessions, etc.) is consistent and durable. Convex and Hono do not need their

own user databases – they refer to the identity in the BetterAuth token and can, if needed, query the

central user info stored in Postgres (for example, Hono could query additional user profile fields in

Postgres, or Convex could periodically sync certain user data).

8

This flow shows a division of concerns: BetterAuth handles auth, Convex handles real-time data logic,

Hono  handles RESTful API and any server-side rendering or integration logic, and  Postgres  persists

data. All components communicate over the internal Docker network securely, and external exposure is

limited to the necessary endpoints for the end-user.

Frontend Integration: Direct vs. Proxy communication with

BetterAuth

One important consideration is whether the frontend should communicate directly with the BetterAuth

service or go through the Hono backend for authentication flows. We have two possible approaches:

•

Direct   Frontend   ↔   BetterAuth   communication:  In   this   model,   the   frontend   (TanStack   app

running in the browser) talks to BetterAuth’s endpoints directly (which could be on a separate
subdomain or port, e.g.   auth.myapp.com   or   localhost:4000 ). This is analogous to how

one might use an external provider like Auth0 – the SPA redirects to Auth0 and gets tokens from

it.   The   advantage   is   that   the   authentication   flow   is   handled   by   the   dedicated   auth   service

(BetterAuth)   and   the   frontend   obtains   tokens   to   use   on   other   APIs.   This   can   simplify   token

exchange in a public client scenario (BetterAuth’s OIDC plugin supports public clients for SPA/

mobile

23

). However, it does introduce  cross-origin considerations. We must enable CORS in

BetterAuth for the frontend origin, and the redirect URIs must be configured. In our setup, since

BetterAuth is actually served via the Hono container (on port 4000), “direct” means the browser
calls  localhost:4000  from an app served on  localhost:3000  – which is cross-origin. We
handle   this   by   setting   trustedOrigins:   ['http://localhost:3000']   in   BetterAuth,
which ensures the proper   Access-Control-Allow-Origin   headers and prevents CSRF

12

13

. In production, this list would include your actual frontend URL(s). The direct approach keeps

the backend (Hono) mostly out of the login process (aside from hosting the endpoints), and the

frontend deals with tokens.

•

Frontend   ↔   Hono   (Proxy)   ↔   BetterAuth:  In   this   approach,   the   frontend   communicates   with

BetterAuth  through  the   Hono   backend   (which   is   possible   since   BetterAuth   is   integrated   into

Hono). Essentially, the Hono server acts as an intermediary or even handles the auth flow server-

side. For example, instead of the SPA doing a OAuth code exchange, the frontend could simply
hit an endpoint on Hono (e.g.  /api/auth/login ) and Hono could perform a server-side OIDC

flow then set a cookie for the frontend. TanStack Start, being a full-stack framework, supports

server-side logic – we could leverage that to manage auth. The potential benefit here is security

and simplicity for the client: the frontend doesn’t need to handle tokens explicitly; Hono could

issue   an   HttpOnly   session   cookie   (containing   the   BetterAuth   access   token   or   a   session

reference).   This   cookie   would   be   on   the   frontend’s   domain,   making   API   calls   automatically

include  it,  and  Hono  can  validate  the  cookie  via  BetterAuth.  This  approach  is  similar  to  how

Next.js with NextAuth might operate – the backend takes care of exchanges and the client just

gets a cookie. It can also avoid storing tokens in JavaScript memory at all (reducing XSS risk).

Another benefit is that all auth traffic stays on the same origin (the frontend domain or API

domain), so CORS issues are minimized.

Recommendation – Hybrid Approach:  Given our architecture, we have  already integrated BetterAuth

with Hono, meaning the distinction between “direct” and “through Hono” is somewhat blurred – the

frontend’s requests to BetterAuth endpoints are in fact going to the Hono service (which routes them to

BetterAuth handler). In development, this is just a different port, but in production we might deploy
Hono and frontend under a single domain (e.g.,  api.myapp.com  hosting both API and auth, with the
frontend app at  myapp.com  or another domain).

9

For a smooth implementation,  we recommend using the frontend’s server capabilities (TanStack

Start’s SSR) to handle the OAuth callback through Hono, resulting in an HttpOnly cookie for the

session.   In   practice,   this   means   the   frontend   will  redirect   the   user   to   BetterAuth’s   sign-in   page

(direct) for login, but the return callback will be caught by a TanStack Start server route which then talks

to BetterAuth’s token endpoint (server to server). This yields tokens which the server can set as a secure

cookie. Subsequent frontend calls to the API will include this cookie (same-site or same-domain), and

Hono can verify the session via BetterAuth. This approach offloads complexity from the client code (no

manual JS token storage) and centralizes auth logic in the backend. It also mitigates cross-origin issues

– the only cross-origin navigation is the initial redirect for login, which is normal in OIDC. All XHR/fetch

calls after login would be same-origin (frontend <-> Hono).

If we wanted to avoid even the initial cross-origin redirect, an alternative is to have the frontend call a

Hono endpoint to initiate login (Hono could then redirect the user to BetterAuth, acting as a proxy).

However, this is usually unnecessary overhead; it’s standard to let the browser redirect to the IdP for

authentication.

In summary, the frontend should talk to BetterAuth’s OIDC endpoints through the Hono service –

and since Hono hosts those endpoints, the difference between direct and proxy is minimal. We ensure

that: - The frontend is configured with the correct base URL for BetterAuth’s endpoints (in our example,
http://localhost:4000/api/auth ). This allows the front-end to know where to send the user for

login or which URL to use for token refresh calls. - We include the frontend’s origin in BetterAuth’s
trustedOrigins   so that any direct XHR communication is allowed

. - For production, consider

13

using the backend to manage tokens and set cookies. This means the TanStack Start server would need

credentials   to   communicate   with   BetterAuth   (client   ID   &   secret   if   using   confidential   flow).   Given

BetterAuth supports first-party integration, this is feasible.

By following this plan, the TanStack Start frontend will have a robust authentication setup where it

either uses BetterAuth’s client SDK or simple redirects to log users in, and all subsequent interactions

with Convex or Hono will automatically carry the user’s identity. The decision to go “direct vs through

Hono” essentially boils down to where token exchange happens: our recommendation is to perform

token   exchange   and   storage   on   the   server   side   (through   Hono),   which   provides   better   security.

Regardless,   both   methods   rely   on   BetterAuth   as   the   single   auth   provider   and   thus   will   function

correctly.

Conclusion

This implementation combines the strengths of each component:  BetterAuth  for flexible, self-hosted

authentication   (with   OIDC   capabilities),  Supabase   Postgres  for   a   reliable   data   store,  Convex  for   a

reactive   backend   datastore,  Hono  for   a   fast   API   layer,   and  TanStack   Start  for   a   modern   full-stack

frontend. By using Docker Compose, we ensure these services run together and can communicate over

a   secure   internal   network.   Each   service   is   configured   with   the   necessary   environment   variables

(database URLs, secrets, client IDs) and appropriate Docker networking so they can discover each other
(using service names like  db ,  api , etc.).

Security is maintained at each boundary: - All passwords/secrets (DB password, BetterAuth secret, any

OAuth   client   secrets)   are   kept   out   of   source   code   and   injected   via   environment   variables

3

4

(consider using Docker secrets or environment files with proper .gitignore in place). - Communication

between   services   does   not   leave   the   Docker   network   –   e.g.,   Hono   queries   the   database   directly,

avoiding   external   exposure.   -   The   frontend   and   API   can   be   configured   with   TLS   (outside   of   Docker

10

Compose   scope,   typically   via   a   reverse   proxy   like   Nginx   or   Caddy   in   front   of   these   containers   in

production).

With this setup, when a user registers or logs in via BetterAuth, their data is stored in Postgres and an

OIDC session is established. The frontend can retrieve application data through Convex or Hono, and

both   of   those   trust   BetterAuth’s   tokens   to   authenticate   requests.   This   unified   approach   avoids

duplicating user management in multiple services and provides a clear separation of concerns:

•

BetterAuth + Postgres: Identity and session management (authentication).

•

Convex: Application data and real-time updates (with built-in auth via OIDC tokens).

•

Hono + BetterAuth (mounted): API gateway/business logic, also serving auth endpoints for

BetterAuth.

•

TanStack Start: Frontend UI/UX, utilizing the backend services securely.

By   following   this   plan   and   using   the   sample   Compose   configuration   as   a   starting   point,   you   can

implement   a   cohesive,   self-hosted   system   where   users   authenticate   once   via   BetterAuth   and   gain

secure access across your frontend and backend components. The result is a scalable and maintainable

architecture   with   centralized   auth   and   seamless   integration   between   the   modern   tech   stack

components.

References:

•

BetterAuth documentation on using PostgreSQL and configuration

, and OIDC plugin
. BetterAuth’s integration with Hono (example showing  trustedOrigins  and

features

2

4

3

route mounting)

12

5

.

•

Convex documentation for custom OIDC integration and self-hosting details

7

6

9

. These

show how Convex accepts ID tokens from an OIDC provider and how to configure a Postgres DB

for Convex.

•

Docker Compose configuration and environment setup for Convex’s self-hosted backend (open-

source)

9

24

, which guided our Compose file structure.

1

20

21

Installation | Better Auth

https://www.better-auth.com/docs/installation

2

8

11

23

OIDC Provider | Better Auth

https://www.better-auth.com/docs/plugins/oidc-provider

3

4

How to connect to your Supabase Database using Better-Auth | by Joshua K Barua | Medium

https://medium.com/@joshua.k.barua/how-to-connect-to-your-supabase-database-using-better-auth-aa6be3e985e1

5

10

12

13

Better Auth - Hono

https://hono.dev/examples/better-auth

6

7

15

16

22

Custom OIDC Provider | Convex Developer Hub

https://docs.convex.dev/auth/advanced/custom-auth

9

14

17

Self-Hosting with Convex: Everything You Need to Know

https://stack.convex.dev/self-hosted-develop-and-deploy

18

19

24

raw.githubusercontent.com

https://raw.githubusercontent.com/get-convex/convex-backend/main/self-hosted/docker/docker-compose.yml

11

