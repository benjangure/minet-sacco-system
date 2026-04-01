package com.minet.sacco.dto;

import com.minet.sacco.entity.KycDocument;
import java.time.LocalDateTime;

public class KycDocumentDTO {

    private Long id;
    private Long memberId;
    private String memberNumber;
    private String memberName;
    private KycDocument.DocumentType documentType;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime uploadDate;
    private Long uploadedById;
    private String uploadedByName;
    private KycDocument.VerificationStatus verificationStatus;
    private Long verifiedById;
    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public KycDocumentDTO() {}

    public KycDocumentDTO(KycDocument document) {
        this.id = document.getId();
        this.memberId = document.getMember().getId();
        this.memberNumber = document.getMember().getMemberNumber();
        this.memberName = document.getMember().getFirstName() + " " + document.getMember().getLastName();
        this.documentType = document.getDocumentType();
        this.fileName = document.getFileName();
        this.filePath = document.getFilePath();
        this.fileSize = document.getFileSize();
        this.mimeType = document.getMimeType();
        this.uploadDate = document.getUploadDate();
        this.uploadedById = document.getUploadedBy().getId();
        this.uploadedByName = document.getUploadedBy().getUsername();
        this.verificationStatus = document.getVerificationStatus();
        if (document.getVerifiedBy() != null) {
            this.verifiedById = document.getVerifiedBy().getId();
            this.verifiedByName = document.getVerifiedBy().getUsername();
        }
        this.verifiedAt = document.getVerifiedAt();
        this.rejectionReason = document.getRejectionReason();
        this.createdAt = document.getCreatedAt();
        this.updatedAt = document.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public KycDocument.DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(KycDocument.DocumentType documentType) { this.documentType = documentType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public Long getUploadedById() { return uploadedById; }
    public void setUploadedById(Long uploadedById) { this.uploadedById = uploadedById; }

    public String getUploadedByName() { return uploadedByName; }
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }

    public KycDocument.VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(KycDocument.VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public Long getVerifiedById() { return verifiedById; }
    public void setVerifiedById(Long verifiedById) { this.verifiedById = verifiedById; }

    public String getVerifiedByName() { return verifiedByName; }
    public void setVerifiedByName(String verifiedByName) { this.verifiedByName = verifiedByName; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
