package com.example.loanservice.repository;

import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.LoanApplicationRecord;
import com.example.loanservice.domain.LoanPurpose;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LoanApplicationRepositoryTest {

    @Autowired
    private LoanApplicationRepository repository;

    @Test
    void savesAndLoadsApprovedRecord() {
        UUID applicationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LoanApplicationRecord record = baseRecord(applicationId);
        record.setStatus(ApplicationStatus.APPROVED);
        record.setRiskBand(RiskBand.LOW);
        record.setInterestRate(new BigDecimal("12.00"));
        record.setEmi(new BigDecimal("16607.15"));
        record.setTotalPayable(new BigDecimal("597857.40"));
        record.setRejectionReasons(List.of());

        repository.saveAndFlush(record);

        LoanApplicationRecord loaded = repository.findById(applicationId).orElseThrow();
        assertBaseFields(loaded, applicationId);
        assertThat(loaded.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(loaded.getRiskBand()).isEqualTo(RiskBand.LOW);
        assertThat(loaded.getInterestRate()).isEqualByComparingTo("12.00");
        assertThat(loaded.getEmi()).isEqualByComparingTo("16607.15");
        assertThat(loaded.getTotalPayable()).isEqualByComparingTo("597857.40");
        assertThat(loaded.getRejectionReasons()).isEmpty();
    }

    @Test
    void savesAndLoadsRejectedRecordWithReasonsInOrder() {
        UUID applicationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        LoanApplicationRecord record = baseRecord(applicationId);
        record.setStatus(ApplicationStatus.REJECTED);
        record.setRiskBand(null);
        record.setInterestRate(null);
        record.setEmi(null);
        record.setTotalPayable(null);
        record.setRejectionReasons(List.of(
                RejectionReason.CREDIT_SCORE_TOO_LOW,
                RejectionReason.AGE_TENURE_LIMIT_EXCEEDED));

        repository.saveAndFlush(record);

        LoanApplicationRecord loaded = repository.findById(applicationId).orElseThrow();
        assertBaseFields(loaded, applicationId);
        assertThat(loaded.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(loaded.getRiskBand()).isNull();
        assertThat(loaded.getInterestRate()).isNull();
        assertThat(loaded.getEmi()).isNull();
        assertThat(loaded.getTotalPayable()).isNull();
        assertThat(loaded.getRejectionReasons()).containsExactly(
                RejectionReason.CREDIT_SCORE_TOO_LOW,
                RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
    }

    @Test
    void countsSavedRecords() {
        LoanApplicationRecord first = baseRecord(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        first.setStatus(ApplicationStatus.APPROVED);
        first.setRiskBand(RiskBand.MEDIUM);
        first.setInterestRate(new BigDecimal("13.50"));
        first.setEmi(new BigDecimal("16967.64"));
        first.setTotalPayable(new BigDecimal("610835.04"));
        first.setRejectionReasons(List.of());

        LoanApplicationRecord second = baseRecord(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        second.setStatus(ApplicationStatus.REJECTED);
        second.setRejectionReasons(List.of(RejectionReason.EMI_EXCEEDS_60_PERCENT));

        repository.saveAllAndFlush(List.of(first, second));

        assertThat(repository.count()).isEqualTo(2);
    }

    private LoanApplicationRecord baseRecord(UUID applicationId) {
        LoanApplicationRecord record = new LoanApplicationRecord();
        record.setApplicationId(applicationId);
        record.setApplicantName("Asha Rao");
        record.setAge(35);
        record.setMonthlyIncome(new BigDecimal("125000"));
        record.setEmploymentType(EmploymentType.SALARIED);
        record.setCreditScore(760);
        record.setLoanAmount(new BigDecimal("500000"));
        record.setTenureMonths(36);
        record.setPurpose(LoanPurpose.HOME);
        record.setCreatedAt(Instant.parse("2026-05-25T18:00:00Z"));
        return record;
    }

    private void assertBaseFields(LoanApplicationRecord loaded, UUID applicationId) {
        assertThat(loaded.getApplicationId()).isEqualTo(applicationId);
        assertThat(loaded.getApplicantName()).isEqualTo("Asha Rao");
        assertThat(loaded.getAge()).isEqualTo(35);
        assertThat(loaded.getMonthlyIncome()).isEqualByComparingTo("125000");
        assertThat(loaded.getEmploymentType()).isEqualTo(EmploymentType.SALARIED);
        assertThat(loaded.getCreditScore()).isEqualTo(760);
        assertThat(loaded.getLoanAmount()).isEqualByComparingTo("500000");
        assertThat(loaded.getTenureMonths()).isEqualTo(36);
        assertThat(loaded.getPurpose()).isEqualTo(LoanPurpose.HOME);
        assertThat(loaded.getCreatedAt()).isEqualTo(Instant.parse("2026-05-25T18:00:00Z"));
    }
}
