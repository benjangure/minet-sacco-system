package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.DepositRequest;
import com.minet.sacco.dto.WithdrawalRequest;
import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.Transaction;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.AccountService;
import com.minet.sacco.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_TELLER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Account>>> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_TELLER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<Account>> getAccountById(@PathVariable Long id) {
        return accountService.getAccountById(id)
                .map(account -> ResponseEntity.ok(ApiResponse.success("Account found", account)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_TELLER', 'ROLE_LOAN_OFFICER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Account>>> getAccountsByMemberId(@PathVariable Long memberId) {
        List<Account> accounts = accountService.getAccountsByMemberId(memberId);
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_TELLER')")
    public ResponseEntity<ApiResponse<Transaction>> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Transaction transaction = accountService.deposit(request, user);
        return ResponseEntity.ok(ApiResponse.success("Deposit successful", transaction));
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_TELLER')")
    public ResponseEntity<ApiResponse<Transaction>> withdraw(
            @Valid @RequestBody WithdrawalRequest request,
            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Transaction transaction = accountService.withdraw(request, user);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful", transaction));
    }

    @GetMapping("/balance/{memberId}/{accountType}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_TELLER', 'ROLE_LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(
            @PathVariable Long memberId,
            @PathVariable String accountType) {
        BigDecimal balance = accountService.getBalance(memberId, Account.AccountType.valueOf(accountType));
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved successfully", balance));
    }
}




