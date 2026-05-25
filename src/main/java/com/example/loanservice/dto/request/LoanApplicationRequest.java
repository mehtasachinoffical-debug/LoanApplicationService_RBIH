package com.example.loanservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class LoanApplicationRequest {

    @NotNull(message = "Applicant is required")
    @Valid
    private ApplicantRequest applicant;

    @NotNull(message = "Loan details are required")
    @Valid
    private LoanRequest loan;

    public ApplicantRequest getApplicant() {
        return applicant;
    }

    public void setApplicant(ApplicantRequest applicant) {
        this.applicant = applicant;
    }

    public LoanRequest getLoan() {
        return loan;
    }

    public void setLoan(LoanRequest loan) {
        this.loan = loan;
    }
}
