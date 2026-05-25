package com.example.loanservice.dto.response;

import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

public class LoanApplicationResponse {
    private UUID applicationId;
    private ApplicationStatus status;
    private RiskBand riskBand;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OfferResponse offer;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<RejectionReason> rejectionReasons;

    public static LoanApplicationResponse approved(UUID applicationId, RiskBand riskBand, OfferResponse offer) {
        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setApplicationId(applicationId);
        response.setStatus(ApplicationStatus.APPROVED);
        response.setRiskBand(riskBand);
        response.setOffer(offer);
        return response;
    }

    public static LoanApplicationResponse rejected(UUID applicationId, List<RejectionReason> rejectionReasons) {
        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setApplicationId(applicationId);
        response.setStatus(ApplicationStatus.REJECTED);
        response.setRejectionReasons(rejectionReasons);
        return response;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public RiskBand getRiskBand() {
        return riskBand;
    }

    public void setRiskBand(RiskBand riskBand) {
        this.riskBand = riskBand;
    }

    public OfferResponse getOffer() {
        return offer;
    }

    public void setOffer(OfferResponse offer) {
        this.offer = offer;
    }

    public List<RejectionReason> getRejectionReasons() {
        return rejectionReasons;
    }

    public void setRejectionReasons(List<RejectionReason> rejectionReasons) {
        this.rejectionReasons = rejectionReasons;
    }
}
