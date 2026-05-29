package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service to initialize member login credentials on application startup
 * This ensures migrated members (EMP019, EMP020) can login with their National IDs
 */
@Service
public class MemberLoginInitializationService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void initializeMemberLogins() {
        // Create login credentials for EMP019
        createMemberLoginIfNotExists("EMP019", "11111111", "samuel.kipchoge@company.com");
        
        // Create login credentials for EMP020
        createMemberLoginIfNotExists("EMP020", "87600321", "grace.omondi@company.com");
    }

    private void createMemberLoginIfNotExists(String memberNumber, String nationalId, String email) {
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByUsername(memberNumber);
        if (existingUser.isPresent()) {
            System.out.println("User account for " + memberNumber + " already exists");
            return;
        }

        // Find the member
        Optional<Member> memberOpt = memberRepository.findByMemberNumber(memberNumber);
        if (!memberOpt.isPresent()) {
            System.out.println("Member " + memberNumber + " not found in database");
            return;
        }

        Member member = memberOpt.get();

        // Create user account
        User user = new User();
        user.setUsername(memberNumber);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(nationalId));
        user.setRole(User.Role.MEMBER);
        user.setMemberId(member.getId());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        System.out.println("✓ Created user account for " + memberNumber + " with encoded password from National ID");
    }
}
