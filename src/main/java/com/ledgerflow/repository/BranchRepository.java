package com.ledgerflow.repository;

import com.ledgerflow.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Branch entities.
 *
 * Spring Data JPA generates all SQL automatically from method names.
 * We don't need to write any SQL here — just declare what we need.
 *
 * JpaRepository<Branch, Long> gives us for free:
 *   - save(branch)
 *   - findById(id)
 *   - findAll()
 *   - delete(branch)
 *   - count()
 *   - existsById(id)
 *   ... and many more
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    /** Find a branch by its exact name (case-sensitive). */
    Optional<Branch> findByName(String name);

    /** Find a branch by its branch code (e.g., "LFLW001"). */
    Optional<Branch> findByBranchCode(String branchCode);

    /** Check if a branch name is already taken. */
    boolean existsByName(String name);

    /** Check if a branch code is already taken. */
    boolean existsByBranchCode(String branchCode);
}
