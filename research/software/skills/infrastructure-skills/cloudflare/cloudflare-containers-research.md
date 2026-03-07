# Cloudflare Containers: Comprehensive Research Report

## Executive Summary

Cloudflare Containers is a serverless container platform currently in public beta (launched June/July 2025) that enables developers to run containerized applications globally on Cloudflare's edge network. The platform uniquely combines container technology with Cloudflare's Workers and Durable Objects to provide programmable, globally distributed compute with scale-to-zero pricing.

**Key Differentiator**: Each container has a programmable sidecar backed by Durable Objects, making Cloudflare Containers uniquely programmable compared to other container platforms.

---

## 1. Core Features and Capabilities

### Container Platform Overview
- **Global Distribution**: Containers are pre-provisioned across 320+ Cloudflare edge locations worldwide
- **Scale-to-Zero**: Containers automatically start and stop based on demand, with no charges when not running
- **Runtime Agnostic**: Supports multiple container runtimes including gVisor and QEMU
- **Architecture Support**: Currently supports linux/amd64 architecture only
- **Language Agnostic**: Run applications in any language or framework that can be containerized

### Current Instance Types (Beta)

| Instance Type | RAM | vCPU | Use Case |
|--------------|-----|------|----------|
| Dev | 256 MB | 1/16 | Development/testing |
| Basic | 1 GB | 1/4 | Small applications |
| Standard | 4 GB | 1/2 | Production workloads |

### Container Registry
- **Managed Registry**: Automatically integrated registry at `registry.cloudflare.com`
- **Backed by R2**: Container images stored in Cloudflare's object storage
- **ECR Support**: Also supports pulling from Amazon ECR
- **Image Limits**: 2 GB per image, 50 GB total per account
- **Authentication**: Automatic authentication handling through Wrangler

### Beta Limitations (Current Status)
- **Resource Caps**: 40 GB total RAM, 40 vCPU across all concurrent instances (being increased for select customers)
- **No GPU Support**: GPU capabilities exist internally but not available to customers yet
- **No Persistent Storage**: All disk is ephemeral; resets on container restart
- **Manual Scaling**: No built-in autoscaling; developers implement scaling logic in code
- **Cold Starts**: 2-3 seconds typical, depending on image size and code execution time

---

## 2. Architecture and Ontology

### How Containers Run on Cloudflare

#### Three-Layer Architecture

```
[User Request] → [Worker Layer] → [Durable Object Layer] → [Container Runtime]
```

1. **Worker Layer (Entry Point)**
   - Requests first route through a Worker in the datacenter with best latency to the user
   - Worker acts as API gateway, handling routing, authentication, caching, rate-limiting

2. **Durable Object Layer (Programmable Sidecar)**
   - Each container instance has an associated Durable Object
   - Provides programmable lifecycle management
   - Maintains persistent state per instance
   - Routes requests to specific container instances
   - Enables custom scheduling, scaling, and health checking logic

3. **Container Runtime Layer**
   - Containers run in isolated VMs on linux/amd64 architecture
   - Supports gVisor for enhanced security isolation
   - Automatic configuration of logging, metrics, and networking

### Container Lifecycle

#### Startup Process
1. Worker receives request and routes to Durable Object
2. Durable Object initiates container start in nearest location with pre-fetched image
3. Cold start occurs (2-3 seconds typical)
4. Container listens on configured port(s)
5. Subsequent requests route to the same container instance location

#### Running State
- Container processes requests on configured ports
- Maintains ephemeral disk state (resets on restart)
- Durable Object manages activity tracking
- Default sleep timeout: 10 minutes of inactivity (configurable via `sleepAfter`)

#### Shutdown Process
1. Container receives SIGTERM signal
2. 15-minute grace period for cleanup
3. SIGKILL signal sent after grace period
4. All ephemeral disk data is lost
5. Container image state is preserved for next startup

### Deployment Strategy
- **Rolling Deployments**: Container instances gradually replaced (unlike Workers which update immediately)
- **Rollout Options**:
  - `gradual` (default): 10% then 100% rollout
  - `immediate`: 100% of instances updated in one step
- **Pre-scheduling**: Cloudflare pre-schedules instances and pre-fetches images globally for quick scaling

### Security and Isolation

#### gVisor Integration
- **Defense-in-Depth**: gVisor provides VM-like security with container-like resource efficiency
- **Syscall Interception**: Sentry (kernel written in Go) intercepts syscalls to protect host
- **Userspace Kernel**: Linux API implemented in userspace for enhanced isolation
- **Trade-offs**: Lower resource footprint than VMs but slightly higher per-syscall overhead

#### VM Isolation
- Containers run in isolated VMs
- Each container instance fully isolated from others
- Runtime-agnostic architecture supports multiple security models

---

## 3. API Patterns and Deployment Workflows

### Wrangler Configuration

**Basic wrangler.toml Structure:**

```toml
name = "my-container-app"
main = "src/index.ts"

# Container configuration
[[durable_objects.bindings]]
class_name = "MyContainer"
name = "MY_CONTAINER"

# Container image
[durable_objects.bindings.image]
image = "./Dockerfile"
max_instances = 5

# Migrations
[[migrations]]
tag = "v1"
new_sqlite_classes = ["MyContainer"]
```

### Deployment Commands

#### Basic Deployment
```bash
# Deploy both Worker and Container code
wrangler deploy

# Build and push container image
wrangler containers build --push

# Or separate push
wrangler containers push
```

#### Create New Project
```bash
# Clone starter template
npm create cloudflare@latest -- --template=cloudflare/templates/containers-template
```

#### Deployment Options
```bash
# Gradual rollout (default: 10% then 100%)
wrangler deploy

# Immediate rollout (100% at once)
wrangler deploy --rollout-strategy immediate
```

### Container Class API

#### Core Methods from `@cloudflare/containers`

**Lifecycle Management:**
```typescript
// Container class properties
class MyContainer extends Container {
  defaultPort = 8080;           // Default port for requests
  sleepAfter = 600000;          // Sleep after 10 minutes (ms)

  // Lifecycle hooks
  onContainerStart() { }        // Called when container starts
  onContainerHealthy() { }      // Called when container becomes healthy
  onContainerShutdown() { }     // Called when container shuts down
}
```

**Starting Containers:**
```typescript
// Start and wait for specific ports
await container.startAndWaitForPorts({
  ports: [8080, 3000],
  envVars: { API_KEY: "secret" },
  timeout: 30000
});
```

**Making Requests:**
```typescript
// Send HTTP request to container
const response = await container.fetch(request);

// Target specific port
const response = await container.fetch(switchPort(request, 8080));

// Standard fetch-like signature
const response = await container.fetch("http://localhost/api", {
  method: "POST",
  body: JSON.stringify(data)
});
```

**Control Methods:**
```typescript
// Access container via Durable Object context
ctx.container.start();
ctx.container.stop();
ctx.container.getStatus();
```

### Worker Integration Pattern

**Basic Integration Example:**
```typescript
export default {
  async fetch(request, env) {
    // Get Durable Object stub
    const id = env.MY_CONTAINER.idFromName("user-session-123");
    const stub = env.MY_CONTAINER.get(id);

    // Forward request to container via Durable Object
    return stub.fetch(request);
  }
}
```

### Environment Variables and Secrets

#### Configuration Methods

**1. Class-level Environment Variables:**
```typescript
class MyContainer extends Container {
  envVars = {
    NODE_ENV: "production",
    API_URL: "https://api.example.com"
  };
}
```

**2. Instance-level Environment Variables:**
```typescript
await container.startAndWaitForPorts({
  ports: [8080],
  envVars: {
    SESSION_ID: sessionId,
    USER_TOKEN: token
  }
});
```

**3. Build-time Variables:**
```toml
# wrangler.toml
[durable_objects.bindings.image]
image = "./Dockerfile"
image_vars = { BUILD_ENV = "production" }
```

**4. Secrets Management:**
```bash
# Set Worker secret (accessible to container)
wrangler secret put API_KEY

# Local development secrets
# Create .dev.vars or .env file
echo "API_KEY=dev-key" > .dev.vars
```

### Port Management

**Default Port Configuration:**
```typescript
class MyContainer extends Container {
  defaultPort = 9000;  // All requests go to port 9000 by default
}
```

**Multi-Port Routing:**
```typescript
async fetch(request) {
  const url = new URL(request.url);

  // Route based on path
  if (url.pathname.startsWith('/api')) {
    return this.container.fetch(switchPort(request, 3000));
  } else if (url.pathname.startsWith('/admin')) {
    return this.container.fetch(switchPort(request, 8080));
  } else {
    return this.container.fetch(switchPort(request, 80));
  }
}
```

---

## 4. Integration Patterns with Workers and Cloudflare Services

### Core Integration Patterns

#### 1. API Gateway Pattern
**Use Case**: Control routing, authentication, caching, rate-limiting before requests reach container

```typescript
export default {
  async fetch(request, env, ctx) {
    // Authentication
    const token = request.headers.get('Authorization');
    if (!isValid(token)) {
      return new Response('Unauthorized', { status: 401 });
    }

    // Rate limiting
    const rateLimiter = await checkRateLimit(request);
    if (rateLimiter.exceeded) {
      return new Response('Too many requests', { status: 429 });
    }

    // Cache check
    const cache = caches.default;
    let response = await cache.match(request);
    if (response) return response;

    // Forward to container
    const containerId = env.MY_CONTAINER.idFromName("instance-1");
    const container = env.MY_CONTAINER.get(containerId);
    response = await container.fetch(request);

    // Cache response
    ctx.waitUntil(cache.put(request, response.clone()));
    return response;
  }
}
```

#### 2. Service Mesh Pattern
**Use Case**: Create private connections between containers with programmable routing

```typescript
// Load balancer that routes to multiple containers
async fetch(request, env) {
  // Pick container instance (random or based on logic)
  const instanceNum = Math.floor(Math.random() * 5);
  const id = env.MY_CONTAINER.idFromName(`instance-${instanceNum}`);
  const container = env.MY_CONTAINER.get(id);

  return container.fetch(request);
}
```

#### 3. Orchestrator Pattern
**Use Case**: Custom scheduling, scaling, and health checking logic

```typescript
class MyContainer extends Container {
  async onContainerHealthy() {
    // Container is healthy, update load balancer
    await this.env.KV.put('healthy-containers', JSON.stringify({
      id: this.id,
      timestamp: Date.now()
    }));
  }

  async onContainerShutdown() {
    // Remove from load balancer
    await this.env.KV.delete(`container-${this.id}`);
  }
}
```

#### 4. Per-Session Isolation Pattern
**Use Case**: Dedicated container for each user session (AI agents, code sandboxes)

```typescript
async fetch(request, env) {
  // Extract session ID from request
  const sessionId = request.headers.get('X-Session-ID');

  // Create unique container instance per session
  const id = env.MY_CONTAINER.idFromName(`session-${sessionId}`);
  const container = env.MY_CONTAINER.get(id);

  return container.fetch(request);
}
```

### Integration with Cloudflare Services

#### Durable Objects
- **Foundation**: Containers are built on top of Durable Objects
- **State Management**: Each container's sidecar has persistent state via Durable Objects
- **Coordination**: Multiple containers can coordinate through shared Durable Objects

#### R2 (Object Storage)
- **Container Registry**: Images stored in R2-backed registry
- **Data Persistence**: Use R2 for persistent data since container disk is ephemeral
- **Media Storage**: Store processed media/files in R2

```typescript
// Store container output in R2
const result = await container.fetch(request);
const data = await result.arrayBuffer();
await env.MY_BUCKET.put(`output-${id}.bin`, data);
```

#### KV (Key-Value Store)
- **Configuration**: Store container configuration
- **Session State**: Track session-to-container mappings
- **Health Tracking**: Monitor container health status

#### D1 (SQL Database)
- **Metadata**: Store container metadata and logs
- **Application Data**: Containers can access D1 for persistent relational data

#### Queues
- **Async Processing**: Queue work for container processing
- **Batch Jobs**: Process queue messages in containers

#### Workflows
- **Orchestration**: Coordinate multi-container workflows
- **Long-Running**: Manage long-running container-based processes

#### Workers AI
- **GPU Inference**: While containers don't have GPU support, combine with Workers AI for ML tasks
- **Preprocessing**: Use containers for data preprocessing, Workers AI for inference

#### Agents SDK
- **AI Agents**: Run AI agents that execute code in isolated containers
- **Tool Execution**: Containers provide safe execution environment for agent tools

---

## 5. Common Use Cases and Examples

### 1. AI Agent Code Execution

**Scenario**: AI agents need to safely execute user-generated or AI-generated code

**Implementation**:
- Each AI session gets dedicated container instance
- Container provides isolated sandbox for code execution
- File system access, git, package managers available
- Results streamed back through Worker

**Example**: Coder.com uses containers to run arbitrary user-generated code safely, with each chat session getting its own container sandbox.

### 2. Media Processing at the Edge

**Scenario**: Video transcoding, image processing, format conversion

**Implementation**:
```typescript
// FFmpeg video processing
class VideoProcessor extends Container {
  defaultPort = 8080;

  // Dockerfile includes FFmpeg
  // Container accepts video upload, transcodes, returns result
}

// Worker handles upload, routes to container
async fetch(request, env) {
  if (request.url.endsWith('/convert')) {
    const id = env.VIDEO_PROCESSOR.idFromName('converter-1');
    const processor = env.VIDEO_PROCESSOR.get(id);

    // Process video in container
    const result = await processor.fetch(request);

    // Store in R2
    const video = await result.arrayBuffer();
    await env.VIDEOS.put(`output-${Date.now()}.mp4`, video);

    return new Response('Processing complete');
  }
}
```

**Use Cases**:
- Converting videos to GIFs with FFmpeg
- Image resizing and optimization
- Audio transcoding
- PDF generation

### 3. Backend Services in Any Runtime

**Scenario**: Run existing applications written in Python, Java, Go, etc.

**Implementation**:
- Package existing application in Docker container
- Deploy to Cloudflare with `wrangler deploy`
- Application runs globally without code changes

**Examples**:
- Python data processing pipelines
- Java Spring Boot applications
- Go microservices
- Ruby on Rails applications

### 4. CLI Tools and Batch Processing

**Scenario**: Run command-line tools for batch jobs

**Implementation**:
```typescript
// Container runs CLI tool
// Worker coordinates batch processing
async fetch(request, env) {
  const { tasks } = await request.json();

  // Process in parallel across multiple containers
  const results = await Promise.all(
    tasks.map(async (task, i) => {
      const id = env.BATCH_PROCESSOR.idFromName(`worker-${i}`);
      const container = env.BATCH_PROCESSOR.get(id);
      return container.fetch(new Request('http://localhost/process', {
        method: 'POST',
        body: JSON.stringify(task)
      }));
    })
  );

  return new Response(JSON.stringify(results));
}
```

### 5. Stateful Session Management

**Scenario**: Applications that need to maintain state across requests

**Implementation**:
- Each user session gets dedicated container
- Container maintains in-memory state and file system
- Subsequent requests route to same container instance
- Session ends when container sleeps after inactivity

**Use Cases**:
- Interactive development environments
- Database query tools with connection pooling
- Stateful game servers
- Long-running user workflows

### 6. Multi-Language Microservices

**Scenario**: Different services in different languages/frameworks

**Implementation**:
```typescript
// Python ML service
const pythonContainer = env.PYTHON_SERVICE.get(id);

// Node.js API service
const nodeContainer = env.NODE_SERVICE.get(id);

// Go data processing
const goContainer = env.GO_SERVICE.get(id);

// Worker orchestrates between them
async fetch(request, env) {
  const url = new URL(request.url);

  if (url.pathname.startsWith('/ml')) {
    return pythonContainer.fetch(request);
  } else if (url.pathname.startsWith('/api')) {
    return nodeContainer.fetch(request);
  } else if (url.pathname.startsWith('/data')) {
    return goContainer.fetch(request);
  }
}
```

---

## 6. Best Practices and Limitations

### Best Practices

#### 1. Image Optimization
- **Keep images small**: Faster cold starts, lower storage costs
- **Multi-stage builds**: Minimize final image size
- **Layer caching**: Order Dockerfile commands to maximize cache hits

```dockerfile
# Good: Dependencies layer cached separately
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --production
COPY . .
CMD ["node", "server.js"]
```

#### 2. Graceful Shutdown
```typescript
// Handle SIGTERM for graceful shutdown
process.on('SIGTERM', async () => {
  console.log('SIGTERM received, shutting down gracefully');

  // Close database connections
  await db.close();

  // Finish pending requests
  server.close(() => {
    console.log('Server closed');
    process.exit(0);
  });

  // Force shutdown after grace period
  setTimeout(() => {
    console.error('Forced shutdown');
    process.exit(1);
  }, 14 * 60 * 1000); // 14 minutes (before SIGKILL)
});
```

#### 3. Ephemeral Disk Management
- **Never rely on disk persistence**: Use R2, D1, or KV for persistent data
- **Initialize on startup**: Always assume fresh disk on container start
- **Temporary files**: Use disk for temporary processing only

#### 4. Environment-Specific Configuration
```typescript
// Different configs for dev/staging/prod
class MyContainer extends Container {
  envVars = {
    NODE_ENV: this.env.ENVIRONMENT || 'production',
    LOG_LEVEL: this.env.DEBUG ? 'debug' : 'info'
  };
}
```

#### 5. Health Checks
```typescript
class MyContainer extends Container {
  async onContainerStart() {
    // Wait for container to be healthy
    const maxRetries = 30;
    for (let i = 0; i < maxRetries; i++) {
      try {
        const response = await this.container.fetch('http://localhost/health');
        if (response.ok) {
          console.log('Container healthy');
          return;
        }
      } catch (e) {
        await new Promise(r => setTimeout(r, 1000));
      }
    }
    throw new Error('Container failed to become healthy');
  }
}
```

#### 6. Resource Sizing
- **Start small**: Use `dev` instances for testing
- **Monitor usage**: Track CPU and memory metrics
- **Scale appropriately**: Move to `basic` or `standard` based on actual needs

#### 7. Request Routing
```typescript
// Use consistent routing for stateful applications
function getContainerIdForUser(userId: string) {
  // Hash user ID to consistent container
  const hash = simpleHash(userId);
  const instanceNum = hash % MAX_INSTANCES;
  return `instance-${instanceNum}`;
}
```

### Current Limitations

#### Resource Constraints
- **Memory**: 40 GB total across all instances
- **CPU**: 40 total vCPUs across all instances
- **Image Size**: 2 GB per image, 50 GB total per account
- **Instance Sizes**: Maximum 4 GB RAM, 1/2 vCPU per instance
- **Note**: Limits being raised, especially for select customers

#### Architecture
- **Platform**: linux/amd64 only (no ARM support)
- **GPU**: Not available to customers (internal use only)

#### Storage
- **Ephemeral Disk**: All data lost on container sleep/restart
- **No Persistent Volumes**: Must use R2, D1, or KV for persistence
- **Future**: Persistent disk being explored but not near-term

#### Scaling
- **Manual Logic**: No built-in autoscaling
- **getRandom Helper**: Temporary solution for load distribution
- **Future**: Native latency-aware autoscaling and load balancing planned

#### Networking
- **No WebSocket Support**: `containerFetch` doesn't support WebSockets
- **Port Limitations**: Manual port routing via `switchPort`
- **Private Networking**: Limited to Cloudflare network

#### Cold Starts
- **2-3 Second Typical**: Dependent on image size
- **No Pre-warming**: Containers start on-demand
- **Mitigation**: Keep images small, use `sleepAfter` to keep warm

#### Deployment
- **Gradual Rollouts**: Cannot instantly update all instances
- **Worker/Container Coupling**: Currently, Worker updates can trigger Container updates (changing in future)

#### Beta-Specific
- **No SLA**: Beta product without service level agreements
- **API Changes**: APIs may change before general availability
- **Feature Gaps**: Many features still in development

### Anti-Patterns to Avoid

#### 1. Database in Container
```typescript
// DON'T: Run database in ephemeral container
// Data will be lost on sleep

// DO: Use D1, external database, or Durable Objects for data
```

#### 2. Large Images
```dockerfile
# DON'T: Include unnecessary dependencies
FROM ubuntu:latest  # Large base image
RUN apt-get install everything...

# DO: Use minimal base images
FROM alpine:latest
RUN apk add --no-cache only-what-needed
```

#### 3. Ignoring Lifecycle Signals
```javascript
// DON'T: Ignore SIGTERM
// Container will be SIGKILL'd after 15 minutes

// DO: Handle graceful shutdown
process.on('SIGTERM', cleanup);
```

#### 4. Assuming Regional Deployment
```typescript
// DON'T: Hardcode assumptions about location
const DB_HOST = 'us-east-1.db.example.com';

// DO: Use environment variables and Cloudflare bindings
const db = env.D1_DATABASE;
```

---

## 7. Pricing Model

### Compute Pricing (per 10ms increments)
- **CPU**: $0.000020 per vCPU-second
- **Memory**: $0.0000025 per GB-second
- **Disk**: $0.00000007 per GB-second

### Workers Paid Plan ($5/month includes)
- 375 vCPU-minutes
- 25 GB-hours of RAM
- 200 GB-hours of disk

### Egress Pricing (varies by region)
- **North America & Europe**: $0.025/GB (1 TB included)
- **Australia, New Zealand, Taiwan, Korea**: $0.050/GB (500 GB included)
- **Other Regions**: $0.040/GB (500 GB included)

### Scale-to-Zero Benefit
No charges when containers are not running - pay only for actual compute time.

### Cost Example
```
Standard instance (4 GB RAM, 0.5 vCPU) running for 1 hour:
- CPU cost: 0.5 * 3600 * $0.000020 = $0.036
- RAM cost: 4 * 3600 * $0.0000025 = $0.036
- Disk cost: 4 * 3600 * $0.00000007 = $0.001
- Total: ~$0.073/hour when running
```

---

## 8. Key Concepts and Terminology

### Container Class
The Durable Object class that extends the base `Container` class from `@cloudflare/containers`. Defines lifecycle hooks, default ports, environment variables, and container behavior.

### Durable Object Sidecar
Each container has an associated Durable Object that acts as a programmable sidecar, managing the container lifecycle, routing requests, and maintaining persistent state separate from the ephemeral container.

### Container Instance
A running container with its own isolated environment, filesystem, and network namespace. Multiple instances can exist from the same container class.

### Wrangler
Cloudflare's CLI tool for managing and deploying Workers and Containers. Handles building Docker images, pushing to registry, and deploying code.

### Container Registry
The Cloudflare-managed Docker registry at `registry.cloudflare.com`, backed by R2 storage, used to store container images.

### Cold Start
The time required to start a new container instance from scratch, typically 2-3 seconds. Occurs when no running instance exists or when scaling up.

### Ephemeral Disk
Container filesystem that resets to the image state on every restart. No data persists between container lifecycle events.

### sleepAfter
Configuration parameter (in milliseconds) that determines how long a container stays running after receiving its last request. Default is 10 minutes (600000ms).

### startAndWaitForPorts()
Method to start a container instance and wait for specific ports to be listening before considering the container ready.

### containerFetch() / fetch()
Method to send HTTP requests from Worker to container. Supports standard fetch API but not WebSockets.

### switchPort()
Helper function to route requests to specific container ports instead of the default port.

### getRandom
Temporary helper function for distributing requests across multiple container instances. Will be replaced by native autoscaling.

### Lifecycle Hooks
Methods called automatically during container state transitions:
- `onContainerStart()` - Container starts
- `onContainerHealthy()` - Container becomes healthy
- `onContainerShutdown()` - Container shuts down

### Rolling Deployment
Gradual update strategy where container instances are replaced in phases (e.g., 10% then 100%) rather than all at once.

### Bindings
Cloudflare's mechanism for connecting Workers to resources like Durable Objects, R2, KV, D1, etc. Containers are accessed via Durable Object bindings.

### Region: Earth
Cloudflare's architecture philosophy where services run globally rather than in specific regions. Containers automatically deploy to optimal locations.

### gVisor
Security isolation runtime that provides VM-like security with container-like efficiency by intercepting system calls in userspace.

---

## 9. Beta Status and Roadmap

### Current Status (as of June/July 2025)
- **Public Beta**: Available to all users on paid plans
- **Production Use**: Platform is in production with GPUs internally at Cloudflare
- **Feedback Collection**: Actively seeking user feedback to shape GA features

### Known Beta Limitations
- Limited instance sizes (max 4 GB RAM, 0.5 vCPU)
- Limited total resources (40 GB RAM, 40 vCPU total)
- No GPU support for customers
- No persistent storage
- Manual scaling logic required
- No native autoscaling or load balancing
- Some API instability expected

### Planned for GA (Before General Availability)

#### Resource Increases
- Higher maximum instance sizes
- Higher total account limits
- Already increasing for select customers

#### Autoscaling & Routing
- Native global autoscaling
- Latency-aware routing
- Automatic load balancing
- Improved container distribution

#### Storage & Persistence
- Persistent disk (being explored, not near-term)
- Better integration with R2, KV, D1

#### Communication
- More ways for Workers and Containers to communicate
- Enhanced networking capabilities
- Possible WebSocket support

#### Platform Integration
- Deeper integration with Workflows, Queues, Agents
- Support for additional APIs
- Easier data storage access

#### Deployment
- Decoupled Worker and Container updates
- Container code only updates when Container code changes
- Better rollout control

### Future Possibilities (Not Committed)
- GPU support for customers
- ARM architecture support
- Multi-region persistent storage
- Container-to-container networking
- Advanced monitoring and observability
- Kubernetes compatibility layer

### Feedback Areas
Cloudflare is seeking feedback on:
- Integration needs with other Cloudflare services
- Desired ways to interact with Containers via Workers
- Routing and scaling mechanisms
- Performance requirements
- Use cases and workloads

---

## 10. Comparison with Other Platforms

### vs. AWS Lambda with Container Images

**Cloudflare Containers Advantages:**
- Global edge deployment (320+ locations)
- Tighter integration with edge services
- Programmable per-container state via Durable Objects
- Scale-to-zero with potentially faster cold starts in some cases

**AWS Lambda Advantages:**
- Larger instance sizes (up to 10 GB RAM, 6 vCPUs)
- Larger image support (up to 10 GB)
- Mature ecosystem and tooling
- GPU support available
- Persistent storage options (EFS)
- Extensive AWS service integrations

### vs. Google Cloud Run

**Cloudflare Containers Advantages:**
- Edge-first architecture
- Unique programmable sidecar pattern
- Global deployment without region selection
- Integration with Workers platform

**Cloud Run Advantages:**
- Larger instance sizes (up to 32 GB RAM)
- Longer execution times (up to 60 minutes)
- More mature platform (GA)
- Built-in autoscaling
- VPC networking
- Cloud SQL integration

### vs. Fly.io

**Cloudflare Containers Advantages:**
- Larger global network (320+ vs ~35 regions)
- Integration with Cloudflare ecosystem
- Durable Objects for coordination
- Scale-to-zero pricing

**Fly.io Advantages:**
- Persistent volumes
- Simpler deployment model
- More mature container platform
- Better ARM support
- Anycast networking

### Unique Differentiators for Cloudflare

1. **Programmable Sidecar**: Every container has a Durable Object sidecar enabling custom lifecycle management
2. **Workers Integration**: Seamless integration with Workers for edge logic
3. **Region: Earth**: True global deployment without region selection
4. **Developer Platform**: Integration with R2, KV, D1, Queues, Workflows, Agents
5. **Edge-First**: Optimized for edge computing patterns

---

## 11. Getting Started Example

### Complete Working Example

**1. Create Project**
```bash
npm create cloudflare@latest -- --template=cloudflare/templates/containers-template
cd my-container-app
```

**2. Create Dockerfile**
```dockerfile
# Dockerfile
FROM node:18-alpine

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci --production

# Copy application code
COPY server.js ./

# Expose port
EXPOSE 8080

# Start server
CMD ["node", "server.js"]
```

**3. Create Server**
```javascript
// server.js
const http = require('http');

const server = http.createServer((req, res) => {
  console.log(`${req.method} ${req.url}`);

  if (req.url === '/health') {
    res.writeHead(200);
    res.end('OK');
  } else if (req.url === '/api/hello') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ message: 'Hello from container!' }));
  } else {
    res.writeHead(404);
    res.end('Not found');
  }
});

server.listen(8080, () => {
  console.log('Server listening on port 8080');
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('SIGTERM received, shutting down');
  server.close(() => {
    console.log('Server closed');
    process.exit(0);
  });
});
```

**4. Create Worker**
```typescript
// src/index.ts
import { Container } from '@cloudflare/containers';

export class MyContainer extends Container {
  defaultPort = 8080;
  sleepAfter = 600000; // 10 minutes

  async onContainerStart() {
    console.log('Container starting...');

    // Wait for health check
    const response = await this.container.fetch('http://localhost/health');
    if (!response.ok) {
      throw new Error('Container failed health check');
    }

    console.log('Container is healthy!');
  }

  async onContainerShutdown() {
    console.log('Container shutting down...');
  }
}

export default {
  async fetch(request: Request, env: Env) {
    // Route to container instance
    const id = env.MY_CONTAINER.idFromName('instance-1');
    const container = env.MY_CONTAINER.get(id);

    return container.fetch(request);
  }
}
```

**5. Configure Wrangler**
```toml
# wrangler.toml
name = "my-container-app"
main = "src/index.ts"
compatibility_date = "2025-01-01"

[[durable_objects.bindings]]
name = "MY_CONTAINER"
class_name = "MyContainer"

[durable_objects.bindings.image]
image = "./Dockerfile"
instance_type = "standard"
max_instances = 5

[[migrations]]
tag = "v1"
new_sqlite_classes = ["MyContainer"]
```

**6. Deploy**
```bash
# Ensure Docker is running
docker --version

# Deploy to Cloudflare
wrangler deploy

# Test
curl https://my-container-app.workers.dev/api/hello
```

**7. Monitor**
```bash
# View logs
wrangler tail

# Check deployment
wrangler deployments list
```

---

## 12. Official Resources

### Documentation
- **Main Docs**: https://developers.cloudflare.com/containers/
- **Getting Started**: https://developers.cloudflare.com/containers/get-started/
- **Architecture**: https://developers.cloudflare.com/containers/platform-details/architecture/
- **Examples**: https://developers.cloudflare.com/containers/examples/
- **FAQ**: https://developers.cloudflare.com/containers/faq/
- **Beta Info & Roadmap**: https://developers.cloudflare.com/containers/beta-info/

### Blog Posts
- **Launch Announcement**: https://blog.cloudflare.com/cloudflare-containers-coming-2025/
- **Beta Release**: https://blog.cloudflare.com/containers-are-available-in-public-beta-for-simple-global-and-programmable-compute/
- **Platform Preview**: https://blog.cloudflare.com/container-platform-preview/

### Code & Tools
- **NPM Package**: `@cloudflare/containers`
- **GitHub**: https://github.com/cloudflare/containers
- **Serverless Registry**: https://github.com/cloudflare/serverless-registry
- **Wrangler CLI**: `npm install -g wrangler`

### Product Pages
- **Containers**: https://workers.cloudflare.com/product/containers
- **Pricing**: https://developers.cloudflare.com/containers/pricing/

---

## 13. Summary and Key Takeaways

### What Makes Cloudflare Containers Unique
1. **Programmable Sidecar Architecture**: Durable Objects provide unprecedented control over container lifecycle and state
2. **Global Edge Distribution**: Truly global deployment across 320+ locations without region selection
3. **Workers Integration**: Seamless integration with Workers for powerful edge computing patterns
4. **Scale-to-Zero**: Pay only when containers are running, automatic shutdown after inactivity
5. **Developer Platform Integration**: Native integration with R2, KV, D1, Queues, Workflows, Agents

### Best Use Cases
- AI agent code execution environments
- Media processing at the edge (FFmpeg, image manipulation)
- Backend services in any language/runtime
- Stateful session management
- Batch processing and CLI tools
- Multi-language microservices

### Current Limitations to Consider
- Beta status with potential API changes
- Limited instance sizes (max 4 GB RAM, 0.5 vCPU)
- No persistent storage (ephemeral disk only)
- No GPU support for customers
- Manual scaling logic required
- linux/amd64 architecture only

### When to Use Cloudflare Containers
- Need global edge deployment
- Want to run existing containerized applications
- Require programmable container lifecycle
- Need integration with Cloudflare services
- Want scale-to-zero economics
- Building AI agents or code sandboxes

### When to Consider Alternatives
- Need large instance sizes (>4 GB RAM)
- Require persistent disk storage
- Need GPU acceleration
- Want mature, GA platform
- Require extensive ecosystem integrations
- Need ARM architecture support

### Future Outlook
Cloudflare Containers is a promising platform in active development with plans for:
- Higher resource limits
- Native autoscaling and load balancing
- Better storage integration
- Enhanced Worker/Container communication
- GPU support (possibly)

The platform's unique architecture combining containers, Durable Objects, and Workers positions it well for edge computing use cases, particularly for globally distributed applications that benefit from Cloudflare's network.

---

**Research Date**: November 2025
**Platform Status**: Public Beta
**Expected GA**: TBD (likely 2026)
