---
title: "Configuration File"
source: "https://docs.pangolin.net/self-host/advanced/config-file"
author:
  - "[[​]]"
published:
created: 2025-12-08
description: "Configure Pangolin using the config.yml file with detailed settings for all components"
tags:
  - "clippings"
---
The `config.yml` file controls all aspects of your Pangolin deployment, including server settings, domain configuration, email setup, and security options. This file is mounted at `config/config.yml` in your Docker container.

## Setting up your config.yml

To get started, create a basic configuration file with the essential settings:Minimal Pangolin configuration:

Generate a strong secret for `server.secret`. Use at least 32 characters with a mix of letters, numbers, and special characters.

## Reference

This section contains the complete reference for all configuration options in `config.yml`.

### Application Settingsapp

object

required

Core application configuration including dashboard URL, logging, and general settings.

ShowAppdashboard\_url

string

required

The URL where your Pangolin dashboard is hosted.**Examples**: `https://example.com`, `https://pangolin.example.com` This URL is used for generating links, redirects, and authentication flows. You can run Pangolin on a subdomain or root domain.log\_level

string

The logging level for the application.**Options**: `debug`, `info`, `warn`, `error` **Default**: `info`save\_logs

boolean

Whether to save logs to files in the `config/logs/` directory.**Default**: `false`

When enabled, logs rotate automatically:
- Max file size: 20MB
- Max files: 7 dayslog\_failed\_attempts

boolean

Whether to log failed authentication attempts for security monitoring.**Default**: `false`telemetry

object

Telemetry configuration settings.

ShowTelemetryanonymous\_usage

boolean

Whether to enable anonymous usage telemetry.**Default**: `true`notifications

object

Notification configuration settings.

ShowNotificationproduct\_updates

boolean

Whether to enable showing product updates notifications on the UI.**Default**: `true`new\_releases

boolean

Whether to enable showing new releases notifications on the UI.**Default**: `true`

### Server Configurationserver

object

required

Server ports, networking, and authentication settings.

ShowServerexternal\_port

integer

The port for the front-end API that handles external requests.**Example**: `3000`internal\_port

integer

The port for the internal private-facing API.**Example**: `3001`

The port for the frontend server (Next.js).**Example**: `3002`integration\_port

integer

The port for the integration API (optional).**Example**: `3003`internal\_hostname

string

The hostname of the Pangolin container for internal communication.**Example**: `pangolin`

If using Docker Compose, this should match your container name.resource\_access\_token\_param

string

Query parameter name for passing access tokens in requests.**Example**: `p_token` **Default**: `p_token`resource\_session\_request\_param

string

Query parameter for session request tokens.**Example**: `p_session_request` **Default**: `p_session_request`cors

object

Cross-Origin Resource Sharing (CORS) configuration.

ShowCORSorigins

array of strings

Allowed origins for cross-origin requests.**Example**: `["https://pangolin.example.com"]`methods

array of strings

Allowed HTTP methods for CORS requests.**Example**: `["GET", "POST", "PUT", "DELETE", "PATCH"]`credentials

boolean

Whether to allow credentials in CORS requests.**Default**: `true`dashboard\_session\_length\_hours

integer

Dashboard session duration in hours.**Example**: `720` (30 days) **Default**: `720`resource\_session\_length\_hours

integer

Resource session duration in hours.**Example**: `720` (30 days) **Default**: `720`secret

string

required

Secret key for encrypting sensitive data.**Environment Variable**: `SERVER_SECRET` **Minimum Length**: 8 characters **Example**: `"d28@a2b.2HFTe2bMtZHGneNYgQFKT2X4vm4HuXUXBcq6aVyNZjdGt6Dx-_A@9b3y"`

Generate a strong, random secret. This is used for encrypting sensitive data and should be kept secure.maxmind\_db\_path

string

Path to the MaxMind GeoIP database file for geolocation features.**Example**: `./config/GeoLite2-Country.mmdb`

Used for IP geolocation functionality. Requires a MaxMind GeoLite2 or GeoIP2 database file.

### Domain Configurationdomains

object

required

Domain settings for SSL certificates and routing.At least one domain must be configured.It is best to add it in the UI for ease of use or when you want the domain to *only be present in the org it was created in*.You should create it in the config file for permanence across installs and if you want the domain to be present in all orgs.

ShowDomains<domain\_key>

object

Domain configuration with a unique key of your choice.base\_domain

string

required

The base domain for this configuration.**Example**: `example.com`cert\_resolver

string

required

The Traefik certificate resolver name.**Example**: `letsencrypt`

This must match the certificate resolver name in your Traefik configuration.prefer\_wildcard\_cert

boolean

Whether to prefer wildcard certificates for this domain.**Example**: `true`

Useful for domains with many subdomains to reduce certificate management overhead.

### Traefik Integrationtraefik

object

Traefik reverse proxy configuration settings.

ShowTraefikhttp\_entrypoint

string

The Traefik entrypoint name for HTTP traffic.**Example**: `web`

Must match the entrypoint name in your Traefik configuration.https\_entrypoint

string

The Traefik entrypoint name for HTTPS traffic.**Example**: `websecure`

Must match the entrypoint name in your Traefik configuration.cert\_resolver

string

The default certificate resolver for domains created through the UI.**Example**: `letsencrypt`

This only applies to domains created through the Pangolin dashboard.prefer\_wildcard\_cert

boolean

Whether to prefer wildcard certificates for UI-created domains.**Example**: `true`

This only applies to domains created through the Pangolin dashboard.additional\_middlewares

array of strings

Additional Traefik middlewares to apply to resource routers.**Example**: `["middleware1", "middleware2"]`

These middlewares must be defined in your Traefik dynamic configuration.certificates\_path

string

Path where SSL certificates are stored. This is used only with managed Pangolin deployments.**Example**: `/var/certificates` **Default**: `/var/certificates`monitor\_interval

integer

Interval in milliseconds for monitoring configuration changes.**Example**: `5000` **Default**: `5000`dynamic\_cert\_config\_path

string

Path to the dynamic certificate configuration file. This is used only with managed Pangolin deployments.**Example**: `/var/dynamic/cert_config.yml` **Default**: `/var/dynamic/cert_config.yml`dynamic\_router\_config\_path

string

Path to the dynamic router configuration file.**Example**: `/var/dynamic/router_config.yml` **Default**: `/var/dynamic/router_config.yml`site\_types

array of strings

Supported site types for Traefik configuration.**Example**: `["newt", "wireguard", "local"]` **Default**: `["newt", "wireguard", "local"]`file\_mode

boolean

Whether to use file-based configuration mode for Traefik.**Example**: `false` **Default**: `false`

When enabled, uses file-based dynamic configuration instead of API-based updates.

### Gerbil Tunnel Controllergerbil

object

required

Gerbil tunnel controller settings for WireGuard tunneling.

ShowGerbilbase\_endpoint

string

required

Domain name included in WireGuard configuration for tunnel connections.**Example**: `pangolin.example.com`start\_port

integer

Starting port for WireGuard tunnels.**Example**: `51820`use\_subdomain

boolean

Whether to assign unique subdomains to Gerbil exit nodes.**Default**: `false`

Keep this set to `false` for most deployments.subnet\_group

string

IP address CIDR range for Gerbil exit node subnets.**Example**: `10.0.0.0/8`block\_size

integer

Block size for Gerbil exit node CIDR ranges.**Example**: `24`site\_block\_size

integer

Block size for site CIDR ranges connected to Gerbil.**Example**: `26`

### Organization Settingsorgs

object

Organization network configuration settings.

ShowOrganizationsblock\_size

integer

Block size for organization CIDR ranges.**Example**: `24` **Default**: `24`

Determines the subnet size allocated to each organization for network isolation.subnet\_group

string

IP address CIDR range for organization subnets.**Example**: `100.90.128.0/24` **Default**: `100.90.128.0/24`

Base subnet from which organization-specific subnets are allocated.

### Rate Limitingrate\_limits

object

Rate limiting configuration for API requests.global

object

Global rate limit settings for all external API requests.

ShowGlobalwindow\_minutes

integer

Time window for rate limiting in minutes.**Example**: `1`max\_requests

integer

Maximum number of requests allowed in the time window.**Example**: `100`auth

object

Rate limit settings specifically for authentication endpoints.window\_minutes

integer

Time window for authentication rate limiting in minutes.**Example**: `1` **Default**: `1`max\_requests

integer

Maximum number of authentication requests allowed in the time window.**Example**: `10` **Default**: `500`

Consider setting this lower than global limits for security.

### Email Configurationemail

object

SMTP settings for sending transactional emails.

ShowEmailsmtp\_host

string

SMTP server hostname.**Example**: `smtp.gmail.com`smtp\_port

integer

SMTP server port.**Example**: `587` (TLS) or `465` (SSL)smtp\_user

string

SMTP username.**Example**: `no-reply@example.com`smtp\_pass

string

SMTP password.**Environment Variable**: `EMAIL_SMTP_PASS`smtp\_secure

boolean

Whether to use secure connection (SSL/TLS).**Default**: `false`

Enable this when using port 465 (SSL).no\_reply

string

From address for sent emails.**Example**: `no-reply@example.com`

Usually the same as `smtp_user`.

smtp\_tls\_reject\_unauthorized

boolean

Whether to fail on invalid server certificates.**Default**: `true`

### Feature Flagsflags

object

Feature flags to control application behavior.

ShowFlagsrequire\_email\_verification

boolean

Whether to require email verification for new users.**Default**: `false`

Only enable this if you have email configuration set up.disable\_user\_create\_org

boolean

Whether to prevent users from creating organizations.**Default**: `false`

Server admins can always create organizations.allow\_raw\_resources

boolean

Whether to allow raw TCP/UDP resource creation.**Default**: `true`

If set to `false`, users will only be able to create http/https resources.enable\_integration\_api

boolean

Whether to enable the integration API.**Default**: `false`disable\_local\_sites

boolean

Whether to disable local site creation and management.**Default**: `false`

When enabled, users cannot create sites that connect to local networks.disable\_basic\_wireguard\_sites

boolean

Whether to disable basic WireGuard site functionality.**Default**: `false`

When enabled, only advanced WireGuard configurations are allowed.disable\_config\_managed\_domains

boolean

Whether to disable domains managed through the configuration file.**Default**: `false`

When enabled, only domains created through the UI are allowed.

### Database Configurationpostgres

object

PostgreSQL database configuration (optional).

ShowPostgreSQLconnection\_string

string

required

PostgreSQL connection string.**Example**: `postgresql://user:password@host:port/database`

See [PostgreSQL documentation](https://docs.pangolin.net/self-host/advanced/database-options#postgresql) for setup instructions.replicas

array of objects

Read-only replica database configurations for load balancing.connection\_string

string

required

Connection string for the read replica database.**Example**: `postgresql://user:password@replica-host:port/database`pool

object

Database connection pool settings.max\_connections

integer

Maximum number of connections to the primary database.**Default**: `20` **Example**: `50`max\_replica\_connections

integer

Maximum number of connections to replica databases.**Default**: `10` **Example**: `25`idle\_timeout\_ms

integer

Time in milliseconds before idle connections are closed.**Default**: `30000` (30 seconds) **Example**: `60000`connection\_timeout\_ms

integer

Time in milliseconds to wait for a database connection.**Default**: `5000` (5 seconds) **Example**: `10000`

### DNS Configurationdns

object

DNS settings for domain name resolution and CNAME extensions.

ShowDNSnameservers

array of strings

List of nameservers used for DNS resolution.**Example**: `["ns1.example.com", "ns2.example.com"]` **Default**: `["ns1.pangolin.net", "ns2.pangolin.net", "ns3.pangolin.net"]`

These nameservers are used for DNS queries and domain resolution.cname\_extension

string

Domain extension used for CNAME record management.**Example**: `cname.example.com` **Default**: `cname.pangolin.net`

Used for creating CNAME records for dynamic domain routing.

## Environment Variables

Some configuration values can be set using environment variables for enhanced security: