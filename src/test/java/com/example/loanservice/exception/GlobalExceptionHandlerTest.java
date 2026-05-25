package com.example.loanservice.exception;

import com.example.loanservice.controller.LoanApplicationController;
import com.example.loanservice.service.LoanApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanApplicationController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanApplicationService service;

    @Test
    void invalidAgeReturnsFieldLevelValidationError() throws Exception {
        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson().replace("\"age\": 30", "\"age\": 18")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("applicant.age")))
                .andExpect(jsonPath("$.errors[0].message").value("Age must be at least 21"));
    }

    @Test
    void malformedJsonReturnsMalformedRequestBodyError() throws Exception {
        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicant\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void invalidEnumValueReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()
                                .replace("\"employmentType\": \"SALARIED\"", "\"employmentType\": \"FREELANCER\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void multipleInvalidFieldsReturnAllFieldErrors() throws Exception {
        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicant": {
                                    "name": "",
                                    "age": 18,
                                    "monthlyIncome": 0,
                                    "employmentType": "SALARIED",
                                    "creditScore": 299
                                  },
                                  "loan": {
                                    "amount": 9999,
                                    "tenureMonths": 5,
                                    "purpose": "PERSONAL"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                        "applicant.name",
                        "applicant.age",
                        "applicant.monthlyIncome",
                        "applicant.creditScore",
                        "loan.amount",
                        "loan.tenureMonths")));
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
