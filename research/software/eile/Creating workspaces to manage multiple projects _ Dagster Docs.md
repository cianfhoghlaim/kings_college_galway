---
title: "Creating workspaces to manage multiple projects | Dagster Docs"
source: "https://docs.dagster.io/guides/build/projects/workspaces/creating-workspaces"
author:
published:
created: 2025-12-12
description: "Manage multiple isolated Dagster projects by creating a workspace directory with the create-dagster command."
tags:
  - "clippings"
---
If you need to collaborate with multiple teams, or work with conflicting dependencies that require isolation from each other, you can scaffold a workspace directory that contains multiple projects, each with their own separate Python environment, while still being able to access all of your assets across every project in a single instance of the Dagster UI or `dg` CLI.

A workspace directory contains a root [`dg.toml` file](https://docs.dagster.io/guides/build/projects/workspaces/dg-toml) with workspace-level settings, and a `projects` directory with one or more projects. It also contains a Python environment in a `deployments/local` folder that can be used for running `dg` commands locally against the workspace.

When a `dg` command runs in a workspace, it will create a subprocess for each project using that project's virtual environment, and communicate with each process through an API layer. The diagram below demonstrates a workspace with two projects, as well as their virtual environments.

![Diagram showing the virtual environments used by a workspace and 2 projects](https://docs.dagster.io/assets/images/workspace-venvs-a1f94cade8ebd825028a704a52bce0ef.png)

## Creating a new workspace and first project

To scaffold a new workspace called `dagster-workspace`, run `uvx create-dagster@latest workspace` and respond yes to the prompt to run `uv sync` after scaffolding:

```markdown
uvx create-dagster workspace dagster-workspace && cd dagster-workspace
```

The scaffolded workspace includes a `projects` folder, which is currently empty, and a `deployments` folder, which includes a `local` folder with a `pyproject.toml` file that specifies an environment for running `dg` commands locally against your workspace.

Next, enter the directory and activate the virtual environment for the `local` environment:

```markdown
source deployments/local/.venv/bin/activate
```

Now we'll create a project inside our workspace called `project-1`. Run `uvx create-dagster@latest project` with the path of the project:

```markdown
uvx create-dagster project projects/project-1
```

```markdown
Creating a Dagster project at /.../dagster-workspace/projects/project-1.
Scaffolded files for Dagster project at /.../dagster-workspace/projects/project-1.
A \`uv\` installation was detected. Run \`uv sync\`? This will create a uv.lock file and the virtual environment you need to activate in order to work on this project. If you wish to use a non-uv package manager, choose "n". (y/n) [y]: Running \`uv sync --group dev\`...
...
```

This will create a new Python environment for this project and associate that project with the workspace.

### Workspace structure

The new workspace has the following structure:

```markdown
tree
```

```markdown
.
├── deployments
│   └── local
│       ├── pyproject.toml
│       └── uv.lock
├── dg.toml
└── projects
    └── project-1
        ├── pyproject.toml
        ├── src
        │   └── project_1
        │       ├── __init__.py
        │       ├── definitions.py
        │       └── defs
        │           └── __init__.py
        ├── tests
        │   └── __init__.py
        └── uv.lock

...
```

The `dg.toml` file for the `dagster-workspace` folder contains a `directory_type = "workspace"` setting that marks this directory as a workspace:

```toml
dagster-workspace/dg.tomldirectory_type = "workspace"

[workspace]

[[workspace.projects]]
path = "projects/project-1"
```

The `project-1` directory contains a `pyproject.toml` file with a `tool.dg.directory_type = "project"` section that defines it as a `dg` project:

```toml
dagster-workspace/projects/project-1/pyproject.toml...
[tool.dg]
directory_type = "project"

[tool.dg.project]
root_module = "project_1"
...
```

## Adding a second project to the workspace

As noted above, environments are scoped per project. `dg` commands will only use the environment of `project-1` when you are inside the `project-1` directory.

Let's create another project:

```markdown
uvx create-dagster project projects/project-2
```

```markdown
Creating a Dagster project at /.../dagster-workspace/projects/project-2.
Scaffolded files for Dagster project at /.../dagster-workspace/projects/project-2.
A \`uv\` installation was detected. Run \`uv sync\`? This will create a uv.lock file and the virtual environment you need to activate in order to work on this project. If you wish to use a non-uv package manager, choose "n". (y/n) [y]: Running \`uv sync --group dev\`...
...
```

Now there are two projects. You can list them with:

```markdown
dg list project
```

```markdown
projects/project-1
projects/project-2
```

The workspace now has the following structure:

Finally, let's load our two projects with `dg dev`. When you run `dg dev` from the workspace root, it will automatically recognize the projects in your workspace and launch each project in a separate process in its virtual environment found in the `.venv` folder in the project.

```markdown
dg dev
```

![](https://docs.dagster.io/assets/images/two-projects-a9d77c4661a67b8435ab48ad6aa8c0eb.png)

Ask Dagster AI