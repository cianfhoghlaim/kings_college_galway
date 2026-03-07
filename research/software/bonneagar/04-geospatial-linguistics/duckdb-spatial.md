# DuckDB Spatial for Celtic Language Mapping

## Overview

DuckDB's spatial extension provides PostGIS-compatible geospatial functions for analyzing Celtic language areas, performing spatial joins between census data and boundaries, and preparing data for visualization.

---

## 1. Setup

### 1.1 Installation

```python
import duckdb

# Create connection and install spatial
conn = duckdb.connect("celtic_geo.duckdb")
conn.execute("INSTALL spatial; LOAD spatial;")
```

### 1.2 Verify Installation

```sql
-- Check spatial functions available
SELECT * FROM duckdb_functions() WHERE function_name LIKE 'ST_%' LIMIT 10;
```

---

## 2. Loading Geospatial Data

### 2.1 GeoJSON Files

```sql
-- Load Gaeltacht boundaries from GeoJSON
CREATE TABLE gaeltacht_areas AS
SELECT * FROM ST_Read('/path/to/gaeltacht_areas.geojson');

-- Load NI Data Zones
CREATE TABLE ni_data_zones AS
SELECT * FROM ST_Read('/path/to/dz2021.geojson');
```

### 2.2 Shapefiles

```sql
-- Load from Shapefile
CREATE TABLE language_planning_areas AS
SELECT * FROM ST_Read('/path/to/lpa_boundaries.shp');
```

### 2.3 CSV with Coordinates

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

---

## 3. Core Spatial Operations

### 3.1 Point in Polygon (Schools in Gaeltacht)

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

### 3.2 Spatial Join (Census to Boundaries)

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

### 3.3 Buffer Analysis

```sql
-- Find schools within 5km of Gaeltacht boundaries
SELECT
    s.school_name,
    ST_Distance(s.geom, g.geom) / 1000 AS distance_km
FROM schools s, gaeltacht_areas g
WHERE ST_DWithin(s.geom, ST_Buffer(g.geom, 5000), 0)
ORDER BY distance_km;
```

### 3.4 Area Calculations

```sql
-- Calculate area of each Gaeltacht region
SELECT
    area_name,
    ROUND(ST_Area(geom) / 1000000, 2) AS area_km2
FROM gaeltacht_areas
ORDER BY area_km2 DESC;
```

---

## 4. Census Data Analysis

### 4.1 Speaker Concentration Mapping

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

### 4.2 Gaeltacht vs Non-Gaeltacht Comparison

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

### 4.3 County-Level Aggregation

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

---

## 5. School Analysis

### 5.1 School Density by Area

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

### 5.2 Schools in Language Planning Areas

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

### 5.3 Distance to Nearest School

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

---

## 6. Cross-Border Analysis

### 6.1 Unified View

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

### 6.2 Border Region Analysis

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

---

## 7. Export for MapLibre

### 7.1 GeoJSON Export

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

### 7.2 Prepare for Vector Tiles

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

### 7.3 Centroid Export (For Labels)

```sql
-- Generate centroids for labeling
SELECT
    area_name,
    ST_X(ST_Centroid(geom)) AS lng,
    ST_Y(ST_Centroid(geom)) AS lat
FROM gaeltacht_areas;
```

---

## 8. Complete Pipeline Example

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

---

## 9. Performance Tips

| Operation | Tip |
|-----------|-----|
| **Large datasets** | Use `ST_Simplify()` for web export |
| **Spatial joins** | Create spatial index with `CREATE INDEX` |
| **Point-in-polygon** | Use `ST_DWithin()` for approximate queries |
| **Memory** | Use disk-based DB for >1GB data |

---

## References

- DuckDB Spatial: https://duckdb.org/docs/extensions/spatial
- PostGIS (compatible functions): https://postgis.net/docs/
- Tailte Éireann Open Data: https://data-osi.opendata.arcgis.com
