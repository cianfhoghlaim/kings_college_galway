# Geospatial Linguistics: Celtic Language Mapping

A comprehensive guide to mapping Irish language areas, schools, and demographic data using DuckDB Spatial, MapLibre GL JS, and modern geospatial tools.

---

## Table of Contents

1. [Data Sources](#1-data-sources)
2. [DuckDB Spatial Queries](#2-duckdb-spatial-queries)
3. [MapLibre Visualization](#3-maplibre-visualization)
4. [Cross-border Harmonization](#4-cross-border-harmonization)

---

## 1. Data Sources

### Overview

This section provides detailed information on official data sources for Gaeltacht boundaries, census statistics, and school locations in the Republic of Ireland and Northern Ireland.

### 1.1 Republic of Ireland - Boundaries

#### 1.1.1 Gaeltacht Areas

Official Gaeltacht regions defined by the Gaeltacht Area Orders (1956, 1967, 1974, 1982).

| Property | Value |
|----------|-------|
| **Source** | Tailte Eireann |
| **Dataset** | Gaeltacht Areas - National Administrative Boundaries - Ungeneralised - 2024 |
| **Portal** | data.gov.ie |
| **Formats** | GeoJSON, Shapefile, CSV, KML |
| **Level** | Electoral Division (parts of) |
| **Coverage** | 155 Electoral Divisions (or parts) |
| **Counties** | Cork, Donegal, Galway, Kerry, Mayo, Meath, Waterford |

**Download URL:**
https://data-osi.opendata.arcgis.com/datasets/osi::gaeltacht-areas-national-administrative-boundaries-ungeneralised-2024

#### 1.1.2 Language Planning Areas (LPAs)

Areas designated under the Gaeltacht Act 2012 for language planning.

| Property | Value |
|----------|-------|
| **Source** | Tailte Eireann |
| **Dataset** | Gaeltacht Language Planning Areas - Ungeneralised - 2024 |
| **Portal** | data.gov.ie |
| **Formats** | GeoJSON, Shapefile, CSV, KML |
| **Count** | 26 LPAs |

**Download URL:**
https://data-osi.opendata.arcgis.com/datasets/osi::gaeltacht-language-planning-areas-national-administrative-boundaries-ungen-2024

#### 1.1.3 Small Area Boundaries

Census geography for detailed population analysis.

| Property | Value |
|----------|-------|
| **Source** | CSO / Tailte Eireann |
| **Count** | ~18,000 Small Areas |
| **Portal** | data.gov.ie / PxStat |
| **Formats** | GeoJSON, Shapefile |

### 1.2 Republic of Ireland - Census Data

#### Census 2022 - Irish Language

**Source:** Central Statistics Office (CSO)
**Portal:** https://data.cso.ie (PxStat)

**Key Statistics:**

| Metric | Value | Change from 2016 |
|--------|-------|------------------|
| Can speak Irish | 1,873,997 (40%) | +112,500 |
| Daily speakers (outside education) | 71,968 | -1,835 |
| Weekly speakers | 115,065 | - |
| Within education only | 553,965 | - |
| Never speak | ~473,000 | - |

**Proficiency Levels:**

| Level | Count | Percentage |
|-------|-------|------------|
| Very well | 195,029 | 10% |
| Well | 593,898 | 32% |
| Not well | 1,034,132 | 55% |

**Gaeltacht Specific:**

| Metric | Value |
|--------|-------|
| Total population | 106,220 |
| Irish speakers | 65,156 (66%) |
| Daily speakers | 20,000+ |

**Key Tables (PxStat):**

| Table ID | Content |
|----------|---------|
| F8014 | Irish speakers by frequency, Gaeltacht area |
| E8014 | Ability to speak Irish by area |
| F8015 | Irish speakers by proficiency |

### 1.3 Republic of Ireland - School Data

#### Department of Education

**Portal:** https://www.gov.ie/en/service/find-a-school/

**Data Available:**
- School Roll Number
- Address
- Eircode
- Phone/Email
- Enrollment figures

**Format:** Excel spreadsheets

#### Gaeloideachas

**Portal:** https://gaeloideachas.ie/directories/

**Lists Available (June 2023):**

| List | Content | Format |
|------|---------|--------|
| Primary Schools | Bunscoileanna 32 counties | Excel |
| Post-Primary Schools | Iar-bhunscoileanna 32 counties | Excel |
| Units (Aonaid) | Irish-medium units | Excel |

**Key Fields:**
- School name
- County
- Irish-medium status (explicit)

### 1.4 Northern Ireland - Boundaries

#### Data Zones (DZ2021)

Primary small-area geography for Census 2021.

| Property | Value |
|----------|-------|
| **Source** | NISRA |
| **Count** | 3,780 Data Zones |
| **Formats** | GeoJSON, Shapefile, Geodatabase |

**Download URLs:**

| Format | URL |
|--------|-----|
| **GeoJSON** | https://www.nisra.gov.uk/files/nisra/publications/geography-dz2021-geojson.zip |
| **Shapefile** | https://www.nisra.gov.uk/files/nisra/publications/geography-dz2021-esri-shapefile.zip |

#### Geographic Hierarchy

| Level | Count | Notes |
|-------|-------|-------|
| Data Zones | 3,780 | Primary census unit |
| Super Data Zones | 890 | Aggregation level |
| District Electoral Areas | 80 | Electoral boundaries |
| Local Government Districts | 11 | Council areas |

### 1.5 Northern Ireland - Census Data

#### Census 2021 - Irish Language

**Source:** NISRA
**Portal:** https://build.nisra.gov.uk (Flexible Table Builder)

**Key Statistics:**

| Metric | Value | Percentage |
|--------|-------|------------|
| Some ability in Irish | 228,617 | 12.45% |
| Irish as main language | 5,969 | 0.32% |
| Daily speakers | 43,557 | 2.43% |

**Detailed Abilities:**

| Ability | Count | % of those with ability |
|---------|-------|------------------------|
| Understand only | 90,800 | 39.7% |
| Understand, speak, read, write | 71,900 | 31.4% |

### 1.6 Northern Ireland - School Data

#### Department of Education NI

**Portal:** https://www.education-ni.gov.uk

**Irish-Medium Schools List:**
https://www.education-ni.gov.uk/articles/irish-medium-schools

**Content:**
- 30 standalone Irish-medium schools
- 10 Irish-medium units
- 46 nurseries (via CnaG)

#### Comhairle na Gaelscolaiochta (CnaG)

**Portal:** https://www.comhairle.org

Authoritative source for Irish-medium education in NI.

### 1.7 Master Data Source Summary

| Data Type | ROI Source | ROI Format | NI Source | NI Format |
|-----------|------------|------------|-----------|-----------|
| **Gaeltacht Boundaries** | Tailte Eireann | GeoJSON | N/A | Define from census |
| **Census - Language** | CSO PxStat | CSV | NISRA Builder | CSV |
| **Small Area Boundaries** | Tailte Eireann | GeoJSON | NISRA | GeoJSON |
| **Schools** | gov.ie + Gaeloideachas | Excel | DE NI + CnaG | Excel |

---

## 2. DuckDB Spatial Queries

### 2.1 Setup

#### Installation

```python
import duckdb

# Create connection and install spatial
conn = duckdb.connect("celtic_geo.duckdb")
conn.execute("INSTALL spatial; LOAD spatial;")
```

#### Verify Installation

```sql
-- Check spatial functions available
SELECT * FROM duckdb_functions() WHERE function_name LIKE 'ST_%' LIMIT 10;
```

### 2.2 Loading Geospatial Data

#### GeoJSON Files

```sql
-- Load Gaeltacht boundaries from GeoJSON
CREATE TABLE gaeltacht_areas AS
SELECT * FROM ST_Read('/path/to/gaeltacht_areas.geojson');

-- Load NI Data Zones
CREATE TABLE ni_data_zones AS
SELECT * FROM ST_Read('/path/to/dz2021.geojson');
```

#### Shapefiles

```sql
-- Load from Shapefile
CREATE TABLE language_planning_areas AS
SELECT * FROM ST_Read('/path/to/lpa_boundaries.shp');
```

#### CSV with Coordinates

```sql
-- Load schools with lat/lng columns
CREATE TABLE schools AS
SELECT
    school_name,
    roll_number,
    eircode,
    ST_Point(longitude, latitude) AS geom
FROM read_csv('/path/to/schools.csv');
```

### 2.3 Core Spatial Operations

#### Point in Polygon (Schools in Gaeltacht)

```sql
-- Find schools within Gaeltacht areas
SELECT
    s.school_name,
    s.roll_number,
    g.area_name AS gaeltacht_name
FROM schools s
JOIN gaeltacht_areas g
ON ST_Within(s.geom, g.geom);
```

#### Spatial Join (Census to Boundaries)

```sql
-- Join census data to Gaeltacht boundaries
SELECT
    g.area_name,
    SUM(c.irish_speakers) AS total_speakers,
    SUM(c.population) AS total_population,
    ROUND(100.0 * SUM(c.irish_speakers) / SUM(c.population), 2) AS speaker_pct
FROM gaeltacht_areas g
JOIN census_small_areas c
ON ST_Intersects(g.geom, c.geom)
GROUP BY g.area_name
ORDER BY speaker_pct DESC;
```

#### Buffer Analysis

```sql
-- Find schools within 5km of Gaeltacht boundaries
SELECT
    s.school_name,
    ST_Distance(s.geom, g.geom) / 1000 AS distance_km
FROM schools s, gaeltacht_areas g
WHERE ST_DWithin(s.geom, ST_Buffer(g.geom, 5000), 0)
ORDER BY distance_km;
```

#### Area Calculations

```sql
-- Calculate area of each Gaeltacht region
SELECT
    area_name,
    ROUND(ST_Area(geom) / 1000000, 2) AS area_km2
FROM gaeltacht_areas
ORDER BY area_km2 DESC;
```

### 2.4 Census Data Analysis

#### Speaker Concentration Mapping

```sql
-- Calculate speaker percentage by Small Area
CREATE TABLE speaker_choropleth AS
SELECT
    sa.sa_code,
    sa.geom,
    c.can_speak_irish,
    c.daily_speakers,
    c.population,
    ROUND(100.0 * c.can_speak_irish / NULLIF(c.population, 0), 2) AS ability_pct,
    ROUND(100.0 * c.daily_speakers / NULLIF(c.population, 0), 2) AS daily_pct
FROM small_area_boundaries sa
JOIN census_language c ON sa.sa_code = c.sa_code;
```

#### Gaeltacht vs Non-Gaeltacht Comparison

```sql
-- Compare speaker rates inside vs outside Gaeltacht
WITH classified AS (
    SELECT
        c.*,
        CASE WHEN g.area_name IS NOT NULL THEN 'Gaeltacht' ELSE 'Non-Gaeltacht' END AS area_type
    FROM census_small_areas c
    LEFT JOIN gaeltacht_areas g ON ST_Within(c.geom, g.geom)
)
SELECT
    area_type,
    SUM(population) AS total_pop,
    SUM(irish_speakers) AS total_speakers,
    ROUND(100.0 * SUM(irish_speakers) / SUM(population), 2) AS speaker_pct,
    SUM(daily_speakers) AS total_daily,
    ROUND(100.0 * SUM(daily_speakers) / SUM(population), 2) AS daily_pct
FROM classified
GROUP BY area_type;
```

#### County-Level Aggregation

```sql
-- Aggregate to county level
SELECT
    county,
    SUM(population) AS pop,
    SUM(irish_speakers) AS speakers,
    ROUND(100.0 * SUM(irish_speakers) / SUM(population), 2) AS pct,
    COUNT(*) AS num_areas
FROM census_small_areas
GROUP BY county
ORDER BY pct DESC;
```

### 2.5 School Analysis

#### School Density by Area

```sql
-- Count Irish-medium schools per county
SELECT
    county,
    COUNT(*) AS num_schools,
    SUM(enrollment) AS total_pupils
FROM irish_medium_schools
GROUP BY county
ORDER BY num_schools DESC;
```

#### Schools in Language Planning Areas

```sql
-- Identify schools in each LPA
SELECT
    lpa.lpa_name,
    COUNT(s.roll_number) AS num_schools,
    STRING_AGG(s.school_name, ', ') AS schools
FROM language_planning_areas lpa
LEFT JOIN irish_medium_schools s
ON ST_Within(s.geom, lpa.geom)
GROUP BY lpa.lpa_name
ORDER BY num_schools DESC;
```

#### Distance to Nearest School

```sql
-- Calculate distance to nearest Irish-medium school for each area
WITH nearest AS (
    SELECT
        sa.sa_code,
        MIN(ST_Distance(sa.geom, s.geom)) AS min_distance
    FROM small_area_boundaries sa
    CROSS JOIN irish_medium_schools s
    GROUP BY sa.sa_code
)
SELECT
    sa_code,
    min_distance / 1000 AS nearest_school_km
FROM nearest
ORDER BY min_distance DESC
LIMIT 20;
```

### 2.6 Cross-Border Analysis

#### Unified View

```sql
-- Create unified view of speaker data
CREATE VIEW all_ireland_speakers AS
SELECT
    'ROI' AS jurisdiction,
    sa_code AS area_code,
    geom,
    population,
    irish_speakers,
    daily_speakers
FROM roi_census_small_areas

UNION ALL

SELECT
    'NI' AS jurisdiction,
    dz_code AS area_code,
    geom,
    population,
    irish_ability AS irish_speakers,
    daily_speakers
FROM ni_census_data_zones;
```

#### Border Region Analysis

```sql
-- Define border counties
WITH border_counties AS (
    SELECT * FROM counties
    WHERE county_name IN (
        'Donegal', 'Leitrim', 'Cavan', 'Monaghan', 'Louth',  -- ROI
        'Derry', 'Tyrone', 'Fermanagh', 'Armagh', 'Down'     -- NI
    )
)
SELECT
    bc.county_name,
    bc.jurisdiction,
    SUM(c.irish_speakers) AS speakers,
    SUM(c.population) AS population,
    ROUND(100.0 * SUM(c.irish_speakers) / SUM(c.population), 2) AS pct
FROM border_counties bc
JOIN all_ireland_speakers c ON ST_Within(c.geom, bc.geom)
GROUP BY bc.county_name, bc.jurisdiction
ORDER BY pct DESC;
```

### 2.7 Export for MapLibre

#### GeoJSON Export

```sql
-- Export choropleth data as GeoJSON
COPY (
    SELECT
        sa_code,
        ability_pct,
        daily_pct,
        ST_AsGeoJSON(geom) AS geometry
    FROM speaker_choropleth
) TO '/output/speakers.geojson'
WITH (FORMAT JSON);
```

#### Prepare for Vector Tiles

```sql
-- Simplify geometries for web display
CREATE TABLE web_gaeltacht AS
SELECT
    area_name,
    speaker_pct,
    ST_Simplify(geom, 100) AS geom  -- 100m tolerance
FROM gaeltacht_areas;

-- Export for tippecanoe
COPY web_gaeltacht TO '/output/gaeltacht.geojson'
WITH (FORMAT JSON);
```

#### Centroid Export (For Labels)

```sql
-- Generate centroids for labeling
SELECT
    area_name,
    ST_X(ST_Centroid(geom)) AS lng,
    ST_Y(ST_Centroid(geom)) AS lat
FROM gaeltacht_areas;
```

### 2.8 Complete Pipeline Example

```python
#!/usr/bin/env python3
"""
DuckDB Spatial Pipeline for Celtic Language Mapping
"""

import duckdb
from pathlib import Path

class CelticGeoPipeline:
    def __init__(self, db_path: str = ":memory:"):
        self.conn = duckdb.connect(db_path)
        self.conn.execute("INSTALL spatial; LOAD spatial;")

    def load_boundaries(self, geojson_path: str, table_name: str):
        """Load GeoJSON boundaries."""
        self.conn.execute(f"""
            CREATE TABLE {table_name} AS
            SELECT * FROM ST_Read('{geojson_path}')
        """)

    def load_census_csv(self, csv_path: str, table_name: str):
        """Load census data from CSV."""
        self.conn.execute(f"""
            CREATE TABLE {table_name} AS
            SELECT * FROM read_csv('{csv_path}')
        """)

    def join_census_to_boundaries(
        self,
        census_table: str,
        boundary_table: str,
        join_key: str
    ):
        """Spatial join census data to boundaries."""
        return self.conn.execute(f"""
            SELECT
                b.*,
                c.population,
                c.irish_speakers,
                c.daily_speakers,
                ROUND(100.0 * c.irish_speakers / NULLIF(c.population, 0), 2) AS pct
            FROM {boundary_table} b
            LEFT JOIN {census_table} c ON b.{join_key} = c.{join_key}
        """).fetchdf()

    def schools_in_areas(self, schools_table: str, areas_table: str):
        """Find schools within areas."""
        return self.conn.execute(f"""
            SELECT
                a.area_name,
                COUNT(s.*) AS num_schools
            FROM {areas_table} a
            LEFT JOIN {schools_table} s ON ST_Within(s.geom, a.geom)
            GROUP BY a.area_name
        """).fetchdf()

    def export_geojson(self, query: str, output_path: str):
        """Export query result as GeoJSON."""
        self.conn.execute(f"""
            COPY ({query}) TO '{output_path}'
            WITH (FORMAT JSON)
        """)

def main():
    pipeline = CelticGeoPipeline("celtic_geo.duckdb")

    # Load data
    pipeline.load_boundaries(
        "gaeltacht_areas.geojson",
        "gaeltacht"
    )

    # Analysis
    results = pipeline.schools_in_areas("schools", "gaeltacht")
    print(results)

    # Export
    pipeline.export_geojson(
        "SELECT * FROM gaeltacht",
        "output/gaeltacht.geojson"
    )

if __name__ == "__main__":
    main()
```

### 2.9 Performance Tips

| Operation | Tip |
|-----------|-----|
| **Large datasets** | Use `ST_Simplify()` for web export |
| **Spatial joins** | Create spatial index with `CREATE INDEX` |
| **Point-in-polygon** | Use `ST_DWithin()` for approximate queries |
| **Memory** | Use disk-based DB for >1GB data |

---

## 3. MapLibre Visualization

### 3.1 Basic Setup

#### HTML Template

```html
<!DOCTYPE html>
<html>
<head>
    <title>Celtic Language Map</title>
    <link href="https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.css" rel="stylesheet" />
    <script src="https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.js"></script>
    <style>
        body { margin: 0; padding: 0; }
        #map { position: absolute; top: 0; bottom: 0; width: 100%; }
        .legend {
            position: absolute;
            bottom: 30px;
            left: 10px;
            background: white;
            padding: 10px;
            border-radius: 4px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.3);
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <div class="legend" id="legend"></div>

    <script src="app.js"></script>
</body>
</html>
```

#### Initialize Map

```javascript
// app.js
const map = new maplibregl.Map({
    container: 'map',
    style: {
        version: 8,
        sources: {
            'osm': {
                type: 'raster',
                tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
                tileSize: 256,
                attribution: '(c) OpenStreetMap'
            }
        },
        layers: [{
            id: 'osm-tiles',
            type: 'raster',
            source: 'osm'
        }]
    },
    center: [-8.0, 53.5],  // Ireland center
    zoom: 6
});
```

### 3.2 Loading Data Sources

#### GeoJSON Source

```javascript
map.on('load', () => {
    // Add Gaeltacht boundaries
    map.addSource('gaeltacht', {
        type: 'geojson',
        data: '/data/gaeltacht_areas.geojson'
    });

    // Add schools
    map.addSource('schools', {
        type: 'geojson',
        data: '/data/irish_medium_schools.geojson'
    });

    // Add census choropleth
    map.addSource('census', {
        type: 'geojson',
        data: '/data/speaker_choropleth.geojson'
    });
});
```

#### Vector Tiles Source

```javascript
// For large datasets, use vector tiles
map.addSource('census-tiles', {
    type: 'vector',
    tiles: ['https://your-server.com/tiles/census/{z}/{x}/{y}.pbf'],
    minzoom: 0,
    maxzoom: 14
});
```

### 3.3 Layer Styling

#### Choropleth Layer (Speaker Percentage)

```javascript
map.addLayer({
    id: 'census-choropleth',
    type: 'fill',
    source: 'census',
    paint: {
        'fill-color': [
            'interpolate',
            ['linear'],
            ['get', 'speaker_pct'],
            0, '#f7fbff',
            10, '#c6dbef',
            20, '#9ecae1',
            40, '#6baed6',
            60, '#3182bd',
            80, '#08519c'
        ],
        'fill-opacity': 0.7
    }
});

// Add outline
map.addLayer({
    id: 'census-outline',
    type: 'line',
    source: 'census',
    paint: {
        'line-color': '#333',
        'line-width': 0.5
    }
});
```

#### Gaeltacht Boundaries

```javascript
map.addLayer({
    id: 'gaeltacht-fill',
    type: 'fill',
    source: 'gaeltacht',
    paint: {
        'fill-color': '#228B22',
        'fill-opacity': 0.3
    }
});

map.addLayer({
    id: 'gaeltacht-outline',
    type: 'line',
    source: 'gaeltacht',
    paint: {
        'line-color': '#228B22',
        'line-width': 2,
        'line-dasharray': [2, 2]
    }
});
```

#### School Points

```javascript
map.addLayer({
    id: 'schools-points',
    type: 'circle',
    source: 'schools',
    paint: {
        'circle-radius': [
            'interpolate',
            ['linear'],
            ['get', 'enrollment'],
            50, 4,
            200, 8,
            500, 12
        ],
        'circle-color': [
            'match',
            ['get', 'school_type'],
            'primary', '#4CAF50',
            'secondary', '#2196F3',
            '#9E9E9E'
        ],
        'circle-stroke-width': 1,
        'circle-stroke-color': '#fff'
    }
});
```

#### Labels

```javascript
map.addLayer({
    id: 'gaeltacht-labels',
    type: 'symbol',
    source: 'gaeltacht',
    layout: {
        'text-field': ['get', 'area_name'],
        'text-size': 12,
        'text-anchor': 'center'
    },
    paint: {
        'text-color': '#333',
        'text-halo-color': '#fff',
        'text-halo-width': 1
    }
});
```

### 3.4 Interactivity

#### Hover Effects

```javascript
// Highlight on hover
map.on('mousemove', 'census-choropleth', (e) => {
    map.getCanvas().style.cursor = 'pointer';

    if (e.features.length > 0) {
        const feature = e.features[0];

        // Update info panel
        document.getElementById('info').innerHTML = `
            <strong>${feature.properties.area_name}</strong><br>
            Population: ${feature.properties.population.toLocaleString()}<br>
            Speakers: ${feature.properties.speaker_pct}%<br>
            Daily: ${feature.properties.daily_pct}%
        `;
    }
});

map.on('mouseleave', 'census-choropleth', () => {
    map.getCanvas().style.cursor = '';
});
```

#### Click Popups

```javascript
map.on('click', 'schools-points', (e) => {
    const feature = e.features[0];
    const coordinates = feature.geometry.coordinates.slice();

    new maplibregl.Popup()
        .setLngLat(coordinates)
        .setHTML(`
            <h3>${feature.properties.school_name}</h3>
            <p>
                <strong>Type:</strong> ${feature.properties.school_type}<br>
                <strong>Enrollment:</strong> ${feature.properties.enrollment}<br>
                <strong>Address:</strong> ${feature.properties.address}
            </p>
        `)
        .addTo(map);
});
```

#### Layer Toggle

```javascript
function toggleLayer(layerId, visible) {
    const visibility = visible ? 'visible' : 'none';
    map.setLayoutProperty(layerId, 'visibility', visibility);
}

// Usage
document.getElementById('toggle-gaeltacht').addEventListener('change', (e) => {
    toggleLayer('gaeltacht-fill', e.target.checked);
    toggleLayer('gaeltacht-outline', e.target.checked);
});
```

### 3.5 Legend

#### Choropleth Legend

```javascript
function createLegend() {
    const legend = document.getElementById('legend');

    const grades = [0, 10, 20, 40, 60, 80];
    const colors = ['#f7fbff', '#c6dbef', '#9ecae1', '#6baed6', '#3182bd', '#08519c'];

    legend.innerHTML = '<h4>Irish Speakers (%)</h4>';

    grades.forEach((grade, i) => {
        const next = grades[i + 1] || '+';
        legend.innerHTML += `
            <div>
                <span style="background:${colors[i]}; width:20px; height:20px; display:inline-block;"></span>
                ${grade}${next !== '+' ? ' - ' + next : next}
            </div>
        `;
    });
}

map.on('load', createLegend);
```

#### School Legend

```javascript
function createSchoolLegend() {
    const legend = document.getElementById('school-legend');

    legend.innerHTML = `
        <h4>Schools</h4>
        <div>
            <span style="background:#4CAF50; width:12px; height:12px; display:inline-block; border-radius:50%;"></span>
            Primary
        </div>
        <div>
            <span style="background:#2196F3; width:12px; height:12px; display:inline-block; border-radius:50%;"></span>
            Secondary
        </div>
    `;
}
```

### 3.6 Vector Tile Generation

#### Using tippecanoe

```bash
#!/bin/bash
# Generate vector tiles from GeoJSON

# Census choropleth tiles
tippecanoe \
    -o census.mbtiles \
    -z 14 \
    -l census \
    --coalesce-densest-as-needed \
    --extend-zooms-if-still-dropping \
    speaker_choropleth.geojson

# Gaeltacht boundaries
tippecanoe \
    -o gaeltacht.mbtiles \
    -z 14 \
    -l gaeltacht \
    --simplify-only-low-zooms \
    gaeltacht_areas.geojson

# Schools (preserve all features)
tippecanoe \
    -o schools.mbtiles \
    -z 14 \
    -l schools \
    --drop-smallest-as-needed \
    -r1 \
    irish_medium_schools.geojson
```

#### Serving Tiles

```bash
# Using tileserver-gl
docker run --rm -it \
    -v $(pwd)/tiles:/data \
    -p 8080:8080 \
    maptiler/tileserver-gl
```

### 3.7 Complete Application

```javascript
// Full application with all features
class CelticLanguageMap {
    constructor(containerId) {
        this.map = new maplibregl.Map({
            container: containerId,
            style: this.getBaseStyle(),
            center: [-8.0, 53.5],
            zoom: 6
        });

        this.map.on('load', () => this.initLayers());
    }

    getBaseStyle() {
        return {
            version: 8,
            sources: {
                'carto': {
                    type: 'raster',
                    tiles: ['https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png'],
                    tileSize: 256,
                    attribution: '(c) CartoDB (c) OSM'
                }
            },
            layers: [{
                id: 'base',
                type: 'raster',
                source: 'carto'
            }]
        };
    }

    async initLayers() {
        // Load data sources
        await this.loadSource('gaeltacht', '/data/gaeltacht.geojson');
        await this.loadSource('census', '/data/census.geojson');
        await this.loadSource('schools', '/data/schools.geojson');

        // Add layers
        this.addChoroplethLayer();
        this.addGaeltachtLayer();
        this.addSchoolsLayer();

        // Setup interactivity
        this.setupPopups();
        this.createLegend();
    }

    async loadSource(id, url) {
        const response = await fetch(url);
        const data = await response.json();

        this.map.addSource(id, {
            type: 'geojson',
            data: data
        });
    }

    addChoroplethLayer() {
        this.map.addLayer({
            id: 'census-fill',
            type: 'fill',
            source: 'census',
            paint: {
                'fill-color': [
                    'interpolate', ['linear'], ['get', 'speaker_pct'],
                    0, '#f7fbff',
                    10, '#c6dbef',
                    20, '#9ecae1',
                    40, '#6baed6',
                    60, '#3182bd',
                    80, '#08519c'
                ],
                'fill-opacity': 0.6
            }
        });
    }

    addGaeltachtLayer() {
        this.map.addLayer({
            id: 'gaeltacht-boundary',
            type: 'line',
            source: 'gaeltacht',
            paint: {
                'line-color': '#228B22',
                'line-width': 3
            }
        });
    }

    addSchoolsLayer() {
        this.map.addLayer({
            id: 'schools',
            type: 'circle',
            source: 'schools',
            paint: {
                'circle-radius': 6,
                'circle-color': '#E91E63',
                'circle-stroke-width': 2,
                'circle-stroke-color': '#fff'
            }
        });
    }

    setupPopups() {
        // School popups
        this.map.on('click', 'schools', (e) => {
            const props = e.features[0].properties;
            new maplibregl.Popup()
                .setLngLat(e.lngLat)
                .setHTML(`<h3>${props.school_name}</h3><p>Enrollment: ${props.enrollment}</p>`)
                .addTo(this.map);
        });

        // Cursor changes
        ['census-fill', 'schools'].forEach(layer => {
            this.map.on('mouseenter', layer, () => {
                this.map.getCanvas().style.cursor = 'pointer';
            });
            this.map.on('mouseleave', layer, () => {
                this.map.getCanvas().style.cursor = '';
            });
        });
    }

    createLegend() {
        // Implementation from section 3.5
    }
}

// Initialize
const celticMap = new CelticLanguageMap('map');
```

### 3.8 Data-Driven Styling Examples

#### Gradient by Daily Speakers

```javascript
'fill-color': [
    'case',
    ['<', ['get', 'daily_pct'], 1], '#fee5d9',
    ['<', ['get', 'daily_pct'], 5], '#fcae91',
    ['<', ['get', 'daily_pct'], 10], '#fb6a4a',
    ['<', ['get', 'daily_pct'], 20], '#de2d26',
    '#a50f15'
]
```

#### School Size by Enrollment

```javascript
'circle-radius': [
    'step',
    ['get', 'enrollment'],
    4,   // Default
    100, 6,
    200, 8,
    500, 12
]
```

---

## 4. Cross-border Harmonization

### 4.1 Geographic Comparability

The challenge of comparing Irish language data across the Republic of Ireland and Northern Ireland involves reconciling fundamentally different statistical geographies and census methodologies.

#### ROI vs NI Geographic Units

| Feature | Republic of Ireland | Northern Ireland |
|---------|---------------------|------------------|
| **Primary Unit** | Small Area (SA) | Data Zone (DZ2021) |
| **Count** | ~18,000 | 3,780 |
| **Average Population** | ~250 | ~500 |
| **Parent Unit** | Electoral Division (ED) | Super Data Zone (SDZ) |
| **Regional Level** | County (31) | Local Government District (11) |

#### Coordinate Reference Systems

| Jurisdiction | Native CRS | Web Map CRS |
|--------------|------------|-------------|
| ROI | Irish Transverse Mercator (ITM) EPSG:2157 | WGS84 (EPSG:4326) |
| NI | Irish Grid (IG) EPSG:29902 | WGS84 (EPSG:4326) |

**Transform in DuckDB:**
```sql
SELECT ST_Transform(geom, 'EPSG:4326') AS geom_wgs84 FROM boundaries;
```

### 4.2 Temporal Alignment

| Data Type | ROI Date | NI Date |
|-----------|----------|---------|
| Census | 2022 | 2021 |
| Boundaries | 2024 | 2021 |
| Schools | 2023/24 | 2022/23 |

### 4.3 Census Question Comparability

#### ROI Census 2022 Questions

1. **Can you speak Irish?** (Yes/No)
2. **How often do you speak Irish?**
   - Daily (within education)
   - Daily (outside education)
   - Weekly
   - Less often
   - Never
3. **How well do you speak Irish?** (New in 2022)
   - Very well
   - Well
   - Not well

#### NI Census 2021 Questions

1. **Can you understand, speak, read or write Irish?** (Multiple choice)
   - Understand spoken Irish
   - Speak Irish
   - Read Irish
   - Write Irish
   - None
2. **How often do you speak Irish?** (New in 2021)
   - Daily
   - Weekly
   - Less often
   - Never

#### Mapping Common Metrics

| Metric | ROI Variable | NI Variable | Comparable? |
|--------|-------------|-------------|-------------|
| **Some ability** | "Can speak Irish" = Yes | Any of understand/speak/read/write | Approximate |
| **Daily speakers** | "Daily outside education" | "Daily" | Good |
| **Fluency** | "Very well" + "Well" | "Speak" + "Read" + "Write" | Approximate |

### 4.4 Harmonization Strategies

#### Strategy 1: Aggregate to Comparable Levels

When direct comparison of small areas is problematic, aggregate to county/council level where populations are large enough for meaningful comparison.

```sql
-- Compare at county/council level
SELECT
    'ROI' AS jurisdiction,
    county_name AS region,
    SUM(daily_speakers) AS daily,
    SUM(population) AS pop,
    ROUND(100.0 * SUM(daily_speakers) / SUM(population), 2) AS daily_pct
FROM roi_census
GROUP BY county_name

UNION ALL

SELECT
    'NI' AS jurisdiction,
    lgd_name AS region,
    SUM(daily_speakers) AS daily,
    SUM(population) AS pop,
    ROUND(100.0 * SUM(daily_speakers) / SUM(population), 2) AS daily_pct
FROM ni_census
GROUP BY lgd_name
ORDER BY daily_pct DESC;
```

#### Strategy 2: Define NI "Gaeltacht-Equivalent" Areas

Since NI has no official Gaeltacht designation, define areas with high speaker concentration:

```sql
-- Identify NI Data Zones with >10% daily Irish speakers
CREATE TABLE ni_irish_concentration AS
SELECT
    dz_code,
    geom,
    daily_speakers,
    population,
    ROUND(100.0 * daily_speakers / NULLIF(population, 0), 2) AS daily_pct
FROM ni_census_data_zones
WHERE (100.0 * daily_speakers / NULLIF(population, 0)) > 10;
```

#### Strategy 3: Focus on Common Metrics

Use "daily speakers" as the primary comparison metric, as both censuses now collect this data with similar methodology.

### 4.5 Border Region Analysis

The border region offers unique analytical opportunities due to geographic proximity.

#### Border Counties

| ROI Counties | NI Counties |
|--------------|-------------|
| Donegal | Derry |
| Leitrim | Tyrone |
| Cavan | Fermanagh |
| Monaghan | Armagh |
| Louth | Down |

#### Cross-Border Query

```sql
-- Analyze Irish language use in border region
WITH border_areas AS (
    -- ROI border Small Areas
    SELECT
        'ROI' AS jurisdiction,
        sa_code AS area_code,
        geom,
        daily_speakers,
        population
    FROM roi_small_areas
    WHERE county IN ('Donegal', 'Leitrim', 'Cavan', 'Monaghan', 'Louth')

    UNION ALL

    -- NI border Data Zones
    SELECT
        'NI' AS jurisdiction,
        dz_code AS area_code,
        geom,
        daily_speakers,
        population
    FROM ni_data_zones
    WHERE lgd IN ('Derry City and Strabane', 'Fermanagh and Omagh',
                  'Mid Ulster', 'Armagh City, Banbridge and Craigavon',
                  'Newry, Mourne and Down')
)
SELECT
    jurisdiction,
    COUNT(*) AS num_areas,
    SUM(population) AS total_pop,
    SUM(daily_speakers) AS total_daily,
    ROUND(100.0 * SUM(daily_speakers) / SUM(population), 2) AS daily_pct
FROM border_areas
GROUP BY jurisdiction;
```

### 4.6 Key Metrics Comparison

| Metric | ROI (Census 2022) | NI (Census 2021) |
|--------|-------------------|------------------|
| **Total Population** | 5,149,139 | 1,903,175 |
| **Can Speak/Some Ability** | 1,873,997 (40%) | 228,617 (12.45%) |
| **Daily Speakers** | 71,968 (1.5%) | 43,557 (2.43%) |
| **Main Language** | N/A | 5,969 (0.32%) |

### 4.7 Gaeltacht vs Belfast Gaeltacht Quarter

A unique comparison can be made between traditional Gaeltacht areas and the urban "neo-Gaeltacht" in Belfast.

| Feature | Traditional Gaeltacht (ROI) | Belfast Gaeltacht Quarter |
|---------|---------------------------|---------------------------|
| **Origin** | Historical continuity | Community revival |
| **Legal Status** | Statutory (Gaeltacht Acts) | None |
| **Population** | 106,220 | ~5,000 |
| **Speaker %** | 66% can speak | ~50% can speak |
| **Daily Use** | Declining | Growing |

### 4.8 Visualization Considerations

When displaying cross-border data:

1. **Use consistent color scales** - Apply the same choropleth breaks to both jurisdictions
2. **Clearly mark the border** - Use a distinct line style for the international border
3. **Provide context** - Include notes about data comparability in legends
4. **Offer both views** - Provide options to view ROI-only, NI-only, or all-island

#### Example MapLibre Layer for Border

```javascript
map.addLayer({
    id: 'border-line',
    type: 'line',
    source: 'border',
    paint: {
        'line-color': '#000',
        'line-width': 2,
        'line-dasharray': [4, 2]
    }
});
```

---

## References

### Data Sources

- Tailte Eireann Open Data: https://data-osi.opendata.arcgis.com
- CSO PxStat: https://data.cso.ie
- NISRA: https://www.nisra.gov.uk
- data.gov.ie: https://data.gov.ie
- OpenDataNI: https://www.opendatani.gov.uk

### Technical Documentation

- DuckDB Spatial: https://duckdb.org/docs/extensions/spatial
- PostGIS (compatible functions): https://postgis.net/docs/
- MapLibre GL JS: https://maplibre.org/maplibre-gl-js/docs/
- tippecanoe: https://github.com/felt/tippecanoe
- GeoParquet: https://geoparquet.org/

### Organizations

- Gaeloideachas: https://gaeloideachas.ie
- Comhairle na Gaelscolaiochta: https://www.comhairle.org
- Gaois Research Group (DCU): https://www.gaois.ie
