# Implementation Notes

## Summary

This document describes key implementation decisions and observable behaviors of the solution.
The focus is on filtering semantics, facet calculation, and price range matching configuration.

## Implementation Details

During the implementation, several practical aspects required additional attention and refinement:

1. JSON serialization and deserialization for REST endpoint required extra configuration to work correctly.
2. The application startup configuration was adjusted to ensure that `sbt run` works correctly when the application is launched from the command line.
3. Some existing tests required fixes, and additional test cases were introduced to properly cover the expected behavior.
4. Two different price range matching policies were implemented. The active policy is controlled by the `priceMatchingMode` configuration parameter.
5. All requirements related to filtering semantics and facet element counting are implemented and behave as expected.

---

## Price Range Matching Modes

The behavior of price range matching is configurable and depends on the selected `priceMatchingMode`.

### PriceMatchingMode.ExactMode

This mode uses a non-overlapping set of price ranges.  
A price always matches exactly one range according to the condition:

`lower <= price < upper`

This mode makes it easier to reason about filtering rules, the interaction between different filter categories, and the correctness of facet count calculations.

**Examples:**
- `12.99` -> `PriceRange(10 - 15)`
- `10.00` -> `PriceRange(10 - 15)`

---

### PriceMatchingMode.FairMode

This mode is designed to be more predictable from a user perspective.

A price may match one or two ranges. In this mode:
- the price may be rounded up to one-cent precision
- both the lower and upper range boundaries are inclusive:  
  `lower <= price <= upper`

**Examples:**
- `10.00` -> `PriceRange(5 - 10)`, `PriceRange(10 - 15)`
- `14.99` -> `PriceRange(10 - 15)`, `PriceRange(15 - 20)` - is rounded up to 15.00
- `12.99` -> `PriceRange(10 - 15)`

---

## Filtering options visibility and results counting

Facet visibility is implemented in a bidirectional and context-aware manner.  
Selected **year** items control the visibility of **price range** items, and vice versa.

- When one or more years are selected, only the price ranges applicable to the selected years are displayed.
- When one or more price ranges are selected, only the years corresponding to albums within those price ranges remain visible.

This behavior ensures that facet options always reflect the current filtering context and prevents users from selecting filter combinations that would yield no results.

---

### Example: Single year selected (2025)

Total **19** items in the price facet group, which matches the total number of albums released in **2025**.

```
price
[ ] 0 - 5 (1)
[ ] 5 - 10 (12)
[ ] 10 - 15 (6)

year
[ ] 2026 (12)
[x] 2025 (19)
[ ] 2024 (7)
```

---

### Example: Two years selected (2025 + 2024)

Total **26** items in the price facet group, which equals the combined total for **2025** and **2024**.

```
price
[ ] 0 - 5 (1)
[ ] 5 - 10 (18)
[ ] 10 - 15 (6)
[ ] 15 - 20 (1)

year
[ ] 2026 (12)
[x] 2025 (19)
[x] 2024 (7)
```

---

### Example: Price filter applied (5–10)

Selecting a single price range updates the year facet counts accordingly.  
The total count for the selected years (**2025 + 2024**) becomes **18**.

```
price
[ ] 0 - 5 (1)
[x] 5 - 10 (18)
[ ] 10 - 15 (6)
[ ] 15 - 20 (1)

year
[ ] 2026 (12)
[x] 2025 (12)
[x] 2024 (6)
```

---

The examples above are based on `PriceMatchingMode.ExactMode`.  
When `PriceMatchingMode.FairMode` is used, a single item may belong to multiple price ranges, and the resulting facet counts will differ accordingly.

PriceMatchingMode.ExactMode is enabled by default and application can be started using standard command: 

```
sbt run
```

PriceMatchingMode.FairMode can be activated by setting the environment variable PRICE_MATCHING_MODE=FAIR.
In this case, the application can be started using the following command:

```
PRICE_MATCHING_MODE=FAIR sbt run
```

Test cases cover the both filtering modes.

## Potential Improvements

As a potential area for further development, the search functionality could be extended with more advanced text matching strategies commonly used in search systems.

For example, the following approaches could be introduced:
- exact word matching
- stemming-based matching, where different morphological forms of a term are reduced to a common base form

---