package com.minet.sacco.repository;

import com.minet.sacco.entity.DepositRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long> {

    List<DepositRequest> findByStatus(String status);

    List<DepositRequest> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<DepositRequest> findByStatusOrderByCreatedAtDesc(String status);

    Optional<DepositRequest> findById(Long id);
}
