package com.example.loanservice.service;

import com.example.loanservice.domain.RiskBand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskBandClassifierTest {

    private final RiskBandClassifier classifier = new RiskBandClassifier();

    @ParameterizedTest
    @CsvSource({
            "900, LOW",
            "750, LOW",
            "749, MEDIUM",
            "700, MEDIUM",
            "650, MEDIUM",
            "649, HIGH",
            "600, HIGH"
    })
    void classifiesCreditScoreRiskBandBoundaries(int creditScore, RiskBand expectedRiskBand) {
        assertThat(classifier.classify(creditScore)).isEqualTo(expectedRiskBand);
    }

    @Test
    void creditScoresBelow600HaveNoRiskBand() {
        assertThatThrownBy(() -> classifier.classify(599))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credit score 599 is below 600 and has no risk band");

        assertThatThrownBy(() -> classifier.classify(300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credit score 300 is below 600 and has no risk band");
    }
}
