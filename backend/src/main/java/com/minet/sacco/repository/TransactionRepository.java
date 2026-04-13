package com.minet.sacco.repository;

import com.minet.sacco.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByTransactionType(Transaction.TransactionType transactionType);

    List<Transaction> findByAccountIdAndTransactionDateBetween(Long accountId, LocalDateTime startDate, LocalDateTime endDate);

    List<Transaction> findByCreatedById(Long createdById);

    @Query("SELECT t FROM Transaction t WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Transaction> findByDescriptionKeywordInPeriod(@Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate,
                                                        @Param("keyword") String keyword);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    BigDecimal sumByDescriptionKeywordInPeriod(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("keyword") String keyword);

    // Operating Expenses Queries
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND (LOWER(t.description) LIKE LOWER(CONCAT('%', :category, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%salary%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%rent%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%utilities%')))")
    BigDecimal calculateTotalOperatingExpenses(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("category") String category);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%salary%'))")
    BigDecimal calculateSalaryExpenses(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%rent%'))")
    BigDecimal calculateRentExpenses(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%utilit%'))")
    BigDecimal calculateUtilitiesExpenses(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) NOT LIKE LOWER(CONCAT('%salary%')) " +
           "AND LOWER(t.description) NOT LIKE LOWER(CONCAT('%rent%')) " +
           "AND LOWER(t.description) NOT LIKE LOWER(CONCAT('%utilit%'))")
    BigDecimal calculateOtherExpenses(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal calculateTotalExpensesByDateRange(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    // Fees and Charges Queries
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%loan%')) " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%fee%'))")
    BigDecimal calculateLoanProcessingFees(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND (LOWER(t.description) LIKE LOWER(CONCAT('%maintenance%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%account%'))) " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%fee%'))")
    BigDecimal calculateAccountMaintenanceFees(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%fee%')) " +
           "AND NOT (LOWER(t.description) LIKE LOWER(CONCAT('%loan%')) " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%fee%'))) " +
           "AND NOT ((LOWER(t.description) LIKE LOWER(CONCAT('%maintenance%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%account%'))) " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%fee%')))")
    BigDecimal calculateOtherFees(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%fee%'))")
    BigDecimal calculateTotalFeesAndCharges(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);


    // Other Income Queries
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%interest%'))")
    BigDecimal calculateInterestIncomeFromSavings(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%income%')) " +
           "AND NOT LOWER(t.description) LIKE LOWER(CONCAT('%interest%'))")
    BigDecimal calculateOtherIncome(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND (LOWER(t.description) LIKE LOWER(CONCAT('%income%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%interest%')))")
    BigDecimal calculateTotalOtherIncomeSources(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    List<Transaction> findByAccountMemberIdOrderByTransactionDateDesc(Long memberId);

    @Query("SELECT t FROM Transaction t WHERE t.account.member.id = :memberId " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByMemberIdAndDateRange(@Param("memberId") Long memberId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.account.member.id = :memberId " +
           "AND t.transactionType = :transactionType " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByMemberIdAndTypeAndDateRange(@Param("memberId") Long memberId,
                                                         @Param("transactionType") Transaction.TransactionType transactionType,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
}
