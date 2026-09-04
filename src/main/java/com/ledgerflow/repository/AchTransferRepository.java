package com.ledgerflow.repository;

import com.ledgerflow.entity.AchTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchTransferRepository extends JpaRepository<AchTransfer, Long> {
    
    List<AchTransfer> findByBatchId(Long batchId);
}
