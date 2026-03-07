# Geospatial Linguistics

This directory contains research on mapping Irish language areas, schools, and demographic data using modern geospatial tools.

## Overview

Visualizing the linguistic landscape of Celtic language communities requires combining census data, administrative boundaries, and educational infrastructure. This research focuses on building a proof-of-concept map for Gaeltacht areas and Irish-medium schools using DuckDB, dlt, and MapLibre.

### Primary Use Cases

1. **Gaeltacht Mapping** - Visualize official Irish-speaking areas
2. **School Distribution** - Map Gaelscoileanna locations
3. **Census Analysis** - Analyze speaker demographics by area
4. **Cross-Border Comparison** - Compare ROI and NI data

## Documents in this Category

| Document | Focus | Key Topics |
|----------|-------|------------|
| `duckdb-spatial.md` | Geospatial analysis | DuckDB spatial extension, queries |
| `maplibre-visualization.md` | Web mapping | MapLibre GL JS, vector tiles |
| `data-sources.md` | Official datasets | Census, boundaries, schools |

## Data Sources Overview

### Republic of Ireland

| Data Type | Source | Format | Level |
|-----------|--------|--------|-------|
| **Gaeltacht Boundaries** | Tailte Éireann | GeoJSON, Shapefile | Electoral Division |
| **Language Planning Areas** | Tailte Éireann | GeoJSON, Shapefile | LPA |
| **Census Data** | CSO (PxStat) | CSV, XLSX | Small Area |
| **School Data** | gov.ie, Gaeloideachas | Excel | Individual |

### Northern Ireland

| Data Type | Source | Format | Level |
|-----------|--------|--------|-------|
| **Data Zones** | NISRA | GeoJSON, Shapefile | DZ2021 |
| **Census Data** | NISRA | CSV | Data Zone |
| **School Data** | DE NI, CnaG | Excel | Individual |

## Technical Stack

```yaml
Data Ingestion: dltHub
Storage: DuckDB with spatial extension
Processing: Ibis, Python
Visualization: MapLibre GL JS
Tile Generation: tippecanoe
```

## Key Metrics

### Irish Language Indicators

| Metric | ROI Census 2022 | NI Census 2021 |
|--------|-----------------|----------------|
| **Can Speak Irish** | 1.87M (40%) | 228,617 (12.45%) |
| **Daily Speakers** | 71,968 | 43,557 |
| **Gaeltacht Pop** | 106,220 | N/A |
| **Gaeltacht Speakers** | 65,156 (66%) | N/A |

### Schools

| Type | ROI | NI |
|------|-----|-----|
| **Primary Irish-medium** | 256 total | ~30 standalone |
| **Post-primary Irish-medium** | ~75 | 10 units |

## Architecture

```
+------------------+     +------------------+
|   Census Data    |     |   Boundary Data  |
|   (CSV/Excel)    |     |   (GeoJSON)      |
+--------+---------+     +--------+---------+
         |                        |
         v                        v
+------------------------------------------+
|              dltHub Pipeline             |
|   - Normalize encoding                   |
|   - Geocode addresses                    |
|   - Join census to boundaries            |
+------------------------------------------+
         |
         v
+------------------------------------------+
|           DuckDB + Spatial               |
|   - Spatial joins                        |
|   - Aggregations                         |
|   - Choropleth calculations              |
+------------------------------------------+
         |
         v
+------------------------------------------+
|         MapLibre GL JS                   |
|   - Vector tiles (MVT)                   |
|   - Interactive layers                   |
|   - Data-driven styling                  |
+------------------------------------------+
```

## Geographic Levels

### Republic of Ireland

| Level | Count | Use Case |
|-------|-------|----------|
| **Small Areas** | ~18,000 | Census data |
| **Electoral Divisions** | 3,409 | Gaeltacht boundaries |
| **Counties** | 31 | Regional analysis |

### Northern Ireland

| Level | Count | Use Case |
|-------|-------|----------|
| **Data Zones (DZ2021)** | 3,780 | Census data |
| **Super Data Zones** | 890 | Aggregation |
| **Council Areas** | 11 | Regional analysis |

## Cross-Border Considerations

| Challenge | Mitigation |
|-----------|------------|
| Different census years | Compare trends, not absolutes |
| Different geographies | Aggregate to comparable levels |
| Different questions | Focus on common metrics (ability, frequency) |
| No NI Gaeltacht | Define by speaker concentration |

## Key Data Downloads

### ROI Boundaries

- **Gaeltacht Areas**: https://data-osi.opendata.arcgis.com/datasets/osi::gaeltacht-areas-national-administrative-boundaries-ungeneralised-2024
- **Language Planning Areas**: https://data-osi.opendata.arcgis.com/datasets/osi::gaeltacht-language-planning-areas-national-administrative-boundaries-ungen-2024

### NI Boundaries

- **Data Zones (GeoJSON)**: https://www.nisra.gov.uk/files/nisra/publications/geography-dz2021-geojson.zip
- **Data Zones (Shapefile)**: https://www.nisra.gov.uk/files/nisra/publications/geography-dz2021-esri-shapefile.zip

## Cross-References

- **Category 02 (Data Acquisition)** - Collection pipelines
- **Category 05 (Education Policy)** - School enrollment context
- Main research Category 03 (AI-Native Data Pipelines) - dlt patterns
- Main research Category 04 (Stealth Browser Stack) - Web scraping
