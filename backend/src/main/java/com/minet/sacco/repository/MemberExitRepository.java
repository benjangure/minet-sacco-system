package com.minet.sacco.repository;

import com.minet.sacco.entity.MemberExit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberExitRepository extends JpaRepository<MemberExit, Long> {
    Optional<MemberExit> findByMemberId(Long memberId);
    List<MemberExit> findByApprovedByIsNull();
    List<MemberExit> findByApprovedByIsNotNull();
}
