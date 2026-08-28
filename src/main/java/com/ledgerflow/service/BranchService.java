package com.ledgerflow.service;

import com.ledgerflow.dto.BranchDto;
import com.ledgerflow.entity.Branch;
import com.ledgerflow.exception.DuplicateResourceException;
import com.ledgerflow.exception.ResourceNotFoundException;
import com.ledgerflow.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * BranchService — business logic for branch management.
 *
 * The @Service annotation tells Spring this is a service bean.
 * The @Transactional annotation ensures database operations are wrapped
 * in a transaction — if anything fails, changes are rolled back automatically.
 *
 * Rule: Controllers NEVER talk to repositories directly.
 *       Controllers → Services → Repositories → Database
 *       This layering keeps code organized and testable.
 */
@Service
@RequiredArgsConstructor
public class BranchService {

    private static final Logger log = LoggerFactory.getLogger(BranchService.class);

    private final BranchRepository branchRepository;

    /**
     * Create a new branch.
     *
     * Business rules:
     *   - Branch name must be unique
     *   - Branch code must be unique
     *
     * @param request the creation request DTO
     * @return the created branch as a Response DTO
     */
    @Transactional
    public BranchDto.Response createBranch(BranchDto.CreateRequest request) {
        // Check for duplicate name
        if (branchRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "A branch with name '" + request.getName() + "' already exists");
        }
        // Check for duplicate code
        if (branchRepository.existsByBranchCode(request.getBranchCode())) {
            throw new DuplicateResourceException(
                    "A branch with code '" + request.getBranchCode() + "' already exists");
        }

        Branch branch = new Branch(
                request.getName(),
                request.getLocation(),
                request.getBranchCode()
        );

        Branch saved = branchRepository.save(branch);
        log.info("Created branch: {} ({})", saved.getName(), saved.getBranchCode());
        return BranchDto.Response.from(saved);
    }

    /**
     * Get all branches.
     * @Transactional(readOnly = true) is an optimization — read-only transactions
     * skip dirty-checking overhead and can use read replicas.
     */
    @Transactional(readOnly = true)
    public List<BranchDto.Response> getAllBranches() {
        return branchRepository.findAll()
                .stream()
                .map(BranchDto.Response::from)
                .toList();
    }

    /**
     * Get a single branch by ID.
     */
    @Transactional(readOnly = true)
    public BranchDto.Response getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found with id: " + id));
        return BranchDto.Response.from(branch);
    }

    /**
     * Get a single branch by branch code.
     */
    @Transactional(readOnly = true)
    public BranchDto.Response getBranchByCode(String branchCode) {
        Branch branch = branchRepository.findByBranchCode(branchCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found with code: " + branchCode));
        return BranchDto.Response.from(branch);
    }

    /**
     * Deactivate a branch (soft disable — sets active=false).
     * Does NOT delete the branch or its accounts.
     */
    @Transactional
    public BranchDto.Response deactivateBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found with id: " + id));
        branch.setActive(false);
        Branch saved = branchRepository.save(branch);
        log.info("Deactivated branch: {} ({})", saved.getName(), saved.getBranchCode());
        return BranchDto.Response.from(saved);
    }

    /**
     * Reactivate a previously deactivated branch.
     */
    @Transactional
    public BranchDto.Response activateBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found with id: " + id));
        branch.setActive(true);
        Branch saved = branchRepository.save(branch);
        log.info("Activated branch: {} ({})", saved.getName(), saved.getBranchCode());
        return BranchDto.Response.from(saved);
    }
}
