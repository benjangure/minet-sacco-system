package com.minet.sacco.repository;

import com.minet.sacco.entity.MemberReactivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberReactivationRepository extends JpaRepository<MemberReactivation, Long> {
    Optional<MemberReactivation> findByMemberIdAndIsActiveTrue(Long memberId);
    List<MemberReactivation> findByMemberId(Long memberId);
    List<MemberReactivation> findByStatus(String status);
}
