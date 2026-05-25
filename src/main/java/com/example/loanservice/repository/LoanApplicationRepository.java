package com.example.loanservice.repository;

import com.example.loanservice.domain.LoanApplicationRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplicationRecord, UUID> {
}
