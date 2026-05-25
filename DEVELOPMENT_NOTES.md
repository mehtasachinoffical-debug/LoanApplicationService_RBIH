# Development Notes

## Overall Approach

The service follows a layered Spring Boot architecture: controllers handle HTTP concerns, services own workflow and business rules, domain classes model persistence and enums, and repositories handle audit storage. Stateless calculators and classifiers keep the financial and risk logic small, deterministic, and easy to unit test.

Loan applications are processed by one orchestration service that composes the risk band classifier, interest rate calculator, EMI calculator, eligibility rules, response DTO mapping, and audit persistence. Every decision path writes a `LoanApplicationRecord` so approved and rejected outcomes can be inspected later.

## Key Design Decisions

- Financial values use `BigDecimal` throughout. Calculators return money/rate values at scale 2 with `RoundingMode.HALF_UP`, while EMI intermediate calculations use higher precision before final rounding.
- Risk band classification, interest rate calculation, EMI calculation, and eligibility evaluation are separate stateless `@Component`s. This keeps each rule set independently testable and keeps the orchestrator readable.
- The orchestration service splits early rejections from EMI-based rejections. This avoids invoking risk classification for credit scores below 600 and avoids computing EMI when the application is already rejected by credit or age-tenure rules.
- `EMI_EXCEEDS_60_PERCENT` and `EMI_EXCEEDS_50_PERCENT_OF_INCOME` are distinct audit reasons because they represent separate rules. When EMI is above 60%, only the stricter 60% reason is returned.
- `@JsonInclude` is applied to response fields rather than the whole response object so rejected responses preserve `riskBand: null` while omitting unused `offer` data.
- Age-at-maturity checks use `ceil(tenureMonths / 12)` for non-whole-year tenures. This is the conservative interpretation of "tenure in years"; for example, a 30-month loan is treated as 3 years.
- `EligibilityEvaluator` accepts a nullable EMI so the orchestration layer can evaluate credit and age rules before EMI is available, while still using the same evaluator after EMI calculation.
- EMI rejection rules overlap at the 50% and 60% income thresholds. When EMI is above 60%, only `EMI_EXCEEDS_60_PERCENT` is returned; `EMI_EXCEEDS_50_PERCENT_OF_INCOME` is returned only when EMI is greater than 50% and at most 60%.

## Trade-offs

- The project uses in-memory H2 for audit persistence to keep local setup simple. A production service would use a durable database such as PostgreSQL with schema migrations.
- Rejection evaluation favors clear feedback and auditability over fail-fast execution, so some paths collect multiple failed rules.
- `ceil(tenureMonths / 12)` is conservative for age-tenure validation. Other interpretations, such as exact months or floor years, could be valid depending on policy.
- Static thresholds and rates are hard-coded for clarity in this implementation. Configuration-backed policy values would be easier to operate in production.

## Assumptions

- Loan amounts and income are represented in rupees.
- Each submitted application is independent; no idempotency key or duplicate detection is applied.
- Audit records are append-only from the API perspective; there is no update or retrieval endpoint in this phase.
- Rejections are successful business outcomes, so the API returns `201 Created` for both approved and rejected decisions.

## Improvements

- Use PostgreSQL or another production database with Flyway or Liquibase migrations.
- Add `GET /applications/{id}` for audit retrieval.
- Externalize rate tables, thresholds, and age-tenure policy into `application.yml`.
- Add OpenAPI/Swagger documentation.
- Replace direct `Math.ceil` usage with an explicit policy class for the age-tenure rule.
- Add idempotency keys to prevent duplicate decisions for the same applicant and loan combination.
- Add structured logging with correlation IDs.
