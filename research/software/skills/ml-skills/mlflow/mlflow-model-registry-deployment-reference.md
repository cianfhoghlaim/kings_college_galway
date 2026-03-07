# MLflow Model Registry and Deployment Reference

## Comprehensive Guide for LLM Model Management and Deployment

This document provides detailed reference documentation for MLflow's model registry, model formats, LLM-specific flavors, deployment patterns, and governance capabilities.

---

## Table of Contents

1. [Model Registry](#1-model-registry)
2. [MLflow Models Format](#2-mlflow-models-format)
3. [LLM-Specific Model Flavors](#3-llm-specific-model-flavors)
4. [Model Serving and Deployment](#4-model-serving-and-deployment)
5. [Model Governance](#5-model-governance)

---

## 1. Model Registry

The MLflow Model Registry is a centralized model store, set of APIs, and UI designed to collaboratively manage the full lifecycle of MLflow Models.

### 1.1 Core Concepts

#### Registered Model
A model stored in the registry with:
- **Unique name**: Identifier for the model
- **Versions**: Multiple versions of the same model
- **Aliases**: Mutable named references to specific versions
- **Tags**: Key-value pairs for categorization
- **Annotations**: Markdown descriptions for documentation

#### Model Version
Each registered model supports multiple versions:
- First model added is version 1
- Each subsequent registration increments the version number
- Versions track creation timestamps, source run IDs, and status

#### Model URI
Reference format for accessing models:
```
models:/<model-name>/<model-version>
models:/<model-name>@<alias>
```

### 1.2 Registering Models

#### Method 1: During Logging
```python
import mlflow.sklearn

mlflow.sklearn.log_model(
    sk_model,
    "model",
    registered_model_name="MyModel"
)
```

#### Method 2: After Experiment
```python
result = mlflow.register_model(
    model_uri="runs:/<run-id>/model",
    name="MyModel"
)
```

#### Method 3: Create Empty Model First
```python
from mlflow import MlflowClient

client = MlflowClient()
client.create_registered_model("MyModel")
client.create_model_version(
    name="MyModel",
    source="runs:/<run-id>/model",
    run_id="<run-id>"
)
```

#### UI Registration
1. Navigate to run's artifacts
2. Select the model folder
3. Click "Register Model"
4. Choose existing model or create new

### 1.3 Model Versions and Stages (Deprecated)

**Important**: Starting in MLflow 2.9, model registry stages are deprecated in favor of aliases and tags.

#### Legacy Stages (Deprecated)
- `None` - Initial state
- `Staging` - Testing/validation
- `Production` - Live deployment
- `Archived` - Deprecated models

### 1.4 Model Aliases (Recommended)

Aliases provide flexible named references for model versions, replacing the rigid stage system.

#### Setting Aliases
```python
from mlflow import MlflowClient

client = MlflowClient()

# Set alias
client.set_registered_model_alias(
    name="MyModel",
    alias="champion",
    version=1
)

# Reassign to new version
client.set_registered_model_alias(
    name="MyModel",
    alias="champion",
    version=2
)
```

#### Loading by Alias
```python
import mlflow.pyfunc

model = mlflow.pyfunc.load_model("models:/MyModel@champion")
```

#### Benefits Over Stages
- Multiple aliases per model version (enables A/B testing)
- Flexible naming (e.g., `champion`, `challenger`, `baseline`)
- Easy rollback via alias reassignment
- No fixed progression path required

### 1.5 Model Version Tags

Tags annotate model versions with metadata for tracking status and organization.

#### Setting Tags
```python
client.set_model_version_tag(
    name="MyModel",
    version="1",
    key="validation_status",
    value="pending"
)

# Update after validation
client.set_model_version_tag(
    name="MyModel",
    version="1",
    key="validation_status",
    value="passed"
)
```

#### Common Tag Patterns
```python
# Task identification
client.set_model_version_tag("MyModel", "1", "task", "text-classification")

# Environment tracking
client.set_model_version_tag("MyModel", "1", "environment", "production")

# Approval status
client.set_model_version_tag("MyModel", "1", "approved_by", "ml-team-lead")
```

### 1.6 Model Lineage Tracking

The Model Registry automatically tracks provenance for each model:

#### Tracked Information
- **Source Run ID**: Which MLflow run produced the model
- **Source Experiment**: Parent experiment
- **Artifacts**: Associated files and data
- **Parameters**: Training configuration
- **Metrics**: Performance measurements
- **Code Version**: Git commit hash (if using MLflow Projects)

#### Accessing Lineage
```python
from mlflow import MlflowClient

client = MlflowClient()
version = client.get_model_version(name="MyModel", version="1")

print(f"Run ID: {version.run_id}")
print(f"Source: {version.source}")
print(f"Created: {version.creation_timestamp}")
```

### 1.7 Environment-Based Model Management

Modern pattern using separate registered models per environment:

```python
# Development
mlflow.register_model("runs:/<run-id>/model", "dev.ml_team.revenue_forecasting")

# Staging
mlflow.register_model("runs:/<run-id>/model", "staging.ml_team.revenue_forecasting")

# Production
mlflow.register_model("runs:/<run-id>/model", "prod.ml_team.revenue_forecasting")
```

#### Promoting Models Between Environments
```python
client.copy_model_version(
    src_model_uri="models:/dev.ml_team.revenue_forecasting/1",
    dst_name="staging.ml_team.revenue_forecasting"
)
```

---

## 2. MLflow Models Format

An MLflow Model is a standard format for packaging machine learning models that can be used across various deployment tools.

### 2.1 Model Directory Structure

```
my_model/
├── MLmodel                    # Model metadata (YAML)
├── model.pkl                  # Model artifact (flavor-specific)
├── conda.yaml                 # Conda environment
├── python_env.yaml            # Python environment
├── requirements.txt           # Pip dependencies
├── input_example.json         # Sample input
├── serving_input_example.json # REST API payload example
├── environment_variables.txt  # Required env var names
└── metadata/                  # Additional metadata
```

### 2.2 MLmodel File Structure

The `MLmodel` file is a YAML configuration defining how to use the model:

```yaml
artifact_path: model
flavors:
  python_function:
    env:
      conda: conda.yaml
      virtualenv: python_env.yaml
    loader_module: mlflow.sklearn
    model_path: model.pkl
    predict_fn: predict
    python_version: 3.10.12
  sklearn:
    code: null
    pickled_model: model.pkl
    serialization_format: cloudpickle
    sklearn_version: 1.3.2
mlflow_version: 2.9.2
model_size_bytes: 123456
model_uuid: abc123def456
run_id: 1234567890abcdef
signature:
  inputs: '[{"type": "double", "name": "feature1"}, {"type": "double", "name": "feature2"}]'
  outputs: '[{"type": "long", "name": "prediction"}]'
utc_time_created: '2024-01-15 10:30:00.000000'
```

### 2.3 Model Flavors

Flavors enable deployment tools to understand models from any ML library without specific integrations.

#### Built-in Flavors

**Traditional ML**
- `sklearn` - Scikit-learn models
- `xgboost` - XGBoost models
- `lightgbm` - LightGBM models
- `catboost` - CatBoost models
- `spark` - Spark MLlib models

**Deep Learning**
- `pytorch` - PyTorch models
- `tensorflow` - TensorFlow models
- `keras` - Keras models
- `onnx` - ONNX models

**LLM/GenAI**
- `transformers` - Hugging Face models
- `langchain` - LangChain applications
- `openai` - OpenAI API models
- `sentence_transformers` - Embedding models
- `llama_index` - LlamaIndex applications

**Time Series**
- `prophet` - Facebook Prophet
- `pmdarima` - Statistical time series
- `statsmodels` - Statsmodels

**Other**
- `spacy` - spaCy NLP
- `h2o` - H2O models
- `diviner` - Grouped time series

#### Universal python_function Flavor

All flavors include the `python_function` flavor, enabling generic inference:

```python
import mlflow.pyfunc

# Load any model as PyFunc
model = mlflow.pyfunc.load_model("models:/MyModel/1")
predictions = model.predict(data)
```

### 2.4 Model Signatures

Signatures define expected input/output schemas, providing validation during inference.

#### Column-Based Signatures (Tabular Data)

```python
from mlflow.models.signature import infer_signature
from mlflow.types.schema import Schema, ColSpec

# Automatic inference
signature = infer_signature(X_train, model.predict(X_train))

# Manual definition
input_schema = Schema([
    ColSpec("double", "sepal_length"),
    ColSpec("double", "sepal_width"),
    ColSpec("string", "species", required=False)
])
output_schema = Schema([ColSpec("long", "prediction")])
signature = ModelSignature(inputs=input_schema, outputs=output_schema)

# Log with signature
mlflow.sklearn.log_model(model, "model", signature=signature)
```

#### Tensor-Based Signatures (Deep Learning)

```python
from mlflow.types.schema import Schema, TensorSpec
import numpy as np

# Image classification model
input_schema = Schema([
    TensorSpec(np.dtype(np.float32), (-1, 28, 28, 1), "images")
])
output_schema = Schema([
    TensorSpec(np.dtype(np.float32), (-1, 10), "probabilities")
])
```

Note: `-1` indicates variable batch size.

#### Signature Enforcement

MLflow automatically validates inputs when:
- Loading models as PyFunc
- Using MLflow deployment tools
- Serving via REST API

**Important**: Signatures are REQUIRED for registering models in Databricks Unity Catalog.

### 2.5 Input Examples

Input examples provide concrete samples of valid model input:

```python
import pandas as pd

input_example = pd.DataFrame({
    "sepal_length": [5.1],
    "sepal_width": [3.5],
    "petal_length": [1.4],
    "petal_width": [0.2]
})

mlflow.sklearn.log_model(
    model,
    "model",
    input_example=input_example  # Signature auto-inferred
)
```

Benefits:
- Automatic signature inference
- Documentation for users
- Auto-generated serving payloads

### 2.6 Custom PyFunc Models

Create custom models by extending `mlflow.pyfunc.PythonModel`:

#### Basic Custom Model

```python
import mlflow.pyfunc

class CustomModel(mlflow.pyfunc.PythonModel):
    def load_context(self, context):
        """Load artifacts during initialization."""
        import pickle
        with open(context.artifacts["preprocessor"], "rb") as f:
            self.preprocessor = pickle.load(f)
        with open(context.artifacts["model"], "rb") as f:
            self.model = pickle.load(f)

    def predict(self, context, model_input, params=None):
        """Generate predictions."""
        processed = self.preprocessor.transform(model_input)
        return self.model.predict(processed)

# Log custom model
artifacts = {
    "preprocessor": "path/to/preprocessor.pkl",
    "model": "path/to/model.pkl"
}

mlflow.pyfunc.log_model(
    artifact_path="model",
    python_model=CustomModel(),
    artifacts=artifacts,
    conda_env="conda.yaml"
)
```

#### Streaming Predictions

```python
class StreamingModel(mlflow.pyfunc.PythonModel):
    def predict(self, context, model_input, params=None):
        return self._generate_all(model_input)

    def predict_stream(self, context, model_input, params=None):
        """Generator for streaming responses."""
        for chunk in self._generate_chunks(model_input):
            yield chunk
```

#### Type Hints for Validation

```python
from typing import List, Dict, Any

class TypedModel(mlflow.pyfunc.PythonModel):
    def predict(
        self,
        context,
        model_input: List[Dict[str, Any]],
        params: Dict[str, str] = None
    ) -> List[str]:
        # Automatic validation enabled
        return [self._process(item) for item in model_input]
```

### 2.7 Models From Code (MLflow 2.12.2+)

Define models in Python scripts without pickling:

```python
# my_model.py
import mlflow

class MyModel(mlflow.pyfunc.PythonModel):
    def predict(self, context, model_input, params=None):
        return model_input * 2

mlflow.models.set_model(MyModel())
```

```python
# Log the script
mlflow.pyfunc.log_model(
    artifact_path="model",
    python_model="my_model.py"
)
```

Benefits:
- Avoids pickle/cloudpickle security risks
- Cleaner code organization
- Better for complex dependencies
- Currently supports: LangChain, LlamaIndex, PythonModel

---

## 3. LLM-Specific Model Flavors

MLflow provides native flavors for popular LLM frameworks, introduced starting in MLflow 2.3.

### 3.1 mlflow.transformers

Integration with Hugging Face Transformers for experiment tracking, model management, and deployment.

#### Logging Models

```python
import mlflow
from transformers import pipeline

# Text generation pipeline
generator = pipeline("text-generation", model="gpt2")

with mlflow.start_run():
    mlflow.transformers.log_model(
        transformers_model=generator,
        artifact_path="generator",
        task="text-generation",
        registered_model_name="GPT2Generator"
    )
```

#### Supported Task Types

- `text-generation` - Text completion
- `text-classification` - Sentiment, classification
- `question-answering` - Q&A
- `summarization` - Text summarization
- `translation` - Language translation
- `fill-mask` - Masked language modeling
- `feature-extraction` - Embeddings

#### OpenAI-Compatible Chat (MLflow 2.11+)

```python
mlflow.transformers.log_model(
    transformers_model=model,
    artifact_path="chat_model",
    task="llm/v1/chat"  # OpenAI-compatible endpoint
)
```

#### Component Logging

```python
# Log individual components
mlflow.transformers.log_model(
    transformers_model={
        "model": model,
        "tokenizer": tokenizer,
        "model_config": config
    },
    artifact_path="model"
)
```

#### PEFT/LoRA Support

```python
from peft import LoraConfig, get_peft_model

# Native support for parameter-efficient fine-tuning
peft_model = get_peft_model(base_model, lora_config)
mlflow.transformers.log_model(peft_model, "lora_model")
```

#### Loading Methods

```python
# Native Transformers format
model = mlflow.transformers.load_model("models:/MyModel/1")

# PyFunc for deployment
pyfunc_model = mlflow.pyfunc.load_model("models:/MyModel/1")
predictions = pyfunc_model.predict(data)
```

### 3.2 mlflow.langchain

Integration for LangChain applications including chains, agents, and retrievers.

#### Supported Components

- **Agents**: Dynamic constructs using LLMs for action sequences
- **Retrievers**: Document sourcing for RAG applications
- **Runnables**: LangChain Expression Language (LCEL) components
- **LangGraph**: Stateful multi-agent applications

#### Logging Chains

```python
import mlflow
from langchain.chains import LLMChain

with mlflow.start_run():
    mlflow.langchain.log_model(
        lc_model=chain,
        artifact_path="chain",
        registered_model_name="MyChain"
    )
```

#### Autologging

```python
mlflow.langchain.autolog()

# All chain executions automatically logged
result = chain.invoke({"question": "What is MLflow?"})
```

#### Models-from-Code (Recommended)

For complex chains with unpicklable components:

```python
# chain_definition.py
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
import mlflow

prompt = ChatPromptTemplate.from_template("Tell me about {topic}")
chain = prompt | ChatOpenAI()

mlflow.models.set_model(chain)
```

```python
# Log the script
mlflow.langchain.log_model(
    lc_model="chain_definition.py",
    artifact_path="chain"
)
```

#### RAG Applications

```python
from langchain.chains import RetrievalQA
from langchain_community.vectorstores import FAISS

# Create retriever
vectorstore = FAISS.from_documents(documents, embeddings)
retriever = vectorstore.as_retriever()

# Create RAG chain
rag_chain = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=retriever
)

mlflow.langchain.log_model(rag_chain, "rag_chain")
```

#### Streaming Support (MLflow 2.12.2+)

```python
model = mlflow.pyfunc.load_model("models:/MyChain/1")

for chunk in model.predict_stream({"question": "Explain MLOps"}):
    print(chunk, end="")
```

### 3.3 mlflow.openai

Integration for OpenAI API applications.

#### Logging OpenAI Models

```python
import mlflow.openai

with mlflow.start_run():
    mlflow.openai.log_model(
        model="gpt-4",
        task="chat.completions",
        artifact_path="openai_model",
        messages=[
            {"role": "system", "content": "You are a helpful assistant."}
        ]
    )
```

#### Task Types

- `chat.completions` - Conversational AI
- `completions` - Text completion (legacy)
- `embeddings` - Text embeddings

#### Autologging and Tracing

```python
mlflow.openai.autolog()

from openai import OpenAI
client = OpenAI()

# Automatically traced
response = client.chat.completions.create(
    model="gpt-4",
    messages=[{"role": "user", "content": "Hello!"}]
)
```

#### Function Calling

```python
functions = [
    {
        "name": "get_weather",
        "description": "Get weather for a location",
        "parameters": {
            "type": "object",
            "properties": {
                "location": {"type": "string"}
            }
        }
    }
]

mlflow.openai.log_model(
    model="gpt-4",
    task="chat.completions",
    artifact_path="function_model",
    functions=functions
)
```

### 3.4 mlflow.sentence_transformers

Integration for Sentence Transformers embedding models.

#### Logging Embedding Models

```python
from sentence_transformers import SentenceTransformer
import mlflow

model = SentenceTransformer("all-MiniLM-L6-v2")

with mlflow.start_run():
    mlflow.sentence_transformers.log_model(
        model,
        artifact_path="embedder",
        registered_model_name="TextEmbedder"
    )
```

#### OpenAI-Compatible Embeddings

```python
mlflow.sentence_transformers.log_model(
    model,
    artifact_path="embedder",
    task="llm/v1/embeddings"  # OpenAI-compatible format
)
```

#### Loading and Inference

```python
# Native loading
model = mlflow.sentence_transformers.load_model("models:/TextEmbedder/1")
embeddings = model.encode(["Hello world", "How are you?"])

# PyFunc loading
pyfunc_model = mlflow.pyfunc.load_model("models:/TextEmbedder/1")
embeddings = pyfunc_model.predict(["Hello world"])
```

#### Inference Configuration

```python
mlflow.sentence_transformers.log_model(
    model,
    artifact_path="embedder",
    inference_config={
        "batch_size": 32,
        "normalize_embeddings": True
    }
)
```

#### Serving

```bash
mlflow models serve -m 'models:/TextEmbedder/1' -p 5000
```

```bash
curl -X POST http://localhost:5000/invocations \
  -H "Content-Type: application/json" \
  -d '{"inputs": ["Hello world"]}'
```

### 3.5 mlflow.llama_index

Integration for LlamaIndex RAG applications and workflows.

#### Supported Objects

- **Index**: Vector stores, knowledge graphs
- **Query Engine**: Straightforward retrieval
- **Chat Engine**: Conversational with context
- **Retriever**: Document retrieval
- **Workflow**: Event-driven orchestration (MLflow 2.15+)

#### Logging LlamaIndex Applications

```python
import mlflow
from llama_index.core import VectorStoreIndex

# Create index
index = VectorStoreIndex.from_documents(documents)

with mlflow.start_run():
    mlflow.llama_index.log_model(
        llama_index_model=index,
        artifact_path="index",
        engine_type="query",  # or "chat", "retriever"
        registered_model_name="RAGIndex"
    )
```

#### Engine Types

```python
# Query engine - simple retrieval
mlflow.llama_index.log_model(index, "model", engine_type="query")

# Chat engine - conversational with history
mlflow.llama_index.log_model(index, "model", engine_type="chat")

# Retriever - document retrieval only
mlflow.llama_index.log_model(index, "model", engine_type="retriever")
```

#### Workflow Support

```python
from llama_index.core.workflow import Workflow

class RAGWorkflow(Workflow):
    @step
    async def retrieve(self, ev: StartEvent) -> RetrieveEvent:
        # Retrieval logic
        pass

    @step
    async def synthesize(self, ev: RetrieveEvent) -> StopEvent:
        # Generation logic
        pass

workflow = RAGWorkflow()
mlflow.llama_index.log_model(workflow, "workflow")
```

#### Autologging and Tracing

```python
mlflow.llama_index.autolog()

# Nested traces automatically logged
response = query_engine.query("What is MLflow?")
```

#### Model Configuration

```python
mlflow.llama_index.log_model(
    index,
    artifact_path="index",
    engine_type="chat",
    model_config={
        "temperature": 0.7,
        "system_prompt": "You are a helpful assistant."
    }
)
```

### 3.6 ChatModel and ResponsesAgent

#### ChatModel (MLflow 2.11+)

Standardized interface for conversational AI, compatible with OpenAI's ChatCompletion API:

```python
import mlflow
from mlflow.pyfunc import ChatModel
from mlflow.types.llm import ChatMessage, ChatResponse

class MyChatModel(ChatModel):
    def predict(self, context, messages, params=None):
        # Process messages
        response_text = self._generate_response(messages)
        return ChatResponse(
            choices=[{
                "message": ChatMessage(
                    role="assistant",
                    content=response_text
                )
            }]
        )

mlflow.pyfunc.log_model(
    artifact_path="chat",
    python_model=MyChatModel()
)
```

#### ResponsesAgent (MLflow 3.0+)

Recommended for new projects:

```python
from mlflow.pyfunc import ResponsesAgent

class MyAgent(ResponsesAgent):
    def predict(self, context, input_data, params=None):
        # Handle tool calls, streaming, etc.
        pass

mlflow.pyfunc.log_model(
    artifact_path="agent",
    python_model=MyAgent()
)
```

Features:
- Full OpenAI API compatibility
- Streaming support
- Tool call tracking
- Usage metrics

---

## 4. Model Serving and Deployment

MLflow provides a unified interface for deploying models to various targets.

### 4.1 Local Model Serving

#### Basic Serving

```bash
mlflow models serve -m runs:/<run-id>/model -p 5000
```

Or from Model Registry:

```bash
mlflow models serve -m "models:/MyModel/1" -p 5000
mlflow models serve -m "models:/MyModel@champion" -p 5000
```

#### REST API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/invocations` | POST | Generate predictions |
| `/ping` | GET | Health check |
| `/health` | GET | Health check |
| `/version` | GET | MLflow version |

#### Request Formats

**JSON (DataFrame Split)**
```bash
curl -X POST http://localhost:5000/invocations \
  -H "Content-Type: application/json" \
  -d '{
    "dataframe_split": {
      "columns": ["feature1", "feature2"],
      "data": [[1.0, 2.0], [3.0, 4.0]]
    }
  }'
```

**JSON (Instances)**
```bash
curl -X POST http://localhost:5000/invocations \
  -H "Content-Type: application/json" \
  -d '{
    "instances": [
      {"feature1": 1.0, "feature2": 2.0}
    ]
  }'
```

**CSV**
```bash
curl -X POST http://localhost:5000/invocations \
  -H "Content-Type: text/csv" \
  -d 'feature1,feature2
1.0,2.0
3.0,4.0'
```

#### Serving Options

```bash
# Specify environment manager
mlflow models serve -m model_uri --env-manager virtualenv

# Enable MLServer (high performance)
mlflow models serve -m model_uri --enable-mlserver
```

### 4.2 Batch Inference

```bash
mlflow models predict \
  -m "models:/MyModel/1" \
  -i input.csv \
  -o output.csv
```

```python
# Python API
import mlflow.pyfunc

model = mlflow.pyfunc.load_model("models:/MyModel/1")
predictions = model.predict(input_data)
```

### 4.3 Docker Containerization

#### Building Docker Images

```bash
# Basic Docker image
mlflow models build-docker \
  -m "models:/MyModel/1" \
  -n my-model-image

# With MLServer for Kubernetes
mlflow models build-docker \
  -m "models:/MyModel/1" \
  -n my-model-image \
  --enable-mlserver
```

#### Running Container

```bash
docker run -p 5000:8080 my-model-image
```

#### Custom Dockerfile

```dockerfile
FROM python:3.10-slim

RUN pip install mlflow

COPY model /model

CMD ["mlflow", "models", "serve", "-m", "/model", "-h", "0.0.0.0", "-p", "8080"]
```

### 4.4 Cloud Platform Deployments

#### Amazon SageMaker

```python
import mlflow.sagemaker

mlflow.sagemaker.deploy(
    app_name="my-model",
    model_uri="models:/MyModel/1",
    region_name="us-west-2",
    mode="create",
    instance_type="ml.m5.large"
)
```

#### Azure ML

```python
from mlflow.deployments import get_deploy_client

client = get_deploy_client("azureml://...")

client.create_deployment(
    name="my-deployment",
    model_uri="models:/MyModel/1",
    config={
        "instance_type": "Standard_DS3_v2",
        "instance_count": 1
    }
)
```

#### Databricks Model Serving

```python
from mlflow.deployments import get_deploy_client

client = get_deploy_client("databricks")

client.create_endpoint(
    name="my-endpoint",
    config={
        "served_models": [{
            "model_name": "MyModel",
            "model_version": "1",
            "workload_size": "Small",
            "scale_to_zero_enabled": True
        }]
    }
)
```

### 4.5 Kubernetes Deployment

#### KServe/Seldon Core

```bash
# Build MLServer-enabled image
mlflow models build-docker \
  -m "models:/MyModel/1" \
  -n my-model:latest \
  --enable-mlserver

# Push to registry
docker push my-registry/my-model:latest
```

**KServe InferenceService:**

```yaml
apiVersion: serving.kserve.io/v1beta1
kind: InferenceService
metadata:
  name: my-model
spec:
  predictor:
    containers:
      - name: mlserver
        image: my-registry/my-model:latest
        ports:
          - containerPort: 8080
        resources:
          requests:
            memory: "2Gi"
            cpu: "1"
```

**Kubernetes Deployment:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mlflow-model
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mlflow-model
  template:
    metadata:
      labels:
        app: mlflow-model
    spec:
      containers:
        - name: model
          image: my-registry/my-model:latest
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: "2Gi"
              cpu: "1"
---
apiVersion: v1
kind: Service
metadata:
  name: mlflow-model-service
spec:
  selector:
    app: mlflow-model
  ports:
    - port: 80
      targetPort: 8080
  type: LoadBalancer
```

### 4.6 Inference Server Options

#### FastAPI (Default)

- Included with MLflow
- Asynchronous request handling
- Suitable for most use cases

#### MLServer (High Performance)

```bash
pip install mlflow[extras]
mlflow models serve -m model_uri --enable-mlserver
```

Benefits:
- Adaptive batching
- Parallel inference
- V2 Inference Protocol
- KServe/Seldon integration

### 4.7 MLflow AI Gateway

Unified endpoint for managing multiple LLM providers.

#### Installation

```bash
pip install 'mlflow[gateway]'
```

#### Configuration

```yaml
# gateway_config.yaml
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
      calls: 100

  - name: embeddings
    endpoint_type: llm/v1/embeddings
    model:
      provider: openai
      name: text-embedding-3-small
      config:
        openai_api_key: $OPENAI_API_KEY
```

#### Starting Gateway

```bash
mlflow gateway start --config-path gateway_config.yaml --port 7000
```

#### Supported Providers

- OpenAI (including Azure OpenAI)
- Anthropic
- Cohere
- Amazon Bedrock
- Mistral
- AI21 Labs
- MosaicML
- Hugging Face TGI

#### Benefits

- Centralized API key management
- Rate limiting
- Provider swapping without code changes
- Consistent API across providers
- Hot-swapping endpoints (no restart required)

---

## 5. Model Governance

MLflow provides capabilities for managing model lifecycle, approvals, and access control.

### 5.1 Model Approval Workflows

#### Using Tags for Status Tracking

```python
from mlflow import MlflowClient

client = MlflowClient()

# Mark for review
client.set_model_version_tag(
    name="MyModel",
    version="1",
    key="approval_status",
    value="pending_review"
)

# Record reviewer
client.set_model_version_tag(
    name="MyModel",
    version="1",
    key="reviewed_by",
    value="ml-team-lead"
)

# Mark approved
client.set_model_version_tag(
    name="MyModel",
    version="1",
    key="approval_status",
    value="approved"
)

# Promote to production
client.set_registered_model_alias(
    name="MyModel",
    alias="champion",
    version=1
)
```

#### CI/CD Integration

```python
# Example: Jenkins/GitHub Actions workflow
def validate_and_promote_model(model_name, version):
    client = MlflowClient()

    # Load model
    model = mlflow.pyfunc.load_model(f"models:/{model_name}/{version}")

    # Run tests
    test_results = run_model_tests(model)

    if test_results.passed:
        # Update tags
        client.set_model_version_tag(
            model_name, version,
            "validation_status", "passed"
        )

        # Promote to staging
        client.copy_model_version(
            f"models:/{model_name}/{version}",
            f"staging.{model_name}"
        )

        return True
    else:
        client.set_model_version_tag(
            model_name, version,
            "validation_status", "failed"
        )
        return False
```

### 5.2 Audit Trails

#### Automatic Tracking

MLflow automatically records:
- **Creation timestamps**: When models were registered
- **Source run IDs**: Training provenance
- **User information**: Who registered the model (with auth enabled)
- **Tag history**: All metadata changes

#### Accessing Audit Information

```python
from mlflow import MlflowClient

client = MlflowClient()

# Get model version details
version = client.get_model_version(name="MyModel", version="1")

print(f"Created: {version.creation_timestamp}")
print(f"Last Updated: {version.last_updated_timestamp}")
print(f"Source Run: {version.run_id}")
print(f"User: {version.user_id}")  # With auth enabled

# Get all tags
for tag in version.tags:
    print(f"{tag.key}: {tag.value}")
```

#### Lineage Queries

```python
# Get complete model history
versions = client.search_model_versions(f"name='{model_name}'")

for version in versions:
    print(f"Version {version.version}:")
    print(f"  Run ID: {version.run_id}")
    print(f"  Created: {version.creation_timestamp}")
    print(f"  Status: {version.status}")
```

### 5.3 Access Control

#### MLflow Authentication (Open Source)

Enable basic HTTP authentication:

```bash
pip install mlflow[auth]
export MLFLOW_FLASK_SERVER_SECRET_KEY="secret-key"
mlflow server --app-name basic-auth
```

#### Permission Levels

| Permission | Capabilities |
|------------|-------------|
| `READ` | View access only |
| `EDIT` | Read and modify |
| `MANAGE` | Full control including permissions |
| `NO_PERMISSIONS` | No access |

#### Managing Permissions

```python
from mlflow.server import get_app_client

auth_client = get_app_client(
    "basic-auth",
    tracking_uri="http://localhost:5000/"
)

# Create user
auth_client.create_user(username="analyst", password="password123")

# Grant experiment access
auth_client.create_experiment_permission(
    experiment_id="1",
    username="analyst",
    permission="EDIT"
)

# Grant model access
auth_client.create_registered_model_permission(
    name="MyModel",
    username="analyst",
    permission="READ"
)
```

#### Admin Users

Default admin credentials (change immediately):
- Username: `admin`
- Password: `password`

Admin capabilities:
- Create/delete users
- Update passwords and admin status
- Grant/revoke all permissions
- Access all resources

#### Authentication Methods

1. **UI Login**: Prompted on first visit

2. **Environment Variables**:
```bash
export MLFLOW_TRACKING_USERNAME=myuser
export MLFLOW_TRACKING_PASSWORD=mypassword
```

3. **Credentials File** (`~/.mlflow/credentials`):
```ini
[mlflow]
mlflow_tracking_username = myuser
mlflow_tracking_password = mypassword
```

4. **REST API**:
```bash
curl -u myuser:mypassword http://localhost:5000/api/2.0/mlflow/experiments/list
```

### 5.4 Enterprise Governance Solutions

#### Databricks Unity Catalog

Features:
- Centralized governance across workspaces
- Fine-grained access control
- Cross-workspace model discovery
- Integrated lineage tracking
- Data masking and encryption

```python
# Register to Unity Catalog
mlflow.set_registry_uri("databricks-uc")

mlflow.sklearn.log_model(
    model,
    "model",
    registered_model_name="main.ml_models.my_model"
)
```

#### AWS Integration

Use API Gateway for enterprise access control:
- Separate authentication/authorization from MLflow server
- IAM role-based access
- Fine-grained permissions via policies

#### Azure ML RBAC

Leverage Azure's role-based access control:
- Built-in roles (ML Scientist, ML Engineer)
- Custom role definitions
- Resource group scoping

### 5.5 Security Best Practices

1. **API Key Management**
   - Never commit API keys to code
   - Use environment variables
   - Rotate keys regularly
   - MLflow logs env var names (not values) in `environment_variables.txt`

2. **Model Validation**
   - Always validate models before promotion
   - Use `mlflow.models.predict()` for pre-deployment testing
   - Implement automated testing pipelines

3. **Environment Separation**
   - Use separate registered models per environment
   - Apply appropriate access controls per environment
   - Document promotion requirements

4. **Audit and Compliance**
   - Enable authentication for tracking
   - Use tags to document approvals
   - Maintain lineage for reproducibility
   - Regular access reviews

5. **Network Security**
   - Use HTTPS for all communications
   - Implement network isolation where needed
   - Consider VPN for remote access

---

## Appendix: Quick Reference

### Common MLflow Commands

```bash
# Start tracking server
mlflow server --host 0.0.0.0 --port 5000

# With authentication
mlflow server --app-name basic-auth

# Serve model locally
mlflow models serve -m "models:/MyModel/1" -p 5000

# Build Docker image
mlflow models build-docker -m "models:/MyModel/1" -n my-image

# Batch prediction
mlflow models predict -m "models:/MyModel/1" -i input.csv -o output.csv
```

### Common Python Patterns

```python
import mlflow
from mlflow import MlflowClient

# Set tracking URI
mlflow.set_tracking_uri("http://localhost:5000")

# Start run and log model
with mlflow.start_run():
    mlflow.log_param("learning_rate", 0.01)
    mlflow.log_metric("accuracy", 0.95)
    mlflow.sklearn.log_model(
        model, "model",
        registered_model_name="MyModel"
    )

# Load model for inference
model = mlflow.pyfunc.load_model("models:/MyModel@champion")
predictions = model.predict(data)

# Client operations
client = MlflowClient()
client.set_registered_model_alias("MyModel", "champion", 1)
client.set_model_version_tag("MyModel", "1", "status", "approved")
```

### Model URI Formats

| Format | Example |
|--------|---------|
| Run artifact | `runs:/<run-id>/model` |
| Registry version | `models:/<name>/<version>` |
| Registry alias | `models:/<name>@<alias>` |
| S3 | `s3://bucket/path/to/model` |
| Local file | `file:///path/to/model` |

### Environment Variables

| Variable | Description |
|----------|-------------|
| `MLFLOW_TRACKING_URI` | Tracking server URL |
| `MLFLOW_REGISTRY_URI` | Model registry URL |
| `MLFLOW_TRACKING_USERNAME` | Auth username |
| `MLFLOW_TRACKING_PASSWORD` | Auth password |
| `MLFLOW_AUTH_CONFIG_PATH` | Auth config file path |

---

## References

- [MLflow Official Documentation](https://mlflow.org/docs/latest/)
- [MLflow Model Registry](https://mlflow.org/docs/latest/ml/model-registry/)
- [MLflow GenAI Documentation](https://mlflow.org/docs/latest/genai/)
- [MLflow Deployment Guide](https://mlflow.org/docs/latest/ml/deployment/)
- [MLflow Authentication](https://mlflow.org/docs/latest/self-hosting/security/basic-http-auth/)
- [MLflow GitHub Repository](https://github.com/mlflow/mlflow)
