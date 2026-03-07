# BAML: Patterns, Best Practices, and Advanced Features

## Overview

BAML (Boundary AI Markup Language) is a domain-specific language developed by BoundaryML for building type-safe LLM applications. It transforms prompt engineering into schema engineering, enabling developers to define structured outputs, test prompts before deployment, and generate type-safe clients across multiple programming languages.

**Key Value Propositions:**
- Type-safe structured outputs that work on Day 1 of any model release
- Universal LLM support (OpenAI, Anthropic, Gemini, Bedrock, Azure, Ollama, etc.)
- Multi-language code generation (Python, TypeScript, Ruby, Go, Rust, Java, C#)
- Integrated testing and validation directly in the IDE

---

## 1. Common Patterns

### 1.1 Structured Data Extraction

BAML excels at extracting structured data from unstructured text. The pattern involves defining a schema and a function that returns that schema.

```baml
// Define the output schema
class Resume {
  name string
  email string
  phone string?
  skills string[]
  experience Experience[]
}

class Experience {
  company string
  role string
  startDate string
  endDate string?
  description string
}

// Define the extraction function
function ExtractResume(resumeText: string) -> Resume {
  client "openai/gpt-4o"
  prompt #"
    Extract structured information from the following resume.

    {{ ctx.output_format }}

    {{ _.role("user") }}
    {{ resumeText }}
  "#
}
```

**Best Practices:**
- Use `@description` annotations to guide the LLM on field semantics
- Mark optional fields with `?` to handle missing data gracefully
- Use arrays for repeating elements

### 1.2 Single-Label Classification

Classification is one of BAML's core use cases. Use enums to define possible categories.

```baml
enum MessageType {
  SPAM
  NOT_SPAM
}

function ClassifyMessage(input: string) -> MessageType {
  client "openai/gpt-4o-mini"
  prompt #"
    Classify the following message as spam or not spam.

    {{ ctx.output_format }}

    {{ _.role("user") }}
    {{ input }}
  "#
}
```

### 1.3 Multi-Label Classification

For scenarios requiring multiple labels, use a class containing an array of enums.

```baml
enum TicketCategory {
  ACCOUNT
  BILLING
  TECHNICAL
  GENERAL_QUERY
  URGENT
}

class TicketClassification {
  categories TicketCategory[]
  priority int @description("Priority from 1 (low) to 5 (high)")
  summary string
}

function ClassifyTicket(ticket: string) -> TicketClassification {
  client "openai/gpt-4o"
  prompt #"
    Analyze this support ticket and classify it with all applicable categories.

    {{ ctx.output_format }}

    {{ _.role("user") }}
    {{ ticket }}
  "#
}
```

### 1.4 Literal Types (Union Types)

Use literal types for constrained string values.

```baml
class DealAnalysis {
  dealType "merger" | "acquisition" @description("Type of business deal")
  amount float @description("The monetary value of the deal")
  currency string @description("Currency code (USD, EUR, etc.)")
  companies string[] @description("Names of companies involved")
}
```

### 1.5 Multi-Step Workflows and Agents

BAML treats prompts as composable functions, enabling multi-step workflows.

```baml
// Step 1: Extract entities
function ExtractEntities(text: string) -> Entity[] {
  client "openai/gpt-4o"
  prompt #"
    Extract all named entities from the text.
    {{ ctx.output_format }}
    {{ _.role("user") }} {{ text }}
  "#
}

// Step 2: Classify sentiment for each entity
function AnalyzeSentiment(entity: string, context: string) -> SentimentResult {
  client "openai/gpt-4o-mini"
  prompt #"
    Analyze the sentiment towards this entity in the given context.
    {{ ctx.output_format }}

    Entity: {{ entity }}
    Context: {{ context }}
  "#
}
```

**Agent Pattern:**
An agent is essentially a while loop that calls a BAML function with state:

```python
from baml_client import b

def run_agent(initial_query: str):
    messages = [{"role": "user", "content": initial_query}]

    while True:
        response = b.AgentStep(messages=messages)

        if response.action == "complete":
            return response.result

        # Execute tool and add to messages
        tool_result = execute_tool(response.tool_call)
        messages.append({"role": "tool", "content": tool_result})
```

---

## 2. Testing

BAML makes testing a first-class citizen. Tests can be written in `.baml` files and run before any application code is written.

### 2.1 Basic Test Structure

```baml
test SpamTest {
  functions [ClassifyMessage]
  args {
    input "Buy cheap watches now! Limited time offer!!!"
  }
  @@assert({{ this == "SPAM" }})
}

test NotSpamTest {
  functions [ClassifyMessage]
  args {
    input "Hey, are we still meeting for coffee tomorrow?"
  }
  @@assert({{ this == "NOT_SPAM" }})
}
```

### 2.2 Complex Object Tests

```baml
test ResumeExtractionTest {
  functions [ExtractResume]
  args {
    resumeText #"
      John Doe
      john.doe@email.com | (555) 123-4567

      Experience:
      - Senior Engineer at TechCorp (2020-Present)
        Led development of distributed systems
    "#
  }
  @@assert({{ this.name == "John Doe" }})
  @@assert({{ this.email == "john.doe@email.com" }})
  @@check(has_experience, {{ this.experience|length > 0 }})
}
```

### 2.3 Checks vs Asserts

- **@@assert**: Hard guarantee - test fails immediately if condition is false
- **@@check**: Soft validation - test continues, result is recorded for inspection

```baml
test ValidationExample {
  functions [ExtractData]
  args { text "sample text" }

  // Hard requirements
  @@assert({{ this.required_field|length > 0 }})

  // Soft checks - recorded but don't fail the test
  @@check(reasonable_length, {{ this.text|length < 1000 }})
  @@check(has_date, {{ this.date != null }})
}
```

### 2.4 Test Context Variables

Available in test expressions:
- `this` - the computed result (shorthand for `_.result`)
- `_.result` - explicit result reference
- `_.latency_ms` - execution time in milliseconds
- `_.checks.$NAME` - access to prior check results

### 2.5 Running Tests

**CLI Commands:**
```bash
# Run all tests
baml-cli test

# Filter by function name
baml-cli test -i "ClassifyMessage::"

# Run tests in parallel
baml-cli test --parallel 5

# List available tests
baml-cli test --list
```

**VSCode Playground:**
- Click play button next to any test to run it instantly
- View rendered prompts and raw API responses
- Run multiple tests in parallel

### 2.6 Media Input Testing

BAML supports testing with images, audio, PDFs, and videos:

```baml
test ImageExtractionTest {
  functions [ExtractFromImage]
  args {
    image {
      file "receipts/sample-receipt.png"  // Relative to baml_src/
    }
  }
}

test URLImageTest {
  functions [ExtractFromImage]
  args {
    image {
      url "https://example.com/image.png"
    }
  }
}
```

---

## 3. Retry Strategies

BAML provides built-in retry policies and fallback strategies for robust LLM calls.

### 3.1 Basic Retry Policy

```baml
retry_policy RetryTwice {
  max_retries 2
}

client<llm> MyClient {
  provider openai
  retry_policy RetryTwice
  options {
    model "gpt-4o"
    api_key env.OPENAI_API_KEY
  }
}
```

### 3.2 Exponential Backoff

```baml
retry_policy Exponential {
  max_retries 3
  strategy {
    type exponential_backoff
    delay_ms 300
    multiplier 1.5
    max_delay_ms 10000
  }
}

client<llm> ResilientClient {
  provider openai
  retry_policy Exponential
  options {
    model "gpt-4o"
    api_key env.OPENAI_API_KEY
  }
}
```

### 3.3 Constant Delay Retry

```baml
retry_policy Constant {
  max_retries 2
  strategy {
    type constant_delay
    delay_ms 500
  }
}
```

### 3.4 Fallback Clients

Use fallback strategies to try alternative models when primary fails:

```baml
client<llm> PrimaryClient {
  provider openai
  options {
    model "gpt-4o"
    api_key env.OPENAI_API_KEY
  }
}

client<llm> FallbackClient {
  provider anthropic
  options {
    model "claude-3-haiku-20240307"
    api_key env.ANTHROPIC_API_KEY
  }
}

client<llm> ResilientClient {
  provider fallback
  options {
    strategy [PrimaryClient, FallbackClient]
  }
  retry_policy RetryTwice  // Applied after entire fallback chain fails
}
```

### 3.5 Round-Robin Load Balancing

Distribute requests across multiple clients:

```baml
client<llm> LoadBalanced {
  provider round-robin
  options {
    strategy [
      "openai/gpt-4o",
      "anthropic/claude-3-5-sonnet-20241022",
      "google-ai/gemini-1.5-pro"
    ]
  }
}
```

### 3.6 Retry Behavior Notes

**Important:** BAML retries are for API availability issues only:
- Retries occur when the API endpoint is down (e.g., api.openai.com is unreachable)
- Application errors (malformed requests, validation failures) are NOT retried
- This prevents wasted API calls on errors that won't resolve with retries

---

## 4. Streaming

BAML provides type-safe streaming with semantically valid partial objects as tokens arrive.

### 4.1 Python Streaming

```python
from baml_client import b, partial_types, types

# Synchronous streaming
def stream_extraction(receipt: str):
    stream = b.stream.ExtractReceiptInfo(receipt)

    for partial in stream:
        # Access partially-filled fields
        print(f"Parsed {len(partial.items or [])} items so far")
        if partial.total:
            print(f"Current total: {partial.total}")

    # Get complete, validated result
    final = stream.get_final_response()
    return final

# Async streaming
async def async_stream(receipt: str):
    from baml_client.async_client import b

    stream = b.stream.ExtractReceiptInfo(receipt)

    async for partial in stream:
        print(f"Items: {len(partial.items or [])}")

    final = await stream.get_final_response()
    return final
```

### 4.2 TypeScript Streaming

```typescript
import { b } from './baml_client'

const streamExample = async (receipt: string) => {
  const stream = b.stream.ExtractReceiptInfo(receipt)

  for await (const partial of stream) {
    // TypeScript knows partial.items may be undefined
    console.log(`Items: ${partial.items?.length ?? 0}`)
  }

  const final = await stream.getFinalResponse()
  return final
}
```

### 4.3 Streaming Attributes

Control streaming behavior with special attributes:

```baml
class Message {
  message_type string @stream.not_null  // Never null during streaming
  message string                         // Wrapped in StreamState
  metadata Metadata @stream.done         // Only included when complete
}
```

- **@stream.done** - Field only appears when fully parsed
- **@stream.not_null** - Field will not be null during streaming
- **@stream.with_state** - Provides streaming state information

### 4.4 Partial Types

BAML generates `partial_types` module where all fields are optional:

```python
from baml_client.partial_types import PartialReceipt

# During streaming
partial: PartialReceipt  # All fields are Optional[...]

# After streaming
final: Receipt  # Fields match original schema
```

---

## 5. Code Generation

BAML compiles `.baml` files into type-safe clients for multiple languages.

### 5.1 Generating Clients

```bash
# Generate client code
baml-cli generate

# Or use the VSCode extension (auto-generates on save)
```

This creates a `baml_client` directory with all generated code.

### 5.2 Python Usage

```python
from baml_client import b
from baml_client.types import Resume, Experience

# Synchronous call
resume = b.ExtractResume(resume_text="...")

# Async call
from baml_client.async_client import b as async_b

async def extract():
    resume = await async_b.ExtractResume(resume_text="...")
    return resume

# Type-safe access
print(resume.name)  # IDE autocomplete works
for exp in resume.experience:
    print(f"{exp.role} at {exp.company}")
```

### 5.3 TypeScript Usage

```typescript
import { b } from './baml_client'
import type { Resume } from './baml_client/types'

async function extractResume(text: string): Promise<Resume> {
  const resume = await b.ExtractResume({ resumeText: text })

  // Full type safety
  console.log(resume.name)
  resume.experience.forEach(exp => {
    console.log(`${exp.role} at ${exp.company}`)
  })

  return resume
}
```

### 5.4 Ruby Usage

```ruby
require 'baml_client'

resume = Baml.Client.ExtractResume(resume_text: text)

puts resume.name
resume.experience.each do |exp|
  puts "#{exp.role} at #{exp.company}"
end
```

### 5.5 Runtime Client Selection

Dynamically select clients at runtime:

```python
from baml_client import b

# Override client at runtime
result = b.MyFunction(
    input="...",
    baml_options={
        "client_registry": my_custom_registry
    }
)
```

### 5.6 Generated Code Structure

```
baml_client/
├── __init__.py
├── async_client.py      # Async version of all functions
├── sync_client.py       # Sync version of all functions
├── types.py             # All generated classes and enums
├── partial_types.py     # Partial versions for streaming
└── ...
```

---

## 6. IDE Support

### 6.1 VSCode Extension

Install from the VSCode Marketplace: `Boundary.baml-extension`

**Features:**
- **Syntax Highlighting** - Language-specific coloring for `.baml` files
- **Real-time Playground** - Test prompts without writing application code
- **Prompt Preview** - See the exact prompt that will be sent to the LLM
- **Raw cURL Visibility** - View the actual API request
- **Auto-generation** - Client code generates on file save

### 6.2 Installation

```bash
# Install runtime first
pip install baml-py          # Python
npm install @boundaryml/baml  # TypeScript/Node
bundle add baml sorbet-runtime # Ruby

# Initialize project
baml-cli init
```

### 6.3 Playground Features

1. **Interactive Testing** - Click play button next to any test
2. **Parallel Execution** - Run multiple tests simultaneously
3. **Environment Variables** - Configure via settings gear icon
4. **Multi-modal Preview** - View images, audio, and other media in prompts
5. **Response Inspection** - See raw LLM output and parsed result

### 6.4 Other Editor Support

- **JetBrains** - Extension on official marketplace with frequent updates
- **Zed** - Extension available
- **Neovim** - Coming soon
- **Any LSP-compatible editor** - BAML uses Language Server Protocol

### 6.5 Workflow

1. Write `.baml` types and functions
2. Test in IDE playground
3. Run `baml-cli generate` (or save file with extension)
4. Import and call generated functions in application code

---

## 7. Error Handling

### 7.1 Schema-Aligned Parsing (SAP)

BAML uses a novel Rust-based error-tolerant parser that can:
- Parse malformed JSON (missing closing brackets, trailing commas)
- Extract valid data even from partially correct outputs
- Coerce types automatically
- Trim junk and whitespace

### 7.2 BamlValidationError

When parsing fails or assertions are violated:

```python
from baml_client.errors import BamlValidationError

try:
    result = b.ExtractData(text=input_text)
except BamlValidationError as e:
    print(f"Validation failed: {e}")
    print(f"Check name: {e.check_name}")  # For named assertions
```

### 7.3 Using Checks and Asserts

**Field-level validation:**

```baml
class Citation {
  quote string @check(not_empty, {{ this|length > 0 }})
  source string @assert(valid_source, {{ this|length > 0 and this != "unknown" }})
  page int? @check(reasonable, {{ this == null or (this > 0 and this < 10000) }})
}
```

**Class-level validation:**

```baml
class DateRange {
  startDate string
  endDate string

  @@assert(end_after_start, {{ this.endDate >= this.startDate }})
}
```

### 7.4 Check Results at Runtime

```python
result = b.ExtractCitation(text=input_text)

# Access check results
if not result._checks.not_empty.passed:
    print("Warning: quote was empty")

# Checks are always available, even if passed
for check_name, check_result in result._checks.items():
    print(f"{check_name}: {'PASS' if check_result.passed else 'FAIL'}")
```

### 7.5 Assertion Behavior

- **Top-level assertion failure** - Raises `BamlValidationError`
- **Nested assertion failure** - Item is removed from container (array/map)
- **Multiple assertions** - Evaluated left to right, first failure stops

### 7.6 Error Prevention Best Practices

1. **Use optional fields** for data that may not always be present
2. **Add @check annotations** for soft validations you want to inspect
3. **Add @assert annotations** for hard requirements
4. **Test edge cases** in BAML tests before deployment
5. **Use descriptive check names** for easier debugging

### 7.7 Handling Streaming Errors

```python
try:
    stream = b.stream.ExtractData(text=input_text)
    for partial in stream:
        process(partial)
    final = stream.get_final_response()
except BamlValidationError as e:
    # Handle validation errors
    pass
except Exception as e:
    # Handle streaming/network errors
    pass
```

---

## Complete Example: Production-Ready Extraction Service

Here's a comprehensive example combining multiple patterns:

```baml
// clients.baml
retry_policy Exponential {
  max_retries 3
  strategy {
    type exponential_backoff
    delay_ms 200
    multiplier 2.0
    max_delay_ms 5000
  }
}

client<llm> GPT4o {
  provider openai
  retry_policy Exponential
  options {
    model "gpt-4o"
    temperature 0
    api_key env.OPENAI_API_KEY
  }
}

client<llm> ClaudeBackup {
  provider anthropic
  options {
    model "claude-3-5-sonnet-20241022"
    api_key env.ANTHROPIC_API_KEY
  }
}

client<llm> ResilientClient {
  provider fallback
  options {
    strategy [GPT4o, ClaudeBackup]
  }
}

// types.baml
class Invoice {
  vendor string @description("Company name of the vendor")
  invoiceNumber string @check(format, {{ this matches "INV-\\d+" }})
  date string @description("Date in YYYY-MM-DD format")
  lineItems LineItem[]
  subtotal float @check(positive, {{ this > 0 }})
  tax float
  total float @assert(matches_sum, {{
    (this.subtotal + this.tax - this) | abs < 0.01
  }})
}

class LineItem {
  description string @check(not_empty, {{ this|length > 0 }})
  quantity int @assert(positive, {{ this > 0 }})
  unitPrice float
  amount float @check(calculated, {{
    (this.quantity * this.unitPrice - this) | abs < 0.01
  }})
}

// functions.baml
function ExtractInvoice(invoiceText: string) -> Invoice {
  client ResilientClient
  prompt #"
    Extract structured invoice information from the following document.
    Be precise with numbers and maintain calculation accuracy.

    {{ ctx.output_format }}

    {{ _.role("user") }}
    {{ invoiceText }}
  "#
}

// tests.baml
test BasicInvoiceTest {
  functions [ExtractInvoice]
  args {
    invoiceText #"
      INVOICE
      From: Acme Corp
      Invoice #: INV-2024-001
      Date: 2024-01-15

      Items:
      - Widget A (x2) @ $50.00 = $100.00
      - Widget B (x1) @ $75.00 = $75.00

      Subtotal: $175.00
      Tax (10%): $17.50
      Total: $192.50
    "#
  }
  @@assert({{ this.vendor == "Acme Corp" }})
  @@assert({{ this.total == 192.50 }})
  @@check(correct_items, {{ this.lineItems|length == 2 }})
}
```

**Python Application:**

```python
from baml_client import b
from baml_client.errors import BamlValidationError

async def process_invoice(invoice_text: str):
    try:
        # Stream for real-time updates
        stream = b.stream.ExtractInvoice(invoice_text)

        async for partial in stream:
            if partial.lineItems:
                print(f"Found {len(partial.lineItems)} items so far...")

        invoice = await stream.get_final_response()

        # Check validation results
        for item in invoice.lineItems:
            if not item._checks.calculated.passed:
                print(f"Warning: Line item calculation may be off")

        return invoice

    except BamlValidationError as e:
        print(f"Invoice validation failed: {e}")
        raise
```

---

## Resources

- **Documentation**: https://docs.boundaryml.com
- **GitHub**: https://github.com/BoundaryML/baml
- **Examples Repository**: https://github.com/BoundaryML/baml-examples
- **VSCode Extension**: https://marketplace.visualstudio.com/items?itemName=Boundary.baml-extension
- **Blog**: https://boundaryml.com/blog

---

## Summary

BAML transforms LLM integration from fragile string manipulation into robust software engineering:

| Feature | Benefit |
|---------|---------|
| Schema-first design | Type safety across all languages |
| Built-in testing | Catch issues before deployment |
| Retry/fallback strategies | Production resilience |
| Type-safe streaming | Real-time UI updates |
| IDE playground | Fast iteration cycles |
| Error-tolerant parsing | Handles malformed LLM output |

By treating prompts as functions with schemas, tests, and retry logic, BAML enables teams to build reliable AI applications with confidence.
