package com.example.loanservice.controller;

import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import com.example.loanservice.dto.response.LoanApplicationResponse;
import com.example.loanservice.dto.response.OfferResponse;
import com.example.loanservice.service.LoanApplicationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanApplicationController.class)
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanApplicationService service;

    @Test
    void validRequestReturnsCreatedApprovedResponse() throws Exception {
        UUID applicationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LoanApplicationResponse response = LoanApplicationResponse.approved(
                applicationId,
                RiskBand.LOW,
                new OfferResponse(
                        new BigDecimal("12.00"),
                        36,
                        new BigDecimal("16607.15"),
                        new BigDecimal("597857.40")));
        when(service.process(any())).thenReturn(response);

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value(ApplicationStatus.APPROVED.name()))
                .andExpect(jsonPath("$.riskBand").value(RiskBand.LOW.name()))
                .andExpect(jsonPath("$.offer.interestRate").value(12.00))
                .andExpect(jsonPath("$.offer.tenureMonths").value(36))
                .andExpect(jsonPath("$.offer.emi").value(16607.15))
                .andExpect(jsonPath("$.offer.totalPayable").value(597857.40))
                .andExpect(jsonPath("$.rejectionReasons").doesNotExist());
    }

    @Test
    void rejectedOutcomeStillReturnsCreatedWithRejectionReasons() throws Exception {
        UUID applicationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        LoanApplicationResponse response = LoanApplicationResponse.rejected(
                applicationId,
                List.of(RejectionReason.CREDIT_SCORE_TOO_LOW));
        when(service.process(any())).thenReturn(response);

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value(ApplicationStatus.REJECTED.name()))
                .andExpect(jsonPath("$.riskBand").isEmpty())
                .andExpect(jsonPath("$.offer").doesNotExist())
                .andExpect(jsonPath("$.rejectionReasons[0]").value(
                        RejectionReason.CREDIT_SCORE_TOO_LOW.name()));
    }

    @Test
    void missingRequiredFieldReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicant": {
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
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private String validRequestJson() {
        return """
                {
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
                }
                """;
    }
}
