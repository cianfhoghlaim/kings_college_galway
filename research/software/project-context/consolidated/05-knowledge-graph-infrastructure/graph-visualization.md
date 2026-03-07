# Graph Visualization Tools and Patterns

## Executive Summary

Effective visualization of knowledge graphs is essential for debugging AI memory systems, understanding entity relationships, and building user-facing interfaces. This document covers the frontend library ecosystem, backend data export patterns, and specialized techniques for temporal graph visualization.

---

## 1. The Visualization Challenge

### 1.1 Graph Data Characteristics

| Challenge | Cause | Solution |
|-----------|-------|----------|
| **Hairball Effect** | Too many edges | Community detection, filtering |
| **Overlapping Labels** | Dense clusters | Semantic zoom, tooltips |
| **Performance** | >1000 nodes | WebGL rendering |
| **Temporal Complexity** | Edge lifespans | Time slider, animation |

### 1.2 Visualization Goals

| Goal | User Question | Required Feature |
|------|---------------|------------------|
| **Structure** | "How is the graph organized?" | Layout algorithms |
| **Navigation** | "What connects A to B?" | Path highlighting |
| **History** | "How has this changed?" | Time slider |
| **Provenance** | "Where did this fact come from?" | Source linking |

---

## 2. Frontend Library Ecosystem

### 2.1 react-force-graph

The recommended library for most use cases due to performance and flexibility.

**Installation:**
```bash
npm install react-force-graph
```

**Basic Usage:**
```typescript
import ForceGraph2D from 'react-force-graph-2d';
import ForceGraph3D from 'react-force-graph-3d';

const GraphVisualization = ({ data }) => {
  return (
    <ForceGraph2D
      graphData={data}
      nodeLabel="label"
      nodeAutoColorBy="group"
      linkLabel="relation"
      linkDirectionalArrowLength={3}
      linkDirectionalArrowRelPos={1}
      onNodeClick={(node) => console.log('Clicked:', node)}
    />
  );
};
```

**Rendering Modes:**

| Mode | Use Case | Node Limit |
|------|----------|------------|
| **2D (Canvas)** | Text readability | ~2,000 |
| **3D (WebGL)** | Large datasets | ~50,000 |
| **VR/AR** | Immersive analytics | ~10,000 |

### 2.2 Cytoscape.js

Best for structured analysis and compound nodes.

**Installation:**
```bash
npm install react-cytoscapejs cytoscape
```

**Usage with Dagre Layout:**
```typescript
import CytoscapeComponent from 'react-cytoscapejs';
import dagre from 'cytoscape-dagre';

cytoscape.use(dagre);

const HierarchicalGraph = ({ elements }) => {
  return (
    <CytoscapeComponent
      elements={elements}
      layout={{ name: 'dagre' }}
      style={{ width: '100%', height: '600px' }}
      stylesheet={[
        {
          selector: 'node',
          style: {
            'label': 'data(label)',
            'background-color': 'data(color)'
          }
        },
        {
          selector: 'edge',
          style: {
            'label': 'data(label)',
            'curve-style': 'bezier',
            'target-arrow-shape': 'triangle'
          }
        }
      ]}
    />
  );
};
```

### 2.3 D3.js (Low-Level)

For maximum customization when library abstractions are insufficient.

```typescript
import * as d3 from 'd3';
import { useRef, useEffect } from 'react';

const D3Graph = ({ data }) => {
  const svgRef = useRef();

  useEffect(() => {
    const svg = d3.select(svgRef.current);

    const simulation = d3.forceSimulation(data.nodes)
      .force('link', d3.forceLink(data.links).id(d => d.id))
      .force('charge', d3.forceManyBody().strength(-100))
      .force('center', d3.forceCenter(400, 300));

    const link = svg.selectAll('line')
      .data(data.links)
      .join('line')
      .attr('stroke', '#999');

    const node = svg.selectAll('circle')
      .data(data.nodes)
      .join('circle')
      .attr('r', 10)
      .attr('fill', d => colorScale(d.group));

    simulation.on('tick', () => {
      link
        .attr('x1', d => d.source.x)
        .attr('y1', d => d.source.y)
        .attr('x2', d => d.target.x)
        .attr('y2', d => d.target.y);

      node
        .attr('cx', d => d.x)
        .attr('cy', d => d.y);
    });
  }, [data]);

  return <svg ref={svgRef} width={800} height={600} />;
};
```

---

## 3. Backend Data Export Patterns

### 3.1 NetworkX to JSON

```python
import networkx as nx
from networkx.readwrite import json_graph

def export_graph_for_frontend(G: nx.DiGraph) -> dict:
    """Export NetworkX graph to react-force-graph format."""
    data = json_graph.node_link_data(G)

    # Enrich nodes with visualization metadata
    for node in data["nodes"]:
        node["group"] = node.get("type", "default")
        node["label"] = node.get("name", str(node["id"]))
        node["val"] = node.get("importance", 1)  # Node size

    # Enrich links with visualization metadata
    for link in data["links"]:
        link["label"] = link.get("relation_type", "RELATES_TO")
        link["curvature"] = 0.2  # Curved edges for readability

    return data
```

### 3.2 Neo4j/FalkorDB Export

```python
async def export_cypher_to_json(driver, query: str = None):
    """Export Cypher query result to visualization format."""
    if query is None:
        query = "MATCH (n)-[r]->(m) RETURN n, r, m"

    async with driver.session() as session:
        result = await session.run(query)

        nodes = {}
        links = []

        async for record in result:
            n = record["n"]
            m = record["m"]
            r = record["r"]

            # Add nodes
            nodes[n.id] = {
                "id": n.id,
                "label": n.get("name", str(n.id)),
                "group": list(n.labels)[0] if n.labels else "default",
                **dict(n)
            }
            nodes[m.id] = {
                "id": m.id,
                "label": m.get("name", str(m.id)),
                "group": list(m.labels)[0] if m.labels else "default",
                **dict(m)
            }

            # Add link
            links.append({
                "source": n.id,
                "target": m.id,
                "label": r.type,
                **dict(r)
            })

        return {
            "nodes": list(nodes.values()),
            "links": links
        }
```

### 3.3 Graphiti Search Result Export

```python
async def export_search_result(results: SearchResult) -> dict:
    """Export Graphiti search results for visualization."""
    nodes = []
    links = []
    node_ids = set()

    for entity in results.nodes:
        nodes.append({
            "id": entity.uuid,
            "label": entity.name,
            "group": entity.entity_type,
            "valid_at": entity.valid_at.isoformat() if entity.valid_at else None,
            "invalid_at": entity.invalid_at.isoformat() if entity.invalid_at else None,
            "score": entity.score
        })
        node_ids.add(entity.uuid)

    for edge in results.edges:
        if edge.source in node_ids and edge.target in node_ids:
            links.append({
                "source": edge.source,
                "target": edge.target,
                "label": edge.relation_type,
                "fact": edge.fact,
                "valid_at": edge.valid_at.isoformat() if edge.valid_at else None,
                "invalid_at": edge.invalid_at.isoformat() if edge.invalid_at else None
            })

    return {"nodes": nodes, "links": links}
```

---

## 4. Temporal Visualization Techniques

### 4.1 Time Slider Component

```typescript
import { useState, useEffect, useMemo } from 'react';

interface TemporalGraphProps {
  fullGraph: GraphData;
  timeRange: { min: Date; max: Date };
}

const TemporalGraph = ({ fullGraph, timeRange }: TemporalGraphProps) => {
  const [currentTime, setCurrentTime] = useState(timeRange.max.getTime());

  // Filter graph based on current time
  const displayedGraph = useMemo(() => {
    const activeLinks = fullGraph.links.filter(link => {
      const validFrom = new Date(link.valid_at).getTime();
      const validUntil = link.invalid_at
        ? new Date(link.invalid_at).getTime()
        : Infinity;
      return currentTime >= validFrom && currentTime < validUntil;
    });

    const activeNodeIds = new Set<string>();
    activeLinks.forEach(link => {
      activeNodeIds.add(link.source);
      activeNodeIds.add(link.target);
    });

    const activeNodes = fullGraph.nodes.filter(node =>
      activeNodeIds.has(node.id)
    );

    return { nodes: activeNodes, links: activeLinks };
  }, [fullGraph, currentTime]);

  return (
    <div>
      <TimeSlider
        min={timeRange.min.getTime()}
        max={timeRange.max.getTime()}
        value={currentTime}
        onChange={setCurrentTime}
        histogram={buildHistogram(fullGraph.links)}
      />
      <ForceGraph2D graphData={displayedGraph} />
    </div>
  );
};

// Histogram showing event distribution
const buildHistogram = (links: Link[]) => {
  const buckets: Record<string, number> = {};

  links.forEach(link => {
    const month = link.valid_at.slice(0, 7); // YYYY-MM
    buckets[month] = (buckets[month] || 0) + 1;
  });

  return Object.entries(buckets).map(([month, count]) => ({
    month,
    count
  }));
};
```

### 4.2 Edge State Visualization

```typescript
// Visual encoding for edge lifecycle
const getEdgeStyle = (edge: Edge, currentTime: number) => {
  const validFrom = new Date(edge.valid_at).getTime();
  const validUntil = edge.invalid_at
    ? new Date(edge.invalid_at).getTime()
    : Infinity;

  if (currentTime < validFrom) {
    // Future edge - not yet valid
    return {
      color: 'rgba(100, 100, 100, 0.2)',
      dashArray: '5,5',
      width: 1
    };
  }

  if (currentTime >= validUntil) {
    // Historical edge - no longer valid
    return {
      color: 'rgba(200, 100, 100, 0.3)',
      dashArray: '2,2',
      width: 1
    };
  }

  // Active edge
  return {
    color: 'rgba(50, 150, 250, 0.8)',
    dashArray: null,
    width: 2
  };
};
```

### 4.3 Animated Time Travel

```typescript
const AnimatedTimeTravel = ({ fullGraph, timeRange }) => {
  const [currentTime, setCurrentTime] = useState(timeRange.min.getTime());
  const [isPlaying, setIsPlaying] = useState(false);

  useEffect(() => {
    if (!isPlaying) return;

    const interval = setInterval(() => {
      setCurrentTime(prev => {
        if (prev >= timeRange.max.getTime()) {
          setIsPlaying(false);
          return prev;
        }
        // Advance by 1 month (30 days)
        return prev + (30 * 24 * 60 * 60 * 1000);
      });
    }, 500); // 500ms per frame

    return () => clearInterval(interval);
  }, [isPlaying, timeRange]);

  return (
    <div>
      <button onClick={() => setIsPlaying(!isPlaying)}>
        {isPlaying ? 'Pause' : 'Play'}
      </button>
      <span>{new Date(currentTime).toLocaleDateString()}</span>
      <ForceGraph2D graphData={filterByTime(fullGraph, currentTime)} />
    </div>
  );
};
```

---

## 5. Semantic Zoom (Level of Detail)

### 5.1 Zoom Level Strategy

| Zoom Level | Content | Implementation |
|------------|---------|----------------|
| **High (Overview)** | Communities/clusters | Hide individual nodes, show aggregates |
| **Medium** | Nodes without labels | Show nodes, hide text |
| **Low (Detail)** | Full labels, edge text | Show all information |

### 5.2 Implementation

```typescript
const SemanticZoomGraph = ({ graphData }) => {
  const [zoomLevel, setZoomLevel] = useState(1);

  const handleZoom = ({ k }) => {
    setZoomLevel(k);
  };

  return (
    <ForceGraph2D
      graphData={graphData}
      onZoom={handleZoom}
      // Conditional rendering based on zoom
      nodeLabel={zoomLevel > 2 ? 'label' : null}
      linkLabel={zoomLevel > 3 ? 'label' : null}
      nodeVal={node => (zoomLevel < 1.5 ? node.communitySize : 1)}
      nodeCanvasObject={(node, ctx, globalScale) => {
        if (globalScale < 1.5) {
          // Draw simple circle
          ctx.beginPath();
          ctx.arc(node.x, node.y, 5, 0, 2 * Math.PI);
          ctx.fill();
        } else {
          // Draw with label
          ctx.font = '12px Sans-Serif';
          ctx.fillText(node.label, node.x + 8, node.y + 4);
        }
      }}
    />
  );
};
```

---

## 6. Performance Optimization

### 6.1 WebGL for Large Graphs

```typescript
import ForceGraph3D from 'react-force-graph-3d';

// For graphs > 1,000 nodes, use 3D WebGL
const LargeGraphVisualization = ({ graphData }) => {
  // 3D separates clusters naturally in z-dimension
  return (
    <ForceGraph3D
      graphData={graphData}
      nodeAutoColorBy="group"
      // Performance optimizations
      warmupTicks={100}
      cooldownTicks={0}
      enableNodeDrag={false} // Disable for very large graphs
    />
  );
};
```

### 6.2 Server-Side Subgraphing

```python
# Never send entire graph to client for large datasets
@app.get("/api/neighborhood/{node_id}")
async def get_neighborhood(node_id: str, depth: int = 2):
    """Get k-hop neighborhood around a node."""
    query = f"""
    MATCH path = (start {{id: $node_id}})-[*1..{depth}]-(neighbor)
    RETURN path
    """

    result = await export_cypher_to_json(driver, query, {"node_id": node_id})
    return result
```

### 6.3 Progressive Loading

```typescript
const ProgressiveGraph = ({ initialNodeId }) => {
  const [graphData, setGraphData] = useState({ nodes: [], links: [] });

  // Load initial neighborhood
  useEffect(() => {
    fetch(`/api/neighborhood/${initialNodeId}?depth=1`)
      .then(res => res.json())
      .then(data => setGraphData(data));
  }, [initialNodeId]);

  // Expand on node click
  const handleNodeClick = async (node) => {
    const expansion = await fetch(`/api/neighborhood/${node.id}?depth=1`)
      .then(res => res.json());

    // Merge new data with existing
    setGraphData(prev => ({
      nodes: [...prev.nodes, ...expansion.nodes.filter(
        n => !prev.nodes.some(existing => existing.id === n.id)
      )],
      links: [...prev.links, ...expansion.links.filter(
        l => !prev.links.some(existing =>
          existing.source === l.source && existing.target === l.target
        )
      )]
    }));
  };

  return (
    <ForceGraph2D
      graphData={graphData}
      onNodeClick={handleNodeClick}
    />
  );
};
```

---

## 7. Layout Algorithm Selection

### 7.1 Comparison

| Algorithm | Best For | Library |
|-----------|----------|---------|
| **Force-Directed** | General exploration | react-force-graph |
| **Dagre** | Hierarchies, DAGs | cytoscape-dagre |
| **Cola** | Constraint-based | cytoscape-cola |
| **Concentric** | Centered on important node | cytoscape |
| **Grid** | Regular structure | cytoscape |

### 7.2 Choosing Layout by Graph Type

```typescript
const getLayoutForGraphType = (graphData) => {
  // Detect graph characteristics
  const isTree = graphData.links.length === graphData.nodes.length - 1;
  const hasHierarchy = graphData.nodes.some(n => n.level !== undefined);
  const isSmall = graphData.nodes.length < 50;

  if (isTree || hasHierarchy) {
    return 'dagre';
  }

  if (isSmall) {
    return 'force-directed';
  }

  return '3d-force'; // WebGL for large graphs
};
```

---

## 8. Implementation Priorities

### Phase 1: Basic Visualization
1. Set up react-force-graph-2d
2. Implement backend JSON export
3. Add basic node/edge styling

### Phase 2: Interactivity
1. Add click handlers for details
2. Implement search highlighting
3. Add zoom controls

### Phase 3: Temporal Features
1. Build time slider component
2. Implement edge filtering by time
3. Add animation playback

### Phase 4: Performance
1. Migrate to WebGL for large graphs
2. Implement server-side subgraphing
3. Add progressive loading

---

## References

- react-force-graph: https://github.com/vasturiano/react-force-graph
- Cytoscape.js: https://js.cytoscape.org/
- D3.js Force: https://d3js.org/d3-force
- NetworkX JSON: https://networkx.org/documentation/stable/reference/readwrite/json_graph.html
