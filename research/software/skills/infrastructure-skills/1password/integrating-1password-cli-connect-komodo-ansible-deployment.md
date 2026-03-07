Integrating 1Password CLI and Connect in a

Komodo Ansible Deployment

Overview

When deploying a Komodo Periphery agent and integrating it with Pangolin’s tunneling (Newt) and

client access (Olm), you need to supply sensitive credentials (client IDs and secrets) for those services.

Instead of hard-coding these secrets or storing them in plain text, you can retrieve them securely from
1Password. This guide compares two approaches – using the 1Password CLI ( op ) vs. using a self-

hosted 1Password Connect API – and shows how to integrate either into your Ansible playbook that
wraps the  bpbradley.komodo  role. We’ll also enable conditional toggling so that you can choose per-

secret whether to fetch it from 1Password or use an alternative (such as an Ansible Vault). The solution

includes   example   playbook   snippets   for   retrieving   secrets   and   injecting   them   into   Komodo’s

configuration (e.g. systemd units or config files) in a secure manner.

Approach 1: Using 1Password CLI ( op ) for Secret Retrieval

Installing and Preparing the CLI on Targets

Since the Komodo Ansible role runs the periphery agent directly on the host (as a systemd service, not

in a container), the 1Password CLI should be installed on that  host  OS if you plan to use it for secret
. The role doesn’t install  op  for you (the author expects external secret management), but

retrieval

1

you can extend your playbook with a pre-task to install the CLI before running the Komodo role

2

. For

example, on Debian/Ubuntu targets you might add:

pre_tasks:

- name: Install 1Password CLI on host

apt:

pkg: 1password-cli

state: present

become: yes

(Add   the   1Password   APT   repo   and   GPG   key   as   needed   before   the   install,   as   shown   in   1Password’s
documentation.) By doing this in a  pre_tasks  section, you ensure  op  is available on the host before

. If you are running Ansible from a control machine (or using an Execution
the Komodo role executes
Environment container), you may also install  op  there if you plan to use Ansible lookup plugins locally

3

to fetch secrets

4

.

Additionally, make sure the CLI is authenticated to your 1Password vault. In non-interactive automation,

the best practice is to use a 1Password Service Account and set its token as an environment variable

(OP_SERVICE_ACCOUNT_TOKEN) so that the CLI can authenticate without manual login

5

. This avoids

prompting for master password, and it’s more suitable for CI/CD or headless servers.

1

Retrieving Secrets with the CLI in Ansible

With   op   installed   and   authenticated,   you   can   fetch   secrets   within   your   playbook.   There   are   two

common methods:

•

Using the  op  CLI via shell/command tasks: You can run commands like  op item get  or
op read  to fetch a secret field. For example:

- name: Fetch Newt agent Client ID from 1Password (CLI)

command: >

op item get "Pangolin Newt Site Credentials" --field label="Client ID"

register: newt_client_id_raw

when: use_1password_for_newt

no_log: true

This uses the  op  CLI to get a field named “Client ID” from an item (entry) called “Pangolin Newt Site
Credentials”. We register the output to a variable. We mark the task with   no_log: true   to avoid

printing the sensitive value in Ansible logs
. You would do a similar task for the “Client Secret” field
(e.g.,   --field label="Secret" ), registering it as   newt_client_secret_raw . Later, you can set

6

Ansible facts or use the values directly when templating configs.

•

Using the 1Password lookup plugin: Ansible’s community.general.onepassword lookup can

fetch a field from 1Password directly. This plugin can work with an existing CLI session or a
service account token. For example, once logged in (or with  OP_SERVICE_ACCOUNT_TOKEN

set), you could:

- name: Lookup Olm client secret from 1Password

set_fact:

olm_client_secret: "{{ lookup('community.general.onepassword', 'Olm VPN

Client', field='Secret', vault='Networking') }}"

when: use_1password_for_olm

no_log: true

This would find the item titled “Olm VPN Client” in the “Networking” vault and retrieve the field named

“Secret”. The plugin handles using the CLI or Connect API under the hood, depending on how you
configure   it   (it   supports   connect_host / connect_token   as   well).   Ensure   the   necessary

environment variables (like OP_SERVICE_ACCOUNT_TOKEN or an active session) are in place for this task

to succeed

7

5

.

In either case, perform these retrieval tasks conditionally using the toggles. In the snippets above, we
use   when:   use_1password_for_newt   or   when:   use_1password_for_olm   to   ensure   we   only

attempt the 1Password lookup if the user enabled it for that secret. If the flag is false, you would skip

these tasks and instead rely on a value provided through another means (for example, an Ansible Vault

encrypted variable for the secret).

Security tip:  Always use   no_log: true   on tasks that handle secret material to prevent accidental

6

.   Also   avoid   printing   the   variable   or   including   it   in   debug   output.   Ansible   facts   (from
exposure
set_fact ) are stored in memory; avoid persisting them to disk (fact cache) when they include secrets

8

.

2

Injecting Secrets into Configuration (Using CLI Approach)

Once you have the secret values (either via CLI command output or lookup), you need to inject them
into the Komodo periphery’s configuration or environment. The   bpbradley.komodo   role provides a
convenient mechanism:  komodo_agent_secrets . This variable accepts a list of name/value pairs that

will be made available only to the Komodo agent on that host

9

. For example, you can set in your

playbook:

komodo_agent_secrets:

- name: "NEWT_ID"

value: "{{ newt_client_id_raw.stdout }}"

- name: "NEWT_SECRET"

value: "{{ newt_client_secret_raw.stdout }}"

- name: "OLM_ID"

value: "{{ olm_client_id }}"

# assume set_fact from lookup

- name: "OLM_SECRET"

value: "{{ olm_client_secret }}"

Each   entry’s   name   will   become   an   environment   variable   for   the   periphery   service,   with   the   given
value . By passing these into the role, the role will ensure they are configured in the systemd service

or a dedicated secrets file for the agent. According to the Komodo role docs, secrets defined this way

are  bound directly to the periphery agent  and not exposed to Komodo Core

9

. In fact, Komodo

supports mounting such secrets on the agent so that, for example, a Pangolin token never leaves the

host and is never sent over the network to the core

10

. This means your Newt and Olm credentials will

reside only on the target machine (as needed for Newt/Olm to function) and are not visible elsewhere,

which is a security advantage

11

10

.

Alternatively, you can inject the secrets by customizing the systemd unit or config file:

•

Systemd unit approach: Provide a drop-in or override file that adds environment variables. For

instance, you might supply a custom unit template via the role’s
komodo_service_file_template  variable if you want full control. In that template, include

lines like:

[Service]

Environment="PANGOLIN_NEWT_ID={{ newt_client_id_raw.stdout }}"
Environment="PANGOLIN_NEWT_SECRET={{ newt_client_secret_raw.stdout }}"

Ensure the file is only readable by root/komodo user. The role will prefer your custom service file if

specified

12

. This approach is useful if the Newt agent is launched as part of the Komodo service or a

related systemd unit.

•

Config file approach: Some use-cases might have the Newt or Olm processes read from a

config file (e.g., a JSON file for Olm or environment file for Newt). You can use an Ansible

template to create such a file on the host, containing the retrieved ID/secret. For example, create
newt-config.env.j2  with contents:

3

NEWT_ID={{ newt_client_id_raw.stdout }}

NEWT_SECRET={{ newt_client_secret_raw.stdout }}

Then use  ansible.builtin.copy  or  template  module to place it at the appropriate location (e.g.,
/etc/komodo/newt.env ) with mode  0600 . Your systemd service for Newt (if separate) could then
EnvironmentFile=/etc/komodo/newt.env   to load these on startup. This is an alternative if you
don’t use  komodo_agent_secrets .

Important:  If   you   installed   op   on   the   host   and   are   calling   it   during   deployment   (as   in   the   first

example), note that those tasks run on the target hosts by default. The host will need network access to

1Password (unless using offline vault cache) to retrieve the secrets at playbook run time. Make sure the
environment is configured so   op   can authenticate (service account token is set, etc.). If direct host
access is an issue, you could instead run the lookup on the control node ( delegate_to: localhost )

and   then   distribute   the   secret   to   the   host   via   variables   or   files.   Adjust   the   strategy   based   on   your

security and connectivity constraints.

Approach 2: Using 1Password Connect API for Secret Retrieval

Setting Up 1Password Connect for Ansible

1Password Connect is a self-hosted service (typically a Docker container) that provides an API to access
your 1Password vaults. To use this in Ansible, you’ll first need to deploy a Connect server and obtain a

credentials   file   or   token   for   it

.   In   Ansible,   the   integration   is   done   through   the
.   Install   this   collection   (e.g.   via   ansible-galaxy
onepassword.connect   Ansible   Collection
collection   install   onepassword.connect )   on   your   control   machine   or   include   it   in   your
playbook’s  collections  list.

14

13

Make sure you have the Connect server’s URL/hostname and an access token ready. The collection’s

modules will need these for each task. You can pass them as parameters or set environment variables
( OP_CONNECT_HOST   and   OP_CONNECT_TOKEN ) in your playbook context

. For security, avoid

15

16

putting   the   token   in   plaintext   in   the   playbook;   instead,   consider   storing   it   in   an   Ansible   Vault   or

supplying it as an extra-var at runtime. It’s recommended to use a variable for the token rather than an

env var, because Ansible environment vars could be exposed in process lists – using a task variable (or

Vault) is more secure

17

18

.

For example, you might have:

vars:

op_connect_host: "http://1password-connect.internal:8080"

op_connect_token: "{{ vault_op_connect_token }}"

# fetched from an

encrypted vault or inventory

then   pass

  hostname:   "{{   op_connect_host   }}"
And
"{{  op_connect_token  }}"   to  the  module  calls.  Always  set   no_log:  true   on  tasks  with  the

  token:

and

token to avoid printing it.

4

Retrieving Secrets with 1Password Connect Modules

The 1Password Connect collection provides modules to fetch data from the vault. Two useful ones are:
onepassword.connect.item_info
onepassword.connect.field_info  (to fetch a specific field’s value from an item)

(to   retrieve   an   item’s   details   by   name   or   UUID)   and

.

19

•

Using  item_info : This module will return the item object (including all fields). You can then

pick out the needed fields. For example:

- name: Fetch Newt credentials item from 1Password (Connect)

collections: [ onepassword.connect ]

onepassword.connect.item_info:

hostname: "{{ op_connect_host }}"

token: "{{ op_connect_token }}"

vault: "Infrastructure"

item: "Pangolin Newt Site"

# Item name in 1Password vault

register: newt_item

no_log: true

when: use_1password_for_newt

This will find the item named “Pangolin Newt Site” in the “Infrastructure” vault (via the Connect API)
.
We register it as  newt_item . The result ( newt_item ) will contain fields (e.g., perhaps a field called

20

“Client ID” and one called “Secret”). You can then extract them:

- set_fact:

newt_client_id: "{{ newt_item.item.fields |

selectattr('label','equalto','Client ID') | first | map(attribute='value') |

first }}"

newt_client_secret: "{{ newt_item.item.fields |

selectattr('label','equalto','Secret') | first | map(attribute='value') |

first }}"

when: use_1password_for_newt

no_log: true

(The exact JSON structure depends on 1Password’s API; adjust the selection of the field value as needed.

The idea is to pull the field values out and store in simple variables.)

•

Using  field_info : This module can retrieve a single field directly, which can be more

convenient. For example, to get the Olm client secret in one go:

- name: Get Olm client secret from 1Password (Connect)

collections: [ onepassword.connect ]

onepassword.connect.field_info:

hostname: "{{ op_connect_host }}"

token: "{{ op_connect_token }}"

vault: "Networking"

item: "Olm VPN Client"

field: "Secret"

5

register: olm_secret_field

no_log: true

when: use_1password_for_olm

This finds the “Olm VPN Client” item and returns the value of the field named “Secret”

19

. The module

will   search   for   the   item   by   title   (or   UUID)   and   then   grab   the   specified   field’s   value.   After   this,
olm_secret_field.field.value   would contain the secret (you can assign that to a var or use it
directly). Similarly, you could fetch the “Client ID” field by setting   field: "Client ID"   in another
task. Using multiple   field_info   tasks avoids dealing with JSON in Ansible, at the cost of a couple

extra API calls – which is usually fine.

Just   like   with   the   CLI,   use   the   when   conditions   tied   to   use_1password_for_newt   or
use_1password_for_olm  so that these tasks run only when needed. If a particular secret is not to be

retrieved from 1Password (toggle is false), you would skip these and instead use whatever value is

provided in inventory or elsewhere.

Note: The Connect modules by default run on the targeted host in Ansible’s context. In many cases, you

may want these to run on the control node (which can reach the Connect API) and then distribute the
secret to targets. You can achieve this by using  delegate_to: localhost  on the tasks, or by making

the   play   run   on   localhost   for   the   secret   retrieval   portion.   The   example   above   assumes   the   play   is

running   on   the   target   host   and   that   host   can   reach   the   Connect   server   –   adjust   to   your   topology.

Keeping secret retrieval centralized (on the Ansible controller) can be safer, so you don’t have to pass

the Connect token to remote hosts

17

.

Injecting Secrets into Komodo/Pangolin Configuration (Connect Approach)

After retrieving the secrets via the Connect API, the integration with the Komodo role is similar to the
CLI  approach.  You  will  end  up  with  variables  like   newt_client_id ,   newt_client_secret ,  etc.,

either as registered outputs or set_facts. You can then:

•

Use  komodo_agent_secrets : Even when using Connect, you populate
komodo_agent_secrets  in the playbook with the retrieved values, just as shown earlier. The

role will handle updating the systemd service environment or config with these secrets
example, if you got  newt_client_id  from  field_info , set:

21

. For

komodo_agent_secrets:

- name: "NEWT_ID"

value: "{{ newt_client_id }}"

- name: "NEWT_SECRET"

value: "{{ newt_client_secret }}"

(and  similarly  for  Olm  if  needed).  These  will  be  available  to  the  periphery  service.  Using  periphery-

bound  secrets  means  Pangolin  credentials  stay  on  that  host  only,  enhancing  security  (the  Pangolin

token/ID never travels through Komodo Core)

10

.

•

Or deploy config files: If not using the role’s built-in mechanism, you can template the

credentials into configuration files as described before (the same strategies apply: environment

file or JSON/YAML config consumed by Newt/Olm). The difference with Connect is just where the

data came from – the usage in templates or systemd units is identical.

6

In all cases, ensure the files or variables containing these secrets are protected. For instance, if you use
a   template   to   drop   a   JSON   config   for   Olm   (which   might   look   like   {   "id":   "{{   olm_id   }}",
"secret":   "{{   olm_secret   }}"   } ),   place   it   in   a   secure   location   ( ~/.config/olm-client/
config.json   with proper permissions, if that’s what Olm expects) and  do not print its content in
logs. Mark the template task with  no_log: true  as well, since the content contains secrets.

Toggling Secret Retrieval with Ansible Variables

We’ve   referenced   variables   like   use_1password_for_newt   and   use_1password_for_olm
throughout – these are boolean flags you can define (e.g., in your playbook  vars  or inventory group

vars) to control whether each credential should be pulled from 1Password. This design allows flexibility:

for example, you might choose to manage Olm client credentials differently while pulling the Newt

agent credentials from 1Password.

Here’s how you could structure the playbook logic using these toggles:

- name: Deploy Komodo Periphery with Pangolin integration

hosts: komodo_hosts

vars:

use_1password_for_newt: true

use_1password_for_olm: false

pre_tasks:

- name: Install 1Password CLI on hosts (if needed for CLI method)

apt:

name: 1password-cli

state: present

when: use_1password_for_newt or use_1password_for_olm

become: yes

- name: Retrieve Pangolin Newt credentials from 1Password

command: >

op read "op://Infrastructure/Pangolin Newt Site/Client ID"

register: newt_id_raw

when: use_1password_for_newt

no_log: true

- name: Retrieve Pangolin Newt secret from 1Password

command: >

op read "op://Infrastructure/Pangolin Newt Site/Secret"

register: newt_secret_raw

when: use_1password_for_newt

no_log: true

- name: Retrieve Olm client credentials from 1Password Connect

onepassword.connect.item_info:

hostname: "{{ op_connect_host }}"

token: "{{ op_connect_token }}"

vault: "Networking"

item: "Olm VPN Client"

register: olm_item

7

when: use_1password_for_olm

no_log: true

- name: Set Olm client vars (if pulled from 1Password)

set_fact:

olm_client_id: "{{ olm_item.item.fields |

selectattr('label','equalto','Client ID') | first | map(attribute='value') |

first }}"

olm_client_secret: "{{ olm_item.item.fields |

selectattr('label','equalto','Secret') | first | map(attribute='value') |

first }}"

when: use_1password_for_olm

no_log: true

roles:

- role: bpbradley.komodo

komodo_action: "install"

komodo_version: "latest"

komodo_agent_secrets: |

{% set secrets_list = [] %}

{% if use_1password_for_newt %}

{{ secrets_list.append({'name': 'NEWT_ID', 'value':

newt_id_raw.stdout.strip()}) }}

{{ secrets_list.append({'name': 'NEWT_SECRET', 'value':

newt_secret_raw.stdout.strip()}) }}

{% else %}

{{ secrets_list.append({'name': 'NEWT_ID', 'value':

provided_newt_id}) }}

{{ secrets_list.append({'name': 'NEWT_SECRET', 'value':

provided_newt_secret}) }}

{% endif %}

{% if use_1password_for_olm %}

{{ secrets_list.append({'name': 'OLM_ID', 'value': olm_client_id}) }}

{{ secrets_list.append({'name': 'OLM_SECRET', 'value':

olm_client_secret}) }}

{% else %}

{{ secrets_list.append({'name': 'OLM_ID', 'value':

provided_olm_id}) }}

{{ secrets_list.append({'name': 'OLM_SECRET', 'value':

provided_olm_secret}) }}

{% endif %}

{{ secrets_list }}

In the above pseudo-playbook, we:

•

Install the CLI if either secret needs it (you might refine this to only install if you’re using the CLI

method – e.g., wrap in another condition or separate tasks for CLI vs Connect usage). In a real

scenario, if you choose Connect for both secrets, you wouldn’t need the CLI at all on the hosts.

•

Fetch Newt credentials with the CLI (two commands for ID and Secret) when
use_1password_for_newt=true .

8

•

•

Fetch the Olm credentials via Connect (one task to get the item, then parsing it) when
use_1password_for_olm=true .
In the role invocation, we construct  komodo_agent_secrets  by combining either the fetched
secrets or some  provided_...  variables for each case. This allows the role to always receive
the necessary values in a unified way. (You could also set  komodo_agent_secrets  outside of

the role call, e.g., in a task right before invoking the role, to keep it cleaner.)

The   conditional   inclusion   could   also   be   done   with   include_tasks   files   for   clarity.   For   example,
include_tasks:   get_newt_1password.yml   when:   use_1password_for_newt   and   that   file
contains the  op  commands or Connect calls for Newt. This modularizes the logic.

By   toggling   the   booleans,   you   activate   or   deactivate   the   1Password   integration   for   each   secret

independently.   This   design   is   very   useful   for   testing   and   gradual   adoption   –   e.g.,   you   can   switch
use_1password_for_newt   to true once you’ve stored Newt creds in 1Password, while still keeping
use_1password_for_olm  false if you haven’t moved Olm creds to 1Password yet.

Pros and Cons: 1Password CLI vs 1Password Connect

When  choosing  between  the  CLI  and  Connect  approaches,  consider  the  following  in  the  context  of
Ansible automation, security, and your environment:

•

1Password CLI ( op ) Approach:

•

Pros:

◦

Simple deployment: just install a binary on each host (or on the control node). No

additional infrastructure needed besides the 1Password account itself
If you’re already using  op  in workflows, it’s straightforward to integrate (lots of

.

22

◦

community examples, plus Ansible’s lookup plugin supports it).

◦

Can work in offline scenarios where a cached session or vault exists on the machine

(though generally internet is needed to fetch the latest secrets unless cached).

◦

Supports new Service Accounts for headless operation – you can export an

OP_SERVICE_ACCOUNT_TOKEN to allow CLI access without user interaction

5

. This is

great for CI/CD.

◦

The CLI runs on the target host, so secrets can be fetched at the moment they’re needed

on that host. This could be useful if, for instance, the application (Komodo or a script on
the host) might use  op  at runtime to fetch rotating secrets (not the case here, but a
consideration for some workflows). Having  op  installed means you have that capability

on the host if needed

23

.

•

Cons:

◦

Requires installing and updating the CLI on every target host (or container) where it’s

used. This adds maintenance overhead and an extra dependency on the host OS

1

. (If

your Ansible runs on many hosts, that’s many installations; however, you could mitigate
by baking  op  into your base AMI or using an Ansible role to install it.)

◦

Authentication needs to be handled per host or per run. With multiple hosts, you might
need to authenticate  op  on each (Service Accounts help by using a token). If not using a

service token, managing 1Password sign-ins (with password, secret key) in automation is

◦

tricky and not recommended.
The CLI output needs careful handling. You must use  no_log  and avoid stdout printing
of secrets. Also be aware that if you pass secrets via command-line (e.g.,  op read

9

"op://vault/item/secret" ), the secret itself stays in output, but CLI avoids showing

it unless captured. Just ensure Ansible registers it and doesn’t log it.

◦

In some minimal environments (Alpine, etc.), installing the official CLI might require extra

steps (there are official packages for many distros, but not all). Ensure compatibility with

your host OS.

◦

If the Ansible control node is separate and you want to use lookups there, you need the

CLI and a login on the control machine as well. That can be a pro or con depending on

perspective (centralized secret pull vs. distributed).

•

1Password Connect Approach:

•

Pros:

◦

No need to install anything on target hosts (agentless secret retrieval). The integration

communicates with the Connect API over HTTP(S), which means even network devices or

appliances that can run Ansible Python code could fetch secrets without a local binary.

This is ideal for varied environments (Linux, Windows, etc. all just use HTTP via the

module)

14

.

◦

Designed for automation and high-scale scenarios. The Connect server is stateless and

uses a long-lived access token – perfect for CI systems and multi-host deployments. You

don’t deal with individual logins or sessions for each host; just ensure each Ansible run

knows the token

17

.

◦

Fine-grained control: You can limit the vaults and permissions that the Connect token has.

For example, create a special vault for Komodo/Pangolin secrets and a token that only

has access to that vault. This way, even if the token is compromised, blast radius is

limited. With CLI (unless using a separate account/vault), the machine might have access

to more than necessary.
Ansible modules ( item_info ,  field_info , etc.) simplify retrieving exactly what you

◦

need (e.g., a single field) in a structured way. No need to parse CLI text output. The

modules return Ansible-friendly JSON. This reduces chances of error and makes

playbooks cleaner.

◦

The Connect server caches vault data and is typically on the same network as your

deployments (if self-hosted), potentially making secret retrieval faster and more reliable

for large deployments (no repeated calls to 1Password cloud from each host; Connect

might serve from cache).

•

Cons:

◦

Requires running the 1Password Connect service. This means extra setup: you have to

deploy and maintain a Docker container (or Kubernetes pod, etc.) in your environment,

and ensure it can reach 1Password (for syncing) and your Ansible controller/targets can

reach it. This is additional infrastructure that not everyone may want to manage.

◦

The Connect access token is powerful – it grants access to secrets. You must store this

token securely (Ansible Vault is a good option). If using AWX/Controller, handle it as a

credential. Improper handling of the token could be a security risk (whereas with CLI,

you’d be using individual logins or service accounts – different risk profile).

◦

Slightly higher initial complexity in playbooks: you have to install the collection and be
mindful of adding  hostname  and  token  for each module call (or set env vars

carefully). This is a minor overhead, but the playbook snippets are a bit more verbose

than the CLI shell calls.

◦

If your Ansible workflow runs in a constrained environment (say, GitHub Actions runner

or an ephemeral VM), deploying a Connect container just for it may not be feasible. In

such cases, CLI (with a service account token) might be easier to use directly. Connect

10

shines in long-lived infrastructure (e.g., a cooperate Jenkins/Ansible Tower with a

permanent Connect instance).

◦

Host environment compatibility: Generally, Connect is quite OS-agnostic (pure HTTP

calls). One consideration: if your target hosts cannot reach the Connect server due to

network segmentation, you’ll need to delegate those tasks to a node that can (like the

controller). This is manageable but adds complexity in playbook design (using
delegate_to ). With CLI, as long as the host can reach 1Password cloud (HTTPS) it can

fetch secrets on its own.

•

Security Considerations: Both methods keep secrets out of your Git repo and playbook in plain

form, which is good. With either, you should still encrypt any meta-credentials (like the Vault’s

master   password,   the   1Password   service   account’s   secret   key,   or   the   Connect   token)   using

Ansible Vault or a secrets manager. In terms of auditing: 1Password will log access to items

either way, but Connect can give you an audit trail centralized on the server (and you can restrict

what vaults a token sees). Using the CLI with a service account is similar in that the service

account’s usage can be monitored in 1Password. One advantage of Connect is that you can run it

within your network, so secrets are fetched locally (1Password<->Connect sync is encrypted and

you control the endpoint). CLI will hit 1Password’s API directly from each host. Depending on

your compliance needs, one or the other might be preferred.

In summary, the CLI approach is simpler if you’re dealing with just a few hosts and you can install the

tool on them, and you want to avoid managing an extra service. The Connect approach is more robust

for larger environments or where installing software on targets is undesirable – it centralizes secret

distribution   through   a   service.   Many   users   start   with   the   CLI   (especially   for   small-scale   or   initial

integration) and later move to Connect as they automate more secrets.

Integrating with the Ansible Playbook (Wrapping
bpbradley.komodo  Role)

Whichever approach you choose, the key is that your playbook should fetch the needed secrets before

or while configuring the Komodo Periphery service, and inject them in a way that the service (or related
Pangolin agent) can use. The typical pattern is: use   pre_tasks   in the play to handle all 1Password

interactions,  then  call  the  Komodo  role.  This  ensures  that  by  the  time  the  role  runs  and  starts  the
service, the necessary credentials are already in place (e.g., provided via  komodo_agent_secrets  or a
config   file).   This   is   illustrated   in   the   earlier   examples,   where   we   put   installation   of   op   and   secret
lookups in  pre_tasks

.

3

An alternative structure could be to include an external task file or role for secrets. For example, you
could have a role  fetch_pangolin_creds  that encapsulates the 1Password setup and lookup tasks,

controlled by the same variables. Your playbook would then simply do:

roles:

- fetch_pangolin_creds

- bpbradley.komodo

And inside   fetch_pangolin_creds   role, use tasks with   when: use_1password_for_newt   etc.
This keeps the main playbook cleaner. Just be cautious to properly set fact scope (use   set_fact   or
register  such that the secrets are available to the subsequent roles). Since  bpbradley.komodo  is

11

expecting   komodo_agent_secrets   in the inventory/vars, you can have   fetch_pangolin_creds

set that variable (as a host fact) if you prefer that encapsulation.

After the role runs, Komodo Periphery will be installed and running. The Pangolin Newt agent (if you

installed it or it’s part of the periphery service startup) will have the credentials injected and should

connect   to   the   Pangolin   server   using   them.   Similarly,   if   Olm   credentials   were   placed   on   the   host

(perhaps to generate an Olm config or to display to a user), that would be handled. Make sure to verify

that the secrets indeed made it into the expected location on the host. For example, you might log in to
the host and check the systemd service environment ( systemctl show periphery  to see if env vars

are present but masked – systemd will usually show “[service has Environment=X]” but not the value,

which is good). Or if using a config file, ensure it exists with correct contents and permissions.

Cleanup and Handling Changes: If a secret in 1Password gets rotated, your next Ansible run will pull

the new value and update the host’s config (the Komodo role will likely restart the service if the systemd
unit or config file changed). Using   komodo_agent_secrets   in the role, any change in the values

should trigger the role to update the agent’s environment file and restart the service to pick up new env

vars. This means your deployment is always using the latest secrets from the vault without manual

intervention – a big win for security (no forgetting to update a static file). Just remember that if you

disable the 1Password integration flags, you should have an alternate source for the credential ready;

otherwise, the playbook would skip fetching and not provide the secret at all (which could cause the

Komodo service or Newt agent to fail due to missing creds). Typically, you’d default the toggle to true

once 1Password is set up, and only set it false if you intentionally want to fall back to manual secrets

(e.g., in a dev environment).

Finally, throughout your playbook, we reiterate: use   no_log  on any tasks or modules that deal with

.   Ansible   will   then
secret   material   (including   the   1Password   token,   the   raw   secret   values,   etc.)
suppress those in output. If you need to debug, you can temporarily turn off   no_log   to see what’s

6

going on, but be cautious not to expose real secrets during debugging. Remove any debug or print

statements before running against production.

Conclusion

Integrating   1Password   into   your   Komodo   +   Pangolin   deployment   workflow   greatly   improves   secret

management by removing hard-coded credentials. We showed two methods – the 1Password CLI and

the 1Password Connect API – each with its own setup and usage in Ansible. Both approaches achieve

the goal: securely retrieving Pangolin Newt agent and Olm client credentials at deploy time and

injecting   them   into   the   Komodo   periphery   configuration.   By   using   Ansible   variables   like
use_1password_for_newt/olm ,   you   gain   fine-grained   control   over   which   secrets   come   from

1Password, enabling a smooth transition and optional use per environment or secret.

In practice, if you prefer minimal dependencies and have a smaller setup, you might start with the CLI

method (perhaps using a 1Password service account for non-interactive auth). If you require a more

centralized solution or are managing many hosts, setting up 1Password Connect is worth the effort for

a robust secrets pipeline. In either case, the Ansible integration points remain similar – fetch in pre-
tasks,   use   komodo_agent_secrets   or   templates   to   provide   the   creds,   and   let   the   Komodo   role

handle the rest. By following these practices, your Komodo periphery deployment will automatically

include the Pangolin integration secrets without exposing them, aligning with zero-trust principles and

infrastructure-as-code best practices.

12

Sources:

•

Komodo Ansible role design and extension points

2

3

24

•

1Password CLI usage in automation

5

4

•

1Password Connect Ansible integration

14

20

•

Komodo and Pangolin security considerations (periphery-bound secrets)

11

10

1

2

3

4

22

23

24

Integrating 1Password CLI into Komodo Ansible Deployment.pdf

file://file_00000000f938720ab9d6df0f291553cb

5

7

8

community.general.onepassword lookup – Fetch field values from 1Password — Ansible

Community Documentation

https://docs.ansible.com/ansible/latest/collections/community/general/onepassword_lookup.html

6

13

14

15

16

17

18

19

Use Connect with Ansible | 1Password Developer

https://developer.1password.com/docs/connect/ansible-collection/

9

12

21

README.md

https://github.com/basher83/hello-komodo/blob/6eb2fd29447cbae2ba8573cd338f5bd5149d561c/ansible/roles/

bpbradley.komodo/README.md

10

11

Comparing Approaches for Pangolin Registration after Komodo Deployment.pdf

file://file_00000000a2d4720abeefda968109e65d

20

GitHub - 1Password/ansible-onepasswordconnect-collection: The 1Password Connect collection

contains modules that interact with your 1Password Connect deployment. The modules communicate

with the 1Password Connect API to support Vault Item create/read/update/delete operations.

https://github.com/1Password/ansible-onepasswordconnect-collection

13

