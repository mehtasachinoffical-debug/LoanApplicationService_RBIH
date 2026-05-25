package com.example.loanservice.dto.request;

import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.LoanPurpose;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoanApplicationRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    private LoanApplicationRequest validRequest() {
        ApplicantRequest applicant = new ApplicantRequest();
        applicant.setName("Asha Rao");
        applicant.setAge(35);
        applicant.setMonthlyIncome(new BigDecimal("125000"));
        applicant.setEmploymentType(EmploymentType.SALARIED);
        applicant.setCreditScore(760);

        LoanRequest loan = new LoanRequest();
        loan.setAmount(new BigDecimal("750000"));
        loan.setTenureMonths(120);
        loan.setPurpose(LoanPurpose.HOME);

        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicant(applicant);
        request.setLoan(loan);
        return request;
    }

    @Test
    void validRequestHasNoViolations() {
        Set<ConstraintViolation<LoanApplicationRequest>> violations = validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    void ageBelow21IsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setAge(20);

        assertHasViolationOn(validator.validate(request), "applicant.age");
    }

    @Test
    void ageAbove60IsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setAge(61);

        assertHasViolationOn(validator.validate(request), "applicant.age");
    }

    @Test
    void age21AndAge60AreAccepted() {
        LoanApplicationRequest minimumAgeRequest = validRequest();
        minimumAgeRequest.getApplicant().setAge(21);

        LoanApplicationRequest maximumAgeRequest = validRequest();
        maximumAgeRequest.getApplicant().setAge(60);

        assertThat(validator.validate(minimumAgeRequest)).isEmpty();
        assertThat(validator.validate(maximumAgeRequest)).isEmpty();
    }

    @Test
    void creditScoreBelow300IsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setCreditScore(299);

        assertHasViolationOn(validator.validate(request), "applicant.creditScore");
    }

    @Test
    void creditScoreAbove900IsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setCreditScore(901);

        assertHasViolationOn(validator.validate(request), "applicant.creditScore");
    }

    @Test
    void loanAmountBelow10000IsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getLoan().setAmount(new BigDecimal("9999.99"));

        assertHasViolationOn(validator.validate(request), "loan.amount");
    }

    @Test
    void loanAmountAbove5000000IsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getLoan().setAmount(new BigDecimal("5000000.01"));

        assertHasViolationOn(validator.validate(request), "loan.amount");
    }

    @Test
    void tenureBelow6IsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getLoan().setTenureMonths(5);

        assertHasViolationOn(validator.validate(request), "loan.tenureMonths");
    }

    @Test
    void tenureAbove360IsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getLoan().setTenureMonths(361);

        assertHasViolationOn(validator.validate(request), "loan.tenureMonths");
    }

    @Test
    void monthlyIncomeZeroIsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setMonthlyIncome(BigDecimal.ZERO);

        assertHasViolationOn(validator.validate(request), "applicant.monthlyIncome");
    }

    @Test
    void nullApplicantIsRejected() {
        LoanApplicationRequest request = validRequest();
        request.setApplicant(null);

        assertHasViolationOn(validator.validate(request), "applicant");
    }

    @Test
    void nullLoanIsRejected() {
        LoanApplicationRequest request = validRequest();
        request.setLoan(null);

        assertHasViolationOn(validator.validate(request), "loan");
    }

    private void assertHasViolationOn(Set<ConstraintViolation<LoanApplicationRequest>> violations,
                                      String propertyPath) {
        assertThat(violations)
                .anySatisfy(violation ->
                        assertThat(violation.getPropertyPath().toString()).isEqualTo(propertyPath));
    }
}
