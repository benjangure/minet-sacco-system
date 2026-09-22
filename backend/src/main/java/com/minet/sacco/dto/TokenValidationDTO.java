package com.minet.sacco.dto;

import java.time.LocalDateTime;

public class TokenValidationDTO {
    private boolean valid;
    private String username;
    private String email;
    private String resetType;
    private LocalDateTime expiresAt;
    private String message;

    public TokenValidationDTO() {}

    public TokenValidationDTO(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public TokenValidationDTO(boolean valid, String username, String email, String resetType, LocalDateTime expiresAt, String message) {
        this.valid = valid;
        this.username = username;
        this.email = email;
        this.resetType = resetType;
        this.expiresAt = expiresAt;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getResetType() {
        return resetType;
    }

    public void setResetType(String resetType) {
        this.resetType = resetType;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
