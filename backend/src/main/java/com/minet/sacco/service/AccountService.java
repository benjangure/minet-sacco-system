package com.minet.sacco.service;

import com.minet.sacco.dto.DepositRequest;
import com.minet.sacco.dto.MemberContributionsReportDTO;
import com.minet.sacco.dto.ShareTransferRequest;
import com.minet.sacco.dto.WithdrawalRequest;
import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.Transaction;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.AccountRepository;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MemberSuspensionService memberSuspensionService;
    
    @Autowired
    private RealtimeNotificationService realtimeNotificationService;

    @Autowired
    private AuditService auditService;

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    public List<Account> getAccountsByMemberId(Long memberId) {
        return accountRepository.findByMemberId(memberId);
    }

    @Transactional
    public Account createAccount(Long memberId, Account.AccountType accountType) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        Account account = new Account();
        account.setMember(member);
        account.setAccountType(accountType);
        account.setBalance(BigDecimal.ZERO);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    @Transactional
    public Transaction deposit(DepositRequest request, User createdBy) {
        Account.AccountType accountType = request.getAccountType() != null ?
                Account.AccountType.valueOf(request.getAccountType()) : Account.AccountType.SAVINGS;

        // Prevent deposits to SHARES account (Minet SACCO does not accept share deposits)
        if (accountType == Account.AccountType.SHARES) {
            throw new RuntimeException("Deposits to SHARES account are not allowed. This SACCO does not accept share contributions.");
        }

        Account account = accountRepository.findByMemberIdAndAccountType(request.getMemberId(), accountType)
                .orElseGet(() -> createAccount(request.getMemberId(), accountType));

        // Update balance
        account.setBalance(account.getBalance().add(request.getAmount()));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setCreatedBy(createdBy);
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Real-time notification
        realtimeNotificationService.notifyTransaction(
            account.getMember().getId(),
            savedTransaction.getId(),
            "DEPOSIT",
            request.getAmount().doubleValue(),
            account.getBalance().doubleValue(),
            accountType.name()
        );

        return savedTransaction;
    }

    @Transactional
    public Transaction withdraw(WithdrawalRequest request, User createdBy) {
        Account.AccountType accountType = request.getAccountType() != null ?
                Account.AccountType.valueOf(request.getAccountType()) : Account.AccountType.SAVINGS;

        // Prevent withdrawals from SHARES account (Kenyan SACCO regulation)
        if (accountType == Account.AccountType.SHARES) {
            throw new RuntimeException("Withdrawals from SHARES account are not allowed. Shares can only be refunded when exiting the SACCO.");
        }

        Account account = accountRepository.findByMemberIdAndAccountType(request.getMemberId(), accountType)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Check if member is suspended
        if (memberSuspensionService.isMemberSuspended(request.getMemberId())) {
            throw new RuntimeException("Member is suspended and cannot withdraw funds");
        }

        // Check sufficient balance
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Check frozen savings (for self-guarantee loans)
        BigDecimal frozenSavings = account.getFrozenSavings() != null ? account.getFrozenSavings() : BigDecimal.ZERO;
        BigDecimal availableBalance = account.getBalance().subtract(frozenSavings);
        
        if (availableBalance.compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient available balance. Total balance: KES " + account.getBalance() + 
                    ", Frozen (for loan guarantees): KES " + frozenSavings + 
                    ", Available: KES " + availableBalance);
        }

        // Update balance
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setCreatedBy(createdBy);

        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Real-time notification
        realtimeNotificationService.notifyTransaction(
            account.getMember().getId(),
            savedTransaction.getId(),
            "WITHDRAWAL",
            request.getAmount().doubleValue(),
            account.getBalance().doubleValue(),
            accountType.name()
        );

        return savedTransaction;
    }

    public BigDecimal getBalance(Long memberId, Account.AccountType accountType) {
        return accountRepository.findByMemberIdAndAccountType(memberId, accountType)
                .map(Account::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Transfer shares from one member to another.
     * Both members must exist and have SHARES accounts.
     * The source must have sufficient shares balance (shares are never frozen).
     * Two Transaction records are created and an audit log entry is written.
     */
    @Transactional
    public List<Transaction> transferShares(ShareTransferRequest request, User performedBy) {

        if (request.getFromMemberId().equals(request.getToMemberId())) {
            throw new RuntimeException("Source and destination member cannot be the same");
        }

        // ── Source account ──────────────────────────────────────────────────
        Account fromAccount = accountRepository
                .findByMemberIdAndAccountType(request.getFromMemberId(), Account.AccountType.SHARES)
                .orElseThrow(() -> new RuntimeException(
                        "Source member does not have a SHARES account. Member ID: " + request.getFromMemberId()));

        // ── Destination account (auto-create if absent) ──────────────────────
        Account toAccount = accountRepository
                .findByMemberIdAndAccountType(request.getToMemberId(), Account.AccountType.SHARES)
                .orElseGet(() -> createAccount(request.getToMemberId(), Account.AccountType.SHARES));

        // ── Validation ───────────────────────────────────────────────────────
        if (memberSuspensionService.isMemberSuspended(request.getFromMemberId())) {
            throw new RuntimeException("Source member is suspended and cannot transfer shares");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException(
                    "Insufficient shares balance. Available: KES " + fromAccount.getBalance()
                    + ", Requested: KES " + request.getAmount());
        }

        String note = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Share transfer";

        String fromMemberName = fromAccount.getMember().getFullName() != null
                ? fromAccount.getMember().getFullName()
                : fromAccount.getMember().getMemberNumber();
        String toMemberName = toAccount.getMember().getFullName() != null
                ? toAccount.getMember().getFullName()
                : toAccount.getMember().getMemberNumber();

        // ── Debit source ─────────────────────────────────────────────────────
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        fromAccount.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(fromAccount);

        Transaction debitTx = new Transaction();
        debitTx.setAccount(fromAccount);
        debitTx.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
        debitTx.setAmount(request.getAmount());
        debitTx.setDescription("Share transfer OUT to " + toMemberName + " — " + note);
        debitTx.setCreatedBy(performedBy);
        Transaction savedDebit = transactionRepository.save(debitTx);

        // ── Credit destination ────────────────────────────────────────────────
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        toAccount.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(toAccount);

        Transaction creditTx = new Transaction();
        creditTx.setAccount(toAccount);
        creditTx.setTransactionType(Transaction.TransactionType.DEPOSIT);
        creditTx.setAmount(request.getAmount());
        creditTx.setDescription("Share transfer IN from " + fromMemberName + " — " + note);
        creditTx.setCreatedBy(performedBy);
        Transaction savedCredit = transactionRepository.save(creditTx);

        // ── Audit log ─────────────────────────────────────────────────────────
        String auditDetails = String.format(
                "{\"fromMemberId\":%d,\"fromMember\":\"%s\",\"toMemberId\":%d,\"toMember\":\"%s\","
                + "\"amount\":%.2f,\"debitTxId\":%d,\"creditTxId\":%d,\"note\":\"%s\"}",
                request.getFromMemberId(), fromMemberName,
                request.getToMemberId(), toMemberName,
                request.getAmount(),
                savedDebit.getId(), savedCredit.getId(),
                note.replace("\"", "'"));

        auditService.logAction(
                performedBy,
                "SHARE_TRANSFER",
                "ACCOUNT",
                fromAccount.getId(),
                auditDetails,
                "Share transfer of KES " + request.getAmount()
                        + " from " + fromMemberName + " to " + toMemberName,
                "SUCCESS");

        // ── Real-time notifications ───────────────────────────────────────────
        realtimeNotificationService.notifyTransaction(
                fromAccount.getMember().getId(),
                savedDebit.getId(),
                "WITHDRAWAL",
                request.getAmount().doubleValue(),
                fromAccount.getBalance().doubleValue(),
                "SHARES");

        realtimeNotificationService.notifyTransaction(
                toAccount.getMember().getId(),
                savedCredit.getId(),
                "DEPOSIT",
                request.getAmount().doubleValue(),
                toAccount.getBalance().doubleValue(),
                "SHARES");

        return List.of(savedDebit, savedCredit);
    }

    /**
     * Builds a full contribution/transaction history report for a single member.
     * The identifier can be either a numeric DB id or a member number string.
     *
     * @param memberIdentifier  numeric DB id OR member number (e.g. "EMP001" / "13121")
     * @param startDate         optional lower bound (inclusive); null = no lower bound
     * @param endDate           optional upper bound (inclusive); null = no upper bound
     * @param accountTypeFilter optional account type string (e.g. "SHARES"); null = all types
     */
    public MemberContributionsReportDTO getMemberContributionsReport(
            String memberIdentifier,
            LocalDate startDate,
            LocalDate endDate,
            String accountTypeFilter) {

        // Resolve member — try numeric DB id first, then member number string
        Member member;
        boolean isNumeric = memberIdentifier != null && memberIdentifier.matches("\\d+");
        if (isNumeric) {
            Long id = Long.parseLong(memberIdentifier);
            member = memberRepository.findById(id)
                    .orElseGet(() -> memberRepository.findByMemberNumber(memberIdentifier)
                            .orElseThrow(() -> new RuntimeException("Member not found: " + memberIdentifier)));
        } else {
            member = memberRepository.findByMemberNumber(memberIdentifier)
                    .orElseThrow(() -> new RuntimeException("Member not found: " + memberIdentifier));
        }

        MemberContributionsReportDTO report = new MemberContributionsReportDTO();
        report.setMemberId(member.getId());
        report.setMemberNumber(member.getMemberNumber());
        report.setMemberName(member.getFullName() != null ? member.getFullName()
                : (member.getFirstName() != null ? member.getFirstName() : ""));
        report.setEmail(member.getEmail());
        report.setPhone(member.getPhone());
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setAccountTypeFilter(accountTypeFilter);
        report.setGeneratedAt(LocalDateTime.now());

        // Determine date window
        LocalDateTime from = startDate != null ? startDate.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime to   = endDate   != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        // Fetch all accounts for this member, optionally filtered by type
        List<Account> accounts = accountRepository.findByMemberId(member.getId());
        if (accountTypeFilter != null && !accountTypeFilter.isBlank()) {
            Account.AccountType filterType = Account.AccountType.valueOf(accountTypeFilter.toUpperCase());
            accounts = accounts.stream()
                    .filter(a -> a.getAccountType() == filterType)
                    .collect(Collectors.toList());
        }

        List<MemberContributionsReportDTO.ContributionEntry> allEntries = new ArrayList<>();
        List<MemberContributionsReportDTO.AccountSummary> accountSummaries = new ArrayList<>();

        BigDecimal totalDeposited = BigDecimal.ZERO;
        BigDecimal totalWithdrawn = BigDecimal.ZERO;

        for (Account account : accounts) {
            List<Transaction> txns = transactionRepository
                    .findByAccountIdAndTransactionDateBetween(account.getId(), from, to);

            MemberContributionsReportDTO.AccountSummary summary = new MemberContributionsReportDTO.AccountSummary();
            summary.setAccountType(account.getAccountType().name());
            summary.setCurrentBalance(account.getBalance());
            summary.setTransactionCount(txns.size());

            BigDecimal accDeposited = BigDecimal.ZERO;
            BigDecimal accWithdrawn = BigDecimal.ZERO;

            for (Transaction tx : txns) {
                MemberContributionsReportDTO.ContributionEntry entry = new MemberContributionsReportDTO.ContributionEntry();
                entry.setTransactionId(tx.getId());
                entry.setTransactionDate(tx.getTransactionDate());
                entry.setTransactionType(tx.getTransactionType().name());
                entry.setAccountType(account.getAccountType().name());
                entry.setAmount(tx.getAmount());
                entry.setDescription(tx.getDescription());
                entry.setProcessedBy(tx.getCreatedBy() != null ? tx.getCreatedBy().getUsername() : "system");
                allEntries.add(entry);

                // Treat DEPOSIT / LOAN_DISBURSEMENT as money-in; everything else as money-out
                if (tx.getTransactionType() == Transaction.TransactionType.DEPOSIT
                        || tx.getTransactionType() == Transaction.TransactionType.LOAN_DISBURSEMENT) {
                    accDeposited = accDeposited.add(tx.getAmount());
                } else {
                    accWithdrawn = accWithdrawn.add(tx.getAmount());
                }
            }

            summary.setTotalDeposited(accDeposited);
            summary.setTotalWithdrawn(accWithdrawn);
            accountSummaries.add(summary);

            totalDeposited = totalDeposited.add(accDeposited);
            totalWithdrawn = totalWithdrawn.add(accWithdrawn);
        }

        // Sort all entries by date descending
        allEntries.sort(Comparator.comparing(
                MemberContributionsReportDTO.ContributionEntry::getTransactionDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        report.setAccountSummaries(accountSummaries);
        report.setEntries(allEntries);
        report.setTotalDeposited(totalDeposited);
        report.setTotalWithdrawn(totalWithdrawn);
        report.setNetContribution(totalDeposited.subtract(totalWithdrawn));

        return report;
    }
}
