package com.ledgerflow.security;

import com.ledgerflow.dto.AuthDto;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UserService — RBAC User Directory & Authentication Provider.
 *
 * Pre-seeds institutional bank manager roles and individual customer accounts
 * as mandated by Section 11 of the DBMS Project Rubric.
 */
@Service
public class UserService implements UserDetailsService {

    private final Map<String, UserPrincipal> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        initUsers();
    }

    private void initUsers() {
        // Institutional Bank Managers (Full administrative, settlement, and compliance access)
        registerUser(new UserPrincipal(
                "admin",
                passwordEncoder.encode("admin123"),
                "Chief Banking Officer",
                "ROLE_BANK_MANAGER",
                null,
                "BR-001"
        ));
        registerUser(new UserPrincipal(
                "manager_dt",
                passwordEncoder.encode("manager123"),
                "Downtown Branch Manager",
                "ROLE_BANK_MANAGER",
                null,
                "BR-001"
        ));
        registerUser(new UserPrincipal(
                "teller",
                passwordEncoder.encode("teller123"),
                "Settlement Operator",
                "ROLE_BANK_MANAGER",
                null,
                "BR-002"
        ));

        // Retail Customers (Transfer capability & statement inquiry)
        registerUser(new UserPrincipal(
                "alice",
                passwordEncoder.encode("alice123"),
                "Alice Johnson",
                "ROLE_CUSTOMER",
                "ACC-10001",
                "BR-001"
        ));
        registerUser(new UserPrincipal(
                "bob",
                passwordEncoder.encode("bob123"),
                "Bob Smith",
                "ROLE_CUSTOMER",
                "ACC-10002",
                "BR-001"
        ));
        registerUser(new UserPrincipal(
                "charlie",
                passwordEncoder.encode("charlie123"),
                "Charlie Davis",
                "ROLE_CUSTOMER",
                "ACC-10003",
                "BR-002"
        ));
    }

    public void registerUser(UserPrincipal user) {
        users.put(user.getUsername().toLowerCase(), user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserPrincipal user = users.get(username.toLowerCase());
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return user;
    }

    public UserPrincipal authenticate(String username, String rawPassword) {
        UserPrincipal user = users.get(username.toLowerCase());
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            return null;
        }
        return user;
    }

    public List<AuthDto.DemoUserDto> getDemoUsers() {
        return List.of(
                new AuthDto.DemoUserDto("admin", "admin123", "ROLE_BANK_MANAGER", "Chief Banking Officer", "Full admin, batch settlement trigger, compliance risk analytics", "N/A"),
                new AuthDto.DemoUserDto("teller", "teller123", "ROLE_BANK_MANAGER", "Settlement Operator", "Account creation, batch settlement & monitoring", "N/A"),
                new AuthDto.DemoUserDto("alice", "alice123", "ROLE_CUSTOMER", "Alice Johnson", "Retail customer with transfer permissions", "ACC-10001"),
                new AuthDto.DemoUserDto("bob", "bob123", "ROLE_CUSTOMER", "Bob Smith", "Retail customer with transfer permissions", "ACC-10002")
        );
    }
}

