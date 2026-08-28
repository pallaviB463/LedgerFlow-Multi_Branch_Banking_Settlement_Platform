package com.ledgerflow.controller;

import com.ledgerflow.dto.ApiResponse;
import com.ledgerflow.dto.BranchDto;
import com.ledgerflow.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BranchController — REST API endpoints for branch management.
 *
 * Base path: /api/branches
 *
 * @RestController: Combines @Controller + @ResponseBody.
 *                  Every method returns JSON automatically.
 * @RequestMapping: Base URL prefix for all methods in this class.
 * @RequiredArgsConstructor: Lombok generates constructor for final fields.
 */
@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    /**
     * POST /api/branches
     * Create a new bank branch.
     *
     * Request body example:
     * {
     *   "name": "North Branch",
     *   "location": "45 North Street",
     *   "branchCode": "LFLW004"
     * }
     *
     * @Valid triggers bean validation on the request body.
     * Returns 201 Created with the new branch data.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BranchDto.Response>> createBranch(
            @Valid @RequestBody BranchDto.CreateRequest request) {
        BranchDto.Response branch = branchService.createBranch(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Branch created successfully", branch));
    }

    /**
     * GET /api/branches
     * List all branches.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchDto.Response>>> getAllBranches() {
        List<BranchDto.Response> branches = branchService.getAllBranches();
        return ResponseEntity.ok(
                ApiResponse.success("Branches retrieved successfully", branches));
    }

    /**
     * GET /api/branches/{id}
     * Get a single branch by its database ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDto.Response>> getBranchById(
            @PathVariable Long id) {
        BranchDto.Response branch = branchService.getBranchById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Branch retrieved successfully", branch));
    }

    /**
     * GET /api/branches/code/{branchCode}
     * Get a branch by its branch code (e.g., "LFLW001").
     */
    @GetMapping("/code/{branchCode}")
    public ResponseEntity<ApiResponse<BranchDto.Response>> getBranchByCode(
            @PathVariable String branchCode) {
        BranchDto.Response branch = branchService.getBranchByCode(branchCode);
        return ResponseEntity.ok(
                ApiResponse.success("Branch retrieved successfully", branch));
    }

    /**
     * PATCH /api/branches/{id}/deactivate
     * Deactivate a branch (soft-disable — no data is deleted).
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<BranchDto.Response>> deactivateBranch(
            @PathVariable Long id) {
        BranchDto.Response branch = branchService.deactivateBranch(id);
        return ResponseEntity.ok(
                ApiResponse.success("Branch deactivated successfully", branch));
    }

    /**
     * PATCH /api/branches/{id}/activate
     * Reactivate a previously deactivated branch.
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<BranchDto.Response>> activateBranch(
            @PathVariable Long id) {
        BranchDto.Response branch = branchService.activateBranch(id);
        return ResponseEntity.ok(
                ApiResponse.success("Branch activated successfully", branch));
    }
}
