---
description: Create Pydantic models with best practices - fields, constraints, config, and type hints.
---

# Pydantic Model Creator

Help create Pydantic v2 models with proper field types, constraints, and configuration.

## Task

When the user describes their data structure:

1. **Analyze requirements** - identify fields, types, constraints, relationships
2. **Design the model** - choose appropriate types and Field options
3. **Configure behavior** - set ConfigDict options as needed
4. **Add validation** - include validators for complex rules
5. **Show usage** - demonstrate instantiation and serialization

## Model Template

```python
from pydantic import BaseModel, ConfigDict, Field
from typing import Optional, List
from datetime import datetime

class ModelName(BaseModel):
    model_config = ConfigDict(
        # Only include if needed:
        # strict=True,
        # frozen=True,
        # extra='forbid',
        # validate_assignment=True,
    )

    # Required fields
    id: int
    name: str = Field(min_length=1, max_length=100)

    # Optional fields
    description: Optional[str] = None

    # Fields with defaults
    tags: List[str] = Field(default_factory=list)
    created_at: datetime = Field(default_factory=datetime.utcnow)

    # Constrained fields
    price: float = Field(gt=0, le=10000)
    quantity: int = Field(ge=0, default=0)
```

## Field Type Reference

| Data | Type | Constraints |
|------|------|-------------|
| Text | `str` | `min_length`, `max_length`, `pattern` |
| Number | `int`, `float` | `gt`, `ge`, `lt`, `le`, `multiple_of` |
| Boolean | `bool` | - |
| Date/Time | `datetime`, `date` | - |
| Email | `EmailStr` | Auto-validated |
| URL | `HttpUrl` | Auto-validated |
| UUID | `UUID` | Auto-validated |
| Enum | `Literal['a', 'b']` | Fixed choices |
| List | `List[T]` | `min_length`, `max_length` |
| Dict | `Dict[K, V]` | - |

## Ask Clarifying Questions

- What fields are required vs optional?
- Are there constraints (min/max, patterns)?
- Should the model be immutable?
- Are there relationships to other models?
- What's the serialization format (JSON keys)?
