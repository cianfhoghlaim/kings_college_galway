---
title: "Manage concurrency of Dagster assets, jobs, and Dagster instances | Dagster Docs"
source: "https://docs.dagster.io/guides/operate/managing-concurrency"
author:
published:
created: 2025-12-12
description: "How to limit the number of runs a job, or assets for an instance of Dagster."
tags:
  - "clippings"
---
This guide covers managing concurrency of Dagster assets, jobs, and Dagster instances to help prevent performance problems and downtime.

## Limit the number of total runs that can be in progress at the same time

- Dagster Core, add the following to your [dagster.yaml](https://docs.dagster.io/deployment/oss/dagster-yaml)
- In Dagster+, add the following to your [full deployment settings](https://docs.dagster.io/deployment/dagster-plus/deploying-code/full-deployments/full-deployment-settings-reference)
```yaml
concurrency:
  runs:
    max_concurrent_runs: 15
```

## Limit the number of assets or ops actively executing across all runs

You can assign assets and ops to concurrency pools which allow you to limit the number of in progress op executions across all runs. You first assign your asset or op to a concurrency pool using the `pool` keyword argument.

```python
src/<project_name>/defs/assets.pyimport dagster as dg

@dg.asset(pool="foo")
def my_asset():
    pass

@dg.op(pool="bar")
def my_op():
    pass

@dg.op(pool="barbar")
def my_downstream_op(inp):
    return inp

@dg.graph_asset
def my_graph_asset():
    return my_downstream_op(my_op())
```

You should be able to verify that you have set the pool correctly by viewing the details pane for the asset or op in the Dagster UI.

![Viewing the pool tag](https://docs.dagster.io/assets/images/asset-pool-tag-fd5900b211765e971bd379b0395edc67.png)

Once you have assigned your assets and ops to a concurrency pool, you can configure a pool limit for that pool in your deployment by using the [Dagster UI](https://docs.dagster.io/guides/operate/webserver) or the [`dagster` CLI](https://docs.dagster.io/api/clis/cli).

To specify a limit for the pool "database" using the UI, navigate to the `Deployments` → `Concurrency` settings page and click the `Add pool limit` button:

![Setting the pool limit](https://docs.dagster.io/assets/images/add-pool-ui-202927770b0febe9c7129b682a0ac3e6.png)

To specify a limit for the pool "database" using the `dagster` CLI, use:

```markdown
dagster instance concurrency set database 1
```

## Limit the number of runs that can be in progress for a set of ops

You can also use concurrency pools to limit the number of in progress runs containing those assets or ops. You can follow the steps in the [Limit the number of assets or ops actively in execution across all runs](https://docs.dagster.io/guides/operate/#limit-the-number-of-assets-or-ops-actively-executing-across-all-runs) section to assign your assets and ops to pools and to configure the desired limit.

Once you have assigned your assets and ops to your pool, you can change your deployment settings to set the pool enforcement granularity. To limit the total number of runs containing a specific op at any given time (instead of the total number of ops actively executing), we need to set the pool granularity to `run`.

- Dagster Core, add the following to your [dagster.yaml](https://docs.dagster.io/deployment/oss/dagster-yaml)
- In Dagster+, add the following to your [deployment settings](https://docs.dagster.io/deployment/dagster-plus/deploying-code/full-deployments/full-deployment-settings-reference)
```yaml
concurrency:
  pools:
    granularity: 'run'
```

Without this granularity set, the default granularity is set to the `op`. This means that for a pool `foo` with a limit `1`, we enforce that only one op is executing at a given time across all runs, but the number of runs in progress is unaffected by the pool limit.

### Setting a default limit for concurrency pools

- Dagster+: Edit the `concurrency` config in deployment settings via the [Dagster+ UI](https://docs.dagster.io/guides/operate/webserver) or the [`dagster-cloud` CLI](https://docs.dagster.io/api/clis/dagster-cloud-cli).
- Dagster Open Source: Use your instance's [dagster.yaml](https://docs.dagster.io/deployment/oss/dagster-yaml)
```yaml
concurrency:
  pools:
    default_limit: 1
```

## Limit the number of runs that can be in progress by run tag

You can also limit the number of in progress runs by run tag. This is useful for limiting sets of runs independent of which assets or ops it is executing. For example, you might want to limit the number of in-progress runs for a particular schedule. Or, you might want to limit the number of in-progress runs for all backfills.

```yaml
concurrency:
  runs:
    tag_concurrency_limits:
      - key: 'dagster/sensor_name'
        value: 'my_cool_sensor'
        limit: 5
      - key: 'dagster/backfill'
        limit: 10
```

### Limit the number of runs that can be in progress by unique tag value

To apply separate limits to each unique value of a run tag, set a limit for each unique value using `applyLimitPerUniqueValue`. For example, instead of limiting the number of backfill runs across all backfills, you may want to limit the number of runs for each backfill in progress:

```yaml
concurrency:
  runs:
    tag_concurrency_limits:
      - key: 'dagster/backfill'
        value:
          applyLimitPerUniqueValue: true
        limit: 10
```

## Limit the number of ops concurrently executing for a single run

While pool limits allow you to [limit the number of ops executing across all runs](https://docs.dagster.io/guides/operate/#limit-the-number-of-assets-or-ops-actively-executing-across-all-runs), to limit the number of ops executing *within a single run*, you need to configure your [run executor](https://docs.dagster.io/guides/operate/run-executors). You can limit concurrency for ops and assets in runs, by using `max_concurrent` in the run config, either in Python or using the Launchpad in the Dagster UI.

### Limit concurrent execution for a specific job

```python
src/<project_name>/defs/assets.pyimport time

import dagster as dg

@dg.asset
def first_asset(context: dg.AssetExecutionContext):
    time.sleep(75)
    context.log.info("first asset executing")

@dg.asset
def second_asset(context: dg.AssetExecutionContext):
    time.sleep(75)
    context.log.info("second asset executing")

@dg.asset
def third_asset(context: dg.AssetExecutionContext):
    time.sleep(75)
    context.log.info("third asset executing")

# limits concurrent asset execution for \`my_job\` runs to 2, overrides the limit set on the Definitions object
my_job = dg.define_asset_job(
    name="my_job",
    selection=[first_asset, second_asset, third_asset],
    executor_def=dg.multiprocess_executor.configured({"max_concurrent": 2}),
)
```

### Limit concurrent execution for all runs in a code location

```python
src/<project_name>/defs/executor.pyimport dagster as dg

@dg.definitions
def executor() -> dg.Definitions:
    return dg.Definitions(
        executor=dg.multiprocess_executor.configured({"max_concurrent": 4})
    )
```

## Prevent runs from starting if another run is already occurring (advanced)

You can use Dagster's rich metadata to use a schedule or a sensor to only start a run when there are no currently running jobs.

```python
src/<project_name>/defs/assets.pyimport time

import dagster as dg

@dg.asset
def first_asset(context: dg.AssetExecutionContext):
    # sleep so that the asset takes some time to execute
    time.sleep(75)
    context.log.info("First asset executing")

my_job = dg.define_asset_job("my_job", [first_asset])

@dg.schedule(
    job=my_job,
    # Runs every minute to show the effect of the concurrency limit
    cron_schedule="* * * * *",
)
def my_schedule(context):
    # Find runs of the same job that are currently running
    run_records = context.instance.get_run_records(
        dg.RunsFilter(
            job_name="my_job",
            statuses=[
                dg.DagsterRunStatus.QUEUED,
                dg.DagsterRunStatus.NOT_STARTED,
                dg.DagsterRunStatus.STARTING,
                dg.DagsterRunStatus.STARTED,
            ],
        )
    )
    # skip a schedule run if another run of the same job is already running
    if len(run_records) > 0:
        return dg.SkipReason(
            "Skipping this run because another run of the same job is already running"
        )
    return dg.RunRequest()
```

## Troubleshooting

When limiting concurrency, you might run into some issues until you get the configuration right.

### Runs going to STARTED status and skipping QUEUED

If you are running a version older than `1.10.0`, you may need to manually configure your deployment to enable run queueing by setting the `run_queue` key in your instance's settings. In the Dagster UI, navigate to **Deployment > Configuration** and verify that the `run_queue` key is set.

### Runs remaining in QUEUED status

The possible causes for runs remaining in `QUEUED` status depend on whether you're using Dagster+ or Dagster Open Source.

Ask Dagster AI