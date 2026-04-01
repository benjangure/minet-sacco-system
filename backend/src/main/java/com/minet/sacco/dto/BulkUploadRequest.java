package com.minet.sacco.dto;

public class BulkUploadRequest {
    private String batchType; // MONTHLY_CONTRIBUTIONS, LOAN_DISBURSEMENTS, MEMBER_REGISTRATION

    public String getBatchType() {
        return batchType;
    }

    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }
}
