# Evidence.dev Component System Reference

A comprehensive technical reference for Evidence.dev's component system, including all available components, their props, data binding patterns, and usage examples.

## Table of Contents

1. [Overview](#overview)
2. [Data Binding Fundamentals](#data-binding-fundamentals)
3. [Data Components](#data-components)
4. [Chart Components](#chart-components)
5. [Map Components](#map-components)
6. [Input Components](#input-components)
7. [UI Components](#ui-components)
8. [Custom Components](#custom-components)
9. [Formatting Options](#formatting-options)
10. [Theming & Styling](#theming--styling)
11. [Advanced Patterns](#advanced-patterns)

---

## Overview

Evidence is an open-source framework for building data products with SQL and Markdown. It uses:
- **ECharts** for charts
- **Leaflet** for maps
- **Shadcn** for UI components
- **Tailwind CSS** for styling
- **DuckDB** for SQL query execution

### Component Syntax

Components use angle bracket syntax similar to HTML:

```svelte
<ComponentName prop1=value prop2={expression} />
```

### Key Conventions

- **Query references**: Use curly braces `{query_name}`
- **String values**: Can omit quotes for simple values
- **Boolean props**: `prop=true` or just `prop`
- **Arrays/Objects**: Use curly braces `{['a', 'b']}`

---

## Data Binding Fundamentals

### Defining SQL Queries

Queries are defined in markdown code fences with a name:

```sql query_name
SELECT category, SUM(sales) as sales
FROM orders
GROUP BY 1
```

### Referencing Query Results

Pass query results to components using the `data` prop:

```svelte
<LineChart data={query_name} />
```

### Query Chaining

Reference other queries within SQL using `${}` syntax:

```sql derived_query
SELECT AVG(sales) as avg_sales
FROM ${query_name}
```

### Input Parameters

Filter queries dynamically with input values:

```sql filtered_query
SELECT * FROM orders
WHERE category = '${inputs.category_dropdown.value}'
```

### URL Parameters

Use templated page parameters:

```sql parameterized_query
SELECT * FROM orders
WHERE region = '${params.region}'
```

### SQL File Queries

Store reusable queries in `/queries/` directory and reference in frontmatter:

```yaml
---
queries:
  - sales_data: my_query.sql
---
```

### JavaScript Expressions

Access data in markdown using curly braces:

```markdown
Total orders: {orders.length}
First value: {orders_by_month[0].sales}
Sum: {orders.reduce((a, b) => a + b.sales, 0)}
```

### Loops

Iterate through data:

```svelte
{#each orders_by_month as month}
- Sales: <Value data={month} column=sales />
{/each}
```

### Conditionals

Control display based on data:

```svelte
{#if orders[0].sales > 1000}
  Sales exceeded target!
{:else}
  Below target.
{/if}
```

---

## Data Components

### Value

Display a formatted value inline in text.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `data` | query | required | Query name in curly braces |
| `column` | string | first column | Column to display |
| `row` | number | 0 | Zero-indexed row number |
| `fmt` | string | - | Value format |
| `agg` | string | - | Aggregation: sum, avg, min, median, max |
| `color` | string | - | Font color (CSS/hex/RGB/HSL) |
| `redNegatives` | boolean | false | Color negative values red |
| `placeholder` | string | - | Text when data unavailable |
| `emptySet` | string | error | error, warn, pass |
| `emptyMessage` | string | "No records" | Message for empty data |
| `description` | string | - | Tooltip text |

#### Examples

```svelte
<!-- Basic usage -->
Total sales: <Value data={sales} column=total fmt=usd />

<!-- With aggregation -->
Average: <Value data={orders} column=amount agg=avg fmt=usd0 />

<!-- Colored value -->
<Value data={sales} column=growth color="#85BB65" />
```

---

### BigValue

Display a large value with optional comparison and sparkline.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `data` | query | required | Query name |
| `value` | string | required | Column for main value |
| `title` | string | column name | Card heading |
| `fmt` | string | - | Value format |
| `minWidth` | string | - | Minimum width (e.g., "18%") |
| `maxWidth` | string | - | Maximum width |
| `link` | string | - | Navigation URL |
| `description` | string | - | Tooltip text |

**Comparison Props:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `comparison` | string | - | Column for comparison value |
| `comparisonFmt` | string | - | Comparison format |
| `comparisonTitle` | string | - | Comparison label |
| `comparisonDelta` | boolean | - | Show as delta |
| `downIsGood` | boolean | false | Invert color coding |
| `neutralMin` | number | 0 | Neutral range minimum |
| `neutralMax` | number | 0 | Neutral range maximum |

**Sparkline Props:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `sparkline` | string | - | Column for sparkline x-axis |
| `sparklineType` | string | line | line, area, bar |
| `sparklineColor` | string | - | Sparkline color |
| `sparklineYScale` | boolean | false | Truncate y-axis |

#### Examples

```svelte
<!-- Basic BigValue -->
<BigValue
  data={sales_summary}
  value=total_sales
  title="Total Sales"
  fmt=usd0
/>

<!-- With comparison -->
<BigValue
  data={sales_comparison}
  value=current_sales
  comparison=growth
  comparisonFmt=pct1
  comparisonTitle="vs. Last Month"
  comparisonDelta=true
/>

<!-- With sparkline -->
<BigValue
  data={sales_trend}
  value=total
  sparkline=month
  sparklineType="area"
/>
```

---

### Delta

Display an inline indicator showing value change.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `data` | query | - | Query name |
| `column` | string | first | Column to display |
| `row` | number | 0 | Row index |
| `value` | number | - | Direct value (override data) |
| `fmt` | string | - | Value format |
| `downIsGood` | boolean | false | Invert colors |
| `showSymbol` | boolean | true | Show arrow |
| `showValue` | boolean | true | Show number |
| `text` | string | - | Text after value |
| `neutralMin` | number | - | Neutral range min |
| `neutralMax` | number | - | Neutral range max |
| `chip` | boolean | false | Badge style |
| `symbolPosition` | string | right | left or right |

#### Examples

```svelte
<!-- Basic delta -->
<Delta data={growth} column=percent fmt=pct1 />

<!-- With text -->
<Delta data={growth} column=change text="vs last month" />

<!-- Chip style -->
<Delta data={growth} column=percent chip=true />

<!-- Inverted colors -->
<Delta data={costs} column=change downIsGood=true />
```

---

### DataTable

Display a richly formatted, interactive data table.

#### DataTable Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `data` | query | required | Query name |
| `rows` | number/string | 10 | Rows before pagination (use "all" for all) |
| `title` | string | - | Table title |
| `subtitle` | string | - | Subtitle |

**Styling:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `headerColor` | string | - | Header background |
| `headerFontColor` | string | - | Header text color |
| `backgroundColor` | string | - | Table background |
| `rowShading` | boolean | false | Alternating row colors |
| `rowLines` | boolean | true | Row borders |
| `rowNumbers` | boolean | false | Show row index |
| `compact` | boolean | false | Compact layout |
| `wrapTitles` | boolean | false | Wrap column titles |

**Functionality:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `sortable` | boolean | true | Enable sorting |
| `sort` | string | - | Initial sort ("column asc/desc") |
| `search` | boolean | false | Add search bar |
| `downloadable` | boolean | true | Enable download |
| `link` | string | - | Column for row links |
| `showLinkCol` | boolean | false | Display link column |

**Totals & Grouping:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `totalRow` | boolean | false | Show totals |
| `totalRowColor` | string | - | Total row background |
| `groupBy` | string | - | Grouping column |
| `groupType` | string | accordion | accordion or section |
| `subtotals` | boolean | false | Show group totals |

#### Column Component Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `id` | string | required | Column identifier |
| `title` | string | - | Override header |
| `description` | string | - | Tooltip text |
| `align` | string | left | left, center, right |
| `wrap` | boolean | false | Wrap text |
| `fmt` | string | - | Format code |
| `redNegatives` | boolean | false | Color negatives red |
| `totalAgg` | string | - | Aggregation function |
| `contentType` | string | - | Special rendering |

**Content Types:**
- `link` - Clickable links
- `image` - Display images
- `delta` - Delta indicators
- `colorscale` - Background color scale
- `html` - Raw HTML
- `sparkline`, `sparkarea`, `sparkbar` - Inline charts
- `bar` - Bar chart in cell

#### Examples

```svelte
<!-- Basic table -->
<DataTable data={orders} search=true />

<!-- Customized table -->
<DataTable
  data={sales}
  rows=20
  totalRow=true
  rowShading=true
  search=true
>
  <Column id=product title="Product Name" />
  <Column id=sales fmt=usd totalAgg=sum />
  <Column id=growth contentType=delta />
  <Column id=trend contentType=sparkline sparkY=values />
</DataTable>

<!-- Grouped table -->
<DataTable
  data={orders}
  groupBy=category
  subtotals=true
/>
```

---

## Chart Components

### Common Chart Props

Most charts share these props:

**Data Props:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `data` | query | required | Query name |
| `x` | string | first column | X-axis column |
| `y` | string/array | numeric columns | Y-axis column(s) |
| `series` | string | - | Multi-series grouping |
| `sort` | boolean | true | Apply sorting |
| `emptySet` | string | error | error, warn, pass |
| `emptyMessage` | string | "No records" | Empty state text |

**Formatting & Styling:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `xFmt`, `yFmt` | string | - | Axis formatting |
| `colorPalette` | array | - | Custom colors |
| `seriesColors` | object | - | Map series to colors |
| `fillOpacity` | number | varies | Transparency (0-1) |

**Axes:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `xAxisTitle`, `yAxisTitle` | string/boolean | - | Axis labels |
| `xGridlines`, `yGridlines` | boolean | varies | Show gridlines |
| `yMin`, `yMax` | number | - | Axis range |
| `yLog` | boolean | false | Logarithmic scale |

**Chart Display:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `title` | string | - | Chart title |
| `subtitle` | string | - | Chart subtitle |
| `legend` | boolean | true | Show legend |
| `chartAreaHeight` | number | 180 | Min height (px) |
| `downloadableData` | boolean | true | CSV download |
| `downloadableImage` | boolean | true | Image download |

**Interactivity:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `connectGroup` | string | - | Sync tooltips across charts |
| `echartsOptions` | object | - | Custom ECharts config |
| `seriesOptions` | object | - | Series-level config |

---

### LineChart

Display data as connected lines over a continuous axis.

#### Specific Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `y2` | string/array | - | Secondary y-axis |
| `y2SeriesType` | string | line | line, bar, scatter |
| `handleMissing` | string | gap | gap, connect, zero |
| `lineColor` | string | - | Line color |
| `lineOpacity` | number | 1 | Line transparency |
| `lineType` | string | solid | solid, dashed, dotted |
| `lineWidth` | number | 2 | Line thickness |
| `markers` | boolean | false | Show data points |
| `markerShape` | string | circle | Point shape |
| `markerSize` | number | 8 | Point size |
| `labels` | boolean | false | Show value labels |
| `labelPosition` | string | above | above, middle, below |
| `step` | boolean | false | Step line |
| `stepPosition` | string | middle | start, middle, end |

#### Examples

```svelte
<!-- Basic line chart -->
<LineChart
  data={sales_by_month}
  x=month
  y=sales
  title="Monthly Sales"
/>

<!-- Multi-series with markers -->
<LineChart
  data={sales_by_category}
  x=month
  y=sales
  series=category
  markers=true
  yFmt=usd0k
/>

<!-- With secondary axis -->
<LineChart
  data={sales_metrics}
  x=month
  y=revenue
  y2=growth_rate
  y2SeriesType=line
/>

<!-- Styled line -->
<LineChart
  data={trend}
  lineColor="#cf0d06"
  lineType="dashed"
  lineWidth=3
/>
```

---

### AreaChart

Display data as filled areas under lines.

#### Specific Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `type` | string | stacked | stacked, stacked100 |
| `handleMissing` | string | gap/zero | Missing value treatment |
| `fillOpacity` | number | 0.7 | Area transparency |
| `line` | boolean | true | Show line on area |

#### Examples

```svelte
<!-- Stacked area -->
<AreaChart
  data={revenue_by_category}
  x=month
  y=revenue
  series=category
/>

<!-- 100% stacked -->
<AreaChart
  data={market_share}
  x=quarter
  y=share
  series=company
  type=stacked100
/>
```

---

### BarChart

Display data as vertical or horizontal bars.

#### Specific Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `type` | string | stacked | stacked, grouped, stacked100 |
| `swapXY` | boolean | false | Horizontal bars |
| `fillColor` | string | - | Bar color |
| `outlineWidth` | number | 0 | Border width |
| `outlineColor` | string | - | Border color |
| `labels` | boolean | false | Value labels |
| `labelPosition` | string | varies | inside or outside |
| `stackTotalLabel` | boolean | true | Stacked totals |

#### Examples

```svelte
<!-- Vertical bar chart -->
<BarChart
  data={sales_by_category}
  x=category
  y=sales
  yFmt=usd0k
/>

<!-- Grouped bars -->
<BarChart
  data={quarterly_sales}
  x=quarter
  y=sales
  series=region
  type=grouped
/>

<!-- Horizontal bar chart -->
<BarChart
  data={top_products}
  x=product
  y=sales
  swapXY=true
/>

<!-- With labels -->
<BarChart
  data={sales}
  x=category
  y=amount
  labels=true
  labelPosition=outside
/>
```

---

### ScatterPlot

Display data as individual points.

#### Specific Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `tooltipTitle` | string | - | Point identifier in tooltip |
| `shape` | string | circle | circle, emptyCircle, rect, triangle, diamond |
| `pointSize` | number | 10 | Point size |
| `opacity` | number | 0.7 | Point transparency |
| `fillColor` | string | - | Point color |
| `outlineColor` | string | - | Border color |
| `outlineWidth` | number | 0 | Border width |

#### Examples

```svelte
<!-- Basic scatter plot -->
<ScatterPlot
  data={products}
  x=price
  y=sales
  tooltipTitle=name
/>

<!-- With series -->
<ScatterPlot
  data={products}
  x=price
  y=sales
  series=category
  shape=triangle
/>
```

---

### BubbleChart

Scatter plot with size dimension.

#### Specific Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `size` | string | required | Column for bubble size |
| `sizeFmt` | string | - | Size value format |
| `scaleTo` | number | - | Maximum bubble size |

#### Examples

```svelte
<BubbleChart
  data={market_data}
  x=gdp
  y=life_expectancy
  size=population
  series=continent
  tooltipTitle=country
/>
```

---

### Histogram

Display distribution of values.

#### Specific Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `x` | string | required | Column to summarize |
| `fillColor` | string | - | Bar color |
| `fillOpacity` | number | 1 | Transparency |

#### Examples

```svelte
<Histogram
  data={orders}
  x=order_value
  title="Order Value Distribution"
/>
```

---

### BoxPlot

Display statistical distribution with quartiles.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `name` | string | required | Box label column |
| `midpoint` | string | required | Median column |
| `intervalBottom` | string | - | Q1 column |
| `intervalTop` | string | - | Q3 column |
| `min`, `max` | string | - | Whisker columns |
| `confidenceInterval` | string | - | CI column |
| `swapXY` | boolean | false | Horizontal |

#### Examples

```svelte
<BoxPlot
  data={salary_stats}
  name=department
  midpoint=median_salary
  intervalBottom=q1
  intervalTop=q3
  min=min_salary
  max=max_salary
/>
```

---

### Heatmap

Display values in a grid with color intensity.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `x` | string | required | X-axis category |
| `y` | string | required | Y-axis category |
| `value` | string | required | Numeric column |
| `colorScale` | array | - | Gradient colors |
| `cellHeight` | number | 30 | Cell height (px) |
| `valueLabels` | boolean | true | Show values |
| `borders` | boolean | true | Cell borders |
| `nullsZero` | boolean | true | Treat nulls as zero |

#### Examples

```svelte
<Heatmap
  data={sales_matrix}
  x=day
  y=hour
  value=orders
  colorScale={['white', 'red']}
/>
```

---

### CalendarHeatmap

Display values on a calendar layout.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `date` | string | required | Date column |
| `value` | string | required | Numeric column |
| `colorScale` | array | - | Gradient colors |
| `yearLabel` | boolean | true | Show year |
| `monthLabel` | boolean | true | Show month |
| `dayLabel` | boolean | true | Show day |

#### Examples

```svelte
<CalendarHeatmap
  data={daily_commits}
  date=commit_date
  value=commit_count
  title="Git Activity"
/>
```

---

### FunnelChart

Display conversion funnel stages.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `nameCol` | string | required | Stage name column |
| `valueCol` | string | required | Stage value column |
| `labelPosition` | string | inside | left, right, inside |
| `showPercent` | boolean | false | Show percentages |
| `funnelSort` | string | none | none, ascending, descending |
| `funnelAlign` | string | center | left, right, center |

#### Examples

```svelte
<FunnelChart
  data={conversion_funnel}
  nameCol=stage
  valueCol=users
  showPercent=true
/>
```

---

### SankeyDiagram

Display flow between nodes.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `sourceCol` | string | required | Source node column |
| `targetCol` | string | required | Target node column |
| `valueCol` | string | required | Flow value column |
| `orient` | string | horizontal | horizontal, vertical |
| `nodeLabels` | string | full | name, value, full |
| `linkLabels` | string | - | full, value, percent |
| `linkColor` | string | base-content-muted | source, target, gradient |
| `nodeAlign` | string | justify | justify, left, right |

#### Examples

```svelte
<SankeyDiagram
  data={user_flow}
  sourceCol=source
  targetCol=target
  valueCol=users
  linkColor=gradient
/>
```

---

### Sparkline

Compact inline chart for single metrics.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `dateCol` | string | required | X-axis column |
| `valueCol` | string | required | Y-axis column |
| `type` | string | line | line, area, bar |
| `color` | string | - | Chart color |
| `height` | number | 15 | Height (px) |
| `width` | number | 50 | Width (px) |
| `yScale` | boolean | false | Truncate y-axis |
| `interactive` | boolean | true | Enable tooltips |

#### Examples

```svelte
Revenue trend: <Sparkline
  data={monthly_revenue}
  dateCol=month
  valueCol=revenue
  type=area
/>
```

---

### Mixed-Type Charts

Combine multiple chart types on same axes.

#### Usage

```svelte
<Chart data={metrics}>
  <Bar y=revenue />
  <Line y=growth name="Growth Rate" />
</Chart>
```

#### Available Primitives

- `<Bar />` - Bar series
- `<Line />` - Line series
- `<Area />` - Area series
- `<Scatter />` - Scatter points
- `<Bubble />` - Bubble series

---

### Custom ECharts

Access full ECharts feature set.

#### Props

| Prop | Type | Description |
|------|------|-------------|
| `config` | object | ECharts configuration object |

#### Examples

```svelte
<ECharts config={{
  title: { text: 'Custom Chart' },
  tooltip: { formatter: '{b}: {c}' },
  series: [{
    type: 'treemap',
    data: [...query_data]
  }]
}} />
```

---

### Annotations

Add context to charts with reference elements.

#### ReferenceLine

Draw lines on charts (targets, dates, regression).

| Prop | Type | Description |
|------|------|-------------|
| `x`, `y` | number/string | Line position |
| `x2`, `y2` | number/string | End position (sloped) |
| `label` | string | Line label |
| `data` | query | Data-driven lines |
| `color` | string | Line color |
| `lineType` | string | solid, dashed, dotted |
| `lineWidth` | number | Thickness (default: 1.3) |
| `labelPosition` | string | Position options |

```svelte
<LineChart data={sales} x=month y=revenue>
  <ReferenceLine y=10000 label="Target" color=positive />
  <ReferenceLine x='2024-01-01' label="Launch" />
</LineChart>
```

#### ReferenceArea

Highlight regions on charts.

| Prop | Type | Description |
|------|------|-------------|
| `xMin`, `xMax` | number/string | X-axis range |
| `yMin`, `yMax` | number/string | Y-axis range |
| `label` | string | Area label |
| `color` | string | Fill color |
| `opacity` | number | Transparency |
| `border` | boolean | Show border |

```svelte
<LineChart data={sales} x=month y=revenue>
  <ReferenceArea
    xMin='2024-03-01'
    xMax='2024-06-01'
    label="Q2"
    color=warning
  />
</LineChart>
```

#### ReferencePoint

Highlight specific points.

| Prop | Type | Description |
|------|------|-------------|
| `x`, `y` | number/string | Point coordinates |
| `label` | string | Point label |
| `symbol` | string | Point shape |
| `symbolSize` | number | Shape size |

```svelte
<LineChart data={sales} x=month y=revenue>
  <ReferencePoint
    x='2024-05-01'
    y=15000
    label="Record High"
  />
</LineChart>
```

#### Callout

Draw attention with descriptive labels.

```svelte
<LineChart data={sales} x=month y=revenue>
  <Callout x='2024-02-01' y=8000 labelPosition=bottom>
    Seasonal dip due to
    holiday period
  </Callout>
</LineChart>
```

---

## Map Components

### Common Map Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `title`, `subtitle` | string | - | Map labels |
| `height` | number | 300 | Height (px) |
| `legend` | boolean | true | Show legend |
| `legendPosition` | string | bottomLeft | Legend placement |
| `basemap` | string | - | Custom tile URL |
| `startingLat`, `startingLong` | number | - | Initial center |
| `startingZoom` | number | - | Initial zoom (1-18) |

---

### PointMap

Display points at lat/long coordinates.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `lat` | string | required | Latitude column |
| `long` | string | required | Longitude column |
| `pointName` | string | - | Point label column |
| `value` | string | - | Value column |
| `color` | string | - | Point color |
| `size` | number | 5 | Point size |
| `opacity` | number | - | Transparency |
| `colorPalette` | array | - | Gradient colors |
| `link` | string | - | Click URL column |
| `name` | string | - | Input name for selection |
| `tooltipType` | string | hover | hover or click |

#### Examples

```svelte
<PointMap
  data={store_locations}
  lat=latitude
  long=longitude
  pointName=store_name
  value=sales
  valueFmt=usd
  colorPalette={['blue', 'red']}
/>
```

---

### BubbleMap

Points with size dimension.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `lat`, `long` | string | required | Coordinates |
| `size` | string | required | Size column |
| `sizeFmt` | string | - | Size format |
| `maxSize` | number | 20 | Max bubble size |
| `value` | string | - | Color scale column |

#### Examples

```svelte
<BubbleMap
  data={cities}
  lat=lat
  long=lng
  size=population
  value=gdp_per_capita
  pointName=city_name
/>
```

---

### AreaMap

Choropleth map with GeoJSON regions.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `geoJsonUrl` | string | required | GeoJSON source |
| `areaCol` | string | required | Data area column |
| `geoId` | string | required | GeoJSON ID property |
| `value` | string | - | Color value column |
| `color` | string | - | Uniform color |
| `colorPalette` | array | - | Color gradient |
| `borderWidth` | number | 0.75 | Border thickness |
| `opacity` | number | 0.8 | Fill transparency |

#### Examples

```svelte
<AreaMap
  data={state_sales}
  geoJsonUrl='https://example.com/states.geojson'
  geoId=STATE_ID
  areaCol=state_code
  value=total_sales
  valueFmt=usd
/>
```

---

### USMap

Simplified US state choropleth.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `state` | string | required | State name column |
| `value` | string | required | Value column |
| `abbreviations` | boolean | false | Use state codes |
| `colorScale` | string | info | info, positive, negative |
| `colorPalette` | array | - | Custom colors |
| `filter` | boolean | false | Filterable legend |

#### Examples

```svelte
<USMap
  data={state_population}
  state=state_name
  value=population
  colorScale=positive
  fmt=num0
/>
```

---

## Input Components

### Dropdown

Single or multi-select dropdown menu.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `name` | string | required | Input identifier |
| `data` | query | required | Options query |
| `value` | string | required | Value column |
| `label` | string | value | Display column |
| `multiple` | boolean | false | Multi-select |
| `defaultValue` | string/array | - | Initial selection |
| `selectAllByDefault` | boolean | false | Select all initially |
| `noDefault` | boolean | false | No default selection |
| `title` | string | - | Dropdown label |
| `order` | string | - | Sort column |
| `where` | string | - | Filter clause |
| `disableSelectAll` | boolean | false | Hide select all |

#### Examples

```svelte
<!-- Single select -->
<Dropdown
  name=category
  data={categories}
  value=category_id
  label=category_name
  title="Select Category"
/>

<!-- Multi-select -->
<Dropdown
  name=regions
  data={regions}
  value=region_code
  multiple=true
  defaultValue={['US', 'CA']}
/>

<!-- Use in query -->
```sql
SELECT * FROM orders
WHERE category_id = '${inputs.category.value}'
```

#### DropdownOption

Hardcoded options:

```svelte
<Dropdown name=status>
  <DropdownOption value="active" valueLabel="Active" />
  <DropdownOption value="inactive" valueLabel="Inactive" />
</Dropdown>
```

---

### ButtonGroup

Toggle button selection.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `name` | string | required | Input identifier |
| `data` | query | - | Options query |
| `value` | string | - | Value column |
| `label` | string | value | Display column |
| `defaultValue` | string | - | Initial selection |
| `display` | string | buttons | buttons or tabs |
| `title` | string | - | Group label |
| `order` | string | - | Sort column |
| `where` | string | - | Filter clause |

#### Examples

```svelte
<ButtonGroup
  name=time_period
  data={periods}
  value=period_id
  label=period_name
  defaultValue="monthly"
/>

<!-- Tab style -->
<ButtonGroup
  name=view
  display=tabs
>
  <ButtonGroupItem value="chart" valueLabel="Chart View" default />
  <ButtonGroupItem value="table" valueLabel="Table View" />
</ButtonGroup>
```

---

### Slider

Numeric range slider.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `name` | string | required | Input identifier |
| `defaultValue` | number | - | Initial value |
| `min` | number | 0 | Minimum |
| `max` | number | 100 | Maximum |
| `step` | number | 1 | Increment |
| `size` | string | small | small, medium, large, full |
| `fmt` | string | - | Value format |
| `showMinMax` | boolean | true | Show range markers |
| `showInput` | boolean | false | Show input field |
| `data` | query | - | Data-driven range |
| `range` | string | - | Column for auto min/max |

#### Examples

```svelte
<Slider
  name=price_filter
  title="Max Price"
  min=0
  max=1000
  step=50
  fmt=usd0
/>

<!-- Data-driven range -->
<Slider
  name=date_slider
  data={orders}
  range=order_value
  size=large
/>
```

---

### DateInput / DateRange

Date selection with presets.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `name` | string | required | Input identifier |
| `data` | query | - | Date data source |
| `dates` | string | - | Date column |
| `range` | boolean | false | Enable range selection |
| `start`, `end` | string | - | Default dates (YYYY-MM-DD) |
| `title` | string | - | Component label |
| `presetRanges` | array | - | Available presets |
| `defaultValue` | string | - | Initial preset |

**Available Presets:**
- `'Last 7 Days'`, `'Last 30 Days'`, `'Last 90 Days'`
- `'Last 3 Months'`, `'Last 6 Months'`, `'Last 12 Months'`
- `'Last Month'`, `'Last Year'`
- `'Month to Date'`, `'Year to Date'`, `'All Time'`

#### Examples

```svelte
<!-- Date range picker -->
<DateRange
  name=date_filter
  data={orders}
  dates=order_date
  title="Order Date"
  presetRanges={['Last 30 Days', 'Last 90 Days', 'Year to Date']}
  defaultValue='Last 30 Days'
/>

<!-- Reference in query -->
```sql
SELECT * FROM orders
WHERE order_date BETWEEN '${inputs.date_filter.start}'
  AND '${inputs.date_filter.end}'
```

---

### TextInput

Free text entry.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `name` | string | required | Input identifier |
| `title` | string | - | Input label |
| `placeholder` | string | "Type to search" | Placeholder text |
| `defaultValue` | string | - | Initial value |

#### Examples

```svelte
<TextInput
  name=search
  title="Search Products"
  placeholder="Enter product name"
/>

<!-- Fuzzy search -->
{inputs.search.search('product_name')}
```

---

### Checkbox

Boolean toggle.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `name` | string | required | Input identifier |
| `title` | string | - | Checkbox label |
| `checked` | boolean | false | Initial state |

#### Examples

```svelte
<Checkbox
  name=include_inactive
  title="Include Inactive Products"
/>

<!-- Use in query -->
```sql
SELECT * FROM products
WHERE is_active = true
  OR ${inputs.include_inactive.value} = true
```

---

### DimensionGrid

Multi-dimensional selector.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `data` | query | required | Data source |
| `metric` | string | - | Aggregation expression |
| `name` | string | - | Input identifier |
| `title`, `subtitle` | string | - | Labels |
| `metricLabel` | string | - | Metric column label |
| `fmt` | string | - | Value format |
| `limit` | number | - | Rows per dimension |
| `multiple` | boolean | false | Multi-select |

#### Examples

```svelte
<DimensionGrid
  data={orders}
  metric='sum(sales)'
  name=dimension_filter
  multiple=true
/>
```

---

## UI Components

### Grid

Layout components in columns.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `cols` | number | 2 | Columns (1-6) |
| `gapSize` | string | md | none, sm, md, lg |

#### Examples

```svelte
<Grid cols=3>
  <BigValue data={metrics} value=revenue />
  <BigValue data={metrics} value=orders />
  <BigValue data={metrics} value=customers />
</Grid>

<!-- Group items in single cell -->
<Grid cols=2>
  <LineChart data={trend} />
  <Group>
    <BarChart data={breakdown} />
    <DataTable data={details} />
  </Group>
</Grid>
```

---

### Tabs

Tabbed content sections.

#### Tabs Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `id` | string | - | URL-shareable tab state |
| `color` | string | base-content | Tab indicator color |
| `fullWidth` | boolean | false | Full width tabs |
| `background` | boolean | false | Background on active |

#### Tab Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `label` | string | required | Tab label |
| `id` | string | - | Tab identifier |
| `printShowAll` | boolean | true | Show in print |

#### Examples

```svelte
<Tabs id="analysis-tabs">
  <Tab label="Overview">
    <LineChart data={overview} />
  </Tab>
  <Tab label="Details">
    <DataTable data={details} />
  </Tab>
</Tabs>
```

---

### Modal

Popup dialog with content.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `buttonText` | string | required | Trigger button text |
| `title` | string | - | Modal title |
| `open` | boolean | false | Initially open |

#### Examples

```svelte
<Modal buttonText="View Details" title="Sales Details">
  <DataTable data={sales_details} />
</Modal>
```

---

### Accordion

Collapsible content sections.

#### Accordion Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `single` | boolean | false | Only one open |
| `class` | string | - | Tailwind classes |

#### AccordionItem Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `title` | string | required | Item header |
| `class` | string | - | Tailwind classes |

#### Examples

```svelte
<Accordion single>
  <AccordionItem title="Revenue Analysis">
    <LineChart data={revenue} />
  </AccordionItem>
  <AccordionItem title="Cost Breakdown">
    <BarChart data={costs} />
  </AccordionItem>
</Accordion>
```

---

### Details

Collapsible section with disclosure.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `title` | string | "Details" | Section label |
| `open` | boolean | false | Initially expanded |
| `printShowAll` | boolean | true | Expand in print |

#### Examples

```svelte
<Details title="Methodology">
  This analysis uses...
</Details>
```

---

### Alert

Styled message container.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `status` | string | - | info, positive, warning, negative |

#### Examples

```svelte
<Alert status="warning">
  Data is 24 hours delayed.
</Alert>

<Alert status="positive">
  Report updated successfully!
</Alert>
```

---

### LinkButton / BigLink

Navigation buttons and links.

#### Props

| Prop | Type | Description |
|------|------|-------------|
| `url` | string | Navigation destination |

#### Examples

```svelte
<LinkButton url="/reports/sales">
  View Sales Report
</LinkButton>

<BigLink url="/dashboard">
  Go to Dashboard
</BigLink>
```

---

### LastRefreshed

Display data freshness timestamp.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `prefix` | string | "Last refreshed" | Label text |
| `printShowDate` | boolean | true | Full date in print |
| `dateFmt` | string | - | Date format |

#### Examples

```svelte
<LastRefreshed prefix="Data updated" />
```

---

### DownloadData

CSV download button.

#### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `data` | query | required | Data to download |
| `text` | string | "Download" | Button text |
| `queryID` | string | "evidence_download" | Filename prefix |

#### Examples

```svelte
<DownloadData
  data={sales_report}
  text="Export CSV"
  queryID=sales_export
/>
```

---

## Custom Components

### Creating Custom Components

Store Svelte components in `/components/` directory.

#### Basic Structure

**`/components/SalesCard.svelte`:**

```svelte
<script>
  export let data;
  export let title = "Sales";
  import { BarChart, Value } from '@evidence-dev/core-components';
</script>

<div class="card">
  <h3>{title}</h3>
  <Value data={data} column=total fmt=usd />
  <BarChart data={data} x=category y=amount />
</div>

<style>
  .card { padding: 1rem; border: 1px solid #e5e7eb; }
</style>
```

**Usage in markdown:**

```svelte
<SalesCard data={sales_by_category} title="Q4 Sales" />
```

### Utility Functions

Evidence provides helper functions:

- `checkInputs` - Validate data and columns
- `ErrorChart` - Display error states
- `getDistinctValues` - Extract unique values
- `formatValue` - Apply formatting
- `getSortedData` - Sort datasets

### Component Plugins

Publish reusable components as npm packages for use across projects.

---

## Formatting Options

### Built-in Formats

**Dates:**
- `ddd` - Mon, Tue...
- `dddd` - Monday, Tuesday...
- `mmm` - Jan, Feb...
- `mmmm` - January, February...
- `yyyy` - 2024
- `shortdate` - 1/1/24
- `longdate` - January 1, 2024
- `fulldate` - Monday, January 1, 2024
- `mdy`, `dmy` - Date order variants
- `hms` - Time with seconds

**Currencies:**
- `usd`, `usd0`, `usd1`, `usd2` - US Dollar
- `eur`, `gbp`, `jpy`, `cny`, etc. - Other currencies
- Add `k`, `m`, `b` suffix for thousands/millions/billions
  - `usd0k` - $1K, `usd1m` - $1.5M

**Numbers:**
- `num0` through `num4` - Decimal places
- `num0k`, `num1m`, `num2b` - With magnitude
- `id` - No formatting
- `fract` - Fraction format
- `mult` - Multiplier format
- `sci` - Scientific notation

**Percentages:**
- `pct` - Auto decimals
- `pct0`, `pct1`, `pct2`, `pct3` - Fixed decimals

### Format Usage

**In components:**
```svelte
<Value data={sales} column=revenue fmt=usd0k />
<LineChart data={growth} yFmt=pct1 />
```

**SQL format tags:**
```sql
SELECT
  revenue AS revenue_usd,
  growth AS growth_pct
FROM sales
```

**Format function:**
```markdown
Revenue: {fmt(data[0].revenue, 'usd0k')}
```

### Custom Formats

Define in Evidence settings using Excel-style codes:
```
#,##0.00    - 1,234.56
0.00%       - 12.34%
$#,##0      - $1,234
```

---

## Theming & Styling

### Color Configuration

Define in `evidence.config.yaml`:

```yaml
appearance:
  default: system  # light, dark, system

colors:
  primary: '#3b82f6'
  secondary: '#6b7280'

colorPalettes:
  default: ['#3b82f6', '#ef4444', '#10b981', '#f59e0b']
  custom:
    light: ['#bfdbfe', '#93c5fd', '#60a5fa']
    dark: ['#1e40af', '#1d4ed8', '#2563eb']
```

### Using Colors

**In components:**
```svelte
<LineChart
  data={sales}
  colorPalette={['#cf0d06', '#eb5752', '#e88a87']}
/>

<BarChart
  data={sales}
  seriesColors={{'North': '#3b82f6', 'South': '#ef4444'}}
/>
```

### Tailwind CSS

Evidence uses Tailwind for styling:

```svelte
<div class="p-4 bg-gray-100 rounded-lg">
  <h2 class="text-xl font-bold text-gray-800">Title</h2>
</div>
```

Apply markdown styling to HTML:
```svelte
<h1 class="markdown">Styled Heading</h1>
```

### Layout Customization

Create `/pages/+layout.svelte`:

```svelte
<script>
  import { EvidenceDefaultLayout } from '@evidence-dev/core-components';
</script>

<EvidenceDefaultLayout
  title="My App"
  logo="/logo.png"
  fullWidth={false}
  maxWidth={1400}
  hideSidebar={false}
  hideHeader={false}
/>
```

---

## Advanced Patterns

### Interactive Filtering

```svelte
<!-- Input -->
<Dropdown
  name=category
  data={categories}
  value=id
  label=name
/>

<!-- Filtered query -->
```sql filtered_sales
SELECT * FROM sales
WHERE category_id = '${inputs.category.value}'
```

<!-- Chart updates automatically -->
<LineChart data={filtered_sales} x=date y=amount />
```

### Synchronized Charts

```svelte
<Grid cols=2>
  <LineChart
    data={revenue}
    connectGroup="sales"
  />
  <BarChart
    data={orders}
    connectGroup="sales"
  />
</Grid>
```

### Conditional Rendering

```svelte
{#if sales[0].total > 100000}
  <Alert status="positive">
    Sales target exceeded!
  </Alert>
{:else}
  <Alert status="warning">
    Below target
  </Alert>
{/if}
```

### Dynamic Components

```svelte
{#each regions as region}
  <BigValue
    data={region}
    value=sales
    title={region.name}
  />
{/each}
```

### Parameterized Pages

Create `/pages/products/[product_id].md`:

```sql product_details
SELECT * FROM products
WHERE id = '${params.product_id}'
```

### Component Composition

```svelte
<Grid cols=3>
  <Group>
    <BigValue data={kpis} value=revenue title="Revenue" />
    <Sparkline data={trend} dateCol=date valueCol=revenue />
  </Group>

  <BigValue data={kpis} value=orders title="Orders" />

  <BigValue
    data={kpis}
    value=customers
    comparison=customer_growth
    comparisonDelta=true
  />
</Grid>

<Tabs>
  <Tab label="Charts">
    <LineChart data={monthly} x=month y=revenue />
  </Tab>
  <Tab label="Data">
    <DataTable data={monthly} search=true />
  </Tab>
</Tabs>
```

---

## Quick Reference

### Most Common Components

| Component | Primary Use |
|-----------|-------------|
| `<Value>` | Inline formatted value |
| `<BigValue>` | KPI display |
| `<DataTable>` | Data grid |
| `<LineChart>` | Time series |
| `<BarChart>` | Comparisons |
| `<Dropdown>` | Selection filter |
| `<Grid>` | Layout |
| `<Tabs>` | Content organization |

### Data Binding Syntax

| Pattern | Syntax |
|---------|--------|
| Query result | `data={query_name}` |
| Input value | `${inputs.name.value}` |
| URL parameter | `${params.name}` |
| JavaScript | `{expression}` |
| Loop | `{#each data as item}...{/each}` |
| Conditional | `{#if condition}...{:else}...{/if}` |

### Format Shortcuts

| Format | Example Output |
|--------|----------------|
| `usd0` | $1,234 |
| `usd1k` | $1.2K |
| `pct1` | 12.3% |
| `num0` | 1,234 |
| `shortdate` | 1/15/24 |

---

## Resources

- **Documentation**: https://docs.evidence.dev/
- **Components**: https://docs.evidence.dev/components/all-components
- **GitHub**: https://github.com/evidence-dev/evidence
- **Examples**: https://evidence.dev/examples
