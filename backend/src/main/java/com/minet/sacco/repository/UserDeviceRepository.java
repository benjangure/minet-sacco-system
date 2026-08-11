package com.minet.sacco.repository;

import com.minet.sacco.entity.User;
import com.minet.sacco.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    
    Optional<UserDevice> findByUserAndDeviceFingerprint(User user, String deviceFingerprint);
    
    List<UserDevice> findByUserOrderByLastLoginAtDesc(User user);
    
    List<UserDevice> findByUserAndIsTrustedTrue(User user);
    
    long countByUser(User user);
}
