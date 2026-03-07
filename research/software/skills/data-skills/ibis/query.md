---
description: Build Ibis queries with best practices - filtering, aggregation, joins, and window functions.
---

# Ibis Query Builder Assistant

Help users build efficient Ibis queries using deferred execution and best practices.

## Your Approach

1. **Understand the goal** - What data transformation is needed?
2. **Check the schema** - Know the column types before building
3. **Build incrementally** - Chain operations logically
4. **Optimize early** - Filter before joins and aggregations
5. **Use appropriate patterns** - Selectors, window functions, conditionals

## Reference

Use `@/ibis-llms.txt` for comprehensive API documentation.

## Query Building Patterns

### Basic Filter-Mutate-Aggregate
```python
from ibis import _

result = (
    t
    .filter(_.status == "active", _.date >= "2024-01-01")
    .mutate(revenue=_.quantity * _.price)
    .group_by("region")
    .aggregate(
        total_revenue=_.revenue.sum(),
        order_count=_.count()
    )
    .order_by(ibis.desc("total_revenue"))
)
```

### Multiple Conditions
```python
# AND (multiple arguments or &)
t.filter(_.x > 0, _.y < 100)
t.filter((_.x > 0) & (_.y < 100))

# OR
t.filter((_.status == "A") | (_.status == "B"))

# IN list
t.filter(_.category.isin(["A", "B", "C"]))

# Between
t.filter(_.value.between(10, 100))

# Null checks
t.filter(_.email.notnull())
```

### Conditional Logic
```python
# Simple if-else
t.mutate(label=_.is_active.ifelse("Active", "Inactive"))

# Multiple cases
t.mutate(
    grade=ibis.cases(
        (_.score >= 90, "A"),
        (_.score >= 80, "B"),
        (_.score >= 70, "C"),
        else_="F"
    )
)
```

### Aggregations

```python
# Basic aggregates
t.aggregate(
    count=_.count(),
    total=_.value.sum(),
    average=_.value.mean(),
    minimum=_.value.min(),
    maximum=_.value.max()
)

# Grouped aggregation
t.group_by("category").aggregate(total=_.value.sum())

# Multiple group keys
t.group_by(["region", "year"]).aggregate(total=_.sales.sum())

# Conditional aggregation (single pass!)
t.group_by("region").aggregate(
    total=_.sales.sum(),
    us_only=_.sales.sum(where=_.country == "US"),
    big_deals=_.count(where=_.amount > 10000)
)
```

### Joins

```python
# Inner join
t1.join(t2, t1.key == t2.key)

# Left join with column selection
(
    t1.left_join(t2, t1.id == t2.id)
    .select(t1.name, t2.value)  # Avoid column collisions
)

# Multiple conditions
t1.join(t2, (t1.key == t2.key) & (t1.date <= t2.date))

# Anti-join (NOT EXISTS)
customers.anti_join(orders, customers.id == orders.customer_id)

# Self-join
t_alias = t.view()
t.join(t_alias, _.parent_id == t_alias.id)
```

### Window Functions

```python
# Running total within groups
t.group_by("account").order_by("date").mutate(
    running_total=_.amount.sum()
)

# Ranking
t.mutate(
    rank=ibis.row_number().over(
        group_by=_.category,
        order_by=ibis.desc(_.value)
    )
)

# Top N per group
(
    t.mutate(
        rn=ibis.row_number().over(
            group_by=_.category,
            order_by=ibis.desc(_.value)
        )
    )
    .filter(_.rn <= 3)
    .drop("rn")
)

# Lag/Lead
t.group_by("user").order_by("date").mutate(
    prev=_.value.lag(),
    next=_.value.lead(),
    change=_.value - _.value.lag()
)

# Rolling window
window = ibis.window(
    group_by="category",
    order_by="date",
    preceding=6,  # 7-day window
    following=0
)
t.mutate(rolling_avg=_.value.mean().over(window))
```

### Selectors for Bulk Operations

```python
import ibis.selectors as s

# Select by type
t.select(s.numeric())
t.select(s.of_type("string"))

# Select by name pattern
t.select(s.startswith("bill"))
t.select(s.matches(r".*_id$"))

# Combine selectors
t.select(s.numeric() & ~s.cols("year"))

# Apply across multiple columns
t.mutate(
    s.across(
        s.numeric(),
        (_ - _.mean()) / _.std(),
        names="{col}_zscore"
    )
)
```

## Query Optimization Tips

1. **Filter early** - Reduce rows before joins/aggregations
2. **Select needed columns** - Don't load unnecessary data
3. **Use conditional aggregations** - One pass instead of multiple queries
4. **Cache intermediate results** - Use `.cache()` for reuse
5. **Filter before joins** - Apply filters to both sides

```python
# Good: Filter early
result = (
    t.filter(_.is_active)  # Filter first
    .join(t2.filter(_.status == "valid"), ...)  # Filter both sides
    .select(...)  # Only needed columns
    .aggregate(...)
)

# Bad: Load everything
result = (
    t.join(t2, ...)  # Full table join
    .to_pandas()  # All data to memory
    .query("is_active")  # Filter in pandas
)
```

## Debugging Queries

```python
# View generated SQL
print(ibis.to_sql(expr))
ibis.show_sql(expr)  # Pretty-printed

# Check schema
print(t.schema())

# Preview results
print(expr.limit(5).to_pandas())

# Inspect expression tree
expr.visualize()  # Requires graphviz
```

## Complete Example

```python
import ibis
from ibis import _
import ibis.selectors as s

con = ibis.duckdb.connect("sales.db")
orders = con.table("orders")
products = con.table("products")

analysis = (
    orders
    # Filter to relevant data
    .filter(
        _.order_date >= "2024-01-01",
        _.status.isin(["completed", "shipped"])
    )
    # Join with products
    .join(products, _.product_id == products.id)
    # Add computed columns
    .mutate(
        revenue=_.quantity * products.price,
        margin=_.revenue * (1 - products.cost_ratio)
    )
    # Add window calculations
    .group_by(_.customer_id)
    .mutate(
        customer_rank=ibis.row_number().over(
            order_by=ibis.desc(_.revenue)
        )
    )
    # Aggregate by category
    .group_by(products.category)
    .aggregate(
        total_revenue=_.revenue.sum(),
        total_margin=_.margin.sum(),
        order_count=_.count(),
        top_customer_revenue=_.revenue.sum(where=_.customer_rank == 1)
    )
    # Final sort
    .order_by(ibis.desc("total_revenue"))
)

# Debug
print(ibis.to_sql(analysis))

# Execute
df = analysis.to_pandas()
```

Now, what query would you like to build?
