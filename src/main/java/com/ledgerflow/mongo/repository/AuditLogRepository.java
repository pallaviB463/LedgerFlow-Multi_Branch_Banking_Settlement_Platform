package com.ledgerflow.mongo.repository;

import com.ledgerflow.mongo.document.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findTop50ByOrderByTimestampDesc();

    List<AuditLog> findByBranchCodeOrderByTimestampDesc(String branchCode);

    List<AuditLog> findByEventTypeOrderByTimestampDesc(String eventType);

    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(Instant start, Instant end);
}

