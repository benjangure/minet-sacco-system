package com.minet.sacco.service;

import com.minet.sacco.entity.User;
import com.minet.sacco.entity.UserActivityLog;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.repository.UserActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityLogRepository activityLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        // Exclude MEMBER role — members are managed via the Members page, not User Management
        return userRepository.findAll().stream()
            .filter(u -> u.getRole() != User.Role.MEMBER)
            .collect(java.util.stream.Collectors.toList());
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Cacheable(value = "users", key = "#username")
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByMemberId(Long memberId) {
        return userRepository.findByMemberId(memberId);
    }

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#user.username")
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deactivateUser(Long id, String reason) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEnabled(false);
            userRepository.save(user);
            
            // Log the deactivation
            logActivity(user, "DEACTIVATE_USER", "User deactivated. Reason: " + reason);
        }
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void reactivateUser(Long id, String reason) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEnabled(true);
            userRepository.save(user);
            
            // Log the reactivation
            logActivity(user, "REACTIVATE_USER", "User reactivated. Reason: " + reason);
        }
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            logActivity(user, "DELETE_USER", "User deleted from system");
            userRepository.deleteById(id);
        }
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void changeUserRole(Long id, String newRole, String reason) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String oldRole = user.getRole().toString();
            user.setRole(User.Role.valueOf(newRole));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            // Log the role change
            logActivity(user, "CHANGE_ROLE", "Role changed from " + oldRole + " to " + newRole + ". Reason: " + reason);
        }
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void changePassword(Long id, String newPassword, String reason) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            // Log the password change
            logActivity(user, "CHANGE_PASSWORD", "Password changed. Reason: " + reason);
        }
    }

    public void logActivity(User user, String action, String details) {
        UserActivityLog log = new UserActivityLog(user, action, details, null);
        activityLogRepository.save(log);
    }

    public void logActivity(User user, String action, String details, String ipAddress) {
        UserActivityLog log = new UserActivityLog(user, action, details, ipAddress);
        activityLogRepository.save(log);
    }

    public List<UserActivityLog> getUserActivityLog(Long userId) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<UserActivityLog> getActivityLogByAction(String action) {
        return activityLogRepository.findByActionOrderByCreatedAtDesc(action);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}