---
title: "Managing machine learning models with Dagster | Dagster Docs"
source: "https://docs.dagster.io/guides/build/ml-pipelines/managing-ml"
author:
published:
created: 2025-12-12
description: "Managing and maintaining your machine learning (ML) models in Dagster."
tags:
  - "clippings"
---
This guide reviews ways to manage and maintain your machine learning (ML) models in Dagster.

Machine learning models are highly dependent on data at a point in time and must be managed to ensure they produce the same results as when you were in the development phase. In this guide, you'll learn how to:

- Automate training of your model when new data is available or when you want to use your model for predictions
- Integrate metadata about your model into the Dagster UI to display info about your model's performance

## Machine learning operations (MLOps)

You might have thought about your data sources, feature sets, and the best model for your use case. Inevitably, you start thinking about how to make this process sustainable and operational and deploy it to production. You want to make the machine learning pipeline self-sufficient and have confidence that the model you built is performing the way you expect. Thinking about machine learning operations, or MLOps, is the process of making your model maintainable and repeatable for a production use case.

### Automating ML model maintenance

Whether you have a large or small model, Dagster can help automate data refreshes and model training based on your business needs.

Declarative Automation can be used to update a machine learning model when the upstream data is updated. This can be done by setting the [`AutomationCondition`](https://docs.dagster.io/api/dagster/assets#dagster.AutomationCondition) to `eager`, which means that our machine learning model asset will be refreshed anytime our data asset is updated.

```python
import dagster as dg

@dg.asset
def my_data(): ...

@dg.asset(automation_condition=dg.AutomationCondition.eager())
def my_ml_model(my_data): ...
```

Some machine learning models might be more cumbersome to retrain; it also might be less important to update them as soon as new data arrives. For this, the `on_cron` condition may be used, which will cause the asset to be updated on a given cron schedule, but only after all of its upstream dependencies have been updated.

```python
import dagster as dg

@dg.asset
def my_other_data(): ...

@dg.asset(automation_condition=dg.AutomationCondition.on_cron("0 9 * * *"))
def my_other_ml_model(my_other_data): ...
```

### Monitoring

Integrating your machine learning models into Dagster allows you to see when the model and its data dependencies were refreshed, or when a refresh process has failed. By using Dagster to monitor performance changes and process failures on your ML model, it becomes possible to set up remediation paths, such as automated model retraining, that can help resolve issues like model drift.

In this example, the model is being evaluated against the previous model’s accuracy. If the model’s accuracy has improved, the model is returned for use in downstream steps, such as inference or deploying to production.

```python
import dagster as dg
from sklearn import linear_model
import numpy as np
from sklearn.model_selection import train_test_split

@dg.asset(output_required=False)
def conditional_machine_learning_model(context: dg.AssetExecutionContext):
    X, y = np.random.randint(5000, size=(5000, 2)), range(5000)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.33, random_state=42
    )
    reg = linear_model.LinearRegression()
    reg.fit(X_train, y_train)

    # Get the model accuracy from metadata of the previous materilization of this machine learning model
    instance = context.instance
    materialization = instance.get_latest_materialization_event(
        dg.AssetKey(["conditional_machine_learning_model"])
    )
    if materialization is None:
        yield dg.Output(
            reg, metadata={"model_accuracy": float(reg.score(X_test, y_test))}
        )

    else:
        previous_model_accuracy = None
        if materialization.asset_materialization and isinstance(
            materialization.asset_materialization.metadata["model_accuracy"].value,
            float,
        ):
            previous_model_accuracy = float(
                materialization.asset_materialization.metadata["model_accuracy"].value
            )
        new_model_accuracy = reg.score(X_test, y_test)
        if (
            previous_model_accuracy is None
            or new_model_accuracy > previous_model_accuracy
        ):
            yield dg.Output(reg, metadata={"model_accuracy": float(new_model_accuracy)})
```

A [sensor](https://docs.dagster.io/guides/automate/sensors) can be set up that triggers if an asset fails to materialize. Alerts can be customized and sent through e-mail or natively through Slack. In this example, a Slack message is sent anytime the `ml_job` fails.

```python
import dagster as dg
from dagster_slack import make_slack_on_run_failure_sensor

ml_job = dg.define_asset_job("ml_training_job", selection=[ml_model])

slack_on_run_failure = make_slack_on_run_failure_sensor(
    channel="#ml_monitor_channel",
    slack_token=slack_token,
    monitored_jobs=([ml_job]),
)
```

Understanding the performance of your ML model is critical to both the model development process and production. [Metadata](https://docs.dagster.io/guides/build/assets/metadata-and-tags) can significantly enhance the usability of the Dagster UI to show what’s going on in a specific asset. Using metadata in Dagster is flexible, can be used for tracking evaluation metrics, and viewing the training accuracy progress over training iterations as a graph.

One of the easiest ways to utilize Dagster’s metadata is by using a dictionary to track different metrics that are relevant for an ML model.

Another way is to store relevant data for a single training iteration as a graph that you can view directly from the Dagster UI. In this example, a function is defined that uses data produced by a machine learning model to plot an evaluation metric as the model goes through the training process and render that in the Dagster UI.

Dagster’s [`MetadataValue`](https://docs.dagster.io/api/dagster/metadata#dagster.MetadataValue) types enable types such as tables, URLs, notebooks, Markdown, etc. In the following example, the Markdown metadata type is used to generate plots. Each plot will show a specific evaluation metric’s performance throughout each training iteration also known as an epoch during the training cycle.

```python
import dagster as dg
import seaborn
import matplotlib.pyplot as plt
import base64
from io import BytesIO

def make_plot(eval_metric):
    plt.clf()
    training_plot = seaborn.lineplot(eval_metric)
    fig = training_plot.get_figure()
    buffer = BytesIO()
    fig.savefig(buffer)
    image_data = base64.b64encode(buffer.getvalue())
    return dg.MetadataValue.md(f"![img](data:image/png;base64,{image_data.decode()})")
```

In this example, a dictionary is used called `metadata` to store the Markdown plots and the score value in Dagster.

In the Dagster UI, the `xgboost_comments_model` has the metadata rendered. Numerical values, such as the `score (mean_absolute_error)` will be logged and plotted for each materialization, which can be useful to understand the score over time for machine learning models.

![Managing ML in the UI](https://docs.dagster.io/assets/images/managing_ml_ui-2944f895ce0916e41c8e28a8b1d145b7.png)

The Markdown plots are also available to inspect the evaluation metrics during the training cycle by clicking on **\[Show Markdown\]**:

![Markdown plot in the UI](https://docs.dagster.io/assets/images/plot_ui-55e26b07af931b531609ddac95986fa1.png)

## Tracking model history

Viewing previous versions of a machine learning model can be useful to understand the evaluation history or referencing a model that was used for inference. Using Dagster will enable you to understand:

- What data was used to train the model
- When the model was refreshed
- The code version and ML model version was used to generate the predictions used for predicted values

In Dagster, each time an asset is materialized, the metadata and model are stored. Dagster registers the code version, data version and source data for each asset, so understanding what data was used to train a model is linked.

In the screenshot below, each materialization of `xgboost_comments_model` and the path for where each iteration of the model is stored.

![Asset materialization for xgboost_components_model](https://docs.dagster.io/assets/images/assets_materilization-33b59a34b7238658cafc1012410ed84f.png)

Any plots generated through the asset's metadata can be viewed in the metadata section. In this example, the plots of `score (mean_absolute_error)` are available for analysis.

![Metadata plot](https://docs.dagster.io/assets/images/metadata_plot-ecd504c22d1e0709b4c79659aedb4f0b.png)