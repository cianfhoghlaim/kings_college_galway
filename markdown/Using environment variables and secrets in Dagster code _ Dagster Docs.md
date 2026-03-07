---
title: "Using environment variables and secrets in Dagster code | Dagster Docs"
source: "https://docs.dagster.io/guides/operate/configuration/using-environment-variables-and-secrets"
author:
published:
created: 2025-12-12
description: "Dagster environment variables allow you to define various configuration options for your Dagster application and securely set up secrets."
tags:
  - "clippings"
---
Environment variables, which are key-value pairs configured outside your source code, allow you to dynamically modify application behavior depending on environment.

Using environment variables, you can define various configuration options for your Dagster application and securely set up secrets. For example, instead of hard-coding database credentials - which is bad practice and cumbersome for development - you can use environment variables to supply user details. This allows you to parameterize your pipeline without modifying code or insecurely storing sensitive data.

## Declaring environment variables

How environment variables are declared depends on whether you're developing locally or have already deployed your Dagster project.

## Accessing environment variables

In this section, we'll demonstrate how to access environment variables once they've been declared. There are two ways to do this:

- [In Python code](https://docs.dagster.io/guides/operate/configuration/#in-python-code)
- [From Dagster configuration](https://docs.dagster.io/guides/operate/configuration/#from-dagster-configuration), which incorporates environment variables into the Dagster config system

### In Python code

To access environment variables in your code, you can either use the [`os.getenv`](https://docs.python.org/3/library/os.html#os.getenv) function or the Dagster [`EnvVar`](https://docs.dagster.io/api/dagster/resources#dagster.EnvVar) class.

- **When you use `os.getenv`**, the variable's value is retrieved when Dagster loads the code location and **will** be visible in the UI.
- **When you use EnvVar**, the variable's value is retrieved at runtime and **won't** be visible in the UI.

Using the `EnvVar` approach has a few unique benefits:

- **Improved observability.** The UI will display information about configuration values sourced from environment variables.
- **Secret values are hidden in the UI.** Secret values are hidden in the Launchpad, Resources page, and other places where configuration is displayed.
- **Simplified testing.** Because you can provide string values directly to configuration rather than environment variables, testing may be easier.

#### os.getenv function

Below is an example of retrieving an environment variable with `os.getenv`:

```python
import os

database_name = os.getenv("DATABASE_NAME")
```

You can also use `os.getenv` to access [built-in environment variables for Dagster+](https://docs.dagster.io/deployment/dagster-plus/management/environment-variables/built-in):

```python
import os

deployment_name = os.getenv("DAGSTER_CLOUD_DEPLOYMENT_NAME")
```

For a real-world example, see the [Dagster+ branch deployments example](https://docs.dagster.io/guides/operate/configuration/#dagster-branch-deployments).

#### Dagster EnvVar class

To use the `EnvVar` approach, call the `get_value()` method on the Dagster [`EnvVar`](https://docs.dagster.io/api/dagster/resources#dagster.EnvVar) class:

```python
import dagster as dg

database_name = dg.EnvVar('DATABASE_NAME').get_value()
```

### From Dagster configuration

[Configurable Dagster objects](https://docs.dagster.io/guides/operate/configuration/run-configuration) (such as ops, assets, resources, I/O managers, and so on) can accept configuration from environment variables with `EnvVar`. These environment variables are retrieved at launch time, rather than on initialization as with `os.getenv`.

## Handling secrets

Using environment variables to provide secrets ensures sensitive information won't be visible in your code or the launchpad in the UI. In Dagster, we recommend using [configuration](https://docs.dagster.io/guides/operate/configuration/run-configuration) and [resources](https://docs.dagster.io/guides/build/external-resources) to manage secrets.

A resource is typically used to connect to an external service or system, such as a database. Resources can be configured separately from your assets, allowing you to define them once and reuse them as needed.

Let's take a look at an example that creates a resource called `SomeResource` and supplies it to assets. Let's start by looking at the resource:

```python
src/<project_name>/defs/resources.pyimport dagster as dg

class SomeResource(dg.ConfigurableResource): ...

@dg.definitions
def defs() -> dg.Definitions:
    return dg.Definitions(
        resources={"some_resource": SomeResource(access_token="foo")},
    )
```

Let's review what's happening here:

- This code creates a resource named `SomeResource`
- By subclassing [`ConfigurableResource`](https://docs.dagster.io/api/dagster/resources#dagster.ConfigurableResource) and specifying the `access_token` field, we're telling Dagster that we want to be able to configure the resource with an `access_token` parameter, which is a string value

By including a reference to `SomeResource` in a `@dg.definitions` -decorated function, we make that resource available to assets defined elsewhere in the `src/<project_name>/defs` directory:

```python
src/<project_name>/defs/assets.pyimport dagster as dg

from .resources import SomeResource

@dg.asset
def my_asset(some_resource: SomeResource) -> None: ...
```

As storing secrets in configuration is bad practice, we'll use an environment variable:

```python
src/<project_name>/defs/resources.pyimport dagster as dg

class SomeResource(dg.ConfigurableResource): ...

@dg.definitions
def defs() -> dg.Definitions:
    return dg.Definitions(
        resources={
            "some_resource": SomeResource(access_token=dg.EnvVar("MY_ACCESS_TOKEN"))
        },
    )
```

In this code, we pass configuration information to the resource when we construct it. In this example, we're telling Dagster to load the `access_token` from the `MY_ACCESS_TOKEN` environment variable by wrapping it in `dg.EnvVar`.

## Parameterizing pipeline behavior

Using environment variables, you define how your code should execute at runtime.

### Per-environment configuration

In this example, we'll demonstrate how to use different I/O manager configurations for `local` and `production` environments using [configuration](https://docs.dagster.io/guides/operate/configuration/run-configuration) (specifically the configured API) and [resources](https://docs.dagster.io/guides/build/external-resources).

This example is adapted from the [Transitioning data pipelines from development to production guide](https://docs.dagster.io/guides/operate/dev-to-prod):

```python
src/<project_name>/defs/resources.pyimport os

from dagster_snowflake_pandas import SnowflakePandasIOManager

import dagster as dg

def resources_by_deployment() -> dict:
    return {
        "local": {
            "snowflake_io_manager": SnowflakePandasIOManager(
                account="abc1234.us-east-1",
                user=dg.EnvVar("DEV_SNOWFLAKE_USER"),
                password=dg.EnvVar("DEV_SNOWFLAKE_PASSWORD"),
                database="LOCAL",
                schema=dg.EnvVar("DEV_SNOWFLAKE_SCHEMA"),
            ),
        },
        "production": {
            "snowflake_io_manager": SnowflakePandasIOManager(
                account="abc1234.us-east-1",
                user="system@company.com",
                password=dg.EnvVar("SYSTEM_SNOWFLAKE_PASSWORD"),
                database="PRODUCTION",
                schema="HACKER_NEWS",
            ),
        },
    }

@dg.definitions
def resources() -> dg.Definitions:
    deployment_name = os.getenv("DAGSTER_DEPLOYMENT", "local")
    return dg.Definitions(resources=resources_by_deployment()[deployment_name])
```

Let's review what's happening here:

- We've created a dictionary of resource definitions called `resources`, with sections for `local` and `production` environments. In this example, we're using a [Pandas Snowflake I/O manager](https://docs.dagster.io/api/libraries/dagster-snowflake-pandas).
- For both `local` and `production`, we constructed the I/O manager using environment-specific run configuration.
- Following the `resources` dictionary, we define the `deployment_name` variable, which determines the current executing environment. This variable defaults to `local`, ensuring that `DAGSTER_DEPLOYMENT=PRODUCTION` must be set to use the `production` configuration.

### Dagster+ branch deployments

You can determine the current deployment type ([branch deployment](https://docs.dagster.io/deployment/dagster-plus/deploying-code/branch-deployments) or [full deployment](https://docs.dagster.io/deployment/dagster-plus/deploying-code/full-deployments)) at runtime with the `DAGSTER_CLOUD_IS_BRANCH_DEPLOYMENT` environment variable. Using this information, you can write code that executes differently when in a branch deployment or a full deployment.

```python
def get_current_env():
  is_branch_depl = os.getenv("DAGSTER_CLOUD_IS_BRANCH_DEPLOYMENT") == "1"
  assert is_branch_depl != None  # env var must be set
  return "branch" if is_branch_depl else "prod"
```

This function checks the value of `DAGSTER_CLOUD_IS_BRANCH_DEPLOYMENT` and, if equal to `1`, returns a variable with the value of `branch`. This indicates that the current deployment is a branch deployment. Otherwise, the deployment is a full deployment and `is_branch_depl` will be returned with a value of `prod`.

## Troubleshooting

| Error | Description | Solution |
| --- | --- | --- |
| **You have attempted to fetch the environment variable "\[variable\]" which is not set. In order for this execution to succeed it must be set in this environment.** | Surfacing when a run is launched in the UI, this error means that an environment variable set using [`StringSource`](https://docs.dagster.io/api/dagster/config#dagster.StringSource) could not be found in the executing environment. | Verify that the environment variable is named correctly and accessible in the environment. - **If developing locally and using a `.env` file**, try reloading the workspace in the UI. The workspace must be reloaded any time this file is modified for the UI to be aware of the changes. - **If using Dagster+**: - Verify that the environment variable is [scoped to the environment and code location](https://docs.dagster.io/deployment/dagster-plus/management/environment-variables/dagster-ui#scope) if using the built-in secrets manager 	- Verify that the environment variable was correctly configured and added to your [agent's configuration](https://docs.dagster.io/deployment/dagster-plus/management/environment-variables/agent-config) |
| **No environment variables in `.env` file.** | Dagster located and attempted to load a local `.env` file while launching `dagster-webserver`, but couldn't find any environment variables in the file. | If this is unexpected, verify that your `.env` is correctly formatted and located in the same folder where you're running `dagster-webserver`. |

Ask Dagster AI