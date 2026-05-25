# Development Notes

## Overall Approach

## Key Design Decisions

- Age-at-maturity checks use `ceil(tenureMonths / 12)` for non-whole-year tenures. This is the conservative interpretation of "tenure in years"; for example, a 30-month loan is treated as 3 years.
- `EligibilityEvaluator` accepts a nullable EMI so the orchestration layer can evaluate credit and age rules before EMI is available, while still using the same evaluator after EMI calculation.

## Trade-offs

## Assumptions

## Improvements
