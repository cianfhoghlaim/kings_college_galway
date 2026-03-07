Multi-Agent System Architecture with Agno &

BAML – Modular Teams Blueprint

Overview of the Architecture

This design introduces a  multi-agent AI system  organized into distinct domain-focused teams, each

orchestrated by  Dagster  pipelines with  DLT  for data ingestion. The architecture leverages  Agno  – an

open-source   framework   for   orchestrating   LLM   agents   –   in   combination   with  BAML   (Boundary   AI

Markup Language) to enforce structured prompt-response formats

1

2

. The data processing stack

unifies batch and streaming workflows by using DuckLake (DuckDB lakehouse) for batch storage and

optionally  RisingWave  for   streaming,   with  Ibis  providing   a   common   interface.   Processed   data

(embeddings,   facts,   metrics)   are   fed   into  CocoIndex  for   vector   indexing   and  Cognee  for   graph/

relational knowledge storage, built on PostgreSQL, LanceDB, and Memgraph

3

4

.

Figure   1  below   illustrates   the   high-level   architecture.   Dagster   orchestrates   three   example   pipelines

(Code Analysis, Sentiment Analysis, Financial Analytics). Each pipeline ingests data (via DLT or Crawl4AI),

indexes   it   (via   CocoIndex),   and   invokes   specialized   Agno   agents   that   produce   structured   outputs

defined by BAML. All results and enriched data flow into a unified knowledge base (Cognee). This allows

the teams to operate independently while contributing to a common semantic index and knowledge

graph.

Figure 1: Multi-agent architecture with domain-specific agent teams. Dagster orchestrates pipelines for each

team (code analysis, sentiment analysis, financial analytics). Each pipeline ingests data (via DLT or Crawl4AI),

updates the DuckLake data lake (with DuckDB + Postgres), triggers CocoIndex for vector indexing, and runs

Agno   LLM   agents   with   BAML   prompts   to   produce   structured   insights.   All   context   and   outputs   feed   into

Cognee’s unified knowledge base (PostgreSQL + Memgraph + LanceDB), enabling cross-domain query and

analysis.

Team 1: Code & Documentation Analysis

Responsibilities: This team focuses on analyzing software repositories – summarizing code structure,

evaluating   documentation,   and   extracting   key   findings.   It   produces   a   structured   report   of   the

repository’s purpose, architecture, code quality, and documentation status

5

6

.

Pipeline   Configuration:  The   code   analysis   pipeline   is   orchestrated   by   Dagster   and   consists   of   the

following stages:

•

Data Ingestion (DLT):  Using  DLT (Data Load Tool), the pipeline ingests repository files (source

code, READMEs, etc.) from a filesystem or git source into a DuckDB/DuckLake instance

7

8

.

DLT’s   filesystem   connector   automatically   scans   directories   and   loads   files,   treating   each   file’s

content as a record

9

10

. This stage runs on triggers (e.g. new repo added or code update) or

on a schedule.

•

Storage   &   Incremental   Updates:  The   repository   data   is   stored   in  DuckLake,   a   lakehouse

format for DuckDB that uses PostgreSQL for metadata and cloud/object storage (e.g. Cloudflare

1

R2) for file data

11

. Every commit of new files or changes results in new Parquet files and an

updated Postgres catalog entry, enabling versioning and multi-user access in DuckDB

12

. The

Postgres   catalog   acts   as   a  trigger   source  for   indexing:   when   repository   data   changes,   a

Postgres trigger (LISTEN/NOTIFY) signals CocoIndex to update the index

13

14

.

•

Indexing  (CocoIndex): CocoIndex  incrementally  indexes  the  repository  content  for  semantic

search. It ingests new or changed file records and generates vector embeddings (for code and

text) and summary metadata. Thanks to CocoIndex’s live update feature, only new/modified files

are   processed,   avoiding   re-embedding   unchanged   files

15

16

.   The   pipeline   uses   a   Postgres

“index tracking” table that CocoIndex listens to; as soon as new files are loaded and an entry is

upserted, CocoIndex receives a NOTIFY event and updates the vector index immediately

17

13

.

This   design   yields   low-latency   indexing   –   an   update   to   the   DuckLake   data   triggers   only   the

necessary embedding updates

18

. Dagster ensures this sequence (ingest -> update index table -

> CocoIndex) happens reliably in order

19

20

.

•

Agno Agents (LLM Analysis):  Once the repository’s content is indexed, an  Agno  agent (or a

small team of agents) performs the analysis. The system message for these agents defines their

role as a “software repository analyst” tasked with summarizing code and documentation

21

22

.

The agent is provided with context from the repository: either the raw text (for smaller repos) or
retrieved   snippets   via   CocoIndex’s   vector   search   (for   large   codebases   that   exceed   context

window)

23

24

. Example: The agent might first get an overview of the project structure (from a

packaged   context   like   a   Repomix   summary)   and   then   use   CocoIndex   to   lookup   specific   file

details on demand

24

. Agno supports such tool use, allowing the agent to call a search function

on the indexed data to dynamically fetch details

24

.

•

Structured Prompt & Output (BAML):  The agent’s prompt and response schema are defined

using BAML to ensure a consistent, parseable report format. In BAML, the task can be defined as

a function: input = repository content, output = structured report. For instance, we specify that the
agent’s   output   should   be   a   JSON   with   fields   like   "summary" ,   "key_findings" ,   and
"conclusion"

. This turns prompt engineering into  schema engineering, making the LLM

25

output deterministic in structure

2

. The agent’s instructions can be multi-step (e.g. “summarize

the code architecture, then list key findings, then give an overall conclusion”), guiding it to populate
each section. Using Agno’s Python API, one might configure an  Agent  with an output schema

and  stepwise  instructions  (checklist  style)  for  these  fields

25

26

.  The  output  is  a  structured

JSON object – for example:

{

"summary":

"This project is an IoT sensor logging system written in C++ and Python...",

"key_findings": [

"The Arduino code handles sensor data gathering using an external XYZ

library.",

"The Python backend has comprehensive unit tests, but logging is

rudimentary.",

"The repository lacks a CI/CD configuration."

],

"conclusion": "Overall, the project is functional and reasonably

documented..."

}

2

– The above format is an example of a BAML-defined schema for repository analysis

27

28

.

•

Multi-Agent Collaboration: Agno allows chaining multiple agents or calls in a deterministic flow

26

.   For   repository   analysis,   one   could   employ   separate   specialized   agents   –   e.g.   one   agent

focuses on code and another on documentation – then use a coordinator agent to merge their

findings

26

.   For   simplicity,   a   single   agent   can   also   handle   both   tasks   in   one   prompt,   as

illustrated above, since BAML ensures it returns all required sections

26

29

. In either approach,

the output is a structured report artifact.

•

Output Integration:  The final JSON report is saved (e.g. in a database or object storage) and

also injected into  Cognee. In Cognee’s graph, the repository could be represented as an entity

node with properties (e.g. languages, repo name) and relations (e.g.  Repo  –has→  Module). The
key findings might be added as linked notes or as vector embeddings (via LanceDB) for future

semantic search. The Cognee knowledge base thus retains the analysis so that other agents (or

users)   can   query   insights   about   this   repo   later.   The   vector   embeddings   produced   for   code

snippets   and   docs   are   stored   via   CocoIndex   (using   LanceDB   under   the   hood   for   similarity

search), and the structured facts (like presence of tests, absence of CI config) can be added as
relationships in the Memgraph graph (for example, Repo – lacks → CI_Config).

Triggers & Schedule: This pipeline can be triggered by version control events (e.g. a new commit or

pull request initiates a fresh analysis), or on a schedule (e.g. nightly scans of designated repositories).

Dagster’s scheduler or sensors can watch for new repository data and launch the pipeline

of DLT’s stateful extraction means only new or changed files are processed on each run

30

31

. The use

32

, and

CocoIndex’s incremental indexer likewise only processes new data, keeping the process efficient

15

33

.

Team 2: Sentiment Analysis & Social Feed Monitoring

Responsibilities:  The sentiment analysis team gathers and interprets unstructured data from news,

forums,  and  social  media  to  gauge  public  sentiment  and  trends.  The  goal  is  to  produce  structured

insights   such   as   sentiment   scores,   key   trending   topics,   and   alerts   on   sentiment   shifts.   This   team’s

output can help explain or contextualize events (for example, correlating social sentiment with market

movements

34

35

).

Pipeline Configuration: The sentiment pipeline handles continuous ingestion of text-rich content and

uses LLM agents to analyze sentiment. Key components:

•

Data Ingestion (Crawl4AI & APIs): For open web sources (blogs, news sites, forums like Hacker

News or Reddit), the pipeline uses  Crawl4AI, an AI-friendly web crawler

36

. Crawl4AI fetches

webpages and outputs clean, markdown-formatted text with minimal noise (e.g. it preserves

main content and reference links, making it LLM-ready)

36

37

. It can even employ LLM-based

strategies during crawl (e.g. asking it to extract specific info from pages)

38

. The crawler can run

on a schedule or be triggered by webhooks (for example, whenever new forum posts or news

articles   appear).   For   social   media   platforms   (like   Twitter/X   or   Discord),   where   content   may

require authentication or API access, the pipeline can use platform-specific APIs or a browser

automation   step.  Feasibility   of   Browser   Agents: Logging   into   Discord   or   similar   sites   via   a

headless browser is possible but not natively handled by Crawl4AI. In practice, one could incorporate

a browser automation agent (using a tool like Playwright or Puppeteer) as part of the pipeline.

Agno agents support tool use, so an agent could be endowed with a “browser” tool to navigate a

site,   login,   and   scrape   protected   content.   However,   this   requires   careful   setup   (managing

3

credentials   or   tokens)   and   might   be   limited   by   site   policies.   For  public   forums   like   Hacker

News, Crawl4AI is sufficient since the content is public HTML – it will scrape threads and output

text that the LLM can parse. For  Discord, a recommended approach is to use Discord’s official

API or webhooks to pull messages from channels (with appropriate authentication) and then

feed that text into the pipeline. In summary, Crawl4AI excels at crawling open websites; to integrate

data behind logins, the pipeline should leverage APIs or a headless browser agent.  (Crawl4AI does

provide   a   Python   API/CLI   for   integration

39

,   but   interactive   login   flows   would   need   custom

steps.)

•

Storage & Streaming: The ingested social/news data can be stored as documents in DuckLake

(e.g. as a table of posts or articles) for persistence. Additionally, if real-time processing is needed

(e.g. analyzing sentiment of live tweets or Discord messages), a streaming pipeline can be set up:

for  instance,  ingest  tweets  via  a  streaming  connector  (Kafka  or  API)  into  RisingWave,  which

maintains a live materialized view of recent sentiment metrics (like a rolling average sentiment).

Ibis  can  unify  this  with  the  batch   historical   data   –   the   same   transformation   (e.g.   computing

sentiment score per hour) can run on DuckDB for history and on RisingWave for live data

40

41

.   For   our   architecture,   suppose   we   treat   social   feed   ingestion   as   a   continuous   stream:

RisingWave could subscribe to a feed (via a connector or a custom source) and compute simple

aggregations (like count of positive/negative posts in the last 5 minutes). This can act as a trigger

for the LLM agent – e.g. if an unusual spike in negative sentiment is observed, trigger an analysis

agent to investigate the content.

•

Indexing (CocoIndex & Embeddings): CocoIndex indexes incoming text data (posts, comments,

articles) incrementally. New items (or even just new daily batches) are embedded into vector

representations (using language model embeddings) so that they can be semantically searched.

The index could include metadata like timestamp, source, and any preliminary sentiment score

from a classifier. As with Team1, CocoIndex live updates can be driven by Postgres triggers or

periodic refresh. The result is a continuously-growing vector store of textual content. This allows

the agents to pull  relevant context  efficiently – for example, fetching all posts about a certain

topic or with certain keywords when analyzing sentiment for that topic.

•

Agno   Agents   (Sentiment   Analysis):  The   team’s   Agno   agent(s)   take   the   raw   text   data   and

produce  a  structured  sentiment  analysis.  A  possible  setup  is  two  agents  in  sequence:  first  a

Topic   Extractor  and   then   a  Sentiment   Summarizer.   The  Topic   Extractor  agent   could   scan   a

batch of recent posts and identify major emerging topics or themes (using an LLM to cluster or

summarize discussions). The Sentiment Summarizer agent then takes those topics along with the

underlying posts as context, and produces a summary of sentiment for each (e.g. Topic A: mostly

positive   reception,   key   quotes...;   Topic   B:   community   is   negative   due   to...).   Alternatively,   a   single
agent can do both – e.g., “Analyze the following social posts and produce a JSON with  topics
and for each topic, the   sentiment   (Positive/Negative/Neutral) and a   summary   of opinions.”

This prompt would be defined in BAML with an output schema specifying a list of topics with

sentiment scores.  System Message & Prompt:  The agent is instructed as a  “sentiment analysis

expert AI that reads social and news content and evaluates the tone and opinions”. The input context

is   a   collection   of   recent   posts/articles   (fetched   via   CocoIndex’s   semantic   search   or   provided

directly from the ingestion stage). Because of BAML, we enforce that the output contain specific

fields   –   for   example:
top_positive_examples ,  top_negative_examples , and  notable_topics .

  overall_sentiment_score

(numerical

  or   categorical),

•

BAML-Structured Output: Using BAML, we define the expected Sentiment Report format. For

instance, the agent could output:

4

{

"overall_sentiment": "Slightly Negative",

"positive_trends": ["Many users excited about feature X", "..."],

"negative_trends": ["Complaints about service Y outage", "..."],

"notable_topics": [

{"topic": "New Feature X Launch", "sentiment": "Positive",

"summary": "Users celebrate the launch..."},

{"topic": "Service Y Outage", "sentiment": "Negative", "summary":

"Widespread frustration about..."}

]

}

This structured result provides a quick view of public sentiment, with supporting details. The
consistency of keys ( overall_sentiment ,  notable_topics , etc.) is guaranteed by the

BAML schema, making it easy to store and query later

2

42

.

•

Adaptive Analysis: The agent can be configured to use tools for deeper analysis. For example, if

quantifiable sentiment is needed, the agent might call a sentiment classifier function on each

post (though often the LLM can infer without an external classifier). Another use of tools: if an

agent wants to verify factual claims in the social data, it could perform a web search or query an

API as part of its reasoning (Agno supports such tool integrations, similar to how one might

equip an agent with a search or calculator tool). This is more relevant if the sentiment analysis

goes into why something is trending (e.g. verifying a news rumor).

•

Output   Integration:  The   resulting   sentiment   insights   are   stored   in  Cognee.   In   the   Cognee

knowledge   graph,   we   might   model   an   entity   for   each  Topic  or  Asset  being   discussed   (for

example, if this system is used in finance context, topics might be crypto tokens or stocks). The

sentiment could be stored as an attribute or related node (e.g.  TopicX  –has→  SentimentScore:
-0.2). The vector index (LanceDB) will store the embeddings of individual posts, which allows

future queries like “find posts similar to this issue” or “retrieve latest positive remarks about X”.

By integrating into the knowledge base, sentiment data can be correlated with other domain

data.   For   instance,   the  financial   team’s   agents  could   query   Cognee   to   retrieve   the   current

sentiment on a particular asset when generating an analysis (cross-team synergy).

Triggers & Streaming: This pipeline can run on a frequent schedule (e.g. every hour) to batch-process

new content, and also leverage streaming for real-time alerts.  Unified DuckLake vs RisingWave:  For

moderate volumes, scheduling DLT + CocoIndex in short intervals can approximate real-time processing

(with Postgres triggers ensuring index updates immediately)

17

. However, if truly low-latency handling

of   high-volume   streams   is   required,   integrating  RisingWave  is   advisable.   RisingWave   can   consume

streams (Twitter firehose, etc.) and maintain rolling metrics with sub-second latency

43

. Ibis enables

the same transformation (e.g. sentiment scoring logic) to run on both historical batch data (DuckDB)

and live streams (RisingWave) without duplicate code

40

41

. In practice, we might use RisingWave to

detect trigger conditions (like sentiment index crossing a threshold) and then use Dagster to kick off the

Agno agent analysis on-demand. Dagster’s sensors could listen to a message or event (via a lightweight

API or poll) indicating “alert: sentiment spike detected”, thereby invoking the sentiment analysis agent

immediately rather than waiting for a schedule.

5

Team 3: Financial Analytics & Anomaly Detection

Responsibilities: The financial analytics team ingests a wide range of structured and unstructured data

in the crypto/finance domain – market prices, on-chain transactions, DeFi protocol metrics, economic
news, etc. – and employs agents to identify trends, correlations, and anomalies. The focus is on  real-

time analytics  (e.g. detecting an on-chain anomaly as it happens) combined with  historical context

(trend analysis over time). This team’s outputs include structured reports on market trends, alerts for

unusual events, and answers to analytical queries, all grounded in data.

Pipeline   Configuration:  The   financial   pipeline   is   the   most   data-intensive,   combining   batch   ETL,

streaming processing, and AI analysis:

•

Data Ingestion (DLT for Batch & Connectors for Stream): Multiple data sources feed into this

pipeline

44

. For batch data, DLT is used to pull periodic snapshots: e.g. daily closing prices from

an exchange API, hourly DeFi protocol stats (TVL, yields) from an API or database, or periodic

snapshots of blockchain state (like a daily summary of new addresses). Each source is set up as a

DLT pipeline (with stateful tracking) writing into DuckDB. For example, one pipeline fetches new
Ethereum blocks and appends them to a  ethereum_blocks  table in DuckDB

, another pulls

45

yesterday’s   trading   volume   for   a   set   of   tokens,   etc.   DLT’s   incremental   loading   (with
write_disposition="merge"  and primary keys) ensures we don’t duplicate data on re-runs

46

47

. For streaming data, we integrate RisingWave to capture live events: e.g. subscribe to

blockchain transaction streams or mempool events, listen to a Kafka topic of real-time trades, or

ingest   database   change   logs   via   CDC

48

49

.   RisingWave’s   connectors   allow   direct   ingestion

from Postgres (for on-chain updates via logical decoding) or Kafka (exchange feeds) in real-time

50

51

. These streams populate RisingWave internal tables continuously. Using Ibis, we can

define   transformations   (windowed   aggregations,   joins)   on   these   streams   to   maintain   live

materialized views  – for example, a view of “total DEX trading volume in last 5 minutes” or

“count   of   large   transfers   (>$1M)   in   last   hour”   that   updates   continuously.  Ibis   Unified   Logic:

Crucially, the same Ibis-defined transformation can also be run on the historical data in DuckDB

for backfills or validation

40

. This unification avoids divergent definitions for metrics in batch vs

streaming.   A   semantic   layer   (defining   metrics   in   YAML,   as   per   the  “boring   semantic   layer”

approach) can further ensure consistency – define metrics once and execute them on either

engine   via   Ibis

52

53

.   In   short,   DuckLake   (DuckDB)   holds   the   long-term   dataset,   while

RisingWave provides the real-time incremental updates, and Ibis bridges them.

•

Storage   (DuckLake   &   Cognee):  All   batch   data   lands   in  DuckLake  tables   (Parquet-backed,

cataloged   in   Postgres),   ensuring   an   ACID   compliant   “data   lakehouse”   for   analytics

54

.   This

serves as the single source of truth for historical data (e.g. price history, historical metrics). The

streaming   results   from   RisingWave   can   be   periodically   persisted   to   DuckLake   as   well   (for

example, end-of-day, flush aggregated results to Parquet)

55

56

, so that no information is lost.

The   Cognee   knowledge   base   ingests   the   enriched   data:   it   builds   a  graph   of   entities   and

relationships  such   as:

 Token– hasPrice →PriceMetric,

 Protocol– hasTVL →Metric,

 Token–

mentionedIn →NewsArticle, etc.

3

57

. Because Cognee combines a graph DB with a vector

index,   it   can   store   numeric   facts   and   also   link   to   relevant   text   (news   or   social   posts)   via

embeddings

3

. For instance, a news article about a hack can be embedded and linked to the

affected protocol’s node in the graph. Memgraph (within Cognee) handles the graph queries –

e.g. find all protocols that have a relation to a certain exchange – and  PostgreSQL  can store

structured tables if needed (though often the data remains in DuckDB; PG might store lighter

metadata or serve as an index table for CocoIndex as discussed). LanceDB (vector store) either

underpins CocoIndex or is integrated in Cognee to handle semantic similarity searches (e.g. find

6

similar events or similar news). The net result is a rich, interconnected store of financial data,

events, and context – a “brain” that agents can query

58

57

.

•

Computed Features & Indexing (CocoIndex):  CocoIndex can be used here not only for text

embedding but also as an ETL layer for feature computation. For example, a CocoIndex flow
could watch the  ethereum_blocks  table for new blocks and compute a summary (number of

transactions, gas used, etc.), or generate embeddings for block metadata (if needed for anomaly

detection by an LLM). It can similarly process new protocol metric entries, attaching tags like

“outlier” if a value deviates strongly from history (potentially via an LLM call or statistical test).

Those results can be stored back (CocoIndex can output to new tables or indexes). Incremental

operation ensures that as each new data point arrives, only lightweight computations are done,

possibly   with   triggers   from   Postgres   (as   in   other   pipelines)   to   react   immediately

59

33

.   All

textual   data   (like   news   headlines,   on-chain   transaction   notes,   GitHub   issue   discussions   if

relevant) are embedded via CocoIndex into the vector index for semantic search. This allows

agents to perform  RAG (Retrieval-Augmented Generation)  by pulling in related context (e.g.

“find news articles mentioning this token in the last week” to explain a price move).

•

Agno   Agents   (Trend   Analysis   &   Anomaly   Detection):  Several   specialized   LLM   agents   are

orchestrated by Agno in this team, potentially working in collaboration

60

:

•

Trend Summarizer Agent: This agent’s role is to analyze recent data and produce a narrative of

market trends. Its system prompt might say: “You are a financial analyst AI tasked with

summarizing the latest trends in the crypto market. Use data provided (prices, volumes, sentiment, on-

chain metrics) to support your summary. Output a structured report.” The input context is pulled

from Cognee: for example, the agent queries Cognee for “latest key metrics for major tokens”

and “any notable events or news in the last 24h” – Cognee can return a bundle of relevant facts

(numbers, and maybe references to events) and vector-retrieved snippets (news headlines, social

sentiment highlights). Armed with this, the LLM produces a structured Market Trends Report.
Using BAML, we define sections such as  "market_overview" ,  "notable_events" ,
"metrics_summary"  and  "outlook"  in the output. The agent then fills these in, citing data

from the context (implicitly or explicitly). The output might look like: market_overview: “BTC and

ETH prices rose ~5%, with altcoins mostly green”; notable_events: list of events (exchange hack,

protocol launch, etc.); metrics_summary: key metrics (trading volumes up 20%, DeFi TVL down 2%,

etc.); outlook: a short forward-looking statement. This structure ensures the result is

comprehensive and machine-consumable (we can store each section in a database or display

nicely in a dashboard).

•

Anomaly Detector Agent: This agent focuses on identifying outliers or unusual patterns.

Triggered either on a schedule or by a real-time event (e.g. if a metric exceeds a threshold), it

examines the latest data for anomalies. The system message might frame it as: “You are an AI risk

analyst that detects anomalies or significant deviations in crypto data and explains them.” The agent

could be fed a digest of changes (e.g. a token’s price jumped by 20% in an hour, or an address
suddenly moved a large sum). The agent then outputs a structured alert, e.g.:  {"entity":
"Token ABC", "anomaly": "Price surged 20% in 1h", "likely_causes": "Rumors

of a partnership (from social media)", "impact": "Liquidations on XYZ

platform, trading volume tripled"} . To do this, the agent again uses Cognee – it might

query for any news or social sentiment related to that token around the time of the anomaly

(leveraging the indexed context) to provide an explanation

34

61

. Because multiple data types

are linked in the knowledge graph, the agent can correlate qualitative signals (tweets, articles)

with quantitative data (price, volume)

62

34

. BAML ensures the output has a fixed schema (so

an alert can be automatically ingested by an alerting system or further pipeline).

7

•

Q&A and Reasoning Agents:  In addition to proactive analysis, the team can include an agent

that answers ad-hoc questions using the Cognee knowledge base. For example, a  Query Agent

might take a user’s question (“Why did Stablecoin X de-peg last night?”) and decompose it: first

retrieve related info (price of X, news of any incident, on-chain data showing large movements),

then formulate an answer. This agent would heavily use the retrieval (vector search in CocoIndex

and graph queries in Cognee) to ground its response. Agno’s framework and Cognee’s API allow

such   an   agent   to   perform   multi-hop   reasoning:   e.g.   find   the   relevant   subgraph   of   entities

. The
(Stablecoin X, Protocol Y, etc.) and data, then call the LLM to synthesize an explanation
output   is   again   structured   (perhaps   a   JSON   with   fields   like   "cause_analysis"   and
"supporting_data_refs"  for traceability).

63

•

Output Integration: All reports and alerts generated are fed back into the Cognee knowledge

base and also can be emitted to external systems (dashboards, email alerts, etc.). In Cognee, a

Trend Report could be stored as a node linked to the date or period it covers, with edges pointing

to the entities it mentions (so you can query “what reports discussed Token ABC?”). An Anomaly

Alert  could similarly be logged in a “Anomaly” relation connected to the entity. Storing these

ensures the system has memory of past analyses – agents could even examine previous reports

to avoid repeating explanations or to see if an anomaly is recurring. Meanwhile, numeric outputs

might be stored in PostgreSQL or DuckDB for easier analytical queries (though Memgraph could
also store attributes on nodes for recent values). The combination of graph + relational + vector

in Cognee means the knowledge base can answer complex queries (some through Cypher/SQL,

some through embedding similarity), giving a rich context to any agent or application

57

4

.

Triggers & Continuous Processing: This pipeline is both schedule-driven and event-driven. A Dagster

schedule might trigger a nightly comprehensive analysis (producing a daily market report), while real-

time events  trigger focused agents: e.g., a RisingWave materialized view can be set with a condition

(like   a   high-value   transaction   counter)   and   use   a  sink  or   custom   notification   to   Dagster   when   a

condition is met

64

65

. Dagster’s sensors or a simple API call can then initiate the anomaly agent

immediately. The architecture allows scaling this: RisingWave handles high-throughput data and keeps

computations   like   aggregation   in   SQL   (sub-second   latency)

43

,   whereas   Agno   agents   handle   the

interpretation of those results. This synergy meets low-latency needs without requiring the LLM to run

on every single event. (For example, rather than the LLM reading every transaction, RisingWave filters to

“only if suspicious pattern X occurs” then engage the LLM.)

Unified Batch & Stream Considerations: Can DuckLake alone handle batch and streaming needs, or is

RisingWave required?  In this design, DuckLake with incremental updates covers a lot: using frequent

mini-batch ingestions and Postgres triggers, new data is indexed and available to agents nearly in real-

time

17

. This “unified DuckLake” approach keeps the system simpler (one primary storage engine) and

works well for moderate data velocities. Ibis provides a unified query interface, so one could simulate

streaming   by   polling   new   data   in   small   intervals   via   Ibis   on   DuckDB.   However,   for  true   streaming

semantics

(millisecond-level   updates,   continuous   SQL   queries,   very   high   event   throughput),

RisingWave is the better choice. RisingWave is purpose-built for real-time incremental processing with

materialized views, and it can ingest millions of events/sec with sub-10ms latencies on queries

43

.

DuckDB/DuckLake is not designed to maintain always-on incremental views; it excels at analytical batch

queries.   Therefore,   the  recommended   setup  is   to   use  both:   DuckLake   for   the   persistent   historical

store, and RisingWave for the live stream processing, unified via Ibis

41

66

. This yields a Lambda/

Delta-style architecture under one hood: RisingWave keeps a live view of recent data (a “speed layer”),

while DuckLake is the source of truth (batch layer)

66

67

. Ibis ensures that the transformations and

business logic are consistent across them. If the use case tolerates a bit of lag and lower complexity,

one  could  attempt  to  rely  on  DuckLake  +  frequent  Dagster  jobs  +  CocoIndex  triggers  only.  But  for

comprehensiveness, this blueprint includes RisingWave to cover high-performance streaming.

8

Integrated Knowledge Base and Orchestration

A core principle of this architecture is modularity with centralized knowledge. Each team operates its

own pipeline and agents, but the outputs coalesce into shared data stores (CocoIndex and Cognee) that
any agent can draw from. This encourages cross-domain insights – for example, a code analysis agent

could query Cognee to see if the repository it’s analyzing has any known security issues mentioned in

the security news (from the sentiment/news pipeline), or the financial agent could include developer

activity (from code repo data) as a factor in analysis.

Cognee   (Hybrid   Graph-Vector   Store):  Cognee   serves   as   the  collective   memory  and   single   query

interface for the agents. It combines multiple backends: - PostgreSQL – storing relational data or acting

as   indexing   metadata   store.   For   instance,   the   Postgres   could   hold   a   consolidated   table   of   all

“documents” or “events” ingested, with IDs and timestamps (which CocoIndex might use as a source

with triggers

14

68

). Also, any structured results (like a table of anomalies or daily summaries) can

reside here for easy querying. - Memgraph – an in-memory graph database for relationships between

entities.   All   entities   extracted   or   defined   by   the   agents   (repos,   code   components,   people,   tokens,

companies, etc.) become nodes in the graph, and relationships form edges (e.g. “Token A is mentioned

in   Document   X”   or   “Developer   Y   contributes   to   Repo   Z”).   This   allows   complex   queries   like   finding

connection   paths   between   items,   community   detection,   etc.   The   graph   enriches   the   context   by

capturing   how   pieces   of   information   relate.   -  LanceDB   (Vector   Index)  –   a   vector   similarity   search

engine   (built   on   Apache   Arrow).   All   embeddings   produced   by   CocoIndex   (from   code   snippets,

documents,   social   posts,   news,   etc.)   are   stored   here,   enabling   semantic   searches   like   “find   similar

documents to X” or “find posts discussing topic Y”. Cognee uses this to answer natural language queries

and to feed relevant chunks into LLM prompts. Example: An agent query “What are people saying about

Project Z in the last month?” could be handled by searching the vector index for top-N similar texts

about Project Z, then traversing the graph to filter by date or source, etc.

By aligning all processing outputs into Cognee, we ensure that each team’s work benefits the others.

Agents query Cognee via its API to retrieve context (Agno agents can be given a tool or function that

queries Cognee with a Cypher/SQL/embedding query)

63

. The response from Cognee might include a

mix of structured data and raw text excerpts, which the agent then incorporates into its reasoning. This

setup was envisioned such that  “agents can retrieve relevant context (latest prices, related news, etc.) via

Cognee’s API and then reason over it”

63

  – indeed, the knowledge base becomes the bridge between

data and AI reasoning.

Dagster   Orchestration: Dagster  is   the   control   plane   that   keeps   this   system   cohesive.   Each   team’s

pipeline is implemented as a Dagster job/graph with tasks (ops/assets) for each step (ingest, transform,

index, agent inference, etc.). Dagster’s scheduler and sensors handle triggering pipelines on schedules

or events

30

. It also provides monitoring – e.g. logging the token usage of LLM calls, caching results,

retrying failed steps, and materializing outputs. The  Dagster + DLT  combination is powerful:  “Dagster

lets you define ops for each step (extract, load to DuckLake, update index, etc.), and DLT handles incremental

extraction/loading   of   source   data”

69

70

.   This   ensures   data   moving   parts   are   robust.   For   example,

Dagster can orchestrate the flow: (1) fetch new data with DLT, (2) load to DuckLake, (3) update Postgres

index table (triggering CocoIndex), (4) invoke the Agno agent step, and (5) store agent results

32

20

. If

any step fails, Dagster can alert and manage retries, without bringing down the whole system. It also

allows modularity – each team’s pipeline can be developed and tested in isolation, then composed in a

larger schedule or triggered by inter-dependencies if needed.

Inter-Team Coordination: While each team focuses on its domain, Dagster can coordinate data flows

between them when necessary. For instance, the sentiment pipeline might feed a summary directly into

9

the financial pipeline (if an event is detected that should immediately trigger a market analysis). This

could be done via Dagster events or by writing to Cognee and having a sensor on the financial pipeline

watch for certain new nodes (e.g. a new “MajorEvent” node triggers deeper analysis). In practice, much

of the coordination is indirect through Cognee – since all data lands there, an agent in Team3 can

simply query what Team2 produced, rather than having a hard-coded pipeline dependency.

Security and Credentials:  If needed (for APIs, etc.), secrets would be managed via Dagster’s secrets

management or a vault (e.g. the Pulumi and 1Password integration mentioned in the docs for deploying

infrastructure

71

72

 ensures keys for APIs or database are handled safely).

Data Flow Summary and Source-Target Mapping

Finally,   we   summarize   how   data   flows   from   sources   to   targets   across   the   teams,   and   how   each

component maps to the technologies used:

•

Code/Repo Data → (DLT) → DuckLake/DuckDB → (CocoIndex) → LanceDB + Cognee: Source
. They
code files and documentation are ingested by DLT from local or remote repositories

73

land in DuckLake (Parquet + PG). CocoIndex picks up new files (via PG triggers) and generates

embeddings

13

16

, storing them in LanceDB (vector index). The content and extracted info also

populate Cognee’s graph (e.g. repo entity, relationships to developers or libraries). Agno’s Repo

Analyzer agent then reads from this index/graph and writes a structured repo report back into

Cognee (and maybe a file or DB table for reports).  Target: Cognee (knowledge graph enriched

with code insights, plus vector index for code text)

5

74

.

•

Documentation   &   Web   Content   →   (Crawl4AI)   →   DuckDB   →   (CocoIndex)   →   LanceDB   +
Cognee: Any external documentation or relevant web pages (like a project’s documentation site

or StackOverflow Q&A) can be crawled by Crawl4AI

37

. The cleaned text is stored and indexed

similar to above, becoming part of the context pool. Agents can use this to cross-reference what

official   docs   say   versus   the   code   (helpful   for   finding   discrepancies   or   missing   docs).  Target:

Cognee (doc nodes linked to repo, embeddings for semantic search).

•

Social   Media/News   →   (Crawl4AI/API)   →   DuckDB   (or   directly   to   index)   →   CocoIndex   →
LanceDB + Cognee: Tweets, forum posts, news articles are ingested either via APIs or crawling.

Some  transient  data  might  bypass  heavy  storage  and  go  straight  into  an  index  (especially  if

using streaming). Generally, storing raw text in a table (with timestamp, source) is useful for

auditing. CocoIndex embeds the text

15

. Cognee stores relationships (e.g. NewsArticle mentions

Token, or Tweet sentiment about Project). The sentiment analysis agent outputs structured trends

which are stored as separate nodes or records (e.g. a  SentimentReport  node connected to  Topic

nodes). Target: Cognee (sentiment metrics per topic, graph links between topics and entities like

products or tokens, vector index of all text for search).

•

Market/On-Chain Data (Batch) → (DLT) → DuckLake (Parquet) → CocoIndex/Transforms →
Cognee/PG: Structured numeric data (prices, volumes, on-chain metrics) flow through DLT into

DuckDB

45

. These are often stored as time-series tables. CocoIndex or custom transformations

compute derived features or labels (like marking unusual values) and insert those into Cognee’s

PG or graph (e.g. a  Metric  node for each day’s price with an edge to the  Token). The data also

stays in DuckDB for any heavy analytical queries.  Target:  Both Cognee (for graph queries and

quick access by agents) and DuckLake (for historical analysis and backup).

10

•

Real-Time Events (Stream) → RisingWave (Materialized views) → (Ibis) → DuckDB (sink) +
. RisingWave
Cognee: Live events like new transactions or trades stream into RisingWave

76

75

maintains computed views (e.g. rolling stats) that agents might query via Postgres interface or a

direct API. It can also output results: e.g. a continuous sink writes out anomalies or aggregated

stats   to   a   DuckDB   table   or   to   Cognee’s   PG.   When   RisingWave   flags   an   event,   it   triggers   the

corresponding agent. Target: Cognee (e.g. a new Anomaly node) and/or direct notifications.

•

Agent   Outputs   (Reports/Alerts)   →   Cognee   (Graph   +   Vector)   +   External   Outputs:  Every
structured output from the agents is stored in the knowledge base and also can be forwarded to

users. For example, a daily trend report could be indexed (so its text is searchable via vectors)

and also emailed to stakeholders. An anomaly alert JSON could be posted to a Slack channel and

also recorded in the graph DB. Cognee’s design ensures these outputs are integrated: an agent’s

conclusions become new knowledge that other agents can consume

4

77

.

Below is a  mapping table  summarizing each team with their primary data sources, processing tools,

and outputs:

Team /
Domain

Data

Sources

(Input)

Local git

Code &

repos, files,

Doc

docs

Analysis

(Markdown,

code files)

Social

media

posts

Sentiment

(Twitter,

Analysis

Discord),

news

articles,

forums

Processing & Tools

Outputs
(Structured)

Storage Targets

DLT (filesystem ingest)

⇒

DuckLake<br>CocoIndex

(file embeddings, code

chunking) ⇒
LanceDB<br>Agno

agents (Repo

Summarizer, Doc QA)

with BAML schema

25

26

Crawl4AI (web scraping

for news/HN) & API

Repository Report

JSON (summary,

key findings, etc.)

Cognee graph (Repo node,

27

<br>Doc

relations)<br>Cognee vector

completeness

index (code/docs)

analysis

(structured)

ingestion for

Sentiment

social<br>DLT (for API

Dashboard JSON

data) ⇒
DuckDB<br>CocoIndex

(text embeddings) ⇒
LanceDB<br>Agno

(overall sentiment,

topics,

examples)<br>Alert

on sentiment spike

agent (Sentiment

(JSON)

Analyzer) with BAML

schema

Cognee graph (Topic and

Sentiment nodes, e.g. Topic–

hasSentiment→Score)<br>Cognee
vector index (posts, articles)

11

Team /

Domain

Data

Sources

(Input)

Processing & Tools

Outputs

(Structured)

Storage Targets

Financial

Analytics

Market

data APIs

(prices,

DLT (batch API pulls) ⇒
DuckLake/DuckDB

44

45

<br>RisingWave

(CDC/Kafka streams) for

volumes),

real-time

43

<br>Ibis

blockchain

(unified transforms on

events

DuckDB & RisingWave)

(ETH, etc.),

40

<br>CocoIndex

protocol

(embedding text data,

stats (DeFi

computing features)

78

APIs),

<br>Agno agents (Trend

economic

Summarizer, Anomaly

news

Detector, Q&A) with

BAML

Market Trend

Report JSON

(sections: overview,

events, metrics)

34

<br>Anomaly

Alert JSON (entity,

description, cause)

<br>Q&A

responses

(structured answer

with references)

Cognee graph (entities: Tokens,

Protocols, etc. with edges for

metrics/events)

58

<br>Cognee

vector index (news, reports,

etc.)<br>DuckLake (historical

tables for metrics, Parquet)

Each   team’s   configuration   includes   a  system   prompt   (role)  for   its   agents,   the  input   context  they

consume (fed from the indexed data), the  output schema  enforced by BAML, and the  triggers  that

initiate  the  pipeline  or  agent  runs.  By  encapsulating   these  in   Dagster  pipelines,   we   achieve   a   clear

separation of concerns with unified outcomes. The table above highlights how different data types are

handled with appropriate tools, yet ultimately all roads lead to Cognee (knowledge base) and CocoIndex

(semantic index).

Integration Summary

In this modular blueprint,  Agno  provides the flexible agent orchestration needed for each analytical

task, while BAML guarantees that each AI agent’s output is well-structured and reliably parsed

1

2

.

The   use   of  Dagster   +   DLT  for   ingestion   and   pipeline   orchestration   brings   reliability   (incremental

loading,   scheduling,   error   handling)   to   feed   the   data-hungry   agents

69

20

.

 DuckLake  and

RisingWave  together   form   a   hybrid   data   backbone   –   DuckLake   for   durable   batch   storage   and

RisingWave for real-time streaming views, unified via Ibis for consistent transformations across both

modes

41

66

.  Crawl4AI  extends   the   system’s   reach   to   external   unstructured   data,   yielding   high-

quality textual context for the LLMs

36

. And Cognee sits at the center as the evolving knowledge graph

and index that every agent consults and updates –  “a persistent ‘brain’ of knowledge that supports both

structured queries and semantic lookups”

3

.

By grouping agents into domain-specific teams, we can tailor each team’s system message, tools, and

triggers to its particular needs, while still enabling collaboration. For example, the financial agents use

Cognee to pull in sentiment from Team2 and developer insights from Team1, painting a holistic picture

that   combines  quantitative  and  qualitative  analysis

34

61

.   Agno’s   multi-agent   workflow   capabilities

ensure that if a task is complex, agents can be chained or work in stages (as we did with topic extraction

then sentiment summary, or separate code and doc analysis)

26

. This approach also makes the system

extensible  – new teams (say, a Compliance Analysis team or Customer Support QA team) could be

added by plugging in another pipeline that feeds into the same backbone.

In   conclusion,   the   architecture   achieves   a  modular   yet   integrated  design:   each   team   is   a   self-

contained pipeline with clear responsibilities and structured LLM interactions, and all teams contribute

to a unified AI-driven data platform. The combination of structured prompt-response handling (BAML) with

12

robust   data   engineering   (DuckLake/Ibis/RisingWave,   DLT,   Dagster)  and  rich   knowledge   integration

(CocoIndex,   Cognee)  ensures   that   insights   are   derived   in   a   repeatable,   transparent,   and   real-time

manner. This blueprint can be implemented incrementally – starting perhaps with batch pipelines and a

few agents, and evolving towards streaming and more agents – to gradually build up an AI-powered

analytics system grounded in both data and knowledge.

Sources:

•

Agno & BAML usage for structured LLM outputs

1

2

•

CocoIndex incremental indexing with Postgres triggers (live updates)

13

16

•

Dagster pipeline orchestration with DLT for incremental ingestion

70

32

•

DuckLake lakehouse format (DuckDB with Postgres metadata)

11

 and Ibis unified batch/stream

logic

40

•

RisingWave streaming integration for real-time processing

43

66

•

Crawl4AI for AI-friendly web scraping and data extraction

36

37

•

Cognee knowledge base combining graph and vector search

3

4

•

Crypto analytics architecture example (multi-agent, Cognee, CocoIndex)

63

58

•

Sentiment analysis in context of crypto (news/social correlation)

34

61

1

2

5

6

7

8

9

10

21

22

23

24

25

26

27

28

29

42

73

74

End-to-End Workflow for

Analyzing Local Git Repositories with DLT, CocoIndex, Repomix, Agno, and BAM.pdf

file://file_000000000cf47246a5fe51c487848593

3

4

34

35

57

58

60

61

62

63

71

72

77

Crypto Analytics Project – Document Summaries and

Spec Update.pdf

file://file_000000004f0072468c419d09ca2c5363

11

12

13

14

15

16

17

18

19

20

30

31

32

33

59

68

69

70

Integrating DuckLake, CocoIndex, and

Dagster for Incremental Updates.pdf

file://file_000000008cbc71f487ff573d73602e63

36

37

38

39

LLM-Assisted Features in Dagster, DLT, Crawl4AI, and CocoIndex.pdf

file://file_0000000068bc7246bfc39455cbe012f1

40

41

52

53

55

56

64

65

66

67

Integrating RisingWave Streaming with DuckLake Batch ETL using

Ibis and a Semantic Layer.pdf

file://file_000000006e747246b8f5a1cf29e99291

43

48

49

50

51

75

76

Integrating RisingWave for Real-Time Streaming and Materialized Views.pdf

file://file_0000000066507246851c7597cc7fa894

44

45

46

47

54

78

Technical Integration Plan_ Dagster + DLT + CocoIndex + Feast + MLflow (with

DuckDB & Dragonfly).pdf

file://file_00000000867471f49c32fd1570040ee9

13



---

# Patterns Analysis

# Agno Codebase: Comprehensive Pattern and Ontology Analysis

## Executive Summary

The Agno framework is a lightweight, modular Python framework for building multi-modal, reasoning agents. The codebase demonstrates sophisticated architecture patterns centered around composable agent systems, knowledge management, and extensible tool ecosystems.

---

## 1. DOMAIN MODELS AND SCHEMAS

### 1.1 Core Agent Domain Model

**Agent Class** - Primary abstraction for agentic behavior
```python
Agent(
    id: str,                                    # Unique identifier
    name: str,                                  # Display name
    model: Model,                               # LLM model instance
    description: str,                           # Agent purpose
    role: str,                                  # Agent role/responsibility
    instructions: str | List[str],              # System prompts
    tools: List[Tool],                          # Available tools
    knowledge: Knowledge,                       # Knowledge base integration
    memory_manager: MemoryManager,              # Custom memory handler
    db: Db,                                     # Storage backend
    session_state: Dict,                        # Per-session state
    dependencies: Dict,                         # Runtime dependencies
    input_schema: BaseModel,                    # Input validation schema
    output_schema: BaseModel,                   # Output schema
    # ... 20+ configuration parameters
)
```

**Key Characteristics:**
- Fully configurable LLM backend
- Tool composition and automatic discovery
- Hybrid knowledge integration (vector + semantic + BM25)
- Session state management with automatic persistence
- Memory system for user/conversation context

### 1.2 Team Domain Model

**Team Class** - Multi-agent coordination
```python
Team(
    name: str,
    model: Model,                               # Team coordinator model
    members: List[Agent],                       # Team member agents
    instructions: str,                          # Team instructions
    db: Db,
    enable_agentic_memory: bool,                # Team-level memory
    enable_agentic_state: bool,                 # Team-level state
    session_state: Dict,                        # Team state
    show_members_responses: bool,               # Show member outputs
)
```

**Example:** OSS Maintainer Team with 5 specialized agents
- PR Review Council (code review expert)
- Issue Triage Specialist (prioritization)
- Security Guardian (vulnerability detection)
- Community Relations Manager (contributor engagement)
- Release Coordinator (release planning)

### 1.3 Workflow Domain Model

**Workflow Class** - Multi-step process orchestration
```python
Workflow(
    name: str,
    description: str,
    steps: List[Step],                         # Sequence of steps
    input_schema: BaseModel,                   # Workflow input
    db: Db,
    # Supports branching, parallel execution, error handling
)

Step(
    name: str,
    agent: Agent | None,                       # Individual agent
    team: Team | None,                         # Or team of agents
    # ... orchestration config
)
```

**Example:** Investment Workflow
1. Research Step (Team with Wikipedia + Search agents)
2. Analysis Step (Stock Analyst agent)
3. Ranking Step (Research Analyst agent)
4. Portfolio Allocation Step (Portfolio Manager agent)

### 1.4 Knowledge Domain Model

**Knowledge Class** - Unified knowledge management
```python
Knowledge(
    vector_db: VectorDb,                       # Embedding storage
    contents_db: Db,                           # Document storage
    embedder: Embedder,                        # Embedding model
    reranker: Reranker | None,                 # Optional reranking
    chunker: ChunkingStrategy,                 # Text chunking
    reader: DocumentReader,                    # Document parsing
)
```

**Supported Backends:**
- PgVector (PostgreSQL with pgvector)
- LanceDB (vector database)
- Chroma (in-memory/persistent)
- Pinecone, Milvus, MongoDB, Weaviate
- LocalAI, Ollama integrations

### 1.5 Memory Domain Model

**MemoryManager Class** - User/session memory handling
```python
MemoryManager(
    model: Model,                              # Summarizer model
    db: Db,
    additional_instructions: str,              # Memory creation rules
    # Custom system prompts for memory generation
)
```

**Memory Types:**
- User Memories: Persistent facts about users
- Session State: Conversation-specific state
- Chat History: Message history management
- Agentic Memory: Agent-managed context

---

## 2. COMPONENT INTERACTION PATTERNS

### 2.1 Agent Execution Flow

```
┌─────────────┐
│ User Input  │
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────┐
│ Pre-Hooks Execution              │
│ - Input validation               │
│ - Input transformation           │
│ - Dependency injection           │
└──────┬───────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│ Context Building                 │
│ - System prompt construction     │
│ - Session history inclusion      │
│ - Knowledge base search          │
│ - Memory retrieval               │
│ - Dependency resolution          │
└──────┬───────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│ LLM Inference                    │
│ - Model prediction               │
│ - Tool selection                 │
│ - Streaming/structured output    │
└──────┬───────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│ Tool Execution Loop              │
│ - Tool call parsing              │
│ - Pre-hook (tool-level)          │
│ - Execute tool                   │
│ - Post-hook (tool-level)         │
│ - Result feedback to LLM         │
└──────┬───────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│ Post-Hooks Execution             │
│ - Output validation              │
│ - Output transformation          │
│ - Side effects (notifications)   │
└──────┬───────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│ State Persistence                │
│ - Session state save             │
│ - Memory update                  │
│ - Chat history storage           │
└──────┬───────────────────────────┘
       │
       ▼
┌─────────────┐
│ Final Output│
└─────────────┘
```

### 2.2 Knowledge Base Integration Pattern

```
Query (text)
    │
    ▼
Retrieve (semantic search)
    │
    ├─ BM25 (keyword search)
    ├─ Vector search (embedding-based)
    └─ Hybrid search (combined)
    │
    ▼
Rerank (optional, using dedicated reranker)
    │
    ▼
Context Window
    │
    ▼
LLM Response with Sources
```

### 2.3 Team Execution Pattern

```
User Query
    │
    ▼
Team Coordinator (receives query)
    │
    ├─ Task analysis
    ├─ Agent routing decision
    │
    ├─ Route to Member 1 ──┐
    ├─ Route to Member 2 ──┤ (Parallel or Sequential)
    └─ Route to Member 3 ──┘
    │
    ▼
Response Synthesis
    │
    ├─ Aggregate member responses
    ├─ Synthesize insights
    └─ Format final response
```

### 2.4 Workflow Execution Pattern

```
Workflow Input (validated against input_schema)
    │
    ▼
Execute Step 1 (Team/Agent)
    │
    ├─ Capture output
    ├─ Validate against schema
    └─ Store in workflow state
    │
    ▼
Execute Step 2 (using previous output as input)
    │
    ├─ Branching support
    ├─ Error handling/recovery
    └─ State propagation
    │
    ▼
Execute Step N...
    │
    ▼
Workflow Output (aggregate all steps)
```

---

## 3. ONTOLOGIES AND DATA MODELS

### 3.1 Agent Ontology

**Agent Types by Capability:**
- Basic Agent: Simple instruction following
- RAG Agent: Knowledge-augmented (Retrieval-Augmented Generation)
- Agentic RAG Agent: Knowledge retrieval as tool
- Reasoning Agent: Extended thinking capability
- Multimodal Agent: Image/video understanding
- Team Coordinator: Multi-agent orchestration

**Agent Specialization:**
- Role-based (e.g., "Financial Advisor", "Code Reviewer")
- Capability-based (e.g., has_knowledge_base, enable_user_memories)
- Model-based (gpt-4, claude-3.5, open-source models)

### 3.2 Tool Ontology

**Tool Categories:**
1. **External Integration Tools**
   - Web Search (DuckDuckGo, Tavily, Exa)
   - Social Media (Twitter, LinkedIn, Reddit)
   - Productivity (Gmail, Slack, Jira, GitHub)
   - Finance (YFinance, Financial Datasets API)
   - Information (Wikipedia, ArXiv, PubMed)

2. **Data Processing Tools**
   - CSV Tools, Pandas Tools
   - File Generation, File Operations
   - Visualization, Data transformation

3. **System Tools**
   - Shell/Command execution
   - Python code execution
   - Docker integration

4. **Specialized Tools**
   - Memory Tools (user memory management)
   - Knowledge Tools (knowledge base search)
   - MCP Tools (Model Context Protocol)
   - Custom Tools (user-defined functions)

**Tool Composition Pattern:**
```python
class CustomToolkit(Toolkit):
    def __init__(self, ...):
        super().__init__(...)
        self.register(self.method1)
        self.register(self.method2)
    
    def method1(self, ...): ...
    def method2(self, ...): ...
```

### 3.3 Model Ontology

**Supported Model Families:**
- OpenAI (GPT-4, GPT-5, o1, o3-mini)
- Anthropic (Claude 3/3.5/4)
- Google (Gemini, LLama via Vertex)
- Open Source (Ollama, LocalAI)
- Specialized (Cerebras, Groq, Azure OpenAI)

**Model Capabilities:**
- Base Models: Standard chat completion
- Reasoning Models: Extended thinking (o1, o3-mini)
- Vision Models: Image understanding
- Code Models: Programming-specific
- Embedders: Text to vectors (OpenAI, Cohere, local)

### 3.4 Vector Database Ontology

**Vector DB Implementations:**
- PostgreSQL + pgvector (relational with vector extension)
- LanceDB (fast vector DB)
- Pinecone (cloud vector DB)
- Chroma (embedded/cloud)
- MongoDB Atlas (document + vector)
- Milvus (open-source vector DB)
- Weaviate (GraphQL-based)

**Search Type Options:**
- Keyword (BM25): Lexical matching
- Vector (Semantic): Embedding similarity
- Hybrid: Combined keyword + semantic
- Semantic (Vector only with reranking)

### 3.5 Database Ontology

**Storage Backends:**
- **Relational**: PostgreSQL, MySQL, SingleStore
- **Key-Value**: Redis, DynamoDB
- **Document**: MongoDB, Firestore
- **Specialized**: SurrealDB (hybrid)
- **In-Memory**: In-memory storage for testing
- **Cloud**: Google Cloud Storage, AWS S3

**Usage Pattern:**
```python
db = PostgresDb(db_url="postgresql://...")
# Stores: sessions, chat history, memories, knowledge
```

---

## 4. API PATTERNS

### 4.1 Agent Execution API

**Synchronous Execution:**
```python
response = agent.run(input="...", user_id="...", session_id="...")
# Returns: RunOutput with content, messages, metrics

agent.print_response(input="...")  # Streams to stdout
```

**Asynchronous Execution:**
```python
response = await agent.arun(input="...", stream=True)
# Async variant for concurrent execution

async for chunk in agent.arun(input="...", stream=True):
    # Stream tokens in real-time
    print(chunk.content, end="", flush=True)
```

**Streaming with Events:**
```python
async for event in agent.arun(input="...", stream_intermediate_steps=True):
    if event.event == RunEvent.tool_call_started:
        print(f"Tool: {event.tool.tool_name}")
    elif event.event == RunEvent.run_content:
        print(event.content, end="", flush=True)
```

### 4.2 Tool Definition API

**Function-Based Tools:**
```python
@tool(requires_confirmation=False)
def my_tool(param1: str, param2: int) -> dict:
    """Tool description for LLM."""
    return {"result": ...}

agent = Agent(tools=[my_tool])
```

**Toolkit Class Pattern:**
```python
from agno.tools.toolkit import Toolkit

class CustomTools(Toolkit):
    def __init__(self):
        super().__init__()
        self.register(self.tool_method)
    
    def tool_method(self, param: str) -> str:
        """Registered as a tool."""
        return ...
```

**Pre/Post Tool Hooks:**
```python
def tool_pre_hook(tool, arguments):
    # Validate/transform arguments
    return modified_arguments

def tool_post_hook(tool, result):
    # Process result
    return modified_result
```

### 4.3 Hook System API

**Agent-Level Hooks:**
```python
def pre_hook(run_input: RunInput) -> None:
    """Called before agent execution."""
    # Input validation/transformation

def post_hook(run_output: RunOutput) -> None:
    """Called after agent execution."""
    # Output validation/transformation/side effects

agent = Agent(
    pre_hooks=[pre_hook],
    post_hooks=[post_hook],
)
```

**Hook Exception Handling:**
```python
from agno.exceptions import InputCheckError, OutputCheckError, CheckTrigger

raise InputCheckError(
    "Invalid input",
    check_trigger=CheckTrigger.INPUT_NOT_ALLOWED
)

raise OutputCheckError(
    "Invalid output",
    check_trigger=CheckTrigger.OUTPUT_NOT_ALLOWED
)
```

### 4.4 Structured I/O API

**Input Validation:**
```python
from pydantic import BaseModel, Field

class ResearchQuery(BaseModel):
    topic: str
    focus_areas: List[str] = Field(description="...")
    sources_required: int = Field(default=5)

agent = Agent(input_schema=ResearchQuery)
```

**Output Structuring:**
```python
class AnalysisResult(BaseModel):
    findings: List[str]
    confidence_score: float
    recommendations: List[str]

agent = Agent(output_schema=AnalysisResult)
response = agent.run(input="...")
# response.content is instance of AnalysisResult
```

### 4.5 Dependency Injection API

**Runtime Dependency Injection:**
```python
def expensive_function():
    # Computed at runtime, once per agent.run()
    return get_market_data()

agent = Agent(
    dependencies={"market_data": expensive_function},
    instructions="Use {market_data} in your analysis"
)
```

**Automatic Dependency Injection:**
```python
agent = Agent(
    dependencies={"top_news": get_news},
    add_dependencies_to_context=True
)
```

---

## 5. COMMON CODING PATTERNS

### 5.1 Configuration Pattern

**Modular Configuration:**
```python
# Agents inherit from modular components
agent = Agent(
    model=OpenAIChat(id="gpt-4o"),
    tools=[DuckDuckGoTools(), YFinanceTools()],
    knowledge=Knowledge(vector_db=PgVector(...), embedder=OpenAIEmbedder()),
    db=PostgresDb(db_url=...),
    memory_manager=MemoryManager(model=Claude(...)),
)
```

**Environment-Driven Configuration:**
```python
import os
api_key = os.getenv("OPENAI_API_KEY")
model = OpenAIChat(id="gpt-4o")
```

### 5.2 State Management Pattern

**Session State:**
```python
agent = Agent(
    session_state={"shopping_list": []},
    instructions="Current list: {shopping_list}"
)

# State auto-persisted to DB, accessible across runs
state = agent.get_session_state()
```

**Dynamic State:**
```python
def update_state(session_state):
    session_state["counter"] = session_state.get("counter", 0) + 1

agent = Agent(change_state=update_state)
```

**Agentic State:**
```python
team = Team(
    enable_agentic_state=True,
    session_state={"project": "Agno", "version": "2.1.0"}
)
# Team manages state across conversations
```

### 5.3 Async Pattern

**Concurrent Agent Execution:**
```python
import asyncio

async def gather_reports():
    tasks = [
        agent1.arun("Task 1"),
        agent2.arun("Task 2"),
        agent3.arun("Task 3"),
    ]
    return await asyncio.gather(*tasks)

results = asyncio.run(gather_reports())
```

**Stream Processing:**
```python
async for chunk in agent.arun(input="...", stream=True):
    # Process tokens as they arrive
    await websocket.send(chunk.content)
```

### 5.4 Error Handling Pattern

**Graceful Degradation:**
```python
try:
    response = agent.run(input="...")
except InputCheckError as e:
    # Pre-hook validation failed
    return handle_invalid_input(e)
except OutputCheckError as e:
    # Post-hook validation failed
    return handle_invalid_output(e)
```

### 5.5 Type Safety Pattern

**Pydantic for All Schemas:**
```python
from pydantic import BaseModel, Field

class Agent Config(BaseModel):
    name: str
    role: str = Field(description="Agent role")
    tools: List[str]
    
    model_config = ConfigDict(extra="forbid")
```

**Type Hints Throughout:**
```python
def process_agent_response(response: RunOutput) -> dict:
    """Type hints for IDE support and validation."""
    return {"content": response.content}
```

### 5.6 Documentation Pattern

**Docstring Standard:**
```python
def my_tool(param1: str, param2: int) -> dict:
    """
    Brief description for LLM and documentation.
    
    Args:
        param1: Description for param1
        param2: Description for param2
    
    Returns:
        dict: Returned structure
    """
```

---

## 6. PLUGIN AND EXTENSION PATTERNS

### 6.1 Tool Plugin Pattern

**Custom Tool Registration:**
```python
from agno.tools.toolkit import Toolkit

class MyServiceTools(Toolkit):
    def __init__(self, api_key: str, ...):
        super().__init__()
        self.api_key = api_key
        self.register(self.fetch_data)
        self.register(self.process_data)
    
    def fetch_data(self, ...): ...
    def process_data(self, ...): ...

# Usage: agent = Agent(tools=[MyServiceTools(api_key=...)])
```

### 6.2 Custom Model Integration

**Model Plugin Pattern:**
```python
from agno.models.base import Model

class CustomModel(Model):
    def response(self, messages: List[Message], **kwargs) -> str:
        # Implement your custom inference
        pass

# Usage: agent = Agent(model=CustomModel(...))
```

### 6.3 Custom Database Backend

**Database Plugin Pattern:**
```python
from agno.db.base import Db

class CustomDb(Db):
    def create_session(...): ...
    def get_session(...): ...
    def update_session(...): ...
    # Implement storage interface

agent = Agent(db=CustomDb(...))
```

### 6.4 Hook Extensions

**Custom Middleware Hooks:**
```python
def validation_hook(run_input: RunInput) -> None:
    """Custom pre-processing."""
    if not is_valid(run_input.input_content):
        raise InputCheckError(...)

def transformation_hook(run_output: RunOutput) -> None:
    """Custom post-processing."""
    run_output.content = transform(run_output.content)

agent = Agent(
    pre_hooks=[validation_hook],
    post_hooks=[transformation_hook],
)
```

### 6.5 Tool Hook Pattern

**Fine-Grained Tool Control:**
```python
def tool_requires_approval(tool_name: str, arguments: dict) -> bool:
    return tool_name in ["delete_user", "modify_system"]

def approve_tool_execution(tool_name: str, arguments: dict) -> bool:
    # Custom approval logic
    return user_confirms(f"Execute {tool_name}?")

agent = Agent(
    tools=[...],
    tool_hooks=[...],  # Pre/post tool execution
)
```

---

## 7. STATE MANAGEMENT PATTERNS

### 7.1 Session State Pattern

**Per-User Session State:**
```python
agent.run(
    input="Add milk to my list",
    user_id="user123",
    session_id="session_abc"
)

# State persisted in DB, retrieved on next call
state = agent.get_session_state(user_id="user123", session_id="session_abc")
```

### 7.2 User Memory Pattern

**Persistent User Memories:**
```python
agent = Agent(
    enable_user_memories=True,
    db=PostgresDb(...)
)

agent.run(
    input="I'm John and I like hiking",
    user_id="john@example.com"
)

# System automatically creates user memory:
# "John likes hiking as a hobby"

memories = agent.get_user_memories(user_id="john@example.com")
```

### 7.3 Agentic Memory Pattern

**Team-Level Memory:**
```python
team = Team(
    enable_agentic_memory=True,
    members=[...],
    db=PostgresDb(...)
)

# Team learns about context and remembers
# across multiple conversations
```

### 7.4 Chat History Pattern

**Automatic History Management:**
```python
agent = Agent(
    add_history_to_context=True,
    num_history_runs=5,  # Last 5 messages
    read_chat_history=True,  # Tool to fetch full history
    db=PostgresDb(...)
)

# History auto-managed, can be customized
```

### 7.5 Dynamic State Updates

**On-Run State Modification:**
```python
def update_visit_count(session_state):
    session_state["visit_count"] = session_state.get("visit_count", 0) + 1
    session_state["last_visited"] = datetime.now().isoformat()

agent = Agent(
    session_state={"visit_count": 0},
    change_state=update_visit_count
)
```

---

## 8. WORKFLOW AND PROCESS PATTERNS

### 8.1 Sequential Workflow Pattern

**Research -> Analyze -> Write Pipeline:**
```python
research_step = Step(name="Research", team=research_team)
analysis_step = Step(name="Analysis", agent=analyst_agent)
writing_step = Step(name="Write", agent=writer_agent)

workflow = Workflow(
    name="Content Pipeline",
    steps=[research_step, analysis_step, writing_step],
    db=PostgresDb(...)
)

result = workflow.run(input={"topic": "AI trends"})
```

### 8.2 Branching Workflow Pattern

**Conditional Execution:**
```python
Step(
    name="Route Decision",
    agent=router_agent,
    # Next step depends on output
)

# Parallel execution
Step(
    name="Parallel Analysis",
    agents=[agent1, agent2, agent3],
)
```

### 8.3 Multi-Agent Collaboration Pattern

**OSS Maintainer Team Workflow:**
```
User Query
    ├─ PR Review Council → Code quality analysis
    ├─ Security Guardian → Vulnerability check (parallel)
    ├─ Issue Triage Agent → Issue categorization
    ├─ Community Manager → Contributor engagement
    └─ Release Coordinator → Release planning

Team Coordinator synthesizes all responses
```

### 8.4 Knowledge-Heavy Workflow

**Research Workflow with RAG:**
```
User Query
    │
    ▼
Research Team (Wikipedia + Web Search)
    │
    ├─ Wikipedia Agent searches knowledge
    └─ Search Agent retrieves web data
    │
    ▼
Aggregated Research Data
    │
    ▼
Analysis Agent with Knowledge Base Access
    │
    ├─ Semantic search
    ├─ Hybrid search with reranking
    └─ Source citation
    │
    ▼
Comprehensive Report
```

### 8.5 Human-in-the-Loop Workflow

**Approval Workflow:**
```
Agent generates action
    │
    ├─ Tool requires_confirmation=True
    │
    ▼
Ask user for confirmation
    │
    ├─ Approve → Execute
    ├─ Deny → Skip
    └─ Modify → User provides modified parameters
    │
    ▼
Continue execution
```

---

## 9. ADVANCED PATTERNS

### 9.1 Parser Model Pattern

**Multi-Stage Output Processing:**
```python
agent = Agent(
    model=OpenAIChat(id="gpt-4o"),  # Reasoning
    output_model=OpenAIChat(id="o3-mini"),  # Structured output
    output_schema=AnalysisResult,
)

# Two-stage: generate then structure
response = agent.run(input="...")  # Structured per schema
```

### 9.2 Concurrent Tool Calls

**Parallel Tool Execution:**
```python
agent = Agent(
    tools=[tool1, tool2, tool3],
    parallel_tool_calls=True,
)

# LLM can call multiple tools simultaneously
# Agno executes them in parallel
```

### 9.3 Multimodal Handling

**Image/Video Processing:**
```python
from agno.tools import Toolkit

class MediaTools(Toolkit):
    def analyze_image(self, image_path: str) -> str:
        """Analyze image at path."""
        from PIL import Image
        img = Image.open(image_path)
        return extract_text(img)

agent = Agent(tools=[MediaTools()])
```

### 9.4 Custom Chunking Strategy

**Document Preprocessing:**
```python
from agno.knowledge.chunker import Chunker

class CustomChunker(Chunker):
    def chunk(self, text: str) -> List[str]:
        # Custom splitting logic
        return chunks

knowledge = Knowledge(
    vector_db=PgVector(...),
    chunker=CustomChunker(...)
)
```

### 9.5 Custom Reranking

**Rerank Search Results:**
```python
from agno.knowledge.reranker import Reranker

knowledge = Knowledge(
    vector_db=PgVector(...),
    reranker=CohereReranker(model="rerank-v3.5"),
    # Or: reranker=CustomReranker()
)
```

---

## 10. UNIQUE AND NOVEL PATTERNS

### 10.1 Dependency Injection at Runtime

Unlike traditional frameworks, agno allows **function-based dependency injection** where dependencies are computed fresh for each agent run:

```python
def current_market_conditions():
    return fetch_latest_market_data()  # Called per run

agent = Agent(
    dependencies={"market": current_market_conditions},
    instructions="Consider the market: {market}"
)
```

### 10.2 Structured Output with Parser Models

The **two-stage reasoning + structuring** pattern:
1. Main model generates comprehensive analysis
2. Parser model formats into structured schema
3. Automatic validation against schema

### 10.3 Agentic Knowledge Searching

Knowledge isn't just context injection—agents get **search tools**:
```python
agent = Agent(
    knowledge=Knowledge(...),
    search_knowledge=True,  # Gives agent search tool
)
# Agent decides WHEN and WHAT to search for
```

### 10.4 Seamless Async Integration

Full async support with unified API:
```python
await agent.arun(...)  # Async execution
async for chunk in agent.arun(..., stream=True):  # Streaming
```

### 10.5 Multi-Backend State Management

State automatically synced across multiple backends:
```python
agent = Agent(
    session_state={...},
    db=PostgresDb(...),  # Chat history + memories
    knowledge=Knowledge(
        vector_db=PgVector(...),  # Knowledge storage
        contents_db=PostgresDb(...)  # Document content
    )
)
```

---

## 11. BEST PRACTICES EVIDENT IN CODE

### 11.1 Composition Over Inheritance
- Agents composed of models, tools, knowledge, memory
- Teams composed of agents
- Workflows composed of steps

### 11.2 Explicit Over Implicit
- Explicit tool registration vs auto-discovery
- Explicit hook declaration
- Explicit state management

### 11.3 Type Safety First
- Pydantic BaseModel for all schemas
- Type hints throughout
- Validation at boundaries

### 11.4 Sensible Defaults
- Pre-configured models
- Default hook behaviors
- Optional but powerful features

### 11.5 Extensibility
- Plugin patterns for tools, models, databases
- Custom hooks at multiple levels
- Custom chunkers, rerankers, embedders

### 11.6 Production Ready
- Session persistence
- Memory management
- Error handling with custom exceptions
- Structured logging
- Metrics and telemetry

---

## 12. INTEGRATION PATTERNS

### 12.1 Web Search Integration
```python
from agno.tools.duckduckgo import DuckDuckGoTools

agent = Agent(
    tools=[DuckDuckGoTools()],
    instructions="Search the web when needed"
)
```

### 12.2 GitHub Integration
```python
from agno.tools.github import GithubTools

agent = Agent(
    tools=[GithubTools(access_token=os.getenv("GITHUB_TOKEN"))],
)

# Can: fetch PRs, issues, create comments, etc.
```

### 12.3 Financial Data Integration
```python
from agno.tools.yfinance import YFinanceTools
from agno.tools.financial_datasets import FinancialDatasetsTools

agent = Agent(
    tools=[YFinanceTools(), FinancialDatasetsTools()],
)
```

### 12.4 MCP (Model Context Protocol)
```python
from agno.tools.mcp import MCPTools

agent = Agent(
    tools=[MCPTools(server_url=...)],
)
```

---

## Summary Table: Pattern Categories

| Pattern | Purpose | Example |
|---------|---------|---------|
| **Domain Model** | Define core entities | Agent, Team, Workflow, Knowledge |
| **Composition** | Combine components | Agent(tools=[], knowledge=..., db=...) |
| **Execution Flow** | Multi-stage processing | Input → Context → LLM → Tools → Output |
| **Hook System** | Intercept and modify | pre_hooks, post_hooks, tool_hooks |
| **State Management** | Persist user context | session_state, user_memories, agentic_state |
| **Tool Plugin** | Extend capabilities | Toolkit subclass registration |
| **Streaming** | Real-time output | async for chunk in agent.arun(...) |
| **Workflow** | Multi-step orchestration | Workflow with Step sequences |
| **Team Coordination** | Multi-agent sync | Team with member routing |
| **Error Handling** | Graceful degradation | CheckError with CheckTrigger |

---

## Key Insights

1. **Modular Architecture**: Every component (model, tool, db, knowledge) is pluggable
2. **Async-First**: Full async support for streaming and concurrency
3. **Type-Safe**: Pydantic for all I/O, eliminating runtime surprises
4. **State-Aware**: Sophisticated state management at session/user/team level
5. **Production-Ready**: Persistence, memory, error handling built-in
6. **Extensible**: Multiple extension points without modifying core
7. **Composable**: Agents → Teams → Workflows create powerful abstractions
8. **Knowledge-Native**: RAG as first-class feature, not add-on



---

# AgentOS Research

# AgentOS: Comprehensive Research Report

**Repository:** https://github.com/buildermethods/agent-os
**Version Analyzed:** v2.1.1
**License:** MIT
**Creator:** Brian Casel (Builder Methods)

---

## Executive Summary

AgentOS is a spec-driven development framework designed to transform AI coding agents from "confused interns into productive developers." Rather than being an agent framework in the traditional sense, it's better understood as an **operating system for spec-driven development workflows**—a structured methodology and tooling system that guides AI agents through repeatable, standards-compliant software development processes.

---

## 1. Core Purpose & Philosophy

### The Problem It Solves

AI coding agents struggle with three fundamental challenges:

1. **Lack of Context:** Agents don't inherently understand project architecture, coding standards, or team conventions
2. **Inconsistent Quality:** Without guidance, agents produce code that requires extensive iteration and correction
3. **Workflow Chaos:** Ad-hoc prompting leads to fragmented, non-repeatable development processes

### Value Proposition

AgentOS addresses these issues through a **three-layer context system** that encodes organizational knowledge into executable specifications:

- **Standards Layer:** Team coding conventions, style guides, and technical preferences
- **Product Layer:** Vision, mission, roadmap, and use-case documentation
- **Specs Layer:** Feature-specific specifications with implementation details

The core philosophy: **"Quality code on the first try"** through structured specifications rather than iterative prompting.

### Design Philosophy

AgentOS is built on several key principles:

1. **Spec-Driven Development:** Replace ad-hoc AI prompting with structured, documented workflows
2. **Modular Composition:** Build large processes from small, reusable components (standards, workflows, commands)
3. **Configuration Over Convention:** Support both multi-agent orchestration and single-agent workflows
4. **Profile-Based Inheritance:** Enable configuration sharing and customization through hierarchical profiles
5. **Pragmatic Over Perfect:** Emphasize speed, token efficiency, and practical outcomes over comprehensive coverage

---

## 2. Key Features & Capabilities

### Core Features

**1. Six-Phase Development Workflow**

AgentOS structures development into six distinct, composable phases:

- **plan-product:** Create mission, roadmap, and tech stack documentation
- **shape-spec:** Research and gather requirements for a feature
- **write-spec:** Generate detailed specification documents
- **create-tasks:** Break specs into organized, executable task groups
- **implement-tasks:** Execute implementation following specs and standards
- **orchestrate-tasks:** Coordinate multi-step implementations across task groups

**2. Dual-Mode Architecture**

Supports both:
- **Multi-Agent Mode:** Specialized Claude Code subagents handle specific phases (60-80% context reduction)
- **Single-Agent Mode:** Sequential numbered prompts guide a single agent through workflows

**3. Standards Management**

Hierarchical organization of coding standards:
- **Global:** coding-style, commenting, conventions, error-handling, tech-stack, validation
- **Backend:** api, migrations, models, queries
- **Frontend:** Component patterns, state management, styling
- **Testing:** test-writing guidelines emphasizing minimal, strategic coverage

**4. Profile System**

Inheritable configuration profiles enabling:
- Team-specific standards sharing
- Project-specific customization
- Version-controlled conventions (via git repositories)

**5. Template Processing Pipeline**

Sophisticated compilation system that:
- Processes conditional blocks (IF/UNLESS tags)
- Expands workflow references
- Injects standards at compile time
- Supports multiple output formats (Claude Code commands, Agent OS commands, Claude Code skills)

### What Developers Can Do

- **Bootstrap Projects:** Install pre-configured development workflows into any codebase
- **Standardize Teams:** Share coding conventions and architectural patterns across projects
- **Guide AI Agents:** Provide structured context that reduces iteration cycles
- **Maintain Consistency:** Ensure all AI-generated code follows team standards
- **Scale Knowledge:** Capture and distribute expert knowledge through specifications
- **Customize Workflows:** Adapt the six-phase process to specific project needs

### Primary Use Cases

1. **Greenfield Development:** Starting new products with clear vision, specs, and standards
2. **Legacy Codebases:** Adding structured workflows to existing projects
3. **Team Collaboration:** Sharing development standards across distributed teams
4. **AI-Assisted Development:** Guiding Claude Code, Cursor, Windsurf, and other AI tools
5. **Feature Development:** From planning through implementation with consistent patterns
6. **Knowledge Capture:** Documenting architectural decisions and technical preferences

---

## 3. Patterns & Architecture

### Architectural Patterns

**1. Profile-Based Inheritance**

Hierarchical configuration system where profiles can inherit from parent profiles:

```
profiles/
  default/
    agents/
    commands/
    standards/
    workflows/
  custom-profile/  (inherits from default)
```

Files are resolved through inheritance chain traversal with exclusion patterns preventing unintended inheritance.

**2. Template Processing Pipeline**

Multi-stage compilation process:

```
Source Template
  → Conditional Compilation (IF/UNLESS)
  → Workflow Expansion ({{workflows/...}})
  → Standards Injection ({{standards/*}})
  → PHASE Tag Embedding
  → Output (Commands/Agents/Skills)
```

**3. Multi-Output Compilation**

Single source files compile to multiple formats:
- `.claude/commands/agent-os/` - Claude Code commands
- `agent-os/commands/` - Generic agent commands
- `.claude/agents/agent-os/` - Claude Code subagents
- `.claude/skills/` - Claude Code skills

Controlled by configuration flags:
- `claude_code_commands: true/false`
- `use_claude_code_subagents: true/false`
- `agent_os_commands: true/false`
- `standards_as_claude_code_skills: true/false`

**4. Lazy Resolution**

Resources are loaded on-demand through inheritance chain traversal, preventing duplicate context loading and optimizing token usage.

**5. Safe Replacement Strategy**

Uses temporary files and Perl for multiline text substitution, avoiding shell escaping issues with complex template replacements.

### System Structure

**Installation Structure (Base)**

```
~/agent-os/
  ├── scripts/
  │   ├── base-install.sh
  │   ├── common-functions.sh
  │   ├── create-profile.sh
  │   ├── project-install.sh
  │   └── project-update.sh
  ├── profiles/
  │   └── default/
  │       ├── agents/
  │       ├── commands/
  │       ├── standards/
  │       ├── workflows/
  │       └── claude-code-skill-template.md
  ├── config.yml
  └── CHANGELOG.md
```

**Project Structure (Per-Project Installation)**

```
project-root/
  ├── agent-os/
  │   ├── product/
  │   │   ├── mission.md
  │   │   ├── roadmap.md
  │   │   └── tech-stack.md
  │   ├── specs/
  │   │   └── YYYY-MM-DD-feature-name/
  │   │       ├── planning/
  │   │       │   ├── requirements.md
  │   │       │   └── visuals/
  │   │       ├── spec.md
  │   │       ├── tasks.md
  │   │       ├── orchestration.yml
  │   │       └── implementation/
  │   ├── standards/
  │   │   ├── global/
  │   │   ├── backend/
  │   │   ├── frontend/
  │   │   └── testing/
  │   └── config.yml
  ├── .claude/
  │   ├── commands/agent-os/  (if enabled)
  │   ├── agents/agent-os/    (if subagents enabled)
  │   └── skills/             (if skills enabled)
```

### Design Principles

1. **Modular Composition:** Break workflows into reusable pieces combined at compile-time
2. **Configuration-Driven Behavior:** Validation rules enforce logical constraints
3. **Normalization First:** Normalize inputs (lowercase, hyphen-separated) before processing
4. **Conditional Loading:** Only load context when needed to optimize token usage
5. **Template-Based Generation:** Use templates for consistency across generated files
6. **Safe Defaults with Flexibility:** Provide sensible defaults while allowing customization

---

## 4. Ontology & Concepts

### Key Terminology

**Agents**

Specialized AI personas with specific roles in the development workflow:

- **product-planner:** Creates mission, roadmap, and tech stack documentation
- **spec-initializer:** Sets up spec folder structure
- **spec-shaper:** Researches and gathers requirements
- **spec-writer:** Generates specification documents
- **spec-verifier:** Validates specifications (deprecated in v2.1)
- **tasks-list-creator:** Breaks specs into task groups
- **implementer:** Executes implementation tasks
- **implementation-verifier:** Validates implementation quality

Each agent has:
- **Name:** Identifier for the agent
- **Color:** UI organization (cyan, purple, etc.)
- **Tools:** Available capabilities (Write, Read, Bash, WebFetch, Playwright)
- **Workflow:** Embedded instructions and process guidance

**Commands**

User-invokable workflows that orchestrate development phases:

- `/plan-product` - Create product documentation
- `/shape-spec` - Research feature requirements
- `/write-spec` - Generate specifications
- `/create-tasks` - Break specs into tasks
- `/implement-tasks` - Execute implementations
- `/orchestrate-tasks` - Coordinate multi-step implementations

Commands exist in two forms:
- **multi-agent:** Delegate to specialized subagents
- **single-agent:** Sequential numbered prompts (e.g., `1-product-concept.md`, `2-create-mission.md`)

**Standards**

Reusable coding conventions organized hierarchically:

- **Global Standards:** Cross-cutting concerns (coding-style, conventions, error-handling)
- **Domain Standards:** Backend, frontend, testing-specific guidelines
- **Tech Stack:** Technology selections and framework requirements

Standards can be:
- Embedded in command prompts (default)
- Delivered as Claude Code Skills (optional)

**Workflows**

Reusable process templates embedded in commands:

- **Planning:** gather-product-info, create-product-mission, create-product-roadmap, create-product-tech-stack
- **Specification:** initialize-spec, research-spec, write-spec, verify-spec
- **Implementation:** compile-implementation-standards, create-tasks-list, implement-tasks

Workflows use `{{workflows/category/name}}` syntax for embedding.

**Profiles**

Configuration packages containing agents, commands, standards, and workflows:

- Support inheritance from parent profiles
- Enable team-specific customization
- Version-controlled via git repositories

**Specs**

Feature-specific documentation organized by date:

```
YYYY-MM-DD-feature-name/
  ├── planning/
  │   ├── requirements.md      # User requirements and research
  │   └── visuals/             # Mockups and design assets
  ├── spec.md                  # Technical specification
  ├── tasks.md                 # Task groups and checklists
  ├── orchestration.yml        # Implementation coordination
  └── implementation/          # Implementation artifacts
```

**Task Groups**

Collections of related implementation tasks:

- Organized by specialization (database, API, frontend, testing)
- Include 2-8 focused tests per group (16-34 total per feature)
- Have acceptance criteria and dependencies
- Tracked via checkboxes in `tasks.md`

### Mental Model

AgentOS models software development as a **spec-driven pipeline**:

```
Product Vision
  ↓
Feature Idea
  ↓
Requirements Research (shape-spec)
  ↓
Technical Specification (write-spec)
  ↓
Task Breakdown (create-tasks)
  ↓
Implementation (implement-tasks / orchestrate-tasks)
  ↓
Verified Delivery
```

Each phase produces **artifacts** (documents) that inform subsequent phases. Standards provide **constraints** that ensure consistency. Agents provide **expertise** for specific tasks.

The framework emphasizes:
- **Documentation Before Code:** Specs precede implementation
- **Reuse Before Creation:** Search for existing patterns first
- **Standards Compliance:** All outputs respect team conventions
- **Minimal Testing:** Strategic coverage over comprehensive tests
- **Iterative Refinement:** Verify specs before implementation begins

---

## 5. Integration & Usage

### Installation & Setup

**Step 1: Base Installation**

```bash
# Install AgentOS to ~/agent-os
curl -sSL https://raw.githubusercontent.com/buildermethods/agent-os/main/scripts/base-install.sh | bash
```

Creates base directory with scripts, profiles, and configuration.

**Step 2: Customize Standards**

Edit standards in `~/agent-os/profiles/default/standards/` to match team preferences:

```
~/agent-os/profiles/default/standards/
  ├── global/
  │   ├── tech-stack.md       # Technology selections
  │   ├── conventions.md      # Project organization
  │   └── ...
  ├── backend/
  ├── frontend/
  └── testing/
```

**Step 3: Project Installation**

```bash
# Within your project directory
~/agent-os/scripts/project-install.sh

# With options
~/agent-os/scripts/project-install.sh \
  --profile=custom \
  --claude-code-commands=true \
  --use-claude-code-subagents=true
```

Configuration options:
- `--profile=NAME` - Select configuration profile
- `--claude-code-commands=true/false` - Install Claude Code commands
- `--use-claude-code-subagents=true/false` - Enable multi-agent delegation
- `--agent-os-commands=true/false` - Install generic commands
- `--standards-as-claude-code-skills=true/false` - Convert standards to skills

### Typical Workflow

**1. Product Planning**

```bash
/plan-product
```

Agent gathers product vision and creates:
- `agent-os/product/mission.md` - Vision and strategy
- `agent-os/product/roadmap.md` - Phased development plan
- `agent-os/product/tech-stack.md` - Technical decisions

**2. Shape Specification**

```bash
/shape-spec
```

Agent conducts research interview, gathers requirements, and creates:
- `agent-os/specs/YYYY-MM-DD-feature/planning/requirements.md`
- `agent-os/specs/YYYY-MM-DD-feature/planning/visuals/` (if provided)

**3. Write Specification**

```bash
/write-spec
```

Agent generates technical specification:
- `agent-os/specs/YYYY-MM-DD-feature/spec.md`

Includes:
- Goals and user stories
- Specific requirements
- Visual design references
- Existing code to leverage
- Out-of-scope items

**4. Create Tasks**

```bash
/create-tasks
```

Agent breaks spec into task groups:
- `agent-os/specs/YYYY-MM-DD-feature/tasks.md`

Organized by specialization with acceptance criteria and test requirements.

**5. Implement Tasks**

```bash
/implement-tasks
```

Agent executes implementation following specs and standards.

**6. Orchestrate Tasks (Multi-Step)**

```bash
/orchestrate-tasks
```

Agent coordinates complex implementations:
- Creates `orchestration.yml` with task group assignments
- Delegates to subagents (if enabled) or generates numbered prompts
- Tracks progress through task checkboxes

### Integration with AI Systems

**Claude Code (Primary)**

Multi-agent configuration leverages Claude Code's subagent system:

```yaml
# .claude/agents/agent-os/product-planner.yml
name: product-planner
color: cyan
model: $inherit
tools:
  - Write
  - Read
  - Bash
  - WebFetch
```

Commands delegate to subagents:

```markdown
Use the product-planner subagent to create documentation:
- Gather product details
- Generate mission.md, roadmap.md, tech-stack.md
```

**Single-Agent Tools (Cursor, Windsurf, etc.)**

Sequential numbered prompts guide the agent:

```
1-product-concept.md → 2-create-mission.md → 3-create-roadmap.md → 4-create-tech-stack.md
```

Each file contains complete instructions for that step.

**Standards Delivery**

Two modes:

1. **Embedded (default):** Standards injected directly into command prompts
2. **Claude Code Skills:** Standards converted to discoverable skills

### Update & Maintenance

```bash
# Update project installation
~/agent-os/scripts/project-update.sh

# Options
~/agent-os/scripts/project-update.sh \
  --overwrite-agents \
  --overwrite-commands \
  --dry-run
```

Updates preserve:
- User-created specs in `agent-os/specs/`
- Product documentation in `agent-os/product/`
- Customized standards (if not overwritten)

Updates replace:
- Command definitions
- Agent definitions
- Workflow templates
- Default standards (if flagged)

---

## 6. Unique Differentiators

### What Makes AgentOS Special

**1. Not a Framework, but an Operating System**

Unlike traditional agent frameworks (AutoGPT, CrewAI, LangGraph), AgentOS doesn't provide runtime orchestration. Instead, it's a **development methodology** packaged as installable workflows and standards.

**2. Spec-Driven, Not Prompt-Driven**

Instead of:
```
"Build a user authentication system"
```

AgentOS produces:
```
agent-os/specs/2025-11-20-user-auth/
  ├── planning/requirements.md  (15 specific requirements)
  ├── spec.md                   (detailed technical spec)
  └── tasks.md                  (organized task groups)
```

**3. Dual Architecture**

Uniquely supports both:
- **Multi-agent orchestration** (Claude Code subagents)
- **Single-agent sequential workflows** (numbered prompts)

Same source files compile to both modes.

**4. Standards as First-Class Citizens**

Most frameworks treat coding standards as documentation. AgentOS:
- Embeds standards directly in agent prompts
- Enforces alignment through verification steps
- Can convert standards to discoverable Claude Code Skills

**5. Template Compilation System**

Sophisticated build system that:
- Processes conditional logic (IF/UNLESS blocks)
- Expands workflow references recursively
- Handles multiline template substitution safely
- Generates multiple output formats from single source

**6. Profile-Based Inheritance**

Configuration sharing through hierarchical profiles:
- Base profile with sensible defaults
- Team profiles with shared conventions
- Project-specific overrides

**7. Pragmatic Testing Philosophy**

Explicitly rejects comprehensive test coverage during development:
- 2-8 tests per implementation task group
- Maximum 10 additional integration tests
- Focus on core user flows only
- Defer edge cases to dedicated testing phases

This contrasts with TDD-focused frameworks.

**8. Built for Real Codebases**

Emphasizes:
- Reusing existing components before creating new ones
- Searching for similar patterns in the codebase
- Respecting established conventions
- Incremental adoption in legacy systems

**9. Tool-Agnostic**

Works with any AI coding assistant:
- Claude Code (with advanced subagent features)
- Cursor
- Windsurf
- Gemini Code Assist
- Any tool that supports markdown commands

**10. Version-Controlled Knowledge**

Standards, workflows, and profiles stored in git repositories enable:
- Team-wide consistency
- Change tracking
- Collaborative improvement
- Rollback capabilities

---

## 7. Best Practices

### Recommended Usage Patterns

**1. Start with Product Planning**

Always begin with `/plan-product` to establish:
- Clear vision and mission
- Phased roadmap
- Technical stack decisions

This provides essential context for all subsequent work.

**2. Customize Standards Early**

Before first project installation, edit `~/agent-os/profiles/default/standards/` to match your:
- Technology choices
- Naming conventions
- Architecture patterns
- Error handling approaches

Generic standards produce generic code.

**3. Use Visual Assets**

Include mockups in `planning/visuals/` during `/shape-spec`:
- Reduces ambiguity
- Enables visual analysis
- Prevents UI rework
- Guides component reuse

**4. Verify Before Implementation**

Review generated specs thoroughly:
- Check alignment with requirements
- Validate reuse opportunities
- Confirm test limits (2-8 per group)
- Ensure nothing is over-engineered

Implementation follows specs exactly—validate early.

**5. Search for Reusability**

Both `/write-spec` and `/create-tasks` emphasize:
- Finding similar existing features
- Identifying reusable components
- Leveraging established patterns

This prevents code duplication and maintains consistency.

**6. Keep Tests Minimal**

Follow AgentOS testing philosophy:
- 2-8 focused tests per implementation task group
- Test behavior, not implementation
- Core user flows only during development
- Fast execution (milliseconds)

Resist the urge for comprehensive coverage during feature work.

**7. Use Orchestration for Complex Features**

For multi-step implementations:
- Use `/orchestrate-tasks` instead of direct implementation
- Assign subagents (multi-agent) or follow numbered prompts (single-agent)
- Track progress through task checkboxes
- Update orchestration.yml as work progresses

**8. Maintain Spec-Code Alignment**

As requirements change:
- Update specs first
- Regenerate tasks if needed
- Adjust implementation to match
- Don't let code drift from specifications

**9. Profile Management**

For teams:
- Create custom profiles inheriting from default
- Store in git repositories
- Share across projects: `--profile=team-profile`
- Version control standard changes

**10. Update Regularly**

```bash
# Update base installation
~/agent-os/scripts/base-install.sh

# Update project installations
~/agent-os/scripts/project-update.sh
```

Stay current with workflow improvements and bug fixes.

### Anti-Patterns to Avoid

**1. Skipping Product Planning**

Jumping directly to feature specs without mission/roadmap context produces disconnected features.

**2. Generic Standards**

Leaving placeholder standards (e.g., `[Rails/Django/FastAPI]`) causes agents to make arbitrary tech stack decisions.

**3. Over-Testing During Development**

Writing comprehensive test suites during feature implementation:
- Slows development velocity
- Wastes tokens on edge cases
- Violates AgentOS testing philosophy

**4. Ignoring Existing Code**

Not searching for reusable components before implementation:
- Creates duplicate functionality
- Violates DRY principles
- Increases maintenance burden

**5. Ad-Hoc Prompting**

Using AgentOS but still prompting agents directly:
- Bypasses standards enforcement
- Fragments development workflow
- Loses spec-code traceability

**6. Premature Implementation**

Starting implementation before:
- Requirements are fully gathered
- Specs are written and verified
- Tasks are broken down and organized

**7. Single-Agent for Complex Features**

Using single-agent mode for large, multi-phase implementations:
- Exhausts context windows
- Reduces parallel work opportunities
- Misses subagent specialization benefits

**8. Mixing Approaches**

Inconsistently using:
- Some features with specs, some without
- Some with standards, some ad-hoc
- Some with orchestration, some direct implementation

Consistency is key for maintainability.

---

## Architectural Evolution

### Version History Insights

**v1.0 (Initial Release)**
- Single-agent framework
- Basic command structure
- Manual spec writing

**v1.1 (Context Optimization)**
- Conditional document loading
- 60-80% context reduction
- Prevents duplicate loading

**v1.2 (Multi-Agent Introduction)**
- Specialized Claude Code subagents
- Focused task delegation
- Improved efficiency

**v1.3 (Pre-flight Checks)**
- Centralized agent detection
- Configuration validation
- Better error messages

**v1.4 (Team Collaboration)**
- Per-project standards customization
- Git-based sharing
- Profile inheritance

**v2.0 (Dual-Mode Architecture)**
- Support for both multi-agent and single-agent
- Flexible boolean configuration
- Eliminated rigid mode structures

**v2.1 (Modular Phases)**
- Expanded from 4 to 6 phases
- Pick-and-choose workflow components
- Claude Code Skills support
- Removed unnecessary verifiers

### Design Decisions

**Eliminated in v2.1:**
- Specialized verifiers (consolidated into workflows)
- Roles system (too complex)
- Documentation bloat (streamlined)
- Mandatory workflows (now optional)

**Emphasis on:**
- Speed and token usage optimization
- Flexible workflow composition
- Standards discoverability (Skills)
- Minimal required structure

---

## Technical Implementation Details

### Template Syntax

**Workflow Embedding:**
```markdown
{{workflows/planning/gather-product-info}}
```

**Standards Injection:**
```markdown
{{standards/*}}
{{standards/global/tech-stack}}
```

**Conditional Blocks:**
```markdown
{{#IF use_claude_code_subagents}}
Use the product-planner subagent...
{{#UNLESS use_claude_code_subagents}}
Follow these steps sequentially...
{{#ENDIF}}
```

**Phase Tags:**
```markdown
{{#PHASE 1}}
First step instructions...
{{#PHASE 2}}
Second step instructions...
```

### Configuration Schema

```yaml
# config.yml
version: "2.1.1"
claude_code_commands: true           # Install .claude/commands/agent-os/
use_claude_code_subagents: true      # Install .claude/agents/agent-os/
agent_os_commands: false             # Install agent-os/commands/
standards_as_claude_code_skills: false  # Convert to .claude/skills/
default_profile: "default"           # Profile to use
```

### Key Functions (from common-functions.sh)

- `get_profile_file()` - Traverse inheritance chain for file resolution
- `process_conditionals()` - Handle IF/UNLESS blocks with stack-based tracking
- `compile_agent()` - Orchestrate all template transformations
- `get_yaml_array()` / `get_yaml_value()` - Robust YAML parsing

---

## Comparison to Other Frameworks

| Aspect | AgentOS | AutoGPT | CrewAI | LangGraph |
|--------|---------|---------|---------|-----------|
| **Type** | Development methodology | Autonomous agent | Multi-agent framework | Workflow orchestration |
| **Runtime** | None (compile-time only) | Python runtime | Python runtime | Python runtime |
| **Target** | AI coding assistants | General automation | Role-based teams | Complex workflows |
| **Standards** | First-class, enforceable | Not emphasized | Not emphasized | Not emphasized |
| **Specs** | Core artifact | Optional | Optional | Optional |
| **Installation** | Per-project + base | pip/npm package | pip package | pip package |
| **Language** | Shell scripts + markdown | Python | Python | Python |
| **Output** | Commands/agents/skills | Task execution | Task execution | Graph execution |

AgentOS is unique in being a **compile-time development methodology** rather than a **runtime orchestration framework**.

---

## Strengths & Limitations

### Strengths

1. **Simplicity:** No runtime dependencies, just shell scripts and markdown
2. **Tool-Agnostic:** Works with any AI coding assistant
3. **Standards-Driven:** Enforces consistency through embedded conventions
4. **Flexible Architecture:** Supports both multi-agent and single-agent modes
5. **Version-Controlled:** All configuration in git repositories
6. **Pragmatic:** Emphasizes speed and practical outcomes
7. **Battle-Tested:** Used in production by Builder Methods

### Limitations

1. **Shell-Based:** Requires bash environment, less portable to Windows
2. **No Runtime Orchestration:** Can't dynamically adjust workflows
3. **Manual Updates:** Requires running update scripts
4. **Claude Code Bias:** Advanced features require Claude Code
5. **Limited Customization:** Template system has learning curve
6. **No Metrics:** No built-in tracking of workflow effectiveness

---

## Conclusion

AgentOS represents a fundamentally different approach to AI-assisted development. Rather than providing runtime orchestration like traditional agent frameworks, it offers a **development operating system**—a structured methodology for spec-driven development with AI coding agents.

Its core insight: AI agents need **context, constraints, and process** more than they need autonomy. By encoding team standards, establishing clear workflows, and producing specifications before code, AgentOS transforms the AI coding experience from iterative prompting to structured, repeatable development.

The framework's dual-mode architecture (multi-agent and single-agent) makes it uniquely flexible, while its profile-based inheritance enables team-wide standardization. Its emphasis on pragmatic testing, code reuse, and token efficiency reflects real-world software development priorities.

AgentOS is best suited for:
- Teams standardizing AI-assisted development practices
- Projects requiring consistent coding standards
- Feature development following spec-driven methodologies
- Organizations wanting version-controlled development knowledge

It's less suitable for:
- Ad-hoc exploratory coding
- Dynamic runtime agent orchestration
- Platform-specific automation (requires bash)
- Projects without defined standards or processes

As AI coding assistants become ubiquitous, frameworks like AgentOS that provide **structure and standards** rather than just **autonomy** will likely become increasingly valuable for professional software development teams.

---

## Resources

- **GitHub Repository:** https://github.com/buildermethods/agent-os
- **Documentation:** https://buildermethods.com/agent-os
- **Creator:** Brian Casel (Builder Methods)
- **Community:** Newsletter and YouTube content available
- **License:** MIT (Open Source)
- **Latest Version:** v2.1.1 (October 28, 2025)

---

**Research Conducted:** November 20, 2025
**Researcher:** Claude (Anthropic)
**Repository Analyzed:** buildermethods/agent-os (main branch)
