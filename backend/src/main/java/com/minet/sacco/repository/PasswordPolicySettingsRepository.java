package com.minet.sacco.repository;

import com.minet.sacco.entity.PasswordPolicySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordPolicySettingsRepository extends JpaRepository<PasswordPolicySettings, Long> {

    /**
     * Find a setting by key
     */
    Optional<PasswordPolicySettings> findBySettingKey(String settingKey);
}
