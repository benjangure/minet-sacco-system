package com.minet.sacco.security;

import com.minet.sacco.entity.User;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.MemberSuspension;
import com.minet.sacco.entity.MemberExit;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.MemberSuspensionRepository;
import com.minet.sacco.repository.MemberExitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberSuspensionRepository memberSuspensionRepository;

    @Autowired
    private MemberExitRepository memberExitRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Check if user is a MEMBER - validate member status
        if (user.getRole() == User.Role.MEMBER) {
            Member member = memberRepository.findByEmployeeId(username)
                    .or(() -> memberRepository.findByMemberNumber(username))
                    .orElseThrow(() -> new UsernameNotFoundException("Member not found: " + username));

            // Check if member is suspended
            Optional<MemberSuspension> suspension = memberSuspensionRepository.findByMemberIdAndIsActiveTrue(member.getId());
            if (suspension.isPresent()) {
                throw new UsernameNotFoundException("Member is suspended: " + username + ". Please contact administrator.");
            }

            // Check if member has exited
            Optional<MemberExit> exit = memberExitRepository.findByMemberIdAndStatus(member.getId(), "APPROVED");
            if (exit.isPresent()) {
                throw new UsernameNotFoundException("Member has exited: " + username + ". Please contact administrator.");
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}