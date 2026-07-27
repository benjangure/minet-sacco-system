package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.SystemSettings;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.SystemSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SystemSettingsController {

    @Autowired
    private SystemSettingsService systemSettingsService;

    /**
     * Get all system settings
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSettings>>> getAllSettings() {
        List<SystemSettings> settings = systemSettingsService.getAllSettings();
        return ResponseEntity.ok(new ApiResponse<>(true, "Settings retrieved successfully", settings));
    }

    /**
     * Get specific setting by key
     */
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSettings>> getSettingByKey(@PathVariable String key) {
        return systemSettingsService.getSettingByKey(key)
                .map(setting -> ResponseEntity.ok(new ApiResponse<>(true, "Setting retrieved", setting)))
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Setting not found: " + key, null)));
    }

    /**
     * Update setting value
     */
    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSettings>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String value = request.get("value");
            if (value == null || value.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Value is required", null));
            }

            User user = (User) authentication.getPrincipal();
            SystemSettings updated = systemSettingsService.updateSetting(key, value, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Setting updated successfully", updated));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Create new setting
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SystemSettings>> createSetting(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String key = request.get("key");
            String value = request.get("value");
            String type = request.get("type");
            String description = request.get("description");

            if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Key and value are required", null));
            }

            User user = (User) authentication.getPrincipal();
            SystemSettings created = systemSettingsService.createSetting(key, value, type, description, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Setting created successfully", created));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Initialize default settings
     */
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<String>> initializeDefaults() {
        try {
            systemSettingsService.initializeDefaultSettings();
            return ResponseEntity.ok(new ApiResponse<>(true, "Default settings initialized", "Success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error initializing settings: " + e.getMessage(), null));
        }
    }
}
