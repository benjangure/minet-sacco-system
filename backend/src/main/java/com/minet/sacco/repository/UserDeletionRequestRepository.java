package com.minet.sacco.repository;

import com.minet.sacco.entity.UserDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeletionRequestRepository extends JpaRepository<UserDeletionRequest, Long> {
    List<UserDeletionRequest> findByStatus(String status);
    Optional<UserDeletionRequest> findByUserIdAndStatus(Long userId, String status);
    List<UserDeletionRequest> findByRequestedById(Long requestedById);
}
