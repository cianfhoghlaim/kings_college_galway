# GPU Experiment Guide: Reproducing & Improving Celtic Language Models

## Overview

This guide provides practical instructions for reproducing and improving state-of-the-art Celtic language ML models using modern efficient training techniques. Total estimated cost: **~$275** using Unsloth optimizations.

## Key Research Findings

### English-Pivoted Chain-of-Thought Training
**Source:** "Reasoning Transfer for an Extremely Low-Resource and Endangered Language" (Tran et al., UCC 2024)

- **Finding:** Training LLMs to reason in English while understanding/responding in Irish achieves **28.33% improvement** in mathematical reasoning
- **Technique:** Model outputs English reasoning chains but produces Irish final answers
- **Benchmark:** LC2024 - first Irish mathematical reasoning dataset (55 questions from Leaving Certificate)

### Irish-BLiMP Evaluation Results
**Source:** "Irish Benchmark of Linguistic Minimal Pairs"

| Model | Accuracy |
|-------|----------|
| Human Baseline | 90.1% |
| GPT-5 | 73.5% |
| Llama 3 70B | 67.8% |
| Random | 50.0% |

**Insight:** Even state-of-the-art LLMs rely on pattern recognition rather than true grammatical understanding for Irish.

---

## Phase 1: Goidelic Cluster (Irish, Scottish Gaelic, Manx)

### Goal
Recreate and merge the **Qomhrá** (Irish) and **ÈIST** (Gaelic) pipelines, exploiting Goidelic morphological similarities.

### Recommended Base Models
1. **Unsloth/Qwen2.5-14B-Instruct** (Primary)
   - Qomhrá used Qwen architecture
   - Unsloth's Qwen implementation is highly optimized
   - 14B is optimal: reasoning capabilities of larger models, fits single A100

2. **Llama-3.1-8B-Instruct** (Alternative)
   - Strong baseline performance
   - Efficient training with Unsloth

### Required Datasets

#### Irish (ga)
```yaml
pretraining:
  - name: CulturaX-ga
    url: https://huggingface.co/datasets/uonlp/CulturaX
    type: Web crawl
    use: Continued pre-training

instruction_tuning:
  - name: Qomhrá 30K
    url: https://huggingface.co/datasets/Qomhraiche/Qomhra-30K
    size: 30,000 examples
    type: Synthetic instruction pairs
    generation: Qwen-based teacher model

evaluation:
  - name: Irish-BLiMP
    size: 1,020 minimal pairs
    features: 11 linguistic phenomena
  - name: LC2024
    size: 55 questions
    type: Mathematical reasoning
```

#### Scottish Gaelic (gd)
```yaml
corpora:
  - name: ARCOSG
    url: https://github.com/Gaelic-Algorithmic-Research-Group/ARCOSG
    type: POS-tagged corpus

  - name: Corpas na Gàidhlig
    url: https://dasg.ac.uk/corpus/
    size: 70M+ words

audio_data:
  - name: Tobar an Dualchais
    url: https://www.tobarandualchais.co.uk/
    type: Folklore recordings
    use: XLTE (Cross-Lingual Transfer) summaries
```

### Training Configuration

```python
# Goidelic CPT + Instruction Tuning Configuration
from unsloth import FastLanguageModel

model, tokenizer = FastLanguageModel.from_pretrained(
    model_name="unsloth/Qwen2.5-14B-Instruct",
    max_seq_length=4096,
    dtype=None,  # Auto-detect
    load_in_4bit=True,  # QLoRA
)

# LoRA Configuration - Higher rank for morphological complexity
model = FastLanguageModel.get_peft_model(
    model,
    r=128,  # Higher rank for Celtic morphology
    target_modules=[
        "q_proj", "k_proj", "v_proj", "o_proj",
        "gate_proj", "up_proj", "down_proj"
    ],
    lora_alpha=256,
    lora_dropout=0.05,
    bias="none",
    use_gradient_checkpointing="unsloth",
    random_state=42,
)

# Training Arguments
training_args = TrainingArguments(
    per_device_train_batch_size=4,
    gradient_accumulation_steps=8,
    warmup_ratio=0.03,
    num_train_epochs=3,
    learning_rate=2e-5,
    fp16=not torch.cuda.is_bf16_supported(),
    bf16=torch.cuda.is_bf16_supported(),
    optim="adamw_8bit",
    weight_decay=0.01,
    lr_scheduler_type="cosine",
    seed=42,
    output_dir="outputs/goidelic-cpt",
)
```

### English-Pivoted CoT Implementation

```python
# English-Pivoted Chain-of-Thought Training
ENGLISH_COT_TEMPLATE = """
<|system|>
You are a bilingual assistant. When solving problems, think step-by-step in English,
but provide your final answer in Irish (Gaeilge).
</|system|>

<|user|>
{irish_question}
</|user|>

<|assistant|>
Let me solve this step by step:

Step 1: {english_reasoning_step_1}
Step 2: {english_reasoning_step_2}
Step 3: {english_reasoning_step_3}

Freagra (Answer in Irish): {irish_answer}
</|assistant|>
"""

def create_cot_dataset(questions, answers, reasoning_chains):
    """Create English-Pivoted CoT training examples."""
    examples = []
    for q, a, chain in zip(questions, answers, reasoning_chains):
        example = ENGLISH_COT_TEMPLATE.format(
            irish_question=q,
            english_reasoning_step_1=chain[0],
            english_reasoning_step_2=chain[1],
            english_reasoning_step_3=chain[2],
            irish_answer=a
        )
        examples.append(example)
    return examples
```

### Cost Estimate

| Component | GPU | Hours | Rate | Cost |
|-----------|-----|-------|------|------|
| Irish CPT | H100 | 20 | $3.95 | $79 |
| Gaelic CPT | H100 | 15 | $3.95 | $59 |
| **Total Phase 1** | | **35** | | **$138.25** |

---

## Phase 2: Brythonic Cluster (Welsh, Breton, Cornish)

### Goal
Recreate **UK-LLM** (Welsh) and solve Breton/Cornish data scarcity through joint "Brythonic Strategy" training.

### Recommended Base Model
**Unsloth/Llama-3.1-8B-Nemotron**
- UK-LLM uses Nemotron architecture
- Strong foundation for Celtic transfer

### Required Datasets

#### Welsh (cy)
```yaml
corpora:
  - name: CorCenCC
    url: https://corcencc.org/
    size: 13.5M tokens
    features: Written + spoken, POS-tagged

  - name: FreeTxt
    type: User-generated Welsh text

instruction_data:
  - name: Synthetic Welsh Instructions
    method: GPT-4o translation from English
    target_size: 1M+ pairs
```

#### Breton (br) / Cornish (kw)
```yaml
available_data:
  - name: Wikipedia dumps
    languages: [br, kw]

  - name: An Drouizig resources
    url: https://drouizig.org/
    type: Lexicons, tools data

  - name: Korpus Kernewek
    url: https://www.akademikernewek.org.uk/corpus/
    language: kw
```

### Synthetic Data Generation (Welsh Blueprint)

```python
# Generate synthetic instruction pairs for Welsh/Breton/Cornish
from openai import OpenAI

client = OpenAI()

def translate_instruction_pair(english_pair, target_lang):
    """Translate English instruction pair to Celtic language."""

    lang_names = {"cy": "Welsh", "br": "Breton", "kw": "Cornish"}

    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {
                "role": "system",
                "content": f"You are an expert translator. Translate the following "
                           f"instruction-response pair to {lang_names[target_lang]}. "
                           f"Preserve the meaning and natural phrasing."
            },
            {
                "role": "user",
                "content": f"Instruction: {english_pair['instruction']}\n"
                           f"Response: {english_pair['response']}"
            }
        ]
    )
    return response.choices[0].message.content

# Quality filtering with CometKiwi
def filter_translations(translations, threshold=0.7):
    """Filter translations using quality estimation."""
    from comet import download_model, load_from_checkpoint

    model = load_from_checkpoint(download_model("Unbabel/wmt22-cometkiwi-da"))

    filtered = []
    for t in translations:
        score = model.predict([{
            "src": t["english"],
            "mt": t["translation"]
        }])
        if score["scores"][0] >= threshold:
            filtered.append(t)
    return filtered
```

### Joint Fine-Tuning Configuration

```python
# Brythonic Joint Training
# Train on Welsh (anchor) + Breton + Cornish simultaneously

training_config = {
    "model": "unsloth/Llama-3.1-8B-Nemotron",
    "datasets": {
        "welsh": {"weight": 0.6, "source": "CorCenCC + synthetic"},
        "breton": {"weight": 0.25, "source": "Wikipedia + synthetic"},
        "cornish": {"weight": 0.15, "source": "Korpus + synthetic"},
    },
    "strategy": "interleaved",  # Mix languages in batches
    "lora_config": {
        "r": 64,
        "lora_alpha": 128,
        "target_modules": ["q_proj", "k_proj", "v_proj", "o_proj"],
    },
    "training": {
        "epochs": 3,
        "batch_size": 4,
        "gradient_accumulation": 8,
        "learning_rate": 1e-5,
    }
}
```

### Cost Estimate

| Component | GPU | Hours | Rate | Cost |
|-----------|-----|-------|------|------|
| Welsh SFT | A100 80GB | 25 | $2.50 | $62.50 |
| Breton/Cornish Joint | A100 80GB | 15 | $2.50 | $37.50 |
| **Total Phase 2** | | **40** | | **$100.00** |

---

## Phase 3: Evaluation

### Benchmarks to Run

```yaml
irish:
  - name: Irish-BLiMP
    type: Grammatical acceptability
    metrics: [accuracy_per_feature, overall_accuracy]
  - name: LC2024
    type: Mathematical reasoning
    metrics: [accuracy, cot_validity]
  - name: BritEval Irish
    components: [ARC-e, PIQA, XNLI]

scottish_gaelic:
  - name: BritEval Scottish Gaelic
    components: [ARC-e, PIQA, XNLI]
  - name: ARCOSG POS
    type: Part-of-speech tagging
    metrics: [accuracy, f1]

welsh:
  - name: BritEval Welsh
    components: [ARC-e, PIQA, XNLI]
  - name: CorCenCC tasks
    type: Various NLU
```

### Running Evaluations

```python
# Fast inference with Unsloth
from unsloth import FastLanguageModel

model, tokenizer = FastLanguageModel.from_pretrained(
    "outputs/celtic-model",
    max_seq_length=2048,
    dtype=None,
    load_in_4bit=True,
)

# Or export to GGUF for Ollama/llama.cpp
model.save_pretrained_gguf("celtic-model-gguf", tokenizer, quantization_method="q4_k_m")
```

### Cost Estimate

| Component | GPU | Hours | Rate | Cost |
|-----------|-----|-------|------|------|
| Inference/Eval | L4 | 20 | $0.80 | $16.00 |

---

## Total Cost Summary

| Phase | Description | Cost |
|-------|-------------|------|
| Phase 1 | Goidelic (Irish, Scottish Gaelic, Manx) | $138.25 |
| Phase 2 | Brythonic (Welsh, Breton, Cornish) | $100.00 |
| Phase 3 | Evaluation | $16.00 |
| Buffer | Debugging/failures | $21.00 |
| **TOTAL** | | **$275.25** |

### Budget Options

| Configuration | GPU | Cost |
|--------------|-----|------|
| Standard (recommended) | H100 + A100 | ~$275 |
| Budget | A100 40GB only | ~$175 |
| Minimal | A10 (with longer training) | ~$100 |

---

## Recommended Hardware Providers

```yaml
cloud_gpu_providers:
  - name: RunPod
    url: https://runpod.io/
    gpus: [H100, A100, A10, L4]
    notes: Good availability, competitive pricing

  - name: Lambda Labs
    url: https://lambdalabs.com/
    gpus: [H100, A100]
    notes: Research-focused, good support

  - name: Vast.ai
    url: https://vast.ai/
    gpus: [Various]
    notes: Marketplace model, variable pricing

  - name: Google Colab Pro+
    url: https://colab.research.google.com/
    gpus: [A100]
    notes: $50/month, good for prototyping
```

---

## Why Unsloth Changes the Game

1. **VRAM Efficiency:** Train 14B models on single A100 40GB (normally requires A100 80GB)
2. **Speed:** 2-3x faster training via kernel optimizations
3. **Packing:** Removes padding tokens - crucial for sparse Celtic datasets (2-5x effective speedup)
4. **QLoRA Support:** 4-bit quantization with minimal quality loss

```python
# Unsloth setup
pip install unsloth

# Memory comparison (approximate)
# Standard: Qwen2.5-14B needs 2x A100 80GB
# Unsloth:  Qwen2.5-14B fits 1x A100 40GB with 4-bit QLoRA
```

---

## Next Steps

1. **Setup Environment**
   ```bash
   pip install unsloth transformers datasets accelerate
   pip install bitsandbytes  # for 4-bit quantization
   ```

2. **Download Datasets**
   ```python
   from datasets import load_dataset

   # Irish instruction data
   qomhra = load_dataset("Qomhraiche/Qomhra-30K")

   # Scottish Gaelic corpus
   # Download from DASG: https://dasg.ac.uk/
   ```

3. **Rent GPU**
   - Start with H100 for 24 hours for Irish/Gaelic pre-training
   - Switch to A100 for Welsh/Brythonic fine-tuning

4. **Train and Evaluate**
   - Follow configurations above
   - Use BritEval and Irish-BLiMP for benchmarking

5. **Expected Results**
   - Likely match or exceed academic paper results (Qomhrá, UCCIX)
   - Newer base models (Llama 3.1, Qwen 2.5) provide better starting point
   - Total investment: <$300 to reproduce SOTA Celtic language models
