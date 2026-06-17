package com.minet.sacco.dto;

public class MemberCreationResponseDTO {
    private Long memberId;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String username;
    private String password; // Temporary password (if generated)
    private boolean hasNationalId;
    private String passwordType; // "NATIONAL_ID" or "GENERATED"
    private String message;

    public MemberCreationResponseDTO(Long memberId, String memberNumber, String firstName, String lastName,
                                     String username, String password, boolean hasNationalId, String passwordType, String message) {
        this.memberId = memberId;
        this.memberNumber = memberNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.hasNationalId = hasNationalId;
        this.passwordType = passwordType;
        this.message = message;
    }

    // Getters and Setters
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isHasNationalId() { return hasNationalId; }
    public void setHasNationalId(boolean hasNationalId) { this.hasNationalId = hasNationalId; }

    public String getPasswordType() { return passwordType; }
    public void setPasswordType(String passwordType) { this.passwordType = passwordType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
