---
title: "Komodo FAQ, Tips, and Tricks"
source: "https://blog.foxxmd.dev/posts/komodo-tips-tricks/"
author:
  - "[[FoxxMD]]"
published: 2025-04-02
created: 2025-12-05
description: "Everything you wanted to know but were afraid to ask 🦎"
tags:
  - "clippings"
---
A semi-organized list of FAQs, tips, and tricks for using Komodo. This is a follow-up to my [migration guide and Introduction for Komodo](https://blog.foxxmd.dev/posts/migrating-to-komodo)

This is living guide that will be updated as Komodo is updated and community knowledge is consolidated. For feedback, contributions, and corrections:

- [PRs are welcome](https://github.com/FoxxMD/blog)
- Use the Giscuss widget at the bottom of the post with your Github account
	- Or directly comment on the [discussion](https://github.com/FoxxMD/blog/discussions) thread for this post
- Available in the [Komodo Discord](https://discord.gg/DRqE8Fvg5c) **only** [^1] as `FoxxMD`

## FAQ

### Can Komodo Core update itself?

Yes! If using [systemd Periphery agent](https://komo.do/docs/setup/connect-servers#install-the-periphery-agent---systemd) you can re-deploy a Stack with Komodo Core without issue. If you are using the Docker agent it’s recommended to keep the periphery and core services in different stacks so the UI continues to work during deployment, but not necessary.

### Can Periphery Agents updates be automated?

Not from within Komodo the same way Core be can updated, unfortunately. However, if you are familiar with [Ansbile](https://docs.ansible.com/ansible/latest/getting_started/introduction.html) there several playbooks available from the community to automate this process:

- from mbecker (Komodo creator) [https://github.com/moghtech/komodo/discussions/220](https://github.com/moghtech/komodo/discussions/220)
- from bpbradley [https://github.com/bpbradley/ansible-role-komodo](https://github.com/bpbradley/ansible-role-komodo)

### How can I customize systemd Periphery Agents?

The komodo repository describes where [systemd **service units** are placed](https://github.com/moghtech/komodo/tree/main/scripts#periphery-setup-script) when using the [official install script.](https://komo.do/docs/setup/connect-servers#install-the-periphery-agent---systemd)

Properties like the Working Directory and [Komodo Environmental Variables](https://komo.do/docs/setup/connect-servers#configuration), specific to Komodo, can be added to the Service Unit to configure the Periphery Agent without having to add these to your global environment.

To make these modification use a [**drop-in** file](https://unix.stackexchange.com/a/468067) so that your modifications survive any future Periphery install script updates.

Create and edit a drop-in for the service:

```shell
1
systemctl edit periphery.service
```

(add `--user` if that is how it was installed)

Or manually create it at `/etc/systemd/system/periphery.service.d/override.conf` (path based on install location mentioned in **service units** link above)

Use this file like a regular systemd service definition. Anything here will add to, or override, properties in the existing `periphery.service` unit. EX:

```
1
2
3
[Service]
Environment="PERIPHERY_ROOT_DIRECTORY=/home/myUser/komodo"
Environment="PERIPHERY_DISABLE_TERMINALS=true"
```

Reload systemd config and restart Periphery after making any changes:

```shell
1
2
systemctl daemon-reload
systemctl restart periphery.service
```

### Systemd Periphery stops after closing SSH?

Likely you installed Periphery using `--user`. Depending on your OS, it may exit all processes *started by that user* when that user logs out IE closes SSH connection. Use [`loginctl enable-linger`](https://docs.oracle.com/en/operating-systems/oracle-linux/8/obe-systemd-linger/) to enable processes started by your user to continue running after the sessions has closed:

### How can I migrate periphery agent to systemd?

It is possible to switch between systemd and container [periphery agents](https://komo.do/docs/setup/connect-servers) without needing to re-create your Resources, Stacks, etc… manually, but it may require some planning depending on which you switch from/to.

##### Concepts to Understand

###### Periphery Configuration and State are Indepedent

- Periphery **Configuration** can be expressed as a [`periphery.config.toml` file](https://komo.do/docs/setup/connect-servers#configuration) or as [environmental variables](https://komo.do/docs/setup/connect-servers#install-the-periphery-agent---container). All agent types can use both, but *where they are defined* is dependent on the agent type used.
- Periphery **State** is data storing the *internal state* of the Periphery agent EX what Stacks are on this machine, cloned repositories for Stacks, Build artifacts,.env files written from Stack Environment, etc…
- **State** be reconstructed from a [Resource Sync](https://blog.foxxmd.dev/posts/migrating-to-komodo#resource-sync) but **Configuration** cannot

###### Periphery State is owned by the user running the Agent

All of the files/folders created by the agent to hold **State** is created by the user running the agent:

- [Periphery **container**](https://komo.do/docs/setup/connect-servers#install-the-periphery-agent---container) is run by `root`
- [Periphery systemd **system**](https://github.com/moghtech/komodo/tree/main/scripts#system-requires-root) is run by `root`
- [Periphery systemd **user**](https://github.com/moghtech/komodo/tree/main/scripts#system-requires-root) is run by your local user

If you switch from an agent run by `root` to one run by a local user you may run into file/folder ownership issues. These can be corrected with `chown -R` but it’s important to recognize this is something you may need to deal with.

###### Compose project volumes are independent from Periphery Data and created by the user running the Agent

Any non-existent **bind-mounted** directories, or explicit files, in your compose `volumes:` are created by the user running `docker compose`.

These volumes are *not* part of Periphery State and cannot be fixed by Sync Resource.

Example
```yaml
1
2
3
4
services:
  myService:
    volumes:
      - /my/host/directory:/config
```

If `/my/host/directory` didn’t already exist then it will be created by the user running `docker compose` on the host machine. For Periphery container and system agent this would be `root`. If you decide to switch to systemd **user**, and setup [docker management using a non-root user](https://docs.docker.com/engine/install/linux-postinstall/#manage-docker-as-a-non-root-user), you *may* have permission issues when trying to run the container.

This *could* be fixed by running `chown myUser:myUser /my/host/directory` **without** recursive (no `-R`). But it might need to be fixed on a case-by-case basis as well.

#### Migrating Configuration

If you are using [`environment:` on the periphery container](https://komo.do/docs/setup/connect-servers#install-the-periphery-agent---container) for configuration, EX `PERIPHERY_ROOT` or `PERIPHERY_PASSKEYS` etc.., you will need to **either**

a) convert these to a [`periphery.config.toml`](https://komo.do/docs/setup/connect-servers#configuration) file and place it in the [appropriate path](https://github.com/moghtech/komodo/tree/main/scripts#system-requires-root). If you are already using a `periphery.config.toml` file then make sure to place it in the correct location.

b) after installing systemd agent, create a [drop-in systemd file](https://blog.foxxmd.dev/posts/komodo-tips-tricks/#how-can-i-customize-systemd-periphery-agents) with `Environment=...` entries containing all the environmental configuration currently used in your periphery container

#### Migrating State

Use either of the two methods below to restore Periphery Agent state:

##### Reproducing from Sync Resource

The [periphery state ownership issues](https://blog.foxxmd.dev/posts/komodo-tips-tricks/#periphery-owned) can be bypassed by using a [Resource Sync](https://blog.foxxmd.dev/posts/migrating-to-komodo#resource-sync) to re-deploy the Resources on your server. In this scenario you would:

- create a **Managed** Resource Sync
- Refresh/get the TOML for your server (periphery agent machine) backed up somewhere
- Switch to systemd agent *without* re-using any directories from the container
	- This will create a “blank” server with only your [migrated configuration](https://blog.foxxmd.dev/posts/komodo-tips-tricks/#migrating-periphery-configuration) from the first step, but no Stacks etc… yet
- Create the Resource Sync from the backed up TOML
- **Execute Sync** to re-create all your stacks etc…

This does not re-deploy the actual docker compose projects or create/destroy anything on the machine, just the **periphery state** of “this stack belongs on this machine” etc – so you’d be back to your original state but with the new periphery agent.

##### Using Existing Periphery State Data

You may need to move the directories you bound in the periphery container `volumes:` to their default location of `/etc/komodo` or specify `root_directory` to tell the new agent where the komodo directories are located.

Depending on which [systemd agent you are switching to](https://blog.foxxmd.dev/posts/komodo-tips-tricks/#periphery-owned) you *may* need to change the ownership of the existing komodo directories using `chown`.

If you can resolve these issues then you can use all of the existing komodo data/directories as-is with the new agent.

### How can I automate stack updates?

Komodo has built-in checking for image updates on a Stack. These need to be enabled on each Stack. Find the configuration at

> **Stack** -> **Config** section -> **Auto Update** -> **Poll For Updates**

The interval at which Stacks/Images are polled for updates can be configured using the env `KOMODO_RESOURCE_POLL_INTERVAL` or `resource_poll_interval` variable found in the [Komodo Core configuration](https://komo.do/docs/setup/advanced#mount-a-config-file).

Stacks can be automatically updated using **Auto Update** or **Full Stack Auto Update** toggles also found in the Stack’s Config section.

#### Updating Specific Stacks

For simple stack matching based on name, wildcard, or regex (no lookbehind/backtracing) create a [**Procedure**](https://komo.do/docs/resources/procedures) and use a **Batch Deploy** stage with your desired target.

For more advanced filtering create an **Action** using the snippet below. Fill out the arrays at the top of the snippet with your **exclude** filter values.

Action Snippet
```ts
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30
31
32
33
34
35
36
37
38
39
40
41
42
43
44
45
46
47
48
49
50
51
52
53
54
55
56
// add values to each filter to NOT re-deploy if stack contains X
const REPOS = []; // Stack X Repo 'MyName/MyRepo' includes ANY part of string Y from list
const SERVER_IDS = []; // Stack X Server '67659da61af880a9d21f25be' matches string Y from list
const TAGS = []; // Stack X Tags A,B,C like '67b8cb3ce8d02869dd500af6' matches string Y from list
const STACKS = []; // Stack 'my-cool-stack' matches ANY part of string Y from list
const SERVICES = []; // Stack X Service 'my-cool-service' includes ANY part of string Y from list
const IMAGES = []; // Stack X Image 'lscr.io/linuxserver/socket-proxy:latest' includes ANY part of string Y from list

const intersect = (a: Array<any>, b: Array<any>) => {
    const setA = new Set(a);
    const setB = new Set(b);
    const intersection = new Set([...setA].filter(x => setB.has(x)));
    return Array.from(intersection);
}

const availableUpdates = await komodo.read('ListStacks', {
  query: {
    specific: {
      update_available: true
    }
  }
});

const candidates = availableUpdates.filter(x => {
  if(REPOS.length > 0 && REPOS.some(x => x.info.repo.includes(x))) {
      return false;
  }
  if(SERVER_IDS.length > 0 && SERVER_IDS.includes(x.info.server_id)) {
    return false;
  }
  if(TAGS.length > 0 && intersect(TAGS, x.tags).length > 0) {
    return false;
  }
  if(STACKS.length > 0 && STACKS.some(y => x.name.includes(y))) {
    return false;
  }
  if(SERVICES.length > 0) {
    const s = x.info.services.map(x => x.service);
    if(s.some(x => SERVICES.some(y => x.includes(y)))) {
      return false;
    }
  }
  if(IMAGES.length > 0) {
    const s = x.info.services.map(x => x.image);
    if(s.some(x => IMAGES.includes(y => y.includes(s)))) {
      return false;
    }
  }
  return true;
});

console.log(\`Redeploying:
${candidates.map(x => x.name).join('\n')}\`);

// comment out the line below to test filtering without actually re-deploying anything
await komodo.execute('BatchDeployStack', {pattern: candidates.map(x => x.id).join(',')});
```

Example: To re-deploy any stacks with image updates available EXCEPT any stack/service that contains the word `periphery`, modify the top arrays to contain:

```ts
1
2
const STACKS = ['periphery'];
const SERVICES = ['periphery'];
```

> Since v1.17.2 Both Actions and Procedures can now be .

#### Advanced Update Automation

See Nick Cunningham’s post: [**How To: Automate version updates for your self-hosted Docker containers with Gitea, Renovate, and Komodo**](https://nickcunningh.am/blog/how-to-automate-version-updates-for-your-self-hosted-docker-containers-with-gitea-renovate-and-komodo)

### How do I bulk import existing compose projects?

An existing compose project can be manually imported as a **Files on Server** mode **Stack**. [Make sure the Stack name is the same as the compose project](https://komo.do/docs/resources/docker-compose#importing-existing-compose-projects) so Komodo picks up its status automatically.

Komodo does not have a built-in way to import compose projects automatically. However, **I have created a small tool that can generate [Sync Resource](https://komo.do/docs/resources/sync-resources) TOML from many existing compose projects.** That TOML can be copy-pasted into Komodo, or directly created with the API, to create Stacks from your existing compose-folders.

More information and instructions to use this tool are at [**https://foxxmd.github.io/komodo-import**](https://foxxmd.github.io/komodo-import) and the [interactive Quickstart docs](https://foxxmd.github.io/komodo-import/docs/quickstart)

You will need to create an Alerter that uses the **Custom** endpoint with a service that can ingest it and forward it to your service.

I have developed a few Alerter implementations for popular notification platforms:

- [ntfy](https://github.com/FoxxMD/deploy-ntfy-alerter)
- [gotify](https://github.com/FoxxMD/deploy-gotify-alerter)
- [discord](https://github.com/FoxxMD/deploy-discord-alerter) (more customization than built-in discord)
- [apprise](https://github.com/FoxxMD/deploy-apprise-alerter) (can be used to notify to any of the [100+ providers apprise supports](https://github.com/caronc/apprise/wiki#notification-services) including email)

And the Komodo community is creating more implementations too:

- [telegram](https://github.com/mattsmallman/komodo-alert-to-telgram) uses Cloudflare Workers
- *(more to be added)*

Generally, these are standalone **Stacks** you can run on Komodo. After the Stack is deployed, create a new Alerter with a Custom endpoint and point it to the IP:PORT of the service to finish setup.

#### How do I stop Komodo from sending transient notifications?

You may find Komodo sends notifications for *unresolved* events like `StackStateChange` when it is redeploying a Stack. Or it sends alerts for 100% CPU when it’s only a temporary spike.

For ntfy/gotify/discord/apprise implementations I developed you can use [`UNRESOLVED_TIMEOUT_TYPES` and `UNRESOLVED_TIMEOUT`](https://github.com/FoxxMD/deploy-gotify-alerter/blob/main/README.md?plain=1#L52) to “timeout” temporary events: If the event of `type` is `unresolved` and the alerter sends another event of the same `type` **before** `timeout` milliseconds then it cancels sending the notification.

#### My notification service isn’t listed here! How do I get it to work?

First, you should check if it’s supported by [apprise](https://github.com/caronc/apprise/wiki#notification-services). If it is then use the apprise implementation from above as that is probably the easiest route.

If it is not supported by apprise or you want to build your own then check out my repository where I implemented notification Alerters, [https://github.com/FoxxMD/komodo-utilities](https://github.com/FoxxMD/komodo-utilities). The repo uses VS Code Devcontainers for easy environment setup and each implementation uses the official [Komodo Typescript API client](https://komo.do/docs/ecosystem/api) to make things simple. It should be straightforward to fork my repo, copy-paste one of the existing implementations, and modify [`program.ts`](https://github.com/FoxxMD/komodo-utilities/blob/main/notifiers/gotify/program.ts) to work with your service.

### Run Directory is defined but the entire repo is downloaded?

In a **Stack** config the **Run Directory** only determines the working directory for Komodo to run `compose up -d` from.

Komodo does not do anything “smart” when cloning the repo for the Stack, even if it knows the Run Directory. It’s not possible for it to know if you only use files from that directory for the Stack.

Why Isn't it Smarter?

The issue is the feasibility of covering all use cases vs complexity of the “smartness” involved.

The use-case you may have considered is

> Komodo only needs to know about the files inside **Run Directory**

which is fine when everything inside `compose.yaml` refers to published images or volumes/bind mounts with absolute paths. But consider this example:

> `compose.yaml` uses relative bind mounts to folders a few parents up and sideways…
> 
> ```yaml
> 1
> 2
> 3
> 4
> 5
> services:
>  myService:
>  # ...
>    volumes:
>     - ../../common-data/secrets:/secrets:ro
> ```

Ok…so cloning only **Run Directory** won’t cover this. So Komodo should implement code that instead parses all `volumes`, both short-hand syntax above and [long-hand syntax](https://docs.docker.com/reference/compose-file/services/#volumes) to look for relative paths, parse those, and then include those when cloning. It’s a bit more complicated but possibly still doable.

But what about this scenario?

> `compose.yaml` uses `build` instead of `image` and dockerfile/context is at a relative path
> 
> ```yaml
> 1
> 2
> 3
> 4
> 5
> services:
>  myService:
>    build:
>      context: ../../master-folder/
>      Dockerfile: ../docker/myservice.Dockerfile
> ```
> 
> AND the `Dockerfile` copies files from another relative directory
> 
> ```dockerfile
> 1
> 2
> 3
> FROM nginx:alpine
> 
> COPY ../common-nginx /var/nginx/html
> ```

So, in order for Komodo to cover this use-case it needs to also:

- Check for `build` instead of `image`
	- Parse relative paths in `context`
	- Parse relative paths in `Dockerfile`
- Parse the `Dockerfile`
	- Look for any `COPY` or `ADD` directives, check those for relative paths, and make sure to copy everything from those folders

This is way more complexity. And it’s just scratching the surface of what is possible with the compose specification.

Covering all use-cases may be possible but its a lot of work and maintenance. But there is a simpler and completely fool-proof approach to making sure all of these use-cases work: **clone the entire repository.**

This is already “how it works”. For any project built from a dockerfile/compose.yaml file that is based on a git repo it must be possible to build it if the repo is cloned, so this is exactly what Komodo does. It may not look smart but its actually the simplest solution to covering all use-cases.

Example of Git Repo and Komodo Stack Directory Structure

Git Repo

```
1
2
3
4
5
6
7
8
9
.
├── stacks/
│   ├── immich/
│   │   └── compose.yaml
│   └── frigate/
│       ├── compose.yaml
│       └── compose-nvidia.yaml
└── resources/
    └── servers.toml
```

Komodo config defines root directory (for Komodo) at `/opt/komodo` and you create a Stack named **immich** that uses the git repo from above with run directory `stacks/immich` …

Immich Stack TOML example
```toml
1
2
3
4
5
6
7
8
9
[[stack]]
name = "immich"
[stack.config]
server = "myServer"
git_account = "GitUser"
repo = "GitUser/komodo"
run_directory = "stacks/immich"
environment = """
"""
```

Directory structure on host running **immich** stack:

```
1
2
3
4
5
6
7
8
9
10
11
12
13
.
└── opt/
    └── komodo/
        └── stacks/
            └── immich/
                ├── stacks/
                │   ├── immich/
                │   │   └── compose.yaml
                │   └── frigate/
                │       ├── compose.yaml
                │       └── compose.nvidia.yaml
                └── resources/
                    └── servers.toml
```

The directory stucture is `komodo root directory` + `komodo stacks` + `stack name` + `git repo`

```
1
2
/opt/komodo      /stacks        /immich      /stacks/immich
komodo root dir  komodo stacks  stack name   git repo + run directory
```

If you are concerned about cloning/pulling the same repo for each Stack see [Stacks in Monorepo vs. Stack Per Repo](https://blog.foxxmd.dev/posts/komodo-tips-tricks/#stacks-monorepo-vs-individual) below.

### How do I view logs in real time?

Komodo doesn’t support “true” realtime log viewing yet but “near realtime” logging can be enabled by toggling the **Poll** switch on any Log tab. [Dozzle](https://dozzle.dev/) is a good alternative if you need consolidated, realtime logging for all containers with rich display, search, regex filtering, etc…

### How do I shell/exec/attach to a container?

Starting with Komodo has the ability to open fully-featured, persisted shells on each connected *server* as well as exec’ing into containers. Make sure to read the release notes for what type of *server* terminal is available to you, based on the type of perihery agent installed. The TLDR:

- periphery docker container => shell is inside container and can interact with docker daemon but not host
- periphery systemd (root) => logs in (like SSH) as `root` on host, access to host system and docker daemon
- periphery systemd (user) => logs in (like SSH) as `user` running periphery systemd service, access to host and docker daemon

Expand the sections below for instructions on how to use both:

Server Shell

To access the terminal navigate to the **Server** details page from any Stack/Resource/Server and open the **Terminals** tab to create a new Terminal.

From this terminal any container can be exec’d in to by using normal docker commands IE

```shell
1
docker container exec -it my-container-name /bin/sh
```

> Install this [fuzzy search => exec into container script](https://blog.foxxmd.dev/posts/komodo-tips-tricks/#container-exec-shortcut) as an alias for the logged in user to make exec’ing into a container easier IE
> 
> ```shell
> 1
> 2
> 3
> $ dex sonarr
> # Found media-sonarr-1
> /app #
> ```

This terminal can also be used for general shell access.

Container Shell

Navigate to any **Container** details page from a Stack or `Server -> Docker -> Container` details list. **Note** that the Container details page is NOT the same as the Stack or Service page IE to access a Container from a Stack:

- Navigate to the Stack page (has `/stacks/` in url)
- Switch to the **Services** tab
- Click on any Service in the list (now has `/service/` in url)
- In the Service header click the link with the green cube ([![komodo container icon](https://blog.foxxmd.dev/assets/img/komodo/komodo-container-icon.png)](https://blog.foxxmd.dev/assets/img/komodo/komodo-container-icon.png)) icon
	- url should now have `/container/` in url

From the container details click on the **Terminal** tab to open a terminal and automatically exec into the container. The shell used to exec can be changed from the dropdown on the right side.

### Environmental Variables/Secrets don’t work!

This is likely a misunderstanding of how [Compose file interpolation](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/#env-file) and [environmental variables in Compose](https://docs.docker.com/compose/how-tos/environment-variables) work. Please read [**this guide**](https://blog.foxxmd.dev/posts/compose-envs-explained) for a better understanding of how `.env` `--env-file` `env_file:` and `environment:` work in Docker *as well as* how [Komodo fits into them.](https://blog.foxxmd.dev/posts/compose-envs-explained#komodo-and-envs)

### How do I deploy a service that doesn’t have a published Docker Image?

#### Dockerfile exists and no modification needed

If the service has a project git repository with a `Dockerfile` and you know the project is “ready” and just needs to be built from the Dockerfile ([example](https://github.com/logdyhq/logdy-core)) then this can be done within your `compose.yaml` file! Compose’s build `context` supports directories or a [**URL to a git repository**](https://docs.docker.com/reference/compose-file/build/#context) so:

```yaml
1
2
3
4
5
6
7
services:
  logdy:
    build:
      # can use a specific branch like logdy-core.git#myBranch
      context: https://github.com/logdyhq/logdy-core.git
      # only needed if not in root dir and named Dockerfile
#      dockerfile:  Dockerfile
```

#### Dockerfile needs modification

Docker Compose also allows inlining [`Dockerfile` contents](https://docs.docker.com/reference/compose-file/build/#dockerfile_inline) so if it’s a simple setup it can be yolo’d:

```yaml
1
2
3
4
5
6
7
services:
  myService:
    build:
      context: . # or use a git URL to build with a repository
      dockerfile_inline: |
        FROM baseimage
        RUN some command
```

#### My setup is more complex…

If you need to keep better track of your changes, want to build the image before the stack is deployed, or want n+1 machines on your network to able to use the same build then you need to **build and publish the image** rather than building it inline in the stack.

##### Standalone Container

If the use-case is building one image that can be deployed to **one, standalone container** than the convenient way to do this is to:

- setup a local [Builder](https://komo.do/docs/resources/build-images/builders) and configure a Build without any Image Registry (not publishing externally)
- Build the image
- Create a [Deployment](https://komo.do/docs/resources#deployment) with the [Komodo build you just made](https://komo.do/docs/resources/deploy-containers/configuration#attaching-a-komodo-build)

##### Same-Machine Stack

If you want to keep everything in a **Stack** then

- follow the same steps above (Builder, configure Build)
- On the Build…
	- Make sure to set **Image Name**
	- Add an **Extra Arg** `--load`

This will push the built image to the **local registry on the machine where the Builder ran.** You can then use the Image Name in a Stack deploy **to that same machine only**.

##### Any-Machine Stack

This is the same as the Same-Machine Stack but requires extra step(s) to get the image to another machine.

In all the below options the first step is to build the image using the [Same-Machine Stack](https://blog.foxxmd.dev/posts/komodo-tips-tricks/#same-machine-stack-image) steps from above.

###### Docker save over SSH

This is the most straightforward but least repeatable option. Assuming you can connect to the remote machine over SSH, run this command from the machine where the image was built:

```shell
1
docker save myImageName:latest | ssh <remote server> docker load
```

This command can be used in a [post-build step](https://blog.foxxmd.dev/posts/komodo-tips-tricks/#post-build-step) to automate pushing to the remote machine.

###### Unregistry

Install and use [**unregistry**](https://github.com/psviderski/unregistry) to push images directly from the builder machine to the remote machine.

This is essentially the same as [Docker `save` over SSH](https://blog.foxxmd.dev/posts/komodo-tips-tricks/#save-over-ssh) but is more efficient in that it only pushes missing layers for the image. Make sure to [read the requirements](https://github.com/psviderski/unregistry?tab=readme-ov-file#requirements) for unregistry!

```shell
1
docker pussh myapp:latest user@server
```

This command can be used in a [post-build step](https://blog.foxxmd.dev/posts/komodo-tips-tricks/#post-build-step) to automate pushing to the remote machine.

###### Self-Hosted Registry

Create a docker container/stack that hosts a registry that any machine on your network can pull from.

This approach requires the most work but makes subsequent image publishing as easy as pushing to official registries like dockerhub, github packages, etc. using the **Image Registry** setting in a Komodo Build config.

There are several popular registry images to choose from:

- [Distribution](https://distribution.github.io/distribution/about/deploying/)
	- Simplest to run, requires no setup
	- No authentication out of the box, requires implementing your own
- [Forgejo](https://forgejo.org/docs/latest/user/packages/container/) or [Gitea](https://docs.gitea.com/usage/packages/container)
	- Full git platforms that also provide registries
	- Requires setup and creating a user
	- Authentication out of the box, works with Komodo Git/Registry providers

**By default Docker will only pull from registries using a secure connection (`https`).** You will need to host your registry behind a real domain with some kind of internal access like a reverse proxy. You may want to check out my post on [LAN-Only DNS](https://blog.foxxmd.dev/posts/redundant-lan-dns) and [Traefik](https://blog.foxxmd.dev/posts/migrating-to-traefik) as a jumping off point if you don’t already have this setup.

Using a Registry with an insecure connection

It is not recommended, but this can be done by [modifying the docker daemon on every machine](https://distribution.github.io/distribution/about/insecure/) that will pull the image.

Modify or create `daemon.json` and add your insecure endpoint:

```json
1
2
3
{
  "insecure-registries" : ["192.168.0.101:5000"]
}
```

Then, restart docker for the change to take effect.

### How to create post-build steps for a Build?

There is no built-in “post-build” functionality for a [Build](https://komo.do/docs/build-images). But this functionality can be created using an [Action](https://komo.do/docs/resources/procedures#actions) to run your post-build commands, combined with a [Procedure](https://komo.do/docs/resources/procedures#procedures) that runs the Build.

Create an **Action** that

- Creates a terminal on the Server the Builder runs on
- Executes your post-build script or commands in that terminal
```ts
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
  await komodo.write("CreateTerminal", {
    server: "MyBuilderServer",
    name: "MyTerminalName",
    command: "bash",
    recreate: Types.TerminalRecreateMode.DifferentCommand,
  });

  await komodo.execute_terminal(
    {
      server: "MyBuilderServer",
      terminal: "MyTerminalName",
      // can run any arbitrary shell commands...
      // Run a script saved on the server or run commands directly
      command:
        "cd /my/cool/path/post-build.sh && docker image tag myDigestId myImage:anotherTag",
    },
    {
      onLine: console.log,
      onFinish: (code) => console.log("Finished:", code),
    },
  );
```

Then, create a **Procedure** with two Stages so your steps run sequentially. The first stage is your Build, the second your Post-Build Action

[![post-build procedure](https://blog.foxxmd.dev/assets/img/komodo/post-build-procedure.jpg)](https://blog.foxxmd.dev/assets/img/komodo/post-build-procedure.jpg)

### Is there a Homepage widget?

> Homepage **1.4.0** [introduced](https://github.com/gethomepage/homepage/pull/5407) an [official Komodo Widget](https://gethomepage.dev/widgets/services/komodo/)! You will still need an API Key and Secret (detailed below) but **the custom widget below is no longer needed**.
> 
> The custom widget, however, is still a useful template if you want to display stats other than what the official widget offers.

~~There is no officially integrated~~ [Homepage](https://gethomepage.dev/) [widget](https://gethomepage.dev/widgets/) yet but [stonkage](https://github.com/stonkage) has created a [Custom API widget](https://gethomepage.dev/widgets/services/customapi/) template to [display Total/Running/Unhealthy/Stopped Stacks:](https://github.com/stonkage/fantastic-broccoli/blob/main/Komodo%2Freadme.md)

First, You’ll need an **API Key and Secret** for a Komodo User. (Settings -> Users -> Select User -> Api Keys section)

> I would recommend creating a new “Read Only” Service User. Give it only permissions for Server/Stack Read. Create the API Key and copy the Secret as it will not be shown again.

Custom API Widget Template

### How do I use the API?

Komodo has official [Rust and Typescript clients](https://komo.do/docs/ecosystem/api) for programmatic usage anywhere outside of Komodo. Inside Komodo, the Typescript Client can be used in an [**Action** Resource](https://komo.do/docs/resources/procedures#actions) (which can then be composed as part of a larger [**Procedure** Resource](https://komo.do/docs/resources/procedures)). When using the client within these mentioned Resource it does not need to be authenticated. Additionally, Actions and Procedures can be run on a schedule configured within Komodo.

See the [available modules](https://docs.rs/komodo_client/latest/komodo_client/api/index.html#modules) for all possible functions and example arguments that can be used with the client libraries.

#### Raw HTTP

The API can also be called as a normal HTTP request. [The API documentation](https://docs.rs/komodo_client/latest/komodo_client/api/index.html) describes everything required to build a request.

> I recommend using `X-Api-Key` and `X-Api-Secret` for authentication. To get these you will need to create an Api Key for a user, located in Komodo UI under `Settings -> Users -> (User Detail) -> Api Keys`
> 
> I also recommend creating a new **Service** user for API usage. Remove or restrict premissions to **Read** based on how you will use the API.

## Tips and Tricks

### Stacks in Monorepo vs. Stack Per Repo

There are valid reasons to use individual repositories per stack such as organizational preference, webhook usage for deployment, permissions, large/binary files, etc…

But with majority *text-based* repositories concerns regarding data usage and performance (clone for new stack or pull repo on each deployment) are not usually valid.

**The Reciepts 🧾**

My own monorepo for Komodo contains **100+ stacks** (folders) ranging from full \*arr/Plex sized stacks to single service test stacks.

A full clone of this repository is **2MB on disk.** Benchmarking a full clone of this monorepo against a repo containing only a few text files, both from github, on my Raspberry Pi 4:

```
1
2
3
4
5
Benchmark 1: git clone https://github.com/FoxxMD/[myrepo] myMonoRepoFolder
  Time (abs ≡):        884.1 ms               [User: 306.9 ms, System: 216.4 ms]
 
Benchmark 1: git clone https://github.com/FoxxMD/compose-env-interpolation-example mySimpleFolder
  Time (abs ≡):        389.9 ms               [User: 150.7 ms, System: 107.4 ms]
```

So, **800ms** for the full monorepo and only ~500ms slower than an almost empty repo. On a low power ARM machine. The subsequent pulls to update the repo on redeployment is in the tens of milliseconds.

If you are critically space constrained the size on disk for each stack may be a valid reason to go with per-stack repos but otherwise even an RP4 with a 512GB sd card is not going to have an issue with this setup.

### Shell-into-Container Shortcut

The bash script below can be used to “fuzzy search” for containers by name and then exec into a shell in that container.

Example Usage
```shell
1
2
$ dex
Usage: CONTAINER_FUZZY_NAME [SHELL_CMD:-sh]
```
```bash
1
2
3
$ ./dex.sh sonarr
# Found media-sonarr-1
/app #
```
```bash
1
2
3
4
$ ./dex.sh test
Found: test-new-app-1
Found: test-1
More than one container found, be more specific
```
```bash
1
2
3
$ ./dex.sh sonarr /bin/ash
# Found container media-sonarr-1
/app #
```
Bash Script
```bash
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30
31
32
33
34
35
36
37
38
39
40
41
42
43
44
45
46
47
48
49
50
51
#!/bin/bash

# * Does a fuzzy search for container by name so you only need a partial name
#   * If there is more than one container with partial name it will print all containers
#     * And if one is an exact match then use it otherwise exit
#   * If there is only one container matching it execs
# * Second arg can be shell command to use, defaults to sh
# EX
# ./dex.sh sonarr
# ./dex.sh sonarr bash
#
# Can be set in a function in .bashrc for easy aliasing

if [[ -z "$1" ]]; then
  printf "Usage: CONTAINER_FUZZY_NAME [SHELL_CMD:-sh]\n"
else

  names=$(docker ps --filter name=^/.*$1.*$ --format '{{.Names}}')
  lines=$(echo -n "$names" | grep -c '^')
  name=""

  if [ "$lines" -eq "0" ]; then

    printf "No container found\n"

  elif [ "$lines" -gt "1" ]; then

    while IFS= read -r line
    do
      printf "Found: %s\n" "$line"
      if [ "$line" = "$1" ]; then
        name="$1"
      fi
    done < <(printf '%s\n' "$names")

    if [[ -z "$name" ]]; then
      printf "More than one container found, be more specific\n"
    else
      printf "More than one container found but input matched one perfectly.\n"
    fi

  else
      name="$names"
      printf "Found: %s\n" "$name"
  fi

  if [[ -n "$name" ]]; then
    docker container exec -it $name ${2:-sh}
  fi

fi
```

Save this script and `chmod +x` it on each machine, then add it as an alias to the appropriate user’s `.bashrc` to make it a command line shortcut:

```bash
1
alias dex="~/dex.sh"
```

### Docker Data Agnostic Location

One of the benefits to Komodo is being able to re-deploy a stack to any Server with basically one click. What isn’t so easy, though, is moving (or generally locating) any persistent data that needs to be mounted into those services.

If you use named volumes and have a backup strategy already this is a moot point but if you are like me and use [bind mounts](https://docs.docker.com/engine/storage/bind-mounts/) I found a good approach is to use a host-specific ENV as a directory prefix when writing compose files.

This has the advantage of making the compose bind mount location agnostic to the host it is on and makes moving data, or rebuilding a host, much easier since compose files don’t need to be modifed if the data location changes parent directories.

An example:

```yaml
1
2
3
4
5
services:
  my-service:
    image: #...
    volumes:
      - $DOCKER_DATA/my-service-data:/app/data
```

As long as `DOCKER_DATA` is set as an ENV on each host then the compose file becomes storage location agnostic. It doesn’t matter whether you use `/home/MyUser/docker` or `/opt/docker` or whatever.

To do this you’ll need to set this ENV in either the shell used by Periphery (`.bashrc` or `.profile`), set in the Periphery’s docker container ENVs, or set it in the [systemd configuration](https://www.baeldung.com/linux/systemd-services-environment-variables) for a [systemd periphery agent.](https://github.com/mbecker20/komodo/blob/main/scripts/readme.md#periphery-setup-script)

Setting ENV for systemd periphery

**For systemd periphery** check which [`periphery.service` install path](https://github.com/moghtech/komodo/tree/main/scripts) you used and then add a folder `periphery.service.d` with file `override.conf` with the contents:

```
1
2
[Service]
Environment="DOCKER_DATA=/home/myUser/docker-data"
```

and then restart the periphery service

EX

```
1
2
/home/foxx/.config/systemd/user/periphery.service <--- systemd unit for periphery
/home/foxx/.config/systemd/user/periphery.service.d/override.conf  <--- config to provide \`Environment\`
```
Setting ENV for docker periphery

**For docker periphery** container make sure you add `DOCKER_DATA` to your environment:

```yaml
1
2
3
4
5
6
7
services:
  periphery:
    image: ghcr.io/moghtech/komodo-periphery:latest
    # ...
    environment:
      # ...
      DOCKER_DATA: /home/myUser/docker-data
```

and then restart the periphery container.

### Monitoring Services with Komodo and Uptime Kuma

[Uptime Kuma](https://uptime.kuma.pet/) has the *Docker Container* monitor type but using Komodo’s API has the advantage of being able to monitor a Stack/Service status **independent of what Server it is deployed to and what the container name is.**

#### Prerequisites

You’ll need an **API Key and Secret** for a Komodo User. (Settings -> Users -> Select User -> Api Keys section)

I would recommend creating a new “Read Only” Service User. Give it only permissions for Server/Stack Read. Create the API Key and copy the Secret as it will not be shown again.

#### Create Uptime Kuma Monitor

Create a new Monitor with the type `HTTP(s) - Json Query`

##### HTTP Options

- Method: `POST`
- Body Encoding: `JSON`

##### Body

Visit the Stack in Komodo UI and copy the ID after `/stacks/` from the URL. Use it in `stack` value below:

```json
1
2
3
4
5
6
{
    "type": "ListStackServices",
    "params": {
        "stack": "67913976afe9cffd0fa1f963"
    }
}
```

##### Headers

Use the Api Key and Secret created earlier:

```json
1
2
3
4
{
    "X-Api-Key": "YourKey",
    "X-Api-Secret": "YourSecret"
}
```

##### URL

```
1
http://YOUR_KOMODO_SERVER/read
```

##### Json Query / Expected Value

To monitor **all** services in the stack and report UP only if **all** are running

- Json Query: `$count($.container[state!='running'].state ) = 0`
- Expected Value: `true`

To monitor a **specific** service in the stack and report UP if it is running

- Json Query: `$[service="SERVICE_NAME_FROM_COMPOSE"].container.state`
- Expected Value: `running`

### Resource Templates

, Komodo now supports Resource **Templates**. These are partial (or full!) Resources that can be used as a “starting point” for any new Resources of the same type you create.

**To create a template** create any Resource as normal (or use an existing one). From the Resource’s detail page toggle the **Template** switch in right-hand corner of the Detail header (across from the Resource’s name).

[![komodo template switch](https://blog.foxxmd.dev/assets/img/komodo/komodo-template.png)](https://blog.foxxmd.dev/assets/img/komodo/komodo-template.png)

#### Example

A common usage for a Template would be to “pre-fill” **Stack** Resource with your default settings for a monorepo:

- Create a new Stack named `Repo Template`
- Leave **Server** empty and choose **Mode** `Git Repo`
- Set **Git Provider**, **Account**, and **Repo** with your monorepo details
- Set the **Run Directory** to your common stack root directory (if applicable)
- Add `TZ=America/New_York` to the **Environment** section so it’s included in any new stacks

Save your changes, then toggle **Template** on for the Stack.

Now, when creating a new Stack, you can choose the `Repo Template` from the **Template** dropdown in the new Stack dialog to get all those fields/config filled automatically.

> Remember, Templates can be created for **any type of Resource**, not just Stacks.

---

[^1]: Please do not DM me unless we have discussed this prior. I get way too much discord DM spam and will most likely ignore you. @ me on the Komodo server instead.