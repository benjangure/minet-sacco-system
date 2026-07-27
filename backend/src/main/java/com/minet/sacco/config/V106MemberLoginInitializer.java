package com.minet.sacco.config;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Initializer that creates user login accounts for all members that don't have one.
 * This allows members added via migrations to login using their National ID as password.
 * 
 * Pattern: Username = member_number, Password = BCrypt(national_id)
 * This mirrors the behavior of BulkProcessingService.createMemberLoginCredentials()
 */
@Component
public class V106MemberLoginInitializer {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void initializeMemberLogins() {
        try {
            // Get all members
            List<Member> allMembers = memberRepository.findAll();
            
            int created = 0;
            int updated = 0;
            int skipped = 0;
            
            for (Member member : allMembers) {
                String username = member.getMemberNumber();
                
                if (username == null || username.isBlank()) {
                    skipped++;
                    continue;
                }
                
                // Check if user already exists
                Optional<User> existingUser = userRepository.findByUsername(username);
                User user;
                
                if (existingUser.isPresent()) {
                    // User exists - only update if member_id is not set (not yet linked)
                    user = existingUser.get();
                    if (user.getMemberId() == null) {
                        // Link user to member and set correct password
                        String encodedPassword = passwordEncoder.encode(member.getNationalId());
                        user.setPassword(encodedPassword);
                        user.setMemberId(member.getId());  // CRITICAL: Link user to member
                        user.setUpdatedAt(LocalDateTime.now());
                        userRepository.save(user);
                        updated++;
                    }
                    // If member_id is already set, skip - password is already correct
                } else {
                    // Create new user account with encoded national ID as password
                    user = new User();
                    user.setUsername(username);
                    user.setEmail(member.getEmail() != null && !member.getEmail().isBlank()
                        ? member.getEmail()
                        : username + "@minet.sacco");
                    user.setPassword(passwordEncoder.encode(member.getNationalId()));
                    user.setRole(User.Role.MEMBER);
                    user.setMemberId(member.getId());
                    user.setEnabled(true);
                    user.setCreatedAt(LocalDateTime.now());
                    
                    userRepository.save(user);
                    created++;
                }
            }
            
            if (created > 0) {
                System.out.println("✓ V106MemberLoginInitializer: Created " + created + " user accounts for members");
            }
            if (updated > 0) {
                System.out.println("✓ V106MemberLoginInitializer: Updated " + updated + " user accounts with correct passwords");
            }
            if (skipped > 0) {
                System.out.println("✓ V106MemberLoginInitializer: Skipped " + skipped + " members (no member number)");
            }
            
        } catch (Exception e) {
            System.err.println("✗ V106MemberLoginInitializer failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
