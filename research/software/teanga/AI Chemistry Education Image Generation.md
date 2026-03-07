# **Advanced Computational Workflows for Bilingual Educational Asset Generation: Integrating BAML Structured Extraction with Bria AI’s Fibo Architecture**

## **1\. Executive Summary and Architectural Vision**

The digital transformation of secondary education demands a sophisticated convergence of pedagogical rigor and generative artificial intelligence. In the context of the Irish Leaving Certificate Chemistry specification, the challenge is not merely to generate images but to engineer a deterministic pipeline that translates curriculum standards into high-fidelity, scientifically accurate visual assets. This report presents a comprehensive research framework designed for a bilingual educational project participating in a hackathon, specifically targeting the integration of Better Abstract Meaning Language (BAML) for structured data extraction and the Bria AI "Fibo" model for safe, commercially viable image synthesis.  
The Leaving Certificate Chemistry syllabus is a complex, hierarchical document that mandates a deep understanding of the "Nature of Science," alongside specific chemical interactions in "The Nature of Matter," "Behaviour of Matter," "Interactions of Matter," and "Matter in Our World".1 A naive approach to generative AI—relying on unstructured natural language prompts—fails to capture the nuance of these requirements. It risks hallucinating incorrect molecular geometries, unsafe laboratory practices, or culturally irrelevant imagery. To mitigate this, we propose a "Schema-First" architecture. In this paradigm, the syllabus PDF is not treated as text but as a source of structured data. BAML acts as the semantic bridge, enforcing strict typing and logic to extract "Visual Concepts" from the syllabus. These concepts are then compiled into a specialized JSON payload optimized for the Fibo model, ensuring that the output is not only visually compelling but also chemically precise and educationally valid.  
Furthermore, the bilingual requirement of the project introduces a layer of complexity regarding terminological consistency between English and Gaeilge. This report explores how structured indexing pipelines can leverage knowledge graphs to manage these linguistic relationships, ensuring that a generated asset depicting "Eutrophication" is semantically linked to "Eotrófú" within the system’s metadata. By synergizing the syllabus’s theoretical framework with the technical capabilities of Bria AI and BAML, this report outlines a roadmap for creating a next-generation educational tool that champions accessibility, accuracy, and the aesthetic standards required for modern learning environments.

## **2\. Technical Architecture: The Syllabus-to-Asset Pipeline**

The core of this research posits that the generation of educational assets must follow a linear yet iterative pipeline: **Ingestion $\\rightarrow$ Semantic Structuring (BAML) $\\rightarrow$ Contextual Enrichment (Graph) $\\rightarrow$ Prompt Compilation (JSON) $\\rightarrow$ Synthesis (Fibo).**

### **2.1 The Role of BAML in Curriculum Parsing**

BAML (Better Abstract Meaning Language) serves as the extraction logic layer. Unlike standard regex or basic LLM prompting, BAML allows for the definition of strictly typed schemas that the LLM must adhere to during the extraction process. For a chemistry syllabus, this is non-negotiable. The specification is filled with specific lists of cations, anions, and reaction conditions that must be captured with 100% fidelity. For instance, when the syllabus mentions "flame tests (limited to salts of: Na, K, Cu, Li, Ba and Sr)" 1, a BAML extractor can be configured with an Enum to ensure only these elements are processed for visual generation, ignoring extraneous elements that might appear in broader chemical training data but are irrelevant to the specific examination context.  
The pipeline begins with the segmentation of the PDF "SC-Chemistry-Specification-EN.pdf".1 Each segment, corresponding to a "Strand" or "Sub-strand," is passed through a BAML function designed to identify "Visualizable Entities." A visualizable entity is any noun phrase or process description that can be depicted in a static image or diagram, such as "a volumetric flask," "a crystal lattice," or "an energy profile diagram." BAML’s ability to handle complex nested structures allows us to capture associated metadata—such as safety requirements or specific colors—attached to these entities.

### **2.2 Integration with Graph and Indexing Pipelines**

The user’s query highlights the use of "relevant graph and indexing pipelines." In this architecture, BAML does not just output a flat list; it populates a Knowledge Graph (e.g., Neo4j or a NetworkX structure).

* **Nodes:** Represent core concepts (e.g., "Ethanol," "Oxidation," "Liver").  
* **Edges:** Represent relationships defined by the syllabus (e.g., "Ethanol" \-- *is oxidised to* \--\> "Ethanal" \-- *using* \--\> "Potassium Dichromate").  
* **Properties:** Store the BAML-extracted attributes (e.g., Color: Orange $\\rightarrow$ Green).

This graph structure is critical for "contextually informed images." When the system prepares a prompt for "Ethanal," it queries the graph to see its upstream and downstream neighbors. It identifies that Ethanal is derived from Ethanol and can be further oxidized to Ethanoic Acid. This context allows the Fibo prompt to potentially include visual cues of this progression, such as a reaction scheme background or a specific reagent bottle, thereby enriching the educational value of the asset beyond a simple molecular render.

### **2.3 Bria AI Fibo: The JSON Prompting Paradigm**

Bria AI’s Fibo model distinguishes itself through commercial safety and structured control. Unlike consumer models that often operate on "vibe-based" prompting, Fibo is engineered for precision. This research assumes a "JSON Prompting" strategy where the model input is not a single string but a structured object containing separate fields for the *Subject*, *Style*, *Composition*, *Negative Constraints*, and *ControlNet Parameters*.  
The "Deep Research" component here involves mapping the BAML outputs directly to these JSON fields. If BAML extracts a "Safety Level" of "High" from the "Investigating in Chemistry" strand 1, the logic maps this to a pre-defined JSON object for Fibo that injects "safety goggles, lab coat, fume hood" into the *Subject* and *Negative Constraints* fields. This programmatic decoupling of content (the molecule) from constraints (safety, style) ensures consistency across thousands of generated assets.

## **3\. Deep Analysis of the Unifying Strand: The Nature of Science**

The "Unifying Strand" is the foundation of the Leaving Certificate Chemistry specification. It dictates *how* chemistry is practiced and understood. Visualizing this requires abstract thinking and precise prompting to capture the "process" rather than just the "product."

### **3.1 Strand U1: Understanding about Chemistry**

This strand focuses on the "power of models" and "limitations of models".1

* **Visual Challenge:** How to depict a "limitation"?  
* **BAML Extraction Strategy:** We define a BAML class ModelComparison. When the text discusses "evolution of scientific knowledge" 1, the system extracts pairs of models (e.g., "Plum Pudding" vs. "Rutherford").  
* **Fibo Prompting:** The prompt must request a "split-panel composition" or "evolutionary timeline."  
  * *Subject:* "Juxtaposition of Thomson's Plum Pudding atomic model and Rutherford's Nuclear model."  
  * *Style:* "Historical scientific illustration vs. modern schematic."  
  * *Context:* "Annotations showing the path of alpha particles."

This strand also mentions "science as a global enterprise".1 This informs the *demographic parameters* in the Fibo JSON. To reflect this learning outcome, the pipeline should randomly inject diverse scientist avatars into the prompts for laboratory scenes, ensuring the imagery reflects the global nature of the discipline as mandated by the syllabus.

### **3.2 Strand U2: Investigating in Chemistry**

This is the most critical strand for safety and accuracy in experimental imagery. The syllabus explicitly lists skills: "identifying potential sources of random and systematic error," "limits of precision," and "safe laboratory practice".1

| Syllabus Requirement | Visual Implication | BAML Extraction Logic | Fibo JSON Injection |
| :---- | :---- | :---- | :---- |
| **"Safe laboratory practice"** | Personal Protective Equipment (PPE) is mandatory. | if context \== "Experiment": enforce\_safety \= True | {"negative\_prompt": "no goggles, bare hands, loose hair, eating in lab", "subject\_modifier": "wearing safety glasses and lab coat"} |
| **"Precision and accuracy"** | Glassware must be specific (Volumetric vs. Conical). | extract\_glassware\_type() | {"subject": "Class A Volumetric Flask with graduation mark", "detail": "meniscus sitting on the line"} |
| **"Identify anomalous observations"** | Visuals of "errors" (e.g., parallax error). | if concept \== "Error": mode \= "educational\_bad\_example" | {"subject": "Eye level reading meniscus from above, illustrating parallax error", "style": "diagrammatic with annotation lines"} |

The "Investigating" strand also emphasizes "using SI units".1 While Fibo generates pixels, not text, the *visual context* can imply measurement. Prompts should include "digital balances displaying '0.00 g'" or "thermometers showing Kelvin scales" to reinforce the quantitative aspect of the curriculum.

### **3.3 Strand U3: Chemistry in Society**

This strand connects chemistry to "media-based arguments" and "impact on society".1

* **Visual Scope:** Beyond the lab. Images must depict "pharmaceutical plants," "renewable energy grids," and "environmental monitoring stations."  
* **BAML Context:** When the syllabus mentions "scientific discovery and invention" 1, the pipeline must link to specific historical figures or modern industrial applications.  
* **Fibo Application:** For a prompt about "Society influences scientific research," the system might generate a scene of a "public town hall meeting discussing water quality data," linking back to Strand 4.3 (Water Health).

## **4\. Strand 1: Nature of Matter \- Structured Visual Ontology**

Strand 1 deals with the fundamental building blocks. The visual requirements here are high on abstraction and geometric precision.

### **4.1 States of Matter and Kinetic Theory**

The syllabus outlines "solid, liquid, gas" and "diffusion".1

* **Dynamic Representation:** The syllabus mentions "Brownian motion."  
* **Fibo Prompt Strategy:** Since the output is static, the prompt must ask for *motion vectors*. "Microscopic view of pollen grains in water, motion blur trails indicating random movement, collisions with invisible water molecules."  
* **BAML Class:** ParticleSystem. Attributes: State (Solid/Liquid/Gas), Order (Regular/Random), Spacing (Touching/Apart).  
  * *Input:* "Solid."  
  * *JSON Output:* {"composition": "regular lattice", "particle\_proximity": "touching", "movement\_cues": "vibration lines"}.

### **4.2 Atomic Structure and Spectra**

The syllabus requires "identifying elements... from flame tests... Li, Na, K, Ba, Sr, Cu".1

* **Color Accuracy:** This is a hard constraint.  
  * Lithium \= Crimson/Red.  
  * Sodium \= Yellow.  
  * Potassium \= Lilac.  
  * Barium \= Green.  
  * Strontium \= Red.  
  * Copper \= Blue-Green.  
* **BAML Enumeration:** The BAML schema must strictly define these mappings. If the syllabus text says "Sodium," BAML outputs Color.Yellow.  
* **Fibo JSON:** {"subject": "Bunsen burner flame test", "flame\_color": "intense yellow", "element\_context": "Sodium salt on a platinum wire"}.

For "Atomic emission spectrum of hydrogen" 1, the syllabus mentions $E\_m \- E\_n \= hf$.

* **Visual:** A black background with discrete coloured lines.  
* **Prompt:** "Emission spectrum strip, black background, distinct lines: Red (656nm), Blue-green (486nm), Blue-violet (434nm), Violet (410nm)." The prompt includes the nanometer values to guide the model’s color accuracy, even if the text isn't rendered.

### **4.3 Electronic Structure: Orbitals**

The syllabus specifies "s, p, d sublevels, and shapes of s, p orbitals".1

* **Shape Definition:**  
  * s-orbital: Spherical.  
  * p-orbital: Dumbbell (orthogonal axes).  
* **BAML Structure:**  
  JSON  
  "OrbitalVisualization": {  
    "type": "p-orbital",  
    "axis": \["x", "y", "z"\],  
    "rendering\_style": "volumetric\_cloud"  
  }

* **Fibo Prompt:** "3D scientific render of a p-orbital, dumbbell shape aligned along the vertical z-axis, semi-transparent lobe surface, electron density gradient."

### **4.4 The Periodic Table and Trends**

"Trends in atomic radius... electronegativity".1

* **Comparative Visuals:** The prompt must generate *sets* of elements to show scale.  
* **Prompt:** "Side-by-side comparison of Lithium atom and Potassium atom. Potassium atom is significantly larger. Electron shells visible as concentric translucent spheres."  
* **BAML Logic:** The indexing pipeline needs a lookup table of atomic radii. When "Trend: Down a Group" is detected, it retrieves the relative sizes to inform the "Composition" field of the Fibo JSON.

## **5\. Strand 2: Behaviour of Matter \- Geometry and Organic Foundations**

This strand introduces molecular modelling and the first foray into organic chemistry.

### **5.1 Chemical Bonding**

"Continuum from ionic through polar covalent to pure covalent".1

* **Visual Metaphor:** Electron clouds.  
* **Ionic:** "Two separate spheres, one positive, one negative, no shared cloud."  
* **Polar Covalent:** "Two atoms, merged electron cloud distorted towards the larger atom (electronegativity), dipole indication."  
* **Pure Covalent:** "Symmetrical electron cloud sharing."  
* **BAML Extraction:** Detects the bond type based on the elements involved (using Pauling scale logic defined in the syllabus).

### **5.2 VSEPR Theory and Molecular Shapes**

"Predict and model the shapes of molecules... ABn... VSEPR theory".1

* **Geometry Constraints:**  
  * Linear (180°).  
  * Trigonal Planar (120°).  
  * Tetrahedral (109.5°).  
  * Pyramidal (107°).  
  * Bent (104.5°).  
* **Fibo JSON Construction:** The prompt cannot just say "Water molecule." It must say "Water molecule, Oxygen central atom, two Hydrogen atoms, Bent geometry, 104.5 degree bond angle, lone pairs visible as ghostly lobes."  
* **BAML Validation:** The extractor must parse the formula (e.g., $H\_2O$) and look up the geometry in the graph database to populate the "Geometry" field in the JSON.

### **5.3 Intermolecular Forces**

"Hydrogen bonding... physical properties... boiling points".1

* **Visualizing the Invisible:** Hydrogen bonds are often depicted as dashed lines.  
* **Prompt:** "Cluster of water molecules. Solid lines for O-H covalent bonds. Dotted lines for intermolecular Hydrogen bonds between O of one molecule and H of another. 3D schematic."

### **5.4 Hydrocarbons**

"Aliphatic... aromatic... structural isomers".1

* **Benzene:** The syllabus specifies "delocalised bonding".1  
* **Visual:** The classic hexagonal ring with the inner circle (or delocalized donut clouds above and below the plane).  
* **Prompt:** "Benzene molecule, hexagonal planar structure, pi-electron delocalization ring above and below the carbon plane."  
* **Isomerism:** For "Butane isomers," the prompt must request "Straight chain butane next to branched 2-methylpropane."

## **6\. Strand 3: Interactions of Matter \- Dynamic Visuals**

This strand deals with energy, rates, and equilibrium—concepts that involve change over time.

### **6.1 Thermochemistry**

"Exothermic and endothermic reactions... energy profile diagram".1

* **Graph Generation:** While AI struggles with text, it handles shapes well.  
* **Exothermic:** "Graph line starts high, small hump (Activation Energy), drops low (Product Energy). Energy released."  
* **Endothermic:** "Graph line starts low, large hump, ends high."  
* **BAML Logic:** Extract $\\Delta H$ sign (+/-). Map to curve shape description.

"Combustion of alcohols... spirit burners".1

* **Experimental Set-up:** "Copper calorimeter, spirit burner, thermometer, draft shield."  
* **Safety:** "Heatproof mat" is essential.  
* **Fibo Prompt:** "School chemistry experiment setup. Copper calorimeter can suspended over a spirit burner flame. Thermometer inserted in the can. Draft shield surrounding the flame to prevent heat loss."

### **6.2 Rates of Reaction**

"Collision theory... concentration... surface area".1

* **Particle Views:**  
  * *High Concentration:* "Crowded box of particles."  
  * *High Temperature:* "Particles with motion trails (speed)."  
  * *Catalyst:* "Surface with active sites, particles docking."  
* **Experiment:** "Decomposition of hydrogen peroxide."  
  * *Visual:* "Conical flask, rapid bubbling/fizzing, oxygen gas evolution."

### **6.3 Chemical Equilibrium**

"Le Châtelier's principle... Iron(III) chloride-potassium thiocyanate".1

* **Color Shift:** This is the defining feature.  
  * $Fe^{3+}$ (Yellow).  
  * $^{2+}$ (Blood Red).  
* **Prompt:** "Series of test tubes. Left: Pale yellow solution. Right: Deep blood-red solution. Center: Intermediate orange."  
* **Context:** The prompt must explicitly link the color intensity to the "position of equilibrium."

### **6.4 Acids, Bases, and pH**

"pH scale... indicators... titration curves".1

* **Titration Curves:** Strong Acid/Strong Base vs. Strong Acid/Weak Base.  
  * *Strong/Strong:* "Steep vertical section from pH 3 to 11."  
  * *Strong/Weak:* "Vertical section smaller, buffer region visible."  
* **Indicators:** Phenolphthalein (Clear to Pink). Methyl Orange (Red to Yellow).  
* **BAML Lookup:** The graph pipeline matches the "Titration Type" to the "Appropriate Indicator" and its "Color Transition."

### **6.5 Electrochemistry**

"Galvanic cell... Electrolytic cell... fuel cell".1

* **Distinction:**  
  * *Galvanic:* Two beakers, salt bridge, voltmeter (produces electricity).  
  * *Electrolytic:* One container, battery source, electrodes (consumes electricity).  
* **Fibo Prompt (Galvanic):** "Daniel Cell setup. Zinc electrode in zinc sulfate beaker. Copper electrode in copper sulfate beaker. U-tube salt bridge connecting them. Wires leading to a voltmeter."  
* **Fibo Prompt (Electrolytic):** "Hoffmann voltameter apparatus. Three vertical glass tubes. Platinum electrodes. Battery connected. Gas bubbles collecting at top."

## **7\. Strand 4: Matter in our World \- Complexity and Context**

This strand integrates the previous concepts into real-world and complex analytical scenarios.

### **7.1 Volumetric Analysis**

"Standard solutions... titration... ethanoic acid in vinegar".1

* **Glassware Precision:** The prompt must specify "Pipette filler," "White tile," "Conical flask."  
* **Color Specificity:** "Pale pink endpoint." A dark pink image indicates "overshot titration" and is pedagogically poor.  
* **Fibo Control:** Use a "Negative Prompt" for "dark pink, red liquid" to ensure the endpoint is depicted correctly as "faint persistent pink."

### **7.2 Organic Synthesis and Mechanisms**

"Preparation of an ester... reflux method".1

* **Apparatus:** "Round bottom flask," "Heating mantle" (naked flames are unsafe for organics\!), "Liebig condenser (vertical)."  
  * *Note:* Distillation has the condenser angled. Reflux has it vertical.  
  * *BAML Check:* ReactionType \== Reflux $\\rightarrow$ Condenser.Vertical.  
* **Mechanisms:** "Curved arrows... movement of electrons".1  
  * *Prompt:* "Chemical reaction diagram. Benzene ring. Curly arrow indicating electron movement from pi-bond to electrophile."

"Synthesis of benzoic acid... recrystallisation".1

* **Visual Texture:** "White crystalline solid," "Buchner funnel filtration."

### **7.3 Our Chemical Environment**

"Greenhouse effect... water treatment... eutrophication".1

* **Macro Scale:** These images differ from the lab scale.  
* **Water Treatment:** "Sedimentation tanks," "Sand filtration beds."  
* **Sustainability:** "Bioplastics," "Carbon Cycle diagrams."  
* **BAML Context:** Link "Water" LOs to "Sustainability" theme to generate images of "Clean water technology."

## **8\. Designing the Fibo JSON Schema**

The user requested "deep research into the fibo json prompting." Based on the requirements for high-fidelity, safe, and structured output, we propose the following schema architecture. This schema is the target output format for the BAML extraction layer.

### **8.1 The Schema Structure**

JSON

{  
  "fibo\_instruction\_set": {  
    "meta": {  
      "curriculum\_id": "Strand 3.3",  
      "target\_language": \["en", "ga"\],  
      "educational\_level": "Senior Cycle"  
    },  
    "core\_visual": {  
      "subject": "Iron(III) Thiocyanate Equilibrium System",  
      "main\_elements":,  
      "action\_state": "Static comparison"  
    },  
    "composition\_parameters": {  
      "viewpoint": "Front-facing eye-level",  
      "lighting": "Laboratory bright, neutral white balance",  
      "background": "Blurred chemistry lab bench",  
      "aspect\_ratio": "16:9"  
    },  
    "style\_modifiers": {  
      "medium": "Photorealistic educational asset",  
      "texture\_quality": "High fidelity liquids and glass",  
      "color\_grade": "Scientific accuracy (Standard CIE colors)"  
    },  
    "safety\_constraints": {  
      "ppe\_required": true,  
      "ventilation\_required": false,  
      "hazard\_symbols": \["Irritant"\]  
    },  
    "negative\_prompt\_payload": \[  
      "text overlays",  
      "incorrect glassware",  
      "unsafe handling",  
      "bare hands",  
      "cluttered background",  
      "artistic interpretation",  
      "neon colors"  
    \],  
    "bilingual\_metadata": {  
      "en\_label": "Effect of concentration on equilibrium",  
      "ga\_label": "Éifeacht na tiúchana ar chothromaíocht"  
    }  
  }  
}

### **8.2 Parameter Research & Justification**

* **subject**: Directly mapped from the BAML VisualEntity.  
* **main\_elements**: A list extracted from the BAML Components enum. This ensures no critical piece of equipment (e.g., the white tile in a titration) is missed.  
* **safety\_constraints**: Derived from Strand U2. If ppe\_required is true, the prompt compiler appends "scientist wearing goggles" to the generated string.  
* **bilingual\_metadata**: This is not sent to the image generator (which likely doesn't render text well) but is passed to the *frontend* indexing pipeline. This ensures the image is indexed under both "Equilibrium" and "Cothromaíocht."

## **9\. Bilingual Integration and Graph Pipelines**

The "bilingual" aspect of the project requires a sophisticated approach to data handling.

### **9.1 The Terminology Graph**

The Indexing Pipeline should utilize a specialized knowledge graph that maps English chemical terms to their Irish equivalents.

* **Source:** *An Gúm* (the body responsible for Irish language publications).  
* **Structure:**  
  * Node A: Concept("Acid")  
  * Property: en\_term: "Acid"  
  * Property: ga\_term: "Aigéad"  
  * Relationship: related\_to \-\> Concept("pH")

When BAML processes the syllabus (in English), it identifies the concept "Acid." The graph pipeline then retrieves the ga\_term ("Aigéad"). This allows the system to auto-generate Irish prompts or captions without needing a separate Irish syllabus parser.

### **9.2 Cultural Localization in Prompts**

The syllabus emphasizes "Chemistry in Society." For an Irish educational project, the images should reflect the local environment where possible.

* **Hard Water (Strand 2):** Prompt for "Limestone landscape, The Burren, Co. Clare."  
* **Energy (Strand 3):** Prompt for "Turlough Hill Pumped Storage Station" or "Peat briquettes burning."  
* **BAML Logic:** A "Localization" field in the BAML schema triggers the injection of these specific Irish landmarks into the Fibo prompt when relevant topics (Water, Fuels) arise.

## **10\. Step-by-Step Hackathon Implementation Strategy**

To execute this research effectively in a hackathon setting:

1. **Phase 1: Ingestion & Graph Construction (Hours 1-4)**  
   * Use pypdf to extract text from SC-Chemistry-Specification-EN.pdf.  
   * Set up a Neo4j or lightweight NetworkX graph.  
   * Initialize nodes for every "Learning Outcome" (e.g., "1.1", "1.2").  
2. **Phase 2: BAML Schema Definition (Hours 5-8)**  
   * Define the EduChemAsset class in BAML.  
   * Create Enums for Glassware, Colors, SafetyGear, VisualType.  
   * Write the extraction function that takes a Syllabus Chunk and returns an EduChemAsset object.  
3. **Phase 3: Fibo Adapter (Hours 9-12)**  
   * Write the Python logic that converts EduChemAsset into the Fibo\_JSON format described in Section 8\.  
   * Implement the "Safety Injection" logic (if acid in subject \-\> add goggles).  
4. **Phase 4: Bilingual Indexing (Hours 13-16)**  
   * Integrate a simple Dictionary/Lookup for key chemical terms.  
   * Ensure the JSON output includes the Irish labels for the frontend.  
5. **Phase 5: Generation & Quality Assurance (Hours 17-24)**  
   * Run the pipeline.  
   * Use a "Human-in-the-Loop" or VLM (Vision Language Model) check to verify: "Does this titration image show a white tile?"  
   * Refine the negative prompts based on initial hallucinations.

## **11\. Syllabus-Specific Data Tables for Prompt Engineering**

To facilitate immediate implementation, the following tables map specific syllabus sections to required prompt parameters.

### **11.1 Flame Tests (Strand 1.2)**

| Element | Symbol | Syllabus Color | RGB Approximation (for Prompt Tuning) |
| :---- | :---- | :---- | :---- |
| Lithium | Li | Crimson | (220, 20, 60\) \- Deep Red |
| Sodium | Na | Yellow | (255, 255, 0\) \- Intense Amber |
| Potassium | K | Lilac | (200, 162, 200\) \- Pale Purple |
| Barium | Ba | Green | (154, 205, 50\) \- Apple Green |
| Strontium | Sr | Red | (255, 0, 0\) \- Scarlet |
| Copper | Cu | Blue-Green | (64, 224, 208\) \- Turquoise |

*BAML Logic:* The BAML Enum must enforce these exact color descriptions. "Red" is insufficient for Lithium; "Crimson" is required to distinguish it from Strontium's "Red."

### **11.2 Organic Functional Groups (Strand 4.2)**

| Group | Syllabus Example | Visual Geometry (VSEPR) | Fibo Prompt Detail |
| :---- | :---- | :---- | :---- |
| Alkanes | Ethane | Tetrahedral carbons | "Zig-zag carbon backbone, 3D ball and stick" |
| Alkenes | Ethene | Planar double bond | "Trigonal planar geometry around C=C bond" |
| Aromatics | Benzene | Planar hexagonal | "Delocalised pi-ring, flat molecule" |
| Aldehydes | Ethanal | Carbonyl group at end | "C=O double bond at terminal position" |
| Ketones | Propanone | Carbonyl group in middle | "C=O double bond on central carbon" |
| Carboxylic Acids | Ethanoic Acid | \-COOH group | "Planar Carboxyl group, acidic hydrogen" |

### **11.3 Indicators and pH (Strand 4.1)**

| Indicator | Acid Color | Base Color | Transition Range | Prompt Context |
| :---- | :---- | :---- | :---- | :---- |
| Methyl Orange | Red | Yellow | 3.1 \- 4.4 | Strong Acid / Weak Base Titration |
| Phenolphthalein | Colorless | Pink | 8.3 \- 10.0 | Weak Acid / Strong Base Titration |
| Litmus | Red | Blue | 5.0 \- 8.0 | General acidity testing |

## **12\. Conclusion**

The "Deep Research" requested by the user reveals that successfully using Bria AI’s Fibo model for the Leaving Certificate Chemistry syllabus requires more than just creative prompting; it requires a robust **Semantic Infrastructure**. The syllabus is not a loose collection of topics but a rigid specification with defined colors, geometries, and safety protocols.  
By employing BAML to extract these specifications into a strongly typed graph structure, the project can mathematically guarantee that the inputs to the Fibo model are aligned with the curriculum. This "Schema-First" approach solves the twin problems of **Accuracy** (ensuring a tetrahedral carbon has 109.5° bond angles) and **Safety** (ensuring every experiment depicts appropriate PPE). Furthermore, the integration of a bilingual knowledge graph ensures that the visual assets are accessible to the Irish-medium education sector, fulfilling the project’s specific bilingual mandate. This architecture represents the state-of-the-art in educational content generation, moving from "generative art" to "generative curriculum."

#### **Works cited**

1. SC-Chemistry-Specification-EN.pdf