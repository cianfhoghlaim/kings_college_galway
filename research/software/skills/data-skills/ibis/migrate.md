---
description: Migrate code from pandas to Ibis - understand key differences and convert patterns.
---

# pandas to Ibis Migration Assistant

Help users migrate their pandas code to Ibis for better scalability and backend portability.

## Reference

Use `@/ibis-llms.txt` for comprehensive Ibis documentation.

## Key Differences

| Aspect | pandas | Ibis |
|--------|--------|------|
| **Execution** | Eager (immediate) | Lazy (deferred) |
| **Mutability** | In-place modification | Immutable (returns new) |
| **Data Location** | In memory | Stays in backend |
| **Row Index** | Has index | No index |
| **NULL** | Conflates NULL/NaN | Distinct NULL |
| **Memory** | Needs 5-10x data size | Only results to memory |

## Pattern Conversions

### Reading Data

```python
# pandas
import pandas as pd
df = pd.read_csv("data.csv")
df = pd.read_parquet("data.parquet")
df = pd.read_sql("SELECT * FROM table", connection)

# Ibis
import ibis
con = ibis.duckdb.connect()
t = con.read_csv("data.csv")
t = con.read_parquet("data.parquet")
t = con.table("table")  # From connected database
```

### In-Memory Data

```python
# pandas
df = pd.DataFrame({"a": [1, 2, 3], "b": ["x", "y", "z"]})

# Ibis
t = ibis.memtable({"a": [1, 2, 3], "b": ["x", "y", "z"]})
# or from existing pandas DataFrame
t = ibis.memtable(df)
```

### Column Selection

```python
# pandas
df[["col1", "col2"]]
df.loc[:, ["col1", "col2"]]

# Ibis
t.select("col1", "col2")
t.select(t.col1, t.col2)
```

### Row Filtering

```python
# pandas
df[df["value"] > 100]
df.query("value > 100")
df[(df["x"] > 0) & (df["y"] < 10)]

# Ibis
from ibis import _
t.filter(_.value > 100)
t.filter(_.x > 0, _.y < 10)  # AND
t.filter((_.x > 0) & (_.y < 10))
```

### Adding/Modifying Columns

```python
# pandas (modifies in place or returns copy)
df["revenue"] = df["quantity"] * df["price"]
df.assign(revenue=df["quantity"] * df["price"])

# Ibis (always returns new table)
t.mutate(revenue=_.quantity * _.price)
```

### Aggregation

```python
# pandas
df["value"].sum()
df["value"].mean()
df.agg({"value": "sum", "count": "size"})

# Ibis
t.value.sum()
t.value.mean()
t.aggregate(total=_.value.sum(), count=_.count())
```

### Groupby Aggregation

```python
# pandas
df.groupby("category")["value"].sum()
df.groupby("category").agg({"value": "sum", "count": "size"})

# Ibis
t.group_by("category").aggregate(total=_.value.sum())
t.group_by("category").aggregate(
    total=_.value.sum(),
    count=_.count()
)
```

### Multiple Aggregations by Condition

```python
# pandas (multiple passes)
total = df["sales"].sum()
us_sales = df[df["region"] == "US"]["sales"].sum()
big_deals = len(df[df["amount"] > 10000])

# Ibis (single pass with conditional aggregation!)
result = t.aggregate(
    total=_.sales.sum(),
    us_sales=_.sales.sum(where=_.region == "US"),
    big_deals=_.count(where=_.amount > 10000)
)
```

### Sorting

```python
# pandas
df.sort_values("value")
df.sort_values("value", ascending=False)
df.sort_values(["a", "b"], ascending=[True, False])

# Ibis
t.order_by("value")
t.order_by(ibis.desc("value"))
t.order_by(["a", ibis.desc("b")])
```

### Limiting Rows

```python
# pandas
df.head(10)
df.iloc[:10]

# Ibis
t.head(10)
t.limit(10)
```

### Joins

```python
# pandas
pd.merge(df1, df2, on="key")
pd.merge(df1, df2, left_on="k1", right_on="k2", how="left")

# Ibis
t1.join(t2, t1.key == t2.key)
t1.left_join(t2, t1.k1 == t2.k2)
```

### Conditional Logic

```python
# pandas
df["label"] = np.where(df["value"] > 0, "positive", "negative")
df["grade"] = pd.cut(df["score"], bins=[0, 60, 70, 80, 90, 100],
                     labels=["F", "D", "C", "B", "A"])

# Ibis
t.mutate(label=_.value.ifelse(_.value > 0, "positive", "negative"))
t.mutate(
    grade=ibis.cases(
        (_.score >= 90, "A"),
        (_.score >= 80, "B"),
        (_.score >= 70, "C"),
        (_.score >= 60, "D"),
        else_="F"
    )
)
```

### String Operations

```python
# pandas
df["name"].str.upper()
df["name"].str.contains("pattern")
df["name"].str.replace("old", "new")

# Ibis
t.name.upper()
t.name.contains("pattern")
t.name.replace("old", "new")
```

### Date Operations

```python
# pandas
df["date"].dt.year
df["date"].dt.month
df["date"].dt.dayofweek

# Ibis
t.date.year()
t.date.month()
t.date.day_of_week.index()
```

### Null Handling

```python
# pandas
df["value"].fillna(0)
df["value"].isna()
df.dropna(subset=["value"])

# Ibis
t.value.fill_null(0)
t.value.isnull()
t.filter(_.value.notnull())
```

### Window Functions

```python
# pandas
df.groupby("category")["value"].transform("sum")
df.groupby("category")["value"].cumsum()
df.groupby("category")["value"].shift(1)

# Ibis
t.group_by("category").mutate(total=_.value.sum())
t.group_by("category").order_by("date").mutate(cumsum=_.value.cumsum())
t.group_by("category").order_by("date").mutate(prev=_.value.lag())
```

### Renaming Columns

```python
# pandas
df.rename(columns={"old": "new"})

# Ibis
t.rename(new="old")
t.rename({"old": "new"})
```

### Dropping Columns

```python
# pandas
df.drop(columns=["col1", "col2"])

# Ibis
t.drop("col1", "col2")
```

### Type Casting

```python
# pandas
df["value"].astype(float)
df["date"].astype("datetime64[ns]")

# Ibis
t.value.cast("float64")
t.date.cast("timestamp")
```

### Getting Results

```python
# pandas - already in memory
result = df.groupby("category").sum()

# Ibis - must execute to get results
expr = t.group_by("category").aggregate(total=_.value.sum())
result = expr.to_pandas()  # Execute and convert to pandas
result = expr.execute()    # Execute with backend's default format
```

## Complete Migration Example

### Before (pandas)
```python
import pandas as pd
import numpy as np

# Read data
df = pd.read_parquet("sales.parquet")

# Filter
df = df[df["date"] >= "2024-01-01"]
df = df[df["status"] == "completed"]

# Add columns
df["revenue"] = df["quantity"] * df["price"]
df["year"] = df["date"].dt.year

# Conditional
df["size"] = np.where(df["quantity"] > 100, "large", "small")

# Aggregate
result = (
    df
    .groupby(["region", "year"])
    .agg({
        "revenue": "sum",
        "order_id": "count"
    })
    .rename(columns={"order_id": "order_count"})
    .sort_values("revenue", ascending=False)
    .head(10)
)
```

### After (Ibis)
```python
import ibis
from ibis import _

# Connect and read
con = ibis.duckdb.connect()
t = con.read_parquet("sales.parquet")

# Build expression (lazy - nothing executes yet)
result = (
    t
    # Filter
    .filter(_.date >= "2024-01-01", _.status == "completed")
    # Add columns
    .mutate(
        revenue=_.quantity * _.price,
        year=_.date.year(),
        size=(_.quantity > 100).ifelse("large", "small")
    )
    # Aggregate
    .group_by(["region", "year"])
    .aggregate(
        revenue=_.revenue.sum(),
        order_count=_.count()
    )
    # Sort and limit
    .order_by(ibis.desc("revenue"))
    .limit(10)
)

# Execute
df = result.to_pandas()
```

## Migration Tips

1. **Don't load all data first** - build the full expression, then execute once
2. **Replace loops with vectorized ops** - Ibis expressions are more efficient
3. **Use `.pipe()` for reusable functions** - same as pandas
4. **Test with small data** - use DuckDB with sample files
5. **Compare SQL output** - use `ibis.to_sql()` to verify logic

## Common Gotchas

### Forgetting to Execute
```python
# Wrong - expr is just an expression, not data
expr = t.filter(_.x > 0)
print(expr)  # Shows schema, not data

# Right - execute to get results
df = expr.to_pandas()
print(df)
```

### Expecting In-Place Modification
```python
# Wrong - doesn't modify t
t.mutate(new_col=_.x * 2)

# Right - assign the result
t = t.mutate(new_col=_.x * 2)
```

### Using Position-Based Indexing
```python
# pandas
df.iloc[0:5]
df.iloc[:, 2]

# Ibis - use names and explicit ordering
t.order_by("date").head(5)  # First 5 by date
t.select("column_name")     # Select by name
```

### Relying on Row Order
```python
# pandas - preserves insertion order
first = df.head(1)

# Ibis - order is undefined without order_by
first = t.order_by("date").head(1)
```

## Benefits After Migration

1. **Scalability** - Handle data larger than memory
2. **Performance** - Backend-optimized query execution
3. **Portability** - Same code on DuckDB, BigQuery, Snowflake
4. **Efficiency** - Only results come to memory

Now, what pandas code would you like to migrate?
