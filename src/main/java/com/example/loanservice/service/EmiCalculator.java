package com.example.loanservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class EmiCalculator {

    private static final int CALC_SCALE = 10;
    private static final int RESULT_SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final BigDecimal MONTHS_IN_YEAR_TIMES_PERCENT = new BigDecimal("1200");

    public BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRatePercent, int tenureMonths) {
        BigDecimal monthlyRate = annualRatePercent.divide(MONTHS_IN_YEAR_TIMES_PERCENT, CALC_SCALE, RM);

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal pow = onePlusR.pow(tenureMonths);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, RESULT_SCALE, RM);
    }

    public BigDecimal totalPayable(BigDecimal emi, int tenureMonths) {
        return emi.multiply(BigDecimal.valueOf(tenureMonths)).setScale(RESULT_SCALE, RM);
    }
}
