# UK Education Datasets Analysis

## Overview

This document provides a comprehensive analysis of education datasets available in `/Users/cliste/dev/dkit/semester_1/joint_project/datasets/` and guidance on obtaining parallel data for the rest of the British Isles.

---

## Dataset Inventory

| Source | Size | Files | Coverage | Primary Use |
|--------|------|-------|----------|-------------|
| **DfE** | 1.4GB | 216 CSV | England | School performance, KS4/KS5 results |
| **GIS** | 98MB | 12 CSV | England (94%) + Wales (5%) | School locations, establishment data |
| **ONS** | 184MB | 3 CSV | UK-wide | Demographics, qualifications, employment |
| **UCAS** | 4.7GB | 284 CSV | UK-wide | University admissions, equality data |
| **Raw** | 550MB | 121 files | England | Original source files, IMD |

---

## 1. Department for Education (DfE) Data

### Source Path
`/Users/cliste/dev/dkit/semester_1/joint_project/datasets/dfe/`

### Coverage
- **Geographic**: England only
- **Temporal**: 2009/10 - 2023/24 (KS4), 1995/96 - 2023/24 (A-Level timeseries)
- **Granularity**: School → Local Authority → Regional → National

### Key Metrics

#### Key Stage 4 (GCSE, Ages 14-16)
- Attainment 8 scores (average across 8 subjects)
- Progress 8 (value-added from KS2 to KS4)
- English Baccalaureate (EBacc) achievement
- Level 2 Basics (English & Maths grades 4+/5+)
- Subject-level entries and grades

#### Key Stage 5 (A-Level, Ages 16-18)
- Grade distributions (A*-U)
- Average Points Score (APS)
- UCAS Tariff thresholds
- Subject timeseries (29 years)
- Retention rates

#### Progression
- Sustained destinations (education, training, employment)
- Russell Group university entry
- Oxbridge progression rates
- Apprenticeship outcomes

### Demographic Breakdowns Available
- Gender
- Ethnicity (detailed categories)
- Free School Meals (FSM) eligibility
- Special Educational Needs (SEN)
- English as Additional Language (EAL)
- Prior attainment bands

### Key Files
```
dfe/key-stage-4-performance_2023-24/
dfe/a-level-and-other-16-to-18-results_2023-24/
dfe/progression-to-higher-education-or-training_2022-23/
```

---

## 2. GIS/Spatial Data

### Source Path
`/Users/cliste/dev/dkit/semester_1/joint_project/datasets/gis/`

### Coverage
- **Total Records**: 51,688 establishments
- **England**: 48,671 (94.2%)
- **Wales**: 2,477 (4.8%)
- **Georeferenced**: 96.7% have Easting/Northing coordinates

### Key Data
- School locations (British National Grid EPSG:27700)
- LSOA/MSOA assignment (97.2% coverage)
- Establishment types (primary, secondary, special, FE)
- Multi-Academy Trust (MAT) membership
- Pupil counts and capacity
- FSM percentages
- Ofsted ratings

### Primary File
```
gis/edubasealldata20250426.csv (60.5MB, 135 columns)
```

### Linking Keys
- URN (Unique Reference Number)
- LSOA Code
- Local Authority Code
- Postcode

---

## 3. ONS Census Data

### Source Path
`/Users/cliste/dev/dkit/semester_1/joint_project/datasets/ons/`

### Coverage
- **Geographic**: UK-wide at LSOA level
- **Total LSOAs**: 35,672

### Datasets

| File | Size | Rows | Categories |
|------|------|------|------------|
| ethnic_group.csv | 55MB | 713,440 | 20 ethnic categories |
| economic_activity_status.csv | 84MB | 713,440 | 20 employment categories |
| highest_qualification.csv | 54MB | 285,376 | 8 qualification levels |

### Use Cases
- Neighbourhood demographic profiling
- Parental employment/education context
- Ethnic diversity analysis
- School catchment characterisation

---

## 4. UCAS Admissions Data

### Source Path
`/Users/cliste/dev/dkit/semester_1/joint_project/datasets/ucas/`

### Coverage
- **Geographic**: UK-wide (England, Scotland, Wales, Northern Ireland)
- **Temporal**: 2006-2023 (18-year longitudinal)
- **Size**: 4.7GB across 284 CSV files

### Key Metrics
- Applicants and acceptances
- Offer rates
- Entry rates per 10,000 population
- Clearing and RPA routes
- Predicted vs achieved grades

### Demographic Dimensions
- Gender, Age, Ethnicity
- POLAR4 quintiles (participation rates)
- Disability status
- Deprivation indices (IMD, SIMD, WIMD, NIMDM)

### Subject Coverage
- HECoS subject classification
- STEM subjects
- Teacher training (dedicated tracking)
- Nursing pathways

### Key Directories
```
ucas/eoc_2023/ (230 files, 2.1GB)
ucas/eoc_provider_2023/ (46 files, 2.6GB)
ucas/equality_2022/ (4 files, 42MB)
```

---

## 5. Raw/Original Data

### Source Path
`/Users/cliste/dev/dkit/semester_1/joint_project/datasets/raw/`

### Contents
- **DfE Raw** (357MB, 85 files): Original school performance files
- **ONS Raw** (184MB, 36 files): Census source data
- **IMD** (9.3MB): Index of Multiple Deprivation scores/ranks/deciles

### IMD Domains
1. Income Deprivation
2. Employment Deprivation
3. Education, Skills & Training
4. Health Deprivation & Disability
5. Crime
6. Barriers to Housing & Services
7. Living Environment

### Sub-indices
- IDACI (Income Deprivation Affecting Children)
- IDAOPI (Income Deprivation Affecting Older People)

---

## Analytical Capabilities

### What These Datasets Enable

1. **Attainment Analysis**
   - Track GCSE/A-Level performance over 15+ years
   - Measure school value-added (Progress 8)
   - Compare subjects and qualification routes

2. **Equity Research**
   - Disadvantage gaps (FSM vs non-FSM)
   - Ethnic attainment disparities
   - Gender differences by subject
   - SEN outcomes

3. **Geographic Studies**
   - Regional performance variation
   - Rural/urban differences
   - Deprivation correlations
   - School accessibility

4. **Progression Pathways**
   - School → FE/6th Form transitions
   - University entry patterns
   - Russell Group access
   - Apprenticeship uptake

5. **STEM Pipeline**
   - Subject entry trends
   - Gender imbalance in Physics/Computing
   - Triple Science uptake
   - HE STEM progression

---

## Data Linkage

### Primary Keys

| Key | Description | Found In |
|-----|-------------|----------|
| URN | School identifier | DfE, GIS |
| LSOA | Lower Super Output Area | ONS, IMD, GIS |
| LA Code | Local Authority | All datasets |
| Postcode | Geographic location | GIS, DfE |

### Example Joins

```
School Performance (DfE)
    ↓ via LA Code
Deprivation Index (IMD)
    ↓ via LSOA
Demographics (ONS)
    ↓ via LSOA
School Location (GIS)
```

---

## Limitations

- **England-centric**: DfE and IMD are England-only
- **Wales partial**: GIS includes Welsh schools but limited metrics
- **Scotland/NI absent**: No primary/secondary school data
- **COVID disruption**: 2020/21 data not comparable (teacher-assessed grades)
- **Data suppression**: Small cohorts masked for privacy
