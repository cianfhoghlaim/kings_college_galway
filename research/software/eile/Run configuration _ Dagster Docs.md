---
title: "Run configuration | Dagster Docs"
source: "https://docs.dagster.io/guides/operate/configuration/run-configuration"
author:
published:
created: 2025-12-12
description: "Dagster Job run configuration allows providing parameters to jobs at the time they're executed."
tags:
  - "clippings"
---
When you launch a job that materializes, executes, or instantiates a configurable entity, such as an asset, op, or resource, you can provide *run configuration* for that entity. Within the function that defines the entity, you can access the passed-in configuration through the `config` parameter. Typically, the provided run configuration values correspond to a *configuration schema* attached to the asset, op, or resource definition. Dagster validates the run configuration against the schema and proceeds only if validation is successful.

A common use of configuration is for a [schedule](https://docs.dagster.io/guides/automate/schedules) or [sensor](https://docs.dagster.io/guides/automate/sensors) to provide configuration to the job run it is launching. For example, a daily schedule might provide the day it's running on to one of the assets as a config value, and that asset might use that config value to decide what day's data to read.

## Defining configurable parameters for an asset, op, or job

You can specify configurable parameters accepted by an asset, op, or job by defining a config model subclass of [`Config`](https://docs.dagster.io/api/dagster/config#dagster.Config) and a `config` parameter to the corresponding asset or op function. These config models utilize [Pydantic](https://docs.pydantic.dev/), a popular Python library for data validation and serialization.

During execution, the specified config is accessed within the body of the asset, op, or job with the `config` parameter.

These examples showcase the most basic config types that can be used. For more information on the set of config types Dagster supports, see [the advanced config types documentation](https://docs.dagster.io/guides/operate/configuration/advanced-config-types).

## Defining configurable parameters for a resource

Configurable parameters for a resource are defined by specifying attributes for a resource class, which subclasses [`ConfigurableResource`](https://docs.dagster.io/api/dagster/resources#dagster.ConfigurableResource). The below resource defines a configurable connection URL, which can be accessed in any methods defined on the resource:

```python
import dagster as dg

class Engine:
    def execute(self, query: str): ...

def get_engine(connection_url: str) -> Engine:
    return Engine()

class MyDatabaseResource(dg.ConfigurableResource):
    connection_url: str

    def query(self, query: str):
        return get_engine(self.connection_url).execute(query)

@dg.definitions
def resources() -> dg.Definitions:
    return dg.Definitions(
        resources={
            # To send a query to the database, you can call my_db_resource.query("QUERY HERE")
            # in the asset, op, or job where you reference my_db_resource
            "my_db_resource": MyDatabaseResource(connection_url="")
        }
    )
```

For more information on using resources, see the [External resources documentation](https://docs.dagster.io/guides/build/external-resources).

## Providing config values at runtime

To execute a job or materialize an asset that specifies config, you'll need to provide values for its parameters. How you provide these values depends on the interface you use: Python, the Dagster UI, or the command line (CLI).

## Using environment variables with config

Assets and ops can be configured using environment variables by passing an [`EnvVar`](https://docs.dagster.io/api/dagster/resources#dagster.EnvVar) when constructing a config object. This is useful when the value is sensitive or may vary based on environment. If using Dagster+, environment variables can be [set up directly in the UI](https://docs.dagster.io/guides/operate/configuration/using-environment-variables-and-secrets).

```python
src/<project_name>/defs/assets.pyimport dagster as dg

from .resources import MyAssetConfig

@dg.asset
def greeting(config: MyAssetConfig) -> str:
    return f"hello {config.person_name}"

asset_result = dg.materialize(
    [greeting],
    run_config=dg.RunConfig(
        {"greeting": MyAssetConfig(person_name=dg.EnvVar("PERSON_NAME"))}
    ),
)
```

For more information on using environment variables in Dagster, see [Using environment variables and secrets in Dagster code](https://docs.dagster.io/guides/operate/configuration/using-environment-variables-and-secrets).

## Validation

Dagster validates any provided run config against the corresponding Pydantic model. It will abort execution with a [`DagsterInvalidConfigError`](https://docs.dagster.io/api/dagster/errors#dagster.DagsterInvalidConfigError) or Pydantic `ValidationError` if validation fails. For example, both of the following will fail, because there is no `nonexistent_config_value` in the config schema:

```python
src/<project_name>/defs/assets.pyimport dagster as dg

from .resources import MyAssetConfig

@dg.asset
def greeting(config: MyAssetConfig) -> str:
    return f"hello {config.person_name}"

asset_result = dg.materialize(
    [greeting],
    run_config=dg.RunConfig({"greeting": MyAssetConfig(nonexistent_config_value=1)}),
)
```

Config is a powerful tool for making Dagster pipelines more flexible and observable. For a deeper dive into the supported config types, see the [advanced config types documentation](https://docs.dagster.io/guides/operate/configuration/advanced-config-types). For more information on using resources, which are a powerful way to encapsulate reusable logic, see the [resources documentation](https://docs.dagster.io/guides/build/external-resources).

Ask Dagster AI