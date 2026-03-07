# **Deep Research Report: End-to-End Fine-Tuning of Qwen3-VL for Historic Manuscript Transcription using Unsloth, MLflow, and Ragas**

## **1\. Introduction: The Convergence of Neural Vision and Celtic Philology**

The digitization of historical archives has traditionally been a bifurcated process: physical artifacts are scanned into raster images, and subsequently, text is extracted via Optical Character Recognition (OCR) or manual transcription. However, for complex collections like the *Dúchas.ie* Schools’ Collection (Bailiúchán na Scol), this binary approach is insufficient. The collection, comprising folklore compiled by Irish schoolchildren in the 1930s, represents a dataset of immense linguistic and visual complexity. It features non-standardized handwriting, dialectal Irish variations, and a rigid yet often violated page structure. The user’s objective—to utilize the Unsloth optimization library to fine-tune the Qwen3-VL (and its architectural lineage including Qwen2-VL) on this dataset—marks a paradigm shift from simple recognition to **Visual Document Understanding (VDU)**.  
This report provides an exhaustive technical and operational roadmap for this undertaking. We move beyond standard OCR metrics to explore how Vision-Language Models (VLMs) can be trained not only to read text but to *reason* about its spatial location using bounding boxes, and how to rigorously evaluate this capability using "LLM-as-a-judge" frameworks like Ragas and MLflow. The core premise is that by leveraging the memory-efficient backpropagation kernels of Unsloth, we can fine-tune massive 7B+ parameter models on high-resolution historical manuscripts, enabling a level of semantic grounding previously unattainable in Digital Humanities.  
The integration of Unsloth allows for the manipulation of the Qwen-VL architecture's projection layers, specifically targeting the "Vision-Language Adapter" mechanism. By training these layers on the Dúchas corpus, we effectively teach the model to perceive the unique stroke patterns of the *Seanchló* (old Gaelic type) and 1930s cursive as semantic tokens rather than mere noise. This process transforms the model from a generic image describer into a specialized paleo-grapher.

## ---

**2\. Theoretical Framework: The Qwen-VL Architecture and Unsloth Optimization**

To successfully execute the fine-tuning detailed in the Unsloth documentation, one must first dissect the underlying architecture of the model and the optimization techniques that make such training feasible on standard hardware.

### **2.1 The NaViT Paradigm in Qwen-VL**

The Qwen-VL family (encompassing Qwen2-VL and the anticipated Qwen3-VL iterations) distinguishes itself from predecessors like CLIP or the original Vision Transformer (ViT) through its handling of image resolution. Traditional models rely on a fixed-resolution crop (e.g., $224 \\times 224$ or $336 \\times 336$ pixels). For the Dúchas dataset, this is catastrophic. The manuscript pages 1 are portrait-oriented, A4-like documents containing dense handwriting. Resizing these to a square distorts the aspect ratio, compressing vertical strokes and stretching horizontal ones, which is particularly detrimental for Irish orthography where the distinction between a *síneadh fada* (acute accent) and a random ink blot depends on precise shape preservation.  
Qwen-VL employs a variant of the **Native Vision Transformer (NaViT)** protocol. Instead of resizing, the model processes images in their native aspect ratio/resolution by breaking them into a variable sequence of $14 \\times 14$ patches.

* **Implication for Dúchas:** A full handwritten page might generate 2,000+ visual tokens. This preserves the high-frequency spatial details required to distinguish between letters like 'a' and 'o' in cursive, or to detect the subtle *séimhiú* (lenition dot) used in older Irish scripts.  
* **The C-Abstractor:** These thousands of tokens are then compressed by a Convolutional Abstractor (C-Abstractor) or a similar Perceiver Resampler mechanism before entering the Large Language Model (LLM) backbone. Fine-tuning with Unsloth primarily targets the attention mechanisms within the LLM that attend to these compressed visual tokens, refining how the "brain" interprets the signals from the "eyes."

### **2.2 Unsloth: Mathematical Optimization for VLM Training**

The user’s query explicitly references Unsloth. Unsloth is not merely a wrapper; it is a fundamental rewrite of the PyTorch training loop using OpenAI’s Triton language. Its primary contribution to VLM fine-tuning is **memory efficiency**.

#### **2.2.1 Gradient Checkpointing and Kernel Fusion**

Training a VLM requires storing the forward pass activations to calculate gradients during the backward pass. For high-resolution images (like the Dúchas scans), these activation maps are massive. Unsloth employs intelligent gradient checkpointing and "Kernel Fusion."

* **Kernel Fusion:** Instead of performing operations like RMSNorm, RoPE (Rotary Positional Embeddings), and Softmax as separate read/write operations to the GPU's High Bandwidth Memory (HBM), Unsloth fuses them into a single kernel. This reduces VRAM usage by up to 60%.  
* **Relevance:** This allows us to feed the model the *full-resolution* Dúchas page. Without Unsloth, we would be forced to downsample the image or chop it into small tiles, destroying the global context (e.g., understanding that a paragraph at the bottom of the page refers to a title at the top).

#### **2.2.2 LoRA (Low-Rank Adaptation) Dynamics**

We utilize LoRA to fine-tune the model. We do not update the full weight matrix $W$ (which might have 7 billion parameters). Instead, we train two smaller matrices $A$ and $B$ such that the weight update is $\\Delta W \= BA$.  
For Qwen-VL on Dúchas.ie, we target specific modules:

* q\_proj, k\_proj, v\_proj: The attention mechanisms.  
* gate\_proj, up\_proj, down\_proj: The MLP layers where "knowledge" and reasoning reside.  
* **Strategic Freezing:** We typically freeze the Vision Encoder. The goal is to teach the LLM to interpret standard visual features as Irish text, rather than teaching the eye to see new shapes. However, if the handwriting is extremely idiosyncratic, Unsloth allows for unlocking the vision tower, though this increases compute cost.

## ---

**3\. The Data Landscape: Dúchas.ie and the Schools' Collection**

The research material provided includes specific examples from the Dúchas.ie archive. A deep analysis of these artifacts is prerequisite to constructing the training dataset.

### **3.1 Artifact Analysis: Image 1 (Weather Lore)**

The first image provided 1 is titled *Comharthaí i dtaobh na h-aimsire* (Signs regarding the Weather). It originates from Cill Éinne (Killeany), Co. Galway, indicating a Connacht dialect.

* **Visual Structure:** The page is lined. The handwriting is a controlled, legible cursive, likely from a senior student.  
* **Text Content:** "Bíonn torann mór ag an bhfairrge. Nuair a bions dath nádúrach ar an bfairrge..." (There is a big noise at the sea. When there is a natural color on the sea...).  
* **Key Challenge \- Orthography:** The text uses standard Roman script but retains Irish orthographic features. The word *bhfairrge* (sea) shows urú (eclipsis) and séimhiú (lenition). The model must learn that the visual cluster bhf maps to the semantic concept of "sea" in the genitive/dative case.  
* **Key Challenge \- Layout:** The image contains marginalia or page numbers (e.g., the number "2" at the top). The model must learn to distinguish between the *content text* (the folklore) and the *meta-text* (page numbers, titles). Bounding box training is essential here to teach the model to ignore the header when asked for the "story."

### **3.2 Artifact Analysis: Image 2 (Cipín Óir)**

The second image is titled *Cipín Óir* (Little Gold Stick/Twig).

* **Visual Structure:** This page is denser. The ink appears slightly more faded or the pen stroke is thinner.  
* **Handwriting Variability:** The hand is different from Image 1\. The loops on letters like 'g' and 'y' extend into the lines below.  
* **Line Separation:** This "inter-line interference" is a major challenge for bounding box generation. If the descender of a 'g' on line 4 crosses into the bounding box of line 5, the model might hallucinate the 'g' as part of the next line.  
* **Linguistic Nuance:** The text begins "Tá sé páidre..." (likely "Tá sé soiléir" or a dialect variant). The transcription provided in the snippet helps, but the model needs to handle dialectal spelling variations that may not exist in its pre-training corpus.

### **3.3 The Alignment Gap**

The fundamental problem identified in the snippet 1 is the lack of granular alignment. We have the full page image and the full page text (XML). We do *not* have line-by-line coordinates.

* **The User Requirement:** The user specifically asks to "develop a finetuning dataset." This implies we cannot just *download* a dataset; we must *construct* one. The primary task is to bridge the gap between the raw XML text and the pixel coordinates on the page.

## ---

**4\. Methodology: Synthetic Grounding and Dataset Construction**

To satisfy the user's request to use "vision transformer reasoning and bounding box capabilities," we must create a dataset that trains these specific modalities. Since manual annotation of 500,000 pages is impossible, we employ a **Weak Supervision** strategy using the model itself.

### **4.1 The Bootstrapping Phase (Zero-Shot Alignment)**

We utilize the pre-trained Qwen2-VL model (before fine-tuning) in inference mode to generate our initial "Silver Standard" dataset. Qwen2-VL has strong zero-shot grounding capabilities.  
Step 1: Segmentation  
We parse the Dúchas XML 1 and split the full text into individual sentences or clauses.

* *Input Text:* "Bíonn torann mór ag an bhfairrge. Nuair a bions dath nádúrach ar an bfairrge..."  
* *Segments:*  
  1. "Bíonn torann mór ag an bhfairrge."  
  2. "Nuair a bions dath nádúrach ar an bfairrge"

Step 2: Recursive Grounding Prompts  
We feed the full image and one segment at a time to the model.

* **Prompt:** "You are an expert archivist. Look at this handwritten page. Locate the exact phrase: '{segment}'. Return the bounding box coordinates in the format \[ymin, xmin, ymax, xmax\]."  
* **Reasoning Injection:** We also ask the model to explain *why* it chose that box. "Identify the first word and the last word of the phrase visually."

Step 3: Verification via OCR  
The model might hallucinate a box. To verify, we perform a crop operation on the image using the returned coordinates. We then run a distinct, lightweight OCR engine (like Tesseract or PaddleOCR optimized for handwriting) on the crop.

* **Metric:** We calculate the Levenshtein Distance between the XML segment and the OCR output of the crop.  
* **Threshold:** If the similarity is $\> 0.8$, we accept the bounding box as "Silver Truth." If not, we discard the segment or flag it for human review.

### **4.2 Formatting for Unsloth**

Unsloth and Qwen-VL require a specific JSONL format known as "ChatML" or "ShareGPT" style, adapted for vision.  
**The Data Structure:**

JSON

{  
  "messages": \[  
    {  
      "role": "user",  
      "content": \[  
        {"type": "image", "image": "path/to/duchas\_image\_01.jpg"},  
        {"type": "text", "text": "Locate and transcribe the first sentence regarding the sea."}  
      \]  
    },  
    {  
      "role": "assistant",  
      "content": "\<|box\_start|\>(150, 200), (400, 800)\<|box\_end|\> Bíonn torann mór ag an bhfairrge. \\n\\nReasoning: The sentence begins with a capitalized 'B' at the top margin and ends with the period after 'bhfairrge'."}  
    }  
  \]  
}

*Note the integration of reasoning.* By including the "Reasoning" field in the training data, we explicitly train the model's "Chain of Thought" capabilities. The model learns that to answer the user, it must first output the box, then the text, then the justification. This structure forces the model to attend to the visual features before committing to a textual answer.

### **4.3 Handling "Reasoning" Tokens**

The user requested utilizing the "vision transformer's reasoning." In Qwen-VL, reasoning is often emergent, but it can be steered. We introduce a special token or a specific prompt header, e.g., \<|reasoning\_step|\>.  
During fine-tuning, we calculate the loss on the entire sequence: the bounding box tokens, the transcription, and the reasoning text. This ensures the model's internal state (hidden layers) contains a representation of the logic used to decipher the handwriting (e.g., "This loop is a 'g', not a 'y' because...").

## ---

**5\. Fine-Tuning Strategy: Implementation with Unsloth**

This section outlines the specific configuration for the Unsloth trainer, satisfying the technical requirements of the user's request.

### **5.1 Environment and Dependencies**

To run the Unsloth Qwen2-VL implementation, specific library versions are critical to support the custom kernels.

| Package | Purpose | Version Constraint |
| :---- | :---- | :---- |
| unsloth | Optimization Kernels | unsloth\[colab-new\] @ git+https://github.com/unslothai/unsloth.git |
| torch | Deep Learning Backend | \>= 2.2.0 (for Triton support) |
| qwen-vl-utils | NaViT Preprocessing | Latest |
| flash-attn | Attention Optimization | \>= 2.5.0 |

### **5.2 Model Configuration**

We initialize the FastVisionModel from Unsloth. The key parameters for a "Deep Research" configuration are:

* **Quantization:** load\_in\_4bit \= True. We use Normal Float 4 (NF4) quantization. This allows the 7B model to fit into approximately 6GB of VRAM, leaving the rest of the 24GB (on a consumer card) or 80GB (on A100) for the massive activation maps of the high-resolution images.  
* **Sequence Length:** max\_seq\_length \= 4096\. This is non-negotiable. The visual tokens alone can take up 1500-2000 spots. The text reasoning and transcription need the remaining buffer.  
* **LoRA Rank:** r \= 64\. While standard text tasks work with rank 8 or 16, Vision-Language alignment is complex. A higher rank allows the adapter matrices to capture more subtle correlations between visual features (ink strokes) and semantic concepts (words).

### **5.3 The Training Loop and Loss Function**

The training objective is Next Token Prediction, but applied to a multimodal sequence.

$$\\mathcal{L} \= \- \\sum\_{i} \\log P(t\_i | t\_{\<i}, I)$$

Where $t\_i$ is a token (which could be a text character or a coordinate integer) and $I$ is the image embedding.  
Bounding Box Regression (Implicit):  
Unlike detection models (YOLO) that use L1 loss or GIoU loss for boxes, Qwen-VL discretizes coordinates into tokens (e.g., integer 0 to 1000). The loss is Cross-Entropy. The model learns to predict "Coordinate 200" with the same mechanism it learns to predict the word "The."

* **Implication:** Fine-tuning on Dúchas coordinates aligns the model's "spatial vocabulary" with the layout of the Schools' Collection notebooks. It learns the typical margins, line heights, and paragraph indentations of 1930s Irish stationery.

## ---

**6\. Evaluation Architecture: MLflow and Ragas**

The user explicitly requested an investigation into **MLflow** and **Ragas** with **LLM-as-a-judge**. This transforms the project from a simple training script into a rigorous scientific experiment.

### **6.1 MLflow for Multimodal Experiment Tracking**

Standard MLflow logs scalar metrics (Loss, Accuracy). For VLM research, we need **Visual Artifact Logging**.  
Implementation Strategy:  
We create a custom MLflowCallback that integrates with the Hugging Face Trainer.

1. **Step-based Logging:** Every 100 steps, we trigger a validation routine.  
2. **Visual Predictions:** We take a held-out image from Dúchas (e.g., Image 2, *Cipín Óir*). We feed it to the current model state.  
3. **Overlay Generation:** We use OpenCV to draw the predicted bounding boxes and the predicted text onto the image.  
4. Artifact Upload: mlflow.log\_image(annotated\_image, f"val\_step\_{step}.png").  
   This allows the researcher to visually scroll through the training history and see the model "learning" to find the lines of text. Initially, the boxes will be random; over time, they will snap to the handwritten lines.

### **6.2 Ragas: Adapting RAG Metrics for Transcription**

Ragas (Retrieval Augmented Generation Assessment) is typically used to evaluate Chatbots. We adapt it for **Transcription Fidelity** and **Visual Grounding**.  
The "LLM-as-a-Judge" Configuration:  
We employ a powerful external model (GPT-4o or Claude 3.5 Sonnet) as the Judge. The Judge does not see the raw pixels (unless using a VLM judge), but evaluates the textual and logical consistency.

#### **6.2.1 Custom Metric: TranscriptionFaithfulness**

We define a custom Ragas metric class.

* **Input:** Ground Truth XML text, Model Predicted Text.  
* **Judge Logic:** "Compare the two texts. Ignore differences in capitalization or punctuation unless they change the meaning. Penalize heavily for missing words or hallucinated phrases. Account for Irish dialect spelling (e.g., 'feilm' vs 'feirm')."  
* **Score:** 0.0 to 1.0.

#### **6.2.2 Custom Metric: BoundingBoxAlignment (IoU)**

This metric is deterministic, not LLM-based, but we log it alongside the Ragas scores.  
We calculate the Intersection over Union (IoU) between the predicted box and the "Silver Standard" box.

$$\\text{IoU} \= \\frac{\\text{Area of Overlap}}{\\text{Area of Union}}$$

* **Threshold:** A score $\> 0.5$ is generally considered a correct detection. We log the *Average IoU* across the validation set.

#### **6.2.3 "Visual Reasoning" Evaluation (The VLM Judge)**

To evaluate the "Reasoning" component requested by the user, we use a VLM as the judge (e.g., GPT-4o-Vision).

* **Prompt:** "I will show you an image and a bounding box with a transcription. 1\. Does the box strictly contain the text? 2\. Is the transcription accurate to the handwriting? 3\. Does the reasoning provided explain the visual features (e.g., mentioning the loop on the 'g')?"  
* **Output:** A structured JSON score. This is the gold standard for evaluating the "Reasoning" capabilities of our fine-tuned Qwen model.

## ---

**7\. Mismatch Detection and Active Learning Loop**

The user asked to "detect mismatches and develop a finetuning dataset." This suggests an iterative process, not a one-off training run.

### **7.1 Taxonomy of Mismatches**

Using our MLflow/Ragas pipeline, we categorize errors into three types:

1. **Spatial Mismatches:** The model transcribes the text correctly but draws the box around the wrong line (or a massive box covering the whole page).  
   * *Cause:* Loss of spatial resolution in the vision encoder.  
   * *Remedy:* Increase the ratio of "detection" tasks in the training mix.  
2. **Orthographic Mismatches:** The model transcribes "bád" instead of "bhád" (missing the lenition).  
   * *Cause:* The visual features of the lenition dot are too small for the $14 \\times 14$ patch size or were lost in compression.  
   * *Remedy:* Upsample the image resolution during preprocessing or use "Zoom-in" crops as data augmentation.  
3. **Hallucination Mismatches:** The model generates text that sounds like fluent Irish folklore but isn't on the page.  
   * *Cause:* Over-reliance on the Language Model priors (the "brain" ignoring the "eyes").  
   * *Remedy:* Increase the weight of the visual loss or use Negative Preference Optimization (DPO) to punish hallucinations.

### **7.2 The Active Learning Cycle**

We do not simply train once. We use the mismatch detection to *improve* the dataset.

1. **Inference:** Run the model on 10,000 unlabeled Dúchas pages.  
2. **Uncertainty Sampling:** Identify pages where the model has high entropy (low confidence) or where the Ragas Judge gives a low score.  
3. **Human-in-the-Loop:** These specific "hard" examples are sent to a human annotator (via a tool like Label Studio). The human corrects the box or the text.  
4. **Re-Training:** These corrected examples are added to the training set with a higher weight. This creates a "Data Flywheel," where the model progressively masters the edge cases of the Dúchas collection.

## ---

**8\. Conclusion**

This report establishes that fine-tuning Qwen3-VL (Qwen2-VL architecture) on the Dúchas.ie dataset is not only feasible via Unsloth but represents a cutting-edge application of Multimodal AI. By rigorously defining the data ingestion pipeline, leveraging the NaViT architecture for high-resolution handwriting analysis, and wrapping the entire process in an MLflow/Ragas evaluation loop, we can create a model that does more than transcribe—it understands.  
The integration of "Reasoning" and "Bounding Box" capabilities transforms the model from a passive observer into an active archivist, capable of pointing to the evidence for its transcriptions. This transparency is vital for digital heritage, where trust in the automated output is paramount. The proposed workflow serves as a blueprint for modernizing not just the Schools' Collection, but any vast, handwritten historical archive.

# ---

**Appendix: Technical Implementation Details**

## **A.1 Unsloth Training Script Skeleton**

The following Python code structure demonstrates how to initialize the Unsloth trainer with the specific LoRA configurations discussed.

Python

from unsloth import FastVisionModel  
from trl import SFTTrainer, SFTConfig  
import torch

\# 1\. Load Model with 4-bit Quantization (Memory Optimization)  
model, tokenizer \= FastVisionModel.from\_pretrained(  
    "Qwen/Qwen2-VL-7B-Instruct",  
    load\_in\_4bit \= True,  
    use\_gradient\_checkpointing \= "unsloth",  
)

\# 2\. Configure LoRA Adapters (Targeting Language & Vision-Language Projection)  
model \= FastVisionModel.get\_peft\_model(  
    model,  
    r \= 64, \# High rank for complex vision tasks  
    target\_modules \=,  
    lora\_alpha \= 64,  
    lora\_dropout \= 0,  
    bias \= "none",  
)

\# 3\. Define Hyperparameters  
training\_args \= SFTConfig(  
    output\_dir \= "./duchas\_finetune",  
    per\_device\_train\_batch\_size \= 2, \# Small batch size due to image size  
    gradient\_accumulation\_steps \= 4, \# Simulate batch size of 8  
    learning\_rate \= 2e-4,  
    max\_seq\_length \= 4096, \# Essential for full-page dense text  
    fp16 \= not torch.cuda.is\_bf16\_supported(),  
    bf16 \= torch.cuda.is\_bf16\_supported(),  
    logging\_steps \= 10,  
    save\_steps \= 500,  
    optim \= "adamw\_8bit", \# Further memory saving  
)

\# 4\. Initialize Trainer  
trainer \= SFTTrainer(  
    model \= model,  
    tokenizer \= tokenizer,  
    train\_dataset \= duchas\_dataset, \# Pre-formatted JSONL  
    dataset\_text\_field \= "messages",  
    args \= training\_args,  
    packing \= False,  
)

\# 5\. Execute Training  
trainer.train()

## **A.2 Custom Ragas Metric Implementation**

This conceptual code block illustrates how to define the "transcription fidelity" metric for the LLM-as-a-judge.

Python

from ragas.metrics import Metric  
from ragas.llms import LangchainLLM

class DuchasFidelity(Metric):  
    def \_\_init\_\_(self):  
        self.name \= "duchas\_fidelity"  
        self.evaluation\_mode \= "q\_a"   
        \# We treat the image+prompt as Q, and transcription as A

    def score(self, row):  
        ground\_truth \= row\['ground\_truth'\]  
        prediction \= row\['prediction'\]  
          
        prompt \= f"""  
        Compare the following two Irish texts.  
        Ground Truth: {ground\_truth}  
        Prediction: {prediction}  
          
        Rate the fidelity on a scale of 0.0 to 1.0.  
        \- Deduct 0.1 for minor spelling errors.  
        \- Deduct 0.5 for missing phrases.  
        \- Deduct 1.0 for hallucinations.  
          
        Return only the score.  
        """  
          
        \# Call the Judge LLM (e.g., GPT-4o)  
        score \= self.llm.predict(prompt)  
        return float(score)

## **A.3 Data Formatting for Qwen-VL**

Correctly formatting the input is crucial for activating the model's vision capabilities.

| Field | Description | Format |
| :---- | :---- | :---- |
| image | Path or base64 string | "path/to/cill\_einne.jpg" |
| text | The user instruction | "Transcribe the text in the bounding box..." |
| box\_2d | Coordinate Tokens | \[ymin, xmin, ymax, xmax\] normalized to 1000x1000 |

The prompt template must wrap these elements correctly so Unsloth can tokenize them into the multimodal sequence expected by the NaViT encoder. Failure to use the correct chat template will result in the model seeing the image as random noise rather than a structured input.

#### **Works cited**

1. Cill Éinne · The Schools' Collection \_ dúchas.ie.pdf