package com.ledgerflow.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB Document Entity: AchClearingReceipt
 * Satisfies DBMS Experiential Learning Section 3: Second Database Paradigm (NoSQL MongoDB).
 */
@Document(collection = "ach_clearing_receipts")
public class AchClearingReceipt {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long batchId;

    private String batchNumber;
    private String status;
    private BigDecimal totalSettledAmount;
    private int transferCount;
    private Instant settledAt;

    private List<Map<String, Object>> transfers = new ArrayList<>();
    private Map<String, BigDecimal> branchNetting = new HashMap<>();

    public AchClearingReceipt() {
        this.settledAt = Instant.now();
    }

    public AchClearingReceipt(Long batchId, String batchNumber, String status,
                              BigDecimal totalSettledAmount, int transferCount,
                              List<Map<String, Object>> transfers,
                              Map<String, BigDecimal> branchNetting) {
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.status = status;
        this.totalSettledAmount = totalSettledAmount;
        this.transferCount = transferCount;
        this.settledAt = Instant.now();
        if (transfers != null) this.transfers.addAll(transfers);
        if (branchNetting != null) this.branchNetting.putAll(branchNetting);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalSettledAmount() { return totalSettledAmount; }
    public void setTotalSettledAmount(BigDecimal totalSettledAmount) { this.totalSettledAmount = totalSettledAmount; }

    public int getTransferCount() { return transferCount; }
    public void setTransferCount(int transferCount) { this.transferCount = transferCount; }

    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }

    public List<Map<String, Object>> getTransfers() { return transfers; }
    public void setTransfers(List<Map<String, Object>> transfers) { this.transfers = transfers; }

    public Map<String, BigDecimal> getBranchNetting() { return branchNetting; }
    public void setBranchNetting(Map<String, BigDecimal> branchNetting) { this.branchNetting = branchNetting; }
}

