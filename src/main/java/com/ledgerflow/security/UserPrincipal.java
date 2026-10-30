package com.ledgerflow.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * UserPrincipal — Authenticated User Details for Spring Security.
 * Encapsulates role (ROLE_BANK_MANAGER or ROLE_CUSTOMER) and bank metadata.
 */
public class UserPrincipal implements UserDetails {

    private final String username;
    private final String password;
    private final String displayName;
    private final String role;
    private final String accountNumber;
    private final String branchCode;

    public UserPrincipal(String username, String password, String displayName,
                         String role, String accountNumber, String branchCode) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        this.accountNumber = accountNumber;
        this.branchCode = branchCode;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getBranchCode() {
        return branchCode;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}

