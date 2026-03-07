A Unified Architecture for AI-Driven Data
Acquisition and Analysis

Architectural Blueprint: A Unified Stack for AI-Driven
Data Acquisition

Executive Summary: The Modern Data Stack, Democratized

This report presents a comprehensive architectural guide for constructing a sophisticated,
end-to-end data acquisition and analysis platform. It leverages a curated selection of modern,
cost-effective, and open-source-centric tools designed to empower individual developers,
researchers, and small teams. The architecture provides capabilities that have traditionally
required large engineering organizations and significant capital expenditure, effectively
democratizing access to a state-of-the-art data stack.
The core philosophy of this architecture is rooted in the strategic use of serverless paradigms,
open standards, and AI-native tooling. The data workflow begins with advanced, AI-aware web
scraping using CRAWL4AI to extract clean, structured information from complex web sources.
This data is then ingested into a robust pipeline orchestrated by dlt (data load tool), a
declarative Python library that automates schema inference and data loading.
A key innovation of this stack is the decoupling of storage and compute. Data is persisted in an
open format to Cloudflare R2, an S3-compatible object storage service notable for its low cost
and absence of egress fees. This data lake is then queried in situ by a powerful analytics layer
composed of DuckDB for local operations and MotherDuck for serverless cloud-based
analysis.
This entire system is designed to run on highly cost-effective infrastructure, utilizing either a
Hetzner cloud VPS or the Oracle Cloud Free Tier. Security and access are managed through
Pangolin (Fossorial), a self-hosted, identity-aware reverse proxy that provides secure
tunneling without exposing server ports. The entire development and operational lifecycle is
supercharged by an agentic AI environment within Visual Studio Code, integrating Gemini 2.5
Pro and GitHub Copilot. This is further extended through custom, containerized Model
Context Protocol (MCP) servers, which grant AI agents programmatic control over the data
stack, transforming the development process into a collaborative human-AI partnership.

System Architecture Diagram

The proposed architecture is a modular system where each component serves a distinct
purpose, interconnected through well-defined interfaces and open standards. The flow of data
and control is designed for efficiency, scalability, and security.

●

Ingestion Layer: At the forefront is the CRAWL4AI service, running within a Docker
container on the chosen VPS. It is responsible for all web scraping activities, targeting
educational resources like examinations.ie and the BBC. It interacts with the public
internet, handling dynamic content, logins, and deep crawling, producing LLM-optimized
Markdown or structured JSON data.

●  Pipelining & Orchestration Layer: The dlt script, also containerized on the VPS, serves

as the central orchestrator. It acts as a client to the CRAWL4AI service (or directly
embeds its logic), taking the raw scraped data as input. Using its declarative resource and
source decorators, dlt manages data normalization, schema evolution, and loading.
●  Storage Layer: The single source of truth for raw and processed data is Cloudflare R2.
dlt writes data, typically in the efficient Parquet format, directly to a designated R2 bucket.
R2's S3-compatible API and zero-egress-fee policy make it the economic cornerstone of
the data backbone.

●  Analytics Layer: This layer is bifurcated. For local development, debugging, and

●

smaller-scale analysis, DuckDB can be used directly within a container or on a local
machine to query the Parquet files in R2. The primary analytics engine, however, is
MotherDuck. It connects to the R2 bucket via its DuckLake feature, providing a
serverless, scalable SQL query interface over the data without requiring a separate
ingestion step.
Infrastructure & Security Layer: The entire stack of containerized services is hosted on
a Hetzner or Oracle Cloud Free Tier VPS. Network traffic and domain management are
handled by Cloudflare DNS. Crucially, Pangolin acts as a secure gateway. Instead of
opening ports on the VPS firewall for services like a potential API endpoint, Pangolin
establishes an outbound WireGuard tunnel, exposing services securely through its own
identity and access management layer.

●  Development & Agentic Layer: The developer interacts with the system from a local

Visual Studio Code environment. This IDE is augmented with the Gemini 2.5 Pro and
GitHub Copilot extensions for AI-assisted coding. The developer connects to the VPS
via SSH to manage the Docker containers. The most advanced interaction occurs through
a custom, containerized MCP Server running on the VPS. The Gemini agent in VS Code
can invoke tools exposed by this server—for instance, a tool to directly query the
MotherDuck database—creating a powerful, automated feedback loop for data analysis
and system management.

Strategic Rationale and Core Synergies

The power of this architecture lies not just in the individual tools but in their synergistic
integration, which addresses common pain points in modern data engineering related to cost,
complexity, and flexibility.

●  CRAWL4AI + dlt: From Raw Web to Structured Pipeline: The integration between

CRAWL4AI and dlt is exceptionally seamless. CRAWL4AI is designed to output clean,
structured data, either as LLM-ready Markdown or, more applicably here, as structured
JSON via its extraction strategies. This output can be yielded directly from a Python
function, which can then be decorated as a @dlt.resource. This creates a robust,
schema-aware ingestion pipeline with minimal boilerplate code, as dlt automatically
handles schema inference, data typing, and normalization. This tight, in-process coupling
eliminates the need for intermediate storage or complex data handoffs between the
scraping and pipelining stages.

●  dlt + R2 + MotherDuck: The DuckLake Architecture: This combination represents a

strategic decoupling of storage and compute, forming a modern, cost-effective alternative
to traditional data warehouses. dlt writes data in an open, columnar format (Parquet) to
Cloudflare R2. R2 provides extremely inexpensive, durable storage with the critical
advantage of zero egress fees, eliminating surprise costs associated with data movement.

MotherDuck's DuckLake feature then allows it to act as a serverless query engine directly
on top of the data in R2. Data does not need to be ingested or duplicated into
MotherDuck's own storage. This "disaggregated" model provides immense flexibility and
cost control, allowing storage and compute to scale independently.

●  Hetzner/Oracle + Pangolin + Docker: Sovereign and Secure Infrastructure: The use
of commodity VPS providers like Hetzner or the Oracle Free Tier offers powerful compute
resources at a fraction of the cost of larger hyperscalers. Deploying all services as Docker
containers ensures portability and reproducible environments. Pangolin introduces a
critical security posture that aligns with modern "zero trust" principles. By tunneling
services through an outbound WireGuard connection, it obviates the need to open
inbound ports on the server's firewall, drastically reducing the attack surface. Pangolin
then layers its own robust identity and access management on top, providing a
self-hosted, fully controlled secure gateway to any internal services.

●  VS Code + Gemini + MCP: AI as a Collaborative Platform: This integration elevates

the role of AI in the development process. While Gemini and Copilot excel at generating
code for the scraper, the dlt pipeline, or Dockerfiles, the true paradigm shift comes from
the Model Context Protocol (MCP). By building a custom MCP server, the developer can
create tools that give the AI agent direct, programmatic access to the live system. For
example, a tool could allow Gemini to query the MotherDuck database, inspect pipeline
logs, or even trigger a new CRAWL4AI run. This transforms the AI from a simple code
completion engine into an active, agentic partner capable of performing complex,
multi-step tasks across the entire stack based on natural language commands.

The selection of these tools reflects a broader industry movement toward a more unbundled and
rebundled data stack. Traditional, monolithic cloud services, while convenient, often come with
high costs and vendor lock-in, particularly through mechanisms like data egress fees. The
architecture detailed here deliberately unbundles these functions, selecting best-of-breed,
cost-effective components for each task: R2 for storage, MotherDuck for compute, and Hetzner
for hosting. These components are then "rebundled" not by a single vendor, but by open
standards (S3 API, Parquet, SQL) and flexible, interoperable software (dlt, DuckDB). This
approach provides greater control, transparency, and economic efficiency.
Furthermore, the entire stack is designed around the principle of "Infrastructure as Code,
Managed by AI." Every component is defined and configured through code—Python scripts for
CRAWL4AI and dlt, and YAML files for Docker Compose and Pangolin. The integration of MCP
servers completes this vision by creating a programmatic interface through which an AI agent
can interact with, query, and ultimately help manage this code-defined infrastructure. This
represents a forward-looking approach, moving beyond AI-assisted development to AI-driven
operations.

The Ingestion Engine: Mastering Advanced Web
Scraping with CRAWL4AI

Deep Dive into CRAWL4AI's Capabilities

CRAWL4AI distinguishes itself as a modern web scraping framework by being purpose-built for
AI and agentic workflows. It moves beyond traditional HTML parsing to provide a
comprehensive suite of tools for interacting with and extracting meaningful data from the

contemporary, dynamic web.

●  LLM-Native Design: The core design philosophy of CRAWL4AI is to produce output that
is immediately consumable by Large Language Models. Instead of raw, noisy HTML, it
can intelligently convert web content into clean, structured Markdown. This process
involves removing superfluous tags like <script> and <style>, structuring content with
proper headings and tables, and converting hyperlinks into citation-style references,
which is ideal for Retrieval-Augmented Generation (RAG) and model fine-tuning
applications.

●  Performance and Control: CRAWL4AI is engineered for speed and efficiency. It utilizes
an asynchronous browser pool and caching mechanisms to minimize network hops and
redundant operations, claiming performance up to six times faster than conventional
methods. Developers retain granular control over the browsing context through the
BrowserConfig class, allowing for precise configuration of headless mode, proxies, user
agents, and persistent browser profiles for session management.

●  Dynamic Content and Interaction: The modern web is overwhelmingly dynamic, and

CRAWL4AI is equipped to handle it. It provides a powerful mechanism for interacting with
pages that rely heavily on JavaScript. Through the CrawlerRunConfig, developers can
execute custom JavaScript code (js_code), instruct the crawler to wait for specific
elements or conditions (wait_for), and reuse sessions across multiple steps. This enables
complex workflows such as repeatedly clicking "Load More" buttons, filling out and
submitting forms, or navigating multi-page application flows.

●  Deep Crawling Strategies: For comprehensive data extraction from entire websites,

CRAWL4AI offers several deep crawling strategies. These include Breadth-First Search
(BFSDeepCrawlStrategy) and Depth-First Search (DFSDeepCrawlStrategy), allowing the
crawler to recursively discover and process linked pages. These strategies are highly
configurable, with parameters to control crawl depth, enforce domain limits, and apply
filters to include or exclude URLs based on specific patterns, ensuring the crawler
retrieves only relevant content.

●  Versatile Extraction Strategies: CRAWL4AI provides a dual approach to data extraction,

catering to different needs for speed, cost, and accuracy:

○  Structured Extraction (LLM-Free): For performance-critical tasks where the data
structure is well-defined, the framework supports extraction using standard CSS or
XPath selectors. This method is significantly faster and cheaper as it does not
require an LLM call, making it ideal for scalable, precise data retrieval.
○  LLM-Powered Extraction: When data is unstructured or requires semantic

understanding, CRAWL4AI can leverage an LLM. This enables sophisticated tasks
like summarization, classification, or extracting information based on natural
language instructions (e.g., "Extract all product prices"). It supports defining the
desired output structure using Pydantic schemas, ensuring the LLM returns clean,
validated JSON. The framework also manages content chunking to respect model
token limits while preserving context.

Tutorial: Scraping Login-Protected Educational Resources

This tutorial demonstrates how to use CRAWL4AI to scrape data from a website that requires
user authentication, a common scenario for accessing personalized educational resources like
those on a student portal modeled after examinations.ie.
The primary challenge is to manage the login state. CRAWL4AI offers two robust methods for

this: Identity-Based Crawling with Managed Browsers, which is the recommended approach for
its simplicity and reliability, and programmatic login using Session Management and Hooks for
fully automated workflows.

Step 1: Authentication with Identity-Based Crawling

This method involves creating a persistent browser profile where the login is performed once,
manually. CRAWL4AI then reuses this profile, along with its cookies and session storage, for all
subsequent automated crawls, making the scraper appear as the logged-in user.

1.  Create a Persistent Profile: CRAWL4AI's BrowserProfiler class automates this process.
The following script will launch a browser window. The user should navigate to the login
page, enter their credentials, and complete the login process. Once logged in, pressing 'q'
in the terminal will save the entire browser profile (cookies, local storage, etc.) to a
specified directory.
import asyncio
from crawl4ai import BrowserProfiler

async def create_login_profile():
    """
    Launches a browser to create a persistent profile with login
state.
    """
    profiler = BrowserProfiler()
    print("A browser window will now open.")
    print("Please log in to the educational portal.")
    print("Once you are logged in, press 'q' in this terminal to
save the profile and close the browser.")

    profile_path = await
profiler.create_profile(profile_name="edu_portal_profile")

    print(f"Profile created and saved at: {profile_path}")
    return profile_path

if __name__ == "__main__":
    asyncio.run(create_login_profile())
This script saves the profile to a default location managed by CRAWL4AI and prints the
path, which is needed for the next step.

2.  Use the Profile for Scraping: Now, configure CRAWL4AI to use this saved profile. The
BrowserConfig is set up to use a "managed browser" and points to the user_data_dir
where the profile was saved.
import asyncio
from crawl4ai import AsyncWebCrawler, BrowserConfig,
CrawlerRunConfig
from crawl4ai.extraction import JsonCssExtractionStrategy

# Path obtained from the previous script
PROFILE_PATH =

"/path/to/your/crawl4ai_profiles/edu_portal_profile"

async def scrape_protected_data():
    """
    Uses the saved browser profile to scrape data from a protected
page.
    """
    # Configure the crawler to use the persistent profile
    browser_config = BrowserConfig(
        headless=True,
        use_managed_browser=True, # Key setting for using
persistent identity
        user_data_dir=PROFILE_PATH,
        browser_type="chromium"
    )

    # Define what to extract from the page
    extraction_strategy = JsonCssExtractionStrategy(
        extractions=[
            {"name": "exam_subject", "css":
".exam-result-subject", "type": "text"},
            {"name": "exam_grade", "css": ".exam-result-grade",
"type": "text"},
            {"name": "exam_date", "css": ".exam-result-date",
"type": "text"}
        ]
    )

    run_config = CrawlerRunConfig(

url="https://www.examinations.ie/candidate-portal/results", #
Hypothetical URL
        extraction_strategy=extraction_strategy
    )

    async with AsyncWebCrawler(config=browser_config) as crawler:
        print("Starting crawl with authenticated profile...")
        result = await crawler.arun(config=run_config)

        if result.status.is_success():
            print("Successfully extracted data:")
            print(result.extracted_data)
        else:
            print(f"Crawl failed: {result.status.error_message}")

if __name__ == "__main__":
    asyncio.run(scrape_protected_data())
This workflow effectively bypasses the login process on each run by inheriting the

authenticated state from the managed profile, a technique that is both robust and less
likely to be detected as bot activity.

Alternative: Programmatic Login with Hooks

For scenarios requiring full automation without manual intervention, CRAWL4AI's hook system
can be used to perform the login steps programmatically. The on_page_context_created hook is
ideal for this, as it provides access to the page object before the crawler navigates to the target
URL.
import asyncio
from crawl4ai import AsyncWebCrawler, BrowserConfig
from playwright.async_api import Page, BrowserContext

async def perform_login(page: Page, context: BrowserContext,
**kwargs):
    """
    Hook function to programmatically log in before crawling.
    """
    print("[HOOK] Navigating to login page...")
    await
page.goto("https://www.examinations.ie/candidate-portal/login") #
Hypothetical

    # Fill in login credentials
    await page.fill('input[name="username"]', "YOUR_USERNAME")
    await page.fill('input[name="password"]', "YOUR_PASSWORD")

    print("[HOOK] Submitting login form...")
    await page.click('button[type="submit"]')

    # Wait for navigation to the dashboard to confirm login
    await page.wait_for_url("**/candidate-portal/dashboard")
    print("[HOOK] Login successful!")
    return page

async def main():
    browser_config = BrowserConfig(
        headless=False, # Often useful to run non-headless for
debugging logins
        hooks={
            "on_page_context_created": perform_login
        }
    )

    async with AsyncWebCrawler(config=browser_config) as crawler:
        result = await
crawler.arun(url="https://www.examinations.ie/candidate-portal/results
")

        print(result.markdown)

if __name__ == "__main__":
    asyncio.run(main())

This approach is more complex and may require adjustments if the site uses CAPTCHAs or
other advanced bot detection, but it offers a fully automated solution.

Analysis of Target Sites and Compliance

A responsible scraping strategy requires an understanding of the target websites' structure and
their stated policies regarding automated access.

●  examinations.ie: The structure of this site revolves around providing official information
and a secure portal for candidates. The public-facing sections contain documents (PDFs
of exam papers, circulars), which are straightforward to crawl. The key challenge is the
Candidate Self Service Portal, which requires a Personal Identification Number (often
derived from a PPSN) for access. This necessitates the authenticated crawling
techniques detailed above. The data within this portal is personal and sensitive, and any
scraping activity must be conducted with explicit user consent and for legitimate
purposes.

●  BBC Bitesize: This is a large, media-rich educational platform with a deeply nested

structure organized by curriculum, subject, and topic. The primary scraping challenge is
not authentication but navigation and content extraction. A deep crawling strategy
(BFSDeepCrawlStrategy) would be effective for discovering the vast number of pages.
The goal would be to extract the core educational text, code snippets, and diagrams while
filtering out navigation menus, related links, and other boilerplate content. CRAWL4AI's
ability to convert content to clean Markdown is particularly valuable here, as it helps
isolate the signal from the noise.

●  robots.txt Compliance: The Robots Exclusion Protocol, implemented via a robots.txt file
at the root of a domain, is a standard for communicating with web crawlers. It is crucial to
understand that this protocol is voluntary; it provides guidelines for "good" bots but is not a
security mechanism to prevent access. Malicious bots can, and often do, ignore it.
○  Analysis: A review of the BBC's robots.txt file, for example, shows specific

directives for different user agents, disallowing paths related to admin areas, search
functions, and user profiles. A compliant scraper should parse this file and respect
these Disallow rules. The file may also contain a Crawl-delay directive, which
specifies a minimum time to wait between requests to avoid overloading the server.
CRAWL4AI's delay parameter in the CrawlerRunConfig should be set to honor this
value.

○  Ethical Considerations: For login-protected sites like examinations.ie, the

robots.txt file is less relevant for the protected areas, as they are not intended for
public crawlers in the first place. The ethical and legal responsibility shifts from
respecting a public policy to handling personal data appropriately and with consent.
Any scraping of such areas should only be done on behalf of the user who has
provided the credentials.

The evolution of web scraping, as exemplified by CRAWL4AI, is a direct response to the
increasing complexity and personalization of websites. The process has shifted from simple,
anonymous HTTP requests for static HTML to sophisticated, identity-aware interactions with

dynamic, stateful applications. The emphasis on features like persistent browser profiles and
session management underscores that modern data acquisition often requires simulating a
genuine user's identity to access valuable, personalized content. This trend suggests that the
future of web data extraction will be less about broad, anonymous crawling and more about
authenticated, permissioned, and stateful automation.
Simultaneously, the rise of applications like Retrieval-Augmented Generation has created a
direct market need for a new form of data preprocessing at the point of ingestion. The value is
no longer just in the raw data itself, but in its semantic cleanliness and structural integrity for
downstream AI consumption. A scraper's function has expanded from merely "getting" the data
to "preparing" it for an LLM. CRAWL4AI's focus on producing "LLM-ready Markdown" is a
leading indicator of this shift, where the scraping tool itself becomes the first and one of the
most critical steps in the AI data preparation pipeline.

The Data Backbone: Building Resilient Pipelines with
dlt, DuckDB, and Cloudflare R2

Introduction to dlt (data load tool)

dlt (data load tool) is an open-source Python library engineered to streamline and automate the
often-tedious process of building data pipelines. It operates on a "load data anywhere"
philosophy, abstracting away the complexities of data ingestion so that developers can focus on
the core logic of their application rather than on boilerplate data engineering tasks.
At its core, dlt is designed to be declarative, user-friendly, and highly extensible. It leverages
simple Python decorators, primarily @dlt.source and @dlt.resource, to define data pipelines. A
developer can write a standard Python function that yields data (e.g., from an API call or a file),
and with a single decorator, dlt transforms it into a robust data resource.
Key features that make dlt a powerful choice for this architecture include:

●  Schema Inference and Evolution: dlt automatically inspects the data being processed

and infers a schema, including data types and table structures. Crucially, it also manages
schema evolution. If the source data changes over time (e.g., a new field is added), dlt
can automatically adapt the destination schema, preventing pipeline failures and
eliminating a significant maintenance burden.

●  Automated Normalization: The library handles the normalization of semi-structured
data, such as nested JSON from APIs, into a structured, relational format suitable for
analytical databases. It creates parent-child tables and adds lineage metadata
automatically.
Incremental Loading: dlt has built-in support for incremental loading, allowing pipelines
to efficiently process only new or updated data since the last run, which is essential for
handling large datasets.

●

●  "Run Anywhere" Philosophy: dlt is a pure Python library with no external dependencies
on backends, APIs, or containers. This means a dlt pipeline can run wherever Python can
run—on a local machine, a serverless function, an Airflow DAG, or, as in this architecture,
a simple VPS. This simplicity and portability make it an ideal fit for a cost-effective,
self-hosted solution.

Tutorial: A Complete Pipeline from Scraper to Cloud Storage

This tutorial demonstrates the creation of a complete, end-to-end data pipeline. It will take the
structured data extracted by the CRAWL4AI scraper and load it into Cloudflare R2 using dlt.

Step 1: Creating a Python Generator Source

The first step is to encapsulate the scraping logic within a dlt resource. A @dlt.resource is a
Python generator function that yields batches of data. This allows dlt to process data in a
memory-efficient, streaming fashion.
The following script integrates the CRAWL4AI scraping logic from the previous section into a
dlt-compatible resource.
import dlt
from typing import Iterable, Dict, Any
import asyncio
from crawl4ai import AsyncWebCrawler, BrowserConfig, CrawlerRunConfig
from crawl4ai.extraction import JsonCssExtractionStrategy

# Assume PROFILE_PATH is configured correctly from the CRAWL4AI
tutorial
PROFILE_PATH = "/path/to/your/crawl4ai_profiles/edu_portal_profile"

@dlt.resource(name="exam_results", write_disposition="replace")
async def get_exam_results_data() -> Iterable]:
    """
    A dlt resource that uses CRAWL4AI to scrape authenticated data
    and yields the results.
    """
    browser_config = BrowserConfig(
        headless=True,
        use_managed_browser=True,
        user_data_dir=PROFILE_PATH,
        browser_type="chromium"
    )

    extraction_strategy = JsonCssExtractionStrategy(
        extractions=[
            {"name": "exam_subject", "css": ".exam-result-subject",
"type": "text"},
            {"name": "exam_grade", "css": ".exam-result-grade",
"type": "text"},
            {"name": "exam_date", "css": ".exam-result-date", "type":
"text"}
        ]
    )

    run_config = CrawlerRunConfig(

        url="https://www.examinations.ie/candidate-portal/results", #
Hypothetical
        extraction_strategy=extraction_strategy
    )

    async with AsyncWebCrawler(config=browser_config) as crawler:
        print("CRAWL4AI: Starting crawl within dlt resource...")
        result = await crawler.arun(config=run_config)

        if result.status.is_success() and result.extracted_data:
            print(f"CRAWL4AI: Successfully extracted
{len(result.extracted_data)} records.")
            # dlt can process an iterator of items directly
            yield result.extracted_data
        else:
            print(f"CRAWL4AI: Crawl failed or returned no data. Error:
{result.status.error_message}")
            # Yield an empty list to signify no data for this run
            yield

This function, get_exam_results_data, is now a reusable component that dlt can use as a data
source. The write_disposition="replace" argument instructs dlt to replace the destination table
with new data on each run.

Step 2: Configuring the Filesystem Destination for Cloudflare R2

dlt's filesystem destination is designed to work with any fsspec-compatible backend, including
S3-compatible services like Cloudflare R2. Configuration is handled through TOML files in a .dlt
directory.

1.  Create .dlt/config.toml: This file contains non-sensitive configuration. Here, we specify

that the filesystem destination should be used.
#.dlt/config.toml
[destination.filesystem]
# This is a placeholder; the actual bucket URL with protocol is in
secrets.toml

2.  Create .dlt/secrets.toml: This file holds sensitive credentials. To connect to Cloudflare

R2, the bucket_url, credentials, and the crucial endpoint_url must be provided.
#.dlt/secrets.toml
[destination.filesystem]
bucket_url = "s3://your-r2-bucket-name" # e.g.,
s3://educational-data

[destination.filesystem.credentials]
aws_access_key_id = "YOUR_R2_ACCESS_KEY_ID"
aws_secret_access_key = "YOUR_R2_SECRET_ACCESS_KEY"
# This is the critical part for R2 compatibility

endpoint_url =
"https://<YOUR_ACCOUNT_ID>.r2.cloudflarestorage.com"

Step 3: Running the Pipeline

With the source and destination configured, the final step is to create a pipeline script that brings
them together. The script will instantiate a dlt.pipeline, specify the destination, and run the
resource function.
# main_pipeline.py

import dlt
# Import the resource function from the previous step
from scraper_source import get_exam_results_data

def run_pipeline():
    """
    Configures and runs the dlt pipeline to load data from the scraper
to R2.
    """
    # Configure the pipeline
    pipeline = dlt.pipeline(
        pipeline_name="educational_resources_pipeline",
        destination="filesystem", # This matches the section in
config.toml
        dataset_name="irish_exams"
    )

    # Run the pipeline, loading data from our async resource.
    # dlt handles running the async generator correctly.
    # We specify the loader file format as parquet for efficiency.
    load_info = pipeline.run(
        get_exam_results_data(),
        loader_file_format="parquet"
    )

    # Pretty-print the outcome
    print(load_info)

if __name__ == "__main__":
    run_pipeline()

When this script is executed, dlt will:

1.  Invoke the get_exam_results_data async generator.
2.  Receive the scraped data yielded by the function.
3.  Infer a schema from the data structure.
4.  Convert the data into Parquet format.
5.  Use the credentials in secrets.toml to connect to the Cloudflare R2 endpoint.

6.  Upload the Parquet files into the s3://your-r2-bucket-name/irish_exams/exam_results/

path.

This completes the data flow from a live, authenticated website into a structured, queryable
format in cloud object storage.

The Analytics Layer: Querying R2 with DuckDB and MotherDuck

Once the data resides in Cloudflare R2 as Parquet files, it is ready for analysis. This architecture
offers two complementary methods for querying this data.

Local Analysis with DuckDB

For development, testing, or ad-hoc analysis on a local machine, DuckDB can directly query
files in S3-compatible storage. This requires installing the httpfs extension and configuring
credentials.
import duckdb
import os

# Set environment variables for DuckDB's S3 extension to use
os.environ = 'YOUR_R2_ACCESS_KEY_ID'
os.environ = 'YOUR_R2_SECRET_ACCESS_KEY'
os.environ[span_61](start_span)[span_61](end_span) =
'<YOUR_ACCOUNT_ID>.r2.cloudflarestorage.com'

# Connect to an in-memory DuckDB database
con = duckdb.connect(config={'s3_region': 'auto'})

# Query the Parquet files directly from R2
# The path matches the one created by dlt
df = con.execute("""
    SELECT
        exam_subject,
        COUNT(*) as num_entries
    FROM 's3://your-r2-bucket-name/irish_exams/exam_results/*.parquet'
    GROUP BY exam_subject
    ORDER BY num_entries DESC;
""").fetch_df()

print(df)

This approach is incredibly powerful for local workflows, providing full SQL analytics on cloud
data without needing any server infrastructure.

Serverless Analytics with MotherDuck and DuckLake

This is the primary cloud architecture for scalable, shared analytics. MotherDuck provides a
serverless platform that extends DuckDB's capabilities to the cloud, and its DuckLake feature is
purpose-built for querying data in external object storage.

1.  Configure R2 Access in MotherDuck: First, securely provide MotherDuck with the

credentials to access the R2 bucket. This is done by creating a SECRET object within
MotherDuck's environment. This SQL command can be run in the MotherDuck web UI:
CREATE SECRET r2_credentials IN MOTHERDUCK (
    TYPE R2,
    KEY_ID 'YOUR_R2_ACCESS_KEY_ID',
    SECRET 'YOUR_R2_SECRET_ACCESS_KEY',
    ACCOUNT_ID '<YOUR_ACCOUNT_ID>'
);

2.  Create the DuckLake Database: Next, define a new database in MotherDuck that is

backed by the R2 bucket. This command tells MotherDuck to treat the specified R2 path
as a managed database, without moving the data.
CREATE DATABASE educational_data_lake (
    TYPE DUCKLAKE,
    DATA_PATH 's3://your-r2-bucket-name/'
);

3.  Reconfigure and Run dlt Pipeline for DuckLake: The dlt pipeline must now be

reconfigured to load data directly into this DuckLake-managed database. This involves
changing the destination to motherduck and updating the credentials.

○  Update .dlt/secrets.toml:

[destination.motherduck.credentials]
# The DuckLake database created in the previous step
database = "educational_data_lake"
# Your MotherDuck service token
password = "YOUR_MOTHERDUCK_TOKEN"

○  Update main_pipeline.py:

# In main_pipeline.py
pipeline = dlt.pipeline(
    pipeline_name="educational_resources_pipeline",
    destination="motherduck", # Change destination to
motherduck
    dataset_name="irish_exams"
)

When this updated pipeline is run, dlt connects to MotherDuck. MotherDuck, using its
stored R2 credentials, then manages the writing of the Parquet files into the correct
location within the R2 bucket under the DuckLake structure.

4.  Querying via MotherDuck: With the pipeline complete, any user or service can connect

to MotherDuck (via its web UI, a Python client, or any DuckDB-compatible tool) and run
standard SQL queries. The query execution is handled by MotherDuck's serverless
compute, while the data-at-rest remains securely and cost-effectively in Cloudflare R2.
import duckdb

# Connect to MotherDuck using its connection string
con =

duckdb.connect(database='md:educational_data_lake?motherduck_token
=YOUR_TOKEN')

# Run the same query, but now it's executed in the cloud by
MotherDuck
df = con.execute("""
    SELECT
        exam_subject,
        COUNT(*) as num_entries
    FROM irish_exams.exam_results
    GROUP BY exam_subject
    ORDER BY num_entries DESC;
""").fetch_df()

print(df)

This architecture represents a fundamental shift in data engineering. The combination of dlt with
the DuckDB ecosystem moves away from complex, multi-tool ETL/ELT frameworks. It
champions a Python-native approach where the entire pipeline, from data extraction to loading,
can be expressed in a single, familiar language. dlt's automation of schema management
removes one of the most significant historical pain points in data pipeline development. This
empowers Python-centric data scientists and developers to build and own their data
infrastructure end-to-end, without needing to become experts in separate SQL-based
transformation tools.
Furthermore, the DuckLake architecture is a powerful manifestation of the "disaggregated data
warehouse" trend, which is reshaping cloud data economics. Traditional cloud data warehouses
bundle storage and compute, creating potential for high costs and vendor lock-in. By
disaggregating these components, this stack allows for the use of the most economically
efficient solution for each job: Cloudflare R2 for storage (leveraging its zero egress fees) and
MotherDuck for pay-as-you-go, serverless compute. The DuckLake technology provides the
critical metadata and cataloging layer that makes this separation possible, giving users
unprecedented control over their costs and data sovereignty, and mitigating the risks of data
gravity.

Infrastructure Deployment: A Cost-Optimized and
Secure Hosting Strategy

VPS Selection: Hetzner vs. Oracle Cloud Free Tier

The choice of a Virtual Private Server (VPS) is a critical foundation for the architecture. The
primary goal is to find a provider that offers sufficient resources for running containerized
scraping and data pipeline workloads at the lowest possible cost. Two standout options are
Hetzner and the Oracle Cloud Infrastructure (OCI) Free Tier.

●  Hetzner: A German cloud provider renowned for its exceptional price-to-performance

ratio, particularly for its cloud server offerings in Europe and the US. Hetzner is an ideal
choice for predictable, low-cost, high-performance hosting. Its key advantages include

straightforward hourly billing with a monthly cap, and a very generous inclusive traffic
allowance (typically 20 TB per month for EU locations), which is more than sufficient for
this project's needs. Its simple web console and developer-friendly tools like an official CLI
and Terraform provider make it easy to manage.

●  Oracle Cloud (OCI) Free Tier: Oracle offers a uniquely compelling "Always Free" tier that
goes far beyond the typical 12-month trial periods of other major cloud providers. The
centerpiece of this offering for compute-intensive tasks is the Ampere A1 instance, which
is based on the ARM architecture. The free tier provides a generous pool of 3,000 OCPU
hours and 18,000 GB hours per month, which can be configured as a single powerful VM
with up to 4 OCPUs and 24 GB of RAM, or as multiple smaller VMs. The free tier also
includes 200 GB of block storage and 10 TB of outbound data transfer per month, making
it a genuinely free option for hosting the entire stack. The main consideration is the ARM
architecture, which requires the use of ARM-compatible Docker images.

The following table provides a direct comparison of a typical low-cost Hetzner instance against
the Oracle Always Free offering.
Feature

Hetzner (CPX21 - AMD)

CPU Architecture
vCPUs
RAM
NVMe SSD Storage
Included Traffic
Price (Monthly)
Key Pros

Key Cons

x86-64
3
4 GB
80 GB
20 TB (EU)
~$7.55 / €7.05
Predictable cost, x86
compatibility, excellent
performance, simple interface.
Not free.

Oracle Always Free (Ampere
A1 Flex)
AArch64 (ARM)
Up to 4
Up to 24 GB
200 GB (total pool)
10 TB
$0 (within free tier limits)
Potentially zero cost, very
generous CPU/RAM allocation,
large storage pool.
ARM architecture requires
compatible software, resource
availability can be limited.

This comparison allows for an informed decision based on project priorities. Hetzner offers a
reliable, powerful, and still very inexpensive option with the broad compatibility of the x86
architecture. Oracle provides a pathway to a completely free, and potentially more powerful,
hosting solution, with the primary trade-off being the need to ensure all software components
are ARM-compatible. Given that many modern tools, including CRAWL4AI, provide multi-arch
Docker images, the Oracle option is highly viable.

Tutorial: Provisioning an Oracle Cloud "Always Free" Ampere VM

This guide provides a step-by-step process for setting up an "Always Free" Ampere A1 virtual
machine on Oracle Cloud Infrastructure, from account creation to establishing a secure SSH
connection.

1.  Account Signup: Navigate to the Oracle Cloud Free Tier signup page. The process

requires providing an email address, creating a password, and selecting a home region. It
is critical to select a home region that supports Always Free Ampere A1 instances. A valid
credit or debit card is required for identity verification; prepaid cards are not accepted. A
small verification charge may be applied and then refunded.

2.  Instance Creation: Once logged into the OCI Console, navigate to the "Compute" section

and select "Instances." Click the "Create instance" button to begin the provisioning
process.

3.  Name and Compartment: Assign a descriptive name to the instance (e.g.,

data-ingestion-vps). Ensure it is being created in the root compartment of your tenancy.

4.  Image and Shape Configuration: This is the most critical step.
In the "Image and shape" section, click "Edit."

○
○  Under "Image," click "Change image" and select a suitable operating system.

Ubuntu is a common and well-supported choice.

○  Under "Shape," click "Change shape." Select the "Ampere" option for the processor
architecture. Check the box for the VM.Standard.A1.Flex shape, which is marked
as "Always Free-eligible".

○  Adjust the sliders to allocate the desired number of OCPUs and amount of memory.
To create a single, powerful instance, slide both to the maximum (4 OCPUs, 24 GB
of RAM). Click "Select shape."

5.  Networking: For most use cases, the default networking settings are sufficient. Ensure

"Create new virtual cloud network" is selected and that the "Assign a public IPv4 address"
option is enabled. This will create the necessary network infrastructure and make the VM
accessible from the internet.

6.  SSH Key Configuration: Secure access to the VM is managed via SSH keys. In the

"Add SSH keys" section, select the "Paste public keys" option. Paste the content of your
local public SSH key (typically found in ~/.ssh/id_rsa.pub on Linux or macOS) into the text
box. Alternatively, you can upload the file or have OCI generate a new key pair for you.
7.  Boot Volume: The Always Free tier includes a total of 200 GB of block volume storage.
The default boot volume size is 50 GB. To maximize storage for a single instance, you
can check "Specify a custom boot volume size" and set it to 200 GB.

8.  Create and Connect: Click the "Create" button. The instance will begin provisioning,

which may take a few minutes. Once the status turns to "Running," its public IP address
will be displayed on the instance details page. Use this IP address and your private SSH
key to connect. The default username for an Ubuntu image is ubuntu.
# Example SSH connection command
ssh -i /path/to/your/private_key ubuntu@<PUBLIC_IP_ADDRESS>

9.  Opening Firewall Ports: By default, OCI's Virtual Cloud Network (VCN) has a restrictive
firewall. To allow web traffic (e.g., for accessing the Pangolin dashboard), you must add
ingress rules.

○  Navigate to the instance's details page, click on the Virtual Cloud Network link, then

the Subnet link, and finally the Security List link.

○  Click "Add Ingress Rules."
○  Add a rule with Source CIDR 0.0.0.0/0 and Destination Port Range 443 for HTTPS

traffic. Add another for port 80 if needed.

○  Additionally, the OS firewall (iptables or ufw) on the instance itself may need to be

configured to allow traffic on these ports.

Secure Networking with Pangolin (Fossorial)

Pangolin is a self-hosted, identity-aware, tunneled reverse proxy. It serves as a powerful
open-source alternative to services like Cloudflare Tunnels, providing a secure method to
expose services running on the VPS to the internet without opening inbound firewall ports. It

achieves this by creating an encrypted, outbound WireGuard tunnel from a client on the host
network to the central Pangolin server. Pangolin then proxies traffic through this tunnel, adding a
layer of centralized authentication and access control.

Tutorial: Deploying Pangolin via Docker Compose

This tutorial outlines the manual installation of Pangolin on the newly provisioned VPS using
Docker Compose.

●  Prerequisites: A domain name is required. An A record for the Pangolin dashboard (e.g.,
pangolin.your-domain.com) and a wildcard CNAME or A record (*.your-domain.com)
should be pointed to the VPS's public IP address in your DNS provider, such as
Cloudflare.

1.  Create File Structure: SSH into the VPS and create the necessary directory structure for

Pangolin's configuration and persistent data.
mkdir -p pangolin-stack/config/traefik
cd pangolin-stack

2.  Create Configuration Files: Create the three essential configuration files within this

directory structure.

○  docker-compose.yml: This file defines the three core services: pangolin (the main
application and UI), gerbil (the WireGuard management server), and traefik (the
underlying reverse proxy that handles HTTP traffic).
# docker-compose.yml
version: '3.8'

services:
  pangolin:
    image: fosrl/pangolin:latest
    container_name: pangolin
    restart: unless-stopped
    volumes:
      -./config:/app/config
    depends_on:
      - traefik
    networks:
      - pangolin_net

  gerbil:
    image: fosrl/gerbil:latest
    container_name: gerbil
    restart: unless-stopped
    volumes:
      -./config:/app/config
    cap_add:
      - NET_ADMIN
    sysctls:
      - net.ipv4.ip_forward=1
    networks:

      - pangolin_net

  traefik:
    image: traefik:v2.10
    container_name: traefik
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      -./config/traefik:/etc/traefik
      -./config/letsencrypt:/letsencrypt
    networks:
      - pangolin_net

networks:
  pangolin_net:
    driver: bridge

○  config/config.yml: This is the main configuration for the Pangolin application itself.

# config/config.yml
dashboard_url: https://pangolin.your-domain.com # REPLACE
database:
  type: sqlite
  path: /app/config/db/db.sqlite

○  config/traefik/traefik_config.yml: This is the static configuration for Traefik, defining

entry points and the Let's Encrypt certificate resolver.
# config/traefik/traefik_config.yml
global:
  checkNewVersion: true
  sendAnonymousUsage: false

entryPoints:
  web:
    address: ":80"
    http:
      redirections:
        entryPoint:
          to: websecure
          scheme: https
  websecure:
    address: ":443"

providers:
  file:
    filename: /etc/traefik/dynamic_config.yml

    watch: true

certificatesResolvers:
  letsencrypt:
    acme:
      email: your-email@your-domain.com # REPLACE
      storage: /letsencrypt/acme.json
      httpChallenge:
        entryPoint: web

○  config/traefik/dynamic_config.yml: This file defines how Traefik routes traffic to the

Pangolin services.
# config/traefik/dynamic_config.yml
http:
  routers:
    pangolin-dashboard:
      rule: "Host(`pangolin.your-domain.com`)" # REPLACE
      service: "pangolin-app"
      entryPoints:
        - websecure
      tls:
        certResolver: letsencrypt

  services:
    pangolin-app:
      loadBalancer:
        servers:
          - url: "http://pangolin:3001"

3.  Launch the Stack: With the configuration files in place, start the services using Docker

Compose.
sudo docker compose up -d

4.  Initial Setup: After a few moments for Traefik to obtain SSL certificates, navigate to

https://pangolin.your-domain.com. You will be prompted with an initial setup screen to
create the first administrator account.

Tutorial: Exposing a Containerized Service

Now, with Pangolin running, we can expose another service (e.g., a container running our
CRAWL4AI application with a simple API) without opening any new ports.

1.  Create a Site in Pangolin: Log in to the Pangolin dashboard. Navigate to "Sites" and
click "Add Site." Give the site a name (e.g., vps-docker-network). This represents the
isolated network where the target service resides. A Newt ID and Newt Secret will be
generated. Copy these securely.

2.  Deploy the Newt Client: Newt is Pangolin's client that establishes the secure tunnel. Add

it as a new service to the docker-compose.yml file on the VPS.
# Add this service to your docker-compose.yml

  newt:
    image: fosrl/newt:latest
    container_name: newt
    restart: unless-stopped
    environment:
      - PANGOLIN_ENDPOINT=https://pangolin.your-domain.com #
REPLACE
      - NEWT_ID=2ix2t8xk22ubpfy # REPLACE with your Newt ID
      -
NEWT_SECRET=nnisrfsdfc7prqsp9ewo1dvtvci50j5uiqotez00dgap0ii2 #
REPLACE with your Newt Secret
    networks:
      - pangolin_net
Restart the stack with sudo docker compose up -d. The newt container will connect to the
gerbil service, establishing the tunnel.

3.  Create a Resource in Pangolin: Navigate to "Resources" in the dashboard and click

"Add Resource."

○  Give it a name (e.g., Scraper-API).
○  Select the vps-docker-network site created earlier.
○  For the "Upstream URL," provide the internal Docker DNS name and port of the

target service (e.g., http://crawl4ai-container:8080).

○  Assign it a public subdomain (e.g., scraper.your-domain.com).
○  Click "Create Resource".

The crawl4ai-container service is now securely accessible at https://scraper.your-domain.com,
proxied through Pangolin's encrypted tunnel. Access can be further restricted using Pangolin's
built-in user, role, and policy management features.
The emergence of powerful, self-hosted infrastructure tools like Pangolin indicates a significant
trend among technically proficient users towards "sovereign infrastructure." While managed
cloud services offer convenience, they can introduce concerns regarding cost, flexibility, and
data privacy. Pangolin provides an alternative by enabling users to deploy an enterprise-grade,
"zero trust" security architecture on their own terms, retaining full control over their security
posture and access policies. The substantial community engagement with such projects
highlights a strong demand for tools that deliver sophisticated functionality without vendor
lock-in, emphasizing ownership and control as a counter-movement to the
"everything-as-a-service" model.
Concurrently, the economic viability of ARM-based servers, driven by offerings like Oracle's
Ampere A1 free tier, is disrupting the cloud hosting market. Historically dominated by x86
processors, the server landscape is now seeing ARM emerge as a power-efficient and
cost-effective alternative. Oracle's strategy to provide substantial ARM-based resources for free
is a clear move to accelerate adoption. While software compatibility was once a major hurdle,
the widespread use of containerization and the increasing availability of multi-architecture
Docker images are mitigating this challenge. This development lowers the financial barrier for
compute-intensive projects, enabling developers and small teams to access significant
resources at little to no cost, provided they operate within the ARM ecosystem.

The Agentic Layer: Supercharging Development with

Gemini and MCP Servers

AI-Assisted Coding with Gemini and GitHub Copilot

The modern development workflow for this stack is fundamentally enhanced by the integration
of powerful AI coding assistants directly within Visual Studio Code. Both Google's Gemini and
GitHub Copilot serve as invaluable partners, accelerating development, improving code quality,
and reducing cognitive load across all components of the architecture.

●  Setup: The integration begins by installing the respective extensions from the VS Code

Marketplace.

○  For Gemini, search for and install the "Gemini Code Assist" extension. After

installation, a sign-in process with a Google Account is required to activate the
service.

○  For GitHub Copilot, install the "GitHub Copilot" extension. This requires an active

GitHub Copilot subscription and involves signing in to a GitHub account to authorize
the extension.

●  Practical Use Cases for this Stack:

○  Generating Scraper Logic: When working on the CRAWL4AI scraper, a developer
can use a natural language comment or a chat prompt to generate complex logic.
For instance, a prompt like, // Using CRAWL4AI and JsonCssExtractionStrategy,
extract the title, date, and PDF link from list items with the class 'publication-item'
can produce a complete, syntactically correct code block, saving significant time.
○  Refactoring Data Pipelines: As the dlt pipeline evolves, it may require refactoring
for better error handling or logging. A developer can select the entire Python script,
invoke the inline chat feature (typically with Ctrl+I), and issue a command like,
"Refactor this pipeline to include try-except blocks around the scraper execution
and log any exceptions to a file." The AI will then propose a diff with the suggested
changes directly in the editor.

○  Debugging and Optimizing SQL: When interacting with the MotherDuck

database, complex SQL queries can be difficult to write or debug. A developer can
paste a query into the Gemini chat pane and ask, "Explain this DuckDB SQL query.
Are there any performance optimizations I can make, such as using a different join
type or creating a materialized view?" The AI can provide both an explanation and
an improved version of the query.

○  Generating Dockerfiles and Configurations: Creating configuration files for

Docker, Docker Compose, or Pangolin can be tedious. A prompt such as, "Create a
multi-stage Dockerfile for a Python FastAPI application. The first stage should
install dependencies using poetry, and the final stage should be a slim production
image," can generate a robust starting point for containerizing any of the stack's
services.

Understanding Agentic Workflows and the Model Context Protocol
(MCP)

While AI-assisted coding significantly boosts productivity, the next evolution is the transition from
AI assistants that suggest code to AI agents that can take actions. This leap is facilitated by
providing the LLM with access to a set of "tools" it can execute to interact with its environment,

and the Model Context Protocol (MCP) is the open standard that makes this possible.

●  What is MCP?: MCP is a standardized communication protocol that defines how an LLM
client (like the Gemini extension in VS Code) can interact with a tool-providing server. It
formalizes the process of:

1.  Discovery: The client asks the server what tools it has available.
2.  Schema Definition: The server responds with a list of tools, including their names,
descriptions of what they do, and a structured schema (often JSON Schema) of the
parameters they accept.

3.  Execution: The LLM, understanding the available tools from their descriptions, can
decide to call one to fulfill a user's request. It formats a request according to the
tool's schema, which the client sends to the server. The server executes the tool's
logic and returns the result to the LLM.

MCP effectively creates a standardized, interoperable API layer for AI agents. This allows
developers to build custom tools and expose them to any MCP-compatible LLM, creating an
extensible and powerful ecosystem for agentic workflows.

Tutorial: Building and Containerizing a Custom MCP Server with
FastMCP

This tutorial demonstrates how to build a custom MCP server that provides a tool for querying
the MotherDuck database. We will use FastMCP, a Python framework that simplifies MCP
server development.

●  Why FastMCP?: Building an MCP server from scratch requires handling the low-level
details of the JSON-RPC protocol. FastMCP abstracts this complexity away, allowing
developers to define tools by simply decorating standard Python functions. It
automatically generates the necessary schemas from Python type hints and docstrings,
dramatically accelerating development.

1.  Server Setup and Tool Creation: The following Python script (mcp_server.py) defines a

FastMCP server with a single tool, query_motherduck.
# mcp_server.py
import os
import duckdb
from fastmcp import FastMCP
from typing import List, Dict, Any

# It's best practice to load secrets from environment variables
MOTHERDUCK_TOKEN = os.getenv("MOTHERDUCK_TOKEN")
DATABASE_NAME = "educational_data_lake" # As created in the
previous section

# 1. Create the FastMCP server instance
mcp = FastMCP(name="DataStackAgentServer")

# 2. Define the tool using the @mcp.tool decorator
@mcp.tool
def query_motherduck(query: str) -> List]:
    """

    Executes a read-only SQL query against the MotherDuck database
    and returns the results as a list of dictionaries.

    :param query: The SQL query string to execute.
    """
    if not MOTHERDUCK_TOKEN:
        return

    connection_string =
f"md:{DATABASE_NAME}?motherduck_token={MOTHERDUCK_TOKEN}"

    try:
        print(f"Executing query: {query}")
        # Connect to MotherDuck and execute the query
        with duckdb.connect(connection_string) as con:
            result = con.execute(query).fetch_arrow_table()
            # Convert Apache Arrow table to list of dicts for JSON
serialization
            return result.to_pylist()
    except Exception as e:
        print(f"Error executing query: {e}")
        return [{"error": str(e)}]

# 3. Add the main block to run the server via HTTP
if __name__ == "__main__":
    mcp.run(transport="http", host="0.0.0.0", port=8000)
This script provides a direct, programmatic bridge between the AI agent and the data
warehouse. The mcp-server-motherduck repository serves as an excellent
production-grade reference for this type of implementation.

2.  Containerizing the Server: To deploy the MCP server on the VPS, it needs to be

containerized.

○

requirements.txt:
fastmcp
duckdb

○  Dockerfile:

FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt.
RUN pip install --no-cache-dir -r requirements.txt

COPY mcp_server.py.

# Expose the port the server will run on
EXPOSE 8000

# The command to run the server
CMD ["python", "mcp_server.py"]

3.  Running the Server with Docker Compose: Add the MCP server to the main

docker-compose.yml file to run it alongside the other services.
# In your main docker-compose.yml
services:
  #... other services like pangolin, newt, etc.
  mcp-server:
    build:./path/to/mcp_server_directory
    container_name: mcp-server
    restart: unless-stopped
    ports:
      - "8000:8000" # Expose port 8000 to the VPS host
    environment:
      - MOTHERDUCK_TOKEN=${MOTHERDUCK_TOKEN} # Pass token from
an.env file
    networks:
      - pangolin_net

Tutorial: Configuring VS Code to Use the Custom MCP Server

With the server running in a Docker container on the VPS, the final step is to configure the
Gemini extension in the local VS Code environment to connect to it.

1.  Configuration File Location: VS Code can be configured to use MCP servers via a
workspace-specific .vscode/mcp.json file. This is the recommended approach for
project-specific tools.

2.  Connecting via HTTP Transport: Since the Docker container's port 8000 is exposed to
the VPS host, the MCP server is accessible over the network. The .vscode/mcp.json file
should be configured to connect to this HTTP endpoint.
//.vscode/mcp.json
{
    "servers": {
        "custom_data_agent": {
            "type": "http",
            "url": "http://<YOUR_VPS_PUBLIC_IP>:8000/mcp/"
        }
    }
}
After creating or saving this file, VS Code may require a reload. The Gemini extension will
then automatically discover the server and its query_motherduck tool.

3.  Invoking the Tool: Now, from the Gemini chat pane in VS Code, the developer can

directly invoke the custom tool using the @ syntax.User Prompt: @custom_data_agent
What are the top 5 most common exam subjects in the dataset?Agent's Thought
Process & Actions:

1.  Gemini: "The user wants to query the dataset. I have a tool called

query_motherduck provided by the @custom_data_agent server that can execute

SQL queries."

2.  Gemini: "I will formulate a SQL query to answer the user's question."
3.  Gemini triggers tool call: query_motherduck(query="SELECT exam_subject,

COUNT(*) as count FROM irish_exams.exam_results GROUP BY exam_subject
ORDER BY count DESC LIMIT 5;")

4.  VS Code: Sends this request to the MCP server at
http://<YOUR_VPS_PUBLIC_IP>:8000/mcp/.

5.  MCP Server: Executes the Python function, connects to MotherDuck, runs the

query, and gets the result.

6.  MCP Server: Returns the result (e.g., [{'exam_subject': 'Mathematics', 'count':

100},...]) as a JSON response.

7.  VS Code: Passes the result back to Gemini.
8.  Gemini: "I have the results from the database. I will now format this into a natural

language answer for the user."
Gemini's Final Response: "The top 5 most common exam subjects are:

1.  Mathematics (100 entries)
2.  English (98 entries) ..."

This demonstrates a complete, end-to-end agentic workflow where the AI seamlessly transitions
from understanding natural language to executing code on remote infrastructure to synthesizing
a final answer.
The MCP ecosystem, while nascent, is growing rapidly. The following table compares several
noteworthy, well-documented open-source implementations that can serve as excellent starting
points or production-ready solutions.
Project Name
FastMCP

Primary Language Key Features
Python

Ease of Use
Very High

philschmid/gemi
ni-mcp-server

Python

motherduckdb/m
cp-server-mother
duck

Python

centminmod/gem
ini-cli-mcp-server

Python

Decorator-based,
automatic schema
generation,
minimal
boilerplate.
Simple, focused
implementation of
Gemini-specific
tools (web search).

High

High

Low (Complex)

Production-ready,
specialized server
for
DuckDB/MotherDu
ck interaction.
Enterprise-grade,
highly modular
server with 30+
tools, Redis
caching, security
features, and
OpenRouter
integration.

Best For...
Rapidly
prototyping and
building custom
Python-based
tools.
Learning the
basics of MCP
with Gemini and a
simple remote
deployment.
Directly integrating
database query
capabilities into an
AI agent.

Building complex,
multi-tool,
multi-LLM agentic
systems for
production.

The emergence of the Model Context Protocol signifies a pivotal moment in the evolution of AI
development, suggesting a future governed by an "Operating System for AI Agents." Just as a
traditional OS provides a standardized set of system calls for applications to interact with
hardware, MCP provides a standardized API layer for LLMs to interact with external tools and
data. This abstraction moves AI models from being passive text generators to active participants
in a computational environment. This will likely lead to a new ecosystem of "AI
drivers"—specialized MCP servers—where the value and differentiation of an AI system will
depend less on the underlying model and more on the richness and power of the tool
ecosystem it can access.
This new paradigm also reframes the role of the developer. The workflow is shifting from a
human-centric model where AI provides assistance, to an AI-centric model where the human
provides supervision and enablement. The developer's primary task evolves from writing every
line of application logic to architecting and implementing the high-level goals and the specific
tools (via MCP servers) that empower an AI agent to achieve those goals. This represents a
fundamental and profound shift in the nature of software creation.

Synthesis and Strategic Recommendations

Summary of the Architecture and Key Learnings

This report has detailed a powerful, modern, and cost-effective architecture for AI-driven data
acquisition and analysis. The stack is intentionally modular, combining best-of-breed
open-source and low-cost services to create a whole that is greater than the sum of its parts.
The workflow begins with CRAWL4AI, an advanced scraping tool that extracts clean,
LLM-ready data from dynamic and authenticated websites. This data is seamlessly fed into a dlt
pipeline, a declarative Python framework that automates data loading, normalization, and
schema management. The pipeline's destination is a disaggregated data warehouse, using
Cloudflare R2 for zero-egress, S3-compatible object storage and MotherDuck's DuckLake
feature for serverless, in-situ SQL analytics.
This entire software stack is deployed in containers on a low-cost Hetzner or free-tier Oracle
Cloud VPS. Secure external access is managed by Pangolin, a self-hosted tunnel and
identity-aware proxy. The development lifecycle is accelerated within VS Code using AI
assistants like Gemini 2.5 Pro, which are further empowered by a custom MCP Server that
grants the AI agent direct, programmatic access to the system's components, such as the
MotherDuck database.
Key learnings from this architectural exploration include the immense economic and
performance advantages of disaggregating storage and compute, the power of Python-native
tools like dlt to simplify data engineering, and the transformative potential of agentic workflows
enabled by the Model Context Protocol.

Operationalizing the Stack: Monitoring, Maintenance, and Scaling

Deploying this stack into a production or semi-production environment requires consideration of
ongoing operations.
●  Monitoring:

○  Pipeline Health: dlt offers built-in telemetry and alerting capabilities that can be
configured to send notifications (e.g., to Slack or email) upon pipeline success or

○

failure. This provides crucial visibility into the health of the data ingestion process.
Infrastructure Resources: The VPS's CPU, memory, and disk usage must be
monitored. For a more sophisticated setup than standard Linux tools like htop and
df, a server management tool like Komodo can be deployed. Komodo provides a
web UI to connect to multiple servers, monitor system resources, and manage
Docker containers and deployments, offering a centralized control plane for the
infrastructure.

○  Service Logs: Centralized logging for all Docker containers (CRAWL4AI, dlt

runner, Pangolin, MCP server) is essential for debugging. This can be achieved
using the Docker logging drivers to forward logs to a service like Grafana Loki or a
simple file-based aggregation.

●  Maintenance:

○  Schema Changes: One of the primary advantages of dlt is its ability to handle

schema evolution automatically. When CRAWL4AI starts extracting a new field, dlt
will detect it and issue the appropriate ALTER TABLE commands to the destination,
minimizing manual intervention.

○  Software Updates: All components are containerized, simplifying updates.

Regularly pulling the latest Docker images for CRAWL4AI, Pangolin, and other
base images, and then redeploying with docker compose up -d --build, is the
standard maintenance procedure.

○  Credential Management: Secrets such as API keys and database tokens should

be managed securely. For Docker Compose, this means using .env files that are
excluded from version control. For more advanced setups, a dedicated secrets
manager like HashiCorp Vault could be integrated.

●  Scaling:

○  Storage and Analytics: The Cloudflare R2 and MotherDuck layers are serverless

and scale automatically to handle virtually any data volume. This part of the
architecture does not represent a scaling bottleneck.
Ingestion: The primary bottleneck is the single VPS running the CRAWL4AI
scraper. Scaling the ingestion process can be approached in several ways:

○

1.  Vertical Scaling: Upgrading the Hetzner/Oracle VPS to a more powerful

instance with more CPU cores and RAM.

2.  Horizontal Scaling: For large-scale scraping, a single VPS will be

insufficient. The architecture can be extended to a fleet of scraping nodes. A
central instance could run a task queue (e.g., RabbitMQ, Redis), and
multiple, cheap Hetzner cloud servers could act as workers, each running a
CRAWL4AI container that pulls jobs from the queue. Komodo could be used
to manage the deployment and monitoring of this fleet of worker nodes.

Future Outlook and Potential Extensions

The architecture presented here serves as a robust foundation that can be extended in
numerous directions.

●  Data Visualization and Serving: The data stored in MotherDuck can be easily

connected to business intelligence and visualization tools. A Streamlit or Dash application
could be built to provide an interactive dashboard for exploring the scraped educational
data. Alternatively, a lightweight API could be built (e.g., with FastAPI) that queries
MotherDuck and serves the data to other applications. This API could itself be

containerized and exposed securely through Pangolin.

●  Advanced AI Applications: The collected and cleaned dataset is a valuable asset for

more advanced AI tasks.

○  Fine-Tuning: The structured Markdown output from CRAWL4AI is ideal for
fine-tuning a smaller, specialized language model on the specific domain of
educational resources, potentially creating a highly effective chatbot or
question-answering system for students.

○  RAG Implementation: The data can be chunked, embedded, and stored in a
vector database to serve as the knowledge base for a Retrieval-Augmented
Generation system, allowing users to ask natural language questions about the
scraped content.

●  Deepening Agentic Integration: The custom MCP server can be expanded with more
powerful tools. For example, a tool could be created to trigger a dlt pipeline run on
demand (@agent.trigger_pipeline('irish_exams')), or another tool could be built to modify
the CRAWL4AI scraping configuration and redeploy the container. This would grant the
Gemini agent end-to-end control over the data lifecycle, moving closer to a fully
autonomous data acquisition system.

Final Recommendations

The software stack detailed in this report represents a highly capable, modern, and
economically efficient solution for the stated goal of scraping, processing, and hosting
educational resources. It is exceptionally well-suited for technically proficient individuals or small
teams who value control, flexibility, and open standards over the locked-in convenience of
monolithic cloud platforms.
The primary trade-off is one of operational responsibility versus cost and control. While this
self-hosted architecture can be operated for a fraction of the cost of a fully managed cloud
solution (and potentially for free), it requires a higher degree of technical expertise to set up and
maintain. The learning curve for new concepts like the Model Context Protocol is non-trivial, but
the power it unlocks in creating truly intelligent, agentic systems is immense.
This architecture is more than just a collection of tools; it is an embodiment of several key trends
shaping the future of data and AI engineering. The move towards disaggregated, open-source
components, the simplification of data pipelines through Python-native frameworks, and the rise
of AI agents as first-class participants in the development and operational loop all point to a
future where powerful data capabilities are more accessible, customizable, and intelligent than
ever before. For the user with the requisite skills, this stack provides a formidable platform for
innovation.

Works cited

1. Cloudflare R2 | Zero Egress Fee Object Storage,
https://www.cloudflare.com/developer-platform/products/r2/ 2. Pricing - R2 - Cloudflare Docs,
https://developers.cloudflare.com/r2/pricing/ 3. fosrl/pangolin: Identity-Aware Tunneled Reverse
Proxy ... - GitHub, https://github.com/fosrl/pangolin 4. Pangolin (beta): Your own tunneled
reverse proxy with authentication (Cloudflare Tunnel replacement) : r/selfhosted - Reddit,
https://www.reddit.com/r/selfhosted/comments/1hujxxo/pangolin_beta_your_own_tunneled_reve
rse_proxy/ 5. Crawling with Crawl4AI. Web scraping in Python has… | by Harisudhan.S -
Medium,

https://medium.com/@speaktoharisudhan/crawling-with-crawl4ai-the-open-source-scraping-bea
st-9d32e6946ad4 6. unclecode/crawl4ai: Crawl4AI: Open-source LLM Friendly ... - GitHub,
https://github.com/unclecode/crawl4ai 7. dlt: the data loading library for Python - dltHub,
https://dlthub.com/product/dlt 8. Moving Data with Python and dlt: A Guide for Data Engineers -
DataCamp, https://www.datacamp.com/tutorial/python-dlt 9. MotherDuck / DuckLake | dlt Docs -
dltHub, https://dlthub.com/docs/dlt-ecosystem/destinations/motherduck 10. DuckLake |
MotherDuck Docs, https://motherduck.com/docs/integrations/file-formats/ducklake/ 11. Secure
vps hosting made in Germany - Hetzner, https://www.hetzner.com/cloud-made-in-germany/ 12.
Oracle Cloud Free Tier, https://www.oracle.com/cloud/free/ 13. Docker Compose - Pangolin
Docs, https://docs.digpangolin.com/self-host/manual/docker-compose 14. Gemini Code Assist |
AI coding assistant, https://codeassist.google/ 15. philschmid/gemini-mcp-server - GitHub,
https://github.com/philschmid/gemini-mcp-server 16. Gemini CLI Tutorial Series — Part 8:
Building your own MCP Server - Medium,
https://medium.com/google-cloud/gemini-cli-tutorial-series-part-8-building-your-own-mcp-server-
74d6add81cca 17. Session Management - Crawl4AI Documentation (v0.7.x),
https://docs.crawl4ai.com/advanced/session-management/ 18. State Exams - ST. PATRICK'S
COMPREHENSIVE SCHOOL, https://www.stpatrickscomprehensive.ie/state-exams.html 19.
Identity Based Crawling - Crawl4AI Documentation (v0.7.x),
https://docs.crawl4ai.com/advanced/identity-based-crawling/ 20. Hooks & Auth - Crawl4AI
Documentation (v0.7.x), https://docs.crawl4ai.com/advanced/hooks-auth/ 21. Teaching
Resources: BBC Bitesize Computer Science,
https://computing.hias.hants.gov.uk/mod/url/view.php?id=112&forceview=1 22. Useful websites
and resources - St Thomas's Centre,
https://www.stthomasscentre.com/attachments/download.asp?file=523&type=docx 23. Create
and Submit a robots.txt File | Google Search Central | Documentation,
https://developers.google.com/search/docs/crawling-indexing/robots/create-robots-txt 24. What
is robots.txt? | Robots.txt file guide - Cloudflare,
https://www.cloudflare.com/learning/bots/what-is-robots-txt/ 25. Robots.txt Introduction and
Guide | Google Search Central | Documentation,
https://developers.google.com/search/docs/crawling-indexing/robots/intro 26. robots.txt
configuration - Security - MDN,
https://developer.mozilla.org/en-US/docs/Web/Security/Practical_implementation_guides/Robots
_txt 27. What are robots.txt files? Featuring 15 of our favourites - MCM.click,
https://mcm.click/what-are-robots-txt-files-featuring-15-of-our-favourites/ 28. An introduction to
robots.txt files - Digital.gov, https://digital.gov/resources/introduction-robots-txt-files 29. DuckDB
Data Engineering Glossary: data load tool (dlt) - MotherDuck,
https://motherduck.com/glossary/data%20load%20tool%20(dlt)/ 30. Cloud storage and
filesystem | dlt Docs - dltHub, https://dlthub.com/docs/dlt-ecosystem/destinations/filesystem 31.
Cloudflare R2 Import - DuckDB,
https://duckdb.org/docs/stable/guides/network_cloud_storage/cloudflare_r2_import.html 32. S3
API Support - DuckDB, https://duckdb.org/docs/stable/core_extensions/httpfs/s3api.html 33. dlt -
MotherDuck, https://motherduck.com/ecosystem/dlt/ 34. Data Warehousing How-to |
MotherDuck Docs, https://motherduck.com/docs/key-tasks/data-warehousing/ 35. Cloudflare R2
| MotherDuck Docs, https://motherduck.com/docs/integrations/cloud-storage/cloudflare-r2/ 36.
Hetzner | Review, Pricing & Alternatives - GetDeploying, https://getdeploying.com/hetzner 37.
Cheap hosted VPS by Hetzner: our cloud hosting services, https://www.hetzner.com/cloud 38.
How to Set Up a Free Oracle Cloud VM for Web Development (2025 Guide) - Hackernoon,
https://hackernoon.com/how-to-set-up-a-free-oracle-cloud-vm-for-web-development-2025-guide

39. oracle-cloud-free-tier-guide - GitHub Gist,
https://gist.github.com/rssnyder/51e3cfedd730e7dd5f4a816143b25dbd 40. Always Free
Resources - Oracle Help Center,
https://docs.oracle.com/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm 41.
unclecode/crawl4ai - Docker Image, https://hub.docker.com/r/unclecode/crawl4ai 42. Creating a
VM on Oracle Cloud(Using Always Free Resources) - Spoon Consulting,
https://blog.spoonconsulting.com/creating-a-vm-on-oracle-cloud-using-always-free-resources-8a
e23c507403 43. Setup Forever Free Oracle 24 GB RAM ARM-based Ampere Cloud - YouTube,
https://www.youtube.com/watch?v=BUxyD-IXP1s 44. Better Than Cloudflare Tunnels? -
Pangolin Guide - YouTube, https://www.youtube.com/watch?v=8VdwOL7nYkY 45. How to
Configure a Hetzner Domains DNS Records for Cloudflare - Blunix GmbH,
https://www.blunix.com/blog/how-to-configure-a-hetzner-domains-dns-records-for-cloudflare.htm
l 46. Pangolin: Easy Self-Hosted Tunneled Reverse Proxy with Built-in ...,
https://noted.lol/pangolin/ 47. Install and Configure Pangolin - Open Source is Awesome,
https://wiki.opensourceisawesome.com/books/self-hosted-tunnels/page/install-and-configure-pa
ngolin 48. Set up Gemini Code Assist for individuals - Google for Developers,
https://developers.google.com/gemini-code-assist/docs/set-up-gemini 49. Gemini Code Assist -
Visual Studio Marketplace,
https://marketplace.visualstudio.com/items?itemName=Google.geminicodeassist 50. GitHub
Copilot in VS Code, https://code.visualstudio.com/docs/copilot/overview 51. GitHub Copilot: Fly
With Python at the Speed of Thought, https://realpython.com/github-copilot-python/ 52. Gemini
Code Assist tools overview - Google for Developers,
https://developers.google.com/gemini-code-assist/docs/tools-agents/tools-overview 53.
Quickstart - FastMCP, https://gofastmcp.com/getting-started/quickstart 54. How to Create an
MCP Server in Python - FastMCP, https://gofastmcp.com/tutorials/create-mcp-server 55. How to
Build MCP Servers in Python: Complete FastMCP Tutorial for AI Developers,
https://www.firecrawl.dev/blog/fastmcp-tutorial-building-mcp-servers-python 56.
motherduckdb/mcp-server-motherduck: MCP server for ... - GitHub,
https://github.com/motherduckdb/mcp-server-motherduck 57. How to Use the DuckDB MCP
Server - Apidog, https://apidog.com/blog/duckdb-mcp-server/ 58. How to Use DuckDB MCP
Server - Apidog, https://apidog.com/blog/motherduck-duckdb-mcp-server-guide/ 59. Use MCP
servers in VS Code, https://code.visualstudio.com/docs/copilot/customization/mcp-servers 60.
Use agentic chat as a pair programmer | Gemini Code Assist - Google for Developers,
https://developers.google.com/gemini-code-assist/docs/use-agentic-chat-pair-programmer 61.
What is Komodo? | Komodo, https://komo.do/docs/intro

