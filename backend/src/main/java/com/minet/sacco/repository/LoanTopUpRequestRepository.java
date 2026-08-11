package com.minet.sacco.repository;

import com.minet.sacco.entity.LoanTopUpRequest;
import com.minet.sacco.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanTopUpRequestRepository extends JpaRepository<LoanTopUpRequest, Long> {
    
    List<LoanTopUpRequest> findByLoanId(Long loanId);
    
    List<LoanTopUpRequest> findByMemberId(Long memberId);
    
    List<LoanTopUpRequest> findByStatus(LoanTopUpRequest.Status status);
    
    @Query("SELECT t FROM LoanTopUpRequest t WHERE t.status IN :statuses ORDER BY t.requestedDate DESC")
    List<LoanTopUpRequest> findByStatusIn(@Param("statuses") List<LoanTopUpRequest.Status> statuses);
    
    @Query("SELECT t FROM LoanTopUpRequest t WHERE t.member.id = :memberId AND t.status IN :statuses ORDER BY t.requestedDate DESC")
    List<LoanTopUpRequest> findByMemberIdAndStatusIn(@Param("memberId") Long memberId, @Param("statuses") List<LoanTopUpRequest.Status> statuses);
    
    @Query("SELECT COUNT(t) FROM LoanTopUpRequest t WHERE t.loan.id = :loanId AND t.status = :status")
    long countByLoanIdAndStatus(@Param("loanId") Long loanId, @Param("status") LoanTopUpRequest.Status status);
}
