---
title: "Train a tiny model to generate 3D files (v2) through example diversification"
source: "https://starmind.comfyspace.tech/experiments/cadmonkey-v2/"
author:
published: 2025-12-19
created: 2025-12-19
description:
tags:
  - "clippings"
---
![Cadmonkey](https://starmind.comfyspace.tech/cadmonkey-v2/thumbnail.png)

---

## Background

Hi, I’m Thomas, a Mechanical/Robotics Engineer turned Software & ML.

![Engineer](https://starmind.comfyspace.tech/cadmonkey-v2/engineer.png)

This is an experiment to create 3D models using a Small Language Model.

Armed with GPU credits from CloudRift & Prime Intellect + the generous free usage of Huggingface, I set out to build a language model to generate 3d files - CADMonkey.

![Dataset snippet](https://starmind.comfyspace.tech/cad-monkey/data-snippet.png)

---

## 1\. Model architecture & 3d Programming Language

At Starmind, we need our models to be small enough to run on Raspberry Pi 4 & 5. The base Language Model of choice is Gemma3-1B for the following reasons:

- It’s easy to finetune (aka not having to spend 10+ hours debugging code)
- No chat template (ease of development and use)
- Tools around training (Unsloth), deployment and quantization (llama.cpp) are matured

![Gemma3](https://starmind.comfyspace.tech/cad-monkey/gemma3.png)

We also briefly considered a diffusion model, but the development complexity is too large. Perhaps we will revisit this idea another day.

The model will generate OPENSCAD code that will be used to render 3D models.

![ide](https://starmind.comfyspace.tech/cadmonkey-v2/openscad.png)

Why OpenSCAD? As a Mechanical Engineer, I find that traditional Voxel & Mesh-based 3D models to bring little to no value. Engineering requires constant revisions and iterations, and shape-based and code-based models are ideal for that.

---

## 2\. Dataset generation

Your model is only as good as its dataset. But the definiton of “good” is relative to the task at hand.

Here are the attempts we made at creating a dataset:

#1: There are 7,000 rows worth of open-source OpenSCAD dataset, scrapped from Thingiverse on Huggingface (redcathode/thingiverse-openscad). However, we have a few issues with this:

- The coding structure is overly diverse and the code is not well written. This caused the model after training to produce in-coherent code (a mix of Python & C).
- The objects present in the dataset are not common objects, instead they are “a specific gear for a specific thing” type. This fails to teach the model the semantic meaning of the code.

![gear](https://starmind.comfyspace.tech/cad-monkey/gear.png)

#2: Synthetic data generation is the method we chose to go with.

- First, we created a list of common object names by categories (animals, kitchen appliances, pokemon, etc). Then, ask a Large Language Model (Kimi) to generate the code, renders the code, and use a VLM (Qwen2.5-VL) to judge the output for ressemblance

The result is the following dataset: [https://kdataset.web.app](https://kdataset.web.app/)

This is the first synthetically generated & reviewed OpenSCAD dataset of large quantity on the internet: ThomasTheMaker/Synthetic-Object-v3 (35k rows of verified data).

![dataset](https://starmind.comfyspace.tech/cad-monkey/kdataset.png)

This would not be possible without the grants provided by CloudRift. Thanks a bunch!

After fine-tuning the model on the dataset, we found that:

- The model generates working OpenSCAD code 80% of the time
- However, the code does not match the object.

![v1](https://starmind.comfyspace.tech/cadmonkey-v2/v1.png)

As the matter of fact, only 1/400 models matched the object. See below for the only good object created - the duck:

![duck](https://starmind.comfyspace.tech/cadmonkey-v2/duck.png)

#3: Scaling dataset horizontally

We tried scaling up with a dataset with more objects, but the same issue of non-matching objects persisted.

#4: Scaling up dataset vertically

Model performance only truly improved when we scaled up the dataset vertically:

- Using the same number of objects
- Scale up the number of examples of each object
- Scale up the diversity of teacher models used to generate the datasets

You can see the improvement below:

![cat](https://starmind.comfyspace.tech/cadmonkey-v2/cat.png) ![dog](https://starmind.comfyspace.tech/cadmonkey-v2/dog.png) ![chicken](https://starmind.comfyspace.tech/cadmonkey-v2/chicken.png) ![universe](https://starmind.comfyspace.tech/cadmonkey-v2/universe.png)

---

## 3\. Mistakes we made along the way

There are many things we tried that did not work, and we hope it helps you avoid wasting time & efforts:

- First, we tried generating data with Claude Sonnet & Haiku models on AWS Bedrock. The cost was estimated to be 40-60$ based on the token count. Due to reasoning tokens, this came out to $170 while the output barely surpasses open models like Kimi-K2 (non-thinking) and Deepseek-V2.
- Second, we tried generating the list of object names through libraries and dictionaries. This was a bad idea as the list was quite random, containing objects that the base model did not even have any knowledge about.

---

## 4\. Training

With the dataset ready, we fine-tuned Gemma3 1B model on the datasets with the following prompt:

‘hey cadmonkey, make me a {object name}’

This was done using Unsloth 4-bit finetuning.![unsloth](https://starmind.comfyspace.tech/cad-monkey/unsloth.png)

The output model is converted into a GGUF model with q8 quantization.

Everything is available here: [https://hf.co/collections/ThomasTheMaker/cadmonkey](https://hf.co/collections/ThomasTheMaker/cadmonkey)

---

## 6\. Make it available to the world!

I’m using Modal to host the model. Since the model is small, it can run very fine even on CPU, Raspberry Pis, etc.

For speed optimization, I’m using T4 GPU on Modal, giving some awesome output speed. Although only 8% of GPU is utilized.

On average, each prompt costs 2 cents to run.

Try out the app at [https://cadmonkey.web.app](https://cadmonkey.web.app/)

![app](https://starmind.comfyspace.tech/cad-monkey/app.jpg)

---

## 6\. The takeaway

I know it’s cliche, but you can just make things!

5 years ago, it would cost me 5 figures and a team of 20 scientists to achieve this.

Now, I ran the whole experiments over 3 weekends, using 500$ in credits from various sources.

Up until now, my knowledge about Language Model was 1-year of obsessive self-taught.

You really can just do things. You just have to be crazy enough to start.

---

The dataset & model files:

- [https://hf.co/collections/ThomasTheMaker/cadmonkey](https://hf.co/collections/ThomasTheMaker/cadmonkey)
- [https://hf.co/ThomasTheMaker](https://hf.co/ThomasTheMaker)

The datasets used in this experiments are named “Synthetic-Openscad-v\*\*”

Dataset generation & training code:

- [https://github.com/ThomasVuNguyen/K](https://github.com/ThomasVuNguyen/K)
- [https://github.com/ThomasVuNguyen/maker-model](https://github.com/ThomasVuNguyen/maker-model)

Awesome compute sponsors (:

- [https://cloudrift.ai](https://cloudrift.ai/)
- [https://primeintellect.ai](https://primeintellect.ai/)

If you have any questions, ask me here: [https://discord.gg/XT23RZ5U6R](https://discord.gg/XT23RZ5U6R)