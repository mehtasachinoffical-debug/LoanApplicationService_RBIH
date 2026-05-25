package com.example.loanservice;

import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.LoanApplicationRecord;
import com.example.loanservice.domain.LoanPurpose;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import com.example.loanservice.repository.LoanApplicationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoanApplicationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanApplicationRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void approvedHappyPathPersistsAuditRecord() throws Exception {
        long countBefore = repository.count();

        MvcResult result = postApplication(requestJson(
                        "Asha Rao", 30, "125000", EmploymentType.SALARIED, 780,
                        "500000", 36, LoanPurpose.PERSONAL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(ApplicationStatus.APPROVED.name()))
                .andExpect(jsonPath("$.riskBand").value(RiskBand.LOW.name()))
                .andExpect(jsonPath("$.offer.interestRate").value(12.00))
                .andExpect(jsonPath("$.offer.emi").value(16607.15))
                .andExpect(jsonPath("$.offer.totalPayable").value(597857.40))
                .andReturn();

        JsonNode response = responseJson(result);
        UUID applicationId = UUID.fromString(response.get("applicationId").asText());
        assertThat(repository.count()).isEqualTo(countBefore + 1);

        LoanApplicationRecord record = repository.findById(applicationId).orElseThrow();
        assertApplicantAndLoanSnapshot(record, "Asha Rao", 30, "125000", EmploymentType.SALARIED, 780,
                "500000", 36, LoanPurpose.PERSONAL);
        assertThat(record.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(record.getRiskBand()).isEqualTo(RiskBand.LOW);
        assertThat(record.getInterestRate()).isEqualByComparingTo("12.00");
        assertThat(record.getEmi()).isEqualByComparingTo("16607.15");
        assertThat(record.getTotalPayable()).isEqualByComparingTo("597857.40");
        assertThat(record.getCreatedAt()).isNotNull();
    }

    @Test
    void selfEmployedHighBandLargeLoanUsesExpectedRate() throws Exception {
        MvcResult result = postApplication(requestJson(
                        "Ravi Iyer", 25, "500000", EmploymentType.SELF_EMPLOYED, 620,
                        "1500000", 36, LoanPurpose.HOME))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(ApplicationStatus.APPROVED.name()))
                .andExpect(jsonPath("$.riskBand").value(RiskBand.HIGH.name()))
                .andExpect(jsonPath("$.offer.interestRate").value(16.50))
                .andReturn();

        JsonNode response = responseJson(result);
        LoanApplicationRecord record = repository.findById(
                UUID.fromString(response.get("applicationId").asText())).orElseThrow();
        assertApplicantAndLoanSnapshot(record, "Ravi Iyer", 25, "500000", EmploymentType.SELF_EMPLOYED, 620,
                "1500000", 36, LoanPurpose.HOME);
        assertThat(record.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(record.getRiskBand()).isEqualTo(RiskBand.HIGH);
        assertThat(record.getInterestRate()).isEqualByComparingTo("16.50");
        assertThat(record.getCreatedAt()).isNotNull();
    }

    @Test
    void lowCreditScoreIsRejectedAndAudited() throws Exception {
        MvcResult result = postApplication(requestJson(
                        "Neha Shah", 30, "125000", EmploymentType.SALARIED, 580,
                        "500000", 36, LoanPurpose.PERSONAL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(ApplicationStatus.REJECTED.name()))
                .andExpect(jsonPath("$.rejectionReasons[0]").value(
                        RejectionReason.CREDIT_SCORE_TOO_LOW.name()))
                .andReturn();

        LoanApplicationRecord record = persistedRecord(result);
        assertApplicantAndLoanSnapshot(record, "Neha Shah", 30, "125000", EmploymentType.SALARIED, 580,
                "500000", 36, LoanPurpose.PERSONAL);
        assertRejected(record, RejectionReason.CREDIT_SCORE_TOO_LOW);
    }

    @Test
    void ageTenureLimitIsRejectedAndAudited() throws Exception {
        MvcResult result = postApplication(requestJson(
                        "Vikram Menon", 60, "125000", EmploymentType.SALARIED, 780,
                        "500000", 120, LoanPurpose.PERSONAL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(ApplicationStatus.REJECTED.name()))
                .andExpect(jsonPath("$.rejectionReasons[0]").value(
                        RejectionReason.AGE_TENURE_LIMIT_EXCEEDED.name()))
                .andReturn();

        LoanApplicationRecord record = persistedRecord(result);
        assertApplicantAndLoanSnapshot(record, "Vikram Menon", 60, "125000", EmploymentType.SALARIED, 780,
                "500000", 120, LoanPurpose.PERSONAL);
        assertRejected(record, RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
    }

    @Test
    void lowIncomeLargeLoanIsRejectedBy60PercentEmiCapAndAudited() throws Exception {
        MvcResult result = postApplication(requestJson(
                        "Meera Das", 30, "50000", EmploymentType.SALARIED, 780,
                        "1500000", 36, LoanPurpose.HOME))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(ApplicationStatus.REJECTED.name()))
                .andExpect(jsonPath("$.rejectionReasons[0]").value(
                        RejectionReason.EMI_EXCEEDS_60_PERCENT.name()))
                .andReturn();

        LoanApplicationRecord record = persistedRecord(result);
        assertApplicantAndLoanSnapshot(record, "Meera Das", 30, "50000", EmploymentType.SALARIED, 780,
                "1500000", 36, LoanPurpose.HOME);
        assertRejected(record, RejectionReason.EMI_EXCEEDS_60_PERCENT);
    }

    @Test
    void midRangeIncomeIsRejectedOnlyBy50PercentEmiCapAndAudited() throws Exception {
        MvcResult result = postApplication(requestJson(
                        "Kabir Khan", 30, "30000", EmploymentType.SALARIED, 780,
                        "500000", 36, LoanPurpose.PERSONAL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(ApplicationStatus.REJECTED.name()))
                .andExpect(jsonPath("$.rejectionReasons").value(containsInAnyOrder(
                        RejectionReason.EMI_EXCEEDS_50_PERCENT_OF_INCOME.name())))
                .andReturn();

        LoanApplicationRecord record = persistedRecord(result);
        assertApplicantAndLoanSnapshot(record, "Kabir Khan", 30, "30000", EmploymentType.SALARIED, 780,
                "500000", 36, LoanPurpose.PERSONAL);
        assertRejected(record, RejectionReason.EMI_EXCEEDS_50_PERCENT_OF_INCOME);
        assertThat(record.getRejectionReasons()).doesNotContain(RejectionReason.EMI_EXCEEDS_60_PERCENT);
    }

    @Test
    void invalidAgeReturnsFieldErrorAndDoesNotPersist() throws Exception {
        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(
                                "Asha Rao", 18, "125000", EmploymentType.SALARIED, 780,
                                "500000", 36, LoanPurpose.PERSONAL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("applicant.age")));

        assertThat(repository.count()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions postApplication(String requestJson) throws Exception {
        return mockMvc.perform(post("/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));
    }

    private LoanApplicationRecord persistedRecord(MvcResult result) throws Exception {
        JsonNode response = responseJson(result);
        UUID applicationId = UUID.fromString(response.get("applicationId").asText());
        return repository.findById(applicationId).orElseThrow();
    }

    private JsonNode responseJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertApplicantAndLoanSnapshot(LoanApplicationRecord record, String name, int age,
                                                String monthlyIncome, EmploymentType employmentType,
                                                int creditScore, String amount, int tenureMonths,
                                                LoanPurpose purpose) {
        assertThat(record.getApplicantName()).isEqualTo(name);
        assertThat(record.getAge()).isEqualTo(age);
        assertThat(record.getMonthlyIncome()).isEqualByComparingTo(monthlyIncome);
        assertThat(record.getEmploymentType()).isEqualTo(employmentType);
        assertThat(record.getCreditScore()).isEqualTo(creditScore);
        assertThat(record.getLoanAmount()).isEqualByComparingTo(amount);
        assertThat(record.getTenureMonths()).isEqualTo(tenureMonths);
        assertThat(record.getPurpose()).isEqualTo(purpose);
        assertThat(record.getCreatedAt()).isNotNull();
    }

    private void assertRejected(LoanApplicationRecord record, RejectionReason reason) {
        assertThat(record.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(record.getRiskBand()).isNull();
        assertThat(record.getInterestRate()).isNull();
        assertThat(record.getEmi()).isNull();
        assertThat(record.getTotalPayable()).isNull();
        assertThat(record.getRejectionReasons()).containsExactly(reason);
        assertThat(record.getCreatedAt()).isNotNull();
    }

    private String requestJson(String name, int age, String monthlyIncome, EmploymentType employmentType,
                               int creditScore, String amount, int tenureMonths, LoanPurpose purpose) {
        return """
                {
                  "applicant": {
                    "name": "%s",
                    "age": %d,
                    "monthlyIncome": %s,
                    "employmentType": "%s",
                    "creditScore": %d
                  },
                  "loan": {
                    "amount": %s,
                    "tenureMonths": %d,
                    "purpose": "%s"
                  }
                }
                """.formatted(name, age, monthlyIncome, employmentType.name(), creditScore,
                amount, tenureMonths, purpose.name());
    }
}
