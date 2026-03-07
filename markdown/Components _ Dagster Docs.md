---
title: "Components | Dagster Docs"
source: "https://docs.dagster.io/dagster-basics-tutorial/custom-components"
author:
published:
created: 2025-12-12
description: "Defining custom components"
tags:
  - "clippings"
---
So far, we have built our pipeline out of existing `Definitions`, but this is not the only way to include `Definitions` in a Dagster project.

If we think about the code for our three assets (`customers`, `orders`, and `payments`), it is all very similar. Each asset performs the same action, turning an S3 file into a DuckDB table, while differing only in the URL path and table name.

These assets are a great candidate for a [custom component](https://docs.dagster.io/guides/build/components/creating-new-components). Components generate `Definitions` through a configuration layer. There are built-in components that let you integrate with common workflows (such as turning Python scripts into assets), or dynamically generate `Definitions` for tools like [dbt](https://docs.dagster.io/integrations/libraries/dbt) or [Fivetran](https://docs.dagster.io/integrations/libraries/fivetran). With custom components, you can define your own specific use cases.

In this step, you will use a custom component to streamline the development of similar assets and replace their `Definitions` in your project with a Component that can generate them from a a YAML configuration file instead.

![2048 resolution](https://docs.dagster.io/assets/images/components-f6b7b1846953643053de87560a4a6583.png)

## 1\. Scaffold a custom component

First, scaffold a custom component using `dg`:

```markdown
dg scaffold component Tutorial
```

```markdown
Creating module at: <YOUR PATH>/dagster-tutorial/src/dagster_tutorial/components
Scaffolded Dagster component at <YOUR PATH>/dagster-tutorial/src/dagster_tutorial/components/tutorial.py.
```

This adds a new directory, `components`, within `src/dagster_tutorial`:

```markdown
src
└── dagster_tutorial
    ├── __init__.py
    ├── components   # NEW
    │   ├── __init__.py
    │   └── tutorial.py
    ├── definitions.py
    └── defs
        ├── __init__.py
        ├── assets.py
        ├── resources.py
        └── schedules.py
```

This directory contains the files needed to define the custom component.

## 2\. Define the custom component

When designing a component, keep its interface in mind. In this case, the assets that the component will create share the following attributes:

- A DuckDB database shared across all assets.
- A list of ETL assets, each with a URL path and a table name.

The first step is to create a `dg.Model` for the ETL assets. `dg.Model` turns any class that inherits from it into a [Pydantic](https://docs.pydantic.dev/) model. This model is then used to implement the YAML interface from the component.

This model will contain the two attributes that define an asset:

```python
src/etl_tutorial/components/tutorial.pyimport dagster as dg

class ETL(dg.Model):
    url_path: str
    table: str
```

Next, add the interface to the `dg.Component` class. In this case, there will be a single attribute for the DuckDB database and a list of the `ETL` models you just defined:

```python
duckdb_database: str
    etl_steps: list[ETL]
```

The rest of the code will look very similar to the asset definitions you wrote earlier. The `build_defs` method constructs a `Definitions` object containing all the Dagster objects created by the component. Based on the interface defined at the class level, you will generate multiple ETL assets. The final Dagster object to include is the `resource` that the assets rely on, which can also be set with an attribute.

```python
src/etl_tutorial/components/tutorial.pyfrom dagster_duckdb import DuckDBResource

class Tutorial(dg.Component, dg.Model, dg.Resolvable):
    # The interface for the component
    duckdb_database: str
    etl_steps: list[ETL]

    def build_defs(self, context: dg.ComponentLoadContext) -> dg.Definitions:
        _etl_assets = []

        for etl in self.etl_steps:

            @dg.asset(
                name=etl.table,
            )
            def _table(duckdb: DuckDBResource):
                with duckdb.get_connection() as conn:
                    conn.execute(
                        f"""
                        create or replace table {etl.table} as (
                            select * from read_csv_auto('{etl.url_path}')
                        )
                        """
                    )

            _etl_assets.append(_table)

        return dg.Definitions(
            assets=_etl_assets,
            resources={"duckdb": DuckDBResource(database=self.duckdb_database)},
        )
```

Run the check again to ensure that the component code is correct:

```markdown
dg check defs
```

```markdown
All component YAML validated successfully.
All definitions loaded successfully.
```

## 3\. Scaffold the component definition

If you list your components again, you should see that the custom component is now registered:

```markdown
dg list components
```

```markdown
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ Key                                           ┃ Summary                                                                     ┃
┡━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╇━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┩
│ dagster.DefinitionsComponent                  │ An arbitrary set of Dagster definitions.                                    │
├───────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
│ dagster.DefsFolderComponent                   │ A component that represents a directory containing multiple Dagster         │
│                                               │ definition modules.                                                         │
├───────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
│ dagster.FunctionComponent                     │ Represents a Python function, alongside the set of assets or asset checks   │
│                                               │ that it is responsible for executing.                                       │
├───────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
│ dagster.PythonScriptComponent                 │ Represents a Python script, alongside the set of assets and asset checks    │
│                                               │ that it is responsible for executing.                                       │
├───────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
│ dagster.TemplatedSqlComponent                 │ A component which executes templated SQL from a string or file.             │
├───────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
│ dagster.UvRunComponent                        │ Represents a Python script, alongside the set of assets or asset checks     │
│                                               │ that it is responsible for executing.                                       │
├───────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
│ dagster_tutorial.components.tutorial.Tutorial │                                                                             │
└───────────────────────────────────────────────┴─────────────────────────────────────────────────────────────────────────────┘
```

You can now scaffold definitions from it just like any other component:

```markdown
dg scaffold defs dagster_tutorial.components.tutorial.Tutorial tutorial
```

```markdown
Creating defs at <YOUR PATH>/dagster-tutorial/src/dagster_tutorial/defs/tutorial.
```

This adds a new directory, `tutorials`, within `defs`:

```markdown
src
└── dagster_tutorial
    ├── __init__.py
    ├── components
    │   ├── __init__.py
    │   └── tutorial.py
    ├── definitions.py
    └── defs
        ├── __init__.py
        ├── assets.py
        ├── resources.py
        ├── schedules.py
        └── tutorial   # NEW
            └── defs.yaml
```

## 4\. Configure the component

To configure the component, update the YAML file created when you scaffolded a definition from the component:

```yaml
src/dagster_tutorial/defs/tutorial/defs.yamltype: dagster_tutorial.components.tutorial.Tutorial

attributes:
  duckdb_database: /tmp/jaffle_platform.duckdb
  etl_steps:
    - url_path: https://raw.githubusercontent.com/dbt-labs/jaffle-shop-classic/refs/heads/main/seeds/raw_customers.csv
      table: customers
    - url_path: https://raw.githubusercontent.com/dbt-labs/jaffle-shop-classic/refs/heads/main/seeds/raw_orders.csv
      table: orders
    - url_path: https://raw.githubusercontent.com/dbt-labs/jaffle-shop-classic/refs/heads/main/seeds/raw_payments.csv
      table: payments
```

## 5\. Remove the old definitions

Before running `dg check` again, remove the `customers`, `orders`, and `payments` assets from `assets.py` and the `resource.py` file. The component is now responsible for generating these objects (otherwise there will be duplicate keys in the asset lineage).

```markdown
src
└── dagster_tutorial
    ├── __init__.py
    ├── components
    │   ├── __init__.py
    │   └── tutorial.py
    ├── definitions.py
    └── defs
        ├── __init__.py
        ├── assets.py   # UPDATED
        ├── schedules.py
        └── tutorial
            └── defs.yaml
```

## 6\. Materialize the assets

When you materialize your assets in the Dagster UI at [http://127.0.0.1:3000/assets](http://127.0.0.1:3000/assets), you should see that the asset graph looks the same as before.

## Summary

Congratulations! You've just built a fully functional, end-to-end data pipeline. This is no small feat! You've laid the foundation for a scalable, maintainable, and observable data platform.

- Join our [Slack community](https://dagster.io/slack).
- Continue learning with [Dagster University](https://courses.dagster.io/) courses.
- Start a [free trial of Dagster+](https://dagster.cloud/signup) for your own project.

Ask Dagster AI