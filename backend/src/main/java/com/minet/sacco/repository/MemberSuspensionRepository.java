package com.minet.sacco.repository;

import com.minet.sacco.entity.MemberSuspension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberSuspensionRepository extends JpaRepository<MemberSuspension, Long> {
    Optional<MemberSuspension> findByMemberIdAndIsActiveTrue(Long memberId);
    List<MemberSuspension> findByMemberId(Long memberId);
    List<MemberSuspension> findByIsActiveTrue();
}
