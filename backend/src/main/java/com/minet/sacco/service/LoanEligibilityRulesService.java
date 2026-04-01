package com.minet.sacco.service;

import com.minet.sacco.entity.LoanEligibilityRules;
import com.minet.sacco.repository.LoanEligibilityRulesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoanEligibilityRulesService {

    @Autowired
    private LoanEligibilityRulesRepository rulesRepository;

    /**
     * Get the current eligibility rules (there should only be one)
     */
    public LoanEligibilityRules getRules() {
        List<LoanEligibilityRules> rules = rulesRepository.findAll();
        if (rules.isEmpty()) {
            // Create default rules if none exist
            return createDefaultRules();
        }
        return rules.get(0);
    }

    /**
     * Update eligibility rules
     */
    @Transactional
    public LoanEligibilityRules updateRules(LoanEligibilityRules updatedRules) {
        LoanEligibilityRules existing = getRules();
        
        if (updatedRules.getMinMemberSavings() != null) {
            existing.setMinMemberSavings(updatedRules.getMinMemberSavings());
        }
        if (updatedRules.getMinMemberShares() != null) {
            existing.setMinMemberShares(updatedRules.getMinMemberShares());
        }
        if (updatedRules.getMinSavingsToLoanRatio() != null) {
            existing.setMinSavingsToLoanRatio(updatedRules.getMinSavingsToLoanRatio());
        }
        if (updatedRules.getMaxOutstandingToSavingsRatio() != null) {
            existing.setMaxOutstandingToSavingsRatio(updatedRules.getMaxOutstandingToSavingsRatio());
        }
        if (updatedRules.getMaxActiveLoans() != null) {
            existing.setMaxActiveLoans(updatedRules.getMaxActiveLoans());
        }
        if (updatedRules.getMinGuarantorSavings() != null) {
            existing.setMinGuarantorSavings(updatedRules.getMinGuarantorSavings());
        }
        if (updatedRules.getMinGuarantorShares() != null) {
            existing.setMinGuarantorShares(updatedRules.getMinGuarantorShares());
        }
        if (updatedRules.getMinGuarantorSavingsToLoanRatio() != null) {
            existing.setMinGuarantorSavingsToLoanRatio(updatedRules.getMinGuarantorSavingsToLoanRatio());
        }
        if (updatedRules.getMaxGuarantorOutstandingToSavingsRatio() != null) {
            existing.setMaxGuarantorOutstandingToSavingsRatio(updatedRules.getMaxGuarantorOutstandingToSavingsRatio());
        }
        if (updatedRules.getMaxGuarantorCommitments() != null) {
            existing.setMaxGuarantorCommitments(updatedRules.getMaxGuarantorCommitments());
        }
        if (updatedRules.getAllowDefaulters() != null) {
            existing.setAllowDefaulters(updatedRules.getAllowDefaulters());
        }
        if (updatedRules.getAllowExitedMembers() != null) {
            existing.setAllowExitedMembers(updatedRules.getAllowExitedMembers());
        }
        if (updatedRules.getMaxLoanTermMonths() != null) {
            existing.setMaxLoanTermMonths(updatedRules.getMaxLoanTermMonths());
        }
        if (updatedRules.getMaxLoanToSavingsMultiplier() != null) {
            existing.setMaxLoanToSavingsMultiplier(updatedRules.getMaxLoanToSavingsMultiplier());
        }

        return rulesRepository.save(existing);
    }

    /**
     * Create default rules
     */
    @Transactional
    private LoanEligibilityRules createDefaultRules() {
        LoanEligibilityRules rules = new LoanEligibilityRules();
        
        // Set default values for all fields
        rules.setMinMemberSavings(new java.math.BigDecimal("5000"));
        rules.setMinMemberShares(new java.math.BigDecimal("1000"));
        rules.setMinSavingsToLoanRatio(new java.math.BigDecimal("0.30"));
        rules.setMaxOutstandingToSavingsRatio(new java.math.BigDecimal("0.50"));
        rules.setMaxActiveLoans(3);
        
        rules.setMinGuarantorSavings(new java.math.BigDecimal("10000"));
        rules.setMinGuarantorShares(new java.math.BigDecimal("2000"));
        rules.setMinGuarantorSavingsToLoanRatio(new java.math.BigDecimal("0.50"));
        rules.setMaxGuarantorOutstandingToSavingsRatio(new java.math.BigDecimal("0.50"));
        rules.setMaxGuarantorCommitments(3);
        
        rules.setAllowDefaulters(false);
        rules.setAllowExitedMembers(false);
        rules.setMaxLoanToSavingsMultiplier(new java.math.BigDecimal("3.0"));
        
        return rulesRepository.save(rules);
    }
}
