---
title: "github (dagster-github) | Dagster Docs"
source: "https://docs.dagster.io/api/libraries/dagster-github"
author:
published:
created: 2025-12-12
description: "github (dagster-github) Dagster API | Comprehensive Python API documentation for Dagster, the data orchestration platform. Learn how to build, test, and maintain data pipelines with our detailed guides and examples."
tags:
  - "clippings"
---
This library provides an integration with GitHub Apps, to support performing various automation operations within your github repositories and with the tighter permissions scopes that github apps allow for vs using a personal token.

Presently, it provides a thin wrapper on the [github v4 graphql API](https://developer.github.com/v4).

To use this integration, you’ll first need to create a GitHub App for it.

1. **Create App**: Follow the instructions in [https://developer.github.com/apps/quickstart-guides/setting-up-your-development-environment/](https://developer.github.com/apps/quickstart-guides/setting-up-your-development-environment), You will end up with a private key and App ID, which will be used when configuring the `dagster-github` resource. **Note** you will need to grant your app the relevent permissions for the API requests you want to make, for example to post issues it will need read/write access for the issues repository permission, more info on GitHub application permissions can be found [here](https://developer.github.com/v3/apps/permissions)
2. **Install App**: Follow the instructions in [https://developer.github.com/apps/quickstart-guides/setting-up-your-development-environment/#step-7-install-the-app-on-your-account](https://developer.github.com/apps/quickstart-guides/setting-up-your-development-environment/#step-7-install-the-app-on-your-account)
3. **Find your installation\_id**: You can pull this from the GitHub app administration page, `https://github.com/apps/<app-name>/installations/<installation_id>`. **Note** if your app is installed more than once you can also programatically retrieve these IDs. Sharing your App ID and Installation ID is fine, but make sure that the Private Key for your app is stored securily.

## Posting Issues

Now, you can create issues in GitHub from Dagster with the GitHub resource:

```python
import os

from dagster import job, op
from dagster_github import GithubResource

@op
def github_op(github: GithubResource):
    github.get_client().create_issue(
        repo_name='dagster',
        repo_owner='dagster-io',
        title='Dagster\'s first github issue',
        body='this open source thing seems like a pretty good idea',
    )

@job(resource_defs={
     'github': GithubResource(
         github_app_id=os.getenv('GITHUB_APP_ID'),
         github_app_private_rsa_key=os.getenv('GITHUB_PRIVATE_KEY'),
         github_installation_id=os.getenv('GITHUB_INSTALLATION_ID')
 )})
def github_job():
    github_op()

github_job.execute_in_process()
```

Run the above code, and you’ll see the issue appear in GitHub:

GitHub enterprise users can provide their hostname in the run config. Provide `github_hostname` as part of your github config like below.

```python
GithubResource(
    github_app_id=os.getenv('GITHUB_APP_ID'),
    github_app_private_rsa_key=os.getenv('GITHUB_PRIVATE_KEY'),
    github_installation_id=os.getenv('GITHUB_INSTALLATION_ID'),
    github_hostname=os.getenv('GITHUB_HOSTNAME'),
)
```

By provisioning `GithubResource` as a Dagster resource, you can post to GitHub from within any asset or op execution.

## Executing GraphQL queries

```python
import os

from dagster import job, op
from dagster_github import github_resource

@op
def github_op(github: GithubResource):
    github.get_client().execute(
        query="""
        query get_repo_id($repo_name: String!, $repo_owner: String!) {
            repository(name: $repo_name, owner: $repo_owner) {
                id
            }
        }
        """,
        variables={"repo_name": repo_name, "repo_owner": repo_owner},
    )

@job(resource_defs={
     'github': GithubResource(
         github_app_id=os.getenv('GITHUB_APP_ID'),
         github_app_private_rsa_key=os.getenv('GITHUB_PRIVATE_KEY'),
         github_installation_id=os.getenv('GITHUB_INSTALLATION_ID')
 )})
def github_job():
    github_op()

github_job.execute_in_process()
```

## Resources

`class` dagster\_github.resources.GithubClient [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-github/dagster_github/resources.py#L104)

A client for interacting with the GitHub API.

This client handles authentication and provides methods for making requests to the GitHub API using an authenticated session.

Parameters:

- **client** (*requests.Session*) – The HTTP session used for making requests.
- **app\_id** (*int*) – The GitHub App ID.
- **app\_private\_rsa\_key** (*str*) – The private RSA key for the GitHub App.
- **default\_installation\_id** (*Optional* *\[**int**\]*) – The default installation ID for the GitHub App.
- **hostname** (*Optional* *\[**str**\]*) – The GitHub hostname, defaults to None.
- **installation\_tokens** (*Dict* *\[**Any**,* *Any**\]*) – A dictionary to store installation tokens.
- **app\_token** (*Dict* *\[**str**,* *Any**\]*) – A dictionary to store the app token.

create\_issue [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-github/dagster_github/resources.py#L291)

Create a new issue in the specified GitHub repository.

This method first retrieves the repository ID using the provided repository name and owner, then creates a new issue in that repository with the given title and body.

Parameters:

- **repo\_name** (*str*) – The name of the repository where the issue will be created.
- **repo\_owner** (*str*) – The owner of the repository where the issue will be created.
- **title** (*str*) – The title of the issue.
- **body** (*str*) – The body content of the issue.
- **installation\_id** (*Optional* *\[**int**\]*) – The installation ID to use for authentication.

Returns: The response data from the GitHub API containing the created issue details.Return type: Dict\[str, Any\]Raises: **RuntimeError** – If there are errors in the response from the GitHub API.

create\_pull\_request [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-github/dagster_github/resources.py#L383)

Create a new pull request in the specified GitHub repository.

This method creates a pull request from the head reference (branch) to the base reference (branch) in the specified repositories. It uses the provided title and body for the pull request description.

Parameters:

- **base\_repo\_name** (*str*) – The name of the base repository where the pull request will be created.
- **base\_repo\_owner** (*str*) – The owner of the base repository.
- **base\_ref\_name** (*str*) – The name of the base reference (branch) to which the changes will be merged.
- **head\_repo\_name** (*str*) – The name of the head repository from which the changes will be taken.
- **head\_repo\_owner** (*str*) – The owner of the head repository.
- **head\_ref\_name** (*str*) – The name of the head reference (branch) from which the changes will be taken.
- **title** (*str*) – The title of the pull request.
- **body** (*Optional* *\[**str**\]*) – The body content of the pull request. Defaults to None.
- **maintainer\_can\_modify** (*Optional* *\[**bool**\]*) – Whether maintainers can modify the pull request. Defaults to None.
- **draft** (*Optional* *\[**bool**\]*) – Whether the pull request is a draft. Defaults to None.
- **installation\_id** (*Optional* *\[**int**\]*) – The installation ID to use for authentication.

Returns: The response data from the GitHub API containing the created pull request details.Return type: Dict\[str, Any\]Raises: **RuntimeError** – If there are errors in the response from the GitHub API.

create\_ref [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-github/dagster_github/resources.py#L334)

Create a new reference (branch) in the specified GitHub repository.

This method first retrieves the repository ID and the source reference (branch or tag) using the provided repository name, owner, and source reference. It then creates a new reference (branch) in that repository with the given target name.

Parameters:

- **repo\_name** (*str*) – The name of the repository where the reference will be created.
- **repo\_owner** (*str*) – The owner of the repository where the reference will be created.
- **source** (*str*) – The source reference (branch or tag) from which the new reference will be created.
- **target** (*str*) – The name of the new reference (branch) to be created.
- **installation\_id** (*Optional* *\[**int**\]*) – The installation ID to use for authentication.

Returns: The response data from the GitHub API containing the created reference details.Return type: Dict\[str, Any\]Raises: **RuntimeError** – If there are errors in the response from the GitHub API.

execute [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-github/dagster_github/resources.py#L235)

Execute a GraphQL query against the GitHub API.

This method sends a POST request to the GitHub API with the provided GraphQL query and optional variables. It ensures that the appropriate installation token is included in the request headers.

Parameters:

- **query** (*str*) – The GraphQL query string to be executed.
- **variables** (*Optional* *\[**Dict* *\[**str**,* *Any**\]**\]*) – Optional variables to include in the query.
- **headers** (*Optional* *\[**Dict* *\[**str**,* *Any**\]**\]*) – Optional headers to include in the request.
- **installation\_id** (*Optional* *\[**int**\]*) – The installation ID to use for authentication.

Returns: The response data from the GitHub API.Return type: Dict\[str, Any\]Raises:

- **RuntimeError** – If no installation ID is provided and no default installation ID is set.
- **requests.exceptions.HTTPError** – If the request to the GitHub API fails.

get\_installations [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-github/dagster_github/resources.py#L173)

Retrieve the list of installations for the authenticated GitHub App.

This method makes a GET request to the GitHub API to fetch the installations associated with the authenticated GitHub App. It ensures that the app token is valid and includes it in the request headers.

Parameters: **headers** (*Optional* *\[**Dict* *\[**str**,* *Any**\]**\]*) – Optional headers to include in the request.Returns: A dictionary containing the installations data.Return type: Dict\[str, Any\]Raises: **requests.exceptions.HTTPError** – If the request to the GitHub API fails.

dagster\_github.resources.GithubResource ResourceDefinition [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-github/dagster_github/resources.py#L449)

A resource configuration class for GitHub integration.

This class provides configuration fields for setting up a GitHub Application, including the application ID, private RSA key, installation ID, and hostname.

Parameters:

- **github\_app\_id** (*int*) – The GitHub Application ID. For more information, see [https://developer.github.com/apps/](https://developer.github.com/apps/).
- **github\_app\_private\_rsa\_key** (*str*) – The private RSA key text for the GitHub Application. For more information, see [https://developer.github.com/apps/](https://developer.github.com/apps/).
- **github\_installation\_id** (*Optional* *\[**int**\]*) – The GitHub Application Installation ID. Defaults to None. For more information, see [https://developer.github.com/apps/](https://developer.github.com/apps/).
- **github\_hostname** (*Optional* *\[**str**\]*) – The GitHub hostname. Defaults to api.github.com. For more information, see [https://developer.github.com/apps/](https://developer.github.com/apps/).

## Legacy

dagster\_github.resources.github\_resource ResourceDefinition [\[source\]](https://github.com/dagster-io/dagster/blob/master/python_modules/libraries/dagster-github/dagster_github/resources.py#L523)