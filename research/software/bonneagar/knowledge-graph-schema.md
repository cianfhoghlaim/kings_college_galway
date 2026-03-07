# Cryptocurrency Knowledge Graph Schema

## Overview

This document defines the entity and relationship ontology for the crypto analytics knowledge graph. The schema supports both static knowledge (protocol documentation, audits) and dynamic/temporal data (prices, TVL, transactions).

## Graph Architecture

### Dual-Engine Design

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│         (Agents, APIs, Frontend, Analytics)                  │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┴───────────────┐
          │                               │
          ▼                               ▼
┌─────────────────────┐     ┌─────────────────────────────────┐
│  FalkorDB + Graphiti │     │  Memgraph + Cognee              │
│  (Temporal/Dynamic)  │     │  (Static/Analytical)            │
├─────────────────────┤     ├─────────────────────────────────┤
│ • Agent memory       │     │ • Protocol knowledge            │
│ • Session context    │     │ • Document insights             │
│ • Real-time events   │     │ • MAGE graph algorithms         │
│ • Price/TVL history  │     │ • PageRank, community detection │
└─────────────────────┘     └─────────────────────────────────┘
          │                               │
          └───────────────┬───────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │  LanceDB (Vectors)    │
              │  Semantic embeddings  │
              │  for hybrid search    │
              └───────────────────────┘
```

## Entity Definitions

### Core Entities

#### Token
```yaml
Token:
  properties:
    symbol: string (required)
    name: string (required)
    contract_address: string
    blockchain: enum [Ethereum, Solana, Polygon, Arbitrum, Base, Bitcoin]
    decimals: integer
    total_supply: decimal
    circulating_supply: decimal
    launch_date: datetime
    token_type: enum [native, erc20, bep20, spl, governance, stablecoin, wrapped]
    is_stablecoin: boolean
    peg_target: decimal  # for stablecoins

  temporal_properties:
    price_usd: decimal
    market_cap: decimal
    holder_count: integer
    daily_volume: decimal

  indexes:
    - symbol (unique per blockchain)
    - contract_address (unique)
```

#### Protocol
```yaml
Protocol:
  properties:
    name: string (required)
    category: enum [defi, nft, gaming, infrastructure, bridge, oracle, lending, dex, derivatives, stablecoin]
    governance_token: Token (relationship)
    chains: Blockchain[] (relationship)
    launch_date: datetime
    website: url
    documentation_url: url
    github_url: url

  temporal_properties:
    tvl_usd: decimal
    daily_active_users: integer
    daily_transactions: integer
    revenue_24h: decimal

  computed:
    risk_score: float  # aggregated from audits and incidents
```

#### Exchange
```yaml
Exchange:
  properties:
    name: string (required)
    type: enum [cex, dex, aggregator, otc]
    chains: Blockchain[] (for DEXs)
    website: url
    api_url: url

  temporal_properties:
    daily_volume_usd: decimal
    open_interest_usd: decimal  # for derivatives
    listed_pairs: integer

  regulatory:
    jurisdictions: string[]
    licenses: string[]
```

#### LiquidityPool
```yaml
LiquidityPool:
  properties:
    address: string (required)
    protocol: Protocol (relationship)
    tokens: Token[] (relationship, 2-8 tokens)
    fee_tier: decimal
    pool_type: enum [constant_product, stable, concentrated, weighted]

  temporal_properties:
    tvl_usd: decimal
    volume_24h: decimal
    fees_24h: decimal
    apy: decimal
    impermanent_loss_30d: decimal
```

#### Wallet
```yaml
Wallet:
  properties:
    address: string (required)
    blockchain: Blockchain (relationship)
    label: string  # "Binance Hot Wallet", "whale", etc.
    entity: Entity  # known entity association
    first_seen: datetime

  temporal_properties:
    total_value_usd: decimal
    transaction_count: integer
    last_active: datetime

  tags:
    - exchange
    - whale
    - smart_money
    - team
    - treasury
```

#### Document
```yaml
Document:
  properties:
    title: string (required)
    doc_type: enum [whitepaper, audit, research, governance_proposal, risk_report]
    url: url
    published_date: datetime
    author: string
    protocol: Protocol (relationship)

  content:
    sections: Section[]
    extracted_entities: Entity[]
    risk_factors: Risk[]

  quality:
    technical_depth: float
    governance_clarity: float
    risk_disclosure: float
    recency_score: float
```

### Supporting Entities

#### Blockchain
```yaml
Blockchain:
  properties:
    name: string (required)
    chain_id: integer
    consensus: enum [pow, pos, dpos, poa, other]
    native_token: Token (relationship)

  temporal_properties:
    tps: decimal
    block_time: decimal
    gas_price_gwei: decimal
```

#### AuditReport
```yaml
AuditReport:
  properties:
    auditor: string (required)
    protocol: Protocol (relationship)
    report_date: datetime
    scope: string[]

  findings:
    critical: integer
    high: integer
    medium: integer
    low: integer
    informational: integer

  status:
    findings_addressed: float  # percentage
```

## Relationship Types

### Token Relationships

```cypher
// Deployment
(t:Token)-[:DEPLOYED_ON {deploy_block: int, deploy_tx: string}]->(b:Blockchain)

// Trading
(t:Token)-[:TRADES_ON {listing_date: datetime}]->(e:Exchange)

// Governance
(t:Token)-[:GOVERNANCE_FOR]->(p:Protocol)

// Derivatives
(t:Token)-[:WRAPPED_AS {wrapper_contract: string}]->(w:Token)
(t:Token)-[:SYNTHETIC_OF]->(underlying:Token)
(t:Token)-[:FORK_OF]->(parent:Token)

// Yield
(t:Token)-[:STAKING_REWARDS]->(r:Token)
```

### Protocol Relationships

```cypher
// Integration
(p1:Protocol)-[:INTEGRATES {integration_type: string}]->(p2:Protocol)

// Security
(p:Protocol)-[:AUDITED_BY {report_date: datetime}]->(a:Auditor)

// Governance
(p:Protocol)-[:GOVERNED_BY]->(g:Governance)

// Bridges
(p:Protocol)-[:BRIDGES {direction: string}]->(chain:Blockchain)
```

### Liquidity Relationships

```cypher
// Pool composition
(t:Token)-[:IN_POOL {weight: float}]->(pool:LiquidityPool)

// Pool hosting
(pool:LiquidityPool)-[:HOSTED_ON]->(dex:Exchange)

// Yield farming
(pool:LiquidityPool)-[:REWARDS_WITH]->(t:Token)
```

### Wallet Relationships

```cypher
// Holdings (temporal)
(w:Wallet)-[:HOLDS {amount: decimal, timestamp: datetime}]->(t:Token)

// Transactions
(w1:Wallet)-[:SENT_TO {tx_hash: string, amount: decimal, timestamp: datetime}]->(w2:Wallet)

// Smart contract interaction
(w:Wallet)-[:INTERACTS_WITH {method: string, count: int}]->(c:SmartContract)

// Protocol usage
(w:Wallet)-[:USES {first_use: datetime, last_use: datetime}]->(p:Protocol)
```

### Document Relationships

```cypher
// Document associations
(d:Document)-[:DESCRIBES]->(p:Protocol)
(d:Document)-[:AUTHORED_BY]->(e:Entity)
(d:Document)-[:REFERENCES]->(d2:Document)

// Extracted knowledge
(d:Document)-[:CONTAINS_ENTITY]->(entity:Entity)
(d:Document)-[:IDENTIFIES_RISK]->(r:Risk)
```

## Temporal Modeling

### Snapshot Pattern for Time-Series Data

```cypher
// TVL snapshots
CREATE (p:Protocol {name: 'Ethena'})
CREATE (s1:TVLSnapshot {timestamp: datetime('2024-01-01'), tvl_usd: 1000000000})
CREATE (s2:TVLSnapshot {timestamp: datetime('2024-01-02'), tvl_usd: 1050000000})
CREATE (p)-[:HAS_TVL]->(s1)
CREATE (p)-[:HAS_TVL]->(s2)
CREATE (s1)-[:NEXT]->(s2)
```

### Event Sourcing for Transactions

```cypher
// Transaction events
CREATE (tx:Transaction {
  hash: '0x...',
  block_number: 19000000,
  timestamp: datetime(),
  type: 'swap',
  protocol: 'Uniswap',
  value_usd: 50000
})
CREATE (from:Wallet {address: '0x...'})
CREATE (to:Wallet {address: '0x...'})
CREATE (from)-[:EXECUTED]->(tx)
CREATE (tx)-[:TRANSFERRED_TO]->(to)
```

## Index Recommendations

### Memgraph Indexes

```cypher
// Token lookups
CREATE INDEX ON :Token(symbol);
CREATE INDEX ON :Token(contract_address);

// Protocol lookups
CREATE INDEX ON :Protocol(name);

// Temporal queries
CREATE INDEX ON :TVLSnapshot(timestamp);
CREATE INDEX ON :Transaction(timestamp);
CREATE INDEX ON :Transaction(block_number);

// Wallet analytics
CREATE INDEX ON :Wallet(address);
CREATE INDEX ON :Wallet(label);
```

### FalkorDB Indexes (via Graphiti)

```python
# Graphiti handles temporal indexing automatically
# Additional indexes for episode queries:
graphiti.create_index("episode_timestamp")
graphiti.create_index("entity_type")
graphiti.create_index("source")
```

## Query Patterns

### Find High-Value DeFi Opportunities

```cypher
MATCH (pool:LiquidityPool)-[:IN_POOL]-(t:Token)
WHERE pool.apy > 10 AND pool.tvl_usd > 1000000
MATCH (pool)-[:HOSTED_ON]->(dex:Exchange)
MATCH (dex)-[:AUDITED_BY]->(auditor)
RETURN pool, t, dex, auditor
ORDER BY pool.apy DESC
LIMIT 10
```

### Track Whale Movements

```cypher
MATCH (w:Wallet {label: 'whale'})-[h:HOLDS]->(t:Token)
WHERE h.timestamp > datetime() - duration('P7D')
WITH w, t, h
ORDER BY h.timestamp
RETURN w.address, t.symbol, collect(h.amount) as balance_history
```

### Protocol Risk Assessment

```cypher
MATCH (p:Protocol)
OPTIONAL MATCH (p)<-[:AUDITED_BY]-(a:AuditReport)
OPTIONAL MATCH (p)<-[:DESCRIBES]-(d:Document {doc_type: 'risk_report'})
OPTIONAL MATCH (p)-[:HAS_TVL]->(tvl:TVLSnapshot)
WHERE tvl.timestamp > datetime() - duration('P30D')
RETURN p.name,
       count(DISTINCT a) as audit_count,
       avg(a.critical + a.high) as avg_severe_findings,
       avg(tvl.tvl_usd) as avg_tvl,
       collect(DISTINCT d.title) as risk_reports
ORDER BY audit_count DESC
```

## Data Sources Mapping

| Entity | Primary Source | Secondary Sources |
|--------|---------------|-------------------|
| Token | CoinGecko API | DeFiLlama, On-chain |
| Protocol | DeFiLlama | Protocol docs, The Graph |
| Exchange | CoinGecko, Direct APIs | DeFiLlama |
| LiquidityPool | The Graph subgraphs | Direct contract queries |
| Wallet | On-chain data | Labeled datasets |
| Document | Crawl4AI, Direct fetch | Research aggregators |
| AuditReport | Auditor websites | Protocol documentation |

## Integration with crypto_sources.json

The `crypto_sources.json` file defines ingestion for these entities. Key mappings:

```json
{
  "source_to_entity": {
    "coingecko_*": "Token",
    "defillama_protocol_*": "Protocol",
    "defillama_yields_*": "LiquidityPool",
    "aave_v3_*_subgraph": "LiquidityPool",
    "pendle_*_subgraph": "LiquidityPool",
    "ethena_docs_*": "Document",
    "ethena_audits_*": "AuditReport"
  }
}
```
