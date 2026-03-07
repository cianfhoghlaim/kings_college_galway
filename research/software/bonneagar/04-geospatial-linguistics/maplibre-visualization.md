# MapLibre Visualization for Celtic Language Data

## Overview

MapLibre GL JS is an open-source library for rendering interactive maps from vector tiles. This document covers implementation patterns for visualizing Irish language areas, schools, and census data.

---

## 1. Basic Setup

### 1.1 HTML Template

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

### 1.2 Initialize Map

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
                attribution: '© OpenStreetMap'
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

---

## 2. Loading Data Sources

### 2.1 GeoJSON Source

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

### 2.2 Vector Tiles Source

```javascript
// For large datasets, use vector tiles
map.addSource('census-tiles', {
    type: 'vector',
    tiles: ['https://your-server.com/tiles/census/{z}/{x}/{y}.pbf'],
    minzoom: 0,
    maxzoom: 14
});
```

---

## 3. Layer Styling

### 3.1 Choropleth Layer (Speaker Percentage)

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

### 3.2 Gaeltacht Boundaries

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

### 3.3 School Points

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

### 3.4 Labels

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

---

## 4. Interactivity

### 4.1 Hover Effects

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

### 4.2 Click Popups

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

### 4.3 Layer Toggle

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

---

## 5. Legend

### 5.1 Choropleth Legend

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

### 5.2 School Legend

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

---

## 6. Vector Tile Generation

### 6.1 Using tippecanoe

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

### 6.2 Serving Tiles

```bash
# Using tileserver-gl
docker run --rm -it \
    -v $(pwd)/tiles:/data \
    -p 8080:8080 \
    maptiler/tileserver-gl
```

---

## 7. Complete Application

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
                    attribution: '© CartoDB © OSM'
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
        // Implementation from section 5
    }
}

// Initialize
const celticMap = new CelticLanguageMap('map');
```

---

## 8. Data-Driven Styling Examples

### 8.1 Gradient by Daily Speakers

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

### 8.2 School Size by Enrollment

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

## References

- MapLibre GL JS: https://maplibre.org/maplibre-gl-js/docs/
- tippecanoe: https://github.com/felt/tippecanoe
- CartoDB Basemaps: https://carto.com/basemaps
- OpenStreetMap: https://www.openstreetmap.org
