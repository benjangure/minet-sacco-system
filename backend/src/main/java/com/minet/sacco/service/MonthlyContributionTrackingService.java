package com.minet.sacco.service;

import com.minet.sacco.dto.MonthlyContributionTrackingDTO;
import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MonthlyContributionTrackingService {

    @Autowired
    private BulkBatchRepository bulkBatchRepository;

    @Autowired
    private BulkTransactionItemRepository bulkTransactionItemRepository;

    @Autowired
    private MemberRepository memberRepository;

    /**
     * Generate Monthly Contribution Tracking Report for a date range
     */
    public MonthlyContributionTrackingDTO generateMonthlyContributionTrackingReport(
            LocalDate startDate, LocalDate endDate, String batchStatus) {

        // Fetch all batches in the date range
        List<BulkBatch> allBatches = bulkBatchRepository.findAll();
        List<BulkBatch> batchesInRange = allBatches.stream()
                .filter(b -> b.getUploadedAt() != null && 
                        !b.getUploadedAt().toLocalDate().isBefore(startDate) && 
                        !b.getUploadedAt().toLocalDate().isAfter(endDate))
                .collect(Collectors.toList());

        // Apply status filter if provided
        if (batchStatus != null && !batchStatus.isEmpty()) {
            batchesInRange = batchesInRange.stream()
                    .filter(b -> b.getStatus().equalsIgnoreCase(batchStatus))
                    .collect(Collectors.toList());
        }

        // Build batch summaries
        List<MonthlyContributionTrackingDTO.BatchSummary> batchSummaries = batchesInRange.stream()
                .map(this::buildBatchSummary)
                .collect(Collectors.toList());

        // Calculate aggregated summary
        MonthlyContributionTrackingDTO.AggregatedSummary aggregatedSummary = calculateAggregatedSummary(batchSummaries);

        return new MonthlyContributionTrackingDTO(batchSummaries, aggregatedSummary);
    }

    /**
     * Build summary for a single batch
     */
    private MonthlyContributionTrackingDTO.BatchSummary buildBatchSummary(BulkBatch batch) {
        MonthlyContributionTrackingDTO.BatchSummary summary = new MonthlyContributionTrackingDTO.BatchSummary();
        summary.setBatchId(batch.getId());
        summary.setBatchDate(batch.getUploadedAt() != null ? batch.getUploadedAt().toLocalDate() : null);
        summary.setBatchStatus(batch.getStatus());
        summary.setUploadedBy(batch.getUploadedBy() != null ? batch.getUploadedBy().getUsername() : "Unknown");

        // Get batch items
        List<BulkTransactionItem> batchItems = bulkTransactionItemRepository.findByBatch_Id(batch.getId());

        // Member processing summary
        summary.setTotalMembersInFile(batchItems.size());
        summary.setSuccessfullyProcessed(batch.getSuccessfulRecords() != null ? batch.getSuccessfulRecords() : 0);
        summary.setMembersWithErrors(batch.getFailedRecords() != null ? batch.getFailedRecords() : 0);
        summary.setMissingMembers(Math.max(0, summary.getTotalMembersInFile() - summary.getSuccessfullyProcessed() - summary.getMembersWithErrors()));
        summary.setTotalMembersExpected(summary.getTotalMembersInFile());

        // Contribution summary - calculate from specific amount fields
        BigDecimal totalSavings = BigDecimal.ZERO;
        BigDecimal totalRepayments = BigDecimal.ZERO;

        for (BulkTransactionItem item : batchItems) {
            // Sum all savings-related amounts
            if (item.getSavingsAmount() != null) {
                totalSavings = totalSavings.add(item.getSavingsAmount());
            }
            if (item.getSharesAmount() != null) {
                totalSavings = totalSavings.add(item.getSharesAmount());
            }
            
            // Sum loan repayments
            if (item.getLoanRepaymentAmount() != null) {
                totalRepayments = totalRepayments.add(item.getLoanRepaymentAmount());
            }
        }

        summary.setTotalSavingsPosted(totalSavings);
        summary.setTotalLoanRepaymentsPosted(totalRepayments);
        summary.setTotalTransactionsProcessed(batchItems.size());

        // Eligibility progress
        MonthlyContributionTrackingDTO.EligibilityProgress eligibilityProgress = calculateEligibilityProgress(batch);
        summary.setEligibilityProgress(eligibilityProgress);

        return summary;
    }

    /**
     * Calculate eligibility progress for a batch
     * Progress is tracked by consecutive months counter on member entity
     */
    private MonthlyContributionTrackingDTO.EligibilityProgress calculateEligibilityProgress(BulkBatch batch) {
        MonthlyContributionTrackingDTO.EligibilityProgress progress = new MonthlyContributionTrackingDTO.EligibilityProgress();

        // Get all members and count by their consecutive months
        List<Member> allMembers = memberRepository.findAll();

        int atMonth1 = 0, atMonth2 = 0, atMonth3 = 0, atMonth4 = 0, atMonth5 = 0, atMonth6Plus = 0;

        for (Member member : allMembers) {
            Integer months = member.getConsecutiveMonthsCounter();
            if (months == null) months = 0;

            switch (months) {
                case 1: atMonth1++; break;
                case 2: atMonth2++; break;
                case 3: atMonth3++; break;
                case 4: atMonth4++; break;
                case 5: atMonth5++; break;
                default:
                    if (months >= 6) atMonth6Plus++;
                    break;
            }
        }

        progress.setAtMonth1(atMonth1);
        progress.setAtMonth2(atMonth2);
        progress.setAtMonth3(atMonth3);
        progress.setAtMonth4(atMonth4);
        progress.setAtMonth5(atMonth5);
        progress.setAtMonth6(atMonth6Plus);

        // Members who became eligible this batch (reached exactly 6 months)
        // This would require tracking membership start date per batch, for now we note those at month 6
        progress.setMembersBecameEligible(atMonth6Plus);

        return progress;
    }

    /**
     * Calculate aggregated summary across all batches in the report
     */
    private MonthlyContributionTrackingDTO.AggregatedSummary calculateAggregatedSummary(
            List<MonthlyContributionTrackingDTO.BatchSummary> batchSummaries) {

        MonthlyContributionTrackingDTO.AggregatedSummary summary = new MonthlyContributionTrackingDTO.AggregatedSummary();

        summary.setTotalBatches(batchSummaries.size());
        summary.setCompletedBatches((int) batchSummaries.stream()
                .filter(b -> "COMPLETED".equalsIgnoreCase(b.getBatchStatus()))
                .count());
        summary.setFailedBatches((int) batchSummaries.stream()
                .filter(b -> "FAILED".equalsIgnoreCase(b.getBatchStatus()))
                .count());

        BigDecimal totalSavings = batchSummaries.stream()
                .map(MonthlyContributionTrackingDTO.BatchSummary::getTotalSavingsPosted)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalSavingsAllBatches(totalSavings);

        BigDecimal totalRepayments = batchSummaries.stream()
                .map(MonthlyContributionTrackingDTO.BatchSummary::getTotalLoanRepaymentsPosted)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalRepaymentsAllBatches(totalRepayments);

        Integer totalMembersProcessed = batchSummaries.stream()
                .mapToInt(MonthlyContributionTrackingDTO.BatchSummary::getSuccessfullyProcessed)
                .sum();
        summary.setTotalMembersProcessed(totalMembersProcessed);

        // Count members at 6+ months as eligible
        List<Member> allMembers = memberRepository.findAll();
        Integer eligibleMembers = (int) allMembers.stream()
                .filter(m -> m.getConsecutiveMonthsCounter() != null && m.getConsecutiveMonthsCounter() >= 6)
                .count();
        summary.setTotalEligibleMembers(eligibleMembers);

        return summary;
    }
}
