package com.example.loanservice.service;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmiCalculatorTest {

    private final EmiCalculator calculator = new EmiCalculator();

    @ParameterizedTest
    @CsvSource({
            "500000, 12.00, 36, 16607.15",
            "100000, 12.00, 12, 8884.88",
            "500000, 13.50, 36, 16967.64",
            "1000000, 10.00, 60, 21247.04"
    })
    void calculatesEmiUsingMonthlyRateFormula(BigDecimal principal, BigDecimal annualRatePercent,
                                              int tenureMonths, BigDecimal expectedEmi) {
        BigDecimal actualEmi = calculator.calculateEmi(principal, annualRatePercent, tenureMonths);

        assertThat(actualEmi).isEqualByComparingTo(expectedEmi);
        assertThat(actualEmi.scale()).isEqualTo(2);
    }

    @Test
    void calculatesTotalPayableFromEmiAndTenure() {
        BigDecimal totalPayable = calculator.totalPayable(new BigDecimal("16607.15"), 36);

        assertThat(totalPayable).isEqualByComparingTo("597857.40");
        assertThat(totalPayable.scale()).isEqualTo(2);
    }

    @Test
    void longTenureAndSmallPrincipalReturnsPositiveScaleTwoEmi() {
        BigDecimal emi = calculator.calculateEmi(new BigDecimal("10000"), new BigDecimal("12.00"), 360);

        assertThat(emi).isPositive();
        assertThat(emi.scale()).isEqualTo(2);
    }
}
