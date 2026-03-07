# Graphiti Adaptation for Cryptocurrency Analytics

## Overview

This document adapts temporal knowledge graph patterns from the Graphiti framework for cryptocurrency and DeFi analytics. The patterns are derived from the gaeilge research on bilingual document processing and knowledge graph construction.

## Temporal Knowledge Graph Design

### Bi-Temporal Model for DeFi

Graphiti's bi-temporal design is ideal for tracking DeFi protocol evolution:

1. **Transaction Time**: When data was recorded in our system
2. **Valid Time**: When the state was valid on-chain (block timestamp)

```
┌─────────────────────────────────────────────────────────────┐
│                    Temporal Graph Layer                      │
├─────────────────────────────────────────────────────────────┤
│  Entity: Token (USDe)                                       │
│  ├── valid_from: block 18000000                             │
│  ├── valid_to: current                                       │
│  ├── transaction_time: 2024-01-15T00:00:00Z                 │
│  └── properties:                                             │
│      ├── total_supply: 2,500,000,000                        │
│      ├── holders: 45,000                                     │
│      └── price_usd: 0.9995                                   │
└─────────────────────────────────────────────────────────────┘
```

### Entity Types for Cryptocurrency Domain

Adapted from educational domain entity patterns:

| Entity Type | Properties | Temporal Tracking |
|-------------|------------|-------------------|
| **Token** | symbol, name, decimals, contract_address, total_supply | Supply changes, price history |
| **Exchange** | name, type (DEX/CEX), supported_chains | Listing/delisting events |
| **LiquidityPool** | token_pair, tvl, fee_tier, address | TVL changes, fee updates |
| **Wallet** | address, label, first_seen, last_active | Balance snapshots |
| **Protocol** | name, tvl, governance_token, chains | TVL, governance changes |
| **Transaction** | hash, block, from, to, value, gas | Immutable (created once) |

### Relationship Types

Derived from bilingual alignment patterns:

```cypher
// Core asset relationships
(token:Token)-[:DEPLOYED_ON]->(chain:Blockchain)
(token:Token)-[:TRADES_ON]->(exchange:Exchange)
(token:Token)-[:GOVERNANCE_FOR]->(protocol:Protocol)
(token:Token)-[:WRAPPED_AS]->(wrapped:Token)
(token:Token)-[:FORK_OF]->(parent:Token)

// Liquidity relationships
(token:Token)-[:IN_POOL]->(pool:LiquidityPool)
(pool:LiquidityPool)-[:HOSTED_ON]->(exchange:Exchange)

// Wallet relationships
(wallet:Wallet)-[:HOLDS {amount: decimal, timestamp: datetime}]->(token:Token)
(wallet:Wallet)-[:INTERACTS_WITH]->(contract:SmartContract)

// Protocol relationships
(protocol:Protocol)-[:INTEGRATES]->(other:Protocol)
(protocol:Protocol)-[:AUDITED_BY]->(auditor:Entity)
```

## Integration with Existing Infrastructure

### FalkorDB + Graphiti (Dynamic/Temporal)

For real-time agent memory and temporal queries:

```python
from graphiti_core import Graphiti
from graphiti_core.llm_client import LLMClient

# Initialize with FalkorDB backend
graphiti = Graphiti(
    uri="bolt://localhost:6379",
    database="crypto_memory",
    llm_client=LLMClient(model="gpt-4o-mini")
)

# Add temporal episode (market event)
await graphiti.add_episode(
    name="ETH price spike",
    episode_body="ETH surged 15% following ETF approval news",
    source="market_data",
    source_description="Real-time market events",
    reference_time=datetime.now()
)

# Query temporal relationships
results = await graphiti.search(
    query="What happened to ETH price after ETF news?",
    num_results=10,
    center_node_uuid=eth_token_uuid
)
```

### Memgraph + Cognee (Static Knowledge)

For persistent protocol knowledge and document insights:

```python
from cognee import cognee

# Configure Memgraph backend
cognee.config.set_graph_database(
    type="memgraph",
    host="localhost",
    port=7687
)

# Add protocol documentation
await cognee.add(whitepaper_content, dataset_name="ethena_docs")
await cognee.cognify()

# Query with graph completion
results = await cognee.search(
    query_text="What are the risks of USDe depegging?",
    query_type="GRAPH_COMPLETION"
)
```

## Document Processing Pipeline

### CocoIndex Flow for Crypto Documents

Adapted from bilingual scraper patterns:

```python
import cocoindex

@cocoindex.flow_def(name="CryptoDocumentFlow")
def crypto_document_flow(flow_builder, data_scope):
    # Source: PDF whitepapers and audits
    data_scope["docs"] = flow_builder.add_source(
        cocoindex.sources.LocalFile(
            path="./documents/whitepapers/*.pdf"
        )
    )

    # Extract text with LaTeX preservation (for math/formulas)
    data_scope["text"] = data_scope["docs"].transform(
        cocoindex.functions.PdfToMarkdown(
            preserve_math=True,
            extract_tables=True
        )
    )

    # LLM-based structured extraction
    data_scope["structured"] = data_scope["text"].transform(
        cocoindex.functions.ExtractByLlm(
            output_type=CryptoProjectMetadata,
            instruction="""Extract:
            - Token properties (name, symbol, supply)
            - Governance structure
            - Risk factors
            - Protocol mechanisms
            - Audit findings (if audit document)
            """
        )
    )

    # Build knowledge graph relationships
    data_scope["entities"] = data_scope["structured"].transform(
        cocoindex.functions.ExtractEntities(
            entity_types=["Token", "Protocol", "Risk", "Mechanism"]
        )
    )

    # Export to Neo4j/Memgraph
    flow_builder.add_export(
        cocoindex.exports.Neo4jGraph(
            uri="bolt://localhost:7687",
            nodes_from="entities",
            relationships_from="structured.relationships"
        )
    )
```

### BAML Schema for Extraction

```baml
class CryptoProjectMetadata {
  project_name string
  token Token?
  governance Governance?
  risks Risk[]
  mechanisms Mechanism[]
  audit_findings AuditFinding[]
  provenance_url string
}

class Token {
  symbol string
  name string
  decimals int
  total_supply string?
  contract_address string?
  blockchain "Ethereum" | "Solana" | "Polygon" | "Arbitrum" | "Base"
}

class Risk {
  category "smart_contract" | "market" | "regulatory" | "custody" | "oracle" | "governance"
  severity "critical" | "high" | "medium" | "low"
  description string
  mitigation string?
}

class AuditFinding {
  auditor string
  severity "critical" | "high" | "medium" | "low" | "informational"
  title string
  description string
  status "resolved" | "acknowledged" | "disputed" | "open"
}
```

## Quality Scoring Framework

Adapted from bilingual alignment quality metrics:

```python
class CryptoDocumentQuality:
    """Quality scoring for crypto documents"""

    def score(self, document: dict) -> float:
        scores = []

        # Technical depth (25%)
        scores.append(self._technical_depth(document) * 0.25)

        # Tokenomics clarity (20%)
        scores.append(self._tokenomics_clarity(document) * 0.20)

        # Governance definition (25%)
        scores.append(self._governance_clarity(document) * 0.25)

        # Risk disclosure (15%)
        scores.append(self._risk_disclosure(document) * 0.15)

        # Update recency (15%)
        scores.append(self._recency_score(document) * 0.15)

        return sum(scores)

    def _technical_depth(self, doc) -> float:
        """Measure technical detail level"""
        indicators = [
            "smart contract" in doc.get("text", "").lower(),
            len(doc.get("mechanisms", [])) > 0,
            doc.get("code_snippets", 0) > 0,
            doc.get("math_formulas", 0) > 0
        ]
        return sum(indicators) / len(indicators)
```

## Graph Query Patterns

### Cypher Queries for Crypto Analytics

```cypher
// Find tokens with highest exchange coverage
MATCH (t:Token)-[:TRADES_ON]->(e:Exchange)
WHERE e.daily_volume > 1000000
RETURN t.symbol, count(e) as exchange_count, sum(e.daily_volume) as total_volume
ORDER BY total_volume DESC
LIMIT 20;

// Find related assets through LP positions
MATCH (t1:Token)-[:IN_POOL]-(pool:LiquidityPool)-[:IN_POOL]-(t2:Token)
WHERE t1.symbol = 'USDe'
RETURN t1.symbol, t2.symbol, pool.tvl, pool.fee_tier
ORDER BY pool.tvl DESC;

// Track protocol TVL changes over time
MATCH (p:Protocol {name: 'Ethena'})-[r:HAS_TVL]->(snapshot:TVLSnapshot)
WHERE snapshot.timestamp > datetime() - duration('P30D')
RETURN snapshot.timestamp, snapshot.tvl_usd
ORDER BY snapshot.timestamp;

// Find tokens in same ecosystem (transitive)
MATCH path = (t:Token)-[:GOVERNANCE_FOR|PART_OF*1..3]->(protocol:Protocol)
WHERE t.symbol = 'ENA'
RETURN path;
```

## Event-Driven Updates

### Sensor Pattern for New Data

```python
from dagster import sensor, RunRequest

@sensor(job=crypto_graph_update_job)
def blockchain_event_sensor(context):
    """Detect new on-chain events and trigger graph updates"""

    # Check for new blocks with relevant transactions
    new_events = fetch_new_events(
        contracts=["0x...ethena", "0x...aave"],
        since=context.cursor
    )

    if new_events:
        yield RunRequest(
            run_key=f"events-{new_events[-1].block}",
            run_config={
                "ops": {
                    "update_knowledge_graph": {
                        "config": {
                            "events": [e.to_dict() for e in new_events]
                        }
                    }
                }
            }
        )
        context.update_cursor(str(new_events[-1].block))
```

## References

- Gaeilge research: `/data/flows/gaeilge/research/organized/02-celtic-data-acquisition/bilingual-scraper-implementation.md`
- Graphiti documentation: Temporal knowledge graph patterns
- CocoIndex: Incremental data transformation
- Cognee: ECL pipeline for knowledge graphs
