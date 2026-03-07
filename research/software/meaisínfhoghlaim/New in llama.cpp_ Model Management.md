---
title: "New in llama.cpp: Model Management"
source: "https://huggingface.co/blog/ggml-org/model-management-in-llamacpp"
author:
published: 2025-12-11
created: 2025-12-13
description: "A Blog post by ggml.ai on Hugging Face"
tags:
  - "clippings"
---
[Back to Articles](https://huggingface.co/blog)

[Community Article](https://huggingface.co/blog/community) Published December 11, 2025

[llama.cpp server](https://github.com/ggml-org/llama.cpp/tree/master/tools/server) now ships with **router mode**, which lets you dynamically load, unload, and switch between multiple models without restarting.

> Reminder: llama.cpp server is a lightweight, OpenAI-compatible HTTP server for running LLMs locally.

This feature was a popular request to bring Ollama-style model management to llama.cpp. It uses a multi-process architecture where each model runs in its own process, so if one model crashes, others remain unaffected.

## Quick Start

Start the server in router mode by **not specifying a model**:

```bash
llama-server
```

This auto-discovers models from your llama.cpp cache (`LLAMA_CACHE` or `~/.cache/llama.cpp`). If you've previously downloaded models via `llama-server -hf user/model`, they'll be available automatically.

You can also point to a local directory of GGUF files:

```bash
llama-server --models-dir ./my-models
```

## Features

1. **Auto-discovery**: Scans your llama.cpp cache (default) or a custom `--models-dir` folder for GGUF files
2. **On-demand loading**: Models load automatically when first requested
3. **LRU eviction**: When you hit `--models-max` (default: 4), the least-recently-used model unloads
4. **Request routing**: The `model` field in your request determines which model handles it

## Examples

### Chat with a specific model

```bash
curl http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "ggml-org/gemma-3-4b-it-GGUF:Q4_K_M",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

On the first request, the server automatically loads the model into memory (loading time depends on model size). Subsequent requests to the same model are instant since it's already loaded.

### List available models

```bash
curl http://localhost:8080/models
```

Returns all discovered models with their status (`loaded`, `loading`, or `unloaded`).

### Manually load a model

```bash
curl -X POST http://localhost:8080/models/load \
  -H "Content-Type: application/json" \
  -d '{"model": "my-model.gguf"}'
```

### Unload a model to free VRAM

```bash
curl -X POST http://localhost:8080/models/unload \
  -H "Content-Type: application/json" \
  -d '{"model": "my-model.gguf"}'
```

## Key Options

| Flag | Description |
| --- | --- |
| `--models-dir PATH` | Directory containing your GGUF files |
| `--models-max N` | Max models loaded simultaneously (default: 4) |
| `--no-models-autoload` | Disable auto-loading; require explicit `/models/load` calls |

All model instances inherit settings from the router:

```bash
llama-server --models-dir ./models -c 8192 -ngl 99
```

All loaded models will use 8192 context and full GPU offload. You can also define per-model settings using [presets](https://github.com/ggml-org/llama.cpp/pull/17859):

```bash
llama-server --models-preset config.ini
```

```bash
[my-model]
model = /path/to/model.gguf
ctx-size = 65536
temp = 0.7
```

## Also available in the Web UI

The [built-in web UI](https://github.com/ggml-org/llama.cpp/tree/master/tools/server/webui) also supports model switching. Just select a model from the dropdown and it loads automatically.

## Join the Conversation

We hope this feature makes it easier to A/B test different model versions, run multi-tenant deployments, or simply switch models during development without restarting the server.

Have questions or feedback? Drop a comment below or open an issue on [GitHub](https://github.com/ggml-org/llama.cpp/issues).

### Community

[bukit](https://huggingface.co/bukit)

Mmproj support?

- [![](https://huggingface.co/avatars/f7540cf1ef2370e402df13b3587384f9.svg)](https://huggingface.co/grailfinder "grailfinder")
·

[sbeltz](https://huggingface.co/sbeltz)

Supported via presets.ini, where you can specify the mmproj (and other long and short arguments) per model.

[sbeltz](https://huggingface.co/sbeltz)

Awesome new feature! Can model selection be done on something other than requested model name? Like maybe specify the ranking in presets.ini, and then the highest ranked model that can satisfy the request will be the default. So maybe one model is best for short context, another (or the same with other settings) for when the context gets too long, and another when image input is required.

[xbruce22](https://huggingface.co/xbruce22)

This is good addition, Thank you.

[etemiz](https://huggingface.co/etemiz)

•

[edited 1 day ago](https://huggingface.co/blog/ggml-org/#693c57ea6107ec9c17bb2879 "Edited 4 times by etemiz")

what is the best way to get <think> </think> and the tokens in between? openAI library is removing them.. i want to run llama-server in console and talk to it using a python library that does not remove the thinking tokens.

i checked the llama-cpp-python but it does not have that.

[razvanab](https://huggingface.co/razvanab)

Now I can use llama.cpp all the time. A big thank you to the devs.

[sbeltz](https://huggingface.co/sbeltz)

Is there currently a way to have a "default" model if the request doesn't specify? Could be the currently loaded model or a specific model. (Just noticed one of my apps broke because it's used to llama-server not requiring a model name.)