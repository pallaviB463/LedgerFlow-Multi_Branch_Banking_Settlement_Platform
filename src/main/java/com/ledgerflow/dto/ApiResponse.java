package com.ledgerflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Generic API response wrapper.
 *
 * ALL API responses are wrapped in this object so the frontend
 * always gets a consistent shape:
 *
 * Success:
 * {
 *   "success": true,
 *   "message": "Account created successfully",
 *   "data": { ...account data... },
 *   "timestamp": "2026-08-28T..."
 * }
 *
 * Error (handled by GlobalExceptionHandler):
 * {
 *   "success": false,
 *   "message": "Account not found with id: 999",
 *   "data": null,
 *   "timestamp": "2026-08-28T..."
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;

    /** Convenience factory for success responses. */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /** Convenience factory for error responses. */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(Instant.now())
                .build();
    }
}
