package com.minet.sacco.service;

import com.minet.sacco.entity.SystemSettings;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.SystemSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SystemSettingsService {

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    @Autowired
    private AuditService auditService;

    /**
     * Get all system settings
     */
    public List<SystemSettings> getAllSettings() {
        return systemSettingsRepository.findAll();
    }

    /**
     * Get setting by key with caching
     */
    @Cacheable(value = "systemSettings", key = "#key")
    public Optional<SystemSettings> getSettingByKey(String key) {
        return systemSettingsRepository.findBySettingKey(key);
    }

    /**
     * Get setting value as string
     */
    public String getSettingValue(String key) {
        return getSettingByKey(key)
                .map(SystemSettings::getSettingValue)
                .orElse(null);
    }

    /**
     * Get setting value as integer
     */
    public Integer getSettingAsInteger(String key) {
        return getSettingByKey(key)
                .map(SystemSettings::getIntValue)
                .orElse(null);
    }

    /**
     * Get setting value as boolean
     */
    public Boolean getSettingAsBoolean(String key) {
        return getSettingByKey(key)
                .map(SystemSettings::getBooleanValue)
                .orElse(false);
    }

    /**
     * Get setting value as double
     */
    public Double getSettingAsDouble(String key) {
        return getSettingByKey(key)
                .map(SystemSettings::getDoubleValue)
                .orElse(null);
    }

    /**
     * Update setting value
     */
    @Transactional
    @CacheEvict(value = "systemSettings", key = "#key")
    public SystemSettings updateSetting(String key, String value, User updatedBy) {
        SystemSettings setting = systemSettingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));

        String oldValue = setting.getSettingValue();
        setting.setSettingValue(value);
        setting.setUpdatedBy(updatedBy);

        SystemSettings updated = systemSettingsRepository.save(setting);

        // Audit log
        auditService.logAction(updatedBy, "SYSTEM_SETTING_UPDATED",
                "SystemSettings", updated.getId(),
                "Key: " + key + ", Old: " + oldValue + ", New: " + value,
                "System setting updated", "SUCCESS");

        return updated;
    }

    /**
     * Create new setting
     */
    @Transactional
    public SystemSettings createSetting(String key, String value, String type, String description, User createdBy) {
        if (systemSettingsRepository.findBySettingKey(key).isPresent()) {
            throw new RuntimeException("Setting already exists: " + key);
        }

        SystemSettings setting = new SystemSettings();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setSettingType(type);
        setting.setDescription(description);
        setting.setUpdatedBy(createdBy);

        SystemSettings saved = systemSettingsRepository.save(setting);

        auditService.logAction(createdBy, "SYSTEM_SETTING_CREATED",
                "SystemSettings", saved.getId(),
                "Key: " + key + ", Value: " + value,
                "New system setting created", "SUCCESS");

        return saved;
    }

    /**
     * Initialize default settings if they don't exist
     */
    @Transactional
    public void initializeDefaultSettings() {
        // Maximum active loans
        if (systemSettingsRepository.findBySettingKey("MAX_ACTIVE_LOANS").isEmpty()) {
            SystemSettings setting = new SystemSettings();
            setting.setSettingKey("MAX_ACTIVE_LOANS");
            setting.setSettingValue("3");
            setting.setSettingType("INTEGER");
            setting.setDescription("Maximum number of active loans per member");
            systemSettingsRepository.save(setting);
        }

        // Loan multiplier
        if (systemSettingsRepository.findBySettingKey("LOAN_MULTIPLIER").isEmpty()) {
            SystemSettings setting = new SystemSettings();
            setting.setSettingKey("LOAN_MULTIPLIER");
            setting.setSettingValue("3");
            setting.setSettingType("DECIMAL");
            setting.setDescription("Loan multiplier (3x savings)");
            systemSettingsRepository.save(setting);
        }

        // Minimum contribution months
        if (systemSettingsRepository.findBySettingKey("MIN_CONTRIBUTION_MONTHS").isEmpty()) {
            SystemSettings setting = new SystemSettings();
            setting.setSettingKey("MIN_CONTRIBUTION_MONTHS");
            setting.setSettingValue("6");
            setting.setSettingType("INTEGER");
            setting.setDescription("Minimum contribution months required for loan eligibility");
            systemSettingsRepository.save(setting);
        }

        // Emergency fund enabled
        if (systemSettingsRepository.findBySettingKey("EMERGENCY_FUND_ENABLED").isEmpty()) {
            SystemSettings setting = new SystemSettings();
            setting.setSettingKey("EMERGENCY_FUND_ENABLED");
            setting.setSettingValue("false");
            setting.setSettingType("BOOLEAN");
            setting.setDescription("Whether emergency fund is enabled");
            systemSettingsRepository.save(setting);
        }

        // Test mode override
        if (systemSettingsRepository.findBySettingKey("TEST_MODE_OVERRIDE").isEmpty()) {
            SystemSettings setting = new SystemSettings();
            setting.setSettingKey("TEST_MODE_OVERRIDE");
            setting.setSettingValue("false");
            setting.setSettingType("BOOLEAN");
            setting.setDescription("Test mode override for development");
            systemSettingsRepository.save(setting);
        }
    }
}
