package com.example.loanservice.service;

import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.LoanPurpose;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.dto.request.ApplicantRequest;
import com.example.loanservice.dto.request.LoanApplicationRequest;
import com.example.loanservice.dto.request.LoanRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityEvaluatorTest {

    private final EligibilityEvaluator evaluator = new EligibilityEvaluator();

    @Test
    void creditScoreBelow600IsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setCreditScore(599);

        List<RejectionReason> reasons = evaluator.evaluate(request, new BigDecimal("10000"));

        assertThat(reasons).containsExactly(RejectionReason.CREDIT_SCORE_TOO_LOW);
    }

    @Test
    void creditScore600IsAcceptedByCreditRule() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setCreditScore(600);

        List<RejectionReason> reasons = evaluator.evaluate(request, new BigDecimal("10000"));

        assertThat(reasons).doesNotContain(RejectionReason.CREDIT_SCORE_TOO_LOW);
    }

    @Test
    void ageAtMaturity65IsAccepted() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setAge(60);
        request.getLoan().setTenureMonths(60);

        List<RejectionReason> reasons = evaluator.evaluate(request, new BigDecimal("10000"));

        assertThat(reasons).doesNotContain(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
    }

    @Test
    void ageAtMaturityAbove65IsRejectedWithCeilingTenureYears() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setAge(60);
        request.getLoan().setTenureMonths(61);

        List<RejectionReason> reasons = evaluator.evaluate(request, new BigDecimal("10000"));

        assertThat(reasons).containsExactly(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
    }

    @Test
    void thirtyYearTenureEndingAtAge60IsAccepted() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setAge(30);
        request.getLoan().setTenureMonths(360);

        List<RejectionReason> reasons = evaluator.evaluate(request, new BigDecimal("10000"));

        assertThat(reasons).doesNotContain(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
    }

    @Test
    void thirtyYearTenureEndingAtAge66IsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setAge(36);
        request.getLoan().setTenureMonths(360);

        List<RejectionReason> reasons = evaluator.evaluate(request, new BigDecimal("10000"));

        assertThat(reasons).containsExactly(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
    }

    @Test
    void emiExactlyAt60PercentOfIncomeIsAccepted() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setMonthlyIncome(new BigDecimal("100000"));

        List<RejectionReason> reasons = evaluator.evaluate(request, new BigDecimal("60000.00"));

        assertThat(reasons).doesNotContain(RejectionReason.EMI_EXCEEDS_60_PERCENT);
    }

    @Test
    void emiAbove60PercentOfIncomeIsRejected() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setMonthlyIncome(new BigDecimal("100000"));

        List<RejectionReason> reasons = evaluator.evaluate(request, new BigDecimal("60010.00"));

        assertThat(reasons).containsExactly(RejectionReason.EMI_EXCEEDS_60_PERCENT);
    }

    @Test
    void evaluatesAllRulesAndReturnsEveryFailedReason() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setCreditScore(550);
        request.getApplicant().setAge(60);
        request.getApplicant().setMonthlyIncome(new BigDecimal("10000"));
        request.getLoan().setTenureMonths(120);

        List<RejectionReason> reasons = evaluator.evaluate(request, new BigDecimal("7000.00"));

        assertThat(reasons).containsExactly(
                RejectionReason.CREDIT_SCORE_TOO_LOW,
                RejectionReason.AGE_TENURE_LIMIT_EXCEEDED,
                RejectionReason.EMI_EXCEEDS_60_PERCENT);
    }

    @Test
    void nullEmiSkipsOnlyEmiRule() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setCreditScore(550);
        request.getApplicant().setAge(60);
        request.getLoan().setTenureMonths(120);

        List<RejectionReason> reasons = evaluator.evaluate(request, null);

        assertThat(reasons).containsExactly(
                RejectionReason.CREDIT_SCORE_TOO_LOW,
                RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
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
}
