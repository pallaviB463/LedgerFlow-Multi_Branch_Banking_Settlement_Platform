package com.ledgerflow.dto;

import com.ledgerflow.entity.Branch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** DTOs for Branch operations. */
public class BranchDto {

    /** Request body for POST /api/branches */
    public static class CreateRequest {
        @NotBlank(message = "Branch name is required")
        @Size(max = 100)
        private String name;

        @Size(max = 255)
        private String location;

        @NotBlank(message = "Branch code is required")
        @Size(max = 20)
        private String branchCode;

        public CreateRequest() {}
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getBranchCode() { return branchCode; }
        public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    }

    /** Response body for branch read operations. */
    public static class Response {
        private Long id;
        private String name;
        private String location;
        private String branchCode;
        private boolean active;
        private Instant createdAt;
        private Instant updatedAt;

        public Response() {}

        public static Response from(Branch branch) {
            Response r = new Response();
            r.id = branch.getId();
            r.name = branch.getName();
            r.location = branch.getLocation();
            r.branchCode = branch.getBranchCode();
            r.active = branch.isActive();
            r.createdAt = branch.getCreatedAt();
            r.updatedAt = branch.getUpdatedAt();
            return r;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getLocation() { return location; }
        public String getBranchCode() { return branchCode; }
        public boolean isActive() { return active; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
    }
}
