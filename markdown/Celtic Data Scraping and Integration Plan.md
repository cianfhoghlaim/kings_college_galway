# **Computational Archiving of Celtic Digital Heritage: An Exhaustive Analysis of Skyvern Integration and Pan-Celtic Resource Extraction**

## **1\. Introduction: The Intersection of Agentic AI and Endangered Language Preservation**

The digital preservation of low-resource languages and their associated cultural heritages presents a complex challenge that transcends traditional archival methods. For the Celtic nations—Ireland, Scotland, Wales, and the Isle of Man—the repository of national identity is increasingly fragmented across diverse digital platforms, ranging from modern, dynamic government educational portals to legacy folklore databases. This fragmentation poses a significant barrier to the development of large-scale linguistic models (LLMs) and comprehensive digital humanities research. The solution lies in the application of "agentic" artificial intelligence—systems capable of reasoning, planning, and executing complex workflows within a web browser—to systematically harvest and structure this data.  
This report provides a rigorous technical and structural analysis of the Skyvern browser automation framework, specifically examining its utility for extracting educational, audio, and spatial data from key Irish websites. Furthermore, it establishes a "Gold Standard" ontology based on these Irish resources to identify and evaluate equivalent high-priority targets in Scotland, Wales, and the Isle of Man. The ultimate objective is to define a robust, automated pipeline capable of navigating the idiosyncratic interfaces of Celtic digital infrastructure to preserve the "Four Nations" heritage in a machine-readable format.  
The analysis is grounded in a deep architectural review of Skyvern’s codebase, particularly its Large Language Model (LLM) integration points, and a granular audit of six primary Irish sites: ncca.ie, examinations.ie, curriculumonline.ie, canuint.ie, duchas.ie, and hiddenheritages.ai. By treating these sites as archetypes, we extrapolate a scraping strategy applicable to the broader Celtic web, culminating in specific prompts and configuration files designed for immediate deployment.

## **2\. Skyvern Architecture: The Mechanics of Agentic Browser Automation**

To understand the feasibility of automating data extraction from complex government and heritage portals, one must first dissect the toolset. Skyvern distinguishes itself from traditional DOM-based scrapers (like Beautiful Soup or Selenium) through its reliance on computer vision and LLM-driven reasoning. This "Task-Driven autonomous agent" design allows it to navigate websites based on visual context rather than brittle code selectors, a critical advantage when dealing with legacy government sites that lack semantic HTML.1

### **2.1 The Agentic Workflow and Vision-Based Navigation**

Skyvern operates by instantiating a "swarm of agents" that function similarly to human users. They observe the browser viewport, interpret visual elements (buttons, forms, maps), and plan a sequence of interactions to achieve a high-level goal.1 This architecture is inspired by the autonomous agent designs of BabyAGI and AutoGPT but is specifically optimized for browser interaction via Playwright.2  
The workflow is defined by "blocks," which represent the atomic units of the agent's reasoning process. Understanding the distinction between these blocks is vital for configuring a successful crawl of complex sites like hwb.gov.wales or canuint.ie.

* **Action Block:** This is the most deterministic unit, representing a single, discrete interaction such as "Click the 'Download' button" or "Type 'Gaelic' into the search field".3 It is best suited for sites with predictable layouts, such as the examinations.ie form interface.  
* **Navigation Block:** This block manages a single navigational goal, allowing the LLM to infer the necessary intermediate steps. For example, a prompt to "Find the Primary Mathematics Curriculum" on curriculumonline.ie would use a Navigation block to traverse the menu hierarchy.3  
* **Navigation V2 Block:** This represents the state-of-the-art in agentic planning, capable of handling multi-goal workflows. It is the "most flexible" option, designed for scenarios requiring complex state management, such as logging into a portal, navigating to a sub-section, and then iterating through a paginated list of results.3

The reliance on visual parsing means Skyvern is resistant to the frequent layout changes that plague long-term archival projects. As noted in the documentation, "Skyvern is resistant to website layout changes, as there are no pre-determined XPaths or other selectors our system is looking for".2 This robustness is essential for maintaining a persistent archive of government curriculum sites, which often undergo cosmetic refreshes without altering the underlying information architecture.

### **2.2 LLM Integration and Configuration Registry**

A critical component of this research involves configuring Skyvern to utilize specific LLMs, particularly for tasks involving the Celtic languages where specialized or fine-tuned models might be superior to generic commercial models. The analysis of the Skyvern codebase reveals a sophisticated configuration registry designed to abstract the complexity of model providers.

#### **2.2.1 The Configuration Registry**

The core logic for LLM handling is located in skyvern/forge/sdk/api/llm/config\_registry.py.4 This module utilizes structlog for observability and defines strict exception handling classes such as DuplicateLLMConfigError, InvalidLLMConfigError, and MissingLLMProviderEnvVarsError.4 These error classes indicate a rigid validation process during system startup, ensuring that the scraping pipeline will fail fast if the LLM provider is misconfigured—a crucial feature for production-grade archiving where resource costs are a concern.  
The system uses LiteLLMParams and LLMConfig models to standardize the interface between Skyvern's reasoning engine and the backend model.4 This abstraction layer allows researchers to swap out the reasoning engine without rewriting the scraping logic, facilitating experiments with different models to optimize for cost or accuracy when processing Welsh or Irish text.

#### **2.2.2 Integration of Local and Custom Providers**

For projects involving sensitive cultural data or requiring operation in offline/air-gapped environments, the ability to integrate local LLMs is paramount. The Skyvern issue tracker 5 provides a blueprint for integrating OpenAI-compatible local servers, such as LM Studio or Ollama.  
The integration process requires the creation of a new provider module, typically located at skyvern/llm/providers/lmstudio.py. This custom class must inherit from BaseLLMProvider and implement the necessary methods to communicate with the local inference server.5 The registration process involves updating skyvern/llm/init.py to include the new provider key (e.g., "LM\_STUDIO") in the PROVIDERS dictionary and modifying the get\_provider() factory function to instantiate the class based on environment variables.5  
**Key Environment Variables for Custom Integration:**

* LLM\_KEY: Identifies the provider (e.g., "LM\_STUDIO").  
* LM\_STUDIO\_SERVER\_URL: The endpoint of the local server (typically http://localhost:1234/v1).  
* LM\_STUDIO\_MODEL: The specific model identifier to be used.5

This capability essentially allows the "brain" of the Skyvern agent to be replaced with a model fine-tuned on the target language (e.g., a Llama-3 model fine-tuned on Gaeilge), potentially improving the agent's ability to interpret navigational cues on monolingual Irish websites.

### **2.3 Recent Architectural Developments and Reliability**

An analysis of recent pull requests (PRs) on the Skyvern GitHub repository highlights several architectural shifts relevant to large-scale data harvesting.

* **Containerization and Deployment:** The addition of Podman support as a container runtime (PR \#4148) suggests a move towards more flexible, daemon-less deployment options, which is beneficial for running scraping clusters in restricted academic computing environments.6  
* **Database and State Management:** The shift to using SQLite in-memory by default (PR \#4207) indicates an optimization for ephemeral scraping sessions where long-term persistence of the agent's internal state is not required.6 This is ideal for "smash-and-grab" runs where the goal is simply to download a set of PDFs and terminate.  
* **Error Handling:** A specific fix for handling 403/404 errors on internal auth status endpoints (PR \#4110) addresses a common pain point in web scraping: graceful failure when a target site blocks the bot or a resource is missing.6 This ensures the agent can recover or log the error rather than crashing the entire workflow.  
* **Model Context Protocol (MCP):** The mention of "MCP Registry" and "Integrate external tools" 6 points to the adoption of the Model Context Protocol, which would allow Skyvern agents to interface with external file systems or databases directly—a feature that could streamline the pipeline from "Scrape" to "Archive."

## **3\. The Irish Ontology: Analyzing the "Gold Standard" Resources**

To effectively identify and extract data from across the Celtic nations, we must first establish a structural ontology based on the six provided Irish websites. These sites represent the full spectrum of data types: structured educational frameworks, legacy database forms, and rich geospatial multimedia archives.

### **3.1 Educational Policy and Curriculum: ncca.ie and curriculumonline.ie**

These two sites function as the central nervous system of the Irish State's educational framework. Their structure is hierarchical and document-heavy, reflecting the bureaucratic nature of curriculum development.  
Site Architecture and Content Analysis:  
The navigation structure is rigidly organized by educational stage: Early Childhood, Primary, Junior Cycle, and Senior Cycle.7

* **Early Childhood (Aistear):** This section focuses on thematic pillars such as "Well-being" and "Identity and Belonging".7 The data here is largely qualitative, contained in framework documents that describe pedagogical principles rather than specific subject content.  
* **Primary Education:** This section is currently undergoing a significant transition, evident in the "Primary Curriculum Framework" documents.9 The "old" 1999 curriculum is being replaced by a competency-based model. The site hosts specific toolkits for areas like "Arts Education," "Language," and "STEM".8 The scraping target here is the **Primary Curriculum Framework** PDF, which outlines the seven key competencies (e.g., "Being mathematical," "Being a digital learner").10 The presence of "Draft" versus "Final" specifications adds a layer of complexity; the agent must be prompted to distinguish between current legal standards and consultation documents.11  
* **Junior Cycle:** This section is highly segmented by subject (History, Geography, Gaeilge, etc.).7 It also includes "Short Courses" (e.g., Coding, Philosophy) and "Level 1/2 Learning Programmes" (L1LPs/L2LPs) designed for students with special educational needs.8 These L1LP documents are crucial for training inclusive AI models, as they break down learning outcomes into their most fundamental components.  
* **Senior Cycle:** The focus here is on high-stakes assessment subjects (Leaving Certificate) and vocational pathways like the Leaving Certificate Applied (LCA).8

Data Extraction Strategy:  
The primary value lies in the PDF specifications (e.g., PrimaryMathematicsCurriculum\_EN.pdf 10\) and the HTML-based "Toolkits".8 The PDFs are unstructured but text-rich, containing the "Learning Outcomes" that define the educational standard. The HTML toolkits offer more structured, bite-sized guidance for teachers. A Skyvern "Navigation Block" is required to traverse the menu tree (/Primary/Curriculum-Areas/Mathematics) to locate the relevant "Download" buttons.

### **3.2 The Legacy Archive: examinations.ie**

The State Examinations Commission website (examinations.ie) represents a classic example of a "Deep Web" legacy database.  
Accessibility and Interface:  
The research indicates significant difficulty in indexing or accessing deep pages on this site ("unavailable in the document" 12). This is characteristic of older government portals built on ASP.NET or similar frameworks that rely on POST requests, session cookies, and \_\_VIEWSTATE parameters rather than clean, restful URLs.  
Inferred Structure:  
Based on standard practices for such archives, the interface likely consists of a multi-step form:

1. **Year Selection:** A dropdown to select the examination year (e.g., 2023, 2022).  
2. **Examination Type:** Junior Cycle vs. Leaving Certificate.  
3. **Subject:** A dropdown list of subjects (Gaeilge, Mathematics, History).  
4. **Level:** Higher, Ordinary, Foundation.

Scraping Implications:  
Standard crawler-based approaches often fail here because they cannot manage the session state or execute the JavaScript required to populate the secondary dropdowns (e.g., the Subject list often only loads after the Year is selected). Skyvern is uniquely suited for this because it uses the visual dropdown element. The scraping prompt must instruct the agent to "Select '2023' from the Year dropdown, wait for the Subject list to update, then select 'Mathematics'."

### **3.3 Geospatial and Audio Data: canuint.ie (Taisce Chanúintí na Gaeilge)**

This site is a critical resource for linguistic data, linking audio recordings of specific dialects to precise geographic locations.  
Interface Analysis:  
The site employs a hybrid interface comprising a visual map and a text-based list structure, organized hierarchically:

* **Province (Cúige):** The top-level category (Ulster, Connacht, Leinster, Munster).14  
* **Area (Limistéar):** Sub-regions within provinces. For example, under Ulster, we find areas like "Cill Mhic Réanáin" (Kilmacrennan), "Baollaigh," and "Ráth Bhoth Theas".14 Under Munster, areas include "Corca Dhuibhne" and "Uíbh Ráthach."  
* **Locality/Townland:** The most granular level, linking to specific speakers.

Data Schema:  
The site functions as a "Linguistic Atlas." Key data points include:

* **The Lemma:** The specific Irish word being spoken (e.g., "gile," "clann," "chorrán").14  
* **The Audio Asset:** A recording of that word in the local dialect.  
* **The Spatial Coordinate:** The area/townland associated with the speaker.

Scraping Strategy:  
Navigating a canvas-based map is notoriously difficult for bots. However, the presence of a "Recordings by Area" (TAIFEADTAÍ DE RÉIR LIMISTÉIR) text list provides a reliable "backdoor" for scraping.14 The Skyvern agent should be instructed to ignore the map visualization and instead iterate through the text links for each province and area. The search function ("Cuardaigh focal Gaeilge") also allows for a dictionary-based scraping approach, where a list of common words is fed into the search bar to retrieve all dialect variants.14

### **3.4 Folklore and Manuscript Archives: duchas.ie and hiddenheritages.ai**

These platforms house the "Schools' Collection" (Bailiúchán na Scol) and other folklore archives, representing a massive corpus of handwritten and transcribed text.  
**Structure of duchas.ie:**

* **Collection Hierarchy:** The core unit is the **Volume** (Imleabhar), indexed sequentially (e.g., CBÉS 0001, CBÉS 0002).15  
* **Item Level:** Inside volumes are "Items" (Stories), often written by schoolchildren in the 1930s.  
* **Metadata:** Rich metadata includes "School Name" (e.g., Cill Éinne), "Location" (County/Townland), and "Transcription Status" (e.g., "99% transcribed").16  
* **Pagination:** The interface uses explicit pagination ("Page number / 225"), making it deterministic to scrape. An agent can be programmed to increment the page number until it reaches the limit.15

**Structure of hiddenheritages.ai:**

* **Transnational Scope:** This project explicitly links Irish and Scottish folklore, bringing together collections from University College Dublin and the University of Edinburgh.17  
* **AI Integration:** The site utilizes Transkribus AI for Handwritten Text Recognition (HTR), making previously unsearchable manuscripts accessible.17  
* **Thematic Classification:** Stories are categorized using **Aarne-Thompson (AT)** folktale types. This provides a "thematic" layer of metadata (e.g., "Type 300: The Dragon Slayer") that complements the "spatial" metadata of Dúchas.17  
* **Filtering:** The site allows filtering by country ("Éire" vs "Albain"), providing a clean separation for creating national datasets.17

## **4\. Pan-Celtic Resource Analysis: Identifying Equivalents**

Using the Irish ontology as a template, we can identify the high-priority equivalent resources in Scotland, Wales, and the Isle of Man. This comparative analysis is essential for creating a unified "Pan-Celtic" dataset that covers education, language, and heritage across all four nations.

### **4.1 Scotland (Alba)**

#### **4.1.1 Curriculum and Policy: Education Scotland**

* **Equivalent to:** ncca.ie  
* **Resource:** **Education Scotland (education.gov.scot)**  
* **Framework:** The "Curriculum for Excellence" (CfE). Unlike the Irish "Stage" system, CfE uses a "Level" system that spans age groups to allow for flexible progression.19  
  * *Early Level:* Pre-school to P1.  
  * *First to Fourth Levels:* P2 to S3.  
  * *Senior Phase:* S4 to S6.  
* **Key Documents:** "Principles and Practice" papers and "Experiences and Outcomes" (Es and Os). These documents are structurally similar to the Irish PDFs but use a specific coding system (e.g., LIT 1-01a for Literacy) which acts as a unique identifier for scraping.20  
* **Core Metadata:** The "Four Capacities" (Successful Learners, Confident Individuals, Responsible Citizens, Effective Contributors) are the fundamental metadata tags that any extracted content should be mapped to.20

#### **4.1.2 Examination Data: SQA**

* **Equivalent to:** examinations.ie  
* **Resource:** **Scottish Qualifications Authority (sqa.org.uk)**  
* **Interface Analysis:** The "Past Papers" search page is significantly more accessible than its Irish counterpart. It features standard HTML dropdowns for "Subject" and "Qualification Level".21  
* **Subject List:** The snippets explicitly list Gàidhlig-specific subjects: "Gàidhlig" (for native speakers), "Gaelic (Learners)", "Eachdraidh" (History in Gaelic), "Matamataig" (Mathematics in Gaelic).21 This separation is crucial for dataset curation.  
* **Scraping Strategy:** The dropdowns allow for a systematic loop. Skyvern can be prompted to "Select 'Gàidhlig' from the Subject list, then select 'Higher' from the Level list, then click 'Go'."

#### **4.1.3 Heritage and Audio: Tobar an Dualchais**

* **Equivalent to:** canuint.ie / duchas.ie  
* **Resource:** **Tobar an Dualchais / Kist O Riches (tobarandualchais.co.uk)**  
* **Content:** A massive repository of over 50,000 oral recordings from the School of Scottish Studies.  
* **Search Interface:** Unlike the map-heavy interface of Canuint, this site relies on a faceted search system. Users can filter by "Language" (Gaelic, Scots, English), "Genre" (Song, Story, Verse), and "Geographic Area".22  
* **Shared Ontology:** The "Hidden Heritages" project 17 confirms that data from this archive is linked to the Irish Dúchas collection, likely via the AT folktale types. This shared classification system enables the creation of a parallel corpus of Scottish and Irish folklore.

### **4.2 Wales (Cymru)**

#### **4.2.1 Curriculum and Policy: Hwb**

* **Equivalent to:** curriculumonline.ie  
* **Resource:** **Hwb (hwb.gov.wales)**  
* **Technical Context:** Hwb is a modern, dynamic web application, heavily reliant on JavaScript and React-like frameworks. This makes it a prime candidate for Skyvern's visual navigation, as traditional curl requests would likely fail to render the content.  
* **Framework:** "Curriculum for Wales" (CfW).  
* **Structure:** The curriculum is organized into six **Areas of Learning and Experience (AoLEs)**: Expressive Arts, Health and Well-being, Humanities, Languages, Literacy and Communication, Mathematics and Numeracy, Science and Technology.23  
* **Key Data:** The atomic unit of the curriculum is the "What Matters" statement. The progression is defined by "Progression Steps" (PS1 at age 5 to PS5 at age 16\) rather than year groups.24 The site also hosts a "Resources" repository with a dedicated search engine that requires interaction.23

#### **4.2.2 Examination Data: WJEC**

* **Equivalent to:** examinations.ie  
* **Resource:** **WJEC (wjec.co.uk)**  
* **Interface Analysis:** The "Past Papers" section uses a text-input search ("Type your subject here") rather than the dropdowns seen on the SQA site.25  
* **Subject List:** It auto-suggests subjects. Key targets include "Welsh Language," "Welsh Literature," "Welsh Second Language," and subjects taught through Welsh (though the interface often lists the English titles like "Geography"). The list of qualifications includes GCSE, AS/A Level, and Vocational Awards.25  
* **Scraping Strategy:** The Skyvern agent must be provided with a list of search terms (e.g., "Welsh", "Cymraeg") to input into the search bar, rather than selecting from a fixed menu.

#### **4.2.3 Heritage and Audio: People's Collection Wales**

* **Equivalent to:** canuint.ie  
* **Resource:** **People's Collection Wales (peoplescollection.wales)**  
* **Interface:** This site features a "Discover" section and a "Maps" interface similar to canuint.ie.26 It aggregates content from various archives (e.g., Glamorgan Archives, Conwy Archive Service).  
* **Content:** The collection includes "Oral History," "Photos," and "Documents." The presence of "Case Studies" suggests curated collections that could serve as high-quality, dense data sources.26

### **4.3 Isle of Man (Mannin)**

#### **4.3.1 Curriculum and Education**

* **Equivalent to:** ncca.ie  
* **Resource:** **Department of Education, Sport and Culture (gov.im)** and **Bunscoill Ghaelgagh (bunscoillghaelgagh.sch.im)**  
* **Context:** The Manx curriculum generally follows the English/Welsh model but with specific adaptations. The **Bunscoill Ghaelgagh** is unique as a Manx-medium primary school where the entire curriculum is delivered in Manx.27  
* **Key Resource:** **Culture Vannin (culturevannin.im)**. While technically a cultural foundation, it produces the bulk of Manx educational materials. The "Publications" section houses books and PDFs relevant to language learning.28  
* **Policy Context:** The "Year of the Manx Language 2026" (Blein ny Gaelgey) is a major driver for current resource creation. Grants are being awarded for projects like "Manx language opera" and "Bringing Music to the Playground," indicating a surge in new multimedia content that should be archived.29

#### **4.3.2 Heritage and Audio: LearnManx**

* **Equivalent to:** canuint.ie  
* **Resource:** **LearnManx.com** (and associated App)  
* **Significance:** This is the primary lexical database. The app contains "Hundreds of words and basic phrases" and an "Integrated bilingual dictionary with audio".30  
* **Scraping Challenge:** Much of this data is locked behind app interfaces or interactive web modules ("Digital Dialects"). Skyvern's ability to interact with web-based games/quizzes could be leveraged here to extract vocabulary lists.

## **5\. Technical Implementation: Configuring Skyvern for the Celtic Web**

This section translates the structural analysis into concrete technical specifications. We define a Global Scraping Strategy based on interaction patterns and provide the necessary configuration files.

### **5.1 Global Scraping Logic: Polymorphic Interaction Types**

To scale the extraction process, we categorize the target sites into four distinct "Interaction Types." This allows us to reuse scraping logic across nations.

| Interaction Type | Description | Target Sites (Examples) | Skyvern Block Strategy |
| :---- | :---- | :---- | :---- |
| **Type A: Hierarchical Drill-Down** | Nested menus leading to documents. | ncca.ie, curriculumonline.ie, culturevannin.im | **Navigation Block:** Traverse menu tree \-\> Extract PDF links. |
| **Type B: Complex Form Logic** | Dropdowns, dependency logic, session state. | examinations.ie, sqa.org.uk | **Navigation V2 Block:** Select Year \-\> Wait \-\> Select Subject \-\> Submit. |
| **Type C: Spatial/Map Traversal** | Map canvas or list-based geo-navigation. | canuint.ie, peoplescollection.wales | **Navigation V2 Block:** Ignore canvas; iterate through text lists of Regions/Towns. |
| **Type D: Sequential/Faceted Archive** | Paginated lists or faceted search. | duchas.ie, tobarandualchais.co.uk, hwb.gov.wales | **Navigation V2 Block:** Iterate page numbers or apply search filters. |

### **5.2 Sources Configuration (sources.yaml)**

The following YAML configuration is designed to be ingested by a scraping orchestrator. It segments the Celtic web into logical groups based on the ontology defined above.

YAML

\# sources.yaml  
\# Comprehensive Configuration for Celtic Nations Educational & Heritage Scraping

groups:  
  \- id: irish\_educational\_framework  
    description: "Primary and Post-Primary Curriculum Specifications and Toolkits"  
    targets:  
      \- url: "https://www.curriculumonline.ie/Primary/Curriculum-Areas/"  
        name: "Irish Primary Curriculum"  
        type: "Type\_A\_Hierarchical"  
        depth: 2  
        content\_types: \["pdf", "html\_toolkit"\]  
        notes: "Prioritize 'Final' specifications over 'Draft'. Look for 'Primary Curriculum Framework' PDF."  
        priority: high  
      \- url: "https://ncca.ie/en/junior-cycle/subjects/"  
        name: "NCCA Junior Cycle Subjects"  
        type: "Type\_A\_Hierarchical"  
        priority: high

  \- id: scottish\_qualifications\_and\_curriculum  
    description: "SQA Past Papers and Education Scotland CfE Documents"  
    targets:  
      \- url: "https://www.sqa.org.uk/pastpapers/findpastpaper.htm"  
        name: "SQA Past Papers"  
        type: "Type\_B\_Form"  
        inputs:  
          subject\_list: \["Gaelic (Learners)", "Gàidhlig", "Eachdraidh", "Matamataig"\]  
          levels: \["National 5", "Higher", "Advanced Higher"\]  
        priority: medium  
      \- url: "https://education.gov.scot/education-scotland/scottish-education-system/policy-for-scottish-education/policy-drivers/cfe-building-from-the-statement-of-principles"  
        name: "Curriculum for Excellence"  
        type: "Type\_A\_Hierarchical"  
        notes: "Target 'Experiences and Outcomes' PDFs."

  \- id: welsh\_digital\_learning  
    description: "Hwb Curriculum Resources and WJEC Exams"  
    targets:  
      \- url: "https://hwb.gov.wales/curriculum-for-wales/"  
        name: "Hwb Curriculum Framework"  
        type: "Type\_D\_Sequential"  
        notes: "Heavy React usage. Requires 'wait\_for\_network\_idle'. Target 'Descriptions of Learning'."  
      \- url: "https://www.wjec.co.uk/home/past-papers/"  
        name: "WJEC Past Papers"  
        type: "Type\_B\_Form\_Input"  
        query\_list:

  \- id: celtic\_audio\_spatial\_archives  
    description: "Dialect Maps and Folklore Archives"  
    targets:  
      \- url: "https://www.canuint.ie/ga/"  
        name: "Taisce Chanúintí na Gaeilge"  
        type: "Type\_C\_Spatial"  
        instruction: "Navigate via Text List 'TAIFEADTAÍ DE RÉIR LIMISTÉIR' (Recordings by Area). Do not use Map Canvas."  
      \- url: "https://www.tobarandualchais.co.uk/"  
        name: "Tobar an Dualchais"  
        type: "Type\_D\_Sequential"  
        filters:  
      \- url: "https://www.peoplescollection.wales/discover"  
        name: "Peoples Collection Wales"  
        type: "Type\_C\_Spatial"  
      \- url: "https://www.culturevannin.im/watchlisten/"  
        name: "Culture Vannin Manx Audio"  
        type: "Type\_A\_Hierarchical"

  \- id: folklore\_manuscripts  
    description: "Handwritten Text Archives"  
    targets:  
      \- url: "https://www.duchas.ie/en/cbes"  
        name: "The Schools Collection"  
        type: "Type\_D\_Sequential"  
        pagination\_indicator: "Page number / "  
        notes: "Extract Volume Number and Transcription Percentage."  
      \- url: "https://www.hiddenheritages.ai/ga/s"  
        name: "Hidden Heritages"  
        type: "Type\_D\_Sequential"  
        filters: \["Éire", "Albain"\]

### **5.3 Skyvern Scraping Prompts: Natural Language Programming**

These prompts are engineered to be fed directly into the Skyvern API. They utilize the "Prompting Guide" best practices, explicitly defining the Main Goal, Guardrails, and Payload.3

#### **Prompt 1: The Scottish Exam Harvester (Type B Interaction)**

Target: https://www.sqa.org.uk/pastpapers/findpastpaper.htm  
Block Type: Navigation V2  
GOAL:  
Download the most recent "Question Paper" and "Marking Instructions" PDFs for the subject "Gàidhlig" (Scottish Gaelic).  
**INSTRUCTIONS:**

1. **Analyze the Interface:** Locate the dropdown menu labeled "Subject".  
2. **Select Subject:** Scroll through the list and select "Gàidhlig". If "Gàidhlig" is not found, check for "Gaelic (Learners)".  
3. **Select Level:** Locate the "Qualification Level" dropdown and select "Higher".  
4. **Submit:** Click the "Go" button to execute the search.  
5. **Identify Results:** On the results page, locate the table of documents. Look for columns labeled "Question Paper" and "Marking Instructions".  
6. **Extract Data:** For the years 2024, 2023, and 2022, click the download links for both the question paper and the marking instructions.

**GUARDRAILS:**

* **Empty Results:** If the message "No results found" appears, change the "Qualification Level" to "National 5" and click "Go" again.  
* **Copyright Popups:** If a modal appears asking to accept copyright terms, click the "I Agree" or "Accept" button to proceed.  
* **File Types:** Only click links that end in .pdf.

**COMPLETION CRITERIA:**

* The browser has initiated downloads for at least 2 PDF files.  
* The agent has successfully navigated to the results page.

#### **Prompt 2: The Irish Dialect Atlas Traverser (Type C Interaction)**

Target: https://www.canuint.ie/ga/  
Block Type: Navigation V2  
GOAL:  
Extract the list of Irish words and their audio URLs for the "Cill Mhic Réanáin" area in Ulster.  
**INSTRUCTIONS:**

1. **Navigate Hierarchy:** Scroll down to the section titled "TAIFEADTAÍ DE RÉIR LIMISTÉIR" (Recordings by Area).  
2. **Select Province:** Click on the text link for "Cúige Uladh" (Ulster).  
3. **Select Area:** On the province page, locate the list of areas. Find and click on "Cill Mhic Réanáin".  
4. **Extract Content:** On the area page, you will see a list of words. For each word entry:  
   * Copy the text of the word (the Lemma).  
   * Identify the associated audio play button or link.  
   * Extract the src URL of the audio file.  
5. **Iterate:** If there are multiple pages of words for this area, find the "Next" button and continue extraction.

**GUARDRAILS:**

* **Map Avoidance:** Do not attempt to click on the interactive map canvas at the top of the page. Only use the text links in the lists below.  
* **Audio Playback:** Do not play the audio in the browser. Only extract the URL.

**COMPLETION CRITERIA:**

* The agent has visited the "Cill Mhic Réanáin" page.  
* A list of word-URL pairs has been generated.

#### **Prompt 3: The Welsh Curriculum Deep Dive (Type D Interaction)**

Target: https://hwb.gov.wales/curriculum-for-wales/  
Block Type: Navigation V2  
GOAL:  
Retrieve the text of the "Descriptions of Learning" for the Humanities Area of Learning and Experience.  
**INSTRUCTIONS:**

1. **Locate Area:** On the homepage, find the section "Areas of Learning and Experience". Click on "Humanities".  
2. **Navigate to Details:** On the Humanities page, look for a sidebar or menu item labeled "Descriptions of learning" or "Statements of what matters". Click it.  
3. **Select Progression Step:** Locate the tab or section for "Progression Step 3".  
4. **Extract Text:** Capture the full text of the learning descriptions visible on the page.  
5. **Download PDF:** If there is a button labeled "Download as PDF" or "Print this page", click it to save the structured document.

**GUARDRAILS:**

* **Dynamic Loading:** This site uses dynamic content loading. Wait for any spinning loading icons to disappear before clicking.  
* **Login Walls:** If prompted to "Log in to Hwb", ignore it. The curriculum content is public. Do not attempt to enter credentials.

**COMPLETION CRITERIA:**

* The text for "Progression Step 3" in Humanities has been displayed or downloaded.

## **6\. Conclusion: Implications for Digital Sovereignty**

The automation of data extraction from the Celtic web is a technically demanding but strategically vital undertaking. This report has demonstrated that while the cultural content across Ireland, Scotland, Wales, and the Isle of Man is deeply interconnected—sharing folklore types, linguistic roots, and educational philosophies—the digital infrastructure hosting this content is highly heterogeneous.  
The "one-size-fits-all" approach to web scraping is obsolete in this context. A successful archival strategy requires a "polymorphic" approach, utilizing Skyvern's agentic capabilities to adapt to the specific interaction paradigms of each nation: from the legacy forms of the Irish Examination Commission to the dynamic React components of the Welsh Hwb.  
Furthermore, the reliance on Skyvern's LLM integration highlights the necessity of **Digital Sovereignty** in AI. To effectively navigate these bilingual and monolingual spaces, the scraping agents must eventually be powered not by generic English-centric models, but by local, fine-tuned Celtic models. The integration of tools like LM Studio into the Skyvern pipeline is the first step towards this independence, ensuring that the preservation of Celtic heritage is conducted with tools that understand the nuance of the languages they are archiving.

#### **Works cited**

1. Skyvern-AI/skyvern: Automate browser based workflows with AI \- GitHub, accessed December 7, 2025, [https://github.com/Skyvern-AI/skyvern](https://github.com/Skyvern-AI/skyvern)  
2. Introduction | Skyvern, accessed December 7, 2025, [https://skyvern.com/docs/introduction](https://skyvern.com/docs/introduction)  
3. Prompting and Troubleshooting Guide | Skyvern, accessed December 7, 2025, [https://skyvern.com/docs/getting-started/prompting-guide](https://skyvern.com/docs/getting-started/prompting-guide)  
4. skyvern/skyvern/forge/sdk/api/llm/config\_registry.py at main · Skyvern-AI/skyvern \- GitHub, accessed December 7, 2025, [https://github.com/Skyvern-AI/skyvern/blob/main/skyvern/forge/sdk/api/llm/config\_registry.py](https://github.com/Skyvern-AI/skyvern/blob/main/skyvern/forge/sdk/api/llm/config_registry.py)  
5. Support for local LLM such as deepseek · Issue \#1783 · Skyvern-AI/skyvern \- GitHub, accessed December 7, 2025, [https://github.com/Skyvern-AI/skyvern/issues/1783](https://github.com/Skyvern-AI/skyvern/issues/1783)  
6. Pull requests · Skyvern-AI/skyvern \- GitHub, accessed December 7, 2025, [https://github.com/Skyvern-AI/skyvern/pulls](https://github.com/Skyvern-AI/skyvern/pulls)  
7. Home \- National Council for Curriculum and Assessment, accessed December 7, 2025, [https://ncca.ie/en](https://ncca.ie/en)  
8. Curriculum Online: Home, accessed December 7, 2025, [https://www.curriculumonline.ie](https://www.curriculumonline.ie)  
9. Primary Curriculum Framework For Primary and Special Schools \- Curriculum Online, accessed December 7, 2025, [https://curriculumonline.ie/getmedia/84747851-0581-431b-b4d7-dc6ee850883e/2023-Primary-Framework-ENG-screen.pdf](https://curriculumonline.ie/getmedia/84747851-0581-431b-b4d7-dc6ee850883e/2023-Primary-Framework-ENG-screen.pdf)  
10. Primary Mathematics Curriculum For Primary and Special Schools \- Curriculum Online, accessed December 7, 2025, [https://curriculumonline.ie/getmedia/484d888b-21d4-424d-9a5c-3d849b0159a1/PrimaryMathematicsCurriculum\_EN.pdf](https://curriculumonline.ie/getmedia/484d888b-21d4-424d-9a5c-3d849b0159a1/PrimaryMathematicsCurriculum_EN.pdf)  
11. Circular 0067/2025 To Boards of Management and Principal Teachers, Teaching Staff of Primary Schools and Special Schools and CEO \- Curriculum Online, accessed December 7, 2025, [https://www.curriculumonline.ie/getmedia/f3d10889-fedd-45a0-a15c-5232bb1f97c6/Circular\_Primary\_Curriculum\_Specifications\_EN.pdf](https://www.curriculumonline.ie/getmedia/f3d10889-fedd-45a0-a15c-5232bb1f97c6/Circular_Primary_Curriculum_Specifications_EN.pdf)  
12. www.examinations.ie, accessed December 7, 2025, [https://www.examinations.ie](https://www.examinations.ie)  
13. accessed January 1, 1970, [https://www.examinations.ie/exammaterialarchive/](https://www.examinations.ie/exammaterialarchive/)  
14. Taisce Chanúintí na Gaeilge, accessed December 7, 2025, [https://www.canuint.ie](https://www.canuint.ie)  
15. Schools · The Schools' Collection | dúchas.ie, accessed December 7, 2025, [https://www.duchas.ie/en/cbes](https://www.duchas.ie/en/cbes)  
16. dúchas.ie | National Folklore Collection UCD Digitization Project, accessed December 7, 2025, [https://www.duchas.ie](https://www.duchas.ie)  
17. Díchódú Oidhreachtaí Folaithe \- Hidden Heritages, accessed December 7, 2025, [https://www.hiddenheritages.ai/ga](https://www.hiddenheritages.ai/ga)  
18. Decoding Hidden Heritages, accessed December 7, 2025, [https://www.hiddenheritages.ai](https://www.hiddenheritages.ai)  
19. CfE Briefing 16 \- Curriculum for Excellence: Religious Observance (Time for Reflection) \- Glow Blogs, accessed December 7, 2025, [https://blogs.glowscotland.org.uk/fi/public/craigrothieps/uploads/sites/12726/2023/03/27140834/Religious-Observance-Time-for-Reflection.pdf](https://blogs.glowscotland.org.uk/fi/public/craigrothieps/uploads/sites/12726/2023/03/27140834/Religious-Observance-Time-for-Reflection.pdf)  
20. Building the Curriculum 1, accessed December 7, 2025, [https://www.aberdeenshire.gov.uk/media/3804/buildingthecurriculum12008.pdf](https://www.aberdeenshire.gov.uk/media/3804/buildingthecurriculum12008.pdf)  
21. SQA \- NQ \- Past papers and marking instructions, accessed December 7, 2025, [https://www.sqa.org.uk/pastpapers/findpastpaper.htm](https://www.sqa.org.uk/pastpapers/findpastpaper.htm)  
22. Tobar an Dualchais, accessed December 7, 2025, [https://www.tobarandualchais.co.uk](https://www.tobarandualchais.co.uk)  
23. Curriculum for Wales \- Hwb, accessed December 7, 2025, [https://hwb.gov.wales/curriculum-for-wales](https://hwb.gov.wales/curriculum-for-wales)  
24. a-new-curriculum-in-wales-a-guide-for-children-young-people-and-families.pdf \- Hwb, accessed December 7, 2025, [https://hwb.gov.wales/api/storage/44b74558-5d89-4a5b-bf54-32bd6dcad1c0/a-new-curriculum-in-wales-a-guide-for-children-young-people-and-families.pdf](https://hwb.gov.wales/api/storage/44b74558-5d89-4a5b-bf54-32bd6dcad1c0/a-new-curriculum-in-wales-a-guide-for-children-young-people-and-families.pdf)  
25. WJEC Past Papers, accessed December 7, 2025, [https://www.wjec.co.uk/home/past-papers](https://www.wjec.co.uk/home/past-papers)  
26. People's Collection Wales, accessed December 7, 2025, [https://www.peoplescollection.wales](https://www.peoplescollection.wales)  
27. Manx Gaelic \- Isle of Man Government, accessed December 7, 2025, [https://www.gov.im/categories/home-and-neighbourhood/manx-gaelic/](https://www.gov.im/categories/home-and-neighbourhood/manx-gaelic/)  
28. Supporting, promoting & celebrating Manx culture | Culture Vannin ..., accessed December 7, 2025, [https://www.culturevannin.im](https://www.culturevannin.im)  
29. Culture Vannin awards £26k in grants \- Manx Radio Motorsport, accessed December 7, 2025, [https://motorsport.manxradio.com/news/isle-of-man-news/culture-vannin-awards-26k-in-grants/](https://motorsport.manxradio.com/news/isle-of-man-news/culture-vannin-awards-26k-in-grants/)  
30. Learn Manx \- App Store, accessed December 7, 2025, [https://apps.apple.com/gb/app/learn-manx/id579288608](https://apps.apple.com/gb/app/learn-manx/id579288608)