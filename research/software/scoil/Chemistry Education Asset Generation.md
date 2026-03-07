# **Digital Transformation of the Irish Chemistry Specification: A Comprehensive Technical Architecture for Next-Generation Educational Assets**

## **1\. The Pedagogical and Technological Landscape**

The modernization of the Irish Leaving Certificate Chemistry syllabus, particularly the specification set for introduction in September 2025, represents a fundamental shift in educational philosophy. Moving away from rote memorization, the new curriculum emphasizes scientific literacy, inquiry-based learning, and the ability to visualize and model complex chemical systems.1 This pedagogical evolution necessitates a parallel revolution in the digital infrastructure used to deliver educational content. Traditional static assets—textbook diagrams and passive video—are increasingly insufficient for conveying the dynamic, three-dimensional, and probabilistic nature of chemical phenomena.  
To address this, educational technologists must look beyond standard web design and embrace a "Chemical Metaverse" architecture. This involves the convergence of three powerful computational ecosystems: the **React** frontend for interactive user interfaces and WebGL rendering; the **Python** backend for rigorous scientific computation and procedural asset generation; and the **Hugging Face** ecosystem for leveraging generative artificial intelligence to automate and enhance visual asset creation.  
The challenge lies not merely in selecting tools but in orchestrating them into a cohesive pipeline that ensures scientific accuracy while maintaining high performance on the diverse range of devices found in Irish classrooms. This report provides an exhaustive analysis of the software packages, libraries, and architectural patterns required to build this next-generation learning environment. It explores alternatives to the standard react-three-fiber stack, identifies complementary tools for data visualization and 2D diagramming, and details how generative models can be constrained to produce textbook-quality illustrations.

### **1.1 The Syllabus as a Technical Specification**

To select the appropriate software architecture, one must first deconstruct the syllabus into technical requirements. The Irish Chemistry course is not a monolith; different topics demand distinct visualization strategies.

* **Atomic Structure & Bonding:** Requires volumetric rendering of electron orbitals (probability clouds), dynamic VSEPR models that respond to user interaction (repulsion simulation), and crystal lattice visualizations that handle thousands of instances efficiently.2  
* **Stoichiometry & Kinetics:** Demands real-time graphing of data streams (e.g., pH curves, reaction rates), simulations of laboratory apparatus (burettes, pipettes) with fluid dynamics approximations, and immediate feedback loops for calculation exercises.2  
* **Organic Chemistry:** Necessitates the handling of complex molecular graphs, the visualization of stereoisomerism (chirality), and the animation of reaction mechanisms (bond breaking/formation) which requires manipulating molecular topology in real-time.2  
* **Industrial & Environmental Chemistry:** Requires macroscopic schematic generation, flow charts of chemical plants (e.g., fractional distillation), and illustrative diagrams of water treatment processes.2

## **2\. The React Ecosystem: The Interactive Frontend Architecture**

React has established itself as the dominant library for building web user interfaces, but its utility in scientific visualization has been exponentially expanded by a rich ecosystem of graphics libraries. While react-three-fiber (R3F) is the standard for 3D integration, a holistic educational platform requires a suite of complementary tools for 2D structure drawing, data charting, and UI logic.

### **2.1 Core 3D Rendering: React-Three-Fiber (R3F) and Alternatives**

react-three-fiber acts as a reconciler for Three.js, translating the declarative component model of React into the imperative API of the WebGL engine. This architecture allows developers to treat chemical entities—atoms, bonds, beakers, sensors—as reusable, state-driven components.6

#### **2.1.1 The R3F Component Model in Chemistry**

The power of R3F lies in its ability to abstract complexity. A developer can create a \<Molecule /\> component that accepts a standard chemical identifier (like a SMILES string or CID) as a prop. Inside this component, R3F hooks like useLoader can fetch geometry data, while useFrame manages the render loop for animations such as molecular vibration or rotation.8  
For example, visualizing the *Trends in the Periodic Table* 2 requires rendering hundreds of atom models simultaneously. R3F supports "Instanced Mesh" rendering via the \<Instances\> component in @react-three/drei, which allows the GPU to draw thousands of identical geometries (e.g., carbon atoms in a diamond lattice) in a single draw call. This is critical for performance on low-end school hardware.10

#### **2.1.2 Complementary Packages in the R3F Ecosystem**

The R3F ecosystem is vast. For chemistry education, specific packages provide essential functionality:

* **@react-three/drei:** This library is indispensable. It provides \<Html\> for overlaying semantic HTML labels (like element symbols or charge density values) directly onto 3D objects, ensuring accessibility and SEO friendliness—a significant advantage over pure Canvas rendering. It also offers controls like \<OrbitControls\> and \<PresentationControls\>, allowing students to manipulate molecules intuitively.7  
* **@react-three/postprocessing:** Chemical reactions often involve visual cues like color changes or light emission (chemiluminescence). This library allows developers to apply bloom (glow) effects to specific meshes (e.g., a burning magnesium ribbon) or depth-of-field effects to focus the student's attention on a specific part of a large molecule.6  
* **react-spring / framer-motion-3d:** Physics-based animation libraries are superior to linear interpolation for demonstrating chemical concepts. When a student drags an atom to explore steric hindrance, the "spring" physics provided by these libraries allow the atom to bounce back into its equilibrium position naturally, reinforcing the concept of bond elasticity.6

### **2.2 Specialized Molecular Visualization Libraries**

While R3F offers limitless flexibility, building a molecular viewer from scratch is resource-intensive. Several "black box" React components exist specifically for chemistry, offering a trade-off between customization and ease of implementation.

#### **2.2.1 Molecule-3d-for-react**

This package is a React wrapper around the widely used 3dmol.js library. It is specifically designed to parse and render standard chemical file formats like PDB (Protein Data Bank) and SDF (Structure-Data File).4

* **Advantages:** It natively handles multiple visualization styles crucial for the *Biochemistry* and *Organic Chemistry* sections of the syllabus, including "Ball and Stick" (for showing connectivity), "CPK" (Space-filling for showing volume), and "Cartoon" (for protein secondary structure). It supports atom selection and labeling out-of-the-box.4  
* **Use Case:** Ideal for the "Families of Organic Compounds" section 2, where students need to rapidly switch between representations to understand the difference between a molecular formula and a 3D structure.

#### **2.2.2 Miew-React**

Based on the high-performance Miew viewer from EPAM, this library focuses on advanced crystallographic visualization.

* **Advantages:** It excels at rendering electron density maps and isosurfaces if provided with the correct volumetric data (e.g., .cube files). This is the most accurate way to visualize atomic orbitals (s, p, d) and molecular bonding orbitals (sigma, pi), moving beyond the simplified "balloon" diagrams often found in textbooks.13  
* **Use Case:** The "Chemical Bonding" and "Atomic Structure" sections 2, where understanding electron probability distributions is a key learning outcome.

#### **2.2.3 React-Chemdoodle**

While 3D is powerful, 2D representation remains the language of chemistry examinations. react-chemdoodle wraps the ChemDoodle Web Components, allowing for the rendering of high-quality 2D Lewis structures and skeletal diagrams.14

* **Advantages:** It includes a "Sketcher" component, which allows students to draw molecules interactively. This is essential for assessment tools where a student might be asked to "Draw the structure of Benzoic Acid".14  
* **Use Case:** Assessment modules and the "Organic Synthesis" section, where tracking functional group changes in 2D is often clearer than in 3D.

### **2.3 Data Visualization: Charting Libraries**

Chemistry is a quantitative science. The syllabus requires the analysis of rates of reaction, pH curves, and equilibrium constants. The React ecosystem offers several powerful charting libraries, each with different strengths.

#### **Table 1: Comparative Analysis of React Charting Libraries for Chemistry**

| Library | Architecture | Strengths for Chemistry | Weaknesses | Best Use Case in Syllabus |
| :---- | :---- | :---- | :---- | :---- |
| **Recharts** | D3-based, Component API | Declarative, easy to animate state changes (e.g., shifting equilibrium). Native SVG support ensures crisp rendering on retina displays.15 | High-level abstraction makes creating non-standard charts (e.g., Mass Spec deflection paths) difficult.16 | Titration curves (pH vs. Vol), Reaction Rate graphs. |
| **Visx (Airbnb)** | Low-level Primitives | "Unopinionated." Provides geometric primitives (lines, curves) to build custom visualizations. Integrates deeply with React state.15 | Steep learning curve. Requires building axes and scales manually.16 | Interactive "Energy Profile Diagrams" where students drag activation energy peaks. |
| **React-Google-Charts** | Wrapper for Google Charts | Extremely robust, wide variety of chart types. "Battle-tested" reliability.15 | Dependency on external Google API (online only). Limited styling customization compared to D3-based tools.15 | Simple bar charts for "Abundance of Isotopes" or analytical data. |
| **Nivo** | D3-based, Highly Styled | Beautiful defaults, supports server-side rendering (SSR). Excellent motion/transitions.16 | Large bundle size. Can be overkill for simple line graphs.16 | Interactive dashboards for "Environmental Chemistry" (water quality data). |
| **Victory** | Modular Components | Cross-platform (works on React Native). Opinionated but flexible.16 | Animation performance can lag with large datasets (thousands of points).16 | Mobile apps for revision/flashcards. |

### **2.4 Animation and Diagramming: Lottie and React-Flow**

Beyond 3D models and charts, explaining processes requires vector animation and flow diagrams.

* **Lottie-React:** This library renders animations exported from Adobe After Effects as JSON files. It is lightweight and resolution-independent.17  
  * **Application:** Ideal for "pre-baked" explanations of concepts that don't require user parameter manipulation, such as the *Water Treatment* process or the *Refining of Oil*. A Lottie animation can show the flow of water through filtration beds more clearly and with a smaller file size than a video.19  
* **React-Flow / React-Flow-Chart:** These libraries provide a node-based UI for building diagrams.15  
  * **Application:** Perfect for "Organic Synthesis Maps." Nodes can represent molecules (Ethanol), and edges can represent reagents (sodium dichromate). Students can interactively rearrange these nodes to map out reaction pathways, visualizing the convergence and divergence of organic families.15

## **3\. The Python Ecosystem: The Computational Engine**

While React handles the presentation, Python serves as the "brain" of the operation. It is responsible for generating accurate assets, performing complex chemical calculations, and automating the production of content.

### **3.1 Procedural Asset Generation: Blender and bpy**

Creating 3D models for the hundreds of molecules in the Leaving Cert syllabus by hand is inefficient and prone to error. Blender, an open-source 3D suite, includes a powerful Python API (bpy) that can automate this process.20

* **The Automation Pipeline:** A Python script can be written to:  
  1. Import molecular data (SMILES strings) using RDKit or Open Babel.  
  2. Generate 3D coordinates and bond topology.  
  3. Instantiate geometric primitives in Blender (spheres for atoms, cylinders for bonds) at these coordinates.22  
  4. Apply materials based on the CPK coloring standard (Carbon=Black, Oxygen=Red).  
  5. Optimize the mesh (decimation) to reduce polygon count for web performance.  
  6. Export the result as a .glb or .gltf file ready for React-Three-Fiber.22  
* **Batch Processing:** This allows for the generation of the entire syllabus's molecular library in minutes. If the syllabus changes or a new visualization style is required (e.g., switching from Ball-and-Stick to Space-Filling), the script is simply updated and re-run.24

### **3.2 Mathematical Animation: Manim**

Originally developed for the 3Blue1Brown YouTube channel, Manim is a Python engine for precise programmatic animation. Unlike standard animation software, Manim defines movement via code, ensuring mathematical accuracy.26

* **Application in Chemistry:**  
  * **Equilibrium:** Manim can simulate dynamic equilibrium by animating particles moving between two containers at rates defined by a probability function. The code ensures the visual representation matches the mathematical law of mass action.26  
  * **Stoichiometry:** The balancing of equations can be visualized by animating the rearrangement of terms and coefficients, synchronizing the mathematical notation with visual representations of the molecules.27  
* **Web Integration Challenges & Solutions:** Manim natively outputs video (MP4). However, the manim-web project leverages Pyodide (Python in WebAssembly) to attempt rendering Manim scenes directly in the browser. While this is cutting-edge and resource-intensive, it points toward a future where students can tweak the code of an animation (e.g., changing a reaction rate constant) and see the video re-render in real-time.28 For now, a hybrid approach—rendering Manim videos on the server via **FastAPI** and streaming them to the React frontend—is the most robust architecture.30

### **3.3 Cheminformatics: RDKit and Open Babel**

These libraries are the industry standard for chemical data manipulation. They are not visualization tools themselves but are the source of truth for all visualizations.5

* **RDKit:** Can generate 2D SVG diagrams of molecules, calculate molecular weight, identify isomers, and determining chirality. In an educational app, RDKit can be used on the backend (or client-side via WebAssembly) to validate student answers (e.g., checking if a drawn structure is indeed an isomer of the target molecule).5  
* **Open Babel:** A chemical toolbox designed to speak the many languages of chemical data. It is essential for converting between file formats (e.g., converting a .mol file from a textbook publisher into a .glb file for the web viewer).5

### **3.4 Scientific Visualization: PyVista**

PyVista is a Python library for 3D plotting and mesh analysis, built on top of VTK. It serves as a bridge between pure data and 3D visualization.32

* **Application:** It is particularly strong at visualizing volumetric data, such as electron density clouds or crystal lattice structures. PyVista can generate isosurfaces from quantum chemical calculations (e.g., from Gaussian or ORCA output files) and export these complex meshes to .gltf format for display in the React frontend.32 This allows for the visualization of "real" scientific data rather than just artistic approximations.

## **4\. Hugging Face and Generative AI: The Visual Synthesizer**

The Hugging Face ecosystem provides access to state-of-the-art generative models. For chemistry education, the goal is not just "image generation" but "diagram synthesis"—creating accurate, labelled, and pedagogically sound illustrations.

### **4.1 The Accuracy Challenge**

Generic text-to-image models (like vanilla Stable Diffusion) struggle with scientific accuracy. They often "hallucinate" incorrect glassware connections, physically impossible bonds, or nonsensical text. To be useful in education, these models must be rigorously constrained.33

### **4.2 ControlNet: Enforcing Geometric Constraints**

ControlNet is a neural network structure that allows users to add spatial conditioning to Stable Diffusion. This is the key to generating usable scientific diagrams.34

* **Canny Edge & Depth Maps:** A developer can create a simplified, blocky 3D model of an experimental setup (e.g., a titration) in Blender. This low-fidelity model acts as the "structure." ControlNet (using Canny or Depth preprocessors) then guides Stable Diffusion to "paint" a high-fidelity, stylized illustration over this structure. The result preserves the correct geometry (the burette is exactly vertical, the clamp is in the right place) while providing the aesthetic quality of a professional illustration.36  
* **Scribble:** Educators can sketch a rough diagram of a process (e.g., the Haber Process) on a digital whiteboard. ControlNet's "Scribble" adapter can transform this rough sketch into a polished diagram in real-time, maintaining the teacher's original layout but enhancing clarity.36

### **4.3 LoRA (Low-Rank Adaptation): Defining a Visual Style**

To create a cohesive "textbook" feel, the AI needs to learn a specific visual style. LoRA allows for the fine-tuning of large models on small datasets without retraining the entire network.37

* **Schematics LoRA:** There are community-trained LoRAs (available on platforms like Civitai and Hugging Face) specifically for "Schematics," "Technical Drawing," or "Line Art".39 Integrating these into the pipeline ensures that generated images are clean, black-and-white, and suitable for printing in exam papers or worksheets.  
* **Custom Training:** A developer could train a custom LoRA on a dataset of existing Leaving Certificate diagrams. This would create a model that generates new content in the exact visual language that Irish students are already trained to recognize, reducing cognitive load.41

### **4.4 Specific Models and Tools**

* **Stable Diffusion XL (SDXL):** Currently the state-of-the-art for open-weights image generation. Its larger parameter count allows for better adherence to complex prompts involving multiple objects (e.g., "a beaker, a flask, and a bunsen burner").43  
* **Animagine XL:** While designed for anime, this model and its associated LoRAs (like "Sketch Style") are surprisingly effective at producing clean, bold line art suitable for educational diagrams.43  
* **Nougat:** A Transformer model from Meta (hosted on Hugging Face) designed for understanding scientific documents. It can convert images of textbook pages into Markdown/LaTeX. This is useful for digitizing legacy chemistry resources and making them accessible/searchable.44

## **5\. Integration Architectures and Workflows**

How do these disparate tools come together? We propose two primary architectural patterns.

### **5.1 The "Pre-Baked" Pipeline (Asset Generation)**

This workflow is used to create static or semi-static assets (3D models, videos, diagrams) that are stored and served to the user.

1. **Data Ingestion:** A CSV file containing molecular data (SMILES) and syllabus topics is read by a Python script.  
2. **Processing:**  
   * RDKit generates molecular structures.  
   * Blender (via bpy) generates 3D glTF models.  
   * Manim renders video explanations of mechanisms.  
   * Stable Diffusion (via Hugging Face Inference API) \+ ControlNet generates illustrative diagrams.  
3. **Optimization:** Assets are compressed (Draco compression for glTF, Handbrake for video).  
4. **Delivery:** Assets are hosted on a CDN and fetched by the React application. R3F renders the molecules; React components display the diagrams and videos.

### **5.2 The "Live Compute" Pipeline (Interactive Simulation)**

This workflow allows for real-time interaction and calculation on the client side.

1. **Client-Side Python (Pyodide):** The React application loads the Pyodide WASM runtime. This brings the Python scientific stack (numpy, scipy, pandas) to the browser.45  
2. **Interactive Logic:**  
   * When a student performs a virtual titration in the R3F scene (adding volume), the React state updates.  
   * React sends the volume and concentration data to the Pyodide worker.  
   * Pyodide uses scipy.optimize to calculate the exact pH based on the dissociation constant ($K\_a$) of the specific acid (e.g., Ethanoic Acid).  
   * The result is returned to React, which updates the Recharts graph and the color of the liquid in the 3D beaker (simulating indicator change).  
3. **Advantages:** This approach removes the need for round-trips to a server, providing instant feedback. It also enables "Sandboxed Coding," where advanced students can write their own Python scripts to analyze data within the browser, fostering the digital literacy required by the new syllabus.46

### **5.3 Server-Side Streaming (FastAPI \+ SSE)**

For heavy computations (e.g., running a large Manim render or a heavy GenAI inference), client-side execution is insufficient.

* **FastAPI:** A lightweight, async Python web server acts as the backend. It exposes endpoints for asset generation.48  
* **Server-Sent Events (SSE):** If a student requests a custom diagram or a complex simulation, FastAPI initiates the process and streams the result (or progress updates) back to the React frontend using SSE. This provides a responsive UX even for long-running tasks.50

## **6\. Implementation Scenarios for Specific Syllabus Topics**

To demonstrate the practical application of this architecture, we map it to three specific "Mandatory Experiments" and topics from the Leaving Cert syllabus.

### **6.1 Scenario A: Volumetric Analysis (Titration)**

* **Concept:** Determining the concentration of Ethanoic Acid in Vinegar.  
* **Frontend (React/R3F):** A 3D scene containing a burette, conical flask, and white tile. The user interacts via a slider to control the tap. The \<Html\> component overlays a digital pH meter reading.  
* **Visualization (Recharts):** A dynamic line chart plots pH vs. Volume Added. As the user adds base, the chart updates in real-time.  
* **Computation (Pyodide):** scipy calculates the pH curve on the fly, handling the buffer region calculations accurately.  
* **GenAI (Hugging Face):** ControlNet is used to generate a "Help" overlay showing a schematic diagram of how to read the meniscus at eye level, ensuring correct technique is reinforced.2

### **6.2 Scenario B: Organic Families and Isomerism**

* **Concept:** Structural isomers of $C\_4H\_{10}$ (Butane and 2-methylpropane).  
* **Frontend (Molecule-3d-for-react):** Two side-by-side viewers allow students to compare the molecules. Students can rotate and zoom to see that they are distinct structures.  
* **Backend (RDKit/Blender):** The 3D models are generated automatically from SMILES strings. RDKit calculates the boiling points of each isomer.  
* **Data Vis (React-Google-Charts):** A bar chart compares the boiling points, illustrating how branching reduces Van der Waals forces (a key syllabus concept).2

### **6.3 Scenario C: Rates of Reaction**

* **Concept:** Decomposition of Hydrogen Peroxide using a Catalyst.  
* **Frontend (R3F \+ React-Spring):** A particle simulation showing molecules colliding. A "Temperature" slider increases the velocity of the particles (using React-Spring for smooth transitions).  
* **Animation (Manim):** A pre-rendered Manim video explains the concept of "Activation Energy" and how the catalyst lowers it, using precise mathematical graphing.2  
* **GenAI:** Stable Diffusion generates realistic textures for the "Manganese Dioxide" catalyst (black powder) to be applied to the 3D model, replacing generic grey materials with photorealistic textures.52

## **7\. Challenges and Mitigations**

### **7.1 Asset Size and Loading Times**

High-fidelity 3D models and textures can be heavy.

* **Solution:** Use **Draco Compression** for glTF files (reducing size by up to 90%) and lazy-loading via React's Suspense API. Use Lottie animations for 2D concepts instead of video where possible.19

### **7.2 Browser Compatibility**

WebGL support is universal, but performance varies.

* **Solution:** Implement "Quality Toggles" in the React UI. Low quality disables post-processing (Bloom, DOF) and lowers shadow resolution. High quality enables all R3F effects.

### **7.3 Scientific Accuracy of GenAI**

AI hallucinations are a risk.

* **Solution:** Never use raw text-to-image for assessment diagrams. Always use **ControlNet** with a geometric ground truth, or use GenAI only for "contextual" imagery (e.g., a background image of a chemical plant) rather than technical diagrams.35

## **Conclusion**

The convergence of **React-Three-Fiber** for interaction, **Python** (Manim, Blender, RDKit) for rigorous generation, and **Hugging Face** (ControlNet, LoRA) for creative illustration offers a robust framework for revitalizing the Irish Chemistry syllabus. This architecture moves beyond the limitations of static media, offering students an immersive, scientifically accurate, and scalable learning environment. By automating asset creation and leveraging client-side computation, educators can deliver a "Chemical Metaverse" that fosters the deep conceptual understanding required by the 2025 specification. This is not merely a technological upgrade; it is a pedagogical necessity for the next generation of scientists.

#### **Works cited**

1. Chemistry \- Curriculum Online, accessed December 12, 2025, [https://curriculumonline.ie/senior-cycle/senior-cycle-subjects/chemistry/](https://curriculumonline.ie/senior-cycle/senior-cycle-subjects/chemistry/)  
2. The entire Leaving Cert Chemistry Course animated, accessed December 12, 2025, [https://www.revisionlab.ie/leaving-cert-chemistry-course](https://www.revisionlab.ie/leaving-cert-chemistry-course)  
3. Leaving Cert Chemistry \- Study Notes & Exam Papers | SimpleStudy Ireland, accessed December 12, 2025, [https://simplestudy.com/ie/leaving-cert/chemistry](https://simplestudy.com/ie/leaving-cert/chemistry)  
4. Autodesk/molecule-3d-for-react: 3D molecular visualization ... \- GitHub, accessed December 12, 2025, [https://github.com/Autodesk/molecule-3d-for-react](https://github.com/Autodesk/molecule-3d-for-react)  
5. Biology and Chemistry | Python across all Disciplines, accessed December 12, 2025, [https://docs.pyclubs.org/python-across-all-disciplines/disciplines/biology-and-chemistry](https://docs.pyclubs.org/python-across-all-disciplines/disciplines/biology-and-chemistry)  
6. React Three Fiber: Introduction, accessed December 12, 2025, [https://r3f.docs.pmnd.rs/](https://r3f.docs.pmnd.rs/)  
7. pmndrs/react-three-fiber: A React renderer for Three.js \- GitHub, accessed December 12, 2025, [https://github.com/pmndrs/react-three-fiber](https://github.com/pmndrs/react-three-fiber)  
8. Tutorial: Use react-three-fiber to render 3D in-browser visuals \- The Software House, accessed December 12, 2025, [https://tsh.io/blog/react-three-fiber](https://tsh.io/blog/react-three-fiber)  
9. Your first scene \- React Three Fiber, accessed December 12, 2025, [https://r3f.docs.pmnd.rs/getting-started/your-first-scene](https://r3f.docs.pmnd.rs/getting-started/your-first-scene)  
10. 3D Data Visualization with React and Three.js | by Peter Beshai | Cortico \- Medium, accessed December 12, 2025, [https://medium.com/cortico/3d-data-visualization-with-react-and-three-js-7272fb6de432](https://medium.com/cortico/3d-data-visualization-with-react-and-three-js-7272fb6de432)  
11. React-Three-Drei, accessed December 12, 2025, [https://drei.docs.pmnd.rs/](https://drei.docs.pmnd.rs/)  
12. Examples \- React Three Fiber, accessed December 12, 2025, [https://r3f.docs.pmnd.rs/getting-started/examples](https://r3f.docs.pmnd.rs/getting-started/examples)  
13. epam/miew: 3D Molecular Viewer \- GitHub, accessed December 12, 2025, [https://github.com/epam/miew](https://github.com/epam/miew)  
14. Tutorial \> React Development \- ChemDoodle Web Components, accessed December 12, 2025, [https://web.chemdoodle.com/tutorial/advanced/reactjs-development](https://web.chemdoodle.com/tutorial/advanced/reactjs-development)  
15. The top 11 React chart libraries for data visualization \- Ably, accessed December 12, 2025, [https://ably.com/blog/top-react-chart-libraries](https://ably.com/blog/top-react-chart-libraries)  
16. Ten React graph visualization libraries to consider in 2024 \- DEV Community, accessed December 12, 2025, [https://dev.to/ably/top-react-graph-visualization-libraries-3gmn](https://dev.to/ably/top-react-graph-visualization-libraries-3gmn)  
17. Lazy Load Lottie animation in React | by Alon Mizrahi \- Medium, accessed December 12, 2025, [https://medium.com/@alonmiz1234/lazy-load-lottie-animation-in-react-e58e67e2aa74](https://medium.com/@alonmiz1234/lazy-load-lottie-animation-in-react-e58e67e2aa74)  
18. airbnb/lottie-web: Render After Effects animations natively on Web, Android and iOS, and React Native. http://airbnb.io/lottie \- GitHub, accessed December 12, 2025, [https://github.com/airbnb/lottie-web](https://github.com/airbnb/lottie-web)  
19. Adding web animations to your React project using Lottie. \- DEV Community, accessed December 12, 2025, [https://dev.to/ugglr/adding-web-animations-to-your-react-project-using-lottie-18fo](https://dev.to/ugglr/adding-web-animations-to-your-react-project-using-lottie-18fo)  
20. export-a-blender-file-to-glb-from-the-command-line.md \- GitHub, accessed December 12, 2025, [https://github.com/jakelazaroff/til/blob/main/blender/export-a-blender-file-to-glb-from-the-command-line.md](https://github.com/jakelazaroff/til/blob/main/blender/export-a-blender-file-to-glb-from-the-command-line.md)  
21. glTF-Tutorials/BlenderGltfConverter/README.md at main \- GitHub, accessed December 12, 2025, [https://github.com/KhronosGroup/glTF-Tutorials/blob/main/BlenderGltfConverter/README.md](https://github.com/KhronosGroup/glTF-Tutorials/blob/main/BlenderGltfConverter/README.md)  
22. New python script: Molecules (pdb-format) to Blender, accessed December 12, 2025, [https://blenderartists.org/t/new-python-script-molecules-pdb-format-to-blender/361379](https://blenderartists.org/t/new-python-script-molecules-pdb-format-to-blender/361379)  
23. glTF 2.0 \- Blender 5.0 Manual \- Blender Documentation, accessed December 12, 2025, [https://docs.blender.org/manual/en/latest/addons/import\_export/scene\_gltf2.html](https://docs.blender.org/manual/en/latest/addons/import_export/scene_gltf2.html)  
24. gLTF batch export \- Python API \- Developer Forum \- Blender Devtalk, accessed December 12, 2025, [https://devtalk.blender.org/t/gltf-batch-export/16618](https://devtalk.blender.org/t/gltf-batch-export/16618)  
25. Batch exporting scene collections or selected objects using glTF-Blender-IO, accessed December 12, 2025, [https://stackoverflow.com/questions/60431827/batch-exporting-scene-collections-or-selected-objects-using-gltf-blender-io](https://stackoverflow.com/questions/60431827/batch-exporting-scene-collections-or-selected-objects-using-gltf-blender-io)  
26. Manim's Output Settings, accessed December 12, 2025, [https://docs.manim.community/en/stable/tutorials/output\_and\_config.html](https://docs.manim.community/en/stable/tutorials/output_and_config.html)  
27. Using Manim For Making UI Animations \- Smashing Magazine, accessed December 12, 2025, [https://www.smashingmagazine.com/2025/04/using-manim-making-ui-animations/](https://www.smashingmagazine.com/2025/04/using-manim-making-ui-animations/)  
28. manim-web \- PyPI, accessed December 12, 2025, [https://pypi.org/project/manim-web/0.1.8/](https://pypi.org/project/manim-web/0.1.8/)  
29. Manim Web: A fork of ManimCE using Pyodide to deliver math animations for the browser, accessed December 12, 2025, [https://www.reddit.com/r/manim/comments/1lixo8r/manim\_web\_a\_fork\_of\_manimce\_using\_pyodide\_to/](https://www.reddit.com/r/manim/comments/1lixo8r/manim_web_a_fork_of_manimce_using_pyodide_to/)  
30. Quickstart \- Manim Community v0.19.1, accessed December 12, 2025, [https://docs.manim.community/en/stable/tutorials/quickstart.html](https://docs.manim.community/en/stable/tutorials/quickstart.html)  
31. Deploying Manim on Render with FastAPI – Advice? \- Reddit, accessed December 12, 2025, [https://www.reddit.com/r/manim/comments/1ob6xxr/deploying\_manim\_on\_render\_with\_fastapi\_advice/](https://www.reddit.com/r/manim/comments/1ob6xxr/deploying_manim_on_render_with_fastapi_advice/)  
32. The PyVista Project, accessed December 12, 2025, [https://pyvista.org/](https://pyvista.org/)  
33. Daily Papers \- Hugging Face, accessed December 12, 2025, [https://huggingface.co/papers?q=Generative%20models](https://huggingface.co/papers?q=Generative+models)  
34. Generated Images with ControlNet. | Download Scientific Diagram \- ResearchGate, accessed December 12, 2025, [https://www.researchgate.net/figure/Generated-Images-with-ControlNet\_fig9\_388931407](https://www.researchgate.net/figure/Generated-Images-with-ControlNet_fig9_388931407)  
35. ControlNet: A Complete Guide \- Stable Diffusion Art, accessed December 12, 2025, [https://stable-diffusion-art.com/controlnet/](https://stable-diffusion-art.com/controlnet/)  
36. The Ultimate Guide to ControlNet (Part 1\) \- Civitai Education Hub, accessed December 12, 2025, [https://education.civitai.com/civitai-guide-to-controlnet/](https://education.civitai.com/civitai-guide-to-controlnet/)  
37. LoRA \- Hugging Face, accessed December 12, 2025, [https://huggingface.co/docs/peft/developer\_guides/lora](https://huggingface.co/docs/peft/developer_guides/lora)  
38. Resource Types \- Civitai Education, accessed December 12, 2025, [https://education.civitai.com/civitais-guide-to-resource-types/](https://education.civitai.com/civitais-guide-to-resource-types/)  
39. Schematics LORA (SD 1.5) released on CivitAI : r/StableDiffusion \- Reddit, accessed December 12, 2025, [https://www.reddit.com/r/StableDiffusion/comments/16bgiyy/schematics\_lora\_sd\_15\_released\_on\_civitai/](https://www.reddit.com/r/StableDiffusion/comments/16bgiyy/schematics_lora_sd_15_released_on_civitai/)  
40. Nishitbaria/Drawing-Art-Lora \- Hugging Face, accessed December 12, 2025, [https://huggingface.co/Nishitbaria/Drawing-Art-Lora](https://huggingface.co/Nishitbaria/Drawing-Art-Lora)  
41. Civitai's LoRA Trainer: Simplifying Model Training for All, accessed December 12, 2025, [https://education.civitai.com/using-civitai-the-on-site-lora-trainer/](https://education.civitai.com/using-civitai-the-on-site-lora-trainer/)  
42. I made an in-depth LoRA training guide. It uses the CivitAI online trainer. While the focus is Flux it works for Stable Diffusion LoRA training as well. \- Reddit, accessed December 12, 2025, [https://www.reddit.com/r/StableDiffusion/comments/1fhwuuo/i\_made\_an\_indepth\_lora\_training\_guide\_it\_uses\_the/](https://www.reddit.com/r/StableDiffusion/comments/1fhwuuo/i_made_an_indepth_lora_training_guide_it_uses_the/)  
43. Linaqruf/sketch-style-xl-lora \- Hugging Face, accessed December 12, 2025, [https://huggingface.co/Linaqruf/sketch-style-xl-lora](https://huggingface.co/Linaqruf/sketch-style-xl-lora)  
44. Nougat \- Hugging Face, accessed December 12, 2025, [https://huggingface.co/docs/transformers/model\_doc/nougat](https://huggingface.co/docs/transformers/model_doc/nougat)  
45. Pyodide Python compatibility — Version 0.29.0, accessed December 12, 2025, [https://pyodide.org/en/stable/usage/wasm-constraints.html](https://pyodide.org/en/stable/usage/wasm-constraints.html)  
46. Feasibility, Use Cases, and Limitations of Pyodide \- Microsoft for Python Developers Blog, accessed December 12, 2025, [https://devblogs.microsoft.com/python/feasibility-use-cases-and-limitations-of-pyodide/](https://devblogs.microsoft.com/python/feasibility-use-cases-and-limitations-of-pyodide/)  
47. Using Python inside a React web app with Pyodide \- Adam Emery, accessed December 12, 2025, [https://adamemery.dev/articles/pyodide-react](https://adamemery.dev/articles/pyodide-react)  
48. Developing a Single Page App with FastAPI and React | TestDriven.io, accessed December 12, 2025, [https://testdriven.io/blog/fastapi-react/](https://testdriven.io/blog/fastapi-react/)  
49. How to Serve a React App with FastAPI Using Static Files \- Deep Learning Nerds, accessed December 12, 2025, [https://www.deeplearningnerds.com/how-to-serve-a-react-app-with-fastapi-using-static-files/](https://www.deeplearningnerds.com/how-to-serve-a-react-app-with-fastapi-using-static-files/)  
50. Stream OpenAI with FastAPI and Consuming it with React.js | by Huan Xu \- Medium, accessed December 12, 2025, [https://medium.com/@hxu296/serving-openai-stream-with-fastapi-and-consuming-with-react-js-part-1-8d482eb89702](https://medium.com/@hxu296/serving-openai-stream-with-fastapi-and-consuming-with-react-js-part-1-8d482eb89702)  
51. How to use Server-Sent Events with FastAPI, React, and Langgraph \- Softgrade, accessed December 12, 2025, [https://www.softgrade.org/sse-with-fastapi-react-langgraph/](https://www.softgrade.org/sse-with-fastapi-react-langgraph/)  
52. The Stable Diffusion Guide \- Hugging Face, accessed December 12, 2025, [https://huggingface.co/docs/diffusers/v0.14.0/stable\_diffusion](https://huggingface.co/docs/diffusers/v0.14.0/stable_diffusion)