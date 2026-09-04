package com.ledgerflow.controller;

import com.ledgerflow.dto.ApiResponse;
import com.ledgerflow.dto.BranchDto;
import com.ledgerflow.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST endpoints for branch management. Base: /api/branches */
@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BranchDto.Response>> createBranch(
            @Valid @RequestBody BranchDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Branch created successfully", branchService.createBranch(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchDto.Response>>> getAllBranches() {
        return ResponseEntity.ok(ApiResponse.success("Branches retrieved successfully", branchService.getAllBranches()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDto.Response>> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Branch retrieved successfully", branchService.getBranchById(id)));
    }

    @GetMapping("/code/{branchCode}")
    public ResponseEntity<ApiResponse<BranchDto.Response>> getBranchByCode(@PathVariable String branchCode) {
        return ResponseEntity.ok(ApiResponse.success("Branch retrieved successfully", branchService.getBranchByCode(branchCode)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<BranchDto.Response>> deactivateBranch(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Branch deactivated successfully", branchService.deactivateBranch(id)));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<BranchDto.Response>> activateBranch(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Branch activated successfully", branchService.activateBranch(id)));
    }
}
