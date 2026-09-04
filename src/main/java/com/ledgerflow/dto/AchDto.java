package com.ledgerflow.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AchDto {

    public static class AchTransferRequest {
        @NotNull
        private Long sourceAccountId;
        @NotNull
        private Long destinationAccountId;
        @NotNull
        @Positive
        private BigDecimal amount;

        public Long getSourceAccountId() { return sourceAccountId; }
        public void setSourceAccountId(Long sourceAccountId) { this.sourceAccountId = sourceAccountId; }

        public Long getDestinationAccountId() { return destinationAccountId; }
        public void setDestinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public static class AchTransferResponse {
        private Long transferId;
        private Long batchId;
        private String status;
        private BigDecimal amount;
        private String message;

        // Getters and Setters
        public Long getTransferId() { return transferId; }
        public void setTransferId(Long transferId) { this.transferId = transferId; }

        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
