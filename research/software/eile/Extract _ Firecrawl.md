---
title: "Extract | Firecrawl"
source: "https://docs.firecrawl.dev/features/extract"
author:
  - "[[Firecrawl Docs]]"
published:
created: 2025-12-21
description: "Extract structured data from pages using LLMs"
tags:
  - "clippings"
---
**Introducing Agent: The Next Evolution of Extract**  
We’re launching [`/agent`](https://docs.firecrawl.dev/features/agent) — the successor to `/extract`. It’s faster, more reliable, and doesn’t require URLs. Just describe what you need and let the AI agent find and extract the data for you. [Try Agent now →](https://docs.firecrawl.dev/features/agent)

The `/extract` endpoint simplifies collecting structured data from any number of URLs or entire domains. Provide a list of URLs, optionally with wildcards (e.g., `example.com/*`), and a prompt or schema describing the information you want. Firecrawl handles the details of crawling, parsing, and collating large or small datasets.

We’ve simplified billing so that Extract now uses credits, just like all of the other endpoints. Each credit is worth 15 tokens.

## Using /extract

You can extract structured data from one or multiple URLs, including wildcards:
- **Single Page**  
	Example: `https://firecrawl.dev/some-page`
- **Multiple Pages / Full Domain**  
	Example: `https://firecrawl.dev/*`
When you use `/*`, Firecrawl will automatically crawl and parse all URLs it can discover in that domain, then extract the requested data. This feature is experimental; email [help@firecrawl.com](https://docs.firecrawl.dev/features/) if you have issues.

### Example Usage

**Key Parameters:**
- **urls**: An array of one or more URLs. Supports wildcards (`/*`) for broader crawling.
- **prompt** (Optional unless no schema): A natural language prompt describing the data you want or specifying how you want that data structured.
- **schema** (Optional unless no prompt): A more rigid structure if you already know the JSON layout.
- **enableWebSearch** (Optional): When `true`, extraction can follow links outside the specified domain.
See [API Reference](https://docs.firecrawl.dev/api-reference/endpoint/extract) for more details.

### Response (sdks)

JSON

## Job status and completion

When you submit an extraction job—either directly via the API or through the starter methods—you’ll receive a Job ID. You can use this ID to:
- Get Job Status: Send a request to the /extract/ endpoint to see if the job is still running or has finished.
- Wait for results: If you use the default `extract` method (Python/Node), the SDK waits and returns final results.
- Start then poll: If you use the start methods— `start_extract` (Python) or `startExtract` (Node)—the SDK returns a Job ID immediately. Use `get_extract_status` (Python) or `getExtractStatus` (Node) to check progress.

This endpoint only works for jobs in progress or recently completed (within 24 hours).

Below are code examples for checking an extraction job’s status using Python, Node.js, and cURL:

### Possible States

- **completed**: The extraction finished successfully.
- **processing**: Firecrawl is still processing your request.
- **failed**: An error occurred; data was not fully extracted.
- **cancelled**: The job was cancelled by the user.

#### Pending Example

JSON

#### Completed Example

JSON

## Extracting without a Schema

If you prefer not to define a strict structure, you can simply provide a `prompt`. The underlying model will choose a structure for you, which can be useful for more exploratory or flexible requests.

JSON

Setting `enableWebSearch = true` in your request will expand the crawl beyond the provided URL set. This can capture supporting or related information from linked pages.Here’s an example that extracts information about dash cams, enriching the results with data from related pages:

JSON

The response includes additional context gathered from related pages, providing more comprehensive and accurate information.

## Extracting without URLs

The `/extract` endpoint now supports extracting structured data using a prompt without needing specific URLs. This is useful for research or when exact URLs are unknown. Currently in Alpha.

## Known Limitations (Beta)

1. **Large-Scale Site Coverage**  
	Full coverage of massive sites (e.g., “all products on Amazon”) in a single request is not yet supported.
2. **Complex Logical Queries**  
	Requests like “find every post from 2025” may not reliably return all expected data. More advanced query capabilities are in progress.
3. **Occasional Inconsistencies**  
	Results might differ across runs, particularly for very large or dynamic sites. Usually it captures core details, but some variation is possible.
4. **Beta State**  
	Since `/extract` is still in Beta, features and performance will continue to evolve. We welcome bug reports and feedback to help us improve.

## Using FIRE-1

FIRE-1 is an AI agent that enhances Firecrawl’s scraping capabilities. It can controls browser actions and navigates complex website structures to enable comprehensive data extraction beyond traditional scraping methods.You can leverage the FIRE-1 agent with the `/extract` endpoint for complex extraction tasks that require navigation across multiple pages or interaction with elements.**Example (cURL):**

> FIRE-1 is already live and available under preview.

## Billing and Usage Tracking

We’ve simplified billing so that Extract now uses credits, just like all of the other endpoints. Each credit is worth 15 tokens.You can monitor Extract usage via the [dashboard](https://www.firecrawl.dev/app/extract).Have feedback or need help? Email [help@firecrawl.com](https://docs.firecrawl.dev/features/).