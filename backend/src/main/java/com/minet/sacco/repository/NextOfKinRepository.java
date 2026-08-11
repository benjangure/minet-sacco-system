package com.minet.sacco.repository;

import com.minet.sacco.entity.NextOfKin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface NextOfKinRepository extends JpaRepository<NextOfKin, Long> {
    
    List<NextOfKin> findByMemberId(Long memberId);
    
    void deleteByMemberId(Long memberId);
    
    @Query("SELECT COALESCE(SUM(n.percentage), 0) FROM NextOfKin n WHERE n.member.id = :memberId")
    BigDecimal getTotalPercentageForMember(@Param("memberId") Long memberId);
    
    @Query("SELECT COUNT(n) FROM NextOfKin n WHERE n.member.id = :memberId")
    int countByMemberId(@Param("memberId") Long memberId);
}
