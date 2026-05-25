package com.example.loanservice.service;

import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.LoanApplicationRecord;
import com.example.loanservice.domain.LoanPurpose;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import com.example.loanservice.dto.request.ApplicantRequest;
import com.example.loanservice.dto.request.LoanApplicationRequest;
import com.example.loanservice.dto.request.LoanRequest;
import com.example.loanservice.dto.response.LoanApplicationResponse;
import com.example.loanservice.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private RiskBandClassifier riskBandClassifier;

    @Mock
    private InterestRateCalculator interestRateCalculator;

    @Mock
    private EmiCalculator emiCalculator;

    @Mock
    private LoanApplicationRepository repository;

    private LoanApplicationService service;

    @BeforeEach
    void setUp() {
        service = new LoanApplicationService(
                riskBandClassifier, interestRateCalculator, emiCalculator, repository);
    }

    @Test
    void approvedHappyPathReturnsOfferAndPersistsDecision() {
        LoanApplicationRequest request = validRequest();
        when(riskBandClassifier.classify(780)).thenReturn(RiskBand.LOW);
        when(interestRateCalculator.calculate(RiskBand.LOW, EmploymentType.SALARIED, new BigDecimal("500000")))
                .thenReturn(new BigDecimal("12.00"));
        when(emiCalculator.calculateEmi(new BigDecimal("500000"), new BigDecimal("12.00"), 36))
                .thenReturn(new BigDecimal("16607.15"));
        when(emiCalculator.totalPayable(new BigDecimal("16607.15"), 36))
                .thenReturn(new BigDecimal("597857.40"));

        LoanApplicationResponse response = service.process(request);

        assertThat(response.getApplicationId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(response.getRiskBand()).isEqualTo(RiskBand.LOW);
        assertThat(response.getOffer().getInterestRate()).isEqualByComparingTo("12.00");
        assertThat(response.getOffer().getEmi()).isEqualByComparingTo("16607.15");
        assertThat(response.getOffer().getTotalPayable()).isEqualByComparingTo("597857.40");
        assertThat(response.getRejectionReasons()).isNull();

        LoanApplicationRecord saved = verifySavedOnce();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(saved.getApplicationId()).isEqualTo(response.getApplicationId());
        assertThat(saved.getRiskBand()).isEqualTo(RiskBand.LOW);
        assertThat(saved.getInterestRate()).isEqualByComparingTo("12.00");
        assertThat(saved.getEmi()).isEqualByComparingTo("16607.15");
        assertThat(saved.getTotalPayable()).isEqualByComparingTo("597857.40");
        assertThat(saved.getRejectionReasons()).isEmpty();
    }

    @Test
    void lowCreditScoreRejectsAndDoesNotCallRiskClassifier() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setCreditScore(580);

        LoanApplicationResponse response = service.process(request);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(response.getRejectionReasons()).containsExactly(RejectionReason.CREDIT_SCORE_TOO_LOW);
        verify(riskBandClassifier, never()).classify(anyInt());
        verifyNoInteractions(interestRateCalculator, emiCalculator);

        LoanApplicationRecord saved = verifySavedOnce();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(saved.getRejectionReasons()).containsExactly(RejectionReason.CREDIT_SCORE_TOO_LOW);
    }

    @Test
    void ageAtMaturityAbove65Rejects() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setAge(60);
        request.getLoan().setTenureMonths(120);

        LoanApplicationResponse response = service.process(request);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(response.getRejectionReasons()).containsExactly(
                RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
        verifyNoInteractions(riskBandClassifier, interestRateCalculator, emiCalculator);

        LoanApplicationRecord saved = verifySavedOnce();
        assertThat(saved.getRejectionReasons()).containsExactly(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
    }

    @Test
    void bothEarlyReasonsAreReturnedInCanonicalOrder() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setCreditScore(580);
        request.getApplicant().setAge(60);
        request.getLoan().setTenureMonths(120);

        LoanApplicationResponse response = service.process(request);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(response.getRejectionReasons()).containsExactly(
                RejectionReason.CREDIT_SCORE_TOO_LOW,
                RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
        verifyNoInteractions(riskBandClassifier, interestRateCalculator, emiCalculator);
        verifySavedOnce();
    }

    @Test
    void highEmiRejectsWith60PercentReason() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setMonthlyIncome(new BigDecimal("25000"));
        stubRiskRateAndEmi(new BigDecimal("20000.00"));

        LoanApplicationResponse response = service.process(request);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(response.getRejectionReasons()).containsExactly(
                RejectionReason.EMI_EXCEEDS_60_PERCENT);
        assertThat(response.getOffer()).isNull();
        verify(emiCalculator, never()).totalPayable(any(), anyInt());
        verifySavedOnce();
    }

    @Test
    void midRangeEmiRejectsWith50PercentReasonOnly() {
        LoanApplicationRequest request = validRequest();
        request.getApplicant().setMonthlyIncome(new BigDecimal("100000"));
        stubRiskRateAndEmi(new BigDecimal("55000.00"));

        LoanApplicationResponse response = service.process(request);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(response.getRejectionReasons()).containsExactly(
                RejectionReason.EMI_EXCEEDS_50_PERCENT_OF_INCOME);
        assertThat(response.getRejectionReasons()).doesNotContain(
                RejectionReason.EMI_EXCEEDS_60_PERCENT);
        verifySavedOnce();
    }

    @Test
    void generatedApplicationIdsAreNonNullAndUniqueAcrossInvocations() {
        LoanApplicationRequest first = validRequest();
        first.getApplicant().setCreditScore(580);
        LoanApplicationRequest second = validRequest();
        second.getApplicant().setCreditScore(580);

        LoanApplicationResponse firstResponse = service.process(first);
        LoanApplicationResponse secondResponse = service.process(second);

        assertThat(firstResponse.getApplicationId()).isNotNull();
        assertThat(secondResponse.getApplicationId()).isNotNull();
        assertThat(firstResponse.getApplicationId()).isNotEqualTo(secondResponse.getApplicationId());
        verify(repository, times(2)).save(any(LoanApplicationRecord.class));
    }

    private void stubRiskRateAndEmi(BigDecimal emi) {
        when(riskBandClassifier.classify(780)).thenReturn(RiskBand.LOW);
        when(interestRateCalculator.calculate(RiskBand.LOW, EmploymentType.SALARIED, new BigDecimal("500000")))
                .thenReturn(new BigDecimal("12.00"));
        when(emiCalculator.calculateEmi(new BigDecimal("500000"), new BigDecimal("12.00"), 36))
                .thenReturn(emi);
    }

    private LoanApplicationRecord verifySavedOnce() {
        ArgumentCaptor<LoanApplicationRecord> captor = ArgumentCaptor.forClass(LoanApplicationRecord.class);
        verify(repository, times(1)).save(captor.capture());
        return captor.getValue();
    }

    private LoanApplicationRequest validRequest() {
        ApplicantRequest applicant = new ApplicantRequest();
        applicant.setName("Asha Rao");
        applicant.setAge(30);
        applicant.setMonthlyIncome(new BigDecimal("125000"));
        applicant.setEmploymentType(EmploymentType.SALARIED);
        applicant.setCreditScore(780);

        LoanRequest loan = new LoanRequest();
        loan.setAmount(new BigDecimal("500000"));
        loan.setTenureMonths(36);
        loan.setPurpose(LoanPurpose.PERSONAL);

        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicant(applicant);
        request.setLoan(loan);
        return request;
    }
}
