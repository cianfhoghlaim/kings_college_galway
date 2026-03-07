# **Architectural Convergence: Orchestrating Skyvern, Crawl4AI, and Stagehand for Semantic Web Mapping and Data Extraction in Irish Educational Repositories**

## **1\. The Paradigm Shift in Autonomous Data Acquisition**

The discipline of web data acquisition is undergoing a fundamental architectural transformation, shifting from rigid, selector-based scripting to autonomous, agentic workflows capable of semantic reasoning and visual interpretation. This report presents an exhaustive technical analysis of integrating three avant-garde technologies—Skyvern, Crawl4AI, and Stagehand—within a containerized Docker ecosystem to address complex data acquisition challenges. The specific application domain for this architectural blueprint is the digital estate of the Irish education system: the State Examinations Commission (*examinations.ie*), the National Council for Curriculum and Assessment (*ncca.ie*), and *curriculumonline.ie*. These targets represent a microcosm of the broader web scraping challenge, featuring a heterogeneous mix of legacy form-based architectures, modern dynamic content management systems (CMS), and deeply nested hierarchical data structures.

### **1.1 The Limitations of Deterministic Extraction**

Historically, extracting data from repositories like the *examinations.ie* archive relied on deterministic logic: inspecting the Document Object Model (DOM), identifying CSS selectors or XPath coordinates, and hard-coding extraction scripts. This approach is inherently brittle. A minor update to the frontend framework, such as a shift from distinct IDs to dynamic, obfuscated class names (common in React or Tailwind implementations), renders the extraction pipeline obsolete.1 Furthermore, legacy sites often employ non-standard form submission behaviors—such as the dependent dropdowns found on *examinations.ie*—where the availability of a "Subject" field depends on the state of a "Year" field, managed via opaque server-side session states rather than clean RESTful APIs.3  
The emergence of Large Language Models (LLMs) and Multimodal AI has enabled a new "Hunter-Gatherer-Operator" paradigm. In this model, the scraping infrastructure does not merely execute instructions; it perceives the interface, reasons about navigation paths, and dynamically adapts to structural volatility. This report posits that a hybrid architecture optimizes the trade-off between the high computational cost of reasoning agents and the efficiency of bulk extraction.

### **1.2 The Hybrid "Swarm" Architecture**

The proposed architecture orchestrates three specialized tools, each occupying a distinct functional niche within the data pipeline:

* **The Semantic Navigator (Skyvern):** Acting as the "Hunter," Skyvern utilizes Computer Vision and LLMs to navigate complex, dynamic interfaces and map site topography without reliance on brittle DOM selectors.2 It is responsible for the "Mapping" phase—discovering the "unknown unknowns" of the target site's structure.  
* **The Precision Operator (Stagehand):** Serving as the "Operator," Stagehand bridges the gap between high-level reasoning and low-level execution. It leverages Playwright with an AI command layer to perform specific, state-dependent interactions—such as expanding accordion menus on *curriculumonline.ie*—that require more intelligence than a raw crawler but less overhead than a full autonomous agent.5  
* **The High-Throughput Gatherer (Crawl4AI):** Functioning as the "Gatherer," Crawl4AI employs asynchronous, adaptive crawling strategies to perform high-speed, bulk content extraction from the coordinates identified by Skyvern and Stagehand.7  
* **The Schema Enforcer (BAML):** Deployed as the "Structurer," BAML (Browser-Augmented Modeling Language) enforces strict schema compliance, transforming the unstructured, probabilistic outputs of the LLMs into deterministic, type-safe JSON artifacts suitable for database ingestion.9

This report details the complete Docker Compose orchestration required to bind these disparate services into a unified, self-healing pipeline. It explores the technical nuances of handling legacy form submissions, hierarchical traversal of curriculum standards, and document repository mapping, ensuring long-term system resilience against layout volatility.

## ---

**2\. Infrastructure Orchestration: The Docker Ecosystem**

The deployment of a multi-agent scraping infrastructure requires a robust orchestration layer to manage dependencies, networking, and state persistence. Docker Compose serves as the substrate for this architecture, enabling the isolation of the Skyvern management services while allowing seamless communication with custom extractor containers running Crawl4AI and Stagehand.

### **2.1 Service Decomposition and Container Strategy**

Traditional scraping architectures are often monolithic, where a single script handles navigation, extraction, and parsing. This coupling creates a single point of failure: if the navigation logic breaks due to a UI change, the extraction logic is unreachable. The "Swarm" architecture decouples these concerns into distinct services.  
The architecture comprises two primary service groups:

1. **The Skyvern Ecosystem:** A set of interdependent services (Server, Worker, UI, Database) that manage the autonomous agents.  
2. **The Controller Ecosystem:** A custom container housing the orchestration logic, Stagehand, Crawl4AI, and BAML, acting as the "brain" that dispatches tasks to Skyvern and processes the results.

#### **2.1.1 The Skyvern Service Mesh**

The Skyvern architecture is complex, requiring a PostgreSQL database for state persistence (workflow history, account data) and a Redis instance for task queuing between the API server and the browser workers.11

* **skyvern-server:** This service exposes the REST API (defaulting to port 8000). It acts as the command-and-control center, receiving task requests from the Controller, managing the task queue, and interacting with the configured LLM provider (e.g., OpenAI, Azure, Anthropic).11 It is stateless but depends heavily on the database connection.  
* **skyvern-worker:** This is the execution unit. It spins up headless browser instances (Playwright/Chromium) to perform the actions dictated by the Server. The worker requires significant resource allocation (CPU and RAM) to handle the Computer Vision processing (analyzing screenshots to identify interactive elements) and the DOM analysis required for self-correction.4  
* **skyvern-ui:** A React-based frontend (port 8080\) that allows human operators to visualize ongoing tasks, review recordings of completed workflows, and debug failures.1

#### **2.1.2 The Controller Service (Custom Integration)**

The scraping-controller is a custom-built Docker service. Unlike the pre-packaged Skyvern images, this container is built from a Dockerfile that installs the specific Python dependencies for the "Gatherer" and "Operator" roles: crawl4ai, stagehand-sdk, baml-py, and the skyvern client library.  
Critically, because both Crawl4AI and Stagehand rely on Playwright, this container must include the necessary system dependencies and browser binaries. A standard Python image is insufficient; the Dockerfile must execute playwright install \--with-deps chromium to ensure the headless browsers can launch successfully within the containerized environment.13

### **2.2 Advanced Networking Configuration**

A critical requirement of this integration is the seamless communication between the custom Controller script and the Skyvern API. In a standard Docker setup, containers are isolated. To bridge them, we utilize a user-defined Bridge Network.15  
The docker-compose.yml defines a network named scraping-network. All services—Skyvern Server, Worker, Database, and the custom Controller—are attached to this network. This enables Docker's internal DNS resolution. The Controller can send HTTP requests to http://skyvern-server:8000 rather than relying on fragile host IP addresses or exposing the API to the public internet.  
Furthermore, managing environment variables is paramount for security and configuration flexibility. The .env file strategy allows for the injection of sensitive credentials—such as OPENAI\_API\_KEY, BROWSERBASE\_API\_KEY, and database passwords—into the containers at runtime without hardcoding them in the image.11 This is particularly important for Skyvern, which supports multiple LLM providers (Azure, Bedrock, Ollama) via specific environment flags (e.g., ENABLE\_AZURE=true).11

### **2.3 Comprehensive Docker Compose Implementation**

The following YAML configuration orchestrates the entire stack, defining the dependencies, health checks, and network topology required for the unified pipeline.

YAML

version: '3.8'

services:  
  \# \---------------------------------------------------------------------------  
  \# PERSISTENCE LAYER  
  \# \---------------------------------------------------------------------------  
  db:  
    image: postgres:14  
    volumes:  
      \- postgres\_data:/var/lib/postgresql/data  
    environment:  
      POSTGRES\_USER: skyvern  
      POSTGRES\_PASSWORD: ${SKYVERN\_DB\_PASSWORD}  
      POSTGRES\_DB: skyvern  
    healthcheck:  
      test:  
      interval: 5s  
      timeout: 5s  
      retries: 5  
    networks:  
      \- scraping-network

  redis:  
    image: redis:stack-server  
    restart: always  
    networks:  
      \- scraping-network

  \# \---------------------------------------------------------------------------  
  \# SKYVERN CORE SERVICES (The Hunter)  
  \# \---------------------------------------------------------------------------  
  skyvern-server:  
    image: skyvern/skyvern:latest  
    container\_name: skyvern-server  
    depends\_on:  
      db:  
        condition: service\_healthy  
      redis:  
        condition: service\_started  
    environment:  
      \- DATABASE\_STRING=postgresql://skyvern:${SKYVERN\_DB\_PASSWORD}@db:5432/skyvern  
      \- REDIS\_URL=redis://redis:6379  
      \- OPENAI\_API\_KEY=${OPENAI\_API\_KEY}  
      \- ENABLE\_OPENAI=true  
      \- BROWSER\_TYPE=chromium  
    ports:  
      \- "8000:8000"  
    networks:  
      \- scraping-network

  skyvern-worker:  
    image: skyvern/skyvern:latest  
    container\_name: skyvern-worker  
    command: /app/scripts/run\_worker.sh  
    depends\_on:  
      \- skyvern-server  
    environment:  
      \- DATABASE\_STRING=postgresql://skyvern:${SKYVERN\_DB\_PASSWORD}@db:5432/skyvern  
      \- REDIS\_URL=redis://redis:6379  
      \- OPENAI\_API\_KEY=${OPENAI\_API\_KEY}  
    shm\_size: '2gb' \# Essential for browser stability  
    networks:  
      \- scraping-network

  skyvern-ui:  
    image: skyvern/skyvern-ui:latest  
    ports:  
      \- "8080:8080"  
    environment:  
      \- NEXT\_PUBLIC\_API\_URL=http://localhost:8000  
    depends\_on:  
      \- skyvern-server  
    networks:  
      \- scraping-network

  \# \---------------------------------------------------------------------------  
  \# CONTROLLER SERVICE (The Gatherer & Operator)  
  \# \---------------------------------------------------------------------------  
  scraping-controller:  
    build:   
      context:./controller  
      dockerfile: Dockerfile  
    container\_name: controller  
    volumes:  
      \-./data\_artifacts:/app/data  
      \-./controller:/app/src  
    environment:  
      \- SKYVERN\_API\_URL=http://skyvern-server:8000  
      \- OPENAI\_API\_KEY=${OPENAI\_API\_KEY}  
      \- BROWSERBASE\_API\_KEY=${BROWSERBASE\_API\_KEY} \# For Stagehand cloud offload  
      \- POSTGRES\_CONNECTION=postgresql://skyvern:${SKYVERN\_DB\_PASSWORD}@db:5432/skyvern  
    depends\_on:  
      \- skyvern-server  
    networks:  
      \- scraping-network

networks:  
  scraping-network:  
    driver: bridge

volumes:  
  postgres\_data:

This configuration ensures that the scraping-controller waits for skyvern-server, which in turn waits for db to be healthy. The shm\_size: '2gb' directive for the worker is a critical optimization; standard Docker shared memory limits (64MB) are often insufficient for modern browsers rendering complex pages, leading to "Aw, Snap\!" crashes during heavy scraping sessions.11

## ---

**3\. The Hunter: Semantic Mapping with Skyvern**

Skyvern distinguishes itself from traditional scrapers by using Computer Vision to interact with web elements rather than code selectors. This makes it the ideal tool for the "Mapping" phase of the operation—establishing the landscape of the target site. For the Irish education repositories, which contain legacy markup and dynamic hierarchies, Skyvern acts as the scout that identifies where the valuable data resides.

### **3.1 Vision-Based Navigation Logic**

Traditional tools (Selenium, raw Playwright) fail when CSS classes are obfuscated or dynamic. Skyvern's agent takes a screenshot, segments the image to identify interactive elements (buttons, inputs), and uses an LLM to decide which element corresponds to the user's intent.2  
Application to Examinations.ie:  
The State Examinations Commission website (examinations.ie) features a legacy interface for its "Exam Material Archive." This section is notoriously difficult for standard scrapers because it relies on a dependent dropdown system: the "Exam" dropdown is often disabled or empty until a "Year" is selected, and the "Subject" dropdown depends on the "Exam" selection. Furthermore, the submission mechanism often lacks a clean GET parameter structure, relying on POST requests with hidden session tokens.3  
Skyvern handles this via a "Planner-Actor-Validator" loop.4 The Planner agent decomposes the high-level goal ("Find 2023 Leaving Certificate Math Papers") into steps. The Actor agent executes the click on the "2023" option in the Year dropdown. Crucially, the Validator agent visually confirms that the interface has updated—that the "Subject" dropdown is now enabled—before proceeding. A code-based script might fire the next event too early, causing a failure. Skyvern's visual grounding makes it resilient to the site's latency or specific AJAX loading behaviors.

### **3.2 The NavigationV2 Block and Multi-Goal Reasoning**

For mapping *ncca.ie*, which is structured as a modern CMS with deep nesting (Cycle \-\> Subject \-\> Documents), we utilize Skyvern's NavigationV2Block.18 This block allows for complex, multi-goal prompts. We do not use Skyvern to download every PDF (which is slow); we use it to *find* the PDF repositories.  
The prompt strategy here is "Map and Report." We instruct Skyvern to navigate to the "Senior Cycle" section and identifying the URL patterns for subject specifications.  
**Skyvern Prompt Strategy:**  
"Navigate to the 'Senior Cycle' section. Identify the list of all available subjects. For each subject, locate the 'Curriculum Specification' page. Do not download the files. Return a JSON list of the URLs for the Specification pages." 19  
This offloads the complex traversal logic. Skyvern handles the cookie banners, the "Read More" expansions, and the navigation hierarchy. The output is a structured JSON artifact containing the direct URLs where the data lives.

### **3.3 API Integration for Mapping**

The scraping-controller initiates this mapping process via Skyvern's REST API. This integration pattern allows the Python script to trigger an autonomous agent and wait for the results asynchronously.

Python

import requests  
import json  
import time

SKYVERN\_URL \= "http://skyvern-server:8000/api/v1"  
HEADERS \= {"x-api-key": "YOUR\_KEY"}

def map\_examinations\_archive(year):  
    """  
    Triggers a Skyvern agent to map the available subjects for a specific year.  
    Returns the task\_id for polling.  
    """  
    payload \= {  
        "url": "https://www.examinations.ie/exammaterialarchive/",  
        "prompt": f"Select Year '{year}' from the dropdown. Wait for the Exam dropdown to update. Select 'Leaving Certificate'. Wait for the Subject dropdown to update. Extract the text of all available options in the Subject dropdown.",  
        "data\_extraction\_schema": {  
            "type": "object",  
            "properties": {  
                "subjects": {  
                    "type": "array",  
                    "items": {"type": "string"},  
                    "description": "List of all subject names found in the dropdown"  
                }  
            }  
        },  
        "max\_steps": 15  
    }  
      
    try:  
        response \= requests.post(f"{SKYVERN\_URL}/tasks", json=payload, headers=HEADERS)  
        response.raise\_for\_status()  
        return response.json()\['run\_id'\]  
    except requests.exceptions.RequestException as e:  
        print(f"Failed to trigger Skyvern mapping: {e}")  
        return None

This request demonstrates the power of the "Hunter" model. We do not write code to handle the dropdown event listeners. We simply describe the *intent* to Skyvern, passing a schema to ensure we get a clean list of strings back.20

## ---

**4\. The Operator: Precision Interaction with Stagehand**

While Skyvern is powerful, it is relatively slow and costly due to the heavy use of Vision LLMs for every step. Stagehand provides a middle ground: it is more controllable and faster for specific, repeatable interactions. It serves as the "Operator" in our architecture, handling tasks that require a sequence of actions too complex for a crawler but too repetitive for a full agent.5

### **4.1 Bridging the Gap: Stagehand's Primitives**

Stagehand introduces four primitives: act, extract, observe, and agent.22 The act primitive is particularly valuable for the Irish curriculum sites.  
Case Study: Curriculumonline.ie  
The curriculumonline.ie website presents learning outcomes in "Strands" and "Elements." These are often hidden behind interactive accordion menus or tabs. A simple HTML crawler (like Crawl4AI) downloading the initial page load would miss this content because it is not in the DOM until a user interaction triggers the expansion.  
Using Skyvern for this would be overkill if we need to scrape 50 subject pages. Stagehand allows us to write a "Precision Interaction" script. We can instruct Stagehand to "Expand all strands" using natural language, and it will find the appropriate buttons and click them.

### **4.2 Caching for Deterministic Performance**

One of Stagehand's most significant advantages is its caching mechanism. When page.act("Click the 'Strand 1' button") is executed for the first time, Stagehand uses an LLM to analyze the DOM and find the selector. Once found, it caches this selector.6  
For *curriculumonline.ie*, where the HTML structure is consistent across different subjects (e.g., Mathematics, English, Science all share the same template), this means:

1. **First Run:** High latency (LLM analysis to find the "Expand" buttons).  
2. **Runs 2-50:** Near-native speed. Stagehand reuses the cached selector from the first run to click the buttons on the subsequent pages without invoking the LLM.

This "learning" capability drastically reduces the cost and time of the scraping operation compared to pure agentic approaches.

### **4.3 Implementing the Operator Loop**

The Controller script utilizes Stagehand to prepare the page state before handing off to the Gatherer.

Python

from stagehand import Stagehand

async def prepare\_curriculum\_page(url, browserbase\_key=None):  
    """  
    Uses Stagehand to interact with the page and reveal hidden content.  
    """  
    stagehand \= Stagehand(api\_key=browserbase\_key)  
    await stagehand.init()  
    page \= stagehand.page  
      
    await page.goto(url)  
      
    \# Observe the page to find interactive elements  
    \# This helps in debugging what the AI 'sees'  
    observations \= await page.observe("accordion headers for curriculum strands")  
      
    \# Act to reveal content. This action will be cached.  
    \# The prompt is natural language, but the execution becomes deterministic.  
    await page.act("Click all accordion headers to expand the strand details")  
      
    \# Once expanded, we can either extract here or hand off to Crawl4AI  
    \# For complex interaction \+ extraction, Stagehand's extract is useful:  
    content \= await page.extract(  
        "Extract all learning outcomes visible on the page",  
        schema=LearningOutcomesSchema \# Pydantic model  
    )  
      
    await stagehand.close()  
    return content

This snippet illustrates the "Operator" role: entering the page, manipulating the state (expanding accordions), and potentially extracting data that requires that specific state.23

## ---

**5\. The Gatherer: Bulk Extraction with Crawl4AI**

Once Skyvern has mapped the URLs and Stagehand has identified the interaction patterns, Crawl4AI is deployed for the "Gathering" phase. Crawl4AI is optimized for speed, token efficiency, and bulk processing, acting as the high-throughput engine of the architecture.

### **5.1 Asynchronous Adaptive Crawling**

Crawl4AI runs within the scraping-controller container. It is configured to run in headless mode and uses aggressive caching strategies. The AsyncWebCrawler class allows for concurrent processing of multiple URLs, which is essential when scraping the hundreds of documents identified by Skyvern.8  
Configuration for NCCA.ie:  
The ncca.ie website is content-heavy. When scraping hundreds of policy documents and research papers, efficiency is key. Crawl4AI's CrawlerRunConfig is tuned to ignore irrelevant assets.

Python

from crawl4ai import AsyncWebCrawler, CacheMode, CrawlerRunConfig

run\_config \= CrawlerRunConfig(  
    cache\_mode=CacheMode.ENABLED,  \# Persist cache to disk/memory  
    word\_count\_threshold=50,       \# Ignore "empty" or redirect pages  
    magic=True,                    \# Enable anti-bot heuristic protections  
    verbose=True  
)

The magic=True parameter is a high-level wrapper that attempts to mimic human browser fingerprints, handling basic anti-bot measures that might be present on government servers.13

### **5.2 Intelligent Markdown and Content Filtering**

A standout feature of Crawl4AI is "Fit Markdown." Standard scraping often returns HTML cluttered with navigation bars, footers, and copyright notices—noise that dilutes the signal for LLM processing. Crawl4AI integrates algorithms like Pruning and BM25 directly into the extraction pipeline.25  
BM25 Filtering for Curriculum Standards:  
When scraping a page on curriculumonline.ie, we are interested specifically in the "Learning Outcomes" and not the general site navigation. We can configure Crawl4AI to prioritize content relevance.

Python

from crawl4ai.markdown\_generation\_strategy import DefaultMarkdownGenerator

md\_generator \= DefaultMarkdownGenerator(  
    content\_filter="bm25",  
    options={  
        "user\_query": "learning outcomes assessment strands",  
        "bm25\_threshold": 1.2  
    }  
)

This generates "Fit Markdown"—a condensed version of the page content that retains only the sections semantically related to the query. This significantly reduces the token count (and thus cost) when this text is subsequently sent to BAML for strict parsing.25

### **5.3 Hybrid Extraction Strategies: CSS vs. LLM**

Crawl4AI supports a Strategy Pattern for extraction, allowing the developer to choose between deterministic CSS/XPath selectors and probabilistic LLM extraction.26

* **JsonCssExtractionStrategy:** Used when the structure is known and stable. For example, if Skyvern identifies that all PDF links on *examinations.ie* are inside a table with class exam-table, we define a CSS schema to extract them instantly without AI cost.27  
* **LLMExtractionStrategy:** Used for *ncca.ie* research reports where the structure varies. We pass the "Fit Markdown" to an LLM strategy that instructs the model to "Extract the publication date, authors, and executive summary".29

## ---

**6\. The Structurer: Schema Enforcement with BAML**

The output from Skyvern (screenshots/logs) and Crawl4AI (Markdown) is often unstructured. To integrate this data into a database, it must be normalized. BAML (Browser-Augmented Modeling Language) serves as the "Type-Safety Layer," enforcing strict schemas on the output of the LLMs.9

### **6.1 The Problem of Probabilistic JSON**

LLMs are non-deterministic. Asking GPT-4o to "extract the exam year" might return {"year": "2023"} in one run and {"Year": 2023} or {"exam\_year": "Two Thousand Twenty-Three"} in another. This variability breaks downstream data pipelines. BAML addresses this by treating the prompt as a function with a strict return type, using a Rust-based parser to coerce the LLM's output into the defined schema.30

### **6.2 Defining Domain-Specific Schemas**

In the Controller container, we define .baml files that map the domain objects of the Irish education system. This schema engineering is the counterpart to prompt engineering.  
**BAML Schema for Exam Papers (examinations.ie):**

Code snippet

// Define the data model for an Exam Paper  
class ExamPaper {  
  subject: string @description("The subject name, e.g., Mathematics")  
  year: int @description("The year of the examination, as a 4-digit integer")  
  level: ExamLevel @description("The academic level of the paper")  
  type: PaperType @description("Whether it is a Question Paper or Marking Scheme")  
  url: string @description("The direct download link to the PDF")  
}

// Enforce strict Enum values  
enum ExamLevel {  
  Higher  
  Ordinary  
  Foundation  
  Common  
}

enum PaperType {  
  QuestionPaper  
  MarkingScheme  
  Aural  
}

// The extraction function  
function ExtractExamDetails(raw\_text: string) \-\> ExamPaper {  
  client GPT4o  
  prompt \#"  
    Analyze the following text from the exam archive and extract a list of exam papers.  
    Ensure the 'year' is an integer and 'level' matches the Enum.  
      
    Input Text:  
    {{ raw\_text }}  
  "\#  
}

### **6.3 BAML Client Integration**

The BAML compiler generates a Python client (baml\_client) that is imported into the Controller script. When Crawl4AI returns the raw markdown of a page, the script passes that string to the BAML function.

Python

from baml\_client import b  
from baml\_client.types import ExamPaper

\# Crawl4AI gets the text  
markdown\_content \= crawl\_result.markdown.fit\_markdown

\# BAML structures it  
try:  
    exam\_papers: List\[ExamPaper\] \= b.ExtractExamDetails(markdown\_content)  
    for paper in exam\_papers:  
        save\_to\_database(paper)  
except Exception as e:  
    \# BAML's Rust parser handles most JSON errors, but we catch logic errors here  
    log\_error(e)

This integration ensures that the database receives clean, integer-based years and standardized Enum strings, enabling robust SQL querying (e.g., SELECT \* FROM papers WHERE level \= 'Higher').10

## ---

**7\. Case Study Implementation: The Irish Educational Data Estate**

The convergence of these technologies allows for tailored workflows for each of the target websites, addressing their unique structural challenges.

### **7.1 Workflow 1: The Legacy Fortress (Examinations.ie)**

* **Challenge:** The *examinations.ie* archive uses a legacy form where the "Subject" dropdown is dynamically populated only after "Year" and "Exam" are selected. There are no direct links to the result pages.  
* **Orchestration:**  
  1. **Skyvern (Hunter):** The Controller triggers a Skyvern task with a visual prompt: "Select '2023', wait for update, select 'Leaving Cert', wait for update. Click 'Subject' dropdown and extract all option text."  
  2. **Controller Logic:** The script parses the list of subjects returned by Skyvern.  
  3. **Stagehand (Operator):** For each subject, Stagehand is tasked to "Select and click Search." It handles the form submission sequence.  
  4. **Crawl4AI (Gatherer):** Once the results page loads (containing the list of PDFs), Stagehand passes the page HTML to Crawl4AI's JsonCssExtractionStrategy to rip the PDF links from the results table.  
  5. **BAML (Structurer):** Not strictly needed for the table structure (CSS is faster), but used to normalize the filenames into metadata (e.g., inferring "Higher Level" from "EV" in LC003ALP000EV.pdf).

### **7.2 Workflow 2: The Hierarchical Maze (Curriculumonline.ie)**

* **Challenge:** Data is nested deep within "Subject" \-\> "Strand" \-\> "Learning Outcome." Content is hidden behind interactive "Expand" buttons.  
* **Orchestration:**  
  1. **Skyvern (Hunter):** Maps the homepage to find the URLs for all "Senior Cycle" subjects. Returns a JSON list of URLs.  
  2. **Stagehand (Operator):** Visits each subject URL. Uses act("Click 'Expand All' on the curriculum strands") to reveal the hidden text. The caching mechanism makes this extremely fast after the first few subjects.  
  3. **Crawl4AI (Gatherer):** Snapshots the DOM *after* Stagehand has expanded the content. Generates "Fit Markdown."  
  4. **BAML (Structurer):** Parses the Markdown into a nested Curriculum object, linking "Strands" to their specific "Learning Outcomes."

### **7.3 Workflow 3: The Document Repository (NCCA.ie)**

* **Challenge:** Broad, deep CMS with thousands of unstructured PDF reports.  
* **Orchestration:**  
  1. **Skyvern (Hunter):** Navigates the high-level menus ("Research," "Publications") to find the pagination controls and category filters.  
  2. **Crawl4AI (Gatherer):** Uses BFSDeepCrawlStrategy (Breadth-First Search) to crawl the publication listings. It filters for hrefs ending in .pdf.  
  3. **BAML (Structurer):** We download the PDF (using Crawl4AI or a simple requests call) and extract its text. BAML is then used with a large context window model (like Claude 3.5 Sonnet) to "Summarize the findings" and "Extract the publication date and authors" from the raw PDF text, creating a searchable metadata index for the documents.

## ---

**8\. Strategic Implications and Feature Comparison**

The decision to use a multi-agent stack versus a single tool involves strategic trade-offs regarding cost, speed, and reliability.

### **8.1 Comparative Analysis of the Swarm Components**

| Feature | Skyvern | Stagehand | Crawl4AI |
| :---- | :---- | :---- | :---- |
| **Primary Role** | Semantic Navigation (Hunter) | Precision Interaction (Operator) | Bulk Extraction (Gatherer) |
| **Technology** | Vision LLM \+ Playwright | LLM \+ Playwright \+ Caching | Async Playwright \+ Algorithms |
| **Cost Profile** | High (Vision Tokens per step) | Medium (LLM per unique action) | Low (Local compute) |
| **Reliability** | Excellent (Visual adaptation) | High (Self-healing selectors) | Medium (Dependent on DOM) |
| **Best For...** | Unknown layouts, Mapping, Complex Forms | Interactive elements (Accordions), Repetitive logic | Static content, High volume, Text extraction |
| **Irish Use Case** | *examinations.ie* dropdowns | *curriculumonline.ie* accordions | *ncca.ie* document lists |

### **8.2 Self-Healing and Resilience**

The proposed architecture is inherently self-healing. If Crawl4AI fails to extract data (returns empty Markdown), the Controller can promote the task to Stagehand. If Stagehand fails to find the selector, it promotes the task to Skyvern. This "Escalation of Intelligence" ensures that the most expensive resources are used only when necessary, while maintaining a near-100% success rate.  
Furthermore, Skyvern's resilience to layout changes means that if *examinations.ie* redesigns their site, the "Hunter" agent will likely adapt without code changes, simply by "looking" for the "Year" dropdown, whereas a CSS-selector based script would immediately break.

### **8.3 Conclusion**

The mapping and extraction of Irish educational data requires a sophisticated approach due to the heterogeneity of the source systems. By orchestrating Skyvern for visual navigation, Stagehand for precise interaction, Crawl4AI for bulk extraction, and BAML for rigorous schema enforcement, a Dockerized "Swarm" architecture provides a robust solution. This system not only automates the current retrieval requirements but establishes a resilient infrastructure capable of adapting to the inevitable evolution of the target web estates. The recommended implementation utilizes a centralized Controller container to dispatch tasks to these specialized agents, ensuring a clean separation of concerns and a scalable, maintainable data pipeline.

#### **Works cited**

1. Unlocking Browser Automation: A Deep Dive into the Official Skyvern MCP Server, accessed December 6, 2025, [https://skywork.ai/skypage/en/browser-automation-skyvern-mcp/1977611439104790528](https://skywork.ai/skypage/en/browser-automation-skyvern-mcp/1977611439104790528)  
2. Skyvern vs Scripts: AI Browser Automation Comparison, accessed December 6, 2025, [https://www.skyvern.com/blog/skyvern-vs-scripts-ai-automation-comparison/](https://www.skyvern.com/blog/skyvern-vs-scripts-ai-automation-comparison/)  
3. State Examination Commission \- Exam Material Archive | PDF | Vocational Education | Qualifications \- Scribd, accessed December 6, 2025, [https://www.scribd.com/document/923264836/State-Examination-Commission-Exam-Material-Archive](https://www.scribd.com/document/923264836/State-Examination-Commission-Exam-Material-Archive)  
4. How Skyvern Reads and Understands the Web, accessed December 6, 2025, [https://www.skyvern.com/blog/how-skyvern-reads-and-understands-the-web/](https://www.skyvern.com/blog/how-skyvern-reads-and-understands-the-web/)  
5. Stagehand Tool \- CrewAI Documentation, accessed December 6, 2025, [https://docs.crewai.com/en/tools/web-scraping/stagehandtool](https://docs.crewai.com/en/tools/web-scraping/stagehandtool)  
6. stagehand-py \- PyPI, accessed December 6, 2025, [https://pypi.org/project/stagehand-py/](https://pypi.org/project/stagehand-py/)  
7. Document crawl4ai.com | DocIngest, accessed December 6, 2025, [https://docingest.com/docs/crawl4ai.com](https://docingest.com/docs/crawl4ai.com)  
8. Quick Start \- Crawl4AI Documentation (v0.7.x), accessed December 6, 2025, [https://docs.crawl4ai.com/core/quickstart/](https://docs.crawl4ai.com/core/quickstart/)  
9. BAML documentation, accessed December 6, 2025, [https://docs.boundaryml.com/home](https://docs.boundaryml.com/home)  
10. Python | Boundary Documentation, accessed December 6, 2025, [https://docs.boundaryml.com/guide/installation-language/python](https://docs.boundaryml.com/guide/installation-language/python)  
11. skyvern/docker-compose.yml at main \- GitHub, accessed December 6, 2025, [https://github.com/Skyvern-AI/skyvern/blob/main/docker-compose.yml](https://github.com/Skyvern-AI/skyvern/blob/main/docker-compose.yml)  
12. Visualizing Results \- Skyvern, accessed December 6, 2025, [https://www.skyvern.com/docs/running-tasks/visualizing-results](https://www.skyvern.com/docs/running-tasks/visualizing-results)  
13. Installation \- Crawl4AI Documentation (v0.7.x), accessed December 6, 2025, [https://docs.crawl4ai.com/core/installation/](https://docs.crawl4ai.com/core/installation/)  
14. unclecode/crawl4ai: Crawl4AI: Open-source LLM Friendly Web Crawler & Scraper. Don't be shy, join here: https://discord.gg/jP8KfhDhyN \- GitHub, accessed December 6, 2025, [https://github.com/unclecode/crawl4ai](https://github.com/unclecode/crawl4ai)  
15. Networks | Docker Docs, accessed December 6, 2025, [https://docs.docker.com/reference/compose-file/networks/](https://docs.docker.com/reference/compose-file/networks/)  
16. Networking in Compose \- Docker Docs, accessed December 6, 2025, [https://docs.docker.com/compose/how-tos/networking/](https://docs.docker.com/compose/how-tos/networking/)  
17. Docker Deployment \- Crawl4AI Documentation (v0.7.x), accessed December 6, 2025, [https://docs.crawl4ai.com/core/docker-deployment/](https://docs.crawl4ai.com/core/docker-deployment/)  
18. Workflow Blocks \- Skyvern, accessed December 6, 2025, [https://www.skyvern.com/docs/workflows/workflow-blocks-details](https://www.skyvern.com/docs/workflows/workflow-blocks-details)  
19. Prompting and Troubleshooting Guide \- Skyvern, accessed December 6, 2025, [https://skyvern.com/docs/getting-started/prompting-guide](https://skyvern.com/docs/getting-started/prompting-guide)  
20. Run a task \- Skyvern, accessed December 6, 2025, [https://www.skyvern.com/docs/api-reference/api-reference/agent/run-task](https://www.skyvern.com/docs/api-reference/api-reference/agent/run-task)  
21. browserbase/stagehand: The AI Browser Automation Framework \- GitHub, accessed December 6, 2025, [https://github.com/browserbase/stagehand](https://github.com/browserbase/stagehand)  
22. Introducing Stagehand \- Stagehand, accessed December 6, 2025, [https://docs.stagehand.dev/](https://docs.stagehand.dev/)  
23. browserbase/stagehand-python: The AI Browser Automation Framework \- GitHub, accessed December 6, 2025, [https://github.com/browserbase/stagehand-python](https://github.com/browserbase/stagehand-python)  
24. Crawling with Crawl4AI. Web scraping in Python has… | by Harisudhan.S | Medium, accessed December 6, 2025, [https://medium.com/@speaktoharisudhan/crawling-with-crawl4ai-the-open-source-scraping-beast-9d32e6946ad4](https://medium.com/@speaktoharisudhan/crawling-with-crawl4ai-the-open-source-scraping-beast-9d32e6946ad4)  
25. Markdown Generation \- Crawl4AI Documentation (v0.7.x), accessed December 6, 2025, [https://docs.crawl4ai.com/core/markdown-generation/](https://docs.crawl4ai.com/core/markdown-generation/)  
26. Extraction & Chunking Strategies API \- Crawl4AI, accessed December 6, 2025, [https://docs.crawl4ai.com/api/strategies/](https://docs.crawl4ai.com/api/strategies/)  
27. Extraction \- LLM-Free Strategies \- 《Crawl4AI v0.4 Documentation》 \- 书栈网 · BookStack, accessed December 6, 2025, [https://www.bookstack.cn/read/crawl4ai-0.4-en/1fd95dd12887623e.md](https://www.bookstack.cn/read/crawl4ai-0.4-en/1fd95dd12887623e.md)  
28. LLM-Free Strategies \- Crawl4AI Documentation (v0.7.x), accessed December 6, 2025, [https://docs.crawl4ai.com/extraction/no-llm-strategies/](https://docs.crawl4ai.com/extraction/no-llm-strategies/)  
29. LLM Strategies \- Crawl4AI Documentation (v0.7.x), accessed December 6, 2025, [https://docs.crawl4ai.com/extraction/llm-strategies/](https://docs.crawl4ai.com/extraction/llm-strategies/)  
30. BoundaryML/baml: The AI framework that adds the engineering to prompt engineering (Python/TS/Ruby/Java/C\#/Rust/Go compatible) \- GitHub, accessed December 6, 2025, [https://github.com/BoundaryML/baml](https://github.com/BoundaryML/baml)  
31. Every Way To Get Structured Output From LLMs | BAML Blog, accessed December 6, 2025, [https://boundaryml.com/blog/structured-output-from-llms](https://boundaryml.com/blog/structured-output-from-llms)  
32. tomsquest/llm\_extract\_books: Source for article "Get structured output from a Language Model using BAML" \- GitHub, accessed December 6, 2025, [https://github.com/tomsquest/llm\_extract\_books](https://github.com/tomsquest/llm_extract_books)