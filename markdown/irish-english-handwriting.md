# Building a bilingual Irish-English mathematical handwriting recognition system

A viable pipeline exists combining Qwen-VL models for recognition, ColPali for retrieval, and the Schools' Collection's **740,000 transcribed pages** as training data. The most promising approach uses **Unsloth-finetuned Qwen2.5-VL-7B** for multi-task recognition (achieving state-of-the-art handwriting accuracy), deployable locally on M4 Max via MLX-VLM at ~5GB memory with 4-bit quantization. Irish HTR research has advanced significantly through the Decoding Hidden Heritages project, achieving **<5% character error rates** on scribe-specific models—techniques directly applicable to the Schools' Collection.

## Acquiring data from duchas.ie requires the official JSON API

The Schools' Collection at duchas.ie contains **1,128 volumes spanning approximately 740,000 manuscript pages** compiled between 1937-1939. Rather than scraping, duchas.ie provides a documented **JSON API (v0.6)** that requires authentication. To obtain an API key, contact eolas@duchas.ie explaining your research purpose.

The API structure follows a hierarchical pattern: Volumes → Parts (Schools) → Items (Stories) → Pages. Key endpoints include `/api/v0.6/cbes/volumes` for volume enumeration and `/api/v0.6/cbes/?VolumeNumber={num}` for page-level data. High-resolution images are served from a separate CDN at `doras.gaois.ie` following this URL pattern:

```
https://doras.gaois.ie/cbe/CBE_{VOLUME}%2FCBE_{VOLUME}_{PAGE}.jpg?format=jpg&quality=100
```

**Crawl4ai** can supplement API access for verification and screenshot capture, but the API approach is both more efficient and ethically appropriate for this UNESCO Memory of the World collection. Implement respectful rate limiting of 1-2 requests per second with exponential backoff. Critical metadata available includes school location (linked to logainm.ie placenames), collector/informant details, topic classifications, and—most valuably—**crowdsourced transcriptions from Meitheal Dúchas covering approximately 400,000 pages**. These paired image-transcription records constitute ready-made training data.

## Qwen-VL models lead handwriting recognition accuracy

Among current vision-language models, **Qwen3-VL achieves the best handwriting recognition** according to 2024-2025 benchmarks, with particular strength in messy handwriting and nuanced character recognition. The model family spans 2B to 235B parameters, with the 7B-8B sweet spot balancing capability and deployability.

| Model | Parameters | HTR Capability | Math Support | Irish Likelihood | Deployment |
|-------|-----------|----------------|--------------|------------------|------------|
| **Qwen3-VL** | 2B-235B | Best overall | Very good | High (32 languages) | Local possible |
| GOT-OCR2.0 | 580M | Good (trained on IAM) | LaTeX/TikZ | Unknown | Edge-ready |
| DeepSeek-OCR | 3B (570M active) | Adequate | Good | ~100 languages | A100 optimal |
| Docling/Granite | 258M | Limited | Good | English focus | Laptop-friendly |

**GOT-OCR2.0** deserves special attention for resource-constrained scenarios—at just **580M parameters**, it was explicitly trained on handwriting datasets including IAM (English) and NorHand (Norwegian), outputs LaTeX for equations, and supports deployment via ONNX, llama.cpp, and MNN for edge devices. The model achieves edit distances of 0.035 (English) and 0.038 (Chinese) on document benchmarks.

For mathematical expression recognition, **DeepSeek-Math 7B** excels at understanding mathematical content (51.7% on MATH benchmark) rather than just transcribing it, though it operates via vision-language fusion rather than direct handwritten equation recognition. The recently released **Uni-MuMER** (fine-tuned Qwen-2.5-VL-3B) achieves **79.74% exact match** on CROHME benchmarks—a 16% improvement over previous state-of-the-art—demonstrating the power of fine-tuning general VLMs on mathematical data.

No VLM explicitly claims Irish language support, but Qwen's 32-language OCR capability covering European Latin-script languages suggests reasonable handling of síneadh fada (á, é, í, ó, ú) diacritics. Fine-tuning on Irish data will be essential.

## ColPali enables visual document retrieval without OCR

ColPali represents a paradigm shift by directly embedding document page images via **late interaction mechanisms**, bypassing traditional OCR pipelines entirely. The architecture combines Google's PaliGemma-3B with ColBERT-style multi-vector representations, producing ~1024 vectors × 128 dimensions per page.

Late interaction computes similarity through **MaxSim scoring**: for each query token, find the document patch with highest cosine similarity, then sum across all query tokens. This enables rich token-to-patch interaction while maintaining pre-computed document embeddings for efficient retrieval.

ColPali is fundamentally a **retrieval model, not a recognition model**. It excels at retrieving handwritten documents by semantic query ("find pages about fairy legends in Donegal") and can visualize which image patches match specific query terms. However, it outputs embeddings rather than text transcriptions. The optimal architecture combines:

- **ColPali/ColQwen** for document retrieval and page-level search
- **TrOCR or fine-tuned Qwen-VL** for actual text transcription
- **ByT5** for post-OCR error correction

**ColQwen2 v1.0** (based on Qwen2-VL-2B) offers the best licensing terms (Apache 2.0) with dynamic resolution support. Fine-tuning requires query-image pairs rather than transcription pairs, making it suitable for building a search interface over handwritten collections. No public research exists on ColPali specifically fine-tuned for handwriting—an open research opportunity.

## Mathematical expression recognition has reached production quality

Current state-of-the-art achieves **60-80% exact match rates** on standard benchmarks, with commercial solutions like Mathpix providing robust real-world performance across printed and handwritten mathematics.

The primary benchmarks include CROHME (8,836 training samples, 101 symbol classes, standard academic benchmark), HME100K (74,502 training samples, most realistic with camera-captured diverse handwriting), and the new **MathWriting dataset from Google** (230K human + 400K synthetic samples, largest available, includes matrices). For training custom models, **TexTeller 3.0** demonstrates that scale matters—trained on 80M samples, it achieves state-of-the-art across all benchmarks.

For open-source deployment, the best options are:

- **Pix2Text**: Most comprehensive Mathpix alternative, supports 80+ languages, layout analysis, table recognition
- **TexTeller 3.0**: Handwritten math support, trained on massive data
- **UniMERNet**: Best on diverse formula complexity via Length-Aware Module
- **pix2tex (LaTeX-OCR)**: Lightweight ViT-based solution for simple formulas

**Critical gap**: Phase portraits, vector fields, and nonlinear system diagrams have no dedicated recognition systems. GPT-4V provides basic figure understanding but not specialized mathematical diagram recognition—this remains under-researched.

## Irish HTR achieved <5% error rates through scribe-specific models

The **Decoding Hidden Heritages** project (DCU, Edinburgh, UCD) successfully developed Irish HTR using Transkribus, achieving character error rates of **2.17% (Liam Mac Coisdealbha) to 4.39% (Seosamh Ó Dálaigh)** on National Folklore Collection manuscripts. The key finding: **individual models per scribe outperform general models** due to style, layout, and orthographic variation.

The Schools' Collection presents unique challenges: children's handwriting, mix of Irish and English, regional dialectal spellings, and pre-standardization orthography. However, the **400,000+ crowdsourced transcriptions** from Meitheal Dúchas provide exceptional ground truth for HTR training.

For handling síneadh fada, training on properly annotated Irish data is essential—general OCR engines frequently confuse accented and unaccented vowels. Post-processing with **An Caighdeánaitheoir** (github.com/kscanne/caighdean) can normalize dialectal spellings to standard Irish.

Cross-language transfer from the public **Scottish Gaelic 1949-1979 Transkribus model** (trained on 2,500 pages) offers a potential starting point, given the linguistic similarities. The DHH project's models, though scribe-specific, provide transfer learning candidates for 20th-century Irish handwriting.

## Unsloth enables efficient VLM fine-tuning with 70% VRAM savings

**Unsloth** supports direct fine-tuning of vision-language models including Qwen2.5-VL, Qwen3-VL, Llama 3.2 Vision, and Gemma 3 Vision. Critically, Unsloth provides a **dedicated notebook for handwriting-to-LaTeX** using Qwen2.5-VL, directly applicable to this use case.

Fine-tuning requires modest data: **300-500 samples** for meaningful improvement (2x accuracy on benchmarks), **1,000-2,000 samples** for robust single-task models, and **5,000-10,000 samples** for production-quality multi-language systems. With the Schools' Collection offering 400,000+ paired samples, data quantity is not the limiting factor.

Multi-task learning (Irish + English + Math) is feasible based on Google's research showing a single LSTM can support **102 languages** with 20-40% error reduction, and UCL-MHTR achieving state-of-the-art across English, Italian, and Russian with unified continual learning. Structure the dataset with task-specific indicators:

```python
system_prompt = """You are a handwriting recognition expert capable of:
1. Transcribing Irish (Gaeilge) handwritten text with síneadh fada
2. Transcribing English handwritten text  
3. Converting mathematical equations to LaTeX
Identify the content type and provide accurate transcription."""
```

Data augmentation strategies include geometric transformations (rotation ±5-15°, scaling, shearing), elastic distortion for simulating pen pressure variation, and GAN-based synthesis using HiGAN+ or FW-GAN (reducing CER by 3-60% per Spoto et al.). For synthetic math data, the CS-433/ml-project-2-radioactiv GitHub repository provides tools for generating handwritten math exercises with LaTeX ground truth.

## Local deployment on M4 Max uses MLX-VLM with 4-bit quantization

**MLX-VLM** provides native Apple Silicon inference for Qwen2-VL, Qwen2.5-VL, Gemma 3, and Llama 3.2 Vision models:

```python
from mlx_vlm import load, generate
model_path = "mlx-community/Qwen2-VL-7B-Instruct-4bit"
model, processor = load(model_path)
output = generate(model, processor, "Transcribe this handwritten text", ["image.jpg"])
```

Memory requirements on M4 Max (36GB+ unified memory):
- Qwen2-VL-7B 4-bit: ~5GB
- Qwen2.5-VL-32B 4-bit: ~18GB

**llama.cpp** added VLM support in May 2025 via libmtmd. VLMs require two files: main GGUF weights and mmproj (multimodal projector). Simon Willison's testing confirms Qwen2-VL has good OCR/handwriting performance through llama.cpp.

For cloud training, **Modal** provides simple Python-native GPU access with $30/month free tier. An A100-40GB processes ~5,000 samples in 4 hours for approximately $6. Training configuration:

```python
@modal.function(gpu="A100", timeout=3600)
def train_vlm(dataset_path: str):
    from unsloth import FastVisionModel
    model, tokenizer = FastVisionModel.from_pretrained(
        "Qwen/Qwen2.5-VL-7B-Instruct",
        load_in_4bit=True,
        finetune_vision_layers=True
    )
```

**BAML** integrates for structured extraction post-recognition, providing type-safe schema-aligned parsing with retry policies and VSCode playground for prompt testing.

## Dataset creation requires PAGE XML with JSON conversion for training

The recommended annotation workflow uses **PAGE XML** (Transkribus standard) for human annotation, then converts to JSON for VLM training:

```json
{
  "id": "cbes_0358_0585",
  "image_path": "images/CBE_0358_0585.jpg",
  "transcription": "Bhí fear ann fadó...",
  "language": "irish",
  "school": "Scoil Mhuire, Baile Átha Cliath",
  "collector": "Seán Ó Murchú",
  "volume": 358,
  "page": 585
}
```

Quality control strategies include CER sampling (manually verify 5-10% of annotations), consensus transcription with multiple annotators, and automated validation checking character sets, length ratios, and language detection. **HTRflow** from Riksarkivet provides modern Python tooling supporting ALTO XML, PAGE XML, and JSON export with built-in evaluation pipelines.

## Conclusion

The recommended architecture combines multiple specialized components: **ColQwen2** for semantic retrieval over the handwritten collection, **fine-tuned Qwen2.5-VL-7B** for transcription (trained via Unsloth on Schools' Collection data), **TexTeller 3.0 or Pix2Text** for mathematical expressions, and **An Caighdeánaitheoir** for Irish text normalization.

Training requires approximately 1,000-2,000 samples per task for production quality—easily achievable given the 400,000+ existing transcriptions. Deploy locally via MLX-VLM on M4 Max (~5GB at 4-bit for the 7B model) or via Modal for batch processing (4 hours, ~$6 for 5,000 samples on A100).

Key open research opportunities include: ColPali fine-tuned for handwriting retrieval, mathematical diagram recognition (phase portraits, vector fields), and unified Irish-English-Math multi-task models. The infrastructure exists; the primary effort is curating training data from the Schools' Collection API and fine-tuning existing foundation models rather than training from scratch.