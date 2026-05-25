package com.example.loanservice.service;

import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.LoanApplicationRecord;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import com.example.loanservice.dto.request.LoanApplicationRequest;
import com.example.loanservice.dto.response.LoanApplicationResponse;
import com.example.loanservice.dto.response.OfferResponse;
import com.example.loanservice.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LoanApplicationService {

    private static final int MIN_CREDIT_SCORE = 600;
    private static final int MAX_AGE_AT_MATURITY = 65;
    private static final BigDecimal RATIO_60_PERCENT = new BigDecimal("0.60");
    private static final BigDecimal RATIO_50_PERCENT = new BigDecimal("0.50");

    private final RiskBandClassifier riskBandClassifier;
    private final InterestRateCalculator interestRateCalculator;
    private final EmiCalculator emiCalculator;
    private final LoanApplicationRepository repository;

    public LoanApplicationService(RiskBandClassifier riskBandClassifier,
                                  InterestRateCalculator interestRateCalculator,
                                  EmiCalculator emiCalculator,
                                  LoanApplicationRepository repository) {
        this.riskBandClassifier = riskBandClassifier;
        this.interestRateCalculator = interestRateCalculator;
        this.emiCalculator = emiCalculator;
        this.repository = repository;
    }

    public LoanApplicationResponse process(LoanApplicationRequest req) {
        UUID id = UUID.randomUUID();

        List<RejectionReason> earlyReasons = collectEarlyRejections(req);
        if (!earlyReasons.isEmpty()) {
            return persistRejected(id, req, earlyReasons);
        }

        RiskBand band = riskBandClassifier.classify(req.getApplicant().getCreditScore());
        BigDecimal rate = interestRateCalculator.calculate(
                band, req.getApplicant().getEmploymentType(), req.getLoan().getAmount());
        BigDecimal emi = emiCalculator.calculateEmi(
                req.getLoan().getAmount(), rate, req.getLoan().getTenureMonths());

        List<RejectionReason> emiReasons = collectEmiRejections(req, emi);
        if (!emiReasons.isEmpty()) {
            return persistRejected(id, req, emiReasons);
        }

        BigDecimal totalPayable = emiCalculator.totalPayable(emi, req.getLoan().getTenureMonths());
        return persistApproved(id, req, band, rate, emi, totalPayable);
    }

    private List<RejectionReason> collectEarlyRejections(LoanApplicationRequest req) {
        List<RejectionReason> reasons = new ArrayList<>();

        if (req.getApplicant().getCreditScore() < MIN_CREDIT_SCORE) {
            reasons.add(RejectionReason.CREDIT_SCORE_TOO_LOW);
        }

        int tenureYears = (int) Math.ceil(req.getLoan().getTenureMonths() / 12.0);
        if (req.getApplicant().getAge() + tenureYears > MAX_AGE_AT_MATURITY) {
            reasons.add(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
        }

        return reasons;
    }

    private List<RejectionReason> collectEmiRejections(LoanApplicationRequest req, BigDecimal emi) {
        BigDecimal monthlyIncome = req.getApplicant().getMonthlyIncome();
        BigDecimal sixtyPercentCap = monthlyIncome.multiply(RATIO_60_PERCENT);
        BigDecimal fiftyPercentCap = monthlyIncome.multiply(RATIO_50_PERCENT);

        if (emi.compareTo(sixtyPercentCap) > 0) {
            return List.of(RejectionReason.EMI_EXCEEDS_60_PERCENT);
        }
        if (emi.compareTo(fiftyPercentCap) > 0) {
            return List.of(RejectionReason.EMI_EXCEEDS_50_PERCENT_OF_INCOME);
        }
        return List.of();
    }

    private LoanApplicationResponse persistApproved(UUID id, LoanApplicationRequest req, RiskBand band,
                                                    BigDecimal rate, BigDecimal emi,
                                                    BigDecimal totalPayable) {
        LoanApplicationRecord record = toRecordBase(id, req);
        record.setStatus(ApplicationStatus.APPROVED);
        record.setRiskBand(band);
        record.setInterestRate(rate);
        record.setEmi(emi);
        record.setTotalPayable(totalPayable);
        record.setRejectionReasons(List.of());
        repository.save(record);

        OfferResponse offer = new OfferResponse(rate, req.getLoan().getTenureMonths(), emi, totalPayable);
        return LoanApplicationResponse.approved(id, band, offer);
    }

    private LoanApplicationResponse persistRejected(UUID id, LoanApplicationRequest req,
                                                    List<RejectionReason> reasons) {
        LoanApplicationRecord record = toRecordBase(id, req);
        record.setStatus(ApplicationStatus.REJECTED);
        record.setRejectionReasons(reasons);
        repository.save(record);

        return LoanApplicationResponse.rejected(id, reasons);
    }

    private LoanApplicationRecord toRecordBase(UUID id, LoanApplicationRequest req) {
        LoanApplicationRecord record = new LoanApplicationRecord();
        record.setApplicationId(id);
        record.setApplicantName(req.getApplicant().getName());
        record.setAge(req.getApplicant().getAge());
        record.setMonthlyIncome(req.getApplicant().getMonthlyIncome());
        record.setEmploymentType(req.getApplicant().getEmploymentType());
        record.setCreditScore(req.getApplicant().getCreditScore());
        record.setLoanAmount(req.getLoan().getAmount());
        record.setTenureMonths(req.getLoan().getTenureMonths());
        record.setPurpose(req.getLoan().getPurpose());
        record.setCreatedAt(Instant.now());
        return record;
    }
}
