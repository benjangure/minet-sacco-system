package com.minet.sacco.repository;

import com.minet.sacco.entity.LoanRepaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepaymentRequestRepository extends JpaRepository<LoanRepaymentRequest, Long> {
    List<LoanRepaymentRequest> findByStatus(LoanRepaymentRequest.Status status);
    List<LoanRepaymentRequest> findByMemberId(Long memberId);
    List<LoanRepaymentRequest> findByLoanId(Long loanId);
    Optional<LoanRepaymentRequest> findByIdAndMemberId(Long id, Long memberId);
}
