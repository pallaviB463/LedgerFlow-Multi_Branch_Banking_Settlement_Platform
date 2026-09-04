package com.ledgerflow.repository;

import com.ledgerflow.entity.AchBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AchBatchRepository extends JpaRepository<AchBatch, Long> {

    @Query("SELECT b FROM AchBatch b WHERE b.status = :status ORDER BY b.createdAt ASC LIMIT 1")
    Optional<AchBatch> findFirstByStatusOrderByCreatedAtAsc(@Param("status") AchBatch.BatchStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM AchBatch b WHERE b.id = :id")
    Optional<AchBatch> findByIdWithLock(@Param("id") Long id);
}
