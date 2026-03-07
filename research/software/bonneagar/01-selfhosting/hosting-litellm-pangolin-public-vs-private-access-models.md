Hosting LiteLLM on Pangolin: Public vs. Private

Access Models

Public Access via Pangolin Domain (Open Endpoint)

In this model, you expose the LiteLLM server over the internet through Pangolin’s reverse proxy and

domain routing. Pangolin will handle HTTPS termination (via Let’s Encrypt) and route requests to your

LiteLLM instance running on the VPS. This allows third-party integrations (e.g. Hugging Face tools or

remote apps) to call your LiteLLM endpoint without any VPN client. Key steps and configurations:

•

Domain   &   DNS   Setup:  Point   a   domain   or   subdomain   to   the   Pangolin   server.   If   self-hosting
Pangolin,   configure   a   wildcard   DNS   (e.g.   *.example.com )   to   your   VPS’s   IP

.   If   using

1

Pangolin   Cloud,   you   can   skip   custom   DNS   by   using   Pangolin’s   provided   domains   (like
*.hostlocal.app   or   *.tunneled.to )

.   Ensure   TCP   port  80  (for   Let’s   Encrypt   HTTP

2

validation)  and  443  (for  HTTPS  traffic)   are   open   on   the   VPS

3

  –   Pangolin   will   automatically

obtain and renew TLS certificates for your domain.

•

Add Pangolin Resource (Reverse Proxy): In the Pangolin dashboard, navigate to Resources >

Add Resource
. Provide a descriptive Name for the service and select Type: HTTP/HTTPS. Set
the  Domain  to your chosen hostname (e.g.   litellm.example.com ) which users will access

4

5

. Specify the Target as the LiteLLM service’s address on the private site – this is the IP/port
where  LiteLLM  runs  on  your  VPS  (for  example,   127.0.0.1:4000   if  LiteLLM  listens  on  port

4000)

5

. Pangolin’s tunnel will route incoming domain traffic to this target.

•

TLS & Reverse Proxy Configuration:  Pangolin uses Traefik under the hood to proxy HTTP(S)

traffic. When you added the resource with a domain, Pangolin automatically set up a secure

HTTPS endpoint. Traefik listens on port 443 with a Let’s Encrypt certificate resolver
, so all
traffic to  https://litellm.example.com  will be encrypted. No manual TLS setup is needed

6

beyond   providing   your   email/domain   during   Pangolin   install   (the   installer   handles   certificate

issuance

7

).

•

Access Control (Public Endpoint):  By default, Pangolin is an identity-aware proxy, meaning it

can require login or PIN for access. Since this endpoint is intended for programmatic use by third

parties,   you   likely   want   it   open   without   interactive   auth.   Pangolin   supports  Allow   Rules  to

bypass authentication

8

. In the Access Control settings, create an allow-rule for this resource

(or set the resource’s access to “public”) so that requests skip any login/PIN requirements

8

.

This effectively makes the domain publicly accessible to anyone (or anyone who knows a secret

API key your LiteLLM might require). Make sure to secure the LiteLLM API itself (e.g. via an API

token or key) if you want to restrict usage, because with public access the endpoint is reachable

by the internet at large.

Configuration Example: For instance, suppose your Pangolin site is named “VPS-Site” and LiteLLM runs

on port 4000 on that VPS. You could configure a resource as follows:

•

Domain: api.example.com  (CNAME or A record pointed to Pangolin’s IP/domain)

1

•

•

Resource Type: HTTP (Pangolin proxies via TLS on port 443)
Target: 127.0.0.1:4000  on VPS-Site (LiteLLM’s local endpoint)

5

•

Access Control: Allow (no auth required, using a rule to bypass Pangolin login)

8

Once set up, external clients can reach the LiteLLM server at   https://api.example.com   without

needing any VPN or special client. Pangolin’s reverse proxy will forward the HTTPS requests through its

tunnel to the LiteLLM service on the VPS.

Public Access Use Cases:  This approach is ideal when you need to integrate LiteLLM with external

services or provide an API endpoint for customers/partners. It prioritizes ease of access – users just hit

a standard HTTPS URL – and Pangolin still provides security through TLS and optional identity checks.

However, since the service is exposed to the internet, you should harden it (use API keys or rate limiting

in LiteLLM) to prevent abuse. This model is slightly less restrictive than the VPN-only approach, trading a

bit of attack surface for simplicity of access.

Private Access via Pangolin Olm VPN (Restricted Endpoint)

In this model, the LiteLLM service is not exposed on any public domain. Instead, it’s accessible only to

authenticated   team   members   through   Pangolin’s   Olm   VPN   client.   Olm   (Pangolin’s   WireGuard-based

client)   creates   a   secure   tunnel   into   your   Pangolin   network,   so   only   users   with   the   proper   Olm

credentials can reach LiteLLM. The service remains invisible to the open internet, greatly enhancing

security

9

. To set this up, you will use Site Resources (internal resources) in Pangolin:

•

Enable Client Access on the Site: Ensure your Pangolin site (the VPS) accepts client connections.
When running the Pangolin agent ( newt ) on the VPS, use the   --accept-clients   flag (or

enable the equivalent setting in Pangolin)

9

. In this mode, Newt runs entirely in userspace

without   creating   a   local   network   interface,   and   Pangolin   will   route   client   traffic   for   specific

resources you define. This means the VPS doesn’t act as a full VPN gateway; it only exposes the

ports you explicitly configure for Olm clients. (No root privileges or OS-level networking changes

are needed on the VPS in this mode

9

.)

•

Define a Site Resource for LiteLLM: In the Pangolin dashboard, go to Resources and switch to

the Site Resources view

10

. Click Add Resource and choose TCP (since LiteLLM’s API runs over

HTTP/TCP). Set a  Local Port  that Olm clients will use to connect (this can be the same as the
LiteLLM port or an arbitrary port not in use – e.g.  4000  for clarity)
. Then specify the Target
address  as the LiteLLM service’s local address on the site (for example,   127.0.0.1:4000   if

11

LiteLLM is listening on localhost:4000)

12

. This tells Pangolin to forward traffic from the site’s

VPN interface (on that local port) to the actual service. Save the resource configuration.

•

Client Connection with Olm: Next, add a Pangolin  Client (in the Pangolin UI under  Clients) to

generate   an   Olm   ID   and   secret   for   each   team   member   or   system   that   needs   access.   Team

members   will   install   the   Olm   client   and   configure   it   with   the   provided  ID,  secret,   and   the
. When an authorized user runs  olm --
Pangolin endpoint (your Pangolin server’s URL)
id   <client-id>   --secret   <secret>   --endpoint   https://<pangolin-server>   on

13

14

their machine

15

, Olm registers and establishes a WireGuard tunnel to the Pangolin network.

Once   the   Olm   VPN   is   connected,   the   user’s   system   is   virtually   inside   the   Pangolin   private

network.

•

Access LiteLLM via the VPN: When connected via Olm, the user can reach the LiteLLM service

using   the  site’s   virtual   IP   and   the   configured   port.   Pangolin   assigns   each   site   a   virtual   IP

2

(visible in the Pangolin dashboard, often in a range like 100.64.x.x or 100.90.x.x) for the VPN
tunnel. For example, if your site’s VPN IP is   100.90.128.0   and you mapped LiteLLM to port
4000 ,   the   user   would   access   http://100.90.128.0:4000   to   hit   the   LiteLLM   API

16

.

Pangolin will route that request through the WireGuard tunnel directly to the LiteLLM service on

the VPS. Only clients who have connected with a valid Olm configuration (ID/secret) can reach

this address – anyone not on the Pangolin VPN cannot even see or connect to the service

9

.

No public DNS or TLS setup is required here, since the service is not exposed via a public domain. All

traffic  is  end-to-end  encrypted  at  the  WireGuard  layer.  (If  needed,  you  could  still  run  HTTPS  on  the

LiteLLM service itself for double encryption, but it’s typically not necessary because the VPN provides

secure transport.)

Configuration Example:  Suppose LiteLLM runs on port 4000. In  Site Resources  you add:  Type:  TCP,
Port:  4000,  Target: 127.0.0.1:4000
. After connecting with Olm, a team member can use the
LiteLLM API by targeting the site’s IP (e.g.   100.90.128.0:4000 ) in their HTTP client. The Pangolin

11

newt agent will forward that to the real localhost:4000 on the VPS. This approach is  ideal for secure

internal access without exposing services to the public internet

9

17

.

Private   Access   Use   Cases:  This   VPN-gated   model   is   suited   for  internal   tools   and   sensitive
applications. It provides strong security – the LiteLLM server is effectively air-gapped from the internet,

accessible only to authenticated VPN clients. Use this when the LLM service is for your team or company

only,   or   when   compliance/security   policies   forbid   exposing   an   endpoint   publicly.   The   trade-off   is

convenience:   each   user   must   run   the   Olm   client   (or   be   on   the   Pangolin   network)   to   connect.   This

method isn’t feasible for third-party services that can’t run a VPN client, but it’s perfect for developers

and team members who can run Olm on their laptops or servers to gain access.

Comparison of Public vs. Private Models

Security: The private (Olm) model offers the highest security since the LiteLLM endpoint is invisible to

the internet – only users with VPN credentials can reach it

9

. This greatly reduces attack surface. The

public domain model is less restrictive: anyone can reach the endpoint URL, so you rely on application-

level security (API keys, auth tokens, or Pangolin’s access rules) to protect it. Pangolin can still secure the

public model with SSL and optional login/SSO, but fundamentally it’s reachable from anywhere, which

requires more vigilance (e.g. monitoring and rate-limiting) compared to a VPN-restricted service.

Simplicity & User Convenience: The public access model is simpler for clients/integrations – no special

software is needed to connect, just standard HTTPS requests. It’s ideal for integrating with external

tools or services that expect a normal web endpoint. Domain and TLS setup is mostly automated by

Pangolin (especially with wildcard DNS or Pangolin’s cloud domains), so configuration is straightforward

5

3

. The private model, on the other hand, requires extra steps for each user (installing/configuring

the Olm VPN client and connecting before use). This added complexity means it’s best for known users

who can be instructed to use the VPN. In summary,  Olm is not necessary  when you want an easily

accessible public API, but  Olm becomes essential  when you need to restrict access to trusted users

only – it’s the difference between an open service and one that lives behind a secure VPN barrier.

Performance:  Both   methods   utilize   Pangolin’s   efficient   WireGuard   tunneling,   but   there   are   minor

differences. With public access, the client’s requests come in over HTTPS to the Pangolin server (or

cloud   node)   and   then   traverse   the   WireGuard   tunnel   to   the   site.   With   Olm,   the   client   itself   is   on

WireGuard  –  effectively,  traffic  goes  through  the   VPN   tunnel   directly.   In  practice,   the  overhead   and

latency are comparable. Olm’s tunnel might add a tiny constant latency (due to encryption/decryption

3

on the client side), whereas the public model adds latency on Pangolin’s reverse proxy handling. These

differences   are   usually   negligible   –   WireGuard   is   very   fast,   and   Pangolin’s   proxy   is   optimized   for

performance. If using Pangolin Cloud, the public model can even route users to the nearest Pangolin

node for improved latency, whereas Olm may route you through a specific region – but unless your

users are globally distributed, this is a small factor. For most use cases, both approaches can handle

real-time LLM traffic well, and throughput is more constrained by the LLM processing speed or network

bandwidth than by Pangolin itself.

Use Cases and When to Use Which: Use the public domain approach when you need to expose an

API endpoint for integration with external services, webhooks, or client applications that cannot easily

use a VPN. For example, if you want to plug your LiteLLM service into a cloud workflow (Hugging Face

pipeline, external web app, etc.), a public HTTPS URL is the way to go – Olm isn’t feasible in those

scenarios. On the other hand, choose the private/Olm approach for in-house or development setups

where security trumps convenience: e.g. an internal LLM tool for your team, or a staging server that

only developers should access. In those cases, running the Olm client to access the service is a non-

issue for users, and you avoid exposing any endpoint to the internet. It’s also possible to start private

and later move to public if needed – Pangolin is flexible. You could even mix models: keep the service

private for most users, but create a public resource for specific purposes (with strict rules or limited

scope) when necessary. The key takeaway is that Olm is necessary only when you deliberately want

to keep a service private to your Pangolin network, and it’s overkill if your goal is to offer a public-

facing service. Pangolin’s domain routing gives you the option to go either way: open and accessible, or

closed and VPN-gated, depending on your needs for security vs. accessibility.

References:  The   configurations   above   are   based   on   Pangolin’s   official   documentation   and   usage

guides. For instance, Pangolin’s docs outline how to add an HTTPS  Resource  with a custom domain,

pointing to a private service’s IP:port and controlling access

18

. Pangolin will automate TLS via Let’s

Encrypt as long as DNS is configured and ports 80/443 are open

3

. For the private model, Pangolin’s

Client Resources  feature is designed specifically to “expose internal services to your remote clients

securely” over the Olm VPN

19

, with no public proxying. The internal resource method is highlighted as

ideal for “services without exposing them to the public internet”

9

  – only Olm-connected clients can

reach the defined port

17

. By comparing these two setups, you can choose the appropriate balance of

security and convenience for hosting your LiteLLM server.

1

2

Domains - Pangolin Docs

https://docs.pangolin.net/manage/domains

3

DNS & Networking - Pangolin Docs

https://docs.pangolin.net/self-host/dns-and-networking

4

5

7

18

Pangolin (CE) | DigitalOcean Documentation

https://docs.digitalocean.com/products/marketplace/catalog/pangolin-ce/

6

Raw TCP & UDP - Pangolin Docs

https://docs.pangolin.net/manage/resources/tcp-udp-resources

8

Rules - Pangolin Docs

https://docs.pangolin.net/manage/access-control/rules

9

10

11

12

16

17

19

Client Resources - Pangolin Docs

https://docs.pangolin.net/manage/resources/client-resources

13

14

15

Configure Client - Pangolin Docs

https://docs.pangolin.net/manage/clients/configure-client

4

