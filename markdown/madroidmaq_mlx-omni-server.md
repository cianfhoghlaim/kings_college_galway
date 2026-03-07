---
title: "madroidmaq/mlx-omni-server"
source: "https://deepwiki.com/madroidmaq/mlx-omni-server/1-overview"
author:
  - "[[DeepWiki]]"
published: 2025-11-06
created: 2025-12-15
description: "This document provides a high-level introduction to the MLX Omni Server codebase, covering its purpose, architecture, and key components. For detailed information about specific subsystems, see:- API"
tags:
  - "clippings"
---
Menu

## Overview

Relevant source files
- [README.md](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/README.md)
- [docs/anthropic-api.md](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/docs/anthropic-api.md)
- [docs/banner.png](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/docs/banner.png)
- [docs/development\_guide.md](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/docs/development_guide.md)
- [docs/openai-api.md](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/docs/openai-api.md)
- [pyproject.toml](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/pyproject.toml)
- [src/mlx\_omni\_server/chat/mlx/outlines\_logits\_processor.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/chat/mlx/outlines_logits_processor.py)
- [uv.lock](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/uv.lock)

This document provides a high-level introduction to the MLX Omni Server codebase, covering its purpose, architecture, and key components. For detailed information about specific subsystems, see:

- API endpoint implementations: [OpenAI Chat Completions API](https://deepwiki.com/madroidmaq/mlx-omni-server/3.1-openai-chat-completions-api), [Anthropic Messages API](https://deepwiki.com/madroidmaq/mlx-omni-server/3.2-anthropic-messages-api), [Audio APIs](https://deepwiki.com/madroidmaq/mlx-omni-server/3.3-audio-apis-\(tts-and-stt\)), [Image Generation API](https://deepwiki.com/madroidmaq/mlx-omni-server/3.4-image-generation-api), [Embeddings API](https://deepwiki.com/madroidmaq/mlx-omni-server/3.5-embeddings-api), [Model Management API](https://deepwiki.com/madroidmaq/mlx-omni-server/3.6-model-management-api)
- Core components: [MLX Model Engine](https://deepwiki.com/madroidmaq/mlx-omni-server/4.1-mlxmodel-and-model-loading), [Model Caching Infrastructure](https://deepwiki.com/madroidmaq/mlx-omni-server/4.2-model-caching-infrastructure), [Chat Generation Pipeline](https://deepwiki.com/madroidmaq/mlx-omni-server/4.3-chatgenerator-and-generation-pipeline)
- Usage examples: [Basic Chat Completions](https://deepwiki.com/madroidmaq/mlx-omni-server/5.1-basic-chat-completions), [Structured Output & JSON Schema](https://deepwiki.com/madroidmaq/mlx-omni-server/5.2-structured-output-and-json-schema), [Function Calling & Tools](https://deepwiki.com/madroidmaq/mlx-omni-server/5.3-function-calling-and-tools)
- Development setup: [Installation & Setup](https://deepwiki.com/madroidmaq/mlx-omni-server/6.1-installation-and-setup), [Running the Server](https://deepwiki.com/madroidmaq/mlx-omni-server/6.2-running-the-server), [Testing](https://deepwiki.com/madroidmaq/mlx-omni-server/6.3-testing)

## What is MLX Omni Server?

MLX Omni Server is a FastAPI-based local AI inference server optimized for Apple Silicon (M1/M2/M3/M4 chips) that provides **dual API compatibility** with both OpenAI and Anthropic SDKs. It acts as a drop-in replacement for cloud-based AI services, enabling completely local inference using Apple's MLX framework.

**Core Capabilities:**

- **Dual API Support**: Full compatibility with OpenAI SDK (`/v1/*`) and Anthropic SDK (`/anthropic/v1/*`)
- **Multi-Modal Processing**: Chat completions, text-to-speech, speech-to-text, image generation, and embeddings
- **Privacy-First Architecture**: All inference runs locally on-device with no external data transmission
- **Hardware Acceleration**: Optimized for Apple Silicon using the MLX framework
- **Intelligent Caching**: Model and prompt caching for improved performance

The server exposes HTTP endpoints that are wire-compatible with official OpenAI and Anthropic SDKs. Applications can switch to local inference by simply changing the `base_url` parameter in their client configuration.

Sources: [README.md 1-28](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/README.md#L1-L28) [pyproject.toml 1-87](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/pyproject.toml#L1-L87)

## Key Features

| Category | Features |
| --- | --- |
| **Chat Completion** | Streaming, function calling, structured output with JSON schema, thinking mode (reasoning), logprobs, prompt caching |
| **Audio Processing** | Text-to-Speech (F5-TTS, MlxAudio), Speech-to-Text (Whisper), multiple voice options |
| **Image Generation** | Text-to-image using Flux models via mflux library |
| **Embeddings** | Vector embeddings for semantic search using BERT-like models |
| **Model Management** | Auto-discovery from HuggingFace cache, on-demand loading, LRU caching with TTL |
| **API Support** | OpenAI SDK compatible, Anthropic SDK compatible, raw HTTP/REST |

Sources: [README.md 20-28](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/README.md#L20-L28) [README.md 86-104](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/README.md#L86-L104)

## High-Level System Architecture

### System Overview

The following diagram shows how external clients interact with the server and how requests flow through the major system layers:

```
StorageMLX Omni Server :10240External ClientsMLX ModelsMLX Generation LayerCore ServicesAPI Layerbase_url=/v1base_url=/anthropicauto-downloadauto-downloadauto-downloadauto-downloadauto-downloadOpenAI SDK ClientAnthropic SDK ClientREST API ClientsOpenAI API Router
/v1/*
routers/openai/Anthropic API Router
/anthropic/v1/*
routers/anthropic/ChatService
chat/adapters/TTSService
audio/tts/STTService
audio/stt/ImagesService
images/service.pyEmbeddingsService
embeddings/service.pyChatGenerator
chat/mlx/generator.pyMLXWrapperCache
chat/mlx/mlx_wrapper_cache.pyPromptCache
chat/mlx/prompt_cache.pyLanguage ModelsTTS ModelsSTT ModelsImage ModelsEmbedding ModelsHuggingFace Cache
~/.cache/huggingface
```

**Key Architectural Layers:**

1. **API Layer**: Dual routing for OpenAI (`/v1/*`) and Anthropic (`/anthropic/v1/*`) endpoints
2. **Service Layer**: Specialized services for each modality (chat, TTS, STT, images, embeddings)
3. **Generation Layer**: Unified `ChatGenerator` with model and prompt caching
4. **Model Layer**: MLX-optimized models for various tasks
5. **Storage Layer**: HuggingFace cache for automatic model management

Sources: [src/mlx\_omni\_server/main.py 1-69](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/main.py#L1-L69) [src/mlx\_omni\_server/routers/\_\_init\_\_.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/routers/__init__.py)

### Dual API Compatibility Strategy

The server implements a dual API surface that converges on a unified backend:

```
Format TranslationUnified BackendAnthropic API Surface /anthropic/v1/*OpenAI API Surface /v1/*/v1/chat/completions
routers/openai/chat.py/v1/audio/speech/v1/audio/transcriptions/v1/images/generations/v1/embeddings/v1/models/anthropic/v1/messages
routers/anthropic/messages.py/anthropic/v1/modelsChatGenerator
chat/mlx/generator.py
Unified Generation EngineModelCacheScanner
models/cache.py
Unified Model DiscoveryOpenAIAdapter
chat/adapters/openai.py
Request/Response TranslationAnthropicMessagesAdapter
chat/adapters/anthropic.py
Request/Response Translation
```

**Design Benefits:**

- **Single Implementation**: Both APIs use the same `ChatGenerator` and model management code
- **Format Translation**: Adapters handle API-specific request/response formats
- **Feature Parity**: Advanced features (tools, streaming, structured output) work with both APIs
- **Drop-in Compatibility**: Wire-compatible with official OpenAI and Anthropic SDKs

Sources: [src/mlx\_omni\_server/chat/adapters/openai.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/chat/adapters/openai.py) [src/mlx\_omni\_server/chat/adapters/anthropic.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/chat/adapters/anthropic.py) [src/mlx\_omni\_server/routers/openai/chat.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/routers/openai/chat.py) [src/mlx\_omni\_server/routers/anthropic/messages.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/routers/anthropic/messages.py)

The server provides independent services for different modalities, all sharing common infrastructure:

```
Shared InfrastructureEmbedding ServicesVision ServicesAudio ServicesChat Servicesdownloadsdownloadsdownloadsdownloadsdownloadsstores inChat Completions
OpenAI & Anthropic APIs
routers/openai/chat.py
routers/anthropic/messages.pyChatGenerator
chat/mlx/generator.py/v1/audio/speech
routers/openai/audio.py/v1/audio/transcriptions
routers/openai/audio.pyF5-TTS-MLX
Kokoro-82M-4bitWhisper Models
mlx-whisper/v1/images/generations
routers/openai/images.pyFLUX.1 Models
mflux/v1/embeddings
routers/openai/embeddings.pyMiniLM Models
BERT-likeHuggingFace Hub
Model RepositoryLocal Model Cache
~/.cache/huggingface
```

**Service Characteristics:**

| Service | Models | Library | Features |
| --- | --- | --- | --- |
| **Chat** | LLMs (Llama, Qwen, Gemma, etc.) | mlx-lm | Streaming, tools, structured output, thinking mode |
| **TTS** | F5-TTS, Kokoro-82M | f5-tts-mlx, mlx-audio | Multiple voices, WAV output |
| **STT** | Whisper variants | mlx-whisper | Transcription, multiple audio formats |
| **Images** | FLUX.1 models | mflux | Text-to-image, various sizes |
| **Embeddings** | MiniLM, BERT | mlx-embeddings | Vector generation for semantic search |

All services share the same HuggingFace cache infrastructure and automatically download models on first use.

Sources: [pyproject.toml 25-47](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/pyproject.toml#L25-L47) [src/mlx\_omni\_server/routers/openai/](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/routers/openai/) [src/mlx\_omni\_server/routers/anthropic/](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/routers/anthropic/)

## Request Flow

### Chat Completion Request Flow

The following sequence diagram shows how a chat completion request flows through the system:

```
"mlx-lm.generate""ChatGeneratorchat/mlx/generator.py""MLXWrapperCachechat/mlx/mlx_wrapper_cache.py""OpenAIAdapter orAnthropicMessagesAdapterchat/adapters/""api_routerrouters/""RequestResponseLoggingMiddlewaremiddleware/logging.py""OpenAI/AnthropicClient SDK""mlx-lm.generate""ChatGeneratorchat/mlx/generator.py""MLXWrapperCachechat/mlx/mlx_wrapper_cache.py""OpenAIAdapter orAnthropicMessagesAdapterchat/adapters/""api_routerrouters/""RequestResponseLoggingMiddlewaremiddleware/logging.py""OpenAI/AnthropicClient SDK"alt[Model Not Cached]"POST /v1/chat/completionsor /anthropic/v1/messages""Forward request""Route to adapter""_prepare_generation_params()""get_or_create(model_id)""Create new ChatGenerator""load_mlx_model(model_id)""Store in cache""Return ChatGenerator""generate() or generate_stream()""_prepare_prompt()apply_chat_template()""mlx_lm.generate()""Generated tokens""GenerationResult""Format to API schema""ChatCompletion or Message"
```

**Processing Stages:**

| Stage | Component | File Location | Responsibility |
| --- | --- | --- | --- |
| 1\. Request Entry | `RequestResponseLoggingMiddleware` | [middleware/logging.py 22-113](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/middleware/logging.py#L22-L113) | Logs all incoming requests |
| 2\. Routing | `api_router` | [routers/\_\_init\_\_.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/__init__.py) | Routes to OpenAI or Anthropic handler |
| 3\. Format Translation | `OpenAIAdapter` or `AnthropicMessagesAdapter` | [chat/adapters/](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/adapters/) | Translates API-specific format to internal representation |
| 4\. Cache Lookup | `MLXWrapperCache` | [chat/mlx/mlx\_wrapper\_cache.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/mlx/mlx_wrapper_cache.py) | Retrieves or creates `ChatGenerator` instance |
| 5\. Model Loading | `load_mlx_model` | [chat/mlx/model.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/mlx/model.py) | Loads model, tokenizer, and chat template if not cached |
| 6\. Generation | `ChatGenerator.generate()` | [chat/mlx/generator.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/mlx/generator.py) | Orchestrates MLX inference |
| 7\. Response Translation | Adapter | [chat/adapters/](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/adapters/) | Converts `GenerationResult` back to API format |

Sources: [src/mlx\_omni\_server/main.py 1-69](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/main.py#L1-L69) [src/mlx\_omni\_server/middleware/logging.py 22-113](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/middleware/logging.py#L22-L113) [src/mlx\_omni\_server/chat/adapters/openai.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/chat/adapters/openai.py) [src/mlx\_omni\_server/chat/adapters/anthropic.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/chat/adapters/anthropic.py) [src/mlx\_omni\_server/chat/mlx/generator.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/chat/mlx/generator.py)

## Core Technology Stack

The following table lists the key dependencies and their roles:

| Dependency | Version | Purpose |
| --- | --- | --- |
| **fastapi** | \>=0.116.1,<0.117 | Web framework for API endpoints |
| **uvicorn** | \>=0.34.0,<0.35 | ASGI server for running FastAPI |
| **pydantic** | \>=2.9.2,<3 | Data validation and settings management |
| **mlx-lm** | \>=0.28.2 | MLX library for language model inference |
| **outlines** | \==1.0.4 | Structured output with JSON schema constraints |
| **f5-tts-mlx** | \>=0.2.5,<0.3 | Text-to-speech using F5 model |
| **mlx-whisper** | \>=0.4.1 | Speech-to-text transcription |
| **mlx-audio** | \>=0.2.4 | Audio processing utilities |
| **mflux** | \>=0.11.0,<0.12 | Image generation using Flux models |
| **mlx-embeddings** | \>=0.0.3 | Text embedding generation |
| **sse-starlette** | \>=2.1.3,<3 | Server-sent events for streaming |
| **huggingface-hub** | \>=0.30 | Model downloading and caching |

Sources: [pyproject.toml 25-47](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/pyproject.toml#L25-L47)

## Project Structure

The codebase is organized under `src/mlx_omni_server/` with the following structure:

```
mlx_omni_server/
├── main.py                          # FastAPI app and server entry point
├── routers/                         # API route handlers
│   ├── __init__.py                  # api_router aggregation
│   ├── openai/                      # OpenAI-compatible endpoints
│   │   ├── chat.py                  # /v1/chat/completions
│   │   ├── audio.py                 # /v1/audio/*
│   │   ├── images.py                # /v1/images/*
│   │   └── embeddings.py            # /v1/embeddings
│   ├── anthropic/                   # Anthropic-compatible endpoints
│   │   └── messages.py              # /anthropic/v1/messages
│   └── models.py                    # /v1/models
├── chat/                            # Chat completion system
│   ├── adapters/                    # API-specific adapters
│   │   ├── openai.py               # OpenAIAdapter
│   │   └── anthropic.py            # AnthropicMessagesAdapter
│   ├── mlx/                        # Core MLX integration
│   │   ├── generator.py            # ChatGenerator (main engine)
│   │   ├── model.py                # MLXModel wrapper
│   │   ├── mlx_wrapper_cache.py   # In-memory model cache
│   │   ├── prompt_cache.py         # KV cache management
│   │   ├── chat_template.py        # Template application
│   │   ├── outlines_logits_processor.py  # Structured output
│   │   └── thinking_decoder.py     # Reasoning mode
│   └── tools/                      # Tool calling support
│       └── parser.py               # Tool call extraction
├── audio/                          # Audio processing
│   ├── tts/                        # Text-to-speech
│   └── stt/                        # Speech-to-text
├── images/                         # Image generation
│   └── service.py                  # ImagesService
├── embeddings/                     # Text embeddings
│   └── service.py                  # EmbeddingsService
├── models/                         # Model management
│   ├── service.py                  # ModelsService
│   └── cache.py                    # ModelCacheScanner
├── middleware/                     # HTTP middleware
│   └── logging.py                  # Request/response logging
└── utils/                          # Utilities
    └── logger.py                   # Logging configuration
```

**Key Directories:**

- **routers/**: HTTP endpoint handlers organized by API (OpenAI vs Anthropic)
- **chat/**: Complete chat completion system including adapters, core engine, and enhancements
- **chat/mlx/**: MLX-specific implementations for model loading, generation, caching
- **chat/adapters/**: Translation layer between API formats and internal representation
- **chat/tools/**: Tool/function calling support with model-specific parsers
- **audio/**, **images/**, **embeddings/**: Service implementations for additional modalities
- **models/**: Model discovery and management utilities

Sources: Inferred from file paths in context

## Core Components

### Component Overview

| Component | File | Responsibility |
| --- | --- | --- |
| **FastAPI App** | [main.py 10](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/main.py#L10-L10) | Application instance, middleware, router registration |
| **api\_router** | [routers/\_\_init\_\_.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/__init__.py) | Aggregates all API endpoint handlers |
| **OpenAIAdapter** | [chat/adapters/openai.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/adapters/openai.py) | OpenAI request/response format translation |
| **AnthropicMessagesAdapter** | [chat/adapters/anthropic.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/adapters/anthropic.py) | Anthropic request/response format translation |
| **ChatGenerator** | [chat/mlx/generator.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/mlx/generator.py) | Core generation engine for both APIs |
| **MLXModel** | [chat/mlx/model.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/mlx/model.py) | Wraps MLX model, tokenizer, chat template |
| **MLXWrapperCache** | [chat/mlx/mlx\_wrapper\_cache.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/mlx/mlx_wrapper_cache.py) | LRU+TTL cache (max\_size=3, ttl=300s) |
| **PromptCache** | [chat/mlx/prompt\_cache.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/mlx/prompt_cache.py) | KV cache for multi-turn conversations |
| **ChatTemplate** | [chat/mlx/chat\_template.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/mlx/chat_template.py) | Applies model-specific templates and tools |
| **OutlinesLogitsProcessor** | [chat/mlx/outlines\_logits\_processor.py 13-75](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/mlx/outlines_logits_processor.py#L13-L75) | JSON schema constraint enforcement |
| **ThinkingDecoder** | [chat/mlx/thinking\_decoder.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/mlx/thinking_decoder.py) | Reasoning/content separation |
| **ToolParser** | [chat/tools/parser.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/chat/tools/parser.py) | Model-specific tool call extraction |
| **ModelsService** | [models/service.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/models/service.py) | Model discovery and management |
| **ModelCacheScanner** | [models/cache.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/models/cache.py) | Scans HuggingFace cache for models |

Sources: [src/mlx\_omni\_server/main.py 1-69](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/main.py#L1-L69) [src/mlx\_omni\_server/chat/mlx/generator.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/chat/mlx/generator.py) [src/mlx\_omni\_server/chat/mlx/outlines\_logits\_processor.py 13-75](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/chat/mlx/outlines_logits_processor.py#L13-L75)

## API Endpoint Overview

The server exposes two sets of compatible endpoints:

**OpenAI Compatible Endpoints (`/v1/*`)**

| Endpoint | Handler | Description |
| --- | --- | --- |
| `POST /v1/chat/completions` | [routers/openai/chat.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/openai/chat.py) | Chat completions with tools, streaming, structured output |
| `POST /v1/audio/speech` | [routers/openai/audio.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/openai/audio.py) | Text-to-speech generation |
| `POST /v1/audio/transcriptions` | [routers/openai/audio.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/openai/audio.py) | Speech-to-text transcription |
| `POST /v1/images/generations` | [routers/openai/images.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/openai/images.py) | Image generation from text |
| `POST /v1/embeddings` | [routers/openai/embeddings.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/openai/embeddings.py) | Text embedding vectors |
| `GET /v1/models` | [routers/models.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/models.py) | List available models |
| `GET /v1/models/{model_id}` | [routers/models.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/models.py) | Get model details |
| `DELETE /v1/models/{model_id}` | [routers/models.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/models.py) | Delete cached model |

**Anthropic Compatible Endpoints (`/anthropic/v1/*`)**

| Endpoint | Handler | Description |
| --- | --- | --- |
| `POST /anthropic/v1/messages` | [routers/anthropic/messages.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/anthropic/messages.py) | Messages with tools, streaming, thinking mode |
| `GET /anthropic/v1/models` | [routers/models.py](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/routers/models.py) | List models with pagination |

Sources: [README.md 86-104](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/README.md#L86-L104)

## Getting Started

### Installation and Server Startup

```
# Install via pip
pip install mlx-omni-server

# Start server with default settings (port 10240)
mlx-omni-server

# Custom configuration
mlx-omni-server --port 8000 --log-level debug --workers 2

# View all options
mlx-omni-server --help
```

**Server Configuration Options:**

| Argument | Default | Description |
| --- | --- | --- |
| `--host` | `0.0.0.0` | Server bind address |
| `--port` | `10240` | Server port |
| `--workers` | `1` | Number of uvicorn workers |
| `--log-level` | `info` | Logging level (debug/info/warning/error/critical) |

Sources: [src/mlx\_omni\_server/main.py 21-68](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/src/mlx_omni_server/main.py#L21-L68) [README.md 29-41](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/README.md#L29-L41)

### Using with OpenAI SDK

```
from openai import OpenAI

# Connect to local server
client = OpenAI(
    base_url="http://localhost:10240/v1",
    api_key="not-needed"  # API key not required for local inference
)

# Basic chat completion
response = client.chat.completions.create(
    model="mlx-community/gemma-3-1b-it-4bit-DWQ",
    messages=[{"role": "user", "content": "Hello!"}]
)
print(response.choices[0].message.content)

# Streaming response
stream = client.chat.completions.create(
    model="mlx-community/gemma-3-1b-it-4bit-DWQ",
    messages=[{"role": "user", "content": "Tell me a story"}],
    stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)
```

Sources: [README.md 44-61](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/README.md#L44-L61) [docs/openai-api.md 23-80](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/docs/openai-api.md#L23-L80)

### Using with Anthropic SDK

```
import anthropic

# Connect to local server
client = anthropic.Anthropic(
    base_url="http://localhost:10240/anthropic",
    api_key="not-needed"  # API key not required for local inference
)

# Basic message completion
message = client.messages.create(
    model="mlx-community/gemma-3-1b-it-4bit-DWQ",
    max_tokens=1000,
    messages=[{"role": "user", "content": "Hello!"}]
)
print(message.content[0].text)

# Streaming message
with client.messages.stream(
    model="mlx-community/gemma-3-1b-it-4bit-DWQ",
    max_tokens=1000,
    messages=[{"role": "user", "content": "Tell me a story"}]
) as stream:
    for text in stream.text_stream:
        print(text, end="", flush=True)
```

Sources: [README.md 63-81](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/README.md#L63-L81) [docs/anthropic-api.md 23-102](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/docs/anthropic-api.md#L23-L102)

## Development Setup

### Local Development Environment

```
# Clone repository
git clone https://github.com/madroidmaq/mlx-omni-server.git
cd mlx-omni-server

# Install dependencies with uv
uv sync

# Start server with hot-reload for development
uv run uvicorn mlx_omni_server.main:app --reload --host 0.0.0.0 --port 10240
```

### Running Tests

```
# Run all tests
uv run pytest

# Run specific test suites
uv run pytest tests/chat/openai/       # OpenAI API tests
uv run pytest tests/chat/anthropic/    # Anthropic API tests

# Run with verbose output
uv run pytest -v
```

### Code Quality

```
# Format code
uv run black . && uv run isort .

# Run pre-commit hooks
uv run pre-commit install
uv run pre-commit run --all-files
```

Sources: [docs/development\_guide.md 1-78](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/docs/development_guide.md#L1-L78) [README.md 120-146](https://github.com/madroidmaq/mlx-omni-server/blob/f11fda84/README.md#L120-L146)

This overview provides a foundation for understanding the MLX Omni Server architecture. For deeper dives into specific areas:

- **Understanding the API layer**: See [OpenAI Chat Completions API](https://deepwiki.com/madroidmaq/mlx-omni-server/3.1-openai-chat-completions-api) and [Anthropic Messages API](https://deepwiki.com/madroidmaq/mlx-omni-server/3.2-anthropic-messages-api)
- **Core generation system**: See [Chat Generation Pipeline](https://deepwiki.com/madroidmaq/mlx-omni-server/4.3-chatgenerator-and-generation-pipeline) and [Chat Template & Tool Integration](https://deepwiki.com/madroidmaq/mlx-omni-server/4.4-chattemplate-and-tool-integration)
- **Advanced features**: See [Structured Output System](https://deepwiki.com/madroidmaq/mlx-omni-server/4.5-structured-output-system), [Reasoning & Thinking Mode](https://deepwiki.com/madroidmaq/mlx-omni-server/4.6-reasoning-and-thinking-mode), [Tool Calling & Function Parsing](https://deepwiki.com/madroidmaq/mlx-omni-server/4.7-tool-calling-and-function-parsing)
- **Practical examples**: See [Usage Examples](https://deepwiki.com/madroidmaq/mlx-omni-server/5-usage-examples) section
- **Contributing**: See [Development Guide](https://deepwiki.com/madroidmaq/mlx-omni-server/6-development-guide) section

The modular architecture allows working on individual services (audio, images, embeddings) independently while sharing common infrastructure (logging, caching, model management).

Sources: Inferred from table of contents in context