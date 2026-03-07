# Pulumi Infrastructure as Code: Comprehensive Guide for LLMs

## Table of Contents
1. [Overview](#overview)
2. [Core Features & Capabilities](#core-features--capabilities)
3. [Key Patterns & Concepts](#key-patterns--concepts)
4. [Ontologies & Architecture](#ontologies--architecture)
5. [Common Use Cases](#common-use-cases)
6. [Best Practices](#best-practices)
7. [Code Examples](#code-examples)

---

## Overview

### What is Pulumi?

Pulumi is a modern infrastructure as code (IaC) platform that enables developers to define, deploy, and manage cloud infrastructure using familiar programming languages rather than domain-specific languages. Pulumi Infrastructure as Code is the easiest way to build and deploy infrastructure of any architecture on any cloud, using programming languages that developers already know and love.

### Key Value Proposition

- **Multi-language Support**: Write infrastructure code in TypeScript, JavaScript, Python, Go, .NET (C#/F#/VB.NET), Java, and YAML
- **Multi-cloud**: Support for 120+ cloud providers including AWS, Azure, Google Cloud, Kubernetes, and many more
- **Real Programming Languages**: Use familiar constructs like loops, conditionals, functions, and classes instead of limited DSLs
- **True Open Source**: Apache 2.0 license (more permissive than some alternatives)

---

## Core Features & Capabilities

### 1. Programming Language Support

Pulumi supports multiple stable, production-ready programming languages:

- **JavaScript/TypeScript**: Node.js (Current, Active, and Maintenance LTS versions)
- **Python**: All supported Python versions
- **Go**: All supported Go versions
- **.NET**: C#, F#, VB.NET on supported .NET versions
- **Java**: JDK 11+
- **YAML**: For simpler, declarative use cases

**Key Characteristic**: Each language is equally capable and supports the entire surface area of all clouds available in Pulumi Registry. No language is a "second-class citizen."

### 2. Cloud Provider Coverage

Pulumi provides comprehensive cloud provider support through the **Pulumi Registry**:

- **120+ Providers**: Including all major cloud platforms and SaaS services
- **Native Providers**:
  - AWS (native provider available)
  - Azure Native (GA)
  - Google Cloud Native (public preview)
  - Kubernetes (native provider available)

**Native Provider Benefits**:
- 100% API coverage
- Same-day updates via automated GitHub Actions
- Automatic generation from cloud provider API specifications checked nightly

### 3. State Management

Pulumi stores metadata about your infrastructure (called **state**) to manage cloud resources effectively.

#### State Characteristics:
- Each stack has its own state
- State tracks when and how to create, read, delete, or update resources
- **Does NOT include cloud credentials** (credentials remain local to the client)
- Encrypts secrets using chosen encryption provider

#### Backend Types:

**A. Pulumi Cloud (Service Backend)**
- Fully managed SaaS at app.pulumi.com
- Features: SSO, audit logs, centralized stack management, policy management, change history
- Default and recommended for most users

**B. Self-Managed (DIY) Backends**
- AWS S3
- Azure Blob Storage
- Google Cloud Storage
- S3-compatible systems (Minio, Ceph)
- PostgreSQL (newer addition)
- Local filesystem

**Important Note**: Project-scoped stack support now aligns behavior across both backend types.

### 4. Key Differentiators from Terraform

| Feature | Pulumi | Terraform |
|---------|--------|-----------|
| **Language** | Real programming languages (TypeScript, Python, Go, etc.) | HCL (domain-specific language) |
| **Testing** | Unit tests, property tests, integration tests with standard testing frameworks | Limited to integration tests |
| **State Backend** | Pulumi Cloud SaaS or self-managed with advanced features | Local file (terraform.tfstate) or remote backends |
| **Provider Support** | Dynamic Provider Support - faster adoption of new resources | Standard provider model |
| **Community** | Growing, smaller but active | Large, mature ecosystem |
| **License** | Apache 2.0 (true open source) | BSL (Business Source License) |
| **Maturity** | Modern, actively evolving | Industry leader, mature and stable |

---

## Key Patterns & Concepts

### 1. Resources

Resources are the fundamental building blocks in Pulumi. There are two main types:

#### A. Custom Resources
- Cloud resources managed by resource providers (AWS, Azure, GCP, Kubernetes, etc.)
- Directly map to cloud provider resources
- Examples: `aws.s3.Bucket`, `azure.storage.Account`, `gcp.compute.Instance`

#### B. Component Resources
- Higher-level abstractions that encapsulate multiple resources
- Useful for creating reusable and shareable abstractions
- Can be defined in programs or packaged as libraries

**Creating a Component Resource**:
```typescript
class MyComponent extends pulumi.ComponentResource {
    constructor(name: string, args: MyComponentArgs, opts?: pulumi.ComponentResourceOptions) {
        super("pkg:index:MyComponent", name, args, opts);

        // Create child resources here
        const bucket = new aws.s3.Bucket(`${name}-bucket`, {}, { parent: this });

        // Register outputs
        this.registerOutputs({
            bucketName: bucket.id
        });
    }
}
```

**Key Benefits**:
- Encapsulates implementation details
- Tracks state across program deployments
- Shows diffs during updates like regular resources
- Enables organizational reuse

### 2. Inputs and Outputs

Pulumi uses special types to manage dependencies and asynchronous resource creation:

#### Inputs
- Wrap "plain" values (strings, integers, etc.)
- Can accept either plain values OR Output values
- Enable declarative infrastructure despite imperative languages

#### Outputs
- Represent asynchronous values (similar to promises/futures)
- Values that aren't initially known but become available after provisioning
- Automatically track dependencies between resources

**Dependency Tracking**:
```typescript
const bucket = new aws.s3.Bucket("my-bucket");
const bucketObject = new aws.s3.BucketObject("my-object", {
    bucket: bucket.id,  // Output passed as Input - creates dependency
    content: "Hello, World!"
});
```

**Working with Outputs**:
```typescript
// Using apply to transform outputs
const url = bucket.websiteEndpoint.apply(endpoint => `https://${endpoint}`);

// Accessing output values
output.apply(value => {
    // Use value here
});
```

### 3. Stacks

**Definition**: Stacks are independent instances of your Pulumi program, typically corresponding to different environments (dev, staging, production).

**Key Characteristics**:
- Each stack has its own state
- Stacks can have different configuration values
- Multiple stacks can be created from a single program
- Stack naming should align with environments (best practice)

**Stack Configuration**:
- Stored in `Pulumi.<stack-name>.yaml` files
- Should be committed to version control
- Contains environment-specific settings and secrets

**Working with Stacks**:
```bash
# Create a new stack
pulumi stack init dev

# Select a stack
pulumi stack select staging

# List all stacks
pulumi stack ls

# View stack configuration
pulumi config

# Set configuration values
pulumi config set region us-west-2
pulumi config set dbPassword --secret mySecretPassword
```

### 4. Configuration and Secrets

#### Configuration Management

Configuration is managed through key-value pairs stored in stack settings files:

```bash
# Add configuration
pulumi config set key value

# Add secret (encrypted)
pulumi config set apiKey myApiKey123 --secret

# Get configuration
pulumi config get key

# Remove configuration
pulumi config rm key
```

**Accessing in Code**:
```typescript
import * as pulumi from "@pulumi/pulumi";

const config = new pulumi.Config();
const region = config.require("region");
const dbPassword = config.requireSecret("dbPassword");
```

#### Secrets Encryption

**Default Behavior**: Pulumi Cloud provides automatic per-stack encryption keys

**Supported Secrets Providers**:
- `default`: Pulumi Cloud managed encryption
- `passphrase`: Local passphrase-based encryption
- `awskms`: AWS Key Management Service
- `azurekeyvault`: Azure Key Vault
- `gcpkms`: Google Cloud KMS
- `hashivault`: HashiCorp Vault

**Changing Secrets Providers**:
```bash
# Initialize stack with specific provider
pulumi stack init prod --secrets-provider="awskms://alias/my-key"

# Change existing stack's provider
pulumi stack change-secrets-provider "azurekeyvault://mykeyvaultname.vault.azure.net/keys/mykeyname"
```

### 5. Resource Options

Resource options control resource behavior and lifecycle:

#### Common Options:

**dependsOn**: Explicit dependencies
```typescript
const database = new aws.rds.Instance("db", {...});
const app = new aws.lambda.Function("app", {...}, {
    dependsOn: [database]  // Ensure DB is created first
});
```

**protect**: Prevent accidental deletion
```typescript
const productionDb = new aws.rds.Instance("prod-db", {...}, {
    protect: true  // Cannot be deleted
});
```

**ignoreChanges**: Ignore specific property changes
```typescript
const instance = new aws.ec2.Instance("web", {
    tags: { Name: "web-server" }
}, {
    ignoreChanges: ["tags"]  // Don't update if tags change
});
```

**parent**: Set parent-child relationships
```typescript
const component = new MyComponent("my-comp", {...});
const bucket = new aws.s3.Bucket("bucket", {...}, {
    parent: component  // Bucket is child of component
});
```

**deleteBeforeReplace**: Control replacement order
```typescript
const resource = new SomeResource("res", {...}, {
    deleteBeforeReplace: true
});
```

**Additional Options**:
- `provider`: Use specific provider instance
- `retainOnDelete`: Keep resource when stack is deleted
- `aliases`: Handle resource renames
- `replaceOnChanges`: Force replacement on specific property changes
- `customTimeouts`: Override default operation timeouts
- `transformations`: Apply transformations to resource and children
- `version`: Specify provider version

### 6. Transformations and Transforms

**Transformations** (older API, being deprecated):
```typescript
const vpc = new aws.ec2.Vpc("my-vpc", {...}, {
    transformations: [(args) => {
        // Modify resource before creation
        if (args.type === "aws:ec2/subnet:Subnet") {
            args.props.tags = { ...args.props.tags, Owner: "platform-team" };
        }
        return { props: args.props, opts: args.opts };
    }]
});
```

**Transforms** (newer, recommended API):
```typescript
// Stack-level transforms
pulumi.runtime.registerStackTransformation((args) => {
    // Apply to all resources in stack
    return {
        props: args.props,
        opts: pulumi.mergeOptions(args.opts, { protect: true })
    };
});
```

**Key Differences**:
- Transforms are called twice for packaged component resources
- Transforms provide opportunity to modify inputs before provider construction
- Transforms will fully replace Transformations in the future

### 7. Stack References and Cross-Stack Dependencies

Stack references allow accessing outputs from one stack in another stack.

**Exporting Outputs**:
```typescript
// In foundation stack
export const vpcId = vpc.id;
export const subnetIds = subnets.map(s => s.id);
```

**Using Stack References**:
```typescript
import * as pulumi from "@pulumi/pulumi";

// Reference another stack
const infraStack = new pulumi.StackReference("org/foundation/prod");

// Access outputs
const vpcId = infraStack.getOutput("vpcId");
const subnetIds = infraStack.getOutput("subnetIds");

// Use in resources
const instance = new aws.ec2.Instance("app", {
    vpcId: vpcId,
    subnetIds: subnetIds
});
```

**Best Practices**:
- Export structured objects for many outputs
- Use fully qualified stack names: `<organization>/<project>/<stack>`
- Only export what needs to be shared
- Document exported outputs

### 8. Dynamic Providers

Dynamic providers enable custom resource management within your Pulumi program.

**When to Use**:
- Cloud provider not yet supported by Pulumi
- Custom REST API integration
- Complex logic not available in standard providers
- Temporary solutions until official providers are available

**Key Characteristics**:
- Defined in the same language as your Pulumi program
- Lightweight compared to full custom providers
- Language-specific (can't be shared across languages)
- Secrets are NOT encrypted in state (important consideration)

**Lifecycle Methods**:
- `check`: Validate input properties
- `create`: Create the resource (required)
- `read`: Read current state of resource
- `update`: Update existing resource
- `delete`: Delete the resource

**Example (TypeScript)**:
```typescript
import * as pulumi from "@pulumi/pulumi";

interface MyResourceInputs {
    name: pulumi.Input<string>;
    value: pulumi.Input<string>;
}

const myProvider: pulumi.dynamic.ResourceProvider = {
    async create(inputs: MyResourceInputs) {
        // Call external API
        const response = await fetch("https://api.example.com/resources", {
            method: "POST",
            body: JSON.stringify(inputs)
        });
        const data = await response.json();

        return {
            id: data.id,
            outs: { ...inputs, endpoint: data.endpoint }
        };
    },

    async update(id, oldInputs, newInputs) {
        // Update logic
        return { outs: newInputs };
    },

    async delete(id, props) {
        // Cleanup logic
        await fetch(`https://api.example.com/resources/${id}`, {
            method: "DELETE"
        });
    }
};

class MyResource extends pulumi.dynamic.Resource {
    constructor(name: string, args: MyResourceInputs, opts?: pulumi.CustomResourceOptions) {
        super(myProvider, name, args, opts);
    }
}
```

**Important Warnings**:
- Code is serialized into state
- Corrupted state can cause significant issues
- Secrets are not encrypted
- Use official providers when available

---

## Ontologies & Architecture

### 1. Pulumi Architecture Overview

Pulumi's architecture is composed of multiple processes communicating via gRPC:

```
┌─────────────────┐
│  Pulumi Program │  (TypeScript, Python, Go, etc.)
│   (Your Code)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Language Host  │  (Language-specific runtime)
│    (Plugin)     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Pulumi Engine   │  (Core orchestration, written in Go)
│  (Deployment)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Providers     │  (AWS, Azure, GCP, etc.)
│   (Plugins)     │
└─────────────────┘
         │
         ▼
┌─────────────────┐
│  Cloud APIs     │  (Actual cloud services)
└─────────────────┘
```

### 2. Core Components

#### A. Pulumi Engine
- **Written in**: Go
- **Responsibilities**:
  - Orchestrates resource lifecycle operations
  - Manages dependency graph
  - Coordinates provider plugins
  - Handles state management
  - Executes preview/update/destroy operations
- **Communication**: gRPC with all components

#### B. Language Host/Runtime
- **Purpose**: Execute Pulumi programs in specific languages
- **Implementation**: Language-specific plugins
- **Responsibilities**:
  - Set up execution environment
  - Register resources with engine
  - Handle language-specific serialization
  - Bridge between user code and engine
- **Support**: TypeScript/JavaScript, Python, Go, .NET, Java, YAML

#### C. Provider Plugins
- **Architecture**: gRPC servers implementing standardized interface
- **Lifecycle**: Started by engine, receive commands, manage cloud resources
- **Operations**: CRUD (Create, Read, Update, Delete) for resources
- **Types**:
  - **Native Providers**: Direct API integration with 100% coverage
  - **Bridged Providers**: Based on Terraform providers
  - **Dynamic Providers**: Defined in user code

#### D. Analyzer Plugins
- **Purpose**: Policy as Code (CrossGuard)
- **Function**: Analyze programs before execution
- **Use Cases**: Security policies, compliance checks, cost controls

### 3. Resource Model and Graph

#### Resource Graph
Pulumi builds a directed acyclic graph (DAG) of resources:

```
VPC
 ├─ Subnet A
 │   └─ EC2 Instance A
 └─ Subnet B
     └─ EC2 Instance B
```

**Graph Properties**:
- Nodes: Resources
- Edges: Dependencies
- Direction: Creation order
- Ensures: Correct creation/destruction sequence

#### Dependency Tracking
**Automatic Dependencies**: Created when Output → Input
```typescript
const bucket = new aws.s3.Bucket("bucket");
const object = new aws.s3.BucketObject("obj", {
    bucket: bucket.id  // Automatic dependency
});
```

**Explicit Dependencies**: Using `dependsOn`
```typescript
const db = new aws.rds.Instance("db", {...});
const app = new App("app", {...}, {
    dependsOn: [db]  // Explicit dependency
});
```

### 4. Deployment Lifecycle

#### Preview (`pulumi preview`)
- **Purpose**: Dry-run showing pending changes
- **Process**:
  1. Load current state
  2. Execute program
  3. Build desired state
  4. Compute diff
  5. Display changes (create/update/delete)
- **No Changes**: Nothing is actually created/modified

#### Up (`pulumi up`)
- **Purpose**: Create/update infrastructure
- **Process**:
  1. Run preview
  2. Ask for confirmation (unless `--yes`)
  3. Execute changes in dependency order
  4. Update state
  5. Display results
- **Idempotent**: Running multiple times with same code = same result

#### Refresh (`pulumi refresh`)
- **Purpose**: Sync state with actual cloud resources
- **When to Use**: Manual changes made outside Pulumi
- **Process**:
  1. Read actual resource state from cloud
  2. Update Pulumi state to match
  3. Report drift
- **New Features** (v3.160.0+): `--run-program` flag to run program first

#### Destroy (`pulumi destroy`)
- **Purpose**: Delete all stack resources
- **Process**:
  1. Build dependency graph
  2. Delete in reverse dependency order
  3. Update state
  4. Keep stack history/config
- **Options**:
  - `--refresh`: Refresh before destroy
  - `--target`: Destroy specific resources
  - `--exclude-protected`: Skip protected resources

#### Additional Operations

**Export/Import State**:
```bash
# Export state
pulumi stack export --file state.json

# Import state
pulumi stack import --file state.json
```

**Cancel Update**:
```bash
pulumi cancel  # Cancel in-progress update
```

### 5. Plugin Architecture

#### Plugin Types and Locations

**Discovery**: Plugins are discovered in:
1. `~/.pulumi/plugins/` (local cache)
2. Downloaded on-demand from Pulumi registry
3. Custom plugin locations (via environment variables)

**Plugin Structure**:
```
~/.pulumi/plugins/
├── resource-aws-v6.0.0/
│   └── pulumi-resource-aws
├── resource-kubernetes-v4.0.0/
│   └── pulumi-resource-kubernetes
└── language-nodejs-v3.0.0/
    └── pulumi-language-nodejs
```

**Plugin Commands**:
```bash
# List installed plugins
pulumi plugin ls

# Install plugin
pulumi plugin install resource aws v6.0.0

# Remove unused plugins
pulumi plugin rm resource aws v5.0.0
```

### 6. Pulumi ESC (Environments, Secrets, Configuration)

**Overview**: Centralized secrets and configuration management introduced in 2023, enhanced in 2025.

**Key Features**:
- **Centralized Integration**: Connects to 20+ secrets stores (HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, 1Password, etc.)
- **Dynamic Credentials**: OIDC-based short-lived credentials
- **Configuration-as-Code**: Hierarchical YAML environments
- **Inheritance**: Environment composition and cascading

**Environment Structure**:
```yaml
# environments/production.yaml
imports:
  - base
  - database-config

values:
  aws:
    login:
      fn::open::aws-login:
        oidc:
          roleArn: arn:aws:iam::123456789:role/pulumi-prod
          sessionName: pulumi-prod

  database:
    host: prod-db.example.com
    port: 5432
    credentials:
      fn::secret:
        fn::open::aws-secrets:
          region: us-east-1
          get:
            secretId: prod-db-password
```

**Access Methods**:
- CLI: `esc env open production`
- API: REST API access
- SDKs: TypeScript/JavaScript, Python, Go
- Kubernetes Operator
- Pulumi Cloud UI

**Security**:
- Fine-grained RBAC
- Audit logging
- Secret encryption at rest and in transit
- Dynamic credential rotation

---

## Common Use Cases

### 1. Multi-Cloud Deployments

**Scenario**: Deploy applications across multiple cloud providers (AWS, Azure, GCP) with consistent patterns.

**Pulumi Advantages**:
- Single codebase for all clouds
- Shared abstractions via Component Resources
- Unified state management
- Cross-cloud dependencies via Stack References

**Example Pattern**:
```typescript
// Multi-cloud Kubernetes clusters
import * as aws from "@pulumi/aws";
import * as azure from "@pulumi/azure-native";
import * as gcp from "@pulumi/gcp";
import * as k8s from "@pulumi/kubernetes";

// AWS EKS
const eksCluster = new aws.eks.Cluster("aws-cluster", {
    roleArn: eksRole.arn,
    vpcConfig: { subnetIds: subnetIds }
});

// Azure AKS
const aksCluster = new azure.containerservice.ManagedCluster("azure-cluster", {
    resourceGroupName: resourceGroup.name,
    agentPoolProfiles: [{ count: 2, vmSize: "Standard_DS2_v2" }]
});

// GCP GKE
const gkeCluster = new gcp.container.Cluster("gcp-cluster", {
    initialNodeCount: 2,
    nodeConfig: { machineType: "n1-standard-1" }
});

// Deploy same application to all clusters
[eksCluster, aksCluster, gkeCluster].forEach((cluster, i) => {
    const provider = new k8s.Provider(`k8s-${i}`, {
        kubeconfig: cluster.kubeconfig
    });

    new k8s.apps.v1.Deployment(`app-${i}`, {
        spec: { /* ... */ }
    }, { provider });
});
```

### 2. Kubernetes Infrastructure

**Scenario**: Manage Kubernetes clusters and applications.

**Capabilities**:
- Create managed clusters (EKS, AKS, GKE)
- Deploy Kubernetes resources (Deployments, Services, etc.)
- Helm chart integration
- Custom Resource Definitions (CRDs)
- Operator integration

**Example**:
```typescript
import * as k8s from "@pulumi/kubernetes";

// Deploy nginx with replicas
const appLabels = { app: "nginx" };
const deployment = new k8s.apps.v1.Deployment("nginx", {
    spec: {
        selector: { matchLabels: appLabels },
        replicas: 3,
        template: {
            metadata: { labels: appLabels },
            spec: {
                containers: [{
                    name: "nginx",
                    image: "nginx:latest",
                    ports: [{ containerPort: 80 }]
                }]
            }
        }
    }
});

const service = new k8s.core.v1.Service("nginx", {
    spec: {
        type: "LoadBalancer",
        selector: appLabels,
        ports: [{ port: 80, targetPort: 80 }]
    }
});

export const url = service.status.loadBalancer.ingress[0].hostname;
```

### 3. Serverless Applications

**Scenario**: Build serverless applications with Lambda, API Gateway, and managed services.

**Pulumi Benefits**:
- Define Lambda functions inline or from code
- Automatic packaging and deployment
- API Gateway integration
- Event source configuration
- IAM role management

**Example**:
```typescript
import * as aws from "@pulumi/aws";
import * as awsx from "@pulumi/awsx";

// Lambda function from inline code
const lambda = new aws.lambda.Function("myFunction", {
    runtime: "nodejs18.x",
    code: new pulumi.asset.AssetArchive({
        ".": new pulumi.asset.FileArchive("./app")
    }),
    handler: "index.handler",
    role: role.arn
});

// API Gateway with Lambda integration
const api = new awsx.apigateway.API("myApi", {
    routes: [{
        path: "/hello",
        method: "GET",
        eventHandler: lambda
    }]
});

export const apiUrl = api.url;
```

**Advanced Serverless**:
- Step Functions orchestration
- EventBridge rules
- SQS/SNS integration
- DynamoDB tables
- S3 event triggers

### 4. Policy as Code (CrossGuard)

**Scenario**: Enforce compliance, security, and cost policies across infrastructure.

**Features**:
- Pre-deployment validation
- Compliance frameworks (PCI DSS, ISO 27001, CIS)
- Custom policy authoring
- Policy packs distribution
- Automatic remediation

**Example Policy**:
```typescript
import * as policy from "@pulumi/policy";
import * as aws from "@pulumi/aws";

const policies = new policy.PolicyPack("aws-policies", {
    policies: [{
        name: "s3-no-public-read",
        description: "S3 buckets must not allow public read",
        enforcementLevel: "mandatory",
        validateResource: policy.validateResourceOfType(aws.s3.Bucket, (bucket, args, reportViolation) => {
            if (bucket.acl === "public-read" || bucket.acl === "public-read-write") {
                reportViolation("S3 bucket must not have public read access");
            }
        })
    }, {
        name: "required-tags",
        description: "All resources must have required tags",
        enforcementLevel: "mandatory",
        validateResource: (args, reportViolation) => {
            const requiredTags = ["Environment", "Owner", "CostCenter"];
            const tags = args.props.tags || {};

            requiredTags.forEach(tag => {
                if (!tags[tag]) {
                    reportViolation(`Missing required tag: ${tag}`);
                }
            });
        }
    }]
});
```

**Policy Languages**:
- TypeScript/JavaScript
- Python
- Open Policy Agent (OPA) Rego

**Compliance Ready Policies**: Pre-built policy packs for common compliance frameworks available in the registry.

### 5. CI/CD Integration

**Scenario**: Automate infrastructure deployment through CI/CD pipelines.

**Supported Platforms**:
- GitHub Actions
- GitLab CI/CD
- Jenkins
- Azure DevOps
- CircleCI
- Bitbucket Pipelines
- Travis CI
- And 10+ more

#### GitHub Actions Example:
```yaml
name: Pulumi
on:
  push:
    branches:
      - main
  pull_request:

jobs:
  preview:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: pulumi/actions@v4
        with:
          command: preview
          stack-name: dev
        env:
          PULUMI_ACCESS_TOKEN: ${{ secrets.PULUMI_ACCESS_TOKEN }}
          AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}

  deploy:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    needs: preview
    steps:
      - uses: actions/checkout@v3
      - uses: pulumi/actions@v4
        with:
          command: up
          stack-name: prod
        env:
          PULUMI_ACCESS_TOKEN: ${{ secrets.PULUMI_ACCESS_TOKEN }}
```

#### GitLab CI Example:
```yaml
image: pulumi/pulumi:latest

stages:
  - preview
  - deploy

preview:
  stage: preview
  script:
    - pulumi login
    - pulumi stack select dev
    - pulumi preview
  only:
    - merge_requests

deploy:
  stage: deploy
  script:
    - pulumi login
    - pulumi stack select prod
    - pulumi up --yes
  only:
    - main
```

**Features**:
- Pull request previews with inline comments (GitHub App, GitLab integration)
- Automatic deployment on merge
- Stack management
- Secrets management through CI/CD variables
- Policy enforcement in pipeline

### 6. Automation API Use Cases

**Scenario**: Programmatically manage infrastructure without CLI.

**Common Applications**:

#### A. Self-Service Developer Platforms
```typescript
import * as automation from "@pulumi/pulumi/automation";

// API endpoint for creating developer environments
app.post("/environments", async (req, res) => {
    const { name, config } = req.body;

    const stack = await automation.LocalWorkspace.createOrSelectStack({
        stackName: name,
        projectName: "developer-environments",
        program: async () => {
            // Infrastructure definition
            const vpc = new aws.ec2.Vpc(`${name}-vpc`, {});
            const db = new aws.rds.Instance(`${name}-db`, {});
            // ...
        }
    });

    await stack.setConfig("region", { value: config.region });
    const result = await stack.up();

    res.json({ id: name, outputs: result.outputs });
});
```

#### B. Multi-Tenant SaaS Infrastructure
```typescript
// Provision infrastructure for each customer
async function provisionCustomerInfrastructure(customerId: string) {
    const stack = await automation.LocalWorkspace.createStack({
        stackName: `customer-${customerId}`,
        projectName: "saas-infrastructure",
        program: customerInfraProgram
    });

    await stack.setConfig("customerId", { value: customerId });
    await stack.up({ onOutput: console.log });
}
```

#### C. RESTful Infrastructure APIs
Build APIs that expose infrastructure as REST resources:
- POST /clusters - Create cluster
- GET /clusters/:id - Get cluster info
- PUT /clusters/:id - Update cluster
- DELETE /clusters/:id - Destroy cluster

---

## Best Practices

### 1. Project Organization

#### Single Project vs Multiple Projects

**Single Project (Monolithic)**:
```
my-infrastructure/
├── Pulumi.yaml
├── Pulumi.dev.yaml
├── Pulumi.staging.yaml
├── Pulumi.production.yaml
└── index.ts
```

**Pros**: Simple, easy to start
**Cons**: All stacks share same code, harder to control access

**Multiple Projects (Recommended for Teams)**:
```
infrastructure/
├── networking/
│   ├── Pulumi.yaml
│   └── index.ts
├── database/
│   ├── Pulumi.yaml
│   └── index.ts
└── applications/
    ├── Pulumi.yaml
    └── index.ts
```

**Pros**:
- Better separation of concerns
- Team-specific access control
- Independent versioning
- Clearer dependencies via Stack References

**Cons**: More complexity, requires stack references

#### Repository Strategies

**Mono-repo (Application + Infrastructure)**:
```
my-app/
├── src/           # Application code
├── infrastructure/ # Pulumi code
└── package.json
```
**When**: Small teams, tight coupling between app and infra

**Separate Repos**:
```
my-app/            # Application repo
my-app-infra/      # Infrastructure repo
```
**When**: Different teams manage app vs infra, need separate access control

#### Recommended Structure for Large Deployments

```
infrastructure/
├── shared/              # Shared components
│   ├── vpc/
│   ├── security-groups/
│   └── iam/
├── foundations/         # Per-environment foundations
│   ├── dev/
│   ├── staging/
│   └── production/
├── services/           # Individual services
│   ├── api/
│   ├── web/
│   └── workers/
└── components/         # Reusable components
    ├── database/
    ├── cache/
    └── cdn/
```

### 2. Stack Management

#### Stack Naming Conventions
```bash
# Format: <environment>[-<region>][-<variant>]
pulumi stack init dev
pulumi stack init staging-us-west-2
pulumi stack init production-eu-central-1
pulumi stack init production-us-east-1-canary
```

#### Stack Configuration Best Practices

**Use Type-Safe Config**:
```typescript
interface Config {
    region: string;
    instanceType: string;
    minInstances: number;
    tags: { [key: string]: string };
}

const config = new pulumi.Config();
const appConfig: Config = config.requireObject<Config>("app");
```

**Environment-Specific Values**:
```yaml
# Pulumi.dev.yaml
config:
  app:region: us-west-2
  app:instanceType: t3.micro
  app:minInstances: 1

# Pulumi.production.yaml
config:
  app:region: us-east-1
  app:instanceType: m5.large
  app:minInstances: 3
```

### 3. Testing Strategies

#### Unit Tests

**Purpose**: Fast, in-memory tests with mocked cloud calls

**Example (TypeScript with Mocha)**:
```typescript
import * as pulumi from "@pulumi/pulumi";
import { describe, it } from "mocha";
import * as assert from "assert";

pulumi.runtime.setMocks({
    newResource: (args) => {
        return {
            id: `${args.name}_id`,
            state: args.inputs
        };
    },
    call: (args) => {
        return args.inputs;
    }
});

describe("Infrastructure", () => {
    let resources: any;

    before(async () => {
        resources = await import("./index");
    });

    it("should create S3 bucket with versioning", (done) => {
        pulumi.all([resources.bucket.urn, resources.bucket.versioning])
            .apply(([urn, versioning]) => {
                assert.strictEqual(versioning.enabled, true);
                done();
            });
    });

    it("should have correct tags", (done) => {
        pulumi.all([resources.bucket.tags]).apply(([tags]) => {
            assert.strictEqual(tags["Environment"], "production");
            done();
        });
    });
});
```

**Example (Python with pytest)**:
```python
import pulumi
import pytest
from unittest import mock

class MyMocks(pulumi.runtime.Mocks):
    def new_resource(self, args):
        return [args.name + '_id', args.inputs]

    def call(self, args):
        return {}

pulumi.runtime.set_mocks(MyMocks())

@pytest.mark.asyncio
async def test_bucket_versioning():
    import infrastructure

    bucket_versioning = await infrastructure.bucket.versioning.future()
    assert bucket_versioning['enabled'] == True
```

#### Property Tests

**Purpose**: Runtime validation during deployment

**Example**:
```typescript
import * as policy from "@pulumi/policy";
import * as aws from "@pulumi/aws";

new policy.PolicyPack("tests", {
    policies: [{
        name: "bucket-versioning-enabled",
        description: "S3 buckets must have versioning enabled",
        enforcementLevel: "mandatory",
        validateResource: policy.validateResourceOfType(
            aws.s3.Bucket,
            (bucket, args, reportViolation) => {
                if (!bucket.versioning || !bucket.versioning.enabled) {
                    reportViolation("S3 bucket must have versioning enabled");
                }
            }
        )
    }]
});
```

#### Integration Tests

**Purpose**: Deploy to real cloud, validate behavior

**Example (Go)**:
```go
package test

import (
    "testing"
    "github.com/pulumi/pulumi/pkg/v3/testing/integration"
)

func TestInfrastructure(t *testing.T) {
    integration.ProgramTest(t, &integration.ProgramTestOptions{
        Dir: "../",
        Quick: true,
        ExtraRuntimeValidation: func(t *testing.T, stack integration.RuntimeValidationStackInfo) {
            // Validate outputs
            url := stack.Outputs["websiteUrl"].(string)
            assert.NotEmpty(t, url)

            // Test actual endpoint
            resp, err := http.Get(url)
            assert.NoError(t, err)
            assert.Equal(t, 200, resp.StatusCode)
        },
    })
}
```

**Best Practices**:
- Run integration tests in ephemeral environments
- Clean up resources after tests
- Use separate stacks for testing
- Integrate into CI/CD pipeline

### 4. Secret Handling

#### Best Practices

**DO**:
- ✅ Use `--secret` flag for sensitive values
- ✅ Use secrets providers (AWS KMS, Azure Key Vault, etc.)
- ✅ Reference secrets from external vaults via Pulumi ESC
- ✅ Rotate credentials regularly
- ✅ Use short-lived credentials via OIDC
- ✅ Audit secret access

**DON'T**:
- ❌ Hard-code secrets in code
- ❌ Commit `.env` files with secrets
- ❌ Use plain text for passwords
- ❌ Share secrets in chat/email
- ❌ Use long-lived credentials

#### Pulumi ESC for Secrets
```yaml
# Environment: production
values:
  database:
    password:
      fn::secret:
        fn::open::aws-secrets:
          region: us-east-1
          get:
            secretId: prod-db-password

  aws:
    creds:
      fn::open::aws-login:
        oidc:
          roleArn: arn:aws:iam::123456789:role/pulumi-prod
          sessionName: pulumi-prod
```

### 5. Resource Naming Conventions

#### Auto-Naming

**Default Behavior**: Pulumi adds random suffix
```typescript
const bucket = new aws.s3.Bucket("my-bucket");
// Physical name: my-bucket-a1b2c3d
```

**Why**: Enables zero-downtime replacements and multi-stack deployments

#### Custom Naming Patterns

**Configure Auto-Naming**:
```yaml
# Pulumi.yaml
config:
  pulumi:autonaming:
    pattern: "${project}-${stack}-${name}-${alphanum(6)}"
```

**Explicit Names**:
```typescript
const bucket = new aws.s3.Bucket("my-bucket", {
    bucket: "company-production-assets"  // Exact name
});
```

**Best Practice**: Use Pulumi logical names, let Pulumi manage physical names unless required

#### Naming Standards
```typescript
// Resource prefix pattern
const prefix = `${organization}-${tenant}-${environment}`;

const vpc = new aws.ec2.Vpc(`${prefix}-vpc`, {});
const subnet = new aws.ec2.Subnet(`${prefix}-subnet-public-1a`, {});
const instance = new aws.ec2.Instance(`${prefix}-web-server-1`, {});
```

### 6. Tagging Standards

#### Default Tags via Configuration
```yaml
# Pulumi.yaml
config:
  aws:defaultTags:
    tags:
      Environment: production
      ManagedBy: pulumi
      CostCenter: engineering
      Owner: platform-team
```

#### Default Tags via Provider
```typescript
const awsProvider = new aws.Provider("custom", {
    region: "us-west-2",
    defaultTags: {
        tags: {
            Environment: pulumi.getStack(),
            ManagedBy: "pulumi",
            Project: pulumi.getProject()
        }
    }
});

const bucket = new aws.s3.Bucket("bucket", {}, { provider: awsProvider });
// Automatically gets default tags
```

#### Enforce Tagging with Policy
```typescript
const tagPolicy = {
    name: "required-tags",
    validateResource: (args, reportViolation) => {
        const required = ["Environment", "Owner", "CostCenter"];
        const tags = args.props.tags || {};

        required.forEach(tag => {
            if (!tags[tag]) {
                reportViolation(`Missing required tag: ${tag}`);
            }
        });
    }
};
```

### 7. Code Organization Patterns

#### Separate Concerns
```typescript
// Bad: Everything in one file
const vpc = new aws.ec2.Vpc(...);
const subnet = new aws.ec2.Subnet(...);
const instance = new aws.ec2.Instance(...);
const database = new aws.rds.Instance(...);
const lambda = new aws.lambda.Function(...);

// Good: Organized by concern
// network.ts
export class Network extends pulumi.ComponentResource {
    public vpc: aws.ec2.Vpc;
    public subnets: aws.ec2.Subnet[];
    // ...
}

// database.ts
export class Database extends pulumi.ComponentResource {
    public instance: aws.rds.Instance;
    // ...
}

// compute.ts
export class Compute extends pulumi.ComponentResource {
    public instances: aws.ec2.Instance[];
    // ...
}

// index.ts
const network = new Network("network", {});
const database = new Database("database", { vpcId: network.vpc.id });
const compute = new Compute("compute", { vpcId: network.vpc.id });
```

#### Use Component Resources for Reusability
```typescript
export interface WebServiceArgs {
    vpcId: pulumi.Input<string>;
    subnetIds: pulumi.Input<string[]>;
    instanceType: pulumi.Input<string>;
    minSize: number;
    maxSize: number;
}

export class WebService extends pulumi.ComponentResource {
    public loadBalancer: aws.lb.LoadBalancer;
    public targetGroup: aws.lb.TargetGroup;
    public autoScalingGroup: aws.autoscaling.Group;

    constructor(name: string, args: WebServiceArgs, opts?: pulumi.ComponentResourceOptions) {
        super("custom:service:WebService", name, args, opts);

        // Create resources
        this.loadBalancer = new aws.lb.LoadBalancer(`${name}-lb`, {
            subnets: args.subnetIds
        }, { parent: this });

        this.targetGroup = new aws.lb.TargetGroup(`${name}-tg`, {
            vpcId: args.vpcId,
            port: 80,
            protocol: "HTTP"
        }, { parent: this });

        // ... more resources

        this.registerOutputs({
            url: this.loadBalancer.dnsName
        });
    }
}

// Usage
const webService = new WebService("api", {
    vpcId: vpc.id,
    subnetIds: subnets.map(s => s.id),
    instanceType: "t3.medium",
    minSize: 2,
    maxSize: 10
});
```

### 8. Performance Optimization

#### Parallel Resource Creation
```typescript
// Bad: Sequential creation
const buckets = [];
for (let i = 0; i < 10; i++) {
    buckets.push(new aws.s3.Bucket(`bucket-${i}`));
}

// Good: Parallel (Pulumi handles this automatically)
const buckets = Array.from({ length: 10 }, (_, i) =>
    new aws.s3.Bucket(`bucket-${i}`)
);
```

#### Minimize State File Size
- Don't export unnecessary stack outputs
- Use stack references instead of duplicating resources
- Consider splitting large stacks

#### Use Explicit Providers for Better Control
```typescript
const provider = new aws.Provider("optimized", {
    region: "us-west-2",
    skipMetadataApiCheck: true,
    skipCredentialsValidation: true
});
```

### 9. Error Handling and Debugging

#### Enable Verbose Logging
```bash
pulumi up --logtostderr -v=9  # Maximum verbosity
```

#### Handle Errors in Dynamic Providers
```typescript
const provider: pulumi.dynamic.ResourceProvider = {
    async create(inputs) {
        try {
            const response = await fetch(...);
            if (!response.ok) {
                throw new Error(`API error: ${response.status}`);
            }
            return { id: response.id, outs: inputs };
        } catch (error) {
            throw new Error(`Failed to create resource: ${error.message}`);
        }
    }
};
```

#### Use Protected Resources for Production
```typescript
const productionDatabase = new aws.rds.Instance("prod-db", {
    // ...
}, {
    protect: true,  // Prevent accidental deletion
    ignoreChanges: ["password"]  // Don't update password via Pulumi
});
```

---

## Code Examples

### Complete Examples by Language

#### TypeScript: AWS VPC with EC2 Instance

```typescript
import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";

// Configuration
const config = new pulumi.Config();
const instanceType = config.get("instanceType") || "t3.micro";
const availabilityZone = config.get("availabilityZone") || "us-west-2a";

// VPC
const vpc = new aws.ec2.Vpc("main-vpc", {
    cidrBlock: "10.0.0.0/16",
    enableDnsHostnames: true,
    enableDnsSupport: true,
    tags: {
        Name: "main-vpc",
        Environment: pulumi.getStack()
    }
});

// Internet Gateway
const igw = new aws.ec2.InternetGateway("main-igw", {
    vpcId: vpc.id,
    tags: { Name: "main-igw" }
});

// Subnet
const subnet = new aws.ec2.Subnet("public-subnet", {
    vpcId: vpc.id,
    cidrBlock: "10.0.1.0/24",
    availabilityZone: availabilityZone,
    mapPublicIpOnLaunch: true,
    tags: { Name: "public-subnet" }
});

// Route Table
const routeTable = new aws.ec2.RouteTable("public-rt", {
    vpcId: vpc.id,
    routes: [{
        cidrBlock: "0.0.0.0/0",
        gatewayId: igw.id
    }],
    tags: { Name: "public-rt" }
});

const routeTableAssociation = new aws.ec2.RouteTableAssociation("public-rta", {
    subnetId: subnet.id,
    routeTableId: routeTable.id
});

// Security Group
const securityGroup = new aws.ec2.SecurityGroup("web-sg", {
    vpcId: vpc.id,
    description: "Allow HTTP and SSH",
    ingress: [
        { protocol: "tcp", fromPort: 80, toPort: 80, cidrBlocks: ["0.0.0.0/0"] },
        { protocol: "tcp", fromPort: 22, toPort: 22, cidrBlocks: ["0.0.0.0/0"] }
    ],
    egress: [{
        protocol: "-1",
        fromPort: 0,
        toPort: 0,
        cidrBlocks: ["0.0.0.0/0"]
    }],
    tags: { Name: "web-sg" }
});

// Get latest Amazon Linux 2 AMI
const ami = aws.ec2.getAmi({
    mostRecent: true,
    owners: ["amazon"],
    filters: [{
        name: "name",
        values: ["amzn2-ami-hvm-*-x86_64-gp2"]
    }]
});

// EC2 Instance
const instance = new aws.ec2.Instance("web-server", {
    ami: ami.then(ami => ami.id),
    instanceType: instanceType,
    subnetId: subnet.id,
    vpcSecurityGroupIds: [securityGroup.id],
    userData: `#!/bin/bash
        yum update -y
        yum install -y httpd
        systemctl start httpd
        systemctl enable httpd
        echo "<h1>Hello from Pulumi!</h1>" > /var/www/html/index.html
    `,
    tags: {
        Name: "web-server",
        Environment: pulumi.getStack()
    }
});

// Exports
export const vpcId = vpc.id;
export const subnetId = subnet.id;
export const instanceId = instance.id;
export const publicIp = instance.publicIp;
export const publicDns = instance.publicDns;
export const url = pulumi.interpolate`http://${instance.publicDns}`;
```

#### Python: S3 Static Website

```python
import pulumi
import pulumi_aws as aws
import json

# Configuration
config = pulumi.Config()
site_dir = config.get("siteDir") or "./website"

# S3 Bucket
bucket = aws.s3.Bucket(
    "website-bucket",
    website=aws.s3.BucketWebsiteArgs(
        index_document="index.html",
        error_document="error.html"
    ),
    tags={
        "Name": "website-bucket",
        "Environment": pulumi.get_stack()
    }
)

# Bucket ownership controls
ownership_controls = aws.s3.BucketOwnershipControls(
    "ownership-controls",
    bucket=bucket.id,
    rule=aws.s3.BucketOwnershipControlsRuleArgs(
        object_ownership="BucketOwnerPreferred"
    )
)

# Public access block (allow public access)
public_access_block = aws.s3.BucketPublicAccessBlock(
    "public-access-block",
    bucket=bucket.id,
    block_public_acls=False,
    block_public_policy=False,
    ignore_public_acls=False,
    restrict_public_buckets=False
)

# Bucket policy for public read
bucket_policy = aws.s3.BucketPolicy(
    "bucket-policy",
    bucket=bucket.id,
    policy=bucket.arn.apply(
        lambda arn: json.dumps({
            "Version": "2012-10-17",
            "Statement": [{
                "Effect": "Allow",
                "Principal": "*",
                "Action": "s3:GetObject",
                "Resource": f"{arn}/*"
            }]
        })
    )
)

# Upload files
import os
for file in os.listdir(site_dir):
    filepath = os.path.join(site_dir, file)
    if os.path.isfile(filepath):
        content_type = "text/html" if file.endswith(".html") else "text/plain"
        aws.s3.BucketObject(
            file,
            bucket=bucket.id,
            source=pulumi.FileAsset(filepath),
            content_type=content_type,
            acl="public-read"
        )

# CloudFront distribution (optional)
cdn = aws.cloudfront.Distribution(
    "cdn",
    enabled=True,
    origins=[aws.cloudfront.DistributionOriginArgs(
        origin_id=bucket.arn,
        domain_name=bucket.website_endpoint,
        custom_origin_config=aws.cloudfront.DistributionOriginCustomOriginConfigArgs(
            origin_protocol_policy="http-only",
            http_port=80,
            https_port=443,
            origin_ssl_protocols=["TLSv1.2"]
        )
    )],
    default_cache_behavior=aws.cloudfront.DistributionDefaultCacheBehaviorArgs(
        target_origin_id=bucket.arn,
        viewer_protocol_policy="redirect-to-https",
        allowed_methods=["GET", "HEAD", "OPTIONS"],
        cached_methods=["GET", "HEAD", "OPTIONS"],
        forwarded_values=aws.cloudfront.DistributionDefaultCacheBehaviorForwardedValuesArgs(
            query_string=False,
            cookies=aws.cloudfront.DistributionDefaultCacheBehaviorForwardedValuesCookiesArgs(
                forward="none"
            )
        ),
        min_ttl=0,
        default_ttl=3600,
        max_ttl=86400
    ),
    restrictions=aws.cloudfront.DistributionRestrictionsArgs(
        geo_restriction=aws.cloudfront.DistributionRestrictionsGeoRestrictionArgs(
            restriction_type="none"
        )
    ),
    viewer_certificate=aws.cloudfront.DistributionViewerCertificateArgs(
        cloudfront_default_certificate=True
    )
)

# Exports
pulumi.export("bucket_name", bucket.id)
pulumi.export("website_url", bucket.website_endpoint)
pulumi.export("cdn_url", cdn.domain_name)
```

#### Go: Kubernetes Deployment

```go
package main

import (
    "github.com/pulumi/pulumi-kubernetes/sdk/v4/go/kubernetes"
    appsv1 "github.com/pulumi/pulumi-kubernetes/sdk/v4/go/kubernetes/apps/v1"
    corev1 "github.com/pulumi/pulumi-kubernetes/sdk/v4/go/kubernetes/core/v1"
    metav1 "github.com/pulumi/pulumi-kubernetes/sdk/v4/go/kubernetes/meta/v1"
    "github.com/pulumi/pulumi/sdk/v3/go/pulumi"
    "github.com/pulumi/pulumi/sdk/v3/go/pulumi/config"
)

func main() {
    pulumi.Run(func(ctx *pulumi.Context) error {
        // Configuration
        cfg := config.New(ctx, "")
        replicas := cfg.GetInt("replicas")
        if replicas == 0 {
            replicas = 3
        }

        // Namespace
        namespace, err := corev1.NewNamespace(ctx, "app-namespace", &corev1.NamespaceArgs{
            Metadata: &metav1.ObjectMetaArgs{
                Name: pulumi.String("my-app"),
            },
        })
        if err != nil {
            return err
        }

        // Labels
        appLabels := pulumi.StringMap{
            "app": pulumi.String("nginx"),
        }

        // Deployment
        deployment, err := appsv1.NewDeployment(ctx, "app-deployment", &appsv1.DeploymentArgs{
            Metadata: &metav1.ObjectMetaArgs{
                Namespace: namespace.Metadata.Name(),
                Name:      pulumi.String("nginx-deployment"),
            },
            Spec: &appsv1.DeploymentSpecArgs{
                Replicas: pulumi.Int(replicas),
                Selector: &metav1.LabelSelectorArgs{
                    MatchLabels: appLabels,
                },
                Template: &corev1.PodTemplateSpecArgs{
                    Metadata: &metav1.ObjectMetaArgs{
                        Labels: appLabels,
                    },
                    Spec: &corev1.PodSpecArgs{
                        Containers: corev1.ContainerArray{
                            &corev1.ContainerArgs{
                                Name:  pulumi.String("nginx"),
                                Image: pulumi.String("nginx:1.21"),
                                Ports: corev1.ContainerPortArray{
                                    &corev1.ContainerPortArgs{
                                        ContainerPort: pulumi.Int(80),
                                    },
                                },
                                Resources: &corev1.ResourceRequirementsArgs{
                                    Requests: pulumi.StringMap{
                                        "cpu":    pulumi.String("100m"),
                                        "memory": pulumi.String("128Mi"),
                                    },
                                    Limits: pulumi.StringMap{
                                        "cpu":    pulumi.String("500m"),
                                        "memory": pulumi.String("512Mi"),
                                    },
                                },
                            },
                        },
                    },
                },
            },
        })
        if err != nil {
            return err
        }

        // Service
        service, err := corev1.NewService(ctx, "app-service", &corev1.ServiceArgs{
            Metadata: &metav1.ObjectMetaArgs{
                Namespace: namespace.Metadata.Name(),
                Name:      pulumi.String("nginx-service"),
            },
            Spec: &corev1.ServiceSpecArgs{
                Type:     pulumi.String("LoadBalancer"),
                Selector: appLabels,
                Ports: corev1.ServicePortArray{
                    &corev1.ServicePortArgs{
                        Port:       pulumi.Int(80),
                        TargetPort: pulumi.Int(80),
                        Protocol:   pulumi.String("TCP"),
                    },
                },
            },
        })
        if err != nil {
            return err
        }

        // Exports
        ctx.Export("namespaceName", namespace.Metadata.Name())
        ctx.Export("deploymentName", deployment.Metadata.Name())
        ctx.Export("serviceName", service.Metadata.Name())
        ctx.Export("serviceType", service.Spec.Type())

        // Export LoadBalancer URL
        ctx.Export("url", service.Status.ApplyT(func(status *corev1.ServiceStatus) string {
            if status != nil && len(status.LoadBalancer.Ingress) > 0 {
                ingress := status.LoadBalancer.Ingress[0]
                if ingress.Hostname != nil {
                    return *ingress.Hostname
                }
                if ingress.Ip != nil {
                    return *ingress.Ip
                }
            }
            return ""
        }))

        return nil
    })
}
```

### Component Resource Example

```typescript
// reusable-web-service.ts
import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";

export interface WebServiceArgs {
    vpcId: pulumi.Input<string>;
    publicSubnetIds: pulumi.Input<pulumi.Input<string>[]>;
    privateSubnetIds: pulumi.Input<pulumi.Input<string>[]>;
    instanceType: pulumi.Input<string>;
    minSize: number;
    maxSize: number;
    desiredCapacity: number;
    healthCheckPath?: string;
}

export class WebService extends pulumi.ComponentResource {
    public readonly loadBalancer: aws.lb.LoadBalancer;
    public readonly targetGroup: aws.lb.TargetGroup;
    public readonly launchTemplate: aws.ec2.LaunchTemplate;
    public readonly autoScalingGroup: aws.autoscaling.Group;
    public readonly url: pulumi.Output<string>;

    constructor(
        name: string,
        args: WebServiceArgs,
        opts?: pulumi.ComponentResourceOptions
    ) {
        super("custom:service:WebService", name, {}, opts);

        const resourceOpts = { parent: this };

        // Security Group for ALB
        const albSg = new aws.ec2.SecurityGroup(`${name}-alb-sg`, {
            vpcId: args.vpcId,
            description: "Security group for ALB",
            ingress: [
                { protocol: "tcp", fromPort: 80, toPort: 80, cidrBlocks: ["0.0.0.0/0"] },
                { protocol: "tcp", fromPort: 443, toPort: 443, cidrBlocks: ["0.0.0.0/0"] }
            ],
            egress: [{
                protocol: "-1",
                fromPort: 0,
                toPort: 0,
                cidrBlocks: ["0.0.0.0/0"]
            }]
        }, resourceOpts);

        // Security Group for Instances
        const instanceSg = new aws.ec2.SecurityGroup(`${name}-instance-sg`, {
            vpcId: args.vpcId,
            description: "Security group for instances",
            ingress: [{
                protocol: "tcp",
                fromPort: 80,
                toPort: 80,
                securityGroups: [albSg.id]
            }],
            egress: [{
                protocol: "-1",
                fromPort: 0,
                toPort: 0,
                cidrBlocks: ["0.0.0.0/0"]
            }]
        }, resourceOpts);

        // Application Load Balancer
        this.loadBalancer = new aws.lb.LoadBalancer(`${name}-alb`, {
            internal: false,
            loadBalancerType: "application",
            securityGroups: [albSg.id],
            subnets: args.publicSubnetIds,
            tags: { Name: `${name}-alb` }
        }, resourceOpts);

        // Target Group
        this.targetGroup = new aws.lb.TargetGroup(`${name}-tg`, {
            port: 80,
            protocol: "HTTP",
            vpcId: args.vpcId,
            healthCheck: {
                enabled: true,
                path: args.healthCheckPath || "/",
                interval: 30,
                timeout: 5,
                healthyThreshold: 2,
                unhealthyThreshold: 2
            },
            tags: { Name: `${name}-tg` }
        }, resourceOpts);

        // Listener
        new aws.lb.Listener(`${name}-listener`, {
            loadBalancerArn: this.loadBalancer.arn,
            port: 80,
            protocol: "HTTP",
            defaultActions: [{
                type: "forward",
                targetGroupArn: this.targetGroup.arn
            }]
        }, resourceOpts);

        // Get latest AMI
        const ami = aws.ec2.getAmi({
            mostRecent: true,
            owners: ["amazon"],
            filters: [{ name: "name", values: ["amzn2-ami-hvm-*-x86_64-gp2"] }]
        });

        // IAM Role for instances
        const role = new aws.iam.Role(`${name}-role`, {
            assumeRolePolicy: JSON.stringify({
                Version: "2012-10-17",
                Statement: [{
                    Action: "sts:AssumeRole",
                    Effect: "Allow",
                    Principal: { Service: "ec2.amazonaws.com" }
                }]
            })
        }, resourceOpts);

        new aws.iam.RolePolicyAttachment(`${name}-policy`, {
            role: role.name,
            policyArn: "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
        }, resourceOpts);

        const instanceProfile = new aws.iam.InstanceProfile(`${name}-profile`, {
            role: role.name
        }, resourceOpts);

        // Launch Template
        this.launchTemplate = new aws.ec2.LaunchTemplate(`${name}-lt`, {
            imageId: ami.then(ami => ami.id),
            instanceType: args.instanceType,
            iamInstanceProfile: { arn: instanceProfile.arn },
            vpcSecurityGroupIds: [instanceSg.id],
            userData: pulumi.output(`#!/bin/bash
                yum update -y
                yum install -y httpd
                systemctl start httpd
                systemctl enable httpd
                echo "<h1>Hello from ${name}!</h1>" > /var/www/html/index.html
            `).apply(script => Buffer.from(script).toString('base64')),
            tagSpecifications: [{
                resourceType: "instance",
                tags: { Name: `${name}-instance` }
            }]
        }, resourceOpts);

        // Auto Scaling Group
        this.autoScalingGroup = new aws.autoscaling.Group(`${name}-asg`, {
            vpcZoneIdentifiers: args.privateSubnetIds,
            targetGroupArns: [this.targetGroup.arn],
            minSize: args.minSize,
            maxSize: args.maxSize,
            desiredCapacity: args.desiredCapacity,
            healthCheckType: "ELB",
            healthCheckGracePeriod: 300,
            launchTemplate: {
                id: this.launchTemplate.id,
                version: "$Latest"
            },
            tags: [{
                key: "Name",
                value: `${name}-asg-instance`,
                propagateAtLaunch: true
            }]
        }, resourceOpts);

        // Auto Scaling Policies
        new aws.autoscaling.Policy(`${name}-scale-up`, {
            autoscalingGroupName: this.autoScalingGroup.name,
            policyType: "TargetTrackingScaling",
            targetTrackingConfiguration: {
                predefinedMetricSpecification: {
                    predefinedMetricType: "ASGAverageCPUUtilization"
                },
                targetValue: 70
            }
        }, resourceOpts);

        this.url = pulumi.interpolate`http://${this.loadBalancer.dnsName}`;

        this.registerOutputs({
            loadBalancerDns: this.loadBalancer.dnsName,
            targetGroupArn: this.targetGroup.arn,
            url: this.url
        });
    }
}

// Usage
const webService = new WebService("my-web-service", {
    vpcId: vpc.id,
    publicSubnetIds: publicSubnets.map(s => s.id),
    privateSubnetIds: privateSubnets.map(s => s.id),
    instanceType: "t3.medium",
    minSize: 2,
    maxSize: 10,
    desiredCapacity: 3,
    healthCheckPath: "/health"
});

export const serviceUrl = webService.url;
```

---

## Additional Resources

### Official Documentation
- **Pulumi Docs**: https://www.pulumi.com/docs/
- **Pulumi Registry**: https://www.pulumi.com/registry/
- **GitHub Repository**: https://github.com/pulumi/pulumi
- **Examples Repository**: https://github.com/pulumi/examples

### Community
- **Pulumi Community Slack**: https://slack.pulumi.com
- **Stack Overflow**: Tag `pulumi`
- **GitHub Discussions**: https://github.com/pulumi/pulumi/discussions

### Learning Resources
- **Pulumi Tutorials**: https://www.pulumi.com/tutorials/
- **Pulumi Blog**: https://www.pulumi.com/blog/
- **YouTube Channel**: Pulumi TV

### Tools and Integrations
- **Pulumi Cloud**: https://app.pulumi.com
- **Pulumi ESC**: https://www.pulumi.com/product/secrets-management/
- **Pulumi Deployments**: https://www.pulumi.com/product/pulumi-deployments/
- **Pulumi CrossGuard**: https://www.pulumi.com/crossguard/
- **Pulumi AI**: https://www.pulumi.com/ai/

---

## Summary

Pulumi is a powerful, modern infrastructure as code platform that enables developers to manage cloud infrastructure using familiar programming languages. Key takeaways for LLMs working with Pulumi:

1. **Language Choice**: Users can write in TypeScript, Python, Go, .NET, Java, or YAML - all equally capable
2. **Resource Types**: Understand the distinction between custom resources (cloud resources) and component resources (abstractions)
3. **Inputs/Outputs**: Master the Output type and dependency tracking mechanism
4. **State Management**: Choose between Pulumi Cloud or self-managed backends
5. **Configuration**: Use stack-specific config files and secrets management
6. **Testing**: Implement unit, property, and integration tests
7. **Organization**: Structure projects appropriately for team size and requirements
8. **Best Practices**: Follow naming conventions, tagging standards, and security practices
9. **Automation**: Leverage Automation API for programmatic infrastructure management
10. **Ecosystem**: Utilize 120+ providers, CrossGuard policies, and ESC for secrets

This comprehensive guide provides the foundational knowledge needed to effectively assist developers working with Pulumi infrastructure as code.
