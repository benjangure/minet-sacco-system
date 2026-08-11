package com.minet.sacco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO for marking a member as exited from the SACCO
 */
public class MemberExitRequest {

    @NotBlank(message = "Exit reason is required")
    private String exitReason; // RESIGNED, RETIRED, TERMINATED, DECEASED, OTHER

    @NotNull(message = "Exit date is required")
    private LocalDateTime exitDate;

    private String exitNotes; // Additional notes about the exit

    // Constructors
    public MemberExitRequest() {}

    public MemberExitRequest(String exitReason, LocalDateTime exitDate, String exitNotes) {
        this.exitReason = exitReason;
        this.exitDate = exitDate;
        this.exitNotes = exitNotes;
    }

    // Getters and Setters
    public String getExitReason() {
        return exitReason;
    }

    public void setExitReason(String exitReason) {
        this.exitReason = exitReason;
    }

    public LocalDateTime getExitDate() {
        return exitDate;
    }

    public void setExitDate(LocalDateTime exitDate) {
        this.exitDate = exitDate;
    }

    public String getExitNotes() {
        return exitNotes;
    }

    public void setExitNotes(String exitNotes) {
        this.exitNotes = exitNotes;
    }
}
