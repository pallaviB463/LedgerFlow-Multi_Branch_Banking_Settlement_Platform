package com.ledgerflow.dto;

import com.ledgerflow.entity.Branch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Objects for Branch.
 *
 * WHY DTOs? We never expose JPA entities directly over the API because:
 *   1. Entities have lazy-loaded collections that cause serialization problems
 *   2. We'd accidentally expose internal fields (e.g., DB IDs of related objects)
 *   3. We want to control exactly what the API input/output looks like
 *   4. Input validation belongs on the DTO, not the entity
 *
 * This file contains nested static classes — one per "shape" of data.
 */
public class BranchDto {

    /**
     * Request DTO: used when creating a new branch (POST /api/branches).
     * Contains only the fields the client needs to provide.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "Branch name is required")
        @Size(max = 100, message = "Branch name must not exceed 100 characters")
        private String name;

        @Size(max = 255, message = "Location must not exceed 255 characters")
        private String location;

        @NotBlank(message = "Branch code is required")
        @Size(max = 20, message = "Branch code must not exceed 20 characters")
        private String branchCode;
    }

    /**
     * Response DTO: returned when reading branch data (GET /api/branches).
     * Contains everything we want to expose about a branch.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String location;
        private String branchCode;
        private boolean active;
        private Instant createdAt;
        private Instant updatedAt;

        /** Convert a Branch entity to a Response DTO. */
        public static Response from(Branch branch) {
            return Response.builder()
                    .id(branch.getId())
                    .name(branch.getName())
                    .location(branch.getLocation())
                    .branchCode(branch.getBranchCode())
                    .active(branch.isActive())
                    .createdAt(branch.getCreatedAt())
                    .updatedAt(branch.getUpdatedAt())
                    .build();
        }
    }
}
