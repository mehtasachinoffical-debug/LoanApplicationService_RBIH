package com.example.loanservice.service;

import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.RiskBand;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class InterestRateCalculatorTest {

    private final InterestRateCalculator calculator = new InterestRateCalculator();

    @ParameterizedTest
    @CsvSource({
            "LOW, SALARIED, 500000, 12.00",
            "LOW, SELF_EMPLOYED, 500000, 13.00",
            "MEDIUM, SALARIED, 500000, 13.50",
            "MEDIUM, SELF_EMPLOYED, 1500000, 15.00",
            "HIGH, SALARIED, 500000, 15.00",
            "HIGH, SELF_EMPLOYED, 1500000, 16.50",
            "LOW, SALARIED, 1000000, 12.00",
            "LOW, SALARIED, 1000001, 12.50"
    })
    void calculatesInterestRateWithPremiumStacking(RiskBand band, EmploymentType employment,
                                                   BigDecimal amount, BigDecimal expectedRate) {
        BigDecimal actualRate = calculator.calculate(band, employment, amount);

        assertThat(actualRate).isEqualByComparingTo(expectedRate);
        assertThat(actualRate.scale()).isEqualTo(2);
    }
}
