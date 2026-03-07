---
title: "LLM based TTS models"
source: "https://huggingface.co/blog/YatharthS/llm-tts-models"
author:
published: 2025-12-19
created: 2025-12-21
description: "A Blog post by Yatharth Sharma on Hugging Face"
tags:
  - "clippings"
---
[Back to Articles](https://huggingface.co/blog)

[Community Article](https://huggingface.co/blog/community) Published December 18, 2025

[Yatharth Sharma](https://huggingface.co/YatharthS)

[YatharthS](https://huggingface.co/YatharthS)

TTS models are increasingly gaining popularity has placed greater importance in their **architectures**. For a long time, TTS models have used **complicated** architectures that are specific for their tasks.

However, recent models such as Orpheus, Spark-TTS, Cosyvoice, Kimi-Audio, 2cent-TTS have shown you can use just use simple **two** part system composed of an LLM and neural codec to infact not only perform high quality TTS but perform various other tasks such as ASR while maintaining excellant quality and scalability!

## How they work

The architecture is infact relatively simple, composed of **two** main parts:

- A neural codec/audio tokenizer to convert audio into **discrete tokens** and decode discrete tokens back into audio.
- A LLM to generate discrete audio tokens from text sequentially, reference discrete audio tokens, instructions, and more. The architecture can be roughly visualized below:

<video controls="controls" src="https://cdn-uploads.huggingface.co/production/uploads/69179c7706b20ca67e59b036/yrV1KePwQRcYTVonmd-MK.mp4"></video>

We will go more in depth below.

## 1\. Neural Codec

There are several **hundreds types** of Neural Codecs so we will only talk about the popular ones. The job of the Neural Codec is simple: compress audio into **discrete tokens**. However, they have several different characteristics as stated below:

1. **Amount of tokens per second**: meaning how much discrete audio tokens represent **1** second of audio. Lower tokens per second will significantly increase the speed of TTS models. XCodec2 from Llasa does **50 t/s**, Snac from Orpheus does **83 t/s**, Cosyvoice's codec does **25 t/s**.
2. **Codebook amount**: Some codecs encode audio into groups of discrete tokens. Most LLM based TTS models either utilize a custom architecture to predict them parallelly(Zonos) or simple concat the codebooks(Orpheus). Usually however, most LLM based TTS models use single codebook codecs as they are usually more efficient for them.
3. **Diffusion/Single pass**: Some codecs are diffusion based such(VibeVoice, Chatterbox, CosyVoice codecs) which are slow as they iteratively refine their output compared to single pass codecs(Orpheus, Spark-TTS, Zonos codecs) which are much **faster** but usually compress less and/or are **inferior** in terms of quality.
4. **Codebook Size**: How many tokens can represent the audio; XCodec's from Llasa codebook size is **65536** tokens while Snac from Orpheus has only **8192** possible tokens. Smaller codebook sizes are better as they speedup training usually.
5. **Sampling rate**: Codecs encode at different sampling rates such as Snac from orpheus operates at 24khz audio while DAC from Zonos operate at a 44.1khz sampling rate. Higher sampling rates results in clearer audio but usually more tokens per second.

The most well known codecs and their characteristics are stated below:

- XCodec2: A single codebook codec that encodes 16khz audio at a rate of 50 tokens per second. Codebook size is 65536 used by [Llasa](https://huggingface.co/HKUSTAudio/Llasa-1B) and [T5GemmaTTS](https://huggingface.co/Aratako/T5Gemma-TTS-2b-2b).
- DAC: 8 codebook codec that encodes 44.1khz audio at a total of 774 tokens per second. Codebook size is 1024 per codebook used by [Zonos](https://huggingface.co/Zyphra/Zonos-v0.1-transformer) and [Parler-TTS](https://huggingface.co/parler-tts/parler-tts-large-v1).
- Cosyvoice's decoder: A single codebook diffusion based codebook that encodes 24khz audio at a rate of 25 tokens per second. Codebook size is 8192 and used by [CosyVoice](https://huggingface.co/FunAudioLLM/CosyVoice2-0.5B), [GLM-TTS](https://huggingface.co/zai-org/GLM-TTS), [Chatterbox](https://huggingface.co/ResembleAI/chatterbox), and [Qwen-Omni](https://huggingface.co/Qwen/Qwen3-Omni-30B-A3B-Instruct).

## 2\. The Large Language Model (LLM)

We have learned about neural codecs, but they simply convert audio into discrete tokens and decode them back into audio. The LLM is the part that truly generates the speech from text and optionally instructions.

However, LLMs are usually used for language as the name suggests, so how can they process these audio tokens and generate them as well?

#### Audio as Language

Essentially, audio is treated as a new "language" which allows LLMs to do variety of tasks without change in architecture! This is usually done by the following steps:

1. Expand the LLM's vocabulary with the audio tokens
2. The model is trained to predict the next audio token given text tokens or reference audio tokens, very similar to a normal LLM.

Yes, it's that simple! Treating Audio as a new "language" also has benefits such as supporting voice cloning without a custom architecture. You simply need to provide prefix audio tokens and their corresponding transcription!

#### Some other advantages of using LLMS:

- **Scalability**: LLM's are already heavily optimized using many techniques(kv-cache, quantization, kernels, etc.) and libraries(vllm, lmdeploy, sglang, etc.). They are incredibly efficient at batching leading to fast large scale generation and training.
- **Multimodality**: A single model can perform TTS, Automatic Speech Recognition, and Speech-to-Speech translation by simply adjusting training data, no need to change architectures.
- **Simplification**: They eliminate the need for phonemes and other complicated processing required in previous generations of TTS.

## Final Notes

Thanks for reading this article and yes, the architecture is that simple once again! I will be later explaining other various modern architectures such as Diffusion based or Hybrid TTS models and how they have their own pros and cons.

Once again, thanks for reading, hope you have a good day!