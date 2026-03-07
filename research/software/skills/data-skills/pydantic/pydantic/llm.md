---
description: Integrate Pydantic with LLMs for structured outputs, function calling, and agent patterns.
---

# Pydantic LLM Integration Expert

Help integrate Pydantic with LLMs for structured output generation and validation.

## Primary Patterns

### 1. Structured Output Schema
```python
from pydantic import BaseModel, Field
from typing import Literal, List

class ExtractedEntity(BaseModel):
    """An entity extracted from text."""
    name: str = Field(description="Entity name")
    type: Literal['person', 'organization', 'location']
    confidence: float = Field(ge=0, le=1, description="Confidence score")

class AnalysisResult(BaseModel):
    """Complete analysis of the input."""
    summary: str = Field(max_length=500, description="Brief summary")
    entities: List[ExtractedEntity] = Field(description="Extracted entities")
    sentiment: Literal['positive', 'negative', 'neutral']

# Generate schema for LLM prompt
import json
schema = AnalysisResult.model_json_schema()
print(json.dumps(schema, indent=2))
```

### 2. Function Calling
```python
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

# Usage
class SearchQuery(BaseModel):
    """Search the knowledge base."""
    query: str = Field(description="Search query")
    max_results: int = Field(default=5, ge=1, le=20)

tool = pydantic_to_openai_function(SearchQuery)
```

### 3. With Instructor (Recommended)
```python
import instructor
from openai import OpenAI
from pydantic import BaseModel

client = instructor.from_openai(OpenAI())

class User(BaseModel):
    name: str
    age: int

# Automatic validation and retries
user = client.chat.completions.create(
    model="gpt-4o-mini",
    response_model=User,
    max_retries=3,  # Retry on validation failure
    messages=[{"role": "user", "content": "John Doe is 25 years old"}]
)
print(user)  # User(name='John Doe', age=25)
```

### 4. With PydanticAI
```python
from pydantic_ai import Agent
from pydantic import BaseModel

class CityInfo(BaseModel):
    name: str
    country: str
    population: int

agent = Agent(
    'openai:gpt-4o-mini',
    output_type=CityInfo,
    system_prompt='You are a geography expert.'
)

result = agent.run_sync('Tell me about Tokyo')
print(result.output)  # CityInfo object
```

### 5. Response Validation
```python
def parse_llm_response(response: str, model: type[BaseModel]):
    """Parse and validate JSON response from LLM."""
    try:
        return model.model_validate_json(response)
    except ValidationError as e:
        # Log error, retry, or return default
        raise ValueError(f"Invalid response: {e}")
```

## Best Practices

1. **Add descriptions** to all fields - helps LLM understand expectations
2. **Use Literal types** for constrained choices
3. **Set reasonable constraints** - min/max length, ranges
4. **Include docstrings** - becomes function description
5. **Use Instructor** for automatic retry on validation failure
6. **Consider PydanticAI** for agent-based workflows

## Ask About

- What data needs to be extracted?
- Which LLM provider (OpenAI, Anthropic)?
- Need automatic retries?
- Building agents with tools?
