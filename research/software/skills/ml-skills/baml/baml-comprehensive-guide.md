# BAML (Basically A Made-up Language) - Comprehensive Research Report

## Executive Summary

BAML (Basically A Made-up Language) is a domain-specific language (DSL) developed by BoundaryML for building LLM-powered applications with structured outputs and improved reliability. It transforms prompt engineering from string manipulation into schema engineering, where developers focus on defining precise input/output models to achieve reliable AI outputs.

**Key Resources:**
- Documentation: https://docs.boundaryml.com
- GitHub: https://github.com/BoundaryML/baml
- License: Apache 2.0 (open source)

---

## 1. Core Features

### What BAML Does

BAML treats prompts as **typed functions** rather than simple strings. It provides:

- **Type-safe structured outputs** - Full type safety even when streaming
- **Auto-generated client code** - Generates Python, TypeScript, Ruby, Go, Java, C#, and Rust clients
- **Schema-Aligned Parsing (SAP)** - Robust parsing algorithm that handles flexible LLM outputs like markdown in JSON or chain-of-thought reasoning
- **Wide LLM support** - OpenAI, Anthropic, Gemini, Vertex, Bedrock, Azure OpenAI, and OpenAI-compatible APIs
- **IDE integration** - Native VSCode support with prompt visualization and testing

### Main Capabilities

1. **Structured Output Extraction** - Parse complex data structures from LLM responses
2. **Multi-model Support** - Switch between providers with minimal code changes
3. **Streaming** - Type-safe streaming interfaces with React hooks support
4. **Testing** - Built-in test framework for validating AI functions
5. **Retry & Fallback** - Production-ready resilience patterns
6. **Dynamic Types** - Runtime type modifications for flexible schemas

### Performance Benefits

- Type definitions use **60% fewer tokens** than JSON schemas
- SAP parsing fixes are applied in **<10ms** (orders of magnitude faster than re-prompting)
- Token efficiency leads to better cost, latency, and accuracy

---

## 2. Syntax and Language Structure

### File Format

BAML files use the `.baml` extension and are stored in the `baml_src/` directory by convention.

### Basic Syntax Rules

- **No colons** between property names and types (unlike Python/TypeScript)
- **Block strings** use `#"..."#` delimiters for multi-line content
- **Comments** use `//` for single-line
- Property names must start with a letter and contain only letters, numbers, and underscores

### Block String Syntax

```baml
// Single-line string
"Hello, world!"

// Multi-line block string (automatically dedented)
#"
  This is a multi-line prompt.
  It will be automatically dedented.

  First and last newlines are stripped.
"#
```

### Generator Configuration

Generators define code generation targets:

```baml
generator target {
  output_type "python/pydantic"      // or "typescript", "ruby/sorbet", "go", etc.
  output_dir "../baml_client"        // Relative to baml_src/
  version "0.71.0"                   // Runtime version
  default_client_mode "async"        // or "sync"
  on_generate "black . && isort ."   // Post-generation commands
}
```

**Supported Output Types:**
- `python/pydantic` (latest) or `python/pydantic/v1` (legacy)
- `typescript` (Node.js) or `typescript/react` (React/Next.js)
- `ruby/sorbet` (beta)
- `go` (requires `client_package_name`)
- `rest/openapi` (API specification)

---

## 3. Type System

### Primitive Types

```baml
bool       // Boolean: true or false
int        // Integer numbers
float      // Floating-point numbers
string     // Text strings
null       // Null value
```

### Literal Types

Introduced in v0.61.0, primitives can be constrained to specific values:

```baml
function Classify(text: string) -> "bug" | "enhancement" | "question"
```

### Optional Types

Denote values that might be absent with `?`:

```baml
class User {
  name string
  email string?    // Optional field
  age int?
}
```

### Union Types

Allow multiple possible types using `|`:

```baml
// Order matters for parsing precedence!
// "1" parsed as int with this:
type IntOrString = int | string

// "1" parsed as string with this:
type StringOrInt = string | int
```

### List/Array Types

Collections of uniform types:

```baml
string[]           // Array of strings
int[][]            // 2D array of integers
Message[]          // Array of custom class
```

### Map Types

Key-value mappings (keys must be strings, enums, or literal strings):

```baml
map<string, int>              // String keys, int values
map<string, string[]>         // String keys, array values
map<Category, Product>        // Enum keys
```

### Type Aliases

Introduced in v0.71.0 for complex type simplification:

```baml
type GraphMap = map<string, string[]>
type Response = string | Error | null

// Recursive aliases supported through containers
type TreeNode = map<string, TreeNode>
```

### Multimodal Types

```baml
// Images
Image.from_url("https://example.com/image.png")
Image.from_base64("...")

// Audio
Audio.from_url("https://example.com/audio.mp3")
Audio.from_base64("...")

// PDFs (base64 only, no URL support)
Pdf.from_base64("...")

// Video
Video.from_url("https://example.com/video.mp4")
```

---

## 4. Enum Definitions

Enums define a set of named constants, ideal for classification tasks:

### Basic Enum

```baml
enum MessageType {
  SPAM
  NOT_SPAM
}
```

### Enum with Descriptions

```baml
enum TicketCategory {
  ACCOUNT
    @description("Issues related to user accounts, login, or profile")
  BILLING
    @description("Payment, subscription, or invoice related")
  TECHNICAL
    @description("Bug reports, errors, or technical problems")
  GENERAL_QUERY
    @description("General questions or information requests")
}
```

### Enum Attributes

- `@alias("name")` - Alternative name for LLM comprehension
- `@description("...")` - Context for the LLM
- `@skip` - Exclude from output schema
- `@@dynamic` - Allow runtime modifications

### Dynamic Enums

```baml
enum DynamicCategory {
  @@dynamic    // Values can be added at runtime
}
```

---

## 5. Class Definitions

Classes define complex data structures for inputs and outputs:

### Basic Class

```baml
class Resume {
  name string
  email string
  skills string[]
  experience Experience[]
}

class Experience {
  company string
  title string
  duration string
  description string?
}
```

### Class with Attributes

```baml
class Person {
  full_name string @alias("name") @description("The person's full legal name")
  birth_date string @alias("dob") @description("Date of birth in YYYY-MM-DD format")
  age int?
}
```

### Field Attributes

- `@alias("name")` - Rename field for LLM while preserving code name
- `@description("...")` - Contextual information for prompts

### Class Attributes

- `@@dynamic` - Allow runtime field additions

```baml
class FlexibleOutput {
  known_field string
  @@dynamic    // Additional fields can be added at runtime
}
```

### Constraints

- Default values are **not supported**
- Optional properties default to `None`/`null`
- Inheritance is **not supported** - use composition instead
- Recursive definitions are supported

---

## 6. Function Definitions

### Modern Function Syntax

Every BAML prompt is a function with parameters, return type, client, and prompt:

```baml
function ExtractResume(resume_text: string) -> Resume {
  client "openai/gpt-4o"
  prompt #"
    Extract the resume information from the following text.

    {{ ctx.output_format }}

    Resume:
    ---
    {{ resume_text }}
    ---
  "#
}
```

### Function with Complex Parameters

```baml
function ChatAgent(messages: Message[], tone: "happy" | "sad") -> string {
  client "anthropic/claude-sonnet-4-20250514"
  prompt #"
    Be a {{ tone }} bot.

    {{ ctx.output_format }}

    {% for m in messages %}
    {{ _.role(m.role) }}
    {{ m.content }}
    {% endfor %}
  "#
}
```

### Function Components

1. **Name and Signature** - `function Name(params) -> ReturnType`
2. **Client** - Which LLM to use
3. **Prompt** - The template with Jinja syntax

### Classification Example

```baml
enum MessageType {
  SPAM
  NOT_SPAM
}

function ClassifyText(input: string) -> MessageType {
  client "openai/gpt-4o-mini"
  prompt #"
    Classify the following message as SPAM or NOT_SPAM.

    {{ ctx.output_format }}

    {{ _.role("user") }}
    {{ input }}
  "#
}
```

### Multi-label Classification

```baml
class TicketClassification {
  labels TicketLabel[]
}

function ClassifyTicket(ticket: string) -> TicketClassification {
  client "openai/gpt-4o-mini"
  prompt #"
    You are a support agent. Analyze the ticket and select all applicable labels.

    {{ ctx.output_format }}

    {{ _.role("user") }}
    {{ ticket }}
  "#
}
```

---

## 7. Client Configuration

### Shorthand Syntax

Quick configuration for common providers:

```baml
function MyFunc(input: string) -> string {
  client "openai/gpt-4o"           // Provider/model shorthand
  // or
  client "anthropic/claude-sonnet-4-20250514"
  prompt #"..."#
}
```

### Named Client Configuration

Full configuration with custom options:

```baml
client<llm> MyOpenAI {
  provider "openai"
  options {
    model "gpt-4o"
    api_key env.OPENAI_API_KEY     // Default
    base_url "https://api.openai.com/v1"
    temperature 0.7
    max_tokens 2000
  }
}
```

### OpenAI Configuration

```baml
client<llm> GPT4o {
  provider "openai"
  options {
    model "gpt-4o"
    api_key env.MY_OPENAI_KEY      // Custom env var
    temperature 0.1

    // Role configuration
    default_role "user"
    allowed_roles ["system", "user", "assistant"]

    // Custom headers
    headers {
      "X-Custom-Header" "value"
    }
  }
}
```

### Anthropic Configuration

```baml
client<llm> Claude {
  provider "anthropic"
  options {
    model "claude-sonnet-4-20250514"
    api_key env.ANTHROPIC_API_KEY   // Default
    max_tokens 4096
    temperature 0

    // Enable prompt caching
    allowed_role_metadata ["cache_control"]
    headers {
      "anthropic-beta" "prompt-caching-2024-07-31"
    }
  }
}
```

### Supported Providers

| Provider | API Endpoint | Default API Key |
|----------|-------------|-----------------|
| `openai` | `/chat/completions` | `env.OPENAI_API_KEY` |
| `anthropic` | `/v1/messages` | `env.ANTHROPIC_API_KEY` |
| `google-ai` | Gemini endpoint | `env.GOOGLE_API_KEY` |
| `vertex-ai` | Vertex endpoint | Service account |
| `aws-bedrock` | Converse API | AWS credentials |
| `azure-openai` | Azure `/chat/completions` | `env.AZURE_OPENAI_KEY` |
| `openai-generic` | OpenAI-compatible | Custom |

### OpenAI-Generic for Other Providers

```baml
client<llm> Ollama {
  provider "openai-generic"
  options {
    model "llama2"
    base_url "http://localhost:11434/v1"
  }
}

client<llm> Together {
  provider "openai-generic"
  options {
    model "meta-llama/Llama-3-70b-chat-hf"
    base_url "https://api.together.xyz/v1"
    api_key env.TOGETHER_API_KEY
  }
}
```

---

## 8. Client Strategies

### Retry Policy

Configure automatic retries on failures:

```baml
retry_policy ExponentialBackoff {
  max_retries 3
  strategy {
    type "exponential_backoff"
    initial_interval_ms 500
    max_interval_ms 10000
    multiplier 2
  }
}

client<llm> ReliableGPT {
  provider "openai"
  retry_policy ExponentialBackoff
  options {
    model "gpt-4o"
  }
}
```

### Fallback Strategy

Chain multiple clients for resilience:

```baml
client<llm> GPT4 {
  provider "openai"
  options { model "gpt-4o" }
}

client<llm> Claude {
  provider "anthropic"
  options { model "claude-sonnet-4-20250514" }
}

client<llm> GPT35 {
  provider "openai"
  options { model "gpt-3.5-turbo" }
}

// Try GPT4 first, then Claude, then GPT-3.5
client<llm> ReliableClient {
  provider "fallback"
  retry_policy MyRetryPolicy   // Applied after all fallbacks fail
  options {
    strategy [
      GPT4
      Claude
      GPT35
    ]
  }
}
```

### Nested Fallbacks

```baml
client<llm> PremiumClients {
  provider "fallback"
  options {
    strategy [GPT4, Claude]
  }
}

client<llm> AllClients {
  provider "fallback"
  options {
    strategy [
      PremiumClients   // Try premium first
      GPT35            // Then fallback
    ]
  }
}
```

### Round-Robin Strategy

Distribute load across multiple clients:

```baml
client<llm> LoadBalanced {
  provider "round-robin"
  options {
    strategy [
      GPT4Instance1
      GPT4Instance2
      GPT4Instance3
    ]
  }
}
```

---

## 9. Template Strings (Jinja Syntax)

BAML uses Jinja2 templating for dynamic prompts.

### Variable Interpolation

```baml
prompt #"
  Process this text: {{ input_text }}

  User name: {{ user.name }}
  User email: {{ user.email }}
"#
```

### Context Variables

#### ctx.output_format

Injects the output schema into the prompt:

```baml
function Extract(text: string) -> Person {
  client "openai/gpt-4o"
  prompt #"
    Extract person information.

    {{ ctx.output_format }}

    Text: {{ text }}
  "#
}
```

**Customization options:**

```baml
{{ ctx.output_format(
  prefix="Respond in JSON matching this schema:",
  always_hoist_enums=true,
  or_splitter="|"
) }}
```

#### ctx.client

Access client metadata:

```baml
{% if ctx.client.provider == "anthropic" %}
  <Message>{{ content }}</Message>
{% else %}
  {{ content }}
{% endif %}
```

### Role Tags

Use `_.role()` to set message roles:

```baml
prompt #"
  {{ _.role("system") }}
  You are a helpful assistant.

  {{ _.role("user") }}
  {{ user_message }}
"#
```

### Loops

```baml
function ProcessMessages(messages: Message[]) -> string {
  client "openai/gpt-4o"
  prompt #"
    Process these messages:

    {% for message in messages %}
    {{ _.role(message.role) }}
    {{ message.content }}
    {% endfor %}
  "#
}
```

**Loop object properties:**

- `loop.index` / `loop.index0` - Current position (1-based / 0-based)
- `loop.first` / `loop.last` - Boolean checks
- `loop.length` - Total items
- `loop.previtem` / `loop.nextitem` - Adjacent items

### Conditionals

```baml
prompt #"
  {% if user.is_premium %}
    Premium support response:
  {% else %}
    Standard response:
  {% endif %}

  {{ ctx.output_format }}
"#
```

### Reusable Template Strings

Create reusable prompt components:

```baml
template_string FormatMessages(messages: Message[]) #"
  {% for message in messages %}
    {% if ctx.client.provider == "anthropic" %}
      <Message role="{{ message.role }}">{{ message.content }}</Message>
    {% else %}
      {{ message.role }}: {{ message.content }}
    {% endif %}
  {% endfor %}
"#

function Chat(messages: Message[]) -> string {
  client Claude
  prompt #"
    {{ FormatMessages(messages) }}

    {{ ctx.output_format }}
  "#
}
```

---

## 10. Testing

### Basic Test Structure

```baml
test TestExtraction {
  functions [ExtractResume]
  args {
    resume_text #"
      John Doe
      Email: john@example.com
      Skills: Python, TypeScript, BAML
    "#
  }
}
```

### Multiple Test Cases

```baml
test SpamTest {
  functions [ClassifyText]
  args {
    input "Click here to win $1000!!!"
  }
}

test NotSpamTest {
  functions [ClassifyText]
  args {
    input "Meeting at 3pm tomorrow"
  }
}
```

### Tests with Assertions

```baml
test TestWithAssert {
  functions [ClassifyText]
  args {
    input "Buy now! Limited offer!"
  }
  @@assert {{ this == "SPAM" }}
}
```

### Testing with Media

```baml
test ImageTest {
  functions [ExtractReceipt]
  args {
    image {
      file "../images/receipt.png"     // Relative to BAML file
    }
  }
}

test URLImageTest {
  functions [ExtractReceipt]
  args {
    image {
      url "https://example.com/receipt.png"
    }
  }
}
```

### Dynamic Types in Tests

```baml
test DynamicTest {
  functions [FlexibleExtract]
  args {
    input "Some text"
  }
  type_builder {
    dynamic class FlexibleOutput {
      custom_field string
    }
  }
}
```

---

## 11. Complete Example: Resume Parser

```baml
// Types
class Resume {
  name string
  email string?
  phone string?
  skills string[]
  experience Experience[]
  education Education[]
}

class Experience {
  company string
  title string
  start_date string
  end_date string?
  description string?
}

class Education {
  institution string
  degree string
  field string?
  graduation_year int?
}

// Client configuration
client<llm> GPT4o {
  provider "openai"
  options {
    model "gpt-4o"
    temperature 0
  }
}

// Function definition
function ExtractResume(resume_text: string) -> Resume {
  client GPT4o
  prompt #"
    You are an expert resume parser. Extract structured information from the resume below.

    Guidelines:
    - Extract all available information
    - Use null for missing optional fields
    - Format dates as "Month Year" (e.g., "January 2020")
    - List skills as individual items

    {{ ctx.output_format }}

    {{ _.role("user") }}
    Resume:
    ---
    {{ resume_text }}
    ---
  "#
}

// Test
test BasicResumeTest {
  functions [ExtractResume]
  args {
    resume_text #"
      Jane Smith
      jane.smith@email.com | (555) 123-4567

      SKILLS
      Python, TypeScript, Machine Learning, BAML, React

      EXPERIENCE
      Senior Engineer at TechCorp
      January 2020 - Present
      Led development of AI-powered features

      EDUCATION
      MIT - MS Computer Science, 2019
    "#
  }
}
```

---

## 12. Complete Example: Chatbot with Tools

```baml
// Message class
class Message {
  role "user" | "assistant"
  content string
}

// Tool definitions
class SearchTool {
  query string @description("The search query")
}

class CalculateTool {
  expression string @description("Mathematical expression to evaluate")
}

// Union type for tool selection
type Tool = SearchTool | CalculateTool | string

// Main function
function ChatWithTools(messages: Message[]) -> Tool {
  client "anthropic/claude-sonnet-4-20250514"
  prompt #"
    You are a helpful assistant with access to tools.

    Available tools:
    - SearchTool: Search the web for information
    - CalculateTool: Perform mathematical calculations
    - Or respond with a string if no tool is needed

    {{ ctx.output_format }}

    {% for m in messages %}
    {{ _.role(m.role) }}
    {{ m.content }}
    {% endfor %}
  "#
}

// Fallback client for reliability
client<llm> ReliableChatClient {
  provider "fallback"
  options {
    strategy [
      "anthropic/claude-sonnet-4-20250514"
      "openai/gpt-4o"
    ]
  }
}
```

---

## 13. Usage in Application Code

### Python

```python
from baml_client import b
from baml_client.types import Message

# Simple function call
result = b.ExtractResume(resume_text="John Doe...")
print(result.name)
print(result.skills)

# Chat with messages
messages = [
    Message(role="user", content="Hello!"),
    Message(role="assistant", content="Hi! How can I help?"),
    Message(role="user", content="What's the weather?")
]
response = b.ChatWithTools(messages)

# Streaming
async for partial in b.stream.ExtractResume(resume_text):
    print(partial)
```

### TypeScript

```typescript
import { b } from './baml_client';
import { Message } from './baml_client/types';

// Simple function call
const result = await b.ExtractResume({ resume_text: "John Doe..." });
console.log(result.name);
console.log(result.skills);

// Chat with messages
const messages: Message[] = [
  { role: "user", content: "Hello!" },
  { role: "assistant", content: "Hi! How can I help?" },
  { role: "user", content: "What's the weather?" }
];
const response = await b.ChatWithTools({ messages });

// Streaming
for await (const partial of b.stream.ExtractResume({ resume_text })) {
  console.log(partial);
}
```

---

## 14. Best Practices

### Prompt Engineering

1. **Always include `{{ ctx.output_format }}`** - Essential for structured outputs
2. **Use `_.role()` for chat models** - Properly format message roles
3. **Add descriptions to enums and classes** - Improves LLM understanding
4. **Use type aliases for complex types** - Improves readability

### Client Configuration

1. **Use fallback strategies in production** - Ensure reliability
2. **Configure retry policies** - Handle transient failures
3. **Set appropriate temperature** - Lower for extraction, higher for generation

### Testing

1. **Test edge cases** - Empty inputs, malformed data
2. **Test with real data samples** - Validate against actual use cases
3. **Use assertions** - Verify output structure and values

### Type Design

1. **Prefer composition over inheritance** - BAML doesn't support inheritance
2. **Use optionals for uncertain fields** - Mark with `?`
3. **Consider union types** - When output varies based on input
4. **Order union types carefully** - Parsing order matters

---

## 15. Summary

BAML revolutionizes prompt engineering by treating prompts as **typed functions**. Its key innovations include:

- **Schema Engineering** - Focus on precise input/output models
- **Multi-Language Support** - Single BAML definition, multiple language clients
- **SAP Algorithm** - Robust parsing for flexible LLM outputs
- **Token Efficiency** - 60% fewer tokens than JSON schemas
- **Production Features** - Retry, fallback, round-robin, streaming

BAML is ideal for:
- Structured data extraction
- Classification tasks
- Chatbots and conversational AI
- Tool-calling and function execution
- Any application requiring reliable, typed LLM outputs

---

## References

- **Official Documentation**: https://docs.boundaryml.com
- **GitHub Repository**: https://github.com/BoundaryML/baml
- **Examples Repository**: https://github.com/BoundaryML/baml-examples
- **Interactive Playground**: https://baml-examples.vercel.app
