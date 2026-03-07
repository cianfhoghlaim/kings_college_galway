---
title: "DuckLake to MotherDuck: Validate locally, deploy to cloud in minutes"
source: "https://dlthub.com/blog/ducklake-to-motherduck-with-dlt"
author:
published: 2025-12-16
created: 2025-12-23
description: "Start local with DuckLake, validate your data, then deploy to MotherDuck in minutes. Same pipeline, same code, just switch the destination."
tags:
  - "clippings"
---
Most data teams start by developing locally, then load data and deploy to the cloud. It’s the familiar path, but not the shortest. The friction usually comes when merging the two worlds: Local prototyping & development vs production credentials and cloud deployment.

So naturally, as data people, we want this friction removed. But we also want to be efficient about it, so we want to keep our local workflows while making the “go to online” path simple.

### Starting local

Early pipeline work is about getting the ground truth right - how am I calling data from the source, what does the data look like, what of it will I load, and how can I make it incremental?

Because in a data pipeline, you can't really separate the code from the semantics of the data. You want to quickly get some data, have a look at it, discard it and continue development.

Having to connect to production just to look at some data is just an inconvenience.

DuckLake fixes this by giving you a fast local environment. You load the data locally, validate it, and once everything looks right, you flip the destination switch to the cloud.

![](https://cdn.sanity.io/images/nsq559ov/production/f6aaff5414bf4b69949971ad6a0d7b1ba84999e2-3792x2544.png?w=120&auto=format)

### How DuckLake helps local development

DuckLake stores table data in Parquet files while keeping all table and partition metadata in a SQL catalog database. Being lightweight, it lets you spin up a lakehouse on your laptop instantly. And that fast feedback loop is what makes pipeline development easier.

### How MotherDuck fits into production

Ducklake lets you use a decoupled runtime + storage, so when you publish the data into production, you want to make sure that there’s a runtime available for users who want to read the data - that runtime is Motherduck.

### Walkthrough: Validate locally, deploy to the cloud

We’ll use the Hacker News API to walk through the workflow. First, we’ll load the data locally in DuckLake. Once we’ve validated it using dlt's DBML export and the Dashboard, we’ll switch the pipeline destination to MotherDuck and run the pipeline as-is.

If you want to try this yourself, here’s the Colab notebook: [link.](https://colab.research.google.com/drive/14t6-k6so_J9ANaKDCr_6NH-GdgNyMllX?usp=sharing)

#### Install dependencies

```
!pip install "dlt[ducklake,motherduck, dbml, workspace]"
```

#### Define a Hacker News source

```
import dlt

import requests

session = requests.Session()

# fetch top Hacker News stories

@dlt.resource(

    table_name="stories",

    write_disposition="merge",

    primary_key="id"

)

def hacker_news(limit=30):

    ids = session.get(

        "https://hacker-news.firebaseio.com/v0/topstories.json"

    ).json()[:limit]

    for story_id in ids:

        story = session.get(

            f"https://hacker-news.firebaseio.com/v0/item/{story_id}.json"

        ).json()

        if story:

            yield story

            

# run the pipeline with 'ducklake' destination

pipeline = dlt.pipeline(

    pipeline_name="hn_local",

    destination="ducklake",

    dataset_name="hacker_news"

)

print(pipeline.run(hacker_news(50)))
```

#### Inspect the schema locally

```
!dlt pipeline hn_local schema --format dbml
```

This prints the inferred schema in DBML format. It’s a quick way to verify that the schema structure matches your expectations. You can take the output and paste it in the third-party apps like dbdiagram, as shown below.

To inspect your data further, we’ll use the [workspace dashboard](https://dlthub.com/docs/general-usage/dashboard).

The dashboard is a web app that shows you pipeline metadata, the schema, and table previews at a glance. It also lets you run SQL directly against your DuckLake files using datasets, so you can check row counts or run quick QA queries.

For example:

**Schema Check:** View tables, columns, data types and hints.

![](https://cdn.sanity.io/images/nsq559ov/production/7919584ad91ad224e47008c9579dfa3b9eeb2577-2286x1618.png?w=120&auto=format)

**Dataset Browser:** Run SQL queries.

**Row count:**

![](https://cdn.sanity.io/images/nsq559ov/production/7c2ab19b71900353f5833749541300f169439af5-2138x1590.png?w=120&auto=format)

**Data Preview:** a quick look at the data usually tells you half the story.

![](https://cdn.sanity.io/images/nsq559ov/production/d1fad67caa937937dc3027f421effdd2af58a238-2294x1444.png?w=120&auto=format)

After the sample data, columns, and schema look good, we’ll deploy to MotherDuck.

#### Switch to MotherDuck

We only change one thing to move to MotherDuck. No rewrites to your logic or API handling. We simply flip the destination parameter to `motherduck`.

```
pipeline = dlt.pipeline(

    pipeline_name="hacker_news_pipeline_md",

    destination="motherduck",

    dataset_name="hacker_news_data",

    dev_mode=True,

)

load_info = pipeline.run(hacker_news(50))

print(load_info)
```

Nothing else changes. Same code. Just a different destination.

#### Confirm the Cloud load

In MotherDuck, let's confirm that the schema and data match what we validated locally.

**Schema:**

![](https://cdn.sanity.io/images/nsq559ov/production/fb416a40fc4aa9c4c574d930486c840b03298bf2-898x1102.png?w=120&auto=format)

**Row count:**

![](https://cdn.sanity.io/images/nsq559ov/production/2cff129922da1ffa69fb90baf3376b222ef61ae1-936x377.png?w=120&auto=format)

Same data, just living in the cloud now.

You can inspect the data further in the dashboard. For example, the trace from the last pipeline run helps you debug any load issues.

![](https://cdn.sanity.io/images/nsq559ov/production/383860776d447b63381243938fdfe862d1df6fe5-2197x663.png?w=120&auto=format)

Here, when you run queries in the dashboard, it connects to MotherDuck and displays the data from the destination.

### Why this works

- **Fast feedback loop:** Develop and test locally, without infrastructure in the way. Answers come fast.
- **What works locally works in production:** DuckLake and MotherDuck both run on the same DuckDB engine. That means the pipeline you trust on your laptop behaves the same way in production. Fewer surprises when you ship.
- **Focus on logic, not config:** Your time goes into extraction, transformation, and shaping how data lands. Not babysitting environments or rewriting things for production.

You write once, validate once, and trust it.

From here, you can:

- read the [dlt docs](https://dlthub.com/docs/intro) to go deeper
- use workspace to vibe code an [API pipeline](https://dlthub.com/workspace) in minutes
- take a [course](https://dlthub.com/events) if you prefer video learning
- or add [tests and quality checks](https://dlthub.com/docs/general-usage/dataset-access/data-quality-dashboard) to your pipeline

The pattern stays simple. Explore and validate locally, then let MotherDuck take it from there. This keeps your local environment close to the logic, and the cloud handles the parts that need scale.

Speaking of cloud scale, we have some news.

We're proud to be **MotherDuck’s official launch partner for Europe.** Read the announcement [here](https://dlthub.com/blog/motherduck-europe-dlt-integration).

### Still here? Try it yourself

You have the code, and you know the pattern. Try this workflow and see how simple it can be. It will not take more than a coffee break.

- Link [to Colab](https://colab.research.google.com/drive/14t6-k6so_J9ANaKDCr_6NH-GdgNyMllX?usp=sharing)
- Need help? Join our [Slack community](https://dlthub.com/community)[Data contract agreement vs enforcement](https://dlthub.com/blog/data-contracts-agreement-vs-enforcement)

[

11 Pythonic Data Quality Recipes for every day

](https://dlthub.com/blog/practical-data-quality-recipes-with-dlt)