package com.minet.sacco.repository;

import com.minet.sacco.entity.LoginHistory;
import com.minet.sacco.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    
    List<LoginHistory> findByUserOrderByLoginTimestampDesc(User user);
    
    List<LoginHistory> findByUserAndLoginStatusOrderByLoginTimestampDesc(User user, String loginStatus);
    
    List<LoginHistory> findByUserAndLoginTimestampAfter(User user, LocalDateTime after);
    
    long countByUserAndLoginStatusAndLoginTimestampAfter(User user, String loginStatus, LocalDateTime after);
}
