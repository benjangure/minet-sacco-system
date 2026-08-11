package com.minet.sacco.repository;

import com.minet.sacco.entity.TopUpGuarantor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopUpGuarantorRepository extends JpaRepository<TopUpGuarantor, Long> {
    
    List<TopUpGuarantor> findByTopUpRequestId(Long topUpRequestId);
    
    List<TopUpGuarantor> findByMemberId(Long memberId);
    
    Optional<TopUpGuarantor> findByTopUpRequestIdAndMemberId(Long topUpRequestId, Long memberId);
    
    @Query("SELECT g FROM TopUpGuarantor g WHERE g.member.id = :memberId AND g.status = :status ORDER BY g.requestedDate DESC")
    List<TopUpGuarantor> findByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") TopUpGuarantor.Status status);
    
    @Query("SELECT COUNT(g) FROM TopUpGuarantor g WHERE g.topUpRequest.id = :topUpRequestId AND g.status = :status")
    long countByTopUpRequestIdAndStatus(@Param("topUpRequestId") Long topUpRequestId, @Param("status") TopUpGuarantor.Status status);
}
