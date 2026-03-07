# **Strategic Architecture for Autonomous Educational Data Acquisition: Integrating Skyvern, Crawl4AI, and Stagehand in the 2025 Open-Source Ecosystem**

## **1\. The 2025 Paradigm Shift in Automated Web Intelligence**

The trajectory of web automation has undergone a seismic shift by the fiscal year 2025\. The industry has moved decisively away from the fragile, deterministic scripting that characterized the early 2020s—typified by rigid XPath selectors and brittle DOM interactions—toward probabilistic, agentic workflows powered by Large Language Models (LLMs) and Vision Transformers (ViTs). This transition is not merely a technological upgrade but a fundamental reimagining of how machine intelligence interacts with the unstructured web. For data engineering teams tasked with aggregating institutional knowledge from disparate sources such as the National Council for Curriculum and Assessment (**ncca.ie**), **curriculumonline.ie**, and the State Examinations Commission (**examinations.ie**), this shift necessitates a re-evaluation of the tooling stack.  
The core challenge in 2025 is no longer access; it is intelligent discrimination. The "brittle selector" problem, where a minor frontend framework update breaks an entire scraping pipeline, has been largely solved by visual-reasoning agents like **Skyvern**.1 However, this solution introduces new constraints: high computational latency, significant token costs associated with vision inference, and heavy infrastructure requirements. Consequently, a monolithic approach relying solely on a visual agent is often economically and operationally inefficient for high-volume data discovery.  
This report provides an exhaustive architectural analysis of **Skyvern** alongside its primary open-source alternatives, **Crawl4AI** and **Stagehand**. It specifically addresses the user's requirement to "smartly gather relevant links" from the Irish educational digital estate. We posit that the optimal architecture for 2025 is not a binary choice but a heterogeneous pipeline: leveraging the **Adaptive Crawling** capabilities of Crawl4AI for broad, hierarchical topology mapping (as required for NCCA and Curriculum Online), while deploying the deterministic agentic capabilities of **Stagehand** to navigate the legacy form-based interfaces of the State Examinations Commission. This analysis rigorously adheres to open-source constraints, evaluating licensing implications (AGPL vs. Apache/MIT), self-hosting feasibility, and the emerging influence of the **Model Context Protocol (MCP)** on tool interoperability.3

### **1.1 The Bifurcation of Automated Web Interaction**

To understand the landscape, one must recognize the bifurcation of tools into two distinct phylogenies: **Visual-Reasoning Agents** and **Adaptive Semantic Crawlers**.  
The **Visual-Reasoning Agent**, exemplified by Skyvern, treats the web browser as a human user does. It renders the page, captures a screenshot, and utilizes multimodal LLMs (such as GPT-4o or Gemini 2.5 Pro) to interpret the visual layout.1 It reasons that a cluster of pixels labeled "Download Specification" is an actionable element, regardless of whether the underlying HTML tag is a \<button\>, \<div\>, or \<a\>. This makes the agent "anti-fragile" to code changes but introduces a "Vision Tax"—a latency of seconds per action and a high cost per step.  
Conversely, the **Adaptive Semantic Crawler**, typified by Crawl4AI, represents the evolution of the traditional spider. It does not "look" at the page in the visual sense; rather, it "reads" the semantic density of the content.5 Utilizing **Information Foraging Theory**, it embeds the text of hyperlinks and the user's query into a vector space, calculating cosine similarity to determine the "scent" of information.6 It follows paths that are semantically relevant to the target topic (e.g., "Leaving Certificate Biology") and prunes branches that lead to low-value areas (e.g., "Privacy Policy" or "Board Minutes").  
The selection of the "best open-source alternative" to Skyvern depends entirely on whether the target domain requires the visual intuition of an agent or the high-velocity traversal of a crawler. For the Irish educational sites in question, which comprise both deep informational hierarchies and interactive search forms, a nuanced integration of both paradigms is required.

## ---

**2\. Skyvern: The Visual Autonomous Platform**

Skyvern has established itself as the reference standard for "best overall" browser automation in 2025, particularly for complex, transactional workflows involving authentication, CAPTCHAs, and dynamic frontend frameworks.1 To identify its best alternative, we must first deeply analyze its operational mechanics, strengths, and significant overheads.

### **2.1 Architectural Mechanics: The Visual Perception Layer**

Skyvern’s primary innovation is its rejection of the DOM as the primary source of truth for navigation. While it interacts with the browser via protocols like Playwright, its decision-making engine is visual. When Skyvern navigates to a URL, it performs a sequence of high-latency operations:

1. **Viewport Capture:** It captures a screenshot of the current viewport and extracts the accessibility tree to map interactive elements to bounding boxes.2  
2. **Visual Inference:** It sends this visual data to a Vision LLM (VLM) alongside the user's natural language prompt (e.g., "Find the latest Chemistry syllabus").  
3. **Action Planning:** The VLM returns a coordinate-based plan (e.g., "Click at \[x,y\]").  
4. **Execution & Verification:** Skyvern executes the action and visually verifies the result.8

This architecture provides unparalleled resilience. If ncca.ie were to undergo a redesign that obfuscated all HTML class names (a common side effect of modern React/Angular builds), Skyvern would continue to function without modification, provided the visual label "Curriculum" remained visible.9 This capability is critical for "write" operations—filling forms, handling complex 2FA logins, or navigating checkout flows where the cost of failure is high.8

### **2.2 The "Vision Tax" and Infrastructure Overhead**

However, this resilience comes at a steep operational price, which acts as a deterrent for the specific use case of "gathering relevant links" across thousands of pages.

* **Token Economics:** Every single step in a Skyvern workflow incurs a cost. Processing a screenshot through a model like GPT-4o or Claude 3.5 Sonnet costs significantly more than processing text. For a scraping task that involves traversing a sitemap of 5,000 pages on curriculumonline.ie, the token costs would be exorbitant compared to a text-based crawler.11  
* **Latency:** Visual inference takes time—often 2 to 5 seconds per step depending on the model and network. A crawler like Crawl4AI can process dozens of pages in the time Skyvern processes one interaction.12  
* **Infrastructure Complexity:** Skyvern is not merely a library; it is a platform. Self-hosting Skyvern (to adhere to the "strictly open-source" requirement) involves orchestrating a Dockerized stack containing a PostgreSQL database for state management, a Redis queue for task orchestration, and the Skyvern service itself.8 This introduces a significant maintenance burden compared to lightweight Python or Node.js libraries.

### **2.3 Licensing Considerations: The AGPL-3.0 Constraint**

A critical factor for enterprise or institutional adoption is Skyvern’s use of the **GNU Affero General Public License v3.0 (AGPL-3.0)**.14 This is a "strong copyleft" license. It mandates that if you modify the software and interact with it over a network (i.e., offer it as a service), you must make your modified source code available to users. For organizations that require strict proprietary control over their internal tooling or wish to embed the scraper into a commercial product, AGPL-3.0 can be a disqualifying factor. This contrasts sharply with the permissive Apache 2.0 and MIT licenses used by Crawl4AI and Stagehand, respectively.12

## ---

**3\. Crawl4AI: The Adaptive Semantic Crawler**

If Skyvern represents the heavy artillery of automation, **Crawl4AI** is the precision guided munition for information retrieval. Identified in 2025 research as the "Best Open Source" alternative specifically for data extraction, it offers a fundamentally different approach to the web: **Adaptive Crawling**.12

### **3.1 Adaptive Crawling Theory: The Knowledge Capacitor**

The user's query specifically requests a comparison to Crawl4AI's "adaptive crawling feature." This feature is based on the concept of the "Knowledge Capacitor"—the idea that a crawler should stop accumulating data once the "charge" (information gain) saturates.5  
Traditional crawlers utilize Breadth-First Search (BFS) or Depth-First Search (DFS), blindly following every link in a queue. This is inefficient for broad domains like ncca.ie, where a significant portion of the link graph points to irrelevant administrative pages. Crawl4AI replaces this blind traversal with Semantic Vector Traversal:

1. **Embedding Generation:** When the crawler encounters a set of links, it generates vector embeddings for the link text and the surrounding context using a lightweight local model (e.g., all-MiniLM-L6-v2) or an API.6  
2. **Cosine Similarity Filtering:** It compares these vectors against the vector of the user's query (e.g., "Primary Curriculum Mathematics Specifications").  
3. **Path Prioritization:** Links with high cosine similarity scores are prioritized in the crawl queue. The crawler literally "follows the scent" of the curriculum, ignoring links to "Tenders" or "Contact Us" that have low semantic relevance.6  
4. **Saturation Pruning:** The **AdaptiveCrawler** class monitors the "freshness" of content found in a cluster. If consecutive pages yield no new semantic information regarding the query topic, the crawler identifies the cluster as "saturated" and terminates that branch, saving compute resources and time.5

### **3.2 Technical Architecture for RAG Pipelines**

Crawl4AI is engineered explicitly for the age of Generative AI. Its output is not raw HTML, which is noisy and token-heavy, but **LLM-Ready Markdown**.

* **Fit Markdown:** The library includes algorithms like BM25 pruning to strip navigation bars, footers, and advertisements, leaving only the semantic core of the document.17  
* **Asynchronous Speed:** Built on top of **Playwright**, it operates asynchronously. While it can render JavaScript (essential for modern sites), it does not require the heavy visual processing of Skyvern. It extracts the DOM, sanitizes it, and moves to the next link in milliseconds.16  
* **Zero-Config Deployment:** Unlike Skyvern's Docker stack, Crawl4AI is a simple Python library (pip install crawl4ai). It runs efficiently in standard CI/CD environments or lightweight containers, making it highly scalable for scraping thousands of educational documents.12

### **3.3 The "Smart Gathering" Advantage**

For the specific task of "gathering relevant links" from ncca.ie and curriculumonline.ie, Crawl4AI is architecturally superior to Skyvern. These sites function as hierarchical repositories. The challenge is filtering out the noise to find the specific PDF documents and specification pages. Crawl4AI's **BestFirstCrawlingStrategy** 18 allows for a defined topic ("Irish Curriculum"), ensuring that the crawler maps the relevant topology of the site without wasting cycles on visual reasoning for every navigation click.

## ---

**4\. Stagehand: The Deterministic Agentic Bridge**

While Crawl4AI excels at discovery, it faces limitations with complex, stateful interactions—specifically the legacy search forms found on **examinations.ie**. This brings us to **Stagehand**, the "Act-Extract-Observe" framework that serves as the ideal middle ground between the crawler and the autonomous agent.20

### **4.1 The "Act-Extract-Observe" Primitive**

Stagehand, developed by Browserbase and released as open source (MIT), abstracts browser automation into three atomic AI-driven primitives:

1. **Observe:** The agent analyzes the DOM and accessibility tree to understand the current state and available actions (e.g., "I see a dropdown for 'Year' and a submit button").21  
2. **Act:** The developer provides a natural language instruction (e.g., page.act("Select 'Leaving Certificate' from the exam type dropdown")). The AI translates this into a Playwright action.  
3. **Extract:** The agent pulls structured data based on a schema (e.g., page.extract("All PDF links in the results table")).22

### **4.2 Caching and Self-Healing: The Economic Differentiator**

The critical innovation in Stagehand, particularly relevant to 2025, is its **Caching and Self-Healing** mechanism.

* **The Problem with Agents:** Using a pure agent (like Skyvern) to loop through 15 years of exam papers involves re-reasoning about the "Year" dropdown 15 times. This is redundant and costly.  
* **The Stagehand Solution:** When Stagehand executes page.act("Click Search") successfully for the first time, it *caches* the specific selector that worked (e.g., \#btn-search-2025). For the next iteration, it tries the cached selector first. This bypasses the LLM entirely, executing at the speed of raw code (milliseconds).  
* **Self-Healing:** If the website changes and the cached selector fails, Stagehand automatically "heals" itself by re-invoking the AI to find the new selector, updating the cache, and continuing. This provides the resilience of Skyvern with the speed and cost-efficiency of a script.21

### **4.3 Chrome DevTools Protocol (CDP) Integration**

In its 2025 iteration (v3), Stagehand moved closer to the metal by integrating directly with the **Chrome DevTools Protocol (CDP)**, bypassing some of the abstraction layers of Playwright.22 This allows for lower-latency control and better handling of anti-bot measures, which is crucial when scraping government archives that might have legacy session management quirks or basic rate limiting.

### **4.4 Browser Use: The Pure Agent Alternative**

A discussion of open-source alternatives must also mention **Browser Use**.24 Like Stagehand, it is a library (Python-based) rather than a platform. It chains LangChain with Playwright to create autonomous agents.

* **Comparison:** Browser Use is more "autonomous" than Stagehand, designed to take a high-level goal ("Find me socks") and figure out the steps. Stagehand is more "deterministic," designed for developers to define the steps (Act, Act, Extract) while letting AI handle the *how*.  
* **Verdict:** For the structured task of iterating through exam years, Stagehand's deterministic control and caching make it superior to Browser Use, which might "hallucinate" or deviate from the strict iteration required for a complete archive download.25

## ---

**5\. Domain Topology Analysis: The Three Target Sites**

To design the optimal scraping architecture, we must map the tool capabilities to the specific topologies of the target websites. The "one size fits all" approach is the primary cause of failure in large-scale data acquisition.

### **5.1 ncca.ie: The Hierarchical Informational Graph**

* **Topology:** The National Council for Curriculum and Assessment website is a classic **Multi-Page Application (MPA)**. It features a deep hierarchy: Home \-\> Education Level (e.g., Primary) \-\> Subject Area \-\> Specification Document.26  
* **Content Characteristics:** The site is document-heavy. The "signal" (curriculum specs) is buried amidst "noise" (corporate governance, news, consultations).  
* **Optimal Tool: Crawl4AI.**  
  * **Reasoning:** The primary task is *traversal* and *filtering*. Visual reasoning is unnecessary; the navigation structure is explicit in the HTML (\<a\> tags).  
  * **Strategy:** Utilize Crawl4AI’s BestFirstCrawlingStrategy. Configure the crawler with a KeywordRelevanceScorer weighted towards terms like "Curriculum", "Specification", "Framework", and "PDF". This ensures the crawler efficiently spider-webs through the education levels, ignoring the "Corporate" and "News" branches that do not match the semantic profile of the query.18

### **5.2 curriculumonline.ie: The Cross-Referenced Database**

* **Topology:** Similar to NCCA but with a more modern frontend. Snippets indicate the presence of "My Account" and "Search" features, suggesting potential dynamic content loading or user-session gating, though the core curriculum is likely public.27  
* **Content Characteristics:** Highly structured. Subjects link to "Toolkits," "Examples of Student Work," and "Assessment Guidelines."  
* **Optimal Tool: Crawl4AI.**  
  * **Reasoning:** Like NCCA, this is a discovery problem. The site's "Search" feature 27 is a distraction; the most reliable way to get *all* data is to traverse the subject hierarchy links directly.  
  * **Configuration:** Enable js\_code execution in Crawl4AI to handle any dynamic hydration of the subject lists. Use the fit\_markdown feature to parse the structured content of the specification pages into clean text, which simplifies the identification of the actual download links for the PDFs.17

### **5.3 examinations.ie: The Legacy Deep Web**

* **Topology:** The State Examinations Commission website functions differently. The critical resource, the **Examination Material Archive**, is a "Deep Web" interface. It is not a hierarchy of links; it is a search form.28  
* **Interaction Model:** Users must select a Year (e.g., 2023), Examination (e.g., Leaving Certificate), and Subject from dropdown menus, then submit a POST request to generate a list of downloadable papers. The URLs for the papers are often dynamically generated or session-dependent.  
* **The Failure of Crawlers:** A standard crawler (even an adaptive one) will hit the search page and stop. It cannot "guess" the matrix of dropdown combinations (Years x Subjects) required to expose the documents.  
* **Optimal Tool: Stagehand.**  
  * **Reasoning:** This is a *transactional* task requiring state management. You need an agent to perform a specific sequence of actions repeatedly.  
  * **Strategy:** Write a Stagehand script that iterates through a defined list of years (e.g., 2010-2025). Inside the loop, use page.act() to select the year and subject, then page.extract() to grab the result links. Stagehand’s caching means that after the first successful interaction with the dropdowns, the subsequent thousands of iterations will be near-instantaneous and free of LLM costs.21

## ---

**6\. Comparative Architecture Analysis**

The following analysis synthesizes the capabilities of the discussed tools specifically against the requirements of the Irish educational dataset.

### **6.1 Feature Matrix: Skyvern vs. Alternatives**

| Feature / Requirement | Skyvern | Crawl4AI | Stagehand | Browser Use |
| :---- | :---- | :---- | :---- | :---- |
| **Core Philosophy** | Visual Autonomous Platform | Adaptive Semantic Crawler | AI-Coding Bridge (Act/Extract) | Autonomous Agent Library |
| **Open Source License** | **AGPL-3.0** (Restrictive) | **Apache 2.0** (Permissive) | **MIT** (Permissive) | **MIT** (Permissive) |
| **Primary Mechanism** | Vision LLM \+ Accessibility Tree | Information Foraging (Embeddings) | Atomic AI Primitives \+ Caching | LangChain \+ Playwright |
| **Best Use Case** | Unseen, complex UIs; 2FA 1 | High-speed discovery; RAG prep 6 | Repetitive forms; Data mining 20 | General prototyping 24 |
| **Link Gathering Efficiency** | **Low** (High latency/cost) | **High** (Async/Semantic filtering) | **Medium** (Browser overhead) | **Low** (Token heavy) |
| **Form Interaction** | Excellent (Visual reasoning) | Weak (Scripting required) | **Excellent** (Cached Actions) | Good (Planner dependent) |
| **Infrastructure** | Heavy (Docker/Postgres/Redis) | Light (Python Library) | Light (Node/Python Library) | Light (Python Library) |

### **6.2 The Cost/Performance Trade-off**

The economic model of 2025 scraping is defined by **Token Efficiency**.

* **Skyvern:** High OpEx. Processing curriculumonline.ie (est. 5,000 pages) visually would require 5,000+ multimodal API calls. At conservative 2025 pricing (e.g., $0.01 per step for complex vision tasks), this is a $50+ run, with slow execution.  
* **Crawl4AI:** Low OpEx. It uses local embeddings (free) or cheap text-only APIs to score links. The cost is negligible (cents). Speed is limited only by the target server's response time and polite rate limiting.11  
* **Stagehand:** Optimized OpEx. For the examinations.ie form loop, it incurs LLM costs *only* when the layout changes or the selector cache is cold. The steady-state operation is free of token costs, offering the reliability of Skyvern at the cost profile of a script.21

## ---

**7\. Implementation Blueprint: The Hybrid Pipeline**

To satisfy the user's requirement to "smartly gather relevant links" across all three domains, we propose a hybrid architecture that integrates these open-source solutions into a cohesive pipeline.

### **7.1 Phase 1: The Semantic Spider (NCCA & Curriculum Online)**

Tool: Crawl4AI (Python)  
Objective: Map the hierarchical content and extract PDF links.

Python

\# Conceptual Implementation for Crawl4AI  
from crawl4ai import AsyncWebCrawler, AdaptiveConfig, CrawlerRunConfig  
from crawl4ai.deep\_crawling import BestFirstCrawlingStrategy

async def harvest\_curriculum():  
    \# Configure Adaptive Strategy  
    \# We use BestFirst to prioritize links that look like curriculum specs  
    strategy \= BestFirstCrawlingStrategy(  
        max\_depth=5,  \# Deep crawl to find nested PDFs  
        max\_pages=5000,  
        \# Score links based on relevance to these terms  
        scorer\_config={  
            "keywords": \["curriculum", "specification", "syllabus", "guidelines", "pdf"\],  
            "weight": 0.85  
        }  
    )

    config \= CrawlerRunConfig(  
        deep\_crawl\_strategy=strategy,  
        \# Strip navigation/footer noise to focus on content  
        fit\_markdown=True,   
        \# Identify saturation to stop crawling irrelevant sections  
        adaptive\_config=AdaptiveConfig(  
            confidence\_threshold=0.8,  
            min\_gain\_threshold=0.05  
        )  
    )

    async with AsyncWebCrawler() as crawler:  
        \# Seed with both hierarchical sites  
        results\_ncca \= await crawler.arun("https://ncca.ie/en/", config=config)  
        results\_curr \= await crawler.arun("https://www.curriculumonline.ie/", config=config)  
          
        \# Process results to extract PDF URLs  
        \#...

### **7.2 Phase 2: The Archive Agent (State Examinations)**

Tool: Stagehand (Node.js/TypeScript)  
Objective: Navigate the search form to expose hidden PDF links.

TypeScript

// Conceptual Implementation for Stagehand  
import { Stagehand } from "@browserbasehq/stagehand";  
import { z } from "zod";

async function harvest\_exams() {  
    const stagehand \= new Stagehand({  
        // Use standard LLM (e.g., GPT-4o) for the 'Act' reasoning  
        llmClient: myLLMClient   
    });  
      
    await stagehand.init();  
    const page \= stagehand.page;  
      
    await page.goto("https://www.examinations.ie/exammaterialarchive/");  
      
    // Define the iteration space  
    const years \= \["2024", "2023", "2022", "2021"\];  
    const examTypes \= \["Leaving Certificate", "Junior Cycle"\];  
      
    for (const type of examTypes) {  
        // Stagehand caches this selector after the first successful run  
        await page.act(\`Select '${type}' from the Examination dropdown\`);  
          
        for (const year of years) {  
            await page.act(\`Select '${year}' from the Year dropdown\`);  
            // We might need to iterate subjects, or select "All" if available  
            await page.act("Click the Search button");  
              
            // Wait for hydration/navigation  
            await page.waitForLoadState("networkidle");  
              
            // Extract the data using a schema  
            const data \= await page.extract({  
                instruction: "Extract all exam paper download links and their titles",  
                schema: z.object({  
                    papers: z.array(z.object({  
                        subject: z.string(),  
                        level: z.string(),  
                        downloadUrl: z.string()  
                    }))  
                })  
            });  
              
            // Save data...  
              
            // Reset for next loop if necessary (e.g. click 'Back' or reload)  
            await page.act("Click the 'New Search' button or reload page");  
        }  
    }  
}

### **7.3 Integration via Model Context Protocol (MCP)**

A forward-looking 2025 architecture should utilize the **Model Context Protocol (MCP)** to unify these tools. Both Crawl4AI and Stagehand (via Browserbase) are moving towards MCP compliance.4

* **The Unifying Layer:** By wrapping the Crawl4AI script and the Stagehand script as MCP Servers, a central AI agent (e.g., in Claude Desktop or a custom orchestrator) can query them naturally.  
* **Workflow:** The orchestrator agent receives the prompt "Get me the 2024 Biology Papers." It knows via MCP that Stagehand\_Exams\_Tool is the correct instrument for this request, while Crawl4AI\_Curriculum\_Tool is for general syllabus queries. This abstraction layer future-proofs the system, allowing individual tools to be swapped without breaking the high-level logic.

## ---

**8\. Operational & Economic Analysis**

### **8.1 The "Open Source" Reality Check**

The user explicitly requested a focus on "open-source solutions." It is vital to distinguish between "Free to Use" and "Open Source."

* **Skyvern:** While the code is available, the *operational reality* often pushes users toward their managed cloud service due to the complexity of the self-hosted stack (Vision models, browser grids).8 The AGPL license is also a barrier for commercial embedded use.  
* **Crawl4AI:** Represents "True" open source. It runs on local compute with local embeddings. It is the most cost-effective solution, with zero marginal cost per page beyond electricity and bandwidth.12  
* **Stagehand:** While the SDK is open source (MIT), it is optimized for Browserbase's cloud. However, it *can* run on local Playwright. Running it locally retains the open-source spirit but requires the user to manage the browser instances (which Playwright handles well for moderate volumes).23

### **8.2 Maintenance and Longevity**

The primary advantage of the proposed Hybrid Architecture over a pure script (e.g., Selenium) is **maintenance reduction**.

* **Crawl4AI:** If ncca.ie changes its menu structure, the AdaptiveCrawler likely adapts automatically because it follows semantic relevance, not specific div paths.5  
* **Stagehand:** If examinations.ie renames the id of the search button, Stagehand's self-healing mechanism triggers: the cached selector fails, the AI re-analyzes the DOM, finds the new button, updates the cache, and the pipeline continues without engineering intervention.21

## ---

**9\. Conclusion**

In the 2025 landscape of automated web intelligence, **Skyvern** remains a powerful tool, but for the specific objective of gathering links from the Irish educational estate, it is an architectural mismatch. Its visual-agent paradigm incurs unnecessary cost and latency for broad information retrieval tasks.  
**Crawl4AI** is the definitive "Best Open Source Alternative" for the discovery phase of this project. Its **Adaptive Crawling** features allow for the intelligent, high-velocity mapping of ncca.ie and curriculumonline.ie, filtering noise through semantic embeddings to identify relevant curriculum links with minimal overhead.  
However, for the **State Examinations Commission** website, which functions as a deep-web database rather than a hyperlinked document graph, **Stagehand** is the required complementary tool. Its ability to bridge the gap between AI reasoning and deterministic script execution—specifically through its caching and self-healing Act primitive—makes it the optimal solution for navigating legacy search forms efficiently.  
**Final Recommendation:** Adopt a **Polyglot Pipeline**. Use **Crawl4AI** with BestFirstCrawlingStrategy for hierarchical site mapping, and deploy **Stagehand** scripts for form-based archive retrieval. This approach maximizes resilience and "smart" discovery while minimizing the operational expenditure and technical debt associated with maintaining purely visual or purely scripted automations.

| Target Domain | Topology | Recommended Tool | Strategic Rationale |
| :---- | :---- | :---- | :---- |
| **ncca.ie** | Hierarchical MPA | **Crawl4AI** | Adaptive crawling effectively filters "Corporate" noise to find "Curriculum" signal using semantic embeddings. |
| **curriculumonline.ie** | Structured MPA | **Crawl4AI** | High-velocity traversal of subject hierarchies; fit\_markdown ensures clean text extraction for downstream processing. |
| **examinations.ie** | Legacy Search Form | **Stagehand** | Act primitive handles dropdown interactions (Year/Subject) with caching for speed; Extract pulls dynamic result links. |

#### **Works cited**

1. 5 Best AI Browser Automation Tools for E-commerce 2025 \- Skyvern, accessed December 7, 2025, [https://www.skyvern.com/blog/best-ai-browser-automation-tools-for-e-commerce-in-2025/](https://www.skyvern.com/blog/best-ai-browser-automation-tools-for-e-commerce-in-2025/)  
2. Skyvern Browser Automation: My Deep Dive into the AI Agent Reshaping Web Workflows, accessed December 7, 2025, [https://skywork.ai/skypage/en/Skyvern-Browser-Automation-My-Deep-Dive-into-the-AI-Agent-Reshaping-Web-Workflows/1975062737322045440](https://skywork.ai/skypage/en/Skyvern-Browser-Automation-My-Deep-Dive-into-the-AI-Agent-Reshaping-Web-Workflows/1975062737322045440)  
3. Crawl4AI-MCP Server: A Comprehensive Guide for AI Engineers, accessed December 7, 2025, [https://skywork.ai/skypage/en/Crawl4AI-MCP-Server-A-Comprehensive-Guide-for-AI-Engineers/1972498543280062464](https://skywork.ai/skypage/en/Crawl4AI-MCP-Server-A-Comprehensive-Guide-for-AI-Engineers/1972498543280062464)  
4. Cole Medin's Crawl4AI MCP Server: The Ultimate Knowledge Engine for Your AI Agent, accessed December 7, 2025, [https://skywork.ai/skypage/en/crawl4ai-mcp-server-knowledge-engine/1977914256336539648](https://skywork.ai/skypage/en/crawl4ai-mcp-server-knowledge-engine/1977914256336539648)  
5. Adaptive Crawling: Building Dynamic Knowledge That Grows on Demand \- Crawl4AI, accessed December 7, 2025, [https://docs.crawl4ai.com/blog/articles/adaptive-crawling-revolution/](https://docs.crawl4ai.com/blog/articles/adaptive-crawling-revolution/)  
6. Adaptive Crawling \- Crawl4AI Documentation (v0.7.x), accessed December 7, 2025, [https://docs.crawl4ai.com/core/adaptive-crawling/](https://docs.crawl4ai.com/core/adaptive-crawling/)  
7. Best Open-source Web Scraping Libraries in 2025 \- Skyvern, accessed December 7, 2025, [https://www.skyvern.com/blog/best-open-source-web-scraping-libraries-in-2025/](https://www.skyvern.com/blog/best-open-source-web-scraping-libraries-in-2025/)  
8. Skyvern-AI/skyvern: Automate browser based workflows with AI \- GitHub, accessed December 7, 2025, [https://github.com/Skyvern-AI/skyvern](https://github.com/Skyvern-AI/skyvern)  
9. Best Free Open Source Browser Automation Tools in 2025 \- Skyvern, accessed December 7, 2025, [https://www.skyvern.com/blog/best-free-open-source-browser-automation-tools-in-2025/](https://www.skyvern.com/blog/best-free-open-source-browser-automation-tools-in-2025/)  
10. Skyvern vs Scripts: AI Browser Automation Comparison, accessed December 7, 2025, [https://www.skyvern.com/blog/skyvern-vs-scripts-ai-automation-comparison/](https://www.skyvern.com/blog/skyvern-vs-scripts-ai-automation-comparison/)  
11. Build Your AI Business on Skyvern, accessed December 7, 2025, [https://www.skyvern.com/blog/build-your-ai-business-on-skyvern/](https://www.skyvern.com/blog/build-your-ai-business-on-skyvern/)  
12. Top 7 AI Web Scraping Tools of 2025: Overhyped or Revolutionary? \- ScrapeOps, accessed December 7, 2025, [https://scrapeops.io/web-scraping-playbook/best-ai-web-scraping-tools/](https://scrapeops.io/web-scraping-playbook/best-ai-web-scraping-tools/)  
13. Unlocking Browser Automation: A Deep Dive into the Official Skyvern MCP Server, accessed December 7, 2025, [https://skywork.ai/skypage/en/browser-automation-skyvern-mcp/1977611439104790528](https://skywork.ai/skypage/en/browser-automation-skyvern-mcp/1977611439104790528)  
14. skyvern/LICENSE at main \- GitHub, accessed December 7, 2025, [https://github.com/Skyvern-AI/skyvern/blob/main/LICENSE](https://github.com/Skyvern-AI/skyvern/blob/main/LICENSE)  
15. Download @browserbasehq\_stagehand@3.0.2 source code.zip (Stagehand), accessed December 7, 2025, [https://sourceforge.net/projects/stagehand.mirror/files/@browserbasehq\_stagehand@3.0.2/@browserbasehq\_stagehand@3.0.2%20source%20code.zip/download](https://sourceforge.net/projects/stagehand.mirror/files/@browserbasehq_stagehand@3.0.2/@browserbasehq_stagehand@3.0.2%20source%20code.zip/download)  
16. Crawl4AI Explained: The AI-Friendly Web Crawling Framework \- Scrapfly, accessed December 7, 2025, [https://scrapfly.io/blog/posts/crawl4AI-explained](https://scrapfly.io/blog/posts/crawl4AI-explained)  
17. Crawling with Crawl4AI. Web scraping in Python has… | by Harisudhan.S | Medium, accessed December 7, 2025, [https://medium.com/@speaktoharisudhan/crawling-with-crawl4ai-the-open-source-scraping-beast-9d32e6946ad4](https://medium.com/@speaktoharisudhan/crawling-with-crawl4ai-the-open-source-scraping-beast-9d32e6946ad4)  
18. Deep Crawling \- Crawl4AI Documentation (v0.7.x), accessed December 7, 2025, [https://docs.crawl4ai.com/core/deep-crawling/](https://docs.crawl4ai.com/core/deep-crawling/)  
19. Home \- Crawl4AI Documentation (v0.7.x), accessed December 7, 2025, [https://docs.crawl4ai.com/](https://docs.crawl4ai.com/)  
20. Introducing Stagehand \- Stagehand, accessed December 7, 2025, [https://docs.stagehand.dev/](https://docs.stagehand.dev/)  
21. Stagehand Review: Best AI Browser Automation Framework? \- Apidog, accessed December 7, 2025, [https://apidog.com/blog/stagehand/](https://apidog.com/blog/stagehand/)  
22. Launching Stagehand v3, the best automation framework, accessed December 7, 2025, [https://www.browserbase.com/blog/stagehand-v3](https://www.browserbase.com/blog/stagehand-v3)  
23. browserbase/stagehand: The AI Browser Automation Framework \- GitHub, accessed December 7, 2025, [https://github.com/browserbase/stagehand](https://github.com/browserbase/stagehand)  
24. Browser-Use: Open-Source AI Agent For Web Automation \- Labellerr, accessed December 7, 2025, [https://www.labellerr.com/blog/browser-use-agent/](https://www.labellerr.com/blog/browser-use-agent/)  
25. Browser-use vs Crawl4ai : r/AI\_Agents \- Reddit, accessed December 7, 2025, [https://www.reddit.com/r/AI\_Agents/comments/1iyw8l6/browseruse\_vs\_crawl4ai/](https://www.reddit.com/r/AI_Agents/comments/1iyw8l6/browseruse_vs_crawl4ai/)  
26. Home \- National Council for Curriculum and Assessment, accessed December 7, 2025, [https://www.ncca.ie/en/](https://www.ncca.ie/en/)  
27. Curriculum Online: Home, accessed December 7, 2025, [https://www.curriculumonline.ie/](https://www.curriculumonline.ie/)  
28. accessed January 1, 1970, [https://www.examinations.ie/exammaterialarchive/](https://www.examinations.ie/exammaterialarchive/)  
29. Childcare Community Care Frequently Asked Questions \- PDST, accessed December 7, 2025, [https://pdst.ie/sites/default/files/Childcare%20Community%20Care%20Frequently%20Asked%20Questions.docx](https://pdst.ie/sites/default/files/Childcare%20Community%20Care%20Frequently%20Asked%20Questions.docx)  
30. Browserbase: An In-Depth Review of the AI-Powered Browser Infrastructure \- Skywork.ai, accessed December 7, 2025, [https://skywork.ai/skypage/en/Browserbase-An-In-Depth-Review-of-the-AI-Powered-Browser-Infrastructure/1972929060068716544](https://skywork.ai/skypage/en/Browserbase-An-In-Depth-Review-of-the-AI-Powered-Browser-Infrastructure/1972929060068716544)