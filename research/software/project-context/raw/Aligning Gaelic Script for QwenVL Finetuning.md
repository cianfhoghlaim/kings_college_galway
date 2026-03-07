# **Automated Weakly-Supervised Alignment of Historical Gaelic Manuscripts: A Pipeline for Fine-Tuning Qwen2-VL using ColPali and Unsloth**

## **1\. Introduction: The Alignment Challenge in Gaelic Digital Philology**

The digitization of cultural heritage materials has transitioned from a phase of mass imaging to one of mass interpretation. In the context of Gaelic manuscript tradition, which spans centuries of literary production in Ireland and Scotland, vast repositories of scanned documents exist alongside separate, often structurally "loose" textual transcriptions. Ideally, digital libraries would provide pixel-perfect mapping between the handwritten ink and the digital character string, enabling sophisticated search, linguistic analysis, and paleographic study. However, the reality for collections such as the National Folklore Collection or the Royal Irish Academy’s manuscript holdings is a fundamental disconnect: high-resolution images of pages exist in one silo, and TEI-XML transcriptions exist in another, linked only by metadata at the page level.  
The user's query highlights a critical bottleneck in modern Digital Humanities: the "Alignment Gap." To train state-of-the-art multimodal Large Language Models (MLLMs) like Qwen2-VL to perform robust Optical Character Recognition (OCR) and Handwritten Text Recognition (HTR) on the idiosyncratic insular scripts of Gaelic, one requires a training dataset of image segments tightly coupled with their ground-truth text. Manually annotating bounding boxes for millions of lines of historical handwriting is economically infeasible. Therefore, an automated, weakly supervised method is required to align existing XML transcriptions with their corresponding visual locations on the manuscript page.  
This report proposes, analyzes, and details a novel technical pipeline to solve this challenge. By leveraging the emergent localization capabilities of **ColPali**—a model originally designed for document retrieval—we can generate "attention heatmaps" that act as proxy bounding boxes. These automatically derived coordinates allow us to construct a massive, high-quality instruction-tuning dataset. Subsequently, we explore the integration of this dataset into the **Unsloth** optimization framework to fine-tune **Qwen2-VL**, a dynamic-resolution VLM capable of mastering the intricate topography of Gaelic handwriting.

### **1.1 Visual Analysis of the Challenge Data**

The images provided for analysis (Image 1 and Image 2\) illustrate the specific complexity of the task. Unlike medieval vellum manuscripts which often suffer from extreme degradation, heavy staining, or irregular shapes, these samples appear to be from a later period, likely the late 19th or early 20th century, written on lined paper—characteristic of the *Schools’ Collection* or similar folklore archives. However, they retain the full morphological complexity of the traditional Gaelic script (*cló-Gaelach*).  
Several visual features visible in these images present significant hurdles for standard OCR engines and necessitate the advanced visual attention mechanisms of models like ColPali and Qwen2-VL:

* **Script Morphology:** The handwriting exhibits classic insular letterforms. The letter 'r' appears in its long form (r rotunda), often resembling a modern 'n' or 's' to the untrained eye. The 's' is frequently written as the "long s" (ſ), which can be confused with 'f'. The letter 'd' often takes an uncial form with a curved ascender.  
* **Diacritical Marks (Punctum Delens):** A defining feature of Irish orthography prior to the mid-20th century standardization is the *séimhiú* (lenition), denoted not by an 'h' following the consonant, but by a *punctum delens*—a single dot placed above the consonant (e.g., ḃ, ċ, ḋ). In Image 1, we see numerous examples of this. A standard vision encoder must learn that this minute visual feature (a few pixels of ink) radically alters the phonetics and meaning of the word.  
* **Scribal Abbreviations (Tironian Notes):** The texts almost certainly contain the *Tironian et* (⁊), a symbol resembling a geometric '7', used to represent the conjunction "agus" (and).1 Standard tokenizers trained on English or Romance languages often fail to tokenize this correctly, or split it into meaningless bytes, causing alignment drift if not handled explicitly.  
* **Baseline Drift and Density:** While the lined paper in the samples provides some structure, the handwriting frequently violates the baseline. Ascenders (like 'l', 'b', 'h') and descenders (like 'g', 'p') from adjacent lines often overlap or touch, creating "connected components" that baffle traditional segmentation algorithms based on pixel projection profiles.2

### **1.2 The XML-Image Disconnect**

The provided XML transcriptions likely follow the Text Encoding Initiative (TEI) guidelines. A typical structure might look like this:

XML

\<pb n="117"/\>  
\<p\>  
  \<lb/\>Tá sé páidre go bfuil cipic...  
  \<lb/\>Deirfear gur b'iad na Lochlannaigh...  
\</p\>

While the \<lb/\> tag indicates a line break, it does not carry spatial coordinates $(x, y, w, h)$. The challenge is to determine *where* on the 2000x3000 pixel image of Page 117 the string "Tá sé páidre..." physically resides. Without this information, we cannot crop the image to train an HTR model, nor can we train a grounding model to find the text. The following sections detail how ColPali serves as the "Bridge" to span this gap.

## ---

**2\. Theoretical Framework: ColPali for Weakly Supervised Alignment**

To align the text to the image without manual labels, we turn to **ColPali** (Contextualized Late Interaction over PaliGemma). Although ColPali is marketed and benchmarked as a document *retrieval* system—designed to find the most relevant PDF page given a query—its underlying architecture is fundamentally a similarity engine that operates at the level of visual patches.3

### **2.1 The Architecture of Late Interaction**

Traditional multimodal retrievers (like CLIP or SigLIP) use a "Dual Encoder" architecture where the entire image is pooled into a single vector $V\_{img}$ and the text is pooled into a single vector $V\_{text}$. Similarity is the dot product $V\_{img} \\cdot V\_{text}$. This approach destroys spatial resolution; the model knows the text matches the image *globally*, but it has discarded the information about *where* the match occurs.5  
ColPali employs **Late Interaction** (specifically the ColBERT architecture adapted for Vision-Language Models). It preserves the granular representations of both modalities until the very end of the process.

* **The Vision Encoder (SigLIP-So400m):** The manuscript page is processed as an image. The encoder divides the image into a grid of patches. For a standard input resolution, this typically results in a $32 \\times 32$ grid, yielding 1024 distinct visual embeddings.3 Each patch $p\_i$ is a vector in $\\mathbb{R}^{128}$ (after projection).  
* **The Language Encoder (Gemma-2B):** The transcription line is tokenized into a sequence of $N$ tokens. Each token $t\_j$ is embedded into a vector in $\\mathbb{R}^{128}$.4

### **2.2 The MaxSim Operator as a Localization Mechanism**

The retrieval score in ColPali is computed using the **MaxSim** operator. For every token in the text query, the model finds the *best matching* patch in the image, and then sums these maximum scores.3

$$S(Q, D) \= \\sum\_{j=1}^{N\_{tokens}} \\max\_{i=1}^{N\_{patches}} (t\_j \\cdot p\_i)$$  
This equation is the key to our alignment strategy. The $\\max$ operator is performing an implicit object detection. For the token "Lochlannaigh" (Vikings) in the transcription, the model calculates the dot product similarity with all 1024 patches on the page. The patch with the highest dot product is the one that visibly contains the word "Lochlannaigh" (or features semantically associated with it).  
By extracting the indices of these maximum similarities, or better yet, by visualizing the scalar similarity scores across the entire $32 \\times 32$ grid for a given sequence of text, we can generate a **Similarity Map** (or Attention Heatmap).9 This map effectively lights up the region of the manuscript where the handwriting matches the transcription.

### **2.3 Why ColPali Surpasses Standard Cross-Attention**

One might ask why we do not simply use the cross-attention maps from a standard VLM. The answer lies in the training objective. Standard generative VLMs (like LLaVA or base Qwen-VL) are trained to predict the next token. Their attention maps can be diffuse or focused on "hallucinated" regions if the model is not confident.  
ColPali, however, is trained with a **contrastive retrieval objective**. It is explicitly penalized if the visual embeddings of the document patches do not align highly with the query tokens.3 This forces the vision encoder to produce semantically rich patch embeddings that are highly discriminative. The resulting alignment is "sharper" and more distinct than typical generative attention, making it suitable for generating training data.10

## ---

**3\. Methodology: The Automated Alignment Pipeline**

We will now detail the step-by-step pipeline to convert the raw Image/XML pairs into a grounded dataset using ColPali.

### **3.1 Step 1: Preprocessing and Query Formulation**

The first step is to parse the XML transcriptions into discrete units. While word-level alignment is the "Holy Grail," it is computationally expensive and noisy due to the density of handwriting. **Line-level alignment** is the optimal sweet spot: it provides enough context for the model to disambiguate the text while being granular enough for training an HTR system.  
Handling The Tironian Note (⁊):  
A specific challenge with Gaelic manuscripts is the Tironian et.

* *Issue:* The standard tokenizer for Gemma-2B (used by ColPali) likely splits the Unicode character ⁊ (U+204A) into multiple byte tokens or treats it as an unknown (\<unk\>), as it is rare in the massive web corpora used for pre-training.1 If the model does not have a strong embedding for ⁊, the alignment for that specific character will fail.  
* *Solution (Query Expansion):* We employ a "visual-semantic substitution" strategy during the alignment phase. When constructing the query for ColPali, we replace instances of ⁊ in the XML string with the word "agus" (or "and").  
  * *Rationale:* ColPali is a semantic retriever. It has likely learned (from multilingual pre-training) that the visual symbol & or ⁊ (if present in training data like Wikipedia) corresponds to the semantic concept of "and/agus". By querying with "agus," we maximize the probability that the model aligns the word to the Tironian symbol on the page.  
  * *Note:* This substitution is *only* for the ColPali query. The target label for the final Qwen2-VL dataset must retain the original ⁊ to ensure historical accuracy in transcription.

### **3.2 Step 2: Generating Similarity Maps via ColPali**

We utilize the colpali\_engine to extract the interaction scores.12  
**The Algorithm:**

1. **Input:** Image $I$ (Manuscript Page), Text $T$ (Line from XML).  
2. **Forward Pass:**  
   * Pass $I$ through the SigLIP backbone to get patch embeddings $P \\in \\mathbb{R}^{32 \\times 32 \\times 128}$.  
   * Pass $T$ through the Gemma backbone to get token embeddings $Q \\in \\mathbb{R}^{L \\times 128}$.  
3. **Interaction:** Compute the dot product tensor $S \= Q \\times P^T$, resulting in a shape of $\[L, 32, 32\]$. This tensor represents the similarity of *each* text token to *each* image patch.  
4. Aggregation: To find the bounding box for the whole line, we sum the similarity maps across the token dimension $L$:

   $$M\_{line} \= \\sum\_{l=1}^{L} S\[l, :, :\]$$

   $M\_{line}$ is now a $32 \\times 32$ heatmap representing the likelihood of the text line existing at each patch.10

### **3.3 Step 3: From Heatmaps to Bounding Boxes**

The $32 \\times 32$ grid is too coarse (approx. $60 \\times 90$ pixels per patch on a standard image) for precise cropping. We must refine this.

1. **Upscaling:** We apply bicubic interpolation to resize the $32 \\times 32$ heatmap to the original image dimensions (e.g., $2500 \\times 3300$).14  
2. **Normalization & Thresholding:** The heatmap values are unbounded scores. We normalize them to the range $$ and apply **Otsu’s Binarization**. Otsu’s method automatically calculates the optimal threshold to separate the "foreground" (high similarity regions) from the "background" (paper texture/other lines).15  
   * *Refinement:* Because handwriting is thin, we might also use morphological dilation (expanding white regions) to ensure the mask covers the ascenders and descenders of the Gaelic script.  
3. **Contour Extraction:** Using OpenCV (cv2.findContours), we identify the connected components in the binary mask.  
4. **Bounding Box Selection:** We select the bounding box of the largest contour.  
   * *Heuristics:* We apply sanity checks. If the aspect ratio of the box is vertical (height \> width), it is likely noise (as lines are horizontal). If the box covers \>50% of the page, it is an error. These filtered boxes constitute our "Weakly Supervised Ground Truth."

## ---

**4\. Fine-Tuning Qwen2-VL with Unsloth**

With a dataset of {Image\_Crop, Text\_Label} (for OCR) and {Full\_Page, BBox\_Coordinates, Text\_Label} (for Grounding), we proceed to the recognition phase. We select **Qwen2-VL-7B-Instruct** as the base model. Qwen2-VL is currently state-of-the-art among open-weights models for OCR, outperforming GPT-4o-mini and others on benchmarks like OCRBench and DocVQA.17  
However, fine-tuning a 7B parameter VLM is computationally expensive. The vision encoder adds significant VRAM overhead. To make this feasible on standard hardware (e.g., a single A100 or even a consumer RTX 3090/4090), we utilize **Unsloth**.

### **4.1 Why Unsloth is Critical for VLM Fine-Tuning**

Unsloth is not merely a wrapper; it is a system-level optimization library that rewrites the backpropagation kernels for Transformer models.19

1. **Memory Efficiency:** Unsloth enables **4-bit quantization** of the model weights during training (QLoRA) with minimal precision loss. Crucially, it manages the memory fragmentation of the vision encoder outputs, which can be massive for high-resolution manuscript images.17  
2. **Speed:** By manually deriving gradients and using Triton kernels, Unsloth achieves up to 2x faster training throughput compared to standard Hugging Face Trainer implementations.19  
3. **Vision Support:** Recent updates to Unsloth have added native support for Qwen2-VL, handling the complex data collation required for the variable-resolution images.20

### **4.2 Handling Gaelic Manuscript Topology: Dynamic Resolution**

A key feature of Qwen2-VL is **Naive Dynamic Resolution**. Unlike previous models that squashed images into fixed squares (e.g., $224 \\times 224$), Qwen2-VL processes images at their native aspect ratio by dynamically allocating visual tokens.17

* *Relevance:* Gaelic manuscripts are rarely perfect A4 rectangles. They may be long strips or scraps. Dynamic resolution ensures that the density of the handwriting (the stroke width relative to the image size) is preserved. Unsloth supports this via the min\_pixels and max\_pixels arguments in the processor.17

### **4.3 Dataset Formatting: The JSONL Standard**

Unsloth requires data in a strict conversational JSONL format. We must transform our ColPali-aligned data into this structure.

#### **Format A: Dense OCR (Crop-Based)**

This is the most effective method for teaching the model to read the script. We crop the image using the ColPali bounding box and ask the model to transcribe it.

JSON

{  
  "messages":  
    },  
    {  
      "role": "assistant",  
      "content": \[  
        {"type": "text", "text": "Agus do bhí an rí..."}  
      \]  
    }  
  \]  
}

*Note:* Unsloth handles the image loading automatically if provided as a path or base64.20

#### **Format B: Visual Grounding (Full Page)**

This trains the model to perform layout analysis and find the text itself.

JSON

{  
  "messages": \[  
    {  
      "role": "user",  
      "content": \[  
        {"type": "image", "image": "file:///path/to/full\_page\_117.jpg"},  
        {"type": "text", "text": "Find the location of the text 'Agus do bhí an rí'"}  
      \]  
    },  
    {  
      "role": "assistant",  
      "content": \[  
        {"type": "text", "text": "\<|box\_start|\>(300,450),(350,800)\<|box\_end|\>"}  
      \]  
    }  
  \]  
}

*Correction:* Qwen2.5-VL and newer variants often use a JSON-style output or normalized coordinates \[0-1000\]. The specific tokens \<|box\_start|\> must be verified against the specific tokenizer config, but Unsloth abstracts much of this via the chat template.18

### **4.4 The Fine-Tuning Configuration**

To successfully fine-tune on this domain, we must adjust the Low-Rank Adaptation (LoRA) settings.

* **Target Modules:** We must target not just the language model layers (q\_proj, v\_proj) but also the **Vision Encoder** layers.  
  * *Why?* The SigLIP encoder in Qwen2-VL was trained on web-scale natural images (photos of cats, cars, signs). It has likely never seen 18th-century Gaelic handwriting on vellum. The "domain shift" is massive. Fine-tuning the vision layers (or applying LoRA to them) allows the model to learn the specific low-level edge features of the insular script.20  
* **Parameters:**  
  * finetune\_vision\_layers \= True 20  
  * finetune\_language\_layers \= True  
  * lora\_rank (r) \= 16 (Sufficient for adaptation, though 32 can be used for complex tasks).  
  * learning\_rate \= 2e-4 (Standard for QLoRA).

## ---

**5\. Experimental Execution and Validation**

### **5.1 Training Workflow**

The training process within the Unsloth framework follows this sequence:

1. **Initialization:** Load FastVisionModel with load\_in\_4bit=True.  
2. **Adapter Attachment:** Apply the PEFT configuration targeting both vision and language modules.  
3. **Data Collation:** Use UnslothVisionDataCollator. This component is vital as it handles the padding of the dynamic visual tokens, ensuring that batches don't crash due to dimension mismatches.20  
4. **Optimization:** Use adamw\_8bit optimizer to further reduce memory footprint.  
5. **Training:** Run for 1-3 epochs. Overfitting is a risk with small datasets, so early stopping based on a validation set (held-out manuscript pages) is recommended.

### **5.2 Dealing with Hallucinations and Gibberish**

A known issue in fine-tuning VLMs, particularly with Reinforcement Learning (RL) or aggressive SFT, is the generation of "gibberish" or repetitive tokens (e.g., "addCriterion").20 While this is more common in RL, SFT can also degrade if the data quality is poor.

* **Mitigation:** Ensure the aspect ratio of the crops is reasonable. Extremely wide, short strips (e.g., a single line of text 3000px wide and 50px high) can confuse the positional embeddings. Padding the crops to a more square aspect ratio before training can stabilize convergence.

### **5.3 Metric-Based Evaluation**

Evaluation should not rely solely on loss. We must use:

* **CER (Character Error Rate):** Critical for Gaelic because a single missed *punctum delens* or incorrect expansion of a Tironian note counts as an error.  
* **WER (Word Error Rate):** To measure semantic coherence.  
* **Grounding IOU:** For the grounding task, we measure the Intersection over Union (IoU) between the predicted boxes and the ColPali-generated pseudo-ground-truth.

## ---

**6\. Discussion: Implications for Digital Humanities**

This proposed pipeline represents a significant methodology shift. Traditionally, training an HTR model for a low-resource language like Early Modern Irish required finding a PhD student to manually annotate thousands of bounding boxes. By introducing **ColPali** as a weak supervisor, we automate the most expensive part of the pipeline.  
The "Inks & Skins" project notes the material complexity of these texts—the interaction of iron-gall ink with vellum.22 While our pipeline is digital, fine-tuning the vision encoder of Qwen2-VL allows the model to implicitly learn these material features (e.g., ignoring transparency/bleed-through) effectively "reading" the manuscript as a material object, not just a collection of shapes.  
Furthermore, this approach is transferable. The same pipeline (ColPali alignment \-\> Unsloth/Qwen Fine-tuning) can be applied to any set of manuscripts where loose transcriptions exist, from Sanskrit palm leaves to Civil War letters, democratizing access to high-performance HTR.

## ---

**7\. Technical Appendix: Implementation Details**

### **Table 1: Model Architecture Comparison**

| Feature | ColPali (Alignment Engine) | Qwen2-VL (Recognition Engine) |
| :---- | :---- | :---- |
| **Backbone** | PaliGemma-3B (SigLIP \+ Gemma 2B) | Qwen2-VL-7B (Qwen2-7B \+ ViT) |
| **Embedding Type** | Multi-Vector (Patch-level) | Auto-regressive Tokens |
| **Interaction** | Late Interaction (MaxSim) | Cross-Attention (M-ROPE) |
| **Resolution** | Fixed Patch Grid ($32 \\times 32$) | Naive Dynamic Resolution |
| **Role in Pipeline** | **Discriminative:** Finds *where* text is. | **Generative:** Reads *what* text is. |
| **Key Advantage** | Zero-shot localization via retrieval map. | State-of-the-art OCR accuracy. |

### **Code Snippet: Concept for ColPali Similarity Map Extraction**

Python

import torch  
import numpy as np  
from colpali\_engine.models import ColQwen2Processor, ColQwen2

def generate\_alignment\_heatmap(image, text\_query, model, processor):  
    """  
    Generates a spatial heatmap for a text query on a manuscript page.  
    """  
    \# 1\. Process Inputs  
    \# Note: Qwen2-VL based ColPali variants use dynamic resolution,   
    \# but the output embeddings still correspond to spatial patches.  
    inputs \= processor(text=\[text\_query\], images=\[image\], return\_tensors="pt")  
    inputs \= {k: v.to(model.device) for k, v in inputs.items()}

    \# 2\. Forward Pass to get Embeddings  
    with torch.no\_grad():  
        \# Get patch embeddings (visual) and token embeddings (text)  
        \# Note: This requires accessing the internal embedding layers   
        \# distinct from the final retriever score.  
        out \= model(\*\*inputs)   
          
        \# patch\_embeddings:  
        \# query\_embeddings:  
        patch\_embeddings \= out.visual\_embeddings   
        query\_embeddings \= out.text\_embeddings

    \# 3\. Compute MaxSim Matrix  
    \# Shape: \[N\_tokens, N\_patches\]  
    interaction \= torch.einsum("bnd,bmd-\>bnm", query\_embeddings, patch\_embeddings)  
      
    \# 4\. Aggregation  
    \# Sum scores across all text tokens to find regions relevant to the \*whole\* string  
    heatmap\_flat \= interaction.sum(dim=1).squeeze() \# \[N\_patches\]  
      
    \# 5\. Reshape to 2D Grid  
    \# Assuming a square grid for simplicity, though dynamic resolution requires   
    \# mapping patch indices back to spatial (x,y) based on the image aspect ratio.  
    side \= int(np.sqrt(heatmap\_flat.shape))   
    heatmap\_2d \= heatmap\_flat.view(side, side).cpu().numpy()  
      
    return heatmap\_2d

*Note:* The reshaping step in generate\_alignment\_heatmap is non-trivial for Qwen2-VL based ColPali models due to dynamic resolution; one must track the grid size returned by the processor or use the vidore/colpali-v1.2 (PaliGemma based) which uses a fixed resolution for easier implementation.12

### **Table 2: Recommended Unsloth Training Hyperparameters**

| Parameter | Value | Rationale |
| :---- | :---- | :---- |
| finetune\_vision\_layers | True | Essential for adapting to Gaelic script features and Vellum noise.20 |
| finetune\_language\_layers | True | Required to learn Gaelic syntax and expansion of abbreviations. |
| lora\_rank | 16 | Balance between plasticity and memory usage. |
| per\_device\_train\_batch\_size | 2 | Low batch size fits in GPU; compensated by gradient accumulation. |
| gradient\_accumulation\_steps | 4 | Simulates effective batch size of 8\. |
| max\_seq\_length | 2048 | Accommodates full page visual tokens \+ transcription text.17 |
| optim | adamw\_8bit | Reduces optimizer state memory by 75%.19 |
| learning\_rate | 2e-4 | Standard LoRA learning rate; lower than full fine-tuning. |

### **7.1 Handling "Gibberish" in VLM Fine-Tuning**

Recent reports indicate that Qwen2-VL can degenerate into outputting internal control tokens or repetitive nonsense (e.g., "addCriterion") during RLHF or aggressive fine-tuning.20

* **Cause:** This is often due to the model overfitting to the specific formatting of the training data or losing the probability mass for the \<|endoftext|\> token.  
* **Solution in Unsloth:** Use SFTTrainer (Supervised Fine-Tuning) rather than RL (Reinforcement Learning) for the initial OCR adaptation. Ensure the tokenizer correctly handles the End-of-Sequence (EOS) token in the chat template. If gibberish persists, restarting training with a lower learning rate or checking the chat\_template formatting in Unsloth is recommended.20

#### **Works cited**

1. Tironian notes : r/etymology \- Reddit, accessed December 7, 2025, [https://www.reddit.com/r/etymology/comments/qjasvk/tironian\_notes/](https://www.reddit.com/r/etymology/comments/qjasvk/tironian_notes/)  
2. Layout Analysis for Historical Manuscripts Using SIFT Features | Request PDF, accessed December 7, 2025, [https://www.researchgate.net/publication/220860496\_Layout\_Analysis\_for\_Historical\_Manuscripts\_Using\_SIFT\_Features](https://www.researchgate.net/publication/220860496_Layout_Analysis_for_Historical_Manuscripts_Using_SIFT_Features)  
3. Spatially-Grounded Document Retrieval via Patch-to-Region Relevance Propagation \- arXiv, accessed December 6, 2025, [https://arxiv.org/html/2512.02660v1](https://arxiv.org/html/2512.02660v1)  
4. ColPali Architecture | ColiVara Documentation, accessed December 6, 2025, [https://docs.colivara.com/getting-started/about/colpali-architecture](https://docs.colivara.com/getting-started/about/colpali-architecture)  
5. ColPali: Efficient Document Retrieval with Vision Language Models \- arXiv, accessed December 7, 2025, [https://arxiv.org/html/2407.01449v2](https://arxiv.org/html/2407.01449v2)  
6. Scaling ColPali to billions of PDFs with Vespa, accessed December 7, 2025, [https://blog.vespa.ai/scaling-colpali-to-billions/](https://blog.vespa.ai/scaling-colpali-to-billions/)  
7. ColPali: Efficient Document Retrieval with Vision Language Models \- arXiv, accessed December 6, 2025, [https://arxiv.org/html/2407.01449v5](https://arxiv.org/html/2407.01449v5)  
8. ColPali \+ Milvus: Redefining Document Retrieval with Vision-Language Models \- Zilliz blog, accessed December 7, 2025, [https://zilliz.com/blog/colpali-milvus-redefine-document-retrieval-with-vision-language-models](https://zilliz.com/blog/colpali-milvus-redefine-document-retrieval-with-vision-language-models)  
9. ColPali: Efficient Document Retrieval with Vision Language Models \- Hugging Face, accessed December 6, 2025, [https://huggingface.co/blog/manu/colpali](https://huggingface.co/blog/manu/colpali)  
10. Bringing Vision-Language Intelligence to RAG with ColPali | Towards Data Science, accessed December 7, 2025, [https://towardsdatascience.com/bringing-vision-language-intelligence-to-rag-with-colpali/](https://towardsdatascience.com/bringing-vision-language-intelligence-to-rag-with-colpali/)  
11. PaliGemmaTokenizer \- Keras, accessed December 7, 2025, [https://keras.io/keras\_hub/api/models/pali\_gemma/pali\_gemma\_tokenizer/](https://keras.io/keras_hub/api/models/pali_gemma/pali_gemma_tokenizer/)  
12. tonywu71/colpali-cookbooks: Recipes for learning, fine-tuning, and adapting ColPali to your multimodal RAG use cases. ‍ \- GitHub, accessed December 6, 2025, [https://github.com/tonywu71/colpali-cookbooks](https://github.com/tonywu71/colpali-cookbooks)  
13. ColPali \- Hugging Face, accessed December 7, 2025, [https://huggingface.co/docs/transformers/model\_doc/colpali](https://huggingface.co/docs/transformers/model_doc/colpali)  
14. Attention of a Kiss: Exploring Attention Maps in Video Diffusion for XAIxArts \- arXiv, accessed December 6, 2025, [https://arxiv.org/html/2509.05323v1](https://arxiv.org/html/2509.05323v1)  
15. Extract all bounding boxes using OpenCV Python \- Stack Overflow, accessed December 7, 2025, [https://stackoverflow.com/questions/21104664/extract-all-bounding-boxes-using-opencv-python](https://stackoverflow.com/questions/21104664/extract-all-bounding-boxes-using-opencv-python)  
16. Generating bounding boxes from heatmap data \- Stack Overflow, accessed December 7, 2025, [https://stackoverflow.com/questions/58419893/generating-bounding-boxes-from-heatmap-data](https://stackoverflow.com/questions/58419893/generating-bounding-boxes-from-heatmap-data)  
17. unsloth/Qwen2-VL-2B-Instruct \- Hugging Face, accessed December 6, 2025, [https://huggingface.co/unsloth/Qwen2-VL-2B-Instruct](https://huggingface.co/unsloth/Qwen2-VL-2B-Instruct)  
18. Qwen/Qwen2.5-VL-7B-Instruct \- Hugging Face, accessed December 7, 2025, [https://huggingface.co/Qwen/Qwen2.5-VL-7B-Instruct](https://huggingface.co/Qwen/Qwen2.5-VL-7B-Instruct)  
19. Unsloth: A Guide from Basics to Fine-Tuning Vision Models \- Learn OpenCV, accessed December 6, 2025, [https://learnopencv.com/unsloth-guide-efficient-llm-fine-tuning/](https://learnopencv.com/unsloth-guide-efficient-llm-fine-tuning/)  
20. Vision Fine-tuning | Unsloth Documentation, accessed December 6, 2025, [https://docs.unsloth.ai/basics/vision-fine-tuning](https://docs.unsloth.ai/basics/vision-fine-tuning)  
21. Qwen2.5 VL\! Qwen2.5 VL\! Qwen2.5 VL\! | Qwen, accessed December 7, 2025, [https://qwenlm.github.io/blog/qwen2.5-vl/](https://qwenlm.github.io/blog/qwen2.5-vl/)  
22. Inks & Skins: Investigating Manuscripts \- Marsh's Library, accessed December 7, 2025, [https://marshlibrary.ie/inks-and-skins/](https://marshlibrary.ie/inks-and-skins/)  
23. nomic-ai/colpali \- GitHub, accessed December 7, 2025, [https://github.com/nomic-ai/colpali](https://github.com/nomic-ai/colpali)