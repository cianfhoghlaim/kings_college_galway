# Pydantic v2 Comprehensive Guide

A comprehensive reference for Pydantic v2 covering core features, validation patterns, type systems, advanced patterns, and LLM integration.

---

## Table of Contents

1. [Core Features](#1-core-features)
2. [Validation Patterns](#2-validation-patterns)
3. [Ontologies and Type System](#3-ontologies-and-type-system)
4. [Advanced Patterns](#4-advanced-patterns)
5. [LLM Integration Patterns](#5-llm-integration-patterns)

---

## 1. Core Features

### 1.1 BaseModel and Model Configuration

The `BaseModel` class is the foundation of Pydantic. Models are defined by inheriting from `BaseModel` and declaring fields as annotated class attributes.

```python
from pydantic import BaseModel, ConfigDict

class User(BaseModel):
    model_config = ConfigDict(
        strict=False,           # Enable/disable strict mode
        frozen=False,           # Make model immutable
        extra='forbid',         # 'allow', 'forbid', or 'ignore' extra fields
        validate_assignment=True,  # Validate on attribute assignment
        populate_by_name=True,  # Allow using field names alongside aliases
        str_strip_whitespace=True,  # Strip whitespace from strings
        str_min_length=0,       # Minimum string length
        use_enum_values=True,   # Use enum values instead of enum objects
    )

    id: int
    name: str
    email: str

# Usage
user = User(id=1, name="John", email="john@example.com")
print(user.model_dump())  # {'id': 1, 'name': 'John', 'email': 'john@example.com'}
```

**Key Model Methods:**

```python
# Validation
User.model_validate({'id': 1, 'name': 'John', 'email': 'john@example.com'})
User.model_validate_json('{"id": 1, "name": "John", "email": "john@example.com"}')

# Serialization
user.model_dump()                    # Returns dict
user.model_dump_json()               # Returns JSON string
user.model_dump(exclude={'email'})   # Exclude fields
user.model_dump(by_alias=True)       # Use aliases

# Schema generation
User.model_json_schema()             # Get JSON Schema
User.model_rebuild()                 # Rebuild model schema
```

### 1.2 Field Types and Validators

Pydantic supports a wide range of field types with automatic validation:

```python
from datetime import datetime, date
from typing import Optional, List, Dict
from pydantic import BaseModel, Field, EmailStr, HttpUrl
from decimal import Decimal
from uuid import UUID

class Product(BaseModel):
    # Basic types
    id: int
    name: str
    price: float
    active: bool

    # Optional and default values
    description: Optional[str] = None
    quantity: int = Field(default=0)

    # Collections
    tags: List[str] = []
    metadata: Dict[str, str] = {}

    # Special types
    uuid: UUID
    email: EmailStr
    website: HttpUrl

    # Date/time types
    created_at: datetime
    launch_date: date

    # Numeric with constraints
    rating: float = Field(ge=0, le=5)
    stock: int = Field(ge=0)
    discount: Decimal = Field(max_digits=5, decimal_places=2)
```

### 1.3 Data Parsing and Serialization

Pydantic provides flexible serialization options:

```python
from pydantic import BaseModel, Field

class User(BaseModel):
    user_id: int = Field(serialization_alias='userId')
    full_name: str = Field(serialization_alias='fullName')
    email_address: str = Field(serialization_alias='email', exclude=True)

user = User(user_id=1, full_name='John Doe', email_address='john@example.com')

# Standard dump
print(user.model_dump())
# {'user_id': 1, 'full_name': 'John Doe'}

# With aliases
print(user.model_dump(by_alias=True))
# {'userId': 1, 'fullName': 'John Doe'}

# JSON serialization
print(user.model_dump_json(by_alias=True, indent=2))

# Include/exclude specific fields
print(user.model_dump(include={'user_id', 'full_name'}))

# Exclude unset, defaults, or None values
print(user.model_dump(exclude_unset=True))
print(user.model_dump(exclude_defaults=True))
print(user.model_dump(exclude_none=True))
```

### 1.4 Type Coercion and Strict Mode

By default, Pydantic coerces values to the correct type. Strict mode disables this behavior:

```python
from pydantic import BaseModel, ConfigDict, Field, ValidationError

# Lax mode (default) - allows coercion
class LaxModel(BaseModel):
    value: int

print(LaxModel(value="42").value)  # 42 (string coerced to int)
print(LaxModel(value=42.9).value)  # 42 (float coerced to int)

# Strict mode - model level
class StrictModel(BaseModel):
    model_config = ConfigDict(strict=True)
    value: int

try:
    StrictModel(value="42")  # ValidationError!
except ValidationError as e:
    print(e)

# Strict mode - field level
class MixedModel(BaseModel):
    strict_value: int = Field(strict=True)
    lax_value: int = Field(strict=False)

# Per-call strict validation
class FlexibleModel(BaseModel):
    value: int

FlexibleModel.model_validate({'value': '42'}, strict=True)  # ValidationError!

# Strict type aliases
from pydantic import StrictInt, StrictStr, StrictFloat, StrictBool

class StrictTypesModel(BaseModel):
    count: StrictInt      # Only accepts int, not bool or float
    name: StrictStr       # Only accepts str
    price: StrictFloat    # Only accepts float, not int
    active: StrictBool    # Only accepts bool
```

### 1.5 Computed Fields and Property Decorators

Computed fields are calculated from other fields and included in serialization:

```python
from functools import cached_property
from pydantic import BaseModel, computed_field

class Rectangle(BaseModel):
    width: float
    height: float

    @computed_field
    @property
    def area(self) -> float:
        """Calculate the area of the rectangle."""
        return self.width * self.height

    @computed_field
    @property
    def perimeter(self) -> float:
        """Calculate the perimeter of the rectangle."""
        return 2 * (self.width + self.height)

    # With cached_property for expensive computations
    @computed_field
    @cached_property
    def diagonal(self) -> float:
        """Calculate diagonal using Pythagorean theorem."""
        return (self.width ** 2 + self.height ** 2) ** 0.5

rect = Rectangle(width=3, height=4)
print(rect.model_dump())
# {'width': 3.0, 'height': 4.0, 'area': 12.0, 'perimeter': 14.0, 'diagonal': 5.0}

# Computed field with setter
class Square(BaseModel):
    model_config = ConfigDict(validate_assignment=True)
    side: float

    @computed_field
    @property
    def area(self) -> float:
        return self.side ** 2

    @area.setter
    def area(self, new_area: float) -> None:
        self.side = new_area ** 0.5

square = Square(side=4)
square.area = 25  # Sets side to 5.0
print(square.side)  # 5.0

# Computed fields with custom metadata
class Product(BaseModel):
    price: float
    tax_rate: float = 0.1

    @computed_field(
        alias='totalPrice',
        description='Price including tax',
        repr=False
    )
    @property
    def total(self) -> float:
        return self.price * (1 + self.tax_rate)
```

---

## 2. Validation Patterns

### 2.1 Field Validators (Before, After, Wrap)

Field validators allow custom validation logic on individual fields:

```python
from typing import Any
from pydantic import BaseModel, field_validator, ValidationInfo

class User(BaseModel):
    name: str
    email: str
    age: int

    # After validator (default) - receives validated value
    @field_validator('name')
    @classmethod
    def name_must_not_be_empty(cls, v: str) -> str:
        if not v.strip():
            raise ValueError('Name cannot be empty')
        return v.title()

    # Before validator - receives raw input
    @field_validator('email', mode='before')
    @classmethod
    def normalize_email(cls, v: Any) -> str:
        if isinstance(v, str):
            return v.lower().strip()
        return v

    # Validator with field info access
    @field_validator('age')
    @classmethod
    def check_age(cls, v: int, info: ValidationInfo) -> int:
        if v < 0:
            raise ValueError('Age must be positive')
        return v

    # Apply to multiple fields
    @field_validator('name', 'email')
    @classmethod
    def not_empty(cls, v: str) -> str:
        if not v:
            raise ValueError('Field cannot be empty')
        return v

# Wrap validator - full control over validation
from pydantic import ValidatorFunctionWrapHandler

class WrapExample(BaseModel):
    value: int

    @field_validator('value', mode='wrap')
    @classmethod
    def wrap_validator(
        cls,
        v: Any,
        handler: ValidatorFunctionWrapHandler
    ) -> int:
        # Pre-processing
        if isinstance(v, str) and v.startswith('$'):
            v = v[1:]

        # Call default validation
        result = handler(v)

        # Post-processing
        return abs(result)

print(WrapExample(value='$-42').value)  # 42
```

### 2.2 Model Validators

Model validators validate the entire model at once:

```python
from typing import Any, Self
from pydantic import BaseModel, model_validator, ValidationInfo

class UserRegistration(BaseModel):
    username: str
    password: str
    password_confirm: str

    # Before model validator - receives raw input
    @model_validator(mode='before')
    @classmethod
    def check_raw_data(cls, data: Any) -> Any:
        if isinstance(data, dict):
            # Transform or validate raw input
            if 'user' in data and 'name' not in data:
                data['username'] = data.pop('user')
        return data

    # After model validator - receives model instance
    @model_validator(mode='after')
    def check_passwords_match(self) -> Self:
        if self.password != self.password_confirm:
            raise ValueError('Passwords do not match')
        return self

class DateRange(BaseModel):
    start_date: str
    end_date: str

    # Wrap model validator - full control
    @model_validator(mode='wrap')
    @classmethod
    def validate_dates(cls, values: Any, handler) -> 'DateRange':
        # Pre-validation logic
        if isinstance(values, dict):
            # Normalize date formats
            pass

        # Run standard validation
        instance = handler(values)

        # Post-validation logic
        if instance.start_date > instance.end_date:
            raise ValueError('start_date must be before end_date')

        return instance
```

### 2.3 Custom Types and Constraints

Create reusable constrained types using `Annotated`:

```python
from typing import Annotated
from pydantic import BaseModel, Field, AfterValidator, BeforeValidator
from pydantic.types import StringConstraints

# String constraints
Username = Annotated[
    str,
    StringConstraints(
        min_length=3,
        max_length=50,
        pattern=r'^[a-zA-Z0-9_]+$'
    )
]

# Numeric constraints
PositiveInt = Annotated[int, Field(gt=0)]
Percentage = Annotated[float, Field(ge=0, le=100)]
Rating = Annotated[int, Field(ge=1, le=5)]

# Custom validator as type
def validate_even(v: int) -> int:
    if v % 2 != 0:
        raise ValueError('Value must be even')
    return v

EvenInt = Annotated[int, AfterValidator(validate_even)]

# Chained validators
def strip_spaces(v: str) -> str:
    return v.strip()

def to_lowercase(v: str) -> str:
    return v.lower()

NormalizedStr = Annotated[
    str,
    BeforeValidator(strip_spaces),
    AfterValidator(to_lowercase)
]

class Product(BaseModel):
    name: Username
    price: PositiveInt
    discount: Percentage
    rating: Rating
    sku: NormalizedStr
    batch_size: EvenInt

# Legacy constraint functions (deprecated in v3)
from pydantic import conint, constr, confloat

class LegacyModel(BaseModel):
    # These still work but will be deprecated
    count: conint(ge=0, le=100)
    name: constr(min_length=1, max_length=50)
    rate: confloat(ge=0.0, le=1.0)
```

### 2.4 Discriminated Unions

Discriminated unions use a field value to determine the correct type:

```python
from typing import Literal, Union
from typing_extensions import Annotated
from pydantic import BaseModel, Field

# Simple discriminated union
class Cat(BaseModel):
    pet_type: Literal['cat']
    name: str
    meows: int

class Dog(BaseModel):
    pet_type: Literal['dog']
    name: str
    barks: float

class Owner(BaseModel):
    pet: Annotated[
        Union[Cat, Dog],
        Field(discriminator='pet_type')
    ]

# Validates correctly based on pet_type
owner1 = Owner(pet={'pet_type': 'cat', 'name': 'Whiskers', 'meows': 5})
owner2 = Owner(pet={'pet_type': 'dog', 'name': 'Rex', 'barks': 3.5})

# Nested discriminated unions
class BlackCat(BaseModel):
    pet_type: Literal['cat']
    color: Literal['black']
    name: str

class WhiteCat(BaseModel):
    pet_type: Literal['cat']
    color: Literal['white']
    name: str

class GermanShepherd(BaseModel):
    pet_type: Literal['dog']
    breed: Literal['german_shepherd']
    name: str

Pet = Annotated[
    Union[
        Annotated[Union[BlackCat, WhiteCat], Field(discriminator='color')],
        GermanShepherd
    ],
    Field(discriminator='pet_type')
]

# Callable discriminator for complex cases
from pydantic import Discriminator

def get_discriminator_value(v: Any) -> str:
    if isinstance(v, dict):
        return v.get('type', 'unknown')
    return getattr(v, 'type', 'unknown')

class Item(BaseModel):
    items: list[
        Annotated[
            Union[Cat, Dog],
            Discriminator(get_discriminator_value)
        ]
    ]
```

### 2.5 Recursive Models

Models that reference themselves for nested structures:

```python
from typing import Optional, List
from pydantic import BaseModel

# Self-referencing model
class TreeNode(BaseModel):
    value: int
    children: List['TreeNode'] = []

# After model definition, rebuild to resolve forward references
TreeNode.model_rebuild()

tree = TreeNode(
    value=1,
    children=[
        TreeNode(value=2, children=[
            TreeNode(value=4),
            TreeNode(value=5)
        ]),
        TreeNode(value=3)
    ]
)

# Linked list pattern
class LinkedNode(BaseModel):
    value: str
    next: Optional['LinkedNode'] = None

LinkedNode.model_rebuild()

# File system structure
class FileSystemItem(BaseModel):
    name: str
    is_directory: bool
    children: Optional[List['FileSystemItem']] = None

FileSystemItem.model_rebuild()

# JSON-like recursive structure
from typing import Union, Dict

JsonValue = Union[
    str, int, float, bool, None,
    List['JsonValue'],
    Dict[str, 'JsonValue']
]

class JsonContainer(BaseModel):
    data: JsonValue

# Mutual recursion
class Person(BaseModel):
    name: str
    friends: List['Person'] = []
    employer: Optional['Company'] = None

class Company(BaseModel):
    name: str
    employees: List[Person] = []

Person.model_rebuild()
Company.model_rebuild()
```

---

## 3. Ontologies and Type System

### 3.1 Pydantic's Type Annotation System

Pydantic leverages Python's type annotation system with extended support:

```python
from typing import (
    Any, Union, Optional, List, Dict, Tuple, Set,
    FrozenSet, Literal, TypedDict, NamedTuple
)
from pydantic import BaseModel

class ComprehensiveTypes(BaseModel):
    # Union types
    string_or_int: Union[str, int]
    optional_str: Optional[str]  # Same as Union[str, None]

    # Python 3.10+ syntax
    modern_union: str | int | None

    # Collections
    string_list: List[str]
    int_set: Set[int]
    frozen_set: FrozenSet[str]
    tuple_fixed: Tuple[int, str, float]
    tuple_variable: Tuple[int, ...]

    # Nested structures
    nested_dict: Dict[str, List[int]]

    # Literal types
    status: Literal['pending', 'active', 'completed']

    # Any type
    flexible: Any

# TypedDict integration
class UserDict(TypedDict):
    name: str
    age: int

class Container(BaseModel):
    user: UserDict

# NamedTuple integration
class Point(NamedTuple):
    x: float
    y: float

class Shape(BaseModel):
    origin: Point
    points: List[Point]
```

### 3.2 Generic Models

Create reusable generic models with type parameters:

```python
from typing import TypeVar, Generic, List, Optional
from pydantic import BaseModel, ValidationError

# Define type variables
T = TypeVar('T')
K = TypeVar('K')
V = TypeVar('V')

# Basic generic model
class Response(BaseModel, Generic[T]):
    data: T
    status: int
    message: str

# Usage with different types
int_response = Response[int](data=42, status=200, message='OK')
str_response = Response[str](data='hello', status=200, message='OK')

# Generic with multiple type parameters
class KeyValuePair(BaseModel, Generic[K, V]):
    key: K
    value: V

pair = KeyValuePair[str, int](key='count', value=42)

# Bounded type variables
from pydantic import BaseModel

class Animal(BaseModel):
    name: str

class Dog(Animal):
    breed: str

AnimalT = TypeVar('AnimalT', bound=Animal)

class Shelter(BaseModel, Generic[AnimalT]):
    animals: List[AnimalT]

dog_shelter = Shelter[Dog](animals=[
    Dog(name='Rex', breed='German Shepherd')
])

# Constrained type variables
NumericT = TypeVar('NumericT', int, float)

class Stats(BaseModel, Generic[NumericT]):
    values: List[NumericT]

    @property
    def average(self) -> float:
        return sum(self.values) / len(self.values)

# Nested generics
class Page(BaseModel, Generic[T]):
    items: List[T]
    page: int
    total: int

class PaginatedUsers(BaseModel):
    result: Page[Response[dict]]

# Inheriting from generic models
class TimestampedResponse(Response[T], Generic[T]):
    timestamp: str

response = TimestampedResponse[dict](
    data={'key': 'value'},
    status=200,
    message='OK',
    timestamp='2024-01-01T00:00:00Z'
)
```

### 3.3 TypeAdapter

`TypeAdapter` enables validation and serialization without creating a model:

```python
from typing import List, Dict, Union
from pydantic import TypeAdapter, ValidationError
from datetime import datetime

# Basic TypeAdapter usage
int_adapter = TypeAdapter(int)
print(int_adapter.validate_python('42'))  # 42

list_adapter = TypeAdapter(List[int])
print(list_adapter.validate_python([1, 2, 3]))  # [1, 2, 3]

# JSON validation
json_data = '[1, 2, 3]'
print(list_adapter.validate_json(json_data))  # [1, 2, 3]

# Complex type validation
from pydantic import BaseModel

class User(BaseModel):
    name: str
    age: int

users_adapter = TypeAdapter(List[User])
users = users_adapter.validate_python([
    {'name': 'Alice', 'age': 30},
    {'name': 'Bob', 'age': 25}
])

# Union type adapter
union_adapter = TypeAdapter(Union[int, str])
print(union_adapter.validate_python(42))      # 42
print(union_adapter.validate_python('hello')) # 'hello'

# JSON Schema generation
print(list_adapter.json_schema())
# {'items': {'type': 'integer'}, 'type': 'array'}

# Serialization
dt_adapter = TypeAdapter(datetime)
dt = datetime(2024, 1, 1, 12, 0, 0)
print(dt_adapter.dump_python(dt, mode='json'))
# '2024-01-01T12:00:00'

# Dict type adapter
dict_adapter = TypeAdapter(Dict[str, int])
print(dict_adapter.validate_python({'a': 1, 'b': 2}))

# TypeAdapter with discriminated union
from typing_extensions import Annotated
from pydantic import Field

class Cat(BaseModel):
    pet_type: str = 'cat'
    meows: int

class Dog(BaseModel):
    pet_type: str = 'dog'
    barks: float

PetUnion = Annotated[
    Union[Cat, Dog],
    Field(discriminator='pet_type')
]
pet_adapter = TypeAdapter(PetUnion)

cat = pet_adapter.validate_python({'pet_type': 'cat', 'meows': 5})
print(type(cat))  # <class 'Cat'>

# Performance: Reuse TypeAdapter instances
# Bad - creates new adapter each time
for item in items:
    TypeAdapter(List[int]).validate_python(item)

# Good - reuse adapter
adapter = TypeAdapter(List[int])
for item in items:
    adapter.validate_python(item)
```

### 3.4 JSON Schema Generation

Generate JSON Schema from Pydantic models:

```python
from typing import List, Optional
from pydantic import BaseModel, Field
import json

class Address(BaseModel):
    street: str = Field(description='Street address')
    city: str
    country: str = Field(default='USA')

class Person(BaseModel):
    """A person with contact information."""
    name: str = Field(
        min_length=1,
        max_length=100,
        description='Full name of the person'
    )
    age: int = Field(
        ge=0,
        le=150,
        description='Age in years'
    )
    email: Optional[str] = Field(
        default=None,
        pattern=r'^[\w\.-]+@[\w\.-]+\.\w+$',
        description='Email address'
    )
    addresses: List[Address] = Field(
        default_factory=list,
        description='List of addresses'
    )

# Generate JSON Schema
schema = Person.model_json_schema()
print(json.dumps(schema, indent=2))

# Schema with different modes
validation_schema = Person.model_json_schema(mode='validation')
serialization_schema = Person.model_json_schema(mode='serialization')

# Custom schema generation
from pydantic.json_schema import GenerateJsonSchema

class CustomJsonSchema(GenerateJsonSchema):
    def generate(self, schema, mode='validation'):
        json_schema = super().generate(schema, mode=mode)
        json_schema['$schema'] = 'https://json-schema.org/draft/2020-12/schema'
        return json_schema

custom_schema = Person.model_json_schema(
    schema_generator=CustomJsonSchema
)

# Schema for multiple models
from pydantic.json_schema import models_json_schema

class User(BaseModel):
    username: str
    email: str

class Product(BaseModel):
    name: str
    price: float

_, top_level_schema = models_json_schema(
    [(User, 'validation'), (Product, 'validation')],
    title='My API Schema'
)

# TypeAdapter JSON Schema
from pydantic import TypeAdapter

adapter = TypeAdapter(List[int])
print(adapter.json_schema())
# {'items': {'type': 'integer'}, 'type': 'array'}
```

### 3.5 Dataclass Integration

Pydantic's `@dataclass` decorator adds validation to standard dataclasses:

```python
from pydantic import ConfigDict
from pydantic.dataclasses import dataclass
from typing import List, Optional

# Basic Pydantic dataclass
@dataclass
class User:
    id: int
    name: str
    email: str

user = User(id=1, name='John', email='john@example.com')

# With configuration
@dataclass(config=ConfigDict(
    strict=True,
    validate_assignment=True
))
class StrictUser:
    id: int
    name: str

# Using __pydantic_config__
@dataclass
class ConfiguredUser:
    __pydantic_config__ = ConfigDict(
        extra='forbid',
        frozen=True
    )
    id: int
    name: str

# Nested dataclasses
@dataclass
class Address:
    street: str
    city: str

@dataclass
class Person:
    name: str
    addresses: List[Address]

person = Person(
    name='John',
    addresses=[
        Address(street='123 Main St', city='NYC'),
        Address(street='456 Oak Ave', city='LA')
    ]
)

# Mixing with stdlib dataclasses
from dataclasses import dataclass as stdlib_dataclass

@stdlib_dataclass
class StandardAddress:
    street: str
    city: str

@dataclass
class PersonWithStdlib:
    name: str
    address: StandardAddress  # Pydantic validates nested stdlib dataclass

# Conversion to dict/JSON
from pydantic import TypeAdapter

@dataclass
class Product:
    name: str
    price: float

product = Product(name='Widget', price=9.99)
adapter = TypeAdapter(Product)

# Serialize
print(adapter.dump_python(product))  # {'name': 'Widget', 'price': 9.99}
print(adapter.dump_json(product))    # b'{"name":"Widget","price":9.99}'

# Get JSON Schema
print(adapter.json_schema())
```

---

## 4. Advanced Patterns

### 4.1 Settings Management with pydantic-settings

`pydantic-settings` provides configuration management from environment variables:

```python
# pip install pydantic-settings
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

class DatabaseSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file='.env',
        env_file_encoding='utf-8',
        env_prefix='DB_',            # All vars prefixed with DB_
        env_nested_delimiter='__',   # For nested settings
        case_sensitive=False,
        extra='ignore'
    )

    host: str = 'localhost'
    port: int = 5432
    name: str = Field(alias='database')
    user: str
    password: str

class AppSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file='.env',
        env_prefix='APP_'
    )

    debug: bool = False
    secret_key: str
    api_version: str = 'v1'

    # Nested settings
    database: DatabaseSettings = Field(default_factory=DatabaseSettings)

# Usage
# Environment variables: APP_DEBUG=true, APP_SECRET_KEY=xxx, DB_HOST=prod-db
settings = AppSettings()

# Multiple environment files (priority: later files override earlier)
class MultiEnvSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=('.env', '.env.local', '.env.production')
    )
    api_key: str

# Secrets directory (for Docker secrets, etc.)
class SecretSettings(BaseSettings):
    model_config = SettingsConfigDict(
        secrets_dir='/run/secrets'
    )
    db_password: str

# Multiple sources with AliasChoices
from pydantic_settings import AliasChoices, AliasPath

class FlexibleSettings(BaseSettings):
    # Accept from multiple env var names
    api_key: str = Field(
        validation_alias=AliasChoices(
            'API_KEY',
            'OPENAI_API_KEY',
            AliasPath('credentials', 'api_key')
        )
    )

# Custom settings sources
from pydantic_settings import (
    BaseSettings,
    PydanticBaseSettingsSource,
)

class CustomSettings(BaseSettings):
    @classmethod
    def settings_customise_sources(
        cls,
        settings_cls,
        init_settings,
        env_settings,
        dotenv_settings,
        file_secret_settings,
    ):
        return (
            init_settings,
            env_settings,
            dotenv_settings,
            file_secret_settings,
        )
```

### 4.2 Custom Serializers/Deserializers

Control how fields are serialized and deserialized:

```python
from typing import Any, Annotated
from datetime import datetime
from pydantic import (
    BaseModel,
    field_serializer,
    field_validator,
    PlainSerializer,
    WrapSerializer,
    SerializerFunctionWrapHandler
)

class User(BaseModel):
    name: str
    created_at: datetime
    tags: list[str]

    # Field serializer
    @field_serializer('created_at')
    def serialize_datetime(self, dt: datetime) -> str:
        return dt.strftime('%Y-%m-%d %H:%M:%S')

    # Serialize multiple fields
    @field_serializer('name', 'tags')
    def uppercase(self, value):
        if isinstance(value, str):
            return value.upper()
        return [v.upper() for v in value]

    # Mode-specific serialization
    @field_serializer('created_at', when_used='json')
    def serialize_for_json(self, dt: datetime) -> str:
        return dt.isoformat()

# Using Annotated with PlainSerializer
def serialize_to_hex(value: int) -> str:
    return hex(value)

HexInt = Annotated[int, PlainSerializer(serialize_to_hex)]

class HexModel(BaseModel):
    value: HexInt

print(HexModel(value=255).model_dump())  # {'value': '0xff'}

# WrapSerializer for complex transformations
def wrap_serialize(value: Any, handler: SerializerFunctionWrapHandler) -> Any:
    # Pre-process
    result = handler(value)
    # Post-process
    if isinstance(result, str):
        return f"[{result}]"
    return result

WrappedStr = Annotated[str, WrapSerializer(wrap_serialize)]

# Model-level serializer
from pydantic import model_serializer

class CustomModel(BaseModel):
    x: int
    y: int

    @model_serializer
    def serialize_model(self) -> dict:
        return {
            'coordinates': f"({self.x}, {self.y})",
            'sum': self.x + self.y
        }

print(CustomModel(x=1, y=2).model_dump())
# {'coordinates': '(1, 2)', 'sum': 3}

# Custom deserialization with validators
class Product(BaseModel):
    price_cents: int

    @field_validator('price_cents', mode='before')
    @classmethod
    def parse_price(cls, v):
        if isinstance(v, str) and v.startswith('$'):
            # Convert "$10.50" to 1050 cents
            return int(float(v[1:]) * 100)
        return v
```

### 4.3 Alias and Field Metadata

Configure field aliases for validation and serialization:

```python
from pydantic import BaseModel, Field, AliasPath, AliasChoices, AliasGenerator
from pydantic.alias_generators import to_camel, to_snake

class User(BaseModel):
    # Simple alias (used for both validation and serialization)
    user_id: int = Field(alias='userId')

    # Separate validation and serialization aliases
    full_name: str = Field(
        validation_alias='fullName',      # Accept 'fullName' in input
        serialization_alias='full_name'   # Output as 'full_name'
    )

    # Multiple validation aliases
    email: str = Field(
        validation_alias=AliasChoices('email', 'emailAddress', 'e-mail')
    )

# Validation with alias
user = User.model_validate({
    'userId': 1,
    'fullName': 'John Doe',
    'email': 'john@example.com'
})

# AliasPath for nested data
class NestedUser(BaseModel):
    first_name: str = Field(validation_alias=AliasPath('names', 0))
    last_name: str = Field(validation_alias=AliasPath('names', 1))
    city: str = Field(validation_alias=AliasPath('address', 'city'))

data = {
    'names': ['John', 'Doe'],
    'address': {'city': 'NYC', 'zip': '10001'}
}
user = NestedUser.model_validate(data)

# AliasGenerator for automatic alias generation
class CamelCaseModel(BaseModel):
    model_config = {
        'alias_generator': AliasGenerator(
            validation_alias=to_camel,
            serialization_alias=to_camel
        ),
        'populate_by_name': True  # Allow both field name and alias
    }

    user_name: str
    email_address: str

# Accepts: {'userName': 'John', 'emailAddress': 'john@example.com'}
# Also accepts: {'user_name': 'John', 'email_address': 'john@example.com'}

# Field metadata for documentation
class Product(BaseModel):
    name: str = Field(
        title='Product Name',
        description='The display name of the product',
        examples=['Widget', 'Gadget'],
        json_schema_extra={'x-custom': 'value'}
    )
    price: float = Field(
        ge=0,
        description='Price in USD',
        examples=[9.99, 19.99],
        deprecated=True  # Mark as deprecated
    )
```

### 4.4 Model Inheritance and Composition

Create complex models through inheritance and composition:

```python
from typing import Optional
from pydantic import BaseModel, ConfigDict, Field

# Basic inheritance
class BaseItem(BaseModel):
    model_config = ConfigDict(
        str_strip_whitespace=True,
        extra='forbid'
    )

    id: int
    created_at: str

class Product(BaseItem):
    name: str
    price: float

class Service(BaseItem):
    name: str
    hourly_rate: float

# Config inheritance and override
class StrictProduct(Product):
    model_config = ConfigDict(
        strict=True,  # Adds to parent config
    )

# Mixin pattern
class TimestampMixin(BaseModel):
    created_at: str = Field(default_factory=lambda: '2024-01-01')
    updated_at: Optional[str] = None

class AuditMixin(BaseModel):
    created_by: str
    modified_by: Optional[str] = None

class AuditedProduct(TimestampMixin, AuditMixin, BaseModel):
    name: str
    price: float

# Composition over inheritance
class Address(BaseModel):
    street: str
    city: str
    country: str = 'USA'

class ContactInfo(BaseModel):
    email: str
    phone: Optional[str] = None

class Customer(BaseModel):
    name: str
    billing_address: Address
    shipping_address: Optional[Address] = None
    contact: ContactInfo

# Factory method pattern
class Animal(BaseModel):
    name: str
    species: str

    @classmethod
    def create_dog(cls, name: str) -> 'Animal':
        return cls(name=name, species='dog')

    @classmethod
    def create_cat(cls, name: str) -> 'Animal':
        return cls(name=name, species='cat')

# Abstract base with required fields
from abc import ABC

class AbstractItem(BaseModel, ABC):
    id: int

    def get_display_name(self) -> str:
        raise NotImplementedError

class ConcreteProduct(AbstractItem):
    name: str

    def get_display_name(self) -> str:
        return f"Product: {self.name}"
```

### 4.5 Private Attributes and Frozen Models

Control mutability and hide internal state:

```python
from pydantic import BaseModel, ConfigDict, Field, PrivateAttr
from typing import Optional

# Private attributes (not validated, not serialized)
class Model(BaseModel):
    public_field: str

    # Private attributes with PrivateAttr
    _private_value: int = PrivateAttr(default=0)
    _cache: Optional[dict] = PrivateAttr(default=None)

    def __init__(self, **data):
        super().__init__(**data)
        self._private_value = len(self.public_field)

    def get_cached(self, key: str) -> Optional[str]:
        if self._cache is None:
            self._cache = {}
        return self._cache.get(key)

    def set_cached(self, key: str, value: str) -> None:
        if self._cache is None:
            self._cache = {}
        self._cache[key] = value

m = Model(public_field='hello')
print(m.model_dump())  # {'public_field': 'hello'} - no private attrs

# Frozen (immutable) models
class FrozenModel(BaseModel):
    model_config = ConfigDict(frozen=True)

    name: str
    value: int

frozen = FrozenModel(name='test', value=42)
# frozen.name = 'new'  # Raises ValidationError!

# Frozen enables hashing
print(hash(frozen))  # Works because model is immutable

# Use in sets and as dict keys
frozen_set = {frozen, FrozenModel(name='other', value=1)}
frozen_dict = {frozen: 'data'}

# Field-level immutability
class PartiallyFrozen(BaseModel):
    model_config = ConfigDict(validate_assignment=True)

    id: int = Field(frozen=True)  # Only this field is immutable
    name: str  # This can be changed

pf = PartiallyFrozen(id=1, name='test')
pf.name = 'updated'  # OK
# pf.id = 2  # Raises ValidationError!

# Combining with validation on assignment
class ValidatedAssignment(BaseModel):
    model_config = ConfigDict(
        validate_assignment=True,
        frozen=False
    )

    count: int = Field(ge=0)

va = ValidatedAssignment(count=5)
va.count = 10  # OK, validates
# va.count = -1  # Raises ValidationError

# Private attrs with initialization
class Counter(BaseModel):
    name: str
    _count: int = PrivateAttr(default=0)

    def increment(self) -> int:
        self._count += 1
        return self._count

counter = Counter(name='clicks')
print(counter.increment())  # 1
print(counter.increment())  # 2
```

---

## 5. LLM Integration Patterns

### 5.1 Structured Output Generation

Use Pydantic to define and validate structured LLM outputs:

```python
from typing import List, Optional, Literal
from pydantic import BaseModel, Field
import json

# Define structured output schema
class ExtractedEntity(BaseModel):
    """An entity extracted from text."""
    name: str = Field(description="The entity name")
    type: Literal['person', 'organization', 'location', 'date']
    confidence: float = Field(ge=0, le=1, description="Confidence score")

class SentimentAnalysis(BaseModel):
    """Sentiment analysis result."""
    sentiment: Literal['positive', 'negative', 'neutral']
    score: float = Field(ge=-1, le=1)
    aspects: List[str] = Field(description="Key aspects mentioned")

class DocumentSummary(BaseModel):
    """Structured document summary."""
    title: str
    summary: str = Field(max_length=500)
    key_points: List[str] = Field(min_length=1, max_length=10)
    entities: List[ExtractedEntity]
    sentiment: SentimentAnalysis
    language: str = Field(default='en')

# Generate JSON Schema for LLM prompt
schema = DocumentSummary.model_json_schema()
print(json.dumps(schema, indent=2))

# Example prompt construction
def create_extraction_prompt(text: str, model_class: type[BaseModel]) -> str:
    schema = model_class.model_json_schema()
    return f"""
Extract information from the following text and return it as JSON
matching this schema:

{json.dumps(schema, indent=2)}

Text:
{text}

Return only valid JSON.
"""

# Validate LLM response
def parse_llm_response(response: str, model_class: type[BaseModel]):
    """Parse and validate LLM JSON response."""
    try:
        return model_class.model_validate_json(response)
    except Exception as e:
        # Handle validation errors
        raise ValueError(f"Invalid LLM response: {e}")

# Example with nested structures
class CodeReview(BaseModel):
    class Issue(BaseModel):
        line: int
        severity: Literal['error', 'warning', 'info']
        message: str
        suggestion: Optional[str] = None

    file_path: str
    issues: List[Issue]
    overall_quality: int = Field(ge=1, le=10)
    summary: str
```

### 5.2 Function Calling Schemas

Generate OpenAI-compatible function schemas:

```python
from typing import List, Optional, Literal
from pydantic import BaseModel, Field
import json

# Define function parameters as Pydantic models
class SearchQuery(BaseModel):
    """Search for information in the knowledge base."""
    query: str = Field(description="The search query")
    max_results: int = Field(default=5, ge=1, le=20)
    filters: Optional[dict] = Field(default=None, description="Optional filters")

class SendEmail(BaseModel):
    """Send an email to a recipient."""
    to: str = Field(description="Recipient email address")
    subject: str = Field(max_length=200)
    body: str
    cc: Optional[List[str]] = None
    priority: Literal['low', 'normal', 'high'] = 'normal'

class CreateCalendarEvent(BaseModel):
    """Create a calendar event."""
    title: str
    start_time: str = Field(description="ISO 8601 datetime")
    end_time: str = Field(description="ISO 8601 datetime")
    attendees: List[str] = Field(default_factory=list)
    location: Optional[str] = None
    description: Optional[str] = None

# Convert to OpenAI function format
def pydantic_to_openai_function(model: type[BaseModel]) -> dict:
    """Convert Pydantic model to OpenAI function schema."""
    schema = model.model_json_schema()

    return {
        "type": "function",
        "function": {
            "name": model.__name__,
            "description": model.__doc__ or "",
            "parameters": {
                "type": "object",
                "properties": schema.get("properties", {}),
                "required": schema.get("required", [])
            }
        }
    }

# Generate tools for OpenAI
tools = [
    pydantic_to_openai_function(SearchQuery),
    pydantic_to_openai_function(SendEmail),
    pydantic_to_openai_function(CreateCalendarEvent),
]

print(json.dumps(tools, indent=2))

# Parse function call result
def execute_function_call(name: str, arguments: str) -> any:
    """Execute a function call from LLM."""
    function_map = {
        'SearchQuery': SearchQuery,
        'SendEmail': SendEmail,
        'CreateCalendarEvent': CreateCalendarEvent,
    }

    model_class = function_map.get(name)
    if not model_class:
        raise ValueError(f"Unknown function: {name}")

    # Validate arguments
    params = model_class.model_validate_json(arguments)

    # Execute the function (implementation depends on your system)
    return execute_action(name, params)

def execute_action(name: str, params: BaseModel):
    """Placeholder for actual function execution."""
    return {"status": "success", "params": params.model_dump()}
```

### 5.3 API Response Validation

Validate responses from LLM APIs and external services:

```python
from typing import List, Optional, Union
from pydantic import BaseModel, Field, field_validator
from datetime import datetime

# OpenAI API response models
class Usage(BaseModel):
    prompt_tokens: int
    completion_tokens: int
    total_tokens: int

class Message(BaseModel):
    role: str
    content: Optional[str] = None
    tool_calls: Optional[List[dict]] = None

class Choice(BaseModel):
    index: int
    message: Message
    finish_reason: str

class ChatCompletion(BaseModel):
    id: str
    object: str
    created: int
    model: str
    choices: List[Choice]
    usage: Usage

    @property
    def content(self) -> Optional[str]:
        """Get the content from the first choice."""
        if self.choices:
            return self.choices[0].message.content
        return None

# Validate API response
def process_openai_response(raw_response: dict) -> ChatCompletion:
    """Validate and parse OpenAI API response."""
    return ChatCompletion.model_validate(raw_response)

# Anthropic API response models
class ContentBlock(BaseModel):
    type: str
    text: Optional[str] = None

class AnthropicResponse(BaseModel):
    id: str
    type: str
    role: str
    content: List[ContentBlock]
    model: str
    stop_reason: Optional[str] = None
    usage: dict

# Generic LLM response wrapper
class LLMResponse(BaseModel):
    """Unified response from any LLM provider."""
    provider: str
    model: str
    content: str
    tokens_used: int
    finish_reason: str
    raw_response: dict = Field(exclude=True)  # Store but don't serialize

    @classmethod
    def from_openai(cls, response: ChatCompletion) -> 'LLMResponse':
        return cls(
            provider='openai',
            model=response.model,
            content=response.content or '',
            tokens_used=response.usage.total_tokens,
            finish_reason=response.choices[0].finish_reason,
            raw_response=response.model_dump()
        )

# Error response handling
class APIError(BaseModel):
    error: dict
    status_code: int

class APIResponse(BaseModel):
    """Wrapper for API responses that may be success or error."""
    success: bool
    data: Optional[ChatCompletion] = None
    error: Optional[APIError] = None

    @field_validator('data', 'error')
    @classmethod
    def validate_response(cls, v, info):
        # Ensure either data or error is present
        return v
```

### 5.4 Prompt Templating with Models

Use Pydantic models to structure and validate prompts:

```python
from typing import List, Optional, Literal
from pydantic import BaseModel, Field, computed_field
from string import Template

# Prompt configuration model
class PromptConfig(BaseModel):
    """Configuration for prompt generation."""
    model: str = 'gpt-4'
    temperature: float = Field(default=0.7, ge=0, le=2)
    max_tokens: int = Field(default=1000, ge=1)
    system_prompt: Optional[str] = None

# Template-based prompt
class PromptTemplate(BaseModel):
    """A prompt template with variables."""
    template: str
    variables: dict[str, str] = Field(default_factory=dict)

    @computed_field
    @property
    def rendered(self) -> str:
        """Render the template with variables."""
        return Template(self.template).safe_substitute(self.variables)

# Structured prompt for specific tasks
class ExtractionPrompt(BaseModel):
    """Prompt for entity extraction tasks."""
    task: Literal['ner', 'sentiment', 'summary', 'qa']
    text: str
    instructions: str = ""
    examples: List[dict] = Field(default_factory=list)
    output_format: str = "json"

    @computed_field
    @property
    def full_prompt(self) -> str:
        prompt_parts = [
            f"Task: {self.task}",
            f"Instructions: {self.instructions}" if self.instructions else "",
            "",
            "Examples:" if self.examples else "",
        ]

        for ex in self.examples:
            prompt_parts.append(f"Input: {ex.get('input', '')}")
            prompt_parts.append(f"Output: {ex.get('output', '')}")
            prompt_parts.append("")

        prompt_parts.extend([
            "Now process the following:",
            f"Input: {self.text}",
            f"Output ({self.output_format}):"
        ])

        return "\n".join(filter(None, prompt_parts))

# Chat message models
class ChatMessage(BaseModel):
    role: Literal['system', 'user', 'assistant']
    content: str

class ChatConversation(BaseModel):
    """A conversation with message history."""
    messages: List[ChatMessage] = Field(default_factory=list)
    config: PromptConfig = Field(default_factory=PromptConfig)

    def add_system(self, content: str) -> 'ChatConversation':
        self.messages.append(ChatMessage(role='system', content=content))
        return self

    def add_user(self, content: str) -> 'ChatConversation':
        self.messages.append(ChatMessage(role='user', content=content))
        return self

    def add_assistant(self, content: str) -> 'ChatConversation':
        self.messages.append(ChatMessage(role='assistant', content=content))
        return self

    def to_openai_format(self) -> List[dict]:
        return [msg.model_dump() for msg in self.messages]

# Usage example
conversation = (
    ChatConversation()
    .add_system("You are a helpful assistant that extracts structured data.")
    .add_user("Extract the person's name and age from: John is 25 years old.")
)

print(conversation.to_openai_format())

# Advanced: Prompt with schema injection
class SchemaPrompt(BaseModel):
    """Prompt that includes output schema."""
    task_description: str
    input_data: str
    output_model: type[BaseModel]

    @computed_field
    @property
    def full_prompt(self) -> str:
        schema = self.output_model.model_json_schema()
        return f"""
{self.task_description}

Input:
{self.input_data}

Return your response as JSON matching this schema:
{schema}
"""
```

### 5.5 Using Instructor Library

Instructor provides automatic validation and retries for LLM structured outputs:

```python
# pip install instructor
import instructor
from pydantic import BaseModel, Field
from typing import List

# Patch OpenAI client
from openai import OpenAI
client = instructor.from_openai(OpenAI())

# Simple extraction
class User(BaseModel):
    name: str
    age: int

user = client.chat.completions.create(
    model="gpt-4o-mini",
    response_model=User,
    messages=[
        {"role": "user", "content": "John Doe is 25 years old"}
    ],
)
print(user)  # User(name='John Doe', age=25)

# Complex nested structures
class Address(BaseModel):
    street: str
    city: str
    country: str

class Person(BaseModel):
    name: str
    age: int
    addresses: List[Address]

person = client.chat.completions.create(
    model="gpt-4o-mini",
    response_model=Person,
    messages=[
        {"role": "user", "content": """
            Extract: John Doe, 30 years old, lives at
            123 Main St, NYC, USA and 456 Oak Ave, LA, USA
        """}
    ],
)

# With validation and automatic retries
from pydantic import field_validator

class ValidatedUser(BaseModel):
    name: str
    age: int = Field(ge=0, le=120)
    email: str

    @field_validator('email')
    @classmethod
    def validate_email(cls, v):
        if '@' not in v:
            raise ValueError('Invalid email format')
        return v.lower()

# Instructor automatically retries if validation fails
user = client.chat.completions.create(
    model="gpt-4o-mini",
    response_model=ValidatedUser,
    max_retries=3,  # Retry up to 3 times on validation failure
    messages=[
        {"role": "user", "content": "John Doe, 25, john@example.com"}
    ],
)

# Streaming with partial validation
from instructor import Partial

class Report(BaseModel):
    title: str
    sections: List[str]
    summary: str

# Stream partial results as they're generated
for partial_report in client.chat.completions.create_partial(
    model="gpt-4o-mini",
    response_model=Report,
    messages=[
        {"role": "user", "content": "Write a report about AI"}
    ],
):
    print(partial_report)  # Partially complete Report object

# Multiple providers
import instructor
from anthropic import Anthropic

# Works with Anthropic
anthropic_client = instructor.from_anthropic(Anthropic())

user = anthropic_client.messages.create(
    model="claude-3-5-sonnet-20241022",
    max_tokens=1024,
    response_model=User,
    messages=[
        {"role": "user", "content": "Extract: Jane Smith is 30"}
    ],
)
```

### 5.6 Using PydanticAI Framework

PydanticAI is the official agent framework from Pydantic:

```python
# pip install pydantic-ai
from pydantic_ai import Agent
from pydantic import BaseModel, Field
from typing import List

# Simple agent with structured output
class CityInfo(BaseModel):
    name: str
    country: str
    population: int
    famous_for: List[str]

agent = Agent(
    'openai:gpt-4o-mini',
    output_type=CityInfo,
    system_prompt='You are a helpful geography assistant.'
)

result = agent.run_sync('Tell me about Paris')
print(result.output)  # CityInfo object

# Agent with dependencies
from dataclasses import dataclass
from pydantic_ai import RunContext

@dataclass
class Dependencies:
    user_id: str
    api_key: str

class UserProfile(BaseModel):
    name: str
    preferences: List[str]

agent = Agent(
    'openai:gpt-4o-mini',
    deps_type=Dependencies,
    output_type=UserProfile,
)

@agent.system_prompt
def get_system_prompt(ctx: RunContext[Dependencies]) -> str:
    return f'Get profile for user {ctx.deps.user_id}'

result = agent.run_sync(
    'Get my profile',
    deps=Dependencies(user_id='123', api_key='xxx')
)

# Agent with tools
class SearchResult(BaseModel):
    query: str
    results: List[str]

agent = Agent(
    'openai:gpt-4o-mini',
    output_type=SearchResult,
)

@agent.tool
def search_database(ctx: RunContext, query: str) -> List[str]:
    """Search the database for information."""
    # Implementation
    return ['Result 1', 'Result 2']

@agent.tool_plain
def get_current_time() -> str:
    """Get the current time."""
    from datetime import datetime
    return datetime.now().isoformat()

result = agent.run_sync('Search for Python tutorials')

# Multiple output types
from typing import Union

class WeatherInfo(BaseModel):
    temperature: float
    conditions: str

class ErrorResponse(BaseModel):
    error: str
    code: int

agent = Agent(
    'openai:gpt-4o-mini',
    output_type=Union[WeatherInfo, ErrorResponse],
)

# Conversation with message history
from pydantic_ai.messages import ModelMessage

agent = Agent('openai:gpt-4o-mini')

result1 = agent.run_sync('My name is John')
result2 = agent.run_sync(
    'What is my name?',
    message_history=result1.all_messages()
)
print(result2.output)  # Will remember the name
```

---

## Quick Reference

### Common Imports

```python
from pydantic import (
    BaseModel,
    Field,
    ConfigDict,
    field_validator,
    model_validator,
    computed_field,
    PrivateAttr,
    TypeAdapter,
    ValidationError,
    # Serializers
    field_serializer,
    PlainSerializer,
    WrapSerializer,
    # Aliases
    AliasPath,
    AliasChoices,
    AliasGenerator,
    # Types
    EmailStr,
    HttpUrl,
    SecretStr,
    StrictInt,
    StrictStr,
)

from pydantic_settings import BaseSettings, SettingsConfigDict

from typing import (
    Annotated,
    List,
    Dict,
    Optional,
    Union,
    Literal,
    TypeVar,
    Generic,
)
```

### Model Lifecycle Methods

| Method | Purpose |
|--------|---------|
| `model_validate(data)` | Validate dict/object |
| `model_validate_json(json_str)` | Validate JSON string |
| `model_dump()` | Convert to dict |
| `model_dump_json()` | Convert to JSON string |
| `model_json_schema()` | Generate JSON Schema |
| `model_copy()` | Create a copy |
| `model_rebuild()` | Rebuild model schema |

### Validator Modes

| Mode | When it Runs | Use Case |
|------|--------------|----------|
| `before` | Before type coercion | Transform raw input |
| `after` | After validation (default) | Validate/transform result |
| `wrap` | Wraps validation | Full control over process |
| `plain` | Replaces validation | Skip default validation |

### Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `strict` | `False` | Disable type coercion |
| `frozen` | `False` | Make model immutable |
| `extra` | `'ignore'` | Handle extra fields |
| `validate_assignment` | `False` | Validate on assignment |
| `populate_by_name` | `False` | Allow field name + alias |
| `str_strip_whitespace` | `False` | Strip string whitespace |

---

## Resources

- **Official Documentation**: https://docs.pydantic.dev/
- **PydanticAI**: https://ai.pydantic.dev/
- **Pydantic Settings**: https://docs.pydantic.dev/latest/concepts/pydantic_settings/
- **Instructor Library**: https://python.useinstructor.com/
- **GitHub**: https://github.com/pydantic/pydantic
- **PyPI**: https://pypi.org/project/pydantic/
