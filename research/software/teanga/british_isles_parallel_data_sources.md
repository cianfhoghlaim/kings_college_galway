# Parallel Education Data Sources for the British Isles

## Overview

This document provides guidance on obtaining equivalent education datasets for Scotland, Wales, Northern Ireland, and the Republic of Ireland to enable UK-wide and British Isles comparative analysis.

---

## Current Coverage Gap

| Region | Current Data | Gap |
|--------|--------------|-----|
| England | Full coverage (DfE, GIS, IMD) | None |
| Wales | Partial (GIS schools, UCAS) | School performance, Welsh IMD |
| Scotland | UCAS only | School performance, SQA results, SIMD |
| Northern Ireland | UCAS only | School performance, NIMDM |
| Republic of Ireland | None | All education data |

---

## Scotland

### Education System Differences
- **Qualifications**: National 5 (≈GCSE), Higher (≈AS), Advanced Higher (≈A-Level)
- **Framework**: Curriculum for Excellence
- **Ages**: S4 (National 5), S5 (Higher), S6 (Advanced Higher)

### Data Sources

| Data Type | Source | URL |
|-----------|--------|-----|
| **School Performance** | Scottish Government | https://statistics.gov.scot |
| **Exam Results** | SQA | https://www.sqa.org.uk/sqa/48269.html |
| **School Information** | Education Scotland | https://education.gov.scot |
| **School Census** | Scottish Government | https://www.gov.scot/collections/school-education-statistics/ |
| **Deprivation (SIMD)** | Scottish Government | https://www.gov.scot/collections/scottish-index-of-multiple-deprivation-2020/ |
| **GIS/Boundaries** | Spatial Hub Scotland | https://data.spatialhub.scot |
| **Demographics** | National Records Scotland | https://www.nrscotland.gov.uk |

### Key Datasets to Request

1. **Attainment Statistics**
   - Search: "Attainment and Initial Leaver Destinations"
   - Includes: National 5/Higher pass rates by school, LA, demographics

2. **School Level Data**
   - Search: "Summary Statistics for Schools in Scotland"
   - Includes: Pupil numbers, FSM, ethnicity, attendance

3. **SIMD 2020**
   - 7 domains: Income, Employment, Education, Health, Access, Crime, Housing
   - Available at Data Zone level (≈LSOA equivalent)

### Geographic Units
- **Data Zones**: 6,976 areas (equivalent to LSOA)
- **Intermediate Zones**: 1,279 areas (equivalent to MSOA)
- **Council Areas**: 32 local authorities

---

## Wales

### Education System Differences
- **Qualifications**: GCSEs and A-Levels (WJEC exam board)
- **Framework**: Curriculum for Wales (from 2022)
- **Key feature**: Welsh-medium education strand
- **Note**: Key Stage testing abolished

### Data Sources

| Data Type | Source | URL |
|-----------|--------|-----|
| **School Performance** | StatsWales | https://statswales.gov.wales/Catalogue/Education-and-Skills |
| **Exam Results** | Qualifications Wales | https://qualificationswales.org |
| **School Information** | My Local School | https://mylocalschool.gov.wales |
| **Deprivation (WIMD)** | Welsh Government | https://statswales.gov.wales/Catalogue/Community-Safety-and-Social-Inclusion/Welsh-Index-of-Multiple-Deprivation |
| **GIS/Boundaries** | DataMapWales | https://datamap.gov.wales |
| **Demographics** | StatsWales | https://statswales.gov.wales |

### Key Datasets to Request

1. **Examination Results**
   - GCSE and A-Level results by school
   - Available via StatsWales "Examination results" category

2. **School Census**
   - Pupil numbers, FSM eligibility, Welsh language
   - Annual publication in "Schools" category

3. **WIMD 2019**
   - 8 domains (includes "Community Safety" vs England's "Crime")
   - Available at LSOA level (1,909 areas)

### Geographic Units
- **LSOAs**: 1,909 areas (same definition as England)
- **MSOAs**: 410 areas
- **Local Authorities**: 22 councils

### Welsh-Specific Considerations
- Track Welsh-medium vs English-medium school performance
- Cymraeg (Welsh language) as subject area
- Different performance measures post-2019

---

## Northern Ireland

### Education System Differences
- **Qualifications**: GCSEs and A-Levels (CCEA exam board)
- **Key feature**: Selective grammar school system (11+ transfer test)
- **School types**: Controlled, Maintained, Integrated, Irish-medium

### Data Sources

| Data Type | Source | URL |
|-----------|--------|-----|
| **School Performance** | Department of Education NI | https://www.education-ni.gov.uk/topics/statistics-and-research |
| **Exam Results** | CCEA | https://ccea.org.uk |
| **School Census** | NISRA | https://www.nisra.gov.uk |
| **Deprivation (NIMDM)** | NISRA | https://www.nisra.gov.uk/statistics/deprivation |
| **GIS/Boundaries** | OpenDataNI | https://www.opendatani.gov.uk |
| **Demographics** | NISRA Census | https://www.nisra.gov.uk/statistics/census |

### Key Datasets to Request

1. **School Performance Tables**
   - GCSE and A-Level results by school
   - Published annually by DE NI

2. **School Census**
   - "Annual enrolments at schools and funded pre-school education"
   - Includes FSM, SEN, religion, Irish-medium

3. **NIMDM 2017**
   - 7 domains matching England
   - Available at Super Output Area level (890 areas)

### Geographic Units
- **Super Output Areas**: 890 (larger than English LSOAs)
- **Local Government Districts**: 11 councils
- **Assembly Areas**: 18 constituencies

### NI-Specific Considerations
- Grammar vs non-selective school comparison
- Controlled (Protestant) vs Maintained (Catholic) vs Integrated
- Irish-medium schools as distinct category
- Transfer test (11+) effects on attainment

---

## Republic of Ireland

### Education System Differences
- **Qualifications**: Junior Certificate (age 15), Leaving Certificate (age 18)
- **Framework**: Different to UK National Curriculum
- **Grading**: H1-H8 (Higher), O1-O8 (Ordinary) from 2017

### Data Sources

| Data Type | Source | URL |
|-----------|--------|-----|
| **School Information** | Department of Education | https://www.gov.ie/en/organisation/department-of-education/ |
| **Exam Results** | State Examinations Commission | https://www.examinations.ie |
| **Open Data** | data.gov.ie | https://data.gov.ie |
| **Deprivation** | Pobal | https://www.pobal.ie (HP Deprivation Index) |
| **Demographics** | CSO | https://www.cso.ie |
| **GIS/Boundaries** | OSi | https://www.osi.ie |

### Key Datasets

1. **State Examinations Statistics**
   - Junior Cert and Leaving Cert results
   - Available at national and subject level

2. **School Information**
   - Search data.gov.ie for "primary schools" and "post-primary schools"
   - Includes location, DEIS status (disadvantage indicator)

3. **Pobal HP Deprivation Index**
   - Based on Census Small Areas
   - Different methodology to UK IMD

### Geographic Units
- **Small Areas**: 18,641 (similar to LSOA)
- **Electoral Divisions**: 3,409
- **Local Authority Areas**: 31 counties/cities

### Ireland-Specific Considerations
- DEIS (Delivering Equality of Opportunity in Schools) status
- Fee-paying vs non-fee-paying schools
- Gaeltacht (Irish-speaking areas) schools
- Transition Year (optional year between Junior and Leaving Cert)

---

## Cross-Nation Comparability

### Challenges

| Issue | Impact | Mitigation |
|-------|--------|------------|
| Different exam systems | Cannot directly compare grades | Use percentile ranks or grade distributions |
| Different subjects | Curriculum content varies | Focus on broad domains (literacy, numeracy, STEM) |
| Different deprivation indices | Scores not comparable | Use decile ranks (1-10) |
| Different geographic units | Areas not aligned | Aggregate to comparable levels |
| Different school structures | Selection effects | Control for school type |

### Recommended Approaches

1. **For Attainment Comparison**
   - Use percentage achieving threshold (e.g., 5 GCSEs A*-C equivalent)
   - Convert to percentile ranks within each nation
   - Focus on outcome measures (university entry, employment)

2. **For Deprivation Analysis**
   - Use decile rankings (most deprived 10%, etc.)
   - Apply within-nation standardisation
   - Consider composite measures (e.g., FSM rates)

3. **For Geographic Analysis**
   - Aggregate to local authority/council level
   - Use population-weighted measures
   - Create cross-border regions for comparison

4. **For Longitudinal Analysis**
   - Account for qualification reform years
   - Note COVID disruption (2020-2021)
   - Use consistent time periods across nations

---

## UK-Wide Data Sources

### Already UK-Wide in Your Dataset

1. **UCAS** - University admissions (your best cross-UK source)
2. **ONS Census** - Demographics at LSOA/Data Zone level

### Additional UK-Wide Sources

| Source | Coverage | URL |
|--------|----------|-----|
| OECD PISA | Standardised international tests | https://www.oecd.org/pisa/ |
| HESA | Higher education statistics | https://www.hesa.ac.uk |
| Labour Force Survey | Employment/qualifications | https://www.ons.gov.uk |
| Annual Population Survey | Regional education levels | https://www.ons.gov.uk |

---

## Data Request Checklist

### For Each Nation, Obtain:

- [ ] School-level performance data (equivalent to DfE KS4/KS5)
- [ ] School census data (pupil characteristics, FSM)
- [ ] Establishment data with geographic coordinates
- [ ] Deprivation index at small area level
- [ ] Census demographics at small area level
- [ ] Documentation/metadata for all datasets

### Format Preferences

- CSV for data files (consistent with existing datasets)
- Ensure geographic identifiers (LSOA/Data Zone codes) are included
- Request time series where available
- Obtain data dictionaries/codebooks

---

## Contact Points

| Nation | Primary Contact |
|--------|-----------------|
| Scotland | statistics.enquiries@gov.scot |
| Wales | stats.educ@gov.wales |
| Northern Ireland | statistics@education-ni.gov.uk |
| Republic of Ireland | info@education.gov.ie |

---

## Recommended Priority Order

1. **Scotland** - Largest gap, well-organised open data
2. **Wales** - Partial coverage exists, straightforward to complete
3. **Northern Ireland** - Smaller dataset, unique characteristics
4. **Republic of Ireland** - Different system, optional for UK-only analysis
