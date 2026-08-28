package com.ledgerflow.service;

import com.ledgerflow.dto.BranchDto;
import com.ledgerflow.entity.Branch;
import com.ledgerflow.exception.DuplicateResourceException;
import com.ledgerflow.exception.ResourceNotFoundException;
import com.ledgerflow.repository.BranchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Business logic for branch management. */
@Service
public class BranchService {

    private static final Logger log = LoggerFactory.getLogger(BranchService.class);
    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Transactional
    public BranchDto.Response createBranch(BranchDto.CreateRequest request) {
        if (branchRepository.existsByName(request.getName()))
            throw new DuplicateResourceException("A branch with name '" + request.getName() + "' already exists");
        if (branchRepository.existsByBranchCode(request.getBranchCode()))
            throw new DuplicateResourceException("A branch with code '" + request.getBranchCode() + "' already exists");

        Branch branch = new Branch(request.getName(), request.getLocation(), request.getBranchCode());
        Branch saved = branchRepository.save(branch);
        log.info("Created branch: {} ({})", saved.getName(), saved.getBranchCode());
        return BranchDto.Response.from(saved);
    }

    @Transactional(readOnly = true)
    public List<BranchDto.Response> getAllBranches() {
        return branchRepository.findAll().stream().map(BranchDto.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public BranchDto.Response getBranchById(Long id) {
        return BranchDto.Response.from(branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id)));
    }

    @Transactional(readOnly = true)
    public BranchDto.Response getBranchByCode(String branchCode) {
        return BranchDto.Response.from(branchRepository.findByBranchCode(branchCode)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with code: " + branchCode)));
    }

    @Transactional
    public BranchDto.Response deactivateBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
        branch.setActive(false);
        return BranchDto.Response.from(branchRepository.save(branch));
    }

    @Transactional
    public BranchDto.Response activateBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
        branch.setActive(true);
        return BranchDto.Response.from(branchRepository.save(branch));
    }
}
