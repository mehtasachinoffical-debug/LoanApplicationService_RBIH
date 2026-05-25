package com.example.loanservice.service;

import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.RiskBand;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class InterestRateCalculator {

    private static final BigDecimal BASE_RATE = new BigDecimal("12.00");
    private static final BigDecimal LARGE_LOAN_THRESHOLD = new BigDecimal("1000000");
    private static final BigDecimal LARGE_LOAN_PREMIUM = new BigDecimal("0.5");

    public BigDecimal calculate(RiskBand band, EmploymentType employment, BigDecimal loanAmount) {
        return BASE_RATE
                .add(riskPremium(band))
                .add(employmentPremium(employment))
                .add(loanSizePremium(loanAmount))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal riskPremium(RiskBand band) {
        return switch (band) {
            case LOW -> BigDecimal.ZERO;
            case MEDIUM -> new BigDecimal("1.5");
            case HIGH -> new BigDecimal("3");
        };
    }

    private BigDecimal employmentPremium(EmploymentType type) {
        return switch (type) {
            case SALARIED -> BigDecimal.ZERO;
            case SELF_EMPLOYED -> new BigDecimal("1");
        };
    }

    private BigDecimal loanSizePremium(BigDecimal amount) {
        return amount.compareTo(LARGE_LOAN_THRESHOLD) > 0 ? LARGE_LOAN_PREMIUM : BigDecimal.ZERO;
    }
}
