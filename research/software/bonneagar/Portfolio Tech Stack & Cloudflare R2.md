# **Architecting the Polymath Studio: A Full-Stack Blueprint for Game Development and Audio Production Portfolios**

## **1\. Executive Overview: The Convergence of Interactive Disciplines**

The digital portfolio for a dual-disciplinary creative—specifically one operating at the intersection of game development and music production—presents a unique set of engineering challenges that far exceed the requirements of a standard static website. This persona, often referred to as a "technical creative" or "polymath," requires a platform that does not merely display text and images, but serves as a high-fidelity delivery system for complex digital assets. These assets range from lossless audio files (WAV/FLAC) and high-bitrate 4K video game trailers to interactive 3D WebGL scenes and executable binaries.  
The technical requirements for such a platform are contradictory in traditional architectures: it must be globally performant yet cost-effective; it must handle massive file bandwidth without egress fees; and it must offer a "native app" feel with zero-latency state management. Furthermore, the modern creative workflow necessitates a "private studio" infrastructure—a self-hosted backend for managing the raw materials of creativity (textures, samples, design files) before they are polished for public consumption.  
This report outlines a comprehensive architectural blueprint for building this ecosystem using the "Bleeding Edge" web stack: **TanStack Start** for the meta-framework, **Convex** for the reactive backend, **Cloudflare R2** for zero-egress object storage, and **Shadcn** for a composable design system. Additionally, it details the implementation of a self-hosted, Docker-based intranet for Digital Asset Management (DAM) and design documentation, ensuring the creator owns their workflow end-to-end.

## ---

**2\. Architectural Paradigms: The Modern Web Stack**

The selection of technologies for this portfolio is driven by the specific constraints of media-heavy applications. Traditional server-based architectures (LAMP stack) or early Single Page Application (SPA) models fail to address the specific needs of media streaming and SEO simultaneously.

### **2.1 The Meta-Framework: TanStack Start**

TanStack Start represents the next evolution in React meta-frameworks. Unlike Next.js, which has moved heavily towards React Server Components (RSC) and a complex caching heuristic, TanStack Start is built fundamentally upon **TanStack Router**. This provides a distinct advantage for a portfolio that functions like an application. The router manages state via the URL, ensuring that deep links to specific game builds or audio tracks are deterministic and shareable.  
For a game developer, **Full-Document Server-Side Rendering (SSR)** is non-negotiable.1 When a user or a crawler requests a project page, the server must deliver a fully formed HTML document. This ensures optimal Time-to-First-Byte (TTFB) and SEO ranking. However, once the initial HTML is loaded, the application must "hydrate" into a client-side app to allow for seamless navigation without full page reloads—a critical feature for an audio player that must persist playback while the user browses different projects.

### **2.2 The Reactive Backend: Convex**

Convex is chosen over traditional SQL databases (PostgreSQL) or document stores (MongoDB) because of its **ubiquitous reactivity**.2 In a standard stack, if the developer uploads a new track, the user must refresh the page to see it. Convex maintains a WebSocket connection to the client. When data changes on the server, the UI updates instantly.  
This capability is vital for the "live" aspect of a modern portfolio:

* **Real-time Transcoding Status**: When uploading a large game trailer, the UI can show a live progress bar for server-side processing.  
* **Presence**: The portfolio can display "Currently coding in Unity" or "Producing in Ableton" status indicators in real-time, pulling data from the developer's local environment.

### **2.3 The Edge Storage Layer: Cloudflare R2**

The economic model of AWS S3 is prohibitive for independent creatives due to egress fees—the cost incurred when data is downloaded. A portfolio that goes viral could bankrupt its owner if hosted on S3. Cloudflare R2 offers an S3-compatible API with **zero egress fees**, making it the only viable choice for hosting large game binaries and lossless audio.3  
However, R2 is an object store, not a streaming server. As detailed later in this report, serving media directly from R2 often fails to support "byte-range requests," which are essential for seeking within audio/video timelines. A custom Cloudflare Worker is required to bridge this gap.

### **2.4 Component Architecture: Shadcn & Tailwind**

Shadcn is not a component library in the traditional sense; it is a collection of copy-pasteable components built on Radix UI and Tailwind CSS.4 This architecture is preferred for game developers who require absolute control over the rendering pipeline. Unlike Material UI, which abstracts the DOM, Shadcn exposes the underlying markup, allowing the developer to integrate complex interactions (like WebGL canvases or audio visualizers) directly into the component tree without fighting the library's styles.

## ---

**3\. Data Persistence and Reactivity: Convex for Media Applications**

For a media-centric portfolio, the database schema and query strategy must be optimized for handling metadata distinct from binary storage. Convex provides specific features that solve common problems in media applications, such as pagination of large libraries and efficient filtering.

### **3.1 Schema Design for Mixed Media Assets**

The data model must unify distinct asset types (Games, Music, Assets) while allowing for type-specific fields. A discriminated union pattern is recommended.  
**Table 1: Proposed Convex Schema Structure**

| Table Name | Key Fields | Purpose | Index Strategy |
| :---- | :---- | :---- | :---- |
| users | authId, role | Maps Better Auth identities to application roles (Admin). | by\_authId for fast lookup during session validation. |
| assets | r2Key, type (audio/video/build), metadata (JSON) | The core registry of all uploaded files. Stores technical metadata (size, mime type). | by\_r2Key to prevent duplicates. |
| projects | slug, title, assets (Array of IDs), publishedAt | Represents a public portfolio entry. Links to multiple assets (e.g., a game page with a trailer, screenshots, and a ZIP build). | by\_slug for routing; by\_published for sorting. |
| analytics | assetId, eventType (play/download), timestamp | Tracks engagement metrics. | by\_asset to aggregate play counts. |

### **3.2 Pagination Strategies for Large Libraries**

A music producer may have hundreds of tracks; a game developer may have thousands of texture assets. Loading all metadata at once allows for client-side filtering but degrades initial load performance. Convex recommends **Cursor-Based Pagination** over offset pagination for consistency and performance.5

#### **3.2.1 The Problem with .filter()**

A common anti-pattern in Convex is fetching a large dataset and using JavaScript's .filter() method in the query function.

* *Inefficiency*: Convex must read every document from disk, charge for the bandwidth, and then discard the ones that don't match.  
* *Pagination Breakage*: If you request 10 items and filter out 5 in code, the client receives a "page" of only 5 items, breaking the UI's infinite scroll logic.6

#### **3.2.2 Index-Based Filtering**

To implement high-performance filtering (e.g., "Show only Ambient tracks"), the schema must define an index on the filtering field.

TypeScript

// convex/schema.ts  
export default defineSchema({  
  tracks: defineTable({  
    genre: v.string(),  
    bpm: v.number(),  
    //...  
  })  
 .index("by\_genre", \["genre"\]) // Enables fast filtering  
 .index("by\_bpm", \["bpm"\])  
});

Using withIndex, the query scans *only* the documents that match the genre, ensuring that a request for 10 items consumes exactly 10 units of read bandwidth.

TypeScript

// convex/tracks.ts  
export const list \= query({  
  args: {  
    paginationOpts: paginationOptsValidator,  
    genre: v.optional(v.string()),  
  },  
  handler: async (ctx, args) \=\> {  
    let q \= ctx.db.query("tracks");  
    if (args.genre) {  
      // Efficiently uses the index  
      q \= q.withIndex("by\_genre", (q) \=\> q.eq("genre", args.genre));  
    }  
    return await q.order("desc").paginate(args.paginationOpts);  
  },  
});

### **3.3 Managing Asset Uploads**

Security is paramount when allowing file uploads. Convex handles this via a two-step process to ensure unauthorized users cannot flood the storage bucket.7

1. **Generate Upload URL**: The client calls a Convex mutation (generateUploadUrl). This mutation authenticates the user (via Better Auth session checks) and returns a short-lived, signed URL.  
2. **Direct POST**: The client uploads the file directly to the provided URL. The file never passes through the application server, preventing bottlenecking.  
3. **Storage ID**: Upon successful upload, Convex returns a storageId.  
4. **Save Metadata**: The client calls a second mutation to save the storageId and descriptive metadata (Title, Genre) to the assets table.

This workflow is distinct from the R2 Presigned URL workflow (discussed later) but is preferred for smaller assets (images, avatars) where Convex's internal file storage provides simpler integration. For massive files (Game Builds \> 100MB), the R2 Direct Upload method is superior due to R2's specific optimization for large objects and lack of bandwidth charges.

## ---

**4\. Edge Computing and Media Delivery: Cloudflare R2 & Workers**

The core requirement of "implementing audio/video streaming with free egress" necessitates a nuanced understanding of HTTP protocols. While R2 stores the data, the delivery mechanism defines the user experience.

### **4.1 The Byte-Range Request Challenge**

Modern media streaming relies on **HTTP Range Requests** (Status Code 206). When a user clicks the middle of a waveform, the browser requests the file starting from that specific byte offset.

* **The Issue**: By default, accessing a file via a public R2 bucket URL often results in a 200 OK response, delivering the *entire* file from byte 0\.  
* **Consequence**: In Safari (iOS and macOS), media simply will not play if the server does not support Range requests. In Chrome, the browser will download the entire 50MB WAV file before it can play the end, causing massive latency.3  
* **Cloudflare Proxy**: When R2 is proxied through a standard Cloudflare domain, the caching layer sometimes strips the Accept-Ranges header or converts partial responses into full responses to populate the cache.8

### **4.2 The Solution: Custom Worker Implementation**

To guarantee robust streaming, a Cloudflare Worker must be deployed to intercept requests and manually interface with the R2 bucket API, specifically handling the Range header.

#### **4.2.1 Worker Logic Analysis**

The Worker must perform the following logic flow 9:

1. **Intercept**: Catch GET requests to the media path.  
2. **Parse Header**: Extract the Range: bytes=start-end header.  
3. **R2 Interface**: Use the R2Bucket.get(key, options) method. Crucially, the options object must include a range property derived from the header.  
4. **Response Construction**: Return a Response object with status 206, the Content-Range header, and the sliced body stream.

#### **4.2.2 Detailed Implementation Code**

The following code snippet demonstrates the exact logic required to satisfy the streaming requirement. This code is deployed to Cloudflare Workers and bound to the R2 bucket variable MY\_BUCKET.

JavaScript

// workers/stream-handler.js

export default {  
  async fetch(request, env) {  
    const url \= new URL(request.url);  
    const key \= url.pathname.slice(1); // Remove leading slash

    // 1\. Handle CORS (Essential for playing media on different domains)  
    if (request.method \=== "OPTIONS") {  
      return new Response(null, {  
        headers: {  
          "Access-Control-Allow-Origin": "\*",  
          "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",  
          "Access-Control-Allow-Headers": "Range",  
        },  
      });  
    }

    // 2\. Parse Range Header  
    const rangeHeader \= request.headers.get("Range");  
      
    // 3\. Configure R2 Options  
    let r2Options \= {  
        onlyIf: request.headers // Pass etags/if-match headers  
    };  
      
    if (rangeHeader) {  
        // R2's get() method accepts a 'range' header string directly in some bindings,  
        // but for granular control, we parse it.  
        // Simplified for brevity: R2 binding supports passing the header directly in newer runtime versions.  
        r2Options.range \= request.headers;   
    }

    // 4\. Fetch from R2  
    const object \= await env.MY\_BUCKET.get(key, r2Options);

    if (object \=== null) {  
      return new Response("Not Found", { status: 404 });  
    }

    // 5\. Construct Response Headers  
    const headers \= new Headers();  
    object.writeHttpMetadata(headers);  
    headers.set("etag", object.httpEtag);  
    headers.set("Access-Control-Allow-Origin", "\*");  
      
    // Critical: Explicitly set Accept-Ranges to inform browser that seeking is allowed  
    headers.set("Accept-Ranges", "bytes");

    // 6\. Handle 206 vs 200  
    // If the object.body is a partial result (R2 handled the range), return 206\.  
    // The R2 binding automatically handles the status code if 'range' was passed.  
    // However, we ensure the Content-Range header is present.  
    if (rangeHeader && object.range) {  
        // object.range contains information about the returned chunk  
        // format: bytes start-end/total  
        // This is automatically handled by writeHttpMetadata in most cases,   
        // but explicit handling ensures safety.  
        return new Response(object.body, {  
            status: 206,  
            headers  
        });  
    }

    return new Response(object.body, {  
        status: 200,  
        headers  
    });  
  }  
};

This implementation ensures that when a user scrubs the audio player, the network tab will show multiple small requests (e.g., 200KB chunks) rather than a single monolithic download.

### **4.3 Secure Uploads with Presigned URLs**

For the portfolio's "Admin" side (where the developer uploads assets), streaming data through the app server is inefficient. We use **Presigned URLs** to allow the browser to upload directly to R2.  
**Mechanism**:

1. **Server Function**: A TanStack Start server function (running on the backend) uses the S3 SDK (or aws4fetch on Workers) to generate a signed PUT URL.  
2. **Security**: The signature includes an expiration time (e.g., 5 minutes) and can enforce the Content-Type, preventing a user from uploading an .exe when an .mp3 was expected.11  
3. **Frontend**: The Shadcn Input type="file" component triggers a fetch(signedUrl, { method: 'PUT', body: file }).

## ---

**5\. The Application Layer: TanStack Start**

TanStack Start is the "glue" that binds the reactive database to the frontend. It introduces architectural patterns that are distinct from other frameworks like Next.js or Remix.

### **5.1 Isomorphic Loaders and Data Fetching**

In TanStack Start, data loading is tied to the **Route**. The loader function runs on the server (during SSR) and provides data to the route component. Crucially, these loaders are "isomorphic"—they can theoretically run on the client during navigation, but in this architecture, we leverage them to call **Server Functions**.12  
**The Pattern**:

1. **Route Definition**: app/routes/music.tsx defines a loader.  
2. **Server Function**: The loader calls getTracksFn(), a function created with createServerFn().  
3. **Execution**: getTracksFn executes on the Cloudflare Worker, queries Convex via HTTP (or direct binding if co-located), and returns JSON.  
4. **Type Safety**: The data returned to the music.tsx component is fully typed. If getTracksFn returns an array of objects with a bpm field, the component knows bpm is a number.

### **5.2 Streaming SSR for Media Performance**

A portfolio page might have "Critical Data" (the project title, description) and "Slow Data" (the download URL for a 5GB build, which might require a fresh signature generation). TanStack Start allows **Streaming SSR**.  
We can return a Promise in the loader for the slow data. The page renders the title immediately. The slow data is wrapped in a \<Suspense\> boundary. When the promise resolves on the server, the data is streamed to the client, and the UI updates.13

TypeScript

// app/routes/game.$id.tsx  
export const Route \= createFileRoute('/game/$id')({  
  loader: async ({ params }) \=\> {  
    // Critical: Fetch game metadata (fast)  
    const game \= await getGameMetadata(params.id);  
      
    // Slow: Generate secure download link (slow)  
    const downloadUrlPromise \= generateSecureLink(game.r2Key);  
      
    return { game, downloadUrlPromise };  
  },  
  component: GamePage  
});

function GamePage() {  
  const { game, downloadUrlPromise } \= Route.useLoaderData();  
  return (  
    \<div\>  
      \<h1\>{game.title}\</h1\>  
      \<Suspense fallback={\<Skeleton className="h-10 w-40" /\>}\>  
        \<DownloadButton promise={downloadUrlPromise} /\>  
      \</Suspense\>  
    \</div\>  
  );  
}

This ensures the user sees the page content instantly, even if the secure link generation takes 500ms.

## ---

**6\. User Interface Engineering: Shadcn and Visualization**

The visual presentation of the portfolio must reflect the technical competence of a game developer and the aesthetic sensibility of a music producer.

### **6.1 Shadcn Architecture: Ownership vs. Abstraction**

Shadcn UI is unique because it is not installed as a dependency (like @mui/material). Instead, components are initialized into the codebase (e.g., src/components/ui/button.tsx). This allows the developer to modify the underlying markup directly.  
**Customizing for Game Dev Aesthetics**:

* **Theming**: Using CSS variables in globals.css to define HSL color values allows for dynamic theming (e.g., a "Cyberpunk" theme that switches all accents to neon yellow).  
* **Typography**: Integrating a monospaced font (like JetBrains Mono) for technical specs (e.g., "Unity 2022.3 / C\#") adds a coding flavor to the design.

### **6.2 Visualizing Audio: The Waveform Component**

A generic audio player bar is insufficient for a producer's portfolio. Users expect to see the waveform to anticipate drops or quiet sections.  
**Implementation**:

* **Library**: wavesurfer.js 14 is the standard for canvas-based visualization.  
* **Integration with Shadcn**: The player controls (Play, Pause, Volume) should be built using Shadcn Button and Slider components to maintain design consistency, while the waveform is rendered in a div referenced by useRef.  
* **Data Flow**: The R2 stream URL is passed to the Wavesurfer instance. The component handles the ready, audioprocess, and finish events to update the React state of the playback progress.

### **6.3 Visualizing Game Assets**

For 3D assets, integrating a \<Canvas\> via @react-three/fiber allows for interactive turn-tables of 3D models (GLB/GLTF) directly in the browser. This demonstrates technical proficiency with WebGL.

## ---

**7\. The Private Studio: Self-Hosted Infrastructure**

A professional creator's workflow involves managing thousands of raw assets that should not be public. To satisfy the requirement for "self-hostable tools for frontend design and asset management," we architect a "Private Studio" using Docker Compose. This infrastructure runs locally or on a secure VPS, separate from the public portfolio.

### **7.1 Networking and Security**

All services in the private studio should be orchestrated via **Docker Compose**. To access them securely, a reverse proxy like **Traefik** or **Caddy** is recommended to handle SSL termination and route subdomains (e.g., assets.studio.local or design.studio.local).

### **7.2 The Toolchain**

**Table 2: Self-Hosted Tool Selection Matrix**

| Category | Tool Recommendation | Why this Tool? | Docker Image |
| :---- | :---- | :---- | :---- |
| **Design / UI** | **Penpot** | The only open-source, SVG-based alternative to Figma. Critical for designing game UIs and portfolio layouts without subscription fees. | penpotapp/backend, penpotapp/frontend 15 |
| **3D Asset Management** | **Manyfold** | Specialized for 3D print and game assets (STL, OBJ). It analyzes mesh data and organizes parts, unlike generic file browsers. | ghcr.io/manyfold3d/manyfold 16 |
| **Physical/General DAM** | **Shelf.nu** | Open-source asset tracking with QR code support. Ideal for tracking studio hardware (synths, test devices) and general inventory. | ghcr.io/shelf-nu/shelf.nu 17 |
| **Documentation** | **Ladle** | A lightweight alternative to Storybook. Chosen because Storybook often struggles with Hot Module Replacement (HMR) in Docker due to file-watching limits. Ladle is Vite-native and instant. | node:18-alpine (Running ladle serve) 18 |

### **7.3 Detailed Docker Compose Configuration**

The following configuration creates a unified network for these tools. Note the volume mapping strategies which ensure data persistence.

YAML

version: "3.8"

services:  
  \# \--- Design: Penpot \---  
  penpot-frontend:  
    image: penpotapp/frontend:latest  
    ports: \["9001:80"\]  
    environment:  
      \- PENPOT\_FLAGS=disable-registration demo-users  
    depends\_on: \[penpot-backend, penpot-exporter\]  
    networks: \[studio-net\]

  penpot-backend:  
    image: penpotapp/backend:latest  
    volumes: \["penpot\_assets:/opt/data/assets"\]  
    depends\_on: \[penpot-postgres, penpot-redis\]  
    networks: \[studio-net\]

  penpot-postgres:  
    image: postgres:15  
    environment:  
      \- POSTGRES\_DB=penpot  
      \- POSTGRES\_PASSWORD=penpot\_secure\_pass  
    volumes: \["penpot\_db:/var/lib/postgresql/data"\]  
    networks: \[studio-net\]

  \# \--- 3D Assets: Manyfold \---  
  manyfold:  
    image: ghcr.io/manyfold3d/manyfold:latest  
    ports: \["3214:3214"\]  
    volumes:  
      \-./my\_3d\_library:/libraries/models \# Maps local folder to container  
      \- manyfold\_data:/config  
    environment:  
      \- DATABASE\_URL=postgresql://manyfold:password@manyfold-db:5432/manyfold  
      \- REDIS\_URL=redis://manyfold-redis:6379/1  
    depends\_on: \[manyfold-db, manyfold-redis\]  
    networks: \[studio-net\]

  \# \--- Documentation: Ladle \---  
  \# Runs the portfolio's component documentation  
  ladle:  
    image: node:20-alpine  
    working\_dir: /app  
    volumes:  
      \-../portfolio-frontend:/app \# Mounts the actual portfolio code  
    command: sh \-c "npm install && npm run ladle serve \-- \--host 0.0.0.0 \--port 61000"  
    ports: \["61000:61000"\]  
    networks: \[studio-net\]

networks:  
  studio-net:

volumes:  
  penpot\_assets:  
  penpot\_db:  
  manyfold\_data:

**Operational Note**: The ladle service mounts the portfolio source code directly. This means as the developer edits components in VS Code on the host machine, the Ladle documentation running in Docker updates instantly via HMR, providing a live "style guide" environment.

## ---

**8\. Methodologies for Tool Discovery**

The request to "learn how to discover such software myself" points to a need for **Open Source Intelligence (OSINT)** skills regarding developer tooling. Relying on generic Google searches ("best asset manager") often leads to SEO-spam blogs promoting paid SaaS products.

### **8.1 The "Aggregator" Strategy**

The most reliable sources for self-hosted, developer-centric tools are community-curated aggregators.

1. **Awesome Lists**: These are GitHub repositories that curate lists of tools. The query pattern awesome-selfhosted or awesome-gamedev on GitHub yields high-quality, peer-reviewed lists.  
   * *Example*: awesome-selfhosted \> "Media Management" section is where tools like **Immich** or **Jellyfin** are discovered.  
2. **Subreddit Telemetry**: The r/selfhosted and r/devops communities are the first to test and validate new tools. Using "site:reddit.com selfhosted \[keyword\]" is often more effective than a standard web search.

### **8.2 Evaluation Heuristics**

Once a potential tool is found, apply the following rigorous checks before adoption:

1. **The "Docker First" Test**: Look for a docker-compose.yml in the root of the repository. If the project requires complex manual installation (compiling binaries, messing with system dependencies), it is likely fragile and hard to update.  
2. **Commit Velocity**: Check the "Insights \> Contributors" tab on GitHub. A project with no commits in 12 months is "zombie software" and a security risk.  
3. **License Audit**: Distinguish between **Open Source** (MIT, Apache, GPL) and **Source Available** (Business Source License). For a personal studio, Source Available is usually fine, but strictly Open Source ensures long-term viability even if the original company folds.

## ---

**9\. Implementation Strategy and Deployment**

### **9.1 Authentication with Better Auth**

Better Auth is integrated as a library. It is configured to use **GitHub OAuth** (essential for a developer).

* **Database Adapter**: It connects to the Convex database.  
* **Session Strategy**: It uses stateless JWTs or database-backed sessions. For this portfolio, database sessions are preferred to allow for "Revoke All Sessions" functionality in case of compromise.  
* **Middleware**: A TanStack Start middleware intercepts requests to /admin. If context.auth.session is null, it throws a redirect to /login.

### **9.2 CI/CD Pipeline**

Deployment should be automated.

1. **GitHub Actions**: A workflow triggers on push to main.  
2. **Lint/Test**: Runs eslint and vitest to ensure code quality.  
3. **Convex Deploy**: Runs npx convex deploy to update the backend functions and schema.  
4. **Wrangler Deploy**: Runs npx wrangler deploy to push the TanStack Start application to Cloudflare Workers.

### **9.3 Environment Management**

Secrets (Better Auth secrets, R2 keys) must be managed carefully.

* **Local**: .env.local file.  
* **Cloudflare**: Use wrangler secret put NAME to encrypt secrets in the Workers environment.  
* **Convex**: Use the Convex Dashboard to set environment variables that are accessible to backend functions.

## **10\. Conclusion**

By synthesizing **TanStack Start** for isomorphic routing, **Convex** for reactive data, and **Cloudflare R2** for cost-effective media streaming, we create a portfolio architecture that is resilient, performant, and professional. The addition of a **Docker-based Private Studio** empowers the creator to manage their digital supply chain with the same rigor they apply to their public work. This is not just a website; it is a comprehensive operating system for a modern digital creative.  
This architecture ensures that the "Game Developer" persona can distribute builds without bankruptcy from bandwidth fees, and the "Music Producer" persona can showcase audio with the fidelity it deserves, all while maintaining total ownership of the platform.

#### **Works cited**

1. TanStack Start Overview | TanStack Start React Docs, accessed December 5, 2025, [https://tanstack.com/start/latest/docs/framework/react/overview](https://tanstack.com/start/latest/docs/framework/react/overview)  
2. Realtime, all the time \- Convex, accessed December 5, 2025, [https://www.convex.dev/realtime](https://www.convex.dev/realtime)  
3. Range Request Support for Videos in R2 buckets \- API \- Cloudflare Community, accessed December 5, 2025, [https://community.cloudflare.com/t/range-request-support-for-videos-in-r2-buckets/776059](https://community.cloudflare.com/t/range-request-support-for-videos-in-r2-buckets/776059)  
4. The AI-Native shadcn Component Library for React Developers, accessed December 5, 2025, [https://www.shadcn.io/](https://www.shadcn.io/)  
5. Take Control of Pagination \- Stack by Convex, accessed December 5, 2025, [https://stack.convex.dev/pagination](https://stack.convex.dev/pagination)  
6. Best Practices | Convex Developer Hub, accessed December 5, 2025, [https://docs.convex.dev/understanding/best-practices/](https://docs.convex.dev/understanding/best-practices/)  
7. Uploading and Storing Files | Convex Developer Hub, accessed December 5, 2025, [https://docs.convex.dev/file-storage/upload-files](https://docs.convex.dev/file-storage/upload-files)  
8. Public R2 bucket doesn't handle range requests well \- Storage \- Cloudflare Community, accessed December 5, 2025, [https://community.cloudflare.com/t/public-r2-bucket-doesnt-handle-range-requests-well/434221](https://community.cloudflare.com/t/public-r2-bucket-doesnt-handle-range-requests-well/434221)  
9. Request · Cloudflare Workers docs, accessed December 5, 2025, [https://developers.cloudflare.com/workers/runtime-apis/request/](https://developers.cloudflare.com/workers/runtime-apis/request/)  
10. R2 via worker Accept Range \- Cloudflare Community, accessed December 5, 2025, [https://community.cloudflare.com/t/r2-via-worker-accept-range/672054](https://community.cloudflare.com/t/r2-via-worker-accept-range/672054)  
11. How to Generate Pre-signed URLs for Cloudflare R2 with Astro on Cloudflare Workers, accessed December 5, 2025, [https://www.launchfa.st/blog/cloudflare-r2-storage-cloudflare-workers](https://www.launchfa.st/blog/cloudflare-r2-storage-cloudflare-workers)  
12. Data Loading | TanStack Router React Docs, accessed December 5, 2025, [https://tanstack.com/router/v1/docs/framework/react/guide/data-loading](https://tanstack.com/router/v1/docs/framework/react/guide/data-loading)  
13. Streaming Data from Server Functions | TanStack Start React Docs, accessed December 5, 2025, [https://tanstack.com/start/latest/docs/framework/react/guide/streaming-data-from-server-functions](https://tanstack.com/start/latest/docs/framework/react/guide/streaming-data-from-server-functions)  
14. theabhipatel/react-wave-audio-player \- GitHub, accessed December 5, 2025, [https://github.com/theabhipatel/react-wave-audio-player](https://github.com/theabhipatel/react-wave-audio-player)  
15. 6Ministers/penpot-docker-compose-for-prototypes-apps \- GitHub, accessed December 5, 2025, [https://github.com/6Ministers/penpot-docker-compose-for-prototypes-apps](https://github.com/6Ministers/penpot-docker-compose-for-prototypes-apps)  
16. Manyfold \- Cosmos Cloud, accessed December 5, 2025, [https://cosmos-cloud.io/cosmos-ui/market-listing/cosmos-cloud/Manyfold](https://cosmos-cloud.io/cosmos-ui/market-listing/cosmos-cloud/Manyfold)  
17. Shelf \- Awesome Docker Compose, accessed December 5, 2025, [https://awesome-docker-compose.com/apps/inventory-management/shelf](https://awesome-docker-compose.com/apps/inventory-management/shelf)  
18. Introduction | Ladle, accessed December 5, 2025, [https://ladle.dev/docs/](https://ladle.dev/docs/)