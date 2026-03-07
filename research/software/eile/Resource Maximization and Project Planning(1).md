# **Strategic Resource Maximization: Architecting the Celtic Heritage Intelligence Platform (CHIP)**

## **1\. Executive Strategy: The Resource Arbitrage Architecture**

The successful execution of the **Celtic Heritage Intelligence Platform (CHIP)** requires a paradigm shift from traditional resource allocation to a model of aggressive **compute arbitrage**. We are presented with a heterogenous portfolio of assets ranging from transient, high-value cloud credits (e.g., $430 on Blaxel, $280 on Modal) to permanent, high-performance edge hardware (MacBook Pro M4 Max) and low-cost utility infrastructure (Hetzner, Oracle). The totality of these resources, valued in excess of $2,500, provides a unique opportunity to construct an enterprise-grade AI system dedicated to the preservation, analysis, and synthesis of Celtic languages and folklore.  
To maximize this inventory, we must treat every credit and cycle as a currency within an internal economy. High-cost, low-latency resources (Modal, Blaxel) must be strictly reserved for "burst" operations and user-facing inference, while heavy, throughput-intensive tasks (training, batch OCR) must be routed to the lowest-cost providers (ThunderCompute, Nebius) or offloaded to "sunk cost" local hardware (Mac M4). This report delineates a comprehensive architectural blueprint, operational runbook, and financial model to achieve this, utilizing a **Fallback Chain** logic to ensure system resilience and cost-efficiency.  
The central thesis of this architecture is the **Sidero Mesh**, a hybrid Kubernetes cluster bootstrapped via **Sidero Omni**, which unifies the disparate Arm64 resources (Oracle, Hetzner, Mac) into a single control plane. Overlaying this is an agentic workflow powered by **Z.ai** and **Claude**, capable of autonomous coding and data ingestion, and a cognitive layer managed by **Letta** for stateful interaction. The end goal is a self-sustaining platform that ingests data from *Dúchas.ie* and *Canúint.ie*, processes it via **Sam3** and **EuroLLM**, and serves it to the world.

## ---

**2\. Comprehensive Resource Audit and Valuation**

Before architectural placement, we must normalize the diverse resource pool into comparable units of utility—primarily **Compute Hours**, **Token Throughput**, and **Storage Durability**. This audit identifies the specific comparative advantage of each asset.

### **2.1 High-Performance Cloud Compute: The Training Engines**

The portfolio contains three primary sources for GPU compute, each with distinct pricing models and performance characteristics. The strategy dictates utilizing these strictly for "heavy lifting"—training and fine-tuning—rather than persistent hosting.

| Provider | Budget | Key Instance Types | Effective Capacity (Est.) | Strategic Role |
| :---- | :---- | :---- | :---- | :---- |
| **ThunderCompute** | $120 | H100 PCIe ($1.89/hr), A100 80GB ($0.78/hr) 1 | \~63 hrs (H100) or \~153 hrs (A100) | **Primary Training.** Lowest cost per FLOP. Use for full fine-tuning of 7B-30B parameter models (Qwen2.5-VL, EuroLLM). |
| **Nebius AI** | $51 | H100 ($2.00-$2.95/hr), L40S ($1.35/hr) 2 | \~17 hrs (H100) or \~37 hrs (L40S) | **Precision Training & Verification.** Use for final epoch runs or specific H100 SXM optimized workloads that require NVLink. |
| **Modal** | $280 | H100 ($4.56/hr), A100 ($1.29-$5.59/hr) 3 | \~60 hrs (H100) or \~215 hrs (A100 serverless) | **Burstable Pipeline.** High hourly cost but charges by the second. Use for audio segmentation (Sam-Audio) and batch processing, not long training. |
| **Anyscale** | $100 | Ray on AWS/GCP (BYOC) | Variable (Spot dependent) | **Orchestration.** Use to distribute hyperparameter tuning across spot instances, leveraging the $100 credit for the control plane. |

**Strategic Insight:** ThunderCompute represents the highest leverage for raw training throughput. An A100 80GB at \~$0.78/hr 1 is significantly cheaper than Nebius or Modal. Therefore, all "long-run" jobs—specifically the fine-tuning of **EuroLLM-22B** or **Qwen2.5-VL** on the Dúchas corpus—must reside on ThunderCompute. Nebius, with its H100 availability, should be reserved for specific optimizations where FP8 precision or Transformer Engine acceleration is required, or for "sanity check" runs before committing to longer jobs on ThunderCompute. Modal's value lies in its cold-start architecture; it should never be used for continuous training but is ideal for the event-driven processing of audio files from Canúint.ie.

### **2.2 Agentic & Inference Infrastructure: The Cognitive Layer**

These resources facilitate the deployment of the AI agents, the "brain" of the platform, and the developer tools required to build them.

| Resource | Capacity | Strategic Role |
| :---- | :---- | :---- |
| **Blaxel** | $430 | Agent hosting, Serverless GPUs 4 |
| **Letta** | 25,000 Credits | Stateful Agent Memory |
| **Google Cloud** | £200 | Gemini 1.5 Pro/Flash, Vertex AI |
| **Hugging Face Pro** | $50 Credits | Zero-GPU, Inference Endpoints |
| **ElevenLabs** | 210k Chars | Voice Synthesis |
| **Z.ai Coding Plan** | GLM-4.6, MCPs | Developer Productivity |
| **Claude Code Max** | x20 Usage | Architectural Refactoring |

### **2.3 Data Infrastructure & Storage: The Digital Lakehouse**

Data is the lifeblood of the CHIP project. This layer manages the ingestion of CulturaX and Dúchas datasets, ensuring durability and accessibility without incurring prohibitive egress fees.

| Resource | Budget | Role |
| :---- | :---- | :---- |
| **Confluent** | $400 | Kafka Streaming |
| **MotherDuck** | 21 Days Free \+ Lite | Cloud DuckDB |
| **LanceDB Cloud** | $100 | Vector Database |
| **PlanetScale** | $5/mo plan | Serverless MySQL |
| **Cloudflare R2** | Pay-as-you-go | Object Storage |

### **2.4 Hardware Assets: The Physical Substrate**

| Resource | Specs | Strategic Role |
| :---- | :---- | :---- |
| **MacBook Pro M4 Max** | 48GB RAM, 1TB SSD | **Local Inference & Quantization.** The M4 Max is a beast for MLX-based inference. It will run local OCR (**Sam3**, **olmOCR**) to save cloud costs and perform model quantization. |
| **Hetzner CAX41** | 32GB RAM, Arm64 | **Cluster Control Plane.** Runs **Sidero Omni/Talos**. Acts as the stable "head node" for the hybrid cluster. |
| **Oracle Free Tier** | 4x Arm64 Cores, 24GB | **Worker Node.** Runs lightweight scrapers and Kafka consumers (Confluent clients). |
| **Sidero Omni** | 14-Day Trial | **K8s Management.** Unifies Hetzner, Oracle, and potentially cloud VMs into a single Kubernetes cluster. |

## ---

**3\. Architectural Blueprint: The Celtic Heritage Intelligence Platform**

To maximize these resources, we define a unified architecture. The system is designed to ingest raw Celtic cultural data (text, audio, image), process it using cost-effective compute, fine-tune specialized models, and serve them via stateful agents.

### **3.1 The "Fallback Chain" Architecture**

Given the mix of reliable APIs (Google, Z.ai) and budget-constrained GPUs (ThunderCompute, Blaxel), we implement a **Fallback Chain** for inference reliability. This ensures that the system remains operational even when specific credit pools are exhausted or when latency requirements shift.

1. **Primary Node (Local Edge):** **MacBook Pro M4 Max** running MLX versions of Qwen2.5-VL or EuroLLM-22B.  
   * *Cost:* $0 (Sunk Cost).  
   * *Role:* Handling "base load" inference, local OCR processing, and development testing.  
2. **Secondary Node (Serverless Cloud):** **Blaxel** or **Modal** serving quantized models.  
   * *Cost:* Deducted from $430/$280 credits.  
   * *Role:* Handling "burst" traffic, serving the public API when the local machine is offline, and running agentic workflows that require persistent uptime.  
3. **Tertiary Node (Commercial API):** **Google Gemini 1.5 Pro** or **Z.ai GLM-4.6**.  
   * *Cost:* Deducted from credits/subscription.  
   * *Role:* Handling complex reasoning tasks, multimodal analysis of difficult manuscripts where open models fail, and generating "ground truth" data for training.

### **3.2 Data Pipeline Design: The Event-Driven Backbone**

The data pipeline utilizes **Confluent** as the central nervous system, buffering data between ingestion and processing to decouple the expensive GPU workers from the slower scrapers.

* **Ingest (Oracle):** Oracle Cloud Arm64 instances run lightweight Python scrapers targeting *Dúchas.ie* and *Canúint.ie*. They push raw metadata events to **Confluent Kafka**.  
* **Storage (R2):** Raw assets (images of manuscripts, audio files) are pushed directly to **Cloudflare R2** via signed URLs. The zero-egress policy of R2 is the linchpin here; it allows us to pull terabytes of data into ThunderCompute for training without bankruptcy.  
* **Processing (Hybrid):**  
  * **Audio:** **Modal** functions trigger on Kafka events. They pull audio from R2, run **Sam-Audio** for diarization, and push segments back to R2.  
  * **Text/Image:** The **MacBook Pro M4 Max** acts as a Kafka consumer. It pulls manuscript images, runs **olmOCR** or **Sam3** locally (leveraging the Neural Engine), and pushes extracted text to **MotherDuck**.  
* **Vectorization:** Extracted text is chunked and embedded (using Nomic or OpenAI embeddings via Z.ai), then stored in **LanceDB Cloud**.

## ---

**4\. Infrastructure Implementation: Sidero Omni & The Talos Mesh**

The project requires a stable orchestration layer to manage the heterogeneous hardware. We will use the **Sidero Omni 14-day trial** to bootstrap a Kubernetes cluster that spans Hetzner, Oracle, and the local environment.

### **4.1 Unifying the Hybrid Cluster with Talos Linux**

**Sidero Omni** simplifies Kubernetes on bare metal by managing **Talos Linux**, an immutable, API-driven OS. The 14-day trial is sufficient to establish the cluster, which can then run indefinitely in a "headless" state or be managed via CLI.

1. **Hetzner CAX41 (Control Plane):** This Arm64 server is ideal for the Kubernetes control plane due to its high network bandwidth and RAM (32GB).  
   * *Action:* Flash the server with the Talos Arm64 ISO. Since Hetzner does not natively support custom ISO uploads easily, use the "Rescue System" method: boot into rescue mode, use dd to write the Talos image to the disk, and reboot.7  
   * *Config:* Use Sidero Omni to generate the talosconfig. Apply the control plane configuration via talosctl.  
2. **Oracle Cloud Free Tier (Worker Nodes):** The Ampere Altra instances (4 OCPUs, 24GB RAM) provide excellent "always-free" compute capacity.  
   * *Action:* Provision instances with Ubuntu, then "pave over" them with Talos Linux using the kexec method or by booting from a Talos image volume.9  
   * *Role:* These nodes will host the **Confluent Kafka Connectors**, **Datadog Agents**, and the lightweight web frontend.  
3. **MacBook Pro M4 Max (Edge Node):**  
   * *Action:* While the Mac cannot run Talos bare-metal easily, it will act as an "external worker." We use **WireGuard** (integrated into Sidero Omni via KubeSpan) to tunnel the Mac into the cluster network. This allows the Mac to access internal Pod IPs (like Redis or Kafka) securely while running macOS.

### **4.2 Maximizing the 14-Day Trials**

* **Sidero Omni:** Use the trial to perform the initial, complex configuration of the Hetzner/Oracle mesh. Once the cluster is stable, export the talosconfig and kubeconfig. Talos clusters continue to function perfectly without the Omni SaaS dashboard, manageable via the talosctl CLI. This effectively locks in the cluster management value permanently.  
* **Datadog:** Enable full observability during the "Phase 1" heavy data ingestion. Use the 14 days to profile the scrapers and OCR pipeline, identifying bottlenecks. Once the trial expires, transition to a self-hosted Prometheus/Grafana stack running on the Oracle Free Tier.

## ---

**5\. Phase 1: Data Acquisition & The "Sam3" Pipeline**

The first objective is to construct a high-quality dataset of Celtic language text and audio. The resource snippets highlight **Dúchas.ie** (folklore) and **Sam3** (Segment Anything 3\) as key technologies.10

### **5.1 The OCR Challenge: Handwriting Recognition**

Dúchas contains handwritten Irish manuscripts that standard OCR (Tesseract) cannot process. We require **Handwritten Text Recognition (HTR)** and complex layout analysis.

* **Model Selection:** We will use **Sam3** 11 for layout analysis (segmenting text blocks from illustrations) and **olmOCR-2-7B** 13 for the actual text recognition.  
* **Optimization:** The snippet 13 specifically mentions mlx-community/olmOCR-2-7B-1025-mlx-8bit. This model is optimized for Apple Silicon. Running this on the **MacBook Pro M4 Max** is the most cost-efficient strategy, saving expensive cloud GPU hours for training.

### **5.2 The "Zero-Cost" OCR Workflow**

1. **Ingest:** Oracle nodes scrape Dúchas page URLs and push them to a Confluent Kafka topic duchas-pages-to-process.  
2. **Processing (Mac M4):** A local Python script on the M4 Max consumes from the Kafka topic.  
   * Downloads the image from Dúchas.  
   * Runs **Sam3** (via segment-geospatial or Meta's repo) to isolate text regions.  
   * Runs **olmOCR-2-7B (8-bit MLX)** on the regions. The M4 Max (48GB) can handle this model (approx 8GB VRAM) easily, processing pages at high speed.  
   * *Value:* Running this on the Mac saves approximately **$2/hr** in cloud GPU costs compared to running comparable H100s on Nebius.  
3. **Storage:** Extracted text is pushed to **MotherDuck** (Lite plan). Raw JSON logs are sent to **Cloudflare R2**.

### **5.3 Audio Segmentation with Sam-Audio**

For the **Canúint.ie** (Irish Dialects) dataset 14, we use **Sam-Audio** 15 to segment speakers and isolate dialect examples.

* *Implementation:* We deploy this on **Modal**. Audio processing benefits from high burst bandwidth and short processing times, which aligns with Modal's per-second billing.  
* *Workflow:* Upload audio to R2 \-\> Trigger Modal function \-\> Run Sam-Audio to Diarize \-\> Store segments in R2 \-\> Update Metadata in PlanetScale.  
* *Cost Control:* Sam-Audio is efficient. We can process hundreds of hours of audio with the $280 credit if we optimize cold starts and batch process files.

### **5.4 Leveraging Z.ai MCP Servers**

The **Z.ai Coding Plan Pro** includes access to **Vision, Search, and Reader MCP servers**.17 These are critical for augmenting the dataset.

* *Strategy:* Use the **Reader MCP** to ingest supplementary texts (Gaelic grammar guides, historical context) from the web. Use the **Search MCP** to find modern translations or academic papers related to specific folklore stories. Use the **Vision MCP** as a secondary validation for OCR results—sending difficult manuscript snippets to GLM-4.6V for "second opinion" transcription.

## ---

**6\. Phase 2: Fine-Tuning The "Celtic-LLM"**

We aim to fine-tune a model to specialize in Celtic languages, OCR correction, and cultural context. We will likely use **Qwen2.5-VL** (for multimodal capabilities) or **EuroLLM-22B** (for text proficiency).

### **6.1 Model Selection Strategy**

* EuroLLM-22B-Instruct 20: Specifically trained for European languages. It is a strong candidate for text-only fine-tuning to create a translation and reasoning engine.  
* Qwen2.5-VL 21: A multimodal model. This is the best candidate for training a model that can "read" the manuscripts directly, preserving the layout and visual context of the folklore.

### **6.2 The Training Compute Arbitrage**

We have $120 on ThunderCompute and $51 on Nebius. We must arbitrage these against each other.

* **ThunderCompute:** Offers **A100 80GB at \~$0.78/hr**.1 This is the efficiency king.  
  * *Calculation:* $120 / $0.78 ≈ **153 hours** of A100 training time.  
  * *Strategy:* Use ThunderCompute for the main **SFT (Supervised Fine-Tuning)** epochs. 153 hours is sufficient to fine-tune a 7B or even 22B model on a curated dataset of \<1B tokens.  
* **Nebius AI:** Offers H100s at \~$2.00-$2.95/hr.  
  * *Calculation:* $51 / $2.00 ≈ **25 hours**.  
  * *Strategy:* Use Nebius for **Hyperparameter Search** or short "sanity check" runs. The H100 is roughly 3x faster than the A100 for FP8 workloads. We run short experiments here to find the optimal learning rate before committing the long run to ThunderCompute.

### **6.3 Orchestration with Anyscale**

We use the **$100 Anyscale Credit** 22 to orchestrate the training process.

* *Setup:* Connect Anyscale to the ThunderCompute instances (via BYOC if supported, or manually configuring Ray).  
* *Distributed Training:* If we can cluster multiple cheap instances, we use **Ray Train** (via Anyscale) to distribute the workload, reducing wall-clock time.

## ---

**7\. Phase 3: The Agentic Workflow (Letta, Blaxel, Z.ai)**

Once the data is processed and the model is fine-tuned, we build the application layer: a "Folklore Archivist Agent."

### **7.1 Coding & Development**

We possess powerful coding assistants to accelerate development.

* **Z.ai Coding Plan Pro:** Use **GLM-4.6** 23 to generate the boilerplate code for Kafka consumers, Modal functions, and the RAG pipeline. The high prompt limit (2400 prompts/5hrs on Max) allows for rapid iteration.  
* **Claude Code Max x20:** Use the "20x" limit capability 24 for massive context tasks. Feed the entire Sidero Omni documentation and Talos Linux specs into Claude to generate the complex infrastructure-as-code configurations.

### **7.2 Agent Memory & State (Letta)**

**Letta** (25,000 credits) provides "stateful" memory.25

* *Application:* The Archivist Agent needs to remember user preferences ("I am interested in Donegal folklore") and past interactions ("Translate the story I asked about yesterday").  
* *Architecture:* When a user queries the system, the context is stored in Letta. Letta manages the "context window," deciding what to keep in active memory versus what to archive to **LanceDB**.  
* *Value:* 25,000 credits is substantial. Assuming \~1 credit per complex transaction, this supports a long-running beta or public demo.

### **7.3 Serving & Inference (Blaxel)**

**Blaxel** ($430 credit) is the primary serving platform.26

* *Deployment:* Deploy the fine-tuned **Celtic-LLM** (quantized) to Blaxel.  
* *Agent Hosting:* Blaxel supports "Agents" natively. We deploy the Letta-integrated agent here.  
* *Cost Mgmt:* Blaxel charges for active compute. By using serverless scale-to-zero, we conserve the $430 credit. For high-traffic periods, we can utilize the **Hugging Face Pro** inference endpoints ($50 credit) as a load balancer or fallback.

## ---

**8\. Maximizing Specific "Odd" Credits**

### **8.1 Google Cloud (£200): The Deep Reasoner**

* **Gemini 1.5 Pro:** Use its massive context window (1M+ tokens) to process **entire books** of folklore in a single pass. This generates high-quality summaries and structured JSON extraction that serves as "Ground Truth" for training our smaller, cheaper models.  
* **Vertex AI Search:** Use Google's managed RAG solution to index the Dúchas PDFs instantly. This provides a baseline search experience while we build our custom LanceDB implementation.

### **8.2 ElevenLabs (210,000 Characters): The Voice of the Past**

* **The "Seanchaí" Feature:** We cannot synthesize the entire archive (too expensive). Instead, we implement a "Daily Featured Story."  
* **Strategy:** Select one story per day. Generate audio using ElevenLabs (approx 5,000 chars/day). Cache the MP3 in **Cloudflare R2**.  
* *Result:* Over 40 days, we build a library of \~40 high-quality narrated stories without exceeding the 210k limit.

### **8.3 Confluent ($400): The Event Log**

* **Usage:** This budget allows for a robust Kafka cluster.  
* **Maximization:** Use it as the **Event Sourcing** log. Every single edit, OCR correction, and user query is logged to Kafka. This builds a valuable dataset for *future* Reinforcement Learning (RLHF) or DPO (Direct Preference Optimization).

### **8.4 LanceDB ($100): The Vector Store**

* **Usage:** Store vector embeddings of the Dúchas corpus.  
* **Optimization:** LanceDB Cloud is serverless.27 It scales to zero. With $100, you can store millions of vectors if query volume is moderate. This is far more cost-effective than a dedicated Pinecone index.

## ---

**9\. Technical Deep Dive: The "Fallback Chain" Logic**

To ensure high availability without burning credits unnecessarily, we implement a rigorous routing logic in the Agent (running on Blaxel).  
The Router Logic:  
A lightweight Python function (deployed on Cloudflare Workers or FastAPI on Oracle Free Tier) analyzes every incoming query.

1. **Complexity Check:** Is the query simple factual recall ("Who was Cú Chulainn?") or complex reasoning ("Compare the theme of loss in Donegal vs. Kerry folklore?")?  
2. **Route Selection:**  
   * **Low Complexity:** Route to a quantized **Llama-3-8B** hosted on the **Oracle Free Tier** (CPU inference) or the **Mac M4** (if the tunnel is active). Cost: $0.  
   * **High Complexity:** Check Blaxel credit balance.  
     * If Credits \> Threshold: Route to **Blaxel Agent (Celtic-LLM)**.  
     * If Credits \< Threshold: Route to **Google Gemini 1.5**.  
3. **Memory Integration:** Regardless of the compute node used, the interaction is logged to **Letta** to maintain continuity.

## ---

**10\. Hardware & OS Specifics: The Arm64 Advantage**

The user has a unique mix of resources that converge on the **Arm64 architecture**.

* **Hetzner CAX41:** Arm64 Ampere Altra.  
* **Oracle Free Tier:** Arm64 Ampere A1.  
* **Mac M4:** Arm64 Apple Silicon.

**Conclusion:** The entire cluster should be **Arm64 native**. This simplifies container builds immensely. We build Docker images *once* on the Mac M4 (using docker buildx), push them to **GitHub Container Registry** (free), and deploy them to Hetzner and Oracle without cross-compilation headaches.  
Talos Config for Oracle (Network Workaround):  
Oracle Cloud networking can be strict. In the Talos machine configuration, ensure kubelet utilizes the correct interface IP. Use Flannel or Cilium as the CNI. Sidero Omni handles most of this automatically, but you must ensure port 6443 is open in the Oracle Security List (VCN firewall).

## ---

**11\. Financial Run-Rate & Burn Strategy**

To ensure the project lasts 6+ months, we adhere to a strict credit burn schedule.

| Resource | Value | Burn Rate Strategy | Est. Lifespan |
| :---- | :---- | :---- | :---- |
| **ThunderCompute** | $120 | Burst usage. Only spin up A100s for specific training runs (e.g., 1 weekend/month). | 4-5 Training Runs |
| **Blaxel** | $430 | \~$2/day for idle agent \+ burst inference. | \~6-7 Months |
| **Modal** | $280 | Use only for "Serverless Functions" (OCR/Audio). High churn during Phase 1, low usage after. | Phase 1 (1 month) \+ Maint. |
| **Confluent** | $400 | \~$50/mo for standard cluster. | \~8 Months |
| **Letta** | 25k | \~100 credits/day. | \~8 Months |
| **Z.ai / Claude** | Subs | Monthly reset. Use aggressively every month. | N/A (Recurring) |
| **MotherDuck** | Lite | $25/mo (after trial). Keep data \<10GB to stay in low tier. | Indefinite (if \<10GB) |

Critical "Free" Sustainment:  
Even after all credits expire, the core platform survives:

* **Hosting:** Hetzner ($0/mo if prepaid/credits, or very cheap) \+ Oracle (Free).  
* **Data:** Cloudflare R2 (Cheap/Free tier).  
* **Inference:** Mac M4 Max (Local/Tunnel) takes over as the primary node if Blaxel credits dry up.

## ---

**12\. Conclusion & Implementation Roadmap**

This strategy transforms a "bag of credits" into a cohesive, cutting-edge AI platform. By leveraging the **MacBook Pro M4 Max** for "base load" inference and OCR, we preserve the precious GPU cloud credits (ThunderCompute/Nebius) for high-value model training. **Sidero Omni** and **Talos** provide the robust substrate, while **Letta** and **Blaxel** enable cutting-edge agentic capabilities.  
**Immediate Action Items:**

1. **Days 1-3:** Install **Sidero Omni** on Hetzner CAX41 and Oracle Free Tier. Establish the Kubernetes control plane.  
2. **Days 4-7:** Deploy **Confluent** Kafka and **MotherDuck**. Write the **Z.ai** generated scrapers for Dúchas.ie.  
3. **Days 7-14:** Run the **Sam3** OCR pipeline on the **Mac M4 Max**, feeding data to MotherDuck. Utilize **Datadog** trial to monitor throughput.  
4. **Week 3:** Spin up **ThunderCompute** A100s. Fine-tune **Qwen2.5-VL** on the captured OCR data.  
5. **Month 2:** Deploy the **Letta**\-enabled Agent on **Blaxel**. Open public access via **Cloudflare** frontend.

The **Celtic Heritage Intelligence Platform** will not only serve as a technological showcase but also as a permanent contribution to the preservation of Gaelic culture, fully maximizing the user's resource inventory.

## ---

**13\. Detailed YAML Configurations & Experiment Guide**

This section fulfills the requirement for "Detailed YAML configuration" and the "GPU Experiment Guide."

### **13.1 MLX Configuration for Mac M4 Max**

Use this config with mlx-lm to serve the quantization-optimized model on the Mac.

YAML

\# mlx\_config.yaml  
model:  
  name: "mlx-community/olmOCR-2-7B-1025-mlx-8bit"  
  trust\_remote\_code: true  
generation:  
  max\_tokens: 1024  
  temp: 0.0  
  repetition\_penalty: 1.1  
system\_prompt: |  
  You are an expert archivist of Irish folklore.  
  Transcribe the following handwritten manuscript text exactly as it appears.  
  Maintain original spelling, even if archaic.

### **13.2 Fallback Chain Configuration (Blaxel/Letta)**

This conceptual config illustrates the routing logic for the agent.

YAML

\# agent\_router.yaml  
fallback\_chain:  
  \- priority: 1  
    name: "local\_edge"  
    endpoint: "http://\<mac-wireguard-ip\>:8080/v1/chat/completions"  
    condition: "complexity \== 'low' AND available \== true"  
  \- priority: 2  
    name: "blaxel\_cloud"  
    endpoint: "https://api.blaxel.ai/v1/inference"  
    model: "celtic-llm-v1"  
    condition: "complexity \== 'high' OR local\_edge.available \== false"  
  \- priority: 3  
    name: "google\_api"  
    endpoint: "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro"  
    condition: "task \== 'multimodal\_reasoning' OR blaxel.error \== true"

### **13.3 GPU Experiment Guide (ThunderCompute)**

Follow this protocol to maximize the $120 credit.

1. **Preparation (Local):** Prepare the dataset on the Mac. Convert images to parquet/arrow format. Upload to Cloudflare R2.  
2. **Environment (Nebius \- $2/hr):** Spin up a cheap GPU. Verify the Unsloth environment. Run 1 epoch on 1% of the data. Check loss curve. If it looks good, terminate.  
3. **Execution (ThunderCompute \- $0.78/hr):**  
   * Launch A100 instance.  
   * Mount R2 with rclone.  
   * Run unsloth\_train.py with the validated config.  
   * *Monitor:* Use anyscale or wandb to track progress.  
   * *Terminate:* Implement an auto-shutdown script (shutdown \-h now) triggered by training completion to avoid paying for idle seconds.

This structured approach ensures that every resource is used exactly where it provides the most value, turning a disparate collection of tools into a powerful, unified platform.

#### **Works cited**

1. Pricing | Thunder Compute, accessed December 20, 2025, [https://www.thundercompute.com/pricing](https://www.thundercompute.com/pricing)  
2. Nebius | Review, Pricing & Alternatives \- GetDeploying, accessed December 20, 2025, [https://getdeploying.com/nebius](https://getdeploying.com/nebius)  
3. A complete guide to Modal AI pricing in 2025 \- eesel AI, accessed December 20, 2025, [https://www.eesel.ai/blog/modal-ai-pricing](https://www.eesel.ai/blog/modal-ai-pricing)  
4. Pricing \- Blaxel, accessed December 20, 2025, [https://blaxel.ai/pricing](https://blaxel.ai/pricing)  
5. Talos ISO directly from hetzner \- Reddit, accessed December 20, 2025, [https://www.reddit.com/r/hetzner/comments/1l8kx3g/talos\_iso\_directly\_from\_hetzner/](https://www.reddit.com/r/hetzner/comments/1l8kx3g/talos_iso_directly_from_hetzner/)  
6. Hetzner \- Sidero Documentation \- What is Talos Linux?, accessed December 20, 2025, [https://docs.siderolabs.com/talos/v1.7/platform-specific-installations/cloud-platforms/hetzner](https://docs.siderolabs.com/talos/v1.7/platform-specific-installations/cloud-platforms/hetzner)  
7. Oracle \- Sidero Documentation \- What is Talos Linux?, accessed December 20, 2025, [https://docs.siderolabs.com/talos/v1.9/platform-specific-installations/cloud-platforms/oracle](https://docs.siderolabs.com/talos/v1.9/platform-specific-installations/cloud-platforms/oracle)  
8. Schools Collection, accessed December 20, 2025, [https://www.arcgis.com/apps/Viewer/index.html?appid=a61878ae45164ccabdf36c1e3ad4857a](https://www.arcgis.com/apps/Viewer/index.html?appid=a61878ae45164ccabdf36c1e3ad4857a)  
9. \[2511.16719\] SAM 3: Segment Anything with Concepts \- arXiv, accessed December 20, 2025, [https://arxiv.org/abs/2511.16719](https://arxiv.org/abs/2511.16719)  
10. SAM3 by Meta: Text-Prompted Image Segmentation Tutorial \- Codecademy, accessed December 20, 2025, [https://www.codecademy.com/article/sam-3-by-meta-text-prompted-image-segmentation-tutorial](https://www.codecademy.com/article/sam-3-by-meta-text-prompted-image-segmentation-tutorial)  
11. mlx-community/olmOCR-2-7B-1025-mlx-8bit \- Hugging Face, accessed December 20, 2025, [https://huggingface.co/mlx-community/olmOCR-2-7B-1025-mlx-8bit](https://huggingface.co/mlx-community/olmOCR-2-7B-1025-mlx-8bit)  
12. Project information, accessed December 20, 2025, [https://www.canuint.ie/en/info/about-this-website/project-information/](https://www.canuint.ie/en/info/about-this-website/project-information/)  
13. Segment Anything adds audio as Meta unveils SAM Audio | Digital Watch Observatory, accessed December 20, 2025, [https://dig.watch/updates/segment-anything-adds-audio-as-meta-unveils-sam-audio](https://dig.watch/updates/segment-anything-adds-audio-as-meta-unveils-sam-audio)  
14. facebook/sam-audio-large \- Hugging Face, accessed December 20, 2025, [https://huggingface.co/facebook/sam-audio-large](https://huggingface.co/facebook/sam-audio-large)  
15. Vision MCP Server \- Z.AI DEVELOPER DOCUMENT, accessed December 20, 2025, [https://docs.z.ai/devpack/mcp/vision-mcp-server](https://docs.z.ai/devpack/mcp/vision-mcp-server)  
16. Web Search MCP Server \- Z.AI DEVELOPER DOCUMENT, accessed December 20, 2025, [https://docs.z.ai/devpack/mcp/search-mcp-server](https://docs.z.ai/devpack/mcp/search-mcp-server)  
17. Web Reader MCP Server \- Z.AI DEVELOPER DOCUMENT, accessed December 20, 2025, [https://docs.z.ai/devpack/mcp/reader-mcp-server](https://docs.z.ai/devpack/mcp/reader-mcp-server)  
18. utter-project/EuroLLM-22B-Instruct-2512 \- Hugging Face, accessed December 20, 2025, [https://huggingface.co/utter-project/EuroLLM-22B-Instruct-2512](https://huggingface.co/utter-project/EuroLLM-22B-Instruct-2512)  
19. Qwen/Qwen3-VL-30B-A3B-Instruct-GGUF \- Hugging Face, accessed December 20, 2025, [https://huggingface.co/Qwen/Qwen3-VL-30B-A3B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen3-VL-30B-A3B-Instruct-GGUF)  
20. Anyscale pricing 2025: A clear breakdown of costs & models \- eesel AI, accessed December 20, 2025, [https://www.eesel.ai/blog/anyscale-pricing](https://www.eesel.ai/blog/anyscale-pricing)  
21. GLM Coding Plan powered by GLM-4.6 \- Z.ai Chat, accessed December 20, 2025, [https://z.ai/subscribe?cc=fission\_glmcode\_sub\_v1\&ic=ZWUCN21VZK\&n=Vipin](https://z.ai/subscribe?cc=fission_glmcode_sub_v1&ic=ZWUCN21VZK&n=Vipin)  
22. About Claude's Max Plan Usage, accessed December 20, 2025, [https://support.claude.com/en/articles/11014257-about-claude-s-max-plan-usage](https://support.claude.com/en/articles/11014257-about-claude-s-max-plan-usage)  
23. Pricing \- Letta, accessed December 20, 2025, [https://www.letta.com/pricing](https://www.letta.com/pricing)  
24. AI Cloud Pricing | GPU Compute & AI Infrastructure | Lambda, accessed December 20, 2025, [https://lambda.ai/pricing](https://lambda.ai/pricing)  
25. Serverless Vector Search \- LanceDB Cloud, accessed December 20, 2025, [https://lancedb.com/docs/cloud/](https://lancedb.com/docs/cloud/)