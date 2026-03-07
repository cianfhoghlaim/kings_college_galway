# Education Data Insights Summary

## Key Findings from Dataset Analysis

---

## 1. Data Richness Assessment

### Strengths of Current Dataset

| Dimension | Quality | Notes |
|-----------|---------|-------|
| **Temporal depth** | Excellent | Up to 29 years of A-Level data |
| **Geographic granularity** | Excellent | School → LSOA → LA → Region → National |
| **Demographic coverage** | Very Good | FSM, ethnicity, SEN, gender, EAL |
| **Outcome tracking** | Very Good | KS4 → KS5 → HE progression |
| **Spatial precision** | Excellent | 96.7% of schools georeferenced |
| **Deprivation context** | Good | IMD at LSOA level (England only) |

### Gaps Identified

| Gap | Severity | Impact |
|-----|----------|--------|
| Scotland school data | High | Cannot do UK-wide school comparison |
| Wales performance data | Medium | Limited Welsh school analysis |
| Northern Ireland data | Medium | Missing unique selective system |
| 2020/21 comparability | Medium | COVID year not usable for trends |
| Independent school metrics | Low | Limited demographic data |

---

## 2. Potential Research Questions

### Equity & Access

1. **Disadvantage Gap Analysis**
   - How does the FSM attainment gap vary by region?
   - Are gaps narrowing or widening over the 15-year period?
   - Which subjects show largest/smallest disadvantage gaps?

2. **Ethnic Attainment Patterns**
   - How do different ethnic groups perform across subject areas?
   - What is the intersection of ethnicity and deprivation?
   - How do patterns differ between primary and secondary education?

3. **University Access**
   - Which demographic groups are underrepresented at Russell Group universities?
   - How does school type affect Oxbridge progression?
   - What is the relationship between POLAR4 quintile and university entry?

### STEM Pipeline

4. **Subject Uptake Trends**
   - How has Computer Science GCSE/A-Level uptake changed since introduction?
   - What is the gender ratio in Physics/Computing by region?
   - How does prior attainment predict STEM subject choice?

5. **Progression Pathways**
   - What proportion of Triple Science students continue to STEM A-Levels?
   - How do BTEC vs A-Level routes affect HE STEM entry?
   - Which schools produce highest STEM progression rates?

### Geographic Analysis

6. **Regional Disparities**
   - Which local authorities show highest/lowest Progress 8 scores?
   - How does deprivation explain regional attainment variation?
   - Are there "cold spots" for specific subjects?

7. **School Accessibility**
   - What is the average distance to nearest secondary school?
   - Are there areas with insufficient school capacity?
   - How do school choice patterns cross LA boundaries?

### Institutional Analysis

8. **School Type Effects**
   - How do academies compare to maintained schools on value-added?
   - What is the effect of Multi-Academy Trust membership?
   - How do selective schools affect non-selective school outcomes?

---

## 3. Data Linkage Opportunities

### Primary Linkage Paths

```
┌─────────────────┐
│   DfE School    │
│   Performance   │
│    (URN key)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│   GIS School    │────▶│   ONS Census    │
│   Locations     │     │   Demographics  │
│  (LSOA/coord)   │     │   (LSOA key)    │
└────────┬────────┘     └─────────────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│   IMD Scores    │     │  UCAS Admissions│
│   (LSOA key)    │     │  (LA/postcode)  │
└─────────────────┘     └─────────────────┘
```

### Example Linked Analysis

**Research Question**: Do schools in deprived areas with diverse populations show better or worse progression to university?

**Data Join**:
1. Start with DfE progression data (school-level)
2. Join GIS data via URN to get LSOA
3. Join IMD via LSOA for deprivation decile
4. Join ONS via LSOA for ethnic composition
5. Aggregate to LA for UCAS comparison

---

## 4. Methodological Considerations

### Value-Added Measures

- **Progress 8** already controls for prior attainment (KS2 baseline)
- Enables fair comparison between schools with different intakes
- 95% confidence intervals provided for significance testing

### Suppression & Privacy

- Cohorts <6 pupils suppressed at school level
- Use aggregated data for small-group analysis
- Consider statistical disclosure control

### Temporal Comparability

| Period | Notes |
|--------|-------|
| Pre-2010 | Different qualification structures |
| 2010-2016 | Old GCSE grading (A*-G) |
| 2017+ | New GCSE grading (9-1) |
| 2020/21 | Teacher-assessed grades (not comparable) |
| 2022+ | Return to exams, potential grade deflation |

### Geographic Boundary Changes

- LSOA boundaries revised between 2001 and 2011 censuses
- Local authority mergers and reorganisations
- Use lookup tables for consistent time series

---

## 5. Quick Statistics

### England Education System (from your data)

| Metric | Value | Source |
|--------|-------|--------|
| Total establishments | 51,688 | GIS |
| Primary schools | 30,163 | GIS |
| Secondary schools | 6,937 | GIS |
| Total pupils in dataset | 10.7 million | GIS |
| Local authorities | 150+ | DfE |
| LSOAs with data | 35,672 | ONS |
| A-Level subjects tracked | 40+ | DfE |
| Years of KS4 data | 15 | DfE |
| Years of A-Level data | 29 | DfE |

### UCAS UK-Wide (from your data)

| Metric | Value |
|--------|-------|
| Years of data | 18 (2006-2023) |
| Total CSV files | 284 |
| Data size | 4.7GB |
| Nations covered | 4 (Eng, Scot, Wales, NI) |
| Demographic dimensions | 10+ |

---

## 6. Recommended Next Steps

### Immediate Actions

1. **Validate data linkages**
   - Test LSOA joins between ONS, IMD, and GIS
   - Verify URN consistency across DfE years
   - Check LA code mappings

2. **Create baseline metrics**
   - National averages for key indicators
   - Regional benchmarks
   - Demographic group baselines

3. **Build analysis framework**
   - Define outcome variables
   - Select control variables
   - Establish comparison groups

### For UK-Wide Analysis

4. **Obtain Scottish data**
   - Priority: SQA results, school census, SIMD
   - Source: statistics.gov.scot

5. **Complete Welsh coverage**
   - Priority: School performance, WIMD
   - Source: statswales.gov.wales

6. **Add Northern Ireland**
   - Priority: School results, NIMDM
   - Source: nisra.gov.uk

### Technical Setup

7. **Standardise formats**
   - Consistent column naming
   - Common geographic identifiers
   - Aligned time periods

8. **Create lookup tables**
   - LSOA to LA mapping
   - School URN to location
   - Qualification grade equivalences

---

## 7. Tool Recommendations

### Data Processing
- **Python**: pandas, geopandas for spatial joins
- **R**: tidyverse, sf for geographic analysis

### Visualisation
- **Mapping**: QGIS, Folium, Kepler.gl
- **Dashboards**: Plotly Dash, Streamlit

### Statistical Analysis
- **Multilevel modelling**: For school/LA nested data
- **Spatial statistics**: For geographic clustering

---

## File Locations

| File | Path |
|------|------|
| DfE data | `/Users/cliste/dev/dkit/semester_1/joint_project/datasets/dfe/` |
| GIS data | `/Users/cliste/dev/dkit/semester_1/joint_project/datasets/gis/` |
| ONS data | `/Users/cliste/dev/dkit/semester_1/joint_project/datasets/ons/` |
| UCAS data | `/Users/cliste/dev/dkit/semester_1/joint_project/datasets/ucas/` |
| Raw/IMD | `/Users/cliste/dev/dkit/semester_1/joint_project/datasets/raw/` |
