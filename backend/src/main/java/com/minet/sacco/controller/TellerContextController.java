package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.service.TellerContextService;
import com.minet.sacco.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teller")
@CrossOrigin
public class TellerContextController {

    @Autowired
    private TellerContextService tellerContextService;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberRepository memberRepository;

    @PostMapping("/set-member-context/{memberId}")
    @PreAuthorize("hasRole('ROLE_TELLER')")
    public ResponseEntity<ApiResponse<String>> setMemberContext(
            @PathVariable Long memberId,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        tellerContextService.setMemberContext(user.getId(), memberId, user);
        return ResponseEntity.ok(ApiResponse.success("Member context set successfully", "OK"));
    }

    @GetMapping("/current-member-context")
    @PreAuthorize("hasRole('ROLE_TELLER')")
    public ResponseEntity<ApiResponse<MemberContextResponse>> getCurrentMemberContext(
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long memberId = tellerContextService.getMemberContext(user.getId());
        if (memberId == null) {
            return ResponseEntity.ok(ApiResponse.success("No member context set", null));
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        MemberContextResponse response = new MemberContextResponse(
                member.getId(),
                member.getMemberNumber(),
                member.getFirstName(),
                member.getLastName()
        );

        return ResponseEntity.ok(ApiResponse.success("Current member context", response));
    }

    @PostMapping("/clear-member-context")
    @PreAuthorize("hasRole('ROLE_TELLER')")
    public ResponseEntity<ApiResponse<String>> clearMemberContext(
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        tellerContextService.clearMemberContext(user.getId(), user);
        return ResponseEntity.ok(ApiResponse.success("Member context cleared", "OK"));
    }

    public static class MemberContextResponse {
        public Long id;
        public String memberNumber;
        public String firstName;
        public String lastName;

        public MemberContextResponse(Long id, String memberNumber, String firstName, String lastName) {
            this.id = id;
            this.memberNumber = memberNumber;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public Long getId() { return id; }
        public String getMemberNumber() { return memberNumber; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
    }
}
