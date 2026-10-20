package com.applicationtracker.config;

import com.applicationtracker.dto.ReauthRequiredResponse;
import com.applicationtracker.exception.ReauthRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReauthRequiredException.class)
    public ResponseEntity<ReauthRequiredResponse> handleReauthRequired(ReauthRequiredException ex) {
        ReauthRequiredResponse response = new ReauthRequiredResponse(
                "reauth_required",
                ex.getMessage(),
                ex.getAuthUrl()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
