---
title: "postgresql (dagster-postgres) | Dagster Docs"
source: "https://docs.dagster.io/api/libraries/dagster-postgres"
author:
published:
created: 2025-12-12
description: "postgresql (dagster-postgres) Dagster API | Comprehensive Python API documentation for Dagster, the data orchestration platform. Learn how to build, test, and maintain data pipelines with our detailed guides and examples."
tags:
  - "clippings"
---
dagster\_postgres.PostgresEventLogStorage `=` <class 'dagster\_postgres.event\_log.event\_log.PostgresEventLogStorage'>

Postgres-backed event log storage.

Users should not directly instantiate this class; it is instantiated by internal machinery when `dagster-webserver` and `dagster-graphql` load, based on the values in the `dagster.yaml` file in `$DAGSTER_HOME`. Configuration of this class should be done by setting values in that file.

To use Postgres for all of the components of your instance storage, you can add the following block to your `dagster.yaml`:

dagster.yaml

```yaml
storage:
  postgres:
    postgres_db:
      username: my_username
      password: my_password
      hostname: my_hostname
      db_name: my_database
      port: 5432
```

If you are configuring the different storage components separately and are specifically configuring your event log storage to use Postgres, you can add a block such as the following to your `dagster.yaml`:

dagster.yaml

```yaml
event_log_storage:
  module: dagster_postgres.event_log
  class: PostgresEventLogStorage
  config:
    postgres_db:
      username: { username }
      password: { password }
      hostname: { hostname }
      db_name: { db_name }
      port: { port }
```

Note that the fields in this config are [`StringSource`](https://docs.dagster.io/api/dagster/config#dagster.StringSource) and [`IntSource`](https://docs.dagster.io/api/dagster/config#dagster.IntSource) and can be configured from environment variables.

dagster\_postgres.PostgresRunStorage `=` <class 'dagster\_postgres.run\_storage.run\_storage.PostgresRunStorage'>

Postgres-backed run storage.

Users should not directly instantiate this class; it is instantiated by internal machinery when `dagster-webserver` and `dagster-graphql` load, based on the values in the `dagster.yaml` file in `$DAGSTER_HOME`. Configuration of this class should be done by setting values in that file.

To use Postgres for all of the components of your instance storage, you can add the following block to your `dagster.yaml`:

dagster.yaml

```yaml
storage:
  postgres:
    postgres_db:
      username: my_username
      password: my_password
      hostname: my_hostname
      db_name: my_database
      port: 5432
```

If you are configuring the different storage components separately and are specifically configuring your run storage to use Postgres, you can add a block such as the following to your `dagster.yaml`:

dagster.yaml

```yaml
run_storage:
  module: dagster_postgres.run_storage
  class: PostgresRunStorage
  config:
    postgres_db:
      username: { username }
      password: { password }
      hostname: { hostname }
      db_name: { db_name }
      port: { port }
```

Note that the fields in this config are [`StringSource`](https://docs.dagster.io/api/dagster/config#dagster.StringSource) and [`IntSource`](https://docs.dagster.io/api/dagster/config#dagster.IntSource) and can be configured from environment variables.

dagster\_postgres.PostgresScheduleStorage `=` <class 'dagster\_postgres.schedule\_storage.schedule\_storage.PostgresScheduleStorage'>

Postgres-backed run storage.

Users should not directly instantiate this class; it is instantiated by internal machinery when `dagster-webserver` and `dagster-graphql` load, based on the values in the `dagster.yaml` file in `$DAGSTER_HOME`. Configuration of this class should be done by setting values in that file.

To use Postgres for all of the components of your instance storage, you can add the following block to your `dagster.yaml`:

dagster.yaml

```yaml
storage:
  postgres:
    postgres_db:
      username: my_username
      password: my_password
      hostname: my_hostname
      db_name: my_database
      port: 5432
```

If you are configuring the different storage components separately and are specifically configuring your schedule storage to use Postgres, you can add a block such as the following to your `dagster.yaml`:

dagster.yaml

```yaml
schedule_storage:
  module: dagster_postgres.schedule_storage
  class: PostgresScheduleStorage
  config:
    postgres_db:
      username: { username }
      password: { password }
      hostname: { hostname }
      db_name: { db_name }
      port: { port }
```

Note that the fields in this config are [`StringSource`](https://docs.dagster.io/api/dagster/config#dagster.StringSource) and [`IntSource`](https://docs.dagster.io/api/dagster/config#dagster.IntSource) and can be configured from environment variables.