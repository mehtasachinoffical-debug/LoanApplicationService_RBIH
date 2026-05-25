package com.example.loanservice.dto.request;

import com.example.loanservice.domain.LoanPurpose;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class LoanRequest {

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "10000", message = "Loan amount must be at least 10,000")
    @DecimalMax(value = "5000000", message = "Loan amount must be at most 50,00,000")
    private BigDecimal amount;

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Tenure must be at least 6 months")
    @Max(value = 360, message = "Tenure must be at most 360 months")
    private Integer tenureMonths;

    @NotNull(message = "Purpose is required")
    private LoanPurpose purpose;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public LoanPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(LoanPurpose purpose) {
        this.purpose = purpose;
    }
}
