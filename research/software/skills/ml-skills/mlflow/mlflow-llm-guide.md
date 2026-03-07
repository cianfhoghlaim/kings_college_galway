# MLflow LLM Features Reference Documentation

> Comprehensive guide to MLflow's LLM-specific features for tracking, evaluating, and managing Large Language Model applications.

## Table of Contents

1. [MLflow LLM Tracking](#1-mlflow-llm-tracking)
2. [MLflow Evaluate for LLMs](#2-mlflow-evaluate-for-llms)
3. [MLflow Tracing](#3-mlflow-tracing)
4. [Prompt Engineering](#4-prompt-engineering)
5. [RAG Support](#5-rag-retrieval-augmented-generation-support)
6. [Best Practices](#6-best-practices)

---

## 1. MLflow LLM Tracking

MLflow's LLM Tracking component provides APIs for logging inputs, outputs, and prompts from LLM interactions, along with a UI for viewing experimental results.

### Core Concepts

- **Runs**: Each run represents a distinct execution or interaction with the LLM
- **Predictions**: Encompasses prompts/inputs and outputs/responses, stored as CSV artifacts
- **Parameters**: Key-value input parameters (temperature, top_k, etc.)
- **Metrics**: Quantitative insights (accuracy, response time, etc.)
- **Artifacts**: Output files including models, visualizations, and data files

### mlflow.llm Module API

#### mlflow.llm.log_predictions()

Logs a batch of inputs, outputs, and prompts for the current evaluation run.

```python
mlflow.llm.log_predictions(
    inputs: List[Union[str, Dict[str, str]]],
    outputs: List[str],
    prompts: List[Union[str, Dict[str, str]]]
) -> None
```

**Parameters:**
- `inputs`: List of input strings or input dictionaries
- `outputs`: List of output strings
- `prompts`: List of prompt strings or prompt dictionaries

**Example:**

```python
import mlflow

inputs = [
    {
        "question": "How do I create a Databricks cluster with UC access?",
        "context": "Databricks clusters are...",
    },
]
outputs = [
    "<Instructions for cluster creation with UC enabled>",
]
prompts = [
    "Get Databricks documentation to answer all the questions: {input}",
]

with mlflow.start_run():
    # Log LLM predictions
    mlflow.llm.log_predictions(inputs, outputs, prompts)
```

### Logging Parameters and Metrics

```python
import mlflow

with mlflow.start_run():
    # Log LLM parameters
    mlflow.log_params({
        "model": "gpt-4",
        "temperature": 0.7,
        "top_k": 50,
        "max_tokens": 1000
    })

    # Log metrics
    mlflow.log_metric("response_time_ms", 245)
    mlflow.log_metric("token_count", 150)
    mlflow.log_metric("accuracy", 0.92)

    # Log multiple metrics at once
    mlflow.log_metrics({
        "precision": 0.89,
        "recall": 0.91,
        "f1_score": 0.90
    })
```

### Logging Artifacts

```python
import mlflow

with mlflow.start_run():
    # Log individual artifact
    mlflow.log_artifact("model_config.json")

    # Log table data (CSV format)
    mlflow.log_table(
        data={"prompt": prompts, "response": responses},
        artifact_file="predictions.csv"
    )
```

---

## 2. MLflow Evaluate for LLMs

MLflow provides comprehensive evaluation capabilities for LLMs using both traditional metrics and LLM-as-a-Judge approaches.

### Modern GenAI Evaluation API (MLflow 3.x)

#### mlflow.genai.evaluate()

The modern evaluation function for GenAI applications.

```python
import mlflow
from mlflow.genai.scorers import Correctness, Guidelines
from mlflow.genai import scorer
from openai import OpenAI

# Set up MLflow
mlflow.set_tracking_uri("http://localhost:5000")
mlflow.set_experiment("GenAI Evaluation")

# Create prediction function
client = OpenAI()
def qa_predict_fn(question: str) -> str:
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": "Answer questions concisely."},
            {"role": "user", "content": question},
        ],
    )
    return response.choices[0].message.content

# Define evaluation dataset
eval_dataset = [
    {
        "inputs": {"question": "What is the capital of France?"},
        "expectations": {"expected_response": "Paris"},
    },
    {
        "inputs": {"question": "Who wrote Romeo and Juliet?"},
        "expectations": {"expected_response": "William Shakespeare"},
    },
]

# Define custom scorer
@scorer
def is_concise(outputs: str) -> bool:
    """Check if answer is concise (less than 5 words)"""
    return len(outputs.split()) <= 5

# Run evaluation
results = mlflow.genai.evaluate(
    data=eval_dataset,
    predict_fn=qa_predict_fn,
    scorers=[
        Correctness(),
        Guidelines(name="is_english", guidelines="The answer must be in English"),
        is_concise,
    ],
)
```

### Predefined LLM Scorers

MLflow provides 8 built-in scorers for common evaluation needs:

| Scorer | Purpose | Required Data |
|--------|---------|---------------|
| `Correctness` | Validates responses against ground truth | expectations |
| `RelevanceToQuery` | Assesses if responses address user input | inputs |
| `Guidelines` | Evaluates adherence to custom criteria | guidelines |
| `ExpectationsGuidelines` | Checks if responses meet specific expectations | expectations |
| `Safety` | Detects harmful or toxic content | outputs |
| `Equivalence` | Compares responses to expected outputs | expectations |
| `RetrievalGroundedness` | Validates responses use retrieved info | trace |
| `RetrievalRelevance` | Ensures retrieved docs match requests | trace |

**Usage with Different Models:**

```python
from mlflow.genai.scorers import Correctness, RelevanceToQuery, Guidelines

# Use different LLM providers as judges
scorers = [
    Correctness(model="openai:/gpt-4o-mini"),
    Correctness(model="anthropic:/claude-sonnet-4-20250514"),
    Correctness(model="google:/gemini-2.0-flash"),
    RelevanceToQuery(),
    Guidelines(
        name="professional_tone",
        guidelines="Response must maintain a professional and respectful tone"
    ),
]
```

### Custom Code-Based Scorers

Create custom evaluation logic using the `@scorer` decorator:

```python
from mlflow.genai import scorer
from mlflow.entities import Feedback, AssessmentSource, SpanType

# Simple boolean scorer
@scorer
def exact_match(outputs: dict, expectations: dict) -> bool:
    return outputs == expectations["expected_response"]

# Numeric scorer
@scorer
def word_count_score(outputs: str) -> float:
    """Score based on response length"""
    count = len(outputs.split())
    return min(count / 100, 1.0)  # Normalize to 0-1

# Scorer with rich feedback
@scorer
def content_quality(outputs: str) -> Feedback:
    score = calculate_quality(outputs)
    return Feedback(
        value=score,
        rationale="Assessment based on clarity, accuracy, and completeness",
        source=AssessmentSource(source_type="CODE", source_id="v1.0"),
        metadata={"scorer_version": "1.0", "model": "custom"}
    )

# Trace-based scorer for agent evaluation
@scorer
def uses_correct_tools(trace, expectations: dict) -> Feedback:
    """Check if agent used the expected tools"""
    tool_spans = trace.search_spans(span_type=SpanType.TOOL)
    used_tools = {span.name for span in tool_spans}
    expected_tools = set(expectations.get("expected_tools", []))

    correct = expected_tools.issubset(used_tools)
    return Feedback(
        value=correct,
        rationale=f"Used tools: {used_tools}, Expected: {expected_tools}"
    )
```

**Scorer Parameter Options:**

```python
@scorer
def my_scorer(
    *,
    inputs: dict[str, Any],       # Input data/questions
    outputs: Any,                  # Generated responses
    expectations: dict[str, Any],  # Ground truth
    trace: Trace,                  # Full execution trace
) -> float | bool | str | Feedback | list[Feedback]:
    pass
```

### Custom LLM Judge Metrics

#### make_judge() - Modern API (MLflow >= 3.4.0)

```python
from mlflow.genai.judges import make_judge
from typing import Literal

# Create coherence judge
coherence_judge = make_judge(
    name="coherence",
    instructions="""
    Evaluate the coherence of the response.

    Question: {{ inputs }}
    Response: {{ outputs }}

    Rate the coherence as: coherent, somewhat coherent, or incoherent.
    Consider logical flow, consistency, and clarity.
    """,
    feedback_value_type=Literal["coherent", "somewhat coherent", "incoherent"],
    model="openai:/gpt-4o-mini",
)

# Use in evaluation
results = mlflow.genai.evaluate(
    data=test_data,
    predict_fn=my_model,
    scorers=[coherence_judge],
)
```

**Template Variables for make_judge:**
- `{{ inputs }}` - Input data/questions
- `{{ outputs }}` - Generated responses
- `{{ expectations }}` - Ground truth
- `{{ trace }}` - Full agent trace data

#### make_genai_metric() - Legacy API

```python
from mlflow.metrics.genai import make_genai_metric, EvaluationExample

# Create evaluation examples
professionalism_example = EvaluationExample(
    input="What is MLflow?",
    output="MLflow is an open-source platform for managing the ML lifecycle.",
    score=5,
    justification="Professional tone, clear and concise explanation."
)

# Create custom metric
professionalism_metric = make_genai_metric(
    name="professionalism",
    definition=(
        "Professionalism refers to the use of a formal, respectful, and "
        "appropriate style of communication."
    ),
    grading_prompt=(
        "Professionalism scoring criteria:\n"
        "- Score 1: Extremely casual, includes slang\n"
        "- Score 2: Somewhat casual\n"
        "- Score 3: Neutral tone\n"
        "- Score 4: Mostly professional\n"
        "- Score 5: Highly professional and polished"
    ),
    examples=[professionalism_example],
    model="openai:/gpt-4",
    parameters={"temperature": 0.0},
)

# Use in evaluation
results = mlflow.evaluate(
    model=my_model,
    data=eval_data,
    extra_metrics=[professionalism_metric],
)
```

### Evaluation Datasets

#### Creating Managed Datasets

```python
from mlflow.genai.datasets import create_dataset

# Create a new evaluation dataset
dataset = create_dataset(
    name="customer_support_qa_v1",
    experiment_id=["0"],
    tags={
        "version": "1.0",
        "purpose": "regression_testing",
        "model": "gpt-4",
        "team": "ml-platform",
    },
)
```

#### Adding Records to Datasets

```python
# From trace data with expectations
traces = mlflow.search_traces(
    experiment_ids=["0"],
    max_results=50,
    filter_string="attributes.name = 'chat_completion'",
    return_type="list",
)

# Add expectations to traces
for trace in traces[:20]:
    mlflow.log_expectation(
        trace_id=trace.info.trace_id,
        name="output_quality",
        value={"relevance": 0.95, "accuracy": 1.0},
    )
```

#### Using Datasets in Evaluation

```python
from mlflow.genai import evaluate
from mlflow.genai.scorers import Correctness, Guidelines

results = evaluate(
    data=dataset,
    predict_fn=my_model.predict,
    scorers=[
        Correctness(name="factual_accuracy"),
        Guidelines(
            name="support_quality",
            guidelines="Response must be helpful, accurate, and professional",
        ),
    ],
)
```

### Human Feedback and Assessments

#### Logging Expectations (Ground Truth)

```python
import mlflow
from mlflow.entities import AssessmentSource, AssessmentSourceType

# Log ground truth expectation
mlflow.log_expectation(
    trace_id="tr-1234567890abcdef",
    name="correct_answer",
    value={"expected": "Paris is the capital of France"},
    source=AssessmentSource(
        source_type=AssessmentSourceType.HUMAN,
        source_id="expert@example.com"
    ),
    metadata={"annotator_confidence": "high"}
)
```

#### Logging Feedback

```python
# Log human feedback
mlflow.log_feedback(
    trace_id=trace_id,
    name="helpfulness",
    value=4,  # 1-5 scale
    rationale="Response was helpful but could be more detailed",
    source=AssessmentSource(
        source_type=AssessmentSourceType.HUMAN,
        source_id="reviewer@example.com"
    )
)

# Log automated feedback
mlflow.log_feedback(
    trace_id=trace_id,
    name="syntax_valid",
    value=True,
    source=AssessmentSource(
        source_type=AssessmentSourceType.CODE,
        source_id="syntax_checker_v1"
    )
)

# Log LLM judge feedback
mlflow.log_feedback(
    trace_id=trace_id,
    name="coherence_score",
    value=0.85,
    rationale="Response maintains logical flow throughout",
    source=AssessmentSource(
        source_type=AssessmentSourceType.LLM_JUDGE,
        source_id="gpt-4-judge"
    )
)
```

---

## 3. MLflow Tracing

MLflow Tracing provides OpenTelemetry-compatible observability for LLM applications, capturing inputs, outputs, and metadata for debugging and monitoring.

### Auto-Tracing

Enable automatic tracing with a single line for supported frameworks:

```python
import mlflow

# OpenAI
mlflow.openai.autolog()

# LangChain
mlflow.langchain.autolog()

# LlamaIndex
mlflow.llama_index.autolog()

# Anthropic
mlflow.anthropic.autolog()

# And many more...
```

**Supported Frameworks (28+):**

| Category | Frameworks |
|----------|------------|
| Agent Platforms | LangChain, LangGraph, CrewAI, AutoGen, OpenAI Agents, PydanticAI, DSPy |
| LLM Providers | OpenAI, Anthropic, Google Gemini, AWS Bedrock, Groq, Mistral, Ollama |
| RAG Frameworks | LlamaIndex, Haystack, txtai |
| Other | Instructor, LiteLLM, Vercel AI SDK |

### Manual Tracing

#### Using the @mlflow.trace Decorator

```python
import mlflow
from mlflow.entities import SpanType

@mlflow.trace(span_type=SpanType.CHAIN)
def process_query(query: str) -> str:
    """Process a user query through multiple steps"""

    # Retrieve context
    context = retrieve_documents(query)

    # Generate response
    response = generate_response(query, context)

    return response

@mlflow.trace(span_type=SpanType.RETRIEVER)
def retrieve_documents(query: str) -> list:
    """Retrieve relevant documents"""
    return vector_db.search(query, k=5)

@mlflow.trace(span_type=SpanType.LLM)
def generate_response(query: str, context: list) -> str:
    """Generate response using LLM"""
    return llm.generate(query=query, context=context)
```

**Decorator Parameters:**

```python
@mlflow.trace(
    name="custom_name",           # Override default span name
    span_type=SpanType.LLM,       # Set span type
    attributes={"version": "1.0"} # Add custom metadata
)
def my_function():
    pass
```

#### Using Context Manager

```python
import mlflow

def complex_workflow(query: str):
    with mlflow.start_span(name="workflow") as parent_span:
        parent_span.set_inputs({"query": query})

        # First step
        with mlflow.start_span(name="retrieval") as retrieval_span:
            docs = retrieve(query)
            retrieval_span.set_outputs({"doc_count": len(docs)})

        # Second step
        with mlflow.start_span(name="generation") as gen_span:
            gen_span.set_inputs({"query": query, "context": docs})
            response = generate(query, docs)
            gen_span.set_outputs({"response": response})

        parent_span.set_outputs({"response": response})
        return response
```

#### Combining Auto-Tracing with Manual Spans

```python
import mlflow
from mlflow.entities import SpanType

# Enable auto-tracing for OpenAI
mlflow.openai.autolog()

@mlflow.trace(span_type=SpanType.CHAIN)
def rag_pipeline(question: str) -> str:
    """RAG pipeline with auto-traced LLM calls"""

    # This retrieval is manually traced
    with mlflow.start_span(name="custom_retrieval") as span:
        docs = custom_retriever.search(question)
        span.set_outputs({"retrieved_docs": len(docs)})

    # OpenAI call is automatically traced
    response = openai_client.chat.completions.create(
        model="gpt-4",
        messages=[
            {"role": "system", "content": f"Context: {docs}"},
            {"role": "user", "content": question}
        ]
    )

    return response.choices[0].message.content
```

### Span Types

MLflow provides predefined span types for categorizing different operations:

| SpanType | Description |
|----------|-------------|
| `AGENT` | Agent orchestration |
| `CHAIN` | Sequential processing chain |
| `CHAT_MODEL` | Chat completion API |
| `EMBEDDING` | Embedding generation |
| `LLM` | LLM inference |
| `MEMORY` | Memory operations |
| `PARSER` | Output parsing |
| `RERANKER` | Result reranking |
| `RETRIEVER` | Document retrieval |
| `TOOL` | Tool/function calls |

```python
from mlflow.entities import SpanType

# Custom span type (string)
@mlflow.trace(span_type="ROUTER")
def route_query(query):
    pass
```

### Trace Utilities

```python
import mlflow

# Get current active span
span = mlflow.get_current_active_span()
if span:
    span.set_attribute("custom_metric", 0.95)

# Update current trace with metadata
mlflow.update_current_trace(
    tags={"environment": "production"},
    request_preview="User query about...",
    response_preview="Response summary..."
)

# Search traces
traces = mlflow.search_traces(
    experiment_ids=["0"],
    filter_string="attributes.name = 'rag_pipeline'",
    max_results=100
)
```

### Production Tracing Configuration

```python
import mlflow

# Enable async logging for production
mlflow.config.enable_async_logging(True)

# Use lightweight tracing SDK
# pip install mlflow-tracing  # Minimal dependencies

# Disable tracing when needed
mlflow.tracing.disable()

# Re-enable tracing
mlflow.tracing.enable()
```

---

## 4. Prompt Engineering

MLflow Prompt Registry provides centralized management for prompt templates with versioning, aliasing, and collaboration features.

### Core Concepts

- **Prompt**: Versioned template with variables in `{{variable}}` format
- **Version**: Immutable, sequential revision numbers
- **Alias**: Mutable references (e.g., "production", "staging") for deployment
- **Tags**: Metadata for organization and filtering

### Creating and Registering Prompts

```python
import mlflow

# Register a new prompt
initial_template = """\
Summarize the following content in {{ num_sentences }} sentences.

Content: {{ content }}

Summary:"""

prompt = mlflow.genai.register_prompt(
    name="summarization-prompt",
    template=initial_template,
    commit_message="Initial version",
    tags={
        "author": "author@example.com",
        "task": "summarization",
        "language": "en",
    },
)

print(f"Created prompt: {prompt.name}, version: {prompt.version}")
```

### Creating New Versions

```python
# Update the prompt (creates new version)
updated_template = """\
Summarize the following content in {{ num_sentences }} sentences.
Focus on key points and maintain clarity.

Content: {{ content }}

Summary:"""

updated_prompt = mlflow.genai.register_prompt(
    name="summarization-prompt",
    template=updated_template,
    commit_message="Added focus instruction for better quality",
    tags={"author": "author@example.com"},
)

print(f"New version: {updated_prompt.version}")
```

### Loading Prompts

```python
import mlflow

# Load latest version
prompt = mlflow.genai.load_prompt("summarization-prompt")

# Load specific version
prompt_v1 = mlflow.genai.load_prompt("prompts:/summarization-prompt/1")

# Load by alias
prod_prompt = mlflow.genai.load_prompt("prompts:/summarization-prompt@production")

# Use the @latest reserved alias
latest = mlflow.genai.load_prompt("prompts:/summarization-prompt@latest")
```

### Managing Aliases

```python
import mlflow

# Set alias for deployment
mlflow.set_prompt_alias(
    name="summarization-prompt",
    alias="production",
    version=2
)

mlflow.set_prompt_alias(
    name="summarization-prompt",
    alias="staging",
    version=3
)

# Load by alias in application code
prompt = mlflow.genai.load_prompt("prompts:/summarization-prompt@production")
```

### Using Prompts in Applications

```python
import mlflow
from openai import OpenAI

# Load prompt
prompt = mlflow.genai.load_prompt("prompts:/summarization-prompt@production")

# Format with variables
formatted = prompt.format(
    num_sentences=3,
    content="MLflow is an open-source platform..."
)

# Use with LLM
client = OpenAI()
response = client.chat.completions.create(
    model="gpt-4",
    messages=[{"role": "user", "content": formatted}]
)
```

### Integration with LangChain

```python
import mlflow
from langchain_core.prompts import PromptTemplate
from langchain_openai import ChatOpenAI

# Load from MLflow
mlflow_prompt = mlflow.genai.load_prompt("summarization-prompt")

# Convert to LangChain format
langchain_template = PromptTemplate.from_template(mlflow_prompt.template)

# Use in chain
chain = langchain_template | ChatOpenAI(model="gpt-4")
result = chain.invoke({
    "num_sentences": 3,
    "content": "Content to summarize..."
})
```

### Best Practices for Prompt Management

1. **Descriptive Naming**: Use clear names reflecting purpose (e.g., `customer-support-qa`, `code-review-feedback`)
2. **Meaningful Commits**: Document changes in commit messages
3. **Logical Tagging**: Use tags for categorization (task type, language, model)
4. **Alias Strategy**: Use environment-based aliases (production, staging, development)
5. **Version Immutability**: Never try to modify existing versions; create new ones

---

## 5. RAG (Retrieval-Augmented Generation) Support

MLflow provides specialized evaluation capabilities for RAG systems, including retrieval metrics and context relevance scoring.

### RAG Evaluation Metrics

#### Built-in Retriever Metrics

```python
import mlflow

# Evaluate retriever performance
results = mlflow.evaluate(
    model=retriever_model,
    data=eval_data,
    targets="ground_truth_docs",
    model_type="retriever",
    evaluators="default",
    evaluator_config={
        "k": [1, 3, 5, 10],  # Evaluate at different k values
    }
)

# Metrics include:
# - precision_at_k
# - recall_at_k
# - ndcg_at_k
```

#### RAG-Specific Scorers

```python
from mlflow.genai.scorers import RetrievalGroundedness, RetrievalRelevance

results = mlflow.genai.evaluate(
    data=rag_dataset,
    predict_fn=rag_pipeline,
    scorers=[
        RetrievalGroundedness(),  # Is response grounded in retrieved docs?
        RetrievalRelevance(),     # Are retrieved docs relevant to query?
    ],
)
```

### Evaluating Complete RAG Systems

```python
import mlflow
from mlflow.genai.scorers import Correctness, RetrievalGroundedness
from mlflow.genai import scorer

# Custom RAG scorer using traces
@scorer
def context_utilization(trace, outputs: str) -> float:
    """Measure how much of the retrieved context was used"""
    from mlflow.entities import SpanType

    # Extract retriever spans
    retriever_spans = trace.search_spans(span_type=SpanType.RETRIEVER)
    if not retriever_spans:
        return 0.0

    # Get retrieved documents
    retrieved_docs = retriever_spans[0].outputs.get("documents", [])

    # Calculate utilization (simplified)
    utilized = sum(1 for doc in retrieved_docs if doc["content"] in outputs)
    return utilized / len(retrieved_docs) if retrieved_docs else 0.0

# RAG evaluation dataset
rag_dataset = [
    {
        "inputs": {"question": "What are MLflow's key features?"},
        "expectations": {
            "expected_response": "MLflow provides tracking, projects, models, and registry",
            "expected_sources": ["mlflow-docs-overview"]
        },
    },
]

# Run evaluation
results = mlflow.genai.evaluate(
    data=rag_dataset,
    predict_fn=rag_pipeline,
    scorers=[
        Correctness(),
        RetrievalGroundedness(),
        context_utilization,
    ],
)
```

### Relevance Evaluation

```python
from mlflow.metrics.genai import relevance

# Create relevance metric for RAG
relevance_metric = relevance(
    model="openai:/gpt-4",
    parameters={"temperature": 0.0}
)

# Configure evaluator
evaluator_config = {
    "col_mapping": {
        "inputs": "question",
        "context": "retrieved_context",
    }
}

results = mlflow.evaluate(
    model=rag_model,
    data=eval_data,
    extra_metrics=[relevance_metric],
    evaluator_config=evaluator_config,
)
```

### Chunk Size Optimization

```python
import mlflow

def evaluate_chunk_strategy(chunk_size: int):
    """Evaluate RAG performance with different chunk sizes"""

    with mlflow.start_run(run_name=f"chunk_size_{chunk_size}"):
        # Log chunk configuration
        mlflow.log_param("chunk_size", chunk_size)

        # Create retriever with chunk size
        retriever = create_retriever(chunk_size=chunk_size)
        rag_pipeline = create_rag_pipeline(retriever)

        # Evaluate
        results = mlflow.genai.evaluate(
            data=eval_dataset,
            predict_fn=rag_pipeline,
            scorers=[Correctness(), RetrievalGroundedness()],
        )

        # Log aggregate metrics
        mlflow.log_metrics({
            "mean_correctness": results.metrics["mean_correctness"],
            "mean_groundedness": results.metrics["mean_groundedness"],
        })

# Test different chunk sizes
for size in [500, 1000, 2000]:
    evaluate_chunk_strategy(size)
```

### Question Generation for Retrieval Evaluation

```python
import mlflow
from openai import OpenAI

def generate_evaluation_questions(documents: list) -> list:
    """Generate test questions from documents for retrieval evaluation"""

    client = OpenAI()
    questions = []

    for doc in documents:
        response = client.chat.completions.create(
            model="gpt-4",
            messages=[
                {
                    "role": "system",
                    "content": "Generate 3 questions that can be answered using this document."
                },
                {"role": "user", "content": doc["content"]}
            ]
        )

        doc_questions = response.choices[0].message.content.split("\n")
        for q in doc_questions:
            questions.append({
                "question": q,
                "source_doc_id": doc["id"],
                "ground_truth_chunks": [doc["id"]]
            })

    return questions
```

---

## 6. Best Practices

### LLM Experiment Tracking

1. **Structured Logging**
   ```python
   with mlflow.start_run():
       # Log all relevant parameters
       mlflow.log_params({
           "model": "gpt-4",
           "temperature": 0.7,
           "prompt_version": "v2.1",
           "retriever_k": 5,
       })

       # Log predictions for reproducibility
       mlflow.llm.log_predictions(inputs, outputs, prompts)
   ```

2. **Consistent Naming Conventions**
   - Use descriptive experiment names
   - Tag runs with environment, model version, purpose
   - Version prompts with semantic versioning

3. **Artifact Management**
   - Store prompt templates as artifacts
   - Log evaluation datasets
   - Archive model configurations

### Evaluation Workflows

1. **Evaluation-Driven Development**
   ```python
   # Define evaluation suite early
   eval_suite = [
       Correctness(),
       Guidelines(name="tone", guidelines="Professional tone"),
       custom_domain_scorer,
   ]

   # Run evaluation on every change
   results = mlflow.genai.evaluate(
       data=golden_dataset,
       predict_fn=model_v2,
       scorers=eval_suite,
   )

   # Compare with baseline
   if results.metrics["mean_correctness"] < baseline:
       raise ValueError("Regression detected")
   ```

2. **Combine Multiple Evaluation Types**
   - Use built-in scorers for common metrics
   - Create custom scorers for domain-specific needs
   - Leverage traces for deep inspection

3. **Maintain Golden Datasets**
   - Curate high-quality evaluation data
   - Include edge cases and failure modes
   - Update regularly with production examples

### Tracing Best Practices

1. **Use Appropriate Span Types**
   ```python
   @mlflow.trace(span_type=SpanType.RETRIEVER)
   def retrieve():
       pass

   @mlflow.trace(span_type=SpanType.LLM)
   def generate():
       pass
   ```

2. **Add Meaningful Attributes**
   ```python
   with mlflow.start_span(name="processing") as span:
       span.set_attribute("doc_count", len(docs))
       span.set_attribute("model_version", "v2.1")
   ```

3. **Production Optimization**
   - Enable async logging
   - Use lightweight tracing SDK
   - Implement sampling for high-volume apps

### Prompt Engineering

1. **Version Control Strategy**
   ```python
   # Use aliases for deployment stages
   mlflow.set_prompt_alias("qa-prompt", "development", version=5)
   mlflow.set_prompt_alias("qa-prompt", "staging", version=4)
   mlflow.set_prompt_alias("qa-prompt", "production", version=3)
   ```

2. **A/B Testing Prompts**
   ```python
   import random

   # Load different versions for testing
   version = "a" if random.random() < 0.5 else "b"
   prompt = mlflow.genai.load_prompt(f"prompts:/qa-prompt@test_{version}")

   # Log which version was used
   mlflow.log_param("prompt_version", version)
   ```

3. **Collaborative Development**
   - Use meaningful commit messages
   - Tag prompts with author and purpose
   - Review prompts before promoting to production

### RAG System Optimization

1. **Systematic Evaluation**
   - Evaluate retrieval and generation separately
   - Use precision@k and recall@k for retrieval
   - Use faithfulness for generation quality

2. **Iterate on Components**
   - Test different chunking strategies
   - Compare embedding models
   - Optimize retrieval parameters

3. **Monitor in Production**
   - Track context utilization
   - Monitor response groundedness
   - Alert on quality degradation

---

## API Reference Summary

### Core Modules

| Module | Purpose |
|--------|---------|
| `mlflow.llm` | LLM tracking utilities |
| `mlflow.genai` | Modern GenAI evaluation and management |
| `mlflow.genai.scorers` | Predefined evaluation scorers |
| `mlflow.genai.datasets` | Evaluation dataset management |
| `mlflow.genai.judges` | Custom LLM judge creation |
| `mlflow.metrics.genai` | GenAI metrics (legacy) |

### Key Functions

| Function | Purpose |
|----------|---------|
| `mlflow.llm.log_predictions()` | Log LLM inputs/outputs |
| `mlflow.genai.evaluate()` | Run GenAI evaluation |
| `mlflow.genai.register_prompt()` | Create/version prompts |
| `mlflow.genai.load_prompt()` | Load prompt by name/alias |
| `mlflow.trace()` | Decorator for manual tracing |
| `mlflow.start_span()` | Context manager for spans |
| `mlflow.log_expectation()` | Log ground truth |
| `mlflow.log_feedback()` | Log evaluation feedback |

### Version Requirements

- MLflow Tracing: >= 2.14.0
- Modern GenAI API: >= 3.0.0
- `make_judge` API: >= 3.4.0
- Evaluation Datasets: >= 3.2.0

---

## Resources

- [MLflow LLM Documentation](https://mlflow.org/docs/latest/llms/index.html)
- [GenAI Evaluation Guide](https://mlflow.org/docs/latest/genai/eval-monitor/)
- [Prompt Registry](https://mlflow.org/docs/latest/genai/prompt-registry/)
- [Tracing Documentation](https://mlflow.org/docs/latest/genai/tracing/)
- [RAG Tutorials](https://mlflow.org/docs/latest/llms/rag/index.html)
- [Python API Reference](https://mlflow.org/docs/latest/python_api/index.html)


---

# LLM Integrations

# MLflow LLM Framework Integrations

Comprehensive reference documentation for MLflow's integrations with LLM frameworks and tools. This covers tracing, autologging, model management, and deployment capabilities.

## Table of Contents

1. [Overview](#overview)
2. [LangChain Integration](#langchain-integration)
3. [OpenAI Integration](#openai-integration)
4. [Transformers/Hugging Face Integration](#transformershugging-face-integration)
5. [LlamaIndex Integration](#llamaindex-integration)
6. [Other LLM Integrations](#other-llm-integrations)
   - [Anthropic Claude](#anthropic-claude)
   - [Google Gemini](#google-gemini)
   - [AWS Bedrock](#aws-bedrock)
   - [Ollama (Local Models)](#ollama-local-models)
   - [Mistral AI](#mistral-ai)
   - [DSPy](#dspy)
7. [Multi-Agent Framework Integrations](#multi-agent-framework-integrations)
   - [AutoGen](#autogen)
   - [CrewAI](#crewai)
8. [MLflow AI Gateway / Deployments](#mlflow-ai-gateway--deployments)
9. [Vector Store & RAG Tracking](#vector-store--rag-tracking)
10. [Manual Tracing & Custom Instrumentation](#manual-tracing--custom-instrumentation)
11. [Common Patterns & Best Practices](#common-patterns--best-practices)

---

## Overview

MLflow provides comprehensive observability and tracking capabilities for LLM applications through:

- **Automatic Tracing**: One-line enablement via `mlflow.<library>.autolog()`
- **Token Usage Tracking**: Automatic capture of input/output tokens
- **Model Logging**: Persist and version LLM models and pipelines
- **Evaluation**: Built-in metrics for LLM quality assessment

### Version Requirements

Most tracing features require **MLflow 2.14.0+**, with enhanced token tracking available in **MLflow 3.1.0+**.

### General Autolog Pattern

```python
import mlflow

# Enable tracing for a specific library
mlflow.<library>.autolog()

# Set experiment for organization
mlflow.set_experiment("My LLM Experiment")

# Optionally set tracking URI
mlflow.set_tracking_uri("http://localhost:5000")
```

### Disabling Autolog

```python
# Disable for specific library
mlflow.<library>.autolog(disable=True)

# Disable all autologging
mlflow.autolog(disable=True)
```

---

## LangChain Integration

### Setup

```python
import mlflow

mlflow.langchain.autolog()
```

**Compatibility**: LangChain versions 0.3.7 through 0.3.27

### Autolog Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `disable` | bool | False | Toggle autologging on/off |
| `exclusive` | bool | False | Prevent logging to user-created fluent runs |
| `disable_for_unsupported_versions` | bool | False | Disable for untested LangChain versions |
| `silent` | bool | False | Suppress MLflow event logs and warnings |
| `log_traces` | bool | True | Capture execution traces via MlflowLangchainTracer |

### What Gets Traced

- `invoke`, `batch`, `stream`
- `ainvoke`, `abatch`, `astream`
- `get_relevant_documents` (retrievers)
- `__call__` (Chains and AgentExecutors)

### Token Usage Tracking

Available in MLflow 3.1.0+:

```python
# Individual LLM calls: mlflow.chat.tokenUsage span attribute
# Total usage: mlflow.trace.tokenUsage metadata field

trace = mlflow.get_trace(trace_id=last_trace_id)
total_usage = trace.info.token_usage
```

### LangGraph Support

MLflow automatically captures LangGraph graph executions when LangChain autolog is enabled:

```python
import mlflow
from langgraph.graph import StateGraph

mlflow.langchain.autolog()  # Also traces LangGraph

# Build and run your graph
graph = StateGraph(...)
app = graph.compile()
result = app.invoke({"input": "..."})
```

### Model Logging

```python
from langchain.chains import LLMChain
from langchain.llms import OpenAI

# Create chain
llm = OpenAI(temperature=0.9)
chain = LLMChain(llm=llm, prompt=prompt)

# Log to MLflow
mlflow.langchain.log_model(
    chain,
    "my_chain",
    input_example={"product": "colorful socks"},
    registered_model_name="my-langchain-model"
)

# Load model
loaded_chain = mlflow.langchain.load_model("runs:/<run_id>/my_chain")
```

### Custom Tracing with MlflowLangchainTracer

```python
from mlflow.langchain.langchain_tracer import MlflowLangchainTracer

class CustomTracer(MlflowLangchainTracer):
    def on_chat_model_start(self, serialized, messages, **kwargs):
        # Custom logic
        super().on_chat_model_start(serialized, messages, **kwargs)
```

---

## OpenAI Integration

### Setup

**Python:**
```python
import mlflow
from openai import OpenAI

mlflow.openai.autolog()

client = OpenAI()
response = client.chat.completions.create(
    model="gpt-4o-mini",
    messages=[{"role": "user", "content": "Hello!"}]
)
```

**TypeScript:**
```typescript
import { tracedOpenAI } from "mlflow-openai";
import OpenAI from "openai";

const client = tracedOpenAI(new OpenAI());
```

**Compatibility**: OpenAI SDK version 1.17+

### What Gets Captured

- Prompts and completion responses
- Latency measurements
- Model names and metadata (temperature, max_tokens)
- Function calling responses
- Built-in tools (web search, file search, computer use)
- Exceptions raised during execution

### Supported APIs

| API | Features Supported |
|-----|-------------------|
| Chat Completions | Normal, function calling, structured outputs, streaming (2.15.0+), async (2.21.0+) |
| Responses API | Web search, file search, computer use, reasoning (2.22.0+) |
| Embeddings | Normal and async |
| Agents SDK | Full support |

### Token Usage Tracking

Available in MLflow 3.1.0+:

```python
import mlflow
from openai import OpenAI

mlflow.openai.autolog()
client = OpenAI()

# For streaming, enable usage tracking
response = client.chat.completions.create(
    model="gpt-4o-mini",
    messages=[{"role": "user", "content": "Hello!"}],
    stream=True,
    stream_options={"include_usage": True}  # Required for streaming
)

# Access token usage
trace = mlflow.get_trace(trace_id=mlflow.get_last_active_trace_id())
print(f"Total tokens: {trace.info.token_usage}")
```

### Tool/Function Calling

```python
from mlflow.entities import SpanType

@mlflow.trace(span_type=SpanType.TOOL)
def get_weather(location: str):
    """Get weather for a location."""
    return {"temperature": 72, "condition": "sunny"}

# Tool calls are automatically captured in traces
```

---

## Transformers/Hugging Face Integration

### Setup

```python
import mlflow
from transformers import pipeline

mlflow.transformers.autolog()
```

**Compatibility**: transformers versions 4.38.2 through 4.57.1

### Autolog Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `log_input_examples` | bool | False | Log input examples |
| `log_model_signatures` | bool | True | Log model signatures |
| `log_models` | bool | True | Log trained models |
| `log_datasets` | bool | True | Log dataset information |
| `disable` | bool | False | Disable autologging |

### Pipeline Logging

```python
from transformers import pipeline
import mlflow

# Create pipeline
text_generator = pipeline(
    "text-generation",
    model="gpt2",
    max_new_tokens=50
)

# Log to MLflow
with mlflow.start_run():
    mlflow.transformers.log_model(
        transformers_model=text_generator,
        artifact_path="text_generator",
        input_example=["Hello, I'm a language model"],
        signature=mlflow.models.infer_signature(
            ["Hello, I'm a language model"],
            ["Hello, I'm a language model that..."]
        )
    )
```

### Supported Pipeline Types

- Text generation and classification
- Summarization and translation
- Question answering and token classification
- Audio processing (ASR, audio classification)
- Feature extraction

**Note**: Computer vision and multi-modal models require native loading via `mlflow.transformers.load_model()`.

### Model Cards & Metadata

MLflow automatically:
- Fetches ModelCard from HuggingFace Hub
- Preserves license information
- Stores metadata about model types and components
- Infers signatures for supported pipeline types

### Storage-Efficient Logging

For unmodified pretrained models, use reference-only mode:

```python
mlflow.transformers.log_model(
    transformers_model=pipe,
    artifact_path="model",
    save_pretrained=False  # Only save reference, not weights
)
```

### PEFT/LoRA Support

MLflow 2.11.0+ supports Parameter-Efficient Fine-Tuning:

```python
from peft import LoraConfig, get_peft_model

# Only adapter weights are saved
mlflow.transformers.log_model(
    transformers_model=peft_model,
    artifact_path="lora_model"
)
```

### MLflowCallback for Training

```python
from transformers import Trainer, TrainingArguments
from mlflow.transformers import MLflowCallback

training_args = TrainingArguments(
    output_dir="./results",
    report_to=["mlflow"],  # Enable MLflow reporting
    # ... other args
)

trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=train_dataset,
    callbacks=[MLflowCallback()]
)

trainer.train()
```

---

## LlamaIndex Integration

### Setup

```python
import mlflow

mlflow.llama_index.autolog()
mlflow.set_experiment("LlamaIndex")
```

### What Gets Traced

- Query engine invocations
- Chat engine interactions
- Retriever operations
- Workflow executions
- LLM calls within the pipeline

### Token Usage Tracking

Available in MLflow 3.2.0+:

```python
trace = mlflow.get_trace(trace_id=last_trace_id)
total_usage = trace.info.token_usage  # Aggregate across all LLM calls
```

### Supported Engine Types

| Engine Type | Load Method |
|-------------|-------------|
| Query Engine | `engine_type="query"` |
| Chat Engine | `engine_type="chat"` |
| Retriever | `engine_type="retriever"` |

### Model Logging

```python
from llama_index.core import VectorStoreIndex

# Create index and engine
index = VectorStoreIndex.from_documents(documents)
query_engine = index.as_query_engine()

# Log to MLflow
with mlflow.start_run():
    mlflow.llama_index.log_model(
        llama_index_model=index,
        artifact_path="index",
        engine_type="query",
        input_example="What is the capital of France?"
    )
```

### Settings Tracking

MLflow tracks the state of the LlamaIndex Settings object for reproducibility:

```python
from llama_index.core import Settings
from llama_index.llms.openai import OpenAI

Settings.llm = OpenAI(model="gpt-4o-mini", temperature=0.1)
Settings.chunk_size = 512

# These settings are captured when logging models
```

---

## Other LLM Integrations

### Anthropic Claude

```python
import anthropic
import mlflow

mlflow.anthropic.autolog()

client = anthropic.Anthropic()
message = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    messages=[{"role": "user", "content": "Hello, Claude"}]
)
```

**Features:**
- Chat completions
- Function/tool calling (automatically captured in trace UI)
- Async operations (MLflow 2.21.0+)
- Token usage tracking (MLflow 3.2.0+)

**Limitations:** Streaming, images, and batch processing not yet supported.

### Google Gemini

```python
import google.generativeai as genai
import mlflow
import os

mlflow.gemini.autolog()

client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])
response = client.models.generate_content(
    model="gemini-1.5-flash",
    contents="The opposite of hot is"
)
```

**Features:**
- Text generation and chat
- Function calling
- Embeddings
- Async operations (MLflow 3.2.0+)
- Token usage tracking (MLflow 3.4.0+)

**SDK Support:** Both Google GenAI SDK and legacy Google AI Python SDK (migration recommended).

### AWS Bedrock

```python
import boto3
import mlflow

mlflow.bedrock.autolog()
mlflow.set_experiment("Bedrock")

bedrock = boto3.client(
    service_name="bedrock-runtime",
    region_name="us-east-1"
)

response = bedrock.converse(
    modelId="anthropic.claude-3-5-sonnet-20241022-v2:0",
    messages=[{
        "role": "user",
        "content": "Describe 'hello world' in one line."
    }],
    inferenceConfig={
        "maxTokens": 512,
        "temperature": 0.1,
        "topP": 0.9
    }
)
```

**Supported APIs:**
- `converse` - Standard conversation
- `converse_stream` - Streaming conversation
- `invoke_model` - Direct model invocation
- `invoke_model_with_response_stream` - Streaming invocation

**Token Usage Tracking:** Automatic for Claude, Jamba, Titan/Nova, and Llama models.

**Streaming Note:** Spans are created when chunks are consumed, not when response is returned.

### Ollama (Local Models)

Ollama uses OpenAI-compatible endpoints, so use OpenAI autolog:

```python
import mlflow
from openai import OpenAI

mlflow.openai.autolog()
mlflow.set_experiment("Ollama")

# Point to Ollama's local endpoint
client = OpenAI(
    base_url="http://localhost:11434/v1",
    api_key="dummy"  # Ollama doesn't require API key
)

response = client.chat.completions.create(
    model="llama3.2:1b",
    messages=[{"role": "user", "content": "Hello!"}]
)
```

**Token Usage:** Supported in MLflow 3.2.0+ for local endpoints.

**Note:** Native Ollama Python SDK requires custom tracing implementation.

### Mistral AI

```python
import mlflow
from mistralai.client import MistralClient

mlflow.mistral.autolog()

client = MistralClient()
response = client.chat(
    model="mistral-small-latest",
    messages=[{"role": "user", "content": "Hello!"}]
)
```

**Limitations:** Only synchronous Text Generation API. Async and streaming not traced.

### DSPy

```python
import dspy
import mlflow

mlflow.dspy.autolog()
mlflow.set_experiment("DSPy")

lm = dspy.LM("openai/gpt-4o-mini")
dspy.configure(lm=lm)

# Define and run module
class QA(dspy.Signature):
    question: str = dspy.InputField()
    answer: str = dspy.OutputField()

qa = dspy.Predict(QA)
result = qa(question="What is 2+2?")
```

**Autolog Parameters:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| `log_traces` | True | Log traces for module invocations |
| `log_traces_from_eval` | True | Log during evaluation |
| `log_traces_from_compile` | False | Log during optimization (high volume) |
| `log_compiles` | False | Log optimization process info |

**Token Usage:** Supported in MLflow 3.5.0+

---

## Multi-Agent Framework Integrations

### AutoGen

```python
import mlflow
from autogen_agentchat.agents import AssistantAgent
from autogen_ext.models.openai import OpenAIChatCompletionClient

# Import classes BEFORE calling autolog
mlflow.autogen.autolog()
mlflow.set_experiment("AutoGen")

model_client = OpenAIChatCompletionClient(model="gpt-4.1-nano")

@mlflow.trace(span_type=SpanType.TOOL)
def add(a: int, b: int) -> int:
    return a + b

agent = AssistantAgent(
    name="assistant",
    model_client=model_client,
    system_message="You are a helpful assistant.",
    tools=[add]
)

await agent.run(task="What is 1+1?")
```

**What Gets Captured:**
- Which agent is called at different turns
- Messages passed between agents
- LLM and tool calls (organized per agent and turn)
- Latencies and exceptions

**Version Requirements:** AutoGen 0.4.9+ (use AG2 integration for AutoGen 0.2)

**Token Usage:** Available in MLflow 3.2.0+

**Limitations:** Async streaming APIs (`run_stream`, `on_messages_stream`) not traced.

### CrewAI

```python
import mlflow
from crewai import Agent, Task, Crew

mlflow.crewai.autolog()
mlflow.set_experiment("CrewAI")

researcher = Agent(
    role='Researcher',
    goal='Research and analyze data',
    backstory='Expert data researcher'
)

task = Task(
    description='Research AI trends',
    agent=researcher
)

crew = Crew(
    agents=[researcher],
    tasks=[task]
)

result = crew.kickoff()
```

**What Gets Captured:**
- Tasks and executing agents
- LLM calls with prompts, responses, and metadata
- Memory load/write operations
- Latencies and exceptions

**Features:**
- OpenTelemetry compatibility (export to Jaeger, Zipkin, AWS X-Ray)
- Model packaging and deployment
- Built-in evaluation via `mlflow.evaluate()`

**Limitations:** Only synchronous task execution. Async kickoff not supported.

---

## MLflow AI Gateway / Deployments

> **Note:** MLflow AI Gateway has been deprecated. Use MLflow Deployments for LLMs instead.

### Overview

The MLflow Deployments Server provides:
- Unified interface for multiple LLM providers
- Centralized API key management
- Rate limiting and access control
- Hot-swapping endpoints without downtime

### Configuration File (YAML)

```yaml
endpoints:
  - name: chat
    endpoint_type: llm/v1/chat
    model:
      provider: openai
      name: gpt-4
      config:
        openai_api_key: $OPENAI_API_KEY
    limit:
      renewal_period: minute
      calls: 10

  - name: embeddings
    endpoint_type: llm/v1/embeddings
    model:
      provider: openai
      name: text-embedding-ada-002
      config:
        openai_api_key: $OPENAI_API_KEY

  - name: claude
    endpoint_type: llm/v1/chat
    model:
      provider: anthropic
      name: claude-sonnet-4-20250514
      config:
        anthropic_api_key: $ANTHROPIC_API_KEY
```

### Supported Providers

- OpenAI (including Azure OpenAI with AAD)
- Anthropic
- Cohere
- MosaicML
- Hugging Face Text Generation Inference
- AI21 Labs
- Amazon Bedrock
- Mistral

### Rate Limiting

```yaml
limit:
  renewal_period: minute  # second|minute|hour|day|month|year
  calls: 10
```

Returns HTTP 429 when limit exceeded.

### API Key Management Options

1. **Direct in YAML** (not recommended for production)
2. **Environment variable**: `$OPENAI_API_KEY`
3. **File reference**: Path to key file

### Security Recommendations

- Restrict server access
- Store config files securely
- Use environment variables for API keys
- Implement proper access controls

### Starting the Server

```bash
mlflow deployments start-server --config-path config.yaml
```

### Querying Endpoints

**Python Client:**
```python
from mlflow.deployments import get_deploy_client

client = get_deploy_client("http://localhost:5000")

response = client.predict(
    endpoint="chat",
    inputs={
        "messages": [{"role": "user", "content": "Hello!"}]
    }
)
```

**REST API:**
```bash
curl -X POST http://localhost:5000/endpoints/chat/invocations \
  -H "Content-Type: application/json" \
  -d '{"messages": [{"role": "user", "content": "Hello!"}]}'
```

---

## Vector Store & RAG Tracking

### RAG System Tracking with MLflow

MLflow provides tracking for RAG applications through integration with vector stores like ChromaDB.

### Setting Up RAG Tracking

```python
import mlflow
from langchain.vectorstores import Chroma
from langchain.embeddings import OpenAIEmbeddings
from langchain.chains import RetrievalQA

mlflow.langchain.autolog()

# Create vector store
embeddings = OpenAIEmbeddings()
vectorstore = Chroma.from_documents(
    documents,
    embeddings,
    collection_name="docs"
)

# Create retrieval chain
retriever = vectorstore.as_retriever()
qa_chain = RetrievalQA.from_chain_type(
    llm=llm,
    chain_type="stuff",
    retriever=retriever
)

# Queries are automatically traced
result = qa_chain.invoke({"query": "What is MLflow?"})
```

### Versioning Vector Stores

Track changes to your document corpus:

```python
with mlflow.start_run():
    # Log vector store configuration
    mlflow.log_params({
        "chunk_size": 1000,
        "embedding_model": "text-embedding-ada-002",
        "num_documents": len(documents)
    })

    # Log the index as an artifact
    vectorstore.persist()
    mlflow.log_artifacts("./chroma_db", "vector_store")
```

### Evaluating RAG Systems

```python
import mlflow

# Define evaluation data
eval_data = [
    {"question": "What is MLflow?", "ground_truth": "..."},
    # ...
]

# Evaluate with built-in metrics
results = mlflow.evaluate(
    model=qa_chain,
    data=eval_data,
    targets="ground_truth",
    model_type="question-answering",
    evaluators=["default"],
    extra_metrics=[
        mlflow.metrics.latency(),
        mlflow.metrics.token_count()
    ]
)
```

### Best Practices for RAG Tracking

1. **Version your index**: Track document additions/removals
2. **Log embedding parameters**: chunk_size, overlap, model
3. **Track retrieval metrics**: relevance scores, latency
4. **Evaluate chunking strategies**: Compare different configurations
5. **Monitor token usage**: Track costs across the pipeline

---

## Manual Tracing & Custom Instrumentation

### Using Decorators

```python
import mlflow
from mlflow.entities import SpanType

@mlflow.trace(
    name="my_function",
    span_type=SpanType.TOOL,
    attributes={"key": "value"}
)
def my_tool(x: int, y: int) -> int:
    return x + y
```

### Available Span Types

- `SpanType.LLM` - Language model calls
- `SpanType.CHAIN` - Chain/workflow steps
- `SpanType.TOOL` - Tool/function calls
- `SpanType.AGENT` - Agent operations
- `SpanType.RETRIEVER` - Retrieval operations
- `SpanType.EMBEDDING` - Embedding generation
- `SpanType.PARSER` - Output parsing

### Dynamic Attribute Setting

```python
@mlflow.trace(span_type=SpanType.LLM)
def invoke(prompt: str):
    model_id = "gpt-4o-mini"

    # Get current span and set attributes
    span = mlflow.get_current_active_span()
    span.set_attributes({
        "model": model_id,
        "temperature": 0.7
    })

    return client.invoke(prompt, model=model_id)
```

### Context Manager for Code Blocks

```python
with mlflow.start_span(name="my_operation") as span:
    span.set_inputs({"x": x, "y": y})

    # Your code here
    result = complex_operation(x, y)

    span.set_outputs(result)
    span.set_attribute("intermediate_value", intermediate)
```

### Combining Auto and Manual Tracing

```python
import mlflow
from openai import OpenAI

mlflow.openai.autolog()

@mlflow.trace(name="my_app", span_type=SpanType.CHAIN)
def my_application(user_input: str):
    # OpenAI calls are auto-traced as child spans
    client = OpenAI()

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": user_input}]
    )

    return response.choices[0].message.content
```

### Low-Level Client API

For advanced control:

```python
from mlflow import MlflowClient

client = MlflowClient()

# Start trace
trace = client.start_trace(
    name="my_trace",
    inputs={"input_key": "input_value"}
)

# Start child span
span = client.start_span(
    name="child_span",
    request_id=trace.info.request_id,
    parent_id=trace.info.root_span_id,
    inputs={"span_input": "value"}
)

# End span
client.end_span(
    request_id=span.request_id,
    span_id=span.span_id,
    outputs={"output_key": "output_value"},
    attributes={"custom_attr": "value"}
)

# End trace
client.end_trace(
    request_id=trace.info.request_id,
    outputs={"final_output": "value"}
)
```

---

## Common Patterns & Best Practices

### 1. Organizing Experiments

```python
import mlflow

# Set experiment by name
mlflow.set_experiment("Production/LLM-Chatbot")

# Or by ID
mlflow.set_experiment(experiment_id="123")

# Set tags for filtering
mlflow.set_tags({
    "environment": "production",
    "model_type": "chat",
    "version": "1.0.0"
})
```

### 2. Accessing Traces Programmatically

```python
# Get last trace
last_trace_id = mlflow.get_last_active_trace_id()
trace = mlflow.get_trace(trace_id=last_trace_id)

# Access spans
for span in trace.data.spans:
    print(f"Span: {span.name}")
    print(f"  Duration: {span.end_time_ns - span.start_time_ns}ns")
    print(f"  Inputs: {span.inputs}")
    print(f"  Outputs: {span.outputs}")
```

### 3. Error Handling

Traces automatically capture exceptions:

```python
@mlflow.trace()
def risky_operation():
    try:
        # Your code
        result = call_api()
    except Exception as e:
        # Exception is automatically logged to span
        raise

# View exceptions in MLflow UI
```

### 4. Production Deployment Pattern

```python
import mlflow

# Disable autolog in production if needed
mlflow.openai.autolog(disable=True)

# Use manual tracing for specific operations
@mlflow.trace(name="production_inference")
def inference(request):
    # Production code
    pass
```

### 5. Multi-Model Comparison

```python
import mlflow

models = ["gpt-4o-mini", "claude-sonnet-4-20250514", "gemini-1.5-flash"]

for model in models:
    with mlflow.start_run(run_name=f"eval_{model}"):
        mlflow.log_param("model", model)

        # Run evaluation
        metrics = evaluate_model(model)

        mlflow.log_metrics(metrics)
```

### 6. Cost Tracking

```python
# Token usage is automatically captured
trace = mlflow.get_trace(trace_id=trace_id)
token_usage = trace.info.token_usage

# Calculate cost (example pricing)
COST_PER_1K_INPUT = 0.001
COST_PER_1K_OUTPUT = 0.003

cost = (
    token_usage.input_tokens * COST_PER_1K_INPUT / 1000 +
    token_usage.output_tokens * COST_PER_1K_OUTPUT / 1000
)

mlflow.log_metric("estimated_cost", cost)
```

### 7. OpenTelemetry Export

Export traces to external systems:

```python
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter

# Configure exporter
provider = TracerProvider()
processor = BatchSpanProcessor(OTLPSpanExporter(endpoint="localhost:4317"))
provider.add_span_processor(processor)
trace.set_tracer_provider(provider)

# MLflow traces will be exported
```

---

## Quick Reference: All Autolog Functions

| Library | Autolog Function | Min MLflow Version | Token Tracking |
|---------|-----------------|-------------------|----------------|
| LangChain | `mlflow.langchain.autolog()` | 2.14.0 | 3.1.0 |
| OpenAI | `mlflow.openai.autolog()` | 2.14.0 | 3.1.0 |
| Transformers | `mlflow.transformers.autolog()` | 2.14.0 | N/A |
| LlamaIndex | `mlflow.llama_index.autolog()` | 2.14.0 | 3.2.0 |
| Anthropic | `mlflow.anthropic.autolog()` | 2.17.0 | 3.2.0 |
| Gemini | `mlflow.gemini.autolog()` | 2.17.0 | 3.4.0 |
| Bedrock | `mlflow.bedrock.autolog()` | 2.17.0 | 3.1.0 |
| Mistral | `mlflow.mistral.autolog()` | 2.17.0 | N/A |
| DSPy | `mlflow.dspy.autolog()` | 2.17.0 | 3.5.0 |
| AutoGen | `mlflow.autogen.autolog()` | 3.1.0 | 3.2.0 |
| CrewAI | `mlflow.crewai.autolog()` | 3.0.0 | N/A |

---

## Resources

### Official Documentation
- [MLflow Tracing Overview](https://mlflow.org/docs/latest/llms/tracing/index.html)
- [MLflow GenAI Integrations](https://mlflow.org/docs/latest/genai/tracing/integrations/)
- [MLflow LLM Evaluation](https://mlflow.org/docs/latest/llms/llm-evaluate/)

### API References
- [mlflow.langchain](https://mlflow.org/docs/latest/python_api/mlflow.langchain.html)
- [mlflow.openai](https://mlflow.org/docs/latest/python_api/mlflow.openai.html)
- [mlflow.transformers](https://mlflow.org/docs/latest/python_api/mlflow.transformers.html)
- [mlflow.llama_index](https://mlflow.org/docs/latest/python_api/mlflow.llama_index.html)

### Tutorials & Guides
- [RAG Evaluation with MLflow](https://mlflow.org/docs/latest/llms/rag/notebooks/mlflow-e2e-evaluation/)
- [Building Advanced RAG with LlamaIndex](https://mlflow.org/blog/mlflow-llama-index-workflow)
- [Custom Tracing Guide](https://mlflow.org/blog/custom-tracing)

---

*Last updated: November 2025*
*MLflow version coverage: 2.14.0 - 3.5.0*
