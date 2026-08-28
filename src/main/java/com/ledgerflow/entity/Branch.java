package com.ledgerflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bank branch.
 *
 * A branch is a physical or logical location of the bank
 * (e.g., "Downtown Branch", "Airport Branch").
 * Each account belongs to exactly one branch.
 *
 * Table: branches
 */
@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
public class Branch {

    /**
     * Auto-generated unique identifier.
     * IDENTITY means the database generates it on INSERT.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name of the branch.
     * Example: "Downtown Branch", "Airport Branch"
     * Must be unique — no two branches can have the same name.
     */
    @NotBlank(message = "Branch name is required")
    @Size(max = 100, message = "Branch name must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Branch location or address (optional descriptive field).
     */
    @Size(max = 255, message = "Location must not exceed 255 characters")
    @Column(length = 255)
    private String location;

    /**
     * IFSC-style branch code. Unique identifier used in inter-branch transfers.
     * Example: "LFLW001", "LFLW002"
     */
    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Branch code must not exceed 20 characters")
    @Column(name = "branch_code", nullable = false, unique = true, length = 20)
    private String branchCode;

    /**
     * Whether the branch is currently active and can process transactions.
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Timestamp when this branch record was created.
     * Set automatically on INSERT, never updated.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when this branch record was last modified.
     * Updated automatically on every UPDATE.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * All accounts that belong to this branch.
     * mappedBy = "branch" means the 'branch' field on Account owns the relationship.
     * cascade = ALL means if a Branch is saved, its accounts are also saved.
     * FetchType.LAZY means accounts are NOT loaded from DB unless explicitly accessed
     * (important for performance).
     */
    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();

    public Branch(String name, String location, String branchCode) {
        this.name = name;
        this.location = location;
        this.branchCode = branchCode;
    }
}
