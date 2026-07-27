package com.minet.sacco.service;

import com.minet.sacco.dto.WithdrawalMonitoringReportDTO;
import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.Transaction;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.AccountRepository;
import com.minet.sacco.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WithdrawalMonitoringReportService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    public WithdrawalMonitoringReportDTO generateWithdrawalMonitoringReport(
            LocalDate startDate, LocalDate endDate, String memberNumber, String withdrawalMethod, String status) {

        // Convert dates to LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Fetch all withdrawal transactions in the date range
        List<Transaction> allTransactions = transactionRepository.findAll();
        
        List<Transaction> withdrawalTransactions = allTransactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.WITHDRAWAL)
                .filter(t -> t.getTransactionDate() != null && 
                        !t.getTransactionDate().isBefore(startDateTime) && 
                        !t.getTransactionDate().isAfter(endDateTime))
                .collect(Collectors.toList());

        // Apply optional filters
        if (memberNumber != null && !memberNumber.isEmpty()) {
            withdrawalTransactions = withdrawalTransactions.stream()
                    .filter(t -> t.getAccount().getMember().getMemberNumber().equalsIgnoreCase(memberNumber))
                    .collect(Collectors.toList());
        }

        if (withdrawalMethod != null && !withdrawalMethod.isEmpty()) {
            withdrawalTransactions = withdrawalTransactions.stream()
                    .filter(t -> extractWithdrawalMethod(t.getDescription()).equalsIgnoreCase(withdrawalMethod))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isEmpty()) {
            withdrawalTransactions = withdrawalTransactions.stream()
                    .filter(t -> extractTransactionStatus(t.getDescription()).equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        // Build withdrawal transaction DTOs
        List<WithdrawalMonitoringReportDTO.WithdrawalTransaction> transactionDTOs = withdrawalTransactions.stream()
                .map(this::buildWithdrawalTransactionDTO)
                .collect(Collectors.toList());

        // Calculate summary totals
        WithdrawalMonitoringReportDTO.SummaryTotals summaryTotals = calculateSummaryTotals(withdrawalTransactions);

        return new WithdrawalMonitoringReportDTO(transactionDTOs, summaryTotals);
    }

    private WithdrawalMonitoringReportDTO.WithdrawalTransaction buildWithdrawalTransactionDTO(Transaction transaction) {
        Account account = transaction.getAccount();
        Member member = account.getMember();
        User createdBy = transaction.getCreatedBy();

        String memberName = member.getFirstName() + " " + member.getLastName();
        String accountType = account.getAccountType().toString();
        BigDecimal withdrawalAmount = transaction.getAmount();
        LocalDateTime transactionDate = transaction.getTransactionDate();
        String withdrawalMethod = extractWithdrawalMethod(transaction.getDescription());
        String processedBy = createdBy != null ? createdBy.getUsername() : "Unknown";
        String transactionStatus = extractTransactionStatus(transaction.getDescription());

        // Calculate account balance before and after
        BigDecimal accountBalanceAfter = account.getBalance();
        BigDecimal accountBalanceBefore = accountBalanceAfter.add(withdrawalAmount);
        BigDecimal remainingBalance = accountBalanceAfter;

        return new WithdrawalMonitoringReportDTO.WithdrawalTransaction(
                transaction.getId(),
                member.getMemberNumber(),
                memberName,
                accountType,
                withdrawalAmount,
                transactionDate,
                withdrawalMethod,
                processedBy,
                transactionStatus,
                accountBalanceBefore,
                accountBalanceAfter,
                remainingBalance
        );
    }

    private WithdrawalMonitoringReportDTO.SummaryTotals calculateSummaryTotals(List<Transaction> withdrawalTransactions) {
        long totalWithdrawals = withdrawalTransactions.size();
        BigDecimal totalAmountWithdrawn = withdrawalTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by withdrawal method
        Map<String, List<Transaction>> byMethod = withdrawalTransactions.stream()
                .collect(Collectors.groupingBy(t -> extractWithdrawalMethod(t.getDescription())));

        WithdrawalMonitoringReportDTO.MethodSummary mpesaSummary = calculateMethodSummary(byMethod.getOrDefault("M_PESA", new ArrayList<>()));
        WithdrawalMonitoringReportDTO.MethodSummary manualCashSummary = calculateMethodSummary(byMethod.getOrDefault("MANUAL_CASH", new ArrayList<>()));
        WithdrawalMonitoringReportDTO.MethodSummary bankTransferSummary = calculateMethodSummary(byMethod.getOrDefault("BANK_TRANSFER", new ArrayList<>()));

        return new WithdrawalMonitoringReportDTO.SummaryTotals(
                totalWithdrawals,
                totalAmountWithdrawn,
                mpesaSummary,
                manualCashSummary,
                bankTransferSummary
        );
    }

    private WithdrawalMonitoringReportDTO.MethodSummary calculateMethodSummary(List<Transaction> transactions) {
        long count = transactions.size();
        BigDecimal amount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WithdrawalMonitoringReportDTO.MethodSummary(count, amount);
    }

    private String extractWithdrawalMethod(String description) {
        if (description == null) return "MANUAL_CASH";
        
        String desc = description.toLowerCase();
        if (desc.contains("mpesa") || desc.contains("m-pesa")) {
            return "M_PESA";
        } else if (desc.contains("bank") || desc.contains("transfer")) {
            return "BANK_TRANSFER";
        }
        return "MANUAL_CASH";
    }

    private String extractTransactionStatus(String description) {
        if (description == null) return "COMPLETED";
        
        String desc = description.toLowerCase();
        if (desc.contains("pending")) {
            return "PENDING";
        } else if (desc.contains("failed")) {
            return "FAILED";
        }
        return "COMPLETED";
    }
}
