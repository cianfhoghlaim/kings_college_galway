# DLT Hub Codebase Analysis

## Overview
The dlthub codebase in `/home/user/hackathon/data-unified` is a sophisticated data engineering platform that integrates multiple data pipelines and orchestration frameworks. It demonstrates advanced Python patterns for building scalable, extensible data platforms.

## 1. Design Patterns

### 1.1 Decorator Pattern

**Usage**: Extensively used in DLT and Dagster for declaring resources and assets.

**Key Examples**:

```python
# DLT Resource Decorator
@dlt.resource(write_disposition="merge", primary_key="full_name")
def github_repos_resource(repos=None, github_token=None):
    # Resource implementation
    yield data

# DLT Source Decorator
@dlt.source
def github_repos_source(repos=None, github_token=None):
    return github_repos_resource(repos=repos, github_token=github_token)

# Dagster Asset Decorator
@asset(
    description="...",
    group_name="github_pipeline",
    deps=[github_repos],
)
def r2_uploaded_repos(context: AssetExecutionContext, github_repos):
    # Asset implementation
    return result
```

**File Locations**:
- `/home/user/hackathon/data-unified/pipelines/github_to_r2/clone_repo.py` (lines 53-144)
- `/home/user/hackathon/data-unified/pipelines/github_to_r2/upload_r2.py` (lines 116-159)
- `/home/user/hackathon/data-unified/pipelines/docs_to_knowledge/scrape_docs.py` (lines 76-128)
- `/home/user/hackathon/data-unified/pipelines/github_to_r2/dagster_assets.py` (lines 16-135)

### 1.2 Builder Pattern

**Usage**: Used for configuring complex flows and pipelines.

**Key Example**: `CodeIndexConfig` dataclass

```python
@dataclass
class CodeIndexConfig:
    """Configuration for code indexing."""
    repo_owner: str
    repo_name: str
    branch: str
    r2_object_prefix: str
    path: Optional[str] = None
    included_patterns: Optional[list[str]] = None
    excluded_patterns: Optional[list[str]] = None
```

**File Location**: `/home/user/hackathon/data-unified/pipelines/github_to_r2/index_cocoindex.py` (lines 21-32)

### 1.3 Factory Pattern

**Usage**: Creating destinations and clients dynamically.

```python
def create_r2_destination(bucket_name=None, endpoint_url=None, ...):
    """Create a DLT destination for Cloudflare R2."""
    return dlt.destinations.filesystem(
        bucket_url=f"s3://{bucket_name}",
        credentials={...}
    )

def create_duckdb_destination(database_path=None):
    """Create a DLT destination for DuckDB."""
    return dlt.destinations.duckdb(database_path)
```

**File Location**: `/home/user/hackathon/data-unified/pipelines/shared/dlt_sources.py` (lines 7-59)

### 1.4 Repository/Client Pattern

**Usage**: Encapsulating external service interactions.

```python
class R2Client:
    """Client for interacting with Cloudflare R2 storage."""
    
    def __init__(self, access_key_id=None, ...):
        self.s3_client = boto3.client("s3", ...)
    
    def upload_file(self, file_path, object_name=None, ...):
        # Implementation
        
    def download_file(self, object_name, file_path, ...):
        # Implementation
```

**File Location**: `/home/user/hackathon/data-unified/pipelines/shared/r2_client.py` (lines 11-196)

### 1.5 Strategy Pattern

**Usage**: Different ways to process and index data.

- **GitHub cloning strategy**: `clone_github_repo()` function
- **Upload strategy**: `upload_repo_to_r2()` function
- **Indexing strategy**: `index_from_r2()` function
- **Documentation scraping strategy**: `scrape_documentation()` function

**File Locations**:
- `/home/user/hackathon/data-unified/pipelines/github_to_r2/clone_repo.py` (lines 15-50)
- `/home/user/hackathon/data-unified/pipelines/github_to_r2/upload_r2.py` (lines 35-108)
- `/home/user/hackathon/data-unified/pipelines/docs_to_knowledge/scrape_docs.py` (lines 13-73)

---

## 2. Data Ontology & Schema System

### 2.1 Type System Foundation

The codebase uses Pydantic v2 for runtime type validation and schema definition.

**Base Type Definitions** (`/home/user/hackathon/data-unified/models/classes.py`):

```python
# Source type literals
SourceType = Literal[
    "github_api",
    "repository_clone",
    "docs_site",
    "issue_thread",
    "release",
    "workflow_run",
]

# Collection method literals
CollectionMethod = Literal[
    "rest_api",
    "graphql_api",
    "git_clone",
    "local_file",
    "manual_upload",
]
```

### 2.2 Core Data Models

**SourceInfo** - Metadata about data source:
```python
class SourceInfo(BaseModel):
    type: SourceType
    collection_method: CollectionMethod
    owner: Optional[str]
    repository: Optional[str]
    ref: Optional[str]  # branch, tag, or commit SHA
    api_endpoint: Optional[str]
    doc_url: Optional[str]
    path: Optional[str]
    description: Optional[str]
```

**RetrievalMetadata** - Tracking retrieval information:
```python
class RetrievalMetadata(BaseModel):
    ingested_at: Optional[datetime]
    sha: Optional[str]
    etag: Optional[str]
    size_bytes: Optional[int]
    content_type: Optional[str]
    toolchain: Optional[str]
```

**RepositoryAnalysis** - Repository intelligence:
```python
class RepositoryAnalysis(BaseModel):
    primary_language: Optional[str]
    languages: Optional[List[LanguageStat]]
    dependency_files: Optional[List[str]]
    services_detected: Optional[List[str]]
    key_workflows: Optional[List[str]]
    risk_flags: Optional[List[str]]
    notes: Optional[str]
```

**DataItem** - Unified data representation:
```python
class DataItem(BaseModel):
    id: Optional[str]
    source: SourceInfo
    retrieval: RetrievalMetadata
    endpoint: Optional[EndpointDescriptor]
    repository: Optional[RepositoryAnalysis]
    documentation: Optional[DocumentationNode]
    semantic: SemanticInfo
    content: Optional[str]
```

**File Location**: `/home/user/hackathon/data-unified/models/classes.py`

### 2.3 Pipeline Configuration Models

**Priority Enum** - Processing priority levels:
```python
class Priority(str, Enum):
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"
```

**GitHubRepoConfig** - GitHub repository configuration:
```python
class GitHubRepoConfig(BaseModel):
    owner: str
    repo: str
    branch: str = "main"
    path: Optional[str] = None
    included_patterns: Optional[List[str]] = None
    excluded_patterns: Optional[List[str]] = None
    enabled: bool = True
    priority: Priority = Priority.MEDIUM
    
    @property
    def full_name(self) -> str:
        return f"{self.owner}/{self.repo}"
```

**PipelineConfig** - Global pipeline settings:
```python
class PipelineConfig(BaseModel):
    incremental: bool = True
    batch_size: int = 10
    max_concurrent: int = 5
    rate_limit_per_second: float = 1.0
```

**IndexingResult** - Code indexing pipeline results:
```python
class IndexingResult(BaseModel):
    repo_name: str
    indexed_files: int
    total_chunks: int
    embeddings_generated: int
    storage_location: str
    index_location: str
    metadata: Dict[str, Any]
    started_at: datetime
    completed_at: datetime
    success: bool
    errors: List[str]
```

**File Location**: `/home/user/hackathon/data-unified/models/schemas.py`

### 2.4 Configuration-Driven Schema

Configuration is loaded from YAML and merged with Pydantic models:

```python
def load_config(config_path: str = "config/sources.yaml"):
    """Load configuration from YAML file."""
    config_file = project_root / config_path
    with open(config_file, "r") as f:
        config = yaml.safe_load(f)
    return config

def get_sources_config() -> dict[str, Any]:
    """Get software sources configuration."""
    config = load_config("config/sources.yaml")
    return config.get("sources", {})
```

**File Location**: `/home/user/hackathon/data-unified/pipelines/shared/config.py`

---

## 3. Resource Patterns

### 3.1 DLT Resource Pattern

DLT resources are the fundamental unit of data extraction. They follow a consistent pattern:

**Pattern Structure**:
```python
@dlt.resource(write_disposition="merge", primary_key="id")
def my_resource(params=None):
    """DLT resource implementation.
    
    Args:
        params: Optional configuration parameters
        
    Yields:
        Data items to be loaded
    """
    # 1. Handle configuration
    if params is None:
        params = load_config()
    
    # 2. Implement extraction logic
    for item in extract_data(params):
        # 3. Transform if needed
        transformed = transform(item)
        # 4. Yield results
        yield transformed
```

**Real Examples**:

**GitHub Repos Resource** (`/home/user/hackathon/data-unified/pipelines/github_to_r2/clone_repo.py:53-127`):
```python
@dlt.resource(write_disposition="merge", primary_key="full_name")
def github_repos_resource(repos=None, github_token=None):
    """Fetches repository metadata from GitHub API."""
    github_token = github_token or get_env("GITHUB_API_KEY")
    if repos is None:
        repos = get_github_repos_config()
    
    auth = BearerTokenAuth(github_token) if github_token else None
    
    for repo_config in repos:
        if not repo_config.get("enabled", True):
            continue
        
        owner = repo_config["owner"]
        repo = repo_config["repo"]
        
        try:
            response = requests.get(
                f"https://api.github.com/repos/{owner}/{repo}",
                headers={"Authorization": f"Bearer {github_token}"}
            )
            response.raise_for_status()
            repo_data = response.json()
            
            yield {
                "full_name": repo_data["full_name"],
                "owner": owner,
                "repo": repo,
                "stars": repo_data.get("stargazers_count"),
                # ... more fields
            }
        except Exception as e:
            print(f"Error fetching {owner}/{repo}: {e}")
            yield {"full_name": f"{owner}/{repo}", "error": str(e)}
```

**R2 Upload Resource** (`/home/user/hackathon/data-unified/pipelines/github_to_r2/upload_r2.py:116-146`):
```python
@dlt.resource(write_disposition="merge", primary_key="full_name")
def r2_upload_resource(cloned_repos):
    """Resource for uploading repos to R2."""
    r2_client = R2Client()
    
    for repo_info in cloned_repos:
        if not repo_info.get("success", False):
            continue
        
        result = upload_repo_to_r2(
            repo_path=Path(repo_info["clone_path"]),
            owner=repo_info["owner"],
            repo=repo_info["repo"],
            branch=repo_info["branch"],
            r2_client=r2_client,
        )
        
        yield result
```

**Docs Scraper Resource** (`/home/user/hackathon/data-unified/pipelines/docs_to_knowledge/scrape_docs.py:76-111`):
```python
@dlt.resource(write_disposition="merge", primary_key="name")
def docs_scraper_resource(sources=None, max_urls_per_source=50):
    """Resource for scraping documentation sources."""
    if sources is None:
        all_sources = get_sources_config()
        sources = [
            {"name": name, **config}
            for name, config in all_sources.items()
            if config.get("enabled", True) and config.get("docs_url")
        ]
    
    for source in sources:
        result = scrape_documentation(
            name=source["name"],
            docs_url=source["docs_url"],
            max_urls=max_urls_per_source,
        )
        yield result
```

### 3.2 DLT Source Pattern

Sources wrap resources and enable composition:

```python
@dlt.source
def github_repos_source(repos=None, github_token=None):
    """DLT source for GitHub repositories."""
    return github_repos_resource(repos=repos, github_token=github_token)

@dlt.source
def r2_upload_source(cloned_repos):
    """DLT source for R2 uploads."""
    return r2_upload_resource(cloned_repos=cloned_repos)

@dlt.source
def docs_scraper_source(sources=None, max_urls_per_source=50):
    """DLT source for documentation scraping."""
    return docs_scraper_resource(
        sources=sources, 
        max_urls_per_source=max_urls_per_source
    )
```

### 3.3 Write Disposition Pattern

DLT supports different write behaviors:

```python
# Merge (upsert): Update existing records or insert new ones
@dlt.resource(write_disposition="merge", primary_key="full_name")
def mergeable_resource():
    yield data

# Replace: Truncate and reload entire table
pipeline.run(source, write_disposition="replace")

# Append: Only insert new records
pipeline.run(source, write_disposition="append")
```

### 3.4 State Management Pattern

DLT resources can use incremental state:

```python
# Pseudo-pattern from dlt documentation
@dlt.resource
def incremental_resource(
    cursor: dlt.sources.incremental[str] = 
        dlt.sources.incremental("updated_at", initial_value="2024-01-01")
):
    """Resource with incremental loading."""
    # dlt automatically tracks the cursor
    # and restarts from last value on subsequent runs
    yield data
```

**File Location in Examples**: `/home/user/hackathon/data/examples/dlt/notebooks/dlt_sentry.py:70`

---

## 4. Pipeline Patterns

### 4.1 Orchestration Pipeline Pattern

Pipelines combine sources with destinations and configuration:

**Pattern**:
```python
pipeline = dlt.pipeline(
    pipeline_name="my_pipeline",
    destination="duckdb",  # or "postgres", etc.
    dataset_name="my_dataset",
)

load_info = pipeline.run(my_source())
```

**Real Examples** (`/home/user/hackathon/data-unified/pipelines/github_to_r2/dagster_assets.py`):

```python
# GitHub Pipeline
pipeline = dlt.pipeline(
    pipeline_name="github_repos",
    destination=create_duckdb_destination(),
    dataset_name="github_metadata",
)
load_info = pipeline.run(github_repos_source(github_token=get_env("GITHUB_API_KEY")))

# R2 Upload Pipeline
pipeline = dlt.pipeline(
    pipeline_name="r2_uploads",
    destination=create_duckdb_destination(),
    dataset_name="r2_uploads",
)
load_info = pipeline.run(r2_upload_source(cloned_repos=github_repos))

# Documentation Pipeline
pipeline = dlt.pipeline(
    pipeline_name="docs_scraper",
    destination=create_duckdb_destination(),
    dataset_name="docs_metadata",
)
load_info = pipeline.run(docs_scraper_source(max_urls_per_source=50))
```

### 4.2 Dagster Asset Composition Pattern

Dagster assets compose pipelines into DAGs with dependencies:

**Pattern**:
```python
@asset(
    description="...",
    group_name="pipeline_group",
    deps=[upstream_asset],
)
def downstream_asset(context: AssetExecutionContext, upstream_asset: List[Dict]):
    """Asset that depends on upstream."""
    context.log.info(f"Processing {len(upstream_asset)} items")
    
    # Process upstream data
    result = process(upstream_asset)
    
    # Optional: Emit events or metadata
    context.log.info(f"Produced result: {result}")
    
    return result
```

**Real Example - GitHub Pipeline** (`/home/user/hackathon/data-unified/pipelines/github_to_r2/dagster_assets.py:16-44`):

```python
@asset(
    description="Fetch GitHub repository metadata and clone repositories",
    group_name="github_pipeline",
)
def github_repos(context: AssetExecutionContext) -> List[Dict[str, Any]]:
    """Clone configured GitHub repositories."""
    context.log.info("Cloning GitHub repositories...")
    
    cloned_repos = clone_all_repos(github_token=get_env("GITHUB_API_KEY"))
    
    pipeline = dlt.pipeline(
        pipeline_name="github_repos",
        destination=create_duckdb_destination(),
        dataset_name="github_metadata",
    )
    load_info = pipeline.run(github_repos_source(github_token=get_env("GITHUB_API_KEY")))
    
    context.log.info(f"Cloned {len(cloned_repos)} repositories")
    return cloned_repos
```

**Real Example - R2 Upload** (`/home/user/hackathon/data-unified/pipelines/github_to_r2/dagster_assets.py:47-89`):

```python
@asset(
    description="Upload cloned repositories to Cloudflare R2",
    group_name="github_pipeline",
    deps=[github_repos],
)
def r2_uploaded_repos(
    context: AssetExecutionContext,
    github_repos: List[Dict[str, Any]],
) -> List[Dict[str, Any]]:
    """Upload cloned repositories to R2."""
    context.log.info(f"Uploading {len(github_repos)} repositories to R2...")
    
    pipeline = dlt.pipeline(
        pipeline_name="r2_uploads",
        destination=create_duckdb_destination(),
        dataset_name="r2_uploads",
    )
    load_info = pipeline.run(r2_upload_source(cloned_repos=github_repos))
    context.log.info(f"Upload complete: {load_info}")
    
    upload_results = []
    for repo in github_repos:
        if repo.get("success"):
            upload_results.append({
                "full_name": repo["full_name"],
                "owner": repo["owner"],
                "repo": repo["repo"],
                "branch": repo["branch"],
                "success": True,
            })
    
    return upload_results
```

### 4.3 Job and Schedule Pattern

Dagster jobs group related assets:

```python
github_pipeline_job = define_asset_job(
    name="github_pipeline",
    selection=["github_repos", "r2_uploaded_repos", "indexed_code"],
    description="Complete GitHub → R2 → CocoIndex pipeline",
)

docs_pipeline_job = define_asset_job(
    name="docs_pipeline",
    selection=["scraped_docs", "r2_uploaded_docs", "knowledge_graphs"],
    description="Complete Docs → R2 → Cognee pipeline",
)

full_pipeline_job = define_asset_job(
    name="full_pipeline",
    selection="*",
    description="Run both GitHub and Docs pipelines",
)

# Scheduling
github_daily_schedule = ScheduleDefinition(
    job=github_pipeline_job,
    cron_schedule="0 2 * * *",  # 2 AM daily
    name="github_daily",
)

docs_weekly_schedule = ScheduleDefinition(
    job=docs_pipeline_job,
    cron_schedule="0 3 * * 0",  # Sundays at 3 AM
    name="docs_weekly",
)
```

**File Location**: `/home/user/hackathon/data-unified/dagster_project/definitions.py`

### 4.4 Multi-Stage Pipeline Pattern

Pipelines typically follow: Extract → Transform → Load (ETL)

**Example - GitHub Code Indexing**:
1. **Extract**: `github_repos` asset - fetches metadata from GitHub API
2. **Transform**: Upload to R2, transform to storage format
3. **Load**: Index with CocoIndex, store vectors in LanceDB

**Example - Documentation Pipeline**:
1. **Extract**: `scraped_docs` - Firecrawl scrapes documentation
2. **Transform**: Generate llms.txt and llms-full.txt files
3. **Load**: Upload to R2, process with Cognee, store in Memgraph

---

## 5. Error Handling

### 5.1 Exception Handling Pattern

The codebase uses consistent exception handling:

**Pattern**:
```python
try:
    # Attempt operation
    result = operation()
    return {"success": True, "result": result}
except SpecificException as e:
    # Log error
    logger.error(f"Specific error: {e}")
    # Return error result
    return {"success": False, "error": str(e)}
except Exception as e:
    # Catch-all for unexpected errors
    logger.error(f"Unexpected error: {e}")
    return {"success": False, "error": str(e)}
```

**Real Examples**:

**GitHub Clone Error Handling** (`/home/user/hackathon/data-unified/pipelines/github_to_r2/clone_repo.py:174-198`):
```python
def clone_all_repos(target_base_dir=None, github_token=None):
    """Clone all configured GitHub repositories."""
    results = []
    
    for repo_config in repos:
        try:
            repo_dir = target_base_dir / f"{owner}_{repo}"
            clone_path = clone_github_repo(owner, repo, branch, repo_dir, github_token)
            
            results.append({
                "full_name": f"{owner}/{repo}",
                "success": True,
                "clone_path": str(clone_path),
            })
        except Exception as e:
            print(f"Error cloning {owner}/{repo}: {e}")
            results.append({
                "full_name": f"{owner}/{repo}",
                "success": False,
                "error": str(e),
            })
    
    return results
```

**R2 Upload Error Handling** (`/home/user/hackathon/data-unified/pipelines/github_to_r2/upload_r2.py:35-108`):
```python
def upload_repo_to_r2(repo_path, owner, repo, branch="main", r2_client=None):
    """Upload a repository to Cloudflare R2."""
    try:
        archive_path = create_repo_archive(repo_path)
        
        object_name = f"repos/{owner}/{repo}/{branch}/archive.tar.gz"
        r2_client.upload_file(file_path=archive_path, object_name=object_name)
        
        return {
            "full_name": f"{owner}/{repo}",
            "success": True,
            "archive_object": object_name,
        }
    except Exception as e:
        return {
            "full_name": f"{owner}/{repo}",
            "error": str(e),
            "success": False,
        }
    finally:
        if archive_path.exists():
            archive_path.unlink()
```

### 5.2 AWS/R2 Exception Handling

```python
class R2Client:
    def upload_file(self, file_path, object_name=None, ...):
        """Upload a file to R2."""
        try:
            self.s3_client.upload_file(str(file_path), bucket, object_name)
            return object_name
        except ClientError as e:
            raise Exception(f"Failed to upload file to R2: {e}")
```

**File Location**: `/home/user/hackathon/data-unified/pipelines/shared/r2_client.py:74-78`

### 5.3 Configuration Error Handling

```python
def get_env(key: str, default: Optional[str] = None, required: bool = False) -> str:
    """Get environment variable with optional default and required check."""
    value = os.getenv(key, default)
    if required and value is None:
        raise ValueError(f"Required environment variable {key} not set")
    return value or ""
```

**File Location**: `/home/user/hackathon/data-unified/pipelines/shared/config.py:13-30`

### 5.4 Firecrawl/OpenAI Error Handling

**Example** (`/home/user/hackathon/data-unified/pipelines/docs_to_knowledge/generate_llmstxt.py:49-77`):

```python
def map_website(self, url: str, limit: int = 100) -> List[str]:
    """Map a website to get all URLs."""
    try:
        response = requests.post(
            f"{self.firecrawl_base_url}/map",
            headers=self.headers,
            json={"url": url, "limit": limit}
        )
        response.raise_for_status()
        
        data = response.json()
        if data.get("success") and data.get("links"):
            return data["links"]
        else:
            logger.error(f"Failed to map website: {data}")
            return []
    except Exception as e:
        logger.error(f"Error mapping website: {e}")
        return []
```

### 5.5 Logging Pattern

The codebase uses Python's standard logging module:

```python
import logging

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Usage
logger.info("Starting process...")
logger.error("Error occurred: {e}")
logger.debug("Debug information")
```

**File Location**: `/home/user/hackathon/data-unified/pipelines/docs_to_knowledge/generate_llmstxt.py:29-33`

---

## 6. Testing Patterns

### 6.1 Test Structure

Testing examples found in the codebase use pytest:

```python
# From /home/user/hackathon/infrastructure/compose/agno/tests/
import pytest
from conftest import setup_fixtures

@pytest.fixture
def client():
    """Fixture for API client."""
    return create_test_client()

def test_health_check(client):
    """Test health endpoint."""
    response = client.get("/health")
    assert response.status_code == 200
```

### 6.2 Conftest Pattern

Pytest conftest.py for shared fixtures:

```python
# conftest.py
import pytest

@pytest.fixture(scope="session")
def setup_database():
    """Setup database for tests."""
    db = create_test_db()
    yield db
    db.cleanup()

@pytest.fixture
def client(setup_database):
    """Create test client."""
    return create_client(setup_database)
```

**File Location**: `/home/user/hackathon/infrastructure/compose/agno/tests/conftest.py`

### 6.3 Integration Testing Pattern

The codebase suggests integration test patterns for pipelines:

```python
def test_github_pipeline():
    """Test complete GitHub pipeline."""
    # Setup
    repos = [{"owner": "test", "repo": "repo"}]
    
    # Execute
    result = clone_all_repos()
    
    # Assert
    assert result[0]["success"] == True
    assert len(result) > 0

def test_dlt_pipeline():
    """Test DLT pipeline integration."""
    pipeline = dlt.pipeline(
        pipeline_name="test_pipeline",
        destination="duckdb",
    )
    
    load_info = pipeline.run(test_source())
    
    assert load_info.has_successfully_loaded == True
```

---

## 7. Extension Points

### 7.1 Configuration-Driven Extensions

**Pattern**: Load pipelines from configuration files

```yaml
# config/sources.yaml
sources:
  dagster:
    github_url: https://github.com/dagster-io/dagster
    docs_url: https://docs.dagster.io/
    enabled: true
    priority: high
  
  dbt:
    github_url: https://github.com/dbt-labs/dbt-core
    docs_url: https://docs.getdbt.com/
    enabled: true
    priority: high
```

**File Location**: `/home/user/hackathon/data-unified/config/`

### 7.2 Custom Resource Extension

**Pattern**: Create new DLT resources

```python
# In pipelines/custom/my_source.py
@dlt.resource(write_disposition="merge", primary_key="id")
def my_custom_resource(config=None):
    """Custom data source."""
    if config is None:
        config = get_config("custom/my_source")
    
    for item in extract_data(config):
        yield transform(item)

@dlt.source
def my_custom_source(config=None):
    return my_custom_resource(config)
```

### 7.3 Custom Destination Extension

**Pattern**: Create new DLT destination

```python
def create_custom_destination(connection_string):
    """Create custom destination."""
    return dlt.destinations.custom_sql(
        connection_string=connection_string,
        schema_inference=True
    )
```

### 7.4 Custom Dagster Asset Extension

**Pattern**: Add new assets to the pipeline

```python
# In pipelines/custom/dagster_assets.py
from dagster import asset

@asset(
    group_name="custom_pipeline",
)
def my_custom_asset(context: AssetExecutionContext):
    """Custom processing asset."""
    context.log.info("Processing custom data...")
    return result
```

### 7.5 Custom Client Pattern

**Pattern**: Extend R2Client for new storage

```python
class CustomStorageClient:
    """Client for custom storage backend."""
    
    def __init__(self, config):
        self.config = config
        self.client = initialize_client(config)
    
    def upload_file(self, file_path, object_name):
        """Upload file implementation."""
        pass
    
    def download_file(self, object_name, file_path):
        """Download file implementation."""
        pass
```

### 7.6 Data Model Extension

**Pattern**: Extend Pydantic models

```python
# Extend existing models
class CustomDataItem(DataItem):
    """Extended data item with custom fields."""
    custom_field: str
    custom_metadata: Dict[str, Any]

class CustomSourceInfo(SourceInfo):
    """Extended source info."""
    custom_source_type: str
```

### 7.7 Hook/Middleware Pattern

**Pattern**: DLT event hooks (pseudo-pattern)

```python
# Example pattern from dlt capabilities
def on_pipeline_run(context):
    """Hook called before/after pipeline run."""
    logger.info(f"Pipeline running: {context.pipeline_name}")

pipeline.on_before_load(on_pipeline_run)
pipeline.on_load_complete(on_pipeline_run)
```

### 7.8 CLI Command Extension

**Pattern**: Add new CLI commands

```python
# In cli/custom_commands.py
import typer

custom_app = typer.Typer()

@custom_app.command()
def custom_operation(
    param1: str = typer.Argument(..., help="Parameter 1"),
    param2: str = typer.Option("default", help="Parameter 2")
):
    """Custom operation command."""
    console.print(f"Running custom operation: {param1}, {param2}")
    result = execute_operation(param1, param2)
    console.print(f"[green]Success![/green]")

# In cli/main.py
app.add_typer(custom_app, name="custom", help="Custom commands")
```

**File Location**: `/home/user/hackathon/data-unified/cli/main.py:18-21`

### 7.9 Notebook Extension

**Pattern**: Add interactive notebooks

```python
# In notebooks/custom_analysis.py
import marimo

app = marimo.App()

@app.cell
def setup():
    import pandas as pd
    return pd,

@app.cell
def analyze_data(pd):
    """Custom analysis."""
    data = load_data()
    return pd.DataFrame(data)

if __name__ == "__main__":
    app.run()
```

---

## 8. Key Architectural Components

### 8.1 Shared Utilities Layer

**Location**: `/home/user/hackathon/data-unified/pipelines/shared/`

- **config.py**: Environment variable and YAML configuration management
- **dlt_sources.py**: Factory functions for DLT destinations
- **r2_client.py**: Cloudflare R2 S3-compatible client wrapper

### 8.2 Models Layer

**Location**: `/home/user/hackathon/data-unified/models/`

- **classes.py**: Data type definitions (SourceInfo, DataItem, etc.)
- **schemas.py**: Pipeline configuration models (GitHubRepoConfig, etc.)

### 8.3 Pipeline Layers

**Location**: `/home/user/hackathon/data-unified/pipelines/`

```
pipelines/
├── github_to_r2/          # Code indexing pipeline
│   ├── clone_repo.py      # GitHub API & cloning
│   ├── upload_r2.py       # R2 upload logic
│   ├── index_cocoindex.py # CocoIndex integration
│   └── dagster_assets.py  # Orchestration
│
├── docs_to_knowledge/     # Documentation pipeline
│   ├── scrape_docs.py     # Firecrawl scraping
│   ├── generate_llmstxt.py # llms.txt generation
│   ├── upload_r2.py       # R2 upload
│   ├── cognify.py         # Cognee integration
│   └── dagster_assets.py  # Orchestration
│
└── shared/                # Reusable components
    ├── config.py
    ├── dlt_sources.py
    └── r2_client.py
```

### 8.4 Interfaces Layer

**Location**: `/home/user/hackathon/data-unified/`

```
├── cli/                   # Typer CLI interface
│   ├── main.py
│   ├── github_commands.py
│   ├── docs_commands.py
│   └── query_commands.py
│
├── notebooks/            # Marimo interactive notebooks
│   ├── github_pipeline.py
│   ├── docs_pipeline.py
│   └── unified_dashboard.py
│
└── dagster_project/      # Dagster orchestration
    └── definitions.py
```

---

## 9. Summary Table: Design Patterns Used

| Pattern | Purpose | Location | Key Files |
|---------|---------|----------|-----------|
| **Decorator** | Declare resources, sources, assets | DLT & Dagster | clone_repo.py, upload_r2.py, dagster_assets.py |
| **Builder** | Configure complex flows | CodeIndexConfig | index_cocoindex.py |
| **Factory** | Create destinations & clients | Destinations | dlt_sources.py |
| **Repository** | Encapsulate external services | R2Client | r2_client.py |
| **Strategy** | Different processing approaches | Pipelines | github_to_r2/, docs_to_knowledge/ |
| **Iterator** | Yield data items from resources | DLT Resources | github_repos_resource, r2_upload_resource |
| **Composition** | Combine assets into DAGs | Dagster | dagster_assets.py, definitions.py |
| **Configuration** | YAML-driven extensibility | Config management | config.py, sources.yaml |
| **Exception Handler** | Graceful error handling | Error handling | Throughout codebase |

---

## 10. Data Flow Architecture

```
GitHub API ──→ Clone Local ──→ Archive ──→ R2 ──→ CocoIndex ──→ LanceDB
                                              ├──→ PostgreSQL
                                              └──→ DuckDB (metadata)

Documentation URL ──→ Firecrawl ──→ llms.txt ──→ R2 ──→ Cognee ──→ Memgraph
                                      │
                                      └──→ OpenAI (LLM processing)
```

---

## 11. Configuration-Driven Design

The platform is highly configurable through:

1. **Environment Variables** (.env)
2. **YAML Configuration** (config/sources.yaml)
3. **Pydantic Models** (type validation & defaults)
4. **Runtime Parameters** (function arguments)

This allows users to:
- Add new sources by editing YAML
- Change processing parameters via env vars
- Extend models without code changes
- Create new pipelines via composition

---

## 12. Extensibility Summary

### How to Extend

1. **Add New Source**: Create new resource + source in `pipelines/*/`
2. **Add New Destination**: Use `create_*_destination()` factory
3. **Add New Asset**: Define `@asset` in `dagster_assets.py`
4. **Add New CLI Command**: Create command in `cli/` and register in `main.py`
5. **Add New Data Model**: Extend Pydantic models in `models/`
6. **Add New Configuration**: Update YAML in `config/`
7. **Add New Interface**: Create notebook or Typer command

### Key Extension Points

- **DLT Resources**: Reusable data extraction units
- **Dagster Assets**: Composable pipeline units
- **Shared Utilities**: R2Client, Config, DLT factories
- **Data Models**: Type-safe schema definitions
- **CLI Commands**: User-facing operations
- **Notebooks**: Interactive exploration

