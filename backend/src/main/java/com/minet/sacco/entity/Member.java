package com.minet.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 20)
    @Column(unique = true)
    private String memberNumber;

    @Size(max = 50)
    @Column(name = "employee_id", unique = true)
    private String employeeId;

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 15)
    private String phone;

    @NotBlank(message = "National ID is required")
    @Size(max = 20)
    @Column(unique = true)
    private String nationalId;

    private LocalDate dateOfBirth;

    @Size(max = 50)
    private String employmentStatus;

    @Size(max = 100)
    private String employer;

    @Size(max = 50)
    private String department;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    // KYC Completion Tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_completion_status")
    private KycCompletionStatus kycCompletionStatus = KycCompletionStatus.INCOMPLETE;

    @Column(name = "kyc_completed_at")
    private LocalDateTime kycCompletedAt;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    // KYC Documents (stored as file paths or URLs)
    @Size(max = 255)
    private String idDocumentPath;

    @Size(max = 255)
    private String photoPath;

    @Size(max = 255)
    private String applicationLetterPath;

    @Size(max = 255)
    private String kraPinPath;

    // Bank Details
    @Size(max = 100)
    private String bankName;

    @Size(max = 50)
    private String bankAccountNumber;

    @Size(max = 50)
    private String bankBranch;

    // Next of Kin
    @Size(max = 100)
    private String nextOfKinName;

    @Size(max = 15)
    private String nextOfKinPhone;

    @Size(max = 50)
    private String nextOfKinRelationship;

    // Workflow tracking
    private Long createdBy; // User ID who created the application
    private Long approvedBy; // User ID who approved
    private LocalDateTime approvedAt;
    private String rejectionReason;

    // Exit tracking
    private LocalDateTime exitDate;
    private String exitReason; // RESIGNED, RETIRED, TERMINATED, DECEASED, OTHER

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, APPROVED, ACTIVE, DORMANT, SUSPENDED, REJECTED, EXITED
    }

    public enum KycCompletionStatus {
        INCOMPLETE("Incomplete"),
        COMPLETE("Complete"),
        VERIFIED("Verified");

        private final String displayName;

        KycCompletionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }

    public String getEmployer() { return employer; }
    public void setEmployer(String employer) { this.employer = employer; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public KycCompletionStatus getKycCompletionStatus() { return kycCompletionStatus; }
    public void setKycCompletionStatus(KycCompletionStatus kycCompletionStatus) { this.kycCompletionStatus = kycCompletionStatus; }

    public LocalDateTime getKycCompletedAt() { return kycCompletedAt; }
    public void setKycCompletedAt(LocalDateTime kycCompletedAt) { this.kycCompletedAt = kycCompletedAt; }

    public LocalDateTime getKycVerifiedAt() { return kycVerifiedAt; }
    public void setKycVerifiedAt(LocalDateTime kycVerifiedAt) { this.kycVerifiedAt = kycVerifiedAt; }

    public String getIdDocumentPath() { return idDocumentPath; }
    public void setIdDocumentPath(String idDocumentPath) { this.idDocumentPath = idDocumentPath; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getApplicationLetterPath() { return applicationLetterPath; }
    public void setApplicationLetterPath(String applicationLetterPath) { this.applicationLetterPath = applicationLetterPath; }

    public String getKraPinPath() { return kraPinPath; }
    public void setKraPinPath(String kraPinPath) { this.kraPinPath = kraPinPath; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }

    public String getNextOfKinName() { return nextOfKinName; }
    public void setNextOfKinName(String nextOfKinName) { this.nextOfKinName = nextOfKinName; }

    public String getNextOfKinPhone() { return nextOfKinPhone; }
    public void setNextOfKinPhone(String nextOfKinPhone) { this.nextOfKinPhone = nextOfKinPhone; }

    public String getNextOfKinRelationship() { return nextOfKinRelationship; }
    public void setNextOfKinRelationship(String nextOfKinRelationship) { this.nextOfKinRelationship = nextOfKinRelationship; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getExitDate() { return exitDate; }
    public void setExitDate(LocalDateTime exitDate) { this.exitDate = exitDate; }

    public String getExitReason() { return exitReason; }
    public void setExitReason(String exitReason) { this.exitReason = exitReason; }
}