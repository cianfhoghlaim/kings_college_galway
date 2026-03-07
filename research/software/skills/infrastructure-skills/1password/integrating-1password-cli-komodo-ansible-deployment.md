Integrating 1Password CLI into Komodo Ansible

Deployment

Host vs. Container Installation for  op

The Komodo Ansible role deploys the periphery agent directly on the host system, running it as a
systemd   service   under   a   restricted   komodo   user

.   In   other   words,   this   role   does  not  run   the

1

periphery inside a Docker container – it installs the binary on the host and manages it via systemd

2

1

.   Because   of   this   design,   the   1Password   CLI   ( op )   should   be   installed   on   the  host   system

(periphery node) rather than inside a container. The role does not provide any built-in mechanism for

customizing or extending the contents of a container image (since no container is used for periphery by
default), so container-level customization isn’t applicable here. Instead, any extra tools like  op  must be

provisioned on the host.

Why host installation? – Installing  op  on the periphery host means it’s available to assist

with   secrets   or   configuration   at   deployment   time   (or   runtime)   on   that   machine.   In

contrast, installing it “inside” a Komodo periphery container would only make sense if you

were running periphery as a container. Since this role avoids that (to eliminate Docker-

related edge cases

1

), the correct approach is to put 1Password CLI on the host OS.

This aligns with best practices: use Ansible to provision any needed tools on the target

servers.

Injecting Custom Tasks to Provision  op

The Ansible role itself focuses on Komodo setup and doesn’t natively install 1Password CLI (the author

mentions   having   separate   tooling   for   1Password   secrets   management   outside   the   core   role
).
However, you can easily extend the deployment playbook by adding your own task to install  op  on

3

each periphery node. Ansible allows you to combine roles with custom tasks in a playbook. For example,

you   can   use  pre-tasks  or   an   additional   role   to   install   the   1Password   CLI  before  running   the
bpbradley.komodo   role.   This   ensures   that   op   is   available   on   the   host   during   or   after   Komodo

installation, if needed for retrieving secrets.

In the provided examples, the Komodo role is invoked in a playbook like so:

- name: Manage Komodo Service

hosts: komodo

roles:

- role: bpbradley.komodo

komodo_action: "install"

komodo_version: "latest"

komodo_passkeys:

- !vault |

1

$ANSIBLE_VAULT;1.1;AES256

... (encrypted passkey) ...

4

. We can inject our custom installation task for  op  in this same playbook. The typical pattern is to
add   a   task  before  the   role   runs   (so   that   the   CLI   is   installed   early).   Using   a   pre_tasks   section   is

convenient, but you could also list the task normally above the role in the play. For example:

Ansible Task Snippet – Installing 1Password CLI via APT

Below  is  a  YAML  snippet  that  installs  the   1Password   CLI  on   Debian/Ubuntu   hosts  using   the   official
1Password APT repository. This can be added as a task in your playbook (with  become: yes  to gain

root privileges on the remote host):

- name: Add 1Password apt signing key

ansible.builtin.apt_key:

url: https://downloads.1password.com/linux/keys/1password.asc

state: present

become: yes

- name: Add 1Password apt repository

ansible.builtin.apt_repository:

repo: "deb [arch=amd64] https://downloads.1password.com/linux/debian/

amd64 stable main"

filename: "1password"

state: present

update_cache: yes

# update apt index after adding repo

become: yes

- name: Install 1Password CLI

ansible.builtin.apt:

name: 1password-cli

state: present

update_cache: yes

# ensure latest package list

become: yes

This   sequence   performs   the   following:   1.  Adds   the   1Password   GPG   key  to   apt   (allowing   package

signature verification). 2. Adds the 1Password APT repository for the CLI to the system’s sources list. 3.
Updates apt cache and installs the  1password-cli  package.

Using the apt package ensures that the CLI can be kept up-to-date easily (and it’s the official installation

method   provided   by   1Password).   Adjust   the   tasks   as   needed   for   your   distribution   (for   RHEL-based
systems you’d use  dnf/yum  repository tasks instead, but Komodo typically targets Debian/Ubuntu in

these examples).

2

Where to Place the Installation Task in the Playbook

You   should   insert   the   above   task(s)   in   the   playbook  before   the   Komodo   role   is   executed  on   the

periphery hosts. In practice, this means either:

•

Defining the snippet under a  pre_tasks:  section in the Komodo deployment play (ensuring it

runs first), or

•

Listing the installation task in the playbook file just before the roles section.

For example, integrating with the earlier playbook snippet:

- name: Manage Komodo Service

hosts: komodo

become: yes

pre_tasks:

# Install 1Password CLI on the host before setting up Komodo

- name: Add 1Password apt signing key

apt_key:

url: https://downloads.1password.com/linux/keys/1password.asc

state: present

- name: Add 1Password apt repository

apt_repository:

repo: "deb [arch=amd64] https://downloads.1password.com/linux/debian/

amd64 stable main"

filename: "1password"

state: present

update_cache: yes

- name: Install 1Password CLI

apt:

name: 1password-cli

state: present

update_cache: yes

roles:

- role: bpbradley.komodo

komodo_action: "install"

...

This way, by the time the Komodo role runs its tasks on each host, the   op   binary will already be

present on that host’s filesystem.

Compatibility with Example Scenarios

Both   provided   examples   (the  “server_management”  playbook   and   the  “komodo_automation”

containerized Ansible setup) can accommodate this addition:

•

Server   Management   example:  This   likely   uses   a   straightforward   playbook   and   inventory   to

configure multiple periphery servers with unique settings. You can safely add the 1Password CLI

3

installation tasks to that playbook. Since the role already assumes a Debian/Ubuntu environment
for the periphery nodes, installing  1password-cli  via apt will fit right in without issue.

•

Komodo   Automation   example:  This   scenario   uses   “ansible-in-docker”   (an   Ansible   Execution

.
Environment container that includes the Komodo role) to automate periphery deployments
Here, you should still treat the   op   install as a step for the  target hosts. Even though Ansible

5

itself is running from within a container, it will SSH into the periphery nodes and execute the apt

tasks normally. Just include the above snippet in the automation playbook that runs inside the

container.   (If   for   some   reason   internet   access   to   the   1Password   repo   is   restricted   in   your
environment, you might instead bake the  1password-cli  into a custom OS image or use an

offline package – but in typical cases, the apt approach is fine.)

Placement recommendation: In both examples, the best practice is to run the  op  installation before

configuring   Komodo.   This   ensures   that   if   Komodo’s   configuration   or   startup   needs   any   secrets
(passkeys, tokens, etc.), you have the 1Password CLI available to fetch them. If you plan to use   op

within Ansible to look up secrets (for example, using Ansible’s local shell or a lookup plugin to query
1Password),   you   might   also   install   op   on   the   Ansible   control   machine   or   execution   environment.
However,   for   storing   secrets   on   the   periphery   host   itself   (via   komodo_agent_secrets   or   other
configs), having  op  on that host could allow runtime retrieval of secrets if ever needed.

Summary & Best Practices

•

Install  op  on the periphery host (not in a container) because the Komodo role runs on the

host’s OS

1

. The role doesn’t support injecting software into a container image – it treats the

host as the deployment target.

•

Use Ansible to provision 1Password CLI by adding tasks to your playbook. This can be done via
pre_tasks  or an external role, ensuring the CLI is present on each node prior to running

Komodo setup. The role is flexible – you can combine it with custom steps in your automation

workflow (the role’s author even uses separate playbooks for 1Password integration

3

).

•

YAML Task Snippet: (provided above) Use the official 1Password apt repository to install
1password-cli  on Debian/Ubuntu hosts. This approach is idempotent and aligns with Ansible

best practices (no manual download steps, just package management).

•

Integration point: Place the installation tasks early in the deployment process. In a single
play, put them before the  bpbradley.komodo  role is invoked. In a larger automation (like the

Docker-based ansible runner), include the tasks in the playbook that the automation triggers,

before the role. This ensures no conflict with the Komodo role and maintains compatibility with

the provided examples’ structure.

By   following   the   above   approach,   you’ll   seamlessly   integrate   1Password   CLI   into   the   Komodo

deployment workflow. The periphery nodes will be provisioned with both the Komodo agent and the
op   tool, allowing you to securely fetch and inject secrets as needed, all within the existing Ansible

automation framework.

Sources:

•

Komodo Ansible Role README (host-based periphery deployment)

1

•

Komodo Ansible Role usage example (playbook structure)

4

•

Reddit – Role author on using 1Password for secrets (external to role)

3

4

1

2

4

5

GitHub - bpbradley/ansible-role-komodo: Ansible role for simplified deployment of

Komodo with systemd

https://github.com/bpbradley/ansible-role-komodo

3

Komodo: manage compose files or how to manage VMs, LXCs, Stacks : r/selfhosted

https://www.reddit.com/r/selfhosted/comments/1ib69yx/komodo_manage_compose_files_or_how_to_manage_vms/

5

