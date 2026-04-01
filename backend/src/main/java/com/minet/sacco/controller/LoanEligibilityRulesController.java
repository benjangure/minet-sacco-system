package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.LoanEligibilityRules;
import com.minet.sacco.service.LoanEligibilityRulesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan-eligibility-rules")
@CrossOrigin(origins = "*")
public class LoanEligibilityRulesController {

    @Autowired
    private LoanEligibilityRulesService rulesService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<LoanEligibilityRules>> getRules() {
        try {
            LoanEligibilityRules rules = rulesService.getRules();
            return ResponseEntity.ok(new ApiResponse<>(true, "Rules retrieved successfully", rules));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PutMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<LoanEligibilityRules>> updateRules(@RequestBody LoanEligibilityRules rules) {
        try {
            LoanEligibilityRules updated = rulesService.updateRules(rules);
            return ResponseEntity.ok(new ApiResponse<>(true, "Rules updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
