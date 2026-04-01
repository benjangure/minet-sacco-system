package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.MemberApprovalRequest;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.MemberService;
import com.minet.sacco.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@CrossOrigin
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private UserService userService;

    private static final String UPLOAD_DIR = "uploads/members/";

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Member>>> getAllMembers() {
        List<Member> members = memberService.getAllMembers();
        return ResponseEntity.ok(ApiResponse.success("Members retrieved successfully", members));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<Member>> getMemberById(@PathVariable Long id) {
        return memberService.getMemberById(id)
                .map(member -> ResponseEntity.ok(ApiResponse.success("Member found", member)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{memberNumber}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<Member>> getMemberByNumber(@PathVariable String memberNumber) {
        return memberService.getMemberByMemberNumber(memberNumber)
                .map(member -> ResponseEntity.ok(ApiResponse.success("Member found", member)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Member>> createMember(
            @Valid @RequestBody Member member,
            Authentication authentication) {
        
        // Get current user ID
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Member createdMember = memberService.createMember(member, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Member application submitted successfully. Awaiting approval.", createdMember));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ROLE_TREASURER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Member>> approveMember(
            @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Member approvedMember = memberService.approveMember(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Member approved successfully", approvedMember));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ROLE_TREASURER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Member>> rejectMember(
            @PathVariable Long id,
            @RequestBody MemberApprovalRequest request,
            Authentication authentication) {
        
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Member rejectedMember = memberService.rejectMember(id, request.getRejectionReason(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Member application rejected", rejectedMember));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Member>> activateMember(
            @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Member activatedMember = memberService.activateMember(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Member activated successfully", activatedMember));
    }

    @PostMapping("/{id}/exit")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Member>> exitMember(
            @PathVariable Long id,
            @RequestBody MemberApprovalRequest request,
            Authentication authentication) {
        
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Member exitedMember = memberService.exitMember(id, request.getRejectionReason(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Member marked as exited successfully", exitedMember));
    }

    @GetMapping("/exited")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LOAN_OFFICER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Member>>> getExitedMembers() {
        List<Member> members = memberService.getExitedMembers();
        return ResponseEntity.ok(ApiResponse.success("Exited members retrieved successfully", members));
    }

    @GetMapping("/exited/outstanding-loans")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LOAN_OFFICER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Member>>> getExitedMembersWithOutstandingLoans() {
        List<Member> members = memberService.getExitedMembersWithOutstandingLoans();
        return ResponseEntity.ok(ApiResponse.success("Exited members with outstanding loans retrieved successfully", members));
    }

    @PostMapping("/{id}/upload-document")
    @PreAuthorize("hasAnyRole('ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<String>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) throws IOException {
        
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || 
            (!contentType.equals("image/jpeg") && 
             !contentType.equals("image/png") && 
             !contentType.equals("application/pdf"))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only JPG, PNG, and PDF files are allowed"));
        }
        
        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File size must not exceed 5MB"));
        }
        
        Member member = memberService.getMemberById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR + id);
        Files.createDirectories(uploadPath);
        
        // Generate unique filename with original extension
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
        String filename = documentType + "_" + UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);
        
        // Save file
        Files.copy(file.getInputStream(), filePath);
        
        // Update member with document path
        String relativePath = UPLOAD_DIR + id + "/" + filename;
        switch (documentType.toLowerCase()) {
            case "id":
                member.setIdDocumentPath(relativePath);
                break;
            case "photo":
                member.setPhotoPath(relativePath);
                break;
            case "application":
                member.setApplicationLetterPath(relativePath);
                break;
            case "kra":
                member.setKraPinPath(relativePath);
                break;
            default:
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid document type"));
        }
        
        memberService.updateMember(member);
        
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", relativePath));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Member>> updateMember(@PathVariable Long id, @Valid @RequestBody Member member) {
        member.setId(id);
        Member updatedMember = memberService.updateMember(member);
        return ResponseEntity.ok(ApiResponse.success("Member updated successfully", updatedMember));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(ApiResponse.success("Member deleted successfully"));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_TELLER', 'ROLE_LOAN_OFFICER', 'ROLE_CUSTOMER_SUPPORT', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Member>>> getMembersByStatus(@PathVariable String status) {
        List<Member> members = memberService.getMembersByStatus(Member.Status.valueOf(status.toUpperCase()));
        return ResponseEntity.ok(ApiResponse.success("Members retrieved successfully", members));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ROLE_TREASURER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Member>>> getPendingMembers() {
        List<Member> members = memberService.getMembersByStatus(Member.Status.PENDING);
        return ResponseEntity.ok(ApiResponse.success("Pending members retrieved successfully", members));
    }

    @GetMapping("/{id}/document/{documentType}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long id,
            @PathVariable String documentType) throws IOException {
        
        Member member = memberService.getMemberById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        String documentPath = null;
        switch (documentType.toLowerCase()) {
            case "id":
                documentPath = member.getIdDocumentPath();
                break;
            case "photo":
                documentPath = member.getPhotoPath();
                break;
            case "application":
                documentPath = member.getApplicationLetterPath();
                break;
            case "kra":
                documentPath = member.getKraPinPath();
                break;
            default:
                return ResponseEntity.badRequest().build();
        }
        
        if (documentPath == null) {
            return ResponseEntity.notFound().build();
        }
        
        Path filePath = Paths.get(documentPath);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        
        byte[] fileContent = Files.readAllBytes(filePath);
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Content-Disposition", "inline; filename=\"" + filePath.getFileName().toString() + "\"")
                .body(fileContent);
    }
}




