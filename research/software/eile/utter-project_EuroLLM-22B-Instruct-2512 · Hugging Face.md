---
title: "utter-project/EuroLLM-22B-Instruct-2512 · Hugging Face"
source: "https://huggingface.co/utter-project/EuroLLM-22B-Instruct-2512"
author:
published: 2025-12-14
created: 2025-12-16
description: "We’re on a journey to advance and democratize artificial intelligence through open source and open science."
tags:
  - "clippings"
---
[Edit model card](https://huggingface.co/utter-project/EuroLLM-22B-Instruct-2512/edit/main/README.md)

## Model Card for EuroLLM-22B-Instruct

This is the model card for EuroLLM-22B-Instruct. You can also check the pre-trained version: [EuroLLM-22B-2515](https://huggingface.co/utter-project/EuroLLM-22B-2512).

- **Developed by:** Instituto Superior Técnico - University of Lisbon, Instituto de Telecomunicações, University of Edinburgh, Aveni, Unbabel, University of Paris-Saclay, Artefact Research Center, University of Amsterdam, Naver Labs, Sorbonne Université.
- **Funded by:** European Union.
- **Model type:** A 22B parameter multilingual transfomer LLM.
- **Language(s) (NLP):** Bulgarian, Croatian, Czech, Danish, Dutch, English, Estonian, Finnish, French, German, Greek, Hungarian, Irish, Italian, Latvian, Lithuanian, Maltese, Polish, Portuguese, Romanian, Slovak, Slovenian, Spanish, Swedish, Arabic, Catalan, Chinese, Galician, Hindi, Japanese, Korean, Norwegian, Russian, Turkish, and Ukrainian.
- **License:** Apache License 2.0.

## Model Details

The EuroLLM project has the goal of creating a suite of LLMs capable of understanding and generating text in all European Union languages as well as some additional relevant languages. EuroLLM-22B is a 22B parameter model trained on 4 trillion tokens divided across the considered languages and several data sources: Web data, parallel data (en-xx and xx-en), and high-quality datasets. EuroLLM-22B-Instruct was further instruction tuned on EuroBlocks, an instruction tuning dataset with focus on general instruction-following and machine translation.

### Architecture

EuroLLM uses a standard, dense Transformer architecture withgrouped query attention (GQA), pre-layer normalization with RMSNorm, SwiGLU activations and rotary positional embeddings (RoPE) in every layer. Here is a summary of the model hyper-parameters:

|  |  |
| --- | --- |
| Sequence Length | 32,768 |
| Number of Layers | 56 |
| Embedding Size | 6,144 |
| FFN Hidden Size | 16,384 |
| Number of Heads | 48 |
| Number of KV Heads (GQA) | 8 |
| Activation Function | SwiGLU |
| Position Encodings | RoPE (\\Theta=1,000,000) |
| Layer Norm | RMSNorm |
| Tied Embeddings | No |
| Embedding Parameters | 0.786B |
| LM Head Parameters | 0.786B |
| Non-embedding Parameters | 21.067B |
| Total Parameters | 22.639B |

### Pre-training

EuroLLM-22B was trained on approximately 4 trillion tokens, using 400 Nvidia H100 GPUs on the MareNostrum5 supercomputer, thanks to an EuroHPC extreme-scale access grant. The training process was carefully structured into three key phases:

1. Initial Pre-training (3.6 trillion tokens) This phase includes the warm-up and constant learning rate stages, during which the model is trained on a mixture of web data alongside higher quality sources such as parallel data, Wikipedia, Arxiv, books, math, code and Apollo datasets. This balanced mix helps the model build a strong multilingual foundation.
2. Annealing (400 billion tokens) During this phase, there is a linear decay of the learning rate and we adjust the data mix to reduce the proportion of web data while increasing the multilingual content and select the highest quality data—by making use of quality filters such as \[CometKiwi-22\](https://huggingface.co/Unbabel/wmt22-cometkiwi-da) and \[EuroFilter\](https://huggingface.co/utter-project/EuroFilter-v1). This shift helps the model refine its understanding across diverse languages and domains.
3. Annealing to Zero (100 billion tokens) In this final stage, the learning rate decays linearly to zero. In this phase, the data mix was optimized to be of even higher quality, in order to polish the model's performance, and long context data sources were upsampled to increase the model context window to 32k tokens.

### Post-training

During post-training, we adapt EuroLLM to be an instruction-following model capable of handling multi-turn conversations. We start by regenerating the final responses from publicly available datasets using several open models, and keep the best candidate using a reward model. To this data, we add records from other datasets (Nemotron, Hermes-3 and Tulu 3), removing duplicates based on the first prompt. This pipeline shows how EuroLLM can be easily adapted for your use-cases.

The model excels at translation tasks being capable of translating across all official EU languages, matching or outperforming strong models like Gemma-3-27B, Qwen-3-32B and Apertus-70B. Furthermore, when it comes to general benchmarks, it is the best EU-made fully open model.

## Run the model

```
from transformers import AutoModelForCausalLM, AutoTokenizer

model_id = "utter-project/EuroLLM-22B-Instruct-2512"
tokenizer = AutoTokenizer.from_pretrained(model_id)
model = AutoModelForCausalLM.from_pretrained(model_id)

messages = [
    {
        "role": "system",
        "content": "You are EuroLLM --- an AI assistant specialized in European languages that provides safe, educational and helpful answers.",
    },
    {
        "role": "user", "content": "What is the capital of Portugal? How would you describe it?"
    },
    ]

inputs = tokenizer.apply_chat_template(messages, tokenize=True, add_generation_prompt=True, return_tensors="pt")
outputs = model.generate(inputs, max_new_tokens=1024)
print(tokenizer.decode(outputs[0], skip_special_tokens=True))
```

## Results

### Multilingual

[![EuroLLM 22B Blog Results - Multilingual](https://cdn-uploads.huggingface.co/production/uploads/674d9994d8897d7d36dfc9e9/MpE3MUksAaTf1vQ2OewOa.png)](https://cdn-uploads.huggingface.co/production/uploads/674d9994d8897d7d36dfc9e9/MpE3MUksAaTf1vQ2OewOa.png)

**Table 1:** Comparison of fully open and open-weight LLMs on a suite of multilingual benchmarks, averaging over all languages supported by EuroLLM-22B that are present in each benchmark. The table reports scores on HellaSwag, MMLU, MMLU-Pro, ARC-Challenge, MGSM, FLORES, and WMT24++. The Borda Count (Colombo et al., 2022) reflects the average ranking of each model across all benchmarks. **Bold** values indicate the best overall system for each benchmark, while underscored values denote the best fully open system.

### English

[![EuroLLM 22B Blog Results - English](https://cdn-uploads.huggingface.co/production/uploads/674d9994d8897d7d36dfc9e9/Nk9QQIuz5C9UT0T9xAUo7.png)](https://cdn-uploads.huggingface.co/production/uploads/674d9994d8897d7d36dfc9e9/Nk9QQIuz5C9UT0T9xAUo7.png)

**Table 2:** Comparison of fully open and open-weight LLMs on a suite of English benchmarks. The table reports scores on IFEval, HellaSwag, MMLU, MMLU-Pro, BBH, ARC-Challenge, GPQA, GSM8K, MATH-500, and HumanEval. The Borda Count reflects the average ranking of each model across all benchmarks. **Bold** values indicate the best overall system for each benchmark, while underscored values denote the best fully open system.

## Bias, Risks, and Limitations

EuroLLM-22B has not been aligned to human preferences, so the model may generate problematic outputs (e.g., hallucinations, harmful content, or false statements).

Downloads last month

220

Safetensors

Model size

23B params

Tensor type

BF16

·

## Model tree for utter-project/EuroLLM-22B-Instruct-2512

Base model

[utter-project/EuroLLM-22B-2512](https://huggingface.co/utter-project/EuroLLM-22B-2512)

Finetuned

([1](https://huggingface.co/models?other=base_model:finetune:utter-project/EuroLLM-22B-2512))

this model

Finetunes

[1 model](https://huggingface.co/models?other=base_model:finetune:utter-project/EuroLLM-22B-Instruct-2512)

Quantizations

[8 models](https://huggingface.co/models?other=base_model:quantized:utter-project/EuroLLM-22B-Instruct-2512)