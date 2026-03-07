---
title: "graphql (dagster-graphql) | Dagster Docs"
source: "https://docs.dagster.io/api/libraries/dagster-graphql"
author:
published:
created: 2025-12-12
description: "graphql (dagster-graphql) Dagster API | Comprehensive Python API documentation for Dagster, the data orchestration platform. Learn how to build, test, and maintain data pipelines with our detailed guides and examples."
tags:
  - "clippings"
---
## Python Client

`class` dagster\_graphql.DagsterGraphQLClient [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/dagster-graphql/dagster_graphql/client/client.py#L38)

Official Dagster Python Client for GraphQL.

Utilizes the gql library to dispatch queries over HTTP to a remote Dagster GraphQL Server

As of now, all operations on this client are synchronous.

Intended usage:

```python
client = DagsterGraphQLClient("localhost", port_number=3000)
status = client.get_run_status(**SOME_RUN_ID**)
```

Parameters:

- **hostname** (*str*) – Hostname for the Dagster GraphQL API, like localhost or YOUR\_ORG\_HERE.dagster.cloud.
- **port\_number** (*Optional* *\[**int**\]*) – Port number to connect to on the host. Defaults to None.
- **transport** (*Optional* *\[**Transport**\]**,* *optional*) – A custom transport to use to connect to the GraphQL API with (e.g. for custom auth). Defaults to None.
- **use\_https** (*bool**,* *optional*) – Whether to use https in the URL connection string for the GraphQL API. Defaults to False.
- **timeout** (*int*) – Number of seconds before requests should time out. Defaults to 60.
- **headers** (*Optional* *\[**Dict* *\[**str**,* *str**\]**\]*) – Additional headers to include in the request. To use this client in Dagster Cloud, set the “Dagster-Cloud-Api-Token” header to a user token generated in the Dagster Cloud UI.

Raises: **ConnectionError** – if the client cannot connect to the host.

get\_run\_status [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/dagster-graphql/dagster_graphql/client/client.py#L302)

Get the status of a given Pipeline Run.

Parameters: **run\_id** (*str*) – run id of the requested pipeline run.Raises:

- [DagsterGraphQLClientError](https://docs.dagster.io/api/libraries/#dagster_graphql.DagsterGraphQLClientError) **DagsterGraphQLClientError** **(****"PipelineNotFoundError"****,** **message****)** – if the requested run id is not found
- [DagsterGraphQLClientError](https://docs.dagster.io/api/libraries/#dagster_graphql.DagsterGraphQLClientError) **DagsterGraphQLClientError** **(****"PythonError"****,** **message****)** – on internal framework errors

Returns: returns a status Enum describing the state of the requested pipeline runReturn type: [DagsterRunStatus](https://docs.dagster.io/api/dagster/internals#dagster.DagsterRunStatus)

reload\_repository\_location [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/dagster-graphql/dagster_graphql/client/client.py#L328)

Reloads a Dagster Repository Location, which reloads all repositories in that repository location.

This is useful in a variety of contexts, including refreshing the Dagster UI without restarting the server.

Parameters: **repository\_location\_name** (*str*) – The name of the repository locationReturns: Object with information about the result of the reload requestReturn type: [ReloadRepositoryLocationInfo](https://docs.dagster.io/api/libraries/#dagster_graphql.ReloadRepositoryLocationInfo)

shutdown\_repository\_location [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/dagster-graphql/dagster_graphql/client/client.py#L370)

Shuts down the server that is serving metadata for the provided repository location.

This is primarily useful when you want the server to be restarted by the compute environment in which it is running (for example, in Kubernetes, the pod in which the server is running will automatically restart when the server is shut down, and the repository metadata will be reloaded)

Parameters: **repository\_location\_name** (*str*) – The name of the repository locationReturns: Object with information about the result of the reload requestReturn type: ShutdownRepositoryLocationInfo

submit\_job\_execution [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/dagster-graphql/dagster_graphql/client/client.py#L245)

Submits a job with attached configuration for execution.

Parameters:

- **job\_name** (*str*) – The job’s name
- **repository\_location\_name** (*Optional* *\[**str**\]*) – The name of the repository location where the job is located. If omitted, the client will try to infer the repository location from the available options on the Dagster deployment. Defaults to None.
- **repository\_name** (*Optional* *\[**str**\]*) – The name of the repository where the job is located. If omitted, the client will try to infer the repository from the available options on the Dagster deployment. Defaults to None.
- **run\_config** (*Optional* *\[**Union* *\[*[*RunConfig*](https://docs.dagster.io/api/dagster/config#dagster.RunConfig)*,* *Mapping* *\[**str**,* *Any**\]**\]**\]*) – This is the run config to execute the job with. Note that runConfigData is any-typed in the GraphQL type system. This type is used when passing in an arbitrary object for run config. However, it must conform to the constraints of the config schema for this job. If it does not, the client will throw a DagsterGraphQLClientError with a message of JobConfigValidationInvalid. Defaults to None.
- **tags** (*Optional* *\[**Dict* *\[**str**,* *Any**\]**\]*) – A set of tags to add to the job execution.
- **op\_selection** (*Optional* *\[**Sequence* *\[**str**\]**\]*) – A list of ops to execute.
- **asset\_selection** (*Optional* *\[**Sequence* *\[**CoercibleToAssetKey**\]**\]*) – A list of asset keys to execute.

Raises:

- [DagsterGraphQLClientError](https://docs.dagster.io/api/libraries/#dagster_graphql.DagsterGraphQLClientError) **DagsterGraphQLClientError** **(****"InvalidStepError"****,** **invalid\_step\_key****)** – the job has an invalid step
- [DagsterGraphQLClientError](https://docs.dagster.io/api/libraries/#dagster_graphql.DagsterGraphQLClientError) **DagsterGraphQLClientError** **(****"InvalidOutputError"****,** **body=error\_object****)** – some solid has an invalid output within the job. The error\_object is of type dagster\_graphql.InvalidOutputErrorInfo.
- [DagsterGraphQLClientError](https://docs.dagster.io/api/libraries/#dagster_graphql.DagsterGraphQLClientError) **DagsterGraphQLClientError** **(****"RunConflict"****,** **message****)** – a DagsterRunConflict occured during execution. This indicates that a conflicting job run already exists in run storage.
- [DagsterGraphQLClientError](https://docs.dagster.io/api/libraries/#dagster_graphql.DagsterGraphQLClientError) **DagsterGraphQLClientError** **(****"PipelineConfigurationInvalid"****,** **invalid\_step\_key****)** – the run\_config is not in the expected format for the job
- [DagsterGraphQLClientError](https://docs.dagster.io/api/libraries/#dagster_graphql.DagsterGraphQLClientError) **DagsterGraphQLClientError** **(****"JobNotFoundError"****,** **message****)** – the requested job does not exist
- [DagsterGraphQLClientError](https://docs.dagster.io/api/libraries/#dagster_graphql.DagsterGraphQLClientError) **DagsterGraphQLClientError** **(****"PythonError"****,** **message****)** – an internal framework error occurred

Returns: run id of the submitted pipeline runReturn type: str

`exception` dagster\_graphql.DagsterGraphQLClientError [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/dagster-graphql/dagster_graphql/client/utils.py#L5)

`class` dagster\_graphql.InvalidOutputErrorInfo [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/dagster-graphql/dagster_graphql/client/utils.py#L78)

This class gives information about an InvalidOutputError from submitting a pipeline for execution from GraphQL.

Parameters:

- **step\_key** (*str*) – key of the step that failed
- **invalid\_output\_name** (*str*) – the name of the invalid output from the given step

`class` dagster\_graphql.ReloadRepositoryLocationInfo [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/dagster-graphql/dagster_graphql/client/utils.py#L28)

This class gives information about the result of reloading a Dagster repository location with a GraphQL mutation.

Parameters:

- **status** ([*ReloadRepositoryLocationStatus*](https://docs.dagster.io/api/libraries/#dagster_graphql.ReloadRepositoryLocationStatus)) – The status of the reload repository location mutation
- **failure\_type** – (Optional\[str\], optional): the failure type if status == ReloadRepositoryLocationStatus.FAILURE. Can be one of ReloadNotSupported, RepositoryLocationNotFound, or RepositoryLocationLoadFailure. Defaults to None.
- **message** (*Optional* *\[**str**\]**,* *optional*) – the failure message/reason if status == ReloadRepositoryLocationStatus.FAILURE. Defaults to None.

`class` dagster\_graphql.ReloadRepositoryLocationStatus [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/dagster-graphql/dagster_graphql/client/utils.py#L11)

This enum describes the status of a GraphQL mutation to reload a Dagster repository location.

Parameters: **Enum** (*str*) – can be either ReloadRepositoryLocationStatus.SUCCESS or ReloadRepositoryLocationStatus.FAILURE.