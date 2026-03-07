---
title: "Blueprints"
source: "https://docs.pangolin.net/manage/blueprints"
author:
  - "[[Pangolin Docs]]"
published:
created: 2025-12-05
description: "Pangolin Blueprints are declarative configurations that allow you to define your resources and their settings in a structured format"
tags:
  - "clippings"
---
Blueprints provide a way to define your Pangolin resources and their configurations in a structured, declarative format. This allows for easier management, version control, and automation of your resource setups.![](https://www.youtube.com/watch?v=lMauwwitSAE)

## Overview

Pangolin supports two blueprint formats:
1. **YAML Configuration Files**: Standalone configuration files
2. **Docker Labels**: Configuration embedded in Docker Compose files

## YAML Configuration Format

YAML config can be applied using Docker labels, API, from a Newt site, or in the UI. *Application through a CLI tool is planned.*

## Newt YAML

Newt automatically discovers and applies blueprints defined in YAML format when passing the `--blueprint-file` argument. For example

```
newt --blueprint-file /path/to/blueprint.yaml <other-args>
```

## API YAML

You can also apply blueprints directly through the Pangolin API with an API key. [Take a look at the API documentation for more details.](https://api.pangolin.net/v1/docs/#/Organization/put_org__orgId__blueprint)POST to `/org/{orgId}/blueprint` with a base64 encodes JSON body like the following:

```
{

  "blueprint": "base64-encoded-json-content"

}
```

[See this python example](https://github.com/fosrl/pangolin/blob/dev/blueprint.py)

### Proxy Resources

Proxy resources are used to expose HTTP, TCP, or UDP services through Pangolin. Below is an example configuration for proxy resources:

### Authentication Configuration

Authentication is off by default. You can enable it by adding the relevant fields in the `auth` section as shown in the example below.

```
proxy-resources:

  secure-resource:

    name: Secured Resource

    protocol: http

    full-domain: secure.example.com

    auth:

      pincode: 123456

      password: your-secure-password

      basic-auth:

        user: asdfa

        password: sadf

      sso-enabled: true

      sso-roles:

        - Member

        - Admin

      sso-users:

        - user@example.com

      whitelist-users:

        - admin@example.com
```

### Targets-Only Resources

You can define simplified resources that contain only target configurations. This is useful for adding targets to existing resources or for simple configurations:

```
proxy-resources:

  additional-targets:

    targets:

    - site: another-site

      hostname: backend-server

      method: https

      port: 8443

    - site: another-site

      hostname: backup-server

      method: http

      port: 8080
```

When using targets-only resources, the `name` and `protocol` fields are not required. All other resource-level validations are skipped for these simplified configurations.

### Client Resources

Client resources define proxied resources accessible when connected via an Olm client:

```
client-resources:

  client-resource-nice-id-uno:

    name: this is my resource

    protocol: tcp

    proxy-port: 3001

    hostname: localhost

    internal-port: 3000

    site: lively-yosemite-toad
```

For containerized applications, you can define blueprints using Docker labels.

Blueprints will **continuously apply** from changes in the docker stack, newt restarting, or when viewing the resource in the dashboard.

### Enabling Docker Socket Access

To use Docker labels, enable the Docker socket when running Newt:

```
newt --docker-socket /var/run/docker.sock <other-args>
```

or using the environment variable:

```
DOCKER_SOCKET=/var/run/docker.sock
```

### Docker Compose Example

The compose file will be the source of truth, any edits through the resources dashboard will be **overwritten** by the blueprint labels defined in the compose stack.

This will create a resource that looks like the following:

![Example resource](https://mintcdn.com/fossorial/urmJt4paswtGsg_S/images/docker-compose-blueprint-example.png?w=280&fit=max&auto=format&n=urmJt4paswtGsg_S&q=85&s=d8a553522179030f7a53067afa2c9d5b)

Example resource

Pangolin UI showing Docker Compose blueprint example

## Automatic Discovery

When hostname and internal port are not explicitly defined in labels, Pangolin will automatically detect them from the container configuration.

## Site Assignment

If no site is specified in the labels, the resource will be assigned to the Newt site that discovered the container.

## Configuration Merging

Configuration across different containers is automatically merged to form complete resource definitions. This allows you to distribute targets across multiple containers while maintaining a single logical resource.

## Configuration Properties

### Proxy Resources

### Target Configuration

### Health Check Configuration

Health checks can be configured for individual targets to monitor their availability. Add a `healthcheck` object to any target:

### Authentication Configuration

Not allowed on TCP/UDP resources.

### Rules Configuration

### Client Resources

These are resources used with Pangolin Olm clients (e.g., SSH, RDP).

## Validation Rules and Constraints

### Resource-Level Validations

1. **Targets-Only Resources**: A resource can contain only the `targets` field, in which case `name` and `protocol` are not required.
2. **Protocol-Specific Requirements**:
	- **HTTP Protocol**: Must have `full-domain` and all targets must have `method` field
	- **TCP/UDP Protocol**: Must have `proxy-port` and targets must NOT have `method` field
	- **TCP/UDP Protocol**: Cannot have `auth` configuration
3. **Port Uniqueness**:
	- `proxy-port` values must be unique within `proxy-resources`
	- `proxy-port` values must be unique within `client-resources`
	- Cross-validation between proxy and client resources is not enforced
4. **Domain Uniqueness**: `full-domain` values must be unique across all proxy resources
5. **Target Method Requirements**: When protocol is `http`, all non-null targets must specify a `method`
When working with blueprints, you may encounter these validation errors:

### “Admin role cannot be included in sso-roles”

The `Admin` role is reserved and cannot be included in the `sso-roles` array for authentication configuration.

### ”Duplicate ‘full-domain’ values found”

Each `full-domain` must be unique across all proxy resources. If you need multiple resources for the same domain, use different subdomains or paths.

### ”Duplicate ‘proxy-port’ values found”

Port numbers in `proxy-port` must be unique within their resource type (proxy-resources or client-resources separately).

### ”When protocol is ‘http’, all targets must have a ‘method’ field”

All targets in HTTP proxy resources must specify whether they use `http`, `https`, or `h2c`.

### ”When protocol is ‘tcp’ or ‘udp’, targets must not have a ‘method’ field”

TCP and UDP targets should not include the `method` field as it’s only applicable to HTTP resources.

### ”When protocol is ‘tcp’ or ‘udp’, ‘auth’ must not be provided”

Authentication is only supported for HTTP resources, not TCP or UDP.

### ”Resource must either be targets-only or have both ‘name’ and ‘protocol’ fields”

Resources must either contain only the `targets` field (targets-only) or include both `name` and `protocol` for complete resource definitions.[Health Checks](https://docs.pangolin.net/manage/healthchecks-failover)

[

Previous

](https://docs.pangolin.net/manage/healthchecks-failover)[

Highly Available Nodes

Next

](https://docs.pangolin.net/manage/remote-node/ha)