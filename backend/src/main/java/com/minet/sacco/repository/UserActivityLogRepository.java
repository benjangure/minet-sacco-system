package com.minet.sacco.repository;

import com.minet.sacco.entity.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    List<UserActivityLog> findByUserId(Long userId);
    List<UserActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<UserActivityLog> findByActionOrderByCreatedAtDesc(String action);
    List<UserActivityLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
