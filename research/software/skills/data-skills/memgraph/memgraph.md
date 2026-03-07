# Memgraph Development Skill

You are an expert Memgraph developer with deep knowledge of graph database design, Cypher query language, and real-time analytics. Use this skill when working with Memgraph graph databases, designing graph data models, writing Cypher queries, or implementing graph-based solutions.

## When to Use This Skill

Activate this skill when:
- Working with Memgraph databases
- Writing or optimizing Cypher queries
- Designing graph data models
- Implementing graph algorithms
- Setting up streaming data pipelines
- Troubleshooting graph database performance
- Building knowledge graphs or GraphRAG applications
- Implementing fraud detection, recommendation engines, or social network analysis

## Core Memgraph Knowledge

### What is Memgraph?

Memgraph is a high-performance, in-memory graph database optimized for:
- **Real-time analytics** with sub-millisecond query latency
- **Streaming data processing** with native Kafka/Pulsar/RabbitMQ connectors
- **HTAP workloads** (Hybrid Transactional/Analytical Processing)
- **GraphRAG and AI integration** with native vector search
- **Mission-critical applications** requiring ACID compliance

**Key Performance Metrics:**
- 3-8x faster than Neo4j
- Sub-millisecond latency (1.07ms minimum)
- 132x higher throughput in write-heavy workloads

### Data Model - Labeled Property Graph (LPG)

**Four Core Elements:**
1. **Nodes**: Entities (people, products, events)
2. **Relationships**: Directed edges connecting nodes
3. **Properties**: Key-value pairs on nodes or relationships
4. **Labels**: Node categorizations

**Naming Conventions:**
- Node labels: `CamelCase` (e.g., `User`, `ProductCategory`)
- Relationship types: `UPPER_CASE_WITH_UNDERSCORES` (e.g., `KNOWS`, `BELONGS_TO`)
- Properties/variables: `camelCase` (e.g., `userName`, `createdAt`)

## Data Modeling Best Practices

### Design Principles

When designing a graph model, follow these principles:

1. **Avoid Over-Modeling**
   - Keep it simple - model only what adds value
   - Ask: "Does this node or relationship add real value?"
   - Don't model every detail

2. **Optimize Memory Usage**
   - Memgraph is in-memory - choose efficient data types
   - Use integers instead of strings where applicable
   - Avoid storing large text in frequently accessed properties

3. **Avoid Data Duplication**
   - Use relationships instead of duplicating data
   - Example: Don't add worker info to every order; create a Worker node and link it

4. **Avoid Supernodes**
   - A supernode has 50k+ connections and severely impacts performance
   - Use `ANALYZE GRAPH;` to optimize queries involving supernodes
   - Consider splitting high-degree nodes when possible

5. **Strategic Indexing**
   - Only index frequently queried properties
   - Focus on high-cardinality properties (many unique values)
   - Avoid indexing low-cardinality properties (gender, boolean values)

6. **Think in Graph Terms**
   - Pattern matching ≠ SQL JOINs
   - Focus on relationships and traversals
   - Design for how you'll query the data

### Property vs Relationship Decision

**Store as Property when:**
- The data is rarely queried independently
- The data is unique to that specific entity
- It's descriptive metadata (name, age, timestamp)

**Store as Relationship when:**
- The data is shared across multiple nodes
- You frequently query or traverse based on this data
- It represents a connection or association
- You need to add properties to the connection itself

## Cypher Query Language Patterns

### Basic CRUD Operations

**Create**
```cypher
// Create nodes
CREATE (u:User {name: 'Alice', email: 'alice@example.com', age: 30});

// Create relationships
MATCH (u:User {name: 'Alice'}), (v:User {name: 'Bob'})
CREATE (u)-[:FOLLOWS {since: date()}]->(v);

// Create pattern in one statement
CREATE (u:User {name: 'Alice'})-[:POSTED]->(p:Post {title: 'Hello World', content: 'My first post'});

// Upsert with MERGE
MERGE (u:User {email: 'alice@example.com'})
ON CREATE SET u.created = timestamp(), u.name = 'Alice'
ON MATCH SET u.lastSeen = timestamp()
RETURN u;
```

**Read (MATCH)**
```cypher
// Simple match
MATCH (u:User {name: 'Alice'}) RETURN u;

// Match with relationships
MATCH (u:User {name: 'Alice'})-[:FOLLOWS]->(followed:User)
RETURN followed.name;

// Complex pattern matching
MATCH (user:User)-[r:RATED]->(movie:Movie)<-[:OF_GENRE]-(genre:Genre {name: 'Comedy'})
WHERE r.rating > 3
RETURN movie.title, r.rating
ORDER BY r.rating DESC
LIMIT 10;
```

**Update**
```cypher
// Update properties
MATCH (u:User {name: 'Alice'})
SET u.age = 31, u.updated = timestamp();

// Add/update multiple properties
MATCH (u:User {name: 'Alice'})
SET u += {location: 'New York', verified: true};

// Remove nested properties (v3.6+)
MATCH (u:User {name: 'Alice'})
SET u.nested.property = null;
```

**Delete**
```cypher
// Delete node and its relationships
MATCH (u:User {name: 'Alice'})
DETACH DELETE u;

// Delete only relationships
MATCH (u:User {name: 'Alice'})-[r:FOLLOWS]->()
DELETE r;

// Remove properties
MATCH (u:User {name: 'Alice'})
REMOVE u.age;

// Remove labels
MATCH (n:Person)
REMOVE n:OldLabel;
```

### Path Traversal Algorithms

**Depth-First Search (DFS)** - All paths
```cypher
// Find all paths
MATCH path=(start {id: 0})-[*]->(end {id: 8})
RETURN path;

// Limited path length (2 to 4 hops)
MATCH path=(start {id: 0})-[r*2..4]->(end {id: 8})
RETURN path;

// With inline filtering
MATCH path=(start {id: 0})-[r:ROAD *..5 (r, n | r.active = true AND n.cost < 100)]->(end {id: 8})
RETURN path;
```

**Breadth-First Search (BFS)** - Shortest path
```cypher
// Shortest path (returns one path)
MATCH path=(start {id: 0})-[*BFS]->(end {id: 8})
RETURN path;

// BFS with filtering and length limit
MATCH path=(:City {name: 'London'})-[r:ROAD *BFS ..3 (r, n | r.continent = 'Europe')]->(:City)
RETURN path;
```

**Weighted Shortest Path (WSHORTEST)**
```cypher
// Basic weighted shortest path
MATCH p = (:City {name: "Paris"})
  -[:Road *WSHORTEST (e, v | e.distance) total_weight]->
  (:City {name: "Berlin"})
RETURN nodes(p) AS cities, total_weight;

// With additional filtering
MATCH p = (:City {name: "Paris"})
  -[:Road *WSHORTEST (e, v | e.distance) total_weight (e, v | e.distance <= 200)]->
  (:City {name: "Berlin"})
RETURN nodes(p) AS cities, total_weight;
```

**All Shortest Paths (ALLSHORTEST)**
```cypher
// Find all shortest paths
MATCH path = (a)-[*ALLSHORTEST]->(b)
RETURN path;
```

### Aggregation and Data Processing

```cypher
// Counting
MATCH (n:User) RETURN count(n);
MATCH (n:User) RETURN count(DISTINCT n.location);

// Statistical functions
MATCH (n:User) RETURN sum(n.age), avg(n.age), min(n.age), max(n.age);

// Collect into list
MATCH (n:User) RETURN collect(n.name) AS names;

// Collect into map
MATCH (n:User) RETURN collect(n.name, n.age) AS name_to_age_map;

// Group by with aggregation
MATCH (u:User)-[:POSTED]->(p:Post)
RETURN u.name, count(p) AS post_count
ORDER BY post_count DESC;
```

## Indexing and Performance Optimization

### Creating Indexes

```cypher
// Label index - indexes all nodes with label
CREATE INDEX ON :User;

// Label-property index - most common
CREATE INDEX ON :User(email);

// Composite index - multiple properties
CREATE INDEX ON :User(name, age);

// Edge-type index
CREATE INDEX ON :FOLLOWS;

// View existing indexes
SHOW INDEX INFO;

// Drop index
DROP INDEX ON :User(email);
```

### ANALYZE GRAPH Command

**Critical for Performance:**
```cypher
ANALYZE GRAPH;
```

Run after:
- Creating indexes
- Bulk data insertion
- Before queries with multiple property indexes
- Before queries involving supernodes

This helps Memgraph:
- Calculate node degree statistics
- Optimize MERGE operations on supernodes
- Improve multi-index intersection queries

### Query Performance Analysis

```cypher
// View query plan without execution
EXPLAIN MATCH (n:User)-[:FOLLOWS]->(m:User)
WHERE n.age > 25
RETURN m.name;

// Execute and profile performance
PROFILE MATCH (n:User)-[:FOLLOWS]->(m:User)
WHERE n.age > 25
RETURN m.name;
```

**PROFILE provides:**
- **OPERATOR**: Operator designation
- **ACTUAL HITS**: Number of times operator executed
- **RELATIVE TIME**: Time spent relative to total
- **ABSOLUTE TIME**: Actual wall time

**Optimization Tips:**
- Lower number of operators = faster execution
- Use inline filtering for path traversals instead of WHERE clauses
- Start traversals from specific nodes using indexed properties
- Limit results early in the query pipeline

## Graph Algorithms - MAGE Library

### Centrality Measures

```cypher
// PageRank - find important nodes
CALL pagerank.get(100, 0.85)
YIELD node, rank
SET node.pagerank = rank;

// Get top-ranked nodes
CALL pagerank.get()
YIELD node, rank
RETURN node.name, rank
ORDER BY rank DESC
LIMIT 10;

// Betweenness Centrality - find bridge nodes
CALL betweenness_centrality.get()
YIELD node, betweenness
RETURN node.name, betweenness
ORDER BY betweenness DESC;

// Katz Centrality
CALL katz_centrality.get()
YIELD node, rank;
```

### Community Detection

```cypher
// Louvain method - static community detection
CALL community_detection.get()
YIELD node, community_id
RETURN community_id, collect(node.name) AS members;

// Count communities and sizes
CALL community_detection.get()
YIELD node, community_id
RETURN community_id, count(*) AS size
ORDER BY size DESC;

// Dynamic community detection (for streaming)
CALL community_detection_online.get()
YIELD node, community_id;
```

### Machine Learning Algorithms

```cypher
// Node2Vec - graph embeddings
CALL node2vec.get()
YIELD node, embedding;

// Link prediction
CALL link_prediction.predict()
YIELD node1, node2, probability
WHERE probability > 0.7
RETURN node1, node2, probability;

// Node classification
CALL node_classification.predict()
YIELD node, predicted_class;
```

## Constraints and Data Validation

### Creating Constraints

```cypher
// Existence constraint - property must exist
CREATE CONSTRAINT ON (n:User) ASSERT EXISTS (n.email);

// Uniqueness constraint - property must be unique
CREATE CONSTRAINT ON (n:User) ASSERT n.email IS UNIQUE;

// View all constraints
SHOW CONSTRAINT INFO;

// Drop specific constraint
DROP CONSTRAINT ON (n:User) ASSERT EXISTS (n.email);

// Drop all constraints (v3.6+)
DROP ALL CONSTRAINTS;
```

## Streaming Data Processing

### Setting Up Streams

```cypher
// Kafka stream
CREATE KAFKA STREAM user_events
TOPICS user_activity, user_signup
TRANSFORM event_processor.process_event
BOOTSTRAP_SERVERS 'localhost:9092'
BATCH_INTERVAL 100;

// Pulsar stream
CREATE PULSAR STREAM transactions
TOPICS financial_transactions
TRANSFORM fraud_detector.check_transaction
SERVICE_URL 'pulsar://localhost:6650';

// View streams
SHOW STREAMS;

// Start/stop streams
START STREAM user_events;
STOP STREAM user_events;

// Drop stream
DROP STREAM user_events;
```

### Transformation Modules

Create Python transformation module:

```python
import mgp

@mgp.transformation
def process_event(messages: mgp.Messages) -> mgp.Record(query=str, parameters=mgp.Nullable[mgp.Map]):
    result_queries = []

    for message in messages:
        payload = message.payload().decode('utf-8')
        # Parse payload and create Cypher query
        query = "CREATE (e:Event {data: $data, timestamp: $ts})"
        params = {"data": payload, "ts": message.timestamp()}
        result_queries.append(mgp.Record(query=query, parameters=params))

    return result_queries
```

## Storage Modes and Transaction Management

### Storage Modes

```cypher
// Transactional mode (default) - OLTP workloads
SET DATABASE SETTING 'storage.storage_mode' TO 'IN_MEMORY_TRANSACTIONAL';

// Analytical mode - OLAP workloads (6x faster import, 6x less memory)
SET DATABASE SETTING 'storage.storage_mode' TO 'IN_MEMORY_ANALYTICAL';
```

### Transaction Control

```cypher
// Start transaction
BEGIN;

// Execute queries
CREATE (n:User {name: 'Alice'});
MATCH (n:User {name: 'Alice'}) SET n.age = 30;

// Commit or rollback
COMMIT;
// or
ROLLBACK;
```

### Triggers

```cypher
// Create trigger for new nodes
CREATE TRIGGER new_user_trigger
ON () CREATE
AFTER COMMIT
EXECUTE
  MATCH (n:User)
  WHERE n.created_at IS NULL
  SET n.created_at = timestamp();

// Trigger for updates
CREATE TRIGGER audit_trigger
ON () UPDATE
AFTER COMMIT
EXECUTE
  MATCH (n)
  SET n.last_modified = timestamp();

// View triggers
SHOW TRIGGERS;

// Drop trigger
DROP TRIGGER new_user_trigger;
```

## Common Use Case Patterns

### Social Network Analysis

```cypher
// Find mutual friends
MATCH (me:User {name: 'Alice'})-[:FRIENDS_WITH]-(mutual)-[:FRIENDS_WITH]-(friend:User {name: 'Bob'})
RETURN mutual.name;

// Friend recommendations (friends of friends)
MATCH (me:User {name: 'Alice'})-[:FRIENDS_WITH]-()-[:FRIENDS_WITH]-(recommendation)
WHERE NOT (me)-[:FRIENDS_WITH]-(recommendation) AND me <> recommendation
RETURN DISTINCT recommendation.name, count(*) AS mutual_friends
ORDER BY mutual_friends DESC
LIMIT 10;

// Find influencers in community
CALL pagerank.get()
YIELD node, rank
WHERE node:User
RETURN node.name, rank
ORDER BY rank DESC
LIMIT 20;
```

### Fraud Detection

```cypher
// Find suspicious transaction patterns (velocity check)
MATCH (account:Account)-[t:TRANSACTION]->(other:Account)
WHERE t.timestamp > timestamp() - 3600000 // Last hour
WITH account, count(t) AS tx_count, sum(t.amount) AS total_amount
WHERE tx_count > 50 OR total_amount > 100000
RETURN account.id, tx_count, total_amount;

// Detect circular money flows (possible money laundering)
MATCH path = (a:Account)-[:TRANSACTION *3..5]->(a)
WHERE all(tx IN relationships(path) WHERE tx.amount > 10000)
RETURN path;

// Find accounts sharing identifiers (mule accounts)
MATCH (a1:Account)-[:HAS_PHONE|HAS_ADDRESS|HAS_EMAIL]->(shared)<-[:HAS_PHONE|HAS_ADDRESS|HAS_EMAIL]-(a2:Account)
WHERE a1 <> a2
RETURN a1.id, a2.id, collect(DISTINCT type(shared)) AS shared_identifiers;
```

### Recommendation Engine

```cypher
// Collaborative filtering - users who liked this also liked
MATCH (user:User {id: $userId})-[:RATED {rating: 5}]->(item:Item)
      <-[:RATED {rating: 5}]-(similar:User)-[:RATED {rating: 5}]->(recommendation:Item)
WHERE NOT (user)-[:RATED]->(recommendation)
RETURN recommendation.title, count(*) AS score
ORDER BY score DESC
LIMIT 10;

// Content-based recommendations
MATCH (user:User {id: $userId})-[:RATED]->(item:Item)-[:HAS_TAG]->(tag:Tag)
      <-[:HAS_TAG]-(recommendation:Item)
WHERE NOT (user)-[:RATED]->(recommendation)
RETURN recommendation.title, count(DISTINCT tag) AS common_tags
ORDER BY common_tags DESC
LIMIT 10;
```

### Knowledge Graph Queries

```cypher
// Multi-hop reasoning
MATCH path = (entity:Entity {name: 'Drug A'})-[:RELATED_TO*1..3]-(related:Entity)
WHERE related.type = 'Disease'
RETURN path;

// Find entities mentioned together in documents
MATCH (e1:Entity)<-[:MENTIONS]-(doc:Document)-[:MENTIONS]->(e2:Entity)
WHERE e1.name = 'Albert Einstein' AND e1 <> e2
RETURN e2.name, count(doc) AS co_occurrences
ORDER BY co_occurrences DESC;

// Semantic search with relationships
MATCH (concept:Concept {name: $searchTerm})-[:IS_A|PART_OF|RELATED_TO*..2]-(related:Concept)
RETURN DISTINCT related.name, related.description;
```

## Integration with Python (GQLAlchemy)

### Object Graph Mapper

```python
from gqlalchemy import Memgraph, Node, Relationship, Field

# Connect to Memgraph
db = Memgraph(host='127.0.0.1', port=7687)

# Define node class
class User(Node):
    email: str = Field(unique=True, exists=True, db=db)
    name: str = Field(exists=True, db=db)
    age: int = Field()

# Define relationship class
class Follows(Relationship, type="FOLLOWS"):
    since: str = Field()

# Create instances
alice = User(email="alice@example.com", name="Alice", age=30).save(db)
bob = User(email="bob@example.com", name="Bob", age=25).save(db)

# Create relationship
follows = Follows(
    _start_node_id=alice._id,
    _end_node_id=bob._id,
    since="2024-01-15"
).save(db)

# Query with Cypher
results = db.execute_and_fetch("MATCH (u:User) WHERE u.age > 25 RETURN u")
for result in results:
    print(result['u'].name)
```

### Custom Query Modules

```python
import mgp

@mgp.read_proc
def recommend_friends(ctx: mgp.ProcCtx, user_id: int, limit: int = 10) -> mgp.Record(friend=mgp.Vertex, score=int):
    # Execute Cypher query
    query = """
    MATCH (me:User {id: $user_id})-[:FRIENDS_WITH]-()-[:FRIENDS_WITH]-(recommendation)
    WHERE NOT (me)-[:FRIENDS_WITH]-(recommendation) AND me <> recommendation
    RETURN recommendation, count(*) AS score
    ORDER BY score DESC
    LIMIT $limit
    """

    results = []
    for record in ctx.graph.execute(query, {"user_id": user_id, "limit": limit}):
        results.append(mgp.Record(friend=record['recommendation'], score=record['score']))

    return results
```

## Troubleshooting and Common Issues

### Performance Issues

**Slow Queries:**
1. Run `PROFILE` to identify bottlenecks
2. Check if indexes exist on filtered properties
3. Run `ANALYZE GRAPH;` after bulk operations
4. Use inline filtering instead of WHERE when possible
5. Consider if you're hitting supernodes

**High Memory Usage:**
1. Check storage mode (analytical uses less memory)
2. Review indexing strategy (over-indexing wastes memory)
3. Look for data duplication
4. Consider batch processing for large operations

**Supernode Problems:**
1. Run `ANALYZE GRAPH;` to help optimizer
2. Consider denormalizing high-degree relationships
3. Use time-based partitioning for temporal data
4. Add filtering early in query

### Data Modeling Issues

**Query Performance Poor:**
- Review if your model matches query patterns
- Consider if relationships should be properties or vice versa
- Check for over-modeling

**Data Duplication:**
- Use relationships instead of copying data
- Create shared entities and link them

**Complex Queries:**
- Simplify data model to match access patterns
- Consider denormalization for read-heavy queries

## Development Workflow Best Practices

### Design Phase
1. **Identify entities** - What are the main nouns?
2. **Define relationships** - How do entities connect?
3. **Plan queries** - What questions will you ask?
4. **Model accordingly** - Design for your query patterns
5. **Validate with stakeholders** - Ensure model meets requirements

### Implementation Phase
1. **Start simple** - Begin with core entities and relationships
2. **Create constraints** - Ensure data integrity from the start
3. **Add indexes strategically** - Only on frequently queried properties
4. **Import data** - Use analytical mode for bulk imports
5. **Test queries** - Validate performance with realistic data
6. **Iterate** - Refine model based on usage

### Optimization Phase
1. **Profile queries** - Use EXPLAIN/PROFILE to find bottlenecks
2. **Run ANALYZE GRAPH** - Help optimizer make better decisions
3. **Review indexes** - Add missing, remove unused
4. **Check storage mode** - Match mode to workload
5. **Monitor memory** - Ensure efficient resource usage

### Production Phase
1. **Set up monitoring** - Track performance metrics
2. **Configure backups** - Regular dumps and snapshots
3. **Plan for scaling** - Consider read replicas, sharding
4. **Implement security** - RBAC, encryption, audit logs
5. **Document schema** - Use `CALL schema()` for documentation

## Quick Reference Commands

### Database Management
```cypher
// Show database info
SHOW STORAGE INFO;
SHOW INDEX INFO;
SHOW CONSTRAINT INFO;
SHOW STREAMS;
SHOW TRIGGERS;

// Schema introspection
CALL schema() YIELD schema_in_prompt RETURN schema_in_prompt;

// Performance
ANALYZE GRAPH;

// Clear database
MATCH (n) DETACH DELETE n;  // Use with caution!
```

### Import/Export
```cypher
// Import CSV
LOAD CSV FROM '/path/to/file.csv' WITH HEADER AS row
CREATE (n:User {name: row.name, age: toInteger(row.age)});

// Export (use dump utility)
// mg_dump --host 127.0.0.1 --port 7687 > backup.cypher
```

## Additional Resources

- **Documentation**: https://memgraph.com/docs
- **Cypher Manual**: https://memgraph.com/docs/cypher-manual
- **MAGE Library**: https://memgraph.com/docs/mage
- **GitHub**: https://github.com/memgraph/memgraph
- **Community Discord**: Join for support and discussions

## Key Reminders

1. **Always think in graphs** - Relationships are first-class citizens
2. **Index strategically** - High-cardinality, frequently queried properties only
3. **Run ANALYZE GRAPH** - After bulk operations and before complex queries
4. **Use inline filtering** - For path traversals when possible
5. **Match storage mode to workload** - Transactional vs Analytical
6. **Avoid supernodes** - High-degree nodes impact performance
7. **Profile before optimizing** - Measure to find real bottlenecks
8. **Keep it simple** - Don't over-model, focus on value

## Success Criteria

When working with Memgraph, you've succeeded when:
- ✅ Queries return results in sub-millisecond to millisecond range
- ✅ Data model is intuitive and matches query patterns
- ✅ Indexes are on high-cardinality, frequently queried properties
- ✅ No unnecessary data duplication
- ✅ Constraints ensure data integrity
- ✅ PROFILE shows efficient query plans
- ✅ Memory usage is reasonable for dataset size
- ✅ Schema is documented and understood by team
