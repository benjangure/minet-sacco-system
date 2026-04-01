package com.minet.sacco.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MemberKycStatusDTO {

    private Long memberId;
    private String memberNumber;
    private String memberName;
    private String email;
    private String phone;
    private String kycCompletionStatus;
    private LocalDateTime kycCompletedAt;
    private LocalDateTime kycVerifiedAt;
    private List<KycDocumentDTO> documents;
    private int totalDocumentsRequired;
    private int documentsUploaded;
    private int documentsVerified;
    private boolean allDocumentsComplete;
    private boolean allDocumentsVerified;

    // Constructors
    public MemberKycStatusDTO() {}

    public MemberKycStatusDTO(Long memberId, String memberNumber, String memberName, String email, String phone) {
        this.memberId = memberId;
        this.memberNumber = memberNumber;
        this.memberName = memberName;
        this.email = email;
        this.phone = phone;
        this.totalDocumentsRequired = 5; // NATIONAL_ID, PASSPORT, PASSPORT_PHOTO, APPLICATION_LETTER, KRA_PIN_CERTIFICATE
    }

    // Getters and Setters
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getKycCompletionStatus() { return kycCompletionStatus; }
    public void setKycCompletionStatus(String kycCompletionStatus) { this.kycCompletionStatus = kycCompletionStatus; }

    public LocalDateTime getKycCompletedAt() { return kycCompletedAt; }
    public void setKycCompletedAt(LocalDateTime kycCompletedAt) { this.kycCompletedAt = kycCompletedAt; }

    public LocalDateTime getKycVerifiedAt() { return kycVerifiedAt; }
    public void setKycVerifiedAt(LocalDateTime kycVerifiedAt) { this.kycVerifiedAt = kycVerifiedAt; }

    public List<KycDocumentDTO> getDocuments() { return documents; }
    public void setDocuments(List<KycDocumentDTO> documents) { this.documents = documents; }

    public int getTotalDocumentsRequired() { return totalDocumentsRequired; }
    public void setTotalDocumentsRequired(int totalDocumentsRequired) { this.totalDocumentsRequired = totalDocumentsRequired; }

    public int getDocumentsUploaded() { return documentsUploaded; }
    public void setDocumentsUploaded(int documentsUploaded) { this.documentsUploaded = documentsUploaded; }

    public int getDocumentsVerified() { return documentsVerified; }
    public void setDocumentsVerified(int documentsVerified) { this.documentsVerified = documentsVerified; }

    public boolean isAllDocumentsComplete() { return allDocumentsComplete; }
    public void setAllDocumentsComplete(boolean allDocumentsComplete) { this.allDocumentsComplete = allDocumentsComplete; }

    public boolean isAllDocumentsVerified() { return allDocumentsVerified; }
    public void setAllDocumentsVerified(boolean allDocumentsVerified) { this.allDocumentsVerified = allDocumentsVerified; }
}
