# LiteLLM API Patterns and Usage Conventions - Comprehensive Research

## 1. MODEL NAMING CONVENTIONS

### General Pattern
LiteLLM uses provider-prefixed model identifiers: `model=<provider_name>/<model_name>`

### Provider-Specific Naming Examples

#### OpenAI
```python
# Chat completions
model = "openai/gpt-4o"
model = "openai/gpt-4-turbo"
model = "openai/gpt-3.5-turbo"

# Text completions (deprecated but supported)
# ⚠️ DEPRECATED: text-davinci-003 is discontinued by OpenAI as of Jan 2024
# Use gpt-3.5-turbo-instruct or gpt-4o for new projects
model = "text-completion-openai/text-davinci-003"
```

#### Azure OpenAI
```python
# Standard models
model = "azure/<deployment_name>"

# O-series models (reasoning)
model = "azure/o_series/<deployment_name>"
# OR auto-detected: model = "azure/o1-deployment"

# GPT-5 series
model = "azure/gpt5_series/<deployment_name>"
# OR auto-detected: model = "azure/gpt-5-deployment"

# Text completion
model = "azure_text/<deployment_name>"
```

#### AWS Bedrock
```python
# Default route
model = "bedrock/anthropic.claude-3-5-sonnet-20240620-v1:0"

# Converse API
model = "bedrock/converse/anthropic.claude-opus-4-1-20250805-v1:0"

# Invoke API
model = "bedrock/invoke/anthropic.claude-3-sonnet-20240229-v1:0"

# Specialized routes
model = "bedrock/deepseek_r1/arn:aws:bedrock:region:account:imported-model/id"
model = "bedrock/llama/meta.llama2-70b-chat-v1"
model = "bedrock/qwen3/qwen/qwen3-vl"
```

Bedrock naming format: `bedrock/{provider}.{model-id}-{version}:{revision}`
- Provider: `anthropic`, `meta`, `cohere`, `mistral`, `amazon`, `ai21`

#### Anthropic (Direct)
```python
model = "anthropic/claude-opus-4-1-20250805"
model = "anthropic/claude-3-5-sonnet-20240620"
model = "anthropic/claude-3-haiku-20240307"
# ⚠️ DEPRECATED: claude-2.1 and claude-instant-1.2 are legacy models
# Use claude-3-5-sonnet or claude-3-haiku for new projects
model = "anthropic/claude-2.1"  # Legacy - avoid for new projects
model = "anthropic/claude-instant-1.2"  # Legacy - avoid for new projects
```

#### Other Popular Providers
```python
model = "openrouter/google/palm-2-chat-bison"
model = "huggingface/WizardLM/WizardCoder-Python-34B-V1.0"
model = "ollama/llama2"
model = "cohere/command-r-plus"
model = "vertexai/gemini-1.5-pro"
model = "nvidia_nim/mistral-7b-instruct-v3"
model = "together_ai/meta-llama/Llama-3-70b-chat-hf"
```

### Proxy Configuration (config.yaml)
```yaml
model_list:
  - model_name: "gpt-3.5"  # User-facing name
    litellm_params:
      model: "openai/gpt-3.5-turbo"  # Actual model sent to LiteLLM
      api_key: "${OPENAI_API_KEY}"  # Environment variable reference
      api_base: "https://api.openai.com/v1"

  - model_name: "gpt-4-azure"
    litellm_params:
      model: "azure/gpt-4-deployment"
      api_key: "${AZURE_API_KEY}"
      api_base: "${AZURE_API_BASE}"
      api_version: "2024-08-01-preview"
```

### Provider-Specific Parameters
Access provider-specific parameters via `litellm.<provider_name>Config`:

```python
from litellm import AzureOpenAIConfig, OpenAIConfig

# Set provider-specific defaults
openai_config = OpenAIConfig(
    organization="org-123",
    timeout=30
)

# Azure-specific
azure_config = AzureOpenAIConfig(
    api_version="2024-08-01-preview"
)
```

---

## 2. MESSAGE FORMAT AND ROLES

### Standard Message Structure
Each message must include `role` and `content`, with optional metadata:

```python
{
    "role": "system|user|assistant|function|tool",
    "content": "string | list[dict] | None",  # None allowed for assistant with function calls
    "name": "string",  # Required if role="function", optional otherwise
    "function_call": {...},  # Optional: function call object
    "tool_call_id": "string"  # Optional: links to previous tool_call
}
```

### Role Types

| Role | Purpose | Content |
|------|---------|---------|
| `system` | Sets context, instructions, and behavior | Usually a string instruction |
| `user` | User input/requests | String or vision content array |
| `assistant` | Model responses | Text, function calls, or tool calls |
| `function` | Function execution result | String result or error message |
| `tool` | Tool/function call result | String result or error message |

### Basic Completion Example
```python
from litellm import completion

messages = [
    {
        "role": "system",
        "content": "You are a helpful assistant specialized in Python programming."
    },
    {
        "role": "user",
        "content": "How do I read a file in Python?"
    }
]

response = completion(
    model="openai/gpt-4o",
    messages=messages,
    temperature=0.7,
    max_tokens=500
)

print(response['choices'][0]['message']['content'])
```

### Multi-turn Conversation Pattern
```python
import litellm

messages = [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "What's the capital of France?"},
]

# First turn
response1 = litellm.completion(model="openai/gpt-4o", messages=messages)
assistant_response = response1['choices'][0]['message']['content']

# Add to conversation
messages.append({"role": "assistant", "content": assistant_response})
messages.append({"role": "user", "content": "When was it founded?"})

# Second turn with context
response2 = litellm.completion(model="openai/gpt-4o", messages=messages)
```

### Message Content Variations

#### Text Only
```python
{"role": "user", "content": "Hello, how are you?"}
```

#### Vision/Image Content
```python
{
    "role": "user",
    "content": [
        {"type": "text", "text": "What's in this image?"},
        {
            "type": "image_url",
            "image_url": {
                "url": "https://example.com/image.jpg",
                # Optional: specify format explicitly
                "format": "image/jpeg"
            }
        }
    ]
}

# OR base64 encoded
{
    "role": "user",
    "content": [
        {"type": "text", "text": "Analyze this image"},
        {
            "type": "image_url",
            "image_url": {
                "url": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
            }
        }
    ]
}
```

### Custom Prompt Templates
For providers like HuggingFace, Ollama, Together AI:

```python
litellm.completion(
    model="ollama/llama2",
    messages=messages,
    pre_message="[INST]",  # Prefix for messages
    post_message="[/INST]"  # Suffix for messages
)
```

### Role Alternation Handling
Some models require alternating message roles (user → assistant → user):

```python
# Problem: consecutive user messages
messages = [
    {"role": "user", "content": "First question"},
    {"role": "user", "content": "Second question"}  # ERROR for some models
]

# Solution: Insert empty assistant messages for compatibility
messages = [
    {"role": "user", "content": "First question"},
    {"role": "assistant", "content": ""},  # Placeholder
    {"role": "user", "content": "Second question"}
]
```

### Prefix Assistant Messages
Pre-fill assistant responses for few-shot examples:

```python
messages = [
    {"role": "system", "content": "You are a JSON formatter."},
    {"role": "user", "content": "Format: {'name': 'John'}"},
    {
        "role": "assistant",
        "content": '{"name": "John"}'  # Pre-filled example
    },
    {"role": "user", "content": "Format: {'name': 'Jane', 'age': 30}"}
]
```

---

## 3. FUNCTION CALLING AND TOOL USE PATTERNS

### Function Definition Schema
```python
tools = [
    {
        "type": "function",  # Always "function"
        "function": {
            "name": "get_current_weather",
            "description": "Get the current weather in a given location",
            "parameters": {
                "type": "object",
                "properties": {
                    "location": {
                        "type": "string",
                        "description": "City and state, e.g., 'San Francisco, CA'"
                    },
                    "unit": {
                        "type": "string",
                        "enum": ["celsius", "fahrenheit"],
                        "description": "Temperature unit"
                    }
                },
                "required": ["location"]
            }
        }
    }
]

# Alternative format (backward compatible)
functions = [
    {
        "name": "get_weather",
        "description": "...",
        "parameters": {...}
    }
]
```

### Basic Function Calling Flow
```python
from litellm import completion
import json

def get_weather(location: str, unit: str = "celsius") -> str:
    """Mock weather function"""
    return f"Weather in {location}: 72°{unit[0].upper()}"

# Step 1: Call model with tools
messages = [
    {
        "role": "system",
        "content": "You are a helpful assistant with access to weather information."
    },
    {
        "role": "user",
        "content": "What's the weather in San Francisco?"
    }
]

response = completion(
    model="openai/gpt-4o",
    messages=messages,
    tools=tools,
    tool_choice="auto"  # "auto", "required", or specific tool name
)

# Step 2: Parse tool call
if response['choices'][0]['message'].get('tool_calls'):
    tool_call = response['choices'][0]['message']['tool_calls'][0]
    function_name = tool_call['function']['name']
    function_args = json.loads(tool_call['function']['arguments'])
    
    # Step 3: Execute function
    if function_name == "get_weather":
        result = get_weather(**function_args)
    
    # Step 4: Add results to conversation
    messages.append({"role": "assistant", "content": response['choices'][0]['message']['content'], "tool_calls": tool_call})
    messages.append({
        "role": "tool",
        "tool_call_id": tool_call['id'],
        "name": function_name,
        "content": result
    })
    
    # Step 5: Get final response
    final_response = completion(model="openai/gpt-4o", messages=messages)
    print(final_response['choices'][0]['message']['content'])
```

### Parallel Function Calling
```python
# Model can call multiple functions at once
response = completion(
    model="openai/gpt-4-turbo",  # Must support parallel calls
    messages=[
        {
            "role": "user",
            "content": "What's the weather in San Francisco, Tokyo, and Paris?"
        }
    ],
    tools=tools
)

# response['choices'][0]['message']['tool_calls'] contains multiple calls
tool_calls = response['choices'][0]['message']['tool_calls']

# Process all calls
results = []
for tool_call in tool_calls:
    function_name = tool_call['function']['name']
    args = json.loads(tool_call['function']['arguments'])
    result = execute_function(function_name, args)
    results.append({
        "tool_call_id": tool_call['id'],
        "result": result
    })
```

### Function Calling with Specific Tool Selection
```python
# Force specific tool
response = completion(
    model="openai/gpt-4o",
    messages=messages,
    tools=tools,
    tool_choice={"type": "function", "function": {"name": "get_weather"}}
)

# Disable tool use
response = completion(
    model="openai/gpt-4o",
    messages=messages,
    tools=tools,
    tool_choice="none"  # Never call tools, respond normally
)
```

### Model Support Detection
```python
import litellm

# Check if model supports function calling
supports_fc = litellm.supports_function_calling(model="openai/gpt-4o")
# Returns: True

# Check parallel function calling
supports_parallel = litellm.supports_parallel_function_calling(model="openai/gpt-4o")
# Returns: True

# For models without native support, use prompt injection
response = litellm.completion(
    model="ollama/llama2",
    messages=messages,
    tools=tools,
    add_function_to_prompt=True  # Embed function schema in prompt
)
```

### Helper: Convert Python Function to Schema
```python
from litellm import function_to_dict

def get_weather(location: str, unit: str = "celsius") -> str:
    """
    Get the current weather in a given location.
    
    Args:
        location: The city and state, e.g. San Francisco, CA
        unit: Temperature unit (celsius or fahrenheit)
    
    Returns:
        Weather description
    """
    pass

# Auto-generate schema from docstring
function_schema = function_to_dict(get_weather)
tools = [{"type": "function", "function": function_schema}]
```

### Fallback for Function Calling
```python
# Use prompt-based function calling for unsupported models
response = completion(
    model="anthropic/claude-3-haiku-20240307",
    messages=messages,
    tools=tools,
    add_function_to_prompt=True  # For models without native support
)
```

---

## 4. ASYNC VS SYNC PATTERNS

### Sync Completion (Blocking)
```python
from litellm import completion

# Simple blocking call
response = completion(
    model="openai/gpt-4o",
    messages=[{"role": "user", "content": "Hello"}]
)
print(response['choices'][0]['message']['content'])
```

### Async Completion (Non-blocking)
```python
import asyncio
from litellm import acompletion

async def main():
    response = await acompletion(
        model="openai/gpt-4o",
        messages=[{"role": "user", "content": "Hello"}]
    )
    print(response['choices'][0]['message']['content'])

# Run
asyncio.run(main())
```

### Async with Streaming
```python
async def stream_response():
    response = await acompletion(
        model="openai/gpt-4o",
        messages=[{"role": "user", "content": "Write a poem"}],
        stream=True
    )
    
    async for chunk in response:
        # Process each chunk as it arrives
        if chunk['choices'][0].get('delta', {}).get('content'):
            print(chunk['choices'][0]['delta']['content'], end='')

asyncio.run(stream_response())
```

### Sync Streaming
```python
from litellm import completion

response = completion(
    model="openai/gpt-4o",
    messages=[{"role": "user", "content": "Write a poem"}],
    stream=True
)

# Iterate over chunks
for chunk in response:
    if chunk['choices'][0].get('delta', {}).get('content'):
        print(chunk['choices'][0]['delta']['content'], end='')
```

### Build Complete Response from Stream
```python
import litellm

stream_response = completion(
    model="openai/gpt-4o",
    messages=messages,
    stream=True
)

# Reconstruct full response from chunks
complete_response = litellm.stream_chunk_builder(
    stream_response,
    messages=messages
)

print(complete_response)
```

### Concurrent Requests
```python
import asyncio
from litellm import acompletion

async def call_model(prompt: str) -> str:
    response = await acompletion(
        model="openai/gpt-4o",
        messages=[{"role": "user", "content": prompt}]
    )
    return response['choices'][0]['message']['content']

async def main():
    # Run multiple requests concurrently
    results = await asyncio.gather(
        call_model("What is 2+2?"),
        call_model("What is the capital of France?"),
        call_model("What is the largest planet?")
    )
    
    for result in results:
        print(result)

asyncio.run(main())
```

### Timeout Configuration
```python
from litellm import completion

# Set timeout in seconds
response = completion(
    model="openai/gpt-4o",
    messages=messages,
    request_timeout=30  # 30 second timeout
)
```

### Streaming Chunk Limits (Infinite Loop Protection)
```python
import litellm

# Set maximum repeated chunks before error
litellm.REPEATED_STREAMING_CHUNK_LIMIT = 100

response = completion(
    model="openai/gpt-4o",
    messages=messages,
    stream=True
)

for chunk in response:
    # If same chunk repeats >100 times, raises InternalServerError
    print(chunk)
```

---

## 5. CALLBACK HANDLERS AND HOOKS

### Custom Logger Class Pattern
```python
from litellm import CustomLogger
import litellm

class MyCustomLogger(CustomLogger):
    """Custom logging implementation"""
    
    def log_pre_api_call(self, model, messages, kwargs):
        print(f"Calling {model} with {len(messages)} messages")
    
    def log_post_api_call(self, kwargs, response_obj, start_time, end_time):
        print(f"API call took {end_time - start_time} seconds")
    
    def log_success_event(self, kwargs, response_obj, start_time, end_time):
        """Sync success callback"""
        print(f"Success: {kwargs['model']}")
        print(f"Cost: ${kwargs.get('response_cost', 0)}")
        print(f"Usage: {response_obj.usage}")
    
    def log_failure_event(self, kwargs, response_obj, start_time, end_time):
        """Sync failure callback"""
        print(f"Failed: {kwargs['model']}")
        print(f"Error: {response_obj}")
    
    async def async_log_success_event(self, kwargs, response_obj, start_time, end_time):
        """Async success callback - recommended for proxy"""
        print(f"Async success: {kwargs['model']}")
        # Send to external service
        # await send_to_analytics_service(...)
    
    async def async_log_failure_event(self, kwargs, response_obj, start_time, end_time):
        """Async failure callback"""
        print(f"Async failure: {kwargs['model']}")

# Register custom logger
custom_logger = MyCustomLogger()
litellm.callbacks = [custom_logger]

# Now all completions use the custom logger
response = litellm.completion(
    model="openai/gpt-4o",
    messages=[{"role": "user", "content": "Hello"}]
)
```

### Simple Callback Functions
```python
import litellm

# Cost tracking callback
def cost_tracker(kwargs, completion_response, start_time, end_time):
    """Track costs for all completions"""
    cost = kwargs.get("response_cost", 0)
    model = kwargs.get("model", "unknown")
    print(f"Model {model} cost: ${cost}")

# Register callback
litellm.success_callback = [cost_tracker]

# Error tracking callback
def error_handler(kwargs, response_obj, start_time, end_time):
    """Handle failures"""
    print(f"Error with {kwargs['model']}: {response_obj}")

litellm.failure_callback = [error_handler]
```

### Input/Pre-call Tracking
```python
import litellm

def log_input(kwargs, completion_response, start_time, end_time):
    """Log transformed inputs before API call"""
    messages = kwargs.get("messages", [])
    model = kwargs.get("model")
    print(f"Input to {model}: {len(messages)} messages")
    print(f"First message: {messages[0]['content'][:100]}...")

litellm.input_callback = [log_input]
```

### Response Cost Tracking
```python
import litellm

total_costs = {}

def track_cost(kwargs, completion_response, start_time, end_time):
    """Track costs by model"""
    model = kwargs['model']
    cost = kwargs.get('response_cost', 0)
    
    if model not in total_costs:
        total_costs[model] = 0
    total_costs[model] += cost

litellm.success_callback = [track_cost]

# Later, check costs
print(f"Total costs by model: {total_costs}")
```

### Async Callbacks for Streaming
```python
import litellm
import asyncio

async def async_cost_tracker(kwargs, completion_response, start_time, end_time):
    """Async callback for streaming"""
    cost = kwargs.get("response_cost", 0)
    model = kwargs.get("model")
    
    # Can call async functions
    # await send_to_service(model, cost)
    print(f"Async: {model} cost ${cost}")

litellm.success_callback = [async_cost_tracker]

async def main():
    response = await litellm.acompletion(
        model="openai/gpt-4o",
        messages=[{"role": "user", "content": "Hello"}]
    )

asyncio.run(main())
```

### Proxy-Specific Hooks

#### Pre-call Hook (Modify/Reject Requests)
```python
# In proxy configuration file
from fastapi import HTTPException

async def async_pre_call_hook(user_api_key_dict, cache, data, call_type):
    """
    Modify or reject requests before API call.
    Most powerful intervention point in proxy request lifecycle.
    """
    
    # Modify model
    if data.get("model") == "gpt-3.5-turbo":
        data["model"] = "gpt-4o"  # Upgrade model
    
    # Reject requests
    if "sensitive" in str(data.get("messages", "")):
        raise HTTPException(
            status_code=400,
            detail={"error": "Sensitive content detected"}
        )
    
    # Transform messages
    if data.get("messages"):
        data["messages"] = transform_messages(data["messages"])
    
    return data
```

#### Post-call Success Hook (Add Metadata)
```python
async def async_post_call_success_hook(data: dict, response: dict):
    """
    Add metadata or headers to response.
    Runs after successful LLM call.
    """
    # Add custom header
    if not response.get("_response_ms"):
        response["_response_ms"] = data.get("response_ms", 0)
    
    # Add request tracing
    response["_request_id"] = data.get("request_id")
    
    return response
```

#### Post-call Failure Hook (Error Handling)
```python
async def async_post_call_failure_hook(data: dict, exception: Exception):
    """Handle failed requests"""
    print(f"Request failed for {data['model']}: {exception}")
    # Log to error tracking service
```

#### Moderation Hook (Runs in Parallel)
```python
async def async_moderation_hook(data: dict):
    """
    Run content moderation in parallel with LLM call.
    Raises exception to reject request.
    """
    messages = data.get("messages", [])
    
    # Check for policy violations
    for msg in messages:
        if "banned_word" in str(msg.get("content", "")):
            raise HTTPException(
                status_code=400,
                detail={"error": "Content policy violated"}
            )
    
    # Check user permissions
    if data.get("user") == "restricted_user":
        raise HTTPException(
            status_code=403,
            detail={"error": "User not authorized"}
        )
```

#### Streaming Post-call Hook
```python
async def async_post_call_streaming_hook(data: dict, stream: Iterator):
    """Handle streamed responses"""
    # Can process streaming chunks
    for chunk in stream:
        # Modify or filter chunks
        yield chunk
```

### Best Practices for Callbacks
```python
import litellm
import traceback

class RobustLogger(litellm.CustomLogger):
    """Production-ready callback with error handling"""
    
    async def async_log_success_event(self, kwargs, response_obj, start_time, end_time):
        try:
            # Keep callbacks lean and fast
            cost = kwargs.get("response_cost", 0)
            model = kwargs.get("model")
            
            # Short operations only
            if cost > 10:  # Log expensive calls
                await self.alert_expensive_call(model, cost)
        
        except Exception as e:
            # Failing callback shouldn't break everything
            print(f"Callback error: {e}")
            traceback.print_exc()
    
    async def alert_expensive_call(self, model: str, cost: float):
        """Send alert for expensive API calls"""
        # Async call to external service
        pass

# Register
litellm.callbacks = [RobustLogger()]
```

---

## 6. CACHING MECHANISMS

### In-Memory Caching (Development/Testing)
```python
import litellm

# Enable simple in-memory cache
litellm.cache = litellm.InMemoryCache()

# First call - cached
response1 = litellm.completion(
    model="openai/gpt-4o",
    messages=[{"role": "user", "content": "What is 2+2?"}]
)

# Second call - retrieved from cache (instant)
response2 = litellm.completion(
    model="openai/gpt-4o",
    messages=[{"role": "user", "content": "What is 2+2?"}]
)

# responses are identical, but second is instant
assert response1 == response2
```

### Redis Caching (Production)
```python
import litellm

# Configure Redis cache
litellm.cache = litellm.RedisCache(
    host="localhost",
    port=6379,
    db=0
)

# Usage is identical to in-memory
response = litellm.completion(
    model="openai/gpt-4o",
    messages=[{"role": "user", "content": "What is AI?"}]
)
```

### Cache Configuration in Proxy
```yaml
# config.yaml
cache:
  type: redis  # or memory
  # Redis config
  host: localhost
  port: 6379
  db: 0
  password: ${REDIS_PASSWORD}
  
  # Cache settings
  default_ttl: 3600  # 1 hour
  supported_call_types: ["completion", "embedding"]

litellm_settings:
  # ... other settings
```

### Disable Cache for Specific Calls
```python
import litellm

litellm.cache = litellm.InMemoryCache()

# This call will be cached
response1 = litellm.completion(
    model="openai/gpt-4o",
    messages=[{"role": "user", "content": "What is 2+2?"}]
)

# This call bypasses cache
response2 = litellm.completion(
    model="openai/gpt-4o",
    messages=[{"role": "user", "content": "What is 2+2?"}],
    cache={"no-cache": True}  # Skip cache
)

# Disable cache entirely
response3 = litellm.completion(
    model="openai/gpt-4o",
    messages=[{"role": "user", "content": "What is 2+2?"}],
    cache={"no-store": True}  # Don't store in cache
)
```

### Proxy Cache Control
```yaml
# Disable caching for specific call types
litellm_settings:
  cache:
    supported_call_types: []  # Disable LLM caching, keep internal
```

### Cache Key Generation
Cache automatically includes:
- Model name
- Messages content
- Temperature and other parameters
- API version

Identical requests = identical cache keys = cache hit

---

## 7. FALLBACKS AND RETRIES

### Basic Retry Configuration
```python
from litellm import Router

# Router with retry policy
router = Router(
    model_list=[
        {
            "model_name": "gpt-4",
            "litellm_params": {
                "model": "openai/gpt-4o",
                "api_key": "${OPENAI_API_KEY}"
            }
        }
    ],
    num_retries=3,  # Retry failed requests 3 times
    request_timeout=30  # 30 second timeout before retry
)

response = router.completion(
    model="gpt-4",
    messages=[{"role": "user", "content": "Hello"}]
)
```

### Fallback Configuration
```python
fallbacks = [
    {
        "gpt-4": ["gpt-3.5-turbo", "claude-3-opus"]  # Try in order
    }
]

# Fallbacks are sequential
# 1. Try gpt-4 (3 times)
# 2. If fails, try gpt-3.5-turbo (3 times)
# 3. If fails, try claude-3-opus (3 times)
# 4. If all fail, raise error

response = router.completion(
    model="gpt-4",
    messages=messages,
    fallbacks=fallbacks
)
```

### Fallback Types

#### Standard Fallbacks
```python
# Handle: rate limits, timeouts, 500 errors, connection issues
fallbacks = [{"primary-model": ["backup-model", "fallback-model"]}]
```

#### Content Policy Fallbacks
```python
# Triggered by ContentPolicyViolationError
content_policy_fallbacks = {
    "gpt-4": ["gpt-3.5-turbo"]  # Use cheaper model if content blocked
}
```

#### Context Window Fallbacks
```python
# Triggered by ContextWindowExceededError
context_fallbacks = {
    "gpt-3.5-turbo": ["gpt-4"]  # Use larger model if context exceeded
}
```

### Combined Fallback Configuration (config.yaml)
```yaml
model_list:
  - model_name: "my-model"
    litellm_params:
      model: "openai/gpt-4o"
    
litellm_settings:
  num_retries: 3
  request_timeout: 10
  retry_policy: "exponential_backoff"
  fallbacks: [{"my-model": ["gpt-3.5-turbo"]}]
  content_policy_fallbacks: [{"my-model": ["gpt-3.5-turbo-cheap"]}]
  context_window_fallbacks: [{"my-model": ["gpt-4-32k"]}]
  
  # Model cooldown
  allowed_fails: 3  # Fail >3 times in 1 min = cooldown
  cooldown_time: 30  # Cooldown for 30 seconds
```

### Retry Policy with Exponential Backoff
```python
# Automatic exponential backoff for RateLimitError
# Attempt 1: Retry immediately
# Attempt 2: Wait 1 second + random
# Attempt 3: Wait 2 seconds + random
# etc.

response = router.completion(
    model="gpt-4",
    messages=messages
    # Exponential backoff applied automatically
)
```

### Custom Retry Logic
```python
import litellm
from litellm import RetryPolicy

# Define which errors trigger retries
retry_policy = RetryPolicy(
    num_retries=3,
    retry_on_exceptions=[
        litellm.RateLimitError,
        litellm.ServiceUnavailableError,
        litellm.Timeout
    ]
)

response = litellm.completion(
    model="openai/gpt-4o",
    messages=messages,
    retry_policy=retry_policy
)
```

### Test Fallbacks
```python
# Mock testing without actual failures
response = router.completion(
    model="my-model",
    messages=messages,
    mock_testing_fallbacks=True  # Triggers fallback logic
)
```

### Per-Request Disable
```python
# Skip fallbacks for specific request
response = router.completion(
    model="my-model",
    messages=messages,
    disable_fallbacks=True  # Use only specified model
)
```

### Per-Model Configuration
```yaml
model_list:
  - model_name: "fast-model"
    litellm_params:
      model: "openai/gpt-3.5-turbo"
      temperature: 0.5  # Custom for fallback
    
    # Model-specific settings
    model_info:
      max_tokens: 1000
      rpm: 100  # 100 requests per minute
```

---

## 8. LOAD BALANCING

### Basic Load Balancing
```python
from litellm import Router

# Multiple deployments of same model
model_list = [
    {
        "model_name": "gpt-4",
        "litellm_params": {
            "model": "azure/gpt-4-us-east"
        }
    },
    {
        "model_name": "gpt-4",
        "litellm_params": {
            "model": "azure/gpt-4-us-west"
        }
    },
    {
        "model_name": "gpt-4",
        "litellm_params": {
            "model": "openai/gpt-4o"
        }
    }
]

router = Router(model_list=model_list)

# Router automatically load balances across instances
response = router.completion(
    model="gpt-4",
    messages=messages
    # Randomly or round-robin distributed across 3 deployments
)
```

### Load Balancing Strategies

#### Round-Robin (Default)
```yaml
router_settings:
  routing_strategy: "round_robin"  # Cycle through each deployment
```

#### Least-Busy
```yaml
router_settings:
  routing_strategy: "least_busy"  # Route to least loaded deployment
```

#### Latency-Based
```yaml
router_settings:
  routing_strategy: "latency_based"  # Route to fastest deployment
```

#### Random
```yaml
router_settings:
  routing_strategy: "random"  # Random selection
```

### Regional Load Balancing
```yaml
model_list:
  # US East
  - model_name: "gpt-4"
    litellm_params:
      model: "azure/gpt-4-us-east-1"
      api_base: "https://us-east-1.openai.azure.com"
  
  # US West
  - model_name: "gpt-4"
    litellm_params:
      model: "azure/gpt-4-us-west-1"
      api_base: "https://us-west-1.openai.azure.com"
  
  # Europe
  - model_name: "gpt-4"
    litellm_params:
      model: "azure/gpt-4-eu-west-1"
      api_base: "https://eu-west-1.openai.azure.com"
```

### Model Group Configuration
```yaml
# A model_group contains multiple deployments with same model_name
# They share: same fallbacks, same retries, same rate limits
# They're load balanced together

model_list:
  - model_name: "gpt-4"  # Group 1: 3 deployments
    litellm_params:
      model: "openai/gpt-4o"
  - model_name: "gpt-4"  # Same group
    litellm_params:
      model: "azure/gpt-4-deployment"
  - model_name: "gpt-4"  # Same group
    litellm_params:
      model: "bedrock/claude-3-opus"
  
  - model_name: "gpt-3.5"  # Group 2: different model_name
    litellm_params:
      model: "openai/gpt-3.5-turbo"
```

### Model Group Cooldown
```yaml
litellm_settings:
  allowed_fails: 3  # If model fails > 3 times in 1 minute
  cooldown_time: 30  # Cool down model for 30 seconds
  
  # After cooldown, model re-enters load balancing
```

---

## 9. RATE LIMITING HANDLING

### Per-API Key Rate Limits
```python
# Via /key/generate endpoint
key_response = requests.post(
    "http://localhost:4000/key/generate",
    json={
        "key_alias": "user-123",
        "tpm_limit": 60000,  # Tokens per minute
        "rpm_limit": 100,     # Requests per minute
        "max_budget": 50      # Monthly budget in USD
    },
    headers={"Authorization": f"Bearer {admin_key}"}
)

api_key = key_response.json()["key"]
```

### User Rate Limits
```python
# Set limits for internal user
user_response = requests.post(
    "http://localhost:4000/user/new",
    json={
        "user_id": "user-456",
        "tpm_limit": 120000,
        "rpm_limit": 200,
        "max_budget": 100
    },
    headers={"Authorization": f"Bearer {admin_key}"}
)
```

### Team Rate Limits
```python
# Set shared team budget
team_response = requests.post(
    "http://localhost:4000/team/new",
    json={
        "team_id": "team-789",
        "max_parallel_requests": 10,
        "tpm_limit": 500000,  # Shared across team
        "rpm_limit": 1000,
        "max_budget": 1000
    },
    headers={"Authorization": f"Bearer {admin_key}"}
)
```

### Model-Specific Rate Limits
```python
# Different limits per model on same key
key_config = {
    "key_alias": "user-multi",
    "model_rpm_limit": {
        "gpt-4": 50,           # 50 requests/min for GPT-4
        "gpt-3.5-turbo": 200   # 200 requests/min for GPT-3.5
    },
    "model_tpm_limit": {
        "gpt-4": 30000,        # 30K tokens/min for GPT-4
        "gpt-3.5-turbo": 60000 # 60K tokens/min for GPT-3.5
    }
}
```

### Parallel Request Limits
```python
# Limit concurrent requests
user_config = {
    "user_id": "user-concurrent",
    "max_parallel_requests": 5  # Only 5 concurrent requests
}
```

### Budget Configuration
```python
# Monthly budget with automatic reset
user_config = {
    "user_id": "user-monthly",
    "max_budget": 100,           # $100 USD
    "budget_duration": "30d"     # Reset every 30 days
}

# Daily budget
daily_budget = {
    "max_budget": 10,
    "budget_duration": "1d"      # Reset daily
}

# Hourly budget
hourly_budget = {
    "max_budget": 1,
    "budget_duration": "1h"      # Reset hourly
}
```

### Rate Limit Response Headers
```python
# Response includes rate limit info
headers = {
    "x-litellm-key-remaining-requests": "49",
    "x-litellm-key-remaining-requests-gpt-4": "49",
    "x-litellm-key-remaining-tokens": "119999",
    "x-litellm-key-remaining-tokens-gpt-4": "29999"
}

# Check if budget exceeded
if response.status_code == 429:
    # Rate limit exceeded
    error = response.json()
    print(f"Rate limited: {error['message']}")
```

### Enforce User Parameter (OpenAI Endpoint)
```yaml
litellm_settings:
  enforce_user_param: True  # Require 'user' in /chat/completions calls
```

```python
# Must include 'user' parameter
response = requests.post(
    "http://localhost:4000/chat/completions",
    json={
        "model": "gpt-4",
        "messages": [...],
        "user": "user-123"  # Required if enforce_user_param=True
    }
)
```

### Multi-Instance Rate Limiting
```yaml
# For distributed deployments
litellm_settings:
  EXPERIMENTAL_MULTI_INSTANCE_RATE_LIMITING: True
  redis_host: "redis.example.com"
  redis_port: 6379
  # Syncs rate limits across all proxy instances via Redis
```

### Custom Rate Limit Tier (Enterprise)
```python
# Define custom tier
tier_config = {
    "tier_name": "premium",
    "max_budget": 1000,
    "tpm_limit": 500000,
    "rpm_limit": 2000,
    "model_rpm_limit": {
        "gpt-4": 500,
        "gpt-3.5-turbo": 1000
    }
}

# Assign to key
key_config = {
    "key_alias": "premium-key",
    "tier": "premium"
}
```

---

## 10. COST TRACKING AND BUDGETS

### Automatic Cost Tracking
```python
# LiteLLM automatically tracks costs for known models
response = litellm.completion(
    model="openai/gpt-4o",
    messages=[{"role": "user", "content": "Hello"}]
)

# Response includes cost
print(f"Cost: ${response.get('_response_ms', 0)}")

# Or via callback
def cost_callback(kwargs, response, start_time, end_time):
    cost = kwargs.get('response_cost', 0)
    tokens = response.usage.total_tokens
    print(f"Cost: ${cost} for {tokens} tokens")

litellm.success_callback = [cost_callback]
```

### Track Cost by User/Team
```python
# Include user identifier in request
response = requests.post(
    "http://localhost:4000/chat/completions",
    json={
        "model": "gpt-4",
        "messages": messages,
        "user": "user-456"  # Tracks cost to this user
    }
)

# Cost automatically attributed to user-456
```

### Metadata Tagging
```python
# Add custom tags for cost tracking
response = litellm.completion(
    model="openai/gpt-4o",
    messages=messages,
    metadata={
        "department": "engineering",
        "project": "chatbot",
        "team": "ai-platform"
    }
)

# Can query costs filtered by metadata
```

### View Spend Reports
```python
# Get daily spend by team
spend_report = requests.get(
    "http://localhost:4000/global/spend/report",
    params={
        "group_by": "team",
        "start_date": "2024-01-01",
        "end_date": "2024-01-31"
    },
    headers={"Authorization": f"Bearer {admin_key}"}
)

# Response: Daily breakdown by team
```

### Customer/End-User Budget
```python
# Track customer spend (no separate API key needed)
response = requests.post(
    "http://localhost:4000/chat/completions",
    json={
        "model": "gpt-4",
        "messages": messages,
        "user": "customer-123"  # customer_id
    }
)

# Cost tracked and attributed to customer-123
# Auto-upserts customer with new spend
```

### Budget Manager Class
```python
from litellm import BudgetManager

# Create budget manager
budget_mgr = BudgetManager()

# Set user budgets
budget_mgr.set_user_budget(
    user_id="user-123",
    budget=100,  # $100 USD
    time_period="30d"
)

# Track spending
budget_mgr.add_user_cost(
    user_id="user-123",
    cost=5.50
)

# Check remaining budget
remaining = budget_mgr.get_user_remaining_budget("user-123")
print(f"Remaining: ${remaining}")

# Alert on high spend
if remaining < 10:
    print("User budget low!")
```

### Model-Specific Budgets (Enterprise)
```python
# Different budget per model
key_config = {
    "key_alias": "advanced-user",
    "model_max_budget": {
        "gpt-4": {
            "budget_limit": 50,
            "time_period": "30d"
        },
        "gpt-3.5-turbo": {
            "budget_limit": 100,
            "time_period": "30d"
        }
    }
}

# Each model has independent budget
```

### Cost Tracking in config.yaml
```yaml
litellm_settings:
  # Database for persistent cost tracking
  database_url: "postgresql://user:pass@localhost/litellm"
  
  # Track all requests
  log_spend: True
  
  # Tags automatically added
  user_agent: True  # Track Claude Code, CLI tools, etc.
```

### View Cost by Model
```python
# LiteLLM maintains model pricing database
import litellm

model_info = litellm.get_model_cost_object("openai/gpt-4o")
# Returns: {
#   "input_cost_per_token": 0.000005,
#   "output_cost_per_token": 0.000015,
#   "tokens_per_minute": 500000,
#   ...
# }
```

### Custom Cost Calculation
```python
# For custom/imported models
response = litellm.completion(
    model="custom/my-model",
    messages=messages,
    litellm_cost_per_token={
        "input": 0.001,      # $0.001 per input token
        "output": 0.002      # $0.002 per output token
    }
)

# Cost calculated as:
# (input_tokens * 0.001) + (output_tokens * 0.002)
```

### Budget Enforcement
```yaml
# Budgets are enforced pre-request
# Request rejected if would exceed budget

litellm_settings:
  database_url: "postgresql://..."  # Required for budget enforcement
```

```python
# Check budget before calling
response = requests.post(
    "http://localhost:4000/chat/completions",
    json={
        "model": "gpt-4",
        "messages": messages,
        "user": "user-456"
    }
)

if response.status_code == 401:
    # Budget exceeded
    error = response.json()
    print(f"Budget error: {error['message']}")
```

---

## VISION/IMAGE PATTERNS

### Basic Vision Usage
```python
from litellm import completion

# Check if model supports vision
supports = completion.supports_vision(model="openai/gpt-4-vision-preview")

# Call with image
response = completion(
    model="openai/gpt-4-vision-preview",
    messages=[
        {
            "role": "user",
            "content": [
                {"type": "text", "text": "What's in this image?"},
                {
                    "type": "image_url",
                    "image_url": {
                        "url": "https://example.com/image.jpg"
                    }
                }
            ]
        }
    ]
)

print(response['choices'][0]['message']['content'])
```

### Base64 Encoded Images
```python
import base64

# Read image file
with open("image.jpg", "rb") as img_file:
    image_data = base64.b64encode(img_file.read()).decode("utf-8")

# Use in message
response = completion(
    model="openai/gpt-4-vision-preview",
    messages=[
        {
            "role": "user",
            "content": [
                {"type": "text", "text": "Describe this image"},
                {
                    "type": "image_url",
                    "image_url": {
                        "url": f"data:image/jpeg;base64,{image_data}"
                    }
                }
            ]
        }
    ]
)
```

### Multiple Images
```python
response = completion(
    model="openai/gpt-4-vision-preview",
    messages=[
        {
            "role": "user",
            "content": [
                {"type": "text", "text": "Compare these images"},
                {
                    "type": "image_url",
                    "image_url": {"url": "https://example.com/image1.jpg"}
                },
                {
                    "type": "image_url",
                    "image_url": {"url": "https://example.com/image2.jpg"}
                }
            ]
        }
    ]
)
```

### Proxy Vision Configuration
```yaml
model_list:
  - model_name: "gpt-4-vision"
    litellm_params:
      model: "openai/gpt-4-vision-preview"
    model_info:
      supports_vision: True  # Mark as vision-capable
```

---

## ADDITIONAL FEATURES

### Response Format (JSON)
```python
# Force JSON output
response = completion(
    model="openai/gpt-4o",
    messages=[
        {
            "role": "user",
            "content": "Return user data as JSON"
        }
    ],
    response_format={"type": "json_object"}
)

# Model returns valid JSON
```

### Temperature and Sampling
```python
response = completion(
    model="openai/gpt-4o",
    messages=messages,
    temperature=0.7,  # 0=deterministic, 2=very random
    top_p=0.9,        # Alternative to temperature
    top_k=40,         # Select from top K tokens
    frequency_penalty=0.5,  # Reduce repetition
    presence_penalty=0.5    # Encourage new topics
)
```

### Max Tokens Limit
```python
response = completion(
    model="openai/gpt-4o",
    messages=messages,
    max_tokens=500  # Limit response length
)
```

### Additional Kwargs
```python
# Pass provider-specific parameters
response = completion(
    model="anthropic/claude-3-opus",
    messages=messages,
    max_tokens=2000,
    temperature=0.5,
    # Provider-specific
    top_k=40,
    **{"custom_param": "value"}
)
```

### Provider API Version
```python
# Azure specific API version
response = completion(
    model="azure/gpt-4-deployment",
    messages=messages,
    api_version="2024-08-01-preview"
)
```

### Custom API Base
```python
# Override API endpoint
response = completion(
    model="anthropic/claude-3-opus",
    messages=messages,
    api_base="https://custom-endpoint.example.com"
)
```

---

## ERROR HANDLING

### Exception Types
```python
from litellm import (
    RateLimitError,
    APIError,
    APIConnectionError,
    Timeout,
    AuthenticationError,
    BadRequestError,
    ServiceUnavailableError,
    ContextWindowExceededError,
    ContentPolicyViolationError
)

try:
    response = completion(model="openai/gpt-4o", messages=messages)
except RateLimitError:
    print("Rate limited - implement backoff")
except ContextWindowExceededError:
    print("Context too large - use smaller model or summarize")
except AuthenticationError:
    print("Invalid API key")
except Timeout:
    print("Request timeout")
except ContentPolicyViolationError:
    print("Content policy violated")
except APIError as e:
    print(f"API error: {e}")
```

### OpenAI Compatibility
```python
# All exceptions inherit from OpenAI exception types
# Existing OpenAI error handlers work directly with LiteLLM

try:
    response = litellm.completion(...)
except OpenAI.APIError:  # Works with LiteLLM too
    pass
```

---

## COMPLETE EXAMPLE: PRODUCTION SETUP

```python
import asyncio
import json
import litellm
from litellm import Router, acompletion
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ProductionLiteLLMSetup:
    def __init__(self):
        # Setup caching
        litellm.cache = litellm.RedisCache(
            host="localhost",
            port=6379
        )
        
        # Setup callbacks
        litellm.callbacks = [self.CostTracker(), self.ErrorHandler()]
        
        # Setup router with fallbacks
        self.router = Router(
            model_list=[
                {
                    "model_name": "primary",
                    "litellm_params": {
                        "model": "openai/gpt-4o",
                        "api_key": "${OPENAI_API_KEY}"
                    }
                },
                {
                    "model_name": "primary",
                    "litellm_params": {
                        "model": "azure/gpt-4-deployment",
                        "api_key": "${AZURE_API_KEY}",
                        "api_base": "${AZURE_API_BASE}"
                    }
                },
                {
                    "model_name": "fallback",
                    "litellm_params": {
                        "model": "anthropic/claude-3-opus-20250219",
                        "api_key": "${ANTHROPIC_API_KEY}"
                    }
                }
            ],
            num_retries=3,
            request_timeout=30,
            fallbacks=[{"primary": ["fallback"]}]
        )
    
    class CostTracker(litellm.CustomLogger):
        async def async_log_success_event(self, kwargs, response, start, end):
            cost = kwargs.get('response_cost', 0)
            model = kwargs.get('model')
            tokens = response.usage.total_tokens if hasattr(response, 'usage') else 0
            logger.info(f"Model: {model}, Cost: ${cost:.4f}, Tokens: {tokens}")
    
    class ErrorHandler(litellm.CustomLogger):
        async def async_log_failure_event(self, kwargs, response, start, end):
            logger.error(f"Failed request to {kwargs['model']}: {response}")
    
    async def complete(self, messages, model="primary", **kwargs):
        """Main completion method with all features"""
        try:
            response = await acompletion(
                model=model,
                messages=messages,
                temperature=kwargs.get('temperature', 0.7),
                max_tokens=kwargs.get('max_tokens', 2000),
                timeout=30,
                # Caching happens automatically
            )
            return response['choices'][0]['message']['content']
        
        except litellm.RateLimitError:
            logger.warning("Rate limited, waiting...")
            await asyncio.sleep(60)
            return await self.complete(messages, model, **kwargs)
        
        except litellm.ContextWindowExceededError:
            logger.warning("Context too large, summarizing...")
            # Handle by summarizing or using larger model
            raise
        
        except Exception as e:
            logger.error(f"Completion failed: {e}")
            raise

# Usage
async def main():
    setup = ProductionLiteLLMSetup()
    
    messages = [
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "Explain quantum computing in simple terms."}
    ]
    
    response = await setup.complete(messages)
    print(response)

asyncio.run(main())
```



---

# API Reference

# LiteLLM Proxy - API Reference & Quick Start

## Table of Contents
1. [Quick Reference](#quick-reference)
2. [LLM API Endpoints](#llm-api-endpoints)
3. [Key Management API](#key-management-api)
4. [User Management API](#user-management-api)
5. [Team Management API](#team-management-api)
6. [Spend & Analytics API](#spend--analytics-api)
7. [Admin API](#admin-api)
8. [Health & Status API](#health--status-api)

---

## Quick Reference

### Base URL
```
http://localhost:4000
```

### Authentication
```
Authorization: Bearer sk-<your-key>
```

### Setup Commands

```bash
# 1. Install
pip install 'litellm[proxy]'

# 2. Create config.yaml
cat > config.yaml << 'CONFIGEOF'
model_list:
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: sk-...

general_settings:
  master_key: sk-1234567890
  database_url: postgresql://...
CONFIGEOF

# 3. Set environment
export OPENAI_API_KEY=sk-...
export DATABASE_URL=postgresql://...
export LITELLM_MASTER_KEY=sk-1234567890

# 4. Run proxy (in shell, not here)
# litellm --config config.yaml

# 5. Generate API key in another shell
curl -X POST http://localhost:4000/key/generate \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{"models": ["gpt-4o"]}'
```

---

## LLM API Endpoints

### Chat Completions

```bash
curl -X POST http://localhost:4000/v1/chat/completions \
  -H "Authorization: Bearer sk-your-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [
      {"role": "system", "content": "You are helpful assistant"},
      {"role": "user", "content": "Hello"}
    ],
    "temperature": 0.7,
    "max_tokens": 1000,
    "top_p": 0.9
  }'
```

### Completions

```bash
curl -X POST http://localhost:4000/v1/completions \
  -H "Authorization: Bearer sk-your-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "prompt": "Once upon a time",
    "temperature": 0.7,
    "max_tokens": 500
  }'
```

### Embeddings

```bash
curl -X POST http://localhost:4000/v1/embeddings \
  -H "Authorization: Bearer sk-your-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "text-embedding-3-small",
    "input": "The quick brown fox"
  }'
```

### List Models

```bash
curl http://localhost:4000/v1/models \
  -H "Authorization: Bearer sk-your-key"
```

### Get Model Info

```bash
curl 'http://localhost:4000/model/info?model=gpt-4o' \
  -H "Authorization: Bearer sk-your-key"
```

---

## Key Management API

### Generate Key

```bash
curl -X POST http://localhost:4000/key/generate \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "models": ["gpt-4o", "gpt-3.5-turbo"],
    "max_budget": 100.0,
    "budget_duration": "30d",
    "rpm_limit": 100,
    "tpm_limit": 50000
  }'
```

### Get Key Info

```bash
curl 'http://localhost:4000/key/info' \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{"key": "sk-abc123def456"}'
```

### Update Key

```bash
curl -X POST http://localhost:4000/key/update \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "sk-abc123def456",
    "max_budget": 200.0,
    "models": ["gpt-4o"]
  }'
```

### Delete Key

```bash
curl -X POST http://localhost:4000/key/delete \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{"key": "sk-abc123def456"}'
```

### List Keys

```bash
curl 'http://localhost:4000/keys' \
  -H "Authorization: Bearer sk-1234567890"
```

---

## User Management API

### Create User

```bash
curl -X POST http://localhost:4000/user/new \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user-123",
    "user_email": "alice@example.com",
    "role": "internal_user",
    "max_budget": 100.0,
    "budget_duration": "30d"
  }'
```

### Get User Info

```bash
curl 'http://localhost:4000/user/info' \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{"user_id": "user-123"}'
```

### Update User

```bash
curl -X POST http://localhost:4000/user/update \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user-123",
    "max_budget": 200.0
  }'
```

### Delete User

```bash
curl -X POST http://localhost:4000/user/delete \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{"user_id": "user-123"}'
```

### List Users

```bash
curl 'http://localhost:4000/user/list' \
  -H "Authorization: Bearer sk-1234567890"
```

---

## Team Management API

### Create Team

```bash
curl -X POST http://localhost:4000/team/new \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "team_id": "team-a",
    "team_alias": "Team A",
    "max_budget": 1000.0,
    "budget_duration": "30d"
  }'
```

### Get Team Info

```bash
curl 'http://localhost:4000/team/info' \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{"team_id": "team-a"}'
```

### Update Team

```bash
curl -X POST http://localhost:4000/team/update \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{
    "team_id": "team-a",
    "max_budget": 2000.0
  }'
```

### Add Team Member

```bash
curl -X POST http://localhost:4000/team/member/add \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{
    "team_id": "team-a",
    "user_id": "user-123",
    "role": "user"
  }'
```

### List Team Members

```bash
curl 'http://localhost:4000/team/members?team_id=team-a' \
  -H "Authorization: Bearer sk-1234567890"
```

### List Teams

```bash
curl 'http://localhost:4000/team/list' \
  -H "Authorization: Bearer sk-1234567890"
```

---

## Spend & Analytics API

### Get Daily Spend

```bash
curl 'http://localhost:4000/spend/daily?start_date=2025-11-01&end_date=2025-11-30' \
  -H "Authorization: Bearer sk-1234567890"
```

### Get Spend by Key

```bash
curl 'http://localhost:4000/key/info' \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{"key": "sk-abc123"}'
```

### Get Spend by User

```bash
curl 'http://localhost:4000/user/info' \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{"user_id": "user-123"}'
```

### Get Spend by Team

```bash
curl 'http://localhost:4000/team/info' \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{"team_id": "team-a"}'
```

---

## Admin API

### Get Proxy Config

```bash
curl 'http://localhost:4000/config' \
  -H "Authorization: Bearer sk-1234567890"
```

### Reload Config

```bash
curl -X POST http://localhost:4000/config/reload \
  -H "Authorization: Bearer sk-1234567890"
```

---

## Health & Status API

### Health Check

```bash
# Full health check
curl http://localhost:4000/health

# Liveness check
curl http://localhost:4000/health/liveliness

# Readiness check
curl http://localhost:4000/health/readiness

# Services health
curl http://localhost:4000/health/services
```

### Response Format

```json
{
  "status": "healthy",
  "timestamp": "2025-11-18T10:00:00Z",
  "models": {
    "gpt-4o": {
      "status": "healthy",
      "latency_ms": 145
    }
  }
}
```

---

## Error Codes

| Code | Error | Solution |
|------|-------|----------|
| 401 | Unauthorized | Check API key starts with 'sk-' |
| 403 | Forbidden | Check key budget and permissions |
| 404 | Not Found | Check model name and endpoint |
| 429 | Rate Limited | Wait or increase rate limits |
| 500 | Server Error | Check logs |
| 503 | Database Unavailable | Check database connection |

---

## Python SDK Examples

### Using LiteLLM SDK

```python
import litellm

litellm.api_base = "http://localhost:4000"
litellm.api_key = "sk-abc123"

response = litellm.completion(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Hello!"}]
)
print(response.choices[0].message.content)
```

### Using OpenAI SDK

```python
from openai import OpenAI

client = OpenAI(
    api_key="sk-abc123",
    base_url="http://localhost:4000"
)

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Hello!"}]
)
print(response.choices[0].message.content)
```

### Using LangChain

```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    model="gpt-4o",
    openai_api_key="sk-abc123",
    openai_api_base="http://localhost:4000"
)

response = llm.invoke("What is the capital of France?")
print(response.content)
```

---



---

# Proxy Server

# LiteLLM Proxy Server: Comprehensive Research Documentation

## Table of Contents
1. [Overview](#overview)
2. [Installation & Quick Start](#installation--quick-start)
3. [CLI Commands](#cli-commands)
4. [Configuration (config.yaml)](#configuration-configyaml)
5. [Virtual Keys & Authentication](#virtual-keys--authentication)
6. [Model Aliases & Routing](#model-aliases--routing)
7. [Spend Tracking & Budgets](#spend-tracking--budgets)
8. [Team & User Management](#team--user-management)
9. [Load Balancing](#load-balancing)
10. [Health Checks](#health-checks)
11. [Logging & Observability](#logging--observability)
12. [Docker Deployment](#docker-deployment)
13. [PostgreSQL Database Setup](#postgresql-database-setup)
14. [Production Best Practices](#production-best-practices)
15. [API Examples](#api-examples)

---

## Overview

### What is LiteLLM Proxy?

LiteLLM Proxy is an **OpenAI-compatible AI Gateway (LLM Proxy)** that provides a unified interface to call 100+ language models with advanced features including:

- **Multi-LLM Support**: Access 100+ models (OpenAI, Azure, Anthropic, Hugging Face, Bedrock, etc.)
- **Unified Interface**: OpenAI ChatCompletions format for all providers
- **Cost Tracking**: Automatic spend tracking per API key, user, team, and model
- **Authentication**: Virtual key management with SHA-256 hashing
- **Load Balancing**: Intelligent routing across multiple deployments
- **Rate Limiting**: RPM/TPM controls at multiple levels
- **Budget Management**: Hard caps and spend limits
- **Error Handling**: Automatic retries and fallbacks
- **Observability**: Integration with Langfuse, Helicone, Datadog, etc.
- **Admin Dashboard**: UI with SSO support
- **Caching**: Prompt caching support
- **Custom Plugins**: Request/response modification capabilities

### Key Statistics

- **Performance**: 8ms P95 latency at 1k RPS
- **Throughput**: 1.5k+ requests/second during load tests
- **Supported Models**: 100+ LLMs across all major providers
- **Database**: PostgreSQL for persistent storage
- **Caching**: Redis support for distributed deployments

---

## Installation & Quick Start

### Prerequisites

```bash
# Install Python 3.8+
python --version

# Install pip dependencies
pip install 'litellm[proxy]'
```

### Minimal Setup

```bash
# Start with a single model
litellm --model gpt-4o

# Start with debugging enabled
litellm --model huggingface/bigcode/starcoder --detailed_debug

# Test the proxy
litellm --test
```

The proxy runs on `http://0.0.0.0:4000` by default.

### Test a Request

```bash
curl http://localhost:4000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Say this is a test"}]
  }'
```

---

## CLI Commands

### Installation & Help

```bash
# Install with proxy support
pip install 'litellm[proxy]'

# View available commands
litellm --help
```

### Starting the Proxy

```bash
# Basic startup with single model
litellm --model gpt-4o

# With configuration file
litellm --config /path/to/config.yaml

# With custom port and host
litellm --host 0.0.0.0 --port 8000

# With debugging
litellm --config config.yaml --detailed_debug

# With specified number of workers
litellm --config config.yaml --num_workers 4
```

### CLI Arguments Reference

| Argument | Description | Example |
|----------|-------------|---------|
| `--config` | Path to config.yaml file | `--config config.yaml` |
| `--model` | Model name/ID to use | `--model gpt-4o` |
| `--api_base` | API base URL | `--api_base https://api.openai.com/v1` |
| `--api_key` | API key | `--api_key sk-...` |
| `--alias` | Model alias | `--alias my-gpt4` |
| `--host` | Server host | `--host 0.0.0.0` |
| `--port` | Server port | `--port 8000` |
| `--num_workers` | Number of workers | `--num_workers 4` |
| `--timeout` | Request timeout (seconds) | `--timeout 60` |
| `--max_tokens` | Max tokens in response | `--max_tokens 2048` |
| `--temperature` | Model temperature | `--temperature 0.7` |
| `--debug` | Enable debug mode | `--debug` |
| `--detailed_debug` | Verbose debugging | `--detailed_debug` |
| `--test` | Test the proxy | `--test` |
| `--drop_params` | Drop unmapped params | `--drop_params` |
| `--run_hypercorn` | Use Hypercorn for HTTP/2 | `--run_hypercorn` |

### Environment Variables for CLI

```bash
# Set master key
export LITELLM_MASTER_KEY=sk-1234567890

# Set database URL
export DATABASE_URL=postgresql://user:password@localhost:5432/litellm

# Set log level
export LITELLM_LOG=INFO  # or DEBUG

# Set API keys
export OPENAI_API_KEY=sk-...
export AZURE_API_KEY=...
export ANTHROPIC_API_KEY=...
```

---

## Configuration (config.yaml)

### Basic Structure

```yaml
# Model list configuration
model_list:
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY}
  
  - model_name: gpt-3.5-turbo
    litellm_params:
      model: gpt-3.5-turbo
      api_key: ${OPENAI_API_KEY}

# General proxy settings
general_settings:
  master_key: sk-1234567890
  database_url: postgresql://user:password@localhost:5432/litellm
  
# Router settings for load balancing
router_settings:
  routing_strategy: simple-shuffle
  num_retries: 2
  timeout: 30

# Global LiteLLM settings
litellm_settings:
  log: INFO
  num_retries: 2
```

### Complete Configuration Example

```yaml
# ==================== MODEL CONFIGURATION ====================
model_list:
  # OpenAI Models
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY}
      api_base: https://api.openai.com/v1
      rpm: 200
      tpm: 90000
    model_info:
      description: "GPT-4 Omni Model"
      max_tokens: 128000
      mode: chat

  - model_name: gpt-3.5-turbo
    litellm_params:
      model: gpt-3.5-turbo
      api_key: ${OPENAI_API_KEY}
      rpm: 3500
      tpm: 90000

  # Azure OpenAI Models
  - model_name: azure-gpt-4
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_API_BASE}
      api_key: ${AZURE_API_KEY}
      api_version: 2024-02-15-preview
      rpm: 100
      tpm: 40000

  # Anthropic Models
  - model_name: claude-3-opus
    litellm_params:
      model: claude-3-5-sonnet-20241022
      api_key: ${ANTHROPIC_API_KEY}
      rpm: 50
      tpm: 40000

  # Load balancing multiple Azure deployments
  - model_name: gpt-4-prod-1
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_API_BASE_1}
      api_key: ${AZURE_API_KEY_1}
      rpm: 100

  - model_name: gpt-4-prod-2
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_API_BASE_2}
      api_key: ${AZURE_API_KEY_2}
      rpm: 100

# ==================== ROUTING & LOAD BALANCING ====================
router_settings:
  # Routing strategy: simple-shuffle, least-busy, usage-based-routing, latency-based-routing, cost-based-routing
  routing_strategy: simple-shuffle
  
  # Number of retries on failure
  num_retries: 2
  
  # Request timeout in seconds
  timeout: 30
  
  # Model aliases for routing
  model_group_alias:
    "gpt-4": "gpt-4o"
    "gpt-4-turbo": "gpt-4o"
  
  # Fallback models on failure
  fallbacks: 
    - "gpt-4o": ["gpt-3.5-turbo"]
  
  # Context window fallbacks
  context_window_fallbacks:
    - "gpt-4o": ["gpt-4o"]
    - "gpt-3.5-turbo": ["gpt-3.5-turbo-16k"]
  
  # Content policy fallbacks
  content_policy_fallbacks:
    - "gpt-4o": ["gpt-3.5-turbo"]
  
  # Redis for distributed deployments
  redis_host: ${REDIS_HOST}
  redis_password: ${REDIS_PASSWORD}
  redis_port: 6379

# ==================== GENERAL SETTINGS ====================
general_settings:
  # Master key for admin operations (must start with 'sk-')
  master_key: ${LITELLM_MASTER_KEY}
  
  # Database URL (PostgreSQL)
  database_url: ${DATABASE_URL}
  
  # Database connection pool settings
  database_connection_pool_limit: 10
  database_connection_timeout: 60
  
  # Allow requests if database is unavailable (graceful degradation)
  allow_requests_on_db_unavailable: true
  
  # Max parallel requests for the entire proxy
  max_parallel_requests: 10000
  
  # Health checks
  background_health_checks: true
  health_check_interval: 300
  
  # Alerting (supports: slack, email)
  alerting: ["slack"]
  
  # Store models in database for UI
  store_model_in_db: true
  
  # Disable error logs in production
  disable_error_logs: false
  
  # Batch write spend updates (seconds)
  proxy_batch_write_at: 60
  
  # Custom authentication
  # custom_auth: path.to.custom_auth_function
  
  # Encryption salt for API keys
  litellm_salt_key: ${LITELLM_SALT_KEY}
  
  # Default budgets for new internal users
  max_internal_user_budget: 100.0
  internal_user_budget_duration: "30d"

# ==================== LITELLM SETTINGS ====================
litellm_settings:
  # Logging level: DEBUG, INFO, WARNING, ERROR
  log: INFO
  
  # Global number of retries
  num_retries: 2
  
  # Global retry policy
  retry_policy: ExponentialBackoffRetry
  
  # Fallbacks (applies to all models not explicitly configured)
  fallbacks:
    - "gpt-4o": ["gpt-3.5-turbo"]
    - "gpt-3.5-turbo": ["gpt-4o"]
  
  # Cache settings
  cache: true
  cache_type: redis
  cache_host: ${REDIS_HOST}
  cache_port: 6379
  cache_password: ${REDIS_PASSWORD}
  
  # Request timeout
  request_timeout: 60
  
  # Success and failure callbacks for observability
  success_callback: ["langfuse", "helicone"]
  failure_callback: ["langfuse", "helicone"]
  
  # Batch mode for better performance
  batch_mode: true
  
  # Track usage metadata
  track_cost: true

# ==================== ENVIRONMENT VARIABLES ====================
environment_variables:
  OPENAI_API_KEY: ${OPENAI_API_KEY}
  AZURE_API_BASE: ${AZURE_API_BASE}
  AZURE_API_KEY: ${AZURE_API_KEY}
  ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
  LANGFUSE_PUBLIC_KEY: ${LANGFUSE_PUBLIC_KEY}
  LANGFUSE_SECRET_KEY: ${LANGFUSE_SECRET_KEY}
  HELICONE_API_KEY: ${HELICONE_API_KEY}
  REDIS_HOST: redis
  REDIS_PASSWORD: ${REDIS_PASSWORD}

# ==================== FILE INCLUDES ====================
include:
  - models_config.yaml
  - team_models.yaml
```

### Configuration by File Management

Create separate config files and include them:

```yaml
# main_config.yaml
include:
  - models/openai_models.yaml
  - models/azure_models.yaml
  - models/anthropic_models.yaml
  - teams/team_a_models.yaml

general_settings:
  master_key: ${LITELLM_MASTER_KEY}
  database_url: ${DATABASE_URL}
```

```yaml
# models/openai_models.yaml
model_list:
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY}
```

---

## Virtual Keys & Authentication

### Overview

Virtual keys are bearer tokens stored in the database that identify and authorize requests to the proxy. They require PostgreSQL and a master key to manage.

### Setup Requirements

```bash
# Set environment variables
export DATABASE_URL=postgresql://user:password@localhost:5432/litellm
export LITELLM_MASTER_KEY=sk-1234567890  # Must start with 'sk-'
export LITELLM_SALT_KEY=sk-salt-key     # For encryption
```

### config.yaml Setup

```yaml
general_settings:
  master_key: ${LITELLM_MASTER_KEY}
  database_url: ${DATABASE_URL}
  store_model_in_db: true
```

### Generate Virtual Keys via API

```bash
# Generate a new key
curl -X POST http://localhost:4000/key/generate \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "models": ["gpt-4o", "gpt-3.5-turbo"],
    "duration": "30d",
    "max_budget": 100.0
  }'

# Response
{
  "key": "sk-skdsjkdsjkd",
  "expires": "2025-01-18",
  "models": ["gpt-4o", "gpt-3.5-turbo"],
  "max_budget": 100.0
}
```

### Get Key Information

```bash
# View key spend and info
curl -X GET http://localhost:4000/key/info \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{"key": "sk-skdsjkdsjkd"}'

# Response
{
  "key": "sk-skdsjkdsjkd",
  "spend": 23.45,
  "max_budget": 100.0,
  "models": ["gpt-4o", "gpt-3.5-turbo"],
  "created_at": "2025-11-18T10:00:00Z"
}
```

### Update or Delete Keys

```bash
# Update a key
curl -X POST http://localhost:4000/key/update \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "sk-skdsjkdsjkd",
    "max_budget": 200.0,
    "models": ["gpt-4o"]
  }'

# Delete a key
curl -X POST http://localhost:4000/key/delete \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{"key": "sk-skdsjkdsjkd"}'
```

### Key Features

- **SHA-256 Hashing**: Keys stored securely in database
- **Multi-tier Caching**: In-memory → Redis → PostgreSQL
- **Key Rotation**: Automatic rotation based on time intervals
- **Custom Headers**: Configure custom key header (default: Authorization)
- **Association**: Keys can be linked to users, teams, or both

---

## Model Aliases & Routing

### Model Group Aliases

```yaml
router_settings:
  model_group_alias:
    # All gpt-4 requests → gpt-4o
    "gpt-4": "gpt-4o"
    
    # All gpt-4-turbo requests → gpt-4o
    "gpt-4-turbo": "gpt-4o"
    
    # Complex aliases with options
    "gpt-4-legacy":
      model: "gpt-3.5-turbo"
      hidden: true  # Don't show in /v1/models
```

### Routing Strategies

```yaml
router_settings:
  # Option 1: Simple-Shuffle (Default, Best Performance)
  # Randomly distributes requests with weighting
  routing_strategy: simple-shuffle
  
  # Option 2: Least-Busy
  # Routes to deployment with fewest active requests
  routing_strategy: least-busy
  
  # Option 3: Usage-Based-Routing
  # Routes based on token usage/limits (not recommended for production)
  routing_strategy: usage-based-routing
  
  # Option 4: Latency-Based-Routing
  # Routes to fastest-responding deployment
  routing_strategy: latency-based-routing
  
  # Option 5: Cost-Based-Routing
  # Routes to lowest cost provider
  routing_strategy: cost-based-routing
```

### Load Balancing Multiple Deployments

```yaml
model_list:
  # Load balance across multiple Azure deployments
  - model_name: gpt-4-prod
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_API_BASE_1}
      api_key: ${AZURE_API_KEY_1}
      rpm: 100
      tpm: 40000

  - model_name: gpt-4-prod
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_API_BASE_2}
      api_key: ${AZURE_API_KEY_2}
      rpm: 100
      tpm: 40000

  - model_name: gpt-4-prod
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_API_BASE_3}
      api_key: ${AZURE_API_KEY_3}
      rpm: 100
      tpm: 40000
```

### Team-Based Model Routing

```bash
# Create a team with model aliases
curl -X POST http://localhost:4000/team/new \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "team_id": "team-a",
    "team_alias": "Team A",
    "model_aliases": {
      "gpt-4": "gpt-4o",
      "gpt-3.5": "gpt-3.5-turbo"
    }
  }'
```

---

## Spend Tracking & Budgets

### Automatic Cost Tracking

LiteLLM automatically tracks spend for all known models with built-in pricing data.

### Key-Level Budgets

```bash
# Generate a key with budget
curl -X POST http://localhost:4000/key/generate \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "models": ["gpt-4o"],
    "max_budget": 100.0,
    "budget_duration": "30d"
  }'
```

### View Spend Information

```bash
# Get key spend
curl http://localhost:4000/key/info \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{"key": "sk-skdsjkdsjkd"}'

# Get user spend
curl http://localhost:4000/user/info \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{"user_id": "user123"}'

# Get team spend
curl http://localhost:4000/team/info \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{"team_id": "team-a"}'

# Get daily spend breakdown
curl 'http://localhost:4000/spend/daily?start_date=2025-11-01&end_date=2025-11-30' \
  -H "Authorization: Bearer sk-1234567890"
```

### Budget Types

| Budget Type | Scope | Use Case |
|------------|-------|----------|
| Key-Level | Single API key | Individual app/service limits |
| User-Level | Single user account | Internal user budgets |
| Team-Level | Team of users | Team/department budgets |
| Tag-Based | Requests with tags | Cost center tracking |
| Customer/End-User | Customer accounts | Per-customer billing |
| Provider | LLM provider | Provider-level caps |
| Model | Specific model | Model-level budgets |

### Tag-Based Budget Tracking

```bash
# Create spend tracking by tags
curl -X POST http://localhost:4000/spend/logs \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "spend_logs_metadata": {
      "cost_center": "engineering",
      "project": "recommendation-engine",
      "customer": "customer-123"
    }
  }'
```

---

## Team & User Management

### User Management Hierarchy

```
Organization
  ├── Team A
  │   ├── User 1 (Admin)
  │   ├── User 2 (Member)
  │   └── API Keys
  └── Team B
      ├── User 3 (Admin)
      └── API Keys
```

### User Roles

**Proxy-Wide Roles:**
- **PROXY_ADMIN**: Full control over entire proxy
- **PROXY_ADMIN_VIEWER**: Read-only access to all proxy data
- **INTERNAL_USER**: Can create keys, view own spend

**Team-Specific Roles:**
- **TEAM_ADMIN**: Can manage team members and settings
- **TEAM_USER**: Can view own spend, cannot create/delete keys (configurable)

### Create a User

```bash
curl -X POST http://localhost:4000/user/new \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user-123",
    "user_alias": "alice@company.com",
    "user_email": "alice@company.com",
    "role": "internal_user",
    "teams": ["team-a"],
    "max_budget": 100.0,
    "budget_duration": "30d"
  }'
```

### Create a Team

```bash
curl -X POST http://localhost:4000/team/new \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "team_id": "team-a",
    "team_alias": "Team A",
    "max_budget": 1000.0,
    "budget_duration": "30d",
    "tpm_limit": 100000,
    "rpm_limit": 1000
  }'
```

### Add User to Team

```bash
curl -X POST http://localhost:4000/team/member/add \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "team_id": "team-a",
    "user_id": "user-123",
    "role": "user",
    "member_budget": 100.0
  }'
```

### List Teams and Users

```bash
# List all teams
curl http://localhost:4000/team/list \
  -H "Authorization: Bearer sk-1234567890"

# List team members
curl 'http://localhost:4000/team/members?team_id=team-a' \
  -H "Authorization: Bearer sk-1234567890"

# List users
curl http://localhost:4000/user/list \
  -H "Authorization: Bearer sk-1234567890"
```

### Update Team Budget

```bash
curl -X POST http://localhost:4000/team/update \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "team_id": "team-a",
    "max_budget": 2000.0,
    "budget_duration": "30d"
  }'
```

### CLI User Management

```bash
# Install CLI
pip install litellm-proxy-cli

# List users
litellm-proxy users list --url http://localhost:4000 --key sk-1234567890

# Create user
litellm-proxy users create \
  --url http://localhost:4000 \
  --key sk-1234567890 \
  --email user@example.com \
  --role internal_user \
  --alias "Alice" \
  --team team1 \
  --max-budget 100.0

# Get user
litellm-proxy users get --id user-123

# Delete user
litellm-proxy users delete --id user-123
```

---

## Load Balancing

### Routing Strategies

```yaml
router_settings:
  routing_strategy: simple-shuffle  # Recommended for production
  num_retries: 2
  timeout: 30
```

### Load Balancing Example

```yaml
model_list:
  # Define same model multiple times for load balancing
  - model_name: gpt-4-prod
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_BASE_1}
      api_key: ${AZURE_KEY_1}
      rpm: 100

  - model_name: gpt-4-prod
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_BASE_2}
      api_key: ${AZURE_KEY_2}
      rpm: 100

  - model_name: gpt-4-prod
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_BASE_3}
      api_key: ${AZURE_KEY_3}
      rpm: 100

router_settings:
  routing_strategy: simple-shuffle
  num_retries: 2
  timeout: 30
```

### Rate Limiting with Load Balancing

```yaml
model_list:
  - model_name: gpt-4
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_KEY}
      rpm: 200     # 200 requests per minute
      tpm: 90000   # 90k tokens per minute
```

### Fallbacks and Cooldowns

```yaml
litellm_settings:
  # Model fallbacks on failure
  fallbacks:
    - "gpt-4o": ["gpt-3.5-turbo"]
    - "gpt-3.5-turbo": ["gpt-4o"]
  
  # Context window fallbacks
  context_window_fallbacks:
    - "gpt-4o": ["gpt-4o"]
    - "gpt-3.5-turbo": ["gpt-3.5-turbo-16k"]

model_list:
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_KEY}
    model_info:
      allowed_fails: 3  # Cooldown after 3 failures/minute
      cooldown_time: 300  # 5 minute cooldown
```

### Redis for Distributed Load Balancing

```yaml
router_settings:
  redis_host: redis.example.com
  redis_password: ${REDIS_PASSWORD}
  redis_port: 6379
  redis_ttl: 300

general_settings:
  database_url: postgresql://...
```

---

## Health Checks

### Health Check Endpoints

```bash
# Full health check (makes API calls)
curl http://localhost:4000/health

# Readiness check (includes database check)
curl http://localhost:4000/health/readiness

# Liveness check (basic alive check)
curl http://localhost:4000/health/liveliness

# Service integrations health
curl http://localhost:4000/health/services

# Shared health status across pods
curl http://localhost:4000/health/shared-status
```

### Background Health Checks Configuration

```yaml
general_settings:
  background_health_checks: true
  health_check_interval: 300  # Check every 5 minutes
  health_check_timeout: 60    # 60 second timeout

model_list:
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_KEY}
    model_info:
      disable_background_health_check: false
      health_check_timeout: 30  # Override for this model
      mode: chat
```

### Custom Health Check Prompt

```bash
export DEFAULT_HEALTH_CHECK_PROMPT="What is 2+2?"
litellm --config config.yaml
```

### Health Check Privacy

```yaml
general_settings:
  health_check_details: false  # Hide URLs and error details in responses
```

---

## Logging & Observability

### Supported Integrations

**Observability Platforms:**
- Langfuse
- Helicone
- Datadog
- Sentry
- Honeycomb
- OpenTelemetry

**Cloud Storage:**
- AWS S3
- Google Cloud Storage
- Azure Blob Storage

**Queues:**
- AWS SQS
- Google Cloud PubSub

**Databases:**
- DynamoDB

**Analytics:**
- Langsmith
- MLflow
- Deepeval
- Lunary
- Arize AI
- Langtrace
- Galileo
- Athina

### Configuration Example

```yaml
litellm_settings:
  # Callbacks on success
  success_callback: ["langfuse", "helicone", "datadog"]
  
  # Callbacks on failure
  failure_callback: ["langfuse", "helicone", "sentry"]
  
  # Message redaction for PII
  redact_message_input: false
  redact_message_output: false

environment_variables:
  LANGFUSE_PUBLIC_KEY: ${LANGFUSE_PUBLIC_KEY}
  LANGFUSE_SECRET_KEY: ${LANGFUSE_SECRET_KEY}
  LANGFUSE_HOST: https://api.langfuse.com
  HELICONE_API_KEY: ${HELICONE_API_KEY}
  DATADOG_API_KEY: ${DATADOG_API_KEY}
```

### Langfuse Integration

```yaml
litellm_settings:
  success_callback: ["langfuse"]
  failure_callback: ["langfuse"]

environment_variables:
  LANGFUSE_PUBLIC_KEY: pk-lf-xxx
  LANGFUSE_SECRET_KEY: sk-lf-xxx
  LANGFUSE_HOST: https://api.langfuse.com
```

### Helicone Integration

```yaml
model_list:
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY}
      # Add Helicone header
      headers:
        "helicone-auth": "Bearer ${HELICONE_API_KEY}"

litellm_settings:
  success_callback: ["helicone"]

environment_variables:
  HELICONE_API_KEY: ${HELICONE_API_KEY}
```

### S3 Logging

```yaml
litellm_settings:
  success_callback: ["s3"]

environment_variables:
  AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
  AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
  AWS_REGION_NAME: us-east-1
  S3_BUCKET_NAME: litellm-logs
```

### Logging Levels

```bash
# Enable debug logging
export LITELLM_LOG=DEBUG
litellm --config config.yaml

# Info level (default)
export LITELLM_LOG=INFO
litellm --config config.yaml

# Suppress logs
export LITELLM_LOG=WARNING
litellm --config config.yaml
```

---

## Docker Deployment

### Basic Docker Setup

```bash
# Pull official image
docker pull ghcr.io/berriai/litellm:main-stable

# Run with config file
docker run -d \
  -v $(pwd)/config.yaml:/app/config.yaml \
  -e LITELLM_MASTER_KEY=sk-1234567890 \
  -e OPENAI_API_KEY=sk-... \
  -p 4000:4000 \
  --name litellm \
  ghcr.io/berriai/litellm:main-stable \
  --config /app/config.yaml
```

### Docker with Database Support

```bash
# Use database-optimized image
docker run -d \
  -v $(pwd)/config.yaml:/app/config.yaml \
  -e LITELLM_MASTER_KEY=sk-1234567890 \
  -e DATABASE_URL=postgresql://user:password@postgres:5432/litellm \
  -e OPENAI_API_KEY=sk-... \
  -p 4000:4000 \
  --name litellm \
  ghcr.io/berriai/litellm-database:main-stable \
  --config /app/config.yaml
```

### Docker Compose Setup

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: litellm
      POSTGRES_USER: litellm
      POSTGRES_PASSWORD: litellm_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U litellm"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis Cache (optional, for distributed deployments)
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # LiteLLM Proxy
  litellm:
    image: ghcr.io/berriai/litellm-database:main-stable
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      LITELLM_MASTER_KEY: ${LITELLM_MASTER_KEY:-sk-1234567890}
      LITELLM_SALT_KEY: ${LITELLM_SALT_KEY:-sk-salt-1234567890}
      DATABASE_URL: postgresql://litellm:litellm_password@postgres:5432/litellm
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      AZURE_API_KEY: ${AZURE_API_KEY}
      AZURE_API_BASE: ${AZURE_API_BASE}
      ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      LANGFUSE_PUBLIC_KEY: ${LANGFUSE_PUBLIC_KEY}
      LANGFUSE_SECRET_KEY: ${LANGFUSE_SECRET_KEY}
      HELICONE_API_KEY: ${HELICONE_API_KEY}
      LITELLM_LOG: INFO
    ports:
      - "4000:4000"
    volumes:
      - ./config.yaml:/app/config.yaml
      - ./litellm_logs:/app/logs
    command: >
      litellm --config /app/config.yaml
      --host 0.0.0.0
      --port 4000
      --num_workers 4
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4000/health"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  postgres_data:
```

### Run Docker Compose

```bash
# Create environment file
cat > .env << EOF
LITELLM_MASTER_KEY=sk-1234567890
LITELLM_SALT_KEY=sk-salt-1234567890
OPENAI_API_KEY=sk-...
AZURE_API_KEY=...
AZURE_API_BASE=...
ANTHROPIC_API_KEY=...
LANGFUSE_PUBLIC_KEY=...
LANGFUSE_SECRET_KEY=...
HELICONE_API_KEY=...


---

# Configuration Examples

# LiteLLM Proxy - Configuration Examples

## Table of Contents
1. [Basic Configuration](#basic-configuration)
2. [Multi-Provider Setup](#multi-provider-setup)
3. [Azure-Focused Setup](#azure-focused-setup)
4. [Advanced Routing](#advanced-routing)
5. [High-Availability Setup](#high-availability-setup)
6. [Cost Optimization Setup](#cost-optimization-setup)
7. [Enterprise Setup](#enterprise-setup)

---

## Basic Configuration

### Minimal config.yaml

```yaml
model_list:
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY}

general_settings:
  master_key: sk-1234567890
  database_url: ${DATABASE_URL}
```

### Startup Command

```bash
# Install
pip install 'litellm[proxy]'

# Run with config
export OPENAI_API_KEY=sk-...
export DATABASE_URL=postgresql://user:pass@localhost/litellm
export LITELLM_MASTER_KEY=sk-1234567890
litellm --config config.yaml
```

---

## Multi-Provider Setup

### Complete Multi-Provider Configuration

```yaml
model_list:
  # OpenAI Models
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY}
      rpm: 200
      tpm: 90000

  - model_name: gpt-3.5-turbo
    litellm_params:
      model: gpt-3.5-turbo
      api_key: ${OPENAI_API_KEY}
      rpm: 3500
      tpm: 90000

  # Anthropic Models
  - model_name: claude-3-5-sonnet
    litellm_params:
      model: claude-3-5-sonnet-20241022
      api_key: ${ANTHROPIC_API_KEY}
      rpm: 50
      tpm: 40000

  # Azure OpenAI Models
  - model_name: azure-gpt-4
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_API_BASE}
      api_key: ${AZURE_API_KEY}
      api_version: 2024-02-15-preview
      rpm: 100
      tpm: 40000

  # Hugging Face Models
  - model_name: mistral-7b
    litellm_params:
      model: huggingface/mistralai/Mistral-7B-Instruct-v0.1
      api_key: ${HUGGINGFACE_API_KEY}
      api_base: https://api-inference.huggingface.co/models
      rpm: 100
      tpm: 20000

  # AWS Bedrock
  - model_name: bedrock-claude
    litellm_params:
      model: bedrock/anthropic.claude-3-sonnet-20240229-v1:0
      aws_access_key_id: ${AWS_ACCESS_KEY_ID}
      aws_secret_access_key: ${AWS_SECRET_ACCESS_KEY}
      aws_region_name: us-east-1

  # Google Vertex AI
  - model_name: vertex-gemini
    litellm_params:
      model: vertex_ai/gemini-pro
      project_id: ${GOOGLE_PROJECT_ID}
      location: us-central1

router_settings:
  routing_strategy: simple-shuffle
  num_retries: 2
  timeout: 30
  
  # Model aliases
  model_group_alias:
    "gpt-4": "gpt-4o"
    "gpt-4-turbo": "gpt-4o"
    "claude": "claude-3-5-sonnet"

general_settings:
  master_key: ${LITELLM_MASTER_KEY}
  database_url: ${DATABASE_URL}
  background_health_checks: true
  health_check_interval: 300

litellm_settings:
  log: INFO
  num_retries: 2
  fallbacks:
    - "gpt-4o": ["gpt-3.5-turbo", "azure-gpt-4"]
    - "claude-3-5-sonnet": ["gpt-4o"]

environment_variables:
  OPENAI_API_KEY: ${OPENAI_API_KEY}
  ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
  AZURE_API_BASE: ${AZURE_API_BASE}
  AZURE_API_KEY: ${AZURE_API_KEY}
  HUGGINGFACE_API_KEY: ${HUGGINGFACE_API_KEY}
  AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
  AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
  GOOGLE_PROJECT_ID: ${GOOGLE_PROJECT_ID}
```

---

## Azure-Focused Setup

### Multiple Azure Deployments with Load Balancing

```yaml
model_list:
  # GPT-4 Turbo across 3 Azure regions
  - model_name: gpt-4-turbo-prod
    litellm_params:
      model: azure/gpt-4-turbo
      api_base: https://eastus-gpt4.openai.azure.com/
      api_key: ${AZURE_EASTUS_KEY}
      api_version: 2024-02-15-preview
      rpm: 150
      tpm: 40000

  - model_name: gpt-4-turbo-prod
    litellm_params:
      model: azure/gpt-4-turbo
      api_base: https://westeurope-gpt4.openai.azure.com/
      api_key: ${AZURE_WESTEU_KEY}
      api_version: 2024-02-15-preview
      rpm: 150
      tpm: 40000

  - model_name: gpt-4-turbo-prod
    litellm_params:
      model: azure/gpt-4-turbo
      api_base: https://uksouth-gpt4.openai.azure.com/
      api_key: ${AZURE_UKSOUTH_KEY}
      api_version: 2024-02-15-preview
      rpm: 150
      tpm: 40000

  # GPT-35-Turbo across 2 regions
  - model_name: gpt-35-turbo
    litellm_params:
      model: azure/gpt-35-turbo
      api_base: https://eastus-gpt35.openai.azure.com/
      api_key: ${AZURE_EASTUS_KEY}
      api_version: 2024-02-15-preview
      rpm: 500
      tpm: 90000

  - model_name: gpt-35-turbo
    litellm_params:
      model: azure/gpt-35-turbo
      api_base: https://westeurope-gpt35.openai.azure.com/
      api_key: ${AZURE_WESTEU_KEY}
      api_version: 2024-02-15-preview
      rpm: 500
      tpm: 90000

router_settings:
  routing_strategy: latency-based-routing
  num_retries: 2
  timeout: 45
  
  # Fallback from GPT-4 to GPT-3.5 if needed
  fallbacks:
    - "gpt-4-turbo-prod": ["gpt-35-turbo"]

general_settings:
  master_key: ${LITELLM_MASTER_KEY}
  database_url: ${DATABASE_URL}
  background_health_checks: true
  health_check_interval: 300

litellm_settings:
  log: INFO
  num_retries: 3
  
environment_variables:
  AZURE_EASTUS_KEY: ${AZURE_EASTUS_KEY}
  AZURE_WESTEU_KEY: ${AZURE_WESTEU_KEY}
  AZURE_UKSOUTH_KEY: ${AZURE_UKSOUTH_KEY}
```

### Docker Compose for Azure Deployment

```bash
# .env file
LITELLM_MASTER_KEY=sk-1234567890
DATABASE_URL=postgresql://litellm:password@postgres:5432/litellm
AZURE_EASTUS_KEY=...
AZURE_WESTEU_KEY=...
AZURE_UKSOUTH_KEY=...
LITELLM_LOG=INFO
```

---

## Advanced Routing

### Intelligent Routing Based on Model Capabilities

```yaml
model_list:
  # High-performance models
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY}
    model_info:
      description: "Best quality, highest cost"
      max_tokens: 128000
      supports_vision: true

  # Balance cost vs quality
  - model_name: gpt-4-turbo
    litellm_params:
      model: gpt-4-turbo-preview
      api_key: ${OPENAI_API_KEY}
    model_info:
      description: "Good quality, moderate cost"
      max_tokens: 128000

  # Budget-friendly option
  - model_name: gpt-3.5-turbo
    litellm_params:
      model: gpt-3.5-turbo
      api_key: ${OPENAI_API_KEY}
      rpm: 3500
      tpm: 90000
    model_info:
      description: "Lower cost, good for simple tasks"
      max_tokens: 16384

  # Specialized models
  - model_name: claude-3-opus
    litellm_params:
      model: claude-3-opus-20240229
      api_key: ${ANTHROPIC_API_KEY}
    model_info:
      description: "Best for reasoning and analysis"
      max_tokens: 200000
      supports_vision: true

router_settings:
  routing_strategy: least-busy
  num_retries: 2
  timeout: 30
  
  # Model aliases for routing based on user needs
  model_group_alias:
    # Route all generic requests to gpt-4o
    "best": "gpt-4o"
    
    # Route budget requests to gpt-3.5
    "budget": "gpt-3.5-turbo"
    
    # Route analysis to Claude
    "analyze": "claude-3-opus"
    
    # Route fallbacks
    "gpt-4": "gpt-4o"
  
  # Fallback chain: GPT-4 → GPT-3.5 → Claude
  fallbacks:
    - "gpt-4o": ["gpt-4-turbo", "gpt-3.5-turbo", "claude-3-opus"]
    - "gpt-4-turbo": ["gpt-3.5-turbo", "claude-3-opus"]
    - "gpt-3.5-turbo": ["claude-3-opus"]
  
  # Context window fallbacks
  context_window_fallbacks:
    - "gpt-3.5-turbo": ["gpt-4-turbo", "gpt-4o"]
    - "gpt-4-turbo": ["gpt-4o"]
  
  # Content policy fallbacks
  content_policy_fallbacks:
    - "gpt-4o": ["gpt-3.5-turbo"]

general_settings:
  master_key: ${LITELLM_MASTER_KEY}
  database_url: ${DATABASE_URL}

litellm_settings:
  log: INFO
```

---

## High-Availability Setup

### Multi-Region with Redis and Database Failover

```yaml
model_list:
  # Primary region
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY_PRIMARY}
      rpm: 200
      tpm: 90000

  # Secondary region
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY_SECONDARY}
      rpm: 200
      tpm: 90000

  # Tertiary region
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY_TERTIARY}
      rpm: 200
      tpm: 90000

router_settings:
  routing_strategy: latency-based-routing
  num_retries: 3
  timeout: 30
  
  # Shared state across regions via Redis
  redis_host: ${REDIS_HOST}
  redis_password: ${REDIS_PASSWORD}
  redis_port: 6379
  redis_ttl: 300

general_settings:
  master_key: ${LITELLM_MASTER_KEY}
  database_url: ${DATABASE_URL}
  
  # Graceful degradation
  allow_requests_on_db_unavailable: true
  
  # Health checks
  background_health_checks: true
  health_check_interval: 60
  
  # Batch writes for consistency
  proxy_batch_write_at: 30

litellm_settings:
  log: INFO
  
  # Aggressive retries for HA
  num_retries: 3
  
  # Use Redis for caching
  cache: true
  cache_type: redis
  cache_host: ${REDIS_HOST}
  cache_port: 6379
  cache_password: ${REDIS_PASSWORD}
  
  # Fallbacks between regions
  fallbacks:
    - "gpt-4o": ["gpt-3.5-turbo"]
```

### Docker Compose with HA

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: litellm
      POSTGRES_USER: litellm
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U litellm"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Proxy instance 1
  litellm-1:
    image: ghcr.io/berriai/litellm-database:main-stable
    environment:
      LITELLM_MASTER_KEY: ${LITELLM_MASTER_KEY}
      DATABASE_URL: postgresql://litellm:${DB_PASSWORD}@postgres:5432/litellm
      OPENAI_API_KEY: ${OPENAI_API_KEY_PRIMARY}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      LITELLM_LOG: INFO
    ports:
      - "4001:4000"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

  # Proxy instance 2
  litellm-2:
    image: ghcr.io/berriai/litellm-database:main-stable
    environment:
      LITELLM_MASTER_KEY: ${LITELLM_MASTER_KEY}
      DATABASE_URL: postgresql://litellm:${DB_PASSWORD}@postgres:5432/litellm
      OPENAI_API_KEY: ${OPENAI_API_KEY_SECONDARY}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      LITELLM_LOG: INFO
    ports:
      - "4002:4000"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

  # Load balancer (Nginx)
  nginx:
    image: nginx:latest
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - litellm-1
      - litellm-2

volumes:
  postgres_data:
```

### Nginx Load Balancer Configuration

```nginx
upstream litellm_backend {
    least_conn;
    server litellm-1:4000 max_fails=3 fail_timeout=30s;
    server litellm-2:4000 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name api.example.com;

    # Health check endpoint
    location /health {
        access_log off;
        proxy_pass http://litellm_backend;
    }

    # API endpoints
    location / {
        proxy_pass http://litellm_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 300s;
        
        # Buffering
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }
}
```

---

## Cost Optimization Setup

### Budget-Conscious Configuration

```yaml
model_list:
  # Tier 1: Budget models for simple tasks
  - model_name: gpt-3.5-turbo
    litellm_params:
      model: gpt-3.5-turbo
      api_key: ${OPENAI_API_KEY}
      rpm: 3500
      tpm: 90000
    model_info:
      cost_per_1k_input: 0.0005
      cost_per_1k_output: 0.0015

  # Tier 2: Mid-range for complex tasks
  - model_name: gpt-4-turbo
    litellm_params:
      model: gpt-4-turbo-preview
      api_key: ${OPENAI_API_KEY}
      rpm: 200
      tpm: 40000
    model_info:
      cost_per_1k_input: 0.01
      cost_per_1k_output: 0.03

  # Tier 3: Premium for reasoning
  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY}
      rpm: 200
      tpm: 90000
    model_info:
      cost_per_1k_input: 0.005
      cost_per_1k_output: 0.015

router_settings:
  routing_strategy: cost-based-routing
  num_retries: 2
  
  # Route based on cost
  model_group_alias:
    "cheap": "gpt-3.5-turbo"
    "balanced": "gpt-4-turbo"
    "best": "gpt-4o"

general_settings:
  master_key: ${LITELLM_MASTER_KEY}
  database_url: ${DATABASE_URL}

litellm_settings:
  log: INFO
  track_cost: true
```

### Budget Enforcement

```bash
# Create team with budget limits
curl -X POST http://localhost:4000/team/new \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "team_id": "finance",
    "team_alias": "Finance Department",
    "max_budget": 5000.0,
    "budget_duration": "30d",
    "tpm_limit": 100000,
    "rpm_limit": 1000
  }'

# Create user with budget
curl -X POST http://localhost:4000/user/new \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "john-doe",
    "user_email": "john@finance.com",
    "role": "internal_user",
    "teams": ["finance"],
    "max_budget": 100.0,
    "budget_duration": "7d"
  }'
```

---

## Enterprise Setup

### Complete Enterprise Configuration

```yaml
model_list:
  # Production models with high rate limits
  - model_name: gpt-4o-prod
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY_PROD}
      rpm: 200
      tpm: 90000
    model_info:
      description: "Production GPT-4 Omni"

  - model_name: gpt-3.5-turbo-prod
    litellm_params:
      model: gpt-3.5-turbo
      api_key: ${OPENAI_API_KEY_PROD}
      rpm: 3500
      tpm: 90000

  - model_name: claude-3-opus
    litellm_params:
      model: claude-3-opus-20240229
      api_key: ${ANTHROPIC_API_KEY}

  # Development models with lower limits
  - model_name: gpt-4o-dev
    litellm_params:
      model: gpt-4o
      api_key: ${OPENAI_API_KEY_DEV}
      rpm: 50
      tpm: 20000

  # Azure models for compliance
  - model_name: azure-gpt-4-compliant
    litellm_params:
      model: azure/gpt-4
      api_base: ${AZURE_API_BASE}
      api_key: ${AZURE_API_KEY}
      api_version: 2024-02-15-preview

router_settings:
  routing_strategy: simple-shuffle
  num_retries: 2
  timeout: 30
  redis_host: ${REDIS_HOST}
  redis_password: ${REDIS_PASSWORD}
  redis_port: 6379

general_settings:
  master_key: ${LITELLM_MASTER_KEY}
  database_url: ${DATABASE_URL}
  database_connection_pool_limit: 10
  allow_requests_on_db_unavailable: true
  background_health_checks: true
  health_check_interval: 300
  alerting: ["slack"]
  proxy_batch_write_at: 60
  disable_error_logs: true
  store_model_in_db: true

litellm_settings:
  log: INFO
  num_retries: 2
  fallbacks:
    - "gpt-4o-prod": ["gpt-3.5-turbo-prod"]
    - "gpt-4o-dev": ["gpt-3.5-turbo-prod"]
  context_window_fallbacks:
    - "gpt-3.5-turbo": ["gpt-4o"]
  
  success_callback: ["langfuse", "helicone"]
  failure_callback: ["langfuse", "helicone"]
  
  cache: true
  cache_type: redis
  cache_host: ${REDIS_HOST}
  cache_port: 6379
  cache_password: ${REDIS_PASSWORD}

environment_variables:
  OPENAI_API_KEY_PROD: ${OPENAI_API_KEY_PROD}
  OPENAI_API_KEY_DEV: ${OPENAI_API_KEY_DEV}
  ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
  AZURE_API_BASE: ${AZURE_API_BASE}
  AZURE_API_KEY: ${AZURE_API_KEY}
  LANGFUSE_PUBLIC_KEY: ${LANGFUSE_PUBLIC_KEY}
  LANGFUSE_SECRET_KEY: ${LANGFUSE_SECRET_KEY}
  HELICONE_API_KEY: ${HELICONE_API_KEY}
  REDIS_HOST: ${REDIS_HOST}
  REDIS_PASSWORD: ${REDIS_PASSWORD}
```

### Enterprise Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: litellm-proxy
  namespace: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: litellm-proxy
  template:
    metadata:
      labels:
        app: litellm-proxy
    spec:
      containers:
      - name: litellm
        image: ghcr.io/berriai/litellm-database:main-stable
        imagePullPolicy: Always
        ports:
        - containerPort: 4000
          name: http
        env:
        - name: LITELLM_MASTER_KEY
          valueFrom:
            secretKeyRef:
              name: litellm-secrets
              key: master-key
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: litellm-secrets
              key: database-url
        - name: REDIS_HOST
          value: redis.production.svc.cluster.local
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: litellm-secrets
              key: redis-password
        - name: OPENAI_API_KEY_PROD
          valueFrom:
            secretKeyRef:
              name: llm-keys
              key: openai-prod
        - name: ANTHROPIC_API_KEY
          valueFrom:
            secretKeyRef:
              name: llm-keys
              key: anthropic
        livenessProbe:
          httpGet:
            path: /health/liveliness
            port: 4000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/readiness
            port: 4000
          initialDelaySeconds: 10
          periodSeconds: 5
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        volumeMounts:
        - name: config
          mountPath: /app/config.yaml
          subPath: config.yaml
      volumes:
      - name: config
        configMap:
          name: litellm-config
---
apiVersion: v1
kind: Service
metadata:
  name: litellm-proxy
  namespace: production
spec:
  selector:
    app: litellm-proxy
  ports:
  - protocol: TCP
    port: 4000
    targetPort: 4000
  type: LoadBalancer
```

---

## Testing Configuration

### Test Script

```python
#!/usr/bin/env python3
import requests
import json
from datetime import datetime

BASE_URL = "http://localhost:4000"
MASTER_KEY = "sk-1234567890"

def test_health():
    """Test proxy health"""
    response = requests.get(f"{BASE_URL}/health")
    print(f"[{datetime.now()}] Health: {response.status_code}")
    print(json.dumps(response.json(), indent=2))

def test_models():
    """List available models"""
    response = requests.get(
        f"{BASE_URL}/v1/models",
        headers={"Authorization": f"Bearer {MASTER_KEY}"}
    )
    print(f"\n[{datetime.now()}] Models: {response.status_code}")
    print(json.dumps(response.json(), indent=2))

def test_completion():
    """Test chat completion"""
    response = requests.post(
        f"{BASE_URL}/v1/chat/completions",
        headers={
            "Authorization": f"Bearer {MASTER_KEY}",
            "Content-Type": "application/json"
        },
        json={
            "model": "gpt-3.5-turbo",
            "messages": [{"role": "user", "content": "Say hello"}],
            "temperature": 0.7
        }
    )
    print(f"\n[{datetime.now()}] Completion: {response.status_code}")
    print(json.dumps(response.json(), indent=2))

if __name__ == "__main__":
    test_health()
    test_models()
    test_completion()
```

---

