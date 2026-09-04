package com.ledgerflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ach_batches")
public class AchBatch {

    public enum BatchStatus {
        OPEN,       // Accepting transfers
        PROCESSING, // Settlement engine is currently processing this batch
        SETTLED,    // Successfully settled
        FAILED      // Settlement failed catastrophically
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus status = BatchStatus.OPEN;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer transactionCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // We don't necessarily need the one-to-many side in code if we use repositories,
    // but it's helpful. For huge batches, this list could get large, but we'll include it.
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AchTransfer> transfers = new ArrayList<>();

    public AchBatch() {}

    public void addTransfer(AchTransfer transfer) {
        transfers.add(transfer);
        transfer.setBatch(this);
        totalAmount = totalAmount.add(transfer.getAmount());
        transactionCount++;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public List<AchTransfer> getTransfers() { return transfers; }
    public void setTransfers(List<AchTransfer> transfers) { this.transfers = transfers; }
}
