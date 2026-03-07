# **Technical Feasibility Study: Archival and Organization of Irish Language Audio Corpora (Teanglann.ie & Canuint.ie)**

## **1\. Introduction**

The preservation, analysis, and technological enablement of minority languages relies heavily on the availability of structured, machine-readable corpora. In the context of the Irish language (*Gaeilge*), the digital landscape has evolved significantly over the last decade, transitioning from static textual dictionaries to rich, multi-modal archives that capture the phonological diversity of the language’s major dialects. This report presents a comprehensive technical investigation into the methodology for extracting, organizing, and archiving audio assets from two primary digital repositories: **Teanglann.ie**, specifically its Pronunciation Database (*Bunachar Foghraíochta*), and **Canuint.ie**, the Repository of Irish Dialects (*Taisce Chanúintí na Gaeilge*).  
The objective of this study is to define a robust, scalable, and ethically grounded architecture for harvesting these audio assets. While both platforms serve the preservation of Irish, they represent fundamentally different technical paradigms. Teanglann.ie operates as a lexicographical resource, offering atomic, isolated word pronunciations organized alphabetically and by dialect.1 In contrast, Canuint.ie functions as a geolinguistic archive, delivering continuous speech recordings deeply embedded in metadata regarding speaker identity, location, and temporal origin.2  
This investigation synthesizes evidence from site architecture analysis, open-source software repositories, and archival metadata standards to propose a unified data ingestion pipeline. By reverse-engineering the retrieval mechanisms—ranging from predictable URL pattern matching on Teanglann to dynamic Single-Page Application (SPA) interception on Canuint—we establish a methodology that ensures data integrity and accessibility for downstream applications such as Automatic Speech Recognition (ASR) training and sociolinguistic analysis. Furthermore, this report addresses the critical intersection of technical capability and legal responsibility, analyzing the copyright and data protection frameworks governing these state-funded assets.

### **1.1 The Linguistic and Digital Landscape**

The prioritization of digital tools for the Irish language is evident in the "20-Year Strategy for the Irish Language" 3, which mandates the creation of electronic resources to support the language's vitality. Teanglann.ie serves as the digital interface for the standard dictionaries: *Foclóir Gaeilge-Béarla* (Ó Dónaill, 1977\) and the *English-Irish Dictionary* (de Bhaldraithe, 1959).4 The audio component, the Pronunciation Database, is crucial because Irish orthography, while regular, poses significant challenges for learners due to dialectal variation in phonology.  
The three primary dialects—Ulster (*Cúige Uladh*), Connacht (*Cúige Chonnacht*), and Munster (*Cúige Mumhan*)—exhibit distinct stress patterns and phonemic inventories. Teanglann captures this by providing up to three distinct audio files for a single lexical entry.5 Canuint.ie, developed by the Gaois research group at Dublin City University (DCU) in collaboration with RTÉ Archives, expands this scope by focusing on the sub-dialects and the natural prosody of native speakers from the mid-20th century to the present.6  
From a technical perspective, this duality requires a bifurcated approach to data extraction. Teanglann represents the "Web 2.0" era of predictable resource locators and server-side rendering, amenable to lightweight scripting. Canuint represents the "Web 3.0" or "Modern Web" era, utilizing dynamic front-end frameworks, API-driven content delivery, and potentially obfuscated media streams to manage a more complex, metadata-rich dataset.

## **2\. Technical Architecture Analysis: Teanglann.ie**

The Pronunciation Database at Teanglann.ie is structured as a hierarchical index. The primary challenge in scraping this resource is not the complexity of the delivery mechanism, but the sheer volume of lexical items and the necessity of handling dialectal segmentation accurately.

### **2.1 Navigation Topology and Index Traversal**

The entry point for any systematic extraction is the alphabetical index, accessible at https://www.teanglann.ie/en/fuaim/\_a.1 The site structure implies a static, paginated organization where the underscore prefix (e.g., \_a, \_b) denotes the initial letter of the target words.  
The index is further segmented to manage load and usability. For high-frequency initial letters, the index breaks down into sub-pages based on the first three letters of the word (e.g., **ACH**, **ACU**, **ADE**).1 This segmentation is critical for a scraper's traversal logic. A naive scraper that only visits the root letter page will miss the majority of the lexicon. The harvesting algorithm must recursively follow these sub-index links until it reaches the terminal list items.  
Each terminal item in the index is a hyperlink to a specific word entry page (e.g., https://www.teanglann.ie/en/fuaim/abhainn).7 However, visiting every individual page to extract the audio link generates unnecessary server load and slows the ingestion process significantly. A more efficient approach relies on reverse-engineering the URL conventions for the audio files themselves.

### **2.2 Reverse-Engineering the Audio Asset Structure**

Evidence from developer communities and user discussions 8 indicates that Teanglann.ie uses a highly deterministic URL structure for its audio assets. Unlike dynamic sites that generate temporary tokens, Teanglann hosts files in static directories corresponding to the dialect codes.  
The audio files are hosted on the same domain (https://www.teanglann.ie) and are organized into three primary directories:

1. **CanU:** Representing the Ulster dialect (*Canúint Uladh*).  
2. **CanC:** Representing the Connacht dialect (*Canúint Chonnacht*).  
3. **CanM:** Representing the Munster dialect (*Canúint na Mumhan*).

The filename corresponds directly to the headword as it appears in the dictionary, followed by the .mp3 extension. This discovery 8 allows for a "prediction-based" scraping strategy. Instead of parsing HTML to find the \<audio\> tag, the system can construct the target URL strings programmatically.

#### **2.2.1 Dialect Codes and File Paths**

The mapping of dialect to directory is consistent across the site. The structure is defined in Table 2.1.

| Dialect Region | Directory Code | Example Construction | Note |
| :---- | :---- | :---- | :---- |
| **Ulster** | CanU | /CanU/headword.mp3 | Distinctive northern accent; stress often on first syllable. |
| **Connacht** | CanC | /CanC/headword.mp3 | Often serves as the *de facto* standard for learners. |
| **Munster** | CanM | /CanM/headword.mp3 | Distinctive southern accent; stress varies. |

#### **2.2.2 Character Encoding and Normalization**

A significant technical hurdle in scraping Irish language resources is character encoding. The Irish alphabet utilizes the acute accent (*síneadh fada*) on vowels (á, é, í, ó, ú). Web servers require these characters to be URL-encoded (percent-encoded) to be interpreted correctly in a GET request.  
The system must convert the raw Unicode string into a valid ASCII URL string. For example, the word *buíon* (platoon) 10 contains the character í (Latin Small Letter I with Acute). In UTF-8, this is represented as 0xC3 0xAD. When URL-encoded, this becomes %C3%AD.

* Input: https://www.teanglann.ie/CanU/buíon.mp3  
* Encoded Request: https://www.teanglann.ie/CanU/bu%C3%ADon.mp3

Failure to strictly apply UTF-8 encoding will result in HTTP 404 (Not Found) errors, even if the file exists. Furthermore, capitalization matters. While Windows servers are case-insensitive, Linux servers (which host most web infrastructure) are case-sensitive. The snippets suggest the filenames generally follow the dictionary casing (lowercase for common nouns, capitalized for proper nouns), but robustness requires handling potential inconsistencies.

### **2.3 Analysis of Prior Art and Tools**

The feasibility of scraping Teanglann is supported by the existence of several open-source projects. The repository lookup-irish 11, maintained by Eoghan Murray and forked by others, includes a Python script designed to cross-reference definitions between Teanglann.ie and Focloir.ie. This confirms that the site's structure has remained stable enough for tool builders to rely on it over time.  
Another relevant repository is audioscrape 12, though it targets YouTube and SoundCloud. Its existence highlights the demand for command-line audio extraction interfaces. More specifically, the user kscanne (Kevin Scannell) has a repository explicitly named canuint.13 Although the snippets indicate this repo is written in Perl and was last updated in 2020, it suggests a long-standing effort to programmatically access dialect data. The presence of kscanne/canuint alongside kscanne/gramadoir and kscanne/caighdean 13 points to a mature ecosystem of Irish language natural language processing (NLP) tools that a modern scraper should acknowledge and potentially integrate with.

### **2.4 Retrieval Algorithm for Teanglann**

Based on the architectural analysis, the optimal algorithm for harvesting Teanglann's audio is a hybrid of index crawling and speculative downloading.

1. **Index Harvesting:** The scraper initiates a session at the root index /fuaim/\_a. It parses the DOM to extract all word links. It detects if sub-indices exist (e.g., ABH, ABL) and recursively traverses them. The output of this phase is a comprehensive List\<String\> of all headwords in the database.  
2. **URL Construction:** For each headword in the list, the system generates three candidate URLs corresponding to the CanU, CanC, and CanM directories.  
3. **Existence Check (HEAD Request):** To avoid downloading non-existent files (since not all words have recordings in all dialects), the system sends an HTTP HEAD request. This lightweight request retrieves only the headers.  
4. **Verification:**  
   * 200 OK: The audio exists. The system proceeds to a GET request to download the payload.  
   * 404 Not Found: The audio is missing for this dialect. The event is logged, and no download is attempted.  
5. **Concurrency Control:** Given the likely server constraints of a non-profit entity like Foras na Gaeilge, the scraper must implement rate limiting. A sequential approach is too slow for 50,000+ words, but unbounded concurrency constitutes a Denial of Service (DoS) attack. A semaphore-based approach allowing 5-10 concurrent requests is a standard "polite" scraping pattern.

## **3\. Technical Architecture Analysis: Canuint.ie**

The technical landscape of Canuint.ie differs radically from Teanglann.ie. Launched in 2025 as a collaboration between RTÉ Archives and the Gaois research group 6, Canuint.ie utilizes modern web technologies designed for interactivity and mapping. This introduces complexity regarding asset discovery and retrieval.

### **3.1 The "Gaois" Infrastructure and API Ecosystem**

The Gaois research group maintains a sophisticated suite of digital resources, including Logainm.ie (Placenames), Dúchas.ie (Folklore), and Téarma.ie (Terminology).14 Canuint.ie is integrated into this ecosystem.  
A critical finding from the research material is the existence of the Gaois Open Data platform and APIs.16 Gaois publishes documentation for APIs accessing Logainm and Dúchas.16 While a public API specifically for Canuint is not explicitly detailed in the snippets as "open" for general developers yet, the architecture suggests it heavily relies on internal APIs similar to its sister projects.  
The Logainm API documentation reveals that audio assets are exposed via a JSON object containing a Uri field.17 It is highly probable that Canuint uses a similar backend schema. The QQTRIN reference IDs found in the URL structure (e.g., QQTRIN023385c1) 18 likely serve as the primary keys for querying these internal endpoints.

### **3.2 Dynamic Asset Delivery and the Single-Page Application (SPA)**

Canuint.ie is described as having an "interactive map" and recordings plotted geographically.6 This functionality typically mandates a Single-Page Application (SPA) framework (such as React, Vue.js, or Angular) where content is loaded dynamically via JavaScript rather than being present in the initial HTML document.

#### **3.2.1 The "Transcript Coming Soon" Indicator**

The snippets reveal that many entries display a "Transcript coming soon" message with links to pages containing the Reference ID.18 This indicates that the text and audio are stored as separate entities in the database and linked via the ID. The "coming soon" status suggests the database is being actively populated, and a scraper needs to be robust enough to handle null values for transcripts while still successfully retrieving audio.

#### **3.2.2 Audio Stream Obfuscation**

Modern archives often obfuscate direct audio links to prevent hotlinking or scraping. Techniques include:

* **Blob URLs:** The audio src might appear as blob:https://www.canuint.ie/uuid.... This URL points to a memory buffer in the browser, not a server file.  
* **Signed URLs:** AWS S3 or Azure Blob Storage links that expire after a set time.  
* **Streaming Protocols:** HLS (.m3u8) or DASH (.mpd) which break the file into chunks.

However, given the Gaois group's history with Logainm.ie—where MP3 files are accessible via standard URIs 19—it is reasonable to hypothesize that Canuint also uses standard HTTP delivery for the underlying files, even if the frontend uses a player wrapper. The presence of the kscanne/canuint repository 13, which is a Perl script, suggests that at some point (or for a precursor version of the data), programmatic access via standard HTTP libraries was possible. If the new site retains the backend logic of the Gaois ecosystem, the audio files will likely be accessible via a consistent URL pattern once the ID is known.

### **3.3 The "QQTRIN" Identifier System**

The key to organizing the Canuint data lies in the QQTRIN identifiers. These IDs appear to be the archival references from the RTÉ digitization project.

* Structure: QQTRIN \+ \+ \`c\` \+ (e.g., QQTRIN023385c1).  
* Function: This ID binds the Speaker metadata, the Location data, and the Audio file together.

A scraper targeting Canuint cannot simply iterate through numbers. It must "spider" the geographic hierarchy (Province $\\rightarrow$ County $\\rightarrow$ Locality) to discover valid IDs. The snippet 2 lists the hierarchy explicitly (e.g., Ulster $\\rightarrow$ Cary; Connacht $\\rightarrow$ Carbury). The scraper must traverse these category pages to parse the tables of recordings and extract the valid IDs.

### **3.4 Data Discovery Strategy**

Unlike Teanglann's linear index, Canuint requires a graph traversal strategy.

1. **Entry Point:** The "Recordings by Area" list.2  
2. **Node Traversal:** Visit each County page, then each Sub-area page.  
3. **Extraction:** On the Sub-area page (e.g., https://www.canuint.ie/en/61250), parse the list of recordings to extract:  
   * Speaker Name (e.g., Roibeárd Mac Cormaic)  
   * Reference ID (e.g., QQTRIN023385c1)  
   * Location (e.g., Rathlin Island)  
   * Year (e.g., 1960\)  
4. **Audio Resolution:** Once the ID is obtained, the system attempts to resolve the audio URL. This may require inspecting network traffic (XHR/Fetch) using browser automation tools if a predictable URL pattern is not found.

## **4\. Methodology for Extraction and Organization**

To operationalize the findings above, a comprehensive extraction and organization methodology is required. This section details the logical flow, tool selection, and data schema necessary to build a cohesive corpus.

### **4.1 Tooling and Software Stack**

The divergent architectures of the two sites necessitate a two-pronged tooling approach.

* **For Teanglann.ie (Static/Predictable):**  
  * **Language:** Python (for robust library support) or Go (for high concurrency).  
  * **Libraries:** requests or aiohttp for HTTP interactions; BeautifulSoup4 or lxml for HTML parsing.  
  * **Justification:** The overhead of a full browser is unnecessary. Raw HTTP requests are faster and impose less load on the server.  
* **For Canuint.ie (Dynamic/SPA):**  
  * **Language:** Python.  
  * **Libraries:** Playwright or Selenium for browser automation; selenium-wire or puppeteer for network interception.  
  * **Justification:** JavaScript execution is likely required to render the recording lists and generate the audio player instances. Browser automation allows the scraper to "see" what the user sees and intercept the underlying API calls that fetch the audio files.

### **4.2 Database Schema Design**

A flat file storage system is insufficient for a corpus of this magnitude and complexity. A relational database is required to maintain the links between words, speakers, locations, and audio files. We propose the following SQL-based schema.

#### **4.2.1 Core Entities**

Table: Lexicon (Teanglann Source)  
This table stores the unique lexical items found in the dictionary.

* lexicon\_id (Primary Key, Integer): Unique identifier.  
* headword (String, Indexed): The word in Irish (e.g., "madra").  
* alpha\_index (String): The initial letter (for easy sorting).

Table: Dialect\_Audio (Teanglann Source)  
This table links lexical items to their specific dialect recordings.

* audio\_id (Primary Key, Integer).  
* lexicon\_id (Foreign Key): Links to the Lexicon table.  
* dialect\_code (Enum): 'UL' (Ulster), 'CO' (Connacht), 'MU' (Munster).  
* file\_path (String): Relative path to the stored file.  
* source\_url (String): The original Teanglann URL.  
* checksum (String): MD5 hash for data integrity.

Table: Speakers (Canuint Source)  
This table captures the rich biographical data of the informants.

* speaker\_id (Primary Key, Integer).  
* name (String): e.g., "Roibeárd Mac Cormaic".  
* native\_location (String): The townland or parish of origin.  
* county (String): e.g., "Donegal".  
* province (String): e.g., "Ulster".

Table: Archive\_Recordings (Canuint Source)  
This table stores the metadata for the continuous speech files.

* recording\_id (Primary Key, String): The QQTRIN ID.  
* speaker\_id (Foreign Key): Links to the Speakers table.  
* year\_recorded (Integer): e.g., 1960\.  
* duration\_sec (Integer): Length of recording.  
* has\_transcript (Boolean): Flag for transcript availability.  
* file\_path (String): Local storage path.

### **4.3 Directory Hierarchy and File Naming**

To ensure the dataset is usable even without the database, the physical file organization must be semantic and logical.  
Proposed Directory Structure:  
/Irish\_Audio\_Corpus  
│  
├── /Teanglann\_Lexicon  
│ ├── /Ulster  
│ │ ├── a\_thiarcais.mp3  
│ │ ├── abhainn.mp3  
│ │ └──...  
│ ├── /Connacht  
│ │ └──...  
│ └── /Munster  
│ └──...  
│  
└── /Canuint\_Archive  
├── /Ulster  
│ ├── /Donegal  
│ │ ├── /Kilmacrenan  
│ │ │ └── 1960\_MacCormaic\_QQTRIN023385c1.mp3  
│ │ └──...  
│ └── /Cary  
│ └──...  
└── /Munster  
└── /Kerry  
└──...  
**Naming Convention Rationale:**

* **Teanglann:** \[word\].mp3 within dialect folders is simple and collision-resistant (assuming homonyms are handled via suffixing e.g., word\_1.mp3).  
* **Canuint:** \_\_.mp3 provides context (Time, Person, Unique Ref) immediately upon visual inspection of the file system.

## **5\. Implementation Logic and Algorithms**

This section details the algorithmic logic required to implement the scraper, incorporating the specific constraints identified in the research.

### **5.1 The Teanglann Harvester Algorithm**

The harvesting process for Teanglann is defined by the need to handle the sub-index pages and the rigorous URL encoding.  
Step 1: Index Expansion  
The algorithm starts at teanglann.ie/en/fuaim/\_a. It parses the HTML to identify the "Quick Navigation" or "Sub-letter" links (e.g., ACE, ACH). It maintains a visited set to ensure no loops occur. It iterates through A-Z, collecting every link that matches the pattern /en/fuaim/\[word\].  
Step 2: Headword Normalization  
The extracted link text (the word) often contains spaces or punctuation. The algorithm must:

* Trim whitespace.  
* Replace spaces with underscores (if the URL convention demands it, though Teanglann often uses %20).  
* Ideally, use the href attribute (the slug) rather than the link text, as the slug is already URL-safe.

Step 3: Speculative Fetching Loop  
For every slug in the list:

* Construct three URLs:  
  1. .../CanU/{slug}.mp3  
  2. .../CanC/{slug}.mp3  
  3. .../CanM/{slug}.mp3  
* Execute an HTTP HEAD request for each.  
* If Status \== 200: Add to download queue.  
* If Status \== 404: Ignore.  
* If Status \== 403/429: Pause execution (backoff strategy) and retry.

Step 4: ID3 Tagging  
Upon download, the system should immediately write metadata to the MP3 file's ID3 tags.

* Title: The Headword.  
* Artist: "Teanglann Native Speaker (Ulster/Connacht/Munster)".  
* Album: "Teanglann Pronunciation Database".  
  This ensures the files are self-documenting if removed from the folder structure.

### **5.2 The Canuint Archivist Algorithm**

The Canuint extraction is more complex due to the potential need to "play" audio to find the source URL.  
Step 1: Hierarchy Spidering  
The scraper visits the main "Recordings by Area" page. It extracts links to Provinces. It visits each Province to extract links to Counties/Regions. It visits each Region to find the tables of recordings.  
Step 2: Metadata Extraction  
On the Recording List page (e.g., for "Cary" 18), the scraper iterates over the table rows. It extracts the visible text: Speaker, Location, Date. Crucially, it extracts the QQTRIN ID, which is often hidden in the href of the "More Info" or "Transcript" buttons.  
Step 3: Dynamic Audio Resolution (The "Interceptor")  
This step assumes direct URL prediction is impossible.

1. Initialize a browser instance (e.g., Playwright).  
2. Enable request interception (Network domain).  
3. Navigate to the recording page.  
4. Programmatically click the "Play" button associated with the QQTRIN ID.  
5. Monitor network traffic for media types (audio/mpeg, audio/wav, application/octet-stream).  
6. Capture the Request URL of the media file.  
7. Pass this URL to a standard download manager.

Step 4: Association  
The downloaded file must be renamed immediately using the metadata extracted in Step 2 (Speaker, Year, ID) to avoid having a directory full of opaque filenames like audio\_123.mp3.

## **6\. Legal, Ethical, and Operational Constraints**

The technical ability to extract this data is bounded by legal and ethical frameworks that must be respected to ensure the longevity of the project and the integrity of the data.

### **6.1 Copyright and Intellectual Property**

**Teanglann.ie:** The research indicates that the site content is © Foras na Gaeilge 4, with sound files supplied by Macalla Teo. The dictionary content is based on copyrighted printed works. While the site is free for public consultation, bulk harvesting creates a "derivative dataset." This falls into a legal grey area. Personal, non-commercial use (e.g., training a local AI model for personal research) is generally defensible under fair use/fair dealing doctrines. However, republishing the raw audio files as a public dataset (e.g., on Hugging Face) would likely constitute copyright infringement.  
**Canuint.ie:** This collection is part of the RTÉ Archives.2 RTÉ maintains strict control over its archival assets. The footage and audio are often licensed only for specific educational uses. The terms of use for Canuint (likely governed by the broader Gaois/RTÉ policies) almost certainly prohibit redistribution. The scraper operator must treat this data as "read-only" for analysis, not for republication.

### **6.2 Data Protection and Sovereignty (GDPR)**

Canuint.ie contains recordings of named individuals.18 While many recordings are historical (1960s), some may involve living individuals or their direct descendants. Under GDPR, voice data is biometric data. Even if the individuals are deceased (where GDPR applies less strictly), ethical considerations regarding "Data Sovereignty" apply. The Gaois group actively engages with ethical best practices 20, emphasizing community involvement. A scraper that extracts this data ignores the contextual presentation intended by the archivists. Any use of this data should respect the dignity of the speakers and the context of the folklore.

### **6.3 Operational "Politeness"**

Scraping puts a load on servers. Gaois and Foras na Gaeilge are publicly funded cultural bodies, not tech giants with infinite bandwidth.

* **Rate Limiting:** The scraper must enforce a strict delay (e.g., 2 seconds) between requests to prevent server strain.  
* **User-Agent String:** The scraper should identify itself honestly (e.g., User-Agent: ResearchProject/1.0 (contact@university.edu)), allowing system administrators to contact the operator if the traffic is disruptive.  
* **Robots.txt:** Although snippets suggest the file was inaccessible during the preliminary scan 21, a production scraper must check and respect robots.txt exclusions.

## **7\. Future Applications and Implications**

The successful archival and organization of these corpora unlock significant potential for the revitalization of the Irish language through technology.

### **7.1 Automatic Speech Recognition (ASR)**

The combination of Teanglann and Canuint provides the "Holy Grail" for ASR training:

* **Teanglann** provides the "Gold Standard" alignment: Text $\\leftrightarrow$ Audio (Single Word). This helps the model learn phonemes and precise articulation.  
* Canuint provides the "Real World" context: Continuous speech, co-articulation, background noise, and dialectal variance.  
  Training a model like OpenAI's Whisper on this unified corpus would significantly improve transcription accuracy for Irish, particularly for regional dialects that are currently underserved by commercial models.

### **7.2 Text-to-Speech (TTS) Synthesis**

The clean, studio-quality recordings from Teanglann are ideal for training neural TTS voices. By segmenting the data by dialect (CanU, CanC, CanM), developers can create dialect-aware TTS systems. This is pedagogically vital, as learners often struggle when a TTS voice does not match the dialect they are studying.

### **7.3 Sociolinguistic Mapping**

The metadata-rich Canuint dataset allows for computational sociolinguistics. By analyzing the geolocation of speakers against the phonological features of their recordings, researchers can map the "isoglosses" (linguistic boundaries) of Irish dialects with unprecedented precision. This could visualize the recession or expansion of specific dialectal traits over the last century.

## **8\. Conclusion**

The investigation confirms that a comprehensive archival project for Irish language audio is technically feasible but requires a sophisticated, hybrid approach. Teanglann.ie offers a structured, predictable resource that can be harvested with standard web scraping techniques, yielding a massive lexicon of dialect-tagged pronunciations. Canuint.ie presents a more complex challenge, requiring browser automation and network interception to unlock its rich archive of continuous speech.  
The proposed architecture—a unified SQL schema linking lexical items, speakers, and audio assets—provides a solid foundation for this work. However, the technical execution must be tempered by a rigorous adherence to ethical and legal standards. The goal of such an archive must be the preservation and promotion of the language, respecting the rights of the creators (Foras na Gaeilge, RTÉ, Gaois) and the speakers who have preserved these dialects for generations. By bridging the gap between traditional archiving and modern data science, this project can ensure that the voices of the Gaeltacht remain audible and relevant in the digital age.

| Resource | Technical Paradigm | Audio Structure | Primary Challenge | Value Proposition |
| :---- | :---- | :---- | :---- | :---- |
| **Teanglann.ie** | Static / Web 2.0 | Predictable URL Paths | Volume / Encoding | Atomic Pronunciation Data |
| **Canuint.ie** | Dynamic / SPA | ID-Referenced / API | Discovery / Interactivity | Contextual / Prosodic Data |

This report outlines the complete roadmap for this endeavor, enabling technical teams to proceed with the development of the necessary ingestion pipelines while remaining cognizant of the linguistic and cultural weight of the data they handle.

#### **Works cited**

1. Irish Pronunciation Database: A \- Teanglann.ie, accessed December 6, 2025, [https://www.teanglann.ie/en/fuaim/\_a](https://www.teanglann.ie/en/fuaim/_a)  
2. Repository of Irish Dialects, accessed December 6, 2025, [https://www.canuint.ie/en/](https://www.canuint.ie/en/)  
3. Developing high-end reusable tools and resources for Irish-language terminology, lexicography, onomastics (toponymy), folklorist \- ACL Anthology, accessed December 6, 2025, [https://aclanthology.org/W14-4610.pdf](https://aclanthology.org/W14-4610.pdf)  
4. About this website \- Teanglann.ie, accessed December 6, 2025, [https://www.teanglann.ie/en/\_about](https://www.teanglann.ie/en/_about)  
5. Irish Pronunciation Database \- Teanglann.ie, accessed December 6, 2025, [https://www.teanglann.ie/en/fuaim/](https://www.teanglann.ie/en/fuaim/)  
6. Project information, accessed December 6, 2025, [https://www.canuint.ie/en/info/about-this-website/project-information/](https://www.canuint.ie/en/info/about-this-website/project-information/)  
7. Irish Pronunciation Database: abhainn \- Teanglann.ie, accessed December 6, 2025, [https://www.teanglann.ie/en/fuaim/abhainn](https://www.teanglann.ie/en/fuaim/abhainn)  
8. Irish text-to-speech : r/languagelearning \- Reddit, accessed December 6, 2025, [https://www.reddit.com/r/languagelearning/comments/ep2ukb/irish\_texttospeech/](https://www.reddit.com/r/languagelearning/comments/ep2ukb/irish_texttospeech/)  
9. Irish pronunciation site? : r/gaeilge \- Reddit, accessed December 6, 2025, [https://www.reddit.com/r/gaeilge/comments/2jm4dq/irish\_pronunciation\_site/](https://www.reddit.com/r/gaeilge/comments/2jm4dq/irish_pronunciation_site/)  
10. Irish to English Drill commands \- Easy to understand table : r/Irishdefenceforces \- Reddit, accessed December 6, 2025, [https://www.reddit.com/r/Irishdefenceforces/comments/1mcohed/irish\_to\_english\_drill\_commands\_easy\_to/](https://www.reddit.com/r/Irishdefenceforces/comments/1mcohed/irish_to_english_drill_commands_easy_to/)  
11. eoghanmurray/lookup-irish: An update script for an Anki deck \- GitHub, accessed December 6, 2025, [https://github.com/eoghanmurray/lookup-irish](https://github.com/eoghanmurray/lookup-irish)  
12. carlthome/audioscrape: Scrape audio from YouTube and SoundCloud with a simple command-line interface. \- GitHub, accessed December 6, 2025, [https://github.com/carlthome/audioscrape](https://github.com/carlthome/audioscrape)  
13. gaeilge · GitHub Topics, accessed December 6, 2025, [https://github.com/topics/gaeilge](https://github.com/topics/gaeilge)  
14. About Gaois | Gaois research group, accessed December 6, 2025, [https://www.gaois.ie/en/about/info](https://www.gaois.ie/en/about/info)  
15. Gaois research group | Fiontar & Scoil na Gaeilge, DCU, accessed December 6, 2025, [https://www.gaois.ie/en](https://www.gaois.ie/en)  
16. Getting started with Gaois open data resources | docs.gaois.ie, accessed December 6, 2025, [https://docs.gaois.ie/en/data/getting-started](https://docs.gaois.ie/en/data/getting-started)  
17. Logainm / Data dictionary (Version 1.0) | docs.gaois.ie, accessed December 6, 2025, [https://docs.gaois.ie/en/data/logainm/v1.0/data](https://docs.gaois.ie/en/data/logainm/v1.0/data)  
18. Repository of Irish Dialects: Cary, accessed December 6, 2025, [https://www.canuint.ie/en/61250](https://www.canuint.ie/en/61250)  
19. Irish Language Featured in the Legacy Data Preservation Pilot \- Digital Repository of Ireland (DRI), accessed December 6, 2025, [https://dri.ie/news/irish-language-featured-in-the-legacy-data-preservation-pilot/](https://dri.ie/news/irish-language-featured-in-the-legacy-data-preservation-pilot/)  
20. Naidheachdan – News – Gaelic Algorithmic Research Group \- Blogs \- The University of Edinburgh, accessed December 6, 2025, [https://blogs.ed.ac.uk/garg/category/content/naidheachdan-news/](https://blogs.ed.ac.uk/garg/category/content/naidheachdan-news/)  
21. accessed January 1, 1970, [https://www.teanglann.ie/robots.txt](https://www.teanglann.ie/robots.txt)