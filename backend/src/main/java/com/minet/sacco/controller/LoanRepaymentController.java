package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.LoanRepayment;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.LoanRepaymentService;
import com.minet.sacco.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin
public class LoanRepaymentController {

    @Autowired
    private LoanRepaymentService loanRepaymentService;

    @Autowired
    private UserService userService;

    @PostMapping("/{loanId}/repay")
    @PreAuthorize("hasAnyRole('ROLE_TELLER', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<LoanRepayment>> recordRepayment(
            @PathVariable Long loanId,
            @Valid @RequestBody RecordRepaymentRequest request,
            Authentication authentication) {
        
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LoanRepayment.PaymentMethod paymentMethod;
        try {
            paymentMethod = LoanRepayment.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid payment method: " + request.getPaymentMethod()));
        }

        LoanRepayment repayment = loanRepaymentService.recordRepayment(
                loanId,
                request.getAmount(),
                paymentMethod,
                request.getReferenceNumber(),
                request.getPaymentDate(),
                user
        );

        return ResponseEntity.ok(ApiResponse.success("Loan repayment recorded successfully", repayment));
    }

    @GetMapping("/{loanId}/repayments")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR', 'ROLE_TELLER')")
    public ResponseEntity<ApiResponse<List<LoanRepayment>>> getRepaymentHistory(@PathVariable Long loanId) {
        List<LoanRepayment> repayments = loanRepaymentService.getRepaymentHistory(loanId);
        return ResponseEntity.ok(ApiResponse.success("Repayment history retrieved", repayments));
    }

    @GetMapping("/{loanId}/schedule")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR', 'ROLE_TELLER')")
    public ResponseEntity<ApiResponse<LoanRepaymentService.LoanAmortizationSchedule>> getAmortizationSchedule(@PathVariable Long loanId) {
        LoanRepaymentService.LoanAmortizationSchedule schedule = loanRepaymentService.calculateAmortizationSchedule(loanId);
        return ResponseEntity.ok(ApiResponse.success("Amortization schedule retrieved", schedule));
    }

    @GetMapping("/{loanId}/repayments/date-range")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<LoanRepayment>>> getRepaymentsByDateRange(
            @PathVariable Long loanId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
        
        List<LoanRepayment> repayments = loanRepaymentService.getRepaymentsByDateRange(loanId, start, end);
        return ResponseEntity.ok(ApiResponse.success("Repayments for date range retrieved", repayments));
    }

    // DTO for recording repayment
    public static class RecordRepaymentRequest {
        private BigDecimal amount;
        private String paymentMethod;
        private String referenceNumber;
        private LocalDateTime paymentDate;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

        public LocalDateTime getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    }
}
