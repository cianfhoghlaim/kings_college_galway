# **Celtic-Bench: A Comprehensive Technical and Linguistic Analysis of Educational Data Architectures for the Construction of Pan-Celtic Low-Resource Language Corpora**

## **Executive Summary**

The digitization of national curriculum frameworks and examination infrastructures across the British Isles has inadvertently created a fragmented yet highly valuable ecosystem of parallel and monolingual text data. For researchers in Computational Linguistics and Natural Language Processing (NLP), particularly those focused on Low-Resource Languages (LRLs), these repositories represent a "Gold Standard" of alignment, domain specificity, and grammatical rigor often absent from web-crawled corpora. This report delivers an exhaustive technical analysis of the educational data landscapes for the Celtic language family: Irish (*Gaeilge*), Scottish Gaelic (*Gàidhlig*), Welsh (*Cymraeg*), and Manx (*Gaelg*).  
The primary focus of this investigation is the feasibility of constructing a robust, multilingual machine learning dataset—tentatively titled "Celtic-Bench"—by systematically scraping and aligning data from three primary Irish domains: examinations.ie, ncca.ie, and curriculumonline.ie. Furthermore, the analysis extends to identifying and characterizing the equivalent data architectures in Northern Ireland (Council for the Curriculum, Examinations & Assessment \- CCEA), Scotland (Scottish Qualifications Authority \- SQA), Wales (WJEC/CBAC), and the Isle of Man (Department of Education, Sport and Culture \- DESC).  
Our findings indicate that while the Republic of Ireland offers the most deterministically aligned bilingual data via predictable filename conventions (specifically the EV/IV taxonomy), the architectures in Scotland and Wales offer distinct advantages in terms of domain breadth and volume, respectively. Conversely, Northern Ireland and the Isle of Man present significant challenges related to data scarcity and archival inconsistency, necessitating bespoke extraction strategies. This report outlines the specific technical stacks, URL patterns, document structures, and linguistic nuances necessary to execute a pan-Celtic data ingestion pipeline.

## ---

**1\. The Irish Data Ecosystem: Architecture, Taxonomy, and Scraping Dynamics**

The educational infrastructure of the Republic of Ireland serves as the foundational anchor for any proposed Celtic dataset. The integrity of the Irish language, protected by constitutional status and integrated into the state apparatus, has resulted in a digital ecosystem where bilingualism is not merely a feature but a structural requirement. This section analyzes the three primary pillars of this ecosystem: assessment (examinations.ie), curriculum specification (ncca.ie), and resource dissemination (curriculumonline.ie).

### **1.1 State Examinations Commission (examinations.ie): The Archive of Parallelism**

The domain examinations.ie is arguably the single most critical source for parallel text data in the Celtic sphere. Unlike general web content, which may suffer from loose translation or summarization, the high-stakes nature of the Leaving Certificate and Junior Cycle examinations mandates strict semantic equivalence between English and Irish versions of examination papers to ensure candidate fairness.

#### **1.1.1 Archive Architecture and File Distribution**

The archive functions primarily as a repository of static files, predominantly PDF, but also including DOCX, ZIP, and MP4 formats for coursework components.1 The site does not utilize a modern RESTful API for public access. Instead, it relies on a query-string-based retrieval system or static directory listings populated by server-side scripts (likely PHP or ASP based on legacy headers).  
A critical architectural feature advantageous for scraping is the distinct separation of language versions. While some jurisdictions produce bilingual booklets where languages are interleaved (complicating text extraction), the State Examinations Commission (SEC) frequently hosts separate PDF files for the English and Irish versions of the same exam paper.2 This separation significantly reduces the "noise" associated with extracting parallel text, as the scraper does not need to distinguish language boundaries within a single document stream.  
The archive covers a vast temporal range, with snippet data confirming the availability of papers ranging from 2005 to 2025\.1 The file sizes vary significantly depending on the subject and year, from compact text-based PDFs (e.g., 2014 Irish LC HL.pdf at 112.98 KB) to larger scans (e.g., 2022 Irish LC HL.pdf at 1.54 MB).1 This variance suggests that a robust ingestion pipeline must include an Optical Character Recognition (OCR) layer to handle older, image-based PDFs, while modern papers can be parsed directly.

#### **1.1.2 The EV / IV Filename Rosetta Stone**

Deep forensic analysis of the file naming conventions used for coursework and digital submissions reveals a highly consistent taxonomy that serves as a "Rosetta Stone" for automated alignment. Research snippets explicitly detail a convention where the core file identifier is suffixed or tagged to denote the language version.2 This finding is pivotal for constructing a deterministic scraper.  
The convention follows a logic where the filename is composed of the Year, Subject Code, Level/Component, and a Language Tag.

* **English Version (EV):** Files intended for English-medium schools or candidates are tagged with EV.  
* **Irish Version (IV):** Files intended for *Gaelcholáistí* (Irish-medium schools) or candidates taking the exam through Irish are tagged with IV.

**Table 1: Derived Filename Logic for examinations.ie Deterministic Scraping**

| Component | Logic / Format | Example Data Points | Interpretation |
| :---- | :---- | :---- | :---- |
| **Year** | YYYY | 2022, 2025 | The exam year. |
| **Subject Code** | 3-digit Integer | 034, 024, 219, 225 | 034 (Economics), 024 (Ag Science), 219 (Computer Science), 225 (PE). |
| **Component** | Single Digit/Char | 2, 3, A, B, C | Represents the specific paper or project section (e.g., 2 for Ordinary Level or Project Report). |
| **Language Tag** | **EV** vs **IV** | EV, IV | The primary key for language alignment. |
| **Candidate ID** | 6-digit Integer | 123456 | Variable placeholder for individual coursework files. |
| **Extension** | docx, pdf, zip, mp4 | .docx, .zip | Indicates content type (Text vs Multimedia). |

Analysis of Causal Implications for Scraping:  
The existence of the EV/IV nomenclature allows for a predictive scraping strategy. Rather than relying solely on crawling links (which may be broken or hidden behind form submissions), a scraper utilizing a tool like Crawl4AI can iterate through known subject codes and years to predict the IV URL based on the successfully located EV URL.  
For example, if the scraper successfully identifies 2022-034-2-EV.docx (Economics Research Study, English), it can infer with high probability the existence of 2022-034-2-IV.docx (Economics Research Study, Irish).2 This allows for the construction of a parallel corpus even if the Irish version is not explicitly linked on the main index page due to CMS errors.  
Furthermore, the presence of specific file types like .zip and .mp4 for subjects like Computer Science and Physical Education 2 adds a layer of complexity.

* **ZIP Archives:** The Computer Science coursework (2022-219-3-IV-123456.zip) likely contains code files (Python, Java) and documentation. This presents a unique opportunity to build a "Code-Switching" dataset, analyzing how Irish variable names and comments are used in programming contexts.  
* **Multimedia:** The Physical Education component includes video files (.mp4). While outside the scope of text extraction, the associated metadata and filenames provide evidence of the thoroughness of the Irish-medium provision.

#### **1.1.3 Subject Coverage and Semantic Domains**

The breadth of subjects available in Irish provides a rich spectrum of domain-specific vocabulary that extends far beyond the literary or conversational Irish found in standard training datasets.

* **STEM Domain:** Mathematics, Physics, Chemistry, and Computer Science papers in Irish provide rare technical terminology. Terms like "vectors" (*veicteoirí*), "thermodynamics" (*teirmidinimic*), and "algorithms" (*algartaim*) are rigorously standardized in these documents. The examinations.ie archive includes papers for "Agricultural Science" (024) and "Economics" (034) in Irish, offering vocabulary related to soil science, macroeconomics, and market theory.2  
* **Humanities Domain:** History and Geography papers offer high-level discourse markers and complex sentence structures. These documents are essential for training translation models on argumentation, cause-and-effect reasoning, and narrative construction. The "Politics and Society" subject (568), with its "Citizenship Project" (2022-568-2-IV), likely contains contemporary sociological and political vocabulary.2  
* **Literary Irish vs. Functional Irish:** The specific "Irish" subject exams (L1 and L2) contain literary criticism, poetry, and prose. Snippets indicate a clear distinction between the syllabus for Irish-medium schools (*L1*) and English-medium schools (*L2*).3 The L1 papers assume native fluency and engage with complex literary texts, while L2 papers focus closer on communicative competence. For a machine learning dataset, distinguishing between these source types is vital; L1 papers provide "gold standard" natural language, while L2 papers may contain simpler, more constrained text suitable for learner modeling.

### **1.2 National Council for Curriculum and Assessment (ncca.ie): The Specification Layer**

While examinations.ie provides the "test set"—the output of the educational process—ncca.ie provides the "training set"—the specifications and guidelines that define the input.

#### **1.2.1 The Bilingual Toggle and Document Structure**

The NCCA website typically employs a Content Management System (CMS) that supports bilingual viewing. Snippets suggest that guidelines are often presented in distinct sections or via separate PDF downloads for English and Irish.4 The "Primary Language Curriculum" (*Curaclam Teanga na Bunscoile*) is a critical document explicitly designed for both English-medium and Irish-medium contexts.5  
The structural insight here is the "Learning Outcome" framework. These specifications are text-heavy definitions of skills.

* **Parallelism:** The curriculum explicitly connects English and Irish learning outcomes, categorizing skills into strands such as "Communicating" (*Ag Cumarsáid*), "Understanding" (*Ag Tuiscint*), and "Exploring and Using" (*Ag Fiosrú agus Ag Úsáid*).6  
* **Data Density:** Unlike exam papers which are sparse (questions), curriculum documents are dense prose describing pedagogical goals. This makes them excellent for training alignment models on abstract, educational terminology.

#### **1.2.2 Tech Stack and Scraping Strategy**

The NCCA and its associated portals appear to use dynamic web technologies. The presence of interactive elements like "Strands" and "Elements" 5 suggests that content is likely stored in a structured database and rendered via JavaScript templates.

* **Scraping Impediment:** A simple curl or requests call might only retrieve the shell of the page.  
* **Solution:** A strategy using Crawl4AI with a headless browser (like Playwright) is necessary. The JsonCssExtractionStrategy mentioned in technical documentation 7 would be ideal here. By defining a schema that targets the specific CSS classes for English (.lang-en) and Irish (.lang-ga) columns in the curriculum tables, the scraper can extract structured, aligned text directly from the HTML, bypassing the need for PDF parsing.

### **1.3 Curriculum Online (curriculumonline.ie): The Digital Interface**

This portal acts as the user-facing frontend for the frameworks developed by the NCCA.

#### **1.3.1 Granularity and Metadata**

The site is organized by educational stage: Early Childhood (*Aistear*), Primary, Junior Cycle, and Senior Cycle.8

* **Metadata Richness:** The site hosts the "Primary Language Curriculum" which incorporates Irish, English, and Modern Foreign Languages. The presence of headings like "Teanga ó Bhéal" (Oral Language), "Léitheoireacht" (Reading), and "Scríbhneoireacht" (Writing) alongside their English equivalents confirms the bilingual nature of the metadata.6  
* **Navigation:** The breakdown into "Short Courses" and "Level 1/2 Learning Programmes" 8 indicates a hierarchical URL structure (e.g., /primary/curriculum-areas/primary-language/). This predictability aids in recursive crawling.

#### **1.3.2 Dynamic Content Delivery**

The site appears to be dynamic, potentially using JavaScript to load content based on user selection (filtering by school type and strand).8 This reinforces the need for a browser-based scraper. The "progression continua" mentioned in the snippets 5 are likely complex, multi-row tables that require precise row-by-row extraction to maintain alignment between the English description of a skill level and its Irish equivalent.

## ---

**2\. Comparative Analysis of Celtic Equivalents in the British Isles**

To build a truly pan-Celtic dataset, the Irish data must be augmented with data from the UK jurisdictions. The analysis below maps the Irish resources to their nearest equivalents in Scotland, Wales, Northern Ireland, and the Isle of Man, highlighting the technical and linguistic disparities that the scraping pipeline must address.

### **2.1 Scotland: The Scottish Qualifications Authority (SQA) and the *Gàidhlig* Corpus**

The SQA represents the most robust equivalent to the SEC in Ireland, offering a significant volume of distinct, high-quality Gaelic-medium examination papers.

#### **2.1.1 Architectural Divergence: The "X-Code" System**

Unlike the Irish system which uses a filename suffix (EV/IV) to distinguish languages for the same subject code, the SQA assigns **entirely distinct course codes** to the Gaelic-medium versions of subjects. This is a critical architectural difference that the scraper must account for.

* **English Medium Mathematics:** Code **C847 76** / Assessment Code **X847 76**.10  
* **Gaelic Medium Mathematics (*Matamataig*):** Code **C874 76** / Assessment Code **X874 76**.11  
* **English Medium History:** Code **C837 76** / Assessment Code **X837 76**.12  
* **Gaelic Medium History (*Eachdraidh*):** Code **X872** (Course).11

**Insight:** A scraper cannot simply append a language tag to a URL. It must utilize a lookup table mapping English subject codes to their Gaelic counterparts. The SQA publishes these codes in "National Ratings" tables or course specification documents.11 The scraper logic must be: "If fetching X847 (Maths), also fetch X874 (Matamataig)."

#### **2.1.2 Linguistic Content and "Modified" Papers**

The SQA archive includes "Modified" papers from the Covid-19 era (2020-2022), where content was reduced to accommodate lost teaching time.13 This introduces a "data alignment noise" factor; a 2022 *Matamataig* paper might not align perfectly with a 2019 English Maths paper in terms of question count or topic coverage.

* **Specific Subject Availability:**  
  * ***Eachdraidh*** **(History):** Offers rich narrative text in Gaelic. The snippet mentions specific papers for "Scottish History" and "British, European and World History" in Gaelic.14  
  * ***Cruinn-eòlas*** **(Geography):** Offers technical geographic terminology regarding landforms, climate, and demographics.15  
  * ***Nuadh-eòlas*** **(Modern Studies):** This subject, unique to Scotland, covers politics, sociology, and international relations. It offers valuable vocabulary related to democracy, rights, and social issues in Gaelic.16  
  * ***Matamataig*** **(Mathematics):** Offers logic and numeric terminology.17

#### **2.1.3 The *Gaelic (Learners)* vs *Gàidhlig* Distinction**

Similar to the Irish *L1/L2* distinction, Scotland structurally separates *Gaelic (Learners)* (taught as a foreign language) from *Gàidhlig* (taught as a native language/medium of instruction).15

* **Dataset Implication:** *Gaelic (Learners)* papers (Reading/Writing/Listening) are suited for simpler, learner-focused datasets (A1-B2 CEFR levels). *Gàidhlig* and subject-specific papers (e.g., *Eachdraidh*) are essential for advanced, domain-specific models (C1+ level), as they assume native-like competence.

### **2.2 Wales: WJEC / CBAC and the Volume of Bilingualism**

The Welsh education system is arguably the most linguistically integrated in the British Isles, with the WJEC (Corff Cyd-bwyllgor Addysg Cymru) providing a massive volume of parallel data due to the widespread nature of Welsh-medium education.

#### **2.2.1 Bilingual Layouts vs Separate Files**

Unlike the SEC (EV/IV) or SQA (Distinct Codes), the WJEC frequently utilizes **bilingual PDF layouts** where English and Welsh text appear side-by-side or on facing pages within the *same* document.19

* **Evidence:** Snippets reference "Question Paper (Test A)" without explicit "Welsh Only" file distinctions for some units. Instead, instructions often appear in both languages (e.g., "Answer all questions... / Atebwch bob cwestiwn...").19  
* **Technical Challenge:** Extracting parallel text from a single bilingual PDF is technically more demanding than aligning two separate files. A standard text extraction (e.g., pypdf) might read across columns, interleaving English and Welsh sentences into a single incoherent string.  
* **Solution:** The pipeline must use **layout-aware PDF parsing** (e.g., pdfplumber or Azure Document Intelligence). The strategy involves defining bounding boxes for the left column (English) and right column (Welsh) and extracting them as separate streams.

#### **2.2.2 "Made-for-Wales" Specifications and Codes**

The new "Made-for-Wales" GCSEs introduce specific units for *Cymraeg* (Welsh Language) and *English Literature*.21

* **Code Patterns:** The WJEC uses a complex suffix system.  
  * English Unit: 3100UA0-1 (History \- Elizabethan Age).23  
  * Welsh Unit: Snippets imply a code variation, often utilizing C prefixes or specific "Welsh Medium" designations in the portal metadata.24  
  * **Prefix Logic:** Snippet 25 shows 3510U10-1 (Business WALES) and C510U10-1 (Business Eduqas). This suggests the C prefix might denote the *Eduqas* (England) board in some contexts, or specific Welsh units in others. Careful validation is required to ensure C-coded papers are indeed the Welsh-language versions and not just English papers for the Eduqas board. The "Question Bank" tool 26 allows filtering by language, which might be a safer scraping target than the raw PDF archive.

### **2.3 Northern Ireland: Council for the Curriculum, Examinations & Assessment (CCEA)**

The data landscape in Northern Ireland is characterized by scarcity and a lack of systematic digital archiving for Irish-medium papers compared to the Republic.

#### **2.3.1 The Translation Gap and Data Scarcity**

Research indicates a systemic issue where Irish-medium past papers are not consistently uploaded or are difficult to locate.27 Teachers in the Irish-medium sector explicitly complain that "there's no past papers translated... It'll all be just there for the English medium sectors".27

* **Implication for Scraping:** A scraper targeting CCEA for Irish data will likely yield a high number of 404 errors or empty directories. The "Translation Gap" means that even if the paper existed physically on exam day, it may not exist digitally.

#### **2.3.2 Identifying Irish Medium Papers**

Where data *does* exist, it follows a specific coding structure.

* **Unit Codes:** English Maths Foundation is GMC11.28 The Irish version, if archived, would likely share this code or have a specific identifier within the "Irish Medium" section of the portal.  
* **Curriculum Context:** The NI curriculum emphasizes "Cross-Curricular Skills" (Communication, Using Mathematics, Using ICT).29 Documents describing these skills in Irish would provide valuable pedagogical vocabulary.  
* **BBC Bitesize Integration:** Recently, CCEA past papers have been added to BBC Bitesize.30 This partnership might offer a more organized repository than the CCEA's own legacy site. If BBC Bitesize hosts the Irish-medium versions (which they often do for Welsh/Gaelic), this could be a superior scraping target.

### **2.4 Isle of Man: Department of Education, Sport and Culture (DESC)**

The Manx language (*Gaelg*) represents the most extreme low-resource environment in this analysis.

#### **2.4.1 Examination Structure: *Teisht Chadjin***

The Isle of Man offers the *Teisht Chadjin Ghaelgagh* (TCG), equivalent to a GCSE, and the *Ard Teisht*, equivalent to an A-Level.31

* **Data Availability:** Online resources are minimal. Snippets explicitly state "unable to display resource" for past papers on the manxlanguage.sch.im portal.33 This suggests the papers are not hosted publicly in a digital format.  
* **Validation:** The qualifications are validated in consultation with the CCEA (Northern Ireland) 31, but the papers themselves are produced locally and appear to be circulated internally or in physical formats.

#### **2.4.2 *Bunscoill Ghaelgagh*: The Primary Text Source**

Given the lack of exam papers, the primary source of digital text is the *Bunscoill Ghaelgagh* (primary school) website.

* **Document Types:** The school hosts newsletters (Newsletter\_Sept\_25.pdf) and policy documents.35  
* **Bilingualism:** School policies (e.g., "Access to the Curriculum") are often bilingual to comply with department regulations. Newsletters frequently contain mixed English and Manx text, providing contemporary usage examples.37  
* **Strategy:** Scraping bunscoillghaelgagh.sch.im for all PDF content is the most viable path to building a small but high-quality Manx corpus. The volume will be low (thousands of words rather than millions), but highly specific to the education domain.

## ---

**3\. Deep Dive: Tech Stack Analysis of Irish Sources**

To facilitate the scraping required for the user's query, we must reverse-engineer the technical delivery methods of the Irish portals.

### **3.1 examinations.ie (SEC)**

* **Server/Platform:** The site appears to be running on an older architecture, likely PHP or ASP-based, serving static files via query parameters.  
* **URL Pattern:** https://www.examinations.ie/archive/exampapers///.pdf.  
* **Scraping Impediments:**  
  * **Session Management:** The site does not appear to require complex authentication for the archive, but rate limiting may be present.  
  * **PDF Formatting:** The older files (pre-2010) may be scanned images rather than text-based PDFs. This requires an OCR (Optical Character Recognition) step in the ingestion pipeline.  
  * **Dynamic Links:** The "Material Archive" uses a JavaScript-based selector that populates dropdowns.1 A headless browser (Selenium/Playwright) is required to simulate the selection of "Year \-\> Subject \-\> Level" to expose the direct download links.

### **3.2 ncca.ie and curriculumonline.ie**

* **Tech Stack:** These are modern, responsive web applications. curriculumonline.ie uses a CMS that renders content dynamically.  
* **Data Delivery:** Content is often delivered as HTML text within \<div\> tags rather than solely as PDFs.6 This is advantageous for text scraping as it bypasses PDF parsing errors.  
* **Structure:** The "Primary Language Curriculum" uses a tabbed interface (Strands, Elements, Outcomes).5  
* **Scraping Strategy:** Crawl4AI is highly recommended here. Its JsonCssExtractionStrategy can be configured to target the specific CSS selectors for the learning outcomes (e.g., .learning-outcome, .strand-header).7

### **3.3 Tech Stack of the "4schools" and "Examcraft" Mirrors**

Third-party sites like 4schools.ie and examcraft.ie act as secondary repositories.38

* **Value:** They often sell physical copies but list digital metadata.  
* **Risk:** They are commercial storefronts (Shopify or similar ecommerce platforms) and are less likely to host free, scrapeable full-text PDFs compared to the official SEC site. They should be used only for metadata verification (e.g., verifying if an Irish version of a "History Chart" exists).

## ---

**4\. Multilingual Dataset Construction Strategy**

Based on the analysis, the following pipeline is proposed for building the Celtic Multilingual Dataset.

### **4.1 Phase 1: The Irish Core (Gaeilge-English)**

1. **Crawler Configuration:** Use Crawl4AI with AsyncWebCrawler.  
2. **Target:** examinations.ie.  
3. **Heuristic:**  
   * Iterate Years 2000 to 2025\.  
   * Iterate Subject Codes.  
   * Download all PDFs.  
   * **Alignment:** Match files with Hamming distance on filenames (e.g., LC003ALP000EV.pdf and LC003ALP000IV.pdf). If EV and IV are swapped but the rest of the string is identical, pair them.  
4. **Extraction:** Convert PDFs to Markdown. Use regex to strip "Page X of Y" and "Examination Number" headers.

### **4.2 Phase 2: The Scottish Extension (Gàidhlig-English)**

1. **Target:** sqa.org.uk Past Paper Search.  
2. **Lookup Table Generation:**  
   * Scrape the SQA "National Ratings" Excel files 11 to build a dictionary of Subject Codes.  
   * Map Mathematics (C847) \-\> Matamataig (C874).  
   * Map History (C837) \-\> Eachdraidh (X872).  
3. **Ingestion:** Download paired PDFs based on these code mappings.  
4. **Verification:** Check the first page of the PDF for the string "Gàidhlig" vs "Intermediate 2" or "Higher" to confirm language.

### **4.3 Phase 3: The Welsh Volume (Cymraeg-English)**

1. **Target:** wjec.co.uk.  
2. **Strategy:** Focus on "Bilingual Papers."  
3. **Parsing:** Use a layout-aware PDF parser (e.g., Microsoft Azure Form Recognizer or a fine-tuned LayoutLM model).  
   * *Logic:* If text is in two columns, detect the language of column A (English) and column B (Welsh).  
   * *Split:* Segment the PDF into two parallel text streams.

### **4.4 Phase 4: The Manx and NI Supplement (Gaelg & Gaeilge)**

1. **Target:** bunscoillghaelgagh.sch.im and ccea.org.uk.  
2. **Strategy:** Manual curation / Low-volume scraping.  
   * For Manx, scrape the *Bunscoill* newsletters.35 Use Crawl4AI to extract text from the "Manx Language Strategy" documents on desc.gov.im.31  
   * For NI, use the BBC Bitesize links mentioned in snippet 30 if CCEA's archive proves empty.

## ---

**5\. Linguistic Insights & Ripple Effects**

The construction of this dataset reveals broader trends in the preservation of Celtic languages through technology.

### **5.1 The "Translation Gap" as a Proxy for Vitality**

The availability of parallel data correlates directly with the vitality and legal status of the language.

* **High Vitality (Welsh/Irish):** State-mandated translation creates a steady stream of data (examinations.ie, wjec.co.uk).  
* **Medium Vitality (Scottish Gaelic):** Specialized subjects (*Eachdraidh*) exist, but the lack of a universal translation policy (unlike Ireland's EV/IV system) creates data silos.  
* **Low Vitality (Manx):** The absence of past papers forces reliance on primary school materials, severely limiting the domain complexity (e.g., no "Manx Physics" vocabulary) available for AI training.

### **5.2 Domain Specificity and Semantic Drift**

Analyzing the Irish History papers (Stair) vs Scottish History papers (*Eachdraidh*) reveals divergent semantic domains.

* **Irish History:** Focuses on "Revolutionary Period," "Land League," and "Home Rule".40  
* **Scottish History:** Focuses on "Wars of Independence," "Clearances," and "Treaty of Union".41  
* *Insight:* A model trained only on Irish Gaeilge history texts will hallucinate Irish political context when processing Scottish Gaelic history texts, despite the linguistic similarities. Distinct tags for \<Gaeilge\_History\> and \<Gàidhlig\_History\> are essential.

### **5.3 Standardization vs. Dialect**

The "Standard Irish" (*An Caighdeán Oifigiúil*) used in examinations.ie is highly standardized. In contrast, older texts or regional resources (like those from specific Gaeltacht schools) might exhibit dialectal variations. The dataset must effectively tag the source to distinguish between "Official Standard" (Exam Papers) and "Natural Language" (Literature exams or creative writing samples).

## ---

**6\. Conclusion and Roadmap**

To satisfy the user's request for a multilingual dataset, the immediate priority is the development of a scraper targeting the **Republic of Ireland's examinations.ie**. Its EV/IV file naming convention offers the highest return on investment for aligned text.  
Following this, the **Scottish SQA** repository offers the second-best quality, provided the X-code mapping strategy is implemented. The **Welsh** data requires advanced PDF parsing, while **Northern Ireland** and the **Isle of Man** serve as supplementary sources for specific niche vocabulary rather than bulk parallel text.  
**Recommendation:** Proceed with Crawl4AI for the web-based curriculums and a custom Python script using requests and PyPDF2 (or OCR tools) for the bulk PDF archives, implementing the filename logic detailed in Table 1\.

#### **Works cited**

1. Leaving Cert Irish HL \- educateplus, accessed December 7, 2025, [https://www.educateplus.ie/markingscheme/leaving-cert-irish-higher-level](https://www.educateplus.ie/markingscheme/leaving-cert-irish-higher-level)  
2. For the attention of School Authorities. Opening of School Portal \- For attention of the Physical Education, Computer Science, Economics, Agricultural Science and Politics & Society Teachers | Coláiste Pobail Setanta, accessed December 7, 2025, [https://cpsetanta.ie/News/For-the-attention-of-School-Authorities-Opening-of-School-Portal--For-attention-of-the-Physical-Education,-Computer-Science,-Economics,-Agricultural-Science-and-Politics-Society-Teachers/95498/Index.html](https://cpsetanta.ie/News/For-the-attention-of-School-Authorities-Opening-of-School-Portal--For-attention-of-the-Physical-Education,-Computer-Science,-Economics,-Agricultural-Science-and-Politics-Society-Teachers/95498/Index.html)  
3. Leaving Cert Exam Papers: Gaeilge/Irish | Schoolbooks Advice, accessed December 7, 2025, [https://schoolbooks.ie/blogs/advice-centre/leaving-cert-exam-papers-gaeilge](https://schoolbooks.ie/blogs/advice-centre/leaving-cert-exam-papers-gaeilge)  
4. NCCA EAL Guidelines for Schools \- Irish National Teachers' Organisation, accessed December 7, 2025, [https://www.into.ie/app/uploads/2019/07/NCCA\_EALGuidelines.pdf](https://www.into.ie/app/uploads/2019/07/NCCA_EALGuidelines.pdf)  
5. Primary Language Curriculum \- National Council for Special Education, accessed December 7, 2025, [https://ncse.ie/primary-language-curriculum](https://ncse.ie/primary-language-curriculum)  
6. Oral Language | Curriculum Online, accessed December 7, 2025, [https://www.curriculumonline.ie/primary/curriculum-areas/primary-language/oral-language/](https://www.curriculumonline.ie/primary/curriculum-areas/primary-language/oral-language/)  
7. Extraction & Chunking Strategies API \- Crawl4AI, accessed December 7, 2025, [https://docs.crawl4ai.com/api/strategies/](https://docs.crawl4ai.com/api/strategies/)  
8. Primary Language \- Curriculum Online, accessed December 7, 2025, [https://www.curriculumonline.ie/primary/curriculum-areas/primary-language/](https://www.curriculumonline.ie/primary/curriculum-areas/primary-language/)  
9. Curriculum Online: Home, accessed December 7, 2025, [https://www.curriculumonline.ie/](https://www.curriculumonline.ie/)  
10. Higher Mathematics Course Specification \- SQA, accessed December 7, 2025, [https://www.sqa.org.uk/files\_ccc/h-course-spec-mathematics.pdf](https://www.sqa.org.uk/files_ccc/h-course-spec-mathematics.pdf)  
11. National 5 \- SQA, accessed December 7, 2025, [https://www.sqa.org.uk/sqa//files\_ccc/foi-23-24-091-national-ratings-august-2023.xlsx](https://www.sqa.org.uk/sqa//files_ccc/foi-23-24-091-national-ratings-august-2023.xlsx)  
12. Higher History Course Specification \- SQA, accessed December 7, 2025, [https://www.sqa.org.uk/sqa/files\_ccc/h-history-course-specification.pdf](https://www.sqa.org.uk/sqa/files_ccc/h-history-course-specification.pdf)  
13. SQA \- NQ \- Past papers and marking instructions, accessed December 7, 2025, [https://www.sqa.org.uk/pastpapers/findpastpaper.htm](https://www.sqa.org.uk/pastpapers/findpastpaper.htm)  
14. Higher History \- Course overview and resources \- SQA, accessed December 7, 2025, [https://www.sqa.org.uk/sqa/47923.html](https://www.sqa.org.uk/sqa/47923.html)  
15. Past papers and marking instructions \- Results \- SQA, accessed December 7, 2025, [https://www.sqa.org.uk/pastpapers/findpastpaper.htm?subject=\&level=NH](https://www.sqa.org.uk/pastpapers/findpastpaper.htm?subject&level=NH)  
16. National 5 Modern Studies \- Course overview and resources \- SQA, accessed December 7, 2025, [https://www.sqa.org.uk/sqa/47448.html](https://www.sqa.org.uk/sqa/47448.html)  
17. 2022 Higher Matamataig Paper 1 Non-calculator Question Paper \- SQA, accessed December 7, 2025, [https://www.sqa.org.uk/pastpapers/papers/papers/2022/NH\_Matamataig\_Paper1-Non-calculator\_2022.pdf](https://www.sqa.org.uk/pastpapers/papers/papers/2022/NH_Matamataig_Paper1-Non-calculator_2022.pdf)  
18. Past papers and marking instructions \- Results \- SQA, accessed December 7, 2025, [https://www.sqa.org.uk/pastpapers/findpastpaper.htm?subject=Gaelic\&searchText=\&level=NAH\&includeMiVal=](https://www.sqa.org.uk/pastpapers/findpastpaper.htm?subject=Gaelic&searchText&level=NAH&includeMiVal)  
19. HISTORY SAMPLE ASSESSMENT MATERIALS \- WJEC, accessed December 7, 2025, [https://www.wjec.co.uk/media/rerhwfcy/wjec-gcse-history-sams-unit-3-e.pdf](https://www.wjec.co.uk/media/rerhwfcy/wjec-gcse-history-sams-unit-3-e.pdf)  
20. HISTORY SAMPLE ASSESSMENT MATERIALS \- WJEC, accessed December 7, 2025, [https://www.wjec.co.uk/media/bfxnffeq/wjec-gcse-history-sams-unit-2-e.pdf](https://www.wjec.co.uk/media/bfxnffeq/wjec-gcse-history-sams-unit-2-e.pdf)  
21. Made-for-Wales GCSE History update \- WJEC, accessed December 7, 2025, [https://www.wjec.co.uk/articles/made-for-wales-gcse-history-update/](https://www.wjec.co.uk/articles/made-for-wales-gcse-history-update/)  
22. WJEC \- Repository \- Hwb, accessed December 7, 2025, [https://hwb.gov.wales/repository/publishers/20e7897e-4c13-4f55-8c9a-3e777ee5c64e](https://hwb.gov.wales/repository/publishers/20e7897e-4c13-4f55-8c9a-3e777ee5c64e)  
23. WJEC GCSE History Past Papers \[PDFs & Mark Schemes\] \- Save My Exams, accessed December 7, 2025, [https://www.savemyexams.com/gcse/history/wjec/past-papers/](https://www.savemyexams.com/gcse/history/wjec/past-papers/)  
24. WJEC Wales and Eduqas Summer 2025 FINAL Examination Timetable \- Chipping Campden School, accessed December 7, 2025, [https://campden.school/wp-content/uploads/2024/08/Eduqas-GCSE-Summer-2025-TT.pdf](https://campden.school/wp-content/uploads/2024/08/Eduqas-GCSE-Summer-2025-TT.pdf)  
25. WJEC Wales and Eduqas Summer 2025 Provisional Examination Timetable, accessed December 7, 2025, [https://www.wjec.co.uk/media/amodrdvh/summer-2025-wales-and-eduqas-gcse-provisional.pdf](https://www.wjec.co.uk/media/amodrdvh/summer-2025-wales-and-eduqas-gcse-provisional.pdf)  
26. Question Bank \- WJEC, accessed December 7, 2025, [https://www.wjec.co.uk/home/question-bank/](https://www.wjec.co.uk/home/question-bank/)  
27. Teacher Workload in the Irish-medium sector \- Comhairle na Gaelscolaíochta, accessed December 7, 2025, [https://www.comhairle.org/gaeilge/wp-content/uploads/sites/2/2025/09/Teacher-Workload-in-the-Irish-medium-Sector-Evidential-Insights-TUAIRISC-DEIRIDH-Bealtaine-2025.pdf](https://www.comhairle.org/gaeilge/wp-content/uploads/sites/2/2025/09/Teacher-Workload-in-the-Irish-medium-Sector-Evidential-Insights-TUAIRISC-DEIRIDH-Bealtaine-2025.pdf)  
28. GCSE Mathematics January 2019 Exam Paper | PDF | Kilogram \- Scribd, accessed December 7, 2025, [https://www.scribd.com/document/719817359/Revised-GCSE-MATH-REVISED-Past-Papers-Mark-Schemes-Standard-January-Series-2019-27911](https://www.scribd.com/document/719817359/Revised-GCSE-MATH-REVISED-Past-Papers-Mark-Schemes-Standard-January-Series-2019-27911)  
29. STRATEGIC REVIEW OF THE NORTHERN IRELAND CURRICULUM SUMMARY OF STAKEHOLDER ENAGEMENT AND ANALYSIS OF RECURRING THEMES \- Public now, accessed December 7, 2025, [https://docs.publicnow.com/viewDoc.aspx?filename=98308\\EXT\\721A7B12F917E836B9E4FDEDE0F13CFAA06A4B77\_07F5655387A68D62447576DFFB072BF6B885C92A.PDF](https://docs.publicnow.com/viewDoc.aspx?filename=98308%5CEXT%5C721A7B12F917E836B9E4FDEDE0F13CFAA06A4B77_07F5655387A68D62447576DFFB072BF6B885C92A.PDF)  
30. BBC Bitesize adds CCEA past papers to support NI GCSE pupils \- Ireland Live, accessed December 7, 2025, [https://www.ireland-live.ie/news/derry-now/1961059/bbc-bitesize-adds-ccea-past-papers-to-support-ni-gcse-pupils.html](https://www.ireland-live.ie/news/derry-now/1961059/bbc-bitesize-adds-ccea-past-papers-to-support-ni-gcse-pupils.html)  
31. Manx Language in schools \- The Department of Education, Sport & Culture, accessed December 7, 2025, [https://desc.gov.im/education/education/manx-language-in-schools/](https://desc.gov.im/education/education/manx-language-in-schools/)  
32. Syllabus, accessed December 7, 2025, [https://archive.gaelg.im/www.gaelg.iofm.net/TCG/syll.html](https://archive.gaelg.im/www.gaelg.iofm.net/TCG/syll.html)  
33. Past Papers \- Manx Language Service \- Sch.im, accessed December 7, 2025, [https://manxlanguage.sch.im/pages/index/view/id/15/Past%20Papers](https://manxlanguage.sch.im/pages/index/view/id/15/Past%20Papers)  
34. Teisht Chadjin Resources \- Manx Language Service, accessed December 7, 2025, [https://manxlanguage.sch.im/pages/index/view/id/19/Teisht%20Chadjin%20Resources](https://manxlanguage.sch.im/pages/index/view/id/19/Teisht%20Chadjin%20Resources)  
35. Fys / Info \- Bunscoill Ghaelgagh, accessed December 7, 2025, [https://bunscoillghaelgagh.sch.im/pages/index/view/id/13/Fys%20-%20Info](https://bunscoillghaelgagh.sch.im/pages/index/view/id/13/Fys%20-%20Info)  
36. Accessibility Plan Bunscoill Ghaelgagh copy, accessed December 7, 2025, [https://bunscoillghaelgagh.sch.im/site/uploads/pages/14/\_media/20240503\_70ba61f1/Accessibility\_Plan\_Bunscoill\_Ghaelgagh\_copy.pdf](https://bunscoillghaelgagh.sch.im/site/uploads/pages/14/_media/20240503_70ba61f1/Accessibility_Plan_Bunscoill_Ghaelgagh_copy.pdf)  
37. Skeeal y Vunscoill / The Bunscoill Story, accessed December 7, 2025, [https://bunscoillghaelgagh.sch.im/pages/index/view/id/2/Skeeal%20y%20Vunscoill%20-%20The%20Bunscoill%20Story](https://bunscoillghaelgagh.sch.im/pages/index/view/id/2/Skeeal%20y%20Vunscoill%20-%20The%20Bunscoill%20Story)  
38. Exam Papers / Irish \- Products | 4schools.ie, accessed December 7, 2025, [https://www.4schools.examcraftgroup.ie/products/product\_category/journals-diaries-9/product\_category/mapscharts-25/profile/36?page=1](https://www.4schools.examcraftgroup.ie/products/product_category/journals-diaries-9/product_category/mapscharts-25/profile/36?page=1)  
39. Products | 4schools.ie, accessed December 7, 2025, [https://4schools.ie/products/product\_category/exam-papers-10/product\_category/history-charts-14/product\_category/journals-diaries-9/school-type/secondary-1](https://4schools.ie/products/product_category/exam-papers-10/product_category/history-charts-14/product_category/journals-diaries-9/school-type/secondary-1)  
40. Exam Papers \- Educate.ie, accessed December 7, 2025, [https://educate.ie/exampapers/](https://educate.ie/exampapers/)  
41. National Qualifications : Higher History \- PlanIT Plus, accessed December 7, 2025, [https://www.planitplus.net/nationals/View/178](https://www.planitplus.net/nationals/View/178)