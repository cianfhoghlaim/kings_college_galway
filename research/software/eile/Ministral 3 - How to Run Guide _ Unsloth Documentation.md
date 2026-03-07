---
title: "Ministral 3 - How to Run Guide | Unsloth Documentation"
source: "https://docs.unsloth.ai/models/ministral-3#fine-tuning"
author:
published: 2025-12-15
created: 2025-12-20
description: "Guide for Mistral Ministral 3 models, to run or fine-tune locally on your device"
tags:
  - "clippings"
---
istral releases Ministral 3, their new multimodal models in Base, Instruct, and Reasoning variants, available in **3B**, **8B**, and **14B** sizes. They offer best-in-class performance for their size, and are fine-tuned for instruction and chat use cases. The multimodal models support **256K context** windows, multiple languages, native function calling, and JSON output.

The full unquantized 14B Ministral-3-Instruct-2512 model fits in **24GB RAM** /VRAM. You can now run, fine-tune and RL on all Ministral 3 models with Unsloth:

[Run Ministral 3 Tutorials](https://docs.unsloth.ai/models/ministral-3#run-ministral-3-tutorials) [Fine-tuning Ministral 3](https://docs.unsloth.ai/models/ministral-3#fine-tuning)

We've also uploaded Mistral Large 3 [GGUFs here](https://huggingface.co/unsloth/Mistral-Large-3-675B-Instruct-2512-GGUF). For all Ministral 3 uploads (BnB, FP8), [see here](https://huggingface.co/collections/unsloth/ministral-3).

To achieve optimal performance for **Instruct**, Mistral recommends using lower temperatures such as `temperature = 0.15` or `0.1 `

For **Reasoning**, Mistral recommends `temperature = 0.7` and `top_p = 0.95`.

Instruct:

Reasoning:

**Adequate Output Length**: Use an output length of `32,768` tokens for most queries for the reasoning variant, and `16,384` for the instruct variant. You can increase the max output size for the reasoning model if necessary.

The maximum context length Ministral 3 can reach is `262,144`

The chat template format is found when we use the below:

```
tokenizer.apply_chat_template([

    {"role" : "user", "content" : "What is 1+1?"},

    {"role" : "assistant", "content" : "2"},

    {"role" : "user", "content" : "What is 2+2?"}

    ], add_generation_prompt = True

)
```

```
<s>[SYSTEM_PROMPT]# HOW YOU SHOULD THINK AND ANSWER

First draft your thinking process (inner monologue) until you arrive at a response. Format your response using Markdown, and use LaTeX for any mathematical equations. Write both your thoughts and the response in the same language as the input.

Your thinking process must follow the template below:[THINK]Your thoughts or/and draft, like working through an exercise on scratch paper. Be as casual and as long as you want until you are confident to generate the response to the user.[/THINK]Here, provide a self-contained response.[/SYSTEM_PROMPT][INST]What is 1+1?[/INST]2</s>[INST]What is 2+2?[/INST]
```

```
<s>[SYSTEM_PROMPT]You are Ministral-3-3B-Instruct-2512, a Large Language Model (LLM) created by Mistral AI, a French startup headquartered in Paris.

You power an AI assistant called Le Chat.

Your knowledge base was last updated on 2023-10-01.

The current date is {today}.

When you're not sure about some information or when the user's request requires up-to-date or specific data, you must use the available tools to fetch the information. Do not hesitate to use tools whenever they can provide a more accurate or complete response. If no relevant tools are available, then clearly state that you don't have the information and avoid making up anything.

If the user's question is not clear, ambiguous, or does not provide enough context for you to accurately answer the question, you do not try to answer it right away and you rather ask the user to clarify their request (e.g. "What are some good restaurants around me?" => "Where are you?" or "When is the next flight to Tokyo" => "Where do you travel from?").

You are always very attentive to dates, in particular you try to resolve dates (e.g. "yesterday" is {yesterday}) and when asked about information at specific dates, you discard information that is at another date.

You follow these instructions in all languages, and always respond to the user in the language they use or request.

Next sections describe the capabilities that you have.

# WEB BROWSING INSTRUCTIONS

You cannot perform any web search or access internet to open URLs, links etc. If it seems like the user is expecting you to do so, you clarify the situation and ask the user to copy paste the text directly in the chat.

# MULTI-MODAL INSTRUCTIONS

You have the ability to read images, but you cannot generate images. You also cannot transcribe audio files or videos.

You cannot read nor transcribe audio files or videos.

# TOOL CALLING INSTRUCTIONS

You may have access to tools that you can use to fetch information or perform actions. You must use these tools in the following situations:

1. When the request requires up-to-date information.

2. When the request requires specific data that you do not have in your knowledge base.

3. When the request involves actions that you cannot perform without tools.

Always prioritize using tools to provide the most accurate and helpful response. If tools are not available, inform the user that you cannot perform the requested action at the moment.[/SYSTEM_PROMPT][INST]What is 1+1?[/INST]2</s>[INST]What is 2+2?[/INST]
```

Below are guides for the [Reasoning](https://docs.unsloth.ai/models/ministral-3#reasoning-ministral-3-reasoning-2512) and [Instruct](https://docs.unsloth.ai/models/ministral-3#instruct-ministral-3-instruct-2512) variants of the model.

### Instruct: Ministral-3-Instruct-2512

To achieve optimal performance for **Instruct**, Mistral recommends using lower temperatures such as `temperature = 0.15` or `0.1`

1

Obtain the latest `llama.cpp` on [GitHub here](https://github.com/ggml-org/llama.cpp). You can follow the build instructions below as well. Change `-DGGML_CUDA=ON` to `-DGGML_CUDA=OFF` if you don't have a GPU or just want CPU inference.

2

You can directly pull from Hugging Face via:

```
./llama.cpp/llama-cli \

    -hf unsloth/Ministral-3-14B-Instruct-2512-GGUF:Q4_K_XL \

    --jinja -ngl 99 --threads -1 --ctx-size 32684 \

    --temp 0.15
```

3

Download the model via (after installing `pip install huggingface_hub hf_transfer` ). You can choose `UD_Q4_K_XL` or other quantized versions.

```
# !pip install huggingface_hub hf_transfer

import os

os.environ["HF_HUB_ENABLE_HF_TRANSFER"] = "1"

from huggingface_hub import snapshot_download

snapshot_download(

    repo_id = "unsloth/Ministral-3-14B-Instruct-2512-GGUF",

    local_dir = "Ministral-3-14B-Instruct-2512-GGUF",

    allow_patterns = ["*UD-Q4_K_XL*"],

)
```

### Reasoning: Ministral-3-Reasoning-2512

To achieve optimal performance for **Reasoning**, Mistral recommends using `temperature = 0.7` and `top_p = 0.95`.

1

Obtain the latest `llama.cpp` on [GitHub](https://github.com/ggml-org/llama.cpp). You can also use the build instructions below. Change `-DGGML_CUDA=ON` to `-DGGML_CUDA=OFF` if you don't have a GPU or just want CPU inference.

2

You can directly pull from Hugging Face via:

```
./llama.cpp/llama-cli \

    -hf unsloth/Ministral-3-14B-Reasoning-2512-GGUF:Q4_K_XL \

    --jinja -ngl 99 --threads -1 --ctx-size 32684 \

    --temp 0.6 --top-p 0.95
```

3

Download the model via (after installing `pip install huggingface_hub hf_transfer` ). You can choose `UD_Q4_K_XL` or other quantized versions.

```
# !pip install huggingface_hub hf_transfer

import os

os.environ["HF_HUB_ENABLE_HF_TRANSFER"] = "1"

from huggingface_hub import snapshot_download

snapshot_download(

    repo_id = "unsloth/Ministral-3-14B-Reasoning-2512-GGUF",

    local_dir = "Ministral-3-14B-Reasoning-2512-GGUF",

    allow_patterns = ["*UD-Q4_K_XL*"],

)
```

Unsloth now supports fine-tuning of all Ministral 3 models, including vision support. To train, you must use the latest 🤗Hugging Face `transformers` v5 and `unsloth` which includes our our recent [ultra long context](https://docs.unsloth.ai/new/500k-context-length-fine-tuning) support. The large 14B Ministral 3 model should fit on a free Colab GPU.

We made free Unsloth notebooks to fine-tune Ministral 3. Change the name to use the desired model.

- Ministral-3B-Instruct [Vision notebook](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Ministral_3_VL_\(3B\)_Vision.ipynb) (vision)
- Ministral-3B-Instruct [GRPO notebook](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Ministral_3_\(3B\)_Reinforcement_Learning_Sudoku_Game.ipynb)

Ministral Vision finetuning notebook

[Google Colab colab.research.google.com](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Ministral_3_VL_\(3B\)_Vision.ipynb)

Ministral Sudoku GRPO RL notebook

[Google Colab colab.research.google.com](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Ministral_3_\(3B\)_Reinforcement_Learning_Sudoku_Game.ipynb)

Unsloth now supports RL and GRPO for the Mistral models as well. As usual, they benefit from all of Unsloth's enhancements and tomorrow, we are going to release a notebook soon specifically for autonomously solving the sudoku puzzle.

- Ministral-3B-Instruct [GRPO notebook](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Ministral_3_\(3B\)_Reinforcement_Learning_Sudoku_Game.ipynb)

**To use the latest version of Unsloth and transformers v5, update via:**

```
pip install --upgrade --force-reinstall --no-cache-dir --no-deps unsloth unsloth_zoo
```

The goal is to auto generate strategies to complete Sudoku!

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252F2qDbhHfpuhNAHOtIernm%252Fimage.png%3Falt%3Dmedia%26token%3D9a3d4bb2-3994-4ec8-aeb8-16bc2bcb77c4&width=768&dpr=4&quality=100&sign=c9cade6a&sv=2) ![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FLZlHHeAjoVAeO6juQDiC%252Fimage.png%3Falt%3Dmedia%26token%3D45abbb30-b705-4eec-81fc-fb99dd0c2621&width=768&dpr=4&quality=100&sign=9ddef3bd&sv=2)

For the reward plots for Ministral, we get the below. We see it works well!

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FqpfPNKkSF2O1T0flshEi%252Funknown.png%3Falt%3Dmedia%26token%3Da2f14139-bcab-40bf-a054-f189de5d23df&width=300&dpr=4&quality=100&sign=373222cd&sv=2)

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252Fe8TBzOVVn5iYhlJ6nh63%252Funknown.png%3Falt%3Dmedia%26token%3D520699f9-ffd0-43a5-a0ef-263fa678b4bd&width=300&dpr=4&quality=100&sign=701e4bf0&sv=2)

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FudSxKSBuSOIXONrtarmp%252Funknown.png%3Falt%3Dmedia%26token%3Dbeefcbce-67df-4ce2-92b8-3e0adc240df6&width=300&dpr=4&quality=100&sign=aa4cc58&sv=2)

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FgwwlcVjMt9nqyqVC6xqD%252Funknown.png%3Falt%3Dmedia%26token%3Db5b390b6-c9e6-4926-9a70-d4aa365caa86&width=300&dpr=4&quality=100&sign=f3650c86&sv=2)

[Previous Devstral 2](https://docs.unsloth.ai/models/devstral-2) [Next GLM-4.6](https://docs.unsloth.ai/models/glm-4.6-how-to-run-locally)

Last updated

Was this helpful?