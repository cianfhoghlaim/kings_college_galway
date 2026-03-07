# Geospatial Data Sources for Celtic Language Mapping

## Overview

This document provides detailed information on official data sources for Gaeltacht boundaries, census statistics, and school locations in the Republic of Ireland and Northern Ireland.

---

## 1. Republic of Ireland - Boundaries

### 1.1 Gaeltacht Areas

Official Gaeltacht regions defined by the Gaeltacht Area Orders (1956, 1967, 1974, 1982).

| Property | Value |
|----------|-------|
| **Source** | Tailte Éireann |
| **Dataset** | Gaeltacht Areas - National Administrative Boundaries - Ungeneralised - 2024 |
| **Portal** | data.gov.ie |
| **Formats** | GeoJSON, Shapefile, CSV, KML |
| **Level** | Electoral Division (parts of) |

**Download URL:**
https://data-osi.opendata.arcgis.com/datasets/osi::gaeltacht-areas-national-administrative-boundaries-ungeneralised-2024

**Coverage:**
- 155 Electoral Divisions (or parts)
- Counties: Cork, Donegal, Galway, Kerry, Mayo, Meath, Waterford

### 1.2 Language Planning Areas (LPAs)

Areas designated under the Gaeltacht Act 2012 for language planning.

| Property | Value |
|----------|-------|
| **Source** | Tailte Éireann |
| **Dataset** | Gaeltacht Language Planning Areas - Ungeneralised - 2024 |
| **Portal** | data.gov.ie |
| **Formats** | GeoJSON, Shapefile, CSV, KML |
| **Count** | 26 LPAs |

**Download URL:**
https://data-osi.opendata.arcgis.com/datasets/osi::gaeltacht-language-planning-areas-national-administrative-boundaries-ungen-2024

### 1.3 Small Area Boundaries

Census geography for detailed population analysis.

| Property | Value |
|----------|-------|
| **Source** | CSO / Tailte Éireann |
| **Count** | ~18,000 Small Areas |
| **Portal** | data.gov.ie / PxStat |
| **Formats** | GeoJSON, Shapefile |

---

## 2. Republic of Ireland - Census Data

### 2.1 Census 2022 - Irish Language

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

### 2.2 Download Instructions

1. Navigate to https://data.cso.ie
2. Search for "Irish language" or table ID
3. Select geographic level (ED, SA, County)
4. Download as CSV or XLSX

---

## 3. Republic of Ireland - School Data

### 3.1 Department of Education

**Portal:** https://www.gov.ie/en/service/find-a-school/

**Data Available:**
- School Roll Number
- Address
- Eircode
- Phone/Email
- Enrollment figures

**Format:** Excel spreadsheets

### 3.2 Gaeloideachas

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

### 3.3 Data Combination Strategy

1. Download Gaeloideachas lists (definitive Irish-medium identification)
2. Download Department of Education lists (Eircodes, official addresses)
3. Join on School Roll Number or normalized school name
4. Geocode using Eircode

---

## 4. Northern Ireland - Boundaries

### 4.1 Data Zones (DZ2021)

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

### 4.2 Geographic Hierarchy

| Level | Count | Notes |
|-------|-------|-------|
| Data Zones | 3,780 | Primary census unit |
| Super Data Zones | 890 | Aggregation level |
| District Electoral Areas | 80 | Electoral boundaries |
| Local Government Districts | 11 | Council areas |

---

## 5. Northern Ireland - Census Data

### 5.1 Census 2021 - Irish Language

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

### 5.2 Data Access

**Flexible Table Builder:** https://build.nisra.gov.uk

Features:
- Custom variable selection
- Cross-tabulation
- Geographic filtering to Data Zone
- CSV export

**Note:** Statistical Disclosure Control may suppress small counts.

---

## 6. Northern Ireland - School Data

### 6.1 Department of Education NI

**Portal:** https://www.education-ni.gov.uk

**Irish-Medium Schools List:**
https://www.education-ni.gov.uk/articles/irish-medium-schools

**Content:**
- 30 standalone Irish-medium schools
- 10 Irish-medium units
- 46 nurseries (via CnaG)

### 6.2 School Enrolment Data

**URL:** https://www.education-ni.gov.uk/publications/school-enrolment-school-level-data-202223

**Files Available:**
- Primary schools (Excel)
- Post-primary schools (Excel)
- Nursery schools (Excel)

**Key Fields:**
- School name
- Address
- Postcode
- School Reference Number

### 6.3 Comhairle na Gaelscolaíochta (CnaG)

**Portal:** https://www.comhairle.org

Authoritative source for Irish-medium education in NI.

**Data Strategy:**
1. Get school names from DE NI Irish-medium list
2. Get postcodes from school enrolment Excel files
3. Cross-reference with CnaG for Naíscoileanna

---

## 7. Data Quality Notes

### 7.1 Coordinate Reference Systems

| Jurisdiction | Native CRS | Web Map CRS |
|--------------|------------|-------------|
| ROI | Irish Transverse Mercator (ITM) | WGS84 (EPSG:4326) |
| NI | Irish Grid (IG) | WGS84 (EPSG:4326) |

**Transform in DuckDB:**
```sql
SELECT ST_Transform(geom, 'EPSG:4326') AS geom_wgs84 FROM boundaries;
```

### 7.2 Temporal Alignment

| Data Type | ROI Date | NI Date |
|-----------|----------|---------|
| Census | 2022 | 2021 |
| Boundaries | 2024 | 2021 |
| Schools | 2023/24 | 2022/23 |

### 7.3 Geographic Comparability

| Challenge | Solution |
|-----------|----------|
| Different census years | Compare trends, not absolutes |
| Different small areas | Aggregate to council/county level |
| No NI Gaeltacht | Define by speaker concentration |

---

## 8. Summary Table

| Data Type | ROI Source | ROI Format | NI Source | NI Format |
|-----------|------------|------------|-----------|-----------|
| **Gaeltacht Boundaries** | Tailte Éireann | GeoJSON | N/A | Define from census |
| **Census - Language** | CSO PxStat | CSV | NISRA Builder | CSV |
| **Small Area Boundaries** | Tailte Éireann | GeoJSON | NISRA | GeoJSON |
| **Schools** | gov.ie + Gaeloideachas | Excel | DE NI + CnaG | Excel |

---

## 9. Download Checklist

### ROI

- [ ] Gaeltacht Areas 2024 (GeoJSON)
- [ ] Language Planning Areas 2024 (GeoJSON)
- [ ] Small Area Boundaries 2022 (GeoJSON)
- [ ] Census F8014 - Irish speakers by frequency
- [ ] Department of Education school list
- [ ] Gaeloideachas school lists

### NI

- [ ] Data Zone Boundaries DZ2021 (GeoJSON)
- [ ] Census 2021 Irish language data (via Flexible Table Builder)
- [ ] School enrolment data (Excel)
- [ ] Irish-medium schools list

---

## References

- Tailte Éireann: https://data-osi.opendata.arcgis.com
- CSO PxStat: https://data.cso.ie
- NISRA: https://www.nisra.gov.uk
- data.gov.ie: https://data.gov.ie
- OpenDataNI: https://www.opendatani.gov.uk
