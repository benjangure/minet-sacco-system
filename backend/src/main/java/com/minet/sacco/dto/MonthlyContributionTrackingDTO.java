package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class MonthlyContributionTrackingDTO {

    private List<BatchSummary> batches;
    private AggregatedSummary aggregatedSummary;

    public MonthlyContributionTrackingDTO() {}

    public MonthlyContributionTrackingDTO(List<BatchSummary> batches, AggregatedSummary aggregatedSummary) {
        this.batches = batches;
        this.aggregatedSummary = aggregatedSummary;
    }

    public List<BatchSummary> getBatches() { return batches; }
    public void setBatches(List<BatchSummary> batches) { this.batches = batches; }

    public AggregatedSummary getAggregatedSummary() { return aggregatedSummary; }
    public void setAggregatedSummary(AggregatedSummary aggregatedSummary) { this.aggregatedSummary = aggregatedSummary; }

    // Batch Information
    public static class BatchSummary {
        private Long batchId;
        private LocalDate batchDate;
        private String batchStatus;  // PENDING, PROCESSING, COMPLETED, FAILED
        private String uploadedBy;
        private Integer totalMembersExpected;
        private Integer totalMembersInFile;
        private Integer missingMembers;
        private Integer successfullyProcessed;
        private Integer membersWithErrors;
        private BigDecimal totalSavingsPosted;
        private BigDecimal totalLoanRepaymentsPosted;
        private Integer totalTransactionsProcessed;
        private EligibilityProgress eligibilityProgress;

        public BatchSummary() {}

        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }

        public LocalDate getBatchDate() { return batchDate; }
        public void setBatchDate(LocalDate batchDate) { this.batchDate = batchDate; }

        public String getBatchStatus() { return batchStatus; }
        public void setBatchStatus(String batchStatus) { this.batchStatus = batchStatus; }

        public String getUploadedBy() { return uploadedBy; }
        public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

        public Integer getTotalMembersExpected() { return totalMembersExpected; }
        public void setTotalMembersExpected(Integer totalMembersExpected) { this.totalMembersExpected = totalMembersExpected; }

        public Integer getTotalMembersInFile() { return totalMembersInFile; }
        public void setTotalMembersInFile(Integer totalMembersInFile) { this.totalMembersInFile = totalMembersInFile; }

        public Integer getMissingMembers() { return missingMembers; }
        public void setMissingMembers(Integer missingMembers) { this.missingMembers = missingMembers; }

        public Integer getSuccessfullyProcessed() { return successfullyProcessed; }
        public void setSuccessfullyProcessed(Integer successfullyProcessed) { this.successfullyProcessed = successfullyProcessed; }

        public Integer getMembersWithErrors() { return membersWithErrors; }
        public void setMembersWithErrors(Integer membersWithErrors) { this.membersWithErrors = membersWithErrors; }

        public BigDecimal getTotalSavingsPosted() { return totalSavingsPosted; }
        public void setTotalSavingsPosted(BigDecimal totalSavingsPosted) { this.totalSavingsPosted = totalSavingsPosted; }

        public BigDecimal getTotalLoanRepaymentsPosted() { return totalLoanRepaymentsPosted; }
        public void setTotalLoanRepaymentsPosted(BigDecimal totalLoanRepaymentsPosted) { this.totalLoanRepaymentsPosted = totalLoanRepaymentsPosted; }

        public Integer getTotalTransactionsProcessed() { return totalTransactionsProcessed; }
        public void setTotalTransactionsProcessed(Integer totalTransactionsProcessed) { this.totalTransactionsProcessed = totalTransactionsProcessed; }

        public EligibilityProgress getEligibilityProgress() { return eligibilityProgress; }
        public void setEligibilityProgress(EligibilityProgress eligibilityProgress) { this.eligibilityProgress = eligibilityProgress; }
    }

    // Eligibility Progress by Month Level
    public static class EligibilityProgress {
        private Integer membersBecameEligible;  // Reached 6 months this period
        private Integer atMonth6;
        private Integer atMonth5;
        private Integer atMonth4;
        private Integer atMonth3;
        private Integer atMonth2;
        private Integer atMonth1;

        public EligibilityProgress() {}

        public Integer getMembersBecameEligible() { return membersBecameEligible; }
        public void setMembersBecameEligible(Integer membersBecameEligible) { this.membersBecameEligible = membersBecameEligible; }

        public Integer getAtMonth6() { return atMonth6; }
        public void setAtMonth6(Integer atMonth6) { this.atMonth6 = atMonth6; }

        public Integer getAtMonth5() { return atMonth5; }
        public void setAtMonth5(Integer atMonth5) { this.atMonth5 = atMonth5; }

        public Integer getAtMonth4() { return atMonth4; }
        public void setAtMonth4(Integer atMonth4) { this.atMonth4 = atMonth4; }

        public Integer getAtMonth3() { return atMonth3; }
        public void setAtMonth3(Integer atMonth3) { this.atMonth3 = atMonth3; }

        public Integer getAtMonth2() { return atMonth2; }
        public void setAtMonth2(Integer atMonth2) { this.atMonth2 = atMonth2; }

        public Integer getAtMonth1() { return atMonth1; }
        public void setAtMonth1(Integer atMonth1) { this.atMonth1 = atMonth1; }
    }

    // Aggregated Summary across all batches
    public static class AggregatedSummary {
        private Integer totalBatches;
        private Integer completedBatches;
        private Integer failedBatches;
        private BigDecimal totalSavingsAllBatches;
        private BigDecimal totalRepaymentsAllBatches;
        private Integer totalMembersProcessed;
        private Integer totalEligibleMembers;

        public AggregatedSummary() {}

        public Integer getTotalBatches() { return totalBatches; }
        public void setTotalBatches(Integer totalBatches) { this.totalBatches = totalBatches; }

        public Integer getCompletedBatches() { return completedBatches; }
        public void setCompletedBatches(Integer completedBatches) { this.completedBatches = completedBatches; }

        public Integer getFailedBatches() { return failedBatches; }
        public void setFailedBatches(Integer failedBatches) { this.failedBatches = failedBatches; }

        public BigDecimal getTotalSavingsAllBatches() { return totalSavingsAllBatches; }
        public void setTotalSavingsAllBatches(BigDecimal totalSavingsAllBatches) { this.totalSavingsAllBatches = totalSavingsAllBatches; }

        public BigDecimal getTotalRepaymentsAllBatches() { return totalRepaymentsAllBatches; }
        public void setTotalRepaymentsAllBatches(BigDecimal totalRepaymentsAllBatches) { this.totalRepaymentsAllBatches = totalRepaymentsAllBatches; }

        public Integer getTotalMembersProcessed() { return totalMembersProcessed; }
        public void setTotalMembersProcessed(Integer totalMembersProcessed) { this.totalMembersProcessed = totalMembersProcessed; }

        public Integer getTotalEligibleMembers() { return totalEligibleMembers; }
        public void setTotalEligibleMembers(Integer totalEligibleMembers) { this.totalEligibleMembers = totalEligibleMembers; }
    }
}
