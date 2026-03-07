---
title: "iceberg (dagster-iceberg) | Dagster Docs"
source: "https://docs.dagster.io/api/libraries/dagster-iceberg"
author:
published:
created: 2025-12-12
description: "iceberg (dagster-iceberg) Dagster API | Comprehensive Python API documentation for Dagster, the data orchestration platform. Learn how to build, test, and maintain data pipelines with our detailed guides and examples."
tags:
  - "clippings"
---
This library provides an integration with the [Iceberg](https://iceberg.apache.org/) table format.

For more information on getting started, see the [Dagster & Iceberg](https://docs.dagster.io/integrations/libraries/iceberg) documentation.

**Note:** This is a community-supported integration. For support, see the [Dagster Community Integrations repository](https://github.com/dagster-io/community-integrations/tree/main/libraries/dagster-iceberg).

## I/O Managers

dagster\_iceberg.io\_manager.arrow.PyArrowIcebergIOManager IOManagerDefinition [\[source\]](https://github.com/dagster-io/dagster/blob/master/docs/.tox/sphinx-mdx-vercel/lib/python3.11/site-packages/dagster_iceberg/io_manager/arrow.py#L56)

An I/O manager definition that reads inputs from and writes outputs to Iceberg tables using PyArrow.

Examples:

```python
import pandas as pd
import pyarrow as pa
from dagster import Definitions, asset
from dagster_iceberg.config import IcebergCatalogConfig
from dagster_iceberg.io_manager.arrow import PyArrowIcebergIOManager

CATALOG_URI = "sqlite:////home/vscode/workspace/.tmp/examples/select_columns/catalog.db"
CATALOG_WAREHOUSE = (
    "file:///home/vscode/workspace/.tmp/examples/select_columns/warehouse"
)

resources = {
    "io_manager": PyArrowIcebergIOManager(
        name="test",
        config=IcebergCatalogConfig(
            properties={"uri": CATALOG_URI, "warehouse": CATALOG_WAREHOUSE}
        ),
        namespace="dagster",
    )
}

@asset
def iris_dataset() -> pa.Table:
    pa.Table.from_pandas(
        pd.read_csv(
            "https://docs.dagster.io/assets/iris.csv",
            names=[
                "sepal_length_cm",
                "sepal_width_cm",
                "petal_length_cm",
                "petal_width_cm",
                "species",
            ],
        )
    )

defs = Definitions(assets=[iris_dataset], resources=resources)
```

If you do not provide a schema, Dagster will determine a schema based on the assets and ops using the I/O manager. For assets, the schema will be determined from the asset key, as in the above example. For ops, the schema can be specified by including a “schema” entry in output metadata. If none of these is provided, the schema will default to “public”. The I/O manager will check if the namespace exists in the Iceberg catalog. It does not automatically create the namespace if it does not exist.

```python
@op(
    out={"my_table": Out(metadata={"schema": "my_schema"})}
)
def make_my_table() -> pa.Table:
    ...
```

To only use specific columns of a table as input to a downstream op or asset, add the metadata “columns” to the `In` or `AssetIn`.

```python
@asset(
    ins={"my_table": AssetIn("my_table", metadata={"columns": ["a"]})}
)
def my_table_a(my_table: pa.Table):
    # my_table will just contain the data from column "a"
    ...
```

dagster\_iceberg.io\_manager.daft.DaftIcebergIOManager IOManagerDefinition [\[source\]](https://github.com/dagster-io/dagster/blob/master/docs/.tox/sphinx-mdx-vercel/lib/python3.11/site-packages/dagster_iceberg/io_manager/daft.py#L54)

An I/O manager definition that reads inputs from and writes outputs to Iceberg tables using Daft.

Examples:

```python
import daft as da
import pandas as pd
from dagster import Definitions, asset
from dagster_iceberg.config import IcebergCatalogConfig
from dagster_iceberg.io_manager.daft import DaftIcebergIOManager

CATALOG_URI = "sqlite:////home/vscode/workspace/.tmp/examples/select_columns/catalog.db"
CATALOG_WAREHOUSE = (
    "file:///home/vscode/workspace/.tmp/examples/select_columns/warehouse"
)

resources = {
    "io_manager": DaftIcebergIOManager(
        name="test",
        config=IcebergCatalogConfig(
            properties={"uri": CATALOG_URI, "warehouse": CATALOG_WAREHOUSE}
        ),
        namespace="dagster",
    )
}

@asset
def iris_dataset() -> da.DataFrame:
    return da.from_pandas(
        pd.read_csv(
            "https://docs.dagster.io/assets/iris.csv",
            names=[
                "sepal_length_cm",
                "sepal_width_cm",
                "petal_length_cm",
                "petal_width_cm",
                "species",
            ],
        )
    )

defs = Definitions(assets=[iris_dataset], resources=resources)
```

If you do not provide a schema, Dagster will determine a schema based on the assets and ops using the I/O manager. For assets, the schema will be determined from the asset key, as in the above example. For ops, the schema can be specified by including a “schema” entry in output metadata. If none of these is provided, the schema will default to “public”. The I/O manager will check if the namespace exists in the Iceberg catalog. It does not automatically create the namespace if it does not exist.

```python
@op(
    out={"my_table": Out(metadata={"schema": "my_schema"})}
)
def make_my_table() -> da.DataFrame:
    ...
```

To only use specific columns of a table as input to a downstream op or asset, add the metadata “columns” to the `In` or `AssetIn`.

```python
@asset(
    ins={"my_table": AssetIn("my_table", metadata={"columns": ["a"]})}
)
def my_table_a(my_table: da.DataFrame):
    # my_table will just contain the data from column "a"
    ...
```

dagster\_iceberg.io\_manager.pandas.PandasIcebergIOManager IOManagerDefinition [\[source\]](https://github.com/dagster-io/dagster/blob/master/docs/.tox/sphinx-mdx-vercel/lib/python3.11/site-packages/dagster_iceberg/io_manager/pandas.py#L43)

An I/O manager definition that reads inputs from and writes outputs to Iceberg tables using pandas.

Examples:

```python
import pandas as pd
from dagster import Definitions, asset
from dagster_iceberg.config import IcebergCatalogConfig
from dagster_iceberg.io_manager.pandas import PandasIcebergIOManager

CATALOG_URI = "sqlite:////home/vscode/workspace/.tmp/examples/select_columns/catalog.db"
CATALOG_WAREHOUSE = (
    "file:///home/vscode/workspace/.tmp/examples/select_columns/warehouse"
)

resources = {
    "io_manager": PandasIcebergIOManager(
        name="test",
        config=IcebergCatalogConfig(
            properties={"uri": CATALOG_URI, "warehouse": CATALOG_WAREHOUSE}
        ),
        namespace="dagster",
    )
}

@asset
def iris_dataset() -> pd.DataFrame:
    return pd.read_csv(
        "https://docs.dagster.io/assets/iris.csv",
        names=[
            "sepal_length_cm",
            "sepal_width_cm",
            "petal_length_cm",
            "petal_width_cm",
            "species",
        ],
    )

defs = Definitions(assets=[iris_dataset], resources=resources)
```

If you do not provide a schema, Dagster will determine a schema based on the assets and ops using the I/O manager. For assets, the schema will be determined from the asset key, as in the above example. For ops, the schema can be specified by including a “schema” entry in output metadata. If none of these is provided, the schema will default to “public”. The I/O manager will check if the namespace exists in the Iceberg catalog. It does not automatically create the namespace if it does not exist.

```python
@op(
    out={"my_table": Out(metadata={"schema": "my_schema"})}
)
def make_my_table() -> pd.DataFrame:
    ...
```

To only use specific columns of a table as input to a downstream op or asset, add the metadata “columns” to the `In` or `AssetIn`.

```python
@asset(
    ins={"my_table": AssetIn("my_table", metadata={"columns": ["a"]})}
)
def my_table_a(my_table: pd.DataFrame):
    # my_table will just contain the data from column "a"
    ...
```

dagster\_iceberg.io\_manager.polars.PolarsIcebergIOManager IOManagerDefinition [\[source\]](https://github.com/dagster-io/dagster/blob/master/docs/.tox/sphinx-mdx-vercel/lib/python3.11/site-packages/dagster_iceberg/io_manager/polars.py#L58)

An I/O manager definition that reads inputs from and writes outputs to Iceberg tables using Polars.

Examples:

If you do not provide a schema, Dagster will determine a schema based on the assets and ops using the I/O manager. For assets, the schema will be determined from the asset key, as in the above example. For ops, the schema can be specified by including a “schema” entry in output metadata. If none of these is provided, the schema will default to “public”. The I/O manager will check if the namespace exists in the Iceberg catalog. It does not automatically create the namespace if it does not exist.

```python
@op(
    out={"my_table": Out(metadata={"schema": "my_schema"})}
)
def make_my_table() -> pl.DataFrame:
    ...
```

To only use specific columns of a table as input to a downstream op or asset, add the metadata “columns” to the `In` or `AssetIn`.

```python
@asset(
    ins={"my_table": AssetIn("my_table", metadata={"columns": ["a"]})}
)
def my_table_a(my_table: pl.DataFrame):
    # my_table will just contain the data from column "a"
    ...
```

dagster\_iceberg.io\_manager.spark.SparkIcebergIOManager IOManagerDefinition [\[source\]](https://github.com/dagster-io/dagster/blob/master/docs/.tox/sphinx-mdx-vercel/lib/python3.11/site-packages/dagster_iceberg/io_manager/spark.py#L145)

An I/O manager definition that reads inputs from and writes outputs to Iceberg tables using PySpark.

This I/O manager is only designed to work with Spark Connect.

Example:

```python
from dagster import Definitions, asset
from dagster_iceberg.io_manager.spark import SparkIcebergIOManager
from pyspark.sql import SparkSession
from pyspark.sql.connect.dataframe import DataFrame

resources = {
    "io_manager": SparkIcebergIOManager(
        catalog_name="test",
        namespace="dagster",
        remote_url="spark://localhost",
    )
}

@asset
def iris_dataset() -> DataFrame:
    spark = SparkSession.builder.remote("sc://localhost").getOrCreate()
    return spark.read.csv(
        "https://docs.dagster.io/assets/iris.csv",
        schema=(
            "sepal_length_cm FLOAT, "
            "sepal_width_cm FLOAT, "
            "petal_length_cm FLOAT, "
            "petal_width_cm FLOAT, "
            "species STRING"
        ),
    )

defs = Definitions(assets=[iris_dataset], resources=resources)
```

## Resources

dagster\_iceberg.resource.IcebergTableResource ResourceDefinition [\[source\]](https://github.com/dagster-io/dagster/blob/master/docs/.tox/sphinx-mdx-vercel/lib/python3.11/site-packages/dagster_iceberg/resource.py#L11)

Resource for interacting with a PyIceberg table.

Example:

```python
from dagster import Definitions, asset
from dagster_iceberg import IcebergTableResource

@asset
def my_table(iceberg_table: IcebergTableResource):
    df = iceberg_table.load().to_pandas()

warehouse_path = "/path/to/warehouse"

defs = Definitions(
    assets=[my_table],
    resources={
        "iceberg_table": IcebergTableResource(
            name="my_catalog",
            config=IcebergCatalogConfig(
                properties={
                    "uri": f"sqlite:///{warehouse_path}/pyiceberg_catalog.db",
                    "warehouse": f"file://{warehouse_path}",
                }
            ),
            table="my_table",
            namespace="my_namespace",
        )
    },
)
```

## Config

`class` dagster\_iceberg.config.IcebergCatalogConfig [\[source\]](https://github.com/dagster-io/dagster/blob/master/docs/.tox/sphinx-mdx-vercel/lib/python3.11/site-packages/dagster_iceberg/config.py#L14)

Configuration for Iceberg Catalogs.

See the [Catalogs section](https://py.iceberg.apache.org/configuration/#catalogs) for configuration options.

You can configure the Iceberg IO manager:

1. Using a `.pyiceberg.yaml` configuration file.
2. Through environment variables.
3. Using the `IcebergCatalogConfig` configuration object.

For more information about the first two configuration options, see [Setting Configuration Values](https://py.iceberg.apache.org/configuration/#setting-configuration-values).

Example:

```python
from dagster_iceberg.config import IcebergCatalogConfig
from dagster_iceberg.io_manager.arrow import PyArrowIcebergIOManager

warehouse_path = "/path/to/warehouse"

io_manager = PyArrowIcebergIOManager(
    name="my_catalog",
    config=IcebergCatalogConfig(
        properties={
            "uri": f"sqlite:///{warehouse_path}/pyiceberg_catalog.db",
            "warehouse": f"file://{warehouse_path}",
        }
    ),
    namespace="my_namespace",
)
```

## Base Classes

`class` dagster\_iceberg.io\_manager.base.IcebergIOManager [\[source\]](https://github.com/dagster-io/dagster/blob/master/docs/.tox/sphinx-mdx-vercel/lib/python3.11/site-packages/dagster_iceberg/io_manager/base.py#L105)

Base class for an I/O manager definition that reads inputs from and writes outputs to Iceberg tables.

Examples:

```python
import pandas as pd
import pyarrow as pa
from dagster import Definitions, asset
from dagster_iceberg.config import IcebergCatalogConfig
from dagster_iceberg.io_manager.arrow import PyArrowIcebergIOManager

CATALOG_URI = "sqlite:////home/vscode/workspace/.tmp/examples/select_columns/catalog.db"
CATALOG_WAREHOUSE = (
    "file:///home/vscode/workspace/.tmp/examples/select_columns/warehouse"
)

resources = {
    "io_manager": PyArrowIcebergIOManager(
        name="test",
        config=IcebergCatalogConfig(
            properties={"uri": CATALOG_URI, "warehouse": CATALOG_WAREHOUSE}
        ),
        namespace="dagster",
    )
}

@asset
def iris_dataset() -> pa.Table:
    pa.Table.from_pandas(
        pd.read_csv(
            "https://docs.dagster.io/assets/iris.csv",
            names=[
                "sepal_length_cm",
                "sepal_width_cm",
                "petal_length_cm",
                "petal_width_cm",
                "species",
            ],
        )
    )

defs = Definitions(assets=[iris_dataset], resources=resources)
```

If you do not provide a schema, Dagster will determine a schema based on the assets and ops using the I/O manager. For assets, the schema will be determined from the asset key, as in the above example. For ops, the schema can be specified by including a “schema” entry in output metadata. If none of these is provided, the schema will default to “public”. The I/O manager will check if the namespace exists in the Iceberg catalog. It does not automatically create the namespace if it does not exist.

```python
@op(
    out={"my_table": Out(metadata={"schema": "my_schema"})}
)
def make_my_table() -> pa.Table:
    ...
```

To only use specific columns of a table as input to a downstream op or asset, add the metadata “columns” to the `In` or `AssetIn`.

```python
@asset(
    ins={"my_table": AssetIn("my_table", metadata={"columns": ["a"]})}
)
def my_table_a(my_table: pa.Table):
    # my_table will just contain the data from column "a"
    ...
```

To select a write mode, set the `write_mode` key in the asset definition metadata or at runtime via output metadata. Write mode set at runtime takes precedence over the one set in the definition metadata. Valid modes are `append`, `overwrite`, and `upsert`; default is `overwrite`.

```python
# set at definition time via definition metadata
@asset(
    metadata={"write_mode": "append"}
)
def my_table_a(my_table: pa.Table):
    return my_table

# set at runtime via output metadata
@asset
def my_table_a(context: AssetExecutionContext, my_table: pa.Table):
    # my_table will be written with append mode
    context.add_output_metadata({"write_mode": "append"})
    return my_table
```

To use upsert mode, set `write_mode` to `upsert` and provide `upsert_options` in asset definition metadata or output metadata. The `upsert_options` dictionary should contain `join_cols` (list of columns to join on),`when_matched_update_all` (boolean), and `when_not_matched_insert_all` (boolean). Upsert options set at runtime take precedence over those set in definition metadata.

```python
# set at definition time via definition metadata
@asset(
    metadata={
        "write_mode": "upsert",
        "upsert_options": {
            "join_cols": ["id"],
            "when_matched_update_all": True,
            "when_not_matched_insert_all": True,
        }
    }
)
def my_table_upsert(my_table: pa.Table):
    return my_table

# set at runtime via output metadata (overrides definition metadata)
@asset(
    metadata={
        "write_mode": "upsert",
        "upsert_options": {
            "join_cols": ["id"],
            "when_matched_update_all": True,
            "when_not_matched_insert_all": False,
        }
    }
)
def my_table_upsert_dynamic(context: AssetExecutionContext, my_table: pa.Table):
    # Override upsert options at runtime
    context.add_output_metadata({
        "upsert_options": {
            "join_cols": ["id", "timestamp"],
            "when_matched_update_all": False,
            "when_not_matched_insert_all": False,
        }
    })
    return my_table
```

`class` dagster\_iceberg.handler.IcebergBaseTypeHandler [\[source\]](https://github.com/dagster-io/dagster/blob/master/docs/.tox/sphinx-mdx-vercel/lib/python3.11/site-packages/dagster_iceberg/handler.py#L35)

Base class for a type handler that reads inputs from and writes outputs to Iceberg tables.