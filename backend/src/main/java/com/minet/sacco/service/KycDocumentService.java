package com.minet.sacco.service;

import com.minet.sacco.dto.KycDocumentDTO;
import com.minet.sacco.dto.MemberKycStatusDTO;
import com.minet.sacco.entity.*;
import com.minet.sacco.repository.KycDocumentAuditRepository;
import com.minet.sacco.repository.KycDocumentRepository;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class KycDocumentService {

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Autowired
    private KycDocumentAuditRepository auditRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Value("${kyc.upload.directory:uploads/kyc}")
    private String uploadDirectory;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    /**
     * Upload a KYC document for a member
     */
    public KycDocumentDTO uploadDocument(Long memberId, KycDocument.DocumentType documentType, 
                                         MultipartFile file, Long uploadedByUserId) throws IOException {
        // Validate member exists
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        // Validate file
        validateFile(file);

        // Check if document already exists for this member and type
        Optional<KycDocument> existingDoc = kycDocumentRepository.findByMemberIdAndDocumentType(memberId, documentType);
        if (existingDoc.isPresent()) {
            // Delete old file
            deleteFile(existingDoc.get().getFilePath());
            // Update existing document
            return updateDocument(existingDoc.get(), file, uploadedByUserId);
        }

        // Get user who is uploading
        User uploadedBy = userRepository.findById(uploadedByUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + uploadedByUserId));

        // Save file
        String filePath = saveFile(file, memberId, documentType);

        // Create document entity
        KycDocument document = new KycDocument();
        document.setMember(member);
        document.setDocumentType(documentType);
        document.setFileName(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setUploadedBy(uploadedBy);
        document.setVerificationStatus(KycDocument.VerificationStatus.PENDING);

        KycDocument savedDocument = kycDocumentRepository.save(document);

        // Create audit log
        createAuditLog(savedDocument, member, "UPLOAD", uploadedBy, null, "PENDING", "Document uploaded");

        // Update member KYC completion status
        updateMemberKycStatus(member);

        return new KycDocumentDTO(savedDocument);
    }

    /**
     * Update an existing KYC document
     */
    private KycDocumentDTO updateDocument(KycDocument document, MultipartFile file, Long uploadedByUserId) throws IOException {
        User uploadedBy = userRepository.findById(uploadedByUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + uploadedByUserId));

        String oldFilePath = document.getFilePath();
        String newFilePath = saveFile(file, document.getMember().getId(), document.getDocumentType());

        document.setFileName(file.getOriginalFilename());
        document.setFilePath(newFilePath);
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setUploadedBy(uploadedBy);
        document.setVerificationStatus(KycDocument.VerificationStatus.PENDING);
        document.setVerifiedBy(null);
        document.setVerifiedAt(null);
        document.setRejectionReason(null);

        KycDocument updated = kycDocumentRepository.save(document);

        // Create audit log
        createAuditLog(updated, document.getMember(), "UPDATE", uploadedBy, "VERIFIED", "PENDING", "Document updated");

        // Delete old file
        deleteFile(oldFilePath);

        return new KycDocumentDTO(updated);
    }

    /**
     * Verify a KYC document
     */
    public KycDocumentDTO verifyDocument(Long documentId, Long verifiedByUserId) {
        KycDocument document = kycDocumentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        User verifiedBy = userRepository.findById(verifiedByUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + verifiedByUserId));

        String oldStatus = document.getVerificationStatus().toString();
        document.setVerificationStatus(KycDocument.VerificationStatus.VERIFIED);
        document.setVerifiedBy(verifiedBy);
        document.setVerifiedAt(LocalDateTime.now());
        document.setRejectionReason(null);

        KycDocument updated = kycDocumentRepository.save(document);

        // Create audit log
        createAuditLog(updated, document.getMember(), "VERIFY", verifiedBy, oldStatus, "VERIFIED", "Document verified");
        
        // Also log to main audit table
        try {
            String docDetails = "KYC Document - Member: " + document.getMember().getFirstName() + " " + 
                               document.getMember().getLastName() + " - Type: " + document.getDocumentType();
            auditService.logAction(verifiedBy, "VERIFY", "KYC_DOCUMENT", updated.getId(), docDetails, "Document verified", "SUCCESS");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to log audit for KYC verification: " + e.getMessage());
        }

        // Update member KYC status
        updateMemberKycStatus(document.getMember());

        return new KycDocumentDTO(updated);
    }

    /**
     * Reject a KYC document
     */
    public KycDocumentDTO rejectDocument(Long documentId, String rejectionReason, Long rejectedByUserId) {
        KycDocument document = kycDocumentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        User rejectedBy = userRepository.findById(rejectedByUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + rejectedByUserId));

        String oldStatus = document.getVerificationStatus().toString();
        document.setVerificationStatus(KycDocument.VerificationStatus.REJECTED);
        document.setVerifiedBy(rejectedBy);
        document.setVerifiedAt(LocalDateTime.now());
        document.setRejectionReason(rejectionReason);

        KycDocument updated = kycDocumentRepository.save(document);

        // Create audit log
        createAuditLog(updated, document.getMember(), "REJECT", rejectedBy, oldStatus, "REJECTED", "Rejection reason: " + rejectionReason);
        
        // Also log to main audit table
        try {
            String docDetails = "KYC Document - Member: " + document.getMember().getFirstName() + " " + 
                               document.getMember().getLastName() + " - Type: " + document.getDocumentType();
            auditService.logAction(rejectedBy, "REJECT", "KYC_DOCUMENT", updated.getId(), docDetails, rejectionReason, "SUCCESS");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to log audit for KYC rejection: " + e.getMessage());
        }

        // Update member KYC status
        updateMemberKycStatus(document.getMember());

        return new KycDocumentDTO(updated);
    }

    /**
     * Get all documents for a member
     */
    public MemberKycStatusDTO getMemberKycStatus(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        List<KycDocument> documents = kycDocumentRepository.findByMemberId(memberId);
        List<KycDocumentDTO> documentDTOs = documents.stream()
            .map(KycDocumentDTO::new)
            .collect(Collectors.toList());

        MemberKycStatusDTO status = new MemberKycStatusDTO(
            member.getId(),
            member.getMemberNumber(),
            member.getFirstName() + " " + member.getLastName(),
            member.getEmail(),
            member.getPhone()
        );

        status.setDocuments(documentDTOs);
        status.setKycCompletionStatus(member.getKycCompletionStatus() != null ? member.getKycCompletionStatus().toString() : "INCOMPLETE");
        status.setKycCompletedAt(member.getKycCompletedAt());
        status.setKycVerifiedAt(member.getKycVerifiedAt());

        // Calculate statistics
        status.setDocumentsUploaded(documents.size());
        long verifiedCount = documents.stream()
            .filter(d -> d.getVerificationStatus() == KycDocument.VerificationStatus.VERIFIED)
            .count();
        status.setDocumentsVerified((int) verifiedCount);
        status.setAllDocumentsComplete(documents.size() == 5);
        status.setAllDocumentsVerified(verifiedCount == 5);

        return status;
    }

    /**
     * Get all pending documents for review
     */
    public List<KycDocumentDTO> getPendingDocuments() {
        List<KycDocument> documents = kycDocumentRepository.findAllPendingDocuments();
        return documents.stream()
            .map(KycDocumentDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Get members with incomplete KYC (fewer than 5 documents uploaded)
     */
    public List<MemberKycStatusDTO> getMembersWithIncompleteKyc() {
        List<Member> members = memberRepository.findAll();
        return members.stream()
            .map(member -> getMemberKycStatus(member.getId()))
            .filter(status -> status.getDocumentsUploaded() < 5) // Only show if fewer than 5 documents
            .collect(Collectors.toList());
    }

    /**
     * Get all documents uploaded by a specific user (for CUSTOMER_SUPPORT to track their uploads)
     */
    public List<KycDocumentDTO> getDocumentsUploadedByUser(Long userId) {
        List<KycDocument> documents = kycDocumentRepository.findByUploadedById(userId);
        return documents.stream()
            .map(KycDocumentDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Bulk approve members (Teller role)
     */
    public List<Long> bulkApproveMembersForActivation(List<Long> memberIds, Long approvedByUserId) {
        User approvedBy = userRepository.findById(approvedByUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + approvedByUserId));

        List<Long> approvedMembers = new ArrayList<>();

        for (Long memberId : memberIds) {
            Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

            // Check if all KYC documents are verified
            List<KycDocument> documents = kycDocumentRepository.findByMemberId(memberId);
            boolean allVerified = documents.size() == 5 && 
                documents.stream().allMatch(d -> d.getVerificationStatus() == KycDocument.VerificationStatus.VERIFIED);

            if (allVerified) {
                member.setStatus(Member.Status.ACTIVE);
                member.setApprovedBy(approvedBy.getId());
                member.setApprovedAt(LocalDateTime.now());
                memberRepository.save(member);
                approvedMembers.add(memberId);

                // Create audit log
                createMemberApprovalAudit(member, approvedBy, "APPROVED", "Member approved for activation");
            }
        }

        return approvedMembers;
    }

    /**
     * Download a KYC document
     */
    public byte[] downloadDocument(Long documentId) throws IOException {
        KycDocument document = kycDocumentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        Path filePath = Paths.get(document.getFilePath());
        return Files.readAllBytes(filePath);
    }

    /**
     * Delete a KYC document
     */
    public void deleteDocument(Long documentId, Long deletedByUserId) throws IOException {
        KycDocument document = kycDocumentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        User deletedBy = userRepository.findById(deletedByUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + deletedByUserId));

        // Delete file
        deleteFile(document.getFilePath());

        // Create audit log
        createAuditLog(document, document.getMember(), "DELETE", deletedBy, document.getVerificationStatus().toString(), null, "Document deleted");

        // Delete document
        kycDocumentRepository.delete(document);

        // Update member KYC status
        updateMemberKycStatus(document.getMember());
    }

    // Helper methods

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }

        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: PDF, JPEG, PNG, DOC, DOCX");
        }
    }

    private String saveFile(MultipartFile file, Long memberId, KycDocument.DocumentType documentType) throws IOException {
        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDirectory, "member_" + memberId);
        Files.createDirectories(uploadPath);

        // Generate unique filename
        String filename = documentType.name() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);

        // Save file
        Files.copy(file.getInputStream(), filePath);

        return filePath.toString();
    }

    private void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            // Log error but don't fail the operation
            System.err.println("Failed to delete file: " + filePath + ", Error: " + e.getMessage());
        }
    }

    private void createAuditLog(KycDocument document, Member member, String action, User performedBy, 
                               String oldStatus, String newStatus, String details) {
        KycDocumentAudit audit = new KycDocumentAudit();
        audit.setKycDocument(document);
        audit.setMember(member);
        audit.setAction(action);
        audit.setPerformedBy(performedBy);
        audit.setOldStatus(oldStatus);
        audit.setNewStatus(newStatus);
        audit.setDetails(details);
        auditRepository.save(audit);
    }

    private void createMemberApprovalAudit(Member member, User approvedBy, String action, String details) {
        KycDocumentAudit audit = new KycDocumentAudit();
        audit.setMember(member);
        audit.setAction(action);
        audit.setPerformedBy(approvedBy);
        audit.setOldStatus(member.getStatus().toString());
        audit.setNewStatus("ACTIVE");
        audit.setDetails(details);
        auditRepository.save(audit);
    }

    private void updateMemberKycStatus(Member member) {
        List<KycDocument> documents = kycDocumentRepository.findByMemberId(member.getId());

        if (documents.isEmpty()) {
            member.setKycCompletionStatus(Member.KycCompletionStatus.INCOMPLETE);
            member.setKycCompletedAt(null);
            member.setKycVerifiedAt(null);
        } else {
            long uploadedCount = documents.size();
            long verifiedCount = documents.stream()
                .filter(d -> d.getVerificationStatus() == KycDocument.VerificationStatus.VERIFIED)
                .count();

            if (uploadedCount == 5) {
                member.setKycCompletionStatus(Member.KycCompletionStatus.COMPLETE);
                member.setKycCompletedAt(LocalDateTime.now());
            }

            if (verifiedCount == 5) {
                member.setKycCompletionStatus(Member.KycCompletionStatus.VERIFIED);
                member.setKycVerifiedAt(LocalDateTime.now());
            }
        }

        memberRepository.save(member);
    }
}
