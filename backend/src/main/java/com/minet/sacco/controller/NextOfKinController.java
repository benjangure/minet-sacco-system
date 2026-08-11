package com.minet.sacco.controller;

import com.minet.sacco.dto.NextOfKinDTO;
import com.minet.sacco.service.NextOfKinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/next-of-kin")
@CrossOrigin(origins = "*")
public class NextOfKinController {
    
    private static final Logger log = LoggerFactory.getLogger(NextOfKinController.class);
    
    @Autowired
    private NextOfKinService nextOfKinService;
    
    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'LOAN_OFFICER', 'MEMBER')")
    public ResponseEntity<?> getNextOfKinByMember(@PathVariable Long memberId) {
        try {
            List<NextOfKinDTO> nextOfKinList = nextOfKinService.getNextOfKinByMemberId(memberId);
            BigDecimal totalPercentage = nextOfKinService.getTotalPercentageForMember(memberId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("nextOfKin", nextOfKinList);
            response.put("totalPercentage", totalPercentage);
            response.put("remainingPercentage", new BigDecimal("100").subtract(totalPercentage));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching next of kin for member {}: {}", memberId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/member/{memberId}/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'MEMBER')")
    public ResponseEntity<?> saveNextOfKinList(
            @PathVariable Long memberId,
            @RequestBody List<NextOfKinDTO> nextOfKinList) {
        try {
            List<NextOfKinDTO> saved = nextOfKinService.saveNextOfKinList(memberId, nextOfKinList);
            return ResponseEntity.ok(Map.of(
                "message", "Next of kin updated successfully",
                "nextOfKin", saved
            ));
        } catch (RuntimeException e) {
            log.error("Error saving next of kin list: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error saving next of kin list: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save next of kin"));
        }
    }
    
    @PostMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'MEMBER')")
    public ResponseEntity<?> addNextOfKin(
            @PathVariable Long memberId,
            @RequestBody NextOfKinDTO nextOfKinDTO) {
        try {
            NextOfKinDTO saved = nextOfKinService.addNextOfKin(memberId, nextOfKinDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Next of kin added successfully",
                "nextOfKin", saved
            ));
        } catch (RuntimeException e) {
            log.error("Error adding next of kin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding next of kin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add next of kin"));
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'MEMBER')")
    public ResponseEntity<?> updateNextOfKin(
            @PathVariable Long id,
            @RequestBody NextOfKinDTO nextOfKinDTO) {
        try {
            NextOfKinDTO updated = nextOfKinService.updateNextOfKin(id, nextOfKinDTO);
            return ResponseEntity.ok(Map.of(
                "message", "Next of kin updated successfully",
                "nextOfKin", updated
            ));
        } catch (RuntimeException e) {
            log.error("Error updating next of kin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating next of kin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update next of kin"));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'MEMBER')")
    public ResponseEntity<?> deleteNextOfKin(@PathVariable Long id) {
        try {
            nextOfKinService.deleteNextOfKin(id);
            return ResponseEntity.ok(Map.of("message", "Next of kin deleted successfully"));
        } catch (RuntimeException e) {
            log.error("Error deleting next of kin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting next of kin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete next of kin"));
        }
    }
}
