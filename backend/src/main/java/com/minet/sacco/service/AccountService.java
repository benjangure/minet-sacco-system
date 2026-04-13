package com.minet.sacco.service;

import com.minet.sacco.dto.DepositRequest;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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

        return transactionRepository.save(transaction);
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

        return transactionRepository.save(transaction);
    }

    public BigDecimal getBalance(Long memberId, Account.AccountType accountType) {
        return accountRepository.findByMemberIdAndAccountType(memberId, accountType)
                .map(Account::getBalance)
                .orElse(BigDecimal.ZERO);
    }
}
