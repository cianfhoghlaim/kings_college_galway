# **Unified Computational Infrastructure for Celtic Languages: Data Integration, Educational Analytics, and Strategic Modelling**

## **Executive Summary**

The preservation, revitalization, and educational proliferation of the autochthonous languages of Britain—Welsh, Scottish Gaelic, Cornish, Manx, and the Germanic language Scots—constitutes a formidable challenge that spans sociolinguistics, computer science, and public policy. While the Republic of Ireland has successfully consolidated a robust, state-funded digital infrastructure for the Irish language, the digital estate for the Celtic languages of Great Britain remains characterized by fragmentation, heterogeneous data standards, and a stark dichotomy between high-resource languages like Welsh and low-resource languages like Cornish and Manx. The effective mobilization of these resources requires not merely the aggregation of files, but the architecting of a sophisticated data ecosystem capable of transforming static archival texts into dynamic educational intelligence.  
This report presents a comprehensive, deep-research analysis of the non-Ireland digital sources identified within the CLARIN "Digital Resources for the Languages in Ireland and Britain" (DR-LIB) framework and associated repositories. It proposes a unified technical architecture—a Federated Linguistic Data Lakehouse—designed to ingest, harmonize, and serve data from these disparate sources. Furthermore, it details the specific feature engineering strategies and SQL relational models required to extract actionable insights into language acquisition, curriculum efficacy, and sociolinguistic variation. By bridging the gap between computational linguistics and learning analytics, this architecture aims to support the *Curriculum for Wales*, Scotland’s *Curriculum for Excellence*, and the broader revivalist movements in Cornwall and the Isle of Man.

## **Part I: The Digital Estate of the Languages of Britain**

To architect a unified system, one must first perform a forensic audit of the existing digital landscape. The resources available for the languages of Britain vary wildly in terms of scale, granularity, and accessibility. Understanding the specific technical attributes of these "non-Ireland" sources is the prerequisite for any integration effort.

### **1.1 Welsh: The Benchmark of Celtic Language Technology**

Among the Celtic languages, Welsh (Cymraeg) stands as the undisputed leader in digital infrastructure. Its resources are not only voluminous but also technically sophisticated, adhering to modern standards of corpus linguistics and software engineering. This maturity makes Welsh the primary "donor" language in any cross-lingual transfer learning framework proposed later in this report.

#### **1.1.1 The National Corpus of Contemporary Welsh (CorCenCC)**

The *Corpws Cenedlaethol Cymraeg Cyfoes* (CorCenCC) represents a pivotal development in the field. Unlike many historical corpora which consist of digitized literature, CorCenCC is a community-driven, multimodal dataset containing approximately 14.4 million tokens of contemporary Welsh.1 The corpus is significant not just for its size but for its representativeness, covering spoken, written, and electronic (e-language) modes of communication.  
The data structure of CorCenCC offers a blueprint for the other languages. It employs a rich metadata taxonomy that categorizes texts by genre, audience, and author demographics. Crucially, the corpus is fully annotated using the **CyTag** part-of-speech (POS) tagger and the **CySemTag** semantic tagger.1 This layer of annotation transforms raw text into structured data, enabling researchers to query not just for words, but for grammatical categories (e.g., "all plural nouns in the genitive context") and semantic fields (e.g., "words related to agriculture"). The corpus utilizes a refined tagset that accounts for the specific morphological features of Welsh, such as initial consonant mutations, which are often stumbling blocks for generic NLP tools.  
A distinct feature of CorCenCC is its explicit pedagogical orientation. The project developers recognized that a corpus, while valuable for linguists, is often inaccessible to teachers and learners. To bridge this gap, they developed **Y Tiwtiadur**, a digital toolkit that interfaces directly with the corpus database.3 Y Tiwtiadur allows educators to generate cloze tests (gap-fill exercises), vocabulary profiles, and word identification tasks automatically. This functionality is driven by the frequency data inherent in the corpus: a teacher can request a gap-fill exercise using only the top 1,000 most frequent words, ensuring the material is appropriate for a specific proficiency level. This direct pipeline from corpus backend to classroom frontend is a model that the proposed unified architecture must replicate for Gaelic, Scots, and Cornish.  
From a data governance perspective, CorCenCC sets a high standard for ethics and privacy. The dataset includes rigorous anonymization protocols. Personal names are replaced with tags like \<anon\>enwb1\</anon\> (female name) or \<anon\>enwg1\</anon\> (male name), and sensitive data points like phone numbers and email addresses are similarly redacted.4 In a unified database aggregating data from multiple jurisdictions, adhering to such GDPR-compliant anonymization standards is non-negotiable.

#### **1.1.2 The Welsh National Corpora Portal and Canolfan Bedwyr**

Complementing CorCenCC is the ecosystem developed by Bangor University’s Language Technologies Unit (Canolfan Bedwyr). The **Welsh National Corpora Portal** acts as an aggregator for various specialized and historical corpora, providing a single search interface for diverse datasets.5 This portal demonstrates the viability of the "federated" search approach, where a central index queries multiple underlying databases.  
The technical contributions of Canolfan Bedwyr extend beyond corpora to essential processing tools. **Cysill** (grammar and spell checker) and **Cysgeir** (electronic dictionaries) provide the normative data—the "ground truth"—necessary for error analysis.6 If we are to build models that detect learner errors, we need a reference for what constitutes "correct" Welsh. Cysill’s algorithms, which handle the complex mutation rules (soft, nasal, aspirate), provide the logic required for such validation. Furthermore, the **Paldaruo Speech Corpus** provides the audio-text alignment data necessary for training Automatic Speech Recognition (ASR) systems.6 In an educational context, ASR is vital for automated pronunciation scoring, allowing a system to listen to a learner and provide feedback on their realization of specific phonemes like the voiceless alveolar lateral fricative (ll).

### **1.2 Scottish Gaelic: Academic Rigor and Distributed Archives**

The situation for Scottish Gaelic (Gàidhlig) is characterized by deep academic involvement, particularly from the Universities of Glasgow and Edinburgh, but arguably less integration into consumer-facing technology compared to Welsh.

#### **1.2.1 The Digital Archive of Scottish Gaelic (DASG)**

The primary repository for Gaelic textual data is the **Digital Archive of Scottish Gaelic (DASG)**, managed by the University of Glasgow.7 DASG is a bipartite resource consisting of *Corpas na Gàidhlig*, a comprehensive text corpus, and the Fieldwork Archive, a collection of vernacular recordings and questionnaires from the mid-20th century.8  
DASG’s technical architecture relies on **CQPweb**, a web-based frontend for the IMS Open Corpus Workbench (CWB).9 This choice of infrastructure is significant. CWB utilizes a specialized binary indexing format for verticalized text (one token per line), which allows for extremely fast querying of massive datasets.10 The data model includes positional attributes (word, lemma, POS) and structural attributes (text boundaries, sentence markers). While CQPweb is powerful for linguistic research—allowing complex queries like "find all instances of the verb *bi* followed by a preposition"—its interface is daunting for non-specialists. The data is effectively locked away from the average school teacher or learner, highlighting the need for an API layer that can expose this richness in a more user-friendly format.

#### **1.2.2 The Annotated Reference Corpus of Scottish Gaelic (ARCOSG)**

For computational modelling, the **Annotated Reference Corpus of Scottish Gaelic (ARCOSG)** is the gold standard. Unlike raw text collections, ARCOSG has been meticulously hand-tagged and verified.11 It utilizes a fine-grained POS tagset derived from the Irish PAROLE system, containing 246 distinct tags.12 This level of granularity captures the nuances of Gaelic morphology, such as the inflected prepositions and the various forms of the verbal noun.  
A critical development for ARCOSG is the mapping of its tagset to the **Universal Dependencies (UD)** standard.13 Universal Dependencies is a framework for consistent grammatical annotation across different human languages. By converting Gaelic data to UD, researchers at the University of Edinburgh have made it possible to train multilingual AI models. A parser trained on a large dataset of Irish or Manx can be fine-tuned on the smaller Gaelic dataset, leveraging the syntactic similarities between the Goidelic languages. This cross-lingual compatibility is a cornerstone of the proposed unified architecture.

#### **1.2.3 Educational Silos: LearnGaelic and SpeakGaelic**

On the learner-facing side, platforms like **LearnGaelic** and **SpeakGaelic** provide high-quality media content, dictionaries (*Am Faclair Beag*), and structured courses aligned with the Common European Framework of Reference for Languages (CEFR) levels A1 through B2.15 *Am Faclair Beag* is particularly notable for integrating Dwelly’s historical dictionary with modern terminology, creating a bridge between the literary past and the functional present.17  
However, from a data integration perspective, these platforms operate as "walled gardens." The rich interaction data—which vocabulary items users look up most frequently, which grammar exercises they fail—is not publicly accessible via APIs. The content itself is often presented as static HTML or embedded media, requiring scraping and parsing to be useful for a unified data lake. Integrating these resources requires negotiating data-sharing agreements or building robust harvesters to index their content for a centralized search engine.

### **1.3 Scots: The Germanic Cousin and the Challenge of Orthography**

While not a Celtic language, Scots is indigenous to Scotland and falls under the purview of CLARIN’s British Isles network. Its inclusion adds a layer of complexity due to its close genetic relationship with English and the lack of a single standardized orthography.

#### **1.3.1 The SCOTS Corpus and Syntax Atlas**

The **Scottish Corpus of Texts & Speech (SCOTS)** offers a substantial dataset of 4.6 million words, covering the period from 1945 to the present.18 A key feature of SCOTS is its extensive sociolinguistic metadata. Texts are tagged not just by genre but by the author's region, age, gender, and occupation.19 This metadata is invaluable for educational modeling, as it allows for the differentiation between "Standard Scottish English," "Urban Scots" (e.g., Glaswegian), and "Insular Scots" (e.g., Shetlandic).  
Complementing this is the **Scots Syntax Atlas (SCOSYA)**, which maps grammatical variation across 140 locations in Scotland.20 SCOSYA data is qualitative and judgement-based, recording what speakers *accept* as valid Scots in their dialect. Integrating this into an educational database prevents the imposition of a false "standard" on learners, allowing for a curriculum that respects dialectal diversity—a key tenet of modern sociolinguistic pedagogy.

#### **1.3.2 The Dictionary of the Scots Language (DSL)**

The **Dictionary of the Scots Language (DSL)** is a monumental digital resource combining the *Scottish National Dictionary* (modern Scots) and the *Dictionary of the Older Scottish Tongue*.21 The DSL data is structured in TEI-XML, a rich format that encodes etymology, sense hierarchies, and quotations. The challenge here is "orthographic synonymy." Because Scots spelling varies widely, a unified database must map multiple surface forms (e.g., *hoose*, *huse*, *hous*) to a single lemma ID to allow for accurate frequency analysis and retrieval.

### **1.4 Manx and Cornish: Revitalization and the Long Tail**

Manx (Gaelg) and Cornish (Kernewek) represent the "long tail" of the linguistic spectrum. With much smaller speaker populations, their digital footprints are correspondingly lighter, yet the strategic importance of technology for their revitalization is arguably higher.

#### **1.4.1 Manx: The Inter-Gaelic Bridge**

Manx resources include the **Manx Corpus** and **Gaelg Corpus Search**.22 Despite the small size of the corpus, a **Universal Dependencies treebank** for Manx has been developed.23 This is significant because Manx, linguistically, sits between Irish and Scottish Gaelic. Its orthography, however, is based on English phonology (e.g., using 'v' instead of 'mh' or 'bh'), which obscures its etymological connections. The unified architecture must include a normalization layer that maps Manx orthography to standard Gaelic forms to facilitate the cross-lingual transfer of educational resources and linguistic models.

#### **1.4.2 Cornish: Standardization and Scarcity**

Cornish faces the unique challenge of competing orthographies (Kernewek Kemmyn, Standard Written Form, Unified Cornish, etc.).24 **Akademi Kernewek** oversees the *Korpus Kernewek* and the *Gerlyver Kernewek* dictionary.25 The corpus is heavily weighted towards official translations from Cornwall Council, which may lack the colloquial vibrancy needed for engaging educational materials.  
For Cornish, the "uselessness" narrative identified in sociolinguistic research 26 poses a barrier to uptake. Data-driven insights that demonstrate the vitality and utility of the language are needed to counter this. The integration of Cornish data into a broader "Celtic" infrastructure can help validate the language, providing learners with a sense of connection to a larger cultural sphere. The database schema for Cornish must explicitly handle "orthographic polymorphism," linking a single concept to its various realizations across the different revivalist spelling systems.

## **Part II: Architectural Convergence \- The Federated Data Lakehouse**

To answer the user's request to "gather all this data in one place," simple file aggregation is insufficient. The disparate nature of the data—XML dictionaries, verticalized corpora, JSON APIs, and raw HTML—demands a **Federated Linguistic Data Lakehouse** architecture. This hybrid approach combines the storage flexibility of a Data Lake (for raw files) with the structured querying capabilities of a Data Warehouse.

### **2.1 The Ingestion Strategy: A Robust ETL Pipeline**

The foundation of the system is an Extract-Transform-Load (ETL) pipeline designed to normalize linguistic heterogeneity.27

#### **2.1.1 Extraction (The 'E')**

The extraction layer must employ multiple strategies to harvest data from the identified sources:

* **OAI-PMH Harvesting:** For academic repositories like the Oxford Text Archive and DASG, the Open Archives Initiative Protocol for Metadata Harvesting (OAI-PMH) allows for the automated ingestion of metadata records.29  
* **Custom Scrapers:** Python-based scrapers (using libraries like Scrapy or BeautifulSoup) will be deployed to harvest vocabulary lists and curriculum tables from "walled garden" sites like *LearnGaelic* and *SpeakGaelic*. These scripts must be robust to changes in the source HTML structure.  
* **API Connectors:** Direct API integrations will be built for resources that expose them, such as the *Dictionary of the Scots Language* or the *Welsh National Corpora Portal*.5  
* **Vertical Text Parsers:** A specialized parser is required for the .vrt (verticalized text) files used by CQPweb (CorCenCC, DASG). This parser reads the column-based format (Token Lemma POS) and converts it into a row-oriented database format.31

#### **2.1.2 Transformation (The 'T')**

This is the most intellectually demanding phase, requiring deep linguistic knowledge to harmonize the data.

* **Tagset Mapping to Universal Dependencies:** The disparate POS tagging schemes (CyTag for Welsh, PAROLE for Gaelic, CLAWS for Scots) must be mapped to the **Universal Dependencies (UD) v2** standard.14  
  * *Mechanism:* A mapping table converts the specific tag (e.g., ARCOSG Ncsmn) to the UD equivalent (NOUN with features Gender=Masc|Number=Sing|Case=Nom).  
  * *Benefit:* This creates a *lingua franca* within the database. An educational researcher can query for "Adjectives preceding Nouns" and retrieve examples from all languages, regardless of the original annotation scheme.  
* **Orthographic Normalization and Lemmatization:** For Scots and Cornish, a normalization step is critical. Algorithms (such as Levenshtein distance matching or phonetic hashing) will map variant spellings to a canonical lemma ID. For Celtic languages, a "Demutation" module is required to strip initial mutations (lenition, eclipsis) and identify the radical form of the word for dictionary lookups.24  
* **TEI Parsing:** XML files from the DSL or historical corpora must be parsed to extract the semantic hierarchy (entries, senses, citations) and flatten it into relational tables.32

#### **2.1.3 Loading (The 'L')**

The transformed data is loaded into a polyglot persistence layer:

* **PostgreSQL:** Serves as the primary data warehouse for structured data (tokens, metadata, user profiles). Its support for JSONB allows for semi-structured data (like morphological features) to be queried efficiently alongside relational columns.33  
* **Neo4j (Graph Database):** Stores the lexical network. This is the ideal store for the OntoLex-Lemon model, representing words as nodes and relationships (synonymy, translation, etymology) as edges.34  
* **Elasticsearch:** Provides the search engine index. It enables fuzzy searching (essential for learners who may misspell words) and full-text retrieval across the millions of documents in the corpus.

### **2.2 Standardization via Linguistic Linked Open Data (LLOD)**

To ensure the system is not just a silo but a node in the global linguistic web, it must adhere to **Linguistic Linked Open Data (LLOD)** principles.35

* **OntoLex-Lemon:** This W3C standard is the target schema for all lexical resources. The LexicalEntry class represents the headword, while LexicalSense captures the meaning.37 By mapping *Am Faclair Beag* (Gaelic) and *Geiriadur Prifysgol Cymru* (Welsh) to OntoLex, we can link concepts via the varnet:cognate property, explicitly modeling the relationship between Welsh *môr* and Gaelic *muir* (sea).  
* **Persistent URIs:** Every lemma, corpus document, and curriculum standard is assigned a persistent Uniform Resource Identifier (URI). This allows external tools to link deeply into the database, enabling a decentralized ecosystem of educational apps.

## **Part III: SQL Models for Educational Intelligence**

The core request is to build features and SQL models "to get insight into... education." This requires shifting from a purely linguistic schema to an **educational data model** that links language usage to learner performance and curriculum standards.

### **3.1 The Core Linguistic Schema**

The foundation is a rigorous representation of the corpora. We use a star schema approach optimized for analytics.  
Table: dim\_Corpus  
Stores metadata about the source datasets.

| Column | Type | Description |
| :---- | :---- | :---- |
| corpus\_id | INT (PK) | Unique identifier |
| name | VARCHAR | e.g., "CorCenCC", "ARCOSG" |
| language\_code | CHAR(3) | ISO 639-3 (gle, cym, glv, cor) |
| modality | ENUM | 'Spoken', 'Written', 'Electronic' |
| license | VARCHAR | 38 |

Table: dim\_Document  
Stores document-level metadata, crucial for filtering educational materials.

| Column | Type | Description |
| :---- | :---- | :---- |
| doc\_id | BIGINT (PK) |  |
| corpus\_id | INT (FK) |  |
| genre | VARCHAR | e.g., 'Fiction', 'News', 'Learner Essay' |
| cefr\_level | VARCHAR | Estimated difficulty (A1-C2) |
| dialect\_region | VARCHAR | e.g., 'Gwynedd', 'Lewis', 'Doric' |
| publication\_date | DATE |  |

Table: fact\_Token  
The atomic unit of the database. This table will contain hundreds of millions of rows and must be heavily indexed.

| Column | Type | Description |
| :---- | :---- | :---- |
| token\_id | BIGINT (PK) |  |
| doc\_id | BIGINT (FK) |  |
| sentence\_index | INT |  |
| position | INT | Index within sentence |
| surface\_form | VARCHAR | The word as written (e.g., 'bhean') |
| lemma | VARCHAR | Dictionary form (e.g., 'bean') |
| pos\_ud | VARCHAR | Universal Dependency tag (e.g., 'NOUN') |
| pos\_native | VARCHAR | Original tag (e.g., 'Ncsfn') |
| mutation | VARCHAR | 'Lenition', 'Nasal', 'Radical' |
| morph\_feats | JSONB | e.g., {"Case": "Gen", "Number": "Sing"} |

### **3.2 The Learner and Curriculum Schema**

To generate educational insights, we must model the *learner* and the *curriculum*. This schema is inspired by Moodle's competency frameworks 39 and learner corpus architectures.  
Table: dim\_CurriculumGoal  
Maps official curriculum standards (Curriculum for Wales, CfE) to linguistic targets.

| Column | Type | Description |
| :---- | :---- | :---- |
| goal\_id | INT (PK) |  |
| framework | VARCHAR | 'Curriculum for Excellence', 'Teisht Vunneydagh' |
| level | VARCHAR | 'First Level', 'A2', 'Progression Step 3' |
| descriptor | TEXT | e.g., "Can use conditional tense" |
| linguistic\_target | VARCHAR | Key to pos\_ud or morph\_feats (e.g., 'Mood=Cnd') |

Table: fact\_LearnerPerformance  
Derived from learner corpora or LMS integration. Tracks specific errors and successes.

| Column | Type | Description |
| :---- | :---- | :---- |
| interaction\_id | BIGINT (PK) |  |
| learner\_id | UUID | Anonymized hash 41 |
| l1\_language | CHAR(3) | Learner's native language |
| token\_id | BIGINT (FK) | Link to the specific token produced |
| is\_error | BOOLEAN |  |
| error\_code | VARCHAR | e.g., 'MUT\_MISSING', 'GEN\_AGR\_FAIL' |
| correction | VARCHAR | Target form |
| context\_id | INT | Link to dim\_Document (the task/prompt) |

Table: dim\_Competency  
Based on Moodle's competency structure 42, linking specific skills to curriculum goals.

| Column | Type | Description |
| :---- | :---- | :---- |
| competency\_id | INT (PK) |  |
| shortname | VARCHAR | e.g., 'Gaelic\_Lenition\_Past\_Tense' |
| parent\_id | INT | Hierarchical link |
| path | VARCHAR | Materialized path for fast querying |

### **3.3 Graph Schema for Semantic Relations**

In the Neo4j graph database, we model the *relationships* that SQL struggles with.

* **Nodes:** LexicalEntry (Words), Concept (Meanings).  
* **Edges:**  
  * (:LexicalEntry)--\>(:Concept)  
  * (:LexicalEntry)--\>(:LexicalEntry)  
  * (:LexicalEntry {lang:'cym'})--\>(:LexicalEntry {lang:'gle'})

This graph structure allows for queries like "Find all agricultural terms in Welsh that have cognates in Breton but not in Gaelic," enabling deep comparative philology and the creation of pan-Celtic educational resources.

## **Part IV: Feature Engineering & Analytics for Insight**

The data architecture is merely the enabler. The true value lies in the features we can engineer from this data to answer the user's question about "insight into education."

### **4.1 Automated Text Leveling and Readability Scoring**

One of the greatest challenges for minority language education is the lack of graded reading materials. Teachers often struggle to find texts appropriate for an A2 or B1 learner.

* **Feature:** Lexical\_Frequency\_Profile  
  * **Mechanism:** Using the fact\_Token table, we calculate the percentage of words in a given text that fall into the top 1,000, 2,000, and 5,000 most frequent words in the reference corpus (CorCenCC or ARCOSG).  
  * **SQL Logic:** SELECT count(\*) FROM fact\_Token WHERE lemma IN (SELECT lemma FROM freq\_list WHERE rank \<= 1000).  
* **Feature:** Mutation\_Density\_Index  
  * **Context:** Celtic languages modify the beginnings of words (mutations) to encode grammatical information. A text with a high density of mutations is significantly harder for learners to parse than one with mostly radical forms.  
  * **Mechanism:** Calculate the ratio of mutated tokens to radical tokens per 100 words. A high score indicates a text with complex syntax (e.g., numerous prepositional phrases or possessives).  
* **Insight:** By combining these features, the system can automatically assign a "CEFR Readiness Score" to any text (e.g., a BBC Alba article), flagging it as "Suitable for B2 Learners."

### **4.2 Error Analysis and Curriculum Gap Detection**

The fact\_LearnerPerformance table allows us to move from anecdotal evidence to data-driven curriculum design.

* **Feature:** Error\_Hotspot\_Identification  
  * **Context:** Do learners struggle more with the Genitive Case or the Conditional Mood?  
  * **SQL Query:**  
    SQL  
    SELECT error\_code, count(\*)  
    FROM fact\_LearnerPerformance  
    WHERE l1\_language \= 'eng' AND proficiency\_level \= 'A2'  
    GROUP BY error\_code  
    ORDER BY count(\*) DESC;

  * **Insight:** If the data reveals a spike in MUT\_NASAL\_MISSING errors at level A2, curriculum designers can infer that the current teaching materials for Nasal Mutation are insufficient or introduced too early/late.  
* **Feature:** L1\_Interference\_Map  
  * **Context:** English speakers make different errors in Gaelic than Polish speakers.  
  * **Mechanism:** Correlate error\_code with l1\_language. For instance, English speakers might consistently fail VSO (Verb-Subject-Object) word order tests, while Polish speakers (familiar with case systems) might master the Genitive case faster but struggle with the specific phonology of preaspiration.

### **4.3 Sociolinguistic Analytics: The Dialect Dimension**

In the context of the *Curriculum for Wales* ("Cynefin") and Scottish initiatives, validating local dialect is politically and educationally vital.43

* **Feature:** Dialect\_Representation\_Score  
  * **Mechanism:** Tag vocabulary items in the lexicon with region codes (e.g., word:buntàta \-\> region:Lewis, word:preas \-\> region:Barra).  
  * **Application:** Analyze a proposed textbook by querying its vocabulary list against the dim\_Document (region) table.  
  * **Insight:** If a textbook claims to be for "National use" but contains 90% "South Wales" vocabulary, the system flags a bias. This ensures equitable representation of dialects (e.g., Doric vs. Glaswegian Scots) in educational materials, fostering inclusivity.

### **4.4 Longitudinal Tracking and Predictive Modelling**

By tracking anonymized learners over time via the Moodle-linked schema:

* **Feature:** Acquisition\_Velocity  
  * **Context:** How long does it take an average learner to master the Irregular Verbs?  
  * **Mechanism:** Measure the time delta between the first exposure to a concept (in dim\_CurriculumGoal) and the point where the error\_rate for that concept drops below 10%.  
  * **Insight:** This allows for "Predictive Analytics" in Dashboards.44 The system can warn a teacher: "Student X is falling behind the typical acquisition curve for Prepositional Pronouns; intervention recommended."

## **Part V: Strategic Implications for Education Policy**

Synthesizing the data from these models allows for high-level strategic insights that can inform policy at the Welsh Government, Scottish Government, and local authority levels.

### **5.1 The "Data Gap" in Immersion Education**

The comparative analysis of resources reveals a stark "Data Gap." While Welsh has *Y Tiwtiadur* 3, which operationalizes corpus data for teachers, Scottish Gaelic and Manx lack an equivalent middleware.

* **Implication:** Educational policy in Scotland should pivot funding from creating static content (PDFs, videos) to building **API wrappers** around existing archives like DASG. The data exists; the interface does not. Prioritizing a "Gaelic Tiwtiadur" would yield high returns on investment.

### **5.2 Standardization vs. Authenticity**

The *Scots Syntax Atlas* data 20 highlights that "Standard Scots" is a construct that contradicts the linguistic reality of speakers.

* **Implication:** A rigid, standardized curriculum for Scots is data-contraindicated. The database architecture supports variant\_of relationships rather than a binary correct/incorrect. Educational tools must be geo-aware, validating a learner's use of *div* (do) in Aberdeen while correcting it in Glasgow. The technology allows for a "pluricentric" education model that static textbooks cannot support.

### **5.3 The Pan-Celtic Network Effect**

The "long tail" languages (Cornish, Manx) suffer from data sparsity. However, the shared typological features (VSO order, conjugated prepositions) offer a solution.

* **Implication:** We can leverage **Cross-Lingual Transfer Learning**. A POS tagger trained on the massive 11-million-word CorCenCC (Welsh) can be fine-tuned on the small Korpus Kernewek (Cornish) with relatively little data, achieving far higher accuracy than training on Cornish alone.  
* **Policy Recommendation:** Funding bodies should incentivize "Pan-Celtic" digital infrastructure projects rather than siloed single-language grants. A shared "Brythonic NLP Node" is more viable than separate Welsh and Cornish projects.

### **5.4 From Preservation to Production: Generative AI**

Most current resources (DASG, DSL) focus on *preservation* (archives). The proposed SQL/Graph model shifts the focus to *production*.

* **Implication:** The cleaned, harmonized data in the Lakehouse is the perfect training set for fine-tuning Large Language Models (LLMs) for Celtic languages. This enables the creation of "Chatbots for Learners" or "Automated Essay Scoring" systems. By controlling the training data (excluding toxic or low-quality text), we can create "Safe LLMs" specifically for the classroom environment.

## **Conclusion**

The disparate digital resources for Welsh, Scottish Gaelic, Manx, Cornish, and Scots represent a latent goldmine for education. However, in their current state—fragmented, siloed, and often archival in nature—they yield only a fraction of their potential value.  
By implementing the **Federated Linguistic Data Lakehouse** architecture proposed in this report, we can transform these static assets into a dynamic intelligence engine. The integration of CorCenCC, ARCOSG, SCOTS, and other corpora into a unified SQL and Graph schema, governed by standards like Universal Dependencies and OntoLex-Lemon, allows for a quantum leap in educational capability.  
We move from asking "Is there a text about history?" to asking "Which historical texts are linguistically appropriate for a B1 learner in Gwynedd?" We move from correcting errors to understanding the cognitive processes behind them. For the endangered languages of the British Isles, this shift from data scarcity to data utility—from preservation to pedagogical mobilization—is not merely a technical upgrade; it is a vital strategy for ensuring their transmission to the next generation. The technology exists; the imperative now is integration.

#### **Works cited**

1. CorCenCC \- Wikipedia, accessed December 13, 2025, [https://en.wikipedia.org/wiki/CorCenCC](https://en.wikipedia.org/wiki/CorCenCC)  
2. CorCenCC: Corpws Cenedlaethol Cymraeg Cyfoes – the National Corpus of Contemporary Welsh (Version 1.0.0) \- Cardiff University \- Figshare, accessed December 13, 2025, [https://research-data.cardiff.ac.uk/articles/dataset/CorCenCC\_Corpws\_Cenedlaethol\_Cymraeg\_Cyfoes\_the\_National\_Corpus\_of\_Contemporary\_Welsh\_Version\_1\_0\_0\_/27053194](https://research-data.cardiff.ac.uk/articles/dataset/CorCenCC_Corpws_Cenedlaethol_Cymraeg_Cyfoes_the_National_Corpus_of_Contemporary_Welsh_Version_1_0_0_/27053194)  
3. CorCenCC – National Corpus of Contemporary Welsh, accessed December 13, 2025, [https://corcencc.org/](https://corcencc.org/)  
4. Y Tiwtiadur – CorCenCC – National Corpus of Contemporary Welsh, accessed December 13, 2025, [https://corcencc.org/y-tiwtiadur/](https://corcencc.org/y-tiwtiadur/)  
5. Welsh National Corpora Portal, accessed December 13, 2025, [https://corpws.cymru/?lang=en](https://corpws.cymru/?lang=en)  
6. Welsh language technology | Helo Blod \- Business Wales, accessed December 13, 2025, [https://businesswales.gov.wales/heloblod/welsh-language-technology](https://businesswales.gov.wales/heloblod/welsh-language-technology)  
7. Digital Archive of Scottish Gaelic: DASG, accessed December 13, 2025, [https://dasg.ac.uk/en](https://dasg.ac.uk/en)  
8. DASG: Digital Archive of Scottish Gaelic / Dachaigh airson Stòras na Gàidhlig, accessed December 13, 2025, [https://digital-humanities.glasgow.ac.uk/project/?id=20](https://digital-humanities.glasgow.ac.uk/project/?id=20)  
9. CQPweb User Page, accessed December 13, 2025, [https://dasg.arts.gla.ac.uk/CQPweb/usr/index.php?ui=latest](https://dasg.arts.gla.ac.uk/CQPweb/usr/index.php?ui=latest)  
10. CQPweb — combining power, flexibility and usability in a corpus analysis tool \- Lancaster University, accessed December 13, 2025, [https://www.lancaster.ac.uk/staff/hardiea/cqpweb-paper.pdf](https://www.lancaster.ac.uk/staff/hardiea/cqpweb-paper.pdf)  
11. Annotated Reference Corpus of Scottish Gaelic (ARCOSG) \- University of Edinburgh Research Explorer, accessed December 13, 2025, [https://www.research.ed.ac.uk/en/datasets/annotated-reference-corpus-of-scottish-gaelic-arcosg/](https://www.research.ed.ac.uk/en/datasets/annotated-reference-corpus-of-scottish-gaelic-arcosg/)  
12. Gaelic-Algorithmic-Research-Group/ARCOSG-S: Annotated Corpus of Scottish Gaelic (Simplified) \- GitHub, accessed December 13, 2025, [https://github.com/Gaelic-Algorithmic-Research-Group/ARCOSG-S](https://github.com/Gaelic-Algorithmic-Research-Group/ARCOSG-S)  
13. Universal dependencies for Scottish Gaelic: syntax \- ACL Anthology, accessed December 13, 2025, [https://aclanthology.org/W19-6902.pdf](https://aclanthology.org/W19-6902.pdf)  
14. UD for Scottish Gaelic \- Universal Dependencies, accessed December 13, 2025, [https://universaldependencies.org/gd/index.html](https://universaldependencies.org/gd/index.html)  
15. Gaelic Resources \- Young Scot, accessed December 13, 2025, [https://young.scot/get-informed/gaelic-resources/](https://young.scot/get-informed/gaelic-resources/)  
16. LearnGaelic, accessed December 13, 2025, [https://learngaelic.net/](https://learngaelic.net/)  
17. Gaelic Resources \- Sgoil Gàidhlig Bhaile an Taigh Mhòir, accessed December 13, 2025, [https://sgoilgaidhlig.org/gaelic-resources/](https://sgoilgaidhlig.org/gaelic-resources/)  
18. Scots Corpus, accessed December 13, 2025, [https://www.scottishcorpus.ac.uk/](https://www.scottishcorpus.ac.uk/)  
19. Corpus Details \- SCOTS, accessed December 13, 2025, [https://www.scottishcorpus.ac.uk/corpus-details/](https://www.scottishcorpus.ac.uk/corpus-details/)  
20. The Scots Syntactic Atlas \- UKRI Gateway to Research, accessed December 13, 2025, [https://gtr.ukri.org/projects?ref=AH%2FM005550%2F1](https://gtr.ukri.org/projects?ref=AH/M005550/1)  
21. Dictionary of the Scots Language \- Wikipedia, accessed December 13, 2025, [https://en.wikipedia.org/wiki/Dictionary\_of\_the\_Scots\_Language](https://en.wikipedia.org/wiki/Dictionary_of_the_Scots_Language)  
22. Digital Resources for the Languages in Ireland and Britain \- CLARIN-UK, accessed December 13, 2025, [https://www.clarin.ac.uk/article/digital-resources-languages-ireland-and-britain](https://www.clarin.ac.uk/article/digital-resources-languages-ireland-and-britain)  
23. Universal Dependencies for Manx Gaelic, accessed December 13, 2025, [https://universaldependencies.org/udw20/papers/2020.udw2020-1.17.pdf](https://universaldependencies.org/udw20/papers/2020.udw2020-1.17.pdf)  
24. Cornish language \- Wikipedia, accessed December 13, 2025, [https://en.wikipedia.org/wiki/Cornish\_language](https://en.wikipedia.org/wiki/Cornish_language)  
25. About | Akademi Kernewek, accessed December 13, 2025, [https://www.akademikernewek.org.uk/corpus/about?locale=en](https://www.akademikernewek.org.uk/corpus/about?locale=en)  
26. “Because They Are Cornish”: Four Uses of a Useless Language \- ResearchGate, accessed December 13, 2025, [https://www.researchgate.net/publication/343744606\_Because\_They\_Are\_Cornish\_Four\_Uses\_of\_a\_Useless\_Language](https://www.researchgate.net/publication/343744606_Because_They_Are_Cornish_Four_Uses_of_a_Useless_Language)  
27. ETL Pipelines. High-Level Overview | by Het Daxeshkumar Patel | Nov, 2025 | Medium, accessed December 13, 2025, [https://medium.com/@Het9979/etl-pipelines-16d3b0847ade](https://medium.com/@Het9979/etl-pipelines-16d3b0847ade)  
28. ETL with SQL: Use Cases & How They Work Together (2024) \- Portable.io, accessed December 13, 2025, [https://portable.io/learn/etl-with-sql](https://portable.io/learn/etl-with-sql)  
29. CLARIN Knowledge Centre for Digital Resources for the Languages in Ireland and Britain, accessed December 13, 2025, [https://centres.clarin.eu/centre/82](https://centres.clarin.eu/centre/82)  
30. DR-LIB | CLARIN ERIC \- Common Language Resources and Technology Infrastructure, accessed December 13, 2025, [https://www.clarin.eu/k-centres/dr-lib](https://www.clarin.eu/k-centres/dr-lib)  
31. The IMS Open Corpus Workbench (CWB) Corpus Encoding Tutorial | Kielipankki, accessed December 13, 2025, [https://www.kielipankki.fi/wp-content/uploads/CWB\_Encoding\_Tutorial.pdf](https://www.kielipankki.fi/wp-content/uploads/CWB_Encoding_Tutorial.pdf)  
32. TEI Encoding as a Unified Structure for Multilingual Digital Editions: The LeggoManzoni Case Study \- AIUCD 2025, accessed December 13, 2025, [https://aiucd2025.dlls.univr.it/assets/pdf/papers/98.pdf](https://aiucd2025.dlls.univr.it/assets/pdf/papers/98.pdf)  
33. Architecture of MySQL \- GeeksforGeeks, accessed December 13, 2025, [https://www.geeksforgeeks.org/mysql/architecture-of-mysql/](https://www.geeksforgeeks.org/mysql/architecture-of-mysql/)  
34. Graph Databases for Diachronic Language Data Modelling \- ACL Anthology, accessed December 13, 2025, [https://aclanthology.org/2023.ldk-1.8.pdf](https://aclanthology.org/2023.ldk-1.8.pdf)  
35. Linguistic Linked Open Data, accessed December 13, 2025, [https://linguistic-lod.org/](https://linguistic-lod.org/)  
36. Linguistic Linked Open Data \- Wikipedia, accessed December 13, 2025, [https://en.wikipedia.org/wiki/Linguistic\_Linked\_Open\_Data](https://en.wikipedia.org/wiki/Linguistic_Linked_Open_Data)  
37. The OntoLex-Lemon Model: Development and Applications \- eLex Conferences, accessed December 13, 2025, [https://elex.link/elex2017/wp-content/uploads/2017/09/paper36.pdf](https://elex.link/elex2017/wp-content/uploads/2017/09/paper36.pdf)  
38. ELG \- Annotated Reference Corpus of Scottish Gaelic \- European Language Grid, accessed December 13, 2025, [https://live.european-language-grid.eu/catalogue/corpus/14441](https://live.european-language-grid.eu/catalogue/corpus/14441)  
39. Moodle \- ER diagram at dbdiagrams.com | Database design, accessed December 13, 2025, [https://www.dbdiagrams.com/mysql/online-er-diagram-moodle/](https://www.dbdiagrams.com/mysql/online-er-diagram-moodle/)  
40. Database schema introduction \- MoodleDocs, accessed December 13, 2025, [https://docs.moodle.org/dev/Database\_schema\_introduction](https://docs.moodle.org/dev/Database_schema_introduction)  
41. Outputs – CorCenCC – National Corpus of Contemporary Welsh, accessed December 13, 2025, [https://corcencc.org/outputs/](https://corcencc.org/outputs/)  
42. Competency API \- MoodleDocs, accessed December 13, 2025, [https://docs.moodle.org/dev/Competency\_API](https://docs.moodle.org/dev/Competency_API)  
43. Annual report on implementation of the recommendations from the Black, Asian and Minority Ethnic Communities, Contributions and Cynefin in the New Curriculum Working Group report \[HTML\] | GOV.WALES, accessed December 13, 2025, [https://www.gov.wales/annual-report-implementation-recommendations-black-asian-and-minority-ethnic-communities-html](https://www.gov.wales/annual-report-implementation-recommendations-black-asian-and-minority-ethnic-communities-html)  
44. Learning analytics dashboard: a tool for providing actionable insights to learners \- PMC, accessed December 13, 2025, [https://pmc.ncbi.nlm.nih.gov/articles/PMC8853217/](https://pmc.ncbi.nlm.nih.gov/articles/PMC8853217/)