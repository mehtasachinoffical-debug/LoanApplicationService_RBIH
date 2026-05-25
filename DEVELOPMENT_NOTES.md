# Development Notes

## Overall Approach

## Key Design Decisions

- Age-at-maturity checks use `ceil(tenureMonths / 12)` for non-whole-year tenures. This is the conservative interpretation of "tenure in years"; for example, a 30-month loan is treated as 3 years.
- `EligibilityEvaluator` accepts a nullable EMI so the orchestration layer can evaluate credit and age rules before EMI is available, while still using the same evaluator after EMI calculation.
- EMI rejection rules overlap at the 50% and 60% income thresholds. When EMI is above 60%, only `EMI_EXCEEDS_60_PERCENT` is returned; `EMI_EXCEEDS_50_PERCENT_OF_INCOME` is returned only when EMI is greater than 50% and at most 60%.

## Trade-offs

## Assumptions

## Improvements
