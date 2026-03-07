# **Architectural & Curricular Analysis: Digital Transformation of Leaving Certificate Prescribed Materials**

## **1\. Executive Summary and Strategic Scope**

### **1.1 Project Objective and Context**

The objective of this report is to provide a comprehensive, expert-level analysis of the prescribed material datasets for the Irish and English Leaving Certificate examinations, specifically for the purpose of architecting a full-stack educational application using the TanStack Start framework. The user’s requirement involves translating static, government-issued curricular circulars—represented by simplistic tabular data 1—into a dynamic, queryable, and user-centric digital experience.  
This report operates at the intersection of educational pedagogy and software engineering. It does not merely list the syllabus content; rather, it deconstructs the underlying data models, relationships, and temporal patterns inherent in the source material to inform a robust database schema and frontend architecture. The source material provided 1 aggregates historical examination questions, poet rotations, and thematic keywords spanning over two decades. This longitudinal data is critical for building features such as predictive analytics, thematic filtering, and archival search, which are standard expectations for modern educational technology platforms.  
The choice of TanStack Start as the underlying framework is particularly pertinent to the nature of this data. The curriculum data is high-volume but semi-static, making it an ideal candidate for server-side generation and efficient data streaming—core capabilities of the framework. However, the complexity lies in the heterogeneity of the data: the English curriculum operates on a cyclical logic of "Poet Rotations" 1, while the Irish curriculum operates on a linear, thematic logic involving distinct literary genres (Prose, Poetry, and Folklore).1 This report will demonstrate that a "one-size-fits-all" data model is insufficient. Instead, a polymorphic architecture is required to handle the distinct metadata shapes of English Poets versus Irish Texts.

### **1.2 The Nature of the Datasets**

The analysis is based on four distinct data clusters identified within the provided research material 1:

1. **The Irish Prose Matrix (2012–2021):** This dataset maps literary works such as *Hurlamboc*, *Dís*, and *Cáca Milis* to specific examination years. Crucially, it includes the raw text of the essay prompts, which reveals the specific *angle* of inquiry (e.g., character analysis of "Lisín" vs. thematic analysis of "Disability").  
2. **The Irish Poetry Repository:** This cluster links poems like *Géibheann* and *An Spailpín Fánach* to a tripartite question structure (A, B, C) that emphasizes emotional impact (*Mothúchán*), stylistic technique (*Meadaracht*), and biographical context (*Saol an fhile*).  
3. **The English Poet Rotation Index (2000–2023):** A binary dataset tracking the presence or absence of 27 distinct poets over a 23-year period. This is the foundational dataset for any predictive features in the application.  
4. **The English Stylistic Taxonomy:** A qualitative dataset that maps specific poets to recurring critical descriptors (e.g., Bishop’s "analytical" style vs. Keats’s "sensuous beauty"). This provides the semantic tags necessary for a rich filtering experience.

### **1.3 Core Recommendations**

The report argues for a "Content-First" schema design. The application should not view "Year" as the primary entity, but rather "Prescribed Work" as the primary entity, with "Exam Appearances" as a child relationship. This inversion allows the application to tell the story of a text (e.g., "How has the interpretation of *Hurlamboc* changed from 2016 to 2021?") rather than just listing what came up in a specific year. Furthermore, the report recommends a dual-language indexing strategy for the Irish content to ensure accessibility for students with varying levels of fluency, utilizing the specific vocabulary found in the snippets (e.g., mapping "Hardship" to "Cruachás").1

## ---

**2\. Domain Analysis: The English Curriculum**

The English syllabus data provided in the source documents constitutes a complex system of cyclical prescription. Unlike a static syllabus where the same texts are examined every year, the English Leaving Certificate employs a rotation system involving a pool of poets. Understanding the mechanics of this rotation is essential for the application's "Study Planner" and "Prediction" features.

### **2.1 The Poet Rotation Matrix (2000–2023)**

The most structurally significant dataset available is the historical record of poet appearances found on Page 3 of the source material.1 This table lists 27 poets and their examination status across 17 distinct years (with some gaps). For a TanStack Start application, this data is not merely historical trivia; it is the raw material for a "Frequency Analysis" engine.

#### **2.1.1 The Poet Pool and Categorization**

The dataset identifies the following 27 poets as the canonical "Universe of Discourse" for the application:  
Bishop, Boland, Dickinson, Donne, Durcan, Eliot, Frost, Hardy, Heaney, Hopkins, Kavanagh, Keats, Kennelly, Kinsella, Larkin, Lawrence, Longley, Mahon, Meehan, Montague, Ní Chuilleanáin, Plath, Rich, Shakespeare, Walcott, Wordsworth, and Yeats.1  
From a data modeling perspective, this list represents a static ENUM or a reference table Poets in the database. The stability of this list over 23 years suggests that the application does not need a highly dynamic CMS for adding new poets frequently, but rather a robust attribute management system for the existing ones.

#### **2.1.2 Temporal Patterns and Probability**

An analysis of the checkmarks in the source table 1 reveals distinct tiers of frequency which the application should visualize for the user.

* **High-Frequency/Anchor Poets:**  
  * **Emily Dickinson:** The data shows appearances in 2023, 2022, 2020, and 2015\.1 The cluster of recent appearances (2020, 2022, 2023\) indicates a current prioritization by the examination board. In the application UI, Dickinson should be flagged as "Trending" or "High Probability."  
  * **W.B. Yeats:** Appearances in 2023, 2022, and 2016 1 mirror Dickinson’s pattern, suggesting a tendency to pair these canonical figures in recent years.  
  * **Hopkins:** A distinct pattern of odd-year appearances is visible: 2021, 2019, 2017, 2013, 2011\.1 This "Odd-Year Cycle" is a massive insight for the application’s predictive logic. A student sitting the exam in an even year might de-prioritize Hopkins based on this historical data.  
* **Sporadic/Rotational Poets:**  
  * **Boland:** Appeared in 2018 and 2015\.1 The three-year gap and subsequent absence suggests a mid-tier rotation frequency.  
  * **Donne:** Appeared in 2023 and 2017\.1 The six-year gap is significant.  
  * **Heaney:** Appeared in 2021 and 2019\.1 The close proximity suggests a recent surge in popularity similar to Dickinson.  
* **Long-Tail/Dormant Poets:**  
  * **Larkin:** The last visible checkmark is in 2007\.1  
  * **Longley:** Last seen in 2010 and 2008\.1  
  * **Montague:** Last seen in 2007\.1  
  * **Wordsworth:** Last seen in 2011 and 2013\.1

Application Implication: The TanStack Start loader for the "English Dashboard" should calculate a "Recency Score" for each poet.

$$\\text{Recency Score} \= \\sum \\frac{1}{\\text{CurrentYear} \- \\text{ExamYear}}$$

Poets like Larkin or Montague would have near-zero scores, signaling to the student that they are likely "off-course" or low priority, whereas Dickinson would have a high score. This transforms raw data into actionable study advice.

#### **2.1.3 Data Inconsistencies and User Trust**

The table contains years with no data for certain poets, or ambiguous markings. For instance, the column for "2000" and "2008" are grouped or compressed in the visual layout.1 The application must handle sparse data gracefully. If the status of a poet in 2005 is unknown, the UI must clearly differentiate between "Confirmed Absent" and "Unknown Data." The table explicitly marks checks (✓) for presence; the absence of a check is interpreted as an absence from the exam. This binary state (Present/Absent) simplifies the database schema to a boolean flag or a sparse link table.

### **2.2 Semantic Analysis of Poet Profiles**

Page 4 of the research material 1 provides a qualitative dataset that is arguably more valuable than the quantitative rotation data: a "Stylistic Taxonomy" of the poets. This text describes *how* the examination board views each poet, providing the keywords that students must use in their essays.

#### **2.2.1 The "Bishop-Keats" Spectrum**

The dataset draws a sharp contrast between Elizabeth Bishop and John Keats, which serves as a perfect example for the application's "Comparative Study" feature.

* **Elizabeth Bishop:** The source defines her work as "analytical but rarely emotional".1 It emphasizes her "skilful use of language and imagery to confront life's harsh realities".1  
* **John Keats:** In contrast, Keats is defined by "sensuous beauty" which is "diminished by our awareness of the fear or melancholy evident in his work".1

**Insight:** The app should utilize a tagging system based on these descriptors.

* Tag: Analytical \-\> Maps to Bishop.  
* Tag: Sensuous \-\> Maps to Keats.  
* Tag: Imagery \-\> Maps to both Bishop ("confront harsh realities") and Keats ("sensuous language").  
  This allows a student to search for "Imagery" and see how it is applied differently across the syllabus (Confrontation vs. Sensation).

#### **2.2.2 The Duality of W.B. Yeats**

The data describes Yeats with a specific duality: "His poetry is both intellectually stimulating and emotionally charged".1 It further elaborates on the "tension between the real world he lives in and the ideal world that he imagines".1  
This "Tension" is a critical database entity. The application should have a Theme entity called "Reality vs. Imagination" and link it heavily to Yeats. The phrase "intellectually stimulating" suggests that questions on Yeats will often require a more philosophical approach compared to the "sensitive exploration" 1 associated with Brendan Kennelly.

#### **2.2.3 Emily Dickinson’s Aesthetic Paradox**

The source material highlights a unique attribute for Dickinson: the "balance between beautiful and horrific imagery".1 It notes that her "unique approach to language... help\[s\] to relieve some of the darker aspects of her poetry".1  
Application Feature: A "Keyword Cloud" for Dickinson must include: Beautiful, Horrific, Darker Aspects, Relief, Original Approach. The snippet also mentions her style can "intrigue and confuse".1 This "Confusion" aspect is a pedagogical hook—the app could offer a "Demystifying Dickinson" module that specifically addresses the confusing aspects mentioned in the circulars.

#### **2.2.4 Adrienne Rich and the Theme of Power**

For Adrienne Rich, the circulars are explicit: she "Explores the twin themes of power and powerlessness".1 This is a definitive, high-yield tag. Any student studying Rich *must* cover Power. The data also links her to "dramatic settings" and "wider social concerns".1 This places her in the "Social Commentary" cluster of poets, likely alongside Boland (though Boland's specific text is cut off, the context implies similar feminist/social themes).

## ---

**3\. Domain Analysis: The Irish Curriculum (An Ghaeilge)**

The Irish data 1 presents a different architectural challenge. While English is defined by *who* is on the paper, Irish is defined by *what* is asked about specific, stable texts. The data provided covers the years 2012–2021 and breaks down into Prose (*Prós*), Poetry (*Filíocht*), and Additional Literature (*Breise*).

### **3.1 Prose (Prós): Character-Centric Metadata**

The Prose section of the syllabus is dominated by a few key texts: *Hurlamboc*, *Oisín i dTír na nÓg*, *Cáca Milis*, *Dís*, and *An Gnáthrud*. The questions provided in the snippets 1 allow us to construct a "Character Graph" for the application.

#### **3.1.1 The "Lisín" Archetype (*Hurlamboc*)**

The text *Hurlamboc* appears frequently (2021, 2016). The questions are remarkably consistent in their focus on the character "Lisín."

* **2021:** "bhí caithréim bainte amach ag Lisín agus a lán ceiliúradh ag a clann, dar leí féin" (Lisín had achieved triumph and much celebration for her family, according to herself).1  
* **2016:** "tá Lisín i gceannas ar a shaol, theaghlach, theach" (Lisín is in control of her life, family, house).1  
* **Analysis:** The recurrence of "caithréim" (triumph) and "i gceannas" (in control) combined with the qualifier "dar leí féin" (according to herself) implies a thematic focus on **Delusion** and **Control**. The application should link *Hurlamboc* not just to the tag Character: Lisín but to the thematic tags Control and Self-Deception.

#### **3.1.2 The Moral Complexity of *Cáca Milis***

The text *Cáca Milis* (Sweet Cake) features distinct questions about the interaction between two characters: Catherine and Paul.

* **2020:** "Is duine le míchumas é Paul nach dtuilleann mórán trua on lucht féachana" (Paul is a person with a disability who doesn't deserve much pity from the audience).1  
* **2015:** "Ní duine deas í Catherine, m.sh. sa chaoi a chaitheann sí le Paul" (Catherine is not a nice person, e.g., in the way she treats Paul).1  
* **Analysis:** The questions invite judgment. The 2020 question is particularly provocative, asking the student to argue *against* pitying a disabled character ("nach dtuilleann mórán trua"). This suggests the app needs to prepare students for **Argumentative** essays, not just descriptive ones. Tags: Disability, Cruelty, Reader Response.

#### **3.1.3 *Oisín i dTír na nÓg*: The Traditional Hero**

The questions for this folklore text focus on the "Traditional Hero" attributes.

* **2021:** "Cruachás Oisín" (Oisín's Hardship).1  
* **2017:** "Oisín duine grámhar" (Oisín as a loving person).1  
* **2013:** "Páirt Niamh sa scéal" (Niamh's role in the story).1  
* **Analysis:** The themes are softer and more romantic/tragic compared to the modern texts. Key vocabulary identified for the app's glossary: Dílseacht (Loyalty), Grá (Love), Fianna.

#### **3.1.4 *Dís* and Domestic Tension**

The text *Dís* (Pair/Couple) has questions focusing on the female protagonist.

* **2021:** "Saol bhean Sheáin agus tionchar a bhí ag cuairt bhean an tsuirbhé" (Life of Seán's wife and the influence of the survey woman's visit).1  
* **2019:** "Saol agus meon bhean Sheáin" (Life and mindset of Seán's wife).1  
* **Analysis:** The focus is on "Meon" (Mindset) and the external catalyst of the "Survey." The consistency of "Bean Sheáin" (Seán's Wife) as the focal point suggests the character is defined by her relationship, a key thematic point for students.

### **3.2 Poetry (Filíocht): The A/B/C Structure**

The structure of Irish poetry questions is distinct from the Prose. The data shows a persistent "A, B, C" pattern in recent years (2012–2021) which effectively dictates the UI layout for the application's "Poem View."

#### **3.2.1 The Components of the Question**

* **Part A (Thematic/Descriptive):** Usually asks for a description or contrast.  
  * *Géibheann* (2021): "Codarsnacht i saol an ainmhí, fadó vs faoi láthair" (Contrast in the animal's life, long ago vs. present).1  
  * *An Spailpín Fánach* (2015): "Cur síos éifeachtach ar shaol agus ar chás an spailpín" (Effective description of the life and case of the spailpín).1  
* **Part B (Technical/Emotional):** Focuses on technique or specific impact.  
  * *An t-Earrach Thiar* (2012): "Éifeacht atá ag 'San Earrach thiar' a úsáid i ngach véarsa?" (Effect of using 'In the Western Spring' in every verse?).1  
  * *Mo Ghrá-sa* (2012): "Úsáid lúibíní sa dán?" (Use of brackets in the poem?).1  
  * *Global:* "Mothúchán" (Emotion) is a ubiquitous Part B question.1  
* **Part C (Biographical):**  
  * "Saol agus saothar an fhile" (Life and work of the poet) appears repeatedly for *Géibheann* (2016, 2021\) and *An t-Earrach Thiar* (2019).1

**Architectural Implication:** The database cannot simply store a string question\_text. It must store a JSON object:

JSON

{  
  "part\_a": "Codarsnacht i saol an ainmhí...",  
  "part\_b": "Teideal oiriúnach?",  
  "part\_c": "Saol agus saothar an fhile"  
}

This structure is mandatory to display the data correctly in the TanStack app, matching the user's "simplistic file" structure which aligns these sub-questions in rows.

#### **3.2.2 Key Poems and Themes**

* **Géibheann (Captivity):** Themes of Freedom vs. Captivity, Contrast, Animal Imagery.  
* **An t-Earrach Thiar (Spring in the West):** Themes of Nostalgia, Idealized Past, Work/Labour.  
* **An Spailpín Fánach:** Themes of Poverty, Pride ("Brón agus bród"), Hardship.  
* **Mo Ghrá-sa:** Themes of Anti-Love Poem, Humour ("Greannmhar"), Satire.  
* **Colscaradh (Divorce):** Themes of Separation, Conflict, Modern Relationships.

### **3.3 Additional Material (Breise)**

The section for "Breise" specifically highlights the text *A Thig Ná Tit Orm* (Maidhc Dainín Ó Sé).

* **Nature of Questions:** These are almost exclusively "Accounts" (*Cuntas*) of the author's life.  
* **Topics:** "Cúrsaí scolaíochta" (School matters) 1, "Eachtraí a óige" (Events of his youth) 1, "Teaghlach agus pobal" (Family and community) 1, "Ceol agus an pheil" (Music and football).1  
* **Strategy:** This section of the app should be presented as a "Memoir Study" distinct from the fiction sections. The tags are factual/biographical rather than abstract.

## ---

**4\. Technical Architecture: TanStack Start Implementation**

Building this application requires a thoughtful translation of the domain analysis into software primitives. TanStack Start, with its emphasis on full-stack type safety and server-side rendering, provides the tools to handle the complexity identified above.

### **4.1 Data Modeling and Schema Design**

The heterogeneity of the data (English Matrices vs. Irish Thematic Trees) suggests a Relational Database (PostgreSQL) is superior to NoSQL here, as the relationships between Years, Texts, and Questions are strict and structured.

#### **4.1.1 Core Entities**

* **Subject:** Root entity (English vs. Gaeilge).  
* **CurriculumYear:** Represents the exam year (e.g., 2021). Important for grouping.  
* **PrescribedItem:** A polymorphic entity representing a Poet (English) or a Text (Irish).  
  * id: UUID  
  * title: String ("Hurlamboc" or "Emily Dickinson")  
  * type: ENUM ("POET", "PROSE\_TEXT", "POEM\_TEXT")  
  * author: String  
* **QuestionEvent:** The intersection of a PrescribedItem and a CurriculumYear.  
  * id: UUID  
  * item\_id: FK \-\> PrescribedItem  
  * year: Int (2021)  
  * content\_payload: JSONB.  
    * *For English:* { "appeared": true, "question\_focus": "Skillful use of technique..." }  
    * *For Irish:* { "sub\_questions": { "A": "...", "B": "..." }, "tags": \["Lisín", "Ceiliúradh"\] }

#### **4.1.2 Handling the "Simplistic File" Structure**

The user wants to "display information like the simplistic file attached." This file is a tabular matrix. To recreate this in TanStack Start:

1. **Server Loader:** Fetch all QuestionEvents for a given Subject.  
2. **Transformation:** Group by PrescribedItem.  
3. **Data Structure:**  
   TypeScript  
   type GridRow \= {  
     textTitle: string;  
     years: {  
       \[year: number\]: {  
         questionSnippet: string; // "bhí caithréim bainte amach..."  
         tags: string;  
       } | null; // Null if not present that year  
     }  
   }

This structure allows the frontend to render the exact grid view the user sees in the PDF, but with interactive capabilities (hover states, click-to-filter).

### **4.2 Search and Indexing Strategy**

The research material 1 is multilingual. The Irish questions are in Gaeilge. A naive search for "Emotion" would fail to find the relevant Irish questions tagged with "Mothúchán."

* **Synonym Layer:** The application needs a translation map in the backend.  
  * Map: {"emotion": \["mothúchán", "mothú"\], "contrast": \["codarsnacht"\], "life": \["saol"\]}.  
* **Full-Text Search:** Postgres tsvector should be used on the content\_payload column.  
  * For Irish text, a custom dictionary might be needed, or simple unaccented matching (treating 'á' as 'a') to help students who struggle with typing fadas.

### **4.3 Frontend Visualization (TanStack)**

The UI must reflect the distinct nature of the subjects.

* **The English Dashboard (The Matrix):**  
  * Use a CSS Grid or HTML Table to replicate the poet rotation view.1  
  * **Visual Cues:** Use color intensity to indicate "Hot" poets (e.g., Yeats with 3 recent checks).  
  * **Interactivity:** Clicking a cell (e.g., "Dickinson 2020") opens a modal with the specific question profile: "Unique approach to language...".1  
* **The Irish Dashboard (The Timeline):**  
  * Instead of a grid, a "Timeline Card" approach is better for the text-heavy Irish questions.  
  * **Grouping:** Group by Text (e.g., a section for *Hurlamboc*).  
  * **Cards:** Each card represents a year. Inside the card, display the A/B/C structure clearly.  
  * **Tagging:** Highlight keywords found in the circulars (e.g., highlight "Míchumas" in Red for *Cáca Milis*).

## ---

**5\. Strategic Insights & Narrative Synthesis**

The data provided in the simplistic files 1 tells a story of evolving educational priorities. By building this application, the user is not just digitizing paper; they are revealing these hidden narratives to students.

### **5.1 The Shift Towards "Personal Response"**

In both subjects, there is a discernable trend towards valuing the student's personal reaction over rote learning.

* **English:** The prompt for Boland/Rich asks: "Does her poetry speak to you? Write your personal response".1  
* **Irish:** The recurrence of "i bhfeidhm ort" (impact on you) in poetry questions (*Géibheann* 2016\) 1 mirrors this.  
* **System Design:** The application should include a "Journaling" or "Notes" feature next to each text, encouraging students to write their *own* response, as this is explicitly rewarded by the exam prompts identified in the data.

### **5.2 The Standardization of Biography**

The Irish data reveals that "Saol agus saothar an fhile" (Life and work of the poet) is a standardized, reusable component of the exam.1 It is not random; it is structural.

* **System Design:** The content management system for the app should have a dedicated "Biography Field" for every poet. This content should be technically separate from the poems, as it can be injected into any year's exam question as Part C.

### **5.3 The Complexity of "Simple" Tables**

The user's request refers to the source as a "simplistic file." This analysis demonstrates that the file is anything but simple. It contains:

* **Temporal Logic:** Rotations and frequencies.  
* **Semantic Logic:** "Analytical" vs "Sensuous."  
* **Structural Logic:** A/B/C sub-questions.  
* **Linguistic Logic:** Gaeilge vocabulary requiring translation.

A successful TanStack Start application must respect this complexity. It must parse the "merged cells" of the PDF not just as visual quirks, but as data grouping instructions (e.g., *Dís* applying to multiple contexts). It must treat the empty cells in the English matrix not as "Null" but as "Not Prescribed," which is a meaningful status.

## **6\. Detailed Data Reconstruction for Seed Generation**

To assist the user in immediately populating their TanStack Start database, the following section reconstructs the core data relationships derived *strictly* from the snippets 1, formatted for direct transcription into a seed script.

### **6.1 English Poet Data (Reconstructed)**

| Poet | Style Keywords (Source: ) | Recent Years Active (Source: ) |
| :---- | :---- | :---- |
| **Bishop** | Analytical, Rarely Emotional, Skilful Technique, Harsh Realities | 2023 |
| **Dickinson** | Unique Language, Beautiful vs Horrific, Darker Aspects, Relief, Intrigue, Confuse | 2023, 2022, 2020, 2015 |
| **Keats** | Sensuous Beauty, Fear, Melancholy, Diminished Enjoyment | (No recent checks visible in snippet, historic data implied) |
| **Kennelly** | Sensitive exploration, Humanity, Characters | 2022 |
| **Lawrence** | Observation, People/Places, Unique Personal Experiences | (No recent checks visible) |
| **Rich** | Dramatic Settings, Personal vs Social, Power and Powerlessness | (No recent checks visible) |
| **Wordsworth** | Natural Imagery, Memory, Reflection, Real vs Ideal | 2013, 2011 |
| **Yeats** | Intellectual vs Emotional, Tension, Real vs Ideal | 2023, 2022, 2016 |
| **Hopkins** | (No style text in snippet) | 2021, 2019, 2017, 2013, 2011 |
| **Heaney** | (No style text in snippet) | 2021, 2019 |

### **6.2 Irish Text Data (Reconstructed)**

| Text | Genre | Year | Key Question Phrase (Source: ) | Derived Tags |
| :---- | :---- | :---- | :---- | :---- |
| **Hurlamboc** | Prós | 2021 | "bhí caithréim bainte amach ag Lisín..." | Lisín, Family, Success |
| **Hurlamboc** | Prós | 2016 | "tá Lisín i gceannas ar a shaol..." | Lisín, Control, Home |
| **Cáca Milis** | Prós | 2020 | "Is duine le míchumas é Paul..." | Paul, Disability, Pity |
| **Cáca Milis** | Prós | 2015 | "Ní duine deas í Catherine..." | Catherine, Cruelty |
| **Oisín** | Prós | 2021 | "Cruachás Oisín mar a gheall ar a ghrá..." | Oisín, Hardship, Love |
| **Oisín** | Prós | 2017 | "Oisín duine grámhar" | Oisín, Loving |
| **Dís** | Prós | 2021 | "Saol bhean Sheáin agus tionchar..." | Bean Sheáin, Survey |
| **Géibheann** | Filíocht | 2021 | A. Codarsnacht, B. Teideal, C. Saol an fhile | Contrast, Title, Bio |
| **Géibheann** | Filíocht | 2016 | A. Mothúchán, B.?, C. Saol an fhile | Emotion, Bio |
| **An Spailpín** | Filíocht | 2020 | A. Brón agus bród, B.?, C. Meadaracht | Sorrow, Pride, Meter |

## **7\. Conclusion**

The transformation of the "simplistic file" 1 into a full-stack application is a high-value exercise in structured data engineering. The provided documents, while visually simple, contain the blueprint for a sophisticated educational tool. By leveraging TanStack Start's server-side capabilities to pre-process the "Poet Rotation" logic and the "Question Taxonomy," the user can deliver an application that doesn't just display data, but actively guides the student through the predictable and unpredictable patterns of the Leaving Certificate. The key to success lies in the fidelity of the data entry—ensuring that the nuance of "Self-Deception" in *Hurlamboc* or "Tension" in *Yeats* is captured in the database tags, as detailed in this report. This attention to detail will differentiate the application from a simple digital list, creating a genuine study aid.  
**End of Report.**

#### **Works cited**

1. Gaeilge.pdf