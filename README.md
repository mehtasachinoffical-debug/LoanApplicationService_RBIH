# Loan Service

Spring Boot REST service for evaluating loan applications, returning approved offers or rejection reasons and storing every decision in an in-memory H2 audit database.

## Prerequisites

- Java 17+
- Maven

## Build

```sh
mvn clean install
```

## Run

```sh
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

## Approved Example

```sh
curl -s -X POST http://localhost:8080/applications \
  -H 'Content-Type: application/json' \
  -d '{
    "applicant": {
      "name": "Asha Rao",
      "age": 30,
      "monthlyIncome": 125000,
      "employmentType": "SALARIED",
      "creditScore": 780
    },
    "loan": {
      "amount": 500000,
      "tenureMonths": 36,
      "purpose": "PERSONAL"
    }
  }'
```

## Rejected Example

```sh
curl -s -X POST http://localhost:8080/applications \
  -H 'Content-Type: application/json' \
  -d '{
    "applicant": {
      "name": "Neha Shah",
      "age": 30,
      "monthlyIncome": 125000,
      "employmentType": "SALARIED",
      "creditScore": 580
    },
    "loan": {
      "amount": 500000,
      "tenureMonths": 36,
      "purpose": "PERSONAL"
    }
  }'
```

See [DEVELOPMENT_NOTES.md](DEVELOPMENT_NOTES.md) for design decisions, trade-offs, assumptions, and future improvements.
