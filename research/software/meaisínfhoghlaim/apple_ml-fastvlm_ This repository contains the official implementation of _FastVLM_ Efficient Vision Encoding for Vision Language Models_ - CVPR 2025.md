---
title: "apple/ml-fastvlm: This repository contains the official implementation of \"FastVLM: Efficient Vision Encoding for Vision Language Models\" - CVPR 2025"
source: "https://github.com/apple/ml-fastvlm?tab=readme-ov-file"
author:
published:
created: 2025-12-15
description:
tags:
  - "clippings"
---
**[ml-fastvlm](https://github.com/apple/ml-fastvlm)** Public

This repository contains the official implementation of "FastVLM: Efficient Vision Encoding for Vision Language Models" - CVPR 2025

[View license](https://github.com/apple/ml-fastvlm/blob/main/LICENSE)

[Code of conduct](https://github.com/apple/ml-fastvlm/blob/main/CODE_OF_CONDUCT.md)

[Contributing](https://github.com/apple/ml-fastvlm/blob/main/CONTRIBUTING.md)

[7.1k stars](https://github.com/apple/ml-fastvlm/stargazers) [517 forks](https://github.com/apple/ml-fastvlm/forks) [65 watching](https://github.com/apple/ml-fastvlm/watchers) [Branches](https://github.com/apple/ml-fastvlm/branches) [Tags](https://github.com/apple/ml-fastvlm/tags) [Activity](https://github.com/apple/ml-fastvlm/activity) [Custom properties](https://github.com/apple/ml-fastvlm/custom-properties)

Public repository

[Open in github.dev](https://github.dev/) [Open in a new github.dev tab](https://github.dev/) [Open in codespace](https://github.com/codespaces/new/apple/ml-fastvlm?resume=1)

This is the official repository of **[FastVLM: Efficient Vision Encoding for Vision Language Models](https://www.arxiv.org/abs/2412.13303). (CVPR 2025)**

[![Accuracy vs latency figure.](https://github.com/apple/ml-fastvlm/raw/main/docs/acc_vs_latency_qwen-2.png)](https://github.com/apple/ml-fastvlm/blob/main/docs/acc_vs_latency_qwen-2.png)

### Highlights

- We introduce FastViTHD, a novel hybrid vision encoder designed to output fewer tokens and significantly reduce encoding time for high-resolution images.
- Our smallest variant outperforms LLaVA-OneVision-0.5B with 85x faster Time-to-First-Token (TTFT) and 3.4x smaller vision encoder.
- Our larger variants using Qwen2-7B LLM outperform recent works like Cambrian-1-8B while using a single image encoder with a 7.9x faster TTFT.
- Demo iOS app to demonstrate the performance of our model on a mobile device.

| [![FastVLM - Counting](https://github.com/apple/ml-fastvlm/raw/main/docs/fastvlm-counting.gif)](https://github.com/apple/ml-fastvlm/blob/main/docs/fastvlm-counting.gif) | [![FastVLM - Handwriting](https://github.com/apple/ml-fastvlm/raw/main/docs/fastvlm-handwriting.gif)](https://github.com/apple/ml-fastvlm/blob/main/docs/fastvlm-handwriting.gif) | [![FastVLM - Emoji](https://github.com/apple/ml-fastvlm/raw/main/docs/fastvlm-emoji.gif)](https://github.com/apple/ml-fastvlm/blob/main/docs/fastvlm-emoji.gif) |
| --- | --- | --- |

## Getting Started

We use LLaVA codebase to train FastVLM variants. In order to train or finetune your own variants, please follow instructions provided in [LLaVA](https://github.com/haotian-liu/LLaVA) codebase. We provide instructions for running inference with our models.

### Setup

```
conda create -n fastvlm python=3.10
conda activate fastvlm
pip install -e .
```

### Model Zoo

For detailed information on various evaluations, please refer to our [paper](https://www.arxiv.org/abs/2412.13303).

| Model | Stage | Pytorch Checkpoint (url) |
| --- | --- | --- |
| FastVLM-0.5B | 2 | [fastvlm\_0.5b\_stage2](https://ml-site.cdn-apple.com/datasets/fastvlm/llava-fastvithd_0.5b_stage2.zip) |
|  | 3 | [fastvlm\_0.5b\_stage3](https://ml-site.cdn-apple.com/datasets/fastvlm/llava-fastvithd_0.5b_stage3.zip) |
| FastVLM-1.5B | 2 | [fastvlm\_1.5b\_stage2](https://ml-site.cdn-apple.com/datasets/fastvlm/llava-fastvithd_1.5b_stage2.zip) |
|  | 3 | [fastvlm\_1.5b\_stage3](https://ml-site.cdn-apple.com/datasets/fastvlm/llava-fastvithd_1.5b_stage3.zip) |
| FastVLM-7B | 2 | [fastvlm\_7b\_stage2](https://ml-site.cdn-apple.com/datasets/fastvlm/llava-fastvithd_7b_stage2.zip) |
|  | 3 | [fastvlm\_7b\_stage3](https://ml-site.cdn-apple.com/datasets/fastvlm/llava-fastvithd_7b_stage3.zip) |

To download all the pretrained checkpoints run the command below (note that this might take some time depending on your connection so might be good to grab ☕️ while you wait).

```
bash get_models.sh   # Files will be downloaded to \`checkpoints\` directory.
```

### Usage Example

To run inference of PyTorch checkpoint, follow the instruction below

```
python predict.py --model-path /path/to/checkpoint-dir \
                  --image-file /path/to/image.png \
                  --prompt "Describe the image."
```

To run inference on Apple Silicon, pytorch checkpoints have to be exported to format suitable for running on Apple Silicon, detailed instructions and code can be found [`model_export`](https://github.com/apple/ml-fastvlm/blob/main/model_export) subfolder. Please see the README there for more details.

For convenience, we provide 3 models that are in Apple Silicon compatible format: [fastvlm\_0.5b\_stage3](https://ml-site.cdn-apple.com/datasets/fastvlm/llava-fastvithd_0.5b_stage3_llm.fp16.zip),[fastvlm\_1.5b\_stage3](https://ml-site.cdn-apple.com/datasets/fastvlm/llava-fastvithd_1.5b_stage3_llm.int8.zip),[fastvlm\_7b\_stage3](https://ml-site.cdn-apple.com/datasets/fastvlm/llava-fastvithd_7b_stage3_llm.int4.zip). We encourage developers to export the model of their choice with the appropriate quantization levels following the instructions in [`model_export`](https://github.com/apple/ml-fastvlm/blob/main/model_export).

To run inference on Apple devices like iPhone, iPad or Mac, see [`app`](https://github.com/apple/ml-fastvlm/blob/main/app) subfolder for more details.

## Citation

If you found this code useful, please cite the following paper:

```
@InProceedings{fastvlm2025,
  author = {Pavan Kumar Anasosalu Vasu, Fartash Faghri, Chun-Liang Li, Cem Koc, Nate True, Albert Antony, Gokul Santhanam, James Gabriel, Peter Grasch, Oncel Tuzel, Hadi Pouransari},
  title = {FastVLM: Efficient Vision Encoding for Vision Language Models},
  booktitle = {Proceedings of the IEEE/CVF Conference on Computer Vision and Pattern Recognition (CVPR)},
  month = {June},
  year = {2025},
}
```

## Acknowledgements

Our codebase is built using multiple opensource contributions, please see [ACKNOWLEDGEMENTS](https://github.com/apple/ml-fastvlm/blob/main/ACKNOWLEDGEMENTS) for more details.

## License

Please check out the repository [LICENSE](https://github.com/apple/ml-fastvlm/blob/main/LICENSE) before using the provided code and [LICENSE\_MODEL](https://github.com/apple/ml-fastvlm/blob/main/LICENSE_MODEL) for the released models.

## Releases

No releases published

## Packages

No packages published  

## Languages

- [Python 81.6%](https://github.com/apple/ml-fastvlm/search?l=python)
- [Swift 17.1%](https://github.com/apple/ml-fastvlm/search?l=swift)
- [Shell 1.2%](https://github.com/apple/ml-fastvlm/search?l=shell)
- Other 0.1%