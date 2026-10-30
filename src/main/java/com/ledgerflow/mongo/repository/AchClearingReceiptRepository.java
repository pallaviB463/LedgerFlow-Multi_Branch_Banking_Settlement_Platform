package com.ledgerflow.mongo.repository;

import com.ledgerflow.mongo.document.AchClearingReceipt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AchClearingReceiptRepository extends MongoRepository<AchClearingReceipt, String> {

    Optional<AchClearingReceipt> findByBatchId(Long batchId);
}

