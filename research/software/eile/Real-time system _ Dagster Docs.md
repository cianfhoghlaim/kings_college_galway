---
title: "Real-time system | Dagster Docs"
source: "https://docs.dagster.io/examples/reference-architectures/real-time"
author:
published:
created: 2025-12-20
description: "A real-time system that detects abandoned carts and sends notifications to a marketing platform."
tags:
  - "clippings"
---
## Objective

Build an abandoned cart notification system that ingests customer data (Postgres) alongside real-time cart data (Kafka). A real-time view (ClickHouse) calculates which users have cart items that haven't been included in an order within the past hour. Newly identified abandoned carts are then sent downstream to the marketing platform (Braze).

## Architecture

    <svg id="mermaid-svg-9156476" width="100%" xmlns="http://www.w3.org/2000/svg" class="flowchart" style="max-width: 642.9166870117188px;" viewBox="0 0 642.9166870117188 274" role="graphics-document document" aria-roledescription="flowchart-v2"><g><marker id="mermaid-svg-9156476_flowchart-v2-pointEnd" class="marker flowchart-v2" viewBox="0 0 10 10" refX="5" refY="5" markerUnits="userSpaceOnUse" markerWidth="8" markerHeight="8" orient="auto"><path d="M 0 0 L 10 5 L 0 10 z" class="arrowMarkerPath" style="stroke-width: 1px; stroke-dasharray: 1px, 0px;"></path></marker><marker id="mermaid-svg-9156476_flowchart-v2-pointStart" class="marker flowchart-v2" viewBox="0 0 10 10" refX="4.5" refY="5" markerUnits="userSpaceOnUse" markerWidth="8" markerHeight="8" orient="auto"><path d="M 0 5 L 10 10 L 10 0 z" class="arrowMarkerPath" style="stroke-width: 1px; stroke-dasharray: 1px, 0px;"></path></marker><marker id="mermaid-svg-9156476_flowchart-v2-circleEnd" class="marker flowchart-v2" viewBox="0 0 10 10" refX="11" refY="5" markerUnits="userSpaceOnUse" markerWidth="11" markerHeight="11" orient="auto"><circle cx="5" cy="5" r="5" class="arrowMarkerPath" style="stroke-width: 1px; stroke-dasharray: 1px, 0px;"></circle></marker><marker id="mermaid-svg-9156476_flowchart-v2-circleStart" class="marker flowchart-v2" viewBox="0 0 10 10" refX="-1" refY="5" markerUnits="userSpaceOnUse" markerWidth="11" markerHeight="11" orient="auto"><circle cx="5" cy="5" r="5" class="arrowMarkerPath" style="stroke-width: 1px; stroke-dasharray: 1px, 0px;"></circle></marker><marker id="mermaid-svg-9156476_flowchart-v2-crossEnd" class="marker cross flowchart-v2" viewBox="0 0 11 11" refX="12" refY="5.2" markerUnits="userSpaceOnUse" markerWidth="11" markerHeight="11" orient="auto"><path d="M 1,1 l 9,9 M 10,1 l -9,9" class="arrowMarkerPath" style="stroke-width: 2px; stroke-dasharray: 1px, 0px;"></path></marker><marker id="mermaid-svg-9156476_flowchart-v2-crossStart" class="marker cross flowchart-v2" viewBox="0 0 11 11" refX="-1" refY="5.2" markerUnits="userSpaceOnUse" markerWidth="11" markerHeight="11" orient="auto"><path d="M 1,1 l 9,9 M 10,1 l -9,9" class="arrowMarkerPath" style="stroke-width: 2px; stroke-dasharray: 1px, 0px;"></path></marker><g class="root"><g class="clusters"></g><g class="edgePaths"><path d="M127.067,60L131.233,60C135.4,60,143.733,60,151.4,60C159.067,60,166.067,60,169.567,60L173.067,60" id="L_PG_DL_0" class="edge-thickness-thick edge-pattern-solid edge-thickness-normal edge-pattern-solid flowchart-link" style=";" data-edge="true" data-et="edge" data-id="L_PG_DL_0" data-points="W3sieCI6MTI3LjA2NjY2NTY0OTQxNDA2LCJ5Ijo2MH0seyJ4IjoxNTIuMDY2NjY1NjQ5NDE0MDYsInkiOjYwfSx7IngiOjE3Ny4wNjY2NjU2NDk0MTQwNiwieSI6NjB9XQ==" marker-end="url(#mermaid-svg-9156476_flowchart-v2-pointEnd)"></path><path d="M287.067,60L291.233,60C295.4,60,303.733,60,312.467,63.744C321.201,67.488,330.335,74.976,334.901,78.72L339.468,82.464" id="L_DL_CH_0" class="edge-thickness-thick edge-pattern-solid edge-thickness-normal edge-pattern-solid flowchart-link" style=";" data-edge="true" data-et="edge" data-id="L_DL_CH_0" data-points="W3sieCI6Mjg3LjA2NjY2NTY0OTQxNDA2LCJ5Ijo2MH0seyJ4IjozMTIuMDY2NjY1NjQ5NDE0MDYsInkiOjYwfSx7IngiOjM0Mi41NjE3OTY1MTAzNzQ0LCJ5Ijo4NX1d" marker-end="url(#mermaid-svg-9156476_flowchart-v2-pointEnd)"></path><path d="M282.067,214L287.067,214C292.067,214,302.067,214,311.634,210.256C321.201,206.512,330.335,199.024,334.901,195.28L339.468,191.536" id="L_KF_CH_0" class="edge-thickness-thick edge-pattern-solid edge-thickness-normal edge-pattern-solid flowchart-link" style=";" data-edge="true" data-et="edge" data-id="L_KF_CH_0" data-points="W3sieCI6MjgyLjA2NjY2NTY0OTQxNDA2LCJ5IjoyMTR9LHsieCI6MzEyLjA2NjY2NTY0OTQxNDA2LCJ5IjoyMTR9LHsieCI6MzQyLjU2MTc5NjUxMDM3NDQsInkiOjE4OX1d" marker-end="url(#mermaid-svg-9156476_flowchart-v2-pointEnd)"></path><path d="M474.917,137L479.083,137C483.25,137,491.583,137,499.25,137C506.917,137,513.917,137,517.417,137L520.917,137" id="L_CH_BZ_0" class="edge-thickness-thick edge-pattern-solid edge-thickness-normal edge-pattern-solid flowchart-link" style=";" data-edge="true" data-et="edge" data-id="L_CH_BZ_0" data-points="W3sieCI6NDc0LjkxNjY3MTc1MjkyOTcsInkiOjEzN30seyJ4Ijo0OTkuOTE2NjcxNzUyOTI5NywieSI6MTM3fSx7IngiOjUyNC45MTY2NzE3NTI5Mjk3LCJ5IjoxMzd9XQ==" marker-end="url(#mermaid-svg-9156476_flowchart-v2-pointEnd)"></path></g><g class="edgeLabels"><g class="edgeLabel"><g class="label" data-id="L_PG_DL_0" transform="translate(0, 0)"></g></g><g class="edgeLabel"><g class="label" data-id="L_DL_CH_0" transform="translate(0, 0)"></g></g><g class="edgeLabel"><g class="label" data-id="L_KF_CH_0" transform="translate(0, 0)"></g></g><g class="edgeLabel"><g class="label" data-id="L_CH_BZ_0" transform="translate(0, 0)"></g></g></g><g class="nodes"><g class="node default" id="flowchart-PG-0" transform="translate(67.53333282470703, 60)"><rect class="basic label-container" style="" x="-59.53333282470703" y="-52" width="119.06666564941406" height="104"></rect><g class="label" style="" transform="translate(-29.53333282470703, -37)"><rect></rect><foreignObject width="59.06666564941406" height="74"><img xmlns="http://www.w3.org/1999/xhtml" src="https://docs.dagster.io/images/examples/icons/postgres.svg" width="50" height="50"></foreignObject></g></g> <g class="node default" id="flowchart-CH-1" transform="translate(405.9916687011719, 137)"><rect class="basic label-container" style="" x="-68.92500305175781" y="-52" width="137.85000610351562" height="104"></rect><g class="label" style="" transform="translate(-38.92500305175781, -37)"><rect></rect><foreignObject width="77.85000610351562" height="74"><img xmlns="http://www.w3.org/1999/xhtml" src="https://docs.dagster.io/images/examples/icons/clickhouse.svg" width="50" height="50"></foreignObject></g></g> <g class="node default" id="flowchart-DL-2" transform="translate(232.06666564941406, 60)"><rect class="basic label-container" style="" x="-55" y="-52" width="110" height="104"></rect><g class="label" style="" transform="translate(-25, -37)"><rect></rect><foreignObject width="50" height="74"><img xmlns="http://www.w3.org/1999/xhtml" src="https://docs.dagster.io/images/examples/icons/dlthub.jpeg" width="50" height="50"></foreignObject></g></g> <g class="node default" id="flowchart-KF-3" transform="translate(232.06666564941406, 214)"><rect class="basic label-container" style="" x="-50" y="-52" width="100" height="104"></rect><g class="label" style="" transform="translate(-20, -37)"><rect></rect><foreignObject width="40" height="74"><img xmlns="http://www.w3.org/1999/xhtml" src="https://docs.dagster.io/images/examples/icons/kafka.svg" width="50" height="50"></foreignObject></g></g> <g class="node default" id="flowchart-BZ-4" transform="translate(579.9166717529297, 137)"><rect class="basic label-container" style="" x="-55" y="-52" width="110" height="104"></rect><g class="label" style="" transform="translate(-25, -37)"><rect></rect><foreignObject width="50" height="74"><img xmlns="http://www.w3.org/1999/xhtml" src="https://docs.dagster.io/images/examples/icons/braze.svg" width="50" height="50"></foreignObject></g></g></g></g></g></svg>

## Dagster Architecture

![2048 resolution](https://docs.dagster.io/assets/images/real-time-da239c1c2885e7edfaddabcdd8691979.png)

### 1\. Postgres ingestion with dlt

The integration between Postgres and Clickhouse is defined in dlt via YAML configuration in the code alongside the Dagster code. Dagster executes dlt on a schedule to extract stateful customer data into Clickhouse.

**Dagster Features**

- [Dagster dlt](https://docs.dagster.io/integrations/libraries/dlt)
- [Schedules](https://docs.dagster.io/guides/automate/schedules)

---

### 2\. Kafka ingestion

Real-time data on carts is brought into Clickhouse from the Kafka topic.

**Dagster Features**

- [Declarative Automation](https://docs.dagster.io/guides/automate/declarative-automation)

---

### 3\. Abandoned cart materialization

The customer data is combined with the real-time cart data to identify users who have not acted on their cart within the last hour. This materialized view lives in Clickhouse (which can be managed with a custom resource), capturing only abandoned carts from the last 3 hours to prevent the view from growing too large over time.

**Dagster Features**

- [Resources](https://docs.dagster.io/guides/build/external-resources)

---

### 4\. Notifications sent to the marketing tool

A sensor checks the abandoned cart view in Clickhouse for new abandoned carts which are sent to Braze via their API.

**Dagster Features**

- [Sensors](https://docs.dagster.io/guides/automate/sensors)