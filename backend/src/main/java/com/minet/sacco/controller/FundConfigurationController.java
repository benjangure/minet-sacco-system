package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.FundConfigurationDTO;
import com.minet.sacco.entity.FundConfiguration;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.FundConfigurationService;
import com.minet.sacco.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fund-configurations")
@CrossOrigin(origins = "*")
public class FundConfigurationController {

    @Autowired
    private FundConfigurationService fundConfigurationService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FundConfigurationDTO>>> getAllFunds() {
        try {
            List<FundConfiguration> funds = fundConfigurationService.getAllFunds();
            List<FundConfigurationDTO> dtos = funds.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse<>(true, "Funds retrieved successfully", dtos));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Error retrieving funds: " + e.getMessage(), null));
        }
    }

    @GetMapping("/enabled")
    public ResponseEntity<ApiResponse<List<FundConfigurationDTO>>> getEnabledFunds() {
        List<FundConfiguration> funds = fundConfigurationService.getEnabledFunds();
        List<FundConfigurationDTO> dtos = funds.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Enabled funds retrieved successfully", dtos));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<FundConfigurationDTO>> updateFundConfiguration(
            @PathVariable Long id,
            @RequestBody FundConfigurationDTO dto,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            FundConfiguration updatedConfig = convertToEntity(dto);
            FundConfiguration saved = fundConfigurationService.updateFundConfiguration(id, updatedConfig, user);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Fund configuration updated successfully", convertToDTO(saved)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<FundConfigurationDTO>> toggleFund(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            FundConfiguration toggled = fundConfigurationService.toggleFund(id, user);
            return ResponseEntity.ok(new ApiResponse<>(true, "Fund toggled successfully", convertToDTO(toggled)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    private FundConfigurationDTO convertToDTO(FundConfiguration entity) {
        FundConfigurationDTO dto = new FundConfigurationDTO();
        dto.setId(entity.getId());
        // fundType is now stored as string, just pass it through
        dto.setFundType(entity.getFundType());
        dto.setEnabled(entity.getEnabled());
        dto.setDisplayName(entity.getDisplayName());
        dto.setDescription(entity.getDescription());
        dto.setMinimumAmount(entity.getMinimumAmount());
        dto.setMaximumAmount(entity.getMaximumAmount());
        dto.setDisplayOrder(entity.getDisplayOrder());
        return dto;
    }

    private FundConfiguration convertToEntity(FundConfigurationDTO dto) {
        FundConfiguration entity = new FundConfiguration();
        entity.setEnabled(dto.getEnabled());
        entity.setDisplayName(dto.getDisplayName());
        entity.setDescription(dto.getDescription());
        entity.setMinimumAmount(dto.getMinimumAmount());
        entity.setMaximumAmount(dto.getMaximumAmount());
        entity.setDisplayOrder(dto.getDisplayOrder());
        return entity;
    }
}




