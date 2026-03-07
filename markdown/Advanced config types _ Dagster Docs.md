---
title: "Advanced config types | Dagster Docs"
source: "https://docs.dagster.io/guides/operate/configuration/advanced-config-types"
author:
published:
created: 2025-12-12
description: "Dagster's config system supports a variety of more advanced config types."
tags:
  - "clippings"
---
In some cases, you may want to define a more complex [config schema](https://docs.dagster.io/guides/operate/configuration/run-configuration) for your assets and ops. For example, you may want to define a config schema that takes in a list of files or complex data. In this guide, we'll walk through some common patterns for defining more complex config schemas.

Config fields can be annotated with metadata, which can be used to provide additional information about the field, using the Pydantic [`Field`](https://docs.dagster.io/api/dagster/config#dagster.Field) class.

For example, we can annotate a config field with a description, which will be displayed in the documentation for the config field. We can add a value range to a field, which will be validated when config is specified.

```python
import dagster as dg
from pydantic import Field

class MyMetadataConfig(dg.Config):
    person_name: str = Field(description="The name of the person to greet")
    age: int = Field(gt=0, lt=100, description="The age of the person to greet")

# errors, since age is not in the valid range!
MyMetadataConfig(person_name="Alice", age=200)
```

## Defaults and optional config fields

Config fields can have an attached default value. Fields with defaults are not required, meaning they do not need to be specified when constructing the config object.

For example, we can attach a default value of `"hello"` to the `greeting_phrase` field, and can construct `MyAssetConfig` without specifying a phrase. Fields which are marked as `Optional`, such as `person_name`, implicitly have a default value of `None`, but can also be explicitly set to `None` as in the example below:

```python
src/<project_name>/defs/assets.py
from typing import Optional
import dagster as dg
from pydantic import Field

class MyAssetConfig(dg.Config):
    person_name: Optional[str] = None

    # can pass default to pydantic.Field to attach metadata to the field
    greeting_phrase: str = Field(
        default="hello", description="The greeting phrase to use."
    )

@dg.asset
def greeting(config: MyAssetConfig) -> str:
    if config.person_name:
        return f"{config.greeting_phrase} {config.person_name}"
    else:
        return config.greeting_phrase

asset_result = dg.materialize(
    [greeting],
    run_config=dg.RunConfig({"greeting": MyAssetConfig()}),
)
```

### Required config fields

By default, fields which are typed as `Optional` are not required to be specified in the config, and have an implicit default value of `None`. If you want to require that a field be specified in the config, you may use an ellipsis (`...`) to [require that a value be passed](https://docs.pydantic.dev/usage/models/#required-fields).

```python
src/<project_name>/defs/assets.pyfrom typing import Optional
from collections.abc import Callable
import dagster as dg
from pydantic import Field

class MyAssetConfig(dg.Config):
    # ellipsis indicates that even though the type is Optional,
    # an input is required
    person_first_name: Optional[str] = ...

    # ellipsis can also be used with pydantic.Field to attach metadata
    person_last_name: Optional[Callable] = Field(
        default=..., description="The last name of the person to greet"
    )

@dg.asset
def goodbye(config: MyAssetConfig) -> str:
    full_name = f"{config.person_first_name} {config.person_last_name}".strip()
    if full_name:
        return f"Goodbye, {full_name}"
    else:
        return "Goodbye"

# errors, since person_first_name and person_last_name are required
goodbye(MyAssetConfig())

# works, since both person_first_name and person_last_name are provided
goodbye(MyAssetConfig(person_first_name="Alice", person_last_name=None))
```

## Basic data structures

Basic Python data structures can be used in your config schemas along with nested versions of these data structures. The data structures which can be used are:

- `List`
- `Dict`
- `Mapping`

For example, we can define a config schema that takes in a list of user names and a mapping of user names to user scores.

```python
src/<project_name>/defs/assets.pyimport dagster as dg

class MyDataStructuresConfig(dg.Config):
    user_names: list[str]
    user_scores: dict[str, int]

@dg.asset
def scoreboard(config: MyDataStructuresConfig): ...

result = dg.materialize(
    [scoreboard],
    run_config=dg.RunConfig(
        {
            "scoreboard": MyDataStructuresConfig(
                user_names=["Alice", "Bob"],
                user_scores={"Alice": 10, "Bob": 20},
            )
        }
    ),
)
```

## Nested schemas

Schemas can be nested in one another, or in basic Python data structures.

Here, we define a schema which contains a mapping of user names to complex user data objects.

```python
src/<project_name>/defs/assets.pyimport dagster as dg

class UserData(dg.Config):
    age: int
    email: str
    profile_picture_url: str

class MyNestedConfig(dg.Config):
    user_data: dict[str, UserData]

@dg.asset
def average_age(config: MyNestedConfig): ...

result = dg.materialize(
    [average_age],
    run_config=dg.RunConfig(
        {
            "average_age": MyNestedConfig(
                user_data={
                    "Alice": UserData(
                        age=10,
                        email="alice@gmail.com",
                        profile_picture_url=...,
                    ),
                    "Bob": UserData(
                        age=20,
                        email="bob@gmail.com",
                        profile_picture_url=...,
                    ),
                }
            )
        }
    ),
)
```

## Permissive schemas

By default, `Config` schemas are strict, meaning that they will only accept fields that are explicitly defined in the schema. This can be cumbersome if you want to allow users to specify arbitrary fields in their config. For this purpose, you can use the `PermissiveConfig` base class, which allows arbitrary fields to be specified in the config.

```python
src/<project_name>/defs/assets.py
import dagster as dg
from typing import Optional
import requests

class FilterConfig(dg.PermissiveConfig):
    title: Optional[str] = None
    description: Optional[str] = None

@dg.asset
def filtered_listings(config: FilterConfig):
    # extract all config fields, including those not defined in the schema
    url_params = config.dict()
    return requests.get("https://my-api.com/listings", params=url_params).json()

# can pass in any fields, including those not defined in the schema
filtered_listings(FilterConfig(title="hotel", beds=4))
```

## Union types

Union types are supported using Pydantic [discriminated unions](https://docs.pydantic.dev/usage/types/#discriminated-unions-aka-tagged-unions). Each union type must be a subclass of [`Config`](https://docs.dagster.io/api/dagster/config#dagster.Config). The `discriminator` argument to [`Field`](https://docs.dagster.io/api/dagster/config#dagster.Field) specifies the field that will be used to determine which union type to use. Discriminated unions provide comparable functionality to the `Selector` type in the legacy Dagster config APIs.

Here, we define a config schema which takes in a `pet` field, which can be either a `Cat` or a `Dog`, as indicated by the `pet_type` field.

```python
src/<project_name>/defs/assets.py
import dagster as dg
from pydantic import Field
from typing import Union
from typing import Literal

class Cat(dg.Config):
    pet_type: Literal["cat"] = "cat"
    meows: int

class Dog(dg.Config):
    pet_type: Literal["dog"] = "dog"
    barks: float

class ConfigWithUnion(dg.Config):
    pet: Union[Cat, Dog] = Field(discriminator="pet_type")

@dg.asset
def pet_stats(config: ConfigWithUnion):
    if isinstance(config.pet, Cat):
        return f"Cat meows {config.pet.meows} times"
    else:
        return f"Dog barks {config.pet.barks} times"

result = dg.materialize(
    [pet_stats],
    run_config=dg.RunConfig(
        {
            "pet_stats": ConfigWithUnion(
                pet=Cat(meows=10),
            )
        }
    ),
)
```

### YAML and config dictionary representations of union types

The YAML or config dictionary representation of a discriminated union is structured slightly differently than the Python representation. In the YAML representation, the discriminator key is used as the key for the union type's dictionary. For example, a `Cat` object would be represented as:

```yaml
pet:
  cat:
    meows: 10
```

In the config dictionary representation, the same pattern is used:

```python
{
    "pet": {
        "cat": {
            "meows": 10,
        }
    }
}
```

## Enum types

Python enums which subclass `Enum` are supported as config fields. Here, we define a schema that takes in a list of users, whose roles are specified as enum values:

```python
src/<project_name>/defs/jobs.py
import dagster as dg
from enum import Enum

class UserPermissions(Enum):
    GUEST = "guest"
    MEMBER = "member"
    ADMIN = "admin"

class ProcessUsersConfig(dg.Config):
    users_list: dict[str, UserPermissions]

@dg.op
def process_users(config: ProcessUsersConfig):
    for user, permission in config.users_list.items():
        if permission == UserPermissions.ADMIN:
            print(f"{user} is an admin")

@dg.job
def process_users_job():
    process_users()

op_result = process_users_job.execute_in_process(
    run_config=dg.RunConfig(
        {
            "process_users": ProcessUsersConfig(
                users_list={
                    "Bob": UserPermissions.GUEST,
                    "Alice": UserPermissions.ADMIN,
                }
            )
        }
    ),
)
```

### YAML and config dictionary representations of enum types

The YAML or config dictionary representation of a Python enum uses the enum's name. For example, a YAML specification of the user list above would be:

```yaml
users_list:
  Bob: GUEST
  Alice: ADMIN
```

In the config dictionary representation, the same pattern is used:

```python
{
    "users_list": {
        "Bob": "GUEST",
        "Alice": "ADMIN",
    }
}
```

## Validated config fields

Config fields can have custom validation logic applied using [Pydantic validators](https://docs.pydantic.dev/usage/validators). Pydantic validators are defined as methods on the config class, and are decorated with the `@validator` decorator. These validators are triggered when the config class is instantiated. In the case of config defined at runtime, a failing validator will not prevent the launch button from being pressed, but will raise an exception and prevent run start.

Here, we define some validators on a configured user's name and username, which will throw exceptions if incorrect values are passed in the launchpad or from a schedule or sensor.

```python
src/<project_name>/defs/jobs.py
    import dagster as dg
    from pydantic import validator

    class UserConfig(dg.Config):
        name: str
        username: str

        @validator("name")
        def name_must_contain_space(cls, v):
            if " " not in v:
                raise ValueError("must contain a space")
            return v.title()

        @validator("username")
        def username_alphanumeric(cls, v):
            assert v.isalnum(), "must be alphanumeric"
            return v

    executed = {}

    @dg.op
    def greet_user(config: UserConfig) -> None:
        print(f"Hello {config.name}!")
        executed["greet_user"] = True

    @dg.job
    def greet_user_job() -> None:
        greet_user()

    # Input is valid, so this will work
    op_result = greet_user_job.execute_in_process(
        run_config=dg.RunConfig(
            {"greet_user": UserConfig(name="Alice Smith", username="alice123")}
        ),
    )

    # Name has no space, so this will fail
    op_result = greet_user_job.execute_in_process(
        run_config=dg.RunConfig(
            {"greet_user": UserConfig(name="John", username="johndoe44")}
        ),
    )
```

Ask Dagster AI