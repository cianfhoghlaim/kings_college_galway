---
title: "BritLLM"
source: "https://llm.org.uk/"
author:
published:
created: 2025-12-16
description:
tags:
  - "clippings"
---
## What and why is BritLLM?

LLMs have become a key component of any advanced natural language processing system. Recently, we have seen an explosion in public and commercial interest in applying these technologies in academia, government, and industry. This technology promises to fundamentally change how humans interact with computers and enable large-scale automation of any task which involves the generation and processing of text or speech.

This arguably makes LLMs a technology of key national importance and one that is likely to grow over the coming years. However, as it stands there is no publicly available LLM or exploration thereof to serve key interests in the UK.

We believe it is essential for UK academia, government, and industry that there is both the know-how and models available freely to serve them. UK society, as any society, is unique and thus has unique needs. For example, the differences between say US and UK law, finance, health, etc. means that LLMs developed solely using US data is unlikely to be sufficiently aligned with UK needs. The same goes for the rich set of languages spoken across the British Isles: English, Scots, Welsh, Scottish Gaelic, Irish, etc.

To this end, we have launched the BritLLM project, an ongoing effort to produce training data, evaluation data, know-how, and freely available models aligned with UK interests. Our goal is to release a series of competitive LLM models, while empirically quantifying the suitability of commercial and non-commercial models made available by other parties.

The effort is led by the [University College London NLP group](https://nlp.cs.ucl.ac.uk/), which has a track record of groundbreaking research in the intersection between NLP and machine learning. They regularly publish their research at the leading venues in their field (ACL, EMNLP, NeurIPS, ICLR, etc.) and have received numerous best and outstanding paper awards.

## Releases

### BritLLM v0.1: Caernarfon 3B

[Available on Hugging Face](https://huggingface.co/britllm/britllm-3b-v0.1) under the [Open Data Commons Attribution License (ODC-By) v1.0](https://opendatacommons.org/licenses/by/1-0) license.

| Model | Mean | ARC-c | HellaSwag | MMLU | TruthfulQA | Winogrande |
| --- | --- | --- | --- | --- | --- | --- |
| OpenLLaMA 7B v2 | 52.4 | 43.7 | 72.2 | 41.3 | 35.5 | 69.4 |
| MPT 7B | 51.3 | 47.4 | 77.7 | 26.8 | 33.3 | 71.1 |
| **Caernarfon 3B** | 50.7 | 43.6 | 69.6 | 37.6 | 37.8 | 65.1 |
| Open LLaMA 7B | 50.5 | 47.0 | 72.0 | 30.5 | 34.9 | 68.0 |
| Open LLaMA 3B v2 | 48.2 | 40.3 | 71.6 | 27.1 | 34.8 | 67.0 |
| OPT 6.7B | 46.6 | 38.2 | 68.9 | 24.8 | 35.1 | 65.8 |
| Open LLaMA 3B | 45.8 | 39.9 | 62.7 | 26.9 | 35.0 | 64.7 |
| TinyLLaMA v1.1 | 44.6 | 37.1 | 62.3 | 26.8 | 35.1 | 61.6 |
| OPT 2.7B | 44.4 | 34.9 | 61.3 | 25.4 | 37.6 | 62.7 |
| Pythia 2.8B | 43.7 | 36.2 | 60.7 | 26.8 | 35.9 | 59.0 |
| TinyLLaMA v1.0 | 43.2 | 34.2 | 60.0 | 25.5 | 37.6 | 58.7 |

On the English [OpenLLM benchmark](https://huggingface.co/open-llm-leaderboard), Caernarfon 3B performs better than open models of the same size and even outperforms several larger open models.

| Model | Mean | ARC-e | PIQA | XNLI |
| --- | --- | --- | --- | --- |
| **Caernarfon 3B** | 44.3 | 36.5 | 55.2 | 41.3 |
| LLaMA 3 8B | 43.0 | 34.0 | 54.1 | 41.0 |
| Bloom 7B | 39.2 | 26.5 | 51.5 | 39.5 |
| mGPT 13B | 38.6 | 26.0 | 52.5 | 37.2 |
| Mistral 7B | 38.1 | 27.5 | 53.7 | 33.1 |
| Phi 2 | 37.8 | 27.1 | 51.6 | 34.8 |

For BritEval: Irish, Caernarfon 3B performs better than a number of comparable models, albeit being smaller in size.

| Model | Mean | ARC-e | PIQA | XNLI |
| --- | --- | --- | --- | --- |
| **Caernarfon 3B** | 46.2 | 40.8 | 60.1 | 37.7 |
| LLaMA 3 8B | 43.6 | 40.2 | 55.0 | 35.5 |
| mGPT 13B | 38.4 | 27.0 | 53.6 | 34.6 |
| Phi 2 | 38.3 | 29.1 | 52.6 | 33.2 |
| Mistral 7B | 38.2 | 28.4 | 52.3 | 33.9 |
| Bloom 7B | 38.1 | 26.4 | 53.2 | 34.7 |

For BritEval: Welsh, the results largely mirrors those for BritEval: Irish.

| Model | Mean | ARC-e | PIQA | XNLI |
| --- | --- | --- | --- | --- |
| Mistral 7B | 63.0 | 67.2 | 72.5 | 49.2 |
| LLaMA 3 8B | 62.7 | 70.3 | 72.8 | 44.9 |
| **Caernarfon 3B** | 55.2 | 56.7 | 65.0 | 43.9 |
| Phi 2 | 54.5 | 57.3 | 66.3 | 39.8 |
| Bloom 7B | 52.2 | 51.4 | 63.4 | 41.9 |
| mGPT 13B | 45.6 | 43.0 | 58.4 | 35.5 |

For BritEval: Scots, larger comparable models that were mainly trained using English data outperform Caernarfon 3B, possibly due to the larger proximity to English of Scots compared to other languages covered by BritEval.

| Model | Mean | ARC-e | PIQA | XNLI |
| --- | --- | --- | --- | --- |
| **Caernarfon 3B** | 42.1 | 32.0 | 55.9 | 38.3 |
| LLaMA 3 8B | 41.2 | 30.8 | 54.2 | 38.7 |
| Bloom 7B | 39.1 | 28.1 | 52.7 | 36.6 |
| Phi 2 | 38.9 | 28.0 | 51.9 | 36.9 |
| mGPT 13B | 38.8 | 29.0 | 52.2 | 35.3 |
| Mistral 7B | 37.3 | 27.5 | 52.2 | 32.2 |

For BritEval: Scottish Gaelic, the results largely mirrors those for BritEval: Irish and BritEval: Welsh.

## Pretraining data

Our most recently released model (Caernarfon 3B) is pretrained on over 1.4 trillion tokens.

### English pretraining data

For English pretraining, we use SlimPajama (627B) as our base pretraining set, which contains a diverse data sources such as Wikipedia, web-crawled data (Common Crawl), academic papers (arXiv), and source code. Inspired by recent research showing the power of synthetic data, we also transform a subset of SlimPajama into various formats such as QA (30B) and Multiple Choice (10B) using an open-source model (Mistral 7B). Initially, the pretraining process only uses the SlimPajama subset, only introducing the latter subset once the model has been trained on one trillion tokens.

### Pretraining data for British Languages

Unlike for English, collecting a large amount of high-quality, clean data is a difficult task for smaller and minority languages. Thus, we employ two pretraining data curation strategies:

1. As high-quality data, we use the full article text from Wikipedia for Irish, Welsh, Scots, and Scottish Gaelic.
2. As cross-lingual parallel data, we use parallel data from NLLB's open-license subsets for [Welsh-English](https://opus.nlpl.eu/NLLB/en&cy/v1/NLLB), [Irish-English](https://opus.nlpl.eu/NLLB/en&ga/v1/NLLB), [Scottish Gaelic-English](https://opus.nlpl.eu/NLLB/en&gd/v1/NLLB), and [Scots-English](https://opus.nlpl.eu/NLLB/en&sco/v1/NLLB) to allow the model to more easily bridge its English knowledge to other languages.

To encourage in-context learning behaviour for the alignment portion of training, we also adapt the parallel data into an in-context learning format.

After curation, we have approximately one billion unique tokens and ten billions tokens in an in-context learning format, which is ~1.5% compared to the total size of the English-only pretraining data. Akin to the English synthetic data subset, the British language subset is introduced at the later stage of pretraining.

## Evaluation data

### BritEval

BritEval aims to be a benchmark datasets covering all the languages of the British Isles. Currently, it covers [Scots](https://en.wikipedia.org/wiki/Scots_language), [Irish](https://en.wikipedia.org/wiki/Irish_language), [Welsh](https://en.wikipedia.org/wiki/Welsh_language), and [Scottish Gaelic](https://en.wikipedia.org/wiki/Scottish_Gaelic), and includes a number of domains: grade-school level multiple-choice science questions, physical commonsense reasoning, and natural language inference.

Concretely, for the current version we have applied state-of-the-art machine translation and quality filtering to convert the [AI2 Reasoning Challenge](https://huggingface.co/datasets/allenai/ai2_arc), [Physical Interaction: Question Answering](https://huggingface.co/datasets/ybisk/piqa), [Cross-lingual Natural Language Inference](https://github.com/facebookresearch/XNLI) datasets into the above British languages and are working to add more languages and domains for future releases and work with local communities to certify the quality of the data.

## Contact

[contact@llm.org.uk](https://llm.org.uk/)

## Members (in alphabetical order)

- [David Ifeoluwa Adelani](https://dadelani.github.io/)
- [Xuanli He](https://xlhex.github.io/)
- [Yao Lu](https://yaolu.github.io/)
- [Pontus Stenetorp](https://pontus.stenetorp.se/)
- [Andrzej Szablewski](https://github.com/TheRootOf3)
- [Jiayi Wang](https://www.linkedin.com/in/jiayi-wang-0a999847)

## Acknowledgements

We would like to acknowledge the support of [DiRAC (Distributed Research using Advanced Computing)](https://dirac.ac.uk/), [Microsoft Research's Accelerate Foundation Models Research Grant](https://www.microsoft.com/en-us/research/collaboration/accelerating-foundation-models-research), the [UCL Centre for Artificial Intelligence](https://www.ucl.ac.uk/ai-centre), and the [Generative Models AI Hub](https://www.genai.ac.uk/).