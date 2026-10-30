package com.ledgerflow.controller;

import com.ledgerflow.dto.ApiResponse;
import com.ledgerflow.dto.AuthDto;
import com.ledgerflow.security.JwtTokenProvider;
import com.ledgerflow.security.UserPrincipal;
import com.ledgerflow.security.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AuthController — Authentication and RBAC endpoints.
 * Handles token issuance, session verification, and credentials catalog.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserService userService, JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        UserPrincipal principal = userService.authenticate(request.getUsername(), request.getPassword());
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        }

        String token = tokenProvider.generateToken(principal);
        AuthDto.AuthResponse response = new AuthDto.AuthResponse(
                token,
                principal.getUsername(),
                principal.getDisplayName(),
                principal.getRole(),
                principal.getAccountNumber(),
                principal.getBranchCode()
        );

        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No active authenticated session"));
        }

        AuthDto.AuthResponse response = new AuthDto.AuthResponse(
                null,
                principal.getUsername(),
                principal.getDisplayName(),
                principal.getRole(),
                principal.getAccountNumber(),
                principal.getBranchCode()
        );
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", response));
    }

    @GetMapping("/demo-users")
    public ResponseEntity<ApiResponse<List<AuthDto.DemoUserDto>>> getDemoUsers() {
        return ResponseEntity.ok(ApiResponse.success("Demo credentials catalog", userService.getDemoUsers()));
    }
}

