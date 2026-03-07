---
description: Add validation logic to Pydantic models - field validators, model validators, and custom types.
---

# Pydantic Validation Expert

Help add validation logic to Pydantic models using validators and custom types.

## Validation Patterns

### Field Validator (Single Field)
```python
from pydantic import BaseModel, field_validator

class User(BaseModel):
    email: str

    @field_validator('email')
    @classmethod
    def validate_email(cls, v: str) -> str:
        if '@' not in v:
            raise ValueError('Invalid email format')
        return v.lower().strip()
```

### Before Validator (Pre-processing)
```python
@field_validator('data', mode='before')
@classmethod
def parse_json(cls, v):
    if isinstance(v, str):
        import json
        return json.loads(v)
    return v
```

### Model Validator (Cross-field)
```python
from pydantic import model_validator
from typing import Self

class DateRange(BaseModel):
    start: datetime
    end: datetime

    @model_validator(mode='after')
    def validate_range(self) -> Self:
        if self.start >= self.end:
            raise ValueError('start must be before end')
        return self
```

### Custom Annotated Types (Reusable)
```python
from typing import Annotated
from pydantic import AfterValidator, Field

def validate_positive(v: int) -> int:
    if v <= 0:
        raise ValueError('Must be positive')
    return v

PositiveInt = Annotated[int, AfterValidator(validate_positive)]
Username = Annotated[str, Field(min_length=3, max_length=50, pattern=r'^[a-z0-9_]+$')]

class User(BaseModel):
    id: PositiveInt
    username: Username
```

## Validator Modes

| Mode | Receives | Use Case |
|------|----------|----------|
| `after` (default) | Validated value | Transform or validate result |
| `before` | Raw input | Parse strings, normalize data |
| `wrap` | Value + handler | Full control, can skip validation |
| `plain` | Raw input | Replace default validation entirely |

## Common Validation Needs

Ask what needs validation:
- Format constraints (email, URL, phone)?
- Business rules (password match, date ranges)?
- Data normalization (strip, lowercase)?
- Cross-field dependencies?
- Conditional validation?
