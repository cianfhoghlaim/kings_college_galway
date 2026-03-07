---
title: "Explore data with marimo | dlt Docs"
source: "https://dlthub.com/docs/general-usage/dataset-access/marimo"
author:
published:
created: 2025-12-09
description: "Explore your data with marimo"
tags:
  - "clippings"
---
Version: 1.19.1 (latest)

[marimo](https://github.com/marimo-team/marimo) is a reactive Python notebook. It completely revamps the Jupyter notebook experience. Whenever code is executed or you interact with a UI element, dependent cells are re-executed ensuring consistency between code and displayed outputs.

This page shows how dlt + marimo + [ibis](https://dlthub.com/docs/general-usage/dataset-access/ibis-backend) provide a rich environment to explore loaded data, write data transformations, and create data applications.

## Prerequisites

To install marimo and ibis with the duckdb extras, run the following command:

```sh
pip install marimo "ibis-framework[duckdb]"
```

## Launch marimo

Use this command to launch marimo (replace `my_notebook.py` with desired name). It will print a link to access the notebook web app.

```sh
marimo edit my_notebook.py

> Edit my_notebook.py in your browser 📝
>   ➜  URL: http://localhost:2718?access_token=Qfo_Hj2RbXqiqM4VT3XOwA
```

Here's a screenshot of the interface you should see:

![](https://dlthub.com/docs/assets/images/marimo_notebook-b12e23a23d11a80d938dfdf2814982ac.png)

## Features

### Use custom dlt widgets

Inside your marimo notebook, you can use widgets built and maintained by the dlt team.

Simply import them from `dlt.helpers.marimo` and pass them to the `render()` function. Note that `render()` is asynchronous and must be awaited with `await`.

```markdown
#%% cell 1
import marimo as mo
from dlt.helpers.marimo import render, load_package_viewer

#%% cell 2
await render(load_package_viewer)
```

![Example marimo widget](https://storage.googleapis.com/dlt-blog-images/marimo-widget-screenshot.png)

### View dataset tables and columns

After loading data with dlt, you can access it via the [dataset interface](https://dlthub.com/docs/general-usage/dataset-access/dataset), including a [native ibis connection](https://dlthub.com/docs/general-usage/dataset-access/ibis-backend).

In marimo, the **Datasources** panel provides a GUI to explore data tables and columns. When a cell contains a variable that's an ibis connection, it is automatically registered.

![](https://dlthub.com/docs/assets/images/marimo_dataset-147692752e667c08066a86e51fa005d4.png)

### Accessing data with SQL

Clicking on the **Add table to notebook** button will create a new SQL cell that you can use to query data. The output cell provides a rich and interactive results dataframe.

![](https://dlthub.com/docs/assets/images/marimo_sql-a28936601a41f5ff445fd4b75ddf6bc9.png)

### Accessing data with Python

You can also retrieve Ibis tables (lazy expressions) using Python. The **Datasources** panel will show under **Python** the output schema of your Ibis query, and the cell output will display detailed query planning.

Use `.execute()`, `.to_pandas()`, `.to_polars()`, or `.to_pyarrow()` to execute the Ibis expression and retrieve data that can displayed in a rich and interactive dataframe.

![](https://dlthub.com/docs/assets/images/marimo_python-74a6f55e32d9789f265444e191d1e161.png)

### Create a dashboard and data apps

marimo notebooks can be [deployed as web applications with interactive UI and charts](https://docs.marimo.io/guides/apps/) and the code hidden. Try adding [marimo UI input elements](https://docs.marimo.io/guides/interactivity/), rich markdown, and charts (matplotlib, plotly, altair, etc.). Combined, dlt + marimo + ibis make it easy to build a simple dashboard on top of fresh data.

- [Access loaded data in Python using dlt datasets](https://dlthub.com/docs/general-usage/dataset-access/dataset).
- [Learn about marimo dataframe and SQL features](https://docs.marimo.io/guides/working_with_data/)
- [Explore databases using the marimo GUI](https://docs.marimo.io/guides/coming_from/streamlit/)
- [Learn about marimo if you're coming from Streamlit](https://docs.marimo.io/guides/coming_from/streamlit/)

This demo works on codespaces. Codespaces is a development environment available for free to anyone with a Github account. You'll be asked to fork the demo repository and from there the README guides you with further steps.

The demo uses the Continue VSCode extension.

  
[Off to codespaces!](https://github.com/codespaces/new/dlt-hub/dlt-llm-code-playground?ref=create-pipeline)

## DHelp

## Ask a question

Welcome to "Codex Central", your next-gen help center, driven by OpenAI's GPT-4 model. It's more than just a forum or a FAQ hub – it's a dynamic knowledge base where coders can find AI-assisted solutions to their pressing problems. With GPT-4's powerful comprehension and predictive abilities, Codex Central provides instantaneous issue resolution, insightful debugging, and personalized guidance. Get your code running smoothly with the unparalleled support at Codex Central - coding help reimagined with AI prowess.