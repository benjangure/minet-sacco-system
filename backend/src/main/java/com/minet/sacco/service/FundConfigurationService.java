package com.minet.sacco.service;

import com.minet.sacco.entity.FundConfiguration;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.FundConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FundConfigurationService {

    @Autowired
    private FundConfigurationRepository fundConfigurationRepository;

    @Autowired
    private AuditService auditService;

    public List<FundConfiguration> getAllFunds() {
        return fundConfigurationRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<FundConfiguration> getEnabledFunds() {
        return fundConfigurationRepository.findByEnabledTrueOrderByDisplayOrderAsc();
    }

    public FundConfiguration getFundByType(String fundType) {
        return fundConfigurationRepository.findByFundType(fundType)
            .orElseThrow(() -> new RuntimeException("Fund configuration not found: " + fundType));
    }

    public boolean isFundEnabled(String fundType) {
        return fundConfigurationRepository.findByFundType(fundType)
            .map(FundConfiguration::getEnabled)
            .orElse(false);
    }

    @Transactional
    public FundConfiguration updateFundConfiguration(Long id, FundConfiguration updatedConfig, User user) {
        FundConfiguration existingConfig = fundConfigurationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Fund configuration not found"));

        String oldValue = String.format("Enabled: %s, Name: %s, Min: %s, Max: %s",
            existingConfig.getEnabled(),
            existingConfig.getDisplayName(),
            existingConfig.getMinimumAmount(),
            existingConfig.getMaximumAmount());

        existingConfig.setEnabled(updatedConfig.getEnabled());
        existingConfig.setDisplayName(updatedConfig.getDisplayName());
        existingConfig.setDescription(updatedConfig.getDescription());
        existingConfig.setMinimumAmount(updatedConfig.getMinimumAmount());
        existingConfig.setMaximumAmount(updatedConfig.getMaximumAmount());
        existingConfig.setDisplayOrder(updatedConfig.getDisplayOrder());
        existingConfig.setUpdatedBy(user);

        FundConfiguration saved = fundConfigurationRepository.save(existingConfig);

        String newValue = String.format("Enabled: %s, Name: %s, Min: %s, Max: %s",
            saved.getEnabled(),
            saved.getDisplayName(),
            saved.getMinimumAmount(),
            saved.getMaximumAmount());

        auditService.logAction(user, "UPDATE_FUND_CONFIG", "FundConfiguration", saved.getId(),
            "Updated fund configuration: " + saved.getFundType(), oldValue, newValue);

        return saved;
    }

    @Transactional
    public FundConfiguration toggleFund(Long id, User user) {
        FundConfiguration config = fundConfigurationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Fund configuration not found"));

        boolean oldEnabled = config.getEnabled();
        config.setEnabled(!oldEnabled);
        config.setUpdatedBy(user);

        FundConfiguration saved = fundConfigurationRepository.save(config);

        auditService.logAction(user, "TOGGLE_FUND", "FundConfiguration", saved.getId(),
            String.format("%s fund: %s", oldEnabled ? "Disabled" : "Enabled", saved.getFundType()),
            String.valueOf(oldEnabled), String.valueOf(saved.getEnabled()));

        return saved;
    }
}
