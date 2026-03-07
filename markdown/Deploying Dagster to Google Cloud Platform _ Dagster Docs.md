---
title: "Deploying Dagster to Google Cloud Platform | Dagster Docs"
source: "https://docs.dagster.io/deployment/oss/deployment-options/gcp"
author:
published:
created: 2025-12-30
description: "To deploy open source Dagster to GCP, Google Compute Engine (GCE) can host the Dagster webserver, Google Cloud SQL can store runs and events, and Google Cloud Storage (GCS) can act as an IO manager."
tags:
  - "clippings"
---
To deploy Dagster to GCP, Google Compute Engine (GCE) can host the Dagster webserver, Google Cloud SQL can store runs and events, and Google Cloud Storage (GCS) can act as an IO manager.

## Hosting the Dagster webserver or Dagster Daemon on GCE

To host the Dagster webserver or Dagster daemon on a bare VM or in Docker on GCE, see [Running Dagster as a service](https://docs.dagster.io/deployment/oss/deployment-options/deploying-dagster-as-a-service).

## Using Cloud SQL for run and event log storage

We recommend launching a Cloud SQL PostgreSQL instance for run and events data. You can configure the webserver to use Cloud SQL to run and events data by setting blocks in your `$DAGSTER_HOME/dagster.yaml` appropriately:

```python
storage:
  postgres:
    postgres_db:
      username: my_username
      password: my_password
      hostname: my_hostname
      db_name: my_database
      port: 5432
```

In this case, you'll want to ensure you provide the right connection strings for your Cloud SQL instance, and that the node or container hosting the webserver is able to connect to Cloud SQL.

Be sure that this file is present, and `_DAGSTER_HOME_` is set, on the node where the webserver is running.

Note that using Cloud SQL for run and event log storage does not require that the webserver be running in the cloud. If you are connecting a local webserver instance to a remote Cloud SQL storage, double check that your local node is able to connect to Cloud SQL.

## Using GCS for IO Management

You'll probably also want to configure a GCS bucket to store op outputs via persistent [IO Managers](https://docs.dagster.io/guides/build/io-managers). This enables reexecution, review and audit of op outputs, and cross-node cooperation (e.g., with the [`multiprocess_executor`](https://docs.dagster.io/api/dagster/execution#dagster.multiprocess_executor) or [`celery_executor`](https://docs.dagster.io/api/libraries/dagster-celery#dagster_celery.celery_executor)).

You'll first need to create a job using [`gcs_pickle_io_manager`](https://docs.dagster.io/api/libraries/dagster-gcp#dagster_gcp.gcs_pickle_io_manager) as its IO Manager (or [define a custom IO Manager](https://docs.dagster.io/guides/build/io-managers/defining-a-custom-io-manager)):

```python
from dagster_gcp.gcs.io_manager import gcs_pickle_io_manager
from dagster_gcp.gcs.resources import gcs_resource

import dagster as dg

@dg.job(
    resource_defs={
        "gcs": gcs_resource,
        "io_manager": gcs_pickle_io_manager,
    },
    config={
        "resources": {
            "io_manager": {
                "config": {
                    "gcs_bucket": "my-cool-bucket",
                    "gcs_prefix": "good/prefix-for-files-",
                }
            }
        }
    },
)
def gcs_job(): ...
```

With this in place, your job runs will store outputs on GCS in the location `gs://<bucket>/dagster/storage/<job run id>/files/<op name>.compute`.