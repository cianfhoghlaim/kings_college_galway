---
title: "dlt (dagster-dlt) | Dagster Docs"
source: "https://docs.dagster.io/api/libraries/dagster-dlt"
author:
published:
created: 2025-12-12
description: "dlt (dagster-dlt) Dagster API | Comprehensive Python API documentation for Dagster, the data orchestration platform. Learn how to build, test, and maintain data pipelines with our detailed guides and examples."
tags:
  - "clippings"
---
This library provides a Dagster integration with [dlt](https://dlthub.com/).

For more information on getting started, see the [Dagster & dlt](https://docs.dagster.io/integrations/libraries/dlt) documentation.

## Component

`class` dagster\_dlt.DltLoadCollectionComponent [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/components/dlt_load_collection/component.py#L71)

Expose one or more dlt loads to Dagster as assets.

execute [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/components/dlt_load_collection/component.py#L143)

Executes the dlt pipeline for the selected resources.

This method can be overridden in a subclass to customize the pipeline execution behavior, such as adding custom logging, validation, or error handling.

Parameters:

- **context** – The asset execution context provided by Dagster
- **dlt\_pipeline\_resource** – The DagsterDltResource used to run the dlt pipeline

Yields: Events from the dlt pipeline execution (e.g., AssetMaterialization, MaterializeResult)

Example:

Override this method to add custom logging during pipeline execution:

```python
from dagster_dlt import DltLoadCollectionComponent
from dagster import AssetExecutionContext

class CustomDltLoadCollectionComponent(DltLoadCollectionComponent):
    def execute(self, context, dlt_pipeline_resource):
        context.log.info("Starting dlt pipeline execution")
        yield from super().execute(context, dlt_pipeline_resource)
        context.log.info("dlt pipeline execution completed")
```

get\_asset\_spec [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/components/dlt_load_collection/component.py#L89)

Generates an AssetSpec for a given dlt resource.

This method can be overridden in a subclass to customize how dlt resources are converted to Dagster asset specs. By default, it delegates to the configured DagsterDltTranslator.

Parameters: **data** – The DltResourceTranslatorData containing information about the dlt source and resource being loadedReturns: An AssetSpec that represents the dlt resource as a Dagster asset

Example:

Override this method to add custom tags based on resource properties:

```python
from dagster_dlt import DltLoadCollectionComponent
from dagster import AssetSpec

class CustomDltLoadCollectionComponent(DltLoadCollectionComponent):
    def get_asset_spec(self, data):
        base_spec = super().get_asset_spec(data)
        return base_spec.replace_attributes(
            tags={
                **base_spec.tags,
                "source": data.source_name,
                "resource": data.resource_name
            }
        )
```

To use the dlt component, see the [dlt component integration guide](https://docs.dagster.io/integrations/libraries/dlt).

### YAML configuration

When you scaffold a dlt component definition, the following `defs.yaml` configuration file will be created:

```yaml
type: dagster_dlt.DltLoadCollectionComponent

attributes:
  loads:
    - source: .loads.my_load_source
      pipeline: .loads.my_load_pipeline
```

## Assets

@dagster\_dlt.dlt\_assets [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/asset_decorator.py#L56)

Asset Factory for using data load tool (dlt).

Parameters:

- **dlt\_source** (*DltSource*) – The DltSource to be ingested.
- **dlt\_pipeline** (*Pipeline*) – The dlt Pipeline defining the destination parameters.
- **name** (*Optional* *\[**str**\]**,* *optional*) – The name of the op.
- **group\_name** (*Optional* *\[**str**\]**,* *optional*) – The name of the asset group.
- **dagster\_dlt\_translator** ([*DagsterDltTranslator*](https://docs.dagster.io/api/libraries/#dagster_dlt.DagsterDltTranslator)*,* *optional*) – Customization object for defining asset parameters from dlt resources.
- **partitions\_def** (*Optional* *\[*[*PartitionsDefinition*](https://docs.dagster.io/api/dagster/partitions#dagster.PartitionsDefinition)*\]*) – Optional partitions definition.
- **backfill\_policy** (*Optional* *\[*[*BackfillPolicy*](https://docs.dagster.io/api/dagster/partitions#dagster.BackfillPolicy)*\]*) – If a partitions\_def is defined, this determines how to execute backfills that target multiple partitions. If a time window partition definition is used, this parameter defaults to a single-run policy.
- **op\_tags** (*Optional* *\[**Mapping* *\[**str**,* *Any**\]**\]*) – The tags for the underlying op.
- **pool** (*Optional* *\[**str**\]*) – A string that identifies the concurrency pool that governs the dlt assets’ execution.

Examples:

Loading Hubspot data to Snowflake with an auto materialize policy using the dlt verified source:

```python
from dagster_dlt import DagsterDltResource, DagsterDltTranslator, dlt_assets

class HubspotDagsterDltTranslator(DagsterDltTranslator):
    @public
    def get_auto_materialize_policy(self, resource: DltResource) -> Optional[AutoMaterializePolicy]:
        return AutoMaterializePolicy.eager().with_rules(
            AutoMaterializeRule.materialize_on_cron("0 0 * * *")
        )

@dlt_assets(
    dlt_source=hubspot(include_history=True),
    dlt_pipeline=pipeline(
        pipeline_name="hubspot",
        dataset_name="hubspot",
        destination="snowflake",
        progress="log",
    ),
    name="hubspot",
    group_name="hubspot",
    dagster_dlt_translator=HubspotDagsterDltTranslator(),
)
def hubspot_assets(context: AssetExecutionContext, dlt: DagsterDltResource):
    yield from dlt.run(context=context)
```

Loading Github issues to snowflake:

```python
from dagster_dlt import DagsterDltResource, dlt_assets

@dlt_assets(
    dlt_source=github_reactions(
        "dagster-io", "dagster", items_per_page=100, max_items=250
    ),
    dlt_pipeline=pipeline(
        pipeline_name="github_issues",
        dataset_name="github",
        destination="snowflake",
        progress="log",
    ),
    name="github",
    group_name="github",
)
def github_reactions_dagster_assets(context: AssetExecutionContext, dlt: DagsterDltResource):
    yield from dlt.run(context=context)
```

dagster\_dlt.build\_dlt\_asset\_specs [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/asset_decorator.py#L21)

Build a list of asset specs from a dlt source and pipeline.

Parameters:

- **dlt\_source** (*DltSource*) – dlt source object
- **dlt\_pipeline** (*Pipeline*) – dlt pipeline object
- **dagster\_dlt\_translator** (*Optional* *\[*[*DagsterDltTranslator*](https://docs.dagster.io/api/libraries/#dagster_dlt.DagsterDltTranslator)*\]*) – Allows customizing how to map dlt project to asset keys and asset metadata.

Returns: List\[AssetSpec\] list of asset specs from dlt source and pipeline

`class` dagster\_dlt.DagsterDltTranslator [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/translator.py#L24)

get\_asset\_key [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/translator.py#L91)

Defines asset key for a given dlt resource key and dataset name.

This method can be overridden to provide custom asset key for a dlt resource.

Parameters: **resource** (*DltResource*) – dlt resourceReturns: AssetKey of Dagster asset derived from dlt resource

get\_auto\_materialize\_policy [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/translator.py#L121)

Defines resource specific auto materialize policy.

This method can be overridden to provide custom auto materialize policy for a dlt resource.

Parameters: **resource** (*DltResource*) – dlt resourceReturns: The auto-materialize policy for a resourceReturn type: Optional\[AutoMaterializePolicy\]

get\_automation\_condition [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/translator.py#L153)

Defines resource specific automation condition.

This method can be overridden to provide custom automation condition for a dlt resource.

Parameters: **resource** (*DltResource*) – dlt resourceReturns: The automation condition for a resourceReturn type: Optional\[[AutomationCondition](https://docs.dagster.io/api/dagster/assets#dagster.AutomationCondition)\]

get\_deps\_asset\_keys [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/translator.py#L188)

Defines upstream asset dependencies given a dlt resource.

Defaults to a concatenation of resource.source\_name and resource.name.

Parameters: **resource** (*DltResource*) – dlt resourceReturns: The Dagster asset keys upstream of dlt\_resource\_key.Return type: Iterable\[[AssetKey](https://docs.dagster.io/api/dagster/assets#dagster.AssetKey)\]

get\_description [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/translator.py#L225)

A method that takes in a dlt resource returns the Dagster description of the resource.

This method can be overridden to provide a custom description for a dlt resource.

Parameters: **resource** (*DltResource*) – dlt resourceReturns: The Dagster description for the dlt resource.Return type: Optional\[str\]

get\_group\_name [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/translator.py#L258)

A method that takes in a dlt resource and returns the Dagster group name of the resource.

This method can be overridden to provide a custom group name for a dlt resource.

Parameters: **resource** (*DltResource*) – dlt resourceReturns: A Dagster group name for the dlt resource.Return type: Optional\[str\]

get\_kinds [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/translator.py#L370)

A method that takes in a dlt resource and returns the kinds which should be attached. Defaults to the destination type and “dlt”.

This method can be overridden to provide custom kinds for a dlt resource.

Parameters:

- **resource** (*DltResource*) – dlt resource
- **destination** (*Destination*) – dlt destination

Returns: The kinds of the asset.Return type: Set\[str\]

Defines resource specific metadata.

Parameters: **resource** (*DltResource*) – dlt resourceReturns: The custom metadata entries for this resource.Return type: Mapping\[str, Any\]

get\_owners [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/translator.py#L312)

A method that takes in a dlt resource and returns the Dagster owners of the resource.

This method can be overridden to provide custom owners for a dlt resource.

Parameters: **resource** (*DltResource*) – dlt resourceReturns: A sequence of Dagster owners for the dlt resource.Return type: Optional\[Sequence\[str\]\]

A method that takes in a dlt resource and returns the Dagster tags of the structure.

This method can be overridden to provide custom tags for a dlt resource.

Parameters: **resource** (*DltResource*) – dlt resourceReturns: A dictionary representing the Dagster tags for the dlt resource.

Return type: Optional\[Mapping\[str, str\]\]

## Resources

`class` dagster\_dlt.DagsterDltResource [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/resource.py#L29)

run [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-dlt/dagster_dlt/resource.py#L176)

Runs the dlt pipeline with subset support.

Parameters:

- **context** (*Union* *\[*[*OpExecutionContext*](https://docs.dagster.io/api/dagster/execution#dagster.OpExecutionContext)*,* [*AssetExecutionContext*](https://docs.dagster.io/api/dagster/execution#dagster.AssetExecutionContext)*\]*) – Asset or op execution context
- **dlt\_source** (*Optional* *\[**DltSource**\]*) – optional dlt source if resource is used from an @op
- **dlt\_pipeline** (*Optional* *\[**Pipeline**\]*) – optional dlt pipeline if resource is used from an @op
- **dagster\_dlt\_translator** (*Optional* *\[*[*DagsterDltTranslator*](https://docs.dagster.io/api/libraries/#dagster_dlt.DagsterDltTranslator)*\]*) – optional dlt translator if resource is used from an @op
- **\*\*kwargs** (*dict* *\[**str**,* *Any**\]*) – Keyword args passed to pipeline run method

Returns: An iterator of MaterializeResult or AssetMaterializationReturn type: DltEventIterator\[DltEventType\]