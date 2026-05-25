package com.example.loanservice.service;

import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.dto.request.LoanApplicationRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EligibilityEvaluator {

    private static final int MIN_CREDIT_SCORE = 600;
    private static final int MAX_AGE_AT_MATURITY = 65;
    private static final BigDecimal MAX_EMI_TO_INCOME_RATIO = new BigDecimal("0.60");

    public List<RejectionReason> evaluate(LoanApplicationRequest req, BigDecimal emi) {
        List<RejectionReason> reasons = new ArrayList<>();

        if (req.getApplicant().getCreditScore() < MIN_CREDIT_SCORE) {
            reasons.add(RejectionReason.CREDIT_SCORE_TOO_LOW);
        }

        int tenureYears = (int) Math.ceil(req.getLoan().getTenureMonths() / 12.0);
        if (req.getApplicant().getAge() + tenureYears > MAX_AGE_AT_MATURITY) {
            reasons.add(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
        }

        BigDecimal emiCap = req.getApplicant().getMonthlyIncome().multiply(MAX_EMI_TO_INCOME_RATIO);
        if (emi != null && emi.compareTo(emiCap) > 0) {
            reasons.add(RejectionReason.EMI_EXCEEDS_60_PERCENT);
        }

        return reasons;
    }
}
