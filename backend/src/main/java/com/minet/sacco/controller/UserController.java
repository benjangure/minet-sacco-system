package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.DeletionApprovalDTO;
import com.minet.sacco.dto.UserDTO;
import com.minet.sacco.dto.UserDeletionRequestDTO;
import com.minet.sacco.entity.User;
import com.minet.sacco.entity.UserDeletionRequest;
import com.minet.sacco.repository.UserDeletionRequestRepository;
import com.minet.sacco.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDeletionRequestRepository deletionRequestRepository;

    // Role hierarchy: what roles each role can create
    private static final List<String> ADMIN_CAN_CREATE = Arrays.asList("ADMIN", "TREASURER", "LOAN_OFFICER", "CREDIT_COMMITTEE", "AUDITOR");
    private static final List<String> TREASURER_CAN_CREATE = Arrays.asList("TELLER", "CUSTOMER_SUPPORT");

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        
        // Convert to DTOs with creator username
        List<UserDTO> userDTOs = users.stream().map(user -> {
            String createdByUsername = "System";
            if (user.getCreatedBy() != null) {
                createdByUsername = userService.getUserById(user.getCreatedBy())
                        .map(User::getUsername)
                        .orElse("Unknown");
            }
            return new UserDTO(user, createdByUsername);
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", userDTOs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success("User found", user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<User>> createUser(
            @Valid @RequestBody User user,
            Authentication authentication) {
        
        // Get current user's role
        String currentUserRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("");
        
        // Get current user ID
        String currentUsername = authentication.getName();
        User currentUser = userService.getUserByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        // Validate role hierarchy
        String targetRole = user.getRole().name();
        boolean canCreate = false;
        
        if ("ADMIN".equals(currentUserRole) && ADMIN_CAN_CREATE.contains(targetRole)) {
            canCreate = true;
        } else if ("TREASURER".equals(currentUserRole) && TREASURER_CAN_CREATE.contains(targetRole)) {
            canCreate = true;
        }
        
        if (!canCreate) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You are not authorized to create users with role: " + targetRole));
        }
        
        if (userService.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username already exists"));
        }
        if (userService.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email already exists"));
        }
        
        // Set who created this user
        user.setCreatedBy(currentUser.getId());
        
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", createdUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        user.setId(id);
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
    }

    @PostMapping("/request-deletion")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<UserDeletionRequest>> requestUserDeletion(
            @Valid @RequestBody UserDeletionRequestDTO request,
            Authentication authentication) {
        
        // Get current user
        String currentUsername = authentication.getName();
        User currentUser = userService.getUserByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        // Get user to delete
        User userToDelete = userService.getUserById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Prevent deleting ADMIN users
        if (userToDelete.getRole() == User.Role.ADMIN) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Cannot delete ADMIN users"));
        }
        
        // Check if user created this account
        if (userToDelete.getCreatedBy() == null || !userToDelete.getCreatedBy().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You can only request deletion of users you created"));
        }
        
        // Check if there's already a pending request
        if (deletionRequestRepository.findByUserIdAndStatus(request.getUserId(), "PENDING").isPresent()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("There is already a pending deletion request for this user"));
        }
        
        // Create deletion request
        UserDeletionRequest deletionRequest = new UserDeletionRequest();
        deletionRequest.setUser(userToDelete);
        deletionRequest.setRequestedBy(currentUser);
        deletionRequest.setReason(request.getReason());
        deletionRequest.setStatus("PENDING");
        
        UserDeletionRequest saved = deletionRequestRepository.save(deletionRequest);
        return ResponseEntity.ok(ApiResponse.success("Deletion request submitted. Awaiting approval from another administrator.", saved));
    }

    @GetMapping("/deletion-requests")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDeletionRequest>>> getPendingDeletionRequests() {
        List<UserDeletionRequest> requests = deletionRequestRepository.findByStatus("PENDING");
        return ResponseEntity.ok(ApiResponse.success("Deletion requests retrieved successfully", requests));
    }

    @PostMapping("/approve-deletion")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<String>> approveDeletion(
            @Valid @RequestBody DeletionApprovalDTO approval,
            Authentication authentication) {
        
        // Get current user
        String currentUsername = authentication.getName();
        User currentUser = userService.getUserByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        // Get deletion request
        UserDeletionRequest deletionRequest = deletionRequestRepository.findById(approval.getRequestId())
                .orElseThrow(() -> new RuntimeException("Deletion request not found"));
        
        // Prevent approving your own request
        if (deletionRequest.getRequestedBy().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You cannot approve your own deletion request"));
        }
        
        // Check if already processed
        if (!"PENDING".equals(deletionRequest.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("This deletion request has already been processed"));
        }
        
        if (approval.getApproved()) {
            // Approve and delete user
            deletionRequest.setStatus("APPROVED");
            deletionRequest.setApprovedBy(currentUser);
            deletionRequest.setApprovedAt(LocalDateTime.now());
            deletionRequestRepository.save(deletionRequest);
            
            // Delete the user
            userService.deleteUser(deletionRequest.getUser().getId());
            
            return ResponseEntity.ok(ApiResponse.success("User deletion approved and executed successfully"));
        } else {
            // Reject
            deletionRequest.setStatus("REJECTED");
            deletionRequest.setApprovedBy(currentUser);
            deletionRequest.setApprovedAt(LocalDateTime.now());
            deletionRequest.setRejectedReason(approval.getRejectionReason());
            deletionRequestRepository.save(deletionRequest);
            
            return ResponseEntity.ok(ApiResponse.success("User deletion request rejected"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        
        // Get current user
        String currentUsername = authentication.getName();
        User currentUser = userService.getUserByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        // Get user to delete
        User userToDelete = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if current user created this user
        if (userToDelete.getCreatedBy() == null || !userToDelete.getCreatedBy().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You can only delete users you created"));
        }
        
        // Prevent deleting ADMIN users
        if (userToDelete.getRole() == User.Role.ADMIN) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Cannot delete ADMIN users"));
        }
        
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deactivateUser(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication) {
        
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() == User.Role.ADMIN) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Cannot deactivate ADMIN users"));
        }
        
        userService.deactivateUser(id, reason);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully"));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<String>> reactivateUser(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication) {
        
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        userService.reactivateUser(id, reason);
        return ResponseEntity.ok(ApiResponse.success("User reactivated successfully"));
    }

    @PostMapping("/{id}/change-role")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<String>> changeUserRole(
            @PathVariable Long id,
            @RequestParam String newRole,
            @RequestParam String reason,
            Authentication authentication) {
        
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() == User.Role.ADMIN) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Cannot change ADMIN user role"));
        }
        
        try {
            userService.changeUserRole(id, newRole, reason);
            return ResponseEntity.ok(ApiResponse.success("User role changed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid role: " + newRole));
        }
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            @RequestParam String reason,
            Authentication authentication) {
        
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Password must be at least 8 characters"));
        }
        
        userService.changePassword(id, newPassword, reason);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @GetMapping("/{id}/activity-log")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Object>> getUserActivityLog(@PathVariable Long id) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(ApiResponse.success("Activity log retrieved successfully", 
                userService.getUserActivityLog(id)));
    }

    @GetMapping("/activity-log/action/{action}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getActivityLogByAction(@PathVariable String action) {
        return ResponseEntity.ok(ApiResponse.success("Activity log retrieved successfully", 
                userService.getActivityLogByAction(action)));
    }
}




