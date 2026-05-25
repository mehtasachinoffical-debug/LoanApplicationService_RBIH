package com.example.loanservice.service;

import com.example.loanservice.domain.RiskBand;
import org.springframework.stereotype.Component;

@Component
public class RiskBandClassifier {

    public RiskBand classify(int creditScore) {
        if (creditScore >= 750) {
            return RiskBand.LOW;
        }
        if (creditScore >= 650) {
            return RiskBand.MEDIUM;
        }
        if (creditScore >= 600) {
            return RiskBand.HIGH;
        }
        throw new IllegalArgumentException(
                "Credit score " + creditScore + " is below 600 and has no risk band");
    }
}
