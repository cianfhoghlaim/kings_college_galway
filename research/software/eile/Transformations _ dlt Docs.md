---
title: "Transformations | dlt Docs"
source: "https://dlthub.com/docs/hub/features/transformations"
author:
published:
created: 2025-12-17
description: "Define Python-based or mixed SQL + Python transformations on data that is **already** in your destination."
tags:
  - "clippings"
---
Version: 1.20.0 (latest)

`dlt transformations` let you build new tables or full datasets from datasets that have *already* been ingested with `dlt`. `dlt transformations` are written and run in a very similar fashion to dlt source and resources. `dlt transformations` require you to have loaded data to a location, for example a local duckdb database, a bucket or a warehouse on which the transformations may be executed. `dlt transformations` are fully supported for all of our sql destinations including all filesystem and bucket formats.

You create them with the `@dlt.hub.transformation` decorator, which has the same signature as the `@dlt.resource` decorator but yields a SQL query, including the resulting column schema, rather than data items. dlt transformations support the same write\_dispositions per destination as dlt resources do.

## Motivations

A few real-world scenarios where dlt transformations can be useful:

- **Build one-stop reporting tables** – Flatten and enrich raw data into a wide table that analysts can pivot, slice, and dice without writing SQL each time.
- **Clean data** – Remove irrelevant columns or anonymize sensitive information before sending it to a layer with lower privacy protections.
- **Normalize JSON into 3-NF** – Break out repeating attributes from nested JSON so updates are consistent and storage isn't wasted.
- **Create dimensional (star-schema) models** – Produce fact and dimension tables so BI users can drag-and-drop metrics and break them down by any dimension.
- **Generate task-specific feature sets** – Deliver slim tables tailored for personalization, forecasting, or other ML workflows.
- **Apply shared business definitions** – Encode rules such as "a *sale* is a transaction whose status became *paid* this month," ensuring every metric is counted the same way.
- **Merge heterogeneous sources** – Combine Shopify, Amazon, WooCommerce (etc.) into one canonical *orders* feed for unified inventory and revenue reporting.
- **Run transformations during ingestion pre-warehouse** – Pre-aggregate or pre-filter data before it hits the warehouse to cut compute and storage costs.
- **…and more** – Any scenario where reshaping, enriching, or aggregating existing data unlocks faster insight or cleaner downstream pipelines.

## Quick-start in three simple steps

For the example below, you can copy–paste everything into one script and run it.

### 1\. Load some example data

The snippets below assume that we have a simple fruitshop dataset as produced by the dlt fruitshop template:

```markdown
import dlt
from dlt.destinations import duckdb
from dlt._workspace._templates._single_file_templates.fruitshop_pipeline import (
    fruitshop as fruitshop_source,
)

fruitshop_pipeline = dlt.pipeline(
    "fruitshop", destination=duckdb("./test_duck.duckdb"), dev_mode=True
)
fruitshop_pipeline.run(fruitshop_source())
```

### 2\. Inspect the dataset

```markdown
# Show row counts for every table
print(fruitshop_pipeline.dataset().row_counts().df())
```

### 3\. Write and run a transformation

```markdown
from typing import Any

@dlt.hub.transformation
def copied_customers(dataset: dlt.Dataset) -> Any:
    customers_table = dataset["customers"]
    yield customers_table.order_by("name").limit(5)

# Same pipeline & same dataset
fruitshop_pipeline.run(copied_customers(fruitshop_pipeline.dataset()))

# show rowcounts again, we now have a new table in the schema and the destination
print(fruitshop_pipeline.dataset().row_counts().df())
```

### 3.1 Alternatively use pure SQL for the transformation

```markdown
# Convert the transformation above that selected the first 5 customers to a sql query
@dlt.hub.transformation
def copied_customers(dataset: dlt.Dataset) -> Any:
    customers_table = dataset(
        """
        SELECT *
        FROM customers
        ORDER BY name
        LIMIT 5
    """
    )
    yield customers_table
```

That's it — `copied_customers` is now a new table in **the same** DuckDB schema with the first 5 customers when ordered by name. `dlt` has detected that we are loading into the same dataset and executed this transformation in SQL - no data was transferred to and from the machine executing this pipeline. Additionally, the new destination table `copied_customers` was automatically evolved to the correct new schema, and you could also set a different write disposition and even merge data from a transformation.

## Defining a transformation

```markdown
@dlt.hub.transformation(name="orders_per_user", write_disposition="merge")
def orders_per_user(dataset: dlt.Dataset) -> Any:
    purchases = dataset.table("purchases").to_ibis()
    yield purchases.group_by(purchases.customer_id).aggregate(
        order_count=purchases.id.count()
    )
```
- **Decorator arguments** mirror those accepted by `@dlt.resource`.
- The transformation function signature must contain at least one `dlt.Dataset` which is used inside the function to create the transformation SQL statements and calculate the resulting schema update.
- A transformation yields a `Relation` created with ibis expressions or a select query which will be materialized into the destination table. If the first item yielded is a valid sql query or relation object, data will be interpreted as a transformation. In all other cases, the transformation decorator will work like any other resource.

Below we load to the same DuckDB instance with a new pipeline that points to another `dataset`. dlt will be able to detect that both datasets live on the same destination and will run the transformation as pure SQL.

```markdown
import dlt
from dlt.destinations import duckdb

@dlt.hub.transformation
def copied_customers(dataset: dlt.Dataset) -> Any:
    customers_table = dataset["customers"]
    yield customers_table.order_by("name").limit(5)

# Same duckdb instance, different dataset
dest_p = dlt.pipeline(
    "fruitshop_dataset",
    destination=duckdb("./test_duck.duckdb"),
    dataset_name="copied_dataset",
    dev_mode=True,
)
dest_p.run(copied_customers(fruitshop_pipeline.dataset()))
```

Below we load the data from our local DuckDB instance to a Postgres instance. dlt will use the query to extract the data as Parquet files and will do a regular dlt load, pushing the data to Postgres. Note that you can use the exact same transformation functions for both scenarios. This can be extremely useful when you want to avoid compute costs in warehouses by running transformations directly from a local duckdb instance or raw data in a bucket into the warehouse, as the compute will happen on the machine executing the pipeline that runs the transformations.

```markdown
# Different engine (Postgres → DuckDB)
duck_p = dlt.pipeline("fruitshop_warehouse", destination="postgres")
duck_p.run(copied_customers(fruitshop_pipeline.dataset()))
```

## Using transformations

### Grouping multiple transformations in a source

`dlt transformations` can be grouped like all other resources into sources and will be executed together. You can even mix regular resources and transformations in one pipeline load.

```markdown
import dlt

@dlt.source
def my_transformations(dataset: dlt.Dataset) -> Any:
    @dlt.hub.transformation(write_disposition="append")
    def enriched_purchases(dataset: dlt.Dataset) -> Any:
        purchases = dataset.table("purchases").to_ibis()
        customers = dataset.table("customers").to_ibis()
        yield purchases.join(customers, purchases.customer_id == customers.id)

    @dlt.hub.transformation(write_disposition="replace")
    def total_items_sold(dataset: dlt.Dataset) -> Any:
        purchases = dataset.table("purchases").to_ibis()
        yield purchases.aggregate(total_qty=purchases.quantity.sum())

    return enriched_purchases(dataset), total_items_sold(dataset)

fruitshop_pipeline.run(my_transformations(fruitshop_pipeline.dataset()))
```

### Yielding multiple transformations from one transformation resource

`dlt transformations` may also yield more than one transformation instruction. If no further table name hints are supplied, the result will be a union of the yielded transformation instructions. `dlt` will take care of the necessary schema migrations, you will just need to ensure that no columns are marked as non-nullable that are missing from one of the transformation instructions:

```markdown
import dlt

# this (probably nonsensical) transformation will create a union of the customers and purchases tables
@dlt.hub.transformation(write_disposition="append")
def union_of_tables(dataset: dlt.Dataset) -> Any:
    yield dataset.table("purchases")
    yield dataset.table("customers")
```

### Supplying additional hints

You may supply column and table hints the same way you do for regular resources. `dlt` will derive schema hints from your query, but in some cases you may need to modify or extend them — for example, making columns nullable as in the example above, or adjusting the precision or type of a column to ensure compatibility with a specific target destination (if it differs from the source).

```markdown
import dlt

# change precision and scale of the price column
@dlt.hub.transformation(
    write_disposition="append", columns={"price": {"precision": 10, "scale": 2}}
)
def precision_change(dataset: dlt.Dataset) -> Any:
    yield dataset.inventory
```

### Writing your queries in SQL

If you prefer to write your queries in SQL, you can omit ibis expressions by simply creating a `Relation` from a query on your dataset:

```markdown
# Convert the transformation above that selected the first 5 customers to a sql query
@dlt.hub.transformation
def copied_customers(dataset: dlt.Dataset) -> Any:
    customers_table = dataset(
        """
        SELECT *
        FROM customers
        ORDER BY name
        LIMIT 5
    """
    )
    yield customers_table

# Joins and other more complex queries are also possible
@dlt.hub.transformation
def enriched_purchases(dataset: dlt.Dataset) -> Any:
    enriched_purchases = dataset(
        """
        SELECT customers.name, purchases.quantity
        FROM purchases
        JOIN customers
            ON purchases.customer_id = customers.id
        """
    )
    yield enriched_purchases

# You can even use a different dialect than the one used by the destination by supplying the dialect parameter
# dlt will compile the query to the right destination dialect
@dlt.hub.transformation
def enriched_purchases_postgres(dataset: dlt.Dataset) -> Any:
    enriched_purchases = dataset(
        """
        SELECT customers.name, purchases.quantity
        FROM purchases
        JOIN customers
            ON purchases.customer_id = customers.id
        """,
        query_dialect="duckdb",
    )
    yield enriched_purchases
```

The identifiers (table and column names) used in these raw SQL expressions must correspond to the identifiers as they are present in your dlt schema, NOT in your destination database schema.

## Using pandas dataframes or arrow tables

You can also write transformations directly using pandas or arrow. Note that in this case your transformation resource behaves like a regular resource: column-level hints will not be propagated, and `dlt` will simply treat the yielded dataframes or arrow tables like data from any other resource. This behavior may change in the future.

```markdown
@dlt.hub.transformation
def copied_customers(dataset: dlt.Dataset) -> Any:
    # get full customers table as arrow table
    customers = dataset.table("customers").arrow()

    # Sort the table by 'name'
    sorted_customers = customers.sort_by([("name", "ascending")])

    # Take first 5 rows
    yield sorted_customers.slice(0, 5)

# Example tables (replace with your actual data)
@dlt.hub.transformation
def enriched_purchases(dataset: dlt.Dataset) -> Any:
    # get both fully tables as dataframes
    purchases = dataset.table("purchases").df()
    customers = dataset.table("customers").df()

    # Merge (JOIN) the DataFrames
    result = purchases.merge(customers, left_on="customer_id", right_on="id")

    # Select only the desired columns
    yield result[["name", "quantity"]]
```

## Schema evolution and hints lineage

When executing transformations, `dlt` computes the resulting schema before the transformation is executed. This allows `dlt` to:

1. Migrate the destination schema accordingly, creating new columns or tables as needed
2. Fail early if there are schema mismatches that cannot be resolved
3. Preserve column-level hints from source to destination

### Schema evolution

For example, if your transformation joins two tables and creates new columns, `dlt` will automatically update the destination schema to accommodate these changes. If your transformation would result in incompatible schema changes (like changing a column's data type in a way that could lose data), `dlt` will fail before executing the transformation, protecting your data and saving execution and debug time.

You can inspect the computed result schema during development by looking at the result of `compute_columns_schema` on your `Relation`:

```markdown
# Show the computed schema before the transformation is executed
dataset = fruitshop_pipeline.dataset()
purchases = dataset.table("purchases").to_ibis()
customers = dataset.table("customers").to_ibis()
enriched_purchases = purchases.join(
    customers, purchases.customer_id == customers.id
)
print(dataset(enriched_purchases).columns)
```

### Column level hint forwarding

When creating or updating tables with transformation resources, `dlt` will also forward certain column hints to the new tables. In our fruitshop source, we have applied a custom hint named `x-annotation-pii` set to True for the `name` column, which indicates that this column contains PII (personally identifiable information). Downstream of the transformation layer, we may want to know which columns originate from columns that contain private data:

```markdown
@dlt.hub.transformation
def enriched_purchases(dataset: dlt.Dataset) -> Any:
    enriched_purchases = dataset(
        """
        SELECT customers.name, purchases.quantity
        FROM purchases
        JOIN customers
            ON purchases.customer_id = customers.id
        """
    )
    yield enriched_purchases

# Let's run the transformation and see that the name column in the NEW table is also marked as PII
fruitshop_pipeline.run(enriched_purchases(fruitshop_pipeline.dataset()))
assert (
    fruitshop_pipeline.dataset().schema.tables["enriched_purchases"]["columns"][
        "name"
    ][
        "x-annotation-pii"  # type: ignore
    ]
    is True
)
```

#### Features and limitations:

- `dlt` will only forward certain types of hints to the resulting tables: custom hints starting with `x-annotation...` and type hints such as `nullable`, `data_type`, `precision`, `scale`, and `timezone`. Other hints, such as `primary_key` or `merge_keys`, will need to be set via the `columns` argument on the transformation decorator, since `dlt` does not know how the transformed tables will be used.
- `dlt` cannot forward hints for columns that result from combining multiple origin columns, such as when they are concatenated or produced through other SQL operations.

## Lifecycle of a SQL transformation

In this section, we focus on the lifecycle of transformations that yield a `Relation` object, which we call SQL transformations here. This is in contrast to Python-based transformations that yield dataframes or arrow tables, which go through the regular extract, normalize, and load lifecycle of a `dlt` resource.

### Extract

In the extract stage, a `Relation` yielded by a transformation is converted into a SQL string and saved as a `.model` file along with its source SQL dialect. At this stage, the SQL string is just the user's original query — either the string that was explicitly provided or the one generated by `Relation.to_sql()`. No `dlt` -specific columns like `_dlt_id` or `_dlt_load_id` are added yet.

### Normalize

In the normalize stage, `.model` files are read and processed. The normalization process modifies your SQL queries to ensure they execute correctly and integrate with `dlt` 's features.

#### Adding dlt columns

During normalization, `dlt` adds internal `dlt` columns to your SQL queries depending on the configuration:

- `_dlt_load_id`, which tracks which load operation created or modified each row, is **added by default**. Even if present in your query, the `_dlt_load_id` column will be **replaced with a constant value** corresponding to the current load ID. To disable this behavior, set:
	```toml
	[normalize.model_normalizer]
	add_dlt_load_id = false
	```
	In this case, the column will not be added or replaced.
- `_dlt_id`, a unique identifier for each row, is **not added by default**. If your query already includes a `_dlt_id` column, it will be left unchanged. To enable automatic generation of this column when it’s missing, set:
	```toml
	[normalize.model_normalizer]
	add_dlt_id = true
	```
	When enabled and the column is not in the query, dlt will generate a `_dlt_id`. Note that if the column is already present, it will **not** be replaced.
	The `_dlt_id` column is generated using the destination's UUID function, such as `generateUUIDv4()` in ClickHouse. For dialects without native UUID support:
	- In **Redshift**, `_dlt_id` is generated using an `MD5` hash of the load ID and row number.
	- In **SQLite**, `_dlt_id` is simulated using `lower(hex(randomblob(16)))`.

#### Query transformations

The normalization process also applies the following transformations to ensure your queries work correctly:

1. Fully qualifies all identifiers with database and dataset prefixes
2. Quotes and adjusts identifier casing to match destination requirements
3. Normalizes column names according to the selected naming convention
4. Aliases columns and tables to handle naming convention differences
5. Reorders columns to match the destination table schema
6. Fills in `NULL` values for columns that exist in the destination but aren't in your query

### Load

In the load stage, the normalized queries from `.model` files are wrapped in INSERT statements and executed on the destination. For example, given this query from the extract stage:

```sql
SELECT
    "my_table"."id" AS "id",
    "my_table"."value" AS "value"
FROM "my_pipeline_dataset"."my_table" AS "my_table"
```

After the normalize stage processes it (adding dlt columns, wrapping in subquery, etc.) and results in:

```sql
SELECT
    _dlt_subquery."id" AS "id",
    _dlt_subquery."value" AS "value",
    '1749134128.17655' AS "_dlt_load_id",
    UUID() AS "_dlt_id"
FROM (
    SELECT
        "my_table"."id" AS "id",
        "my_table"."value" AS "value"
    FROM "my_pipeline_dataset"."my_table" AS "my_table"
    )
AS _dlt_subquery
```

The load stage executes:

```sql
INSERT INTO
    "my_pipeline_dataset"."my_transformation" ("id", "value", "_dlt_load_id", "_dlt_id")
SELECT
    _dlt_subquery."id" AS "id",
    _dlt_subquery."value" AS "value",
    '1749134128.17655' AS "_dlt_load_id",
    UUID() AS "_dlt_id"
FROM (
    SELECT
        "my_table"."id" AS "id",
        "my_table"."value" AS "value"
    FROM "my_pipeline_dataset"."my_table" AS "my_table"
    )
AS _dlt_subquery
```

The query is executed via the destination's SQL client, materializing the transformation result directly in the database.

## Examples

### Local in-transit transformations example

If you require aggregated or otherwise transformed data in your warehouse, but would like to avoid or reduce the costs of running queries across many rows in your warehouse tables, you can run some or all of your transformations "in transit" while loading data from your source. The code below demonstrates how you can extract data with our `rest_api` source to a local DuckDB instance and then forward aggregated data to a warehouse destination.

```markdown
from dlt.sources.rest_api import (
    rest_api_source,
)

# loads some data from our example api at https://jaffle-shop.scalevector.ai/docs
source = rest_api_source(
    {
        "client": {
            "base_url": "https://jaffle-shop.scalevector.ai/api/v1",
        },
        "resources": [
            "stores",
            {
                "name": "orders",
                "endpoint": {
                    "path": "orders",
                    "params": {
                        "start_date": "2017-01-01",
                        "end_date": "2017-01-31",
                    },
                },
            },
        ],
    }
)

# load to a local DuckDB instance
transit_pipeline = dlt.pipeline(
    "jaffle_shop", destination="duckdb", dataset_name="in_transit"
)
transit_pipeline.run(source)

# load aggregated data to a warehouse destination
@dlt.hub.transformation
def orders_per_store(dataset: dlt.Dataset) -> Any:
    orders = dataset.table("orders").to_ibis()
    stores = dataset.table("stores").to_ibis()
    yield (
        orders.join(stores, orders.store_id == stores.id)
        .group_by(stores.name)
        .aggregate(order_count=orders.id.count())
    )

# load aggregated data to a warehouse destination
warehouse_pipeline = dlt.pipeline(
    "jaffle_warehouse",
    destination="postgres",
    dataset_name="warehouse",
    dev_mode=True,
)
warehouse_pipeline.run(orders_per_store(transit_pipeline.dataset()))
```

This script demonstrates:

- Fetching data from a REST API using dlt's rest\_api\_source
- Loading raw data into a local DuckDB instance as an intermediate step
- Transforming the data by joining orders with stores and aggregating order counts directly on the local DuckDB instance, not in the destination warehouse
- Loading only the aggregated results to a production warehouse (Postgres)
- Reducing warehouse compute costs by performing transformations locally in DuckDB
- Using multiple pipelines in a single workflow for different stages of processing

### Incremental transformations example

```markdown
from dlt.pipeline.exceptions import PipelineNeverRan

@dlt.hub.transformation(
    write_disposition="append",
    primary_key="id",
)
def cleaned_customers(dataset: dlt.Dataset) -> Any:
    # get newest primary key from the output dataset
    max_pimary_key = -1
    try:
        output_dataset = dlt.current.pipeline().dataset()
        if output_dataset.schema.tables.get("cleaned_customers"):
            max_pimary_key_expr = (
                output_dataset.table("cleaned_customers").to_ibis().id.max()
            )
            max_pimary_key = output_dataset(max_pimary_key_expr).fetchscalar()
    except PipelineNeverRan:
        # we get this exception if the destination dataset has not been run yet
        # so we can assume that all customers are new
        pass

    # return filtered transformation
    customers_table = dataset.table("customers").to_ibis()

    # filter only new customers and exclude the name column in the result
    yield customers_table.filter(customers_table.id > max_pimary_key).drop(
        customers_table.name
    )

# create a warehouse dataset, would ordinarily be snowflake or some other warehousing destination
warehouse_pipeline = dlt.pipeline(
    "warehouse", destination="duckdb", dataset_name="cleaned_customers"
)
warehouse_pipeline.run(cleaned_customers(fruitshop_pipeline.dataset()))

# new items get added to the input dataset
# ...

# run the transformation again, only new customers are processed and appended to the destination table
warehouse_pipeline.run(cleaned_customers(fruitshop_pipeline.dataset()))
```

This example demonstrates how you can incrementally transform incoming new data for the customers table into a cleaned\_customers table where the name column has been removed. It:

- Uses primary key-based incremental loading to process only new data
- Tracks the highest ID processed so far to filter out already processed records
- Handles first-time runs with the PipelineNeverRan exception
- Removes sensitive data (name column) during the transformation
- Uses write\_disposition="append" to add only new records to the destination table
- Can be run repeatedly as new data arrives, processing only the delta

This demo works on codespaces. Codespaces is a development environment available for free to anyone with a Github account. You'll be asked to fork the demo repository and from there the README guides you with further steps.

The demo uses the Continue VSCode extension.

  
[Off to codespaces!](https://github.com/codespaces/new/dlt-hub/dlt-llm-code-playground?ref=create-pipeline)

## DHelp

## Ask a question

Welcome to "Codex Central", your next-gen help center, driven by OpenAI's GPT-4 model. It's more than just a forum or a FAQ hub – it's a dynamic knowledge base where coders can find AI-assisted solutions to their pressing problems. With GPT-4's powerful comprehension and predictive abilities, Codex Central provides instantaneous issue resolution, insightful debugging, and personalized guidance. Get your code running smoothly with the unparalleled support at Codex Central - coding help reimagined with AI prowess.