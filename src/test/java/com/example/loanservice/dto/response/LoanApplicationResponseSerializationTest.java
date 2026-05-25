package com.example.loanservice.dto.response;

import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoanApplicationResponseSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void approvedResponseSerializesOfferAndOmitsRejectionReasons() throws Exception {
        UUID applicationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OfferResponse offer = new OfferResponse(
                new BigDecimal("12.50"),
                120,
                new BigDecimal("10671.13"),
                new BigDecimal("1280535.60"));
        LoanApplicationResponse response =
                LoanApplicationResponse.approved(applicationId, RiskBand.LOW, offer);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("applicationId").asText()).isEqualTo(applicationId.toString());
        assertThat(json.get("status").asText()).isEqualTo("APPROVED");
        assertThat(json.get("riskBand").asText()).isEqualTo("LOW");
        assertThat(json.has("rejectionReasons")).isFalse();

        JsonNode offerJson = json.get("offer");
        assertThat(offerJson).isNotNull();
        assertThat(offerJson.get("interestRate").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(offerJson.get("tenureMonths").asInt()).isEqualTo(120);
        assertThat(offerJson.get("emi").decimalValue()).isEqualByComparingTo("10671.13");
        assertThat(offerJson.get("totalPayable").decimalValue()).isEqualByComparingTo("1280535.60");
    }

    @Test
    void rejectedResponseSerializesRiskBandAsNullAndOmitsOffer() throws Exception {
        UUID applicationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        LoanApplicationResponse response = LoanApplicationResponse.rejected(
                applicationId,
                List.of(RejectionReason.CREDIT_SCORE_TOO_LOW, RejectionReason.EMI_EXCEEDS_60_PERCENT));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("applicationId").asText()).isEqualTo(applicationId.toString());
        assertThat(json.get("status").asText()).isEqualTo("REJECTED");
        assertThat(json.has("riskBand")).isTrue();
        assertThat(json.get("riskBand").isNull()).isTrue();
        assertThat(json.has("offer")).isFalse();
        assertThat(json.get("rejectionReasons")).hasSize(2);
        assertThat(json.get("rejectionReasons").get(0).asText()).isEqualTo("CREDIT_SCORE_TOO_LOW");
        assertThat(json.get("rejectionReasons").get(1).asText()).isEqualTo("EMI_EXCEEDS_60_PERCENT");
    }
}
